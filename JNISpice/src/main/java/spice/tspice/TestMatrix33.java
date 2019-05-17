
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestMatrix33 provides methods that implement test families for
the class Matrix33.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 13-DEC-2009 (NJB)</h3>
*/
public class TestMatrix33 extends Object
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
   Test Matrix33 and associated classes.
   */
   public static boolean f_Matrix33()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 2.e-6;
      final double                      SQ2       = Math.sqrt(2.0);
      final double                      SQ3       = Math.sqrt(3.0);

      //
      // Local variables
      //
      Matrix33                          m0;
      Matrix33                          m1;
      Matrix33                          m2;
      Matrix33                          m3;
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
      double[][]                        eltArray;
      double                            det;
      double                            dist;
      double                            elt0;
      double                            elt1;
      double                            elt2;
      double                            value;
      double[]                          xArray;
      double[][]                        xArray2D;
      double                            xElt;

      int                               from;
      int                               i;
      int                               j;
      int                               to;


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

         JNITestutils.topen ( "f_Matrix33" );


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
         JNITestutils.tcase (  "Error: getElt: indices out of range." );

         try
         {
            m0   = new Matrix33();

            elt0 = m0.getElt( -1, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }

         try
         {
            m0   = new Matrix33();

            elt0 = m0.getElt(  3, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }

         try
         {
            m0   = new Matrix33();

            elt0 = m0.getElt(  0, -1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }

         try
         {
            m0   = new Matrix33();

            elt0 = m0.getElt(  0, 3 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: attempt to create Matrix33 from " +
                               "array of wrong size."                     );

         try
         {

            double[][] bigArray = new double[4][4];

            m0 = new Matrix33( bigArray );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INVALIDSIZE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INVALIDSIZE)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: attempt to create Matrix33 from " +
                               "1-D array of wrong size."                     );

         try
         {

            double[] bigArray = new double[10];

            m0 = new Matrix33( bigArray );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INVALIDSIZE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INVALIDSIZE)", ex );
         }


         try
         {

            double[] bigArray = new double[3];

            m0 = new Matrix33( bigArray );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INVALIDSIZE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INVALIDSIZE)", ex );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: \"twovec\" constructor: indices out " +
                              "of range." );

         try
         {
            v0   = new Vector3( 1.0, 0.0, 0.0 );
            v1   = new Vector3( 0.0, 1.0, 0.0 );

            m0   = new Matrix33( v0, 0, v1, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }

         try
         {
            v0   = new Vector3( 1.0, 0.0, 0.0 );
            v1   = new Vector3( 0.0, 1.0, 0.0 );

            m0   = new Matrix33( v0, 4, v1, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }

         try
         {
            v0   = new Vector3( 1.0, 0.0, 0.0 );
            v1   = new Vector3( 0.0, 1.0, 0.0 );

            m0   = new Matrix33( v0, 2, v1, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }

         try
         {
            v0   = new Vector3( 1.0, 0.0, 0.0 );
            v1   = new Vector3( 0.0, 1.0, 0.0 );

            m0   = new Matrix33( v0, 2, v1, 4 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: \"rotate\" constructor: index out " +
                              "of range." );

         try
         {
            m0   = new Matrix33( 0, 0.0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }

         try
         {
            m0   = new Matrix33( 4, 0.0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }





         //
         // --------Case-----------------------------------------------
         //

         //
         // This error case should be added if the inversion spec is
         // changed to reject singular inputs. Currently the output
         // is set to the zero matrix.
         //

         /*
         JNITestutils.tcase (  "Attempt to invert a singular matrix." );

         try
         {

            m0 = new Matrix33();

            m1 = m0.invert();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DEGENERATECASE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(DEGENERATECASE)", ex );
         }
         */





         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test zero-args constructor." );

         m0       = new Matrix33();

         eltArray = m0.toArray();

         xArray   = new double[3];

         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                        eltArray[i],
                                        "=",
                                        xArray,
                                        0.0          );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test row-vector constructor." );

         rows     = new Vector3[3];

         rows[0]  = new Vector3( 1.0, 2.0, 3.0 );
         rows[1]  = new Vector3( 4.0, 5.0, 6.0 );
         rows[2]  = new Vector3( 7.0, 8.0, 9.0 );

         m0       = new Matrix33( rows[0], rows[1], rows[2] );

         eltArray = m0.toArray();

         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                        eltArray[i],
                                        "=",
                                       rows[i].toArray(),
                                        0.0               );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test double[][] constructor." );


         xArray2D = new double[3][3];

         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] =  1.0 + i*3 + j;
            }
         }


         m0       = new Matrix33( xArray2D );
         eltArray = m0.toArray();

         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                        eltArray[i],
                                        "=",
                                        xArray2D[i],
                                        0.0               );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test double[] constructor." );


         xArray = new double[9];

         for ( i = 0;  i < 9;  i++ )
         {
            xArray[i] =  i;
         }

         from = 0;

         for ( i = 0;  i < 3;  i++ )
         {
            System.arraycopy( xArray, from, xArray2D[i], 0, 3 );

            from += 3;
         }


         m0       = new Matrix33( xArray );
         eltArray = m0.toArray();

         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                        eltArray[i],
                                        "=",
                                        xArray2D[i],
                                        0.0               );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );


         xArray2D = new double[3][3];

         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] =  1.0 + i*3 + j;
            }
         }


         m0       = new Matrix33( xArray2D );
         m2       = new Matrix33( xArray2D );

         m1       = new Matrix33( m0 );

         //
         // Change m0; make sure m1 doesn't change.
         //
         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] =  ( 1.0 + i*3 + j ) * 5;
            }
         }

         m0 = new Matrix33( xArray2D );


         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m1.toArray()[i],
                                       "=",
                                       m2.toArray()[i],
                                       0.0               );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test \"axisar\" constructor." );

         v0    = new Vector3( 0.0, 0.0, 1.0 );
         angle = -Math.PI/4;

         m0    = new Matrix33( v0, angle );


         m1    = m0.mxm( m0 );

         m2    = new Matrix33 ( new Vector3(  0.0, 1.0, 0.0 ),
                                new Vector3( -1.0, 0.0, 0.0 ),
                                new Vector3(  0.0, 0.0, 1.0 )  );

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("m1 Row " + i),
                                       m1.toArray()[i],
                                       "~~",
                                       m2.toArray()[i],
                                       TIGHT_TOL               );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test \"rotate\" constructor." );

         v0    = new Vector3( 0.0, 0.0, 1.0 );
         angle = Math.PI/4;

         m0    = new Matrix33( v0, -angle );


         m1    = new Matrix33( 3,  angle );


         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("m0 Row " + i),
                                       m0.toArray()[i],
                                       "~~",
                                       m1.toArray()[i],
                                       TIGHT_TOL               );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test \"twovec\" constructor." );

         v0    = new Vector3( 0.0, 0.0, 1.0 );
         v1    = new Vector3( 1.0, 1.0, 1.0 );


         rows    = new Vector3[3];

         rows[0] = v1.hat();

         rows[1] = ( new Vector3( -1.0, 1.0, 0.0 ) ).hat();

         rows[2] = ( new Vector3( -1.0, -1.0, 2.0 ) ).hat();

         m0    = new Matrix33( v1, 1, v0, 3 );

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("m0 Row " + i),
                                       m0.toArray()[i],
                                       "~~",
                                       rows[i].toArray(),
                                       TIGHT_TOL               );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test add." );



         rows     = new Vector3[3];

         rows[0]  = new Vector3( 1.0, 2.0, 3.0 );
         rows[1]  = new Vector3( 4.0, 5.0, 6.0 );
         rows[2]  = new Vector3( 7.0, 8.0, 9.0 );

         m0       = new Matrix33( rows[0], rows[1], rows[2] );


         rows[0]  = new Vector3( 2.0, 3.0,  4.0 );
         rows[1]  = new Vector3( 5.0, 6.0,  7.0 );
         rows[2]  = new Vector3( 8.0, 9.0, 10.0 );

         m1       = new Matrix33( rows[0], rows[1], rows[2] );


         m2       = m0.add( m1 );


         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] =  ( 1.0 + i*3 + j ) * 2   +   1;
            }
         }

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m2.toArray()[i],
                                       "~~",
                                       xArray2D[i],
                                       TIGHT_TOL        );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test det: singular case." );



         rows     = new Vector3[3];

         rows[0]  = new Vector3( 1.0, 2.0, 3.0 );
         rows[1]  = new Vector3( 4.0, 5.0, 6.0 );
         rows[2]  = new Vector3( 7.0, 8.0, 9.0 );

         m0       = new Matrix33( rows[0], rows[1], rows[2] );

         det      = m0.det();

         ok = JNITestutils.chcksd ( "det",
                                    det,
                                    "~",
                                    0.0,
                                    TIGHT_TOL );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test det: non-singular case." );



         rows     = new Vector3[3];

         rows[0]  = new Vector3( 0.0, 1.0, 0.0 );
         rows[1]  = new Vector3( 0.0, 0.0, 1.0 );
         rows[2]  = new Vector3( 1.0, 0.0, 0.0 );

         m0       = new Matrix33( rows[0], rows[1], rows[2] );

         det      = m0.det();

         ok = JNITestutils.chcksd ( "det",
                                    det,
                                    "~",
                                    1.0,
                                    TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test dist." );


         rows     = new Vector3[3];

         rows[0]  = new Vector3( 1.0, 2.0, 3.0 );
         rows[1]  = new Vector3( 4.0, 5.0, 6.0 );
         rows[2]  = new Vector3( 7.0, 8.0, 9.0 );

         m0       = new Matrix33( rows[0], rows[1], rows[2] );


         rows[0]  = new Vector3( 2.0, 3.0,  4.0 );
         rows[1]  = new Vector3( 5.0, 6.0,  7.0 );
         rows[2]  = new Vector3( 8.0, 9.0, 10.0 );

         m1       = new Matrix33( rows[0], rows[1], rows[2] );


         dist     = m0.dist( m1 );

         ok = JNITestutils.chcksd ( "dist",
                                    dist,
                                    "~",
                                    3.0,
                                    TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test fill." );


         value = 888.0;

         m0 = Matrix33.fill( value );


         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               //
               // We expect equality in this test.
               //
               ok = JNITestutils.chcksd ( ("Elt[" + i + "][" + j + "]"),
                                          m0.getElt(i,j),
                                          "=",
                                          value,
                                          0.0                           );
            }
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getElt." );

         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] =  ( 1.0 + i*3 + j ) * 2   +   1;
            }
         }

         m0 = new Matrix33( xArray2D );



         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               //
               // We expect equality in this test.
               //
               ok = JNITestutils.chcksd ( ("Elt[" + i + "][" + j + "]"),
                                          m0.getElt(i,j),
                                          "=",
                                          xArray2D[i][j],
                                          0.0                    );
            }
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test identity." );


         m0 = Matrix33.identity();


         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               if ( i == j )
               {
                  value = 1.0;
               }
               else
               {
                  value = 0.0;
               }

               //
               // We expect equality in this test.
               //
               ok = JNITestutils.chcksd ( ("Elt[" + i + "][" + j + "]"),
                                          m0.getElt(i,j),
                                          "=",
                                          value,
                                          0.0                           );
            }
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test invert." );

         rows     = new Vector3[3];

         rows[0]  = new Vector3( 0.0, 1.0, 0.0 );
         rows[1]  = new Vector3( 0.0, 0.0, 1.0 );
         rows[2]  = new Vector3( 1.0, 0.0, 0.0 );

         m0       = new Matrix33( rows[0], rows[1], rows[2] );


         rows[0]  = new Vector3( 0.0, 0.0, 1.0 );
         rows[1]  = new Vector3( 1.0, 0.0, 0.0 );
         rows[2]  = new Vector3( 0.0, 1.0, 0.0 );

         m1       = new Matrix33( rows[0], rows[1], rows[2] );

         m2       = m0.invert();


         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m2.toArray()[i],
                                       "~~",
                                       m1.toArray()[i],
                                       TIGHT_TOL        );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test isRotation" );


         //
         // Start by creating a matrix with valid column lengths but
         // a determinant that is too small.
         //

         eltArray = new double[3][3];

         eltArray[0][0] = 1.0;
         eltArray[1][0] = 0.0;
         eltArray[2][0] = 0.0;

         eltArray[0][1] = 0.0;
         eltArray[1][1] = 1.0;
         eltArray[2][1] = 0.0;

         eltArray[0][2] = 0.0;
         eltArray[1][2] = SQ2/2;
         eltArray[2][2] = SQ2/2;

         m0 = new Matrix33( eltArray );

         isrot = m0.isRotation( 1.e-12, 1.e-12 );

         ok = JNITestutils.chcksl ( "m0 isrot", isrot, false );


         //
         // Now try one where the determinant is valid but
         // one vector is too long.
         //

         eltArray[0][0] = 1.0;
         eltArray[1][0] = 0.0;
         eltArray[2][0] = 0.0;

         eltArray[0][1] = 0.0;
         eltArray[1][1] = 2.0;
         eltArray[2][1] = 0.0;

         eltArray[0][2] = 0.0;
         eltArray[1][2] = SQ3/2;
         eltArray[2][2] = 0.5;

         m1 = new Matrix33( eltArray );

         // For debugging:
         //System.out.println( ">>>>>>>>>det = " + m1.det() );

         isrot = m1.isRotation( 1.e-12, 1.e-12 );

         ok = JNITestutils.chcksl ( "m1 isrot", isrot, false );


         //
         // Try a case where the matrix *is* a rotation.
         //
         isrot = (Matrix33.identity()).isRotation( 1.e-12, 1.e-12 );

         ok = JNITestutils.chcksl ( "identity isrot", isrot, true );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test mtxm." );

         et = new TDBTime( 0.0 );

         m0 = IAU_MARS.getPositionTransformation   ( J2000,    et );
         m1 = IAU_MOON.getPositionTransformation   ( J2000,    et );
         m2 = IAU_MARS.getPositionTransformation   ( IAU_MOON, et );

         m3 = m1.mtxm( m0 );

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m3.toArray()[i],
                                       "~~",
                                       m2.toArray()[i],
                                       TIGHT_TOL        );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test mtxv." );

         et = new TDBTime( 0.0 );

         m0 = J2000.getPositionTransformation   ( IAU_MARS, et );
         m1 = IAU_MARS.getPositionTransformation( J2000,    et );

         v0 = new Vector3( 1.0, 2.0, 3.0 );

         vout0 = m0.mtxv( v0 );
         vout1 = m1.mxv ( v0 );

         ok = JNITestutils.chckad ( "vout0",
                                    vout0.toArray(),
                                    "~",
                                    vout1.toArray(),
                                    TIGHT_TOL         );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test mxm." );

         et = new TDBTime( 0.0 );

         m0 = J2000.getPositionTransformation   ( IAU_MARS, et );
         m1 = IAU_MARS.getPositionTransformation( J2000,    et );

         m2 = m0.mxm( m1 );

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m2.toArray()[i],
                                       "~~",
                                       (Matrix33.identity()).toArray()[i],
                                       TIGHT_TOL        );
         }





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test mxmt." );

         et = new TDBTime( 0.0 );

         m0 = J2000.getPositionTransformation      ( IAU_MARS, et );
         m1 = J2000.getPositionTransformation      ( IAU_MOON, et );
         m2 = IAU_MARS.getPositionTransformation   ( IAU_MOON, et );

         m3 = m1.mxmt( m0 );

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m3.toArray()[i],
                                       "~~",
                                       m2.toArray()[i],
                                       TIGHT_TOL        );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test mxv." );

         et = new TDBTime( 0.0 );

         m0 = J2000.getPositionTransformation( IAU_MARS, et );


         for ( i = 0;  i < 3;  i++ )
         {
            rows[i] = new Vector3( m0.toArray()[i] );
         }

         //
         // Since m0 is orthogonal, it maps its own ith row to the
         // ith basis vector, which is the ith row of the identity matrix.
         //
         for ( i = 0;  i < 3;  i++ )
         {
            vout0 = m0.mxv( rows[i] );

            ok = JNITestutils.chckad ( ("Row " + i),
                                       vout0.toArray(),
                                       "~~",
                                       (Matrix33.identity()).toArray()[i],
                                       TIGHT_TOL        );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test norm." );

         //
         // The norm of an orthogonal 3x3 matrix should be sqrt(3).
         //
         m0 = J2000.getPositionTransformation( IAU_MARS, et );

         ok = JNITestutils.chcksd ( "norm of m0",
                                    m0.norm(),
                                    "~",
                                    SQ3,
                                    TIGHT_TOL        );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test scale." );

         et = new TDBTime( 0.0 );

         m0 = J2000.getPositionTransformation   ( IAU_MARS, et );
         m1 = m0.add  ( m0 );
         m2 = m0.scale( 2.0 );

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m2.toArray()[i],
                                       "~",
                                       m1.toArray()[i],
                                       TIGHT_TOL       );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test sub." );

         et = new TDBTime( 0.0 );

         m0 = J2000.getPositionTransformation   ( IAU_MARS, et );
         m1 = m0.scale( 2.0 );
         m2 = m1.sub  ( m0  );

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m2.toArray()[i],
                                       "~",
                                       m0.toArray()[i],
                                       TIGHT_TOL       );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toArray." );

         rows[0]  = new Vector3( 2.0, 3.0,  4.0 );
         rows[1]  = new Vector3( 5.0, 6.0,  7.0 );
         rows[2]  = new Vector3( 8.0, 9.0, 10.0 );

         m0       = new Matrix33( rows[0], rows[1], rows[2] );


         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] =  2.0 + i*3 + j;
            }
         }

         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m0.toArray()[i],
                                       "~",
                                       xArray2D[i],
                                       TIGHT_TOL       );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toArray1D." );


         xArray = new double[9];

         for ( i = 0;  i < 9;  i++ )
         {
            xArray[i] = i;
         }

         m0 = new Matrix33( xArray );


         ok = JNITestutils.chckad ( "m0",
                                    m0.toArray1D(),
                                    "~",
                                    xArray,
                                    TIGHT_TOL       );






         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString." );

         String endl = System.getProperty( "line.separator" );

         rows[0]  = new Vector3( -1.e-100, -2.e-200, -3.e-300  );
         rows[1]  = new Vector3( -4.e-100, -5.e-200, -6.e-300  );
         rows[2]  = new Vector3( -7.e-100, -8.e-200, -9.e-300  );

         m0       = new Matrix33( rows[0], rows[1], rows[2] );


         //
         // We expect 17 mantissa digits in each component.
         //

         xstr0 = "-1.0000000000000000e-100, " +
                 "-2.0000000000000000e-200, " +
                 "-3.0000000000000000e-300,"  + endl +
                 "-4.0000000000000000e-100, " +
                 "-5.0000000000000000e-200, " +
                 "-6.0000000000000000e-300,"  + endl +
                 "-7.0000000000000000e-100, " +
                 "-8.0000000000000000e-200, " +
                 "-9.0000000000000000e-300";


         outStr = m0.toString();

         // For debugging
         //
         //System.out.println( xstr0  );
         //System.out.println( outStr );

         ok  =  JNITestutils.chcksc( "xvec0.toString() (0)",
                                     outStr,
                                     "=",
                                     xstr0                  );






         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test xpose." );

         //
         // The norm of an orthogonal 3x3 matrix should be sqrt(3).
         //
         m0 = J2000.getPositionTransformation   ( IAU_MARS, et );
         m1 = IAU_MARS.getPositionTransformation( J2000,    et );

         m2 = m0.xpose();


         for ( i = 0;  i < 3;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m2.toArray()[i],
                                       "~~",
                                       m1.toArray()[i],
                                       TIGHT_TOL        );
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

