package agent;

import java.util.Map;

/** Öz-onarım: başarısız araç çağrılarını analiz edip düzeltilmiş argümanlar üretir. */
public final class SelfHealing {
    private final OllamaClient client;
    private int repairs = 0;
    private int attempts = 0;

    public SelfHealing(OllamaClient client) { this.client = client; }

    /** Hata mesajından yola çıkarak aynı araç için düzeltilmiş JSON argümanları döndürür. */
    public String repair(String tool, String error, String oldArgs, String task) {
        attempts++;
        String system =
            "Sen bir hata onarımcisin. Bir aracın çağrısı hata verdi.\n" +
            "Aynı araç için DÜZELTİLMİŞ argümanları SADECE JSON olarak ver:\n" +
            "{\"args\":{...}}\n" +
            "Başka metin yazma. Araç adını değiştirme. Argüman isimlerini düzelt.";
        String user = "Araç: " + tool + "\nHata: " + error +
                "\nÖnceki argümanlar: " + oldArgs + "\nGörev: " + task;
        try {
            String out = client.ask(system, user, 0.0);
            Map<String, Object> m = Json.asObject(Json.parse(out));
            if (m != null && m.containsKey("args") && m.get("args") instanceof java.util.Map) {
                repairs++;
                return out;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public boolean isFailure(String result) {
        return result != null && (result.startsWith("Hata") || result.startsWith("Araç hatası")
                || result.startsWith("Derleme hatası") || result.contains("hatası:"));
    }

    public String report() {
        return "Öz-onarım: " + attempts + " deneme | " + repairs + " başarılı düzeltme";
    }

    public int repairs() { return repairs; }
}
