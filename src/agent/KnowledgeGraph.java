package agent;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Ajanın dünya modeli: varlık-ilişki üçlülerinden oluşan kalıcı bilgi grafığı.
 * Metinden LLM ile olgu çıkarır, varlıklar arasında çok adımlı (multi-hop)
 * akıl yürütme yapar ve ilgili bilgileri sistem prompt'una enjekte eder.
 */
public final class KnowledgeGraph {
    private final OllamaClient client;
    private final File file = new File("knowledge-graph.json");
    private final Map<String, Entity> entities = new LinkedHashMap<String, Entity>();
    private final List<Relation> relations = new ArrayList<Relation>();

    public KnowledgeGraph(OllamaClient client) {
        this.client = client;
        load();
    }

    public int entityCount() { return entities.size(); }
    public int relationCount() { return relations.size(); }

    public void addFact(String subject, String predicate, String object) {
        if (subject == null || predicate == null || object == null) return;
        String s = subject.trim();
        String p = predicate.trim();
        String o = object.trim();
        if (s.isEmpty() || p.isEmpty() || o.isEmpty()) return;
        ensure(s);
        ensure(o);
        for (Relation r : relations) {
            if (r.subject().equalsIgnoreCase(s) && r.predicate().equalsIgnoreCase(p)
                    && r.object().equalsIgnoreCase(o)) return; // tekrar yok
        }
        relations.add(new Relation(s, p, o, 1.0));
        save();
    }

    public List<Relation> factsFor(String entity) {
        List<Relation> out = new ArrayList<Relation>();
        for (Relation r : relations) {
            if (r.subject().equalsIgnoreCase(entity) || r.object().equalsIgnoreCase(entity)) out.add(r);
        }
        return out;
    }

    public List<Relation> allFacts() { return new ArrayList<Relation>(relations); }

    public List<String> entityNames() {
        List<String> names = new ArrayList<String>();
        for (Entity e : entities.values()) names.add(e.name());
        return names;
    }

    public List<String> relationDescriptions() {
        List<String> out = new ArrayList<String>();
        for (Relation r : relations) out.add(r.describe());
        return out;
    }

    public String renderEntity(String name) {
        List<Relation> facts = factsFor(name);
        if (facts.isEmpty()) return "'" + name + "' için bilgi yok.";
        StringBuilder sb = new StringBuilder();
        sb.append("'").append(name).append("' için ilişkiler:\n");
        for (Relation r : facts) sb.append("  ").append(r.describe()).append('\n');
        return sb.toString().trim();
    }

