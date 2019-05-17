
package spice.basic;

import java.io.*;

/**
Class TextWriter provides a very simple interface that supports
writing lines of text to files.


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
public class TextWriter extends Object
{
   //
   // Fields
   //
   private PrintWriter                  pw;
   private String                       filename;

   /**
   Create a TextWriter instance associated with a specified file.
   */
   public TextWriter( String filename )

      throws SpiceException
   {
      try
      {
         FileWriter fw = new FileWriter( filename, false );

         //
         // Create PrintWriter instance; enable autoflush.
         //
         pw = new PrintWriter( fw, true );

         this.filename = filename;
      }
      catch( IOException ioexc )
      {
         SpiceErrorException ex = SpiceErrorException.create(

            "TextWriter.TextWriter",

            "SPICE(IOEXCEPTION)",

            ioexc.getMessage()            );

         throw( ex );
      }

   }

   /**
   Write a string to the file associated with this TextWriter instance. A
   newline is automatically written.

   <p> The caller is responsible for calling {@link #close()} after all
   lines have been written to the file.
   */
   public void writeLine( String line )

      throws SpiceException
   {
      pw.format( "%s%n", line );

      if ( pw.checkError() )
      {
         //
         // An error occurred during the write operation.
         // Unfortunately, PrintWriter doesn't tell us specifically
         // what went wrong.
         //
         SpiceErrorException ex = SpiceErrorException.create(

            "writeLine",

            "SPICE(WRITEERROR)",

            "PrintWriter associated with" + filename +
            "indicated an error while trying to write " +
            "line <" + line + ">"                        );

         throw( ex );
      }
   }

   /**
   Close this TestWriter instance.
   */
   public void close()
   {
      pw.close();
   }
}
