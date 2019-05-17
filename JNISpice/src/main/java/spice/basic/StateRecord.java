
package spice.basic;

import spice.basic.CSPICE;

/**
Class StateRecord represents the states (positions and velocities)
of ephemeris objects relative to other objects; state records carry
along with them one-way light time between target and observer.

<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.


<p>1)  Find the apparent state of the Moon as seen from the Earth,
expressed in the J2000 reference frame, at the observation epoch
2009 JUL 09 00:00:00 TDB. Use light time and stellar aberration
corrections.

<p>      Use the meta-kernel shown below to load the required SPICE
      kernels.

<pre>
   KPL/MK

         This meta-kernel is intended to support operation of SPICE
         example programs. The kernels shown here should not be
         assumed to contain adequate or correct versions of data
         required by SPICE-based user applications.

         In order for an application to use this meta-kernel, the
         kernels referenced here must be present in the user's
         current working directory.

         The names and contents of the kernels referenced
         by this meta-kernel are as follows:

            File name                     Contents
            ---------                     --------
            de421.bsp                     Planetary ephemeris
            pck00008.tpc                  Planet orientation and
                                          radii
            naif0009.tls                  Leapseconds


   \begindata

   KERNELS_TO_LOAD = ( '/kernels/gen/lsk/naif0009.tls'
                       '/kernels/gen/spk/de421.bsp'
                       '/kernels/gen/pck/pck00008.tpc'
                     )

</pre>

<p> Example code begins here.

<pre>
   import spice.basic.*;

   class StateRecordEx1
   {
      //
      // Load the JNISpice shared object library
      // at initialization time.
      //
      static { System.loadLibrary( "JNISpice" ); }

      public static void main ( String[] args )
      {
         try
         {
            //
            // Load kernels.
            //
            KernelDatabase.load ( "standard.tm" );

            //
            // Create input objects required to specify the
            // StateRecord.
            //
            String timstr               = "2009 JUL 09 00:00:00 TDB";
            Body target                 = new Body ( "Moon" );
            TDBTime et                  = new TDBTime ( timstr );
            ReferenceFrame ref          = new ReferenceFrame( "J2000" );
            AberrationCorrection abcorr = new AberrationCorrection( "LT+S" );
            Body observer               = new Body ( 399    );

            //
            // Create a StateRecord and display it.
            //
            StateRecord s = new StateRecord ( target,  et,  ref,
                                              abcorr,  observer );
            System.out.println ( s );

         }
         catch ( SpiceException exc )
         {
            exc.printStackTrace();
         }
      }
   }
</pre>

<p> When run on a PC/Linux/java 1.6.0_14/gcc platform, the output
   from this program was:

<pre>

State vector =

    X:   2.283960670189394e+05 (km)
    Y:  -3.088436590471006e+05 (km)
    Z:  -1.308849289940148e+05 (km)
   VX:   7.946652340856368e-01 (km/s)
   VY:   4.715096134721694e-01 (km/s)
   VZ:   3.015826664840431e-01 (km/s)

Distance           =   4.058078779436026e+05  (km)
Speed              =   9.719908716570748e-01  (km/s)
One way light time =   1.353629376305400e+00  (s)

</pre>



<h3> Version 2.0.0 25-JAN-2017 (NJB) </h3>
<pre>
   Added constructors corresponding to CSPICE methods

      CSPICE.spkcpo
      CSPICE.spkcpt
      CSPICE.spkcvo
      CSPICE.spkcvt
</pre>
<h3> Version 1.0.0 30-DEC-2009 (NJB) </h3>
*/

public class StateRecord extends StateVector
{

   /*
   Instance variables
   */
   TDBDuration                lightTime;


   /*
   Constructors
   */



   /**
   Copy constructor.

   <p> This constructor creates a deep copy.
   */
   public StateRecord ( StateRecord  sr )

      throws SpiceException
   {
      super( sr );

      lightTime = sr.getLightTime();
   }


   /**
   Construct aberration-corrected StateRecord from ephemeris data.
   */
   public StateRecord ( Body                   target,
                        Time                   t,
                        ReferenceFrame         ref,
                        AberrationCorrection   abcorr,
                        Body                   observer )

      throws SpiceException
   {
      super();

      String   targ     = target.getName();
      double   et       = t.getTDBSeconds();
      String   frame    = ref.getName();
      String   corr     = abcorr.getName();
      String   obs      = observer.getName();
      double[] v        = new double[6];
      double[] lt       = new double[1];

      CSPICE.spkezr ( targ, et, frame, corr, obs, v, lt );

      super.assign( v );

      this.lightTime = new TDBDuration( lt[0] );
   }



   /**
   Construct aberration-corrected StateRecord from ephemeris data
   and an observer position vector. This method provides
   functionality analogous to that of the CSPICE routine spkcpo_c.
   */
   public StateRecord ( Body                   target,
                        Time                   t,
                        ReferenceFrame         outref,
                        String                 refloc,
                        AberrationCorrection   abcorr,
                        Vector3                obspos,
                        Body                   obsctr,
                        ReferenceFrame         obsref  )

