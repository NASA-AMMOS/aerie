
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.GFAngularSeparationSearch.*;
import static spice.basic.AngularUnits.*;
import static spice.basic.TimeConstants.*;


/**
Class TestGFAngularSeparationSearch provides methods that implement
test families for the class GFAngularSeparationSearch.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 21-DEC-2009 (NJB) (EDW)</h3>
*/
public class TestGFAngularSeparationSearch extends Object
{

   //
   // Class constants
   //
   private static String  REF1          = "J2000";
   private static String  PCK           = "test.pck";
   private static String  SPK           = "gfsubc.bsp";
   private static String  NATPCK        = "nat.tpc";
   private static String  NATSPK        = "nat.bsp";

   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test GFAngularSeparationSearch and associated classes.
   */
   public static boolean f_GFAngularSeparationSearch()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL  = 2.e-6;
      final double                      VTIGHT_TOL = 2.e-9;
      final double                      MED_TOL   = 2.e-5;
      final double                      MEDABS = 1.e-5;
      final double                      MEDREL = 1.e-10;


      final int                         MAXIVL = 100000;
      final int                         MAXWIN = 2 * MAXIVL;
      final int                         NCORR  = 9;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              observer;
      Body                              targ1;
      Body                              targ2;

      GFConstraint                      cons;

      GFAngularSeparationSearch         search;

      ReferenceFrame                    frame1;
      ReferenceFrame                    frame2;

      SpiceWindow                       cnfine;
      SpiceWindow                       result = null;

      String[]                          CORR   = {
                                           "NONE",
                                           "lt",
                                           " lt+s",
                                           " cn",
                                           " cn + s",
                                           "XLT",
                                           "XLT + S",
                                           "XCN",
                                           "XCN+S"
                                        };

      String                            qname;
      String                            relateStr;
      String                            shape1;
      String                            shape2;
      String                            title;

      TDBTime                           et0;
      TDBTime                           et1;
      TDBTime                           xEt;

      boolean                           ok;

      double                            adjust;
      double                            angrad1;
      double                            angrad2;
      double[]                          interval;
      double[]                          prevIval;
      double                            r1;
      double                            r2;
      double[]                          radii;
      double                            refval;
      double                            sep;
      double                            step;

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

