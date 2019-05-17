
package spice.tspice;


import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;



/**
Class TestGFOccultationSearch provides methods that implement test families for
the class GFOccultationSearch.

<h3>Version 1.0.0 29-DEC-2016 (NJB)</h3>

Changed references to GFOccultationSearch.ELLIPSOID to
references to GF.EDSHAP.

<p>Moved clean-up code to "finally" block.

<h3>Version 1.0.0 30-DEC-2009 (NJB)</h3>
*/
public class TestGFOccultationSearch extends Object
{

   //
   // Class constants
   //
   private static String  REF1          = "J2000";
   private static String  PCK           = "nat.tpc";
   private static String  SPK           = "nat.bsp";


   //
   // Class variables
   //



   //
   // Methods
   //



   /**
   Test GFOccultationSearch and associated classes.
   */
   public static boolean f_GFOccultationSearch()

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

      Body                              back;
      Body                              front;
      Body                              observer;

      GFOccultationSearch               search;

      GFSearchUtils                     gfutils;

      ReferenceFrame                    bframe;
      ReferenceFrame                    fframe;

      SpiceWindow                       cnfine;
      SpiceWindow                       result = null;

      String                            qname;
      String                            bshape;
      String                            fshape;

      TDBTime                           et0;
      TDBTime                           et1;

      boolean                           ok;

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

         JNITestutils.topen ( "f_GFOccultationSearch" );


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

         JNITestutils.natpck( PCK, true, false );

         ( new File ( PCK ) ).delete();


         //
         // Delete SPK if it exists. Create and load a new
         // version of the file.
         //
         ( new File ( SPK ) ).delete();

         handle = JNITestutils.natspk( SPK, true );



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Empty input strings."   );


         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
         et1     = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.

         front    =  new Body( "beta" );
         fframe   =  new ReferenceFrame( "betafixed" );
         fshape   =  GF.EDSHAP;

         back     =  new Body( "alpha" );
         bframe   =  new ReferenceFrame( "alphafixed" );
         bshape   =  GF.EDSHAP;

         observer = new Body( "sun" );

         abcorr   = new AberrationCorrection( "LT" );

         step     = 300.0;


         //
         // Empty occultation type string:
         //

         try
         {
            search   = new GFOccultationSearch( "",
                                                front,   fshape,   fframe,
                                                back,    bshape,   bframe,
                                                abcorr,  observer          );

            result   = search.run ( cnfine, step, MAXIVL );

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
         // Empty front shape string:
         //

         try
         {
            search   = new GFOccultationSearch( "ANY",
                                                front,   "",       fframe,
                                                back,    bshape,   bframe,
                                                abcorr,  observer          );

            result   = search.run ( cnfine, step, MAXIVL );

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
         // Empty back shape string:
         //

         try
         {
            search   = new GFOccultationSearch( "ANY",
                                                front,   fshape,   fframe,
                                                back,    "",       bframe,
                                                abcorr,  observer          );

            result   = search.run ( cnfine, step, MAXIVL );

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
         JNITestutils.tcase (  "Result window too small (detected during " +
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
            front    =  new Body( "moon" );
            fframe   =  new ReferenceFrame( "iau_moon" );
            fshape   =  GF.EDSHAP;

            back     =  new Body( "sun" );
            bframe   =  new ReferenceFrame( "iau_sun" );
            bshape   =  GF.EDSHAP;

            observer = new Body( "earth" );

            abcorr   = new AberrationCorrection( "LT" );

            search   = new GFOccultationSearch( GFOccultationSearch.ANY,
                                                front,   fshape,   fframe,
                                                back,    bshape,   bframe,
                                                abcorr,  observer          );
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

            front    =  new Body( "beta" );
            fframe   =  new ReferenceFrame( "betafixed" );
            fshape   =  GF.EDSHAP;

            back     =  new Body( "alpha" );
            bframe   =  new ReferenceFrame( "alphafixed" );
            bshape   =  GF.EDSHAP;

            observer = new Body( "sun" );

            abcorr   = new AberrationCorrection( "LT" );


            search   = new GFOccultationSearch( GFOccultationSearch.ANY,
                                                front,   fshape,   fframe,
                                                back,    bshape,   bframe,
                                                abcorr,  observer          );
            step     = 300.0;

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




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Normal search: find transits of beta " +
                              "across alpha" );


         double[]                       interval;
         double                         xsec;

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
         front    =  new Body( "beta" );
         fframe   =  new ReferenceFrame( "betafixed" );
         fshape   =  GF.EDSHAP;

         back     =  new Body( "alpha" );
         bframe   =  new ReferenceFrame( "alphafixed" );
         bshape   =  GF.EDSHAP;

         observer = new Body( "sun" );

         abcorr   = new AberrationCorrection( "LT" );

         //
         // Create the search instance.
         //
         search   = new GFOccultationSearch( GFOccultationSearch.ANY,
                                             front,   fshape,   fframe,
                                             back,    bshape,   bframe,
                                             abcorr,  observer          );
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
            // Check occultation start time.
            //
            interval = result.getInterval( i );

            xsec     = i * TimeConstants.SPD + 1.0;

            qname    = "Transit start " + i;

            ok       = JNITestutils.chcksd( qname, interval[0], "~", xsec,
                                                                      TIMTOL );


            //
            // Check occultation end time.
            //
            xsec     += 600.0;

            qname    = "Transit stop " + i;

            ok       = JNITestutils.chcksd( qname, interval[1], "~", xsec,
                                                                      TIMTOL );

         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Run the previous search using the custom " +
                              "search utilities." );

         gfutils = new GFSearchUtils();

         gfutils.setSearchStep( 60.0 );

         result = search.run( cnfine, gfutils, MAXWIN );

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
            // Check occultation start time.
            //
            interval = result.getInterval( i );

            xsec     = i * TimeConstants.SPD + 1.0;

            qname    = "Transit start " + i;

            ok       = JNITestutils.chcksd( qname, interval[0], "~", xsec,
                                                                      TIMTOL );


            //
            // Check occultation end time.
            //
            xsec     += 600.0;

            qname    = "Transit stop " + i;

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

      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

