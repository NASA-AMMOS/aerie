
package spice.basic;

/**
Class KernelVarDescriptor packages attributes of kernel variables.

<p> Version 1.0.0 03-DEC-2009 (NJB)
*/

public class KernelVarDescriptor
{

   //
   // Public constants
   //
   public static final int    CHARACTER  =  0;
   public static final int    NUMERIC    =  1;

   public static final String CSPICE_CHARACTER  = "C";
   public static final String CSPICE_NUMERIC    = "N";

   //
   // Fields
   //
   private boolean                 exists;
   private String                  name;
   private int                     size;
   private int                     dataType;

   //
   // Constructors
   //

   /**
   Zero-arguments constructor.
   */
   public KernelVarDescriptor()
   {
      exists   = false;
      name     = null;
      size     = -1;
      dataType = -1;
   }

   /**
   Copy constructor.
   */
   public KernelVarDescriptor( KernelVarDescriptor   descr )
   {
      exists   = descr.exists;
      name     = new String( descr.name );
      size     = descr.size;
      dataType = descr.dataType;
   }


   /**
   Construct a descriptor from a kernel variable's attributes.
   */
   public KernelVarDescriptor( boolean      exists,
                               String       name,
                               int          size,
                               int          dataType )
   {
      this.exists   = exists;
      this.name     = name;
      this.size     = size;
      this.dataType = dataType;
   }


   //
   // Methods
   //

   /**
   Return a boolean indicating whether a specified kernel
   variable exists (is present in the kernel pool).

   <p>User applications should call this method to determine
   whether the variable's size and data type can be fetched.
   */
   public boolean exists()
   {
      return( exists );
   }

   /**
   Return the name of the kernel variable associated with
   this instance. The variable need not exist in the kernel pool.
   */
   public String getName()
   {
      return( name );
   }

   /**
   Return the size of the kernel variable, if any, associated
   with this instance. This method throws an exception of the variable
   does not exist.
   */
   public int getSize()

      throws KernelVarNotFoundException
   {
      if ( !exists )
      {
         KernelVarNotFoundException exc = KernelVarNotFoundException.create(

            "getSize",
            "Kernel variable " + name.trim() + " does not exist."    );

         throw( exc );
      }

      return( size );
   }

   /**
   Return the data type of the kernel variable, if any, associated
   with this instance. This method throws an exception of the variable
   does not exist.
   */
   public int getDataType()

      throws KernelVarNotFoundException
   {
      if ( !exists )
      {
         KernelVarNotFoundException exc = KernelVarNotFoundException.create(

            "getDataType",
            "Kernel variable " + name.trim() + " does not exist."    );

         throw( exc );
      }

      return( dataType );
   }
}

