package spice.tspice;

import spice.basic.*;
import spice.basic.TriangularPlate.*;
import spice.basic.TriangularPlateVertices.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import spice.basic.CSPICE;


/**
Class TestTriangularPlateVertices provides methods 
that implement test families for the class 
TriangularPlateVertices.


<p> Version 1.0.0 31-DEC-2016 (NJB)

*/
public class TestTriangularPlateVertices
{


   /**
   Test family 001 for methods of the class 
   spice.basic.TriangularPlateVertices.
   <pre>
   -Procedure f_TriangularPlateVertices (Test TriangularPlateVertices)

   -Copyright

      Copyright (2004), California Institute of Technology.
      U.S. Government sponsorship acknowledged.

   -Required_Reading

      None.

   -Keywords

      TESTING

   -Brief_I/O

      VARIABLE  I/O  DESCRIPTION
      --------  ---  --------------------------------------------------

      The method returns the boolean true if all tests pass,
      false otherwise.

   -Detailed_Input

      None.

   -Detailed_Output

      The method returns the boolean true if all tests pass,
      false otherwise.  If any tests fail, diagnostic messages
      are sent to the test logger.

   -Parameters

      None.

   -Files

      None.

   -Exceptions

      Error free.

   -Particulars

      This routine tests methods of class Vector3.
      The current set is:

         Name               Status (x=="done")
         ==============     ==================
         add                x 

   -Examples

      None.

   -Restrictions

      None.

   -Author_and_Institution

      N.J. Bachman   (JPL)
      E.D. Wright    (JPL)

   -Literature_References

      None.

   -Version

      -JNISpice Version 1.0.0 31-DEC-2016 (NJB) (EDW)

   -&
   </pre>
   */

   public static boolean f_TriangularPlateVertices()

