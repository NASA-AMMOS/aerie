
package spice.tspice;


import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.OccultationCode.*;
import static spice.basic.AngularUnits.*;


/**
Class TestOccultationState provides methods that implement test families for
the class OccultationState.

<h3>Version 1.0.0 31-DEC-2016 (NJB)</h3>
*/
public class TestOccultationState extends Object
{

   //
   // Class constants
   //
   static final String                  DSK  = "occult_nat.bds";
   static final String                  PCK1 = "nat.tpc"; 
   static final String                  PCK2 = "generic.tpc"; 
   static final String                  SPK1 = "nat.bsp"; 
   static final String                  SPK2 = "generic.bsp";  

   //
   // Class variables
   //

   //
   // Methods
   //
 



   /**
   Test OccultationCode methods.
   */
   public static boolean f_OccultationState()

      throws SpiceException
   {
      //
      // Constants
      //
       

      //
      // Local variables
      //
      AberrationCorrection               abcorr;

      Body                               alpha = new Body( "ALPHA" );
      Body                               beta  = new Body( "BETA"  );
      Body                               gamma = new Body( "GAMMA" );
      Body                               obsrvr;
      Body                               sun   = new Body( "Sun" );
      Body                               targ1;
      Body                               targ2;

      OccultationCode                    occCode;

      OccultationCode[]                  result = 
                                         {
                                            NOOCC,  PARTL1, TOTAL1,
                                            PARTL2, ANNLR2         
                                         };

      OccultationCode                    xOccCode;

      ReferenceFrame                     alphaFixed = 
                                         new ReferenceFrame( "ALPHAFIXED" );

      ReferenceFrame                     betaFixed = 
                                         new ReferenceFrame( "BETAFIXED" );

      ReferenceFrame                     gammaFixed = 
                                         new ReferenceFrame( "GAMMAFIXED" );

      ReferenceFrame                     aframe; 
      ReferenceFrame                     bframe; 
      ReferenceFrame                     frame;
      ReferenceFrame                     frame1;
      ReferenceFrame                     frame2;

      String                             label;
      String                             shape1 = null;
      String                             shape2 = null;

      String[]                           times = 
                                         { "2011-JAN-02 19:00:00",
                                           "2011-JAN-02 21:00:00",
                                           "2011-JAN-03 00:00:00",
                                           "2011-JAN-03 09:00:00",
                                           "2011-JAN-03 11:00:00"  };

      TDBTime                            et;

      boolean                            ok;
      boolean                            makvtl;
      boolean                            usepad;

      double[]                           arad;
      double[][]                         bounds = new double[2][2];
      double                             c;

      double[]                           corpar = 
                                            new double[ DSKDescriptor.NSYPAR ];

      double                             first;
      double                             last;

      int                                bodyid;
      int                                code;  
      int                                corsys;
      int                                han1   = 0; 
      int                                han2   = 0; 
      int                                i;
      int                                j;
      int                                mltfac;
      int                                nlat;
      int                                nlon;
      int                                surfid;
      int                                xCode;

 
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

         JNITestutils.topen ( "f_OccultationState" );

         // ***********************************************************
         //
         //   Set up.
         //
         // ***********************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Setup: create and load SPK, PCK, LSK files." );

         han1 = JNITestutils.natspk( SPK1, true );

         han2 = JNITestutils.tstspk( SPK2, true );

         JNITestutils.natpck( PCK1, true, false );

         JNITestutils.tstpck( PCK2, true, false );

         JNITestutils.tstlsk();


         // ***********************************************************
         //
         //   Error cases for getOccultationState
         //
         // ***********************************************************

         //
         // Need to test exception handling control flow, so at least
         // one CSPICE error must be checked.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "getOccultationState error: invalid "  +
                             "first shape"                            );

         try
         {
            et      = new TDBTime( 0.0 );

            abcorr  = new AberrationCorrection( "NONE" );

            occCode = 

               OccultationState.
               getOccultationState( alpha,  "PNT",   alphaFixed,
                                    beta,   "POINT", betaFixed,
                                    abcorr, sun,     et        );

            Testutils.dogDidNotBark( "SPICE(BADMETHODSYNTAX)" );

         }
         catch ( SpiceException exc )
         {
            JNITestutils.chckth( true, "SPICE(BADMETHODSYNTAX)", exc );
         }



         // ***********************************************************
         //
         //   Normal cases for getOccultationState
         //
         // ***********************************************************


