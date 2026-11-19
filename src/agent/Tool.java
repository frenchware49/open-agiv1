package agent;

import java.util.Map;

/** Ajanın kullanabileceği bir araç. */
public interface Tool {
    String name();
    String description();
    String execute(Map<String, Object> args) throws Exception;
}
