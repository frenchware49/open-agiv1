package neuralnet;

/** Kayıp fonksiyonları; değer ve çıktıya göre gradyan (dA) üretir. */
public enum Loss {
    MSE {
        @Override
        public double value(Matrix a, Matrix y) {
            double s = 0.0;
            for (int i = 0; i < a.rows(); i++) {
                for (int j = 0; j < a.cols(); j++) {
                    double d = a.get(i, j) - y.get(i, j);
                    s += d * d;
                }
            }
            return 0.5 * s / (a.rows() * a.cols());
        }
        @Override
        public Matrix derivative(Matrix a, Matrix y) {
            return a.sub(y).scale(1.0 / (a.rows() * a.cols()));
        }
    },
    CROSS_ENTROPY {
        private static final double EPS = 1e-12;
        @Override
        public double value(Matrix a, Matrix y) {
            double s = 0.0;
            for (int i = 0; i < a.rows(); i++) {
                for (int j = 0; j < a.cols(); j++) {
                    s -= y.get(i, j) * Math.log(Math.max(a.get(i, j), EPS));
                }
            }
            return s / a.rows();
        }
        @Override
        public Matrix derivative(Matrix a, Matrix y) {
            Matrix r = new Matrix(a.rows(), a.cols());
            int n = a.rows();
            for (int i = 0; i < a.rows(); i++) {
                for (int j = 0; j < a.cols(); j++) {
                    r.set(i, j, -y.get(i, j) / (Math.max(a.get(i, j), EPS) * n));
                }
            }
            return r;
        }
    };

    public abstract double value(Matrix a, Matrix y);
    public abstract Matrix derivative(Matrix a, Matrix y);
}
