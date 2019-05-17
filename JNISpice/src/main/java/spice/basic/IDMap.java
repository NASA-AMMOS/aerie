
package spice.basic;


/**
Class IDMap is an abstract superclass for JNISpice
classes implementing mappings between names and ID codes.

<h3>Version 2.0.0 26-DEC-2016</h3>

Updated to include the deepCopy method.

*/
public abstract class IDMap extends Object
{
   /**
   Return the name belonging to a name-ID pair.
   */
   public abstract String getName()

      throws SpiceException;

   /**
   Return the ID code belonging to a name-ID pair.
   */
   public abstract int getIDCode()

      throws SpiceException;

   /**
   Return a deep copy of an IDMap instance.
   */
   public abstract IDMap deepCopy()

      throws SpiceException;

}


