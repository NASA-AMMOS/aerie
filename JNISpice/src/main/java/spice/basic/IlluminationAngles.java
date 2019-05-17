
package spice.basic;

/**
Class IlluminationAngles supports illumination angle computations.

<h3>Particulars</h3>
<pre>
   Illumination angles 
   =================== 
 
   The term "illumination angles" refers to following set of 
   angles: 
 
 
      phase angle              Angle between the vectors from the 
                               surface point to the observer and 
                               from the surface point to the  
                               illumination source. 
 
      incidence angle          Angle between the surface normal at 
                               the specified surface point and the 
                               vector from the surface point to the 
                               illumination source. 
 
      emission angle           Angle between the surface normal at 
                               the specified surface point and the 
                               vector from the surface point to the 
                               observer. 
  
   The diagram below illustrates the geometric relationships 
   defining these angles. The labels for the incidence, emission, 
   and phase angles are "inc.", "e.", and "phase". 
 
 
                                                    * 
                                            illumination source 
 
                  surface normal vector 
                            ._                 _. 
                            |\                 /|  illumination  
                              \    phase      /    source vector 
                               \   .    .    / 
                               .            . 
                                 \   ___   / 
                            .     \/     \/ 
                                  _\ inc./ 
                           .    /   \   / 
                           .   |  e. \ / 
       *             <--------------- *  surface point on 
    viewing            vector            target body 
    location           to viewing 
    (observer)         location 
 
 
   Note that if the target-observer vector, the target normal vector 
   at the surface point, and the target-illumination source vector 
   are coplanar, then phase is the sum of the incidence and emission 
   angles. This rarely occurs; usually 
 
      phase angle  <  incidence angle + emission angle 
 
   All of the above angles can be computed using light time 
   corrections, light time and stellar aberration corrections, or no 
   aberration corrections. In order to describe apparent geometry as 
   observed by a remote sensing instrument, both light time and 
   stellar aberration corrections should be used. 
    
   The way aberration corrections are applied by the constructors
   of this class is described below. 
 
      Light time corrections 
      ====================== 
 
         Observer-target surface point vector 
         ------------------------------------ 
 
         Let `et' be the epoch at which an observation or remote 
         sensing measurement is made, and let et-lt ("lt" stands 
         for "light time") be the epoch at which the photons 
         received at `et' were emitted from the surface point `spoint'. 
         Note that the light time between the surface point and 
         observer will generally differ from the light time between 
         the target body's center and the observer. 
 
 
         Target body's orientation 
         ------------------------- 
 
         Using the definitions of `et' and `lt' above, the target body's 
         orientation at et-lt is used. The surface normal is 
         dependent on the target body's orientation, so the body's 
         orientation model must be evaluated for the correct epoch. 
 
 
         Target body -- illumination source vector 
         ----------------------------------------- 
 
         The surface features on the target body near `spoint' will 
         appear in a measurement made at `et' as they were at et-lt. 
         In particular, lighting on the target body is dependent on 
         the apparent location of the illumination source as seen 
         from the target body at et-lt. So, a second light time 
         correction is used to compute the position of the 
         illumination source relative to the surface point. 
 
 
      Stellar aberration corrections 
      ============================== 
 
      Stellar aberration corrections are applied only if 
      light time corrections are applied as well. 
 
         Observer-target surface point body vector 
         ----------------------------------------- 
 
         When stellar aberration correction is performed, the 
         direction vector `srfvec' is adjusted so as to point to the 
         apparent position of `spoint': considering `spoint' to be an 
         ephemeris object, `srfvec' points from the observer's 
         position at `et' to the light time and stellar aberration 
         corrected position of `spoint'. 
 
         Target body-illumination source vector 
         -------------------------------------- 
 
         The target body-illumination source vector is the apparent 
         position of the illumination source, corrected for light 
         time and stellar aberration, as seen from the target body 
         at time et-lt. 
 
 
   Using DSK data 
   ============== 
 
      DSK loading and unloading 
      ------------------------- 
 
      DSK files providing data used by this class are loaded by
      calling {@link KernelDatabase#load} and can be unloaded by calling 
      {@link KernelDatabase#unload} or {@link KernelDatabase#clear} 
      See the documentation of {@link KernelDatabase#load} for limits 
      on numbers of loaded DSK files.
 
      For run-time efficiency, it's desirable to avoid frequent 
      loading and unloading of DSK files. When there is a reason to 
      use multiple versions of data for a given target body---for 
      example, if topographic data at varying resolutions are to be 
      used---the surface list can be used to select DSK data to be 
      used for a given computation. It is not necessary to unload 
      the data that are not to be used. This recommendation presumes 
      that DSKs containing different versions of surface data for a 
      given body have different surface ID codes. 
 
 
      DSK data priority 
      ----------------- 
 
      A DSK coverage overlap occurs when two segments in loaded DSK 
      files cover part or all of the same domain---for example, a 
      given longitude-latitude rectangle---and when the time 
      intervals of the segments overlap as well. 
 
      When DSK data selection is prioritized, in case of a coverage 
      overlap, if the two competing segments are in different DSK 
      files, the segment in the DSK file loaded last takes 
      precedence. If the two segments are in the same file, the 
      segment located closer to the end of the file takes 
      precedence. 
 
      When DSK data selection is unprioritized, data from competing 
      segments are combined. For example, if two competing segments 
      both represent a surface as a set of triangular plates, the 
      union of those sets of plates is considered to represent the 
      surface.  
 
      Currently only unprioritized data selection is supported. 
      Because prioritized data selection may be the default behavior 
      in a later version of the class, the UNPRIORITIZED keyword is 
      required in the `method' argument. 
 
       
      Syntax of the `method' input argument 
      ----------------------------------- 
 
      The keywords and surface list in the `method' argument 
      are called "clauses." The clauses may appear in any 
      order, for example 
 
         "DSK/<surface list>/UNPRIORITIZED"
         "DSK/UNPRIORITIZED/<surface list>"
         "UNPRIORITIZED/<surface list>/DSK"
 
      The simplest form of the `method' argument specifying use of 
      DSK data is one that lacks a surface list, for example: 
 
         "DSK/UNPRIORITIZED" 
 
      For applications in which all loaded DSK data for the target 
      body are for a single surface, and there are no competing 
      segments, the above string suffices. This is expected to be 
      the usual case. 
 
      When, for the specified target body, there are loaded DSK 
      files providing data for multiple surfaces for that body, the 
      surfaces to be used by the constructors of this class
      for a given call must be specified in a surface list, unless 
      data from all of the surfaces are to be used together. 
 
      The surface list consists of the string 
 
         "SURFACES ="
 
      followed by a comma-separated list of one or more surface 
      identifiers. The identifiers may be names or integer codes in 
      string format. For example, suppose we have the surface 
      names and corresponding ID codes shown below: 
 
         Surface Name                              ID code 
         ------------                              ------- 
         "Mars MEGDR 128 PIXEL/DEG"                1 
         "Mars MEGDR 64 PIXEL/DEG"                 2 
         "Mars_MRO_HIRISE"                         3 
 
      If data for all of the above surfaces are loaded, then 
      data for surface 1 can be specified by either 
 
         "SURFACES = 1" 
 
      or 
 
         "SURFACES = "\"Mars MEGDR 128 PIXEL/DEG\"" 
 
      Escaped double quotes are used to delimit the surface name
      because it contains blank characters.
          
      To use data for surfaces 2 and 3 together, any 
      of the following surface lists could be used: 
 
         "SURFACES = 2, 3" 
 
         "SURFACES = \"Mars MEGDR  64 PIXEL/DEG\", 3" 
 
         "SURFACES = 2, Mars_MRO_HIRISE" 
 
         "SURFACES = \"Mars MEGDR 64 PIXEL/DEG\", Mars_MRO_HIRISE" 
        
      An example of a `method' argument that could be constructed 
      using one of the surface lists above is 

         "DSK/UNPRIORITIZED/SURFACES = \"Mars MEGDR 64 PIXEL/DEG\", 3"
  
 
      Aberration corrections using DSK data 
      ------------------------------------- 
 
      For irregularly shaped target bodies, the distance between the 
      observer and the nearest surface intercept need not be a 
      continuous function of time; hence the one-way light time 
      between the intercept and the observer may be discontinuous as 
      well. In such cases, the computed light time, which is found 
      using iterative algorithm, may converge slowly or not at all. 
      In all cases, the light time computation will terminate, but 
      the result may be less accurate than expected. 
</pre>


<h3>Code Examples</h3>

<p>  The numerical results shown for this example may differ across 
   platforms. The results depend on the SPICE kernels used as 
   input, the compiler and supporting libraries, and the machine  
   specific arithmetic implementation.  
 
<pre>
   1) Find the phase, solar incidence, and emission angles at the 
      sub-solar and sub-spacecraft points on Mars as seen from the Mars
      Global Surveyor spacecraft at a specified UTC time.
 
      Use both an ellipsoidal Mars shape model and topographic data
      provided by a DSK file. For both surface points, use the "near
      point" and "nadir" definitions for ellipsoidal and DSK shape
      models, respectively.
 
      Use converged Newtonian light time and stellar aberration
      corrections.

      The topographic model is based on data from the MGS MOLA DEM 
      megr90n000cb, which has a resolution of 4 pixels/degree. A 
      triangular plate model was produced by computing a 720 x 1440 
      grid of interpolated heights from this DEM, then tessellating 
      the height grid. The plate model is stored in a type 2 segment 
      in the referenced DSK file. 

      Use the meta-kernel shown below to load the required SPICE 
      kernels. 
 
         KPL/MK 
 
         File: IlluminationAnglesEx1.tm 
 
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
            mgs_ext12_ipng_mgs95j.bsp        MGS ephemeris 
            megr90n000cb_plate.bds           Plate model based on 
                                             MEGDR DEM, resolution 
                                             4 pixels/degree. 
 
         \begindata 
 
            KERNELS_TO_LOAD = ( 'de430.bsp', 
                                'mar097.bsp', 
                                'pck00010.tpc', 
                                'naif0011.tls', 
                                'mgs_ext12_ipng_mgs95j.bsp', 
                                'megr90n000cb_plate.bds'      ) 
         \begintext 
 

   Example code begins here. 


   //
   // IlluminationAngles example 1
   //

   //
   // Import JNISpice API classes.
   //
   import spice.basic.*;

   //
   // Import the conversion factor DPR.
   //
   import static spice.basic.AngularUnits.*;


   public class IlluminationAnglesEx1
   {

      static{ System.loadLibrary( "JNISpice" ); }

      public static void main( String[] args )
      {
         //
         // Local constants
         //
         final String     META  = "IlluminationAngles.tm";

         final int        NMETH = 2;

         //
         // Local variables
         //

         //
         // `method' strings for illumination angle computations.
         //
         String[]         ilumth = { "Ellipsoid", 
                                     "DSK/Unprioritized" };

         //
         // `method' strings for sub-spacecraft point and 
         // sub-solar point computations.
         //
         String[]         submth = { "Near Point/Ellipsoid", 
                                     "DSK/Nadir/Unprioritized" };
         int              i;


         try
         {
            //
            // Load kernels.
            //
            KernelDatabase.load( META );

            //
            // Convert the UTC request time string to seconds past 
            // J2000 TDB. 
            //
            String utc  = "2003 OCT 13 06:00:00 UTC";

            TDBTime et  = new TDBTime( utc );

            System.out.format ( "%n" +
                                "UTC epoch is %s%n", utc );

            //            
            // Assign observer, target, and illumination source
            // names. The acronym MGS indicates Mars Global 
            // Surveyor. See NAIF_IDS for a list of names 
            // recognized by SPICE. 
            //
            // Also set the target body-fixed frame and
            // the aberration correction flag.  
            //

            Body target = new Body( "Mars" );
            Body obsrvr = new Body( "MGS"  );
            Body ilusrc = new Body( "Sun"  );

            ReferenceFrame       fixref = new ReferenceFrame( "IAU_MARS");
            AberrationCorrection abcorr = new AberrationCorrection( "CN+S" );

            for ( i = 0;  i < NMETH;  i++ )
            {
               //
               // Find the sub-solar point on Mars as 
               // seen from the MGS spacecraft at `et'. Use the 
               // "near point" style of sub-point definition 
               // when the shape model is an ellipsoid, and use 
               // the "nadir" style when the shape model is 
               // provided by DSK data. This makes it easy to  
               // verify the solar incidence angle when 
               // the target is modeled as an  ellipsoid. 
               //
               SubSolarRecord ssolrec 

                 = new SubSolarRecord( submth[i], target, et,
                                       fixref,    abcorr, obsrvr );

               Vector3 ssolpt = ssolrec.getSubPoint(); 

               //
               // Now find the sub-spacecraft point.  
               //
               SubObserverRecord sscrec

                  = new SubObserverRecord( submth[i], target, et,
                                           fixref,    abcorr, obsrvr );

               Vector3 sscpt = sscrec.getSubPoint();

               //
               // Find the phase, solar incidence, and emission 
               // angles at the sub-solar point on Mars as 
               // seen from MGS at time `et'.  
               //
               IlluminationAngles solAngles

                  = new IlluminationAngles( ilumth[i], target, ilusrc,
                                            et,        fixref, abcorr,
                                            obsrvr,    ssolpt         );
               //
               // Do the same for the sub-spacecraft point. 
               //
               IlluminationAngles sscAngles

                  = new IlluminationAngles( ilumth[i], target, ilusrc,
                                            et,        fixref, abcorr,
                                            obsrvr,    sscpt          );
               //
               // Obtain the illumination angles and flags.
               // Convert the angles to degrees and write 
               // the outputs.   
               //
               double  sslphs = solAngles.getPhaseAngle()     * DPR;
               double  sslemi = solAngles.getEmissionAngle()  * DPR;
               double  sslinc = solAngles.getIncidenceAngle() * DPR;
               boolean sslvis = solAngles.isVisible();
               boolean ssllit = solAngles.isLit();

               double  sscphs = sscAngles.getPhaseAngle()     * DPR;
               double  sscemi = sscAngles.getEmissionAngle()  * DPR;
               double  sscinc = sscAngles.getIncidenceAngle() * DPR;
               boolean sscvis = sscAngles.isVisible();
               boolean ssclit = sscAngles.isLit();

               System.out.format(

                  "%n" +
                  "   IlluminationAngle method: %s%n" +
                  "   SubObserverRecord method: %s%n" +
                  "   SubSolarRecord method:    %s%n" +
                  "%n" +
                  "      Illumination angles at the " +
                  "sub-solar point:%n" +
                  "%n" +
                  "      Phase angle            (deg): %15.9f%n" +
                  "      Solar incidence angle  (deg): %15.9f%n" +
                  "      Emission angle         (deg): %15.9f%n" +
                  "      Visible:                        %b%n"   +
                  "      Lit:                            %b%n",
                  ilumth[i],
                  submth[i],
                  submth[i],
                  sslphs,
                  sslinc,
                  sslemi,
                  sslvis,
                  ssllit  );           

               if ( i == 0 )
               {
                  System.out.format( "%n" +
                                     "         The solar incidence angle "  +
                                     "should be 0.%n"                       +
                                     "         The emission "               +
                                     "and phase angles should be equal.%n"  );
               }

               System.out.format(

                  "%n" +
                  "      Illumination angles at the " +
                  "sub-s/c point:%n" +
                  "%n" +
                  "      Phase angle            (deg): %15.9f%n" +
                  "      Solar incidence angle  (deg): %15.9f%n" +
                  "      Emission angle         (deg): %15.9f%n" +
                  "      Visible:                        %b%n"   +
                  "      Lit:                            %b%n",
                  sscphs,
                  sscinc,
                  sscemi,
                  sscvis,
                  ssclit  );           

               if ( i == 0 )
               {
                  System.out.format( "%n" +
                                     "         The emission angle "  +
                                     "should be 0.%n"                +
                                     "         The solar incidence " +
                                     "and phase angles should be equal.%n"  );
               }

            }

            System.out.println( " " );
         }
         catch ( SpiceException exc )
         {
            exc.printStackTrace();
         }
      }

   }


   When this program was executed on a PC/Linux/gcc/64-bit/Java 1.5 
   platform, the output was:   


      UTC epoch is 2003 OCT 13 06:00:00 UTC

         IlluminationAngle method: Ellipsoid
         SubObserverRecord method: Near Point/Ellipsoid
         SubSolarRecord method:    Near Point/Ellipsoid

            Illumination angles at the sub-solar point:

            Phase angle            (deg):   138.370270685
            Solar incidence angle  (deg):     0.000000000
            Emission angle         (deg):   138.370270685
            Visible:                        false
            Lit:                            true

               The solar incidence angle should be 0.
               The emission and phase angles should be equal.

            Illumination angles at the sub-s/c point:

            Phase angle            (deg):   101.439331040
            Solar incidence angle  (deg):   101.439331041
            Emission angle         (deg):     0.000000002
            Visible:                        true
            Lit:                            false

               The emission angle should be 0.
               The solar incidence and phase angles should be equal.

         IlluminationAngle method: DSK/Unprioritized
         SubObserverRecord method: DSK/Nadir/Unprioritized
         SubSolarRecord method:    DSK/Nadir/Unprioritized

            Illumination angles at the sub-solar point:

            Phase angle            (deg):   138.387071677
            Solar incidence angle  (deg):     0.967122745
            Emission angle         (deg):   137.621480599
            Visible:                        false
            Lit:                            true

            Illumination angles at the sub-s/c point:

            Phase angle            (deg):   101.439331359
            Solar incidence angle  (deg):   101.555993667
            Emission angle         (deg):     0.117861156
            Visible:                        true
            Lit:                            false
</pre>


<h3> Version 2.0.0 11-NOV-2016 (NJB)</h3>

<p> Upgraded to support DSK usage.

<p> Now supports "isLit" and "isVisible" methods. These indicate
whether the illumination source and observer, respectively, are visible
from the surface point.   

<h3> Version 1.0.0 16-JUL-2009 (NJB)</h3>
*/
public class IlluminationAngles extends Object
{
   //
   // Public Constants
   //

