package agent;

import java.util.Locale;

/** LLM tabanlı kısa genelleme benchmark'ı (Turing tarzı sorular + bilgi/reasoning). */
public final class BenchmarkHarness {
    private final OllamaClient client;

    public BenchmarkHarness(OllamaClient client) { this.client = client; }

    public String run() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Benchmark (").append(client.model()).append(") ===\n\n");
        int correct = 0;

        for (int i = 0; i < QUESTIONS.length; i++) {
            String[] q = QUESTIONS[i];
            String system =
                "Aşağıdaki çoktan seçmeli soruyu cevapla. SADECE doğru seçeneğin harfini yaz (A, B, C veya D). " +
                "Açıklama veya başka metin yazma.";
            String user = q[0] + "\n" + q[1];
            String answer = "";
            try { answer = client.ask(system, user, 0.0).trim(); }
            catch (Exception e) { answer = "HATA"; }

            String letter = extractLetter(answer);
            boolean ok = letter.equalsIgnoreCase(q[2]);
            if (ok) correct++;
            sb.append(String.format(Locale.ROOT, "%d) %s -> %s (beklenen %s)%n",
                    i + 1, ok ? "PASS" : "FAIL", answer, q[2]));
        }

        sb.append(String.format(Locale.ROOT, "\nSONUÇ: %d/%d (%%%.0f)%n",
                correct, QUESTIONS.length, 100.0 * correct / QUESTIONS.length));
        sb.append("Not: Bu benchmark modelin genelleme yeteneğini ölçer; ajan katmanlarını değil.");
        return sb.toString();
    }

    private static String extractLetter(String s) {
        String up = s.toUpperCase(java.util.Locale.ROOT);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b([ABCD])\\s*[\\).:-]").matcher(up);
        if (m.find()) return m.group(1);
        String t = up.trim();
        if (t.isEmpty()) return "?";
        char c = t.charAt(0);
        return (c == 'A' || c == 'B' || c == 'C' || c == 'D') ? String.valueOf(c) : "?";
    }

    private static final String[][] QUESTIONS = {
        {"Bir sayının 3 katına 5 eklenirse 26 olur. Bu sayı kaçtır?",
         "A) 6   B) 7   C) 8   D) 9", "B"},
        {"Aşağıdakilerden hangisi bir programlama dilidir?",
         "A) HTTP   B) Python   C) SQL sorgusu   D) HTML etiketi", "B"},
        {"Güneş sisteminde hangi gezegen Güneş'e en yakındır?",
         "A) Venüs   B) Dünya   C) Merkür   D) Mars", "C"},
        {"Bir üçgenin iç açıları toplamı kaç derecedir?",
         "A) 90   B) 180   C) 270   D) 360", "B"},
        {"'Yapay zeka' terimi ilk kez hangi yıl ortaya atılmıştır?",
         "A) 1943   B) 1950   C) 1956   D) 1969", "C"},
        {"Bir dizideki en büyük elemanı bulmak için kullanılabilecek en basit yöntem nedir?",
         "A) Doğrusal tarama   B) İkili arama   C) Kabarcık sıralama   D) Özyineleme", "A"},
        {"Bir sayının yarısı, üçte birinden 5 fazlaysa bu sayı kaçtır?",
         "A) 20   B) 24   C) 30   D) 36", "C"},
        {"A, B'den hızlı; B, C'den hızlıdır. En yavaş olan kimdir?",
         "A) A   B) B   C) C   D) Hepsi eşit", "C"},
        {"2^5 + 3^2 işleminin sonucu kaçtır?",
         "A) 41   B) 43   C) 49   D) 51", "A"},
        {"Bir işçi 6 günde 18 ürün yaparsa, aynı hızla 10 günde kaç ürün yapar?",
         "A) 24   B) 30   C) 36   D) 40", "B"},
        {"Bir kutu içinde 3 kırmızı, 2 mavi top vardır. Rastgele çekilen ilk topun mavi olma olasılığı kaçtır?",
         "A) 1/5   B) 2/5   C) 3/5   D) 1/2", "B"}
    };
}
