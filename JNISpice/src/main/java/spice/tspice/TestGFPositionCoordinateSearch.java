
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.*;
import static spice.basic.TimeConstants.*;


/**
Class TestGFPositionCoordinateSearch provides methods that implement
test families for the class GFPositionCoordinateSearch.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 15-DEC-2009 (NJB)</h3>
*/
public class TestGFPositionCoordinateSearch extends Object
{

   //
   // Class constants
   //
   private static String  REF1          = "J2000";
   private static String  NATPCK        = "nat.tpc";
   private static String  NATSPK        = "nat.bsp";
   private static String  PCK           = "gfposc.tpc";
   private static String  SPK           = "gfposc.bsp";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test GFPositionCoordinateSearch and associated classes.
   */
   public static boolean f_GFPositionCoordinateSearch()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 2.e-6;
      final double                      MED_TOL   = 2.e-6;


      final int                         MAXIVL = 100000;
      final int                         MAXWIN = 2 * MAXIVL;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              observer;
      Body                              target;

      GFConstraint                      cons;

      GFPositionCoordinateSearch        search;

      PositionRecord                    pr;

      ReferenceFrame                    ref;

      SpiceWindow                       cnfine;
      SpiceWindow                       result = null;

      String                            bshape;
      String                            coordStr;
      String                            crdsysStr;
      String                            fshape;
      String                            qname;
      String                            relateStr;
      String                            title;

      TDBTime                           et0;
      TDBTime                           et1;

      boolean                           ok;

      double                            adjust;
      double                            refval;
      double                            step;
      double[]                          interval;
      double[]                          prevIval;
      double                            xsec;

      int                               handle = 0;
      int                               i;
      int                               j;
      int                               k;
      int                               n;
      int                               nathan = 0;
      int                               ntest;


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

         JNITestutils.topen ( "f_GFPositionCoordinateSearch" );


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
         //
         ( new File ( PCK ) ).delete();

         JNITestutils.tstpck( PCK, true, false );

         //
         // Same for Nat's solar system PCK.
         //
         ( new File ( NATPCK ) ).delete();

         JNITestutils.natpck( PCK, true, false );


         //
         // Delete SPK if it exists. Create and load a new
         // version of the file.
         //
         ( new File ( SPK ) ).delete();

         handle = JNITestutils.tstspk( SPK, true );


         //
         // Same for Nat's solar system SPK.
         //
         ( new File ( NATSPK ) ).delete();

         nathan = JNITestutils.natspk( NATSPK, true );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: empty coordinate system name" );

