
package spice.basic;

/**
Class KernelVarStringComponent represents components of
continued string values associated with kernel variables.

<p> This class is used by JNISpice to package components
of continued kernel pool string variables and corresponding
"found" flags. User applications normally will not need to
construct instances of this class.

<p> This class supports fetching string components using
a "found flag," as opposed to using the exception handling
system to respond to an attempt to fetch a non-existent
component.

<p> Version 1.0.0 08-JAN-2010 (NJB)
*/

public class KernelVarStringComponent extends Object
{

   //
   // Fields
   //
   private String                       kervarName;
   private int                          index;
   private String                       value;
   private boolean                      found;


   //
   // Constructors
   //

   /**
   Create a KernelVarStringComponent from a string and a
   boolean value indicating whether the requested component
   was found.
   */
   public KernelVarStringComponent ( String      kervarName,
                                     int         index,
                                     String      componentValue,
                                     boolean     found          )
   {
      this.kervarName = kervarName.trim();
      this.index      = index;
      this.value      = componentValue;
      this.found      = found;
   }

   //
   // Methods
   //


   /**
   Indicate whether the component associated with this
   instance exists. Normally user applications should
   call this method to verify the existence of a kernel
   variable string component before attempting to retrieve
   the component.
   */
   public boolean wasFound()
   {
      return( found );
   }


   /**
   Return the String component from this instance. This method
   should only be called after confirming the existence of
   the component.
   */
   public String getComponent()

      throws SpiceException
   {
      if ( !found )
      {
         SpiceException exc = SpiceErrorException.create(

            "KernelVarStringComponent",
            "SPICE(NOCOMPONENT)",
            "Kernel variable " + kervarName + " does not have " +
            "a component at index " + index                       );

         throw ( exc );
      }

      return ( value );
   }


   /**
   Return the name of the kernel variable to which this component belongs.
   */
   public String getKerVarName()
   {
      return( kervarName );
   }


   /**
   Return the index at which this component resides, if the component
   exists. This index is that supplied by the caller of the constructor
   that created this instance; it doesn't necessarily corresponding to
   an extant kernel variable component.
   */
   public int getIndex()
   {
      return( index );
   }
}




