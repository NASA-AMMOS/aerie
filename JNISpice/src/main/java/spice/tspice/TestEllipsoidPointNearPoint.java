
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.*;


/**
Class TestEllipsoidPointNearPoint provides methods that implement
test families for the class EllipsoidPointNearPoint.

<p>Version 1.0.0 15-DEC-2009 (NJB)
*/
public class TestEllipsoidPointNearPoint extends Object
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
   Test EllipsoidPointNearPoint and associated classes.
   */
   public static boolean f_EllipsoidPointNearPoint()

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
      Ellipse                           limb;

      Ellipsoid                         e0;
      Ellipsoid                         e1;
      Ellipsoid                         e2;

      EllipsoidPointNearPoint           np;

      LatitudinalCoordinates            latCoords;

      String                            outStr;
      String                            xStr;

      Vector3                           v0;
      Vector3                           normal;
      Vector3                           xAxis    = new Vector3( 1.0, 0.0, 0.0 );
      Vector3                           xCenter;
      Vector3                           xSmajor;
      Vector3                           xSminor;

      boolean                           ok;

      double                            r;
      double[]                          radii;
      double[]                          xArray;

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

         JNITestutils.topen ( "f_EllipsoidPointNearPoint" );





         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test ellipsoid-point constructor."  );


         //
         // Create a unit sphere.
         //
         Ellipsoid uSphere = new Ellipsoid();

         //
         // Find the nearest point on the sphere to a given point.
         //

         Vector3 viewpt = new Vector3( 3.0, 4.0, 12.0 );

         np = new EllipsoidPointNearPoint( uSphere, viewpt );

         //
         // Create the expected near point.
         //

         Vector3 xpt = viewpt.hat();

         ok = JNITestutils.chckad ( "near point",
                                    np.getNearPoint().toArray(),
                                    "~~",
                                    xpt.toArray(),
                                    TIGHT_TOL         );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getDistance."  );

         //
         // Just check the distance between the view point and the near
         // point from the last test case. Note that the view point
         // has norm 13.
         //
         ok = JNITestutils.chcksd ( "near point distance",
                                    np.getDistance(),
                                    "~",
                                    12.0,
                                    TIGHT_TOL         );




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

