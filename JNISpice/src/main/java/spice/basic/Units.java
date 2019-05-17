
package spice.basic;

import spice.basic.CSPICE;

/**
Class Units represents physical units and supports conversions
between them.

<p> Version 1.0.0 12-JUL-2009 (NJB)
*/
public abstract class Units
{

   /**
   Return a String representation of a Unit.
   */
   public abstract String toString();



   /**
   Convert a quantity from one set of units to another.

   <p> The full set of units supported by the underlying
   conversion algorithm is shown below. However, support for many of
   these units is not yet implemented in the Alpha version of
   JNISpice.

   <pre>

              Angles:                 "RADIANS"
                                      "DEGREES"
                                      "ARCMINUTES"
                                      "ARCSECONDS"
                                      "HOURANGLE"
                                      "MINUTEANGLE"
                                      "SECONDANGLE"

              Metric Distances:       "METERS"
                                      "KM"
                                      "CM"
                                      "MM"

              English Distances:      "FEET"
                                      "INCHES"
                                      "YARDS"
                                      "STATUTE_MILES"
                                      "NAUTICAL_MILES"

              Astrometric Distances:  "AU"
                                      "PARSECS"
                                      "LIGHTSECS"
                                      "LIGHTYEARS" julian lightyears

              Time:                   "SECONDS"
                                      "MINUTES"
                                      "HOURS"
                                      "DAYS"
                                      "JULIAN_YEARS"
                                      "TROPICAL_YEARS"
                                      "YEARS" (same as julian years)
   </pre>

   */
   public static double convert ( double quantity,
                                  Units  fromUnits,
                                  Units  toUnits   )

      throws SpiceException
   {
      double result = CSPICE.convrt ( quantity,
                                      fromUnits.toString(),
                                      toUnits.toString()    );

      return ( result );
   }
}
