
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestRayPlaneIntercept provides methods that implement test families for
the class RayPlaneIntercept.

<p>Version 1.0.0 08-DEC-2009 (NJB)
*/
public class TestRayPlaneIntercept extends Object
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
   Test RayPlaneIntercept and associated classes.
   */
   public static boolean f_RayPlaneIntercept()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-9;


      //
      // Local variables
      //
      Plane                             pl0;

      Ray                               ray0;

      RayPlaneIntercept                 rpi0;
      RayPlaneIntercept                 rpi1;
      RayPlaneIntercept                 rpi2;
      RayPlaneIntercept                 rpi3;

      Vector3                           dirVec;
      Vector3                           xpt0;
      Vector3                           xpt1;
      Vector3                           normal;
      Vector3                           v0;
      Vector3                           vertex;

      boolean                           ok;

      double                            cons;
      double                            xAngle;

      int                               i;
      int                               nPts;

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

         JNITestutils.topen ( "f_RayPlaneIntercept" );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Attempt to fetch intercept for a " +
                              "non-intercept case." );



         try
         {
            normal = new Vector3( 0.0, 0.0, 1.0 );
            cons   = -2.0;

            pl0    = new Plane( normal, cons );

            vertex = normal;
            dirVec = normal;

            ray0   = new Ray( vertex, dirVec );

            rpi0   = new RayPlaneIntercept( ray0, pl0 );

            xpt0   = rpi0.getIntercept();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(POINTNOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true,   "SPICE(POINTNOTFOUND)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test constructor: single intercept case."  );


         normal = new Vector3( 0.0, 0.0, 1.0 );
         cons   = -2.0;

         pl0    = new Plane( normal, cons );

         vertex = normal;
         dirVec = new Vector3( -3.0, -2.0, -1.0 );

         ray0   = new Ray( vertex, dirVec );



         //
         // xpt1 is the expected intercept.
         //
         xpt1   = new Vector3( -9.0, -6.0, -2.0 );


         rpi0   = new RayPlaneIntercept( ray0, pl0 );

         xpt0   = rpi0.getIntercept();


         //
         // Check the intercept count.
         //
         ok = JNITestutils.chcksi ( "n intercept points",
                                    rpi0.getInterceptCount(),
                                    "=",
                                    1,
                                    0                 );

         //
         // Check the intercept.
         //
         ok = JNITestutils.chckad ( "intercept",
                                    xpt0.toArray(),
                                    "~~/",
                                    xpt1.toArray(),
                                    TIGHT_TOL                 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test constructor: non-intercept case."  );


         normal = new Vector3( 0.0, 0.0, 1.0 );
         cons   = -2.0;

         pl0    = new Plane( normal, cons );

         vertex = normal;
         dirVec = normal;

         ray0   = new Ray( vertex, dirVec );



         //
         // xpt1 is the expected intercept.
         //
         xpt1   = new Vector3( -9.0, -6.0, -2.0 );



         rpi0   = new RayPlaneIntercept( ray0, pl0 );

         //
         // Check the intercept count.
         //
         ok = JNITestutils.chcksi ( "n intercept points",
                                    rpi0.getInterceptCount(),
                                    "=",
                                    0,
                                    0                 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test constructor: ray in plane case."  );


         //
         // The plane is the X-Y coordinate plane.
         //
         normal = new Vector3( 0.0, 0.0, 1.0 );
         cons   = 0.0;

         pl0    = new Plane( normal, cons );

         //
         // The ray is the positive X-axis.
         //
         vertex = new Vector3();
         dirVec = new Vector3( 1.0, 0.0, 0.0 );

         ray0   = new Ray( vertex, dirVec );



         rpi0   = new RayPlaneIntercept( ray0, pl0 );

         //
         // Check the intercept count.
         //
         ok = JNITestutils.chcksi ( "n intercept points",
                                    rpi0.getInterceptCount(),
                                    "=",
                                    RayPlaneIntercept.INFINITY,
                                    0                           );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test constructor: ray vertex in plane, " +
                              "rest of ray out of plane case."            );


         //
         // The plane is the X-Y coordinate plane.
         //
         normal = new Vector3( 0.0, 0.0, 1.0 );
         cons   = 0.0;

         pl0    = new Plane( normal, cons );

         //
         // The ray is the positive X-axis.
         //
         vertex = new Vector3();
         dirVec = new Vector3( 1.0, 0.0, 1.0 );

         ray0   = new Ray( vertex, dirVec );



         rpi0   = new RayPlaneIntercept( ray0, pl0 );

         //
         // Check the intercept count.
         //
         ok = JNITestutils.chcksi ( "n intercept points",
                                    rpi0.getInterceptCount(),
                                    "=",
                                    1,
                                    0                           );

         xpt0 = rpi0.getIntercept();

         //
         // Check the intercept.
         //
         ok = JNITestutils.chckad ( "intercept",
                                    xpt0.toArray(),
                                    "~~/",
                                    vertex.toArray(),
                                    TIGHT_TOL                 );



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

