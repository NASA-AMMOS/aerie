
package spice.basic;

/**
Class SphericalCoordinates represents sets of coordinates
expressed in the spherical system: positions are
specified by radius, colatitude, and longitude.

<p> Longitude increases in the counterclockwise sense about
the +Z axis. Colatitude of a point is the angular separation
between the +Z axis and the
vector from the origin to the point.

<p> Version 1.0.0 28-NOV-2009 (NJB)
*/
public class SphericalCoordinates extends Coordinates
{
   //
   // Fields
   //
   private double                 colatitude;
   private double                 longitude;
   private double                 radius;


   //
   // Constructors
   //

   /**
   No-arguments constructor.
   */
   public SphericalCoordinates()
   {
   }


   /**
   Copy constructor.

   <p> This method creates a deep copy.
   */
   public SphericalCoordinates ( SphericalCoordinates coords )
   {
      this.radius     = coords.radius;
      this.colatitude = coords.colatitude;
      this.longitude  = coords.longitude;
   }


   /**
   Construct a SphericalCoordinates instance from
   a radius, colatitude, and longitude. Angular units are
   radians.
   */
   public SphericalCoordinates ( double radius,
                                 double colatitude,
                                 double longitude   )
      throws SpiceException
   {
      if ( radius < 0.0 )
      {
         SpiceException exc = SpiceErrorException.create(

            "LatitudinalCoordinates",
            "SPICE(VALUEOUTOFRANGE)",
            "Input radius must be non-negative but was " + radius );

         throw ( exc );
      }

      this.radius     = radius;
      this.colatitude = colatitude;
      this.longitude  = longitude;
   }


   /**
   Construct a SphericalCoordinates instance from a 3-vector.
   */
   public SphericalCoordinates ( Vector3  v )

      throws SpiceException
   {
      double[] coords = CSPICE.recsph( v.toArray() );

      radius     =  coords[0];
      colatitude =  coords[1];
      longitude  =  coords[2];
   }

   //
   // Instance methods
   //

   /**
   Return radius.
   */
   public double getRadius()
   {
      return ( radius );
   }

   /**
   Return longitude in radians.
   */
   public double getLongitude()
   {
      return ( longitude );
   }
   /**
   Return colatitude in radians.
   */
   public double getColatitude()
   {
      return ( colatitude );
   }

   /**
   Convert this instance to rectangular coordinates.
   */
   public Vector3 toRectangular()

      throws SpiceException
   {
      double[] retArray = CSPICE.sphrec ( radius, colatitude, longitude );

      return (  new Vector3(retArray)  );
   }


   /**
   Return the Jacobian matrix of the spherical-to-rectangular coordinate
   transformation at the point specified by this instance.
   */
   public Matrix33 getSphRecJacobian()

      throws SpiceException
   {
      double[][] retMat = CSPICE.drdsph ( radius, colatitude, longitude );

      return (  new Matrix33( retMat )  );
   }


   //
   // Static methods
   //


   /**
   Return the Jacobian matrix of the rectangular-to-spherical coordinate
   transformation at the point specified by a 3-vector.
   */
   public static Matrix33 getRecSphJacobian ( Vector3   v )

      throws SpiceException
   {
      double[]   a      = v.toArray();

      double[][] retMat = CSPICE.dsphdr ( a[0], a[1], a[2] );

      return (  new Matrix33( retMat )  );
   }

}
