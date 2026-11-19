package agent;

import agent.tools.CalculatorTool;
import agent.tools.ClockTool;
import agent.tools.CodeRunnerTool;
import agent.tools.FileTool;
import agent.tools.WebSearchTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hibrit ajan — dört gelişmiş katmanla birlikte:
 *   1. Planner        : görevi alt adımlara böler
 *   2. Self-reflection: cevabı denetler ve düzeltir
 *   3. Vektör hafıza  : anlamsal aramayla ilgili anıları hatırlatır
 *   4. Multi-agent    : yerel sinir ağı uzman ajanlara görev dağıtır
 *
 * Varsayılan: planner + reflection + vektör hafıza AÇIK, multi-agent KAPALI
 * (çok ajanlı mod daha fazla LLM çağrısı yaptığı için yavaştır).
 */
public final class Agent {
    private final OllamaClient client;
    private final IntentRouter router;
    private final Planner planner;
    private final Reflector reflector;
    private final VectorMemory memory;
    private final MultiAgent multiAgent;
    private final KnowledgeGraph graph;
    private final WebResearch research;
    private final WorldModel world;
    private final DIDLBrain brain;
    private final CognitiveCore core = new CognitiveCore();
    private final SelfModel selfModel = new SelfModel();
    private final AnalogyEngine analogy = new AnalogyEngine();
    private final CurriculumLearner curriculum;
    private final AGIProfile agiProfile = new AGIProfile();
    private final ImmuneSystem immune = ImmuneSystem.GLOBAL;
    private final SelfHealing healing;
    private final GoalManager goalManager;
    private final AutonomousRunner autonomous;
    private final List<Tool> tools;
    private final List<Map<String, Object>> history = new ArrayList<Map<String, Object>>();

    private boolean plannerEnabled = true;
    private boolean reflectionEnabled = true;
    private boolean multiAgentEnabled = false;
    private boolean fastMode = true;
    private boolean deepMode = false;
    private boolean autonomyEnabled = false;
    private boolean consensusEnabled = false;
    private int toolCallsThisTurn = 0;
    private final Metrics metrics = new Metrics();
    private String primaryModel;
    private String reasoningModel;
    private int complexityChars = 90;
    private java.util.List<String> fallbackModels = new java.util.ArrayList<String>();
    private boolean graphEnabled = true;
    private String lastUserText = "";
    private String lastAnswer = "";
    private String lastToolContext = "";
    private String lastRouteInfo = "";
    private double lastConfidence = 0.5;

    private static final int MAX_HISTORY = 20;
    private static final int MAX_TOOL_ROUNDS = 4;

    public Agent(String model) {
        this.client = new OllamaClient(model);
        this.router = new IntentRouter(7L);
        this.planner = new Planner(client);
        this.reflector = new Reflector(client);
        this.memory = new VectorMemory("vector-memory.json");
        this.tools = new java.util.ArrayList<Tool>(Arrays.asList(
                new CalculatorTool(), new FileTool(), new ClockTool(),
                new WebSearchTool(), new CodeRunnerTool()));
        Config cfg = Config.load();
        this.primaryModel = model;
        this.reasoningModel = cfg.reasoningModel;
        this.complexityChars = cfg.complexityChars;
        this.fallbackModels = cfg.fallbackModels;
        if (!fallbackModels.contains(model)) fallbackModels.add(0, model);
        this.fastMode = cfg.fastMode;
        this.deepMode = cfg.deepMode;
        this.consensusEnabled = cfg.consensus;
        this.autonomyEnabled = cfg.autonomy;
        this.graphEnabled = cfg.graph;

        this.multiAgent = new MultiAgent(client, tools, router);
        this.graph = new KnowledgeGraph(client);
        this.research = new WebResearch(client, graph, memory);
        this.world = new WorldModel(graph);
        this.curriculum = new CurriculumLearner(client);
        this.healing = new SelfHealing(client);
        this.brain = new DIDLBrain();
        brain.setEnabled(cfg.brain);
        try { core.setMode(CognitiveCore.Mode.valueOf(cfg.coreMode.toUpperCase(java.util.Locale.ROOT))); }
        catch (Exception ignored) {}
        core.setThreshold(cfg.coreThreshold);
        this.goalManager = new GoalManager(client);
        this.autonomous = new AutonomousRunner(goalManager, this);
        PluginLoader.load(this);
    }

