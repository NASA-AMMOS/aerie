public class MerlinDurationParameter extends MerlinParameter<Float> {

    public Float getDefault() {
        return 1.0;
    }

    public String getUnits() {
        return "seconds";
    }
    public String getDimension() {
        return "time";
    }
    public String getBrief() {
        return "Length of time";
    }
    public String getDocumentation() {
        return "A duration for an activity, in seconds";
    }

    public List<Validator> getValidators() {
        return null;
    }
}