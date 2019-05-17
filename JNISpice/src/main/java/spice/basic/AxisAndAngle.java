
package spice.basic;

/**
Class AxisAndAngle provides containers for rotation
axis and angle combinations.

<p> Version 1.0.0 08-DEC-2009 (NJB)
*/

public class AxisAndAngle extends Object
{

   //
   // Fields
   //
   private Vector3                 axis;
   private double                  angle;


   //
   // Constructors
   //


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public AxisAndAngle( AxisAndAngle a )
   {
      this.axis  = new Vector3( a.axis );
      this.angle = a.angle;
   }


   /**
   Create a rotation axis and angle from a vector and
   scalar.

   <p> The axis must be non-zero.
   */
   public AxisAndAngle( Vector3       v,
                        double        angle )

      throws SpiceException
   {
      if ( v.isZero() )
      {
         SpiceException exc = SpiceErrorException.create(

            "AxisAndAngle",
            "SPICE(ZEROVECTOR)",
            "Input axis vector must be non-zero but was in fact zero."  );

         throw ( exc );
      }

      //
      // Store a unit-length version of the axis vector.
      //
      this.axis  = v.hat();
      this.angle = angle;
   }

   /**
   Create a rotation axis and angle from a rotation matrix.
   */
   public AxisAndAngle ( Matrix33  r )

      throws SpiceException
   {
      double[] axisArray   = new double[3];
      double[] angleArray  = new double[1];

      CSPICE.raxisa( r.toArray(), axisArray, angleArray );

      this.axis  = new Vector3( axisArray );
      this.angle = angleArray[0];
   }


   //
   // Methods
   //

   /**
   Get the rotation axis from this instance.
   */
   public Vector3 getAxis()
   {
      return(  new Vector3( axis )  );
   }

   /**
   Get the rotation angle from this instance.
   */
   public double getAngle()
   {
      return( angle );
   }

   /**
   Create a rotation matrix from this instance.
   */
   public Matrix33 toMatrix()

      throws SpiceException
   {
      double[][] rmat = CSPICE.axisar( axis.toArray(), angle );

      return(  new Matrix33( rmat )  );
   }

}



