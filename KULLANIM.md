# Kullanım Rehberi

Bu rehber, projeyi **kullanan** biri için yazılmıştır. Kod geliştirmeyle ilgilenmenize
gerek yok — sadece açın ve yazın.

---

## 1. İlk açılış

Masaüstünde şu kısayola çift tıklayın:

```
Çalıştır-GUI.vbs
```

Hiçbir terminal penceresi açılmaz. Birkaç saniye sonra **Yerel Ajan** penceresi gelir.
Başlıkta `AGI • DENEYSEL V1` yazısını görmelisiniz.

> İlk açılışta model GPU'ya yüklenirken cevap biraz gecikebilir. Sonraki sorular hızlıdır.

Eğer "Bağlantı hatası" görürseniz: Ollama açık değil demektir. Bir konsolda
`ollama serve` yazıp açık bırakın, sonra ajanı yeniden başlatın.

---

## 2. Nasıl soru sorarsınız?

**Alt kısımdaki kutuya Türkçe yazın ve Enter'a basın.** Komut öğrenmenize gerek yok.

Örnekler:

| Yazdığınız | Olan |
|---|---|
| `merhaba, nasılsın?` | Samimi sohbet cevabı |
| `2+2*3 kaç eder?` | Hesap yapılır, sonuç açıklanır |
| `readme.md dosyasını oku` | Dosya aracı kendiliğinden çalışır, içerik gelir |
| `saat kaç?` | Sistem saati söylenir |
| `Java'da merhaba yazdıran kod yaz` | Kod üretilir, isterseniz çalıştırılır |
| `yapay zeka nedir, wikipedia'da ara` | Web'den bilgi getirilir |
| `klasördeki dosyaları listele` | Klasör içeriği gelir |

Ajan, ihtiyaç duyarsa **kendiliğinden** hesap makinesini, dosya aracını, saat aracını,
web aramasını veya kod çalıştırıcıyı çağırır. Sizin bir şey açmanıza gerek yok.

### Zor sorular

Kısa/günlük sorularda **hızlı model** kullanılır. Uzun, çok adımlı veya "neden/analiz/kanıtla"
içeren sorularda ajan **daha güçlü akıl modeline** kendiliğinden geçer (biraz daha yavaş ama
daha isabetli). Bu otomatiktir, ayar yapmanız gerekmez.

### Metin kesilmez

Uzun bir cevap isterseniz ("150 kelimelik bir açıklama yaz" gibi), cevap yarım kalırsa
ajan kaldığı yerden devam eder. Sizin "devam et" demenize gerek yok.

---

## 3. Görüntü ile çalışma

İki yol var:

1. **⊕ GÖRÜNTÜ** butonuna basın → dosya seçin → "Bu resimde ne var?" sorusunu yazın
2. Görüntüyü doğrudan **sohbet alanına sürükleyip bırakın** → sorunuzu yazın

Gemma 4 görüntüyü inceler ve Türkçe cevap verir.

---

## 4. Arayüzü tanıyın

### Üst kısım
- **YEREL AJAN** başlığı ve `AGI • DENEYSEL V1` rozeti
- Animasyonlu neon çizgi — canlı çalıştığını gösterir

### Sol panel — Katman kontrolleri

| Anahtar | Ne yapar |
|---|---|
| **Planner** | Görevi alt adımlara böler (karmaşık işlerde açık kalsın) |
| **Reflection** | Cevabı kontrol edip düzeltir |
| **Çok Ajan** | Uzman ajanları devreye sokar (yavaş ama güçlü) |
| **Dünya Modeli** | Nedensel tahmin ve "ya öyle olmasaydı?" analizi |
| **Özerklik** | Ajan kendi hedefini üretebilir |
| **Hızlı Mod** | Hızlı cevap için tek model çağrısı (önerilen) |

### Sol panel — Sekmeler

| Sekme | İçerik |
|---|---|
| **Bilgi** | AGI profili, öz-model, çekirdek, bağışıklık, performans, model zinciri — hepsi tek sayfada |
| **Bellek** | Hafızaya kaydedilen anılar |
| **Grafik** | Bildiği varlıklar ve ilişkiler |
| **Hedefler** | Özerk hedeflerin durumu |
| **Metin** | Son metin analizinin sonucu |
| **Bağışıklık** | Zararlı içerik taraması ve karantina durumu |
| **Araçlar** | Kullanılabilir araçların listesi |
| **Kaynaklar** | Araştırma URL'leri (+ URL ekle butonu) |
| **Log** | Arka planda olan biten (yönlendirme, araç çağrıları) |

### Alt durum çubuğu

Aktif model, akıl modeli, açık katmanlar, hafıza sayısı ve dünya modeli durumu
sürekli görünür.

---

## 5. Kısayollar

| Kısayol | İşlev |
|---|---|
| `Ctrl + N` | Yeni oturum |
| `Ctrl + S` | Sohbeti metin dosyasına kaydet |
| `Ctrl + L` | Ekranı temizle |
| `Ctrl + O` | Görüntü seç |
| `↑` / `↓` | Önceki mesajlarda gezin |

---

## 6. Sık sorulan sorular

**AGI mi bu?**
Hayır. Deneysel V1 yetenek profili — planlayan, hatırlayan, araç kullanan gelişmiş bir
ajan. Gerçek AGI için ölçek, kendi modelini eğitme ve dünya etkileşimi gerekir.

**Neden bazen yavaş?**
Zor sorularda daha büyük akıl modeli devreye girer. Bu normaldir. Hızlı mod açıksa
günlük sorular hızlı cevaplanır.

**Kişisel verilerim nereye gidiyor?**
Hiçbir yere. Her şey bilgisayarınızda, Ollama yerel olarak çalışır. İnternet yalnızca
web araması yaptığınızda kullanılır.

**Bir şey ters giderse?**
`Log` sekmesine bakın; hangi aracın çağrıldığını ve hatayı gösterir. Gerekirse yeni oturum
açın (`Ctrl+N`).

---

## 7. Geliştiriciler için (isteğe bağlı)

Sohbet kutusuna `/` ile başlayan gizli komutlar yazılabilir; kullanım için gerekli değildir:

```
/agi        yetenek profili raporu
/eval       14 testi çalıştır
/bench      6 model karşılaştırma sorusu
/models     kurulu modeller
/brain      DIDL beynin durumu
/immune     bağışıklık raporu
/meta       öğrenmeyi öğrenme raporu
/research <url>   URL'den bilgi öğren
```

Detaylar: [README.md](README.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
