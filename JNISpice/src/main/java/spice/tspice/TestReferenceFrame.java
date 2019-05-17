
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestReferenceFrame provides methods that implement test families for
the class ReferenceFrame.

<h3>Version 2.0.0 28-DEC-2016 (NJB)</h3>

<p>Updated to test two-epoch position transformation 
   constructor.

<p>Updated to test equals and hashCode methods.

<p>Clean-up code has been moved to "finally" block.

<h3>Version 1.0.0 09-DEC-2009 (NJB)</h3>
*/
public class TestReferenceFrame extends Object
{

   //
   // Class constants
   //
   private static String  PCK           = "ref.tpc";
   private static String  SPK           = "ref.bsp";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test ReferenceFrame and associated classes.
   */
   public static boolean f_ReferenceFrame()

      throws SpiceException
   {
      //
      // Constants
      //

      final double                      TIGHT_TOL = 1.e-12;

      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              observer;
      Body                              target;

      Matrix33                          m0;
      Matrix33                          m1;
      Matrix33                          ptrans;
      Matrix33                          ptrans0;
      Matrix33                          ptrans1;

      Matrix66                          strans;

      PositionVector                    p0;
      PositionVector                    p1;

      ReferenceFrame                    ref0;
      ReferenceFrame                    ref1;
      ReferenceFrame                    ref2;

      StateVector                       s0;
      StateVector                       s1;
      StateVector                       s2;

      String                            name0;
      String                            name1;

      TDBTime                           et0;
      TDBTime                           et1;

      Vector3                           v3pos;

      Vector6                           v6state;

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

         JNITestutils.topen ( "f_ReferenceFrame" );



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
         // Delete PCK if it exists. Create and load a PCK file.
         //
         ( new File ( PCK ) ).delete();

         JNITestutils.tstpck( PCK, true, false );

         //
         // Delete SPK if it exists. Create and load a SPK file.
         //
         ( new File ( SPK ) ).delete();

         handle = JNITestutils.tstspk( SPK, true );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "Error: create ReferenceFrame using " +
                               "blank name." );

         try
         {
            ref0 = new ReferenceFrame( " " );

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
         JNITestutils.tcase (  "Error: create ReferenceFrame using " +
                               "empty name." );

         try
         {
            ref0 = new ReferenceFrame( "" );

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

         JNITestutils.tcase ( "Create ReferenceFrame from name." );

         ref0    = new ReferenceFrame( "j2000" );

         name0 = ref0.getName();


         ok = JNITestutils.chcksc ( "name",
                                    name0,
                                    "=",
                                    "J2000" );


         ok = JNITestutils.chcksi ( "ID",
                                    ref0.getIDCode(),
                                    "=",
                                    1,
                                    0               );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create ReferenceFrame from name not " +
                              "known to SPICE." );

         ref0    = new ReferenceFrame( "xyz" );

         name0 = ref0.getName();

         //
         // We expect the frame won't be normalized in this case.
         //
         ok = JNITestutils.chcksc ( "name",
                                    name0,
                                    "=",
                                    "xyz" );

         //
         // There's no ID code for this frame (so we don't try to obtain it).
         //


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Create ReferenceFrame from ID code." );

         ref0    = new ReferenceFrame( 1 );

         name0 = ref0.getName();


         ok = JNITestutils.chcksc ( "name",
                                    name0,
                                    "=",
                                    "J2000" );


         ok = JNITestutils.chcksi ( "ID",
                                    ref0.getIDCode(),
                                    "=",
                                    1,
                                    0               );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor" );

         ref0    = new ReferenceFrame( "J2000" );
         ref1    = new ReferenceFrame( ref0 );

         //
         // Make sure that changing ref0 doesn't affect ref1.
         //
         ref0    = new ReferenceFrame( "IAU_MOON" );

         name1 = ref1.getName();


         ok = JNITestutils.chcksc ( "name",
                                    name1,
                                    "=",
                                    "J2000" );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor on unknown frame." );

         ref0    = new ReferenceFrame( "xyz" );
         ref1    = new ReferenceFrame( ref0 );

         //
         // Make sure that changing ref0 doesn't affect ref1.
         //
         ref0    = new ReferenceFrame( "IAU_MOON" );

         name1 = ref1.getName();


         ok = JNITestutils.chcksc ( "name",
                                    name1,
                                    "=",
                                    "xyz" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test equality operator" );

         ref0    = new ReferenceFrame( 1        );
         ref1    = new ReferenceFrame( "j2000"  );
         ref2    = new ReferenceFrame( "b1950"  );

         //
         // Make sure that ref0 and ref1 are equal.
         //
         ok = JNITestutils.chcksl ( "ref0 == ref1",
                                    ref0.equals( ref1 ),
                                    true             );

         //
         // Make sure that ref0 and ref2 are not equal.
         //
         ok = JNITestutils.chcksl ( "ref0 == ref2",
                                    ref0.equals( ref2 ),
                                    false            );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test equality operator on unknown frames" );

         ref0    = new ReferenceFrame( "xyz"    );
         ref1    = new ReferenceFrame( "XYZ"    );
         ref2    = new ReferenceFrame( "b1950"  );

         //
         // Make sure that ref0 and ref1 are equal.
         //
         ok = JNITestutils.chcksl ( "ref0 == ref1",
                                    ref0.equals( ref1 ),
                                    true             );

         //
         // Make sure that ref0 and ref2 are not equal.
         //
         ok = JNITestutils.chcksl ( "ref0 == ref2",
                                    ref0.equals( ref2 ),
                                    false            );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test equality operator on an object not " + 
                              "of class ReferenceFrame."                  );

         ref0    = new ReferenceFrame( 1 );

         //
         // Make sure that ref0 and observer are not equal.
         //
         observer = new Body( "Moon" );

         ok = JNITestutils.chcksl ( "ref0 == observer",
                                    ref0.equals( observer ),
                                    false                    );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test equality operator on a null value." );

         ref0    = new ReferenceFrame( 1 );

         //
         // Make sure that ref0 and null are not equal.
         //
         observer = new Body( "Moon" );

         ok = JNITestutils.chcksl ( "ref0 == null",
                                    ref0.equals( null ),
                                    false                    );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test hashCode: use ReferenceFrame instance " +
                              "created from an integer."                     );

