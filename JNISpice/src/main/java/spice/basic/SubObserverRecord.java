
package spice.basic;

/**
Class SubObserverRecord supports sub-observer point
computations.

<p> A SubObserverRecord instance consists of
<ul>
<li> A 3-dimensional vector representing the sub-observer point.</li>
<li> The epoch of participation of the target body.</li>
<li> The vector from the observer to the sub-observer point,
expressed in the target body-fixed, body-centered reference frame,
evaluated at the target body's epoch of participation.
</ul> 

<p> See the detailed documentation of constructor
{@link #SubObserverRecord(String, Body, Time, ReferenceFrame,
AberrationCorrection, Body)} for code examples.


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




<h2> Class SubObserverRecord Particulars </h2>

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
Upgraded to support DSK-based surface representations.

<p> This class now is derived from class Vector3.

<h3> Version 1.0.0 22-NOV-2009 (NJB) </h3>
*/
public class SubObserverRecord extends Vector3
{
   //
   // Public Constants
   //

   //
   // The values below are the geometric "methods" supported by
   // the SubObserverRecord constructor.
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
   Compute a specified sub-observer point; create a record
   containing the result.
 
   <h3>Code Examples</h3>

<p> 
   The numerical results shown for these examples may differ across 
   platforms. The results depend on the SPICE kernels used as 
   input, the compiler and supporting libraries, and the machine  
   specific arithmetic implementation.  
 
 
   <ol>

   <li> Find the sub-Earth point on Mars for a specified time. 
 
  <p> Compute the sub-Earth points using both triaxial ellipsoid 
      and topographic surface models. Topography data are provided by 
      a DSK file. For the ellipsoid model, use both the "intercept" 
      and "near point" sub-observer point definitions; for the DSK 
      case, use both the "intercept" and "nadir" definitions. 
 
  <p> Display the locations of both the Earth and the sub-Earth 
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

File: SubObserverRecordEx1.tm 

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
// Program SubObserverRecordEx1
//

import spice.basic.*;
import static spice.basic.AngularUnits.*;

//
// Find the sub-Earth point on Mars for a specified time. 
//
public class SubObserverRecordEx1
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
      final String                      META   = "SubObserverRecordEx1.tm";
      final int                         NMETH  = 4;

      //
      // Local variables
      //
      AberrationCorrection              abcorr = 
                                           new AberrationCorrection( "CN+S" );

      Body                              obsrvr = new Body( "Earth" );
      Body                              target = new Body( "Mars"  );
      
      LatitudinalCoordinates            latCoordsObs;
      LatitudinalCoordinates            latCoordsSub;

      PlanetographicCoordinates         pgrCoordsObs;
      PlanetographicCoordinates         pgrCoordsSub;

      ReferenceFrame                    fixref =
                                           new ReferenceFrame( "IAU_MARS" );
 
      String[]                          submth =  
                                        {
                                           "Intercept/ellipsoid",
                                           "Near point/ellipsoid",
                                           "Intercept/DSK/Unprioritized",
                                           "Nadir/DSK/Unprioritized"
                                        };

      String                            tdbstr = "2008 AUG 11 00:00:00 UTC";

      SubObserverRecord                 subrec;

      TDBTime                           et;

      Vector3                           obspos;
      Vector3                           srfvec;

