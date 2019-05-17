
package spice.tspice;


//import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;



/**
Class TestEllipsoidPlaneIntercept provides methods that 
implement test families for the class 
EllipsoidPlaneIntercept

<p>Version 1.0.0 17-DEC-2016 (NJB)
*/
public class TestEllipsoidPlaneIntercept extends Object
{
   //
   // Class constants
   //
   static final double                  TIGHT  = 1.e-12; 
   static final int                     PLNSIZ = 4;

   //
   // Class variables
   //
   static boolean                       found;
   static boolean                       ok;

   static Ellipse                       ellipse;
   static Ellipse                       limb;
   static Ellipsoid                     ellipsoid; 
   static EllipsoidPlaneIntercept       intercept;
   static EllipsoidPlaneIntercept       intercept1;
   static EllipsoidPlaneIntercept       intercept2;

   static Plane                         badPlane;
   static Plane                         limbPlane;
   static Plane                         plane;

   static Vector3                       centerLimb;
   static Vector3                       centerIntercept;
   static Vector3                       smajorLimb;
   static Vector3                       smajorIntercept;
   static Vector3                       sminorLimb;
   static Vector3                       sminorIntercept;
   static Vector3                       viewpt;
   static Vector3                       z = new Vector3( 0.0, 0.0, 1.0 );

   static double                        a;
   static double                        b;
   static double                        c;
   static double[]                      ellipseArray;
   static double[]                      planeArray = new double[ PLNSIZ ];
   static double                        tol;




   /**
   Test EllipsoidPlaneIntercept methods.
   */
   public static boolean f_EllipsoidPlaneIntercept()

      throws SpiceException
   {
       
      try
      {


         JNITestutils.topen ( "f_EllipsoidPlaneIntercept" );


         //
         // Constructor tests
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test no-args constructor: make sure " +
                              "constructor can be called."             );

         intercept = new EllipsoidPlaneIntercept();


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test principal constructor by executing "   +
                              "an example program: compare limb computed " +
                              "using the constructor with that computed "  +
                              "by class Ellipsoid's getLimb method."        );
 
         //
         // Choose semi-axis lengths of a triaxial ellipsoid.
         // 
         a = 5000.0;
         b = 3000.0;
         c = 1000.0;

         ellipsoid = new Ellipsoid( a, b, c );

         viewpt    = new Vector3( -7000.0, 6000.0, 2000.0 );

         //
         // Find the limb of the ellipsoid as seen from `viewpt'.
         // Extract the center and semi-axes of the limb.
         //          
         limb       = ellipsoid.getLimb( viewpt );

         centerLimb = limb.getCenter();
         smajorLimb = limb.getSemiMajorAxis();
         sminorLimb = limb.getSemiMinorAxis();

         //
         // Construct the limb plane from the center and the semi-axes,
         // which serve as spanning vectors.
         //
         limbPlane = new Plane( centerLimb, smajorLimb, sminorLimb );

         //
         // Compute the ellipsoid-plane intercept using the 
         // limb plane and the ellipsoid.
         // 
         intercept = new EllipsoidPlaneIntercept( ellipsoid, limbPlane );

 
         //
         // Make sure we have a result.
         //
         ok = JNITestutils.chcksl( "found", intercept.wasFound(), true );

         if ( intercept.wasFound() )
         {

            //
            // Compare the limb and the intercept we just computed.
            //
            centerLimb      = limb.getCenter();
            smajorLimb      = limb.getSemiMajorAxis();
            sminorLimb      = limb.getSemiMinorAxis();
 
            centerIntercept = intercept.getIntercept().getCenter();
            smajorIntercept = intercept.getIntercept().getSemiMajorAxis();
            sminorIntercept = intercept.getIntercept().getSemiMinorAxis();

            //
            // Adjust intercept semi-axes if they're not aligned with
            // those of the limb. The semi-axes are determined only up
            // to sign.
            //
            if ( smajorLimb.dot( smajorIntercept ) < 0.0 )
            {
               smajorIntercept = smajorIntercept.negate();
            }

            if ( sminorLimb.dot( sminorIntercept ) < 0.0 )
            {
               sminorIntercept = sminorIntercept.negate();
            }
 
            //
            // Check the intercept ellipse against the limb.
            //
            tol = TIGHT;

            ok  = JNITestutils.chckad( "center", 
                                       centerIntercept.toArray(), 
                                       "~~/",
                                       centerLimb.toArray(),     tol );


            ok  = JNITestutils.chckad( "smajor", 
                                       smajorIntercept.toArray(), 
                                       "~~/",
                                       smajorLimb.toArray(),     tol );


            ok  = JNITestutils.chckad( "sminor", 
                                       sminorIntercept.toArray(), 
                                       "~~/",
                                       sminorLimb.toArray(),     tol );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor: make copy of result " +
                              "from previous test case."                     );

         intercept1 = new EllipsoidPlaneIntercept( intercept );


         //
         // Compare found flags.
         //
         ok  = JNITestutils.chcksl( "found", 
                                    intercept1.wasFound(), 
                                    intercept.wasFound()   );
         // 
         // Compare ellipse members.
         //
         ok  = JNITestutils.chckad( "copy ellipse", 
                                    intercept1.getIntercept().toArray(), 
                                       "~~/",
                                    intercept.getIntercept().toArray(),  tol );
         //
         // Save members for later comparison.
         //
         found        = intercept.wasFound();
         ellipseArray = intercept.getIntercept().toArray();

         //
         // Update `intercept'.
         //
         intercept = new EllipsoidPlaneIntercept();

         //
         // Check the members of `intercept1'.
         // 

         ok  = JNITestutils.chcksl( "found (1)", 
                                    intercept1.wasFound(), 
                                    found                  );
 
         ok  = JNITestutils.chckad( "copy ellipse (1)", 
                                    intercept1.getIntercept().toArray(), 
                                       "~~/",
                                    ellipseArray,    tol );


         //
         // Constructor exception tests
         // 
         //
         // None so far.
         //
  


         //
         // Method tests
         //
      
         //
         // Normal use of the methods
         //
         //    wasFound
         //    getIntercept
         // 
         // has already been demonstrated above. 
         //
         // We do need to check the "PointNotFoundException" case.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Try to retrieve a non-existent intercept." );

         //
         // Use the ellipsoid from the principal constructor case.
         //
         // This plane is parallel to the x-y plane and lies above the 
         // ellipsoid.
         //
         plane      = new Plane ( z, 2*a );

         intercept2 = new EllipsoidPlaneIntercept ( ellipsoid, plane );

         try
         {
            ellipse = intercept2.getIntercept();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }

         //
         // We should be able to check the found flag of this intercept 
         // instance. No exception should be thrown.
         //
         found = intercept2.wasFound();

         ok    = JNITestutils.chcksl( "found", found, false );
 



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

}

