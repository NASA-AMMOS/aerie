

package spice.basic;

/**
Class GFIlluminationAngleSearch supports searches for
illumination angle events.

<p>
   Illumination angle searches determine a set of one or more time intervals
   within a confinement window when a specified illumination
   angle satisfies a caller-specified constraint. The resulting set
   of intervals is returned as a SPICE window.

<p>
   The term "illumination angles" refers to the following set of
   angles:
<pre>

      phase angle              Angle between the vectors from the
                               surface point to the observer and
                               from the surface point to the
                               illumination source.

      incidence angle          Angle between the surface normal at the
                               specified surface point and the vector
                               from the surface point to the
                               illumination source. When the
                               illumination source is the sun, this
                               angle is commonly called the "solar
                               incidence angle."

      emission angle           Angle between the surface normal at
                               the specified surface point and the
                               vector from the surface point to the
                               observer.
</pre>
   The diagram below illustrates the geometric relationships
   defining these angles. The labels for the incidence, emission,
   and phase angles are "inc.", "e.", and "phase".
<pre>

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
</pre>

   Note that if the target-observer vector, the target normal vector
   at the surface point, and the target-illumination source vector
   are coplanar, then phase is the sum of the incidence and emission
   angles. This rarely occurs; usually
<pre>
      phase angle  <  incidence angle + emission angle
</pre>
   All of the above angles can be computed using light time
   corrections, light time and stellar aberration corrections, or no
   aberration corrections. In order to describe apparent geometry as
   observed by a remote sensing instrument, both light time and
   stellar aberration corrections should be used.
<p>
   The way aberration corrections are applied by this routine
   is described below.
<pre>
      Light time corrections
      ======================

         Observer-target surface point vector
         ------------------------------------

         Let ET be the epoch at which an observation or remote
         sensing measurement is made, and let ET - LT ("LT" stands
         for "light time") be the epoch at which the photons
         received at ET were emitted from the surface point `spoint'.
         Note that the light time between the surface point and
         observer will generally differ from the light time between
         the target body's center and the observer.


         Target body's orientation
         -------------------------

         Using the definitions of ET and LT above, the target body's
         orientation at ET - LT is used. The surface normal is
         dependent on the target body's orientation, so the body's
         orientation model must be evaluated for the correct epoch.


         Target body -- illumination source vector
         -----------------------------------------

         The surface features on the target body near `spoint' will
         appear in a measurement made at ET as they were at ET-LT.
         In particular, lighting on the target body is dependent on
         the apparent location of the illumination source as seen
         from the target body at ET-LT. So, a second light time
         correction is used to compute the position of the
         illumination source relative to the surface point.


      Stellar aberration corrections
      ==============================

      Stellar aberration corrections are applied only if
      light time corrections are applied as well.

         Observer-target surface point body vector
         -----------------------------------------

         When stellar aberration correction is performed, the
         observer-to-surface point direction vector, which we'll
         call SRFVEC, is adjusted so as to point to the apparent
         position of `spoint': considering `spoint' to be an ephemeris
         object, SRFVEC points from the observer's position at ET to
         the light time and stellar aberration
         corrected position of `spoint'.

         Target body-illumination source vector
         --------------------------------------

         The target body-illumination source vector is the apparent
         position of the illumination source, corrected for light
         time and stellar aberration, as seen from the surface point
         `spoint' at time ET-LT.
</pre>



<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.


<p>1)  Determine time intervals over which the MER-1 ("Opportunity")
         rover's location satisfies certain constraints on its
         illumination and visibility as seen from the Mars
         Reconaissance Orbiter (MRO) spacecraft.

         In this case we require the emission angle to be less than
         20 degrees and the solar incidence angle to be less than
         60 degrees.

         The reader can verify that the observation start times of the
         MRO HIRISE images
<pre>
            Product ID              Image start time
            ----------              ----------------
            TRA_000873_1780_RED     2006-10-03T12:44:13.425
            PSP_001414_1780_RED     2006-11-14T15:39:55.373
            PSP_001612_1780_RED     2006-11-30T01:38:34.390
</pre>
         are contained within the result window found by the
         example program shown below.

         Use the meta-kernel shown below to load the required SPICE
         kernels.
<pre>

            KPL/MK

            File: mer1_ex.tm

            This meta-kernel is intended to support operation of SPICE
            example programs. The kernels shown here should not be
            assumed to contain adequate or correct versions of data
            required by SPICE-based user applications.

            In order for an application to use this meta-kernel, the
            kernels referenced here must be present in the user's
            current working directory.

            The names and contents of the kernels referenced
            by this meta-kernel are as follows:

               File name                     Contents
               ---------                     --------
               de421.bsp                     Planetary ephemeris
               pck00010.tpc                  Planet orientation
                                             and radii
               naif0010.tls                  Leapseconds
               mer1_surf_rover_ext10_v1.bsp  MER-1 ephemeris
               mer1_surf_rover_ext11_v1.bsp  MER-1 ephemeris
               mer1_ls_040128_iau2000_v1.bsp MER-1 landing site
                                             ephemeris
               mro_psp1.bsp                  MRO ephemeris
               mer1_v10.tf                   MER-1 frame kernel


            \begindata

               KERNELS_TO_LOAD = ( 'de421.bsp',
                                   'pck00010.tpc',
                                   'naif0010.tls',
                                   'mro_psp1.bsp',
                                   'mer1_surf_rover_ext10_v1.bsp',
                                   'mer1_surf_rover_ext11_v1.bsp',
                                   'mer1_ls_040128_iau2000_v1.bsp',
                                   'mro_psp1.bsp',
                                   'mer1_v10.tf'                    )
            \begintext

</pre>

 Example code begins here.

<pre>

   //
   // Determine time intervals over which the MER-1 ("Opportunity")
   // rover's location satisfies certain constraints on its
   // illumination and visibility as seen from the Mars
   // Reconaissance Orbiter (MRO) spacecraft.
   //
   // In this case we require the emission angle to be less than
   // 20 degrees and the solar incidence angle to be less than
   // 60 degrees.
   //
   import spice.basic.*;
   import static spice.basic.AngularUnits.*;
   import static spice.basic.GFConstraint.*;

   class GFIlluminationAngleSearchEx1
   {
      //
      // Load the JNISpice shared object library
      // at initialization time.
      //
      static { System.loadLibrary( "JNISpice" ); }

      //
      // Class constants
      //
      private final static String TIMFMT  =
         "YYYY MON DD HR:MN:SC.###### UTC";

      public static void main ( String[] args )
      {
         try {

            //
            // Constants
            //
            final int      NINTVLS = 1000;

            //
            // Declare and assign values to variables required to
            // specify the geometric condition to search for.
            //
            // Set observer, target, aberration correction, and the
            // Mars body-fixed, body-centered reference frame. The
            // lighting source is the sun.
            //
            // Aberration corrections are set for remote observations.
            //
            AberrationCorrection
               abcorr = new AberrationCorrection( "CN+S" );

            AberrationCorrection
               nocorr = new AberrationCorrection( "NONE" );

            Body target            = new Body ( "Mars"  );
            Body illmn             = new Body ( "Sun"   );
            Body observer          = new Body ( "MRO"   );
            Body rover             = new Body ( "MER-1" );
            IlluminationAngles angles;
            ReferenceFrame fixref  = new ReferenceFrame( "IAU_MARS" );
            PositionVector rovpos;
            TDBTime et;
            String[] labels        = { "Start", "Stop " };
            String method          = "Ellipsoid";
            String timstr;

            //
            // Load kernels via a meta-kernel.
            //
            final String META  = "mer1_ex.tm";

            KernelDatabase.load ( META );

            //
            // Set up a simple confinement window.
            //
            TDBTime et0 = new TDBTime ( "2006 OCT 02 00:00:00 UTC" );
            TDBTime et1 = new TDBTime ( "2006 NOV 30 12:00:00 UTC" );

            SpiceWindow confine = new SpiceWindow ();

            confine.insert( et0.getTDBSeconds(),
                            et1.getTDBSeconds() );

            //
            // Use the rover position at the start of
            // the search interval as the surface point.
            //
            rovpos = new PositionVector ( rover,  et0,   fixref, 
                                          nocorr, target        );

            //
            // Create a GF illumination angle search instance.
            // This instance specifies the geometric condition
            // to be found.
            //
            GFIlluminationAngleSearch incidenceSearch =

               new GFIlluminationAngleSearch ( method,   "INCIDENCE", target,
                                               illmn,    fixref,      abcorr,
                                               observer, rovpos               );
            //
            // Set up the GF search constraint.
            //
            double refval = 60.0 * RPD;

            GFConstraint incidenceCon =
               createReferenceConstraint ( "<", refval );

            //
            // Select a search step size. Units are TDB seconds.
            //
            double step = 21600.0;

            //
            // Search over the confinement window for times
            // when the solar incidence angle is less than
            // the reference value.
            //
            // Specify the maximum number of intervals in the result
            // window.
            //
            SpiceWindow wnsolr =

               incidenceSearch.run( confine, incidenceCon, step, NINTVLS );

            //
            // Next, we'll set up an emission angle search.
            //
            
            GFIlluminationAngleSearch emissionSearch =

               new GFIlluminationAngleSearch ( method,   "EMISSION", target,
                                               illmn,    fixref,     abcorr,
                                               observer, rovpos              );
            //
            // Set the reference value for the emission angle search.
            //
            refval = 20.0 * RPD;

            GFConstraint emissionCon =
               createReferenceConstraint ( "<", refval );

            //
            // We'll use 15 minutes as the search step. This step
            // is small enough to be suitable for Mars orbiters.
            // Units are seconds.
            //
            step   = 900.0;

            //
            // Run the search over the result window of the incidence
            // angle search.
            //
            SpiceWindow result =

               emissionSearch.run( wnsolr, emissionCon, step, NINTVLS );
 
            //
            // Display the result window. Show the solar incidence
            // and emission angles at the window's interval
            // boundaries.
            //
            int wncard   = result.card();

            System.out.format( "%n" );

            if ( wncard == 0 )
            {
               System.out.println ( "Result window is empty: " + 
                                    "condition is not met."      );
            }
            else
            {
               System.out.format ( "                                    " +
                                   "        Solar Incidence   Emission%n" +
                                   "                                    " +
                                   "              (deg)         (deg) %n" +
                                   "%n"                                   );

               for ( int i = 0;  i < wncard;  i++ )
               {
                  double[] interval = result.getInterval( i );

                  for ( int j = 0;  j < 2;  j++ )
                  {
                     et  = new TDBTime( interval[j] );

                     //
                     // Compute the angles of interest at the boundary
                     // epochs.
                     //
                     timstr = et.toString( TIMFMT );

                     angles = 

                        new IlluminationAngles ( "Ellipsoid", target, et,    
                                                 fixref,      abcorr, observer, 
                                                 rovpos                       );

                     System.out.format( "    %s: %s %14.9f %14.9f%n",
                                        labels[j],
                                        timstr, 
                                        angles.getSolarIncidenceAngle() * DPR,
                                        angles.getEmissionAngle()       * DPR );
                  }
                  System.out.format ( "%n" );
               }                
            }
         }
         catch ( SpiceException exc ) {
            exc.printStackTrace();
         }
      }
   } 
</pre>

<p> When run on a PC/Linux/java 1.6.0_14/gcc platform, the output
   from this program was:

<pre>
                                            Solar Incidence   Emission
                                                  (deg)         (deg)

    Start: 2006 OCT 03 12:43:46.949483 UTC   56.104150191   20.000000187
    Stop : 2006 OCT 03 12:44:42.288747 UTC   56.299961806   20.000000155

    Start: 2006 OCT 08 16:03:33.956839 UTC   56.489554846   20.000000207
    Stop : 2006 OCT 08 16:04:29.495919 UTC   56.687545101   19.999999969

    Start: 2006 OCT 13 19:23:24.634854 UTC   56.887410591   19.999999879
    Stop : 2006 OCT 13 19:24:12.492952 UTC   57.059318573   20.000000174

    Start: 2006 OCT 18 22:43:21.631086 UTC   57.309244667   20.000000118
    Stop : 2006 OCT 18 22:43:47.966990 UTC   57.404572725   20.000000043

    Start: 2006 NOV 14 15:39:44.153177 UTC   54.328758385   19.999999935
    Stop : 2006 NOV 14 15:40:10.446479 UTC   54.426680766   19.999999896

    Start: 2006 NOV 19 18:59:10.190551 UTC   54.630961112   20.000000067
    Stop : 2006 NOV 19 18:59:54.776369 UTC   54.798407529   19.999999848

    Start: 2006 NOV 24 22:18:38.342454 UTC   54.949599996   19.999999822
    Stop : 2006 NOV 24 22:19:30.964843 UTC   55.148838833   20.000000029

    Start: 2006 NOV 30 01:38:07.309245 UTC   55.280547838   19.999999832
    Stop : 2006 NOV 30 01:39:03.296253 UTC   55.494189248   19.999999989
</pre>


<p> Version 1.0.0 28-FEB-2014 (NJB)
*/
public class GFIlluminationAngleSearch extends GFNumericSearch
{
   //
   // Public constants
   //

