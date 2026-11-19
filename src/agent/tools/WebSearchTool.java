package agent.tools;

import agent.Json;
import agent.Tool;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Wikipedia API üzerinden gerçek web araması (API anahtarı gerektirmez). */
public final class WebSearchTool implements Tool {
    @Override public String name() { return "web_search"; }

    @Override public String description() {
        return "İnternetten bilgi arar (Wikipedia). args: {\"query\":\"yapay zeka\"}.";
    }

    @Override public String execute(Map<String, Object> args) {
        String q = Json.str(args.get("query"));
        if (q == null) q = Json.str(args.get("q"));
        if (q == null || q.trim().isEmpty()) return "Hata: 'query' argümanı eksik.";
        try {
            String encoded = URLEncoder.encode(q.trim(), "UTF-8");
            String searchUrl = "https://tr.wikipedia.org/w/api.php?action=query&list=search&srsearch=" +
                    encoded + "&format=json&srlimit=3";
            Object parsed = Json.parse(get(searchUrl));
            Map<String, Object> root = Json.asObject(parsed);
            Map<String, Object> query = root == null ? null : Json.asObject(root.get("query"));
            Object results = query == null ? null : query.get("search");
            if (!(results instanceof java.util.List) || ((java.util.List<?>) results).isEmpty()) {
                return "Sonuç bulunamadı: " + q;
            }

            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Object o : (java.util.List<?>) results) {
                Map<String, Object> r = Json.asObject(o);
                String title = r == null ? null : Json.str(r.get("title"));
                if (title == null) continue;
                String extract = fetchExtract(title);
                String safeTitle = agent.ImmuneSystem.GLOBAL.guard(title);
                String safeExtract = agent.ImmuneSystem.GLOBAL.guard(extract);
                sb.append("[").append(safeTitle).append("] ").append(safeExtract).append("\n\n");
                if (++count == 3) break;
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return "Web arama hatası: " + e.getMessage();
        }
    }

    private static String fetchExtract(String title) throws Exception {
        String url = "https://tr.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=1" +
                "&explaintext=1&titles=" + URLEncoder.encode(title, "UTF-8") + "&format=json";
        Object parsed = Json.parse(get(url));
        Map<String, Object> root = Json.asObject(parsed);
        Map<String, Object> query = root == null ? null : Json.asObject(root.get("query"));
        Object pages = query == null ? null : query.get("pages");
        if (!(pages instanceof Map)) return "(özet alınamadı)";
        for (Object o : ((Map<?, ?>) pages).values()) {
            Map<String, Object> p = Json.asObject(o);
            String text = p == null ? null : Json.str(p.get("extract"));
            if (text == null) continue;
            text = text.replace('\n', ' ').trim();
            return text.length() > 700 ? text.substring(0, 700) + "..." : text;
        }
        return "(özet alınamadı)";
    }

    private static String get(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(12000);
        conn.setRequestProperty("User-Agent", "YerelAjan/1.0 (Java)");
        try {
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) throw new Exception("HTTP " + code);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            if (code >= 400) throw new Exception("HTTP " + code + ": " + sb);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
