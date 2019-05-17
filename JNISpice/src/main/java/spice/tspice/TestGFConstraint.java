
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.GFConstraint.*;

/**
Class TestGFConstraint provides methods that implement test families for
the class GFConstraint.

<p>Version 1.0.0 21-DEC-2009 (NJB)
*/
public class TestGFConstraint extends Object
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
   Test GFConstraint and associated classes.
   */
   public static boolean f_GFConstraint()

      throws SpiceException
   {
      //
      // Constants
      //

      //
      // Local variables
      //
      GFConstraint                      cons0;
      GFConstraint                      cons1;
      GFConstraint                      cons2;

      String                            str0;
      String                            str1;
      String                            str2;

      boolean                           ok;

      double                            value;
      double                            xval;

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

         JNITestutils.topen ( "f_GFConstraint" );



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create reference constraint " +
                               "using invalid operator."               );

         try
         {
            cons0 = createReferenceConstraint( "==", 0.0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(NOTAPPLICABLE)" );

         }
         catch ( SpiceException ex )
         {
            //
            // For debugging
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,   "SPICE(NOTAPPLICABLE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create extremum constraint " +
                               "using invalid operator."               );

         try
         {
            cons0 = createExtremumConstraint( "absolute maximum" );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(NOTAPPLICABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(NOTAPPLICABLE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create adjusted extremum constraint " +
                               "using invalid operator."               );

         try
         {
            cons0 = createExtremumConstraint( "adjusted absolute maximum",
                                                                       10.0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(NOTAPPLICABLE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(NOTAPPLICABLE)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create adjusted extremum constraint " +
                               "using negative adjustment value."             );

         try
         {
            cons0 = createExtremumConstraint( "ADJ_ABSMAX", -10.0  );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,   "SPICE(VALUEOUTOFRANGE)", ex );
         }






         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check public field values." );


         ok = JNITestutils.chcksc ( "ABSOLUTE_MINIMUM",
                                    ABSOLUTE_MINIMUM,
                                    "=",
                                    "ABSMIN"           );

         ok = JNITestutils.chcksc ( "ABSOLUTE_MAXIMUM",
                                    ABSOLUTE_MAXIMUM,
                                    "=",
                                    "ABSMAX"           );

         ok = JNITestutils.chcksc ( "ADJUSTED_ABSMIN",
                                    ADJUSTED_ABSMIN,
                                    "=",
                                    "ADJ_ABSMIN"        );

         ok = JNITestutils.chcksc ( "ADJUSTED_ABSMAX",
                                    ADJUSTED_ABSMAX,
                                    "=",
                                    "ADJ_ABSMAX"        );

         ok = JNITestutils.chcksc ( "LOCAL_MINIMUM",
                                    LOCAL_MINIMUM,
                                    "=",
                                    "LOCMIN"           );

         ok = JNITestutils.chcksc ( "LOCAL_MAXIMUM",
                                    LOCAL_MAXIMUM,
                                    "=",
                                    "LOCMAX"           );

         ok = JNITestutils.chcksc ( "EQUALS",
                                    EQUALS,
                                    "=",
                                    "="                );

         ok = JNITestutils.chcksc ( "LESS_THAN",
                                    LESS_THAN,
                                    "=",
                                    "<"                );

         ok = JNITestutils.chcksc ( "GREATER_THAN",
                                    GREATER_THAN,
                                    "=",
                                    ">"                );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create ABSMAX constraint; make sure all " +
                              "attributes can be fetched and are correct."   );

         cons0 = createExtremumConstraint( ABSOLUTE_MAXIMUM );


         //
         // Check the "isExtremum" property.
         //
         ok = JNITestutils.chcksl( "isExtremum", cons0.isExtremum(), true );



         //
         // Check the "isUnadjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isUnadjustedExtremum",
                                    cons0.isUnadjustedExtremum(), true );


         //
         // Check the "isAdjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isAdjustedExtremum",
                                    cons0.isAdjustedExtremum(), false );


         //
         // Check the "isOrder" property.
         //
         ok = JNITestutils.chcksl( "isOrder",
                                    cons0.isOrder(), false );


         //
         // Get the JNISpice relational operator string for this constraint.
         //
         str0  = cons0.getRelation();

         ok = JNITestutils.chcksc ( "relation",
                                    str0,
                                    "=",
                                    ABSOLUTE_MAXIMUM  );

         //
         // Get the CSPICE relational operator string for this constraint.
         //
         str0  = cons0.getCSPICERelation();

         ok = JNITestutils.chcksc ( "CSPICE relation",
                                    str0,
                                    "=",
                                    "ABSMAX"           );


         //
         // Check the reference value for this constraint.
         //
         value = cons0.getReferenceValue();

         ok = JNITestutils.chcksd( "Reference value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );

         //
         // Check the adjustment value for this constraint.
         //
         value = cons0.getAdjustmentValue();

         ok = JNITestutils.chcksd( "Adjustment value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create ABSMIN constraint; make sure all " +
                              "attributes can be fetched and are correct."   );

         cons0 = createExtremumConstraint( ABSOLUTE_MINIMUM );


         //
         // Check the "isExtremum" property.
         //
         ok = JNITestutils.chcksl( "isExtremum", cons0.isExtremum(), true );



         //
         // Check the "isUnadjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isUnadjustedExtremum",
                                    cons0.isUnadjustedExtremum(), true );


         //
         // Check the "isAdjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isAdjustedExtremum",
                                    cons0.isAdjustedExtremum(), false );


         //
         // Check the "isOrder" property.
         //
         ok = JNITestutils.chcksl( "isOrder",
                                    cons0.isOrder(), false );


         //
         // Get the JNISpice relational operator string for this constraint.
         //
         str0  = cons0.getRelation();

         ok = JNITestutils.chcksc ( "relation",
                                    str0,
                                    "=",
                                    ABSOLUTE_MINIMUM  );

         //
         // Get the CSPICE relational operator string for this constraint.
         //
         str0  = cons0.getCSPICERelation();

         ok = JNITestutils.chcksc ( "CSPICE relation",
                                    str0,
                                    "=",
                                    "ABSMIN"           );


         //
         // Check the reference value for this constraint.
         //
         value = cons0.getReferenceValue();

         ok = JNITestutils.chcksd( "Reference value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );

         //
         // Check the adjustment value for this constraint.
         //
         value = cons0.getAdjustmentValue();

         ok = JNITestutils.chcksd( "Adjustment value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create adjusted ABSMAX constraint; make sure " +
                              "all attributes can be fetched and are " +
                              "correct."   );

         xval = 10.0;

         cons0 = createExtremumConstraint( ADJUSTED_ABSMAX, xval );


         //
         // Check the "isExtremum" property.
         //
         ok = JNITestutils.chcksl( "isExtremum", cons0.isExtremum(), true );



         //
         // Check the "isUnadjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isUnadjustedExtremum",
                                    cons0.isUnadjustedExtremum(), false );


         //
         // Check the "isAdjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isAdjustedExtremum",
                                    cons0.isAdjustedExtremum(), true );


         //
         // Check the "isOrder" property.
         //
         ok = JNITestutils.chcksl( "isOrder",
                                    cons0.isOrder(), false );


         //
         // Get the JNISpice relational operator string for this constraint.
         //
         str0  = cons0.getRelation();

         ok = JNITestutils.chcksc ( "relation",
                                    str0,
                                    "=",
                                    ADJUSTED_ABSMAX  );

         //
         // Get the CSPICE relational operator string for this constraint.
         //
         str0  = cons0.getCSPICERelation();

         ok = JNITestutils.chcksc ( "CSPICE relation",
                                    str0,
                                    "=",
                                    "ABSMAX"           );


         //
         // Check the reference value for this constraint.
         //
         value = cons0.getReferenceValue();

         ok = JNITestutils.chcksd( "Reference value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );

         //
         // Check the adjustment value for this constraint.
         //
         value = cons0.getAdjustmentValue();

         ok = JNITestutils.chcksd( "Adjustment value",
                                    value,
                                    "=",
                                    xval,
                                    0.0                );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create adjusted ABSMIN constraint; make " +
                              "sure all attributes can be fetched and " +
                              "are correct."   );

         xval = 15.0;

         cons0 = createExtremumConstraint( ADJUSTED_ABSMIN, xval );


         //
         // Check the "isExtremum" property.
         //
         ok = JNITestutils.chcksl( "isExtremum", cons0.isExtremum(), true );



         //
         // Check the "isUnadjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isUnadjustedExtremum",
                                    cons0.isUnadjustedExtremum(), false );


         //
         // Check the "isAdjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isAdjustedExtremum",
                                    cons0.isAdjustedExtremum(), true );


         //
         // Check the "isOrder" property.
         //
         ok = JNITestutils.chcksl( "isOrder",
                                    cons0.isOrder(), false );


         //
         // Get the JNISpice relational operator string for this constraint.
         //
         str0  = cons0.getRelation();

         ok = JNITestutils.chcksc ( "relation",
                                    str0,
                                    "=",
                                    ADJUSTED_ABSMIN  );

         //
         // Get the CSPICE relational operator string for this constraint.
         //
         str0  = cons0.getCSPICERelation();

         ok = JNITestutils.chcksc ( "CSPICE relation",
                                    str0,
                                    "=",
                                    "ABSMIN"           );


         //
         // Check the reference value for this constraint.
         //
         value = cons0.getReferenceValue();

         ok = JNITestutils.chcksd( "Reference value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );

         //
         // Check the adjustment value for this constraint.
         //
         value = cons0.getAdjustmentValue();

         ok = JNITestutils.chcksd( "Adjustment value",
                                    value,
                                    "=",
                                    xval,
                                    0.0                );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create LOCMAX constraint; make sure all " +
                              "attributes can be fetched and are correct."   );

         cons0 = createExtremumConstraint( LOCAL_MAXIMUM );


         //
         // Check the "isExtremum" property.
         //
         ok = JNITestutils.chcksl( "isExtremum", cons0.isExtremum(), true );



         //
         // Check the "isUnadjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isUnadjustedExtremum",
                                    cons0.isUnadjustedExtremum(), true );


         //
         // Check the "isAdjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isAdjustedExtremum",
                                    cons0.isAdjustedExtremum(), false );


         //
         // Check the "isOrder" property.
         //
         ok = JNITestutils.chcksl( "isOrder",
                                    cons0.isOrder(), false );


         //
         // Get the JNISpice relational operator string for this constraint.
         //
         str0  = cons0.getRelation();

         ok = JNITestutils.chcksc ( "relation",
                                    str0,
                                    "=",
                                    LOCAL_MAXIMUM  );

         //
         // Get the CSPICE relational operator string for this constraint.
         //
         str0  = cons0.getCSPICERelation();

         ok = JNITestutils.chcksc ( "CSPICE relation",
                                    str0,
                                    "=",
                                    "LOCMAX"           );


         //
         // Check the reference value for this constraint.
         //
         value = cons0.getReferenceValue();

         ok = JNITestutils.chcksd( "Reference value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );

         //
         // Check the adjustment value for this constraint.
         //
         value = cons0.getAdjustmentValue();

         ok = JNITestutils.chcksd( "Adjustment value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create LOCMIN constraint; make sure all " +
                              "attributes can be fetched and are correct."   );

         cons0 = createExtremumConstraint( LOCAL_MINIMUM );


         //
         // Check the "isExtremum" property.
         //
         ok = JNITestutils.chcksl( "isExtremum", cons0.isExtremum(), true );



         //
         // Check the "isUnadjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isUnadjustedExtremum",
                                    cons0.isUnadjustedExtremum(), true );


         //
         // Check the "isAdjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isAdjustedExtremum",
                                    cons0.isAdjustedExtremum(), false );


         //
         // Check the "isOrder" property.
         //
         ok = JNITestutils.chcksl( "isOrder",
                                    cons0.isOrder(), false );


         //
         // Get the JNISpice relational operator string for this constraint.
         //
         str0  = cons0.getRelation();

         ok = JNITestutils.chcksc ( "relation",
                                    str0,
                                    "=",
                                    LOCAL_MINIMUM  );

         //
         // Get the CSPICE relational operator string for this constraint.
         //
         str0  = cons0.getCSPICERelation();

         ok = JNITestutils.chcksc ( "CSPICE relation",
                                    str0,
                                    "=",
                                    "LOCMIN"           );


         //
         // Check the reference value for this constraint.
         //
         value = cons0.getReferenceValue();

         ok = JNITestutils.chcksd( "Reference value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );

         //
         // Check the adjustment value for this constraint.
         //
         value = cons0.getAdjustmentValue();

         ok = JNITestutils.chcksd( "Adjustment value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create < constraint; make sure all " +
                              "attributes can be fetched and are correct."   );


         xval = 3.0;

         cons0 = createReferenceConstraint( LESS_THAN, xval );


         //
         // Check the "isExtremum" property.
         //
         ok = JNITestutils.chcksl( "isExtremum", cons0.isExtremum(), false );



         //
         // Check the "isUnadjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isUnadjustedExtremum",
                                    cons0.isUnadjustedExtremum(), false );


         //
         // Check the "isAdjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isAdjustedExtremum",
                                    cons0.isAdjustedExtremum(), false );


         //
         // Check the "isOrder" property.
         //
         ok = JNITestutils.chcksl( "isOrder",
                                    cons0.isOrder(), true );


         //
         // Get the JNISpice relational operator string for this constraint.
         //
         str0  = cons0.getRelation();

         ok = JNITestutils.chcksc ( "relation",
                                    str0,
                                    "=",
                                    LESS_THAN  );

         //
         // Get the CSPICE relational operator string for this constraint.
         //
         str0  = cons0.getCSPICERelation();

         ok = JNITestutils.chcksc ( "CSPICE relation",
                                    str0,
                                    "=",
                                    "<"           );


         //
         // Check the reference value for this constraint.
         //
         value = cons0.getReferenceValue();

         ok = JNITestutils.chcksd( "Reference value",
                                    value,
                                    "=",
                                    xval,
                                    0.0                );

         //
         // Check the adjustment value for this constraint.
         //
         value = cons0.getAdjustmentValue();

         ok = JNITestutils.chcksd( "Adjustment value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create > constraint; make sure all " +
                              "attributes can be fetched and are correct."   );


         xval = -3.0;

         cons0 = createReferenceConstraint( GREATER_THAN, xval );


         //
         // Check the "isExtremum" property.
         //
         ok = JNITestutils.chcksl( "isExtremum", cons0.isExtremum(), false );



         //
         // Check the "isUnadjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isUnadjustedExtremum",
                                    cons0.isUnadjustedExtremum(), false );


         //
         // Check the "isAdjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isAdjustedExtremum",
                                    cons0.isAdjustedExtremum(), false );


         //
         // Check the "isOrder" property.
         //
         ok = JNITestutils.chcksl( "isOrder",
                                    cons0.isOrder(), true );


         //
         // Get the JNISpice relational operator string for this constraint.
         //
         str0  = cons0.getRelation();

         ok = JNITestutils.chcksc ( "relation",
                                    str0,
                                    "=",
                                    GREATER_THAN  );

         //
         // Get the CSPICE relational operator string for this constraint.
         //
         str0  = cons0.getCSPICERelation();

         ok = JNITestutils.chcksc ( "CSPICE relation",
                                    str0,
                                    "=",
                                    ">"           );


         //
         // Check the reference value for this constraint.
         //
         value = cons0.getReferenceValue();

         ok = JNITestutils.chcksd( "Reference value",
                                    value,
                                    "=",
                                    xval,
                                    0.0                );

         //
         // Check the adjustment value for this constraint.
         //
         value = cons0.getAdjustmentValue();

         ok = JNITestutils.chcksd( "Adjustment value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create = constraint; make sure all " +
                              "attributes can be fetched and are correct."   );


         xval = -33.0;

         cons0 = createReferenceConstraint( EQUALS, xval );


         //
         // Check the "isExtremum" property.
         //
         ok = JNITestutils.chcksl( "isExtremum", cons0.isExtremum(), false );



         //
         // Check the "isUnadjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isUnadjustedExtremum",
                                    cons0.isUnadjustedExtremum(), false );


         //
         // Check the "isAdjustedExtremum" property.
         //
         ok = JNITestutils.chcksl( "isAdjustedExtremum",
                                    cons0.isAdjustedExtremum(), false );


         //
         // Check the "isOrder" property.
         //
         ok = JNITestutils.chcksl( "isOrder",
                                    cons0.isOrder(), true );


         //
         // Get the JNISpice relational operator string for this constraint.
         //
         str0  = cons0.getRelation();

         ok = JNITestutils.chcksc ( "relation",
                                    str0,
                                    "=",
                                    EQUALS  );

         //
         // Get the CSPICE relational operator string for this constraint.
         //
         str0  = cons0.getCSPICERelation();

         ok = JNITestutils.chcksc ( "CSPICE relation",
                                    str0,
                                    "=",
                                    "="           );


         //
         // Check the reference value for this constraint.
         //
         value = cons0.getReferenceValue();

         ok = JNITestutils.chcksd( "Reference value",
                                    value,
                                    "=",
                                    xval,
                                    0.0                );

         //
         // Check the adjustment value for this constraint.
         //
         value = cons0.getAdjustmentValue();

         ok = JNITestutils.chcksd( "Adjustment value",
                                    value,
                                    "=",
                                    0.0,
                                    0.0                );







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

