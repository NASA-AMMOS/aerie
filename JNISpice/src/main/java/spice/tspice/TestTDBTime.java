
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.TimeConstants.*;


/**
Class TestTDBTime provides methods that implement test families for
the class TDBTime.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 21-DEC-2009 (NJB)</h3>
*/
public class TestTDBTime extends Object
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
   public static boolean f_TDBTime()

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

      TDBDuration                       TDBDurationdiff;
      TDBDuration                       diff;
      TDBDuration                       TDBInc;

      TDTDuration                       TDTInc;

      TDBTime                           et;
      TDBTime                           et2;
      TDBTime                           TDBdiff;
      TDBTime                           TDBsum;

      TDTTime                           tdt;

      String                            timfmt;
      String                            timstr;
      String                            xString;

      boolean                           ok;

      double                            JEDDays;
      double                            offset;
      double                            sec;
      double                            sclkdp;
      double                            TDTSec;
      double                            xSec;

      int                               handle = 0;
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

         JNITestutils.topen ( "f_TDBTime" );


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


            et = new TDBTime( "2009 NOV 2" );


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

         et   = new TDBTime( xSec );


         ok = JNITestutils.chcksd ( "sec", et.getTDBSeconds(),
                                    "~",   xSec,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test 'STR2ET' constructor." );


         xSec = TimeConstants.SPD + 10.0;

         et   = new TDBTime( "2000 JAN 2 12:00:10.000 TDB" );


         ok = JNITestutils.chcksd ( "sec", et.getTDBSeconds(),
                                    "~",   xSec,               TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );


         xSec = TimeConstants.SPD + 10.0;

         et   = new TDBTime( "2000 JAN 2 12:00:10.000 TDB" );

         et2  = new TDBTime( et );

         //
         // Change `et'; make sure et2 doesn't change.
         //
         et   = new TDBTime( -1.e6 );


         ok = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                    "~",   xSec,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: TDT input" );

         xSec   = TimeConstants.SPD + 10.0;

         et     = new TDBTime( "2000 JAN 2 12:00:10.000 TDB" );

         //
         // Create a TDTtime instance from `et'.
         //
         TDTSec = CSPICE.unitim( et.getTDBSeconds(), "TDB", "TDT" );

         tdt    = new TDTTime( TDTSec );

         //
         // Now create a TDBTime instance from `tdt'.
         //
         et2    = new TDBTime( tdt );

         //
         // We should be able to recover `et' from `et2'.
         //
         ok = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                    "~",   xSec,               TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: " +
                              "JEDTime input" );

         xSec   = TimeConstants.SPD + 10.0;

         et     = new TDBTime( "2000 JAN 2 12:00:10.000 TDB" );

         //
         // Create a JEDTime instance from `et'.
         //
         JEDDays = CSPICE.unitim( et.getTDBSeconds(), "TDB", "JED" );

         jed    = new JEDTime( JEDDays );

         //
         // Now create a TDBTime instance from `jed'.
         //
         et2    = new TDBTime( jed );

         //
         // We should be able to recover `et' from `et2'. Use a coarse
         // tolerance, since we've lost precision due to use of JED.
         //
         ok = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                    "~",   xSec,               MED_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: " +
                              "SCLKTime input" );

         xSec   = TimeConstants.SPD + 10.0;

         et     = new TDBTime( "2000 JAN 2 12:00:10.000 TDB" );

         //
         // Create a SCLKTime instance from `et'.
         //
         sclkdp   = CSPICE.sce2c( CLKID, et.getTDBSeconds() );

         clk      = new SCLK( CLKID );

         //
         // Create an SCLKTime instance from ticks.
         //
         sclkTime = new SCLKTime( clk, sclkdp );

         //
         // Now create a TDBTime instance from `sclkTime'.
         //
         et2    = new TDBTime( sclkTime );

         //
         // We should be able to recover `et' from `et2'.
         //
         ok = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
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
         JNITestutils.tcase( "Recover time string using TIMOUT-style format." );


         xString = "2009 NOV 03 00:01:00.123456 TDB";

         et      = new TDBTime( xString );

         timfmt  = "YYYY MON DD HR:MN:SC.###### TDB ::TDB ::RND";

         timstr  = et.toString( timfmt );

         JNITestutils.chcksc( "timstr", timstr, "=", xString );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Recover time string using default format." );


         xString = "2009 NOV 03 00:01:00.123456 TDB";

         et      = new TDBTime( "2009 NOV 03 00:01:00.123456 TDB" );

         timstr  = et.toString();

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
                              "equal-valued TDBTimes." );

         et     = new TDBTime( "2009 JAN 2 12:00:10.000 TDB" );
         et2    = new TDBTime( "2009 JAN 2 12:00:10.000 TDB" );


         ok     = JNITestutils.chcksl ( "et == et2", et.equals( et2 ),  true );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test inequality of two distinct TDBTimes." );

         et     = new TDBTime( "2009 JAN 2 12:00:10.000 TDB" );
         et2    = new TDBTime( "2009 JAN 2 12:00:20.000 TDB" );


         ok     = JNITestutils.chcksl ( "et == et2", et.equals( et2 ), false );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test hashcodes of two distinct but " +
                              "equal-valued TDBTimes." );

         et     = new TDBTime( "2009 JAN 2 12:00:10.000 TDB" );
         et2    = new TDBTime( "2009 JAN 2 12:00:10.000 TDB" );

         hashCode  = et.hashCode();
         hashCode2 = et2.hashCode();

         ok     = JNITestutils.chcksi ( "hashcode", hashCode, "=",
                                        hashCode2,  0              );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test hashcodes of two distinct, unequal " +
                              "TDBTimes." );

         et     = new TDBTime( "2009 JAN 2 12:00:10.000 TDB" );
         et2    = new TDBTime( "2009 JAN 2 12:00:20.000 TDB" );

         hashCode  = et.hashCode();
         hashCode2 = et2.hashCode();


         ok     = JNITestutils.chcksi ( "hashcode match",
                                        hashCode-hashCode2,
                                        "!=",
                                        0,
                                        0                  );



         //
         // TDBTime arithmetic: subtraction
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract one TDBTime from another." );

         et     = new TDBTime( "2009 JAN 2 12:00:00.000 TDB" );
         et2    = new TDBTime( "2009 JAN 2 12:00:10.000 TDB" );

         TDBInc = et.sub( et2 );

         xSec   = -10.0;


         ok     = JNITestutils.chcksd ( "difference", TDBInc.getMeasure(),
                                        "~",   xSec,  TIGHT_TOL            );


         //
         // Duration arithmetic: addition
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a TDBDuration to a TDBTime." );

         offset = 10.0;

         TDBInc = new TDBDuration( offset );

         et     = new TDBTime( "2009 JAN 2 12:00:00.000 TDB" );

         et2    = et.add( TDBInc );

         xSec   = et.getTDBSeconds() + offset;

         ok     = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                        "~",   xSec,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a TDTDuration to a TDBTime." );

         offset = 10.0;

         TDTInc = new TDTDuration( offset );

         et     = new TDBTime( "2009 JAN 2 12:00:00.000 TDB" );

         et2    = et.add( TDBInc );

         //
         // Compute the expected offset TDT time.
         //
         TDTSec = CSPICE.unitim( et.getTDBSeconds(), "TDB", "TDT" ) + offset;

         //
         // Compute the expected offset TDB time.
         //
         xSec   = CSPICE.unitim( TDTSec, "TDT", "TDB" );


         ok     = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                        "~",   xSec,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a JEDDuration to a TDBTime." );

         offset = 10.0;

         JEDInc = new JEDDuration( offset );

         et     = new TDBTime( "2009 JAN 2 12:00:00.000 TDB" );

         et2    = et.add( JEDInc );


         //
         // Compute the expected offset TDB time.
         //
         // This is a bit complicated, but we want to avoid using JED as
         // an intermediate step, so as to preserve precision.
         //
         xSec   = et.getTDBSeconds() + SPD*offset;


         ok     = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                        "~",   xSec,               TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a SCLKDuration to a TDBTime." );

         offset = 10.0;

         clk     = new SCLK( CLKID );

         SCLKInc = new SCLKDuration( clk, offset );

         et     = new TDBTime( "2009 JAN 2 12:00:00.000 TDB" );

         et2    = et.add( SCLKInc );

         //
         // Compute the expected offset SCLK time.
         //
         sclkdp = CSPICE.sce2c( CLKID, et.getTDBSeconds() ) + offset;

         //
         // Compute the expected offset TDB time.
         //
         xSec   = CSPICE.sct2e( CLKID, sclkdp );


         ok     = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                        "~",   xSec,               TIGHT_TOL );




         //
         // Duration arithmetic: subtraction
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDBDuration from a TDBTime." );

         offset = 10.0;

         TDBInc = new TDBDuration( offset );

         et     = new TDBTime( "2009 JAN 2 12:00:00.000 TDB" );

         et2    = et.sub( TDBInc );

         xSec   = et.getTDBSeconds() - offset;

         ok     = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                        "~",   xSec,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDTDuration from a TDBTime." );

         offset = 10.0;

         TDTInc = new TDTDuration( offset );

         et     = new TDBTime( "2009 JAN 2 12:00:00.000 TDB" );

         et2    = et.sub( TDBInc );

         //
         // Compute the expected offset TDT time.
         //
         TDTSec = CSPICE.unitim( et.getTDBSeconds(), "TDB", "TDT" ) - offset;

         //
         // Compute the expected offset TDB time.
         //
         xSec   = CSPICE.unitim( TDTSec, "TDT", "TDB" );


         ok     = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                        "~",   xSec,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a JEDDuration from a TDBTime." );

         offset = 10.0;

         JEDInc = new JEDDuration( offset );

         et     = new TDBTime( "2009 JAN 2 12:00:00.000 TDB" );

         et2    = et.sub( JEDInc );

         //
         // Compute the expected offset JED time.
         //
         JEDDays = CSPICE.unitim( et.getTDBSeconds(), "TDB", "JED" ) - offset;

         //
         // Compute the expected offset TDB time.
         //
         xSec   = CSPICE.unitim( JEDDays, "JED", "TDB" );


         ok     = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                        "~",   xSec,               TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a SCLKDuration from a TDBTime." );

         offset  = 10.0;

         clk     = new SCLK( CLKID );

         SCLKInc = new SCLKDuration( clk, offset );

         et      = new TDBTime( "2009 JAN 2 12:00:00.000 TDB" );

         et2     = et.sub( SCLKInc );

         //
         // Compute the expected offset SCLK time.
         //
         sclkdp  = CSPICE.sce2c( CLKID, et.getTDBSeconds() ) - offset;

         //
         // Compute the expected offset TDB time.
         //
         xSec    = CSPICE.sct2e( CLKID, sclkdp );


         ok     = JNITestutils.chcksd ( "et2", et2.getTDBSeconds(),
                                        "~",   xSec,               TIGHT_TOL );
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

