
package spice.testutils;

import spice.basic.SpiceException;

/**
Class Testutils provides test utilities
implemented in pure Java. These utilities
have functionality distinct from that provided
by methods of class JNITestutils.

<p> Version 1.0.0 23-SEP-2009 (NJB)
*/
public class Testutils extends Object
{
   //
   //  Utility methods
   //


   /**
   Method dogDidNotBark handles the situation where a test case
   tried to cause a specific SpiceErrorException to be thrown, but failed.

   <p> The caller passes in the short error message associated with
   the SPICE error.
   */
   public static void dogDidNotBark ( String shortMsg )

      throws SpiceException
   {
      //
      //   Declare exception to be thrown to indicate that an exception
      //   was expected but wasn't thrown.
      //
      SpiceException noException = new SpiceException (

            "dogDidNotBark : <NO EXCEPTION THROWN> : exception " + shortMsg +
            " was expected but not thrown."                   );

      throw ( noException );
   }

}
