package agent;

import ownmodel.MiniTransformer;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * DIDL — Dijital İstihbarat Decision Layer.
 * Ajanın KENDİ modellerinden oluşan karar/beyan katmanı:
 *   - Mini transformer (kendi ağırlıklarımız) → çağrışımsal ön sezgi
 *   - Niyet MLP'si → yönlendirme
 *   - Dünya modeli → sonuç tahmini
 *   - Vektör hafıza → ilgili anılar
 *
 * LLM (Gemma 4) bu beynin üzerine gelir; beyin tek başına da çalışabilir
 * (zayıf ama kendi kendine bir cevap üretebilir).
 */
public final class DIDLBrain {
    private MiniTransformer transformer;
    private boolean enabled = true;
    private String corpus = "";

    public DIDLBrain() {
        try {
            File f = new File("corpus.txt");
            corpus = f.exists()
                    ? new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)
                    : "yapay zeka ajan öğrenme dünya modeli karar hafıza araç plan ";
            if (new File("own-model.json").exists()) {
                transformer = MiniTransformer.load("own-model.json", corpus);
            }
        } catch (Exception e) {
            System.out.println("  [DIDL] model yüklenemedi: " + e.getMessage());
        }
    }

    public boolean hasOwnModel() { return transformer != null; }
    public boolean enabled() { return enabled; }
    public void setEnabled(boolean v) { enabled = v; }

    public String stats() {
        return "DIDL Beyin: " + (enabled ? "AÇIK" : "KAPALI") +
               " | Kendi transformer: " + (hasOwnModel() ? "YÜKLÜ" : "YOK (build-ownmodel.bat)") +
               " | Corpus: " + corpus.length() + " karakter";
    }

    /** LLM'in sistem prompt'una eklenecek, kendi modellerimizden gelen bağlam. */
    public String contextFor(String text, Agent agent) {
        if (!enabled) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("[DIDL — kendi beynimizin analizi]\n");

        if (agent.lastRouteInfo() != null && !agent.lastRouteInfo().isEmpty()) {
            sb.append("- Niyet ağı: ").append(agent.lastRouteInfo()).append('\n');
        }

        List<String> entities = agent.graphEntities();
        int shown = 0;
        for (String e : entities) {
            if (shown >= 2) break;
            if (text.toLowerCase(Locale.ROOT).contains(e.toLowerCase(Locale.ROOT))) {
                sb.append("- Dünya modeli: ").append(agent.worldPredict(e, "ilgili")).append('\n');
                shown++;
            }
        }

        List<String> memories = agent.recall(text);
        for (int i = 0; i < Math.min(2, memories.size()); i++) {
            sb.append("- Anı: ").append(memories.get(i)).append('\n');
        }

        if (transformer != null) {
            try {
                String thought = transformer.generate(text, 18);
                if (thought.length() > text.length()) {
                    sb.append("- Kendi modelimizin çağrışımı (deneysel): ")
                      .append(thought.substring(text.length()).trim()).append('\n');
                }
            } catch (Exception ignored) {}
        }
        return sb.toString();
    }

    /** LLM olmadan, yalnızca kendi modellerimizle üretilen yedek cevap. */
    public String think(String text, Agent agent) {
        StringBuilder sb = new StringBuilder();
        sb.append("[DIDL — yerel cevap] ");

        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("saat") || lower.contains("tarih")) {
            sb.append("Zaman bilgisini araçtan alıyorum. ");
        }
        if (lower.matches(".*\\d+\\s*[-+*/]\\s*\\d+.*")) {
            sb.append("Bu bir hesaplama; hesap makinesiyle çözülebilir. ");
        }
        List<String> memories = agent.recall(text);
        if (!memories.isEmpty()) {
            sb.append("Hatırladım: ").append(memories.get(0)).append(". ");
        }
        if (transformer != null) {
            try {
                String t = transformer.generate(text, 24);
                if (t.length() > text.length()) {
                    sb.append("Kendi modelim: \"").append(t.substring(text.length()).trim()).append("\". ");
                }
            } catch (Exception ignored) {}
        }
        sb.append("(Bu cevap yalnızca yerel modellerimden geldi; LLM katmanı şu an yok.)");
        return sb.toString();
    }
}
