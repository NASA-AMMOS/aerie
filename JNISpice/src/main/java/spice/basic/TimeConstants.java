
package spice.basic;

/**
Class TimeConstants provides commonly used
dates and scale factors relating to time conversion.

<p> Version 1.0.0 04-SEP-2009 (NJB)
*/

public class TimeConstants extends Object
{

   //
   // Public fields
   //

   /**
   B1950 is the Julian date corresponding to Besselian Date 1950.0.
   */
   public static final double B1950 = CSPICE.b1950();


   /**
   J1950 is the Julian date of 1950 January 1.0.
   */
   public static final double J1950 = CSPICE.j1950();


   /**
   J2000 is the Julian date of 2000 January 1.5.
   */
   public static final double J2000 = CSPICE.j2000();


   /**
   JYEAR is the count of seconds in a Julian year.
   */
   public static final double JYEAR = CSPICE.jyear();


   /**
   SPD is the count of seconds per Julian day.
   */
   public static final double SPD = CSPICE.spd();





}


