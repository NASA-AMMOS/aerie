
package spice.basic;

/**
Class SurfaceIntercept supports ray-target surface
intercept computations.

<p> Surface intercept computations are performed by 
creation of SurfaceIntercept objects. Each SurfaceIntercept
instance consists of:
<ul>
<li>A boolean "found flag" indicating whether the ray
intersects the target surface. The other elements of
the instance are valid if and only if the found flag is true.
</li>
<li>A Vector3 instance representing the 
intercept point. This vector is expressed in the 
target body-fixed, body-centered reference frame 
specified in the method call that created the intercept;
the evaluation epoch of the frame's orientation is
that associated with the intercept point. 
</li>
<li>A Vector3 instance representing the vector from the
ray's vertex to the intercept. This vector is expressed
in the same reference frame as the intercept.
</li>
</ul>
<p>If a surface intercept computation does not find
a point of intersection, an attempt to access a member,
other than the "found" flag, of the
resulting SurfaceIntercept instance will cause a
{@link PointNotFoundException} to be thrown.

<p> For functionality similar to that of the SPICE routine
SINCPT, see the constructor 
{@link #SurfaceIntercept( String, Body, Time, ReferenceFrame,
AberrationCorrection, Body, ReferenceFrame, Vector3 )}.
 
<p> For functionality similar to that of the SPICE routine
DSKXV, see the method 
{@link #create( boolean, Body, Surface[], Time, ReferenceFrame,
Vector3[], Vector3[])}.
 

<p> For functionality similar to that of the SPICE routine
DSKXSI, see the constructor 
{@link SurfaceInterceptWithDSKInfo#SurfaceInterceptWithDSKInfo( 
boolean, Body, Surface[], Time, ReferenceFrame,
Vector3, Vector3 )}.


<p> Code examples are provided in the detailed documentation
for the constructors
and methods.



<h2> Files </h2>


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
           pool.  Similarly, the frame definition required to map 
           between the frame designated by ray direction vector's
           frame `rayRef' and the target 
           body-fixed frame must be available.
           Typically the definition is supplied by loading a
           frame kernel via KernelDatabase.load.
</li>

<li>        CK data: if the frame to which `rayRef' refers is fixed to a 
            spacecraft instrument or structure, one or more CK files
            may be needed to permit transformation of vectors between 
            that frame and both the J2000 and the target body-fixed 
            frames. 
</li>

<li>       Surface name-ID associations: if surface names are specified
           in a constructors' `method' arguments, 
           the association of these names with their
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




<h2> Class SurfaceIntercept Particulars </h2>

<h3>Using DSK data</h3>

 
<p><b>DSK loading and unloading</b>
 
<p>DSK files providing data used by this class are loaded by 
      calling {@link KernelDatabase#load} and can be unloaded by 
      calling {@link KernelDatabase#unload} or
      {@link KernelDatabase#clear}. See the documentation of 
      {@link KernelDatabase#load} for limits on numbers 
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
      required in the constructors' `method' arguments. 




<h2> Version and Date</h2>


<h3> Version 2.0.0 10-JAN-2017 (NJB) </h3>

<pre>
   Access of fields has been changed from private to
   package private.

   Added vectorized creation method using arrays of ray
   vertices and directions as inputs.  

   Added copy constructor.

   Changed private fields: inputs are now stored in a 
   separate object of an inner class. There are 
   separate inner classes for the different types of
   intercept methods: FullInputs, RayArrayInputs, and
   SurfaceInterceptWithDSKInfo.RayInputs. 

</pre>

<h3> Version 1.0.0 24-NOV-2009 (NJB)</h3>
*/
public class SurfaceIntercept 
{
   //
   // Public Constants
   //

   //
   // The values below are the geometric "methods" supported by
   // the SurfaceIntercept constructor.
   //
   public final static String ELLIPSOID = "ELLIPSOID";

   //
   // Fields
   //
   TDBTime               targetEpoch;
   Vector3               surfaceIntercept;
   Vector3               surfaceVector;
   boolean               wasFound;
   Inputs                inputs;

   //
   // Package private class constants
   //
   final static String endl = System.getProperty( "line.separator" );


