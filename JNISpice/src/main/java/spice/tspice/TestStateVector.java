
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestStateVector provides methods that implement test families for
the class StateVector.

<h3>Version 2.0.0 25-JAN-2017 (NJB)</h3>

Added tests for SPK constant position and constant velocity
constructors. These are analogs of SPKCPO, SPKCVO, SPKCPT,
and SPKCVT.

<p>Moved clean-up code to "finally" block.

<h3>Version 1.0.0 18-DEC-2009 (NJB)</h3>
*/
public class TestStateVector extends Object
{

   //
   // Class constants
   //
   private static String  NATPCK        = "nat.tpc";
   private static String  NATSPK        = "nat.bsp";
   private static String  PCK           = "test.tpc";
   private static String  SPK           = "test.bsp";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test StateVector and associated classes.
   */
   public static boolean f_StateVector()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;

      final String                      endl = System.getProperty(
                                                            "line.separator" );

      //
      // Local variables
      //
      AberrationCorrection              abcorr;
      AberrationCorrection              LTCorr;
      AberrationCorrection              LTSCorr;

      AberrationCorrection              noCorr = new
                                        AberrationCorrection( "None" );

      Body                              Alpha  = new Body( "Alpha" );
      Body                              Beta   = new Body( "Beta"  );
      Body                              SSB    = new Body (
                                                   "Solar System Barycenter" );
      Body                              center;
      Body                              observer;
      Body                              target;

      PositionVector                    corVec;
      PositionVector                    obspos;
      PositionVector                    trgpos;

      StateRecord                       sr;

      StateVector                       obssta;
      StateVector                       s0;
      StateVector                       s1;
      StateVector                       s2;
      StateVector                       state;
      StateVector                       trgsta;

      ReferenceFrame                    frame;
      ReferenceFrame                    outFrame;

      String                            displayStr;
      String                            refloc;
      String                            timstr;
      String                            xStr;

      TDBTime                           et;
      TDBTime                           trgepc;

      Vector6                           v0      = new Vector6(
                                                            9.0,  12.0, 36.0,
                                                            12.0, 16.0, 48.0 );
      Vector6                           zeroVec = new Vector6();

      boolean                           ok;

      double                            sep;

      int                               handle = 0;
      int                               i;
      int                               natHan = 0;


      //
      //  We enclose all tests in a try/catch block in order to
      //  facilitate handling unexpected exceptions.  Unexpected
      //  exceptions are trapped by the catch block at the end of
      //  the routine; expected exceptions are handled locally by
      //  catch blocks associated with error handling test cases.
      //
      //  Therefore, JNISpice calls that are expected to succeed don't
      //  have any subsequent "chckxc" type calls following them, nor
      //  are they wrapped in in try/catch blocks.
      //
      //  Expected exceptions that are *not* thrown are tested
      //  via a call to {@link spice.testutils.Testutils#dogDidNotBark}.
      //

