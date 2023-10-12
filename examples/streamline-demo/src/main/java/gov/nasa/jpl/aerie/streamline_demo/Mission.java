package gov.nasa.jpl.aerie.streamline_demo;

import gov.nasa.jpl.aerie.contrib.streamline.modeling.Registrar;

public final class Mission {
  public final DataModel dataModel;
  public final ErrorTestingModel errorTestingModel;

  public Mission(final gov.nasa.jpl.aerie.merlin.framework.Registrar registrar$, final Configuration config) {
    var registrar = new Registrar(registrar$);
    dataModel = new DataModel(registrar, config);
    errorTestingModel = new ErrorTestingModel(registrar, config);
  }
}
