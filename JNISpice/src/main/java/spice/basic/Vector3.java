
package spice.basic;

/**
Class Vector3 represents and provides methods implementing
mathematical operations on three-dimensional vectors.

<p> Supported operations include:
<pre>
   addition                             {@link #add}
   cross product                        {@link #cross}
   dot product                          {@link #dot}
   distance between vectors             {@link #dist}
   unitize vector                       {@link #hat}
   test for equality to zero            {@link #isZero}
   two-vector linear combination        {@link #lcom(double a, Vector3 v1, 
double b, Vector3 v2)}
   three-vector linear combination      {@link #lcom(double a, Vector3 v1, 
double b, Vector3 v2, double c, Vector3 v3)}
   vector negation                      {@link #negate}
   vector norm                          {@link #norm}
   vector perpendicular component       {@link #perp}
   vector projection onto vector        {@link #proj}
   rotate vector about coordinate axis  {@link #rotate(int axisIndex, 
double angle) }
   rotate vector about vector           {@link #rotate(Vector3 axisVector, 
double angle) }
   scalar multiplication                {@link #scale}
   angular separation                   {@link #sep}
   subtraction                          {@link #sub}
   unitized cross product               {@link #ucross}
</pre>

<p>For methods involving derivatives of functions of 3-dimensional
vectors, such as derivatives of dot products, see the
class {@link spice.basic.Vector6}.


<p> Version 1.0.0 17-NOV-2009 (NJB)
*/
public class Vector3 extends Object
{

   /*
   Instance variables
   */
   double[]           v;



   /*
   Constructors
   */

   /**
   Construct a zero-filled Vector3.
   */
   public Vector3()
   {
      v = new double[3];
   }


   /**
   Copy constructor: create a new Vector3 from another.
   */
   public Vector3 ( Vector3 vin )
   {
      v = new double[3];

      System.arraycopy ( vin.v, 0, v, 0, 3 );
   }


   /**
   Construct a Vector3 from an array of 3 doubles.
   */
   public Vector3 ( double[] vin )
   {
      v = new double[3];

      System.arraycopy ( vin, 0, v, 0, 3 );
   }


   /**
   Construct a Vector3 from three double scalars.
   */
   public Vector3 ( double  v0,
                    double  v1,
                    double  v2 )
   {
      v = new double[3];

      v[0] = v0;
      v[1] = v1;
      v[2] = v2;
   }


   /*
   Instance methods
   */



   /**
   Assign the contents of an array of three doubles to a Vector3.
   */
   public void assign ( double[] values )

      throws SpiceException
   {
      int n = values.length;

      if ( n != 3 )
      {
         SpiceException exc = SpiceErrorException.create(

            "assign",
            "SPICE(INVALIDARRAYSIZE)",
            "Size must be 3 but was " + n      );

         throw ( exc );
      }

      v[0] = values[0];
      v[1] = values[1];
      v[2] = values[2];
   }






   /**
   Add two 3 dimensional vectors.
   */
   public Vector3 add ( Vector3   v2  )


   { /* Begin add */

      Vector3 vout = new Vector3();

      vout.v[0] = this.v[0] + v2.v[0];
      vout.v[1] = this.v[1] + v2.v[1];
      vout.v[2] = this.v[2] + v2.v[2];

      return ( vout );

   } /* End add */



   /**
   Compute the cross product of two 3-dimensional vectors.
   */
   public Vector3 cross ( Vector3   v2 )
   {
      /*
      Local variables
      */
      Vector3  vtemp = new Vector3();

      /*
      Calculate the cross product of this and v2, store in vtemp.
      */
      vtemp.v[0] = this.v[1]*v2.v[2] - this.v[2]*v2.v[1];
      vtemp.v[1] = this.v[2]*v2.v[0] - this.v[0]*v2.v[2];
      vtemp.v[2] = this.v[0]*v2.v[1] - this.v[1]*v2.v[0];

      return ( vtemp );
   }



   /**
   Return the distance between two three-dimensional vectors.
   */
   public double dist ( Vector3  v1 )
   {
      /*
      Return the norm of the difference of this Vector3 instance and
      v1.
      */
      return (  ( this.sub(v1) ).norm()  );
   }



