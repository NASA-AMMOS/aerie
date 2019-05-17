
package spice.basic;

import spice.basic.*;

/**
The PointingNotFoundException class is used to indicate that a C-kernel
lookup failed to find requested pointing.  This condition is normally
non-fatal.  It is handled in CSPICE by returning a boolean "found flag"
having a value of SPICEFALSE.

<p> Version 1.0.0 13-DEC-2009 (NJB)
*/

public class PointingNotFoundException extends SpiceException
{
   //
   // Constants
   //
   private static final String SHORT_MSG = "SPICE(POINTINGNOTFOUND)";


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


   /*
   Instance variables
   */
   int                    instrument;
   double                 encodedSCLK;
   double                 tolerance;
   String                 referenceFrame;
   boolean                avRequired;

   /*
   Constructors
   */
   /**
   Construct a PointingNotFoundException, supplying an accompanying
   message string.
   */
   public PointingNotFoundException ( String message )
   {
      super ( message );
   }




   /*
   public PointingNotFoundException ( int         inst,
                                      double      sclkdp,
                                      double      tol,
                                      String      ref,
                                      boolean     needav )
   {
      instrument     = inst;
      encodedSCLK    = sclkdp;
      tolerance      = tol;
      referenceFrame = new String ( ref );
      avRequired     = needav;
   }
   */



   //
   // Methods
   //

   /**
   Create an exception having a compound message.
   */
   public static PointingNotFoundException create ( String caller,
                                                    String longMsg  )
   {
      String compoundMsg = VERSION   + ": " + caller + ": " +
                           SHORT_MSG + ": " + longMsg;

      return( new PointingNotFoundException(compoundMsg) );
   }



}
