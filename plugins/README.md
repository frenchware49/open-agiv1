# Eklenti Klasörü

Buraya koyduğunuz `Tool` arayüzünü uygulayan derlenmiş `.class` dosyaları ajan
tarafından açılışta otomatik yüklenir. Kod değiştirmek gerekmez.

## Örnek araç

```java
package plugins;

import agent.Tool;
import java.util.Map;

public final class HavaDurumuAraci implements Tool {
    public String name() { return "weather"; }
    public String description() { return "Hava durumu verir. args: {\"city\":\"...\"}"; }
    public String execute(Map<String, Object> args) { return "..." ; }
}
```

Derleyip bu klasöre koyun:

```bat
javac -cp out-gui -d plugins plugins\HavaDurumuAraci.java
```

Ajanı yeniden başlattığınızda araç otomatik eklenir ve LLM sistem prompt'unda
görünür. `plugins/` klasörü git'e dahildir; kendi eklentilerinizi paylaşabilirsiniz.
