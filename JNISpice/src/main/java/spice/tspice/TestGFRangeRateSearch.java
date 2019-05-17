
package spice.tspice;

import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.GFConstraint.*;
import static spice.basic.TimeConstants.*;


/**
Class TestGFRangeRateSearch provides methods that implement
test families for the class GFRangeRateSearch.

<h3>Version 1.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 11-MAR-2014 (NJB) (EDW)</h3>
*/
public class TestGFRangeRateSearch extends Object
   {

   //
   // Class constants
   //
   private static String  PCK           = "gfrr.pck";
   private static String  SPK           = "gfrr.bsp";
   private static String  NATPCK        = "nat.tpc";
   private static String  NATSPK        = "nat.bsp";


   //
   // Methods
   //

   /**
   Test GFRangeRateSearch and associated classes.
   */
   public static boolean f_GFRangeRateSearch() throws SpiceException
      {
      //
      // Constants
      //

      final int                         MAXIVL = 5000;
      final int                         MAXWIN = 2 * MAXIVL;
      final int                         NCORR  = 9;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              observer;
      Body                              target;

      GFConstraint                      cons;

      GFRangeRateSearch                 search;

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

      double                            et0;
      double                            et1;
      double                            step;

      boolean                           ok;

      double                            adjust;
      double                            refval;

      int                               handle = 0;
      int                               i;
      int                               nathan = 0;

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

        //
        // Begin every test family with an open call.
        //
        JNITestutils.topen ( "f_GFRangeRateSearch" );


        JNITestutils.tcase ( "Setup: create and load SPK, PCK, LSK files." );


         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();

         JNITestutils.tstlsk();


         //
         // Create needed test kernels: leapseconds, PCK, and SPK. Delete
         // existing kernels of the same name if they exist.
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


         //
         // Error cases
         //

         //
         // Case 1
         //
         JNITestutils.tcase ( "Non-positive step size."  );

         try
            {
            cnfine         = new SpiceWindow();
            int    ndays   = 90;
            et0            = 0.;
            et1            = ndays * SPD;

            cnfine  = cnfine.insert( et0, et1 );

            //
            // Set up the search geometry parameters.
            //
            target           = new Body( "moon"  );
            abcorr           = new AberrationCorrection( "none" );
            observer         = new Body( "earth" );

            search   = new GFRangeRateSearch( target, abcorr, observer   );
            step     = -1.0;
            refval   = 0;

            //
            // Set up the constraint.
            //
            adjust   = 0;
            cons     = GFConstraint.createReferenceConstraint( ">", refval );

            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXWIN );

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
         // Case 2
         //
         JNITestutils.tcase ( "Negative adjustment value."  );

         try
            {

            //
            // Set up the constraint.
            //
            adjust   = -1;
            cons     = 
            GFConstraint.createExtremumConstraint( ADJUSTED_ABSMAX, adjust );

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
         // Case 3
         //
         JNITestutils.tcase ( "Ephemeris data unavailable."  );

         try
            {
            cnfine         = new SpiceWindow();
            int    ndays   = 90;
            et0            = 0.;
            et1            = ndays * SPD;

            cnfine  = cnfine.insert( et0, et1 );

            //
            // Set up the search geometry parameters.
            //
            target           = new Body( "dawn"  );
            abcorr           = new AberrationCorrection( "none" );
            observer         = new Body( "sun" );

            search   = new GFRangeRateSearch( target, abcorr, observer );
            step     = 7. * SPD;

            //
            // Set up the constraint.
            //
            cons = GFConstraint.createExtremumConstraint( LOCAL_MAXIMUM );

            //
            // Run the search.
            //
            result = search.run ( cnfine, cons, step, MAXIVL );

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
         // Case 4
         //
         JNITestutils.tcase ( "Invalid value for nintvls."  );

         try
            {
            cnfine         = new SpiceWindow();
            int    ndays   = 90;
            int  nintvls   = 0;

            et0            = 0.;
            et1            = ndays * SPD;

            cnfine  = cnfine.insert( et0, et1 );

            //
            // Set up the search geometry parameters.
            //
            target           = new Body( "moon"  );
            abcorr           = new AberrationCorrection( "none" );
            observer         = new Body( "sun" );

            search   = new GFRangeRateSearch( target, abcorr, observer );
            step     = 7. * SPD;

            //
            // Set up the constraint.
            //
            cons =  GFConstraint.createExtremumConstraint( LOCAL_MAXIMUM );

            //
            // Run the search.
            //
            result = search.run ( cnfine, cons, step, nintvls );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(VALUEOUTOFRANGE)" );

            }
         catch ( SpiceException ex )
            {
            ok = JNITestutils.chckth ( true, "SPICE(VALUEOUTOFRANGE)", ex );
            }

         //
         // Usable size of WORK windows is positive but is too small
         // to hold all intervals found across CNFINE. CNFINE spans
         // 360 days, the local maximums occur approximately every
         // 28 days.
         //

         try
            {
            cnfine         = new SpiceWindow();
            int  ndays     = 360;
            int  nintvls   = 2 * 3;

            et0            = 0.;
            et1            = ndays * SPD;

            cnfine  = cnfine.insert( et0, et1 );

            //
            // Set up the search geometry parameters.
            //
            target           = new Body( "moon"  );
            abcorr           = new AberrationCorrection( "none" );
            observer         = new Body( "sun" );

            search   = new GFRangeRateSearch( target, abcorr, observer );
            step     = 7. * SPD;

            //
            // Set up the constraint.
            //
            cons = GFConstraint.createExtremumConstraint( LOCAL_MAXIMUM );

            //
            // Run the search.
            //
            result = search.run ( cnfine, cons, step, nintvls );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(WINDOWEXCESS)" );

            }
         catch ( SpiceException ex )
            {
            ok = JNITestutils.chckth ( true, "SPICE(WINDOWEXCESS)", ex );
            }


         //
         // Case 5
         //
         // Loop over aberration corrections for a configuration with
         // a known geometry and behavior.
         //
         try
            {
            cnfine         = new SpiceWindow();
            int    ndays   = 90;
            int    count   = 0;

            et0            = 0.;
            et1            = ndays * SPD;

            cnfine  = cnfine.insert( et0, et1 );

            //
            // Set up the search geometry parameters.
            //
            target           = new Body( "alpha"  );
            observer         = new Body( "beta" );
            step             = 0.4 * SPD;

            for ( i = 0;  i < NCORR;  i++ )
               {

               abcorr = new AberrationCorrection( CORR[i] );
               JNITestutils.tcase ( CORR[i] + " Local Max" );

               search   = new GFRangeRateSearch( target, abcorr, observer );

               cons =  GFConstraint.createExtremumConstraint( LOCAL_MAXIMUM );

               result = search.run ( cnfine, cons, step, MAXWIN );

               count = 0;
               count = result.card();
               ok = JNITestutils.chcksi( "COUNT", count, "=", ndays, 0 );


               JNITestutils.tcase ( CORR[i] + " Local Min" );

               cons =  GFConstraint.createExtremumConstraint( LOCAL_MINIMUM );

               result = search.run ( cnfine, cons, step, MAXWIN );

               count = 0;
               count = result.card();
               ok = JNITestutils.chcksi( "COUNT", count, "=", ndays, 0 );


               //
               // Each orbit of BETA includes a local minimum and local
               // maximum range rate event with respect to ALPHA. As the
               // orbit is periodic and closed, two events per orbit must
               // exist where the sign of the range rate changes., i.e.
               // the range rate equals zero.
               //
               JNITestutils.tcase ( CORR[i] + " =" );

               refval = 0;
               cons = GFConstraint.createReferenceConstraint( "=", refval );

               result = search.run ( cnfine, cons, step, MAXWIN );

               count = 0;
               count = result.card();
               ok = JNITestutils.chcksi( "COUNT", count, "=", 2*ndays, 0 );

               }

            }
         catch ( SpiceException ex )
            {
            ok = JNITestutils.chckth ( false, " ", ex );
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
         // Case n
         //
         JNITestutils.tcase ( "Clean up:  delete kernels." );

         KernelDatabase.clear();

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

