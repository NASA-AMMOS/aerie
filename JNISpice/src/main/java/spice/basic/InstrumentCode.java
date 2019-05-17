
package spice.basic;

/**
Class InstrumentCode represents instrument-name mappings that
are initialized by specification of a instrument ID code.

<p> SPICE applications normally won't need to use
this class directly; they should use the class
{@link spice.basic.Instrument} instead.

<h3> Version 2.0.0 26-DEC-2016 (NJB)</h3>

Added deepCopy method.

<h3> Version 1.0.0 25-AUG-2009 (NJB)</h3>
*/
public class InstrumentCode extends IDMap
{

   //
   // Fields
   //
   private int            IDcode;


   //
   // Constructors
   //

   /**
   Create a new InstrumentCode from an int ID code.
   */
   public InstrumentCode ( int code )
   {
      IDcode = code;
   }

   /**
   Return the integer Instrument ID code of this InstrumentCode instance.
   */
   public int getIDCode()
   {
      return ( IDcode );
   }


   /**
   Return the instrument name associated with this ID code. The name
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
   public InstrumentCode deepCopy()

      throws SpiceException
   {
      return (  new InstrumentCode(this.IDcode)  );
   }

}
