package neuralnet;

import java.util.Random;

/** Sıfırdan yazılmış sinir ağının two-moons verisi üzerinde eğitim demosu. */
public final class Main {
    public static void main(String[] args) {
        Random rng = new Random(42);

        Dataset all = Dataset.twoMoons(600, 0.18, rng);
        Dataset[] split = Dataset.split(all, 0.75, rng);
        Dataset train = split[0];
        Dataset test = split[1];

        int[] sizes = {2, 16, 16, 2};
        Activation[] acts = {
            Activation.RELU,
            Activation.RELU,
            Activation.SOFTMAX
        };

        NeuralNetwork nn = new NeuralNetwork(sizes, acts, Loss.CROSS_ENTROPY, 0.01, 7);

        Matrix xTrain = train.features();
        Matrix yTrain = train.oneHot(2);
        Matrix xTest = test.features();

        System.out.println("Mimari: 2 -> 16 (ReLU) -> 16 (ReLU) -> 2 (Softmax)");
        System.out.println("Optimizer: Adam (lr=0.01), loss: CrossEntropy, batch=32");
        System.out.printf("Eğitim: %d örnek, Test: %d örnek%n%n", train.size(), test.size());

        int epochs = 300;
        for (int e = 1; e <= epochs; e++) {
            nn.train(xTrain, yTrain, 1, 32);
            if (e == 1 || e % 30 == 0) {
                System.out.printf("epoch %3d | loss %.4f | train acc %.3f | test acc %.3f%n",
                        e,
                        nn.loss(xTrain, yTrain),
                        nn.accuracy(xTrain, train.labels()),
                        nn.accuracy(xTest, test.labels()));
            }
        }

        int[] pred = nn.predict(xTest);
        int[] truth = test.labels();
        System.out.println("\nİlk 10 test tahmini (tahmin / gerçek):");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(10, pred.length); i++) {
            sb.append(pred[i]).append('/').append(truth[i]).append("  ");
        }
        System.out.println(sb.toString().trim());
    }
}
