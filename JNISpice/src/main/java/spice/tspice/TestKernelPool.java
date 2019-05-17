
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.KernelVarDescriptor.*;


/**
Class TestKernelPool provides methods that implement test families for
the class KernelPool.

<pre>
Version 1.1.0 20-JUN-2013 (NJB)(BVS)

   Changed MAXVAR check value from 5003 to 26003 for N0065 
   POOL buffer increase.

Version 1.0.0 03-DEC-2009 (NJB)
</pre>
*/
public class TestKernelPool extends Object
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
   Test KernelPool and associated classes.
   */
   public static boolean f_KernelPool()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-9;

      final int                         BUFDIM       = 100;
      final int                         LMPOOL_NVARS = 5;
      final int                         NLINES       = 27;
      final int                         PCPOOL_DIM   = 10;
      final int                         PDPOOL_DIM   = 20;
      final int                         PIPOOL_DIM   = 30;
      final int                         SPK_BUFSIZE  = 5;

      final String                      LSK            = "ldpool.tls";
      final String                      PCPOOL_VAR     = "pcpool_array";
      final String                      PCPOOL_VAR_TMP = "pcpool_val_%d";

      final String                      PDPOOL_VAR     = "pdpool_array";
      final String                      PIPOOL_VAR     = "pipool_array";
      final String                      PCPOOL_VAL_TMP = "pcpool_val_%d";

      final String                      SPK_FILE_VAR   = "SPK_FILES";

      final String                      SPK_FILE0      =

                          "this_is_the_full_path_specification_"  +
                          "of_a_file_with_a_long_name";

      final String                      SPK_FILE1      =

                          "this_is_the_full_path_specification_"  +
                          "of_a_second_file_with_a_very_long_name";



      //
      // Local variables
      //
      boolean                           found;
      boolean                           ok;
      boolean                           update;

      double[]                          dvals;
      double[]                          putDoubleArray;

      int                               dtype;
      int                               handle;
      int                               i;
      int[]                             ivals;
      int[]                             lmpoolDims = { 46, 1, 1, 1, 2 };
      int                               n;
      int[]                             putIntArray;

      KernelVarDescriptor               KVDescr;

      KernelVarStringComponent          KVComp;

      String[]                          kervars;

      String                            kvname;

      String[]                          spkbuf = {

         "SPK_FILES = ( 'this_is_the_full_path_specification_*'",
                       "'of_a_file_with_a_long_name'",
                       "'this_is_the_full_path_specification_*'",
                       "'of_a_second_file_with_a_very_long_*'",
                       "'name' )"
                                                                    };


      String[]                          textbuf = {

                       "DELTET/DELTA_T_A = 32.184",
                       "DELTET/K         = 1.657D-3",
                       "DELTET/EB        = 1.671D-2",
                       "DELTET/M         = ( 6.239996 1.99096871D-7 )",
                       "DELTET/DELTA_AT  = ( 10, @1972-JAN-1",
                       "                     11, @1972-JUL-1",
                       "                     12, @1973-JAN-1",
                       "                     13, @1974-JAN-1",
                       "                     14, @1975-JAN-1",
                       "                     15, @1976-JAN-1",
                       "                     16, @1977-JAN-1",
                       "                     17, @1978-JAN-1",
                       "                     18, @1979-JAN-1",
                       "                     19, @1980-JAN-1",
                       "                     20, @1981-JUL-1",
                       "                     21, @1982-JUL-1",
                       "                     22, @1983-JUL-1",
                       "                     23, @1985-JUL-1",
                       "                     24, @1988-JAN-1",
                       "                     25, @1990-JAN-1",
                       "                     26, @1991-JAN-1",
                       "                     27, @1992-JUL-1",
                       "                     28, @1993-JUL-1",
                       "                     29, @1994-JUL-1",
                       "                     30, @1996-JAN-1",
                       "                     31, @1997-JUL-1",
                       "                     32, @1999-JAN-1 )"
                                                                      };

      String[]                          cvals;
      String[]                          emptyBuf;
      String[]                          files;
      String[]                          kerBuffer;

      String[]                          lmpoolNames = {

                              "DELTET/DELTA_AT",
                              "DELTET/DELTA_T_A",
                              "DELTET/EB",
                              "DELTET/K",
                              "DELTET/M"
                                                      };

      String                            name;
      String                            outStr;
      String[]                          putCharacterArray;

      StringBuilder                     sb;

      TDBTime                           et;









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

         JNITestutils.topen ( "f_KernelPool" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: create and load kernels." );


         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();


         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: load zero-length buffer via " +
                               "loadFromBuffer "                       );


         try
         {
            emptyBuf = new String[0];

            KernelPool.loadFromBuffer( emptyBuf );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(ZEROLENGTHARRAY)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(ZEROLENGTHARRAY)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: load zero-length strings via " +
                               "loadFromBuffer "                       );


         try
         {
            emptyBuf = new String[2];

            emptyBuf[0] = "";
            emptyBuf[1] = "";

            KernelPool.loadFromBuffer( emptyBuf );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to getAttributes."   );

         try
         {
            KVDescr = KernelPool.getAttributes( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Error: getAttributes: attempt to get " +
                              "attributes of a non-existent variable."   );


         try
         {
            KVDescr = KernelPool.getAttributes( "<bogus_variable>" );

            i       = KVDescr.getSize();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(KERNELVARNOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(KERNELVARNOTFOUND)", ex );
         }

         try
         {
            KVDescr = KernelPool.getAttributes( "<bogus_variable>" );

            i       = KVDescr.getDataType();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(KERNELVARNOTFOUND)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(KERNELVARNOTFOUND)", ex );
         }





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty template to getNames."   );

         try
         {
            kervars = KernelPool.getNames( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to `delete'."   );

         try
         {
            KernelPool.delete( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty agent to setWatch."   );

         try
         {
            KernelPool.setWatch( "", lmpoolNames );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name array to setWatch."   );

         try
         {
            emptyBuf = new String[0];

            KernelPool.setWatch( "", emptyBuf );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass array of empty kernel variable " +
                               "name strings to setWatch."   );

         try
         {
            emptyBuf    = new String[2];

            emptyBuf[0] = "";
            emptyBuf[1] = "";

            KernelPool.setWatch( "", emptyBuf );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty agent to checkWatch."   );

         try
         {
            KernelPool.checkWatch( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to putCharacter."   );

         try
         {
            KernelPool.putCharacter( "", textbuf );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: pass empty value array to " +
                              "putCharacter." );

         try
         {
            emptyBuf = new String[0];

            KernelPool.putCharacter( "emptyBuf", emptyBuf );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(ZEROLENGTHARRAY)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(ZEROLENGTHARRAY)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass array of empty value strings " +
                               "to putCharacter."                           );

         try
         {
            emptyBuf = new String[2];

            emptyBuf[0] = "";
            emptyBuf[1] = "";

            KernelPool.putCharacter( "emptyBuf", emptyBuf );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Error: pass empty name to " +
                              "getCharacter(name)." );

         try
         {
            KernelPool.getCharacter( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to " +
                               "getCharacter(name, start, room)." );

         try
         {
            KernelPool.getCharacter( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass invalid start to " +
                               "getCharacter(name, start, room)." );

         try
         {
            cvals = new String[1];

            cvals[0] = "cvals+0";

            KernelPool.putCharacter( PCPOOL_VAR, cvals );

            cvals = KernelPool.getCharacter( PCPOOL_VAR, 0, -1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BADARRAYSIZE)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(BADARRAYSIZE)", ex );
         }
         finally
         {
            KernelPool.clear();
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to putDouble."   );

         try
         {
            dvals = new double[1];

            KernelPool.putDouble( "", dvals );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to getDouble."   );

         try
         {
            dvals = KernelPool.getDouble( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to " +
                               "getDouble( name, start, room."   );

         try
         {
            dvals = KernelPool.getDouble( "", 0, 1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass invalid start to " +
                               "getDouble(name, start, room)." );

         try
         {
            dvals = new double[1];

            dvals[0] = 1;

            KernelPool.putDouble( PDPOOL_VAR, dvals );

            dvals = KernelPool.getDouble( PDPOOL_VAR, 0, -1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BADARRAYSIZE)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(BADARRAYSIZE)", ex );
         }
         finally
         {
            KernelPool.clear();
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to putInteger."   );

         try
         {
            ivals = new int[1];

            KernelPool.putInteger( "", ivals );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to getInteger."   );

         try
         {
            ivals = KernelPool.getInteger( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to " +
                               "getInteger( name, start, room."   );

         try
         {
            ivals = KernelPool.getInteger( "", 0, 1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass invalid start to " +
                               "getInteger(name, start, room)." );

         try
         {
            ivals = new int[1];

            ivals[0] = 1;

            KernelPool.putInteger( PIPOOL_VAR, ivals );

            ivals = KernelPool.getInteger( PIPOOL_VAR, 0, -1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BADARRAYSIZE)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(BADARRAYSIZE)", ex );
         }
         finally
         {
            KernelPool.clear();
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty name to `getParameter'."   );

         try
         {
            n = KernelPool.getParameter( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty kernel variable name " +
                               "to `getStringComponent'."   );

         try
         {
            KernelPool.loadFromBuffer( spkbuf );

            KVComp  = KernelPool.getStringComponent( "", 0, "*" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }
         finally
         {
            KernelPool.clear();
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass empty continuation marker " +
                               "to `getStringComponent'."   );

         try
         {
            KernelPool.loadFromBuffer( spkbuf );

            KVComp = KernelPool.getStringComponent( "SPK_FILES", 0, "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }
         finally
         {
            KernelPool.clear();
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: pass invalid index to " +
                               "to `getStringComponent'."   );

         try
         {
            KernelPool.loadFromBuffer( spkbuf );

            KVComp = KernelPool.getStringComponent( "SPK_FILES", -1, "*" );


            outStr = KVComp.getComponent();

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(NOCOMPONENT)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            // ex.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(NOCOMPONENT)", ex );
         }
         finally
         {
            KernelPool.clear();
         }


         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test loadFromBuffer; a leapseconds "       +
                              "kernel is (effectively) loaded via this "  +
                              "method."                                      );

         KernelPool.clear();

         KernelPool.loadFromBuffer( textbuf );

         //
         // If the kernel pool was loaded successfully, we should be able to
         // make a call that requires leapseconds kernel data. No error should
         // be signaled.
         //
         et = new TDBTime( "1999 JUN 7" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests getAttributes; we make sure " +
                              "this method returns correct info on the "     +
                              "variables loaded in the loadFromBuffer test." );


         for ( i = 0;  i < LMPOOL_NVARS;  i++ )
         {
            KVDescr = KernelPool.getAttributes( lmpoolNames[i] );

            ok      = JNITestutils.chcksl( "exists",
                                           KVDescr.exists(),
                                           true              );

            ok      = JNITestutils.chcksc( "name",
                                           KVDescr.getName(),
                                           "=",
                                           lmpoolNames[i]    );


            ok      = JNITestutils.chcksi( "size",
                                           KVDescr.getSize(),
                                           "=",
                                           lmpoolDims[i],
                                           0                    );

            ok      = JNITestutils.chcksi( "data type",
                                           KVDescr.getDataType(),
                                           "=",
                                           NUMERIC,
                                           0                     );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests individual attribute " +
                              "fetch methods; we make sure " +
                              "these methods return correct info on the "     +
                              "variables loaded in the loadFromBuffer test." );


         for ( i = 0;  i < LMPOOL_NVARS;  i++ )
         {
            name    = lmpoolNames[i];

            ok      = JNITestutils.chcksl( "exists",
                                           KernelPool.exists( name ),
                                           true                       );


            ok      = JNITestutils.chcksi( "size",
                                           KernelPool.getSize( name ),
                                           "=",
                                           lmpoolDims[i],
                                           0                    );

            ok      = JNITestutils.chcksi( "data type",
                                           KernelPool.getDataType( name ),
                                           "=",
                                           NUMERIC,
                                           0                     );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests getNames; we make sure "   +
                              "this method can return a correct list of " +
                              "the variable names in the pool."             );

         //
         // Make sure all the names are there.
         //
         for ( i = 0;  i < LMPOOL_NVARS;  i++ )
         {
            kervars = KernelPool.getNames( lmpoolNames[i] );

            ok      = JNITestutils.chcksi( "buffer size for name[" + i + "]",
                                           kervars.length,
                                           "=",
                                           1,
                                           0                                 );

            if ( kervars.length == 1 )
            {
              ok = JNITestutils.chcksc( "name[" + i + "]",
                                        kervars[0],
                                        "=",
                                        lmpoolNames[i]     );
            }
         }

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests getNames; we make sure "   +
                              "this method can return a correct list of " +
                              "names for an * template."                   );

         //
         // Make sure all the names are there.
         //
         kervars = KernelPool.getNames( "*" );

         ok      = JNITestutils.chcksi( "name count",
                                        kervars.length,
                                        "=",
                                        LMPOOL_NVARS,
                                        0              );

         //
         // The returned array has unknown order; sort it before testing
         // it against lmpoolNames, which is sorted.
         //
         Arrays.sort( kervars );

         for ( i = 0;  i < LMPOOL_NVARS;  i++ )
         {
            ok = JNITestutils.chcksc( "name[" + i + "]",
                                      kervars[i],
                                      "=",
                                      lmpoolNames[i]     );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests `delete'.  We'll delete "  +
                              "the loadFromBuffer variables from the pool."  );


         for ( i = 0;  i < LMPOOL_NVARS;  i++ )
         {
            name    = lmpoolNames[i];

            KernelPool.delete( name );

            ok      = JNITestutils.chcksl( lmpoolNames[i] + " exists",
                                           KernelPool.exists( name ),
                                           false                       );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests setWatch and checkWatch. " +
                              "We'll set a watch on all the variables "   +
                              "from the loadFromBuffer test. We'll load " +
                              "them again and make sure their agent is "  +
                              "notified. "                                   );


         KernelPool.setWatch( "pool_agent", lmpoolNames );


         KernelPool.loadFromBuffer( textbuf );

         update  = KernelPool.checkWatch ( "pool_agent" );

         ok      = JNITestutils.chcksl( "update", update, true );

         update  = KernelPool.checkWatch ( "pool_agent" );

         ok      = JNITestutils.chcksl( "update", update, false );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests `clear'." );

         KernelPool.clear();

         kervars = KernelPool.getNames( "*" );

         found   = kervars.length > 0;

         ok      = JNITestutils.chcksl( "found", found, false );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests putCharacter and "    +
                              "getCharacter.  A variable "           +
                              "associated with an array of strings " +
                              "is loaded via putCharacter. The "     +
                              "values are retrieved via "            +
                              "getCharacter."                         );


         //
         // Populate the putCharacter array with values.
         //
         putCharacterArray = new String[PCPOOL_DIM];

         try
         {
            for ( i = 0;  i < PCPOOL_DIM;  i++ )
            {
               putCharacterArray[i] = String.format( PCPOOL_VAL_TMP, i );
            }
         }
         catch ( Exception exc )
         {
            throw(  new SpiceException( exc.getMessage() )  );
         }

         //
         // Insert the variable into the kernel pool.
         //
         KernelPool.putCharacter ( PCPOOL_VAR, putCharacterArray );

         //
         // Retrieve the variable's associated values.
         //
         cvals = KernelPool.getCharacter( PCPOOL_VAR );

         //
         // Check the results.
         //
         found = cvals.length > 0;

         ok = JNITestutils.chcksl ( "found", found, true );


         ok = JNITestutils.chcksi ( "cvals.length",
                                    cvals.length,
                                    "=",
                                    PCPOOL_DIM,
                                    0              );

         for ( i = 0;  i < PCPOOL_DIM;  i++ )
         {
            ok = JNITestutils.chcksc ( "cvals["+i+"]",
                                       cvals[i],
                                       "=",
                                       putCharacterArray[i] );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Continue previous test using getCharacter( " +
                              "name, start, room)."                          );


         for ( i = 0;  i < PCPOOL_DIM;  i++ )
         {
            //
            // Retrieve the variable's associated values.
            //
            cvals = KernelPool.getCharacter( PCPOOL_VAR, i, 1 );

            //
            // Check the results.
            //
            found = (cvals.length > 0);

            ok = JNITestutils.chcksl ( "found", found, true );

            ok = JNITestutils.chcksi ( "cvals.length",
                                       cvals.length,
                                       "=",
                                       1,
                                       0                );

            ok = JNITestutils.chcksc ( "cvals[0], fetch[" + i + "]",
                                       cvals[0],
                                       "=",
                                       putCharacterArray[i] );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests putDouble and "    +
                              "getDouble.  A variable "           +
                              "associated with an array of doubles " +
                              "is loaded via putDouble. The "     +
                              "values are retrieved via "            +
                              "getDouble."                           );
         //
         // Populate the putDouble array with values.
         //
         putDoubleArray = new double[PDPOOL_DIM];

         for ( i = 0;  i < PDPOOL_DIM;  i++ )
         {
            putDoubleArray[i] = i;
         }


         //
         // Insert the variable into the kernel pool.
         //
         KernelPool.putDouble ( PDPOOL_VAR, putDoubleArray );

         //
         // Retrieve the variable's associated values.
         //
         dvals = KernelPool.getDouble ( PDPOOL_VAR );

         //
         // Check the results.
         //
         found = dvals.length > 0;

         ok = JNITestutils.chcksl ( "found", found, true );


         ok = JNITestutils.chcksi ( "dvals.length",
                                    dvals.length,
                                    "=",
                                    PDPOOL_DIM,
                                    0              );


         ok = JNITestutils.chckad ( "dvals",
                                    dvals,
                                    "=",
                                    putDoubleArray,
                                    0.0                );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Continue previous test using getDouble( " +
                              "name, start, room)."                          );


         for ( i = 0;  i < PDPOOL_DIM;  i++ )
         {
            //
            // Retrieve the variable's associated values.
            //
            dvals = KernelPool.getDouble( PDPOOL_VAR, i, 1 );

            //
            // Check the results.
            //
            found = (dvals.length > 0);

            ok = JNITestutils.chcksl ( "found", found, true );

            ok = JNITestutils.chcksi ( "dvals.length",
                                       dvals.length,
                                       "=",
                                       1,
                                       0                );

            ok = JNITestutils.chcksd ( "dvals[0], fetch[" + i + "]",
                                       dvals[0],
                                       "=",
                                       putDoubleArray[i],
                                       0.0                         );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests putInteger and "    +
                              "getInteger.  A variable "           +
                              "associated with an array of ints " +
                              "is loaded via putInteger. The "     +
                              "values are retrieved via "            +
                              "getInteger."                           );
         //
         // Populate the putInteger array with values.
         //
         putIntArray = new int[PIPOOL_DIM];

         for ( i = 0;  i < PIPOOL_DIM;  i++ )
         {
            putIntArray[i] = i;
         }


         //
         // Insert the variable into the kernel pool.
         //
         KernelPool.putInteger ( PIPOOL_VAR, putIntArray );

         //
         // Retrieve the variable's associated values.
         //
         ivals = KernelPool.getInteger ( PIPOOL_VAR );

         //
         // Check the results.
         //
         found = ivals.length > 0;

         ok = JNITestutils.chcksl ( "found", found, true );


         ok = JNITestutils.chcksi ( "ivals.length",
                                    ivals.length,
                                    "=",
                                    PIPOOL_DIM,
                                    0              );


         ok = JNITestutils.chckai ( "ivals",
                                    ivals,
                                    "=",
                                    putIntArray  );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Continue previous test using getInteger( " +
                              "name, start, room)."                          );


         for ( i = 0;  i < PIPOOL_DIM;  i++ )
         {
            //
            // Retrieve the variable's associated values.
            //
            ivals = KernelPool.getInteger( PIPOOL_VAR, i, 1 );

            //
            // Check the results.
            //
            found = (ivals.length > 0);

            ok = JNITestutils.chcksl ( "found", found, true );

            ok = JNITestutils.chcksi ( "ivals.length",
                                       ivals.length,
                                       "=",
                                       1,
                                       0                );

            ok = JNITestutils.chcksi ( "ivals[0], fetch[" + i + "]",
                                       ivals[0],
                                       "=",
                                       putIntArray[i],
                                       0                           );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "This case tests method `load'. " +
                              "We create a new LSK and " +
                              "load it via `load'.  Then we check " +
                              "that the expected kernel variables " +
                              "are present."                          );


         KernelPool.clear();

         //
         // Create a kernel file.  We must prepend a "\begindata" control
         // word to the kernel variable assignments in the lmpool_c text
         // buffer.
         //
         kerBuffer    = new String[NLINES+1];

         //
         // This declaration can be used to test handling
         // of null String array elements in the C utilities.
         //
         // kerBuffer    = new String[BUFDIM];
         //

         kerBuffer[0] = "\\begindata";

         for ( i = 0; i < NLINES; i ++ )
         {
            kerBuffer[i+1] = textbuf[i];
         }

         JNITestutils.tsttxt ( LSK, kerBuffer, false, false );

         KernelPool.load( LSK );

         for ( i = 0;  i < LMPOOL_NVARS;  i++ )
         {
            found = KernelPool.exists( lmpoolNames[i] );

            ok    = JNITestutils.chcksl ( "found", found, true );

            n     = KernelPool.getSize( lmpoolNames[i] );

            ok = JNITestutils.chcksi ( "size of lmpoolNames[" + i + "]",
                                       n,
                                       "=",
                                       lmpoolDims[i],
                                       0                           );
            //
            // Both the actual and expected values are automatically
            // promoted to ints.
            //
            dtype = KernelPool.getDataType( lmpoolNames[i] );

            ok = JNITestutils.chcksi ( "data type of lmpoolNames[" + i + "]",
                                       dtype,
                                       "=",
                                       NUMERIC,
                                       0                           );
         }


         //
         // Clean up the kernel.
         //
         ( new File( LSK ) ).delete();



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getParameter.  Make sure we can " +
                              "retrieve the number of kernel variable " +
                              "names the pool can hold."                   );

         n = KernelPool.getParameter( "MAXVAR" );

         ok = JNITestutils.chcksi ( "n",
                                    n,
                                    "=",
                                    26003,
                                    0               );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getStringComponent. Use " +
                              "loadFromBuffer to load into the kernel " +
                              "pool strings representing two very long " +
                              "SPK file names. Retrieve these names " +
                              "using getStringComponent."                   );

         KernelPool.clear();

         KernelPool.loadFromBuffer( spkbuf );

         files    = new String[SPK_BUFSIZE];

         KVComp   = KernelPool.getStringComponent( "SPK_FILES", 0, "*" );

         found    = KVComp.wasFound();

         ok = JNITestutils.chcksl( "found (0)", found, true );

         kvname   = KVComp.getKerVarName();

         ok = JNITestutils.chcksc( "kvname (0)", kvname, "=", "SPK_FILES" );

         i  = KVComp.getIndex();

         ok = JNITestutils.chcksi( "index (0)", i, "=", 0, 0 );

         files[0] = KVComp.getComponent();

         ok = JNITestutils.chcksc ( "files[0]",
                                    files[0],
                                    "=",
                                    SPK_FILE0   );



         KVComp   = KernelPool.getStringComponent( "SPK_FILES", 1, "*" );
         files[1] = KVComp.getComponent();

         found    = KVComp.wasFound();

         ok = JNITestutils.chcksl( "found (1)", found, true );

         kvname   = KVComp.getKerVarName();

         ok = JNITestutils.chcksc( "kvname (1)", kvname, "=", "SPK_FILES" );

         i  = KVComp.getIndex();

         ok = JNITestutils.chcksi( "index (1)", i, "=", 1, 0 );

         files[0] = KVComp.getComponent();

         ok = JNITestutils.chcksc ( "files[1]",
                                    files[1],
                                    "=",
                                    SPK_FILE1   );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getStringComponent on a blank string." );


         //
         // Check handling of a blank component.  The returned string
         // should contain a single blank followed by a null.
         //

         putCharacterArray    = new String[1];
         putCharacterArray[0] = "    ";

         KernelPool.putCharacter ( "BLANK_VAR", putCharacterArray );

         KVComp = KernelPool.getStringComponent( "BLANK_VAR", 0, "*" );
         cvals[0] = KVComp.getComponent();

         ok = JNITestutils.chcksc ( "cvals[0]",
                                    cvals[0],
                                    "=",
                                    " "        );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Clean up." );




      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         // For debugging:
         // ex.printStackTrace();

         ok = JNITestutils.chckth ( false, "", ex );
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

