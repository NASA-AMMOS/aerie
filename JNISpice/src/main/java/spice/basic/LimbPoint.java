
package spice.basic;

/**
Class LimbPoint supports limb point computations.

<p> LimbPoint instances consist of
<ul>
<li> An inherited {@link Vector3} instance representing a limb point. </li>

<li> The epoch of participation of the target body. 

<p>  This is
  the observation epoch, minus the approximate one-way
  light time from the limb point to the observer, if aberration
  corrections are used. The way the light time is computed
  depends on the choice of aberration correction locus.
</li>

<li> A vector from the observer to the limb point, expressed
  in the target body-fixed reference frame, evaluated at the
  epoch of participation of the target body.
</li>
</ul>
</pre>

<p> The principal computational method of this class is
{@link #create(String,Body,Time,ReferenceFrame,
AberrationCorrection,String,Body,Vector3,double,
int,double,double,int)}.
See the detailed documentation of this method 
for code examples.



<h2>Files</h2>

<p>Appropriate SPICE kernels must be loaded by the calling program
   before methods of this class are called.

<p>The following data are required:

<ul>
<li>
        SPK data: the calling application must load ephemeris data
        for the target and observer. If aberration
        corrections are used, the states of the target body and of
        the observer relative to the solar system barycenter must be
        calculable from the available ephemeris data. Typically
        ephemeris data are made available by loading one or more SPK
        files via {@link KernelDatabase#load}.
</li>

<li>
        Target body orientation data: these may be provided in a text
        or binary PCK file. In some cases, target body orientation
        may be provided by one more more CK files. In either case,
        data are made available by loading the files via KernelDatabase.load.
</li>
<li>    Shape data for the target body:
        <pre>
   PCK data:
 
      If the target body shape is modeled as an ellipsoid,
      triaxial radii for the target body must be loaded into
      the kernel pool. Typically this is done by loading a
      text PCK file via KernelDatabase.load.
 
      Triaxial radii are also needed if the target shape is
      modeled by DSK data but one or both of the GUIDED limb
      definition method or the ELLIPSOID LIMB aberration
      correction locus are selected.
 
   DSK data:
 
      If the target shape is modeled by DSK data, DSK files
      containing topographic data for the target body must be
      loaded. If a surface list is specified, data for at
      least one of the listed surfaces must be loaded.
</pre>
</ul>
<p>    The following data may be required:
<ul>
<li>       Frame data: if a frame definition is required to convert the
           observer and target states to the body-fixed frame of the
           target, that definition must be available in the kernel
           pool. Typically the definition is supplied by loading a
           frame kernel via KernelDatabase.load.
</li>
<li>       Surface name-ID associations: if surface names are specified
           in `method', the association of these names with their
           corresponding surface ID codes must be established by
           assignments of the kernel variables
<pre>
   NAIF_SURFACE_NAME
   NAIF_SURFACE_CODE
   NAIF_SURFACE_BODY
</pre>
<p>        Normally these associations are made by loading a text
           kernel containing the necessary assignments. An example
           of such a set of assignments is
<pre>
   NAIF_SURFACE_NAME += 'Mars MEGDR 128 PIXEL/DEG'
   NAIF_SURFACE_CODE += 1
   NAIF_SURFACE_BODY += 499
</pre>
<li>
           SCLK data: if the target body's orientation is provided by
           CK files, an associated SCLK kernel must be loaded.
</li>
</ul>
 
<p>
   Kernel data are normally loaded once per program run, NOT every
   time a method of this class is called.



<h2> Class LimbPoint Particulars </h2>

<h3>Using DSK data</h3>

 
<p><b>DSK loading and unloading</b>
 
<p>DSK files providing data used by this class are loaded by 
      calling {@link KernelDatabase#load} and can be unloaded by 
      calling {@link KernelDatabase#unload} or
      {@link KernelDatabase#clear}. See the documentation of 
      KernelDatabase.load for limits on numbers 
      of loaded DSK files. 
 
      For run-time efficiency, it's desirable to avoid frequent 
      loading and unloading of DSK files. When there is a reason to 
      use multiple versions of data for a given target body---for 
      example, if topographic data at varying resolutions are to be 
      used---the surface list can be used to select DSK data to be 
      used for a given computation. It is not necessary to unload 
      the data that are not to be used. This recommendation presumes 
      that DSKs containing different versions of surface data for a 
      given body have different surface ID codes. 
 
 
<p><b>DSK data priority</b>

 
<p>   A DSK coverage overlap occurs when two segments in loaded DSK 
      files cover part or all of the same domain---for example, a 
      given longitude-latitude rectangle---and when the time 
      intervals of the segments overlap as well. 
 
<p>   When DSK data selection is prioritized, in case of a coverage 
      overlap, if the two competing segments are in different DSK 
      files, the segment in the DSK file loaded last takes 
      precedence. If the two segments are in the same file, the 
      segment located closer to the end of the file takes 
      precedence. 
 
<p>   When DSK data selection is unprioritized, data from competing 
      segments are combined. For example, if two competing segments 
      both represent a surface as a set of triangular plates, the 
      union of those sets of plates is considered to represent the 
      surface.  
 
<p>   Currently only unprioritized data selection is supported. 
      Because prioritized data selection may be the default behavior 
      in a later version of the routine, the UNPRIORITIZED keyword is 
      required in the `method' argument of the method `create'. 


<h2> Version and Date </h2>

<h3> Version 1.0.0 11-JAN-2017 (NJB) </h3>

*/
public class LimbPoint extends Vector3
{
   //
   // Public Constants
   //

   //
   // Fields
   //
   private TDBTime          targetEpoch;
   private Vector3          tangentVector;


   //
   // Constructors
   //

   /**
   Create a limb point from a surface point, epoch, and
   observer-to limb point vector.
   */
   public LimbPoint ( Vector3      limbPoint,
                      Time         targetEpoch,
                      Vector3      tangentVector )

      throws SpiceException
   {
      super( limbPoint );

      this.targetEpoch   = new TDBTime( targetEpoch   );
      this.tangentVector = new Vector3( tangentVector );
   }


   /**
   Copy constructor.
   */
   public LimbPoint ( LimbPoint   lpoint )

      throws SpiceException
   {
      super( lpoint );

      this.targetEpoch   = new TDBTime( lpoint.targetEpoch   );
      this.tangentVector = new Vector3( lpoint.tangentVector );
   }


   /**
   No-arguments constructor.
   */
   public LimbPoint ()
   {
      super();
   }


   //
   // Methods
   //
 
