
package spice.basic;

import static spice.basic.DLADescriptor.DLADSZ;
import static spice.basic.DSKDescriptor.DSKDSZ;

/**
Class SurfaceInterceptWithDSKInfo provides methods that
compute ray-DSK surface intercepts and return information on the
data source used to define the surface representation at 
the intercept points.

<p> A SurfaceInterceptWithDSKInfo instance consists of:
<ul>
<li>A SurfaceIntercept instance, which is inherited.</li>
<li>A DSK instance designating the DSK file that provided
data representing the target surface at the intercept point.</li>
<li>The DLA descriptor of the DSK segment providing
these surface data.</li>
<li>The DSK descriptor of the DSK segment providing
these surface data.</li>
<li>A type double array containing data-type specific
parameters associated with the DSK segment.</li>
<li>A type int array containing data-type specific
parameters associated with the DSK segment.</li></li>
</ul>

<p> This class provides functionality analogous to that
of the SPICE routine DSKXSI. In this class, surface intercept 
computations are performed by construction of 
SurfaceInterceptWithDSKInfo objects. The principal
constructor is 
{@link SurfaceInterceptWithDSKInfo#SurfaceInterceptWithDSKInfo(
boolean, Body, Surface[], Time, ReferenceFrame, Vector3,
Vector3 )}.

<p> For functionality similar to that of the SPICE routine
SINCPT, see the constructor 
{@link SurfaceIntercept#SurfaceIntercept( String, Body, Time, ReferenceFrame,
AberrationCorrection, Body, ReferenceFrame, Vector3 )}.
 
<p> For functionality similar to that of the SPICE routine
DSKXV, see the method 
{@link #create( boolean, Body, Surface[], Time, ReferenceFrame,
Vector3[], Vector3[])}.
 

<p> Code examples are provided in the detailed documentation
for the constructors
and methods.



<h2>Files</h2>

<p>Appropriate SPICE kernels must be loaded by the calling program
   before methods of this class are called.

<p>The following data are required:

<ul>
<li>    DSK data:  DSK files containing topographic data
        for the target body must be loaded. If a surface list is
        specified, data for at least one of the listed surfaces must
        be loaded.
</li>
</ul>

<p>
   The following data may be required:
<ul>

<li>
        SPK data: ephemeris data for the positions of the centers 
        of DSK reference frames relative to the target body are 
        required if those frames are not centered at the target 
        body center. Typically
        ephemeris data are made available by loading one or more SPK
        files via {@link KernelDatabase#load}.
</li>
<li> PCK data: orientation data for the target body may be needed
     if a loaded DSK segment for the target uses a body-fixed
     frame other than the one designated by `fixref'.
        Typically these data are made available by loading a text
        PCK file via {@link KernelDatabase#load}.
</li>
<li>
        FK data: if the reference frame designated by `fixref'
        is not built in to the SPICE system, an FK specifying 
        this frame must be loaded. Typically
        frame data are made available by loading one or more FK
        files via {@link KernelDatabase#load}.
</li>

 


<li>
        CK data: the body-fixed frame to which `fixref'
        refers might be a CK frame. If so, at least one CK
        file will be needed to permit transformation of vectors
        between that frame and the J2000 frame.
</li>
<li>
        SCLK data: if a CK file is needed, an associated SCLK
        kernel is required to enable conversion between encoded SCLK
        (used to time-tag CK data) and barycentric dynamical time
        (TDB).
</li>
</ul>
<p>
   Kernel data are normally loaded once per program run, NOT every
   time a method of this class is called.







<h2> Particulars </h2>

<p>
This is the lowest-level public interface for computing 
ray-surface intercepts, where the target surface is modeled using 
topographic data provided by DSK files, and where
DSK segment selection is performed automatically. The highest-level 
interface for this purpose is the principal constructor
of {@link SurfaceIntercept}. Ray-surface
intercept computations can be performed using data from
individual, user-specified segments by calling the methods 
<pre>
   dskx**
</pre>
of class {@link CSPICE}. 

<p>
In cases where the data source information returned by this 
routine are not needed, the routine 
{@link #create( boolean, Body, Surface[], Time, ReferenceFrame,
Vector3[], Vector3[])} may be more suitable. 

<p>
This routine works with multiple DSK files. It places no 
restrictions on the data types or coordinate systems of the DSK 
segments used in the computation. DSK segments using different 
reference frames may be used in a single computation. The only 
restriction is that any pair of reference frames used directly or 
indirectly are related by a constant rotation. 

<p>
This routine enables calling applications to identify the source 
of the data defining the surface on which an intercept was found. 
The file, segment, and segment-specific information such as 
a DSK type 2 plate ID are returned. 

<p>
This routine can be used for improved efficiency in situations  
in which multiple ray-surface intercepts are to be performed 
using a constant ray vertex. 


<h3>Using DSK data</h3>

<p><b>DSK loading and unloading</b></p>

<p>
   DSK files providing data used by this class are loaded by
   calling {@link KernelDatabase#load} and can be unloaded by calling 
   {@link KernelDatabase#unload} or {@link KernelDatabase#clear} 
   See the documentation of KernelDatabase.load for limits 
   on numbers of loaded DSK files.

<p>
   For run-time efficiency, it's desirable to avoid frequent 
   loading and unloading of DSK files. When there is a reason to 
   use multiple versions of data for a given target body---for 
   example, if topographic data at varying resolutions are to be 
   used---the surface list can be used to select DSK data to be 
   used for a given computation. It is not necessary to unload 
   the data that are not to be used. This recommendation presumes 
   that DSKs containing different versions of surface data for a 
   given body have different surface ID codes. 


<p><b>DSk data priority</b></p>
 
<p>
   A DSK coverage overlap occurs when two segments in loaded DSK 
   files cover part or all of the same domain---for example, a 
   given longitude-latitude rectangle---and when the time 
   intervals of the segments overlap as well. 

<p>
   When DSK data selection is prioritized, in case of a coverage 
   overlap, if the two competing segments are in different DSK 
   files, the segment in the DSK file loaded last takes 
   precedence. If the two segments are in the same file, the 
   segment located closer to the end of the file takes 
   precedence. 

<p>
   When DSK data selection is unprioritized, data from competing 
   segments are combined. For example, if two competing segments 
   both represent a surface as sets of triangular plates, the 
   union of those sets of plates is considered to represent the 
   surface.  

<p>
   Currently only unprioritized data selection is supported. 
   Because prioritized data selection may be the default behavior 
   in a later version of the routine, the presence of the `pri' 
   argument in the principal constructor is required. 

<p><b>Round-off errors and mitigating algorithms</b></p>

<p>
   When topographic data are used to represent the surface of a 
   target body, round-off errors can produce some results that 
   may seem surprising. 

<p>
   Note that, since the surface in question might have mountains, 
   valleys, and cliffs, the points of intersection found for 
   nearly identical sets of inputs may be quite far apart from 
   each other: for example, a ray that hits a mountain side in a 
   nearly tangent fashion may, on a different host computer, be 
   found to miss the mountain and hit a valley floor much farther 
   from the observer, or even miss the target altogether. 

<p>
   Round-off errors can affect segment selection: for example, a 
   ray that is expected to intersect the target body's surface 
   near the boundary between two segments might hit either 
   segment, or neither of them; the result may be 
   platform-dependent. 

<p>
   A similar situation exists when a surface is modeled by a set 
   of triangular plates, and the ray is expected to intersect the 
   surface near a plate boundary. 

<p>
   To avoid having the intercept computation
   fail to find an intersection when 
   one clearly should exist, the computation uses two "greedy" 
   algorithms: 
<ol>
   <li> If the ray passes sufficiently close to any of the  
         boundary surfaces of a segment (for example, surfaces of 
         maximum and minimum longitude or latitude), that segment 
         is tested for an intersection of the ray with the 
         surface represented by the segment's data. 

    <p>  This choice prevents all of the segments from being 
         missed when at least one should be hit, but it could, on 
         rare occasions, cause an intersection to be found in a 
         segment other than the one that would be found if higher 
         precision arithmetic were used. 
    </li>
    <li> For type 2 segments, which represent surfaces as  
         sets of triangular plates, each plate is expanded very 
         slightly before a ray-plate intersection test is 
         performed. The default plate expansion factor is  
         <pre>
            1 + XFRACT 
         </pre>
         where XFRACT can be obtained from 
         {@link DSK#getTolerance(DSKToleranceKey)}.

     <p> For example, given a value for XFRACT of 1.e-10, the 
         sides of the plate are lengthened by 1/10 of a millimeter 
         per km. The expansion keeps the centroid of the plate 
         fixed. 

     <p> Plate expansion prevents all plates from being missed 
         in cases where clearly at least one should be hit. 

     <p> As with the greedy segment selection algorithm, plate 
         expansion can occasionally cause an intercept to be 
         found on a different plate than would be found if higher 
         precision arithmetic were used. It also can occasionally 
         cause an intersection to be found when the ray misses 
         the target by a very small distance.  
</li>
</ol>


<h3>Version 1.0.0 03-JAN-2017 (NJB)</h3>

*/
public class SurfaceInterceptWithDSKInfo extends SurfaceIntercept
{
   //
   // Private constants
   //

