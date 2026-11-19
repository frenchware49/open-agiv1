# Entegrasyon Rehberi

Bu proje, daha büyük sistemlere gömülmek üzere tasarlanmıştır. Aşağıda
geliştiriciler ve kurumlar için pratik entegrasyon notları vardır.

## Lisans

- Bu kod: **MIT** — ticari kullanım, değiştirme, dağıtım serbest.
- Gemma 4: **Apache 2.0** (Google DeepMind).
- Weka (opsiyonel modül): kendi lisansına tabidir; ticari kullanımda kontrol edin.

Yani çekirdek mimariyi ticari bir ürüne gömmek için izin gerekmez.

## Gömme (embedding)

`Agent` sınıfı doğrudan kullanılabilir:

```java
Agent agent = new Agent("gemma4:e4b");
agent.check();                     // Ollama + model kontrolü
String cevap = agent.respond("2+2 kaç eder?");
```

Not: `Main` sadece bir CLI örneğidir; kendi arayüzünüzü yazabilirsiniz.

## Servis olarak çalıştırma

Şu an proje CLI tabanlıdır. Bir HTTP servisi için:
1. `Agent` örneğini uygulama başında oluşturun (ağ eğitimi ~1 saniye sürer)
2. İstek başına `agent.respond(mesaj)` çağırın
3. `agent` örneğini **paylaşımlı** tutun (her istekte yeniden eğitim yapmayın)

Thread güvenliği: şu an `history` ve `router` paylaşımlı durum tutar.
Eşzamanlı istekler için istek başına `Agent` oluşturun veya `Agent`'i senkronize edin.

## Model seçimi

| Model | VRAM | Kullanım |
|---|---|---|
| `gemma4:e4b` | ~6.6 GB | Önerilen (güçlü, yerel, Apache 2.0) |
| `gemma4:e2b` | ~4.6 GB | Düşük kaynaklı ortam |
| `gemma4:12b` | ~7.7 GB | 8 GB VRAM'de sınırda |
| `qwen2.5:7b-instruct` | ~4.7 GB | Varsayılan, Türkçe başarılı |

Ağ (neuralnet) CPU'da çalışır; GPU çakışması yoktur.

## Ölçekleme notu

Tek bir makinede sınırlar:
- Ollama tek seferde bir modeli GPU'da tutar (diğerleri boşta kapanır)
- Vektör hafıza ve bilgi grafığı JSON dosyalarında tutulur; büyük ölçekte
  veritabanı (örn. SQLite/pgvector) ile değiştirilmelidir
- `learned-intents.json` büyüdükçe yeniden eğitim süresi artar; örnek
  seçimi (replay sampling) eklenebilir

## Güvenlik

- `filesystem` aracı proje klasörüyle sınırlıdır
- `code_runner` **keyfi kod çalıştırır** — üretimde izole bir konteyner/
  sanal makinede çalıştırın veya aracı kapatın
- `web_search` sadece Wikipedia API'sine gider

## Katkı ve destek

Bkz. [CONTRIBUTING.md](../CONTRIBUTING.md) ve [ROADMAP.md](ROADMAP.md).
