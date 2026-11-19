package agent;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/** config.properties dosyasından ayarlar (yoksa güvenli varsayılanlar). */
public final class Config {
    public final String model;
    public final int serverPort;
    public final boolean fastMode;
    public final boolean deepMode;
    public final boolean consensus;
    public final boolean autonomy;
    public final boolean graph;
    public final boolean brain;
    public final java.util.List<String> fallbackModels;
    public final String coreMode;
    public final double coreThreshold;
    public final String reasoningModel;
    public final int complexityChars;

    private Config(Properties p) {
        this.model = p.getProperty("model", "gemma4:e4b");
        this.serverPort = Integer.parseInt(p.getProperty("server.port", "8080"));
        this.fastMode = Boolean.parseBoolean(p.getProperty("fast", "true"));
        this.deepMode = Boolean.parseBoolean(p.getProperty("deep", "false"));
        this.consensus = Boolean.parseBoolean(p.getProperty("consensus", "false"));
        this.autonomy = Boolean.parseBoolean(p.getProperty("autonomy", "false"));
        this.graph = Boolean.parseBoolean(p.getProperty("graph", "true"));
        this.brain = Boolean.parseBoolean(p.getProperty("brain", "true"));
        this.fallbackModels = new java.util.ArrayList<String>();
        for (String s : p.getProperty("fallback.models", "gemma4:e4b,qwen2.5:7b-instruct").split(",")) {
            if (!s.trim().isEmpty()) this.fallbackModels.add(s.trim());
        }
        this.coreMode = p.getProperty("core.mode", "autonomous");
        this.coreThreshold = Double.parseDouble(p.getProperty("core.threshold", "0.55"));
        this.reasoningModel = p.getProperty("model.reasoning", "gemma4:12b");
        this.complexityChars = Integer.parseInt(p.getProperty("route.complexity_chars", "90"));
    }

    public static Config load() {
        Properties p = new Properties();
        File f = new File("config.properties");
        if (f.exists()) {
            try {
                FileInputStream in = new FileInputStream(f);
                p.load(in);
                in.close();
            } catch (Exception ignored) {}
        }
        return new Config(p);
    }
}
