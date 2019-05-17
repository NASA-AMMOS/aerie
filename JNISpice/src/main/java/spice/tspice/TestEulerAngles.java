
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.*;

/**
Class TestEulerAngles provides methods that implement test families for
the class EulerAngles.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 07-DEC-2009 (NJB)</h3>
*/
public class TestEulerAngles extends Object
{

   //
   // Class constants
   //
   private static String  PCK        = "EulerAngles.tpc";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test EulerAngles and associated classes.
   */
   public static boolean f_EulerAngles()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 2.e-6;
      final double                      SQ2       = Math.sqrt(2.0);
      final double                      SQ3       = Math.sqrt(3.0);

      final String                      endl = System.getProperty(
                                                           "line.separator" );

      //
      // Local variables
      //
      EulerAngles                       e0;
      EulerAngles                       e1;
      EulerAngles                       e2;
      EulerAngles                       e3;

      Matrix33                          m0;
      Matrix33                          m1;
      Matrix33                          m2;
      Matrix33                          m3;
      Matrix33                          m4;
      Matrix33                          xMat0;

      ReferenceFrame                    J2000    =
                                        new ReferenceFrame( "J2000"    );

      ReferenceFrame                    IAU_MARS =
                                        new ReferenceFrame( "IAU_MARS" );

      ReferenceFrame                    IAU_MOON =
                                        new ReferenceFrame( "IAU_MOON" );

      String                            outStr;
      String                            xstr0;


      TDBTime                           et;

      Vector3                           row0;
      Vector3                           row1;
      Vector3                           row2;
      Vector3[]                         rows;
      Vector3                           v0;
      Vector3                           v1;
      Vector3                           vout0;
      Vector3                           vout1;

      boolean                           isrot;
      boolean                           ok;

      double                            angle;
      double[]                          angles;
      double[][]                        eltArray;
      double                            det;
      double                            dist;
      double                            elt0;
      double                            elt1;
      double                            elt2;
      double                            value;
      double[]                          xAngles;
      double[][]                        xArray2D;
      double                            xElt;

      int[]                             axes;
      int                               i;
      int                               j;
      int[]                             xAxes;


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

         JNITestutils.topen ( "f_EulerAngles" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: create and load kernels." );


         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();

         JNITestutils.tstlsk();

         //
         // Delete PCK if it exists. Create and load a PCK file.
         //
         ( new File ( PCK ) ).delete();

         JNITestutils.tstpck( PCK, true, false );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: angles/axes array constructor: " +
                              "indices out of range."                   );

         for ( i = 0;  i < 6;  i++ )
         {
            try
            {
               angles  = new double[3];
               axes    = new int[3];

               axes[0] = 1;
               axes[1] = 3;
               axes[2] = 1;

               if ( i < 3 )
               {
                  axes[i]   = 0;
               }
               else
               {
                  axes[i-3] = 4;
               }

               e0     = new EulerAngles( angles, axes );

               //
               // If an exception is *not* thrown, we'll hit this call.
               //

               Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

            }
            catch ( SpiceException ex )
            {
               ok = JNITestutils.chckth ( true, "SPICE(INDEXOUTOFRANGE)", ex );
            }
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: angles/axes scalar constructor: " +
                              "indices out of range."                   );

         for ( i = 0;  i < 6;  i++ )
         {
            try
            {
               angles  = new double[3];
               axes    = new int[3];

               axes[0] = 1;
               axes[1] = 3;
               axes[2] = 1;

               if ( i < 3 )
               {
                  axes[i]   = 0;
               }
               else
               {
                  axes[i-3] = 4;
               }

               e0     = new EulerAngles( angles[0], angles[1], angles[2],
                                         axes[0],   axes[1],   axes[2]   );

               //
               // If an exception is *not* thrown, we'll hit this call.
               //

               Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

            }
            catch ( SpiceException ex )
            {
               ok = JNITestutils.chckth ( true, "SPICE(INDEXOUTOFRANGE)", ex );
            }
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: angles/axes scalar constructor " +
                              "w/units: indices out of range."               );

         for ( i = 0;  i < 6;  i++ )
         {
            try
            {
               angles  = new double[3];
               axes    = new int[3];

               axes[0] = 1;
               axes[1] = 3;
               axes[2] = 1;

               if ( i < 3 )
               {
                  axes[i]   = 0;
               }
               else
               {
                  axes[i-3] = 4;
               }

               e0 = new EulerAngles( angles[0], angles[1], angles[2], DEGREES,
                                     axes[0],   axes[1],   axes[2]           );

               //
               // If an exception is *not* thrown, we'll hit this call.
               //

               Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

            }
            catch ( SpiceException ex )
            {
               ok = JNITestutils.chckth ( true, "SPICE(INDEXOUTOFRANGE)", ex );
            }
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: matrix constructor: " +
                              "indices out of range."          );

