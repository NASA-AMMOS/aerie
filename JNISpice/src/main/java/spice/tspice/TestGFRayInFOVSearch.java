
package spice.tspice;


import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;



/**
Class TestGFRayInFOVSearch provides methods that implement test families for
the class GFRayInFOVSearch.

<h3>Version 1.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 23-OCT-2009 (NJB)</h3>
*/
public class TestGFRayInFOVSearch extends Object
{

   //
   // Class constants
   //
   private static String  REF1          = "J2000";
   private static String  IK            = "nat.ti";
   private static String  PCK           = "nat.tpc";
   private static String  SPK           = "nat.bsp";


   //
   // Class variables
   //



   //
   // Methods
   //



   /**
   Test GFRayInFOVSearch and associated classes.
   */
   public static boolean f_GFRayInFOVSearch()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIMTOL = 1.e-6;
      final int                         MAXIVL = 100000;
      final int                         MAXWIN = 2 * MAXIVL;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              observer;

      GFRayInFOVSearch                  search;

      Instrument                        inst;

      ReferenceFrame                    rframe;

      SpiceWindow                       cnfine;
      SpiceWindow                       result = null;

      String                            qname;
      String                            bshape;
      String                            fshape;

      TDBTime                           et0;
      TDBTime                           et1;

      Vector3                           raydir;

      boolean                           ok;

      double[]                          interval;
      double                            step;
      double                            xsec;

      int                               handle = 0;
      int                               n;


      //
      //  We enclose all tests in a try/catch block in order to
      //  facilitate handling unexpected exceptions.  Unexpected
      //  exceptions are trapped by the catch block at the end of
      //  the routine; expected exceptions are handled locally by
      //  catch blocks associated with error handling test cases.
      //
      //  Therefore, JNISpice calls that are expected to succeed don't
      //  have any subsequent "chckxc" type calls following them, nor
      //  are they wrapped in in try/catch blocks.
      //
      //  Expected exceptions that are *not* thrown are tested
      //  via a call to {@link spice.testutils.Testutils#dogDidNotBark}.
      //

