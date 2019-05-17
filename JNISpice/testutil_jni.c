/*

-Procedure testutil_jni ( JNISpice test utility bridge )

-Abstract

   This file contains bride code supporting native methods
   of the JNITestutils class.

-Disclaimer

   THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE
   CALIFORNIA INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S.
   GOVERNMENT CONTRACT WITH THE NATIONAL AERONAUTICS AND SPACE
   ADMINISTRATION (NASA). THE SOFTWARE IS TECHNOLOGY AND SOFTWARE
   PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS AND IS PROVIDED "AS-IS"
   TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, INCLUDING ANY
   WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR A
   PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC
   SECTIONS 2312-2313) OR FOR ANY PURPOSE WHATSOEVER, FOR THE
   SOFTWARE AND RELATED MATERIALS, HOWEVER USED.

   IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA
   BE LIABLE FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT
   LIMITED TO, INCIDENTAL OR CONSEQUENTIAL DAMAGES OF ANY KIND,
   INCLUDING ECONOMIC DAMAGE OR INJURY TO PROPERTY AND LOST PROFITS,
   REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE ADVISED, HAVE
   REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.

   RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF
   THE SOFTWARE AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY
   CALTECH AND NASA FOR ALL THIRD-PARTY CLAIMS RESULTING FROM THE
   ACTIONS OF RECIPIENT IN THE USE OF THE SOFTWARE.

-Required_Reading

   JNISpice

-Keywords

   TEST
   UTILITIES

*/


/*

-Brief_I/O

   See Particulars.

-Detailed_Input

   See Particulars.

-Detailed_Output

   See Particulars.

-Parameters

   See the header files

      cspice_jni.h
      tutils_c.h

   for definitions of parameters and for prototypes of 
   utility functions used in this routine. 

-Exceptions

   The routines in this file use several exception handling 
   techniques:

      - SPICE exceptions 

           There are two macros, 

              SPICE_EXC
              SPICE_EXC_VAL

           which, respectively, are used by void and non-void
           functions. Each of these throws a 

              SpiceErrorException

           when a SPICE error is detected.

     - JNI exceptions

           There are two macros, 

              JNI_EXC
              JNI_EXC_VAL

           which, respectively, are used by void and non-void
           functions. Each of these calls the JNI function

              ExceptionOccurred

           to test whether a JNI function has thrown an exception.
           Both macros execute a return if an exception has been
           thrown.               

-Files

   See Particulars.

-Particulars

   This file contains implementations of native methods of the
   
      JNITestutils

   class. These methods provide functionality analogous to that
   provided by the Fortran 

      testutil 

   library. This set of routines provides many utilities for generating
   test data and test kernels. It also supports TSPICE-style routines
   for checking results against expected values, including

      JNITestutils.chcksd
      JNITestutils.chckad
      JNITestutils.chckai
      JNITestutils.chcksi
      JNITestutils.chcksc
      JNITestutils.chcksl

   Note that error handling is tested by a subsystem implemented in
   Java. This subsystem checks that expected exceptions are thrown,
   and that unexpected exceptions are not thrown. Consequently there 
   is no need for an interface for 

      chckxc_c

   Documentation for the JNITestutils methods is primarily provided
   by the headers of the corresponding routines in the CSPICE test
   utility library

      tutils_c

   There are some differences in calling sequences; for example,
   some tutils_c routines that use SpiceCell arguments have 
   counterparts in JNITestutils that use data arrays and separate
   cardinality arguments. Also, some multi-dimensional array
   arguments in tutils_c routines are replaced by one-dimensional
   arrays in these routines' JNITestutils counterparts.

   Consult the Java source file

      JNITestutils.java

   for the declarations of methods of the JNITestutils class.
   

-Examples

   See the routine 

      cspice_jni.c

   for usage examples.

-Restrictions

   The routines in this file should be considered "private." They
   are intended for use only by JNISpice, not by user applications.

-Literature_References

   None.

-Author_and_Institution

   N.J. Bachman  (JPL)

-Version

   -JNISpice Version 3.0.0, 27-FEB-2017 (NJB)
       
       Added code for APIs

          JNITestutils.natdsk
          JNITestutils.t_cg
          JNITestutils.t_el2dsz
          JNITestutils.t_secds2
          JNITestutils.t_smldsk
          JNITestutils.t_torus
          JNITestutils.t_wrtplt
          JNITestutils.t_wrtplz
          JNITestutils.zzellplt
          JNITestutils.zzpsball
          JNITestutils.zzpsbox
          JNITestutils.zzpspoly
          JNITestutils.zzpsscal
          JNITestutils.zzpsun
          JNITestutils.zzpsxfrm
          JNITestutils.zzpsxlat


   -JNISpice Version 2.0.0, 08-JUN-2014 (NJB)

       Added code for APIs

          JNITestutils.illum
          JNITestutils.subpt
          JNITestutils.subsol
          JNITestutils.zzt_boddsk

       Updated comments to correct association of 
       APIs with products. For example, "CSPICE"
       was changed to "tutils_c" in many instances.

   -JNISpice Version 1.0.0, 17-MAY-2010 (NJB) 

      Updated to support interface change of tsetup_c.

   -JNISpice Version 1.0.0, 04-DEC-2009 (NJB)

-Index_Entries

   TBD

-&
*/

#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include "spice_basic_CSPICE.h"
#include "spice_testutils_JNITestutils.h"
#include "SpiceUsr.h"
#include "SpiceZfc.h"
#include "SpiceZim.h"
#include "zzalloc.h"
#include "tutils_c.h"
#include "cspice_jni.h"



/* 
Wrapper for tutils_c function chckad_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_testutils_JNITestutils_chckad
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jdoubleArray       J_array,
   jstring            J_comp,
   jdoubleArray       J_exp,
   jdouble            J_tol )
{
   /*
   Local variables 
   */
   SpiceBoolean            ok  =  0;

   static SpiceChar      * caller = "JNITestutils.chckad";
   SpiceChar             * namePtr;
   SpiceChar             * compPtr;

   SpiceDouble           * arrayPtr;
   SpiceDouble           * expPtr;

   SpiceInt                arrayLen;
   SpiceInt                expLen;
   SpiceInt                nameLen;
   SpiceInt                compLen;


   /*
   Store input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name, &nameLen, &namePtr );
   getVarInputString_jni ( env, J_comp, &compLen, &compPtr );

   /*
   Store input d.p. arrays in dynamically allocated memory. 
   */
   getVecGD_jni ( env, J_array, &arrayLen, &arrayPtr );
   getVecGD_jni ( env, J_exp,   &expLen,   &expPtr   );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC_VAL( env, ((jboolean)ok) );

   chckad_c ( namePtr, 
              arrayPtr, 
              compPtr, 
              expPtr, 
              arrayLen, 
              (SpiceDouble) J_tol, 
              &ok                    );

   /*
   Free all dynamically allocated items. 
   */
   freeVarInputString_jni ( env, J_name,  namePtr  );
   freeVarInputString_jni ( env, J_comp,  compPtr  );
   freeVecGD_jni          ( env, J_array, arrayPtr );
   freeVecGD_jni          ( env, J_exp,   expPtr   );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)ok) );

   /*
   Normal return. 
   */
   return ( (jboolean)ok );
}



/* 
Wrapper for tutils_c function chckai_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_testutils_JNITestutils_chckai
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jintArray          J_array,
   jstring            J_comp,
   jintArray          J_exp   )
{
   /*
   Local variables 
   */
   SpiceBoolean            ok  =  0;

   static SpiceChar      * caller = "JNITestutils.chckai";
   SpiceChar             * compPtr;   
   SpiceChar             * namePtr;


   SpiceInt              * arrayPtr;
   SpiceInt              * expPtr;

   SpiceInt                arrayLen;
   SpiceInt                expLen;
   SpiceInt                nameLen;
   SpiceInt                compLen;


   /*
   Store input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name, &nameLen, &namePtr );
   getVarInputString_jni ( env, J_comp, &compLen, &compPtr );

   /*
   Store input integer arrays in dynamically allocated memory. 
   */
   getVecGI_jni ( env, J_array, &arrayLen, &arrayPtr );
   getVecGI_jni ( env, J_exp,   &expLen,   &expPtr   );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC_VAL( env, ((jboolean)ok) );


   chckai_c ( namePtr, 
              arrayPtr, 
              compPtr, 
              expPtr, 
              arrayLen, 
              &ok                );
 
   /*
   Free all dynamically allocated items. 
   */
   freeVarInputString_jni ( env, J_name,  namePtr  );
   freeVarInputString_jni ( env, J_comp,  compPtr  );
   freeVecGI_jni          ( env, J_array, arrayPtr );
   freeVecGI_jni          ( env, J_exp,   expPtr   );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)ok) );

   /*
   Normal return. 
   */
   return ( (jboolean)ok );
}



