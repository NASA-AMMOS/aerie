
package spice.basic;

import spice.basic.*;

/**
Class SurfaceName represents surface-name mappings that
are initialized by specification of a surface name and
a Body.

<p> SPICE applications normally won't need to use
this class directly; they should use the class
{@link spice.basic.Surface} instead.

<p> Version 1.0.0 26-DEC-2016 (NJB)
*/
public class SurfaceName extends SurfaceIDMap
{

   //
   // Fields
   //
   private String           surfaceName;
   private Body             body;

   //
   // Constructors
   //

   /**
   Create a new SurfaceName from a name string and a Body.
   */
   public SurfaceName ( String name,
                        Body   body  )

      throws SpiceException
   {
      surfaceName = new String(name);
      this.body   = new Body  (body);
   }

   /**
   Return the integer Surface ID code of this SurfaceCode instance.
   The code is that associated with the name at the time of the call.
   */
   public int getIDCode()

      throws SpiceException
   {
      boolean[]  foundArray = new boolean[1];
      int[]      codeArray  = new int[1];

      int bodyid = body.getIDCode();

      CSPICE.srfscc( surfaceName, bodyid, codeArray, foundArray );

      return ( codeArray[0] );
   }


   /**
   Return the surface name associated with this ID code. This method
   returns a deep copy of the object's `surfaceName' field.
   */
   public String getName()

      throws SpiceException
   {
      return (  new String( surfaceName )  );
   }


   /**
   Return the Body associated with this surface.

   This method returns a deep copy.
   */
   public Body getBody()

      throws SpiceException
   {
      return new Body( body );
   }


   /**
   Return a deep copy of this instance.
   */
   public SurfaceName deepCopy()

      throws SpiceException
   {
      return (  new SurfaceName( surfaceName, body )  );
   }


}
