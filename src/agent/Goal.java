package agent;

import java.util.LinkedHashMap;
import java.util.Map;

/** Özerk sistemdeki tek bir hedef. */
public final class Goal {
    public enum Status { PENDING, IN_PROGRESS, DONE, FAILED }

    private int id;
    private String description;
    private Status status;
    private int priority;
    private String result;

    public Goal(int id, String description, int priority, Status status) {
        this.id = id;
        this.description = description;
        this.priority = priority;
        this.status = status;
        this.result = "";
    }

    public int id() { return id; }
    public String description() { return description; }
    public Status status() { return status; }
    public int priority() { return priority; }
    public String result() { return result; }

    public void setStatus(Status s) { this.status = s; }
    public void setResult(String r) { this.result = r == null ? "" : r; }
    public void setPriority(int p) { this.priority = p; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("description", description);
        m.put("status", status.name());
        m.put("priority", priority);
        m.put("result", result);
        return m;
    }

    public static Goal fromMap(Map<String, Object> m) {
        int id = ((Number) m.get("id")).intValue();
        String desc = Json.str(m.get("description"));
        int pr = ((Number) m.get("priority")).intValue();
        String st = Json.str(m.get("status"));
        Status status = Status.PENDING;
        try { status = Status.valueOf(st); } catch (Exception ignored) {}
        Goal g = new Goal(id, desc, pr, status);
        g.setResult(Json.str(m.get("result")));
        return g;
    }
}