/* 
Wrapper for tutils_c function chcksc_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_testutils_JNITestutils_chcksc
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jstring            J_val,
   jstring            J_comp,
   jstring            J_exp   )
{
   /*
   Local variables
   */
   SpiceBoolean            ok  =  0;

   static SpiceChar      * caller = "JNITestutils.chcksc";
   SpiceChar             * compPtr;
   SpiceChar             * expPtr;
   SpiceChar             * namePtr;
   SpiceChar             * valPtr;

   SpiceInt                compLen;
   SpiceInt                expLen;
   SpiceInt                nameLen;
   SpiceInt                valLen;
 

   /*
   Store input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name, &nameLen, &namePtr );
   getVarInputString_jni ( env, J_val,  &valLen,  &valPtr  );
   getVarInputString_jni ( env, J_comp, &compLen, &compPtr );
   getVarInputString_jni ( env, J_exp,  &expLen,  &expPtr  );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC_VAL( env, ((jboolean)ok) );

 

   chcksc_c ( namePtr, valPtr, compPtr, expPtr, &ok );
 
   /*
   Free strings. 
   */
   freeVarInputString_jni ( env, J_name, namePtr );
   freeVarInputString_jni ( env, J_val,  valPtr  );
   freeVarInputString_jni ( env, J_comp, compPtr );
   freeVarInputString_jni ( env, J_exp,  expPtr  );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)ok) );

   /*
   Normal return. 
   */
   return ( (jboolean)ok );
}


/* 
Wrapper for tutils_c function chcksd_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_testutils_JNITestutils_chcksd
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jdouble            J_val,
   jstring            J_comp,
   jdouble            J_exp,
   jdouble            J_tol )
{
   /*
   Local variables 
   */
   SpiceBoolean            ok  =  0;

   static SpiceChar      * caller = "JNITestutils.chcksd";
   SpiceChar             * namePtr;
   SpiceChar             * compPtr;

   SpiceInt                nameLen;
   SpiceInt                compLen;


   /*
   Store input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name, &nameLen, &namePtr );
   getVarInputString_jni ( env, J_comp, &compLen, &compPtr );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC_VAL( env, ((jboolean)ok) );


   chcksd_c ( namePtr, 
              (SpiceDouble)J_val, 
              compPtr, 
              (SpiceDouble)J_exp, 
              (SpiceDouble)J_tol, 
              &ok               );

   /*
   Free strings. 
   */
   freeVarInputString_jni ( env, J_name, namePtr );
   freeVarInputString_jni ( env, J_comp, compPtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)ok) );

   /*
   Normal return. 
   */
   return ( (jboolean)ok );
}





/* 
Wrapper for tutils_c function chcksi_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_testutils_JNITestutils_chcksi
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jint               J_val,
   jstring            J_comp,
   jint               J_exp,
   jint               J_tol )
{
   /*
   Local variables 
   */
   SpiceBoolean            ok  =  0;

   static SpiceChar      * caller = "JNITestutils.chcksi";
   SpiceChar             * namePtr;
   SpiceChar             * compPtr;

   SpiceInt                nameLen;
   SpiceInt                compLen;


   /*
   Store input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name, &nameLen, &namePtr );
   getVarInputString_jni ( env, J_comp, &compLen, &compPtr );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC_VAL( env, ((jboolean)ok) );


   chcksi_c ( namePtr, 
              (SpiceInt)J_val, 
              compPtr, 
              (SpiceInt)J_exp, 
              (SpiceInt)J_tol, 
              &ok               );

   /*
   Free strings. 
   */
   freeVarInputString_jni ( env, J_name, namePtr );
   freeVarInputString_jni ( env, J_comp, compPtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)ok) );

   /*
   Normal return. 
   */
   return ( (jboolean)ok );
}



/* 
Wrapper for tutils_c function chcksl_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_testutils_JNITestutils_chcksl
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jboolean           J_val,
   jboolean           J_exp   )
{
   /*
   Local variables
   */
   SpiceBoolean            ok     =  0;

   static SpiceChar      * caller = "JNITestutils.chcksl";

   SpiceChar             * namePtr;
   SpiceInt                nameLen;


   /*
   Store the input string in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name,  &nameLen,  &namePtr );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC_VAL( env, (jboolean)ok );

  
   chcksl_c ( namePtr, 
              (SpiceBoolean)J_val, 
              (SpiceBoolean)J_exp, 
              &ok                 );

   /*
   Free the name string.
   */
   freeVarInputString_jni ( env, J_name,  namePtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller,  ((jboolean)ok) );
 
   /*
   Normal return. 
   */
   return ( (jboolean)ok );
}



/* 
Wrapper for CSPICE function illum_c 

Note that this is a deprecated routine in CSPICE. Hence its
placement in the spice.testutils package.
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_illum
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_target, 
   jdouble            J_et,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jdoubleArray       J_spoint, 
   jdoubleArray       J_angles  )
{
   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller  = "JNITestutils.illum";
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];
   
   SpiceDouble             angles  [ 3 ];
   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];
  

   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr  );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr  );
   getFixedInputString_jni ( env, J_target, BDNMLN, target  );
   getVec3D_jni            ( env, J_spoint,         spoint  );

   JNI_EXC( env );

   et = (SpiceDouble)J_et;

   
   illum_c ( target,  et,     abcorr,   obsrvr,  
             spoint,  angles, angles+1, angles+2 );

   /*
   Handle any SPICE exception that may have occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Set the values of our output array.
   */
   updateVec3D_jni ( env, angles, J_angles );
}





/* 
Wrapper for tutils_c function t_natdsk
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_natdsk
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_dsk,
   jstring            J_aframe,
   jint               J_anlon,
   jint               J_anlat,
   jstring            J_bframe,
   jint               J_bnlon,
   jint               J_bnlat  )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.natdsk";
   SpiceChar             * dsk;
   SpiceChar             * aframe;
   SpiceChar             * bframe;

   SpiceInt                afrNameLen;
   SpiceInt                anlat;
   SpiceInt                anlon;
   SpiceInt                bfrNameLen;
   SpiceInt                bnlat;
   SpiceInt                bnlon;
   SpiceInt                dskNameLen;

   /*
   Fetch local copies of scalar inputs. 
   */
   anlat  = (SpiceInt) J_anlat;
   anlon  = (SpiceInt) J_anlon;
   bnlat  = (SpiceInt) J_bnlat;
   bnlon  = (SpiceInt) J_bnlon;

   /*
   Store the input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_aframe, &afrNameLen, &aframe );
   getVarInputString_jni ( env, J_bframe, &bfrNameLen, &bframe );
   getVarInputString_jni ( env, J_dsk,    &dskNameLen, &dsk    );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC( env );


   /*
   Create the DSK. 
   */
   natdsk_c ( dsk, aframe, anlon, anlat, bframe, bnlon, bnlat );


   /*
   Free the frame and DSK strings.
   */
   freeVarInputString_jni ( env, J_aframe, aframe );
   freeVarInputString_jni ( env, J_bframe, bframe );
   freeVarInputString_jni ( env, J_dsk,    dsk    );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}





