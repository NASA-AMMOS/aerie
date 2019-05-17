
package spice.basic;

/**
Class PlanetographicCoordinates represents sets of coordinates
expressed in the planetographic system: positions are
specified by longitude, latitude, and altitude.

<p> Planetographic coordinates are defined using a reference
spheroid. The spheroid may be oblate, prolate, or a sphere.

<p> By default, for objects other than the Earth, Moon, and Sun,
planetographic longitude is defined such that, for a distant, fixed observer,
the sub-observer point's longitude increases with time. For the
Earth, Moon and Sun, longitude is positive East by default.
The default sense of planetographic longitude for a given
body can be overridden via kernel pool assignments; see
Particulars for details.

<p>Planetographic latitude of a point is the angle between the
X-Y plane and the line normal to the reference spheroid that
passes through the point and the closest point on the reference
spheroid to the point.

<p> Version 1.0.0 28-NOV-2009 (NJB)
*/
public class PlanetographicCoordinates extends Coordinates
{
   //
   // Fields
   //
   private Body                   body;
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
   public PlanetographicCoordinates()
   {
      body       = null;
   }


   /**
   Copy constructor.

   <p> This method creates a deep copy.
   */
   public PlanetographicCoordinates ( PlanetographicCoordinates coords )

      throws SpiceException
   {
      this.body       =  new Body( coords.body );
      this.re         =  coords.re;
      this.f          =  coords.f;
      this.longitude  =  coords.longitude;
      this.latitude   =  coords.latitude;
      this.altitude   =  coords.altitude;
   }


   /**
   Construct a PlanetographicCoordinates instance from
   a Body, an equatorial radius, flattening coefficient,
   altitude, longitude, and latitude. Angular units are
   radians.
   */
   public PlanetographicCoordinates ( Body   body,
                                      double longitude,
                                      double latitude,
                                      double altitude,
                                      double re,
                                      double f         )
      throws SpiceException
   {
      if ( re < 0.0 )
      {
         SpiceException exc = SpiceErrorException.create(

            "PlanetographicCoordinates",
            "SPICE(VALUEOUTOFRANGE)",
            "Input equatorial radius must be non-negative " +
            "but was " + re                                   );


         throw ( exc );
      }

      this.body       =  new Body(body);
      this.re         =  re;
      this.f          =  f;
      this.longitude  =  longitude;
      this.latitude   =  latitude;
      this.altitude   =  altitude;
   }


   /**
   Construct a PlanetographicCoordinates instance from a Body,
   a 3-vector, and reference spheroid parameters.
   */
   public PlanetographicCoordinates ( Body     body,
                                      Vector3  v,
                                      double   re,
                                      double   f    )
      throws SpiceException
   {
      double[] coords = CSPICE.recpgr( body.getName(), v.toArray(), re, f );

      this.body =  new Body( body );
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
   Return central body with with this coordinate instance is
   associated.
   */
   public Body getBody()

      throws SpiceException
   {
      return ( new Body(this.body) );
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
      double[] retArray = CSPICE.pgrrec ( body.getName(),
                                          longitude,
                                          latitude,
                                          altitude,
                                          re,
                                          f         );

      return (  new Vector3(retArray)  );
   }


   /**
   Return the Jacobian matrix of the planetographic-to-rectangular coordinate
   transformation at the point specified by this instance.
   */
   public Matrix33 getPgrRecJacobian()

      throws SpiceException
   {
      double[][] retMat = CSPICE.drdpgr ( body.getName(),
                                          longitude,
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
   Return the Jacobian matrix of the rectangular-to-planetographic coordinate
   transformation at the point specified by a Body, a 3-vector and reference
   spheroid parameters.
   */
   public static Matrix33 getRecPgrJacobian ( Body      body,
                                              Vector3   v,
                                              double    re,
                                              double    f    )
      throws SpiceException
   {
      double[]   a      = v.toArray();

      double[][] retMat = CSPICE.dpgrdr ( body.getName(), a[0], a[1], a[2],
                                                                      re, f );

      return (  new Matrix33( retMat )  );
   }

}
