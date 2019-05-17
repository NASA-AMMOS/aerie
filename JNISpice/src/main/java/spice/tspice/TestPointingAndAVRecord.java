
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;



/**
Class TestPointingAndAVRecord provides methods that implement test families for
the class PointingAndAVRecord.

<h3> Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3> Version 1.0.0 14-DEC-2009 (NJB)</h3>
*/
public class TestPointingAndAVRecord extends Object
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
   Test PointingAndAVRecord and associated classes.
   */
   public static boolean f_PointingAndAVRecord()

      throws SpiceException
   {
      //
      // Local constants
      //
      final String                      CK0        = "custom0.bc";
      final String                      CK1        = "generic.bc";

      final String                      SCLKKER    = "testsclk.tsc";

      final double                      TIGHT_TOL  = 1.e-12;
      final double                      VTIGHT_TOL = 1.e-14;

      final int                         CLKID      =  -9;

      //
      // Local variables
      //

      CK                                ck0;

      Instrument                        inst;
      Instrument                        inst_9999;
      Instrument                        pavrecInst;

      Matrix33                          CMatrix;
      Matrix33                          r;

      PointingAndAVRecord               pavrec;
      PointingAndAVRecord               pavrec1;

      ReferenceFrame                    pavrecRef;
      ReferenceFrame                    ref0;
      ReferenceFrame                    ref1;

      SCLK                              clock9 = new SCLK( CLKID );

      SCLKDuration                      pavrecTol;
      SCLKDuration                      tol;

      SCLKTime                          actualTime;
      SCLKTime                          first;
      SCLKTime                          last;
      SCLKTime                          requestTime;
      SCLKTime[]                        startArray;
      SCLKTime                          ticks;
      SCLKTime[]                        timeArray;

      SCLK                              sclk       = new SCLK ( CLKID );


      SpiceQuaternion[]                 quatArray;

      String                            segid;

      TDBTime                           et;

      Vector3                           av;
      Vector3[]                         avArray;
      Vector3                           axis;

      boolean                           found;
      boolean                           ok;

      double                            angle;
      double                            angularRate;
      double                            delta;

      int                               han1 = 0;
      int                               i;
      int                               j;
      int                               n;
      int                               nints;





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

         JNITestutils.topen ( "f_PointingAndAVRecord" );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Create an SCLK kernel. The routine "     +
                              "we use for this purpose also creates a " +
                              "C-kernel, which we don't need."            );

         JNITestutils.tstlsk();

         han1 = JNITestutils.tstck3 ( CK1, SCLKKER, false, true, true );

         ( new File ( CK1 ) ).delete();




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Attempt to construct PointingAndAVRecord with " +
                              "no CKs loaded."                               );

         try
         {
            clock9 = new SCLK ( CLKID );

            et     = new TDBTime( "2000 Jan 1" );

            inst   = new Instrument( -9999 );

            ref0   = new ReferenceFrame( "J2000" );

            tol    = new SCLKDuration( clock9, 0.0 );

            pavrec = new PointingAndAVRecord( inst, et, ref0, tol );


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

         JNITestutils.tcase ( "Attempt to get C-matrix from " +
                              "PointingAndAVRecord when no data were found." );

         try
         {
            han1 = JNITestutils.tstck3 ( CK1, SCLKKER, true, true, true );

            clock9 = new SCLK ( CLKID );

            et     = new TDBTime( "2200 Jan 1" );

            inst   = new Instrument( -9999 );

            ref0   = new ReferenceFrame( "J2000" );

            tol    = new SCLKDuration( clock9, 0.0 );

            pavrec = new PointingAndAVRecord( inst, et, ref0, tol );

            CMatrix = pavrec.getCMatrix();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(POINTINGNOTFOUND)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(POINTINGNOTFOUND)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Attempt to get output SCLK time from " +
                              "PointingAndAVRecord when no data were found." );

         try
         {
            clock9 = new SCLK ( CLKID );

            et     = new TDBTime( "2200 Jan 1" );

            inst   = new Instrument( -9999 );

            ref0   = new ReferenceFrame( "J2000" );

            tol    = new SCLKDuration( clock9, 0.0 );

            pavrec = new PointingAndAVRecord( inst, et, ref0, tol );

            ticks  = pavrec.getActualSCLKTime();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(POINTINGNOTFOUND)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(POINTINGNOTFOUND)", ex );
         }


         //
         // Unload the generic kernel, since we don't want it to contribute
         // coverage in the following tests.
         //
         CSPICE.ckupf( han1 );




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create a new CK containing two type 3 " +
                              "segments with AV and one without AV." );

         //
         // Delete any file of the same name that may already exist.
         //
         ( new File(CK0) ).delete();

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

         ref0  = new ReferenceFrame( "J2000" );

         inst  = new Instrument( -9999 );
         segid = "Segment 1 for instrument " + inst;

         nints         = 1;
         startArray[0] = timeArray[0];

         //
         // Write the segment to the file.
         //
         ck0.writeType03Segment( first,      last,       inst,
                                 ref0,       true,       segid,
                                 timeArray,  quatArray,  avArray,
                                 startArray  );


         //
         // Create a second segment with AV.
         //

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

         ref1  = new ReferenceFrame( "ECLIPJ2000" );

         inst  = new Instrument( -9999 );
         segid = "Segment 2 for instrument " + inst;

         nints         = 3;

         startArray    = new SCLKTime[nints];

         startArray[0] = timeArray[0];
         startArray[1] = timeArray[200];
         startArray[2] = timeArray[600];

         //
         // Write the segment to the file.
         //
         ck0.writeType03Segment( first,      last,       inst,
                                 ref1,       true,       segid,
                                 timeArray,  quatArray,  avArray,
                                 startArray  );



         //
         // Create a third segment without AV.
         //

         for ( i = 0;  i < n;  i++ )
         {
            //
            // Create the ith quaternion.
            //

            j            = i + 2*n;

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

         ref1  = new ReferenceFrame( "ECLIPJ2000" );

         inst  = new Instrument( -9999 );
         segid = "Segment 3 for instrument " + inst;

         nints         = 3;

         startArray    = new SCLKTime[nints];

         startArray[0] = timeArray[0];
         startArray[1] = timeArray[200];
         startArray[2] = timeArray[600];

         //
         // Write the segment to the file. Note that the AV flag
         // is false.
         //
         ck0.writeType03Segment( first,      last,       inst,
                                 ref1,       false,      segid,
                                 timeArray,  quatArray,  avArray,
                                 startArray  );

         //
         // Close the CK.
         //

         ck0.close();




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check the data from the first segment " +
                              "of the file created in the last test case." );

         //
         // Load the CK.
         //
         KernelDatabase.load( CK0 );


         //
         // Check data from the first segment.
         //

         for ( i = 0;  i < n;  i++ )
         {
            //
            // Set up the expected data values.
            //

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


            //
            // Look up pointing corresponding to the ith time tag.
            //
            ticks  = timeArray[i];

            //
            // Set the tolerance to a non-zero value so we can ensure the
            // value is recoverable.
            //
            tol  = new SCLKDuration( clock9, 3.0 );

            pavrec = new PointingAndAVRecord( inst, ticks, ref0, tol );

            //
            // First, make sure the pointing was found.
            //

            found = pavrec.wasFound();

            ok    = JNITestutils.chcksl( "found" + "(" + i + ")", found,
                                                                        true );

            if ( found )
            {
               //
               // Check the stored instrument.
               //
               ok = JNITestutils.chcksi( "instrument" + "(" + i + ")",
                                         pavrec.getInstrument().getIDCode(),
                                         "=",
                                         inst.getIDCode(),
                                         0                          );

               //
               // Check the stored frame.
               //
               ok = JNITestutils.chcksc( "frame" + "(" + i + ")",
                                         pavrec.getReferenceFrame().getName(),
                                         "=",
                                         ref0.getName()                      );


               //
               // Check the stored tolerance.
               //
               ok = JNITestutils.chcksd( "tolerance" + "(" + i + ")",
                                         pavrec.getTolerance().getMeasure(),
                                         "=",
                                         tol.getMeasure(),
                                         0                          );

               //
               // Check the stored request time. We expect an exact match.
               //
               ok = JNITestutils.chcksd( "request time" + "(" + i + ")",
                              pavrec.getRequestSCLKTime().getContinuousTicks(),
                                         "=",
                                         ticks.getContinuousTicks(),
                                         0.0                          );

               //
               // Check the actual SCLK time. We expect a tight match.
               //
               ok = JNITestutils.chcksd( "actual time" + "(" + i + ")",
                               pavrec.getActualSCLKTime().getContinuousTicks(),
                                         "~/",
                                         ticks.getContinuousTicks(),
                                         VTIGHT_TOL                     );

               //
               // Check the C-matrix.
               //
               ok = JNITestutils.chckad( "C-matrix" + "(" + i + ")",
                                         pavrec.getCMatrix().toArray1D(),
                                         "~~/",
                                         quatArray[i].toMatrix().toArray1D(),
                                         TIGHT_TOL                        );

               //
               // Check the angualar velocity.
               //
               ok = JNITestutils.chckad( "Angular velocity" + "(" + i + ")",
                                         pavrec.getAngularVelocity().toArray(),
                                         "~~/",
                                         avArray[i].toArray(),
                                         TIGHT_TOL                        );

            }

         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check the data from the second segment " +
                              "of the file created in the last test case." );

         for ( i = 0;  i < n;  i++ )
         {
            //
            // Set up the expected data values.
            //

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


            //
            // Look up pointing corresponding to the ith time tag.
            //
            ticks  = timeArray[i];

            //
            // Set the tolerance to a non-zero value so we can ensure the
            // value is recoverable.
            //
            tol  = new SCLKDuration( clock9, 3.0 );

            pavrec = new PointingAndAVRecord( inst, ticks, ref1, tol );

            //
            // First, make sure the pointing was found.
            //

            found = pavrec.wasFound();

            ok    = JNITestutils.chcksl( "found" + "(" + i + ")", found,
                                                                        true );

            if ( found )
            {
               //
               // Check the stored instrument.
               //
               ok = JNITestutils.chcksi( "instrument" + "(" + i + ")",
                                         pavrec.getInstrument().getIDCode(),
                                         "=",
                                         inst.getIDCode(),
                                         0                          );

               //
               // Check the stored frame.
               //
               ok = JNITestutils.chcksc( "frame" + "(" + i + ")",
                                         pavrec.getReferenceFrame().getName(),
                                         "=",
                                         ref1.getName()                      );


               //
               // Check the stored tolerance.
               //
               ok = JNITestutils.chcksd( "tolerance" + "(" + i + ")",
                                         pavrec.getTolerance().getMeasure(),
                                         "=",
                                         tol.getMeasure(),
                                         0                          );

               //
               // Check the stored request time. We expect an exact match.
               //
               ok = JNITestutils.chcksd( "request time" + "(" + i + ")",
                              pavrec.getRequestSCLKTime().getContinuousTicks(),
                                         "=",
                                         ticks.getContinuousTicks(),
                                         0.0                          );

               //
               // Check the actual SCLK time. We expect a tight match.
               //
               ok = JNITestutils.chcksd( "actual time" + "(" + i + ")",
                               pavrec.getActualSCLKTime().getContinuousTicks(),
                                         "~/",
                                         ticks.getContinuousTicks(),
                                         VTIGHT_TOL                     );

               //
               // Check the C-matrix.
               //
               ok = JNITestutils.chckad( "C-matrix" + "(" + i + ")",
                                         pavrec.getCMatrix().toArray1D(),
                                         "~~/",
                                         quatArray[i].toMatrix().toArray1D(),
                                         TIGHT_TOL                        );

               //
               // Check the angualar velocity.
               //
               ok = JNITestutils.chckad( "Angular velocity" + "(" + i + ")",
                                         pavrec.getAngularVelocity().toArray(),
                                         "~~/",
                                         avArray[i].toArray(),
                                         TIGHT_TOL                        );
            }

         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Try to fetch data from the third segment " +
                              "of the file created in the last test case." );



         inst = new Instrument( -9999 );

         ref1 = new ReferenceFrame( "eclipj2000" );

         tol  = new SCLKDuration( clock9, 3.0 );

         for ( i = 0;  i < n;  i++ )
         {
            //
            // Set up the expected time values. We don't expect to
            // find data, so we won't need pointing or angular velocity.
            //

            j = i + 2*n;

            //
            // Create the ith time tag.
            //
            ticks = new SCLKTime( clock9,  10 * j );

            //
            // Set the tolerance to a non-zero value so we can ensure the
            // value is recoverable.
            //

            pavrec = new PointingAndAVRecord( inst, ticks, ref1, tol );

            //
            // First, make sure the pointing was NOT found.
            //

            found = pavrec.wasFound();

            ok    = JNITestutils.chcksl( "found" + "(" + i + ")", found,
                                                                       false );

         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor." );



         ticks = new SCLKTime( clock9, 00000.0 );

         tol   = new SCLKDuration( clock9, 3.0 );

         ref0  = new ReferenceFrame( "j2000" );

         inst  = new Instrument( -9999 );

         pavrec  = new PointingAndAVRecord( inst, ticks, ref0, tol );

         pavrec1 = new PointingAndAVRecord( pavrec );

         found = pavrec.wasFound();

         i     = 0;

         ok    = JNITestutils.chcksl( "found" + "(" + i + ")", found, true );

         if ( found )
         {
            //
            // Check the stored instrument.
            //
            ok = JNITestutils.chcksi( "instrument" + "(" + i + ")",
                                      pavrec1.getInstrument().getIDCode(),
                                      "=",
                                      pavrec.getInstrument().getIDCode(),
                                      0                          );

            //
            // Check the stored frame.
            //
            ok = JNITestutils.chcksc( "frame" + "(" + i + ")",
                                      pavrec1.getReferenceFrame().getName(),
                                      "=",
                                      pavrec.getReferenceFrame().getName()   );


            //
            // Check the stored tolerance.
            //
            ok = JNITestutils.chcksd( "tolerance" + "(" + i + ")",
                                      pavrec1.getTolerance().getMeasure(),
                                      "=",
                                      pavrec.getTolerance().getMeasure(),
                                      0                                );

            //
            // Check the stored request time. We expect an exact match.
            //
            ok = JNITestutils.chcksd( "request time" + "(" + i + ")",
                             pavrec1.getRequestSCLKTime().getContinuousTicks(),
                                      "=",
                             pavrec.getRequestSCLKTime().getContinuousTicks(),
                                      0.0                                    );

            //
            // Check the actual SCLK time. We expect an exact match.
            //
            ok = JNITestutils.chcksd( "actual time" + "(" + i + ")",
                              pavrec1.getActualSCLKTime().getContinuousTicks(),
                                      "=",
                              pavrec.getActualSCLKTime().getContinuousTicks(),
                                      0.0                                    );

            //
            // Check the C-matrix.
            //
            ok = JNITestutils.chckad( "C-matrix" + "(" + i + ")",
                                      pavrec1.getCMatrix().toArray1D(),
                                      "=",
                                      pavrec.getCMatrix().toArray1D(),
                                      0.0                             );

            //
            // Check the angualar velocity.
            //
            ok = JNITestutils.chckad( "Angular velocity" + "(" + i + ")",
                                      pavrec1.getAngularVelocity().toArray(),
                                      "~~/",
                                      pavrec.getAngularVelocity().toArray(),
                                      TIGHT_TOL                              );

         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test no-args constructor." );

         //
         // Since the fields of PointingAndAVRecord are package private,
         // we can't examine them here, since this test family doesn't
         // belong to the spice.basic package. Just make sure that the
         // record indicates pointing "was not found."
         //

         pavrec = new PointingAndAVRecord();

         found = pavrec.wasFound();

         ok    = JNITestutils.chcksl( "found", found, false );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Look for data for an instrument not " +
                              "covered by the CK."                     );

         ticks = timeArray[1];

         tol   = new SCLKDuration( clock9, 0.0 );

         ref1  = new ReferenceFrame( "eclipj2000" );

         pavrec = new PointingAndAVRecord( new Instrument( -10001 ),
                                                           ticks, ref1, tol );

         found = pavrec.wasFound();

         ok    = JNITestutils.chcksl( "found", found, false );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Look for data for instrument -9999 at a " +
                              "time not covered by the CK."                  );

         ticks = new SCLKTime( clock9, 30000.0 );

         tol   = new SCLKDuration( clock9, 0.0 );

         ref1  = new ReferenceFrame( "eclipj2000" );

         pavrec = new PointingAndAVRecord( inst, ticks, ref1, tol );

         found = pavrec.wasFound();

         ok    = JNITestutils.chcksl( "found", found, false );

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
         // Get rid of the CK files.
         //
         KernelDatabase.unload( CK0 );

         ( new File ( CK0 ) ).delete();

         CSPICE.ckupf( han1 );

         ( new File ( CK1 ) ).delete();

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

} /* End f_SCLKTime */

