package agent;

/** Ajanın kendi yeteneklerini ve sınırlarını bilmesi (iç gözlem / self-model). */
public final class SelfModel {
    public String describe(Agent a) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ÖZ-MODEL] Ben Yerel Ajan (AGI-v1 deneysel profil).\n");
        sb.append("Biliyorum ki: ").append(a.tools().size()).append(" aracım var (")
          .append(toolNames(a)).append(").\n");
        sb.append("Hafızamda ").append(a.memorySize()).append(" anı, ")
          .append(a.learnedCount()).append(" öğrenilmiş niyet örneği var.\n");
        sb.append("Bilişsel çekirdek: ").append(a.core().mode()).
          append(" | DIDL: ").append(a.brainEnabled() ? "açık" : "kapalı").
          append(" | Dünya modeli: ").append(a.graphEnabled() ? "açık" : "kapalı").append(".\n");
        sb.append("Modelleri araç olarak kullanırım: hızlı=").append(a.model())
          .append(", akıl=").append(a.reasoningModel()).append(".\n");
        sb.append("Sınırlarım: kendi temel dil modelimi sıfırdan eğitmiyorum; ")
          .append("fiziksel dünyayla doğrudan etkileşimim yok; ")
          .append("bilinci veya öznel deneyimim yok.\n");
        sb.append("Bu bir AGI değil; AGI'ye giden yolda deneysel bir yetenek profilidir.");
        return sb.toString();
    }

    private static String toolNames(Agent a) {
        StringBuilder sb = new StringBuilder();
        for (Tool t : a.tools()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.name());
        }
        return sb.toString();
    }
}
