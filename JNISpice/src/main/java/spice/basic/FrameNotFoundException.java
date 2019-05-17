
package spice.basic;

import spice.basic.*;

/**
The FrameNotFoundException class is used to signal a "not found"
condition when an attempt is made to locate data for a reference frame.
This exception is meant to be used for non-error cases
which normally would be indicated in CSPICE by setting a "found flag"
to SPICEFALSE.

<p> Version 1.0.0 09-DEC-2009 (NJB)
*/

public class FrameNotFoundException extends SpiceException
{
   //
   // Constants
   //
   private static final String SHORT_MSG = "SPICE(FRAMENOTFOUND)";


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
   //Instance variables
   //

   //
   // Constructors
   //

   /**
   Construct a FrameNotFoundException, supplying an accompanying
   message string.
   */
   public FrameNotFoundException ( String message )
   {
      super ( message );
   }



   //
   // Methods
   //


   /**
   Create an exception having a compound message.
   */
   public static FrameNotFoundException create ( String caller,
                                                 String longMsg  )
   {
      String compoundMsg = VERSION   + ": " + caller + ": " +
                           SHORT_MSG + ": " + longMsg;

      return( new FrameNotFoundException(compoundMsg) );
   }

}
