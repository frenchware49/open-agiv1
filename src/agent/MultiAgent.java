package agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Çok ajanlı çözüm: yerel sinir ağı her alt görevi ilgili uzmana yönlendirir,
 * yazıcı birleştirir, denetçi (self-reflection) son kontrolü yapar.
 */
public final class MultiAgent {
    private final Planner planner;
    private final Reflector reflector;
    private final IntentRouter router;
    private final Map<String, Specialist> byIntent = new LinkedHashMap<String, Specialist>();
    private final Map<String, Specialist> verifiers = new LinkedHashMap<String, Specialist>();
    private final Specialist writer;

    public MultiAgent(OllamaClient client, List<Tool> tools, IntentRouter router) {
        this.planner = new Planner(client);
        this.reflector = new Reflector(client);
        this.router = router;

        byIntent.put("matematik", new Specialist(client, tools, "Hesaplayıcı",
                "Sayısal ifadeleri hesaplar, sonucu doğrular."));
        byIntent.put("dosya", new Specialist(client, tools, "Dosya Uzmanı",
                "Dosya okuma/yazma/listeme işlemlerini yürütür."));
        byIntent.put("kod", new Specialist(client, tools, "Yazılımcı",
                "Kod yazar, hata ayıklar, algoritma önerir."));
        byIntent.put("bilgi", new Specialist(client, tools, "Araştırmacı",
                "Saat/tarih gibi bilgileri toplar, soruları yanıtlar."));
        byIntent.put("sohbet", new Specialist(client, tools, "Sohbet Uzmanı",
                "Samimi ve kısa sohbet cevapları verir."));

        verifiers.put("matematik", new Specialist(client, tools, "Sayı Denetçisi",
                "Hesaplama sonuçlarını bağımsız olarak doğrular, tutarsızlık bulursa düzeltir."));
        verifiers.put("kod", new Specialist(client, tools, "Kod Denetçisi",
                "Üretilen kodu gözden geçirir, çalıştırma sonucunu doğrular."));
        verifiers.put("bilgi", new Specialist(client, tools, "Kaynak Denetçisi",
                "Bilgi iddialarını kaynaklarla karşılaştırır, şüpheli iddiaları işaretler."));

        this.writer = new Specialist(client, tools, "Yazar",
                "Uzmanların sonuçlarını tek, tutarlı, kısa bir Türkçe cevapta birleştirir.");
    }

    public String solve(String task, IntentRouter.Result route, String context) {
        List<String> steps = planner.plan(task);
        StringBuilder results = new StringBuilder();

        for (int i = 0; i < steps.size(); i++) {
            String step = steps.get(i);
            IntentRouter.Result r = steps.size() == 1 ? route : router.route(step);
            Specialist specialist = byIntent.get(r.intent);
            if (specialist == null) specialist = byIntent.get("sohbet");

            System.out.println("  [Multi] adım " + (i + 1) + " → " + specialist.name() + ": " + step);
            String res = ImmuneSystem.GLOBAL.guard(specialist.run(step, context + "\n" + results));

            Specialist verifier = verifiers.get(r.intent);
            if (verifier != null) {
                System.out.println("  [Multi] doğrulayıcı → " + verifier.name());
                res = verifier.run("Şu sonucu doğrula ve gerekirse düzelt: " + res +
                        "\nGörev: " + step, context);
            }

            results.append("Adım ").append(i + 1).append(" [")
                   .append(specialist.name()).append("]: ").append(res).append('\n');
        }

        String draft = ImmuneSystem.GLOBAL.guard(writer.run("Kullanıcı sorusu: " + task +
                "\n\nUzman sonuçları:\n" + results.toString(), context));

        System.out.println("  [Multi] Yazar birleştirdi, denetçi kontrol ediyor...");
        Reflector.Review review = reflector.review(task, draft, results.toString());
        if (!review.ok && review.revised != null && !review.revised.trim().isEmpty()
                && !review.revised.trim().equals(draft.trim())) {
            System.out.println("  [Multi] Denetçi düzeltti.");
            return review.revised;
        }
        return draft;
    }
}
