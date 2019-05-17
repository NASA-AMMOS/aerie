
package spice.basic;


import java.util.*;
import java.text.*;
import spice.basic.CSPICE;


/**
Class Matrix66 represents 6 by 6 double precision matrices.

<p> Version 1.0.0 22-DEC-2009 (NJB)
*/
public class Matrix66 extends Object
{

   /*
   Instance variables
   */
   private double[][]                  m;




   /*
   Private instance methods
   */


   /**
   Private utility to construct a Matrix66 from an array of type double[][].
   Caution:  this utility does no error checking.
   */
   private Matrix66 createMatrix66 ( double[][]  array6x6 )
   {
      Matrix66 temp = new Matrix66();

      for ( int row = 0;  row < 6;  row++ )
      {
         System.arraycopy ( array6x6[row], 0, temp.m[row], 0, 6 );
      }

      return ( temp );
   }






   /*
   Constructors
   */


   /**
   Construct a zero-filled matrix.
   */
   public Matrix66()
   {
      m = new double[6][6];
   }



   /**
   Construct a Matrix66 from another Matrix66. This method
   creates a deep copy.
   */
   public Matrix66 ( Matrix66 matrix )
   {
      m = new double[6][6];

      for ( int i = 0;  i < 6;  i++ )
      {
         System.arraycopy ( matrix.m[i], 0, m[i], 0, 6 );
      }
   }



   /**
   Construct a Matrix66 from an array of type double[][].
   */
   public Matrix66 ( double[][]  array6x6 )

      throws SpiceException
   {
      int width  = (array6x6[0]).length;
      int height =  array6x6.length;

      if (  ( width != 6 ) || ( height != 6 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix66",
            "SPICE(INVALIDSIZE)",
            "Input array must have dimensions [6][6] but has height "
         +  height
         +  " and width "
         +  width
         +  "."                                                      );

         throw( exc );
      }

      m = new double[6][6];

      for ( int row = 0;  row < 6;  row++ )
      {
         System.arraycopy ( array6x6[row], 0,  m[row], 0, 6 );
      }
   }


   /**
   Construct a Matrix66 from an array of type double[]. The
   array elements are assumed to be arin row-major order.
   */
   public Matrix66 ( double[]  array36 )

      throws SpiceException
   {
      int length  = array36.length;


      if ( length != 36 )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix66",
            "SPICE(INVALIDSIZE)",
            "Input array must have dimension [36] but has length " +
            +  length + "."                                             );

