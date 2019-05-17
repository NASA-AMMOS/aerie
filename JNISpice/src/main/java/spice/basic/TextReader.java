
package spice.basic;

import java.io.BufferedReader;
import java.io.*;



/**
Class TextReader provides a simple interface for
reading text files sequentially.

<h3>Code Examples</h3>

1) The following program writes a small text file, then reads and prints
   all lines of the file.

<pre>
   import java.io.*;
   import spice.basic.*;

   //
   // Create a new text file, write to it, then
   // read back what was written. The name of the
   // file is supplied on the command line.
   //
   class TextIOEx1
   {
      //
      // Load the JNISpice shared library.
      //
      static { System.loadLibrary( "JNISpice" ); }

      public static void main ( String[] args )

         throws SpiceException
      {
         //
         // Create a small text file.
         //
         final  int  NLINES = 10;
         String      fname  = args[0];

         TextWriter tw = new TextWriter( fname );

         for ( int i = 0; i < NLINES; i++ )
         {
            String line = "This is line " + i + " of the text file.";

            tw.writeLine( line );
         }

         //
         // Close the TextWriter.
         //
         tw.close();

         //
         // Now read the text file and print each line as we
         // read it.
         //

         TextReader tr = new TextReader( fname );

         String line   = tr.getLine();

         while( line != null )
         {
            System.out.println( line );

            line = tr.getLine();
         }

         //
         // Close the TextReader.
         //
         tr.close();

      }
   }
</pre>

<p> The program can be executed using the command
<pre>
   java TextWriterEx1 ex1.txt
</pre>

<p>When run on a PC/Linux/java 1.6.0_14/gcc platform,
the output from this program was a file named "ex1.txt" containing
the text:

<pre>
   This is line 0 of the text file.
   This is line 1 of the text file.
   This is line 2 of the text file.
   This is line 3 of the text file.
   This is line 4 of the text file.
   This is line 5 of the text file.
   This is line 6 of the text file.
   This is line 7 of the text file.
   This is line 8 of the text file.
   This is line 9 of the text file.
</pre>



<p> Version 1.0.0 21-DEC-2009 (NJB)
*/
public class TextReader extends Object
{
   //
   // Fields
   //

   private String             fname       = null;
   private BufferedReader     inputStream = null;


   //
   // Constructors
   //

   /**
   Create a TextReader for a text file specified by name.
   */
   public TextReader( String fname )

      throws SpiceException
   {
      String                            msg;
      SpiceException                    ex;


      try
      {
         File infile = new File( fname );

         if ( !infile.exists() )
         {
            ex = SpiceErrorException.create(

               "TextReader.TextReader",

               "SPICE(FILENOTFOUND)",

               "Could not create BufferedReader for file " + fname +
               "because file doesn't exist."                          );

            throw( ex );
         }

         if ( !infile.canRead() )
         {
            ex = SpiceErrorException.create(

               "TextReader.TextReader",

               "SPICE(NOTREADABLE)",

               "Could not create BufferedReader for file " + fname +
               "because file is not readable."                       );

            throw( ex );
         }


         inputStream = new BufferedReader ( new FileReader(fname) );

         if ( inputStream == null )
         {
            ex = SpiceErrorException.create(

               "TextReader.TextReader",

               "SPICE(IOEXCEPTION)",

               "Could not create BufferedReader for file " + fname  );

            throw( ex );
         }
      }

      catch ( IOException ioexc )
      {

         msg = ioexc.getMessage();

         try
         {
            if ( inputStream != null )
            {
               inputStream.close();
            }
         }
         catch( IOException ioexc2 )
         {
            msg = "IOException was thrown while trying to close " +
                  "BufferedReader after an IOException was thrown " +
                  "(yes, that's two IOExceptions). Error message " +
                  "from close attempt IOException was <" +
                  ioexc2.getMessage() + ">" + " Error message from " +
                  "original IOException was " + ioexc.getMessage();
         }

         ex = SpiceErrorException.create(

            "TextReader.TextReader",

            "SPICE(IOEXCEPTION)",

            msg                          );

         throw( ex );
      }
   }


   /**
   Read the next line from the text file associated with this
   TextReader instance.

   <p> A null return value indicates the end of file.

   <p> This method closes the underlying input stream if
   end-of-file is reached or if an exception is thrown.
   */
   public String getLine()

      throws SpiceException
   {
      SpiceErrorException               ex;

      String                            line = null;
      String                            msg;


      try
      {
         line = inputStream.readLine();

         if ( line == null )
         {
            inputStream.close();
         }

      }
      catch ( IOException ioexc )
      {
         msg = ioexc.getMessage();

         try
         {
            if ( inputStream != null )
            {
               inputStream.close();
            }
         }
         catch( IOException ioexc2 )
         {
            msg = "IOException was thrown while trying to close " +
                  "BufferedReader after an IOException was thrown " +
                  "(yes, that's two IOExceptions). Error message " +
                  "from close attempt IOException was <" +
                  ioexc2.getMessage() + ">" + " Error message from " +
                  "original IOException was " + ioexc.getMessage();
         }

         ex = SpiceErrorException.create(

            "TextReader.getLine",

            "SPICE(IOEXCEPTION)",

            msg                          );

         throw( ex );
      }

      return line;
   }


   /**
   Close a TextReader instance.

   <p> This method allows the caller to close the underlying
   input stream in situations where it would not automatically
   be closed. Compare {@link #getLine} above.
   */
   public void close()

      throws SpiceException
   {
      try
      {

         inputStream.close();

      }
      catch( IOException ioexc )
      {
         SpiceErrorException ex = SpiceErrorException.create(

            "TextReader.close",

            "SPICE(IOEXCEPTION)",

            ioexc.getMessage()           );

         throw( ex );
      }
   }

}
