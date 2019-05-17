
package spice.tspice;


import java.io.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestDAS provides methods that implement test families for
class DAS.

<h3>Version 1.0.0 28-DEC-2016 (NJB)</h3>

*/
public class TestDAS extends Object
{

 
   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test class DAS APIs.
   */
   public static boolean f_DAS()

      throws SpiceException
   {
      //
      // Constants
      //

      final String                      DAS0             =    "das_test0.bds";
      final String                      DAS1             =    "das_test1.bds";

      final double                      TIGHT            =     1.e-12;
      final double                      VTIGHT           =     1.e-14;
 
      final int                         MAXBUF           = 200;
      final int                         LINSIZ           = 80;


      //
      // Local variables
      //
      Body[]                            bodies;
      Body                              body;

      DAS                               das;
      DAS                               das0  =  null;
      DAS                               das1;

      String                            bodnam;
      String[]                          combuf2;
      String[]                          comments;
      String                            fileName;
      String                            frame;
      String                            internalFileName;

      boolean                           readable;
      boolean                           writable;
      boolean                           ok;
       
      int                               bodyid;
      int                               handle;
      int                               i;
      int                               j;
      int                               n;
      int                               ncomc;
      int                               ncomr;
      int                               nresvc;
      int                               nresvr;
      int                               surfid0;
      int                               surfid1;
      int                               xncomc;
      int                               xncomr;


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

         JNITestutils.topen ( "f_DAS" );




         // ***********************************************************
         //
         //      Normal cases
         //
         // ***********************************************************


         //
         // Test constructors.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Call no-arguments constructor." );

         das = new DAS();

         //
         // All we expect to be set in this object are false values
         // for the "readable" and "writable" flags.
         //
         ok = JNITestutils.chcksl( "readable", das.isReadable(), false );
         ok = JNITestutils.chcksl( "writable", das.isWritable(), false );


         //
         // --------Case-----------------------------------------------
         //
         //  The standard constructor DAS( String ) is tested indirectly
         //  by the test family TestDASProt.
         // 
         //
 
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Create DAS files for testing." );

         //
         // Delete the DAS files if they exist.
         //

         ( new File ( DAS0 ) ).delete();
         ( new File ( DAS1 ) ).delete();

         // 
         // Create a simple DSK file; this is an instance of a DAS file.
         //
         bodyid  = 499;
         surfid0 = 1;
         frame   = "IAU_MARS";

         JNITestutils.t_smldsk( bodyid, surfid0, frame, DAS0 );
 
         surfid1 = 2;
         JNITestutils.t_smldsk( bodyid, surfid1, frame, DAS1 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor: check fields of copy." );

         das0 = DAS.openForRead( DAS0 );

         das1 = new DAS( das0 );

         ok = JNITestutils.chcksc( "fileName", das1.getFileName(), "=",
                                               das0.getFileName()        );


         ok = JNITestutils.chcksi( "handle",   das1.getHandle(), "=",
                                               das0.getHandle(),  0      );

         ok = JNITestutils.chcksl( "isReadable", das1.isReadable(), 
                                                 das0.isReadable()  );

         ok = JNITestutils.chcksl( "isWritable", das1.isWritable(), 
                                                 das0.isWritable()  );

         ok = JNITestutils.chcksc( "internalFileName", 
                                             das1.getInternalFileName(), "=",
                                             das0.getInternalFileName()      );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test copy constructor: change original DAS " +
                              "instance; make sure copy is unchanged." );

         //
         // Capture field values of copy.
         //
         handle           = das1.getHandle();
         fileName         = das1.getFileName();
         internalFileName = das1.getInternalFileName();
         readable         = das1.isReadable();
         writable         = das1.isWritable();


         //
         // Re-assign the value of das0.
         //
         das0 = DAS.openForWrite( DAS1 );

         //
         // Check das1.
         //
         ok = JNITestutils.chcksc( "fileName", das1.getFileName(), "=",
                                               fileName                 );


         ok = JNITestutils.chcksi( "handle",   das1.getHandle(), "=",
                                               handle,  0      );

         ok = JNITestutils.chcksl( "isReadable", das1.isReadable(), 
                                                 readable         );

         ok = JNITestutils.chcksl( "isWritable", das1.isWritable(), 
                                                 writable         );

         ok = JNITestutils.chcksc( "internalFileName", 
                                             internalFileName, "=",
                                             internalFileName        );

         //
         // Test close() on files open for read and write.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Close readable DAS file associated with das0." );
         
         das0.close();

         ok = JNITestutils.chcksl( "isReadable", das0.isReadable(), 
                                                 false         );

         ok = JNITestutils.chcksl( "isWritable", das0.isWritable(), 
                                                 false         );
         

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Close writable DAS file associated with das1." );
         
         das1.close();

         ok = JNITestutils.chcksl( "isReadable", das1.isReadable(), 
                                                 false         );

         ok = JNITestutils.chcksl( "isWritable", das1.isWritable(), 
                                                 false         );
         



         //
         // Test comment area access methods.
         //
 
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Add comments to DAS0." );
         
         //
         // Create buffer of comments.
         //
         comments = new String[ MAXBUF ];

         for ( i = 0;  i < MAXBUF;  i++ )
         {
            comments[i] = String.format( 

                "This is line %3d of the text buffer----"   +
                "--------------------------------------->",
                i                                            );

            //System.out.println( comments[i] );
         }       

         das0 = DAS.openForWrite( DAS0 );

         //
         // Check the number of comment characters already in the file.
         // We're not expecting any. Check comment records too.
         //
         ncomc = das0.getCommentCharacterCount();
         ncomr = das0.getCommentRecordCount();

         ok = JNITestutils.chcksi( "ncomc", ncomc, "=", 0, 0 );
         ok = JNITestutils.chcksi( "ncomr", ncomr, "=", 0, 0 );

         //
         // Add comments.
         //
         das0.addComments ( comments );

         //
         // Close the file; re-open it for read access.
         //
         das0.close();

         das0 = DAS.openForRead( DAS0 );

         //
         // Now check the comment counts.
         //
         xncomc = LINSIZ * MAXBUF;
         ncomc  = das0.getCommentCharacterCount();

         ok = JNITestutils.chcksi( "ncomc after addition", ncomc, 
                                   "=",                    xncomc, 0 );

         //
         // Comment records contain 1024 characters. Comment lines
         // are stored with terminating nulls, so we have one extra
         // character per line.
         //
         xncomr = ( ( MAXBUF + ncomc - 1 ) / 1024 ) + 1;

         ncomr  = das0.getCommentRecordCount();

         ok = JNITestutils.chcksi( "ncomr after addition", ncomr, 
                                   "=",                    xncomr, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Read comments from DAS0." );
         
         combuf2 = das0.readComments( LINSIZ );

         for ( i = 0;  i < MAXBUF;  i++ )
         {
            String label = String.format( "combuf2[%d]", i );

            JNITestutils.chcksc( label, combuf2[i], "=", comments[i] );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Delete comments from DAS0." );

         //
         // We'll need to open the file for write access in order to
         // modify the comment area.
         //
         das0.close();

         das0 = DAS.openForWrite( DAS0 );

         das0.deleteComments();

         //
         // Check the number of comment characters already in the file.
         // We're not expecting any. Check comment records too.
         //
         ncomc = das0.getCommentCharacterCount();
         ncomr = das0.getCommentRecordCount();

         ok = JNITestutils.chcksi( "ncomc", ncomc, "=", 0, 0 );
         ok = JNITestutils.chcksi( "ncomr", ncomr, "=", 0, 0 );



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

         //********************************************************************
         //
         // Clean up.
         //
         //********************************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Clean up: unload and delete DAS files." );

         //
         // Unload all kernels.
         //

         das0.close();

         CSPICE.kclear();

         //
         // Delete the DAS files if they exist.
         //

         ( new File ( DAS0 ) ).delete();
         ( new File ( DAS1 ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

