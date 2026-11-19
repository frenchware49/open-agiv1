package agent;

import java.util.Map;

/** Self-reflection: cevabı birden fazla turda denetler, güven skoru üretir ve düzeltir. */
public final class Reflector {
    private final OllamaClient client;
    private static final int MAX_ROUNDS = 2;

    public Reflector(OllamaClient client) {
        this.client = client;
    }

    /** Birden fazla denetim turu; ilk turda sorun yoksa erken durur. */
    public Review reviewMulti(String task, String draft, String context) {
        String current = draft;
        Review last = new Review(true, "", draft, 0.5);
        for (int i = 0; i < MAX_ROUNDS; i++) {
            Review r = review(task, current, context);
            last = r;
            if (r.ok) break;
            if (r.revised != null && !r.revised.trim().isEmpty()
                    && !r.revised.trim().equals(current.trim())) {
                current = r.revised;
            } else {
                break;
            }
        }
        return new Review(last.ok, last.critique, current, last.confidence);
    }

    public Review review(String task, String draft, String context) {
        String system =
            "Sen bir denetçisin (self-reflection). Bir yapay zeka cevabını kontrol et.\n" +
            "Kontrol et: (1) Kullanıcının sorusuna gerçekten cevap veriyor mu?\n" +
            "(2) Araç sonuçlarıyla çelişiyor mu? (3) Tutarsız veya eksik mi?\n" +
            "Cevap iyiyse ok=true, kötüyse ok=false ve düzeltilmiş cevabı 'revised' alanına yaz.\n" +
            "SADECE gerçek bir yanlışlık veya araç sonucuyla çelişki varsa ok=false ver.\n" +
            "Araç sonuçları kesin doğrudur; cevap onlarla uyumluysa MUTLAKA ok=true ver.\n" +
            "Matematiksel bir ifadeyi kendi başına yeniden hesaplayıp cevabı 'düzeltmeye' çalışma.\n" +
            "Üslup, uzunluk veya kişisel tercih nedeniyle ok=false VERME. Emin değilsen ok=true ver.\n" +
            "confidence: cevabın doğruluğuna dair 0.0-1.0 arası tahminin.\n" +
            "Cevabını SADECE şu JSON ile ver:\n" +
            "{\"ok\":true,\"critique\":\"kısa gerekçe\",\"revised\":\"cevap\",\"confidence\":0.9}";
        String user = "Görev: " + task + "\n\nBağlam / araç sonuçları:\n" +
                (context == null ? "(yok)" : context) + "\n\nDenetlenecek cevap:\n" + draft;
        try {
            String out = client.ask(system, user, 0.1);
            int s = out.indexOf('{');
            int e = out.lastIndexOf('}');
            if (s < 0 || e <= s) return new Review(true, "", draft, 0.5);
            Object parsed = Json.parse(out.substring(s, e + 1));
            Map<String, Object> obj = Json.asObject(parsed);
            if (obj == null) return new Review(true, "", draft, 0.5);
            Object okObj = obj.get("ok");
            boolean ok = !(okObj instanceof Boolean) || (Boolean) okObj;
            String critique = Json.str(obj.get("critique"));
            String revised = Json.str(obj.get("revised"));
            Object confObj = obj.get("confidence");
            double confidence = confObj instanceof Number ? ((Number) confObj).doubleValue() : 0.5;
            if (confidence < 0 || confidence > 1) confidence = 0.5;
            if (!ok && revised != null && !revised.trim().isEmpty()) {
                return new Review(false, critique == null ? "" : critique, revised.trim(), confidence);
            }
            return new Review(ok, critique == null ? "" : critique, draft, confidence);
        } catch (Exception ex) {
            return new Review(true, "", draft, 0.5);
        }
    }

    public static final class Review {
        public final boolean ok;
        public final String critique;
        public final String revised;
        public final double confidence;

        Review(boolean ok, String critique, String revised, double confidence) {
            this.ok = ok;
            this.critique = critique;
            this.revised = revised;
            this.confidence = confidence;
        }
    }
}
