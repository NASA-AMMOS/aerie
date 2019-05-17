
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestSCLKDuration provides methods that implement test families for
the class SCLKDuration.

<p>Version 1.0.0 27-DEC-2009 (NJB)
*/
public class TestSCLKDuration extends Object
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
   Test SCLKDuration and associated classes.
   */
   public static boolean f_SCLKDuration()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL  = 1.e-12;
      final double                      VTIGHT_TOL = 1.e-14;
      final double                      MED_TOL    = 1.e-4;
      final double                      LOOSE_TOL  = 1.e-1;

      final String                      CK        = "SCLKtest.bc";
      final String                      SCLKKER   = "testsclk.tsc";
      final String                      SCLKKER2  = "testsclk2.tsc";

      final int                         CLK_ID    =  -9;

      //
      // Local variables
      //

      JEDDuration                       jeddur0;

      JEDTime                           jed0;
      JEDTime                           jed1;

      SCLK                              sclk      = new SCLK ( CLK_ID );
      SCLK                              sclk888   = new SCLK ( -888   );

      SCLKDuration                      sclkdur0;

      SCLKTime                          sclkTime0;
      SCLKTime                          sclkTime1;

      TDBDuration                       tdbdur0;

      TDBTime                           et0;
      TDBTime                           et1;

      SCLKDuration                      d0;
      SCLKDuration                      d1;
      SCLKDuration                      d2;
      SCLKDuration                      d3;

      TDTDuration                       tdtdur0;

      TDTTime                           tdt0;
      TDTTime                           tdt1;

      boolean                           ok;

      double                            diff;
      double                            s;
      double                            startval;
      double                            xval;
      double                            xTicks;

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

         JNITestutils.topen ( "f_SCLKDuration" );



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

         handle = JNITestutils.tstck3 (  CK, SCLKKER, false, true, true );

         ( new File ( CK ) ).delete();




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





         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create SCLK duration from a " +
                               "string without required SCLK loaded."        );

         try
         {
            d0  = new SCLKDuration( new SCLK(-99), "1/00012.12345" );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(KERNELVARNOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(KERNELVARNOTFOUND)", ex );
         }






         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test string constructor." );


         xval = 888.0;

         d0   = new SCLKDuration( sclk, "000000000.0888" );


         ok = JNITestutils.chcksd ( "d0 measure",
                                    d0.getMeasure(),
                                    "~",
                                    xval,
                                    TIGHT_TOL       );





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test double constructor." );


         xval = 888.0;

         d0   = new SCLKDuration( sclk, xval );


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

         d0   = new SCLKDuration( sclk, xval );

         d2   = new SCLKDuration( d0 );

         d0   = new SCLKDuration( sclk, xval + 2 );

         ok = JNITestutils.chcksd ( "d2 measure",
                                    d2.getMeasure(),
                                    "~",
                                    xval,
                                    TIGHT_TOL       );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test universal duration constructor: " +
                              "TDTDuration case." );


         xval       = 1.e13;
         sclkTime0  = new SCLKTime( sclk, xval );

         s          = 1.e7;

         sclkTime1  = new SCLKTime( sclk, xval + s );

         tdt0       = new TDTTime( sclkTime0 );
         tdt1       = new TDTTime( sclkTime1 );

         tdtdur0    = tdt1.sub( tdt0 );

         d1         = new SCLKDuration( sclk, tdtdur0,
                                                      new TDBTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDB epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         d1       = new SCLKDuration( sclk, tdtdur0, new TDTTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDT epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         //
         // Note: we use a very loose tolerance (1/10 tick) since we
         // expect loss of precision due to use of JED.
         //
         d1       = new SCLKDuration( sclk, tdtdur0, new JEDTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (JED epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    LOOSE_TOL       );


         d1       = new SCLKDuration( sclk, tdtdur0, sclkTime0 );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, matching clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         d1       = new SCLKDuration( sclk, tdtdur0,
                                             new SCLKTime(sclk888,sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, different clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test universal duration constructor: " +
                              "JEDDuration case." );

         //
         // We use a loose tolerance throughout since we expect loss
         // of precision due to use of JED.
         //

         xval       = 1.e13;
         sclkTime0  = new SCLKTime( sclk, xval );

         s          = 1.e7;

         sclkTime1  = new SCLKTime( sclk, xval + s );

         jed0       = new JEDTime( sclkTime0 );
         jed1       = new JEDTime( sclkTime1 );

         jeddur0    = jed1.sub( jed0 );

         d1         = new SCLKDuration( sclk, jeddur0,
                                                      new TDBTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDB epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    LOOSE_TOL       );


         d1       = new SCLKDuration( sclk, jeddur0, new TDTTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDT epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    LOOSE_TOL       );


         //
         // Note: we use a very loose tolerance (1/10 tick) since we
         // expect loss of precision due to use of JED.
         //
         d1       = new SCLKDuration( sclk, jeddur0, new JEDTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (JED epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    LOOSE_TOL       );


         d1       = new SCLKDuration( sclk, jeddur0, sclkTime0 );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, matching clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    LOOSE_TOL       );


         d1       = new SCLKDuration( sclk, jeddur0,
                                             new SCLKTime(sclk888,sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, different clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    LOOSE_TOL       );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test universal duration constructor: " +
                              "TDBDuration case." );


         xval       = 1.e13;
         sclkTime0  = new SCLKTime( sclk, xval );

         s          = 1.e7;

         sclkTime1  = new SCLKTime( sclk, xval + s );

         et0        = new TDBTime( sclkTime0 );
         et1        = new TDBTime( sclkTime1 );

         tdbdur0    = et1.sub( et0 );

         d1         = new SCLKDuration( sclk, tdbdur0,
                                                      new TDBTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDB epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         d1       = new SCLKDuration( sclk, tdbdur0, new TDTTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDT epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         //
         // Note: we use a very loose tolerance (1/10 tick) since we
         // expect loss of precision due to use of JED.
         //
         d1       = new SCLKDuration( sclk, tdbdur0, new JEDTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (JED epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    LOOSE_TOL       );


         d1       = new SCLKDuration( sclk, tdbdur0, sclkTime0 );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, matching clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         d1       = new SCLKDuration( sclk, tdbdur0,
                                             new SCLKTime(sclk888,sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, different clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );








         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test universal duration constructor: " +
                              "SCLKDuration case; duration and epoch " +
                              "have matching clocks."                    );



         xval       = 1.e13;
         sclkTime0  = new SCLKTime( sclk, xval );

         s          = 1.e7;

         sclkTime1  = new SCLKTime( sclk, xval + s );


         d0         = sclkTime1.sub( sclkTime0 );

         d1         = new SCLKDuration( sclk, d0, new TDBTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDB epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         d1       = new SCLKDuration( sclk, d0, new TDTTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDT epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         //
         // Note: we use a very loose tolerance (1/10 tick) since we
         // expect loss of precision due to use of JED.
         //
         d1       = new SCLKDuration( sclk, d0, new JEDTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (JED epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    LOOSE_TOL       );


         d1       = new SCLKDuration( sclk, d0, sclkTime0 );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, matching clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         d1       = new SCLKDuration( sclk, d0,
                                             new SCLKTime(sclk888,sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, different clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test universal duration constructor: " +
                              "SCLKDuration case; duration and epoch " +
                              "have different clocks."                    );



         xval       = 1.e13;
         sclkTime0  = new SCLKTime( sclk, xval );

         s          = 1.e7;

         sclkTime1  = new SCLKTime( sclk, xval + s );

         //
         // Create a duration expressed in ticks of clock `sclk'.
         //
         d2         = sclkTime1.sub( sclkTime0 );

         //
         // Convert the duration to one expressed in ticks of clock `sclk888'.
         //
         d0         = new SCLKDuration( sclk888, d2, sclkTime0 );

         //
         // Apply the universal constructor using a TDB input epoch.
         //
         d1         = new SCLKDuration( sclk, d0, new TDBTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDB epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         d1       = new SCLKDuration( sclk, d0, new TDTTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (TDT epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         //
         // Note: we use a very loose tolerance (1/10 tick) since we
         // expect loss of precision due to use of JED.
         //
         d1       = new SCLKDuration( sclk, d0, new JEDTime(sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (JED epoch)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    LOOSE_TOL       );


         d1       = new SCLKDuration( sclk, d0, sclkTime0 );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, matching clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         d1       = new SCLKDuration( sclk, d0,
                                             new SCLKTime(sclk888,sclkTime0) );

         ok = JNITestutils.chcksd ( "d1 measure (SCLK epoch, different clock)",
                                    d1.getMeasure(),
                                    "~",
                                    s,
                                    MED_TOL       );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test format" );

         xval = 1234567890123.0;
         d0   = new SCLKDuration( sclk888, xval );

         ok = JNITestutils.chcksc ( "formatted SCLK",
                                    SCLKDuration.format( d0 ),
                                    "=",
                                    "123456789:0123"   );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getSCLK" );

         xval = 999.0;
         d0   = new SCLKDuration( new SCLK(-777), xval );

         ok = JNITestutils.chcksi ( "SCLK",
                                    d0.getSCLK().getIDCode(),
                                    "=",
                                    -777,
                                    0       );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getMeasure." );

         xval = 999.0;
         d0   = new SCLKDuration( sclk, xval );

         ok = JNITestutils.chcksd ( "d0 measure",
                                    d0.getMeasure(),
                                    "~",
                                    xval,
                                    TIGHT_TOL       );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getTDBSeconds." );


         xval     = 999.0;
         tdbdur0  = new TDBDuration( xval );

         et0      = new TDBTime( -777.0 );

         sclkTime0 = new SCLKTime( sclk, et0 );

         sclkTime1 = new SCLKTime( sclk, et0.add( tdbdur0 ) );

         d0   = sclkTime1.sub( sclkTime0 );

         ok = JNITestutils.chcksd ( "d0 TDB seconds",
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

         d0   = new SCLKDuration( sclk, xval );

         d1   = new SCLKDuration( sclk, s );

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

         d0   = new SCLKDuration( sclk, xval );

         d1   = new SCLKDuration( sclk, s );

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

         d0   = new SCLKDuration( sclk888, xval );

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

         d0   = new SCLKDuration( sclk888, xval );

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

