
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.TimeConstants.*;


/**
Class TestTDTTime provides methods that implement test families for
the class TDTTime.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block

<h3>Version 1.0.0 21-DEC-2009 (NJB)</h3>
*/
public class TestTDTTime extends Object
{

   //
   // Class constants
   //


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test TDBTime and associated classes.
   */
   public static boolean f_TDTTime()

      throws SpiceException
   {
      //
      // Constants
      //
      final String                      TEST_CK   = "test.bc";
      final String                      TEST_SCLK = "test.tsc";

      final double                      TIGHT_TOL = 2.e-6;
      final double                      MED_TOL   = 2.e-4;

      final int                         CLKID     = -9;

      //
      // Local variables
      //
      Duration                          dur;

      JEDDuration                       JEDInc;
      JEDTime                           jed;

      SCLK                              clk;

      SCLKDuration                      SCLKInc;

      SCLKTime                          sclkTime;

      TDBDuration                       TDTDurationdiff;
      TDBDuration                       diff;
      TDBDuration                       TDBInc;

      TDTDuration                       TDTInc;

      TDBTime                           et;
      TDBTime                           TDTdiff;
      TDBTime                           TDTsum;

      TDTTime                           tdt;
      TDTTime                           tdt2;

      String                            timfmt;
      String                            timstr;
      String                            xString;

      boolean                           ok;

      double                            JEDDays;
      double                            offset;
      double                            sec;
      double                            sclkdp;
      double                            TDBSec;
      double                            xSec;

      int                               handle   = 0;
      int                               hashCode;
      int                               hashCode2;
      int                               i;
      int                               scid;


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

         JNITestutils.topen ( "f_TDTTime" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Clear the kernel database." );

         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();

         //
         // We defer loading an LSK until after the error checks.
         //



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************




         try
         {

            //
            // --------Case-----------------------------------------------
            //

            JNITestutils.tcase ( "Convert a time string without loaded LSK." );


            tdt = new TDTTime( "2009 NOV 2" );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(NOLEAPSECONDS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(NOLEAPSECONDS)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: create and load LSK; create " +
                              "SCLK kernel."                          );

         JNITestutils.tstlsk();

         //
         // If the test CK already exists, delete it.
         //
         ( new File( TEST_CK )).delete();

         //
         // Load the SCLK kernel but don't keep it.
         //
         handle = JNITestutils.tstck3( TEST_CK, TEST_SCLK, false, true, false );



         //
         // Constructor tests
         //



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test d.p. constructor." );


         xSec = 10.0;

         tdt  = new TDTTime( xSec );


         ok = JNITestutils.chcksd ( "sec", tdt.getTDTSeconds(),
                                    "~",   xSec,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test 'STR2ET' constructor." );


         xSec = TimeConstants.SPD + 10.0;

         tdt  = new TDTTime( "2000 JAN 2 12:00:10.000 TDT" );


         ok = JNITestutils.chcksd ( "sec", tdt.getTDTSeconds(),
                                    "~",   xSec,               TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );


         xSec = TimeConstants.SPD + 10.0;

         tdt  = new TDTTime( "2000 JAN 2 12:00:10.000 TDT" );

         tdt2 = new TDTTime( tdt );

         //
         // Change `tdt'; make sure tdt2 doesn't change.
         //
         tdt  = new TDTTime( -1.e6 );


         ok = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                    "~",    xSec,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: TDB input" );

         xSec   = TimeConstants.SPD + 10.0;

         tdt    = new TDTTime( "2000 JAN 2 12:00:10.000 TDT" );

         //
         // Create a TDBtime instance from `tdt'.
         //
         TDBSec = tdt.getTDBSeconds();

         et     = new TDBTime( TDBSec );

         //
         // Now create a TDTTime instance from `et'.
         //
         tdt2   = new TDTTime( et );

         //
         // We should be able to recover `tdt' from `tdt2'.
         //
         ok = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                    "~",   xSec,               TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: " +
                              "JEDTime input" );

         xSec   = TimeConstants.SPD + 10.0;

         tdt    = new TDTTime( "2000 JAN 2 12:00:10.000 TDT" );


         //
         // Create a JEDTime instance from `tdt'.
         //
         JEDDays = CSPICE.unitim( tdt.getTDTSeconds(), "TDT", "JED" );

         jed    = new JEDTime( JEDDays );

         //
         // Now create a TDTTime instance from `jed'.
         //
         tdt2   = new TDTTime( jed );

         //
         // We should be able to recover `tdt' from `tdt2'. Use a coarse
         // tolerance, since we've lost precision due to use of JED.
         //
         // Note: this assertion can be tested by using tdt instead of
         // tdt2 in the test below.
         //
         ok = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                    "~",    xSec,               MED_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: " +
                              "SCLKTime input" );

         xSec   = TimeConstants.SPD + 10.0;

         tdt    = new TDTTime( "2000 JAN 2 12:00:10.000 TDT" );

         //
         // Create a SCLKTime instance from `tdt'.
         //
         sclkdp   = CSPICE.sce2c( CLKID, tdt.getTDBSeconds() );

         clk      = new SCLK( CLKID );

         //
         // Create an SCLKTime instance from ticks.
         //
         sclkTime = new SCLKTime( clk, sclkdp );

         //
         // Now create a TDTTime instance from `sclkTime'.
         //
         tdt2     = new TDTTime( sclkTime );

         //
         // We should be able to recover `tdt' from `tdt2'.
         //
         ok = JNITestutils.chcksd ( "tdt2", tdt.getTDTSeconds(),
                                    "~",   xSec,               TIGHT_TOL );



         //
         // Method tests
         //


         //
         // String formatting tests:
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Recover time string using TIMOUT-style " +
                             "format." );


         xString = "2009 NOV 03 00:01:00.123456 TDT";

         tdt     = new TDTTime( xString );

         timfmt  = "YYYY MON DD HR:MN:SC.###### TDT ::TDT ::RND";

         timstr  = tdt.toString( timfmt );

         JNITestutils.chcksc( "timstr", timstr, "=", xString );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Recover time string using default format." );

         //
         // Note: expected time system of output string is TDB.
         //

         xString = "2009 NOV 03 00:01:00.123456 TDT";

         tdt     = new TDTTime( "2009 NOV 03 00:01:00.123456 TDT" );

         timstr  = tdt.toString();

         JNITestutils.chcksc( "timstr", timstr, "=", xString );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Recover time string using ET2UTC format." );


         xString = "2009 NOV 03 00:01:00.1235";

         et      = new TDBTime( "2009 NOV 03 00:01:00.12347" );

         timstr  = et.toUTCString( "C", 4 );

         JNITestutils.chcksc( "timstr", timstr, "=", xString );




         //
         // Support for equality comparison:
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test equality of two distinct but " +
                              "equal-valued TDTTimes." );

