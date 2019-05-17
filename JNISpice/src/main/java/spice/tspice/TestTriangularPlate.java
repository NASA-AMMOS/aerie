package spice.tspice;

import spice.basic.*;
import spice.basic.TriangularPlate.*;
import spice.basic.TriangularPlateVertices.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import spice.basic.CSPICE;


/**
Class TestTriangularPlate provides methods that implement test families for
the class TriangularPlate.


<p> Version 1.0.0 09-NOV-2016 (NJB)

*/
public class TestTriangularPlate
{


   /**
   Test family 001 for methods of the class spice.basic.TriangularPlate.
   <pre>
   -Procedure f_TriangularPlate (Test TriangularPlate)

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

      -JNISpice Version 1.0.0 09-NOV-2016 (NJB) (EDW)

   -&
   </pre>
   */

   public static boolean f_TriangularPlate()

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

      boolean                        ok;

      double                         a;
      double                         area;
      double                         b;
      double                         c;
      double                         tol;
      double                         volume;
      double[]                       vout;
      double                         xArea;
      double                         xVolume;

      int[]                          expPltArray;
      int                            np;
      int[]                          npArray;
      int                            nv;
      int[]                          nvArray;
      int[]                          pltArray;
      int[]                          pout;

      TriangularPlate                plt;
      TriangularPlate                plt0;
      TriangularPlate[]              plates;

      Vector3[]                      vertices;

      //
      // Start tests.
      //


      try
      {

         JNITestutils.topen ( "f_TriangularPlate" );

         //
         // Constructor tests
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlate no-args constructor" );

         plt = new TriangularPlate();

         //
         // We should have a zero-filled plate. 
         //
         pltArray    = plt.toArray();
         expPltArray = new int[3];

         ok = JNITestutils.chckai( "pltArray", pltArray,   "=", 
                                               expPltArray      );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlate copy constructor" );

         plt  = new TriangularPlate( 4, 5, 7 );

         plt0 = new TriangularPlate( plt );

         //
         // We should have a plate containing the contents of
         // the first plate. 
         //
         pltArray    = plt0.toArray();
         expPltArray = plt.toArray();

         ok = JNITestutils.chckai( "pltArray", pltArray,   "=", 
                                               expPltArray      );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlate 3-int constructor" );

         plt = new TriangularPlate( 3, 2, 1 );

         //
         // We should have a plate filled with the initialization values.
         //
         pltArray       = plt.toArray();
         
         expPltArray[0] = 3;
         expPltArray[1] = 2;
         expPltArray[2] = 1;

         ok = JNITestutils.chckai( "pltArray", pltArray,   "=", 
                                               expPltArray      );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TriangularPlate 3-int constructor; bad " +
                              "vertex index."                            );

         try
         {
            plt = new TriangularPlate( 0, 1, 4 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BADVERTEXNUMBER)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(BADVERTEXNUMBER)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test method toArray" );

         plt  = new TriangularPlate( 4, 5, 7 );

         pltArray = plt.toArray();

         //
         // We should have a plate containing the contents of
         // the first plate. 
         //
         expPltArray[0] = 4;
         expPltArray[1] = 5;
         expPltArray[2] = 7;

         ok = JNITestutils.chckai( "pltArray", pltArray,   "=", 
                                               expPltArray      );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Create a plate set representing a box; " +
                              "find the volume of the box."               );

         a = 10.0;
         b = 20.0;
         c = 30.0;

         nvArray = new int[1];
         npArray = new int[1];

         vout    = new double[ 3*MAXV ];
         pout    = new int   [ 3*MAXP ];

         //
         // Create box; output vertices and plates into 1-D arrays.
         //         
         JNITestutils.zzpsbox( a, b, c, nvArray, vout, npArray, pout );

         np     = npArray[0];
         plates = new TriangularPlate[np];

         for ( int i = 0, j = 0;  i < np;  i++ )
         {
            j         = 3*i;
            plates[i] = new TriangularPlate( pout[j], pout[j+1], pout[j+2] );
         }

         nv            = nvArray[0];
         vertices      = new Vector3[nv];

         for ( int i = 0, j = 0;  i < nv;  i++ )
         {
            j           = 3*i;
            vertices[i] = new Vector3( vout[j], vout[j+1], vout[j+2] );
         }

         xVolume = a * b * c;

         volume  = TriangularPlate.volume( plates, vertices );
 
         tol = VTIGHT;

         ok  = JNITestutils.chcksd ( "volume", volume, "~", 
                                               xVolume, tol ); 



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Find the area of the box from the " +
                              "previous case."                       );

         xArea = 2 * ( (a * b) + (a * c) + (b * c) );

         area  = TriangularPlate.area( plates, vertices );

         tol = VTIGHT;

         ok  = JNITestutils.chcksd ( "area", area, "~", 
                                             xArea, tol ); 







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











