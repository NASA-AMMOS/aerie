package spice.tspice;

import java.io.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.GFConstraint.*;
import static spice.basic.TimeConstants.*;


/**
Class TestGFPhaseAngleSearch provides methods that implement test families 
for the class GFPhaseAngleSearch.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 11-MAR-2014 (EDW)</h3>
*/
public class TestGFPhaseAngleSearch extends Object
{

   //
   // Class constants
   //
   private static String  PCK           = "gfpa.pck";
   private static String  SPK           = "gfpa.bsp";
   private static String  NATPCK        = "nat.tpc";
   private static String  NATSPK        = "nat.bsp";


   //
   // Methods
   //

   /**
   Test GFPhaseAngleSearch and associated classes.
   */
   public static boolean f_GFPhaseAngleSearch()

      throws SpiceException
      {
      //
      // Constants
      //

      final int                         MAXIVL = 5000;
      final int                         MAXWIN = 2 * MAXIVL;
      final int                         NCORR  = 5;
      final double                      TIGHT = 10e-10;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              lumin;
      Body                              obsrvr;
      Body                              target;

      GFConstraint                      cons;

      GFPhaseAngleSearch                     search;

      SpiceWindow                       cnfine;
      SpiceWindow                       result = null;

      String                            title;
      String[]                          CORR   = {
                                                 "NONE",
                                                 "lt",
                                                 " lt+s",
                                                 " cn",
                                                 " cn + s",
                                                 };

      double                             step;

      boolean                           ok;

      double                            refval;
      double                            alpha;
      double []                         timint;
      double                            value;
      double                            left;
      double                            right;


      int                               handle = 0;
      int                               i;
      int                               j;
      int                               nathan = 0;
      int                               ndays;
      int                               count;

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
        JNITestutils.topen ( "f_GFPhaseAngleSearch" );


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


         left   = 0.;
         right  = SPD;
         cnfine = new SpiceWindow();

         cnfine.insert( left, right );


         //
         // Error cases
         //

         //
         // Case 1
         //
         JNITestutils.tcase ( "Non-positive step size."  );

         try
            {

            //
            // Set up the search geometry parameters.
            //
            target = new Body( "alpha"  );
            lumin  = new Body( "sun"  );
            abcorr = new AberrationCorrection( "none" );
            obsrvr = new Body( "beta" );
            search = new GFPhaseAngleSearch ( target, lumin, abcorr, obsrvr );
            step   = -1.0;
            refval = 0;
            //
            // Set up the constraint.
            //
            cons     = GFConstraint.createReferenceConstraint( "=", refval );


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
         JNITestutils.tcase ( "Non unique body IDs"  );

         try
            {

            //
            // Set up the search geometry parameters.
            //
            target = new Body( "alpha"  );
            lumin  = new Body( "sun"  );
            abcorr = new AberrationCorrection( "none" );
            obsrvr = new Body( "alpha" );
            search = new GFPhaseAngleSearch ( target, lumin, abcorr, obsrvr );
            step   = 1.0;
            refval = 0;
            //
            // Set up the constraint.
            //
            cons     = GFConstraint.createReferenceConstraint( "=", refval );


            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXWIN );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(BODIESNOTDISTINCT)" );

            }
         catch ( SpiceException ex )
            {
            ok = JNITestutils.chckth ( true, "SPICE(BODIESNOTDISTINCT)", ex );
            }


         try
            {

            //
            // Set up the search geometry parameters.
            //
            target = new Body( "sun"  );
            lumin  = new Body( "sun"  );
            abcorr = new AberrationCorrection( "none" );
            obsrvr = new Body( "beta" );
            search = new GFPhaseAngleSearch ( target, lumin, abcorr, obsrvr );
            step   = 1.0;
            refval = 0;
            //
            // Set up the constraint.
            //
            cons     = GFConstraint.createReferenceConstraint( "=", refval );


            //
            // Run the search.
            //
            result = search.run ( cnfine, cons, step, MAXWIN );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(BODIESNOTDISTINCT)" );

            }
         catch ( SpiceException ex )
            {
            ok = JNITestutils.chckth ( true,  "SPICE(BODIESNOTDISTINCT)", ex );
            }


         try
            {

             //
             // Set up the search geometry parameters.
             //
             target = new Body( "alpha"  );
             lumin  = new Body( "sun"  );
             abcorr = new AberrationCorrection( "none" );
             obsrvr = new Body( "sun" );
             search = new GFPhaseAngleSearch ( target, lumin, abcorr, obsrvr );
             step   = 1.0;
             refval = 0;
             //
             // Set up the constraint.
             //
            cons     = GFConstraint.createReferenceConstraint( "=", refval );


             //
             // Run the search.
             //
             result = search.run ( cnfine, cons, step, MAXWIN );

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
// Case 4
//
// Invalid relations operator
//

//
// Case 5
//
// Invalid body names
//

//
// Case 6
//
// Negative adjustment value
//

         //
         // Case 7
         //
         JNITestutils.tcase ( "Ephemeris data unavailable"  );

         try
            {

            //
            // Set up the search geometry parameters.
            //
            target = new Body( "alpha"  );
            lumin  = new Body( "sun"  );
            abcorr = new AberrationCorrection( "none" );
            obsrvr = new Body( "dawn" );
            search = new GFPhaseAngleSearch ( target, lumin, abcorr, obsrvr );
            step   = 1.0;
            refval = 0;
            //
            // Set up the constraint.
            //
            cons     = GFConstraint.createReferenceConstraint( "=", refval );


            //
            // Run the search.
            //
            result   = search.run ( cnfine, cons, step, MAXWIN );

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
         // Case 8
         //
         JNITestutils.tcase ( "Invalid value for nintvls"  );

         try
            {

            //
            // Set up the search geometry parameters.
            //
            target = new Body( "alpha"  );
            lumin  = new Body( "sun"  );
            abcorr = new AberrationCorrection( "none" );
            obsrvr = new Body( "beta" );
            search = new GFPhaseAngleSearch ( target, lumin, abcorr, obsrvr );
            step   = 1.0;
            refval = 0;
            //
            // Set up the constraint.
            //
            cons     = GFConstraint.createReferenceConstraint( "=", refval );


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
         // Case 9
         //
         // Loop over aberration corrections for a configuration with
         // a known geometry and behavior.
         //

         //
         // Confine for ninety days.
         //
         ndays  = 90;
         left   = 0.;
         right  = ndays*SPD;
         cnfine = new SpiceWindow();

         cnfine.insert( left, right );

         try
            {

            //
            // Set up the search geometry parameters.
            //
            target = new Body( "alpha"  );
            lumin  = new Body( "sun"  );
            obsrvr = new Body( "beta" );
            step   = 0.2d * SPD;
            refval = 0;
            for ( i = 0;  i < NCORR;  i++ )
               {

               JNITestutils.tcase ( CORR[i] + " LOCAL_MINIMUM" );

               abcorr = new AberrationCorrection( CORR[i] );
               search = new GFPhaseAngleSearch( target, lumin, abcorr, obsrvr );

               cons = GFConstraint.createExtremumConstraint( LOCAL_MINIMUM );

               result = search.run ( cnfine, cons, step, MAXWIN );

               //
               // Check the number of intervals in the result window.
               // We expect two events per day over CNFINE.
               //
               count = 0;
               count = result.card();
               ok = JNITestutils.chcksi( "COUNT", count, "=", 2*ndays, 0 );


               JNITestutils.tcase ( CORR[i] + " LOCAL_MAXIMUM" );

               abcorr = new AberrationCorrection( CORR[i] );
               search = new GFPhaseAngleSearch( target, lumin, abcorr, obsrvr );

               cons = GFConstraint.createExtremumConstraint( LOCAL_MAXIMUM );

               result = search.run( cnfine, cons, step, MAXWIN );

               //
               // Check the number of intervals in the result window.
               // We expect two events per day over CNFINE.
               //
               count = 0;
               count = result.card();
               ok = JNITestutils.chcksi( "COUNT", count, "=", 2*ndays, 0 );

               }

            }
         catch ( SpiceException ex )
            {
            ok = JNITestutils.chckth ( false, " ", ex );
            }


         //
         // Check for a specific reference value.
         //
         try
            {

            //
            // Set up the search geometry parameters.
            //
            target = new Body( "alpha"  );
            lumin  = new Body( "sun"  );
            obsrvr = new Body( "beta" );
            step   = 0.2d * SPD;
            refval = 0.1;
            for ( i = 0;  i < NCORR;  i++ )
               {

               title = CORR[i] + " EQUALS";
               JNITestutils.tcase ( title );

               abcorr = new AberrationCorrection( CORR[i] );
               search = new GFPhaseAngleSearch( target, lumin, abcorr, obsrvr );

               cons   = GFConstraint.createReferenceConstraint( "=", refval );


               result = search.run ( cnfine, cons, step, MAXWIN );

               count = 0;
               count = result.card();

               for ( j = 0;  j < count;  j++ )
                  {
                  timint = result.getInterval( j );
                  value = zzgfpsq( (new TDBTime(timint[0]) ), target, lumin, 
                                                             obsrvr, abcorr);

                  ok = JNITestutils.chcksd( title , refval, "~", value, TIGHT );
                  }

               }

            }
         catch ( SpiceException ex )
            {
            ok = JNITestutils.chckth ( false, " ", ex );
            }





         //
         // Case 10
         //
         // Loop over aberration corrections for a configuration with
         // a known geometry and behavior.
         //

         try
            {

            //
            // Set up the search geometry parameters.
            //
            target = new Body( "alpha"  );
            abcorr = new AberrationCorrection( "NONE" );
            lumin  = new Body( "sun"  );
            obsrvr = new Body( "beta" );
            step   = 0.2d * SPD;
            refval = 0;
            ReferenceFrame frame =  new ReferenceFrame( "J2000" );

            //
            // We can compute the value of the maximum phase angle based
            // on the ALPHA-BETA geometry, the half-angle of the BETA
            // orbit as seen from ALPHA.
            //
            //    sin(alpha) = beta_orbit_radius     5.246368076245e+05
            //                 ------------------ ~  ------------------
            //                 alpha_orbit_radius    2.098547206045e+06
            //

            PositionRecord posa = new PositionRecord( target,
                                      (new TDBTime(left)),
                                      frame, abcorr, lumin );
            PositionRecord posb = new PositionRecord( obsrvr, 
                                      (new TDBTime(left)),
                                      frame, abcorr, lumin );

            alpha = Math.asin( posb.norm()/posa.norm() );

            title = "PHASE LOCAL_MAXIMUM";
            JNITestutils.tcase ( title );

            search = new GFPhaseAngleSearch( target, lumin, abcorr, obsrvr );

            cons = GFConstraint.createExtremumConstraint( LOCAL_MAXIMUM );

            result = search.run ( cnfine, cons, step, MAXWIN );

            count = 0;
            count = result.card();

            for ( j = 0;  j < count;  j++ )
               {
               timint = result.getInterval( j );
               value = zzgfpsq ( (new TDBTime(timint[0]) ), target, lumin, 
                                                           obsrvr, abcorr);

               ok = JNITestutils.chcksd ( title , alpha, "~", value, TIGHT );
               }


            title = "PHASE  LOCAL_MINIMUM";
            JNITestutils.tcase ( title );

            search = new GFPhaseAngleSearch( target, lumin, abcorr, obsrvr );

            cons = GFConstraint.createExtremumConstraint( LOCAL_MINIMUM );

            result = search.run ( cnfine, cons, step, MAXWIN );

            count = 0;
            count = result.card();

            for ( j = 0;  j < count;  j++ )
               {
               timint = result.getInterval( j );
               value = zzgfpsq ( (new TDBTime(timint[0]) ), target, lumin, 
                                                          obsrvr, abcorr);

               ok = JNITestutils.chcksd ( title , 0., "~", value, TIGHT );
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

   /**
   Compute the angular separation of two spherical targets at
   a given epoch, as seen by a specified observer.
   */
   private static double zzgfpsq (  TDBTime              et,
                                    Body                 targ,
                                    Body                 illum,
                                    Body                 obs,
                                    AberrationCorrection abcorr)

      throws SpiceException
      {
      //
      // Local constants
      //
      final ReferenceFrame frame =  new ReferenceFrame( "J2000" );


      //
      // Local variables
      //
      PositionRecord                    pr1;
      PositionRecord                    pr2;

      double                            sep;
      double                            value;

      //
      // Get positions of targets as seen by the observer.
      //
      pr1 =  new PositionRecord( targ, et, frame, abcorr, obs );


      if (  abcorr.getName().equals( "NONE" ) )
         {
         pr2 =  new PositionRecord( illum, et, frame, abcorr, targ );
         }
      else
         {
         pr2 =  new PositionRecord( illum, et.sub( pr1.getLightTime() ), 
                                                  frame, abcorr, targ );
         }

      sep = pr1.sep(pr2 );

      value = Math.acos(-1.) - sep;

      return( value );
      }

   }

