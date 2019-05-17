
package spice.basic;

/**
Class Coordinates is an abstract superclass of all JNISpice
classes that represent coordinate systems.

JNISpice methods that accept an input argument of type
Coordinates can be passed an instance of any Coordinate
subclass.

<p> Version 1.0.0 18-MARCH-2011 (NJB)(EDW)
*/
public abstract class Coordinates extends Object
{
   //
   // Public fields
   //

   //
   // Coordinate systems
   //
   public final static String CYLINDRICAL    = "CYLINDRICAL";
   public final static String GEODETIC       = "GEODETIC";
   public final static String LATITUDINAL    = "LATITUDINAL";
   public final static String PLANETOGRAPHIC = "PLANETOGRAPHIC";
   public final static String RADEC          = "RA/DEC";
   public final static String RECTANGULAR    = "RECTANGULAR";
   public final static String SPHERICAL      = "SPHERICAL";


   //
   // Coordinates
   //
   public final static String ALTITUDE       = "ALTITUDE";
   public final static String COLATITUDE     = "COLATITUDE";
   public final static String DECLINATION    = "DECLINATION";
   public final static String LATITUDE       = "LATITUDE";
   public final static String LONGITUDE      = "LONGITUDE";
   public final static String RA             = "RIGHT ASCENSION";
   public final static String RADIUS         = "RADIUS";
   public final static String RANGE          = "RANGE";
   public final static String X              = "X";
   public final static String Y              = "Y";
   public final static String Z              = "Z";

   
   //
   // Methods
   //

   /**
   Convert this Coordinate instance to
   rectangular coordinates.
   */
   public abstract Vector3 toRectangular()

      throws SpiceException;

}