         ref0 = new ReferenceFrame( 1 );

         ok = JNITestutils.chcksi ( "ref0.hashCode()",
                                    ref0.hashCode(), "=", 1, 0 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test hashCode: use ReferenceFrame instance " +
                              "created from a String."                       );

         ref0 = new ReferenceFrame( "J2000" );

         ok = JNITestutils.chcksi ( "ref0.hashCode()",
                                    ref0.hashCode(), "=", 1, 0 );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getPositionTransformation." );



         //
         // Get a position vector referenced to J2000; transform to
         // the IAU_EARTH frame explicitly. Compare to the same
         // vector looked up in the IAU_EARTH frame.
         //
         abcorr   = new AberrationCorrection( "None" );

         observer = new Body( "Earth" );
         target   = new Body( "Moon"  );

         ref0     = new ReferenceFrame( "J2000"     );
         ref1     = new ReferenceFrame( "IAU_EARTH" );

         et0      = new TDBTime( "2009 Dec 9" );

         ptrans   = ref0.getPositionTransformation( ref1, et0 );

         p0       = new PositionVector( target, et0, ref0, abcorr, observer );
         p1       = new PositionVector( target, et0, ref1, abcorr, observer );

         v3pos    = ptrans.mxv( p0 );

         //
         // We expect pos to be nearly equal to p1.
         //
         ok = JNITestutils.chckad ( "v3pos",
                                    v3pos.toArray(),
                                    "~~/",
                                    p1.toArray(),
                                    TIGHT_TOL        );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test two-epoch version of " +
                              "getPositionTransformation." );



         //
         // Compute the transformation from the IAU_EARTH frame at
         // one epoch to that of the IAU_MARS frame at another.
         // Do this by using J2000 as an intermediate frame. 
         // Compare the resulting transformation to one obtained
         // by calling getPositionTransformation(ReferenceFrame,Time,Time).
         // 
         abcorr   = new AberrationCorrection( "None" );

         observer = new Body( "Earth" );
         target   = new Body( "Moon"  );

         ref0     = new ReferenceFrame( "IAU_EARTH" );
         ref1     = new ReferenceFrame( "J2000"     );
         ref2     = new ReferenceFrame( "IAU_MARS"  );
         et0      = new TDBTime( "2009 Dec 9"  );
         et1      = new TDBTime( "2016 Nov 11" );

         ptrans0  = ref0.getPositionTransformation( ref1, et0 );
         ptrans1  = ref1.getPositionTransformation( ref2, et1 );

         m0       = ptrans1.mxm( ptrans0 );

         m1       = ref0.getPositionTransformation( ref2, et0, et1 );



         //
         // We expect m1 to be nearly equal to m0.
         //
         ok = JNITestutils.chckad ( "m1",
                                    m1.toArray1D(),
                                    "~~/",
                                    m0.toArray1D(),
                                    TIGHT_TOL        );

          
         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getStateTransformation." );

         //
         // Get a state vector referenced to J2000; transform to
         // the IAU_EARTH frame explicitly. Compare to the same
         // vector looked up in the IAU_EARTH frame.
         //
         abcorr   = new AberrationCorrection( "None" );

         observer = new Body( "Earth" );
         target   = new Body( "Moon"  );

         ref0     = new ReferenceFrame( "J2000"     );
         ref1     = new ReferenceFrame( "IAU_EARTH" );

         et0      = new TDBTime( "2009 Dec 9" );

         strans   = ref0.getStateTransformation( ref1, et0 );

         s0       = new StateVector( target, et0, ref0, abcorr, observer );
         s1       = new StateVector( target, et0, ref1, abcorr, observer );

         v6state  = strans.mxv( s0 );

         //
         // We expect the position component of `v6state' to be nearly equal to
         // that of `s1'.
         //
         ok = JNITestutils.chckad ( "v6state/position",
                                    v6state.getVector3(0).toArray(),
                                    "~~/",
                                    s1.getPosition().toArray(),
                                    TIGHT_TOL                     );

         //
         // We expect the velocity component of `v6state' to be nearly equal to
         // that of `s1'.
         //
         ok = JNITestutils.chckad ( "v6state/velocity",
                                    v6state.getVector3(1).toArray(),
                                    "~~/",
                                    s1.getVelocity().toArray(),
                                    TIGHT_TOL                     );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toString." );

         ref0    = new ReferenceFrame( "iau_Earth"   );

         ok = JNITestutils.chcksc ( "string form of frame 34",
                                    ref0.toString(),
                                    "=",
                                    "IAU_EARTH"             );

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
         // Get rid of the PCK file.
         //
         ( new File ( PCK ) ).delete();

         //
         // Get rid of the SPK file.
         //
         CSPICE.spkuef( handle );

         ( new File ( SPK ) ).delete();

      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

