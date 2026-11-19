package neuralnet;

import java.util.Random;

/** Tam bağlantılı (dense) katman: Z = X*W + b. Geri yayılım için girişi önbelleğe alır. */
public final class DenseLayer {
    private final int in;
    private final int out;
    private Matrix w;
    private Matrix b;
    private Matrix inputCache;

    public DenseLayer(int in, int out, Init init, Random rng) {
        this.in = in;
        this.out = out;
        double scale = (init == Init.HE)
                ? Math.sqrt(6.0 / in)          // He uniform
                : Math.sqrt(6.0 / (in + out)); // Glorot / Xavier uniform
        this.w = Matrix.random(in, out, scale, rng);
        this.b = Matrix.zeros(1, out);
    }

    public Matrix forward(Matrix x) {
        this.inputCache = x; // A_(i-1)
        return x.mul(w).addRowVector(b);
    }

    /** dZ (N x out) -> dW, db ve dA_(i-1). Zincir kuralının katman kısmı. */
    public Backward backward(Matrix dZ) {
        Matrix dW = inputCache.transpose().mul(dZ); // (in x N)(N x out) = in x out
        Matrix db = dZ.columnSums();               // 1 x out
        Matrix dX = dZ.mul(w.transpose());         // (N x out)(out x in) = N x in
        return new Backward(dW, db, dX);
    }

    public Matrix weights() { return w; }
    public Matrix biases() { return b; }
    public void setWeights(Matrix w) { this.w = w; }
    public void setBiases(Matrix b) { this.b = b; }
    public int inputSize() { return in; }
    public int outputSize() { return out; }

    public enum Init { HE, XAVIER }

    public static final class Backward {
        public final Matrix dW;
        public final Matrix db;
        public final Matrix dX;
        private Backward(Matrix dW, Matrix db, Matrix dX) {
            this.dW = dW;
            this.db = db;
            this.dX = dX;
        }
    }
}
