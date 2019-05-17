
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestTimeConstants provides methods that implement test families for
the class TimeConstants.

<p>Version 1.0.0 09-DEC-2009 (NJB)
*/
public class TestTimeConstants extends Object
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
   Test TimeConstants and associated classes.
   */
   public static boolean f_TimeConstants()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      VTIGHT_TOL = 1.e-14;

      //
      // Local variables
      //
      boolean                           ok;

      double                            xval;




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

         JNITestutils.topen ( "f_TimeConstants" );







         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************

         //
         // None so far.
         //

         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check B1950" );

         //
         // Value is from SPICELIB routine b1950.for.
         //
         xval = 2433282.42345905e0;

         ok = JNITestutils.chcksd( "B1950",
                                   TimeConstants.B1950,
                                   "~",
                                   xval,
                                   VTIGHT_TOL          );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check J1950" );

         //
         // Value is from SPICELIB routine j1950.for.
         //
         xval = 2433282.5e0;

         ok = JNITestutils.chcksd( "J1950",
                                   TimeConstants.J1950,
                                   "~",
                                   xval,
                                   VTIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check J2000" );

         //
         // Value is from SPICELIB routine j2000.for.
         //
         xval = 2451545.0e0;

         ok = JNITestutils.chcksd( "J2000",
                                   TimeConstants.J2000,
                                   "~",
                                   xval,
                                   VTIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check JYEAR" );

         //
         // Value is from SPICELIB routine jyear.for.
         //
         xval = 31557600.0e0;

         ok = JNITestutils.chcksd( "JYEAR",
                                   TimeConstants.JYEAR,
                                   "~",
                                   xval,
                                   VTIGHT_TOL          );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check SPD" );

         //
         // Value is from SPICELIB routine spd.for.
         //
         xval = 86400.0e0;

         ok = JNITestutils.chcksd( "SPD",
                                   TimeConstants.SPD,
                                   "~",
                                   xval,
                                   VTIGHT_TOL          );






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

