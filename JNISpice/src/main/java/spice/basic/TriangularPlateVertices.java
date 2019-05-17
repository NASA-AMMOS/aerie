
package spice.basic;

import spice.basic.*;


/**
Class TriangularPlateVertices represents triangular shapes
in 3-dimensional space as sets of three 3-vectors.

<p> The order of a plate's vertices implies an "outward" 
normal direction: the vertices are ordered in the positive
(counterclockwise) sense about the outward normal direction.

<p> See the related class {@link spice.basic.TriangularPlate} which 
supports various geometric plate operations not covered by this class.

<h3> Version 1.0.0 31-DEC-2016 (NJB)</h3>

*/
public class TriangularPlateVertices
{

   //
   // Fields
   //
   public Vector3[]     vertices;


   //
   // Private utility methods
   //



   //
   // Check the dimension of an input array.
   //
   //    Values of `relation' can be "=", "<", or ">".
   //
   private static void checkArrayDim( String     caller, 
                                      String     type,
                                      String     relation,
                                      int        expectedDim,
                                      Object[]   vArray      )
    
      throws SpiceErrorException

   {
      if ( relation.equals( "=" ) )
      {
         if ( vArray.length != expectedDim )
         {

            SpiceErrorException exc = 

               SpiceErrorException.create( caller,

                  "SPICE(BADARRAYSIZE)",
                  "The "                     + type          +  
                  " array for a triangular " +
                  "plate must have length "  + expectedDim   +
                  " but has length "         + vArray.length + "." );

            throw( exc );
         }
      }
      else if ( relation.equals( ">" ) )
      {
         if ( vArray.length <= expectedDim )
         {

            SpiceErrorException exc = 

               SpiceErrorException.create( caller,

                  "SPICE(BADARRAYSIZE)",
                  "The "                       + type          +  
                  " array for a triangular "   +
                  "plate must have length > "  + expectedDim   +
                  " but has length "           + vArray.length + "." );

            throw( exc );
         }
      }
      else if ( relation.equals( "<" ) )
      {
         if ( vArray.length >= expectedDim )
         {

            SpiceErrorException exc = 

               SpiceErrorException.create( caller,

                  "SPICE(BADARRAYSIZE)",
                  "The "                       + type          +  
                  " array for a triangular "   +
                  "plate must have length < "  + expectedDim   +
                  " but has length "           + vArray.length + "." );

            throw( exc );
         }
      }
      else
      {
         SpiceErrorException exc = 

           SpiceErrorException.create( "TriangularPlateVertices.checkArrayDim",
              "SPICE(BUG)",
              "Unrecognized relational operator was seen:" + relation) ;

         throw( exc );
      }      

   }


   //
   // Check the dimension of an input vertex array.
   //
   private static void checkVertex( String     caller, 
                                    int        index, 
                                    double[]   v      )
    
      throws SpiceErrorException

   {
      if ( v.length != 3 )
      {

         SpiceErrorException exc = 

            SpiceErrorException.create( caller,

               "SPICE(BADVERTEXSIZE)",
               "The vertex at 0-based index " + index +
               " must have length 3 but "     +
               "has length " + v.length  + "."    );

         throw( exc );
      }
   }


   //
   // Constructors
   //

   /**
   No-arguments constructor. This constructor creates 
   a set of zero vectors.
   */
   public TriangularPlateVertices()
   {
      vertices = new Vector3[3];     

      for ( int i = 0;  i < 3;  i++ )
      {
         vertices[i] = new Vector3();
      }
   }

   /**
   Construct a triangular plate array from three Vector3 objects.
   */
   public TriangularPlateVertices( Vector3 v1,
                                   Vector3 v2, 
                                   Vector3 v3  )

      throws SpiceErrorException

   {
      vertices = new Vector3[3];

      vertices[0] = new Vector3( v1 ); 
      vertices[1] = new Vector3( v2 ); 
      vertices[2] = new Vector3( v3 ); 
   }


   /**
   Construct a triangular plate array from three vertex arrays.
   */
   public TriangularPlateVertices( double[] v1,
                                   double[] v2, 
                                   double[] v3  )

      throws SpiceErrorException

   {
      checkVertex( "TriangularPlate constructor", 0, v1 );
      checkVertex( "TriangularPlate constructor", 1, v2 );
      checkVertex( "TriangularPlate constructor", 2, v3 );

      vertices = new Vector3[3];

      vertices[0] = new Vector3( v1 ); 
      vertices[1] = new Vector3( v2 ); 
      vertices[2] = new Vector3( v3 ); 
   }


