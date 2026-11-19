package ownmodel;

import neuralnet.Matrix;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Sıfırdan, saf Java ile yazılmış küçük bir GPT benzeri dil modeli.
 * Karakter bazlı tokenization, konum kodlaması, çok başlı öz-dikkat,
 * residual + layer norm, feed-forward katman ve tam manuel geri yayılım.
 * Hiçbir harici kütüphane kullanmaz; ağırlıklar kendi kodumuzdur.
 */
public final class MiniTransformer {
    private static final double EPS = 1e-5;

    private final int vocabSize;
    private final int seqLen;
    private final int dim;
    private final int heads;
    private final int headDim;
    private final int ffDim;
    private final int layers;
    private final String chars;
    private final Map<Character, Integer> charToId = new LinkedHashMap<Character, Integer>();

    private final Matrix[] Wq, Wk, Wv, Wo, W1, b1, W2, b2;
    private final Matrix We, Wout, bout;
    private final List<Matrix> params = new ArrayList<Matrix>();
    private final List<Matrix> mState = new ArrayList<Matrix>();
    private final List<Matrix> vState = new ArrayList<Matrix>();
    private int adamT = 0;
    private final double lr;
    private final Random rng;

    // ileri geçiş önbelleği
    private Matrix[] cacheX, cacheS1, cacheX2, cacheQ, cacheK, cacheV,
            cacheO, cacheZ, cacheS2, cacheF, cacheOut;
    private Matrix[][] cacheAttn;

    public MiniTransformer(String corpus, int seqLen, int dim, int heads, int ffDim,
                           int layers, double lr, long seed) {
        this.seqLen = seqLen;
        this.dim = dim;
        this.heads = heads;
        this.headDim = dim / heads;
        this.ffDim = ffDim;
        this.layers = layers;
        this.lr = lr;
        this.rng = new Random(seed);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < corpus.length(); i++) {
            char c = corpus.charAt(i);
            if (!charToId.containsKey(c)) charToId.put(c, charToId.size());
        }
        StringBuilder alphabet = new StringBuilder();
        for (Character c : charToId.keySet()) alphabet.append(c);
        this.chars = alphabet.toString();
        this.vocabSize = chars.length();

        double scale = 0.08;
        Wq = new Matrix[layers]; Wk = new Matrix[layers]; Wv = new Matrix[layers];
        Wo = new Matrix[layers]; W1 = new Matrix[layers]; b1 = new Matrix[layers];
        W2 = new Matrix[layers]; b2 = new Matrix[layers];
        for (int l = 0; l < layers; l++) {
            Wq[l] = Matrix.random(dim, dim, scale, rng);
            Wk[l] = Matrix.random(dim, dim, scale, rng);
            Wv[l] = Matrix.random(dim, dim, scale, rng);
            Wo[l] = Matrix.random(dim, dim, scale, rng);
            W1[l] = Matrix.random(dim, ffDim, scale, rng);
            b1[l] = Matrix.zeros(1, ffDim);
            W2[l] = Matrix.random(ffDim, dim, scale, rng);
            b2[l] = Matrix.zeros(1, dim);
        }
        We = Matrix.random(vocabSize, dim, scale, rng);
        Wout = Matrix.random(dim, vocabSize, scale, rng);
        bout = Matrix.zeros(1, vocabSize);