         throw( exc );
      }

      m = new double[6][6];

      int start = 0;

      for ( int row = 0;   row < 6;   row++ )
      {
         System.arraycopy ( array36, start,  m[row], 0, 6 );

         start += 6;
      }
   }





   /**
   Create a Matrix66 from a rotation matrix and an angular
   velocity vector.
   */
   Matrix66 ( Matrix33 r,  Vector3 av )

      throws SpiceException
   {
      /*
      Local variables
      */
      double[][]         rArray;
      double[]           avArray;
      double[]           retArray;

      rArray  = r.toArray();
      avArray = av.toArray();

      /*
      CSPICE.rav2xf returns a 1-dimensional array of length 36.
      The array has row-major order.
      */
      retArray = CSPICE.rav2xf ( rArray, avArray );

      for ( int i = 0;  i < 6;  i++ )
      {
         /*
         Fill in the ith row of m.
         */
         System.arraycopy ( retArray, 6*i, m[i], 0, 6 );
      }
   }




   //
   // Public methods
   //


   /**
   Return the identity matrix.
   */
   public static Matrix66 identity()
   {
      Matrix66 m0 = new Matrix66();

      for ( int i = 0;  i < 6;  i++  )
      {
         m0.m[i][i] = 1.0;
      }

      return( m0 );
   }


   /**
   Return a 6x6 array containing the contents of a Matrix66 instance.
   */
   public double[][] toArray()
   {
      double[][] retArray = new double[6][6];


      for ( int i = 0;  i < 6;  i++ )
      {
         System.arraycopy ( m[i], 0, retArray[i], 0, 6 );
      }

      return ( retArray );
   }


   /**
   Return a one-dimensional array containing the
   contents of a Matrix66 instance. The contents of the array
   are arranged in row-major order.
   */
   public double[] toArray1D()
   {
      double[] retArray = new double[36];

      int start = 0;

      for ( int i = 0;  i < 6;  i++ )
      {
         System.arraycopy ( m[i], 0, retArray, start, 6 );

         start += 6;
      }

      return ( retArray );
   }


   /**
   Return the element of this instance at index [i][j].
   */
   public double getElt ( int i,  int j )

      throws SpiceException
   {
      if (  ( i < 0 ) || ( i > 5 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix66",
            "SPICE(INDEXOUTOFRANGE)",
            "Row index must be in range 0:5 but was " + i );

         throw ( exc );
      }

      if (  ( j < 0 ) || ( j > 5 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix66",
            "SPICE(INDEXOUTOFRANGE)",
            "Column index must be in range 0:5 but was " + j );

         throw ( exc );
      }
      return ( m[i][j] );
   }


   /**
   Return a specified 3x3 block from this instance.

   <p> The arguments `blockRow' and `blockCol' refer to row and
   column indices of the matrix when considered as a 2x2
   matrix of 3x3 blocks. The range of `blockRow' and `blockCol' is
   0:1.
   */
   public Matrix33 getBlock( int blockRow,  int blockCol )

      throws SpiceException
   {
      if (  ( blockRow < 0 ) || ( blockRow > 1 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix66",
            "SPICE(INDEXOUTOFRANGE)",
            "Allowed range of block row is 0:1 but " +
                      "block row is " + blockRow + "."   );

         throw ( exc );
      }

      if (  ( blockCol < 0 ) || ( blockCol > 1 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Matrix66",
            "SPICE(INDEXOUTOFRANGE)",
            "Allowed range of block column is 0:1 but " +
                      "block column is " + blockCol + "."   );

         throw( exc );
      }

      double[][] block = new double[3][3];

      int rb = 3 * blockRow;
      int cb = 3 * blockCol;

      for ( int i = 0; i < 3; ++i )
      {
         for ( int j = 0;  j < 3;  ++j )
         {
            block[i][j] = this.m[rb + i][cb + j];
         }
      }

      return( new Matrix33(block) );
   }


   /**
   Left-multiply a second Matrix66 instance by this instance.
   */
   public Matrix66 mxm ( Matrix66 m2 )
   {
      /*
      Local variables
      */
      double[][] temp = new double[6][6];

      /*
      Compute the result in a temporary 6x6 array.
      */
      for ( int i = 0; i < 6; ++i )
      {
         for ( int j = 0;  j < 6;  ++j )
         {
            temp[i][j] = 0.0;

            for ( int k = 0;  k < 6;  ++k )
            {
               temp[i][j] += this.m[i][k] * m2.m[k][j];
            }
         }
      }

      return (  createMatrix66 ( temp )  );
   }




   /**
   Left-multiply a 6-dimensional double precision vector by a 6x6
   double precision matrix.
   */
   public Vector6  mxv ( Vector6 vin )

      throws SpiceException
   {
      /*
      Local variables
      */
      double[]       vtemp    = new double[6];
      double[]       vinArray = vin.toArray();


      for ( int i = 0;  i < 6;  i++ )
      {
         vtemp[i] = 0.0;

         for ( int j = 0;  j < 6;  j++ )
         {
            vtemp[i] += this.m[i][j]*vinArray[j];
         }
      }

      return (  new Vector6 ( vtemp )  );
   }


   /**
   Add this instance to another Matrix66 instance.
   */
   public Matrix66 add( Matrix66 m2 )
   {
      //
      // Local variables
      //
      Matrix66 mout = new Matrix66();

      for ( int i = 0; i < 6;  i++ )
      {
         for ( int j = 0; j < 6;  j++ )
         {
            mout.m[i][j] = this.m[i][j] + m2.m[i][j];
         }
      }

      return( mout );
   }


   /**
   Subtract another Matrix66 instance from this instance.
   */
   public Matrix66 sub( Matrix66 m2 )
   {
      //
      // Local variables
      //
      Matrix66 mout = new Matrix66();

      for ( int i = 0; i < 6;  i++ )
      {
         for ( int j = 0; j < 6;  j++ )
         {
            mout.m[i][j] = this.m[i][j] - m2.m[i][j];
         }
      }

      return( mout );
   }


   /**
   Multiply this instance by a scalar.
   */
   public Matrix66 scale( double  s )
   {
      //
      // Local variables
      //
      Matrix66 mout = new Matrix66();

      for ( int i = 0; i < 6;  i++ )
      {
         for ( int j = 0; j < 6;  j++ )
         {
            mout.m[i][j] = this.m[i][j]  *  s;
         }
      }

      return( mout );
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

      for ( int i = 0; i < 6;  i++ )
      {
         for ( int j = 0; j < 6;  j++ )
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
      Matrix66 scaleMat = this.scale( 1.0/maxabs );

      //
      // Sum the squares of the elements of scaleMat.
      //
      double sum = 0.0;
      double elt;

      for ( int i = 0; i < 6;  i++ )
      {
         for ( int j = 0; j < 6;  j++ )
         {
            elt = scaleMat.m[i][j];

            sum = sum  +  (elt*elt);
         }
      }

      return(  Math.sqrt(sum) * maxabs  );
   }


   /**
   Return the vector (L2) distance between this
   instance and another Matrix66 instance.
   */
   public double dist( Matrix66 m2 )
   {
      return(   ( this.sub(m2) ).norm()   );
   }








   /**
   Transpose the 3x3 blocks of this Matrix66 instance.

   <p>This transformation inverts a state transformation matrix.
   */

   public Matrix66 transposeByBlocks()

      throws SpiceException
   {
      /*
      Local variables
      */
      double[][]    temp = new double[6][6];

      int           cb;
      int           rb;

      for ( int i = 0;  i < 2;  i++ )
      {
         for ( int j = 0;  j < 2;  j++ )
         {
            /*
            Transpose the (i,j) block.
            */
            rb = 3*i;
            cb = 3*j;

            for ( int k = 0;  k < 3;  k++ )
            {
               for ( int w = 0;  w < 3;  w++ )
               {
                  temp[rb+k][cb+w] = m[rb+w][cb+k];
               }
            }

         }
      }

      return (  new Matrix66( temp )  );
   }







   /**
   Utility for displaying a Matrix66.  This method overrides Object's
   toString() method.
   */
   public String toString()
   {
      String        s;
      StringBuilder strbuf = new StringBuilder( "" );

      int          i;
      int          j;

      try
      {
         String      endl   = System.getProperty( "line.separator" );

         String[][]  titles = new String[2][2];

         titles[0][0] = "Upper left block:";
         titles[0][1] = "Upper right block:";
         titles[1][0] = "Lower left block:";
         titles[1][1] = "Lower right block:";


         for ( int blockRow = 0;  blockRow < 2;  blockRow++ )
         {
            for ( int blockCol = 0;  blockCol < 2;  blockCol++ )
            {
               strbuf.append ( "" + endl + endl );

               i  = 3*blockRow;
               j  = 3*blockCol;

               s = String.format(

                           "%s%n%n"                       +
                           "%24.16e, %24.16e, %24.16e,%n" +
                           "%24.16e, %24.16e, %24.16e,%n" +
                           "%24.16e, %24.16e, %24.16e",

                           titles[blockRow][blockCol],

                           m[i+0][j+0],  m[i+0][j+1],  m[i+0][j+2],
                           m[i+1][j+0],  m[i+1][j+1],  m[i+1][j+2],
                           m[i+2][j+0],  m[i+2][j+1],  m[i+2][j+2]  );

               strbuf.append( s );
            }
         }

         strbuf.append( endl );

      }
      catch ( Exception exc )
      {
         strbuf = new StringBuilder( exc.getMessage() );
      }


      return ( new String(strbuf) );
   }



} /* End class Matrix66 */