         JNITestutils.topen ( "f_GFAngularSeparationSearch" );


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
         // Delete Nat's solar system SPK if it exists. Create and load a new
         // version of the file.
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
         JNITestutils.tcase ( "Non-positive step size."  );

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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "Sun"   );
            shape1           = SPHERE;
            shape2           = SPHERE;
            frame1           = new ReferenceFrame( "IAU_MOON" );
            frame2           = new ReferenceFrame( "IAU_SUN"  );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );
            step     = 0.0;

            //
            // Set up the constraint.
            //
            adjust   = 10.0 * RPD;
            cons     = GFConstraint.createExtremumConstraint(

                          GFConstraint.ADJUSTED_ABSMAX, adjust );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXIVL );

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
         JNITestutils.tcase ( "Target coincides with observer."  );

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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "MOON"   );
            shape1           = SPHERE;
            shape2           = SPHERE;
            frame1           = new ReferenceFrame( "IAU_MOON" );
            frame2           = new ReferenceFrame( "IAU_MOON"  );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );

            step     = 30000.0;

            //
            // Set up the constraint.
            //
            adjust   = 10.0 * RPD;

            cons     = GFConstraint.createExtremumConstraint(

                          GFConstraint.ADJUSTED_ABSMAX, adjust );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(BODIESNOTDISTINCT)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(BODIESNOTDISTINCT)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Negative adjustment value."  );

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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "SUN"   );
            shape1           = SPHERE;
            shape2           = SPHERE;
            frame1           = new ReferenceFrame( "IAU_MOON" );
            frame2           = new ReferenceFrame( "IAU_SUN"  );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );

            step     = 30000.0;

            //
            // Set up the constraint.
            //
            adjust   = -10.0;
            cons     = GFConstraint.createExtremumConstraint(

                          GFConstraint.ADJUSTED_ABSMAX, adjust );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXIVL );

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
         JNITestutils.tcase ( "Invalid relational operator."  );

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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "sun"   );
            shape1           = SPHERE;
            shape2           = SPHERE;
            frame1           = new ReferenceFrame( "IAU_MOON" );
            frame2           = new ReferenceFrame( "IAU_sun"  );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );

            step     = 30000.0;

            //
            // Set up the constraint.
            //
            refval   = 30.0 * RPD;
            cons     = GFConstraint.createReferenceConstraint( "==", refval );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(NOTAPPLICABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(NOTAPPLICABLE)", ex );
         }




         //
         // Add the bad frame tests when SPICELIB is updated to support
         // these checks.
         //


         /*


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Invalid reference frame."  );

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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "sun"   );
            shape1           = SPHERE;
            shape2           = SPHERE;
            frame1           = new ReferenceFrame( "IAU_MOON"  );
            frame2           = new ReferenceFrame( "XXX" );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );
            step     = 30000.0;

            //
            // Set up the constraint.
            //
            refval   = 30.0 * RPD;
            cons     = GFConstraint.createReferenceConstraint( "<", refval );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(INVALIDFRAME)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(INVALIDFRAME)", ex );
         }


         */




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Invalid body shape"  );

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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "sun"   );
            shape1           = "CUBE";
            shape2           = "point";
            frame1           = new ReferenceFrame( "IAU_MOON" );
            frame2           = new ReferenceFrame( "IAU_sun"  );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );

            step     = 30000.0;

            //
            // Set up the constraint.
            //
            refval   = 30.0 * RPD;
            cons     = GFConstraint.createReferenceConstraint( "<", refval );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(NOTRECOGNIZED)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(NOTRECOGNIZED)", ex );
         }


         //
         // Now give a bad shape for the second target.
         //

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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "sun"   );
            shape1           = "point";
            shape2           = "square";
            frame1           = new ReferenceFrame( "IAU_MOON" );
            frame2           = new ReferenceFrame( "IAU_sun"  );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );

            step     = 30000.0;

            //
            // Set up the constraint.
            //
            refval   = 30.0 * RPD;
            cons     = GFConstraint.createReferenceConstraint( "<", refval );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(NOTRECOGNIZED)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(NOTRECOGNIZED)", ex );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Ephemeris data unavailable."  );

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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "dawn"   );
            shape1           = "sphere";
            shape2           = "point";
            frame1           = new ReferenceFrame( "IAU_MOON" );
            frame2           = new ReferenceFrame( "N/A"  );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );

            step     = 30000.0;

            //
            // Set up the constraint.
            //
            adjust   = 10.0;
            cons     = GFConstraint.createExtremumConstraint(

                          GFConstraint.ADJUSTED_ABSMAX, adjust );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(SPKINSUFFDATA)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(SPKINSUFFDATA)", ex );
         }


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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "sun"   );
            shape1           = "sphere";
            shape2           = "sphere";
            frame1           = new ReferenceFrame( "IAU_MOON" );
            frame2           = new ReferenceFrame( "IAU_sun"  );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );

            step     = 30000.0;

            //
            // Set up the constraint.
            //
            refval   = 30.0 * RPD;
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
            targ1            = new Body( "moon"  );
            targ2            = new Body( "sun"   );
            shape1           = "sphere";
            shape2           = "sphere";
            frame1           = new ReferenceFrame( "IAU_MOON" );
            frame2           = new ReferenceFrame( "IAU_sun"  );
            abcorr           = new AberrationCorrection( "LT" );
            observer         = new Body( "earth" );

            search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                      targ2,  shape2,  frame2,
                                                      abcorr, observer       );

            step     = 30000.0;

            //
            // Set up the constraint.
            //
            refval   = 30.0 * AngularUnits.RPD;
            cons     = GFConstraint.createReferenceConstraint( "<", refval );

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
         JNITestutils.tcase( "Absolute maximum test." );

         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 23:00:00 TDB" );
         et1     = new TDBTime( "2000 Jan 2 01:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.
         //
         targ1            = new Body( "alpha" );
         targ2            = new Body( "beta"  );
         shape1           = "point";
         shape2           = "point";
         frame1           = new ReferenceFrame( "ALPHAFIXED" );
         frame2           = new ReferenceFrame( "BETAFIXED"  );
         abcorr           = new AberrationCorrection( "NONE" );
         observer         = new Body( "SUN" );

         search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                   targ2,  shape2,  frame2,
                                                   abcorr, observer         );
         //
         // Use a 5-hour step.
         //
         step     = 5 * 3600.0;

         //
         // Set up the constraint.
         //
         cons     = GFConstraint.createExtremumConstraint(
                                               GFConstraint.ABSOLUTE_MAXIMUM );

         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );

         //
         // There should be a single, singleton interval in the result window.
         //

         ok = JNITestutils.chcksi( "interval count", result.card(), "=",
                                                                        1, 0 );

         //
         // Check the event time.
         //
         interval = result.getInterval( 0 );

         //
         // The max angular separation should occur 12 hours apart
         // from the midpoint of the occultation, which is 5 minutes
         // past noon.
         //
         xEt = new TDBTime( "2000 jan 2 00:05:00 TDB" );


         ok = JNITestutils.chcksd ( "event start",
                                    interval[0],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         ok = JNITestutils.chcksd ( "event stop",
                                    interval[1],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Local maximum test. Use geometry " +
                             "from previous case."                );

         //
         // Set up the constraint.
         //
         cons     = GFConstraint.createExtremumConstraint(
                                                  GFConstraint.LOCAL_MAXIMUM );

         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );

         //
         // There should be a single, singleton interval in the result window.
         //

         ok = JNITestutils.chcksi( "interval count", result.card(), "=",
                                                                        1, 0 );

         //
         // Check the event time.
         //
         interval = result.getInterval( 0 );

         //
         // The max angular separation should occur 12 hours apart
         // from the midpoint of the occultation, which is 5 minutes
         // past noon.
         //
         xEt = new TDBTime( "2000 jan 2 00:05:00 TDB" );


         ok = JNITestutils.chcksd ( "event start",
                                    interval[0],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         ok = JNITestutils.chcksd ( "event stop",
                                    interval[1],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Absolute minimum test." );

         //
         // Re-run the previous test with a new constraint and a new
         // confinement window.
         //

         //
         // Set up the constraint.
         //
         cons     = GFConstraint.createExtremumConstraint(
                                               GFConstraint.ABSOLUTE_MINIMUM );

         cnfine   = new SpiceWindow();

         et0      = new TDBTime( "2000 Jan 1 12:00 TDB" );
         et1      = new TDBTime( "2000 Jan 1 13:00 TDB" );

         cnfine   = cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );

         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );

         //
         // There should be a single, singleton interval in the result window.
         //

         ok = JNITestutils.chcksi( "interval count", result.card(), "=",
                                                                        1, 0 );

         //
         // Check the event time. The min should occur at the occultation
         // midpoint.
         //
         interval = result.getInterval( 0 );

         xEt = new TDBTime( "2000 jan 1 12:05:00 TDB" );


         ok = JNITestutils.chcksd ( "event start",
                                    interval[0],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         ok = JNITestutils.chcksd ( "event stop",
                                    interval[1],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Local minimum test. Use geometry " +
                             "from previous case."                );
         //
         // Set up the constraint.
         //
         cons     = GFConstraint.createExtremumConstraint(
                                               GFConstraint.ABSOLUTE_MINIMUM );

         cnfine   = new SpiceWindow();

         et0      = new TDBTime( "2000 Jan 1 12:00 TDB" );
         et1      = new TDBTime( "2000 Jan 1 13:00 TDB" );

         cnfine   = cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );

         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );

         //
         // There should be a single, singleton interval in the result window.
         //

         ok = JNITestutils.chcksi( "interval count", result.card(), "=",
                                                                        1, 0 );

         //
         // Check the event time. The min should occur at the occultation
         // midpoint.
         //
         interval = result.getInterval( 0 );

         xEt = new TDBTime( "2000 jan 1 12:05:00 TDB" );


         ok = JNITestutils.chcksd ( "event start",
                                    interval[0],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         ok = JNITestutils.chcksd ( "event stop",
                                    interval[1],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Adjusted absolute maximum test." );

         //
         // Re-run the previous test with a new constraint and a new
         // confinement window.
         //

         //
         // Set up the constraint.
         //

         //
         // The angular separation of the Sun-Alpha and Sun-Beta vectors
         // has a 24-hr period, so it increases at 15 degrees/hour.
         //


         adjust   = 1.0 * RPD;

         cons     = GFConstraint.createExtremumConstraint(

                      GFConstraint.ADJUSTED_ABSMAX, adjust );

         cnfine   = new SpiceWindow();

         et0      = new TDBTime( "2000 Jan 1 23:00 TDB" );
         et1      = new TDBTime( "2000 Jan 2 01:00 TDB" );

         cnfine   = cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );


         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );

         //
         // There should be a single, singleton interval in the result window.
         //

         ok = JNITestutils.chcksi( "interval count", result.card(), "=",
                                                                        1, 0 );


         //
         // Check the event time. The absolute maximum is expected
         // at 5 minutes past midnight. The bounds of the result window
         // should be at this time +/- 4 minutes.
         //
         interval = result.getInterval( 0 );

         xEt = new TDBTime( "2000 jan 2 00:01 TDB" );


         ok = JNITestutils.chcksd ( "event start",
                                    interval[0],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         xEt = new TDBTime( "2000 jan 2 00:09 TDB" );

         ok = JNITestutils.chcksd ( "event stop",
                                    interval[1],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Adjusted absolute minimum test." );

         //
         // Re-run the previous test with a new constraint and a new
         // confinement window.
         //

         //
         // Set up the constraint.
         //

         //
         // See the previous case for an explanation.
         //
         adjust   = 1.0 * RPD;

         cons     = GFConstraint.createExtremumConstraint(

                      GFConstraint.ADJUSTED_ABSMIN, adjust );

         cnfine   = new SpiceWindow();

         et0      = new TDBTime( "2000 Jan 1 11:00 TDB" );
         et1      = new TDBTime( "2000 Jan 1 13:00 TDB" );

         cnfine   = cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );


         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );

         //
         // There should be a single, singleton interval in the result window.
         //

         ok = JNITestutils.chcksi( "interval count", result.card(), "=",
                                                                        1, 0 );


         //
         // Check the event time.
         //
         interval = result.getInterval( 0 );

         xEt = new TDBTime( "2000 jan 1 12:01 TDB" );


         ok = JNITestutils.chcksd ( "event start",
                                    interval[0],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         xEt = new TDBTime( "2000 jan 1 12:09 TDB" );

         ok = JNITestutils.chcksd ( "event stop",
                                    interval[1],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test local angular separation utility: " +
                              "normal case." );

         et0    = new TDBTime( "2000 Jan 1 12:05:00 TDB" );
         abcorr = new AberrationCorrection( "NONE" );

         sep = getTargetSep( targ1, targ2, 0.0, 0.0, et0, abcorr, observer );

         ok = JNITestutils.chcksd ( "sep",
                                    sep,
                                    "~",
                                    0.0,
                                    TIGHT_TOL           );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test local angular separation utility: " +
                              "observer is inside a target."              );


         et0    = new TDBTime( "2000 Jan 1 12:05:00 TDB" );
         abcorr = new AberrationCorrection( "NONE" );

         try
         {
            sep = getTargetSep( targ1, targ2, 0.0, 0.0, et0, abcorr, targ1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(INVALIDGEOMETRY)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //exc.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(INVALIDGEOMETRY)", ex );
         }

         try
         {
            sep = getTargetSep( targ1, targ2, 0.0, 0.0, et0, abcorr, targ2 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(INVALIDGEOMETRY)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //exc.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(INVALIDGEOMETRY)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Equality test: spherical targets." );

         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 11:00:00 TDB" );
         et1     = new TDBTime( "2000 Jan 1 13:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.
         //
         targ1            = new Body( "alpha" );
         targ2            = new Body( "beta"  );
         shape1           = "sphere";
         shape2           = "sphere";
         frame1           = new ReferenceFrame( "ALPHAFIXED" );
         frame2           = new ReferenceFrame( "BETAFIXED"  );
         abcorr           = new AberrationCorrection( "NONE" );
         observer         = new Body( "SUN" );

         search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                   targ2,  shape2,  frame2,
                                                   abcorr, observer         );
         //
         // Use a 5-hour step.
         //
         step     = 5 * 3600.0;

         //
         // Set up the constraint.
         //
         refval   = 10.0 * RPD;
         cons     = GFConstraint.createReferenceConstraint( "=", refval );

         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );

         //
         // There should be two singleton intervals in the result window.
         //

         ok = JNITestutils.chcksi( "interval count", result.card(), "=",
                                   2, 0 );

         //
         // Check the angular separation of the targets at each
         // interval's endpoints.
         //


         //
         // Get the maximum radius of each target.
         //
         radii = targ1.getValues("RADII");
         Arrays.sort( radii );
         r1    = radii[2];

         radii = targ2.getValues("RADII");
         Arrays.sort( radii );
         r2    = radii[2];


         for( i = 0;  i < 2;  i++ )
         {
            interval = result.getInterval( i );

            //
            // Make sure the interval is a singleton.
            //
            ok = JNITestutils.chcksd ( "interval start",
                                       interval[0],
                                       "=",
                                       interval[1],
                                       0.0              );
            //
            // Check the angular separation of the targets,
            // where the target shapes are modeled as spheres
            // of radii r1 and r2 respectively.
            //
            et0 = new TDBTime( interval[0] );

            sep = getTargetSep( targ1, targ2, r1, r2, et0, abcorr, observer );

            //
            // Regarding the tolerance for sep: the time tolerance is 1.e-6
            // seconds, and the relative angular rate of the targets is
            // 360 degrees/day, or about 7.27e-5 rad/sec, so we can use a
            // nanosecond tolerance here.
            //
            ok = JNITestutils.chcksd ( "sep for interval " + i,
                                       sep,
                                       "~",
                                       refval,
                                       VTIGHT_TOL                           );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "< Inequality test: spherical targets." );

         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 11:00:00 TDB" );
         et1     = new TDBTime( "2000 Jan 1 13:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.
         //
         targ1            = new Body( "alpha" );
         targ2            = new Body( "beta"  );
         shape1           = "sphere";
         shape2           = "sphere";
         frame1           = new ReferenceFrame( "ALPHAFIXED" );
         frame2           = new ReferenceFrame( "BETAFIXED"  );
         abcorr           = new AberrationCorrection( "NONE" );
         observer         = new Body( "SUN" );

         search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                   targ2,  shape2,  frame2,
                                                   abcorr, observer         );
         //
         // Use a 5-hour step.
         //
         step     = 5 * 3600.0;

         //
         // Set up the constraint.
         //
         refval   = 10.0 * RPD;
         cons     = GFConstraint.createReferenceConstraint( "<", refval );

         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );

         //
         // There should be one interval in the result window.
         //

         ok = JNITestutils.chcksi( "interval count", result.card(), "=",
                                                                        1, 0 );

         //
         // Check the angular separation of the targets at the
         // interval's endpoints.
         //


         //
         // Get the maximum radius of each target.
         //
         radii = targ1.getValues("RADII");
         Arrays.sort( radii );
         r1    = radii[2];

         radii = targ2.getValues("RADII");
         Arrays.sort( radii );
         r2    = radii[2];

         interval = result.getInterval( 0 );

         for( i = 0;  i < 2;  i++ )
         {
            //
            // Check the angular separation of the targets,
            // where the target shapes are modeled as spheres
            // of radii r1 and r2 respectively.
            //
            et0 = new TDBTime( interval[i] );

            sep = getTargetSep( targ1, targ2, r1, r2, et0, abcorr, observer );

            //
            // Regarding the tolerance for sep: the time tolerance is 1.e-6
            // seconds, and the relative angular rate of the targets is
            // 360 degrees/day, or about 7.27e-5 rad/sec, so we can use a
            // nanosecond tolerance here.
            //
            ok = JNITestutils.chcksd ( "sep for interval endpoint " + i,
                                       sep,
                                       "~",
                                       refval,
                                       VTIGHT_TOL                           );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "> Inequality test: spherical targets." );

         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 11:00:00 TDB" );
         et1     = new TDBTime( "2000 Jan 1 13:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.
         //
         targ1            = new Body( "alpha" );
         targ2            = new Body( "beta"  );
         shape1           = "sphere";
         shape2           = "sphere";
         frame1           = new ReferenceFrame( "ALPHAFIXED" );
         frame2           = new ReferenceFrame( "BETAFIXED"  );
         abcorr           = new AberrationCorrection( "NONE" );
         observer         = new Body( "SUN" );

         search   = new GFAngularSeparationSearch( targ1,  shape1,  frame1,
                                                   targ2,  shape2,  frame2,
                                                   abcorr, observer         );
         //
         // Use a 5-hour step.
         //
         step     = 5 * 3600.0;

         //
         // Set up the constraint.
         //
         refval   = 10.0 * RPD;
         cons     = GFConstraint.createReferenceConstraint( ">", refval );

         //
         // Run the search.
         //
         result   = search.run ( cnfine, cons, step, MAXIVL );

         //
         // There should be two intervals in the result window.
         //

         ok = JNITestutils.chcksi( "interval count", result.card(), "=",
                                                                        2, 0 );

         //
         // Check the angular separation of the targets the first
         // interval's right endpoints and the second interval's
         // left endpoint.
         //


         //
         // Get the maximum radius of each target.
         //
         radii = targ1.getValues("RADII");
         Arrays.sort( radii );
         r1    = radii[2];

         radii = targ2.getValues("RADII");
         Arrays.sort( radii );
         r2    = radii[2];


         for( i = 0;  i < 2;  i++ )
         {
            interval = result.getInterval( i );

            //
            // Check the angular separation of the targets,
            // where the target shapes are modeled as spheres
            // of radii r1 and r2 respectively.
            //

            //
            // For the first interval, check the second endpoint,
            // and vice versa.
            //
            j   = 1 - i;

            et0 = new TDBTime( interval[j] );

            sep = getTargetSep( targ1, targ2, r1, r2, et0, abcorr, observer );

            //
            // Regarding the tolerance for sep: the time tolerance is 1.e-6
            // seconds, and the relative angular rate of the targets is
            // 360 degrees/day, or about 7.27e-5 rad/sec, so we can use a
            // nanosecond tolerance here.
            //
            ok = JNITestutils.chcksd ( "sep for endpoint " + j +
                                       " of interval " + i,
                                       sep,
                                       "~",
                                       refval,
                                       VTIGHT_TOL                           );
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
         // Get rid of the SPK files.
         //
         CSPICE.spkuef( handle );

         ( new File ( SPK ) ).delete();


         CSPICE.spkuef( nathan );

         ( new File ( NATSPK ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }


   /**
   Compute the angular separation of two spherical targets at
   a given epoch, as seen by a specified observer.
   */
   private static double getTargetSep ( Body                 targ1,
                                        Body                 targ2,
                                        double               r1,
                                        double               r2,
                                        Time                 t,
                                        AberrationCorrection abcorr,
                                        Body                 observer )

      throws SpiceException
   {
      //
      // Local constants
      //
      final ReferenceFrame J2000 = new ReferenceFrame( "J2000" );

      //
      // Local variables
      //
      PositionRecord                    pr1;
      PositionRecord                    pr2;

      double                            angrad1;
      double                            angrad2;
      double                            ctrsep;
      double                            dist1;
      double                            dist2;
      double                            sep = 0.0;

      //
      // Get positions of targets as seen by the observer.
      //

      pr1     =  new PositionRecord( targ1, t, J2000, abcorr, observer );
      pr2     =  new PositionRecord( targ2, t, J2000, abcorr, observer );

      dist1   =  pr1.norm();
      dist2   =  pr2.norm();

      if (  ( dist1 <= r1 ) || ( dist2 <= r2 )  )
      {
         SpiceErrorException exc = SpiceErrorException.create(

         "TestGFAngularSeparationSearch.getTargetSep",

         "SPICE(INVALIDGEOMETRY)",

         "Observer must not be inside either target, but " +
         "target 1 radius = " + r1 + "; observer-target 1 " +
         "distance = " + dist1 + "; " +
         "target 2 radius = " + r2 + "; observer-target 2 " +
         "distance = " + dist2 + "; "                         );

         throw( exc );
      }

      angrad1 =  Math.asin( r1 / dist1 );
      angrad2 =  Math.asin( r2 / dist2 );

      ctrsep  =  pr1.sep( pr2 );

      sep     =  ctrsep - ( angrad1 + angrad2 );

      return( sep );
   }

}

