package gov.nasa.jpl.mpsa.resources;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArrayedResource {
    // composition relationship with Resource

    private HashMap<String, Resource> individualResources;
    private String name;

    public ArrayedResource(String name, String subsystem, String units, String interpolation, String[] entries) {
        this.name = name;
        this.individualResources = new HashMap<>();

        if(entries == null){
            throw new RuntimeException("Tried to create ArrayedResource with a null object for the list of entries. " +
                    "Make sure that your array gets initialized above the ArrayedResource in its class definition, " +
                    "or from a class that extends ParameterDeclaration");
        }

        for (int i = 0; i < entries.length; i++) {

            Resource resourceInstance = new Resource.Builder(name + "_" + entries[i])
                    .forSubsystem(subsystem)
                    .withUnits(units)
                    .withInterpolation(interpolation)
                    .build();

            individualResources.put(entries[i], resourceInstance);

        }

    }

    public ArrayedResource(String name, String subsystem, String units, String[] entries) {
        this(name, subsystem, units, "constant", entries);
    }

    public ArrayedResource(String name, String subsystem, String[] entries) {
        this(name, subsystem, "", entries);
    }

    public ArrayedResource(String name, String[] entries) {
        this(name, "", entries);
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
}