   //
   // The values below are the geometric "methods" supported by
   // the IlluminationAngles constructor.
   //
   public final static String ELLIPSOID = "ELLIPSOID";

   //
   // See the class documentation for a detailed description of
   // the `method' argument.
   //

   //
   // Fields
   //
   private boolean          lit;
   private boolean          visible;
   private TDBTime          targetEpoch;
   private Vector3          surfaceVector;
   private double           phaseAngle;
   private double           incidenceAngle;
   private double           emissionAngle;


   //
   // Constructors
   //


   /**
   Find the illumination angles (phase, solar incidence, and emission)
   at a specified surface point on a target body, using a specified
   illumination source; create a record containing the result. 
   The illumination source may be any ephemeris object.

   <p> Values and meanings of the `method' argument are described below.

   <pre>
   method      is a short string providing parameters defining 
               the computation method to be used. In the syntax 
               descriptions below, items delimited by brackets 
               are optional. 
               
               `method' may be assigned the following values:    
 
                  "ELLIPSOID" 
  
                     The illumination angle computation uses a 
                     triaxial ellipsoid to model the surface of the 
                     target body. The ellipsoid's radii must be 
                     available in the kernel pool. 
 
 
                  "DSK/UNPRIORITIZED[/SURFACES = <surface list>]" 
 
                     The illumination angle computation uses 
                     topographic data to model the surface of the 
                     target body. These data must be provided by 
                     loaded DSK files. 
 
                     The surface list specification is optional. The 
                     syntax of the list is 
 
                        <surface 1> [, <surface 2>...] 
 
                     If present, it indicates that data only for the 
                     listed surfaces are to be used; however, data 
                     need not be available for all surfaces in the 
                     list. If absent, loaded DSK data for any surface 
                     associated with the target body are used. 
 
                     The surface list may contain surface names or 
                     surface ID codes. Names containing blanks must 
                     be delimited by escaped double quotes, for example 
 
                        "SURFACES = \"Mars MEGDR 128 PIXEL/DEG\""
                                        
                     If multiple surfaces are specified, their names 
                     or IDs must be separated by commas. 
 
                     See the Particulars section below for details 
                     concerning use of DSK data. 
 
 
               Neither case nor white space are significant in 
               `method', except within double-quoted strings 
               representing surface names. For example, the string 
               " eLLipsoid " is valid.
 
               Within double-quoted strings representing surface names,
               blank characters are significant, but multiple
               consecutive blanks are considered equivalent to a single
               blank. Case is not significant. So
 
                  \"Mars MEGDR 128 PIXEL/DEG\" 
 
               is equivalent to  
 
                  \" mars megdr  128  pixel/deg \"
 
               but not to 
 
                  \"MARS MEGDR128PIXEL/DEG\" 
   </pre>

   */
   public IlluminationAngles( String                method,
                              Body                  target,
                              Body                  ilusrc,
                              Time                  t,
                              ReferenceFrame        fixref,
                              AberrationCorrection  abcorr,
                              Body                  observer,
                              Vector3               spoint   )
      throws SpiceException