    /** Sorguyla ilgili varlıklardan başlayıp hops adım ileri/geri yürür, bulguları döndürür. */
    public String contextFor(String query, int hops) {
        if (relations.isEmpty()) return "";
        Set<String> seeds = seedsFor(query);
        if (seeds.isEmpty()) return "";

        Set<String> visited = new HashSet<String>();
        Deque<String> frontier = new ArrayDeque<String>(seeds);
        List<String> lines = new ArrayList<String>();

        for (int hop = 0; hop < hops && !frontier.isEmpty(); hop++) {
            List<String> next = new ArrayList<String>();
            for (String current : frontier) {
                if (!visited.add(current.toLowerCase(Locale.ROOT))) continue;
                for (Relation r : relations) {
                    if (r.subject().equalsIgnoreCase(current) || r.object().equalsIgnoreCase(current)) {
                        String line = r.describe();
                        if (!lines.contains(line)) lines.add(line);
                        String other = r.subject().equalsIgnoreCase(current) ? r.object() : r.subject();
                        if (!visited.contains(other.toLowerCase(Locale.ROOT))) next.add(other);
                    }
                    if (lines.size() >= 14) break;
                }
                if (lines.size() >= 14) break;
            }
            frontier = new ArrayDeque<String>(next);
            if (lines.size() >= 14) break;
        }
        if (lines.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("[Dünya modeli - ilgili bilgiler]\n");
        for (String line : lines) sb.append("- ").append(line).append('\n');
        return sb.toString();
    }

    /** İki varlık arasındaki en kısa ilişki zincirini bulur (BFS, çok adımlı akıl yürütme). */
    public String path(String from, String to, int maxHops) {
        String a = resolve(from);
        String b = resolve(to);
        if (a == null || b == null) {
            return "Yol bulunamadı: varlıklardan biri grafikte yok. (Sorgulanan: " +
                    (a == null ? from : to) + ")";
        }
        Deque<PathState> queue = new ArrayDeque<PathState>();
        Set<String> seen = new HashSet<String>();
        List<String> startDisplay = new ArrayList<String>();
        startDisplay.add(a);
        queue.add(new PathState(a, startDisplay));
        seen.add(lower(a));

        while (!queue.isEmpty()) {
            PathState st = queue.poll();
            if (st.node.equalsIgnoreCase(b)) return String.join(" -> ", st.display);
            if (st.display.size() - 1 >= maxHops) continue;

            List<String[]> neighbors = new ArrayList<String[]>();
            for (Relation r : relations) {
                if (r.subject().equalsIgnoreCase(st.node)) neighbors.add(new String[]{r.predicate(), r.object()});
                else if (r.object().equalsIgnoreCase(st.node)) neighbors.add(new String[]{r.predicate(), r.subject()});
            }
            // Varlık adları birbirini kapsıyorsa (örn. "Python" ve "Python programlama dili")
            // bunları zayıf bir "ilgili" bağlantısı olarak kabul et: çok adımlı akıl yürütmeyi sağlar.
            for (Entity e : entities.values()) {
                String n = e.name();
                if (n.equalsIgnoreCase(st.node)) continue;
                if (lower(n).contains(lower(st.node)) || lower(st.node).contains(lower(n))) {
                    neighbors.add(new String[]{"ilgili", n});
                }
            }

            for (String[] nb : neighbors) {
                if (seen.contains(lower(nb[1]))) continue;
                seen.add(lower(nb[1]));
                List<String> display = new ArrayList<String>(st.display);
                display.add("(" + nb[0] + ") " + nb[1]);
                queue.add(new PathState(nb[1], display));
            }
        }
        return "Yol bulunamadı (" + maxHops + " adım içinde).";
    }

    private static final class PathState {
        final String node;
        final List<String> display;
        PathState(String node, List<String> display) { this.node = node; this.display = display; }
    }

    /** Metinden LLM ile özne-yüklem-nesne üçlülerini çıkarır ve grafiğe ekler. */
    public int extractFacts(String text) {
        String system =
            "Sen bir bilgi çıkarıcıyın. Metinde açıkça geçen özne-yüklem-nesne üçlülerini çıkar.\n" +
            "Tahmin etme, metinde olmayan bilgi üretme.\n" +
            "Özne ve nesne KISA varlık adları olmalı: 'bir programlama dilidir' yerine 'programlama dili' yaz.\n" +
            "'dir/dır/tir' gibi ekleri ve 'bir' sözcüğünü varlık adına katma.\n" +
            "Yüklem mümkün olduğunca tek bir fiil olmalı (seviyor, kullanır, içerir gibi).\n" +
            "Cevabını SADECE şu JSON ile ver:\n" +
            "{\"facts\":[{\"subject\":\"...\",\"predicate\":\"...\",\"object\":\"...\"}]}\n" +
            "Hiç olgu yoksa {\"facts\":[]} ver.";
        try {
            String out = client.ask(system, text, 0.0);
            int s = out.indexOf('{');
            int e = out.lastIndexOf('}');
            if (s < 0 || e <= s) return 0;
            Object parsed = Json.parse(out.substring(s, e + 1));
            Map<String, Object> obj = Json.asObject(parsed);
            Object arr = obj == null ? null : obj.get("facts");
            if (!(arr instanceof List)) return 0;
            int before = relations.size();
            for (Object o : (List<?>) arr) {
                Map<String, Object> m = Json.asObject(o);
                if (m == null) continue;
                addFact(Json.str(m.get("subject")), Json.str(m.get("predicate")), Json.str(m.get("object")));
            }
            return relations.size() - before;
        } catch (Exception ex) {
            System.out.println("  [Uyarı] Olgu çıkarılamadı: " + ex.getMessage());
            return 0;
        }
    }

    public void clear() {
        entities.clear();
        relations.clear();
        if (file.exists() && !file.delete()) file.deleteOnExit();
    }

    /** Tam ad yoksa varlık adları arasında yumuşak (içerme) eşleştirme yapar. */
    private String resolve(String name) {
        if (name == null) return null;
        if (entities.containsKey(lower(name))) return entities.get(lower(name)).name();
        for (Entity e : entities.values()) {
            String n = lower(e.name());
            if (n.contains(lower(name)) || lower(name).contains(n)) return e.name();
        }
        return null;
    }

    private void ensure(String name) {
        String key = lower(name);
        if (!entities.containsKey(key)) entities.put(key, new Entity(name));
    }

    private Set<String> seedsFor(String query) {
        Set<String> seeds = new HashSet<String>();
        String[] tokens = query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+");
        for (String token : tokens) {
            if (token.length() < 3) continue;
            for (Entity e : entities.values()) {
                if (lower(e.name()).contains(token)) seeds.add(e.name());
                if (lower(e.type()).contains(token)) seeds.add(e.name());
                for (String v : e.attributes().values()) {
                    if (lower(v).contains(token)) seeds.add(e.name());
                }
            }
        }
        return seeds;
    }

    private void save() {
        try {
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            List<Object> ents = new ArrayList<Object>();
            for (Entity e : entities.values()) ents.add(e.toMap());
            List<Object> rels = new ArrayList<Object>();
            for (Relation r : relations) rels.add(r.toMap());
            root.put("entities", ents);
            root.put("relations", rels);
            List<Object> wrapper = new ArrayList<Object>();
            wrapper.add(root);
            Files.write(file.toPath(), Json.stringify(wrapper).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.out.println("  [Uyarı] Bilgi grafığı kaydedilemedi: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!file.exists()) return;
        try {
            String s = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            Object parsed = Json.parse(s);
            if (!(parsed instanceof List)) return;
            Object first = ((List<Object>) parsed).get(0);
            Map<String, Object> root = Json.asObject(first);
            if (root == null) return;
            Object ents = root.get("entities");
            if (ents instanceof List) {
                for (Object o : (List<Object>) ents) {
                    Entity e = Entity.fromMap(Json.asObject(o));
                    if (e.name() != null) entities.put(lower(e.name()), e);
                }
            }
            Object rels = root.get("relations");
            if (rels instanceof List) {
                for (Object o : (List<Object>) rels) {
                    relations.add(Relation.fromMap(Json.asObject(o)));
                }
            }
        } catch (Exception e) {
            System.out.println("  [Uyarı] Bilgi grafığı okunamadı: " + e.getMessage());
        }
    }

    private static String lower(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
}
