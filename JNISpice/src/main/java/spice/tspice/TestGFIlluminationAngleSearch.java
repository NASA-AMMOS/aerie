
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.GFIlluminationAngleSearch.*;
import static spice.basic.AngularUnits.*;
import static spice.basic.TimeConstants.*;


/**
Class TestGFIlluminationAngleSearch provides methods that implement
test families for the class GFIlluminationAngleSearch.

<h3>Version 2.0.0 28-DEC-2016 (NJB)</h3>

Moved clean-up to finally block.

<p> Previous update was 02-APR-2016 (NJB)

<p> Previous update was 11-MAR-2014  
*/
public class TestGFIlluminationAngleSearch extends Object
{

   //
   // Class constants
   //
   private static String  OCTLSPK       = "octl_test.bsp";
   private static String  PCK1          = "nat.tpc";
   private static String  PCK2          = "generic.tpc";   
   private static String  SPK1          = "nat.bsp";
   private static String  SPK2          = "generic.bsp";

   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test GFIlluminationAngleSearch and associated classes.
   */
   public static boolean f_GFIlluminationAngleSearch()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIMTOL = 1.e-6;
      final double                      LOOSE  = 1.e-5;
      final double                      ANGTOL = 1.e-9;
 

      final int                         MAXIVL = 100;
      final int                         MAXWIN = 2 * MAXIVL;


      final AberrationCorrection        NONE   = 
                                           new AberrationCorrection ( "NONE" );

      final Body                        OCTL   = new Body( "OCTL" ); 

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              illum;
      Body                              obsrvr;
      Body                              target;

      Ellipsoid                         ell;

      GFConstraint                      cons;

      GFIlluminationAngleSearch         search;
 
      GFPhaseAngleSearch                phsearch;

      GFPositionCoordinateSearch        pcsearch;

      IlluminationAngles                illumAngles;

      LatitudinalCoordinates            lc;

      PositionRecord                    pr;

      ReferenceFrame                    fixref;

      ReferenceFrame                    J2000 = 
                                           new ReferenceFrame( "J2000" );

      TDBDuration                       lt;

      TDBTime                           et;
      TDBTime                           et0; 
      TDBTime                           et1;
      TDBTime[]                         TDBInterval;
      TDBTime                           left;
      TDBTime[]                         modET = new TDBTime[2];
      TDBTime                           right;
      TDBTime                           xTime;

      Ray                               ray;

      SpiceWindow                       cnfine;
      SpiceWindow                       modcnf = null;
      SpiceWindow                       modres = null;
      SpiceWindow                       result = null;
      SpiceWindow                       xrsult = null;

      Vector3                           spoint;
      Vector3                           origin;
      Vector3                           dir;

      String                            angtyp;

      String[]                          corrs   = {
                                           "NONE",
                                           "lt",
                                           " lt+s",
                                           " cn",
                                           " cn + s"
                                         };

      String                             method;
      String                             relate;

      String[]                           relops = {         
                                             "=",      "<",      ">",  
                                             "LOCMIN", "LOCMAX", 
                                             "ABSMIN", "ADJ_ABSMIN",  
                                             "ABSMAX", "ADJ_ABSMAX"   };

      String                             time0;
      String                             title;

      boolean                            ok;
 
      double[]                           adj = { 0.0,  0.0,    0.0,  0.0, 0.0,
                                                 0.0,  5.e-2,  0.0,  5.e-2    };

      double                             adjust;
      double[]                           interval;
      double                             lat;
      double                             lon;
      double[][]                         modResultArray;
      double[]                           radii;
      double                             refval;
      double                             step;

      double[]                           vals   = { 65.0, 70.0, 60.0,  
                                                    0.0,  0.0,  0.0,
                                                    0.0,  0.0,  0.0  };

      int                                han1 = 0;
      int                                han2 = 0;
      int                                n;
      int                                wncard;
      int                                xn;

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

         JNITestutils.topen ( "f_GFIlluminationAngleSearch" );


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
         ( new File ( PCK1 ) ).delete();

         JNITestutils.tstpck( PCK1, true, false );

         //
         // Same for Nat's solar system PCK.
         //
         ( new File ( PCK2 ) ).delete();

         JNITestutils.natpck( PCK2, true, false );

         //
         // Delete Nat's SPK if it exists. Create and load a new
         // version of the file.
         //
         ( new File ( SPK1 ) ).delete();

         han1 = JNITestutils.natspk( SPK1, true );

         //
         // Delete generic SPK if it exists. Create and load a new
         // version of the file.
         //
         ( new File ( SPK2 ) ).delete();

         han2 = JNITestutils.tstspk( SPK2, true );



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Non-positive step size."  );

         cnfine  = new SpiceWindow();

         et0     = new TDBTime( "2000 Jan 1 00:00:00 TDB" );
         et1     = new TDBTime( "2000 Mar 1 00:00:00 TDB" );

         cnfine  = cnfine.insert( et0.getTDBSeconds(),
                                  et1.getTDBSeconds() );

         //
         // Set up the search geometry parameters.
         //
         target = new Body( "earth" );
         illum  = new Body( "sun"   );
         obsrvr = new Body( "moon"  );
         fixref = new ReferenceFrame( "IAU_EARTH" );
         abcorr = new AberrationCorrection( "CN+S" );
         method = ELLIPSOID;
         angtyp = INCIDENCE;

         lon    = 100.0 * RPD;
         lat    =  30.0 * RPD;

         lc     = new LatitudinalCoordinates( 1.0, lon, lat );
         dir    = lc.toRectangular();
         origin = new Vector3( 0.0, 0.0, 0.0 );
          
         radii  = KernelPool.getDouble( "BODY399_RADII" );
 
         ell    = new Ellipsoid ( radii[0], radii[1], radii[2] );
         ray    = new Ray ( origin, dir );

         spoint = ( new RayEllipsoidIntercept(ray, ell) ).getIntercept();

         //System.out.println ( spoint );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, obsrvr, spoint );

         //
         // Set up the constraint.
         //
         relate  = "=";
         refval  = 0.0;
         cons    = GFConstraint.createReferenceConstraint( relate, refval );

         //
         // Run the search.
         //