   /**
   Compute the dot product of two double precision, 3-dimensional
   vectors.
   */
   public double dot ( Vector3  v2 )
   {
      return ( this.v[0]*v2.v[0] + this.v[1]*v2.v[1] + this.v[2]*v2.v[2] );
   }



   /**
   Return the element of this instance at index [i].
   */
   public double getElt ( int i )

      throws SpiceException
   {
      if (  ( i < 0 ) || ( i > 2 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "Vector3",
            "SPICE(INDEXOUTOFRANGE)",
            "Index must be in range 0:2 but was " + i );

         throw ( exc );
      }

      return ( v[i] );
   }



   /**
   Find the unit vector along a double precision 3-dimensional vector.
   */
   public Vector3 hat()
   {
      /*
      Obtain the magnitude of this vector.
      */
      double vmag = this.norm();

      /*
      Initialize vout to be the zero vector.
      */
      Vector3 vout = new Vector3();

      /*
      If vmag is nonzero, then unitize.  Note that this process is
      numerically stable: overflow could only happen if vmag were small,
      but this could only happen if each component of `this' were small.
      In fact, the magnitude of any vector is never less than the
      magnitude of any component.
      */
      if ( vmag > 0.0 )
      {
         /*
         Implement

            vout = this.scale ( 1.0/vmag );

         in a numerically robust manner:
         */
         vout.v[0] = this.v[0] / vmag;
         vout.v[1] = this.v[1] / vmag;
         vout.v[2] = this.v[2] / vmag;
      }

      return ( vout );

   }



   /**
   Indicate whether a 3-vector is the zero vector.
   */
   public boolean isZero()
   {
      return  ( v[0] == 0. && v[1] == 0. && v[2] == 0. );
   }



   /**
   Compute a vector linear combination of two double precision,
   3-dimensional vectors.
   */
   public static Vector3 lcom ( double          a,
                                Vector3         v1,
                                double          b,
                                Vector3         v2 )
   {
      return (  ( v1.scale(a) ).add(  v2.scale(b) )  );
   }


   /**
   Compute a vector linear combination of three double precision,
   3-dimensional vectors.
   */
   public static Vector3 lcom ( double          a,
                                Vector3         v1,
                                double          b,
                                Vector3         v2,
                                double          c,
                                Vector3         v3 )
   {
      return (   lcom(a, v1, b, v2).add( v3.scale(c) )   );
   }



   /**
   Negate a double precision 3-dimensional vector.
   */

   public Vector3 negate()

   { /* Begin negate */

      return (  new Vector3 ( -this.v[0], -this.v[1], -this.v[2] )  );

   } /* End negate */



   /**
   Compute the magnitude of a double precision, 3-dimensional vector.
   */
   public double norm ()
   {
      /*
      Determine the maximum component of the vector.
      */
      double max1 = Math.max ( Math.abs(v[0]), Math.abs(v[1]) );
      double vmax = Math.max ( max1,           Math.abs(v[2]) );

      /*
      If the vector is zero, return zero; otherwise normalize first.
      Normalizing helps in the cases where squaring would cause overflow
      or underflow.  In the cases where such is not a problem it not worth
      it to optimize further.
      */

      if ( vmax == 0.0 )
      {
         return ( 0.0 );
      }
      else
      {
         double tmp0     =  v[0]/vmax;
         double tmp1     =  v[1]/vmax;
         double tmp2     =  v[2]/vmax;

         double normSqr  =  tmp0*tmp0 + tmp1*tmp1 + tmp2*tmp2;

         return (  vmax * Math.sqrt( normSqr )  );
      }
   }



   /**
   Find the component of this vector orthogonal to a given vector.
   */
   public Vector3 perp( Vector3 v2 )

      throws SpiceException
   {
      double[] retArray = CSPICE.vperp( this.toArray(), v2.toArray() );

      return(   new Vector3( retArray )  );
   }


   /**
   Find the orthogonal projection of this vector onto a given vector.
   */
   public Vector3 proj( Vector3 v2 )

