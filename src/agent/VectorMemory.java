package agent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Harici embedding modeli gerektirmeyen vektör hafıza.
 * Kelime + bigram hashing ile metni 96 boyutlu vektöre çevirir,
 * kosinüs benzerliğiyle anlamsal (anahtar kelime bazlı) arama yapar.
 */
public final class VectorMemory {
    private static final int DIM = 96;
    private static final double MIN_SCORE = 0.12;

    private final File file;
    private final List<Entry> entries = new ArrayList<Entry>();

    public VectorMemory(String path) {
        this.file = new File(path);
        load();
    }

    public int size() { return entries.size(); }

    /** Tüm anıları listeler (CLI gösterimi için). */
    public String listAll() {
        if (entries.isEmpty()) return "(hafıza boş)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            sb.append(i + 1).append(". ").append(entries.get(i).text).append('\n');
        }
        return sb.toString().trim();
    }

    public void add(String text) {
        String t = text.trim();
        if (t.isEmpty()) return;
        entries.add(new Entry(t, embed(t)));
        save();
    }

    public void clear() {
        entries.clear();
        if (file.exists() && !file.delete()) file.deleteOnExit();
    }

    /** Sorguya en yakın k anıyı sistem prompt'una eklenecek metin olarak döndürür. */
    public String contextFor(String query, int k) {
        if (entries.isEmpty()) return "";
        double[] q = embed(query);
        List<Scored> scored = new ArrayList<Scored>();
        for (Entry e : entries) {
            double s = cosine(q, e.vector);
            if (s >= MIN_SCORE) scored.add(new Scored(e.text, s));
        }
        if (scored.isEmpty()) return "";
        Collections.sort(scored, new Comparator<Scored>() {
            @Override public int compare(Scored a, Scored b) {
                return Double.compare(b.score, a.score);
            }
        });
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(k, scored.size()); i++) {
            Scored s = scored.get(i);
            sb.append("- [benzersizlik ").append(String.format(Locale.ROOT, "%.2f", s.score))
              .append("] ").append(s.text).append('\n');
        }
        return sb.toString();
    }

    public List<String> search(String query, int k) {
        List<String> out = new ArrayList<String>();
        String ctx = contextFor(query, k);
        for (String line : ctx.split("\n")) {
            int idx = line.indexOf("] ");
            if (idx > 0) out.add(line.substring(idx + 2));
        }
        return out;
    }

    private static double[] embed(String text) {
        double[] v = new double[DIM];
        String[] tokens = text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+");
        List<String> words = new ArrayList<String>();
        for (String t : tokens) if (!t.isEmpty()) words.add(t);
        for (String w : words) {
            v[bucket(w)] += 1.0;
        }
        for (int i = 0; i + 1 < words.size(); i++) {
            v[bucket(words.get(i) + "_" + words.get(i + 1))] += 0.5;
        }
        double norm = 0.0;
        for (double d : v) norm += d * d;
        norm = Math.sqrt(norm);
        if (norm > 0) for (int i = 0; i < DIM; i++) v[i] /= norm;
        return v;
    }

    private static int bucket(String s) {
        return (s.hashCode() & 0x7fffffff) % DIM;
    }

    private static double cosine(double[] a, double[] b) {
        double dot = 0.0;
        for (int i = 0; i < DIM; i++) dot += a[i] * b[i];
        return dot; // vektörler normalize edildi
    }

    private void save() {
        try {
            List<Object> list = new ArrayList<Object>();
            for (Entry e : entries) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("text", e.text);
                List<Double> vec = new ArrayList<Double>();
                for (double d : e.vector) vec.add(d);
                m.put("v", vec);
                list.add(m);
            }
            Files.write(file.toPath(), Json.stringify(list).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            System.out.println("  [Uyarı] Vektör hafıza kaydedilemedi: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!file.exists()) return;
        try {
            String s = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Object parsed = Json.parse(s);
            if (!(parsed instanceof List)) return;
            for (Object o : (List<Object>) parsed) {
                Map<String, Object> m = Json.asObject(o);
                if (m == null) continue;
                String text = Json.str(m.get("text"));
                Object vecObj = m.get("v");
                if (text == null || !(vecObj instanceof List)) continue;
                double[] vec = new double[DIM];
                int i = 0;
                for (Object d : (List<Object>) vecObj) {
                    if (i >= DIM) break;
                    vec[i++] = ((Number) d).doubleValue();
                }
                entries.add(new Entry(text, vec));
            }
        } catch (Exception ex) {
            System.out.println("  [Uyarı] Vektör hafıza okunamadı: " + ex.getMessage());
        }
    }

    private static final class Entry {
        final String text;
        final double[] vector;
        Entry(String text, double[] vector) { this.text = text; this.vector = vector; }
    }

    private static final class Scored {
        final String text;
        final double score;
        Scored(String text, double score) { this.text = text; this.score = score; }
    }
}
