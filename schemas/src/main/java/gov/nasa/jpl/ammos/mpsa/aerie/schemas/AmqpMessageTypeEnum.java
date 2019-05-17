
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;

import java.util.HashMap;
import java.util.Map;

public enum AmqpMessageTypeEnum {

    LOAD_ADAPTATION("LoadAdaptation"),
    UNLOAD_ADAPTATION("UnloadAdaptation"),
    SIMULATE_ACTIVITY("SimulateActivity");
    private final String value;
    private final static Map<String, AmqpMessageTypeEnum> CONSTANTS = new HashMap<String, AmqpMessageTypeEnum>();

    static {
        for (AmqpMessageTypeEnum c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    private AmqpMessageTypeEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public String value() {
        return this.value;
    }

    public static AmqpMessageTypeEnum fromValue(String value) {
        AmqpMessageTypeEnum constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
