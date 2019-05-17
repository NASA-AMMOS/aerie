
package spice.tspice;


import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;



/**
Class TestSPK provides methods that implement test families for
the class SPK.

<p>Version 1.0.0 04-JAN-2010 (NJB)
*/
public class TestSPK extends Object
{

   //
   // Class constants
   //
   private static String  REF1          = "J2000";
   private static String  SPK1          = "test1.bsp";
   private static String  SPK12         = "test12.bsp";
   private static String  SPK12BIG      = "test12big.bsp";
   private static String  SPK12SUB      = "test12sub.bsp";
   private static String  SPK13         = "test13.bsp";
   private static String  SPK13BIG      = "test13big.bsp";
   private static String  SPK13SUB      = "test13sub.bsp";
   private static String  SPK14         = "test14.bsp";
   private static String  SPK2          = "test2.bsp";
   private static String  SPK3          = "test3.bsp";
   private static String  SPK5          = "test5.bsp";
   private static String  SPK5SUB       = "test5sub.bsp";
   private static String  SPK8          = "test8.bsp";
   private static String  SPK9          = "test9.bsp";
   private static String  SPK9SUB       = "test9sub.bsp";
   private static String  UTC1          = "1999 jul 1";

   private static double  BIG_STEP      = 10.0;
   private static double  GM_SUN        = 132712440023.310;
   private static double  LOOSE_RE      = 1.e-3;
   private static double  TIGHT_RE      = 1.e-14;
   private static double  VERY_LOOSE_RE = 0.1;

   private static int     BIG_CTR       = 5;
   private static int     BIG_DEG       = 3;
   private static int     BIG_ID        = -10000;
   private static int     BIG_N         = 1000;
   private static int     CHBDEG        = 2;
   private static int     LNSIZE        = 81;
   private static int     NUMCAS        = 10;
   private static int     N_DISCRETE    = 9;
   private static int     N_RECORDS     = 4;
   private static int     POLY_DEG      = 3;
   private static int     SIDLEN        = 61;


   //
   // Class variables
   //

   //
   // The following state vectors are from de405s.bsp. They're for the
   // UTC epoch 1999 July 1 00:00:00.  Approximate light times were
   // obtained by dividing distance by c.
   //
   private static double[]      earthSunGeo  =
                                {
                                   23171841.660,
                                  -137908150.483,
                                  -59790964.759,
                                   28.946,
                                   4.054,
                                   1.759
                                };

   private static double      earthSunGeoLT = 507.30867946318;


   private static double[]      earthSunLT  =
                                {
                                   23157154.182,
                                   -137910199.678,
                                   -59791853.752,
                                   28.946,
                                   4.052,
                                   1.758
                                };

   private static double        earthSunLTLT = 507.30858140200;


   private static double[]      earthSunLTS  =
                                {
                                   23157155.813,
                                   -137910199.435,
                                   -59791853.683,
                                   28.946,
                                   4.052,
                                   1.758
                                };


   static String[]              LTs =
                                {
                                   "none",
                                   "LT",
                                   "LT+S"
                                };

   static String[]              ltPhrases =
                                {
                                   "geometric",
                                   "LT-corrected",
                                   "LT+S_corrected"
                                };

   //
   // Note:  this is *supposed* to be the same as LT only, since the
   //range from which it was derived is the same.
   //
   private static double        earthSunLTSLT = 507.30858140200;


   //
   // Cheby coefficients for testing writeType02Segment
   //
   private static double[][][]  ChebyCoeffs02 =
                                {
                                   {
                                      { 1.0101, 1.0102, 1.0103 },
                                      { 1.0201, 1.0202, 1.0203 },
                                      { 1.0301, 1.0302, 1.0303 }
                                   },

                                   {
                                      { 2.0101, 2.0102, 2.0103 },
                                      { 2.0201, 2.0202, 2.0203 },
                                      { 2.0301, 2.0302, 2.0303 }
                                   },

                                   {
                                      { 3.0101, 3.0102, 3.0103 },
                                      { 3.0201, 3.0202, 3.0203 },
                                      { 3.0301, 3.0302, 3.0303 }
                                   },

                                   {
                                      { 4.0101, 4.0102, 4.0103 },
                                      { 4.0201, 4.0202, 4.0203 },
                                      { 4.0301, 4.0302, 4.0303 }
                                   }
                                };



