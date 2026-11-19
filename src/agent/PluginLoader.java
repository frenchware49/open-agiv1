package agent;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Eklenti sistemi: plugins/klasorundaki derlenmis Tool siniflarini yukler.
 * Kod degistirmeden yeni arac eklemek icin: Tool arayuzunu uygulayan bir sinif
 * derleyip plugins/ altina koyun, ajani yeniden baslatin. Otomatik yuklenir.
 */
public final class PluginLoader {
    public static int load(Agent agent) {
        File dir = new File("plugins");
        if (!dir.exists() || !dir.isDirectory()) return 0;
        int loaded = 0;
        try {
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{dir.toURI().toURL()},
                    PluginLoader.class.getClassLoader());
            File[] files = dir.listFiles();
            if (files == null) return 0;
            for (File f : files) {
                if (!f.getName().endsWith(".class")) continue;
                String className = f.getName().substring(0, f.getName().length() - 6);
                try {
                    Class<?> c = Class.forName(className, true, loader);
                    if (Tool.class.isAssignableFrom(c)) {
                        agent.addTool((Tool) c.getDeclaredConstructor().newInstance());
                        loaded++;
                        System.out.println("  [Eklenti] yuklendi: " + className);
                    }
                } catch (Exception ignored) {
                    // ilgisiz siniflar atlanir
                }
            }
        } catch (Exception e) {
            System.out.println("  [Eklenti] yukleme hatasi: " + e.getMessage());
        }
        return loaded;
    }
}
