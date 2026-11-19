# Mimari ve Genişletme Noktaları

Bu belge, projeyi devralan geliştiriciler için teknik haritadır.

## Katman diyagramı

```
Kullanıcı
   │
   ▼
┌──────────────────────────────────────────────┐
│ agent.Main            CLI / komut yönetimi   │
└───────────────────┬──────────────────────────┘
                    ▼
┌──────────────────────────────────────────────┐
│ agent.Agent           Orkestrasyon           │
│  ├─ IntentRouter      Yerel MLP (yönlendirici)│
│  ├─ Planner           Görev bölme            │
│  ├─ Reflector         Öz-denetim             │
│  ├─ VectorMemory      Anlamsal hafıza        │
│  ├─ KnowledgeGraph    Dünya modeli           │
│  ├─ GoalManager       Özerk hedefler         │
│  └─ MultiAgent        Uzman ajanlar          │
└───────────────────┬──────────────────────────┘
                    ▼
┌──────────────────────────────────────────────┐
│ agent.OllamaClient    Yerel LLM köprüsü       │
│  → http://localhost:11434/api/chat           │
└───────────────────┬──────────────────────────┘
                    ▼
        Ollama  (qwen2.5 / gemma4 / ...)
                    │
                    ▼
        Tools: calculator, filesystem, clock,
               web_search, code_runner
```

## Bağımlılık yönü

```
neuralnet  ←  agent  ←  Main
              ↑
           tools (Tool arayüzü)
```

- `neuralnet` paketi **tamamen bağımsızdır**; hiçbir şeye bağımlı değil, saf Java.
- `agent` paketi `neuralnet`'i kullanır ama tersi yoktur.
- `mlweka` tamamen ayrı bir örnektir.

## Genişletme noktaları

### Yeni model bağlamak
`OllamaClient` model adını bir string olarak alır. Yeni model için:
```bat
ollama pull <model>
```
ve ajan içinde `/model <model>`. Kod değişikliği gerekmez.
Farklı bir backend (vLLM, llama.cpp) için `OllamaClient.chat()` imzasını koruyan yeni bir sınıf yazıp `Agent` constructor'ında değiştirin.

### Yeni araç eklemek
1. `Tool` arayüzünü uygulayın: `name()`, `description()`, `execute(Map<String,Object>)`
2. Sınıfı `src/agent/tools/` altına koyun
3. `Agent` constructor'ındaki `tools` listesine ekleyin

LLM aracı otomatik keşfeder; sistem prompt'u `ToolCalls.describe()` ile üretilir.

### Yeni uzman ajan eklemek
`MultiAgent` constructor'ındaki `byIntent` map'ine yeni `Specialist` ekleyin.
Yeni niyet için `IntentRouter.INTENTS` dizisini ve eğitim verisini de güncelleyin.

### Yeni aktivasyon / kayıp fonksiyonu
`Activation` veya `Loss` enum'una yeni bir değer ekleyin; `NeuralNetwork` kodu değişmeden çalışır.

## Veri ve kalıcılık

| Dosya | İçerik | Git'e girer mi? |
|---|---|---|
| `vector-memory.json` | Anı vektörleri | Hayır (kişisel) |
| `knowledge-graph.json` | Varlık-ilişki grafığı | Hayır |
| `learned-intents.json` | Öğrenilen niyet örnekleri | Hayır |
| `goals.json` | Özerk hedefler | Hayır |

## Bilinen sınırlar

Bkz. [AGI_REQUIREMENTS.md](AGI_REQUIREMENTS.md). Özetle: hazır model kullanılır,
ana model eğitilmez, ölçek sınırlıdır. Bu bir AGI değil, AGI'ye giden yolda
açık bir deney platformudur.
