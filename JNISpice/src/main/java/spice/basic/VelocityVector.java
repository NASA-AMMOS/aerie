
package spice.basic;

import spice.basic.CSPICE;

/**
Class VelocityVector represents velocities
of ephemeris objects relative to other objects. VelocityVectors
implicitly carry units of kilometers/second.

<p> Version 1.0.0 22-DEC-2009 (NJB)
*/

public class VelocityVector extends Vector3
{


   /*
   Constructors
   */


   /**
   Default constructor: create a zero-filled velocity vector.
   */
   public VelocityVector()
   {
      super();
   }


   /**
   Copy constructor:  create a deep copy of another VelocityVector.
   */
   public VelocityVector ( VelocityVector velocity )
   {
      super( velocity.toArray() );
   }


   /**
   Construct a VelocityVector from a Vector3 instance.
   */
   public VelocityVector ( Vector3 v )
   {
      super( v );
   }


   /**
   Construct aberration-corrected VelocityVector from ephemeris data.
   */
   public VelocityVector ( Body                   target,
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

      double[] state = new double[6];

      CSPICE.spkezr ( targ, et, frame, corr, obs, state, lt );

      double[] v     = new double[3];

      System.arraycopy ( state, 3, v, 0, 3 );

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
            "Velocity vector = "                        + "%n" +
            "%n" +
            "    VX: " + "%24.16e"        + " (km/s)"   + "%n" +
            "    VY: " + "%24.16e"        + " (km/s)"   + "%n" +
            "    VZ: " + "%24.16e"        + " (km/s)"   + "%n" +
            "%n" +

            "Speed = " + "%24.16e"  + " (km/s)"   + "%n",


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
