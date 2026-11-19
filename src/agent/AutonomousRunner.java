package agent;

/** Özerk çalışma döngüsü: hedefleri sırayla çalıştırır, sonuçları kaydeder, ara ara yeniden planlar. */
public final class AutonomousRunner {
    private final GoalManager manager;
    private final Agent agent;

    public AutonomousRunner(GoalManager manager, Agent agent) {
        this.manager = manager;
        this.agent = agent;
    }

    /** En fazla maxSteps hedefi özerk olarak çalıştırır. */
    public String run(int maxSteps) throws Exception {
        if (!manager.hasPending()) {
            return "Çalıştırılacak bekleyen hedef yok. Önce /goal <hedef> ile bir hedef belirleyin.";
        }
        StringBuilder log = new StringBuilder();
        log.append("Özerk çalışma başladı. ").append(manager.goals().size()).append(" hedef var.\n\n");

        for (int step = 0; step < maxSteps; step++) {
            Goal g = manager.nextPending();
            if (g == null) break;

            g.setStatus(Goal.Status.IN_PROGRESS);
            manager.save();
            System.out.println("  [Özerk] Hedef " + g.id() + " çalıştırılıyor: " + g.description());

            String result;
            try {
                result = agent.executeTask(g.description());
            } catch (Exception e) {
                result = "Hata: " + e.getMessage();
            }
            g.setResult(result);
            g.setStatus(result.startsWith("Hata:") ? Goal.Status.FAILED : Goal.Status.DONE);
            manager.save();

            log.append(String.format(java.util.Locale.ROOT, "%d) %s%n   -> %s%n%n",
                    g.id(), g.description(), shorten(result, 300)));

            if (manager.hasPending() && step % 2 == 1) {
                System.out.println("  [Özerk] Hedefler sonuç ışığında yeniden değerlendiriliyor...");
                manager.replan(result);
            }
        }

        log.append("--- Hedef Durumu ---\n").append(manager.render());
        return log.toString();
    }

    /** Sadece bir sonraki hedefi çalıştırır (adım adım mod). */
    public String runNext() throws Exception {
        Goal g = manager.nextPending();
        if (g == null) return "Bekleyen hedef yok.";
        g.setStatus(Goal.Status.IN_PROGRESS);
        manager.save();
        System.out.println("  [Özerk] Hedef " + g.id() + " çalıştırılıyor: " + g.description());
        String result;
        try { result = agent.executeTask(g.description()); }
        catch (Exception e) { result = "Hata: " + e.getMessage(); }
        g.setResult(result);
        g.setStatus(result.startsWith("Hata:") ? Goal.Status.FAILED : Goal.Status.DONE);
        manager.save();
        return "Hedef " + g.id() + " tamamlandı:\n" + result + "\n\n" + manager.render();
    }

    private static String shorten(String s, int n) {
        if (s == null) return "";
        String one = s.replace('\n', ' ').trim();
        return one.length() > n ? one.substring(0, n) + "..." : one;
    }
}
