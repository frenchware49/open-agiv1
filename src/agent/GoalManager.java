package agent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Özerk hedef yönetimi: üst hedefi alt hedeflere böler, önceliklendirir,
 * ilerlemeyi izler ve her adımdan sonra hedefleri yeniden değerlendirir (replan).
 */
public final class GoalManager {
    private final OllamaClient client;
    private final File file = new File("goals.json");
    private final List<Goal> goals = new ArrayList<Goal>();
    private String objective = "";
    private int nextId = 1;

    public GoalManager(OllamaClient client) {
        this.client = client;
        load();
    }

    public String objective() { return objective; }
    public List<Goal> goals() { return goals; }
    public boolean hasPending() {
        for (Goal g : goals) if (g.status() == Goal.Status.PENDING) return true;
        return false;
    }

    /** Üst hedefi LLM ile somut alt hedeflere böler. */
    public void setObjective(String objective) {
        this.objective = objective.trim();
        String system =
            "Sen bir hedef planlayıcısın. Verilen üst hedefi 2-6 somut, tek başına yapılabilir alt hedefe böl.\n" +
            "Her alt hedef fiil ile başlayan kısa bir Türkçe cümle olsun.\n" +
            "Cevabını SADECE şu JSON ile ver:\n" +
            "{\"goals\":[{\"description\":\"...\",\"priority\":1}]}\n" +
            "priority: 1 en yüksek, 5 en düşük öncelik.";
        try {
            String out = client.ask(system, "Üst hedef: " + objective, 0.2);
            List<Spec> specs = parse(out);
            if (specs.isEmpty()) throw new IllegalStateException("Hedef listesi boş döndü.");
            goals.clear();
            for (Spec s : specs) {
                goals.add(new Goal(nextId++, s.description, s.priority, Goal.Status.PENDING));
            }
            save();
        } catch (Exception e) {
            goals.clear();
            goals.add(new Goal(nextId++, objective, 1, Goal.Status.PENDING));
            save();
            System.out.println("  [Uyarı] Otomatik bölme başarısız, tek hedef kullanıldı: " + e.getMessage());
        }
    }

    /**
     * Özerk hedef üretimi: sistemin kendi durumundaki eksiklikleri tespit edip
     * kendiliğinden bir hedef oluşturur (düşük güven, zayıf dünya modeli, boş hafıza).
     */
    public String suggestFromState(String state) {
        String hint = heuristicHint(state);
        if (hint == null) return null;

        String system =
            "Sen özerk bir ajanın hedef üreticisisin. Sistem durumundaki eksikliği giderecek " +
            "TEK somut bir hedef öner. Türkçe, fiille başlasın.\n" +
            "SADECE JSON ver: {\"description\":\"...\",\"priority\":1}";
        String desc = null;
        int priority = 2;
        try {
            String out = client.ask(system, "Sistem durumu:\n" + state, 0.2);
            int s = out.indexOf('{'), e = out.lastIndexOf('}');
            if (s >= 0 && e > s) {
                Map<String, Object> obj = Json.asObject(Json.parse(out.substring(s, e + 1)));
                desc = obj == null ? null : Json.str(obj.get("description"));
                Object p = obj == null ? null : obj.get("priority");
                if (p instanceof Number) priority = ((Number) p).intValue();
            }
        } catch (Exception ignored) {}
        if (desc == null || desc.trim().isEmpty()) desc = hint;
        desc = ImmuneSystem.GLOBAL.guard(desc.trim());

        for (Goal g : goals) {
            if (g.status() != Goal.Status.DONE && g.description().equalsIgnoreCase(desc.trim())) return null;
        }
        goals.add(new Goal(nextId++, desc.trim(), Math.max(1, Math.min(5, priority)), Goal.Status.PENDING));
        save();
        return desc.trim();
    }