      try
      {

         JNITestutils.topen ( "f_StateVector" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: create and load kernels." );


         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();

         JNITestutils.tstlsk();

         //
         // Delete PCK if it exists. Create and load a PCK file.
         //
         ( new File ( PCK ) ).delete();

         JNITestutils.tstpck( PCK, true, false );

         //
         // Same for Nat's solar system PKK.
         //
         ( new File ( NATPCK ) ).delete();

         JNITestutils.natpck( PCK, true, false );


         //
         // Delete SPK if it exists. Create and load a new
         // version of the file.
         //
         ( new File ( SPK ) ).delete();

         handle = JNITestutils.tstspk( SPK, true );


         //
         // Same for Nat's solar system SPK.
         //
         ( new File ( NATSPK ) ).delete();

         natHan = JNITestutils.natspk( NATSPK, true );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error:  " +
                               "Lookup for non-existent object." );

         try
         {
            timstr      = "2009 NOV 18 00:00:00";

            target      = new Body( "Ida" );
            et          = new TDBTime( timstr );
            frame       = new ReferenceFrame( "J2000" );
            abcorr      = new AberrationCorrection( "LT+S" );
            observer    = new Body( "Earth" );

            state         = new StateVector( target, et, frame, abcorr,
                                                                   observer );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(SPKINSUFFDATA)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(SPKINSUFFDATA)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test no-args constructor." );

         state = new StateVector();

         ok = JNITestutils.chckad ( "state",
                                    state.toArray(),
                                    "~~",
                                    zeroVec.toArray(),
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test Vector6-based constructor." );

         state = new StateVector( v0 );

         ok = JNITestutils.chckad ( "state",
                                    state.toArray(),
                                    "~~",
                                    v0.toArray(),
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test array-based constructor." );

         state = new StateVector( v0.toArray() );

         ok = JNITestutils.chckad ( "state",
                                    state.toArray(),
                                    "~~",
                                    v0.toArray(),
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test Vector3-based constructor." );

         double[] v = v0.toArray();

         Vector3 v3a = new Vector3( v[0], v[1], v[2] );
         Vector3 v3b = new Vector3( v[3], v[4], v[5] );

         state = new StateVector( v3a, v3b );

         ok = JNITestutils.chckad ( "state",
                                    state.toArray(),
                                    "~~",
                                    v,
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor." );

         s0  = new StateVector( v0 );

         state = new StateVector( s0 );

         ok = JNITestutils.chckad ( "state",
                                    state.toArray(),
                                    "~~",
                                    s0.toArray(),
                                    TIGHT_TOL         );

         //
         // Make sure that changing s0 doesn't affect state.
         //
         s0 = new StateVector(  v0.scale(2.0)  );

         s1 = new StateVector( v0 );

         ok = JNITestutils.chckad ( "state",
                                    state.toArray(),
                                    "~~",
                                    s1.toArray(),
                                    TIGHT_TOL         );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test principal constructor: find state of " +
                              "Moon relative to Earth; compare to " +
                              "StateRecord."                                 );


         frame    = new ReferenceFrame      ( "J2000" );
         abcorr   = new AberrationCorrection( "LT+S"  );
         observer = new Body( "Earth" );
         target   = new Body( "Moon"  );
         et       = new TDBTime ( "2009 NOV 11 18:00" );

         state      = new StateVector( target, et, frame, abcorr, observer );
         sr       = new StateRecord   ( target, et, frame, abcorr, observer );

         ok = JNITestutils.chckad ( "state",
                                    state.toArray(),
                                    "~~/",
                                    sr.toArray(),
                                    TIGHT_TOL                   );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test principal constructor: find states of " +
                              "Alpha and Beta relative to Sun at " +
                              "occultation midpoint time."                   );


         frame    = new ReferenceFrame      ( "J2000" );
         abcorr   = new AberrationCorrection( "LT+S"  );
         observer = new Body( "Sun" );
         et       = new TDBTime ( "2000 JAN 1 12:05:01.000 TDB" );

         s0      = new StateVector( Alpha, et, frame, abcorr, observer );
         s1      = new StateVector( Beta,  et, frame, abcorr, observer );

         //
         // Find the angular separation between the position components
         // of s0 and s1.
         //
         sep     = (s0.getPosition()).sep( s1.getPosition() );

         ok = JNITestutils.chcksd ( "position sep",
                                    sep,
                                    "~",
                                    0.0,
                                    TIGHT_TOL                   );

         sep     = (s0.getVelocity()).sep( s1.getVelocity() );

         ok = JNITestutils.chcksd ( "velocity sep",
                                    sep,
                                    "~",
                                    0.0,
                                    TIGHT_TOL                   );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCPO analog constructor: find state of " +
                              "the moon relative to the \"Goldstone tracking " +
                              "station in the IAU_EARTH frame.\""             );

         //
         // First compute the expected state `s0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( 399001  );
         target   = new Body( "Moon"  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         s0       = new StateVector( target, et, frame, abcorr, observer );


         //
         // Get the observer's body-fixed position vector.
         //
         obspos   = new PositionVector( observer, et, frame, noCorr, center );

         // System.out.println( "obspos = " + obspos );


         //
         // Get the target state relative to the observer's position. For 
         // the output state, evaluate the body-fixed frame at an epoch
         // corrected for one-way light time between the surface point 
         // and the frame center, since that's what SPKEZR will do.
         //
         refloc   = "CENTER";

         s1 = new StateVector( target, et,     frame,  refloc, 
                               abcorr, obspos, center, frame );

         // System.out.println( s0 );
         // System.out.println( s1 );


         ok = JNITestutils.chckad ( "position",
                                    s1.getPosition().toArray(),
                                    "~~/",
                                    s0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    s1.getVelocity().toArray(),
                                    "~~/",
                                    s0.getVelocity().toArray(),
                                    TIGHT_TOL                 );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCPO analog constructor: find state of " +
                              "the moon relative to the \"Goldstone tracking " +
                              "station in the J2000 frame.\""              );

         //
         // First compute the expected state `s0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );
         outFrame = new ReferenceFrame      ( "J2000"     );
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( 399001  );
         target   = new Body( "Moon"  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         s0       = new StateVector( target, et, outFrame, abcorr, observer );


         //
         // Get the observer's body-fixed position vector.
         //
         obspos   = new PositionVector( observer, et, frame, noCorr, center );

         // System.out.println( "obspos = " + obspos );


         //
         // Get the target state relative to the observer's position. For 
         // the output state, since we use an inertial frame, REFLOC 
         // shouldn't matter. Make sure that it's recognized, though.
         //
         refloc   = "OBSERVER";

         s1 = new StateVector( target, et,     outFrame, refloc, 
                               abcorr, obspos, center,   frame  );

         // System.out.println( s0 );
         // System.out.println( s1 );


         ok = JNITestutils.chckad ( "position",
                                    s1.getPosition().toArray(),
                                    "~~/",
                                    s0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    s1.getVelocity().toArray(),
                                    "~~/",
                                    s0.getVelocity().toArray(),
                                    TIGHT_TOL                 );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCVO analog constructor: find state of " +
                              "the moon relative to the \"Goldstone tracking " +
                              "station in the IAU_EARTH frame.\""             );

         //
         // First compute the expected state `s0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( 399001  );
         target   = new Body( "Moon"  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         s0       = new StateVector( target, et, frame, abcorr, observer );


         //
         // Get the observer's body-fixed state vector.
         //
         obssta   = new StateVector( observer, et, frame, noCorr, center );

         // System.out.println( "obspos = " + obspos );


         //
         // Get the target state relative to the observer's position. For 
         // the output state, evaluate the body-fixed frame at an epoch
         // corrected for one-way light time between the surface point 
         // and the frame center, since that's what SPKEZR will do.
         //
         refloc   = "CENTER";

         s1 = new StateVector( target, et,     frame, refloc, 
                               abcorr, obssta, et,    center, frame );

         // System.out.println( s0 );
         // System.out.println( s1 );


         ok = JNITestutils.chckad ( "position",
                                    s1.getPosition().toArray(),
                                    "~~/",
                                    s0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    s1.getVelocity().toArray(),
                                    "~~/",
                                    s0.getVelocity().toArray(),
                                    TIGHT_TOL                 );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCVO analog constructor: find state of " +
                              "the sun relative to the moon " +
                              "station in the J200O frame. Note that " +
                              "this gives us a non-zero observer velocity. " +
                              "We use CN correction."                        );

         //
         // First compute the expected state `s0.'
         //
         frame    = new ReferenceFrame      ( "J2000" );

         //
         // We would need time-dependent observer velocity in order to
         // derive an estimate of acceleration, which is needed to
         // compute velocity when stellar aberration correction is
         // performed. So just use light time correction.
         //
         abcorr   = new AberrationCorrection( "CN"  );
         observer = new Body( "Moon"  );
         target   = new Body( "Sun"  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         s0       = new StateVector( target, et, frame, abcorr, observer );


         //
         // Get the observer's body-fixed state vector.
         //
         obssta   = new StateVector( observer, et, frame, noCorr, center );

         //System.out.println( "obssta = " + obssta );


         //
         // Get the target state relative to the observer's position. For 
         // the output state, evaluate the body-fixed frame at an epoch
         // corrected for one-way light time between the surface point 
         // and the frame center, since that's what SPKEZR will do.
         //
         refloc   = "CENTER";

         s1 = new StateVector( target, et,     frame, refloc, 
                               abcorr, obssta, et,    center, frame );

         // System.out.println( s0 );
         // System.out.println( s1 );


         ok = JNITestutils.chckad ( "position",
                                    s1.getPosition().toArray(),
                                    "~~/",
                                    s0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    s1.getVelocity().toArray(),
                                    "~~/",
                                    s0.getVelocity().toArray(),
                                    TIGHT_TOL                 );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCPT analog constructor: find state of " +
                              "the \"Goldstone tracking station\" relative " +
                              "to the moon in the IAU_MOON frame."            );

         //
         // First compute the expected state `s0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( "Moon"  );
         target   = new Body( 399001  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         s0       = new StateVector( target, et, frame, abcorr, observer );


         //
         // Get the target's body-fixed position vector.
         //
         trgpos   = new PositionVector( target, et, frame, noCorr, center );

         // System.out.println( "obspos = " + obspos );


         //
         // Get the target state relative to the observer's position. For 
         // the output state, evaluate the body-fixed frame at an epoch
         // corrected for one-way light time between the surface point 
         // and the frame center, since that's what SPKEZR will do.
         //
         refloc   = "CENTER";

         s1 = new StateVector( trgpos, center, frame,  et, 
                               frame,  refloc, abcorr, observer );

         // System.out.println( s0 );
         // System.out.println( s1 );


         ok = JNITestutils.chckad ( "position",
                                    s1.getPosition().toArray(),
                                    "~~/",
                                    s0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    s1.getVelocity().toArray(),
                                    "~~/",
                                    s0.getVelocity().toArray(),
                                    TIGHT_TOL                 );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCPT analog constructor: find state of " +
                              "the \"Goldstone tracking station\" relative " +
                              "to the moon in the J2000 frame."              );

         //
         // First compute the expected state `s0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );
         outFrame = new ReferenceFrame      ( "J2000"     );
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( "Moon"  );
         target   = new Body( 399001  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         s0       = new StateVector( target, et, outFrame, abcorr, observer );


         //
         // Get the target's body-fixed position vector.
         //
         trgpos   = new PositionVector( target, et, frame, noCorr, center );

         // System.out.println( "obspos = " + obspos );

         //
         // Get the target state relative to the observer's position. For 
         // the output state, since we use an inertial frame, REFLOC 
         // shouldn't matter. Make sure that it's recognized, though.
         //
         refloc   = "OBSERVER";

         s1 = new StateVector( trgpos,   center, frame,  et, 
                               outFrame, refloc, abcorr, observer );

         // System.out.println( s0 );
         // System.out.println( s1 );


         ok = JNITestutils.chckad ( "position",
                                    s1.getPosition().toArray(),
                                    "~~/",
                                    s0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    s1.getVelocity().toArray(),
                                    "~~/",
                                    s0.getVelocity().toArray(),
                                    TIGHT_TOL                 );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCVT analog constructor: find state of " +
                              "the \"Goldstone tracking station\" relative " +
                              "to the moon in the IAU_EARTH frame."           );

         //
         // First compute the expected state `s0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );         
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( "Moon"  );
         target   = new Body( 399001  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         sr       = new StateRecord( target, et, frame, abcorr, observer );


         //
         // Get the target's body-fixed state vector at the target epoch.
         //
         trgepc   = et.sub ( sr.getLightTime() );

         trgsta   = new StateVector( target, trgepc, frame, noCorr, center );

         //System.out.println( "trgsta = " + trgsta );

       
         //
         // Get the target state relative to the observer's position. For 
         // the output state, evaluate the body-fixed frame at an epoch
         // corrected for one-way light time between the surface point 
         // and the frame center, since that's what SPKEZR will do.
         //

         refloc   = "CENTER";

         s1 = new StateVector( trgsta, trgepc, center, frame,  et, 
                               frame,  refloc, abcorr, observer );

         // System.out.println( sr );
         // System.out.println( s1 );


         ok = JNITestutils.chckad ( "position",
                                    s1.getPosition().toArray(),
                                    "~~/",
                                    sr.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    s1.getVelocity().toArray(),
                                    "~~/",
                                    sr.getVelocity().toArray(),
                                    TIGHT_TOL                 );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCVT analog constructor: find state of " +
                              "the \"Goldstone tracking station\" relative " +
                              "to the moon in the IAU_MOON frame."            );

         //
         // First compute the expected state `s0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );         
         outFrame = new ReferenceFrame      ( "IAU_MOON"  );         
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( "Moon"  );
         target   = new Body( 399001  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         sr       = new StateRecord( target, et, outFrame, abcorr, observer );


         //
         // Get the target's body-fixed state vector at the target epoch.
         //
         trgepc   = et.sub ( sr.getLightTime() );

         trgsta   = new StateVector( target, trgepc, frame, noCorr, center );

         //System.out.println( "trgsta = " + trgsta );

       
         //
         // Get the target state relative to the observer's position. For 
         // the output state, evaluate the body-fixed frame at an epoch
         // corrected for one-way light time between the surface point 
         // and the frame center, since that's what SPKEZR will do.
         //

         refloc   = "OBSERVER";

         s1 = new StateVector( trgsta,   trgepc, center, frame,  et, 
                               outFrame, refloc, abcorr, observer );

         // System.out.println( sr );
         // System.out.println( s1 );


         ok = JNITestutils.chckad ( "position",
                                    s1.getPosition().toArray(),
                                    "~~/",
                                    sr.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    s1.getVelocity().toArray(),
                                    "~~/",
                                    sr.getVelocity().toArray(),
                                    TIGHT_TOL                 );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test reception stellar aberration " +
                              "correction method." );

         //
         // Look up the state of the Jupiter barycenter relative
         // to the Earth with light time only and with light
         // and stellar aberration corrections.

         frame    = new ReferenceFrame      ( "J2000" );
         LTCorr   = new AberrationCorrection( "LT"    );
         LTSCorr  = new AberrationCorrection( "LT+S"  );
         observer = new Body( "Earth" );
         target   = new Body( "Jupiter barycenter" );
         et       = new TDBTime ( "2000 JAN 1 12:05:01.000 TDB" );

         s0       = new StateVector( target, et, frame, LTCorr,  observer );
         s1       = new StateVector( target, et, frame, LTSCorr, observer );

         //
         // Get the state of the Earth relative to the solar system
         // barycenter.
         //
         abcorr   = new AberrationCorrection( "NONE" );

         s2       = new StateVector( observer, et, frame, abcorr, SSB );

         //
         // Correct the light-time corrected Earth-Jupiter barycenter
         // vector for stellar aberration.
         //
         corVec = StateVector.correctStelab( s0.getPosition(),
                                             s2.getVelocity() );

         //
         // Compare corVec to the position component of s1.
         //
         ok = JNITestutils.chckad ( "corVec",
                                    corVec.toArray(),
                                    "~~/",
                                    s1.getPosition().toArray(),
                                    TIGHT_TOL                   );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test transmission stellar aberration " +
                              "correction method." );


         frame    = new ReferenceFrame      ( "J2000" );
         LTCorr   = new AberrationCorrection( "XLT"    );
         LTSCorr  = new AberrationCorrection( "XLT+S"  );
         observer = new Body( "Earth" );
         target   = new Body( "Jupiter barycenter" );
         et       = new TDBTime ( "2000 JAN 1 12:05:01.000 TDB" );

         s0       = new StateVector( target, et, frame, LTCorr,  observer );
         s1       = new StateVector( target, et, frame, LTSCorr, observer );

         //
         // Get the state of the Earth relative to the solar system
         // barycenter.
         //
         abcorr   = new AberrationCorrection( "NONE" );

         s2       = new StateVector( observer, et, frame, abcorr, SSB );

         //
         // Correct the transmission light-time corrected Earth-Jupiter
         // barycenter vector for stellar aberration.
         //
         corVec = StateVector.correctStelabXmit( s0.getPosition(),
                                                 s2.getVelocity() );

         //
         // Compare corVec to the position component of s1.
         //
         ok = JNITestutils.chckad ( "corVec",
                                    corVec.toArray(),
                                    "~~/",
                                    s1.getPosition().toArray(),
                                    TIGHT_TOL                   );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString method." );



         s0         = new StateVector( v0  );

         displayStr = s0.toString();


         xStr       = endl +
                    "State vector ="                           + endl + endl +
                    "    X:   9.0000000000000000e+00 (km)"     + endl +
                    "    Y:   1.2000000000000000e+01 (km)"     + endl +
                    "    Z:   3.6000000000000000e+01 (km)"     + endl +
                    "   VX:   1.2000000000000000e+01 (km/s)"   + endl +
                    "   VY:   1.6000000000000000e+01 (km/s)"   + endl +
                    "   VZ:   4.8000000000000000e+01 (km/s)"   + endl + endl +
                    "Distance =  3.9000000000000000e+01 (km)"   + endl +
                    "Speed    =  5.2000000000000000e+01 (km/s)" + endl;

         // For debugging:
         // System.out.println( xStr );
         // For debugging:
         // System.out.println( displayStr );


         ok = JNITestutils.chcksc ( "display string",
                                    displayStr,
                                    "=",
                                    xStr             );
      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         ex.printStackTrace();

         ok = JNITestutils.chckth ( false, "", ex );
      }

      finally
      {
         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Clean up." );

         //
         // Get rid of the SPK files.
         //
         CSPICE.spkuef( handle );

         ( new File ( SPK ) ).delete();

         CSPICE.spkuef( natHan );

         ( new File ( NATSPK ) ).delete();


         //
         // Get rid of the PCK files.
         //
         ( new File ( PCK    ) ).delete();
         ( new File ( NATPCK ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