   /**
   Construct a triangular plate from an array of three vertex arrays.
   */
   public TriangularPlateVertices( double[][] vertices )

      throws SpiceErrorException
   {
      checkArrayDim( "TriangularPlate constructor", 
                     "vertex array",
                     "=",
                     3,
                     vertices );

      for ( int i = 0;  i < 3;  i++ )
      {
         checkVertex( "TriangularPlate constructor", i, vertices[i] );
      }

      this.vertices = new Vector3[3];

      for ( int i = 0;  i < 3;  i++ )
      {
         this.vertices[i] = new Vector3( vertices[i] ); 
      }
   }

 


   /**
   Copy constructor. This constructor generates a deep copy.
   */
   public TriangularPlateVertices( TriangularPlateVertices p )
   {
      this.vertices = new Vector3[3];

      for ( int i = 0;  i < 3;  i++ )
      {
         this.vertices[i] = new Vector3( p.vertices[i] ) ;
      }
   }


   //
   // Public methods
   //

   

   /**
   Extract the vertices of a TriangularPlateVertices instance into an
   array of 3-vectors.
   */
   public Vector3[] toVectors()
   {
      Vector3[] retArray = new Vector3[3];


      for ( int i = 0;  i < 3;  i++ )
      {
         retArray[i] = new Vector3( this.vertices[i] );
      }
 
      return( retArray );
   }



   /**
   Extract the vertices of a TriangularPlateVertices instance into a 
   two-dimensional array of doubles.
   */
   public double[][] toArray()
   {
      double[][] retArray = new double[3][3];

      for ( int i = 0;  i < 3;  i++ )
      {
         System.arraycopy( this.vertices[i].toArray(),  0, 
                           retArray[i],                 0, 3 );
      }
        
      return( retArray );
   }



   /**
   Extract the vertices of a TriangularPlateVertices instance into a 
   one-dimensional array of doubles.
   */
   public double[] toArray1D()
   {
      double[] retArray = new double[9];

      for ( int i = 0;  i < 3;  i++ )
      {
         System.arraycopy( this.vertices[i].toArray(),  0, 
                           retArray,                    3*i,  3 );
      }
        
      return( retArray );
   }

   /**
   Compute an outward normal vector of a triangular plate.
   The vector does not necessarily have unit length.
   */
   public Vector3 getOutwardNormal()

      throws SpiceErrorException
   {
      double[][] vArray = this.toArray();
   
      double[] nArray   = CSPICE.pltnrm( vArray[0], vArray[1], vArray[2] );

      return( new Vector3(nArray) );
   }

   /**
   Expand a triangular plate by a specified amount. The expanded
   plate is co-planar with, and has the same orientation as, the
   original. The centroids of the two plates coincide.

<pre>
   delta      is a fraction by which the plate is to be scaled.
              Scaling is done so that the scaled plate has the
              following properties:

                 -  it is co-planar with the input plate

                 -  its centroid coincides with that of the input
                    plate

                 -  its sides remain parallel to the corresponding
                    sides of the input plate

                 -  the distance of each vertex from the centroid is
                    (1+delta) times the corresponding distance for
                    the input plate
</pre>

   */
   public TriangularPlateVertices expand ( double delta )

      throws SpiceErrorException
   {
      double[][] iverts = this.toArray();
   
      double[][] overts = CSPICE.pltexp( iverts, delta );

      return( new TriangularPlateVertices(overts) );
   }   


   /**
   Find the nearest point on a triangular plate to a 
   specified point.
   */
   public Vector3 getNearPoint( Vector3 point )

      throws SpiceErrorException
   {
      double[]   dist   = new double[1];
      double[][] iverts = this.toArray();
      double[]   p      = point.toArray();
      double[]   pnear  = new double[3];
   
      CSPICE.pltnp( p, iverts[0], iverts[1], iverts[2], pnear, dist );

      return( new Vector3( pnear ) );
   }   


   /**
   Compute the centroid of a triangular plate.
   */
   public Vector3 getCentroid()

      throws SpiceErrorException
   {
      double frac = 1.0/3.0;

      Vector3 centroid = Vector3.lcom( frac, this.vertices[0],
                                       frac, this.vertices[1],
                                       frac, this.vertices[2]  );

      return ( centroid );
   }   


}


















