
package spice.basic;

import spice.basic.CSPICE;

/**
Class LocalSolarTime supports local solar time
computations.

<h3> Particulars</h3>

   <p>
   This class computes the local solar time at a user
   specified location on a user specified body.

   <p>
   Let SUNLNG be the planetocentric longitude (in degrees) of
   the sun as viewed from the center of the body of interest.

   <p>
   Let SITLNG be the planetocentric longitude (in degrees) of
   the site for which local time is desired.

   <p>
   We define local time to be
   <pre>
   12 + (SITLNG - SUNLNG)/15
   </pre>
   (where appropriate care is taken to map ( SITLNG - SUNLNG )
   into the range from -180 to 180).

   <p>
   Using this definition, we see that from the point of view
   of this class, local solar time is simply a measure of angles
   between meridians on the surface of a body.  Consequently,
   this class is not appropriate for computing "local times"
   in the sense of Pacific Standard Time. For computing times
   relative to standard time zones on earth, see the class
   {@link spice.basic.TDBTime}.

   <p>
   <h4>Regarding planetographic longitude</h4>

   <p>
   In the planetographic coordinate system, longitude is defined using
   the spin sense of the body.  Longitude is positive to the west if
   the spin is prograde and positive to the east if the spin is
   retrograde.  The spin sense is given by the sign of the first degree
   term of the time-dependent polynomial for the body's prime meridian
   Euler angle "W":  the spin is retrograde if this term is negative
   and prograde otherwise.  For the sun, planets, most natural
   satellites, and selected asteroids, the polynomial expression for W
   may be found in a SPICE PCK kernel.

   <p>
   The earth, moon, and sun are exceptions: planetographic longitude
   is measured positive east for these bodies.

   <p>
   If you wish to override the default sense of positive planetographic
   longitude for a particular body, you can do so by defining the
   kernel variable
   <pre>

      BODY<body ID>_PGR_POSITIVE_LON
   </pre>
   <p>
   where <body ID> represents the NAIF ID code of the body. This
   variable may be assigned either of the values
   <pre>
      'WEST'
      'EAST'
   </pre>
   <p>
   For example, you can have this routine treat the longitude of the
   earth as increasing to the west using the kernel variable assignment
   <pre>
      BODY399_PGR_POSITIVE_LON = 'WEST'
   </pre>
   <p>
   Normally such assignments are made by placing them in a text kernel
   and loading that kernel via {@link spice.basic.KernelDatabase#load}.





<p> Version 1.0.0 18-DEC-2009 (NJB)
*/
public class LocalSolarTime extends Object
{
   //
   // Public constants
   //

   //
   // Longitude types
   //
   public final static String PLANETOCENTRIC  =  "PLANETOCENTRIC";
   public final static String PLANETOGRAPHIC  =  "PLANETOGRAPHIC";

   //
   // Fields
   //

   int             hour;
   int             minute;
   int             second;
   String          localSolarTime;
   String          ampm;


   //
   // Methods
   //

   /**
   Create a local solar time instance.

   <p> Longitude has units of radians.
   Longitude type must be either of
   <pre>
       PLANETOCENTRIC
       PLANETOGRAPHIC
   </pre>

   */
   public LocalSolarTime ( Time              time,
                           Body              body,
                           double            longitude,
                           String            longitudeType )
      throws SpiceException
   {
      //
      // Declare local arrays to capture JNI outputs.
      //
      int[]       hrArray   = new int[1];
      int[]       mnArray   = new int[1];
      int[]       scArray   = new int[1];
      String[]    timeArray = new String[1];
      String[]    ampmArray = new String[1];

      //
      // Call the JNI routine:
      //
      CSPICE.et2lst (  time.getTDBSeconds(),
                       body.getIDCode(),
                       longitude,
                       longitudeType,
                       hrArray,
                       mnArray,
                       scArray,
                       timeArray,
                       ampmArray           );

      //
      // Transfer the outputs to the record's fields.
      //
      hour           = hrArray[0];
      minute         = mnArray[0];
      second         = scArray[0];
      localSolarTime = timeArray[0];
      ampm           = ampmArray[0];
   }


   /**
   Return the local solar time as a string on 24 hour clock.
   */
   public String getLocalSolarTime24Hr()
   {
      return (  new String( localSolarTime )  );
   }

   /**
   Return the local solar time as a string on a 12 hour clock.
   */
   public String getLocalSolarTime12Hr()
   {
      return (  new String( ampm )  );
   }

   /**
   Return the hour component of the local solar time.
   */
   public int getHour()
   {
      return ( hour );
   }

   /**
   Return the minute component of the local solar time.
   */
   public int getMinute()
   {
      return ( minute );
   }

   /**
   Return the second component of the local solar time.
   */
   public int getSecond()
   {
      return ( second );
   }


}
