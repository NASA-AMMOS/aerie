
package spice.basic;

/**
Class RADecCoordinates represents sets of coordinates
expressed in the right ascension/declination system: positions are
specified by radius, right ascension, and declination.

<p> Right ascension increases in the counterclockwise sense about
the +Z axis. The range of right ascension is 0-360 degrees.

<p> Declination of a point is the angle between the
X-Y plane and the vector from the origin to the point.

<p> Version 1.0.0 18-DEC-2009 (NJB)
*/
public class RADecCoordinates extends Coordinates
{
   //
   // Fields
   //
   private double                 radius;
   private double                 rightAscension;
   private double                 declination;


   //
   // Constructors
   //

   /**
   No-arguments constructor.
   */
   public RADecCoordinates()
   {
   }


   /**
   Copy constructor.

   <p> This method creates a deep copy.
   */
   public RADecCoordinates ( RADecCoordinates coords )
   {
      this.radius         = coords.radius;
      this.rightAscension = coords.rightAscension;
      this.declination    = coords.declination;
   }


   /**
   Construct a RADecCoordinates instance from
   a radius, RA, and declination. Angular units are
   radians.
   */
   public RADecCoordinates ( double radius,
                             double rightAscension,
                             double declination    )
      throws SpiceException
   {
      if ( radius < 0.0 )
      {
         SpiceException exc = SpiceErrorException.create(

            "RADecCoordinates",
            "SPICE(VALUEOUTOFRANGE)",
            "Input radius must be non-negative but was " + radius );

         throw ( exc );
      }

      this.radius         = radius;
      this.rightAscension = rightAscension;
      this.declination    = declination;
   }


   /**
   Construct a RADecCoordinates instance from a 3-vector.
   */
   public RADecCoordinates ( Vector3  v )

      throws SpiceException
   {
      double[] coords = CSPICE.recrad( v.toArray() );

      radius         =  coords[0];
      rightAscension =  coords[1];
      declination    =  coords[2];
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
   Return RA in radians.
   */
   public double getRightAscension()
   {
      return ( rightAscension );
   }
   /**
   Return declination in radians.
   */
   public double getDeclination()
   {
      return ( declination );
   }

   /**
   Convert this instance to rectangular coordinates.
   */
   public Vector3 toRectangular()

      throws SpiceException
   {
      double[] retArray = CSPICE.radrec ( radius, rightAscension, declination );

      return (  new Vector3(retArray)  );
   }


   /**
   Return the Jacobian matrix of the RA/Dec-to-rectangular coordinate
   transformation at the point specified by this instance.
   */
   public Matrix33 getRADRecJacobian()

      throws SpiceException
   {
      //
      // The Jacobian is identical to that for the latitudinal system.
      //
      double[][] retMat = CSPICE.drdlat ( radius, rightAscension, declination );

      return (  new Matrix33( retMat )  );
   }


   //
   // Static methods
   //


   /**
   Return the Jacobian matrix of the rectangular-to-RA/Dec coordinate
   transformation at the point specified by a 3-vector.
   */
   public static Matrix33 getRecRADJacobian ( Vector3   v )

      throws SpiceException
   {
      double[] a = v.toArray();

      //
      // The Jacobian is identical to that for the latitudinal system.
      //
      double[][] retMat = CSPICE.dlatdr ( a[0], a[1], a[2] );

      return (  new Matrix33( retMat )  );
   }

}
