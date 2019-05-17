

package spice.basic;

/**
Class GFDistanceSearch supports searches for
distance events.

<h2>Code Examples</h2>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<p>1)  Find times during the first three months of the year 2007
when the Earth-Moon distance is greater than 400000 km.
Display the start and stop times of the time intervals
over which this constraint is met, along with the Earth-Moon
distance at each interval endpoint.

<p>      We expect the Earth-Moon distance to be an oscillatory function
with extrema roughly two weeks apart. Using a step size of one
day will guarantee that the GF system will find all distance
extrema. (Recall that a search for distance extrema is an
intermediate step in the GF search process.)

<p>      Use the meta-kernel shown below to load the required SPICE
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
   import static spice.basic.TimeConstants.*;

   class GFDistanceSearchEx1
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
            final String      TIMFMT  = "YYYY-MON-DD HR:MN:SC.###";

            //
            // Declare the needed windows. The confinement window
            // must be non-null, since we'll later call the "insert"
            // method to add values to it. The result window will
            // be assigned a value by the `run' method.
            //
            SpiceWindow       confin = new SpiceWindow();
            SpiceWindow       result = null;

            AberrationCorrection
                              abcorr = new AberrationCorrection( "NONE" );
            Body              target = new Body ( "MOON" );
            Body              obsrvr = new Body ( "EARTH" );
            ReferenceFrame    J2000  = new ReferenceFrame( "J2000" );

            //
            // Load kernels.
            //
            KernelDatabase.load ( "standard.tm" );

            //
            // Store the time bounds of our search interval in
            // the confinement window.
            //
            double begtim = ( new TDBTime ( "2007 JAN 01" ) ).getTDBSeconds();
            double endtim = ( new TDBTime ( "2007 APR 01" ) ).getTDBSeconds();

            confin.insert( begtim, endtim );


            //
            // Specify the search, including the relational constraint.
            // We're looking for times when the observer-target distance
            // is greater than 4.0e5 km.
            //
            GFDistanceSearch search =

               new GFDistanceSearch ( target, abcorr, obsrvr );

            GFConstraint relate =
               GFConstraint.createReferenceConstraint( GREATER_THAN, 4.0e5 );

            //
            // Search using a step size of 1 day (in units of seconds).
            //
            double step = SPD;

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
               double[]       interval = new double[2];
               PositionVector pos;
               String         begstr;
               String         endstr;
               TDBTime        start;
               TDBTime        stop;

               for ( int i = 0;  i < count;  i++ )
               {
                  //
                  // Fetch the endpoints of the Ith interval
                  // of the result window.
                  //
                  interval = result.getInterval( i );

                  //
                  // Check the distance at the interval's
                  // start and stop times.
                  //
                  start = new TDBTime( interval[0] );
                  stop  = new TDBTime( interval[1] );

                  pos =  new PositionVector ( target, start, J2000,
                                              abcorr, obsrvr       );

                  System.out.format ( "Start time, distance = %s %17.9f%n",
                                      start.toString(TIMFMT),
                                      pos.norm()                          );

                  pos =  new PositionVector ( target, stop, J2000,
                                              abcorr, obsrvr       );

                  System.out.format ( "Stop time, distance  = %s %17.9f%n",
                                      stop.toString(TIMFMT),
                                      pos.norm()                          );
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
   Start time, distance = 2007-JAN-08 00:10:02.439  399999.999999989
   Stop time, distance  = 2007-JAN-13 06:36:42.770  400000.000000010
   Start time, distance = 2007-FEB-04 07:01:30.094  399999.999999990
   Stop time, distance  = 2007-FEB-10 09:29:56.659  399999.999999998
   Start time, distance = 2007-MAR-03 00:19:19.998  400000.000000006
   Stop time, distance  = 2007-MAR-10 14:03:33.312  400000.000000007
   Start time, distance = 2007-MAR-29 22:52:52.961  399999.999999995
   Stop time, distance  = 2007-APR-01 00:00:00.000  404531.955232216
</pre>




<p> Version 1.0.0 29-DEC-2009 (NJB)
*/
public class GFDistanceSearch extends GFNumericSearch
{
   //
   // Public constants
   //


   //
   // Fields
   //
   private Body                      target;
   private AberrationCorrection      abcorr;
   private Body                      observer;

   //
   // Constructors
   //

   public GFDistanceSearch ( Body                      target,
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
      this.target            = target;
      this.abcorr            = abcorr;
      this.observer          = observer;
   }


   /**
   Run a distance search over a specified confinement window, using
   a specified constraint and step size (units are TDB seconds).
   */
   public SpiceWindow run ( SpiceWindow   confinementWindow,
                            GFConstraint  constraint,
                            double        step,
                            int           maxWorkspaceIntervals )

      throws SpiceException
   {

      double[] resultArray = CSPICE.gfdist ( target.getName(),
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
