public abstract class MerlinParameter<T> {
    private T value;

    public MerlinParameter(T val) {
        setValue(val);
    }

    // Method for static fields -- static fields can't be set by children
    public abstract T getDefault();
    public abstract String getUnits();
    public abstract String getDimension();
    public abstract String getBrief();
    public abstract String getDocumentation();
    public abstract List<Validator> getValidators;

    public void setValue(T val) {
        value = val;
    }

    public T getValue() {
        return value;
    }
}