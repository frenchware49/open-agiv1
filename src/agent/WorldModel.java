package agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dünya modeli: gözlemlerdeki olayları ve geçişleri öğrenir, gelecek sonuçları tahmin eder.
 * Bilgi grafığındaki üçlüleri "eylem → sonuç" kurallarına dönüştürür ve zincirler.
 */
public final class WorldModel {
    private final KnowledgeGraph graph;
    private final List<Event> events = new ArrayList<Event>();
    private final Map<String, Map<String, Integer>> transitions =
            new LinkedHashMap<String, Map<String, Integer>>();
    private int syncedCount = 0;

    public WorldModel(KnowledgeGraph graph) {
        this.graph = graph;
        sync();
    }

    /** Grafığa sonradan eklenen olguları modele aktarır (çift saymamak için fark tutar). */
    private void sync() {
        List<Relation> facts = graph.allFacts();
        if (facts.size() == syncedCount) return;
        for (int i = syncedCount; i < facts.size(); i++) {
            Relation r = facts.get(i);
            learn(r.subject(), r.predicate(), r.object());
        }
        syncedCount = facts.size();
    }

    /** Bir gözlemi kaydeder ve olası sonuç istatistiğini günceller. */
    public void observe(String subject, String action, String object) {
        graph.addFact(subject, action, object);
        events.add(new Event(subject, action, object, System.currentTimeMillis()));
        sync();
    }

    private void learn(String subject, String action, String object) {
        String key = subject.toLowerCase(Locale.ROOT) + "|" + action.toLowerCase(Locale.ROOT);
        Map<String, Integer> outcomes = transitions.get(key);
        if (outcomes == null) {
            outcomes = new LinkedHashMap<String, Integer>();
            transitions.put(key, outcomes);
        }
        Integer n = outcomes.get(object);
        outcomes.put(object, n == null ? 1 : n + 1);
    }

    /** Bir varlığın eyleminin muhtemel sonucunu tahmin eder. */
    public Prediction predict(String subject, String action) {
        sync();
        String key = subject.toLowerCase(Locale.ROOT) + "|" + action.toLowerCase(Locale.ROOT);
        Map<String, Integer> outcomes = transitions.get(key);
        if (outcomes == null || outcomes.isEmpty()) {
            return new Prediction(subject, action, "(bilinmiyor)", 0.0, "Bu eylem için gözlem yok.");
        }
        String best = null;
        int count = 0;
        int total = 0;
        for (Map.Entry<String, Integer> e : outcomes.entrySet()) {
            total += e.getValue();
            if (e.getValue() > count) { count = e.getValue(); best = e.getKey(); }
        }
        double confidence = total == 0 ? 0 : (double) count / total;
        return new Prediction(subject, action, best, confidence,
                subject + " --" + action + "--> " + best);
    }

    /** Çok adımlı simülasyon: zincirleme tahminler üretir. */
    public String simulate(String subject, int steps) {
        StringBuilder sb = new StringBuilder();
        String current = subject;
        sb.append(current);
        for (int i = 0; i < steps; i++) {
            List<String> actions = actionsFor(current);
            if (actions.isEmpty()) { sb.append("  → (durdu: bilgi yok)"); break; }
            Prediction best = null;
            for (String a : actions) {
                Prediction p = predict(current, a);
                if (best == null || p.confidence > best.confidence) best = p;
            }
            sb.append(" --").append(best.action).append("(")
              .append(String.format(Locale.ROOT, "%.2f", best.confidence))
              .append(")--> ").append(best.outcome);
            if (best.outcome.equals("(bilinmiyor)")) break;
            current = best.outcome;
        }
        return sb.toString();
    }

    /** Karşı-olgusal akıl: "ya bu eylem olmasaydı / farklı olsaydı ne olurdu?" */
    public String counterfactual(String subject, String action) {
        sync();
        String key = subject.toLowerCase(Locale.ROOT) + "|" + action.toLowerCase(Locale.ROOT);
        Map<String, Integer> outcomes = transitions.get(key);
        if (outcomes == null || outcomes.isEmpty()) {
            return "Bu eylem için gözlem yok; karşı-olgusal çıkarım yapılamıyor.";
        }
        Prediction likely = predict(subject, action);
        if (outcomes.size() < 2) {
            return "Bu eylemin yalnızca bir bilinen sonucu var (" + likely.outcome +
                   "); alternatif senaryo için daha fazla gözlem gerekli.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Eğer ").append(subject).append(" ").append(action)
          .append(" yapmasaydı ya da farklı sonuçlansaydı, olası alternatifler: ");
        int total = 0;
        for (Integer n : outcomes.values()) total += n;
        boolean first = true;
        for (Map.Entry<String, Integer> e : outcomes.entrySet()) {
            if (e.getKey().equals(likely.outcome)) continue;
            if (!first) sb.append(", ");
            first = false;
            sb.append(e.getKey()).append(" (olasılık %")
              .append(Math.round(100.0 * e.getValue() / total)).append(")");
        }
        return sb.toString();
    }

    public List<String> actionsFor(String subject) {
        List<String> out = new ArrayList<String>();
        String prefix = subject.toLowerCase(Locale.ROOT) + "|";
        for (String key : transitions.keySet()) if (key.startsWith(prefix)) {
            out.add(key.substring(prefix.length()));
        }
        return out;
    }

    public String rules() {
        sync();
        if (transitions.isEmpty()) return "(kurallar yok — önce gözlem ekleyin)";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Integer>> e : transitions.entrySet()) {
            String[] parts = e.getKey().split("\\|", 2);
            Prediction p = predict(parts[0], parts[1]);
            sb.append(String.format(Locale.ROOT, "  %s --%s--> %s  [güven %.2f, n=%d]%n",
                    parts[0], parts[1], p.outcome, p.confidence, e.getValue().size()));
        }
        return sb.toString().trim();
    }

    public String stats() {
        sync();
        return "Gözlem: " + events.size() + " | Kural: " + transitions.size() +
               " | Grafik: " + graph.entityCount() + " varlık / " + graph.relationCount() + " ilişki";
    }

    public static final class Prediction {
        public final String subject;
        public final String action;
        public final String outcome;
        public final double confidence;
        public final String explanation;
        Prediction(String subject, String action, String outcome, double confidence, String explanation) {
            this.subject = subject; this.action = action; this.outcome = outcome;
            this.confidence = confidence; this.explanation = explanation;
        }
        @Override public String toString() {
            return explanation + "  (güven " + String.format(Locale.ROOT, "%.2f", confidence) + ")";
        }
    }

    private static final class Event {
        final String subject, action, object;
        final long time;
        Event(String subject, String action, String object, long time) {
            this.subject = subject; this.action = action; this.object = object; this.time = time;
        }
    }
}