   {

      Init( method, target, ilusrc,   t,  
            fixref, abcorr, observer, spoint );
 
   }


   /**
   Find the illumination angles (phase, solar incidence, and emission)
   at a specified surface point on a target body, using the sun as
   the illumination source; create a record containing the result.  

   <p>See the discussion of the argument `method' in the detailed
   documentation of {@link #IlluminationAngles(String,Body,
   Body,Time,ReferenceFrame,AberrationCorrection,Body,Vector3)}.
   */
   public IlluminationAngles( String                method,
                              Body                  target,
                              Time                  t,
                              ReferenceFrame        fixref,
                              AberrationCorrection  abcorr,
                              Body                  observer,
                              Vector3               spoint   )
      throws SpiceException

   {
  
      //
      // Hard-code the sun as the illumination source. Use the
      // value 10 so run-time updates of the body-ID mapping
      // won't affect this call.
      //
      final Body ilusrc = new Body( 10 );

      //
      // Call the general IlluminationAngle constructor.
      //
      Init( method, target, ilusrc,   t, 
            fixref, abcorr, observer, spoint );
 
   }


   //
   // Methods
   //


   /**
   Private helper method that initializes fields for constructors.
   */
   private void Init ( String                method,
                       Body                  target,
                       Body                  ilusrc,
                       Time                  t,
                       ReferenceFrame        fixref,
                       AberrationCorrection  abcorr,
                       Body                  observer,
                       Vector3               spoint   )

