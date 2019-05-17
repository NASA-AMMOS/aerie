
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestFOV provides methods that implement test families for
the class FOV.

<h3>Version 2.0.0 29-DEC-2016 (NJB)</h3>

Moved clean-up code to "finally" block.

<h3>Version 1.0.0 09-DEC-2009 (NJB)</h3>
*/
public class TestFOV extends Object
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
   Test FOV and associated classes.
   */
   public static boolean f_FOV()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-9;

      final int                         NLINES       = 9;
      final int                         INSTID       = -22100;
      final int                         LMPOOL_NVARS = 4;

      //
      // Local variables
      //
      boolean                           ok;

      int                               handle;
      int                               i;
      int                               xN;

      double[]                          xBoresight = { 0.0, 0.0, 1.0 };

      double[][]                        xBoundary  = {
                                                        {  1.0,  1.0, 1.0 },
                                                        {  1.0, -1.0, 1.0 },
                                                        { -1.0, -1.0, 1.0 },
                                                        { -1.0,  1.0, 1.0 }
                                                     };

      FOV                               fov;

      Instrument                        inst;
      Instrument                        xInst;

      ReferenceFrame                    frame;

      String                            Shape;

      String[]                          textbuf =  {
         "INS-22100_FOV_FRAME             = '22100-FRAME' ",
         "INS-22100_FOV_SHAPE             = 'RECTANGLE' ",
         "INS-22100_BORESIGHT             = ( 0.0, 0.0, 1.0 )",
         "INS-22100_FOV_BOUNDARY_CORNERS  = ( ",
         "                                     1.0,  1.0, 1.0, ",
         "                                     1.0, -1.0, 1.0, ",
         "                                    -1.0, -1.0, 1.0, ",
         "                                    -1.0,  1.0, 1.0, ",
         "                                                       )"
      };

      Vector3                           boresight;





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

         JNITestutils.topen ( "f_FOV" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: insert FOV description into " +
                              "kernel pool." );


         KernelPool.loadFromBuffer( textbuf );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: try to create FOV for which " +
                               "kernel data are unavailable."          );

         try
         {
            inst = new Instrument( -1500004 );

            fov  = new FOV( inst );


            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(FRAMEMISSING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(FRAMEMISSING)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create a FOV instance." );

         //
         //
         xInst = new Instrument( INSTID );

         fov   = new FOV( xInst );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check FOV shape." );

         ok = JNITestutils.chcksc ( "shape", fov.getShape(), "=", "RECTANGLE" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check FOV frame." );

         ok = JNITestutils.chcksc ( "frame",
                                    fov.getReferenceFrame().getName(),
                                    "=",
                                    "22100-FRAME"                     );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check FOV boresight." );

         ok = JNITestutils.chckad ( "boresight",
                                    fov.getBoresight().toArray(),
                                    "~~",
                                    xBoresight,
                                    TIGHT_TOL                     );
         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check FOV boundary." );


         //
         // Check the boundary dimension.
         //

         xN = 4;

         ok = JNITestutils.chcksi ( "boundary dimension",
                                    fov.getBoundary().length,
                                    "=",
                                    xN,
                                    0                        );


         //
         // Check the boundary.
         //

         for( i = 0;  i < xN;  i++ )
         {
            ok = JNITestutils.chckad( "boundary vector " + i,
                                      fov.getBoundary()[i].toArray(),
                                      "~~",
                                      xBoundary[i],
                                      TIGHT_TOL               );
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

         //
         // Just clear the kernel pool.
         //
         KernelDatabase.clear();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

