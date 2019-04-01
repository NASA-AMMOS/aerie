package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources;

import java.util.ArrayList;
import java.util.List;

public class ResourcesContainer {

    private static ResourcesContainer instance;
    private List<Resource> resources = new ArrayList<Resource>();

    private ResourcesContainer(){}

    public static synchronized ResourcesContainer getInstance(){
        if(instance == null){
            instance = new ResourcesContainer();
        }
        return instance;
    }

    public void addResource(Resource resource){
        resources.add(resource);
    }

    public List<Resource> getAvailableResources(){
        return this.resources;
    }

    public Resource getResourceByName(String name) {

        // Should this be a hashmap?? yes if we can guarantee that the resource names are unique.
        Resource resource = null;
        for(Resource r: this.resources) {
            if (r.getName().equals(name)) {
                resource = r;
                break;
            }
        }

        return resource;
    }
}
