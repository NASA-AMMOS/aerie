
package spice.basic;

/**
Class DistanceUnits represents distance units and
supports conversion between them.

<p> Version 1.0.0 21-DEC-2009 (NJB)
*/
public class DistanceUnits extends Units
{
   //
   // Public static fields
   //


   /**
   AU represents astronomical units.
   */
   public final static DistanceUnits AU = new DistanceUnits( "AU" );



   /**
   KM represents kilometers
   */
   public final static DistanceUnits KM = new DistanceUnits( "KM" );



   /**
   METERS represents meters.
   */
   public final static DistanceUnits METERS = new DistanceUnits( "METERS" );


   //
   // Private Fields
   //
   private final String          name;

   //
   // Private Constructors
   //
   private DistanceUnits ( String       name )
   {
      this.name       = name;
   }

   //
   // Methods
   //

   /**
   Return the name of this unit as a String.
   */
   public String toString()
   {
      return ( new String(name) );
   }

   /**
   Return the magnitude of this unit in km.
   */
   public double toKm()

      throws SpiceException
   {
      double inKm = CSPICE.convrt( 1.0, this.name, "KM" );

      return ( inKm );
   }


}

