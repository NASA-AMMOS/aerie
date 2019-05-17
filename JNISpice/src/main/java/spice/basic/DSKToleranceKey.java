
package spice.basic;

/**
Enum DSKToleranceKey represents keywords associated with 
tolerances used by the DSK subsystem.

<p> Version 1.0.0 08-NOV-2016 (NJB)
*/

public enum DSKToleranceKey
{

   //
   // Set enum attributes.
   //

   /**
   DSK Type 2 plate expansion factor keyword.


   <p>
   The DSK type 2 plate expansion factor
   `XFRACT' is used to slightly expand plates
   read from DSK type 2 segments in order to perform ray-plate
   intercept computations.

   <p>
   This expansion is performed to prevent rays from passing through
   a target object without any intersection being detected. Such
   "false miss" conditions can occur due to round-off errors.

   <p>
   Plate expansion is done by computing the difference vectors
   between a plate's vertices and the plate's centroid, scaling
   those differences by (1 + XFRACT), then producing new
   vertices by adding the scaled differences to the centroid. This
   process doesn't affect the stored DSK data.

   <p>
   Plate expansion is also performed when surface points are mapped
   to plates on which they lie, as is done for illumination angle
   computations.

   <p>
   This parameter is user-adjustable.


   */
   KEYXFR( 1 ),
 
   /**
   Greedy segment selection factor keyword.

   <p>
   The greedy segment selection factor
   `SGREED' is used to slightly expand DSK segment
   boundaries in order to select segments to consider for
   ray-surface intercept computations. The effect of this factor is
   to make the multi-segment intercept algorithm consider all
   segments that are sufficiently close to the ray of interest, even
   if the ray misses those segments.

   <p>
   This expansion is performed to prevent rays from passing through
   a target object without any intersection being detected. Such
   "false miss" conditions can occur due to round-off errors.
 
  <p>
   The exact way this parameter is used is dependent on the
   coordinate system of the segment to which it applies, and the DSK
   software implementation. This parameter may be changed in a
   future version of SPICE.

   <p>
   This parameter is user-adjustable.
   */
   KEYSGR( KEYXFR.getIntKeyword() + 1 ),

   /**
   Segment pad margin keyword.

  <p>
   The segment pad margin is a scale factor used to determine when a
   point resulting from a ray-surface intercept computation, if
   outside the segment's boundaries, is close enough to the segment
   to be considered a valid result.
  <p>
   This margin is required in order to make DSK segment padding
   (surface data extending slightly beyond the segment's coordinate
   boundaries) usable: if a ray intersects the pad surface outside
   the segment boundaries; the pad is useless if the intercept is
   automatically rejected.
  <p>
   However, an excessively large value for this parameter is
   detrimental, since a ray-surface intercept solution found "in" a
   segment will supersede solutions in segments farther from the ray's
   vertex. Solutions found outside of a segment thus can mask solutions
   that are closer to the ray's vertex by as much as the value of this
   margin, when applied to a segment's boundary dimensions.

   <p>
   This parameter is user-adjustable.
   */
   KEYSPM( KEYSGR.getIntKeyword() + 1 ),

   /**
   Surface-point membership margin keyword.
  <p>
   The surface-point membership margin limits the distance
   between a point and a surface to which the point is
   considered to belong. The margin is a scale factor applied
   to the size of the segment containing the surface.
  <p>
   This margin is used to map surface points to outward
   normal vectors at those points.
  <p>
   If this margin is set to an excessively small value,
   routines that make use of the surface-point mapping won't
   work properly.

   <p>
   This parameter is user-adjustable.
   */
   KEYPTM( KEYSPM.getIntKeyword() + 1 ),


   /**
   Angular rounding margin keyword.

   <p>
   This margin specifies an amount by which angular values
   may deviate from their proper ranges without a SPICE error
   condition being signaled.
   <p>
   For example, if an input latitude exceeds pi/2 radians by a
   positive amount less than this margin, the value is treated as
   though it were pi/2 radians.
   <p>
   Units are radians.
   <p>
   This parameter is not user-adjustable.
   */
   KEYAMG( KEYPTM.getIntKeyword() + 1 ),

   /**
   Longitude alias margin keyword.
   <p>
   This margin specifies an amount by which a longitude
   value can be outside a given longitude range without
   being considered eligible for transformation by
   addition or subtraction of 2*pi radians.
   <p>
   A longitude value, when compared to the endpoints of
   a longitude interval, will be considered to be equal
   to an endpoint if the value is outside the interval
   differs from that endpoint by a magnitude less than
   the alias margin.
   <p>
   Units are radians.
   <p>
   This parameter is not user-adjustable.
   */
   KEYLAL( KEYAMG.getIntKeyword() + 1 );



   //
   // Fields
   //
   private final int     keyword;
    

   //
   // Constructor
   // 
   DSKToleranceKey( int keyword )
   {
      this.keyword = keyword;
   }

   //
   // Methods
   //

   /**
   Return integer keyword parameter used by CSPICE.
   */
   public int getIntKeyword()
   {
      return( keyword );
   }
}
