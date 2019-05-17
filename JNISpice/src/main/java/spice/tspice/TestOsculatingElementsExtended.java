
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestOsculatingElementsExtended
provides methods that implement test families for
the class OsculatingElementsExtended.

<h3>Version 1.0.0 26-JAN-2017 (NJB)</h3>
*/
public class TestOsculatingElementsExtended extends Object
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
   Test OsculatingElementsExtended and associated classes.
   */
   public static boolean f_OsculatingElementsExtended()

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
      final double                      BODY399_GM =         398600.436;

      final int                         NELTS      = 8;

      final int                         IDX_RP    =  0;
      final int                         IDX_ECC   =  1;
      final int                         IDX_INC   =  2;
      final int                         IDX_LNODE =  3;
      final int                         IDX_ARGP  =  4;
      final int                         IDX_M0    =  5;
      final int                         IDX_T0    =  6;
      final int                         IDX_MU    =  7;

      //
      // Note that NXELTS could change in a future Toolkit version.
      //
      final int                         NXELTS     = 11;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              observer;
      Body                              target;

      OsculatingElementsExtended        eltsX0;
      OsculatingElementsExtended        eltsX1;
      OsculatingElementsExtended        eltsX2;

      OsculatingElements                elts0;
      OsculatingElements                elts1;

      ReferenceFrame                    ref;

      StateVector                       sv0;
      StateVector                       sv1;
      StateVector                       sv2;

      TDBTime                           et0;
      TDBTime                           et1;

      Vector3                           h;
      Vector3                           pos;
      Vector3                           vel;

      boolean                           ok;

      double                            a;
      double                            ecc;
      double[]                          eltArray0;
      double[]                          eltArray1;
      double[]                          eltArray2;
      double                            h2;
      double                            lnode;
      double                            m0;
      double                            mu;
      double                            nu;
      double                            p;
      double                            r;
      double                            rp;
      double[]                          stateArray;
      double                            tau;
      double                            t0;
      double                            xa;
      double                            xnu;
      double                            xr;
      double                            xtau;

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

         JNITestutils.topen ( "f_OsculatingElementsExtended" );


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

         JNITestutils.tcase ( "Constructor error: bad GM." );
  

         abcorr   = new AberrationCorrection( "NONE" );

         observer = new Body( "Sun"  );
         target   = new Body( "Mars" );

         ref      = new ReferenceFrame( "J2000" );

         et0      = new TDBTime( "2009 Dec 3" );

         sv0      = new StateVector( target, et0, ref, abcorr, observer );

         try
         {

            eltsX0   = new OsculatingElementsExtended( sv0, et0, -1.0 );

            Testutils.dogDidNotBark( "SPICE(NONPOSITIVEMASS)" );

         }
         catch ( SpiceException exc )
         {       
            ok = JNITestutils.chckth( true, "SPICE(NONPOSITIVEMASS)", exc );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Fetch period for hyperbolic orbit." );

         stateArray = new double[6];

         stateArray[0] = 4.0e4;
         stateArray[4] = 3.0e1;

         sv1    = new StateVector( stateArray );

         eltsX0 = new OsculatingElementsExtended( sv1, et0, BODY399_GM );

         try
         {
            tau = eltsX0.getPeriod();

            Testutils.dogDidNotBark( "SPICE(NOTCOMPUTABLE)" );
         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(NOTCOMPUTABLE)", exc );
         }


        //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Fetch semi-major axis for near-parabolic " + 
                              "orbit." );

         eltArray0 = new double[NELTS];
 
         mu = 1.0;

         eltArray0[IDX_RP   ]   =   1.e100;
         eltArray0[IDX_ECC  ]   =   1.0;
         eltArray0[IDX_INC  ]   =   Math.PI/6;
         eltArray0[IDX_MU   ]   =   mu;
         eltArray0[IDX_ARGP ]   =   0.0;
         eltArray0[IDX_T0   ]   =   0.0;
         eltArray0[IDX_LNODE]   =   0.0;

         elts0 = new OsculatingElements( eltArray0 );

         et1   = new TDBTime( 0.0 );

         sv0   = elts0.propagate( et1 );

         //System.out.println ( "sv0 = " + sv0 );

         eltsX1 = new OsculatingElementsExtended( sv0, et1, mu );

         //System.out.println ( "ecc - 1 = " + (eltsX1.getEccentricity()-1.0) );

         try
         {
            tau = eltsX1.getSemiMajorAxis();

            Testutils.dogDidNotBark( "SPICE(NOTCOMPUTABLE)" );
         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(NOTCOMPUTABLE)", exc );
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

         eltsX0   = new OsculatingElementsExtended( sv0, et0, BODY10_GM );

         //
         // Propagate these elements for a day.
         //

         et1      = et0.add( new JEDDuration(1.0) );

         sv1      = eltsX0.propagate( et1 );

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
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test state-based constructor: convert " +
                              "elements back to a state vector."             );

         //
         // Propagating to the epoch of the elements (in other words,
         // using a time delta of 0) just converts elements to a state.
         //

         sv1  = eltsX0.propagate(et0);

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

         JNITestutils.tcase ( "Test toArray." );


         /*
         Compare classical elements to those from an extended element set.
         */

         eltsX0 = new OsculatingElementsExtended( sv0, et0, BODY10_GM );
         elts0  = new OsculatingElements        ( sv0, et0, BODY10_GM );

         eltArray0 = eltsX0.toArray();
         eltArray1 = new double[NELTS];

         System.arraycopy( eltArray0, 0, eltArray1, 0, NELTS );

         //
         // We should have an exact match.
         //
         ok = JNITestutils.chckad ( "eltArray1",
                                    eltArray1,
                                    "=",
                                    elts0.toArray(),
                                    0.0             );

         //
         // Check the extended portion of eltArray0.
         //
         ok = JNITestutils.chcksd ( "Nu",
                                    eltArray0[8],
                                    "=",
                                    eltsX0.getTrueAnomaly(),
                                    0.0                      );

         ok = JNITestutils.chcksd ( "A",
                                    eltArray0[9],
                                    "=",
                                    eltsX0.getSemiMajorAxis(),
                                    0.0                        );

         ok = JNITestutils.chcksd ( "Tau",
                                    eltArray0[10],
                                    "=",
                                    eltsX0.getPeriod(),
                                    0.0                 );

         //
         // Make sure we have the expected array size.
         //
         ok = JNITestutils.chcksi ( "Array size",
                                    eltArray0.length,
                                    "=",
                                    NXELTS,
                                    0                );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );


         abcorr   = new AberrationCorrection( "NONE" );

         observer = new Body( "Sun"  );
         target   = new Body( "Mars" );

         ref      = new ReferenceFrame( "J2000" );

         et0      = new TDBTime( "2009 JAN 25" );

         sv0      = new StateVector( target, et0, ref, abcorr, observer );

         eltsX0   = new OsculatingElementsExtended( sv0, et0, BODY10_GM );

         eltsX1   = new OsculatingElementsExtended( sv0, et0, BODY10_GM );

         eltsX2 = new OsculatingElementsExtended( eltsX0 );

         
         //
         // Compare eltsX2 to eltsX0. We should have an exact match.
         //
         ok = JNITestutils.chckad ( "eltsX2",
                                    eltsX2.toArray(),
                                    "=",
                                    eltsX0.toArray(),
                                    0.0             );

      
         //
         // Make sure that changing eltsX0 doesn't affect eltsX2.
         //
     
         et0      = new TDBTime( "2009 FEB 25" );

         sv1      = new StateVector( target, et1, ref, abcorr, observer );

         eltsX0   = new OsculatingElementsExtended( sv1, et1, BODY10_GM );

         //
         // Compare eltsX2 to eltsX1. We should have an exact match.
         //
         ok = JNITestutils.chckad ( "eltsX2",
                                    eltsX2.toArray(),
                                    "=",
                                    eltsX1.toArray(),
                                    0.0             );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getPerifocalDistance." );


         abcorr   = new AberrationCorrection( "NONE" );

         observer = new Body( "Sun"  );
         target   = new Body( "Mars" );

         ref      = new ReferenceFrame( "J2000" );

         et0      = new TDBTime( "2009 JAN 25" );

         sv0      = new StateVector( target, et0, ref, abcorr, observer );

         eltsX0   = new OsculatingElementsExtended( sv0, et0, BODY10_GM );



         //
         // Continue with the last computed extended element set.
         //

         eltArray0 = eltsX0.toArray();

         ok = JNITestutils.chcksd ( "rp",
                                    eltsX0.getPerifocalDistance(),
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
                                    eltsX0.getEccentricity(),
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
                                    eltsX0.getInclination(),
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
                                    eltsX0.getLongitudeOfNode(),
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
                                    eltsX0.getArgumentOfPeriapsis(),
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
                                    eltsX0.getMeanAnomaly(),
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
                                    eltsX0.getEpoch().getTDBSeconds(),
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
                                    eltsX0.getGM(),
                                    "=",
                                    eltArray0[7],
                                    0.0             );
        

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getTrueAnomaly" );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "Nu",
                                    eltsX0.getTrueAnomaly(),
                                    "=",
                                    eltArray0[8],
                                    0.0             );

         //
         // See whether the value of nu makes sense.
         //
         // Use nu to obtain the expected value of r; check this 
         // against the value of r obtained from the original state.
         //
         nu  = eltsX0.getTrueAnomaly();

         ecc =  eltsX0.getEccentricity();
         pos =  sv0.getPosition();
         vel =  sv0.getVelocity();
         h   =  pos.cross( vel );
         h2  =  h.dot(h);
         mu  =  BODY10_GM;

         p   = h2/mu;

         xr  = p / ( 1.0 + ecc*Math.cos(nu) );

         r   =  pos.norm();       
 
         ok = JNITestutils.chcksd ( "r", r, "~/", xr, TIGHT_TOL );
                          

         //
         // Make sure that the mean and true anomalies have the 
         // same sign. We assume neither is zero.
         // 
         m0 = eltsX0.getMeanAnomaly();

         ok = JNITestutils.chcksd ( "nu/m0", nu/m0, ">", 0.0, 0.0 );         
        

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getSemiMajorAxis" );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "A",
                                    eltsX0.getSemiMajorAxis(),
                                    "=",
                                    eltArray0[9],
                                    0.0             );

         //
         // See whether the value of A makes sense.
         //

         a   = eltsX0.getSemiMajorAxis();

         ecc = eltsX0.getEccentricity();
         rp  = eltsX0.getPerifocalDistance();

         xa  = rp / ( 1.0 - ecc );

         ok = JNITestutils.chcksd ( "A vs XA", a, "~/", xa, TIGHT_TOL );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getPeriod" );

         //
         // Continue with the element set from the previous case.
         //
         ok = JNITestutils.chcksd ( "Tau",
                                    eltsX0.getPeriod(),
                                    "=",
                                    eltArray0[10],
                                    0.0             );

         //
         // See whether the value of tau makes sense.
         //

         a    = eltsX0.getSemiMajorAxis();

         ecc  = eltsX0.getEccentricity();
         rp   = eltsX0.getPerifocalDistance();

         xtau = 2*Math.PI * Math.sqrt( Math.pow(a,3) / BODY10_GM  );

         tau  = eltsX0.getPeriod();

         ok = JNITestutils.chcksd ( "tau vs xtau", tau, "~/", xtau, TIGHT_TOL );



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

