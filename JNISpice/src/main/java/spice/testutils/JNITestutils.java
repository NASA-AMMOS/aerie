
package spice.testutils;

import java.util.*;
import spice.basic.*;


/**
Class JNITestutils provides JNI C wrappers for the CSPICE
tutils_c test utility wrappers.  The JNI C wrappers support
implementation of tspice in Java.

<p>
<pre>
Version 3.0.0 29-OCT-2016 (NJB)

   Added declarations of methods
 
      natdsk
      t_cg
      t_elds2z
      t_secds2
      t_smldsk
      t_torus
      t_wrtplt
      t_wrtplz
      zzellplt
      zzpsball
      zzpsbox
      zzpspoly
      zzpsscal
      zzpsun
      zzpsxfrm
      zzpsxlat
      
</pre>
<p>
<pre>
Version 2.0.0 07-JUN-2014 (NJB)

   Added declarations of methods
 
      illum
      subpt
      subslr
      zzt_boddsk
</pre>
<p>
<pre>
Version 1.0.0 17-MAY-2010 (NJB)

   Updated to accommodate change to argument list of 
   tsetup_c.
</pre>
Version 1.0.0 03-JAN-2010 (NJB)
*/


public class JNITestutils extends Object
{

   /**
   Check the values in a double precision array.
   */
   public native static synchronized boolean chckad ( String      name,
                                                      double[]    array,
                                                      String      comp,
                                                      double[]    exp,
                                                      double      tol    )
      throws SpiceErrorException;


   /**
   Check the values in an integer array.
   */
   public native static synchronized boolean chckai ( String      name,
                                                      int[]       array,
                                                      String      comp,
                                                      int[]       exp    )
      throws SpiceErrorException;


   /**
   Check a string scalar value against some expected value.
   */
   public native static synchronized boolean chcksc ( String      name,
                                                      String      val,
                                                      String      comp,
                                                      String      exp    )
      throws SpiceErrorException;


   /**
   Check a double precision scalar value against some expected value.
   */
   public native static synchronized boolean chcksd ( String      name,
                                                      double      val,
                                                      String      comp,
                                                      double      exp,
                                                      double      tol    )
      throws SpiceErrorException;


   /**
   Check an integer scalar value against some expected value.
   */
   public native static synchronized boolean chcksi ( String      name,
                                                      int         val,
                                                      String      comp,
                                                      int         exp,
                                                      int         tol    )
      throws SpiceErrorException;


   /**
   Check a boolean scalar value against some expected value.
   */
   public native static synchronized boolean chcksl ( String      name,
                                                      boolean     val,
                                                      boolean     exp    )
      throws SpiceErrorException;


   /**
   Check whether an expected SpiceException was thrown.

   Note that this not a native method.
   */
   public static        boolean chckth ( boolean     expected,
                                         String      expectedMsg,
                                         Exception   ex          )

      throws SpiceErrorException
   {
      boolean ok;

      String compoundMsg        = ex.getMessage();
      StringTokenizer tokenizer = new StringTokenizer ( compoundMsg, ":" );

      //
      // For debugging:
      //System.out.println( compoundMsg );

      /*
      Get the third token.
      */
      String shortMsg = null;

      for ( int i = 0;  i < 3;  i++ )
      {
         try
         {
            shortMsg = tokenizer.nextToken();
         }
         catch( java.util.NoSuchElementException exc )
         {
            //
            // For debugging
            //
            System.out.println( compoundMsg );
         }
      }
      shortMsg = shortMsg.trim();


      if ( expected )
      {
         ok  = JNITestutils.chcksc ( "Exception message",
                                     shortMsg,
                                     "=",
                                     expectedMsg         );
      }
      else
      {
         ok = JNITestutils.chcksc ( "Exception compound message",
                                    compoundMsg,
                                    "=",
                                    "<No exception expected>" );
      }

      return ( ok );
   }


   /**
   Check whether an expected short error message was signaled.
   */
   public native static synchronized boolean chckxc ( boolean     except,
                                                      String      shmsg   )
      throws SpiceErrorException;


   /**
   Call the deprecated CSPICE routine illum_c.
   */
   public native static synchronized void illum ( String    target,
                                                  double    et,
                                                  String    abcorr,
                                                  String    obsrvr,
                                                  double[]  spoint,
                                                  double[]  angles )
      throws SpiceErrorException;


   /*
   Return a pseudo-random double precision number lying in a specified
   interval.
   */

   /*
   public native static synchronized double rand ( double    lb,
                                                   double    ub )
      throws SpiceErrorException;
   */



   /**
   Create a DSK file for use with Nat's solar system.
   */
   public native static synchronized  void natdsk ( String    dsk,
                                                    String    aframe,
                                                    int       anlon,
                                                    int       anlat,
                                                    String    bframe,
                                                    int       bnlon,
                                                    int       bnlat   )
      throws SpiceErrorException;



   /**
   Create a IK file for use with Nat's solar system.
   */
   public native static synchronized  void natik ( String    IK,
                                                   String    SPK,
                                                   String    PCK,
                                                   boolean   loadIK,
                                                   boolean   keepIK  )
      throws SpiceErrorException;


