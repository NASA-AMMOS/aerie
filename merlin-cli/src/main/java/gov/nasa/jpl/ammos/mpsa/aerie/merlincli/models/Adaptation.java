package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

public class Adaptation {

    private String name;
    private String version;
    private String mission;
    private String owner;

    public Adaptation() {
        this.name = null;
        this.version = null;
        this.mission = null;
        this.owner = null;
    }

    public Adaptation(String name, String version, String mission, String owner) {
        this.name = name;
        this.version = version;
        this.mission = mission;
        this.owner = owner;
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

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