   //
   // The following maximum sizes for the info arrays returned by
   // CSPICE.dskxsi must be kept in sync with the values in the
   // Fortran INCLUDE file dsksrc.inc.
   //
   private static final int             DCSIZE = 1;
   private static final int             ICSIZE = 1;

   //
   // Fields
   //
   private DSK                          dsk;
   private DLADescriptor                dladsc;
   private DSKDescriptor                dskdsc;
   private int[]                        ic;
   private double[]                     dc;


   /**
   Class RayInputs is an inner class which supports creation of
   descriptive error messages containing inputs used to attempt to
   create a SurfaceIntercept instance via the SurfaceInterceptWithDSKInfo
   primary constructor.
   */
   class RayInputs extends Inputs
   {
      //
      // Saved DSKXSI-style inputs used for error message construction:
      //
      private boolean               prioritized;
      private Body                  target;
      private TDBTime               time;


      //
      // Constructors
      //
      RayInputs( boolean           prioritized,
                 Body              target,
                 Time              time        )

         throws SpiceException
      {
         this.prioritized  = prioritized;
         this.target       = new Body   ( target );
         this.time         = new TDBTime( time   );
      }

      /**
      No-arguments constructor.
      */
      RayInputs()
      {
      }



      //
      // RayInputs Methods
      // 

