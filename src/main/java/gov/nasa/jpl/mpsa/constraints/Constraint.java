package gov.nasa.jpl.mpsa.constraints;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.UUID;

public abstract class Constraint implements Serializable, PropertyChangeListener {

    private UUID id;
    private String name;
    private String version;
    private String message;


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}