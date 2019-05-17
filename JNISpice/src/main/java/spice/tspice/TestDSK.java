
package spice.tspice;


import java.io.*;
import spice.basic.*;
import static spice.basic.DSK.*;
import static spice.basic.DSKDescriptor.*;
import static spice.basic.DSKToleranceKey.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestDSK provides methods that implement test families for
class DSK.

<h3>Version 1.0.0 28-DEC-2016 (NJB)</h3>

*/
public class TestDSK extends Object
{

   //
   // Class constants                   
   //
   // The values here must be kept consistent with the SPICELIB
   // file dsktol.inc.
   //
   final static double                  DEF_XFRACT = 1.e-10;
   final static double                  DEF_SGREED = 1.e-8;
   final static double                  DEF_SGPADM = 1.e-10;
   final static double                  DEF_PTMEMM = 1.e-7;
   final static double                  DEF_ANGMRG = 1.e-12;
   final static double                  DEF_LONALI = 1.e-12;

   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test class DSK APIs.
   */
   public static boolean f_DSK()

      throws SpiceException
   {
      //
      // Constants
      //

      final String                      DSK0             =    "dsk_test0.bds";
      final String                      DSK1             =    "dsk_test1.bds";

      final double                      TIGHT            =     1.e-12;
      final double                      VTIGHT           =     1.e-14;
 
      //
      // Local variables
      //
      Body[]                            bodies;
      Body                              body;

      DSK                               dsk     = null;

      String                            bodnam;
      String                            frame;

      Surface[]                         surfaces;
      Surface[]                         xSurfaces;


      boolean[]                         foundArray = new boolean[1];
      boolean                           ok;
 
      double                            angmrg;
      double                            lonali;
      double                            sgpadm;
      double                            sgreed;
      double                            tol;
      double                            ptmemm;
      double                            xfract;

      int[]                             bodyArray;
      int[]                             bodyIDs;
      int                               bodyid;
      int                               i;
      int                               j;
      int                               n;
      int                               nbody;
      int                               nsurf;
      int[]                             surfaceIDs;
      int                               surfid;
      int[]                             xBodies;

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

         JNITestutils.topen ( "f_DSK" );




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

         dsk = new DSK();

         //
         // All we expect to be set in this object are false values
         // for the "readable" and "writable" flags.
         //
         ok = JNITestutils.chcksl( "readable", dsk.isReadable(), false );
         ok = JNITestutils.chcksl( "writable", dsk.isWritable(), false );

         //
         // --------Case-----------------------------------------------
         //
         //
         // JNITestutils.tcase ( "Call standard constructor; create DSK " +
         //                       "instance from a file name."              );
         //
         // See class TestDSKPro for code implementing this test case. 
         //
 

         //
         // Test DSK tolerance fetch method. 
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch angular margin." );

         angmrg = DSK.getTolerance ( KEYAMG );

         tol = VTIGHT;

         ok  = JNITestutils.chcksd( "angmrg", angmrg, "~", DEF_ANGMRG, tol );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch longitude alias margin." );

         lonali = DSK.getTolerance ( KEYLAL );

         tol = VTIGHT;

         ok  = JNITestutils.chcksd( "lonali", lonali, "~", DEF_LONALI, tol );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch surface-point membership margin." );

         ptmemm = DSK.getTolerance ( KEYPTM );

         tol = VTIGHT;

         ok  = JNITestutils.chcksd( "ptmemm", ptmemm, "~", DEF_PTMEMM, tol );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch greedy margin." );

         sgreed = DSK.getTolerance ( KEYSGR );

         tol = VTIGHT;

         ok  = JNITestutils.chcksd( "sgreed", sgreed, "~", DEF_SGREED, tol );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch segment pad margin." );

         sgpadm = DSK.getTolerance ( KEYSPM );

         tol = VTIGHT;

         ok  = JNITestutils.chcksd( "sgpadm", sgpadm, "~", DEF_SGPADM, tol );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch plate expansion fraction" );

         xfract = DSK.getTolerance ( KEYXFR );

         tol = VTIGHT;

         ok  = JNITestutils.chcksd( "xfract", xfract, "~", DEF_XFRACT, tol );



         //
         // Test DSK tolerance set method. 
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Set angular rounding margin. This is an " +
                              "immutable parameter, so we expect an "    +
                              "exception to be thrown."                    );