      throws SpiceException

   {
      //
      // Declare arrays to hold outputs from the native method;
      // even scalars are returned in arrays.
      //
      boolean[]      litFlag = new boolean[1];
      boolean[]      visFlag = new boolean[1];
      double[]       trgepc  = new double[1];
      double[]       srfvec  = new double[3];
      double[]       angles  = new double[3];
    
      //
      // The native method call:
      //
      CSPICE.illumf ( method,
                      target.getName(),
                      ilusrc.getName(),
                      t.getTDBSeconds(),
                      fixref.getName(),
                      abcorr.getName(),
                      observer.getName(),
                      spoint.toArray(),
                      trgepc,
                      srfvec,
                      angles,
                      visFlag,
                      litFlag            );

      //
      // The outputs become the values of this record's fields.
      //
      targetEpoch         = new TDBTime( trgepc[0] );
      surfaceVector       = new Vector3( srfvec    );
      phaseAngle          = angles[0];
      incidenceAngle      = angles[1];
      emissionAngle       = angles[2];
      visible             = visFlag[0];
      lit                 = litFlag[0];
   } 


   /**
   Return the phase angle. Units are radians.

   <p> Let `phase' refer to the phase angle at `spoint', 
   as seen from `obsrvr' at
   time `et'. This is the angle between the spoint-obsrvr
   vector and the spoint-illumination source vector. Units
   are radians. The range of `phase' is [0, pi]. See
   Particulars above for a detailed discussion of the
   definition.
   */
   public double getPhaseAngle()
   {
      return (  phaseAngle );
   }