         try
         {
            step   = 0.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

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
         JNITestutils.tcase ( "Workspace too small (detected " +
                              "before search)."                  );

         //
         // Note that this case differs from the corresponding one 
         // in the tspice_c test family f_gfilum_c, because in JNISpice
         // one cannot separate the result window size from the number of
         // workspace intervals. The C version of the test has `nintvls'
         // set to 1 and the size of `result' set to 1 (endpoint).
         //

         try
         {

            //
            // Use the search criteria created in the last test, but use
            // a positive step size. Set the maximum number of result intervals
            // to 0.
            //
            step   = 300.0;
            result = search.run ( cnfine, cons, step, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(INVALIDDIMENSION)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(INVALIDDIMENSION)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Workspace too small (detected " +
                              "during search)."                  );

         //
         // Note that this case differs from the corresponding one 
         // in the tspice_c test family f_gfilum_c, because in JNISpice
         // one cannot separate the result window size from the number of
         // workspace intervals. The C version of the test has `nintvls'
         // set to MAXWIN and the size of `result' set to 2 (1 interval).
         //
         method = "Ellipsoid";
         angtyp = "INCIDENCE";
         target = new Body( "EARTH" );
         illum  = new Body( "SUN" );
         fixref = new ReferenceFrame( "IAU_EARTH" );
         abcorr = new AberrationCorrection( "CN+S" );
         obsrvr = new Body( "MOON" );
         relate = "LOCMAX";

         //
         // `spoint' was set in the previous case.
         //

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, obsrvr, spoint );

         et0 = new TDBTime( "2000 Jan 1 TDB" );
         et1 = new TDBTime( "2000 Jan 5 TDB" );

         cnfine = new SpiceWindow();

         cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );

         //
         // Set up the constraint.
         //
         relate  = "LOCMAX";
         
         cons    = GFConstraint.createExtremumConstraint( relate );

         try
         {
            //
            // Use the search criteria created in the last test, but use
            // a positive step size. Set the maximum number of workspace 
            // intervals to 1.
            //
            step   = 300.0;
            result = search.run ( cnfine, cons, step, 1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(WINDOWEXCESS)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(WINDOWEXCESS)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Unrecognized value of FIXREF." );

         ReferenceFrame badref = new ReferenceFrame( "xxx" );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            badref, abcorr, obsrvr, spoint );

         try
         {
            //
            // Use the search criteria created in the last test, but use
            // a positive step size. Set the maximum number of workspace 
            // intervals to 1.
            //
            step   = 300.0;
            result = search.run ( cnfine, cons, step, MAXWIN );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(UNKNOWNFRAME)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(UNKNOWNFRAME)", ex );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "FIXREF is not centered on target body." );

         badref = new ReferenceFrame( "IAU_MARS" );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            badref, abcorr, obsrvr, spoint );

         try
         {
            //
            // Use the search criteria created in the last test, but use
            // a positive step size. Set the maximum number of workspace 
            // intervals to 1.
            //
            step   = 300.0;
            result = search.run ( cnfine, cons, step, MAXWIN );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(INVALIDFRAME)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(INVALIDFRAME)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Bad computation method" );

         method = "DSK";

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, obsrvr, spoint );

         try
         {
            //
            // Use the search criteria created in the last test, but use
            // a positive step size. Set the maximum number of workspace 
            // intervals to 1.
            //
            step   = 300.0;
            result = search.run ( cnfine, cons, step, MAXWIN );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(INVALIDMETHOD)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(INVALIDMETHOD)", ex );
         }

         method = ELLIPSOID;

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Bad illumination angle type" );

         angtyp = "XYZ";

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, obsrvr, spoint );

         try
         {
            //
            // Use the search criteria created in the last test, but use
            // a positive step size. Set the maximum number of workspace 
            // intervals to 1.
            //
            step   = 300.0;
            result = search.run ( cnfine, cons, step, MAXWIN );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(NOTSUPPORTED)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(NOTSUPPORTED)", ex );
         }


         angtyp = "INCIDENCE";

         //
         // --------Case-----------------------------------------------
         //

         //
         // The case "bad relational operator" is not applicable, thanks
         // to error checking in the GFConstraint class.
         //

         //
         // --------Case-----------------------------------------------
         //

         //
         // The case "bad aberration correction values" is not applicable, 
         // thanks to error checking in the AberrationCorrection class.
         //
         // However transmission corrections must be checked.
         // 

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Transmission aberration corrections" );

         AberrationCorrection badcor = new AberrationCorrection( "XLT+S" );
 
         method = ELLIPSOID;


         try
         {

            search = 

                new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                               fixref, badcor, obsrvr, spoint );
   
            step   = 300.0;
            result = search.run ( cnfine, cons, step, MAXWIN );


            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(NOTSUPPORTED)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(NOTSUPPORTED)", ex );
         }

  

         //
         // --------Case-----------------------------------------------
         //