         tdt    = new TDTTime( "2009 JAN 2 12:00:10.000 TDT" );
         tdt2   = new TDTTime( "2009 JAN 2 12:00:10.000 TDT" );


         ok     = JNITestutils.chcksl ( "tdt == tdt2", tdt.equals( tdt2 ),
                                                                        true );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test inequality of two distinct TDTTimes." );

         tdt    = new TDTTime( "2009 JAN 2 12:00:10.000 TDT" );
         tdt2   = new TDTTime( "2009 JAN 2 12:00:20.000 TDT" );


         ok     = JNITestutils.chcksl ( "tdt == tdt2", tdt.equals( tdt2 ),
                                                                       false );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test hashcodes of two distinct but " +
                              "equal-valued TDBTimes." );

         tdt   = new TDTTime( "2009 JAN 2 12:00:10.000 TDT" );
         tdt2  = new TDTTime( "2009 JAN 2 12:00:10.000 TDT" );

         hashCode  = tdt.hashCode();
         hashCode2 = tdt2.hashCode();

         ok     = JNITestutils.chcksi ( "hashcode", hashCode, "=",
                                        hashCode2,  0              );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test hashcodes of two distinct, unequal " +
                              "TDTTimes." );

         tdt    = new TDTTime( "2009 JAN 2 12:00:10.000 TDT" );
         tdt2   = new TDTTime( "2009 JAN 2 12:00:20.000 TDT" );

         hashCode  = tdt.hashCode();
         hashCode2 = tdt2.hashCode();


         ok     = JNITestutils.chcksi ( "hashcode match",
                                        hashCode-hashCode2,
                                        "!=",
                                        0,
                                        0                  );



         //
         // TDTTime arithmetic: subtraction
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract one TDTTime from another." );

         tdt    = new TDTTime( "2009 JAN 2 12:00:00.000 TDT" );
         tdt2   = new TDTTime( "2009 JAN 2 12:00:10.000 TDT" );

         TDTInc = tdt.sub( tdt2 );

         xSec   = -10.0;


         ok     = JNITestutils.chcksd ( "difference", TDTInc.getMeasure(),
                                        "~",   xSec,  TIGHT_TOL            );


         //
         // Duration arithmetic: addition
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a TDTDuration to a TDTTime." );

         offset = 10.0;

         TDTInc = new TDTDuration( offset );

         tdt    = new TDTTime( "2009 JAN 2 12:00:00.000 TDT" );

         tdt2   = tdt.add( TDTInc );

         xSec   = tdt.getTDTSeconds() + offset;

         ok     = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                        "~",    xSec,             TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a TDBDuration to a TDTTime." );

         offset = 10.0;

         TDBInc = new TDBDuration( offset );

         tdt    = new TDTTime( "2009 JAN 2 12:00:00.000 TDT" );

         tdt2   = tdt.add( TDBInc );

         //
         // Compute the expected offset TDB time.
         //
         TDBSec = tdt.getTDBSeconds() + offset;

         //
         // Compute the expected offset TDB time.
         //
         xSec   = CSPICE.unitim( TDBSec, "TDB", "TDT" );


         ok     = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                        "~",    xSec,             TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a JEDDuration to a TDTTime." );

         offset = 10.0;

         JEDInc = new JEDDuration( offset );

         tdt    = new TDTTime( "2009 JAN 2 12:00:00.000 TDT" );

         tdt2   = tdt.add( JEDInc );


         //
         // Compute the expected offset TDT time.
         //
         // This is a bit complicated, but we want to avoid using JED as
         // an intermediate step, so as to preserve precision.
         //
         xSec   = CSPICE.unitim( tdt.getTDBSeconds() + SPD*offset, "TDB",
                                                                       "TDT" );


         ok     = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                        "~",    xSec,              TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a SCLKDuration to a TDTTime." );

         offset  = 10.0;

         clk     = new SCLK( CLKID );

         SCLKInc = new SCLKDuration( clk, offset );

         tdt     = new TDTTime( "2009 JAN 2 12:00:00.000 TDT" );

         tdt2    = tdt.add( SCLKInc );

         //
         // Compute the expected offset SCLK time.
         //
         sclkdp = CSPICE.sce2c( CLKID, tdt.getTDBSeconds() ) + offset;

         //
         // Compute the expected offset TDT time.
         //
         xSec   = CSPICE.unitim( CSPICE.sct2e(CLKID, sclkdp),  "TDB",  "TDT" );


         ok     = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                        "~",    xSec,              TIGHT_TOL );




         //
         // Duration arithmetic: subtraction
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDTDuration from a TDTTime." );

         offset = 10.0;

         TDTInc = new TDTDuration( offset );

         tdt    = new TDTTime( "2009 JAN 2 12:00:00.000 TDT" );

         tdt2   = tdt.sub( TDTInc );

         xSec   = tdt.getTDTSeconds() - offset;

         ok     = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                        "~",    xSec,              TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDBDuration from a TDTTime." );

         offset = 10.0;

         TDBInc = new TDBDuration( offset );

         tdt    = new TDTTime( "2009 JAN 2 12:00:00.000 TDT" );

         tdt2   = tdt.sub( TDBInc );

         //
         // Compute the expected offset TDB time.
         //
         TDBSec = tdt.getTDBSeconds() - offset;

         //
         // Compute the expected offset TDT time.
         //
         xSec   = CSPICE.unitim( TDBSec, "TDB", "TDT" );


         ok     = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                        "~",    xSec,              TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a JEDDuration from a TDBTime." );

         offset = 10.0;

         JEDInc = new JEDDuration( offset );

         tdt    = new TDTTime( "2009 JAN 2 12:00:00.000 TDT" );

         tdt2   = tdt.sub( JEDInc );

         //
         // Compute the expected offset TDT time.
         //
         // This is a bit complicated, but we want to avoid using JED as
         // an intermediate step, so as to preserve precision.
         //
         xSec   = CSPICE.unitim( tdt.getTDBSeconds() - SPD*offset, "TDB",
                                                                       "TDT" );


         ok     = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                        "~",    xSec,              TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract an SCLKDuration from a TDBTime." );

         offset  = 10.0;

         clk     = new SCLK( CLKID );

         SCLKInc = new SCLKDuration( clk, offset );

         tdt     = new TDTTime( "2009 JAN 2 12:00:00.000 TDT" );

         tdt2    = tdt.sub( SCLKInc );

         //
         // Compute the expected offset SCLK time.
         //
         sclkdp  = CSPICE.sce2c( CLKID, tdt.getTDBSeconds() ) - offset;

         //
         // Compute the expected offset TDB time.
         //
         xSec    = CSPICE.unitim(  CSPICE.sct2e( CLKID, sclkdp ),  "TDB",
                                                                       "TDT" );


         ok     = JNITestutils.chcksd ( "tdt2", tdt2.getTDTSeconds(),
                                        "~",    xSec,              TIGHT_TOL );
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
         // Get rid of the CK file.
         //
         CSPICE.ckupf( handle );

         ( new File ( TEST_CK ) ).delete();


         //
         // Get rid of the SLCK file, if necessary.
         //
         ( new File ( TEST_SCLK ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

