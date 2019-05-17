
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.*;


/**
Class TestAngularUnits provides methods that implement test families for
the class AngularUnits.

<p>Version 1.0.0 18-DEC-2009 (NJB)
*/
public class TestAngularUnits extends Object
{



   /**
   Test AngularUnits and associated classes.
   */
   public static boolean f_AngularUnits()

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

      AngularUnits                      angularUnits0;
      AngularUnits                      angularUnits1;

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

         JNITestutils.topen ( "f_AngularUnits" );


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

            JNITestutils.tcase ( "Pass invalid unit name to constructor." );


            angularUnits0 = new AngularUnits( "XXX" );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(UNITSNOTREC)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(UNITSNOTREC)", ex );
         }



         try
         {

            //
            // --------Case-----------------------------------------------
            //

            JNITestutils.tcase ( "Pass distance unit name to constructor." );


            angularUnits0 = new AngularUnits( "METERS" );


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
         JNITestutils.tcase ( "Check String-based constructor." );


         angularUnits0 = new AngularUnits( "HOURANGLE" );

         //
         // Express this unit as radians.
         //

         xval = 15.0 * RPD;

         ok = JNITestutils.chcksd( "angularUnits0.toRadians()",
                                   angularUnits0.toRadians(),
                                   "~/",
                                   xval,
                                   VTIGHT      );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check toRadians method."   );

         //
         // Check DEGREES.
         //

         xval = Math.PI/180.0;


         ok = JNITestutils.chcksd( "DEGREES.toRadians()",
                                   DEGREES.toRadians(),
                                   "~/",
                                   xval,
                                   VTIGHT      );

         //
         // Check ARCSECONDS.
         //

         xval = (Math.PI/180.0)/3600.0;

         ok = JNITestutils.chcksd( "ARCSECONDS.toRadians()",
                                   ARCSECONDS.toRadians(),
                                   "~/",
                                   xval,
                                   VTIGHT      );


         //
         // Check RADIANS.
         //

         xval = 1.0;

         ok = JNITestutils.chcksd( "RADIANS.toRadians()",
                                   RADIANS.toRadians(),
                                   "~/",
                                   xval,
                                   VTIGHT      );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check toString." );


         //
         // Check DEGREES.
         //

         ok = JNITestutils.chcksc( "DEGREES string",
                                   DEGREES.toString(),
                                   "=",
                                   "DEGREES"             );

         //
         // Check ARCSECONDS.
         //
         ok = JNITestutils.chcksc( "ARCSECONDS string",
                                   ARCSECONDS.toString(),
                                   "=",
                                   "ARCSECONDS"             );


         //
         // Check RADIANS.
         //

         ok = JNITestutils.chcksc( "RADIANS string",
                                   RADIANS.toString(),
                                   "=",
                                   "RADIANS"             );



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

