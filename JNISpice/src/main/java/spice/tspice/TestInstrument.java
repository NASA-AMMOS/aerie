
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestInstrument provides methods that implement test families for
the class Instrument.

<h3>Version 2.0.0 28-DEC-2016 (NJB)</h3>

Moved clean-up to finally block.

<h3>Version 1.0.0 07-DEC-2009 (NJB)</h3>
*/
public class TestInstrument extends Object
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
   Test Instrument and associated classes.
   */
   public static boolean f_Instrument()

      throws SpiceException
   {
      //
      // Constants
      //

      //
      // We need a PCK and SPK to support creation of the IK.
      // The PCK and SPK files are not used directly.
      //
      final String                      IK        = "nat.ti";
      final String                      PCK       = "nat.tpc";
      final String                      SPK       = "nat.bsp";

      final double                      TIGHT_TOL = 1.e-12;

      //
      // Local variables
      //
      Instrument                        i0;
      Instrument                        i1;
      Instrument                        i2;

      String                            name0;
      String                            name1;

      Vector3                           v0;

      boolean                           ok;

      int                               handle = 0;
      int                               i;


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

         JNITestutils.topen ( "f_Instrument" );



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
         // We need a PCK and SPK to support creation of the IK.
         // The PCK and SPK files are not used directly.
         //


         //
         // Delete PCK if it exists. Create and load a PCK file.
         //
         ( new File ( PCK ) ).delete();

         JNITestutils.natpck( PCK, true, true );

         //
         // Delete SPK if it exists. Create and load an SPK file.
         //
         ( new File ( SPK ) ).delete();

         handle = JNITestutils.natspk( SPK, true );


         //
         // Delete IK if it exists. Create and load an IK file.
         //
         ( new File ( IK ) ).delete();

         JNITestutils.natik( IK, SPK, PCK, true, false );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create Instrument using blank " +
                               "instrument name." );

         try
         {
            i0 = new Instrument( " " );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(BLANKSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(BLANKSTRING)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create Instrument using " +
                               "empty instrument name." );

         try
         {
            i0 = new Instrument( "" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }





         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create Instrument from name." );

         //      i0    = new Instrument( "Alpha_rectangle_none" );
         i0    = new Instrument( "ALPHA_RECTANGLE_NONE" );

         name0 = i0.getName();


         ok = JNITestutils.chcksc ( "name",
                                    name0,
                                    "=",
                                    "ALPHA_RECTANGLE_NONE" );


         ok = JNITestutils.chcksi ( "ID",
                                    i0.getIDCode(),
                                    "=",
                                    -1500003,
                                    0               );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create Instrument from ID code." );

         i1    = new Instrument( -1500003 );

         name1 = i1.getName();


         ok = JNITestutils.chcksc ( "name",
                                    name1,
                                    "=",
                                    "ALPHA_RECTANGLE_NONE" );


         ok = JNITestutils.chcksi ( "ID",
                                    i1.getIDCode(),
                                    "=",
                                    -1500003,
                                    0               );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor" );

         i0    = new Instrument( -1500001 );
         i2    = new Instrument( -1500001 );

         i1    = new Instrument( i0 );

         //
         // Make sure that changing i0 doesn't affect i1.
         //
         i0    = new Instrument( 399 );

         name1 = i1.getName();


         ok = JNITestutils.chcksc ( "name",
                                    name1,
                                    "=",
                                    "ALPHA_CIRCLE_NONE" );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test equality operator" );

         i0    = new Instrument( -1500001      );
         i1    = new Instrument( "alphA_CiRcLe_noNe" );
         i2    = new Instrument( "earth"  );

         //
         // Make sure that i0 and i1 are equal.
         //
         ok = JNITestutils.chcksl ( "i0 == i1",
                                    i0.equals( i1 ),
                                    true             );

         //
         // Make sure that i0 and i2 are not equal.
         //
         ok = JNITestutils.chcksl ( "i0 == i2",
                                    i0.equals( i2 ),
                                    false            );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString." );

         i0    = new Instrument( -1500004   );

         ok = JNITestutils.chcksc ( "string form of -1500004",
                                    i0.toString(),
                                    "=",
                                    "ALPHA_DIAMOND_NONE"             );




      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         //ex.printStackTrace();

         ok = JNITestutils.chckth ( false, "", ex );
      }

      finally
      {
         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Clean up." );


         //
         // Get rid of the PCK file.
         //
         ( new File ( PCK    ) ).delete();

         //
         // Get rid of the SPK file.
         //
         CSPICE.spkuef( handle );

         ( new File ( SPK ) ).delete();

         //
         // Get rid of the IK.
         //
         ( new File ( IK ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