   /**
   Create a PCK file for Nat's solar system.

   <p> This method creates the following PCK file:
   <pre>
   \begindata

         NAIF_BODY_NAME += ( 'ALPHA', 'BETA',  'GAMMA' )
         NAIF_BODY_CODE += ( 1000,     2000,   1001    )

   \begintext

      Radii for

         ALPHA
         BETA
         GAMMA

   \begindata

         BODY1000_RADII = ( 0.73249397533875424E+05,
                            0.36624698766937712E+05,
                            0.36624698766937712E+05 )

         BODY1001_RADII = ( 1.E4, 1.E4, 1.E4 )

         BODY2000_RADII = ( 0.22891526271046937E+04,
                            0.22891526271046937E+04,
                            0.1E+04 )

   \begintext

      Orientation data for

         ALPHA
         BETA
         GAMMA

   \begindata

         BODY1000_POLE_RA        = (    0.       0.         0. )
         BODY1000_POLE_DEC       = (  +90.       0.         0. )
         BODY1000_PM             = (    0.       0.         0. )

         BODY1001_POLE_RA        = ( +180.       0.         0. )
         BODY1001_POLE_DEC       = (    0.       0.         0. )
         BODY1001_PM             = ( +180.       360.       0. )

         BODY2000_POLE_RA        = (    0.       0.         0. )
         BODY2000_POLE_DEC       = (  +90.       0.         0. )
         BODY2000_PM             = (    0.       0.         0. )


   \begintext

      Body-fixed frame specifications for

         ALPHA
         BETA
         GAMMA

   \begindata

         FRAME_ALPHAFIXED          =  1000001
         FRAME_1000001_NAME        = 'ALPHAFIXED'
         FRAME_1000001_CLASS       =  4
         FRAME_1000001_CLASS_ID    =  1000001
         FRAME_1000001_CENTER      =  1000
         TKFRAME_1000001_RELATIVE  = 'BETAFIXED'
         TKFRAME_1000001_SPEC      = 'MATRIX'
         TKFRAME_1000001_MATRIX    = ( 0, 0, 1,
                                       1, 0, 0,
                                       0, 1, 0 )

         OBJECT_1000_FRAME       = 'ALPHAFIXED'



         FRAME_BETAFIXED         =  1000002
         FRAME_1000002_NAME      = 'BETAFIXED'
         FRAME_1000002_CLASS     =  2
         FRAME_1000002_CLASS_ID  =  2000
         FRAME_1000002_CENTER    =  2000

         OBJECT_2000_FRAME       = 'BETAFIXED'


         FRAME_GAMMAFIXED        =  1000003
         FRAME_1000003_NAME      = 'GAMMAFIXED'
         FRAME_1000003_CLASS     =  2
         FRAME_1000003_CLASS_ID  =  1001
         FRAME_1000003_CENTER    =  1001

         OBJECT_1001_FRAME       = 'GAMMAFIXED'

   \begintext

         View frames for body ALPHA relative to the Sun

            ALPHA_VIEW_XY     orbital motion of ALPHA lies in X-Y plane
                              of this frame.

            ALPHA_VIEW_XZ     orbital motion of ALPHA lies in X-Z plane
                              of this frame.

   \begindata

         FRAME_ALPHA_VIEW_XY         =  1700000
         FRAME_1700000_NAME           = 'ALPHA_VIEW_XY'
         FRAME_1700000_CLASS          =  5
         FRAME_1700000_CLASS_ID       =  1700000
         FRAME_1700000_CENTER         = 'ALPHA'
         FRAME_1700000_RELATIVE       = 'J2000'
         FRAME_1700000_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700000_FAMILY         = 'TWO-VECTOR'
         FRAME_1700000_PRI_AXIS       = 'X'
         FRAME_1700000_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700000_PRI_OBSERVER   = 'SUN'
         FRAME_1700000_PRI_TARGET     = 'ALPHA'
         FRAME_1700000_PRI_ABCORR     = 'NONE'
         FRAME_1700000_SEC_AXIS       = 'Y'
         FRAME_1700000_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700000_SEC_OBSERVER   = 'SUN'
         FRAME_1700000_SEC_TARGET     = 'ALPHA'
         FRAME_1700000_SEC_ABCORR     = 'NONE'
         FRAME_1700000_SEC_FRAME      = 'J2000'


         FRAME_ALPHA_VIEW_XZ         =  1700001
         FRAME_1700001_NAME           = 'ALPHA_VIEW_XZ'
         FRAME_1700001_CLASS          =  5
         FRAME_1700001_CLASS_ID       =  1700001
         FRAME_1700001_CENTER         = 'ALPHA'
         FRAME_1700001_RELATIVE       = 'J2000'
         FRAME_1700001_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700001_FAMILY         = 'TWO-VECTOR'
         FRAME_1700001_PRI_AXIS       = 'X'
         FRAME_1700001_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700001_PRI_OBSERVER   = 'SUN'
         FRAME_1700001_PRI_TARGET     = 'ALPHA'
         FRAME_1700001_PRI_ABCORR     = 'NONE'
         FRAME_1700001_SEC_AXIS       = 'Z'
         FRAME_1700001_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700001_SEC_OBSERVER   = 'SUN'
         FRAME_1700001_SEC_TARGET     = 'ALPHA'
         FRAME_1700001_SEC_ABCORR     = 'NONE'
         FRAME_1700001_SEC_FRAME      = 'J2000'

   \begintext

         Aberration corrected view frames for body ALPHA relative to the Sun

   \begindata

         FRAME_ALPHA_VIEW_XY_CN       =  1700002
         FRAME_1700002_NAME           = 'ALPHA_VIEW_XY_CN'
         FRAME_1700002_CLASS          =  5
         FRAME_1700002_CLASS_ID       =  1700002
         FRAME_1700002_CENTER         = 'ALPHA'
         FRAME_1700002_RELATIVE       = 'J2000'
         FRAME_1700002_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700002_FAMILY         = 'TWO-VECTOR'
         FRAME_1700002_PRI_AXIS       = 'X'
         FRAME_1700002_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700002_PRI_OBSERVER   = 'SUN'
         FRAME_1700002_PRI_TARGET     = 'ALPHA'
         FRAME_1700002_PRI_ABCORR     = 'CN'
         FRAME_1700002_SEC_AXIS       = 'Y'
         FRAME_1700002_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700002_SEC_OBSERVER   = 'SUN'
         FRAME_1700002_SEC_TARGET     = 'ALPHA'
         FRAME_1700002_SEC_ABCORR     = 'CN'
         FRAME_1700002_SEC_FRAME      = 'J2000'

         FRAME_ALPHA_VIEW_XY_CNS      =  1700003
         FRAME_1700003_NAME           = 'ALPHA_VIEW_XY_CNS'
         FRAME_1700003_CLASS          =  5
         FRAME_1700003_CLASS_ID       =  1700003
         FRAME_1700003_CENTER         = 'ALPHA'
         FRAME_1700003_RELATIVE       = 'J2000'
         FRAME_1700003_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700003_FAMILY         = 'TWO-VECTOR'
         FRAME_1700003_PRI_AXIS       = 'X'
         FRAME_1700003_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700003_PRI_OBSERVER   = 'SUN'
         FRAME_1700003_PRI_TARGET     = 'ALPHA'
         FRAME_1700003_PRI_ABCORR     = 'CN+S'
         FRAME_1700003_SEC_AXIS       = 'Y'
         FRAME_1700003_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700003_SEC_OBSERVER   = 'SUN'
         FRAME_1700003_SEC_TARGET     = 'ALPHA'
         FRAME_1700003_SEC_ABCORR     = 'CN+S'
         FRAME_1700003_SEC_FRAME      = 'J2000'


         FRAME_ALPHA_VIEW_XY_XCN      =  1700004
         FRAME_1700004_NAME           = 'ALPHA_VIEW_XY_XCN'
         FRAME_1700004_CLASS          =  5
         FRAME_1700004_CLASS_ID       =  1700004
         FRAME_1700004_CENTER         = 'ALPHA'
         FRAME_1700004_RELATIVE       = 'J2000'
         FRAME_1700004_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700004_FAMILY         = 'TWO-VECTOR'
         FRAME_1700004_PRI_AXIS       = 'X'
         FRAME_1700004_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700004_PRI_OBSERVER   = 'SUN'
         FRAME_1700004_PRI_TARGET     = 'ALPHA'
         FRAME_1700004_PRI_ABCORR     = 'XCN'
         FRAME_1700004_SEC_AXIS       = 'Y'
         FRAME_1700004_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700004_SEC_OBSERVER   = 'SUN'
         FRAME_1700004_SEC_TARGET     = 'ALPHA'
         FRAME_1700004_SEC_ABCORR     = 'XCN'
         FRAME_1700004_SEC_FRAME      = 'J2000'

         FRAME_ALPHA_VIEW_XY_XCNS     =  1700005
         FRAME_1700005_NAME           = 'ALPHA_VIEW_XY_XCNS'
         FRAME_1700005_CLASS          =  5
         FRAME_1700005_CLASS_ID       =  1700005
         FRAME_1700005_CENTER         = 'ALPHA'
         FRAME_1700005_RELATIVE       = 'J2000'
         FRAME_1700005_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700005_FAMILY         = 'TWO-VECTOR'
         FRAME_1700005_PRI_AXIS       = 'X'
         FRAME_1700005_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700005_PRI_OBSERVER   = 'SUN'
         FRAME_1700005_PRI_TARGET     = 'ALPHA'
         FRAME_1700005_PRI_ABCORR     = 'XCN+S'
         FRAME_1700005_SEC_AXIS       = 'Y'
         FRAME_1700005_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700005_SEC_OBSERVER   = 'SUN'
         FRAME_1700005_SEC_TARGET     = 'ALPHA'
         FRAME_1700005_SEC_ABCORR     = 'XCN+S'
         FRAME_1700005_SEC_FRAME      = 'J2000'







         FRAME_ALPHA_VIEW_XZ_CN       =  1700006
         FRAME_1700006_NAME           = 'ALPHA_VIEW_XZ_CN'
         FRAME_1700006_CLASS          =  5
         FRAME_1700006_CLASS_ID       =  1700006
         FRAME_1700006_CENTER         = 'ALPHA'
         FRAME_1700006_RELATIVE       = 'J2000'
         FRAME_1700006_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700006_FAMILY         = 'TWO-VECTOR'
         FRAME_1700006_PRI_AXIS       = 'X'
         FRAME_1700006_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700006_PRI_OBSERVER   = 'SUN'
         FRAME_1700006_PRI_TARGET     = 'ALPHA'
         FRAME_1700006_PRI_ABCORR     = 'CN'
         FRAME_1700006_SEC_AXIS       = 'Z'
         FRAME_1700006_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700006_SEC_OBSERVER   = 'SUN'
         FRAME_1700006_SEC_TARGET     = 'ALPHA'
         FRAME_1700006_SEC_ABCORR     = 'CN'
         FRAME_1700006_SEC_FRAME      = 'J2000'

         FRAME_ALPHA_VIEW_XZ_CNS      =  1700007
         FRAME_1700007_NAME           = 'ALPHA_VIEW_XZ_CNS'
         FRAME_1700007_CLASS          =  5
         FRAME_1700007_CLASS_ID       =  1700007
         FRAME_1700007_CENTER         = 'ALPHA'
         FRAME_1700007_RELATIVE       = 'J2000'
         FRAME_1700007_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700007_FAMILY         = 'TWO-VECTOR'
         FRAME_1700007_PRI_AXIS       = 'X'
         FRAME_1700007_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700007_PRI_OBSERVER   = 'SUN'
         FRAME_1700007_PRI_TARGET     = 'ALPHA'
         FRAME_1700007_PRI_ABCORR     = 'CN+S'
         FRAME_1700007_SEC_AXIS       = 'Z'
         FRAME_1700007_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700007_SEC_OBSERVER   = 'SUN'
         FRAME_1700007_SEC_TARGET     = 'ALPHA'
         FRAME_1700007_SEC_ABCORR     = 'CN+S'
         FRAME_1700007_SEC_FRAME      = 'J2000'


         FRAME_ALPHA_VIEW_XZ_XCN      =  1700005
         FRAME_1700005_NAME           = 'ALPHA_VIEW_XZ_XCN'
         FRAME_1700005_CLASS          =  5
         FRAME_1700005_CLASS_ID       =  1700005
         FRAME_1700005_CENTER         = 'ALPHA'
         FRAME_1700005_RELATIVE       = 'J2000'
         FRAME_1700005_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700005_FAMILY         = 'TWO-VECTOR'
         FRAME_1700005_PRI_AXIS       = 'X'
         FRAME_1700005_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700005_PRI_OBSERVER   = 'SUN'
         FRAME_1700005_PRI_TARGET     = 'ALPHA'
         FRAME_1700005_PRI_ABCORR     = 'XCN'
         FRAME_1700005_SEC_AXIS       = 'Z'
         FRAME_1700005_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700005_SEC_OBSERVER   = 'SUN'
         FRAME_1700005_SEC_TARGET     = 'ALPHA'
         FRAME_1700005_SEC_ABCORR     = 'XCN'
         FRAME_1700005_SEC_FRAME      = 'J2000'

         FRAME_ALPHA_VIEW_XZ_XCNS     =  1700008
         FRAME_1700008_NAME           = 'ALPHA_VIEW_XZ_XCNS'
         FRAME_1700008_CLASS          =  5
         FRAME_1700008_CLASS_ID       =  1700008
         FRAME_1700008_CENTER         = 'ALPHA'
         FRAME_1700008_RELATIVE       = 'J2000'
         FRAME_1700008_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700008_FAMILY         = 'TWO-VECTOR'
         FRAME_1700008_PRI_AXIS       = 'X'
         FRAME_1700008_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700008_PRI_OBSERVER   = 'SUN'
         FRAME_1700008_PRI_TARGET     = 'ALPHA'
         FRAME_1700008_PRI_ABCORR     = 'XCN+S'
         FRAME_1700008_SEC_AXIS       = 'Z'
         FRAME_1700008_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700008_SEC_OBSERVER   = 'SUN'
         FRAME_1700008_SEC_TARGET     = 'ALPHA'
         FRAME_1700008_SEC_ABCORR     = 'XCN+S'
         FRAME_1700008_SEC_FRAME      = 'J2000'



   \begintext

         View frames for body BETA relative to the Sun

            BETA_VIEW_XY      orbital motion of BETA lies in X-Y plane
                              of this frame.

            BETA_VIEW_XZ      orbital motion of BETA lies in X-Z plane
                              of this frame.

   \begindata

         FRAME_BETA_VIEW_XY           =  1700009
         FRAME_1700009_NAME           = 'BETA_VIEW_XY'
         FRAME_1700009_CLASS          =  5
         FRAME_1700009_CLASS_ID       =  1700009
         FRAME_1700009_CENTER         = 'BETA'
         FRAME_1700009_RELATIVE       = 'J2000'
         FRAME_1700009_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700009_FAMILY         = 'TWO-VECTOR'
         FRAME_1700009_PRI_AXIS       = 'X'
         FRAME_1700009_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700009_PRI_OBSERVER   = 'SUN'
         FRAME_1700009_PRI_TARGET     = 'BETA'
         FRAME_1700009_PRI_ABCORR     = 'NONE'
         FRAME_1700009_SEC_AXIS       = 'Y'
         FRAME_1700009_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700009_SEC_OBSERVER   = 'SUN'
         FRAME_1700009_SEC_TARGET     = 'BETA'
         FRAME_1700009_SEC_ABCORR     = 'NONE'
         FRAME_1700009_SEC_FRAME      = 'J2000'


         FRAME_BETA_VIEW_XZ           =  1700010
         FRAME_1700010_NAME           = 'BETA_VIEW_XZ'
         FRAME_1700010_CLASS          =  5
         FRAME_1700010_CLASS_ID       =  1700010
         FRAME_1700010_CENTER         = 'BETA'
         FRAME_1700010_RELATIVE       = 'J2000'
         FRAME_1700010_DEF_STYLE      = 'PARAMETERIZED'
         FRAME_1700010_FAMILY         = 'TWO-VECTOR'
         FRAME_1700010_PRI_AXIS       = 'X'
         FRAME_1700010_PRI_VECTOR_DEF = 'OBSERVER_TARGET_POSITION'
         FRAME_1700010_PRI_OBSERVER   = 'SUN'
         FRAME_1700010_PRI_TARGET     = 'BETA'
         FRAME_1700010_PRI_ABCORR     = 'NONE'
         FRAME_1700010_SEC_AXIS       = 'Z'
         FRAME_1700010_SEC_VECTOR_DEF = 'OBSERVER_TARGET_VELOCITY'
         FRAME_1700010_SEC_OBSERVER   = 'SUN'
         FRAME_1700010_SEC_TARGET     = 'BETA'
         FRAME_1700010_SEC_ABCORR     = 'NONE'
         FRAME_1700010_SEC_FRAME      = 'J2000'

   \begintext

      Radii and GM for the Sun.

   \begindata

         BODY10_RADII            = ( 1000, 1000, 1000 )

         BODY10_GM               =  0.99745290739151156E+09

   \begintext

      GM for ALPHA. This GM, together with a circular
      orbit of radius 1.E5 for body GAMMA, yields a 24 hour period
      for GAMMA.

   \begindata

         BODY1000_GM             =  0.52884968712970233E+07

   \begintext
   </pre>
   */
   public native static synchronized void natpck ( String    file,
                                                   boolean   load,
                                                   boolean   keep  )
      throws SpiceErrorException;



