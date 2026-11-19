package agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gelişmiş, bağımlılıksız, Türkçe-odaklı metin işleme motoru.
 * İçerir: normalizasyon, tokenization, gövdeleme (stemming), stopword filtresi,
 * TF-IDF, TextRank benzeri anahtar kelime çıkarımı, çıkarımsal özetleme,
 * sözlük tabanlı duygu analizi,varlık tanıma, okunabilirlik ve çıkarımsal Q&A.
 */
public final class TextProcessor {
    private static final Set<String> STOP = new LinkedHashSet<String>();
    private static final Map<String, Double> POSITIVE = new HashMap<String, Double>();
    private static final Map<String, Double> NEGATIVE = new HashMap<String, Double>();
    private static final String[] SUFFIXES = {
        "larından","lerinden","larını","lerini","larımız","lerimiz","larınız","leriniz",
        "larım","lerim","ların","lerin","ları","leri","lar","ler",
        "ından","inden","undan","ünden","ını","ini","unu","ünü","ı","i","u","ü",
        "dan","den","tan","ten","da","de","ta","te",
        "nın","nin","nun","nün","na","ne","nu","nü","n",
        "a","e","ya","ye","yı","yi","yu","yü","y",
        "ması","mesi","mak","mek","mış","miş","muş","müş","mı","mi","mu","mü",
        "lı","li","lu","lü","la","le","lık","lik","luk","lük"
    };

    static {
        String[] stops = {"ve","bir","bu","şu","o","the","a","an","ile","için","den","dan",
            "de","da","ki","olarak","olan","oldu","olduğu","çok","daha","en","gibi",
            "ancak","fakat","ama","veya","ya","ise","şey","şeyler","var","yok","bana",
            "seni","onu","bizi","sizi","onları","beni","bu","şu","orada","burada",
            "sadece","çünkü","neden","nasıl","kaç","hangi","hangi","olduğunu"};
        for (String s : stops) STOP.add(s);

        String[] pos = {"iyi","güzel","mutlu","faydalı","başarılı","hızlı","zeki","doğru",
            "kolay","seviyorum","memnun","harika","mükemmel","gelişmiş","güçlü","akıllı",
            "temiz","dürüst","açık","verimli","yararlı","hoş","sevinç","neşe"};
        for (String s : pos) POSITIVE.put(s, 1.0);
        String[] neg = {"kötü","çirkin","mut_suz","mutsuz","başarısız","yavaş","yanlış",
            "zor","kötü","sorun","hata","eksik","tehlike","zarar","korku","üzüntü",
            "sinir","bıkkın","zayıf","kapan","engel","risk"};
        for (String s : neg) NEGATIVE.put(s, 1.0);
    }