      //
      // Create a deep copy of a RayInputs instance.
      //
      RayInputs copy()

         throws SpiceException
      {
         RayInputs retval = new RayInputs( this.prioritized,
                                           this.target,
                                           this.time        );
         return ( retval );
      }


      //
      // Return a string representation of a RayInputs instance.
      //
      String getString()

         throws SpiceException
      {
         //
         // Generate display string.
         //
         String retString = new String( 
        
         "   prioritized  = " + prioritized                      + endl  +
         "   target       = " + target.getName()                 + endl  +
         "   time         = " + ( new TDBTime(time) ).toString() + endl    );
          
         return( retString );
      }
   }

 

   //
   // Constructors
   // 

   /**
   No-arguments constructor.
   */
   public SurfaceInterceptWithDSKInfo()
   {
      super();
   }


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public SurfaceInterceptWithDSKInfo( SurfaceInterceptWithDSKInfo surfx )

      throws SpiceException
   {
      super( surfx );

      this.dsk    = new DSK          ( surfx.dsk    );
      this.dladsc = new DLADescriptor( surfx.dladsc );
      this.dskdsc = new DSKDescriptor( surfx.dskdsc );

      dc = new double[DCSIZE];
      ic = new int   [ICSIZE];

      System.arraycopy( surfx.ic, 0, this.ic, 0, DCSIZE );
      System.arraycopy( surfx.dc, 0, this.dc, 0, ICSIZE );

      this.inputs = surfx.inputs.copy();
   }
   



