
package spice.basic;

import java.io.*;

/**
Class IOUtils provides miscellaneous static methods
that simplify common console I/O tasks.

<p> Version 1.0.0 15-JUL-2009 (NJB)
*/
public class IOUtils
{

   /**
   Display a string that prompts the user for an input
   string; read and return the string supplied by the user.
   */
   public static String prompt ( String outputText )

      throws IOException
   {
      InputStreamReader isr = new InputStreamReader( System.in );
      BufferedReader    br  = new BufferedReader    ( isr );

      System.out.format ( "%s", outputText );

      String inputString = br.readLine();

      return ( inputString );
   }
}
