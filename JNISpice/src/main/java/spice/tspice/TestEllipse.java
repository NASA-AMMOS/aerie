
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestEllipse provides methods that implement test families for
the class Ellipse.

<p>Version 1.0.0 15-DEC-2009 (NJB)
*/
public class TestEllipse extends Object
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
   Test Ellipse and associated classes.
   */
   public static boolean f_Ellipse()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-9;
      final double                      SQ2       = Math.sqrt( 2.0 );

      final int                         NELT      = 9;
      //
      // Local variables
      //
      Ellipse                           e0;
      Ellipse                           e1;
      Ellipse                           e2;
      Ellipse                           e3;

      Plane                             pl0;

      String                            outStr;
      String                            xStr;

      Vector3                           center;
      Vector3                           normal;
      Vector3                           smajor;
      Vector3                           sminor;
      Vector3                           v0;
      Vector3                           v1;
      Vector3                           v2;
      Vector3                           v3;
      Vector3                           xCenter;
      Vector3                           xSmajor;
      Vector3                           xSminor;


      boolean                           ok;

      double                            cons;
      double[]                          eltArray0;
      double[]                          eltArray1;

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

         JNITestutils.topen ( "f_Ellipse" );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: array constructor: " +
                               "array has incorrect length."   );

         try
         {
            eltArray0 = new double[8];

            e0 = new Ellipse( eltArray0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INVALIDARRAYSIZE)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true,   "SPICE(INVALIDARRAYSIZE)", ex );
         }



         try
         {
            eltArray0 = new double[10];

            e0 = new Ellipse( eltArray0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INVALIDARRAYSIZE)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true,   "SPICE(INVALIDARRAYSIZE)", ex );
         }


         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test array-based constructor. " +
                              "Also test getCenter, and getSemi*Axis."  );

         eltArray0 = new double[NELT];

         v0 = new Vector3(  1.0,  2.0,  3.0 );
         v1 = new Vector3(  1.0,  1.0,  0.0 );
         v2 = new Vector3(  0.0,  0.0,  2.0 );

         System.arraycopy( v0.toArray(), 0, eltArray0, 0, 3 );
         System.arraycopy( v1.toArray(), 0, eltArray0, 3, 3 );
         System.arraycopy( v2.toArray(), 0, eltArray0, 6, 3 );

         e0 = new Ellipse( eltArray0 );

         //
         // Check the ellipse's center.
         //
         ok = JNITestutils.chckad ( "center",
                                    e0.getCenter().toArray(),
                                    "~~/",
                                    v0.toArray(),
                                    TIGHT_TOL                 );

         //
         // Check the semi-major axis. There are two possible
         // correct choices; handle both.
         //

         if ( e0.getSemiMajorAxis().toArray()[2]  >  0.0 )
         {
            v3 = v2;
         }
         else
         {
            v3 = v2.negate();
         }

         ok = JNITestutils.chckad ( "smajor",
                                    e0.getSemiMajorAxis().toArray(),
                                    "~~/",
                                    v3.toArray(),
                                    TIGHT_TOL                      );


         //
         // Check the semi-minor axis. There are two possible
         // correct choices; handle both.
         //

         if ( e0.getSemiMinorAxis().toArray()[0]  >  0.0 )
         {
            v3 = v1;
         }
         else
         {
            v3 = v1.negate();
         }

         ok = JNITestutils.chckad ( "sminor",
                                    e0.getSemiMinorAxis().toArray(),
                                    "~~/",
                                    v3.toArray(),
                                    TIGHT_TOL                      );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test vector-based constructor. " +
                              "Also test getCenter, and getSemi*Axis."  );


         v0 = new Vector3(  1.0,  2.0,  3.0 );
         v1 = new Vector3(  1.0,  1.0,  0.0 );
         v2 = new Vector3(  0.0,  0.0,  2.0 );

         e1 = new Ellipse( v0, v1, v2 );

         //
         // Check the ellipse's center.
         //
         ok = JNITestutils.chckad ( "center",
                                    e1.getCenter().toArray(),
                                    "~~/",
                                    v0.toArray(),
                                    TIGHT_TOL                 );

         //
         // Check the semi-major axis. There are two possible
         // correct choices; handle both.
         //

         if ( e1.getSemiMajorAxis().toArray()[2]  >  0.0 )
         {
            v3 = v2;
         }
         else
         {
            v3 = v2.negate();
         }

         ok = JNITestutils.chckad ( "smajor",
                                    e1.getSemiMajorAxis().toArray(),
                                    "~~/",
                                    v3.toArray(),
                                    TIGHT_TOL                      );


         //
         // Check the semi-minor axis. There are two possible
         // correct choices; handle both.
         //

         if ( e1.getSemiMinorAxis().toArray()[0]  >  0.0 )
         {
            v3 = v1;
         }
         else
         {
            v3 = v1.negate();
         }

         ok = JNITestutils.chckad ( "sminor",
                                    e1.getSemiMinorAxis().toArray(),
                                    "~~/",
                                    v3.toArray(),
                                    TIGHT_TOL                      );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor. " +
                              "Also test getCenter, and getSemi*Axis."  );


         v0 = new Vector3(  1.0,  2.0,  3.0 );
         v1 = new Vector3(  1.0,  1.0,  0.0 );
         v2 = new Vector3(  0.0,  0.0,  2.0 );

         e0 = new Ellipse( v0, v1, v2 );

         e2 = new Ellipse( e0 );

         //
         // Change e0; make sure e2 doesn't change.
         //

         e0 = new Ellipse( v2, v0, v1 );

         //
         // Check the ellipse's center.
         //
         ok = JNITestutils.chckad ( "center",
                                    e2.getCenter().toArray(),
                                    "~~/",
                                    v0.toArray(),
                                    TIGHT_TOL                 );

         //
         // Check the semi-major axis. There are two possible
         // correct choices; handle both.
         //

         if ( e2.getSemiMajorAxis().toArray()[2]  >  0.0 )
         {
            v3 = v2;
         }
         else
         {
            v3 = v2.negate();
         }

         ok = JNITestutils.chckad ( "smajor",
                                    e2.getSemiMajorAxis().toArray(),
                                    "~~/",
                                    v3.toArray(),
                                    TIGHT_TOL                      );


         //
         // Check the semi-minor axis. There are two possible
         // correct choices; handle both.
         //

         if ( e2.getSemiMinorAxis().toArray()[0]  >  0.0 )
         {
            v3 = v1;
         }
         else
         {
            v3 = v1.negate();
         }

         ok = JNITestutils.chckad ( "sminor",
                                    e2.getSemiMinorAxis().toArray(),
                                    "~~/",
                                    v3.toArray(),
                                    TIGHT_TOL                      );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test project."  );


         v0  = new Vector3( 0.0, 0.0, 5.0 );
         v1  = new Vector3( 6.0, 0.0, 0.0 );
         v2  = new Vector3( 0.0, 1.0, 1.0 );

         e0  = new Ellipse( v0, v1, v2 );

         normal = new Vector3( 0.0, 0.0, 1.0 );
         cons   = 2.0;

         pl0    = new Plane( normal, cons );

         e1 = e0.project( pl0 );

         //
         // Check the projected ellipse's center.
         //
         xCenter = new Vector3( 0.0, 0.0, 2.0 );

         ok = JNITestutils.chckad ( "center",
                                    e1.getCenter().toArray(),
                                    "~~/",
                                    xCenter.toArray(),
                                    TIGHT_TOL                 );

         //
         // Check the projected ellipse's semi-major axis. There are
         // two possible correct choices; handle both.
         //

         if ( e1.getSemiMajorAxis().toArray()[0]  >  0.0 )
         {
            xSmajor = v1;
         }
         else
         {
            xSmajor = v1.negate();
         }


         ok = JNITestutils.chckad ( "smajor",
                                    e1.getSemiMajorAxis().toArray(),
                                    "~~/",
                                    xSmajor.toArray(),
                                    TIGHT_TOL                 );



         //
         // Check the projected ellipse's semi-minor axis. There
         // are two possible correct choices; handle both.
         //
         xSminor = new Vector3( 0.0, 1.0, 0.0 );

         if ( e1.getSemiMinorAxis().toArray()[0]  <  0.0 )
         {
            xSminor = xSminor.negate();
         }


         ok = JNITestutils.chckad ( "sminor",
                                    e1.getSemiMinorAxis().toArray(),
                                    "~~/",
                                    xSminor.toArray(),
                                    TIGHT_TOL                 );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test toArray."  );


         eltArray0 = new double[NELT];
         eltArray1 = new double[NELT];

         v0 = new Vector3(  1.0,  2.0,  3.0 );
         v1 = new Vector3(  1.0,  1.0,  0.0 );
         v2 = new Vector3(  0.0,  0.0,  2.0 );

         e0 = new Ellipse( v0, v1, v2 );


         eltArray0 = e0.toArray();

         //
         // Set up the expected array.
         //
         System.arraycopy( v0.toArray(), 0, eltArray1, 0, 3 );
         System.arraycopy( v2.toArray(), 0, eltArray1, 3, 3 );
         System.arraycopy( v1.toArray(), 0, eltArray1, 6, 3 );

         ok = JNITestutils.chckad ( "eltArray0",
                                    eltArray0,
                                    "~~/",
                                    eltArray1,
                                    TIGHT_TOL                      );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test unpack."  );


         eltArray0 = new double[NELT];

         for ( i = 0;  i < NELT;  i++ )
         {
            eltArray0[i] = i;
         }

         Vector3[] vArray = Ellipse.unpack( eltArray0 );


         //
         // Check the unpacked vectors.
         //
         eltArray1 = new double[3];

         System.arraycopy( eltArray0, 0, eltArray1, 0, 3 );

         ok = JNITestutils.chckad ( "vArray[0]",
                                    vArray[0].toArray(),
                                    "~~/",
                                    eltArray1,
                                    TIGHT_TOL                      );


         System.arraycopy( eltArray0, 3, eltArray1, 0, 3 );

         ok = JNITestutils.chckad ( "vArray[1]",
                                    vArray[1].toArray(),
                                    "~~/",
                                    eltArray1,
                                    TIGHT_TOL                      );


         System.arraycopy( eltArray0, 6, eltArray1, 0, 3 );

         ok = JNITestutils.chckad ( "vArray[2]",
                                    vArray[2].toArray(),
                                    "~~/",
                                    eltArray1,
                                    TIGHT_TOL                      );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test toString."  );

         v0  = new Vector3( -1.e-100, -2.e-100, -3.e-100 );
         v1  = new Vector3(  0.0,      1.e-100,  0.e-100 );
         v2  = new Vector3(  0.0,      0.e-100, -1.e-100 );

         e0  = new Ellipse( v0, v1, v2 );

         outStr = e0.toString();


         String endl = System.getProperty( "line.separator" );

         xStr   =

         "Center, Semi-major axis, Semi-minor axis:" + endl +
         "(-1.0000000000000000e-100, " +
          "-2.0000000000000000e-100, " +
          "-3.0000000000000000e-100)"  + endl +
         "(  0.0000000000000000e+00,"  +
         "  1.0000000000000000e-100,"  +
         "   0.0000000000000000e+00)"  + endl +
         "(  0.0000000000000000e+00,"  +
         "   0.0000000000000000e+00,"  +
         " -1.0000000000000000e-100)";

         // For debugging:
         //System.out.println( outStr );
         //System.out.println( xStr );

         ok = JNITestutils.chcksc( "outStr", outStr, "=", xStr );

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