         try
         {
            cnfine   = new SpiceWindow();

            et0      = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
            et1      = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

            cnfine   = cnfine.insert( et0.getTDBSeconds(),
                                      et1.getTDBSeconds() );

            //
            // Set up the search geometry parameters.
            //
            target    = new Body( "moon"  );
            observer  = new Body( "earth" );

            abcorr    = new AberrationCorrection( "LT" );

            crdsysStr = "";
            coordStr  = "RADIUS";
            ref       = new ReferenceFrame( "J2000" );

            search    = new GFPositionCoordinateSearch(
                                               target,   ref,       abcorr,
                                               observer, crdsysStr, coordStr );

            cons      = GFConstraint.createReferenceConstraint( ">", 0.0 );

            step      = 600.0;

            result    = search.run( cnfine, cons, step, MAXIVL );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: blank coordinate system name" );

         try
         {
            cnfine   = new SpiceWindow();

            et0      = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
            et1      = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

            cnfine   = cnfine.insert( et0.getTDBSeconds(),
                                      et1.getTDBSeconds() );

            //
            // Set up the search geometry parameters.
            //
            target    = new Body( "moon"  );
            observer  = new Body( "earth" );

            abcorr    = new AberrationCorrection( "LT" );

            crdsysStr = " ";
            coordStr  = "RADIUS";
            ref       = new ReferenceFrame( "J2000" );

            search    = new GFPositionCoordinateSearch(
                                               target,   ref,       abcorr,
                                               observer, crdsysStr, coordStr );

            cons      = GFConstraint.createReferenceConstraint( ">", 0.0 );

            step      = 600.0;

            result    = search.run( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(NOTSUPPORTED)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(NOTSUPPORTED)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: empty coordinate name" );

         try
         {
            cnfine   = new SpiceWindow();

            et0      = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
            et1      = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

            cnfine   = cnfine.insert( et0.getTDBSeconds(),
                                      et1.getTDBSeconds() );

            //
            // Set up the search geometry parameters.
            //
            target    = new Body( "moon"  );
            observer  = new Body( "earth" );

            abcorr    = new AberrationCorrection( "LT" );

            crdsysStr = "LATITUDINAL";
            coordStr  = "";
            ref       = new ReferenceFrame( "J2000" );

            search    = new GFPositionCoordinateSearch(
                                               target,   ref,       abcorr,
                                               observer, crdsysStr, coordStr );

            cons      = GFConstraint.createReferenceConstraint( ">", 0.0 );

            step      = 600.0;

            result    = search.run( cnfine, cons, step, MAXIVL );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: blank coordinate system name" );

         try
         {
            cnfine   = new SpiceWindow();

            et0      = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
            et1      = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

            cnfine   = cnfine.insert( et0.getTDBSeconds(),
                                      et1.getTDBSeconds() );

            //
            // Set up the search geometry parameters.
            //
            target    = new Body( "moon"  );
            observer  = new Body( "earth" );

            abcorr    = new AberrationCorrection( "LT" );

            crdsysStr = "RECTANGULAR";
            coordStr  = " ";
            ref       = new ReferenceFrame( "J2000" );

            search    = new GFPositionCoordinateSearch(
                                               target,   ref,       abcorr,
                                               observer, crdsysStr, coordStr );

            cons      = GFConstraint.createReferenceConstraint( ">", 0.0 );

            step      = 600.0;

            result    = search.run( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(NOTSUPPORTED)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(NOTSUPPORTED)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: non-positive step size." );

         try
         {
            cnfine   = new SpiceWindow();

            et0      = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
            et1      = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

            cnfine   = cnfine.insert( et0.getTDBSeconds(),
                                      et1.getTDBSeconds() );

            //
            // Set up the search geometry parameters.
            //
            target    = new Body( "moon"  );
            observer  = new Body( "earth" );

            abcorr    = new AberrationCorrection( "LT" );

            crdsysStr = "RECTANGULAR";
            coordStr  = "x";
            ref       = new ReferenceFrame( "J2000" );

            search    = new GFPositionCoordinateSearch(
                                               target,   ref,       abcorr,
                                               observer, crdsysStr, coordStr );

            cons      = GFConstraint.createReferenceConstraint( ">", 0.0 );

            step      = 0.0;

            result    = search.run( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(INVALIDSTEP)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(INVALIDSTEP)", ex );
         }





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Workspace window too small (detected during " +
                               "search initialization)"                      );

         try
         {
            cnfine   = new SpiceWindow();

            et0      = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
            et1      = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

            cnfine   = cnfine.insert( et0.getTDBSeconds(),
                                      et1.getTDBSeconds() );

            //
            // Set up the search geometry parameters.
            //
            target    = new Body( "moon"  );
            observer  = new Body( "earth" );

            abcorr    = new AberrationCorrection( "LT" );

            crdsysStr = "LATITUDINAL";
            coordStr  = "RADIUS";
            ref       = new ReferenceFrame( "J2000" );

            search    = new GFPositionCoordinateSearch(
                                               target,   ref,       abcorr,
                                               observer, crdsysStr, coordStr );
            step      = 30000.0;

            //
            // Set up the constraint.
            //
            refval    = 3.8e5;
            cons      = GFConstraint.createReferenceConstraint( "<", refval );

            //
            // Run the search.
            //
            result    = search.run ( cnfine, cons, step, 0 );

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
         JNITestutils.tcase (  "Workspace window too small (detected during " +
                               "search execution)"                           );

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

            crdsysStr = "LATITUDINAL";
            coordStr  = "RADIUS";
            ref       = new ReferenceFrame( "J2000" );

            search   = new GFPositionCoordinateSearch(
                                               target,   ref,       abcorr,
                                               observer, crdsysStr, coordStr );
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
         // Event test block.
         //
         // Define the coordinate test conditions.
         //
         // Conditions: all conditions should occur once per
         // GAMMA orbit (delta_t = 24 hours).
         //
         //
         //
         // Assign the test conditions in the MDESC and MREFS arrays.
         //
         String[] MDESC = { "RECTANGULAR   : X               : >",
                            "RECTANGULAR   : Y               : <",
                            "LATITUDINAL   : LONGITUDE       : <",
                            "RA/DEC        : RIGHT ASCENSION : >",
                            "SPHERICAL     : LONGITUDE       : <",
                            "CYLINDRICAL   : LONGITUDE       : >",
                            "LATITUDINAL   : LONGITUDE       : =",
                            "SPHERICAL     : LONGITUDE       : >",
                            "RECTANGULAR   : X               : LOCMAX",
                            "RECTANGULAR   : Y               : LOCMAX",
                            "RECTANGULAR   : X               : LOCMIN",
                            "RECTANGULAR   : Y               : LOCMIN",
                            "SPHERICAL     : LONGITUDE       : =",
                            "RA/DEC        : RIGHT ASCENSION : =",
                            "SPHERICAL     : LONGITUDE       : =" };

         //
         // Test conditions reference values.
         //
         double[] MREFS = {    0.0,
                               0.0,
                             -90.0*RPD,
                             270.0*RPD,
                             -90.0*RPD,
                             270.0*RPD,
                             179.0*RPD,
                             179.0*RPD,
                               0.0,
                               0.0,
                               0.0,
                               0.0,
                              90.0*RPD,
                             359.0*RPD,
                             270.0*RPD  };

         //
         // Aberration corrections as strings.
         //
         String[] abcorrStrings = { "NONE",
                                    "lt",
                                    " lt+s",
                                    " cn",
                                    " cn + s",
                                    "XLT",
                                    "XLT + S",
                                    "XCN",
                                    "XCN+S"
                                              };

         int NCORR = abcorrStrings.length;



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Alpha-gamma searches using various " +
                              "relational constraints."                      );


         ntest = MDESC.length;

         for ( i = 0;  i < ntest;  i++ )
         {

            JNITestutils.tcase( "loop index = " + i );
            //
            // Create the confinement window.
            //
            cnfine  = new SpiceWindow();

            et0     = new TDBTime( "2000 Jan 01 03:00:00 TDB" );
            et1     = new TDBTime( "2000 Jan 06 03:00:00 TDB" );

            cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                     et1.getTDBSeconds() );


            //
            // Set up the search geometry parameters.
            //
            target   = new Body( "Gamma"  );
            observer = new Body( "Alpha" );
            ref      = new ReferenceFrame( "Alphafixed" );
            abcorr   = new AberrationCorrection( "None" );
            step     = 5 * 3600.0;

            //
            // Parse the coordinate system, coordinate, and relational
            // operator.
            //
            StringTokenizer tokenizer = new StringTokenizer ( MDESC[i], ":" );

            crdsysStr = tokenizer.nextToken();
            coordStr  = tokenizer.nextToken();
            relateStr = tokenizer.nextToken();

            //
            // Specify the search.
            //
            search   = new GFPositionCoordinateSearch(
                                               target,   ref,       abcorr,
                                               observer, crdsysStr, coordStr );

            //
            // Set up the constraint.
            //
            //
            // This assignment works for relational operators other
            // than extrema.
            //

            if (      ( relateStr.trim().equals( ">" ) )
                  ||  ( relateStr.trim().equals( "=" ) )
                  ||  ( relateStr.trim().equals( "<" ) )  )
            {
               cons = GFConstraint.createReferenceConstraint( relateStr,
                                                                    MREFS[i] );
            }

            else if (      (  relateStr.trim().equals( "LOCMAX" )  )
                       ||  (  relateStr.trim().equals( "LOCMIN" )  )
                       ||  (  relateStr.trim().equals( "ABSMAX" )  )
                       ||  (  relateStr.trim().equals( "ABSMIN" )  )  )
            {
               cons = GFConstraint.createExtremumConstraint( relateStr );
            }
            else
            {
               adjust = 0.0;
               cons   = GFConstraint.createExtremumConstraint( relateStr,
                                                                      adjust );
            }

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXIVL );


            // System.out.println( "result window for i = " + i + " = "
            //                                                      + result );

            //
            // Fill in any millisecond-length gaps in the solution window;
            // these are spurious.
            //
            // result.fill( 1.e-3 );

            //
            // Check the number of solution intervals.
            //
            n  = result.card();

            ok = JNITestutils.chcksi( "n", n, "=", 5, 0 );


            //
            // Check the entry and exit times.
            //
            prevIval = result.getInterval( 0 );


            for ( j = 1;  j < n;  j++ )
            {
               //
               // Confirm the time separating the start times for subseqent
               // intervals and the end times for subsequent intervals has
               // value one day in seconds.
               //
               interval = result.getInterval( j );

               ok = JNITestutils.chcksd ( "SWEEP BEG",
                                          interval[0] - prevIval[0],
                                           "~",
                                          SPD, TIGHT_TOL );

               ok = JNITestutils.chcksd ( "SWEEP END",
                                          interval[1] - prevIval[1],
                                           "~",
                                          SPD, TIGHT_TOL );

               prevIval[0] = interval[0];
               prevIval[1] = interval[1];
            }

         }



         //
         // --------Case-----------------------------------------------
         //

         //
         // Test the aberration correction values
         // in a search. Increase the error tolerance
         // to MEDTOL to account for the light-time
         // calculation artifacts.
         //

         et0 = new TDBTime( "2000 Jan 01 00:00:00 TDB" );
         et1 = new TDBTime( "2000 Jan 06 00:00:00 TDB" );

         //
         // Use the last constraint in the MDESC array.
         //

         i   = ntest-1;

         //
         // Loop over the aberration correction set.
         //
         for ( k = 1; k < NCORR; k++ )
         {
            abcorr = new AberrationCorrection( abcorrStrings[k] );


            title = MDESC[i] + " " + abcorr;

            //
            // --------Case-----------------------------------------------
            //
            JNITestutils.tcase ( title );



            cnfine = new SpiceWindow();

            cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );

            //
            // Parse the coordinate system, coordinate, and relational
            // operator.
            //
            StringTokenizer tokenizer = new StringTokenizer ( MDESC[i], ":" );

            crdsysStr = tokenizer.nextToken();
            coordStr  = tokenizer.nextToken();
            relateStr = tokenizer.nextToken();

            //
            // Specify the search geometry.
            //
            //
            // Set up the search geometry parameters.
            //
            target   = new Body( "Gamma"  );
            observer = new Body( "Alpha" );
            ref      = new ReferenceFrame( "Alphafixed" );
            abcorr   = new AberrationCorrection( "None" );

            search = new GFPositionCoordinateSearch(
                                               target,   ref,       abcorr,
                                               observer, crdsysStr, coordStr );

            //
            // Specify the constraint.
            //
            cons   = GFConstraint.createReferenceConstraint( relateStr,
                                                                    MREFS[i] );

            //
            // Use a 5 hour step.
            //
            step   = 5 * 3600.0;

            //
            // Run the search.
            //

            result = search.run( cnfine, cons, step, MAXIVL );

            //
            // Check the result window.
            //

            // System.out.println( "Iteration: " + k + "Result window: " +
            //                                                        result );

            //
            // Check the number of solution intervals.
            //
            n  = result.card();

            ok = JNITestutils.chcksi( "n", n, "=", 5, 0 );


            //
            // Check the entry and exit times.
            //
            prevIval = result.getInterval( 0 );


            for ( j = 1;  j < n;  j++ )
            {
               //
               // Confirm the time separating the start times for subseqent
               // intervals and the end times for subsequent intervals has
               // value one day in seconds.
               //
               interval = result.getInterval( j );

               ok = JNITestutils.chcksd ( "SWEEP BEG",
                                          interval[0] - prevIval[0],
                                           "~",
                                          SPD, MED_TOL );

               ok = JNITestutils.chcksd ( "SWEEP END",
                                          interval[1] - prevIval[1],
                                           "~",
                                          SPD, MED_TOL );

               prevIval[0] = interval[0];
               prevIval[1] = interval[1];
            }

         }



      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         ex.printStackTrace();

         ok = JNITestutils.chckth ( false, "", ex );
      }

      finally
      {

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Clean up." );

         //
         // Get rid of the SPK files.
         //
         CSPICE.spkuef( handle );

         ( new File ( SPK ) ).delete();

         CSPICE.spkuef( nathan );

         ( new File ( NATSPK ) ).delete();


         //
         // Get rid of the PCK files.
         //
         ( new File ( PCK    ) ).delete();
         ( new File ( NATPCK ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

