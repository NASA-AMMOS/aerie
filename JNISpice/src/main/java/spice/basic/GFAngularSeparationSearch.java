
package spice.basic;

/**
Class GFAngularSeparationSearch supports searches for
angular separation events.


<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.


<p> 1) Determine the times of local maxima of the angular separation
 between the Moon and Earth as observed from the Sun from
 Jan 1, 2007 to Jan 1 2008.


<p>
The example shown below requires a "standard" set of SPICE
kernels. We list these kernels in a meta kernel named 'standard.tm'.
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

            File name                     Contents
            ---------                     --------
            de421.bsp                     Planetary ephemeris
            pck00008.tpc                  Planet orientation and
                                          radii
            naif0009.tls                  Leapseconds


   \begindata

   KERNELS_TO_LOAD = ( '/kernels/gen/lsk/naif0009.tls'
                       '/kernels/gen/spk/de421.bsp'
                       '/kernels/gen/pck/pck00008.tpc'
                     )
</pre>

<p> Example code begins here.

<pre>
   import spice.basic.*;
   import static spice.basic.GFConstraint.*;

   class GFAngularSeparationSearchEx1
   {
      //
      // Load the JNISpice shared library.
      //
      static { System.loadLibrary( "JNISpice" ); }


      public static void main ( String[] args )
      {
         try
         {
            final int         MAXWIN  = 1000;
            final String      TIMFMT  =
               "YYYY-MON-DD HR:MN:SC.###### (TDB) ::TDB ::RND";

            //
            // Declare the needed windows. The confinement window
            // must be non-null, since we'll later call the "insert"
            // method to add values to it. The result window will
            // be assigned a value by the `run' method.
            //
            SpiceWindow       confin = new SpiceWindow();
            SpiceWindow       result = null;

            Body              targ1  = new Body ( "MOON" );
            ReferenceFrame    frame1 = new ReferenceFrame( " " );
            String            shape1 = GFAngularSeparationSearch.SPHERE;
            Body              targ2  = new Body ( "EARTH" );
            ReferenceFrame    frame2 = new ReferenceFrame( " " );
            String            shape2 = GFAngularSeparationSearch.SPHERE;
            AberrationCorrection
                              abcorr = new AberrationCorrection( "NONE" );
            Body              obsrvr = new Body ( "SUN" );

            GFConstraint      relate =
               GFConstraint.createExtremumConstraint( LOCAL_MAXIMUM );

            //
            // Load kernels.
            //
            KernelDatabase.load ( "standard.tm" );

            //
            // Store the time bounds of our search interval in
            // the confinement window.
            //
            double begtim = ( new TDBTime ( "2007 JAN 01" ) ).getTDBSeconds();
            double endtim = ( new TDBTime ( "2008 JAN 01" ) ).getTDBSeconds();

            confin.insert( begtim, endtim );

            //
            // Search using a step size of 6 days (in units of seconds).
            //
            double step   = 6.0 * TimeConstants.SPD;

            //
            // Specify the search.
            //
            GFAngularSeparationSearch search =

               new GFAngularSeparationSearch ( targ1,  shape1, frame1,
                                               targ2,  shape2, frame2,
                                               abcorr, obsrvr         );

            //
            // Run the search over the confinement window,
            // using the selected constraint and step size.
            // Indicate the maximum number of workspace
            // intervals to use.
            //

            result = search.run ( confin, relate, step, MAXWIN );

            //
            // Display the results.
            //
            int count = result.card();

            if ( count == 0 )
            {
               System.out.format ( "Result window is empty.%n%n" );
            }
            else
            {
               double[] interval = new double[2];
               String   begstr;
               String   endstr;

               for ( int i = 0;  i < count;  i++ )
               {
                  //
                  // Fetch the endpoints of the Ith interval
                  // of the result window.
                  //
                  interval = result.getInterval( i );

                  if ( interval[0] == interval[1] )
                  {
                     begstr = ( new TDBTime(interval[0]) ).toString(TIMFMT);

                     System.out.format( "Event time: %s %n", begstr );
                  }
                  else
                  {
                     begstr = ( new TDBTime(interval[0]) ).toString(TIMFMT);
                     endstr = ( new TDBTime(interval[1]) ).toString(TIMFMT);

                     System.out.format( "%nInterval %d%n", i + 1);
                     System.out.format( "From: %s %n",   begstr );
                     System.out.format( "To:   %s %n",   endstr );
                  }
               }
            }
         }
         catch ( SpiceException exc ) {
            exc.printStackTrace();
         }
      }
   }
</pre>

<p> When run on a PC/Linux/java 1.6.0_14/gcc platform,
output from this program was:

<pre>
   Event time: 2007-JAN-11 11:21:20.214305 (TDB)
   Event time: 2007-JAN-26 01:43:41.027309 (TDB)
   Event time: 2007-FEB-10 04:49:53.431964 (TDB)
   Event time: 2007-FEB-24 13:18:18.953256 (TDB)
   Event time: 2007-MAR-11 20:41:59.571964 (TDB)
   Event time: 2007-MAR-26 01:20:26.860201 (TDB)
   Event time: 2007-APR-10 10:24:39.017514 (TDB)
   Event time: 2007-APR-24 14:00:49.422728 (TDB)
   Event time: 2007-MAY-09 21:53:25.643532 (TDB)
   Event time: 2007-MAY-24 03:14:05.873982 (TDB)
   Event time: 2007-JUN-08 07:24:13.686616 (TDB)
   Event time: 2007-JUN-22 16:45:56.506850 (TDB)
   Event time: 2007-JUL-07 15:30:03.706532 (TDB)
   Event time: 2007-JUL-22 06:26:17.397353 (TDB)
   Event time: 2007-AUG-05 23:03:21.625229 (TDB)
   Event time: 2007-AUG-20 20:14:56.801678 (TDB)
   Event time: 2007-SEP-04 07:13:25.162360 (TDB)
   Event time: 2007-SEP-19 10:16:42.721117 (TDB)
   Event time: 2007-OCT-03 17:11:17.188939 (TDB)
   Event time: 2007-OCT-19 00:30:31.300060 (TDB)
   Event time: 2007-NOV-02 05:43:48.902220 (TDB)
   Event time: 2007-NOV-17 14:38:21.314771 (TDB)
   Event time: 2007-DEC-01 20:50:27.562519 (TDB)
   Event time: 2007-DEC-17 04:04:46.933247 (TDB)
   Event time: 2007-DEC-31 13:43:52.558812 (TDB)
</pre>




<p> Version 1.0.0 29-DEC-2009 (NJB)
*/
public class GFAngularSeparationSearch extends GFNumericSearch
{
   //
   // Public constants
   //
   public final static String SPHERE         = "SPHERE";
   public final static String POINT          = "POINT";