   /**
   Search quantity is emission angle
   */
   public final static String        EMISSION  = "EMISSION";

   /**
   Search quantity is incidence angle
   */
   public final static String        INCIDENCE = "INCIDENCE";
 
   /**
   Search quantity is phase angle
   */
   public final static String        PHASE     = "PHASE";

   /**
   Computation method uses an ellipsoidal target body
   shape model
   */
   public final static String        ELLIPSOID = "ELLIPSOID";

   //
   // Fields
   //
   private String                    method;
   private String                    angtyp;
   private Body                      target;
   private Body                      illmn;
   private ReferenceFrame            fixref;
   private AberrationCorrection      abcorr;
   private Body                      observer;
   private Vector3                   spoint;

   //
   // Constructors
   //

   /**
   Specify the geometric inputs for an illumination angle search.

   <p> `method' currently is limited to 
   <pre>
      "Ellipsoid"
   </pre>
   This argument is case-insensitive.

   <p> `angtyp' can have the values
   <pre>
   EMISSION
   INCIDENCE
   PHASE
   </pre>

   <p> `target' identifies the target body.

   <p> `illmn' identifies the illumination source  
   <p> `fixref' is the body-fixed, body-centered 
   reference frame of the target.
   <p> `abcorr' is the aberration correction.
   <p> `observer' identifies the observing body.
   <p> `spoint' is a vector specifying a surface point
       on the target body; illumination angles will be computed
       at this point. `spoint' is expressed in the reference
       frame designated by `fixref.'

   */
   public GFIlluminationAngleSearch ( String                  method,
                                      String                  angtyp,
                                      Body                    target,
                                      Body                    illmn,
                                      ReferenceFrame          fixref,
                                      AberrationCorrection    abcorr,
                                      Body                    observer,
                                      Vector3                 spoint    )
   {
      //
      // Just save the inputs. 
      //
      this.method            = method;
      this.angtyp            = angtyp;
      this.target            = target;
      this.illmn             = illmn;
      this.fixref            = fixref;
      this.abcorr            = abcorr;
      this.observer          = observer;
      this.spoint            = spoint;
   }


   /**
   Run a search over a specified confinement window, using
   a specified step size (units are TDB seconds).
   */
   public SpiceWindow run ( SpiceWindow   confinementWindow,
                            GFConstraint  constraint,
                            double        step,
                            int           maxWorkspaceIntervals )

      throws SpiceException
   {

      double[] resultArray = CSPICE.gfilum ( method,
                                             angtyp,
                                             target.getName(),
                                             illmn.getName(),
                                             fixref.getName(),
                                             abcorr.getName(),
                                             observer.getName(),
                                             spoint.toArray(),
                                             constraint.getCSPICERelation(),
                                             constraint.getReferenceValue(),
                                             constraint.getAdjustmentValue(),
                                             step,
                                             maxWorkspaceIntervals,
                                             confinementWindow.toArray()    );

      return (  new SpiceWindow( resultArray )  );
   }
}
