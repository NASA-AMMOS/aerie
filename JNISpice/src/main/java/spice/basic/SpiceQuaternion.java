
package spice.basic;

/**
Class SpiceQuaternion represents and supports operations
on SPICE-style quaternions.


<p>
Let M be a rotation matrix such that for any vector V,
<pre>
   M*V
</pre>
is the result of rotating V by theta radians in the
counterclockwise direction about unit rotation axis vector A.
Then the SPICE quaternions representing M are
<pre>

   (+/-) (  cos(theta/2),
            sin(theta/2) A(1),
            sin(theta/2) A(2),
            sin(theta/2) A(3)  )
</pre>

<p> Version 1.0.0 22-DEC-2009 (NJB)

*/
public class SpiceQuaternion extends Quaternion
{

   //
   // Fields
   //
   private double[]    q = null;


   //
   // Constructors
   //

   /**
   Zero-arguments constructor: this creates a quaternion
   initialized with zeros.
   */
   public SpiceQuaternion()
   {
      this.q = new double[4];
   }

   /**
   Copy constructor. This creates a deep copy.
   */
   public SpiceQuaternion ( SpiceQuaternion q )
   {
      this.q = new double[4];

      System.arraycopy ( q.q, 0, this.q, 0, 4 );
   }


   /**
   Create a SpiceQuaternion from a double array of length 4.
   The first array element is the scalar part of the quaternion;
   the remaining elements are the vector part.
   */
   public SpiceQuaternion ( double[] inArray )

      throws SpiceException
   {
      if ( inArray.length != 4 )
      {
         SpiceException exc = SpiceErrorException.create(

            "SpiceQuaternion",
            "SPICE(BADDIMENSION)",
            "Input array length must be 4 but was " +
            inArray.length                             );

         throw ( exc );
      }

      this.q = new double[4];

      System.arraycopy ( inArray, 0, this.q, 0, 4 );
   }


   /**
   Create a SpiceQuaternion from a list of four scalars.
   The first element of the list is the scalar part of
   the quaternion.
   */
   public SpiceQuaternion ( double q0,
                            double q1,
                            double q2,
                            double q3 )
   {
      double[] elts = { q0, q1, q2, q3 };
      this.q        = elts;
   }


   /**
   Create a unit SpiceQuaternion from a rotation matrix.
   */
   public SpiceQuaternion ( Matrix33 m )

      throws SpiceException
   {
      q = CSPICE.m2q( m.toArray() );
   }



   //
   // Methods
   //




   /**
   Add a second SpiceQuaternion to this instance.
   */
   public SpiceQuaternion add ( SpiceQuaternion q2 )
   {
      return(  new SpiceQuaternion( this.q[0] + q2.q[0],
                                    this.q[1] + q2.q[1],
                                    this.q[2] + q2.q[2],
                                    this.q[3] + q2.q[3] )  );
   }




   /**
   Return the conjugate of this SpiceQuaternion.
   */
   public SpiceQuaternion conjugate()
   {
      return(  new SpiceQuaternion( q[0], -q[1], -q[2], -q[3] )  );
   }


   /**
   Return the distance (L2) between this quaternion and another.
   */
   public double dist( SpiceQuaternion q2 )
   {
      return(  (this.sub(q2)).norm()  );
   }



   /**
   Map this SpiceQuaternion and its derivative with respect
   to time to an angular velocity vector.
   */
   public Vector3 getAngularVelocity ( SpiceQuaternion  dq )

      throws SpiceException
   {
      double[] avArray = CSPICE.qdq2av ( this.toArray(), dq.toArray() );

      Vector3 av = new Vector3 ( avArray );

      return ( av );
   }




   /**
   Return the element of this quaternion at index [i].
   */
   public double getElt( int i )

      throws SpiceException
   {
      if (  ( i < 0 ) || ( i > 3 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "SpiceQuaternion",
            "SPICE(INDEXOUTOFRANGE)",
            "Index must be in range 0:3 but was " + i );

         throw ( exc );
      }

      return ( q[i] );
   }



   /**
   Return the scalar (real) portion of this instance.
   */
   public double getScalar()
   {
      return( q[0] );
   }


   /**
   Return the vector (imaginary) portion of this instance.
   */
   public Vector3 getVector()
   {
      return(  new Vector3( q[1], q[2], q[3] )  );
   }





