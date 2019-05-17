
package spice.basic;


/**
Class SubSolarRecord supports sub-solar point computations.

<p> A SubSolarRecord instance consists of
<ul>
<li> A 3-dimensional vector representing the sub-solar point.</li>
<li> The epoch of participation of the target body.</li>
<li> The vector from the observer to the sub-solar point,
expressed in the target body-fixed, body-centered reference frame,
evaluated at the target body's epoch of participation.
</ul> 

<p> The principal method for computing sub-illumination source
points is the constructor 
{@link #SubSolarRecord(String,Body,Time,ReferenceFrame,
AberrationCorrection,Body)}. See documentation of this
constructor for code examples.



<h2> Files </h2>


<p>Appropriate SPICE kernels must be loaded by the calling program
   before methods of this class are called.

<p>The following data are required:

<ul>
<li>
        SPK data: the calling application must load ephemeris data
        for the target, observer, and sun. If aberration
        corrections are used, the states of the target body, the
        observer, and the sun relative to the solar system barycenter must be
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
           pool. Typically the definition is supplied by loading a
           frame kernel via KernelDatabase.load.
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





<h2> Class SubSolarRecord Particulars </h2>

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



<h3> Version 2.0 10-JAN-2017 (NJB)</h3>

<p> This class now supports DSK-based target body surface
representations. 

<p> This class is now derived from class Vector3.

<p>Changed name of constructor input argument from 
`solar' to `obsrvr'.

<h3> Version 1.0.0 14-JUL-2009 (NJB)</h3>
*/
public class SubSolarRecord extends Vector3
{
   //
   // Public Constants
   //

   //
   // The values below are the geometric "methods" supported by
   // the SubSolarRecord constructor.
   //
   public final static String NEAR_POINT_ELLIPSOID =

      "NEAR POINT: ELLIPSOID";


   public final static String INTERCEPT_ELLIPSOID =

      "INTERCEPT: ELLIPSOID";



   //
   // Fields
   //
   private TDBTime          targetEpoch;
   private Vector3          surfaceVector;


   //
   // Constructors
   //

   /**
   No-arguments constructor.
   */
   public SubSolarRecord()
   {
      super();
   }

   /**
   Copy constructor. This method creates a deep copy.
   */   
   public SubSolarRecord( SubSolarRecord ssr )

      throws SpiceException
   {
      v                  = ssr.toArray();

      this.targetEpoch   = new TDBTime ( ssr.targetEpoch   );
      this.surfaceVector = new Vector3 ( ssr.surfaceVector );
   }

