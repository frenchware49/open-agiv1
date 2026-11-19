package agent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bilgi grafığındaki bir varlık (entity). */
public final class Entity {
    private final String name;
    private String type;
    private final Map<String, String> attributes = new LinkedHashMap<String, String>();

    public Entity(String name) { this.name = name; this.type = ""; }

    public String name() { return name; }
    public String type() { return type; }
    public void setType(String t) { this.type = t == null ? "" : t; }
    public Map<String, String> attributes() { return attributes; }

    public void setAttribute(String key, String value) { attributes.put(key, value); }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", name);
        m.put("type", type);
        m.put("attributes", new LinkedHashMap<String, String>(attributes));
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Entity fromMap(Map<String, Object> m) {
        Entity e = new Entity(Json.str(m.get("name")));
        e.setType(Json.str(m.get("type")));
        Object attrs = m.get("attributes");
        if (attrs instanceof Map) {
            for (Map.Entry<String, String> entry : ((Map<String, String>) attrs).entrySet()) {
                e.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        return e;
    }
}
