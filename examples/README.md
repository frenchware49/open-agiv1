# Örnek Oturumlar

Aşağıdaki komutlar `Çalıştır-Ajan.bat` (veya `build-agent.bat`) açıldıktan sonra yazılabilir.

## 1. Temel kullanım

```
Sen> merhaba
  [Yerel Ağ] niyet=sohbet  güven=1.00
Ajan> Merhaba! Nasıl yardımcı olabilirim?

Sen> 2+2*3 kaç eder
  [Yerel Ağ] niyet=matematik  güven=1.00
  [Araç] calculator -> 8.0
Ajan> 2+2*3 = 8 eder.

Sen> saat kaç
  [Yerel Ağ] niyet=bilgi  güven=1.00
  [Araç] clock -> 19.11.2026 Perşembe 14:08:04
Ajan> Şu an 14:08, Perşembe 19 Kasım 2026.
```

## 2. Sürekli öğrenme

```
Sen> /teach bilgi trafik durumu nasıl
  [Öğrenme] Öğrenildi. Bu metin artık 'bilgi' olarak sınıflanıyor (güven 1.00).

Sen> /learnstatus
  Öğrenilen örnek: 1 | Öğrenilenlerde doğruluk: %100.0 | Genel doğruluk: %100.0
```

## 3. Dünya modeli (çok adımlı akıl yürütme)

```
Sen> /remember Ahmet Python seviyor
  [Dünya modeli] 1 olgu çıkarıldı ve grafiğe eklendi.

Sen> /remember Python programlama dili, programlama dili yazılım geliştirmede kullanılır
  [Dünya modeli] 2 olgu çıkarıldı ve grafiğe eklendi.

Sen> /path Ahmet yazılım
  Ahmet -> (seviyor) Python -> (ilgili) Python programlama dili -> (kullanılır) yazılım geliştirme
```

## 4. Özerk hedefler

```
Sen> /goal saati öğren ve 2+2 kaç olduğunu bul
  [ ] 1. P1  2+2'i hesapla
  [ ] 2. P1  Saati kontrol et

Sen> /run 2
  [Özerk] Hedef 1 çalıştırılıyor: 2+2'i hesapla
  [Araç] calculator -> 4
  [Özerk] Hedef 2 çalıştırılıyor: Saati kontrol et
  [Araç] clock -> 19.11.2026 Perşembe 14:08:04
  [x] 1. P1  2+2'i hesapla
  [x] 2. P1  Saati kontrol et
```

## 5. Çok ajan modu

```
Sen> /mode multi
Sen> 2+2 kaç eder ve saat kaç
  [Yerel Ağ] niyet=matematik güven=0.95
  [Mod] Multi-agent (uzmanlar devrede)
  [Multi] adım 1 → Hesaplayıcı: 2+2 kaç eder
  [Multi] adım 2 → Araştırmacı: saat kaç
  [Multi] Yazar birleştirdi, denetçi kontrol ediyor...
Ajan> 2+2 = 4 ve şu an saat 14:08.
```

## 6. Değerlendirme

```
Sen> /eval
=== Değerlendirme ===
[1] Niyet sınıflandırma (görülmemiş ifadeler)
  PASS 3 çarpı 5 kaç yapar -> matematik
  ...
[2] Hesap makinesi
  PASS 2+2*3 -> 8
  ...
SONUÇ: 14/14 geçti (%100)
```
