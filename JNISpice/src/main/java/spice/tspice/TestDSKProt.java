
package spice.tspice;

import java.io.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestDSKProt provides methods that test protected
methods in class DSK.

<p>Version 1.0.0 29-DEC-2016 (NJB)

*/
public class TestDSKProt extends DSK
{
   //
   // This test class extends DSK so that this class can
   // access protected members of the DSK class.
   //

  

   //
   // Class variables
   //

   //
   // Constructors
   //

   //
   // This constructor is used to obtain access to the 
   // constructor
   // 
   //     DSK( String )
   //
   // of the DSK class. Using this constructor allows this
   // test class to be located in the package spice.tspice
   // and still access the DSK constructor.
   //
   public TestDSKProt( String fname )

      throws SpiceException
   {
      super(fname);
   }


   //
   // Methods
   //

   /**
   Test protected class DSK APIs.
   */
   public static boolean f_DSKProt()

      throws SpiceException
   {

      //
      // Constants
      //

      final String                      DSK0             =    "dsk_test0.bds";
  
      //
      // Local variables
      //

      DSK                               dsk;
 
      String                            frame;
  
      boolean                           ok;
  
      int                               bodyid;
      int                               surfid;

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

         JNITestutils.topen ( "f_DSKProt" );




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
         JNITestutils.tcase ( "Call standard constructor; create DSK " +
                              "instance from a file name."              );

         //
         // Make sure the file we're about to create doesn't already exist.
         // Since we append to the file, this is especially important.
         //
         ( new File ( DSK0 ) ).delete();

         //
         // Make a trivial DSK file.
         //
         bodyid = 499;
         surfid = 1;
         frame  = "IAU_MARS";

         JNITestutils.t_smldsk ( bodyid, surfid, frame, DSK0 );

         //
         // This constructor call exercises spice.basic.DSK( String).
         //
         dsk = new TestDSKProt ( DSK0 );

         
         ok = JNITestutils.chcksc( "fileName", dsk.getFileName(), "=", DSK0  );
         ok = JNITestutils.chcksl( "readable", dsk.isReadable(),  false );
         ok = JNITestutils.chcksl( "writable", dsk.isWritable(),  false );
         
         try
         {
            ok = JNITestutils.chcksi( "handle",   dsk.getHandle(),   "=",  
                                       0,         0                        );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth( true, "SPICE(DASFILECLOSED)", ex );
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

         //********************************************************************
         //
         // Clean up.
         //
         //********************************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Clean up: unload and delete DSK files." );

         //
         // Unload all kernels.
         //
         CSPICE.kclear();


         //
         // Delete the DSKs if they exist.
         //

         ( new File ( DSK0 ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

