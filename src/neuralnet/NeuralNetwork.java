package neuralnet;

import java.util.Random;

/**
 * Sıfırdan yazılmış çok katmanlı perceptron (MLP).
 *
 * Özellikler:
 *  - Mini-batch SGD + Adam (bias-corrected moment tahmini)
 *  - He/Xavier uniform ağırlık başlatma
 *  - Softmax + CrossEntropy için birleşik (bircik) gradyan: dZ = (A - Y)/N
 *  - Softmax numerically stable (satır maksimumu çıkarılır)
 */
public final class NeuralNetwork {
    private final DenseLayer[] layers;
    private final Activation[] acts;
    private final Loss loss;
    private final boolean comboSoftmaxCE;
    private double lr;
    private final double beta1;
    private final double beta2;
    private final double eps;
    private final Random rng;

    // Adam durumları (katman başına)
    private final Matrix[][] mW, vW, mB, vB;
    private int t = 0;

    // İleri geçiş önbelleği
    private Matrix[] zCache;
    private Matrix[] aCache;

    public NeuralNetwork(int[] sizes, Activation[] acts, Loss loss, double lr, long seed) {
        if (sizes.length < 2) throw new IllegalArgumentException("En az giriş + çıkış boyutu gerekli.");
        if (acts.length != sizes.length - 1) {
            throw new IllegalArgumentException("Aktivasyon sayısı katman sayısına eşit olmalı.");
        }
        this.acts = acts.clone();
        this.loss = loss;
        this.lr = lr;
        this.beta1 = 0.9;
        this.beta2 = 0.999;
        this.eps = 1e-8;
        this.rng = new Random(seed);
        this.comboSoftmaxCE = (acts[acts.length - 1] == Activation.SOFTMAX && loss == Loss.CROSS_ENTROPY);

        int L = sizes.length - 1;
        this.layers = new DenseLayer[L];
        this.mW = new Matrix[L][];
        this.vW = new Matrix[L][];
        this.mB = new Matrix[L][];
        this.vB = new Matrix[L][];
        for (int i = 0; i < L; i++) {
            boolean relu = (acts[i] == Activation.RELU || acts[i] == Activation.LEAKY_RELU);
            DenseLayer.Init init = relu ? DenseLayer.Init.HE : DenseLayer.Init.XAVIER;
            layers[i] = new DenseLayer(sizes[i], sizes[i + 1], init, rng);

            int fanIn = sizes[i], fanOut = sizes[i + 1];
            mW[i] = new Matrix[]{Matrix.zeros(fanIn, fanOut)};
            vW[i] = new Matrix[]{Matrix.zeros(fanIn, fanOut)};
            mB[i] = new Matrix[]{Matrix.zeros(1, fanOut)};
            vB[i] = new Matrix[]{Matrix.zeros(1, fanOut)};
        }
    }

    /** Tüm ağın ileri geçişi; Z ve A değerlerini geri yayılım için önbelleğe alır. */
    public Matrix forward(Matrix x) {
        Matrix a = x;
        zCache = new Matrix[layers.length];
        aCache = new Matrix[layers.length];
        for (int i = 0; i < layers.length; i++) {
            Matrix z = layers[i].forward(a);
            Matrix out = acts[i].apply(z);
            zCache[i] = z;
            aCache[i] = out;
            a = out;
        }
        return a;
    }

    /** Her satır için en yüksek olasılıklı sınıf indeksini döndürür. */
    public int[] predict(Matrix x) {
        Matrix p = forward(x);
        int[] out = new int[p.rows()];
        for (int i = 0; i < p.rows(); i++) {
            int best = 0;
            for (int j = 1; j < p.cols(); j++) {
                if (p.get(i, j) > p.get(i, best)) best = j;
            }
            out[i] = best;
        }
        return out;
    }

    public double loss(Matrix x, Matrix y) {
        return loss.value(forward(x), y);
    }

