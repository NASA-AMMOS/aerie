
package spice.basic;

/**
Class OsculatingElements provides methods for conversion between
state vectors and osculating elements. The class also supports
two-body propagation of osculating elements or state vectors.

<h3> Version 2.0.0 25-JAN-2017 (NJB)</h3>

<p>Added no-arguments constructor.

<p>The protection of the field `eltArray' and of the index
constants has been changed from "private" to "protected," in order 
to support derived classes.

<h3> Version 1.0.0 03-DEC-2009 (NJB)</h3>
*/
public class OsculatingElements extends Object
{
   //
   // Private constants
   //
   protected static final int        NELTS     =  8;

   protected static final int        IDX_RP    =  0;
   protected static final int        IDX_ECC   =  1;
   protected static final int        IDX_INC   =  2;
   protected static final int        IDX_LNODE =  3;
   protected static final int        IDX_ARGP  =  4;
   protected static final int        IDX_M0    =  5;
   protected static final int        IDX_T0    =  6;
   protected static final int        IDX_MU    =  7;


   //
   // Fields
   //
   protected double[]              eltArray;


   //
   // Constructors
   //

   /**
   No-arguments constructor.

   <p>This constructor creates the element array field.
   */
   OsculatingElements()
   {
      eltArray = new double[NELTS];
   }


   /**
   Array constructor.

   <p>Set the elements of this instance to the values
   contained in the input array.

   <p> The array elements are, in order:
   <pre>
       RP      Perifocal distance.
       ECC     Eccentricity.
       INC     Inclination.
       LNODE   Longitude of the ascending node.
       ARGP    Argument of periapsis.
       M0      Mean anomaly at epoch.
       T0      Epoch.
       MU      Gravitational parameter.
   </pre>

   <p> Distance units are km. Angular units are radians.

   */
   public OsculatingElements( double[] eltArray )

      throws SpiceException
   {
      if ( eltArray.length != NELTS )
      {
         SpiceException exc = SpiceErrorException.create(

            "OsculatingElements",
            "SPICE(INVALIDARRAYSIZE)",
            "Size must be " + NELTS + " but was " + eltArray.length );

         throw ( exc );
      }

      this.eltArray = new double[NELTS];

      System.arraycopy( eltArray, 0, this.eltArray, 0, NELTS );
   }


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public OsculatingElements( OsculatingElements elts )
   {
      eltArray = new double[NELTS];

      System.arraycopy( elts.eltArray, 0, this.eltArray, 0, NELTS );
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
   public OsculatingElements( StateVector       state,
                              Time              t,
                              double            mu     )
      throws SpiceException
   {
      eltArray = CSPICE.oscelt(  state.toArray(),
                                 t.getTDBSeconds(),
                                 mu                 );
   }



   //
   // Methods
   //

   /**
   Obtain the elements of this instance in an array.

   <p> The elements are, in order:
   <pre>
       RP      Perifocal distance.
       ECC     Eccentricity.
       INC     Inclination.
       LNODE   Longitude of the ascending node.
       ARGP    Argument of periapsis.
       M0      Mean anomaly at epoch.
       T0      Epoch.
       MU      Gravitational parameter.
   </pre>

   <p> Distance units are km. Angular units are radians.

   */
   public double[] toArray()
   {
      double[] retArray = new double[NELTS];

      System.arraycopy( eltArray, 0, retArray, 0, NELTS );

      return( retArray );
   }

   /**
   Return the perifocal distance. Units are km.
   */
   public double getPerifocalDistance()
   {
      return( eltArray[IDX_RP] );
   }

   /**
   Return the eccentricity.
   */
   public double getEccentricity()
   {
      return( eltArray[IDX_ECC] );
   }

   /**
   Return the inclination. Units are radians.
   */
   public double getInclination()
   {
      return( eltArray[IDX_INC] );
   }

   /**
   Return the longitude of the ascending node. Units are radians.
   */
   public double getLongitudeOfNode()
   {
      return( eltArray[IDX_LNODE] );
   }

   /**
   Return the argument of periapsis. Units are radians.
   */
   public double getArgumentOfPeriapsis()
   {
      return( eltArray[IDX_ARGP] );
   }


   /**
   Return the mean anomaly at epoch. Units are radians.
   */
   public double getMeanAnomaly()
   {
      return( eltArray[IDX_M0] );
   }


   /**
   Return the epoch of this instance.
   */
   public TDBTime getEpoch()
   {
      return(   new TDBTime( eltArray[IDX_T0] )   );
   }


   /**
   Return the gravitational parameter of this instance.

   <p> The GM value `mu' is expressed in units of
   <pre>
         3      2
       km  / sec
   </pre>

   */
   public double getGM()
   {
      return( eltArray[IDX_MU] );
   }



   /**
   Propagate these elements to a state at a given epoch.
   */
   public StateVector propagate( Time t )

      throws SpiceException
   {
      double[] stateArray = CSPICE.conics( eltArray, t.getTDBSeconds() );

      return(   new StateVector( new Vector6(stateArray) )   );
   }


   //
   // Static methods
   //

   /**
   Propagate an initial state vector and gravitational parameter
   to another state vector at a given time offset.
   */
   public static StateVector propagate( StateVector   initialState,
                                        double        mu,
                                        TDBDuration   dt            )
      throws SpiceException
   {
      double[]  pvprop = new double[6];

      CSPICE.prop2b( mu,
                     initialState.toArray(),
                     dt.getMeasure(),
                     pvprop                 );

      return(   new StateVector( new Vector6(pvprop) )   );
   }

}
