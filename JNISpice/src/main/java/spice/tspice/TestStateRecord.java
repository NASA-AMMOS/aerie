
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestStateRecord provides methods that implement test families for
the class StateRecord.

<p> Much of the testing of StateRecord is done in the class TestSPK.
For example, that test family writes SPK files and then recovers
the data using StateRecord constructor calls. These checks ensure
that accurate states are returned.

<p> This test family checks other aspects of the StateRecord API.

<h3>Version 2.0.0 25-JAN-2017 (NJB)</h3>

Added tests for SPK constant position and constant velocity
constructors. These are analogs of SPKCPO, SPKCVO, SPKCPT,
and SPKCVT.

<p>Moved clean-up code to "finally" block.

<h3>Version 1.0.0 30-DEC-2009 (NJB)</h3>
*/
public class TestStateRecord extends Object
{

   //
   // Class constants
   //
   private static String  NATSPK        = "nat.bsp";
   private static String  NATPCK        = "nat.tpc";
   private static String  PCK           = "test.tpc";
   private static String  SPK           = "test.bsp";

   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test StateRecord and associated classes.
   */
   public static boolean f_StateRecord()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-9;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      AberrationCorrection              noCorr = new
                                        AberrationCorrection( "None" );

      Body                              center;
      Body                              observer;
      Body                              target;
      Body                              target2;

      PositionVector                    obspos;
      PositionVector                    trgpos;
         

      ReferenceFrame                    ref;
      ReferenceFrame                    frame;
      ReferenceFrame                    outFrame;

      StateRecord                       sr0;
      StateRecord                       sr1;
      StateRecord                       sr2;

      StateVector                       obssta;
      StateVector                       trgsta;

      String                            outStr;
      String                            refloc;
      String                            xStr;

      TDBDuration                       lt0;
      TDBDuration                       lt1;

      TDBTime                           et;
      TDBTime                           et0;
      TDBTime                           trgepc;

      boolean                           ok;

      double                            sep0;
      double[]                          v0;
      double                            xLt;
      double                            xSep;

      int                               nathan = 0;
      int                               handle;
      int                               i;


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

         JNITestutils.topen ( "f_StateRecord" );


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
         // Load Nat's solar system PCK. This provides body/name mappings.
         //
         // Delete the file after loading it.
         //
         ( new File ( NATPCK ) ).delete();

         JNITestutils.natpck( NATPCK, true, false );


         //
         // Delete SPK if it exists. Create and load a new
         // version of the file.
         //
         ( new File ( SPK ) ).delete();

         handle = JNITestutils.tstspk( SPK, true );

         //
         // Load Nat's solar system SPK.
         //
         ( new File ( NATSPK ) ).delete();