         //
         // The case "bad value of ADJUST" is not applicable, thanks
         // to error checking in the GFConstraint class.
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Unrecognized target, observer, or " + 
                              "illumination source."                 );

         Body badobj = new Body( "badTarget" );
         
         search = 

             new GFIlluminationAngleSearch( method, angtyp, badobj, illum,
                                            fixref, abcorr, obsrvr, spoint );

         //
         // Set up the constraint.
         //
         relate  = "=";
         refval  = 0.0;
         cons    = GFConstraint.createReferenceConstraint( relate, refval );

         //
         // Run the search.
         //

         try
         {
            step   = 300.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(IDCODENOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(IDCODENOTFOUND)", ex );
         }


         //
         // Bad observer test case:
         //
         badobj = new Body( "badObserver" );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, badobj, spoint );

         try
         {
            step   = 300.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(IDCODENOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(IDCODENOTFOUND)", ex );
         }


         //
         // Bad illumination source test case:
         //
         badobj = new Body( "badIllumSource" );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, badobj,
                                            fixref, abcorr, obsrvr, spoint );

         try
         {
            step   = 300.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(IDCODENOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(IDCODENOTFOUND)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Target and observer are identical." );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, target, spoint );

         try
         {
            step   = 300.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(BODIESNOTDISTINCT)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(BODIESNOTDISTINCT)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Target and illumination source are identical." );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, target,
                                            fixref, abcorr, obsrvr, spoint );

         try
         {
            step   = 300.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(BODIESNOTDISTINCT)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(BODIESNOTDISTINCT)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "No SPK data for observer." );

         badobj = new Body( "GASPRA" );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, badobj, spoint );

         try
         {
            step   = 300.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(SPKINSUFFDATA)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(SPKINSUFFDATA)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "No SPK data for target." );

         badobj = new Body( "GASPRA" );
         badref = new ReferenceFrame( "IAU_GASPRA" );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, badobj, illum,
                                            badref, abcorr, obsrvr, spoint );

         try
         {
            step   = 300.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(SPKINSUFFDATA)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(SPKINSUFFDATA)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "No SPK data for illumination source." );

         badobj = new Body( "GASPRA" );
         fixref = new ReferenceFrame ( "IAU_EARTH" );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, badobj,
                                            fixref, abcorr, obsrvr, spoint );

         try
         {
            step   = 300.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(SPKINSUFFDATA)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(SPKINSUFFDATA)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "No PCK orientation data for target." );


         badref = new ReferenceFrame ( "ITRF93" );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            badref, abcorr, obsrvr, spoint );

         try
         {
            step   = 300.0;

            result = search.run ( cnfine, cons, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(FRAMEDATANOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,  "SPICE(FRAMEDATANOTFOUND)", ex );
         }



         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         /*
         *********************************************************************
 
         Simple cases using Nat's solar system

         *********************************************************************
         */

         /*
         The cases below all involve finding times when local extrema are
         attained. In each case we know the correct answers. All of these
         tests are done with aberration corrections set to "NONE".
   
         Following these tests, a series of comparisons is performed using
         results produced by alternate methods. The second set of tests is
         comprehensive: those tests use all combinations of operators and
         aberration corrections.
         */

         // 
         // ---- Case ---------------------------------------------------------
         //

         //
         // All expected event times are based on Nat's solar system.
         //
         JNITestutils.tcase( "Local minimum of emission angle at north "  +
                             "pole of ALPHA; observer is SUN; abcorr = NONE" );
         /*
         The period of ALPHA's orbit is 7 days. This can be derived
         from the relative angular velocities of ALPHA and BETA
         and the period of the occultation starts.

         A minimum of the emission angle should occur whenever ALPHA's
         north pole points toward the sun. This should happen at the
         J2000 epoch and every 7 days before or after.

         Create a confinement window with 4 intervals.
         */
         cnfine = new SpiceWindow();

         time0   = "2000 Jan 1 12:00:00 TDB";
         et0     = new TDBTime( time0 );

         for ( int i = 0;  i < 4;  i++ )
         {
            left  = et0.add ( new TDBDuration( (i * SPD * 7) - 3600.0 )   );

            right = left.add( new TDBDuration( 2 * 3600.0 )  );

            cnfine.insert( left.getTDBSeconds(), right.getTDBSeconds() );
         }

         //
         // Set up the search geometry parameters.
         //
         target = new Body( "alpha" );
         illum  = new Body( "sun"   );
         obsrvr = new Body( "sun"  );
         fixref = new ReferenceFrame( "alphafixed" );
         abcorr = NONE;
         method = ELLIPSOID;
         angtyp = EMISSION;

         radii  = target.getValues( "RADII" );

         spoint = new Vector3 ( 0.0, 0.0, radii[2] );
  
         //System.out.println ( spoint );

         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, obsrvr, spoint );

         //
         // Set up the constraint.
         //
         relate  = "LOCMIN";
         
         cons    = GFConstraint.createExtremumConstraint( relate );

         //
         // Run the search. Use a 10 hour step.
         //
         step   = 10.0 * 3600.0;

         result = search.run ( cnfine, cons, step, MAXIVL );
 
         //
         // Check the result window.
         //
         wncard = result.card();

         ok = JNITestutils.chcksi( "result cardinality", wncard, "=", 4, 0 );



         for ( int i = 0;  i < wncard;  i++ )
         {
            interval = result.getInterval( i );

            left     = new TDBTime( interval[0] );
            right    = new TDBTime( interval[1] );

            //
            //  Check event start time.
            //
            title    = String.format ( "Event %d start time", i );

            xTime    = et0.add(  new TDBDuration( i * SPD * 7 )  );

            ok = JNITestutils.chcksd ( "event start",
                                       left.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
            //
            //  Check event stop time; this should equal the start time
            //  since we're searching for an extremum.
            //
            title    = String.format ( "Event %d stop time", i );

            ok = JNITestutils.chcksd ( "event stop",
                                       right.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
         }
 


         // 
         // ---- Case ---------------------------------------------------------
         //

         //
         // All expected event times are based on Nat's solar system.
         //
         JNITestutils.tcase( "Local minimum of solar incidence "   +
                             "angle at north pole of ALPHA; "      +
                             "observer is SUN; abcorr = NONE"        );

         angtyp = INCIDENCE;

        
         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, obsrvr, spoint );

         //
         // Use constraint and confinement window from the previous case.
         //
         result = search.run ( cnfine, cons, step, MAXIVL );
 
         //
         // Check the result window.
         //
         wncard = result.card();

         ok = JNITestutils.chcksi( "result cardinality", wncard, "=", 4, 0 );



         for ( int i = 0;  i < wncard;  i++ )
         {
            interval = result.getInterval( i );

            left     = new TDBTime( interval[0] );
            right    = new TDBTime( interval[1] );

            //
            //  Check event start time.
            //
            title    = String.format ( "Event %d start time", i );

            xTime    = et0.add(  new TDBDuration( i * SPD * 7 )  );

            ok = JNITestutils.chcksd ( "event start",
                                       left.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
            //
            //  Check event stop time; this should equal the start time
            //  since we're searching for an extremum.
            //
            title    = String.format ( "Event %d stop time", i );

            ok = JNITestutils.chcksd ( "event stop",
                                       right.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
         }
 
         //       
         // ---- Case ---------------------------------------------------------
         //
         JNITestutils.tcase( "Local maximum of emission angle at north "  +
                             "pole of ALPHA; observer is SUN; abcorr = NONE" );

         angtyp = EMISSION;

         //
         // ALPHA orbits with constant angular velocity on a circular path,
         // so local maxima occur 1/2 period apart from local minima.
         //
         // Create a confinement window with 4 intervals.
         //
         cnfine = new SpiceWindow();

         for ( int i = 0;  i < 4;  i++ )
         {
            left  = et0.add (new TDBDuration( 3.5*SPD + (i*SPD*7) - 3600.0 ) );
            right = left.add(new TDBDuration( 2 * 3600.0 ) );

            cnfine.insert ( left.getTDBSeconds(), right.getTDBSeconds() );
         }


         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, obsrvr, spoint );

         //
         // Set up the constraint.
         //
         relate  = "LOCMAX";
         
         cons    = GFConstraint.createExtremumConstraint( relate );

         //
         // Run the search. Use a 10 hour step.
         //
         step   = 10.0 * 3600.0;

         result = search.run ( cnfine, cons, step, MAXIVL );

         //
         // Check the result window.
         //
         wncard = result.card();

         ok = JNITestutils.chcksi( "result cardinality", wncard, "=", 4, 0 );


         for ( int i = 0;  i < wncard;  i++ )
         {
            interval = result.getInterval( i );

            left     = new TDBTime( interval[0] );
            right    = new TDBTime( interval[1] );

            //
            //  Check event start time.
            //
            title    = String.format ( "Event %d start time", i );

            xTime    = et0.add(  new TDBDuration( (3.5 + (7*i)) * SPD )  );

            ok = JNITestutils.chcksd ( "event start",
                                       left.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
            //
            //  Check event stop time; this should equal the start time
            //  since we're searching for an extremum.
            //
            title    = String.format ( "Event %d stop time", i );

            ok = JNITestutils.chcksd ( "event stop",
                                       right.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
         }


         //       
         // ---- Case ---------------------------------------------------------
         //
         JNITestutils.tcase( "Local maximum of solar incidence angle at north" +
                             " pole of ALPHA; observer is SUN; abcorr = NONE" );

         angtyp = INCIDENCE;

         //
         // ALPHA orbits with constant angular velocity on a circular path,
         // so local maxima occur 1/2 period apart from local minima.
         //
         // Create a confinement window with 4 intervals.
         //
         
         cnfine = new SpiceWindow();

         for ( int i = 0;  i < 4;  i++ )
         {
            left  = et0.add (new TDBDuration( 3.5*SPD + (i*SPD*7) - 3600.0 ) );
            right = left.add(new TDBDuration( 2 * 3600.0 ) );

            cnfine.insert ( left.getTDBSeconds(), right.getTDBSeconds() );
         }


         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, obsrvr, spoint );

         //
         // Set up the constraint.
         //
         relate  = "LOCMAX";
         
         cons    = GFConstraint.createExtremumConstraint( relate );

         //
         // Run the search. Use a 10 hour step.
         //
         step   = 10.0 * 3600.0;

         result = search.run ( cnfine, cons, step, MAXIVL );

         //
         // Check the result window.
         //
         wncard = result.card();

         ok = JNITestutils.chcksi( "result cardinality", wncard, "=", 4, 0 );


         for ( int i = 0;  i < wncard;  i++ )
         {
            interval = result.getInterval( i );

            left     = new TDBTime( interval[0] );
            right    = new TDBTime( interval[1] );

            //
            //  Check event start time.
            //
            title    = String.format ( "Event %d start time", i );

            xTime    = et0.add(  new TDBDuration( (3.5 + (7*i)) * SPD )  );

            ok = JNITestutils.chcksd ( "event start",
                                       left.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
            //
            //  Check event stop time; this should equal the start time
            //  since we're searching for an extremum.
            //
            title    = String.format ( "Event %d stop time", i );

            ok = JNITestutils.chcksd ( "event stop",
                                       right.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
         }





         //       
         // ---- Case ---------------------------------------------------------
         //
         JNITestutils.tcase( "Local minimum of phase angle at -X axis of " +
                             "ALPHA; observer is BETA; abcorr = NONE"       );

         angtyp = PHASE;

         //
         // This time we want to use Beta as the observer. Note that
         // the surface point on Alpha's north pole won't do for this
         // computation.
         //
         // We're going to work around this problem by changing the
         // orientation of Alpha. We'll use the ALPHA_VIEW_XY frame as
         // Alpha's body-fixed frame. This is a two-vector dynamic frame
         // in which the +X axis points from the sun to Alpha at all times.
         // We'll create a sun-facing surface point on Alpha at the tip
         // of Alpha's -X axis.
         //

         target = new Body( "ALPHA" );
         obsrvr = new Body( "BETA"  );
         illum  = new Body( "SUN"   );

         //
         // Create the surface point on ALPHA.
         //
         radii  = target.getValues( "RADII" );

         spoint = new Vector3( -radii[2], 0.0, 0.0 );

         //
         // `fixref' is the ALPHA view frame with the X-Y plane 
         // coincident with that of the J2000 frame.
         //
         fixref = new ReferenceFrame( "ALPHA_VIEW_XY" );

         //
         // Set the confinement window to cover a single 4-day time interval.
         //
         cnfine = new SpiceWindow();

         et0    = new TDBTime( 0. );
         et1    = et0.add( new TDBDuration( 4 * SPD ) );
 
         cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );


         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, illum,
                                            fixref, abcorr, obsrvr, spoint );

         //
         // Set up the constraint.
         //
         relate  = "LOCMIN";
         
         cons    = GFConstraint.createExtremumConstraint( relate );

         //
         // Run the search. Use a 1 hour step.
         //
         step   = 3600.0;

         result = search.run ( cnfine, cons, step, MAXIVL );

         //
         // Check the result window.
         //
         // Note that the phase angle is zero when the beta-sun-alpha angle
         // is zero or 180 degrees, so there are two minima of the phase angle
         // per 24 hours.
         //
         wncard = result.card();

         ok = JNITestutils.chcksi( "result cardinality", wncard, "=", 8, 0 );


         for ( int i = 0;  i < wncard;  i++ )
         {
            interval = result.getInterval( i );

            left     = new TDBTime( interval[0] );
            right    = new TDBTime( interval[1] );

            //
            //  Check event start time.
            //
            title    = String.format ( "Event %d start time", i );

            xTime    = new TDBTime ( 300.0  +  ( i * SPD / 2 )  );

            ok = JNITestutils.chcksd ( "event start",
                                       left.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
            //
            //  Check event stop time; this should equal the start time
            //  since we're searching for an extremum.
            //
            title    = String.format ( "Event %d stop time", i );

            ok = JNITestutils.chcksd ( "event stop",
                                       right.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
         }


         //       
         // ---- Case ---------------------------------------------------------
         //
         JNITestutils.tcase( "Repeat the previous test with the "   +
                             "illumination source set to BETA and " +
                             "the observer set to SUN."              );

         angtyp = PHASE;

         //
         // Switch observer and illumination source in the search constructor
         // call.
         //
         search = 

             new GFIlluminationAngleSearch( method, angtyp, target, obsrvr,
                                            fixref, abcorr, illum,  spoint );

         result = search.run ( cnfine, cons, step, MAXIVL );

         //
         // Check the result window.
         //
         // Note that the phase angle is zero when the beta-sun-alpha angle
         // is zero or 180 degrees, so there are two minima of the phase angle
         // per 24 hours.
         //
         wncard = result.card();

         ok = JNITestutils.chcksi( "result cardinality", wncard, "=", 8, 0 );


         for ( int i = 0;  i < wncard;  i++ )
         {
            interval = result.getInterval( i );

            left     = new TDBTime( interval[0] );
            right    = new TDBTime( interval[1] );

            //
            //  Check event start time.
            //
            title    = String.format ( "Event %d start time", i );

            xTime    = new TDBTime ( 300.0  +  ( i * SPD / 2 )  );

            ok = JNITestutils.chcksd ( "event start",
                                       left.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
            //
            //  Check event stop time; this should equal the start time
            //  since we're searching for an extremum.
            //
            title    = String.format ( "Event %d stop time", i );

            ok = JNITestutils.chcksd ( "event stop",
                                       right.getTDBSeconds(),
                                       "~",
                                       xTime.getTDBSeconds(),
                                       TIMTOL                 );
         }



         /*
         *********************************************************************
         *
         *    Comprehensive cases using comparisons against alternate
         *    computations
         *
         *********************************************************************
         */

 
         /*
         *********************************************************************
         *
         *    PHASE angle tests
         *
         *********************************************************************
         */ 
     
         /*
         We'use use the earth, moon, and sun as the three participating
         bodies. The surface point will be the OCTL telescope; we'll
         create an SPK file for this point. We also need an FK for a
         topocentric frame centered at the point.

         Though it's not strictly necessary, we'll use real data for
         these kernels, with one exception: we'll use the terrestrial
         reference frame IAU_EARTH rather than ITRF93.

         The original reference frame specifications follow:


            Topocentric frame OCTL_TOPO

               The Z axis of this frame points toward the zenith.
               The X axis of this frame points North.

               Topocentric frame OCTL_TOPO is centered at the site OCTL
               which has Cartesian coordinates

                  X (km):                 -0.2448937761729E+04
                  Y (km):                 -0.4667935793438E+04
                  Z (km):                  0.3582748499430E+04

               and planetodetic coordinates

                  Longitude (deg):      -117.6828380000000
                  Latitude  (deg):        34.3817491000000
                  Altitude   (km):         0.2259489999999E+01

               These planetodetic coordinates are expressed relative to
               a reference spheroid having the dimensions

                  Equatorial radius (km):  6.3781400000000E+03
                  Polar radius      (km):  6.3567523100000E+03
   
               All of the above coordinates are relative to the frame
               ITRF93.
         */


         //       
         // ---- Case ---------------------------------------------------------
         //
         JNITestutils.tcase( "Create OCTL kernels." );
 
         /*
         This isn't a test, but we'll call it that so we'll have
         a meaningful label in any error messages that arise.
         */

         /*
         As mentioned, we go with a frame that's more convenient than
         ITRF93:
         */
         fixref = new ReferenceFrame( "IAU_EARTH" );

         /*
         Prepare a frame kernel in a String array. 
         */
         String[] txtptr = new String[25];

         txtptr[ 0] = "FRAME_OCTL_TOPO            =  1398962";
         txtptr[ 1] = "FRAME_1398962_NAME         =  'OCTL_TOPO' ";
         txtptr[ 2] = "FRAME_1398962_CLASS        =  4";
         txtptr[ 3] = "FRAME_1398962_CLASS_ID     =  1398962";
         txtptr[ 4] = "FRAME_1398962_CENTER       =  398962";

         txtptr[ 5] = "OBJECT_398962_FRAME        =  'OCTL_TOPO' ";

         txtptr[ 6] = "TKFRAME_1398962_RELATIVE   =  'IAU_EARTH' ";
         txtptr[ 7] = "TKFRAME_1398962_SPEC       =  'ANGLES' ";
         txtptr[ 8] = "TKFRAME_1398962_UNITS      =  'DEGREES' ";
         txtptr[ 9] = "TKFRAME_1398962_AXES       =  ( 3, 2, 3 )";
         txtptr[10] = "TKFRAME_1398962_ANGLES     =  ( -242.3171620000000,";
         txtptr[11] = "                                 -55.6182509000000,";
         txtptr[12] = "                                 180.0000000000000  )";
         txtptr[13] = "NAIF_BODY_NAME            +=  'OCTL' ";
         txtptr[14] = "NAIF_BODY_CODE            +=  398962";

         /*
         It will be convenient to have a version of this frame that
         has the +Z axis pointed down instead of up.
         */
         txtptr[15] = "FRAME_OCTL_FLIP            =  2398962";
         txtptr[16] = "FRAME_2398962_NAME         =  'OCTL_FLIP' ";
         txtptr[17] = "FRAME_2398962_CLASS        =  4";
         txtptr[18] = "FRAME_2398962_CLASS_ID     =  2398962";
         txtptr[19] = "FRAME_2398962_CENTER       =  398962";

         txtptr[20] = "TKFRAME_2398962_RELATIVE   =  'OCTL_TOPO' ";
         txtptr[21] = "TKFRAME_2398962_SPEC       =  'ANGLES' ";
         txtptr[22] = "TKFRAME_2398962_UNITS      =  'DEGREES' ";
         txtptr[23] = "TKFRAME_2398962_AXES       =  ( 3, 2, 3 )";
         txtptr[24] = "TKFRAME_2398962_ANGLES     =  ( 0, 180.0, 0 ) ";
   
         //
         // Load the frame definitions into the kernel pool. 
         //
         KernelPool.loadFromBuffer( txtptr );



         /*
         Now create an SPK file containing a type 9 segment for OCTL.

         Note: the data type is 8 for other SPICE language versions, but
         type 8 writing support is not currently available.

         Delete the SPK file first if it already exists.
         */
         ( new File( OCTLSPK ) ).delete();

         SPK octlSPK = SPK.openNew( OCTLSPK, OCTLSPK, 0 );
 

         /*
         The first OCTL position component:
         */
         spoint = new Vector3 ( -0.2448937761729e4,
                                -0.4667935793438e4,
                                 0.3582748499430e4 );
         /*
         We're going to use a version of the OCTL position
         that has zero altitude relative to the earth's
         reference ellipsoid. This is done to enable
         consistency checks to be done using GFPOSC.

         For compatibility with the topocentric frame
         specification above, we'll use the following
         earth radii:
         */
         radii[0]  = 6.3781400000000e+03;
         radii[1]  = 6.3781400000000e+03;
         radii[2]  = 6.3567523100000e+03;

         KernelPool.putDouble ( "BODY399_RADII", radii );

         Ellipsoid earthShape = new Ellipsoid ( radii[0], radii[1], radii[2] );

         Vector3 np = 
            ( new EllipsoidPointNearPoint(earthShape, spoint) ).getNearPoint();

         //
         // Overwrite `spoint' with the near point on the reference 
         // ellipsoid.
         //
         spoint = np;
 
         /*
         The second position matches the first: we don't model
         plate motion.
         */ 
         StateVector[] states = new StateVector[2];

         Vector3 OCTLVelocity = new Vector3();

         for ( int i = 0;  i < 2;  i++ )
         {
            states[i] = new StateVector( spoint, OCTLVelocity );
         }

         /*
         Time bounds for the segment:
         */
         TDBTime first = new TDBTime    (  ( -50 ) * SPD * 365.25  );
         step          = 100 * SPD * 365.25;

         TDBTime last  = first.add(  new TDBDuration( step - 1.e-6 )  );

         Time[]  epochs = { first, last };
 
         //System.out.println ( epochs[0] );

         /*
         Write the segment.
         */   
         Body EARTH = new Body( "Earth" );

         octlSPK.writeType09Segment( OCTL,  EARTH,  fixref, 
                                     first, last,   "octl",  1,      
                                     2,     states, epochs      );
         //
         // Close the SPK file.
         //
         octlSPK.close();


         /*
         *********************************************************************
         *
         *    PHASE angle tests
         *
         *********************************************************************
         */ 

         
         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Phase angle test setup" );

         //
         // Phase angle tests: we'll compare results from 
         // GFIlluminationAngleSearch to those obtained using 
         // GFPhaseAngleSearch. Note that the surface point must be
         // an ephemeris object in order to carry out these tests.
         //
         angtyp = " Phase";

         illum  = new Body( "Sun"   );
         target = new Body( " 399"  );
         obsrvr = new Body( " moon" );
         fixref = new ReferenceFrame( "IAU_EARTH" );
      
         //
         // Load SPKs required for this test case.
         //
         KernelDatabase.load ( OCTLSPK );    
         KernelDatabase.load ( SPK2    );    

         //
         // Set up the confinement window.
         //
         time0 = "2011 JAN 1";

         et0   = new TDBTime( time0 );

         //
         // Search over approximately two months.
         //
         et1    =  et0.add(  new TDBDuration( 60 * SPD )  );

         cnfine = new SpiceWindow();

         cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );
 
         //
         // Use a 10 day step. We expect the extrema to
         // be 14 days apart.
         //
         step   = 10.0 * SPD;


         //
         // Loop over all operators and aberration corrections.
         //
         int NREL  = relops.length;
         int NCORR = corrs.length;

         for ( int opidx = 0;  opidx < NREL;  opidx++ )
         {
            //
            // Convert the reference value to radians.
            //
            relate = relops [ opidx ];
            refval = vals   [ opidx ] * RPD;
            adjust = adj    [ opidx ];

            //
            // Create the search constraint.
            //
            if ( opidx < 3 )
            {
               //
               // The operator is binary.
               //
               cons = GFConstraint.createReferenceConstraint ( relate, refval );
            }
            else
            {
               //
               // The constraint is of extremum type.
               //
               if ( adjust == 0.0 ) 
               {
                  cons = GFConstraint.createExtremumConstraint ( relate );
               }
               else
               {
                  cons = GFConstraint.createExtremumConstraint(relate, adjust);
               }
            }

            for ( int coridx = 0;  coridx < NCORR;  coridx++ )
            {
               abcorr = new AberrationCorrection( corrs[coridx] );

               //
               // ---- Case ---------------------------------------------------
               //
               title = String.format( 
                          "Phase angle search: RELATE = \"%s\"; "            +
                          "REFVAL (deg) = %f; AJDUST = %e; ABCORR = %s; "    +
                          "observer = %s; target = %s; illum source = %s; "  +
                          "FIXREF = %s; SPOINT = ( %e %e %e ).",
                          relate,
                          vals[opidx],
                          adjust,
                          abcorr.toString(),
                          obsrvr.getName(),
                          target.getName(),
                          illum.getName(),
                          fixref.getName(),
                          spoint.getElt(0),  
                          spoint.getElt(1),  
                          spoint.getElt(2)     );
  
               JNITestutils.tcase ( title );
 
               //
               // Find the expected result window. Note that the
               // target is OCTL in this case.
               // 
               
               phsearch =

                  new GFPhaseAngleSearch ( OCTL, illum, abcorr, obsrvr );

               xrsult = phsearch.run ( cnfine, cons, step, MAXIVL );
         
               //
               // Search for the condition of interest using 
               // GFIlluminationAngleSearch. The angle type is 
               //
               //    PHASE 
               //
               // (this angle type was set near the start of the
               // test case)
               //
               method = ELLIPSOID;

               search = 

                  new GFIlluminationAngleSearch( method, angtyp, target, 
                                                 illum,  fixref, abcorr,
                                                 obsrvr, spoint         );

               result = search.run ( cnfine, cons, step, MAXIVL );

               //
               // Compare result window cardinalities first.
               // 
               xn = xrsult.card();

               //
               // Test family validity check: we're not supposed to
               // have any cases where the results are empty.
               // 
               ok = JNITestutils.chcksi( "xn", xn, ">", 0, 0 );

               n  = result.card(); 

               ok = JNITestutils.chcksi( "n", n, "=", xn, 0 );


               if ( ok ) 
               {
                  //
                  // Compare result window interval bounds. 
                  // 
                  ok = JNITestutils.chckad ( "result", 
                                             result.toArray(), 
                                             "~", 
                                             xrsult.toArray(),
                                             LOOSE           );
                  if ( ok )
                  {
                     //
                     // The result window found by GFIlluminationAngleSearch 
                     // agrees with that found by GFPositionCoordinateSearch. 
                     // Check the actual angular values at the window endpoints
                     // for the cases where the operator is
                     // binary.
                     //

                     if ( CSPICE.eqstr( relate, "=" ) )
                     {
                        //
                        // Check the emission angle at each interval
                        // endpoint.
                        //
                        for ( int i = 0;  i < n;  i++ )
                        {
                           interval = result.getInterval( i );

                           left   = new TDBTime( interval[0] );
                           right  = new TDBTime( interval[1] );
  
                           title  = String.format( 
                                      "Angle at start of interval %d", i );
                                    
                           illumAngles = new IlluminationAngles ( 

                                            method, target, left,  fixref, 
                                            abcorr, obsrvr, spoint         );

                           ok = JNITestutils.chcksd ( title, 
                                             illumAngles.getPhaseAngle(), 
                                             "~", 
                                             refval, 
                                             ANGTOL                        );
 
                           title  = String.format( 
                                      "Angle at end of interval %d", i );
                                    
                           illumAngles = new IlluminationAngles ( 

                                            method, target, right, fixref, 
                                            abcorr, obsrvr, spoint         );

                           ok = JNITestutils.chcksd ( title, 
                                             illumAngles.getPhaseAngle(), 
                                             "~", 
                                             refval, 
                                             ANGTOL                        );
                        }
                     }

                     
                     else if (    CSPICE.eqstr( relate, "<" )               
                               || CSPICE.eqstr( relate, ">" )  )
                     {
                        //
                        // Perform checks only at interval endpoints
                        // contained in the interior of the confinement
                        // window. At the confinement window boundaries,
                        // the inequality may be satisfied without the
                        // value being equal to the reference value.
                        //
                        for ( int i = 0;  i < n;  i++ )
                        {
                           interval = result.getInterval( i );

                           left   = new TDBTime( interval[0] );
                           right  = new TDBTime( interval[1] );
  
                           if ( left.getTDBSeconds() > et0.getTDBSeconds() )
                           {
                              title  = String.format( 
                                      "Angle at start of interval %d", i );
                                    
                              illumAngles = new IlluminationAngles ( 

                                            method, target, left,  fixref, 
                                            abcorr, obsrvr, spoint         );

                              ok = JNITestutils.chcksd ( title, 
                                             illumAngles.getPhaseAngle(), 
                                             "~", 
                                             refval, 
                                             ANGTOL                        );
                           }

                           if ( right.getTDBSeconds() < et1.getTDBSeconds() )
                           {
                              title  = String.format( 
                                      "Angle at end of interval %d", i );
                                    
                              illumAngles = new IlluminationAngles ( 

                                            method, target, right, fixref, 
                                            abcorr, obsrvr, spoint         );

                              ok = JNITestutils.chcksd ( title, 
                                             illumAngles.getPhaseAngle(), 
                                             "~", 
                                             refval, 
                                             ANGTOL                        );
                           }
                        }
                     }
                     //
                     // End of emission angle checks.
                     //
                  }
                  //
                  // End of IF block for successful RESULT check.
                  //
               }
               //
               // End of interval endpoint checks.
               //
            }
            //
            // End of aberration correction loop.
            //
         }
         //
         // End of operator loop.
         //


         //
         // Unload SPKs required for this test case.
         //
         KernelDatabase.unload ( OCTLSPK );    
         KernelDatabase.unload ( SPK2    );    


         /*
         *********************************************************************
         *
         *    EMISSION angle tests
         *
         *********************************************************************
         */ 

         /*
         Emission angle tests: we'll compare results from
         GFIlluminationAngleSearch to those obtained using 
         GFPositionCoordinateSearch. Note that the surface point must be
         an ephemeris object having an associated topocentric frame
         in order to carry out these tests.
   
         The emission angle is the supplement of the colatitude,
         measured in the surface point topocentric frame, of
         the observer-surface point vector. Equivalently, the emission
         angle is the colatitude of the observer-surface point vector
         in the "flip" frame, which has its +Z axis pointing downward.

         We can use GFPositionCoordinateSearch to find times when this 
         colatitude, measured in the flip frame, meets conditions on 
         the emission angle.
         */

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Emission angle test setup" );

         //
         // Load SPKs required for this test case.
         //
         KernelDatabase.load ( OCTLSPK );    
      
         CSPICE.spkuef       ( han2 );
         KernelDatabase.load ( SPK2 );


         angtyp = EMISSION;
         illum  = new Body( "Sun" );
         target = EARTH;
         obsrvr = new Body( "Moon" );
         fixref = new ReferenceFrame( "IAU_EARTH" );
    
         ReferenceFrame srfref = new ReferenceFrame( "OCTL_FLIP" );

         String corsys = "Spherical";
         String coord  = " colatitude";


         //
         // Set up the confinement window.
         //
         time0 = "2011 JAN 1";

         et0   = new TDBTime( time0 );

         //
         // For the emission angle test, we don't need to use
         // a month-long confinement window. A few days is enough.
         //
         et1    =  et0.add(  new TDBDuration( 3 * SPD )  );

         cnfine = new SpiceWindow();

         cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );
 
         //
         // Use a 6 hour step. We expect the extrema to
         // be 12+delta hours apart, where delta may be
         // a few hours.
         //
         step   = 6.0 * 3600.0;

         //
         // Loop over all operators and aberration corrections.
         //
         for ( int opidx = 0;  opidx < NREL;  opidx++ )
         {
            //
            // Convert the reference value to radians.
            //
            relate = relops [ opidx ];
            refval = vals   [ opidx ] * RPD;
            adjust = adj    [ opidx ];

            //
            // Create the search constraint.
            //
            if ( opidx < 3 )
            {
               //
               // The operator is binary.
               //
               cons = GFConstraint.createReferenceConstraint ( relate, refval );
            }
            else
            {
               //
               // The constraint is of extremum type.
               //
               if ( adjust == 0.0 ) 
               {
                  cons = GFConstraint.createExtremumConstraint ( relate );
               }
               else
               {
                  cons = GFConstraint.createExtremumConstraint(relate, adjust);
               }
            }

            for ( int coridx = 0;  coridx < NCORR;  coridx++ )
            {
               abcorr = new AberrationCorrection( corrs[coridx] );

               //
               // ---- Case ---------------------------------------------------
               //
               title = String.format( 
                          "Emission angle search: RELATE = \"%s\"; "         +
                          "REFVAL (deg) = %f; AJDUST = %e; ABCORR = %s; "    +
                          "observer = %s; target = %s; illum source = %s; "  +
                          "FIXREF = %s; SPOINT = ( %e %e %e ).",
                          relate,
                          vals[opidx],
                          adjust,
                          abcorr.toString(),
                          obsrvr.getName(),
                          target.getName(),
                          illum.getName(),
                          fixref.getName(),
                          spoint.getElt(0),  
                          spoint.getElt(1),  
                          spoint.getElt(2)     );
  
               JNITestutils.tcase ( title );
 
               //
               // Find the expected result window. Note that the
               // target is OCTL in this case.
               // 
               

               pcsearch =

                  new GFPositionCoordinateSearch ( OCTL,   srfref, abcorr, 
                                                   obsrvr, corsys, coord  );

               xrsult = pcsearch.run ( cnfine, cons, step, MAXIVL );


         
               //
               // Search for the condition of interest using 
               // GFIlluminationAngleSearch. The angle type is 
               //
               //    EMISSION 
               //
               // (this angle type was set near the start of the
               // test case)
               //
               method = ELLIPSOID;

               search = 

                  new GFIlluminationAngleSearch( method, angtyp, target, 
                                                 illum,  fixref, abcorr,
                                                 obsrvr, spoint         );

               result = search.run ( cnfine, cons, step, MAXIVL );

               //
               // Compare result window cardinalities first.
               // 
               xn = xrsult.card();

               //
               // Test family validity check: we're not supposed to
               // have any cases where the results are empty.
               // 
               ok = JNITestutils.chcksi( "xn", xn, ">", 0, 0 );

               n  = result.card(); 

               ok = JNITestutils.chcksi( "n", n, "=", xn, 0 );


               if ( ok ) 
               {
                  //
                  // Compare result window interval bounds. 
                  // 
                  ok = JNITestutils.chckad ( "result", 
                                             result.toArray(), 
                                             "~", 
                                             xrsult.toArray(),
                                             LOOSE           );
                  if ( ok )
                  {
                     //
                     // The result window found by GFIlluminationAngleSearch 
                     // agrees with that found by GFPositionCoordinateSearch. 
                     // Check the actual angular values at the window endpoints
                     // for the cases where the operator is
                     // binary.
                     //

                     if ( CSPICE.eqstr( relate, "=" ) )
                     {
                        //
                        // Check the emission angle at each interval
                        // endpoint.
                        //
                        for ( int i = 0;  i < n;  i++ )
                        {
                           interval = result.getInterval( i );

                           left   = new TDBTime( interval[0] );
                           right  = new TDBTime( interval[1] );
  
                           title  = String.format( 
                                      "Angle at start of interval %d", i );
                                    
                           illumAngles = new IlluminationAngles ( 

                                            method, target, left,  fixref, 
                                            abcorr, obsrvr, spoint         );

                           ok = JNITestutils.chcksd ( title, 
                                             illumAngles.getEmissionAngle(), 
                                             "~", 
                                             refval, 
                                             ANGTOL                        );
 
                           title  = String.format( 
                                      "Angle at end of interval %d", i );
                                    
                           illumAngles = new IlluminationAngles ( 

                                            method, target, right, fixref, 
                                            abcorr, obsrvr, spoint         );

                           ok = JNITestutils.chcksd ( title, 
                                             illumAngles.getEmissionAngle(), 
                                             "~", 
                                             refval, 
                                             ANGTOL                        );
                        }
                     }

                     
                     else if (    CSPICE.eqstr( relate, "<" )               
                               || CSPICE.eqstr( relate, ">" )  )
                     {
                        //
                        // Perform checks only at interval endpoints
                        // contained in the interior of the confinement
                        // window. At the confinement window boundaries,
                        // the inequality may be satisfied without the
                        // value being equal to the reference value.
                        //
                        for ( int i = 0;  i < n;  i++ )
                        {
                           interval = result.getInterval( i );

                           left   = new TDBTime( interval[0] );
                           right  = new TDBTime( interval[1] );
  
                           if ( left.getTDBSeconds() > et0.getTDBSeconds() )
                           {
                              title  = String.format( 
                                      "Angle at start of interval %d", i );
                                    
                              illumAngles = new IlluminationAngles ( 

                                            method, target, left,  fixref, 
                                            abcorr, obsrvr, spoint         );

                              ok = JNITestutils.chcksd ( title, 
                                             illumAngles.getEmissionAngle(), 
                                             "~", 
                                             refval, 
                                             ANGTOL                        );
                           }

                           if ( right.getTDBSeconds() < et1.getTDBSeconds() )
                           {
                              title  = String.format( 
                                      "Angle at end of interval %d", i );
                                    
                              illumAngles = new IlluminationAngles ( 

                                            method, target, right, fixref, 
                                            abcorr, obsrvr, spoint         );

                              ok = JNITestutils.chcksd ( title, 
                                             illumAngles.getEmissionAngle(), 
                                             "~", 
                                             refval, 
                                             ANGTOL                        );
                           }
                        }
                     }
                     //
                     // End of emission angle checks.
                     //
                  }
                  //
                  // End of IF block for successful RESULT check.
                  //
               }
               //
               // End of interval endpoint checks.
               //
            }
            //
            // End of aberration correction loop.
            //
         }
         //
         // End of operator loop.
         //




         /*
         *********************************************************************
         *
         *    INCIDENCE angle tests
         *
         *********************************************************************
         */ 

         /*
         Incidence angle tests: we'll compare results from 
         GFIlluminationAngleSearch to those obtained using 
         GFPositionCoordinateSearch, where in the latter case, the
         surface point is treated as the observer. Results obtained using
         GFIlluminationAngleSearch must have one-way observer-surface point 
         light time subtracted in order to be comparable to those from
         GFPositionCoordinateSearch.

         Note that the surface point must be an ephemeris object having an
         associated topocentric frame in order to carry out these tests.

         In the geometric case, the solar incidence angle is the
         colatitude, measured in the surface point topocentric frame, of
         the surface point-sun vector. When aberration corrections are
         used, the surface point-sun vector must be computed at an epoch
         corrected for observer-surface point light time.

         We can use GFPositionCoordinateSearch to find times when this 
         colatitude, measured in the OCTL topocentric frame, meets 
         conditions on the solar incidence angle.
         */


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Solar incidence angle test setup" );

         //
         // SPKs required for this test case are already loaded.
         //
         // KernelDatabase.load ( OCTLSPK );    
      
         // CSPICE.spkuef       ( han2 );
         // KernelDatabase.load ( SPK2 );

         angtyp = INCIDENCE;

         obsrvr = new Body( "MOON"  );
         target = new Body( "EARTH" );
         illum  = new Body( "SUN"   );

         fixref = new ReferenceFrame( "IAU_EARTH" );
         srfref = new ReferenceFrame( "OCTL_TOPO" );

         corsys = "SPHERICAL";
         coord  = "COLATITUDE";

         //
         // Set up the confinement window.
         //
         time0 = "2011 JAN 1";

         et0   = new TDBTime( time0 );

         //
         // For the emission angle test, we don't need to use
         // a month-long confinement window. A few days is enough.
         //
         et1    =  et0.add(  new TDBDuration( 3 * SPD )  );

         cnfine = new SpiceWindow();

         cnfine.insert( et0.getTDBSeconds(), et1.getTDBSeconds() );
 
         //
         // Use a 6 hour step. We expect the extrema to
         // be 12+delta hours apart, where delta may be
         // a few hours.
         //
         step   = 6.0 * 3600.0;

         //
         // Loop over all operators and aberration corrections.
         //
         for ( int opidx = 0;  opidx < NREL;  opidx++ )
         {
            //
            // Convert the reference value to radians.
            //
            relate = relops [ opidx ];
            refval = vals   [ opidx ] * RPD;
            adjust = adj    [ opidx ];

            //
            // Create the search constraint.
            //
            if ( opidx < 3 )
            {
               //
               // The operator is binary.
               //
               cons = GFConstraint.createReferenceConstraint ( relate, refval );
            }
            else
            {
               //
               // The constraint is of extremum type.
               //
               if ( adjust == 0.0 ) 
               {
                  cons = GFConstraint.createExtremumConstraint ( relate );
               }
               else
               {
                  cons = GFConstraint.createExtremumConstraint(relate, adjust);
               }
            }

            for ( int coridx = 0;  coridx < NCORR;  coridx++ )
            {
               abcorr = new AberrationCorrection( corrs[coridx] );

               //
               // ---- Case ---------------------------------------------------
               //
               title = String.format( 
                          "Solar incidence angle search: RELATE = \"%s\"; "  +
                          "REFVAL (deg) = %f; AJDUST = %e; ABCORR = %s; "    +
                          "observer = %s; target = %s; illum source = %s; "  +
                          "FIXREF = %s; SPOINT = ( %e %e %e ).",
                          relate,
                          vals[opidx],
                          adjust,
                          abcorr.toString(),
                          obsrvr.getName(),
                          target.getName(),
                          illum.getName(),
                          fixref.getName(),
                          spoint.getElt(0),  
                          spoint.getElt(1),  
                          spoint.getElt(2)     );
  
               JNITestutils.tcase ( title );


               //
               // Unless we're in the geometric case, we'll need an
               // adjusted version of the confinement window for the
               // GF position coordinate search. This window will have
               // its endpoints adjusted for the light time between the
               // observer and surface point.
               //         
               if ( abcorr.hasLightTime() )
               {
                  //
                  // Find the adjusted confinement window endpoints. 
                  //
                  TDBInterval    = new TDBTime[2];
                  TDBInterval[0] = et0;
                  TDBInterval[1] = et1;

                  for ( int i = 0;  i < 2;  i++ )
                  {
                     pr = 

                        new PositionRecord( OCTL,   TDBInterval[i], J2000, 
                                            abcorr, obsrvr                );

                     lt = pr.getLightTime();

                     if ( abcorr.isReceptionType() )
                     {
                        lt = lt.negate();
                     }

                     modET[i] = TDBInterval[i].add( lt );
                  }                           

                  modcnf = new SpiceWindow();

                  modcnf.insert ( modET[0].getTDBSeconds(), 
                                  modET[1].getTDBSeconds() );
               }
               else
               {
                  modcnf = cnfine;
               }
 
               //
               // Find the expected result window. Note that the
               // target is the illumination source and the observer
               // is the surface point in this case.
               //                
               pcsearch =

                  new GFPositionCoordinateSearch ( illum, srfref, abcorr, 
                                                   OCTL,  corsys, coord  );

               xrsult = pcsearch.run ( modcnf, cons, step, MAXIVL );

         
               //
               // Search for the condition of interest using 
               // GFIlluminationAngleSearch. The angle type is 
               //
               //    INCIDENCE 
               //
               // (this angle type was set near the start of the
               // test case)
               //
               method = ELLIPSOID;

               search = 

                  new GFIlluminationAngleSearch( method, angtyp, target, 
                                                 illum,  fixref, abcorr,
                                                 obsrvr, spoint         );

               result = search.run ( cnfine, cons, step, MAXIVL );

               //
               // Compare result window cardinalities first.
               // 
               xn = xrsult.card();

               //
               // Test family validity check: we're not supposed to
               // have any cases where the results are empty.
               // 
               ok = JNITestutils.chcksi( "xn", xn, ">", 0, 0 );

               n  = result.card(); 

               ok = JNITestutils.chcksi( "n", n, "=", xn, 0 );

               if ( !ok ) { return ok; }

               if ( ok ) 
               {
                  //
                  // Compare result window interval bounds. 
                  // 
                  // If light time corrections are used, we must
                  // adjust the elements of `result' for the one-way
                  // light time between the observer and surface point.
                  //
                  if ( abcorr.hasLightTime() )
                  {
                     //
                     // Find the adjusted result window endpoints. 
                     //
                     modResultArray = new double[n][2];

                     for ( int i = 0;  i < n;  i++ )
                     {
                        interval = result.getInterval(i);

                        for ( int j = 0;  j < 2;  j++ )
                        {
                           et = new TDBTime( interval[j] );

                           pr = new PositionRecord( OCTL,   et,    J2000, 
                                                    abcorr, obsrvr       );

                           lt = pr.getLightTime();

                           if ( abcorr.isReceptionType() )
                           {
                              lt = lt.negate();
                           }

                           //System.out.println ( "lt = " + lt.getMeasure() );

                           modResultArray[i][j] = 

                              interval[j] + lt.getMeasure();
                        }
                     }
                    
                     modres = new SpiceWindow( modResultArray );
                  }                           
                  else
                  {
                     modres = result;
                  }


                  ok = JNITestutils.chckad ( "modified result", 
                                             modres.toArray(), 
                                             "~", 
                                             xrsult.toArray(),
                                             LOOSE           );              
                  if ( ok )
                  {
                     //
                     // The result window found by GFIlluminationAngleSearch 
                     // agrees with that found by GFPositionCoordinateSearch. 
                     // Check the actual angular values at the window endpoints
                     // for the cases where the operator is
                     // binary.
                     //

                     if ( CSPICE.eqstr( relate, "=" ) )
                     {
                        //
                        // Check the emission angle at each interval
                        // endpoint.
                        //
                        for ( int i = 0;  i < n;  i++ )
                        {
                           interval = result.getInterval( i );

                           left   = new TDBTime( interval[0] );
                           right  = new TDBTime( interval[1] );
  
                           title  = String.format( 
                                      "Angle at start of interval %d", i );
                                    
                           illumAngles = new IlluminationAngles ( 

                                            method, target, left,  fixref, 
                                            abcorr, obsrvr, spoint         );

                           ok = JNITestutils.chcksd ( title, 
                                           illumAngles.getSolarIncidenceAngle(),
                                           "~", 
                                           refval, 
                                           ANGTOL                        );
 
                           title  = String.format( 
                                      "Angle at end of interval %d", i );
                                    
                           illumAngles = new IlluminationAngles ( 

                                            method, target, right, fixref, 
                                            abcorr, obsrvr, spoint         );

                           ok = JNITestutils.chcksd ( title, 
                                           illumAngles.getSolarIncidenceAngle(),
                                           "~", 
                                           refval, 
                                           ANGTOL                        );
                        }
                     }

                     
                     else if (    CSPICE.eqstr( relate, "<" )          
                               || CSPICE.eqstr( relate, ">" )  )
                     {
                        //
                        // Perform checks only at interval endpoints
                        // contained in the interior of the confinement
                        // window. At the confinement window boundaries,
                        // the inequality may be satisfied without the
                        // value being equal to the reference value.
                        //
                        for ( int i = 0;  i < n;  i++ )
                        {
                           interval = result.getInterval( i );

                           left   = new TDBTime( interval[0] );
                           right  = new TDBTime( interval[1] );
  
                           if ( left.getTDBSeconds() > et0.getTDBSeconds() )
                           {
                              title  = String.format( 
                                      "Angle at start of interval %d", i );
                                    
                              illumAngles = new IlluminationAngles ( 

                                          method, target, left,  fixref, 
                                          abcorr, obsrvr, spoint         );

                              ok = JNITestutils.chcksd ( title, 
                                           illumAngles.getSolarIncidenceAngle(),
                                           "~", 
                                           refval, 
                                           ANGTOL                        );
                           }

                           if ( right.getTDBSeconds() < et1.getTDBSeconds() )
                           {
                              title  = String.format( 
                                      "Angle at end of interval %d", i );
                                    
                              illumAngles = new IlluminationAngles ( 

                                           method, target, right, fixref, 
                                           abcorr, obsrvr, spoint         );

                              ok = JNITestutils.chcksd ( title, 
                                           illumAngles.getSolarIncidenceAngle(),
                                           "~", 
                                           refval, 
                                           ANGTOL                        );
                           }
                        }
                     }
                     //
                     // End of incidence angle checks.
                     //
                  }
                  //
                  // End of IF block for successful RESULT check.
                  //
               }
               //
               // End of interval endpoint checks.
               //
            }
            //
            // End of aberration correction loop.
            //
         }
         //
         // End of operator loop.
         //


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
         KernelDatabase.clear();

         CSPICE.spkuef( han1 );
         CSPICE.spkuef( han2 );

         ( new File ( SPK1 ) ).delete();

         ( new File ( SPK2 ) ).delete();

         ( new File ( OCTLSPK ) ).delete();
      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