    public void check() throws Exception { client.check(); }

    public String model() { return client.model(); }
    public void setModel(String model) {
        client.setModel(model);
        primaryModel = model;
        if (!fallbackModels.contains(model)) fallbackModels.add(0, model);
    }
    public String fallbackChain() {
        return String.join(" → ", fallbackModels) + " → DIDL (yerel)";
    }

    /** Uyarlanabilir yönlendirme: basit soru → hızlı model, zor soru → akıl modeli. */
    public String chooseModel(String text) {
        String s = text.toLowerCase(java.util.Locale.ROOT);
        boolean hard = text.length() > complexityChars
                || s.contains("neden") || s.contains("çünkü") || s.contains("kanıtla")
                || s.contains("karşılaştır") || s.contains("analiz") || s.contains("ispat")
                || s.contains("reason") || s.matches(".*(\\d+\\s*[-+*/^]\\s*\\d+).*");
        return hard ? reasoningModel : primaryModel;
    }

    public String reasoningModel() { return reasoningModel; }
    public String activeModel() { return client.model(); }
    public java.util.List<String> availableModels() throws Exception { return client.availableModels(); }

    public double routerAccuracy() { return router.trainingAccuracy(); }

    public boolean plannerEnabled() { return plannerEnabled; }
    public boolean reflectionEnabled() { return reflectionEnabled; }
    public boolean multiAgentEnabled() { return multiAgentEnabled; }
    public void setPlanner(boolean v) { plannerEnabled = v; }
    public void setReflection(boolean v) { reflectionEnabled = v; }
    public void setMultiAgent(boolean v) { multiAgentEnabled = v; }
    public boolean graphEnabled() { return graphEnabled; }
    public void setGraphEnabled(boolean v) { graphEnabled = v; }
    public boolean fastMode() { return fastMode; }
    public void setFastMode(boolean v) { fastMode = v; }
    public boolean deepMode() { return deepMode; }
    public void setDeepMode(boolean v) { deepMode = v; }
    public boolean autonomyEnabled() { return autonomyEnabled; }
    public void setAutonomy(boolean v) { autonomyEnabled = v; }
    public boolean consensusEnabled() { return consensusEnabled; }
    public void setConsensus(boolean v) { consensusEnabled = v; }
    public Metrics metrics() { return metrics; }
    public String metricsJson() { return metrics.toJson(); }
    public String brainStats() { return brain.stats(); }
    public boolean brainEnabled() { return brain.enabled(); }
    public void setBrain(boolean v) { brain.setEnabled(v); }
    public String brainThink(String text) { return brain.think(text, this); }
    public String selfModelText() { return selfModel.describe(this); }
    public String analogyFor(String text) { return analogy.infer(text, this); }
    public String curriculumStep() { return curriculum.runOnce(this); }
    public String curriculumReport() { return curriculum.report(); }
    public String counterfactual(String entity, String action) { return world.counterfactual(entity, action); }
    public String agiReport() { return agiProfile.report(this); }

    public String analyzeText(String text) {
        return TextProcessor.analyze(text, graphEntities()).toString();
    }
    public String summarizeText(String text, int n) { return TextProcessor.summarize(text, n); }
    public String keyphrases(String text, int k) { return String.join(", ", TextProcessor.keyphrases(text, k)); }
    public String textAnswer(String question, String context) { return TextProcessor.answer(question, context); }
    public String textSearch(String query, int k) {
        List<String> docs = new ArrayList<String>();
        for (String line : memoryList().split("\n")) if (!line.trim().isEmpty()) docs.add(line.trim());
        return String.join("\n", TextProcessor.search(query, docs, k));
    }
    public CognitiveCore core() { return core; }
    public String coreReport() { return core.report(); }
    public void setCoreMode(String m) {
        try { core.setMode(CognitiveCore.Mode.valueOf(m.toUpperCase(java.util.Locale.ROOT))); }
        catch (Exception ignored) {}
    }
    public String immuneReport() { return immune.report(); }
    public String healingReport() { return healing.report(); }
    public long immuneBlocked() { return immune.blocked(); }
    public long immuneScanned() { return immune.scanned(); }

    public String metaReport() { return router.metaReport(); }