    /** yLabels: satır başına sınıf indeksi (one-hot değil). */
    public double accuracy(Matrix x, int[] yLabels) {
        int[] p = predict(x);
        int correct = 0;
        for (int i = 0; i < p.length; i++) {
            if (p[i] == yLabels[i]) correct++;
        }
        return (double) correct / p.length;
    }

    /** Epoch sayısı ve batch boyutu ile eğitim; her epoch'ta veri karıştırılır. */
    public void train(Matrix x, Matrix y, int epochs, int batchSize) {
        int n = x.rows();
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;

        for (int e = 0; e < epochs; e++) {
            fisherYates(order);
            for (int start = 0; start < n; start += batchSize) {
                int end = Math.min(start + batchSize, n);
                Matrix xb = new Matrix(end - start, x.cols());
                Matrix yb = new Matrix(end - start, y.cols());
                for (int k = start; k < end; k++) {
                    int src = order[k];
                    copyRow(x, src, xb, k - start);
                    copyRow(y, src, yb, k - start);
                }
                trainBatch(xb, yb);
            }
        }
    }

    private void trainBatch(Matrix x, Matrix y) {
        Matrix out = forward(x);
        Matrix dZ;
        if (comboSoftmaxCE) {
            // L = -sum(y log softmax(z))/N  =>  dL/dz = (softmax(z) - y)/N
            dZ = out.sub(y).scale(1.0 / x.rows());
        } else {
            Matrix dA = loss.derivative(out, y);
            int last = layers.length - 1;
            dZ = dA.elementWiseMul(acts[last].derivative(zCache[last], aCache[last]));
        }

        Matrix[] dWs = new Matrix[layers.length];
        Matrix[] dbs = new Matrix[layers.length];
        for (int i = layers.length - 1; i >= 0; i--) {
            DenseLayer.Backward b = layers[i].backward(dZ);
            dWs[i] = b.dW;
            dbs[i] = b.db;
            if (i > 0) {
                Matrix dAprev = b.dX; // A_(i-1) üzerindeki gradyan
                dZ = dAprev.elementWiseMul(acts[i - 1].derivative(zCache[i - 1], aCache[i - 1]));
            }
        }

        t++;
        double bc1 = 1.0 - Math.pow(beta1, t);
        double bc2 = 1.0 - Math.pow(beta2, t);
        for (int i = 0; i < layers.length; i++) {
            adam(layers[i].weights(), mW[i][0], vW[i][0], dWs[i], bc1, bc2);
            adam(layers[i].biases(), mB[i][0], vB[i][0], dbs[i], bc1, bc2);
        }
    }

    /** Adam parametre güncellemesi: m,v birikimleri yerinde güncellenir, param yerinde değişir. */
    private void adam(Matrix param, Matrix m, Matrix v, Matrix grad, double bc1, double bc2) {
        m.update(beta1, 1.0 - beta1, grad);
        Matrix gradSq = grad.map(g -> g * g);
        v.update(beta2, 1.0 - beta2, gradSq);
        for (int i = 0; i < param.rows(); i++) {
            for (int j = 0; j < param.cols(); j++) {
                double mHat = m.get(i, j) / bc1;
                double vHat = v.get(i, j) / bc2;
                param.set(i, j, param.get(i, j) - lr * mHat / (Math.sqrt(vHat) + eps));
            }
        }
    }

    private void fisherYates(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    private static void copyRow(Matrix src, int srcRow, Matrix dst, int dstRow) {
        for (int j = 0; j < src.cols(); j++) {
            dst.set(dstRow, j, src.get(srcRow, j));
        }
    }

    public double learningRate() { return lr; }
    public void setLearningRate(double lr) {
        if (lr <= 0) throw new IllegalArgumentException("Öğrenme oranı pozitif olmalı.");
        this.lr = lr;
    }

    public DenseLayer layer(int i) { return layers[i]; }
}
