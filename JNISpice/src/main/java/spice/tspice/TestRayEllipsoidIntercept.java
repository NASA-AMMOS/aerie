
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestRayEllipsoidIntercept provides methods that implement
test families for the class RayEllipsoidIntercept.

<p>Version 1.0.0 03-DEC-2009 (NJB)
*/
public class TestRayEllipsoidIntercept extends Object
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
   Test RayEllipsoidIntercept and associated classes.
   */
   public static boolean f_RayEllipsoidIntercept()

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
      Ellipsoid                         e1;

      Ray                               ray0;
      Ray                               ray1;

      RayEllipsoidIntercept             x0;
      RayEllipsoidIntercept             x1;


      Vector3                           dir;
      Vector3                           vertex;
      Vector3                           intercept;
      Vector3                           xpt;

      boolean                           found;
      boolean                           ok;



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

         JNITestutils.topen ( "f_RayEllipsoidIntercept" );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: attempt to fetch intercept in " +
                               "a non-intercept case."                   );

         try
         {

            e0             = new Ellipsoid( SQ3, SQ3, SQ3 );

            vertex = new Vector3( 5.0, 5.0, 5.0 );
            dir    = vertex;

            ray0   = new Ray( vertex, dir );


            x0     = new RayEllipsoidIntercept( ray0, e0 );

            //
            // This call should cause an exception to be thrown.
            //
            xpt    = x0.getIntercept();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(POINTNOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            //
            // For debugging:
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

         JNITestutils.tcase ( "Test intercept on sphere of radius sqrt(3)."  );

         e0             = new Ellipsoid( SQ3, SQ3, SQ3 );

         vertex = new Vector3( 5.0, 5.0, 5.0 );
         dir    = vertex.negate();

         ray0   = new Ray( vertex, dir );


         x0     = new RayEllipsoidIntercept( ray0, e0 );

         //
         // Check found flag.
         //
         found  = x0.wasFound();

         ok = JNITestutils.chcksl( "found", found, true );


         //
         // Check the intercept location.
         //
         intercept = x0.getIntercept();

         // For debugging:
         //System.out.println( "intercept: " + intercept);

         xpt       = new Vector3( 1.0, 1.0, 1.0 );


         ok = JNITestutils.chckad ( "intercept",
                                    intercept.toArray(),
                                    "~~",
                                    xpt.toArray(),
                                    TIGHT_TOL            );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test non-intercept case."  );

         e0             = new Ellipsoid( SQ3, SQ3, SQ3 );

         vertex = new Vector3( 5.0, 5.0, 5.0 );
         dir    = vertex;

         ray0   = new Ray( vertex, dir );


         x0     = new RayEllipsoidIntercept( ray0, e0 );

         //
         // Check found flag.
         //
         found  = x0.wasFound();

         ok = JNITestutils.chcksl( "found", found, false );

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

