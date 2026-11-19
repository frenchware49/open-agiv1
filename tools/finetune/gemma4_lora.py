#!/usr/bin/env python3
r"""
Gemma 4 üzerinde 4-bit LoRA ince ayar (standart PEFT + transformers).
Kendi verimizle (data/train.jsonl) ajanın davranışını öğretir.

Kurulum (Python 3.12 önerilir):
    py -3.12 -m pip install torch --index-url https://download.pytorch.org/whl/cu121
    py -3.12 -m pip install transformers peft trl datasets accelerate bitsandbytes

Çalıştırma:
    py -3.12 tools\finetune\gemma4_lora.py --epochs 3

Çıktı: tools/finetune/lora-adapter + Modelfile.lora
"""

import argparse
import json
import os

import torch
from datasets import Dataset
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
from transformers import (AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig,
                          DataCollatorForLanguageModeling, Trainer, TrainingArguments)


def load_rows(path):
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line:
                rows.append(json.loads(line))
    return rows


def formatting(row):
    instruction = row.get("instruction", "")
    inp = row.get("input", "")
    out = row.get("output", "")
    if inp:
        return f"### Talimat:\n{instruction}\n\n### Girdi:\n{inp}\n\n### Cevap:\n{out}"
    return f"### Talimat:\n{instruction}\n\n### Cevap:\n{out}"


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default="google/gemma-4-e2b-it")
    ap.add_argument("--data", default=os.path.join(here, "data", "train.jsonl"))
    ap.add_argument("--out", default=os.path.join(here, "lora-adapter"))
    ap.add_argument("--epochs", type=int, default=3)
    ap.add_argument("--batch", type=int, default=1)
    ap.add_argument("--lr", type=float, default=2e-4)
    args = ap.parse_args()

    print(f"Model yukleniyor: {args.model}")
    tokenizer = AutoTokenizer.from_pretrained(args.model)
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    compute_dtype = torch.bfloat16 if torch.cuda.is_bf16_supported() else torch.float16
    bnb = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_quant_type="nf4",
        bnb_4bit_compute_dtype=compute_dtype,
        bnb_4bit_use_double_quant=True,
    )
    model = AutoModelForCausalLM.from_pretrained(
        args.model,
        quantization_config=bnb,
        device_map={"": 0},
        dtype=compute_dtype,
    )
    model = prepare_model_for_kbit_training(model)
    model = get_peft_model(model, LoraConfig(
        r=16,
        lora_alpha=32,
        lora_dropout=0.05,
        bias="none",
        task_type="CAUSAL_LM",
        target_modules="all-linear",
    ))
    model.print_trainable_parameters()

    rows = load_rows(args.data)
    dataset = Dataset.from_dict({"text": [formatting(r) for r in rows]})
    print(f"Egitim ornekleri: {len(rows)}")

    def tokenize(batch):
        return tokenizer(batch["text"], truncation=True, max_length=512)

    tokenized = dataset.map(tokenize, batched=True, remove_columns=["text"])

    collator = DataCollatorForLanguageModeling(tokenizer=tokenizer, mlm=False)
    trainer = Trainer(
        model=model,
        train_dataset=tokenized,
        data_collator=collator,
        args=TrainingArguments(
            per_device_train_batch_size=args.batch,
            gradient_accumulation_steps=8,
            num_train_epochs=args.epochs,
            learning_rate=args.lr,
            logging_steps=5,
            output_dir="lora-out",
            save_strategy="no",
            report_to="none",
            optim="adamw_torch",
        ),
    )
    trainer.train()

    model.save_pretrained(args.out)
    tokenizer.save_pretrained(args.out)
    print(f"\nLoRA adapter kaydedildi: {args.out}")

    modelfile = """FROM gemma4:e2b
ADAPTER ./lora-adapter
SYSTEM \"\"\"Sen Yerel Ajan'sın. Türkçe, kısa ve net cevap ver.
Araçları gerektiğinde kullan. Dürüst ol; bilmediğini söyle.\"\"\"
"""
    with open(os.path.join(here, "Modelfile.lora"), "w", encoding="utf-8") as f:
        f.write(modelfile)
    print("Ollama modeli: ollama create yerel-ajen-lora -f Modelfile.lora")


if __name__ == "__main__":
    main()
