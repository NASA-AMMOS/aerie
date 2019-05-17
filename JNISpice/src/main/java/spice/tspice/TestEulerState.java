
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.*;

/**
Class TestEulerState provides methods that implement test families for
the class EulerState.

<h3>Version 3.0.0 29-DEC-2016 (NJB)</h3>

   Moved clean-up code to "finally" block.

<h3>Version 2.0.0 01-SEP-2013 (NJB)</h3>

<h3>Version 1.0.0 07-DEC-2009 (NJB)</h3>
*/
public class TestEulerState extends Object
{

   //
   // Class constants
   //
   private static String  PCK        = "EulerState.tpc";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test EulerState and associated classes.
   */
   public static boolean f_EulerState()

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
      EulerState                        e0;
      EulerState                        e1;
      EulerState                        e2;
      EulerState                        e3;

      Matrix33                          m0;
      Matrix33                          m1;
      Matrix33                          m2;
      Matrix33                          m3;
      Matrix33                          m4;
      Matrix33                          xMat0;

      Matrix66                          trans0;
      Matrix66                          trans1;
      Matrix66                          trans2;
      Matrix66                          trans3;

      ReferenceFrame                    J2000    =
                                        new ReferenceFrame( "J2000"    );

      ReferenceFrame                    IAU_MARS =
                                        new ReferenceFrame( "IAU_MARS" );

      ReferenceFrame                    IAU_MOON =
                                        new ReferenceFrame( "IAU_MOON" );


      RotationAndAV                     rav;
      RotationAndAV                     rav1;


      String                            outStr;
      String                            xstr0;


      TDBTime                           et;

      Vector3                           av;
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
      double[]                          rates;
      double[]                          stateArray0;
      double[]                          stateArray1;
      double                            value;
      double[]                          xAngles;
      double[]                          xRates;
      double[]                          xState;
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

         JNITestutils.topen ( "f_EulerState" );


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
               stateArray0  = new double[6];
               axes         = new int[3];

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

               e0     = new EulerState( stateArray0, axes );

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
               rates   = new double[3];
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