   /**
   Create an SPK file for Nat's solar system.

<pre>
      This SPK file represents a contrived "solar system" containing a
      "Sun" and two planets, ALPHA and BETA. Body ALPHA has a moon
      named GAMMA.

      GAMMA orbits ALPHA in a plane normal to the J2000 X-axis, with
      a period of 24 hours.

      The Sun has zero offset from the solar system barycenter. Both
      planets orbit the Sun with circular two-body motion.  BETA orbits
      closer to the Sun than ALPHA.  When the radii of BETA and ALPHA
      are set appropriately, BETA occults ALPHA as seen from the center
      of the Sun at regular intervals:

         With

           aberration correction NONE

         Occultation starts:        2000 JAN 1 12:00:00 TDB
                                    and recurs every 24 hours

         Occultation ends:          2000 JAN 1 12:10:00 TDB
                                    and recurs every 24 hours

         With

           aberration correction LT

         Occultation starts:        2000 JAN 1 12:00:01 TDB
                                    and recurs every 24 hours

         Occultation ends:          2000 JAN 1 12:10:01 TDB
                                    and recurs every 24 hours

      At the J2000 epoch, the vector from the center of the
      Sun to the center of body ALPHA lies on the +X axis of
      the J2000 frame.

      The default time range covered by the file is approximately

         1899 DEC 31 12:00:00 TDB
         2100 DEC 31 12:00:00 TDB


      The following parameters control the behavior of this
      solar system:

         N1:       Occultation duration, seconds
         N2:       Ratio of angular size of alpha to that of beta
         N3:       Ratio of angular velocity of beta to that of alpha
         N4:       Time between occultations, seconds
         BASELT:   Offset of occultation time with LT correction,
                   seconds.

      If these parameters are changed, the radii of ALPHA and
      BETA must be re-computed.

      Below we derive the constants needed to create the ephemeris
      for the planets, as well as their radii.

      Relationships between angular velocities, orbital radii:


         OMEGA       = N  * OMEGA         (definition)
              beta      3        alpha


         N4 is time between occultations:

         ( OMEGA     - OMEGA     ) * N4  =  2 * Pi
                beta        alpha


         OMEGA                   =  2 * Pi /  ( N4 * ( N3 - 1 ) )
              alpha


                  2                      2                 2
         ACC  =  V  / R          =  OMEGA  * R    =  GM / R
            i     i    i                 i    i            i


                                            3   1/2
         OMEGA                   =  ( GM / R  )
              i                             i

                                                    3/2
         OMEGA     / OMEGA       = ( R      / R    )     =  N
              beta        alpha       alpha    beta          3


                   2/3
         R      = N     *  R
          alpha    3        beta


         OMEGA      =  OMEGA     -  OMEGA       (definition)
              delta         beta         alpha


      Central GM:

                   2
         GM = OMEGA       / R
                   alpha     alpha


      Light time correction---relationship between orbital radius and
      time offset of occultation when light time correction is used:


         THETA   = -LT  * OMEGA
              i       i        i

         GAMMA   = THETA  + OMEGA  * BASELT
              i         i        i

         GAMMA      = GAMMA       =>
              alpha        beta

         OMEGA     ( BASELT - LT     ) = OMEGA    ( BASELT - LT    ) =>
              alpha             alpha         beta             beta


         BASELT - R     /c  =  N * ( BASELT - R    /c ) =>
                   alpha        3              beta


                   2/3
         BASELT - N    * R    /c  =  N * ( BASELT - R    /c ) =>
                   3      beta        3              beta

                              2/3
         ( R    / c ) ( N  - N   )   =  BASELT * ( N  - 1 ) =>
            beta         3    3                     3

                                                     2/3
         R     =  c * BASELT * ( N  - 1 ) / ( N   - N    )
          beta                    3            3     3


      Ratio of angular sizes of Alpha and Beta:

         N2 = ARAD     / ARAD         (definition)
                  alpha      beta


      Occultation duration:

         N1 = duration (definition)


      Angular movement of beta past alpha during occultation, definition
      of angular size of beta:

         PROG = N1 * OMEGA
                          delta

         From start to finish of occultation, beta moves past alpha
         by the angular diameter of alpha plus 2*the angular radius
         of beta:

         PROG      = 2 * ( ARAD      + ARAD    )
                               alpha       beta

                   = 2 * ARAD     ( 1 + N2 )   =>
                             beta

         ARAD      = N1 * OMEGA      / ( 2 * (1 + N2) )
             beta              delta


      Radii of alpha, beta:

         RAD  =  R  * sin ( ARAD )
            i     i             i
</pre>


   */
   public native static synchronized int natspk ( String    file,
                                                  boolean   load )
      throws SpiceErrorException;




