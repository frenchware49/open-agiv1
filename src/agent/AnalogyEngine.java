package agent;

import java.util.List;
import java.util.Locale;

/**
 * Analoji / transfer motoru: bilinen bir ilişki kalıbını yeni bir duruma taşır.
 * Dar AI'da bilgi tek alanda kalır; burada yapısal benzerlik aranır.
 */
public final class AnalogyEngine {
    public String infer(String text, Agent agent) {
        List<String> facts = agent.graphRelations();
        if (facts.isEmpty()) return "";

        String lower = text.toLowerCase(Locale.ROOT);
        String best = null;
        int bestScore = 0;
        for (String f : facts) {
            String fl = f.toLowerCase(Locale.ROOT);
            int score = 0;
            for (String word : fl.split("[^\\p{L}\\p{N}]+")) {
                if (word.length() > 2 && lower.contains(word)) score++;
            }
            if (score > bestScore) { bestScore = score; best = f; }
        }
        if (best == null || bestScore == 0) return "";

        return "[Analoji/transfer] Benzer bir kalıp biliyorum: " + best +
               ". Bu yapıyı yeni duruma uyarlayarak sonuç çıkarılabilir.";
    }
}
