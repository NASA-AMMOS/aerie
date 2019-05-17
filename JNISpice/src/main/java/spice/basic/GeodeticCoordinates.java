
package spice.basic;

/**
Class GeodeticCoordinates represents sets of coordinates
expressed in the "geodetic" system: positions are
specified by longitude, latitude, and altitude.

<p> "Geodetic" is the term used in SPICE for the
"planetodetic" coordinate system.

<p> Geodetic coordinates are defined using a reference
spheroid. The spheroid may be oblate, prolate, or a sphere.

<p> Longitude increases in the counterclockwise sense about
the +Z axis. Latitude of a point is the angle between the
X-Y plane and the line normal to the reference spheroid that
passes through the point and the closest point on the reference
spheroid to the point.

<p> Version 1.0.0 28-NOV-2009 (NJB)
*/
public class GeodeticCoordinates extends Coordinates
{
   //
   // Fields
   //
   private double                 re;
   private double                 altitude;
   private double                 f;
   private double                 latitude;
   private double                 longitude;


   //
   // Constructors
   //

   /**
   No-arguments constructor.
   */
   public GeodeticCoordinates()
   {
   }


   /**
   Copy constructor.

   <p> This method creates a deep copy.
   */
   public GeodeticCoordinates ( GeodeticCoordinates coords )
   {
      this.re         =  coords.re;
      this.f          =  coords.f;
      this.longitude  =  coords.longitude;
      this.latitude   =  coords.latitude;
      this.altitude   =  coords.altitude;
   }


   /**
   Construct a GeodeticCoordinates instance from
   an equatorial radius, flattening coefficient,
   altitude, longitude, and latitude. Angular units are
   radians.
   */
   public GeodeticCoordinates ( double longitude,
                                double latitude,
                                double altitude,
                                double re,
                                double f         )
      throws SpiceException
   {
      if ( re < 0.0 )
      {
         SpiceException exc = SpiceErrorException.create(

            "GeodeticCoordinates",
            "SPICE(VALUEOUTOFRANGE)",
            "Input equatorial radius must be non-negative " +
            "but was " + re                                   );


         throw ( exc );
      }

      this.re         =  re;
      this.f          =  f;
      this.longitude  =  longitude;
      this.latitude   =  latitude;
      this.altitude   =  altitude;
   }


   /**
   Construct a GeodeticCoordinates instance from a 3-vector
   and reference spheroid parameters.
   */
   public GeodeticCoordinates ( Vector3  v,
                                double   re,
                                double   f   )
      throws SpiceException
   {
      double[] coords = CSPICE.recgeo( v.toArray(), re, f );

      this.re   =  re;
      this.f    =  f;
      longitude =  coords[0];
      latitude  =  coords[1];
      altitude  =  coords[2];
   }

   //
   // Instance methods
   //


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
   Return altitude.
   */
   public double getAltitude()
   {
      return ( altitude );
   }

   /**
   Return the equatorial radius of the reference spheroid.
   */
   public double getEquatorialRadius()
   {
      return ( re );
   }

   /**
   Return the flattening coefficient of the reference spheroid.
   */
   public double getFlatteningCoefficient()
   {
      return ( f );
   }


   /**
   Convert this instance to rectangular coordinates.
   */
   public Vector3 toRectangular()

      throws SpiceException
   {
      double[] retArray = CSPICE.georec ( longitude,
                                          latitude,
                                          altitude,
                                          re,
                                          f         );

      return (  new Vector3(retArray)  );
   }


   /**
   Return the Jacobian matrix of the geodetic-to-rectangular coordinate
   transformation at the point specified by this instance.
   */
   public Matrix33 getGeoRecJacobian()

      throws SpiceException
   {
      double[][] retMat = CSPICE.drdgeo ( longitude,
                                          latitude,
                                          altitude,
                                          re,
                                          f         );

      return (  new Matrix33( retMat )  );
   }


   //
   // Static methods
   //


   /**
   Return the Jacobian matrix of the rectangular-to-geodetic coordinate
   transformation at the point specified by a 3-vector and reference
   spheroid parameters.
   */
   public static Matrix33 getRecGeoJacobian ( Vector3   v,
                                              double    re,
                                              double    f    )
      throws SpiceException
   {
      double[]   a      = v.toArray();

      double[][] retMat = CSPICE.dgeodr ( a[0], a[1], a[2], re, f );

      return (  new Matrix33( retMat )  );
   }

}
