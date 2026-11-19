package agent;

import neuralnet.Activation;
import neuralnet.Loss;
import neuralnet.Matrix;
import neuralnet.NeuralNetwork;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Kullanıcı mesajının niyetini sınıflandıran yerel sinir ağı + sürekli öğrenme.
 *
 * Özellikler sabit boyutlu hashing ile üretilir; böylece yeni kelimeler
 * gelince ağ mimarisi değişmez ve ağırlıklar canlı olarak güncellenebilir.
 * Geri bildirimle gelen yeni örnekler experience replay (eski örneklerle
 * birlikte düşük lr ile kısa yeniden eğitim) kullanılarak işlenir.
 */
public final class IntentRouter {
    private static final int DIMS = 512;
    private static final String[] INTENTS = {"matematik", "dosya", "kod", "bilgi", "sohbet"};
    private static final String STORE = "learned-intents.json";

    private final NeuralNetwork net;
    private final MetaLearner meta = new MetaLearner();
    private final List<Example> learned = new ArrayList<Example>();
    private final List<double[]> learnedVectors = new ArrayList<double[]>();
    private final List<Example> base;

    public IntentRouter(long seed) {
        this.base = loadBase();
        loadLearned();
        rebuildLearnedVectors();
        this.net = new NeuralNetwork(
                new int[]{DIMS, 32, INTENTS.length},
                new Activation[]{Activation.RELU, Activation.SOFTMAX},
                Loss.CROSS_ENTROPY, 0.01, seed);
        this.net.train(features(allExamples()), labels(allExamples()), 600, 16);
    }

    public Result route(String text) {
        Matrix p = net.forward(featuresFromTexts(singleton(text)));
        int best = 0;
        for (int j = 1; j < INTENTS.length; j++) {
            if (p.get(0, j) > p.get(0, best)) best = j;
        }
        Result slow = new Result(INTENTS[best], p.get(0, best));

        // Hızlı ağırlıklar: tek örnekten öğrenme. Yeni örnek ağa işlenmeden önce
        // benzer bir öğrenilmiş örnek varsa onun kararını anında uygula.
        if (!learnedVectors.isEmpty()) {
            double[] q = embed(text);
            int match = -1;
            double bestSim = 0.80;
            for (int i = 0; i < learnedVectors.size(); i++) {
                double sim = dot(q, learnedVectors.get(i));
                if (sim > bestSim) { bestSim = sim; match = i; }
            }
            if (match >= 0) {
                return new Result(learned.get(match).intent, Math.max(slow.confidence, bestSim));
            }
        }
        return slow;
    }

    /** Metni normalize edilmiş özellik vektörüne çevirir (hızlı eşleştirme için). */
    public double[] embed(String text) {
        Matrix m = featuresFromTexts(singleton(text));
        double[] v = new double[DIMS];
        for (int j = 0; j < DIMS; j++) v[j] = m.get(0, j);
        return v;
    }

