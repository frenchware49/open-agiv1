package agent.tools;

import agent.Tool;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** Güncel tarih ve saati Türkiye saat diliminde verir. */
public final class ClockTool implements Tool {
    @Override public String name() { return "clock"; }

    @Override public String description() {
        return "Güncel tarih ve saati verir. args: {}";
    }

    @Override public String execute(Map<String, Object> args) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy EEEE HH:mm:ss", new java.util.Locale("tr", "TR"));
        return now.format(fmt);
    }
}
