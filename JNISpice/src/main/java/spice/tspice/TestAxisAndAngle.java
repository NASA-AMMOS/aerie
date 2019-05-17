
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestAxisAndAngle provides methods that implement test families for
the class AxisAndAngle.

<p>Version 1.0.0 08-DEC-2009 (NJB)
*/
public class TestAxisAndAngle extends Object
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
   Test AxisAndAngle and associated classes.
   */
   public static boolean f_AxisAndAngle()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-9;


      //
      // Local variables
      //
      Matrix33                          m0;
      Matrix33                          m1;
      Matrix33                          m2;

      AxisAndAngle                      axa0;
      AxisAndAngle                      axa1;
      AxisAndAngle                      axa2;

      Vector3                           v0;

      boolean                           ok;

      double                            xAngle;

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

         JNITestutils.topen ( "f_AxisAndAngle" );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: axis/angle constructor: " +
                               "axis vector is zero."      );

         try
         {
            axa0 = new AxisAndAngle( new Vector3(), 1.0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(ZEROVECTOR)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true,   "SPICE(ZEROVECTOR)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: matrix constructor: " +
                               "input matrix is not a rotation."      );

         try
         {
            m0   = new Matrix33( new Vector3( 1.0, 0.0, 0.0 ),
                                 new Vector3( 0.0, 1.0, 0.0 ),
                                 new Vector3( 1.0, 0.0, 1.0 )  );

            axa0 = new AxisAndAngle( m0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(NOTAROTATION)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true,   "SPICE(NOTAROTATION)", ex );
         }









         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         JNITestutils.tcase ( "Test axis and angle-based constructor. " +
                              "Also test getAxis and getAngle."           );

         v0     = new Vector3( 1.0, 2.0, 3.0 );
         xAngle = 30.0 * AngularUnits.RPD;

         axa0  = new AxisAndAngle( v0, xAngle );


         //
         // Check axis. Note we're looking for a unit-length vector.
         //
         ok = JNITestutils.chckad ( "axis",
                                    axa0.getAxis().toArray(),
                                    "~~/",
                                    v0.hat().toArray(),
                                    TIGHT_TOL                 );

         //
         // Check angle.
         //
         ok = JNITestutils.chcksd ( "angle",
                                    axa0.getAngle(),
                                    "~",
                                    xAngle,
                                    TIGHT_TOL                 );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test matrix-based constructor." );


         //
         // Recover axis and angle of rotation formed using this constructor,
         // where the constructor is passed a matrix created using the
         // comparable constructor of Matrix33.
         //

         v0     = new Vector3( -1.0, 2.0, -3.0 );
         xAngle = 20.0 * AngularUnits.RPD;

         m0     = new Matrix33( v0, xAngle );

         axa0  = new AxisAndAngle( m0 );


         //
         // Check axis. Note we're looking for a unit-length vector.
         //
         ok = JNITestutils.chckad ( "axis",
                                    axa0.getAxis().toArray(),
                                    "~~/",
                                    v0.hat().toArray(),
                                    TIGHT_TOL                 );

         //
         // Check angle.
         //
         ok = JNITestutils.chcksd ( "angle",
                                    axa0.getAngle(),
                                    "~",
                                    xAngle,
                                    TIGHT_TOL                 );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );

         v0     = new Vector3( -1.0, 2.0, -3.0 );
         xAngle = 20.0 * AngularUnits.RPD;

         m0     = new Matrix33( v0, xAngle );

         axa0 = new AxisAndAngle( m0 );
         axa1 = new AxisAndAngle( m0 );

         axa2 = new AxisAndAngle( axa0 );

         //
         // Make sure that changing axa0 doesn't affect axa2.
         //

         axa0 = new AxisAndAngle( m0.xpose() );

         //
         // Check axis. Note we're looking for a unit-length vector.
         //
         ok = JNITestutils.chckad ( "axis",
                                    axa2.getAxis().toArray(),
                                    "~~/",
                                    v0.hat().toArray(),
                                    TIGHT_TOL                 );

         //
         // Check angle.
         //
         ok = JNITestutils.chcksd ( "angle",
                                    axa2.getAngle(),
                                    "~",
                                    xAngle,
                                    TIGHT_TOL                 );







         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toMatrix." );

         v0     = new Vector3( -1.0, 2.0, -3.0 );
         xAngle = 20.0 * AngularUnits.RPD;

         m0     = new Matrix33( v0, xAngle );

         axa0   = new AxisAndAngle( m0 );

         m1     = axa0.toMatrix();

         m2     = new Matrix33( v0, xAngle );

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("m1 row " + i),
                                       m1.toArray()[i],
                                       "~~",
                                       m2.toArray()[i],
                                       TIGHT_TOL       );
         }



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

