
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestVelocityVector provides methods that implement test families for
the class VelocityVector.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 28-NOV-2009 (NJB)</h3>
*/
public class TestVelocityVector extends Object
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
   Test VelocityVector and associated classes.
   */
   public static boolean f_VelocityVector()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final String                      endl      = System.getProperty(
                                                            "line.separator" );

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              Alpha  = new Body( "Alpha" );
      Body                              Beta   = new Body( "Beta"  );
      Body                              observer;
      Body                              target;

      VelocityVector                    v0;
      VelocityVector                    v1;
      VelocityVector                    vel;

      ReferenceFrame                    frame;

      StateRecord                       sr;

      String                            displayStr;
      String                            timstr;
      String                            xStr;

      TDBTime                           et;

      Vector3                           vec0    = new Vector3( 1.0, 2.0, 3.0 );
      Vector3                           zeroVec = new Vector3();

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

         JNITestutils.topen ( "f_VelocityVector" );


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

            vel         = new VelocityVector( target, et, frame, abcorr,
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

         vel = new VelocityVector();

         ok = JNITestutils.chckad ( "vel",
                                    vel.toArray(),
                                    "~~",
                                    zeroVec.toArray(),
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test Vector3-based constructor." );

         vel = new VelocityVector( vec0 );

         ok = JNITestutils.chckad ( "vel",
                                    vel.toArray(),
                                    "~~",
                                    vec0.toArray(),
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor." );

         v0  = new VelocityVector( vec0 );

         vel = new VelocityVector( v0 );

         ok = JNITestutils.chckad ( "vel",
                                    vel.toArray(),
                                    "~~",
                                    vec0.toArray(),
                                    TIGHT_TOL         );

         //
         // Make sure that changing v0 doesn't affect vel.
         //
         v0 = new VelocityVector(  vec0.scale(2.0)  );

         v1 = new VelocityVector( vec0 );

         ok = JNITestutils.chckad ( "vel",
                                    vel.toArray(),
                                    "~~",
                                    v1.toArray(),
                                    TIGHT_TOL         );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test principal constructor: find velocity of " +
                              "Moon relative to Earth; compare to " +
                              "StateRecord."                                 );


         frame    = new ReferenceFrame      ( "J2000" );
         abcorr   = new AberrationCorrection( "LT+S"  );
         observer = new Body( "Earth" );
         target   = new Body( "Moon"  );
         et       = new TDBTime ( "2009 NOV 11 18:00" );

         vel      = new VelocityVector( target, et, frame, abcorr, observer );
         sr       = new StateRecord   ( target, et, frame, abcorr, observer );

         ok = JNITestutils.chckad ( "vel",
                                    vel.toArray(),
                                    "~~/",
                                    sr.getVelocity().toArray(),
                                    TIGHT_TOL                   );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test principal constructor: find velocities " +
                              "of  Alpha and Beta relative to Sun at " +
                              "occultation midpoint time."                   );


         frame    = new ReferenceFrame      ( "J2000" );
         abcorr   = new AberrationCorrection( "LT+S"  );
         observer = new Body( "Sun" );
         et       = new TDBTime ( "2000 JAN 1 12:05:01.000 TDB" );

         v0      = new VelocityVector( Alpha, et, frame, abcorr, observer );
         v1      = new VelocityVector( Beta,  et, frame, abcorr, observer );

         //
         // Find the angular separation between v0 and v1.
         //
         sep     = v0.sep( v1 );

         ok = JNITestutils.chcksd ( "sep",
                                    sep,
                                    "~",
                                    0.0,
                                    TIGHT_TOL                   );

         /*

         For debugging:

         System.out.println( "v0: " + endl + v0 + endl + "v1: "  + v1 );

         double scale = v0.norm() /  v1.norm();

         VelocityVector v3 = new VelocityVector(  v1.scale( scale )  );

         System.out.println( "v0: " + endl + v0 + endl + "v3: "  + v3 );
         */



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString method." );



         v0         = new VelocityVector( new Vector3( 3.0, 4.0, 12.0 )  );

         displayStr = v0.toString();

         // For debugging:
         //System.out.println( displayStr );

         xStr       = endl +
                    "Velocity vector = " + endl + endl +
                    "    VX:   3.0000000000000000e+00 (km/s)"   + endl +
                    "    VY:   4.0000000000000000e+00 (km/s)"   + endl +
                    "    VZ:   1.2000000000000000e+01 (km/s)"   + endl + endl +
                    "Speed =   1.3000000000000000e+01 (km/s)"   + endl;

         // For debugging:
         //System.out.println( xStr );


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