   /**
   Construct a SurfaceInterceptWithDSKInfo instance representing
   a ray-DSK surface intercept.

<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<ol>
<li>  Compute surface intercepts of rays emanating from a set of 
      vertices distributed on a longitude-latitude grid. All 
      vertices are outside the target body, and all rays point 
      toward the target's center. 
<p>
      Check intercepts against expected values. Indicate the 
      number of errors, the number of computations, and the 
      number of intercepts found. 
<p>
      Use the meta-kernel shown below to load example SPICE 
      kernels. 

<pre>
KPL/MK

File: SurfaceInterceptWithDSKInfoEx1.tm

This meta-kernel is intended to support operation of SPICE
example programs. The kernels shown here should not be
assumed to contain adequate or correct versions of data
required by SPICE-based user applications.

In order for an application to use this meta-kernel, the
kernels referenced here must be present in the user's
current working directory.

The names and contents of the kernels referenced
by this meta-kernel are as follows:

   File name                        Contents
   ---------                        --------
   phobos512.bds                    DSK based on
                                    Gaskell ICQ Q=512
                                    plate model
\begindata

   PATH_SYMBOLS    = 'GEN'
   PATH_VALUES     = '/ftp/pub/naif/generic_kernels/dsk'

   KERNELS_TO_LOAD = ( '$GEN/phobos/phobos512.bds' )

\begintext


</pre>


<p> Example code begins here.

<pre>
//
// Program SurfaceInterceptWithDSKInfoEx1
//

//
// Multi-segment spear program.
//
// This program computes surface intercepts of rays emanating from a set of 
// vertices distributed on a longitude-latitude grid. All 
// vertices are outside the target body, and all rays point 
// toward the target's center. 
//
// The program checks intercepts against expected values. It reports the 
// number of errors, the number of computations, and the 
// number of intercepts found. 
//
// This program expects all loaded DSKs
// to represent the same body and surface.
//
//    Syntax: java -Djava.library.path=<JNISpice path> 
//                  SurfaceInterceptWithDSKInfoEx1 <meta-kernel>
//

import spice.basic.*;
import static spice.basic.AngularUnits.*;

public class SurfaceInterceptWithDSKInfoEx1
{
   //
   // Load SPICE shared library.
   //
   static{ System.loadLibrary( "JNISpice" ); }


   public static void main( String[] args )

      throws SpiceException
   {
      //
      // Local constants
      //
      final String                      META   = 
                                        "SurfaceInterceptWithDSKInfoEx1.tm";

       //
      // Local constants
      //
      final double                      DTOL   = 1.0e-14;
      final double                      SML    = 1.0e-12;

      final int                         MAXN   = 100000;      
      final int                         MAXSRF = 100;


      //
      // Local variables
      //
      Body                              target;

      DLADescriptor                     dladsc;
      DLADescriptor                     dladscInfo;

      DSK                               dsk;
   
      DSKDescriptor                     dskdsc;

      LatitudinalCoordinates            vtxLatCoords;
 
      ReferenceFrame                    fixref;

      String                            DSKName;
      String                            meta;

      Surface[]                         srflst;

      SurfaceInterceptWithDSKInfo       surfx;

      TDBTime                           et;
 
      Vector3[]                         dirArr;
      Vector3[]                         vtxArr;
      Vector3                           xxpt;

      boolean[]                         fndArr      = new boolean[1];
      boolean                           plidok;
      boolean                           prioritized = false;
 
      double                            d;
      double                            lat;
      double                            latstp;
      double                            lon;
      double                            lonstp;
      double                            polmrg;
      double                            r;      
      double[]                          timcov;
      double[]                          xptArr;
      double[]                          xxptArr = new double[3];

      int                               bodyid;
      int                               dtype;
      int                               framid;
      int                               handle;
      int                               i;
      int                               latix;
      int                               lonix;
      int                               nderr;
      int                               nhits;
      int                               nlat;
      int                               nlon;
      int                               nrays;
      int                               nstep;
      int                               nsurf;
      int                               plid    = 0;
      int[]                             plidArr = new int[1];
      int                               surfid;
      int                               xplid;


      try
      {
         //
         // Get meta-kernel name.
         //
         if ( args.length != 1 )
         {
            System.out.println( "Command syntax:  "                +
                                "SurfaceInterceptWithDSKInfoEx1 "  +
                                "<meta-kernel>"                      );
            return;
         }

         meta = args[0];

         //
         // Load kernels.
         //
         KernelDatabase.load( meta );

         //
         // Open the first (according to load order) loaded DSK,
         // then find the first segment and extract the body and 
         // surface IDs. 
         //
         DSKName = KernelDatabase.getFileName( 0, "DSK" );

         dsk     = DSK.openForRead( DSKName );

         dladsc  = dsk.beginForwardSearch();
         
         dskdsc  = dsk.getDSKDescriptor( dladsc );

         bodyid  = dskdsc.getCenterID();
         target  = new Body( bodyid );

         surfid  = dskdsc.getSurfaceID();

         framid  = dskdsc.getFrameID();
         fixref  = new ReferenceFrame( framid );

         //
         // Set the DSK data look-up time to the midpoint of 
         // the time coverage of the segment we just looked up.
         //
         timcov = dskdsc.getTimeBounds();

         et = new TDBTime(  ( timcov[0] + timcov[1] ) / 2  ); 

         //
         // Set the magnitude of the ray vertices. Use a large
         // number to ensure the vertices are outside of
         // any realistic target.
         //
         r = 1.0e10;

         //
         // Spear the target with rays pointing toward
         // the origin.  Use a grid of ray vertices
         // located on a sphere enclosing the target.
         //
         // The variable `polmrg' ("pole margin") can
         // be set to a small positive value to reduce
         // the number of intercepts done at the poles.
         // This may speed up the computation for
         // the multi-segment case, since rays parallel
         // to the Z axis will cause all segments converging
         // at the pole of interest to be tested for an
         // intersection.
         //    

         polmrg =    0.5 * RPD;
         latstp =    1.0;
         lonstp =    2.0;
         nlat   =    (int)( (180.0 + SML) / latstp )  +  1;
         nlon   =    (int)( (360.0 + SML) / lonstp );

         nhits  =    0;
         nderr  =    0;

         lon    = -180.0;
         lat    =   90.0;
         lonix  =    0;
         latix  =    0;
         nrays  =    nlat * nlon;
         
         vtxArr = new Vector3[nrays];
         dirArr = new Vector3[nrays];

         //
         // Generate rays. 
         //

         i = 0;
        
         while ( lonix < nlon )
         {
            lon = ( lonix * lonstp ) + ( 0.5 * DPR );

            while ( latix < nlat )
            {
               if ( lonix == 0 )
               {
                  lat = 90.0 - latix*latstp;
               }
               else
               {
                  if ( latix == 0 )
                  {
                     lat =  90.0 - polmrg;
                  }
                  else if ( latix == nlat-1 )
                  {
                     lat = -90.0 + polmrg;
                  }
                  else
                  {
                     lat =  90.0 - latix*latstp;
                  }
               }

               vtxLatCoords = 

                  new LatitudinalCoordinates( r, lon*RPD, lat*RPD );

               vtxArr[i] = vtxLatCoords.toRectangular();

               dirArr[i] = vtxArr[i].negate();
 
               ++ i;   
               ++ latix;      
            }

            ++lonix;

            latix  = 0;
         }

         //
         // Assign surface ID list. 
         //
         // We assume all DSK files referenced in the meta-kernel have the 
         // same body and surface IDs. We'll create a trivial surface list
         // containing the surface from the first segment of the first 
         // loaded DSK file. 
         //
         // We could check this using the DSK coverage routines,
         // but for brevity, we won't do so here.
         //
         nsurf     = 1;
         srflst    = new Surface[nsurf];
         srflst[0] = new Surface( surfid, target );

         System.out.println ( "Computing intercepts..." );

         for ( i = 0;  i < nrays;  i++ )
         {
            //
            // Compute the surface intercept.
            //
            surfx = new SurfaceInterceptWithDSKInfo( prioritized,  target, 
                                                     srflst,       et,
                                                     fixref,       vtxArr[i],
                                                     dirArr[i]               );
            //
            // Check results.
            //
            // Compare the intercept that found by using the low-level
            // intercept interface.
            //

            if ( surfx.wasFound() )
            {
               //
               // Record the fact that a new intercept was found.
               //
               ++ nhits;
 
               //
               // Extract the DLA descriptor and DSK handle from
               // the "info" fields of the intercept instance.
               //
               dladscInfo = surfx.getDLADescriptor();

               handle     = surfx.getDSK().getHandle();

               CSPICE.dskx02( handle,              dladscInfo.toArray(), 
                              vtxArr[i].toArray(), dirArr[i].toArray(),
                              plidArr,             xxptArr,
                              fndArr                                     );
               //
               // Compute the distance between the intercept and the
               // point having latitude and longitude of the ray's vertex,
               // and radius equal to the radius of the intercept.
               //
               xxpt = new Vector3( xxptArr );
               d    = surfx.getIntercept().dist( xxpt );

               //
               // If the intercept is on a surface provided by a type 2
               // DSK segment, check the intercept plate ID against the
               // expected ID. This check is valid only if the intercept
               // does not occur on a plate edge.
               // 
              
               plidok = true;

               dtype  = surfx.getDSKDescriptor().getDataType();

               if ( dtype == 2 )
               {
                  plid   = surfx.getIntComponent()[0];

                  plidok = ( plid == plidArr[0] );
               }
                              
               
               if (  ( d/r  >  DTOL )  ||  ( !plidok )  )
               {
                  vtxLatCoords = new LatitudinalCoordinates( vtxArr[i] );

                  lon    = vtxLatCoords.getLongitude() * DPR;
                  lat    = vtxLatCoords.getLatitude()  * DPR;


                  xptArr = surfx.getIntercept().toArray();

                  System.out.format ( "===========================%n" );
                  System.out.format ( "Vertex lon = %f, lat = %f%n",
                           lon, lat                        );
                  System.out.format ( "Bad intercept%n"               );
                  System.out.format ( "Distance error = %e%n", d      );

                  if ( dtype == 2 )
                  {
                     System.out.format ( "Plate ID mismatch: intercept " +
                                         "may be on a plate edge. "      +
                                         "Expected plate ID = %d; "      +
                                         "actual ID = %d%n",
                                         plidArr[0], plid                 );
                  }

                  System.out.format ( "surfx   = (%e %e %e)%n",
                           xptArr[0], xptArr[1], xptArr[2] );
                  System.out.format ( "xxptArr = (%e %e %e)%n",
                           xxptArr[0], xxptArr[1], xxptArr[2] );

                  ++ nderr;

                  return;
               }
            }
            else
            {
               //
               // Missing the target entirely is a fatal error.
               //
               // This is true only for this program, not in
               // general. For example, if the target shape is
               // a torus, many rays would miss the target.

               vtxLatCoords = new LatitudinalCoordinates( vtxArr[i] );

               lon    = vtxLatCoords.getLongitude() * DPR;
               lat    = vtxLatCoords.getLatitude()  * DPR;
         
               System.out.format ( "===========================%n" );
               System.out.format ( "Vertex lon = %f; lat = %f%n",
                        lon, lat                                   );
               System.out.format ( "No intercept%n"                );
               return;
            }
         }
       
         System.out.println( "Done." );

         System.out.format( "nrays = %d%n", nrays );
         System.out.format( "nhits = %d%n", nhits );
         System.out.format( "nderr = %d%n", nderr );


      } // End of try block

      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }

   } // End of main method 
   
}

</pre>


<p> When this program was executed on a PC/Linux/gcc/64-bit/java 1.5
platform, the output was:

<pre>
Computing intercepts...
Done.
nrays = 32580
nhits = 32580
nderr = 0
</pre>

</li>
</ol>






   */
   public SurfaceInterceptWithDSKInfo ( boolean         prioritized,
                                        Body            target,
                                        Surface[]       surfList,
                                        Time            t,
                                        ReferenceFrame  fixref, 
                                        Vector3         vertex,
                                        Vector3         raydir       )
      throws SpiceException 
   {
      //
      // Create the SurfaceIntercept component of this instance.
      //
      super();


      //
      // Declare local variables. Reserve memory for items having
      // known sizes.
      //
      boolean[]                         foundArr  = new boolean[1];

      int[]                             dladscArr = new int[ DLADSZ ];
      int[]                             handleArr = new int[ 1 ];
      int                               i;
      int[]                             icArr     = new int[ ICSIZE ];  
      int                               nsurf;
      int[]                             srflstArr;
      double[]                          dcArr     = new double[ DCSIZE ];
      double[]                          dskdscArr = new double[ DSKDSZ ];
      double[]                          srfvec    = new double[ 3 ];
      double[]                          xpt       = new double[ 3 ];
       

      //
      // Create an array of integer Surface ID codes. The list may be
      // empty.
      //      
      nsurf     = surfList.length;

      srflstArr = new int[nsurf];

      for ( i = 0;  i < nsurf;  i++ )
      {
         srflstArr[i] = surfList[i].getIDCode();   
      }

      //
      // Compute the intercept.
      // 
      CSPICE.dskxsi( prioritized,      target.getName(),  nsurf, 
                     srflstArr,        t.getTDBSeconds(), fixref.getName(), 
                     vertex.toArray(), raydir.toArray(),  DCSIZE,
                     ICSIZE,           xpt,               handleArr,
                     dladscArr,        dskdscArr,         dcArr,
                     icArr,            foundArr                            );
                 
      //
      // Store the "found flag" for this computation.
      // 
      this.wasFound = foundArr[0];

      if ( wasFound )
      {
         //
         // Store the intercept point. 
         // 
         this.surfaceIntercept = new Vector3( xpt );

         //
         // Create the surface vector of the superclass.
         //
         this.surfaceVector = (new Vector3(xpt)).sub( vertex );
 
         //
         // Store the input epoch; this is the same as the target
         // epoch for this computation, since no aberration
         // corrections are performed.
         //
         this.targetEpoch = new TDBTime( t );


         //
         // Now store the source information.
         //         
         this.dsk    = new DSK(  new DAS(handleArr[0])  );

         this.dladsc = new DLADescriptor( dladscArr );
         this.dskdsc = new DSKDescriptor( dskdscArr );

         this.dc     = dcArr;
         this.ic     = icArr;         
     }

     //
     // Store input information for error message construction.
     //
     this.inputs = new RayInputs( prioritized, target, t );
   }