/* 
Wrapper for tutils_c function natik_c 
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_natik
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_IK,
   jstring            J_SPK,
   jstring            J_PCK,
   jboolean           J_loadIK,
   jboolean           J_keepIK  )
{
   /*
   Local variables 
   */
   logical                 loadIK;
   logical                 keepIK;


   static SpiceChar      * caller = "JNITestutils.natik";
   static SpiceChar        IK  [ FNAMLN ];
   static SpiceChar        SPK [ FNAMLN ];
   static SpiceChar        PCK [ FNAMLN ];

   getFixedInputString_jni ( env, J_IK,  FNAMLN, IK  );
   getFixedInputString_jni ( env, J_SPK, FNAMLN, SPK );
   getFixedInputString_jni ( env, J_PCK, FNAMLN, PCK );

   JNI_EXC( env );


   loadIK =  (integer) J_loadIK;
   keepIK =  (integer) J_keepIK;

   natik_ ( (char     *) IK,
            (char     *) SPK,
            (char     *) PCK,
            (logical  *) &loadIK,
            (logical  *) &keepIK,
            (ftnlen    ) strlen(IK),
            (ftnlen    ) strlen(SPK),
            (ftnlen    ) strlen(PCK)  );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for tutils_c function natpck_c 
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_natpck
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_file,
   jboolean           J_load,
   jboolean           J_keep  )
{
   /*
   Local variables 
   */
   logical                 load;
   logical                 keep;


   static SpiceChar      * caller = "JNITestutils.natpck";
   static SpiceChar        file [ FNAMLN ];

   getFixedInputString_jni ( env, J_file, FNAMLN, file );

   JNI_EXC( env );


   load   =  (integer) J_load;
   keep   =  (integer) J_keep;

   natpck_ ( (char     *) file,
             (logical  *) &load,
             (logical  *) &keep,
             (ftnlen    ) strlen(file) );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for tutils_c function natspk_c 
*/
JNIEXPORT jint JNICALL Java_spice_testutils_JNITestutils_natspk
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_file,
   jboolean           J_load )
{
   /*
   Local variables 
   */
   logical                 load;
   integer                 handle = 0;

   static SpiceChar      * caller = "JNITestutils.natspk";
   static SpiceChar        file [ FNAMLN ];

   getFixedInputString_jni ( env, J_file, FNAMLN, file );

   JNI_EXC_VAL( env, ((jint) handle) );


   load   =  (integer) J_load;

   natspk_ ( (char     *) file,
             (logical  *) &load,
             (integer  *) &handle,
             (ftnlen    ) strlen(file) );

   SPICE_EXC_VAL( env, caller, ((jint)handle) );


   return ( (jint)handle );
}



/* 
Wrapper for CSPICE function subpt_c

Note that this is a deprecated routine in CSPICE. Hence its
placement in the spice.testutils package.
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_subpt
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_method, 
   jstring            J_target, 
   jdouble            J_et,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jdoubleArray       J_spoint, 
   jdoubleArray       J_alt     )
{

   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller  = "JNITestutils.subpt";
   static SpiceChar      * method;
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             alt;
   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];
  
   SpiceInt                methodLen;


   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target );
   getVarInputString_jni   ( env, J_method, &methodLen, &method );

   et = (SpiceDouble)J_et;

   JNI_EXC( env );


   subpt_c ( method,  target,  et,  
             abcorr,  obsrvr,  spoint,  &alt);


   /*
   Regardless of whether a SPICE error occurred, free the 
   dynamically allocated memory here. 
   */
   freeVarInputString_jni ( env, J_method, method );

   
   /*
   Handle any SPICE exception that may have occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Set the values of our output arrays. 
   */
   updateVec3D_jni ( env,    spoint,  J_spoint );
   updateVecGD_jni ( env, 1, &alt,    J_alt    );
}




/* 
Wrapper for CSPICE function subsol_c

Note that this is a deprecated routine in CSPICE. Hence its
placement in the spice.testutils package.
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_subsol
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_method, 
   jstring            J_target, 
   jdouble            J_et,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jdoubleArray       J_spoint   )
{

   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller  = "JNITestutils.subsol";
   static SpiceChar      * method;
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];
  
   SpiceInt                methodLen;


   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target );
   getVarInputString_jni   ( env, J_method, &methodLen, &method );

   et = (SpiceDouble)J_et;

   JNI_EXC( env );


   subsol_c ( method,  target,  et,  
              abcorr,  obsrvr,  spoint );


   /*
   Regardless of whether a SPICE error occurred, free the 
   dynamically allocated memory here. 
   */
   freeVarInputString_jni ( env, J_method, method );

   
   /*
   Handle any SPICE exception that may have occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Set the values of our output array. 
   */
   updateVec3D_jni ( env,    spoint,  J_spoint );
}




/* 
Wrapper for tutils_c function t_cg
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_t_1cg
(  JNIEnv           * env, 
   jclass             J_class,
   jint               J_bodyid,
   jint               J_surfid,   
   jstring            J_frame,
   jstring            J_dsk     )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.t_cg";
   SpiceChar             * dsk;
   SpiceChar             * frame;

   SpiceInt                bodyid;   
   SpiceInt                dskNameLen;
   SpiceInt                frNameLen;
   SpiceInt                surfid;


   /*
   Fetch local copies of scalar inputs. 
   */
   bodyid = (SpiceInt) J_bodyid;
   surfid = (SpiceInt) J_surfid;

   /*
   Store the input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_frame,  &frNameLen,  &frame );
   getVarInputString_jni ( env, J_dsk,    &dskNameLen, &dsk   );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC( env );


   /*
   Create the DSK. 
   */
   t_cg_c ( bodyid, surfid, frame, dsk );


   /*
   Free the frame and DSK strings.
   */
   freeVarInputString_jni ( env, J_frame, frame );
   freeVarInputString_jni ( env, J_dsk,   dsk   );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}






/* 
Wrapper for tutils_c function t_elds2z
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_t_1elds2z
(  JNIEnv           * env, 
   jclass             J_class,
   jint               J_bodyid,
   jint               J_surfid,   
   jstring            J_frame,
   jint               J_nlon,
   jint               J_nlat,
   jstring            J_dsk     )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.t_el2dsz";
   SpiceChar             * dsk;
   SpiceChar             * frame;

   SpiceInt                bodyid;   
   SpiceInt                dskNameLen;
   SpiceInt                frNameLen;
   SpiceInt                nlat;
   SpiceInt                nlon;
   SpiceInt                surfid;


   /*
   Fetch local copies of scalar inputs. 
   */
   bodyid = (SpiceInt) J_bodyid;
   surfid = (SpiceInt) J_surfid;
   nlon   = (SpiceInt) J_nlon;
   nlat   = (SpiceInt) J_nlat;

   /*
   Store the input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_frame,  &frNameLen,  &frame );
   getVarInputString_jni ( env, J_dsk,    &dskNameLen, &dsk   );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC( env );


   /*
   Create the DSK. 
   */
   t_elds2z_c ( bodyid, surfid, frame, nlon, nlat, dsk );


   /*
   Free the frame and DSK strings.
   */
   freeVarInputString_jni ( env, J_frame, frame );
   freeVarInputString_jni ( env, J_dsk,   dsk   );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}






/* 
Wrapper for tutils_c function t_secds2
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_t_1secds2
(  JNIEnv           * env, 
   jclass             J_class,
   jint               J_bodyid,
   jint               J_surfid,   
   jstring            J_frame,
   jdouble            J_first,
   jdouble            J_last,
   jint               J_corsys,
   jdoubleArray       J_corpar,
   jobjectArray       J_bounds,
   jdouble            J_a,
   jdouble            J_b,
   jdouble            J_c,
   jint               J_nlon,
   jint               J_nlat,
   jboolean           J_makvtl,
   jboolean           J_usepad,
   jstring            J_dsk     )

{
   /*
   Local variables 
   */
   SpiceBoolean            makvtl;
   SpiceBoolean            usepad;

   static SpiceChar      * caller = "JNITestutils.t_secds2";
   SpiceChar             * dsk;
   SpiceChar             * frame;

   SpiceDouble             a;
   SpiceDouble             b;
   ConstSpiceDouble    ( * bounds )[2];
   SpiceDouble             c;
   SpiceDouble             corpar [ SPICE_DSK_NSYPAR ];
   SpiceDouble             first;
   SpiceDouble             last;

   SpiceInt                bodyid;
   SpiceInt                corsys;
   SpiceInt                dskNameLen;
   SpiceInt                frNameLen;
   SpiceInt                ncols;
   SpiceInt                nlat;
   SpiceInt                nlon;
   SpiceInt                nrows;
   SpiceInt                surfid;


   /*
   Fetch local copies of scalar inputs. 
   */
   first  = (SpiceDouble)  J_first;
   last   = (SpiceDouble)  J_last;

   makvtl = (SpiceBoolean) J_makvtl;
   usepad = (SpiceBoolean) J_usepad;

   a      = (SpiceDouble) J_a;
   b      = (SpiceDouble) J_b;
   c      = (SpiceDouble) J_c;

   bodyid = (SpiceInt) J_bodyid;
   corsys = (SpiceInt) J_corsys;
   surfid = (SpiceInt) J_surfid;
   nlon   = (SpiceInt) J_nlon;
   nlat   = (SpiceInt) J_nlat;

   /*
   Store the input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_frame,  &frNameLen,  &frame );
   getVarInputString_jni ( env, J_dsk,    &dskNameLen, &dsk   );

   /*
   Fetch fixed-size input arrays. 
   */
   getVecFixedD_jni ( env, J_corpar, SPICE_DSK_NSYPAR, corpar );
  
   getMatGD_jni ( env, J_bounds, &nrows, &ncols, (SpiceDouble **)&bounds );

   /*
   Check the returned matrix dimensions.
   */
   if (  ( nrows != 2 ) || ( ncols != 2 )  )
   {
      setmsg_c ( "Input matrix must be 2x2 but had "
                 "dimensions #x#."                   );
      errint_c ( "#", nrows                          );
      errint_c ( "#", ncols                          );
      sigerr_c ( "SPICE(BADDIMENSIONS)"              );
   }

   /*
   Handle a JNI exception, if one occurred.
   */
   JNI_EXC( env );

   if ( !failed_c() )
   {
      /*
      Create the DSK. 
      */
      t_secds2_c ( bodyid, surfid, frame,  first, 
                   last,  corsys,  corpar, bounds,
                   a,      b,      c,      nlon, 
                   nlat,   makvtl, usepad, dsk    );
   }

   /*
   Free the frame and DSK strings.
   */
   freeVarInputString_jni ( env, J_frame, frame );
   freeVarInputString_jni ( env, J_dsk,   dsk   );

   /*
   Free the dynamically allocated bounds array. 
   */
   free_SpiceMemory ( (void *)bounds );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}




