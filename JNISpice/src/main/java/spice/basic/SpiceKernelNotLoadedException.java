
package spice.basic;

/**
Class SpiceKernelNotLoadedException indicates that a specified
file is not loaded in the JNISpice kernel databasee.
*/
public class SpiceKernelNotLoadedException extends SpiceException
{

   /**
   Constructor for SpiceKernelNotLoadedException
   */
   public SpiceKernelNotLoadedException ( String fileName )
   {
      super (  "Kernel " + fileName.trim() +
               " is not loaded in the JNISpice kernel database." );


   }

}
