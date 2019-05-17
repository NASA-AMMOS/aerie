
package spice.tspice;

import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import spice.basic.CSPICE;
import static spice.basic.AngularUnits.*;

/**
Class testVector6 provides methods that implement test families for
the class Vector6.


<p> Version 1.0.0 15-DEC-2009 (NJB)

*/
public class TestVector6 extends Object
{

   //
   // Constants
   //

   private final static double  TOL        = 1.e-09;
   private final static double  TIGHT_TOL  = 1.e-12;

   private static final Vector3       e1       = new Vector3( 1.0, 0.0, 0.0 );
   private static final Vector3       e2       = new Vector3( 0.0, 1.0, 0.0 );
   private static final Vector3       e3       = new Vector3( 0.0, 0.0, 1.0 );


   private static final Vector6       vec0     = new Vector6 (
                                             9., 12.0, 36., 12.0, 16.0, 48.0 );




   private static final Vector6       vec1     = new Vector6 (
                                              11., 12., 13., 14., 15., 16.  );

   private static final Vector6       vec2     = new Vector6 (
                                           10., 25.,  90., -5., 0.56, 12.3   );


   /**
   Test family 001 for methods of the class spice.basic.Vector6.
   <pre>
   -Procedure f_Vector6 ( Test family 001 for class Vector6 )

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

      This routine tests methods of class Vector6.
      The current set is:

         Name               Status (x=="done")
         ==============     ==================
         add                x
         assign             x
         dcross             x
         ddot               x
         dhat               x
         dist               x
         dot                x
         dsep               x
         getElt             x
         isZero             x
         lcom               x
         negate             x
         norm               x
         scale              x
         sub                x
         toArray            x
         toString           x

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

      -JNISpice Version 2.0.0 28-NOV-2009 (NJB)


      -JNISpice Version 1.0.0 10-MAY-2004 (NJB) (EDW)

   -&
   </pre>
   */

   public static boolean f_Vector6()

