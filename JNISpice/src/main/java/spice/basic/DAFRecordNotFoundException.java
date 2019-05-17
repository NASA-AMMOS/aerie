
package spice.basic;


/**
The DAFRecordNotFoundException class is used to signal a "not found"
condition when an attempt is made to access a DAF record. This
exception is meant to be used for non-error cases which normally would
be indicated in CSPICE by setting a "found flag" to SPICEFALSE.
*/

public class DAFRecordNotFoundException extends SpiceException
{
   /*
   Instance variables
   */

   /*
   Constructors
   */

   /**
   Create a new DAFRecordNotFoundException using a single
   detailed error message String.
   */
   public DAFRecordNotFoundException ( String message )
   {
      super ( message );
   }
}
