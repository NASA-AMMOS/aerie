
package spice.basic;

/**
Class AngularUnits represents angular units and
supports conversion between them.

<p> Version 1.0.0 18-DEC-2009 (NJB)
*/
public class AngularUnits extends Units
{
   //
   // Initialization
   //
   static
   {
      DPR = CSPICE.dpr();
      RPD = CSPICE.rpd();
   }

   //
   // Public fields
   //

   /**
   DPR represents degrees per radian.
   */
   public final static double DPR;

   /**
   RPD represents radians per degree.
   */
   public final static double RPD;


   /**
   ARCSECONDS represents 1/3600 of a degree.
   */
   public final static AngularUnits

      ARCSECONDS = new AngularUnits ( "ARCSECONDS", RPD/3600.0 );

   /**
   RADIANS represents the unit radians.
   */
   public final static AngularUnits

      RADIANS = new AngularUnits ( "RADIANS", 1.0 );


   /**
   DEGREES represents the unit degrees.
   */
   public final static AngularUnits

      DEGREES = new AngularUnits ( "DEGREES", RPD );


   //
   // Private Fields
   //
   private final String          name;
   private final double          radiansPerUnit;

   //
   // Private Constructors
   //
   private AngularUnits ( String       name,
                          double       radiansPerUnit )
   {
      this.name           = name;
      this.radiansPerUnit = radiansPerUnit;
   }

   //
   // Public constructors
   //

   /**
   Construct an angular unit from a string.

   <p> The unit name must be one of the following:
   <pre>
      "RADIANS"
      "DEGREES"
      "ARCMINUTES"
      "ARCSECONDS"
      "HOURANGLE"
      "MINUTEANGLE"
      "SECONDANGLE"
   </pre>

   */
   public AngularUnits( String name )

      throws SpiceException
   {
      this.name      = name.toUpperCase().trim();

      radiansPerUnit = CSPICE.convrt( 1.0, this.name, "RADIANS" );
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
   Return the magnitude of this unit in radians.
   */
   public double toRadians()
   {
      return ( this.radiansPerUnit );
   }


}

