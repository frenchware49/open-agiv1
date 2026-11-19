package agent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URL'lerden metin çekip ajanın kalıcı hafızasını ve bilgi grafığını besler.
 * "Araştıra araştıra öğrenme" katmanı: kaynak listesindeki sayfaları ziyaret eder,
 * olguları çıkarır, özetleri vektör hafızaya yazar.
 */
public final class WebResearch {
    private static final String SOURCES = "research-sources.txt";
    private final OllamaClient client;
    private final KnowledgeGraph graph;
    private final VectorMemory memory;
    private final Set<String> seeds = new LinkedHashSet<String>();

    public WebResearch(OllamaClient client, KnowledgeGraph graph, VectorMemory memory) {
        this.client = client;
        this.graph = graph;
        this.memory = memory;
        loadSeeds();
    }

    public List<String> seeds() { return new ArrayList<String>(seeds); }

    public void addSeed(String url) {
        if (url == null || !url.trim().startsWith("http")) return;
        String u = url.trim();
        if (seeds.add(u)) saveSeeds();
    }

    /** Tek bir URL'yi araştırır: metni çeker, özetleri hafızaya, olguları grafiğe yazar. */
    public String research(String url) {
        try {
            String html = fetch(url);
            String text = stripHtml(html);
            if (text.length() < 200) return "Sayfa yeterli metin içermiyor: " + url;

            String excerpt = text.length() > 6000 ? text.substring(0, 6000) : text;
            String safe = ImmuneSystem.GLOBAL.guard(excerpt);
            memory.add("Kaynak: " + url + "\n" + safe);
            int facts = graph.extractFacts(safe);

            return "İncelendi: " + url + "\nMetin: " + excerpt.length() + " karakter\n" +
                   "Yeni olgu: " + facts + "\nBilgi grafiği: " + graph.entityCount() +
                   " varlık / " + graph.relationCount() + " ilişki";
        } catch (Exception e) {
            return "Araştırma hatası (" + url + "): " + e.getMessage();
        }
    }

    /** Kaynak listesindeki ilk maxPages URL'yi araştırır. */
    public String researchSeeds(int maxPages) {
        if (seeds.isEmpty()) return "Kaynak listesi boş. /addsource <url> ile ekleyin.";
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (String url : seeds) {
            if (n++ >= maxPages) break;
            sb.append(research(url)).append("\n\n");
        }
        return sb.toString().trim();
    }

    private void loadSeeds() {
        java.io.File f = new java.io.File(SOURCES);
        if (!f.exists()) return;
        try {
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String t = line.trim();
                if (!t.isEmpty() && !t.startsWith("#")) seeds.add(t);
            }
        } catch (Exception ignored) {}
    }

    private void saveSeeds() {
        try {
            List<String> lines = new ArrayList<String>();
            lines.add("# Araştırma kaynakları (bir satır bir URL)");
            lines.addAll(seeds);
            Files.write(new java.io.File(SOURCES).toPath(), lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("  [Uyarı] Kaynak listesi kaydedilemedi: " + e.getMessage());
        }
    }

    private static String fetch(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(20000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (YerelAjan/1.0; research)");
        try {
            int code = conn.getResponseCode();
            InputStream in = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (in == null) throw new Exception("HTTP " + code);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
                if (sb.length() > 200000) break;
            }
            br.close();
            if (code >= 400) throw new Exception("HTTP " + code);
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static String stripHtml(String html) {
        String s = html.replaceAll("(?is)<script.*?</script>", " ")
                       .replaceAll("(?is)<style.*?</style>", " ")
                       .replaceAll("(?is)<sup.*?</sup>", " ")
                       .replaceAll("(?is)<[^>]+>", " ");
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
             .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
             .replace("&uuml;", "ü").replace("&ouml;", "ö").replace("&ccedil;", "ç")
             .replace("&scaron;", "ş").replace("&gbreve;", "ğ").replace("&Idot;", "İ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }
}
