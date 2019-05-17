
package spice.tspice;

import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;



/**
Class TestSCLKTime provides methods that implement test families for
the class SCLKTime.

<h3> Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3> Version 1.0.0 24-DEC-2009 (NJB)</h3>
*/
public class TestSCLKTime extends Object
{


   public static boolean f_SCLKTime()

      throws SpiceException
   {
      /*
      Local constants
      */
      final String    CK        = "SCLKtest.bc";
      final String    SCLKKER   = "testsclk.tsc";
      final String    SCLKKER2  = "testsclk2.tsc";
      final String    SCLK_STR  = "1/315662457.1839";
      final String    UTC       = "1990 JAN 01 12:00:00";
      final String    TICK_STR  = "315662457.1839";

      final double    TIGHT_TOL = 1.e-12;

      final int       SC1       =  -9999;
      final int       SC2       =  -10000;
      final int       SC3       =  -10001;
      final int       CLK_ID    =  -9;
      final int       MXPART    =  50;

      /*
      Local variables
      */
      JEDDuration             jeddur0    = null;

      JEDTime                 jed0       = null;
      JEDTime                 jed1       = null;

      SCLK                    sclk       = new SCLK ( CLK_ID );
      SCLK                    sclk888    = new SCLK ( -888   );

      SCLKDuration            sclkInc;
      SCLKDuration            sclkInc2;

      SCLKTime                cclkdp     = null;
      SCLKTime                expsclkdp  = null;
      SCLKTime                expSCLK    = null;
      SCLKTime                sclkTime0  = null;
      SCLKTime                sclkTime1  = null;
      SCLKTime                sclkTime2  = null;
      SCLKTime                sclkTime3  = null;
      SCLKTime                sclkdp     = null;
      SCLKTime                ticks      = null;

      String                  label;
      String                  outStr;
      String                  sclkstr    = "";
      String                  shortMsg;
      String                  tickstr    = "";
      String                  utcout     = "";
      String                  xStr;

      StringTokenizer         tokenizer;

      TDBDuration             tdbdur0    = null;


      TDBTime                 et         = null;
      TDBTime                 et2        = null;
      TDBTime                 expet      = null;
      TDBTime                 tdb0       = null;
      TDBTime                 tdb1       = null;

      TDTDuration             tdtdur0    = null;

      TDTTime                 tdt0       = null;
      TDTTime                 tdt1       = null;

      boolean                 ok;

      double                  contTicks;
      double                  discreteTicks;
      double                  durTicks0;
      double                  ETSecPastJ2000;
      double                  expContTicks;
      double                  expETSecPastJ2000;
      double[]                pstart     = new double[MXPART];
      double[]                pstop      = new double[MXPART];
      double                  ticks0;
      double                  xDays;
      double                  xSec;
      double                  xTicks;
      double                  xVal;

      int                     count;
      int                     expRoundTicks;
      int                     handle = 0;
      int                     i;
      int                     j;
      int                     npart;
      int                     roundTicks;

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

         JNITestutils.topen ( "f_SCLKTime" );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Create an SCLK kernel. The routine "     +
                              "we use for this purpose also creates a " +
                              "C-kernel, which we don't need."            );

         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();


         //
         // Don't load the SCLK kernel yet.
         //
         handle = JNITestutils.tstck3 (  CK, SCLKKER, false, false, true );

         ( new File ( CK ) ).delete();



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

            JNITestutils.tcase ( "Convert a time string without loaded " +
                                 "SCLK kernel or LSK."                    );


            sclkTime0 = new SCLKTime( sclk, "1/00000.000" );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(KERNELVARNOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(KERNELVARNOTFOUND)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Load LSK and SCLK." );

         JNITestutils.tstlsk();
         KernelDatabase.load( SCLKKER );



         try
         {

            //
            // --------Case-----------------------------------------------
            //

            JNITestutils.tcase ( "Construct an SCLKTime from a " +
                                 "string with invalid format." );


            sclkTime0 = new SCLKTime( sclk, "1/00000:0:0:000" );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INVALIDSCLKSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(INVALIDSCLKSTRING)", ex );
         }


         try
         {

            //
            // --------Case-----------------------------------------------
            //
            JNITestutils.tcase ( "Construct an SCLKTime from a negative " +
                                 "tick value." );


            sclkTime0 = new SCLKTime( sclk, -1.0 );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INVALIDTICKS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INVALIDTICKS)", ex );
         }


