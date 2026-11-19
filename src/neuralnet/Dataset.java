package neuralnet;

import java.util.Random;

/** Özellik matrisi + sınıf etiketleri (0..C-1) tutan basit veri kabı. */
public final class Dataset {
    private final double[][] x; // N x F
    private final int[] y;      // N

    public Dataset(double[][] x, int[] y) {
        if (x.length != y.length) throw new IllegalArgumentException("X ve y satır sayıları farklı.");
        this.x = x;
        this.y = y.clone();
    }

    public Matrix features() { return Matrix.from2D(x); }
    public int[] labels() { return y.clone(); }
    public int size() { return y.length; }

    /** Sınıf indekslerini one-hot matrisine çevirir (N x classes). */
    public Matrix oneHot(int classes) {
        Matrix m = new Matrix(y.length, classes);
        for (int i = 0; i < y.length; i++) {
            m.set(i, y[i], 1.0);
        }
        return m;
    }

    /** İki aylık (two moons) sentetik ikili sınıflandırma verisi. */
    public static Dataset twoMoons(int n, double noise, Random rng) {
        double[][] xs = new double[n][2];
        int[] ys = new int[n];
        for (int i = 0; i < n; i++) {
            double theta = rng.nextDouble() * Math.PI;
            if (i % 2 == 0) {
                xs[i][0] = Math.cos(theta);
                xs[i][1] = Math.sin(theta);
                ys[i] = 0;
            } else {
                xs[i][0] = 1.0 - Math.cos(theta);
                xs[i][1] = 0.5 - Math.sin(theta);
                ys[i] = 1;
            }
            xs[i][0] += rng.nextGaussian() * noise;
            xs[i][1] += rng.nextGaussian() * noise;
        }
        return new Dataset(xs, ys);
    }

    /** Veriyi karıştırıp eğitim/test olarak böler. */
    public static Dataset[] split(Dataset d, double trainFraction, Random rng) {
        int n = d.size();
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = order[i]; order[i] = order[j]; order[j] = tmp;
        }
        int nTrain = (int) Math.round(n * trainFraction);
        double[][] xtr = new double[nTrain][2];
        double[][] xte = new double[n - nTrain][2];
        int[] ytr = new int[nTrain];
        int[] yte = new int[n - nTrain];
        for (int k = 0; k < n; k++) {
            int src = order[k];
            if (k < nTrain) {
                xtr[k] = d.x[src].clone();
                ytr[k] = d.y[src];
            } else {
                int t = k - nTrain;
                xte[t] = d.x[src].clone();
                yte[t] = d.y[src];
            }
        }
        return new Dataset[]{new Dataset(xtr, ytr), new Dataset(xte, yte)};
    }
}