      throws SpiceErrorException
   {
      //
      // Local constants
      //
      final double                   VTIGHT = 1.e-14;
      final double                   TIGHT  = 1.e-12;
      final int                      MAXV   = 1000;
      final int                      MAXP   = ( 2 * MAXV );

      //
      // Local variables
      //
      String                         label;
 
      boolean                        ok;

      double                         a;
      double                         b;
      double                         c;
      double                         delta;
      double                         frac;
      double[][]                     pltVertArray;
      double                         scale;
      double                         tol;
      double[]                       v;
      double[]                       vertArray1D;
      double[]                       vout;
      double[][]                     xPltVertArray;
      double[]                       xVertArray1D;

      int[]                          expPltArray;
      int                            i;
      int                            j;
      int                            np;
      int[]                          npArray;
      int                            nv;
      int[]                          nvArray;
      int[]                          pltArray;
      int[]                          pout;

      TriangularPlateVertices        pltVerts;
      TriangularPlateVertices        pltVerts0;
      TriangularPlateVertices        pltVerts1;
      TriangularPlate[]              plates;

      Vector3                        centroid;
      Vector3                        edge01;
      Vector3                        edge12;
      Vector3                        nearpt;
      Vector3                        normal;
      Vector3                        offset;
      Vector3[]                      pltVertices;
      Vector3[]                      pltVertices1;
      Vector3[]                      vertices;
      Vector3                        xCentroid;
      Vector3                        xNormal;
      Vector3                        xOffset;


      //
      // Start tests.
      //


      try
      {

         JNITestutils.topen ( "f_TriangularPlateVertices" );

         //
         // Constructor tests
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Set up: create a plate set for a box." );

         a = 10.0;
         b = 20.0;
         c = 30.0;

         nvArray = new int[1];
         npArray = new int[1];

         vout    = new double[ 3*MAXV ];
         pout    = new int   [ 3*MAXP ];

         //
         // Create box; output vertices into three 3-vectors.
         //         
         JNITestutils.zzpsbox( a, b, c, nvArray, vout, npArray, pout );

         nv            = nvArray[0];
         vertices      = new Vector3[nv];

         for ( i = 0, j = 0;  i < nv;  i++ )
         {
            j           = 3*i;
            vertices[i] = new Vector3( vout[j], vout[j+1], vout[j+2] );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: no-args constructor" );

         pltVerts = new TriangularPlateVertices();

         //
         // We should have a set of zero-filled Vectors. 
         //
         pltVertArray  = pltVerts.toArray();
         xPltVertArray = new double[3][3];

         for ( i = 0;  i < 3;  i++ )
         {
            label = String.format( "pltVertArray row %d", i );

            ok = JNITestutils.chckad( label, pltVertArray[i],   "=", 
                                             xPltVertArray[i],  0.0  );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: three " +
                              "Vector3 constructor"               );

         //
         // Use the first three vertices from the set-up section above.
         // Note that these vertices don't have to correspond to a plate.
         //
         pltVerts = new TriangularPlateVertices( vertices[0], 
                                                 vertices[1], 
                                                 vertices[2] );       

         pltVertArray  = pltVerts.toArray();

         for ( i = 0;  i < 3;  i++ )
         {
            label = String.format( "pltVertArray row %d", i );

            ok = JNITestutils.chckad( label, pltVertArray[i],        "=", 
                                             vertices[i].toArray(),  0.0  );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: three double[] " +
                              "constructor"                               );

         //
         // Use the vertices from the set-up section above.
         //
         pltVerts = new TriangularPlateVertices( vertices[0].toArray(), 
                                                 vertices[1].toArray(), 
                                                 vertices[2].toArray() );       

         pltVertArray  = pltVerts.toArray();

         for ( i = 0;  i < 3;  i++ )
         {
            label = String.format( "pltVertArray row %d", i );

            ok = JNITestutils.chckad( label, pltVertArray[i],        "=", 
                                             vertices[i].toArray(),  0.0  );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: double[][] " +
                              "constructor"                               );

         //
         // Use the vertices from the set-up section above.
         //
         //
         // Use the Vector3 constructor to get started. Then extract
         // the vertices to a double[][] array.
         //
         pltVerts = new TriangularPlateVertices( vertices[0], 
                                                 vertices[1], 
                                                 vertices[2] );       
         pltVertArray  = pltVerts.toArray();

         //
         // Now use the double[][] constructor.
         //
         
         pltVerts1 = new TriangularPlateVertices( pltVertArray );

         //
         // Check the contents of pltVerts.
         // 

         pltVertArray = pltVerts1.toArray();

         for ( i = 0;  i < 3;  i++ )
         {
            label = String.format( "pltVertArray row %d", i );

            ok = JNITestutils.chckad( label, pltVertArray[i],        "=", 
                                             vertices[i].toArray(),  0.0  );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices copy constructor" );

         pltVerts0 = new TriangularPlateVertices( vertices[0], 
                                                  vertices[1], 
                                                  vertices[2] );       
    

         pltVerts  = new TriangularPlateVertices( pltVerts0 );

         //
         // We should have a set of plate vertices containing the contents of
         // the first set of vertices.
         //
         pltVertArray = pltVerts.toArray();

         for ( i = 0;  i < 3;  i++ )
         {
            label = String.format( "pltVertArray row %d", i );

            ok = JNITestutils.chckad( label, pltVertArray[i],        "=", 
                                             vertices[i].toArray(),  0.0  );
         }

         //
         // Modify pltVerts0; make sure pltVerts is unchanged.
         //
 
         pltVerts0     = new TriangularPlateVertices();
         pltVertArray  = pltVerts.toArray();


         for ( i = 0;  i < 3;  i++ )
         {
            label = String.format( "pltVertArray row %d", i );

            ok = JNITestutils.chckad( label, pltVertArray[i],        "=", 
                                             vertices[i].toArray(),  0.0  );
         }


         //
         // Test methods
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: toArray1D" );

         pltVerts0 = new TriangularPlateVertices( vertices[0], 
                                                  vertices[1], 
                                                  vertices[2] );       

         vertArray1D  = pltVerts0.toArray1D();
         xVertArray1D = new double[9];

         for ( i = 0;  i < 3;  i++ )
         {
            v = vertices[i].toArray();

            System.arraycopy( v, 0, xVertArray1D, 3*i, 3 );
         }


         ok = JNITestutils.chckad( "vertArray1D", vertArray1D,       
                                   "=",           xVertArray1D,  0.0  );

 
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: toVectors" );

         pltVerts0 = new TriangularPlateVertices( vertices[0], 
                                                  vertices[1], 
                                                  vertices[2] );       

         pltVertices  = pltVerts0.toVectors();

         for ( i = 0;  i < 3;  i++ )
         {
            label = String.format( "Vertex vector %d", i );

            ok = JNITestutils.chckad( label,
                                      pltVertices[i].toArray(),       
                                      "=",         
                                      vertices[i].toArray(),  0.0  );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: getCentroid" );

         //
         // For this test, we need a set of plate vertices corresponding
         // to an actual plate. Use the first plate of the set.
         //
         np          = npArray[0];
         pltArray    = new int[3];
         pltVertices = new Vector3[3];

         System.arraycopy ( pout, 0, pltArray, 0, 3 );

         for ( i = 0;  i < 3;  i++ )
         {
            //
            // Note that the index into the vertices array must be
            // adjusted, since pltArray contains 1-based vertex IDs.
            // 
            pltVertices[i] = vertices[ pltArray[i] - 1 ];
         }

         pltVerts0 = new TriangularPlateVertices( pltVertices[0], 
                                                  pltVertices[1], 
                                                  pltVertices[2] );       
         //
         // Compute the plate's centroid.
         //
         centroid = pltVerts0.getCentroid();


         //
         // Compute the expected centroid.
         //
         frac      = 1.0/3.0;

         xCentroid = new Vector3();

         for ( i = 0;  i < 3;  i++ )
         {
            xCentroid = xCentroid.add(  pltVertices[i].scale( frac )  );
         }

         //
         // Compare. We need a non-zero tolerance for this one.
         //
         tol = TIGHT;

         ok = JNITestutils.chckad( "centroid", 
                                   centroid.toArray(),
                                   "~~/",
                                   xCentroid.toArray(), 
                                   tol                  );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: expand" );

         //
         // For this test, we need a set of plate vertices corresponding
         // to an actual plate. Use the first plate of the set.
         //
         np          = npArray[0];
         pltArray    = new int[3];
         pltVertices = new Vector3[3];

         System.arraycopy ( pout, 0, pltArray, 0, 3 );

         for ( i = 0;  i < 3;  i++ )
         {
            //
            // Note that the index into the vertices array must be
            // adjusted, since pltArray contains 1-based vertex IDs.
            // 
            pltVertices[i] = vertices[ pltArray[i] - 1 ];
         }

         pltVerts0 = new TriangularPlateVertices( pltVertices[0], 
                                                  pltVertices[1], 
                                                  pltVertices[2] );       
         
         //
         // Expand the plate by a factor of 2.
         //
         scale     = 2.0;
         delta     = scale - 1.0;

         pltVerts1 = pltVerts0.expand( delta );

         //
         // Make sure the expanded plate has the same centroid as the 
         // original plate.
         //
         xCentroid = pltVerts0.getCentroid();

         centroid  = pltVerts1.getCentroid();

         //
         // Compare. We need a non-zero tolerance for this one.
         //
         tol = TIGHT;

         ok = JNITestutils.chckad( "centroid", 
                                   centroid.toArray(),
                                   "~~/",
                                   xCentroid.toArray(), 
                                   tol                  );
         //
         // Check each vertex's offset from the centroid.
         //
         pltVertices1 = pltVerts1.toVectors();

         for ( i = 0;  i < 3;  i++ )
         {
            offset  = pltVertices1[i].sub( centroid );

            xOffset = ( pltVertices[i].sub( xCentroid ) ).scale( scale );

            //
            // Compare. We need a non-zero tolerance for this one.
            //
            label = String.format( "Centroid offset %d", i );
 
            tol = TIGHT;

            ok = JNITestutils.chckad( label, 
                                      offset.toArray(),
                                      "~~/",
                                      xOffset.toArray(), 
                                      tol                  );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: getOutwardNormal." );

         //
         // Use the TriangularPlateVertices instance from the previous case.
         //

         normal = pltVerts0.getOutwardNormal();

         //
         // Compute the expected normal from the vertices of the instance.
         //
         edge01  = pltVertices[1].sub( pltVertices[0] );
         edge12  = pltVertices[2].sub( pltVertices[1] );

         xNormal = edge01.ucross( edge12 );

         //
         // For comparison, convert the TriangularPlateVertices normal
         // to unit length.
         // 
         normal = normal.hat();

         //
         // Compare.
         // 
         tol = TIGHT;

         ok = JNITestutils.chckad( "normal", 
                                   normal.toArray(),
                                   "~~/",
                                   xNormal.toArray(), 
                                   tol                  );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlateVertices: getNearPoint." );

         //
         // We'll use the TriangularPlateVertices instance from the 
         // previous case.
         //
         // We'll create a point off the plate for which the near point can be
         // computed trivially: we'll add the outward normal to the centroid.
         // The expected near point is the centroid itself.
         // 
         normal   = pltVerts0.getOutwardNormal();
         centroid = pltVerts0.getCentroid();

         offset   = centroid.add( normal );

         nearpt   = pltVerts0.getNearPoint( offset );

         //
         // Compare.
         // 
         tol = TIGHT;

         ok = JNITestutils.chckad( "nearpt", 
                                   nearpt.toArray(),
                                   "~~/",
                                   centroid.toArray(), 
                                   tol                  );


      }
      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         ok = JNITestutils.chckth ( false, "", ex );
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

} /* End f_TriangularPlate */











