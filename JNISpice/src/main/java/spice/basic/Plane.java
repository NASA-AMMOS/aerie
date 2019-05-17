
package spice.basic;

import spice.basic.CSPICE;

/**
Class plane represents planes in 3-dimensional space
and supports geometric operations using planes.

<p> Ray-plane intercept computations
are performed by class {@link spice.basic.RayPlaneIntercept}.

<p> Limb computations are performed by class {@link spice.basic.Ellipse}.

<p> Ellipse-plane intersections are
 performed by class {@link spice.basic.EllipsePlaneIntercept}.

<h3> Version 1.1.0 17-DEC-2017 (NJB) </h3>

Bug fix: in constructor Plane(double[]), now uses double[]
variable to capture normal vector contents from input array.
This constructor now throws an exception if the input array
has the wrong length.

<h3> Version 1.0.0 08-DEC-2009 (NJB)</h3>
*/
public class Plane extends Object
{
   //
   // Constants
   //
   private static final int        PLMAX = 4;
   //
   // Fields
   //
   private Vector3                 normal;
   private double                  constant;

   //
   // Constructors
   //


   /**
   Copy constructor: this constructor creates a deep copy.
   */
   public Plane ( Plane pl )
   {
      this.normal   = new Vector3( pl.normal );
      this.constant = pl.constant;
   }


   /**
   Create a Plane from a normal vector and a constant. The
   Plane represents a 3-dimensional plane satisfying the
   plane equation
   <pre>
      &#60 x, normal &#62  = constant
   </pre>
   */
   public Plane ( Vector3 normal,
                  double  constant )

      throws SpiceException
   {
      double[] pl = CSPICE.nvc2pl( normal.toArray(), constant );

      this.normal   = new Vector3( pl[0], pl[1], pl[2] );
      this.constant = pl[3];
   }


   /**
   Create a Plane from a normal vector and a point. The
   Plane represents a 3-dimensional plane satisfying the
   plane equation
   <pre>
      &#60 x, normal &#62  =  &#60 point, normal &#62
   </pre>
   */
   public Plane ( Vector3 normal,
                  Vector3 point  )

      throws SpiceException
   {
      double[] pl = CSPICE.nvp2pl( normal.toArray(),
                                   point.toArray()   );

      this.normal   = new Vector3( pl[0], pl[1], pl[2] );
      this.constant = pl[3];
   }


   /**
   Create a Plane from a point and two spanning vectors. The
   Plane represents a 3-dimensional plane consisting of
   the set of points
   <pre>
      point  +  s1 * span1  +  s2 * span2
   </pre>
   where s1, s2 are scalars.
   */
   public Plane ( Vector3 point,
                  Vector3 span1,
                  Vector3 span2  )

      throws SpiceException
   {
      double[] pl = CSPICE.psv2pl( point.toArray(),
                                   span1.toArray(),
                                   span2.toArray()  );

      this.normal   = new Vector3( pl[0], pl[1], pl[2] );
      this.constant = pl[3];
   }



   /**
   Contruct a Plane from a double array.

   The order of elements is the same as that used in SPICELIB planes.
   */
   public Plane( double[] planeArray )

      throws SpiceException
   {
      if ( planeArray.length != PLMAX )
      {
         String msg = "Length of input array must be " + PLMAX +
                      ". Actual length was " + planeArray.length + ".";

         SpiceErrorException exc 

            = SpiceErrorException.create( "Plane( double[] )", 
                                          "SPICE(BADARRAYLENGTH)",
                                          msg                      );
         throw ( exc );
      }

      double[] normalArray = new double[3];

      System.arraycopy ( planeArray, 0, normalArray, 0, 3 );


      constant    = planeArray[3];

      double[] pl = CSPICE.nvc2pl( normalArray, constant );

      this.normal = new Vector3( pl[0], pl[1], pl[2] );
   }


   //
   // Methods
   //

   /**
   Retrieve a normal vector from a Plane. The vector has unit
   length and points away from the origin.
   */
   public Vector3 getNormal()
   {
      return (  new Vector3( this.normal )  );
   }

   /**
   Retrieve a plane constant from a Plane. The constant
   represents the distance of the Plane from the origin.
   The plane is the set of points `x' satisfying the plane
   equation
   <pre>
      &#60 x, normal &#62  = constant
   </pre>
   where `normal' is the vector returned by `getNormal'.
   */
   public double getConstant()
   {
      return ( this.constant );
   }


   /**
   Retrieve a point from a Plane. The point
   closest to the origin is returned.
   */
   public Vector3 getPoint()

      throws SpiceException
   {
      double[] planeArray = this.toArray();

      double[] normal = new double[3];
      double[] point  = new double[3];

      CSPICE.pl2nvp ( planeArray, normal, point );

      return (  new Vector3(point)  );
   }


   /**
   Retrieve two spanning vectors from a plane.
   */
   public Vector3[] getSpanningVectors()

      throws SpiceException
   {
      double[] planeArray = this.toArray();

      double[] point = new double[3];
      double[] span1 = new double[3];
      double[] span2 = new double[3];


      CSPICE.pl2psv ( planeArray, point, span1, span2 );

      Vector3[] retArray = new Vector3[2];

      retArray[0] = new Vector3( span1 );
      retArray[1] = new Vector3( span2 );


      return ( retArray );
   }


   /**
   Project a vector orthogonally onto a Plane. The projection
   is the closest point in the plane to the input vector.
   */
   public Vector3 project( Vector3  v )

      throws SpiceException
   {
      double[]  retArray;

      retArray = CSPICE.vprjp( v.toArray(), this.toArray() );

      return(  new Vector3( retArray )  );
   }



   /**
   Extract contents of a Plane into a double array.

   The order of elements is the same as that used in SPICELIB planes.
   */
   public double[] toArray()
   {
      double[] plArray = new double[4];

      System.arraycopy ( normal.toArray(), 0, plArray, 0, 3 );

      plArray[3] = constant;

      return ( plArray );
   }

}