         try
         {

            //
            // --------Case-----------------------------------------------
            //
            JNITestutils.tcase ( "Construct an SCLKTime from a TDBTime " +
                                 "preceding the SCLK start time."          );


            sclkTime0 = new SCLKTime( sclk, new TDBTime( "1200 Jan 1" ) );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(VALUEOUTOFRANGE)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create and load second SCLK kernel " +
                              "for future use."                    );

         double[] moduli  = { 100000000.0, 10000.0 };
         double[] offsets = {         0.0,     0.0 };
         double   rate    =  10.0;

         //
         // Note that the delimiter code is 2.
         //
         TestSCLKTime.createSCLK( SCLKKER2,
                                  true,
                                  true,
                                  -888,
                                  new TDBTime( "2000 jan 1 12:00 TDB" ),
                                  TimeSystem.TDB,
                                  2,
                                  moduli,
                                  offsets,
                                  rate                                  );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test d.p. constructor." );


         xTicks    = 10.0;

         sclkTime0 = new SCLKTime( sclk, xTicks );


         ok = JNITestutils.chcksd ( "ticks", sclkTime0.getContinuousTicks(),
                                    "~",     xTicks,        TIGHT_TOL );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test SCLK string constructor." );


         xTicks = 1239999.0;

         sclkTime0  = new SCLKTime( sclk888, "1/000123:9999" );


         ok = JNITestutils.chcksd ( "ticks", sclkTime0.getContinuousTicks(),
                                    "~",     xTicks,              TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );



         xTicks = 1239999.0;

         sclkTime0  = new SCLKTime( sclk888, "1/000123:9999" );
         sclkTime1  = new SCLKTime( sclk888, "1/000123:9999" );

         sclkTime2  = new SCLKTime( sclkTime0 );

         //
         // Change sclkTime0; make sure sclkTime2 doesn't change.
         //
         sclkTime0  = new SCLKTime( sclk, 1/0000.0001 );


         ok = JNITestutils.chcksd ( "sclkTime2",
                                    sclkTime2.getContinuousTicks(),
                                    "~",
                                    xTicks,
                                    TIGHT_TOL );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: TDB input" );

         xSec   = TimeConstants.SPD + 10.0;

         tdb0   = new TDBTime( xSec );

         //
         // Create an SCLK time instance from `tdb0'.
         //

         sclkTime0 = new SCLKTime ( sclk, tdb0 );

