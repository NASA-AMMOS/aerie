
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestOsculatingElements provides methods that implement test families for
the class OsculatingElements.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 03-DEC-2009 (NJB)</h3>
*/
public class TestOsculatingElements extends Object
{

   //
   // Class constants
   //


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test OsculatingElements and associated classes.
   */
   public static boolean f_OsculatingElements()

      throws SpiceException
   {
      //
      // Constants
      //
      final String                      SPK        = "oscelt.bsp";

      final double                      TIGHT_TOL  = 1.e-12;
      final double                      MED_TOL    = 1.e-9;
      final double                      LOOSE_TOL  = 1.e-4;

      final double                      BODY10_GM  =   132712440023.310;

      final int                         NELTS      = 8;


      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              observer;
      Body                              target;

      OsculatingElements                elts0;
      OsculatingElements                elts1;
      OsculatingElements                elts2;
      OsculatingElements                elts3;

      ReferenceFrame                    ref;

      StateVector                       sv0;
      StateVector                       sv1;
      StateVector                       sv2;

      TDBTime                           et0;
      TDBTime                           et1;


      boolean                           ok;

      double                            ecc;
      double[]                          eltArray0;
      double[]                          eltArray1;
      double                            lnode;
      double                            m0;
      double                            mu;
      double                            rp;
      double                            t0;

      int                               handle = 0;
      int                               i;


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

         JNITestutils.topen ( "f_OsculatingElements" );


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
         JNITestutils.tcase (  "Error: pass array of invalid size " +
                               "to array-based constructor."          );

         try
         {
            eltArray0 = new double[9];

            elts0     = new OsculatingElements( eltArray0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INVALIDARRAYSIZE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(INVALIDARRAYSIZE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass invalid GM " +
                               "to state-based constructor." );

         try
         {
            abcorr   = new AberrationCorrection( "NONE" );

            observer = new Body( "Sun"  );
            target   = new Body( "Mars" );

            ref      = new ReferenceFrame( "J2000" );

            et0      = new TDBTime( "2009 Dec 3" );

            sv0      = new StateVector( target, et0, ref, abcorr, observer );


            elts0    = new OsculatingElements( sv0, et0, -1.0 );

            Testutils.dogDidNotBark (  "SPICE(NONPOSITIVEMASS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(NONPOSITIVEMASS)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass invalid GM " +
                               "to element-based propagator." );

         try
         {
            abcorr   = new AberrationCorrection( "NONE" );

            observer = new Body( "Sun"  );
            target   = new Body( "Mars" );

            ref      = new ReferenceFrame( "J2000" );

            et0      = new TDBTime( "2009 Dec 3" );

            sv0      = new StateVector( target, et0, ref, abcorr, observer );


            elts0    = new OsculatingElements( sv0, et0, BODY10_GM );


            eltArray0 = elts0.toArray();

            //
            // Corrupt the GM value.
            //
            eltArray0[7] = -1.0;

            elts1        = new OsculatingElements( eltArray0 );

            sv1          = elts1.propagate( et0 );


            Testutils.dogDidNotBark (  "SPICE(BADGM)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(BADGM)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass invalid GM " +
                               "to state-based propagator." );

         try
         {
            abcorr   = new AberrationCorrection( "NONE" );

            observer = new Body( "Sun"  );
            target   = new Body( "Mars" );

            ref      = new ReferenceFrame( "J2000" );

            et0      = new TDBTime( "2009 Dec 3" );

            sv0      = new StateVector( target, et0, ref, abcorr, observer );


            sv1      = OsculatingElements.propagate( sv0, -1.0,
                                                       new TDBDuration(0.0) );


            Testutils.dogDidNotBark (  "SPICE(NONPOSITIVEMASS)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(NONPOSITIVEMASS)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test state-based constructor and " +
                              "Propagate(Time)." );

         //
         // This constructor is analogous to OSCELT.
         //
         abcorr   = new AberrationCorrection( "NONE" );

         observer = new Body( "Sun"  );
         target   = new Body( "Mars" );

         ref      = new ReferenceFrame( "J2000" );

         et0      = new TDBTime( "2009 Dec 3" );

         sv0      = new StateVector( target, et0, ref, abcorr, observer );

         elts0    = new OsculatingElements( sv0, et0, BODY10_GM );

         //
         // Propagate these elements for a day.
         //

         et1      = et0.add( new JEDDuration(1.0) );

         sv1      = elts0.propagate( et1 );

         //
         // Look up the target's state at et1 and compare. Position and
         // velocity should agree to at least 4 decimal places.
         //
         sv2      = new StateVector( target, et1, ref, abcorr, observer );


         ok = JNITestutils.chckad ( "Position",
                                    sv1.getPosition().toArray(),
                                    "~~/",
                                    sv2.getPosition().toArray(),
                                    LOOSE_TOL                   );

         ok = JNITestutils.chckad ( "Velocity",
                                    sv1.getVelocity().toArray(),
                                    "~~/",
                                    sv2.getVelocity().toArray(),
                                    LOOSE_TOL                   );


         //
         // For debugging:
         // System.out.println( "sv1:" + "\n" + sv1 + "\n" +
         //                     "sv2:" + "\n" + sv2           );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test state-based constructor: convert " +
                              "elements back to a state vector."             );

         //
         // Propagating to the epoch of the elements (in other words,
         // using a time delta of 0) just converts elements to a state.
         //

         sv1  = elts0.propagate(et0);

         ok = JNITestutils.chckad ( "Position",
                                    sv1.getPosition().toArray(),
                                    "~~/",
                                    sv0.getPosition().toArray(),
                                    MED_TOL                   );

         ok = JNITestutils.chckad ( "Velocity",
                                    sv1.getVelocity().toArray(),
                                    "~~/",
                                    sv0.getVelocity().toArray(),
                                    MED_TOL                   );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test array-based constructor and toArray." );


         eltArray0 = new double[NELTS];

         for ( i = 0;  i < NELTS;  i++ )
         {
            eltArray0[i] = i;
         }

         elts0 = new OsculatingElements( eltArray0 );


         //
         // We should have an exact match.
         //
         ok = JNITestutils.chckad ( "eltArray0",
                                    elts0.toArray(),
                                    "=",
                                    eltArray0,
                                    0.0             );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );


         eltArray0 = new double[NELTS];

         for ( i = 0;  i < NELTS;  i++ )
         {
            eltArray0[i] = i;
         }

         elts0 = new OsculatingElements( eltArray0 );
         elts1 = new OsculatingElements( eltArray0 );

         elts2 = new OsculatingElements( elts0 );

         //
         // Make sure that changing elts0 doesn't affect elts2.
         //
         eltArray1 = new double[NELTS];

         for ( i = 0;  i < NELTS;  i++ )
         {
            eltArray1[i] = i;
         }

         elts0 = new OsculatingElements( eltArray1 );

         //
         // We should have an exact match.
         //
         ok = JNITestutils.chckad ( "elts2",
                                    elts2.toArray(),
                                    "=",
                                    elts1.toArray(),
                                    0.0             );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getPerifocalDistance." );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "rp",
                                    elts0.getPerifocalDistance(),
                                    "=",
                                    eltArray0[0],
                                    0.0             );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getEccentricity." );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "ecc",
                                    elts0.getEccentricity(),
                                    "=",
                                    eltArray0[1],
                                    0.0             );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getInclination." );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "inc",
                                    elts0.getInclination(),
                                    "=",
                                    eltArray0[2],
                                    0.0             );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getLongitudeOfNode." );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "LNODE",
                                    elts0.getLongitudeOfNode(),
                                    "=",
                                    eltArray0[3],
                                    0.0             );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getArgumentOfPeriapsis." );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "ARGP",
                                    elts0.getArgumentOfPeriapsis(),
                                    "=",
                                    eltArray0[4],
                                    0.0             );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getMeanAnomaly." );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "M0",
                                    elts0.getMeanAnomaly(),
                                    "=",
                                    eltArray0[5],
                                    0.0             );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getEpoch." );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "T0",
                                    elts0.getEpoch().getTDBSeconds(),
                                    "=",
                                    eltArray0[6],
                                    0.0             );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getGM" );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "GM",
                                    elts0.getGM(),
                                    "=",
                                    eltArray0[7],
                                    0.0             );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test state-based (PROP2B-style) propagator." );

         //
         // This constructor is analogous to OSCELT.
         //
         abcorr   = new AberrationCorrection( "NONE" );

         observer = new Body( "Sun"  );
         target   = new Body( "Mars" );

         ref      = new ReferenceFrame( "J2000" );

         et0      = new TDBTime( "2009 Dec 3" );

         sv0      = new StateVector( target, et0, ref, abcorr, observer );

         elts0    = new OsculatingElements( sv0, et0, BODY10_GM );


         //
         // Propagate these elements for a day.
         //

         TDBDuration dt = new TDBDuration( TimeConstants.SPD );

         et1      = et0.add( dt );

         sv1      = OsculatingElements.propagate( sv0, BODY10_GM, dt );

         //
         // Look up the target's state at et1 and compare. Position and
         // velocity should agree to at least 4 decimal places.
         //
         sv2      = new StateVector( target, et1, ref, abcorr, observer );


         ok = JNITestutils.chckad ( "Position",
                                    sv1.getPosition().toArray(),
                                    "~~/",
                                    sv2.getPosition().toArray(),
                                    LOOSE_TOL                   );

         ok = JNITestutils.chckad ( "Velocity",
                                    sv1.getVelocity().toArray(),
                                    "~~/",
                                    sv2.getVelocity().toArray(),
                                    LOOSE_TOL                   );


         //
         // For debugging:
         // System.out.println( "sv1:" + "\n" + sv1 + "\n" +
         //                     "sv2:" + "\n" + sv2           );

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
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

