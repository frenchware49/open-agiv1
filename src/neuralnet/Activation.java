package neuralnet;

/**
 * Aktivasyon fonksiyonları. Her fonksiyon ileri uygulama ve geri yayılım için
 * element-wise türev sağlar (dA -> dZ dönüşümü).
 */
public enum Activation {
    LINEAR {
        @Override public Matrix apply(Matrix z) { return z.copy(); }
        @Override public Matrix derivative(Matrix z, Matrix a) { return z.map(v -> 1.0); }
    },
    SIGMOID {
        @Override public Matrix apply(Matrix z) {
            return z.map(v -> 1.0 / (1.0 + Math.exp(-clip(v))));
        }
        @Override public Matrix derivative(Matrix z, Matrix a) {
            return a.map(v -> v * (1.0 - v));
        }
    },
    TANH {
        @Override public Matrix apply(Matrix z) { return z.map(Math::tanh); }
        @Override public Matrix derivative(Matrix z, Matrix a) { return a.map(v -> 1.0 - v * v); }
    },
    RELU {
        @Override public Matrix apply(Matrix z) { return z.map(v -> Math.max(0.0, v)); }
        @Override public Matrix derivative(Matrix z, Matrix a) { return z.map(v -> v > 0.0 ? 1.0 : 0.0); }
    },
    LEAKY_RELU {
        private static final double ALPHA = 0.01;
        @Override public Matrix apply(Matrix z) { return z.map(v -> v > 0.0 ? v : ALPHA * v); }
        @Override public Matrix derivative(Matrix z, Matrix a) { return z.map(v -> v > 0.0 ? 1.0 : ALPHA); }
    },
    SOFTMAX {
        @Override public Matrix apply(Matrix z) {
            Matrix r = new Matrix(z.rows(), z.cols());
            for (int i = 0; i < z.rows(); i++) {
                double max = Double.NEGATIVE_INFINITY;
                for (int j = 0; j < z.cols(); j++) {
                    max = Math.max(max, z.get(i, j));
                }
                double sum = 0.0;
                for (int j = 0; j < z.cols(); j++) {
                    double e = Math.exp(z.get(i, j) - max); // sayısal kararlılık
                    r.set(i, j, e);
                    sum += e;
                }
                for (int j = 0; j < z.cols(); j++) {
                    r.set(i, j, r.get(i, j) / sum);
                }
            }
            return r;
        }
        @Override public Matrix derivative(Matrix z, Matrix a) {
            throw new UnsupportedOperationException(
                "Softmax için element-wise türev tanımsızdır (Jacobi gerekir). " +
                "CrossEntropy ile birleşik gradyan kullanılmalıdır.");
        }
    };

    private static double clip(double v) {
        return Math.max(-50.0, Math.min(50.0, v));
    }

    public abstract Matrix apply(Matrix z);
    public abstract Matrix derivative(Matrix z, Matrix a);
}