         //
         // We should be able to recover `xSec' from `sclkTime0'.
         //
         ok = JNITestutils.chcksd ( "sclkTime0", sclkTime0.getTDBSeconds(),
                                    "~/",   xSec,                 TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: TDT input" );

         xSec   = TimeConstants.SPD + 10.0;

         tdt0   = new TDTTime( xSec );

         //
         // Create an SCLK time instance from `tdt0'.
         //

         sclkTime0 = new SCLKTime ( sclk, tdt0 );

         //
         // We should be able to recover `xSec' from `sclkTime0'.
         //
         ok = JNITestutils.chcksd ( "sclkTime0",
                                    (new TDTTime(sclkTime0)).getTDTSeconds(),
                                    "~/",   xSec,                 TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: JED input" );

         xDays   = TimeConstants.J2000 + 10.0;

         jed0    = new JEDTime( xDays );

         //
         // Create an SCLK time instance from `jed0'.
         //

         sclkTime0 = new SCLKTime ( sclk, jed0 );

         //
         // We should be able to recover `xDays' from `sclkTime0'.
         //
         ok = JNITestutils.chcksd ( "sclkTime0",
                                    (new JEDTime(sclkTime0)).getDays(),
                                    "~/",   xDays,                 TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: " +
                              "SCLKTime input, input SCLKTime clock " +
                              "matches clock passed to constructor."         );

         xTicks    = 100.0;
         sclkTime1 = new SCLKTime( sclk, xTicks );

         sclkTime0 = new SCLKTime( sclk, sclkTime1 );


         //
         // We should be able to recover `xTicks' from `sclkTime0'.
         //
         ok = JNITestutils.chcksd ( "sclkTime0",
                                    sclkTime0.getContinuousTicks(),
                                    "~/",
                                    xTicks,
                                    TIGHT_TOL                       );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test universal Time constructor: " +
                              "SCLKTime input, input SCLKTime clock " +
                              "*differs* from clock passed to constructor."  );

         xTicks    = 1.e8;
         sclkTime1 = new SCLKTime( sclk888, xTicks );

         sclkTime0 = new SCLKTime( sclk, sclkTime1 );


         //
         // We should be able to recover `xTicks' from `sclkTime0'.
         //
         ok = JNITestutils.chcksd ( "sclkTime0",
                                    ( new SCLKTime( sclk888, sclkTime0) ).
                                       getContinuousTicks(),
                                    "~/",
                                    xTicks,
                                    TIGHT_TOL                       );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getSCLK." );


         sclkTime0 = new SCLKTime( sclk888, "1/000:0001" );

         ok = JNITestutils.chcksi ( "sclkTime0 clock",
                                    sclkTime0.getSCLK().getIDCode(),
                                    "=",
                                    -888,
                                    0                              );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getContinuousTicks." );


         sclkTime0 = new SCLKTime( sclk888, 1.0 );

         tdb0      = new TDBTime ( sclkTime0 );

         //
         // Add 0.3 ticks to tdb0.
         //
         tdb1      = tdb0.add( new SCLKDuration(sclk888,0.3) );

         //
         // Convert tdb1 to an SCLK time.
         //
         sclkTime1 = new SCLKTime( sclk888, tdb1 );

         //
         // Get continuous ticks from sclkTime1.
         //

         xVal = 1.3;

         ok = JNITestutils.chcksd ( "sclkTime1 ticks",
                                    sclkTime1.getContinuousTicks(),
                                    "~/",
                                    xVal,
                                    TIGHT_TOL                       );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getDiscreteTicks." );


         sclkTime0 = new SCLKTime( sclk888, 1.0 );

         tdb0      = new TDBTime ( sclkTime0 );

         //
         // Add 0.3 ticks to tdb0.
         //
         tdb1      = tdb0.add( new SCLKDuration(sclk888,0.3) );

         //
         // Convert tdb1 to an SCLK time.
         //
         sclkTime1 = new SCLKTime( sclk888, tdb1 );

         //
         // Get continuous ticks from sclkTime1.
         //

         xVal = 1.0;

         ok = JNITestutils.chcksd ( "sclkTime1 ticks",
                                    sclkTime1.getDiscreteTicks(),
                                    "~/",
                                    xVal,
                                    TIGHT_TOL                       );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getTDBSeconds." );


         xVal      = 1.e6 + 1.e-7;

         tdb0      = new TDBTime( xVal );

         sclkTime0 = new SCLKTime( sclk888, tdb0 );


         ok = JNITestutils.chcksd ( "sclkTime0 TDB seconds",
                                    sclkTime0.getTDBSeconds(),
                                    "~/",
                                    xVal,
                                    TIGHT_TOL                       );



         //
         // Some miscellaneous conversion tests follow.
         //


         //
         // --------Case-----------------------------------------------
         //

         //
         //   Test SCLKTime ( SCLK, Time ) constructor and
         //   getString() method.
         //
         JNITestutils.tcase ( "Convert a UTC time to ET, then to an "  +
                              "SCLK string. Make sure the conversion " +
                              "is invertible."                           );

         et      = new TDBTime  ( UTC );
         sclkdp  = new SCLKTime ( sclk, et  );

         sclkstr = sclkdp.getString();

         ok      = JNITestutils.chcksc ( "sclkstr", sclkstr, "=", SCLK_STR );


         et      = new TDBTime ( sclkdp );
         utcout  = et.toString ( "YYYY MON DD HR:MN:SC ::RND" );

         ok      = JNITestutils.chcksc ( "utcout", utcout, "=", UTC );

         //
         //   Save this ET value as the "expected ET."
         //
         expet   = new TDBTime ( et );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Encode an SCLK string as discrete ticks. " +
                              "Make sure the conversion is invertible."     );


         cclkdp  = new SCLKTime ( sclk, SCLK_STR );

         sclkstr = cclkdp.getString();

