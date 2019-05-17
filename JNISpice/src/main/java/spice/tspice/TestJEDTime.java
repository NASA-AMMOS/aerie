
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.TimeConstants.*;


/**
Class TestJEDTime provides methods that implement test families for
the class JEDTime.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 07-DEC-2009 (NJB)</h3>
*/
public class TestJEDTime extends Object
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
   Test JEDTime and associated classes.
   */
   public static boolean f_JEDTime()

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

      JEDTime                           jed0;
      JEDTime                           jed1;
      JEDTime                           jed2;

      SCLK                              clk;

      SCLKDuration                      SCLKInc;

      SCLKTime                          sclkTime;
      SCLKTime                          sclkTime0;
      SCLKTime                          sclkTime1;

      /*
      TDBDuration                       TDBDurationdiff;
      TDBDuration                       diff;
      */


      TDBDuration                       TDBInc;

      TDTDuration                       TDTInc;


      TDBTime                           et;
      TDBTime                           et2;
      TDBTime                           TDBdiff;
      TDBTime                           TDBsum;

      TDTTime                           tdt0;
      TDTTime                           tdt1;

      String                            timfmt;
      String                            timstr;
      String                            xString;

      boolean                           ok;

      double                            JEDDays;
      double                            offset;
      double                            sec;
      double                            sclkdp;
      double                            TDTSec;
      double                            xDays;
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

         JNITestutils.topen ( "f_JEDTime" );


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


            jed0 = new JEDTime( new TDBTime("2009 NOV 2") );


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


         xDays = 10.0;

         jed0  = new JEDTime( xDays );


         ok = JNITestutils.chcksd ( "days", jed0.getDays(),
                                    "~",    xDays,       TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );


         xDays = 10.0;

         jed0 = new JEDTime( xDays );
         jed1 = new JEDTime( xDays );

         jed2 = new JEDTime( jed0 );

         //
         // Change jed0; make sure jed2 doesn't change.
         //
         jed0 = new JEDTime( 55.0 );


         ok = JNITestutils.chcksd ( "jed2", jed2.getDays(),
                                    "~",    xDays,          TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: TDB input" );

         xSec   = TimeConstants.SPD + 10.0;

         et     = new TDBTime( xSec );

         //
         // Create a JED instance from `xDays'.
         //
         xDays = CSPICE.unitim( et.getTDBSeconds(), "TDB", "JED" );

         jed0  = new JEDTime( xDays );

         //
         // Now create a JEDTime instance from `et'.
         //
         jed1  = new JEDTime( et );

         //
         // We should be able to recover `xDays' from `jed1'.
         //
         ok = JNITestutils.chcksd ( "jed1 days", jed1.getDays(),
                                    "~",         xDays,        TIGHT_TOL );



         //
         // Test fetch methods.
         //

         //
         // --------Case-----------------------------------------------
         //


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getDays." );

         xDays =  2451555.0;

         jed0  =  new JEDTime( xDays );

         ok = JNITestutils.chcksd ( "days", jed0.getDays(),
                                    "~",    xDays,          TIGHT_TOL );

         JNITestutils.tcase ( "Test getTDBSeconds." );

         jed0  =  new JEDTime( 2451546.0 );

         xSec  =  TimeConstants.SPD;

         ok = JNITestutils.chcksd ( "sec", jed0.getTDBSeconds(),
                                    "~",   xSec,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: TDT input" );

         xSec   = TimeConstants.SPD + 10.0;

         et     = new TDBTime( xSec );

         tdt0   = new TDTTime( et );

         //
         // Create a JED instance from `xDays'.
         //
         xDays = CSPICE.unitim( et.getTDBSeconds(), "TDB", "JED" );

         jed0  = new JEDTime( xDays );

         //
         // Now create a JEDTime instance from `tdt'.
         //
         jed1  = new JEDTime( tdt0 );

         //
         // We should be able to recover `xDays' from `jed1'.
         //
         ok = JNITestutils.chcksd ( "jed1 days", jed1.getDays(),
                                    "~",         xDays,        TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: SCLKTime " +
                              "input" );

         xSec   = TimeConstants.SPD + 10.0;

         et     = new TDBTime( xSec );

         //
         // Create a SCLKTime instance from `et'.
         //
         clk      = new SCLK( CLKID );

         sclkTime = new SCLKTime( clk, et );


         //
         // Create a JEDTime instance from `xDays'.
         //
         xDays = CSPICE.unitim( et.getTDBSeconds(), "TDB", "JED" );

         jed0  = new JEDTime( xDays );

         //
         // Now create a JEDTime instance from `sclkTime'.
         //
         jed1  = new JEDTime( sclkTime );

         //
         // We should be able to recover `xDays' from `jed1'.
         //
         ok = JNITestutils.chcksd ( "jed1 days", jed1.getDays(),
                                    "~",         xDays,        TIGHT_TOL );




         //
         // --------Case-----------------------------------------------
         //

         //
         // The case of JED input is already handled by the copy constructor
         // test.
         //



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


         xString = "2009 NOV 03 00:01:00.1234 TDB";

         et      = new TDBTime( xString );

         jed0    = new JEDTime( et );

         //
         // Note that we expect lower precision due to use of JED.
         //
         timfmt  = "YYYY MON DD HR:MN:SC.#### TDB ::TDB::RND";

         timstr  = jed0.toString( timfmt );

         JNITestutils.chcksc( "timstr", timstr, "=", xString );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Recover time string using ET2UTC-style " +
                             "format." );


         xString = "JD 2451546.1234";

         et      = new TDBTime( xString );

         jed0    = new JEDTime( et );

         //
         // Note that we expect lower precision due to use of JED.
         //
         timstr  = jed0.toUTCString( "J", 4 );

         JNITestutils.chcksc( "timstr", timstr, "=", xString );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Recover time string using default format." );


         xString = "2451546.000000000 (JED)";

         et      = new TDBTime( "2000 Jan 2 12:00 TDB" );

         jed0    = new JEDTime( et );


         timstr  = jed0.toString();

         JNITestutils.chcksc( "timstr", timstr, "=", xString );



         //
         // Support for equality comparison:
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test equality of two distinct but " +
                              "equal-valued JED Times." );

         jed0 = new JEDTime( 10.0 );
         jed1 = new JEDTime( 10.0 );

         ok     = JNITestutils.chcksl ( "jed0 == jed1", jed0.equals(jed1),
                                                                        true );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test inequality of two distinct JEDTimes." );

         jed0 = new JEDTime( 10.0 );
         jed1 = new JEDTime( 20.0 );

         ok     = JNITestutils.chcksl ( "jed0 == jed1", jed0.equals(jed1),
                                                                       false );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test hashcodes of two distinct but " +
                              "equal-valued JEDTimes." );

         jed0 = new JEDTime( 10.0 );
         jed1 = new JEDTime( 10.0 );

         hashCode  = jed0.hashCode();
         hashCode2 = jed1.hashCode();

         ok     = JNITestutils.chcksi ( "hashcode", hashCode, "=",
                                        hashCode2,  0              );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test hashcodes of two distinct, unequal " +
                              "JEDTimes."                                  );

         jed0 = new JEDTime( 10.0 );
         jed1 = new JEDTime( 20.0 );

         hashCode  = jed0.hashCode();
         hashCode2 = jed1.hashCode();

         ok     = JNITestutils.chcksi ( "hashcode", hashCode, "!=",
                                        hashCode2,  0              );


         //
         // JEDTime arithmetic: subtraction
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract one JEDTime from another." );

         jed0 = new JEDTime( 10.0 );
         jed1 = new JEDTime( 20.0 );

         JEDInc = jed0.sub( jed1 );

         xDays  = -10.0;


         ok     = JNITestutils.chcksd ( "difference", JEDInc.getMeasure(),
                                        "~",   xDays,  TIGHT_TOL            );




         //
         // Duration arithmetic: addition
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a JEDDuration to a JEDTime." );

         offset = 10.0;

         JEDInc = new JEDDuration( offset );

         jed0   = new JEDTime( TimeConstants.J2000 + 25.0 );

         jed1   = jed0.add( JEDInc );

         xDays   = jed0.getDays() + offset;

         ok     = JNITestutils.chcksd ( "jed1", jed1.getDays(),
                                        "~",   xDays,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a TDBDuration to a JEDTime." );

         offset = 10.0 * TimeConstants.SPD;

         TDBInc = new TDBDuration( offset );

         jed0   = new JEDTime( TimeConstants.J2000 + 25.0 );

         jed1   = jed0.add( TDBInc );

         xDays   = jed0.getDays() + offset/TimeConstants.SPD;

         ok     = JNITestutils.chcksd ( "jed1", jed1.getDays(),
                                        "~",   xDays,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a TDTDuration to a JEDTime." );

         //
         // Start by creating a TDT offset from a known JED offset.
         //
         JEDInc  = new JEDDuration( 10.0 );

         jed0    = new JEDTime( TimeConstants.J2000 + 25.0 );
         jed1    = jed0.add( JEDInc );

         tdt0    = new TDTTime( jed0 );
         tdt1    = new TDTTime( jed1 );

         TDTInc  = tdt1.sub( tdt0 );

         jed2   = jed0.add( TDTInc );

         xDays   = jed0.getDays() + JEDInc.getMeasure();

         ok     = JNITestutils.chcksd ( "jed2", jed2.getDays(),
                                        "~",   xDays,               TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a SCLKDuration to a JEDTime." );


         //
         // Start by creating an SCLK offset from a known JED offset.
         //
         JEDInc  = new JEDDuration( 10.0 );

         jed0    = new JEDTime( TimeConstants.J2000 + 25.0 );
         jed1    = jed0.add( JEDInc );

         clk     = new SCLK( CLKID );

         sclkTime0 = new SCLKTime( clk, jed0 );
         sclkTime1 = new SCLKTime( clk, jed1 );

         SCLKInc   = sclkTime1.sub( sclkTime0 );

         jed2   = jed0.add( SCLKInc );

         xDays   = jed0.getDays() + JEDInc.getMeasure();

         ok     = JNITestutils.chcksd ( "jed2", jed2.getDays(),
                                        "~",    xDays,           TIGHT_TOL );




         //
         // Duration arithmetic: subtraction
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a JEDDuration from a JEDTime." );

         offset = 10.0;

         JEDInc = new JEDDuration( offset );

         jed0   = new JEDTime( TimeConstants.J2000 + 25.0 );

         jed1   = jed0.sub( JEDInc );

         xDays   = jed0.getDays() - offset;

         ok     = JNITestutils.chcksd ( "jed1", jed1.getDays(),
                                        "~",   xDays,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDBDuration from a JEDTime." );

         offset = 10.0 * TimeConstants.SPD;

         TDBInc = new TDBDuration( offset );

         jed0   = new JEDTime( TimeConstants.J2000 + 25.0 );

         jed1   = jed0.sub( TDBInc );

         xDays   = jed0.getDays() - offset/TimeConstants.SPD;

         ok     = JNITestutils.chcksd ( "jed1", jed1.getDays(),
                                        "~",   xDays,               TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDTDuration from a JEDTime." );

         //
         // Start by creating a TDT offset from a known JED offset.
         //
         JEDInc  = new JEDDuration( 10.0 );

         jed0    = new JEDTime( TimeConstants.J2000 + 25.0 );
         jed1    = jed0.add( JEDInc );

         tdt0    = new TDTTime( jed0 );
         tdt1    = new TDTTime( jed1 );

         TDTInc  = tdt1.sub( tdt0 );

         jed2   = jed0.sub( TDTInc );

         xDays   = jed0.getDays() - JEDInc.getMeasure();

         ok     = JNITestutils.chcksd ( "jed2", jed2.getDays(),
                                        "~",   xDays,               TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a SCLKDuration from a JEDTime." );


         //
         // Start by creating an SCLK offset from a known JED offset.
         //
         JEDInc  = new JEDDuration( 10.0 );

         jed0    = new JEDTime( TimeConstants.J2000 + 25.0 );
         jed1    = jed0.add( JEDInc );

         clk     = new SCLK( CLKID );

         sclkTime0 = new SCLKTime( clk, jed0 );
         sclkTime1 = new SCLKTime( clk, jed1 );

         SCLKInc   = sclkTime1.sub( sclkTime0 );

         jed2   = jed0.sub( SCLKInc );

         xDays   = jed0.getDays() - JEDInc.getMeasure();

         ok     = JNITestutils.chcksd ( "jed2", jed2.getDays(),
                                        "~",    xDays,           TIGHT_TOL );
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