      double                            dist;  
      double                            f;
      double                            odist;
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
      double                            spcrad;
      double                            spgalt;
      double                            spglat;
      double                            spglon;

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
         // Compute the sub-observer point using light time and stellar
         // aberration corrections. Use both ellipsoid and DSK 
         // shape models, and use all of the "near point," 
         // "intercept," and "nadir" sub-observer point definitions. 
         //
         for ( i = 0;  i < NMETH;  i++ )
         {
            System.out.format ( "%nSub-observer point computation " +
                                "method = %s%n",  submth[i]           );

            subrec = new SubObserverRecord ( submth[i], target, et, 
                                             fixref,    abcorr, obsrvr ); 
            //
            // Compute the observer's distance from `subrec'.
            //
            srfvec = subrec.getSurfaceVector();
            odist  = srfvec.norm();  

            //
            // Convert sub-observer point rectangular coordinates to
            // planetographic longitude, latitude and altitude. 
            // Convert radians to degrees.
            //
            pgrCoordsSub = 

               new PlanetographicCoordinates( target, subrec, re, f );

            spglon = pgrCoordsSub.getLongitude() * DPR;
            spglat = pgrCoordsSub.getLatitude()  * DPR;
            spgalt = pgrCoordsSub.getAltitude();

               
            //
            // Convert sub-observer point rectangular coordinates to
            // planetocentric latitude and longitude. Convert radians to 
            // degrees.
            //
            latCoordsSub = new LatitudinalCoordinates( subrec );

            spcrad    = latCoordsSub.getRadius();
            spclon    = latCoordsSub.getLongitude() * DPR; 
            spclat    = latCoordsSub.getLatitude()  * DPR; 
 
            //
            // Compute the observer's position relative to the center
            // of the target, where the center's location has been
            // adjusted using the aberration corrections applicable
            // to the sub-point. Express the observer's location in
            // planetographic coordinates.
            //
            obspos = subrec.sub( srfvec );

            pgrCoordsObs = 

               new PlanetographicCoordinates( target, obspos, re, f );

            opglon = pgrCoordsObs.getLongitude() * DPR;
            opglat = pgrCoordsObs.getLatitude()  * DPR;
            opgalt = pgrCoordsObs.getAltitude();

            //
            // Convert the observer's rectangular coordinates to
            // planetocentric radius, longitude, and latitude.
            // Convert radians to degrees.
            //
            latCoordsObs = new LatitudinalCoordinates( obspos );

            opclon    = latCoordsObs.getLongitude() * DPR; 
            opclat    = latCoordsObs.getLatitude()  * DPR; 


            //
            // Write the results.
            // 
            System.out.format( 
                "%n"                                                       +
                " Computation method = %s%n%n"                             +
                "  Observer altitude relative to spheroid (km) = %21.9f%n" +
                "  Length of SRFVEC                       (km) = %21.9f%n" +
                "  Sub-observer point altitude            (km) = %21.9f%n" +
                "  Sub-observer planetographic longitude (deg) = %21.9f%n" +
                "  Observer planetographic longitude     (deg) = %21.9f%n" +
                "  Sub-observer planetographic latitude  (deg) = %21.9f%n" +
                "  Observer planetographic latitude      (deg) = %21.9f%n" +
                "  Sub-observer planetocentric longitude (deg) = %21.9f%n" +
                "  Observer planetocentric longitude     (deg) = %21.9f%n" +
                "  Sub-observer planetocentric latitude  (deg) = %21.9f%n" +
                "  Observer planetocentric latitude      (deg) = %21.9f%n" +
                "%n",
                submth[i], 
                opgalt,
                odist,
                spgalt, 
                spglon,
                opglon, 
                spglat, 
                opglat, 
                spclon, 
                opclon,
                spclat,
                opclat      );
                             
 
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

Sub-observer point computation method = Intercept/ellipsoid

 Computation method = Intercept/ellipsoid

  Observer altitude relative to spheroid (km) =   349199089.540947000
  Length of SRFVEC                       (km) =   349199089.577642700
  Sub-observer point altitude            (km) =           0.000000000
  Sub-observer planetographic longitude (deg) =         199.302305029
  Observer planetographic longitude     (deg) =         199.302305029
  Sub-observer planetographic latitude  (deg) =          26.262401237
  Observer planetographic latitude      (deg) =          25.994936751
  Sub-observer planetocentric longitude (deg) =         160.697694971
  Observer planetocentric longitude     (deg) =         160.697694971
  Sub-observer planetocentric latitude  (deg) =          25.994934171
  Observer planetocentric latitude      (deg) =          25.994934171


Sub-observer point computation method = Near point/ellipsoid

 Computation method = Near point/ellipsoid

  Observer altitude relative to spheroid (km) =   349199089.540938700
  Length of SRFVEC                       (km) =   349199089.540938700
  Sub-observer point altitude            (km) =          -0.000000000
  Sub-observer planetographic longitude (deg) =         199.302305029
  Observer planetographic longitude     (deg) =         199.302305029
  Sub-observer planetographic latitude  (deg) =          25.994936751
  Observer planetographic latitude      (deg) =          25.994936751
  Sub-observer planetocentric longitude (deg) =         160.697694971
  Observer planetocentric longitude     (deg) =         160.697694971
  Sub-observer planetocentric latitude  (deg) =          25.729407227
  Observer planetocentric latitude      (deg) =          25.994934171


Sub-observer point computation method = Intercept/DSK/Unprioritized

 Computation method = Intercept/DSK/Unprioritized

  Observer altitude relative to spheroid (km) =   349199089.541017230
  Length of SRFVEC                       (km) =   349199091.785406700
  Sub-observer point altitude            (km) =          -2.207669751
  Sub-observer planetographic longitude (deg) =         199.302304999
  Observer planetographic longitude     (deg) =         199.302304999
  Sub-observer planetographic latitude  (deg) =          26.262576677
  Observer planetographic latitude      (deg) =          25.994936751
  Sub-observer planetocentric longitude (deg) =         160.697695001
  Observer planetocentric longitude     (deg) =         160.697695001
  Sub-observer planetocentric latitude  (deg) =          25.994934171
  Observer planetocentric latitude      (deg) =          25.994934171


Sub-observer point computation method = Nadir/DSK/Unprioritized

 Computation method = Nadir/DSK/Unprioritized

  Observer altitude relative to spheroid (km) =   349199089.541007700
  Length of SRFVEC                       (km) =   349199091.707172300
  Sub-observer point altitude            (km) =          -2.166164622
  Sub-observer planetographic longitude (deg) =         199.302305000
  Observer planetographic longitude     (deg) =         199.302305000
  Sub-observer planetographic latitude  (deg) =          25.994936752
  Observer planetographic latitude      (deg) =          25.994936751
  Sub-observer planetocentric longitude (deg) =         160.697695000
  Observer planetocentric longitude     (deg) =         160.697695000
  Sub-observer planetocentric latitude  (deg) =          25.729237570
  Observer planetocentric latitude      (deg) =          25.994934171

</pre>

   </li>




   <li>
 Use this constructor to find the sub-spacecraft point on Mars for the 
      Mars Reconnaissance Orbiter spacecraft (MRO) at a specified time,
      using both the 'Ellipsoid/Near point' computation method and an
      ellipsoidal target shape, and the "DSK/Unprioritized/Nadir"
      method and a DSK-based shape model.
 
 <p>  Use both LT+S and CN+S aberration corrections to illustrate 
      the differences. 
  
 <p>  Convert the spacecraft to sub-observer point vector obtained from
      this constructor into the MRO_HIRISE_LOOK_DIRECTION reference frame at
      the observation time. Perform a consistency check with this
      vector: compare the Mars surface intercept of the ray emanating
      from the spacecraft and pointed along this vector with the
      sub-observer point.
 
 <p>  Perform the sub-observer point and surface intercept computations
      using both triaxial ellipsoid and topographic surface models.
 
 <p>  For this example, the topographic model is based on the MGS MOLA
      DEM megr90n000eb, which has a resolution of 16 pixels/degree.
      Eight DSKs, each covering longitude and latitude ranges of 90
      degrees, were made from this data set. For the region covered by
      a given DSK, a grid of approximately 1500 x 1500 interpolated
      heights was produced, and this grid was tessellated using
      approximately 4.5 million triangular plates, giving a total plate
      count of about 36 million for the entire DSK set.
 
 <p>  All DSKs in the set use the surface ID code 499001, so there is
      no need to specify the surface ID in the `method' strings passed
      to the SubObserverRecord and SurfaceIntercept constructors.
 
 <p>  Use the meta-kernel shown below to load the required SPICE
      kernels.
<pre>
KPL/MK

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
   mro_psp4_ssd_mro95a.bsp          MRO ephemeris
   mro_v11.tf                       MRO frame specifications
   mro_sclkscet_00022_65536.tsc     MRO SCLK coefficients
                                    parameters
   mro_sc_psp_070925_071001.bc      MRO attitude
   megr90n000eb_*_plate.bds         Plate model DSKs based
                                    on MEGDR DEM, resolution
                                    16 pixels/degree.

\begindata

   KERNELS_TO_LOAD = (

      'de430.bsp',
      'mar097.bsp',
      'pck00010.tpc',
      'naif0012.tls',
      'mro_psp4_ssd_mro95a.bsp',
      'mro_v11.tf',
      'mro_sclkscet_00022_65536.tsc',
      'mro_sc_psp_070925_071001.bc',
      'megr90n000eb_LL000E00N_UR090E90N_plate.bds'
      'megr90n000eb_LL000E90S_UR090E00S_plate.bds'
      'megr90n000eb_LL090E00N_UR180E90N_plate.bds'
      'megr90n000eb_LL090E90S_UR180E00S_plate.bds'
      'megr90n000eb_LL180E00N_UR270E90N_plate.bds'
      'megr90n000eb_LL180E90S_UR270E00S_plate.bds'
      'megr90n000eb_LL270E00N_UR360E90N_plate.bds'
      'megr90n000eb_LL270E90S_UR360E00S_plate.bds'  )

\begintext
</pre>



<p> Example code begins here.

<pre>
 //
// Program SubObserverRecordEx2
//

import spice.basic.*;
import static spice.basic.AngularUnits.*;

//
// This program finds the sub-spacecraft point on Mars for the 
// Mars Reconnaissance Orbiter spacecraft (MRO) at a specified time,
// using both the 'Ellipsoid/Near point' computation method and an
// ellipsoidal target shape, and the "DSK/Unprioritized/Nadir"
// method and a DSK-based shape model. 
//
public class SubObserverRecordEx2
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
      final String                      META   = "SubObserverRecordEx2.tm";
      final int                         NCORR  = 2;
      final int                         NMETH  = 2;

      //
      // Local variables
      //
      AberrationCorrection[]            abcorr = {
                                           new AberrationCorrection( "LT+S" ),
                                           new AberrationCorrection( "CN+S" ) };

      Body                              obsrvr = new Body( "MRO"  );
      Body                              target = new Body( "Mars" );

      LatitudinalCoordinates            latCoords;

      Matrix33                          xform;

      ReferenceFrame                    fixref =
                                           new ReferenceFrame( "IAU_MARS" );

      ReferenceFrame                    hiref = 
                                           new ReferenceFrame( 
                                             "MRO_HIRISE_LOOK_DIRECTION" );

      String[]                          sinmth = { "Ellipsoid",
                                                   "DSK/Unprioritized" };
 
      String[]                          submth = { "Ellipsoid/Near point",
                                                   "DSK/Unprioritized/Nadir" };

      String                            tdbstr = "2007 SEP 30 00:00:00 TDB";

      SubObserverRecord                 subrec;

      SurfaceIntercept                  surfx;

      TDBTime                           et;
      TDBTime                           trgepc;

      Vector3                           mrovec;
      Vector3                           srfvec;

      boolean                           found;

      double                            alt;
      double                            dist;  
      double                            lat;
      double                            lon;
      double                            radius;

      int                               i;
      int                               j;


      try
      {
         //
         // Load kernels.
         //
         KernelDatabase.load( META );

         //
         // Convert the TDB request time string to ET (seconds past
         // J2000, TDB), represented by a TDBTime instance.
         //
         et = new TDBTime( tdbstr );
        
         //
         // Compute the sub-spacecraft point using each method. 
         // Compute the results using both LT+S and CN+S aberration 
         // corrections.
         //
         for ( i = 0;  i < NMETH;  i++ )
         {
            System.out.format ( "%nSub-observer point computation " +
                                "method = %s%n",  submth[i]           );

            for ( j = 0;  j < NCORR;  j++ )
            {
               subrec 

                  = new SubObserverRecord ( submth[i], target,    et, 
                                            fixref,    abcorr[j], obsrvr ); 
               //
               // Compute the observer's altitude above `subrec'.
               //
               srfvec = subrec.getSurfaceVector();
               alt    = srfvec.norm();  

               //
               // Express `srfvec' in the MRO_HIRISE_LOOK_DIRECTION
               // reference frame at epoch `et'. Since `srfvec' is expressed
               // relative to the IAU_MARS frame at `trgepc', we must
               // call getPositionTransformation(ReferenceFrame,
               // Time,Time) to compute the position transformation matrix
               // from IAU_MARS at `trgepc' to the MRO_HIRISE_LOOK_DIRECTION
               // frame at time `et'.
               //
               trgepc = subrec.getTargetEpoch();
               
               xform  = fixref.getPositionTransformation( hiref, trgepc, et );

               mrovec = xform.mxv( srfvec );
               
               //
               // Convert sub-observer point rectangular coordinates to
               // planetocentric latitude and longitude. Convert radians to 
               // degrees.
               //
               latCoords = new LatitudinalCoordinates( subrec );

               radius    = latCoords.getRadius();
               lon       = latCoords.getLongitude() * DPR; 
               lat       = latCoords.getLatitude()  * DPR; 

               //
               // Write the results.
               // 
               System.out.format( 
                          "%n"                                               +
                          "   Aberration correction = %s%n%n"                +
                          "      MRO-to-sub-observer vector in%n"            +
                          "      MRO HIRISE look direction frame%n"          +
                          "         X-component             (km) = %21.9f%n" +
                          "         Y-component             (km) = %21.9f%n" +
                          "         Z-component             (km) = %21.9f%n" +
                          "      Sub-observer point radius  (km) = %21.9f%n" +
                          "      Planetocentric latitude   (deg) = %21.9f%n" +
                          "      Planetocentric longitude  (deg) = %21.9f%n" +
                          "      Observer altitude          (km) = %21.9f%n",
                          abcorr[j],
                          mrovec.getElt(0),
                          mrovec.getElt(1),
                          mrovec.getElt(2),         
                          radius,
                          lat,
                          lon,
                          alt         );

               //
               // Consistency check: find the surface intercept on
               // Mars of the ray emanating from the spacecraft and having
               // direction vector `mrovec' in the MRO HIRISE look direction
               // reference frame at `et'. Call the intercept point
               // `xpoint'. `xpoint' should coincide with `subrec', up to a
               // small round-off error.
               //
               surfx = new SurfaceIntercept( sinmth[i], target, et,    fixref,  
                                             abcorr[j], obsrvr, hiref, mrovec );
                            
               if ( !surfx.wasFound() )
               {
                  System.out.format ( "Bug: no intercept%n" );        
               }
               else
               {
                  //
                  // Report the distance between `surfx' and `subrec'.
                  //
                  System.out.format( "      Intercept comparison error " +
                                     "(km) = %21.9f\n\n",
                                     surfx.getIntercept().dist( subrec )  );
               }

            } // End of aberration correction loop

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
Sub-observer point computation method = Ellipsoid/Near point