    public static final class Analysis {
        public String normalized = "";
        public final List<String> tokens = new ArrayList<String>();
        public final List<String> stems = new ArrayList<String>();
        public final List<String> sentences = new ArrayList<String>();
        public final List<String> keyphrases = new ArrayList<String>();
        public final List<String> entities = new ArrayList<String>();
        public String summary = "";
        public double sentiment = 0;
        public double avgWordLength = 0;
        public double lexicalDiversity = 0;
        public double readability = 0;
        public int wordCount = 0;
        public int charCount = 0;

        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Metin Analizi\n");
            sb.append("  Kelime: ").append(wordCount).append(" | Karakter: ").append(charCount).append('\n');
            sb.append("  Ort. kelime uzunluğu: ").append(String.format(Locale.ROOT, "%.1f", avgWordLength)).append('\n');
            sb.append("  Sözcük çeşitliliği: ").append(String.format(Locale.ROOT, "%.2f", lexicalDiversity)).append('\n');
            sb.append("  Okunabilirlik: ").append(String.format(Locale.ROOT, "%.0f", readability)).append("/100\n");
            sb.append("  Duygu: ").append(String.format(Locale.ROOT, "%+.2f", sentiment))
              .append(sentiment > 0.15 ? " (pozitif)" : sentiment < -0.15 ? " (negatif)" : " (nötr)").append('\n');
            sb.append("  Anahtar ifadeler: ").append(String.join(", ", keyphrases)).append('\n');
            sb.append("  Varlıklar: ").append(entities.isEmpty() ? "(yok)" : String.join(", ", entities)).append('\n');
            sb.append("  Özet: ").append(summary).append('\n');
            return sb.toString();
        }
    }

    public static String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("\\s+", " ").trim();
    }

    public static List<String> tokenize(String s) {
        List<String> out = new ArrayList<String>();
        if (s == null) return out;
        String[] parts = normalize(s).toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+");
        for (String p : parts) {
            if (p.length() >= 2 && !STOP.contains(p)) out.add(p);
        }
        return out;
    }

    /** Türkçe ekleri soyut, kural tabanlı gövdeleme. */
    public static String stem(String word) {
        String w = word.toLowerCase(Locale.ROOT);
        if (w.length() <= 4) return w;
        for (String suf : SUFFIXES) {
            if (suf.length() < 2) continue; // tek harfli ekler kökü bozuyor
            if (w.endsWith(suf) && w.length() - suf.length() >= 4) {
                return w.substring(0, w.length() - suf.length());
            }
        }
        return w;
    }

    public static List<String> stems(String s) {
        List<String> out = new ArrayList<String>();
        for (String t : tokenize(s)) out.add(stem(t));
        return out;
    }

    public static List<String> sentences(String s) {
        List<String> out = new ArrayList<String>();
        if (s == null) return out;
        String[] parts = s.split("(?<=[.!?])\\s+");
        for (String p : parts) {
            String t = normalize(p);
            if (t.length() > 3) out.add(t);
        }
        return out;
    }

    /** TF-IDF benzerliği (iki metin arası, gövdelenmiş tokenlar üzerinden). */
    public static double similarity(String a, String b) {
        List<String> ta = stems(a), tb = stems(b);
        if (ta.isEmpty() || tb.isEmpty()) return 0;
        Map<String, Integer> fa = freq(ta), fb = freq(tb);
        double dot = 0, na = 0, nb = 0;
        for (Map.Entry<String, Integer> e : fa.entrySet()) {
            double w = 1.0 + Math.log(e.getValue());
            na += w * w;
            if (fb.containsKey(e.getKey())) {
                dot += w * (1.0 + Math.log(fb.get(e.getKey())));
            }
        }
        for (Integer n : fb.values()) { double w = 1.0 + Math.log(n); nb += w * w; }
        return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-9);
    }

    private static Map<String, Integer> freq(List<String> tokens) {
        Map<String, Integer> m = new HashMap<String, Integer>();
        for (String t : tokens) m.put(t, (m.get(t) == null ? 0 : m.get(t)) + 1);
        return m;
    }

    /** TextRank benzeri: eş-gösterim grafiğinde dereceye göre anahtar kelime. */
    public static List<String> keyphrases(String text, int k) {
        List<String> toks = stems(text);
        Map<String, Integer> f = freq(toks);
        Map<String, Double> score = new HashMap<String, Double>();
        int window = 4;
        for (int i = 0; i < toks.size(); i++) {
            String a = toks.get(i);
            for (int j = i + 1; j < Math.min(i + window + 1, toks.size()); j++) {
                String b = toks.get(j);
                if (a.equals(b)) continue;
                double add = 1.0 / (j - i);
                score.put(a, (score.get(a) == null ? 0 : score.get(a)) + add);
                score.put(b, (score.get(b) == null ? 0 : score.get(b)) + add);
            }
        }
        List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(score.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            @Override public int compare(Map.Entry<String, Double> x, Map.Entry<String, Double> y) {
                return Double.compare(y.getValue(), x.getValue());
            }
        });
        List<String> out = new ArrayList<String>();
        for (Map.Entry<String, Double> e : list) {
            if (out.size() >= k) break;
            if (f.get(e.getKey()) >= 1) out.add(e.getKey());
        }
        return out;
    }

    /** Çıkarımsal özet: anahtar kelime skoruna göre cümle seçer. */
    public static String summarize(String text, int count) {
        List<String> sents = sentences(text);
        if (sents.isEmpty()) return "";
        if (sents.size() <= count) return String.join(" ", sents);
        List<String> keys = keyphrases(text, Math.max(4, count * 3));
        List<ScoredSentence> scored = new ArrayList<ScoredSentence>();
        for (int i = 0; i < sents.size(); i++) {
            List<String> st = stems(sents.get(i));
            double s = 0;
            for (String w : st) if (keys.contains(w)) s += 1.0;
            s = s / (Math.sqrt(st.size()) + 1);
            scored.add(new ScoredSentence(i, s));
        }
        Collections.sort(scored, new Comparator<ScoredSentence>() {
            @Override public int compare(ScoredSentence x, ScoredSentence y) {
                return Double.compare(y.score, x.score);
            }
        });
        List<Integer> picked = new ArrayList<Integer>();
        for (int i = 0; i < Math.min(count, scored.size()); i++) picked.add(scored.get(i).index);
        Collections.sort(picked);
        StringBuilder sb = new StringBuilder();
        for (int idx : picked) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(sents.get(idx));
        }
        return sb.toString();
    }

    /** Sözlük tabanlı duygu skoru (-1..+1). */
    public static double sentiment(String text) {
        List<String> t = tokenize(text);
        double s = 0;
        for (String w : t) {
            if (POSITIVE.containsKey(w)) s += POSITIVE.get(w);
            if (NEGATIVE.containsKey(w)) s -= NEGATIVE.get(w);
        }
        return t.isEmpty() ? 0 : Math.max(-1, Math.min(1, s / Math.sqrt(t.size())));
    }

    /** Büyük harfle başlayan varlık adayları + bilinen grafik varlıkları. */
    public static List<String> entities(String text, java.util.Collection<String> known) {
        List<String> out = new ArrayList<String>();
        if (text == null) return out;
        Matcher m = Pattern.compile("\\b([A-ZÇĞİÖŞÜ][\\p{L}]{2,})\\b").matcher(text);
        while (m.find()) {
            String e = m.group(1);
            if (!out.contains(e)) out.add(e);
        }
        if (known != null) {
            String lower = text.toLowerCase(Locale.ROOT);
            for (String k : known) {
                if (lower.contains(k.toLowerCase(Locale.ROOT)) && !out.contains(k)) out.add(k);
            }
        }
        return out;
    }

    /** Çıkarımsal Q&A: soru kelimeleriyle en çok örtüşen cümleyi döndürür. */
    public static String answer(String question, String context) {
        List<String> q = stems(question);
        List<String> sents = sentences(context);
        String best = null;
        double bestScore = 0;
        for (String s : sents) {
            List<String> st = stems(s);
            int hit = 0;
            for (String w : q) if (st.contains(w)) hit++;
            double score = (double) hit / (Math.sqrt(st.size()) + 1);
            if (score > bestScore) { bestScore = score; best = s; }
        }
        return best == null ? "Bu konuda elimde yeterli metin yok." : best;
    }

    /** Koleksiyondan soruya en ilgili parçaları getirir (anlamsal arama). */
    public static List<String> search(String query, List<String> docs, int k) {
        List<Scored> out = new ArrayList<Scored>();
        for (String d : docs) {
            double s = similarity(query, d);
            if (s > 0.05) out.add(new Scored(d, s));
        }
        Collections.sort(out, new Comparator<Scored>() {
            @Override public int compare(Scored x, Scored y) { return Double.compare(y.s, x.s); }
        });
        List<String> res = new ArrayList<String>();
        for (int i = 0; i < Math.min(k, out.size()); i++) res.add(out.get(i).text);
        return res;
    }

    public static Analysis analyze(String text, java.util.Collection<String> knownEntities) {
        Analysis a = new Analysis();
        a.normalized = normalize(text);
        a.tokens.addAll(tokenize(text));
        a.stems.addAll(stems(text));
        a.sentences.addAll(sentences(text));
        a.keyphrases.addAll(keyphrases(text, 8));
        a.entities.addAll(entities(text, knownEntities));
        a.summary = summarize(text, 2);
        a.sentiment = sentiment(text);
        a.charCount = text == null ? 0 : text.length();
        a.wordCount = a.tokens.size();
        a.avgWordLength = a.tokens.isEmpty() ? 0 :
                (double) sumLengths(a.tokens) / a.tokens.size();
        a.lexicalDiversity = a.tokens.isEmpty() ? 0 :
                (double) new LinkedHashSet<String>(a.tokens).size() / a.tokens.size();
        a.readability = Math.max(0, Math.min(100,
                100 - a.avgWordLength * 4 - a.sentences.size() * 1.5));
        return a;
    }

    private static int sumLengths(List<String> l) {
        int n = 0;
        for (String s : l) n += s.length();
        return n;
    }

    private static final class ScoredSentence {
        final int index; final double score;
        ScoredSentence(int index, double score) { this.index = index; this.score = score; }
    }
    private static final class Scored {
        final String text; final double s;
        Scored(String text, double s) { this.text = text; this.s = s; }
    }
}
