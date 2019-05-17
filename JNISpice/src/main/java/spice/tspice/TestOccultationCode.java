
package spice.tspice;


import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.OccultationCode.*;


/**
Class TestOccultationCode provides methods that implement test families for
the enum OccultationCode.

<h3>Version 1.0.0 30-DEC-2016 (NJB)</h3>
*/
public class TestOccultationCode extends Object
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
   Test OccultationCode methods.
   */
   public static boolean f_OccultationCode()

      throws SpiceException
   {
      //
      // Constants
      //
       

      //
      // Local variables
      //
      OccultationCode                    occCode;
      OccultationCode                    xOccCode;

      boolean                            ok;

      int                                code;   
      int                                xCode;

 
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

         JNITestutils.topen ( "f_OccultationCode" );



         // ***********************************************************
         //
         //   Normal cases for getOccultationCode
         //
         // ***********************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain code for enum TOTAL1." );

         code  = TOTAL1.getOccultationCode();

         xCode = -3;

         JNITestutils.chcksi( "code", code, "=", xCode, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain code for enum ANNLR1." );

         code  = ANNLR1.getOccultationCode();

         xCode = -2;

         JNITestutils.chcksi( "code", code, "=", xCode, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain code for enum PARTL1." );

         code  = PARTL1.getOccultationCode();

         xCode = -1;

         JNITestutils.chcksi( "code", code, "=", xCode, 0 );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain code for enum TOTAL2." );

         code  = TOTAL2.getOccultationCode();

         xCode = 3;

         JNITestutils.chcksi( "code", code, "=", xCode, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain code for enum ANNLR2." );

         code  = ANNLR2.getOccultationCode();

         xCode = 2;

         JNITestutils.chcksi( "code", code, "=", xCode, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain code for enum PARTL2." );

         code  = PARTL2.getOccultationCode();

         xCode = 1;

         JNITestutils.chcksi( "code", code, "=", xCode, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain code for enum NOOCC." );

         code  = NOOCC.getOccultationCode();

         xCode = 0;

         JNITestutils.chcksi( "code", code, "=", xCode, 0 );


         // ***********************************************************
         //
         //   Normal cases for mapIntCode
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain enum for code -3." );


         occCode  = OccultationCode.mapIntCode( -3 );

         xOccCode = TOTAL1;

         JNITestutils.chcksi( "code", occCode.getOccultationCode(), 
                              "=",    xOccCode.getOccultationCode(), 0 );
 

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain enum for code -2." );


         occCode  = OccultationCode.mapIntCode( -2 );

         xOccCode = ANNLR1;

         JNITestutils.chcksi( "code", occCode.getOccultationCode(), 
                              "=",    xOccCode.getOccultationCode(), 0 );
 

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain enum for code -1." );


         occCode  = OccultationCode.mapIntCode( -1 );

         xOccCode = PARTL1;

         JNITestutils.chcksi( "code", occCode.getOccultationCode(), 
                              "=",    xOccCode.getOccultationCode(), 0 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain enum for code  3." );


         occCode  = OccultationCode.mapIntCode( 3 );

         xOccCode = TOTAL2;

         JNITestutils.chcksi( "code", occCode.getOccultationCode(), 
                              "=",    xOccCode.getOccultationCode(), 0 );
 

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain enum for code 2." );


         occCode  = OccultationCode.mapIntCode( 2 );

         xOccCode = ANNLR2;

         JNITestutils.chcksi( "code", occCode.getOccultationCode(), 
                              "=",    xOccCode.getOccultationCode(), 0 );
 

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain enum for code 1." );


         occCode  = OccultationCode.mapIntCode( 1 );

         xOccCode = PARTL2;

         JNITestutils.chcksi( "code", occCode.getOccultationCode(), 
                              "=",    xOccCode.getOccultationCode(), 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Obtain enum for code 0." );


         occCode  = OccultationCode.mapIntCode( 0 );

         xOccCode = NOOCC;

         JNITestutils.chcksi( "code", occCode.getOccultationCode(), 
                              "=",    xOccCode.getOccultationCode(), 0 );





         // ***********************************************************
         //
         //   Error cases for mapIntCode
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Error: try to obtain enum for code -4." );


         try
         {
            occCode = OccultationCode.mapIntCode( -4 );

            Testutils.dogDidNotBark ( "SPICE(INVALIDCODE)" );
         }
         catch ( SpiceException exc )
         {
            JNITestutils.chckth ( true, "SPICE(INVALIDCODE)", exc );

            //exc.printStackTrace();
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Error: try to obtain enum for code 4." );


         try
         {
            occCode = OccultationCode.mapIntCode( 4 );

            Testutils.dogDidNotBark ( "SPICE(INVALIDCODE)" );
         }
         catch ( SpiceException exc )
         {
            JNITestutils.chckth ( true, "SPICE(INVALIDCODE)", exc );

            //exc.printStackTrace();
         }

 
      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         ok = JNITestutils.chckth ( false, "", ex );
      }

      finally
      {
         //
         // --------Case-----------------------------------------------
         //
 
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