      throws SpiceErrorException
   {
      //
      // Constants
      //
      final String            endl = System.getProperty( "line.separator" );

      //
      // Local variables
      //
      String                  outstr0;
      String                  xstr0;

      Vector6                 vout0;
      Vector6                 vout1;
      Vector6                 vout2;
      Vector6                 vout3;
      Vector6                 vtemp0;
      Vector6                 vtemp1;
      Vector6                 vtemp2;
      Vector6                 vtest3;
      Vector6                 uvec0;
      Vector6                 zeroVec = new Vector6();
      Vector6                 xvec0;
      Vector6                 xvec1;

      boolean                 ok;

      double                  dist;
      double                  elt;
      double                  norm;
      double                  rate;
      double[]                vArray;
      double[]                xArray;
      double                  xNorm;



      //
      // Start tests.
      //


      try
      {

         JNITestutils.topen ( "f_Vector6" );

         //
         // Constructor tests
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Double array constructor" );

         xArray = vec1.toArray();

         vout0 = new Vector6 ( xArray );

         ok    =  JNITestutils.chckad ( "vout0/double array cons",
                                        vout0.toArray(),
                                        "=",
                                        xArray,
                                        0.0                      );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Double array constructor: 9-elt input array" );

         double[] bigArray = new double[9];
         xArray            = new double[9];

         for ( int i = 0;  i< 9;  i++ )
         {
            bigArray[i] = i-2;
         }

         System.arraycopy( bigArray, 0, xArray, 0, 6 );

         vout3 = new Vector6 ( bigArray );

         ok  =  JNITestutils.chckad ( "vout3/double array cons",
                                      vout3.toArray(),
                                      "=",
                                      xArray,
                                      0.0               );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Zero-args constructor" );

         vout0 = new Vector6();

         //
         // vArray is initialized to zero.
         //
         vArray = new double[6];


         ok  =  JNITestutils.chckad ( "vout0/zero args cons",
                                      vout0.toArray(),
                                      "=",
                                      vArray,
                                      0.0               );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Copy constructor" );

         vout0  = new Vector6 ( vec1 );
         vArray = vout0.toArray();

         vout1  = new Vector6 ( vout0 );

         ok  =  JNITestutils.chckad ( "vout0/copy cons",
                                      vout0.toArray(),
                                      "=",
                                      vArray,
                                      0.0               );

         //
         // Make sure we can change vout0 without affecting vout1.
         //
         vout0 = vout0.scale(2.0);

         ok  =  JNITestutils.chckad ( "vout1/copy cons (1)",
                                      vout1.toArray(),
                                      "=",
                                      vArray,
                                      0.0               );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector3 pair constructor" );

         vout0  = new Vector6 ( new Vector3(11.0,12.0,13.0),
                                new Vector3(14.0,15.0,16.0)  );

         ok  =  JNITestutils.chckad ( "Vector3 cons",
                                      vout0.toArray(),
                                      "=",
                                      vec1.toArray(),
                                      0.0               );



         //
         // Method tests
         //



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - add" );

         vtemp0 = new Vector6 ( 1., 2., 3., 4., 5., 6. );
         vtemp1 = vtemp0.scale(-3.);

         xvec0  = vtemp0.scale(-2);

         vout0  = vtemp0.add ( vtemp1 );

         ok  =  JNITestutils.chckad ( "vout0/add",
                                       vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - assign" );

         vout0  = new Vector6( vec1 );

         vout0.assign( vec2.toArray() );


         ok  =  JNITestutils.chckad ( "vout0/assign",
                                      vout0.toArray(),
                                      "~/",
                                      vec2.toArray(),
                                      TIGHT_TOL          );



         //
         // --------Case-----------------------------------------------
         //

         //
         // ddot tests
         //
         JNITestutils.tcase ( "Vector operation tests - ddot" );



         Vector3 v00  = vec0.getVector3( 0 );
         Vector3 v01  = vec0.getVector3( 1 );

         Vector3 v10  = vec1.getVector3( 0 );
         Vector3 v11  = vec1.getVector3( 1 );

         double xddot = v00.dot(v11) + v10.dot(v01);



         ok  =  JNITestutils.chcksd ( "vec0.dot(vec1)",
                                      vec0.ddot( vec1 ),
                                      "~/",
                                      xddot,
                                      TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //

         //
         // ddot tests
         //
         JNITestutils.tcase ( "Vector operation tests - dcross" );



         v00  = vec0.getVector3( 0 );
         v01  = vec0.getVector3( 1 );

         v10  = vec1.getVector3( 0 );
         v11  = vec1.getVector3( 1 );

         Vector3 xDeriv3  = ( v00.cross(v11) ).add( v01.cross(v10)  );

         Vector3 xVec3    = v00.cross( v10 );

         Vector6 xDcross  = new Vector6( xVec3, xDeriv3 );


         ok  =  JNITestutils.chckad ( "vec0.dcross(vec1)",
                                      vec0.dcross( vec1 ).toArray(),
                                      "~~/",
                                      xDcross.toArray(),
                                      TIGHT_TOL                    );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - dhat" );

         //
         // dhat tests
         //

         //
         // Create a state with position vector e1, velocity vector e1+e2.
         //
         vout0 = new Vector6( e1, e2.add(e1) );

         vout1 = vout0.dhat();

         //
         // Check the position and velocity components of vout1.
         //
         ok  =  JNITestutils.chckad ( "vout1 position",
                                      vout1.getVector3(0).toArray(),
                                      "~~/",
                                      e1.toArray(),
                                      TIGHT_TOL              );

         ok  =  JNITestutils.chckad ( "vout1 velocity",
                                      vout1.getVector3(1).toArray(),
                                      "~~/",
                                      e2.toArray(),
                                      TIGHT_TOL              );



         //
         // --------Case-----------------------------------------------
         //

         //
         // dist tests
         //
         JNITestutils.tcase ( "Vector operation tests - dist" );

         vout0 = vec0.scale(-1.0);

         ok  =  JNITestutils.chcksd ( "dist",
                                      vout0.dist( vec0 ),
                                      "~/",
                                      130.0,
                                      TIGHT_TOL          );

         //
         // --------Case-----------------------------------------------
         //

         //
         // dot tests
         //
         JNITestutils.tcase ( "Vector operation tests - dot" );


         ok  =  JNITestutils.chcksd ( "vec0.dot(vec0)",
                                      vec0.dot( vec0 ),
                                      "~/",
                                      Math.pow( vec0.norm(), 2 ),
                                      TIGHT_TOL                   );





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - dsep" );

         //
         // dsep tests
         //

         //
         // Create a state with position vector e1, velocity vector e1+e2.
         //
         vout0 = new Vector6( e1, e2.add(e1) );


         //
         // Create a state with position vector e2, velocity vector e2+e1.
         //
         vout1 = new Vector6( e2, e2.add(e1) );


         //
         // The vectors should be converging at an angular separate rate
         // of 2 radians/unit time.
         //
         rate = vout0.dsep( vout1 );

         //
         // Check the angular separation rate.
         //
         ok  =  JNITestutils.chcksd ( "rate",
                                      rate,
                                      "~",
                                      -2.0,
                                      TIGHT_TOL    );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - getElt" );

         //
         // getElt tests
         //
         ok  =  JNITestutils.chcksd ( "vec1[0]",
                                       vec1.getElt(0),
                                       "~/",
                                       11.0,
                                       TIGHT_TOL          );

         ok  =  JNITestutils.chcksd ( "vec1[1]",
                                       vec1.getElt(1),
                                       "~/",
                                       12.0,
                                       TIGHT_TOL          );

         ok  =  JNITestutils.chcksd ( "vec1[2]",
                                       vec1.getElt(2),
                                       "~/",
                                       13.0,
                                       TIGHT_TOL          );

         ok  =  JNITestutils.chcksd ( "vec1[3]",
                                       vec1.getElt(3),
                                       "~/",
                                       14.0,
                                       TIGHT_TOL          );

        ok  =  JNITestutils.chcksd ( "vec1[4]",
                                       vec1.getElt(4),
                                       "~/",
                                       15.0,
                                       TIGHT_TOL          );

         ok  =  JNITestutils.chcksd ( "vec1[5]",
                                       vec1.getElt(5),
                                       "~/",
                                       16.0,
                                       TIGHT_TOL          );





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getElt --- bad indices" );

         try
         {

            elt = vec1.getElt(-1);

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

            elt = vec1.getElt(6);

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
         JNITestutils.tcase ( "Vector operation tests - isZero" );



         ok  =  JNITestutils.chcksl ( "zeroVec is zero",
                                      zeroVec.isZero(),
                                      true              );

         ok  =  JNITestutils.chcksl ( "vec1 is zero",
                                      vec1.isZero(),
                                      false             );




         //
         // --------Case-----------------------------------------------
         //

         //
         // lcom tests
         //
         JNITestutils.tcase ( "Vector operation tests - lcom (2 args)" );

         vout0  = Vector6.lcom( 2.0, vec1, -3.0, vec1 );

         xArray = vec1.negate().toArray();

         ok  =  JNITestutils.chckad ( "vout0",
                                      vout0.toArray(),
                                      "~~/",
                                      xArray,
                                      TIGHT_TOL              );


         //
         // --------Case-----------------------------------------------
         //

         //
         // negate tests
         //
         JNITestutils.tcase ( "Vector operation tests - negate" );


         vout0 = vec1.scale( -1.0  );
         vout1 = vec1.negate();


         ok  =  JNITestutils.chckad ( "vout1",
                                      vout1.toArray(),
                                      "~~/",
                                      vout0.toArray(),
                                      TIGHT_TOL         );





         //
         // --------Case-----------------------------------------------
         //

         //
         // norm tests
         //
         JNITestutils.tcase ( "Vector operation tests - norm" );


         ok  =  JNITestutils.chcksd ( "||vec0||",
                                      vec0.norm(),
                                      "~/",
                                      65.0,
                                      TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //

         //
         // scale tests
         //
         JNITestutils.tcase ( "Vector operation tests - scale" );

         vout0 = vec1.scale( 2.0  );
         vout1 = vec1.add  ( vec1 );


         ok  =  JNITestutils.chckad ( "vout0",
                                      vout0.toArray(),
                                      "~~/",
                                      vout1.toArray(),
                                      TIGHT_TOL         );





         //
         // --------Case-----------------------------------------------
         //

         //
         // sub tests
         //
         JNITestutils.tcase ( "Vector operation tests - sub" );


         vout0 = vec1.scale ( 2.0  );
         vout1 = vout0.sub  ( vec1 );


         ok  =  JNITestutils.chckad ( "vout1",
                                      vout1.toArray(),
                                      "~~/",
                                      vec1.toArray(),
                                      TIGHT_TOL         );




         //
         // --------Case-----------------------------------------------
         //

         //
         // toArray tests
         //
         JNITestutils.tcase ( "Vector operation tests - toArray" );


         xArray = new double[6];

         for(  int i = 0;  i < 6;  i++ )
         {
            xArray[i] = (double)i;
         }

         vout0 = new Vector6( xArray );

         //
         // We expect an exact match in this case.
         //
         ok  =  JNITestutils.chckad ( "vout0 array",
                                      vout0.toArray(),
                                      "=",
                                      xArray,
                                      0.0            );





         //
         // --------Case-----------------------------------------------
         //

         //
         // toString tests
         //
         JNITestutils.tcase ( "Vector operation tests - toString" );



         xvec0 = new Vector6( -1.e-100, -2.e-200, -3.e-300,
                              -4.e-301, -5.e-302, -6.e-303 );

         //
         // We expect 17 mantissa digits in each component.
         //

         xstr0 = "(-1.0000000000000000e-100, " +
                  "-2.0000000000000000e-200, "  +
                  "-3.0000000000000000e-300,"  + endl +
                  " -4.0000000000000000e-301, "  +
                  "-5.0000000000000000e-302, "  +
                  "-6.0000000000000000e-303)";

         // For debugging:
         //System.out.println( xstr0 );
         //System.out.println( xvec0.toString() );

         ok  =  JNITestutils.chcksc( "xvec0.toString() (0)",
                                     xvec0.toString(),
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

         ok = JNITestutils.chckth ( false, "", ex );
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

} /* End f_Vector6 */

