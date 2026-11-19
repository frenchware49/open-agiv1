# Güvenlik Politikası

Bu proje **deneysel bir araştırma kodudur (AGI Deneysel V1)**, üretim sistemlerine
yönelik değildir. Yine de güvenlik açıkları ciddiye alınır.

## Desteklenen Sürümler

| Sürüm | Durum |
|---|---|
| Deneysel V1 (`master` ana dalı) | ✅ Destekleniyor |
| Etiketlenmiş eski sürümler | ❌ Desteklenmiyor (yok) |

Yeni sürüm çıktığında yalnızca en son ana dal güvenlik güncellemeleri alır.
Kendi fork'unuzu kullanıyorsanız güncellemeleri takip etmek sizin sorumluluğunuzdur.

## Güvenlik Açığı Bildirme

**Açığı herkese açık bir issue olarak paylaşmayın.**

Tercih edilen yöntem: GitHub reposunda **Security → Report a vulnerability**
(özel bildirim) bölümünü kullanın. Bu mümkün değilse repo sahibine doğrudan
mesaj gönderin.

Lütfen şunları ekleyin:
- Etkilenen dosya/katman (örn. `FileTool`, `WebResearch`, `OllamaClient`)
- Yeniden üretme adımları
- Olası etki (veri sızıntısı, yetkisiz dosya erişimi, prompt injection vb.)
- Dilerseniz önerilen düzeltme

## Yanıt Süreleri (en iyi çaba)

| Aşama | Süre |
|---|---|
| Bildirim onayı | 7 gün içinde |
| İlk değerlendirme / kabul-red kararı | 14 gün içinde |
| Düzeltme yayını | Onaydan sonra 30 gün içinde (duruma göre değişir) |

Kabul edilen açıklar için düzeltme `master` dalına işlenir ve sürüm notlarında
belirtilir (isterseniz bildiren kişinin adı anılır). Reddedilen bildirimlerde
gerekçe açıklanır.

## Kapsam

**Kapsam dahilinde:**
- `src/agent/` ve `src/neuralnet/` kodundaki güvenlik açıkları
- Bağışıklık sistemi (`ImmuneSystem`) atlatılabilen prompt injection / zihin virüsü kalıpları
- `filesystem` aracının proje klasörü dışına çıkması
- Kişisel veri dosyalarının (`vector-memory.json`, `goals.json`, `learned-intents.json`,
  `knowledge-graph.json`, `immune-quarantine.json`) yanlışlıkla commit edilmesi
- API sunucusunda (`localhost:8080`) kimlik doğrulama/CORS sorunları

**Kapsam dışında:**
- `code_runner` aracının tasarım gereği keyfi kod çalıştırması — bu **bilinçli bir
  özelliktir**. Üretimde izole bir ortamda çalıştırın veya aracı kapatın.
- İndirilen üçüncü parti modellerin (Ollama / Hugging Face) kendi güvenlik sorunları
- Kullanıcının kendi yazdığı prompt'ların sonuçları
- AGI iddiaları / model çıktılarının doğruluğu — bu bir güvenlik konusu değildir

## Not

Bu proje açık kaynak ve deneyseldir. Güvenlik düzeltmeleri "en iyi çaba" esasına
dayanır; ticari bir destek sözleşmesi yoktur. Kritik sistemlerde kullanmayın.
