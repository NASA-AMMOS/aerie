
package spice.basic;

import static spice.basic.AngularUnits.*;

/**
Class EulerAngles represents Euler angle sequences.

<p> Version 1.0.0 22-DEC-2009 (NJB)
*/

public class EulerAngles extends Object
{
   //
   // Private constants
   //
   private static String[] indexNames = { "Left", "Center", "Right" };


   //
   // Private fields
   //
   double[]        angles;
   int[]           axes;




   //
   // Private utilities
   //
   private static void checkAxes( int[] axes )

      throws SpiceException

   {
      for ( int i = 0;  i < 3;  i++ )
      {
         if (  ( axes[i] < 1 )  ||  ( axes[i] > 3 )  )
         {
            SpiceException exc = SpiceErrorException.create(

               "EulerAngles",
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

            "EulerAngles",
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
   public EulerAngles( EulerAngles  eul )
   {
      angles = new double[3];
      axes   = new int[3];

      System.arraycopy( eul.angles, 0, angles, 0, 3 );
      System.arraycopy( eul.axes,   0, axes,   0, 3 );
   }


   /**
   Construct an Euler angle sequence from three angles
   and three axis indices.

   <p> Angular units are radians.

   <p> Axis numbers are in the set { 1, 2, 3 }.

   <p> The Euler angle sequence represents the rotation

   <pre>
   [leftAngle]         [centerAngle]           [rightAngle]
              leftAxis              centerAxis             rightAxis
   </pre>
   */
   public EulerAngles ( double       leftAngle,
                        double       centerAngle,
                        double       rightAngle,
                        int          leftAxis,
                        int          centerAxis,
                        int          rightAxis    )

      throws SpiceException

   {
      angles    = new double[3];
      axes      = new int[3];

      angles[0] = leftAngle;
      angles[1] = centerAngle;
      angles[2] = rightAngle;

      axes[0]   = leftAxis;
      axes[1]   = centerAxis;
      axes[2]   = rightAxis;

      checkAxes( axes );
   }



   /**
   Construct an Euler angle sequence from three angles
   having specified units and three axis indices.

   <p> The angles will be converted to radians on input.

   <p> Axis numbers are in the set { 1, 2, 3 }.

   <p> The Euler angle sequence represents the rotation

   <pre>
   [leftAngle]         [centerAngle]           [rightAngle]
              leftAxis              centerAxis             rightAxis
   </pre>
   */
   public EulerAngles ( double       leftAngle,
                        double       centerAngle,
                        double       rightAngle,
                        AngularUnits units,
                        int          leftAxis,
                        int          centerAxis,
                        int          rightAxis    )

      throws SpiceException

   {
      angles    = new double[3];
      axes      = new int[3];

      angles[0] = leftAngle   * units.toRadians();
      angles[1] = centerAngle * units.toRadians();
      angles[2] = rightAngle  * units.toRadians();

      axes[0]   = leftAxis;
      axes[1]   = centerAxis;
      axes[2]   = rightAxis;

      checkAxes( axes );
   }




   /**
   Construct an Euler angle sequence from an array of three angles
   and an array of three axis indices.

   <p> Angular units are radians.

   <p> Axis numbers are in the set { 1, 2, 3 }.

   <p> The Euler angle sequence represents the rotation

   <pre>
   [ angles[0] ]         [ angles[1] ]         [ angles[2] ]
                axes[0]               axes[1]               axes[2]
   </pre>
   */
   public EulerAngles ( double[]    angles,
                        int[]       axes    )

      throws SpiceException

   {
      this.angles = new double[3];
      this.axes   = new int[3];

      System.arraycopy( angles, 0, this.angles, 0, 3 );
      System.arraycopy( axes,   0, this.axes,   0, 3 );

      checkAxes( axes );
   }


   /**
   Construct an Euler angle sequence from a rotation matrix
   and an array of three axis indices.

   <p> Axis numbers are in the set { 1, 2, 3 }.

   <p> The Euler angle sequence represents the rotation

   <pre>
   m  =  [ angles[0] ]        [ angles[1] ]         [ angles[2] ]
                      axes[0]              axes[1]               axes[2]
   </pre>
   */
   public EulerAngles ( Matrix33    m,
                        int[]       axes    )

      throws SpiceException

   {
      this.angles = new double[3];
      this.axes   = new int[3];

      //
       // Copy the input axes.
      //
      System.arraycopy( axes, 0, this.axes, 0, 3 );

      checkAxes( this.axes );

      //
      // Let CSPICE perform the Euler angle decomposition.
      //
      angles = CSPICE.m2eul( m.toArray(), this.axes );
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
   Return the angle sequence for this instance.

   <p> Angular units are radians.
   */
   public double[] getAngles()
   {
      double[] retArray = new double[3];

      System.arraycopy( angles, 0, retArray, 0, 3 );

      return( retArray );
   }

   /**
   Return the angle sequence for this instance,
   where the angles are expressed in user-specified
   units.
   */
   public double[] getAngles( AngularUnits units )
   {
      //
      // Let s represent the factor (input unit)/radian.
      //
      double  s = 1.0 / units.toRadians();

      Vector3 v =  ( new Vector3( angles ) ).scale( s );

      return( v.toArray() );
   }



   /**
   Convert this instance to a rotation matrix.
   */
   public Matrix33 toMatrix()

      throws SpiceException
   {
      double[][] m = CSPICE.eul2m( angles, axes );

      return(  new Matrix33(m)  );
   }


   /**
   Return a string representation of the contents of this
   EulerAngles instance. This overrides Object's toString method.
   */
   public String toString()
   {
      String outStr;

      try
      {
         outStr = String.format

                   ( "[%24.16e (deg)]  [%24.16e (deg)]  [%24.16e (deg)]%n" +
                     "                                %d " +
                     "                                %d " +
                     "                                %d",
                     angles[0]*DPR,    angles[1]*DPR,    angles[2]*DPR,
                     axes[0],          axes[1],          axes[2]              );


      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr );
   }

}
