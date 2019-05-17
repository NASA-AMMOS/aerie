
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestPhysicalConstants provides methods that implement test families for
the class PhysicalConstants.

<p>Version 1.0.0 30-DEC-2009 (NJB)
*/
public class TestPhysicalConstants extends Object
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
   Test PhysicalConstants and associated classes.
   */
   public static boolean f_PhysicalConstants()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      VTIGHT_TOL = 1.e-14;

      //
      // Local variables
      //
      boolean                           ok;

      double                            xval;




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

         JNITestutils.topen ( "f_PhysicalConstants" );







         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************

         //
         // None so far.
         //

         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check CLIGHT" );

         //
         // Value is from SPICELIB routine clight.for.
         //
         xval = CSPICE.clight();

         ok = JNITestutils.chcksd( "CLIGHT",
                                   PhysicalConstants.CLIGHT,
                                   "~",
                                   xval,
                                   VTIGHT_TOL          );


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