   /**
   Call the deprecated CSPICE routine subpt_c.
   */
   public native static synchronized void subpt ( String    method,
                                                  String    target,
                                                  double    et,
                                                  String    abcorr,
                                                  String    obsrvr,
                                                  double[]  spoint,    
                                                  double[]  alt     )
      throws SpiceErrorException;


   /**
   Call the deprecated CSPICE routine subsol_c.
   */
   public native static synchronized void subsol ( String    method,
                                                   String    target,
                                                   double    et,
                                                   String    abcorr,
                                                   String    obsrvr,
                                                   double[]  spoint  )
      throws SpiceErrorException;



   /*
   Return a pseudo-random integer lying in a specified interval.
   */

   /*
   public native static synchronized int rani ( int    lb,
                                                int    ub )
      throws SpiceErrorException;
   */


   /**
   Write a DSK containing an extremely simplified shape model
   representing the nucleus of comet Churymov-Gerasimenko. The model
   is a union of boxes.
   */
   public native static synchronized void t_cg ( int     bodyid,
                                                 int     surfid,
                                                 String  frame,
                                                 String  dsk     )
      throws SpiceErrorException;



   /**
   Write a type 2 DSK containing a tessellated ellipsoid.
   Use default values to simplify the call.
   */
   public native static synchronized void t_elds2z ( int     bodyid,
                                                     int     surfid,
                                                     String  frame,
                                                     int     nlon,
                                                     int     nlat,
                                                     String  dsk     )
      throws SpiceErrorException;




