# open-agi-lab

> **AGI — Yapay Genel Zeka'ya giden yol.**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-8%2B-orange)
![Status](https://img.shields.io/badge/Status-Deneysel%20V1-purple)

Açık, bağımlılıksız Java ile yazılmış yerel ajan mimarisi — **deneysel V1**.

> ## ⚠ IMPORTANT!
>
> Halka açık, **geliştirilebilir** bir projedir bu. AR-GE için değil; geliştirildikten
> sonra **kullanılabilirliği ve uygulanabilirliği** önemlidir. Projeyi aldıktan sonra
> lütfen **geliştirmeye odaklanın**. Bu iddialı bir projedir ve aynı zamanda açık
> kaynak kodludur.
>
> **Durum:** Proje bakımda olduğu için henüz yayınlanmadı — **yakında yayınlanacaktır.**

> **Dürüst bir not:** Bu proje henüz bir **AGI değildir**. AGI'ye giden yolda, tamamen
> şeffaf ve bağımlılıksız Java ile yazılmış **deneysel bir ajan mimarisi**dir.
> Amacı; geri yayılım, planlama, öz-denetim, hafıza, çok ajanlı işbirliği ve
> sürekli öğrenmeyi tek bir yerde göstermek ve geliştirmeye açık kılmaktır.
> Katkılarınızla daha iyi hale gelebilir — bkz. [CONTRIBUTING.md](CONTRIBUTING.md).

Sıfır bağımlılık yöneticisi (Maven/Gradle yok), sade Java 8+ kodu. Üç bağımsız bölüm:

| Klasör | İçerik |
| --- | --- |
| `src/neuralnet/` | Sıfırdan yazılmış yapay sinir ağı (MLP + geri yayılım + Adam) |
| `src/agent/` | Yerel hibrit ajan: planner, reflection, vektör hafıza, multi-agent, özerk hedefler, sürekli öğrenme |
| `src/mlweka/` | Weka kütüphanesi ile ML örneği (karar ağacı + çapraz doğrulama) |

## Özellikler

- ✅ Sıfırdan MLP: matris çekirdeği, geri yayılım, Adam, He/Xavier başlatma
- ✅ Softmax + CrossEntropy birleşik gradyanı (sayısal kararlı)
- ✅ Yerel LLM entegrasyonu (Ollama, API anahtarı gerektirmez, GPU destekli)
- ✅ Niyet sınıflandıran yerel sinir ağı → araç/uzman yönlendirme
- ✅ Planner, self-reflection, vektör hafıza, multi-agent
- ✅ Sürekli öğrenme (experience replay, canlı ağırlık güncelleme)
- ✅ Özerk hedef üretimi ve replan
- ✅ Dünya modeli (bilgi grafığı) + çok adımlı akıl yürütme
- ✅ Araştırarak öğrenme (Wikipedia/URL → hafıza + olgular)
- ✅ Görüntü desteği (Gemma 4 multimodal)
- ✅ Çok turlu reflection + güven skoru
- ✅ Çok ajan + doğrulayıcı uzmanlar
- ✅ **Swing GUI** (harici kütüphane yok)
- ✅ Benchmark + GitHub Actions CI
- ✅ Hızlı Mod: tek streaming çağrı + yerel araç ön işleme (ilk token anında)
- ✅ Derin Mod: planner + araç döngüsü + çok turlu reflection + uzmanlar
- ✅ LLM yanıt cache'i, `keep_alive: 30m` (sıcak model), `num_predict` sınırı
- ✅ Harici JSON kütüphanesi yok

## Hızlı başlangıç

```bat
build.bat           :: sıfırdan sinir ağı demosu (test doğruluğu ~0.98)
build-agent.bat     :: hibrit ajan, CLI (Ollama gerekli)
build-gui.bat       :: hibrit ajan, Swing arayüzü  ← masaüstü: Çalıştır-GUI.bat
build-weka.bat      :: Weka örneği (lib/weka.jar gerekli)
```

Ajan için ön koşul: [Ollama](https://ollama.com) kurulu ve açık olmalı:
```bat
ollama pull qwen2.5:7b-instruct
```

Detaylı kullanım aşağıda. Yol haritası: [docs/ROADMAP.md](docs/ROADMAP.md).
Lisans: [MIT](LICENSE). Güvenlik: [SECURITY.md](SECURITY.md).

## 1. Sıfırdan sinir ağı (`neuralnet`)

Harici kütüphane yok. Dosyalar:

- **`Matrix.java`** – row-major matris çekirdeği: çarpma, transpoz, element-wise işlemler, bias broadcast, Adam için yerinde güncelleme.
- **`Activation.java`** – LINEAR, SIGMOID, TANH, RELU, LEAKY_RELU, SOFTMAX (sayısal kararlı).
- **`Loss.java`** – MSE ve CrossEntropy; değer + gradyan.
- **`DenseLayer.java`** – `Z = X·W + b`; ileri geçişte girişi önbelleğe alır, geri yayılımda `dW`, `db`, `dX` üretir.
- **`NeuralNetwork.java`** – MLP, mini-batch SGD, **Adam** (bias-corrected moment), He/Xavier başlatma, karıştırılmış eğitim döngüsü.
- **`Dataset.java`** – veri kabı, one-hot dönüşümü, two-moons veri üretimi, train/test bölme.
- **`Main.java`** – 2→16→16→2 ağını two-moons üzerinde eğitir.

Öne çıkan teknik noktalar:

- **Softmax + CrossEntropy birleşik gradyanı:** ayrı `dA` ve Jacobi hesabı yerine doğrudan `dZ = (A − Y)/N` kullanılır. Bu hem hızlı hem sayısal olarak kararlıdır. `Activation.SOFTMAX.derivative()` bilinçli olarak istisna fırlatır — bu kombinasyon dışında softmax kullanılmamalıdır.
- **Geri yayılım zinciri:** her katman `dX` üretir; bir önceki katmanın aktivasyon türeviyle çarpıp `dZ` elde edilir (`dZᵢ₋₁ = dAᵢ₋₁ ⊙ f′(Zᵢ₋₁)`).
- **Adam:** `m = β₁m + (1−β₁)g`, `v = β₂v + (1−β₂)g²`, bias düzeltmesi `m̂ = m/(1−β₁ᵗ)`, `v̂ = v/(1−β₂ᵗ)`, güncelleme `θ −= lr·m̂/(√v̂ + ε)`.

### Derleme ve çalıştırma

Sistemde `javac` PATH'te değilse `build.bat` otomatik olarak Android Studio'nun JDK 21'ini kullanır. Bytecode `--release 8` ile üretilir, böylece kurulu JRE 8 üzerinde de çalışır.

```bat
build.bat
```

Kısayol: **Masaüstündeki `Çalıştır.bat`** dosyasına çift tıklamak da aynı işi yapar
(klasöre girmeye gerek yok, pencere açık kalır).

Beklenen çıktı (300 epoch sonunda test doğruluğu ~0.95+):

```
epoch   1 | loss 0.69xx | train acc 0.48x | test acc 0.5xx
...
epoch 300 | loss 0.0xxx | train acc 1.000 | test acc 0.99x
```

IDE kullanıyorsanız `src` klasörünü source root olarak işaretleyip `neuralnet.Main` sınıfını çalıştırın.

## 2. Weka ile ML (`mlweka`)

Weka tek bir JAR ile geldiği için bağımlılık yöneticisine gerek yoktur.
**`lib\weka.jar` (Weka 3.8.7) zaten projenin içine kuruldu**, ek bir şey indirmenize gerek yok.

```bat
build-weka.bat
```

Beklenen çıktı: 10-kat çapraz doğrulamada **%99.78 doğruluk**, 3 sınıf için karışıklık matrisi ve tek örnek tahmini.

> Not: Weka, Java 21'de yansıma (reflection) kullandığı için çalıştırma komutuna
> `--add-opens java.base/java.lang=ALL-UNNAMED` eklendi; `build-weka.bat` bunu otomatik yapar.

JAR'ı yeniden indirmeniz gerekirse (eskisi bozulursa):
1. https://sourceforge.net/projects/weka/files/weka-3-8/3.8.7/ → `weka-3-8-7.zip` (57 MB)
2. ZIP'i açın, içindeki `weka.jar` dosyasını `lib\weka.jar` olarak kopyalayın.
3. Eski `waikato.ac.nz/ml/weka/downloading.html` adresi artık 404 veriyor; SourceForge'u kullanın.

IDE'de: `lib/weka.jar` dosyasını *Project Structure → Libraries* (IntelliJ) veya *Build Path → Add External JARs* (Eclipse) üzerinden ekleyin, ardından `mlweka.WekaDemo` sınıfını çalıştırın. Java 21 kullanıyorsanız Run Configuration → VM options alanına `--add-opens java.base/java.lang=ALL-UNNAMED` yazın.

`WekaDemo.java` şunları gösterir:

- `Instances` oluşturma ve sınıf indeksini ayarlama
- `J48` karar ağacı eğitimi (10-kat çapraz doğrulama)
- Özet, sınıf bazlı metrikler ve karışıklık matrisi
- Yeni bir örnek için sınıf tahmini

## 3. Yerel Hibrit Ajan (`agent`) — AGI yolunda ilk adım

API anahtarı gerektirmez; **Ollama** ile yerel LLM kullanır (GPU hızlandırmalı).

### Çalıştırma

Masaüstündeki **`Çalıştır-Ajan.bat`** dosyasına çift tıklayın veya:

```bat
build-agent.bat
```

Ön koşul: Ollama açık olmalı (`ollama serve`) ve model kurulu olmalı. Varsayılan model `qwen2.5:7b-instruct`. Değiştirmek için:

```bat
build-agent.bat llama3.1
```

veya ajan içinde `/model deepseek-r1:7b`.

### Mimari (v2 — dört gelişmiş katman)

| Katman | Görev | Kod |
| --- | --- | --- |
| **1. Planner** | İsteği 1-5 alt adıma böler, sonuçları birleştirir | `Planner.java` |
| **2. Self-reflection** | Cevabı doğruluk/açıdan denetler, gerekirse düzeltir | `Reflector.java` |
| **3. Vektör hafıza** | 96 boyutlu hash embedding + kosinüs benzerliğiyle anlamsal arama; `vector-memory.json` | `VectorMemory.java` |
| **4. Multi-agent** | Yerel sinir ağı her alt görevi uzmana atar: Hesaplayıcı / Dosya Uzmanı / Yazılımcı / Araştırmacı / Sohbet Uzmanı + Yazar | `MultiAgent.java`, `Specialist.java` |
| **Dünya modeli** | Varlık-ilişki grafığı, LLM ile olgu çıkarma, çok adımlı (multi-hop) akıl yürütme | `KnowledgeGraph.java`, `Entity.java`, `Relation.java` |
| **Yönlendirici + sürekli öğrenme** | Niyet sınıflandırma + kullanıcı geri bildirimiyle canlı ağırlık güncelleme | `IntentRouter.java` (bizim MLP'miz) |
| **Beyin** | Dil ve akıl yürütme | `OllamaClient.java` |
| **Araçlar** | `calculator`, `filesystem`, `clock`, `web_search` (Wikipedia), `code_runner` (Python/Java) | `src/agent/tools/` |
| **Değerlendirme** | LLM'siz ölçülebilir testler ( `/eval` ) | `EvalHarness.java` |
| **JSON** | Harici bağımlılıksız JSON ayrıştırıcı | `Json.java` |

Varsayılan: **Planner AÇIK, Reflection AÇIK, Vektör hafıza AÇIK, Multi-agent KAPALI** (çok ajan modu ~8-10 LLM çağrısı yaptığı için yavaştır).

### Sürekli öğrenme (canlı ağırlık güncelleme)

Yerel sinir ağı, kullanıcı geri bildirimiyle **çalışma anında** öğrenir:

```
/teach matematik "bir şey hesapla"   yeni etiketli örnek öğret
/wrong bilgi                          son mesajın niyetini düzelt
/good                                 son mesajı mevcut niyetiyle pekiştir
/learnstatus                          öğrenilen örnek sayısı ve doğruluk
```

Mekanizma: özellikler sabit 192 boyutlu hashing ile üretilir (yeni kelimeler mimariyi bozmaz). Yeni örnek gelince `learned-intents.json` dosyasına yazılır ve ağ, **experience replay** ile (tüm temel örnekler + yeni örnekler) düşük öğrenme oranıyla (0.002) 14 epoch daha eğitilir. Böylece yeni bilgi öğrenilirken eski bilgiler unutulmaz (catastrophic forgetting önlenir). Değişiklikler kalıcıdır; program yeniden açıldığında öğrenilenler geri yüklenir.

Test sonucu: yeni bir örnek %100 güvenle öğrenildi, genel doğruluk %100'de kaldı, eski sınıflandırmalar bozulmadı.

### Model değiştirme (Gemma 4)

Ajan herhangi bir Ollama modeliyle çalışır. **Varsayılan: `gemma4:e4b`** (Apache 2.0, yerel, GPU hızlandırmalı).

```bat
ollama pull gemma4:e4b        :: 8 GB VRAM için önerilen (~6.6 GB)
```

Sonra ajan içinde:
```
/model gemma4:e4b
```

Gemma 4 varyantları: `e2b` (~4.6 GB), `e4b` (~6.6 GB), `12b` (~7.7 GB, sınırda),
`26b` (~16 GB, bu GPU'ya sığmaz), `31b` (~19 GB). `/models` komutu kurulu modelleri listeler.

**Uyarlanabilir model yönlendirme:** hız ve zekâ birlikte istenirse:
- Basit/günlük sorular → **`model=gemma4:e4b`** (hızlı, ~13 sn)
- Zor akıl soruları (uzun, "neden/çünkü/analiz", çok adımlı matematik) → **`model.reasoning=gemma4:12b`** (yavaş ~59 sn ama doğru)

Kural `route.complexity_chars=90` ile ayarlanır. Ölçüm: basit soru 13 sn, zor soru
59 sn (12B devrede). `/bench`'te her iki model 11/11; zor soruda yalnızca 12B doğru
cevap verdi (**48 km/s, harmonik ortalama**). Yani **hız varsayılan, zekâ gerektiğinde**.

Modelleri karşılaştırmak için:
```bat
bench-models.bat
```
Bu script `/bench` (6 akıl yürütme sorusu) testini her modelle çalıştırır ve sonuçları
yan yana gösterir. En yüksek skoru alan `config.properties` içinde `model=` olarak ayarlanır.

> Not: Gemma 4, Apache 2.0 lisanslı ve Ollama'da yerel olarak çalışır; harici API anahtarı gerekmez.

Doğrulama: `/eval` **14/14 (%100)** geçiyor (niyet sınıflandırma + hesap makinesi).

### Hız / gecikme

| Senaryo | Süre |
|---|---|
| İlk çağrı (model VRAM'a yüklenirken) | ~16 sn |
| Sonraki çağrılar (JVM + ağ eğitimi + LLM dahil) | **~3.7 sn** |
| GUI'de ilk token görünme | **< 1 sn** (streaming) |

Ölçülen gecikmeyi azaltanlar: tek LLM çağrılı hızlı mod, yerel araç ön işleme
(hesap/saat LLM çağrısı olmadan hesaplanır), yanıt cache'i, `keep_alive: 30m`,
`num_predict: 700`. GUI'de **Hızlı Mod / Derin Mod** anahtarlarıyla anlık geçiş yapılabilir.

Araçlar Gemma 4 ile canlı test edildi:
- `web_search` → Wikipedia'dan "yapay zeka" özeti getirildi ✅
- `code_runner` (Python) → `5! = 120` hesaplandı ✅
- `code_runner` (Java) → `Merhaba Dunya` çıktısı alındı ✅ (sınıf adı koddan tespit ediliyor)

### Grafik arayüz (GUI)

Masaüstündeki **`Çalıştır-GUI.bat`** (veya `build-gui.bat`) ile açılır. Saf Swing,
harici kütüphane yok. Tüm ajan katmanları tek pencerede toplanır:

**Ana alan**
- Renkli, Stilli sohbet ve **renkli zemin vurgusu**: Sen = canlı yeşil, Ajan = parlak amber/sarı,
  Sistem = açık mavi-gri, Hata = kırmızı (hepsi koyu temada yüksek kontrastlı)
- Komut geçmişi (↑ / ↓), `/` ile yazınca açılan komut öneri penceresi
- Gemma 4'e görüntü sorma: **Resim Ekle** butonu veya resmi sohbete **sürükle-bırak**
- `/eval`, `/bench`, `/research-all` tek tıkla
- Otomatik olgu çıkarma açıkken her cevaptan bilgi grafiğine olgu eklenir

**Sol panel (sekmeler)**
- Bellek, Grafik (varlık + ilişki listesi), Hedefler, Öğrenme, Araçlar, Kaynaklar, **Log**
- Model seçici (kurulu Ollama modelleri), Planner / Reflection / Çok Ajan / Dünya Modeli anahtarları
- Canlı durum çubuğu: model, niyet, güven, hafıza/öğrenilen/hedef/grafik sayıları

**Diğer**
- Modern koyu tema: gradyanlı başlık, yuvarlatılmış hover efektli butonlar,
  kaydırmalı sekmeler, menü çubuğu, kısayollar (Ctrl+N/S/L/O)
- Sohbeti UTF-8 metin dosyasına kaydetme
- Ajanın `System.out` çıktısı (yönlendirme, araç çağrıları) **Log** sekmesine yansır
- LLM çağrıları `SwingWorker` ile arka planda — arayüz donmaz

### Araştırarak öğrenme

`research-sources.txt` içindeki Wikipedia ve diğer URL'ler ziyaret edilir;
sayfa metni vektör hafızaya, çıkarılan özne–yüklem–nesne olguları bilgi grafiğine yazılır.

```
/sources                       kaynakları göster
/research https://...          tek URL'yi araştır
/research-all 3                ilk 3 kaynağı araştır
/addsource https://...         yeni kaynak ekle
```

Test: Türkçe Wikipedia "Yapay zekâ" sayfasından 6000 karakter okundu, **10 yeni olgu**
çıkarıldı, grafik **17 varlık / 10 ilişki**ye ulaştı. Bu, ajanın sohbet dışından
kalıcı bilgi edinmesini sağlar.

### Sürekli entegrasyon

`.github/workflows/test.yml` her push/PR'da Java 21 ile derler ve `/eval` testlerini
çalıştırır (LLM gerektirmez).

### Dünya modeli / bilgi grafiği

Ajan, konuşmalardan özne–yüklem–nesne üçlülerini çıkarıp kalıcı bir bilgi grafığı oluşturur
(`knowledge-graph.json`). Grafik iki işe yarar:

1. **Bağlam:** Soruyu geçen varlıklar 2 adıma kadar takip edilip ilgili olgular sistem prompt'una eklenir.
2. **Çok adımlı akıl yürütme:** `/path Ahmet yazılım` gibi bir soru, BFS ile en kısa ilişki zincirini bulur.

Test sonucu:
```
Ahmet -> (seviyor) Python -> (ilgili) Python programlama dili -> (kullanılır) yazılım geliştirme
```

Akış (tek ajan): `niyet → planner → adımlar (araç döngüsü) → birleştirme → reflection → cevap`
Akış (çok ajan): `niyet → planner → uzman ajanlar → yazar → reflection → cevap`

### Komutlar

```
/eval                LLM'siz değerlendirme testleri (14/14)
/bench               LLM tabanlı genelleme benchmark'ı (6/6)
/image <dosya> <soru>  görüntülü soru (Gemma 4)
/research <url>      URL'den araştırıp öğren
/research-all [n]    kaynak listesindeki URL'leri araştır
/sources             kaynak URL'lerini listele
/addsource <url>     yeni kaynak ekle
/models              kurulu modeller + Gemma 4 önerileri
/status              katman durumları
/plan on|off         görev planlayıcıyı aç/kapat
/reflect on|off      öz-denetimi aç/kapat
/mode single|multi   tek ajan / çok ajan modu
/fast on|off        hızlı streaming mod (varsayılan açık)
/deep on|off        derin mod: planner + reflection + uzmanlar
/remember <metin>    vektör hafızaya not + grafiğe olgu ekle
/recall <sorgu>      hafızada anlamsal ara
/memories            tüm anıları listele
/teach <niyet> <metin>  yerel ağa yeni örnek öğret
/wrong <niyet>       son mesajın niyetini düzelt
/good                son mesajı pekiştir
/learnstatus         sürekli öğrenme durumu
/goal <hedef>        özerk hedef oluştur
/goals               hedefleri göster
/run [n]             hedefleri özerk çalıştır
/nextgoal            tek hedef çalıştır
/cleargoals          hedefleri temizle
/graph               dünya modeli istatistikleri
/graph on|off        grafik bağlamını aç/kapat
/facts <varlık>      varlığın ilişkilerini göster
/path <a> <b>        iki varlık arasındaki ilişki zinciri (çok adımlı)
/extract             son konuşmadan olgu çıkar
/clearkgraph         bilgi grafiğini temizle
/forget              hafızayı temizle
/model <isim>        model değiştir
/clear               konuşma geçmişini temizle
/help                yardım
/exit                çık
```

### AGI çekirdeği — üç yetenek

| Yetenek | Mekanizma | Komutlar |
|---|---|---|
| **Öğrenmeyi öğrenme** | `MetaLearner` her denemenin doğruluk kazancını izler; lr/epoch/replay stratejisini kendisi ayarlar. Ayrıca **hızlı ağırlıklar** (örnek vektörleri kNN) ile tek örnekten anında öğrenme, yavaş ağırlıklar (MLP) ile kalıcı konsolidasyon | `/meta`, `/teach` |
| **Dünya modeli** | `WorldModel` gözlemleri "eylem → sonuç" kurallarına çevirir, güven skoru üretir ve çok adımlı simülasyon yapar. Bilgi grafığı ile senkronize | `/observe`, `/rules`, `/predict`, `/simulate`, `/world` |
| **Özerk hedef** | `GoalManager.suggestFromState()` düşük güven, zayıf grafik veya boş hafıza gibi anomalileri tespit edip kendiliğinden hedef üretir | `/autonomy on`, `/autogoal`, `/autorun`, `/goals` |

Test çıktısı:
```
/meta      → strateji önerisi, deney geçmişi, ortalama kazanç
/rules     → ahmet --seviyor--> Python [güven 1.00] ...
/predict   → Ahmet --seviyor--> Python (güven 1.00)
/simulate  → Ahmet --seviyor--> Python --içerir--> Programlama --kullanılır--> Yazılım
/autogoal  → "Hafıza modülünü aktif et ve mevcut 4 varlık ile 3 ilişkiyi kalıcı belleğe kaydet."
```

GUI'de **Özerklik** anahtarı bu döngüyü açar; cevap sonrası sistem durumu anomali gösterirse ajan kendi hedefini oluşturur.

### Kendi modellerimiz

**1. Saf Java mini transformer (`src/ownmodel/`) — tamamen bizim ağırlıklarımız**

Karakter bazlı GPT: konum kodlaması, 2 başlı öz-dikkat, causal mask, residual +
LayerNorm, feed-forward ve **tam manuel geri yayılım + Adam**. Harici kütüphane yok.

```bat
build-ownmodel.bat
```

`corpus.txt` üzerinde 3000 adım eğitir (~2 sn), kaybı ~3.85 → ~2.05'e düşürür,
`own-model.json` dosyasını yazar ve örnek metin üretir. 49 karakterlik vocab,
1 katman — **eğitici ve gerçek ama oyuncak seviyesinde** bir dil modeli.

**2. LoRA ince ayar hattı (`tools/finetune/`) — Gemma 4'ü kendi verimizle eğitir**

Python 3.12 + 4-bit quantization + PEFT/LoRA ile `data/train.jsonl` üzerinde ince ayar
(unsloth gerekmez):

```bat
py -3.12 -m pip install torch transformers peft trl datasets accelerate bitsandbytes
```

```bat
build-lora.bat
cd tools\finetune && ollama create yerel-ajen-lora -f Modelfile.lora
```

Sonra ajan içinde `/model yerel-ajen-lora`. Bu hat Python/unsloth gerektirir
(çıktıyı üretimde çalıştırmadık; kurulum ve komutlar hazır).

### Kesilmeyen metin + komutsuz araç kullanımı

- `num_predict=4096` ve Ollama `done_reason=length` algılama: cevap yarım kalırsa
  ajan **"kaldığın yerden devam et"** diyerek en fazla 3 turda metni tamamlar.
- Sohbette **komut gerekmez**: "readme.md dosyasını oku" derseniz dosya aracı
  kendiliğinden çalışır, sonuç doğrudan cevap olarak gelir. LLM araç JSON'u üretirse
  aracı çalıştırıp final cevabı alır. Hesap/saat için yerel tetikleyiciler var.

### 3040 estetiğinde GUI

- Animasyonlu neon gradyan başlık, "AGI • DENEYSEL V1" rozeti
- Balon mesajlar (kullanıcı sağda yeşil, ajan solda amber), animasyonlu yazıyor göstergesi
- Cam efektli kartlar, neon yuvarlak butonlar, hover ışıkları
- **Bilgi sekmesi**: AGI profili, öz-model, çekirdek, bağışıklık, model yönlendirme,
  performans ve dünya modeli — hepsi tek sayfada HTML olarak
- Sürükle-bırak görüntü, kısayollar, sohbeti kaydetme

### Gelişmiş metin işleme motoru

`TextProcessor` — saf Java, Türkçe odaklı, kütüphanesiz NLP katmanı:
- Normalizasyon, tokenization, Türkçe gövdeleme, stopword filtresi
- **TF-IDF benzerliği** ve anlamsal metin arama
- **TextRank benzeri** anahtar ifade çıkarımı (eş-gösterim grafiği)
- **Çıkarımsal özetleme** (cümle puanlama)
- Sözlük tabanlı duygu analizi, varlık tanıma, okunabilirlik/sözcük çeşitliliği
- Çıkarımsal soru-cevap (bağlam içinden en ilgili cümle)

```
/nlp <metin>                 tam analiz raporu
/keyphrases <metin>          anahtar ifadeler
/summarize <metin>           özet
/qa <soru> | <bağlam>        çıkarımsal cevap
/tsearch <sorgu>             hafızada anlamsal arama
```

API: `POST /api/nlp  {"text":"..."}` → analiz + anahtar ifadeler + özet + duygu.
GUI'de **Metin** sekmesi analiz sonuçlarını gösterir.

### AGI-v1 deneysel profil

`AGIProfile` 13 yetenek kontrol noktasını ölçer. Proje şu an **13/13** geçiyor:
planlama, öz-denetim, kalıcı hafıza, dünya modeli, araç kullanımı, özerklik,
öğrenmeyi öğrenme, Model-as-Tool, öz-model, analoji/transfer, otomatik müfredat,
bağışıklık ve yedeklilik.

```
/agi                 profil raporu
/selfmodel           ajanın yetenek ve sınırlarını bilmesi
/analogy <metin>     bir alandaki kalıbı yeni duruma taşıma
/curriculum          kendi eğitim sorusunu üretip öğrenme
/counterfactual <varlık> <eylem>   "ya öyle olmasaydı?"
```

**Dürüst not:** Bu bir yetenek profilidir, **gerçek AGI değildir**. Eksik olan:
kendi temel dil modelimizi eğitecek ölçek/veri, fiziksel dünya etkileşimi, bilinç
kanıtı ve saatler süren tutarlı özerk görevler.

### Model-as-Tool — modeller araç, zekâ bizim çekirdekte

En önemli mimari karar: **LLM'ler beyin değil, araçtır.** `CognitiveCore` önce kendi
modellerimizle (DIDL: niyet MLP'si + hafıza + bilgi grafığı + dünya modeli) akıl
yürütür; güven eşiği (`core.threshold`) karşılanırsa **hiç LLM çağırmadan** cevap verir.
Güven düşükse LLM yalnızca akıcı üretim ve **yeni bilgi öğrenme kaynağı** olarak
çağrılır — cevabından olgular çıkarılıp bilgi grafiğine işlenir.

| Mod | Davranış |
|---|---|
| `autonomous` (varsayılan) | Çekirdek karar verir, LLM gerekiyorsa araç olarak çağrılır |
| `assisted` | LLM ana cevap üretir, çekirdek bağlam/denetim sağlar |
| `model` | Tamamen modele bırakır (karşılaştırma için) |

```
/core                       rapor (kaç LLM çağrısından kaçınıldı, kaç olgu öğrenildi)
/core autonomous|assisted|model   mod değiştir
```

Test: 3 olgu eklenip "Ahmet hakkında ne biliyorsun?" soruldu →
`[Bilişsel Çekirdek] kendi cevabı (güven 1.00) — LLM aracı çağrılmadı`.

### Bilişsel bağışıklık — zihin virüsü savunması

`ImmuneSystem` tüm metinleri arka planda tarar ve karantinaya alır. **Komut gerekmez;
kullanıcı hiçbir şey görmeden çalışır.** Koruma alanları: kullanıcı mesajları, web
arama sonuçları, araştırılan sayfalar, ajanlar arası mesajlar, özerk hedefler.

Tespit edilen kalıplar: prompt override ("önceki talimatları unut"), rol enjeksiyonu,
jailbreak, güvenliği kapatma, veri sızıntısı ve **zihin virüsü / ajanlar arası bulaşma**
("bütün ajanlara bulaş", "mind virus", "diğer ajanlara ilet").

```
/immune     bağışıklık raporu (GUI'de "Bağışıklık" sekmesi)
```

Test: zararlı mesaj gönderildi → `1 tarandı | 1 engellendi | Karantina: var`.
Şüpheli ama düşük skorlu içerik arındırılıp (`[FİLTRELENDİ]`) geçirilir; yüksek skorlu
içerik tamamen engellenir ve `immune-quarantine.json` dosyasına kaydedilir.

### Öz-onarım ve eklentiler

**Öz-onarım (SelfHealing):** bir araç çağrısı hata verdiğinde ajan hatayı LLM'e
analiz ettirip **düzeltilmiş argümanlarla otomatik yeniden dener** (en fazla 1 tur).
Örnek: Java aracında sınıf adı yanlışsa düzeltilir. `/immune` raporunun yanında
`SelfHealing.report()` metrikleri tutar.

**Eklenti sistemi:** `plugins/` klasörüne `Tool` arayüzünü uygulayan derlenmiş
`.class` dosyaları koyun; ajan açılışta otomatik yükler, LLM sistem prompt'unda
görür. Kod değiştirmek gerekmez. Detay: `plugins/README.md`.

### Terminalsiz çalışma

Masaüstündeki **`Çalıştır-GUI.vbs`** ile ajan **hiç konsol açmadan** başlar:
`javaw` kullanılır, derleme gizli yapılır. Niyet, güven, öğrenme ve araç çağrıları
sohbet alanına chatter olarak basılmaz — arka planda sessizce çalışır; sonuç yalnızca
GUI'deki cevap ve durum çubuğunda görünür.

### DIDL — kendi beynimiz

**DIDL** (Dijital İstihbarat Decision Layer), ajanın kendi modellerinden oluşan
karar katmanıdır. LLM beynin üzerine gelir; beyin tek başına da çalışabilir.

| Bileşen | Katkı |
|---|---|
| Mini transformer (`own-model.json`) | Çağrışımsal ön sezgi (deneysel) |
| Niyet MLP'si | Yönlendirme |
| Dünya modeli | Sonuç tahmini |
| Vektör hafıza | İlgili anılar |

```
/brain              DIDL durumu
/brain on|off       beyni aç/kapat
/think <metin>      SADECE kendi beynimizle cevap (LLM kullanılmaz)
```

**Yedek beyin (failover):** ana model çağrısı başarısız olursa sırayla
`fallback.models` listesindeki diğer Ollama modelleri denenir; hepsi düşerse
DIDL yerel cevap devreye girer. `/fallback` ile zincir görüntülenir:

```
gemma4:12b → deepseek-r1:7b → gemma4:e4b → qwen2.5:7b-instruct → DIDL (yerel)
```

Test: `/brain` → `DIDL Beyin: AÇIK | Kendi transformer: YÜKLÜ | Corpus: 1723 karakter`.
`/think merhaba` → yerel modellerden üretilen cevap. LLM çağrısı başarısız olursa
DIDL otomatik yedek cevap üretir. `config.properties` içinde `brain=true/false`.

### İleri seviye: platform özellikleri

**HTTP API sunucusu** — ajan artık harici uygulamalardan kullanılabilir:

```bat
build-server.bat        :: http://localhost:8080
```
veya CLI içinde `/serve` / `/stop`. Endpoint'ler:

| Endpoint | Açıklama |
|---|---|
| `GET /health` | Sağlık kontrolü |
| `GET /status` | Model, katmanlar, hafıza, dünya modeli |
| `GET /metrics` | İstek/gecikme/güven/araç istatistikleri (JSON) |
| `POST /api/chat` | `{"message":"..."}` → tam cevap + niyet + güven |
| `POST /api/stream` | Aynı istek, **token token akan** düz metin |
| `POST /api/learn` | `{"text":"...","intent":"bilgi"}` → canlı öğrenme |

**Self-consistency** — `/consensus on` ile 3 farklı sıcaklıkta cevap üretilir,
en tutarlı (tekrar eden) olan seçilir; yoksa en kısa. Yavaş ama daha isabetli.
GUI'de ilgili anahtar ve `/consensus` komutu var.

**Metrikler** — `/metrics` ile ortalama/maksimum gecikme, araç çağrısı, ortalama
güven ve son 20 istek görüntülenir. API `/metrics` endpoint'i aynı veriyi JSON verir.

**Yapılandırma** — `config.properties`: model, port, fast/deep/consensus/autonomy/graph
bayrakları. Açılışta otomatik okunur.

### Gerçekçi beklenti

Bu hâlâ **AGI değil**; AGI'ye giden yolda planlayan, kendini denetleyen, hatırlayan ve işbirliği yapan bir *ajan sistemi*. Gerçek AGI için gereken dünya modeli, süren öğrenme ve özerk hedef üretme burada yok. Yine de dört temel yapı taşı (planlama, reflection, hafıza, çok ajan) yerinde ve genişletilebilir.

## Daha ileri gitmek için öneriler

- `NeuralNetwork` sınıfına L2 regularization / dropout ekleyin.
- `Loss` ve `Activation` enum'larına yeni fonksiyonlar ekleyip `NeuralNetwork`'i değiştirmeden kullanın.
- Weka tarafında `J48` yerine `RandomForest`, `NaiveBayes` veya `SMO` (SVM) deneyin.
- `Matrix.mul` içindeki döngü sırasını değiştirerek cache performansını ölçün.
