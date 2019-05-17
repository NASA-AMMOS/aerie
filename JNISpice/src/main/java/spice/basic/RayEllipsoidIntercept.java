
package spice.basic;

import spice.basic.CSPICE;

/**
Class RayEllipsoidIntercept represents the result of a
Ray-Ellipsoid intercept computation.

<p> This is a low-level, "pure geometric" class having
functionality analogous to that provided
by the CSPICE routine surfpt_c. SPICE application developers
should consider using the high-level class
{@link spice.basic.SurfaceIntercept} instead.


<p> Version 1.0.0 03-DEC-2009 (NJB)
*/
public class RayEllipsoidIntercept extends Object
{
   //
   // Fields
   //
   private boolean           found     = false;
   private Vector3           xpt       = null;
   private Ray               ray       = null;
   private Ellipsoid         ellipsoid = null;


   //
   // Constructors
   //

   /**
   Construct a ray-ellipsoid intercept from a Ray and
   an Ellipsoid.
   */
   public RayEllipsoidIntercept ( Ray       ray,
                                  Ellipsoid ellipsoid )
      throws SpiceException
   {
      this.ray             = ray;
      this.ellipsoid       = ellipsoid;

      boolean[] foundArray = new boolean[1];
      double[]  radii      = ellipsoid.getRadii();
      double[]  xptArray   = new double[3];

      CSPICE.surfpt ( ray.getVertex().toArray(),
                      ray.getDirection().toArray(),
                      radii[0],
                      radii[1],
                      radii[2],
                      xptArray,
                      foundArray                    );

      found = foundArray[0];

      if ( found )
      {
         xpt = new Vector3( xptArray );
      }
   }


   //
   // Methods
   //

   /**
   Fetch the found flag.
   */
   public boolean wasFound()
   {
      return ( found );
   }

   /**
   Fetch the intercept. This method should be called only if
   the intercept was found, as indicated by the method wasFound().
   */
   public Vector3 getIntercept()

      throws PointNotFoundException
   {
      if ( !found )
      {
         String endl   = System.getProperty( "line.separator" );

         PointNotFoundException exc = PointNotFoundException.create(

            "getIntercept",
            "Ray-ellipsoid intercept does not exist." + endl +
            "Ray       = " + endl +
            ray            + endl + endl +
            "Ellipsoid = " + endl +
            ellipsoid      + endl                     );

         throw ( exc );
      }

      return (  new Vector3( this.xpt )  );
   }
}

