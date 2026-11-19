# Katkı Rehberi (Contributing)

Projeye katkı sağlamak isteyen herkese hoş geldiniz! Amacımız şeffaf, bağımlılıksız
ve eğitici bir yerel ajan mimarisi geliştirmek.

## Katkı türleri

- **Kod:** yeni araçlar, yeni aktivasyon/kayıp fonksiyonları, optimizer'lar
- **Dokümantasyon:** README, örnekler, Türkçe/İngilizce açıklamalar
- **Test:** senaryo testleri, hata ayıklama, performans ölçümleri
- **Fikirler:** AGI yolunda deneysel mimari önerileri (issues)

## Geliştirme ortamı

- JDK 8+ (projede `--release 8` ile derlenir; JDK 21 ile test edildi)
- Ollama + yerel bir model (örn. `qwen2.5:7b-instruct`) — sadece `agent` modülü için
- Weka 3.8.7 JAR — sadece `mlweka` modülü için (`lib/weka.jar`)

```bat
build.bat           :: sıfırdan sinir ağı demosu
build-agent.bat     :: hibrit ajan
build-weka.bat      :: Weka örneği
```

## Kurallar

1. **Bağımlılık eklemeyin.** `neuralnet` ve `agent` paketleri tamamen bağımlılıksız kalmalı.
2. **Java 8 uyumluluğu.** `var`, record, text block gibi yeni sözdizimi kullanmayın.
3. **Kişisel veri commit etmeyin.** `vector-memory.json`, `goals.json`, `learned-intents.json`
   `.gitignore` içinde; öğrenilen veriler kullanıcıya özeldir.
4. **AGI iddiasından kaçının.** Bu proje deneysel bir ajan mimarisidir. Dokümantasyonda
   "AGI" ifadesini gelecek hedef olarak kullanın, gerçekleşmiş bir sonuç olarak değil.
5. Değişiklikleri küçük commit'lerle yapın; commit mesajları Türkçe veya İngilizce olabilir.

## Pull request öncesi kontrol

- [ ] Tüm `.bat` scriptleri sorunsuz çalışıyor
- [ ] `javac --release 8 -encoding UTF-8 -Xlint:-options` ile derleme hatasız
- [ ] README/ilgili doküman güncellendi
- [ ] Yeni dosyalar `.gitignore`'a takılmadı

## İletişim

Bir issue açın veya tartışma bölümünde fikirlerinizi paylaşın.