         nathan = JNITestutils.natspk( NATSPK, true );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: state lookup without required " +
                               "ephemeris data."                         );

         try
         {
            abcorr   = new AberrationCorrection( "NONE" );
            ref      = new ReferenceFrame( "J2000" );
            observer = new Body( 888 );
            target   = new Body( 10  );
            et0      = new TDBTime( "2009 Dec 30" );

            sr0      = new StateRecord( target, et0, ref, abcorr, observer );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(SPKINSUFFDATA)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(SPKINSUFFDATA)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: state lookup with unrecognized " +
                               "reference frame."                         );

         try
         {
            abcorr   = new AberrationCorrection( "NONE" );
            ref      = new ReferenceFrame( "J200" );
            observer = new Body( "sun"   );
            target   = new Body( "beta"  );
            et0      = new TDBTime( "2009 Dec 30" );

            sr0      = new StateRecord( target,  et0, ref, abcorr, observer );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(UNKNOWNFRAME)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(UNKNOWNFRAME)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );


         abcorr   = new AberrationCorrection( "NONE" );
         ref      = new ReferenceFrame( "J2000" );
         observer = new Body( "sun"   );
         target   = new Body( "alpha" );
         target2  = new Body( "beta"  );
         et0      = new TDBTime( "2000 Jan 1 12:05:00 TDB" );

         sr0      = new StateRecord( target,  et0, ref, abcorr, observer );
         sr1      = new StateRecord( target,  et0, ref, abcorr, observer );
         sr2      = new StateRecord( sr0 );

         //
         // Change sr0; make sure sr2 doesn't change.
         //
         sr0      = new StateRecord( target2,  et0, ref, abcorr, observer );


         //
         // The contents of sr2 should match those of sr1 exactly.
         //
         ok = JNITestutils.chckad( "sr2",
                                   sr2.toArray(),
                                   "=",
                                   sr1.toArray(),
                                   0.0             );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Find angular separation of Sun-Alpha and " +
                              "Sun-Beta positions at occultation midpoint. " +
                              "Use geometric states."                         );


         abcorr   = new AberrationCorrection( "NONE" );
         ref      = new ReferenceFrame( "J2000" );
         observer = new Body( "sun"   );
         target   = new Body( "alpha" );
         target2  = new Body( "beta"  );
         et0      = new TDBTime( "2000 Jan 1 12:05:00 TDB" );

         sr0      = new StateRecord( target,  et0, ref, abcorr, observer );
         sr1      = new StateRecord( target2, et0, ref, abcorr, observer );

         sep0     = ( sr0.getPosition() ).sep( sr1.getPosition() );




         ok = JNITestutils.chcksd ( "sep0",
                                    sep0,
                                    "~",
                                    0.0,
                                    TIGHT_TOL );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Find angular separation of Sun-Alpha and " +
                              "Sun-Beta velocities at occultation midpoint. " +
                              "Use geometric states."                         );

         sep0     = ( sr0.getVelocity() ).sep( sr1.getVelocity() );


         ok = JNITestutils.chcksd ( "sep0",
                                    sep0,
                                    "~",
                                    0.0,
                                    TIGHT_TOL );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Find angular separation of Sun-Alpha and " +
                              "Sun-Beta positions at occultation midpoint. " +
                              "Use LT correction."                         );


         abcorr   = new AberrationCorrection( "LT" );
         ref      = new ReferenceFrame( "J2000" );
         observer = new Body( "sun"   );
         target   = new Body( "alpha" );
         target2  = new Body( "beta"  );
         et0      = new TDBTime( "2000 Jan 1 12:05:01.000000 TDB" );

         sr0      = new StateRecord( target,  et0, ref, abcorr, observer );
         sr1      = new StateRecord( target2, et0, ref, abcorr, observer );

         sep0     = ( sr0.getPosition() ).sep( sr1.getPosition() );




         ok = JNITestutils.chcksd ( "sep0",
                                    sep0,
                                    "~",
                                    0.0,
                                    TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Find angular separation of Sun-Alpha and " +
                              "Sun-Beta velocities at occultation midpoint. " +
                              "Use CN correction."                         );

         abcorr   = new AberrationCorrection( "CN" );

         sr0      = new StateRecord( target,  et0, ref, abcorr, observer );
         sr1      = new StateRecord( target2, et0, ref, abcorr, observer );


         sep0     = ( sr0.getVelocity() ).sep( sr1.getVelocity() );


         ok = JNITestutils.chcksd ( "sep0",
                                    sep0,
                                    "~",
                                    0.0,
                                    TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getLightTime."  );


         //
         // Both Alpha and Beta perform circular motion about the Sun,
         // so the light times are constant. Check for consistency
         // between light time and distance.
         //

         sr0 = new StateRecord( target,  et0, ref, abcorr, observer );


         xLt = sr0.getPosition().norm() / CSPICE.clight();

         ok = JNITestutils.chcksd ( "light time",
                                    sr0.getLightTime().getMeasure(),
                                    "~/",
                                    xLt,
                                    TIGHT_TOL );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCPO analog constructor: find state of " +
                              "the moon relative to the \"Goldstone tracking " +
                              "station in the IAU_EARTH frame.\""             );

         //
         // First compute the expected state `sr0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( 399001  );
         target   = new Body( "Moon"  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         sr0      = new StateRecord( target, et, frame, abcorr, observer );


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

         sr1 = new StateRecord( target, et,     frame,  refloc, 
                                abcorr, obspos, center, frame );

         // System.out.println( sr0 );
         // System.out.println( sr1 );


         ok = JNITestutils.chckad ( "position",
                                    sr1.getPosition().toArray(),
                                    "~~/",
                                    sr0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    sr1.getVelocity().toArray(),
                                    "~~/",
                                    sr0.getVelocity().toArray(),
                                    TIGHT_TOL                 );

         ok = JNITestutils.chcksd ( "Light time",
                                    sr1.getLightTime().getMeasure(),
                                    "~/",
                                    sr0.getLightTime().getMeasure(),
                                    TIGHT_TOL                       );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCPO analog constructor: find state of " +
                              "the moon relative to the \"Goldstone tracking " +
                              "station in the J2000 frame.\""              );

         //
         // First compute the expected state `sr0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );
         outFrame = new ReferenceFrame      ( "J2000"     );
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( 399001  );
         target   = new Body( "Moon"  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         sr0      = new StateRecord( target, et, outFrame, abcorr, observer );


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

         sr1 = new StateRecord( target, et,     outFrame, refloc, 
                                abcorr, obspos, center,   frame  );

         // System.out.println( sr0 );
         // System.out.println( sr1 );


         ok = JNITestutils.chckad ( "position",
                                    sr1.getPosition().toArray(),
                                    "~~/",
                                    sr0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    sr1.getVelocity().toArray(),
                                    "~~/",
                                    sr0.getVelocity().toArray(),
                                    TIGHT_TOL                 );


         ok = JNITestutils.chcksd ( "Light time",
                                    sr1.getLightTime().getMeasure(),
                                    "~/",
                                    sr0.getLightTime().getMeasure(),
                                    TIGHT_TOL                       );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCVO analog constructor: find state of " +
                              "the moon relative to the \"Goldstone tracking " +
                              "station in the IAU_EARTH frame.\""             );

         //
         // First compute the expected state `sr0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( 399001  );
         target   = new Body( "Moon"  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         sr0      = new StateRecord( target, et, frame, abcorr, observer );


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

         sr1 = new StateRecord( target, et,     frame, refloc, 
                                abcorr, obssta, et,    center, frame );

         // System.out.println( sr0 );
         // System.out.println( sr1 );


         ok = JNITestutils.chckad ( "position",
                                    sr1.getPosition().toArray(),
                                    "~~/",
                                    sr0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    sr1.getVelocity().toArray(),
                                    "~~/",
                                    sr0.getVelocity().toArray(),
                                    TIGHT_TOL                   );

         ok = JNITestutils.chcksd ( "Light time",
                                    sr1.getLightTime().getMeasure(),
                                    "~/",
                                    sr0.getLightTime().getMeasure(),
                                    TIGHT_TOL                       );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCVO analog constructor: find state of " +
                              "the sun relative to the moon " +
                              "station in the J200O frame. Note that " +
                              "this gives us a non-zero observer velocity. " +
                              "We use CN correction."                        );

         //
         // First compute the expected state `sr0.'
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

         sr0      = new StateRecord( target, et, frame, abcorr, observer );


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

         sr1 = new StateRecord( target, et,     frame, refloc, 
                                abcorr, obssta, et,    center, frame );

         // System.out.println( sr0 );
         // System.out.println( sr1 );


         ok = JNITestutils.chckad ( "position",
                                    sr1.getPosition().toArray(),
                                    "~~/",
                                    sr0.getPosition().toArray(),
                                    TIGHT_TOL                   );


         ok = JNITestutils.chckad ( "velocity",
                                    sr1.getVelocity().toArray(),
                                    "~~/",
                                    sr0.getVelocity().toArray(),
                                    TIGHT_TOL                  );

         ok = JNITestutils.chcksd ( "Light time",
                                    sr1.getLightTime().getMeasure(),
                                    "~/",
                                    sr0.getLightTime().getMeasure(),
                                    TIGHT_TOL                       );


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

         sr0      = new StateRecord( target, et, frame, abcorr, observer );


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

         sr1 = new StateRecord( trgpos, center, frame,  et, 
                                frame,  refloc, abcorr, observer );

         // System.out.println( sr0 );
         // System.out.println( sr1 );


         ok = JNITestutils.chckad ( "position",
                                    sr1.getPosition().toArray(),
                                    "~~/",
                                    sr0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    sr1.getVelocity().toArray(),
                                    "~~/",
                                    sr0.getVelocity().toArray(),
                                    TIGHT_TOL                 );

         ok = JNITestutils.chcksd ( "Light time",
                                    sr1.getLightTime().getMeasure(),
                                    "~/",
                                    sr0.getLightTime().getMeasure(),
                                    TIGHT_TOL                       );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCPT analog constructor: find state of " +
                              "the \"Goldstone tracking station\" relative " +
                              "to the moon in the J2000 frame."              );

         //
         // First compute the expected state `sr0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );
         outFrame = new ReferenceFrame      ( "J2000"     );
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( "Moon"  );
         target   = new Body( 399001  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         sr0      = new StateRecord( target, et, outFrame, abcorr, observer );


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

         sr1 = new StateRecord( trgpos,   center, frame,  et, 
                                outFrame, refloc, abcorr, observer );

         // System.out.println( sr0 );
         // System.out.println( sr1 );


         ok = JNITestutils.chckad ( "position",
                                    sr1.getPosition().toArray(),
                                    "~~/",
                                    sr0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    sr1.getVelocity().toArray(),
                                    "~~/",
                                    sr0.getVelocity().toArray(),
                                    TIGHT_TOL                 );

         ok = JNITestutils.chcksd ( "Light time",
                                    sr1.getLightTime().getMeasure(),
                                    "~/",
                                    sr0.getLightTime().getMeasure(),
                                    TIGHT_TOL                       );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCVT analog constructor: find state of " +
                              "the \"Goldstone tracking station\" relative " +
                              "to the moon in the IAU_EARTH frame."           );

         //
         // First compute the expected state `sr0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );         
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( "Moon"  );
         target   = new Body( 399001  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         sr0      = new StateRecord( target, et, frame, abcorr, observer );


         //
         // Get the target's body-fixed state vector at the target epoch.
         //
         trgepc   = et.sub ( sr0.getLightTime() );

         trgsta   = new StateVector( target, trgepc, frame, noCorr, center );

         //System.out.println( "trgsta = " + trgsta );

       
         //
         // Get the target state relative to the observer's position. For 
         // the output state, evaluate the body-fixed frame at an epoch
         // corrected for one-way light time between the surface point 
         // and the frame center, since that's what SPKEZR will do.
         //

         refloc   = "CENTER";

         sr1 = new StateRecord( trgsta, trgepc, center, frame,  et, 
                                frame,  refloc, abcorr, observer );

         // System.out.println( sr0);
         // System.out.println( sr1 );


         ok = JNITestutils.chckad ( "position",
                                    sr1.getPosition().toArray(),
                                    "~~/",
                                    sr0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    sr1.getVelocity().toArray(),
                                    "~~/",
                                    sr0.getVelocity().toArray(),
                                    TIGHT_TOL                 );

         ok = JNITestutils.chcksd ( "Light time",
                                    sr1.getLightTime().getMeasure(),
                                    "~/",
                                    sr0.getLightTime().getMeasure(),
                                    TIGHT_TOL                       );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SPKCVT analog constructor: find state of " +
                              "the \"Goldstone tracking station\" relative " +
                              "to the moon in the IAU_MOON frame."            );

         //
         // First compute the expected state `sr0.'
         //
         frame    = new ReferenceFrame      ( "IAU_EARTH" );         
         outFrame = new ReferenceFrame      ( "IAU_MOON"  );         
         abcorr   = new AberrationCorrection( "CN+S"      );
         observer = new Body( "Moon"  );
         target   = new Body( 399001  );
         center   = new Body( "Earth" );
         et       = new TDBTime ( "2000 JAN 1 1:00:00.000 TDB" );

         sr0      = new StateRecord( target, et, outFrame, abcorr, observer );


         //
         // Get the target's body-fixed state vector at the target epoch.
         //
         trgepc   = et.sub ( sr0.getLightTime() );

         trgsta   = new StateVector( target, trgepc, frame, noCorr, center );

         //System.out.println( "trgsta = " + trgsta );

       
         //
         // Get the target state relative to the observer's position. For 
         // the output state, evaluate the body-fixed frame at an epoch
         // corrected for one-way light time between the surface point 
         // and the frame center, since that's what SPKEZR will do.
         //

         refloc   = "OBSERVER";

         sr1 = new StateRecord( trgsta,   trgepc, center, frame,  et, 
                                outFrame, refloc, abcorr, observer );

         // System.out.println( sr0 );
         // System.out.println( sr1 );


         ok = JNITestutils.chckad ( "position",
                                    sr1.getPosition().toArray(),
                                    "~~/",
                                    sr0.getPosition().toArray(),
                                    TIGHT_TOL                  );


         ok = JNITestutils.chckad ( "velocity",
                                    sr1.getVelocity().toArray(),
                                    "~~/",
                                    sr0.getVelocity().toArray(),
                                    TIGHT_TOL                  );

         ok = JNITestutils.chcksd ( "Light time",
                                    sr1.getLightTime().getMeasure(),
                                    "~/",
                                    sr0.getLightTime().getMeasure(),
                                    TIGHT_TOL                       );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check toString."  );


         abcorr   = new AberrationCorrection( "NONE" );
         ref      = new ReferenceFrame( "J2000" );
         observer = new Body( "sun"   );
         target   = new Body( "alpha" );
         et0      = new TDBTime( "2000 Jan 1 12:05:00 TDB" );

         sr0      = new StateRecord( target,  et0, ref, abcorr, observer );

         outStr   = sr0.toString();

         v0       = sr0.toArray();

         xStr = String.format(

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

            v0[0], v0[1], v0[2],
            v0[3], v0[4], v0[5],
            sr0.getPosition().norm(),
            sr0.getVelocity().norm(),
            sr0.getLightTime().getMeasure()                           );

         // For debugging:
         //System.out.println( outStr );
         //System.out.println( xStr );


         ok = JNITestutils.chcksc( "outStr",
                                   outStr,
                                   "=",
                                   xStr      );


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
         CSPICE.spkuef( nathan );

         ( new File ( NATSPK ) ).delete();
 
         ( new File ( SPK ) ).delete();


      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

