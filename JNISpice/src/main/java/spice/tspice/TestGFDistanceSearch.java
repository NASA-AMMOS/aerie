
package spice.tspice;


import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;



/**
Class TestGFDistanceSearch provides methods that implement test families for
the class GFDistanceSearch.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 15-DEC-2009 (NJB)</h3>
*/
public class TestGFDistanceSearch extends Object
{

   //
   // Class constants
   //
   private static String  REF1          = "J2000";
   private static String  PCK           = "test.pck";
   private static String  SPK           = "gfdist.bsp";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test GFDistanceSearch and associated classes.
   */
   public static boolean f_GFDistanceSearch()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      MEDABS = 1.e-5;
      final double                      MEDREL = 1.e-10;

      final int                         MAXIVL = 100000;
      final int                         MAXWIN = 2 * MAXIVL;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              observer;
      Body                              target;

      GFConstraint                      cons;

      GFDistanceSearch                  search;

      PositionRecord                    pr;

      SpiceWindow                       cnfine;
      SpiceWindow                       result = null;

      String                            qname;
      String                            bshape;
      String                            fshape;

      TDBTime                           et0;
      TDBTime                           et1;

      boolean                           ok;

      double                            refval;
      double                            step;

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

         JNITestutils.topen ( "f_GFDistanceSearch" );


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
         // Delete the file afterward.
         //
         ( new File ( PCK ) ).delete();

         JNITestutils.tstpck( PCK, true, false );

         ( new File ( PCK ) ).delete();


         //
         // Delete SPK if it exists. Create and load a new
         // version of the file.
         //
         ( new File ( SPK ) ).delete();

         handle = JNITestutils.tstspk( SPK, true );



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************






         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Workspace window too small (detected during " +
                               "search initialization)"                      );

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
            target   = new Body( "moon"  );
            observer = new Body( "earth" );

            abcorr   = new AberrationCorrection( "LT" );

            search   = new GFDistanceSearch( target, abcorr, observer );
            step     = 30000.0;

            //
            // Set up the constraint.
            //
            refval   = 3.8e5;
            cons     = GFConstraint.createReferenceConstraint( "<", refval );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(VALUEOUTOFRANGE)", ex );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Workspace window too small (detected " +
                               "during search execution)"                    );

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
            target   = new Body( "moon"  );
            observer = new Body( "earth" );

            abcorr   = new AberrationCorrection( "LT" );

            search   = new GFDistanceSearch( target, abcorr, observer );
            step     = 30000.0;

            //
            // Set up the constraint.
            //
            refval   = 3.8e5;
            cons     = GFConstraint.createReferenceConstraint( ">", refval );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, 2 );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(WINDOWEXCESS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(WINDOWEXCESS)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Normal search: Earth-Moon distance is " +
                              "390000 km." );


         double[]                       interval;
         double                         xsec;

         //
         // Create the confinement window.
         //
         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
         et1     = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );


         //
         // Set up the search geometry parameters.
         //
         target   = new Body( "moon"  );
         observer = new Body( "earth" );

         abcorr   = new AberrationCorrection( "XCN+S" );

         search   = new GFDistanceSearch( target, abcorr, observer );
         step     = 30000.0;

         //
         // Set up the constraint.
         //
         refval   = 3.9e5;
         cons     = GFConstraint.createReferenceConstraint( "=", refval );

         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );


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
            // Check interval start and stop times for equality.
            //
            interval = result.getInterval( i );

            ok = JNITestutils.chcksd ( "interval[0]",
                                       interval[0],
                                        "~",
                                       interval[1], 0.0 );

            //
            // Check the distance at each solution time.
            //

            pr = new PositionRecord( target,
                                     new TDBTime(interval[0]),
                                     new ReferenceFrame( "J2000" ),
                                     abcorr,
                                     observer                       );

            //
            // Check relative and absolute errors.
            //
            ok = JNITestutils.chcksd ( "dist", pr.norm(), "~/", refval,
                                                                      MEDREL );
            ok = JNITestutils.chcksd ( "dist", pr.norm(), "~",  refval,
                                                                      MEDABS );

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
      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

