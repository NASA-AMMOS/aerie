
package spice.basic;

import spice.basic.*;

/**
The PointNotFoundException class is used to signal a "not found"
condition when an attempt is made to access a 3-vector that
does not exist.

<p> Version 1.0.0 03-DEC-2009 (NJB)
*/

public class PointNotFoundException extends SpiceException
{
   //
   // Constants
   //
   private static final String SHORT_MSG = "SPICE(POINTNOTFOUND)";


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
   Construct a PointNotFoundException, supplying an accompanying
   message string.
   */
   public PointNotFoundException ( String message )
   {
      super ( message );
   }


   //
   // Methods
   //

   /**
   Create an exception having a compound message.
   */
   public static PointNotFoundException create ( String caller,
                                                 String longMsg  )
   {
      String compoundMsg = VERSION   + ": " + caller + ": " +
                           SHORT_MSG + ": " + longMsg;

      return( new PointNotFoundException(compoundMsg) );
   }



}
