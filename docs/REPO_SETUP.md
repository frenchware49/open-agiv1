# GitHub Yayın Rehberi

## Repo bilgileri

**Ad:** `open-agi-lab`

**Açıklama (About alanına yapıştırılır, 350 karakter):**
```
AGI — Yapay Genel Zeka'ya giden yol. Açık, bağımlılıksız Java ile yazılmış yerel ajan mimarisi: planlama, öz-denetim, hafıza, dünya modeli, çok ajan, sürekli öğrenme. Deneysel V1.
```

**Topics (konular):**
```
agi, artificial-intelligence, java, agents, llm, ollama, open-source, experimental, machine-learning, cognitive-architecture
```

**Website:** (yoksa boş bırakın)

## Yayınlama adımları

1. GitHub CLI girişi (tarayıcı açılır):
   ```
   gh auth login
   ```
   Seçenekler: `GitHub.com` → `HTTPS` → `Login with a web browser`.

2. Repo'yu oluştur ve yükle:
   ```
   gh repo create open-agi-lab --public --source=. --remote=origin --push
   ```

3. Doğrula:
   ```
   git remote -v
   gh repo view --web
   ```

## Yayın sonrası

- README'deki **"yakında yayınlanacaktır"** satırını kaldırın, gerçek repo URL'sini ekleyin.
- `SECURITY.md` içindeki özel bildirim yöntemini GitHub'ın **Security → Report a vulnerability**
  özelliğiyle eşleştirin (repo ayarlarından etkinleştirin).
- `.github/workflows/test.yml` ilk push'ta çalışacak; Actions sekmesinden yeşil olduğunu kontrol edin.
- Katkı bekleyenler için `CONTRIBUTING.md` ve `docs/ROADMAP.md` hazır.

## Lisans ve sorumluluk

MIT lisansı ticari kullanıma izin verir. Proje deneyseldir; "AGI gerçekleşti" iddiası
yoktur. Bu dürüstlük katkı çeker — tanıtımda bunu koruyun.
