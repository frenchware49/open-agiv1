package agent.tools;

import agent.Json;
import agent.Tool;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Kodu gerçekten çalıştıran araç (Python / Java). Çalışma alanı proje içindeki
 * .agent-tmp klasörüyle sınırlıdır. Dikkat: keyfi kod çalıştırır, güvenmediğiniz
 * kod için kullanmayın.
 */
public final class CodeRunnerTool implements Tool {
    private static final String JDK = "C:\\Program Files\\Android\\openjdk\\jdk-21.0.8\\bin";
    private static final int TIMEOUT = 25;

    @Override public String name() { return "code_runner"; }

    @Override public String description() {
        return "Kod çalıştırır. args: {\"language\":\"python|java\",\"code\":\"...\"}. " +
               "Çıktıyı döndürür, en fazla 25 saniye.";
    }

    @Override public String execute(Map<String, Object> args) {
        String lang = Json.str(args.get("language"));
        String code = Json.str(args.get("code"));
        if (code == null || code.trim().isEmpty()) return "Hata: 'code' argümanı eksik.";
        if (lang == null) lang = "python";

        try {
            File dir = new File(".agent-tmp");
            if (!dir.exists() && !dir.mkdirs()) return "Hata: geçici klasör oluşturulamadı.";
            if ("java".equalsIgnoreCase(lang)) return runJava(code, dir);
            if ("python".equalsIgnoreCase(lang) || "py".equalsIgnoreCase(lang)) return runPython(code, dir);
            return "Hata: desteklenen diller python, java. ('" + lang + "')";
        } catch (Exception e) {
            return "Kod çalıştırma hatası: " + e.getMessage();
        }
    }

    private static String runPython(String code, File dir) throws Exception {
        File src = new File(dir, "ajan_kod.py");
        Files.write(src.toPath(), code.getBytes(StandardCharsets.UTF_8));
        String cmd = "python";
        try {
            Process p = new ProcessBuilder(cmd, src.getAbsolutePath())
                    .redirectErrorStream(true).start();
            String out = read(p);
            return out.trim().isEmpty() ? "(çıktı yok)" : out;
        } catch (Exception e) {
            return "Python çalıştırılamadı (" + e.getMessage() + "). 'python' PATH'te mi?";
        }
    }

    private static String runJava(String code, File dir) throws Exception {
        // LLM herhangi bir sınıf adı yazabilir (örn. "public class Merhaba").
        // Dosya adını ve çalıştırılacak sınıfı koddan tespit et.
        String className = "AjanKod";
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\bpublic\\s+class\\s+([A-Za-z_$][A-Za-z0-9_$]*)")
                .matcher(code);
        if (m.find()) className = m.group(1);

        File src = new File(dir, className + ".java");
        Files.write(src.toPath(), code.getBytes(StandardCharsets.UTF_8));

        String javac = new File(JDK, "javac.exe").exists() ? JDK + "\\javac.exe" : "javac";
        String java = new File(JDK, "java.exe").exists() ? JDK + "\\java.exe" : "java";

        Process compile = new ProcessBuilder(javac, "-encoding", "UTF-8",
                src.getAbsolutePath()).redirectErrorStream(true).start();
        String compileOut = read(compile);
        if (compile.exitValue() != 0) return "Derleme hatası:\n" + compileOut;

        Process run = new ProcessBuilder(java, "-cp", dir.getAbsolutePath(), className)
                .redirectErrorStream(true).start();
        String out = read(run);
        return out.trim().isEmpty() ? "(çıktı yok, derleme başarılı)" : out;
    }

    /** Süreci okur ve zaman aşımında sonlandırır; çıktı 6000 karakterle sınırlı. */
    private static String read(Process p) throws Exception {
        final StringBuilder sb = new StringBuilder();
        Thread t = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    InputStream in = p.getInputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        if (sb.length() < 6000) {
                            sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
        t.setDaemon(true);
        t.start();

        if (!p.waitFor(TIMEOUT, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            sb.append("\n(zaman aşımı: ").append(TIMEOUT).append(" sn)");
        }
        t.join(1000);
        String s = sb.toString();
        return s.length() > 6000 ? s.substring(0, 6000) + "\n...(kırpıldı)" : s;
    }
}