   //
   // Fields
   //
   private Body                      target1;
   private String                    shape1;
   private ReferenceFrame            frame1;
   private Body                      target2;
   private String                    shape2;
   private ReferenceFrame            frame2;
   private AberrationCorrection      abcorr;
   private Body                      observer;

   //
   // Constructors
   //

   public GFAngularSeparationSearch ( Body                      target1,
                                      String                    shape1,
                                      ReferenceFrame            frame1,
                                      Body                      target2,
                                      String                    shape2,
                                      ReferenceFrame            frame2,
                                      AberrationCorrection      abcorr,
                                      Body                      observer )
   {
      //
      // Just save the inputs. It would be nice to perform
      // error checking at this point; the only practical way
      // to do that is call the ZZGFOCU initialization entry point.
      //
      // Maybe later.
      //
      this.target1           = target1;
      this.shape1            = shape1;
      this.frame1            = frame1;
      this.target2           = target2;
      this.shape2            = shape2;
      this.frame2            = frame2;
      this.abcorr            = abcorr;
      this.observer          = observer;
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
      double[] resultArray = CSPICE.gfsep ( target1.getName(),
                                            shape1,
                                            frame1.getName(),
                                            target2.getName(),
                                            shape2,
                                            frame2.getName(),
                                            abcorr.getName(),
                                            observer.getName(),
                                            constraint.getCSPICERelation(),
                                            constraint.getReferenceValue(),
                                            constraint.getAdjustmentValue(),
                                            step,
                                            maxWorkspaceIntervals,
                                            confinementWindow.toArray()    );

      return (  new SpiceWindow( resultArray )  );
   }


}