         ok      = JNITestutils.chcksc ( "sclkstr", sclkstr, "=", SCLK_STR );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Encode ET as continuous ticks. " +
                              "Convert ticks to ET. "           +
                              "Make sure the conversion is invertible." );

         //
         //  Note:  according to our SCLK kernel, one tick is 1.e-4 seconds.
         //
         expet             = new TDBTime ( "1980 Jan 1 00:00:10.00005 TDB" );

         expContTicks      = 10.00005 * 1.e4;

         cclkdp            = new SCLKTime ( sclk, expet );

         contTicks         = cclkdp.getContinuousTicks();

         ok                = JNITestutils.chcksd( "continuous ticks",
                                                  contTicks,
                                                  "~",
                                                  expContTicks,
                                                  0.001                 );


         cclkdp            = new SCLKTime( sclk, contTicks );
         et                = new TDBTime( cclkdp );

         ETSecPastJ2000    = et.getTDBSeconds();
         expETSecPastJ2000 = expet.getTDBSeconds();

         ok                = JNITestutils.chcksd( "et",
                                                  ETSecPastJ2000,
                                                  "~",
                                                  expETSecPastJ2000,
                                                  0.001                 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Round tick value from previous case " +
                              "to the nearest int.  Convert ticks "  +
                              "to ET. Make sure the conversion is "  +
                              "invertible."                            );

         expRoundTicks = (int) cclkdp.getDiscreteTicks();

         expsclkdp     = new SCLKTime ( sclk, (double)expRoundTicks );

         et            = new TDBTime( expsclkdp );

         sclkdp        = new SCLKTime ( sclk, et );

         roundTicks    = (int) cclkdp.getDiscreteTicks();

         ok            = JNITestutils.chcksi( "roundTicks",
                                              roundTicks,
                                              "=",
                                              expRoundTicks,
                                              0                  );



         //
         // SCLKTime arithmetic: subtraction
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract one SCLKTime from another; both " +
                              "times have the same clock."                 );

         sclkTime0  = new SCLKTime( sclk, 20000.0 );
         sclkTime1  = new SCLKTime( sclk, 30000.0 );

         sclkInc    = sclkTime0.sub( sclkTime1 );

         xTicks     = -10000.0;


         ok     = JNITestutils.chcksd ( "difference",   sclkInc.getMeasure(),
                                        "~/",   xTicks,  TIGHT_TOL           );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract one SCLKTime from another; the SCLK " +
                              "times have *different* clocks."               );

         sclkTime0  = new SCLKTime( sclk,    7.0e12 );
         sclkTime1  = new SCLKTime( sclk,    7.3e12 );
         sclkTime2  = new SCLKTime( sclk888, sclkTime1 );

         sclkInc    = sclkTime0.sub( sclkTime2 );

         xTicks     = -3.0e11;


         ok     = JNITestutils.chcksd ( "difference",   sclkInc.getMeasure(),
                                        "~/",   xTicks,  TIGHT_TOL           );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract one SCLKTime from another; the SCLK " +
                              "times have *different* clocks."               );

         sclkTime0  = new SCLKTime( sclk,    7.0e12 );
         sclkTime1  = new SCLKTime( sclk,    7.3e12 );
         sclkTime2  = new SCLKTime( sclk888, sclkTime1 );

         sclkInc    = sclkTime0.sub( sclkTime2 );

         xTicks     = -3.0e11;


         ok     = JNITestutils.chcksd ( "difference",   sclkInc.getMeasure(),
                                        "~/",   xTicks,  TIGHT_TOL           );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDBTime from an SCLKTime." );

         sclkTime0  = new SCLKTime( sclk, 7.0e12 );

         tdb0       = new TDBTime( sclkTime0.add( new
                                                 SCLKDuration(sclk, 3.e11) ) );

         sclkInc    = sclkTime0.sub( tdb0 );

         xTicks     = -3.0e11;


         ok     = JNITestutils.chcksd ( "difference",   sclkInc.getMeasure(),
                                        "~/",   xTicks,  TIGHT_TOL           );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDTTime from an SCLKTime." );

         sclkTime0  = new SCLKTime( sclk, 7.0e12 );

         tdt0       = new TDTTime( sclkTime0.add( new
                                                 SCLKDuration(sclk, 3.e11) ) );

         sclkInc    = sclkTime0.sub( tdb0 );

         xTicks     = -3.0e11;


         ok     = JNITestutils.chcksd ( "difference",   sclkInc.getMeasure(),
                                        "~/",   xTicks,  TIGHT_TOL           );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a JEDTime from an SCLKTime." );

         sclkTime0  = new SCLKTime( sclk, 7.0e12 );

         jed0       = new JEDTime( sclkTime0.add( new
                                                 SCLKDuration(sclk, 3.e11) ) );

         sclkInc    = sclkTime0.sub( tdb0 );

         xTicks     = -3.0e11;


         ok     = JNITestutils.chcksd ( "difference",   sclkInc.getMeasure(),
                                        "~/",   xTicks,  TIGHT_TOL           );





         //
         // SCLK duration arithmetic: subtraction
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract an SCLK Duration from an SCLKTime; " +
                              "both times have the same clock."              );

         ticks0     = 20000.0;
         durTicks0  = 11000.0;
         xVal       = ticks0 - durTicks0;

         sclkTime0  = new SCLKTime( sclk, ticks0 );

         sclkInc    = new SCLKDuration( sclk, durTicks0 );

         sclkTime1  = sclkTime0.sub( sclkInc );


         ok     = JNITestutils.chcksd ( "difference",
                                        sclkTime1.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract an SCLK Duration from an SCLKTime; " +
                              "the times have different clocks."            );

         ticks0     = 7.0e12;
         durTicks0  = 11000.0;
         xVal       = ticks0 - durTicks0;

         sclkTime0  = new SCLKTime( sclk,    ticks0    );

         sclkInc    = new SCLKDuration( sclk, durTicks0 );

         //
         // `sclkInc2' is the duration `sclkInc' expressed in the sclk888
         // system, with start time sclkTime0.
         //
         sclkInc2   = new SCLKDuration( sclk888, sclkInc, sclkTime0 );


         sclkTime1  = sclkTime0.sub( sclkInc2 );


         ok     = JNITestutils.chcksd ( "difference",
                                        sclkTime1.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDB Duration from an SCLKTime."  );

         ticks0     = 7.e13;
         durTicks0  = 11000.0;
         xVal       = ticks0 - durTicks0;

         sclkTime0  = new SCLKTime( sclk, ticks0 );

         sclkTime1  = new SCLKTime( sclk, ticks0 - durTicks0 );

         tdb0       = new TDBTime( sclkTime0 );
         tdb1       = new TDBTime( sclkTime1 );

         tdbdur0    = new TDBDuration( tdb0.getTDBSeconds() -
                                                        tdb1.getTDBSeconds() );

         sclkTime2  = sclkTime0.sub( tdbdur0 );

         ok     = JNITestutils.chcksd ( "difference",
                                        sclkTime2.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a TDT Duration from an SCLKTime."  );

         ticks0     = 7.e13;
         durTicks0  = 11000.0;
         xVal       = ticks0 - durTicks0;

         sclkTime0  = new SCLKTime( sclk, ticks0 );

         sclkTime1  = new SCLKTime( sclk, ticks0 - durTicks0 );

         tdt0       = new TDTTime( sclkTime0 );
         tdt1       = new TDTTime( sclkTime1 );

         tdtdur0    = new TDTDuration( tdt0.getTDTSeconds() -
                                                        tdt1.getTDTSeconds() );

         sclkTime2  = sclkTime0.sub( tdtdur0 );

         ok     = JNITestutils.chcksd ( "difference",
                                        sclkTime2.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Subtract a JED Duration from an SCLKTime."  );

         ticks0     = 7.e13;
         durTicks0  = 11000.0;
         xVal       = ticks0 - durTicks0;

         sclkTime0  = new SCLKTime( sclk, ticks0 );

         sclkTime1  = new SCLKTime( sclk, ticks0 - durTicks0 );

         jed0       = new JEDTime( sclkTime0 );
         jed1       = new JEDTime( sclkTime1 );

         jeddur0    = new JEDDuration( jed0.getDays() - jed1.getDays() );

         sclkTime2  = sclkTime0.sub( jeddur0 );

         ok     = JNITestutils.chcksd ( "difference",
                                        sclkTime2.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );






         //
         // SCLK duration arithmetic: addition
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add an SCLK Duration to an SCLKTime; " +
                              "both times have the same clock."              );

         ticks0     = 20000.0;
         durTicks0  = 11000.0;
         xVal       = ticks0 + durTicks0;

         sclkTime0  = new SCLKTime( sclk, ticks0 );

         sclkInc    = new SCLKDuration( sclk, durTicks0 );

         sclkTime1  = sclkTime0.add( sclkInc );


         ok     = JNITestutils.chcksd ( "sum",
                                        sclkTime1.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add an SCLK Duration to an SCLKTime; " +
                              "the times have different clocks."            );

         ticks0     = 7.0e12;
         durTicks0  = 11000.0;
         xVal       = ticks0 + durTicks0;

         sclkTime0  = new SCLKTime( sclk,    ticks0    );

         sclkInc    = new SCLKDuration( sclk, durTicks0 );

         //
         // `sclkInc2' is the duration `sclkInc' expressed in the sclk888
         // system, with start time sclkTime0.
         //
         sclkInc2   = new SCLKDuration( sclk888, sclkInc, sclkTime0 );


         sclkTime1  = sclkTime0.add( sclkInc2 );


         ok     = JNITestutils.chcksd ( "sum",
                                        sclkTime1.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a TDB Duration to an SCLKTime."  );

         ticks0     = 7.e13;
         durTicks0  = 11000.0;
         xVal       = ticks0 + durTicks0;

         sclkTime0  = new SCLKTime( sclk, ticks0 );

         sclkTime1  = new SCLKTime( sclk, ticks0 + durTicks0 );

         tdb0       = new TDBTime( sclkTime0 );
         tdb1       = new TDBTime( sclkTime1 );

         tdbdur0    = new TDBDuration( tdb1.getTDBSeconds() -
                                                        tdb0.getTDBSeconds() );

         sclkTime2  = sclkTime0.add( tdbdur0 );

         ok     = JNITestutils.chcksd ( "sum",
                                        sclkTime2.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a TDT Duration to an SCLKTime."  );

         ticks0     = 7.e13;
         durTicks0  = 11000.0;
         xVal       = ticks0 + durTicks0;

         sclkTime0  = new SCLKTime( sclk, ticks0 );

         sclkTime1  = new SCLKTime( sclk, ticks0 + durTicks0 );

         tdt0       = new TDTTime( sclkTime0 );
         tdt1       = new TDTTime( sclkTime1 );

         tdtdur0    = new TDTDuration( tdt1.getTDTSeconds() -
                                                        tdt0.getTDTSeconds() );

         sclkTime2  = sclkTime0.add( tdtdur0 );

         ok     = JNITestutils.chcksd ( "sum",
                                        sclkTime2.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add a JED Duration to an SCLKTime."  );

         ticks0     = 7.e13;
         durTicks0  = 11000.0;
         xVal       = ticks0 + durTicks0;

         sclkTime0  = new SCLKTime( sclk, ticks0 );

         sclkTime1  = new SCLKTime( sclk, ticks0 + durTicks0 );

         jed0       = new JEDTime( sclkTime0 );
         jed1       = new JEDTime( sclkTime1 );

         jeddur0    = new JEDDuration( jed1.getDays() - jed0.getDays() );

         sclkTime2  = sclkTime0.add( jeddur0 );

         ok     = JNITestutils.chcksd ( "sum",
                                        sclkTime2.getContinuousTicks(),
                                        "~/",   xVal,  TIGHT_TOL            );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString." );


         xTicks     = 1239999.0;

         sclkTime0  = new SCLKTime( sclk888, "1/123:9999" );

         xStr       = "1/00000123:9999";

         outStr     = sclkTime0.toString();

         ok = JNITestutils.chcksc ( "outStr", outStr, "=", xStr );
      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

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

         ( new File ( CK ) ).delete();

         //
         // Get rid of the SCLK kernels.
         //
         ( new File ( SCLKKER  ) ).delete();
         ( new File ( SCLKKER2 ) ).delete();
      }
     

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }


   /**
   Create and optionally load an SCLK kernel for a clock
   having specified attributes.
   */
   public static void createSCLK( String          filename,
                                  boolean         load,
                                  boolean         keep,
                                  int             idcode,
                                  Time            startEpoch,
                                  TimeSystem      timeSys,
                                  int             delimCode,
                                  double[]        moduli,
                                  double[]        offsets,
                                  double          rate       )

      throws SpiceException
   {
      double                            etsec0;
      double                            maxtick;
      double                            prod;
      double                            pstart;
      double                            pstop;

      int                               NLINES;
      int                               nfields;
      int                               i;
      int                               timesysCode;

      String[]                          textbuf;

      //
      // Create the assignments required for an SCLK kernel.
      //
      NLINES     = 9;
      textbuf    = new String[NLINES];

      //
      // SCLK data type
      //
      textbuf[0] = String.format(
                   "SCLK_DATA_TYPE_%d           = ( 1               )",
                   -idcode );


      //
      // Time system
      //
      timesysCode = 1;

      if ( timeSys == TimeSystem.TDT )
      {
         timesysCode = 2;
      }

      textbuf[1] = String.format(
                   "SCLK_TIME_SYSTEM_%d         = ( %d               )",
                   -idcode, timesysCode );


      //
      // Number of fields
      //
      nfields = moduli.length;

      textbuf[2] = String.format(
                   "SCLK01_N_FIELDS_%d          = ( %d               )",
                   -idcode, nfields );

      //
      // Moduli. Note that these are doubles so as to allow
      // for greater capacity.
      //
      textbuf[3] =  String.format(
                   "SCLK01_MODULI_%d            = ( %10.0f",
                   -idcode, moduli[0] );

      for ( i = 1;  i < nfields;  i++ )
      {
         textbuf[3] = String.format(
                      textbuf[3] + "   %10.0f",
                       moduli[i] );
      }

      textbuf[3] = textbuf[3] + " )";


      //
      // Offsets. These also are doubles.
      //
      textbuf[4] = String.format(
                   "SCLK01_OFFSETS_%d           = ( %10.0f",
                   -idcode,  offsets[0] );

      for ( i = 1;  i < nfields;  i++ )
      {
         textbuf[4] = String.format(
                      textbuf[4] + "   %10.0f",
                       offsets[i] );
      }

      textbuf[4] = textbuf[4] + " )";

      //
      // Output delimiter (integer code)
      //
      textbuf[5] = String.format(
                   "SCLK01_OUTPUT_DELIM_%d      = ( %d )",
                   -idcode, delimCode                     );

      //
      // Partition start
      //
      textbuf[6] = String.format(
                   "SCLK_PARTITION_START_%d     = ( %24.16e )",
                   -idcode, 0.0 );


      //
      // Partition stop. The max value is the number of ticks corresponding
      // to one tick less than the modulus of the most significant field.
      //

      prod = moduli[nfields-1];

      for ( i = nfields-2;  i >= 0;  i-- )
      {
         prod *= moduli[i];
      }

      maxtick = prod - 1.0;

      textbuf[7] = String.format(
                   "SCLK_PARTITION_END_%d       = ( %24.16e )",
                   -idcode, maxtick );


      //
      // Coefficient record.
      //
      etsec0     = ( new TDBTime(startEpoch) ).getTDBSeconds();

      textbuf[8] = String.format(
                   "SCLK01_COEFFICIENTS_%d      = ( %24.16e%n" +
                   "                                 %24.16e%n" +
                   "                                 %24.16e   )",
                   -idcode, 0.0, etsec0, rate );


      // For debugging
      //
      // for( i = 0; i < NLINES; i++ )
      // {
      //    System.out.println( textbuf[i] );
      // }


      //
      // Create an SCLK kernel containing the assignments stored
      // in textbuf.
      //
      TextWriter tw = new TextWriter( filename );

      tw.writeLine( " "          );
      tw.writeLine( "\\begindata" );
      tw.writeLine( " "          );

      for ( i = 0;  i < NLINES;  i++ )
      {
         tw.writeLine( textbuf[i] );
      }

      tw.writeLine( " " );
      tw.close();


      if ( load )
      {
         KernelDatabase.load( filename );
      }

      if ( !keep )
      {
         ( new File(filename) ).delete();
      }

   }

}