   //
   // Cheby coefficients for testing writeType03Segment
   //

   private static double[][][]  ChebyCoeffs03 =
                                {
                                   {
                                      { 1.0101, 1.0102, 1.0103 },
                                      { 1.0201, 1.0202, 1.0203 },
                                      { 1.0301, 1.0302, 1.0303 },
                                      { 1.0401, 1.0402, 1.0403 },
                                      { 1.0501, 1.0502, 1.0503 },
                                      { 1.0601, 1.0602, 1.0603 }
                                   },

                                   {
                                      { 2.0101, 2.0102, 2.0103 },
                                      { 2.0201, 2.0202, 2.0203 },
                                      { 2.0301, 2.0302, 2.0303 },
                                      { 2.0401, 2.0402, 2.0403 },
                                      { 2.0501, 2.0502, 2.0503 },
                                      { 2.0601, 2.0602, 2.0603 }
                                   },

                                   {
                                      { 3.0101, 3.0102, 3.0103 },
                                      { 3.0201, 3.0202, 3.0203 },
                                      { 3.0301, 3.0302, 3.0303 },
                                      { 3.0401, 3.0402, 3.0403 },
                                      { 3.0501, 3.0502, 3.0503 },
                                      { 3.0601, 3.0602, 3.0603 }
                                   },

                                   {
                                      { 4.0101, 4.0102, 4.0103 },
                                      { 4.0201, 4.0202, 4.0203 },
                                      { 4.0301, 4.0302, 4.0303 },
                                      { 4.0401, 4.0402, 4.0403 },
                                      { 4.0501, 4.0502, 4.0503 },
                                      { 4.0601, 4.0602, 4.0603 }
                                   }
                                };


   //
   // Cheby coefficient and interval records for testing
   //
   //     beginType14Segment
   //     appendToType14Segment
   //     endType14Segment
   //
   private static double[][]    ChebyRecords14 =
                                {
                                   {
                                        150.0,
                                        50.0,
                                        1.0101, 1.0102, 1.0103,
                                        1.0201, 1.0202, 1.0203,
                                        1.0301, 1.0302, 1.0303,
                                        1.0401, 1.0402, 1.0403,
                                        1.0501, 1.0502, 1.0503,
                                        1.0601, 1.0602, 1.0603
                                   },

                                   {
                                        250.0,
                                        50.0,
                                        2.0101, 2.0102, 2.0103,
                                        2.0201, 2.0202, 2.0203,
                                        2.0301, 2.0302, 2.0303,
                                        2.0401, 2.0402, 2.0403,
                                        2.0501, 2.0502, 2.0503,
                                        2.0601, 2.0602, 2.0603
                                   },

                                   {
                                        350.0,
                                        50.0,
                                        3.0101, 3.0102, 3.0103,
                                        3.0201, 3.0202, 3.0203,
                                        3.0301, 3.0302, 3.0303,
                                        3.0401, 3.0402, 3.0403,
                                        3.0501, 3.0502, 3.0503,
                                        3.0601, 3.0602, 3.0603
                                   },

                                   {
                                        450.0,
                                        50.0,
                                        4.0101, 4.0102, 4.0103,
                                        4.0201, 4.0202, 4.0203,
                                        4.0301, 4.0302, 4.0303,
                                        4.0401, 4.0402, 4.0403,
                                        4.0501, 4.0502, 4.0503,
                                        4.0601, 4.0602, 4.0603
                                   }
                                };


