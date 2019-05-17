
package spice.basic;

import spice.basic.*;

/**
Class SurfaceCode represents surface-name mappings that
are initialized by specification of a surface ID code
and a Body.

<p> SPICE applications normally won't need to use
this class directly; they should use the class
{@link spice.basic.Surface} instead.


<p> Version 1.0.0 26-DEC-2016 (NJB)
*/
public class SurfaceCode extends SurfaceIDMap
{

   //
   // Fields
   //
   private int            IDcode;
   private Body           body;

   //
   // Constructors
   //

   /**
   Create a new SurfaceCode from an int surface ID code
   and a Body.
   */
   public SurfaceCode ( int  code, 
                        Body body )

      throws SpiceException
   {
      IDcode    = code;
      this.body = new Body( body );
   }

   /**
   Return the integer Surface ID code of this SurfaceCode instance.
   */
   public int getIDCode()
   {
      return ( IDcode );
   }


   /**
   Return the surface name associated with this ID code. The name
   is that associated with the ID code at the time of the call.
   */
   public String getName()

      throws SpiceException
   {
      boolean[]   isName      = new boolean[1];
      String[]    srfStrArray = new String[1];

      int bodyid  = body.getIDCode();

      CSPICE.srfc2s ( IDcode, bodyid, srfStrArray, isName );
    
      return ( srfStrArray[0] );
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
   public SurfaceCode deepCopy()

      throws SpiceException
   {
      return (  new SurfaceCode( this.IDcode, this.body )  );
   }
     
}