/* 
Wrapper for tutils_c function t_smldsk
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_t_1smldsk
(  JNIEnv           * env, 
   jclass             J_class,
   jint               J_bodyid,
   jint               J_surfid,   
   jstring            J_frame,
   jstring            J_dsk     )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.t_smldsk";
   SpiceChar             * dsk;
   SpiceChar             * frame;

   SpiceInt                bodyid;   
   SpiceInt                dskNameLen;
   SpiceInt                frNameLen;
   SpiceInt                surfid;


   /*
   Fetch local copies of scalar inputs. 
   */
   bodyid = (SpiceInt) J_bodyid;
   surfid = (SpiceInt) J_surfid;

   /*
   Store the input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_frame,  &frNameLen,  &frame );
   getVarInputString_jni ( env, J_dsk,    &dskNameLen, &dsk   );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC( env );


   /*
   Create the DSK. 
   */
   t_smldsk_c ( bodyid, surfid, frame, dsk );


   /*
   Free the frame and DSK strings.
   */
   freeVarInputString_jni ( env, J_frame, frame );
   freeVarInputString_jni ( env, J_dsk,   dsk   );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}






/* 
Wrapper for tutils_c function t_torus
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_t_1torus
(  JNIEnv           * env, 
   jclass             J_class,
   jint               J_bodyid,
   jint               J_surfid,   
   jstring            J_frame,
   jint               J_npolyv,
   jint               J_ncross,
   jdouble            J_r,
   jdouble            J_rcross,
   jdoubleArray       J_center,
   jdoubleArray       J_normal,
   jstring            J_dsk     )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.t_torus";
   SpiceChar             * dsk;
   SpiceChar             * frame;

   SpiceDouble             center [3];
   SpiceDouble             normal [3];
   SpiceDouble             r;
   SpiceDouble             rcross;

   SpiceInt                bodyid;   
   SpiceInt                dskNameLen;
   SpiceInt                frNameLen;
   SpiceInt                ncross;
   SpiceInt                npolyv;
   SpiceInt                surfid;


   /*
   Fetch local copies of scalar inputs. 
   */
   r      = (SpiceDouble) J_r;
   rcross = (SpiceDouble) J_rcross;

   bodyid = (SpiceInt) J_bodyid;
   ncross = (SpiceInt) J_ncross;
   npolyv = (SpiceInt) J_npolyv;
   surfid = (SpiceInt) J_surfid;

   /*
   Fetch local copies of input vectors. 
   */
   getVec3D_jni ( env, J_center, center );
   getVec3D_jni ( env, J_normal, normal );

   /*
   Store the input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_frame,  &frNameLen,  &frame );
   getVarInputString_jni ( env, J_dsk,    &dskNameLen, &dsk   );
    
   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC( env );

   /*
   Create the DSK. 
   */
   t_torus_c ( bodyid, surfid, frame,  npolyv, ncross, 
               r,      rcross, center, normal, dsk     );

   /*
   Free the frame and DSK strings.
   */
   freeVarInputString_jni ( env, J_frame, frame );
   freeVarInputString_jni ( env, J_dsk,   dsk   );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}





/* 
Wrapper for tutils_c function t_wrtplt
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_t_1wrtplt
(  JNIEnv           * env, 
   jclass             J_class,
   jint               J_bodyid,
   jint               J_surfid,   
   jstring            J_frame,
   jdouble            J_first,
   jdouble            J_last,
   jint               J_corsys,
   jdoubleArray       J_corpar,
   jobjectArray       J_bounds,
   jint               J_nv,
   jint               J_np,
   jdoubleArray       J_usrvrt,
   jintArray          J_usrplt,
   jboolean           J_makvtl,
   jstring            J_dsk     )

{
   /*
   Local variables 
   */
   SpiceBoolean            makvtl;

   static SpiceChar      * caller = "JNITestutils.t_wrtplt";
   SpiceChar             * dsk;
   SpiceChar             * frame;

   ConstSpiceDouble    ( * bounds )[2];
   SpiceDouble             corpar [ SPICE_DSK_NSYPAR ];
   SpiceDouble             first;
   SpiceDouble             last;
   SpiceDouble           * usrvrt;

   SpiceInt                bodyid;   
   SpiceInt                corsys;
   SpiceInt                dskNameLen;
   SpiceInt                frNameLen;
   SpiceInt                ncols;
   SpiceInt                np;
   SpiceInt                nrows;
   SpiceInt                nv;
   SpiceInt                plateArrayLen;
   SpiceInt                surfid;
   SpiceInt              * usrplt;
   SpiceInt                vertArrayLen;



   /*
   Fetch local copies of scalar inputs. 
   */
   makvtl = (SpiceBoolean) J_makvtl;

   first  = (SpiceDouble) J_first;
   last   = (SpiceDouble) J_last;

   bodyid = (SpiceInt) J_bodyid;
   corsys = (SpiceInt) J_corsys;
   surfid = (SpiceInt) J_surfid;
   nv     = (SpiceInt) J_nv;
   np     = (SpiceInt) J_np;


   /*
   Store the input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_frame,  &frNameLen,  &frame );
   getVarInputString_jni ( env, J_dsk,    &dskNameLen, &dsk   );

   /*
   Fetch fixed-size input arrays. 
   */
   getVecFixedD_jni ( env, J_corpar, SPICE_DSK_NSYPAR, corpar );
  
   getMatGD_jni ( env, J_bounds, &nrows, &ncols, (SpiceDouble **)&bounds );

   /*
   Check the returned matrix dimensions.
   */
   if (  ( nrows != 2 ) || ( ncols != 2 )  )
   {
      setmsg_c ( "Input matrix must be 2x2 but had "
                 "dimensions #x#."                   );
      errint_c ( "#", nrows                          );
      errint_c ( "#", ncols                          );
      sigerr_c ( "SPICE(BADDIMENSIONS)"              );
   }


   /*
   Get dynamically allocated, local copies of the input arrays.
   */
   getVecGD_jni ( env, J_usrvrt, &vertArrayLen,  &usrvrt );
   getVecGI_jni ( env, J_usrplt, &plateArrayLen, &usrplt );

   /*
   Handle a JNI exception, if one occurred.
   */
   JNI_EXC( env );


   if ( !failed_c() ) 
   {
      /*
      Create the DSK. 
      */
      t_wrtplt_c ( bodyid, 
                   surfid, 
                   frame,
                   first,
                   last,
                   corsys,
                   corpar,
                   bounds,
                   nv,
                   np, 
                   (ConstSpiceDouble (*)[3]) usrvrt, 
                   (ConstSpiceInt    (*)[3]) usrplt, 
                   makvtl,
                   dsk                               );
   }

   /*
   Deallocate the vertex and plate arrays that were used to capture
   the corresponding input arguments. 
   */
   freeVecGD_jni ( env, J_usrvrt, usrvrt );
   freeVecGI_jni ( env, J_usrplt, usrplt );

   /*
   Free the frame and DSK strings.
   */
   freeVarInputString_jni ( env, J_frame, frame );
   freeVarInputString_jni ( env, J_dsk,   dsk   );

   /*
   Free the dynamically allocated bounds array. 
   */
   free_SpiceMemory ( (void *)bounds );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}




