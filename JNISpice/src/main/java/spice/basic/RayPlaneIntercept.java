
package spice.basic;

import spice.basic.CSPICE;

/**
Class RayPlaneIntercept represents the result of a
ray-plane intercept computation.

<p> Version 1.0.0 08-DEC-2009 (NJB)
*/
public class RayPlaneIntercept extends Object
{
   //
   // Public constants
   //
   public final static int       INFINITY         =   -1;

   //
   // Fields
   //
   private int               nxpts;
   private Vector3           xpt;


   //
   // Constructors
   //

   /**
   Construct a ray-plane intercept from a Ray and a Plane.
   */
   public RayPlaneIntercept ( Ray        ray,
                              Plane      plane   )
      throws SpiceException
   {
      double[]       planeArray = plane.toArray();
      double[]       xptArray   = new double[3];

      int[]          nxptsArray = new int[1];

      CSPICE.inrypl ( ray.getVertex().toArray(),
                      ray.getDirection().toArray(),
                      plane.toArray(),
                      nxptsArray,
                      xptArray           );

      nxpts = nxptsArray[0];

      if ( nxpts != 0 )
      {
         xpt = new Vector3( xptArray );
      }
   }


   //
   // Methods
   //

   /**
   Fetch the intercept count.
   */
   public int getInterceptCount()
   {
      return nxpts;
   }

   /**
   Fetch the intercept. This method should be called only if
   the intercept count is non-zero.
   */
   public Vector3 getIntercept()

      throws PointNotFoundException
   {
      if ( nxpts == 0 )
      {
         PointNotFoundException exc;

         exc = PointNotFoundException.create(

            "RayPlaneIntercept.getIntercept",
            "Ray-plane intercept does not exist." );

         throw ( exc );
      }

      return (  new Vector3( this.xpt )  );
   }
}

