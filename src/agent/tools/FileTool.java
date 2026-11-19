package agent.tools;

import agent.Json;
import agent.Tool;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

/** Proje klasörüyle sınırlı dosya işlemleri: read / write / list. */
public final class FileTool implements Tool {
    private final File root;

    public FileTool() {
        try {
            this.root = new File(".").getCanonicalFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override public String name() { return "filesystem"; }

    @Override public String description() {
        return "Dosya işlemleri. args: {\"action\":\"read|write|list\",\"path\":\"goreli/yol.txt\",\"content\":\"...\"}. " +
               "Yollar sadece proje klasörü içinde olabilir.";
    }

    @Override public String execute(Map<String, Object> args) {
        String action = Json.str(args.get("action"));
        String rel = Json.str(args.get("path"));
        if (rel == null) rel = Json.str(args.get("file"));
        if (rel == null || rel.trim().isEmpty()) return "Hata: 'path' argümanı eksik.";

        try {
            File target = safe(rel);
            if ("read".equalsIgnoreCase(action)) {
                if (!target.isFile()) return "Hata: dosya bulunamadı: " + rel;
                String content = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
                if (content.length() > 6000) content = content.substring(0, 6000) + "\n... (kırpıldı)";
                return content;
            }
            if ("write".equalsIgnoreCase(action)) {
                String content = Json.str(args.get("content"));
                if (content == null) return "Hata: 'content' argümanı eksik.";
                File parent = target.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                Files.write(target.toPath(), content.getBytes(StandardCharsets.UTF_8));
                return "Yazıldı: " + rel + " (" + content.length() + " karakter)";
            }
            if ("list".equalsIgnoreCase(action)) {
                if (!target.isDirectory()) return "Hata: klasör bulunamadı: " + rel;
                StringBuilder sb = new StringBuilder();
                File[] children = target.listFiles();
                if (children == null) return "Klasör okunamadı.";
                for (int i = 0; i < Math.min(children.length, 60); i++) {
                    sb.append(children[i].isDirectory() ? "[D] " : "[F] ")
                      .append(children[i].getName()).append('\n');
                }
                return sb.length() == 0 ? "(klasör boş)" : sb.toString();
            }
            return "Hata: bilinmeyen action '" + action + "'. read/write/list kullanın.";
        } catch (Exception e) {
            return "Dosya hatası: " + e.getMessage();
        }
    }

    private File safe(String rel) throws Exception {
        File f = new File(root, rel);
        String canonical = f.getCanonicalPath();
        String prefix = root.getPath() + File.separator;
        if (!canonical.equals(root.getPath()) && !canonical.startsWith(prefix)) {
            throw new SecurityException("Proje klasörü dışına erişim engellendi.");
        }
        return f;
    }
}
