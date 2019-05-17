
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.GFSurfaceInterceptCoordinateSearch.*;
import static spice.basic.AngularUnits.*;
import static spice.basic.TimeConstants.*;


/**
Class TestGFSurfaceInterceptCoordinateSearch provides methods that
implement test families for the class GFSurfaceInterceptCoordinateSearch.

<h3>Version 2.0.0 29-DEC-2016 (NJB) (EDW)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 17-DEC-2009 (NJB) (EDW)</h3>
*/
public class TestGFSurfaceInterceptCoordinateSearch extends Object
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
   Test GFSurfaceInterceptCoordinateSearch and associated classes.
   */
   public static boolean f_GFSurfaceInterceptCoordinateSearch()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 2.e-6;
      final double                      MED_TOL   = 2.e-5;
      final double                      MEDABS = 1.e-5;
      final double                      MEDREL = 1.e-10;


      final int                         MAXIVL = 100000;
      final int                         MAXWIN = 2 * MAXIVL;
      final int                         NCORR  = 9;
      final int                         NFRAME = 19;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              Alpha = new Body( "ALPHA" );
      Body                              observer;
      Body                              target;

      GFConstraint                      cons;

      GFSurfaceInterceptCoordinateSearch     search;

      ReferenceFrame                    dref;
      ReferenceFrame                    fixref;
      ReferenceFrame                    gammaNadir;

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


      String                            coordinate;
      String                            coordinateSystem;
      String                            kvname;
      String                            method;
      String                            qname;
      String                            relateStr;
      String[]                          textbuf;
      String                            title;

      TDBTime                           et0;
      TDBTime                           et1;
      TDBTime                           xEt;

      Vector3                           dvec;


      boolean                           ok;

      double                            adjust;
      double[]                          interval;
      double[]                          newRadii;
      double[]                          originalRadii;
      double[]                          prevIval;
      double[]                          radii;
      double                            refval;
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

         JNITestutils.topen ( "f_GFSurfaceInterceptCoordinateSearch" );


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
            target           = new Body( "moon"  );
            observer         = new Body( "earth" );
            fixref           = new ReferenceFrame( "IAU_MOON" );
            abcorr           = new AberrationCorrection( "LT" );
            coordinateSystem = Coordinates.LATITUDINAL;
            coordinate       = Coordinates.LATITUDE;
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 1.0, 0.0, 0.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

            step     = 0.0;

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
            target           = new Body( "moon"  );
            observer         = new Body( "moon" );
            fixref           = new ReferenceFrame( "IAU_MOON" );
            abcorr           = new AberrationCorrection( "LT" );
            coordinateSystem = Coordinates.LATITUDINAL;
            coordinate       = Coordinates.LATITUDE;
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 1.0, 0.0, 0.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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
            target           = new Body( "moon"  );
            observer         = new Body( "earth" );
            fixref           = new ReferenceFrame( "IAU_MOON" );
            abcorr           = new AberrationCorrection( "LT" );
            coordinateSystem = Coordinates.LATITUDINAL;
            coordinate       = Coordinates.LATITUDE;
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 1.0, 0.0, 0.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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
            target           = new Body( "moon"  );
            observer         = new Body( "earth" );
            fixref           = new ReferenceFrame( "IAU_MOON" );
            abcorr           = new AberrationCorrection( "LT" );
            coordinateSystem = Coordinates.LATITUDINAL;
            coordinate       = Coordinates.LATITUDE;
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 1.0, 0.0, 0.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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
            target           = new Body( "moon"  );
            observer         = new Body( "earth" );
            fixref           = new ReferenceFrame( "J2000" );
            abcorr           = new AberrationCorrection( "LT" );
            coordinateSystem = Coordinates.LATITUDINAL;
            coordinate       = Coordinates.LATITUDE;
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 1.0, 0.0, 0.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Invalid coordinate system."  );

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
            target           = new Body( "moon"  );
            observer         = new Body( "earth" );
            fixref           = new ReferenceFrame( "IAU_MOON" );
            abcorr           = new AberrationCorrection( "LT" );
            coordinateSystem = "latlon";
            coordinate       = Coordinates.LATITUDE;
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 1.0, 0.0, 0.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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

            Testutils.dogDidNotBark ( "SPICE(NOTSUPPORTED)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(NOTSUPPORTED)", ex );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Invalid coordinate."  );

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
            target           = new Body( "moon"  );
            observer         = new Body( "earth" );
            fixref           = new ReferenceFrame( "IAU_MOON" );
            abcorr           = new AberrationCorrection( "LT" );
            coordinateSystem = Coordinates.LATITUDINAL;
            coordinate       = "LAT";
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 1.0, 0.0, 0.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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

            Testutils.dogDidNotBark ( "SPICE(NOTSUPPORTED)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(NOTSUPPORTED)", ex );
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
            target           = new Body( "moon"  );
            observer         = new Body( "dawn" );
            fixref           = new ReferenceFrame( "IAU_MOON" );
            abcorr           = new AberrationCorrection( "LT" );
            coordinateSystem = Coordinates.LATITUDINAL;
            coordinate       = Coordinates.LATITUDE;
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 1.0, 0.0, 0.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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
            target           = new Body( "moon"  );
            observer         = new Body( "earth" );
            fixref           = new ReferenceFrame( "IAU_MOON" );
            abcorr           = new AberrationCorrection( "LT" );
            coordinateSystem = Coordinates.LATITUDINAL;
            coordinate       = Coordinates.LATITUDE;
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 1.0, 0.0, 0.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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
            target           = new Body( "alpha"  );
            observer         = new Body( "gamma" );
            fixref           = new ReferenceFrame( "ALPHAFIXED" );
            abcorr           = new AberrationCorrection( "NONE" );
            coordinateSystem = Coordinates.LATITUDINAL;
            coordinate       = Coordinates.LATITUDE;
            method           = ELLIPSOID;
            dref             = new ReferenceFrame( "J2000" );
            dvec             = new Vector3( 0.0, 0.0, -1.0 );

            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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

         et0     = new TDBTime( "2000 Jan 1 17:00:00 TDB" );
         et1     = new TDBTime( "2000 Jan 1 19:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.
         //
         target           = new Body( "alpha"  );
         observer         = new Body( "gamma" );
         fixref           = new ReferenceFrame( "alphafixed" );
         abcorr           = new AberrationCorrection( "none" );
         coordinateSystem = Coordinates.RECTANGULAR;
         coordinate       = Coordinates.X;
         method           = ELLIPSOID;

         //
         // Use a ray pointing in the ALPHAFIXED -X direction.
         // This coincides with the J2000 -Z direction.
         //
         dref             = new ReferenceFrame( "J2000" );
         dvec             = new Vector3( 0.0, 0.0, -1.0 );

         search   = new GFSurfaceInterceptCoordinateSearch(

                       target,    fixref,   method,  abcorr,
                       observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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

         xEt = new TDBTime( "2000 jan 1 18:00 TDB" );


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
         // For debugging;
         //
         // PositionRecord pos = new PositionRecord( target, xEt, fixref,
         //                                                 abcorr, observer );
         //
         // System.out.println( pos );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Absolute minimum test." );

         //
         // Re-run the previous test with a new ray, new constraint and a new
         // confinement window.
         //

         //
         // Use a ray pointing in the ALPHAFIXED +X direction.
         // This coincides with the J2000 +Z direction.
         //
         dref             = new ReferenceFrame( "J2000" );
         dvec             = new Vector3( 0.0, 0.0, 1.0 );

         search   = new GFSurfaceInterceptCoordinateSearch(

                       target,    fixref,   method,  abcorr,
                       observer,  dref,     dvec,    coordinateSystem,
                       coordinate );

         //
         // Set up the constraint.
         //
         cons     = GFConstraint.createExtremumConstraint(
                                               GFConstraint.ABSOLUTE_MINIMUM );

         cnfine   = new SpiceWindow();

         et0      = new TDBTime( "2000 Jan 2 05:00 TDB" );
         et1      = new TDBTime( "2000 Jan 2 07:00 TDB" );

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

         xEt = new TDBTime( "2000 jan 2 06:00 TDB" );


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
         // For this test we want to duplicate, as far as possible,
         // the geometry of the corresponding
         // TestGFSubObserverCoordinateSearch test case. So we want
         // to pick a ray such that the surface intercept coincides
         // with the sub-observer point given by the "intercept"
         // defintion. We'll make up a nadir-oriented dynamic frame
         // for this purpose.
         //

         textbuf = new String[NFRAME];

         textbuf[0]  = "FRAME_GAMMA_NADIR             =  1450000 ";
         textbuf[1]  = "FRAME_1450000_NAME            = 'GAMMA_NADIR' ";
         textbuf[2]  = "FRAME_1450000_CLASS           = 5 ";
         textbuf[3]  = "FRAME_1450000_CLASS_ID        = 1450000 ";
         textbuf[4]  = "FRAME_1450000_CENTER          = 'GAMMA' ";
         textbuf[5]  = "FRAME_1450000_RELATIVE        = 'J2000' ";
         textbuf[6]  = "FRAME_1450000_DEF_STYLE       = 'PARAMETERIZED' ";
         textbuf[7]  = "FRAME_1450000_FAMILY          = 'TWO-VECTOR' ";
         textbuf[8]  = "FRAME_1450000_PRI_AXIS        = '-Z' ";
         textbuf[9]  = "FRAME_1450000_PRI_VECTOR_DEF  = " +
                                                 "'OBSERVER_TARGET_POSITION' ";
         textbuf[10] = "FRAME_1450000_PRI_OBSERVER    = 'GAMMA' ";
         textbuf[11] = "FRAME_1450000_PRI_TARGET      = 'ALPHA' ";
         textbuf[12] = "FRAME_1450000_PRI_ABCORR      = 'NONE' ";
         textbuf[13] = "FRAME_1450000_SEC_AXIS        = '-X' ";
         textbuf[14] = "FRAME_1450000_SEC_VECTOR_DEF  = " +
                                                 "'OBSERVER_TARGET_VELOCITY' ";
         textbuf[15] = "FRAME_1450000_SEC_OBSERVER    = 'GAMMA' ";
         textbuf[16] = "FRAME_1450000_SEC_TARGET      = 'ALPHA' ";
         textbuf[17] = "FRAME_1450000_SEC_FRAME       = 'J2000' ";
         textbuf[18] = "FRAME_1450000_SEC_ABCORR      = 'NONE' ";

         KernelPool.loadFromBuffer( textbuf );

         //
         // Re-run the previous test with a new constraint, new ray, and a new
         // confinement window.
         //
         target           = new Body( "alpha"  );
         observer         = new Body( "gamma" );
         fixref           = new ReferenceFrame( "alphafixed" );
         abcorr           = new AberrationCorrection( "none" );
         coordinateSystem = Coordinates.RECTANGULAR;
         coordinate       = Coordinates.X;
         method           = ELLIPSOID;

         gammaNadir       = new ReferenceFrame( "GAMMA_NADIR" );
         dref             = gammaNadir;
         dvec             = new Vector3( 0.0, 0.0, -1.0 );

         search   = new GFSurfaceInterceptCoordinateSearch(

                       target,    fixref,   method,  abcorr,
                       observer,  dref,     dvec,    coordinateSystem,
                       coordinate );

         //
         // Set up the constraint.
         //

         //
         // We're going to make the math simpler by replacing the radii of
         // Alpha with those of a sphere of radius 1.e4 km. Save the original
         // radii so we can restore them at the end of the test case.
         //

         kvname        = "BODY1000_RADII";
         originalRadii = KernelPool.getDouble( kvname );

         newRadii      = ( new Vector3( 1.e4, 1.e4, 1.e4 ) ).toArray();

         KernelPool.putDouble( kvname, newRadii );

         //
         // The expected absolute maximum ALPHAFIXED X value is 10000 km.
         // An X value of half that is attained at orbital position
         // +/- 60 degrees from the +X axis of the ALPHAFIXED frame.
         // This corresponds to +/- 4 hours from the epoch of the extremum.
         //

         adjust   = 5000.0;

         cons     = GFConstraint.createExtremumConstraint(

                      GFConstraint.ADJUSTED_ABSMAX, adjust );

         cnfine   = new SpiceWindow();

         et0      = new TDBTime( "2000 Jan 1 12:00 TDB" );
         et1      = new TDBTime( "2000 Jan 2 00:00 TDB" );

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

         xEt = new TDBTime( "2000 jan 1 14:00 TDB" );


         ok = JNITestutils.chcksd ( "event start",
                                    interval[0],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         xEt = new TDBTime( "2000 jan 1 22:00 TDB" );

         ok = JNITestutils.chcksd ( "event stop",
                                    interval[1],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         //
         // Restore the radii of Alpha.
         //
         KernelPool.putDouble( kvname, originalRadii );





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Adjusted absolute minimum test." );

         //
         // Re-run the previous test with a new constraint and a new
         // confinement window.
         //
         target           = new Body( "alpha"  );
         observer         = new Body( "gamma" );
         fixref           = new ReferenceFrame( "alphafixed" );
         abcorr           = new AberrationCorrection( "none" );
         coordinateSystem = Coordinates.RECTANGULAR;
         coordinate       = Coordinates.X;
         method           = ELLIPSOID;

         gammaNadir       = new ReferenceFrame( "GAMMA_NADIR" );
         dref             = gammaNadir;
         dvec             = new Vector3( 0.0, 0.0, -1.0 );

         search   = new GFSurfaceInterceptCoordinateSearch(

                       target,    fixref,   method,  abcorr,
                       observer,  dref,     dvec,    coordinateSystem,
                       coordinate );

         //
         // Set up the constraint.
         //

         //
         // We're going to make the math simpler by replacing the radii of
         // Alpha with those of a sphere of radius 1.e4 km. Save the original
         // radii so we can restore them at the end of the test case.
         //

         kvname        = "BODY1000_RADII";
         originalRadii = KernelPool.getDouble( kvname );

         newRadii      = ( new Vector3( 1.e4, 1.e4, 1.e4 ) ).toArray();

         KernelPool.putDouble( kvname, newRadii );


         // The expected absolute minimum ALPHAFIXED X value is -10000 km.
         // An X value of half that is attained at orbital position
         // +/- 60 degrees from the -X axis of the ALPHAFIXED frame. This
         // corresponds to +/- 4 hours from the epoch of the extremum.
         //

         adjust   = 5000.0;

         cons     = GFConstraint.createExtremumConstraint(

                      GFConstraint.ADJUSTED_ABSMIN, adjust );

         cnfine   = new SpiceWindow();

         et0      = new TDBTime( "2000 Jan 2 00:00 TDB" );
         et1      = new TDBTime( "2000 Jan 2 12:00 TDB" );

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

         xEt = new TDBTime( "2000 jan 2 02:00 TDB" );


         ok = JNITestutils.chcksd ( "event start",
                                    interval[0],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         xEt = new TDBTime( "2000 jan 2 10:00 TDB" );

         ok = JNITestutils.chcksd ( "event stop",
                                    interval[1],
                                    "~",
                                    xEt.getTDBSeconds(),
                                    TIGHT_TOL           );

         //
         // Restore the radii of Alpha.
         //
         KernelPool.putDouble( kvname, originalRadii );





         //
         // --------Case-----------------------------------------------
         //
         //
         // A variety of test cases that don't use aberration corrections
         // follow. These are essentially copied from Ed Wright's tspice_c
         // test family f_gfsubc_c.
         //

         //
         //
         // Event test block.
         //
         // Define the coordinate test conditions.
         //
         // Conditions: all conditions should occur once per
         // GAMMA orbit (delta_t = 24 hours).
         //
         //
         // Define a block for event tests. Assign the test conditions
         // in the MDESC and MREFS arrays.
         //
         String[] MDESC = {  "RECTANGULAR   : X           : <",
                             "RECTANGULAR   : Y           : <",
                             "RECTANGULAR   : Z           : <",
                             "RECTANGULAR   : X           : >",
                             "RECTANGULAR   : Y           : >",
                             "RECTANGULAR   : Z           : >",
                             "LATITUDINAL   : RADIUS      : >",
                             "LATITUDINAL   : LATITUDE    : >",
                             "LATITUDINAL   : LATITUDE    : <",
                             "RA/DEC        : RANGE       : >",
                             "RA/DEC        : DECLINATION : >",
                             "RA/DEC        : DECLINATION : <",
                             "SPHERICAL     : RADIUS      : >",
                             "SPHERICAL     : COLATITUDE  : >",
                             "SPHERICAL     : COLATITUDE  : <",
                             "CYLINDRICAL   : RADIUS      : >",
                             "CYLINDRICAL   : Z           : <",
                             "CYLINDRICAL   : Z           : >",
                             "CYLINDRICAL   : LONGITUDE   : >",
                             "SPHERICAL     : LONGITUDE   : <",
                             "LATITUDINAL   : LONGITUDE   : <",
                             "RA/DEC        : RIGHT ASCENSION : >" };


         //
         // Test conditions reference values.
         //

         radii = Alpha.getValues( "RADII" );

         double[] MREFS =  { radii[0],
                             radii[1],
                             radii[2],
                            -radii[0],
                            -radii[1],
                            -radii[2],
                             0.,
                            -90.*RPD,
                             90.*RPD,
                             0.,
                            -90.*RPD,
                             90.*RPD,
                             0.,
                             0.,
                             180.*RPD,
                             0.,
                             radii[2],
                            -radii[2],
                             0.,
                             0.,
                             0.,
                             0. };

         //
         // The original time interval was thirty days; we'll make do with 5.
         //

         et0      = new TDBTime( "2000 JAN 01 18:00:00 TDB" );
         et1      = new TDBTime( "2000 JAN 06 18:00:00 TDB" );

         adjust   = 0.0;

         target   = new Body( "Alpha" );
         fixref   = new ReferenceFrame( "Alphafixed" );
         method   = ELLIPSOID;
         observer = new Body( "Gamma" );

         abcorr   = new AberrationCorrection( CORR[0] );

         //
         // --------Case-----------------------------------------------
         //
         //
         // Sweeps of the GAMMA centered J2000 Z axis on ALPHA.
         //

         dref     = new ReferenceFrame( "J2000" );
         dvec     = new Vector3( 0.0, 0.0, 1.0 );

         //
         // Use a 1.25-hour step.
         //
         step   = SPD * (1.25/24.);

         ntest = MDESC.length - 1;

         for ( i = 0;  i < ntest;  i++ )
         {
            cnfine = new SpiceWindow();

            cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );


            //
            // --------Case-----------------------------------------------
            //
            JNITestutils.tcase ( "ALPHA sweeps from GAMMA, Z " + MDESC[i] );


            StringTokenizer tokenizer = new StringTokenizer ( MDESC[i], ":" );

            coordinateSystem = tokenizer.nextToken();
            coordinate       = tokenizer.nextToken();
            relateStr        = tokenizer.nextToken();

            refval           = MREFS[i];

            //
            // Specify the search.
            //
            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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
            result = search.run ( cnfine, cons, step, MAXIVL );


            // System.out.println( "result window for i = " + i + " = " +
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
         // Sweeps of the GAMMA centered J2000 Y axis on ALPHA.
         //


         dref     = new ReferenceFrame( "J2000" );
         dvec     = new Vector3( 0.0, 1.0, 0.0 );

         //
         // Use a 3.25-hour step.
         //
         step   = SPD * (3.25/24.);

         ntest = MDESC.length - 1;

         for ( i = 0;  i < ntest;  i++ )
         {
            cnfine = new SpiceWindow();

            cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );


            //
            // --------Case-----------------------------------------------
            //
            JNITestutils.tcase (  "ALPHA sweeps from GAMMA, Y " + MDESC[i] );


            StringTokenizer tokenizer = new StringTokenizer ( MDESC[i], ":" );

            coordinateSystem = tokenizer.nextToken();
            coordinate       = tokenizer.nextToken();
            relateStr        = tokenizer.nextToken();

            refval           = MREFS[i];

            //
            // Specify the search.
            //
            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

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
            result = search.run ( cnfine, cons, step, MAXIVL );


            // System.out.println( "result window for i = " + i + " = " +
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
         // Sweeps of the GAMMA centered J2000 X axis on ALPHA.
         //


         dref     = new ReferenceFrame( "J2000" );
         dvec     = new Vector3( 1.0, 0.0, 0.0 );

         //
         // Use a 1.25-hour step.
         //
         step   = SPD * (1.25/24.);

         ntest = MDESC.length - 1;

         for ( i = 0;  i < ntest;  i++ )
         {
            cnfine = new SpiceWindow();

            cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );


            //
            // --------Case-----------------------------------------------
            //
            JNITestutils.tcase (  "ALPHA sweeps from GAMMA, X " + MDESC[i] );


            StringTokenizer tokenizer = new StringTokenizer ( MDESC[i], ":" );

            coordinateSystem = tokenizer.nextToken();
            coordinate       = tokenizer.nextToken();
            relateStr        = tokenizer.nextToken();

            refval           = MREFS[i];

            //
            // Specify the search.
            //
            search   = new GFSurfaceInterceptCoordinateSearch(

                          target,    fixref,   method,  abcorr,
                          observer,  dref,     dvec,    coordinateSystem,
                          coordinate );

            //
            // Set up the constraint.
            //
            //
            // This assignment works for relational operators other than
            // extrema.
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
            result = search.run ( cnfine, cons, step, MAXIVL );


            // System.out.println( "result window for i = " + i + " = " +
            //                                                        result );


            //
            // Check the number of solution intervals.
            //
            // The J2000 X unit with origin at GAMMA's center should never
            // intersect ALPHA.
            //
            n  = result.card();

            ok = JNITestutils.chcksi( "n", n, "=", 0, 0 );
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

}