      try
      {

         JNITestutils.topen ( "f_GFRayInFOVSearch" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: create and load kernels." );


         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();

         JNITestutils.tstlsk();

         //
         // Delete PCK if it exists. Create and load a PCK file.
         // Do NOT delete the file afterward.
         //
         ( new File ( PCK ) ).delete();

         JNITestutils.natpck( PCK, true, true );


         //
         // Delete SPK if it exists. Create and load a new
         // version of the file.
         //
         ( new File ( SPK ) ).delete();

         handle = JNITestutils.natspk( SPK, true );

         //
         // Create and load an IK.
         //
         ( new File ( IK ) ).delete();

         JNITestutils.natik( IK, SPK, PCK, true, false );



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Ray's direction vector is zero."   );


         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
         et1     = new TDBTime( "2000 Feb 1 00:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.
         //
         inst     = new Instrument( "ALPHA_ELLIPSE_NONE" );

         rframe   = new ReferenceFrame( "J2000" );

         observer = new Body( "sun" );

         abcorr   = new AberrationCorrection( "NONE" );

         step     = 30000.0;


         try
         {
            raydir = new Vector3();

            search   = new GFRayInFOVSearch( inst,    raydir,
                                             rframe,  abcorr,  observer );

            result   = search.run ( cnfine, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(ZEROVECTOR)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(ZEROVECTOR)", ex );
         }






         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Result window too small (detected during " +
                               "search initialization)"                      );

         try
         {
            raydir   = new Vector3( 0.0, 0.0, 1.0 );

            search   = new GFRayInFOVSearch( inst,    raydir,
                                             rframe,  abcorr,  observer );
            cnfine   = new SpiceWindow();

            step     = 30000.0;


            result   = search.run ( cnfine, step, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(WINDOWTOOSMALL)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(WINDOWTOOSMALL)", ex );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Result window too small (detected during " +
                               "search execution)"                         );

         try
         {
            cnfine  = new SpiceWindow();

            et0     = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
            et1     = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

            cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                     et1.getTDBSeconds() );

            //
            // Set up the search geometry parameters.
            //
            inst     = new Instrument( "ALPHA_ELLIPSE_NONE" );

            rframe   = new ReferenceFrame( "J2000" );
            raydir   = new Vector3( 1.0, 0.0, 0.0 );

            observer = new Body( "sun" );

            abcorr   = new AberrationCorrection( "NONE" );

            search   = new GFRayInFOVSearch( inst,    raydir,
                                             rframe,  abcorr,  observer );
            step     = 300.0;

            result   = search.run ( cnfine, step, 2 );

            System.out.println( "WNCARD(result) == " + result.card() );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(WINDOWEXCESS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(WINDOWEXCESS)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Normal search: find appearances of beta " +
                              "in FOV of ALPHA_RECTANGLE_NONE."           );

         //
         // Create the confinement window.
         //
         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
         et1     = new TDBTime( "2000 Jan 5 13:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.
         //
         inst     = new Instrument( "ALPHA_RECTANGLE_NONE" );

         //
         // The boresight vector points along the +X axis of the FOV frame
         // of this instrument.
         //
         raydir   = new Vector3( 1.0, 0.0, 0.0 );


         rframe   = new ReferenceFrame( "beta_view_xy" );

         observer = new Body( "sun" );

         abcorr   = new AberrationCorrection( "NONE" );


         //
         // Create the search instance.
         //
         search   = new GFRayInFOVSearch( inst,    raydir,
                                          rframe,  abcorr,  observer );

         //
         // Set the step size.
         //
         step = 300.0;

         //
         // Run the search. MAXIVL is the maximum number of result window
         // intervals.
         //
         result = search.run( cnfine, step, MAXIVL );

         //
         // Check the number of solution intervals.
         //
         n  = result.card();

         ok = JNITestutils.chcksi( "n", n, "=", 5, 0 );

         //
         // Check the entry and exit times.
         //
         for ( int i = 0;  i < n;  i++ )
         {
            //
            // Check start time.
            //
            interval = result.getInterval( i );

            xsec     = i * TimeConstants.SPD + 60.0;

            qname    = "Appearance start " + i;

            ok       = JNITestutils.chcksd( qname, interval[0], "~", xsec,
                                                                      TIMTOL );


            //
            // Check end time.
            //
            xsec     += ( 600.0 - 2*60.0 );

            qname    = "Appearance stop " + i;

            ok       = JNITestutils.chcksd( qname, interval[1], "~", xsec,
                                                                      TIMTOL );

         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Normal search: find appearances of beta " +
                              "in FOV of ALPHA_ELLIPSE_NONE."            );


         //
         // Create the confinement window.
         //
         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
         et1     = new TDBTime( "2000 Jan 5 13:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.
         //
         inst     = new Instrument( "ALPHA_ELLIPSE_NONE" );

         rframe   = new ReferenceFrame( "beta_view_xy" );
         raydir   = new Vector3( 1.0, 0.0, 0.0 );

         observer = new Body( "sun" );

         abcorr   = new AberrationCorrection( "NONE" );


         //
         // Create the search instance.
         //
         search   = new GFRayInFOVSearch( inst,    raydir,
                                          rframe,  abcorr,  observer );

         //
         // Set the step size.
         //
         step = 300.0;

         //
         // Run the search. MAXIVL is the maximum number of result window
         // intervals.
         //
         result = search.run( cnfine, step, MAXIVL );

         //
         // Check the number of solution intervals.
         //
         n  = result.card();

         ok = JNITestutils.chcksi( "n", n, "=", 5, 0 );

         //
         // Check the entry and exit times.
         //
         for ( int i = 0;  i < n;  i++ )
         {
            //
            // Check start time.
            //
            interval = result.getInterval( i );

            xsec     = i * TimeConstants.SPD + 60.0;

            qname    = "Appearance start " + i;

            ok       = JNITestutils.chcksd( qname, interval[0], "~", xsec,
                                                                      TIMTOL );


            //
            // Check end time.
            //
            xsec     += ( 600.0  -2*60.0 );

            qname    = "Appearance stop " + i;

            ok       = JNITestutils.chcksd( qname, interval[1], "~", xsec,
                                                                      TIMTOL );

         }
      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         ok = JNITestutils.chckth ( false, "", ex );
      }

      finally
      {
         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Clean up." );

         //
         // Get rid of the SPK file.
         //
         CSPICE.spkuef( handle );

         ( new File ( SPK ) ).delete();

         ( new File ( IK  ) ).delete();

      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