   /**
   Create an array of limb points on a specified target body, 
   as seen from a specified observer.

   <p> This is the principal method for constructing a representation
   of a limb.

   <p> In the returned two-dimensional array, the ith row contains the
   limb points for the ith cutting half-plane. The rows do not 
   necessarily have equal length. 
   
   <h2> Inputs </h2>
   
<pre>
   method   is a String instance providing parameters defining 
            the computation method to be used. In the syntax 
            descriptions below, items delimited by brackets 
            are optional. 
 
            `method' may be assigned the following values: 
 
              "TANGENT/DSK/UNPRIORITIZED[/SURFACES = <surface list>]" 
 
                  The limb point computation uses topographic data 
                  provided by DSK files (abbreviated as "DSK data" 
                  below) to model the surface of the target body. A 
                  limb point is defined as the point of tangency, on 
                  the surface represented by the DSK data, of a ray 
                  emanating from the observer. 
 
                  Limb points are generated within a specified set 
                  of "cutting" half-planes that have as an edge the 
                  line containing the observer-target vector. 
                  Multiple limb points may be found within a given 
                  half-plane, if the target body shape allows for 
                  this. 
 
                  The surface list specification is optional. The 
                  syntax of the list is 
 
                     &#60surface 1&#62 [, &#60surface 2&#62...] 
 
                  If present, it indicates that data only for the 
                  listed surfaces are to be used; however, data need 
                  not be available for all surfaces in the list. If 
                  the list is absent, loaded DSK data for any 
                  surface associated with the target body are used. 
 
                  The surface list may contain surface names or 
                  surface ID codes. Names containing blanks must 
                  be delimited by double quotes, for example 
 
                     SURFACES = \"Mars MEGDR 128 PIXEL/DEG\" 
 
                  If multiple surfaces are specified, their names 
                  or IDs must be separated by commas. 
 
                  See the Particulars section below for details 
                  concerning use of DSK data. 
 
                  This is the highest-accuracy method supported by 
                  this subroutine. It generally executes much more 
                  slowly than the "GUIDED" method described below. 
                   
                   
              "GUIDED/DSK/UNPRIORITIZED[/SURFACES = &#60surface list&#62]" 
 
                  This method uses DSK data as described above, but 
                  limb points generated by this method are "guided" 
                  so as to lie in the limb plane of the target 
                  body's reference ellipsoid, on the target body's 
                  surface. This method produces a unique limb point 
                  for each cutting half-plane. If multiple limb 
                  point candidates lie in a given cutting 
                  half-plane, the outermost one is chosen. 
 
                  This method may be used only with the "CENTER" 
                  aberration correction locus (see the description 
                  of `refloc' below). 
 
                  Limb points generated by this method are 
                  approximations; they are generally not true 
                  ray-surface tangent points. However, these 
                  approximations can be generated much more quickly 
                  than tangent points. 
 
 
              "TANGENT/ELLIPSOID" 
              "GUIDED/ELLIPSOID" 
 
                  Both of these methods generate limb points on the 
                  target body's reference ellipsoid. The "TANGENT" 
                  option may be used with any aberration correction 
                  locus, while the "GUIDED" option may be used only 
                  with the "CENTER" locus (see the description of 
                  `refloc' below).  
 
                  When the locus is set to "CENTER", these methods 
                  produce the same results. 
 
 
               Neither case nor white space are significant in 
               `method', except within double-quoted strings. For 
               example, the string " eLLipsoid/tAnGenT " is valid. 
 
               Within double-quoted strings, blank characters are 
               significant, but multiple consecutive blanks are 
               considered equivalent to a single blank. Case is  
               not significant. So 
 
                  \"Mars MEGDR 128 PIXEL/DEG\" 
 
               is equivalent to  
 
                  \" mars megdr  128  pixel/deg \" 
 
               but not to 
 
                  \"MARS MEGDR128PIXEL/DEG\" 
 
                
   target      is a {@link Body} instance identifying the target body. The 
               target body is an extended ephemeris object. 
  
               When the target body's surface is represented by a 
               tri-axial ellipsoid, this routine assumes that a 
               kernel variable representing the ellipsoid's radii is 
               present in the kernel pool. Normally the kernel 
               variable would be defined by loading a PCK file. 
 
 
   et          is a {@link Time} instance representing the epoch of 
               participation of the observer: `et' is the epoch at 
               which the observer's state is computed. 
 
               When aberration corrections are not used, `et' is also 
               the epoch at which the position and orientation of 
               the target body are computed. 
 
               When aberration corrections are used, the position 
               and orientation of the target body are computed at 
               et-lt, where lt is the one-way light time between the 
               aberration correction locus and the observer. The 
               locus is specified by the input argument `corloc'. 
               See the descriptions of `abcorr' and `corloc' below for 
               details. 
 
 
   fixref      is a {@link ReferenceFrame} instance representing
               a body-fixed reference frame centered 
               on the target body. `fixref' may be any such frame 
               supported by the SPICE system, including built-in 
               frames (documented in the Frames Required Reading) 
               and frames defined by a loaded frame kernel (FK).  
 
               The output limb points and observer-target tangent vectors
               in the returned LimbPoint array are expressed relative 
               to this reference frame. 
 
 
   abcorr      is an {@link AberrationCorrection} instance that
               indicates the aberration corrections to be applied 
               when computing the target's position and orientation. 
               Corrections are applied at the location specified by 
               the aberration correction locus argument `corloc', 
               which is described below. 
 
               For remote sensing applications, where apparent limb 
               points seen by the observer are desired, normally 
               either of the corrections 
             
                  "LT+S"  
                  "CN+S" 
    
               should be used. The correction "NONE" may be suitable 
               for cases in which the target is very small and the 
               observer is close to, and has small velocity relative 
               to, the target (e.g. comet Churyumov-Gerasimenko and 
               the Rosetta Orbiter). 
 
               These and the other supported options are described 
               below. `abcorr' may be any of the following: 
 
                  "NONE"     Apply no correction. Return the 
                             geometric limb points on the target 
                             body. 
 
               Let `lt' represent the one-way light time between the 
               observer and the aberration correction locus. The 
               following values of `abcorr' apply to the "reception" 
               case in which photons depart from the locus at the 
               light-time corrected epoch et-lt and *arrive* at the 
               observer's location at `et': 
 
 
                  "LT"       Correct for one-way light time (also 
                             called "planetary aberration") using a 
                             Newtonian formulation. This correction 
                             yields the locus at the moment it 
                             emitted photons arriving at the 
                             observer at `et'. 
  
                             The light time correction uses an 
                             iterative solution of the light time 
                             equation. The solution invoked by the 
                             "LT" option uses one iteration. 
 
                             Both the target position as seen by the 
                             observer, and rotation of the target 
                             body, are corrected for light time. 
 
                  "LT+S"     Correct for one-way light time and 
                             stellar aberration using a Newtonian 
                             formulation. This option modifies the 
                             locus obtained with the "LT" option to 
                             account for the observer's velocity 
                             relative to the solar system 
                             barycenter. These corrections yield 
                             points on the apparent limb. 
 
                  "CN"       Converged Newtonian light time 
                             correction. In solving the light time 
                             equation, the "CN" correction iterates 
                             until the solution converges. Both the 
                             position and rotation of the target 
                             body are corrected for light time. 
 
                  "CN+S"     Converged Newtonian light time and 
                             stellar aberration corrections. This 
                             option produces a solution that is at 
                             least as accurate at that obtainable 
                             with the "LT+S" option. Whether the 
                             "CN+S" solution is substantially more 
                             accurate depends on the geometry of the 
                             participating objects and on the 
                             accuracy of the input data. In all 
                             cases this routine will execute more 
                             slowly when a converged solution is 
                             computed. 
 
 
   corloc      is a String specifying the aberration correction 
               locus: the point or set of points for which 
               aberration corrections are performed. `corloc' may be 
               assigned the values: 
 
                  "CENTER"  
 
                      Light time and stellar aberration corrections 
                      are applied to the vector from the observer to 
                      the center of the target body. The one way 
                      light time from the target center to the 
                      observer is used to determine the epoch at 
                      which the target body orientation is computed. 
 
                      This choice is appropriate for small target 
                      objects for which the light time from the 
                      surface to the observer varies little across 
                      the entire target. It may also be appropriate 
                      for large, nearly ellipsoidal targets when the 
                      observer is very far from the target. 
 
                      Computation speed for this option is faster 
                      than for the "ELLIPSOID LIMB" option. 
 
                  "ELLIPSOID LIMB" 
 
                      Light time and stellar aberration corrections 
                      are applied to individual limb points on the 
                      reference ellipsoid. For a limb point on the 
                      surface described by topographic data, lying 
                      in a specified cutting half-plane, the unique 
                      reference ellipsoid limb point in the same 
                      half-plane is used as the locus of the 
                      aberration corrections. 
 
                      This choice is appropriate for large target 
                      objects for which the light time from the limb 
                      to the observer is significantly different 
                      from the light time from the target center to 
                      the observer. 
 
                      Because aberration corrections are repeated for 
                      individual limb points, computational speed for 
                      this option is relatively slow. 
 
 
   obsrvr      is a {@link Body} instance identifying
               the observing body. The observing body 
               is an ephemeris object: it typically is a spacecraft, 
               the earth, or a surface point on the earth.  
 
 
   refvec, 
   rolstp, 
   ncuts       are, respectively, a reference vector, a roll step 
               angle, and a count of cutting half-planes. 
 
               `refvec' is a {@link Vector3} instance that
               defines the first of a sequence of cutting 
               half-planes in which limb points are to be found. 
               Each cutting half-plane has as its edge the line 
               containing the observer-target vector; the first 
               half-plane contains `refvec'. 
 
               `refvec' is expressed in the body-fixed reference frame 
               designated by `fixref'. 
 
               `rolstp' is an angular step by which to roll the 
               cutting half-planes about the observer-target vector. 
               The first half-plane is aligned with `refvec'; the ith 
               half-plane is rotated from `refvec' about the 
               observer-target vector in the counter-clockwise 
               direction by (i-1)*rolstp. Units are radians. 
               `rolstp' should be set to  
 
                  2*pi/ncuts  
 
               to generate an approximately uniform distribution of 
               limb points along the limb. 
 
               `ncuts' is the number of cutting half-planes used to 
               find limb points; the angular positions of 
               consecutive half-planes increase in the positive 
               sense (counterclockwise) about the target-observer 
               vector and are distributed roughly equally about that 
               vector: each half-plane has angular separation of 
               approximately 
 
                  `rolstp' radians 
 
               from each of its neighbors. When the aberration 
               correction locus is set to "CENTER", the angular 
               separation is the value above, up to round-off. When 
               the locus is "ELLIPSOID LIMB", the separations are 
               less uniform due to differences in the aberration 
               corrections used for the respective limb points. 
 
 
   schstp, 
   soltol      are used only for DSK-based surfaces. These inputs
               are, respectively, the search angular step size and 
               solution convergence tolerance used to find tangent 
               rays and associated limb points within each cutting 
               half plane. These values are used when the `method' 
               argument includes the "TANGENT" option. In this case, 
               limb points are found by a two-step search process: 
 
                  1) Bracketing: starting with the direction 
                     opposite the observer-target vector, rays 
                     emanating from the observer are generated 
                     within the half-plane at successively greater 
                     angular separations from the initial direction, 
                     where the increment of angular separation is 
                     `schstp'. The rays are tested for intersection 
                     with the target surface. When a transition 
                     between non-intersection to intersection is 
                     found, the angular separation of a tangent ray 
                     has been bracketed. 
 
                  2) Root finding: each time a tangent ray is  
                     bracketed, a search is done to find the angular 
                     separation from the starting direction at which 
                     a tangent ray exists. The search terminates 
                     when successive rays are separated by no more 
                     than `soltol'. When the search converges, the 
                     last ray-surface intersection point found in 
                     the convergence process is considered to be a 
                     limb point. 
                    
    
                `schstp' and `soltol' have units of radians. 
 
                Target bodies with simple surfaces---for example, 
                convex shapes---will have a single limb point within 
                each cutting half-plane. For such surfaces, `schstp' 
                can be set large enough so that only one bracketing 
                step is taken. A value greater than pi, for example 
                4.0, is recommended. 
 
                Target bodies with complex surfaces can have 
                multiple limb points within a given cutting 
                half-plane. To find all limb points, `schstp' must be 
                set to a value smaller than the angular separation 
                of any two limb points in any cutting half-plane, 
                where the vertex of the angle is the observer. 
                `schstp' must not be too small, or the search will be 
                excessively slow. 
 
                For both kinds of surfaces, `soltol' must be chosen so 
                that the results will have the desired precision. 
                Note that the choice of `soltol' required to meet a 
                specified bound on limb point height errors depends 
                on the observer-target distance. 
 
 
   maxn         is the maximum number of limb points that can be 
                stored in the output array `points'.  
</pre>


   <h2> Output </h2>

<pre>
                The returned {@link LimbPoint} array contains
                the limb points found by this routine. The sets of limb 
                points associated with the ith half-plane is contained
                in the ith row of the returned array. The rows need not
                have equal length. 

                The limb points in a given half-plane are ordered by 
                decreasing angular separation from the observer-target 
                direction; the outermost limb point in a given half-plane 
                is the first of that set. 
 
                The limb points for the half-plane containing `refvec' 
                occupy the first row of the output array 
 
                Limb points are expressed in the reference frame 
                designated by `fixref'. For each limb point, the 
                orientation of the frame is evaluated at the epoch 
                corresponding to the limb point.
</pre>


<h2> Method `create' Particulars </h2>

<p><b> Syntax of the `method' input argument   </b>
     
<p> 
      The keywords and surface list in the `method' argument 
      of `create' are called "clauses." The clauses may appear in any 
      order, for example 
<pre> 
         TANGENT/DSK/UNPRIORITIZED/&#60surface list&#62
         DSK/TANGENT/&#60surface list&#62/UNPRIORITIZED 
         UNPRIORITIZED/&#60surface list&#62/DSK/TANGENT 
</pre> 
      The simplest form of the `method' argument specifying use of 
      DSK data is one that lacks a surface list, for example: 
<pre>
         "TANGENT/DSK/UNPRIORITIZED" 
         "GUIDED/DSK/UNPRIORITIZED" 
</pre>
      For applications in which all loaded DSK data for the target 
      body are for a single surface, and there are no competing 
      segments, the above strings suffice. This is expected to be 
      the usual case. 
<p>
      When, for the specified target body, there are loaded DSK 
      files providing data for multiple surfaces for that body, the 
      surfaces to be used by this routine for a given call must be 
      specified in a surface list, unless data from all of the 
      surfaces are to be used together. 
<p>
      The surface list consists of the string 
<pre>
         SURFACES = 
</pre>
      followed by a comma-separated list of one or more surface 
      identifiers. The identifiers may be names or integer codes in 
      string format. For example, suppose we have the surface 
      names and corresponding ID codes shown below: 
<pre> 
         Surface Name                              ID code 
         ------------                              ------- 
         "Mars MEGDR 128 PIXEL/DEG"                1 
         "Mars MEGDR 64 PIXEL/DEG"                 2 
         "Mars_MRO_HIRISE"                         3 
</pre>
      If data for all of the above surfaces are loaded, then 
      data for surface 1 can be specified by either 
<pre>
         "SURFACES = 1" 
</pre>
      or 
<pre>
         "SURFACES = \"Mars MEGDR 128 PIXEL/DEG\"" 
</pre>
      Double quotes are used to delimit the surface name because 
      it contains blank characters.  
<p>    
      To use data for surfaces 2 and 3 together, any 
      of the following surface lists could be used: 
<pre>
         "SURFACES = 2, 3" 
 
         "SURFACES = \"Mars MEGDR  64 PIXEL/DEG\", 3" 
 
         "SURFACES = 2, Mars_MRO_HIRISE" 
 
         "SURFACES = \"Mars MEGDR 64 PIXEL/DEG\", Mars_MRO_HIRISE" 
</pre>   
      An example of a `method' argument that could be constructed 
      using one of the surface lists above is 
<pre>
      "NADIR/DSK/UNPRIORITIZED/SURFACES= \"Mars MEGDR 64 PIXEL/DEG\",3" 
</pre>








   <h2>Code Examples</h2>

<p> 
   The numerical results shown for these examples may differ across 
   platforms. The results depend on the SPICE kernels used as 
   input, the compiler and supporting libraries, and the machine  
   specific arithmetic implementation.  
 
 
   <ol>    

   <li> Find apparent limb points on Phobos as seen from Mars.

 
<p>   Due to Phobos' irregular shape, the TANGENT limb point 
      definition will used. It suffices to compute light time and 
      stellar aberration corrections for the center of Phobos, so 
      the "CENTER" aberration correction locus will be used. Use 
      converged Newtonian light time and stellar aberration 
      corrections in order to model the apparent position and  
      orientation of Phobos. 
       
<p>   For comparison, compute limb points using both ellipsoid 
      and topographic shape models. 
 
<p>   Use the target body-fixed +Z axis as the reference direction 
      for generating cutting half-planes. This choice enables the 
      user to see whether the first limb point is near the target's 
      north pole. 
 
<p>   For each option, use just three cutting half-planes, in order 
      to keep the volume of output manageable. In most applications, 
      the number of cuts and the number of resulting limb points 
      would be much greater. 
 
 
   </li>

<p>  Use the meta-kernel shown below to load the required SPICE
     kernels.

<pre>
KPL/MK

File: LimbPointEx1.tm

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
  de430.bsp                        Planetary ephemeris
  mar097.bsp                       Mars satellite ephemeris
  pck00010.tpc                     Planet orientation and
                                   radii
  naif0012.tls                     Leapseconds
  phobos512.bds                    DSK based on
                                   Gaskell ICQ Q=512
                                   Phobos plate model
\begindata

  PATH_SYMBOLS    = 'GEN'
  PATH_VALUES     = '/ftp/pub/naif/generic_kernels'

  KERNELS_TO_LOAD = ( 'de430.bsp',
                      'mar097.bsp',
                      'pck00010.tpc',
                      'naif0012.tls',
                      '$GEN/dsk/phobos/phobos512.bds' )
\begintext

</pre>


<p> Example code begins here.

<pre>

//
// Program LimbPointEx1
//

import spice.basic.*;
import static spice.basic.AngularUnits.*;
import static java.lang.Math.PI;

//
// Find apparent limb points on Phobos as seen from Mars. 
//
public class LimbPointEx1
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
      final String                      META   = "LimbPointEx1.tm";

      final int                         MAXN   = 10000;
      final int                         NMETH  = 2;

      //
      // Local variables
      //
      AberrationCorrection              abcorr = 
                                           new AberrationCorrection( "CN+S" );

      Body                              obsrvr = new Body( "MARS"   );
      Body                              target = new Body( "PHOBOS" );
      
      LimbPoint[][]                     limbPoints;

      ReferenceFrame                    fixref =
                                           new ReferenceFrame( "IAU_PHOBOS" );

      String[]                          methds = {
                                                    "TANGENT/ELLIPSOID",
                                                    "TANGENT/DSK/UNPRIORITIZED" 
                                                 };

      String                            corloc = "CENTER";
      String                            utc    = "2008 AUG 11 00:00:00 UTC";

      TDBTime                           et;
      TDBTime                           trgepc;

      Vector3                           z      = new Vector3( 0.0, 0.0, 1.0 );

      double                            delrol;  
      double[]                          pointArray;
      double                            roll;
      double                            schstp;
      double                            soltol;

      int                               i;
      int                               j;
      int                               k;
      int                               ncuts;
      int                               npts;


      try
      {
         //
         // Load kernels.
         //
         KernelDatabase.load( META );

         //
         // Convert the UTC request time to ET (seconds past
         // J2000, TDB). 
         //
         et = new TDBTime( utc );

         //
         // Compute a set of limb points using light time and
         // stellar aberration corrections. Use both ellipsoid
         // and DSK shape models. Use a step size of 100
         // microradians to ensure we don't miss the limb.
         // Set the convergence tolerance to 100 nanoradians,
         // which will limit the height error to about 1 meter.
         // Compute 3 limb points for each computation method.
         //
         schstp = 1.0e-4;
         soltol = 1.0e-7;
         ncuts  = 3;

         System.out.format ( "%n"                   +
                             "Observer:       %s%n" +
                             "Target:         %s%n" +
                             "Frame:          %s%n" +
                             "%n"                   +
                             "Number of cuts: %d%n",
                             obsrvr.getName(),
                             target.getName(),
                             fixref.getName(),
                             ncuts                  );

         delrol = 2*PI / ncuts;

         for ( i = 0;  i < NMETH;  i++ )
         {
            //
            // Compute a set of limb points using the current
            // computation method.
            //
            limbPoints = 

               LimbPoint.create( methds[i], target, et,     fixref,
                                 abcorr,    corloc, obsrvr, z,
                                 delrol,    ncuts,  schstp, soltol,
                                 MAXN                               );
            //
            // Write the results.
            //
            System.out.format ( "%n%n"                      +
                                "Computation method = %s%n" +
                                "Locus              = %s%n", 
                                methds[i],
                                corloc                       );

            for ( j = 0;  j < ncuts;  j++ )
            {
               //
               // Display the roll angle, target epoch, and limb point
               // count for the current cutting half-plane. Note that
               // the epoch associated with the first limb point applies
               // to all points in the current half-plane.
               //
               roll   = j * delrol;
               npts   = limbPoints[j].length;
               trgepc = limbPoints[j][0].getTargetEpoch();

               System.out.format ( "%n"                                  +
                                   "  Roll angle (deg) = %21.9f%n"       +
                                   "     Target epoch  = %21.9f%n"       +
                                   "     Number of limb points at this " +
                                   "roll angle: %d%n",
                                   roll * DPR,
                                   trgepc.getTDBSeconds(),
                                   npts                                    );

               System.out.format ( "      Limb points%n" );

               for ( k = 0;  k < npts;  k++ )
               {
                  pointArray = limbPoints[j][k].toArray();

                  System.out.format ( " %20.9f %20.9f %20.9f%n",
                                      pointArray[0],
                                      pointArray[1],
                                      pointArray[2]               );

               } // End of loop for current cut.

            } // End of loop for limb, using current method.
         
         } // End of method loop.
         
         System.out.format ( "%n" );

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

Observer:       MARS
Target:         PHOBOS
Frame:          IAU_PHOBOS

Number of cuts: 3


Computation method = TANGENT/ELLIPSOID
Locus              = CENTER

  Roll angle (deg) =           0.000000000
     Target epoch  =   271684865.152078200
     Number of limb points at this roll angle: 1
      Limb points
          0.016445326         -0.000306114          9.099992715

  Roll angle (deg) =         120.000000000
     Target epoch  =   271684865.152078200
     Number of limb points at this roll angle: 1
      Limb points
         -0.204288375         -9.235230829         -5.333237706

  Roll angle (deg) =         240.000000000
     Target epoch  =   271684865.152078200
     Number of limb points at this roll angle: 1
      Limb points
          0.242785221          9.234520095         -5.333231253


Computation method = TANGENT/DSK/UNPRIORITIZED
Locus              = CENTER

  Roll angle (deg) =           0.000000000
     Target epoch  =   271684865.152078200
     Number of limb points at this roll angle: 1
      Limb points
         -0.398901673          0.007425178          9.973720555

  Roll angle (deg) =         120.000000000
     Target epoch  =   271684865.152078200
     Number of limb points at this roll angle: 1
      Limb points
         -0.959300281         -8.537573427         -4.938700447

  Roll angle (deg) =         240.000000000
     Target epoch  =   271684865.152078200
     Number of limb points at this roll angle: 1
      Limb points
         -1.380536729          9.714334047         -5.592916790

</pre>




<li>  Find apparent limb points on Mars as seen from the earth. 
      Compare results using different computation options. 
 
<p>   Use both the "TANGENT" and "GUIDED" limb point definitions. For 
      the tangent limb points, use the "ELLIPSOID LIMB" aberration 
      correction locus; for the guided limb points, use the "CENTER" 
      locus. For the "GUIDED" limb points, also compute the distance 
      of each point from the corresponding point computed using the 
      "TANGENT" definition. 
 
<p>   For comparison, compute limb points using both ellipsoid and 
      topographic shape models. 
 
<p>   Check the limb points by computing the apparent emission 
      angles at each limb point. 
 
<p>   For the ellipsoid shape model, we expect emission angles very 
      close to 90 degrees, since each illumination angle calculation 
      is done using aberration corrections for the limb point at 
      which the angles are measured. 
 
<p>   Use the target body-fixed +Z axis as the reference direction 
      for generating cutting half-planes. This choice enables the 
      user to see whether the first limb point is near the target's 
      north pole. 
       
<p>   For each option, use just three cutting half-planes, in order 
      to keep the volume of output manageable. In most applications, 
      the number of cuts and the number of resulting limb points 
      would be much greater. 
 
<p>   Use the meta-kernel shown below. 

<pre>
KPL/MK 

File: LimbPointEx2.tm 

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
   de430.bsp                        Planetary ephemeris 
   mar097.bsp                       Mars satellite ephemeris 
   pck00010.tpc                     Planet orientation and 
                                    radii 
   naif0012.tls                     Leapseconds 
   megr90n000cb_plate.bds           DSK plate model based on 
                                    MGS MOLAR MEGDR DEM,  
                                    resolution 4  
                                    pixels/degree. 

\begindata 

   KERNELS_TO_LOAD = ( 'de430.bsp', 
                       'mar097.bsp', 
                       'pck00010.tpc', 
                       'naif0012.tls', 
                       'megr90n000cb_plate.bds' ) 
\begintext 

</pre>

<p> Example code begins here.

<pre>

//
// Program LimbPointEx2
//

import spice.basic.*;
import static spice.basic.AngularUnits.*;
import static java.lang.Math.PI;

//
// Find apparent limb points on Mars as seen from Earth. 
// Compare results using different computation options. 
//
public class LimbPointEx2
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
      final String                      META   = "LimbPointEx2.tm";

      final int                         MAXN   = 10000;
      final int                         NMETH  = 3;

      //
      // Local variables
      //
      AberrationCorrection              abcorr = 
                                           new AberrationCorrection( "CN+S" );

      Body                              obsrvr = new Body( "EARTH" );
      Body                              target = new Body( "MARS"  );
      
      GeodeticCoordinates               geoCoords;

      IlluminationAngles                iluAng;

      LimbPoint[][]                     limbPoints;
      LimbPoint[][]                     svPoints = new LimbPoint[MAXN][1];

      PositionRecord                    pr;

      ReferenceFrame                    fixref =
                                           new ReferenceFrame( "IAU_MARS" );

      String[]                          corloc = {
                                                    "ELLIPSOID LIMB",
                                                    "ELLIPSOID LIMB",
                                                    "CENTER"
                                                 };

      String[]                          ilumth = { 
                                                    "ELLIPSOID",
                                                    "DSK/UNPRIORITIZED",
                                                    "DSK/UNPRIORITIZED"  
                                                 };

      String[]                          methds = {
                                                   "TANGENT/ELLIPSOID",
                                                   "TANGENT/DSK/UNPRIORITIZED",
                                                   "GUIDED/DSK/UNPRIORITIZED" 
                                                 };

      String                            utc    = "2008 AUG 11 00:00:00 UTC";

      TDBTime                           et;
      TDBTime                           trgepc;

      Vector3                           z      = new Vector3( 0.0, 0.0, 1.0 );

      double                            delrol;  
      double                            dist;
      double                            emissn;
      double                            f;
      double[]                          pointArray;
      double[]                          radii;
      double                            re;
      double                            rp;
      double                            roll;
      double                            schstp;
      double                            soltol;

      int                               i;
      int                               j;
      int                               k;
      int                               ncuts;
      int                               npts;


      try
      {
         //
         // Load kernels.
         //
         KernelDatabase.load( META );

         //
         // Convert the UTC request time to ET (seconds past
         // J2000, TDB). 
         //
         et = new TDBTime( utc );

         //
         // Look up the target body's radii. We'll use these to
         // convert Cartesian to planetographic coordinates. Use
         // the radii to compute the flattening coefficient of
         // the reference ellipsoid.
         // 
         radii = target.getValues( "RADII" );

         //
         // Compute the flattening coefficient for planetodetic
         // coordinates.
         //
         re = radii[0];
         rp = radii[2];
         f  = ( re - rp ) / re;
        
         //
         // Obtain the observer-target distance at `et'.
         // 
         pr   = new PositionRecord( target, et, fixref, abcorr, obsrvr );

         dist = pr.norm();
 
         //
         // Compute a set of limb points using light time and
         // stellar aberration corrections. Use both ellipsoid
         // and DSK shape models.
         //
         //  Set the angular step size so that a single step will
         // be taken in the root bracketing process; that's all
         // that is needed since we don't expect to have multiple
         // limb points in any cutting half-plane.
         // 
         schstp = 4.0;

         //
         // Set the convergence tolerance to minimize the height
         // error. We can't achieve the 1 millimeter precision
         // suggested by the formula because the earth-Mars
         // distance is about 3.5e8 km. 
         //
         // Compute 3 limb points for each computation method.
         //
         soltol = 1.0e-6 / dist;

         //
         // Set the number of cutting half-planes and roll step.
         //
         ncuts  = 3;
         delrol = ( 2 * Math.PI ) / ncuts;

         System.out.format ( "%n"                   +
                             "Observer:       %s%n" +
                             "Target:         %s%n" +
                             "Frame:          %s%n" +
                             "%n"                   +
                             "Number of cuts: %d%n",
                             obsrvr.getName(),
                             target.getName(),
                             fixref.getName(),
                             ncuts                  );

         delrol = 2*PI / ncuts;

         for ( i = 0;  i < NMETH;  i++ )
         {
            //
            // Compute a set of limb points using the current
            // computation method.
            //
            limbPoints = 

               LimbPoint.create( methds[i], target,    et,     fixref,
                                 abcorr,    corloc[i], obsrvr, z,
                                 delrol,    ncuts,     schstp, soltol,
                                 MAXN                                 );

            //
            // If we're using the TANGENT/DSK/UNPRIORITIZED method,
            // save the limb points for later use.
            //
            if ( i == 1 )
            {
               svPoints = new LimbPoint[ncuts][0];

               for ( j = 0;  j < ncuts;  j++ )
               {
                  npts        = limbPoints[j].length;
                  svPoints[j] = new LimbPoint[npts];

                  for ( k = 0;  k < npts;  k++ )
                  {
                     svPoints[j][k] = limbPoints[j][k];
                  }
               }
            }

            //
            // Write the results.
            //
            System.out.format ( "%n%n"                      +
                                "Computation method = %s%n" +
                                "Locus              = %s%n", 
                                methds[i],
                                corloc[i]                    );

            for ( j = 0;  j < ncuts;  j++ )
            {
               //
               // Display the roll angle, target epoch, and limb point
               // count for the current cutting half-plane. Note that
               // the epoch associated with the first limb point applies
               // to all points in the current half-plane.
               //
               roll   = j * delrol;
               npts   = limbPoints[j].length;
               trgepc = limbPoints[j][0].getTargetEpoch();

               System.out.format ( "%n"                                  +
                                   "  Roll angle (deg) = %21.9f%n"       +
                                   "     Target epoch  = %21.9f%n"       +
                                   "     Number of limb points at this " +
                                   "roll angle: %d%n",
                                   roll * DPR,
                                   trgepc.getTDBSeconds(),
                                   npts                                    );


               for ( k = 0;  k < npts;  k++ )
               {
                  geoCoords = new GeodeticCoordinates( limbPoints[j][k], re, f);

                  System.out.format ( "      Limb point planetodetic " +
                                      "coordinates:%n"                  );

                  System.out.format ( 
                           "       Longitude      (deg): %21.9f%n" + 
                           "       Latitude       (deg): %21.9f%n" + 
                           "       altitude        (km): %21.9f5%n",
                           geoCoords.getLongitude()*DPR, 
                           geoCoords.getLatitude() *DPR, 
                           geoCoords.getAltitude()                    );
                  //
                  // Get illumination angles for this limb point. 
                  //
                  iluAng = 

                     new IlluminationAngles ( ilumth[i], target, et,
                                              fixref,    abcorr, obsrvr,
                                              limbPoints[j][k]          );

                  System.out.format ( "       Emission angle (deg): %21.9f%n",
                                      iluAng.getEmissionAngle()*DPR           );

                  //
                  // Show the difference between the GUIDED and TANGENT
                  // results when a DSK model is used.
                  //
                  if ( i == 2 ) 
                  {
                     dist = limbPoints[j][k].dist( svPoints[j][k] );

                     System.out.format ( "       Distance error  (km): " +
                                         "%21.9f%n",
                                         dist                           );
                  }

               } // End of loop for current cut.

            } // End of loop for limb, using current method.
         
         } // End of method loop.
         
         System.out.format ( "%n" );

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

Observer:       EARTH
Target:         MARS
Frame:          IAU_MARS

Number of cuts: 3


Computation method = TANGENT/ELLIPSOID
Locus              = ELLIPSOID LIMB

  Roll angle (deg) =           0.000000000
     Target epoch  =   271683700.368869900
     Number of limb points at this roll angle: 1
      Limb point planetodetic coordinates:
       Longitude      (deg):         -19.302258950
       Latitude       (deg):          64.005620446
       altitude        (km):          -0.0000000005
       Emission angle (deg):          90.000000000

  Roll angle (deg) =         120.000000000
     Target epoch  =   271683700.368948160
     Number of limb points at this roll angle: 1
      Limb point planetodetic coordinates:
       Longitude      (deg):          85.029135674
       Latitude       (deg):         -26.912378799
       altitude        (km):           0.0000000005
       Emission angle (deg):          90.000000000

  Roll angle (deg) =         240.000000000
     Target epoch  =   271683700.368949800
     Number of limb points at this roll angle: 1
      Limb point planetodetic coordinates:
       Longitude      (deg):        -123.633654215
       Latitude       (deg):         -26.912378799
       altitude        (km):          -0.0000000005
       Emission angle (deg):          90.000000000


Computation method = TANGENT/DSK/UNPRIORITIZED
Locus              = ELLIPSOID LIMB

  Roll angle (deg) =           0.000000000
     Target epoch  =   271683700.368869900
     Number of limb points at this roll angle: 1
      Limb point planetodetic coordinates:
       Longitude      (deg):         -19.302258949
       Latitude       (deg):          63.893637432
       altitude        (km):          -3.6675539585
       Emission angle (deg):          89.979580513

  Roll angle (deg) =         120.000000000
     Target epoch  =   271683700.368948160
     Number of limb points at this roll angle: 1
      Limb point planetodetic coordinates:
       Longitude      (deg):          85.434644181
       Latitude       (deg):         -26.705411232
       altitude        (km):          -0.0448323825
       Emission angle (deg):          88.089500425

  Roll angle (deg) =         240.000000000
     Target epoch  =   271683700.368949800
     Number of limb points at this roll angle: 1
      Limb point planetodetic coordinates:
       Longitude      (deg):        -123.375003592
       Latitude       (deg):         -27.043096738
       altitude        (km):           3.6956284895
       Emission angle (deg):          89.875890611


Computation method = GUIDED/DSK/UNPRIORITIZED
Locus              = CENTER

  Roll angle (deg) =           0.000000000
     Target epoch  =   271683700.368922530
     Number of limb points at this roll angle: 1
      Limb point planetodetic coordinates:
       Longitude      (deg):         -19.302259163
       Latitude       (deg):          64.005910146
       altitude        (km):          -3.6764245525
       Emission angle (deg):          89.979580513
       Distance error  (km):           6.664208540

  Roll angle (deg) =         120.000000000
     Target epoch  =   271683700.368922530
     Number of limb points at this roll angle: 1
      Limb point planetodetic coordinates:
       Longitude      (deg):          85.029135792
       Latitude       (deg):         -26.912405352
       altitude        (km):          -0.3289889155
       Emission angle (deg):          91.525256314
       Distance error  (km):          24.686472888

  Roll angle (deg) =         240.000000000
     Target epoch  =   271683700.368922530
     Number of limb points at this roll angle: 1
      Limb point planetodetic coordinates:
       Longitude      (deg):        -123.633653487
       Latitude       (deg):         -26.912086524
       altitude        (km):           3.6260588505
       Emission angle (deg):          89.809897171
       Distance error  (km):          15.716056568

</pre>

</li>




<li>Find apparent limb points on comet Churyumov-Gerasimenko
      as seen from the Rosetta orbiter.
<p>
      This computation is an example of a case for which some
      of the cutting half-planes contain multiple limb points.
<p>
      Use the "TANGENT" limb definition, since the target shape
      is not well approximated by its reference ellipsoid.
      Use the "CENTER" aberration correction locus since the
      light time difference across the object is small.

<p>  Use the meta-kernel shown below to load the required SPICE
     kernels.

<pre>
KPL/MK

File: LimbPointEx3.tm

This meta-kernel is intended to support operation of SPICE
example programs. The kernels shown here should not be
assumed to contain adequate or correct versions of data
required by SPICE-based user applications.

In order for an application to use this meta-kernel, the
paths of the kernels referenced here must be adjusted to
be compatible with the user's host computer directory
structure.

The names and contents of the kernels referenced
by this meta-kernel are as follows:

  File name                          Contents
  ---------                          --------
  DE405.BSP                          Planetary ephemeris
  NAIF0011.TLS                       Leapseconds
  ROS_CG_M004_NSPCESA_N_V1.BDS       DSK plate model based on
                                     Rosetta NAVCAM data
  RORB_DV_145_01_______00216.BSP     Rosetta orbiter
                                     ephemeris
  CORB_DV_145_01_______00216.BSP     Comet Churyumov-
                                     Gerasimenko ephemeris
  ROS_CG_RAD_V10.TPC                 Comet Churyumov-
                                     Gerasimenko radii
  ROS_V25.TF                         Comet C-G frame kernel
                                     (includes SCLK
                                     parameters)
  CATT_DV_145_01_______00216.BC      Comet C-G C-kernel


\begindata

   PATH_VALUES     = (

      '/ftp/pub/naif/pds/data/+'
      'ro_rl-e_m_a_c-spice-6-v1.0/rossp_1000/DATA'

                     )

   PATH_SYMBOLS    = (

      'KERNELS'
                     )

   KERNELS_TO_LOAD = (

      '$KERNELS/SPK/DE405.BSP'
      '$KERNELS/LSK/NAIF0011.TLS'
      '$KERNELS/SPK/RORB_DV_145_01_______00216.BSP'
      '$KERNELS/SPK/CORB_DV_145_01_______00216.BSP'
      '$KERNELS/PCK/ROS_CG_RAD_V10.TPC'
      '$KERNELS/FK/ROS_V25.TF'
      '$KERNELS/CK/CATT_DV_145_01_______00216.BC'
      '$KERNELS/DSK/ROS_CG_M004_NSPCESA_N_V1.BDS'

                     )
\begintext


</pre>


<p> Example code begins here.

<pre>

//
// Program LimbPointEx3
//

import spice.basic.*;
import static spice.basic.AngularUnits.*;
import static java.lang.Math.PI;

//
// Find limb points on comet Churyumov-Gerasimenko
// as seen from the Rosetta orbiter.
//
public class LimbPointEx3
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
      final String                      META   = "LimbPointEx3.tm";

      final int                         MAXN   = 10000;
      final int                         NMETH  = 2;

      //
      // Local variables
      //
      AberrationCorrection              abcorr = 
                                           new AberrationCorrection( "CN+S" );

      Body                              obsrvr = new Body( "ROSETTA" );

      Body                              target =

                                           new Body( "CHURYUMOV-GERASIMENKO" );
      
      LimbPoint[][]                     limbPoints;

      PositionVector                    trgpos;

      ReferenceFrame                    fixref =
                                           new ReferenceFrame( "67P/C-G_CK" );

      String                            method = "TANGENT/DSK/UNPRIORITIZED";

      String                            corloc = "CENTER";
      String                            utc    = "2015 MAY 10 00:00:00 UTC";

      TDBTime                           et;
      TDBTime                           trgepc;

      Vector3                           axis;
      Vector3                           refvec;
      Vector3                           xvec   = new Vector3( 1.0, 0.0, 0.0 );

      double                            angle;
      double                            delrol;  
      double[]                          pointArray;
      double                            roll;
      double                            schstp;
      double                            soltol;

      int                               i;
      int                               j;
      int                               ncuts;
      int                               npts;


      try
      {
         //
         // Load kernels.
         //
         KernelDatabase.load( META );

         //
         // Convert the UTC request time to ET (seconds past
         // J2000, TDB). 
         //
         et = new TDBTime( utc );

         //
         // Compute a set of limb points using light time and
         // stellar aberration corrections. Use a step size 
         // corresponding to a 1 meter height error to ensure 
         // we don't miss the limb. Set the convergence tolerance 
         // to 1/100 of this amount, which will limit the height 
         // convergence error to about 1 cm.
         //
         trgpos = new PositionVector( target, et,    fixref, 
                                      abcorr, obsrvr        );

         schstp = 1.0e-3 / trgpos.norm();
         soltol = schstp / 100.0;

         //
         // Set the reference vector to the start of a
         // region of the roll domain in which we know
         // (from an external computation) that we'll
         // find multiple limb points in some half planes.
         // Compute 30 limb points, starting with the
         // half-plane containing the reference vector.
         //
         axis   = trgpos.negate();
         angle  = 310.0 * RPD;

         refvec = xvec.rotate( axis, angle );

         ncuts  = 30;
         delrol = 2*PI / 1000.0;
       

         System.out.format ( "%n"                   +
                             "Observer:       %s%n" +
                             "Target:         %s%n" +
                             "Frame:          %s%n" +
                             "%n"                   +
                             "Number of cuts: %d%n",
                             obsrvr.getName(),
                             target.getName(),
                             fixref.getName(),
                             ncuts                  );
         //
         // Compute limb points.
         //
         limbPoints = LimbPoint.create( method, target, et,     fixref,
                                        abcorr, corloc, obsrvr, refvec, 
                                        delrol, ncuts,  schstp, soltol,
                                        MAXN                           );
         //
         // Write the results.
         //
         System.out.format ( "%n%n"                      +
                             "Computation method = %s%n" +
                             "Locus              = %s%n", 
                             method,
                             corloc                       );

         for ( i = 0;  i < ncuts;  i++ )
         {
            //
            // Display the roll angle, target epoch, and limb point
            // count for the current cutting half-plane. Note that
            // the epoch associated with the lowest-indexed limb point 
            // applies to all points in the current half-plane.
            //
            roll   = i * delrol;
            npts   = limbPoints[i].length;
            trgepc = limbPoints[i][0].getTargetEpoch();

            System.out.format ( "%n"                                  +
                                "  Roll angle (deg) = %21.9f%n"       +
                                "     Target epoch  = %21.9f%n"       +
                                "     Number of limb points at this " +
                                "roll angle: %d%n",
                                roll * DPR,
                                trgepc.getTDBSeconds(),
                                npts                                    );

            System.out.format ( "      Limb points%n" );

            for ( j = 0;  j < npts;  j++ )
            {
               pointArray = limbPoints[i][j].toArray();

               System.out.format ( " %20.9f %20.9f %20.9f%n",
                                   pointArray[0],
                                   pointArray[1],
                                   pointArray[2]               );

            } // End of loop for current cut.

         } // End of loop for limb.
         
         System.out.format ( "%n" );

      } // End of try block

      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }

   } // End of main method 
   
}

</pre>


<p> When this program was executed on a PC/Linux/gcc/64-bit/java 1.5
platform, the output was (only the first three and last three
limb points are shown here): 

<pre>

Observer:       ROSETTA ORBITER
Target:         CHURYUMOV-GERASIMENKO
Frame:          67P/C-G_CK

Number of cuts: 30


Computation method = TANGENT/DSK/UNPRIORITIZED
Locus              = CENTER

  Roll angle (deg) =           0.000000000
     Target epoch  =   484488067.184933800
     Number of limb points at this roll angle: 3
      Limb points
          1.320362370         -0.347604560          1.445254172
          0.970323084          0.201631414          0.961979719
          0.436713864          0.048193273          0.442280570

  Roll angle (deg) =           0.360000000
     Target epoch  =   484488067.184933800
     Number of limb points at this roll angle: 3
      Limb points
          1.330124598         -0.352820747          1.438735635
          0.965299850          0.201734528          0.946088598
          0.453749999          0.081575117          0.447557495

  Roll angle (deg) =           0.720000000
     Target epoch  =   484488067.184933800
     Number of limb points at this roll angle: 3
      Limb points
          1.338848631         -0.358372459          1.431175507
          0.961970200          0.192000406          0.934228157
          0.458205425          0.079784540          0.447433687

     ...

  Roll angle (deg) =           9.720000000
     Target epoch  =   484488067.184933800
     Number of limb points at this roll angle: 3
      Limb points
          1.567889849         -0.675587383          1.254779196
          0.709821051         -0.111518380          0.547753702
          0.491097248         -0.144183621          0.385975970

  Roll angle (deg) =          10.080000000
     Target epoch  =   484488067.184933800
     Number of limb points at this roll angle: 3
      Limb points
          1.583510591         -0.668467845          1.249125044
          0.633077981         -0.300058272          0.502702168
          0.254698631         -0.760413229          0.266773664

  Roll angle (deg) =          10.440000000
     Target epoch  =   484488067.184933800
     Number of limb points at this roll angle: 3
      Limb points
          1.599288724         -0.662045674          1.243576395
          0.633123187         -0.293598781          0.495368615
          0.271957881         -0.762004976          0.274621861

</pre>

</li>


   </ol>

   */
   public static LimbPoint[][] create ( String                  method,
                                        Body                    target,
                                        Time                    et,
                                        ReferenceFrame          fixref,
                                        AberrationCorrection    abcorr,
                                        String                  corloc,
                                        Body                    obsrvr,
                                        Vector3                 refvec,
                                        double                  rolstp,
                                        int                     ncuts,
                                        double                  schstp,
                                        double                  soltol,
                                        int                     maxn    )
      throws SpiceException