    private static String heuristicHint(String state) {
        if (state == null) return null;
        String s = state.toLowerCase(Locale.ROOT);
        try {
            java.util.regex.Matcher conf = java.util.regex.Pattern
                    .compile("güven\\s+([0-9.]+)").matcher(s);
            if (conf.find() && Double.parseDouble(conf.group(1)) < 0.7) {
                return "son cevaptaki düşük güvenin nedenini araştır";
            }
            java.util.regex.Matcher rel = java.util.regex.Pattern
                    .compile("ilişki:\\s*(\\d+)").matcher(s);
            if (rel.find() && Integer.parseInt(rel.group(1)) < 5) {
                return "dünya modelini yeni olgularla zenginleştir";
            }
            if (s.contains("hafıza: 0")) {
                return "kullanıcı hakkında kalıcı hafıza oluştur";
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Bir sonraki bekleyen hedefi (en yüksek öncelikli) döndürür. */
    public Goal nextPending() {
        Goal best = null;
        for (Goal g : goals) {
            if (g.status() != Goal.Status.PENDING) continue;
            if (best == null || g.priority() < best.priority()) best = g;
        }
        return best;
    }

    public void save() {
        try {
            List<Object> list = new ArrayList<Object>();
            Map<String, Object> wrapper = new java.util.LinkedHashMap<String, Object>();
            wrapper.put("objective", objective);
            List<Object> items = new ArrayList<Object>();
            for (Goal g : goals) items.add(g.toMap());
            wrapper.put("goals", items);
            list.add(wrapper);
            Files.write(file.toPath(), Json.stringify(list).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("  [Uyarı] Hedefler kaydedilemedi: " + e.getMessage());
        }
    }

    /** Son sonuç ışığında hedefleri günceller: ekleme, çıkarma, öncelik değiştirme. */
    public void replan(String lastResult) {
        String system =
            "Sen bir özerk ajanın hedef denetçisisin. Mevcut hedefleri ve son çalıştırılan hedefin sonucunu gözden geçir.\n" +
            "Gerekirse: yeni hedef ekle, gereksiz hedefi çıkar, önceliği değiştir.\n" +
            "Tamamlanmış (done) hedefleri MUTLAKA koru ve tekrar çalıştırma.\n" +
            "Cevabını SADECE şu JSON ile ver:\n" +
            "{\"goals\":[{\"description\":\"...\",\"priority\":1,\"status\":\"pending|done\"}]}";
        String user = "Üst hedef: " + objective + "\n\nMevcut hedefler:\n" + render() +
                "\n\nSon hedefin sonucu:\n" + truncate(lastResult, 1200);
        try {
            String out = client.ask(system, user, 0.1);
            List<Spec> specs = parse(out);
            if (specs.isEmpty()) return;
            List<Goal> updated = new ArrayList<Goal>();
            for (Spec s : specs) {
                Goal existing = findByDescription(s.description);
                if (s.status == Goal.Status.DONE && existing != null) {
                    existing.setPriority(s.priority);
                    updated.add(existing);
                } else if (s.status == Goal.Status.DONE) {
                    Goal done = new Goal(nextId++, s.description, s.priority, Goal.Status.DONE);
                    updated.add(done);
                } else {
                    updated.add(new Goal(nextId++, s.description, s.priority, Goal.Status.PENDING));
                }
            }
            goals.clear();
            goals.addAll(updated);
            save();
        } catch (Exception e) {
            System.out.println("  [Uyarı] Yeniden planlama başarısız: " + e.getMessage());
        }
    }

    public void clear() {
        goals.clear();
        objective = "";
        if (file.exists() && !file.delete()) file.deleteOnExit();
    }

    public String render() {
        if (goals.isEmpty()) return "(henüz hedef belirlenmedi — /goal <hedef> kullanın)";
        StringBuilder sb = new StringBuilder();
        sb.append("Üst hedef: ").append(objective).append('\n');
        for (Goal g : goals) {
            String icon = " ";
            if (g.status() == Goal.Status.DONE) icon = "x";
            else if (g.status() == Goal.Status.IN_PROGRESS) icon = ">";
            else if (g.status() == Goal.Status.FAILED) icon = "!";
            sb.append(String.format(Locale.ROOT, "  [%s] %d. P%d  %s%n",
                    icon, g.id(), g.priority(), g.description()));
        }
        return sb.toString().trim();
    }

    private Goal findByDescription(String desc) {
        for (Goal g : goals) if (g.description().equalsIgnoreCase(desc)) return g;
        return null;
    }

    private static List<Spec> parse(String out) {
        List<Spec> result = new ArrayList<Spec>();
        int s = out.indexOf('{');
        int e = out.lastIndexOf('}');
        if (s < 0 || e <= s) return result;
        try {
            Object parsed = Json.parse(out.substring(s, e + 1));
            Map<String, Object> obj = Json.asObject(parsed);
            Object arr = obj == null ? null : obj.get("goals");
            if (!(arr instanceof List)) return result;
            for (Object o : (List<?>) arr) {
                Map<String, Object> m = Json.asObject(o);
                if (m == null) continue;
                String desc = Json.str(m.get("description"));
                if (desc == null || desc.trim().isEmpty()) continue;
                int pr = 3;
                Object p = m.get("priority");
                if (p instanceof Number) pr = ((Number) p).intValue();
                pr = Math.max(1, Math.min(5, pr));
                String st = Json.str(m.get("status"));
                Goal.Status status = Goal.Status.PENDING;
                try { status = Goal.Status.valueOf(st.toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}
                result.add(new Spec(desc.trim(), pr, status));
            }
        } catch (Exception ignored) {}
        return result;
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!file.exists()) return;
        try {
            String s = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Object parsed = Json.parse(s);
            if (!(parsed instanceof List)) return;
            Object first = ((List<Object>) parsed).get(0);
            Map<String, Object> wrapper = Json.asObject(first);
            if (wrapper == null) return;
            objective = Json.str(wrapper.get("objective")) == null ? "" : Json.str(wrapper.get("objective"));
            Object arr = wrapper.get("goals");
            if (!(arr instanceof List)) return;
            for (Object o : (List<Object>) arr) {
                Goal g = Goal.fromMap(Json.asObject(o));
                goals.add(g);
                if (g.id() >= nextId) nextId = g.id() + 1;
            }
        } catch (Exception ignored) {}
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    private static final class Spec {
        final String description;
        final int priority;
        final Goal.Status status;
        Spec(String description, int priority, Goal.Status status) {
            this.description = description;
            this.priority = priority;
            this.status = status;
        }
    }
}
