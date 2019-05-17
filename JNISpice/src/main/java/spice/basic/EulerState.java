
package spice.basic;

import static spice.basic.AngularUnits.*;

/**
Class EulerState represents sequences of Euler angles and
their corresponding rates of change.

<p> Version 1.0.0 22-DEC-2009 (NJB)
*/

public class EulerState extends Object
{
   //
   // Private constants
   //
   private static String[] indexNames = { "Left", "Center", "Right" };


   //
   // Private fields
   //
   double[]        state;
   int[]           axes;




   //
   // Private utilities
   //
   private static void checkAxes( int[] axes )

      throws SpiceException
   {
      if ( axes.length != 3 )
      {
         SpiceException exc = SpiceErrorException.create(

            "EulerState",
            "SPICE(INVALIDARRAYSIZE)",
            "Axis array length must be 3 but was " + axes.length  );

         throw ( exc );
      }


      for ( int i = 0;  i < 3;  i++ )
      {
         if (  ( axes[i] < 1 )  ||  ( axes[i] > 3 )  )
         {
            SpiceException exc = SpiceErrorException.create(

               "EulerState",
               "SPICE(INDEXOUTOFRANGE)",
               indexNames[i] + " axis index " + axes[i] +
                         " is outside of the range 1:3."     );

            throw ( exc );
         }
      }


      if (     ( axes[1] == axes[0] )
           ||  ( axes[1] == axes[2] )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "EulerState",
            "SPICE(BADAXISNUMBERS)",
            "Axis indices are: " +
            axes[0] + ", " +
            axes[1] + ", " +
            axes[2] + "."  +
            "The center index must be distinct from " +
            "the left and right indices."               );

         throw( exc );
      }
   }


   //
   // Constructors
   //


   /**
   Copy constructor.
   */
   public EulerState( EulerState  eul )
   {
      state = new double[6];
      axes  = new int[3];

      System.arraycopy( eul.state, 0, state, 0, 6 );
      System.arraycopy( eul.axes,  0, axes,  0, 3 );
   }


   /**
   Construct an Euler angle state from three angles,
   three angular rates, and three axis indices.

   <p> Angular units are radians.

   <p> Axis numbers are in the set { 1, 2, 3 }.

   <p> The Euler angle sequence represents the rotation

   <pre>
   [leftAngle]         [centerAngle]           [rightAngle]
              leftAxis              centerAxis             rightAxis
   </pre>
   */
   public EulerState ( double       leftAngle,
                       double       centerAngle,
                       double       rightAngle,
                       double       leftRate,
                       double       centerRate,
                       double       rightRate,
                       int          leftAxis,
                       int          centerAxis,
                       int          rightAxis    )

      throws SpiceException

   {
      //
      // Insert the input axes into a local array;
      // then use this method's axis checking utility.
      //
      axes  = new int[3];

      axes[0]  = leftAxis;
      axes[1]  = centerAxis;
      axes[2]  = rightAxis;

      checkAxes( axes );

      state = new double[6];

      state[0] = leftAngle;
      state[1] = centerAngle;
      state[2] = rightAngle;
      state[3] = leftRate;
      state[4] = centerRate;
      state[5] = rightRate;
   }



   /**
   Construct an Euler angle state from an angular
   state---an array of angles and angular rates---
   and an array of three axis indices.

   <p> Angular units are radians.

   <p> Time units are unspecified.

   <p> Axis numbers are in the set { 1, 2, 3 }.

   <p> The Euler angle sequence represents the rotation

   <pre>
   [ angles[0] ]         [ angles[1] ]         [ angles[2] ]
                axes[0]               axes[1]               axes[2]
   </pre>
   */
   public EulerState ( double[]   angularState,
                       int[]      axes          )

      throws SpiceException

   {
      checkAxes( axes );

      if ( angularState.length != 6 )
      {
         SpiceException exc = SpiceErrorException.create(

            "EulerState",
            "SPICE(INVALIDARRAYSIZE)",
            "Angular state array length must be 6 but was " +
            angularState.length                               );

         throw ( exc );
      }



      this.state = new double[6];
      this.axes  = new int[3];

      System.arraycopy( angularState, 0, this.state, 0, 6 );
      System.arraycopy( axes,         0, this.axes,  0, 3 );
   }


   /**
   Construct an Euler angle state from a state transformation matrix
   and an array of three axis indices.

   <p> Axis numbers are in the set { 1, 2, 3 }.

   <p> The Euler angle sequence represents the rotation

   <pre>
   m  =  [ angles[0] ]        [ angles[1] ]        [ angles[2] ]
                      axes[0]              axes[1]              axes[2]
   </pre>
   */
   public EulerState ( Matrix66    xform,
                       int[]       axes    )

      throws SpiceException

   {
      checkAxes( axes );


      boolean[] uniqueArray = new boolean[1];

      this.state = new double[6];
      this.axes  = new int[3];

      //
      // Copy the input axes.
      //
      System.arraycopy( axes, 0, this.axes, 0, 3 );

      //
      // Let CSPICE perform the Euler state decomposition.
      //
      CSPICE.xf2eul( xform.toArray1D(), this.axes,
                     this.state,        uniqueArray );
   }


   //
   // Methods
   //

   /**
   Return the axis index sequence for this instance.
   */
   public int[] getAxes()
   {
      int[] retArray = new int[3];

      System.arraycopy( axes, 0, retArray, 0, 3 );

      return( retArray );
   }


   /**
   Return the Euler angle sequence for this instance. Units
   are radians.
   */
   public double[] getAngles()
   {
      double[] retArray = new double[3];

      System.arraycopy( state, 0, retArray, 0, 3 );

      return( retArray );
   }



   /**
   Return the Euler angle rate sequence for this instance. Angular units
   are radians. Time units are unspecified.
   */
   public double[] getRates()
   {
      double[] retArray = new double[3];

      System.arraycopy( state, 3, retArray, 0, 3 );

      return( retArray );
   }




   /**
   Return the angular state for this instance
   in a one-dimensional array of length six.
   The first three elements of the array are
   Euler angles; the last three elements are
   the corresponding rates of change.

   <p> Angular units are radians. Time units
   are unspecified.
   */
   public double[] toArray()
   {
      double[] retArray = new double[6];

      System.arraycopy( state, 0, retArray, 0, 6 );

      return( retArray );
   }


   /**
   Convert this instance to a state transformation matrix.
   */
   public Matrix66 toMatrix()

      throws SpiceException
   {
      double[] xformArray = CSPICE.eul2xf( state, axes );

      return(  new Matrix66(xformArray)  );
   }


   /**
   Return a string representation of the contents of this
   EulerState instance. This overrides Object's toString method.
   */
   public String toString()
   {
      String outStr;

      try
      {
         outStr = String.format(

            "[%24.16e (deg)]  [%24.16e (deg)]  [%24.16e (deg)]%n" +
            "                                %d " +
            "                                %d " +
            "                                %d%n"  +
            " %24.16e          %24.16e          %24.16e%n",
            state[0]*DPR,     state[1]*DPR,     state[2]*DPR,
            axes[0],          axes[1],          axes[2],
            state[3]*DPR,     state[4]*DPR,     state[5]*DPR         );
      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr );
   }

}
