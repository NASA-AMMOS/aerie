
package spice.tspice;


import spice.basic.*;
import static spice.basic.DLA.*;
import static spice.basic.DLADescriptor.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestDLADescriptor provides methods that implement test families for
class DLADescriptor

<p>Version 1.0.0 15-NOV-2016 (NJB)

*/
public class TestDLADescriptor extends Object
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
   Test class DLADescriptor APIs.
   */
   public static boolean f_DLADescriptor()

      throws SpiceException
   {
      //
      // Constants
      //
 
      //
      // Local variables
      //
      DLADescriptor                     dladsc;
      DLADescriptor                     dladsc2;
 
      boolean                           ok;
 
      int                               i;
      int[]                             iArray;
      int                               xival;


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

         JNITestutils.topen ( "f_DLADescriptor" );




         // ***********************************************************
         //
         //      Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Construct DLADescriptor from array." );

         //
         // Create integer array. The values of the array must be
         // distinct but need not be realistic.
         //
         iArray = new int[ DLADSZ ];

         for ( i = 0;  i < DLADSZ;  i++ )
         {
            iArray[i] = i+1;
         }

         dladsc = new DLADescriptor( iArray );

         //
         // Below we'll test the DLADescriptor accessor functions.
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch backward pointer." );

         int bwdptr = dladsc.getBackwardPointer();

         xival = 1;

         ok = JNITestutils.chcksi( "bwdptr", bwdptr, "=", xival, 0 );
 
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch forward pointer." );

         int fwdptr = dladsc.getForwardPointer();

         xival = 2;

         ok = JNITestutils.chcksi( "fwdptr", fwdptr, "=", xival, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch integer base address." );

         int ibase = dladsc.getIntBase();

         xival = 3;

         ok = JNITestutils.chcksi( "ibase", ibase, "=", xival, 0 );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch integer component size." );

         int isize = dladsc.getIntSize();

         xival = 4;

         ok = JNITestutils.chcksi( "isize", isize, "=", xival, 0 );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch double precision base address." );

         int dbase = dladsc.getDoubleBase();

         xival = 5;

         ok = JNITestutils.chcksi( "dbase", dbase, "=", xival, 0 );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch double precision component size." );

         int dsize = dladsc.getDoubleSize();

         xival = 6;

         ok = JNITestutils.chcksi( "dsize", dsize, "=", xival, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch character base address." );

         int cbase = dladsc.getCharBase();

         xival = 7;

         ok = JNITestutils.chcksi( "cbase", cbase, "=", xival, 0 );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch character component size." );

         int csize = dladsc.getCharSize();

         xival = 8;

         ok = JNITestutils.chcksi( "csize", csize, "=", xival, 0 );

       
         //
         // Test the toArray method.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch descriptor contents into int array." );

         int[] intArray2 = dladsc.toArray();

         ok = JNITestutils.chckai( "intArray2", intArray2, "=", iArray );


         //
         // Test the other constructors.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Call no-args constructor." );

         dladsc = new DLADescriptor();

         int[] intArray3 = new int[ DLADSZ ];

         ok = JNITestutils.chckai( "no-args array", dladsc.toArray(), 
                                   "=",             intArray3         );
         

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Call copy constructor." );

         dladsc = new DLADescriptor( iArray );

         dladsc2 = new DLADescriptor( dladsc );

         ok = JNITestutils.chckai( "copy's array", dladsc2.toArray(), 
                                   "=",            dladsc.toArray() );

         //
         // Verify that we can modify dladsc without changing dladsc2.
         //
         // Save the contents of dladsc2.
         //
         intArray2 = dladsc2.toArray();

         //
         // Update dladsc.
         //
         iArray[0] = -1;

         dladsc = new DLADescriptor( iArray );  

         //
         // Compare the current contents of dladsc2 against this
         // object's previously saved contents.
         //
         ok = JNITestutils.chckai( "dladsc2 array", dladsc2.toArray(), 
                                   "=",             intArray2         );

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

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

