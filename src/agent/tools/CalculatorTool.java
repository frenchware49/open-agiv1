package agent.tools;

import agent.Json;
import agent.Tool;

import java.util.Locale;
import java.util.Map;

/** Güvenli matematik ifade değerlendirici (eval kullanmaz, recursive descent parser). */
public final class CalculatorTool implements Tool {
    @Override public String name() { return "calculator"; }

    @Override public String description() {
        return "Matematiksel ifade hesaplar. args: {\"expression\":\"(12+8)*3/2\"}. + - * / ve parantez destekler.";
    }

    @Override public String execute(Map<String, Object> args) {
        String expr = Json.str(args.get("expression"));
        if (expr == null) expr = Json.str(args.get("expr"));
        if (expr == null) return "Hata: 'expression' argümanı eksik.";
        try {
            double v = new Parser(expr).parseExpression();
            if (Double.isNaN(v) || Double.isInfinite(v)) return "Hata: geçersiz sonuç.";
            if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
            String s = String.format(Locale.ROOT, "%.6f", v);
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
            return s;
        } catch (Exception e) {
            return "Hesaplama hatası: " + e.getMessage();
        }
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) { this.s = s; }

        double parseExpression() {
            double v = parseTerm();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '+') { i++; v += parseTerm(); }
                else if (c == '-') { i++; v -= parseTerm(); }
                else break;
            }
            return v;
        }

        private double parseTerm() {
            double v = parseFactor();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '*') { i++; v *= parseFactor(); }
                else if (c == '/') {
                    i++;
                    double d = parseFactor();
                    if (d == 0) throw new IllegalArgumentException("sıfıra bölme");
                    v /= d;
                } else break;
            }
            return v;
        }

        private double parseFactor() {
            skip();
            if (i >= s.length()) throw new IllegalArgumentException("ifade eksik");
            char c = s.charAt(i);
            if (c == '-') { i++; return -parseFactor(); }
            if (c == '+') { i++; return parseFactor(); }
            if (c == '(') {
                i++;
                double v = parseExpression();
                skip();
                if (i >= s.length() || s.charAt(i) != ')') throw new IllegalArgumentException("'(' kapatılmadı");
                i++;
                return v;
            }
            int start = i;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
            if (start == i) throw new IllegalArgumentException("beklenmeyen karakter: " + c);
            return Double.parseDouble(s.substring(start, i));
        }

        private void skip() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }
    }
}
