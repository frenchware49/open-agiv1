package agent;

import agent.server.AgentServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Yerel hibrit ajanın komut satırı arayüzü. */
public final class Main {
    private static AgentServer server;

    public static void main(String[] args) throws Exception {
        String model = args.length > 0 ? args[0] : "gemma4:e4b";

        System.out.println("=========================================");
        System.out.println("  YEREL HİBRİT AJAN  (v2 - AGI denemesi)");
        System.out.println("  Beyin : Ollama (" + model + ")");
        System.out.println("  Router: kendi sinir ağımız (MLP)");
        System.out.println("=========================================");

        Agent agent = new Agent(model);
        try {
            agent.check();
        } catch (Exception e) {
            System.out.println("\nBAĞLANTI HATASI: " + e.getMessage());
            System.out.println("Ollama'nın açık olduğundan emin olun: ollama serve");
            return;
        }

        printStatus(agent);
        System.out.println("Komutlar: /help  /plan  /reflect  /mode  /remember  /recall  /model  /exit\n");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            System.out.print("Sen> ");
            String line = br.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.equalsIgnoreCase("/exit") || line.equalsIgnoreCase("/quit")) break;

            if (line.equalsIgnoreCase("/help")) {
                System.out.println("/eval                LLM'siz değerlendirme testleri");
                System.out.println("/bench               LLM tabanlı genelleme benchmark'ı");
                System.out.println("/image <dosya> <soru>  görüntülü soru (Gemma 4)");
                System.out.println("/research <url>      URL'den araştırıp öğren");
                System.out.println("/research-all [n]    kaynak listesindeki URL'leri araştır");
                System.out.println("/sources             kaynak URL'lerini listele");
                System.out.println("/addsource <url>     yeni kaynak URL'si ekle");
                System.out.println("/models              kurulu modeller + Gemma 4 önerileri");
                System.out.println("/help                bu yardım");
                System.out.println("/plan on|off         görev planlayıcıyı aç/kapat");
                System.out.println("/reflect on|off      öz-denetimi aç/kapat");
                System.out.println("/mode single|multi   tek ajan / çok ajan modu");
                System.out.println("/fast on|off        hızlı streaming mod (varsayılan açık)");
                System.out.println("/deep on|off        derin mod: planner + reflection + uzmanlar");
                System.out.println("/meta               öğrenmeyi öğrenme raporu");
                System.out.println("/world              dünya modeli durumu");
                System.out.println("/predict <varlık> <eylem>  sonuç tahmini");
                System.out.println("/simulate <varlık> [n]     çok adımlı simülasyon");
                System.out.println("/rules              öğrenilen nedensel kurallar");
                System.out.println("/observe <özne> <eylem> <sonuç>  dünya modeline gözlem ekle");
                System.out.println("/autonomy on|off    özerk hedef üretimini aç/kapat");
                System.out.println("/autogoal           şimdi kendiliğinden hedef üret");
                System.out.println("/autorun [n]        özerk hedefleri çalıştır");
                System.out.println("/consensus on|off   self-consistency (3 örnek, en tutarlı cevap)");
                System.out.println("/metrics            performans istatistikleri");
                System.out.println("/brain              DIDL beyninin durumu");
                System.out.println("/brain on|off       beyni aç/kapat");
                System.out.println("/fallback           yedek beyin zincirini göster");
                System.out.println("/immune             bağışıklık sistemi raporu");
                System.out.println("/nlp <metin>        gelişmiş metin analizi");
                System.out.println("/summarize <metin>  çıkarımsal özet");
                System.out.println("/keyphrases <metin> anahtar ifadeler");
                System.out.println("/qa <soru> | <bağlam>  çıkarımsal soru-cevap");
                System.out.println("/tsearch <sorgu>    hafızada anlamsal metin arama");
                System.out.println("/agi                AGI-v1 profil raporu");
                System.out.println("/selfmodel          ajanın kendini tanıması");
                System.out.println("/analogy <metin>    analoji/transfer çıkarımı");
                System.out.println("/curriculum         kendi müfredatından bir tur öğren");
                System.out.println("/counterfactual <varlık> <eylem>  ya öyle olmasaydı?");
                System.out.println("/core               bilişsel çekirdek raporu");
                System.out.println("/core autonomous|assisted|model  mod değiştir");
                System.out.println("/think <metin>      sadece kendi beynimizle cevap (LLM yok)");
                System.out.println("/serve              HTTP API sunucusunu başlat (:8080)");
                System.out.println("/stop               API sunucusunu durdur");
                System.out.println("/remember <metin>    vektör hafızaya not ekle");
                System.out.println("/recall <sorgu>      hafızada anlamsal ara");
                System.out.println("/memories            tüm anıları listele");
                System.out.println("/teach <niyet> <metin>  yerel ağa yeni örnek öğret");
                System.out.println("/wrong <niyet>       son mesajın niyetini düzelt");
                System.out.println("/good                son mesajı mevcut niyetiyle pekiştir");
                System.out.println("/learnstatus         sürekli öğrenme durumu");
                System.out.println("/goal <hedef>        özerk hedef belirle (alt hedefleri üretir)");
                System.out.println("/goals               hedef listesini göster");
                System.out.println("/run [n]             hedefleri özerk çalıştır (varsayılan 5)");
                System.out.println("/nextgoal            sadece bir sonraki hedefi çalıştır");
                System.out.println("/cleargoals          hedefleri temizle");
                System.out.println("/graph               dünya modeli istatistikleri");
                System.out.println("/graph on|off        grafik bağlamını aç/kapat");
                System.out.println("/facts <varlık>      varlığın ilişkilerini göster");
                System.out.println("/path <a> <b>        iki varlık arasındaki ilişki zinciri");
                System.out.println("/extract             son konuşmadan olgu çıkar");
                System.out.println("/clearkgraph         bilgi grafiğini temizle");
                System.out.println("/forget              hafızayı temizle");
                System.out.println("/model <isim>        model değiştir");
                System.out.println("/clear               konuşma geçmişini temizle");
                System.out.println("/exit                çık");
                continue;
            }
            if (line.equalsIgnoreCase("/brain")) { System.out.println(agent.brainStats()); continue; }
            if (line.startsWith("/nlp")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /nlp <metin>");
                else System.out.println(agent.analyzeText(p[1]));
                continue;
            }
            if (line.startsWith("/summarize")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /summarize <metin>");
                else System.out.println(agent.summarizeText(p[1], 2));
                continue;
            }
            if (line.startsWith("/keyphrases")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /keyphrases <metin>");
                else System.out.println(agent.keyphrases(p[1], 8));
                continue;
            }
            if (line.startsWith("/qa")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2 || !p[1].contains("|")) {
                    System.out.println("Kullanım: /qa <soru> | <bağlam>");
                } else {
                    String[] qc = p[1].split("\\|", 2);
                    System.out.println(agent.textAnswer(qc[0], qc[1]));
                }
                continue;
            }
            if (line.startsWith("/tsearch")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /tsearch <sorgu>");
                else System.out.println(agent.textSearch(p[1], 3));
                continue;
            }
            if (line.equalsIgnoreCase("/agi")) { System.out.println(agent.agiReport()); continue; }
            if (line.equalsIgnoreCase("/selfmodel")) { System.out.println(agent.selfModelText()); continue; }
            if (line.startsWith("/analogy")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /analogy <metin>");
                else {
                    String a = agent.analogyFor(p[1]);
                    System.out.println(a.isEmpty() ? "Benzer kalıp bulunamadı." : a);
                }
                continue;
            }
            if (line.equalsIgnoreCase("/curriculum")) { System.out.println(agent.curriculumStep()); continue; }
            if (line.startsWith("/counterfactual")) {
                String[] p = line.split("\\s+", 3);
                if (p.length < 3) System.out.println("Kullanım: /counterfactual Ahmet seviyor");
                else System.out.println(agent.counterfactual(p[1], p[2]));
                continue;
            }
            if (line.startsWith("/core")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println(agent.coreReport());
                else { agent.setCoreMode(p[1]); System.out.println("Çekirdek modu: " + agent.core().mode()); }
                continue;
            }
            if (line.equalsIgnoreCase("/immune")) { System.out.println(agent.immuneReport()); continue; }
            if (line.equalsIgnoreCase("/fallback")) {
                System.out.println("Yedek beyin zinciri: " + agent.fallbackChain());
                continue;
            }
            if (line.startsWith("/think")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /think <metin>");
                else System.out.println(agent.brainThink(p[1]));
                continue;
            }
            if (line.equalsIgnoreCase("/metrics")) { System.out.println(agent.metrics().summary()); continue; }
            if (line.equalsIgnoreCase("/serve")) {
                if (server == null) {
                    int port = Config.load().serverPort;
                    server = new AgentServer(agent, port);
                    server.start();
                } else System.out.println("Sunucu zaten çalışıyor.");
                continue;
            }
            if (line.equalsIgnoreCase("/stop")) {
                if (server != null) { server.stop(); server = null; System.out.println("Sunucu durduruldu."); }
                else System.out.println("Çalışan sunucu yok.");
                continue;
            }
            if (line.equalsIgnoreCase("/meta")) { System.out.println(agent.metaReport()); continue; }
            if (line.equalsIgnoreCase("/world")) { System.out.println(agent.worldStats()); continue; }
            if (line.equalsIgnoreCase("/rules")) { System.out.println(agent.worldRules()); continue; }
            if (line.startsWith("/observe")) {
                String[] p = line.split("\\s+", 4);
                if (p.length < 4) System.out.println("Kullanım: /observe Ahmet seviyor Python");
                else { agent.observe(p[1], p[2], p[3]); System.out.println("Gözlem eklendi: " + agent.worldStats()); }
                continue;
            }
            if (line.startsWith("/predict")) {
                String[] p = line.split("\\s+", 3);
                if (p.length < 3) System.out.println("Kullanım: /predict Ahmet seviyor");
                else System.out.println(agent.worldPredict(p[1], p[2]));
                continue;
            }
            if (line.startsWith("/simulate")) {
                String[] p = line.split("\\s+", 3);
                int n = 3;
                if (p.length > 2) { try { n = Integer.parseInt(p[2]); } catch (Exception ignored) {} }
                System.out.println(agent.worldSimulate(p.length > 1 ? p[1] : "Ahmet", n));
                continue;
            }
            if (line.equalsIgnoreCase("/autogoal")) { System.out.println(agent.suggestAutonomousGoal()); continue; }
            if (line.startsWith("/autorun")) {
                String[] p = line.split("\\s+", 2);
                int n = 3;
                if (p.length > 1) { try { n = Integer.parseInt(p[1]); } catch (Exception ignored) {} }
                System.out.println(agent.runAutonomousGoals(n));
                continue;
            }
            if (line.equalsIgnoreCase("/eval")) { System.out.println(agent.runEvaluation()); continue; }
            if (line.equalsIgnoreCase("/bench")) { System.out.println(agent.runBenchmark()); continue; }
            if (line.startsWith("/image")) {
                String[] p = line.split("\\s+", 3);
                if (p.length < 3) System.out.println("Kullanım: /image <dosya_yolu> <soru>");
                else {
                    try { System.out.println("Ajan> " + agent.respondWithImage(p[1], p[2])); }
                    catch (Exception e) { System.out.println("Görüntü hatası: " + e.getMessage()); }
                }
                continue;
            }
            if (line.startsWith("/research-all")) {
                String[] p = line.split("\\s+", 2);
                int n = 3;
                if (p.length > 1) { try { n = Integer.parseInt(p[1].trim()); } catch (Exception ignored) {} }
                System.out.println(agent.researchSources(n));
                continue;
            }
            if (line.startsWith("/research")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /research <url>");
                else System.out.println(agent.researchUrl(p[1]));
                continue;
            }
            if (line.equalsIgnoreCase("/sources")) { System.out.println(agent.researchSeeds()); continue; }
            if (line.startsWith("/addsource")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /addsource <url>");
                else { agent.addSource(p[1]); System.out.println("Kaynak eklendi."); }
                continue;
            }
            if (line.equalsIgnoreCase("/models")) {
                System.out.println("Kurulu modeller:");
                try {
                    for (String m : agent.availableModels()) System.out.println("  • " + m);
                } catch (Exception e) { System.out.println("  " + e.getMessage()); }
                System.out.println("\nGemma 4 önerileri (8 GB VRAM):");
                System.out.println("  gemma4:e4b   ~6.6 GB  - önerilen");
                System.out.println("  gemma4:e2b   ~4.6 GB  - en hafif");
                System.out.println("  gemma4:12b   ~7.7 GB  - sınırda");
                System.out.println("  gemma4:26b   ~16 GB   - bu GPU'ya sığmaz");
                continue;
            }
            if (line.equalsIgnoreCase("/status")) { printStatus(agent); continue; }

            if (line.startsWith("/plan") || line.startsWith("/reflect") || line.startsWith("/mode")
                    || line.startsWith("/fast") || line.startsWith("/deep") || line.startsWith("/autonomy")
                    || line.startsWith("/consensus") || line.startsWith("/brain")) {
                String[] p = line.split("\\s+", 2);
                String flag = p[0].substring(1);
                String value = p.length > 1 ? p[1].trim() : "";
                if ("plan".equals(flag)) {
                    Boolean b = parseBool(value, agent.plannerEnabled(), "plan");
                    if (b != null) agent.setPlanner(b);
                } else if ("reflect".equals(flag)) {
                    Boolean b = parseBool(value, agent.reflectionEnabled(), "reflect");
                    if (b != null) agent.setReflection(b);
                } else if ("mode".equals(flag)) {
                    if (value.equalsIgnoreCase("multi")) agent.setMultiAgent(true);
                    else if (value.equalsIgnoreCase("single")) agent.setMultiAgent(false);
                    else System.out.println("Kullanım: /mode single|multi");
                } else if ("fast".equals(flag)) {
                    Boolean b = parseBool(value, agent.fastMode(), "fast");
                    if (b != null) agent.setFastMode(b);
                } else if ("deep".equals(flag)) {
                    Boolean b = parseBool(value, agent.deepMode(), "deep");
                    if (b != null) agent.setDeepMode(b);
                } else if ("autonomy".equals(flag)) {
                    Boolean b = parseBool(value, agent.autonomyEnabled(), "autonomy");
                    if (b != null) agent.setAutonomy(b);
                } else if ("consensus".equals(flag)) {
                    Boolean b = parseBool(value, agent.consensusEnabled(), "consensus");
                    if (b != null) agent.setConsensus(b);
                } else if ("brain".equals(flag)) {
                    Boolean b = parseBool(value, agent.brainEnabled(), "brain");
                    if (b != null) agent.setBrain(b);
                }
                printStatus(agent);
                continue;
            }
            if (line.startsWith("/model")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) {
                    System.out.println("Şu anki model: " + agent.model());
                } else {
                    String prev = agent.model();
                    agent.setModel(p[1].trim());
                    try {
                        agent.check();
                        System.out.println("Model değiştirildi: " + agent.model());
                    } catch (Exception e) {
                        agent.setModel(prev);
                        System.out.println("Model bulunamadı: " + e.getMessage());
                    }
                }
                continue;
            }
            if (line.startsWith("/remember")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /remember <metin>");
                else { agent.remember(p[1]); System.out.println("Vektör hafızaya eklendi (" + agent.memorySize() + " anı)."); }
                continue;
            }
            if (line.startsWith("/recall")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /recall <sorgu>");
                else {
                    java.util.List<String> hits = agent.recall(p[1]);
                    if (hits.isEmpty()) System.out.println("İlgili anı bulunamadı.");
                    else for (String h : hits) System.out.println("  • " + h);
                }
                continue;
            }
            if (line.equalsIgnoreCase("/memories")) {
                System.out.println(agent.memoryList());
                continue;
            }
            if (line.startsWith("/teach")) {
                String[] p = line.split("\\s+", 3);
                if (p.length < 3) {
                    System.out.println("Kullanım: /teach matematik \"bir şey hesapla\"");
                } else {
                    String intent = p[1].trim();
                    String text = p[2].trim().replaceAll("^\"|\"$", "");
                    IntentRouter.LearnResult res = agent.teach(text, intent);
                    System.out.println("  [Öğrenme] " + res.message);
                }
                continue;
            }
            if (line.startsWith("/wrong")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /wrong matematik");
                else {
                    IntentRouter.LearnResult res = agent.correctLast(p[1].trim());
                    System.out.println("  [Öğrenme] " + res.message);
                }
                continue;
            }
            if (line.equalsIgnoreCase("/good")) {
                IntentRouter.LearnResult res = agent.reinforceLast();
                System.out.println("  [Öğrenme] " + res.message);
                continue;
            }
            if (line.equalsIgnoreCase("/learnstatus")) {
                System.out.println(agent.learnStatus());
                continue;
            }
            if (line.startsWith("/goal")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /goal <hedef>");
                else {
                    agent.setObjective(p[1]);
                    System.out.println("Hedefler oluşturuldu:\n" + agent.renderGoals());
                }
                continue;
            }
            if (line.equalsIgnoreCase("/goals")) { System.out.println(agent.renderGoals()); continue; }
            if (line.startsWith("/run")) {
                String[] p = line.split("\\s+", 2);
                int n = 5;
                if (p.length > 1) {
                    try { n = Integer.parseInt(p[1].trim()); }
                    catch (NumberFormatException e) { System.out.println("Geçersiz sayı, 5 kullanılıyor."); }
                }
                System.out.println(agent.runAutonomous(n));
                continue;
            }
            if (line.equalsIgnoreCase("/nextgoal")) { System.out.println(agent.runNextGoal()); continue; }
            if (line.equalsIgnoreCase("/cleargoals")) { agent.clearGoals(); System.out.println("Hedefler temizlendi."); continue; }
            if (line.equalsIgnoreCase("/graph")) { System.out.println(agent.graphStats()); continue; }
            if (line.equalsIgnoreCase("/clearkgraph")) { agent.clearGraph(); System.out.println("Bilgi grafiği temizlendi."); continue; }
            if (line.equalsIgnoreCase("/extract")) {
                int n = agent.extractFactsFromLast();
                System.out.println("  [Dünya modeli] " + n + " yeni olgu çıkarıldı. " + agent.graphStats());
                continue;
            }
            if (line.startsWith("/facts")) {
                String[] p = line.split("\\s+", 2);
                if (p.length < 2) System.out.println("Kullanım: /facts Ahmet");
                else System.out.println(agent.graphFacts(p[1]));
                continue;
            }
            if (line.startsWith("/path")) {
                String[] p = line.split("\\s+", 3);
                if (p.length < 3) System.out.println("Kullanım: /path Ahmet Python");
                else System.out.println(agent.graphPath(p[1], p[2]));
                continue;
            }
            if (line.startsWith("/graph ")) {
                String v = line.substring(7).trim();
                if (v.equalsIgnoreCase("on")) agent.setGraphEnabled(true);
                else if (v.equalsIgnoreCase("off")) agent.setGraphEnabled(false);
                else System.out.println("Kullanım: /graph on|off");
                printStatus(agent);
                continue;
            }
            if (line.equalsIgnoreCase("/forget")) {
                agent.forget();
                System.out.println("Vektör hafıza temizlendi.");
                continue;
            }
            if (line.equalsIgnoreCase("/clear")) {
                agent.clearHistory();
                System.out.println("Konuşma geçmişi temizlendi.");
                continue;
            }
            if (line.startsWith("/")) {
                System.out.println("Bilinmeyen komut. /help yazın.");
                continue;
            }

            try {
                System.out.print("Ajan> ");
                agent.respondStream(line, s -> System.out.print(s));
                System.out.println("\n");
            } catch (Exception e) {
                System.out.println("Hata: " + e.getMessage());
            }
        }
        System.out.println("Görüşmek üzere!");
    }

    private static void printStatus(Agent a) {
        System.out.println("Durum → Planner: " + onOff(a.plannerEnabled()) +
                " | Reflection: " + onOff(a.reflectionEnabled()) +
                " | Vektör hafıza: " + a.memorySize() + " anı" +
                " | Öğrenilen: " + a.learnedCount() +
                " | Dünya modeli: " + (a.graphEnabled() ? "AÇIK" : "KAPALI") +
                " | Bekleyen hedef: " + a.pendingGoalCount() +
                " | DIDL: " + onOff(a.brainEnabled()) +
                " | Consensus: " + onOff(a.consensusEnabled()) +
                " | Mod: " + (a.multiAgentEnabled() ? "çok ajan" : "tek ajan"));
    }

    private static String onOff(boolean b) { return b ? "AÇIK" : "KAPALI"; }

    private static Boolean parseBool(String value, boolean current, String name) {
        if (value.equalsIgnoreCase("on") || value.equalsIgnoreCase("aç") || value.equalsIgnoreCase("ac")) return true;
        if (value.equalsIgnoreCase("off") || value.equalsIgnoreCase("kapat")) return false;
        if (value.isEmpty()) System.out.println(name + " şu an: " + onOff(current) + " (on|off kullanın)");
        else System.out.println("Kullanım: /" + name + " on|off");
        return null;
    }
}
