

package spice.basic;

/**
Class GFSurfaceInterceptCoordinateSearch supports searches for
sub-observer point coordinate events.

<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

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


   The examples shown below require a frames kernel defining a
   a dynamic frame, Sun-Earth Motion. The frame defined by the
   sun-to-earth direction vector as the X axis. The Y axis in the
   earth orbital plane, and Z completing the right hand system.

   We name this frames kernel "sem.tf".
<pre>
   \begindata

        FRAME_SEM                     =  10100000
        FRAME_10100000_NAME           = 'SEM'
        FRAME_10100000_CLASS          =  5
        FRAME_10100000_CLASS_ID       =  10100000
        FRAME_10100000_CENTER         =  10
        FRAME_10100000_RELATIVE       = 'J2000'
        FRAME_10100000_DEF_STYLE      = 'PARAMETERIZED'
        FRAME_10100000_FAMILY         = 'TWO-VECTOR'
        FRAME_10100000_PRI_AXIS       = 'X'
        FRAME_10100000_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
        FRAME_10100000_PRI_OBSERVER   = 'SUN'
        FRAME_10100000_PRI_TARGET     = 'EARTH'
        FRAME_10100000_PRI_ABCORR     = 'NONE'
        FRAME_10100000_SEC_AXIS       = 'Y'
        FRAME_10100000_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
        FRAME_10100000_SEC_OBSERVER   = 'SUN'
        FRAME_10100000_SEC_TARGET     = 'EARTH'
        FRAME_10100000_SEC_ABCORR     = 'NONE'
        FRAME_10100000_SEC_FRAME      = 'J2000'
</pre>


<p>1) Find the time during 2007 for which the latitude of the
      intercept point of the vector pointing from the sun towards
     the earth in the IAU_EARTH frame equals zero i.e. the intercept
      point crosses the equator.

<p> Example code begins here.

<pre>
   import spice.basic.*;
   import static spice.basic.AngularUnits.*;
   import static spice.basic.Coordinates.*;
   import static spice.basic.GFConstraint.*;
   import static spice.basic.GFSurfaceInterceptCoordinateSearch.*;
   import static spice.basic.TimeConstants.*;

   class GFSurfaceInterceptSearchEx1
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
               abcorr = new AberrationCorrection( "NONE" );

            Body target            = new Body ( "Earth" );
            Body observer          = new Body ( "Sun"   );
            ReferenceFrame fixref  = new ReferenceFrame( "IAU_EARTH" );
            ReferenceFrame dref    = new ReferenceFrame( "SEM" );
            String coordSys;
            String coordinate;
            String method          = ELLIPSOID;
            Vector3 dvec           = new Vector3 ( 1.0, 0.0, 0.0 );

            //
            // Load kernels.
            //
            KernelDatabase.load ( "standard.tm" );
            KernelDatabase.load ( "sem.tf"      );

            //
            // Store the time bounds of our search interval in
            // the `cnfine' confinement window.
            //
            TDBTime et0 = new TDBTime ( "2007 Jan 1 00:00:00" );
            TDBTime et1 = new TDBTime ( "2008 Jan 1 00:00:00" );

            cnfine.insert( et0.getTDBSeconds(),
                           et1.getTDBSeconds() );
            //
            // The latitude varies relatively slowly, ~46 degrees during the
            // year. The extrema occur approximately every six months.
            // Search using a step size less than half that value (180 days).
            // For this example use ninety days (in units of seconds).
            //
            double step = 90.0 * SPD;

            //
            // Create a GF position coordinate search instance.
            // This instance specifies the geometric condition
            // to be found.
            //
            coordSys   = LATITUDINAL;
            coordinate = LATITUDE;

            GFSurfaceInterceptCoordinateSearch latitudeSearch =

               new GFSurfaceInterceptCoordinateSearch ( target,    fixref,
                                                        method,    abcorr,
                                                        observer,  dref,
                                                        dvec,      coordSys,
                                                        coordinate           );
            //
            // Set up the GF search constraint.
            //
            GFConstraint latEQCons =
               createReferenceConstraint ( "=", 0.0 );


            //
            // Run the search.
            //
            // Specify the maximum number of intervals in the result
            // window.
            //
            result = latitudeSearch.run( cnfine, latEQCons, step, NINTVLS );

            //
            // Display results.
            //
            int count = result.card();

            if ( count == 0 )
            {
               System.out.println ( "Result window is empty.%n%n" );
            }
            else
            {
               double[] interval = new double[2];
               String   begstr;
               String   endstr;
               SubObserverRecord sr;
               LatitudinalCoordinates latcoords;

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


<p> When run on a PC/Linux/java 1.6.0_14/gcc platform, the output
   from this program was:

<pre>
Event time: 2007 MAR 21 00:01:25.500672 (TDB)
Event time: 2007 SEP 23 09:46:39.579484 (TDB)
</pre>


<p>2) Find the time during 2007 for which the intercept point on the
      earth of the sun-to-earth vector as described in Example 1 in
      the IAU_EARTH frame lies within a geodetic latitude-longitude
      "box" defined as
<pre>
         16 degrees &#62= latitude  &#60= 17 degrees
         85 degrees &#62= longitude &#60= 86 degrees
</pre>
      This problem requires four searches, each search on one of the
      box restrictions. The user needs also realize the temporal behavior
      of latitude greatly differs from that of the longitude. The
      the intercept latitude varies between approximately 23.44 degrees
      and -23.44 degrees during the year. The intercept longitude varies
      between -180 degrees and 180 degrees in one day.


<p> Example code begins here.

<pre>
   import spice.basic.*;
   import static spice.basic.AngularUnits.*;
   import static spice.basic.Coordinates.*;
   import static spice.basic.GFConstraint.*;
   import static spice.basic.GFSurfaceInterceptCoordinateSearch.*;
   import static spice.basic.TimeConstants.*;

   class GFSurfaceInterceptSearchEx2
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
            // Declare the SPICE windows we'll need for the searches
            // and window arithmetic. The result windows will be
            // assigned values later; the confinement window must
            // be non-null before it's used.
            //
            SpiceWindow result1    = null;
            SpiceWindow result2    = null;
            SpiceWindow result3    = null;
            SpiceWindow result4    = null;
            SpiceWindow cnfine     = new SpiceWindow();

            //
            // Declare and assign values to variables required to
            // specify the geometric condition to search for.
            //

            AberrationCorrection
               abcorr = new AberrationCorrection( "NONE" );

            Body target            = new Body ( "Earth" );
            Body observer          = new Body ( "Sun"   );
            ReferenceFrame fixref  = new ReferenceFrame( "IAU_EARTH" );
            ReferenceFrame dref    = new ReferenceFrame( "SEM" );
            String coordSys;
            String coordinate;
            String method          = ELLIPSOID;
            Vector3 dvec           = new Vector3 ( 1.0, 0.0, 0.0 );
            //
            // Load kernels.
            //
            KernelDatabase.load ( "standard.tm" );
            KernelDatabase.load ( "sem.tf"      );

            //
            // Store the time bounds of our search interval in
            // the `cnfine' confinement window.
            //
            TDBTime et0 = new TDBTime ( "2007 Jan 1 00:00:00" );
            TDBTime et1 = new TDBTime ( "2008 Jan 1 00:00:00" );

            cnfine.insert( et0.getTDBSeconds(),
                           et1.getTDBSeconds() );

            //
            // Perform four searches to determine the times when the
            // latitude-longitude box restriction conditions apply to
            // the subpoint vector.
            //
            // Perform the searches such that the result window of a search
            // serves as the confinement window of the subsequent search.
            //
            // Since the latitude coordinate varies slowly and is well behaved
            // over the time of the confinement window, search first for the
            // windows satisfying the latitude requirements, then use that
            // result as confinement for the longitude search.
            //
            // The latitude varies relatively slowly, ~46 degrees during the
            // year. The extrema occur approximately every six months.
            // Search using a step size less than half that value (180 days).
            // For this example use ninety days (in units of seconds).
            //
            double step = 90.0 * SPD;

            //
            // Create a GF position coordinate search instance.
            // This instance specifies the geometric condition
            // to be found.
            //
            coordSys   = GEODETIC;
            coordinate = LATITUDE;

            GFSurfaceInterceptCoordinateSearch latitudeSearch =

               new GFSurfaceInterceptCoordinateSearch ( target,    fixref,
                                                        method,    abcorr,
                                                        observer,  dref,
                                                        dvec,      coordSys,
                                                        coordinate           );
            //
            // Set up the first GF search constraint.
            //
            GFConstraint latGTcons =
               createReferenceConstraint ( ">", 16.0 * RPD );


            //
            // Run the first latitude search.
            //
            // Specify the maximum number of intervals in the result
            // window.
            //
            result1 = latitudeSearch.run( cnfine, latGTcons, step, NINTVLS );

            //
            // Set up the second GF search constraint.
            //
            GFConstraint latLTcons =
               createReferenceConstraint ( "<", 17.0 * RPD );

            //
            // Run the second latitude search over the previous result window.
            //
            result2 = latitudeSearch.run( result1, latLTcons, step, NINTVLS );



            //
            // Now perform the longitude searches.
            //

            coordinate = LONGITUDE;

            GFSurfaceInterceptCoordinateSearch longitudeSearch =

               new GFSurfaceInterceptCoordinateSearch ( target,    fixref,
                                                        method,    abcorr,
                                                        observer,  dref,
                                                        dvec,      coordSys,
                                                        coordinate           );

            // Reset the stepsize to something appropriate for the 360
            // degrees in 24 hours domain. The longitude shows near
            // linear behavior so use a step size less than half the period
            // of twelve hours. Ten hours will suffice in this case.
            //
            step = 10.0 * 3600.0;

            GFConstraint lonGTcons =
               createReferenceConstraint ( ">", 85.0 * RPD );

            result3 = longitudeSearch.run( result2, lonGTcons, step, NINTVLS );

            //
            // Contract the endpoints of the window `result3' to account
            // for possible round-off error at the -180/180 degree branch.
            //
            // A contraction value of a millisecond should eliminate
            // any branch crossing caused by round-off.

            result3.contract( 1.0e-3, 1.0e-3 );

            GFConstraint lonLTcons =
               createReferenceConstraint ( "<", 86.0 * RPD );

            result4 = longitudeSearch.run( result3, lonLTcons, step, NINTVLS );

            //
            // Display results.
            //
            int count = result4.card();

            if ( count == 0 )
            {
               System.out.format ( "Result window is empty.%n%n" );
            }
            else
            {
               double[] interval = new double[2];
               String   begstr;
               String   endstr;
               SubObserverRecord sr;
               LatitudinalCoordinates latcoords;

               for ( int i = 0;  i < count;  i++ )
               {
                  //
                  // Fetch the endpoints of the Ith interval
                  // of the result window.
                  //
                  interval = result4.getInterval( i );

                  begstr = ( new TDBTime(interval[0]) ).toString(TIMFMT);
                  endstr = ( new TDBTime(interval[1]) ).toString(TIMFMT);

                  System.out.format( "%nInterval %d%n", i + 1);
                  System.out.format( "Beginning TDB %s %n",   begstr );
                  System.out.format( "Ending TDB    %s %n",   endstr );
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

   Interval 1
   Beginning TDB 2007 MAY 05 06:14:04.637735 (TDB)
   Ending TDB    2007 MAY 05 06:18:03.621906 (TDB)

   Interval 2
   Beginning TDB 2007 MAY 06 06:13:59.583483 (TDB)
   Ending TDB    2007 MAY 06 06:17:58.569239 (TDB)

   Interval 3
   Beginning TDB 2007 MAY 07 06:13:55.102940 (TDB)
   Ending TDB    2007 MAY 07 06:17:54.090298 (TDB)

   Interval 4
   Beginning TDB 2007 AUG 06 06:23:17.282927 (TDB)
   Ending TDB    2007 AUG 06 06:27:16.264009 (TDB)

   Interval 5
   Beginning TDB 2007 AUG 07 06:23:10.545441 (TDB)
   Ending TDB    2007 AUG 07 06:27:09.524924 (TDB)

   Interval 6
   Beginning TDB 2007 AUG 08 06:23:03.233996 (TDB)
   Ending TDB    2007 AUG 08 06:27:02.211888 (TDB)

   Interval 7
   Beginning TDB 2007 AUG 09 06:22:55.351256 (TDB)
   Ending TDB    2007 AUG 09 06:26:54.327565 (TDB)
</pre>


<p> Version 1.0.0 29-DEC-2009 (NJB)
*/
public class GFSurfaceInterceptCoordinateSearch extends GFNumericSearch
{
   //
   // Public Constants
   //