   /**
   Write a type 2 DSK containing a section of a tessellated
   ellipsoid.
   */
   public native static synchronized void t_secds2 ( int         bodyid,
                                                     int         surfid,
                                                     String      frname,
                                                     double      first,
                                                     double      last,
                                                     int         corsys,
                                                     double[]    corpar,
                                                     double[][]  bounds,
                                                     double      a,
                                                     double      b,
                                                     double      c,
                                                     int         nlon,
                                                     int         nlat,
                                                     boolean     makvtl,
                                                     boolean     usepad,
                                                     String      dsk     )
      throws SpiceErrorException;



   /**
   Write a trivial type 2 DSK containing one plate.
   */
   public native static synchronized void t_smldsk ( int         bodyid,
                                                     int         surfid,
                                                     String      frname,
                                                     String      dsk    )
      throws SpiceErrorException;



   /**
   Write a DSK containing a plate model representing a torus.
   */
   public native static synchronized void t_torus ( int         bodyid,
                                                    int         surfid,
                                                    String      frname,
                                                    int         npolyv,
                                                    int         ncross,
                                                    double      r,
                                                    double      rcross,
                                                    double[]    center,
                                                    double[]    normal,
                                                    String      dsk    )
      throws SpiceErrorException;

  
   /**
   Write a type 2 DSK containing vertices and plates passed
   in by the caller.
   */
   public native static synchronized void t_wrtplt ( int         bodyid,
                                                     int         surfid,
                                                     String      frname,
                                                     double      first,
                                                     double      last,
                                                     int         corsys,
                                                     double[]    corpar,
                                                     double[][]  bounds,
                                                     int         nv,
                                                     int         np,
                                                     double[]    usrvrt,
                                                     int[]       usrplt,
                                                     boolean     makvtl,
                                                     String      dsk    )
      throws SpiceErrorException;


