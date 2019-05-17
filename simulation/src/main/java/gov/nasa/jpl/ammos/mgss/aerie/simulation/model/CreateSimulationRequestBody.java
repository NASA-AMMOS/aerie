package gov.nasa.jpl.ammos.mgss.aerie.simulation.model;

public class CreateSimulationRequestBody {

    private String scheduleId;
    private String adaptationId;

    public CreateSimulationRequestBody() {}

    public CreateSimulationRequestBody(String scheduleId, String adaptationId) {
        this.scheduleId = scheduleId;
        this.adaptationId = adaptationId;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(String scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getAdaptationId() {
        return adaptationId;
    }

    public void setAdaptationId(String adaptationId) {
        this.adaptationId = adaptationId;
    }
}
