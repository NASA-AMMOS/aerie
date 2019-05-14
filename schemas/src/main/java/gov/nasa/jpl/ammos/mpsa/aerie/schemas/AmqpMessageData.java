
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;

import java.util.HashMap;
import java.util.Map;

public class AmqpMessageData {

    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
