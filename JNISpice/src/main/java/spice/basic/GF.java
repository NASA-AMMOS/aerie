
package spice.basic;

/**
Class GF is the root class of the JNISpice Geometry Finder
subsystem. This subsystem finds times when specific geometric
conditions are met. 

<p>
See the subclasses of {@link GFNumericSearch} and 
{@link GFBinaryStateSearch} for the methods used to perform
searches.

 
<h2>Version 1.0.0 29-NOV-2016 (NJB)</h2>

*/
public abstract class GF
{

//
// Public class fields
//

   //
   // FOV parameters
   //
   /**
   Maximum allowed number of boundary vectors for a polygonal FOV.
   */
   public static final int               MAXVRT = 10000;

   /**
   Parameter identifying a circular FOV.
   */
   public static final String            CIRFOV = "CIRCLE";

   /**
   Parameter identifying an elliptical FOV.
   */
   public static final String            ELLFOV = "ELLIPSE";
   
   /**
   Parameter identifying a polygonal FOV.
   */
   public static final String            POLFOV = "POLYGON";

   /**
   Parameter identifying a rectangular FOV.
   */
   public static final String            RECFOV = "RECTANGLE";

   /**
   A small positive number used to
   constrain the orientation of the
   boundary vectors of polygonal FOVs. Such
   FOVs must satisfy the following
   constraints:
   <pre>
    1)  The boundary vectors must be
        contained within a right circular
        cone of angular radius less than
        than (pi/2) - MARGIN radians; in
        other words, there must be a vector
        A such that all boundary vectors
        have angular separation from A of
        less than (pi/2)-MARGIN radians.

    2)  There must be a pair of boundary
        vectors U, V such that all other
        boundary vectors lie in the same
        half space bounded by the plane
        containing U and V. Furthermore, all
        other boundary vectors must have
        orthogonal projections onto a plane
        normal to this plane such that the
        projections have angular separation
        of at least 2*MARGIN radians from
        the plane spanned by U and V.
   </pre>

   */
   public static final double            MARGIN = 1.e-12;



   //
   // Occultation parameters
   //

   /**
   Parameter identifying an "annular
   occultation." This geometric condition
   is more commonly known as a "transit."
   The limb of the background object must
   not be blocked by the foreground object
   in order for an occultation to be
   "annular."   
   */
   public static final String            ANNULR = "ANNULAR";

   /**
   Parameter identifying any type of
   occultation or transit.   
   */
   public static final String            ANY    = "ANY";

   /**
   Parameter identifying a full
   occultation: the foreground body
   entirely blocks the background body.   
   */
   public static final String            FULL   = "FULL";

   /**
   Parameter identifying a "partial
   occultation." This is an occultation in
   which the foreground body blocks part,
   but not all, of the limb of the
   background body.   
   */
   public static final String            PARTL  = "PARTIAL";


   //
   // Target shape parameters
   //

   /**
   Parameter indicating a target object's
   shape is modeled as an ellipsoid.   
   */
   public static final String            EDSHAP = "ELLIPSOID";

   /**
   Parameter indicating a target object's
   shape is modeled as a point.   
   */
   public static final String            PTSHAP = "POINT";

   /**
   Parameter indicating a target object's
   "shape" is modeled as a ray emanating
   from an observer's location. This model
   may be used in visibility computations
   for targets whose direction, but not
   position, relative to an observer is
   known.   
   */
   public static final String            RYSHAP = "RAY";

   /**
   Parameter indicating a target object's
   shape is modeled as a sphere.   
   */
   public static final String            SPSHAP = "SPHERE";

   //
   // Search parameters
   //

   /**
   ADDWIN is a parameter used in numeric quantity
   searches that use an equality
   constraint. This parameter is used to
   expand the confinement window (the
   window over which the search is
   performed) by a small amount at both
   ends. This expansion accommodates the
   case where a geometric quantity is equal
   to a reference value at a boundary point
   of the original confinement window. 

   <p>Units are TDB seconds.  
   */
   public static final double            ADDWIN = 1.0;

   /**
   is the default convergence tolerance
   used by GF routines that don't support a
   user-supplied tolerance value. GF
   searches for roots will terminate when a
   root is bracketed by times separated by
   no more than this tolerance.

   <p> Units are TDB seconds.   
   */
   public static final double            CNVTOL = 1.e-6;

//
// Static methods
//

/**
Set the convergence tolerance of the GF subsystem.
*/
public static void setTolerance ( TDBDuration tol )

  throws SpiceException
{
   CSPICE.gfstol( tol.getMeasure() );
}


}
