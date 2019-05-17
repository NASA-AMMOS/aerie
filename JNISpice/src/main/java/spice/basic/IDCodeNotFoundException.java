
package spice.basic;

import spice.basic.*;

/**
The IDCodeNotFoundException class is used to signal a "not found"
condition when an attempt is made to translate a name to an ID code.
This exception is meant to be used for non-error cases which normally
would be indicated in CSPICE by setting a "found flag" to SPICEFALSE.
*/

public class IDCodeNotFoundException extends SpiceException
{
   /*
   Instance variables
   */

   /*
   Constructors
   */

   /**
   Construct an IDCodeNotFoundException using a detailed error
   message string.
   */
   public IDCodeNotFoundException ( String message )
   {
      super ( message );
   }
}