   //
   // States for testing
   //
   //     writeType05Segment
   //     writeType08Segment
   //     writeType09Segment
   //     writeType12Segment
   //     writeType13Segment
   //

   private static double[]      discreteEpochs  =
                                {
                                   100., 200., 300., 400., 500.,
                                   600., 700., 800., 900.
                                };

   private static double[][]    discreteStates =
                                {
                                   { 101., 201., 301., 401., 501., 601. },
                                   { 102., 202., 302., 402., 502., 602. },
                                   { 103., 203., 303., 403., 503., 603. },
                                   { 104., 204., 304., 404., 504., 604. },
                                   { 105., 205., 305., 405., 505., 605. },
                                   { 106., 206., 306., 406., 506., 606. },
                                   { 107., 207., 307., 407., 507., 607. },
                                   { 108., 208., 308., 408., 508., 608. },
                                   { 109., 209., 309., 409., 509., 609. },
                                };


   private static final int[] SPK1Bods = {

      -9,
      1,
      2,
      3,
      4,
      5,
      6,
      7,
      8,
      9,
      10,
      199,
      299,
      301,
      399,
      401,
      402,
      499,
      501,
      502,
      503,
      504,
      599,
      603,
      604,
      605,
      606,
      607,
      608,
      699,
      701,
      702,
      703,
      704,
      705,
      799,
      801,
      802,
      899,
      901,
      999,
      301001,
      399001,
      399002,
      399003,
      401001
   };


   //
   // Methods
   //


   /**
   Evaluate an nth degree Chebyshev polynomial at angle theta.
   */
   private static double T( int n, double theta )
   {
      return( Math.cos( n*Math.acos( Math.min( 1.0, Math.max( -1.0,
                                                              theta ) ) ) ) );
   }




   /**
   Test SPK methods.
   */
   public static boolean f_SPK()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;


      //
      // Local variables
      //

      boolean                           ok;

      double                            etsec;
      double[]                          expState = new double[6];
      double                            lt;
      double                            midpt;
      double                            radius;
      double                            theta;

      int[]                             bodyList;
      int                               expBody;
      int[]                             expBodyList0;
      int[]                             expBodyList1;
      int                               expCenter;
      int                               handle;
      int                               i;
      int                               n;

      String                            expRef;
      String                            timstr;
      String                            segid;

      AberrationCorrection              abcorr;

      Body                              observer;
      Body                              target;

      ReferenceFrame                    J2000;
      ReferenceFrame                    ref;

      SPK                               spk1;
      SPK                               spk2;
      SPK                               spk3;
      SPK                               spk3e;
      SPK                               spk5;
      SPK                               spk9;
      SPK                               spk13;

      SpiceWindow                       cover;
      SpiceWindow                       initCover;
      SpiceWindow                       xCover;

      StateRecord                       sr;

      TDBDuration                       intlen;

      TDBTime[]                         epochs;
      TDBTime                           et;

      StateVector[]                     states;


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

