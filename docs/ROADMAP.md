# Yol Haritası

> **Önemli not:** Bu proje bir AGI değildir. AGI'ye giden yolda deneysel bir
> *ajan mimarisi*dir. Buradaki amaç, şeffaf ve bağımlılıksız bir şekilde
> planlama, öğrenme ve hafıza mekanizmalarını bir arada göstermektir.

## Tamamlanan

| # | Katman | Açıklama |
|---|---|---|
| 1 | Sıfırdan MLP | Matris çekirdeği, geri yayılım, Adam, softmax+cross-entropy |
| 2 | Weka örneği | Karar ağacı, 10-kat çapraz doğrulama |
| 3 | Hibrit ajan | Yerel sinir ağı + Ollama LLM + araçlar |
| 4 | Planner | İsteği alt adımlara bölme, sonuçları birleştirme |
| 5 | Self-reflection | Cevap denetimi ve düzeltilmesi |
| 6 | Vektör hafıza | 96 boyutlu hash embedding, kosinüs arama |
| 7 | Multi-agent | Niyet bazlı uzman ajanlara görev dağıtımı |
| 8 | Sürekli öğrenme | Experience replay ile canlı ağırlık güncelleme |
| 9 | Özerk hedefler | Kendi hedef üretimi, önceliklendirme, replan |
| 10 | Dünya modeli | Varlık-ilişki grafığı, olgu çıkarma, çok adımlı akıl yürütme |
| 11 | Araçlar | web_search, code_runner (Python/Java) |
| 12 | Çok turlu reflection | 2 tur + güven skoru |
| 13 | Doğrulayıcı uzmanlar | Sayı/Kod/Kaynak denetçileri |
| 14 | Benchmark + CI | `/bench`, GitHub Actions |
| 15 | Araştırarak öğrenme | Wikipedia/URL → hafıza + olgular |
| 16 | Multimodal | Gemma 4 ile görüntülü sorular |
| 17 | Swing GUI | Bağımlılıksız grafik arayüz |
| 18 | Öğrenmeyi öğrenme | MetaLearner + hızlı/slow weights |
| 19 | Dünya modeli | Nedensel kural çıkarımı + tahmin + simülasyon |
| 20 | Özerk hedef | Sistem anomalilerinden kendiliğinden hedef üretimi |
| 21 | Kendi mini transformer | Saf Java GPT: dikkat + positional + manuel backprop |
| 22 | LoRA hattı | Gemma 4 + 4-bit LoRA ince ayar pipeline |
| 23 | HTTP API sunucusu | /health /status /metrics /api/chat /api/stream /api/learn |
| 24 | Metrikler + config | Gözlemlenebilirlik ve config.properties |
| 25 | Self-consistency | 3 örnekle en tutarlı cevabı seçme |
| 26 | DIDL beyni | Kendi modellerimizi tek karar katmanında birleştirme + LLM yedeği |
| 27 | Yedek beyin | Model failover zinciri + DIDL son çare |
| 28 | Bağışıklık sistemi | Zihin virüsü / prompt injection savunması + karantina |
| 29 | Terminalsiz GUI | VBS + javaw, sessiz arka plan |
| 30 | Öz-onarım | Araç hatalarını analiz edip düzelterek yeniden deneme |
| 31 | Eklenti sistemi | plugins/ klasöründen dinamik Tool yükleme |
| 32 | Model-as-Tool | CognitiveCore: LLM araç, öğrenme kaynağı; kendi kararımız |
| 33 | AGI-v1 profil | 13 yetenek noktası, öz-model, analoji, müfredat, karşı-olgusal akıl |
| 34 | Gelişmiş metin işleme | TF-IDF, TextRank, çıkarımsal özet/QA, duygu, varlık, okunabilirlik |

## Sıradaki adımlar

### Kısa vadeli
- [x] Daha fazla araç: web arama, kod çalıştırma (takvim kaldı)
- [x] Uzman ajan sayısını artırma (doğrulayıcılar eklendi)
- [x] Reflection için çoklu tur + güven skoru
- [x] Senaryo tabanlı test altyapısı (`/eval` 14/14, `/bench` 6/6)
- [x] Swing GUI, multimodal, araştırma katmanı

### Orta vadeli
- [ ] Gerçek embedding modeli ile vektör hafıza (Ollama `/api/embed`)
- [x] Knowledge graph / dünya modeli (tamamlandı)
- [ ] Grafik ile LLM arasında daha derin entegrasyon (sorgu planlayıcı)
- [ ] Uzun vadeli öğrenmede göreli önem ağırlıkları (importance weighting)
- [ ] İngilizce dokümantasyon

### Uzun vadeli (AGI'ye yaklaşan fikirler)
- [ ] Özerk hedef üretiminin gerçek bir dünya modeline bağlanması
- [ ] Sürekli öğrenmenin LLM tarafına da uygulanması (yerel ince ayar)
- [ ] Öz-değerlendirme ile kendi mimarisini değiştirebilen meta-ajan
- [ ] Çok ajanlı müzakere ve ortak karar mekanizması

## Katkı

Bu yol haritası canlı bir belgedir. Yeni fikirler için issue veya PR açabilirsiniz.
 Gerçek AGI, tek bir kişinin değil, açık topluluğun eseri olacaktır.