         try
         {
            DSK.setTolerance ( KEYAMG, 2 * DEF_ANGMRG );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(IMMUTABLEVALUE)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Set longitude alias margin. This is an " +
                              "immutable parameter, so we expect an "    +
                              "exception to be thrown."                    );

         try
         {
            DSK.setTolerance ( KEYLAL, 2 * DEF_LONALI );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(IMMUTABLEVALUE)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Set point-plate membership margin." );

         DSK.setTolerance ( KEYPTM, 2 * DEF_PTMEMM );

         tol    = VTIGHT;

         ptmemm = DSK.getTolerance( KEYPTM );

         ok  = JNITestutils.chcksd( "ptmemm", ptmemm, "~", 2*DEF_PTMEMM, tol );

         //
         // Restore original value.
         //
         DSK.setTolerance ( KEYPTM, DEF_PTMEMM );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Set segment pad margin." );

         DSK.setTolerance ( KEYSPM, 2 * DEF_SGPADM );

         tol    = VTIGHT;

         sgpadm = DSK.getTolerance( KEYSPM );

         ok  = JNITestutils.chcksd( "sgpadm", sgpadm, "~", 2*DEF_SGPADM, tol );

         //
         // Restore original value.
         //
         DSK.setTolerance ( KEYSPM, DEF_SGPADM );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Set greedy margin." );

         DSK.setTolerance ( KEYSGR, 2 * DEF_SGREED );

         tol    = VTIGHT;

         sgreed = DSK.getTolerance( KEYSGR );

         ok  = JNITestutils.chcksd( "sgreed", sgreed, "~", 2*DEF_SGREED, tol );

         //
         // Restore original value.
         //
         DSK.setTolerance ( KEYSGR, DEF_SGREED );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Set expansion fraction." );

         DSK.setTolerance ( KEYXFR, 2 * DEF_XFRACT );

         tol    = VTIGHT;

         xfract = DSK.getTolerance( KEYXFR );

         ok  = JNITestutils.chcksd( "xfract", xfract, "~", 2*DEF_XFRACT, tol );

         //
         // Restore original value.
         //
         DSK.setTolerance ( KEYXFR, DEF_XFRACT );

 


         //
         // Test DSK summary methods. 
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Create DSK file with multiple bodies and " +
                              "multiple surfaces per body."                );

         //
         // Make sure the file we're about to create doesn't already exist.
         // Since we append to the file, this is especially important.
         //
         ( new File ( DSK1 ) ).delete();


         nbody   = 9;
   
         bodyIDs = new int[nbody];


         for ( i = 1;  i <= nbody;  i++ )
         {
            bodyid       = (100 * i) + 99;
            bodyIDs[i-1] = bodyid;
 
            bodnam       = ( new Body( bodyid ) ).getName();

            frame        = String.format( "IAU_%s", bodnam );
            


           
            //
            // Create i segments for the current body, each 
            // with a different surface ID.
            //           
            for ( j = 0;  j < i;  j++ )
            {
                surfid = j;

                JNITestutils.t_smldsk ( bodyid, surfid, frame, DSK1 );
            }              
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Open for read access the file we just "  + 
                              "created."  );


         dsk = DSK.openForRead( DSK1 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get the set of bodies in the file we just " + 
                              "created."                                    );

         
         bodies  = dsk.getBodies();

         bodyArray = new int[nbody];
         xBodies   = new int[nbody];

         for ( i = 0;  i < nbody;  i++ )
         {
            xBodies[i]   = bodyIDs[i];

            bodyArray[i] = bodies[i].getIDCode();
         }

         ok = JNITestutils.chckai( "bodies", bodyArray, 
                                   "=",      xBodies   );


          //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "For each body in the file we just " + 
                              "created, get the set of associated surfaces." );


         for ( i = 0;  i < nbody;  i++ )
         {
            surfaces = dsk.getSurfaces( bodies[i] );

            n        = surfaces.length;

            //
            // Check the number of surfaces.
            //
            ok = JNITestutils.chcksi( "n", n, "=", i+1, 0 );

            for ( j = 0;  j < n;  j++ )
            {
               String label = 

                 String.format( "surface id %d for body %s", 
                                j+1,
                                (bodies[i]).getName() );

               surfid = (surfaces[j]).getIDCode();

               ok = JNITestutils.chcksi( label, surfid, "=", j, 0 );
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

         //ex.printStackTrace();

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
         dsk.close();

         CSPICE.kclear();


         //
         // Delete the DSKs if they exist.
         //

         ( new File ( DSK0 ) ).delete();
         ( new File ( DSK1 ) ).delete();
      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