    public String worldPredict(String entity, String action) {
        return world.predict(entity, action).toString();
    }
    public String worldSimulate(String entity, int steps) { return world.simulate(entity, steps); }
    public String worldRules() { return world.rules(); }
    public String worldStats() { return world.stats(); }
    public void observe(String subject, String action, String object) {
        world.observe(subject, action, object);
    }

    public String suggestAutonomousGoal() {
        String state = lastRouteInfo + " | " + graphStats() + " | Hafıza: " + memorySize();
        String goal = goalManager.suggestFromState(state);
        return goal == null ? "Yeni hedef gerekmiyor (sistem durumu normal)." : "Yeni özerk hedef: " + goal;
    }
    public String runAutonomousGoals(int n) throws Exception { return runAutonomous(n); }

    public void clearHistory() { history.clear(); }

    public void remember(String text) {
        memory.add(text);
        int n = graph.extractFacts(text);
        System.out.println("  [Dünya modeli] " + n + " olgu çıkarıldı ve grafiğe eklendi.");
    }
    public void forget() { memory.clear(); }
    public String memoryList() { return memory.listAll(); }
    public int memorySize() { return memory.size(); }

    public List<String> recall(String query) { return memory.search(query, 3); }

    public String lastUserText() { return lastUserText; }
    public String lastAnswer() { return lastAnswer; }
    public String lastRouteInfo() { return lastRouteInfo; }
    public double lastConfidence() { return lastConfidence; }

    /** Araştırma: URL'den olgu çıkarıp hafızaya/grafiğe yazar. */
    public String researchUrl(String url) { return research.research(url); }
    public String researchSources(int maxPages) { return research.researchSeeds(maxPages); }
    public String researchSeeds() { return String.join("\n", research.seeds()); }
    public void addSource(String url) { research.addSeed(url); }

    /** Görüntülü soru: Gemma 4 gibi çok modlu modelle resmi yorumlar. */
    public String respondWithImage(String path, String question) throws Exception {
        question = immune.guard(question);
        lastUserText = question;
        IntentRouter.Result r = router.route(question);
        lastRouteInfo = "niyet=" + r.intent + " güven=" +
                String.format(java.util.Locale.ROOT, "%.2f", r.confidence);
        String mem = memory.contextFor(question, 3) + "\n" + graph.contextFor(question, 2);
        String system = "Sen 'Yerel Ajan'sın. Kullanıcı bir görüntü ve soru gönderdi. " +
                "Görüntüyü dikkatle incele, Türkçe ve kısa cevap ver.\n" +
                (mem.trim().isEmpty() ? "" : "[Bağlam]\n" + mem + "\n");
        String answer = client.askWithImage(system, question, path, 0.2);
        lastAnswer = answer;
        int n = graph.extractFacts(question + "\n" + answer);
        if (n > 0) System.out.println("  [Dünya modeli] " + n + " olgu çıkarıldı.");
        history.add(Json.msg("user", question));
        history.add(Json.msg("assistant", answer));
        trimHistory();
        return answer;
    }

    public String graphFacts(String entity) { return graph.renderEntity(entity); }
    public String graphPath(String from, String to) { return graph.path(from, to, 4); }
    public String graphStats() {
        return "Varlık: " + graph.entityCount() + " | İlişki: " + graph.relationCount();
    }
    public java.util.List<String> graphEntities() { return graph.entityNames(); }
    public java.util.List<String> graphRelations() { return graph.relationDescriptions(); }
    public java.util.List<Tool> tools() { return tools; }
    public void addTool(Tool t) {
        for (Tool x : tools) if (x.name().equalsIgnoreCase(t.name())) return;
        tools.add(t);
    }
    public java.util.List<String> learnedExamples() { return router.learnedExamples(); }
    public int extractFacts(String text) { return graph.extractFacts(text); }
    public int extractFactsFromLast() {
        return graph.extractFacts(lastUserText + "\n" + lastAnswer);
    }
    public void clearGraph() { graph.clear(); }

    /** Yeni etiketli bir örnekle yerel sinir ağını canlı günceller. */
    public IntentRouter.LearnResult teach(String text, String intent) {
        return router.learn(text, intent);
    }

