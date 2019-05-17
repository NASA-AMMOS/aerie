
package spice.basic;

import spice.basic.*;

/**
The KernelVarNotFoundException class is used to signal a "not found"
condition when an attempt is made to access a CSPICE kernel pool
variable.  This exception is meant to be used for non-error cases
which normally would be indicated in CSPICE by setting a "found flag"
to SPICEFALSE.

<p> Version 1.0.0 03-DEC-2009 (NJB)
*/

public class KernelVarNotFoundException extends SpiceException
{
   //
   // Constants
   //
   private static final String SHORT_MSG = "SPICE(KERNELVARNOTFOUND)";


   private static String VERSION;

   static
   {
      try
      {
         VERSION = CSPICE.tkvrsn( "TOOLKIT" );

      }
      catch( SpiceException exc )
      {
         VERSION = "<ERROR: Could not obtain SPICE Toolkit version>";
      }
   }



   //
   // Constructors
   //

   /**
   Construct a KernelVarNotFoundException, supplying an accompanying
   message string.
   */
   public KernelVarNotFoundException ( String message )
   {
      super ( message );
   }


   //
   // Methods
   //

   /**
   Create an exception having a compound message.
   */
   public static KernelVarNotFoundException create ( String caller,
                                                     String longMsg  )
   {
      String compoundMsg = VERSION   + ": " + caller + ": " +
                           SHORT_MSG + ": " + longMsg;

      return( new KernelVarNotFoundException(compoundMsg) );
   }



}
