
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.*;


/**
Class TestLocalSolarTime provides methods that implement test families for
the class LocalSolarTime.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 18-DEC-2009 (NJB)</h3>
*/
public class TestLocalSolarTime extends Object
{

   //
   // Class constants
   //
   private static String  NATPCK        = "nat.tpc";
   private static String  NATSPK        = "nat.bsp";
   private static String  PCK           = "lst.tpc";
   private static String  SPK           = "lst.bsp";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test LocalSolarTime and associated classes.
   */
   public static boolean f_LocalSolarTime()

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

      Body                              center;
      Body                              Earth  = new Body( "Earth" );
      Body                              Sun    = new Body( "Sun" );

      LatitudinalCoordinates            latCoords;

      LocalSolarTime                    lst0;
      LocalSolarTime                    lst1;

      PositionRecord                    pr;

      ReferenceFrame                    ref;

      String                            longitudeType;
      String                            lstString0;
      String                            lstString1;

      TDBTime                           epoch;


      boolean                           ok;

      double                            frac;
      double                            longitude;
      double                            lon;
      double                            sec;

      int                               handle = 0;
      int                               i;
      int                               nathan = 0;
      int                               xHour;
      int                               xMinute;
      int                               xSecond;

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

         JNITestutils.topen ( "f_LocalSolarTime" );


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
         // Same for Nat's solar system PCK.
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

         nathan = JNITestutils.natspk( NATSPK, true );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //


         JNITestutils.tcase (  "Error: invalid longitude type." );

         try
         {
            epoch         = new TDBTime( "2000 jan 1 12:00:00 TDB" );
            center        = new Body( "Earth" );
            longitude     = 0.0;
            longitudeType = "cylindrical";

            lst0          = new LocalSolarTime( epoch,     center,
                                                longitude, longitudeType );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(UNKNOWNSYSTEM)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(UNKNOWNSYSTEM)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Find the LST on earth at UTC 2000 jan 1 " +
                              "18:00:00 UTC, on the 15 deg east meridian." );

         epoch         = new TDBTime( "2000 jan 1 18:00:00 UTC" );
         center        = new Body( "Earth" );
         longitude     = 15.0 * AngularUnits.RPD;
         longitudeType = LocalSolarTime.PLANETOCENTRIC;

         lst0          = new LocalSolarTime( epoch,     center,
                                             longitude, longitudeType );

         lstString0    = lst0.getLocalSolarTime24Hr();


         ok = JNITestutils.chcksc ( "24 hour string",
                                    lstString0,
                                    "=",
                                    "18:56:14"      );

         lstString1    = lst0.getLocalSolarTime12Hr();

         ok = JNITestutils.chcksc ( "12 hour string",
                                    lstString1,
                                    "=",
                                    "06:56:14 P.M."      );


         ok = JNITestutils.chcksi ( "Hours",
                                    lst0.getHour(),
                                    "=",
                                    18,
                                    0               );

         ok = JNITestutils.chcksi ( "Minutes",
                                    lst0.getMinute(),
                                    "=",
                                    56,
                                    0               );

         ok = JNITestutils.chcksi ( "Seconds",
                                    lst0.getSecond(),
                                    "=",
                                    14,
                                    0               );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Continue previous case: check results " +
                              "against solar position."                   );

         epoch     = new TDBTime( "2000 jan 1 18:00:00 UTC" );
         abcorr    = new AberrationCorrection( "LT+S" );
         ref       = new ReferenceFrame( "IAU_EARTH" );

         pr        = new PositionRecord( Sun, epoch, ref, abcorr, Earth );

         latCoords = new LatitudinalCoordinates( pr );

         lon       = latCoords.getLongitude();

         frac      = 12.0 + 24*( longitude - lon )/ (2 * Math.PI);


         xHour     = (int)Math.floor( frac );


         ok = JNITestutils.chcksi ( "Hours",
                                    lst0.getHour(),
                                    "=",
                                    xHour,
                                    0               );

         xMinute   = (int)Math.floor( ( frac - xHour ) * 60 );


         ok = JNITestutils.chcksi ( "Minutes",
                                    lst0.getMinute(),
                                    "=",
                                    xMinute,
                                    0               );


         xSecond   = (int)Math.floor( 3600*((frac - xHour) - xMinute/60.0) );


         ok = JNITestutils.chcksi ( "Seconds",
                                    lst0.getSecond(),
                                    "=",
                                    xSecond ,
                                    0               );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Find the LST on earth at UTC 2000 jan 1 " +
                              "18:00:00 UTC, on the 15 deg east meridian. " +
                              "Use planetographic coordinates."               );

         epoch         = new TDBTime( "2000 jan 1 18:00:00 UTC" );
         center        = new Body( "Earth" );
         longitude     = 15.0 * AngularUnits.RPD;
         longitudeType = LocalSolarTime.PLANETOGRAPHIC;

         lst0          = new LocalSolarTime( epoch,     center,
                                             longitude, longitudeType );

         lstString0    = lst0.getLocalSolarTime24Hr();


         ok = JNITestutils.chcksc ( "24 hour string",
                                    lstString0,
                                    "=",
                                    "18:56:14"      );

         lstString1    = lst0.getLocalSolarTime12Hr();

         ok = JNITestutils.chcksc ( "12 hour string",
                                    lstString1,
                                    "=",
                                    "06:56:14 P.M."      );


         ok = JNITestutils.chcksi ( "Hours",
                                    lst0.getHour(),
                                    "=",
                                    18,
                                    0               );

         ok = JNITestutils.chcksi ( "Minutes",
                                    lst0.getMinute(),
                                    "=",
                                    56,
                                    0               );

         ok = JNITestutils.chcksi ( "Seconds",
                                    lst0.getSecond(),
                                    "=",
                                    14,
                                    0               );
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

         CSPICE.spkuef( nathan );

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

