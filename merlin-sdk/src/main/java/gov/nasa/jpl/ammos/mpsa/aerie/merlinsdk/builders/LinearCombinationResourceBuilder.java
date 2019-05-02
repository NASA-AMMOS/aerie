package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.LinearCombinationResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;

import com.google.common.collect.ImmutableMap;

public class LinearCombinationResourceBuilder extends ResourceBuilder {
    
    public LinearCombinationResourceBuilder(ImmutableMap<Resource, ? extends Number> terms) {
        // TODO: it feels wrong to overwrite the _resource attribute, but I don't know how else to solve this as is
        // TODO: should I auto-set type (to Double.class)?
        // TODO: should I auto-set max/min values from the component resources' min/max values?
        super();
        _resource = new LinearCombinationResource(terms);
    }
}