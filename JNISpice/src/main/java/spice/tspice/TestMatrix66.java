
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestMatrix66 provides methods that implement test families for
the class Matrix66.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 04-DEC-2009 (NJB)</h3>
*/
public class TestMatrix66 extends Object
{

   //
   // Class constants
   //
   private static String  PCK        = "matrix66.tpc";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test Matrix66 and associated classes.
   */
   public static boolean f_Matrix66()

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
      Matrix33                          block0;
      Matrix33                          block00;
      Matrix33                          block1;

      Matrix66                          m0;
      Matrix66                          m1;
      Matrix66                          m2;
      Matrix66                          m3;
      Matrix66                          xMat0;

      ReferenceFrame                    J2000    =
                                        new ReferenceFrame( "J2000"    );

      ReferenceFrame                    IAU_MARS =
                                        new ReferenceFrame( "IAU_MARS" );

      ReferenceFrame                    IAU_EARTH =
                                        new ReferenceFrame( "IAU_EARTH" );

      String                            outStr;
      String                            xstr0;


      TDBTime                           et;

      Vector6                           v0;
      Vector6                           v1;
      Vector6                           v3;

      boolean                           ok;

      double[]                          array1D;
      double[][]                        eltArray;
      double                            dist;
      double                            elt0;
      double                            elt1;
      double                            elt2;
      double                            ss;
      double                            value;
      double[]                          xArray;
      double[][]                        xArray2D;
      double                            xElt;

      int                               i;
      int                               j;
      int                               k;
      int                               w;
      int                               start;


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

         JNITestutils.topen ( "f_Matrix66" );


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
            m0   = new Matrix66();

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
            m0   = new Matrix66();

            elt0 = m0.getElt(  6, 0 );

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
            m0   = new Matrix66();

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
            m0   = new Matrix66();

            elt0 = m0.getElt(  0, 6 );

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
         JNITestutils.tcase (  "Error: attempt to create Matrix66 from " +
                               "array of wrong size."                     );

         try
         {

            double[][] bigArray = new double[4][4];

            m0 = new Matrix66( bigArray );

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
         JNITestutils.tcase (  "Error: getBlock: indices out of range." );

         try
         {
            m0      = new Matrix66();

            block00 = m0.getBlock( -1, 0 );

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
            m0      = new Matrix66();

            block00 = m0.getBlock( 2, 0 );

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
            m0      = new Matrix66();

            block00 = m0.getBlock( 0, -1 );

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
            m0      = new Matrix66();

            block00 = m0.getBlock( 0, 2 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );
         }






         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test zero-args constructor." );

         m0       = new Matrix66();

         eltArray = m0.toArray();

         xArray   = new double[6];

         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 6;  i++ )
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

         JNITestutils.tcase ( "Test 1-D array constructor and toArray." );


         array1D = new double[36];

         for ( i = 0;  i < 3;  i++ )
         {
            array1D[i] = i;
         }

