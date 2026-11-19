package agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Gözlemlenebilirlik: istek, gecikme, güven ve araç kullanım istatistikleri. */
public final class Metrics {
    private long requests = 0;
    private long totalMs = 0;
    private long maxMs = 0;
    private long toolCalls = 0;
    private long errors = 0;
    private double confidenceSum = 0;
    private final List<String> recent = new ArrayList<String>();
    private long started = System.currentTimeMillis();

    public synchronized void record(long ms, String intent, double confidence, int tools, boolean ok) {
        requests++;
        totalMs += ms;
        maxMs = Math.max(maxMs, ms);
        toolCalls += tools;
        confidenceSum += confidence;
        if (!ok) errors++;
        recent.add(String.format(Locale.ROOT,
                "%d ms | %s | güven %.2f | araç %d", ms, intent, confidence, tools));
        if (recent.size() > 20) recent.remove(0);
    }

    public synchronized String toJson() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("requests", requests);
        m.put("avg_ms", requests == 0 ? 0 : Math.round((double) totalMs / requests));
        m.put("max_ms", maxMs);
        m.put("tool_calls", toolCalls);
        m.put("errors", errors);
        m.put("avg_confidence", requests == 0 ? 0 : Math.round(100.0 * confidenceSum / requests) / 100.0);
        m.put("uptime_s", (System.currentTimeMillis() - started) / 1000);
        m.put("recent", new ArrayList<Object>(recent));
        return Json.stringify(m);
    }

    public synchronized String summary() {
        return "İstek: " + requests + " | Ort: " + (requests == 0 ? 0 : totalMs / requests) +
                " ms | Maks: " + maxMs + " ms | Araç: " + toolCalls +
                " | Ort güven: " + String.format(Locale.ROOT, "%.2f",
                        requests == 0 ? 0 : confidenceSum / requests) +
                " | Hata: " + errors;
    }
}
