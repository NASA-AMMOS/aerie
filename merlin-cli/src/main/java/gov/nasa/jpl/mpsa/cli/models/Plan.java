package gov.nasa.jpl.mpsa.cli.models;


public class Plan {

    private String _id;

    private String adaptationId;
    private String endTimestamp;
    private String name;
    private String startTimestamp;

    public Plan() {}

    public Plan(
        String _id,
        String adaptationId,
        String endTimestamp,
        String name,
        String startTimestamp
    ) {
        this.set_id(_id);
        this.setAdaptationId(adaptationId);
        this.setEndTimestamp(endTimestamp);
        this.setName(name);
        this.setStartTimestamp(startTimestamp);
    }
    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getAdaptationId() {
        return adaptationId;
    }

    public void setAdaptationId(String adaptationId) {
        this.adaptationId = adaptationId;
    }

    public String getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(String endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(String startTimestamp) {
        this.startTimestamp = startTimestamp;
    }
}
