package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;


import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Registry {

    private List<Event> eventLog = new ArrayList<>();
    private Map<Pair<SystemModel, String>, Supplier> modelMap = new HashMap<>();


    public void provide(SystemModel model, String stateName, Supplier supplier) {
        Pair<SystemModel, String> key = new Pair<>(model, stateName);
        modelMap.put(key, supplier);
    }

    public Supplier getSupplier(SystemModel model, String stateName){
        Pair<SystemModel, String> key = new Pair<>(model, stateName);
        return modelMap.get(key);
    }
}
