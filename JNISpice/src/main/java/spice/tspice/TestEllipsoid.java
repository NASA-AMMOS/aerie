
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.*;


/**
Class TestEllipsoid provides methods that implement test families for
the class Ellipsoid.

<p>Version 1.0.0 15-DEC-2009 (NJB)
*/
public class TestEllipsoid extends Object
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
   Test Ellipsoid and associated classes.
   */
   public static boolean f_Ellipsoid()

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

         JNITestutils.topen ( "f_Ellipsoid" );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: attempt to create ellipsoid " +
                               "with bad radii."                      );

         try
         {

            e0 = new Ellipsoid( -1.0, 2.0, 3.0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(VALUEOUTOFRANGE)", ex );
         }


         try
         {

            e0 = new Ellipsoid( 1.0, 0.0, 3.0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(VALUEOUTOFRANGE)", ex );
         }

         try
         {

            e0 = new Ellipsoid( 1.0, 2.0, -3.0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(VALUEOUTOFRANGE)", ex );
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

         e0 = new Ellipsoid();

         //
         // We expect to have a unit sphere.
         //
         Vector3 uSphere = new Vector3( 1.0, 1.0, 1.0 );

         ok = JNITestutils.chckad ( "e0 radii",
                                    e0.getRadii(),
                                    "~~",
                                    uSphere.toArray(),
                                    TIGHT_TOL         );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test scalar-based constructor and " +
                              "getRadii()." );

         e0     = new Ellipsoid( 5.0, 4.0, 3.0 );

         xArray = (  new Vector3( 5.0, 4.0, 3.0 )  ).toArray();

         ok = JNITestutils.chckad ( "e0 radii",
                                    e0.getRadii(),
                                    "~~",
                                    xArray,
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor."  );


         //
         // Create e0 and an identical ellipsoid.
         //
         e0 = new Ellipsoid( 1.0, 2.0, 3.0 );
         e2 = new Ellipsoid( 1.0, 2.0, 3.0 );

         //
         // Make e1: a deep copy of e0.
         //
         e1 = new Ellipsoid( e0 );

         //
         // Change e0 and make sure e1 doesn't change.
         //
         e0 = new Ellipsoid( 4.0, 5.0, 6.0 );


         ok = JNITestutils.chckad ( "e1 radii",
                                    e1.getRadii(),
                                    "~~",
                                    e2.getRadii(),
                                    TIGHT_TOL         );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getNormal."  );

         //
         // We'll work with the unit sphere.
         //
         e0 = new Ellipsoid();

         //
         // Pick a surface point at 30 degrees longitude, 60 degrees latitude.
         //
         latCoords = new LatitudinalCoordinates( 1.0, 30*RPD, 60*RPD );

         v0        = latCoords.toRectangular();

         normal    = e0.getNormal( v0 );

         //
         // `normal' should be identical to v0, up to round-off.
         //
         ok = JNITestutils.chckad ( "normal",
                                    normal.toArray(),
                                    "~~",
                                    v0.toArray(),
                                    TIGHT_TOL         );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getLimb."  );



         //
         // View the unit sphere from (2.0, 0.0, 0.0).
         //
         e0   = new Ellipsoid();

         v0   = new Vector3( 2.0, 0.0, 0.0 );

         limb = e0.getLimb( v0 );

         //
         // The limb should lie in the plane x == 1/2. The limb should
         // be circular with radius sqrt(3)/2.
         //

         xCenter = new Vector3( 0.5, 0.0, 0.0 );

         ok = JNITestutils.chckad ( "limb center",
                                    limb.getCenter().toArray(),
                                    "~~",
                                    xCenter.toArray(),
                                    TIGHT_TOL         );

         //
         // Check the semi-axes:
         //
         //    - They both should have length sqrt(3)/2.
         //
         //    - They both should be orthogonal to the x-axis.
         //
         //    - They should be orthogonal to each other.
         //

         ok = JNITestutils.chcksd ( "semi-major axis norm",
                                    limb.getSemiMajorAxis().norm(),
                                    "~",
                                    SQ3/2,
                                    TIGHT_TOL         );

         ok = JNITestutils.chcksd ( "semi-minor axis norm",
                                    limb.getSemiMinorAxis().norm(),
                                    "~",
                                    SQ3/2,
                                    TIGHT_TOL         );

         ok = JNITestutils.chcksd ( "semi-major axis dot x-axis",
                                    limb.getSemiMajorAxis().dot( xAxis ),
                                    "~",
                                    0.0,
                                    TIGHT_TOL         );

         ok = JNITestutils.chcksd ( "semi-minor axis dot x-axis",
                                    limb.getSemiMinorAxis().dot( xAxis ),
                                    "~",
                                    0.0,
                                    TIGHT_TOL         );

         ok = JNITestutils.chcksd ( "dot product of semi-axes",
                        limb.getSemiMinorAxis().dot( limb.getSemiMajorAxis() ),
                                    "~",
                                    0.0,
                                    TIGHT_TOL         );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString."  );


         e0 = new Ellipsoid( 1.e-100, 2e-200, 3e-300 );

         //
         // We expect 17 mantissa digits in each component.
         //
         String endl = System.getProperty( "line.separator" );

         xStr  = "Ellipsoid Radii:" + endl +
                 "( 1.0000000000000000e-100, " +
                  " 2.0000000000000000e-200, "  +
                  " 3.0000000000000000e-300)";


         // For debugging:
         // System.out.println( xStr );

         // For debugging:
         // System.out.println( e0.toString() );


         ok  =  JNITestutils.chcksc( "e0.toString() (0)",
                                     e0.toString(),
                                      "=",
                                      xStr                   );



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

