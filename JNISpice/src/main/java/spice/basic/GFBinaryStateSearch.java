



package spice.basic;

/**
Class GFBinaryStateSearch is the abstract superclass for GF binary
state searches.

<h3> Version 2.0.0 29-NOV-2016 (NJB)</h3>

This class is now derived from class {@link GF}.

<h3> Version 1.0.0 19-JUL-2009 (NJB)</h3>
*/
public abstract class GFBinaryStateSearch extends GF
{

   /**
   Run a search over a specified confinement window, using
   a specified step size (units are TDB seconds).

   <p> The input `maxIntervals' is an upper bound on the number of
   intervals in the output result window.
   */
   public abstract SpiceWindow run ( SpiceWindow   confinementWindow,
                                     double        step,
                                     int           maxResultIntervals )
      throws SpiceException;



}
