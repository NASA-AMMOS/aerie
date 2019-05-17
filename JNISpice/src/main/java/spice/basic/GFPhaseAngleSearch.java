package spice.basic;


/**
<p>Class GFPhaseSearch specifies a search for phase angle events.

<h3> Disclaimer </h3>

<pre>
   THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE
   CALIFORNIA INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S.
   GOVERNMENT CONTRACT WITH THE NATIONAL AERONAUTICS AND SPACE
   ADMINISTRATION (NASA). THE SOFTWARE IS TECHNOLOGY AND SOFTWARE
   PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS AND IS PROVIDED "AS-IS"
   TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, INCLUDING ANY
   WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR A
   PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC
   SECTIONS 2312-2313) OR FOR ANY PURPOSE WHATSOEVER, FOR THE
   SOFTWARE AND RELATED MATERIALS, HOWEVER USED.

   IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA
   BE LIABLE FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT
   LIMITED TO, INCIDENTAL OR CONSEQUENTIAL DAMAGES OF ANY KIND,
   INCLUDING ECONOMIC DAMAGE OR INJURY TO PROPERTY AND LOST PROFITS,
   REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE ADVISED, HAVE
   REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.

   RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF
   THE SOFTWARE AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY
   CALTECH AND NASA FOR ALL THIRD-PARTY CLAIMS RESULTING FROM THE
   ACTIONS OF RECIPIENT IN THE USE OF THE SOFTWARE.
</pre>


<h3> Required_Reading </h3>

   <pre>
   GF
   NAIF_IDS
   SPK
   TIME
   WINDOWS
   </pre>

<h3> Keywords </h3>

   <pre>
   EVENT
   GEOMETRY
   EPHEMERIS
   SEARCH
   WINDOW
   </pre>

<h3> Parameters </h3>

   <pre>
   </pre>

<h3> Exceptions </h3>

   <pre>
   </pre>

<h3> Particulars </h3>

   <pre>
   </pre>

<h3><a name="EXAMPLE"> Examples </a></h3>

   <pre>
   The numerical results shown for these examples may differ across
   platforms. The results depend on the SPICE kernels used as
   input, the compiler and supporting libraries, and the machine
   specific arithmetic implementation.

      Use the meta-kernel shown below to load the required SPICE
      kernels.

         KPL/MK

         File name: standard.tm

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
            pck00009.tpc                  Planet orientation and
                                          radii
            naif0009.tls                  Leapseconds

         \begindata

            KERNELS_TO_LOAD = ( 'de421.bsp',
                                'pck00009.tpc',
                                'naif0009.tls'  )

         \begintext

   </pre>
   <p>Example:
   <pre>

   Determine the time windows from December 1, 2006 UTC to
   January 31, 2007 UTC for which the sun-moon-earth configuration
   phase angle satisfies the relation conditions with respect to a
   reference value of .57598845 radians (the phase angle at
   January 1, 2007 00:00:00.000 UTC, 33.001707 degrees). Also
   determine the time windows corresponding to the local maximum and
   minimum phase angles, and the absolute maximum and minimum phase
   angles during the search interval. The configuration defines the
   sun as the illuminator, the moon as the target, and the earth as
   the observer.

   import spice.basic.*;
   import static spice.basic.GFConstraint.*;
   import static spice.basic.TimeConstants.*;

   class GFPhaseSearch_t
      {

      static String[]   relation = { "LOCMIN", "ABSMIN",
                                     "LOCMAX", "ABSMAX" };

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
                            abcorr = new AberrationCorrection( "LT+S" );
            Body            target = new Body ( "MOON" );
            Body            illum  = new Body ( "SUN" );
            Body            obsrvr = new Body ( "EARTH" );

            //
            // Load kernels.
            //
            KernelDatabase.load ( "/kernels/standard.tm" );

            //
            // Store the time bounds of our search interval in
            // the confinement window.
            //

            double begtim = ( new TDBTime ( "2006 DEC 01" ) ).getTDBSeconds();
            double endtim = ( new TDBTime ( "2007 JAN 31" ) ).getTDBSeconds();

            confin.insert( begtim, endtim );

            GFPhaseSearch search = 
                           new GFPhaseSearch( target, illum, abcorr, obsrvr );

            //
            // Search using a step size of 1 day (in units of seconds).
            //
            double step = SPD;

            for ( int k = 0;  k < relation.length;  k++ )
               {
               GFConstraint relate =
               GFConstraint.createExtremumConstraint( relation[k] );

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
                  TDBTime        start;
                  TDBTime        stop;

                  System.out.format ( "Relation = %s%n", relation[k] );

                  for ( int i = 0;  i < count;  i++ )
                     {
                     //
                     // Fetch the endpoints of the Ith interval of the result
                     // window.
                     //
                     interval = result.getInterval( i );

                     //
                     // Check the distance at the interval's
                     // start and stop times.
                     //
                     start = new TDBTime( interval[0] );
                     stop  = new TDBTime( interval[1] );

                     System.out.format ( "Start time = %s %n",
                                        start.toString(TIMFMT) );
                     System.out.format ( "Stop time  = %s %n",
                                        stop.toString(TIMFMT) );
                     }

                  System.out.format ( "%n" );

                  }

               }

            }
         catch ( SpiceException exc )
            {
            exc.printStackTrace();
            }

         }

      }
  </pre>

The program outputs:

   <pre>
      Relation = LOCMIN
      Start time = 2006-DEC-05 00:16:50.317
      Stop time  = 2006-DEC-05 00:16:50.317
      Start time = 2007-JAN-03 14:18:31.977
      Stop time  = 2007-JAN-03 14:18:31.977

      Relation = ABSMIN
      Start time = 2007-JAN-03 14:18:31.977
      Stop time  = 2007-JAN-03 14:18:31.977

      Relation = LOCMAX
      Start time = 2006-DEC-20 14:09:10.392
      Stop time  = 2006-DEC-20 14:09:10.392
      Start time = 2007-JAN-19 04:27:54.600
      Stop time  = 2007-JAN-19 04:27:54.600

      Relation = ABSMAX
      Start time = 2007-JAN-19 04:27:54.600
      Stop time  = 2007-JAN-19 04:27:54.600
   </pre>

<h3> Version </h3>

   <pre>
   Version 1.0.0 08-MAR-2014 (NJB)(EDW)
   </pre>

*/
public class GFPhaseAngleSearch extends GFNumericSearch
   {

   //
   // Fields
   //
   private Body                      target;
   private Body                      illum;
   private AberrationCorrection      abcorr;
   private Body                      observer;

   /**
   <p>
   Constructor description.
   </p>

   <h3>I/O</h3>

   <pre>
   </pre>

   <h3> Particulars </h3>

   <pre>
   </pre>

   <h3> Exceptions </h3>

   <pre>
   </pre>

   <h3> Examples </h3>

   @see GFPhaseAngleSearch GFPhaseSearch Example

   */
   public GFPhaseAngleSearch ( Body                      target,
                          Body                      illum,
                          AberrationCorrection      abcorr,
                          Body                      observer )
      {
      //
      // Just save the inputs.
      //
      this.target            = target;
      this.illum             = illum;
      this.abcorr            = abcorr;
      this.observer          = observer;
      }


   /**
   <p>
   Run a phase angle search over a specified confinement window, using
   a specified constraint and step size (units are TDB seconds).
   </p>

   <p>For important details concerning this module's function, please
   refer to the CSPICE routine
   <a href="../../../../doc/html/cspice/gfpa_c.html">gfpa_c</a>.
   </p>


   <h3>I/O</h3>

   <pre>
   </pre>

   <h3> Particulars </h3>

   <pre>
   None.
   </pre>

   <h3> Exceptions </h3>

   <pre>
   </pre>

   <h3> Examples </h3>

   <a href="#EXAMPLE">GFPhaseSearch Example</a> 

   */
   public SpiceWindow run ( SpiceWindow   confinementWindow,
                            GFConstraint  constraint,
                            double        step,
                            int           maxWorkspaceIntervals )
      throws SpiceException
      {

      double[] resultArray = CSPICE.gfpa ( target.getName(),
                                           illum.getName(),
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


   /**
   <p>
   Run a phase angle search over a specified confinement window, using
   a specified constraint, step size, and convergence tolerance.
   </p>

   <p>For important details concerning this module's function, please
   refer to the CSPICE routine
   <a href="../../../../doc/html/cspice/gfpa_c.html">gfpa_c</a>
   and
   <a href="../../../../doc/html/cspice/gfrr_c.html">gfstol_c</a>.
   </p>
   </p>


   <h3>I/O</h3>

   <pre>
   </pre>

   <h3> Particulars </h3>

   <pre>
   None.
   </pre>

   <h3> Exceptions </h3>

   <pre>
   </pre>

   <h3> Examples </h3>

   @see GFPhaseAngleSearch GFPhaseSearch Example

   */

   public SpiceWindow run ( SpiceWindow   confinementWindow,
                            GFConstraint  constraint,
                            double        step,
                            int           maxWorkspaceIntervals,
                            double        tol )
      throws SpiceException
      {

      //
      // Set the tolerance.
      //
      CSPICE.gfstol( tol );

      //
      // Execute the search.
      //
      double[] resultArray = CSPICE.gfpa ( target.getName(),
                                           illum.getName(),
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
