package gov.nasa.jpl.adaptation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity // This tells Hibernate to make a table out of this class
public class Adaptation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private String location;
    private String owner;
    private String mission;
    private String version;

    public Adaptation() {

    }

    public Adaptation(String name, String version, String owner, String mission, String location) {
        this.name = name;
        this.version = version;
        this.owner = owner;
        this.mission = mission;
        this.location = location;
    }

    public boolean equals(Adaptation adaptation) {
        return name.equals(adaptation.getName()) &&
                mission.equals(adaptation.getMission()) &&
                version.equals(adaptation.getVersion());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    @JsonIgnore
    @JsonProperty("location")
    public void setLocation(String location) {
        this.location = location;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