   //
   // Methods
   //

   /**
   Get the DSK instance from a SurfaceInterceptWithDSKInfo instance.
   This method returns a deep copy.
   */
   public DSK getDSK()

      throws PointNotFoundException, SpiceException
   {
      if ( !wasFound )
      {
         throwNotFoundExc();
      }

      return (  new DSK( this.dsk )  );
   }


   /**
   Get the DLA Descriptor from a SurfaceInterceptWithDSKInfo instance.
   This method returns a deep copy.
   */
   public DLADescriptor getDLADescriptor()

      throws PointNotFoundException, SpiceException
   {
      if ( !wasFound )
      {
         throwNotFoundExc();
      }

      return (  new DLADescriptor( this.dladsc )  );
   }


   /**
   Get the DSK Descriptor from a SurfaceInterceptWithDSKInfo instance.
   This method returns a deep copy.
   */
   public DSKDescriptor getDSKDescriptor()

      throws PointNotFoundException, SpiceException
   {
      if ( !wasFound )
      {
         throwNotFoundExc();
      }

      return (  new DSKDescriptor( this.dskdsc )  );
   }

  
   /**
   Get the integer information component from a SurfaceInterceptWithDSKInfo 
   instance. This method returns a deep copy.

   <p> For DSK type 2 segments, element 0 of the returned array
   contains the ID of the plate on which the intercept was
   found. This is the only element of the returned array.
   */
   public int[] getIntComponent()

      throws PointNotFoundException, SpiceException
   {
      if ( !wasFound )
      {
         throwNotFoundExc();
      }

      int[] retval = new int[ICSIZE];

      System.arraycopy( this.ic, 0, retval, 0, ICSIZE );

      return ( retval );
   }


   /**
   Get the double information component from a SurfaceInterceptWithDSKInfo 
   instance. This method returns a deep copy.

   <p> For DSK type 2 segments, the returned array contains no meaningful
   data.
 
   */
   public double[] getDoubleComponent()

      throws PointNotFoundException, SpiceException
   {
      if ( !wasFound )
      {
         throwNotFoundExc();
      }

      double[] retval = new double[DCSIZE];

      System.arraycopy( this.dc, 0, retval, 0, DCSIZE );

      return ( retval );
   }

}





