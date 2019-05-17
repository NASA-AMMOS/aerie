
package spice.basic;


/**
Class Matrix33 represents 3 by 3 double precision matrices.

<p> Version 1.0.0  22-DEC-2009 (NJB)
*/
public class Matrix33 extends Object
{

   /*
   Instance variables
   */
   private double[][]                  m;



   //
   // Private methods
   //

   /**
   Private utility to construct a Matrix33 from an array of type double[][].
   Caution:  this utility does no error checking.
   */
   private Matrix33 createMatrix33 ( double[][]  array3x3 )
   {
      Matrix33 temp = new Matrix33();

      for ( int row = 0;  row < 3;  row++ )
      {
         System.arraycopy ( array3x3[row], 0, temp.m[row], 0,  3 );
      }

      return ( temp );
   }




   //
   // Constructors
   //


   /**
   No-arguments constructor: create a zero-filled 3x3 matrix.
   */
   public Matrix33()
   {
      m = new double[3][3];
   }



   /**
   Construct a Matrix33 from another Matrix33. This method
   creates a deep copy.
   */
   public Matrix33 ( Matrix33 matrix )
   {
      m = new double[3][3];

      for ( int i = 0;  i < 3;  i++ )
      {
         System.arraycopy ( matrix.m[i], 0, m[i], 0, 3 );
      }
   }



   /**
   Construct a Matrix33 from an array of type double[][].
   */
   public Matrix33 ( double[][]  array3x3 )

      throws SpiceException
   {
      int width  = (array3x3[0]).length;
      int height =  array3x3.length;

      if (  ( width != 3 ) || ( height != 3 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix33",
            "SPICE(INVALIDSIZE)",
            "Input array must have dimensions [3][3] but has height "
         +  height
         +  " and width "
         +  width
         +  "."                                                      );

         throw ( exc );
      }

      m = new double[3][3];

      for ( int row = 0;  row < 3;  row++ )
      {
         System.arraycopy ( array3x3[row], 0,  m[row], 0, 3 );
      }
   }



   /**
   Construct a Matrix33 from an array of type double[].

   <p> The array is considered to consist of rows 0-2
   of the matrix, in that order.
   */
   public Matrix33 ( double[] array )

      throws SpiceException
   {
      if ( array.length != 9  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix33",
            "SPICE(INVALIDSIZE)",
            "Input array must have length 9 but has length "
         +  array.length
         +  "."                                               );

         throw ( exc );
      }

      m        = new double[3][3];
      int from = 0;

