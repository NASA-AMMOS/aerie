
package spice.basic;


/**
Class Surface IDMap is an abstract superclass for JNISpice
classes implementing mappings between surface names and ID codes,
where the mappings are associated with specified Bodies.

<h3>Version 1.0.0 26-DEC-2016 (NJB)</h3>
*/
public abstract class SurfaceIDMap extends IDMap
{
   /**
   Return the Body associated with a SurfaceIDMap.
   */
   public abstract Body getBody()

      throws SpiceException;

   /**
   Override class IDMap's deepCopy method: create a copy
   of class SurfaceIDMap.
   */
   public abstract SurfaceIDMap deepCopy()

      throws SpiceException;
}


