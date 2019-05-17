
package spice.tspice;

import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import spice.basic.CSPICE;
import static spice.basic.AngularUnits.*;

/**
Class testVector3 provides methods that implement test families for
the class Vector3.


<p> Version 1.0.0 15-DEC-2009 (NJB)

*/
public class TestVector3 extends Object
{

   //
   // Constants
   //

   private final static double  TOL        = 1.e-09;
   private final static double  TIGHT_TOL  = 1.e-12;

   private static final Vector3       e1       = new Vector3( 1.0, 0.0, 0.0 );
   private static final Vector3       e2       = new Vector3( 0.0, 1.0, 0.0 );
   private static final Vector3       e3       = new Vector3( 0.0, 0.0, 1.0 );

   private static final Vector3       vec0     = new Vector3 ( new double[]
                           {3., 4.0, 12.}            );

   private static final Vector3       vec1     = new Vector3 ( new double[]
                           {11., 12.,  13.}            );


   private static final Vector3       vec2     = new Vector3 ( new double[]
                           {10., 25.,  90., -5., 0.56, 12.3}   );

   private static final Vector3       vec3     = new Vector3 ( new double[]
                           {10., 25.,  90. }                   );

   private static final Vector3       vec4     = new Vector3 ( new double[]
                           { 3.,  5., -14.,  2.}               );

   private static final Vector3       vec5     = new Vector3 ( new double[]
                           { 9.,  2., -11.,  3.}               );

   private static final Vector3       vec6     = new Vector3 ( new double[]
                           {-7.,  3., -0.7 }                   );

   private static final Vector3       vec7      = new Vector3 ( new double[]
                           {34., -12.3, 14.73, 45.1, -8., -16.2 } );

   private static final Vector3       null3     = new Vector3 ( new double[]
                           { 0.,  0.,  0. }                    );

   private static final Vector3       x3        = new Vector3 ( new double[]
                           {1., 0.,  0. }                      );

   private static final Vector3       z3        = new Vector3 ( new double[]
                           {0., 0.,  1. }                      );

   private static final Vector3       null4     = new Vector3 ( new double[]
                           { 0.,  0.,  0.,  0.}                );

   private static final Vector3       x5        = new Vector3 ( new double[]
                           {1., 0., 0., 0., 0. }               );

   private static final Vector3       z5        = new Vector3 ( new double[]
                           {0., 0., 1., 0., 0. }               );



   /**
   Test family 001 for methods of the class spice.basic.Vector3.
   <pre>
   -Procedure f_Vector3 ( Test family 001 for class Vector3 )

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
         assign             x
         cross              x
         dist               x
         dot                x
         getElt             x
         hat                x
         isZero             x
         lcom               x
         lcom (3 args)      x
         negate             x
         norm               x
         perp               x
         proj               x
         rotate (rotvec)    x
         rotate (vrotv)     x
         scale              x
         sep                x
         sub                x
         toArray            x
         toString           x
         ucrss              x

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

      -JNISpice Version 2.0.0 12-NOV-2009 (NJB)

         Completed test set.

      -JNISpice Version 1.0.0 10-MAY-2004 (NJB) (EDW)

   -&
   </pre>
   */

   public static boolean f_Vector3()