      for ( int row = 0;  row < 3;  row++ )
      {
         System.arraycopy ( array, from,  m[row], 0, 3 );

         from += 3;
      }
   }






   /**
   Construct a Matrix33 from a set of three row vectors.  Each
   row is represented by a Vector3.
   */
   public Matrix33 ( Vector3        row0,
                     Vector3        row1,
                     Vector3        row2  )
   {

      m = new double[3][3];

      System.arraycopy ( row0.toArray(), 0, m[0], 0,  3 );
      System.arraycopy ( row1.toArray(), 0, m[1], 0,  3 );
      System.arraycopy ( row2.toArray(), 0, m[2], 0,  3 );
   }


   /**
   Construct a rotation matrix representing a reference frame
   defined by primary and secondary vectors. A specified axis
   of the frame is aligned with the primary vector; another
   specified axis is aligned with the component of the secondary
   vector that is orthogonal to the primary vector.

   <p> This constructor is analogous to the twovec_c function
   of CSPICE.

   <p> The association of axes and axis numbers is as follows:
   <pre>
      axis     index
      ----     -----
       X         1
       Y         2
       Z         3
   </pre>

   <p>The matrix created by this constructor transforms vectors
   from the standard basis to that defined by the input vectors.

   <h3> Example </h3>

   <p> The numerical results shown for this example
   may differ across platforms. The results depend on
   the compiler and supporting libraries, and the machine
   specific arithmetic implementation.

   <p> Create a matrix that transforms vectors from the standard
   basis to one whose X-axis is aligned with the vector (1,1,1)
   and whose Z-axis is aligned with the component of (0,0,1) that
   is orthogonal to its X-axis.

   <pre>
      import spice.basic.*;

      class Twovec
      {
         //
         // Load the JNISpice shared library.
         //
         static { System.loadLibrary( "JNISpice" ); }


         public static void main ( String[] args )
         {
            try
            {
               Vector3  primary   = new Vector3( 1, 1, 1 );
               Vector3  secondary = new Vector3( 0, 0, 1 );

               Matrix33 r         = new Matrix33( primary, 1, secondary, 3 );

               System.out.println ( r );

            }
            catch ( SpiceException exc ) {
               exc.printStackTrace();
            }
         }
      }
   </pre>

   <p> When run on a PC/Linux/java 1.6.0_14/gcc platform, the output from
       this program was (lines below were wrapped to fit into the
       80 character page width):

   <pre>
        5.7735026918962580e-01,   5.7735026918962580e-01,   5.773502691896258
0e-01,
       -7.0710678118654750e-01,   7.0710678118654750e-01,   0.000000000000000
0e+00,
       -4.0824829046386310e-01,  -4.0824829046386310e-01,   8.164965809277261
0e-01
   </pre>
   */
   public Matrix33 ( Vector3          primaryVector,
                     int              primaryIndex,
                     Vector3          secondaryVector,
                     int              secondaryIndex  )

      throws SpiceException
   {
      if (  ( primaryIndex < 1 ) || ( primaryIndex > 3 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix33",
            "SPICE(INDEXOUTOFRANGE)",
            "Primary axis index must be in range 0:2 but was " + primaryIndex );

         throw ( exc );
      }

      if (  ( secondaryIndex < 1 ) || ( secondaryIndex > 3 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix33",
            "SPICE(INDEXOUTOFRANGE)",
            "Secondary axis index must be in range 0:2 but was " +
            secondaryIndex );

         throw ( exc );
      }

      m = CSPICE.twovec( primaryVector.toArray(),
                         primaryIndex,
                         secondaryVector.toArray(),
                         secondaryIndex             );
   }




   /**
   Construct a rotation matrix that transforms vectors from the standard basis
   to a basis rotated about a specified coordinate axis by a
   specified angle.

   <p> This constructor is analogous to the rotate_c function of CSPICE.

   <p> The association of axes and axis numbers is as follows:
   <pre>
      axis     index
      ----     -----
       X         1
       Y         2
       Z         3
   </pre>

   <p> The angle is expressed in radians.
   */

   public Matrix33 ( int      axisIndex,
                     double   angle     )

      throws SpiceException
   {
      if (  ( axisIndex < 1 ) || ( axisIndex > 3 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix33",
            "SPICE(INDEXOUTOFRANGE)",
            "Axis index must be in range 0:2 but was " + axisIndex );

         throw ( exc );
      }

      //
      // Note the reversal of arguments in the CSPICE routine.
      //
      m = CSPICE.rotate( angle, axisIndex );
   }


   /**
   Construct a rotation matrix that rotates vectors about a
   specified axis vector by a specified angle.

   <p> This constructor is analogous to the axisar_c function of CSPICE.

   <p> The angle is expressed in radians.
   */
   public Matrix33 ( Vector3 axis,
                     double  angle )

      throws SpiceException
   {
      m = CSPICE.axisar( axis.toArray(),  angle );
   }




   //
   // Public methods
   //




   /**
   Add this instance to another Matrix33 instance.
   */
   public Matrix33 add( Matrix33 m2 )
   {
      //
      // Local variables
      //
      Matrix33 mout = new Matrix33();

      for ( int i = 0; i < 3;  i++ )
      {
         for ( int j = 0; j < 3;  j++ )
         {
            mout.m[i][j] = this.m[i][j] + m2.m[i][j];
         }
      }

      return( mout );
   }



   /**
   Return the determinant of this instance.
   */
   public double det()

      throws SpiceException
   {
      return(  CSPICE.det(m) );
   }




   /**
   Return the vector (L2) distance between this
   instance and another Matrix33 instance.
   */
   public double dist( Matrix33 m2 )
   {
      return(   ( this.sub(m2) ).norm()   );
   }



   /**
   Fill all elements of a matrix with a given constant.
   */
   public static Matrix33 fill( double value )
   {
      Matrix33 m0 = new Matrix33();

      for ( int i = 0;  i < 3;  i++  )
      {
         for ( int j = 0;  j < 3;  j++  )
         {
            m0.m[i][j] = value;
         }
      }

      return( m0 );
   }



   /**
   Return the element of this instance at index [i][j].
   */
   public double getElt ( int i,  int j )

      throws SpiceException
   {
      if (  ( i < 0 ) || ( i > 2 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix33",
            "SPICE(INDEXOUTOFRANGE)",
            "Row index must be in range 0:2 but was " + i );

         throw ( exc );
      }

      if (  ( j < 0 ) || ( j > 2 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix33",
            "SPICE(INDEXOUTOFRANGE)",
            "Column index must be in range 0:2 but was " + j );

         throw ( exc );
      }


      return ( m[i][j] );
   }




   /**
   Return the identity matrix.
   */
   public static Matrix33 identity()
   {
      Matrix33 m0 = new Matrix33();

      for ( int i = 0;  i < 3;  i++  )
      {
         m0.m[i][i] = 1.0;
      }

      return( m0 );
   }



   /**
   Invert this instance.
   */
   public Matrix33 invert()

      throws SpiceException
   {
      double[][] mArray = CSPICE.invert( m );

      return(  new Matrix33( mArray )  );
   }


   /**
   Indicate whether this instance is a rotation matrix.

   <p> The input tolerance values are used, respectively,
   as the maximum deviation of the magnitude of the matrix's
   columns from 1 and of the matrix's determinant from 1.
   */
   public boolean isRotation( double  normTol,
                              double  determinantTol )
      throws SpiceException
   {
      return(   CSPICE.isrot(  this.toArray(),
                               normTol,
                               determinantTol  )   );
   }


   /**
   Multiply another Matrix33 instance on the left by this instance.
   */
   public Matrix33 mxm ( Matrix33 m2 )
   {
      //
      // Local variables
      //
      Matrix33 mout = new Matrix33();

      //
      // Compute the result in a temporary 3x3 array.
      //
      for ( int i = 0; i < 3; ++i )
      {
         for ( int j = 0;  j < 3;  ++j )
         {
            mout.m[i][j] = this.m[i][0] * m2.m[0][j] +
                           this.m[i][1] * m2.m[1][j] +
                           this.m[i][2] * m2.m[2][j];
         }
      }

      return ( mout );
   }




   /**
   Multiply a Matrix33 instance on the left by the transpose of this instance.
   */
   public Matrix33  mtxm ( Matrix33  m2 )
   {
      //
      // Local variables
      //
      Matrix33 mout = new Matrix33();

      //
      // Compute the result in a temporary 3x3 array.
      //
      for ( int i = 0; i < 3; ++i )
      {
         for ( int j = 0;  j < 3;  ++j )
         {
            mout.m[i][j] = this.m[0][i] * m2.m[0][j] +
                           this.m[1][i] * m2.m[1][j] +
                           this.m[2][i] * m2.m[2][j];
         }
      }

      return ( mout );
   }



   /**
   Multiply a vector on the left by the transpose of this instance.
   */
   public Vector3  mtxv ( Vector3  vin )
   {
      //
      // Local variables
      //
      Vector3 vtemp = new Vector3();

      for ( int i = 0; i < 3;  i++ )
      {
         vtemp.v[i]   =     this.m[0][i] * vin.v[0]
                          + this.m[1][i] * vin.v[1]
                          + this.m[2][i] * vin.v[2];
      }

      return ( vtemp );
   }



   /**
   Multiply the transpose of a Matrix33 instance on the left by this instance.
   */
   public Matrix33  mxmt ( Matrix33  m2 )
   {
      //
      // Local variables
      //
      Matrix33 mout = new Matrix33();

      //
      // Compute the result in a temporary 3x3 array.
      //
      for ( int i = 0; i < 3; ++i )
      {
         for ( int j = 0;  j < 3;  ++j )
         {
            mout.m[i][j] = this.m[i][0] * m2.m[j][0] +
                           this.m[i][1] * m2.m[j][1] +
                           this.m[i][2] * m2.m[j][2];
         }
      }

      return ( mout );
   }




   /**
   Multiply a Vector3 on the left by this instance.
   */
   public Vector3  mxv ( Vector3 vin )
   {
      //
      // Local variables
      //
      double[]       vtemp    = new double[3];
      double[]       vinArray = vin.toArray();


      for ( int i = 0;  i < 3;  i++ )
      {
         vtemp[i] =    this.m[i][0]*vinArray[0]
                     + this.m[i][1]*vinArray[1]
                     + this.m[i][2]*vinArray[2];
      }

      //
      // Move the computed result to an output Vector3.
      //
      return (  new Vector3( vtemp )  );
   }



   /**
   Compute the vector (L2) norm of this instance.
   */
   public double norm()
   {
      //
      // Find the element of maximum absolute value.
      //
      double maxabs = 0.0;

      for ( int i = 0; i < 3;  i++ )
      {
         for ( int j = 0; j < 3;  j++ )
         {
            maxabs = Math.max(  maxabs,  Math.abs(this.m[i][j])  );
         }
      }

      //
      // If this is the zero matrix, we're ready to return the result.
      //
      if ( maxabs == 0.0 )
      {
         return( 0.0 );
      }

      //
      // Create a scaled copy of this matrix; this will prevent
      // overflow when we square the elements.
      //
      Matrix33 scaleMat = this.scale( 1.0/maxabs );

      //
      // Sum the squares of the elements of scaleMat.
      //
      double sum = 0.0;
      double elt;

      for ( int i = 0; i < 3;  i++ )
      {
         for ( int j = 0; j < 3;  j++ )
         {
            elt = scaleMat.m[i][j];

            sum = sum  +  (elt*elt);
         }
      }

      return(  Math.sqrt(sum) * maxabs  );
   }



   /**
   Subtract another Matrix33 instance from this instance.
   */
   public Matrix33 sub( Matrix33 m2 )
   {
      //
      // Local variables
      //
      Matrix33 mout = new Matrix33();

      for ( int i = 0; i < 3;  i++ )
      {
         for ( int j = 0; j < 3;  j++ )
         {
            mout.m[i][j] = this.m[i][j] - m2.m[i][j];
         }
      }

      return( mout );
   }


   /**
   Multiply this instance by a scalar.
   */
   public Matrix33 scale( double  s )
   {
      //
      // Local variables
      //
      Matrix33 mout = new Matrix33();

      for ( int i = 0; i < 3;  i++ )
      {
         for ( int j = 0; j < 3;  j++ )
         {
            mout.m[i][j] = this.m[i][j]  *  s;
         }
      }

      return( mout );
   }




   /**
   Return a 3x3 array containing the contents of this Matrix33 instance.
   */
   public double[][] toArray()
   {
      double[][] retArray = new double[3][3];


      for ( int i = 0;  i < 3;  i++ )
      {
         System.arraycopy ( m[i], 0, retArray[i], 0, 3 );
      }

      return ( retArray );
   }



   /**
   Return a 1-dimensional array containing the contents of this
   Matrix33 instance.
   */
   public double[] toArray1D()
   {
      double[] retArray = new double[9];

      int to = 0;

      for ( int i = 0;  i < 3;  i++ )
      {
         System.arraycopy ( m[i], 0, retArray, to, 3 );

         to += 3;
      }

      return ( retArray );
   }




   /**
   Convert this instance to a String.  This method overrides Object's
   toString() method.
   */
   public String toString()
   {
      String outStr;

      try
      {
         outStr = String.format(

            "%24.16e, %24.16e, %24.16e,%n" +
            "%24.16e, %24.16e, %24.16e,%n" +
            "%24.16e, %24.16e, %24.16e",

            m[0][0],  m[0][1],  m[0][2],
            m[1][0],  m[1][1],  m[1][2],
            m[2][0],  m[2][1],  m[2][2]      );
      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr );
   }





   /**
   Transpose this instance.
   */
   public Matrix33 xpose()
   {
      Matrix33  mout = new Matrix33();

      /*
      Move the three diagonal elements from this.m to mout.
      */
      mout.m[0][0] = this.m[0][0];
      mout.m[1][1] = this.m[1][1];
      mout.m[2][2] = this.m[2][2];

      /*
      Switch the three pairs of off-diagonal elements.
      */
      mout.m[0][1] = this.m[1][0];
      mout.m[1][0] = this.m[0][1];

      mout.m[0][2] = this.m[2][0];
      mout.m[2][0] = this.m[0][2];

      mout.m[1][2] = this.m[2][1];
      mout.m[2][1] = this.m[1][2];

      return ( mout );
   }



} /* End class Matrix33 */
