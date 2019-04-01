
package gov.nasa.jpl.ammos.mpsa.aerie.schemas;



/**
 * Adaptation
 * <p>
 * 
 * 
 */
public class Adaptation {

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
    private String location;
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
    private String mission;
    /**
     * 
     * (Required)
     * 
     */
    private String owner;
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
    public Adaptation() {
    }

    /**
     * 
     * @param owner
     * @param mission
     * @param name
     * @param location
     * @param id
     * @param version
     */
    public Adaptation(String id, String location, String name, String mission, String owner, String version) {
        super();
        this.id = id;
        this.location = location;
        this.name = name;
        this.mission = mission;
        this.owner = owner;
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
    public String getLocation() {
        return location;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setLocation(String location) {
        this.location = location;
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
    public String getMission() {
        return mission;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setMission(String mission) {
        this.mission = mission;
    }

    /**
     * 
     * (Required)
     * 
     */
    public String getOwner() {
        return owner;
    }

    /**
     * 
     * (Required)
     * 
     */
    public void setOwner(String owner) {
        this.owner = owner;
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
