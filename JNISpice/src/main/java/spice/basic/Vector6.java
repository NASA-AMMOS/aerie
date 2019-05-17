
package spice.basic;

import java.util.Arrays;

/**
Class Vector6 represents six-dimensional, double precision vectors.
This class provides the common mathematical operations applicable
to state vectors.

<p> Version 1.0.0 22-DEC-2009 (NJB)
*/
public class Vector6 extends Object
{

   //
   // Instance variables
   //
   private double[]           v;



   //
   // Constructors
   //

   /**
   Construct a zero-filled Vector6.
   */
   public Vector6()
   {
      v = new double[6];
   }


   /**
   Copy constructor: create a Vector6 from another.
   */
   public Vector6 ( Vector6 vin )
   {
      v = new double[6];

      System.arraycopy ( vin.v, 0, v, 0, 6 );
   }

   /**
   Construct a Vector6 from 6 double scalars.
   */
   public Vector6 ( double s0, double s1, double s2,
                    double s3, double s4, double s5 )
   {
      v = new double[6];

      v[0] = s0;
      v[1] = s1;
      v[2] = s2;
      v[3] = s3;
      v[4] = s4;
      v[5] = s5;
   }


   /**
   Construct a Vector6 from an array of 6 doubles.
   */
   public Vector6 ( double[] vin )

      throws SpiceException
   {
      if ( vin.length < 6 )
      {
         SpiceException exc = SpiceErrorException.create(

            "Vector6",

            "SPICE(LENGTHOUTOFRANGE)",

            "Input vector `vin' has length " +
            vin.length + "; a length >= 6 is " +
            "required."                            );

         throw ( exc );
      }

      v = new double[6];

      System.arraycopy ( vin, 0, v, 0, 6 );
   }



   /**
   Construct a Vector6 from two three-vectors.
   */
   public Vector6( Vector3  v1,  Vector3  v2 )
   {
      this.v = new double[6];

      System.arraycopy( v1.toArray(), 0, this.v, 0, 3 );
      System.arraycopy( v2.toArray(), 0, this.v, 3, 3 );
   }
   //
   // Methods
   //



   /**
   Add a Vector6 instance to this instance.
   */
   public Vector6 add ( Vector6  v2  )
   {
      Vector6 vout = new Vector6();

      for ( int i = 0;  i < 6;  i++ )
      {
         vout.v[i] = this.v[i] + v2.v[i];
      }

      return ( vout );
   }



   /**
   Assign to a Vector6 instance the contents of an array of 6 doubles.
   */
   public void assign ( double[] values )

      throws SpiceException
   {
      int dim = values.length;

      if ( dim != 6 )
      {
         String msg = "Dimension of input array must be 6 but was " + dim;

         SpiceException exc = new SpiceException ( msg );

         throw ( exc );
      }

      System.arraycopy ( values, 0, this.v, 0, 6 );
   }


   /**
   Return the cross product and corresponding derivative defined by two
   state vectors, where the state vectors are represented by this and a
   second Vector6 instance.
   */
   public Vector6 dcross( Vector6 s2 )

      throws SpiceException
   {
      double[] retArray = CSPICE.dvcrss( this.toArray(), s2.toArray() );

      return(  new Vector6( retArray )  );
   }



   /**
   Return the derivative of the dot product of the position
   components of two state vectors, where the state vectors
   are represented by this and a second Vector6 instance.
   */
   public double ddot( Vector6 s2 )

      throws SpiceException
   {
      return(  CSPICE.dvdot( this.toArray(), s2.toArray() )  );
   }


   /**
   Return the unit-length vector and corresponding derivative
   defined by a state vector, where the state vector is represented by
   this instance.
   */
   public Vector6 dhat()

      throws SpiceException
   {
      double[] retArray = CSPICE.dvhat( this.toArray() );

      return( new Vector6(retArray) );
   }



   /**
   Return the distance between this and a second Vector6 instance.
   */
   public double dist( Vector6  v2 )
   {
      return(   ( this.sub(v2) ).norm()  );
   }


   /**
   Return the dot product of this and a second Vector6 instance.
   */
   public double dot( Vector6 v2 )

      throws SpiceException
   {
      double sum = 0.0;

      for ( int i = 0;  i < 6;  i++ )
      {
         sum  +=  this.v[i] * v2.v[i];
      }

      return( sum );
   }




   /**
   Return the derivative of the angular separation of the position
   components of two state vectors, where the state vectors
   are represented by this and a second Vector6 instance.
   */
   public double dsep( Vector6 s2 )

