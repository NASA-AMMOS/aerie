
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.*;


/**
Class TestEllipsoidLineNearPoint provides methods that implement test
families for the class EllipsoidLineNearPoint.

<p>Version 1.0.0 23-NOV-2009 (NJB)
*/
public class TestEllipsoidLineNearPoint extends Object
{

   //
   // Class constants
   //
   private static String  NATPCK        = "nat.tpc";
   private static String  NATSPK        = "nat.bsp";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test EllipsoidLineNearPoint and associated classes.
   */
   public static boolean f_EllipsoidLineNearPoint()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      SQ3       = Math.sqrt(3.0);

      //
      // Local variables
      //
      Ellipsoid                         e0;

      EllipsoidLineNearPoint            np;

      LatitudinalCoordinates            latCoords;

      Line                              line0;

      String                            outStr;
      String                            xStr;

      Vector3                           dirVec;
      Vector3                           v0;
      Vector3                           vNear;
      Vector3                           normal;
      Vector3                           Z      = new Vector3( 0.0, 0.0, 1.0 );

      boolean                           ok;

      double                            alt;
      double[]                          xArray;

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

         JNITestutils.topen ( "f_EllipsoidLineNearPoint" );






         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test constructor, getNearPoint, and " +
                              "getDistance."  );

         //
         // Create a unit sphere.
         //

         e0 = new Ellipsoid();

         //
         // Create a point on the line.
         //
         v0 = new Vector3( 3.0, 4.0, 12.0 );

         //
         // Create the line's direction vector. We want the direction
         // to be normal to v0.
         //

         dirVec = Z.cross( v0 );

         //
         // Create the line.
         //

         line0 = new Line( v0, dirVec );

         //
         // Find the nearest point on the ellipsoid to the line.
         //

         np = new EllipsoidLineNearPoint( e0, line0 );

         //
         // Check the near point. It should be a unit length version of
         // v0.
         //

         vNear = np.getNearPoint();

         ok    = JNITestutils.chckad( "vNear",
                                      vNear.toArray(),
                                      "~~/",
                                      v0.hat().toArray(),
                                      TIGHT_TOL           );

         //
         // Check the altitude of the line above the near point.
         //

         alt   = np.getDistance();

         ok    = JNITestutils.chcksd( "alt",
                                      alt,
                                      "~",
                                      12.0,
                                      TIGHT_TOL           );

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