/* 
Wrapper for tutils_c function t_wrtplz
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_t_1wrtplz
(  JNIEnv           * env, 
   jclass             J_class,
   jint               J_bodyid,
   jint               J_surfid,   
   jstring            J_frame,
   jint               J_nv,
   jint               J_np,
   jdoubleArray       J_usrvrt,
   jintArray          J_usrplt,
   jstring            J_dsk     )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.t_wrtplz";
   SpiceChar             * dsk;
   SpiceChar             * frame;

   SpiceDouble           * usrvrt;

   SpiceInt                bodyid;   
   SpiceInt                dskNameLen;
   SpiceInt                frNameLen;
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt                plateArrayLen;
   SpiceInt                surfid;
   SpiceInt              * usrplt;
   SpiceInt                vertArrayLen;

   /*
   Fetch local copies of scalar inputs. 
   */
   bodyid = (SpiceInt) J_bodyid;
   surfid = (SpiceInt) J_surfid;
   nv     = (SpiceInt) J_nv;
   np     = (SpiceInt) J_np;


   /*
   Store the input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_frame,  &frNameLen,  &frame );
   getVarInputString_jni ( env, J_dsk,    &dskNameLen, &dsk   );

   /*
   Get dynamically allocated, local copies of the input arrays.
   */
   getVecGD_jni ( env, J_usrvrt, &vertArrayLen,  &usrvrt );
   getVecGI_jni ( env, J_usrplt, &plateArrayLen, &usrplt );

   /*
   Handle a JNI exception, if one occurred.
   */
   JNI_EXC( env );


   if ( !failed_c() ) 
   {
      /*
      Create the DSK. 
      */
      t_wrtplz_c ( bodyid, 
                   surfid, 
                   frame,
                   nv,
                   np, 
                   (ConstSpiceDouble (*)[3]) usrvrt, 
                   (ConstSpiceInt    (*)[3]) usrplt, 
                   dsk                               );
   }

   /*
   Deallocate the vertex and plate arrays that were used to capture
   the corresponding input arguments. 
   */
   freeVecGD_jni ( env, J_usrvrt, usrvrt );
   freeVecGI_jni ( env, J_usrplt, usrplt );

   /*
   Free the frame and DSK strings.
   */
   freeVarInputString_jni ( env, J_frame, frame );
   freeVarInputString_jni ( env, J_dsk,   dsk   );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}




/* 
Wrapper for tutils_c function tcase_c 
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_tcase
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_name   )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.tcase";
   SpiceChar             * namePtr;

   SpiceInt                nameLen;


   /*
   Store the input string in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name,  &nameLen,  &namePtr );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC( env );


   tcase_c ( namePtr );

   /*
   Free the name string.
   */
   freeVarInputString_jni ( env, J_name,  namePtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for tutils_c function tclose_c 
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_tclose
(  JNIEnv           * env, 
   jclass             J_class )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.tclose";
 
   tclose_c();

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}
 


/* 
Wrapper for tutils_c function topen_c 
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_topen
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_name   )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.topen";
   SpiceChar             * namePtr;

   SpiceInt                nameLen;


   /*
   Store the input string in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name,  &nameLen,  &namePtr );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC( env );


   topen_c ( namePtr );

   /*
   Free the name string.
   */
   freeVarInputString_jni ( env, J_name,  namePtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for tutils_c function t_pck08_c 

Note: this wrapper uses a name without an underscore, since
such names have caused problems for the Java utility javah.
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_tpck08
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_file,
   jboolean           J_load,
   jboolean           J_keep  )
{
   /*
   Local variables 
   */
   logical                 load;
   logical                 keep;


   static SpiceChar      * caller = "JNITestutils.tpck08";
   static SpiceChar        file [ FNAMLN ];

   getFixedInputString_jni ( env, J_file, FNAMLN, file );

   JNI_EXC( env );


   load   =  (integer) J_load;
   keep   =  (integer) J_keep;

   t_pck08__ ( (char     *) file,
               (logical  *) &load,
               (logical  *) &keep,
               (ftnlen    ) strlen(file) );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for tutils_c function tsetup_c 
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_tsetup
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_cmdLine,
   jstring            J_lognam,
   jstring            J_version     )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.tsetup";
   SpiceChar             * cmdLinePtr;
   SpiceChar             * lognamPtr;
   SpiceChar             * versionPtr;

   SpiceInt                cmdLineLen;
   SpiceInt                lognamLen;
   SpiceInt                versionLen;


   /*
   Store input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_cmdLine, &cmdLineLen, &cmdLinePtr );
   getVarInputString_jni ( env, J_lognam,  &lognamLen,  &lognamPtr  );
   getVarInputString_jni ( env, J_version, &versionLen, &versionPtr );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC( env );


   tsetup_c ( cmdLinePtr, lognamPtr, versionPtr );

   /*
   Free strings. 
   */
   freeVarInputString_jni ( env, J_cmdLine, cmdLinePtr );
   freeVarInputString_jni ( env, J_lognam,  lognamPtr  );
   freeVarInputString_jni ( env, J_version, versionPtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}


/* 
Wrapper for tutils_c function tstatd_c

NOTE: the output arrays are updated by this routine; these
arrays must be created by the caller.
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_tstatd
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_inst, 
   jdouble            J_et, 
   jobjectArray       J_cmat,
   jdoubleArray       J_av    )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.tstatd";

   SpiceDouble             av       [3];
   SpiceDouble             cmat     [3][3];


   /*
   Fetch the attitude data for the request time.
   */
   tstatd_c ( (SpiceDouble )J_et, cmat, av );           

   /*
   If the lookup resulted in a SPICE error, throw an exception. 
   */
   SPICE_EXC ( env, caller );

   /*
   Set the outputs. We copy all of tstatd_c's outputs to Java
   arrays.
   */
   updateMat33D_jni ( env,    (CONST_MAT cmat),    J_cmat   );
   updateVec3D_jni  ( env,    (CONST_VEC av  ),    J_av     );

   /*
   The outputs are set. 
   */
}



/* 
Wrapper for tutils_c function tstck3_c 
*/
JNIEXPORT jint JNICALL Java_spice_testutils_JNITestutils_tstck3
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_cknm,
   jstring            J_sclknm,
   jboolean           J_loadck,
   jboolean           J_loadsc,
   jboolean           J_keepsc   )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.tstck3";
   SpiceChar             * cknmPtr;
   SpiceChar             * sclknmPtr;

   SpiceInt                cknmLen;
   SpiceInt                handle = 0;
   SpiceInt                sclknmLen;


   /*
   Store input strings in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_cknm,   &cknmLen,   &cknmPtr  );
   getVarInputString_jni ( env, J_sclknm, &sclknmLen, &sclknmPtr );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC_VAL( env,  ((jint)handle) );


   tstck3_c ( cknmPtr, 
              sclknmPtr, 
              (SpiceBoolean)J_loadck, 
              (SpiceBoolean)J_loadsc, 
              (SpiceBoolean)J_keepsc, 
              &handle                 );

   /*
   Free strings. 
   */
   freeVarInputString_jni ( env, J_cknm,   cknmPtr   );
   freeVarInputString_jni ( env, J_sclknm, sclknmPtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller,  ((jint)handle) );

   /*
   Normal return. 
   */
   return ( (jint)handle );
}



/* 
Wrapper for tutils_c function tstlsk_c 
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_tstlsk
(  JNIEnv           * env, 
   jclass             J_class )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.tstlsk";

   /*
   Make the call. 
   */
   tstlsk_c();

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for tutils_c function tstpck_c 
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_tstpck
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_name,
   jboolean           J_load,
   jboolean           J_keep    )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "JNITestutils.tstpck";
   SpiceChar             * namePtr;

   SpiceInt                nameLen;


   /*
   Store the input string in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name, &nameLen, &namePtr );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC( env );
  

   tstpck_c ( namePtr, 
              (SpiceBoolean)J_load, 
              (SpiceBoolean)J_keep  );

   /*
   Free the name string.
   */
   freeVarInputString_jni ( env, J_name, namePtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for tutils_c function tstspk_c 
*/
JNIEXPORT jint JNICALL Java_spice_testutils_JNITestutils_tstspk
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_name,
   jboolean           J_load    )

