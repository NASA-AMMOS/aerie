import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;

module gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk {
  requires gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;

  exports gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk;

  uses MerlinAdaptation;
}

