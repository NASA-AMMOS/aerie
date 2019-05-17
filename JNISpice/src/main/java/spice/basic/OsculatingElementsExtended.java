
package spice.basic;

/**
Class OsculatingElementsExtended provides methods for conversion between
state vectors and osculating elements. The class also supports
two-body propagation of osculating elements or state vectors.

<p>This class provides the true anomaly and, when these values are computable,
the orbital semi-major axis and period.

<p> The full set of elements is:
   <pre>
   RP      Perifocal distance.
   ECC     Eccentricity.
   INC     Inclination.
   LNODE   Longitude of the ascending node.
   ARGP    Argument of periapsis.
   M0      Mean anomaly at epoch.
   T0      Epoch.
   MU      Gravitational parameter.
   NU      True anomaly.
   A       Semi-major axis.
   TAU     Orbital period.

Distance units are km. Angular units are radians. Time units
are seconds.

</pre>

<p>Methods for retrieving the first eight elements shown above
are inherited from the superclass {@link OsculatingElements}.

<h3> Version 1.0.0 25-JAN-2017 (NJB)</h3>
*/
public class OsculatingElementsExtended extends OsculatingElements
{
   //
   // Private constants
   //
   private static final int        NXELTS    =  11;
   private static final int        IDX_NU    =  8;   
   private static final int        IDX_A     =  9;
   private static final int        IDX_TAU   =  10;

   //
   // Fields
   //
   private double                  nu;
   private double                  a;
   private double                  tau;

   //
   // Constructors
   //

   //
   // Note that there is no public array constructor: the existence of 
   // such a constructor would prevent further extension of the
   // element set.
   // 




   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public OsculatingElementsExtended( OsculatingElementsExtended elts )
   {
      eltArray = new double[NELTS];

      System.arraycopy( elts.eltArray, 0, this.eltArray, 0, NELTS );

      this.nu  = elts.nu;
      this.a   = elts.a;
      this.tau = elts.tau;
   }


   /**
   Create osculating elements from a StateVector, a Time, and a
   gravitational parameter (GM).

   <p> The GM value `mu' is expressed in units of
   <pre>
         3      2
       km  / sec
   </pre>

   */
   public OsculatingElementsExtended( StateVector       state,
                                      Time              t,
                                      double            mu     )
      throws SpiceException
   {
      double[]               eltArrayExt;

      eltArrayExt = CSPICE.oscltx(  state.toArray(),
                                    t.getTDBSeconds(),
                                    mu                 );

      System.arraycopy( eltArrayExt, 0, this.eltArray, 0, NELTS );

      this.nu  = eltArrayExt[IDX_NU ];
      this.a   = eltArrayExt[IDX_A  ];  
      this.tau = eltArrayExt[IDX_TAU];
   }



   //
   // Methods
   //

   /**
   Extract elements to an array of type double. 

   <p> The full set of elements is:
   <pre>
   RP      Perifocal distance.
   ECC     Eccentricity.
   INC     Inclination.
   LNODE   Longitude of the ascending node.
   ARGP    Argument of periapsis.
   M0      Mean anomaly at epoch.
   T0      Epoch.
   MU      Gravitational parameter.
   NU      True anomaly.
   A       Semi-major axis.
   TAU     Orbital period.

Distance units are km. Angular units are radians. Time units
are seconds.

</pre>


   */
   public double[] toArray()
   {
      double[] retArray = new double[NXELTS];

      System.arraycopy( eltArray, 0, retArray, 0, NELTS );

      retArray[IDX_NU ] = this.nu;
      retArray[IDX_A  ] = this.a;
      retArray[IDX_TAU] = this.tau;


      return( retArray );
   }

 
   /**
   Return true anomaly.

   <p>Units are radians.
   */
   public double getTrueAnomaly()

      throws SpiceException
   {
      return ( this.nu );
   }
      

   /**
   Return the orbital semi-major axis.

   <p>Units are km.

   <p>This method throws an exception if the semi-major axis 
   cannot be computed.
   */
   public double getSemiMajorAxis()

      throws SpiceException
   {
      SpiceErrorException               exc;
      String                            longMsg;


      if ( this.a == 0.0 )
      {
         //
         // The value 0.0 is reserved: it indicates the semi-major 
         // axis was not computable.
         //
         longMsg = String.format ( 

                     "The semi-major axis corresponding to this set of " +
                     "elements was not computable. The eccentricity is " +
                     "%e24.17",
                     eltArray[IDX_ECC]

                                 );
                      

         exc = SpiceErrorException.create( "getSemiMajorAxis", 
                                           "SPICE(NOTCOMPUTABLE)",
                                           longMsg                 );
         throw( exc );                  
      }

      
      return ( this.a );
   }


   /**
   Return the orbital period.

   <p>Units are seconds.

   <p>This method throws an exception if the period 
   cannot be computed.
   */
   public double getPeriod()

      throws SpiceException
   {
      SpiceErrorException               exc;
      String                            longMsg;


      if ( this.tau == 0.0 )
      {
         //
         // The value 0.0 is reserved: it indicates the period
         // was not computable.
         //
         longMsg = String.format ( 

                     "The orbital period corresponding to this set of " +
                     "elements was not computable. The eccentricity is " +
                     "%e24.17",
                     eltArray[IDX_ECC]

                                 );
                      

         exc = SpiceErrorException.create( "getPeriod", 
                                           "SPICE(NOTCOMPUTABLE)",
                                           longMsg                 );
         throw( exc );                  
      }

      
      return ( this.tau );
   }


}
