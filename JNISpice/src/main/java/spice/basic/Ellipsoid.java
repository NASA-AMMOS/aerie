
package spice.basic;

/**
Class Ellipsoid represents tri-axial ellipsoids in three-dimensional,
Euclidean space.

<p> JNISpice Ellipsoids are centered at the origin and have semi-axes
aligned with the x, y, and z coordinate axes.
JNISpice Ellipsoids are required to have positive semi-axis lengths.

<p> To find the closest point on an Ellipsoid to a given point,
see class {@link spice.basic.EllipsoidPointNearPoint}.

<p> To find the closest point on an Ellipsoid to a given line,
see class {@link spice.basic.EllipsoidLineNearPoint}.

<p> To find the intercept on an Ellipsoid of a given Ray,
see class {@link spice.basic.RayEllipsoidIntercept}.


<p> Version 1.0.0 28-NOV-2009 (NJB)
*/
public class Ellipsoid extends Object
{


   //
   // Fields
   //
   private double[]            radii;


   //
   // Constructors
   //

   /**
   No-arguments constructor. This constructor generates a unit sphere.
   */
   public Ellipsoid()
   {
      radii    = new double[3];

      radii[0] = 1.0;
      radii[1] = 1.0;
      radii[2] = 1.0;
   }


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public Ellipsoid( Ellipsoid ell )
   {
      radii = new double[3];

      for ( int i = 0;  i < 3;  i++ )
      {
         this.radii[i] = ell.radii[i];
      }
   }


   /**
   Construct an ellipsoid from three semi-axis lengths.
   */
   public Ellipsoid( double         a,
                     double         b,
                     double         c  )

      throws SpiceException

   {
      String     msg;

      radii    = new double[3];

      radii[0] = a;
      radii[1] = b;
      radii[2] = c;


      for ( int i = 0;  i < 3;  i++ )
      {
         if ( radii[i] <= 0.0 )
         {
            SpiceException exc = SpiceErrorException.create(

               "Ellipsoid",
               "SPICE(VALUEOUTOFRANGE)",

               "Ellipsoid radii must be positive, but " +
               "radius " + i + " is " + radii[i]            );

            throw exc;
         }
      }
   }


   //
   // Methods
   //

   /**
   Get radii of this Ellipsoid.
   */
   public double[] getRadii()
   {
      double[]   retArray = new double[3];

      System.arraycopy ( this.radii, 0, retArray, 0, 3 );

      return ( retArray );
   }


   /**
   Find the unit outward surface normal at a specified point
   on this Ellipsoid's surface.
   */
   public Vector3 getNormal( Vector3 point )

      throws SpiceException
   {
      double[] normal = CSPICE.surfnm( radii[0],
                                       radii[1],
                                       radii[2],
                                       point.toArray() );

      return (  new Vector3( normal )  );
   }






   /**
   Find the limb of this Ellipsoid, as seen from a given viewing
   location.
   */
   public Ellipse getLimb( Vector3  viewpt )

      throws SpiceException

   {
      double[] limbArray = CSPICE.edlimb( radii[0],
                                          radii[1],
                                          radii[2],
                                          viewpt.toArray() );

      return ( new Ellipse( limbArray )  );
   }




   /**
   Display an Ellipsoid as a string; override Object's toString() method.
   */
   public String toString()
   {
      String endl = System.getProperty( "line.separator" );

      String str  = "Ellipsoid Radii:" + endl + ( new Vector3(radii) );

      return ( str );
   }



}