        for (Matrix[] arr : new Matrix[][]{Wq, Wk, Wv, Wo, W1, b1, W2, b2}) {
            for (Matrix m : arr) addParam(m);
        }
        addParam(We); addParam(Wout); addParam(bout);
    }

    private void addParam(Matrix p) {
        params.add(p);
        mState.add(Matrix.zeros(p.rows(), p.cols()));
        vState.add(Matrix.zeros(p.rows(), p.cols()));
    }

    public int vocabSize() { return vocabSize; }
    public String alphabet() { return chars; }
    public int idOf(char c) {
        Integer id = charToId.get(c);
        return id == null ? 0 : id;
    }

    /** Tek eğitim adımı; ortalama cross-entropy kaybını döndürür. */
    public double trainStep(int[] tokens, int[] targets) {
        Matrix embed = new Matrix(seqLen, dim);
        for (int t = 0; t < seqLen; t++) {
            int id = tokens[t];
            for (int d = 0; d < dim; d++) {
                double pos = 0.01 * Math.sin(t * 0.01 * (d + 1)); // basit konum kodlaması
                embed.set(t, d, We.get(id, d) + pos);
            }
        }

        cacheX = new Matrix[layers + 1];
        cacheS1 = new Matrix[layers]; cacheX2 = new Matrix[layers];
        cacheQ = new Matrix[layers]; cacheK = new Matrix[layers]; cacheV = new Matrix[layers];
        cacheAttn = new Matrix[layers][]; cacheO = new Matrix[layers]; cacheZ = new Matrix[layers];
        cacheS2 = new Matrix[layers]; cacheF = new Matrix[layers];
        cacheOut = new Matrix[layers + 1];
        cacheX[0] = embed;

        for (int l = 0; l < layers; l++) {
            Matrix x = cacheX[l];
            Matrix q = x.mul(Wq[l]), k = x.mul(Wk[l]), v = x.mul(Wv[l]);
            Matrix a = new Matrix(seqLen, dim);
            cacheAttn[l] = new Matrix[heads];
            for (int h = 0; h < heads; h++) {
                Matrix qh = sliceCols(q, h * headDim, headDim);
                Matrix kh = sliceCols(k, h * headDim, headDim);
                Matrix vh = sliceCols(v, h * headDim, headDim);
                Matrix scores = qh.mul(kh.transpose()).scale(1.0 / Math.sqrt(headDim));
                for (int i = 0; i < seqLen; i++) {
                    for (int j = i + 1; j < seqLen; j++) scores.set(i, j, -1e9); // causal mask
                }
                Matrix attn = rowSoftmax(scores);
                cacheAttn[l][h] = attn;
                Matrix out = attn.mul(vh);
                for (int i = 0; i < seqLen; i++) {
                    for (int j = 0; j < headDim; j++) {
                        a.set(i, h * headDim + j, out.get(i, j));
                    }
                }
            }
            Matrix z = a.mul(Wo[l]);
            Matrix s1 = x.add(z);
            Matrix x2 = layerNorm(s1);
            Matrix f = x2.mul(W1[l]).addRowVector(b1[l]).map(vv -> Math.max(0.0, vv));
            Matrix g = f.mul(W2[l]).addRowVector(b2[l]);
            Matrix s2 = x2.add(g);
            Matrix out = layerNorm(s2);

            cacheQ[l] = q; cacheK[l] = k; cacheV[l] = v;
            cacheO[l] = a; cacheZ[l] = z;
            cacheS1[l] = s1; cacheX2[l] = x2; cacheS2[l] = s2; cacheF[l] = f;
            cacheX[l + 1] = out;
            cacheOut[l + 1] = out;
        }

        Matrix last = cacheX[layers];
        Matrix logits = last.mul(Wout).addRowVector(bout);
        Matrix prob = rowSoftmax(logits);

        double loss = 0.0;
        Matrix dLogits = new Matrix(seqLen, vocabSize);
        for (int t = 0; t < seqLen; t++) {
            int target = targets[t];
            double p = Math.max(prob.get(t, target), 1e-9);
            loss -= Math.log(p);
            for (int c = 0; c < vocabSize; c++) {
                dLogits.set(t, c, prob.get(t, c) / seqLen);
            }
            dLogits.set(t, target, dLogits.get(t, target) - 1.0 / seqLen);
        }
        loss /= seqLen;

        backward(dLogits, tokens);
        adamStep();
        return loss;
    }

    private void backward(Matrix dLogits, int[] tokens) {
        Matrix dX = dLogits.mul(Wout.transpose());
        grad(Wout, cacheX[layers].transpose().mul(dLogits));
        grad(bout, rowSums(dLogits));

        for (int l = layers - 1; l >= 0; l--) {
            Matrix dS2 = layerNormBackward(dX, cacheS2[l]);
            Matrix dG = dS2;
            Matrix dX2fromRes = dS2;

            grad(W2[l], cacheF[l].transpose().mul(dG));
            grad(b2[l], rowSums(dG));
            Matrix dF = dG.mul(W2[l].transpose());
            Matrix dPre = dF.elementWiseMul(cacheF[l].map(v -> v > 0 ? 1.0 : 0.0));
            grad(W1[l], cacheX2[l].transpose().mul(dPre));
            grad(b1[l], rowSums(dPre));
            Matrix dX2fromFF = dPre.mul(W1[l].transpose());

            Matrix dX2 = dX2fromRes.add(dX2fromFF);
            Matrix dS1 = layerNormBackward(dX2, cacheS1[l]);
            Matrix dXres = dS1;
            Matrix dZ = dS1;

            grad(Wo[l], cacheO[l].transpose().mul(dZ));
            Matrix dO = dZ.mul(Wo[l].transpose());

            Matrix dQ = new Matrix(seqLen, dim);
            Matrix dK = new Matrix(seqLen, dim);
            Matrix dV = new Matrix(seqLen, dim);
            for (int h = 0; h < heads; h++) {
                int off = h * headDim;
                Matrix qh = sliceCols(cacheQ[l], off, headDim);
                Matrix kh = sliceCols(cacheK[l], off, headDim);
                Matrix vh = sliceCols(cacheV[l], off, headDim);
                Matrix dh = sliceCols(dO, off, headDim);
                Matrix attn = cacheAttn[l][h];

                Matrix dVh = attn.transpose().mul(dh);
                Matrix dAh = dh.mul(vh.transpose());
                Matrix dSh = new Matrix(seqLen, seqLen);
                for (int i = 0; i < seqLen; i++) {
                    double rowDot = 0.0;
                    for (int j = 0; j < seqLen; j++) {
                        rowDot += attn.get(i, j) * dAh.get(i, j);
                    }
                    for (int j = 0; j < seqLen; j++) {
                        dSh.set(i, j, attn.get(i, j) * (dAh.get(i, j) - rowDot) / Math.sqrt(headDim));
                    }
                }
                Matrix dQh = dSh.mul(kh);
                Matrix dKh = dSh.transpose().mul(qh);
                for (int i = 0; i < seqLen; i++) {
                    for (int j = 0; j < headDim; j++) {
                        dQ.set(i, off + j, dQh.get(i, j));
                        dK.set(i, off + j, dKh.get(i, j));
                        dV.set(i, off + j, dVh.get(i, j));
                    }
                }
            }
            grad(Wq[l], cacheX[l].transpose().mul(dQ));
            grad(Wk[l], cacheX[l].transpose().mul(dK));
            grad(Wv[l], cacheX[l].transpose().mul(dV));

            Matrix dXattn = dQ.mul(Wq[l].transpose())
                    .add(dK.mul(Wk[l].transpose()))
                    .add(dV.mul(Wv[l].transpose()));
            dX = dXres.add(dXattn);
        }

        // gömme katmanı gradyanı
        Matrix dWe = new Matrix(We.rows(), We.cols());
        for (int t = 0; t < seqLen; t++) {
            int id = tokens[t];
            for (int d = 0; d < dim; d++) {
                dWe.set(id, d, dWe.get(id, d) + dX.get(t, d));
            }
        }
        grad(We, dWe);
    }

    private final Map<Matrix, Matrix> grads = new java.util.IdentityHashMap<Matrix, Matrix>();

    private void grad(Matrix param, Matrix g) {
        Matrix acc = grads.get(param);
        grads.put(param, acc == null ? g : acc.add(g));
    }

    private void adamStep() {
        adamT++;
        double bc1 = 1.0 - Math.pow(0.9, adamT);
        double bc2 = 1.0 - Math.pow(0.999, adamT);
        for (int i = 0; i < params.size(); i++) {
            Matrix p = params.get(i);
            Matrix g = grads.get(p);
            if (g == null) continue;
            Matrix m = mState.get(i), v = vState.get(i);
            m.update(0.9, 0.1, g);
            Matrix g2 = g.map(x -> x * x);
            v.update(0.999, 0.001, g2);
            for (int r = 0; r < p.rows(); r++) {
                for (int c = 0; c < p.cols(); c++) {
                    double mh = m.get(r, c) / bc1;
                    double vh = Math.sqrt(v.get(r, c) / bc2) + 1e-8;
                    p.set(r, c, p.get(r, c) - lr * mh / vh);
                }
            }
        }
        grads.clear();
    }

    /** Prompt'tan devam ederek metin üretir (greedy). */
    public String generate(String prompt, int length) {
        StringBuilder sb = new StringBuilder(prompt);
        for (int n = 0; n < length; n++) {
            int[] tokens = new int[seqLen];
            int start = Math.max(0, sb.length() - seqLen);
            for (int t = 0; t < seqLen; t++) {
                int pos = start + t;
                char c = pos < sb.length() ? sb.charAt(pos) : ' ';
                Integer id = charToId.get(c);
                tokens[t] = id == null ? 0 : id;
            }
            Matrix embed = new Matrix(seqLen, dim);
            for (int t = 0; t < seqLen; t++) {
                for (int d = 0; d < dim; d++) {
                    embed.set(t, d, We.get(tokens[t], d) + 0.01 * Math.sin(t * 0.01 * (d + 1)));
                }
            }
            Matrix x = embed;
            for (int l = 0; l < layers; l++) {
                Matrix q = x.mul(Wq[l]), k = x.mul(Wk[l]), v = x.mul(Wv[l]);
                Matrix a = new Matrix(seqLen, dim);
                for (int h = 0; h < heads; h++) {
                    int off = h * headDim;
                    Matrix scores = sliceCols(q, off, headDim)
                            .mul(sliceCols(k, off, headDim).transpose())
                            .scale(1.0 / Math.sqrt(headDim));
                    for (int i = 0; i < seqLen; i++) {
                        for (int j = i + 1; j < seqLen; j++) scores.set(i, j, -1e9);
                    }
                    Matrix attn = rowSoftmax(scores);
                    Matrix o = attn.mul(sliceCols(v, off, headDim));
                    for (int i = 0; i < seqLen; i++) {
                        for (int j = 0; j < headDim; j++) a.set(i, off + j, o.get(i, j));
                    }
                }
                Matrix s1 = x.add(a.mul(Wo[l]));
                Matrix x2 = layerNorm(s1);
                Matrix f = x2.mul(W1[l]).addRowVector(b1[l]).map(z -> Math.max(0.0, z));
                x = layerNorm(x2.add(f.mul(W2[l]).addRowVector(b2[l])));
            }
            Matrix logits = x.mul(Wout).addRowVector(bout);
            Matrix prob = rowSoftmax(logits);
            int best = 0;
            for (int c = 1; c < vocabSize; c++) {
                if (prob.get(seqLen - 1, c) > prob.get(seqLen - 1, best)) best = c;
            }
            sb.append(chars.charAt(best));
        }
        return sb.toString();
    }

    public void save(String path) throws Exception {
        List<Object> list = new ArrayList<Object>();
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("chars", chars);
        root.put("seqLen", seqLen);
        root.put("dim", dim);
        root.put("heads", heads);
        root.put("ffDim", ffDim);
        root.put("layers", layers);
        List<Object> weights = new ArrayList<Object>();
        for (Matrix p : params) weights.add(toList(p));
        root.put("weights", weights);
        list.add(root);
        Files.write(new File(path).toPath(), JsonLite.stringify(list).getBytes(StandardCharsets.UTF_8));
    }

    private static List<Object> toList(Matrix m) {
        List<Object> rows = new ArrayList<Object>();
        for (int i = 0; i < m.rows(); i++) {
            List<Object> row = new ArrayList<Object>();
            for (int j = 0; j < m.cols(); j++) row.add(m.get(i, j));
            rows.add(row);
        }
        return rows;
    }

    // ---------- yardımcılar ----------

    private static Matrix sliceCols(Matrix m, int off, int len) {
        Matrix r = new Matrix(m.rows(), len);
        for (int i = 0; i < m.rows(); i++) {
            for (int j = 0; j < len; j++) r.set(i, j, m.get(i, off + j));
        }
        return r;
    }

    private static Matrix rowSoftmax(Matrix m) {
        Matrix r = new Matrix(m.rows(), m.cols());
        for (int i = 0; i < m.rows(); i++) {
            double max = Double.NEGATIVE_INFINITY;
            for (int j = 0; j < m.cols(); j++) max = Math.max(max, m.get(i, j));
            double sum = 0.0;
            for (int j = 0; j < m.cols(); j++) {
                double e = Math.exp(m.get(i, j) - max);
                r.set(i, j, e);
                sum += e;
            }
            for (int j = 0; j < m.cols(); j++) r.set(i, j, r.get(i, j) / sum);
        }
        return r;
    }

    private static Matrix rowSums(Matrix m) {
        Matrix r = new Matrix(1, m.cols());
        for (int i = 0; i < m.rows(); i++) {
            for (int j = 0; j < m.cols(); j++) r.set(0, j, r.get(0, j) + m.get(i, j));
        }
        return r;
    }

    private static Matrix layerNorm(Matrix x) {
        Matrix r = new Matrix(x.rows(), x.cols());
        for (int i = 0; i < x.rows(); i++) {
            double mean = 0.0;
            for (int j = 0; j < x.cols(); j++) mean += x.get(i, j);
            mean /= x.cols();
            double var = 0.0;
            for (int j = 0; j < x.cols(); j++) {
                double d = x.get(i, j) - mean;
                var += d * d;
            }
            var /= x.cols();
            double s = Math.sqrt(var + EPS);
            for (int j = 0; j < x.cols(); j++) r.set(i, j, (x.get(i, j) - mean) / s);
        }
        return r;
    }

    private static Matrix layerNormBackward(Matrix dy, Matrix x) {
        Matrix dx = new Matrix(x.rows(), x.cols());
        for (int i = 0; i < x.rows(); i++) {
            double mean = 0.0, dymean = 0.0;
            for (int j = 0; j < x.cols(); j++) {
                mean += x.get(i, j);
                dymean += dy.get(i, j);
            }
            mean /= x.cols();
            dymean /= x.cols();
            double var = 0.0;
            for (int j = 0; j < x.cols(); j++) {
                double d = x.get(i, j) - mean;
                var += d * d;
            }
            var /= x.cols();
            double s = Math.sqrt(var + EPS);
            double dot = 0.0;
            for (int j = 0; j < x.cols(); j++) {
                dot += dy.get(i, j) * (x.get(i, j) - mean) / s;
            }
            dot /= x.cols();
            for (int j = 0; j < x.cols(); j++) {
                double xhat = (x.get(i, j) - mean) / s;
                dx.set(i, j, (dy.get(i, j) - dymean - xhat * dot) / s);
            }
        }
        return dx;
    }

    /** JSON yazıcı (agent.Json'a bağımlı olmadan, bağımsız modül için). */
    /** Kaydedilmiş ağırlıklarla yeni bir model oluşturur (çıkarım için). */
    public static MiniTransformer load(String path, String corpus) throws Exception {
        String json = new String(Files.readAllBytes(new File(path).toPath()), StandardCharsets.UTF_8);
        Object parsed = JsonLite.parse(json);
        List<?> root = (List<?>) parsed;
        Map<?, ?> r = (Map<?, ?>) root.get(0);
        String chars = (String) r.get("chars");
        int seqLen = ((Number) r.get("seqLen")).intValue();
        int dim = ((Number) r.get("dim")).intValue();
        int heads = ((Number) r.get("heads")).intValue();
        int ff = ((Number) r.get("ffDim")).intValue();
        int layers = ((Number) r.get("layers")).intValue();
        MiniTransformer m = new MiniTransformer(corpus, seqLen, dim, heads, ff, layers, 0.0, 1);
        if (!m.chars.equals(chars)) throw new IllegalStateException("Corpus karakter kümesi değişmiş.");
        List<?> weights = (List<?>) r.get("weights");
        for (int i = 0; i < m.params.size(); i++) {
            List<?> rows = (List<?>) weights.get(i);
            Matrix p = m.params.get(i);
            for (int a = 0; a < rows.size(); a++) {
                List<?> cols = (List<?>) rows.get(a);
                for (int b = 0; b < cols.size(); b++) {
                    p.set(a, b, ((Number) cols.get(b)).doubleValue());
                }
            }
        }
        return m;
    }

    private static final class JsonLite {
        static Object parse(String s) { return new P(s).value(); }
        static String stringify(Object o) {
            StringBuilder sb = new StringBuilder();
            write(o, sb);
            return sb.toString();
        }
        private static void write(Object o, StringBuilder sb) {
            if (o == null) sb.append("null");
            else if (o instanceof String) {
                sb.append('"');
                for (char c : ((String) o).toCharArray()) {
                    if (c == '"' || c == '\\') sb.append('\\').append(c);
                    else if (c == '\n') sb.append("\\n");
                    else sb.append(c);
                }
                sb.append('"');
            } else if (o instanceof Number || o instanceof Boolean) sb.append(o);
            else if (o instanceof Iterable) {
                sb.append('[');
                boolean first = true;
                for (Object v : (Iterable<?>) o) {
                    if (!first) sb.append(','); first = false; write(v, sb);
                }
                sb.append(']');
            } else if (o instanceof Map) {
                sb.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                    if (!first) sb.append(','); first = false;
                    write(String.valueOf(e.getKey()), sb); sb.append(':'); write(e.getValue(), sb);
                }
                sb.append('}');
            } else sb.append('"').append(o).append('"');
        }

        /** Minimal JSON okuyucu (sadece kaydedilen model formatı için). */
        static final class P {
            private final String s;
            private int i;
            P(String s) { this.s = s; }
            Object value() {
                skip();
                char c = s.charAt(i);
                if (c == '[') return arr();
                if (c == '{') return obj();
                if (c == '"') return str();
                return num();
            }
            private List<Object> arr() {
                List<Object> a = new ArrayList<Object>();
                i++; skip();
                if (s.charAt(i) == ']') { i++; return a; }
                while (i < s.length()) {
                    a.add(value()); skip();
                    if (i < s.length() && s.charAt(i) == ',') { i++; skip(); continue; }
                    if (i < s.length() && s.charAt(i) == ']') { i++; break; }
                }
                return a;
            }
            private Map<String, Object> obj() {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                i++; skip();
                while (i < s.length() && s.charAt(i) != '}') {
                    String k = str(); skip();
                    if (s.charAt(i) == ':') i++;
                    skip();
                    m.put(k, value()); skip();
                    if (i < s.length() && s.charAt(i) == ',') { i++; skip(); }
                }
                i++;
                return m;
            }
            private String str() {
                skip();
                if (s.charAt(i) != '"') throw new IllegalArgumentException("string bekleniyor");
                i++;
                StringBuilder sb = new StringBuilder();
                while (i < s.length()) {
                    char c = s.charAt(i++);
                    if (c == '"') break;
                    if (c == '\\' && i < s.length()) {
                        char e = s.charAt(i++);
                        if (e == 'n') sb.append('\n');
                        else if (e == 't') sb.append('\t');
                        else sb.append(e);
                    } else sb.append(c);
                }
                return sb.toString();
            }
            private Object num() {
                int start = i;
                while (i < s.length() && "+-.eE0123456789".indexOf(s.charAt(i)) >= 0) i++;
                String n = s.substring(start, i);
                if (n.indexOf('.') < 0 && n.indexOf('e') < 0) return Long.valueOf(n);
                return Double.valueOf(n);
            }
            private void skip() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        }
    }
}
