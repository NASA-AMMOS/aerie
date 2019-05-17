
package spice.basic;

/**
Class RotationAndAV packages combinations of rotation matrices
and angular velocity vectors that correspond to state
transformation matrices.

<p> Version 1.0.0 15-DEC-2009 (NJB)
*/
public class RotationAndAV extends Object
{
   //
   // Public constants
   //

   /**
   NORM_TOL is the maximum allowed deviation from unit length
   of any column of an input rotation matrix.
   */
   public static final double      NORM_TOL = 1.e-12;

   /**
   DET_TOL is the maximum allowed deviation from 1
   of the determinant of an input rotation matrix.
   */
   public static final double      DET_TOL  = 1.e-12;

   //
   // Fields
   //
   private Matrix33                rotation;
   private Vector3                 angvel;

   //
   // Constructors
   //


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public RotationAndAV( RotationAndAV  r )
   {
      this.rotation = new Matrix33( r.rotation );
      this.angvel   = new Vector3 ( r.angvel   );
   }

   /**
   Create a rotation and angular velocity from a rotation
   matrix and an angular velocity vector. The vector
   has units of radians/sec.
   */
   public RotationAndAV( Matrix33  r,
                         Vector3   av )

      throws SpiceException
   {
      //
      // Make sure the input matrix is a rotation.
      //
      if (  ! r.isRotation( NORM_TOL, DET_TOL )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "RotationAndAV",
            "SPICE(NOTAROTATION)",
            "Input matrix either has column magnitudes too far from 1 or " +
            "determinant too far from 1. Matrix is " + r                     );

         throw ( exc );
      }

      rotation = new Matrix33( r  );
      angvel   = new Vector3 ( av );
   }


   /**
   Create a rotation and angular velocity from a state
   transformation matrix.
   */
   public RotationAndAV( Matrix66   xform )

      throws SpiceException
   {
      double[][] m   =   new double[3][3];
      double[]v      =   new double[3];

      //
      // Pass the matrix to CSPICE.xf2rav as a one-dimensional array.
      //
      CSPICE.xf2rav( xform.toArray1D(), m, v );

      rotation = new Matrix33( m );
      angvel   = new Vector3 ( v );

      if (  ! rotation.isRotation( NORM_TOL, DET_TOL )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "RotationAndAV",
            "SPICE(NOTAROTATION)",
            "Rotation derived from input state transformation " +
            "matrix either has column magnitudes too far from 1 or " +
            "determinant too far from 1. Matrix is " + rotation         );

         throw ( exc );
      }

   }


   //
   // Methods
   //

   /**
   Get the rotation matrix from this instance.
   */
   public Matrix33 getRotation()
   {
      return(   new Matrix33( rotation )  );
   }


   /**
   Get the angular velocity from this instance.
   */
   public Vector3 getAngularVelocity()
   {
      return(   new Vector3( angvel )  );
   }


   /**
   Convert this instance to a state transformation matrix.
   */
   public Matrix66 toMatrix()

      throws SpiceException
   {
      double[] xformArray = CSPICE.rav2xf( rotation.toArray(),
                                           angvel.toArray()    );

      return(  new Matrix66(xformArray)  );
   }

}