      throws SpiceException
   {
      double[] retArray = CSPICE.vproj( this.toArray(), v2.toArray() );

      return(   new Vector3( retArray )  );
   }



   /**
   Transform this vector into a basis that is rotated
   in the counterclockwise sense about a given coordinate
   axis by a given angle. Units are radians.

   <p>Equivalently, this method rotates this vector by
   the negative of the input angle about the indicated axis.

   <p>The coordinate axis is identified by an integer:
   <pre>
     1 == X axis
     2 == Y axis
     3 == Z axis
   </pre>
   */
   public Vector3  rotate ( int axisIndex,  double angle )

      throws SpiceException
   {
      double[] retArray = CSPICE.rotvec( this.toArray(),
                                         angle,
                                         axisIndex        );

      return (  new Vector3( retArray )  );
   }



   /**
   Rotate this vector in the counterclockwise sense about a given
   vector by a given angle. Units are radians.
   */
   public Vector3  rotate ( Vector3  axisVector,   double angle )

      throws SpiceException
   {
      double[] retArray = CSPICE.vrotv( this.toArray(),
                                        axisVector.toArray(),
                                        angle                 );

      return (  new Vector3( retArray )  );
   }



   /**
   Multiply a Vector3 by a scalar.
   */
   public Vector3  scale ( double  s )
   {
      return (   new Vector3 (  s * v[0],  s * v[1],  s * v[2]  )   );
   }


   /**
   Compute the difference between two 3-dimensional, double
   precision vectors.
   */
   public Vector3 sub ( Vector3 v2 )

   { /* Begin sub */

      return (  new Vector3 ( this.v[0] - v2.v[0],
                              this.v[1] - v2.v[1],
                              this.v[2] - v2.v[2] )  );

   } /* End sub */




   /**
   Find the separation angle in radians between two double
   precision, 3-dimensional vectors.  This angle is defined as zero
   if either vector is zero.
   */
   public double sep ( Vector3  v2 )
   {
      /*
      Local variables

      The following declarations represent, respectively:

         Unit vectors parallel to this and v2
         Either of the difference vectors: this-v2 or this-(-v2)
         Magnitudes of this, v2
         The return value in radians
      */


      Vector3    u1;
      Vector3    u2;
      Vector3    vtemp;

      double     dmag1;
      double     dmag2;
      double     sep;

      /*
      Calculate the magnitudes of this and v2; if either is 0, sep = 0
      */

      dmag1 = this.norm();
      u1    = this.hat();

      if ( dmag1 == 0.0 )
      {
         return ( 0.0 );
      }

      dmag2 = v2.norm();
      u2    = v2.hat();

      if ( dmag2 == 0.0 )
      {
         return ( 0.0 );
      }

      /*
      At this point we know both vectors are non-zero.
      */
      double dp = u1.dot( u2 );

      if ( dp > 0.0 )
      {
         vtemp = u1.sub ( u2 );

         sep  = 2.0 * Math.asin ( 0.5 * vtemp.norm() );
      }

      else if ( dp < 0.0 )
      {
         vtemp = u1.add ( u2 );

         sep  = Math.PI - 2.0 * Math.asin( 0.5 * vtemp.norm() );
      }

      else
      {
         sep = Math.PI/2;
      }

      return ( sep );

   }



   /**
   Return the contents of a Vector3 in an array of 3 doubles.
   */
   public double[] toArray()
   {
      double[] retVal = new double[3];

      System.arraycopy ( v, 0, retVal, 0, 3 );

      return ( retVal );
   }



   /**
   Return a string representation of the contents of a Vector3.  This
   overrides Object's toString method.
   */
   public String toString()
   {
      String outStr;

      try
      {
         outStr = String.format ( "(%24.16e, %24.16e, %24.16e)",

                                    v[0],    v[1],    v[2] );
      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr );
   }



   /**
   Return the unitized cross product of this vector and a given vector.

   <p>Return the zero vector if the cross product is zero.
   */
   public Vector3 ucross( Vector3 v2 )

      throws SpiceException
   {
      double[] retArray = CSPICE.ucrss( this.toArray(), v2.toArray() );

      return(  new Vector3( retArray )  );
   }


} /* End of class Vector 3 */


