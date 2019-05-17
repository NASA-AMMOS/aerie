
package spice.basic;

import spice.basic.CSPICE;

/**
Class PositionVector represents positions
of ephemeris objects relative to other objects. PositionVectors
implicitly carry units of kilometers.

<p> Version 1.0.0 22-DEC-2009 (NJB)
*/

public class PositionVector extends Vector3
{


   /*
   Constructors
   */


   /**
   Default constructor: create a zero-filled position vector.
   */
   public PositionVector()
   {
      super();
   }


   /**
   Copy constructor:  create a deep copy of another PositionVector.
   */
   public PositionVector ( PositionVector position )
   {
      super( position.toArray() );
   }


   /**
   Construct a PositionVector from a Vector3 instance.
   */
   public PositionVector ( Vector3 v )
   {
      super( v );
   }


   /**
   Construct aberration-corrected PositionVector from ephemeris data.
   */
   public PositionVector ( Body                   target,
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
      double[] lt       = new double[1];

      double[] v = new double[3];

      CSPICE.spkpos ( targ, et, frame, corr, obs, v, lt );

      super.assign( v );
   }



   //
   // Instance methods
   //


   /**
   Create a String representation of this PositionVector.
   */
   public String toString()
   {
      String outStr;

      double[] v = this.toArray();


      try
      {
         outStr = String.format (

            "%n" +
            "Position vector = "                     + "%n" +
            "%n" +
            "    X: " + "%24.16e"        + " (km)"   + "%n" +
            "    Y: " + "%24.16e"        + " (km)"   + "%n" +
            "    Z: " + "%24.16e"        + " (km)"   + "%n" +
            "%n" +

            "Distance = " + "%24.16e"  + " (km)"   + "%n",


            v[0], v[1], v[2],
            this.norm()

         );
      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }


      return ( outStr );
   }

}