{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "JNITestutils.tstspk";
   SpiceChar             * namePtr;

   SpiceInt                handle   =  0;
   SpiceInt                nameLen;


   /*
   Store the input string in dynamically allocated memory. 
   */
   getVarInputString_jni ( env, J_name, &nameLen, &namePtr );

   /*
   Handle an exception, if one occurred.
   */
   JNI_EXC_VAL( env,  ((jint)handle)  );


   tstspk_c ( namePtr, (SpiceBoolean)J_load, &handle );
 

   /*
   Free the name string.
   */
   freeVarInputString_jni ( env, J_name, namePtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller,  ((jint)handle)  );

   /*
   Normal return. 
   */
   return ( (jint)handle );
}



JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_tsttxt
  (JNIEnv * env,
   jclass             J_class,
   jstring            J_name,
   jobjectArray       J_cvals,
   jboolean           J_load,
   jboolean           J_keep  )
{
   /*
   Local variables 
   */
   logical                 keep;
   logical                 load;

   static SpiceChar      * caller   = "JNITestutils.tsttxt";
   SpiceChar             * fname;

   SpiceInt                fnameLen;
   SpiceInt                fStrLen;
   SpiceInt                nStr;

   void                  * fStrArray;


   /*
   First step: get the file name. This string must be freed
   before exiting.
   */
   getVarInputString_jni ( env, J_name, &fnameLen, &fname );
   
   JNI_EXC ( env );
   

   /*
   Grab the input Java string array in a dynamically allocated
   Fortran-style array. 
   */
   getFortranStringArray_jni ( env,    J_cvals, 
                               &nStr,  &fStrLen,  &fStrArray );
   JNI_EXC ( env );
   SPICE_EXC( env, caller  );

 
   /*
   Call the f2c'd routine tsttxt_.  Given the string manipulation tools
   we have, it's more convenient to do this than to call tsttxt_c.
   */
   load = (logical) J_load;
   keep = (logical) J_keep;


   tsttxt_ ( (char    * ) fname,
             (char    * ) fStrArray,
             (integer * ) &nStr,
             (logical * ) &load,
             (logical * ) &keep,
             (ftnlen    ) fnameLen,
             (ftnlen    ) fStrLen       );

   /*
   Clean up all of our dynamically allocated items.
  
   Start with the file name.
   */
   freeVarInputString_jni ( env, J_name, fname );

   /*
   Regardless of the outcome of the insertion,  
   free the Fortran string array.
   */
   free ( fStrArray );             
}




/* 
Wrapper for tutils_c function tsuccess_c 

Note: this wrapper uses a name without an underscore, since
such names have caused problems for the Java utility javah.
*/
JNIEXPORT jboolean JNICALL Java_spice_testutils_JNITestutils_tsuccess
(  JNIEnv           * env, 
   jclass           J_class)
{
   /*
   Local variables 
   */
   SpiceBoolean            ok     =  0;

   static SpiceChar      * caller = "JNITestutils.tsuccess";


   t_success_c ( &ok );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)ok)  );

   /*
   Normal return. 
   */
   return ( (jboolean)ok );
}




/* 
Wrapper for dsklib_c function zzellplt
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_zzellplt
(  JNIEnv           * env, 
   jclass             J_class,
   jdouble            J_a,
   jdouble            J_b,
   jdouble            J_c,
   jint               J_nlon,
   jint               J_nlat,
   jintArray          J_nv,
   jdoubleArray       J_vout,
   jintArray          J_np,
   jintArray          J_pout )
{
   /*
   Prototypes 
   */


   /*
   Local variables 
   */
   SpiceCell             * plateCell;
   SpiceCell             * vertexCell;

   static SpiceChar      * caller = "JNITestutils.zzellplt";

   SpiceDouble             a;
   SpiceDouble             b;
   SpiceDouble             c;

   SpiceInt                nlat;
   SpiceInt                nlon;
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt                psize;
   SpiceInt                vsize;


   /*
   Get local copies of scalar inputs.
   */
   a    =  (SpiceDouble) J_a;
   b    =  (SpiceDouble) J_b;
   c    =  (SpiceDouble) J_c;
   nlon =  (SpiceInt   ) J_nlon;
   nlat =  (SpiceInt   ) J_nlat;

   /*
   Compute required sizes of cells. See the header of 
   zzellplt_c for details. 
   */   
   vsize = 3 * (  ( nlon * ( nlat - 1 ) )  +  2  );
   psize = 3 * (  ( nlon * ( nlat - 1 ) )  *  2  );

   /*
   Allocate cells dynamically. We're not copying data from an 
   input source, so the initial cardinality is zero, and the
   input data pointer is null.
   */   
   plateCell  = zzalcell_c ( SPICE_INT, psize, 0, 0, NULL );
   vertexCell = zzalcell_c ( SPICE_DP,  vsize, 0, 0, NULL );

   SPICE_EXC ( env, caller );


   /*
   Generate plate set. 
   */ 
   zzellplt_c ( a, b, c, nlon, nlat, vertexCell, plateCell );

   nv = card_c( vertexCell ) / 3;
   np = card_c( plateCell  ) / 3;

   /*
   Transfer outputs to output arguments, as long as the plate
   set generation succeeded.
   */
   if ( !failed_c() ) 
   {                 
      updateVecGI_jni ( env, psize, plateCell->data,  J_pout );
      updateVecGD_jni ( env, vsize, vertexCell->data, J_vout );
      
      updateVecGI_jni ( env, 1, &nv,  J_nv );
      updateVecGI_jni ( env, 1, &np,  J_np );      
   }

   /*
   Deallocate the dynamic cells. 
   */
   zzdacell_c ( plateCell  );
   zzdacell_c ( vertexCell );

   return;
}




/* 
Wrapper for dsklib_c function zzpsball
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_zzpsball
(  JNIEnv           * env, 
   jclass             J_class,
   jdouble            J_r,
   jint               J_nlon,
   jint               J_nlat,
   jintArray          J_nv,
   jdoubleArray       J_vout,
   jintArray          J_np,
   jintArray          J_pout )
{
   /*
   Prototypes 
   */


   /*
   Local variables 
   */
   SpiceCell             * plateCell;
   SpiceCell             * vertexCell;

   static SpiceChar      * caller = "JNITestutils.zzpsball";

   SpiceDouble             r;

   SpiceInt                nlat;
   SpiceInt                nlon;
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt                psize;
   SpiceInt                vsize;


   /*
   Get local copies of scalar inputs.
   */
   r    =  (SpiceDouble) J_r;
   nlon =  (SpiceInt   ) J_nlon;
   nlat =  (SpiceInt   ) J_nlat;

   /*
   Compute required sizes of cells. See the header of 
   zzpsball_c for details. 
   */   
   vsize = 3 * (  ( nlon * ( nlat - 1 ) )  +  2  );
   psize = 3 * (  ( nlon * ( nlat - 1 ) )  *  2  );

   /*
   Allocate cells dynamically. We're not copying data from an 
   input source, so the initial cardinality is zero, and the
   input data pointer is null.
   */   
   plateCell  = zzalcell_c ( SPICE_INT, psize, 0, 0, NULL );
   vertexCell = zzalcell_c ( SPICE_DP,  vsize, 0, 0, NULL );

   SPICE_EXC ( env, caller );


   /*
   Generate plate set. 
   */ 
   zzpsball_c ( r, nlon, nlat, vertexCell, plateCell );

   nv = card_c( vertexCell ) / 3;
   np = card_c( plateCell  ) / 3;

   /*
   Transfer outputs to output arguments, as long as the plate
   set generation succeeded.
   */
   if ( !failed_c() ) 
   {                 
      updateVecGI_jni ( env, psize, plateCell->data,  J_pout );
      updateVecGD_jni ( env, vsize, vertexCell->data, J_vout );
      
      updateVecGI_jni ( env, 1, &nv,  J_nv );
      updateVecGI_jni ( env, 1, &np,  J_np );      
   }

   /*
   Deallocate the dynamic cells. 
   */
   zzdacell_c ( plateCell  );
   zzdacell_c ( vertexCell );

   return;
}



