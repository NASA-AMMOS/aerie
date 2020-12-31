package gov.nasa.jpl.ammos.mpsa.aerie.services.cli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.services.cli.exceptions.InvalidTokenException;

import static gov.nasa.jpl.ammos.mpsa.aerie.services.cli.models.TokenMap.parseToken;

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

    public static Adaptation fromTokens(String[] tokens) throws InvalidTokenException {
        Adaptation adaptation = new Adaptation();

        for (String token : tokens) {
            TokenMap tokenMap = parseToken(token);
            switch(tokenMap.getName()) {
                case "name":
                    adaptation.setName(tokenMap.getValue());
                    break;
                case "version":
                    adaptation.setVersion(tokenMap.getValue());
                    break;
                case "mission":
                    adaptation.setMission(tokenMap.getValue());
                    break;
                case "owner":
                    adaptation.setOwner(tokenMap.getValue());
                    break;
                default:
                    throw new InvalidTokenException(token, String.format("'%s' is not a valid attribute", tokenMap.getName()));
            }
        }

        return adaptation;
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

    @Override
    public boolean equals(Object o) {
        if (o instanceof Adaptation) {
            Adaptation other = (Adaptation)o;
            return other.getName().equals(this.name)
                    && other.getVersion().equals(this.version)
                    && other.getMission().equals(this.mission)
                    && other.getOwner().equals(this.owner);
        }

        return false;
    }
}