               e0     = new EulerState( angles[0], angles[1], angles[2],
                                        rates[0],  rates[1],  rates[2],
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

               trans0 = Matrix66.identity();

               e0 = new EulerState( trans0, axes );

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
            stateArray0  = new double[6];
            axes         = new int[3];

            axes[0] = 1;
            axes[1] = 3;
            axes[2] = 3;

            e0     = new EulerState( stateArray0, axes );

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
            rates   = new double[3];
            axes    = new int[3];

            axes[0] = 1;
            axes[1] = 3;
            axes[2] = 3;

            e0     = new EulerState( angles[0], angles[1], angles[2],
                                     rates[0],  rates[1],  rates[2],
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
         JNITestutils.tcase ( "Error: matrix constructor: " +
                              "invalid axis sequence."         );

         try
         {
            axes    = new int[3];

            axes[0] = 1;
            axes[1] = 3;
            axes[2] = 3;


            e0  = new EulerState( Matrix66.identity(), axes );

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

         JNITestutils.tcase ( "Test state/axis array constructor. " +
                              "Also test getAngles, getRates, and getAxes."  );


         xState    = new double[6];
         xAngles   = new double[3];
         xRates    = new double[3];

         xAxes      = new int[3];

         xState[0] = 10.0   * RPD;
         xState[1] = 20.0   * RPD;
         xState[2] = 30.0   * RPD;
         xState[0] =  1.e-1 * RPD;
         xState[1] =  2.e-1 * RPD;
         xState[2] =  3.e-1 * RPD;


         System.arraycopy( xState, 0, xAngles, 0, 3 );
         System.arraycopy( xState, 3, xRates,  0, 3 );

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 2;

         e0         = new EulerState( xState, xAxes );

         angles     = e0.getAngles();

         ok = JNITestutils.chckad ( "angles",
                                    angles,
                                    "~~",
                                    xAngles,
                                    TIGHT_TOL    );

         rates = e0.getRates();

         ok = JNITestutils.chckad ( "rates",
                                    rates,
                                    "~~",
                                    xRates,
                                    TIGHT_TOL    );

         axes      = e0.getAxes();

         ok = JNITestutils.chckai ( "axes",
                                    axes,
                                    "=",
                                    xAxes   );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test angle/rate/axis scalar constructor. " +
                              "Also test getAngles, getRates and getAxes."   );



         xState    = new double[6];
         xAngles   = new double[3];
         xRates    = new double[3];

         xAxes      = new int[3];

         xState[0] = 10.0   * RPD;
         xState[1] = 20.0   * RPD;
         xState[2] = 30.0   * RPD;
         xState[0] =  1.e-1 * RPD;
         xState[1] =  2.e-1 * RPD;
         xState[2] =  3.e-1 * RPD;


         System.arraycopy( xState, 0, xAngles, 0, 3 );
         System.arraycopy( xState, 3, xRates,  0, 3 );

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 2;


         e0     = new EulerState( angles[0],  angles[1],  angles[2],
                                  xRates[0],  xRates[1],  xRates[2],
                                  xAxes[0],   xAxes[1],   xAxes[2]   );


         angles     = e0.getAngles();

         ok = JNITestutils.chckad ( "angles",
                                    angles,
                                    "~~",
                                    xAngles,
                                    TIGHT_TOL    );

         rates = e0.getRates();

         ok = JNITestutils.chckad ( "rates",
                                    rates,
                                    "~~",
                                    xRates,
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
                              "getAngles, getRates, and getAxes."   );



         xState    = new double[6];
         xAngles   = new double[3];
         xRates    = new double[3];
         xAxes     = new int[3];

         xState[0] = 10.0   * RPD;
         xState[1] = 20.0   * RPD;
         xState[2] = 30.0   * RPD;
         xState[0] =  1.e-1 * RPD;
         xState[1] =  2.e-1 * RPD;
         xState[2] =  3.e-1 * RPD;


         System.arraycopy( xState, 0, xAngles, 0, 3 );
         System.arraycopy( xState, 3, xRates,  0, 3 );

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 2;

         //
         // Create a rotation matrix `m3' corresponding to the angles we've
         // selected.
         //
         m2 = new Matrix33( xAxes[2], xAngles[2] );
         m1 = new Matrix33( xAxes[1], xAngles[1] );
         m0 = new Matrix33( xAxes[0], xAngles[0] );

         m3 = m1.mxm( m2 );
         m3 = m0.mxm( m3 );

         //
         // Create an angular velocity vector from our rates.
         //
         av = new Vector3( xRates );

         //
         // Create a RotationAndAV instance; extract the expected
         // state transformation `trans0' from this instance.
         //
         rav    = new RotationAndAV( m3, av );

         trans0 = rav.toMatrix();


         //
         // Create an EulerState from trans0.
         //
         e0 = new EulerState( trans0, xAxes );

         //
         // Check the recovered angles, rates, and axes.
         //
         angles = e0.getAngles();

         ok = JNITestutils.chckad ( "angles",
                                    angles,
                                    "~~",
                                    xAngles,
                                    TIGHT_TOL    );


         rates = e0.getRates();

         ok = JNITestutils.chckad ( "rates",
                                    rates,
                                    "~~",
                                    xRates,
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



         xState    = new double[6];
         xAngles   = new double[3];
         xRates    = new double[3];
         xAxes     = new int[3];

         xState[0] = 10.0   * RPD;
         xState[1] = 20.0   * RPD;
         xState[2] = 30.0   * RPD;
         xState[0] =  1.e-1 * RPD;
         xState[1] =  2.e-1 * RPD;
         xState[2] =  3.e-1 * RPD;


         System.arraycopy( xState, 0, xAngles, 0, 3 );
         System.arraycopy( xState, 3, xRates,  0, 3 );

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 2;

         e0 = new EulerState( xState, xAxes );
         e1 = new EulerState( xState, xAxes );

         e2 = new EulerState( e0 );

         //
         // Change e0; make sure e2 doesn't change.
         //

         e0 = new EulerState( 0.4, 0.5, 0.6, -0.4, -0.5, -0.6, 2, 1, 2 );

         ok = JNITestutils.chckad ( "angles",
                                    e2.getAngles(),
                                    "~~",
                                    e1.getAngles(),
                                    TIGHT_TOL    );
         rates = e2.getRates();

         ok = JNITestutils.chckad ( "rates",
                                    rates,
                                    "~~",
                                    e1.getRates(),
                                    TIGHT_TOL    );


         ok = JNITestutils.chckai ( "axes",
                                    e2.getAxes(),
                                    "=",
                                    e1.getAxes()  );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toArray." );


         xState    = new double[6];
         xAngles   = new double[3];
         xRates    = new double[3];

         xAxes      = new int[3];

         xState[0] = 10.0   * RPD;
         xState[1] = 20.0   * RPD;
         xState[2] = 30.0   * RPD;
         xState[0] =  1.e-1 * RPD;
         xState[1] =  2.e-1 * RPD;
         xState[2] =  3.e-1 * RPD;


         System.arraycopy( xState, 0, xAngles, 0, 3 );
         System.arraycopy( xState, 3, xRates,  0, 3 );

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 2;

         e0         = new EulerState( xState, xAxes );


         ok = JNITestutils.chckad ( "state array",
                                    e0.toArray(),
                                    "~~",
                                    xState,
                                    TIGHT_TOL    );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toMatrix: recover state and axes."  );


         xState    = new double[6];
         xAngles   = new double[3];
         xRates    = new double[3];
         xAxes     = new int[3];

         xState[0] = 10.0   * RPD;
         xState[1] = 20.0   * RPD;
         xState[2] = 30.0   * RPD;
         xState[0] =  1.e-1 * RPD;
         xState[1] =  2.e-1 * RPD;
         xState[2] =  3.e-1 * RPD;


         System.arraycopy( xState, 0, xAngles, 0, 3 );
         System.arraycopy( xState, 3, xRates,  0, 3 );

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 2;

         //
         // Create a rotation matrix `m3' corresponding to the angles we've
         // selected.
         //
         m2 = new Matrix33( xAxes[2], xAngles[2] );
         m1 = new Matrix33( xAxes[1], xAngles[1] );
         m0 = new Matrix33( xAxes[0], xAngles[0] );

         m3 = m1.mxm( m2 );
         m3 = m0.mxm( m3 );

         //
         // Create an angular velocity vector from our rates.
         //
         av = new Vector3( xRates );

         //
         // Create a RotationAndAV instance; extract the expected
         // state transformation `trans0' from this instance.
         //
         rav    = new RotationAndAV( m3, av );

         trans0 = rav.toMatrix();


         //
         // Create an EulerState from trans0.
         //
         e0 = new EulerState( trans0, xAxes );


         //
         // Convert the EulerState e0 back to a Matrix; from this
         // Matrix recover the input rotation and angular velocity.
         //

         trans1 = e0.toMatrix();

         rav1   = new RotationAndAV( trans1 );

         ok = JNITestutils.chckad ( "angular velocity",
                                    rav1.getAngularVelocity().toArray(),
                                    "~~",
                                    av.toArray(),
                                    TIGHT_TOL    );
         //
         // Check the rotation against `m3'.
         //
         m4 = rav1.getRotation();

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( "m4 row " +i,
                                       m4.toArray()[i],
                                       "~~",
                                       m3.toArray()[i],
                                       TIGHT_TOL          );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString." );

         xState    = new double[6];

         xState[0] = -1.e-100 * RPD;
         xState[1] = -2.e-100 * RPD;
         xState[2] = -3.e-100 * RPD;
         xState[3] = -4.e-100 * RPD;
         xState[4] = -5.e-100 * RPD;
         xState[5] = -6.e-100 * RPD;

         xAxes      = new int[3];

         xAxes[0]   = 3;
         xAxes[1]   = 1;
         xAxes[2]   = 2;
         xAngles    = new double[3];


         e0 = new EulerState( xState, xAxes );




         xstr0 = "[-1.0000000000000000e-100 (deg)]  " +
                 "[-2.0000000000000000e-100 (deg)]  " +
                 "[-3.0000000000000000e-100 (deg)]"   + endl +
                 "                                3 " +
                 "                                1 " +
                 "                                2"  + endl +
                 " -4.0000000000000000e-100         " +
                 " -5.0000000000000000e-100         " +
                 " -6.0000000000000000e-100"          + endl;

         outStr = e0.toString();

         // For debugging:
         //System.out.println( xstr0  );
         //System.out.println( outStr );


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

