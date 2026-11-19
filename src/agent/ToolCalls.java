package agent;

import java.util.Map;

/** Araç çağrısı JSON'unu ayrıştırmak için paylaşılan yardımcı. */
public final class ToolCalls {
    private ToolCalls() {}

    public static Map<String, Object> parse(String text) {
        if (text == null) return null;
        String s = text.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            Object o = Json.parse(s.substring(start, end + 1));
            Map<String, Object> m = Json.asObject(o);
            return (m != null && m.containsKey("tool")) ? m : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String describe(Iterable<Tool> tools) {
        StringBuilder sb = new StringBuilder();
        for (Tool t : tools) sb.append("- ").append(t.name()).append(": ").append(t.description()).append('\n');
        return sb.toString();
    }

    public static String protocol() {
        return "Eğer bir araç gerekiyorsa cevabını SADECE şu JSON ile ver, başka metin yazma:\n" +
               "{\"tool\":\"calculator\",\"args\":{\"expression\":\"2+2\"}}\n" +
               "Araç gerekmiyorsa normal Türkçe cevap yaz, JSON yazma.\n";
    }
}