         JNITestutils.topen ( "f_SPK" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: load lsk; create SPK." );


         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();

         JNITestutils.tstlsk();


         //
         // Delete SPK1 if it exists. Create but do not load a new
         // version of the file.
         //
         ( new File ( SPK1 ) ).delete();

         handle = JNITestutils.tstspk( SPK1, false );



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Try to compute state with no SPKs loaded." );

         try
         {
            timstr      = "2009 Oct 16 00:00:00 UTC";

            abcorr      = new AberrationCorrection( "LT+S"  );
            ref         = new ReferenceFrame      ( "J2000" );
            observer    = new Body                ( "Earth" );
            target      = new Body                ( "Moon"  );
            et          = new TDBTime             ( timstr  );

            sr          = new StateRecord( target, et, ref, abcorr, observer );

            /*
            If an exception is *not* thrown, we'll hit this call.
            */

            Testutils.dogDidNotBark ( "SPICE(NOLOADEDFILES)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(NOLOADEDFILES)", ex );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Open new SPK with empty file name." );

         //
         // Open a new SPK file. First delete any existing file of
         // the name we want to use.
         //
         ( new File ( SPK3 ) ).delete();

         spk3e = null;

         try
         {
            spk3e = SPK.openNew(   "", "Type 3 SPK internal file name.",  4 );

            /*
            If an exception is *not* thrown, we'll hit this call.
            */

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         if ( spk3e != null )
         {
            spk3e.close();
         }

         ( new File ( SPK3 ) ).delete();



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Open new SPK with empty internal file name." );

         //
         // Open a new SPK file. First delete any existing file of
         // the name we want to use.
         //
         ( new File ( SPK3 ) ).delete();

         spk3e = null;

         try
         {
            spk3e = SPK.openNew(  SPK3, "",  4 );

            /*
            If an exception is *not* thrown, we'll hit this call.
            */

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         if ( spk3e != null )
         {
            spk3e.close();
         }

         ( new File ( SPK3 ) ).delete();




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Call writeType03Segment with empty SEGID." );

         expBody   = 3;
         expCenter = 10;
         expRef    =  "J2000";

         //
         // Create a segment identifier.
         //
         segid = "";

         //
         // Open a new SPK file. First delete any existing file of
         // the name we want to use.
         //
         ( new File ( SPK3 ) ).delete();

         spk3e = SPK.openNew(   SPK3, "Type 3 SPK internal file name.",  4 );


         //
         // Create a type 3 segment.
         //

         epochs = new TDBTime[N_RECORDS+1];

         for ( i = 0;  i < N_RECORDS+1;  i++ )
         {
            epochs[i] = new TDBTime( discreteEpochs[i] );
         }

         intlen = epochs[1].sub( epochs[0] );

         try
         {

            spk3e.writeType03Segment ( new Body( expBody   ),
                                       new Body( expCenter ),
                                       new ReferenceFrame( expRef ),
                                       epochs[0],
                                       epochs[N_RECORDS],
                                       segid,
                                       intlen,
                                       N_RECORDS,
                                       CHBDEG,
                                       ChebyCoeffs03,
                                       epochs[0]              );


            /*
            If an exception is *not* thrown, we'll hit this call.
            */

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         spk3e.close();

         ( new File ( SPK3 ) ).delete();



         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test StateRecord principal constructor. "   +
                              "Get the geometric state of the "  +
                              "earth relative to the sun."         );

         //
         // Load the generic test SPK.
         //
         KernelDatabase.load( SPK1 );

         target   = new Body( "earth" );

         et       = new TDBTime( UTC1 );

         J2000    = new ReferenceFrame( "J2000" );

         abcorr   = new AberrationCorrection( "NONE" );

         observer = new Body( "sun" );

         sr =       new StateRecord ( target, et, J2000, abcorr, observer );

         ok = JNITestutils.chckad( "StateRecord geometric position",
                                   sr.getPosition().toArray(),
                                   "~~/",
                                   earthSunGeo,
                                   LOOSE_RE            );

         //
         // Arrays.copyOfRange is not supported until Java 1.6, so work
         // around this limitation.
         //

         double[] vec = new double[3];

         System.arraycopy ( earthSunGeo, 3, vec, 0, 3 );

         ok = JNITestutils.chckad( "StateRecord geometric velocity",
                                   sr.getVelocity().toArray(),
                                   "~~/",
                                   vec,
                                   // Arrays.copyOfRange(earthSunGeo, 3, 6 ),
                                   LOOSE_RE                                 );


         lt = sr.getLightTime().getMeasure();

         ok = JNITestutils.chcksd( "StateRecord geometric light time",
                                   lt,
                                   "~/",
                                   earthSunGeoLT,
                                   LOOSE_RE                           );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test StateRecord principal constructor. "   +
                              "Get the LT-corrected state of the "  +
                              "earth relative to the sun."         );


         abcorr = new AberrationCorrection( "lt" );

         sr     = new StateRecord ( target, et, J2000, abcorr, observer );

         ok = JNITestutils.chckad( "StateRecord LT-corrected position",
                                   sr.getPosition().toArray(),
                                   "~~/",
                                   earthSunLT,
                                   LOOSE_RE            );

         System.arraycopy ( earthSunLT, 3, vec, 0, 3 );

         ok = JNITestutils.chckad( "StateRecord LT-corrected velocity",
                                   sr.getVelocity().toArray(),
                                   "~~/",
                                   vec,
                                   // Arrays.copyOfRange(earthSunLT, 3, 6 ),
                                   LOOSE_RE                                 );


         lt = sr.getLightTime().getMeasure();

         ok = JNITestutils.chcksd( "StateRecord LT-corrected light time",
                                   lt,
                                   "~/",
                                   earthSunLTLT,
                                   LOOSE_RE                           );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test StateRecord principal constructor. "   +
                              "Get the LTS-corrected state of the "  +
                              "earth relative to the sun."         );


         abcorr = new AberrationCorrection( "lt+s" );

         sr     = new StateRecord ( target, et, J2000, abcorr, observer );

         ok = JNITestutils.chckad( "StateRecord LTS-corrected position",
                                   sr.getPosition().toArray(),
                                   "~~/",
                                   earthSunLTS,
                                   LOOSE_RE            );

         System.arraycopy ( earthSunLTS, 3, vec, 0, 3 );

         ok = JNITestutils.chckad( "StateRecord LTS-corrected velocity",
                                   sr.getVelocity().toArray(),
                                   "~~/",
                                   vec,
                                   // Arrays.copyOfRange(earthSunLTS, 3, 6 ),
                                   LOOSE_RE                                 );


         lt = sr.getLightTime().getMeasure();

         ok = JNITestutils.chcksd( "StateRecord LTS-corrected light time",
                                   lt,
                                   "~/",
                                   earthSunLTSLT,
                                   LOOSE_RE                           );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test writeType02Segment." );

         expBody   = 2;
         expCenter = 10;
         expRef    =  "J2000";


         //
         // Create a segment identifier.
         //
         segid = "SPK type 2 test segment";


         //
         // Open a new SPK file. First delete any existing file of
         // the name we want to use.
         //
         ( new File ( SPK2 ) ).delete();

         spk2 = SPK.openNew(   SPK2, "Type 2 SPK internal file name.",  4 );


         //
         // Create a type 2 segment.
         //
         n      = N_RECORDS;

         epochs = new TDBTime[n+1];

         for ( i = 0;  i < n+1;  i++ )
         {
            epochs[i] = new TDBTime( discreteEpochs[i] );
         }

         intlen = epochs[1].sub( epochs[0] );


         spk2.writeType02Segment ( new Body( expBody   ),
                                   new Body( expCenter ),
                                   new ReferenceFrame( expRef ),
                                   epochs[0],
                                   epochs[n],
                                   segid,
                                   intlen,
                                   n,
                                   CHBDEG,
                                   ChebyCoeffs02,
                                   epochs[0]              );


         //
         // Close the SPK file.
         //
         spk2.close();


         //
         // Load the SPK file.
         //
         KernelDatabase.load( SPK2 );


         //
         // Look up states for each epoch in our list.  Compare.
         //

         for ( i = 0;  i < N_RECORDS; i++ )
         {
            radius = 0.5 * intlen.getMeasure();
            midpt  = discreteEpochs[i] + radius;

            etsec  =  midpt +  (0.5*radius);

            sr     =  new StateRecord( new Body(expBody),
                                       new TDBTime( etsec ),
                                       new ReferenceFrame( expRef ),
                                       new AberrationCorrection( "None" ),
                                       new Body( expCenter )               );
            //
            // Evaluate the position manually.
            //
            theta = ( etsec - midpt ) / radius;

            for ( int j = 0; j < 3; j++ )
            {
               expState[j] = 0.;

               for ( int k = 0; k <= CHBDEG; k++ )
               {
                  expState[j] += ( ChebyCoeffs02[i][j][k] * T( k, theta ) );
               }
            }

            System.arraycopy( expState, 0, vec, 0, 3 );

            ok = JNITestutils.chckad ( "<type 2 position>",
                                       sr.getPosition().toArray(),
                                       "~",
                                       vec,
                                       // Arrays.copyOfRange(expState, 0, 3),
                                       TIGHT_RE                            );
         }

         KernelDatabase.unload( SPK2 );

         ( new File ( SPK2 ) ).delete();



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test writeType03Segment." );

         expBody   = 3;
         expCenter = 10;
         expRef    =  "J2000";

         //
         // Create a segment identifier.
         //
         segid = "SPK type 3 test segment";

         //
         // Open a new SPK file. First delete any existing file of
         // the name we want to use.
         //
         ( new File ( SPK3 ) ).delete();

         spk3 = SPK.openNew(   SPK3, "Type 3 SPK internal file name.",  4 );


         //
         // Create a type 3 segment.
         //
         n      = N_RECORDS;

         epochs = new TDBTime[n+1];

         for ( i = 0;  i < n+1;  i++ )
         {
            epochs[i] = new TDBTime( discreteEpochs[i] );
         }

         intlen = epochs[1].sub( epochs[0] );


         spk3.writeType03Segment ( new Body( expBody   ),
                                   new Body( expCenter ),
                                   new ReferenceFrame( expRef ),
                                   epochs[0],
                                   epochs[n],
                                   segid,
                                   intlen,
                                   n,
                                   CHBDEG,
                                   ChebyCoeffs03,
                                   epochs[0]              );


         //
         // Close the SPK file.
         //
         spk3.close();


         //
         // Load the SPK file.
         //
         KernelDatabase.load( SPK3 );


         //
         // Look up states for each epoch in our list.  Compare.
         //

         for ( i = 0;  i < N_RECORDS; i++ )
         {
            radius = 0.5 * intlen.getMeasure();
            midpt  = discreteEpochs[i] + radius;

            etsec  =  midpt +  (0.5*radius);

            sr     =  new StateRecord( new Body(expBody),
                                       new TDBTime( etsec ),
                                       new ReferenceFrame( expRef ),
                                       new AberrationCorrection( "None" ),
                                       new Body( expCenter )               );
            //
            // Evaluate the state manually.
            //
            theta = ( etsec - midpt ) / radius;

            for ( int j = 0; j < 6; j++ )
            {
               expState[j] = 0.;

               for ( int k = 0; k <= CHBDEG; k++ )
               {
                  expState[j] += ( ChebyCoeffs03[i][j][k] * T( k, theta ) );
               }
            }


            ok = JNITestutils.chckad ( "<type 3 state>",
                                       sr.getPosition().toArray(),
                                       "~",
                                       expState,
                                       TIGHT_RE                    );
         }

         KernelDatabase.unload( SPK3 );

         ( new File ( SPK3 ) ).delete();





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test writeType05Segment." );

         expBody   = 5;
         expCenter = 10;
         expRef    =  "J2000";

         //
         // Create a segment identifier.
         //
         segid = "SPK type 5 test segment";

         //
         // Open a new SPK file. First delete any existing file of
         // the name we want to use.
         //
         ( new File ( SPK5 ) ).delete();

         spk5 = SPK.openNew(   SPK5, "Type 5 SPK internal file name.",  4 );


         //
         // Create a type 5 segment.
         //
         n = N_DISCRETE;

         epochs = new TDBTime[n];
         states = new StateVector[n];

         for ( i = 0;  i < n;  i++ )
         {
            epochs[i] = new TDBTime( discreteEpochs[i] );

            states[i] = new StateVector( discreteStates[i] );
         }

         //
         // Create a type 05 segment.
         //

         spk5.writeType05Segment ( new Body( expBody   ),
                                   new Body( expCenter ),
                                   new ReferenceFrame( expRef ),
                                   epochs[0],
                                   epochs[N_DISCRETE-1],
                                   segid,
                                   GM_SUN,
                                   N_DISCRETE,
                                   states,
                                   epochs                     );


         //
         // Close the SPK file.
         //
         spk5.close();


         //
         // Load the SPK file.
         //
         KernelDatabase.load( SPK5 );


         //
         // Look up states for each epoch in our list.  Compare.
         //

         for ( i = 0;  i < N_DISCRETE; i++ )
         {
            etsec  =  discreteEpochs[i];

            sr     =  new StateRecord( new Body(expBody),
                                       new TDBTime( etsec ),
                                       new ReferenceFrame( expRef ),
                                       new AberrationCorrection( "None" ),
                                       new Body( expCenter )               );


            ok = JNITestutils.chckad ( "<type 5 state>",
                                       sr.toArray(),
                                       "~",
                                       discreteStates[i],
                                       TIGHT_RE                    );
         }

         KernelDatabase.unload( SPK5 );

         ( new File ( SPK5 ) ).delete();





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test writeType09Segment." );

         expBody   = 9;
         expCenter = 10;
         expRef    =  "J2000";

         //
         // Create a segment identifier.
         //
         segid = "SPK type 09 test segment";

         //
         // Open a new SPK file. First delete any existing file of
         // the name we want to use.
         //
         ( new File ( SPK9 ) ).delete();

         spk9 = SPK.openNew( SPK9, "Type 09 SPK internal file name.",  4 );

         n = N_DISCRETE;

         epochs = new TDBTime[n];
         states = new StateVector[n];

         for ( i = 0;  i < n;  i++ )
         {
            epochs[i] = new TDBTime( discreteEpochs[i] );

            states[i] = new StateVector( discreteStates[i] );
         }

         //
         // Create a type 09 segment.
         //

         spk9.writeType09Segment ( new Body( expBody   ),
                                   new Body( expCenter ),
                                   new ReferenceFrame( expRef ),
                                   epochs[0],
                                   epochs[N_DISCRETE-1],
                                   segid,
                                   POLY_DEG,
                                   N_DISCRETE,
                                   states,
                                   epochs                     );

         //
         // Close the SPK file.
         //
         spk9.close();

         //
         // Load the SPK file.
         //
         KernelDatabase.load( SPK9 );

         //
         // Look up states for each epoch in our list.  Compare.
         //

         for ( i = 0;  i < N_DISCRETE; i++ )
         {
            etsec  =  discreteEpochs[i];

            sr     =  new StateRecord( new Body(expBody),
                                       new TDBTime( etsec ),
                                       new ReferenceFrame( expRef ),
                                       new AberrationCorrection( "None" ),
                                       new Body( expCenter )               );


            ok = JNITestutils.chckad ( "<type 9 state>",
                                       sr.toArray(),
                                       "~",
                                       discreteStates[i],
                                       TIGHT_RE                    );
         }

         KernelDatabase.unload( SPK9 );

         ( new File ( SPK9 ) ).delete();




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test writeType13Segment." );

         expBody   = 13;
         expCenter = 10;
         expRef    =  "J2000";

         //
         // Create a segment identifier.
         //
         segid = "SPK type 13 test segment";

         //
         // Open a new SPK file. First delete any existing file of
         // the name we want to use.
         //
         ( new File ( SPK13 ) ).delete();

         spk13 = SPK.openNew(   SPK13, "Type 13 SPK internal file name.",  4 );

         n = N_DISCRETE;

         epochs = new TDBTime[n];
         states = new StateVector[n];

         for ( i = 0;  i < n;  i++ )
         {
            epochs[i] = new TDBTime( discreteEpochs[i] );

            states[i] = new StateVector( discreteStates[i] );
         }





         //
         // Create a type 13 segment.
         //

         spk13.writeType13Segment ( new Body( expBody   ),
                                    new Body( expCenter ),
                                    new ReferenceFrame( expRef ),
                                    epochs[0],
                                    epochs[N_DISCRETE-1],
                                    segid,
                                    POLY_DEG,
                                    N_DISCRETE,
                                    states,
                                    epochs                     );

         //
         // Close the SPK file.
         //
         spk13.close();


         //
         // Load the SPK file.
         //
         KernelDatabase.load( SPK13 );


         //
         // Look up states for each epoch in our list.  Compare.
         //

         for ( i = 0;  i < N_DISCRETE; i++ )
         {
            etsec  =  discreteEpochs[i];

            sr     =  new StateRecord( new Body(expBody),
                                       new TDBTime( etsec ),
                                       new ReferenceFrame( expRef ),
                                       new AberrationCorrection( "None" ),
                                       new Body( expCenter )               );


            ok = JNITestutils.chckad ( "<type 13 state>",
                                       sr.toArray(),
                                       "~",
                                       discreteStates[i],
                                       TIGHT_RE                    );
         }

         KernelDatabase.unload( SPK13 );

         ( new File ( SPK13 ) ).delete();



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getBodies()." );

         spk1 = SPK.openForRead( SPK1 );

         bodyList = spk1.getBodies();

         ok = JNITestutils.chckai ( "SPK1 body list",
                                    bodyList,
                                    "=",
                                    SPK1Bods           );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getBodies( int[] initialSet )." );

         spk1 = SPK.openForRead( SPK1 );

         int[] intSet0 = { 1000887, 1000888, 1000889 };

         n = SPK1Bods.length;

         expBodyList0 = new int[ n + 3 ];

         System.arraycopy( SPK1Bods, 0, expBodyList0, 0, n );
         System.arraycopy( intSet0,  0, expBodyList0, n, 3 );

         bodyList = spk1.getBodies( intSet0 );

         ok = JNITestutils.chckai ( "SPK1 body list",
                                    bodyList,
                                    "=",
                                    expBodyList0       );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getCoverage( int body )." );

         spk1.close();

         spk1 = SPK.openForRead( SPK1 );

         //
         // Get coverage for body 399.
         //
         cover = spk1.getCoverage( 399 );

         xCover = new SpiceWindow();
         xCover.insert( -5e8, 5e8 );


         ok = JNITestutils.chckad ( "cover",
                                    cover.toArray(),
                                    "~/",
                                    xCover.toArray(),
                                    TIGHT_TOL       );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getCoverage( int body )." );

         spk1.close();

         spk1 = SPK.openForRead( SPK1 );

         //
         // Get coverage for body 399.
         //
         cover = spk1.getCoverage( 399 );

         xCover = new SpiceWindow();
         xCover.insert( -5e8, 5e8 );


         ok = JNITestutils.chckad ( "cover",
                                    cover.toArray(),
                                    "~/",
                                    xCover.toArray(),
                                    TIGHT_TOL       );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getCoverage( int body, SpiceWindow " +
                              "cover )." );

         spk1.close();

         spk1 = SPK.openForRead( SPK1 );

         //
         // Create an intial "coverage" window.
         //
         initCover = new SpiceWindow();

         initCover.insert( 6.e8, 7.e8 );

         //
         // Get coverage for body 399.
         //
         cover = spk1.getCoverage( 399, initCover );

         xCover = new SpiceWindow();
         xCover.insert( -5e8, 5e8 );

         xCover = xCover.union( initCover );

         ok = JNITestutils.chckad ( "cover",
                                    cover.toArray(),
                                    "~/",
                                    xCover.toArray(),
                                    TIGHT_TOL       );

         // For debugging:
         //System.out.println( "xCover: " + xCover );

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

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