   /**
   Write a type 2 DSK containing vertices and plates passed
   in by the caller. Use default values to simplify the call.
   */
   public native static synchronized void t_wrtplz ( int         bodyid,
                                                     int         surfid,
                                                     String      frname,
                                                     int         nv,
                                                     int         np,
                                                     double[]    usrvrt,
                                                     int[]       usrplt, 
                                                     String      dsk    )
      throws SpiceErrorException;


   /**
   Set the title for the next test case and log the success of
   the last test case if it passed and logging of individual
   test case success is enabled.
   */
   public native static synchronized void tcase ( String title )

      throws SpiceErrorException;


   /**
   Close out all testing.
   */
   public native static synchronized void tclose ()

      throws SpiceErrorException;


   /**
   Open a collection of tests.
   */
   public native static synchronized void topen ( String testFamilyName )

      throws SpiceErrorException;



   /**
   This routine handles the initializations needed for making use
   of the SPICE testing utilities.
   */
   public native static synchronized void tsetup ( String   commandLine,
                                                   String   logname,
                                                   String   version    )

      throws SpiceErrorException;


   /**
   This routine produces attitude and angular velocity values
   that should duplicate the values for the test spacecraft
   with ID code -10001.
   */
   public native static synchronized void tstatd ( double       et,
                                                   double[][]   matrix,
                                                   double[]     av      )

