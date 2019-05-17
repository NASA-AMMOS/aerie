

package spice.basic;

/**
Class GFPositionCoordinateSearch supports searches for
position coordinate events.

<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.


<p>1)  Find times during the year 2009 when the Earth-Moon
distance attains a local maximum.

<pre>
   //
   // Find times during the year 2009 when the Earth-Moon
   // distance attains a local maximum.
   //
   import spice.basic.*;
   import static spice.basic.GFConstraint.*;

   class GFPositionSearchEx1
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
         "YYYY MON DD HR:MN:SC.###### (TDB)::TDB";

      public static void main ( String[] args )
      {
         try {

            System.out.format ( "%nGFPositionCoordinateSearch test:" );

            //
            // Constants
            //
            final int      NINTVLS = 10000;

            //
            // Declare and assign values to variables required to
            // specify the geometric condition to search for.
            //

            AberrationCorrection
               abcorr = new AberrationCorrection( "NONE" );

            Body target            = new Body ( "Moon"  );
            Body observer          = new Body ( "Earth" );
            ReferenceFrame frame   = new ReferenceFrame( "J2000" );
            String coordSys        = Coordinates.LATITUDINAL;
            String coordinate      = Coordinates.RADIUS;

            //
            // Load kernels via a meta-kernel.
            //
            final String META  = "standard.tm";

            KernelDatabase.load ( META );


            //
            // Create a GF position coordinate search instance.
            // This instance specifies the geometric condition
            // to be found.
            //
            GFPositionCoordinateSearch PositionSearch =

               new GFPositionCoordinateSearch ( target,   frame,
                                                abcorr,   observer,
                                                coordSys, coordinate );
            //
            // Set up the GF search constraint.
            //
            GFConstraint locMaxCon =
               createExtremumConstraint ( GFConstraint.LOCAL_MAXIMUM );

            //
            // Set up a simple confinement window.
            //
            TDBTime et0 = new TDBTime ( "2009 Jan 1 00:00:00 TDB" );
            TDBTime et1 = new TDBTime ( "2010 Jan 1 00:00:00 TDB" );

            SpiceWindow confine = new SpiceWindow ();

            confine.insert( et0.getTDBSeconds(),
                            et1.getTDBSeconds() );

            //
            // Select a search step size. Units are TDB seconds.
            //
            double step = 300.0;

            //
            // Run the search over a specified confinement
            // window, using a specified constraint and search step.
            //
            // Specify the maximum number of intervals in the result
            // window.
            //
            SpiceWindow result =

               PositionSearch.run( confine, locMaxCon, step, NINTVLS );

            //
            // Display results, along with the distance maxima.
            //
            int            wncard   = result.card();
            double[]       interval = new double[2];
            TDBTime        start;

            System.out.format( "%n" );

            if ( wncard == 0 )
            {
               System.out.println ( "Result window is empty" );
            }
            else
            {
               for ( int i = 0;  i < wncard;  i++ )
               {
                  interval = result.getInterval( i );

                  //
                  // The interval start and stop times will be
                  // the same; so we use only the start time.
                  //
                  start  = new TDBTime( interval[0] );

                  PositionVector pos =

                     new PositionVector( target, start,
                                         frame,  abcorr, observer );

                  System.out.format ( "[%3d] Time =  %s; Distance (km) = %f%n",
                                      i, start, pos.norm() );
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

   GFPositionCoordinateSearch test:

   [  0] Time =  2009 JAN 23 00:11:27.349 (TDB); Distance (km) = 406118.280521
   [  1] Time =  2009 FEB 19 17:00:27.766 (TDB); Distance (km) = 405129.319072
   [  2] Time =  2009 MAR 19 13:16:31.265 (TDB); Distance (km) = 404299.012195
   [  3] Time =  2009 APR 16 09:16:32.424 (TDB); Distance (km) = 404231.558212
   [  4] Time =  2009 MAY 14 02:58:21.798 (TDB); Distance (km) = 404915.320536
   [  5] Time =  2009 JUN 10 16:04:39.215 (TDB); Distance (km) = 405786.712801
   [  6] Time =  2009 JUL 07 21:39:54.041 (TDB); Distance (km) = 406231.773499
   [  7] Time =  2009 AUG 04 00:42:47.614 (TDB); Distance (km) = 406028.070923
   [  8] Time =  2009 AUG 31 11:03:00.455 (TDB); Distance (km) = 405269.213190
   [  9] Time =  2009 SEP 28 03:33:32.530 (TDB); Distance (km) = 404432.011671
   [ 10] Time =  2009 OCT 25 23:18:56.423 (TDB); Distance (km) = 404166.446368
   [ 11] Time =  2009 NOV 22 20:07:24.518 (TDB); Distance (km) = 404732.974282
   [ 12] Time =  2009 DEC 20 14:54:35.508 (TDB); Distance (km) = 405730.572479
</pre>


<p> Version 1.0.0 29-DEC-2009 (NJB)
*/
public class GFPositionCoordinateSearch extends GFNumericSearch
{
   //
   // Public constants
   //


   //
   // Fields
   //
   private Body                      target;
   private ReferenceFrame            frame;
   private AberrationCorrection      abcorr;
   private Body                      observer;
   private String                    crdsys;
   private String                    coord;

   //
   // Constructors
   //

   public GFPositionCoordinateSearch ( Body                   target,
                                       ReferenceFrame         frame,
                                       AberrationCorrection   abcorr,
                                       Body                   observer,
                                       String                 coordinateSystem,
                                       String                 coordinate      )
   {
      //
      // Just save the inputs. It would be nice to perform
      // error checking at this point; the only practical way
      // to do that is call the ZZGFOCU initialization entry point.
      //
      // Maybe later.
      //
      this.target            = target;
      this.frame             = frame;
      this.abcorr            = abcorr;
      this.observer          = observer;
      this.crdsys            = coordinateSystem;
      this.coord             = coordinate;
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

      double[] resultArray = CSPICE.gfposc ( target.getName(),
                                             frame.getName(),
                                             abcorr.getName(),
                                             observer.getName(),
                                             crdsys,
                                             coord,
                                             constraint.getCSPICERelation(),
                                             constraint.getReferenceValue(),
                                             constraint.getAdjustmentValue(),
                                             step,
                                             maxWorkspaceIntervals,
                                             confinementWindow.toArray()    );

      return (  new SpiceWindow( resultArray )  );
   }
}
