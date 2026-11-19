# LoRA İnce Ayar Hattı

Gemma 4'ü **kendi verimizle** (`data/train.jsonl`) eğitmek için hazır pipeline.
Bu, "kendi modelimiz" hedefinin ikinci parçası: mini transformer tamamen bizim
ağırlıklarımız; burada ise açık bir modeli kendi davranışımıza uyarlıyoruz.

## Gereksinimler

- Python 3.10+ ve NVIDIA GPU (8 GB yeterli — 4-bit quantization + LoRA)
- internet (ilk çalıştırmada HF'den model indirilir)

```bat
py -3.12 -m pip install torch transformers peft trl datasets accelerate bitsandbytes
```

> Not: Python **3.12** kullanın (3.15'te ML paketleri henüz uyumlu değil).
> Standart PEFT + bitsandbytes yolu kullanılır; unsloth gerekmez.

## Çalıştırma

Proje kökünden:

```bat
python tools\finetune\gemma4_lora.py --epochs 3
```

veya:

```bat
build-lora.bat
```

Çıktı:
- `tools/finetune/lora-adapter/` — LoRA ağırlıkları
- `tools/finetune/Modelfile.lora` — Ollama tanımı

## Yeni modeli ajana bağlamak

```bat
cd tools\finetune
ollama create yerel-ajen-lora -f Modelfile.lora
cd ..\..
```

Sonra ajan içinde:

```
/model yerel-ajen-lora
```

Artık "beyin" de sizin verinizle eğitilmiş olur.

## Veri formatı

`data/train.jsonl` — her satır bir örnek:

```json
{"instruction":"2+2 kaç eder?","input":"","output":"2+2 = 4 eder."}
```

Kendi örneklerinizi ekleyin; ne kadar çeşitli veri, o kadar iyi sonuç.
Dikkat: bu dosya eğitim verisidir, kişisel hafıza değildir — git'e dahil edilebilir.
