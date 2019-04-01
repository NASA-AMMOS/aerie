package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.ResourceBuilder;
import java.util.*;

public class ArrayedResource<V extends Comparable> {
    // composition relationship with Resource

    private HashMap<String, Resource> individualResources;
    private String name;
    private String[] entries;

    private ArrayedResource(Builder builder){
        this.name = builder.name;
        this.entries = builder.entries;
        this.individualResources = new HashMap<>();

        if(this.entries == null){
            throw new RuntimeException("Tried to create ArrayedResource with a null object for the list of entries. " +
                    "Make sure that your array gets initialized above the ArrayedResource in its class definition, " +
                    "or from a class that extends ParameterDeclaration");
        }

        for (int i = 0; i < this.entries.length; i++) {

            ResourceBuilder resourceInstance = new ResourceBuilder()
                .withName(this.name + "_" + this.entries[i])
                .forSubsystem(builder.subsystem)
                .withUnits(builder.units)
                .withInterpolation(builder.interpolation)
                .withMin((V) builder.minimum)
                .withMax((V) builder.maximum)
                .withAllowedValues(builder.allowedValues)
                .isFrozen(builder.frozen)
                .withInitialValue(builder.initialValue);

            individualResources.put(this.entries[i], resourceInstance.getResource());

        }

    }

    public Resource get(String index) {
        Resource ofInterest = individualResources.get(index);
        if (ofInterest == null) {
            throw new IndexOutOfBoundsException("Index " + index + " not found in ArrayedResource " + name);
        }
        else {
            return ofInterest;
        }
    }

    public String[] getEntries() {
        String[] indices = new String[individualResources.size()];
        return individualResources.keySet().toArray(indices);
    }


    // goes through all maps and registers member resources with the given resource list
    public void registerArrayedResource(ResourcesContainer resources) {
        for (Map.Entry<String, Resource> singleRes : individualResources.entrySet()) {
            resources.addResource(singleRes.getValue());
        }
    }

    // Sticking with a builder design pattern for a streamlined interface
    public static class Builder<V extends Comparable> {

        private String name;
        private String subsystem;
        private String units;
        private String interpolation;
        private Set allowedValues;
        private V minimum;
        private V maximum;
        private String[] entries;
        private boolean frozen;
        private V initialValue;

        public Builder(String name) {
            this.name = name;
        }

        public Builder withInitialValue(V value) {
            this.initialValue = value;
            return this;
        }

        public Builder withEntries(String[] entries) {
            this.entries = entries;
            return this;
        }

        public Builder forSubsystem(String subsystem) {
            this.subsystem = subsystem;
            return this;
        }

        public Builder withUnits(String units) {
            this.units = units;
            return this;
        }

        public Builder withInterpolation(String interpolation) {
            this.interpolation = interpolation;
            return this;
        }

        public Builder withAllowedValues(Set allowedValues) {
            this.allowedValues = allowedValues;
            return this;
        }

        public Builder withMin(V minimum) {
            this.minimum = minimum;
            return this;
        }

        public Builder withMax(V maximum) {
            this.maximum = maximum;
            return this;
        }

        public Builder isFrozen(boolean frozen) {
            this.frozen = frozen;
            return this;
        }

        public ArrayedResource build(){
            if (this.entries == null) {
                this.entries = new String[]{""};
            }
            return new ArrayedResource(this);
        }

    }
}