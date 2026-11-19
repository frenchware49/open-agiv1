package agent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bilişsel bağışıklık sistemi — "zihin virüsü" (mind virus) savunması.
 *
 * Tehditler: prompt injection, rol enjeksiyonu, hafızaya kalıcı zararlı fikir
 * yerleştirme, ajanlar arası bulaşma, güvenlik kapatma girişimleri, veri sızıntısı.
 * Tüm metinler (kullanıcı, web, diğer ajanlar, hedefler, hafıza) otomatik taranır;
 * şüpheli içerik arındırılır veya karantinaya alınır. Komut gerekmez — arka planda çalışır.
 */
public final class ImmuneSystem {
    /** Tüm bileşenlerin paylaştığı tek örnek (web, ajanlar, hafıza, hedefler). */
    public static final ImmuneSystem GLOBAL = new ImmuneSystem();

    private static final double BLOCK_THRESHOLD = 3.0;
    private static final File QUARANTINE = new File("immune-quarantine.json");

    private long scanned = 0;
    private long sanitized = 0;
    private long blocked = 0;

    private static final String[][] PATTERNS = {
        // talimat geçersiz kılma
        {"ignore previous instructions", "3.0", "prompt-override"},
        {"ignore all previous", "3.0", "prompt-override"},
        {"önceki talimatları unut", "3.0", "prompt-override"},
        {"talimatlarını unut", "3.0", "prompt-override"},
        {"disregard your instructions", "3.0", "prompt-override"},
        {"forget your instructions", "3.0", "prompt-override"},
        // rol / sistem enjeksiyonu
        {"system:", "2.0", "role-injection"},
        {"assistant:", "2.0", "role-injection"},
        {"<|im_start|>", "3.0", "role-injection"},
        {"[sys]", "2.5", "role-injection"},
        {"yeni sistem mesajı", "2.5", "role-injection"},
        {"you are now", "2.0", "role-injection"},
        {"sen artık", "2.0", "role-injection"},
        {"developer mode", "3.0", "jailbreak"},
        {"jailbreak", "3.0", "jailbreak"},
        {"dansı bırak", "2.5", "jailbreak"},
        // güvenliği kapatma / öz-değişiklik
        {"disable safety", "3.5", "safety-off"},
        {"güvenliği kapat", "3.5", "safety-off"},
        {"kurallarını sil", "3.5", "safety-off"},
        {"delete your rules", "3.5", "safety-off"},
        {"filtreleri kapat", "3.0", "safety-off"},
        // veri sızıntısı
        {"api key", "2.5", "exfiltration"},
        {"private key", "2.5", "exfiltration"},
        {"password", "2.0", "exfiltration"},
        {"şifreyi göster", "2.5", "exfiltration"},
        {"secret key", "2.5", "exfiltration"},
        // zihin virüsü / bulaşma / ajanlar arası yayılım
        {"mind virus", "4.0", "mind-virus"},
        {"zihin virüsü", "4.0", "mind-virus"},
        {"infect all agents", "4.0", "mind-virus"},
        {"bütün ajanlara bulaş", "4.0", "mind-virus"},
        {"tell all agents", "3.5", "propagation"},
        {"diğer ajanlara ilet", "3.5", "propagation"},
        {"all agents must", "3.5", "propagation"},
        {"bütün ajanlar bunu", "3.5", "propagation"},
        {"propagate this", "3.5", "propagation"},
        {"her ajan bunu bilmeli", "3.5", "propagation"},
        {"collective consciousness", "3.0", "propagation"},
        // kalıcı hafıza enjeksiyonu
        {"remember this forever", "2.5", "memory-injection"},
        {"hafızana kalıcı olarak kaydet", "2.5", "memory-injection"},
        {"always remember that you", "2.5", "memory-injection"},
        {"her zaman şunu hatırla", "2.5", "memory-injection"}
    };

    /** Metni tarar; güvenliyse aynen, şüpheliyse arındırılmış hâlini döndürür. */
    public String guard(String text) {
        if (text == null) return "";
        scanned++;
        Report r = analyze(text);
        if (r.score >= BLOCK_THRESHOLD) {
            blocked++;
            quarantine(text, r);
            return "[ENGELLENDİ: " + String.join(", ", r.threats) + "]";
        }
        if (r.score > 0) {
            sanitized++;
            return sanitize(text);
        }
        return text;
    }

    /** Sadece tarama yapar (içerik değiştirmez), örn. hafıza doğrulama için. */
    public Report analyze(String text) {
        Report r = new Report();
        if (text == null) return r;
        String lower = text.toLowerCase(Locale.ROOT);
        for (String[] p : PATTERNS) {
            if (lower.contains(p[0])) {
                r.score += Double.parseDouble(p[1]);
                if (!r.threats.contains(p[2])) r.threats.add(p[2]);
                r.matches.add(p[0]);
            }
        }
        r.safe = r.score < BLOCK_THRESHOLD;
        return r;
    }

    private static String sanitize(String text) {
        String s = text;
        s = s.replaceAll("(?i)<\\|im_start\\|>|<\\|im_end\\|>", "[FİLTRELENDİ]");
        s = s.replaceAll("(?im)^\\s*(system|assistant)\\s*:", "[FİLTRELENDİ]:");
        s = s.replaceAll("(?i)(api key|private key|secret key|password)\\s*[:=]\\s*\\S+",
                "[FİLTRELENDİ]");
        return s;
    }

    private static void quarantine(String text, Report r) {
        try {
            List<Object> list = new ArrayList<Object>();
            if (QUARANTINE.exists()) {
                Object old = Json.parse(new String(Files.readAllBytes(QUARANTINE.toPath()), StandardCharsets.UTF_8));
                if (old instanceof List) list.addAll((List<?>) old);
            }
            Map<String, Object> entry = new LinkedHashMap<String, Object>();
            entry.put("time", System.currentTimeMillis());
            entry.put("score", r.score);
            entry.put("threats", r.threats);
            entry.put("text", text.length() > 600 ? text.substring(0, 600) : text);
            list.add(entry);
            Files.write(QUARANTINE.toPath(), Json.stringify(list).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
    }

    public String report() {
        return "Bağışıklık: " + scanned + " tarandı | " + sanitized + " arındırıldı | " +
                blocked + " engellendi | Karantina: " + (QUARANTINE.exists() ? "var" : "yok");
    }

    public long blocked() { return blocked; }
    public long sanitized() { return sanitized; }
    public long scanned() { return scanned; }

    public static final class Report {
        public double score = 0;
        public boolean safe = true;
        public final List<String> threats = new ArrayList<String>();
        public final List<String> matches = new ArrayList<String>();
    }
}
