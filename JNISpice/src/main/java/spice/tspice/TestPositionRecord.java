
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestPositionRecord provides methods that implement test families for
the class PositionRecord.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 28-NOV-2009 (NJB)</h3>
*/
public class TestPositionRecord extends Object
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
   Test PositionRecord and associated classes.
   */
   public static boolean f_PositionRecord()

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

      Body                              Alpha  = new Body( "Alpha" );
      Body                              Beta   = new Body( "Beta"  );
      Body                              observer;
      Body                              target;

      PositionRecord                    p0;
      PositionRecord                    p1;
      PositionRecord                    pos;

      ReferenceFrame                    frame;

      StateRecord                       sr;

      String                            displayStr;
      String                            timstr;
      String                            xStr;

      TDBTime                           et;

      Vector3                           v0      = new Vector3( 1.0, 2.0, 3.0 );
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

         JNITestutils.topen ( "f_PositionRecord" );


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

            pos         = new PositionRecord( target, et, frame, abcorr,
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

         pos = new PositionRecord();

         ok = JNITestutils.chckad ( "pos",
                                    pos.toArray(),
                                    "~~",
                                    zeroVec.toArray(),
                                    TIGHT_TOL         );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor." );


         timstr      = "2009 NOV 18 00:00:00";

         target      = new Body( "Moon" );
         et          = new TDBTime( timstr );
         frame       = new ReferenceFrame( "J2000" );
         abcorr      = new AberrationCorrection( "LT+S" );
         observer    = new Body( "Earth" );

         p0          = new PositionRecord( target, et, frame, abcorr,
                                                                    observer );
         p1          = new PositionRecord( target, et, frame, abcorr,
                                                                    observer );


         pos = new PositionRecord( p0 );

         ok = JNITestutils.chckad ( "pos",
                                    pos.toArray(),
                                    "~~",
                                    p0.toArray(),
                                    TIGHT_TOL         );

         //
         // Make sure that changing p0 doesn't affect pos.
         //
         target = new Body( "Sun" );

         p0 = new PositionRecord( target, et, frame, abcorr, observer );


         ok = JNITestutils.chckad ( "pos",
                                    pos.toArray(),
                                    "~~",
                                    p1.toArray(),
                                    TIGHT_TOL         );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test principal constructor: find position of " +
                              "Moon relative to Earth; compare to " +
                              "StateRecord."                                 );


         frame    = new ReferenceFrame      ( "J2000" );
         abcorr   = new AberrationCorrection( "LT+S"  );
         observer = new Body( "Earth" );
         target   = new Body( "Moon"  );
         et       = new TDBTime ( "2009 NOV 11 18:00" );

         pos      = new PositionRecord( target, et, frame, abcorr, observer );
         sr       = new StateRecord   ( target, et, frame, abcorr, observer );

         ok = JNITestutils.chckad ( "pos",
                                    pos.toArray(),
                                    "~~/",
                                    sr.getPosition().toArray(),
                                    TIGHT_TOL                   );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test principal constructor: find positions " +
                              "of Alpha and Beta relative to Sun at " +
                              "occultation midpoint time."                   );


         frame    = new ReferenceFrame      ( "J2000" );
         abcorr   = new AberrationCorrection( "LT+S"  );
         observer = new Body( "Sun" );
         et       = new TDBTime ( "2000 JAN 1 12:05:01.000 TDB" );

         p0      = new PositionRecord( Alpha, et, frame, abcorr, observer );
         p1      = new PositionRecord( Beta,  et, frame, abcorr, observer );

         //
         // Find the angular separation between p0 and p1.
         //
         sep     = p0.sep( p1 );

         ok = JNITestutils.chcksd ( "sep",
                                    sep,
                                    "~",
                                    0.0,
                                    TIGHT_TOL                   );

         /*

         For debugging:

         System.out.println( "p0: " + endl + p0 + endl + "p1: "  + p1 );

         double scale = p0.norm() /  p1.norm();

         PositionRecord p3 = new PositionRecord(  p1.scale( scale )  );

         System.out.println( "p0: " + endl + p0 + endl + "p3: "  + p3 );
         */



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString method." );

         TDBDuration d = new TDBDuration( 10.0 );

         p0           = new PositionRecord( new Vector3( 3.0, 4.0, 12.0 ), d );

         displayStr = p0.toString();

         // For debugging:
         // System.out.println( displayStr );

         xStr       = endl +
                  "Position vector = " + endl + endl +
                  "    X:   3.0000000000000000e+00 (km)" + endl +
                  "    Y:   4.0000000000000000e+00 (km)" + endl +
                  "    Z:   1.2000000000000000e+01 (km)" + endl + endl +
                  "Distance           =   1.3000000000000000e+01 (km)" + endl +
                  "One way light time =   1.0000000000000000e+01 (s)"  + endl;

         // For debugging:
         // System.out.println( xStr );


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

