
package spice.tspice;


import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;

/**
Class TestKernelDatabase provides methods that implement test families for
the class KernelDatabase.

<h3>Version 2.0.0 27-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 27-OCT-2009 (NJB)</h3>
*/
public class TestKernelDatabase extends Object
{

   //
   // Class constants
   //
   private static String  CK            = "keeptest.bc";
   private static String  SPK           = "keeptest.bsp";
   private static String  META          = "meta.ker";
   private static String  SCLK          = "keeptest.tsc";

   private static int     NMETA         = 6;


   //
   // Class variables
   //

   //
   // Methods
   //

   /**
   Test KernelDatabase and associated classes.
   */
   public static boolean f_KernelDatabase()

      throws SpiceException, IOException

   {
      //
      // Constants
      //

      //
      // Local variables
      //
      String                            qname;

      String                            name;

      String[]                          files    =  { SPK,   CK,   SCLK };

      String                            fileType;

      String[]                          ftypes   =  { "SPK", "CK", "TEXT" };

      String[]                          metatxt  =
                                        {
                                       "\\begindata",
                                       " ",
                                       "KERNELS_TO_LOAD =  ( 'keeptest.bsp',",
                                       "                     'keeptest.bc',",
                                       "                     'keeptest.tsc' )",
                                       "\\begintext"
                                        };

      String                            source;
      String                            type;

      boolean                           ok;

      int                               ckhan;
      int                               count;
      int                               han;
      int                               handle = 0;
      int                               j;
      int                               n;


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

         JNITestutils.topen ( "f_KernelDatabase" );


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

         //
         // Don't load the SPK using tstspk; use the furnsh-level loader.
         //
         handle = JNITestutils.tstspk( SPK, false );

         KernelDatabase.load( SPK );

         //
         // Delete CK and SCLK if they exist. Create and load a PCK file.
         // Do NOT delete the file afterward.
         //
         ( new File ( CK   ) ).delete();
         ( new File ( SCLK ) ).delete();

         //
         // Don't load the CK and SCLK kernel using tstck3;
         // use the furnsh-level loader.
         //
         ckhan = JNITestutils.tstck3( CK, SCLK, false, false, true );

         KernelDatabase.load( CK   );
         KernelDatabase.load( SCLK );



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Call load with an empty file name."   );

         try
         {
            KernelDatabase.load ( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(EMPTYSTRING)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Call ktotal with an empty file name."   );

         try
         {
            KernelDatabase.ktotal ( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(EMPTYSTRING)", ex );
         }







         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test ktotal; find out how many " +
                              "kernels are loaded."                );


         //
         // There should be three kernels loaded.
         //
         count = KernelDatabase.ktotal ( "ALL" );

         ok    = JNITestutils.chcksi( "Kernel count", count, "=", 3, 0 );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getFileName; get the name of each loaded " +
                              "kernel."                                     );


         for ( int i = 0;  i < count;  i++ )
         {
            name = KernelDatabase.getFileName( i, "ALL" );

            ok   = JNITestutils.chcksc( "Kernel name", name, "=", files[i] );

         }

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getFileType; get the type of each " +
                              "loaded kernel."                               );


         for ( int i = 0;  i < count;  i++ )
         {
            fileType = KernelDatabase.getFileType( files[i] );

            ok       = JNITestutils.chcksc( "Kernel type", fileType, "=",
                                                                   ftypes[i] );

         }

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getHandle; get the handle of each " +
                              "loaded kernel."                               );


         for ( int i = 0;  i < count;  i++ )
         {
            fileType = KernelDatabase.getFileType( files[i] );

            if (     ( !fileType.equals( "TEXT" ) )
                 &&  ( !fileType.equals( "META" ) )  )
            {
               han   = KernelDatabase.getHandle( files[i] );

               ok    = JNITestutils.chcksi( "handle sign", han, ">", 0, 0 );
            }

         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getSource; get the source of each " +
                              "loaded kernel."                               );


         for ( int i = 0;  i < count;  i++ )
         {
            source = KernelDatabase.getSource( files[i] );

            //
            // In this case we expect the source to be blank. Later we'll try
            // the test case in which the files come from a meta-kernel.
            //
            // We use chcksl here because we can't test an empty string using
            // our scalar string test utility.
            //
            ok = JNITestutils.chcksl( "source == \"\"", source.equals(""),
                                                                        true );

         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test unload; find out how many " +
                              "kernels are loaded after each call."  );

         for ( int i = 0;  i < count;  i++ )
         {
            KernelDatabase.unload( files[i] );

            j  = KernelDatabase.ktotal( "ALL" );

            ok = JNITestutils.chcksi( "total", j, "=", count-i-1, 0 );
         }

         //
         // There should be zero kernels loaded after the clear call.
         //


         KernelDatabase.clear();

         count = KernelDatabase.ktotal ( "ALL" );

         ok    = JNITestutils.chcksi( "Kernel count", count, "=", 0, 0 );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getFileName, getSource, getFileType " +
                              "and getHandle in the meta-kernel case. "    );


         //
         // Create a new meta-kernel.  The contents will be:
         //
         //
         //    \begindata
         //
         //        KERNELS_TO_LOAD =  ( 'keeptest.bsp',
         //                             'keeptest.bc',
         //                             'keeptest.tsc' )
         //

         ( new File ( META ) ).delete();



         FileWriter fw = new FileWriter( META, false );

         // Enable autoflush

         PrintWriter pw = new PrintWriter( fw, true );

         for ( int i = 0;  i < NMETA;  i++ )
         {
            pw.format( "%s%n", metatxt[i] );
         }

         pw.close();


         //
         // Ok, see if load likes our artistic efforts.
         //
         KernelDatabase.load ( META );

         //
         // There should be one SPK loaded.
         //
         count = KernelDatabase.ktotal ( "SPK" );

         ok    = JNITestutils.chcksi( "SPK count", count, "=", 1, 0 );


         //
         // There should be one CK loaded.
         //
         count = KernelDatabase.ktotal ( "CK" );

         ok    = JNITestutils.chcksi( "CK count", count, "=", 1, 0 );


         //
         // There should be one text kernel loaded.
         //
         count = KernelDatabase.ktotal ( "text" );

         ok    = JNITestutils.chcksi( "Text kernel count", count, "=", 1, 0 );


         //
         // There should be one (meta) kernel loaded.
         //
         count = KernelDatabase.ktotal ( "meta" );

         ok    = JNITestutils.chcksi( "Meta kernel count", count, "=", 1, 0 );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getSource; get the source of each " +
                              "loaded kernel, except for the meta-kernel."   );


         for ( int i = 0;  i < count;  i++ )
         {
            source = KernelDatabase.getSource( files[i] );

            ok     = JNITestutils.chcksc( "source", source, "=", META );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test clear; find out how many " +
                              "kernels are loaded after the call."  );


         //
         // There should be zero kernels loaded after the clear call.
         //
         KernelDatabase.clear();

         count = KernelDatabase.ktotal ( "ALL" );

         ok    = JNITestutils.chcksi( "Kernel count", count, "=", 0, 0 );
      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         // ex.printStackTrace();

         ok = JNITestutils.chckth ( false, "", ex );
      }

      finally
      {
         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Clean up." );

         //
         // Get rid of the SPK file.
         //
         CSPICE.spkuef( handle );

         ( new File ( SPK ) ).delete();

         //
         // Get rid of the CK file.
         //
         CSPICE.ckupf( handle );

         ( new File ( CK ) ).delete();

         //
         // Get rid of the meta-kernel and SCLK file.
         //
         ( new File ( META ) ).delete();
         ( new File ( SCLK ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

