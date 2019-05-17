package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.LinearCombinationResource;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.resources.Resource;

import java.util.Set;

public class ResourceBuilder {

  private Resource _resource;

  public ResourceBuilder() {
    _resource = new Resource();
  }

  /**
   * An alternative constructor for building subclasses of the Resource class rather than a Resource
   * 
   * @param cls the class from which a resource should be built (must be a subclass of Resource)
   */
  public ResourceBuilder(Class<? extends Resource> cls) {
    try {
        _resource = cls.newInstance();
    } catch (IllegalAccessException e) {
        //TODO: handle exception
        e.printStackTrace();
    } catch (InstantiationException e) {
        //TODO: handle exception
        e.printStackTrace();
    }
  }

  public ResourceBuilder withName(String name) {
    _resource.setName(name);
    return this;
  }

  public ResourceBuilder ofType(Object type) {
    _resource.setType(type);
    return this;
  }

  public ResourceBuilder withInitialValue(Object value) {
    if (value != null) {
      _resource.setValue(value);
    }
    return this;
  }

  public ResourceBuilder forSubsystem(String subsystem) {
    _resource.setSubsystem(subsystem);
    return this;
  }

  public ResourceBuilder withUnits(String units) {
    _resource.setUnits(units);
    return this;
  }

  public ResourceBuilder withInterpolation(String interpolation) {
    _resource.setInterpolation(interpolation);
    return this;
  }

  public ResourceBuilder withAllowedValues(Set allowedValues) {
    _resource.setAllowedValues(allowedValues);
    return this;
  }

  public ResourceBuilder withMin(Object minimum) {
    _resource.setMinimum(minimum);
    return this;
  }

  public ResourceBuilder withMax(Object maximum) {
    _resource.setMaximum(maximum);
    return this;
  }

  /**
   * Adds a term to a LinearCombinationResource.
   * 
   * @param resource the input Resource whose value forms part of the linear combination
   * @param coefficient the coefficient that should be applied to the input Resource's value
   * @throws UnsupportedOperationException if the resource's type is not LinearCombinationResource
   * @return
   */
  public ResourceBuilder withTerm(Resource resource, Number coefficient) {
    if (_resource instanceof LinearCombinationResource) {
        ((LinearCombinationResource) _resource).addTerm(resource, coefficient);
    } else {
        throw new UnsupportedOperationException("'withTerm' not supported for Resources of this type");
    }
    return this;
  }

  public ResourceBuilder isFrozen(boolean frozen) {
    _resource.setFrozen(frozen);
    return this;
  }

  public Resource getResource() {
    return _resource;
  }

}