/* 
Wrapper for dsklib_c function zzpsbox
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_zzpsbox
(  JNIEnv           * env, 
   jclass             J_class,
   jdouble            J_a,
   jdouble            J_b,
   jdouble            J_c,
   jintArray          J_nv,
   jdoubleArray       J_vout,
   jintArray          J_np,
   jintArray          J_pout )
{
   /*
   Prototypes 
   */


   /*
   Local variables 
   */
   SpiceCell             * plateCell;
   SpiceCell             * vertexCell;

   static SpiceChar      * caller = "JNITestutils.zzpsbox";

   SpiceDouble             a;
   SpiceDouble             b;
   SpiceDouble             c;

   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt                psize;
   SpiceInt                vsize;


   /*
   Get local copies of scalar inputs.
   */
   a    =  (SpiceDouble) J_a;
   b    =  (SpiceDouble) J_b;
   c    =  (SpiceDouble) J_c;

   /*
   Compute required sizes of cells. Each face of the box
   is composed of two plates.
   */   
   nv    = 8;
   np    = 12;

   vsize = 3 * nv;
   psize = 3 * np;

   /*
   Allocate cells dynamically. We're not copying data from an 
   input source, so the initial cardinality is zero, and the
   input data pointer is null.
   */   
   plateCell  = zzalcell_c ( SPICE_INT, psize, 0, 0, NULL );
   vertexCell = zzalcell_c ( SPICE_DP,  vsize, 0, 0, NULL );

   SPICE_EXC ( env, caller );


   /*
   Generate plate set. 
   */ 
   zzpsbox_c ( a, b, c, vertexCell, plateCell );


   /*
   Transfer outputs to output arguments, as long as the plate
   set generation succeeded.
   */
   if ( !failed_c() ) 
   {                 
      updateVecGI_jni ( env, psize, plateCell->data,  J_pout );
      updateVecGD_jni ( env, vsize, vertexCell->data, J_vout );
      
      updateVecGI_jni ( env, 1, &nv,  J_nv );
      updateVecGI_jni ( env, 1, &np,  J_np );      
   }

   /*
   Deallocate the dynamic cells. 
   */
   zzdacell_c ( plateCell  );
   zzdacell_c ( vertexCell );

   return;
}




/* 
Wrapper for dsklib_c function zzpspoly
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_zzpspoly
(  JNIEnv           * env, 
   jclass             J_class,
   jint               J_n,
   jdoubleArray       J_vrtces,
   jintArray          J_nv,
   jdoubleArray       J_vout,
   jintArray          J_np,
   jintArray          J_pout )
{
   /*
   Prototypes 
   */


   /*
   Local variables 
   */
   SpiceCell             * plateCell;
   SpiceCell             * vertexCell;

   static SpiceChar      * caller = "JNITestutils.zzpspoly";

   SpiceDouble           * vrtPtr;

   SpiceInt                n;
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt                psize;
   SpiceInt                vertArrayLen;
   SpiceInt                vsize;


   /*
   Get dynamically allocated, local copy of the input vertex array.
   */
   getVecGD_jni ( env, J_vrtces, &vertArrayLen, &vrtPtr );

   /*
   Get local copies of scalar inputs.
   */
   n    =  (SpiceInt) J_n;

   /*
   Compute required sizes of cells.
   */   
   nv    = n+1;
   np    = n;

   /*
   Note that the input vectors are 2-D, but the output vertices
   are 3-D.
   */
   vsize = 3 * nv;
   psize = 3 * np;

   /*
   Allocate cells dynamically. We're not copying data from an 
   input source, so the initial cardinality is zero, and the
   input data pointer is null.
   */   
   plateCell  = zzalcell_c ( SPICE_INT, psize, 0, 0, NULL );
   vertexCell = zzalcell_c ( SPICE_DP,  vsize, 0, 0, NULL );

   if ( !failed_c() )
   {
      /*
      Generate plate set. 
      */ 
      zzpspoly_c ( n, (SpiceDouble (*)[2])vrtPtr, vertexCell, plateCell );

      /*
      Transfer outputs to output arguments, as long as the plate
      set generation succeeded.
      */
      if ( !failed_c() ) 
      {                 
         updateVecGI_jni ( env, psize, plateCell->data,  J_pout );
         updateVecGD_jni ( env, vsize, vertexCell->data, J_vout );

         updateVecGI_jni ( env, 1, &nv,  J_nv );
         updateVecGI_jni ( env, 1, &np,  J_np );      
      }
   }

   /*
   Deallocate the input vertex cell. 
   */
   freeVecGD_jni ( env, J_vrtces, vrtPtr );

   /*
   Deallocate the dynamic cells. 
   */
   zzdacell_c ( plateCell  );
   zzdacell_c ( vertexCell );

   /*
   Handle SPICE errors.
   */
   SPICE_EXC ( env, caller );


   return;
}


 



/* 
Wrapper for dsklib_c function zzpsscal
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_zzpsscal
(  JNIEnv           * env, 
   jclass             J_class,
   jdouble            J_scale,
   jdoubleArray       J_v1,
   jintArray          J_nv,
   jdoubleArray       J_vout  )
{
   /*
   Prototypes 
   */


   /*
   Local variables 
   */
   SpiceCell             * inVertexCell;
   SpiceCell             * outVertexCell;

   static SpiceChar      * caller = "JNITestutils.zzpsscal";

   SpiceDouble             scale;
   SpiceDouble           * vrtPtr;

   SpiceInt                nv;
   SpiceInt                vertArrayLen;
   SpiceInt                vsize;


   /*
   Get local copy of vertex array.
   */
   getVecGD_jni ( env, J_v1, &vertArrayLen, &vrtPtr );

   nv = vertArrayLen / 3;

   /*
   Get local copies of scalar inputs.
   */
   scale = (SpiceDouble) J_scale;

   /*
   Compute required size of the vertex cells.
   */   
   vsize = 3 * nv;

   /*
   Allocate the vertex cells dynamically. For the output cell, we're
   not copying data from an input source, so the initial cardinality is
   zero, and the input data pointer is null.
   */   
   inVertexCell  = zzalcell_c ( SPICE_DP,  vsize, vsize, 0, vrtPtr );
   outVertexCell = zzalcell_c ( SPICE_DP,  vsize, 0,     0, NULL );

   /*
   Deallocate the input dynamic array. 
   */
   freeVecGD_jni ( env, J_v1, vrtPtr );


   if ( !failed_c() )
   {
      /*
      Scale the vertex set. 
      */ 
      zzpsscal_c ( scale, inVertexCell, outVertexCell );

      /*
      Transfer outputs to output arguments, as long as the plate
      set generation succeeded.
      */
      if ( !failed_c() ) 
      {                 
         updateVecGD_jni ( env, vsize, outVertexCell->data, J_vout );

         updateVecGI_jni ( env, 1, &nv,  J_nv );
      }
   }

   /*
   Deallocate the dynamic cells. 
   */
   zzdacell_c ( inVertexCell  );
   zzdacell_c ( outVertexCell );

   /*
   Handle SPICE errors. 
   */
   SPICE_EXC ( env, caller );

   return;
}