   Aberration correction = LT+S

      MRO-to-sub-observer vector in
      MRO HIRISE look direction frame
         X-component             (km) =           0.286933229
         Y-component             (km) =          -0.260425939
         Z-component             (km) =         253.816326386
      Sub-observer point radius  (km) =        3388.299078378
      Planetocentric latitude   (deg) =         -38.799836378
      Planetocentric longitude  (deg) =        -114.995297227
      Observer altitude          (km) =         253.816622175
      Intercept comparison error (km) =           0.000002144


   Aberration correction = CN+S

      MRO-to-sub-observer vector in
      MRO HIRISE look direction frame
         X-component             (km) =           0.286933107
         Y-component             (km) =          -0.260426683
         Z-component             (km) =         253.816315915
      Sub-observer point radius  (km) =        3388.299078376
      Planetocentric latitude   (deg) =         -38.799836382
      Planetocentric longitude  (deg) =        -114.995297449
      Observer altitude          (km) =         253.816611705
      Intercept comparison error (km) =           0.000000001


Sub-observer point computation method = DSK/Unprioritized/Nadir

   Aberration correction = LT+S

      MRO-to-sub-observer vector in
      MRO HIRISE look direction frame
         X-component             (km) =           0.282372596
         Y-component             (km) =          -0.256289313
         Z-component             (km) =         249.784871247
      Sub-observer point radius  (km) =        3392.330239436
      Planetocentric latitude   (deg) =         -38.800230156
      Planetocentric longitude  (deg) =        -114.995297338
      Observer altitude          (km) =         249.785162334
      Intercept comparison error (km) =           0.000002412


