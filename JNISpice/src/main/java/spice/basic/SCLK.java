

package spice.basic;


/**
Class SCLK represents identities of spacecraft clocks.

<p> Version 1.0.0 29-SEP-2009 (NJB)
*/

public class SCLK extends Object
{

   /*
   Instance variables
   */
   private             int clockID;


   /*
   Constructors
   */

   /**
   Construct an SCLK from an integer code.
   */
   public SCLK ( int SCLKcode )
   {
      clockID = SCLKcode;
   }

   /**
   Construct an SCLK from another SCLK.
   */
   public SCLK ( SCLK ID )
   {
      clockID = ID.clockID;
   }


   /*
   Instance methods
   */
   public int getIDCode()
   {
      return clockID;
   }
}
