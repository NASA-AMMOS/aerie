
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestFrameInfo provides methods that implement test families for
the class FrameInfo.

<p>Version 1.0.0 09-DEC-2009 (NJB)
*/
public class TestFrameInfo extends Object
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
   Test FrameInfo and associated classes.
   */
   public static boolean f_FrameInfo()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      VTIGHT_TOL = 1.e-14;

      //
      // Local variables
      //
      boolean                           ok;

      double                            xval;

      int                               frameCenter;
      int                               frameClass;
      int                               frameClassID;
      int                               frameID;
      int                               xFrameCenter;
      int                               xFrameClass;
      int                               xFrameClassID;
      int                               xFrameID;

      FrameInfo                         info0;
      FrameInfo                         info1;
      FrameInfo                         info2;

      ReferenceFrame                    ref0;
      ReferenceFrame                    ref1;

      String                            name;
      String                            xName;



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

         JNITestutils.topen ( "f_FrameInfo" );







         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Pass unknown frame to frame constructor." );

         try
         {
            ref0  = new ReferenceFrame( "xyz" );

            info0 = new FrameInfo( ref0 );

            /*
            If an exception is *not* thrown, we'll hit this call.
            */
            Testutils.dogDidNotBark ( "SPICE(FRAMENOTFOUND)" );


         }
         catch( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(FRAMENOTFOUND)", ex );
         }



         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check ID constructor and `get' methods" );

         //
         //
         //
         ref0  = new ReferenceFrame( "IAU_EARTH" );

         info0 = new FrameInfo( ref0.getIDCode() );

         ok = JNITestutils.chcksi( "IAU_EARTH ID code",
                                   info0.getFrameID(),
                                   "=",
                                   10013,
                                   0                 );

         ok = JNITestutils.chcksi( "IAU_EARTH class ID code",
                                   info0.getFrameClassID(),
                                   "=",
                                   399,
                                   0                    );

         ok = JNITestutils.chcksi( "IAU_EARTH class",
                                   info0.getFrameClass(),
                                   "=",
                                   2,
                                   0                    );

         ok = JNITestutils.chcksi( "IAU_EARTH center ID",
                                   info0.getFrameCenterID(),
                                   "=",
                                   399,
                                   0                    );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check ID constructor and `get' methods, cont." );

         ref1  = new ReferenceFrame( "ITRF93" );

         info1 = new FrameInfo( ref1.getIDCode() );

         ok = JNITestutils.chcksi( "ITRF93 ID code",
                                   info1.getFrameID(),
                                   "=",
                                   13000,
                                   0                 );

         ok = JNITestutils.chcksi( "ITRF93 class ID code",
                                   info1.getFrameClassID(),
                                   "=",
                                   3000,
                                   0                    );

         ok = JNITestutils.chcksi( "ITRF93 class",
                                   info1.getFrameClass(),
                                   "=",
                                   2,
                                   0                    );

         ok = JNITestutils.chcksi( "ITRF93 center ID",
                                   info1.getFrameCenterID(),
                                   "=",
                                   399,
                                   0                    );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check frame constructor and `get' methods" );

         //
         //
         //
         ref0  = new ReferenceFrame( "IAU_EARTH" );

         info0 = new FrameInfo( ref0 );

         ok = JNITestutils.chcksi( "IAU_EARTH ID code",
                                   info0.getFrameID(),
                                   "=",
                                   10013,
                                   0                 );

         ok = JNITestutils.chcksi( "IAU_EARTH class ID code",
                                   info0.getFrameClassID(),
                                   "=",
                                   399,
                                   0                    );

         ok = JNITestutils.chcksi( "IAU_EARTH class",
                                   info0.getFrameClass(),
                                   "=",
                                   2,
                                   0                    );

         ok = JNITestutils.chcksi( "IAU_EARTH center ID",
                                   info0.getFrameCenterID(),
                                   "=",
                                   399,
                                   0                    );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check frame constructor and `get' methods, " +
                              "cont." );

         ref1  = new ReferenceFrame( "ITRF93" );

         info1 = new FrameInfo( ref1 );

         ok = JNITestutils.chcksi( "ITRF93 ID code",
                                   info1.getFrameID(),
                                   "=",
                                   13000,
                                   0                 );

         ok = JNITestutils.chcksi( "ITRF93 class ID code",
                                   info1.getFrameClassID(),
                                   "=",
                                   3000,
                                   0                    );

         ok = JNITestutils.chcksi( "ITRF93 class",
                                   info1.getFrameClass(),
                                   "=",
                                   2,
                                   0                    );

         ok = JNITestutils.chcksi( "ITRF93 center ID",
                                   info1.getFrameCenterID(),
                                   "=",
                                   399,
                                   0                    );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check copy constructor." );


         ref0 = new ReferenceFrame( "IAU_Earth" );

         info0 = new FrameInfo( ref0.getIDCode() );
         info1 = new FrameInfo( ref0.getIDCode() );

         info2 = new FrameInfo( info0 );

         //
         // Change inf0; make sure info2 doesn't change.
         //
         info0 = new FrameInfo(
                              new ReferenceFrame( "ECLIPJ2000" ).getIDCode() );


         ok = JNITestutils.chcksi( "IAU_EARTH ID code",
                                   info2.getFrameID(),
                                   "=",
                                   10013,
                                   0                 );

         ok = JNITestutils.chcksi( "IAU_EARTH class ID code",
                                   info2.getFrameClassID(),
                                   "=",
                                   399,
                                   0                    );

         ok = JNITestutils.chcksi( "IAU_EARTH class",
                                   info2.getFrameClass(),
                                   "=",
                                   2,
                                   0                    );

         ok = JNITestutils.chcksi( "IAU_EARTH center ID",
                                   info2.getFrameCenterID(),
                                   "=",
                                   399,
                                   0                    );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check toString." );

         String endl = System.getProperty( "line.separator" );

         String outStr;
         String xStr;

         info0  = new FrameInfo( new ReferenceFrame("ITRF93") );

         outStr = info0.toString();

         xStr   = "Frame ID:          13000" + endl +
                  "Frame center ID:   399"   + endl +
                  "Frame class:       2"     + endl +
                  "Frame class ID:    3000"  + endl;


         // For debugging:
         // System.out.println( outStr );
         // System.out.println( xStr   );


         ok = JNITestutils.chcksc( "ITRF93 info string",
                                   outStr,
                                   "=",
                                   xStr                 );

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

