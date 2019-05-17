
package spice.basic;

import spice.basic.CSPICE;

/**
Class StateVector represents states (positions and velocities)
of ephemeris objects relative to other objects.  StateVectors
implicitly carry units of kilometers and kilometers/second.

<h3> Version 2.0.0 25-JAN-2017 (NJB)</h3>
<pre>
   Added constructors corresponding to CSPICE methods

      CSPICE.spkcpo
      CSPICE.spkcpt
      CSPICE.spkcvo
      CSPICE.spkcvt
</pre>
<h3> Version 1.0.0 22-DEC-2009 (NJB)</h3>
*/
public class StateVector extends Vector6
{

   /*
   Static constants
   */


   /*
   Constructors
   */


   /**
   Default constructor:  create a zero-filled state vector.
   */
   public StateVector()
   {
      super ();
   }



   /**
   Copy constructor:  create a deep copy of another StateVector.
   */
   public StateVector ( StateVector state )

      throws SpiceException
   {
      super ( state.toArray() );
   }


   /**
   Construct a StateVector from a Vector6 instance.
   */
   public StateVector ( Vector6 v )
   {
      super( v );
   }

   /**
   Construct a StateVector from a double array of length 6.
   */
   public StateVector( double[] v )

      throws SpiceException
   {
      super( v );
   }


   /**
   Construct a StateVector from two Vector3 instances.
   */
   public StateVector ( Vector3 v1,  Vector3 v2 )
   {
      super( v1, v2 );
   }


   /**
   Construct aberration-corrected StateVector from ephemeris data.
   */
   public StateVector ( Body                   target,
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
      double[] lt       = new double[1];

      double[] v = new double[6];

      CSPICE.spkezr ( targ, et, frame, corr, obs, v, lt );

      super.assign( v );
   }


   /**
   Construct aberration-corrected StateVector from ephemeris data
   and an observer position vector. This method provides
   functionality analogous to that of the CSPICE routine spkcpo_c.
   */
   public StateVector ( Body                   target,
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
   }


   /**
   Construct aberration-corrected StateVector from ephemeris data
   and an observer state vector. This method provides
   functionality analogous to that of the CSPICE routine spkcvo_c.
   */
   public StateVector ( Body                   target,
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
   }


   /**
   Construct aberration-corrected StateVector from ephemeris data
   and a target position vector. This method provides
   functionality analogous to that of the CSPICE routine spkcpt_c.
   */
   public StateVector ( Vector3                trgpos,
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
   }


   /**
   Construct aberration-corrected StateVector from ephemeris data
   and a target state vector. This method provides
   functionality analogous to that of the CSPICE routine spkcvt_c.
   */
   public StateVector ( Vector6                trgsta,
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
   }



   //
   // Instance methods
   //

   /**
   Return a PositionVector instance consisting of the position portion of
   this state vector.
   */
   public PositionVector getPosition()
   {
      double[] v   =  this.toArray();

      Vector3 pos  =  new Vector3( v[0], v[1], v[2] );

      return (  new PositionVector( pos )  );
   }


   /**
   Return a VelocityVector instance consisting of the velocity portion of
   this state vector.
   */
   public VelocityVector getVelocity()
   {
      double[] v   =  this.toArray();

      Vector3 pos  =  new Vector3( v[3], v[4], v[5] );

      return (  new VelocityVector( pos )  );
   }


   /**
   Create a String representation of this StateVector.
   */
   public String toString()
   {
      String outStr;

      double[] v = this.toArray();

      try
      {
         outStr = String.format(

            "%n" +
            "State vector ="                         + "%n" +
            "%n" +
            "    X: " + "%24.16e"        + " (km)"   + "%n" +
            "    Y: " + "%24.16e"        + " (km)"   + "%n" +
            "    Z: " + "%24.16e"        + " (km)"   + "%n" +
            "   VX: " + "%24.16e"        + " (km/s)" + "%n" +
            "   VY: " + "%24.16e"        + " (km/s)" + "%n" +
            "   VZ: " + "%24.16e"        + " (km/s)" + "%n" +
            "%n" +

            "Distance =" + "%24.16e"  + " (km)"     + "%n" +
            "Speed    =" + "%24.16e"  + " (km/s)"   + "%n",

            v[0], v[1], v[2],
            v[3], v[4], v[5],
            this.getPosition().norm(),
            this.getVelocity().norm()
         );

      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr );
   }


   //
   // Static methods
   //

   /**
   Correct a PositionVector for reception stellar aberration.
   */
   public static PositionVector correctStelab( PositionVector     pobj,
                                               VelocityVector     vobs )
      throws SpiceException
   {
      double[] appobj = CSPICE.stelab( pobj.toArray(), vobs.toArray() );

      return(  new PositionVector( new Vector3(appobj) )  );
   }

   /**
   Correct a PositionVector for transmission stellar aberration.
   */
   public static PositionVector correctStelabXmit( PositionVector     pobj,
                                                   VelocityVector     vobs )
      throws SpiceException
   {
      double[] appobj = CSPICE.stlabx( pobj.toArray(), vobs.toArray() );

      return(  new PositionVector( new Vector3(appobj) )  );
   }

}