      throws SpiceErrorException
   {
      //
      // Local variables
      //
      String                  outstr0;
      String                  xstr0;

      Vector3                 vout0;
      Vector3                 vout1;
      Vector3                 vout2;
      Vector3                 vout3;
      Vector3                 vtemp0;
      Vector3                 vtemp1;
      Vector3                 vtemp2;
      Vector3                 vtest3;
      Vector3                 uvec0;
      Vector3                 zeroVec = new Vector3();
      Vector3                 xvec0;
      Vector3                 xvec1;

      boolean                 ok;

      double                  dist;
      double                  norm;
      double                  sep;
      double[]                vArray;
      double[]                xArray;
      double                  xNorm;



      //
      // Start tests.
      //


      try
      {

         JNITestutils.topen ( "f_Vector3" );

         //
         // Constructor tests
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Double array constructor" );

         xArray = vec1.toArray();

         vout3  = new Vector3 ( xArray );

         ok  =  JNITestutils.chckad ( "vout3/double array cons",
                                      vout3.toArray(),
                                      "=",
                                      xArray,
                                      0.0               );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Double array constructor: 6-elt input array" );

         double[] bigArray = new double[6];
         xArray            = new double[3];

         for ( int i = 0;  i< 6;  i++ )
         {
            bigArray[i] = i-2;
         }

         System.arraycopy( bigArray, 0, xArray, 0, 3 );

         vout3 = new Vector3 ( bigArray );

         ok  =  JNITestutils.chckad ( "vout3/double array cons",
                                      vout3.toArray(),
                                      "=",
                                      xArray,
                                      0.0               );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Zero-args constructor" );

         vout3 = new Vector3();

         //
         // vArray is initialized to zero.
         //
         vArray = new double[3];


         ok  =  JNITestutils.chckad ( "vout3/zero args cons",
                                      vout3.toArray(),
                                      "=",
                                      vArray,
                                      0.0               );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Copy constructor" );


         vout0  = new Vector3 ( 1., 2., 3. );
         vArray = vout0.toArray();

         vout1  = new Vector3 ( vout0 );

         ok  =  JNITestutils.chckad ( "vout1/copy cons (0)",
                                      vout1.toArray(),
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
         JNITestutils.tcase ( "Three-scalar constructor" );


         vArray = new double[3];

         vArray[0] = 1.0;
         vArray[1] = 2.0;
         vArray[2] = 3.0;

         vout3 = new Vector3( vArray[0], vArray[1], vArray[2] );

         ok    =  JNITestutils.chckad ( "vout3/3-scalar cons",
                                        vout3.toArray(),
                                        "=",
                                        vArray,
                                        0.0                  );


         //
         // Method tests
         //



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - add" );

         vout3  = vec3.add ( vec6 );

         vtest3 = new Vector3 ( new double[] { 3.0, 28.0, 89.3 } );

         ok  =  JNITestutils.chckad ( "vout3/add",
                                       vout3.toArray(),
                                       "~/",
                                       vtest3.toArray(),
                                       TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - assign" );

         vout3  = new Vector3( vec1 );

         vArray = new double[3];

         vArray[0] = 1.0;
         vArray[1] = 2.0;
         vArray[2] = 3.0;

         vout3.assign( vArray );

         ok  =  JNITestutils.chckad ( "vout3/assign",
                                       vout3.toArray(),
                                       "~/",
                                       vArray,
                                       TIGHT_TOL          );





         //
         // --------Case-----------------------------------------------
         //

         //
         // cross tests
         //
         JNITestutils.tcase ( "Vector operation tests - cross" );


         vout3 = e1.cross(e2);

         ok  =  JNITestutils.chckad ( "e1 x e2",
                                       vout3.toArray(),
                                       "~/",
                                       e3.toArray(),
                                       TIGHT_TOL          );

         vout3 = e2.cross(e3);

         ok  =  JNITestutils.chckad ( "e2 x e3",
                                       vout3.toArray(),
                                       "~/",
                                       e1.toArray(),
                                       TIGHT_TOL          );
         vout3 = e3.cross(e1);

         ok  =  JNITestutils.chckad ( "e3 x e1",
                                       vout3.toArray(),
                                       "~/",
                                       e2.toArray(),
                                       TIGHT_TOL          );

         //
         // --------Case-----------------------------------------------
         //

         //
         // dist tests
         //
         JNITestutils.tcase ( "Vector operation tests - dist" );

         dist = vec1.dist ( vec3 );

         ok  =  JNITestutils.chcksd ( "dist/dist",
                                       dist, "~/", 78.09609465, TOL );


         vtemp0 = new Vector3( vec0          );
         vtemp1 = new Vector3( vec0.negate() );

         dist = vtemp0.dist ( vtemp1 );

         ok  =  JNITestutils.chcksd ( "dist/dist",
                                       dist, "~/", 26.0, TIGHT_TOL );

         //
         // --------Case-----------------------------------------------
         //

         //
         // dot tests
         //
         JNITestutils.tcase ( "Vector operation tests - dot" );

         double dot  = vec1.dot ( vec6 );

         double xdot = -50.1;

         ok  =  JNITestutils.chcksd ( "<vec1,vec6>",
                                       dot,
                                       "~/",
                                       xdot,
                                       TIGHT_TOL          );


         dot  = vec0.dot ( vec0 );

         xdot = 169.0;

         ok  =  JNITestutils.chcksd ( "<vec0,vec0>",
                                       dot,
                                       "~/",
                                       xdot,
                                       TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - getElt" );

         //
         // getElt tests
         //
         ok  =  JNITestutils.chcksd ( "vec0[0]",
                                       vec0.getElt(0),
                                       "~/",
                                       3.0,
                                       TIGHT_TOL          );

         ok  =  JNITestutils.chcksd ( "vec0[1]",
                                       vec0.getElt(1),
                                       "~/",
                                       4.0,
                                       TIGHT_TOL          );

         ok  =  JNITestutils.chcksd ( "vec0[2]",
                                       vec0.getElt(2),
                                       "~/",
                                       12.0,
                                       TIGHT_TOL          );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - getElt bad index" );


         try
         {
            double elt = vec0.getElt(-1);

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
            double elt = vec0.getElt(3);

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
         JNITestutils.tcase ( "Vector operation tests - hat" );

         //
         // hat tests
         //

         //
         // vec0 has norm 13.
         //
         uvec0 = vec0.hat();

         xvec0 = vec0.scale(1.0/13.0);

         ok  =  JNITestutils.chckad ( "unitized vec0",
                                       uvec0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - hat/zero vector" );

         //
         // hat tests
         //

         //
         // Currently unitizing the zero vector doesn't cause an
         // exception to be thrown, though it probably should.
         //
         uvec0 = zeroVec.hat();

         xvec0 = new Vector3();

         ok  =  JNITestutils.chckad ( "unitized zero vector",
                                       uvec0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Vector operation tests - isZero" );

         ok  =  JNITestutils.chcksl ( "T-null3/isZero",
                                      null3.isZero(), true  );

         ok  =  JNITestutils.chcksl ( "F-vec1/isZero",
                                      vec1.isZero(),  false );



         //
         // --------Case-----------------------------------------------
         //

         //
         // lcom tests
         //
         JNITestutils.tcase ( "Vector operation tests - lcom (2 args)" );


         vout0 = Vector3.lcom( 4.0, vec0, -1.0, vec1 );

         xvec0 = new Vector3( 1.0, 4.0, 35.0 );

         ok  =  JNITestutils.chckad ( "vout0",
                                       vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );



         //
         // --------Case-----------------------------------------------
         //

         //
         // lcom (3 args) tests
         //
         JNITestutils.tcase ( "Vector operation tests - lcom (3 args)" );


         vout0 = Vector3.lcom( 4.0, vec0, -1.0, vec1, 2.0, vec3 );

         xvec0 = new Vector3( 21.0, 54.0, 215.0 );

         ok  =  JNITestutils.chckad ( "vout0",
                                       vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //

         //
         // negate tests
         //
         JNITestutils.tcase ( "Vector operation tests - negate" );

         vout3 = vec3.negate ();

         double[] v3 = vec3.toArray();

         vtest3 = new Vector3 ( new double[] { -v3[0], -v3[1], -v3[2] } );

         ok  =  JNITestutils.chckad ( "vout3/negate",
                                       vout3.toArray(),
                                       "~/",
                                       vtest3.toArray(),
                                       TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //

         //
         // norm tests
         //
         JNITestutils.tcase ( "Vector operation tests - norm" );

         norm  = vec0.norm();
         xNorm = 13.0;

         ok  =  JNITestutils.chcksd ( "vec0 norm",
                                       norm,
                                       "~/",
                                       xNorm,
                                       TIGHT_TOL          );

         norm  = zeroVec.norm();
         xNorm = 0.0;

         ok  =  JNITestutils.chcksd ( "zeroVec norm",
                                       norm,
                                       "~/",
                                       xNorm,
                                       TIGHT_TOL          );

         //
         // --------Case-----------------------------------------------
         //

         //
         // norm tests
         //
         JNITestutils.tcase ( "Vector operation tests - perp" );

         vout0 = vec0.perp(e1);

         xvec0 = new Vector3( 0.0, vec0.getElt(1), vec0.getElt(2) );

         ok  =  JNITestutils.chckad ( "vec0 perp e1",
                                      vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );

         vout0 = vec0.perp(e2);

         xvec0 = new Vector3( vec0.getElt(0), 0.0, vec0.getElt(2) );

         ok  =  JNITestutils.chckad ( "vec0 perp e2",
                                       vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );


         vout0 = vec0.perp(e3);

         xvec0 = new Vector3( vec0.getElt(0), vec0.getElt(1), 0.0 );

         ok  =  JNITestutils.chckad ( "vec0 perp e3",
                                       vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //

         //
         // norm tests
         //
         JNITestutils.tcase ( "Vector operation tests - proj" );

         vout0 = vec0.proj(e1);

         xvec0 = new Vector3( vec0.getElt(0), 0.0, 0.0 );

         ok  =  JNITestutils.chckad ( "vec0 proj e1",
                                      vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );

         vout0 = vec0.proj(e2);

         xvec0 = new Vector3( 0.0, vec0.getElt(1), 0.0 );

         ok  =  JNITestutils.chckad ( "vec0 proj e2",
                                       vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );


         vout0 = vec0.proj(e3);

         xvec0 = new Vector3( 0.0, 0.0, vec0.getElt(2) );

         ok  =  JNITestutils.chckad ( "vec0 proj e3",
                                       vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );



         //
         // --------Case-----------------------------------------------
         //

         //
         // scale tests
         //
         JNITestutils.tcase ( "Vector operation tests - rotate " +
                              "(axis index and angle)"             );



         vout0 = e1.rotate( 3, -90.0*RPD );

         xvec0 = e2;


         ok  =  JNITestutils.chckad ( "e1 about e3",
                                       vout0.toArray(),
                                       "~~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );

         vout0 = e2.rotate( 1, -90.0*RPD );

         xvec0 = e3;


         ok  =  JNITestutils.chckad ( "e2 about e1",
                                       vout0.toArray(),
                                       "~~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );

         vout0 = e3.rotate( 2, -90.0*RPD );

         xvec0 = e1;


         ok  =  JNITestutils.chckad ( "e3 about e2",
                                       vout0.toArray(),
                                       "~~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );


         /*

         Currently this case is not an error, but should it be?

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Rotate about coordinate axis - bad index" );


         try
         {
            vout0 = vec0.rotate( -1, 90*RPD );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INDEXOUTOFRANGE)", ex );

         }

         */




         //
         // --------Case-----------------------------------------------
         //

         //
         // scale tests
         //
         JNITestutils.tcase ( "Vector operation tests - rotate " +
                              "(vector and angle)"                );



         vout0 = e1.rotate( e3, 90.0*RPD );

         xvec0 = e2;


         ok  =  JNITestutils.chckad ( "e1 about e3",
                                       vout0.toArray(),
                                       "~~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );

         vout0 = e2.rotate( e1, 90.0*RPD );

         xvec0 = e3;


         ok  =  JNITestutils.chckad ( "e2 about e1",
                                       vout0.toArray(),
                                       "~~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );

         vout0 = e3.rotate( e2, 90.0*RPD );

         xvec0 = e1;


         ok  =  JNITestutils.chckad ( "e3 about e2",
                                       vout0.toArray(),
                                       "~~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );



         //
         // --------Case-----------------------------------------------
         //

         //
         // scale tests
         //
         JNITestutils.tcase ( "Vector operation tests - scale" );

         vout0 = vec0.scale( -2.0 );

         xvec0 = new Vector3( -6.0, -8.0, -24.0 );

         ok  =  JNITestutils.chckad ( "vec0 * -2",
                                       vout0.toArray(),
                                       "~/",
                                       xvec0.toArray(),
                                       TIGHT_TOL          );



         //
         // --------Case-----------------------------------------------
         //

         //
         // sep tests
         //
         JNITestutils.tcase ( "Vector operation tests - sep" );


         sep = e1.sep( e2 );

         ok  =  JNITestutils.chcksd ( "e1 sep e2",
                                       sep,
                                       "~",
                                       90.0*RPD,
                                       TIGHT_TOL          );

         sep = e2.sep( e3 );

         ok  =  JNITestutils.chcksd ( "e2 sep e3",
                                       sep,
                                       "~",
                                       90.0*RPD,
                                       TIGHT_TOL          );

         sep = e3.sep( e1 );

         ok  =  JNITestutils.chcksd ( "e3 sep e1",
                                       sep,
                                       "~",
                                       90.0*RPD,
                                       TIGHT_TOL          );

         sep = e1.sep( e1 );

         ok  =  JNITestutils.chcksd ( "e1 sep e1",
                                       sep,
                                       "~",
                                       0.0,
                                       TIGHT_TOL          );
         sep = e2.sep( e2 );

         ok  =  JNITestutils.chcksd ( "e2 sep e2",
                                       sep,
                                       "~",
                                       0.0,
                                       TIGHT_TOL          );
         sep = e3.sep( e3 );

         ok  =  JNITestutils.chcksd ( "e3 sep e3",
                                       sep,
                                       "~",
                                       0.0,
                                       TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //

         //
         // sub tests
         //
         JNITestutils.tcase ( "Vector operation tests - sub" );

         vout3 = vec3.sub ( vec6 );

         vtest3 = new Vector3 ( new double[] { 17.0, 22.0, 90.7 } );

         ok  =  JNITestutils.chckad ( "vout3/sub",
                                       vout3.toArray(),
                                       "~/",
                                       vtest3.toArray(),
                                       TIGHT_TOL          );



         //
         // --------Case-----------------------------------------------
         //

         //
         // toArray tests
         //
         JNITestutils.tcase ( "Vector operation tests - toArray" );


         vArray = vec0.toArray();

         xArray = new double[3];

         for ( int i = 0;  i < 3;  i++ )
         {
            xArray[i] = vec0.getElt(i);
         }

         //
         // Note that we're looking for an exact match.
         //
         ok  =  JNITestutils.chckad ( "vec0.toArray()",
                                      vArray,
                                      "=",
                                      xArray,
                                      0.0              );


         //
         // --------Case-----------------------------------------------
         //

         //
         // toString tests
         //
         JNITestutils.tcase ( "Vector operation tests - toString" );


         xvec0 = new Vector3( -1.e-100, -2e-200, -3e-300 );

         //
         // We expect 17 mantissa digits in each component.
         //

         xstr0 = "(-1.0000000000000000e-100, " +
                  "-2.0000000000000000e-200, "  +
                  "-3.0000000000000000e-300)";

         ok  =  JNITestutils.chcksc( "xvec0.toString() (0)",
                                     xvec0.toString(),
                                      "=",
                                      xstr0                  );



         xvec0 = new Vector3(  1.e-100,  2e-200,  3e-300 );

         //
         // We expect 17 mantissa digits in each component.
         //

         xstr0 = "( 1.0000000000000000e-100, " +
                  " 2.0000000000000000e-200, "  +
                  " 3.0000000000000000e-300)";

         ok  =  JNITestutils.chcksc( "xvec0.toString() (1)",
                                     xvec0.toString(),
                                      "=",
                                      xstr0               );



         xvec0 = new Vector3(  1.e100,  2e200,  3e300 );

         //
         // We expect 17 mantissa digits in each component.
         //

         xstr0 = "( 1.0000000000000000e+100, " +
                  " 2.0000000000000000e+200, "  +
                  " 3.0000000000000000e+300)";

         ok  =  JNITestutils.chcksc( "xvec0.toString() (2)",
                                     xvec0.toString(),
                                      "=",
                                      xstr0                );

         xvec0 = new Vector3(  1.e0,  2e0,  3e0 );

         //
         // We expect 17 mantissa digits in each component.
         //

         xstr0 = "(  1.0000000000000000e+00,   " +
                    "2.0000000000000000e+00,   " +
                    "3.0000000000000000e+00)";

         ok  =  JNITestutils.chcksc( "xvec0.toString() (2)",
                                     xvec0.toString(),
                                      "=",
                                      xstr0                );




         //
         // --------Case-----------------------------------------------
         //

         //
         // ucross tests
         //
         JNITestutils.tcase ( "Vector operation tests - ucross" );

         vout0 = ( e1.scale(2) ).ucross( e2.scale(3) );

         xvec0 = e3;

         ok    =  JNITestutils.chckad ( "2*e1 x 3*e2",
                                        vout0.toArray(),
                                        "~~/",
                                        xvec0.toArray(),
                                        TIGHT_TOL        );

         vout0 = ( e2.scale(2) ).ucross( e3.scale(3) );

         xvec0 = e1;

         ok    =  JNITestutils.chckad ( "2*e2 x 3*e3",
                                        vout0.toArray(),
                                        "~~/",
                                        xvec0.toArray(),
                                        TIGHT_TOL        );

         vout0 = ( e3.scale(2) ).ucross( e1.scale(3) );

         xvec0 = e2;

         ok    =  JNITestutils.chckad ( "2*e3 x 3*e1",
                                        vout0.toArray(),
                                        "~~/",
                                        xvec0.toArray(),
                                        TIGHT_TOL        );

         //
         // Check a degenerate case.
         //
         vout0 = ( e3.scale(2) ).ucross( e3.scale(3) );

         xvec0 = zeroVec;

         ok    =  JNITestutils.chckad ( "2*e3 x 3*e1",
                                        vout0.toArray(),
                                        "~",
                                        xvec0.toArray(),
                                        TIGHT_TOL        );




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

} /* End f_Vector3 */

