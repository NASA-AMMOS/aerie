
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestCK provides methods that implement test families for
the class CK.

<h3>Version 2.0.0 28-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 29-DEC-2009 (NJB)</h3>
*/
public class TestCK extends Object
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
   Test CK and associated classes.
   */
   public static boolean f_CK()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      VTIGHT_TOL = 1.e-14;
      final double                      TIGHT_TOL  = 1.e-12;
      final double                      MED_TOL    = 1.e-9;

      final int                         CLKID     = -9;
      final int                         NPOINT    = 1000;
      final int                         NSEG      = 3;
      final int                         MAXWIN    = 20000;
      final String                      CK0       = "custom0.bc";
      final String                      CK1       = "generic.bc";
      final String                      CK2       = "bogus.bc";
      final String                      SCLKKER   = "generic.tsc";

      //
      // Local variables
      //
      CK                                ck0 = null;
      CK                                ck1 = null;
      CK                                ck2 = null;

      Instrument                        inst;
      Instrument                        inst_9999  = new Instrument(  -9999 );
      Instrument                        inst_10000 = new Instrument( -10000 );
      Instrument                        inst_10001 = new Instrument( -10001 );
      Instrument                        pavrecInst;

      Matrix33                          CMatrix;
      Matrix33                          quotient;
      Matrix33                          r;
      Matrix33                          xMatrix;

      PointingAndAVRecord               pavrec;

      PointingRecord                    prec;

      ReferenceFrame                    pavrecRef;
      ReferenceFrame                    ref;

      SCLK                              clock9 = new SCLK( CLKID );

      SCLKDuration                      intervalLength;
      SCLKDuration                      offset;
      SCLKDuration                      pavrecTol;
      SCLKDuration                      tol;

      SCLKTime                          actualTime;
      SCLKTime                          first;
      SCLKTime                          last;
      SCLKTime                          requestTime;
      SCLKTime[]                        startArray;
      SCLKTime[]                        stopArray;
      SCLKTime                          ticks;
      SCLKTime[]                        timeArray;
      SCLKTime[]                        xIntStart;
      SCLKTime[]                        xIntStop;
      SCLKTime                          xStartSCLK;
      SCLKTime                          xStopSCLK;


      SpiceQuaternion                   q;
      SpiceQuaternion[]                 quatArray;

      SpiceWindow                       cover;
      SpiceWindow                       initialWindow;

      String                            segid;

      TDBTime                           et;
      TDBTime                           xStart;
      TDBTime                           xStop;

      Vector3                           av;
      Vector3[]                         avArray;
      Vector3                           axis;

      boolean                           found;
      boolean                           ok;

      double                            angle;
      double                            angularRate;
      double                            delta;
      double                            deltaSec;
      double[]                          initArray;
      double[]                          ival;
      double                            secPerTick;
      double[]                          rateArray;

      int                               han0 = 0;
      int                               han1 = 0;
      int                               han2 = 0;
      int                               i;
      int[]                             instArray0;
      int[]                             instArray1;
      int                               j;
      int                               n;
      int                               nints;
      int                               xCard;



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

         JNITestutils.topen ( "f_CK" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: create and load kernels." );


         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();

         //
         // An LSK is always handy.
         //
         JNITestutils.tstlsk();

         //
         // Delete CK0 if it exists.
         //
         ( new File ( CK0 ) ).delete();


         //
         // Delete CK1 if it exists. Create and load CK1 and the
         // corresponding SCLK kernel.
         //
         ( new File ( CK1 ) ).delete();

         //
         // Don't load the CK kernel yet; load and keep the SCLK kernel.
         //
         han1 = JNITestutils.tstck3( CK1, SCLKKER, false, true, true );


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: open an existing CK as a new file." );


         try
         {
            //
            // Try to open CK1 (which exists) as a new file.
            //

            ck1 = CK.openNew( CK1, CK1, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(FILEOPENFAIL)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(FILEOPENFAIL)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: open a non-existent CK for " +
                              "read access." );


         try
         {
            ck2 = CK.openForRead( CK2 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(FILENOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(FILENOTFOUND)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: open a non-existent CK for " +
                              "write access." );


         try
         {
            ck2 = CK.openForWrite( CK2 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(FILENOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(FILENOTFOUND)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: write a segment with descriptor " +
                               "bounds out of order."                      );


         try
         {
            ck2 = CK.openNew( CK2, CK2, 0 );


            n      = 1000;

            quatArray   = new SpiceQuaternion[n];
            avArray     = new Vector3[n];

            timeArray   = new SCLKTime[n];
            startArray  = new SCLKTime[1];

            clock9 = new SCLK( -9 );

            first = new SCLKTime( clock9, 1000.0 );
            last  = new SCLKTime( clock9,    0.0 );

            ref   = new ReferenceFrame( "J2000" );

            inst  = new Instrument( -9999 );
            segid = "Segment 1 for instrument -9999";

            nints         = 1;
            startArray[0] = timeArray[0];

            axis   = new Vector3( 1.0, 2.0, 3.0 );
            delta  = 1.e-3 * Math.PI / n;


            for ( i = 0;  i < n;  i++ )
            {
               //
               // Create the ith quaternion.
               //
               angle        = i * delta;

               r            = new Matrix33( axis, angle );

               quatArray[i] = new SpiceQuaternion( r );


               //
               // Create the ith angular velocity vector. Note
               // that the angualar velocity is not consistent
               // with the orientation, nor need it be.
               //
               angularRate = i * delta;

               avArray[i]  = axis.hat().scale( angularRate );

               //
               // Create the ith time tag.
               //
               timeArray[i] = new SCLKTime( clock9,  10 * i );
            }

            nints         = 1;
            startArray[0] = timeArray[0];


            //
            // Write the segment to the file.
            //
            ck2.writeType03Segment( first,      last,       inst,
                                    ref,        true,       segid,
                                    timeArray,  quatArray,  avArray,
                                    startArray  );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(INVALIDDESCRTIME)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(INVALIDDESCRTIME)", ex );
         }

         finally
         {
            ck2.close();
            ( new File(CK2) ).delete();
         }



         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Count the segments in CK1." );

         ck1 = CK.openForRead( CK1 );

         n   = ck1.countSegments();

         ok  = JNITestutils.chcksi( "n", n, "=", 3, 0 );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Get the instrument set for CK1." );

         ck1           = CK.openForRead( CK1 );

         instArray1    = ck1.getInstruments();

         int[] intvals0 = { -10001, -10000, -9999 };


         ok            = JNITestutils.chckai( "instrument set",
                                              instArray1,
                                              "=",
                                              intvals0          );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get the instrument set for CK1, this time " +
                              "merging the results with an existing set."   );

         ck1            = CK.openForRead( CK1 );

         int[] intvals1 = { -10003, -10002, -9998 };
         int[] merged   = { -10003, -10002, -10001, -10000, -9999, -9998 };

         instArray1     = ck1.getInstruments( intvals1 );


         ok             = JNITestutils.chckai( "instrument set",
                                               instArray1,
                                               "=",
                                               merged          );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get the segment-level time coverage window " +
                              "for instrument -9999 in CK1."                 );

         tol       = new SCLKDuration( clock9, 0.0 );

         inst_9999 = new Instrument( -9999 );

         cover     = ck1.getCoverage( inst_9999, true,           "SEGMENT",
                                      tol,       TimeSystem.TDB, MAXWIN    );

         //
         // The documentation of tstck3 says
         //
         // The C-kernel contains a single segment for each of the
         // fictional objects.  These segments give continuous attitude
         // over the time interval
         //
         //    from 1980 JAN 1, 00:00:00.000 (ET)
         //    to   2011 SEP 9, 01:46:40.000 (ET)
         //
         // Verify this for instrument -9999.
         //

         xStart = new TDBTime( "1980 JAN 1, 00:00:00.000 TDB" );
         xStop  = new TDBTime( "2011 SEP 9, 01:46:40.000 TDB" );

         ival = cover.getInterval( 0 );

         ok   = JNITestutils.chcksd( "start time",
                                     ival[0],
                                     "~/",
                                     xStart.getTDBSeconds(),
                                     TIGHT_TOL               );

         ok   = JNITestutils.chcksd( "stop time",
                                     ival[1],
                                     "~/",
                                     xStop.getTDBSeconds(),
                                     TIGHT_TOL               );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Repeat previous test using SCLK as the time " +
                              "system. Also set the NEEDAV flag to false."   );

         tol       = new SCLKDuration( clock9, 0.0 );

         inst_9999 = new Instrument( -9999 );

         cover     = ck1.getCoverage( inst_9999, false,           "SEGMENT",
                                      tol,       TimeSystem.SCLK, MAXWIN    );

         //
         // The documentation of tstck3 says
         //
         // The C-kernel contains a single segment for each of the
         // fictional objects.  These segments give continuous attitude
         // over the time interval
         //
         //    from 1980 JAN 1, 00:00:00.000 (ET)
         //    to   2011 SEP 9, 01:46:40.000 (ET)
         //
         // Verify this for instrument -9999.
         //

         xStart = new TDBTime( "1980 JAN 1, 00:00:00.000 TDB" );
         xStop  = new TDBTime( "2011 SEP 9, 01:46:40.000 TDB" );

         xStartSCLK = new SCLKTime( clock9, xStart );
         xStopSCLK  = new SCLKTime( clock9, xStop  );

         ival = cover.getInterval( 0 );

         ok   = JNITestutils.chcksd( "start SCLK time",
                                     ival[0],
                                     "~/",
                                     xStartSCLK.getContinuousTicks(),
                                     TIGHT_TOL               );

         ok   = JNITestutils.chcksd( "stop SCLK time",
                                     ival[1],
                                     "~/",
                                     xStopSCLK.getContinuousTicks(),
                                     TIGHT_TOL               );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Repeat previous test using SCLK as the time " +
                              "system and INTERVAL as the coverage level."   );

         tol       = new SCLKDuration( clock9, 0.0 );

         inst_9999 = new Instrument( -9999 );

         cover     = ck1.getCoverage( inst_9999, true,            "INTERVAL",
                                      tol,       TimeSystem.SCLK, MAXWIN     );

         //
         // The documentation of tstck3 says
         //
         // The C-kernel contains a single segment for each of the
         // fictional objects.  These segments give continuous attitude
         // over the time interval
         //
         //    from 1980 JAN 1, 00:00:00.000 (ET)
         //    to   2011 SEP 9, 01:46:40.000 (ET)
         //
         // Verify this for instrument -9999.
         //


         //
         // Since the coverage has no gaps, we expect no change from the
         // segment-level coverage. *Note that this doesn't properly
         // test transmission of the "level" argument.* Later, we create a
         // CK segment with gaps so we can test this functionality.
         //
         xStart = new TDBTime( "1980 JAN 1, 00:00:00.000 TDB" );
         xStop  = new TDBTime( "2011 SEP 9, 01:46:40.000 TDB" );

         xStartSCLK = new SCLKTime( clock9, xStart );
         xStopSCLK  = new SCLKTime( clock9, xStop  );

         ival = cover.getInterval( 0 );

         ok   = JNITestutils.chcksd( "start SCLK time",
                                     ival[0],
                                     "~/",
                                     xStartSCLK.getContinuousTicks(),
                                     TIGHT_TOL               );

         ok   = JNITestutils.chcksd( "stop SCLK time",
                                     ival[1],
                                     "~/",
                                     xStopSCLK.getContinuousTicks(),
                                     TIGHT_TOL               );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create a new CK containing a type 3 " +
                              "segment with AV." );

         ck0 = CK.openNew( CK0, "Internal file name for " + CK0, 0 );

         //
         // Create data for a segment with continuous coverage.
         //

         n      = 1000;

         quatArray   = new SpiceQuaternion[n];
         avArray     = new Vector3[n];

         timeArray   = new SCLKTime[n];
         startArray  = new SCLKTime[1];

         axis   = new Vector3( 1.0, 2.0, 3.0 );
         delta  = 1.e-3 * Math.PI / n;

         //
         // Find the nominal tick length of the SCLK we're using.
         //
         secPerTick =    ( new SCLKTime(clock9,1) ).getTDBSeconds()
                       - ( new SCLKTime(clock9,0) ).getTDBSeconds();

         // System.out.println( "sec per tick = " + secPerTick);

         for ( i = 0;  i < n;  i++ )
         {
            //
            // Create the ith quaternion.
            //
            angle        = i * delta;

            r            = new Matrix33( axis, angle );

            quatArray[i] = new SpiceQuaternion( r );


            //
            // Create the ith angular velocity vector. Note
            // that the angualar velocity is not consistent
            // with the orientation, nor need it be.
            //
            angularRate = i * delta;

            avArray[i]  = axis.hat().scale( angularRate );

            //
            // Create the ith time tag.
            //
            timeArray[i] = new SCLKTime( clock9,  10 * i );
         }

         first = timeArray[0  ];
         last  = timeArray[n-1];

         ref   = new ReferenceFrame( "J2000" );

         inst  = new Instrument( inst_9999 );
         segid = "Segment 1 for instrument " + inst;

         nints         = 1;
         startArray[0] = timeArray[0];

         //
         // Write the segment to the file.
         //
         ck0.writeType03Segment( first,      last,       inst,
                                 ref,        true,       segid,
                                 timeArray,  quatArray,  avArray,
                                 startArray  );
         //
         // Close the CK.
         //

         ck0.close();



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check the data from the file created " +
                              "in the last test case." );

         //
         // Load the CK.
         //
         KernelDatabase.load( CK0 );

         for ( i = 0;  i < n;  i++ )
         {
            //
            // Look up pointing corresponding to the ith time tag.
            //
            ticks  = timeArray[i];

            //
            // Set the tolerance to a non-zero value so we can ensure the
            // value is recoverable.
            //
            tol    = new SCLKDuration( clock9, 3.0 );

            pavrec = new PointingAndAVRecord( inst, ticks, ref, tol );

            //
            // First, make sure the pointing was found.
            //

            found = pavrec.wasFound();

            ok    = JNITestutils.chcksl( "found", found, true );

            if ( found )
            {
               //
               // Check the stored instrument.
               //
               ok = JNITestutils.chcksi( "instrument",
                                         pavrec.getInstrument().getIDCode(),
                                         "=",
                                         inst.getIDCode(),
                                         0                          );

               //
               // Check the stored frame.
               //
               ok = JNITestutils.chcksc( "frame",
                                         pavrec.getReferenceFrame().getName(),
                                         "=",
                                         ref.getName()                       );


               //
               // Check the stored tolerance.
               //
               ok = JNITestutils.chcksd( "tolerance",
                                         pavrec.getTolerance().getMeasure(),
                                         "=",
                                         tol.getMeasure(),
                                         0                          );

               //
               // Check the stored request time. We expect an exact match.
               //
               ok = JNITestutils.chcksd( "request time",
                              pavrec.getRequestSCLKTime().getContinuousTicks(),
                                         "=",
                                         ticks.getContinuousTicks(),
                                         0.0                          );

               //
               // Check the actual SCLK time. We expect a tight match.
               //
               ok = JNITestutils.chcksd( "actual time",
                               pavrec.getActualSCLKTime().getContinuousTicks(),
                                         "~/",
                                         ticks.getContinuousTicks(),
                                         VTIGHT_TOL                     );
               //
               // Check the quaternion. Allow for the possibility that
               // the quaternion sign might be flipped.
               //

               q = new SpiceQuaternion( pavrec.getCMatrix() );

               if ( q.dist( quatArray[i] )  < 1.0 )
               {
                  ok = JNITestutils.chckad( "quaternion",
                                            q.toArray(),
                                            "~~/",
                                            quatArray[i].toArray(),
                                            TIGHT_TOL                     );
               }
               else
               {
                  ok = JNITestutils.chckad( "quaternion",
                                            q.negate().toArray(),
                                            "~~/",
                                            quatArray[i].toArray(),
                                            TIGHT_TOL                     );
               }


               //
               // Check the C-matrix directly.
               //
               ok = JNITestutils.chckad( "C-matrix",
                                         pavrec.getCMatrix().toArray1D(),
                                         "~~/",
                                         quatArray[i].toMatrix().toArray1D(),
                                         TIGHT_TOL                        );

               //
               // Check the angular velocity.
               //
               ok = JNITestutils.chckad( "Angular velocity",
                                         pavrec.getAngularVelocity().toArray(),
                                         "~~/",
                                         avArray[i].toArray(),
                                         TIGHT_TOL                        );

            }

         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Repeat the test (minus the AV part) " +
                              "using class PointingRecord." );


         for ( i = 0;  i < n;  i++ )
         {
            //
            // Look up pointing corresponding to the ith time tag.
            //
            ticks  = timeArray[i];

            //
            // Set the tolerance to a non-zero value so we can ensure the
            // value is recoverable.
            //
            tol  = new SCLKDuration( clock9, 3.0 );

            prec = new PointingRecord( inst, ticks, ref, tol );

            //
            // First, make sure the pointing was found.
            //

            found = prec.wasFound();

            ok    = JNITestutils.chcksl( "found", found, true );

            if ( found )
            {
               //
               // Check the stored instrument.
               //
               ok = JNITestutils.chcksi( "instrument",
                                         prec.getInstrument().getIDCode(),
                                         "=",
                                         inst.getIDCode(),
                                         0                          );

               //
               // Check the stored frame.
               //
               ok = JNITestutils.chcksc( "frame",
                                         prec.getReferenceFrame().getName(),
                                         "=",
                                         ref.getName()                       );


               //
               // Check the stored tolerance.
               //
               ok = JNITestutils.chcksd( "tolerance",
                                         prec.getTolerance().getMeasure(),
                                         "=",
                                         tol.getMeasure(),
                                         0                          );

               //
               // Check the stored request time. We expect an exact match.
               //
               ok = JNITestutils.chcksd( "request time",
                                prec.getRequestSCLKTime().getContinuousTicks(),
                                         "=",
                                         ticks.getContinuousTicks(),
                                         0.0                          );

               //
               // Check the actual SCLK time. We expect a tight match.
               //
               ok = JNITestutils.chcksd( "actual time",
                                 prec.getActualSCLKTime().getContinuousTicks(),
                                         "~/",
                                         ticks.getContinuousTicks(),
                                         VTIGHT_TOL                     );
               //
               // Check the quaternion. Allow for the possibility that
               // the quaternion sign might be flipped.
               //

               q = new SpiceQuaternion( prec.getCMatrix() );

               if ( q.dist( quatArray[i] )  < 1.0 )
               {
                  ok = JNITestutils.chckad( "quaternion",
                                            q.toArray(),
                                            "~~/",
                                            quatArray[i].toArray(),
                                            TIGHT_TOL                     );
               }
               else
               {
                  ok = JNITestutils.chckad( "quaternion",
                                            q.negate().toArray(),
                                            "~~/",
                                            quatArray[i].toArray(),
                                            TIGHT_TOL                     );
               }


               //
               // Check the C-matrix directly.
               //
               ok = JNITestutils.chckad( "C-matrix",
                                         prec.getCMatrix().toArray1D(),
                                         "~~/",
                                         quatArray[i].toMatrix().toArray1D(),
                                         TIGHT_TOL                        );

            }

         }

         //
         // Unload the CK so we can append a segment to it.
         //
         KernelDatabase.unload( CK0 );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Open the CK for write access and append " +
                              "a segment." );

         //
         // This segment will have gaps.
         //

         ck0 = CK.openForWrite( CK0 );


         for ( i = 0;  i < n;  i++ )
         {
            //
            // Create the ith quaternion.
            //

            j            = i + n;

            angle        = j * delta;

            r            = new Matrix33( axis, angle );

            quatArray[i] = new SpiceQuaternion( r );


            //
            // Create the ith angular velocity vector. Note
            // that the angualar velocity is not consistent
            // with the orientation, nor need it be.
            //
            angularRate = j * delta;

            avArray[i]  = axis.hat().scale( angularRate );

            //
            // Create the ith time tag.
            //
            timeArray[i] = new SCLKTime( clock9,  10 * j );
         }

         first = timeArray[0  ];
         last  = timeArray[n-1];

         ref   = new ReferenceFrame( "J2000" );

         inst  = new Instrument( inst_9999 );
         segid = "Segment 1 for instrument " + inst;

         nints         = 3;

         startArray    = new SCLKTime[nints];

         startArray[0] = timeArray[0];
         startArray[1] = timeArray[200];
         startArray[2] = timeArray[600];

         //
         // Write the segment to the file.
         //
         ck0.writeType03Segment( first,      last,       inst,
                                 ref,        true,       segid,
                                 timeArray,  quatArray,  avArray,
                                 startArray  );
         //
         // Close the CK.
         //

         ck0.close();




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check the data from the file created in " +
                              "the last test case." );

         //
         // Load the CK.
         //
         KernelDatabase.load( CK0 );

         for ( i = 0;  i < n;  i++ )
         {
            //
            // Look up pointing corresponding to the ith time tag.
            //
            ticks  = timeArray[i];

            //
            // Set the tolerance to a non-zero value so we can ensure the
            // value is recoverable.
            //
            tol    = new SCLKDuration( clock9, 3.0 );

            pavrec = new PointingAndAVRecord( inst, ticks, ref, tol );

            //
            // First, make sure the pointing was found.
            //

            found = pavrec.wasFound();

            ok    = JNITestutils.chcksl( "found", found, true );

            if ( found )
            {
               //
               // Check the stored instrument.
               //
               ok = JNITestutils.chcksi( "instrument",
                                         pavrec.getInstrument().getIDCode(),
                                         "=",
                                         inst.getIDCode(),
                                         0                          );

               //
               // Check the stored frame.
               //
               ok = JNITestutils.chcksc( "frame",
                                         pavrec.getReferenceFrame().getName(),
                                         "=",
                                         ref.getName()                       );


               //
               // Check the stored tolerance.
               //
               ok = JNITestutils.chcksd( "tolerance",
                                         pavrec.getTolerance().getMeasure(),
                                         "=",
                                         tol.getMeasure(),
                                         0                          );

               //
               // Check the stored request time. We expect an exact match.
               //
               ok = JNITestutils.chcksd( "request time",
                              pavrec.getRequestSCLKTime().getContinuousTicks(),
                                         "=",
                                         ticks.getContinuousTicks(),
                                         0.0                          );

               //
               // Check the actual SCLK time. We expect a tight match.
               //
               ok = JNITestutils.chcksd( "actual time",
                               pavrec.getActualSCLKTime().getContinuousTicks(),
                                         "~/",
                                         ticks.getContinuousTicks(),
                                         VTIGHT_TOL                     );
               //
               // Check the quaternion. Allow for the possibility that
               // the quaternion sign might be flipped.
               //

               q = new SpiceQuaternion( pavrec.getCMatrix() );

               if ( q.dist( quatArray[i] )  < 1.0 )
               {
                  ok = JNITestutils.chckad( "quaternion",
                                            q.toArray(),
                                            "~~/",
                                            quatArray[i].toArray(),
                                            TIGHT_TOL                     );
               }
               else
               {
                  ok = JNITestutils.chckad( "quaternion",
                                            q.negate().toArray(),
                                            "~~/",
                                            quatArray[i].toArray(),
                                            TIGHT_TOL                     );
               }


               //
               // Check the C-matrix directly.
               //
               ok = JNITestutils.chckad( "C-matrix",
                                         pavrec.getCMatrix().toArray1D(),
                                         "~~/",
                                         quatArray[i].toMatrix().toArray1D(),
                                         TIGHT_TOL                        );

               //
               // Check the angular velocity.
               //
               ok = JNITestutils.chckad( "Angular velocity",
                                         pavrec.getAngularVelocity().toArray(),
                                         "~~/",
                                         avArray[i].toArray(),
                                         TIGHT_TOL                        );

            }

         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get the interval-level time coverage window " +
                              "for instrument -9999 in CK0, using the SCLK " +
                              "time system."  );


         //
         // The file CK0 has the following coverage for instrument -9999:
         //
         //    interval 0:    tick     0  to  tick  9990
         //    interval 1:    tick 10000  to  tick 11990
         //    interval 2:    tick 12000  to  tick 15990
         //    interval 3:    tick 16000  to  tick 19990
         //

         tol       = new SCLKDuration( clock9, 0.0 );

         inst_9999 = new Instrument( -9999 );

         xCard     = 4;

         cover     = ck0.getCoverage( inst_9999, true,            "interval",
                                      tol,       TimeSystem.SCLK, xCard      );

         //System.out.println( cover );


         //
         // Check cardinality of the result window.
         //
         ok   = JNITestutils.chcksi( "cover card",
                                     cover.card(),
                                     "=",
                                     xCard,
                                     0            );


         xIntStart = new SCLKTime[xCard];
         xIntStop  = new SCLKTime[xCard];

         xIntStart[0] = new SCLKTime( clock9,     0.0 );
         xIntStop [0] = new SCLKTime( clock9,  9990.0 );
         xIntStart[1] = new SCLKTime( clock9, 10000.0 );
         xIntStop [1] = new SCLKTime( clock9, 11990.0 );
         xIntStart[2] = new SCLKTime( clock9, 12000.0 );
         xIntStop [2] = new SCLKTime( clock9, 15990.0 );
         xIntStart[3] = new SCLKTime( clock9, 16000.0 );
         xIntStop [3] = new SCLKTime( clock9, 19990.0 );


         for ( i = 0;  i < cover.card();  i++ )
         {
            ival = cover.getInterval( i );

            ok   = JNITestutils.chcksd( "start time " +i,
                                        ival[0],
                                        "~/",
                                        xIntStart[i].getContinuousTicks(),
                                        TIGHT_TOL                         );

            ok   = JNITestutils.chcksd( "stop time " +i,
                                        ival[1],
                                        "~/",
                                        xIntStop[i].getContinuousTicks(),
                                        TIGHT_TOL                        );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get the interval-level time coverage window " +
                              "for instrument -9999 in CK0, using the SCLK " +
                              "time system, using a non-empty initial " +
                              "coverage window."                             );


         //
         // Create an input "coverage" window.
         //
         n = 3;
         initArray = new double[2*n];

         for( i = 0;  i < 2*n;  i+=2 )
         {
            initArray[i  ] = 20000.0 + (10000.0 * (i/2));
            initArray[i+1] = initArray[i] + 10.0;
         }

         initialWindow = new SpiceWindow( initArray );

         //
         // The file CK0 has the following coverage for instrument -9999:
         //
         //    interval 0:    tick     0  to  tick  9990
         //    interval 1:    tick 10000  to  tick 11990
         //    interval 2:    tick 12000  to  tick 15990
         //    interval 3:    tick 16000  to  tick 19990
         //

         xCard     = 4+n;

         tol       = new SCLKDuration( clock9, 0.0 );

         inst_9999 = new Instrument( -9999 );

         cover     = ck0.getCoverage( inst_9999, true,            "interval",
                                      tol,       TimeSystem.SCLK,
                                      initialWindow, xCard );

         //System.out.println( cover );

         //
         // Check cardinality of the result window.
         //
         ok   = JNITestutils.chcksi( "cover card",
                                     cover.card(),
                                     "=",
                                     xCard,
                                     0            );



         xIntStart = new SCLKTime[xCard];
         xIntStop  = new SCLKTime[xCard];

         xIntStart[0] = new SCLKTime( clock9,     0.0 );
         xIntStop [0] = new SCLKTime( clock9,  9990.0 );
         xIntStart[1] = new SCLKTime( clock9, 10000.0 );
         xIntStop [1] = new SCLKTime( clock9, 11990.0 );
         xIntStart[2] = new SCLKTime( clock9, 12000.0 );
         xIntStop [2] = new SCLKTime( clock9, 15990.0 );
         xIntStart[3] = new SCLKTime( clock9, 16000.0 );
         xIntStop [3] = new SCLKTime( clock9, 19990.0 );
         xIntStart[4] = new SCLKTime( clock9, 20000.0 );
         xIntStop [4] = new SCLKTime( clock9, 20010.0 );
         xIntStart[5] = new SCLKTime( clock9, 30000.0 );
         xIntStop [5] = new SCLKTime( clock9, 30010.0 );
         xIntStart[6] = new SCLKTime( clock9, 40000.0 );
         xIntStop [6] = new SCLKTime( clock9, 40010.0 );


         for ( i = 0;  i < cover.card();  i++ )
         {
            ival = cover.getInterval( i );

            ok   = JNITestutils.chcksd( "start time " +i,
                                        ival[0],
                                        "~/",
                                        xIntStart[i].getContinuousTicks(),
                                        TIGHT_TOL                         );

            ok   = JNITestutils.chcksd( "stop time " +i,
                                        ival[1],
                                        "~/",
                                        xIntStop[i].getContinuousTicks(),
                                        TIGHT_TOL                        );
         }






         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test type 2 writer: open the CK for write " +
                              "access and append a segment for instrument " +
                              "-10001." );

         //
         // Unload CK0 and open the file for write access.
         //
         KernelDatabase.unload( CK0 );

         ck0 = CK.openForWrite( CK0 );

         //
         // Find the nominal tick length of the SCLK we're using.
         //
         secPerTick =    ( new SCLKTime(clock9,1) ).getTDBSeconds()
                       - ( new SCLKTime(clock9,0) ).getTDBSeconds();


         n = 1000;

         startArray = new SCLKTime[n];
         stopArray  = new SCLKTime[n];
         rateArray  = new double[n];

         for ( i = 0;  i < n;  i++ )
         {
            //
            // Create the ith quaternion.
            //

            j            = i;

            angle        = j * delta;

            r            = new Matrix33( axis, angle );

            quatArray[i] = new SpiceQuaternion( r );


            //
            // Create the ith angular velocity vector. Note
            // that the angualar velocity is not consistent
            // with the orientation, nor need it be.
            //
            angularRate = j * delta;

            avArray[i]  = axis.hat().scale( angularRate );

            //
            // Create the ith time interval
            //
            startArray[i] = new SCLKTime( clock9,  10*j     );
            stopArray[i]  = new SCLKTime( clock9,  10*j + 3 );

            //
            // Note that these rates are *bogus*; they're not
            // compatible with the SCLK and AV data.
            //
            rateArray[i]  = i;
         }

         first = startArray[0  ];
         last  = stopArray [n-1];

         ref   = new ReferenceFrame( "J2000" );


         inst  = new Instrument( inst_10001 );
         segid = "Segment 1 for instrument " + inst;


         //
         // Write the segment to the file.
         //
         ck0.writeType02Segment( first,      last,       inst,
                                 ref,        segid,      startArray,
                                 stopArray,  quatArray,  avArray,
                                 rateArray  );
         //
         // Close the CK.
         //

         ck0.close();




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check the data from the file created in " +
                              "the last test case." );

         //
         // Load the CK.
         //
         KernelDatabase.load( CK0 );

         for ( i = 0;  i < n;  i++ )
         {
            //
            // Look up pointing corresponding to the midpoint of the ith
            // interval.
            //
            intervalLength = stopArray[i].sub( startArray[i] );
            offset         = intervalLength.scale( 0.5 );

            ticks          = startArray[i].add( offset );

            //
            // Set the tolerance to a non-zero value so we can ensure the
            // value is recoverable.
            //
            tol    = new SCLKDuration( clock9, 3.0 );

            pavrec = new PointingAndAVRecord( inst, ticks, ref, tol );

            //
            // First, make sure the pointing was found.
            //

            found = pavrec.wasFound();

            ok    = JNITestutils.chcksl( "found", found, true );

            if ( found )
            {
               //
               // Check the stored instrument.
               //
               ok = JNITestutils.chcksi( "instrument",
                                         pavrec.getInstrument().getIDCode(),
                                         "=",
                                         inst.getIDCode(),
                                         0                          );

               //
               // Check the stored frame.
               //
               ok = JNITestutils.chcksc( "frame",
                                         pavrec.getReferenceFrame().getName(),
                                         "=",
                                         ref.getName()                       );


               //
               // Check the stored tolerance.
               //
               ok = JNITestutils.chcksd( "tolerance",
                                         pavrec.getTolerance().getMeasure(),
                                         "=",
                                         tol.getMeasure(),
                                         0                          );

               //
               // Check the stored request time. We expect an exact match.
               //
               ok = JNITestutils.chcksd( "request time",
                              pavrec.getRequestSCLKTime().getContinuousTicks(),
                                         "=",
                                         ticks.getContinuousTicks(),
                                         0.0                          );

               //
               // Check the actual SCLK time. We expect a tight match.
               //
               ok = JNITestutils.chcksd( "actual time",
                               pavrec.getActualSCLKTime().getContinuousTicks(),
                                         "~/",
                                         ticks.getContinuousTicks(),
                                         VTIGHT_TOL                     );



               //
               // Generate the expected C-matrix.
               //
               // The attitude at the start of the interval is given by
               // quatArray[i].
               //

               r =  quatArray[i].toMatrix();

               //
               // Compute the time offset from the interval start in seconds.
               // Note that we rely on the bogus seconds per tick data for
               // this.
               //

               deltaSec = rateArray[i] * offset.getMeasure();

               //System.out.println( "deltaSec = " + deltaSec );

               //
               // Compute the instrument frame's rotation, expressed in the
               // base frame, corresponding to the time delta.
               //

               quotient = new Matrix33( avArray[i], avArray[i].norm() *
                                                                   deltaSec );

               //
               // Applying q to the basis vectors of the C-matrix (the columns
               // of the transpose of the C-matrix, in other words) gives us
               // the transpose of the rotated C-matrix. Transpose that
               // product to get the expected C-matrix.
               //

               xMatrix = ( quotient.mxmt(r) ).xpose();


               //
               // Check the C-matrix directly.
               //
               ok = JNITestutils.chckad( "C-matrix",
                                         pavrec.getCMatrix().toArray1D(),
                                         "~~/",
                                         xMatrix.toArray1D(),
                                         TIGHT_TOL                        );



               //
               // Check the angular velocity.
               //
               ok = JNITestutils.chckad( "Angular velocity",
                                         pavrec.getAngularVelocity().toArray(),
                                         "~~/",
                                         avArray[i].toArray(),
                                         TIGHT_TOL                        );

            }

         }
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
         // Get rid of the CK files.
         //

         CSPICE.ckupf( han1 );
         ( new File ( CK1 ) ).delete();


         ck2.close();
         ( new File ( CK2 ) ).delete();

         //
         // Get rid of the SCLK kernel.
         //
         ( new File ( SCLKKER ) ).delete();
      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

