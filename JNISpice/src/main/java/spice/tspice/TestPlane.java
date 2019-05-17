
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestPlane provides methods that implement test families for
the class Plane.

<h3>Version 2.0.0 17-DEC-2016 (NJB) </h3>

   Added test cases for Plane creation from invalid arrays.

<h3>Version 1.0.0 15-DEC-2009 (NJB) </h3>
*/
public class TestPlane extends Object
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
   Test Plane and associated classes.
   */
   public static boolean f_Plane()

      throws SpiceException
   {
      //
      // Constants
      //
      final int                         PLMAX     = 4;

      final double                      TIGHT_TOL = 1.e-12;

      //
      // Local variables
      //
      Plane                             plane;
      Plane                             plane2;

      Vector3                           normal;
      Vector3                           point;
      Vector3                           span1;
      Vector3                           span2;
      Vector3[]                         spanVecs;
      Vector3                           v0;
      Vector3                           v1;
      Vector3                           xNormal;
      Vector3                           xPoint;
      Vector3                           z;

      boolean                           ok;

      double                            constant;
      double[]                          normalArray;
      double[]                          planeArray;
      double                            xConstant;

      int                               handle;
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

         JNITestutils.topen ( "f_Plane" );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create plane using zero " +
                               "normal vector and constant."        );

         try
         {
            normal = new Vector3( 0.0, 0.0, 0.0 );

            plane = new Plane( normal, 1.0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(ZEROVECTOR)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(ZEROVECTOR)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create plane using zero " +
                               "array."        );

         try
         {
         
            planeArray = new double[ PLMAX ];

          
            plane = new Plane( planeArray );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(ZEROVECTOR)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(ZEROVECTOR)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create plane using array of " +
                               "length 2."                             );

         try
         {
         
            planeArray = new double[ 2 ];

          
            plane = new Plane( planeArray );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BADARRAYLENGTH)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(BADARRAYLENGTH)", ex );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create plane using zero " +
                               "normal vector and point."          );

         try
         {
            normal = new Vector3( 0.0, 0.0, 0.0 );

            point  = new Vector3( 1.0, 1.0, 1.0 );

            plane  = new Plane( normal, point );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(ZEROVECTOR)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(ZEROVECTOR)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create plane using point " +
                               "and spanning vectors, but spanning " +
                               "vectors are linearly dependent."       );

         try
         {
            point = new Vector3( 1.0, 1.0, 1.0 );
            span1 = new Vector3( 0.0, 0.0, 0.1 );

            span2 = span1;

            plane = new Plane( point, span1, span2 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DEGENERATECASE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(DEGENERATECASE)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Create plane using " +
                               "normal vector and constant."        );

         //
         // This vector has norm == 13.
         //
         normal = new Vector3( 3.0, 4.0, 12.0 );

         plane   = new Plane( normal, 26.0 );

         //
         // Check plane's normal vector. We expect to get back a unit
         // length copy of the input vector.
         //
         normal  = plane.getNormal();

         xNormal = normal.hat();


         ok = JNITestutils.chckad( "normal",
                                   normal.toArray(),
                                   "~~",
                                   xNormal.toArray(),
                                   TIGHT_TOL          );
         //
         // Check plane's constant. We expect to get back a constant
         // consistent with the unit length copy of the input vector.
         // In this case, the expected constant is 2.
         //
         xConstant = 2.0;

         constant  = plane.getConstant();

         ok = JNITestutils.chcksd( "constant", constant,
                                   "~",        xConstant,
                                   TIGHT_TOL              );



         //
         // --------Case-----------------------------------------------
         //
         //
         JNITestutils.tcase (  "Create plane using " +
                               "normal vector and point."  );

         //
         // This vector has norm == 13.
         //
         normal = new Vector3( 3.0, 4.0, 12.0 );

         //
         // This vector has inner product with normal equal to 52,
         // so the inner product with the unit length normal is 4.
         //
         point  = new Vector3( 8.0, -8.0, 5.0 );

         plane  = new Plane( normal, point );

         //
         // Check plane's normal vector. We expect to get back a unit
         // length copy of the input vector.
         //
         xNormal = normal.hat();

         normal  = plane.getNormal();


         ok = JNITestutils.chckad( "normal",
                                   normal.toArray(),
                                   "~~",
                                   xNormal.toArray(),
                                   TIGHT_TOL          );
         //
         // Check plane's constant. We expect to get back a constant
         // consistent with the unit length copy of the input vector.
         // In this case, the expected constant is 2.
         //
         xConstant = 4.0;

         constant  = plane.getConstant();

         ok = JNITestutils.chcksd( "constant", constant,
                                   "~",        xConstant,
                                   TIGHT_TOL              );


         //
         // --------Case-----------------------------------------------
         //
         //
         JNITestutils.tcase (  "Create plane using point and " +
                               "spanning vectors."  );

         //
         // This vector has norm == 13.
         //
         normal = new Vector3( 3.0, 4.0, 12.0 );

         //
         // Create two spanning vectors using this normal vector.
         //
         z      = new Vector3( 0.0, 0.0, 1.0 );

         span1  = z.cross( normal );
         span2  = normal.cross( span1 );


         //
         // This vector has inner product with normal equal to 52,
         // so the inner product with the unit length normal is 4.
         //
         point  = new Vector3( 8.0, -8.0, 5.0 );

         plane  = new Plane( point, span1, span2 );

         //
         // Check plane's normal vector. We expect to get back a unit
         // length copy of the input vector. Note that this test
         // depends on the inner product of the input point and the
         // normal vector being positive.
         //
         xNormal = normal.hat();

         normal  = plane.getNormal();


         ok = JNITestutils.chckad( "normal",
                                   normal.toArray(),
                                   "~~",
                                   xNormal.toArray(),
                                   TIGHT_TOL          );
         //
         // Check plane's constant. We expect to get back a constant
         // consistent with the unit length copy of the input vector.
         // In this case, the expected constant is 2.
         //
         xConstant = 4.0;

         constant  = plane.getConstant();

         ok = JNITestutils.chcksd( "constant", constant,
                                   "~",        xConstant,
                                   TIGHT_TOL              );


         //
         // --------Case-----------------------------------------------
         //
         //
         JNITestutils.tcase ( "Test copy constructor: ensure copy is deep." );


         //
         // This vector has norm == 13.
         //
         normal = new Vector3( 3.0, 4.0, 12.0 );

         plane  = new Plane( normal, 26.0 );

         //
         // Make a new plane from `plane'. Check the normal and
         // constant of the new plane.
         //
         plane2  = new Plane( plane );

         xNormal = plane.getNormal();

         ok = JNITestutils.chckad( "normal",
                                   plane2.getNormal().toArray(),
                                   "~~",
                                   xNormal.toArray(),
                                   TIGHT_TOL          );


         xConstant = plane.getConstant();

         ok = JNITestutils.chcksd( "constant", plane2.getConstant(),
                                   "~",        xConstant,
                                   TIGHT_TOL                        );

         //
         // Change plane; make sure plane2 doesn't change.
         //
         plane  = new Plane( normal, 52.0 );

         //
         // Test plane2 against the old expected normal
         // and constant values.
         //

         ok = JNITestutils.chckad( "normal",
                                   plane2.getNormal().toArray(),
                                   "~~",
                                   xNormal.toArray(),
                                   TIGHT_TOL          );

         ok = JNITestutils.chcksd( "constant", plane2.getConstant(),
                                   "~",        xConstant,
                                   TIGHT_TOL                        );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Retrieve a point from a plane." );

         //
         // This vector has norm == 13.
         //
         normal   = new Vector3( 3.0, 4.0, 12.0 );

         constant = 13.0;

         plane    = new Plane( normal, constant );

         xPoint   = normal.hat();

         point    = plane.getPoint();

         ok = JNITestutils.chckad( "point",
                                   point.toArray(),
                                   "~~",
                                   xPoint.toArray(),
                                   TIGHT_TOL          );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Retrieve spanning vectors from a plane." );

         //
         // This vector has norm == 13.
         //
         normal   = new Vector3( 3.0, 4.0, 12.0 );

         constant = 13.0;

         plane    = new Plane( normal, constant );

         spanVecs = plane.getSpanningVectors();

         //
         // Make sure the spanning vectors are orthogonal to the
         // plane's normal vector.
         //

         for ( i = 0;  i < 2;  i++ )
         {
            ok = JNITestutils.chcksd( "dot product w/ normal " + i,
                                      spanVecs[i].dot( normal ),
                                      "~",
                                      0.0,
                                      TIGHT_TOL                );
         }

         //
         // Make sure the spanning vectors are orthogonal to
         // each other.
         //
         ok = JNITestutils.chcksd( "dot product of spanning vectors",
                                   spanVecs[0].dot( spanVecs[1] ),
                                   "~",
                                   0.0,
                                   TIGHT_TOL                          );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Project a vector orthogonally onto  a plane." );

         normal   = new Vector3( 0.0, 0.0, 1.0 );

         constant = 2.0;

         plane    = new Plane( normal, constant );

         v0       = new Vector3( 3.0, 4.0, 1.0 );

         xPoint   = new Vector3( 3.0, 4.0, 2.0 );

         v1       = plane.project( v0 );

         ok = JNITestutils.chckad( "v1",
                                   v1.toArray(),
                                   "~~",
                                   xPoint.toArray(),
                                   TIGHT_TOL          );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Pack plane contents into an array." );

         //
         // This vector has norm == 13.
         //
         normal     = new Vector3( 3.0, 4.0, 12.0 );

         constant   = 13.0;

         plane      = new Plane( normal, constant );

         planeArray = plane.toArray();

         //
         // The returned array should have length PLMAX.
         //
         ok = JNITestutils.chcksi( "array length",
                                   planeArray.length,
                                   "~",
                                   PLMAX,
                                   0                      );
         //
         // Check the normal vector stored in planeArray.
         //
         xNormal     = plane.getNormal();

         normalArray = new double[3];

         System.arraycopy( planeArray, 0, normalArray, 0, 3 );

         ok = JNITestutils.chckad( "normal slice of planeArray",
                                   normalArray,
                                   "~~",
                                   xNormal.toArray(),
                                   TIGHT_TOL          );

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