   /**
   Find a specified sub-solar point; create a record
   containing the result.

   <h3>Code Examples</h3>

<p> 
   The numerical results shown for these examples may differ across 
   platforms. The results depend on the SPICE kernels used as 
   input, the compiler and supporting libraries, and the machine  
   specific arithmetic implementation.  
 
 
   <ol>

   <li> 
      Find the sub-solar point on Mars as seen from the Earth for a 
      specified time.  
 
      <p> Compute the sub-solar point using both triaxial ellipsoid 
      and topographic surface models. Topography data are provided by 
      a DSK file. For the ellipsoid model, use both the "intercept" 
      and "near point" sub-observer point definitions; for the DSK 
      case, use both the "intercept" and "nadir" definitions. 
 
      <p> Display the locations of both the sun and the sub-solar 
      point relative to the center of Mars, in the IAU_MARS 
      body-fixed reference frame, using both planetocentric and 
      planetographic coordinates. 
 
      <p> The topographic model is based on data from the MGS MOLA DEM 
      megr90n000cb, which has a resolution of 4 pixels/degree. A 
      triangular plate model was produced by computing a 720 x 1440 
      grid of interpolated heights from this DEM, then tessellating 
      the height grid. The plate model is stored in a type 2 segment 
      in the referenced DSK file. 

<p>  Use the meta-kernel shown below to load the required SPICE
     kernels.

<pre>

KPL/MK 

File: SubSolarRecordEx1.tm 

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
   megr90n000cb_plate.bds           Plate model based on 
                                    MEGDR DEM, resolution 
                                    4 pixels/degree. 

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
// Program SubSolarRecordEx1
//

import spice.basic.*;
import static spice.basic.AngularUnits.*;

//
// Find the sub-solar point on Mars as seen from the Earth for a 
// specified time.  
//
public class SubSolarRecordEx1
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
      final String                      META   = "SubSolarRecordEx1.tm";
      final int                         NMETH  = 4;

      //
      // Local variables
      //
      AberrationCorrection              abcorr = 
                                           new AberrationCorrection( "CN+S" );

      Body                              Sun    = new Body( "Sun"   );
      Body                              obsrvr = new Body( "Earth" );
      Body                              target = new Body( "Mars"  );
      
      LatitudinalCoordinates            latCoordsObs;
      LatitudinalCoordinates            latCoordsSub;

      PlanetographicCoordinates         pgrCoordsObs;
      PlanetographicCoordinates         pgrCoordsSub;

      ReferenceFrame                    fixref =
                                           new ReferenceFrame( "IAU_MARS" );

      StateVector                       sunst;

      String                            refloc = "OBSERVER";
 
      String[]                          submth =  
                                        {
                                           "Intercept/ellipsoid",
                                           "Near point/ellipsoid",
                                           "Intercept/DSK/Unprioritized",
                                           "Nadir/DSK/Unprioritized"
                                        };

      String                            tdbstr = "2008 AUG 11 00:00:00 UTC";

      SubSolarRecord                    subrec;

      TDBTime                           et;
      TDBTime                           trgepc;

      Vector3                           srfvec;
      Vector3                           sunpos;

      double                            dist;  
      double                            f;
      double                            opclat;
      double                            opclon;
      double                            opgalt;
      double                            opglat;
      double                            opglon;
      double[]                          radii;
      double                            re;
      double                            rp;
      double                            spclat;
      double                            spclon;
      double                            spgalt;
      double                            spglat;
      double                            spglon;
      double                            sunlt;
      double                            supcln;
      double                            supclt;
      double                            supgal;
      double                            supgln;
      double                            supglt;
   
      int                               i;
      int                               n;


      try
      {
         //
         // Load kernels.
         //
         KernelDatabase.load( META );

         //
         // Convert the UTC request time string to seconds past
         // J2000, TDB, represented by a TDBTime instance.
         //
         et = new TDBTime( tdbstr );
        
         //
         // Look up the target body's radii. We'll use these to
         // convert Cartesian to planetographic coordinates. Use
         // the radii to compute the flattening coefficient of
         // the reference ellipsoid.
         //
         radii = target.getValues( "RADII" );

         //
         // Let `re and `rp' be, respectively, the equatorial and
         // polar radii of the target.
         //
         re = radii[0];
         rp = radii[2];

         f  = ( re - rp ) / re;

         //
         // Compute the sub-solar point using light time and stellar
         // aberration corrections. Use both ellipsoid and DSK 
         // shape models, and use all of the "near point," 
         // "intercept," and "nadir" sub-observer point definitions. 
         //
         for ( i = 0;  i < NMETH;  i++ )
         {
            System.out.format ( "%nSub-solar point computation " +
                                "method = %s%n",  submth[i]           );

            subrec = new SubSolarRecord ( submth[i], target, et, 
                                          fixref,    abcorr, obsrvr ); 

            trgepc = subrec.getTargetEpoch();

            //
            // Convert the sub-solar point's rectangular coordinates to
            // planetographic longitude, latitude and altitude. 
            // Convert radians to degrees.
            //
            pgrCoordsSub = 

               new PlanetographicCoordinates( target, subrec, re, f );

            spgalt = pgrCoordsSub.getAltitude();
            spglon = pgrCoordsSub.getLongitude() * DPR;
            spglat = pgrCoordsSub.getLatitude()  * DPR;

               
            //
            // Convert the sub-solar point's rectangular coordinates to
            // planetocentric latitude and longitude. Convert radians to 
            // degrees.
            //
            latCoordsSub = new LatitudinalCoordinates( subrec );

            spclon    = latCoordsSub.getLongitude() * DPR; 
            spclat    = latCoordsSub.getLatitude()  * DPR; 
 
            //
            // Compute the Sun's apparent position relative to the 
            // sub-solar point at `trgepc'. Add the position of the
            // sub-solar point relative to the target's center to
            // obtain the position of the sun relative to the target's
            // center. Express the latter position in planetographic 
            // coordinates.
            //
            
            sunst  = new StateVector( Sun,    trgepc, fixref, refloc,
                                      abcorr, subrec, target, fixref );

            sunpos = (sunst.getPosition()).add( subrec );
           
            pgrCoordsObs = 

               new PlanetographicCoordinates( target, sunpos, re, f );

            supgln = pgrCoordsObs.getLongitude() * DPR;
            supglt = pgrCoordsObs.getLatitude()  * DPR;


            //
            // Convert the sun's rectangular coordinates to
            // planetocentric radius, longitude, and latitude.
            // Convert radians to degrees.
            //
            latCoordsObs = new LatitudinalCoordinates( sunpos );

            supcln    = latCoordsObs.getLongitude() * DPR; 
            supclt    = latCoordsObs.getLatitude()  * DPR; 

            //
            // Write the results.
            // 
            System.out.format( 
                "%n"                                                    +
                " Computation method = %s%n%n"                          +
                "  Sub-solar point altitude            (km) = %21.9f\n" +
                "  Sub-solar planetographic longitude (deg) = %21.9f\n" +
                "  Sun's planetographic longitude     (deg) = %21.9f\n" +
                "  Sub-solar planetographic latitude  (deg) = %21.9f\n" +
                "  Sun's planetographic latitude      (deg) = %21.9f\n" +
                "  Sub-solar planetocentric longitude (deg) = %21.9f\n" +
                "  Sun's planetocentric longitude     (deg) = %21.9f\n" +
                "  Sub-solar planetocentric latitude  (deg) = %21.9f\n" +
                "  Sun's planetocentric latitude      (deg) = %21.9f\n" +
                "%n",
                submth[i], 
                spgalt,
                spglon,
                supgln, 
                spglat,
                supglt, 
                spclon, 
                supcln,
                spclat,
                supclt      );
                             
 
         } // End of method loop

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

Sub-solar point computation method = Intercept/ellipsoid

 Computation method = Intercept/ellipsoid

  Sub-solar point altitude            (km) =           0.000000000
  Sub-solar planetographic longitude (deg) =         175.810675508
  Sun's planetographic longitude     (deg) =         175.810675508
  Sub-solar planetographic latitude  (deg) =          23.668550281
  Sun's planetographic latitude      (deg) =          23.420823362
  Sub-solar planetocentric longitude (deg) =        -175.810675508
  Sun's planetocentric longitude     (deg) =        -175.810675508
  Sub-solar planetocentric latitude  (deg) =          23.420819936
  Sun's planetocentric latitude      (deg) =          23.420819936


Sub-solar point computation method = Near point/ellipsoid

 Computation method = Near point/ellipsoid

  Sub-solar point altitude            (km) =           0.000000000
  Sub-solar planetographic longitude (deg) =         175.810675408
  Sun's planetographic longitude     (deg) =         175.810675408
  Sub-solar planetographic latitude  (deg) =          23.420823362
  Sun's planetographic latitude      (deg) =          23.420823362
  Sub-solar planetocentric longitude (deg) =        -175.810675408
  Sun's planetocentric longitude     (deg) =        -175.810675408
  Sub-solar planetocentric latitude  (deg) =          23.175085578
  Sun's planetocentric latitude      (deg) =          23.420819936


Sub-solar point computation method = Intercept/DSK/Unprioritized

 Computation method = Intercept/DSK/Unprioritized

  Sub-solar point altitude            (km) =          -4.052254284
  Sub-solar planetographic longitude (deg) =         175.810675512
  Sun's planetographic longitude     (deg) =         175.810675512
  Sub-solar planetographic latitude  (deg) =          23.668848891
  Sun's planetographic latitude      (deg) =          23.420823362
  Sub-solar planetocentric longitude (deg) =        -175.810675512
  Sun's planetocentric longitude     (deg) =        -175.810675512
  Sub-solar planetocentric latitude  (deg) =          23.420819936
  Sun's planetocentric latitude      (deg) =          23.420819936


Sub-solar point computation method = Nadir/DSK/Unprioritized

 Computation method = Nadir/DSK/Unprioritized

  Sub-solar point altitude            (km) =          -4.022302438
  Sub-solar planetographic longitude (deg) =         175.810675412
  Sun's planetographic longitude     (deg) =         175.810675412
  Sub-solar planetographic latitude  (deg) =          23.420823362
  Sun's planetographic latitude      (deg) =          23.420823362
  Sub-solar planetocentric longitude (deg) =        -175.810675412
  Sun's planetocentric longitude     (deg) =        -175.810675412
  Sub-solar planetocentric latitude  (deg) =          23.174793924
  Sun's planetocentric latitude      (deg) =          23.420819936


</pre>

   </li>


   */
   public SubSolarRecord( String                method,
                          Body                  target,
                          Time                  t,
                          ReferenceFrame        fixref,
                          AberrationCorrection  abcorr,
                          Body                  obsrvr )

      throws SpiceException

   {
      //
      // Declare arrays to hold outputs from the native method;
      // even scalars are returned in arrays.
      //

      double[]       trgepc = new double[1];
      double[]       spoint = new double[3];
      double[]       trgvec = new double[3];

      //
      // The native method call:
      //
      CSPICE.subslr ( method,
                      target.getName(),
                      t.getTDBSeconds(),
                      fixref.getName(),
                      abcorr.getName(),
                      obsrvr.getName(),
                      spoint,
                      trgepc,
                      trgvec             );

      //
      // The outputs become the values of this record's fields.
      //
      v             = spoint;
      targetEpoch   = new TDBTime( trgepc[0] );
      surfaceVector = new Vector3( trgvec    );
   }


   //
   // Methods
   //

   /**
   Return the sub-solar point.
   */
   public Vector3 getSubPoint()
   {
      return (  new Vector3(this)  );
   }

   /**
   Return the target epoch.
   */
   public TDBTime getTargetEpoch()
   {
      return (  new TDBTime(targetEpoch)  );
   }

   /**
   Return the observer to sub-solar point vector.
   */
   public Vector3 getSurfaceVector()
   {
      return (  new Vector3(surfaceVector)  );
   }

}
