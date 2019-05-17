
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestTimeSystem provides methods that implement test families for
the enum TimeSystem.

<p>Version 1.0.0 14-DEC-2009 (NJB)
*/
public class TestTimeSystem extends Object
{


   /**
   Test TimeSystem and associated classes.
   */
   public static boolean f_TimeSystem()

      throws SpiceException
   {
      //
      // Constants
      //

      //
      // Local variables
      //

      String[]                          timeSysNames = {

         "JED", "SCLK", "TAI", "TDB", "TDT", "TT", "UTC" };

      TimeSystem                        elt;
      TimeSystem[]                      timeSysValues;

      boolean                           ok;

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

         JNITestutils.topen ( "f_TimeSystem" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Get the array returned by values(); compare " +
                              "to the values returned by valueOf()."         );


         n             = timeSysNames.length;
         timeSysValues = TimeSystem.values();

         //
         // Make sure we have the expected number of values.
         //
         ok = JNITestutils.chcksi( "n", n, "=", timeSysValues.length, 0 );


         //
         // Make sure each name maps to the expected value.
         //
         for ( i = 0;  i < n;  i++ )
         {
            elt = TimeSystem.valueOf( timeSysNames[i] );

            ok  = JNITestutils.chcksl( timeSysNames[i],
                                       elt.equals( timeSysValues[i] ),
                                       true                            );
         }



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

