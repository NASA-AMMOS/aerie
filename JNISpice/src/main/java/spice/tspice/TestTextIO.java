
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestTextIO provides methods that implement test families for
the classes TextReader and TextWriter.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 21-DEC-2009 (NJB)</h3>
*/
public class TestTextIO extends Object
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
   Test classes TextReader and TextWriter.
   */
   public static boolean f_TextIO()

      throws SpiceException
   {
      //
      // Constants
      //
      final int                         N_LINES = 100;
      final int                         N_FILES = 10;


      //
      // Local variables
      //
      String[]                          buffer;
      String                            filename0;
      String[]                          filenames = new String[N_FILES];
      String                            line0;
      String[][]                        multiBuffer;

      TextReader                        tr0;
      TextReader[]                      trs;

      TextWriter                        tw0;
      TextWriter[]                      tws;

      boolean                           ok;

      int                               i;
      int                               j;

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

         JNITestutils.topen ( "f_TextIO" );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: try to read from a file " +
                               "that doesn't exist."               );

         try
         {

            filename0 = "textfile0";

            tr0       = new TextReader( filename0 );

            line0     = tr0.getLine();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(FILENOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(FILENOTFOUND)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Write a text file and read it back."  );


         filename0 = "textfile0";

         tw0 = new TextWriter( filename0 );

         //
         // Write to a new text file.
         //
         buffer = new String[N_LINES];

         for ( i = 0;  i < N_LINES;  i++ )
         {
            buffer[i] = "This is line " + i + " of file " + filename0;

            tw0.writeLine( buffer[i] );
         }

         tw0.close();

         //
         // Now read the file and compare the text we read against
         // the text we wrote.
         //

         tr0   = new TextReader( filename0 );

         i     = 0;
         line0 = tr0.getLine();

         while( line0 != null )
         {
            // For debugging:
            //System.out.println( line0 );

            ok = JNITestutils.chcksc ( filename0 + " line " + i,
                                       line0,
                                       "=",
                                       buffer[i]               );
            ++i;

            line0 = tr0.getLine();
         }

         //
         // Delete the file.
         //

         ( new File(filename0) ).delete();


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Write to multiple files in interleaved " +
                              "fashion."  );

         //
         // Create TextWriters for N_FILES new files.
         //
         filenames = new String    [N_FILES];
         tws       = new TextWriter[N_FILES];

         for ( i = 0;  i < N_FILES;  i++ )
         {
            filenames[i] = "textfile" + i;

            tws[i]       = new TextWriter( filenames[i] );
         }

         //
         // Write to each new text file.
         //
         multiBuffer = new String[N_FILES][N_LINES];

         for ( i = 0;  i < N_LINES;  i++ )
         {
            //
            // Write the ith line to each file.
            //
            for ( j = 0;  j < N_FILES; j++ )
            {
               multiBuffer[j][i] = "This is line " + i + " of file " +
                                                                  filenames[j];

               tws[j].writeLine( multiBuffer[j][i] );
            }
         }

         //
         // Close the TextWriters.
         //
         for ( j = 0;  j < N_FILES; j++ )
         {
            tws[j].close();
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Read from multiple files in interleaved " +
                              "fashion."  );

         //
         // We read the files created in the previous test case.
         //
         trs       = new TextReader[N_FILES];
         filenames = new String[N_FILES];

         for ( i = 0;  i < N_FILES;  i++ )
         {
            filenames[i] = "textfile" + i;

            trs[i] = new TextReader( filenames[i] );
         }

         for ( i = 0;  i < N_LINES;  i++ )
         {
            //
            // Read the ith line from each file.
            //
            for ( j = 0;  j < N_FILES; j++ )
            {
               //
               // Read the ith line from the jth file.
               //
               line0 = trs[j].getLine();

               // For debugging:
               // System.out.println( line0 );


               ok = JNITestutils.chcksc ( filenames[j] + " line " + i,
                                          line0,
                                          "=",
                                          multiBuffer[j][i]             );
            }
         }


         //
         // Close all of the TextReaders.
         //
         for ( i = 0;  i < N_FILES;  i++ )
         {
            trs[i].close();
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

         for ( i = 0;  i < N_FILES;  i++ )
         {
            filenames[i] = "textfile" + i;

            ( new File( filenames[i] ) ).delete();
         }
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

