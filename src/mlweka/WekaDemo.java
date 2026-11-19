package mlweka;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Random;

/**
 * Weka ile makine öğrenmesi örneği: sentetik 3 sınıflı veri üzerinde
 * J48 karar ağacı, 10-kat çapraz doğrulama ve tek örnek tahmini.
 *
 * Derleme/çalıştırma için lib/weka.jar dosyasını classpath'e ekleyin
 * (bkz. build-weka.bat veya README.md).
 */
public final class WekaDemo {
    public static void main(String[] args) throws Exception {
        Instances data = buildData(450);
        data.setClassIndex(data.numAttributes() - 1);

        J48 tree = new J48();
        tree.setOptions(new String[]{"-C", "0.25", "-M", "2"}); // confidence, min instances

        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(tree, data, 10, new Random(1));

        System.out.println("=== 10-kat çapraz doğrulama (J48) ===");
        System.out.println(eval.toSummaryString("\nÖzet\n====\n", false));
        System.out.println(eval.toClassDetailsString("\nSınıf bazlı\n===========\n"));
        System.out.println(eval.toMatrixString("\nKarışıklık matrisi\n==================\n"));

        tree.buildClassifier(data);
        Instance sample = new DenseInstance(1.0, new double[]{0.4, 2.6, 0.0, 0.0, 0.0});
        sample.setDataset(data);
        double predicted = tree.classifyInstance(sample);
        String label = data.classAttribute().value((int) predicted);
        System.out.println("Tek örnek tahmini: " + label);
    }

    /** Üç Gauss kümesinden 2 özellikli, 3 sınıflı veri üretir. */
    private static Instances buildData(int n) {
        ArrayList<Attribute> attrs = new ArrayList<Attribute>();
        attrs.add(new Attribute("x"));
        attrs.add(new Attribute("y"));
        ArrayList<String> classes = new ArrayList<String>();
        classes.add("A");
        classes.add("B");
        classes.add("C");
        attrs.add(new Attribute("sinif", classes));

        Instances data = new Instances("sentetik", attrs, n);
        Random rng = new Random(123);
        double[][] centers = {{0.0, 0.0}, {3.0, 3.0}, {0.0, 4.0}};
        for (int i = 0; i < n; i++) {
            int c = i % 3;
            double[] vals = new double[3];
            vals[0] = centers[c][0] + rng.nextGaussian() * 0.55;
            vals[1] = centers[c][1] + rng.nextGaussian() * 0.55;
            vals[2] = c; // nominal sınıf indeksi: 0=A, 1=B, 2=C
            data.add(new DenseInstance(1.0, vals));
        }
        return data;
    }
}
