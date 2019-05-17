



package spice.basic;

/**
Class GFSearch is the abstract superclass for GF numeric
quantity searches.

<h3> Version 2.0.0 29-NOV-2016 (NJB)</h3>

This class is now derived from class {@link GF}.

<h3> Version 1.0.0 17-AUG-2009 (NJB)</h3>
*/
public abstract class GFNumericSearch extends GF
{

   /**
   Run a search over a specified confinement window, using
   a specified step size (units are TDB seconds).

   <p> The input `maxWorkspaceIntervals' is an upper bound on the number of
   intervals in each workspace window.
   */
   public abstract SpiceWindow run ( SpiceWindow   confinementWindow,
                                     GFConstraint  constraint,
                                     double        step,
                                     int           maxWorkspaceIntervals      )
      throws SpiceException;



}
