package neuralnet;

import java.util.function.DoubleUnaryOperator;

/**
 * Yoğun matris işlemleri. Satır-baskın (row-major) düzen; satırlar örnek, sütunlar özellik.
 * Değişmez (immutable) davranır: çoğu metod yeni Matrix döndürür, {@link #set} hariç.
 */
public final class Matrix {
    private final int rows;
    private final int cols;
    private final double[][] data;

    public Matrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.data = new double[rows][cols];
    }

    private Matrix(int rows, int cols, double[][] data) {
        this.rows = rows;
        this.cols = cols;
        this.data = data;
    }

    public static Matrix zeros(int rows, int cols) {
        return new Matrix(rows, cols);
    }

    /** [-scale, scale] aralığında üniform rastgele değerler (Glorot/He uniform için uygun). */
    public static Matrix random(int rows, int cols, double scale, java.util.Random rng) {
        Matrix m = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                m.data[i][j] = (rng.nextDouble() * 2.0 - 1.0) * scale;
            }
        }
        return m;
    }

    public static Matrix from2D(double[][] arr) {
        int r = arr.length;
        int c = r == 0 ? 0 : arr[0].length;
        Matrix m = new Matrix(r, c);
        for (int i = 0; i < r; i++) {
            System.arraycopy(arr[i], 0, m.data[i], 0, c);
        }
        return m;
    }

    public int rows() { return rows; }
    public int cols() { return cols; }

    public double get(int i, int j) { return data[i][j]; }
    public void set(int i, int j, double v) { data[i][j] = v; }

    /** Yerinde güncelleme: this = a*this + b*other (Adam birikimi için). */
    public void update(double a, double b, Matrix other) {
        check(rows == other.rows && cols == other.cols, "update: boyut uyuşmuyor");
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                data[i][j] = a * data[i][j] + b * other.data[i][j];
            }
        }
    }

    public Matrix copy() {
        Matrix m = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            System.arraycopy(data[i], 0, m.data[i], 0, cols);
        }
        return m;
    }

    public Matrix mul(Matrix o) {
        check(o.rows == cols, "mul: boyut uyuşmuyor");
        Matrix r = new Matrix(rows, o.cols);
        for (int i = 0; i < rows; i++) {
            for (int k = 0; k < cols; k++) {
                double v = data[i][k];
                if (v == 0.0) continue;
                for (int j = 0; j < o.cols; j++) {
                    r.data[i][j] += v * o.data[k][j];
                }
            }
        }
        return r;
    }

    public Matrix add(Matrix o) {
        check(rows == o.rows && cols == o.cols, "add: boyut uyuşmuyor");
        Matrix r = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                r.data[i][j] = data[i][j] + o.data[i][j];
            }
        }
        return r;
    }

    public Matrix sub(Matrix o) {
        check(rows == o.rows && cols == o.cols, "sub: boyut uyuşmuyor");
        Matrix r = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                r.data[i][j] = data[i][j] - o.data[i][j];
            }
        }
        return r;
    }

    /** 1 x cols satır vektörünü her satıra ekler (bias broadcast). */
    public Matrix addRowVector(Matrix v) {
        check(v.rows == 1 && v.cols == cols, "addRowVector: boyut uyuşmuyor");
        Matrix r = copy();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                r.data[i][j] += v.data[0][j];
            }
        }
        return r;
    }

    public Matrix transpose() {
        Matrix r = new Matrix(cols, rows);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                r.data[j][i] = data[i][j];
            }
        }
        return r;
    }

    public Matrix map(DoubleUnaryOperator f) {
        Matrix r = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                r.data[i][j] = f.applyAsDouble(data[i][j]);
            }
        }
        return r;
    }

    public Matrix elementWiseMul(Matrix o) {
        check(rows == o.rows && cols == o.cols, "elementWiseMul: boyut uyuşmuyor");
        Matrix r = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                r.data[i][j] = data[i][j] * o.data[i][j];
            }
        }
        return r;
    }

    public Matrix scale(double s) {
        return map(v -> v * s);
    }

    /** Sütun bazlı toplam: 1 x cols matris döndürür (db gradyanı). */
    public Matrix columnSums() {
        Matrix r = new Matrix(1, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                r.data[0][j] += data[i][j];
            }
        }
        return r;
    }

    public double sum() {
        double s = 0.0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                s += data[i][j];
            }
        }
        return s;
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new IllegalArgumentException(msg);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            sb.append('[');
            for (int j = 0; j < cols; j++) {
                if (j > 0) sb.append(", ");
                sb.append(String.format("%.4f", data[i][j]));
            }
            sb.append(']');
            if (i < rows - 1) sb.append('\n');
        }
        return sb.toString();
    }
}
