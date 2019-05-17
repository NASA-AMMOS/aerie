
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestSurface provides methods that implement test families for
the class Surface.

<h3> Version 1.0.0 09-NOV-2016 (NJB) </h3>


*/
public class TestSurface extends Object
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
   Test Surface and associated classes.
   */
   public static boolean f_Surface()

      throws SpiceException
   {
      //
      // Constants
      //

      final double                      TIGHT_TOL = 1.e-12;

      //
      // Local variables
      //
      Body                              b0;
      Body                              b1;

      Surface                           s0;
      Surface                           s1;
      Surface                           s2;

      String[]                          cvals;
      String                            name0;
      String                            name1;
      String                            name2;
      String                            xname;

      Vector3                           v0;

      boolean                           ok;

      int                               bodyid;
      int                               handle;
      int                               i;
      int[]                             ivals;
      int                               surfid;
      int                               xhash;
      int                               xsurfid;


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

         JNITestutils.topen ( "f_Surface" );


 



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create Surface using blank body name." );

         try
         {
            s0 = new Surface( " ", new Body(399) );

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
         JNITestutils.tcase (  "Error: create Surface using empty body name." );

         try
         {
            s0 = new Surface( "", new Body(399)  );

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

         JNITestutils.tcase ( "Call no-args constructor." );

         //
         // This test simply verifies that the constructor exists
         // and can be called. The resulting object is uninitialized.
         //
         s0 = new Surface();


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create Surface from name." );


         //
         // Set up surface name-ID mapping.
         //
         String namkvn = "NAIF_SURFACE_NAME";    
         String codkvn = "NAIF_SURFACE_CODE";
         String bodkvn = "NAIF_SURFACE_BODY";

         xname         = new String  ( "Mars surface 1" );
         cvals         = new String[1];
         cvals[0]      = xname;

         CSPICE.pcpool( namkvn, cvals );         

         surfid        = 1;
         ivals         = new int[1];
         ivals[0]      = surfid;

         CSPICE.pipool( codkvn, ivals );         

         bodyid        = 499;
         ivals[0]      = bodyid;
         CSPICE.pipool( bodkvn, ivals );         

         //
         // Create surface instance.
         //
         b0    = new Body( bodyid );

         s0    = new Surface ( xname, b0 );

         name0 = s0.getName();


         ok = JNITestutils.chcksc ( "name",
                                    name0,
                                    "=",
                                    xname );


         ok = JNITestutils.chcksi ( "ID",
                                    s0.getIDCode(),
                                    "=",
                                    surfid,
                                    0               );

         ok = JNITestutils.chcksi ( "body",
                                    s0.getBody().getIDCode(),
                                    "=",
                                    bodyid,
                                    0               );

 


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create Surface from ID code." );


         xname         = new String  ( "Earth surface 1" );
         cvals         = new String[1];
         cvals[0]      = xname;

         CSPICE.pcpool( namkvn, cvals );         

         surfid        = 2;
         ivals         = new int[1];
         ivals[0]      = surfid;

         CSPICE.pipool( codkvn, ivals );         

         bodyid        = 399;
         ivals[0]      = bodyid;
         CSPICE.pipool( bodkvn, ivals );         

         b1     = new Body( bodyid );
         s1     = new Surface ( surfid, b1 );

         name1 = s1.getName();


         ok = JNITestutils.chcksc ( "name",
                                    name1,
                                    "=",
                                    xname );


         ok = JNITestutils.chcksi ( "ID",
                                    s1.getIDCode(),
                                    "=",
                                    surfid,
                                    0               );

         ok = JNITestutils.chcksi ( "body",
                                    s1.getBody().getIDCode(),
                                    "=",
                                    bodyid,
                                    0               );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString." );



         ok = JNITestutils.chcksc ( "name from toString",
                                    s1.toString(),
                                    "=",
                                    xname                );


 
        


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor." );

         s2    = new Surface( s1 );
         xname = new String ( "Earth surface 1" );

         //
         // Make sure that changing s1 doesn't affect s2.
         //
         s1    = new Surface ( 3, new Body(699) );

         name2 = s2.getName();


         ok    = JNITestutils.chcksc ( "name",
                                       name2,
                                       "=",
                                       xname );
 

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test equality operator for surfaces." );



         cvals         = new String[2];
         cvals[0]      = "saTurN surface 0";
         cvals[1]      = "saTurN surface 1";

         CSPICE.pcpool( namkvn, cvals );         


         ivals         = new int[2];
         ivals[0]      = 0;
         ivals[1]      = 1;
         CSPICE.pipool( codkvn, ivals );         

         bodyid        = 699;
         ivals[0]      = bodyid;
         ivals[1]      = bodyid;
         CSPICE.pipool( bodkvn, ivals );         


         surfid = 0;

         Surface s3 = new Surface( cvals[0], new Body(bodyid) );
         Surface s4 = new Surface( surfid,   new Body(bodyid) );
         Surface s5 = new Surface( cvals[1], new Body(bodyid) );

         //
         // Make sure that s3 and s4 are equal.
         //
         ok = JNITestutils.chcksl ( "s3 == s4",
                                    s3.equals( s4 ),
                                    true             );

         //
         // Make sure that s4 and s5 are not equal.
         //
         ok = JNITestutils.chcksl ( "s4 == s5",
                                    s4.equals( s5 ),
                                    false            );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test equality operator for null object." );

         //
         // Make sure that s4 is not equal to a null object.
         //
         ok = JNITestutils.chcksl ( "s4 == null",
                                    s4.equals( null ),
                                    false            );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test equality operator for non-surface " +
                              "object."                                  );

         //
         // Make sure that s4 is not equal to a Body.
         //
         ok = JNITestutils.chcksl ( "s4 == null",
                                    s4.equals( new Body(699) ),
                                    false                        );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test hashcode function." );

         bodyid = 11;
         surfid = 5;

         Surface s6 = new Surface( surfid, new Body(bodyid) );

         int h      = s6.hashCode();

         xhash      = (bodyid/2) + (surfid/2);

         ok = JNITestutils.chcksi ( "hashCode",
                                    h,
                                    "=",
                                    xhash,
                                    0         );
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