   /**
   Class Inputs is an abstract nested class which supports creation of
   descriptive error messages containing inputs used to attempt to
   define a surface intercept.
   */
   abstract class Inputs
   {
      //
      // Return a string representation of the fields of an Inputs instance.
      //
      abstract String getString()

         throws SpiceException;

      //
      // Create a deep copy of an Inputs instance.
      //
      abstract Inputs copy()

         throws SpiceException;
   }

   /**
   Class FullInputs is an inner class which supports creation of
   descriptive error messages containing inputs used to attempt to
   create a SurfaceIntercept instance via the primary SurfaceIntercept
   constructor.
   */

   class FullInputs extends Inputs
   {
      //
      // Saved SINCPT-style inputs used for error message construction:
      //
      private String                method;
      private Body                  target;
      private Time                  time;
      private ReferenceFrame        fixRef;
      private AberrationCorrection  abcorr;
      private Body                  observer;
      private ReferenceFrame        rayRef;
      private Vector3               rayDir;

      private FullInputs( String                method,
                          Body                  target,
                          Time                  time,
                          ReferenceFrame        fixRef,
                          AberrationCorrection  abcorr,
                          Body                  observer,
                          ReferenceFrame        rayRef,
                          Vector3               rayDir   )
      {
         this.method   = method;
         this.target   = target;
         this.time     = time;
         this.fixRef   = fixRef;
         this.abcorr   = abcorr;
         this.observer = observer;
         this.rayRef   = rayRef;
         this.rayDir   = rayDir;
      }


      //
      // FullInputs Methods
      // 

      //
      // Create a deep copy of a FullInputs instance.
      //
      FullInputs copy()

         throws SpiceException
      {
         FullInputs retval = new FullInputs( this.method,
                                             this.target,
                                             this.time,
                                             this.fixRef,
                                             this.abcorr,
                                             this.observer,
                                             this.rayRef,
                                             this.rayDir   );
         return ( retval );
      }

      //
      // Return a string representation of a FullInputs instance.
      //
      String getString()

         throws SpiceException
      {
         String retString = new String( 
        
         "   method       = " + method                           + endl  +
         "   target       = " + target.getName()                 + endl  +
         "   time         = " + ( new TDBTime(time) ).toString() + endl  +
         "   fixRef       = " + fixRef.getName()                 + endl  +
         "   abcorr       = " + abcorr.getName()                 + endl  +
         "   observer     = " + observer.getName()               + endl  +
         "   rayRef       = " + rayRef.getName()                 + endl  +
         "   rayDir       = " + rayDir.toString()                            );
        
         return( retString );
      }
   }


   /**
   Class RayArrayInputs is an inner class which supports creation of
   descriptive error messages containing inputs used to attempt to
   create a SurfaceIntercept instance via the SurfaceIntercept
   `create' method.
   */
   class RayArrayInputs extends Inputs
   {
      //
      // Saved DSKXV-style inputs used for error message construction:
      //
      private boolean               prioritized;
      private Body                  target;
      private Time                  time;
      private int                   index;

      //
      // Constructors
      //
      RayArrayInputs( boolean           prioritized,
                      Body              target,
                      Time              time,
                      int               index        )
      {
         this.prioritized  = prioritized;
         this.target       = target;
         this.time         = time;
         this.index        = index;
      }

      /**
      No-arguments constructor.
      */
      RayArrayInputs()
      {
      }



      //
      // RayArrayInputs Methods
      // 

      //
      // Create a deep copy of a RayArrayInputs instance.
      //
      RayArrayInputs copy()

         throws SpiceException
      {
         RayArrayInputs retval = new RayArrayInputs( this.prioritized,
                                                     this.target,
                                                     this.time,
                                                     this.index        );
         return ( retval );
      }




      //
      // Return a string representation of a RayArrayInputs instance.
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
         "   time         = " + ( new TDBTime(time) ).toString() + endl  +
         "   ray index    = " + index                            + endl   );
          