   /**
   Return the illumination source incidence angle. Units are radians.

   <p>
   Let `incdnc' refer to the illumination source incidence angle at `spoint',
   as seen from `obsrvr' at time `et'. This is the angle
   between the surface normal vector at `spoint' and the
   spoint-illumination source vector. Units are radians.
   The range of `incdnc' is [0, pi]. See Particulars above
   for a detailed discussion of the definition.
 
   */
   public double getIncidenceAngle()
   {
      return ( incidenceAngle );
   }

   /**
   Return the solar incidence angle. Units are radians.

   <p>This method is deprecated and is retained for 
   backward compatibility. Use 
   {@link #getIncidenceAngle}
   in place of this method.
 
   */
   public double getSolarIncidenceAngle()
   {
      return ( incidenceAngle );
   }



   /**
   Return the emission angle. Units are radians.

   <p> Let `emissn' refer to emission angle at `spoint', as seen from `obsrvr'
   at time `et'. This is the angle between the surface
   normal vector at `spoint' and the spoint-observer
   vector. Units are radians. The range of `emissn' is 
   [0, pi]. See Particulars above for a detailed discussion 
   of the definition.
   */
   public double getEmissionAngle()
   {
      return ( emissionAngle );
   }

   /**
   Return the target epoch.

   <p> 
   Let `trgepc' refer to "target surface point epoch." `trgepc' is defined
   as follows: letting `lt' be the one-way light time
               between the observer and the input surface point
               `spoint', `trgepc' is either the epoch et-lt or `et'
               depending on whether the requested aberration correction
               is, respectively, for received radiation or omitted.
               `lt' is computed using the method indicated by `abcorr'.
    


   */
   public TDBTime getTargetEpoch()
   {
      return (  new TDBTime(targetEpoch)  );
   }

