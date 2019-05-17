
package spice.basic;

/**
Class Ray represents rays in three-dimensional Euclidean space.

<p> Rays always have unit-length direction vectors.

<p> Version 1.0.0 03-DEC-2009 (NJB)

*/
public class Ray extends Object
{

   //
   // Fields
   //
   private Vector3           vertex;
   private Vector3           direction;


   //
   // Constructors
   //

   /**
   Zero-arguments constructor.
   */
   public Ray()
   {
      //
      // The default ray is aligned with the +Z axis.
      //
      vertex    = new Vector3( 0.d, 0.d, 0.d );
      direction = new Vector3( 0.d, 0.d, 1.d );
   }

   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public Ray( Ray r )
   {
      vertex    = new Vector3( r.vertex    );
      direction = new Vector3( r.direction );
   }

   /**
   Create a Ray from a vertex and direction.

   <p> The stored direction vector is a unit-length copy
   of the corresponding input vector.
   */
   public Ray ( Vector3       vertex,
                Vector3       direction )

      throws SpiceException
   {
      if ( direction.isZero() )
      {
         SpiceException exc = SpiceErrorException.create(

            "Ray",
            "SPICE(ZEROVECTOR)",
            "Input direction vector is the zero vector. " +
            "Rays must have non-zero direction vectors."     );

         throw ( exc );
      }

      this.vertex    =   new Vector3( vertex    );
      this.direction = ( new Vector3( direction ) ).hat();
   }



   //
   // Public Methods
   //

   /**
   Return the vertex for this Ray..
   */
   public Vector3 getVertex()
   {
      return ( new Vector3(vertex) );
   }

   /**
   Return a direction vector for this Ray..
   */
   public Vector3 getDirection()
   {
      return ( new Vector3(direction) );
   }

   /**
   Convert this Ray to a String.
   */
   public String toString()
   {
      String endl   = System.getProperty( "line.separator" );

      String outStr = "Vertex:"   + endl + vertex    + endl +
                      "Direction" + endl + direction;

      return( outStr );
   }
}

