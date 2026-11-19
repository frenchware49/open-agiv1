package agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Görevi alt adımlara bölen ve sonuçları tek cevapta birleştiren katman. */
public final class Planner {
    private final OllamaClient client;

    public Planner(OllamaClient client) {
        this.client = client;
    }

    /** 1-5 adımlık bir plan döndürür; JSON bozuksa tek adım olarak görev döner. */
    public List<String> plan(String task) {
        String system =
            "Sen bir görev planlayıcısın. Kullanıcının isteğini küçük, bağımsız alt adımlara böl.\n" +
            "Kurallar:\n" +
            "- En fazla 5 adım, mümkünse 1-3 adım.\n" +
            "- Basit bir selamlaşma veya tek bilgi sorusuysa TEK adım döndür.\n" +
            "- Her adım tek bir işi tanımlayan kısa Türkçe cümle olsun.\n" +
            "- Cevabını SADECE şu JSON ile ver, başka metin yazma:\n" +
            "{\"steps\":[\"adım 1\",\"adım 2\"]}";
        try {
            String out = client.ask(system, "Görev: " + task, 0.2);
            int s = out.indexOf('{');
            int e = out.lastIndexOf('}');
            if (s < 0 || e <= s) return fallback(task);
            Object parsed = Json.parse(out.substring(s, e + 1));
            Map<String, Object> obj = Json.asObject(parsed);
            Object steps = obj == null ? null : obj.get("steps");
            if (!(steps instanceof List)) return fallback(task);
            List<String> result = new ArrayList<String>();
            for (Object o : (List<?>) steps) {
                String step = Json.str(o);
                if (step != null && !step.trim().isEmpty()) result.add(step.trim());
                if (result.size() == 5) break;
            }
            return result.isEmpty() ? fallback(task) : result;
        } catch (Exception ex) {
            return fallback(task);
        }
    }

    /** Adım sonuçlarını birleştirip kullanıcıya verilecek tek cevabı üretir. */
    public String synthesize(String task, String results) {
        String system =
            "Sen bir birleştiricisin. Alt adımlarda elde edilen sonuçları kullanarak\n" +
            "kullanıcının asıl sorusuna tek, tutarlı, kısa bir Türkçe cevap ver.\n" +
            "Adım başlıklarını tekrar etme. Araç JSON'u yazma.";
        String user = "Asıl görev: " + task + "\n\nAdım sonuçları:\n" + results;
        try {
            return client.ask(system, user, 0.3);
        } catch (Exception e) {
            return results;
        }
    }

    private static List<String> fallback(String task) {
        List<String> one = new ArrayList<String>();
        one.add(task);
        return one;
    }
}