         m0 = new Matrix66( array1D );


         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 6;  i++ )
         {
            start = 6*i;

            System.arraycopy( array1D, start, xArray, 0, 6 );


            ok = JNITestutils.chckad ( ("Row " + i),
                                       m0.toArray()[i],
                                        "=",
                                       xArray,
                                        0.0               );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test double[][] constructor." );


         xArray2D = new double[6][6];

         for ( i = 0;  i < 6;  i++ )
         {
            for ( j = 0;  j < 6;  j++ )
            {
               xArray2D[i][j] =  1.0 + i*6 + j;
            }
         }


         m0 = new Matrix66( xArray2D );


         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 6;  i++ )
         {
            ok = JNITestutils.chckad ( ("Row " + i),
                                       m0.toArray()[i],
                                       "=",
                                       xArray2D[i],
                                       0.0               );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );

         xArray2D = new double[6][6];

         for ( i = 0;  i < 6;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] =  1.0 + i*6 + j;
            }
         }

         m0       = new Matrix66( xArray2D );
         m2       = new Matrix66( xArray2D );

         m1       = new Matrix66( m0 );

         //
         // Change m0; make sure m1 doesn't change.
         //
         for ( i = 0;  i < 6;  i++ )
         {
            for ( j = 0;  j < 6;  j++ )
            {
               xArray2D[i][j] =  ( 1.0 + i*3 + j ) * 5;
            }
         }

         m0 = new Matrix66( xArray2D );


         //
         // Use a tolerance value of zero for this test.
         //
         for ( i = 0;  i < 6;  i++ )
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

         JNITestutils.tcase ( "Test GetElt." );

         xArray2D = new double[6][6];

         for ( i = 0;  i < 6;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] =  1.0 + i*6 + j;
            }
         }

         m0 = new Matrix66( xArray2D );


         for ( i = 0;  i < 6;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] = m0.getElt(i,j);
            }
         }

         m1 = new Matrix66( xArray2D );


         ok = JNITestutils.chckad ( "m1",
                                    m1.toArray1D(),
                                    "=",
                                    m0.toArray1D(),
                                    0.0            );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test GetBlock." );

         xArray2D = new double[6][6];

         for ( i = 0;  i < 6;  i++ )
         {
            for ( j = 0;  j < 3;  j++ )
            {
               xArray2D[i][j] =  1.0 + i*6 + j;
            }
         }

         m0 = new Matrix66( xArray2D );


         for ( i = 0;  i < 2;  i++ )
         {
            for ( j = 0;  j < 2;  j++ )
            {

               block0   = m0.getBlock(i,j);

               eltArray = new double[3][3];

               for ( k = 0;  k < 3;  k++ )
               {
                  for ( w = 0;  w < 3;  w++ )
                  {
                     eltArray[k][w] = xArray2D[3*i+k][3*j+w];
                  }
               }

               block1 = new Matrix33( eltArray );

               dist   = block0.dist( block1 );

               ok = JNITestutils.chcksd( "block[" + i + "][" + j+ "]",
                                         dist,
                                         "~",
                                         0.0,
                                         TIGHT_TOL                    );
            }
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test add and toArray1D." );


         array1D = new double[36];

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i;
         }

         m0 = new Matrix66( array1D );


         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = 2*i;
         }

         m1 = new Matrix66( array1D );


         m2 = m0.add( m1 );


         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = 3*i;
         }

         ok = JNITestutils.chckad ( "m2",
                                    m2.toArray1D(),
                                    "=",
                                    array1D,
                                    0.0            );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test scale." );


         array1D = new double[36];

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i;
         }

         m0 = new Matrix66( array1D );


         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = 5*i;
         }

         m1 = new Matrix66( array1D );


         m2 = m0.scale( 5.0 );


         ok = JNITestutils.chckad ( "m2",
                                    m2.toArray1D(),
                                    "=",
                                    m1.toArray1D(),
                                    0.0            );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test sub." );


         array1D = new double[36];

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i;
         }

         m0 = new Matrix66( array1D );


         m1 = m0.scale( 4.0 );


         m2 = m0.sub( m1 );


         m3 = m0.scale( -3.0 );

         ok = JNITestutils.chckad ( "m2",
                                    m2.toArray1D(),
                                    "=",
                                    m3.toArray1D(),
                                    0.0            );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test identity." );


         m0 = Matrix66.identity();


         for ( i = 0;  i < 6;  i++ )
         {
            for ( j = 0;  j < 6;  j++ )
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
               ok = JNITestutils.chcksd ( "Elt[" + i + "][" + j + "]",
                                          m0.getElt(i,j),
                                          "=",
                                          value,
                                          0.0                           );
            }
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test norm." );


         array1D = new double[36];
         ss      = 0.0;

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i / 0.01;

            ss += array1D[i] * array1D[i];
         }

         m0 = new Matrix66( array1D );


         ok = JNITestutils.chcksd ( "norm",
                                    m0.norm(),
                                    "~",
                                    Math.sqrt( ss ),
                                    TIGHT_TOL        );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test dist." );


         array1D = new double[36];
         ss      = 0.0;

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i / 0.01;

            ss += array1D[i] * array1D[i];
         }

         m0 = new Matrix66( array1D );

         m1 = m0.scale( 2.0 );

         ok = JNITestutils.chcksd ( "dist",
                                    m1.dist(m0),
                                    "~",
                                    m1.sub(m0).norm(),
                                    TIGHT_TOL        );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test mxm using state transformations." );

         //
         // This is a sanity check; its purpose is to guard against
         // mimicked errors.
         //
         // Note that this test, while convenient, uses matrices
         // with upper right blocks set to zero.
         //

         et = new TDBTime( "2009 Dec 4" );

         m0 = J2000.getStateTransformation   ( IAU_MARS,  et );
         m1 = IAU_MARS.getStateTransformation( IAU_EARTH, et );
         m2 = J2000.getStateTransformation   ( IAU_EARTH, et );

         m3 = m1.mxm( m0 );


         ok = JNITestutils.chckad ( "m3",
                                    m3.toArray1D(),
                                    "~~",
                                    m2.toArray1D(),
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test mxm using alternate algorithm." );

         array1D = new double[36];

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i / 0.01;
         }

         m0 = new Matrix66( array1D );

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i*i / 0.01;
         }

         m1 = new Matrix66( array1D );

         m2 = m0.mxm(m1);

         //
         // Compute m0*m1 directly.
         //
         xArray2D = new double[6][6];

         for ( i = 0;  i < 6;  i++ )
         {
            for ( j = 0;  j < 6;  j++ )
            {
               xArray2D[i][j] = 0.0;

               for ( k = 0; k < 6; k++ )
               {
                  xArray2D[i][j] += m0.getElt(i,k) * m1.getElt(k,j);
               }
            }
         }

         m3 = new Matrix66( xArray2D );


         ok = JNITestutils.chckad ( "m2",
                                    m2.toArray1D(),
                                    "~~",
                                    m3.toArray1D(),
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test mxv using alternate algorithm." );

         array1D = new double[36];

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i / 0.01;
         }

         m0 = new Matrix66( array1D );

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i*i / 0.01;
         }

         m1 = new Matrix66( array1D );

         m2 = m0.mxm(m1);




         //
         // Compute m0*m1 directly.
         //
         xArray = new double[6];

         for ( i = 0;  i < 6;  i++ )
         {
            //
            // Store column i of m1 in a 6-vector.
            //
            for ( j = 0;  j < 6;  j++ )
            {
               xArray[j] = m1.getElt(j,i);
            }

            v0 = new Vector6( xArray );

            //
            // Store column i of m2 (== m0*m1) in a 6-vector.
            //
            for ( j = 0;  j < 6;  j++ )
            {
               xArray[j] = m2.getElt(j,i);
            }

            //
            // v1 is the expected result.
            //
            v1 = new Vector6( xArray );

            //
            // Compute the product directly.
            //
            v3 = m0.mxv( v0 );

            ok = JNITestutils.chckad ( "v3(" + i + "," + j + ")",
                                       v3.toArray(),
                                       "~~",
                                       v1.toArray(),
                                       TIGHT_TOL                );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test transposeByBlocks." );


         array1D = new double[36];

         for ( i = 0;  i < 36;  i++ )
         {
            array1D[i] = i / 0.01;
         }

         m0 = new Matrix66( array1D );

         m1 = m0.transposeByBlocks();

         //
         // Check each block.
         //
         for ( i = 0;  i < 2;  i++ )
         {
            for ( j = 0;  j < 2;  j++ )
            {
               block1 = m1.getBlock(i,j);
               block0 = m0.getBlock(i,j).xpose();

               ok = JNITestutils.chcksd ( "block(" + i + "," + j + ") error",
                                          block0.dist( block1 ),
                                          "~",
                                          0.0,
                                          TIGHT_TOL                          );
            }
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString." );

         String endl = System.getProperty( "line.separator" );


         array1D = new double[36];

         for ( i = 0;  i < 36;  i++ )
         {
            //
            // Create values that are portable and have maximum width.
            //
            array1D[i] = (-i -1) * Math.pow( 2.0, -350.0 );
         }

         m0 = new Matrix66( array1D );


         //
         // We expect 17 mantissa digits in each component.
         //

         xstr0 =

         endl + endl +
         "Upper left block:" + endl +
         endl +
         "-4.3601508761683463e-106, -8.7203017523366930e-106, " +
         "-1.3080452628505039e-105," + endl +
         "-3.0521056133178424e-105, -3.4881207009346770e-105, " +
         "-3.9241357885515120e-105," + endl +
         "-5.6681961390188500e-105, -6.1042112266356850e-105, " +
         "-6.5402263142525195e-105"  + endl +
         endl +
         "Upper right block:" + endl +
         endl +
         "-1.7440603504673385e-105, -2.1800754380841730e-105, " +
         "-2.6160905257010078e-105," + endl +
         "-4.3601508761683460e-105, -4.7961659637851810e-105, " +
         "-5.2321810514020156e-105," + endl +
         "-6.9762414018693540e-105, -7.4122564894861890e-105, " +
         "-7.8482715771030230e-105"  + endl +
         endl +
         "Lower left block:" + endl +
         endl +
         "-8.2842866647198580e-105, -8.7203017523366930e-105, " +
         "-9.1563168399535270e-105," + endl +
         "-1.0900377190420866e-104, -1.1336392278037700e-104, " +
         "-1.1772407365654535e-104," + endl +
         "-1.3516467716121874e-104, -1.3952482803738708e-104, " +
         "-1.4388497891355543e-104"  + endl +
         endl +
         "Lower right block:" + endl +
         endl +
         "-9.5923319275703620e-105, -1.0028347015187197e-104, " +
         "-1.0464362102804031e-104," + endl +
         "-1.2208422453271370e-104, -1.2644437540888204e-104, " +
         "-1.3080452628505039e-104," + endl +
         "-1.4824512978972378e-104, -1.5260528066589212e-104, " +
         "-1.5696543154206047e-104"  + endl;

         outStr = m0.toString();

         // For debugging
         //

         //System.out.println( "length of xstr0  = " + xstr0.length()  );
         //System.out.println( "length of outStr = " + outStr.length()  );
         //System.out.println( xstr0  );
         //System.out.println( outStr );


         ok  =  JNITestutils.chcksc( "m0.toString()",
                                     outStr,
                                     "=",
                                     xstr0                  );
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

