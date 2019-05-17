
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestAberrationCorrection provides methods that implement test families for
the class AberrationCorrection.

<p>Version 1.0.0 10-DEC-2009 (NJB)
*/
public class TestAberrationCorrection extends Object
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
   Test AberrationCorrection and associated classes.
   */
   public static boolean f_AberrationCorrection()

      throws SpiceException
   {
      //
      // Constants
      //

      //
      // Local variables
      //
      boolean                           ok;

      //
      // Attributes for corrections
      //
      //    NONE
      //    LT
      //    LTS
      //    CN
      //    CN+S
      //    XLT
      //    XLTS
      //    XCN
      //    XCN+S
      //    S
      //    XS
      //
      // The attribute set is
      //
      //    has light time
      //    has stellar aberration
      //    is converged Newtonian
      //    is receptionType
      //
      boolean[][]                       attributes = {

         { false, false, false, false },
         { true,  false, false, true  },
         { true,  true,  false, true  },
         { true,  false, true,  true  },
         { true,  true,  true,  true  },
         { true,  false, false, false },
         { true,  true,  false, false },
         { true,  false, true,  false },
         { true,  true,  true,  false },
         { false, true,  false, true  },
         { false, true,  false, false }              };


      int                               i;
      int                               j;

      AberrationCorrection              abcorr0;
      AberrationCorrection              abcorr1;
      AberrationCorrection              abcorr2;

      String[]                          abcorrNames = {

                                           "NONE",
                                           "LT",
                                           "LT+S",
                                           "CN",
                                           "CN+S",
                                           "XLT",
                                           "XLT+S",
                                           "XCN",
                                           "XCN+S",
                                           "S",
                                           "XS"       };

      String[]                          lowerCaseNames = {

                                           "none",
                                           "lt",
                                           "lt+s",
                                           "cn",
                                           "cn+s",
                                           "xlt",
                                           "xlt+s",
                                           "xcn",
                                           "xcn+s",
                                           "s",
                                           "xs"       };


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

         JNITestutils.topen ( "f_AberrationCorrection" );







         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Pass unknown correction to constructor." );

         try
         {
            abcorr0  = new AberrationCorrection( "xyz" );

            /*
            If an exception is *not* thrown, we'll hit this call.
            */
            Testutils.dogDidNotBark ( "SPICE(NOTSUPPORTED)" );


         }
         catch( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(NOTSUPPORTED)", ex );
         }



         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check name constructor and `get' methods. " +
                              "Loop over the set of corrections."           );

         for ( i = 0;  i < abcorrNames.length;  i++ )
         {
            abcorr0 = new AberrationCorrection( abcorrNames[i] );

            //System.out.println( abcorr0 );

            //
            // Check getName().
            //
            ok = JNITestutils.chcksc( "name of correction " + i,
                                      abcorr0.getName(),
                                      "=",
                                      abcorrNames[i]            );


            //
            // Check hasLightTime().
            //

            //System.out.println( abcorr0.hasLightTime() );

            ok = JNITestutils.chcksl( "'has light time' of correction " + i,
                                      abcorr0.hasLightTime(),
                                      attributes[i][0]                      );

            //
            // Check hasStellarAberration().
            //

            //System.out.println( abcorr0.hasStellarAberration() );

            ok = JNITestutils.chcksl(
                                 "'has stellar aberration' of correction " + i,
                                      abcorr0.hasStellarAberration(),
                                      attributes[i][1]                      );

            //
            // Check isConvergedNewtonian().
            //

            //System.out.println( abcorr0.isConvergedNewtonian() );

            ok = JNITestutils.chcksl(
                                 "'is converged newtonian' of correction " + i,
                                      abcorr0.isConvergedNewtonian(),
                                      attributes[i][2]                      );

            //
            // Check isReceptionType().
            //

            //System.out.println( abcorr0.isReceptionType() );

            ok = JNITestutils.chcksl( "'is reception type' of correction " + i,
                                      abcorr0.isReceptionType(),
                                      attributes[i][3]                      );



         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check toString." );

         for ( i = 0;  i < abcorrNames.length;  i++ )
         {
            abcorr0 = new AberrationCorrection( abcorrNames[i] );

            //System.out.println( abcorr0 );

            //
            // Check toString().
            //
            ok = JNITestutils.chcksc( "name of correction " + i,
                                      abcorr0.toString(),
                                      "=",
                                      abcorr0.getName()          );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check equals." );

         for ( i = 0;  i < abcorrNames.length;  i++ )
         {
            abcorr0 = new AberrationCorrection( abcorrNames[i] );

            for ( j = 0;  j < abcorrNames.length;  j++ )
            {
                abcorr1 = new AberrationCorrection( lowerCaseNames[j] );

                //
                // We expect equality iff i == j.
                //
                ok = JNITestutils.chcksl( "equals case(" + i + "," + j + ")",
                                          abcorr0.equals( abcorr1 ),
                                          ( i == j )                        );

            }
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check hashcode." );


         //
         // Make sure hash codes are equal for equal aberration corrections.
         //
         for ( i = 0;  i < abcorrNames.length;  i++ )
         {
            abcorr0 = new AberrationCorrection( abcorrNames[i] );

            for ( j = 0;  j < abcorrNames.length;  j++ )
            {
                abcorr1 = new AberrationCorrection( lowerCaseNames[j] );

                //
                // We expect equality if i == j. There is no assumption
                // about what happens when i != j.
                //

                if ( i == j )
                {
                   ok = JNITestutils.chcksl(
                                   "hash code case(" + i + "," + j + ")",
                                   abcorr0.hashCode() == abcorr1.hashCode(),
                                   true                                     );
                }
            }
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

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

