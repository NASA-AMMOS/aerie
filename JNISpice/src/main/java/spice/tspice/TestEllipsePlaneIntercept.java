
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestEllipsePlaneIntercept provides methods that implement test
families for the class EllipsePlaneIntercept.

<p>Version 1.0.0 09-DEC-2009 (NJB)
*/
public class TestEllipsePlaneIntercept extends Object
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
   Test EllipsePlaneIntercept and associated classes.
   */
   public static boolean f_EllipsePlaneIntercept()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-9;
      final double                      SQ2       = Math.sqrt( 2.0 );

      final int                         NELT      = 9;
      //
      // Local variables
      //
      Ellipse                           e0;

      EllipsePlaneIntercept             epx0;

      Ellipse                           e1;
      Ellipse                           e2;
      Ellipse                           e3;

      Plane                             pl0;

      String                            outStr;
      String                            xStr;

      Vector3                           center;
      Vector3                           normal;
      Vector3                           smajor;
      Vector3                           sminor;
      Vector3                           v0;
      Vector3                           v1;
      Vector3                           v2;
      Vector3                           v3;
      Vector3                           xpt0;
      Vector3                           xpt1;
      Vector3[]                         intercepts;



      boolean                           ok;

      double                            cons;

      int                               i;
      int                               n;

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

         JNITestutils.topen ( "f_EllipsePlaneIntercept" );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: attemp to fetch intercepts for a " +
                               "non-intercept case."                         );

         try
         {
            v0 = new Vector3( 1.0, 2.0, 3.0 );
            v1 = new Vector3( 2.0, 0.0, 0.0 );
            v2 = new Vector3( 0.0, 1.0, 0.0 );

            e0 = new Ellipse( v0, v1, v2 );

            normal = new Vector3( 0.0, 0.0, 1.0 );
            cons   = 5.0;

            pl0    = new Plane( normal, cons );

            epx0   = new EllipsePlaneIntercept( e0, pl0 );

            intercepts = epx0.getIntercepts();


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(POINTNOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,   "SPICE(POINTNOTFOUND)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: attempt to fetch intercepts for an " +
                               "infinite intercept case."                    );

         try
         {
            v0 = new Vector3( 0.0, 0.0, 0.0 );
            v1 = new Vector3( 2.0, 0.0, 0.0 );
            v2 = new Vector3( 0.0, 1.0, 0.0 );

            e0 = new Ellipse( v0, v1, v2 );

            normal = new Vector3( 0.0, 0.0, 1.0 );
            cons   = 0.0;

            pl0    = new Plane( normal, cons );

            epx0   = new EllipsePlaneIntercept( e0, pl0 );

            intercepts = epx0.getIntercepts();


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(POINTNOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //
            //ex.printStackTrace();

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
         JNITestutils.tcase ( "Test constructor: two-point intercept. " +
                              "Also test getInterceptCount, wasFound, " +
                              "and getIntercepts."  );


         v0 = new Vector3( 0.0, 0.0, 0.0 );
         v1 = new Vector3( 2.0, 0.0, 0.0 );
         v2 = new Vector3( 0.0, 1.0, 0.0 );

         e0 = new Ellipse( v0, v1, v2 );

         normal = new Vector3( 1.0, 0.0, 0.0 );
         cons   = 0.0;

         pl0    = new Plane( normal, cons );

         epx0   = new EllipsePlaneIntercept( e0, pl0 );

         intercepts = epx0.getIntercepts();

         //
         // Check whether a finite intercept count was found.
         //

         ok = JNITestutils.chcksl ( "found", epx0.wasFound(), true );

         //
         // Check intercept count.
         //
         n  = epx0.getInterceptCount();

         ok = JNITestutils.chcksi ( "n", n, "=", 2, 0 );


         //
         // Check the intercepts. We expect them to occur at
         //
         //    ( 0, +/- 1, 0)
         //
         // but we don't know the order in which they'll be stored.
         //
         //
         if ( intercepts[0].getElt(1) > 0 )
         {
            //
            // The first intercept has a positive y-component.
            //
            xpt0 = new Vector3( 0.0,  1.0, 0.0 );
            xpt1 = new Vector3( 0.0, -1.0, 0.0 );
         }
         else
         {
            xpt0 = new Vector3( 0.0, -1.0, 0.0 );
            xpt1 = new Vector3( 0.0,  1.0, 0.0 );
         }


         ok = JNITestutils.chckad ( "first intercept",
                                    intercepts[0].toArray(),
                                    "~~/",
                                    xpt0.toArray(),
                                    TIGHT_TOL                      );

         ok = JNITestutils.chckad ( "second intercept",
                                    intercepts[1].toArray(),
                                    "~~/",
                                    xpt1.toArray(),
                                    TIGHT_TOL                      );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test constructor: no intercept. " +
                              "Also test getInterceptCount and wasFound." );

         v0 = new Vector3( 1.0, 2.0, 3.0 );
         v1 = new Vector3( 2.0, 0.0, 0.0 );
         v2 = new Vector3( 0.0, 1.0, 0.0 );

         e0 = new Ellipse( v0, v1, v2 );

         normal = new Vector3( 0.0, 0.0, 1.0 );
         cons   = 5.0;

         pl0    = new Plane( normal, cons );

         epx0   = new EllipsePlaneIntercept( e0, pl0 );

         //
         // Check whether a finite intercept count was found.
         //

         ok = JNITestutils.chcksl ( "found", epx0.wasFound(), false );

         //
         // Check intercept count.
         //
         n  = epx0.getInterceptCount();

         ok = JNITestutils.chcksi ( "n", n, "=", 0, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test constructor: infinite intercept. " +
                              "Also test getInterceptCount and wasFound." );


         v0 = new Vector3( 0.0, 0.0, 0.0 );
         v1 = new Vector3( 2.0, 0.0, 0.0 );
         v2 = new Vector3( 0.0, 1.0, 0.0 );

         e0 = new Ellipse( v0, v1, v2 );

         normal = new Vector3( 0.0, 0.0, 1.0 );
         cons   = 0.0;

         pl0    = new Plane( normal, cons );

         epx0   = new EllipsePlaneIntercept( e0, pl0 );

         //
         // Check whether a finite intercept count was found.
         //

         ok = JNITestutils.chcksl ( "found", epx0.wasFound(), false );

         //
         // Check intercept count.
         //
         n  = epx0.getInterceptCount();

         ok = JNITestutils.chcksi ( "n", n, "=",
                                           EllipsePlaneIntercept.INFINITY, 0 );



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