   /**
   Left-multiply a SpiceQuaternion by this SpiceQuaternion.

   <pre>
   Let this instance be represented by q1. The returned
   SpiceQuaternion `qout' is the quaternion product

      q1 * q2

   Representing q(i) as the sum of scalar (real)
   part s(i) and vector (imaginary) part v(i)
   respectively,

      q1 = s1 + v1
      q2 = s2 + v2

   `qout' has scalar part s3 defined by

      s3 = s1 * s2 - &#60v1, v2&#62

   and vector part v3 defined by

      v3 = s1 * v2  +  s2 * v1  +  v1 x v2

   where the notation < , > denotes the inner
   product operator and x indicates the cross
   product operator.
   </pre>
   */
   public SpiceQuaternion mult ( SpiceQuaternion q2 )

      throws SpiceException
   {
      double[] retArray = CSPICE.qxq( this.toArray(),
                                      q2.toArray()   );

      return ( new SpiceQuaternion(retArray) );
   }




   /**
   Negate this SpiceQuaternion.
   */
   public SpiceQuaternion negate()
   {
      return(  new SpiceQuaternion( -q[0], -q[1], -q[2], -q[3] )  );
   }



   /**
   Return the norm of this SpiceQuaternion.
   */
   public double norm()
   {
      double ss = 0.0;

      for ( int i = 0;  i < 4;  i++ )
      {
         ss += q[i]*q[i];
      }

      ss = Math.max( 0.0, ss );

      return( Math.sqrt(ss) );
   }




   /**
   Scale this SpiceQuaternion.
   */
   public SpiceQuaternion scale( double s )
   {
      return(  new SpiceQuaternion( s*q[0], s*q[1], s*q[2], s*q[3] )  );
   }



   /**
   Subtract a second SpiceQuaternion from this instance.
   */
   public SpiceQuaternion sub ( SpiceQuaternion q2 )
   {
      return(  new SpiceQuaternion( this.q[0] - q2.q[0],
                                    this.q[1] - q2.q[1],
                                    this.q[2] - q2.q[2],
                                    this.q[3] - q2.q[3] )  );
   }




   /**
   Return the contents of this quaternion in a double array.
   The first array element is the scalar part of the quaternion;
   the remaining elements are the vector part.
   */
   public double[] toArray()
   {
      double[] retArray = new double[4];

      System.arraycopy ( this.q, 0, retArray, 0, 4 );

      return ( retArray );
   }





   /**
   Convert this quaternion to a matrix. If this quaternion
   has unit length, the output will be a rotation matrix.
   No checking is performed on the magnitude of the quaternion.

   <h3>Associating SPICE Quaternions with Rotation Matrices</h3>

   <pre>

   Let FROM and TO be two right-handed reference frames, for
   example, an inertial frame and a spacecraft-fixed frame. Let the
   symbols

      V    ,   V
       FROM     TO

   denote, respectively, an arbitrary vector expressed relative to
   the FROM and TO frames. Let M denote the transformation matrix
   that transforms vectors from frame FROM to frame TO; then

      V   =  M * V
       TO         FROM

   where the expression on the right hand side represents left
   multiplication of the vector by the matrix.

   Then if the unit-length SPICE quaternion q represents M, where

      q = (q0, q1, q2, q3)

   the elements of M are derived from the elements of q as follows:

        +-                                                         -+
        |           2    2                                          |
        | 1 - 2*( q2 + q3 )   2*(q1*q2 - q0*q3)   2*(q1*q3 + q0*q2) |
        |                                                           |
        |                                                           |
        |                               2    2                      |
    M = | 2*(q1*q2 + q0*q3)   1 - 2*( q1 + q3 )   2*(q2*q3 - q0*q1) |
        |                                                           |
        |                                                           |
        |                                                   2    2  |
        | 2*(q1*q3 - q0*q2)   2*(q2*q3 + q0*q1)   1 - 2*( q1 + q2 ) |
        |                                                           |
        +-                                                         -+

   Note that substituting the elements of -q for those of q in the
   right hand side leaves each element of M unchanged; this shows
   that if a quaternion q represents a matrix M, then so does the
   quaternion -q.
   </pre>

   */
   public Matrix33 toMatrix()

      throws SpiceException
   {
      double[][] retArray = CSPICE.q2m( this.toArray() );

      Matrix33 retMat = new Matrix33( retArray );

      return ( retMat );
   }


   /**
   Convert a SpiceQuaternion to a String. This overrides
   Object's toString method.
   */
   public String toString()
   {
      String outStr;

      try
      {
         outStr = String.format(

            "(%24.16e,%n %24.16e, %24.16e, %24.16e)",
              q[0],      q[1],    q[2],    q[3] );

      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr );
   }

}