      throws SpiceErrorException;


   /**
   This routine creates, optionally loads, and optionally deletes a
   type 3 C-kernel and an associated SCLK kernel.

   <p> Below are comments from the header of tstck3_c:
   <pre>
   This routine creates two files.

      1) A C-kernel for the fictional objects with ID codes -9999,
        -10000, and -10001

      2) A SCLK kernel to be associated with the C-kernel.

   The C-kernel contains a single segment for each of the
   fictional objects.  These segments give continuous attitude
   over the time interval

      from 1980 JAN 1, 00:00:00.000 (ET)
      to   2011 SEP 9, 01:46:40.000 (ET)

   (a span of exactly 1 billion seconds).


   The frames of the objects are

      Object    Frame
      -------   --------
      -9999     Galactic
      -10000    FK5
      -10001    J2000

   All three objects rotate  at a rate of 1 radian per 10 million
   seconds. The axis of rotation changes every 100 million seconds.

   At various epochs the axes of the objects are exactly aligned
   with their associated reference frame.

      Object     Aligned with reference frame at epoch
      ------     -------------------------------------
      -9999      Epoch of the J2000 frame
      -10000     Epoch of J2000
      -10001     Epoch of J2000

   At the moment when the frames are aligned. The are rotating
   around the direction (2, 1, 3) in their associated frames.

   The C-kernel contains 606 attitude instances.

   The attitude and angular velocity produced by the CK software
   should very nearly duplicate the results returned by the test
   routine tstatd_c.

   More specifically suppose we set up the arrays:

      ID[0]     = -9999
      ID[1]     = -10000
      ID[2]     = -10001


      FRAME[0]  = "GALACTIC"
      FRAME[1]  = "FK4"
      FRAME[2]  = "J2000"


   Then the two methods of getting ROT and AV below should
   produce results that agree to nearly roundoff.

      Method 1.

         #include "SpiceUsr.h"
              .
              .
              .
         sce2c_c  ( -9, et, &tick );
         ckgpav_c ( id[i], tick, 0.0, frame[i], rot, av, out, &fnd );

      Method 2.

         #include "SpiceUsr.h"
         #include "tutils_c.h"
              .
              .
              .
         tstatd_c ( et, rot, av );
   </pre>


<p> The SCLK kernel produced by this method contains
the data shown below:
<pre>
    SCLK_KERNEL_ID                = ( @28-OCT-1994        )

    SCLK_DATA_TYPE_9              = ( 1 )

    SCLK01_TIME_SYSTEM_9          = ( 1 )
    SCLK01_N_FIELDS_9             = ( 2 )
    SCLK01_MODULI_9               = ( 1000000000     10000 )
    SCLK01_OFFSETS_9              = ( 0         0 )
    SCLK01_OUTPUT_DELIM_9         = ( 1 )

    SCLK_PARTITION_START_9        = ( 0.0000000000000E+00 )
    SCLK_PARTITION_END_9          = ( 1.00000000E+14      )
    SCLK01_COEFFICIENTS_9         = ( 0.00000000E+00
                                      &#64;01-JAN-1980-00:00:00.000
                                      1                             )
</pre>


   */
   public native static synchronized int  tstck3 ( String    cknm,
                                                   String    sclknm,
                                                   boolean   loadck,
                                                   boolean   loadsc,
                                                   boolean   keepsc  )

      throws SpiceErrorException;


   /**
   This routine creates, loads, and deletes a leapseconds kernel.
   */
   public native static synchronized void tstlsk ()

      throws SpiceErrorException;


   /*
   This routine replaces a test message marker with a string.
   */

   /*
   public native static synchronized void tstmsc ( String  cval )

      throws SpiceErrorException;
   */

   /*
   This routine replaces a test message marker with a double precision value.
   Format is floating point.
   */

   /*
   public native static synchronized void tstmsd ( double  dval  )

      throws SpiceErrorException;
   */

   /*
   This routine replaces a test message marker with a double precision value.
   Format is fixed point.
   */

   /*
   public native static synchronized void tstmsf ( double  dval  )

      throws SpiceErrorException;
   */

   /*
   This routine creates a message template for the test logger
   system.  The message will be printed if an error is detected.
   */

   /*
   public native static synchronized void tstmsg ( String     marker,
                                                   String     message )

      throws SpiceErrorException;
   */


   /*
   This routine replaces a test message marker with an integer value.
   */

   /*
   public native static synchronized void tstmsi ( int  ival  )

      throws SpiceErrorException;
   */

   /*
   This routine replaces a test message marker
   with an cardinal number represented in English.
   */
   /*
   public native static synchronized void tstmst ( int        ival,
                                                   String     marker )

      throws SpiceErrorException;

   */

   /*
   This routine replaces a test message marker with
   an ordinal number represented in English.
   */

   /*
   public native static synchronized void tstmso ( int        ival,
                                                   String     marker )

      throws SpiceErrorException;
   */

   /**
   Create and if appropriate load a test PCK kernel.
   */
   public native static synchronized void tstpck ( String    pck,
                                                   boolean   load,
                                                   boolean   keep   )

      throws SpiceErrorException;


