

package spice.basic;

/**
Class GFTargetInFOVSearch conducts searches for time intervals
over which a specified target body appears in a specified instrument
field of view (FOV).


<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<p> 1) Search for times when Saturn's satellite Phoebe is within
      the FOV of the Cassini narrow angle camera (CASSINI_ISS_NAC).
      To simplify the problem, restrict the search to a short time
      period where continuous Cassini bus attitude data are
      available.

<p>      Use a step size of 10 seconds to reduce chances of missing
      short visibility events.

<p>      Use the meta-kernel shown below to load the required SPICE
      kernels.

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

   class GFTargetInFOVSearchEx1
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

            AberrationCorrection
               abcorr = new AberrationCorrection( "LT+S" );

            Body target            = new Body ( "Phoebe"  );
            Body observer          = new Body ( "Cassini" );
            Instrument inst        = new Instrument ( "CASSINI_ISS_NAC" );
            ReferenceFrame tframe  = new ReferenceFrame( "IAU_PHOEBE" );
            String tshape          = ELLIPSOID;
            double stepsz          = 10.0;

            System.out.format ( "%n"                 +
                                " Instrument: %s%n"  +
                                " Target:     %s%n"  +
                                "%n",
                                inst.getName(),
                                target.getName()      );

            //
            // Store the time bounds of our search interval in
            // the `cnfine' confinement window.
            //
            TDBTime et0 = new TDBTime ( "2004 JUN 11 06:30:00 TDB" );
            TDBTime et1 = new TDBTime ( "2004 JUN 11 12:00:00 TDB" );

            cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );

            //
            // Create a GF target in FOV search instance.
            // This instance specifies the geometric condition
            // to be found.
            //
            GFTargetInFOVSearch search =

               new GFTargetInFOVSearch ( inst,   target, tshape,
                                         tframe, abcorr, observer );
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

   Instrument: CASSINI_ISS_NAC
   Target:     PHOEBE

    Visibility start time              Stop time
    2004 JUN 11 07:35:49.958590 (TDB)  2004 JUN 11 08:48:27.485966 (TDB)
    2004 JUN 11 09:03:19.767800 (TDB)  2004 JUN 11 09:35:27.634791 (TDB)
    2004 JUN 11 09:50:19.585474 (TDB)  2004 JUN 11 10:22:27.854254 (TDB)
    2004 JUN 11 10:37:19.332697 (TDB)  2004 JUN 11 11:09:28.116017 (TDB)
    2004 JUN 11 11:24:19.049485 (TDB)  2004 JUN 11 11:56:28.380305 (TDB)

</pre>




<p> Version 1.0.0 29-DEC-2009 (NJB)
*/
public class GFTargetInFOVSearch extends GFBinaryStateSearch
{
   //
   // Public constants
   //
   public final static String ELLIPSOID      = "ELLIPSOID";
   public final static String POINT          = "POINT";


   //
   // Fields
   //
   private AberrationCorrection      abcorr;
   private Body                      observer;
   private Body                      target;
   private Instrument                inst;
   private ReferenceFrame            tframe;
   private String                    tshape;


   //
   // Constructors
   //

   public GFTargetInFOVSearch ( Instrument                inst,
                                Body                      target,
                                String                    tshape,
                                ReferenceFrame            tframe,
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
      this.target            = target;
      this.tshape            = tshape;
      this.tframe            = tframe;
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

      double[] resultArray = CSPICE.gftfov ( inst.getName(),
                                             target.getName(),
                                             tshape,
                                             tframe.getName(),
                                             abcorr.getName(),
                                             observer.getName(),
                                             step,
                                             maxResultIntervals,
                                             confinementWindow.toArray() );

      return (  new SpiceWindow( resultArray )  );
   }










}
