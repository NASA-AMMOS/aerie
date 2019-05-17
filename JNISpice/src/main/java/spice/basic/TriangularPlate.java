
package spice.basic;

import spice.basic.*;

/**
Class TriangularPlate represents triangular shapes used to
tessellate surfaces of extended bodies. 

<p> This class represents each plate as an integer-valued 3-vector,
which is associated with a set of 3-vectors. The integers
are the indices of the vectors in that set comprising the plate's three
vertices. The vertex indices range from 1 to `nv'---the number of
vectors in the associated set. 

<p> Vertex indices are 1-based in all SPICE language versions; this
convention makes them compatible with vertex indices 
used in type 2 DSK segments.

<p> The order of a plate's vertices implies an "outward" 
normal direction: the vertices are ordered in the positive
(counterclockwise) sense about the outward normal direction.

<p> See the related class {@link spice.basic.TriangularPlateVertices} which 
supports various geometric plate operations not covered by this class.

<p> Version 1.0.0 09-NOV-2016 (NJB)

*/
public class TriangularPlate
{

   //
   // Fields
   // 
   public int[]   vertexNumbers;


   //
   // Constructors
   //

   /**
   No-arguments constructor. This constructor
   generates a zero-filled array of ints. Note that
   the resulting plate is not valid until it has
   been initialized.
   */
   public TriangularPlate()
   {
      vertexNumbers = new int[3];
   }


   /**
   Construct a plate from three vertex numbers.

   <p> The caller must ensure the numbers are within
   the valid range for a given set of vertices.
   */
   public TriangularPlate ( int        vix1,
                            int        vix2,
                            int        vix3  )

      throws SpiceErrorException

   {
      vertexNumbers = new int[3];

      vertexNumbers[0] = vix1;
      vertexNumbers[1] = vix2;
      vertexNumbers[2] = vix3;      

      for ( int i = 0;  i < 3;  i++ )
      {
         if ( vertexNumbers[i] < 1 )
         {
            SpiceErrorException exc = SpiceErrorException.create (

                "TriangularPlate constructor",
                "SPICE(BADVERTEXNUMBER)",
                "Vertex numbers must be positive "  + 
                "but the vertex at 1-based index "  +
                (i+1)                               +
                " was " + vertexNumbers[i]          +
                "."                                   );

            throw( exc );
         }
           
      }

   }


   /**
   Construct a plate from another plate.

   <p> This constructor creates a deep copy.
   */
   public TriangularPlate ( TriangularPlate p )
   {
      vertexNumbers = new int[3];
    
      int[] pArray  = p.toArray();

      System.arraycopy ( pArray, 0, vertexNumbers, 0, 3 );
   }




   //
   // Public methods
   //

   /**
   Extract the vertex number of a Triangular plate into an
   array of ints.
   */
   public int[] toArray()
   {
      int[] retArray = new int[3];

      System.arraycopy( vertexNumbers, 0, retArray, 0, 3 );

      return( retArray );
   }


   /**
   Compute the volume of a three-dimensional region bounded by a
   collection of triangular plates.
   */
   public static double volume ( TriangularPlate[]  plates,
                                 Vector3[]          vertices )

      throws SpiceErrorException

   {
      int np = plates.length;
 
      if ( np < 1 )
      {
            SpiceErrorException exc = SpiceErrorException.create (

                "TriangularPlate.volume",
                "SPICE(BADPLATECOUNT)",
                "Plate count must be at least 1 "  + 
                "but was " + np                    +
                "."                                   );

            throw( exc );
      }

      int nv = vertices.length;
 
      if ( nv < 3 )
      {
            SpiceErrorException exc = SpiceErrorException.create (

                "TriangularPlate.volume",
                "SPICE(BADVERTEXCOUNT)",
                "Vertex count must be at least 3 "  + 
                "but was " + nv                     +
                "."                                   );

            throw( exc );
      }

      //
      // Create arrays of native types for use by CSPICE.
      //
      int[]    plateArray  = new int   [np * 3];
      double[] vertexArray = new double[nv * 3];

      //
      // The plate and volume arguments to CSPICE.pltvol
      // are input-only, so we needn't construct new
      // arrays to hold the input values.
      //

      for ( int i = 0;  i < np;  i++ )
      {
         int[] plt = plates[i].toArray();

         int j     = 3*i;

         System.arraycopy( plt, 0, plateArray, j, 3 );
      }
 
      for ( int i = 0;  i < nv;  i++ )
      {
         double[] vert = vertices[i].toArray();

         int   j    = 3*i;

         System.arraycopy( vert, 0, vertexArray, j, 3 );
      }

      double volume = CSPICE.pltvol( nv, vertexArray, np, plateArray );

      return( volume );    
   }


   /**
   Compute the total area of a collection of triangular plates.
   */
   public static double area( TriangularPlate[]  plates,
                              Vector3[]          vertices )

      throws SpiceErrorException

   {
      int np = plates.length;
 
      if ( np < 1 )
      {
            SpiceErrorException exc = SpiceErrorException.create (

                "TriangularPlate.area",
                "SPICE(BADPLATECOUNT)",
                "Plate count must be at least 1 "  + 
                "but was " + np                    +
                "."                                   );

            throw( exc );
      }

      int nv = vertices.length;
 
      if ( nv < 3 )
      {
            SpiceErrorException exc = SpiceErrorException.create (

                "TriangularPlate.area",
                "SPICE(BADVERTEXCOUNT)",
                "Vertex count must be at least 3 "  + 
                "but was " + nv                     +
                "."                                   );

            throw( exc );
      }

      //
      // Create arrays of native types for use by CSPICE.
      //
      int[]    plateArray  = new int   [np * 3];
      double[] vertexArray = new double[nv * 3];

      //
      // The plate and volume arguments to CSPICE.pltar
      // are input-only, so we needn't construct new
      // arrays to hold the input values.
      //

      for ( int i = 0;  i < np;  i++ )
      {
         int[] plt = plates[i].toArray();

         int j     = 3*i;

         System.arraycopy( plt, 0, plateArray, j, 3 );
      }
 
      for ( int i = 0;  i < nv;  i++ )
      {
         double[] vert = vertices[i].toArray();

         int   j    = 3*i;

         System.arraycopy( vert, 0, vertexArray, j, 3 );
      }

      double area = CSPICE.pltar( nv, vertexArray, np, plateArray );

      return( area );    
   }

}