    private static double dot(double[] a, double[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    private void rebuildLearnedVectors() {
        learnedVectors.clear();
        for (Example e : learned) learnedVectors.add(embed(e.text));
    }

    public String metaReport() { return meta.report(); }

    public double trainingAccuracy() { return accuracy(allExamples()); }
    public int learnedCount() { return learned.size(); }

    public java.util.List<String> learnedExamples() {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (Example e : learned) out.add("[" + e.intent + "] " + e.text);
        return out;
    }
    public double learnedAccuracy() { return accuracy(learned); }

    /** Yeni bir etiketli örnekle ağı canlı olarak günceller. */
    public LearnResult learn(String text, String intent) {
        int label = indexOf(INTENTS, intent);
        if (label < 0 || text == null || text.trim().isEmpty()) {
            return new LearnResult(false, "Geçersiz niyet. Şunlardan biri: " + String.join(", ", INTENTS));
        }

        long t0 = System.currentTimeMillis();
        double accBefore = trainingAccuracy();
        learned.add(new Example(text.trim(), intent));
        learnedVectors.add(embed(text.trim()));
        saveLearned();

        MetaLearner.Params params = meta.suggest(); // meta-öğrenme: stratejiyi kendisi seçer
        double oldLr = net.learningRate();
        net.setLearningRate(params.lr);
        List<Example> all = allExamples();
        int epochs = (int) (params.epochs * Math.min(params.replayBoost, 2.0));
        net.train(features(all), labels(all), epochs, 8); // experience replay
        net.setLearningRate(oldLr);
        meta.record(1, accBefore, trainingAccuracy(), System.currentTimeMillis() - t0, params);

        Result r = route(text);
        return new LearnResult(true, "Öğrenildi. Bu metin artık '" + r.intent +
                "' olarak sınıflanıyor (güven " + String.format(Locale.ROOT, "%.2f", r.confidence) +
                "). Toplam doğruluk %" + String.format(Locale.ROOT, "%.1f", trainingAccuracy() * 100) + ".");
    }

    private double accuracy(List<Example> data) {
        if (data.isEmpty()) return 1.0;
        int correct = 0;
        for (Example e : data) {
            if (route(e.text).intent.equals(e.intent)) correct++;
        }
        return (double) correct / data.size();
    }

    private List<Example> allExamples() {
        List<Example> all = new ArrayList<Example>(base);
        all.addAll(learned);
        return all;
    }

    private static Matrix features(List<Example> data) {
        List<String> texts = new ArrayList<String>();
        for (Example e : data) texts.add(e.text);
        return featuresFromTexts(texts);
    }

    private static Matrix featuresFromTexts(List<String> texts) {
        Matrix m = new Matrix(texts.size(), DIMS);
        for (int i = 0; i < texts.size(); i++) {
            List<String> words = tokens(texts.get(i));
            for (String w : words) {
                int b = bucket(w);
                m.set(i, b, m.get(i, b) + 1.0);
            }
            for (int k = 0; k + 1 < words.size(); k++) {
                int b = bucket(words.get(k) + "_" + words.get(k + 1));
                m.set(i, b, m.get(i, b) + 0.5);
            }
            // Türkçe eklenmeli yapı için karakter 3-gram'ları: "teşekkürler" ~ "teşekkür ederim"
            StringBuilder joined = new StringBuilder();
            for (String w : words) joined.append(w);
            String s = joined.toString();
            for (int k = 0; k + 2 < s.length(); k++) {
                int b = bucket("#" + s.substring(k, k + 3));
                m.set(i, b, m.get(i, b) + 0.15);
            }
            double sum = 0.0;
            for (int j = 0; j < DIMS; j++) sum += m.get(i, j);
            if (sum > 0) {
                for (int j = 0; j < DIMS; j++) m.set(i, j, m.get(i, j) / sum);
            }
        }
        return m;
    }

    private static Matrix labels(List<Example> data) {
        Matrix y = new Matrix(data.size(), INTENTS.length);
        for (int i = 0; i < data.size(); i++) {
            y.set(i, indexOf(INTENTS, data.get(i).intent), 1.0);
        }
        return y;
    }

    private static int bucket(String s) {
        return (s.hashCode() & 0x7fffffff) % DIMS;
    }

    private static int indexOf(String[] a, String v) {
        for (int i = 0; i < a.length; i++) if (a[i].equals(v)) return i;
        return -1;
    }

    private static List<String> singleton(String s) {
        List<String> l = new ArrayList<String>();
        l.add(s == null ? "" : s);
        return l;
    }

    private static List<String> tokens(String s) {
        List<String> out = new ArrayList<String>();
        String[] parts = s.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+");
        for (String p : parts) if (!p.isEmpty()) out.add(p);
        return out;
    }

    private void saveLearned() {
        try {
            List<Object> list = new ArrayList<Object>();
            for (Example e : learned) {
                Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
                m.put("text", e.text);
                m.put("intent", e.intent);
                list.add(m);
            }
            Files.write(new File(STORE).toPath(), Json.stringify(list).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            System.out.println("  [Uyarı] Öğrenilen örnekler kaydedilemedi: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadLearned() {
        File f = new File(STORE);
        if (!f.exists()) return;
        try {
            String s = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            Object parsed = Json.parse(s);
            if (!(parsed instanceof List)) return;
            for (Object o : (List<Object>) parsed) {
                Map<String, Object> m = Json.asObject(o);
                if (m == null) continue;
                String text = Json.str(m.get("text"));
                String intent = Json.str(m.get("intent"));
                if (text != null && intent != null && indexOf(INTENTS, intent) >= 0) {
                    learned.add(new Example(text, intent));
                }
            }
        } catch (Exception ex) {
            System.out.println("  [Uyarı] Öğrenilen örnekler okunamadı: " + ex.getMessage());
        }
    }

    private static List<Example> loadBase() {
        String[][] data = trainingData();
        List<Example> list = new ArrayList<Example>();
        for (String[] row : data) list.add(new Example(row[0], row[1]));
        return list;
    }

    private static String[][] trainingData() {
        return new String[][]{
            {"2+2 kaç eder", "matematik"},
            {"toplamı hesapla", "matematik"},
            {"çarpma işlemi yap", "matematik"},
            {"bölme yap kaç çıkar", "matematik"},
            {"hesap makinesi aç", "matematik"},
            {"125 ile 8 çarpımı kaç", "matematik"},
            {"yüzde hesapla", "matematik"},
            {"karekök al", "matematik"},
            {"faktöriyel hesapla", "matematik"},
            {"denklem çöz", "matematik"},
            {"kaçtır bu işlem sonucu", "matematik"},
            {"sayıları topla çıkar", "matematik"},
            {"çarpı kaç yapar", "matematik"},
            {"5 çarpı 6 kaç eder", "matematik"},
            {"bölü kaç yapar", "matematik"},

            {"dosyayı oku", "dosya"},
            {"klasörü listele", "dosya"},
            {"dosya yaz kaydet", "dosya"},
            {"içeriği göster", "dosya"},
            {"bir dosya oluştur", "dosya"},
            {"config txt dosyasını aç", "dosya"},
            {"klasördeki dosyaları göster", "dosya"},
            {"readme dosyasını oku", "dosya"},
            {"yeni dosya oluştur", "dosya"},
            {"dosya içeriğini değiştir", "dosya"},
            {"listele klasör", "dosya"},
            {"path dosya kaydet", "dosya"},

            {"java kodu yaz", "kod"},
            {"fonksiyon oluştur", "kod"},
            {"hata ayıkla debug", "kod"},
            {"kod örneği ver", "kod"},
            {"sınıf yaz", "kod"},
            {"programlama sorusu", "kod"},
            {"derleme hatası veriyor", "kod"},
            {"metod yaz", "kod"},
            {"algoritma oluştur", "kod"},
            {"python kodu yaz", "kod"},
            {"kodunu düzenle", "kod"},
            {"script yaz", "kod"},
            {"fonksiyon çalışmıyor", "kod"},
            {"kod hatalı çalışıyor", "kod"},
            {"metod hata veriyor", "kod"},

            {"hava durumu nasıl", "bilgi"},
            {"saat kaç", "bilgi"},
            {"tarih nedir bugün", "bilgi"},
            {"haber ver", "bilgi"},
            {"bilgi ver", "bilgi"},
            {"internetten bul", "bilgi"},
            {"araştır bul", "bilgi"},
            {"sıcaklık kaç derece", "bilgi"},
            {"güncel bilgi ver", "bilgi"},
            {"nedir bu konu", "bilgi"},
            {"kimdir bu kişi", "bilgi"},
            {"nerede bulunur", "bilgi"},

            {"merhaba", "sohbet"},
            {"nasılsın", "sohbet"},
            {"teşekkürler", "sohbet"},
            {"güle güle", "sohbet"},
            {"hakkında ne düşünüyorsun", "sohbet"},
            {"biraz sohbet edelim", "sohbet"},
            {"günaydın", "sohbet"},
            {"iyi günler", "sohbet"},
            {"naber", "sohbet"},
            {"hoş geldin", "sohbet"},
            {"tanışalım", "sohbet"},
            {"tebrikler", "sohbet"},
            {"çok teşekkür ederim", "sohbet"},
            {"sağ ol", "sohbet"},
            {"iyi günler dilerim", "sohbet"}
        };
    }

    private static final class Example {
        final String text;
        final String intent;
        Example(String text, String intent) { this.text = text; this.intent = intent; }
    }

    public static final class Result {
        public final String intent;
        public final double confidence;
        Result(String intent, double confidence) { this.intent = intent; this.confidence = confidence; }
    }

    public static final class LearnResult {
        public final boolean success;
        public final String message;
        LearnResult(boolean success, String message) { this.success = success; this.message = message; }
    }
}