   Aberration correction = CN+S

      MRO-to-sub-observer vector in
      MRO HIRISE look direction frame
         X-component             (km) =           0.282372464
         Y-component             (km) =          -0.256290075
         Z-component             (km) =         249.784860121
      Sub-observer point radius  (km) =        3392.330239564
      Planetocentric latitude   (deg) =         -38.800230162
      Planetocentric longitude  (deg) =        -114.995297569
      Observer altitude          (km) =         249.785151209
      Intercept comparison error (km) =           0.000000001
   </pre>



   </li>
   </ol>

   */
   public SubObserverRecord( String                method,
                             Body                  target,
                             Time                  t,
                             ReferenceFrame        fixref,
                             AberrationCorrection  abcorr,
                             Body                  observer )

      throws SpiceException

   {
      //
      // Create the instance's point array.
      //
      super();

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
      CSPICE.subpnt ( method,
                      target.getName(),
                      t.getTDBSeconds(),
                      fixref.getName(),
                      abcorr.getName(),
                      observer.getName(),
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


   /**
   No-arguments constructor
   */
   public SubObserverRecord()
   {
      super();
   }


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public SubObserverRecord( SubObserverRecord subpt )
   
      throws SpiceException
   {
      v                  = ( new Vector3( subpt.getSubPoint() ) ).toArray();

      this.surfaceVector = subpt.getSurfaceVector();
      this.targetEpoch   = subpt.getTargetEpoch();
   }


   //
   // Methods
   //

   /**
   Return the sub-observer point.
   */
   public Vector3 getSubPoint()
   {
      return (  new Vector3(this)  );
   }

   /**
   Return the target epoch.
   */
   public TDBTime getTargetEpoch()

      throws SpiceException
   {
      return (  new TDBTime(targetEpoch)  );
   }

   /**
   Return the observer to sub-observer point vector.
   */
   public Vector3 getSurfaceVector()
   {
      return (  new Vector3(surfaceVector)  );
   }

}
