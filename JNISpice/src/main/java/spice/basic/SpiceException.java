
package spice.basic;

/**
The SpiceException class is used to signal errors or non-error exceptional
cases encountered within the JNISpice system.
*/

public class SpiceException extends Exception
{
   /*
   Constructors
   */

   /**
   Create a SpiceException with an associated diagnostic message.
   */
   public SpiceException ( String message )
   {
      super ( message );
   }

}
