
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestRotationAndAV provides methods that implement test families for
the class RotationAndAV.

<p>Version 1.0.0 03-DEC-2009 (NJB)
*/
public class TestRotationAndAV extends Object
{

   //
   // Class constants
   //
   private static String  PCK        = "matrix33.tpc";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test RotationAndAV and associated classes.
   */
   public static boolean f_RotationAndAV()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-9;

      final double                      SQ2       = Math.sqrt(2.0);
      final double                      SQ3       = Math.sqrt(3.0);

      //
      // Local variables
      //
      Matrix33                          dm0;
      Matrix33                          m0;
      Matrix33                          m1;
      Matrix33                          m2;
      Matrix33                          omega;

      Matrix66                          xform0;
      Matrix66                          xform1;

      ReferenceFrame                    J2000    =
                                        new ReferenceFrame( "J2000"    );

      ReferenceFrame                    IAU_MARS =
                                        new ReferenceFrame( "IAU_MARS" );


      RotationAndAV                     rav0;
      RotationAndAV                     rav1;
      RotationAndAV                     rav2;

      TDBTime                           et0;

      Vector3                           av;
      Vector3                           xav;

      boolean                           isrot;
      boolean                           ok;

      double[][]                        eltArray;
      double[][]                        omegaElts;

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

         JNITestutils.topen ( "f_RotationAndAV" );


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
         JNITestutils.tcase (  "Error: matrix-vector constructor: " +
                               "input matrix is not a rotation."      );

         try
         {
            m0   = new Matrix33( new Vector3( 1.0, 0.0, 0.0 ),
                                 new Vector3( 0.0, 1.0, 0.0 ),
                                 new Vector3( 1.0, 0.0, 1.0 )  );

            av   = new Vector3 ( 0.0, 0.0, 1.0 );

            rav0 = new RotationAndAV( m0, av );

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


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test matrix66-based constructor." );

         et0    = new TDBTime( "2009 DEC 3" );

         xform0 = J2000.getStateTransformation( IAU_MARS, et0 );

         rav0   = new RotationAndAV( xform0 );

         //
         // Test rotation matrix.
         //

         m0 = rav0.getRotation();
         m1 = xform0.getBlock(0,0);

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Rotation row " + i),
                                       m0.toArray()[i],
                                       "=",
                                       m1.toArray()[i],
                                       0.0                    );
         }

         //
         // Create angular velocity from the state trasformation.
         // First compute
         //
         //                   t
         //    omega = ( d( m0 )/dt ) * m0
         //

         dm0       = xform0.getBlock(1,0);

         omega     = dm0.mtxm( m0 );

         omegaElts = omega.toArray();

         //
         // Pick off the angular velocity components from omega.
         // (See rotation.req for a description of the cross
         // product matrix.)
         //

         xav = new Vector3(  -omegaElts[1][2],
                              omegaElts[0][2],
                             -omegaElts[0][1] );

         av  = rav0.getAngularVelocity();

         // For debugging:
         //
         // System.out.println ( "av: " + av );
         // System.out.println ( "xav: " + xav );

         ok = JNITestutils.chckad ( "av",
                                    av.toArray(),
                                    "~~",
                                    xav.toArray(),
                                    TIGHT_TOL       );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test matrix-vector constructor." );

         //
         // Use m0 and av from the previous case.
         //

         rav1 = new RotationAndAV( m0, xav );

         m1 = rav1.getRotation();


         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Rotation row " + i),
                                       m1.toArray()[i],
                                       "=",
                                       m0.toArray()[i],
                                       0.0                    );
         }

         //
         // Use a tolerance value of zero for the AV test as well.
         //

         av  = rav1.getAngularVelocity();


         ok = JNITestutils.chckad ( "av",
                                    av.toArray(),
                                    "=",
                                    xav.toArray(),
                                    0.0           );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );


         // Use m0 and av from the previous case.
         //

         rav0 = new RotationAndAV( m0, xav );
         rav1 = new RotationAndAV( m0, xav );

         rav2 = new RotationAndAV( rav0 );

         //
         // Make sure that changing rav0 doesn't affect rav2.
         //

         rav0 = new RotationAndAV( m0.xpose(), xav.scale(3.0) );


         //
         // Use a tolerance value of zero for this test.
         //
         m2 = rav2.getRotation();
         m1 = rav1.getRotation();

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Rotation row " + i),
                                       m2.toArray()[i],
                                       "=",
                                       m1.toArray()[i],
                                       0.0                    );
         }

         //
         // Use a tolerance value of zero for the AV test as well.
         //

         av  = rav2.getAngularVelocity();
         xav = rav1.getAngularVelocity();

         ok = JNITestutils.chckad ( "av",
                                    av.toArray(),
                                    "=",
                                    xav.toArray(),
                                    0.0           );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toMatrix." );

         et0    = new TDBTime( "2009 DEC 3" );

         xform0 = J2000.getStateTransformation( IAU_MARS, et0 );

         rav0   = new RotationAndAV( xform0 );

         xform1 = rav0.toMatrix();

         for ( i = 0;  i < 6;  i++ )
         {
            ok = JNITestutils.chckad ( ("State transformation row " + i),
                                       xform1.toArray()[i],
                                       "~~",
                                       xform0.toArray()[i],
                                       TIGHT_TOL                           );
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

