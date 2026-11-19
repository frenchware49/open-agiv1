package agent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bilgi grafığında iki varlık arasındaki ilişki: özne --yüklem--> nesne. */
public final class Relation {
    private final String subject;
    private final String predicate;
    private final String object;
    private final double confidence;

    public Relation(String subject, String predicate, String object, double confidence) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.confidence = confidence;
    }

    public String subject() { return subject; }
    public String predicate() { return predicate; }
    public String object() { return object; }
    public double confidence() { return confidence; }

    public String describe() {
        return subject + " --" + predicate + "--> " + object;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("subject", subject);
        m.put("predicate", predicate);
        m.put("object", object);
        m.put("confidence", confidence);
        return m;
    }

    public static Relation fromMap(Map<String, Object> m) {
        Object c = m.get("confidence");
        double conf = c instanceof Number ? ((Number) c).doubleValue() : 1.0;
        return new Relation(Json.str(m.get("subject")), Json.str(m.get("predicate")),
                Json.str(m.get("object")), conf);
    }
}