         return( retString );
      }
   }

   //
   // Constructors
   //

   /**
   No-arguments constructor.
   */
   public SurfaceIntercept()
   {
   }


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public SurfaceIntercept( SurfaceIntercept surfx )

      throws SpiceException
   {
      surfaceIntercept = new Vector3( surfx.surfaceIntercept );
      surfaceVector    = new Vector3( surfx.surfaceVector    );
      targetEpoch      = new TDBTime( surfx.targetEpoch      );
      inputs           = surfx.inputs.copy();
      wasFound         = surfx.wasFound;
   }



   /**
   Find a specified surface intercept point; create a SurfaceIntercept
   instance containing the result.

<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<ol>
<li> Compute surface intercept points on Mars 
   for the boresight and FOV boundary vectors of the MGS MOC 
   narrow angle camera. The intercepts are computed for a single
   observation epoch. Converged Newtonian light time and stellar
   aberration corrections are used. For simplicity, camera
   distortion is ignored.
 
<p>
   Intercepts are computed using both triaxial ellipsoid and  
   topographic surface models.  
 
<p>
   The topographic model is based on data from the MGS MOLA DEM 
   megr90n000cb, which has a resolution of 4 pixels/degree. A 
   triangular plate model was produced by computing a 720 x 1440 
   grid of interpolated heights from this DEM, then tessellating 
   the height grid. The plate model is stored in a type 2 segment 
   in the referenced DSK file. 




<p> Use the meta-kernel shown below to load the required SPICE
kernels.
<pre>
KPL/MK 

File: SurfaceIntercept_ex1.tm 

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
   naif0011.tls                     Leapseconds  
   mgs_moc_v20.ti                   MGS MOC instrument 
                                    parameters 
   mgs_sclkscet_00061.tsc           MGS SCLK coefficients 
   mgs_sc_ext12.bc                  MGS s/c bus attitude 
   mgs_ext12_ipng_mgs95j.bsp        MGS ephemeris 
   megr90n000cb_plate.bds           Plate model based on 
                                    MEGDR DEM, resolution 
                                    4 pixels/degree. 

\begindata 

   KERNELS_TO_LOAD = ( 'de430.bsp', 
                       'mar097.bsp', 
                       'pck00010.tpc', 
                       'naif0012.tls', 
                       'mgs_moc_v20.ti', 
                       'mgs_sclkscet_00061.tsc', 
                       'mgs_sc_ext12.bc', 
                       'mgs_ext12_ipng_mgs95j.bsp', 
                       'megr90n000cb_plate.bds'      ) 
\begintext 
</pre>


<p> Example code begins here.

<pre>
//
// Program SurfaceInterceptEx1
//
// Compute surface intercept points on Mars 
// for the boresight and FOV boundary vectors of the MGS MOC 
// narrow angle camera.
//
import spice.basic.*;
import static spice.basic.AngularUnits.*;

public class SurfaceInterceptEx1
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
      final String                      META   = "SurfaceInterceptEx1.tm";

      final int                         NMETH  = 2;

      //
      // Local variables
      //
      AberrationCorrection              abcorr = 
                                           new AberrationCorrection( "CN+S" );

      Body                              obsrvr = new Body( "MGS"  );
      Body                              target = new Body( "Mars" );
      
      FOV                               MOCNACFov;

      String                            camnam = "MGS_MOC_NA";
      Instrument                        camera = new Instrument( camnam );

      LatitudinalCoordinates            latCoords;

      ReferenceFrame                    fixref =
                                           new ReferenceFrame( "IAU_MARS" );

      ReferenceFrame                    dref;

      String[]                          methds = {
                                                    "Ellipsoid",
                                                    "DSK/Unprioritized" 
                                                 };

      String[]                          srftyp = 
                                        { 
                                           "Ellipsoid", 
                                           "MGS/MOLA topography, 4 pixel/deg"
                                        };

      String                            utc      = "2003 OCT 13 06:00:00 UTC";
      String                            title;

      SurfaceIntercept                  surfx;

      TDBTime                           et;
      Time                              trgepc;

      Vector3[]                         bounds;
      Vector3                           bsight;
      Vector3                           dvec;
      Vector3                           spoint;
      Vector3                           srfvec;
 
      boolean                           found;

      double                            dist;  
      double                            lat;
      double                            lon;
      double                            radius;

      int                               i;
      int                               j;
      int                               ncornr;    


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
         // Get the MGS MOC Narrow angle camera (MGS_MOC_NA) 
         // field of view (FOV) parameters. 
         //
         // We'll store the camera-fixed frame in `dref', 
         // the camera boresight vector in `bsight', and the 
         // FOV corner vectors in the array `bounds'.
         //
         MOCNACFov = new FOV( camera );
  
         bsight    = MOCNACFov.getBoresight();
         bounds    = MOCNACFov.getBoundary();
         ncornr    = bounds.length;
         dref      = MOCNACFov.getReferenceFrame();

         System.out.format( "%n" +                          
                            "Surface Intercept Locations for Camera%n"  +
                            "FOV Boundary and Boresight Vectors%n"      +
                            "%n"                                        +
                            "   Instrument:             %s%n"           +
                            "   Epoch:                  %s%n"           +
                            "   Aberration correction:  %s%n"           +
                            "%n",
                            camera.getName(), utc, abcorr.toString()  ); 
         //
         // Now compute and display the surface intercepts for the 
         // boresight and all of the FOV boundary vectors. 
         //
         for( i = 0;  i <= ncornr;  i++ )
         {
            if ( i < ncornr )
            {
               title = String.format( "Corner vector %d", (i+1) );
               dvec  = bounds[i];
            }
            else
            {
               title = "Boresight vector";
               dvec  = bsight;
            }

            System.out.format( "%n" +
                               "%s%n",
                               title  );

            System.out.format( "%n" +
                               "  Vector in %s frame = %n", dref.toString() );

            System.out.format( "   %18.10e %18.10e %18.10e%n", 
                               dvec.getElt(0), dvec.getElt(1), dvec.getElt(2) );
             
            System.out.format( "%n" +
                               "  Intercept:%n" );
            //
            // Compute the surface intercept point using
            // the specified aberration corrections. Loop
            // over the set of computation methods.
            //
            for ( j = 0;  j < NMETH;  j++ )
            {
               surfx = 

                  new SurfaceIntercept ( methds[j], target, et,   fixref, 
                                         abcorr,    obsrvr, dref, dvec   );

               if ( surfx.wasFound() )
               {
                  //
                  // Compute range from observer to apparent intercept. 
                  //
                  srfvec = surfx.getSurfaceVector();                  
                  dist   = srfvec.norm();

                  //
                  // Convert rectangular coordinates of the intercept 
                  // point `surfx' to planetocentric latitude and longitude.
                  // Convert radians to degrees. 
                  //
                  latCoords = new LatitudinalCoordinates(surfx.getIntercept() );

                  lon       = latCoords.getLongitude() * DPR;
                  lat       = latCoords.getLatitude()  * DPR;
                  radius    = latCoords.getRadius();

                  //
                  // Display the results. 
                  //
                  System.out.format ( 
                           "%n" +
                           "    Surface representation: %s%n" +
                           "%n"                               +
                           "     Radius                   (km)  = %18.10f%n" +
                           "     Planetocentric Latitude  (deg) = %18.10f%n" +
                           "     Planetocentric Longitude (deg) = %18.10f%n" +
                           "     Range                    (km)  = %18.10f%n" +
                           "%n",
                           srftyp[j], radius,  lat,  lon,  dist               );
               } 
               else 
               { 
                  System.out.format ( "%n" +
                                      "Intercept not found.%n" +
                                      "%n"                       );
               } // End of intercept processing for current vector

            } // End of method loop

         } // End of vector loop

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
Surface Intercept Locations for Camera
FOV Boundary and Boresight Vectors

   Instrument:             MGS_MOC_NA
   Epoch:                  2003 OCT 13 06:00:00 UTC
   Aberration correction:  CN+S


Corner vector 1

  Vector in MGS_MOC_NA frame = 
     1.8571383810e-06  -3.8015622659e-03   9.9999277403e-01

  Intercept:

    Surface representation: Ellipsoid

     Radius                   (km)  =    3384.9411357607
     Planetocentric Latitude  (deg) =     -48.4774823672
     Planetocentric Longitude (deg) =    -123.4740748197
     Range                    (km)  =     388.9830822570


    Surface representation: MGS/MOLA topography, 4 pixel/deg

     Radius                   (km)  =    3387.6408267726
     Planetocentric Latitude  (deg) =     -48.4922595600
     Planetocentric Longitude (deg) =    -123.4754119350
     Range                    (km)  =     386.1451004041


Corner vector 2

  Vector in MGS_MOC_NA frame = 
     1.8571383810e-06   3.8015622659e-03   9.9999277403e-01

  Intercept:

    Surface representation: Ellipsoid

     Radius                   (km)  =    3384.9396985743
     Planetocentric Latitude  (deg) =     -48.4816367789
     Planetocentric Longitude (deg) =    -123.3988187487
     Range                    (km)  =     388.9751000527


    Surface representation: MGS/MOLA topography, 4 pixel/deg

     Radius                   (km)  =    3387.6403704508
     Planetocentric Latitude  (deg) =     -48.4963866889
     Planetocentric Longitude (deg) =    -123.4007435481
     Range                    (km)  =     386.1361644332


Corner vector 3

  Vector in MGS_MOC_NA frame = 
    -1.8571383810e-06   3.8015622659e-03   9.9999277403e-01

  Intercept:

    Surface representation: Ellipsoid

     Radius                   (km)  =    3384.9396897287
     Planetocentric Latitude  (deg) =     -48.4816623489
     Planetocentric Longitude (deg) =    -123.3988219550
     Range                    (km)  =     388.9746411355


    Surface representation: MGS/MOLA topography, 4 pixel/deg

     Radius                   (km)  =    3387.6403603146
     Planetocentric Latitude  (deg) =     -48.4964120424
     Planetocentric Longitude (deg) =    -123.4007467292
     Range                    (km)  =     386.1357106985


Corner vector 4

  Vector in MGS_MOC_NA frame = 
    -1.8571383810e-06  -3.8015622659e-03   9.9999277403e-01

  Intercept:

    Surface representation: Ellipsoid

     Radius                   (km)  =    3384.9411269138
     Planetocentric Latitude  (deg) =     -48.4775079405
     Planetocentric Longitude (deg) =    -123.4740779752
     Range                    (km)  =     388.9826233195


    Surface representation: MGS/MOLA topography, 4 pixel/deg

     Radius                   (km)  =    3387.6408166345
     Planetocentric Latitude  (deg) =     -48.4922849169
     Planetocentric Longitude (deg) =    -123.4754150656
     Range                    (km)  =     386.1446466486


Boresight vector

  Vector in MGS_MOC_NA frame = 
     0.0000000000e+00   0.0000000000e+00   1.0000000000e+00

  Intercept:

    Surface representation: Ellipsoid

     Radius                   (km)  =    3384.9404100069
     Planetocentric Latitude  (deg) =     -48.4795802622
     Planetocentric Longitude (deg) =    -123.4364497355
     Range                    (km)  =     388.9757144062


    Surface representation: MGS/MOLA topography, 4 pixel/deg

     Radius                   (km)  =    3387.6402755068
     Planetocentric Latitude  (deg) =     -48.4943418633
     Planetocentric Longitude (deg) =    -123.4380804236
     Range                    (km)  =     386.1376152656

</pre>

</li>
</ol>

   */
   public SurfaceIntercept( String                method,
                            Body                  target,
                            Time                  time,
                            ReferenceFrame        fixRef,
                            AberrationCorrection  abcorr,
                            Body                  observer,
                            ReferenceFrame        rayRef,
                            Vector3               rayDir   )
      throws SpiceException

   {
      //
      // Initialize the Vector3 component of this instance.
      //
      super();

      //
      // Save inputs that may be used in generating "point not found"
      // exceptions.
      //
      this.inputs = new FullInputs( method, target,   time,   fixRef, 
                                    abcorr, observer, rayRef, rayDir );
 
      //
      // Declare arrays to hold outputs from the native method;
      // even scalars are returned in arrays.
      //
      boolean[]      found  = new boolean[1];

      double[]       spoint = new double[3];
      double[]       trgepc = new double[1];
      double[]       trgvec = new double[3];

      //
      // The native method call:
      //
      CSPICE.sincpt ( method,
                      target.getName(),
                      time.getTDBSeconds(),
                      fixRef.getName(),
                      abcorr.getName(),
                      observer.getName(),
                      rayRef.getName(),
                      rayDir.toArray(),
                      spoint,
                      trgepc,
                      trgvec,
                      found             );

      //
      // The outputs become the values of this record's fields.
      //

      wasFound = found[0];

      if ( wasFound )
      {
         surfaceIntercept = new Vector3( spoint    );
         surfaceVector    = new Vector3( trgvec    );
         targetEpoch      = new TDBTime( trgepc[0] );
      }
      else
      {
         surfaceIntercept = null;
         surfaceVector    = null;
         targetEpoch      = null;      
      }
   }

 

   //
   // Methods
   //


   /**
   Vectorized SurfaceIntercept creation method.



<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<ol>
<li> Compute surface intercepts of rays emanating from a set of 
      vertices distributed on a longitude-latitude grid. All 
      vertices are outside the target body, and all rays point 
      toward the target's center. 
<p>
      Check intercepts against expected values. Indicate the 
      number of errors, the number of computations, and the 
      number of intercepts found. 

<p> Use the meta-kernel shown below to load the required SPICE
kernels.
<pre>
KPL/MK
File: SurfaceInterceptCreateEx1.tm

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
 

<p>   Example code begins here. 

<pre>

//
// Program SurfaceInterceptCreateEx1
//
import java.util.ArrayList;
import spice.basic.*;
import static spice.basic.AngularUnits.*;

//
// <p> Multi-segment, vectorized spear program.
//
// <p> This program computes surface intercepts of rays emanating from a set of 
// vertices distributed on a longitude-latitude grid. All 
// vertices are outside the target body, and all rays point 
// toward the target's center. 
//
// <p> The program checks intercepts against expected values. It reports the 
// number of errors, the number of computations, and the 
// number of intercepts found. 
//
// <p> This program expects all loaded DSKs
//     to represent the same body and surface.
//
// <p> Syntax: java -Djava.library.path=<JNISpice path> 
//                   SurfaceInterceptCreateEx1 <meta-kernel>
//
public class SurfaceInterceptCreateEx1
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
      final double                      DTOL   = 1.0e-14;
      final double                      SML    = 1.0e-12;

      final int                         MAXN   = 100000;      
      final int                         MAXSRF = 100;


      //
      // Local variables
      //
      Body                              target;

      DLADescriptor                     dladsc;

      DSK                               dsk;
   
      DSKDescriptor                     dskdsc;

      LatitudinalCoordinates            vtxLatCoords;
      LatitudinalCoordinates            xptLatCoords;
      LatitudinalCoordinates            xxLatCoords;

      ReferenceFrame                    fixref;

      String                            DSKName;
      String                            meta;

      Surface[]                         srflst;

      SurfaceIntercept[]                surfxArr;

      TDBTime                           et;
 
      Vector3[]                         dirArr;
      Vector3[]                         rayVertices;
      Vector3[]                         rayDirections;    
      Vector3[]                         vtxArr;
      Vector3                           xpt;
      Vector3                           xxpt;

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
      double[]                          xhitArr;

      int                               bodyid;
      int                               framid;
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
      int                               nvals;
      int                               surfid;


      try
      {
         //
         // Get meta-kernel name.
         //
         if ( args.length != 1 )
         {
            System.out.println( "Command syntax:  " +
                                "SurfaceInterceptCreateEx1 <meta-kernel>" );
            
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

         polmrg =    0.5;
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
            lon = lonix * lonstp;

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
         // We assume all DSK files referenced in the 
         // meta-kernel have the same body and surface IDs.
         // We could check this using the DSK coverage routines,
         // but for brevity, we won't do so here. We'll create
         // a trivial surface list containing the surface from
         // the first segment of the first loaded DSK file. 
         //
         nsurf     = 1;
         srflst    = new Surface[nsurf];
         srflst[0] = new Surface( surfid, target );

         System.out.println ( "Computing intercepts..." );


         surfxArr = 

           SurfaceIntercept.create ( prioritized, target, srflst, et,
                                     fixref,      vtxArr, dirArr      );
 
         System.out.println ( "Done." );

         //
         // Check results.
         //
         for ( i = 0;  i < nrays;  i++ )
         {
            //
            // Recover the vertex longitude and latitude. These
            // values will be used to generate diagnostic messages,
            // if necessary.
            //
            vtxLatCoords = new LatitudinalCoordinates( vtxArr[i] );

            lon = vtxLatCoords.getLongitude() * DPR;
            lat = vtxLatCoords.getLatitude()  * DPR;


            if ( surfxArr[i].wasFound() )
            {
               //
               // Record the fact that a new intercept was found.
               //
               ++ nhits;

               //
               // Compute the latitude and longitude of         
               // the intercept. Make sure these agree
               // well with those of the vertex.
               //
               xpt          = surfxArr[i].getIntercept();
               xptLatCoords = new LatitudinalCoordinates( xpt );

               //
               // Recover the vertex longitude and latitude. Generate
               // a new point having these coordinates and radius equal
               // to that of xpt.
               //
               vtxLatCoords = new LatitudinalCoordinates( vtxArr[i] );

               xxLatCoords  = 

                 new LatitudinalCoordinates( xptLatCoords.getRadius(),
                                             vtxLatCoords.getLongitude(),
                                             vtxLatCoords.getLatitude()  );


               //
               // Compute the distance between the intercept and the
               // point having latitude and longitude of the ray's vertex,
               // and radius equal to the radius of the intercept.
               //
               xxpt = xxLatCoords.toRectangular();

               d    = xpt.dist( xxpt );

               if ( d/r  >  DTOL )
               {
                  xptArr  = xpt.toArray();
                  xhitArr = xxpt.toArray();

                  System.out.format ( "===========================%n" );
                  System.out.format ( "Lon = %f;  Lat = %f%n",
                           lon, lat                        );
                  System.out.format ( "Bad intercept%n"               );
                  System.out.format ( "Distance error = %e%n", d      );
                  System.out.format ( "xpt    = (%e %e %e)%n",
                           xptArr[0], xptArr[1], xptArr[2] );
                  System.out.format ( "xhitArr = (%e %e %e)%n",
                           xhitArr[0], xhitArr[1], xhitArr[2] );

                  ++ nderr;
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

               System.out.format ( "===========================%n" );
               System.out.format ( "Lon = %f;  Lat = %f%n",
                        lon, lat                                   );
               System.out.format ( "No intercept%n"                );
               return;
            }
         }
       
         System.out.format( "nrays = %d%n", nrays );
         System.out.format( "nhits = %d%n", nhits );
         System.out.format( "nderr = %d%n", nderr );

      } // End of try block

      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }

   } // End of main

}


</pre>
<p> When this program was executed on a PC/Linux/gcc/64-bit/java 1.5
platform, using the meta-kernel shown above, the output was:

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
   public static SurfaceIntercept[] create ( boolean         prioritized, 
                                             Body            target,
                                             Surface[]       surfList,
                                             Time            t,
                                             ReferenceFrame  fixref,
                                             Vector3[]       rayVertices,
                                             Vector3[]       rayDirections )

      throws SpiceException

   {
      //
      // Local variables
      // 
      SpiceErrorException    exc;
      String                 refnam;
      String                 trgnam;

      boolean[]              fndArray;

      double                 et;
      double[][]             raydirs;
      double[][]             vertices;
      double[][]             xptArray;

      int                    i;
      int[]                  srflst;
      int                    ndirs;
      int                    nsurf;
      int                    nrays;

   
      //
      // Get and check vector counts. Trap mismatched array sizes
      // here; delegate other checks to the CSPICE routine.
      //
      nsurf = surfList.length;
      nrays = rayVertices.length;
      ndirs = rayDirections.length;

      if ( ndirs != nrays )
      {
         String errmsg 

            = String.format ( "Number of ray vertices  = %d; "  +
                              "number of ray directions = %d; " +
                              "counts must match but do not.",
                              nrays,
                              ndirs                              );

         exc = SpiceErrorException.create( "SurfaceIntercept.create",
                                           "SPICE(SIZEMISMATCH)",
                                           errmsg                    );
         throw( exc );
      }

      if ( nrays < 1 )
      {
         String errmsg 

            = String.format ( "Number of ray vertices = %d; "  +
                              "count must be > 0.",
                              nrays,
                              ndirs                              );

         exc = SpiceErrorException.create( "SurfaceIntercept.create",
                                           "SPICE(INVALIDCOUNT)",
                                           errmsg                    );
         throw( exc );
      }



      //
      // Fetch scalar inputs.
      //
      et     = t.getTDBSeconds();
      trgnam = target.getName();
      refnam = fixref.getName();

      //
      // Get a list of surface ID codes.
      //
      srflst = new int[nsurf];

      for ( i = 0;  i < nsurf;  i++ )
      {
         srflst[i] = surfList[i].getIDCode();
      }

      //
      // Get a list of ray vertices.
      //
      vertices = new double[nrays][3];

      for ( i = 0;  i < nrays;  i++ )
      {
         vertices[i] = rayVertices[i].toArray();
      }

      //
      // Get a list of ray direction vectors.
      //
      raydirs = new double[nrays][3];

      for ( i = 0;  i < nrays;  i++ )
      {
         raydirs[i] = rayDirections[i].toArray();
      }

      //
      // Allocate space for the output arrays.
      //
      xptArray = new double [ nrays ][3];
      
      for ( i = 0; i < nrays; i++ )
      {
         xptArray[i] = new double[3];
      }

      fndArray = new boolean[ nrays ];

      //
      // Let CSPICE find the solution.
      //
      CSPICE.dskxv ( prioritized, trgnam,   nsurf,   srflst, 
                     et,          refnam,   nrays,   vertices,
                     raydirs,     xptArray, fndArray          );

      SurfaceIntercept[] intercepts = new SurfaceIntercept[nrays];

      for ( i = 0;  i < nrays;  i++ )
      {
         intercepts[i]                  = new SurfaceIntercept();
         intercepts[i].surfaceIntercept = new Vector3( xptArray[i] );
         intercepts[i].wasFound         = fndArray[i];
         intercepts[i].targetEpoch      = new TDBTime( t );

         if ( intercepts[i].wasFound )
         {
            intercepts[i].surfaceVector 

               = rayVertices[i].sub( intercepts[i].surfaceIntercept );
         }

         //
         // Save inputs that may be used in generating "point not found"
         // exceptions.
         //
         // The peculiar syntax below generates an inner class instance, when
         // the instance is being created by a static method of the enclosing
         // class.
         //
         //
         SurfaceIntercept.RayArrayInputs rayinputs = 

            intercepts[i].new RayArrayInputs();
         
         rayinputs.prioritized = prioritized;
         rayinputs.target      = target;
         rayinputs.time        = t;
         rayinputs.index       = i;

         intercepts[i].inputs  = rayinputs; 
      }
   
      return( intercepts );

   }




   /**
   Indicate whether the intercept was found.
   */
   public boolean wasFound()
   {
      return ( wasFound );
   }


   /**
   Return the surface intercept.
   */
   public Vector3 getIntercept()

      throws PointNotFoundException, SpiceException
   {
      if ( !wasFound )
      {
         throwNotFoundExc();
      }

      return (  new Vector3(surfaceIntercept)  );
   }

   /**
   Return the target epoch.
   */
   public TDBTime getTargetEpoch()

      throws PointNotFoundException, SpiceException

   {
      if ( !wasFound )
      {
         throwNotFoundExc();
      }

      return (  new TDBTime(targetEpoch)  );
   }

   /**
   Return the observer to intercept vector.
   */
   public Vector3 getSurfaceVector()

      throws PointNotFoundException, SpiceException

   {
      if ( !wasFound )
      {
         throwNotFoundExc();
      }

      return (  new Vector3(surfaceVector)  );
   }


   //
   // Package private methods
   //
   void throwNotFoundExc()

      throws PointNotFoundException, SpiceException

   {
      String msg = "Ray did not intersect surface; intercept is undefined." + 
                   endl + endl  + 
                   this.inputs.getString();

      PointNotFoundException exc 

          = PointNotFoundException.create( this.getClass().toString(), (msg) );

      throw( exc );
   }
}
