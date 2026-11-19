package agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Model-as-Tool mimarisinin çekirdeği.
 * Karar merkezi KENDİ modellerimizdir (DIDL); LLM'ler yalnizca gerektiginde
 * cagrilan araclardir (akici uretim ve ogrenme kaynagi olarak).
 */
public final class CognitiveCore {
    public enum Mode { AUTONOMOUS, ASSISTED, MODEL }

    private Mode mode = Mode.AUTONOMOUS;
    private double threshold = 0.55;
    private int modelCallsAvoided = 0;
    private int factsLearned = 0;

    public Mode mode() { return mode; }
    public void setMode(Mode m) { this.mode = m; }
    public void setThreshold(double t) { this.threshold = Math.max(0, Math.min(1, t)); }
    public double threshold() { return threshold; }
    public int modelCallsAvoided() { return modelCallsAvoided; }
    public int factsLearned() { return factsLearned; }

    public static final class Reasoning {
        public String intent = "";
        public final List<String> facts = new ArrayList<String>();
        public final List<String> memories = new ArrayList<String>();
        public final List<String> predictions = new ArrayList<String>();
        public double confidence = 0;
        public String ownAnswer = "";
    }

    /** Kendi modellerimizle akil yurut - hic LLM cagrisi yapmaz. */
    public Reasoning reason(String text, Agent agent) {
        Reasoning r = new Reasoning();
        r.intent = agent.lastRouteInfo();

        for (String e : agent.graphEntities()) {
            if (text.toLowerCase(Locale.ROOT).contains(e.toLowerCase(Locale.ROOT))) {
                String block = agent.graphFacts(e);
                for (String line : block.split("\n")) {
                    String t = line.trim();
                    if (!t.isEmpty() && !t.startsWith("'")) r.facts.add(t);
                }
                r.predictions.add(agent.worldPredict(e, "ilgili"));
            }
        }
        r.memories.addAll(agent.recall(text));

        int known = 0;
        for (String p : r.predictions) {
            if (!p.contains("bilinmiyor") && !p.contains("gözlem yok")) known++;
        }
        r.confidence = Math.min(1.0,
                0.15 + 0.25 * r.facts.size() + 0.15 * r.memories.size() + 0.10 * known);
        r.ownAnswer = compose(r);
        return r;
    }

    /** LLM gerekli mi? Guven dusukse veya kod/uzun soruysa arac cagrilir. */
    public boolean needsModel(Reasoning r, String text) {
        if (mode != Mode.AUTONOMOUS) return true;
        if (r.ownAnswer.isEmpty() || r.ownAnswer.startsWith("(henüz")) return true;
        if (r.confidence < threshold) return true;
        if (r.intent.contains("kod")) return true;
        if (text.length() > 140) return true;
        return false;
    }

    private String compose(Reasoning r) {
        if (r.facts.isEmpty() && r.memories.isEmpty() && r.predictions.isEmpty()) {
            return "(henüz bu konuda yerel bilgim yok)";
        }
        StringBuilder sb = new StringBuilder("Bildigim kadariyla: ");
        if (!r.memories.isEmpty()) sb.append(r.memories.get(0)).append('.');
        if (!r.facts.isEmpty()) {
            if (sb.length() > 20) sb.append(' ');
            sb.append(String.join("; ", r.facts)).append('.');
        }
        for (String p : r.predictions) {
            if (!p.contains("bilinmiyor") && !p.contains("gözlem yok")) {
                sb.append(' ').append(p).append('.');
            }
        }
        return sb.toString().replace("..", ".").trim();
    }

    /** LLM cevabindan olgu cikarir — model yalnizca ogrenim kaynagidir. */
    public int learnFrom(String question, String modelAnswer, Agent agent) {
        int n = agent.extractFacts(question + "\n" + modelAnswer);
        factsLearned += n;
        return n;
    }

    public void noteModelAvoided() { modelCallsAvoided++; }

    public String report() {
        return "Bilisel Cekirdek: " + mode + " | Esik: " + threshold +
               " | LLM cagrisindan kacilan: " + modelCallsAvoided +
               " | Modelden ogrenilen olgu: " + factsLearned;
    }
}
