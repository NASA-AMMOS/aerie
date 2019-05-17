
package spice.basic;

/**
Class CylindricalCoordinates represents sets of coordinates
expressed in the cylindrical system: positions are
specified by radius, longitude and Z coordinate.

<p> Longitude increases in the counterclockwise sense about
the +Z axis.

<p> Version 1.0.0 28-NOV-2009 (NJB)
*/
public class CylindricalCoordinates extends Coordinates
{
   //
   // Fields
   //
   private double                 radius;
   private double                 longitude;
   private double                 z;


   //
   // Constructors
   //

   /**
   No-arguments constructor.
   */
   public CylindricalCoordinates()
   {
   }


   /**
   Copy constructor.

   <p> This method creates a deep copy.
   */
   public CylindricalCoordinates ( CylindricalCoordinates coords )
   {
      this.radius     =  coords.radius;
      this.longitude  =  coords.longitude;
      this.z          =  coords.z;
   }


   /**
   Construct a CylindricalCoordinates instance from
   a radius, longitude, and z. Angular units are
   radians.
   */
   public CylindricalCoordinates ( double radius,
                                   double longitude,
                                   double z         )
      throws SpiceException
   {
      if ( radius < 0.0 )
      {
         SpiceException exc = SpiceErrorException.create(

            "CylindricalCoordinates",
            "SPICE(VALUEOUTOFRANGE)",
            "Input radius must be non-negative " +
            "but was " + radius                           );

         throw ( exc );
      }

      this.radius     = radius;
      this.longitude  = longitude;
      this.z          = z;
   }


   /**
   Construct a CylindricalCoordinates instance from a 3-vector.
   */
   public CylindricalCoordinates ( Vector3  v )

      throws SpiceException
   {
      double[] coords = CSPICE.reccyl( v.toArray() );

      radius    =  coords[0];
      longitude =  coords[1];
      z         =  coords[2];
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
   Return Z.
   */
   public double getZ()
   {
      return ( z );
   }

   /**
   Convert this instance to rectangular coordinates.
   */
   public Vector3 toRectangular()

      throws SpiceException
   {
      double[] retArray = CSPICE.cylrec ( radius, longitude, z );

      return (  new Vector3(retArray)  );
   }


   /**
   Return the Jacobian matrix of the cylindrical-to-rectangular coordinate
   transformation at the point specified by this instance.
   */
   public Matrix33 getCylRecJacobian()

      throws SpiceException
   {
      double[][] retMat = CSPICE.drdcyl ( radius, longitude, z );

      return (  new Matrix33( retMat )  );
   }


   //
   // Static methods
   //


   /**
   Return the Jacobian matrix of the rectangular-to-cylindrical coordinate
   transformation at the point specified by a 3-vector.
   */
   public static Matrix33 getRecCylJacobian ( Vector3   v )

      throws SpiceException
   {
      double[]   a      = v.toArray();

      double[][] retMat = CSPICE.dcyldr ( a[0], a[1], a[2] );

      return (  new Matrix33( retMat )  );
   }

}
