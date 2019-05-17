
package spice.basic;

/**
Class LatitudinalCoordinates represents sets of coordinates
expressed in the "latitudinal" system: positions are
specified by radius, longitude and latitude.

<p> Longitude increases in the counterclockwise sense about
the +Z axis. Latitude of a point is the angle between the
X-Y plane and the vector from the origin to the point.

<p> Version 1.0.0 28-NOV-2009 (NJB)
*/
public class LatitudinalCoordinates extends Coordinates
{
   //
   // Fields
   //
   private double                 radius;
   private double                 longitude;
   private double                 latitude;


   //
   // Constructors
   //

   /**
   No-arguments constructor.
   */
   public LatitudinalCoordinates()
   {
   }


   /**
   Copy constructor.

   <p> This method creates a deep copy.
   */
   public LatitudinalCoordinates ( LatitudinalCoordinates coords )
   {
      this.radius     =  coords.radius;
      this.longitude  =  coords.longitude;
      this.latitude   =  coords.latitude;
   }


   /**
   Construct a LatitudinalCoordinates instance from
   a radius, longitude, and latitude. Angular units are
   radians.
   */
   public LatitudinalCoordinates ( double radius,
                                   double longitude,
                                   double latitude  )
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
      this.longitude  = longitude;
      this.latitude   = latitude;
   }


   /**
   Construct a LatitudinalCoordinates instance from a 3-vector.
   */
   public LatitudinalCoordinates ( Vector3  v )

      throws SpiceException
   {
      double[] coords = CSPICE.reclat( v.toArray() );

      radius    =  coords[0];
      longitude =  coords[1];
      latitude  =  coords[2];
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
   Return latitude in radians.
   */
   public double getLatitude()
   {
      return ( latitude );
   }

   /**
   Convert this instance to rectangular coordinates.
   */
   public Vector3 toRectangular()

      throws SpiceException
   {
      double[] retArray = CSPICE.latrec ( radius, longitude, latitude );

      return (  new Vector3(retArray)  );
   }


   /**
   Return the Jacobian matrix of the latitudinal-to-rectangular coordinate
   transformation at the point specified by this instance.
   */
   public Matrix33 getLatRecJacobian()

      throws SpiceException
   {
      double[][] retMat = CSPICE.drdlat ( radius, longitude, latitude );

      return (  new Matrix33( retMat )  );
   }


   //
   // Static methods
   //


   /**
   Return the Jacobian matrix of the rectangular-to-latitudinal coordinate
   transformation at the point specified by a 3-vector.
   */
   public static Matrix33 getRecLatJacobian ( Vector3   v )

      throws SpiceException
   {
      double[]   a      = v.toArray();

      double[][] retMat = CSPICE.dlatdr ( a[0], a[1], a[2] );

      return (  new Matrix33( retMat )  );
   }

}
