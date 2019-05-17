
package spice.basic;

import spice.basic.CSPICE;

/**
Class EllipsoidPointNearPoint represents the result of an
Ellipsoid-Point near point computation. This computation
finds the nearest point on a given ellipsoid to a 
specified point. The input point may be inside the 
ellipsoid.

<p> An EllipsoidPointNearPoint instance consists of

<ol>

<li> An inherited array of 3 doubles representing the near
point on the ellipsoid. </li>
<li> A double representing the altitude of the input
point relative to the ellipsoid</li>

</ol>

<p> This is a low-level, "pure geometric" class having
functionality analogous to that provided
by the CSPICE routine nearpt_c. SPICE application developers
should consider using the high-level class
{@link spice.basic.SubObserverRecord} instead.



<h3> Version 2.0.0 23-JAN-2017 (NJB) </h3>

   This class now is derived from Vector3.

<h3> Version 1.0.0 28-NOV-2009 (NJB) </h3>

*/
public class EllipsoidPointNearPoint extends Vector3
{
   //
   // Fields
   //
   private double            dist;

   //
   // Constructors
   //

   /**
   Construct an EllipsoidPointNearPoint from an Ellipsoid and
   a specified point.
   */
   public EllipsoidPointNearPoint ( Ellipsoid   ellipsoid,
                                    Vector3     point     )
      throws SpiceException

   {
      super();

      double[] radii      = ellipsoid.getRadii();
      double[] altArray   = new double[1];

      CSPICE.nearpt( point.toArray(),
                     radii[0],
                     radii[1],
                     radii[2],
                     v,
                     altArray        );

      dist = altArray[0];
   }



   //
   // Methods
   //

   /**
   Fetch the nearest point on the Ellipsoid to the point.
   */
   public Vector3 getNearPoint()
   {
      return (  new Vector3( this )  );
   }


   /**
   Fetch the distance between the Ellipsoid and the point.
   */
   public double getDistance()
   {
      return ( dist );
   }
}

