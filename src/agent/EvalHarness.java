package agent;

import agent.tools.CalculatorTool;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Hafif değerlendirme harness'ı: LLM çağrısı gerektirmeyen ölçülebilir testler. */
public final class EvalHarness {
    private int passed = 0;
    private int total = 0;

    public String run(IntentRouter router) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Değerlendirme ===\n\n");

        sb.append("[1] Niyet sınıflandırma (görülmemiş ifadeler)\n");
        String[][] cases = {
            {"3 çarpı 5 kaç yapar", "matematik"},
            {"yüzde 20 hesapla", "matematik"},
            {"readme dosyasını aç", "dosya"},
            {"klasör listesini göster", "dosya"},
            {"java sınıfı yaz", "kod"},
            {"fonksiyon hatalı çalışıyor", "kod"},
            {"bugün tarih kaç", "bilgi"},
            {"hava sıcaklığı kaç", "bilgi"},
            {"selam", "sohbet"},
            {"teşekkür ederim", "sohbet"}
        };
        for (String[] c : cases) {
            String got = router.route(c[0]).intent;
            check(got.equals(c[1]), c[0] + " -> " + got + " (beklenen: " + c[1] + ")");
        }

        sb.append("[2] Hesap makinesi\n");
        String[] exprs = {"2+2*3", "(10-4)/2", "100/4", "7*8-6"};
        String[] expected = {"8", "3", "25", "50"};
        CalculatorTool calc = new CalculatorTool();
        for (int i = 0; i < exprs.length; i++) {
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("expression", exprs[i]);
            String got = calc.execute(args);
            check(got.equals(expected[i]), exprs[i] + " -> " + got + " (beklenen: " + expected[i] + ")");
        }

        sb.append(String.format(Locale.ROOT, "\nSONUÇ: %d/%d geçti (%%%.0f)%n",
                passed, total, total == 0 ? 0 : 100.0 * passed / total));
        sb.append("\nNot: Bu test LLM içermez. LLM tabanlı senaryolar için /goal + /run kullanın.");
        return sb.toString();
    }

    private void check(boolean ok, String label) {
        total++;
        if (ok) passed++;
        System.out.println("  " + (ok ? "PASS " : "FAIL ") + label);
    }
}
