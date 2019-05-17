
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.DistanceUnits.*;


/**
Class TestDistanceUnits provides methods that implement test families for
the class DistanceUnits.

<p>Version 1.0.0 21-DEC-2009 (NJB)
*/
public class TestDistanceUnits extends Object
{



   /**
   Test DistanceUnits and associated classes.
   */
   public static boolean f_DistanceUnits()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      VTIGHT = 1.e-14;

      //
      // Local variables
      //
      boolean                           ok;

      double                            xval;

      DistanceUnits                     distUnits0;
      DistanceUnits                     distUnits1;

      String                            name;
      String                            xName;



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

         JNITestutils.topen ( "f_DistanceUnits" );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check toKM method."   );

         //
         // Check AU. The expected value is obtained by scaling the
         // value from the SPICELIB routine CONVRT, which uses units
         // of meters:
         //
         //    1.4959787061368887D11
         //
         // Our value has units of KM/AU.
         //

         xval = 1.4959787061368887e8;


         ok = JNITestutils.chcksd( "AU.toKm()",
                                   AU.toKm(),
                                   "~/",
                                   xval,
                                   VTIGHT      );


         //
         // Check KM.
         //

         xval = 1.0;

         ok = JNITestutils.chcksd( "KM.toKm()",
                                   KM.toKm(),
                                   "~/",
                                   xval,
                                   VTIGHT      );

         //
         // Check METERS.
         //

         xval = 1.e-3;

         ok = JNITestutils.chcksd( "METERS.toKm()",
                                   METERS.toKm(),
                                   "~/",
                                   xval,
                                   VTIGHT      );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check toString." );


         //
         // Check AU.
         //

         ok = JNITestutils.chcksc( "AU string",
                                   AU.toString(),
                                   "=",
                                   "AU"             );


         //
         // Check KM.
         //

         ok = JNITestutils.chcksc( "KM string",
                                   KM.toString(),
                                   "=",
                                   "KM"             );


         //
         // Check METERS.
         //

         ok = JNITestutils.chcksc( "METERS string",
                                   METERS.toString(),
                                   "=",
                                   "METERS"             );



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