    /** Son mesajın niyetini düzeltir (yanlış yönlendirme geri bildirimi). */
    public IntentRouter.LearnResult correctLast(String intent) {
        if (lastUserText.isEmpty()) {
            return new IntentRouter.LearnResult(false, "Düzeltilecek bir mesaj yok.");
        }
        return router.learn(lastUserText, intent);
    }

    /** Son mesajı mevcut sınıflandırmasıyla pekiştirir (olumlu geri bildirim). */
    public IntentRouter.LearnResult reinforceLast() {
        if (lastUserText.isEmpty()) {
            return new IntentRouter.LearnResult(false, "Pekiştirilecek bir mesaj yok.");
        }
        return router.learn(lastUserText, router.route(lastUserText).intent);
    }

    /** Hedef döngüsüne girmeden tek bir görevi çalıştırır (özerk runner tarafından kullanılır). */
    public String executeTask(String task) throws Exception {
        IntentRouter.Result r = router.route(task);
        String memContext = memory.contextFor(task, 3);
        return respondSingle(task, r, memContext);
    }

    public void setObjective(String objective) { goalManager.setObjective(objective); }
    public String renderGoals() { return goalManager.render(); }
    public int pendingGoalCount() {
        int n = 0;
        for (Goal g : goalManager.goals()) if (g.status() == Goal.Status.PENDING) n++;
        return n;
    }
    public String runAutonomous(int steps) throws Exception { return autonomous.run(steps); }
    public String runNextGoal() throws Exception { return autonomous.runNext(); }
    public void clearGoals() { goalManager.clear(); }

    public int learnedCount() { return router.learnedCount(); }

    public String runEvaluation() { return new EvalHarness().run(router); }
    public String runBenchmark() { return new BenchmarkHarness(client).run(); }

    public String learnStatus() {
        return "Öğrenilen örnek: " + router.learnedCount() +
                " | Öğrenilenlerde doğruluk: %" + String.format(java.util.Locale.ROOT, "%.1f", router.learnedAccuracy() * 100) +
                " | Genel doğruluk: %" + String.format(java.util.Locale.ROOT, "%.1f", router.trainingAccuracy() * 100);
    }

    /** Ana giriş noktası: yönlendirme -> (planner | multi-agent) -> reflection. */
    public String respond(String userText) throws Exception {
        userText = immune.guard(userText);
        long t0 = System.currentTimeMillis();
        toolCallsThisTurn = 0;
        lastUserText = userText;
        IntentRouter.Result r = router.route(userText);
        lastRouteInfo = "niyet=" + r.intent + " güven=" +
                String.format(java.util.Locale.ROOT, "%.2f", r.confidence);
        lastConfidence = r.confidence;
        System.out.println("  [Yerel Ağ] " + lastRouteInfo);

        String memContext = memory.contextFor(userText, 3);
        if (graphEnabled) {
            String facts = graph.contextFor(userText, 2);
            if (!facts.isEmpty()) memContext += "\n" + facts;
        }
        String answer;
        if (multiAgentEnabled) {
            System.out.println("  [Mod] Multi-agent (uzmanlar devrede)");
            answer = multiAgent.solve(userText, r, memContext);
        } else {
            answer = respondSingle(userText, r, memContext);
        }

        lastAnswer = answer;
        history.add(Json.msg("user", userText));
        history.add(Json.msg("assistant", answer));
        trimHistory();
        metrics.record(System.currentTimeMillis() - t0, r.intent, r.confidence, toolCallsThisTurn, true);
        if (core.mode() == CognitiveCore.Mode.AUTONOMOUS) core.learnFrom(userText, answer, this);
        if (autonomyEnabled) maybeAutonomousGoal();
        return answer;
    }

    private void maybeAutonomousGoal() {
        String state = lastRouteInfo + " | " + graphStats() + " | Hafıza: " + memorySize();
        String goal = goalManager.suggestFromState(state);
        if (goal != null) System.out.println("  [Özerk] kendi hedefini oluşturdu: " + goal);
    }

