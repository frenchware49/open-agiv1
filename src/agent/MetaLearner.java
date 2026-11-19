package agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Öğrenmeyi öğrenme (meta-learning): ağın kendi güncelleme stratejisini ayarlar.
 * Her öğrenme denemesinin kazancını izler; kazanç düşükse öğrenme oranını/epoch
 * sayısını değiştirir, unutma olursa replay ağırlığını artırır.
 */
public final class MetaLearner {
    private final List<Episode> episodes = new ArrayList<Episode>();
    private double lr = 0.002;
    private int epochs = 14;
    private double replayBoost = 1.0;

    public static final class Params {
        public final double lr;
        public final int epochs;
        public final double replayBoost;
        Params(double lr, int epochs, double replayBoost) {
            this.lr = lr; this.epochs = epochs; this.replayBoost = replayBoost;
        }
    }

    /** Bir sonraki öğrenme denemesi için hiperparametreleri önerir. */
    public Params suggest() {
        if (!episodes.isEmpty()) {
            Episode last = episodes.get(episodes.size() - 1);
            if (last.gain < -0.01) {
                lr *= 0.55; epochs += 8; replayBoost *= 1.35; // unutma → yavaşla, daha çok replay
            } else if (last.gain < 0.005) {
                lr *= 0.85; epochs += 3; replayBoost *= 1.1;  // az kazanç → biraz daha uğraş
            } else {
                lr *= 1.08; epochs = Math.max(8, epochs - 2); // iyi → hızlan
                replayBoost *= 0.95;
            }
        }
        lr = clamp(lr, 0.0003, 0.012);
        epochs = (int) clamp(epochs, 6, 45);
        replayBoost = clamp(replayBoost, 1.0, 5.0);
        return new Params(lr, epochs, replayBoost);
    }

    public void record(int examples, double accBefore, double accAfter, long timeMs, Params used) {
        episodes.add(new Episode(examples, used.lr, used.epochs, accBefore,
                accAfter - accBefore, timeMs, used.replayBoost));
        if (episodes.size() > 50) episodes.remove(0);
    }

    public String report() {
        if (episodes.isEmpty()) return "Meta-öğrenme: henüz deney yok.";
        StringBuilder sb = new StringBuilder();
        sb.append("=== Meta-Öğrenme (öğrenmeyi öğrenme) ===\n");
        sb.append("Önerilen strateji: lr=").append(String.format(Locale.ROOT, "%.5f", lr))
          .append(", epoch=").append(epochs)
          .append(", replay=").append(String.format(Locale.ROOT, "%.2f", replayBoost)).append('\n');
        sb.append("Deney sayısı: ").append(episodes.size()).append('\n');
        double totalGain = 0;
        for (Episode e : episodes) totalGain += e.gain;
        sb.append("Ortalama doğruluk kazancı: ")
          .append(String.format(Locale.ROOT, "%+.4f", totalGain / episodes.size())).append('\n');
        Episode last = episodes.get(episodes.size() - 1);
        sb.append("Son deneme: ").append(String.format(Locale.ROOT, "%+.4f", last.gain))
          .append(" kazanç, ").append(last.timeMs).append(" ms\n");
        sb.append("Strateji: ").append(last.gain < 0 ? "unutma önleme (yavaşlat, replay artır)"
                : last.gain < 0.005 ? "konsolidasyon (daha fazla epoch)"
                : "hızlandırma (lr artır, epoch azalt)");
        return sb.toString();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static final class Episode {
        final int examples;
        final double lr;
        final int epochs;
        final double accBefore;
        final double gain;
        final long timeMs;
        final double replayBoost;
        Episode(int examples, double lr, int epochs, double accBefore,
                double gain, long timeMs, double replayBoost) {
            this.examples = examples; this.lr = lr; this.epochs = epochs;
            this.accBefore = accBefore; this.gain = gain; this.timeMs = timeMs;
            this.replayBoost = replayBoost;
        }
    }
}
