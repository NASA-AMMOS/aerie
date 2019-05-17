
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestLine provides methods that implement test families for
the class Line.

<p>Version 1.0.0 01-DEC-2009 (NJB)
*/
public class TestLine extends Object
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
   Test Line and associated classes.
   */
   public static boolean f_Line()

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
      Line                              line0;
      Line                              line1;
      Line                              line2;

      Ray                               ray0;

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

         JNITestutils.topen ( "f_Line" );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create Line using zero " +
                               "direction vector."               );

         try
         {
            dir   = new Vector3( 0.0, 0.0, 0.0 );
            point = new Vector3( 1.0, 0.0, 0.0 );

            line0 = new Line( point, dir );

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


         line0 = new Line();

         xDir  = new Vector3( 0.0, 0.0, 1.0 );

         dir   = line0.getDirection();

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

         point  = line0.getPoint();

         ok    = JNITestutils.chckad( "point",
                                      point.toArray(),
                                      "~~",
                                      xPoint.toArray(),
                                      TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Create line using " +
                               "direction vector and point. " +
                               "Test getPoint and getDirection as well."  );


         dir   = new Vector3( 1.0, 2.0, 3.0 );
         point = new Vector3( 4.0, 3.0, 2.0 );

         line0 = new Line( point, dir );

         //
         // Check line's direction vector. We expect to get back a unit
         // length copy of the input direction vector.
         //
         uDir  = line0.getDirection();

         xDir  = dir.hat();


         ok = JNITestutils.chckad( "dir",
                                   uDir.toArray(),
                                   "~~",
                                   xDir.toArray(),
                                   TIGHT_TOL          );
         //
         // Check line's point. We expect to get back the input point.
         //
         ok = JNITestutils.chckad( "point",
                                   line0.getPoint().toArray(),
                                   "~~",
                                   point.toArray(),
                                   TIGHT_TOL          );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor."  );



         dir0  = new Vector3( 3.0, 2.0, 1.0 );

         point = new Vector3( 5.0, 6.0, 7.0 );

         line0 = new Line( point, dir0 );
         line1 = new Line( point, dir0 );

         line2 = new Line( line0 );

         //
         // Make sure that changing line0 doesn't affect line1.
         //
         line0 = new Line( dir0, point );

         dir   = line2.getDirection();
         xDir  = line1.getDirection();

         ok    = JNITestutils.chckad( "dir",
                                      dir.toArray(),
                                      "~~",
                                      xDir.toArray(),
                                      TIGHT_TOL );

         point  = line2.getPoint();
         xPoint = line1.getPoint();

         ok    = JNITestutils.chckad( "point",
                                      point.toArray(),
                                      "~~",
                                      xPoint.toArray(),
                                      TIGHT_TOL );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test ray-based constructor."  );



         dir0  = new Vector3( 3.0, 2.0, 1.0 );

         point = new Vector3( 5.0, 6.0, 7.0 );

         ray0  = new Ray( point, dir0 );

         line0 = new Line( ray0 );

         dir   = line0.getDirection();
         xDir  = ray0.getDirection();

         ok    = JNITestutils.chckad( "dir",
                                      dir.toArray(),
                                      "~~",
                                      xDir.toArray(),
                                      TIGHT_TOL );

         point  = line0.getPoint();
         xPoint = ray0.getVertex();

         ok    = JNITestutils.chckad( "point",
                                      point.toArray(),
                                      "~~",
                                      xPoint.toArray(),
                                      TIGHT_TOL );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getRay."  );


         dir0  = new Vector3( 3.0, 2.0, 1.0 );

         xPoint = new Vector3( 5.0, 6.0, 7.0 );

         line0 = new Line( xPoint, dir0 );

         ray0  = line0.getRay();

         //
         // We expect to get a ray whose vertex is `point' and
         // whose direction is a unit length version of dir0.
         //

         point = ray0.getVertex();

         ok    = JNITestutils.chckad( "point",
                                      point.toArray(),
                                      "~~",
                                      xPoint.toArray(),
                                      TIGHT_TOL );
         xDir  = dir0.hat();

         dir   = ray0.getDirection();

         ok    = JNITestutils.chckad( "dir",
                                      dir.toArray(),
                                      "~~",
                                      xDir.toArray(),
                                      TIGHT_TOL );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test getNearPoint."  );


         dir0   = new Vector3( -1.0, -1.0, SQ2 );

         vertex = new Vector3(  2.0,  2.0, 0.0 );

         point  = new Vector3(  0.0,  0.0, 0.0 );

         xPoint = new Vector3( 1.0, 1.0, SQ2 );

         line0 = new Line( vertex, dir0 );

         np    = line0.getNearPoint( point );

         ok    = JNITestutils.chckad( "near point",
                                      np.toArray(),
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