    /**
     * Hızlı cevap: tek streaming çağrı + yerel araç ön işleme.
     * İlk token saniyeler içinde görünür; planner/reflection gecikmesi yoktur.
     * Derin mod açıksa tam ajan hattını kullanır ve sonucu parça parça yayınlar.
     */
    public String respondStream(String userText, java.util.function.Consumer<String> onToken) throws Exception {
        userText = immune.guard(userText);
        long t0 = System.currentTimeMillis();
        toolCallsThisTurn = 0;
        lastUserText = userText;
        IntentRouter.Result r = router.route(userText);
        lastRouteInfo = "niyet=" + r.intent + " güven=" +
                String.format(java.util.Locale.ROOT, "%.2f", r.confidence);
        System.out.println("  [Yerel Ağ · hızlı] " + lastRouteInfo);

        String pre = precomputeTools(userText);
        // Dosya/klasör sonucu kesin bilgidir — LLM bunu yok sayabiliyor, doğrudan veriyoruz.
        if (pre.contains("[Dosya:") || pre.contains("[Klasör]")) {
            String local = "İstediğin işlemi aracımla yaptım:\n\n" + pre.trim();
            System.out.println("  [Otomatik Araç] dosya işlemi tamamlandı (LLM çağrılmadı)");
            for (String chunk : chunks(local, 16)) onToken.accept(chunk);
            lastAnswer = local;
            history.add(Json.msg("user", userText));
            history.add(Json.msg("assistant", local));
            trimHistory();
            metrics.record(System.currentTimeMillis() - t0, r.intent, r.confidence, toolCallsThisTurn, true);
            return local;
        }

        StringBuilder context = new StringBuilder();
        context.append(memory.contextFor(userText, 3));
        if (graphEnabled) context.append('\n').append(graph.contextFor(userText, 2));
        context.append(pre);

        String answer;
        if (core.mode() == CognitiveCore.Mode.AUTONOMOUS && fastMode && !deepMode && !multiAgentEnabled) {
            // MODEL-AS-TOOL: önce kendi beynimizle akıl yürüt.
            CognitiveCore.Reasoning reasoning = core.reason(userText, this);
            if (!core.needsModel(reasoning, userText)) {
                answer = reasoning.ownAnswer;
                System.out.println("  [Bilişsel Çekirdek] kendi cevabı (güven " +
                        String.format(java.util.Locale.ROOT, "%.2f", reasoning.confidence) +
                        ") — LLM aracı çağrılmadı");
                core.noteModelAvoided();
                for (String chunk : chunks(answer, 14)) onToken.accept(chunk);
            } else {
                answer = streamOnce(userText, context.toString(), r, onToken);
                int learned = core.learnFrom(userText, answer, this);
                if (learned > 0) {
                    System.out.println("  [Bilişsel Çekirdek] LLM aracından " + learned + " olgu öğrenildi");
                }
            }
            lastConfidence = r.confidence;
        } else if (fastMode && !deepMode) {
            if (consensusEnabled) {
                answer = consensusAnswer(userText, context.toString(), r, onToken);
            } else {
                answer = streamOnce(userText, context.toString(), r, onToken);
            }
            lastConfidence = r.confidence;
        } else {
            answer = multiAgentEnabled ? multiAgent.solve(userText, r, context.toString())
                                       : respondSingle(userText, r, context.toString());
            for (String chunk : chunks(answer, 18)) onToken.accept(chunk);
        }

        lastAnswer = answer;
        history.add(Json.msg("user", userText));
        history.add(Json.msg("assistant", answer));
        trimHistory();
        metrics.record(System.currentTimeMillis() - t0, r.intent, r.confidence, toolCallsThisTurn, true);
        if (autonomyEnabled) maybeAutonomousGoal();
        return answer;
    }

    /** Self-consistency: farklı sıcaklıklarla 3 cevap üret, en tutarlı olanı seç. */
    private String consensusAnswer(String text, String context, IntentRouter.Result r,
                                   java.util.function.Consumer<String> onToken) throws Exception {
        String system = buildFastSystem(context, r) + "\n" + brain.contextFor(text, this);
        List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>(history);
        msgs.add(Json.msg("user", text));
        client.setModel(chooseModel(text));
        double[] temps = {0.2, 0.5, 0.8};
        List<String> samples = new ArrayList<String>();
        for (double t : temps) {
            try { samples.add(chatWithFailover(text, msgs, system, t).trim()); }
            catch (Exception e) { /* bozuk örnek atlanır */ }
        }
        if (samples.isEmpty()) return streamOnce(text, context, r, onToken);
        String best = pickConsensus(samples);
        System.out.println("  [Consensus] " + samples.size() + " örnek, seçilen: " + shorten(best));
        for (String chunk : chunks(best, 14)) onToken.accept(chunk);
        return best;
    }

