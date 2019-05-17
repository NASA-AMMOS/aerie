

package spice.basic;

/**
Class GFRayInFOVSearch conducts searches for time intervals
over which a specified target body appears in a specified instrument
field of view (FOV).

<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<p> This example is an extension of example #1 in the
header of {@link spice.basic.GFTargetInFOVSearch}.
The problem statement for that example is

<pre>
   Search for times when Saturn's satellite Phoebe is within the
   FOV of the Cassini narrow angle camera (CASSINI_ISS_NAC). To
   simplify the problem, restrict the search to a short time
   period where continuous Cassini bus attitude data are
   available.

   Use a step size of 10 seconds to reduce chances of missing
   short visibility events.
</pre>

<p> Here we search the same confinement window for times when a
selected background star is visible. We use the FOV of the
Cassini ISS wide angle camera (CASSINI_ISS_WAC) to enhance the
probability of viewing the star.

<p> The star we'll use has catalog number 6000 in the Hipparcos
Catalog. The star's J2000 right ascension and declination, proper
motion, and parallax are taken from that catalog.

<p> Use the meta-kernel from the
{@link spice.basic.GFTargetInFOVSearch} example:


<pre>
   KPL/MK

   File name: gftfov_ex1.tm

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
      naif0009.tls                  Leapseconds
      cpck05Mar2004.tpc             Satellite orientation and
                                    radii
      981005_PLTEPH-DE405S.bsp      Planetary ephemeris
      020514_SE_SAT105.bsp          Satellite ephemeris
      030201AP_SK_SM546_T45.bsp     Spacecraft ephemeris
      cas_v37.tf                    Cassini FK
      04135_04171pc_psiv2.bc        Cassini bus CK
      cas00084.tsc                  Cassini SCLK kernel
      cas_iss_v09.ti                Cassini IK


   \begindata

      KERNELS_TO_LOAD = ( 'naif0009.tls',
                          'cpck05Mar2004.tpc',
                          '981005_PLTEPH-DE405S.bsp',
                          '020514_SE_SAT105.bsp',
                          '030201AP_SK_SM546_T45.bsp',
                          'cas_v37.tf',
                          '04135_04171pc_psiv2.bc',
                          'cas00084.tsc',
                          'cas_iss_v09.ti'            )
   \begintext
</pre>



<p>      Example code begins here.

<pre>
   import spice.basic.*;
   import static spice.basic.GFTargetInFOVSearch.*;
   import static spice.basic.AngularUnits.*;
   import static spice.basic.DistanceUnits.*;
   import static spice.basic.TimeConstants.*;

   class GFRayInFOVSearchEx1
   {
      //
      // Load the JNISpice shared object library
      // at initialization time.
      //
      static { System.loadLibrary( "JNISpice" ); }

      public static void main ( String[] args )
      {
         try {

            //
            // Constants
            //
            final String TIMFMT  =
               "YYYY MON DD HR:MN:SC.###### (TDB)::TDB::RND";

            final int    NINTVLS = 100;

            //
            // Load kernels.
            //
            KernelDatabase.load ( "gftfov_ex1.tm" );

            //
            // Declare the SPICE windows we'll need for the searches
            // and window arithmetic. The result window will be
            // assigned values later; the confinement window must
            // be non-null before it's used.
            //
            SpiceWindow result     = null;
            SpiceWindow cnfine     = new SpiceWindow();

            //
            // Declare and assign values to variables required to
            // specify the geometric condition to search for.
            //

            //
            // Correct the star direction for stellar aberration when
            // we conduct the search.
            //
            AberrationCorrection abcorr =
               new AberrationCorrection( "S" );
            Body observer         = new Body ( "Cassini" );
            Instrument inst       = new Instrument ( "CASSINI_ISS_WAC" );
            ReferenceFrame rframe = new ReferenceFrame( "J2000" );
            double stepsz         = 10.0;

            //
            // Store the time bounds of our search interval in
            // the `cnfine' confinement window.
            //
            TDBTime et0 = new TDBTime ( "2004 JUN 11 06:30:00 TDB" );
            TDBTime et1 = new TDBTime ( "2004 JUN 11 12:00:00 TDB" );

            cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );

            //
            // Create a unit direction vector pointing from observer to star.
            // We'll assume the direction is constant during the confinement
            // window, and we'll use et0 as the epoch at which to compute the
            // direction from the spacecraft to the star.
            //
            // The data below are for the star with catalog number 6000
            // in the Hipparcos catalog. Angular units are degrees; epochs
            // have units of Julian years and have a reference epoch of J1950.
            // The reference frame is J2000.
            //
            int    catno        = 6000;

            double parallax_deg = 0.000001056d;

            double ra_deg_0     = 19.290789927d;
            double ra_pm        = -0.000000720d;
            double ra_epoch     = 41.2000d;

            double dec_deg_0    =  2.015271007d;
            double dec_pm       =  0.000001814d;
            double dec_epoch    = 41.1300d;

            //
            // Correct the star's direction for proper motion.
            //
            // The argument t represents et0 as Julian years past J1950.
            //
            JEDTime jed              = new JEDTime( et0 );
            JEDDuration daysPast1950 = jed.sub( new JEDTime(J1950)  );

            double t         = daysPast1950.getMeasure()/365.25d;

            double dtra      = t - ra_epoch;
            double dtdec     = t - dec_epoch;

            double ra_deg    = ra_deg_0  +  dtra  * ra_pm;
            double dec_deg   = dec_deg_0 +  dtdec * dec_pm;

            double ra        = ra_deg  * RPD;
            double dec       = dec_deg * RPD;

            RADecCoordinates starRADec =
               new RADecCoordinates ( 1.0, ra, dec );

            Vector3 starpos  = starRADec.toRectangular();

            //
            // Correct star position for parallax applicable at
            // the Cassini orbiter's position. (The parallax effect
            // is negligible in this case; we're simply demonstrating
            // the computation.)
            //
            double parallax = parallax_deg * RPD;
            double stardist = AU.toKm() / Math.tan(parallax);

            //
            // Scale the star's direction vector by its distance from
            // the solar system barycenter. Subtract off the position
            // of the spacecraft relative to the solar system barycenter;
            // the result is the ray's direction vector.
            //
            starpos = starpos.scale( stardist );

            PositionVector pos =
               new PositionVector ( observer,
                                    et0,
                                    new ReferenceFrame( "J2000" ),
                                    new AberrationCorrection("NONE"),
                                    new Body( "solar system barycenter" )  );

            Vector3 raydir = starpos.sub( pos );


            System.out.format ( "%n"                            +
                                " Instrument:            %s%n"  +
                                " Star's catalog number: %d%n"  +
                                "%n",
                                inst.getName(),
                                catno                         );

            //
            // Create a GF ray in FOV search instance.
            // This instance specifies the geometric condition
            // to be found.
            //
            GFRayInFOVSearch search =

               new GFRayInFOVSearch ( inst,   raydir, rframe,
                                      abcorr, observer        );
            //
            // Run the search.
            //
            // Specify the maximum number of intervals in the result
            // window.
            //
            result = search.run( cnfine, stepsz, NINTVLS );

            //
            // Display results.
            //
            int count = result.card();

            if ( count == 0 )
            {
               System.out.format ( "No FOV intersection found.%n" );
            }
            else
            {
               double[] interval = new double[2];
               String[] timstr   = new String[2];

               System.out.format
                  ( "  Visibility start time              Stop time%n" );

               for ( int i = 0;  i < count;  i++ )
               {
                  //
                  // Fetch the endpoints of the Ith interval
                  // of the result window.
                  //
                  interval = result.getInterval( i );

                  for ( int j = 0;  j < 2;  j++ )
                  {
                     timstr[j] = ( new TDBTime(interval[j]) ).toString(TIMFMT);
                  }

                  System.out.format( "  %s  %s%n", timstr[0], timstr[1] );
               }
            }
            System.out.println( "" );
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

   Instrument:            CASSINI_ISS_WAC
   Star's catalog number: 6000

    Visibility start time              Stop time
    2004 JUN 11 06:30:00.000000 (TDB)  2004 JUN 11 12:00:00.000000 (TDB)

</pre>






<p> Version 1.0.0 29-DEC-2009 (NJB)
*/
public class GFRayInFOVSearch extends GFBinaryStateSearch
{

