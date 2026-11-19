package agent;

import java.util.List;
import java.util.Map;

/**
 * Otomatik müfredat öğrenme: ajan kendi bilgi grafiğinden soru-cevap üretir,
 * cevabı hafızaya, soruyu niyet ağına işler. Öğretmen olmadan kendini eğitir.
 */
public final class CurriculumLearner {
    private final OllamaClient client;
    private int generated = 0;
    private int learned = 0;

    public CurriculumLearner(OllamaClient client) { this.client = client; }

    /** Tek bir öğrenme turu. Arka planda sessizce çağrılabilir. */
    public String runOnce(Agent agent) {
        List<String> facts = agent.graphRelations();
        if (facts.isEmpty()) return "Müfredat için yeterli bilgi yok (önce /observe veya /research).";
        String fact = facts.get((int) (System.currentTimeMillis() / 1000) % facts.size());

        String system =
            "Sen bir öğretmensin. Verilen bilgiden Türkçe BİR soru-cevap çifti üret.\n" +
            "SADECE JSON ver: {\"question\":\"...\",\"answer\":\"...\",\"intent\":\"bilgi\"}\n" +
            "intent şunlardan biri: matematik, dosya, kod, bilgi, sohbet.";
        try {
            String out = client.ask(system, "Bilgi: " + fact, 0.3);
            Map<String, Object> obj = Json.asObject(Json.parse(out));
            String q = obj == null ? null : Json.str(obj.get("question"));
            String a = obj == null ? null : Json.str(obj.get("answer"));
            String intent = obj == null ? null : Json.str(obj.get("intent"));
            if (q == null || a == null) return "Soru üretilemedi.";
            generated++;
            agent.remember("Kendi müfredatım: " + a);
            IntentRouter.LearnResult res = agent.teach(q, intent == null ? "bilgi" : intent);
            if (res.success) learned++;
            return "Müfredat: " + q + " → " + a;
        } catch (Exception e) {
            return "Müfredat hatası: " + e.getMessage();
        }
    }

    public String report() {
        return "Otomatik müfredat: " + generated + " soru üretildi, " + learned + " öğrenildi";
    }
}
