package ownmodel;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;

/** Kendi mini transformer modelimizi corpus.txt üzerinde eğitir ve örnek üretir. */
public final class Train {
    public static void main(String[] args) {
        try {
            String corpus = new String(Files.readAllBytes(
                    new File("corpus.txt").toPath()), StandardCharsets.UTF_8);
            int steps = args.length > 0 ? Integer.parseInt(args[0]) : 500;

            System.out.println("Kendi modelimiz eğitiliyor...");
            System.out.println("Corpus: " + corpus.length() + " karakter, " +
                    new java.util.HashSet<Character>() {{ for (char c : corpus.toCharArray()) add(c); }}.size()
                    + " benzersiz karakter\n");

            long t0 = System.currentTimeMillis();
            MiniTransformer model = new MiniTransformer(corpus, 24, 32, 2, 64, 1, 0.002, 42);
            System.out.println("Parametre boyutu: vocab=" + model.vocabSize() +
                    ", seq=24, dim=32, heads=2, layers=1\n");

            Random rng = new Random(7);
            double total = 0;
            int[] tokens = new int[24];
            int[] targets = new int[24];
            for (int step = 1; step <= steps; step++) {
                int start = rng.nextInt(corpus.length() - 25);
                for (int t = 0; t < 24; t++) {
                    tokens[t] = model.idOf(corpus.charAt(start + t));
                    targets[t] = model.idOf(corpus.charAt(start + t + 1));
                }
                double loss = model.trainStep(tokens, targets);
                total += loss;
                if (step % 50 == 0 || step == 1) {
                    System.out.printf("adım %4d | kayıp %.4f | ort %.4f%n",
                            step, loss, total / step);
                }
            }

            model.save("own-model.json");
            System.out.println("\nModel kaydedildi: own-model.json");
            System.out.println("Süre: " + (System.currentTimeMillis() - t0) / 1000 + " sn\n");
            System.out.println("=== Örnek üretim ===");
            System.out.println(model.generate("yapay zeka", 180));
        } catch (Exception e) {
            System.out.println("Eğitim hatası: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
