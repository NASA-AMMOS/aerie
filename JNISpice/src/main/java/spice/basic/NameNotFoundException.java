
package spice.basic;

import spice.basic.*;

/**
The NameNotFoundException class is used to signal a "not found"
condition when an attempt is made to translate an ID code to a name.
This exception is meant to be used for non-error cases which normally
would be indicated in CSPICE by setting a "found flag" to SPICEFALSE.
*/

public class NameNotFoundException extends SpiceException
{
   /*
   Instance variables
   */

   /*
   Constructors
   */

   /**
   Construct a NameNotFoundException, supplying an accompanying
   message string.
   */
   public NameNotFoundException ( String message )
   {
      super ( message );
   }
}