   /**
   Return the observer to surface point vector.

   <p> Let `srfvec' refer to this vector; it is
   the vector from the observer's position at `et' to
   the aberration-corrected (or optionally, geometric)
   position of `spoint', where the aberration corrections
   are specified by `abcorr'. `srfvec' is expressed in the
   target body-fixed reference frame designated by
   `fixref', evaluated at `trgepc'.
 
   <p> The components of `srfvec' are given in units of km. 
 
   <p>One can use {@link Vector3#norm} to obtain the 
   distance between the observer and `spoint': 
   <pre>
   dist = srfvec.norm();
   </pre>
   <p>
   The observer's position `obspos', relative to the 
   target body's center, where the center's position is 
   corrected for aberration effects as indicated by 
   `abcorr', can be computed via the call: 
   <pre>
   obspos = spoint.sub(srfvec); 
   </pre>
   To transform the vector `srfvec' from a reference frame
   `fixref' at time `trgepc' to a time-dependent reference
   frame `ref' at time `et', the method 
   {@link ReferenceFrame#getPositionTransformation(ReferenceFrame,Time,Time)} 
   should be called. Let `xform' be the 3x3 matrix representing the
   rotation from the reference frame `fixref' at time
   `trgepc' to the reference frame `ref' at time `et'. Then
   `srfvec' can be transformed to the result `refvec' as
   follows:
   <pre>
   xform  = fixref.getPositionTransformation( ref, trgepc, et );
   refvec = xform.mxv( srfvec );                                            
   </pre>
   <p>
   */
   public Vector3 getSurfaceVector()
   {
      return (  new Vector3(surfaceVector)  );
   }

   /**
   Return the visibility flag.

   <p> Let `visible' refer to this flag, which indicates whether the surface
       point is visible to the observer. `visible' takes into
       account whether the target surface occults `spoint',
       regardless of the emission angle at `spoint'. `visible' is
       returned with the value true if `spoint' is visible;
       otherwise it is false.
   */
   public boolean isVisible()
   {
      return ( visible );
   }

   /**
   Return the illumination flag.

   <p> Let `lit' refer to this logical flag,
   which indicates whether the surface
   point is illuminated; the point is considered to be
   illuminated if the vector from the point to the
   center of the illumination source doesn't intersect
   the target surface. `lit' takes into account whether
   the target surface casts a shadow on `spoint',
   regardless of the incidence angle at `spoint'. `lit' is
   returned with the value true if `spoint' is
   illuminated; otherwise it is false.
  
   */
   public boolean isLit()
   {
      return ( lit );
   }
}
