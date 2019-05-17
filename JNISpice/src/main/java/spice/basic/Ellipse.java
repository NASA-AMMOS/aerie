
package spice.basic;

import spice.basic.CSPICE;

/**
Class Ellipse represents ellipses in 3-dimensional space
and supports geometric operations using ellipses.

<p> Ellipses have a center and two orthogonal semi-axis
vectors. The longer of these is called the "semi-major axis";
the shorter is called the "semi-minor axis."

<p> Ellipses are allowed to be degenerate: one or both semi-axes
may be the zero vector.

<p> Ellipse-plane intersection computations are supported by
class {@link spice.basic.EllipsePlaneIntercept}.

<h3> Version 2.0.0 17-DEC-2016 (NJB) </h3>

Changed access of members from private to package private.
Added no-arguments constructor.

<h3> Version 1.0.0 09-DEC-2009 (NJB) </h3>
*/
public class Ellipse extends Object
{

   //
   // Fields
   //
   Vector3         center;
   Vector3         semiMajorAxis;
   Vector3         semiMinorAxis;


   //
   // Constructors
   //

   /**
   Copy constructor: this constructor creates a deep copy.
   */
   public Ellipse ( Ellipse el )
   {
      this.center        = new Vector3( el.center        );
      this.semiMajorAxis = new Vector3( el.semiMajorAxis );
      this.semiMinorAxis = new Vector3( el.semiMinorAxis );
   }


   /**
   No-arguments constructor.
   */
   public Ellipse()
   {
   }

   /**
   Create a Ellipse from a center and two generating vectors. The
   Ellipse is the set of points `x' such that
   <pre>
      x = center  + ( s * gv1 )  +  ( t * gv2 )
   </pre>
   where `s' and `t' are scalars.
   */
   public Ellipse ( Vector3 center,
                    Vector3 gv1,
                    Vector3 gv2    )

      throws SpiceException
   {
      double[] elArray = CSPICE.cgv2el( center.toArray(),
                                        gv1.toArray(),
                                        gv2.toArray()    );

      Vector3[] cgv    = unpack( elArray );


      this.center        = cgv[0];
      this.semiMajorAxis = cgv[1];
      this.semiMinorAxis = cgv[2];
   }


   /**
   Create a Ellipse from a center and two generating vectors
   that have been packed into a double array, in that order.
   */
   public Ellipse ( double[] elArray )

      throws SpiceException
   {
      if ( elArray.length != 9 )
      {
         SpiceException exc = SpiceErrorException.create(

            "Ellipse",
            "SPICE(INVALIDARRAYSIZE)",
            "Input array `elArray' has length " +
            elArray.length + "; a length of 9 is " +
            "required."                            );

         throw ( exc );
      }

      double[] ctrArray = new double[3];
      double[] majArray = new double[3];
      double[] minArray = new double[3];

      System.arraycopy( elArray, 0, ctrArray, 0, 3 );
      System.arraycopy( elArray, 3, majArray, 0, 3 );
      System.arraycopy( elArray, 6, minArray, 0, 3 );

      double[] elArray1 = CSPICE.cgv2el( ctrArray,
                                         majArray,
                                         minArray );

      Vector3[] cgv      = unpack( elArray1 );

      this.center        = cgv[0];
      this.semiMajorAxis = cgv[1];
      this.semiMinorAxis = cgv[2];
   }



   //
   // Methods
   //

   /**
   Retrieve the center from an Ellipse.
   */
   public Vector3 getCenter()
   {
      return (  new Vector3( this.center )  );
   }

   /**
   Retrieve the semi-major axis from an Ellipse.
   */
   public Vector3 getSemiMajorAxis()
   {
      return (  new Vector3( this.semiMajorAxis )  );
   }


   /**
   Retrieve the semi-minor axis from an Ellipse.
   */
   public Vector3 getSemiMinorAxis()
   {
      return (  new Vector3( this.semiMinorAxis )  );
   }


   /**
   Return the components of this ellipse in a double array. The array contains
   the ellipse's center, semi-major axis, and semi-minor axis,
   in that order.
   */
   public double[] toArray()
   {
      double[] elArray = new double[9];

      System.arraycopy ( this.center.toArray(),        0, elArray, 0,  3 );
      System.arraycopy ( this.semiMajorAxis.toArray(), 0, elArray, 3,  3 );
      System.arraycopy ( this.semiMinorAxis.toArray(), 0, elArray, 6,  3 );

      return ( elArray );
   }


   /**
   Project an Ellipse orthogonally onto a Plane.
   */
   public Ellipse project ( Plane plane )

      throws SpiceException
   {
      //
      // Pack this ellipse into a double array.
      //
      double[] inElArray  = this.toArray();

      double[] outArray   = CSPICE.pjelpl ( inElArray, plane.toArray() );

      return (  new Ellipse( outArray )  );
   }


   /**
   Display an Ellipse as a string; override Object's toString() method.
   */
   public String toString()
   {
      String endl = System.getProperty( "line.separator" );

      /*
      String str = "Center:          " + center        + endl +
                   "Semi-major axis: " + semiMajorAxis + endl +
                   "Semi-minor axis: " + semiMinorAxis;
      */



      String str = "Center, Semi-major axis, Semi-minor axis:" + endl +
                   center        + endl +
                   semiMajorAxis + endl +
                   semiMinorAxis;

      return ( str );
   }



   //
   // Public static methods
   //

   /**
   Unpack a double array into a center and generating vectors.
   */
   public static Vector3[] unpack( double[] elArray )
   {
      Vector3[] resultArray = new Vector3[3];

      resultArray[0] = new Vector3( elArray[0], elArray[1], elArray[2] );
      resultArray[1] = new Vector3( elArray[3], elArray[4], elArray[5] );
      resultArray[2] = new Vector3( elArray[6], elArray[7], elArray[8] );

      return ( resultArray );
   }



   //
   // Private methods
   //



}