         for ( i = 0;  i < 6;  i++ )
         {
            try
            {
               angles  = new double[3];
               axes    = new int[3];

               axes[0] = 1;
               axes[1] = 3;
               axes[2] = 1;

               if ( i < 3 )
               {
                  axes[i]   = 0;
               }
               else
               {
                  axes[i-3] = 4;
               }

               m0 = Matrix33.identity();

               e0 = new EulerAngles( m0, axes );

               //
               // If an exception is *not* thrown, we'll hit this call.
               //

               Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

            }
            catch ( SpiceException ex )
            {
               ok = JNITestutils.chckth ( true, "SPICE(INDEXOUTOFRANGE)", ex );
            }
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: angles/axes array constructor: " +
                              "invalid axis sequence."                   );

         try
         {
            angles  = new double[3];
            axes    = new int[3];

            axes[0] = 1;
            axes[1] = 3;
            axes[2] = 3;

            e0     = new EulerAngles( angles, axes );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BADAXISNUMBERS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(BADAXISNUMBERS)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: angles/axes scalar constructor: " +
                              "invalid axis sequence."                   );

         try
         {
            angles  = new double[3];
            axes    = new int[3];

            axes[0] = 1;
            axes[1] = 3;
            axes[2] = 3;


            e0     = new EulerAngles( angles[0], angles[1], angles[2],
                                      axes[0],   axes[1],   axes[2]   );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BADAXISNUMBERS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(BADAXISNUMBERS)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: angles/axes scalar constructor " +
                              "w/units: invalid axis sequence."              );

         try
         {
            angles  = new double[3];
            axes    = new int[3];

            axes[0] = 1;
            axes[1] = 3;
            axes[2] = 3;


            e0     = new EulerAngles( angles[0], angles[1], angles[2], DEGREES,
                                      axes[0],   axes[1],   axes[2]          );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BADAXISNUMBERS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(BADAXISNUMBERS)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: matrix constructor: " +
                              "invalid axis sequence."         );

