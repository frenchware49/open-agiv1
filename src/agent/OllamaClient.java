package agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ollama'nın yerel HTTP API'si (/api/chat) için bağımlılıksız istemci. */
public final class OllamaClient {
    private final String baseUrl;
    private String model;
    private final java.util.Map<String, String> cache =
            new java.util.LinkedHashMap<String, String>(128, 0.75f, true) {
                @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, String> e) {
                    return size() > 100;
                }
            };

    public OllamaClient(String model) {
        this.baseUrl = "http://localhost:11434";
        this.model = model;
    }

    public String model() { return model; }
    public void setModel(String model) { this.model = model; }

    /** Tek tur soru-soru cevabı (planner, reflector, uzman ajanlar için). */
    public String ask(String system, String user, double temperature) throws IOException {
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        messages.add(Json.msg("user", user));
        return chat(messages, system, temperature);
    }

    /** Sunucunun ayakta olduğunu ve modelin kurulu olduğunu kontrol eder. */
    /** Kurulu modellerin adlarını döndürür. */
    public List<String> availableModels() throws IOException {
        List<String> names = new ArrayList<String>();
        Object parsed = Json.parse(get("/api/tags"));
        Map<String, Object> obj = Json.asObject(parsed);
        Object models = obj == null ? null : obj.get("models");
        if (!(models instanceof List)) return names;
        for (Object o : (List<?>) models) {
            Map<String, Object> m = Json.asObject(o);
            String name = m == null ? null : Json.str(m.get("name"));
            if (name != null) names.add(name);
        }
        return names;
    }

    public void check() throws IOException {
        String body = get("/api/tags");
        Object parsed = Json.parse(body);
        Map<String, Object> obj = Json.asObject(parsed);
        Object models = obj == null ? null : obj.get("models");
        if (!(models instanceof List)) throw new IOException("Ollama model listesi okunamadı.");
        boolean found = false;
        for (Object o : (List<?>) models) {
            Map<String, Object> m = Json.asObject(o);
            String name = m == null ? null : Json.str(m.get("name"));
            if (name != null && (name.equals(model) || name.startsWith(model + ":"))) {
                found = true;
                break;
            }
        }
        if (!found) throw new IOException("'" + model + "' modeli kurulu değil. `ollama pull " + model + "` ile indirin.");
    }

    /** sohbet turu; sistem mesajı ayrı olarak gönderilir, geçmiş mesajlar sırayla iletilir. */
    public String chat(List<Map<String, Object>> history, String system, double temperature) throws IOException {
        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        if (system != null && !system.trim().isEmpty()) {
            messages.add(Json.msg("system", system));
        }
        messages.addAll(history);
        return chatMessages(messages, temperature);
    }

    /** Hazır mesaj listesiyle sohbet (görüntülü mesajlar için). */
    public String chatMessages(List<Map<String, Object>> messages, double temperature) throws IOException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("stream", false);
        body.put("messages", messages);
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("temperature", temperature);
        options.put("num_predict", 4096); // uzun cevaplar kesilmesin
        body.put("options", options);
        body.put("keep_alive", "30m"); // model GPU'da sıcak kalsın

        String key = Json.stringify(body);
        synchronized (cache) {
            String hit = cache.get(key);
            if (hit != null) return hit;
        }
        String response = post("/api/chat", Json.stringify(body));
        Object parsed = Json.parse(response);
        Map<String, Object> obj = Json.asObject(parsed);
        Map<String, Object> message = obj == null ? null : Json.asObject(obj.get("message"));
        String content = message == null ? null : Json.str(message.get("content"));
        String result = content == null ? "" : content.trim();
        synchronized (cache) { cache.put(key, result); }
        return result;
    }

    /** Token token akan sohbet: ilk kelime saniyeler içinde görünür. */
    public String chatStream(List<Map<String, Object>> messages, double temperature,
                             java.util.function.Consumer<String> onToken) throws IOException {
        return chatStream(messages, temperature, onToken, 0);
    }

    /** Kesilirse otomatik "devam et" turlarıyla metni tamamlar. */
    private String chatStream(List<Map<String, Object>> messages, double temperature,
                              java.util.function.Consumer<String> onToken, int round) throws IOException {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("model", model);
        body.put("stream", true);
        body.put("messages", messages);
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("temperature", temperature);
        options.put("num_predict", 4096);
        body.put("options", options);
        body.put("keep_alive", "30m");

        StringBuilder full = new StringBuilder();
        String reason = "";
        byte[] bytes = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/api/chat").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setFixedLengthStreamingMode(bytes.length);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(300000);
        try {
            OutputStream os = conn.getOutputStream();
            os.write(bytes);
            os.close();
            int code = conn.getResponseCode();
            java.io.InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) throw new IOException("HTTP " + code);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    Map<String, Object> obj = Json.asObject(Json.parse(line));
                    if (obj == null) continue;
                    Map<String, Object> msg = Json.asObject(obj.get("message"));
                    String token = msg == null ? null : Json.str(msg.get("content"));
                    if (token != null && !token.isEmpty()) {
                        full.append(token);
                        onToken.accept(token);
                    }
                    if (Boolean.TRUE.equals(obj.get("done")) && obj.get("done_reason") != null) {
                        reason = Json.str(obj.get("done_reason"));
                    }
                } catch (Exception ignored) {}
            }
            br.close();
            if (code >= 400) throw new IOException("Ollama HTTP " + code);
        } finally {
            conn.disconnect();
        }

        if ("length".equals(reason) && round < 3 && full.length() > 0) {
            List<Map<String, Object>> continued = new ArrayList<Map<String, Object>>(messages);
            continued.add(Json.msg("user", "Cevabın yarım kaldı. Kaldığın yerden DEVAM ET, " +
                    "aynı cümleyi tekrar etmeden metni tamamla."));
            onToken.accept("\n");
            String more = chatStream(continued, temperature, onToken, round + 1);
            full.append(more);
        }
        return full.toString();
    }

    /** Görüntü + soru ile tek tur (Gemma 4 gibi çok modlu modeller için). */
    public String askWithImage(String system, String question, String imagePath, double temperature) throws IOException {
        byte[] bytes = java.nio.file.Files.readAllBytes(new java.io.File(imagePath).toPath());
        String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
        Map<String, Object> userMsg = Json.msg("user", question);
        List<String> images = new ArrayList<String>();
        images.add(base64);
        userMsg.put("images", images);

        List<Map<String, Object>> messages = new ArrayList<Map<String, Object>>();
        if (system != null && !system.trim().isEmpty()) messages.add(Json.msg("system", system));
        messages.add(userMsg);
        return chatMessages(messages, temperature);
    }

    private String get(String path) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(15000);
        try {
            int code = conn.getResponseCode();
            String text = read(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
            if (code >= 400) throw new IOException("Ollama HTTP " + code + ": " + text);
            return text;
        } finally {
            conn.disconnect();
        }
    }

    private String post(String path, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setFixedLengthStreamingMode(bytes.length);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(300000);
        try {
            OutputStream os = conn.getOutputStream();
            os.write(bytes);
            os.close();
            int code = conn.getResponseCode();
            String text = read(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
            if (code >= 400) throw new IOException("Ollama HTTP " + code + ": " + text);
            return text;
        } finally {
            conn.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        br.close();
        return sb.toString();
    }
}