   {
      //
      // Allocate space for the output arrays returned by 
      //
      //    CSPICE.limbpt
      //
      int[]                             npts   = new int   [ ncuts ];
      double[][]                        points = new double[ maxn ][3];
      double[]                          epochs = new double[ maxn ];
      double[][]                        tangts = new double[ maxn ][3];

      //
      // Delegate the job to the CSPICE limb finding method.
      //
      CSPICE.limbpt ( method,           target.getName(), et.getTDBSeconds(),
                      fixref.getName(), abcorr.getName(), corloc,
                      obsrvr.getName(), refvec.toArray(), rolstp,           
                      ncuts,            schstp,           soltol,          
                      maxn,             npts,             points,
                      epochs,           tangts                               );

      //
      // retArray is the LimbPoint array to be returned. The row 
      // dimensions are unknown as of yet.
      //
      LimbPoint[][] retArray = new LimbPoint[ ncuts ][];

      //
      // Set the values of the output limb point array.
      //
      // `pix' is the "point index": it's the index of the current
      // limb point in the `points' array.
      //
      int pix = 0;

      for ( int cut = 0;  cut < ncuts;  cut++ )
      {
         //
         // retArray[cut] is the row of LimbPoints in the current cutting
         // half-plane.
         //
         int rowsiz    = npts[cut];
        
         retArray[cut] = new LimbPoint[ rowsiz ];

         for ( int i = 0;  i < rowsiz;  i++ )
         {
            retArray[cut][i] = 

               new LimbPoint( new Vector3( points[pix] ),
                              new TDBTime( epochs[pix] ), 
                              new Vector3( tangts[pix] )  );
            ++pix;
         }
      }

      return ( retArray );
   }



   /**
   Return the target epoch from a LimbPoint instance. This
   method returns a deep copy.
   */
   public TDBTime getTargetEpoch()
   {
      return (  new TDBTime(targetEpoch)  );
   }

   /**
   Return the observer to limb point vector from a LimbPoint instance.
   This method returns a deep copy.
   */
   public Vector3 getTangentVector()
   {
      return (  new Vector3(tangentVector)  );
   }

}
