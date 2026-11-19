# Gerçek AGI için Gerekenler — Bu Projenin Durumu

> Dürüst bir değerlendirme: bu proje bir AGI değildir. Aşağıda AGI için gereken
> yetenekler ve bu kod tabanındaki karşılıkları listelenmiştir. Açık kaynak
> katkılarıyla bu boşluklar kademeli olarak kapatılabilir.

| Yetenek | Neden gerekli | Bu projede | Boşluk |
|---|---|---|---|
| **Güçlü temel model** | Dil, akıl yürütme, dünya bilgisi | Ollama üzerinden qwen2.5 / gemma4 | Modeli biz eğitmiyoruz; hazır ağırlık kullanıyoruz |
| **Ölçek** | Genelleme yeteneği | 7-12B parametre, 8 GB VRAM | Frontier modeller 100B+ ve devasa veri/kompute |
| **Online öğrenme (ana model)** | Deneyimden kalıcı öğrenme | Sadece 192 boyutlu niyet sınıflandırıcı güncelleniyor | LLM ağırlıkları sabit |
| **Dünya modeli** | Nedensellik, fizik, sosyal bilgi | Küçük varlık-ilişki grafığı | Gerçek dünyadan öğrenilmiş, sürekli güncellenen bir model yok |
| **Algı** | Metin ötesi girdi | Metin (gemma4 çok modlu olabilir) | Görüntü/ses/video işleme hattı yok |
| **Eylem** | Dünyayı değiştirme | calculator, filesystem, clock, web_search, code_runner | Kalıcı, gerçek dünya etkisi olan araçlar sınırlı |
| **Özerklük** | Kendi hedefini belirleme | LLM üretimi hedefler + replan | Ortam geri bildirimiyle gerçek öz-optimizasyon yok |
| **Bellek** | Uzun vadeli tutarlılık | Vektör hafıza + bilgi grafığı | Bellek sıkıştırma, unutma, önceliklendirme sınırlı |
| **Öz-değerlendirme** | Hataları düzeltme | Tek turlu Reflection | Çok turlu, ölçülebilir güven kalibrasyonu yok |
| **Ölçüm** | İlerlemeyi kanıtlama | `/eval` (LLM'siz testler) | ARC, MMLU, HLE gibi standart kıstaslar entegre değil |

## Yapılabilecek somut katkılar

1. `ModelProvider` soyutlaması ile farklı modeller/backend'ler (Ollama, llama.cpp, vLLM) takılabilir hale getirmek
2. LoRA/QLoRA ile yerel modelin küçük bir kısmını gerçekten eğitmek
3. Araçları genişletmek: tarayıcı kontrolü, dosya/terminal, takvim, veritabanı
4. Reflection'ı çok turlu hale getirip güven skoru kalibre etmek
5. Standart benchmark'ları (ARC/MMLU alt kümesi) eval harness'a eklemek
6. Bilgi grafığını embedding tabanlı otomatik olgu çıkarımıyla büyütmek

## Sonuç

AGI, tek bir mimari veya kütüphane değil; **ölçek + öğrenme + dünya etkileşimi + ölçüm**
birleşimidir. Bu proje o birleşimin şeffaf, çalışan ve herkesin katkı verebileceği
bir iskeletini sunar. Gerçek AGI, açık topluluğun uzun vadeli eseridir.
