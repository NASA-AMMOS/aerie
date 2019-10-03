package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models;

public final class ResponseAdaptation {
    public String name;
    public String version;
    public String mission;
    public String owner;

    public ResponseAdaptation(final Adaptation adaptation) {
        this.name = adaptation.name;
        this.version = adaptation.version;
        this.mission = adaptation.version;
        this.owner = adaptation.version;
    }

    public Adaptation toAdaptation() {
        final Adaptation adaptation = new Adaptation();
        adaptation.name = name;
        adaptation.version = version;
        adaptation.mission = mission;
        adaptation.owner = owner;
        return adaptation;
    }
}
