
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestTDBDuration provides methods that implement test families for
the class TDBDuration.

<p>Version 1.0.0 13-DEC-2009 (NJB)
*/
public class TestTDBDuration extends Object
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
   Test TDBDuration and associated classes.
   */
   public static boolean f_TDBDuration()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-7;
      final double                      MED_TOL   = 1.e-4;

      final String                      CK        = "SCLKtest.bc";
      final String                      SCLK      = "testsclk.tsc";

      final int                         CLK_ID    =  -9;

      //
      // Local variables
      //
      JEDDuration                       jdur0;

      SCLK                              sclk      = new SCLK ( CLK_ID );

      SCLKDuration                      sclkdur0;

      SCLKTime                          sclk0;
      SCLKTime                          sclk1;

      TDBDuration                       d0;
      TDBDuration                       d1;
      TDBDuration                       d2;
      TDBDuration                       d3;

      TDBTime                           et0;
      TDBTime                           et1;

      TDTDuration                       tdtdur0;

      TDTTime                           tdt0;
      TDTTime                           tdt1;

      boolean                           ok;

      double                            diff;
      double                            s;
      double                            xval;

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

         JNITestutils.topen ( "f_TDBDuration" );





         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create duration from TDTDuration " +
                               "with no LSK loaded."                         );

         try
         {
            et0     = new TDBTime    (  0.0 );
            tdtdur0 = new TDTDuration(  5.0 );

            d0      = new TDBDuration( tdtdur0, et0 );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(MISSINGTIMEINFO)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(MISSINGTIMEINFO)", ex );
         }


         // ***********************************************************
         //
         //    Setup
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: create and load kernels." );


         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();

         JNITestutils.tstlsk();



         JNITestutils.tcase ( "Create an SCLK kernel. The routine "     +
                              "we use for this purpose also creates a " +
                              "C-kernel, which we don't need."            );

         handle = JNITestutils.tstck3 (  CK, SCLK, false, true, true );

         ( new File ( CK ) ).delete();




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test zero-args constructor." );


         d0 = new TDBDuration();


         ok = JNITestutils.chcksd ( "d0 measure",
                                    d0.getMeasure(),
                                    "~",
                                    0.0,
                                    TIGHT_TOL       );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test double constructor." );

         xval = 888.0;

         d0   = new TDBDuration( xval );


         ok = JNITestutils.chcksd ( "d0 measure",
                                    d0.getMeasure(),
                                    "~",
                                    xval,
                                    TIGHT_TOL       );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor." );

         xval = 888.0;

         d0   = new TDBDuration( xval );
         d1   = new TDBDuration( xval );

         d2   = new TDBDuration( d0 );

         d0   = new TDBDuration( xval + 2 );

         ok = JNITestutils.chcksd ( "d2 measure",
                                    d2.getMeasure(),
                                    "~",
                                    xval,
                                    TIGHT_TOL       );
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test universal duration constructor: " +
                              "TDBDuration case." );


         xval     = 888.0;
         et0      = new TDBTime( xval );

         s        = 10.0;
         d0       = new TDBDuration( s );

         d1       = new TDBDuration( d0, et0 );

         ok = JNITestutils.chcksd ( "d1 measure (TDB epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    TIGHT_TOL       );


         d1       = new TDBDuration( d0, new TDTTime(et0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDT epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    TIGHT_TOL       );


         d1       = new TDBDuration( d0, new JEDTime(et0) );

         ok = JNITestutils.chcksd ( "d1 measure (JED epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    TIGHT_TOL       );


         d1       = new TDBDuration( d0, new SCLKTime(sclk, et0) );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    TIGHT_TOL       );





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test universal duration constructor: " +
                              "JEDDuration case." );


         xval     = 888.0;
         et0      = new TDBTime( xval );

         s        = 10.0;
         jdur0    = new JEDDuration( s );

         d1       = new TDBDuration( jdur0, et0 );

         ok = JNITestutils.chcksd ( "d1 measure (TDB epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s * TimeConstants.SPD,
                                    TIGHT_TOL       );


         d1       = new TDBDuration( jdur0, new TDTTime(et0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDT epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s * TimeConstants.SPD,
                                    TIGHT_TOL       );


         d1       = new TDBDuration( jdur0, new JEDTime(et0) );

         ok = JNITestutils.chcksd ( "d1 measure (JED epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s * TimeConstants.SPD,
                                    TIGHT_TOL       );


         d1       = new TDBDuration( jdur0, new SCLKTime(sclk, et0) );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s * TimeConstants.SPD,
                                    TIGHT_TOL       );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test universal duration constructor: " +
                              "TDTDuration case." );

         xval     = 888.0;
         et0      = new TDBTime( xval );

         tdtdur0  = new TDTDuration( 112.0 );

         d0       = new TDBDuration( tdtdur0, et0 );

         tdt0     = new TDTTime( et0 );

         tdt1     = tdt0.add( tdtdur0 );

         et1      = new TDBTime( tdt1 );

         diff     = et1.getTDBSeconds() - et0.getTDBSeconds();

         ok = JNITestutils.chcksd ( "d0 measure (TDB epoch)",
                                    d0.getMeasure(),
                                    "~",
                                    diff,
                                    TIGHT_TOL             );


         d0       = new TDBDuration( tdtdur0, new TDTTime(et0) );

         tdt0     = new TDTTime( et0 );

         tdt1     = tdt0.add( tdtdur0 );

         et1      = new TDBTime( tdt1 );

         diff     = et1.getTDBSeconds() - et0.getTDBSeconds();

         ok = JNITestutils.chcksd ( "d0 measure (TDT epoch)",
                                    d0.getMeasure(),
                                    "~",
                                    diff,
                                    TIGHT_TOL             );


         d0       = new TDBDuration( tdtdur0, new JEDTime(et0) );

         tdt0     = new TDTTime( et0 );

         tdt1     = tdt0.add( tdtdur0 );

         et1      = new TDBTime( tdt1 );

         diff     = et1.getTDBSeconds() - et0.getTDBSeconds();

         ok = JNITestutils.chcksd ( "d0 measure (JED epoch)",
                                    d0.getMeasure(),
                                    "~",
                                    diff,
                                    TIGHT_TOL             );


         d0       = new TDBDuration( tdtdur0, new SCLKTime(sclk,et0) );

         tdt0     = new TDTTime( et0 );

         tdt1     = tdt0.add( tdtdur0 );

         et1      = new TDBTime( tdt1 );

         diff     = et1.getTDBSeconds() - et0.getTDBSeconds();

         ok = JNITestutils.chcksd ( "d0 measure (SCLK epoch)",
                                    d0.getMeasure(),
                                    "~",
                                    diff,
                                    TIGHT_TOL             );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test universal duration constructor: " +
                              "SCLKDuration case." );


         xval     = 888.0;
         et0      = new TDBTime( xval );

         sclkdur0 = new SCLKDuration( sclk, 112.0 );

         d0       = new TDBDuration( sclkdur0, et0 );

         sclk0    = new SCLKTime( sclk, et0 );

         sclk1    = sclk0.add( sclkdur0 );

         et1      = new TDBTime( sclk1 );

         diff     = et1.getTDBSeconds() - et0.getTDBSeconds();

         ok = JNITestutils.chcksd ( "d0 measure (TDB epoch)",
                                    d0.getMeasure(),
                                    "~",
                                    diff,
                                    TIGHT_TOL             );


         d0       = new TDBDuration( sclkdur0, new TDTTime(et0) );

         sclk0    = new SCLKTime( sclk, et0 );

         sclk1    = sclk0.add( sclkdur0 );

         et1      = new TDBTime( sclk1 );

         diff     = et1.getTDBSeconds() - et0.getTDBSeconds();

         ok = JNITestutils.chcksd ( "d0 measure (TDT epoch)",
                                    d0.getMeasure(),
                                    "~",
                                    diff,
                                    TIGHT_TOL             );


         d0       = new TDBDuration( sclkdur0, new JEDTime(et0) );

         sclk0    = new SCLKTime( sclk, et0 );

         sclk1    = sclk0.add( sclkdur0 );

         et1      = new TDBTime( sclk1 );

         diff     = et1.getTDBSeconds() - et0.getTDBSeconds();

         ok = JNITestutils.chcksd ( "d0 measure (JED epoch)",
                                    d0.getMeasure(),
                                    "~",
                                    diff,
                                    TIGHT_TOL             );


         d0       = new TDBDuration( sclkdur0, new SCLKTime(sclk,et0) );

         sclk0    = new SCLKTime( sclk, et0 );

         sclk1    = sclk0.add( sclkdur0 );

         et1      = new TDBTime( sclk1 );

         diff     = et1.getTDBSeconds() - et0.getTDBSeconds();

         ok = JNITestutils.chcksd ( "d0 measure (SCLK epoch)",
                                    d0.getMeasure(),
                                    "~",
                                    diff,
                                    TIGHT_TOL             );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getMeasure." );

         xval = 999.0;
         d0   = new TDBDuration( xval );

         ok = JNITestutils.chcksd ( "d0 seconds",
                                    d0.getMeasure(),
                                    "~",
                                    xval,
                                    TIGHT_TOL       );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getTDBSeconds." );


         xval = 999.0;
         d0   = new TDBDuration( xval );
         et0  = new TDBTime( -777.0 );

         ok = JNITestutils.chcksd ( "d0 seconds",
                                    d0.getTDBSeconds( et0 ),
                                    "~",
                                    xval,
                                    TIGHT_TOL       );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test add." );

         xval = 999.0;
         s    = 222.0;

         d0   = new TDBDuration( xval );

         d1   = new TDBDuration( s );

         d2   = d1.add( d0 );

         ok = JNITestutils.chcksd ( "d2",
                                    d2.getMeasure(),
                                    "~",
                                    s+xval,
                                    TIGHT_TOL       );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test sub." );

         xval = 999.0;
         s    = 222.0;

         d0   = new TDBDuration( xval );

         d1   = new TDBDuration( s );

         d2   = d1.sub( d0 );

         ok = JNITestutils.chcksd ( "d2",
                                    d2.getMeasure(),
                                    "~",
                                    s-xval,
                                    TIGHT_TOL       );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test negate." );

         xval = 999.0;

         d0   = new TDBDuration( xval );

         d1   = d0.negate();

         ok = JNITestutils.chcksd ( "d1",
                                    d1.getMeasure(),
                                    "~",
                                    -xval,
                                    TIGHT_TOL       );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test scale." );

         xval = 999.0;
         s    = 2.0;

         d0   = new TDBDuration( xval );

         d1   = d0.scale( s);

         ok = JNITestutils.chcksd ( "d1",
                                    d1.getMeasure(),
                                    "~",
                                    xval*s,
                                    TIGHT_TOL       );


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

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

