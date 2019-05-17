
package spice.basic;

/**
Class BodyCode represents body-name mappings that
are initialized by specification of a body ID code.

<p> SPICE applications normally won't need to use
this class directly; they should use the class
{@link spice.basic.Body} instead.

<h3> Version 2.0.0 26-DEC-2016 (NJB)</h3>

Added deepCopy method.

<h3> Version 1.0.0 24-AUG-2009 (NJB)</h3>
*/
public class BodyCode extends IDMap
{

   //
   // Fields
   //
   private int            IDcode;


   //
   // Constructors
   //

   /**
   Create a new BodyCode from an int ID code.
   */
   public BodyCode ( int code )
   {
      IDcode = code;
   }

   /**
   Return the integer Body ID code of this BodyCode instance.
   */
   public int getIDCode()
   {
      return ( IDcode );
   }


   /**
   Return the body name associated with this ID code. The name
   is that associated with the ID code at the time of the call.
   */
   public String getName()

      throws SpiceException
   {
      String name = CSPICE.bodc2s ( IDcode );

      return ( name );
   }

  
   /**
   Return a deep copy of this instance.
   */
   public BodyCode deepCopy()

      throws SpiceException
   {
      return ( new BodyCode(IDcode) );
   }

   

}
