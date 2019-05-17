
package spice.basic;


/**
The SpiceErrorException class is used to signal errors
encountered within the JNISpice system.


<p> Version 1.0.0 11-NOV-2009 (NJB)
*/

public class SpiceErrorException extends SpiceException
{
   //
   // Constants
   //
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
   // Instance variables
   //
   private String                 version;
   private String                 caller;
   private String                 longMessage;
   private String                 shortMessage;
   private String                 traceback;

   //
   // Constructors
   //

   /**
   Create a SpiceErrorException using an already-formed
   compound message string.
   */
   public SpiceErrorException ( String message )
   {
      super ( message );
   }

   //
   // Methods
   //

   /**
   Create a SpiceErrorException using a caller name, short message,
   and long message.
   */
   public static SpiceErrorException create ( String caller,
                                              String shortMsg,
                                              String longMsg  )
   {
      String compoundMsg = VERSION  + ": " + caller + ": " +
                           shortMsg + ": " + longMsg;

      return( new SpiceErrorException(compoundMsg) );
   }




   /*
   public SpiceErrorException  ( String        lmsg,
                                 String        smsg,
                                 String        trace )
   {
      longMessage  = new String ( lmsg  );
      shortMessage = new String ( smsg  );
      traceback    = new String ( trace );
   }
   */
}
