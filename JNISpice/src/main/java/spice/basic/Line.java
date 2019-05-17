
package spice.basic;

/**
Class Line represents lines in three-dimensional Euclidean space.

<p> Lines always have non-zero direction vectors.

<p> Version 1.0.0 01-DEC-2009 (NJB)

*/
public class Line extends Object
{
   //
   // Fields
   //
   private Ray               ray = null;


   //
   // Constructors
   //

   /**
   Zero-arguments constructor. This creates a line coincident
   with the Z axis.
   */
   public Line()
   {
      //
      // The default ray is aligned with the +Z axis.
      //
      ray = new Ray();
   }

   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public Line( Line line )
   {
      ray = new Ray( line.getRay() );
   }


   /**
   Create a Line from a Ray.
   */
   public Line ( Ray ray )
   {
      this.ray = new Ray( ray );
   }


   /**
   Create a Line from a point and direction.
   */
   public Line ( Vector3      point,
                 Vector3      direction )

      throws SpiceException
   {
      if ( direction.isZero() )
      {
         SpiceException exc = SpiceErrorException.create(

            "Line",
            "SPICE(ZEROVECTOR)",
            "Input direction vector is the zero vector. " +
            "Lines must have non-zero direction vectors."     );

         throw ( exc );
      }

      ray = new Ray( point, direction );
   }




   //
   // Public Methods
   //

   /**
   Return a ray included by this Line.
   */
   public Ray getRay()
   {
      return ( new Ray(ray) );
   }

   /**
   Return a point on this Line.
   */
   public Vector3 getPoint()
   {
      return ( ray.getVertex() );
   }

   /**
   Return a direction vector for this Line.
   */
   public Vector3 getDirection()
   {
      return ( ray.getDirection() );
   }


   /**
   Find the closest point on this line to a specified point.
   */
   public Vector3 getNearPoint ( Vector3 point )

      throws SpiceException
   {
      double[] dist    = new double[1];
      double[] npArray = new double[3];

      CSPICE.nplnpt( ray.getVertex().toArray(),
                     ray.getDirection().toArray(),
                     point.toArray(),
                     npArray,
                     dist                        );

      return( new Vector3(npArray) );
   }

}