   //
   // Fields
   //
   private AberrationCorrection      abcorr;
   private Body                      observer;
   private Instrument                inst;
   private ReferenceFrame            rframe;
   private Vector3                   rayDir;


   //
   // Constructors
   //

   public GFRayInFOVSearch ( Instrument                inst,
                             Vector3                   rayDir,
                             ReferenceFrame            rframe,
                             AberrationCorrection      abcorr,
                             Body                      observer )
   {
      //
      // Just save the inputs. It would be nice to perform
      // error checking at this point; the only practical way
      // to do that is call the ZZGFFVIN initialization entry point.
      //
      // Maybe later.
      //
      this.inst              = inst;
      this.rayDir            = rayDir;
      this.rframe            = rframe;
      this.abcorr            = abcorr;
      this.observer          = observer;
   }


   /**
   Run a search over a specified confinement window, using
   a specified step size (units are TDB seconds).
   */
   public SpiceWindow run ( SpiceWindow   confinementWindow,
                            double        step,
                            int           maxResultIntervals )

      throws SpiceException
   {

      double[] resultArray = CSPICE.gfrfov ( inst.getName(),
                                             rayDir.toArray(),
                                             rframe.getName(),
                                             abcorr.getName(),
                                             observer.getName(),
                                             step,
                                             maxResultIntervals,
                                             confinementWindow.toArray() );

      return (  new SpiceWindow( resultArray )  );
   }










}
