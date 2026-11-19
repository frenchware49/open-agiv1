package agent.server;

import agent.Agent;
import agent.Config;
import agent.Json;
import agent.TextProcessor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Ajanı HTTP API olarak sunar. Harici uygulamalar yerel modele erişebilir.
 * Endpoint'ler: /health, /status, /metrics, /api/chat, /api/stream, /api/learn
 */
public final class AgentServer {
    private final Agent agent;
    private final HttpServer server;
    private final int port;

    public AgentServer(Agent agent, int port) throws IOException {
        this.agent = agent;
        this.port = port;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", ex -> send(ex, 200,
                "{\"status\":\"ok\",\"model\":\"" + agent.model() + "\"}"));
        server.createContext("/status", ex -> send(ex, 200, statusJson()));
        server.createContext("/metrics", ex -> send(ex, 200, agent.metricsJson()));
        server.createContext("/api/chat", this::chat);
        server.createContext("/api/stream", this::stream);
        server.createContext("/api/learn", this::learn);
        server.createContext("/api/nlp", this::nlp);
        server.setExecutor(Executors.newFixedThreadPool(4));
    }

    public void start() {
        server.start();
        System.out.println("API sunucusu: http://localhost:" + port);
        System.out.println("  GET  /health   /status   /metrics");
        System.out.println("  POST /api/chat   /api/stream   /api/learn");
    }

    public void stop() { server.stop(0); }
    public int port() { return port; }

    private String statusJson() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("model", agent.model());
        m.put("fast_model", agent.model());
        m.put("reasoning_model", agent.reasoningModel());
        m.put("fast", agent.fastMode());
        m.put("deep", agent.deepMode());
        m.put("consensus", agent.consensusEnabled());
        m.put("autonomy", agent.autonomyEnabled());
        m.put("graph", agent.graphEnabled());
        m.put("brain", agent.brainEnabled());
        m.put("fallback", agent.fallbackChain());
        m.put("immune", agent.immuneReport());
        m.put("core", agent.coreReport());
        m.put("agi_profile", agent.agiReport().contains("AGI-v1") ? "deneysel-v1" : "gelismis-dar-ajan");
        m.put("memory", agent.memorySize());
        m.put("learned", agent.learnedCount());
        m.put("world", agent.worldStats());
        return Json.stringify(m);
    }

    private void chat(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"POST gerekli\"}"); return; }
        try {
            Map<String, Object> req = Json.asObject(Json.parse(read(ex.getRequestBody())));
            String message = req == null ? null : Json.str(req.get("message"));
            if (message == null) { send(ex, 400, "{\"error\":\"message eksik\"}"); return; }
            StringBuilder collected = new StringBuilder();
            String answer = agent.respondStream(message, collected::append); // hızlı yol
            Map<String, Object> res = new LinkedHashMap<String, Object>();
            res.put("answer", answer);
            res.put("route", agent.lastRouteInfo());
            res.put("confidence", agent.lastConfidence());
            send(ex, 200, Json.stringify(res));
        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void stream(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "error"); return; }
        try {
            Map<String, Object> req = Json.asObject(Json.parse(read(ex.getRequestBody())));
            String message = req == null ? null : Json.str(req.get("message"));
            if (message == null) { send(ex, 400, "message eksik"); return; }
            ex.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            ex.sendResponseHeaders(200, 0); // chunked streaming
            OutputStream os = ex.getResponseBody();
            agent.respondStream(message, token -> {
                try { os.write(token.getBytes(StandardCharsets.UTF_8)); os.flush(); }
                catch (IOException ignored) {}
            });
            os.close();
        } catch (Exception e) {
            send(ex, 500, "error: " + e.getMessage());
        }
    }

    private void learn(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "error"); return; }
        try {
            Map<String, Object> req = Json.asObject(Json.parse(read(ex.getRequestBody())));
            String text = req == null ? null : Json.str(req.get("text"));
            String intent = req == null ? null : Json.str(req.get("intent"));
            if (text == null || intent == null) { send(ex, 400, "{\"error\":\"text/intent eksik\"}"); return; }
            agent.teach(text, intent);
            send(ex, 200, "{\"ok\":true,\"message\":\"" + escape(agent.learnStatus()) + "\"}");
        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private void nlp(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "error"); return; }
        try {
            Map<String, Object> req = Json.asObject(Json.parse(read(ex.getRequestBody())));
            String text = req == null ? null : Json.str(req.get("text"));
            if (text == null) { send(ex, 400, "{\"error\":\"text eksik\"}"); return; }
            Map<String, Object> res = new LinkedHashMap<String, Object>();
            res.put("analysis", agent.analyzeText(text));
            res.put("keyphrases", agent.keyphrases(text, 8));
            res.put("summary", agent.summarizeText(text, 2));
            res.put("sentiment", TextProcessor.sentiment(text));
            send(ex, 200, Json.stringify(res));
        } catch (Exception e) {
            send(ex, 500, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    private static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        in.close();
        return sb.toString();
    }

    private static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    public static void main(String[] args) throws Exception {
        Config cfg = Config.load();
        Agent agent = new Agent(cfg.model);
        agent.check();
        new AgentServer(agent, cfg.serverPort).start();
    }
}