/* 
Wrapper for dsklib_c function zzpsun
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_zzpsun
(  JNIEnv           * env, 
   jclass             J_class,
   jdoubleArray       J_v1,
   jintArray          J_p1,
   jdoubleArray       J_v2,
   jintArray          J_p2,
   jintArray          J_nv,
   jdoubleArray       J_vout,
   jintArray          J_np,
   jintArray          J_pout )
{
   /*
   Prototypes 
   */


   /*
   Local variables 
   */
   SpiceCell             * p1Cell;
   SpiceCell             * p2Cell;
   SpiceCell             * plateCell;
   SpiceCell             * v1Cell;
   SpiceCell             * v2Cell;
   SpiceCell             * vertexCell;

   static SpiceChar      * caller = "JNITestutils.zzpsun";

   SpiceDouble           * v1Ptr;
   SpiceDouble           * v2Ptr;

   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt                p1ArrayLen;
   SpiceInt                p2ArrayLen;
   SpiceInt              * p1Ptr;
   SpiceInt              * p2Ptr;
   SpiceInt                psize;
   SpiceInt                v1ArrayLen;
   SpiceInt                v2ArrayLen;
   SpiceInt                vsize;


   /*
   Get dynamically allocated, local copies of the input arrays.
   */
   getVecGD_jni ( env, J_v1, &v1ArrayLen, &v1Ptr );
   getVecGD_jni ( env, J_v2, &v2ArrayLen, &v2Ptr );

   getVecGI_jni ( env, J_p1, &p1ArrayLen, &p1Ptr );
   getVecGI_jni ( env, J_p2, &p2ArrayLen, &p2Ptr );

   /*
   Allocate input cells dynamically. 
   */
   v1Cell = zzalcell_c ( SPICE_DP,  v1ArrayLen, v1ArrayLen, 0, v1Ptr );
   v2Cell = zzalcell_c ( SPICE_DP,  v2ArrayLen, v2ArrayLen, 0, v2Ptr );
   p1Cell = zzalcell_c ( SPICE_INT, p1ArrayLen, p1ArrayLen, 0, p1Ptr );
   p2Cell = zzalcell_c ( SPICE_INT, p2ArrayLen, p2ArrayLen, 0, p2Ptr );

   /*
   Deallocate the vertex and plate arrays that were used to capture
   the corresponding input arguments. 
   */
   freeVecGD_jni ( env, J_v1, v1Ptr );
   freeVecGD_jni ( env, J_v2, v2Ptr );

   freeVecGI_jni ( env, J_p1, p1Ptr );
   freeVecGI_jni ( env, J_p2, p2Ptr );

   if ( !failed_c() )
   {
      /*
      Compute required sizes of output cells.
      */   
      vsize = v1ArrayLen + v2ArrayLen;
      psize = p1ArrayLen + p2ArrayLen;

      /*
      Allocate output cells dynamically. We're not copying data from an 
      input source, so the initial cardinality is zero, and the
      input data pointer is null.
      */   
      plateCell  = zzalcell_c ( SPICE_INT, psize, 0, 0, NULL );
      vertexCell = zzalcell_c ( SPICE_DP,  vsize, 0, 0, NULL );

      /*
      Generate plate set. 
      */ 
      zzpsun_c ( v1Cell, p1Cell, v2Cell, p2Cell, vertexCell, plateCell );

      /*
      Transfer outputs to output arguments, as long as the plate
      set generation succeeded.
      */
      if ( !failed_c() ) 
      {                 
         updateVecGI_jni ( env, psize, plateCell->data,  J_pout );
         updateVecGD_jni ( env, vsize, vertexCell->data, J_vout );

         nv = card_c( vertexCell ) / 3;
         np = card_c( plateCell  ) / 3;

         updateVecGI_jni ( env, 1, &nv,  J_nv );
         updateVecGI_jni ( env, 1, &np,  J_np );      
      }
   }

   /*
   Deallocate the dynamic cells. 
   */
   zzdacell_c ( v1Cell );
   zzdacell_c ( v2Cell );
   zzdacell_c ( p1Cell );
   zzdacell_c ( p2Cell );

   /*
   Handle SPICE errors.
   */
   SPICE_EXC ( env, caller );

   return;
}







/* 
Wrapper for dsklib_c function zzpsxfrm
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_zzpsxfrm
(  JNIEnv           * env, 
   jclass             J_class,
   jdoubleArray       J_v1,
   jobjectArray       J_xform,
   jintArray          J_nv,
   jdoubleArray       J_vout  )
{
   /*
   Prototypes 
   */


   /*
   Local variables 
   */
   SpiceCell             * inVertexCell;
   SpiceCell             * outVertexCell;

   static SpiceChar      * caller = "JNITestutils.zzpsxfrm";

   SpiceDouble           * vrtPtr;
   SpiceDouble             xform[3][3];

   SpiceInt                nv;
   SpiceInt                vertArrayLen;
   SpiceInt                vsize;


   /*
   Get local copy of vertex array.
   */
   getVecGD_jni ( env, J_v1, &vertArrayLen, &vrtPtr );

   nv = vertArrayLen / 3;

   /*
   Get the transformation matrix. 
   */
   getMat33D_jni ( env, J_xform, xform );

   /*
   Compute required size of the vertex cells.
   */   
   vsize = 3 * nv;

   /*
   Allocate the vertex cells dynamically. For the output cell, we're
   not copying data from an input source, so the initial cardinality is
   zero, and the input data pointer is null.
   */   
   inVertexCell  = zzalcell_c ( SPICE_DP,  vsize, vsize, 0, vrtPtr );
   outVertexCell = zzalcell_c ( SPICE_DP,  vsize, 0,     0, NULL );

   /*
   Deallocate the input dynamic array. 
   */
   freeVecGD_jni ( env, J_v1, vrtPtr );

   if ( !failed_c() )
   {
      /*
      Transform the vertex set. 
      */ 
      zzpsxfrm_c ( inVertexCell, 
                   (ConstSpiceDouble (*)[3]) xform, 
                   outVertexCell                    );

      /*
      Transfer outputs to output arguments, as long as the plate
      set generation succeeded.
      */
      if ( !failed_c() ) 
      {                 
         updateVecGD_jni ( env, vsize, outVertexCell->data, J_vout );

         updateVecGI_jni ( env, 1, &nv,  J_nv );
      }
   }

   /*
   Deallocate the dynamic cells. 
   */
   zzdacell_c ( inVertexCell  );
   zzdacell_c ( outVertexCell );

   /*
   Handle SPICE errors. 
   */
   SPICE_EXC ( env, caller );

   return;
}




/* 
Wrapper for dsklib_c function zzpsxlat
*/
JNIEXPORT void JNICALL Java_spice_testutils_JNITestutils_zzpsxlat
(  JNIEnv           * env, 
   jclass             J_class,
   jdoubleArray       J_v1,
   jdoubleArray       J_offset,
   jintArray          J_nv,
   jdoubleArray       J_vout  )
{
   /*
   Prototypes 
   */


   /*
   Local variables 
   */
   SpiceCell             * inVertexCell;
   SpiceCell             * outVertexCell;

   static SpiceChar      * caller = "JNITestutils.zzpsxlat";

   SpiceDouble             offset [3];
   SpiceDouble           * vrtPtr;

   SpiceInt                nv;
   SpiceInt                vertArrayLen;
   SpiceInt                vsize;


   /*
   Get local copy of offset vector. 
   */
   getVec3D_jni ( env, J_offset, offset );

   /*
   Get local copy of vertex array.
   */
   getVecGD_jni ( env, J_v1, &vertArrayLen, &vrtPtr );

   nv = vertArrayLen / 3;


   /*
   Compute required size of the vertex cells.
   */   
   vsize = 3 * nv;

   /*
   Allocate the vertex cells dynamically. For the output cell, we're
   not copying data from an input source, so the initial cardinality is
   zero, and the input data pointer is null.
   */   
   inVertexCell  = zzalcell_c ( SPICE_DP,  vsize, vsize, 0, vrtPtr );
   outVertexCell = zzalcell_c ( SPICE_DP,  vsize, 0,     0, NULL );

   /*
   Deallocate the input dynamic array. 
   */
   freeVecGD_jni ( env, J_v1, vrtPtr );

   if ( !failed_c() )
   {
      /*
      Translate the vertex set. 
      */ 
      zzpsxlat_c ( inVertexCell, offset, outVertexCell );


      /*
      Transfer outputs to output arguments, as long as the plate
      set generation succeeded.
      */
      if ( !failed_c() ) 
      {                 
         updateVecGD_jni ( env, vsize, outVertexCell->data, J_vout );

         updateVecGI_jni ( env, 1, &nv,  J_nv );
      }
   }

   /*
   Deallocate the dynamic cells. 
   */
   zzdacell_c ( inVertexCell  );
   zzdacell_c ( outVertexCell );

   /*
   Handle SPICE errors. 
   */
   SPICE_EXC ( env, caller );

   return;
}





/* 
Wrapper for dsklib_c function zzt_boddsk_c
*/
JNIEXPORT jint JNICALL Java_spice_testutils_JNITestutils_zztboddsk
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_dsk,
   jstring            J_body,
   jstring            J_fixref,
   jboolean           J_load )
{
   /*
   Prototypes 
   */
   void zzt_boddsk_c ( ConstSpiceChar  * dsk,
                       ConstSpiceChar  * body,
                       ConstSpiceChar  * fixref,
                       SpiceBoolean      load,
                       SpiceInt        * handle );
   /*
   Local variables 
   */
   logical                 load;
   integer                 handle = 0;

   static SpiceChar      * caller = "JNITestutils.zzt_boddsk";
   static SpiceChar        dsk    [ FNAMLN ];
   static SpiceChar        body   [ BDNMLN ];
   static SpiceChar        fixref [ FRNMLN ];

   getFixedInputString_jni ( env, J_dsk,    FNAMLN, dsk    );
   getFixedInputString_jni ( env, J_body,   BDNMLN, body   );
   getFixedInputString_jni ( env, J_fixref, FRNMLN, fixref );

   JNI_EXC_VAL( env, ((jint) handle) );


   load   =  (SpiceInt) J_load;

   zzt_boddsk_c ( dsk, body, fixref, load, &handle );

   SPICE_EXC_VAL( env, caller, ((jint)handle) );

   return ( (jint)handle );
}




/*
End of testutil_jni.c 
*/
