import gov.nasa.jpl.ammos.mpsa.aerie.banananation.Banananation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;

module gov.nasa.jpl.ammos.mpsa.aerie.banananation {
  requires gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk;
  requires org.apache.commons.lang3;
  requires JNISpice;

  provides MerlinAdaptation with Banananation;

  uses MerlinAdaptation;
}
