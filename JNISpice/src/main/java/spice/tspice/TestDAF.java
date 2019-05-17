
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestDAF provides methods that implement test families for
the class DAF.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 19-DEC-2009 (NJB)</h3>
*/
public class TestDAF extends Object
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
   Test DAF and associated classes.
   */
   public static boolean f_DAF()

      throws SpiceException
   {
      //
      // Constants
      //
      final String                      SPK       = "test.daf";

      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-9;

      final int                         MAX_LINE_LENGTH = 1000;

      //
      // Local variables
      //
      Body                              body;
      Body                              center;
      Body                              xBody;

      DAF                               daf0;
      DAF                               daf1;

      ReferenceFrame                    frame;

      String[]                          comments;
      String                            filename;
      String                            internalFilename;
      String                            name;
      String                            segid;
      String[]                          xComments;
      String                            xSegid;

      TDBTime[]                         bounds;
      TDBTime                           xStart;
      TDBTime                           xStop;

      boolean                           found;
      boolean                           IDFound;
      boolean                           ok;
      boolean                           readable;
      boolean                           writable;

      double[]                          dpComp;

      int                               dataType;
      int                               handle0 = 0;
      int                               handle1;
      int                               IDCode;
      int                               i;
      int[]                             intComp;
      int                               n;
      int                               nd;
      int                               ni;
      int                               nSegments;
      int                               segno;
      int                               xCode;

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

         JNITestutils.topen ( "f_DAF" );


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
         // Delete SPK if it exists. Create and but don't load a new
         // version of the file.
         //
         ( new File ( SPK ) ).delete();

         handle0 = JNITestutils.tstspk( SPK, false );



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: try open a DAF for both write and " +
                               "read access."  );

         daf0 = null;
         daf1 = null;

         try
         {
            //
            // Open a DAF for read access, then try to delete comments from it.
            //

            daf0 = DAF.openForWrite( SPK );
            daf1 = DAF.openForRead ( SPK );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(RWCONFLICT)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(RWCONFLICT)", ex );
         }
         finally
         {
            daf0.close();
         }


         //
         // Reverse the order of the open calls and try again.
         //
         try
         {
            //
            // Open a DAF for read access, then try to delete comments from it.
            //

            daf1 = DAF.openForRead ( SPK );
            daf0 = DAF.openForWrite( SPK );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(FILEOPENCONFLICT)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(FILEOPENCONFLICT)", ex );
         }
         finally
         {
            daf1.close();
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: try to delete comments from a DAF " +
                               "that has been opened for read access."  );
         daf0 = null;

         try
         {
            //
            // Open a DAF for read access, then try to delete comments from it.
            //

            daf0 = DAF.openForRead( SPK );

            daf0.deleteComments();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTWRITABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTWRITABLE)", ex );
         }
         finally
         {
            daf0.close();
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: try to add comments to a DAF " +
                               "that has been opened for read access."  );
         daf0 = null;

         try
         {
            //
            // Open a DAF for read access, then try to add comments to it.
            //

            daf0     = DAF.openForRead( SPK );

            n        = 10;
            comments = new String[n];

            for( i = 0;  i < n;  i++ )
            {
               comments[i] = "This is line " + n + " of the comments.";
            }

            daf0.addComments( comments );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTWRITABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTWRITABLE)", ex );
         }
         finally
         {
            daf0.close();
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: begin a forward search on a " +
                               "closed DAF."  );

         daf0 = DAF.openForRead( SPK );
         daf0.close();

         try
         {
            daf0.beginForwardSearch();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTREADABLE)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: begin a backward search on a " +
                               "closed DAF."  );

         daf0 = DAF.openForRead( SPK );
         daf0.close();

         try
         {
            daf0.beginBackwardSearch();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTREADABLE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: find next array in a closed DAF."  );

         daf0 = DAF.openForRead( SPK );
         daf0.close();

         try
         {
            found = daf0.findNextArray();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTREADABLE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: find previous array in a " +
                               "closed DAF."  );

         daf0 = DAF.openForRead( SPK );
         daf0.close();

         try
         {
            found = daf0.findPreviousArray();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTREADABLE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: get integer descriptor component " +
                               "from a closed DAF."                          );

         daf0 = DAF.openForRead( SPK );
         daf0.close();

         try
         {
            intComp = daf0.getIntegerSummaryComponent();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTREADABLE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: get d.p. descriptor component from " +
                               "a closed DAF."                               );

         daf0 = DAF.openForRead( SPK );
         daf0.close();

         try
         {
            dpComp = daf0.getDoubleSummaryComponent();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTREADABLE)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: get array name from " +
                               "a closed DAF."                               );

         daf0 = DAF.openForRead( SPK );
         daf0.close();

         try
         {
            name = daf0.getArrayName();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTREADABLE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: count segments in a closed DAF."  );

         daf0 = DAF.openForRead( SPK );
         daf0.close();

         try
         {
            n = daf0.countSegments();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOTREADABLE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: find next array without starting a " +
                               "search."                                     );

         daf0 = DAF.openForRead( SPK );

         try
         {
            found = daf0.findNextArray();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOSEARCH)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOSEARCH)", ex );
         }
         finally
         {
            daf0.close();
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: find previous array without starting " +
                               "a search."                                   );

         daf0 = DAF.openForRead( SPK );

         try
         {
            found = daf0.findPreviousArray();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(DAFNOSEARCH)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(DAFNOSEARCH)", ex );
         }
         finally
         {
            daf0.close();
         }








         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Open an existing SPK for read access. " +
                              "Examine the returned DAF object."             );


         daf0 = DAF.openForRead( SPK );

         //
         // We should be able to recover the file name from the DAF object.
         //
         filename = daf0.getFileName();

         ok       = JNITestutils.chcksc ( "file name", filename, "=", SPK );

         //
         // We should be able to recover the internal file name
         // from the DAF object.
         //
         internalFilename = daf0.getInternalFileName();

         ok       = JNITestutils.chcksc ( "interval file name",
                                          internalFilename,
                                          "=",
                                          "TestUtilitySPK" );


         //
         // The file handle should be positive. Note the value of the
         // handle depends on the history of the test program, since
         // handles are not re-used.
         //

         handle1   = daf0.getHandle();

         ok       = JNITestutils.chcksi ( "handle", handle1, ">", 0, 0 );

         //
         // The values of ND and NI should be 2 and 6, respectively.
         //

         nd = daf0.getND();

         ok = JNITestutils.chcksi ( "ND", nd, "=", 2, 0 );

         ni = daf0.getNI();

         ok = JNITestutils.chcksi ( "NI", ni, "=", 6, 0 );

         //
         // The file should be readable.
         //

         readable = daf0.isReadable();

         ok       = JNITestutils.chcksl ( "readable", readable, true );

         //
         // The file should not be writable.
         //

         writable = daf0.isWritable();

         ok       = JNITestutils.chcksl ( "writable", writable, false );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Search the previously opened SPK in the " +
                              "forward direction."                         );

         bounds = new TDBTime[2];

         xStart = new TDBTime( "1984 FEB 27 11:06:40.000 TDB" );
         xStop  = new TDBTime( "2015 NOV 05 12:53:20.000 TDB" );


         nSegments = 0;

         daf0.beginForwardSearch();

         found = daf0.findNextArray();

         while ( found )
         {
            ++nSegments;

            //
            // Fetch the double precision component of the segment
            // descriptor.
            //
            dpComp    = daf0.getDoubleSummaryComponent();

            bounds[0] = new TDBTime( dpComp[0] );
            bounds[1] = new TDBTime( dpComp[1] );

            ok        = JNITestutils.chcksd( "descr start",
                                             bounds[0].getTDBSeconds(),
                                             "~/",
                                             xStart.getTDBSeconds(),
                                             TIGHT_TOL                );

            ok        = JNITestutils.chcksd( "descr stop",
                                             bounds[1].getTDBSeconds(),
                                             "~/",
                                             xStop.getTDBSeconds(),
                                             TIGHT_TOL                );

            //
            // Fetch the current segment ID. In this file, the segment IDs
            // are the names of the corresponding bodies.
            //
            segid   = daf0.getArrayName();

            IDFound = false;

            try
            {
               xBody   = new Body( segid );

               xCode   = xBody.getIDCode();

               IDFound = true;
            }
            catch ( SpiceException exc )
            {
               xBody = null;
               xCode = 0;
            }


            //
            // Fetch the integer component of the segment
            // descriptor.
            //
            intComp = daf0.getIntegerSummaryComponent();


            if ( IDFound )
            {
               //
               // Check the body name. This should match the segment ID,
               // if the body is known to SPICE. Don't worry about the few
               // creative names that aren't built in.
               //
               ok    = JNITestutils.chcksi( "int comp 0",
                                            intComp[0],
                                            "=",
                                            xBody.getIDCode(),
                                            0                  );
            }

            //
            // Check the central body: just make sure it's known to
            // the SPICE system.
            //
            IDFound = false;

            try
            {
               center = new Body(intComp[1]);

               name   = center.getName();

               IDFound = true;
            }
            catch( SpiceException exc )
            {
               center = null;
            }

            ok  = JNITestutils.chcksl ( "center ID found", IDFound, true );


            //
            // Check the frame ID: just make sure it's known to
            // the SPICE system.
            //
            IDFound = false;

            try
            {
               frame = new ReferenceFrame(intComp[2]);

               name   = frame.getName();

               IDFound = true;
            }
            catch( SpiceException exc )
            {
               center = null;
            }

            ok  = JNITestutils.chcksl ( "frame ID found", IDFound, true );


            //
            // Check the data type; all the segments are type 5 or 8.
            //
            ok    = JNITestutils.chcksl( "int comp 3",
                                         ( intComp[3] == 5 ) ||
                                         ( intComp[3] == 8 ),
                                         true                   );


            //      System.out.println( intComp[1] );


            //
            // Check the addresses: just make sure the start is positive
            // and the end is greater than or equal to the start.
            //
            ok    = JNITestutils.chcksl( "int comp 4",
                                         ( intComp[4] >  0          ) &&
                                         ( intComp[4] <= intComp[5] ),
                                         true                            );

            //
            // Advance to the next segment.
            //
            found = daf0.findNextArray();
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test countSegments: compare against the " +
                              "count from the previous search."              );

         n  = daf0.countSegments();

         ok = JNITestutils.chcksi( "segment count",
                                   n,
                                   "=",
                                   nSegments,
                                   0                    );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Search the previously opened SPK in the " +
                              "backward direction."                         );

         bounds = new TDBTime[2];

         xStart = new TDBTime( "1984 FEB 27 11:06:40.000 TDB" );
         xStop  = new TDBTime( "2015 NOV 05 12:53:20.000 TDB" );


         nSegments = 0;

         daf0.beginBackwardSearch();

         found = daf0.findPreviousArray();

         while ( found )
         {
            ++nSegments;

            //
            // Fetch the double precision component of the segment
            // descriptor.
            //
            dpComp    = daf0.getDoubleSummaryComponent();

            bounds[0] = new TDBTime( dpComp[0] );
            bounds[1] = new TDBTime( dpComp[1] );

            ok        = JNITestutils.chcksd( "descr start",
                                             bounds[0].getTDBSeconds(),
                                             "~/",
                                             xStart.getTDBSeconds(),
                                             TIGHT_TOL                );

            ok        = JNITestutils.chcksd( "descr stop",
                                             bounds[1].getTDBSeconds(),
                                             "~/",
                                             xStop.getTDBSeconds(),
                                             TIGHT_TOL                );

            //
            // Fetch the current segment ID. In this file, the segment IDs
            // are the names of the corresponding bodies.
            //
            segid   = daf0.getArrayName();

            IDFound = false;

            try
            {
               xBody   = new Body( segid );

               xCode   = xBody.getIDCode();

               IDFound = true;
            }
            catch ( SpiceException exc )
            {
               xBody = null;
               xCode = 0;
            }


            //
            // Fetch the integer component of the segment
            // descriptor.
            //
            intComp = daf0.getIntegerSummaryComponent();


            if ( IDFound )
            {
               //
               // Check the body name. This should match the segment ID,
               // if the body is known to SPICE. Don't worry about the few
               // creative names that aren't built in.
               //
               ok    = JNITestutils.chcksi( "int comp 0",
                                            intComp[0],
                                            "=",
                                            xBody.getIDCode(),
                                            0                  );
            }

            //
            // Check the central body: just make sure it's known to
            // the SPICE system.
            //
            IDFound = false;

            try
            {
               center = new Body(intComp[1]);

               name   = center.getName();

               IDFound = true;
            }
            catch( SpiceException exc )
            {
               center = null;
            }

            ok  = JNITestutils.chcksl ( "center ID found", IDFound, true );


            //
            // Check the frame ID: just make sure it's known to
            // the SPICE system.
            //
            IDFound = false;

            try
            {
               frame = new ReferenceFrame(intComp[2]);

               name   = frame.getName();

               IDFound = true;
            }
            catch( SpiceException exc )
            {
               center = null;
            }

            ok  = JNITestutils.chcksl ( "frame ID found", IDFound, true );


            //
            // Check the data type; all the segments are type 5 or 8.
            //
            ok    = JNITestutils.chcksl( "int comp 3",
                                         ( intComp[3] == 5 ) ||
                                         ( intComp[3] == 8 ),
                                         true                   );


            //      System.out.println( intComp[1] );


            //
            // Check the addresses: just make sure the start is positive
            // and the end is greater than or equal to the start.
            //
            ok    = JNITestutils.chcksl( "int comp 4",
                                         ( intComp[4] >  0          ) &&
                                         ( intComp[4] <= intComp[5] ),
                                         true                            );

            //
            // Back up to the previous segment.
            //
            found = daf0.findPreviousArray();
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Close the DAF. Make sure it's neither " +
                              "readable nor writable."                   );

         daf0.close();

         readable = daf0.isReadable();
         ok       = JNITestutils.chcksl ( "readable", readable, false );

         writable = daf0.isWritable();
         ok       = JNITestutils.chcksl ( "writable", writable, false );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Search a DAF opened for write access in the " +
                              "forward direction."                           );


         daf0 = DAF.openForWrite( SPK );

         bounds = new TDBTime[2];

         xStart = new TDBTime( "1984 FEB 27 11:06:40.000 TDB" );
         xStop  = new TDBTime( "2015 NOV 05 12:53:20.000 TDB" );


         nSegments = 0;

         daf0.beginForwardSearch();

         found = daf0.findNextArray();

         while ( found )
         {
            ++nSegments;

            //
            // Fetch the double precision component of the segment
            // descriptor.
            //
            dpComp    = daf0.getDoubleSummaryComponent();

            bounds[0] = new TDBTime( dpComp[0] );
            bounds[1] = new TDBTime( dpComp[1] );

            ok        = JNITestutils.chcksd( "descr start",
                                             bounds[0].getTDBSeconds(),
                                             "~/",
                                             xStart.getTDBSeconds(),
                                             TIGHT_TOL                );

            ok        = JNITestutils.chcksd( "descr stop",
                                             bounds[1].getTDBSeconds(),
                                             "~/",
                                             xStop.getTDBSeconds(),
                                             TIGHT_TOL                );

            //
            // Fetch the current segment ID. In this file, the segment IDs
            // are the names of the corresponding bodies.
            //
            segid   = daf0.getArrayName();

            IDFound = false;

            try
            {
               xBody   = new Body( segid );

               xCode   = xBody.getIDCode();

               IDFound = true;
            }
            catch ( SpiceException exc )
            {
               xBody = null;
               xCode = 0;
            }


            //
            // Fetch the integer component of the segment
            // descriptor.
            //
            intComp = daf0.getIntegerSummaryComponent();


            if ( IDFound )
            {
               //
               // Check the body name. This should match the segment ID,
               // if the body is known to SPICE. Don't worry about the few
               // creative names that aren't built in.
               //
               ok    = JNITestutils.chcksi( "int comp 0",
                                            intComp[0],
                                            "=",
                                            xBody.getIDCode(),
                                            0                  );
            }

            //
            // Check the central body: just make sure it's known to
            // the SPICE system.
            //
            IDFound = false;

            try
            {
               center = new Body(intComp[1]);

               name   = center.getName();

               IDFound = true;
            }
            catch( SpiceException exc )
            {
               center = null;
            }

            ok  = JNITestutils.chcksl ( "center ID found", IDFound, true );


            //
            // Check the frame ID: just make sure it's known to
            // the SPICE system.
            //
            IDFound = false;

            try
            {
               frame = new ReferenceFrame(intComp[2]);

               name   = frame.getName();

               IDFound = true;
            }
            catch( SpiceException exc )
            {
               center = null;
            }

            ok  = JNITestutils.chcksl ( "frame ID found", IDFound, true );


            //
            // Check the data type; all the segments are type 5 or 8.
            //
            ok    = JNITestutils.chcksl( "int comp 3",
                                         ( intComp[3] == 5 ) ||
                                         ( intComp[3] == 8 ),
                                         true                   );


            //      System.out.println( intComp[1] );


            //
            // Check the addresses: just make sure the start is positive
            // and the end is greater than or equal to the start.
            //
            ok    = JNITestutils.chcksl( "int comp 4",
                                         ( intComp[4] >  0          ) &&
                                         ( intComp[4] <= intComp[5] ),
                                         true                            );

            //
            // Advance to the next segment.
            //
            found = daf0.findNextArray();
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Search the previously opened SPK in the " +
                              "backward direction."                         );

         bounds = new TDBTime[2];

         xStart = new TDBTime( "1984 FEB 27 11:06:40.000 TDB" );
         xStop  = new TDBTime( "2015 NOV 05 12:53:20.000 TDB" );


         nSegments = 0;

         daf0.beginBackwardSearch();

         found = daf0.findPreviousArray();

         while ( found )
         {
            ++nSegments;

            //
            // Fetch the double precision component of the segment
            // descriptor.
            //
            dpComp    = daf0.getDoubleSummaryComponent();

            bounds[0] = new TDBTime( dpComp[0] );
            bounds[1] = new TDBTime( dpComp[1] );

            ok        = JNITestutils.chcksd( "descr start",
                                             bounds[0].getTDBSeconds(),
                                             "~/",
                                             xStart.getTDBSeconds(),
                                             TIGHT_TOL                );

            ok        = JNITestutils.chcksd( "descr stop",
                                             bounds[1].getTDBSeconds(),
                                             "~/",
                                             xStop.getTDBSeconds(),
                                             TIGHT_TOL                );

            //
            // Fetch the current segment ID. In this file, the segment IDs
            // are the names of the corresponding bodies.
            //
            segid   = daf0.getArrayName();

            IDFound = false;

            try
            {
               xBody   = new Body( segid );

               xCode   = xBody.getIDCode();

               IDFound = true;
            }
            catch ( SpiceException exc )
            {
               xBody = null;
               xCode = 0;
            }


            //
            // Fetch the integer component of the segment
            // descriptor.
            //
            intComp = daf0.getIntegerSummaryComponent();


            if ( IDFound )
            {
               //
               // Check the body name. This should match the segment ID,
               // if the body is known to SPICE. Don't worry about the few
               // creative names that aren't built in.
               //
               ok    = JNITestutils.chcksi( "int comp 0",
                                            intComp[0],
                                            "=",
                                            xBody.getIDCode(),
                                            0                  );
            }

            //
            // Check the central body: just make sure it's known to
            // the SPICE system.
            //
            IDFound = false;

            try
            {
               center = new Body(intComp[1]);

               name   = center.getName();

               IDFound = true;
            }
            catch( SpiceException exc )
            {
               center = null;
            }

            ok  = JNITestutils.chcksl ( "center ID found", IDFound, true );


            //
            // Check the frame ID: just make sure it's known to
            // the SPICE system.
            //
            IDFound = false;

            try
            {
               frame = new ReferenceFrame(intComp[2]);

               name   = frame.getName();

               IDFound = true;
            }
            catch( SpiceException exc )
            {
               center = null;
            }

            ok  = JNITestutils.chcksl ( "frame ID found", IDFound, true );


            //
            // Check the data type; all the segments are type 5 or 8.
            //
            ok    = JNITestutils.chcksl( "int comp 3",
                                         ( intComp[3] == 5 ) ||
                                         ( intComp[3] == 8 ),
                                         true                   );


            //      System.out.println( intComp[1] );


            //
            // Check the addresses: just make sure the start is positive
            // and the end is greater than or equal to the start.
            //
            ok    = JNITestutils.chcksl( "int comp 4",
                                         ( intComp[4] >  0          ) &&
                                         ( intComp[4] <= intComp[5] ),
                                         true                            );

            //
            // Back up to the previous segment.
            //
            found = daf0.findPreviousArray();
         }

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Close the DAF. Make sure it's neither " +
                              "readable nor writable."                   );

         daf0.close();

         readable = daf0.isReadable();
         ok       = JNITestutils.chcksl ( "readable", readable, false );

         writable = daf0.isWritable();
         ok       = JNITestutils.chcksl ( "writable", writable, false );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Add comments to a DAF." );

         daf0      = DAF.openForWrite( SPK );

         n         = 1000;
         xComments = new String[n];

         for( i = 0;  i < n;  i++ )
         {
            xComments[i] = "This is line " + i + " of the comments.";
         }

         daf0.addComments( xComments );


         //
         // Check the comments before we close the file.
         //
         comments = daf0.readComments( MAX_LINE_LENGTH );

         for( i = 0;  i < comments.length;  i++ )
         {

            ok = JNITestutils.chcksc( "comment line " + i,
                                      comments[i],
                                      "=",
                                      xComments[i]         );
         }


         daf0.close();


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Open the file for read access; read and " +
                              "check the comments just written."             );

         daf0     = DAF.openForRead( SPK );

         n         = 1000;
         xComments = new String[n];

         for( i = 0;  i < n;  i++ )
         {
            xComments[i] = "This is line " + i + " of the comments.";
         }

         //
         // We should be able to fetch the comments from the file.
         //
         comments = daf0.readComments( MAX_LINE_LENGTH );

         for( i = 0;  i < comments.length;  i++ )
         {

            ok = JNITestutils.chcksc( "comment line " + i,
                                      comments[i],
                                      "=",
                                      xComments[i]         );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Delete the comments." );

         daf0.close();
         daf0 = DAF.openForWrite( SPK );

         daf0.deleteComments();

         //
         // Try to fetch the comments from the file; verify there are none.
         //
         comments = daf0.readComments( MAX_LINE_LENGTH );

         ok       = JNITestutils.chcksi( "n lines", comments.length, "=",
                                                                        0, 0 );

         daf0.close();

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Re-open the file and check the comment area." );

         daf0.close();
         daf0 = DAF.openForRead( SPK );

         //
         // Try to fetch the comments from the file; verify there are none.
         //
         comments = daf0.readComments( MAX_LINE_LENGTH );

         ok       = JNITestutils.chcksi( "n lines", comments.length, "=",
                                                                        0, 0 );

         daf0.close();


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
         CSPICE.spkuef( handle0 );

         ( new File ( SPK ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

