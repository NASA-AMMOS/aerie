
package spice.basic;

/**
Enum OccultationCode declares parameters associated with 
occultation states computed by the class {@link OccultationState}.

<p>The values of this class designate states of occultation of
one target by another, as seen from a viewing location that
is external to both objects. The possible geometric states
considered by this class are:
<ul>
<li> One target completely occults the other. </li>

<li> One target is in transit the other, such that the
entire limb of the occulted target is visible from the 
viewing location. This is called an "annular occultation."</li>

<li> One target partially occults the other, such that a portion
of the limb of the occulted object is not visible from 
the viewing location. This is called a "partial occultation."</li>

<li>Neither target occults the other.</li>
</ul>


<h3> Version 1.0.0 21-DEC-2016 (NJB)</h3>
*/

public enum OccultationCode
{

   //
   // Set enum attributes.
   //

   /**
   Parameter indicating total occultation of the first target by the second.
   */
   TOTAL1( -3 ),
  
   /**
   Parameter indicating annular occultation of the first target by the second.
   */
   ANNLR1( -2 ),

   /**
   Parameter indicating partial occultation of the first target by the second.
   */
   PARTL1( -1 ),

   /**
   Parameter indicating that neither target occults the other.
   */
   NOOCC (  0 ),

   /**
   Parameter indicating partial occultation of the second target by the first.
   */
   PARTL2(  1 ),

   /**
   Parameter indicating annular occultation of the second target by the first.
   */
   ANNLR2(  2 ),

   /**
   Parameter indicating total occultation of the second target by the first.
   */
   TOTAL2(  3 );


   //
   // Fields
   //
   private final int                    code;
    

   //
   // Constructor
   // 
   OccultationCode( int code )
   {
      this.code = code;
   }

   //
   // Methods
   //

   /**
   For a given OccultationCode instance, return the corresponding 
   integer occultation code used by CSPICE and SPICELIB.
   */
   public int getOccultationCode()
   {
      return( code );
   }

   /**
   Return the OccultationCode instance corresponding to a SPICE 
   integer occultation state parameter.
   */
   public static OccultationCode mapIntCode( int  intCode )

      throws SpiceException
   {
      for ( OccultationCode occ : OccultationCode.values() )
      {
         if ( occ.code == intCode )
         {
            return( occ );
         }
      }
 
      //
      // We arrive here only if the input code is invalid.
      //

      String errmsg = String.format( "Integer code %d does not "           +
                                     "designate a recognized occultation " +
                                     "state",
                                     intCode                                );

      SpiceErrorException exc = 

         SpiceErrorException.create( "OccultationCode.mapIntCode(int)", 
                                     "SPICE(INVALIDCODE)",
                                     errmsg                            );
      throw( exc );                              
   }

}


