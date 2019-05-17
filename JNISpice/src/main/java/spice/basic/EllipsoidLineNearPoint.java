
package spice.basic;

import spice.basic.CSPICE;

/**
Class EllipsoidLineNearPoint represents the result of an
Ellipsoid-Line near point computation.

<p> Version 1.0.0 28-AUG-2009 (NJB)
*/
public class EllipsoidLineNearPoint extends Object
{
   //
   // Fields
   //
   private Ellipsoid         ellipsoid;
   private Line              line;
   private Vector3           nearPoint;
   private double            dist;

   //
   // Constructors
   //

   /**
   Construct an EllipsoidLineNearPoint from an Ellipsoid and
   a Line.
   */
   public EllipsoidLineNearPoint ( Ellipsoid   ellipsoid,
                                   Line        line      )
      throws SpiceException

   {
      double[] radii      = ellipsoid.getRadii();
      double[] pointArray = new double[3];
      double[] distArray  = new double[1];

      CSPICE.npedln( radii[0],
                     radii[1],
                     radii[2],
                     line.getPoint().toArray(),
                     line.getDirection().toArray(),
                     pointArray,
                     distArray                     );

      nearPoint     = new Vector3   ( pointArray );
      dist           = distArray[0];
      this.ellipsoid = new Ellipsoid ( ellipsoid  );
      this.line      = new Line      ( line       );
   }



   //
   // Methods
   //

   /**
   Fetch the nearest point on the Ellipsoid to the Line.
   */
   public Vector3 getNearPoint()
   {
      return (  new Vector3( nearPoint )  );
   }


   /**
   Fetch the distance between the Ellipsoid and the Line.
   */
   public double getDistance()
   {
      return ( dist );
   }
}

