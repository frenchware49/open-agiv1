package agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tek bir uzmanlık rolüyle çalışan ajan (multi-agent sistemin işçisi). */
public final class Specialist {
    private final OllamaClient client;
    private final List<Tool> tools;
    private final String name;
    private final String role;

    public Specialist(OllamaClient client, List<Tool> tools, String name, String role) {
        this.client = client;
        this.tools = tools;
        this.name = name;
        this.role = role;
    }

    public String name() { return name; }

    public String run(String task, String context) {
        String system =
            "Sen '" + name + "' adlı bir uzman ajansın.\n" +
            "Uzmanlığın: " + role + "\n" +
            "Görevini yaparken kısa ve öz ol. Sadece kendi uzmanlığınla ilgilen.\n\n" +
            "[Araçlar]\n" + ToolCalls.describe(tools) + "\n" + ToolCalls.protocol();

        List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>();
        msgs.add(Json.msg("user", "Görev: " + task + "\n\nBağlam:\n" +
                (context == null ? "(yok)" : context)));

        String answer = "";
        for (int round = 0; round < 3; round++) {
            try {
                answer = client.chat(msgs, system, 0.3);
            } catch (Exception e) {
                return "Uzman hatası: " + e.getMessage();
            }
            Map<String, Object> call = ToolCalls.parse(answer);
            if (call == null) break;
            String toolName = Json.str(call.get("tool"));
            Object raw = call.get("args");
            Map<String, Object> args = raw instanceof Map ? castMap((Map<?, ?>) raw) : new HashMap<String, Object>();
            String result = execute(toolName, args);
            msgs.add(Json.msg("user", "ARAÇ SONUCU (" + toolName + "): " + result +
                    "\nBu sonucu kullanarak görevi tamamla. Araç JSON'u yazma."));
            answer = "";
        }
        if (answer.isEmpty() || ToolCalls.parse(answer) != null) {
            msgs.add(Json.msg("user", "Artık araç kullanma. Kısa Türkçe cevap ver."));
            try {
                answer = client.chat(msgs, system, 0.2);
            } catch (Exception e) {
                answer = "Cevap üretilemedi: " + e.getMessage();
            }
        }
        return answer;
    }

    private String execute(String name, Map<String, Object> args) {
        if (name == null) return "Hata: araç adı yok.";
        for (Tool t : tools) {
            if (t.name().equalsIgnoreCase(name)) {
                try { return t.execute(args); }
                catch (Exception e) { return "Araç hatası: " + e.getMessage(); }
            }
        }
        return "Bilinmeyen araç: " + name;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> src) {
        Map<String, Object> out = new HashMap<String, Object>();
        for (Map.Entry<?, ?> e : src.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
        return out;
    }
}