   /**
   This routine creates and optionally loads an SPK kernel.  If the
   file is loaded, the file handle is returned.

   <p> The following description is from the tutils_c version of
   this routine:

   <pre>

    This routine creates an SPK file with ephemeris information
    for the following objects.

         SUN
            MERCURY
               MERCURY_BARYCENTER
            VENUS_BARYCENTER
               VENUS
            EARTH-MOON-BARYCENTER
               EARTH
                  GOLDSTONE_TRACKING_STATION
                  MADRID_TRACKING_STATION
                  CANBERRA_TRACKING_STATION
                  MOON
                     SPACECRAFT_PHOENIX
                     TRANQUILITY_BASE
            MARS_BARYCENTER
               MARS
                  PHOBOS
                     PHOBOS_BASECAMP
                  DEIMOS
            JUPITER_BARYCENTER
               JUPITER
                  IO
                  EUROPA
                  GANYMEDE
                  ISTO
            SATURN_BARYCENTER
               SATURN

                  TITAN
            URANUS_BARYCENTER
               URANUS
                  OBERON
                  ARIEL
                  UMBRIEL
                  TITANIA
                  MIRANDA
            NEPTUNE_BARYCENTER
               NEPTUNE
                  TRITON
                  NEREID
            PLUTO_BARYCENTER
               PLUTO
                  CHARON


   This routine creates a "TOY" solar system model for use
   in testing the SPICE ephemeris system.

   The data in this file are "good" for the epochs

      from 1980 JAN 1, 00:00:00.000 (ET)
      to   2011 SEP 9, 01:46:40.000 (ET)
      (a span of exactly 1 billion seconds).


   If the input file already exists, it is deleted prior to the
   creation of this file.
   </pre>

   */
   public native static synchronized int tstspk ( String    file,
                                                   boolean   load  )

      throws SpiceErrorException;


   /**
   Create and if appropriate load a test NAIF text kernel.
   */
   public native static synchronized void tsttxt ( String    namtxt,
                                                   String[]  txt,
                                                   boolean   load,
                                                   boolean   keep   )

      throws SpiceErrorException;


   /**
   Indicate whether all test cases since set up via t_begin_ have
   passed.
   */
   public native static synchronized boolean tsuccess()

      throws SpiceErrorException;


   /**
   Create a set of triangular plates covering a specified triaxial
   ellipsoid.
   */
   public native static synchronized  void zzellplt ( double   a,
                                                      double   b,
                                                      double   c,
                                                      int      nlon,
                                                      int      nlat,
                                                      int[]    nv,
                                                      double[] vout,
                                                      int[]    np,
                                                      int[]    pout )    
      throws SpiceErrorException;



   /**
   Create a plate set representing a sphere centered at the origin.
   The radius of the sphere is specified by the caller.
   */
   public native static synchronized  void zzpsball ( double   r,
                                                      int      nlon,
                                                      int      nlat,
                                                      int[]    nv,
                                                      double[] vout,
                                                      int[]    np,
                                                      int[]    pout )
      throws SpiceErrorException;


   /**
   Create a plate set representing a box centered at the origin.
   The dimensions of the box are specified by the caller.
   */
   public native static synchronized  void zzpsbox  ( double   a,
                                                      double   b,
                                                      double   c,
                                                      int[]    nv,
                                                      double[] vout,
                                                      int[]    np,
                                                      int[]    pout )
      throws SpiceErrorException;


   /**
   Create a plate set with a polygonal boundary in the X-Y plane.
   The normal vectors of the plates point in the +Z direction.

   The input vertices are required to be ordered in the positive
   sense about the +Z axis.
   */
   public native static synchronized  void zzpspoly ( int         n,
                                                      double[]    vrtces,
                                                      int[]       nv,
                                                      double[]    vout,
                                                      int[]       np,
                                                      int[]       pout )
      throws SpiceErrorException;



   /**
   Scale a plate set: multiply the vertices of a plate set by a
   specified factor.

   The cardinality of v1 is assumed to be v1.length.
   */
   public native static synchronized  void zzpsscal ( double      scale,
                                                      double[]    v1,
                                                      int[]       nv,
                                                      double[]    vout  )
      throws SpiceErrorException;


   /**
   Compute the union of two plate sets.

   The cardinalities of the input arrays 

      v1
      p1
      v2
      p2

   are assumed to be the lengths of the respective arrays.

   */
   public native static synchronized  void zzpsun ( double[]      v1,
                                                    int[]         p1,
                                                    double[]      v2,
                                                    int[]         p2,
                                                    int[]         nv,
                                                    double[]      vout,
                                                    int[]         np,
                                                    int[]         pout )
      throws SpiceErrorException;


   /**
   Apply a linear transformation to a plate set: left-multiply
   the vertices of a plate set by a specified 3x3 matrix.

   The cardinality of v1 is assumed to be v1.length.
   */
   public native static synchronized  void zzpsxfrm ( double[]      v1,
                                                      double[][]    xform,
                                                      int[]         nv,
                                                      double[]      vout )
      throws SpiceErrorException;


   /**
   Apply a translation to a plate set: add a specified offset
   vector to the vertices of a plate set.

   The cardinality of v1 is assumed to be v1.length.
   */
   public native static synchronized  void zzpsxlat ( double[]      v1,
                                                      double[]      offset,
                                                      int[]         nv,
                                                      double[]      vout  )
      throws SpiceErrorException;




   /**
   Generate a type 2 DSK for a specified body. The DSK surface
   approximates that of the reference ellipsoid of the body.
   */
   public native static synchronized  int zztboddsk ( String    dsk,
                                                      String    body,
                                                      String    fixref,
                                                      boolean   load   )
      throws SpiceErrorException;


}
