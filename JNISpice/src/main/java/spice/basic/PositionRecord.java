
package spice.basic;

import spice.basic.CSPICE;

/**
Class PositionRecord represents the positions of ephemeris objects relative
to other objects; position records carry along with them one-way light
time.

<p> Version 1.0.0 22-DEC-2009 (NJB)
*/

public class PositionRecord extends PositionVector
{

   //
   // Instance variables
   //
   TDBDuration                lightTime;

   //
   // Constructors
   //

   /**
   No-arguments constructor.
   */
   public PositionRecord()
   {
      super();

      lightTime = new TDBDuration( 0.0 );
   }


   /**
   Copy constructor.

   <p> This constructor creates a deep copy.
   */
   public PositionRecord ( PositionRecord position )
   {
      super( position );

      lightTime = position.getLightTime();
   }


   /**
   Assignment constructor.

   <p> This constructor allows a caller to create a PositionRecord
   containing specified values.
   */
   public PositionRecord ( Vector3 v,  TDBDuration d )
   {
      super( v );

      lightTime = new TDBDuration( d );
   }



   /**
   Construct aberration-corrected PositionRecord from ephemeris data.
   */
   public PositionRecord ( Body                   target,
                           Time                   t,
                           ReferenceFrame         ref,
                           AberrationCorrection   abcorr,
                           Body                   observer )

      throws SpiceException
   {
      super();

      String   targ     = target.getName();
      double   et       = t.getTDBSeconds();
      String   frame    = ref.getName();
      String   corr     = abcorr.getName();
      String   obs      = observer.getName();
      double[] v        = new double[3];
      double[] lt       = new double[1];

      CSPICE.spkpos ( targ, et, frame, corr, obs, v, lt );

      super.assign( v );

      this.lightTime = new TDBDuration( lt[0] );
   }

   /*
   Instance methods
   */


   /**
   Create a String representation of this PositionRecord.
   */
   public String toString()
   {
      String outStr;


      double[] v = this.toArray();

      try
      {
         outStr = String.format(

            "%n" +
            "Position vector = "                             + "%n" +
            "%n" +
            "    X: " + "%24.16e"        + " (km)" + "%n" +
            "    Y: " + "%24.16e"        + " (km)" + "%n" +
            "    Z: " + "%24.16e"        + " (km)" + "%n" +
            "%n" +
            "Distance           = "  + "%24.16e" + " (km)" + "%n" +
            "One way light time = "  + "%24.16e" + " (s)"  + "%n",

            v[0], v[1], v[2], this.norm(), lightTime.getMeasure()

         );
      }
      catch ( SpiceException exc )
      {
         outStr = exc.getMessage();
      }


      return ( outStr );
   }



   /**
   Get the position vector.
   */
   public PositionVector getPositionVector()

      throws SpiceErrorException
   {
      return ( new PositionVector ( this )  );
   }

   /**
   Get one way light time between target and observer.
   */
   public TDBDuration getLightTime()
   {
      return (  new TDBDuration ( lightTime )  );
   }

}