      //
      //     When TARG1 is GAMMA (abcorr = none):
      //
      // 2000-JAN-02 19 GAMMA  not occulted by        ALPHA  as seen by SUN   0
      // 2000-JAN-02 21 GAMMA  partially occulted by  ALPHA  as seen by SUN  -1
      // 2000-JAN-03 00 GAMMA  totally occulted by    ALPHA  as seen by SUN  -3
      // 2000-JAN-03 07 GAMMA  not occulted by        ALPHA  as seen by SUN   0
      // 2000-JAN-03 09 ALPHA  partially occulted by  GAMMA  as seen by SUN   1
      // 2000-JAN-03 11 ALPHA  transited by           GAMMA  as seen by SUN   2
      // 2000-JAN-03 16 GAMMA  not occulted by        ALPHA  as seen by SUN   0
      //
      //     When TARG1 is ALPHA (abcorr = none):
      //
      // 2000-JAN-02 19 ALPHA not occulted by         GAMMA  as seen by SUN   0
      // 2000-JAN-02 21 GAMMA partially occulted by   ALPHA  as seen by SUN   1
      // 2000-JAN-03 00 GAMMA totally occulted by     ALPHA  as seen by SUN   3
      // 2000-JAN-03 07 ALPHA not occulted by         GAMMA  as seen by SUN   0
      // 2000-JAN-03 09 ALPHA partially occulted by   GAMMA  as seen by SUN  -1
      // 2000-JAN-03 11 ALPHA transited by            GAMMA  as seen by SUN  -2
      // 2000-JAN-03 16 ALPHA not occulted by         GAMMA  as seen by SUN   0
      //
      //     From GFOCLT, Front (alpha), back (gamma), abcorr (none), 
      //                  occultation type (any)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-02 20:41:15
      //                     Stop time:  2000-JAN-03 03:51:58
      //
      //     From GFOCLT, Front (alpha), back (gamma), abcorr (none), 
      //                  occultation type (partial)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-02 20:41:15
      //                     Stop time:  2000-JAN-02 22:52:53
      //                  Interval            2
      //                     Start time: 2000-JAN-03 02:28:28
      //                     Stop time:  2000-JAN-03 03:51:58
      //
      //     From GFOCLT, Front (alpha), back (gamma), abcorr (none), 
      //                  occultation type (annular)
      //
      //                  No occultation was found.
      //
      //     From GFOCLT, Front (alpha), back (gamma), abcorr (none), 
      //                  occultation type (full, total)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-02 22:52:53
      //                     Stop time:  2000-JAN-03 02:28:28
      //
      //     From GFOCLT, Front (gamma), back (alpha), abcorr (none), 
      //                  occultation type (any)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-03 08:24:46
      //                     Stop time:  2000-JAN-03 14:57:23
      //
      //     From GFOCLT, Front (gamma), back (alpha), abcorr (none), 
      //                  occultation type (partial)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-03 08:24:46
      //                     Stop time:  2000-JAN-03 09:58:08
      //                  Interval            2
      //                     Start time: 2000-JAN-03 12:34:35
      //                     Stop time:  2000-JAN-03 14:57:23
      //
      //     From GFOCLT, Front (gamma), back (alpha), abcorr (none), 
      //                  occultation type (annular)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-03 09:58:08
      //                     Stop time:  2000-JAN-03 12:34:35
      //
      //     From GFOCLT, Front (gamma), back (alpha), abcorr (none), 
      //                  occultation type (full, total)
      //
      //                  No occultation was found.
      //
      //           -   -   -   -   -   -   -   -   -   -   -   -   
      //     From GFOCLT, Front (gamma), back (alpha), abcorr (xcn),
      //                  occultation type (any)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-03 08:24:39
      //                     Stop time:  2000-JAN-03 14:57:17
      //
      //     From GFOCLT, Front (gamma), back (alpha), abcorr (xcn),
      //                  occultation type (partial)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-03 08:24:39
      //                     Stop time:  2000-JAN-03 09:57:59
      //                  Interval            2
      //                     Start time: 2000-JAN-03 12:34:32
      //                     Stop time:  2000-JAN-03 14:57:17
      //
      //     From GFOCLT, Front (gamma), back (alpha), abcorr (xcn),
      //                  occultation type (annular transit)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-03 09:57:59
      //                     Stop time:  2000-JAN-03 12:34:32
      //
      //     From GFOCLT, Front (gamma), back (alpha), abcorr (xcn),
      //                  occultation type (full)
      //
      //                  No occultation was found.
      //
      //     From GFOCLT, Front (alpha), back (gamma), abcorr (xcn), 
      //                  occultation type (any)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-02 20:41:09
      //                     Stop time:  2000-JAN-03 03:51:51
      //
      //     From GFOCLT, Front (alpha), back (gamma), abcorr (xcn), 
      //                  occultation type (partial)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-02 20:41:09
      //                     Stop time:  2000-JAN-02 22:52:49
      //                  Interval            2
      //                     Start time: 2000-JAN-03 02:28:19
      //                     Stop time:  2000-JAN-03 03:51:51
      //
      //     From GFOCLT, Front (alpha), back (gamma), abcorr (xcn), 
      //                  occultation type (annular)
      //
      //                  No occultation was found.
      //
      //     From GFOCLT, Front (alpha), back (gamma), abcorr (xcn), 
      //                  occultation type (full)
      //
      //                  Interval            1
      //                     Start time: 2000-JAN-02 22:52:49
      //                     Stop time:  2000-JAN-03 02:28:19


 

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Verify the occultation states for " +
                             "specific times are correct."          );
 
         //
         // In order to test all occultation codes, first make GAMMA the
         // first target, and then make ALPHA the first target.  The same
         // configuration will have an occultation code of -1*result if the
         // target 1 and target 2 bodies are reversed.  This is why MLTFAC
         //  exists.
         //

         obsrvr = sun;
         abcorr = new AberrationCorrection( "None" );


         for ( i = 0;  i < 2;  i++ )
         {
            if ( i == 0 )
            {
               targ1  = gamma;
               shape1 = "ELLIPSOID";
               frame1 = gammaFixed;
               targ2  = alpha;
               shape2 = "ellipsoid";
               frame2 = alphaFixed;

               mltfac = 1;
            }
            else
            {
               targ1  = alpha;
               frame1 = alphaFixed;
               targ2  = gamma;
               frame2 = gammaFixed;
               mltfac = -1;
            }

            //
            // For each test, convert the time to ET, call 
            // getOccultationState, and verify that the returned
            // OccultationCode matches the desired result.
            //

            for ( j = 0;  j < times.length;  j++ )
            {

               et = new TDBTime( times[j] );

               occCode = OccultationState.
                         getOccultationState( targ1,  shape1, frame1, 
                                              targ2,  shape2, frame2,
                                              abcorr, obsrvr, et     );

               label = String.format( "Occ Code for case %d", j );

               ok = JNITestutils.chcksi( label, 
                                         occCode.getOccultationCode(),
                                         "=",   
                                         result[j].getOccultationCode()*mltfac,
                                         0                                   );
            }
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Point case: Point totally occulted " +
                             "by ellipsoid."                         );

         //
         // At times[2] with Gamma as the first target (point), the
         // occultationCode should be TOTAL1, representing that Gamma is 
         // completely occulted by Alpha.
         //

         obsrvr = sun;
         abcorr = new AberrationCorrection( "None" );
         et     = new TDBTime( times[2] );

         targ1  = gamma;
         frame1 = gammaFixed;
         shape1 = "POINT";
         targ2  = alpha;
         frame2 = alphaFixed;
         shape2 = "ELLIPSOID";

         occCode = OccultationState.
                   getOccultationState( targ1,  shape1, frame1, 
                                        targ2,  shape2, frame2,
                                        abcorr, obsrvr, et     );


         ok = JNITestutils.chcksi( "occCode",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   TOTAL1.getOccultationCode(),   
                                   0                             );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Point case: Point transiting " +
                             "ellipsoid."                      );

         //
         // At times[4] with Gamma as the first target (point), the
         // occultationCode should be ANNLR2, representing that Gamma is 
         // transiting Alpha.
         //

         obsrvr = sun;
         abcorr = new AberrationCorrection( "None" );
         et     = new TDBTime( times[4] );

         targ1  = gamma;
         frame1 = gammaFixed;
         shape1 = "POINT";
         targ2  = alpha;
         frame2 = alphaFixed;
         shape2 = "ELLIPSOID";

         occCode = OccultationState.
                   getOccultationState( targ1,  shape1, frame1, 
                                        targ2,  shape2, frame2,
                                        abcorr, obsrvr, et     );


         ok = JNITestutils.chcksi( "occCode",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   ANNLR2.getOccultationCode(),   
                                   0                             );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Verify results using different aberration " +
                             "corrections."                                );
 
         //
         // At the time below, if the abcorr of 'none' is used, a partial
         // occultation is reported.  If 'xcn' is used, an annular transit
         // is reported. The time and occultation types were calculated
         // using GFOCLT.
         //

         et = new TDBTime( "2000-JAN-03 09:58:02 (TDB)" );

         // 
         // Calculate the occultation type with no aberration correction.
         // Check that the result is PARTL2.
         //
         obsrvr = sun;
         abcorr = new AberrationCorrection( "None" );

         targ1  = gamma;
         frame1 = gammaFixed;
         shape1 = "ELLIPSOID";
         targ2  = alpha;
         frame2 = alphaFixed;
         shape2 = "ELLIPSOID";

         occCode = OccultationState.
                   getOccultationState( targ1,  shape1, frame1, 
                                        targ2,  shape2, frame2,
                                        abcorr, obsrvr, et     );


         ok = JNITestutils.chcksi( "occCode using 'NONE'",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   PARTL2.getOccultationCode(),   
                                   0                             );

         abcorr = new AberrationCorrection( "XCN" );         

         occCode = OccultationState.
                   getOccultationState( targ1,  shape1, frame1, 
                                        targ2,  shape2, frame2,
                                        abcorr, obsrvr, et     );

         // 
         // Calculate the occultation type with aberration correction
         // set to XCN. Check that the result is ANNLR2.
         //

         ok = JNITestutils.chcksi( "occCode using 'XCN'",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   ANNLR2.getOccultationCode(),   
                                   0                             );


         // ***********************************************************
         //
         //   DSK tests
         //
         // ***********************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Create a DSK containing shape models for " +
                             "bodies Alpha and Beta."                     );


         //
         // Make sure the generic SPK is unloaded! We need the sun ephemeris
         // from Nat's SPK.
         //
         CSPICE.spkuef( han2 );
 

         //
         // This block is suitable for use in F_GFOCLT. We don't actually
         // have to have the high-resolution DSK patches to test OCCULT.
         //
         // We'll enhance the generic DSK models for Alpha and Beta by
         // appending segments containing small patches of high-resolution
         // data for the surface regions that participate in computation of
         // accurate occultation ingress and egress times. We use small
         // patches because the size and run time needed to create full
         // models at this resolution would be prohibitive.
         //
         // Start by creating the basic DSK.
         //
         ( new File( DSK ) ).delete();

         //
         // Use low resolution tessellations for the main models.
         //
         nlon   = 20;
         nlat   = 10;

         aframe = alphaFixed;
         bframe = betaFixed;

         JNITestutils.natdsk( DSK, aframe.getName(), nlon, nlat,
                                   bframe.getName(), nlon, nlat );

         //
         // Create the -Y patch for body Alpha. The patch covers
         // the lon/lat rectangle
         //
         //   -92 deg. <= lon <= -88 deg.
         //     0 deg. <= lat <=   4 deg.
         //
         // Note that Alpha' body-fixed Z axis lies in Alpha's orbital
         // plane.
         //
         frame  =  aframe;
         bodyid =  1000;
         surfid =  2;
         first  = -100 * CSPICE.jyear();
         last   =  100 * CSPICE.jyear();
 
         arad   = alpha.getValues( "RADII" );

         // 
         // Make the patch spherical, using the ellipsoid's Z
         // semi-axis length.        
         //
         c      = arad[2];
   
         corsys = DSKDescriptor.LATSYS;

         for ( i = 0;  i < DSKDescriptor.NSYPAR;  i++ )
         {
            corpar[i] = 0.0;
         }

         makvtl = false;

         bounds[0][0] = (-92.0)*RPD;
         bounds[0][1] = (-88.0)*RPD;
         bounds[1][0] = (  0.0)*RPD;
         bounds[1][1] = (  4.0)*RPD;

         nlon  = 200;
         nlat  = 200;

         usepad = true;

         //
         // Append the patch segment to the existing DSK.
         //
         JNITestutils.t_secds2( bodyid, surfid, frame.getName(), first,  last,
                                corsys, corpar, bounds,          c,      c,
                                c,      nlon,   nlat,            makvtl, usepad,
                                DSK                                           );
         //
         // Create the +Y patch for body Alpha. The patch covers
         // the lon/lat rectangle
         //
         // 88 deg. <= lon <=  92 deg.
         // 0 deg. <= lat <=   4 deg.
         //

         bounds[0][0] = ( 92.0)*RPD;
         bounds[0][1] = ( 88.0)*RPD;
         bounds[1][0] = (  0.0)*RPD;
         bounds[1][1] = (  4.0)*RPD;

         nlon  = 200;
         nlat  = 200;

         usepad = true;

         //
         // Append the patch segment to the existing DSK.
         //
         JNITestutils.t_secds2( bodyid, surfid, frame.getName(), first,  last,
                                corsys, corpar, bounds,          c,      c,
                                c,      nlon,   nlat,            makvtl, usepad,
                                DSK                                           );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Check for non-occultation just prior to " +
                             "transit start, for point BETA and DSK "   +
                             "ALPHA. BETA is body 1."                    );


         //
         // Make sure we're using constants for Nat's solar system.
         // 
         JNITestutils.natpck( PCK1, true, false );

         KernelDatabase.load( DSK );

         //
         // Use a time 2 microseconds before the nominal start of occultation.
         //
         et     = new TDBTime( "2000 JAN 1 12:00:59.999998 TDB" );

         abcorr = new AberrationCorrection( "None" );
        
         occCode = OccultationState.
                   getOccultationState( beta,   "POINT",             betaFixed,
                                        alpha,  "DSK/UNPRIORITIZED", alphaFixed,
                                        abcorr, sun,                 et       );


         ok = JNITestutils.chcksi( "occCode",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   NOOCC.getOccultationCode(),   
                                   0                             );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Check for non-occultation just prior to " +
                             "transit start, for point BETA and DSK "   +
                             "ALPHA. BETA is body 2."                     );

         occCode = OccultationState.
                   getOccultationState( alpha,  "DSK/UNPRIORITIZED", alphaFixed,
                                        beta,   "POINT",             betaFixed,
                                        abcorr, sun,                 et       );


         ok = JNITestutils.chcksi( "occCode",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   NOOCC.getOccultationCode(),   
                                   0                             );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Check for annular transit just after " +
                             "transit start, for point BETA and DSK "    +
                             "ALPHA. BETA is body 1."                     );

         //
         // Use a time 2 microseconds after the nominal start of occultation.
         //
         et = new TDBTime( "2000 JAN 1 12:01:00.000002 TDB" );


         occCode = OccultationState.
                   getOccultationState( beta,   "POINT",             betaFixed,
                                        alpha,  "DSK/UNPRIORITIZED", alphaFixed,
                                        abcorr, sun,                 et       );
 

         ok = JNITestutils.chcksi( "occCode",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   ANNLR2.getOccultationCode(),   
                                   0                             );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Check for annular transit just after " +
                             "transit start, for point BETA and DSK "    +
                             "ALPHA. BETA is body 2."                     );

         occCode = OccultationState.
                   getOccultationState( alpha,  "DSK/UNPRIORITIZED", alphaFixed,
                                        beta,   "POINT",             betaFixed,
                                        abcorr, sun,                 et       );


         ok = JNITestutils.chcksi( "occCode",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   ANNLR1.getOccultationCode(),   
                                   0                             );

         //
         // In the tests below, BETA is modeled by a DSK and ALPHA is 
         // treated as a point.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Check for full occultation at the midpoint of " +
                             "the occultation interval, for DSK BETA and "    +
                             "point ALPHA. BETA is body 1."                  );


         et = new TDBTime( "2000 JAN 1 12:05:00 TDB" );

         occCode = OccultationState.
                   getOccultationState( beta,  "DSK/UNPRIORITIZED", betaFixed,
                                        alpha, "POINT",             alphaFixed,
                                        abcorr, sun,                et       );


         ok = JNITestutils.chcksi( "occCode",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   TOTAL2.getOccultationCode(),   
                                   0                             );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Check for full occultation at the midpoint of " +
                             "the occultation interval, for DSK BETA and "    +
                             "point ALPHA. BETA is body 2."                  );

         occCode = OccultationState.
                   getOccultationState( alpha, "POINT",             alphaFixed,
                                        beta,  "DSK/UNPRIORITIZED", betaFixed,
                                        abcorr, sun,                et       );


         ok = JNITestutils.chcksi( "occCode",
                                   occCode.getOccultationCode(),
                                   "=",   
                                   TOTAL1.getOccultationCode(),   
                                   0                             );

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
         JNITestutils.tcase( "Clean up. Unload and delete kernels." );

         CSPICE.spkuef( han1 );
         CSPICE.spkuef( han2 );

         KernelDatabase.clear();

         ( new File(SPK1) ).delete();
         ( new File(SPK2) ).delete();    
         ( new File(PCK1) ).delete();   
         ( new File(PCK2) ).delete();      
         ( new File(DSK)  ).delete();

      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