      throws SpiceException
   {
      super();

      String   targ     = target.getName();
      double   et       = t.getTDBSeconds();
      String   outfrm   = outref.getName();
      String   corr     = abcorr.getName();
      double[] obsvec   = obspos.toArray();
      String   obscen   = obsctr.getName();
      String   obsfrm   = obsref.getName();

      double[] v        = new double[6];
      double[] lt       = new double[1];

      CSPICE.spkcpo ( targ, et, outfrm, refloc, corr, 
                      obsvec,   obscen, obsfrm, v,      lt );

      super.assign( v );

      this.lightTime = new TDBDuration( lt[0] );
   }


   /**
   Construct aberration-corrected StateRecord from ephemeris data
   and an observer state vector. This method provides
   functionality analogous to that of the CSPICE routine spkcvo_c.
   */
   public StateRecord ( Body                   target,
                        Time                   t,
                        ReferenceFrame         outref,
                        String                 refloc,
                        AberrationCorrection   abcorr,
                        Vector6                obssta,
                        Time                   obsepc,
                        Body                   obsctr,
                        ReferenceFrame         obsref  )

      throws SpiceException
   {
      super();

      String   targ     = target.getName();
      double   et       = t.getTDBSeconds();
      String   outfrm   = outref.getName();
      String   corr     = abcorr.getName();
      double[] obspv    = obssta.toArray();
      double   obset    = obsepc.getTDBSeconds();
      String   obscen   = obsctr.getName();
      String   obsfrm   = obsref.getName();

      double[] v        = new double[6];
      double[] lt       = new double[1];

      CSPICE.spkcvo ( targ,  et,    outfrm, refloc, corr, 
                      obspv, obset, obscen, obsfrm, v,    lt );

      super.assign( v );

      this.lightTime = new TDBDuration( lt[0] );
   }


   /**
   Construct aberration-corrected StateRecord from ephemeris data
   and a target position vector. This method provides
   functionality analogous to that of the CSPICE routine spkcpt_c.
   */
   public StateRecord ( Vector3                trgpos,
                        Body                   trgctr,
                        ReferenceFrame         trgref,
                        Time                   t,
                        ReferenceFrame         outref,
                        String                 refloc,
                        AberrationCorrection   abcorr,
                        Body                   obsrvr )

      throws SpiceException
   {
      super();

      double[] trgvec   = trgpos.toArray();
      String   trgcen   = trgctr.getName();
      String   trgfrm   = trgref.getName();
      double   et       = t.getTDBSeconds();
      String   outfrm   = outref.getName();
      String   corr     = abcorr.getName();
      String   obs      = obsrvr.getName();

      double[] v        = new double[6];
      double[] lt       = new double[1];

      CSPICE.spkcpt ( trgvec, trgcen, trgfrm, et, outfrm,  
                      refloc, corr,   obs,    v,  lt      );

      super.assign( v );

      this.lightTime = new TDBDuration( lt[0] );
   }


   /**
   Construct aberration-corrected StateRecord from ephemeris data
   and a target state vector. This method provides
   functionality analogous to that of the CSPICE routine spkcvt_c.
   */
   public StateRecord ( Vector6                trgsta,
                        Time                   trgepc,
                        Body                   trgctr,
                        ReferenceFrame         trgref,
                        Time                   t,
                        ReferenceFrame         outref,
                        String                 refloc,
                        AberrationCorrection   abcorr,
                        Body                   obsrvr )

      throws SpiceException
   {
      super();

      double[] trgpv    = trgsta.toArray();
      double   trget    = trgepc.getTDBSeconds();
      String   trgcen   = trgctr.getName();
      String   trgfrm   = trgref.getName();
      double   et       = t.getTDBSeconds();
      String   outfrm   = outref.getName();
      String   corr     = abcorr.getName();
      String   obs      = obsrvr.getName();

      double[] v        = new double[6];
      double[] lt       = new double[1];

      CSPICE.spkcvt ( trgpv,  trget, trgcen, trgfrm, et, outfrm,  
                      refloc, corr,  obs,    v,      lt          );

      super.assign( v );

      this.lightTime = new TDBDuration( lt[0] );
   }



   /*
   Instance methods
   */


   /**
   Create a String representation of this StateRecord.
   */
   public String toString()
   {
      String outStr;


      double[] v = this.toArray();

      try
      {
         outStr = String.format(

            "%n" +
            "State vector = "                                + "%n" +
            "%n" +
            "    X: " + "%24.16e"        + " (km)" + "%n" +
            "    Y: " + "%24.16e"        + " (km)" + "%n" +
            "    Z: " + "%24.16e"        + " (km)" + "%n" +
            "   VX: " + "%24.16e"        + " (km/s)" + "%n" +
            "   VY: " + "%24.16e"        + " (km/s)" + "%n" +
            "   VZ: " + "%24.16e"        + " (km/s)" + "%n" +
            "%n" +

            "Distance           = " + "%24.16e"  + "  (km)" + "%n" +
            "Speed              = " + "%24.16e"  + "  (km/s)" + "%n" +
            "One way light time = " + "%24.16e"  + "  (s)" + "%n",

            v[0], v[1], v[2],
            v[3], v[4], v[5],
            this.getPosition().norm(),
            this.getVelocity().norm(),
            lightTime.getMeasure()
         );
      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr );
   }



   /**
   Get the state vector.
   */
   public StateVector getStateVector()

      throws SpiceException
   {
      return ( new StateVector ( this )  );
   }

   /**
   Get one way light time between target and observer.
   */
   public TDBDuration getLightTime()
   {
      return (  new TDBDuration ( lightTime )  );
   }

}
