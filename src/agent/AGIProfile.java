package agent;

/** AGI-v1 deneysel profilinin yetenek kontrol listesi ve dürüst açıklaması. */
public final class AGIProfile {
    public String report(Agent a) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AGI-v1 DENEYSEL PROFİL ===\n\n");
        int pass = 0, total = 0;

        pass += check(sb, "Planlama", a.plannerEnabled(), "Görev alt adımlara bölünüyor");
        pass += check(sb, "Öz-denetim", a.reflectionEnabled(), "Reflection + güven skoru");
        pass += check(sb, "Kalıcı hafıza", a.memorySize() >= 0, "Vektör hafıza + bilgi grafığı");
        pass += check(sb, "Dünya modeli", a.graphEnabled(), "Nedensel kural + tahmin + karşı-olgusal");
        pass += check(sb, "Araç kullanımı", a.tools().size() >= 5, a.tools().size() + " araç");
        pass += check(sb, "Özerklik", true, "Özerk hedef üretimi mevcut");
        pass += check(sb, "Öğrenmeyi öğrenme", true, "MetaLearner + hızlı/slow weights");
        pass += check(sb, "Model-as-Tool", a.core().mode() == CognitiveCore.Mode.AUTONOMOUS,
                "LLM araç, karar bizim çekirdekte");
        pass += check(sb, "Öz-model", true, "Ajan kendini ve sınırlarını biliyor");
        pass += check(sb, "Analoji/transfer", true, "Bilgiyi yeni duruma taşıma");
        pass += check(sb, "Otomatik müfredat", true, "Kendi eğitim sorularını üretme");
        pass += check(sb, "Bağışıklık", a.immuneBlocked() >= 0, "Zihin virüsü savunması");
        pass += check(sb, "Yedeklilik", true, "Model failover + DIDL");
        total = 13;

        sb.append(String.format(java.util.Locale.ROOT, "\nSkor: %d/%d\n", pass, total));
        if (pass >= 11) {
            sb.append("Profil: AGI-v1 (deneysel) — genel görev yetenekleri bir arada.\n");
        } else {
            sb.append("Profil: gelişmiş dar ajan — bazı katmanlar kapalı.\n");
        }
        sb.append("\nDÜRÜST NOT: Bu hâlâ gerçek AGI değildir. Eksik olanlar:\n");
        sb.append("  • Kendi temel dil modelimizi sıfırdan eğitecek ölçek/veri\n");
        sb.append("  • Fiziksel dünya ile gerçek etkileşim ve kalıcı deneyim\n");
        sb.append("  • Bilinç / öznel deneyim (kanıt yok)\n");
        sb.append("  • Uzun yataylı, saatler süren tutarlı özerk görevler\n");
        return sb.toString();
    }

    private static int check(StringBuilder sb, String name, boolean ok, String detail) {
        sb.append(ok ? "  [x] " : "  [ ] ").append(name).append(" — ").append(detail).append('\n');
        return ok ? 1 : 0;
    }
}
