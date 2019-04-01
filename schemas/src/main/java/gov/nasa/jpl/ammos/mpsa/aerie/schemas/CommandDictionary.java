
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;



/**
 * CommandDictionary
 * <p>
 * 
 * 
 */
public class CommandDictionary {

    /**
     * 
     * (Required)
     * 
     */
    private String id;
    /**
     * 
     * (Required)
     * 
     */
    private String name;
    /**
     * 
     * (Required)
     * 
     */
    private String version;

    /**
     * No args constructor for use in serialization
     * 
     */
    public CommandDictionary() {
    }

    /**
     * 
     * @param name
     * @param id
     * @param version
     */
    public CommandDictionary(String id, String name, String version) {
        super();
        this.id = id;
        this.name = name;
        this.version = version;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getId() {
        return id;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getVersion() {
        return version;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setVersion(String version) {
        this.version = version;
    }

}