   //
   // The values below are the geometric "methods" supported by
   // the SubObserverRecord constructor.
   //
   public final static String ELLIPSOID = "ELLIPSOID";

   //
   // Fields
   //
   private Body                      target;
   private ReferenceFrame            fixref;
   private String                    method;
   private AberrationCorrection      abcorr;
   private Body                      observer;
   private ReferenceFrame            dref;
   private Vector3                   dvec;
   private String                    crdsys;
   private String                    coord;

   //
   // Constructors
   //

   public GFSurfaceInterceptCoordinateSearch ( Body                  target,
                                               ReferenceFrame        fixref,
                                               String                method,
                                               AberrationCorrection  abcorr,
                                               Body                  observer,
                                               ReferenceFrame        dref,
                                               Vector3               dvec,
                                               String         coordinateSystem,
                                               String              coordinate )
   {
      //
      // Just save the inputs. It would be nice to perform
      // error checking at this point; the only practical way
      // to do that is call the ZZGFCOIN initialization entry point.
      //
      // Maybe later.
      //
      this.target            = target;
      this.fixref            = fixref;
      this.method            = method;
      this.abcorr            = abcorr;
      this.observer          = observer;
      this.dref              = dref;
      this.dvec              = dvec;
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
      double[] resultArray = CSPICE.gfsntc ( target.getName(),
                                             fixref.getName(),
                                             method,
                                             abcorr.getName(),
                                             observer.getName(),
                                             dref.getName(),
                                             dvec.toArray(),
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
