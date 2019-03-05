
package gov.nasa.jpl.aerie.schemas;



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
    private Boolean selected;
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
     * @param selected
     */
    public CommandDictionary(String id, String name, Boolean selected, String version) {
        super();
        this.id = id;
        this.name = name;
        this.selected = selected;
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
    public Boolean getSelected() {
        return selected;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setSelected(Boolean selected) {
        this.selected = selected;
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