         try
         {
            angles  = new double[3];
            axes    = new int[3];

            axes[0] = 1;
            axes[1] = 3;
            axes[2] = 3;


            e0  = new EulerAngles( Matrix33.identity(), axes );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BADAXISNUMBERS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(BADAXISNUMBERS)", ex );
         }



         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test angle/axis array constructor. " +
                              " Also test getAngles() (i.e. without " +
                              "units) and getAxes."   );


         xAngles    = new double[3];
         xAxes      = new int[3];

         xAngles[0] = 10.0 * RPD;
         xAngles[1] = 20.0 * RPD;
         xAngles[2] = 30.0 * RPD;

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 3;

         e0         = new EulerAngles( xAngles, xAxes );

         angles     = e0.getAngles();

         ok = JNITestutils.chckad ( "angles",
                                    angles,
                                    "~~",
                                    xAngles,
                                    TIGHT_TOL    );

         axes      = e0.getAxes();

         ok = JNITestutils.chckai ( "axes",
                                    axes,
                                    "=",
                                    xAxes   );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test angle/axis scalar constructor. " +
                              "Also test getAngles() (i.e. without units) " +
                              "and getAxes."   );


         xAngles    = new double[3];
         xAxes      = new int[3];

         xAngles[0] = 10.0 * RPD;
         xAngles[1] = 20.0 * RPD;
         xAngles[2] = 30.0 * RPD;

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 3;


         e0     = new EulerAngles( xAngles[0], xAngles[1], xAngles[2],
                                   xAxes[0],   xAxes[1],   xAxes[2]    );

         angles     = e0.getAngles();

         ok = JNITestutils.chckad ( "angles",
                                    angles,
                                    "~~",
                                    xAngles,
                                    TIGHT_TOL    );

         axes      = e0.getAxes();

         ok = JNITestutils.chckai ( "axes",
                                    axes,
                                    "=",
                                    xAxes   );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test angle/axis scalar constructor w/units. " +
                              "Also test getAngles() (i.e. without units) " +
                              "and getAxes."   );


         xAngles    = new double[3];
         xAxes      = new int[3];

         xAngles[0] = 10.0;
         xAngles[1] = 20.0;
         xAngles[2] = 30.0;

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 3;


         e0    = new EulerAngles( xAngles[0], xAngles[1], xAngles[2], DEGREES,
                                  xAxes[0],   xAxes[1],   xAxes[2]           );

         angles     = e0.getAngles();

         //
         // The angles were input as degrees but we're extracting them as
         // radians.
         //
         for ( i = 0;  i < 3;  i++ )
         {
            xAngles[i] *= RPD;
         }

         ok = JNITestutils.chckad ( "angles",
                                    angles,
                                    "~~",
                                    xAngles,
                                    TIGHT_TOL    );

         axes      = e0.getAxes();

         ok = JNITestutils.chckai ( "axes",
                                    axes,
                                    "=",
                                    xAxes   );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test matrix constructor.  Also test " +
                              "getAngles() (i.e. without units) and " +
                              "getAxes."   );


         xAngles    = new double[3];
         xAxes      = new int[3];

         xAngles[0] = 10.0 * RPD;
         xAngles[1] = 20.0 * RPD;
         xAngles[2] = 30.0 * RPD;

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 3;

         //
         // Create a rotation matrix corresponding to the angles we've
         // selected.
         //
         m2 = new Matrix33( xAxes[2], xAngles[2] );
         m1 = new Matrix33( xAxes[1], xAngles[1] );
         m0 = new Matrix33( xAxes[0], xAngles[0] );

         m3 = m1.mxm( m2 );
         m3 = m0.mxm( m3 );

         e0 = new EulerAngles( m3, xAxes );

         angles = e0.getAngles();

         ok = JNITestutils.chckad ( "angles",
                                    angles,
                                    "~~",
                                    xAngles,
                                    TIGHT_TOL    );

         axes = e0.getAxes();

         ok = JNITestutils.chckai ( "axes",
                                    axes,
                                    "=",
                                    xAxes   );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );



         xAngles    = new double[3];
         xAxes      = new int[3];

         xAngles[0] = 10.0 * RPD;
         xAngles[1] = 20.0 * RPD;
         xAngles[2] = 30.0 * RPD;

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 3;

         e0 = new EulerAngles( xAngles, xAxes );
         e1 = new EulerAngles( xAngles, xAxes );

         e2 = new EulerAngles( e0 );

         //
         // Change e0; make sure e2 doesn't change.
         //

         e0 = new EulerAngles( 0.4, 0.5, 0.6, 2, 1, 2 );

         ok = JNITestutils.chckad ( "angles",
                                    e2.getAngles(),
                                    "~~",
                                    e1.getAngles(),
                                    TIGHT_TOL    );

         ok = JNITestutils.chckai ( "axes",
                                    e2.getAxes(),
                                    "=",
                                    e1.getAxes()  );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getAngles( AngularUnits )."   );


         xAngles    = new double[3];
         xAxes      = new int[3];

         xAngles[0] = 10.0 * RPD;
         xAngles[1] = 20.0 * RPD;
         xAngles[2] = 30.0 * RPD;

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 3;


         e0 = new EulerAngles( xAngles, xAxes );

         angles = e0.getAngles( DEGREES );

         for ( i = 0;  i < 3;  i++ )
         {
            xAngles[i] *= DPR;
         }


         ok = JNITestutils.chckad ( "angles",
                                    angles,
                                    "~~",
                                    xAngles,
                                    TIGHT_TOL    );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toMatrix."  );


         xAngles    = new double[3];
         xAxes      = new int[3];

         xAngles[0] = 10.0 * RPD;
         xAngles[1] = 20.0 * RPD;
         xAngles[2] = 30.0 * RPD;

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 3;

         //
         // Create a rotation matrix corresponding to the angles we've
         // selected.
         //
         m2 = new Matrix33( xAxes[2], xAngles[2] );
         m1 = new Matrix33( xAxes[1], xAngles[1] );
         m0 = new Matrix33( xAxes[0], xAngles[0] );

         m3    = m1.mxm( m2 );
         xMat0 = m0.mxm( m3 );

         e0 = new EulerAngles( xMat0, xAxes );

         m4 = e0.toMatrix();

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( "m4 row " +i,
                                       m4.toArray()[i],
                                       "~~",
                                       xMat0.toArray()[i],
                                       TIGHT_TOL          );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString." );



         xAngles    = new double[3];
         xAxes      = new int[3];

         xAngles[0] = -1.e-100 * RPD;
         xAngles[1] = -2.e-200 * RPD;
         xAngles[2] = -3.e-300 * RPD;

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 3;


         e0 = new EulerAngles( xAngles, xAxes );




         xstr0 = "[-1.0000000000000000e-100 (deg)]  " +
                 "[-2.0000000000000000e-200 (deg)]  " +
                 "[-3.0000000000000000e-300 (deg)]"   + endl +
                 "                                3 " +
                 "                                1 " +
                 "                                3";

         outStr = e0.toString();

         // For debugging:
         // System.out.println( xstr0  );
         // System.out.println( outStr );


         ok = JNITestutils.chcksc( "outStr",
                                   outStr,
                                   "=",
                                   xstr0     );
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
         // Get rid of the PCK file.
         //
         ( new File ( PCK    ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