      throws SpiceException
   {
      return(  CSPICE.dvsep( this.toArray(), s2.toArray() )  );
   }




   /**
   Return the element of this instance at index [i].
   */
   public double getElt ( int i )

      throws SpiceException
   {
      if (  ( i < 0 ) || ( i > 5 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "getElt",
            "SPICE(INDEXOUTOFRANGE)",
            "Index must be in range 0:5 but was " + i );

         throw ( exc );
      }

      return ( v[i] );
   }



   /**
   Regarding this instance as an array of two 3-vectors, return
   the specified 3-vector.
   */
   public Vector3 getVector3 ( int i )

      throws SpiceException
   {
      if (  ( i < 0 ) || ( i > 1 )  )
      {
         SpiceException exc = SpiceErrorException.create(

            "getVector3",
            "SPICE(INDEXOUTOFRANGE)",
            "Index must be in range 0:1 but was " + i );

         throw ( exc );
      }

      return(  new Vector3( v[3*i], v[3*i + 1], v[3*i + 2] )  );
   }



   /**
   Indicate whether a Vector6 instance is the zero vector.
   */
   public boolean isZero()
   {
      return  ( v[0] == 0.0 && v[1] == 0.0 && v[2] == 0.0 &&
                v[3] == 0.0 && v[4] == 0.0 && v[5] == 0.0     );
   }




   /**
   Compute a vector linear combination of two Vector6 instances.
   */
   public static Vector6 lcom ( double          a,
                                Vector6         v1,
                                double          b,
                                Vector6         v2 )
   {
      Vector6 vout = new Vector6();

      for ( int i = 0;  i < 6;  i++ )
      {
         vout.v[i] = ( a * v1.v[i] ) + ( b * v2.v[i] );
      }

      return ( vout );

   } /* End lcom */




   /**
   Negate a Vector6 instance, returning a new instance.
   */
   public Vector6 negate()
   {
      Vector6 vout = new Vector6();

      for ( int i = 0;  i < 6;  i++ )
      {
         vout.v[i] = -this.v[i];
      }

      return ( vout );
   }



   /**
   Return the vector (L2) norm of this instance.
   */
   public double norm()
   {
      if ( this.isZero() )
      {
         return( 0.0 );
      }

      //
      // Find the element of maximum magnitude.
      //

      double maxmag = Math.abs( v[0] );

      for( int i = 1;  i < 6;  i++ )
      {
         maxmag = Math.max(  maxmag,  Math.abs( v[i] )  );
      }

      //
      // Scale this vector by the reciprocal of this magnitude.
      //
      Vector6 scaledV = new Vector6( this );

      for( int i = 0;  i < 6;  i++ )
      {
         scaledV.v[i] = scaledV.v[i] / maxmag;
      }

      //
      // We can safely sum the squares of the elements of
      // scaledV.
      //

      double sumsquare = 0.0;

      for( int i = 0;  i < 6;  i++ )
      {
         sumsquare  +=  scaledV.v[i] * scaledV.v[i];
      }

      //
      // The norm of the original vector is obtained by
      // scaling the norm of scaledV by maxmag.
      //
      return(   maxmag  *  Math.sqrt(  Math.max( 1.0, sumsquare )  )   );
   }



   /**
   Scale a Vector6 instance, creating a new instance.
   */
   public Vector6  scale ( double  s )
   {
      Vector6 vout = new Vector6();

      for ( int i = 0;  i < 6;  i++ )
      {
         vout.v[i] = s * this.v[i];
      }

      return ( vout );
   }




   /**
   Subtract a Vector6 instance from this instance.
   */
   public Vector6 sub ( Vector6 v2 )
   {
      Vector6 vout = new Vector6();

      for ( int i = 0;  i < 6;  i++ )
      {
         vout.v[i] = this.v[i] - v2.v[i];
      }

      return ( vout );
   }



   /**
   Return the contents of a Vector6 in an array of 6 doubles.
   */
   public double[] toArray()
   {
      double[] retVal = new double[6];

      System.arraycopy ( v, 0, retVal, 0, 6 );

      return ( retVal );
   }


   /**
   Return a string representation of the contents of a Vector6.  This
   overrides Object's toString method.
   */
   public String toString()
   {
      String outStr;

      try
      {
         outStr = String.format ( "(%24.16e, %24.16e, %24.16e,%n" +
                                  " %24.16e, %24.16e, %24.16e)",
                                   v[0], v[1], v[2],
                                   v[3], v[4], v[5]              );
      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr );
   }

}


