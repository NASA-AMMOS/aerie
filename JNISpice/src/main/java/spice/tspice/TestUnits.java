
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestUnits provides methods that implement test families for
the class Units.

<p>Version 1.0.0 21-DEC-2009 (NJB)
*/
public class TestUnits extends Object
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
   Test Units and associated classes.
   */
   public static boolean f_Units()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL  = 1.e-12;
      final double                      VTIGHT_TOL = 1.e-14;
      final double                      MED_TOL    = 1.e-9;

      //
      // Local variables
      //
      boolean                           ok;

      double                            s;

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

         JNITestutils.topen ( "f_Units" );


         //
         // --------Case-----------------------------------------------
         //



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: attempt to convert radians " +
                               "to meters."                         );

         try
         {
            s = Units.convert( 1.0,
                               AngularUnits.RADIANS,
                               DistanceUnits.METERS  );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INCOMPATIBLEUNITS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INCOMPATIBLEUNITS)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Convert radians to degrees."  );

         s = Units.convert( 1.0,
                            AngularUnits.RADIANS,
                            AngularUnits.DEGREES  );

         ok = JNITestutils.chcksd( "s",
                                   s,
                                   "~/",
                                   AngularUnits.DPR,
                                   VTIGHT_TOL        );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Convert meters to km."  );

         s = Units.convert( 1.0,
                            DistanceUnits.METERS,
                            DistanceUnits.KM  );

         ok = JNITestutils.chcksd( "s",
                                   s,
                                   "~/",
                                   1.e-3,
                                   VTIGHT_TOL        );




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

