
package spice.basic;

import spice.basic.*;
import spice.basic.CSPICE;


/**
Class Surface is used to represent identities of 
surfaces associated with ephemeris objects.

<p> This class takes the place of integer ID codes and surface names
used by subroutine interfaces in SPICELIB and CSPICE.  However,
Surface names and codes are still used to construct Surface objects:
either a name or NAIF integer code must be supplied in order to construct
a Surface.

<p> Each Surface is associated with a Body. Surface ID-name mappings
associated with a given Body are distinct from those associated with
another Body: Bodies can be thought of as identifying name spaces for
Surface ID codes and names. A set 
consisting of a Surface ID code and a Body can be mapped to a Surface name,
and a set consisting of a Surface name and a Body can be mapped to
a Surface ID code.


<h3> Version 1.0.0 26-DEC-2016 (NJB) </h3>


*/
public class Surface extends Object
{

   /*
   Instance variables
   */
   private SurfaceIDMap      surfaceID;


   /*
   Constructors
   */

   /**
   No-arguments constructor. This constructor creates
   an uninitialized Surface instance.
   */
   public Surface()
   {
   }

   /**
   Construct a Surface from a surface name and an associated
   Body.
   */
   public Surface ( String   name,
                    Body     body )

      throws SpiceException
   {
      //
      // Reject blank or empty surface names.
      //

      if ( name.equals( "" ) )
      {
         SpiceException exc = SpiceErrorException.create(

            "Surface",
            "SPICE(EMPTYSTRING)",
            "Input surface name string was empty." );

         throw ( exc );
      }
      else if ( name.trim().equals( "" ) )
      {
         SpiceException exc = SpiceErrorException.create(

            "Surface",
            "SPICE(BLANKSTRING)",
            "Input surface name string was blank." );

         throw ( exc );
      }


      surfaceID = new SurfaceName ( name.trim(), body );
   }


   /**
   Construct a Surface from an integer code and an associated Body.
   */
   public Surface ( int   code,
                    Body  body  )

      throws SpiceException
   {
      surfaceID = new SurfaceCode( code, body );
   }


   /**
   Construct a Surface from another Surface. This constructor 
   creates a deep copy.
   */
   public Surface ( Surface s )

      throws SpiceException
   {
      this.surfaceID = s.surfaceID.deepCopy();
   }


   /*
   Instance methods
   */


   /**
   Return NAIF ID code associated with a Surface.
   */
   public int getIDCode()

      throws IDCodeNotFoundException, SpiceException
   {
      return (  surfaceID.getIDCode()  );
   }



   /**
   Return name associated with a Surface.
   */
   public String getName()

      throws SpiceException
   {
      return (  surfaceID.getName()  );
   }



   /**
   Return the body associated with this surface. This method
   returns a deep copy.
   */
   public Body getBody()

      throws SpiceException
   {
      return( new Body( surfaceID.getBody() ) );
   }

  

   /**
   Return surface name in String.  This method overrides Object's
   toString() method.

   Note that this method can't throw a SpiceException.
   */
   public String toString()
   {
      String name;

      try
      {
         name = new String (  this.getName() );
      }
      catch ( SpiceException se )
      {
         //
         // Return the exception's message as the name.
         //
         name = se.getMessage();
      }

      return ( name );
   }


   /**
   Test two Surfaces for equality.

   <p> The integer codes of the surfaces and associated bodies
   are used for the comparison.
   */
   public boolean equals ( Object obj )
   {
       if (  obj == null )
       {
          return false;
       }

       if (  !( obj instanceof Surface )  )
       {
          return false;
       }

       //
       // Since this method overrides a method that doesn't
       // throw a SpiceException, this method can't throw
       // a SpiceException either. We'll have to catch that
       // exception if it occurs.
       //
       try
       {
          boolean IDMatch   =  ( (Surface)obj ).getIDCode() == this.getIDCode();

          boolean BodyMatch =     ( (Surface)obj ).getBody().getIDCode() 
                               ==             this.getBody().getIDCode();


          return (  (IDMatch && BodyMatch)  );

       }
       catch ( SpiceException e )
       {
          return false;
       }
   }


   /**
   Return a hash code for this Surface. This method overrides Object's
   hashcode() method.

   <p> Hash codes are not necessarily distinct for distinct surfaces.

   <p> Note that this method can't throw a SpiceException.
   */
   public int hashCode()
   {
      /*
      The hashcode value is the hash code of the sum of
      one half of each of the surface and body IDs. 
      Each of these codes is divided by 2 in the hash code 
      computation to prevent overflow. 

      Note that the division is of integer type so the result
      is not necessarily the average of the two codes.
      */
      try
      {
         int ival =   (this.getIDCode()           / 2 )
                    + (this.getBody().getIDCode() / 2 );
  
         Integer IntVal = new Integer( ival );

         return (  IntVal.hashCode() );
      }
      catch ( SpiceException e )
      {
         return ( 0 );
      }
   }


}

