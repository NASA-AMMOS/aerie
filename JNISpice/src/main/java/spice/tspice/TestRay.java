
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestRay provides methods that implement test families for
the class Ray.

<p>Version 1.0.0 01-DEC-2009 (NJB)
*/
public class TestRay extends Object
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
   Test Ray and associated classes.
   */
   public static boolean f_Ray()

      throws SpiceException
   {
      //
      // Constants
      //
      final int                         PLMAX     = 4;

      final double                      SQ2       = Math.sqrt(2.0);
      final double                      TIGHT_TOL = 1.e-12;

      //
      // Local variables
      //
      Ray                               ray0;
      Ray                               ray1;
      Ray                               ray2;

      Vector3                           dir;
      Vector3                           dir0;
      Vector3                           np;
      Vector3                           point;
      Vector3                           uDir;
      Vector3                           vertex;
      Vector3                           xDir;
      Vector3                           xPoint;
      Vector3                           z;

      boolean                           ok;

      double[]                          dirArray;
      double[]                          pointArray;

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

         JNITestutils.topen ( "f_Ray" );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create Ray using zero " +
                               "direction vector."               );

         try
         {
            dir   = new Vector3( 0.0, 0.0, 0.0 );
            point = new Vector3( 1.0, 0.0, 0.0 );

            ray0 = new Ray( point, dir );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(ZEROVECTOR)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(ZEROVECTOR)", ex );
         }





         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test zero-args constructor."  );


         ray0 = new Ray();

         xDir  = new Vector3( 0.0, 0.0, 1.0 );

         dir   = ray0.getDirection();

         //
         //    We expect the direction vector to be the unit +Z basis vector.
         //

         ok    = JNITestutils.chckad( "dir",
                                      dir.toArray(),
                                      "~~",
                                      xDir.toArray(),
                                      TIGHT_TOL );

         //
         // We expect the vertex to be the origin.
         //
         xPoint = new Vector3( 0.0, 0.0, 0.0 );

         point  = ray0.getVertex();

         ok    = JNITestutils.chckad( "point",
                                      point.toArray(),
                                      "~~",
                                      xPoint.toArray(),
                                      TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Create ray using " +
                               "direction vector and point. " +
                               "Test getVertex and getDirection as well."  );


         dir   = new Vector3( 1.0, 2.0, 3.0 );
         point = new Vector3( 4.0, 3.0, 2.0 );

         ray0 = new Ray( point, dir );

         //
         // Check ray's direction vector. We expect to get back a unit
         // length copy of the input direction vector.
         //
         uDir  = ray0.getDirection();

         xDir  = dir.hat();


         ok = JNITestutils.chckad( "dir",
                                   uDir.toArray(),
                                   "~~",
                                   xDir.toArray(),
                                   TIGHT_TOL          );
         //
         // Check ray's point. We expect to get back the input point.
         //
         ok = JNITestutils.chckad( "point",
                                   ray0.getVertex().toArray(),
                                   "~~",
                                   point.toArray(),
                                   TIGHT_TOL          );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor."  );



         dir0  = new Vector3( 3.0, 2.0, 1.0 );

         point = new Vector3( 5.0, 6.0, 7.0 );

         ray0 = new Ray( point, dir0 );
         ray1 = new Ray( point, dir0 );

         ray2 = new Ray( ray0 );

         //
         // Make sure that changing ray0 doesn't affect ray1.
         //
         ray0 = new Ray( dir0, point );

         dir   = ray2.getDirection();
         xDir  = ray1.getDirection();

         ok    = JNITestutils.chckad( "dir",
                                      dir.toArray(),
                                      "~~",
                                      xDir.toArray(),
                                      TIGHT_TOL );

         point  = ray2.getVertex();
         xPoint = ray1.getVertex();

         ok    = JNITestutils.chckad( "point",
                                      point.toArray(),
                                      "~~",
                                      xPoint.toArray(),
                                      TIGHT_TOL );








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

