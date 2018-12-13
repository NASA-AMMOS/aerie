package gov.nasa.jpl.mpsa.activities;


public class Parameter {

    private String name;
    private Object type;
    private Object value;
    private boolean readOnly;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getType() {
        return type;
    }

    public void setType(Object type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }


    public static class Builder {

        private String name;
        private Object type;
        private Object value;
        private boolean readOnly = false;

        public Builder(String name) {
            this.name = name;
        }

        public Builder ofType(Object type) {
            this.type = type;
            return this;
        }

        public Builder withValue(Object value) {
            this.value = value;
            return this;
        }

        public Builder asReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Parameter build(){
            return new Parameter(this);
        }
    }

    private Parameter(Builder builder){
        this.name = builder.name;
        this.type = builder.type;
        this.value = builder.value;
        this.readOnly = builder.readOnly;
    }

}