    private static String pickConsensus(List<String> samples) {
        Map<String, Integer> counts = new java.util.LinkedHashMap<String, Integer>();
        Map<String, String> canonical = new java.util.LinkedHashMap<String, String>();
        for (String s : samples) {
            if (s.isEmpty()) continue;
            String key = s.toLowerCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").trim();
            counts.put(key, (counts.get(key) == null ? 0 : counts.get(key)) + 1);
            if (!canonical.containsKey(key)) canonical.put(key, s);
        }
        String bestKey = null;
        int bestCount = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) { bestCount = e.getValue(); bestKey = e.getKey(); }
        }
        if (bestCount >= 2) return canonical.get(bestKey);
        String shortest = null;
        for (String s : samples) {
            if (s.isEmpty()) continue;
            if (shortest == null || s.length() < shortest.length()) shortest = s;
        }
        return shortest == null ? "" : shortest;
    }

    private String buildFastSystem(String context, IntentRouter.Result r) {
        String system =
            "Sen 'Yerel Ajan'sın. Kullanıcı Türkçe konuşuyor; kısa, net ve doğru Türkçe cevap ver.\n" +
            "Gereksiz giriş, tekrar veya uzun açıklama yapma.\n\n" +
            "[Sinir ağı analizi] Niyet: " + r.intent + " (güven " +
            String.format(java.util.Locale.ROOT, "%.2f", r.confidence) + ")\n";
        if (context != null && !context.trim().isEmpty()) {
            system += "\n[Bağlam / araç sonuçları]\n" + context + "\n";
        }
        system = "[KURAL — EN ÖNEMLİ] Sen gerçek araçların olan bir ajansın. " +
                "Kullanıcı bir dosya okumak/şifre hesaplamak/kod çalıştırmak/web'de aramak istediğinde " +
                "\"yapamam\" DEME, kullanıcıdan veri İSTEME, komut bekleme. " +
                "Doğrudan aracı çağır: cevabını SADECE {\"tool\":\"calculator|filesystem|clock|web_search|code_runner\"," +
                "\"args\":{...}} JSON'u olarak ver. Araç sonucu gelince doğal Türkçe cevap ver.\n\n" + system +
                "\n[KULLANILABİLİR ARAÇLAR]\n" + ToolCalls.describe(tools);
        return system;
    }

    /** Ana model düşerse sırayla yedekleri dener, hepsi düşerse DIDL yerel cevap verir. */
    private void streamWithFailover(String text, List<Map<String, Object>> msgs,
                                    java.util.function.Consumer<String> onToken) throws Exception {
        Exception last = null;
        String preferred = client.model(); // seçilen model (hızlı veya akıl)
        List<String> ordered = new ArrayList<String>();
        ordered.add(preferred);
        for (String m : fallbackModels) if (!m.equals(preferred)) ordered.add(m);
        for (String m : ordered) {
            try {
                client.setModel(m);
                System.out.println("  [Yönlendirme] model: " + m);
                client.chatStream(msgs, 0.2, onToken);
                return; // başarılı
            } catch (Exception e) {
                last = e;
                System.out.println("  [Yedek Beyin] " + m + " başarısız: " + shorten(e.getMessage()));
            }
        }
        System.out.println("  [Yedek Beyin] Tüm modeller düşte — DIDL yerel devrede. " +
                (last == null ? "" : "Son hata: " + shorten(last.getMessage())));
        String local = brain.think(text, this);
        for (String chunk : chunks(local, 14)) onToken.accept(chunk);
    }

    private String chatWithFailover(String text, List<Map<String, Object>> msgs,
                                    String system, double temperature) throws Exception {
        Exception last = null;
        for (String m : fallbackModels) {
            try {
                client.setModel(m);
                return client.chat(msgs, system, temperature);
            } catch (Exception e) {
                last = e;
            }
        }
        return brain.think(text, this);
    }

    private String streamOnce(String text, String context, IntentRouter.Result r,
                              java.util.function.Consumer<String> onToken) throws Exception {
        String system = buildFastSystem(context, r) + "\n" + brain.contextFor(text, this);
        String analogy = analogyFor(text);
        if (!analogy.isEmpty()) system += "\n" + analogy;
        List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>(history);
        msgs.add(Json.msg("user", text));
        client.setModel(chooseModel(text)); // hızlı mı, akıl modeli mi?
        StringBuilder sb = new StringBuilder();
        streamWithFailover(text, msgs, token -> sb.append(token));
        String out = sb.toString().trim();

        // Kullanıcı komut yazmasa bile LLM bir araç çağrısı ürettiyse çalıştır ve cevabı tamamla.
        Map<String, Object> call = ToolCalls.parse(out);
        if (call != null) {
            String name = Json.str(call.get("tool"));
            Object raw = call.get("args");
            Map<String, Object> args = raw instanceof Map
                    ? stringMap((Map<?, ?>) raw) : new HashMap<String, Object>();
            String result = runToolWithRepair(name, args, text);
            System.out.println("  [Otomatik Araç] " + name + " → " + shorten(result));
            msgs.add(Json.msg("user", "ARAÇ SONUCU (" + name + "): " + result +
                    "\nBunu kullanarak kullanıcıya doğal Türkçe cevap ver. JSON yazma."));
            StringBuilder finalSb = new StringBuilder();
            streamWithFailover(text, msgs, token -> { finalSb.append(token); onToken.accept(token); });
            return finalSb.toString().trim();
        }
        onToken.accept(out);
        return out;
    }

    /** LLM çağrısı yapmadan yerel araçlarla hesaplama/saat bilgisini önceden hesaplar. */
    private String precomputeTools(String text) {
        StringBuilder out = new StringBuilder();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+(?:\\s*[-+*/]\\s*\\d+)+)")
                .matcher(text);
        if (m.find()) {
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("expression", m.group(1));
            String result = runTool("calculator", args);
            if (!result.startsWith("Hata")) {
                out.append("[Hesap sonucu] ").append(m.group(1)).append(" = ").append(result).append('\n');
            }
        }
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("saat") || lower.contains("tarih") || lower.contains("zaman")) {
            out.append("[Saat/Tarih] ").append(runTool("clock", new HashMap<String, Object>())).append('\n');
        }

        // Dosya isteği: "readme.md dosyasını oku" → aracı kendiliğinden çalıştır
        java.util.regex.Matcher fm = java.util.regex.Pattern
                .compile("([\\w\\-./]+\\.(md|txt|java|json|py|bat|properties|xml))")
                .matcher(text);
        boolean wantsRead = lower.contains("oku") || lower.contains("aç") ||
                lower.contains("göster") || lower.contains("içerik") || lower.contains("bak");
        if (fm.find() && wantsRead) {
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("action", "read");
            args.put("path", fm.group(1));
            String result = runToolWithRepair("filesystem", args, text);
            out.append("[Dosya: ").append(fm.group(1)).append("]\n").append(result).append('\n');
        } else if ((lower.contains("listele") || lower.contains("klasör")) &&
                (lower.contains("dosya") || lower.contains("klasör"))) {
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("action", "list"); args.put("path", ".");
            out.append("[Klasör]\n").append(runTool("filesystem", args)).append('\n');
        }
        return out.toString();
    }

    private static List<String> chunks(String s, int size) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < s.length(); i += size) out.add(s.substring(i, Math.min(s.length(), i + size)));
        return out;
    }

    /** Tek ajanlı akış: planner -> adımlar -> reflection. */
    private String respondSingle(String userText, IntentRouter.Result r, String memContext) throws Exception {
        List<String> steps = plannerEnabled ? planner.plan(userText) : null;
        String draft;

        if (steps != null && steps.size() > 1) {
            System.out.println("  [Planner] " + steps.size() + " adım: " + steps);
            StringBuilder results = new StringBuilder();
            for (String step : steps) {
                String res = runStep(step, r.intent, memContext + "\n" + results);
                results.append("Adım: ").append(step).append("\nSonuç: ").append(res).append('\n');
            }
            draft = planner.synthesize(userText, results.toString());
        } else {
            draft = runStep(userText, r.intent, memContext);
        }

        if (reflectionEnabled) {
            Reflector.Review review = reflector.reviewMulti(userText, draft, memContext + "\n" + lastToolContext);
            lastConfidence = review.confidence;
            if (!draft.trim().equals(review.revised.trim())) {
                System.out.println("  [Reflection] düzeltildi (güven " +
                        String.format(java.util.Locale.ROOT, "%.2f", review.confidence) + "): " +
                        shorten(review.critique));
                draft = review.revised;
            }
        }
        return draft;
    }

    /** Bir alt görevi araç döngüsüyle çalıştırır (recursion'a girmez). */
    private String runStep(String text, String intent, String context) throws Exception {
        String system = buildSystem(intent, context);
        List<Map<String, Object>> msgs = new ArrayList<Map<String, Object>>(history);
        msgs.add(Json.msg("user", text));

        String answer = "";
        StringBuilder toolLog = new StringBuilder();
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            answer = client.chat(msgs, system, 0.3);
            Map<String, Object> call = ToolCalls.parse(answer);
            if (call == null) break;

            String toolName = Json.str(call.get("tool"));
            Object rawArgs = call.get("args");
            Map<String, Object> args = rawArgs instanceof Map
                    ? stringMap((Map<?, ?>) rawArgs)
                    : new HashMap<String, Object>();

            String result = runToolWithRepair(toolName, args, text);
            toolLog.append("ARAÇ SONUCU (").append(toolName).append("): ").append(result).append('\n');
            System.out.println("  [Araç] " + toolName + " -> " + shorten(result));
            msgs.add(Json.msg("user", "ARAÇ SONUCU (" + toolName + "): " + result +
                    "\nBu sonucu kullanarak kullanıcıya doğal Türkçe cevap ver. Araç çağrısı yazma."));
            answer = "";
        }

        if (answer.isEmpty() || ToolCalls.parse(answer) != null) {
            msgs.add(Json.msg("user", "Artık araç kullanma. Önceki araç sonuçlarını kullanarak kısa Türkçe cevap ver."));
            answer = client.chat(msgs, system, 0.2);
        }
        lastToolContext = toolLog.toString();
        return answer;
    }

    private String buildSystem(String intent, String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sen 'Yerel Ajan' adlı bir yapay zeka asistanısın. Kullanıcı Türkçe konuşuyor, Türkçe cevap ver.\n");
        sb.append("Kısa, net ve samimi ol. Gereksiz uzatma.\n\n");
        sb.append("[Yerel sinir ağı analizi] Kullanıcı niyeti: ").append(intent).append(".\n\n");

        if (context != null && !context.trim().isEmpty()) {
            sb.append("[İlgili anılar / bağlam]\n").append(context).append('\n');
        }
        sb.append("[Araçlar]\n").append(ToolCalls.describe(tools)).append('\n');
        sb.append(ToolCalls.protocol());
        return sb.toString();
    }

    /** Araç hatası olursa LLM'den düzeltilmiş argüman isteyip bir kez daha dener. */
    private String runToolWithRepair(String name, Map<String, Object> args, String task) {
        String result = runTool(name, args);
        if (!healing.isFailure(result)) return result;
        String fixed = healing.repair(name, result, Json.stringify(args), task);
        if (fixed == null) return result;
        Map<String, Object> call = ToolCalls.parse("{\"tool\":\"" + name + "\"," + fixed.substring(1));
        Object a = call == null ? null : call.get("args");
        if (!(a instanceof Map)) return result;
        System.out.println("  [Öz-onarım] " + name + " argümanları düzeltildi, yeniden deneniyor");
        return runTool(name, stringMap((Map<?, ?>) a));
    }

    private String runTool(String name, Map<String, Object> args) {
        if (name == null) return "Hata: araç adı yok.";
        for (Tool t : tools) {
            if (t.name().equalsIgnoreCase(name)) {
                toolCallsThisTurn++;
                try { return t.execute(args); }
                catch (Exception e) { return "Araç hatası: " + e.getMessage(); }
            }
        }
        return "Hata: bilinmeyen araç '" + name + "'. Kullanılabilirler: calculator, filesystem, clock.";
    }

    private static Map<String, Object> stringMap(Map<?, ?> src) {
        Map<String, Object> out = new java.util.LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> e : src.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
        return out;
    }

    private static String shorten(String s) {
        if (s == null) return "";
        String one = s.replace('\n', ' ').trim();
        return one.length() > 160 ? one.substring(0, 160) + "..." : one;
    }

    private void trimHistory() {
        while (history.size() > MAX_HISTORY) history.remove(0);
    }
}
