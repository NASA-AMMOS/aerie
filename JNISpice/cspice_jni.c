/*

-Procedure cspice_jni ( JNISpice, Java-C JNI bridge suite )

-Abstract

   SPICE Private routine intended solely for the support of SPICE
   routines.  Users should not call this routine directly due
   to the volatile nature of this routine.

   This is a suite of bridge routines that enable the Java
   layer of JNISpice to call C native methods calling CSPICE.

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

   JAVA
   JNI
   PRIVATE
   UTILITY

*/

   #include <stdlib.h>
   #include <string.h>
   #include <jni.h>
   #include "spice_basic_CSPICE.h"
   #include "SpiceUsr.h"
   #include "SpiceZim.h"
   #include "SpiceZfc.h"
   #include "SpiceZmc.h"
   #include "SpiceZst.h"
   #include "zzalloc.h"
   #include "cspice_jni.h"
   #include "zzGFSearchUtils.h"
   #include "zzGFScalarQuantity.h"
 
/*

-Brief_I/O

   None.

-Detailed_Input

   See the called CSPICE routines for descriptions of their inputs.

-Detailed_Output

   See the called CSPICE routines for descriptions of their outputs.

-Parameters

   See the included headers for descriptions of the parameters
   they declare.

-Exceptions

   TBD

-Files

   None.

-Particulars

   None.

-Examples

   See calls to these native implementations made in the classes of 
   JNISpice package cspice.basic.

-Restrictions

   1) This set of routines is private; SPICE user applications should
      not call these routines directly. APIs may change without
      notice.

   2) The ability of JNI C-language code to properly free memory
      in the event of of Java exceptions is limited; some of
      these routines may leak memory if the Java-supplied utilities
      they call throw exceptions.

-Literature_References

   [1] Java Native Interface 5.0 Specification, URL = 
       http://java.sun.com/j2se/1.5.0/docs/guide/jni/index.html
   

-Author_and_Institution

   N.J. Bachman  (JPL)

-Version

   -JNISpice Version 4.0.0, 07-FEB-2017 (NJB)

      Changed interface of CSPICE.dskw02. Added implementations
      of methods

         CSPICE.dlabbs
         CSPICE.dlafps
         CSPICE.dasac
         CSPICE.dasdc
         CSPICE.dasec
         CSPICE.dashfn
         CSPICE.dasopw
         CSPICE.dasrfr
         CSPICE.dskgtl
         CSPICE.dskmi2
         CSPICE.dskobj
         CSPICE.dskrb2
         CSPICE.dsksrf
         CSPICE.dskstl
         CSPICE.dskxsi
         CSPICE.dskxv
         CSPICE.illumf
         CSPICE.inedpl
         CSPICE.latsrf
         CSPICE.limbpt
         CSPICE.occult
         CSPICE.oscltx
         CSPICE.pckcls
         CSPICE.pckcov
         CSPICE.pckfrm
         CSPICE.pckopn
         CSPICE.pckw02
         CSPICE.pltar
         CSPICE.pltexp
         CSPICE.pltnp
         CSPICE.pltnrm
         CSPICE.pltvol
         CSPICE.pxfrm2
         CSPICE.spkcpo
         CSPICE.spkcpt
         CSPICE.spkcvo
         CSPICE.spkcpt
         CSPICE.srfc2s
         CSPICE.srfcss
         CSPICE.srfnrm
         CSPICE.srfs2c
         CSPICE.srfscc
         CSPICE.termpt
         CSPICE.tparse

      Bug fix: updated

         CSPICE.dskd02
         CSPICE.dski02
         CSPICE.dskp02
         CSPICE.dskv02

      so they don't crash when the input room is non-positive.
      Previously, these routines attempted to free dynamic 
      memory using invalid pointers.

      Now arguments to free_SpiceMemory are explicitly cast to type
    
         (void *)


   Previous update was 16-JUN-2014 (NJB)

      Added support for Alpha DSK APIs.

      Argument list change: input grid accepted by CSPICE.llgridPl02
      is now 2-dimensional.

   Last update was 10-JUN-2014 (NJB)

      Bug fix: in routine CSPICE.dskw02, changed array deallocation
      calls to use freeVecGD and freeVecGI rather than the C library
      function `free'. 

    Previous update was 06-JUN-2014 (NJB)

      Added error checks on `npoints' to limb_pl02
      and term_pl02. 

    Previous update was 08-MAY-2014 (NJB)

      Added include statement for dsk_proto.h.

    Last update was 10-MAR-2014 (NJB)(EDW)

      Added routines

         CSPICE.ccifrm
         CSPICE.gfilum
         CSPICE.gfpa
         CSPICE.gfrr
         CSPICE.gfstol

   -JNISpice Version 2.0.0, 08-OCT-2013 (NJB)

      Bug fix: in function CSPICE.gnpool, a call to free_SpiceMemory
      was removed from the error handling code for an empty input
      template string.

      Bug fix: axis order in CSPICE.xf2eul was corrected.

      Added routine 

         CSPICE.getfat

   -JNISpice Version 1.1.0, 11-APR-2011 (NJB)

      Bug fixes: In CSPICE.gnpool, corrected call
      to maxi_c. Also corrected handling of zero-length
      input string argument: now the argument is freed
      before a SPICE error is signaled, and the routine
      returns from the if block rather continuing.

   -JNISpice Version 1.0.0, 21-MAY-2010 (NJB)

-Index_Entries

   TBD

-&
*/




/*
   Version 1.1.0 21-MAY-2010 (NJB)

      Bug fix: memory leaks due to missing calls to freeVecGD_jni
      were corrected in 

          CSPICE.gfdist
          CSPICE.gfocce
          CSPICE.gfoclt
          CSPICE.gfsep
          CSPICE.gfsntc
          CPSICE.gfsubc
          CSPICE.gftfov
          CSPICE.gfrfov
          CSPICE.gfuds

      Bug fix: memory leak due to failure to free dynamically 
      allocated result window of gfrfov_c failure was corrected
      in

          CSPICE.gfrfov

      Bug fix: memory leak due to failure to free Fortran string
      array was corrected in

          CSPICE.dafac
          

   Version 1.0.0 29-DEC-2009 (NJB) 
*/

/*
   CSPICE WRAPPER IMPLEMENTATIONS FOLLOW 
*/


/* 
Wrapper for CSPICE function axisar_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_axisar
  (JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_axis,
   jdouble            J_angle  )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.axisar";

   SpiceDouble             axis [3];
   SpiceDouble             mout [3][3];


   /*
   Get the input axis in a C array. 
   */
   getVec3D_jni ( env, J_axis, axis );

   JNI_EXC_VAL ( env, retArray );


   /*
   Make the CSPICE call. 
   */
   axisar_c ( axis,  (SpiceDouble)J_angle,  mout );

   SPICE_EXC_VAL( env, caller, retArray );


   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( mout ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function b1900_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_b1900
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)b1900_c()  );
}



/* 
Wrapper for CSPICE function b1950_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_b1950
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)b1950_c()  );
}



/* 
Wrapper for CSPICE function badkpv_c


NOTE: this routine assumes string inputs are left-justified. 
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_badkpv
  ( JNIEnv *  env, 
    jclass    cls,
    jstring   J_caller,
    jstring   J_name,
    jstring   J_comp,
    jint      J_size,
    jint      J_divby,
    jstring   J_type   )        
{
   /*
   Local constants 
   */
   #define                 CMPLEN  11
   #define                 TYPLEN  2

   /*
   Local variables 
   */
   jboolean                retVal  = 0;

   SpiceBoolean            ok;

   static SpiceChar        caller  [ FNAMLN ];
   static SpiceChar        name    [ KVNMLN ];
   static SpiceChar        comp    [ CMPLEN ];
   static SpiceChar        type    [ TYPLEN ];


   /*
   Fetch the input strings.  
   */
   getFixedInputString_jni ( env, J_caller, FNAMLN, caller );
   getFixedInputString_jni ( env, J_name,   KVNMLN, name   );
   getFixedInputString_jni ( env, J_comp,   CMPLEN, comp   );
   getFixedInputString_jni ( env, J_type,   TYPLEN, type   );

   JNI_EXC_VAL( env, retVal );

   
   ok = badkpv_c ( caller,              name,               comp, 
                   (SpiceInt)J_size,   (SpiceInt)J_divby,   type[0]  );

   SPICE_EXC_VAL ( env, caller, ((jboolean)ok) );


   return ( (jboolean)ok );
}



/* 
Wrapper for CSPICE function bodc2n_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_bodc2n
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_code ) 
{
   /*
   Local variables 
   */
   jstring                 retName  = 0;

   SpiceBoolean            found;

   static SpiceChar      * caller   = "CSPICE.bodc2n";
   static SpiceChar        name     [ BDNMLN ];


   bodc2n_c ( (SpiceInt)J_code, BDNMLN, name, &found );

   if (  !failed_c()  &&  !found  )
   {
      setmsg_c ( "ID code # could not be translated to a name." );
      errint_c ( "#", (SpiceInt)J_code                          );
      sigerr_c ( "SPICE(NOTRANSLATION)"                         );

      /*
      Don't return; allow the code below to throw an exception. 
      */
   }
 
   /*
   Note that control does NOT return to the caller when     
   an exception is thrown from C code; hence we return
   a null pointer if we throw an exception.
   */
   SPICE_EXC_VAL( env, caller, retName );

   /*
   Normal return. 
   */
   return ( createJavaString_jni( env, name )  );
}



/* 
Wrapper for CSPICE function bodc2s_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_bodc2s
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_code ) 
{
   /*
   Local variables 
   */
   jstring                 retName  = 0;

   static SpiceChar      * caller   = "CSPICE.bodc2s";
   static SpiceChar        name     [ BDNMLN ];


   bodc2s_c ( (SpiceInt)J_code, BDNMLN, name );

   SPICE_EXC_VAL( env, caller, retName );

   /*
   Normal return. 
   */
   return ( createJavaString_jni( env, name )  );
}




/* 
Wrapper for CSPICE function bodfnd_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_bodfnd
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_ID,
   jstring            J_item  )

{
   /*
   Local variables 
   */
   SpiceBoolean            found = 0;
   static SpiceChar      * caller   = "CSPICE.bodfnd";
   static SpiceChar        item [ BDNMLN ];



   /*
   Fetch C string input in dynamic memory; copy to
   local memory; release dynamic memory. 

   Handle any exception that may occur.
   */
   getFixedInputString_jni ( env, J_item, BDNMLN, item );

   JNI_EXC_VAL( env, (jboolean)found );


   found = bodfnd_c ( (SpiceInt)J_ID, item );


   /*
   Note that control does NOT return to the caller when     
   an exception is thrown from C code; hence we return
   a null pointer if we throw an exception.
   */
   SPICE_EXC_VAL( env, caller, (jboolean)found );

   return ( (jboolean) found );
}



/* 
Wrapper for CSPICE function bodn2c_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_bodn2c
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_name ) 
{
   /*
   Local variables 
   */
   SpiceBoolean            found;
   static SpiceChar      * caller   = "CSPICE.bodn2c";
   static SpiceChar        locName  [ BDNMLN ];
   SpiceInt                code     =   0;


   /*
   Fetch C string input in dynamic memory; copy to
   local memory; release dynamic memory. 

   Handle any exception that may occur.
   */
   getFixedInputString_jni ( env, J_name, BDNMLN, locName );

   JNI_EXC_VAL( env, (jint)code );


   /*
   The CSPICE call. 
   */
   bodn2c_c ( locName, &code, &found );


   if (  !failed_c()  &&  !found  )
   {
      setmsg_c ( "Name # could not be translated to an ID code." );
      errch_c  ( "#", locName                                    );
      sigerr_c ( "SPICE(NOTRANSLATION)"                          );

      /*
      Don't return; allow the code below to throw an exception. 
      */
   }
 

   /*
   Note that control does NOT return to the caller when     
   an exception is thrown from C code; hence we return
   a zero if we throw an exception.
   */
   SPICE_EXC_VAL( env, caller, (jint)code );

   /*
   Normal return. 
   */
   return ( (jint) code );
}



/* 
Wrapper for CSPICE function bods2c_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_bods2c
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_name ) 
{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   static SpiceChar      * caller   = "CSPICE.bods2c";
   static SpiceChar        locName  [ BDNMLN ];

   SpiceInt                code  =   0;


   /*
   Fetch C string input in dynamic memory; copy to
   local memory; release dynamic memory. 

   Handle any exception that may occur.
   */
   getFixedInputString_jni ( env, J_name, BDNMLN, locName );

   JNI_EXC_VAL( env, (jint)code );


   /*
   The CSPICE call. 
   */
   bods2c_c ( locName, &code, &found );


   if (  !failed_c()  &&  !found  )
   {
      setmsg_c ( "Name # could not be translated to an ID code." );
      errch_c  ( "#", locName                                    );
      sigerr_c ( "SPICE(NOTRANSLATION)"                          );

      /*
      Don't return; allow the code below to throw an exception. 
      */
   }

   /*
   Note that control does NOT return to the caller when     
   an exception is thrown from C code; hence we return
   a zero if we throw an exception.
   */
   SPICE_EXC_VAL( env, caller, (jint)code );
 
   /*
   Normal return. 
   */
   return ( (jint) code );
}



/* 
Wrapper for CSPICE function bodvcd_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_bodvcd
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_ID,
   jstring            J_item  )

{
   /*
   Local variables 
   */
   jdoubleArray            jVec  =  0;

   SpiceBoolean            found;

   static SpiceChar      * caller   = "CSPICE.bodvcd";
   static SpiceChar        dtype  [ 2 ];
   static SpiceChar        item   [ KVNMLN ];
   static SpiceChar        varnam [ KVNMLN ];

   SpiceDouble           * dpdata;

   SpiceInt                n;
   SpiceInt                size;


   /*
   Capture the input item name string in a local buffer.

   Return if an exception occurs. No deallocation is required.
   */
   getFixedInputString_jni ( env, J_item, KVNMLN, item );
   
   JNI_EXC_VAL ( env, jVec );

   /*
   Create the full variable name so we can use dtpool_c to find
   the variable's dimension.
   */
   strncpy ( varnam, "BODY#_#", KVNMLN );

   repmi_c ( varnam, "#", (SpiceInt)J_ID, KVNMLN, varnam );
   repmc_c ( varnam, "#", item,           KVNMLN, varnam );


   dtpool_c ( varnam, &found, &size, dtype );

   /*
   If the variable was not found, signal an error. 
   */
   if ( !found ) 
   {
      setmsg_c ( "Double precision kernel variable # "
                 "was not found in the kernel pool."  );
      errch_c  ( "#",  varnam                         );
      sigerr_c ( "SPICE(KERVARNOTFOUND)"              );
   }


   /*
   If the variable was found but has character type, 
   signal an error. 
   */
   if (  found   &&  ( dtype[0] != 'N' )  )
   {
      setmsg_c ( "Kernel variable # has character type." );
      errch_c  ( "#",  varnam                            );
      sigerr_c ( "SPICE(BADDATATYPE)"                    );
   }

   /*
   If the lookup resulted in a SPICE error, throw an exception. 
   */
   SPICE_EXC_VAL ( env, caller, jVec  );

 
   /*
   At this point we know the variable's data array consists 
   of `size' d.p. numbers. 
 
   Allocate memory to hold the data array.
   */
   dpdata = alloc_SpiceDouble_C_array ( 1, size );

   if ( failed_c() )
   {
      /*
      Throw an exception. 
      */
      SPICE_EXC_VAL ( env, caller,  jVec  );
   }


   /*
   At long last, we're ready to make the call. 
   */
   bodvcd_c ( (SpiceInt)J_ID, item, size, &n, dpdata );

   /*
   If the lookup resulted in a SPICE error, throw an exception. 
   */
   SPICE_EXC_VAL ( env, caller,  jVec );

   /*
   Create a Java array to hold the d.p data. 
   */
   createVecGD_jni ( env, size, dpdata, &jVec );
 
   /*
   Regardless of the outcome of the Java array creation
   attempt, free the dynamically allocated memory now. 
   */
   free_SpiceMemory( (void *)dpdata );

   return jVec;
}



/* 
Wrapper for CSPICE function ccifrm_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ccifrm
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_frclss,
    jint          J_clssid,
    jintArray     J_frcode,
    jobjectArray  J_frname,
    jintArray     J_center,
    jbooleanArray J_found      )
    
{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   static SpiceChar      * caller = "CSPICE.ccifrm";
   static SpiceChar        frname   [ FRNMLN ];

   SpiceInt                frcode;
   SpiceInt                center;
   
   jstring                 jfrname;

   ccifrm_c ( (SpiceInt) J_frclss,
              (SpiceInt) J_clssid,
              FRNMLN,
              &frcode,
              frname,
              &center,
              &found );

   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC( env, caller );
  
   updateVecGI_jni ( env, 1, &frcode,  J_frcode  );
   
   /*
   Create a Java string holding the name string; update the 
   name object array string with this Java string.
   */
   jfrname = createJavaString_jni ( env, frname );
   JNI_EXC( env ); 

   (*env)->SetObjectArrayElement ( env, J_frname, 0, jfrname );
   JNI_EXC( env );

   updateVecGI_jni ( env, 1, &center,  J_center  );
   updateVecGB_jni ( env, 1, &found,   J_found   );
}



/* 
Wrapper for CSPICE function cgv2el_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_cgv2el
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_center,
   jdoubleArray       J_gv1,
   jdoubleArray       J_gv2  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.cgv2el";

   SpiceDouble             array   [9] ;
   SpiceDouble             center  [3];
   SpiceDouble             gv1     [3];
   SpiceDouble             gv2     [3];

   static SpiceEllipse     ellipse;


   /*
   Fetch the input vectors into C arrays. 
   */
   getVec3D_jni ( env, J_center, center );
   getVec3D_jni ( env, J_gv1, gv1 );
   getVec3D_jni ( env, J_gv2, gv2 );
  
   JNI_EXC_VAL ( env, retArray );

   /*
   Take advantage of any error handling that cgv2el_c
   may provide. 
   */
   cgv2el_c ( center, gv1, gv2, &ellipse );

   SPICE_EXC_VAL( env, caller, retArray );


   vequ_c ( ellipse.center,    array   );
   vequ_c ( ellipse.semiMajor, array+3 );
   vequ_c ( ellipse.semiMinor, array+6 );

   createVecGD_jni ( env, 9, array, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function ckcls_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ckcls
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.ckcls";

   
   ckcls_c ( (SpiceInt)J_handle );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function ckcov_c 

NOTE: the input and returned arrays have no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_ckcov
  (JNIEnv * env, 
   jclass             J_class,
   jstring            J_ck,
   jint               J_inst,
   jboolean           J_needav,
   jstring            J_level,
   jdouble            J_tol,
   jstring            J_timsys,
   jint               J_size,
   jdoubleArray       J_cover  )
{ 
   /*
   Local constants 
   */
   #define  LEVLLN         81
   #define  TSYSLN         81

   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;


   /*
   The cell below will be allocated dynamically.
   */
   SpiceCell             * cover;

   static SpiceChar      * caller   = "CSPICE.ckcov";
   static SpiceChar        ck     [ FNAMLN ];
   static SpiceChar        level  [ LEVLLN ];
   static SpiceChar        timsys [ TSYSLN ];

   SpiceDouble           * coverData;

   SpiceInt                coverSize;
   SpiceInt                maxSize;


   /*
   Get the size of the input window data array. 
   */
   coverSize = (*env)->GetArrayLength ( env, J_cover );
   
   JNI_EXC_VAL  ( env, retArray );


   /*
   Capture the input CK name, coverage level, and time system.
   */
   getFixedInputString_jni ( env, J_ck,     FNAMLN, ck );
   JNI_EXC_VAL  ( env, retArray );

   getFixedInputString_jni ( env, J_level,  LEVLLN, level );
   JNI_EXC_VAL  ( env, retArray );

   getFixedInputString_jni ( env, J_timsys, TSYSLN, timsys );
   JNI_EXC_VAL  ( env, retArray );

 
   if ( coverSize > 0 )
   {
      /*
      Capture the contents of the input array `cover' in dynamic
      memory.  Check out and return if an exception is thrown.
      */
      getVecGD_jni ( env, J_cover, &coverSize, &coverData );
      JNI_EXC_VAL  ( env, retArray );
   }
   else
   {
      coverData = 0;
   }

   
   /*
   If the specified output cell size is smaller than the input
   array size, we have a problem. 
   */
   maxSize = (SpiceInt)J_size;

   if ( maxSize < coverSize )
   {
      /*
      We must free the data from the input array before
      returning. 
      */
      if ( coverSize > 0 )
      {
         freeVecGD_jni ( env, J_cover, coverData );
      }

      setmsg_c ( "Input cell size is #; output size is #;" );
      errint_c ( "#",  coverSize                           );
      errint_c ( "#",  maxSize                             );
      sigerr_c ( "SPICE(OUTPUTCELLTOOSMALL)"               );

      SPICE_EXC_VAL(env, caller, retArray );
   }

   /*
   Create a dynamically allocated cell of size maxSize to hold
   the results of our ckcov_c call. Initialize the cell with
   the data from the input array, if any.
   */
   cover = zzalcell_c ( SPICE_DP, maxSize, coverSize, 0, coverData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   At this point, we're holding on to a dynamically allocated
   cell. We can't return before freeing this cell, so we must
   be careful about how we handle errors.

   However, we're now done with the coverData array.
   */
   if ( coverSize > 0 )
   {
      freeVecGD_jni ( env, J_cover, coverData );   
   }
   

   /*
   Make `cover' into a SPICE window. 
   */
   wnvald_c ( maxSize, coverSize, cover );

   
   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   We're finally ready for our CSPICE call. 
   */
   ckcov_c ( ck, 
             (SpiceInt    ) J_inst,
             (SpiceBoolean) J_needav,
             level,
             (SpiceDouble ) J_tol,
             timsys,
             cover                   );


   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   At this point, the data portion of `cover' is exactly
   what we want to return. 
   */
   createVecGD_jni ( env, 
                     card_c(cover), 
                     (SpiceDouble *)cover->data, 
                     &retArray                   );

   /*
   De-allocate the dynamic cell before departure.
   */
   zzdacell_c ( cover );

   /*
   Handle any JNI or SPICE error. 
   */
   JNI_EXC_VAL  ( env,         retArray );
   SPICE_EXC_VAL( env, caller, retArray );

   
   return ( retArray );
}



/* 
Wrapper for CSPICE function ckgp_c 

NOTE: the output arrays are updated by this routine; these
arrays must be created by the caller.
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ckgp
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_inst, 
   jdouble            J_sclkdp, 
   jdouble            J_tol, 
   jstring            J_ref,
   jobjectArray       J_cmat,
   jdoubleArray       J_clkout,
   jbooleanArray      J_found   )
{
   /*
   Local variables 
   */   
   SpiceBoolean            found;

   static SpiceChar      * caller   = "CSPICE.ckgp";
   static SpiceChar        ref      [ FRNMLN ];

   SpiceDouble             clkout;
   SpiceDouble             cmat     [3][3];
 

   /*
   Capture the input reference frame string in a local buffer.

   Return if an exception occurs. No deallocation is required.
   */
   getFixedInputString_jni ( env, J_ref, FRNMLN, ref);
   
   JNI_EXC ( env );

 
   ckgp_c ( (SpiceInt    ) J_inst,
            (SpiceDouble ) J_sclkdp,
            (SpiceDouble ) J_tol,
            ref,
            cmat,
            &clkout,
            &found                  );           


   /*
   If the lookup resulted in a SPICE error, throw an exception. 
   */
   SPICE_EXC ( env, caller );

   /*
   Set the outputs. We copy all of ckgp_c's outputs to Java
   arrays. Note that we don't treat the "not found" case
   differently.

   Start with the c-matrix; then handle the output SCLK
   and found flag.
   */
   updateMat33D_jni ( env,    (CONST_MAT cmat),    J_cmat   );
   updateVecGD_jni  ( env, 1, &clkout,             J_clkout );
   updateVecGB_jni  ( env, 1, &found,              J_found  );

   /*
   The outputs are set. 
   */
}



/* 
Wrapper for CSPICE function ckgpav_c 

NOTE: the output arrays are updated by this routine; these
arrays must be created by the caller.
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ckgpav
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_inst, 
   jdouble            J_sclkdp, 
   jdouble            J_tol, 
   jstring            J_ref,
   jobjectArray       J_cmat,
   jdoubleArray       J_av,
   jdoubleArray       J_clkout,
   jbooleanArray      J_found   )
{
   /*
   Local variables 
   */   
   SpiceBoolean            found;

   static SpiceChar      * caller   = "CSPICE.ckgpav";
   static SpiceChar        ref      [ FRNMLN ];

   SpiceDouble             av       [3];
   SpiceDouble             clkout;
   SpiceDouble             cmat     [3][3];


   /*
   Capture the input reference frame string in a local buffer.

   Return if an exception occurs. No deallocation is required.
   */
   getFixedInputString_jni ( env, J_ref, FRNMLN, ref);
   
   JNI_EXC ( env );


   ckgpav_c ( (SpiceInt    ) J_inst,
              (SpiceDouble ) J_sclkdp,
              (SpiceDouble ) J_tol,
              ref,
              cmat,
              av,
              &clkout,
              &found                  );           

   /*
   If the lookup resulted in a SPICE error, throw an exception. 
   */
   SPICE_EXC ( env, caller );

   /*
   Set the outputs. We copy all of ckgp_c's outputs to Java
   arrays. Note that we don't treat the "not found" case
   differently.

   Start with the c-matrix and a.v.; then handle the output SCLK
   and found flag.
   */
   updateMat33D_jni ( env,    (CONST_MAT cmat),    J_cmat   );
   updateVec3D_jni  ( env,    (CONST_VEC av  ),    J_av     );
   updateVecGD_jni  ( env, 1, &clkout,             J_clkout );
   updateVecGB_jni  ( env, 1, &found,              J_found  );

   /*
   The outputs are set. 
   */
}



/* 
Wrapper for CSPICE function ckupf_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ckupf
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle )
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.ckupf";

 
   ckupf_c ( (SpiceInt) J_handle );
  
   /*
   If the call resulted in a SPICE error, throw an exception. 
   */
   SPICE_EXC ( env, caller );
}



/* 
Wrapper for CSPICE function ckmeta_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_ckmeta
  (JNIEnv * env, 
   jclass             J_class,
   jint               J_CKID,
   jstring            J_meta    )
{ 
   /*
   Local parameters
   */
   #define METALN          11

   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.ckmeta";
   static SpiceChar        meta      [ METALN ];

   /*
   Use f2c integers to interface with the f2c'd ckmeta routine.
   */
   integer                 CKID;
   integer                 code = 0;

   /*
   Get the input metadata type string. 
   */
   getFixedInputString_jni ( env, J_meta, METALN, meta );
   
   JNI_EXC_VAL ( env, ((jint)code) );

   CKID = (integer)J_CKID;

   ckmeta_ ( (integer    *) &CKID,
             (char       *) meta,
             (integer    *) &code,
             (ftnlen      ) strlen(meta) );

   SPICE_EXC_VAL( env, caller, (jint)code );

   /*
   Normal return. 
   */   
   return ( (jint)code );
}







/* 
Wrapper for CSPICE function ckobj_c 

NOTE: the returned array has no control area.
*/
JNIEXPORT jintArray JNICALL Java_spice_basic_CSPICE_ckobj
  (JNIEnv * env, 
   jclass             J_class,
   jstring            J_file,
   jint               J_size,
   jintArray          J_cover  )
{ 
   /*
   Local variables 
   */
   jintArray            retArray = 0;


   /*
   The cells below will be dynamically allocated.
   */
   SpiceCell             * cover;

   static SpiceChar      * caller   = "CSPICE.ckobj";
   static SpiceChar        file  [ FNAMLN ];

   SpiceInt                coverSize;
   SpiceInt                maxSize;
   SpiceInt              * coverData;


   /*
   Get the size of the input set data array. 
   */
   coverSize = (*env )->GetArrayLength ( env, J_cover );
   
   JNI_EXC_VAL  ( env, retArray );


   /*
   Capture the input CK name. 
   */
   getFixedInputString_jni ( env, J_file, FNAMLN, file );
   JNI_EXC_VAL  ( env, retArray );

 
   if ( coverSize > 0 )
   {
      /*
      Capture the contents of the input array `cover' in dynamic
      memory.  Check out and return if an exception is thrown.
      */
      getVecGI_jni ( env, J_cover, &coverSize, &coverData );
      JNI_EXC_VAL  ( env, retArray );
   }
   else
   {
      coverData = 0;
   }

   
   /*
   If the specified output cell size is smaller than the input
   array size, we have a problem. 
   */
   maxSize = (SpiceInt)J_size;

   if ( maxSize < coverSize )
   {
      /*
      We must free the data from the input array before
      returning. 
      */
      if ( coverSize > 0 )
      {
         freeVecGI_jni ( env, J_cover, coverData );
      }

      setmsg_c ( "Input cell size is #; output size is #;" );
      errint_c ( "#",  coverSize                           );
      errint_c ( "#",  maxSize                             );
      sigerr_c ( "SPICE(OUTPUTCELLTOOSMALL)"               );

      SPICE_EXC_VAL(env, caller, retArray );
   }

   /*
   Create a dynamically allocated cell of size maxSize to hold
   the results of our ckobj_c call. Initialize the cell with
   the data from the input array, if any.
   */
   cover = zzalcell_c ( SPICE_INT, maxSize, coverSize, 0, coverData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   At this point, we're holding on to a dynamically allocated
   cell. We can't return before freeing this cell, so we must
   be careful about how we handle errors.

   However, we're now done with the coverData array.
   */
   if ( coverSize > 0 )
   {
      freeVecGI_jni ( env, J_cover, coverData );   
   }
   

   /*
   Make the input cell into a set before passing it to ckobj_c. 
   */
   valid_c ( maxSize, coverSize, cover );

   if ( failed_c() )
   {
      /*
      Free the ID set before departure. 
      */
      zzdacell_c ( cover );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   We're finally ready for our CSPICE call. 
   */
   ckobj_c ( file, cover );


   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   At this point, the data portion of `cover' is exactly
   what we want to return. 
   */
   createVecGI_jni ( env, 
                     card_c(cover), 
                     (SpiceInt *)cover->data, 
                     &retArray                   );

   /*
   De-allocate the dynamic cell before departure.
   */
   zzdacell_c ( cover );

   /*
   Handle any JNI or SPICE error. 
   */
   JNI_EXC_VAL  ( env,         retArray );
   SPICE_EXC_VAL( env, caller, retArray );
   
   return ( retArray );
}



/* 
Wrapper for CSPICE function ckopn_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_ckopn
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_fname,
   jstring            J_ifname,   
   jint               J_ncomch )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.ckopn";
   static SpiceChar        fname  [ FNAMLN ];
   static SpiceChar        ifname [ IFNLEN ];

   SpiceInt                handle = 0;


   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_fname,  FNAMLN, fname  );
   getFixedInputString_jni ( env, J_ifname, IFNLEN, ifname );
   
   JNI_EXC_VAL( env, ((jint) handle) );


   ckopn_c ( fname, ifname, (SpiceInt)J_ncomch, &handle );

   SPICE_EXC_VAL( env, caller, ((jint)handle) );


   /*
   Normal return. 
   */
   return ( (jint)handle );
}



/* 
Wrapper for CSPICE function ckw01_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ckw01
  (JNIEnv             * env, 
   jclass             J_class, 
   jint               J_handle,
   jdouble            J_begtim,
   jdouble            J_endtim,
   jint               J_inst,
   jstring            J_ref,
   jboolean           J_avflag,
   jstring            J_segid,
   jint               J_nrec,
   jdoubleArray       J_sclkdp,
   jdoubleArray       J_quats,
   jdoubleArray       J_avvs     )
{
   /*
   Local constants
   */                                        

   /*
   Local variables and initializations
   */
   static SpiceChar      * caller    = "CSPICE.ckw01";

   static SpiceChar        ref    [ FRNMLN ];
   static SpiceChar        segid  [ SIDLEN ];

   SpiceDouble           * avvs;
   SpiceDouble           * quats;
   SpiceDouble           * sclkdp;

   SpiceInt                nrec;
   SpiceInt                asize;
   SpiceInt                qsize;
   SpiceInt                csize;

   /*
   Fetch the frame name and segment ID.
   */
   getFixedInputString_jni ( env, J_ref,   FRNMLN, ref   );
   getFixedInputString_jni ( env, J_segid, SIDLEN, segid );

   JNI_EXC( env );

   
   /*
   Fetch the quaternion array into a one-dimensional C array.
   Both Java and C use row-major order, so the quaternions
   are correctly ordered. 

   Fetch the angular velocity vectors and epochs as well.
   */
   getVecGD_jni ( env, J_quats,  &qsize, &quats );
   JNI_EXC( env );

   getVecGD_jni ( env, J_avvs,   &asize, &avvs );
   JNI_EXC( env );

   getVecGD_jni ( env, J_sclkdp, &csize, &sclkdp );
   JNI_EXC( env );

 
   /*
   Check input quaternion, avv, and sclkdp array sizes against nrec. 
   */
   nrec = (SpiceInt) J_nrec;

   if (    ( csize     != nrec )
        || ( qsize/4   != nrec )
        || ( asize/3   != nrec )  )
   {
      setmsg_c ( "Input array sizes were: "
                 "quats: #; avvs: #, sclkdp: #. "
                 "The sizes of these arrays should be "
                 "the record count (#) times the respective "
                 "object sizes."                             );
      errint_c ( "#", qsize                                  );
      errint_c ( "#", asize                                  );
      errint_c ( "#", csize                                  );
      errint_c ( "#", nrec                                   );
      sigerr_c ( "SPICE(DIMENSIONMISMATCH)"                  );

      /*
      Free the dynamically allocated arrays.
      */
      freeVecGD_jni ( env, J_quats,  quats  );
      freeVecGD_jni ( env, J_avvs,   avvs   );
      freeVecGD_jni ( env, J_sclkdp, sclkdp );

      SPICE_EXC( env, caller );
   }

 

   ckw01_c (  (SpiceInt)     J_handle,
              (SpiceDouble)  J_begtim,
              (SpiceDouble)  J_endtim,
              (SpiceInt)     J_inst,
              ref,
              (SpiceBoolean) J_avflag,
              segid,
              nrec,
              sclkdp,
              quats,
              avvs                     );


   /*
   Free the dynamically allocated arrays.
   */
   freeVecGD_jni ( env, J_quats,  quats  );
   freeVecGD_jni ( env, J_avvs,   avvs   );
   freeVecGD_jni ( env, J_sclkdp, sclkdp );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function ckw02_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ckw02
  (JNIEnv             * env, 
   jclass             J_class, 
   jint               J_handle,
   jdouble            J_begtim,
   jdouble            J_endtim,
   jint               J_inst,
   jstring            J_ref,
   jstring            J_segid,
   jint               J_nrec,
   jdoubleArray       J_start,
   jdoubleArray       J_stop,
   jdoubleArray       J_quats,
   jdoubleArray       J_avvs,
   jdoubleArray       J_rates   )
{
   /*
   Local constants
   */                                        

   /*
   Local variables and initializations
   */
   static SpiceChar      * caller    = "CSPICE.ckw02";

   static SpiceChar        ref    [ FRNMLN ];
   static SpiceChar        segid  [ SIDLEN ];

   SpiceDouble           * avvs;
   SpiceDouble           * quats;
   SpiceDouble           * rates;
   SpiceDouble           * start;
   SpiceDouble           * stop;

   SpiceInt                nrec;
   SpiceInt                qsize;
   SpiceInt                asize;
   SpiceInt                bsize;
   SpiceInt                esize;
   SpiceInt                rsize;

   /*
   Fetch the frame name and segment ID.
   */
   getFixedInputString_jni ( env, J_ref,   FRNMLN, ref   );
   getFixedInputString_jni ( env, J_segid, SIDLEN, segid );

   JNI_EXC( env );

   
   /*
   Fetch the interval start and stop times. 
   */
   getVecGD_jni ( env, J_start, &bsize, &start );
   JNI_EXC( env );

   getVecGD_jni ( env, J_stop,  &esize, &stop  );
   JNI_EXC( env );


   /*
   Fetch the quaternion array into a one-dimensional C array.
   Both Java and C use row-major order, so the quaternions
   are correctly ordered. 

   Fetch the angular velocity vectors and rates as well.
   */
   getVecGD_jni ( env, J_quats, &qsize, &quats );
   JNI_EXC( env );

   getVecGD_jni ( env, J_avvs,  &asize, &avvs );
   JNI_EXC( env );

   getVecGD_jni ( env, J_rates, &rsize, &rates );
   JNI_EXC( env );


   /*
   Check input array sizes against nrec. 
   */
   nrec = (SpiceInt) J_nrec;

   if (    ( bsize     != nrec )
        || ( esize     != nrec )
        || ( qsize/4   != nrec )
        || ( asize/3   != nrec )
        || ( rsize     != nrec )  )
   {
      setmsg_c ( "Input array sizes were: start: #; "
                 "stop: #; quats: #; avvs: #, rates: #. "
                 "The sizes of these arrays should be "
                 "the record count (#) times the respective "
                 "object sizes."                             );
      errint_c ( "#", bsize                                  );
      errint_c ( "#", esize                                  );
      errint_c ( "#", qsize                                  );
      errint_c ( "#", asize                                  );
      errint_c ( "#", rsize                                  );
      errint_c ( "#", nrec                                   );
      sigerr_c ( "SPICE(DIMENSIONMISMATCH)"                  );

      /*
      Free the dynamically allocated arrays.
      */
      freeVecGD_jni ( env, J_start, start );
      freeVecGD_jni ( env, J_stop,  stop );
      freeVecGD_jni ( env, J_quats, quats );
      freeVecGD_jni ( env, J_avvs,  avvs  );
      freeVecGD_jni ( env, J_rates, rates );

      SPICE_EXC( env, caller );

   }


   ckw02_c (  (SpiceInt)    J_handle,
              (SpiceDouble) J_begtim,
              (SpiceDouble) J_endtim,
              (SpiceInt)    J_inst,
              ref,
              segid,
              nrec,
              start,
              stop,
              quats,
              avvs,
              rates                   );


   /*
   Free the dynamically allocated arrays.
   */
   freeVecGD_jni ( env, J_start, start );
   freeVecGD_jni ( env, J_stop,  stop  );
   freeVecGD_jni ( env, J_quats, quats );
   freeVecGD_jni ( env, J_avvs,  avvs  );
   freeVecGD_jni ( env, J_rates, rates );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function ckw03_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ckw03
  (JNIEnv             * env, 
   jclass             J_class, 
   jint               J_handle,
   jdouble            J_begtim,
   jdouble            J_endtim,
   jint               J_inst,
   jstring            J_ref,
   jboolean           J_avflag,
   jstring            J_segid,
   jint               J_nrec,
   jdoubleArray       J_sclkdp,
   jdoubleArray       J_quats,
   jdoubleArray       J_avvs,
   jint               J_nints,
   jdoubleArray       J_starts   )
{
   /*
   Local constants
   */                                        

   /*
   Local variables and initializations
   */
   static SpiceChar      * caller    = "CSPICE.ckw03";

   static SpiceChar        ref    [ FRNMLN ];
   static SpiceChar        segid  [ SIDLEN ];

   SpiceDouble           * avvs;
   SpiceDouble           * quats;
   SpiceDouble           * sclkdp;
   SpiceDouble           * starts;

   SpiceInt                nints;
   SpiceInt                nrec;
   SpiceInt                asize;
   SpiceInt                qsize;
   SpiceInt                csize;
   SpiceInt                ssize;

   /*
   Fetch the frame name and segment ID.
   */
   getFixedInputString_jni ( env, J_ref,   FRNMLN, ref   );
   getFixedInputString_jni ( env, J_segid, SIDLEN, segid );

   JNI_EXC( env );

   
   /*
   Fetch the quaternion array into a one-dimensional C array.
   Both Java and C use row-major order, so the quaternions
   are correctly ordered. 

   Fetch the angular velocity vectors and epochs as well.
   */
   getVecGD_jni ( env, J_quats,  &qsize, &quats );
   JNI_EXC( env );

   getVecGD_jni ( env, J_avvs,   &asize, &avvs );
   JNI_EXC( env );

   getVecGD_jni ( env, J_sclkdp, &csize, &sclkdp );
   JNI_EXC( env );

   /*
   Fetch the interpolation interval start times. 
   */
   getVecGD_jni ( env, J_starts, &ssize, &starts );
   JNI_EXC( env );


   /*
   Check input quaternion, avv, and sclkdp array sizes against nrec. 
   */
   nrec = (SpiceInt) J_nrec;

   if (    ( csize     != nrec )
        || ( qsize/4   != nrec )
        || ( asize/3   != nrec )  )
   {
      setmsg_c ( "Input array sizes were: "
                 "quats: #; avvs: #, sclkdp: #. "
                 "The sizes of these arrays should be "
                 "the record count (#) times the respective "
                 "object sizes."                             );
      errint_c ( "#", qsize                                  );
      errint_c ( "#", asize                                  );
      errint_c ( "#", csize                                  );
      errint_c ( "#", nrec                                   );
      sigerr_c ( "SPICE(DIMENSIONMISMATCH)"                  );

      /*
      Free the dynamically allocated arrays.
      */
      freeVecGD_jni ( env, J_quats,  quats  );
      freeVecGD_jni ( env, J_avvs,   avvs   );
      freeVecGD_jni ( env, J_sclkdp, sclkdp );
      freeVecGD_jni ( env, J_starts, starts );

      SPICE_EXC( env, caller );
   }

   /*
   Check the input interval start time array size against nints. 
   */   
   nints = (SpiceInt) J_nints;

   if ( ssize != nints  )
   {
      setmsg_c ( "Input interpolation interval array size "
                 "was #; this size should match the "
                 "input interpolation interval count #."     );
      errint_c ( "#", ssize                                  );
      errint_c ( "#", nrec                                   );
      sigerr_c ( "SPICE(DIMENSIONMISMATCH)"                  );

      /*
      Free the dynamically allocated arrays.
      */
      freeVecGD_jni ( env, J_quats,  quats  );
      freeVecGD_jni ( env, J_avvs,   avvs   );
      freeVecGD_jni ( env, J_sclkdp, sclkdp );
      freeVecGD_jni ( env, J_starts, starts );

      SPICE_EXC( env, caller );
   }



   ckw03_c (  (SpiceInt)     J_handle,
              (SpiceDouble)  J_begtim,
              (SpiceDouble)  J_endtim,
              (SpiceInt)     J_inst,
              ref,
              (SpiceBoolean) J_avflag,
              segid,
              nrec,
              sclkdp,
              quats,
              avvs,
              nints,
              starts                   );


   /*
   Free the dynamically allocated arrays.
   */
   freeVecGD_jni ( env, J_quats,  quats  );
   freeVecGD_jni ( env, J_avvs,   avvs   );
   freeVecGD_jni ( env, J_sclkdp, sclkdp );
   freeVecGD_jni ( env, J_starts, starts );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function clight_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_clight
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)clight_c()  );
}



/* 
Wrapper for CSPICE function clpool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_clpool
  (JNIEnv * env, jclass J_class)
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.clpool";

 
   clpool_c();
 

   SPICE_EXC ( env, caller );
}



/* 
Wrapper for CSPICE function conics_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_conics
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_elts,
   jdouble            J_et     )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.conics";

   SpiceDouble             elts   [8];
   SpiceDouble             state  [6];

   
   /*
   Fetch the input elements into a C array. 
   */
   getVecFixedD_jni ( env, J_elts, 8, elts );
  
   JNI_EXC_VAL ( env, retArray );


   conics_c ( elts,  (SpiceDouble)J_et,  state );

   SPICE_EXC_VAL( env, caller, retArray );


   createVecGD_jni ( env, 6, state, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function convrt_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_convrt
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jdouble       J_x,
    jstring       J_in,
    jstring       J_out ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.convrt";
   static SpiceChar        in  [UNITLN];
   static SpiceChar        out [UNITLN];

   SpiceDouble             y  =  0.0;


   /*
   Capture the unit strings in local variables.
   */    
   getFixedInputString_jni ( env, J_in,  UNITLN, in  );
   getFixedInputString_jni ( env, J_out, UNITLN, out );
   
   JNI_EXC_VAL ( env, ((jdouble)y) );
   
 
   convrt_c ( J_x, in, out, &y );
   

   /*
   If the lookup resulted in a SPICE error, throw an exception. 
   */
   SPICE_EXC_VAL ( env, caller, ((jdouble)y) );

   /*
   Normal return. 
   */
   return ( (jdouble)y );
}



/* 
Wrapper for CSPICE function cvpool_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_cvpool
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_agent ) 
{
   /*
   Local variables 
   */
   SpiceBoolean            update = 0;

   static SpiceChar        agent [ KVNMLN ];
   static SpiceChar      * caller = "CSPICE.cvpool";   

   /*
   Fetch the agent name.
   */
   getFixedInputString_jni ( env, J_agent, KVNMLN, agent );

   JNI_EXC_VAL( env, ((jboolean)update) );


   cvpool_c ( agent, &update );


   SPICE_EXC_VAL( env, caller, ((jboolean)update) );

   return ( (jboolean) update );
}



/* 
Wrapper for CSPICE function cyllat_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_cyllat
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_r,
   jdouble            J_lon,
   jdouble            J_z     )
{
 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             result[3];


   cyllat_c ( (SpiceDouble) J_r, 
              (SpiceDouble) J_lon, 
              (SpiceDouble) J_z, 
              result,
              result+1,
              result+2             );

   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function cylrec_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_cylrec
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_r,
   jdouble            J_lon,
   jdouble            J_z     )
{
 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             result[3];


   cylrec_c ( (SpiceDouble) J_r, 
              (SpiceDouble) J_lon, 
              (SpiceDouble) J_z, 
              result               );

   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function cylsph_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_cylsph
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_r,
   jdouble            J_lon,
   jdouble            J_z     )
{
 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             result[3];


   cylsph_c ( (SpiceDouble) J_r, 
              (SpiceDouble) J_lon, 
              (SpiceDouble) J_z, 
              result,
              result+1,
              result+2             );

   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function dafac_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dafac
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle,
   jobjectArray       J_buffer  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.dafac";

   SpiceInt                fStrLen;
   SpiceInt                handle;

   SpiceInt                nStr;

   void                  * fStrArray;


   /*
   Fetch the input string buffer into a dynamically allocated
   array of Fortran-style strings. 
  */
   getFortranStringArray_jni ( env,   J_buffer, 
                               &nStr, &fStrLen, &fStrArray );
   JNI_EXC( env );


   /*
   Add the contents of the buffer to the DAF. 
   */
   
   handle = (SpiceInt) J_handle;

   dafac_ ( (integer *) &handle,
            (integer *) &nStr,
            (char    *) fStrArray,
            (ftnlen   ) fStrLen    );

   /*
   Always free the Fortran string array.
   */
   free ( fStrArray );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function dafbbs_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dafbbs
  (JNIEnv      * env, 
   jclass        J_class, 
   jint          J_handle )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.dafbbs";


   dafbbs_c ( (SpiceInt)J_handle );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function dafbfs_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dafbfs
  (JNIEnv      * env, 
   jclass        J_class, 
   jint          J_handle )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.dafbfs";


   dafbfs_c ( (SpiceInt)J_handle );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function dafcls_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dafcls
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_handle ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.dafcls";


   dafcls_c ( (SpiceInt)J_handle );


   SPICE_EXC ( env, caller );
}



/* 
Wrapper for CSPICE function dafcs_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dafcs
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_handle ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.dafcs";


   dafcs_c ( (SpiceInt)J_handle );


   SPICE_EXC ( env, caller );
}



/* 
Wrapper for CSPICE function dafdc_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dafdc
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.dafdc";

   
   dafdc_c ( (SpiceInt)J_handle );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function dafec_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dafec
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_bufsiz,
   jint               J_lenout,
   jintArray          J_n,
   jobjectArray       J_buffer,
   jbooleanArray      J_done    )
{
   /*
   Local variables 
   */
   jstring                 bufferElt;

   SpiceBoolean            done;

   SpiceChar             * buffer;
   static SpiceChar      * caller = "CSPICE.dafec";
   SpiceChar             * strPtr;

   SpiceInt                bufsiz;
   SpiceInt                i;
   SpiceInt                lenout ;
   SpiceInt                n;
   SpiceInt                nBytes;


   /*
   Dynamically allocate an array of C strings to hold
   the indicated size block of comment data.
   */
   lenout =  (SpiceInt) J_lenout;
   bufsiz =  (SpiceInt) J_bufsiz;

   if ( bufsiz == 0  )
   {
      n    = 0;
      done = SPICEFALSE;

      updateVecGI_jni ( env, 1, &n,    J_n    );
      updateVecGB_jni ( env, 1, &done, J_done );      

      return;
   }

   nBytes = bufsiz * lenout * sizeof(SpiceChar);

   buffer = (void *)alloc_SpiceMemory ( (size_t)nBytes );

   SPICE_EXC( env, caller );


   /*
   Fetch the comment area data that fits in a buffer of
   the specified dimensions.
   */
   dafec_c ( (SpiceInt) J_handle,
             bufsiz,
             lenout,
             &n,
             buffer,
             &done               );


   /*
   Set the output arguments. 

   The first step will be to create a Java string for
   each element of `buffer' and update the corresponding
   elements of J_buffer with these Java strings.
   */
   for ( i = 0;  i < n;  i++ )
   {
      strPtr    = buffer + ( i * lenout );

      bufferElt = createJavaString_jni ( env, strPtr );

      (*env)->SetObjectArrayElement ( env, J_buffer, i, bufferElt );
   }

   /*
   Always de-allocate the buffer of C-style strings. 
   */
   free_SpiceMemory ( (void *)  buffer );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Update the other outputs.
   */
   updateVecGI_jni ( env, 1, &n,    J_n    );
   updateVecGB_jni ( env, 1, &done, J_done );
}



/* 
Wrapper for CSPICE function daffna_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_daffna
  (JNIEnv      * env, 
   jclass        cls  )
{
   /*
   Local variables 
   */
   SpiceBoolean            found  = 0;

   static SpiceChar      * caller = "CSPICE.daffna";


   daffna_c ( &found );

   SPICE_EXC_VAL( env, caller, ((jboolean)found) );

   return ( (jboolean)found );
}



/* 
Wrapper for CSPICE function daffpa_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_daffpa
  (JNIEnv      * env, 
   jclass        cls  )
{
   /*
   Local variables 
   */
   SpiceBoolean            found  = 0;

   static SpiceChar      * caller = "CSPICE.daffpa";


   daffpa_c ( &found );

   SPICE_EXC_VAL( env, caller, ((jboolean)found) );

   return ( (jboolean)found );
}



/* 
Wrapper for CSPICE function dafgda_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_dafgda
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_begin,
   jint               J_end    )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.dafgda";

   SpiceInt                begin;
   SpiceInt                end;
   SpiceInt                size;
 
   SpiceDouble           * dpData;


   /*
   Allocate a d.p. array large enough to hold the requested
   data.
   */
   begin = (SpiceInt)J_begin;
   end   = (SpiceInt)J_end;

   size =  end - begin + 1;

   dpData = alloc_SpiceDouble_C_array ( 1, size );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Fetch the data. 
   */
   dafgda_c ( (SpiceInt)J_handle, begin, end, dpData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create a Java array to hold the output data.
   */
   createVecGD_jni ( env, size, dpData, &retArray );

   /*
   De-allocate the C array. 
   */
   free_SpiceMemory ( (void *)  dpData );

   return ( retArray );
}



/* 
Wrapper for CSPICE function dafgn_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_dafgn
  ( JNIEnv     *  env, 
    jclass        cls  ) 
{
   /*
   Local constants 
   */
   #define RECLEN          1024
   #define ANAMLN        ( RECLEN + 1 ) 

   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.dafgn";
   static SpiceChar        segid  [ ANAMLN ];
   

   dafgn_c ( ANAMLN, segid );


   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jstring)0) );

   /*
   Normal return. 
   */ 
   return (  createJavaString_jni( env, segid )  );
}



/* 
Wrapper for CSPICE function dafgs_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_dafgs
  (JNIEnv * env, 
   jclass             J_class,
   jint               J_size   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.dafgs";

   SpiceInt                size;
 
   SpiceDouble           * dpData;


   /*
   Allocate a d.p. array large enough to hold the requested
   data.
   */
   size   = (SpiceInt)J_size;

   dpData = alloc_SpiceDouble_C_array ( 1, size );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Fetch the data. 
   */
   dafgs_c ( dpData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create a Java array to hold the output data.
   */
   createVecGD_jni ( env, size, dpData, &retArray );

   /*
   De-allocate the C array. 
   */
   free_SpiceMemory ( (void *)  dpData );

   return ( retArray );
}



/* 
Wrapper for CSPICE function dafgsr_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_dafgsr
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_recno,
   jint               J_begin,
   jint               J_end    )
{
   /*
   Local constants 

   DAF record size in units of d.p. numbers.
   */
   #define DAFRSZ         128
 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceBoolean            found;

   static SpiceChar      * caller = "CSPICE.dafgsr";
   static SpiceChar        fname   [ FNAMLN ];
   static SpiceChar        message [ LMSGLN ];

   static SpiceDouble      dscrec  [ DAFRSZ ];

   SpiceInt                begin;
   SpiceInt                end;
   SpiceInt                handle;
   SpiceInt                recno;
   SpiceInt                size;


   /*
   Attempt to fetch data from the specified descriptor record. 
   */
   handle = (SpiceInt) J_handle;
   recno  = (SpiceInt) J_recno;
   begin  = (SpiceInt) J_begin;
   end    = (SpiceInt) J_end;

   dafgsr_c ( handle, recno, begin, end, dscrec, &found );

              
   SPICE_EXC_VAL ( env, caller, retArray );

   /*
   If we didn't find the requested record, throw an exception. 
   */
   if ( !found )
   {
      /*
      Get the name of the DAF file.
      */
      dafhfn_ ( (integer *) &handle,
                (char    *) fname,
                (ftnlen   ) LMSGLN-1 );

      strncpy ( message, 
                "Record number # was not found in DAF file #.",
                LMSGLN                                         );
      /*
      Substitute the record number and DAF name into the message. 
      */
      repmi_c ( message, "#", recno, LMSGLN, message );
      repmc_c ( message, "#", fname, LMSGLN, message );

      /*
      Throw the exception and return. 
      */
      zzThrowException_jni ( env, DAFRECNF_EXC, message );

      return ( retArray );
   }

   /*
   Create the output array. 
   */
   size = end - begin + 1;

   createVecGD_jni ( env, size, dscrec, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function dafopr_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_dafopr
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_fname ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.dafopr";
   static SpiceChar      * fname;

   SpiceInt                fileLen;
   SpiceInt                handle  = 0;


   /*
   Fetch input string into dynmically allocated memory. 
   Check for a JNI exception.
   */
   getVarInputString_jni ( env, J_fname, &fileLen, &fname );

   JNI_EXC_VAL( env, ((jint)handle) );


   /*
   Open the file. 
   */
   dafopr_c ( fname, &handle );


   /*
   Free the dynamically allocated memory for the file name.
   */
   freeVarInputString_jni ( env, J_fname, fname );

   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jint)handle) );
   
   return ( (jint) handle );
}



/* 
Wrapper for CSPICE function dafopw_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_dafopw
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_fname ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.dafopw";
   static SpiceChar      * fname;

   SpiceInt                fileLen;
   SpiceInt                handle  = 0;


   /*
   Fetch input string into dynmically allocated memory. 
   Check for a JNI exception.
   */
   getVarInputString_jni ( env, J_fname, &fileLen, &fname );

   JNI_EXC_VAL( env, ((jint)handle) );


   /*
   Open the file. 
   */
   dafopw_c ( fname, &handle );


   /*
   Free the dynamically allocated memory for the file name.
   */
   freeVarInputString_jni ( env, J_fname, fname );

   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jint)handle) );
   
   return ( (jint) handle );
}



/* 
Wrapper for CSPICE function dafrfr_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dafrfr
  (JNIEnv           * env, 
   jclass             J_class,
   jint               J_handle,
   jintArray          J_nd,
   jintArray          J_ni, 
   jobjectArray       J_ifname,
   jintArray          J_bward,
   jintArray          J_fward, 
   jintArray          J_free   )
{
   /*
   Local variables 
   */
   jstring                 jIfname;
 
   static SpiceChar      * caller = "CSPICE.dafrfr";
   static SpiceChar        ifname [ IFNLEN ];

   SpiceInt                bward;
   SpiceInt                free;
   SpiceInt                fward;
   SpiceInt                nd;
   SpiceInt                ni;


   /*
   Fetch the requested file record, if possible. 
   */
   dafrfr_c ( (SpiceInt)J_handle,  IFNLEN,  &nd,     &ni,    
              ifname,              &bward,  &fward,  &free );

   SPICE_EXC( env, caller );

   /*
   Set the numeric output arguments. Each argument is an array whose 
   elements will be updated. 
   */
   
   updateVecGI_jni ( env, 1, &nd,    J_nd    );
   updateVecGI_jni ( env, 1, &ni,    J_ni    );
   updateVecGI_jni ( env, 1, &bward, J_bward );
   updateVecGI_jni ( env, 1, &fward, J_fward );
   updateVecGI_jni ( env, 1, &free,  J_free  );

   JNI_EXC( env );

   /*
   Create a new Java string to hold the internal file name. 
   */
   jIfname = createJavaString_jni ( env, ifname );

   /*
   Update the first element of J_ifname with a reference
   to jIfname.
   */

   (*env)->SetObjectArrayElement ( env, J_ifname, 0, jIfname );
}



/* 
Wrapper for CSPICE function dafus_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dafus
(  JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_sum, 
   jint               J_nd,
   jint               J_ni,
   jdoubleArray       J_dc, 
   jintArray          J_ic        )

{
   /*
   Local constants 
   */

   /*
   Local variables 
   */ 
   static SpiceChar      * caller = "CSPICE.dafus";

   SpiceDouble           * sum;
   SpiceDouble           * dc;

   SpiceInt                dscsiz;
   SpiceInt              * ic;
   SpiceInt                nd;
   SpiceInt                ni; 


   /*
   Fetch the input summary into a dynamically allocated
   d.p. array. 
   */
   getVecGD_jni ( env, J_sum, &dscsiz, &sum );

   JNI_EXC( env );

   /*
   Allocate memory to hold the d.p. and integer 
   components of the descriptor. 
   */
   nd = J_nd;
   ni = J_ni; 

   dc = alloc_SpiceDouble_C_array ( 1, nd );
   ic = alloc_SpiceInt_C_array    ( 1, ni );

   dafus_c ( sum, nd, ni, dc, ic );

   /*
   Free the dynamically allocated descriptor array. 
   */
   freeVecGD_jni ( env, J_sum, sum );


   if ( failed_c() )   {
      /*
      De-allocate the descriptor component arrays. 
      */
      free_SpiceMemory ( (void *)  dc );
      free_SpiceMemory ( (void *)  ic );

      /*
      Throw an exception and return. 
      */
      SPICE_EXC( env, caller );
   }
   
   /*
   Update the output arrays using the contents of arrays 
   `dc' and `ic'.
   */
   updateVecGD_jni ( env, nd, dc, J_dc );
   updateVecGI_jni ( env, ni, ic, J_ic );

   /*
   De-allocate the descriptor component arrays. 
   */
   free_SpiceMemory ( (void *)  dc );
   free_SpiceMemory ( (void *)  ic );   

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function dcyldr_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_dcyldr
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_x, 
   jdouble            J_y, 
   jdouble            J_z    )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.dcyldr";

   SpiceDouble             jacob [3][3];

   /*
   Get the Jacobian matrix.
   */
   dcyldr_c ( (SpiceDouble) J_x,
              (SpiceDouble) J_y,
              (SpiceDouble) J_z,  jacob );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function deltet_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_deltet
  ( JNIEnv *  env, 
    jclass    cls,
    jdouble   J_epoch,
    jstring   J_eptype  ) 
{

   /*
   Local variables
   */
   static SpiceChar      * caller = "CSPICE.deltet";
   static SpiceChar      * eptype;

   SpiceDouble             delta;

   SpiceInt                eptypeLen;

   /*
   Fetch the epoch type. 
   */
   getVarInputString_jni ( env, J_eptype, &eptypeLen, &eptype );

   JNI_EXC_VAL( env, ((jdouble)0.0) );


   deltet_c ( (SpiceDouble)J_epoch, eptype, &delta );

   
   /*
   Always free the dynamically allocated string holding the 
   epoch type. 
   */
   freeVarInputString_jni ( env, J_eptype, eptype );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jdouble)0.0) );

   /*
   Normal return. 
   */
   return (  (jdouble)delta  );
}



/* 
Wrapper for CSPICE function det_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_det
  ( JNIEnv     *  env, 
    jclass        cls,
    jobjectArray  J_m )
{

   /*
   Local variables
   */
   static SpiceChar      * caller = "CSPICE.det";


   SpiceDouble             det = 0.0;
   SpiceDouble             m    [3][3];


   /*
   Get the input matrix in a C array. 
   */
   getMat33D_jni ( env, J_m, m );

   JNI_EXC_VAL( env, ((jdouble)det) );

   det = det_c ( m );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jdouble)det) );

   /*
   Normal return. 
   */
   return (  (jdouble)det  );
}



/* 
Wrapper for CSPICE function dgeodr_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_dgeodr
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_x, 
   jdouble            J_y, 
   jdouble            J_z,
   jdouble            J_re,
   jdouble            J_f     )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.dgeodr";

   SpiceDouble             jacob [3][3];

   /*
   Get the Jacobian matrix.
   */
   dgeodr_c ( (SpiceDouble) J_x,
              (SpiceDouble) J_y,
              (SpiceDouble) J_z,  
              (SpiceDouble) J_re,  
              (SpiceDouble) J_f,  
              jacob              );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function dlatdr_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_dlatdr
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_x, 
   jdouble            J_y, 
   jdouble            J_z    )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.dlatdr";

   SpiceDouble             jacob [3][3];

   /*
   Get the Jacobian matrix.
   */
   dlatdr_c ( (SpiceDouble) J_x,
              (SpiceDouble) J_y,
              (SpiceDouble) J_z,  jacob );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function dpgrdr_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_dpgrdr
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_body,
   jdouble            J_x, 
   jdouble            J_y, 
   jdouble            J_z,
   jdouble            J_re,
   jdouble            J_f     )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar        body [ BDNMLN ];
   static SpiceChar      * caller = "CSPICE.dpgrdr";

   SpiceDouble             jacob [3][3];

   /*
   Get the input string.
   */
   getFixedInputString_jni ( env, J_body, BDNMLN, body );

   /*
   Get the Jacobian matrix.
   */
   dpgrdr_c ( body,
              (SpiceDouble) J_x,
              (SpiceDouble) J_y,
              (SpiceDouble) J_z,  
              (SpiceDouble) J_re,  
              (SpiceDouble) J_f,  
              jacob              );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function dpr_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_dpr
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)dpr_c()  );
}



/* 
Wrapper for CSPICE function drdgeo_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_drdgeo
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_longitude, 
   jdouble            J_latitude,
   jdouble            J_altitude,
   jdouble            J_re,
   jdouble            J_f         )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.drdgeo";

   SpiceDouble             jacob [3][3];

   /*
   Get the Jacobian matrix.
   */
   drdgeo_c ( (SpiceDouble) J_longitude,
              (SpiceDouble) J_latitude, 
              (SpiceDouble) J_altitude, 
              (SpiceDouble) J_re, 
              (SpiceDouble) J_f, 
              jacob                     );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function drdcyl_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_drdcyl
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_radius, 
   jdouble            J_longitude, 
   jdouble            J_z    )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.drdcyl";

   SpiceDouble             jacob [3][3];

   /*
   Get the Jacobian matrix.
   */
   drdcyl_c ( (SpiceDouble) J_radius,
              (SpiceDouble) J_longitude,
              (SpiceDouble) J_z,       
              jacob                    );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function drdlat_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_drdlat
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_radius, 
   jdouble            J_longitude, 
   jdouble            J_latitude    )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.drdlat";

   SpiceDouble             jacob [3][3];

   /*
   Get the Jacobian matrix.
   */
   drdlat_c ( (SpiceDouble) J_radius,
              (SpiceDouble) J_longitude,
              (SpiceDouble) J_latitude,     jacob );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}


/* 
Wrapper for CSPICE function drdpgr_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_drdpgr
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_body,
   jdouble            J_longitude, 
   jdouble            J_latitude,
   jdouble            J_altitude,
   jdouble            J_re,
   jdouble            J_f         )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar        body [ BDNMLN ];
   static SpiceChar      * caller = "CSPICE.drdpgr";

   SpiceDouble             jacob [3][3];

   /*
   Get the input string.
   */
   getFixedInputString_jni ( env, J_body, BDNMLN, body );

   /*
   Get the Jacobian matrix.
   */
   drdpgr_c ( body,
              (SpiceDouble) J_longitude,
              (SpiceDouble) J_latitude, 
              (SpiceDouble) J_altitude, 
              (SpiceDouble) J_re, 
              (SpiceDouble) J_f, 
              jacob                     );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function drdsph_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_drdsph
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_radius, 
   jdouble            J_colatitude,
   jdouble            J_longitude   )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.drdsph";

   SpiceDouble             jacob [3][3];

   /*
   Get the Jacobian matrix.
   */
   drdsph_c ( (SpiceDouble) J_radius,
              (SpiceDouble) J_colatitude,
              (SpiceDouble) J_longitude,       
              jacob                    );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}





/* 
Wrapper for CSPICE function dskb02_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dskb02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc,
   jintArray         J_nv,
   jintArray         J_np,
   jintArray         J_nvxtot,
   jobjectArray      J_vtxbds,
   jdoubleArray      J_voxsiz,    
   jdoubleArray      J_voxori,
   jintArray         J_vgrext,
   jintArray         J_cgscal,
   jintArray         J_vtxnpl,
   jintArray         J_voxnpt,
   jintArray         J_voxnpl     )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dskb02";

   SpiceDLADescr           dladsc;

   SpiceDouble             voxori [3];
   SpiceDouble             voxsiz;
   SpiceDouble             vtxbds [3][2];

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                cgscal;
   SpiceInt                handle;
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt                nvxtot;
   SpiceInt                vgrext [3];
   SpiceInt                voxnpl;
   SpiceInt                voxnpt;
   SpiceInt                vtxnpl;



   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC( env );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC( env, caller );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Read the bookkeeping parameters.
   */
   handle =  (SpiceInt) J_handle;

   dskb02_c ( handle,   &dladsc,  &nv,      &np,
              &nvxtot,  vtxbds,   &voxsiz,  voxori,
              vgrext,   &cgscal,  &vtxnpl,  &voxnpt,  &voxnpl );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Set the output arguments. 
   */
   updateVecGI_jni ( env,  1,      &nv,     J_nv     );
   updateVecGI_jni ( env,  1,      &np,     J_np     );
   updateVecGI_jni ( env,  1,      &nvxtot, J_nvxtot );
   
   updateMatGD_jni ( env,  3,  2,  vtxbds,  J_vtxbds );

   updateVecGD_jni ( env,  1,      &voxsiz, J_voxsiz );
   updateVecGD_jni ( env,  3,      voxori,  J_voxori );

   updateVecGI_jni ( env,  3,      vgrext,  J_vgrext );
   updateVecGI_jni ( env,  1,      &cgscal, J_cgscal );
   updateVecGI_jni ( env,  1,      &vtxnpl, J_vtxnpl );
   updateVecGI_jni ( env,  1,      &voxnpt, J_voxnpt );
   updateVecGI_jni ( env,  1,      &voxnpl, J_voxnpl );

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function dskd02_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_dskd02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jint              J_item,
   jint              J_start,
   jint              J_room   )

{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static ConstSpiceChar * caller = "CSPICE.dskd02";

   SpiceDLADescr           dladsc;

   SpiceDouble           * dpPtr;

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;
   SpiceInt                item;
   SpiceInt                n;
   SpiceInt                needed;
   SpiceInt                room;
   SpiceInt                start;


   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC_VAL( env, retArray );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Find out how many values we'll need to fetch.
   */
   handle = (SpiceInt) J_handle;
   start  = (SpiceInt) J_start;
   room   = (SpiceInt) J_room;
   item   = (SpiceInt) J_item;

   /*
   Allocate dynamic memory in which to store the data. We'll
   always allocate at least one int, even if `room' is 
   non-positive. We'll let the CSPICE routine signal an error
   if necessary. 
   */

   needed = maxi_c( 2, room, 1 );

   dpPtr = alloc_SpiceDouble_C_array( 1, needed );

   /*
   Fetch the data.
   */
   dskd02_c ( handle, &dladsc, item, start, room, &n, dpPtr );

   /*
   Create the output array. If there are no data to fetch, create
   an empty array. DO NOT return a null pointer!
   */
   if ( failed_c() ) 
   {
      n = 0;
   }

   /*
   Create the output array. If there are no data to fetch, create
   an empty array. DO NOT return a null pointer!
   */
   createVecGD_jni ( env, n, dpPtr, &retArray );

   /*
   Free the dynamically allocated array. 
   */
   free_SpiceMemory( (void *)  dpPtr );

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Return the data array. 
   */
   return( retArray );
}



/* 
Wrapper for CSPICE function dskgd_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_dskgd
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static ConstSpiceChar * caller = "CSPICE.dskgd";

   SpiceDouble             DSKDescrArray [SPICE_DSK_DSCSIZ];

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];



   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC_VAL( env, retArray );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Look up the DSK descriptor from the f2c'd dsklib routine.
   */
   dskgd_ ( (integer    *) &J_handle, 
            (integer    *) DLADescrArray, 
            (doublereal *) DSKDescrArray  );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create a Java return value.
   */
   createVecGD_jni( env, SPICE_DSK_DSCSIZ, (SpiceDouble *)DSKDescrArray, 
                    &retArray );

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   return( retArray );
}





/* 
Wrapper for CSPICE function dskgtl_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_dskgtl
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_keywrd )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dskgtl";

   SpiceDouble             dpval  = 0;


 
   /*
   Look up the value.
   */
   dskgtl_c ( (SpiceInt) J_keywrd,  &dpval );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, dpval );

   return( (jdouble)dpval );
}





/* 
Wrapper for CSPICE function dski02_c 
*/
JNIEXPORT jintArray JNICALL Java_spice_basic_CSPICE_dski02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jint              J_item,
   jint              J_start,
   jint              J_room   )

{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   jintArray               retArray = 0;

   static ConstSpiceChar * caller = "CSPICE.dski02";

   SpiceDLADescr           dladsc;

   SpiceInt              * intPtr;

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;
   SpiceInt                item;
   SpiceInt                n;
   SpiceInt                needed;
   SpiceInt                room;
   SpiceInt                start;

   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC_VAL( env, retArray );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Find out how many values we'll need to fetch.
   */
   handle = (SpiceInt) J_handle;
   start  = (SpiceInt) J_start;
   room   = (SpiceInt) J_room;
   item   = (SpiceInt) J_item;

   /*
   Allocate dynamic memory in which to store the data. We'll
   always allocate at least one int, even if `room' is 
   non-positive. We'll let the CSPICE routine signal an error
   if necessary. 
   */

   needed = maxi_c( 2, room, 1 );

   intPtr = alloc_SpiceInt_C_array( 1, needed );

   /*
   Fetch the data.
   */
   dski02_c ( handle, &dladsc, item, start, room, &n, intPtr );

   /*
   Create the output array. If there are no data to fetch, create
   an empty array. DO NOT return a null pointer!
   */
   if ( failed_c() ) 
   {
      n = 0;
   }

   createVecGI_jni ( env, n, intPtr, &retArray );

   /*
   Free the dynamically allocated array. 
   */
   free_SpiceMemory( (void *)  intPtr );

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Return the data array. 
   */
   return( retArray );
}



/* 
Wrapper for CSPICE function dskmi2_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dskmi2
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_nv,
   jdoubleArray       J_vrtces,
   jint               J_np,
   jintArray          J_plates,
   jdouble            J_finscl,
   jint               J_corscl,
   jint               J_worksz,
   jint               J_voxpsz,
   jint               J_voxlsz,
   jboolean           J_makvtl,
   jint               J_spxisz,
   jdoubleArray       J_spaixd,
   jintArray          J_spaixi  )
{
   /*
   Local variables 
   */
   SpiceBoolean            makvtl;

   static SpiceChar      * caller   = "CSPICE.dskmi2";

   SpiceDouble             corscl;
   SpiceDouble             finscl;
   SpiceDouble             spaixd [ SPICE_DSK02_SPADSZ ];
   SpiceDouble          (* vrtces) [3];

   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt                pltArrSize;
   SpiceInt                spxisz;
   SpiceInt                voxlsz;
   SpiceInt                voxpsz;
   SpiceInt                vrtArrSize;
   SpiceInt                worksz;
   SpiceInt             (* plates) [3];
   SpiceInt              * spaixi;
   SpiceInt             (* work )  [2];

   /*
   Get local copies of scalar inputs. 
   */
   makvtl = (SpiceBoolean) J_makvtl;

   finscl = (SpiceDouble)  J_finscl;

   np     = (SpiceInt) J_np;
   nv     = (SpiceInt) J_nv;
   corscl = (SpiceInt) J_corscl;
   worksz = (SpiceInt) J_worksz;
   voxpsz = (SpiceInt) J_voxpsz;
   voxlsz = (SpiceInt) J_voxlsz;
   spxisz = (SpiceInt) J_spxisz;
 

   /*
   Grab the input Java plate array in a dynamically allocated
   C-style array. 
   */
   getVecGI_jni ( env, J_plates, &pltArrSize, (SpiceInt **)&plates );

   /*
   Grab the input Java vertex array in a dynamically allocated
   C-style array. 
   */
   getVecGD_jni ( env, J_vrtces, &vrtArrSize, (SpiceDouble **)&vrtces );

   /*
   Allocate the workspace array. 
   */
   work = ( SpiceInt(*)[2] )alloc_SpiceInt_C_array( worksz, 2 );

   /*
   Allocate the integer index array.
   */
   spaixi = (SpiceInt *)alloc_SpiceInt_C_array( spxisz, 1 );

   /*
   Exit here if an exception or a SPICE error occurred. 
   */
   if ( failed_c()  ||  (*env)->ExceptionOccurred(env)  )
   {
      /*
      De-allocate all dynamic arrays. 
      */
      if ( vrtces != 0 )
      {
         freeVecGD_jni ( env, J_vrtces, (SpiceDouble *)vrtces );             
      }

      if ( plates != 0 )
      {
         freeVecGI_jni ( env, J_plates, (SpiceInt *)plates );             
      }

      free_SpiceMemory( (void *)  work   );
      free_SpiceMemory( (void *)  spaixi );

      JNI_EXC   ( env );
      SPICE_EXC ( env, caller );
   }


   /*
   Create the spatial index.
   */ 
   dskmi2_c ( nv,     vrtces, np,     plates, finscl,
              corscl, worksz, voxpsz, voxlsz, makvtl,
              spxisz, work,   spaixd, spaixi         );

   
   /*
   Regardless of the outcome of the call,  
   free the dynamically allocated arrays.
   */
   freeVecGD_jni ( env, J_vrtces, (SpiceDouble *)vrtces );             
   freeVecGI_jni ( env, J_plates, (SpiceInt    *)plates );             
   free_SpiceMemory( (void *)  work );

   /*
   Exit here if an exception or a SPICE error occurred. 
   */
   if ( failed_c()  ||  (*env)->ExceptionOccurred(env)  )
   {
      free_SpiceMemory( (void *)  spaixi );

      JNI_EXC   ( env );
      SPICE_EXC ( env, caller );
   }


   /*
   Set the fixed-size output argument. 
   */
   updateVecGD_jni ( env, SPICE_DSK02_SPADSZ, spaixd, J_spaixd );

   /*
   Set the integer spatial index output argument. 
   */
   updateVecGI_jni ( env, spxisz, spaixi, J_spaixi );    

   /*
   Free the dynamically allocated integer index array. 
   */
   free_SpiceMemory( (void *)  spaixi );
}



/* 
Wrapper for CSPICE function dskn02_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_dskn02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jint              J_plid   )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static ConstSpiceChar * caller = "CSPICE.dskn02";

   SpiceDLADescr           dladsc;

   SpiceDouble             normal [3];

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];



   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC_VAL( env, retArray );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Look up the normal vector.
   */
   dskn02_c ( (SpiceInt)J_handle, &dladsc, (SpiceInt)J_plid, normal );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create a Java return value.
   */
   createVec3D_jni( env, normal, &retArray );

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   return( retArray );
}





/* 
Wrapper for CSPICE function dskobj_c 

NOTE: the returned array has no control area.
*/
JNIEXPORT jintArray JNICALL Java_spice_basic_CSPICE_dskobj
  (JNIEnv * env, 
   jclass             J_class,
   jstring            J_file,
   jint               J_size,
   jintArray          J_cover  )
{ 
   /*
   Local variables 
   */
   jintArray            retArray = 0;


   /*
   The cells below will be dynamically allocated.
   */
   SpiceCell             * cover;

   static SpiceChar      * caller   = "CSPICE.dskobj";
   static SpiceChar        file  [ FNAMLN ];

   SpiceInt                coverSize;
   SpiceInt                maxSize;
   SpiceInt              * coverData;


   /*
   Get the size of the input set data array. 
   */
   coverSize = (*env)->GetArrayLength ( env, J_cover );
   
   JNI_EXC_VAL  ( env, retArray );


   /*
   Capture the input DSK name. 
   */
   getFixedInputString_jni ( env, J_file, FNAMLN, file );
   JNI_EXC_VAL  ( env, retArray );

 
   if ( coverSize > 0 )
   {
      /*
      Capture the contents of the input array `cover' in dynamic
      memory.  Check out and return if an exception is thrown.
      */
      getVecGI_jni ( env, J_cover, &coverSize, &coverData );
      JNI_EXC_VAL  ( env, retArray );
   }
   else
   {
      coverData = 0;
   }

   
   /*
   If the specified output cell size is smaller than the input
   array size, we have a problem. 
   */
   maxSize = (SpiceInt)J_size;

   if ( maxSize < coverSize )
   {
      /*
      We must free the data from the input array before
      returning. 
      */
      if ( coverSize > 0 )
      {
         freeVecGI_jni ( env, J_cover, coverData );
      }

      setmsg_c ( "Input cell size is #; output size is #;" );
      errint_c ( "#",  coverSize                           );
      errint_c ( "#",  maxSize                             );
      sigerr_c ( "SPICE(OUTPUTCELLTOOSMALL)"               );

      SPICE_EXC_VAL(env, caller, retArray );
   }

   /*
   Create a dynamically allocated cell of size maxSize to hold
   the results of our dskobj_c call. Initialize the cell with
   the data from the input array, if any.
   */
   cover = zzalcell_c ( SPICE_INT, maxSize, coverSize, 0, coverData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   At this point, we're holding on to a dynamically allocated
   cell. We can't return before freeing this cell, so we must
   be careful about how we handle errors.

   However, we're now done with the coverData array.
   */
   if ( coverSize > 0 )
   {
      freeVecGI_jni ( env, J_cover, coverData );   
   }
   

   /*
   Make the input cell into a set before passing it to dskobj_c. 
   */
   valid_c ( maxSize, coverSize, cover );

   if ( failed_c() )
   {
      /*
      Free the ID set before departure. 
      */
      zzdacell_c ( cover );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   We're finally ready for our CSPICE call. 
   */
   dskobj_c ( file, cover );


   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   At this point, the data portion of `cover' is exactly
   what we want to return. 
   */
   createVecGI_jni ( env, 
                     card_c(cover), 
                     (SpiceInt *)cover->data, 
                     &retArray                   );

   /*
   De-allocate the dynamic cell before departure.
   */
   zzdacell_c ( cover );

   
   return ( retArray );
}





/* 
Wrapper for CSPICE function dskopn_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_dskopn
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_fname,
   jstring            J_ifname,   
   jint               J_ncomch )
{
   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dskopn";
   static SpiceChar        fname  [ FNAMLN ];
   static SpiceChar        ifname [ IFNLEN ];

   SpiceInt                handle = 0;


   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_fname,  FNAMLN, fname  );
   getFixedInputString_jni ( env, J_ifname, IFNLEN, ifname );
   
   JNI_EXC_VAL( env, ((jint) handle) );


   dskopn_c ( fname, ifname, (SpiceInt)J_ncomch, &handle );


   SPICE_EXC_VAL( env, caller, ((jint)handle) );

   /*
   Normal return. 
   */
   return ( (jint)handle );
}



/* 
Wrapper for CSPICE function dskp02_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_dskp02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jint              J_start,
   jint              J_room   )

{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   jobjectArray            retArray = 0;

   static ConstSpiceChar * caller = "CSPICE.dskp02";

   SpiceDLADescr           dladsc;

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;
   SpiceInt                n;
   SpiceInt                nFetch;
   SpiceInt                needed;
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt              * platePtr;
   SpiceInt                room;
   SpiceInt                start;



   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC_VAL( env, retArray );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Find out how many plates we'll need to fetch.
   */
   handle = (SpiceInt) J_handle;
   start  = (SpiceInt) J_start;
   room   = (SpiceInt) J_room;

   dskz02_c ( handle, &dladsc, &nv, &np );

   needed = mini_c( 2, np-start+1, room );

   nFetch = maxi_c( 2, needed, 1 );

   /*
   Allocate dynamic memory in which to store the data.
   */
   platePtr = alloc_SpiceInt_C_array( nFetch, 3 );

   /*
   Fetch the plates. 
   */
   dskp02_c ( handle, &dladsc, start, room, &n, (SpiceInt(*)[3])platePtr );

   /*
   Create the output array. If there are no data to fetch, create
   an array having one row. DO NOT return a null pointer!
   */
   if ( failed_c() )
   {
      n = 1;
   }

   createMatGI_jni ( env, n, 3, platePtr, &retArray );

   /*
   Always free the dynamically allocated array. 
   */
   free_SpiceMemory( (void *)  platePtr );

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Return the vertex array. 
   */
   return( retArray );
}




/* 
Wrapper for CSPICE function dskrb2_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dskrb2
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_nv,
   jdoubleArray       J_vrtces,
   jint               J_np,
   jintArray          J_plates,
   jint               J_corsys,
   jdoubleArray       J_corpar,
   jdoubleArray       J_mncor3,
   jdoubleArray       J_mxcor3  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.dskrb2";

   SpiceDouble             corpar [ SPICE_DSK_NSYPAR ];
   SpiceDouble             mncor3;
   SpiceDouble             mxcor3;
   SpiceDouble           * vrtces;

   SpiceInt                corsys;
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt              * plates;
   SpiceInt                pltArrSize;
   SpiceInt                vrtArrSize;


   /*
   Get local copies of scalar inputs. 
   */
   corsys = (SpiceInt)J_corsys;
   np     = (SpiceInt)J_np;
   nv     = (SpiceInt)J_nv;

   /*
   Grab the coordinate system parameters. 
   */
   getVecFixedD_jni ( env, J_corpar, SPICE_DSK_NSYPAR, corpar );

   /*
   Grab the input Java plate array in a dynamically allocated
   C-style array. 
   */
   getVecGI_jni ( env, J_plates, &pltArrSize, &plates );

   /*
   Grab the input Java vertex array in a dynamically allocated
   C-style array. 
   */
   getVecGD_jni ( env, J_vrtces, &vrtArrSize, &vrtces );

   /*
   Exit here if an exception or a SPICE error occurred. 
   */
   JNI_EXC   ( env );
   SPICE_EXC ( env, caller );

   /*
   Get the coordinate range bounds.
   */ 
   dskrb2_c ( nv,     vrtces, np,      plates, 
              corsys, corpar, &mncor3, &mxcor3 );

   /*
   Regardless of the outcome of the insertion,  
   free the dynamically allocated arrays.
   */
   freeVecGD_jni ( env, J_vrtces, vrtces );             
   freeVecGI_jni ( env, J_plates, plates );             

   /*
   Set the output arguments. 
   */
   updateVecGD_jni ( env, 1, &mncor3, J_mncor3 );
   updateVecGD_jni ( env, 1, &mxcor3, J_mxcor3 );
}





/* 
Wrapper for CSPICE function dsksrf_c 

NOTE: the returned array has no control area.
*/
JNIEXPORT jintArray JNICALL Java_spice_basic_CSPICE_dsksrf
  (JNIEnv * env, 
   jclass             J_class,
   jstring            J_file,
   jint               J_bodyid,
   jint               J_size,
   jintArray          J_cover  )
{ 
   /*
   Local variables 
   */
   jintArray            retArray = 0;


   /*
   The cells below will be dynamically allocated.
   */
   SpiceCell             * cover;

   static SpiceChar      * caller   = "CSPICE.dsksrf";
   static SpiceChar        file  [ FNAMLN ];

   SpiceInt                coverSize;
   SpiceInt                maxSize;
   SpiceInt              * coverData;


   /*
   Get the size of the input set data array. 
   */
   coverSize = (*env)->GetArrayLength ( env, J_cover );
   
   JNI_EXC_VAL  ( env, retArray );


   /*
   Capture the input DSK name. 
   */
   getFixedInputString_jni ( env, J_file, FNAMLN, file );
   JNI_EXC_VAL  ( env, retArray );

 
   if ( coverSize > 0 )
   {
      /*
      Capture the contents of the input array `cover' in dynamic
      memory.  Check out and return if an exception is thrown.
      */
      getVecGI_jni ( env, J_cover, &coverSize, &coverData );
      JNI_EXC_VAL  ( env, retArray );
   }
   else
   {
      coverData = 0;
   }

   
   /*
   If the specified output cell size is smaller than the input
   array size, we have a problem. 
   */
   maxSize = (SpiceInt)J_size;

   if ( maxSize < coverSize )
   {
      /*
      We must free the data from the input array before
      returning. 
      */
      if ( coverSize > 0 )
      {
         freeVecGI_jni ( env, J_cover, coverData );
      }

      setmsg_c ( "Input cell size is #; output size is #;" );
      errint_c ( "#",  coverSize                           );
      errint_c ( "#",  maxSize                             );
      sigerr_c ( "SPICE(OUTPUTCELLTOOSMALL)"               );

      SPICE_EXC_VAL(env, caller, retArray );
   }

   /*
   Create a dynamically allocated cell of size maxSize to hold
   the results of our dskobj_c call. Initialize the cell with
   the data from the input array, if any.
   */
   cover = zzalcell_c ( SPICE_INT, maxSize, coverSize, 0, coverData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   At this point, we're holding on to a dynamically allocated
   cell. We can't return before freeing this cell, so we must
   be careful about how we handle errors.

   However, we're now done with the coverData array.
   */
   if ( coverSize > 0 )
   {
      freeVecGI_jni ( env, J_cover, coverData );   
   }
   

   /*
   Make the input cell into a set before passing it to dskobj_c. 
   */
   valid_c ( maxSize, coverSize, cover );

   if ( failed_c() )
   {
      /*
      Free the ID set before departure. 
      */
      zzdacell_c ( cover );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   We're finally ready for our CSPICE call. 
   */
   dsksrf_c ( file, (SpiceInt)J_bodyid, cover );


   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   At this point, the data portion of `cover' is exactly
   what we want to return. 
   */
   createVecGI_jni ( env, 
                     card_c(cover), 
                     (SpiceInt *)cover->data, 
                     &retArray                   );

   /*
   De-allocate the dynamic cell before departure.
   */
   zzdacell_c ( cover );

   
   return ( retArray );
}





/* 
Wrapper for CSPICE function dskstl_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dskstl
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_keywrd,
   jdouble           J_dpval   )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dskstl";

 
   /*
   Set the value.
   */
   dskstl_c ( (SpiceInt) J_keywrd,  (SpiceDouble)J_dpval );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   return;
}





/* 
Wrapper for CSPICE function dskv02_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_dskv02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jint              J_start,
   jint              J_room   )

{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   jobjectArray            retArray = 0;

   static ConstSpiceChar * caller = "CSPICE.dskv02";

   SpiceDLADescr           dladsc;

   SpiceDouble           * vertPtr;

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;
   SpiceInt                n;
   SpiceInt                nFetch;
   SpiceInt                needed;
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt                room;
   SpiceInt                start;



   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC_VAL( env, retArray );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Find out how many vertices we'll need to fetch.
   */
   handle = (SpiceInt) J_handle;
   start  = (SpiceInt) J_start;
   room   = (SpiceInt) J_room;

   dskz02_c ( handle, &dladsc, &nv, &np );

   needed = mini_c( 2, nv-start+1, room );

   nFetch = maxi_c( 2, needed, 1 );

   /*
   Allocate dynamic memory in which to store the data.
   */
   vertPtr = alloc_SpiceDouble_C_array( nFetch, 3 );

   /*
   Fetch the vertices. 
   */
   dskv02_c ( handle, &dladsc, start, room, &n, (SpiceDouble(*)[3])vertPtr );

   /*
   Create the output array. If there are no data to fetch, create
   an array having one row. DO NOT return a null pointer!
   */
   if ( failed_c() )
   {
      n = 1;
   }

   createMatGD_jni ( env, n, 3, vertPtr, &retArray );

   /*
   Always free the dynamically allocated array. 
   */

   free_SpiceMemory( (void *)  vertPtr ); 

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Return the vertex array. 
   */
   return( retArray );
}



/* 
Wrapper for CSPICE function dskw02_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dskw02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jint              J_center,
   jint              J_surfce,
   jint              J_dclass,
   jstring           J_frame,
   jint              J_corsys,
   jdoubleArray      J_corpar,
   jdouble           J_mncor1,
   jdouble           J_mxcor1,
   jdouble           J_mncor2,
   jdouble           J_mxcor2,
   jdouble           J_mncor3,
   jdouble           J_mxcor3,
   jdouble           J_first,
   jdouble           J_last,
   jint              J_nv,
   jdoubleArray      J_vrtces,
   jint              J_np,
   jintArray         J_plates,
   jdoubleArray      J_spaixd,
   jintArray         J_spaixi  )

{
   /*
   Constants 
   */
   #define N_D_DYN_ARRAY     1
   #define N_I_DYN_ARRAY     2


   /*
   Local variables 
   */
   jdoubleArray          * JDPtr  [N_D_DYN_ARRAY];                     
   jintArray             * JIPtr  [N_I_DYN_ARRAY];                     


   SpiceBoolean            allocFailed;

   static ConstSpiceChar * caller     = "CSPICE.dskw02";


   static ConstSpiceChar * DPtrNames[] = { "vrtces" };

   SpiceChar               frame    [ FRNMLN ];

   static ConstSpiceChar * IPtrNames[] = { "plates",
                                           "spaixi"  };
   ConstSpiceChar        * namePtr;


   SpiceDouble             corpar [ SPICE_DSK_NSYPAR ];
   SpiceDouble             first;
   SpiceDouble             last;
   SpiceDouble             mncor1;
   SpiceDouble             mncor2;
   SpiceDouble             mncor3;
   SpiceDouble             mxcor1;
   SpiceDouble             mxcor2;
   SpiceDouble             mxcor3;
   SpiceDouble             spaixd [ SPICE_DSK02_SPADSZ ];
   SpiceDouble         ( * vrtces)[3];

   SpiceInt                center;
   SpiceInt                corsys;
   SpiceInt                dclass;
   SpiceInt                failIndex;
   SpiceInt                handle;
   SpiceInt                i;
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt            ( * plates)[3];
   SpiceInt                platesLen;
   SpiceInt              * spaixi;
   SpiceInt                spaixiLen;
   SpiceInt                surfce;
   SpiceInt                vrtcesLen;

   void                  * DPtrList[N_D_DYN_ARRAY];
   void                  * IPtrList[N_I_DYN_ARRAY];


   /*
   Fetch the frame name.
   */
   getFixedInputString_jni ( env, J_frame, FRNMLN, frame );
   JNI_EXC( env );


   /*
   Capture the input arrays in C arrays. The arrays we're collecting are:

      int[][]     plates
      int[]       spaixi

      double[]    corpar
      double[]    spaixd
      double[][]  vrtces

   The arrays `corpar' and `spaixd' have fixed size. For `vrtces',
   we'll dynamically allocate memory.

   The contents of the variable-size arrays must be stored in
   dynamically allocated memory. We'll have to free each such
   dynamically allocated array before returning. 
   */

   /*
   Fetch the contents of the fixed-size d.p. input arrays.
   */
   getVecFixedD_jni ( env, J_corpar, SPICE_DSK_NSYPAR,   corpar );
   getVecFixedD_jni ( env, J_spaixd, SPICE_DSK02_SPADSZ, spaixd );

   /*
   Fetch addresses of input arrays of type jintArray. 
   */
   JIPtr[0] = &J_plates;
   JIPtr[1] = &J_spaixi;

   /*
   Fetch addresses of input arrays of type jdoubleArray. 
   */
   JDPtr[0] = &J_vrtces;

   /*
   Initialize all pointers to dynamic memory.
   We set them to zero here; we won't try to free any pointer
   whose value is zero.
   */
   plates    = 0;
   spaixi    = 0;

   getVecGI_jni ( env, J_plates, &platesLen, (SpiceInt **) &plates );
   getVecGI_jni ( env, J_spaixi, &spaixiLen, (SpiceInt **) &spaixi );

   IPtrList[0] = plates;
   IPtrList[1] = spaixi;


   vrtces    = 0;

   getVecGD_jni ( env, J_vrtces, &vrtcesLen, (SpiceDouble **) &vrtces );

   DPtrList[0] = vrtces;



   /*
   Handle any SPICE error or Java exception.
   */ 
   if ( failed_c()  ||  (*env)->ExceptionOccurred(env)  )
   {
      /*
      De-allocate all dynamic arrays. 
      */
      for ( i = 0;  i < N_I_DYN_ARRAY;  i++ )
      {
         if ( IPtrList[i] != 0 )
         {
            freeVecGI_jni( env, *(JIPtr[i]), IPtrList[i] );
         }
      }

      for ( i = 0;  i < N_D_DYN_ARRAY;  i++ )
      {
         if ( DPtrList[i] != 0 )
         {
            freeVecGD_jni( env, *(JDPtr[i]), DPtrList[i] );
         }
      }


      if (  !(*env)->ExceptionOccurred(env)  )
      {
         /*
         Throw an exception that indicates the type of SPICE error
         that occurred. 
         */
         zzThrowSpiceErrorException_jni( env, caller );
      }
    
      return;
   }


   /*
   If we have an allocation failure indicated by a zero pointer,
   handle that case. (We're not assuming that any allocation
   error resulted in an exception or SPICE error.)
   */
   allocFailed = SPICEFALSE;

   for ( i = 0;  i < N_I_DYN_ARRAY;  i++ )
   {
      if ( IPtrList[i] == 0 )
      {
         namePtr     = IPtrNames[i];
         allocFailed = SPICETRUE;
         break;
      }
   }

   /*
   Check for d.p. allocation failures, but only if we haven't
   already detected a failure. Our error message will name 
   the first variable for which a failure, if any, occurred.
   */
   if ( !allocFailed )
   {
      for ( i = 0;  i < N_D_DYN_ARRAY;  i++ )
      {
         if ( DPtrList[i] == 0 )
         {
            namePtr     = DPtrNames[i];
            allocFailed = SPICETRUE;
            break;
         }
      }
   } 

   /*
   Clean up any arrays we already allocated successfully,
   if we detected a zero pointer.
   */
   if ( allocFailed )
   {
      failIndex = i;

      for ( i = 0;  i < N_I_DYN_ARRAY;  i++ )
      {
         if ( IPtrList[i] != 0 )
         {
            freeVecGI_jni( env, *(JIPtr[i]), IPtrList[i] );
         }
      }

      for ( i = 0;  i < N_D_DYN_ARRAY;  i++ )
      {
         if ( DPtrList[i] != 0 )
         {
            freeVecGD_jni( env, *(JDPtr[i]), DPtrList[i] );
         }
      }

      /*
      Now signal an error identifying the first failure case. 
      */
      setmsg_c ( "Dynamic allocation in Java utility occurred "
                 "for pointer #."                               );
      errch_c  ( "#",  namePtr                                  );
      sigerr_c ( "SPICE(JNIMALLOCFAILED)"                       );

      /*
      Throw an exception that indicates the type of SPICE error
      that occurred. 
      */
      zzThrowSpiceErrorException_jni( env, caller );

      return;
   }

   /*
   Set scalar input arguments. 
   */
   handle = (SpiceInt)    J_handle;
   center = (SpiceInt)    J_center;
   surfce = (SpiceInt)    J_surfce;
   dclass = (SpiceInt)    J_dclass;
   corsys = (SpiceInt)    J_corsys;
   mncor1 = (SpiceDouble) J_mncor1;
   mxcor1 = (SpiceDouble) J_mxcor1;
   mncor2 = (SpiceDouble) J_mncor2;
   mxcor2 = (SpiceDouble) J_mxcor2;
   mncor3 = (SpiceDouble) J_mncor3;
   mxcor3 = (SpiceDouble) J_mxcor3;
   first  = (SpiceDouble) J_first;
   last   = (SpiceDouble) J_last;
   nv     = (SpiceInt)    J_nv;
   np     = (SpiceInt)    J_np;
  

   /*
   Write the segment.
   */   
   dskw02_c ( handle,    center,    surfce,   dclass,    frame,
              corsys,    corpar,    mncor1,   mxcor1,    mncor2,    
              mxcor2,    mncor3,    mxcor3,   first,     last,      
              nv,        vrtces,    np,       plates,    spaixd,
              spaixi                                             );

   /*
   Free the dynamically allocated memory regardless of whether the call 
   succeeded. At this point all of our pointers are non-zero.
   */      
   for ( i = 0; i < N_I_DYN_ARRAY;  i++ )
   {
      freeVecGI_jni( env, *(JIPtr[i]), IPtrList[i] ); 
   }
   
   for ( i = 0; i < N_D_DYN_ARRAY;  i++ )
   {
      freeVecGD_jni( env, *(JDPtr[i]), DPtrList[i] ); 
   }   
  
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function dskx02_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dskx02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jdoubleArray      J_vertex, 
   jdoubleArray      J_raydir, 
   jintArray         J_plid,
   jdoubleArray      J_xpt, 
   jbooleanArray     J_found    )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   SpiceBoolean            found;

   static ConstSpiceChar * caller = "CSPICE.dskx02";

   SpiceDLADescr           dladsc;

   SpiceDouble             raydir [3];
   SpiceDouble             vertex [3];
   SpiceDouble             xpt    [3];

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                plid;



   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );

   /*
   Capture the input ray vertex and direction in C arrays.
   */
   getVec3D_jni ( env, J_vertex, (SpiceDouble *)vertex );
   getVec3D_jni ( env, J_raydir, (SpiceDouble *)raydir );

   JNI_EXC( env  );


   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC( env, caller );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];


   /*
   Find the intercept if it exists.
   */
   dskx02_c ( (SpiceInt) J_handle, &dladsc, vertex, raydir,
              &plid,               xpt,     &found         );
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Set the output arguments. 
   */
   updateVecGI_jni ( env,  1,  &plid,   J_plid  );
   updateVec3D_jni ( env,      xpt,     J_xpt   );
   updateVecGB_jni ( env,  1,  &found,  J_found );
}







/* 
Wrapper for CSPICE function dskxsi_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dskxsi
  (JNIEnv          * env, 
   jclass            J_class, 
   jboolean          J_pri,
   jstring           J_target,
   jint              J_nsurf,
   jintArray         J_srflst,
   jdouble           J_et,
   jstring           J_fixref,
   jdoubleArray      J_vertex,
   jdoubleArray      J_raydir,
   jint              J_maxd,
   jint              J_maxi,
   jdoubleArray      J_xpt,
   jintArray         J_handle,
   jintArray         J_dladsc,
   jdoubleArray      J_dskdsc,
   jdoubleArray      J_dc,
   jintArray         J_ic,
   jbooleanArray     J_found    )

{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   SpiceBoolean            found;
   SpiceBoolean            pri;

   static ConstSpiceChar * caller = "CSPICE.dskxsi";
   SpiceChar               fixref [ FRNMLN ];
   SpiceChar               target [ BDNMLN ];

   SpiceDLADescr           dladsc;
   SpiceDSKDescr           dskdsc;

   SpiceDouble             dc           [ SPICE_DSKXSI_DCSIZE ];
   SpiceDouble             DSKDescrArray[ SPICE_DSK_DSCSIZ    ];
   SpiceDouble             raydir [3];
   SpiceDouble             et;
   SpiceDouble             vertex [3];
   SpiceDouble             xpt    [3];

   /*
   An integer argument to be used when the 
   surface list is empty.
   */ 
   static SpiceInt         bogus = 0;
   SpiceInt                DLADescrArray[ SPICE_DLA_DSCSIZ    ];
   SpiceInt                handle;
   SpiceInt                ic           [ SPICE_DSKXSI_ICSIZE ];
   SpiceInt                maxd;
   SpiceInt                maxi;
   SpiceInt                nsurf;
   SpiceInt              * srflst;
   SpiceInt                tempi;


   /*
   Fetch scalar inputs. 
   */
   pri   = (SpiceBoolean)J_pri;
   et    = (SpiceDouble) J_et;
   nsurf = (SpiceInt)    J_nsurf; 
   maxd  = (SpiceInt)    J_maxd;
   maxi  = (SpiceInt)    J_maxi;

   if ( nsurf > 0  )
   {
      /*
      Fetch surface list.

      Grab the input data array in a dynamically allocated
      C-style array. The declared size of the surface list
      may exceed the number of valid elements; we rely on
      the input J_nsurf to give us the correct count.

      The list array obtained here must be freed by

         freeVecGI_jni

      */
      getVecGI_jni ( env, J_srflst, &tempi, &srflst );

      /*
      Exit here if an exception or a SPICE error occurred. 
      */
      JNI_EXC   ( env );
      SPICE_EXC ( env, caller );
   }
   else
   {
      /*
      Assign a valid value to `srflst' so it can
      pass checks for null pointers, if any.
      */
      srflst = &bogus;
   }


   /*
   Fetch the fixed-size input arrays. 
   */
   getVec3D_jni ( env, J_vertex, vertex );
   JNI_EXC( env );

   getVec3D_jni ( env, J_raydir, raydir );
   JNI_EXC( env );

   /*
   Fetch target and frame strings. 
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_fixref, FRNMLN, fixref );
   JNI_EXC( env );


   /*
   Compute surface intercept.
   */
   dskxsi_c ( pri,     target,  nsurf, srflst, et,    fixref, 
              vertex,  raydir,  maxd,  maxi,   xpt,   &handle,
              &dladsc, &dskdsc, dc,    ic,     &found         );

   /*
   Free the dynamically allocated surface list regardless of whether
   the CSPICE call succeeded.
   */
   if ( nsurf > 1 )
   {
      free_SpiceMemory( (void *)  srflst );
   }
            
   if ( !failed_c() ) 
   {

      if ( found )
      {
         /*
         Map the DLA and DSK descriptors to arrays for output. 
         */
         DLADescrArray[SPICE_DLA_BWDIDX] = dladsc.bwdptr;
         DLADescrArray[SPICE_DLA_FWDIDX] = dladsc.fwdptr;
         DLADescrArray[SPICE_DLA_IBSIDX] = dladsc.ibase;
         DLADescrArray[SPICE_DLA_ISZIDX] = dladsc.isize;
         DLADescrArray[SPICE_DLA_DBSIDX] = dladsc.dbase;
         DLADescrArray[SPICE_DLA_DSZIDX] = dladsc.dsize;
         DLADescrArray[SPICE_DLA_CBSIDX] = dladsc.cbase;
         DLADescrArray[SPICE_DLA_CSZIDX] = dladsc.csize;

         /*
         We'll let the f2c'd routine dskgd_ do the mapping for us,
         even though we sacrifice some efficiency by doing so. 
         */
         dskgd_ ( (integer    *) &handle,
                  (integer    *) DLADescrArray, 
                  (doublereal *) DSKDescrArray  );

         SPICE_EXC( env, caller );


         /*
         Update the output Java arrays.
         */
         updateVec3D_jni ( env, xpt,                               J_xpt    );
         JNI_EXC( env );

         updateVecGI_jni( env, 1,                   &handle,       J_handle );
         JNI_EXC( env );

         updateVecGD_jni( env, SPICE_DSK_DSCSIZ,    DSKDescrArray, J_dskdsc );
         JNI_EXC( env );

         updateVecGI_jni( env, SPICE_DLA_DSCSIZ,    DLADescrArray, J_dladsc );
         JNI_EXC( env );

         updateVecGD_jni( env, SPICE_DSKXSI_DCSIZE, dc,            J_dc     );
         JNI_EXC( env );

         updateVecGI_jni( env, SPICE_DSKXSI_ICSIZE, ic,            J_ic     );
         JNI_EXC( env );
      }

      /*
      Update the returned "found" flag, unless an error was signaled.
      */
      updateVecGB_jni( env, 1,                   &found,        J_found  );
      JNI_EXC( env );
   }

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   return;
}





/* 
Wrapper for CSPICE function dskxv_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dskxv
  (JNIEnv          * env, 
   jclass            J_class, 
   jboolean          J_pri,
   jstring           J_target,
   jint              J_nsurf,
   jintArray         J_srflst,
   jdouble           J_et,
   jstring           J_fixref,
   jint              J_nrays,
   jobjectArray      J_vtxarr,
   jobjectArray      J_dirarr,
   jobjectArray      J_xptarr,
   jobjectArray      J_fndarr   )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dskxv";

   SpiceBoolean          * fndarr;
   SpiceBoolean            pri;

   SpiceChar               fixref [ FRNMLN ];
   SpiceChar               target [ BDNMLN ];

   SpiceDouble          (* dirarr)[3];
   SpiceDouble             et;
   SpiceDouble          (* vtxarr)[3];
   SpiceDouble          (* xptarr)[3];

   /*
   An integer argument to be used when the 
   surface list is empty.
   */ 
   static SpiceInt         bogus = 0;
   SpiceInt                nBytes;
   SpiceInt                ncols;
   SpiceInt                needed;
   SpiceInt                nrays;
   SpiceInt                nrows;
   SpiceInt                nsurf;
   SpiceInt              * srflst;
   SpiceInt                tempi;



   /*
   Fetch scalar inputs. 
   */
   pri   = (SpiceBoolean) J_pri;
   et    = (SpiceDouble)  J_et;
   nsurf = (SpiceInt)     J_nsurf; 
   nrays = (SpiceInt)     J_nrays;

   if ( nrays < 1 ) 
   {
      /*
      The arrays defining the input rays must be non-empty. 
      */
      setmsg_c ( "Array count was #; this count must be at least 1." );
      errint_c ( "#", nrays                                          );
      sigerr_c ( "SPICE(INVALIDCOUNT)"                               );
      SPICE_EXC( env, caller );
   }


   if ( nsurf > 0  )
   {
      /*
      Fetch surface list.

      Grab the input data array in a dynamically allocated
      C-style array. The declared size of the surface list
      may exceed the number of valid elements; we rely on
      the input J_nsurf to give us the correct count.

      The list array obtained here must be freed by

         freeVecGI_jni

      */
      getVecGI_jni ( env, J_srflst, &tempi, &srflst );

      /*
      Exit here if an exception or a SPICE error occurred. 
      */
      JNI_EXC   ( env );
      SPICE_EXC ( env, caller );
   }
   else
   {
      /*
      Assign a valid value to `srflst' so it can
      pass checks for null pointers, if any.
      */
      srflst = &bogus;
   }

   /*
   Fetch target and frame strings. 
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_fixref, FRNMLN, fixref );
   JNI_EXC( env );

   /*
   Fetch the vertex and direction arrays into dynamic memory. 
   Check 
   */
   getMatGD_jni ( env, J_vtxarr, &nrows, &ncols, (SpiceDouble**) &vtxarr );

   if (  ( nrows < nrays ) || ( ncols != 3 )  )
   {
      /*
      Free the surface list before signaling an error. 
      */
      if ( nsurf > 0 ) 
      {
         freeVecGI_jni( env, J_srflst, srflst );
      }

      setmsg_c( "Vertex array should have row dimension at "
                "least # and column dimension 3 but "
                "has dimensions [#][#]."                    );
      errint_c( "#", nrays                                  );
      errint_c( "#", nrows                                  );
      errint_c( "#", ncols                                  );
      sigerr_c( "SPICE(BADDIMENSIONS)"                      );

      SPICE_EXC ( env, caller );      
   }

   getMatGD_jni ( env, J_dirarr, &nrows, &ncols, (SpiceDouble**) &dirarr );

   if (  ( nrows < nrays ) || ( ncols != 3 )  )
   {
      /*
      Free the surface list before signaling an error. 
      */
      if ( nsurf > 0 ) 
      {
         freeVecGI_jni( env, J_srflst, srflst );
      }

      setmsg_c( "Direction vector array should have row "
                "dimension at least # and column dimension "
                "3 but has dimensions [#][#]."              );
      errint_c( "#", nrays                                  );
      errint_c( "#", nrows                                  );
      errint_c( "#", ncols                                  );
      sigerr_c( "SPICE(BADDIMENSIONS)"                      );

      SPICE_EXC ( env, caller );      
   }


   /*
   Allocate dynamic memory to hold the output intercept and found
   arrays.
   */
   needed    = maxi_c ( 2, nrays, 1 );
   nBytes    = needed * 3 * sizeof(SpiceDouble);

   xptarr = (SpiceDouble (*)[3] )alloc_SpiceMemory ( (size_t)nBytes );

   nBytes = needed * sizeof(SpiceBoolean);

   fndarr = (SpiceBoolean *     )alloc_SpiceMemory ( (size_t)nBytes );

   SPICE_EXC ( env, caller );      

   /*
   Compute surface intercepts.
   */
   dskxv_c ( pri,   target, nsurf,  srflst, et,    fixref, 
             nrays, vtxarr, dirarr, xptarr, fndarr         );

   /*
   Release the memory used to hold the input arrays. This must
   be done regardless of whether a SPICE error occurred.
   */
   if ( nsurf > 0 )
   {
      freeVecGI_jni( env, J_srflst, srflst );
   }

   free_SpiceMemory( (void *)  vtxarr );
   free_SpiceMemory( (void *)  dirarr );
   
   if ( !failed_c() ) 
   {
      /*
      Update the Java output arrays.
      */
      updateMatGD_jni ( env, nrays, 3, xptarr, J_xptarr );

      updateVecGB_jni ( env, nrays, (SpiceBoolean *)fndarr, J_fndarr );
   }

   /*
   Release the memory used to hold the output C arrays. 
   */
   free_SpiceMemory( (void *)  xptarr );
   free_SpiceMemory( (void *)  fndarr );

   JNI_EXC( env );

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   return;
}





/* 
Wrapper for CSPICE function dskz02_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dskz02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_descr, 
   jintArray         J_nv, 
   jintArray         J_np     )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dskz02";

   SpiceDLADescr           dladsc;

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                np;
   SpiceInt                nv;


   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_descr, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC( env );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC( env, caller );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Fetch the plate model size parameters.
   */
   dskz02_c ( (SpiceInt) J_handle, &dladsc, &nv, &np );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );


   /*
   Set the output arguments. 
   */
   updateVecGI_jni ( env, 1, &nv,  J_nv );
   updateVecGI_jni ( env, 1, &np,  J_np );
}





/* 
Wrapper for CSPICE function dsphdr_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_dsphdr
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_x, 
   jdouble            J_y, 
   jdouble            J_z    )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.dsphdr";

   SpiceDouble             jacob [3][3];

   /*
   Get the Jacobian matrix.
   */
   dsphdr_c ( (SpiceDouble) J_x,
              (SpiceDouble) J_y,
              (SpiceDouble) J_z,  jacob );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( jacob ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function dtpool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dtpool
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_name,
   jbooleanArray      J_found,
   jintArray          J_n,
   jobjectArray       J_type   )
{
   /*
   Local variables 
   */
   jstring                 jStrPtr;
 
   static SpiceChar      * caller = "CSPICE.dtpool";

   SpiceBoolean            found;
   static SpiceChar        dtype    [ 2 ];
   static SpiceChar        kvname   [ KVNMLN ];

   SpiceInt                size;


   /*
   Capture the kernel variable name in a local buffer, then
   release the dynamically allocated version of the name.
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvname );
   
   JNI_EXC( env );

   /*
   Find out whether the kernel variable is present in the kernel
   pool, and if so, retrieve its name and data type.
   */
   dtpool_c ( kvname, &found, &size, dtype );

   SPICE_EXC( env, caller );      
  
   /*
   Set the output "found" argument. 
   */
   updateVecGB_jni ( env, 1, &found, J_found );
   
   if ( found )
   {
      /*
      Set the output size argument.
      */
      updateVecGI_jni ( env, 1, &size, J_n );


      /*
      Set the output data type argument.

      First, null-terminate the output character array dtype.
      */

      dtype[1] = (SpiceChar) 0;

      /*
      Create a Java string representing the data type.
      */
      jStrPtr  = createJavaString_jni ( env, dtype );

      JNI_EXC( env );

      /*
      Make the sole element of the Java string array J_type point
      to the Java string we just created.
      */
      (*env)->SetObjectArrayElement ( env, J_type, 0, jStrPtr );
   }
}


/* 
Wrapper for CSPICE function dvdot_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_dvdot
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_s1,
   jdoubleArray       J_s2  )
{
   /*
   Local variables 
   */
   SpiceDouble             result = 0;
   SpiceDouble             s1 [6];
   SpiceDouble             s2 [6];
 

   /*
   Fetch the Java vectors. 
   */
   getVecFixedD_jni ( env, J_s1, 6, s1 );
   getVecFixedD_jni ( env, J_s2, 6, s2 );

   JNI_EXC_VAL( env, ((jdouble) result) );

   result = dvdot_c( s1, s2 );

   return ( (jdouble)result );
}



/* 
Wrapper for CSPICE function dvcrss_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_dvcrss
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_s1,
   jdoubleArray       J_s2  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             s1   [6];
   SpiceDouble             s2   [6];
   SpiceDouble             sout [6];
 

   /*
   Fetch the Java vectors. 
   */
   getVecFixedD_jni ( env, J_s1, 6, s1 );
   getVecFixedD_jni ( env, J_s2, 6, s2 );

   JNI_EXC_VAL( env, retArray );

   /*
   Note: wrapper for dvcrss is needed!
   */
   dvcrss_( s1, s2, sout );

   createVecGD_jni ( env, 6, (SpiceDouble *)sout, &retArray );

   return ( retArray );
}


/* 
Wrapper for CSPICE function dvhat_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_dvhat
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_s1  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             s1   [6];
   SpiceDouble             sout [6];
 

   /*
   Fetch the Java vectors. 
   */
   getVecFixedD_jni ( env, J_s1, 6, s1 );

   JNI_EXC_VAL( env, retArray );

   /*
   Note: wrapper for dvcrss is needed!
   */
   dvhat_c( s1, sout );

   createVecGD_jni ( env, 6, (SpiceDouble *)sout, &retArray );

   return ( retArray );
}


/* 
Wrapper for CSPICE function dvsep_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_dvsep
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_s1,
   jdoubleArray       J_s2  )
{
   /*
   Local variables 
   */
   SpiceDouble             result = 0;
   SpiceDouble             s1 [6];
   SpiceDouble             s2 [6];
 

   /*
   Fetch the Java vectors. 
   */
   getVecFixedD_jni ( env, J_s1, 6, s1 );
   getVecFixedD_jni ( env, J_s2, 6, s2 );

   JNI_EXC_VAL( env, ((jdouble) result) );

   result = dvsep_c( s1, s2 );

   return ( (jdouble)result );
}



/* 
Wrapper for CSPICE function dvpool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dvpool
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_name ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.dvpool";
   static SpiceChar        kvname  [ KVNMLN ];


   /*
   Capture the kernel variable name in a local buffer, then
   release the dynamically allocated version of the name.
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvname );
   
   JNI_EXC( env );

   /*
   Delete the kernel variable from the pool.
   */
   dvpool_c ( kvname );

   SPICE_EXC( env, caller );      
}



/* 
Wrapper for CSPICE function edlimb_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_edlimb
  (JNIEnv * env, 
   jclass             J_class, 
   jdouble            J_a,
   jdouble            J_b,
   jdouble            J_c,
   jdoubleArray       J_viewpt )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.edlimb";

   SpiceDouble             ellipseArray [ELMAX];
   SpiceDouble             viewpt       [3];
 
   SpiceEllipse            ellipse;

   /*
   Get the input vector in a one-dimensional C array. 
   */
   getVec3D_jni ( env, J_viewpt, (SpiceDouble *)viewpt );

   JNI_EXC_VAL( env, retArray );

   edlimb_c ( (SpiceDouble) J_a,
              (SpiceDouble) J_b,
              (SpiceDouble) J_c,
              viewpt,
              &ellipse          );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Transfer the components of the ellipse to a 1-d array.
   */
   MOVED( ellipse.center,     3,  ellipseArray   );
   MOVED( ellipse.semiMajor,  3,  ellipseArray+3 );
   MOVED( ellipse.semiMinor,  3,  ellipseArray+6 );

   /*
   Normal return. 
   */
   createVecGD_jni ( env, ELMAX, ellipseArray, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function el2cgv_c 

NOTE: the output arrays are updated by this routine; these
arrays must be created by the caller.
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_el2cgv
  (JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_ellipse,
   jdoubleArray       J_center,
   jdoubleArray       J_smajor,
   jdoubleArray       J_sminor   )
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.el2cgv";

   SpiceDouble             array   [9] ;
   SpiceDouble             center  [3];
   SpiceDouble             smajor  [3];
   SpiceDouble             sminor  [3];

   static SpiceEllipse     ellipse;
 

   /*
   Capture the input ellipse array in a local buffer.

   Return if an exception occurs. No deallocation is required.
   */
   getVecFixedD_jni ( env, J_ellipse, 9, array );
   
   JNI_EXC ( env );

   /*
   Create a SpiceEllipse, then derive the semi-axes of
   the ellipse. 
   */
   cgv2el_c ( array,    array+3, array+6, &ellipse );

   el2cgv_c ( &ellipse, center,  smajor,  sminor   );

   SPICE_EXC( env, caller );


   /*
   Set the outputs. 
   */
   updateVec3D_jni ( env, center, J_center );
   updateVec3D_jni ( env, smajor, J_smajor );
   updateVec3D_jni ( env, sminor, J_sminor );
}



/* 
Wrapper for CSPICE function eqstr_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_eqstr
  ( JNIEnv   * env, 
    jclass     J_class,
    jstring    J_a,
    jstring    J_b   )
{
   /*
   Local variables 
   */
   SpiceBoolean            result = 0;

   SpiceChar             * aPtr;
   SpiceChar             * bPtr;
   static SpiceChar      * caller = "CSPICE.eqstr";

   SpiceInt                aLen;
   SpiceInt                bLen;


   /*
   Capture the input strings in dynamically allocated buffers. 
   */
   getVarInputString_jni ( env, J_a, &aLen, &aPtr );
   getVarInputString_jni ( env, J_b, &bLen, &bPtr );

   JNI_EXC_VAL ( env, ((jboolean)result) );

   result = eqstr_c ( aPtr, bPtr );

   
   /*
   Regardless of whether an error occurred, free the
   dynamically allocated strings.
   */
   freeVarInputString_jni ( env, J_a, aPtr );
   freeVarInputString_jni ( env, J_b, bPtr );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)result) );
   
   /*
   Normal return. 
   */
   return (jboolean) result;
}



/* 
Wrapper for CSPICE function erract_c
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_erract
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_op,
   jstring            J_action )
{

   /*
   Local constants 
   */
   #define OPLEN          11
   #define ACTLEN         81

   /*
   Local variables 
   */
   jstring                 retString = 0;

   static SpiceChar        op     [ OPLEN  ];
   static SpiceChar        action [ ACTLEN ];


   /*
   Capture the input strings in local arrays. 
   */
   getFixedInputString_jni ( env, J_op,     OPLEN,  op     );
   getFixedInputString_jni ( env, J_action, ACTLEN, action );

   JNI_EXC_VAL( env, retString );


   erract_c ( op, ACTLEN, action );

   if (  eqstr_c( op, "SET" )  )
   {
      retString = createJavaString_jni ( env, "" );
   }
   else
   {
      retString = createJavaString_jni ( env, action );
   }

   return ( retString );
}



/* 
Wrapper for CSPICE function errdev_c
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_errdev
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_op,
   jstring            J_device )
{
   /*
   Local constants 
   */
   #define OPLEN          11
   #define DEVLEN         256

   /*
   Local variables 
   */
   jstring                 retString = 0;

   static SpiceChar        op     [ OPLEN  ];
   static SpiceChar        device [ DEVLEN ];


   /*
   Capture the input strings in local arrays. 
   */
   getFixedInputString_jni ( env, J_op,     OPLEN,  op     );
   getFixedInputString_jni ( env, J_device, DEVLEN, device );

   JNI_EXC_VAL( env, retString );

   errdev_c ( op, DEVLEN, device );


   if (  eqstr_c( op, "SET" )  )
   {
      retString = createJavaString_jni ( env, "" );
   }
   else
   {
      retString = createJavaString_jni ( env, device );
   }

   return ( retString );
}



/* 
Wrapper for CSPICE function et2lst_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_et2lst
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jdouble       J_et,
    jint          J_body,
    jdouble       J_lon,
    jstring       J_type,
    jintArray     J_hr,
    jintArray     J_min,
    jintArray     J_sec,
    jobjectArray  J_time,
    jobjectArray  J_ampm )
{
   /*
   Local variables 
   */   
   jstring                 jAmpm;
   jstring                 jTime;
   
   static SpiceChar        ampm   [ TIMLEN ];
   static SpiceChar      * caller   = "CSPICE.et2lst";
   static SpiceChar        time   [ TIMLEN ];
   SpiceChar             * type;
   
   SpiceInt                hr;
   SpiceInt                min;
   SpiceInt                sec;
   SpiceInt                typeLen;


   /*
   Fetch the input string into a dynamically allocated array.
   */
   getVarInputString_jni ( env, J_type, &typeLen, &type );

   /*
   Check for a JNI exception.
   */
   JNI_EXC( env );


   et2lst_c ( (SpiceDouble)J_et,  (SpiceInt)J_body,  (SpiceDouble)J_lon,
              type,               TIMLEN,            TIMLEN,
              &hr,                &min,              &sec,      
              time,               ampm                                  );

   /*
   Always free the longitude type string. 
   */
   freeVarInputString_jni ( env, J_type, type );


   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC( env, caller );


   /*
   Update the integer output arrays.
   */ 
   updateVecGI_jni ( env, 1, &hr,  J_hr  );
   updateVecGI_jni ( env, 1, &min, J_min );
   updateVecGI_jni ( env, 1, &sec, J_sec );

   /*
   Create a Java string holding the time string; update the 
   time object array string with this Java string.
   */
   jTime = createJavaString_jni ( env, time );
   JNI_EXC( env );
   
   (*env)->SetObjectArrayElement ( env, J_time, 0, jTime );
   JNI_EXC( env );

   /*
   Create a Java string holding the ampm string; update the 
   J_ampm object array string with this Java string.
   */
   jAmpm = createJavaString_jni ( env, ampm );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_ampm, 0, jAmpm );
}



/* 
Wrapper for CSPICE function et2utc_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_et2utc
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jdouble       J_et,
    jstring       J_format,
    jint          J_prec ) 
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.et2utc";
   static SpiceChar        utcstr [ TIMLEN ];
   static SpiceChar        format [ TIMLEN ];
   

   /*
   Fetch the input string into a fixed length local array.
   */
   getFixedInputString_jni ( env, J_format, TIMLEN, format );

   /*
   Check for a JNI exception.
   */
   JNI_EXC_VAL( env, ((jstring)0) );


   et2utc_c ( (SpiceDouble)J_et, format, (SpiceInt)J_prec, TIMLEN, utcstr );


   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jstring)0) );

   /*
   Normal return. 
   */ 
   return (  createJavaString_jni( env, utcstr )  );
}



/* 
Wrapper for CSPICE function etcal_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_etcal
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jdouble       J_et     ) 
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.etcal";
   static SpiceChar        timstr [ TIMLEN ];
   

 
   etcal_c ( (SpiceDouble)J_et, TIMLEN, timstr );


   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jstring)0) );

   /*
   Normal return. 
   */ 
   return (  createJavaString_jni( env, timstr )  );
}



/* 
Wrapper for CSPICE function eul2m_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_eul2m
  (JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_angles, 
   jintArray          J_axes   )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.eul2m";

   SpiceDouble             angles [3];
   SpiceDouble             r      [3][3];
   SpiceInt                axes   [3];

   /*
   Get the input angle array in a C array. 
   */
   getVec3D_jni ( env, J_angles, angles );

   JNI_EXC_VAL ( env, retArray );

   /*
   Get the input axis sequence in an array as well. 
   */
   getVec3I_jni ( env, J_axes, axes );

   JNI_EXC_VAL ( env, retArray );

   /*
   Make the CSPICE call. 
   */
   eul2m_c ( angles[0],   angles[1],   angles[2], 
             axes  [0],   axes  [1],   axes  [2],  r );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( r ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function eul2xf_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_eul2xf
  (JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_angles, 
   jintArray          J_axes   )
{
   /*
   Local variables  
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.eul2xf";

   SpiceDouble             angles [6];
   SpiceDouble             xform  [6][6];

   SpiceInt                axes   [3];


   /*
   Get the input array of angles and rates in a dynamically
   allocated C array. 
   */
   getVecFixedD_jni ( env, J_angles, 6, angles );

   JNI_EXC_VAL ( env, retArray );

   /*
   Get the input axis sequence in an array as well. 
   */
   getVec3I_jni ( env, J_axes, axes );

   JNI_EXC_VAL ( env, retArray );

   /*
   Make the CSPICE call. 
   */
   eul2xf_c ( angles,  axes[0],  axes[1],  axes[2],  xform );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createVecGD_jni ( env,  36,  CONST_VEC(xform),  &retArray );

   return ( retArray );
}




/* 
Wrapper for CSPICE function frinfo_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_frinfo
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_frameID,
    jintArray     J_frcenterID,
    jintArray     J_frclass,
    jintArray     J_frclassID,
    jbooleanArray J_found      )
    
{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   static SpiceChar      * caller = "CSPICE.frinfo";

   SpiceInt                frcenter;
   SpiceInt                frclass;
   SpiceInt                frclassID;

   frinfo_c ( (SpiceInt)J_frameID,  &frcenter, 
              &frclass,             &frclassID,  &found );

   SPICE_EXC( env, caller );
  
   updateVecGI_jni ( env, 1, &frcenter,  J_frcenterID  );
   updateVecGI_jni ( env, 1, &frclass,   J_frclass     );
   updateVecGI_jni ( env, 1, &frclassID, J_frclassID   );
   updateVecGB_jni ( env, 1, &found,     J_found       );

   JNI_EXC( env );
}



/* 
Wrapper for CSPICE function frmnam_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_frmnam
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_code ) 
{
   /*
   Local variables 
   */
   jstring                 retStr = 0;

   static SpiceChar      * caller = "CSPICE.frmnam";

   static SpiceChar        name  [ BDNMLN ];



   frmnam_c ( (SpiceInt)J_code, BDNMLN, name );

   SPICE_EXC_VAL( env, caller, retStr );

   retStr = createJavaString_jni( env, name );

   return ( retStr );
}



/* 
Wrapper for CSPICE function furnsh_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_furnsh
  ( JNIEnv *  env, 
    jclass    J_class, 
    jstring   J_file ) 
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.furnsh";
   SpiceChar             * file;
   SpiceInt                fileLen;

   /*
   Fetch input string into dynmically allocated memory. 
   Check for a JNI exception.
   */
   getVarInputString_jni ( env, J_file, &fileLen, &file );
   JNI_EXC( env );


   /*
   Load the file. 
   */
   furnsh_c ( file );


   /*
   Free the dynamically allocated memory.
   */
   freeVarInputString_jni ( env, J_file, file );

   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC( env, caller );
}


/* 
Wrapper for CSPICE function gcpool_c
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_gcpool
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_name,
   jint               J_start,
   jint               J_room   )
{
   /*
   Local variables 
   */

   integer                 start;
   integer                 room;

   jobjectArray            retArray = 0;

   logical                 yes;

   SpiceBoolean            found;

   static SpiceChar      * caller   = "CSPICE.gcpool";
   static SpiceChar        dtype   [ 2 ];
   static SpiceChar        kvname  [ KVNMLN ];
   static SpiceChar        message [ LMSGLN  ];

   SpiceInt                n;
   SpiceInt                nBytes;
   SpiceInt                needed;
   SpiceInt                size;
   
   void                  * fArrayPtr;


   /*
   Capture the kernel variable name in a local buffer, then
   release the dynamically allocated version of the name.
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvname );
   
   JNI_EXC_VAL ( env, retArray );

   /*
   See whether the requested kernel variable is present in the kernel
   pool, and whether it has character data type.
   */
   dtpool_c ( kvname, &found, &size, dtype );

   SPICE_EXC_VAL ( env, caller, retArray );      
   

   if (  ( !found )  ||  ( dtype[0] != 'C' )  )
   {
      /*
      We're going to throw a "kernel variable not found" 
      exception. The exception message will indicate
      the specific cause.
      */

      if ( !found )
      {
         strncpy ( message, 
                   "Kernel variable # was not found in the "
                   "kernel pool.",
                   LMSGLN                                   );
      }
      else
      {
         strncpy ( message, 
                   "Character kernel variable # "
                   "was not found in the kernel pool. "
                   "A numeric variable having this name "
                   "is present in the pool.",
                   LMSGLN                                );
      }

      /*
      Substitute the kernel variable name into the message. 
      */
      repmc_c ( message, "#", kvname, LMSGLN, message );

      /*
      Throw the exception and return. 
      */
      zzThrowException_jni ( env, KVNF_EXC, message );


      return retArray;
   }


   /*
   At this point we know the variable exists. Allocate enough
   memory to hold the requested portion of the data.
   */
   needed    = mini_c ( 2,  (SpiceInt)J_room,  (size-(SpiceInt)J_start) );

   /*
   Adjust `needed' so we don't get an allocation error.
   */
   needed = maxi_c ( 2, needed, 1 );


   nBytes    = needed * F_KVSTLN;

   fArrayPtr = alloc_SpiceMemory ( (size_t)nBytes );

   SPICE_EXC_VAL ( env, caller, retArray );      


   /*
   Fetch the requested data. We need not check the found flag,
   since we've already confirmed existence of the variable
   via dtpool_c.

   We call the f2c'd routine to avoid redundant dynamic
   memory allocation.

   Adjust the start value to Fortran-style indexing.
   */
   start = ( (integer) J_start ) + 1;
   room  = (integer) J_room;
  
   gcpool_( ( char    * ) kvname,
            ( integer * ) &start,
            ( integer * ) &room,
            ( integer * ) &n,
            ( char    * ) fArrayPtr,
            ( logical * ) &yes,
            ( ftnlen    ) strlen(kvname),
            ( ftnlen    ) F_KVSTLN        );


   if ( failed_c() )
   {
      /*
      Free the Fortran-style string array. 
      */
      free_SpiceMemory ( (void *)  fArrayPtr );
      
      SPICE_EXC_VAL ( env, caller, retArray );      
   }

   /*
   Create an array of Java strings from the Fortran-style array.
   */   
   retArray = createJavaStringArray_jni ( env, 
                                          needed, 
                                          F_KVSTLN, 
                                          fArrayPtr );
   /*
   Free the Fortran-style string array. 
   */
   free_SpiceMemory ( (void *)  fArrayPtr );

                
   return ( retArray );
}



/* 
Wrapper for CSPICE function gdpool_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gdpool
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_name,
   jint               J_start,
   jint               J_room   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceBoolean            found;     
 
   static SpiceChar      * caller   = "CSPICE.gdpool";
   static SpiceChar        dtype[1];
   static SpiceChar        kvname   [ KVNMLN ];
   static SpiceChar        message  [ LMSGLN ];

   SpiceDouble           * dpdata = 0;

   SpiceInt                n;
   SpiceInt                needed;
   SpiceInt                size;

 
   /*
   Capture the kernel variable name in a local buffer, then
   release the dynamically allocated version of the name.
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvname );
   
   JNI_EXC_VAL ( env, retArray );

   /*
   See whether the requested kernel variable is present in the kernel
   pool, and whether it has numeric data type.
   */
   dtpool_c ( kvname, &found, &size, dtype );

   SPICE_EXC_VAL ( env, caller, retArray );      
   

   if (  ( !found )  ||  ( dtype[0] != 'N' )  )
   {
      /*
      We're going to throw a "kernel variable not found" 
      exception. The exception message will indicate
      the specific cause.
      */

      if ( !found )
      {
         strncpy ( message, 
                   "Kernel variable # was not found in the "
                   "kernel pool.",
                   LMSGLN                                   );
      }
      else
      {
         strncpy ( message, 
                   "Numeric kernel variable # "
                   "was not found in the kernel pool. "
                   "A character variable having this name "
                   "is present in the pool.",
                   LMSGLN                                );
      }

      /*
      Substitute the kernel variable name into the message. 
      */
      repmc_c ( message, "#", kvname, LMSGLN, message );

      /*
      Throw the exception and return. 
      */
      zzThrowException_jni ( env, KVNF_EXC, message );


      return retArray;
   }

   /*
   At this point we know the variable exists. Allocate enough
   memory to hold the requested portion of the data.
   */
   needed = mini_c ( 2,  (SpiceInt)J_room,  (size-(SpiceInt)J_start) );

   /*
   Adjust `needed' so we don't get an allocation error.
   */
   needed = maxi_c ( 2, needed, 1 );

   dpdata = alloc_SpiceDouble_C_array ( 1, needed );

   SPICE_EXC_VAL ( env, caller, retArray );
 
   /*
   Let gdpool_c diagnose bad values of J_start or J_room.
   */
   gdpool_c ( kvname, (SpiceInt)J_start, (SpiceInt)J_room, &n, dpdata, &found );


   if ( failed_c() )
   {
      /*
      Free the dynamically allocated memory before leaving.
      */
      free_SpiceMemory( (void *)  dpdata );

      SPICE_EXC_VAL ( env, caller, retArray );
   }

   /*
   Create the output array. 
   */
   createVecGD_jni ( env, needed, dpdata, &retArray );

   /*
   Free the dynamically allocated memory before leaving.
   */
   free_SpiceMemory( (void *)  dpdata );

   return retArray;
}



/* 
Wrapper for CSPICE function georec_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_georec
  (JNIEnv * env,
   jclass             J_class,
   jdouble            J_longitude,
   jdouble            J_latitude,
   jdouble            J_altitude,
   jdouble            J_re,
   jdouble            J_f           )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller   = "CSPICE.georec";
   SpiceDouble             result[3];



   georec_c ( (SpiceDouble) J_longitude, 
              (SpiceDouble) J_latitude, 
              (SpiceDouble) J_altitude, 
              (SpiceDouble) J_re, 
              (SpiceDouble) J_f, 
              result                   );


   SPICE_EXC_VAL ( env, caller, retArray );

   /*
   Create a new Java array of jdoubles to hold the result. 
   */
   createVec3D_jni ( env, result, &retArray );


   return retArray;
}





/* 
Wrapper for CSPICE function getfat_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_getfat
  ( JNIEnv *        env, 
    jclass          J_class,
    jstring         J_file,
    jobjectArray    J_arch,
    jobjectArray    J_type  )
{
   /*
   Local variables 
   */   
   jstring                 jArch;
   jstring                 jType;

   static SpiceChar      * caller = "CSPICE.getfat";
   static SpiceChar        file   [ FNAMLN ];
   static SpiceChar        arch   [ KARCLN ];
   static SpiceChar        type   [ KTYPLN ];
 
   
   getFixedInputString_jni ( env, J_file, FNAMLN, file );

   JNI_EXC( env );

   getfat_c ( file, KARCLN, KTYPLN, arch, type );

   SPICE_EXC( env, caller );

   /*
   Fill in the output arrays. 
   */
   jArch = createJavaString_jni ( env, arch );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_arch, 0, jArch );
   JNI_EXC( env );


   jType = createJavaString_jni ( env, type );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_type, 0, jType );
   JNI_EXC( env );
}





/* 
Wrapper for CSPICE function getfov_c
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_getfov
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_instid,
   jobjectArray       J_shape,
   jobjectArray       J_frame,
   jdoubleArray       J_bsight,
   jintArray          J_size,
   jobjectArray       J_bounds )

{
   /*
   Local constants 
   */
   #define MAXVRT          SPICE_GF_MAXVRT
   #define SHPLEN          SPICE_GF_SHPLEN

   /*
   Local variables 
   */
   jstring                 jFrame;
   jstring                 jShape;
   
   static SpiceChar      * caller = "CSPICE.getfov";
   static SpiceChar        frame  [ FRNMLN ];
   static SpiceChar        shape  [ SHPLEN ];

   static SpiceDouble      bounds [ SPICE_GF_MAXVRT ][3];
   SpiceDouble             bsight [ 3 ];

   SpiceInt                n;

   /*
   Look up the FOV. Throw an exception if the call fails.
   */
   getfov_c ( (SpiceInt)J_instid, MAXVRT,  SHPLEN, FRNMLN, 
              shape,              frame,   bsight, &n,     bounds );

   SPICE_EXC( env, caller );

   /*
   Set the output arrays. 

   Create a Java string representing the FOV shape; update the
   J_shape array with a reference to this string.
   */
   jShape  = createJavaString_jni ( env, shape );

   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_shape, 0, jShape );

   JNI_EXC( env );

   /*
   Create a Java string representing the FOV frame; update the
   J_frame array with a reference to this string.
   */
   jFrame  = createJavaString_jni ( env, frame );

   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_frame, 0, jFrame );

   JNI_EXC( env );

   /*
   Update the boresight array. 
   */
   updateVec3D_jni ( env, bsight, J_bsight );

   JNI_EXC( env );

   /*
   Update the FOV vector count array.
   */
   updateVecGI_jni ( env, 1, (SpiceInt *)&n, J_size );

   /*
   Update the FOV bounds array.
   */
   updateVecGD_jni ( env, 3*n, (SpiceDouble *)bounds, J_bounds );

   JNI_EXC( env );
}



/* 
Wrapper for CSPICE function gfbail_c
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_gfbail
 ( JNIEnv *      env, 
   jclass        J_class )
{
   /*
   Local variables 
   */
   jboolean                retVal = 0;

   static SpiceChar      * caller = "CSPICE.gfbail"; 


   retVal = (jboolean)gfbail_c();

   SPICE_EXC_VAL( env, caller, retVal );

   return ( retVal );
}
   


/* 
Wrapper for CSPICE function gfclrh_c
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_gfclrh
 ( JNIEnv *      env, 
   jclass        J_class )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.gfclrh"; 


   gfclrh_c();

   SPICE_EXC( env, caller );
}
   
   


/* 
Wrapper for CSPICE function gfdist_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfdist
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_target, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jstring       J_relate, 
   jdouble       J_refval, 
   jdouble       J_adjust, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller = "CSPICE.gfdist";
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        relate  [ RLOPLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             adjust;
   SpiceDouble           * cnfineData;
   SpiceDouble             refval;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_relate, RLOPLN, relate );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   adjust  = (SpiceDouble) J_adjust;
   refval  = (SpiceDouble) J_refval;
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );
  
   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray );      


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   if ( failed_c() )
   {
      /*
      De-allocate the confinement window before handling the error. 
      */
      zzdacell_c ( cnfine );

      SPICE_EXC_VAL( env, caller, retArray );      
   }


   /*
   Perform the search. 
   */
   gfdist_c ( target, abcorr, obsrvr,  relate, refval,
              adjust, step,   nintvls, cnfine, result );


   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}




/* 
Wrapper for CSPICE function gfilum_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfilum
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_method, 
   jstring       J_angtyp, 
   jstring       J_target, 
   jstring       J_illmn, 
   jstring       J_fixref, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jdoubleArray  J_spoint, 
   jstring       J_relate, 
   jdouble       J_refval, 
   jdouble       J_adjust, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar        angtyp  [ ANTPLN ];
   static SpiceChar      * caller = "CSPICE.gfilum";
   static SpiceChar        fixref  [ FRNMLN ];
   static SpiceChar        illmn   [ BDNMLN ];
   static SpiceChar        method  [ METHLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        relate  [ RLOPLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             adjust;
   SpiceDouble           * cnfineData;
   SpiceDouble             refval;
   SpiceDouble             spoint  [3] ;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_method, METHLN, method );
   getFixedInputString_jni ( env, J_angtyp, ANTPLN, angtyp );
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_illmn,  BDNMLN, illmn  );
   getFixedInputString_jni ( env, J_fixref, FRNMLN, fixref );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_relate, RLOPLN, relate );

   /*
   Fetch the surface point 3-vector.
   */
   getVec3D_jni ( env, J_spoint, spoint );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   adjust  = (SpiceDouble) J_adjust;
   refval  = (SpiceDouble) J_refval;
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );

   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray ); 

   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );

   /*
   Perform the search. 
   */
   gfilum_c ( method, angtyp, target,  illmn,  fixref, 
              abcorr, obsrvr, spoint,  relate, refval,
              adjust, step,   nintvls, cnfine, result );

   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

 
   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}




/* 
Wrapper for CSPICE function gfocce_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfocce
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_occtyp, 
   jstring       J_front, 
   jstring       J_fshape, 
   jstring       J_fframe, 
   jstring       J_back, 
   jstring       J_bshape, 
   jstring       J_bframe, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jint          J_nintvls,
   jdoubleArray  J_cnfine,
   jobject       J_utils     )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceBoolean            bail;
   SpiceBoolean            rpt;

   
   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar        back    [ BDNMLN ];
   static SpiceChar        bframe  [ FRNMLN ];
   static SpiceChar        bshape  [ BSHPLN ];
   static SpiceChar      * caller = "CSPICE.gfocce";
   static SpiceChar        fframe  [ FRNMLN ];
   static SpiceChar        front   [ BDNMLN ];
   static SpiceChar        fshape  [ BSHPLN ];
   static SpiceChar        occtyp  [ OCCLN  ];
   static SpiceChar        obsrvr  [ BDNMLN ];

   SpiceDouble           * cnfineData;
   SpiceDouble             tol;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;


   /*
   Store the class, object, and method information
   required to call Java search methods from within
   gfocce_c.
   */
   zzGFSearchUtilsInit ( env, J_utils );

   JNI_EXC_VAL  ( env,         retArray );
   SPICE_EXC_VAL( env, caller, retArray );      


   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_occtyp, OCCLN,  occtyp );
   getFixedInputString_jni ( env, J_front,  BDNMLN, front  );
   getFixedInputString_jni ( env, J_fshape, BSHPLN, fshape );
   getFixedInputString_jni ( env, J_fframe, FRNMLN, fframe );
   getFixedInputString_jni ( env, J_back,   BDNMLN, back   );
   getFixedInputString_jni ( env, J_bshape, BSHPLN, bshape );
   getFixedInputString_jni ( env, J_bframe, FRNMLN, bframe );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );

   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray );      
   

   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   if ( failed_c() )
   {
      /*
      De-allocate the confinement window before handling the error. 
      */
      zzdacell_c ( cnfine );

      SPICE_EXC_VAL( env, caller, retArray );      
   }


   /*
   Perform the search. 

   For now, use defaults for the interrupt handling functions.

   The step size must be set by the calling Java application
   prior to the GFOccultationSearch.run call.
   */

   tol  = zzgetTolerance_jni();
   rpt  = zzisReportingEnabled_jni();
   bail = zzisInterruptHandlingEnabled_jni();



   gfocce_c ( occtyp, 
              front,  
              fshape, 
              fframe, 
              back,   
              bshape, 
              bframe, 
              abcorr, 
              obsrvr, 
              tol,
              zzgfstep_jni,   
              zzgfrefn_jni,
              rpt,
              zzinitializeReport_jni,
              zzupdateReport_jni,
              zzfinalizeReport_jni,
              bail,
              zzinterruptOccurred_jni,
              cnfine, 
              result       );



   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

 
   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}




/* 
Wrapper for CSPICE function gfoclt_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfoclt
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_occtyp, 
   jstring       J_front, 
   jstring       J_fshape, 
   jstring       J_fframe, 
   jstring       J_back, 
   jstring       J_bshape, 
   jstring       J_bframe, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jdouble       J_step, 
   jint          J_nintvls,
   jdoubleArray  J_cnfine   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar        back    [ BDNMLN ];
   static SpiceChar        bframe  [ FRNMLN ];
   static SpiceChar        bshape  [ BSHPLN ];
   static SpiceChar      * caller = "CSPICE.gfoclt";
   static SpiceChar        fframe  [ FRNMLN ];
   static SpiceChar        front   [ BDNMLN ];
   static SpiceChar        fshape  [ BSHPLN ];
   static SpiceChar        occtyp  [ OCCLN  ];
   static SpiceChar        obsrvr  [ BDNMLN ];

   SpiceDouble           * cnfineData;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;


   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_occtyp, OCCLN,  occtyp );
   getFixedInputString_jni ( env, J_front,  BDNMLN, front  );
   getFixedInputString_jni ( env, J_fshape, BSHPLN, fshape );
   getFixedInputString_jni ( env, J_fframe, FRNMLN, fframe );
   getFixedInputString_jni ( env, J_back,   BDNMLN, back   );
   getFixedInputString_jni ( env, J_bshape, BSHPLN, bshape );
   getFixedInputString_jni ( env, J_bframe, FRNMLN, bframe );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );

   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray ); 


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   if ( failed_c() )
   {
      /*
      De-allocate the confinement window before handling the error. 
      */
      zzdacell_c ( cnfine );

      SPICE_EXC_VAL( env, caller, retArray );      
   }


   /*
   Perform the search. 
   */
   gfoclt_c ( occtyp, front,  fshape, fframe, back,   bshape, 
              bframe, abcorr, obsrvr, step,   cnfine, result );



   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

 
   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}



/* 
Wrapper for CSPICE function gfpa_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfpa
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_target, 
   jstring       J_illum, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jstring       J_relate, 
   jdouble       J_refval, 
   jdouble       J_adjust, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
   {
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller = "CSPICE.gfpa";
   static SpiceChar        illum   [ BDNMLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        relate  [ RLOPLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             adjust;
   SpiceDouble           * cnfineData;
   SpiceDouble             refval;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_illum,  BDNMLN, illum  );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_relate, RLOPLN, relate );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   adjust  = (SpiceDouble) J_adjust;
   refval  = (SpiceDouble) J_refval;
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );
  
   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray );      


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   if ( failed_c() )
      {
      /*
      De-allocate the confinement window before handling the error. 
      */
      zzdacell_c ( cnfine );

      SPICE_EXC_VAL( env, caller, retArray );      
      }


   /*
   Perform the search. 
   */
   gfpa_c ( target, illum, abcorr,  obsrvr, relate, refval,
            adjust, step,  nintvls, cnfine, result );


   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

   if ( failed_c() )
      {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
      }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
   }



/* 
Wrapper for CSPICE function gfposc_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfposc
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_target, 
   jstring       J_frame, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jstring       J_crdsys, 
   jstring       J_coord, 
   jstring       J_relate, 
   jdouble       J_refval, 
   jdouble       J_adjust, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller = "CSPICE.gfposc";
   static SpiceChar        coord   [ CORDLN ];
   static SpiceChar        crdsys  [ CSYSLN ];
   static SpiceChar        frame   [ FRNMLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        relate  [ RLOPLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             adjust;
   SpiceDouble           * cnfineData;
   SpiceDouble             refval;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_frame,  FRNMLN, frame  );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_crdsys, CSYSLN, crdsys );
   getFixedInputString_jni ( env, J_coord,  CORDLN, coord  );
   getFixedInputString_jni ( env, J_relate, RLOPLN, relate );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   adjust  = (SpiceDouble) J_adjust;
   refval  = (SpiceDouble) J_refval;
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );

   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray ); 


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );


   /*
   Perform the search. 
   */
   gfposc_c ( target, frame,  abcorr, obsrvr, crdsys,  coord,
              relate, refval, adjust, step,   nintvls, cnfine, result );


   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

 
   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}



/* 
Wrapper for CSPICE function gfrefn_c
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_gfrefn
 ( JNIEnv *      env, 
   jclass        J_class,
   jdouble       J_t1, 
   jdouble       J_t2, 
   jboolean      J_s1,
   jboolean      J_s2   )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.gfrefn";

   SpiceDouble             retVal = 0.0;

   
   gfrefn_c ( (SpiceDouble ) J_t1,
              (SpiceDouble ) J_t2,
              (SpiceBoolean) J_s1,
              (SpiceBoolean) J_s2,
              &retVal              );

   SPICE_EXC_VAL( env, caller, ((jdouble)retVal) );

   return( (jdouble)retVal );
}



/* 
Wrapper for CSPICE function gfrepf_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_gfrepf
  (JNIEnv * env, 
   jclass             J_class )
{ 
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.gfrepf";
   
   /*
   Make the progress reporter finalization call.
   */
   gfrepf_c();

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function gfrepi_c 

NOTE: the input array has no control area.
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_gfrepi
  (JNIEnv * env, 
   jclass             J_class,
   jdoubleArray       J_window,
   jstring            J_begmsg,
   jstring            J_endmsg  )
{ 
   /*
   Local constants 
   */

   /*
   These string bounds should be kept consistent with
   those defined in the SPICELIB INCLUDE file gf.inc.
   The lengths here have been increased by 1 to account
   for null terminators.
   */
   #define MXBEGM     56
   #define MXENDM     14


   /*
   Local variables 
   */

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceCell             * window;
      
   static SpiceChar        begmsg [ MXBEGM ];
   static SpiceChar      * caller   = "CSPICE.gfrepi";
   static SpiceChar        endmsg [ MXENDM ];

   SpiceDouble           * windowData;

   SpiceInt                insize;

   /*
   Get local copies of the input strings.

   Note that the strings are presumed to be left-justified.
   */
   getFixedInputString_jni ( env, J_begmsg, MXBEGM, begmsg );
   getFixedInputString_jni ( env, J_endmsg, MXENDM, endmsg );

   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC( env ); 

   /*
   At this point we can create a dynamically allocated cell representing
   the input window.
   */   
   window = zzalcell_c ( SPICE_DP, insize, insize, 0, windowData );
  
   SPICE_EXC( env, caller );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );

   /*
   Make the progress reporter initialization call.
   */
   gfrepi_c ( window, begmsg, endmsg );


   /*
   De-allocate the cell.
   */
   zzdacell_c ( window );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function gfrepu_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_gfrepu
  (JNIEnv * env, 
   jclass             J_class,
   jdouble            J_ivbeg,
   jdouble            J_ivend,
   jdouble            J_time    )
{ 
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.gfrepi";
   
   /*
   Make the progress reporter update call.
   */
   gfrepu_c ( (SpiceDouble) J_ivbeg,
              (SpiceDouble) J_ivend,
              (SpiceDouble) J_time   );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function gfrfov_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfrfov
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_inst,
   jdoubleArray  J_raydir,
   jstring       J_rframe, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
{
   /*
   Local constants 
   */
   #define SHPLEN          SPICE_GF_SHPLEN

   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller = "CSPICE.gfrfov";
   static SpiceChar        inst    [ BDNMLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        rframe  [ FRNMLN ];



   SpiceDouble           * cnfineData;
   SpiceDouble             raydir  [3];
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_inst,   BDNMLN, inst   );
   getFixedInputString_jni ( env, J_rframe, FRNMLN, rframe );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the ray's direction vector. 
   */
   getVec3D_jni ( env, J_raydir, raydir );

   /*
   Fetch the input scalars. 
   */
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );

   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray ); 


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   SPICE_EXC_VAL( env, caller, retArray );      
  


   /*
   Perform the search. 
   */
   gfrfov_c ( inst,   raydir, rframe,  abcorr,
              obsrvr, step,   cnfine,  result );


   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

   /*
   If a CSPICE error occurred, free the result window now.
   */
   if ( failed_c() ) 
   {
      free ( result );
   }

   SPICE_EXC_VAL( env, caller, retArray ); 
 

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}



/* 
Wrapper for CSPICE function gfrr_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfrr
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_target, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jstring       J_relate, 
   jdouble       J_refval, 
   jdouble       J_adjust, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
   {

   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller = "CSPICE.gfrr";
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        relate  [ RLOPLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             adjust;
   SpiceDouble           * cnfineData;
   SpiceDouble             refval;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_relate, RLOPLN, relate );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   adjust  = (SpiceDouble) J_adjust;
   refval  = (SpiceDouble) J_refval;
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );
  
   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray );      


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   if ( failed_c() )
      {
      /*
      De-allocate the confinement window before handling the error. 
      */
      zzdacell_c ( cnfine );

      SPICE_EXC_VAL( env, caller, retArray );      
      }


   /*
   Perform the search. 
   */
   gfrr_c ( target, abcorr, obsrvr,  relate, refval,
            adjust, step,   nintvls, cnfine, result );


   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

   if ( failed_c() )
      {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
      }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
   }



/* 
Wrapper for CSPICE function gfsep_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfsep
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_targ1, 
   jstring       J_shape1, 
   jstring       J_frame1, 
   jstring       J_targ2, 
   jstring       J_shape2, 
   jstring       J_frame2, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jstring       J_relate, 
   jdouble       J_refval, 
   jdouble       J_adjust, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller = "CSPICE.gfsep";
   static SpiceChar        frame1  [ FRNMLN ];
   static SpiceChar        frame2  [ FRNMLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        relate  [ RLOPLN ];
   static SpiceChar        shape1  [ BSHPLN ];
   static SpiceChar        shape2  [ BSHPLN ];
   static SpiceChar        targ1   [ BDNMLN ];
   static SpiceChar        targ2   [ BDNMLN ];

   SpiceDouble             adjust;
   SpiceDouble           * cnfineData;
   SpiceDouble             refval;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;


   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_targ1,  BDNMLN, targ1  );
   getFixedInputString_jni ( env, J_shape1, BSHPLN, shape1 );
   getFixedInputString_jni ( env, J_frame1, FRNMLN, frame1 );
   getFixedInputString_jni ( env, J_targ2,  BDNMLN, targ2  );
   getFixedInputString_jni ( env, J_shape2, BSHPLN, shape2 );
   getFixedInputString_jni ( env, J_frame2, FRNMLN, frame2 );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_relate, RLOPLN, relate );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   adjust  = (SpiceDouble) J_adjust;
   refval  = (SpiceDouble) J_refval;
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );
  
   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray ); 


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;


   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );

   SPICE_EXC_VAL( env, caller, retArray ); 


   /*
   Perform the search. 
   */ 
   gfsep_c (  targ1,  shape1, frame1,  targ2,  shape2, 
              frame2, abcorr, obsrvr,  relate, refval,
              adjust, step,   nintvls, cnfine, result );


   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

 
   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}



/* 
Wrapper for CSPICE function gfsntc_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfsntc
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_target, 
   jstring       J_fixref, 
   jstring       J_method, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jstring       J_dref, 
   jdoubleArray  J_dvec,
   jstring       J_crdsys, 
   jstring       J_coord, 
   jstring       J_relate, 
   jdouble       J_refval, 
   jdouble       J_adjust, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller = "CSPICE.gfsntc";
   static SpiceChar        coord   [ CORDLN ];
   static SpiceChar        crdsys  [ CSYSLN ];
   static SpiceChar        dref    [ FRNMLN ];
   static SpiceChar        fixref  [ FRNMLN ];
   static SpiceChar        method  [ METHLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        relate  [ RLOPLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             adjust;
   SpiceDouble           * cnfineData;
   SpiceDouble             dvec    [3];
   SpiceDouble             refval;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_fixref, FRNMLN, fixref );
   getFixedInputString_jni ( env, J_method, METHLN, method );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_dref,   FRNMLN, dref   );
   getFixedInputString_jni ( env, J_crdsys, CSYSLN, crdsys );
   getFixedInputString_jni ( env, J_coord,  CORDLN, coord  );
   getFixedInputString_jni ( env, J_relate, RLOPLN, relate );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the ray direction vector. 
   */
   getVec3D_jni ( env, J_dvec, dvec );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   adjust  = (SpiceDouble) J_adjust;
   refval  = (SpiceDouble) J_refval;
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );
  
   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray ); 


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   if ( failed_c() )
   {
      /*
      De-allocate the confinement window before handling the error. 
      */
      zzdacell_c ( cnfine );

      SPICE_EXC_VAL( env, caller, retArray );      
   }


   /*
   Perform the search. 
   */
   gfsntc_c ( target, fixref,  method, abcorr, 
              obsrvr, dref,    dvec,   crdsys,  
              coord,  relate,  refval, adjust, 
              step,   nintvls, cnfine, result  );


   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }


   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}
 


/* 
Wrapper for CSPICE function gfsstp_c
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_gfsstp
 ( JNIEnv *      env, 
   jclass        J_class,
   jdouble       J_step    )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.gfsstp";

   
   gfsstp_c ( (SpiceDouble ) J_step );


   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function gfstep_c
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_gfstep
 ( JNIEnv *      env, 
   jclass        J_class,
   jdouble       J_et    )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.gfstep";

   SpiceDouble             retVal = 0.0;

   
   gfstep_c ( (SpiceDouble ) J_et,
              &retVal              );


   SPICE_EXC_VAL( env, caller, ((jdouble)retVal) );

   return( (jdouble)retVal );
}



/* 
Wrapper for CSPICE function gfstol_c
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_gfstol
 ( JNIEnv *      env, 
   jclass        J_class,
   jdouble       J_tol    )
   {
   
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.gfstol";

   
   gfstol_c ( (SpiceDouble ) J_tol );


   SPICE_EXC( env, caller );
   }


 
/* 
Wrapper for CSPICE function gfsubc_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfsubc
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_target, 
   jstring       J_fixref, 
   jstring       J_method, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jstring       J_crdsys, 
   jstring       J_coord, 
   jstring       J_relate, 
   jdouble       J_refval, 
   jdouble       J_adjust, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller = "CSPICE.gfsubc";
   static SpiceChar        coord   [ CORDLN ];
   static SpiceChar        crdsys  [ CSYSLN ];
   static SpiceChar        fixref  [ FRNMLN ];
   static SpiceChar        method  [ METHLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        relate  [ RLOPLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             adjust;
   SpiceDouble           * cnfineData;
   SpiceDouble             refval;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_fixref, FRNMLN, fixref );
   getFixedInputString_jni ( env, J_method, METHLN, method );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_crdsys, CSYSLN, crdsys );
   getFixedInputString_jni ( env, J_coord,  CORDLN, coord  );
   getFixedInputString_jni ( env, J_relate, RLOPLN, relate );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   adjust  = (SpiceDouble) J_adjust;
   refval  = (SpiceDouble) J_refval;
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );

   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray ); 


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   if ( failed_c() )
   {
      /*
      De-allocate the confinement window before handling the error. 
      */
      zzdacell_c ( cnfine );

      SPICE_EXC_VAL( env, caller, retArray );      
   }


   /*
   Perform the search. 
   */
   gfsubc_c ( target, fixref, method, abcorr, obsrvr, crdsys,  coord,
              relate, refval, adjust, step,   nintvls, cnfine, result );

   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

 
   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}



/* 
Wrapper for CSPICE function gftfov_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gftfov
 ( JNIEnv *      env, 
   jclass        J_class,
   jstring       J_inst,
   jstring       J_target, 
   jstring       J_tshape, 
   jstring       J_tframe, 
   jstring       J_abcorr, 
   jstring       J_obsrvr, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
{
   /*
   Local constants 
   */
   #define SHPLEN          SPICE_GF_SHPLEN

   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller = "CSPICE.gftfov";
   static SpiceChar        inst    [ BDNMLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];
   static SpiceChar        tframe  [ FRNMLN ];
   static SpiceChar        tshape  [ SHPLEN ];


   SpiceDouble           * cnfineData;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_inst,   BDNMLN, inst   );
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_tshape, SHPLEN, tshape );
   getFixedInputString_jni ( env, J_tframe, FRNMLN, tframe );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );

   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray ); 


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   if ( failed_c() )
   {
      /*
      De-allocate the confinement window before handling the error. 
      */
      zzdacell_c ( cnfine );

      SPICE_EXC_VAL( env, caller, retArray );      
   }


   /*
   Perform the search. 
   */
   gftfov_c ( inst,   target, tshape,  tframe, abcorr,
              obsrvr, step,   cnfine,  result         );


   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

 
   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}



/* 
Wrapper for CSPICE function gfuds_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_gfuds
 ( JNIEnv *      env, 
   jclass        J_class,
   jobject       J_GFScalarQuantity,
   jstring       J_relate, 
   jdouble       J_refval, 
   jdouble       J_adjust, 
   jdouble       J_step, 
   jint          J_nintvls, 
   jdoubleArray  J_cnfine   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * cnfine;
   SpiceCell             * result;

   static SpiceChar      * caller = "CSPICE.gfuds";
   static SpiceChar        relate  [ RLOPLN ];

   SpiceDouble             adjust;
   SpiceDouble           * cnfineData;
   SpiceDouble             refval;
   SpiceDouble             step;

   SpiceInt                cnfineCard;
   SpiceInt                nintvls;
   SpiceInt                resultCard;
   SpiceInt                resultSize;


   /*
   Pass the input GFScalarQuantity object to the GFScalarQuantity
   utility package.
   */
   zzGFScalarQuantityInit_jni( env, J_GFScalarQuantity );

   JNI_EXC_VAL( env, retArray );


   /*
   Fetch the input string. 
   */
   getFixedInputString_jni ( env, J_relate, RLOPLN, relate );

   JNI_EXC_VAL( env, retArray );

   /*
   Fetch the input scalars. 
   */
   adjust  = (SpiceDouble) J_adjust;
   refval  = (SpiceDouble) J_refval;
   step    = (SpiceDouble) J_step;
   nintvls = (SpiceInt)    J_nintvls;

   /*
   Fetch the input Java confinement array into a local 
   dynamically allocated array.
   */
   getVecGD_jni ( env, J_cnfine, &cnfineCard, &cnfineData );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a dynamically allocated CSPICE window representing the
   confinement window. 
   */
   cnfine = zzalcell_c ( SPICE_DP, cnfineCard, cnfineCard, 0, cnfineData );

   /*
   The cell we just created has its own data, so we can release the
   confinement window data provided by the input argument J_cnfine.
  
   We must free the Java data regardless of whether the cell creation
   call succeeded.
   */
   freeVecGD_jni ( env, J_cnfine, cnfineData );

   SPICE_EXC_VAL( env, caller, retArray ); 


   /*
   Create an empty, dynamically allocated CSPICE result window.
   */
   resultSize = 2 * nintvls;

   result = zzalcell_c ( SPICE_DP, resultSize, 0, 0, 0 );
  
   if ( failed_c() )
   {
      /*
      De-allocate the confinement window before handling the error. 
      */
      zzdacell_c ( cnfine );

      SPICE_EXC_VAL( env, caller, retArray );      
   }


   /*
   Perform the search. Pass in the GFScalarSearch utility
   adapters as the user-defined functions.
   */
   gfuds_c ( zzudfunc_jni,  zzudqdec_jni,   
             relate,        refval,        adjust, 
             step,          nintvls,       cnfine,      result );


   /*
   Regardless of whether an error occurred, free the confinement
   window now. 
   */
   zzdacell_c ( cnfine );

 
   if ( failed_c() )
   {
      /*
      Since we have a SPICE error, we won't create an output
      array; hence we don't need the result window.
      */
      zzdacell_c ( result );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Create a Java array containing the data portion of the result window.

   Below, "cardinality" refers to the Cell version of this concept.
   */ 
   resultCard = card_c ( result );

   createVecGD_jni ( env, resultCard, (SpiceDouble *)result->data, &retArray );

   /*
   Now we're done with the result window. 
   */
   zzdacell_c ( result );

  
   return ( retArray );
}



/* 
Wrapper for CSPICE function gipool_c
*/
JNIEXPORT jintArray JNICALL Java_spice_basic_CSPICE_gipool
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_name,
   jint               J_start,
   jint               J_room   )
{
   /*
   Local variables 
   */
   jintArray               retArray = 0;

   SpiceBoolean            found;     
 
   static SpiceChar      * caller   = "CSPICE.gipool";
   static SpiceChar        dtype[1];
   static SpiceChar        kvname   [ KVNMLN ];
   static SpiceChar        message  [ LMSGLN ];

   SpiceInt              * intdata = 0;
   SpiceInt                n;
   SpiceInt                needed;
   SpiceInt                size;

 
   /*
   Capture the kernel variable name in a local buffer, then
   release the dynamically allocated version of the name.
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvname );
   
   JNI_EXC_VAL ( env, retArray );

   /*
   See whether the requested kernel variable is present in the kernel
   pool, and whether it has numeric data type.
   */
   dtpool_c ( kvname, &found, &size, dtype );

   SPICE_EXC_VAL ( env, caller, retArray );      
   

   if (  ( !found )  ||  ( dtype[0] != 'N' )  )
   {
      /*
      We're going to throw a "kernel variable not found" 
      exception. The exception message will indicate
      the specific cause.
      */

      if ( !found )
      {
         strncpy ( message, 
                   "Kernel variable # was not found in the "
                   "kernel pool.",
                   LMSGLN                                   );
      }
      else
      {
         strncpy ( message, 
                   "Numeric kernel variable # "
                   "was not found in the kernel pool. "
                   "A character variable having this name "
                   "is present in the pool.",
                   LMSGLN                                );
      }

      /*
      Substitute the kernel variable name into the message. 
      */
      repmc_c ( message, "#", kvname, LMSGLN, message );

      /*
      Throw the exception and return. 
      */
      zzThrowException_jni ( env, KVNF_EXC, message );

      return retArray;
   }

   /*
   At this point we know the variable exists. Allocate enough
   memory to hold the requested portion of the data.
   */
   needed = mini_c ( 2,  (SpiceInt)J_room,  (size-(SpiceInt)J_start) );

   /*
   Adjust `needed' so we don't get an allocation error.
   */
   needed = maxi_c ( 2, needed, 1 );

   intdata = alloc_SpiceInt_C_array ( 1, needed );


   SPICE_EXC_VAL ( env, caller, retArray );
 
   /*
   Let gipool_c diagnose bad values of J_start or J_room.
   */
   gipool_c ( kvname, (SpiceInt)J_start, (SpiceInt)J_room, &n, intdata, 
                                                                     &found );


   if ( failed_c() )
   {
      /*
      Free the dynamically allocated memory before leaving.
      */
      free_SpiceMemory( (void *)  intdata );

      SPICE_EXC_VAL ( env, caller, retArray );
   }

   /*
   Create the output array. 
   */
   createVecGI_jni ( env, needed, intdata, &retArray );

   /*
   Free the dynamically allocated memory before leaving.
   */
   free_SpiceMemory( (void *)  intdata );

   return retArray;
}



/* 
Wrapper for CSPICE function gnpool_c
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_gnpool
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_name,
   jint               J_start,
   jint               J_room  )
{
   /*
   Local variables 
   */

   integer                 start;
   integer                 room;

   jobjectArray            retArray = 0;

   logical                 yes;

   static SpiceChar      * caller   = "CSPICE.gnpool";
   static SpiceChar        kvtemp  [ KVNMLN ];

   SpiceInt                n;
   SpiceInt                nBytes;
   SpiceInt                needed;
   
   void                  * fArrayPtr;


   /*
   Capture the kernel variable template in a local buffer, then
   release the dynamically allocated version of the template.
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvtemp );
   
   JNI_EXC_VAL ( env, retArray );

   /*
   Don't accept an empty input template.

   This unusual check is performed here because this routine
   calls the f2c'd routine gnpool_ instead of the wrapper
   gnpool_c. The wrapper properly handles an empty input string;
   the f2c'd routine cannot.

   The wrapper is avoided in order to improve speed and reduce
   use of dynamically allocated memory.
   */
   if ( strlen(kvtemp) == 0 )
   {
      chkin_c ( caller                               );
      setmsg_c( "Input template is an empty string." );
      sigerr_c( "SPICE(EMPTYSTRING)"                 );
      chkout_c( caller                               );

      zzThrowSpiceErrorException_jni( env, caller );

      return ( retArray );
   }

   /*
   At this point we know the variable exists. Allocate enough
   memory to hold the requested portion of the data.
   */
   start = (integer) J_start;  
   room  = (integer) J_room;

   needed    = maxi_c ( 2,  room,  1 );

   nBytes    = needed * F_KVSTLN;

   fArrayPtr = alloc_SpiceMemory ( (size_t)nBytes );

   SPICE_EXC_VAL ( env, caller, retArray );      


   /*
   Fetch the requested file names. 

   We call the f2c'd routine to avoid redundant dynamic
   memory allocation.
   */
  
   gnpool_( ( char    * ) kvtemp,
            ( integer * ) &start,
            ( integer * ) &room,
            ( integer * ) &n,
            ( char    * ) fArrayPtr,
            ( logical * ) &yes,
            ( ftnlen    ) strlen(kvtemp),
            ( ftnlen    ) F_KVSTLN        );


   if ( failed_c() )
   {
     /*
      Free the Fortran-style string array. 
      */
      free_SpiceMemory ( (void *)  fArrayPtr );
      
      SPICE_EXC_VAL ( env, caller, retArray );      
   }

   /*
   Create an array of Java strings from the Fortran-style array.
   */   
   retArray = createJavaStringArray_jni ( env, 
                                          n, 
                                          F_KVSTLN, 
                                          fArrayPtr );
   /*
   Free the Fortran-style string array. 
   */
   free_SpiceMemory ( (void *)  fArrayPtr );
                
   return ( retArray );
}



/* 
Wrapper for CSPICE function inrypl_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_inrypl
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_vertex,
   jdoubleArray       J_dir,
   jdoubleArray       J_plane,
   jintArray          J_nxpts,
   jdoubleArray       J_xpt     )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.inrypl";

   SpiceDouble             planeArray [PLMAX];
   SpiceDouble             vertex     [3];
   SpiceDouble             dir        [3];
   SpiceDouble             xpt        [3];

   SpiceInt                nxpts;

   SpicePlane              plane;

 
   /*
   Get the input vectors in one-dimensional C arrays. 
   */
   getVec3D_jni     ( env, J_vertex,       (SpiceDouble *)vertex     );
   getVec3D_jni     ( env, J_dir,          (SpiceDouble *)dir        );
   getVecFixedD_jni ( env, J_plane, PLMAX, (SpiceDouble *)planeArray );

   JNI_EXC( env );

   /*
   Create a SPICE plane using the normal vector and constant
   from the input array.
   */
   nvc2pl_c ( planeArray, planeArray[3], &plane );

   /*
   Compute the intercept, if it exists.
   */
   inrypl_c ( vertex, dir, &plane, &nxpts, xpt );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Update the output arrays.
   */
   updateVecGI_jni ( env, 1, &nxpts, J_nxpts );
   updateVec3D_jni ( env,    xpt,    J_xpt   );
}



/* 
Wrapper for CSPICE function illumf_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_illumf
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_method, 
   jstring            J_target, 
   jstring            J_ilusrc, 
   jdouble            J_et,
   jstring            J_fixref,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jdoubleArray       J_spoint, 
   jdoubleArray       J_trgepc, 
   jdoubleArray       J_srfvec,
   jdoubleArray       J_angles,
   jbooleanArray      J_visibl,
   jbooleanArray      J_lit     )
{
   /*
   Local variables 
   */
   SpiceBoolean            visibl;
   SpiceBoolean            lit;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller  = "CSPICE.illumf";
   static SpiceChar        fixref  [ FRNMLN ];
   static SpiceChar        ilusrc  [ BDNMLN ];
   static SpiceChar      * method;
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];
   
   SpiceDouble             angles  [ 3 ];
   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];
   SpiceDouble             srfvec  [ 3 ];
   SpiceDouble             trgepc;
  
   SpiceInt                methodLen;


   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr  );
   getFixedInputString_jni ( env, J_ilusrc, BDNMLN,     ilusrc  );
   getFixedInputString_jni ( env, J_fixref, FRNMLN,     fixref  );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr  );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target  );
   getVarInputString_jni   ( env, J_method, &methodLen, &method );
   getVec3D_jni            ( env, J_spoint,             spoint  );

   JNI_EXC( env );

   et = (SpiceDouble)J_et;

   illumf_c ( method,  target,   ilusrc,   et,      fixref,  
              abcorr,  obsrvr,   spoint,   &trgepc, srfvec, 
              angles,  angles+1, angles+2, &visibl, &lit    );

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
   updateVecGD_jni ( env, 1, &trgepc, J_trgepc );
   updateVec3D_jni ( env,    srfvec,  J_srfvec );
   updateVec3D_jni ( env,    angles,  J_angles );
   updateVecGB_jni ( env, 1, &visibl, J_visibl );
   updateVecGB_jni ( env, 1, &lit,    J_lit    );
}




/* 
Wrapper for CSPICE function ilumin_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ilumin
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_method, 
   jstring            J_target, 
   jdouble            J_et,
   jstring            J_fixref,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jdoubleArray       J_spoint, 
   jdoubleArray       J_trgepc, 
   jdoubleArray       J_srfvec,
   jdoubleArray       J_angles  )
{
   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller  = "CSPICE.ilumin";
   static SpiceChar        fixref  [ FRNMLN ];
   static SpiceChar      * method;
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];
   
   SpiceDouble             angles  [ 3 ];
   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];
   SpiceDouble             srfvec  [ 3 ];
   SpiceDouble             trgepc;
  
   SpiceInt                methodLen;


   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr  );
   getFixedInputString_jni ( env, J_fixref, FRNMLN,     fixref  );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr  );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target  );
   getVarInputString_jni   ( env, J_method, &methodLen, &method );
   getVec3D_jni            ( env, J_spoint,             spoint  );

   JNI_EXC( env );

   et = (SpiceDouble)J_et;

   
   ilumin_c ( method,  target,  et,     fixref,  abcorr,    obsrvr,  
              spoint,  &trgepc, srfvec, angles,  angles+1,  angles+2 );


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
   updateVecGD_jni ( env, 1, &trgepc, J_trgepc );
   updateVec3D_jni ( env,    srfvec,  J_srfvec );
   updateVec3D_jni ( env,    angles,  J_angles );
}





/* 
Wrapper for CSPICE function inedpl_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_inedpl
  (JNIEnv * env, 
   jclass             J_class, 
   jdouble            J_a,
   jdouble            J_b,
   jdouble            J_c,
   jdoubleArray       J_plane,
   jdoubleArray       J_ellipse,
   jbooleanArray      J_found    )
{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   static SpiceChar      * caller = "CSPICE.inedpl";

   SpiceDouble             ellipseArray [ELMAX];
   SpiceDouble             planeArray   [PLMAX];

   SpiceEllipse            ellipse;
   SpicePlane              plane;

   /*
   Get the input plane in a one-dimensional C array. 
   */
   getVecFixedD_jni ( env, J_plane, PLMAX, (SpiceDouble *)planeArray );
   JNI_EXC( env );

   /*
   Transfer the plane to a SPICE plane structure. 
   */
   MOVED( planeArray, 3, plane.normal );
   plane.constant = planeArray[3];

   /*
   Let CSPICE perform the computation. 
   */
   inedpl_c ( (SpiceDouble) J_a,
              (SpiceDouble) J_b,
              (SpiceDouble) J_c,
              &plane,
              &ellipse,
              &found             );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   if ( found )
   {
      /*
      Transfer the components of the ellipse to a 1-d array.
      */
      MOVED( ellipse.center,     3,  ellipseArray   );
      MOVED( ellipse.semiMajor,  3,  ellipseArray+3 );
      MOVED( ellipse.semiMinor,  3,  ellipseArray+6 );
   }

   /*
   Set the output arguments. 
   */
   updateVecGD_jni ( env, ELMAX, ellipseArray, J_ellipse );
   updateVecGB_jni ( env, 1,     &found,       J_found   );
}




/* 
Wrapper for CSPICE function inelpl_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_inelpl
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_ellipse,
   jdoubleArray       J_plane,
   jintArray          J_nxpts,
   jdoubleArray       J_xpt1,  
   jdoubleArray       J_xpt2   )  
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.inelpl";

   SpiceDouble             ellipseArray [ELMAX];
   SpiceDouble             planeArray   [PLMAX];
   SpiceDouble             xpt1         [3];
   SpiceDouble             xpt2         [3];

   SpiceInt                nxpts;

   SpiceEllipse            ellipse;
   SpicePlane              plane;

   /*
   Fetch the Java arrays.
   */
   getVecFixedD_jni( env, J_ellipse, ELMAX, ellipseArray );
   getVecFixedD_jni( env, J_plane,   PLMAX, planeArray   );

   JNI_EXC( env );

   /*
   Create a SPICE ellipse and plane from the input arrays. 
   */
   cgv2el_c ( ellipseArray, ellipseArray+3, ellipseArray+6, &ellipse );
   nvc2pl_c ( planeArray,   planeArray[3],  &plane );


   inelpl_c ( &ellipse, &plane, &nxpts, xpt1, xpt2 );


   SPICE_EXC ( env, caller );

   /*
   Set output arguments. 
   */
   updateVecGI_jni( env, 1,  &nxpts,  J_nxpts );  
   updateVec3D_jni( env,     xpt1,    J_xpt1  );   
   updateVec3D_jni( env,     xpt2,    J_xpt2  );   
}



/* 
Wrapper for CSPICE function invert_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_invert
  (JNIEnv           * env, 
   jclass             J_class, 
   jobjectArray       J_m      )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.invert";

   SpiceDouble             m    [3][3];
   SpiceDouble             mout [3][3];

   /*
   Get the input matrix in a C array. 
   */
   getMat33D_jni ( env, J_m, m );

   JNI_EXC_VAL ( env, retArray );

   /*
   Make the CSPICE call. 
   */
   invert_c ( m, mout );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( mout ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function isrot_c 
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_isrot
  ( JNIEnv     *  env, 
    jclass        cls,
    jobjectArray  J_m,
    jdouble       J_ntol,
    jdouble       J_dtol  )
{

   /*
   Local variables
   */
   static SpiceChar      * caller = "CSPICE.isrot";


   SpiceBoolean            retval = 0;

   SpiceDouble             m  [3][3];


   /*
   Get the input matrix in a C array. 
   */
   getMat33D_jni ( env,  J_m,  m  );

   JNI_EXC_VAL( env,  ((jboolean)retval)  );

   retval = isrot_c ( m, (SpiceDouble)J_ntol, (SpiceDouble)J_dtol );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC_VAL( env, caller,  ((jboolean)retval) );

   /*
   Normal return. 
   */
   return (  (jboolean)retval  );
}



/* 
Wrapper for CSPICE function j1900_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_j1900
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)j1900_c()  );
}



/* 
Wrapper for CSPICE function j1950_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_j1950
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)j1950_c()  );
}



/* 
Wrapper for CSPICE function j2000_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_j2000
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)j2000_c()  );
}



/* 
Wrapper for CSPICE function j2000_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_j2100
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)j2100_c()  );
}



/* 
Wrapper for CSPICE function jyear_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_jyear
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)jyear_c()  );
}



/* 
Wrapper for CSPICE function kclear_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_kclear
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.kclear";

   kclear_c();

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function kdata_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_kdata
  ( JNIEnv *        env, 
    jclass          J_class,
    jint            J_which,
    jstring         J_kind,
    jobjectArray    J_file,
    jobjectArray    J_filtyp,
    jobjectArray    J_source,
    jintArray       J_handle,
    jbooleanArray   J_found   )
{
   /*
   Local variables 
   */   
   jstring                 jFile;
   jstring                 jFilTyp;
   jstring                 jSource;

   SpiceBoolean            found;

   static SpiceChar      * caller = "CSPICE.kclear";
   static SpiceChar        file   [ FNAMLN ];
   static SpiceChar        filtyp [ KTYPLN ];
   static SpiceChar        kind   [ KTYPLN ];
   static SpiceChar        source [ FNAMLN ];

   SpiceInt                handle;
   SpiceInt                which;


   which = (SpiceInt) J_which;
   
   getFixedInputString_jni ( env, J_kind, KTYPLN, kind );

   JNI_EXC( env );


   kdata_c( which,  kind,   FNAMLN, KTYPLN,  FNAMLN, 
            file,   filtyp, source, &handle, &found  );


   SPICE_EXC( env, caller );

   /*
   Fill in the output arrays. 
   */
   jFile = createJavaString_jni ( env, file );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_file, 0, jFile );
   JNI_EXC( env );


   jFilTyp = createJavaString_jni ( env, filtyp );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_filtyp, 0, jFilTyp );
   JNI_EXC( env );


   jSource = createJavaString_jni ( env, source );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_source, 0, jSource );
   JNI_EXC( env );

   
   updateVecGI_jni ( env, 1, &handle, J_handle );
   updateVecGB_jni ( env, 1, &found,  J_found  );      
}



/* 
Wrapper for CSPICE function kinfo_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_kinfo
  ( JNIEnv *        env, 
    jclass          J_class,
    jstring         J_file,
    jobjectArray    J_filtyp,
    jobjectArray    J_source,
    jintArray       J_handle,
    jbooleanArray   J_found   )
{
   /*
   Local variables 
   */   
   jstring                 jFilTyp;
   jstring                 jSource;

   SpiceBoolean            found;

   static SpiceChar      * caller = "CSPICE.kclear";
   static SpiceChar        file   [ FNAMLN ];
   static SpiceChar        filtyp [ KTYPLN ];
   static SpiceChar        source [ FNAMLN ];

   SpiceInt                handle;


   getFixedInputString_jni ( env, J_file, FNAMLN, file );

   JNI_EXC( env );


   kinfo_c( file,   KTYPLN, FNAMLN, 
            filtyp, source, &handle, &found );


   SPICE_EXC( env, caller );

   /*
   Fill in the output arrays. 
   */
   jFilTyp = createJavaString_jni ( env, filtyp );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_filtyp, 0, jFilTyp );
   JNI_EXC( env );


   jSource = createJavaString_jni ( env, source );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_source, 0, jSource );
   JNI_EXC( env );

   
   updateVecGI_jni ( env, 1, &handle, J_handle );
   updateVecGB_jni ( env, 1, &found,  J_found  );      
}




/* 
Wrapper for CSPICE function ktotal_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_ktotal
  ( JNIEnv *  env, 
    jclass    J_class,
    jstring   J_kind  ) 
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller = "CSPICE.kclear";
   static SpiceChar        kind  [ KTYPLN ];

   SpiceInt                count = 0;


   getFixedInputString_jni ( env, J_kind, KTYPLN, kind );

   JNI_EXC_VAL( env, ((jint)count) );


   ktotal_c( kind, &count );

   SPICE_EXC_VAL( env, caller, ((jint)count) );


   return ( (jint)count );
}







/* 
Wrapper for CSPICE function latcyl_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_latcyl
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_radius,
   jdouble            J_longitude,
   jdouble            J_latitude   )
{
   /*
   Local variables 
   */
   jdoubleArray            jVec = (jdoubleArray)0;

   SpiceDouble             cVec[3];


   /*
   Perform the conversion. 
   */
   latcyl_c ( (SpiceDouble) J_radius,
              (SpiceDouble) J_longitude,
              (SpiceDouble) J_latitude,
              cVec, 
              cVec+1, 
              cVec+2                     );
   
   /*
   Create an output Java vector containing the result. 
   */
   createVec3D_jni ( env, cVec, &jVec );
   
   return jVec;
}



/* 
Wrapper for CSPICE function latrec_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_latrec
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_radius,
   jdouble            J_longitude,
   jdouble            J_latitude   )
{
   /*
   Local variables 
   */
   jdoubleArray            jVec = (jdoubleArray)0;

   SpiceDouble             cVec[3];


   /*
   Perform the conversion. 
   */
   latrec_c ( (SpiceDouble) J_radius,
              (SpiceDouble) J_longitude,
              (SpiceDouble) J_latitude,
              cVec                      );
   
   /*
   Create an output Java vector containing the result. 
   */
   createVec3D_jni ( env, cVec, &jVec );
   
   return jVec;
}



/* 
Wrapper for CSPICE function latsph_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_latsph
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_radius,
   jdouble            J_longitude,
   jdouble            J_latitude   )
{
   /*
   Local variables 
   */
   jdoubleArray            jVec = (jdoubleArray)0;

   SpiceDouble             cVec[3];


   /*
   Perform the conversion. 
   */
   latsph_c ( (SpiceDouble) J_radius,
              (SpiceDouble) J_longitude,
              (SpiceDouble) J_latitude,
              cVec, 
              cVec+1, 
              cVec+2                     );
   
   /*
   Create an output Java vector containing the result. 
   */
   createVec3D_jni ( env, cVec, &jVec );
   
   return jVec;
}






/* 
Wrapper for CSPICE function latsrf_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_latsrf
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_method,
   jstring            J_target,
   jdouble            J_et,
   jstring            J_fixref,
   jint               J_npts,
   jobjectArray       J_lonlat   )
{
   /*
   Local variables 
   */
   jobjectArray            jSrfPts = (jobjectArray)0;

   static SpiceChar      * caller   = "CSPICE.latsrf";
   SpiceChar             * method;
   SpiceChar               fixref[ FRNMLN ];
   SpiceChar               target[ BDNMLN ];

   SpiceDouble             et;
   SpiceDouble          (* lonlat)[2];
   SpiceDouble          (* srfpts)[3];

   SpiceInt                methodLen;
   SpiceInt                ncols;
   SpiceInt                npts;
   SpiceInt                nrows;


   /*
   Get local copies of input scalars. 
   */
   et   = (SpiceDouble) J_et;
   npts = (SpiceInt   ) J_npts;

   if ( npts < 1 ) 
   {
      /*
      The array defining the input coordinates must be non-empty. 
      */
      setmsg_c ( "Coordinate pair count was #; this count must "
                 "be at least 1."                                );
      errint_c ( "#", npts                                       );
      sigerr_c ( "SPICE(INVALIDCOUNT)"                           );

      SPICE_EXC_VAL( env, caller, jSrfPts );
   }


   /*
   Fetch the input strings. 
   */
   getVarInputString_jni   ( env, J_method, &methodLen, &method );
   JNI_EXC_VAL( env, jSrfPts );

   getFixedInputString_jni ( env, J_target, BDNMLN,     target  );
   JNI_EXC_VAL( env, jSrfPts );

   getFixedInputString_jni ( env, J_fixref, FRNMLN,     fixref  );
   JNI_EXC_VAL( env, jSrfPts );

   /*
   Fetch the input longitude/latitude array. 
   */
   getMatGD_jni ( env, J_lonlat, &nrows, &ncols, (SpiceDouble **)&lonlat );
   JNI_EXC_VAL( env, jSrfPts );

   /*
   Check the dimensions of the `lonlat' array.
   */
   if (  ( nrows < npts ) || ( ncols != 2 )  )
   {
      /*
      Free the dynamic memory that has been allocated so far. 
      */
      free_SpiceMemory( (void *)  lonlat );

      freeVarInputString_jni ( env, J_method, method );


      setmsg_c ( "Input longitude/latitude array must have "
                 "row count at least as large as the input npts, "
                 "and must have column count equal to 2."
                 "The actual array dimensions were #x#."           );
      errint_c ( "#", nrows                                        );
      errint_c ( "#", ncols                                        );
      sigerr_c ( "SPICE(INVALIDDIMENSION)"                         );
      
      SPICE_EXC_VAL( env, caller, jSrfPts );
   }


   /*
   Allocate a dynamic array to hold the output surface points. 
   */
   srfpts = ( SpiceDouble(*)[3] )alloc_SpiceDouble_C_array( npts, 3 );

   SPICE_EXC_VAL( env, caller, jSrfPts );


   /*
   Find the surface points. 
   */
   latsrf_c ( method, target, et, fixref, npts, lonlat, srfpts );
   

   /*
   Regardless of whether the call succeeded, free the dynamic
   memory used to hold inputs. 

   Free the method string. 
   */
   freeVarInputString_jni ( env, J_method, method );

   /*
   Free the longitude/latitude array. 
   */
   free_SpiceMemory( (void *)  lonlat );


   /*
   Create an output Java vector containing the result. 
   */
   if ( !failed_c() )
   {
      createMatGD_jni ( env, npts, 3, srfpts, &jSrfPts );
      JNI_EXC_VAL( env, jSrfPts );
   }

   /*
   Free the dynamically allocated C array used to store
   the computed surface points. 
   */
   free_SpiceMemory( (void *)  srfpts );

   /*
   Handle any SPICE error generated by latsrf_c.
   */
   SPICE_EXC_VAL( env, caller, jSrfPts );

   
   return jSrfPts;
}








/* 
Wrapper for CSPICE function ldpool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ldpool
  ( JNIEnv *  env, 
    jclass    J_class, 
    jstring   J_file ) 
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.ldpool";
   SpiceChar             * file;
   SpiceInt                fileLen;

   /*
   Fetch input string into dynmically allocated memory. 
   Check for a JNI exception.
   */
   getVarInputString_jni ( env, J_file, &fileLen, &file );
   JNI_EXC( env );


   /*
   Load the file. 
   */
   ldpool_c ( file );


   /*
   Free the dynamically allocated memory.
   */
   freeVarInputString_jni ( env, J_file, file );

   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC( env, caller );
}





/* 
Wrapper for CSPICE function limbpt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_limbpt
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_method,
   jstring            J_target,
   jdouble            J_et,
   jstring            J_fixref,
   jstring            J_abcorr,
   jstring            J_corloc,
   jstring            J_obsrvr,
   jdoubleArray       J_refvec,
   jdouble            J_rolstp,
   jint               J_ncuts,
   jdouble            J_schstp,
   jdouble            J_soltol,
   jint               J_maxn,
   jintArray          J_npts,
   jobjectArray       J_points,
   jdoubleArray       J_epochs,
   jobjectArray       J_tangts    )
{
   /*
   Local variables 
   */
   SpiceChar               abcorr[ CORRLN ];
   static SpiceChar      * caller   = "CSPICE.limbpt";
   SpiceChar               corloc[ LOCLEN ];
   SpiceChar             * method;
   SpiceChar               fixref[ FRNMLN ];
   SpiceChar               obsrvr[ BDNMLN ];
   SpiceChar               target[ BDNMLN ];

   SpiceDouble           * epochs;
   SpiceDouble             et;
   SpiceDouble          (* points)[3];
   SpiceDouble             refvec [3];
   SpiceDouble             rolstp;
   SpiceDouble             schstp;
   SpiceDouble             soltol;
   SpiceDouble          (* tangts)[3];
   
   SpiceInt                i;
   SpiceInt                maxn;
   SpiceInt                methodLen;
   SpiceInt                ncuts;
   SpiceInt                needed;
   SpiceInt              * npts;
   SpiceInt                totpts;

   /*
   Get local copies of input scalars. 
   */
   et     = (SpiceDouble) J_et;
   rolstp = (SpiceDouble) J_rolstp;
   maxn   = (SpiceInt   ) J_maxn;
   ncuts  = (SpiceInt   ) J_ncuts;
   schstp = (SpiceDouble) J_schstp;
   soltol = (SpiceDouble) J_soltol;

   /*
   Fetch the reference vector. 
   */
   getVec3D_jni ( env, J_refvec, refvec );
   JNI_EXC( env );


   /*
   Fetch the input strings. 
   */
   getVarInputString_jni   ( env, J_method, &methodLen, &method );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_target, BDNMLN,     target  );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_fixref, FRNMLN,     fixref  );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr  );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_corloc, LOCLEN,     corloc  );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr  );
   JNI_EXC( env );

 
   /*
   Let `needed' be the item count we'll use for dynamic memory allocation. 
   To avoid unpleasantness related to malloc and free, we'll make sure that 
   `needed' is strictly positive.
   */
   needed = maxi_c( 2, maxn, 1 );

    /*
   Allocate a dynamic array to hold the output point counts for each cut. 
   */
   npts = ( SpiceInt *)alloc_SpiceMemory( needed * sizeof(SpiceInt) );

   SPICE_EXC( env, caller);

   /*
   Allocate a dynamic array to hold the output limb points. 
   */
   points = ( SpiceDouble(*)[3] )alloc_SpiceDouble_C_array( needed, 3 );

   SPICE_EXC( env, caller);

   /*
   Allocate a dynamic array to hold the output epochs. 
   */
   epochs = ( SpiceDouble * )alloc_SpiceDouble_C_array( needed, 1 );

   /*
   Allocate a dynamic array to hold the output tangent points. 
   */
   tangts = ( SpiceDouble(*)[3] )alloc_SpiceDouble_C_array( needed, 3 );

   SPICE_EXC( env, caller);


   /*
   Find the limb points. 
   */
   limbpt_c ( method, target, et,     fixref, abcorr, corloc,
              obsrvr, refvec, rolstp, ncuts,  schstp, soltol,
              maxn,   npts,   points, epochs, tangts          );   

   /*
   Regardless of whether the call succeeded, free the dynamic
   memory used to hold inputs. 

   Free the method string. 
   */
   freeVarInputString_jni ( env, J_method, method );


   /*
   Update the output arrays. 
   */
   if ( !failed_c() )
   {
      /*
      Compute the total number of points found. 
      */
      totpts = 0;

      for ( i = 0;  i < ncuts;  i++ )
      {
         totpts += npts[i];
      }

      /*
      The `npts' array is a simple jintArray. 
      */
      updateVecGI_jni ( env, ncuts, npts, J_npts );
      JNI_EXC( env );

      /*
      The `points' array is a jobjectArray which has jdoubleArray
      elements.  
      */
      updateMatGD_jni ( env, totpts, 3, points, J_points );
      JNI_EXC( env );

      /*
      The `epochs' array is a simple jdoubleArray. 
      */
      updateVecGD_jni ( env, totpts,    epochs, J_epochs );
      JNI_EXC( env );

      /*
      The `tangts' array is a jobjectArray which has jdoubleArray
      elements.  
      */
      updateMatGD_jni ( env, totpts, 3, tangts, J_tangts );
      JNI_EXC( env );
   }
 
   /*
   Free the dynamic memory used to store the values returned
   from limbpt_c. 
   */
   free_SpiceMemory( (void *)  npts   );
   free_SpiceMemory( (void *)  points );
   free_SpiceMemory( (void *)  epochs );
   free_SpiceMemory( (void *)  tangts );

   /*
   Handle any SPICE errors. 
   */
   SPICE_EXC( env, caller );

   return;
}




 
/* 
Wrapper for CSPICE function lmpool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_lmpool
  (JNIEnv * env, 
   jclass             J_class, 
   jobjectArray       J_cvals  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.lmpool";
   SpiceInt                fStrLen;
   SpiceInt                nStr;

   void                  * fStrArray;
   

   /*
   Grab the input Java string array in a dynamically allocated
   Fortran-style array. 
   */
   getFortranStringArray_jni ( env,    J_cvals, 
                               &nStr,  &fStrLen,  &fStrArray );

   /*
   Exit here if an exception or a SPICE error occurred. 
   */
   JNI_EXC   ( env );
   SPICE_EXC ( env, caller );

   /*
   Load the kernel variable assignments from the  
   string array into the kernel pool. We use
   the f2c'd interface to avoid redundant dynamic allocation.
   */ 
   lmpool_ (  ( char       * ) fStrArray,
              ( integer    * ) &nStr,
              ( ftnlen       ) fStrLen     );

   /*
   Regardless of the outcome of the insertion,  
   free the Fortran string array.
   */
   free ( fStrArray );             
}



/* 
Wrapper for CSPICE function ltime_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_ltime
(  JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_etobs, 
   jint               J_obs,
   jstring            J_dir,
   jint               J_targ,
   jdoubleArray       J_ettarg, 
   jdoubleArray       J_elapsd )
{
   /*
   Local variables 
   */
   #define DIRLEN          11

   static SpiceChar        dir    [ DIRLEN ];
   static SpiceChar      * caller   = "CSPICE.ltime";

   SpiceDouble             elapsd;
   SpiceDouble             etobs;
   SpiceDouble             ettarg;

   SpiceInt                obs;
   SpiceInt                targ;
 

   /*
   Get a local copy of the input string.

   Note that the string is presumed to be left-justified.
   */
   getFixedInputString_jni ( env, J_dir, DIRLEN, dir );

   /*
   Check for exceptions and SPICE errors; return if any are 
   found. 
   */
   JNI_EXC  ( env );

   obs   = (SpiceInt   ) J_obs;
   etobs = (SpiceDouble) J_etobs;
   targ  = (SpiceInt   ) J_targ;
   
 
   ltime_c ( etobs, obs, dir, targ, &ettarg, &elapsd );

 
   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Transfer the target epoch and elapsed time to the corresponding 
   output Java arrays. Note that these scalars are stored in
   arrays of length 1 so that they may be treated as output arguments.
   */
   updateVecGD_jni ( env, 1, &ettarg, J_ettarg );
   updateVecGD_jni ( env, 1, &elapsd, J_elapsd );

   return;
}



/* 
Wrapper for CSPICE function m2eul_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_m2eul
  (JNIEnv           * env, 
   jclass             J_class, 
   jobjectArray       J_r, 
   jintArray          J_axes   )
{
   /*
   Local variables  
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.m2eul";

   SpiceDouble             angles [3];
   SpiceDouble             r      [3][3];
   SpiceInt                axes   [3];


   /*
   Get the input matrix in a C array. 
   */
   getMat33D_jni ( env, J_r, r );

   JNI_EXC_VAL ( env, retArray );

   /*
   Get the input axis sequence in an array as well. 
   */
   getVec3I_jni ( env, J_axes, axes );

   JNI_EXC_VAL ( env, retArray );

   /*
   Make the CSPICE call. 
   */
   m2eul_c ( r,  axes[0],   axes[1],   axes[2], 
                 angles,    angles+1,  angles+2   );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createVec3D_jni ( env, angles, &retArray );


   return ( retArray );
}



/* 
Wrapper for CSPICE function m2q_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_m2q
  (JNIEnv           * env, 
   jclass             J_class, 
   jobjectArray       J_m      )
{
   /*
   Local variables  
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.m2q";

   SpiceDouble             m [3][3];
   SpiceDouble             q [4];

   /*
   Get the input matrix in a C array. 
   */
   getMat33D_jni ( env, J_m, m );

   JNI_EXC_VAL ( env, retArray );


   /*
   Make the CSPICE call. 
   */
   m2q_c ( m, q );

   SPICE_EXC_VAL( env, caller, retArray );


   /*
   Create the output array. 
   */
   createVecGD_jni ( env, 4, q, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function mxm_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_mxm
  (JNIEnv           * env, 
   jclass             J_class, 
   jobjectArray       J_m1,
   jobjectArray       J_m2     )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.mxm";

   SpiceDouble             m1   [3][3];
   SpiceDouble             m2   [3][3];
   SpiceDouble             mout [3][3];

   /*
   Get the input matrices in C arrays. 
   */
   getMat33D_jni ( env, J_m1, m1 );
   getMat33D_jni ( env, J_m2, m2 );

   JNI_EXC_VAL ( env, retArray );

   /*
   Make the CSPICE call. 
   */
   mxm_c ( m1, m2, mout );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( mout ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function mxv_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_mxv
  (JNIEnv           * env, 
   jclass             J_class, 
   jobjectArray       J_m,
   jdoubleArray       J_v     )
{
   /*
   Local variables  
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.mxv";

   SpiceDouble             m    [3][3];
   SpiceDouble             v    [3];
   SpiceDouble             vout [3];

   /*
   Get the input matrix and vector in C arrays. 
   */
   getMat33D_jni ( env, J_m, m );
   getVec3D_jni  ( env, J_v, v );

   JNI_EXC_VAL ( env, retArray );

   /*
   Make the CSPICE call. 
   */
   mxv_c ( m, v, vout );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createVec3D_jni ( env,  CONST_VEC( vout ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function namfrm_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_namfrm
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_name ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.namfrm";

   static SpiceChar        name  [ BDNMLN ];

   SpiceInt                code = 0;

   /*
   Capture the body name in a local buffer, then
   release the dynamically allocated version of the name.
   */
   getFixedInputString_jni ( env, J_name, BDNMLN, name );
   
   JNI_EXC_VAL( env, ((jint)code) );


   namfrm_c ( name, &code );

    
   SPICE_EXC_VAL( env, caller, ((jint)code) );
   

   return ( (jint) code );
}



/* 
Wrapper for CSPICE function nearpt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_nearpt
(  JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_positn, 
   jdouble            J_a,
   jdouble            J_b,
   jdouble            J_c,
   jdoubleArray       J_npoint, 
   jdoubleArray       J_alt        )
{
   /*
   Local variables 
   */
   SpiceChar             * caller = "CSPICE.nearpt";

   SpiceDouble             alt;
   SpiceDouble             npoint [3];
   SpiceDouble             positn [3];


   /*
   Get the input position vector. 
   */
   getVec3D_jni ( env, J_positn, positn );

   JNI_EXC( env );


   nearpt_c ( positn, 
              (SpiceDouble)J_a, 
              (SpiceDouble)J_b, 
              (SpiceDouble)J_c, 
              npoint, 
              &alt              ); 

   SPICE_EXC( env, caller );

   /*
   Assign values to the output arguments by updating
   their elements. Note that J_alt is an array. 
   */
   updateVec3D_jni ( env,    npoint, J_npoint );
   updateVecGD_jni ( env, 1, &alt,   J_alt    );
}



/* 
Wrapper for CSPICE function npedln_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_npedln
  (JNIEnv * env, 
   jclass             J_class, 
   jdouble            J_a,
   jdouble            J_b,
   jdouble            J_c,
   jdoubleArray       J_linpt,
   jdoubleArray       J_lindir,  
   jdoubleArray       J_pnear,
   jdoubleArray       J_dist   )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.npedln";

   SpiceDouble             dist;
   SpiceDouble             linptArray  [3];
   SpiceDouble             lindirArray [3];
   SpiceDouble             pnear       [3];

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni( env, J_linpt,  linptArray  );
   getVec3D_jni( env, J_lindir, lindirArray );

   JNI_EXC( env );


   npedln_c(  (SpiceDouble) J_a,
              (SpiceDouble) J_b,
              (SpiceDouble) J_c,
              linptArray, 
              lindirArray, 
              pnear, 
              &dist              );


   SPICE_EXC( env, caller );

   /*
   Set output arguments. 
   */
   updateVec3D_jni( env,    pnear, J_pnear );   
   updateVecGD_jni( env, 1, &dist, J_dist  );  
}



/* 
Wrapper for CSPICE function npelpt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_npelpt
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_point,
   jdoubleArray       J_ellipse,
   jdoubleArray       J_pnear,
   jdoubleArray       J_dist    )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.npelpt";

   SpiceDouble             ellipseArray [ELMAX];
   SpiceDouble             point        [3];
   SpiceDouble             pnear        [3];
   SpiceDouble             dist;

   SpiceEllipse            ellipse;

 
   /*
   Get the input vectors in one-dimensional C arrays. 
   */
   getVec3D_jni     ( env, J_point,          (SpiceDouble *)point        );
   getVecFixedD_jni ( env, J_ellipse, ELMAX, (SpiceDouble *)ellipseArray );

   JNI_EXC( env );

   /*
   Create a SPICE ellipse from the input ellipse array.
   */
   cgv2el_c ( ellipseArray, ellipseArray+3, ellipseArray+6, &ellipse );

   /*
   Compute the near point.
   */
   npelpt_c ( point, &ellipse, pnear, &dist );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Update the output arrays.
   */
   updateVec3D_jni ( env,    pnear, J_pnear );
   updateVecGD_jni ( env, 1, &dist, J_dist  );
}



/* 
Wrapper for CSPICE function nplnpt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_nplnpt
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_linpt,
   jdoubleArray       J_lindir,  
   jdoubleArray       J_point,  
   jdoubleArray       J_pnear,
   jdoubleArray       J_dist   )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.nplnpt";

   SpiceDouble             dist;
   SpiceDouble             linptArray  [3];
   SpiceDouble             lindirArray [3];
   SpiceDouble             pointArray  [3];
   SpiceDouble             pnear       [3];

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni( env, J_linpt,  linptArray  );
   getVec3D_jni( env, J_lindir, lindirArray );
   getVec3D_jni( env, J_point,  pointArray  );

   JNI_EXC( env );


   nplnpt_c( linptArray, lindirArray, pointArray, pnear, &dist );


   SPICE_EXC ( env, caller );

   /*
   Set output arguments. 
   */
   updateVecGD_jni( env, 1, &dist, J_dist  );  
   updateVec3D_jni( env,    pnear, J_pnear );   
}



/* 
Wrapper for CSPICE function nvc2pl_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_nvc2pl
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_normal,
   jdouble            J_constant )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.nvc2pl";

   SpiceDouble             normal     [3];
   SpiceDouble             planeArray [PLMAX];

   SpicePlane              plane;

 
   /*
   Get the input vector in a one-dimensional C array. 
   */
   getVec3D_jni ( env, J_normal, (SpiceDouble *)normal);

   JNI_EXC_VAL( env, retArray );


   nvc2pl_c ( normal, (SpiceDouble)J_constant, &plane );


   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );


   MOVED( plane.normal, 3, planeArray );
   planeArray[3] = plane.constant;

   /*
   Normal return. 
   */
   createVecGD_jni ( env, PLMAX, planeArray, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function nvp2pl_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_nvp2pl
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_normal,
   jdoubleArray       J_point )
{

   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.nvc2pl";

   SpiceDouble             normal     [3];
   SpiceDouble             planeArray [PLMAX];
   SpiceDouble             point      [3];

   SpicePlane              plane;

 
   /*
   Get the input vectors in one-dimensional C arrays. 
   */
   getVec3D_jni ( env, J_normal, (SpiceDouble *)normal);
   getVec3D_jni ( env, J_point,  (SpiceDouble *)point );

   JNI_EXC_VAL( env, retArray );


   nvp2pl_c ( normal, point, &plane );


   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );


   MOVED( plane.normal, 3, planeArray );
   planeArray[3] = plane.constant;

   /*
   Normal return. 
   */
   createVecGD_jni ( env, PLMAX, planeArray, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function occult_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_occult
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_targ1,
   jstring            J_shape1,
   jstring            J_frame1,
   jstring            J_targ2,
   jstring            J_shape2,
   jstring            J_frame2,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jdouble            J_et      )

{

   /*
   Local constants 
   */

   /*
   Local variables 
   */
   jint                    retVal = 0;

   static SpiceChar      * caller = "CSPICE.occult";
   
   SpiceChar               abcorr [ CORRLN ];
   SpiceChar               frame1 [ FRNMLN ];
   SpiceChar               frame2 [ FRNMLN ];
   SpiceChar               obsrvr [ BDNMLN ];
   SpiceChar               shape1 [ BSHPLN ];
   SpiceChar               shape2 [ BSHPLN ];
   SpiceChar               targ1  [ BDNMLN ];
   SpiceChar               targ2  [ BDNMLN ];

   SpiceDouble             et;

   SpiceInt                code;


   /*
   Get a local copy of the sole input scalar. 
   */
   et = (SpiceDouble) J_et;

   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_targ1,  BDNMLN, targ1  );
   JNI_EXC_VAL( env, retVal );

   getFixedInputString_jni ( env, J_shape1, BSHPLN, shape1 );
   JNI_EXC_VAL( env, retVal );

   getFixedInputString_jni ( env, J_frame1, FRNMLN, frame1  );
   JNI_EXC_VAL( env, retVal );

   getFixedInputString_jni ( env, J_targ2,  BDNMLN, targ2  );
   JNI_EXC_VAL( env, retVal );

   getFixedInputString_jni ( env, J_shape2, BSHPLN, shape2 );
   JNI_EXC_VAL( env, retVal );

   getFixedInputString_jni ( env, J_frame2, FRNMLN, frame2  );
   JNI_EXC_VAL( env, retVal );

   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr  );
   JNI_EXC_VAL( env, retVal );

   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr  );
   JNI_EXC_VAL( env, retVal );
 

   /*
   Get the requested occultation condition code.
   */
   occult_c ( targ1,  shape1, frame1,
              targ2,  shape2, frame2,
              abcorr, obsrvr, et,     &code );

   retVal = (jint)code;

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retVal );

   return ( retVal );
}



/* 
Wrapper for CSPICE function oscelt_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_oscelt
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_state,
   jdouble            J_et,
   jdouble            J_mu    )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.oscelt";

   SpiceDouble             state [6];
   SpiceDouble             elts  [8];


 
   /*
   Get the input matrix in a one-dimensional C array. 
   */
   getVecFixedD_jni ( env, J_state, 6, (SpiceDouble *)state );

   JNI_EXC_VAL( env, retArray );


   oscelt_c ( state, (SpiceDouble)J_et, (SpiceDouble)J_mu, elts );


   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Normal return. 
   */
   createVecGD_jni ( env, 8, elts, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function oscltx_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_oscltx
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_state,
   jdouble            J_et,
   jdouble            J_mu    )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.oscltx";

   SpiceDouble             state [6];
   SpiceDouble             elts  [SPICE_OSCLTX_NELTS];


 
   /*
   Get the input matrix in a one-dimensional C array. 
   */
   getVecFixedD_jni ( env, J_state, 6, (SpiceDouble *)state );

   JNI_EXC_VAL( env, retArray );


   oscltx_c ( state, (SpiceDouble)J_et, (SpiceDouble)J_mu, elts );


   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Normal return. 
   */
   createVecGD_jni ( env, SPICE_OSCLTX_NELTS, elts, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function pckcls_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pckcls
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.pckcls";

   
   pckcls_c ( (SpiceInt)J_handle );

   SPICE_EXC( env, caller );
}




/* 
Wrapper for CSPICE function pckcov_c 

NOTE: the input and returned arrays have no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_pckcov
  (JNIEnv * env, 
   jclass             J_class,
   jstring            J_file,
   jint               J_clssid, 
   jint               J_size,
   jdoubleArray       J_cover  )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;


   /*
   The cell below will be allocated dynamically.
   */
   SpiceCell             * cover;

   static SpiceChar      * caller   = "CSPICE.pckcov";
   static SpiceChar        file  [ FNAMLN ];

   SpiceDouble           * coverData;

   SpiceInt                coverSize;
   SpiceInt                maxSize;


   /*
   Get the size of the input window data array. 
   */
   coverSize = (*env)->GetArrayLength ( env, J_cover );
   
   JNI_EXC_VAL  ( env, retArray );


   /*
   Capture the input PCK name. 
   */
   getFixedInputString_jni ( env, J_file, FNAMLN, file );
   JNI_EXC_VAL  ( env, retArray );

 
   if ( coverSize > 0 )
   {
      /*
      Capture the contents of the input array `cover' in dynamic
      memory. Check out and return if an exception is thrown.
      */
      getVecGD_jni ( env, J_cover, &coverSize, &coverData );
      JNI_EXC_VAL  ( env, retArray );
   }
   else
   {
      coverData = 0;
   }

   
   /*
   If the specified output cell size is smaller than the input
   array size, we have a problem. 
   */
   maxSize = (SpiceInt)J_size;

   if ( maxSize < coverSize )
   {
      /*
      We must free the data from the input array before
      returning. 
      */
      if ( coverSize > 0 )
      {
         freeVecGD_jni ( env, J_cover, coverData );
      }

      setmsg_c ( "Input cell size is #; output size is #;" );
      errint_c ( "#",  coverSize                           );
      errint_c ( "#",  maxSize                             );
      sigerr_c ( "SPICE(OUTPUTCELLTOOSMALL)"               );

      SPICE_EXC_VAL(env, caller, retArray );
   }

   /*
   Create a dynamically allocated cell of size maxSize to hold
   the results of our spkcov_c call. Initialize the cell with
   the data from the input array, if any.
   */
   cover = zzalcell_c ( SPICE_DP, maxSize, coverSize, 0, coverData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   At this point, we're holding on to a dynamically allocated
   cell. We can't return before freeing this cell, so we must
   be careful about how we handle errors.

   However, we're now done with the coverData array.
   */
   if ( coverSize > 0 )
   {
      freeVecGD_jni ( env, J_cover, coverData );   
   }
   

   /*
   Make `cover' into a SPICE window. 
   */
   wnvald_c ( maxSize, coverSize, cover );

   
   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   We're finally ready for our CSPICE call. 
   */
   pckcov_c ( file, (SpiceInt)J_clssid, cover );


   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   At this point, the data portion of `cover' is exactly
   what we want to return. 
   */
   createVecGD_jni ( env, 
                     card_c(cover), 
                     (SpiceDouble *)cover->data, 
                     &retArray                   );

   /*
   De-allocate the dynamic cell before departure.
   */
   zzdacell_c ( cover );

   /*
   Handle any JNI or SPICE error. 
   */
   JNI_EXC_VAL  ( env,         retArray );
   SPICE_EXC_VAL( env, caller, retArray );

   return ( retArray );
}




/* 
Wrapper for CSPICE function pckfrm_c 

NOTE: the returned array has no control area.
*/
JNIEXPORT jintArray JNICALL Java_spice_basic_CSPICE_pckfrm
  (JNIEnv * env, 
   jclass             J_class,
   jstring            J_file,
   jint               J_size,
   jintArray          J_ids  )
{ 
   /*
   Local variables 
   */
   jintArray            retArray = 0;


   /*
   The cells below will be dynamically allocated.
   */
   SpiceCell             * ids;

   static SpiceChar      * caller   = "CSPICE.pckfrm";
   static SpiceChar        file  [ FNAMLN ];

   SpiceInt                idSetSize;
   SpiceInt                maxSize;
   SpiceInt              * idSetData;


   /*
   Get the size of the input set data array. 
   */
   idSetSize = (*env)->GetArrayLength ( env, J_ids );
   
   JNI_EXC_VAL  ( env, retArray );


   /*
   Capture the input PCK name. 
   */
   getFixedInputString_jni ( env, J_file, FNAMLN, file );
   JNI_EXC_VAL  ( env, retArray );

 
   if ( idSetSize > 0 )
   {
      /*
      Capture the contents of the input array `cover' in dynamic
      memory.  Check out and return if an exception is thrown.
      */
      getVecGI_jni ( env, J_ids, &idSetSize, &idSetData );
      JNI_EXC_VAL  ( env, retArray );
   }
   else
   {
      idSetData = 0;
   }

   
   /*
   If the specified output cell size is smaller than the input
   array size, we have a problem. 
   */
   maxSize = (SpiceInt)J_size;

   if ( maxSize < idSetSize )
   {
      /*
      We must free the data from the input array before
      returning. 
      */
      if ( idSetSize > 0 )
      {
         freeVecGI_jni ( env, J_ids, idSetData );
      }

      setmsg_c ( "Input cell size is #; output size is #;" );
      errint_c ( "#",  idSetSize                           );
      errint_c ( "#",  maxSize                             );
      sigerr_c ( "SPICE(OUTPUTCELLTOOSMALL)"               );

      SPICE_EXC_VAL(env, caller, retArray );
   }

   /*
   Create a dynamically allocated cell of size maxSize to hold
   the results of our pckfrm_c call. Initialize the cell with
   the data from the input array, if any.
   */
   ids = zzalcell_c ( SPICE_INT, maxSize, idSetSize, 0, idSetData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   At this point, we're holding on to a dynamically allocated
   cell. We can't return before freeing this cell, so we must
   be careful about how we handle errors.

   However, we're now done with the idSetData array.
   */
   if ( idSetSize > 0 )
   {
      freeVecGI_jni ( env, J_ids, idSetData );   
   }
   

   /*
   Make the input cell into a set before passing it to pckfrm_c. 
   */
   valid_c ( maxSize, idSetSize, ids );

   if ( failed_c() )
   {
      /*
      Free the ID set before departure. 
      */
      zzdacell_c ( ids );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   We're finally ready for our CSPICE call. 
   */
   pckfrm_c ( file, ids );


   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( ids );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   At this point, the data portion of `ids' is exactly
   what we want to return. 
   */
   createVecGI_jni ( env, 
                     card_c(ids), 
                     (SpiceInt *)ids->data, 
                     &retArray                   );

   /*
   De-allocate the dynamic cell before departure.
   */
   zzdacell_c ( ids );

   
   /*
   Handle any JNI or SPICE error. 
   */
   JNI_EXC_VAL  ( env,         retArray );
   SPICE_EXC_VAL( env, caller, retArray );


   return ( retArray );
}



/* 
Wrapper for CSPICE function pckopn_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_pckopn
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_fname,
   jstring            J_ifname,   
   jint               J_ncomch )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.pckopn";
   static SpiceChar        fname  [ FNAMLN ];
   static SpiceChar        ifname [ IFNLEN ];

   SpiceInt                handle = 0;


   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_fname,  FNAMLN, fname  );
   getFixedInputString_jni ( env, J_ifname, IFNLEN, ifname );
   
   JNI_EXC_VAL( env, ((jint) handle) );


   pckopn_c ( fname, ifname, (SpiceInt)J_ncomch, &handle );


   SPICE_EXC_VAL( env, caller, ((jint)handle) );

   /*
   Normal return. 
   */
   return ( (jint)handle );
}




/* 
Wrapper for CSPICE function pckuof_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pckuof
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle )
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.pckuof";

 
   pckuof_c ( (SpiceInt) J_handle );
  
   /*
   If the call resulted in a SPICE error, throw an exception. 
   */
   SPICE_EXC ( env, caller );
}




/* 
Wrapper for CSPICE function pckw02_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pckw02
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_clssid,
   jstring            J_frame,
   jdouble            J_first,
   jdouble            J_last,
   jstring            J_segid,
   jdouble            J_intlen,
   jint               J_n,
   jint               J_polydg,
   jdoubleArray       J_cdata,
   jdouble            J_btime   )
{
   /*
   Local constants
   */                                        

   /*
   Local variables and initializations
   */
   static SpiceChar      * caller    = "CSPICE.pckw02";

   static SpiceChar        frame  [ FRNMLN ];
   static SpiceChar        segid  [ SIDLEN ];

   SpiceDouble           * cdata;

   SpiceInt                nElts;

   /*
   Fetch the frame name and segment ID.
   */
   getFixedInputString_jni ( env, J_frame, FRNMLN, frame );
   getFixedInputString_jni ( env, J_segid, SIDLEN, segid );

   JNI_EXC( env );

   /*
   Fetch the coefficient array into a one-dimensional C array.
   Both Java and C use row-major order, so the coefficients
   are correctly ordered.    
   */
   getVecGD_jni ( env, J_cdata, &nElts, &cdata );
   JNI_EXC( env );


   pckw02_c ( (SpiceInt)      J_handle,
              (SpiceInt)      J_clssid,
              frame,
              (SpiceDouble)   J_first,
              (SpiceDouble)   J_last,
              segid,
              (SpiceDouble)   J_intlen,
              (SpiceInt)      J_n,
              (SpiceInt)      J_polydg,
              (SpiceDouble *) cdata,
              (SpiceDouble)   J_btime );

   /*
   Always free the dynamically allocated coefficient array. 
   */
   freeVecGD_jni ( env, J_cdata, cdata );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );
}



 
/* 
Wrapper for CSPICE function pcpool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pcpool
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jobjectArray       J_cvals  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.pcpool";
   static SpiceChar        kvname   [ KVNMLN ];

   SpiceInt                fStrLen;
   SpiceInt                nStr;

   void                  * fStrArray;


   /*
   First step: get the kernel variable's name. 
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvname );
   
   JNI_EXC ( env );
   

   /*
   Grab the input Java string array in a dynamically allocated
   Fortran-style array. 
   */
   getFortranStringArray_jni ( env,    J_cvals, 
                               &nStr,  &fStrLen,  &fStrArray );

   /*
   Exit here if an exception or a SPICE error occurred. 
   */
   JNI_EXC   ( env );
   SPICE_EXC ( env, caller );

 
   /*
   Insert the string array into the kernel pool. We use
   the f2c'd interface to avoid redundant dynamic allocation.
   */ 
   pcpool_ (  ( char       * ) kvname,
              ( integer    * ) &nStr,
              ( char       * ) fStrArray,
              ( ftnlen       ) strlen(kvname),
              ( ftnlen       ) fStrLen         );

   /*
   Regardless of the outcome of the insertion,  
   free the Fortran string array.
   */
   free ( fStrArray );             
}



/* 
Wrapper for CSPICE function pdpool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pdpool
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jdoubleArray       J_dvals  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.pdpool";
   static SpiceChar        kvname [ KVNMLN ];

   SpiceInt                nVals;
 
   SpiceDouble           * dpdata;


   /*
   First step: get the kernel variable's name. 
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvname );
   
   JNI_EXC ( env );
   
   /*
   Grab the input data array in a dynamically allocated
   C-style array. 
   */
   getVecGD_jni ( env, J_dvals, &nVals, &dpdata );

   /*
   Exit here if an exception or a SPICE error occurred. 
   */
   JNI_EXC   ( env );
   SPICE_EXC ( env, caller );

   /*
   Insert the d.p. array into the kernel pool.
   */ 
   pdpool_c (  kvname, nVals, dpdata );


   /*
   Regardless of the outcome of the insertion,  
   free the dynamically allocated d.p. array.
   */
   freeVecGD_jni ( env, J_dvals, dpdata );             
}



/* 
Wrapper for CSPICE function pgrrec_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_pgrrec
  (JNIEnv * env,
   jclass             J_class,
   jstring            J_body,
   jdouble            J_longitude,
   jdouble            J_latitude,
   jdouble            J_altitude,
   jdouble            J_re,
   jdouble            J_f           )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar        body  [ BDNMLN ];
   static SpiceChar      * caller   = "CSPICE.pgrrec";
   SpiceDouble             result[3];


   /*
   Fetch the input string. 
   */
   getFixedInputString_jni ( env, J_body, BDNMLN, body );

   JNI_EXC_VAL( env, retArray );


   pgrrec_c ( body,
              (SpiceDouble) J_longitude, 
              (SpiceDouble) J_latitude, 
              (SpiceDouble) J_altitude, 
              (SpiceDouble) J_re, 
              (SpiceDouble) J_f, 
              result                   );


   SPICE_EXC_VAL ( env, caller, retArray );

   /*
   Create a new Java array of jdoubles to hold the result. 
   */
   createVec3D_jni ( env, result, &retArray );


   return retArray;
}



/* 
Wrapper for CSPICE function pi_c
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_pi
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)pi_c()  );
}



/* 
Wrapper for CSPICE function pipool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pipool
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jintArray          J_ivals  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.pipool";
   static SpiceChar        kvname [ KVNMLN ];

   SpiceInt                nVals;
 
   SpiceInt              * idata;


   /*
   First step: get the kernel variable's name. 
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvname );
   
   JNI_EXC ( env );
   
   /*
   Grab the input Java int array in a dynamically allocated
   C-style array. 
   */
   getVecGI_jni ( env, J_ivals, &nVals, &idata );

   /*
   Exit here if an exception or a SPICE error occurred. 
   */
   JNI_EXC   ( env );
   SPICE_EXC ( env, caller );


   /*
   Insert the integer array into the kernel pool.
   */ 
   pipool_c (  kvname, nVals, idata );


   /*
   Regardless of the outcome of the insertion,  
   free the dynamically allocated SpiceInt array.
   */
   freeVecGI_jni ( env, J_ivals, idata );             
}



/* 
Wrapper for CSPICE function pjelpl_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_pjelpl
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_ellipse,
   jdoubleArray       J_plane    )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.pjelpl";

   SpiceDouble             ellipseArray [ELMAX];
   SpiceDouble             planeArray   [PLMAX];
 
   SpiceEllipse            inEllipse;
   SpiceEllipse            pjEllipse;
   SpicePlane              plane;

   /*
   Get the input arrays in C arrays.
   */
   getVecFixedD_jni ( env, J_ellipse, ELMAX, ellipseArray );
   getVecFixedD_jni ( env, J_plane,   PLMAX, planeArray   );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a CSPICE ellipse and plane from the input arrays.
   */
   cgv2el_c ( ellipseArray, 
              ellipseArray+3, 
              ellipseArray+6, 
              &inEllipse      );

   nvc2pl_c ( planeArray, planeArray[3], &plane );

   pjelpl_c ( &inEllipse, &plane, &pjEllipse );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Transfer the components of the projected
   ellipse to a 1-d array.
   */
   MOVED( pjEllipse.center,     3,  ellipseArray   );
   MOVED( pjEllipse.semiMajor,  3,  ellipseArray+3 );
   MOVED( pjEllipse.semiMinor,  3,  ellipseArray+6 );

   /*
   Normal return. 
   */
   createVecGD_jni ( env, ELMAX, ellipseArray, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function pl2nvc_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pl2nvc
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_plane,
   jdoubleArray       J_normal,
   jdoubleArray       J_constant  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.pl2nvp";

   SpiceDouble             planeArray [PLMAX];
   SpiceDouble             normal     [3];
   SpiceDouble             constant;

   SpicePlane              plane;

   /*
   Get the input vector in a one-dimensional C array. 
   */
   getVecFixedD_jni ( env, J_plane, PLMAX, (SpiceDouble *)planeArray );

   JNI_EXC( env );

   /*
   Create a SPICE plane using the normal vector and constant
   from the input array.
   */
   nvc2pl_c ( planeArray, planeArray[3], &plane );

   pl2nvc_c ( &plane, normal, &constant );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Update the output arrays.
   */
   updateVecGD_jni ( env, 3, normal,    J_normal   );
   updateVecGD_jni ( env, 1, &constant, J_constant );
}



/* 
Wrapper for CSPICE function pl2nvp_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pl2nvp
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_plane,
   jdoubleArray       J_normal,
   jdoubleArray       J_point  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.pl2nvp";

   SpiceDouble             planeArray [PLMAX];
   SpiceDouble             normal     [3];
   SpiceDouble             point      [3];

   SpicePlane              plane;

   /*
   Get the input vector in a one-dimensional C array. 
   */
   getVecFixedD_jni ( env, J_plane, PLMAX, (SpiceDouble *)planeArray );

   JNI_EXC( env );

   /*
   Create a SPICE plane using the normal vector and constant
   from the input array.
   */
   nvc2pl_c ( planeArray, planeArray[3], &plane );

   pl2nvp_c ( &plane, normal, point );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Update the output arrays.
   */
   updateVecGD_jni ( env, 3, normal, J_normal );
   updateVecGD_jni ( env, 3, point,  J_point  );
}



/* 
Wrapper for CSPICE function pl2psv_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pl2psv
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_plane,
   jdoubleArray       J_point,
   jdoubleArray       J_span1,
   jdoubleArray       J_span2 )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.pl2psv";

   SpiceDouble             planeArray [PLMAX];
   SpiceDouble             point      [3];
   SpiceDouble             span1      [3];
   SpiceDouble             span2      [3];

   SpicePlane              plane;

 
   /*
   Get the input vector in a one-dimensional C array. 
   */
   getVecFixedD_jni ( env, J_plane, PLMAX, (SpiceDouble *)planeArray );

   JNI_EXC( env );

   /*
   Create a SPICE plane using the normal vector and constant
   from the input array.
   */
   nvc2pl_c ( planeArray, planeArray[3], &plane );

   pl2psv_c ( &plane, point, span1, span2 );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Update the output arrays.
   */
   updateVecGD_jni ( env, 3, point, J_point );
   updateVecGD_jni ( env, 3, span1, J_span1 );
   updateVecGD_jni ( env, 3, span2, J_span2 );
}





/* 
Wrapper for CSPICE function pltar_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_pltar
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_nv,
   jdoubleArray       J_vrtces,
   jint               J_np,
   jintArray          J_plates   )
{
   /*
   Local variables 
   */
   jdouble                 retVal = 0;

   static SpiceChar      * caller = "CSPICE.pltar";

   SpiceDouble         ( * vrtces)[3];
 
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt            ( * plates)[3];
   SpiceInt                pltLen;
   SpiceInt                vrtLen;


   /*
   Get the input vertices in a local dynamic C array. 
   */
   getVecGD_jni ( env, J_vrtces, &vrtLen, (SpiceDouble **) &vrtces );

   JNI_EXC_VAL( env, retVal );

   /*
   Get the input plates in a local dynamic C array. 
   */
   getVecGI_jni ( env, J_plates, &pltLen, (SpiceInt **) &plates );

   JNI_EXC_VAL( env, retVal );

   /*
   Get input scalars. 
   */
   np = (SpiceInt) J_np;
   nv = (SpiceInt) J_nv;

 
   retVal = (jdouble) pltar_c ( nv, vrtces, np, plates );

   /*
   Regardless of whether the call succeeded, free the
   dynamic input arrays. 
   */
   freeVecGD_jni ( env, J_vrtces, (SpiceDouble *)vrtces );
   freeVecGI_jni ( env, J_plates, (SpiceInt    *)plates );

   JNI_EXC_VAL( env, retVal );


   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retVal );


   return ( retVal );
}






/* 
Wrapper for CSPICE function pltexp_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_pltexp
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_iverts,
   jdouble            J_delta   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.pltexp";

   SpiceDouble             delta;
   SpiceDouble             iverts [3][3];
   SpiceDouble             overts [3][3];
 
   /*
   Get the input vertices in a local C array. 
   */
   getMat33D_jni ( env, J_iverts, iverts );

   JNI_EXC_VAL( env, retArray );

   /*
   Get input scalar. 
   */
   delta = (SpiceDouble) J_delta;

   pltexp_c ( iverts, delta, overts );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( overts ),  &retArray );

   JNI_EXC_VAL( env, retArray );

   return ( retArray );
}




/* 
Wrapper for CSPICE function pltnp_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_pltnp
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_point,
   jdoubleArray       J_v1,
   jdoubleArray       J_v2,
   jdoubleArray       J_v3,
   jdoubleArray       J_pnear,
   jdoubleArray       J_dist    )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.pltnp";

   SpiceDouble             dist ;
   SpiceDouble             pnear  [3];
   SpiceDouble             point  [3];
   SpiceDouble             v1     [3];
   SpiceDouble             v2     [3];
   SpiceDouble             v3     [3];
 
   /*
   Get the input vectors in local C arrays. 
   */
   getVec3D_jni ( env, J_point, (SpiceDouble *)point );
   getVec3D_jni ( env, J_v1,    (SpiceDouble *)v1 );
   getVec3D_jni ( env, J_v2,    (SpiceDouble *)v2 );
   getVec3D_jni ( env, J_v3,    (SpiceDouble *)v3 );

   JNI_EXC( env );
 
   pltnp_c ( point, v1, v2, v3, pnear, &dist );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Update the output arrays.
   */
   updateVec3D_jni ( env,    pnear, J_pnear );
   updateVecGD_jni ( env, 1, &dist, J_dist  );
}




/* 
Wrapper for CSPICE function pltnrm_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_pltnrm
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v1,
   jdoubleArray       J_v2,
   jdoubleArray       J_v3    )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.pltnrm";

   SpiceDouble             normal [3];
   SpiceDouble             v1     [3];
   SpiceDouble             v2     [3];
   SpiceDouble             v3     [3];
 
   /*
   Get the input vectors in local C arrays. 
   */
   getVec3D_jni ( env, J_v1, (SpiceDouble *)v1 );
   getVec3D_jni ( env, J_v2, (SpiceDouble *)v2 );
   getVec3D_jni ( env, J_v3, (SpiceDouble *)v3 );

   JNI_EXC_VAL( env, retArray );
 
   pltnrm_c ( v1, v2, v3, normal );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array.
   */
   createVec3D_jni ( env, normal, &retArray );

   return ( retArray );
}





/* 
Wrapper for CSPICE function pltvol_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_pltvol
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_nv,
   jdoubleArray       J_vrtces,
   jint               J_np,
   jintArray          J_plates   )
{
   /*
   Local variables 
   */
   jdouble                 retVal = 0;

   static SpiceChar      * caller = "CSPICE.pltvol";

   SpiceDouble         ( * vrtces)[3];
 
   SpiceInt                np;
   SpiceInt                nv;
   SpiceInt            ( * plates)[3];
   SpiceInt                pltLen;
   SpiceInt                vrtLen;


   /*
   Get the input vertices in a local dynamic C array. 
   */
   getVecGD_jni ( env, J_vrtces, &vrtLen, (SpiceDouble **) &vrtces );

   JNI_EXC_VAL( env, retVal );

   /*
   Get the input plates in a local dynamic C array. 
   */
   getVecGI_jni ( env, J_plates, &pltLen, (SpiceInt **) &plates );

   JNI_EXC_VAL( env, retVal );

   /*
   Get input scalars. 
   */
   np = (SpiceInt) J_np;
   nv = (SpiceInt) J_nv;

 
   retVal = (jdouble) pltvol_c ( nv, vrtces, np, plates );

   /*
   Regardless of whether the call succeeded, free the
   dynamic input arrays. 
   */
   freeVecGD_jni ( env, J_vrtces, (SpiceDouble *)vrtces );
   freeVecGI_jni ( env, J_plates, (SpiceInt    *)plates );

   JNI_EXC_VAL( env, retVal );


   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retVal );


   return ( retVal );
}







/* 
Wrapper for CSPICE function prop2b_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_prop2b
  (JNIEnv * env, 
   jclass             J_class, 
   jdouble            J_gm,
   jdoubleArray       J_pvinit,
   jdouble            J_dt,
   jdoubleArray       J_pvprop )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.prop2b";

   SpiceDouble             pvinit [6];
   SpiceDouble             pvprop [6];
 
   /*
   Get the input vector in a one-dimensional C array. 
   */
   getVecFixedD_jni ( env, J_pvinit, 6, (SpiceDouble *)pvinit );

   JNI_EXC( env );

 
   prop2b_c ( (SpiceDouble)J_gm, pvinit, (SpiceDouble)J_dt, pvprop );

   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Update the output array.
   */
   updateVecGD_jni ( env, 6, pvprop, J_pvprop );
}




/* 
Wrapper for CSPICE function psv2pl_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_psv2pl
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_point,
   jdoubleArray       J_span1,
   jdoubleArray       J_span2 )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.psv2pl";

   SpiceDouble             planeArray [PLMAX];
   SpiceDouble             point      [3];
   SpiceDouble             span1      [3];
   SpiceDouble             span2      [3];

   SpicePlane              plane;

 
   /*
   Get the input vectors in one-dimensional C arrays. 
   */
   getVec3D_jni ( env, J_point, (SpiceDouble *)point );
   getVec3D_jni ( env, J_span1, (SpiceDouble *)span1 );
   getVec3D_jni ( env, J_span2, (SpiceDouble *)span2 );

   JNI_EXC_VAL( env, retArray );

   psv2pl_c ( point, span1, span2, &plane );


   /*
   Now handle any SPICE error. 
   */
   SPICE_EXC_VAL( env, caller, retArray );


   MOVED( plane.normal, 3, planeArray );
   planeArray[3] = plane.constant;

   /*
   Normal return. 
   */
   createVecGD_jni ( env, PLMAX  , planeArray, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function putcml_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_putcml
  (JNIEnv * env, 
   jclass             J_class, 
   jobjectArray       J_cvals  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.putcml";
   SpiceChar            ** cPtrArray;
   SpiceChar            ** cPtrArray2;
   static SpiceChar      * program  = "java";

   SpiceInt                fStrLen;
   SpiceInt                i;
   static SpiceInt         nCalls = 0;
   SpiceInt                nStr;

   SpiceStatus             status;

   void                  * fStrArray;
   

   /*
   You can only call this routine once during a program run. 
   */
   if ( nCalls > 0 )
   {
      setmsg_c ( "An attempt was made to call CSPICE.putcml twice. "
                 "This method may be called only once during a "
                 "program run."                                     );
      sigerr_c ( "SPICE(INVALIDCALL)"                               );

      SPICE_EXC( env, caller );
   }

   ++ nCalls;

   

   /*
   Find out how many command line arguments we have. 
   */
   nStr = (*env)->GetArrayLength ( env, J_cvals );

   JNI_EXC( env );


   /*
   Use the fake program name "java."

   If we have no arguments, we still must pass the program name 
   to putcml_c. This is because the program name is not
   present in the command line argument array in Java, but
   it is present in the C array. 
   */
   if ( nStr == 0 )
   {
      cPtrArray = &program;

      putcml_c ( 1, cPtrArray );

      SPICE_EXC( env, caller );

      return;
   }

   /*
   Grab the input Java string array in a dynamically allocated
   Fortran-style array. 
   */
   getFortranStringArray_jni ( env,    J_cvals, 
                               &nStr,  &fStrLen,  &fStrArray );

   /*
   Exit here if an exception or a SPICE error occurred. 
   */
   JNI_EXC   ( env );
   SPICE_EXC ( env, caller );

   /*
   Now we're going to do something expedient but strange: 
   we'll convert our array of Fortran strings to an array
   of pointers to C strings.
   */
   status = F2C_CreateStrArr ( nStr, fStrLen, fStrArray, &cPtrArray );

   /*
   Whether or not an error occurred, free the Fortran string array. 
   */
   free ( fStrArray );

   if ( status == SPICEFAILURE )
   {
      /*
      The C string data structure creation attempt failed. 
      */
      setmsg_c ( "An error occurred while creating an "
                 "array of pointers to C strings. The "
                 "type of error is likely a memory allocation "
                 "failure."                                     );
      sigerr_c ( "SPICE(STRINGMAPERROR)"                        );

      SPICE_EXC( env, caller );
   }

   /*
   The array we have just created doesn't contain the program name.
   Create a new array that does.
   */
   cPtrArray2 = alloc_SpiceString_Pointer_array ( nStr + 1 );

   SPICE_EXC ( env, caller );

   cPtrArray2[0] = program;

   for ( i = 0;  i < nStr;  i++ )
   {
      cPtrArray2[i+1] = cPtrArray[i];
   }

   
   /*
   Now we have the inputs required by putcml_c. 
   */
   putcml_c ( nStr+1, cPtrArray2 );

 
   /*
   Regardless of the outcome of the putcml_c call,  
   free the first C string pointer array. Note that
   we *can't* free the strings referenced by the pointers
   or the second string pointer array, since getcml_c 
   relies on these strings being present. Hence we have
   a fixed-size memory leak.
   */
   free ( cPtrArray  );       


   /*
   If a SPICE error occurred, throw an exception. 
   */      
   SPICE_EXC( env, caller );
}




/* 
Wrapper for CSPICE function pxform_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_pxform
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_from, 
   jstring            J_to, 
   jdouble            J_et     )
{
   /*
   Local variables 
   */
   jobjectArray            jMat = (jobjectArray)0;

   static SpiceChar      * caller   = "CSPICE.pxform";
   static SpiceChar        from  [ FRNMLN ];
   static SpiceChar        to    [ FRNMLN ];

   SpiceDouble             xform [3][3];


   /*
   Capture the input frame name strings in local buffers. 
   Note that the strings are presumed to be left-justified.

   Return if an exception occurs. No deallocation is required.
   */
   getFixedInputString_jni ( env, J_from, FRNMLN, from );
   
   JNI_EXC_VAL ( env, jMat );


   getFixedInputString_jni ( env, J_to, FRNMLN, to );
   
   JNI_EXC_VAL ( env, jMat );


   pxform_c ( from, to, (SpiceDouble)J_et, xform );
 
  
   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, jMat );

   /*
   Create the output matrix.
   */
   createMat33D_jni ( env, CONST_MAT( xform ), &jMat ); 

   return jMat;
}




/* 
Wrapper for CSPICE function pxfrm2_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_pxfrm2
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_from, 
   jstring            J_to, 
   jdouble            J_etfrom,
   jdouble            J_etto     )
{
   /*
   Local variables 
   */
   jobjectArray            jMat = (jobjectArray)0;

   static SpiceChar      * caller   = "CSPICE.pxfrm2";
   static SpiceChar        from  [ FRNMLN ];
   static SpiceChar        to    [ FRNMLN ];

   SpiceDouble             etfrom;
   SpiceDouble             etto;
   SpiceDouble             xform [3][3];


   /*
   Fetch scalar inputs. 
   */
   etfrom = (SpiceDouble) J_etfrom;
   etto   = (SpiceDouble) J_etto;

   /*
   Capture the input frame name strings in local buffers. 
   Note that the strings are presumed to be left-justified.

   Return if an exception occurs. No deallocation is required.
   */
   getFixedInputString_jni ( env, J_from, FRNMLN, from );
   
   JNI_EXC_VAL ( env, jMat );


   getFixedInputString_jni ( env, J_to, FRNMLN, to );
   
   JNI_EXC_VAL ( env, jMat );


   pxfrm2_c ( from, to, etfrom, etto, xform );
 
  
   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, jMat );

   /*
   Create the output matrix.
   */
   createMat33D_jni ( env, CONST_MAT( xform ), &jMat ); 

   return jMat;
}




/* 
Wrapper for CSPICE function q2m_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_q2m
  (JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_q      )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.q2m";

   SpiceDouble             q [4];
   SpiceDouble             m [3][3];


   /*
   Get the input quaternion in a C array. 
   */
   getVecFixedD_jni ( env, J_q, 4, q );

   JNI_EXC_VAL ( env, retArray );


   /*
   Make the CSPICE call. 
   */
   q2m_c ( q, m );

   SPICE_EXC_VAL( env, caller, retArray );


   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( m ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function qdq2av_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_qdq2av
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_q,
   jdoubleArray       J_dq  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             av [3];
   SpiceDouble             dq [4];
   SpiceDouble             q  [4];


   /*
   Fetch the Java vectors. 
   */
   getVecFixedD_jni ( env, J_q,  4, q  );
   getVecFixedD_jni ( env, J_dq, 4, dq );

   JNI_EXC_VAL( env, retArray );

   /*
   qdq2av_c is error-free.
   */
   qdq2av_c( q, dq, av );


   createVec3D_jni ( env, av, &retArray );

   return ( retArray );
}


/* 
Wrapper for CSPICE function qxq_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_qxq
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_q1,
   jdoubleArray       J_q2  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             q1 [4];
   SpiceDouble             q2 [4];
   SpiceDouble             q3 [4];
 

   /*
   Fetch the Java vectors. 
   */
   getVecFixedD_jni ( env, J_q1, 4, q1 );
   getVecFixedD_jni ( env, J_q2, 4, q2 );

   JNI_EXC_VAL( env, retArray );

   /*
   qxq_c is error-free.
   */
   qxq_c( q1, q2, q3 );


   createVecGD_jni ( env, 4, q3, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function radrec_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_radrec
  (JNIEnv * env, 
   jclass             J_class, 
   jdouble            J_range,
   jdouble            J_ra,
   jdouble            J_dec   )
{
 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             result[3];


   radrec_c ( (SpiceDouble) J_range, 
              (SpiceDouble) J_ra, 
              (SpiceDouble) J_dec, 
              result               );

   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function rav2xf_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_rav2xf
  (JNIEnv           * env, 
   jclass             J_class, 
   jobjectArray       J_r, 
   jdoubleArray       J_av    )
{
   /*
   Local variables  
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.rav2xf";

   SpiceDouble             av     [3];
   SpiceDouble             r      [3][3];
   SpiceDouble             xform  [6][6];


   /*
   Get the input rotation matrix.
   */
   getMat33D_jni ( env, J_r, r );

   JNI_EXC_VAL( env, retArray );

   /*
   Get the input angular velocity vector as well. 
   */
   getVec3D_jni ( env, J_av, av );

   JNI_EXC_VAL( env, retArray );

   /*
   Make the CSPICE call. 
   */
   rav2xf_c ( r, av, xform );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. 
   */
   createVecGD_jni ( env,  36,  CONST_VEC(xform),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function raxisa_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_raxisa
  (JNIEnv * env, 
   jclass             J_class, 
   jobjectArray       J_r,
   jdoubleArray       J_axis,
   jdoubleArray       J_angle   )
{
 
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.raxisa";

   SpiceDouble             angle;
   SpiceDouble             axis  [3];
   SpiceDouble             r     [3][3];

   /*
   Fetch the input matrix. 
   */
   getMat33D_jni ( env, J_r, r );

   JNI_EXC( env );

   
   raxisa_c ( r, axis, &angle );

   SPICE_EXC( env, caller );

   /*
   Update the output arrays. 
   */
   updateVec3D_jni ( env,     axis,   J_axis  );
   updateVecGD_jni ( env, 1,  &angle, J_angle );
}



/* 
Wrapper for CSPICE function reccyl_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_reccyl
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_rectan  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             rectan [3];
   SpiceDouble             result [3];

 
   /*
   Fetch the input coordinates into a C array. 
   */
   getVec3D_jni ( env, J_rectan, rectan );
  
   JNI_EXC_VAL ( env, retArray );


   reccyl_c ( rectan, result, result+1, result+2 );


   createVec3D_jni ( env, result, &retArray );

   return retArray;
}




/* 
Wrapper for CSPICE function recgeo_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_recgeo
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_rectan,
   jdouble            J_re,
   jdouble            J_f       )
{
   /*
   Local variables 
   */  
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.recgeo";

   SpiceDouble             rectan [3];
   SpiceDouble             result [3];

 
   /*
   Fetch the input coordinates into a C array. 
   */
   getVec3D_jni ( env, J_rectan, rectan );
  
   JNI_EXC_VAL ( env, retArray );




   recgeo_c ( rectan,  (SpiceDouble)J_re, (SpiceDouble)J_f, 
              result,  result+1,          result+2          );

   SPICE_EXC_VAL( env, caller, retArray );


   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function reclat_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_reclat
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_rectan )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             rectan [3];
   SpiceDouble             result [3];

 
   /*
   Fetch the input coordinates into a C array. 
   */
   getVec3D_jni ( env, J_rectan, rectan );
  
   JNI_EXC_VAL ( env, retArray );


   reclat_c ( rectan, result, result+1, result+2 );


   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function recpgr_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_recpgr
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_body,
   jdoubleArray       J_rectan,
   jdouble            J_re,
   jdouble            J_f       )
{
   /*
   Local variables 
   */  
   jdoubleArray            retArray = 0;

   static SpiceChar        body   [ BDNMLN ];

   static SpiceChar      * caller = "CSPICE.recpgr";

   SpiceDouble             rectan [ 3 ];
   SpiceDouble             result [ 3 ];


   /*
   Fetch the input string. 
   */
   getFixedInputString_jni ( env, J_body, BDNMLN, body );

   JNI_EXC_VAL ( env, retArray );

   /*
   Fetch the input coordinates into a C array. 
   */
   getVec3D_jni ( env, J_rectan, rectan );
  
   JNI_EXC_VAL ( env, retArray );


   recpgr_c ( body,
              rectan,  (SpiceDouble)J_re, (SpiceDouble)J_f, 
              result,  result+1,          result+2          );

   SPICE_EXC_VAL( env, caller, retArray );



   createVec3D_jni ( env, result, &retArray );

   return retArray;
}




/* 
Wrapper for CSPICE function recrad_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_recrad
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_rectan  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             rectan [3];
   SpiceDouble             result [3];

 
   /*
   Fetch the input coordinates into a C array. 
   */
   getVec3D_jni ( env, J_rectan, rectan );
  
   JNI_EXC_VAL ( env, retArray );


   recrad_c ( rectan, result, result+1, result+2 );


   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function recsph_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_recsph
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_rectan  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             rectan [3];
   SpiceDouble             result [3];

 
   /*
   Fetch the input coordinates into a C array. 
   */
   getVec3D_jni ( env, J_rectan, rectan );
  
   JNI_EXC_VAL ( env, retArray );


   recsph_c ( rectan, result, result+1, result+2 );


   createVec3D_jni ( env, result, &retArray );

   return retArray;
}




/* 
Wrapper for CSPICE function reset_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_reset
  (JNIEnv * env, jclass J_class)
{
   reset_c();
}



/* 
Wrapper for CSPICE function rotate_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_rotate
  (JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_angle,
   jint               J_iaxis    )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.rotate";

   SpiceDouble             mout [3][3];



   /*
   Make the CSPICE call. 
   */
   rotate_c ( (SpiceDouble)J_angle,  (SpiceInt)J_iaxis, mout );

   SPICE_EXC_VAL( env, caller, retArray );


   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( mout ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function rotmat_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_rotmat
  (JNIEnv           * env, 
   jclass             J_class, 
   jobjectArray       J_r,
   jdouble            J_angle,
   jint               J_iaxis    )
{
   /*
   Local variables  
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.rotmat";

   SpiceDouble             mout [3][3];
   SpiceDouble             r    [3][3];

   /*
   Get the input matrix in a C array. 
   */
   getMat33D_jni ( env, J_r, r );

   JNI_EXC_VAL ( env, retArray );


   /*
   Make the CSPICE call. 
   */
   rotmat_c ( r,  (SpiceDouble)J_angle,  (SpiceInt)J_iaxis, mout );

   SPICE_EXC_VAL( env, caller, retArray );


   /*
   Create the output array. 
   */
   createMat33D_jni ( env,  CONST_MAT( mout ),  &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function rotvec_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_rotvec
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v,
   jdouble            J_theta,
   jint               J_iaxis  )

{
   /*
   Local variables 
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.rotvec";

   SpiceDouble             rv   [3];
   SpiceDouble             v    [3];


   /*
   Fetch the Java vector. 
   */
   getVec3D_jni( env, J_v, v );

   JNI_EXC_VAL( env, retArray );


   rotvec_c( v,  (SpiceDouble)J_theta,  (SpiceInt)J_iaxis,  rv );

   SPICE_EXC_VAL( env, caller, retArray );


   createVec3D_jni( env, rv, &retArray );

   return( retArray );
}



/* 
Wrapper for CSPICE function rpd_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_rpd
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)rpd_c()  );
}



/* 
Wrapper for CSPICE function scdecd_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_scdecd
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_sc,
    jdouble       J_sclkdp ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.scdecd";
   static SpiceChar        sclkch [ SCLKLN ];


   /*
   Null-terminate `sclkch' in case the string is not initialized
   by the CSPICE call.
   */
   sclkch[0] = 0;
 

   scdecd_c ( (SpiceInt)J_sc, (SpiceDouble)J_sclkdp, SCLKLN, sclkch );
 

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller,  ((jstring)0) );

   /*
   Normal return. 
   */
   return (  createJavaString_jni( env, sclkch )  );
}



/* 
Wrapper for CSPICE function sce2c_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_sce2c
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_sc,
    jdouble       J_et  ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller  = "CSPICE.sce2c";
   SpiceDouble             sclkdp  = 0.0;

 
   sce2c_c ( (SpiceInt)J_sc, (SpiceDouble)J_et, &sclkdp );
 

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller,  ((jdouble)sclkdp) );

   /*
   Normal return. 
   */
   return ( (jdouble) sclkdp );
}


/* 
Wrapper for CSPICE function sce2s_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_sce2s
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_sc,
    jdouble       J_et  ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.sce2s";
   static SpiceChar        sclkch [ SCLKLN ];



   /*
   Null-terminate `sclkch' in case the string is not initialized
   by the CSPICE call.
   */
   sclkch[0] = 0;

   sce2s_c ( (SpiceInt)J_sc, (SpiceDouble)J_et, SCLKLN, sclkch );

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller,  ((jstring)0) );

   /*
   Normal return. 
   */
   return (  createJavaString_jni( env, sclkch )  );
}



/* 
Wrapper for CSPICE function scencd_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_scencd
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_sc,
    jstring       J_sclkch ) 
{ 
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.scencd";
   static SpiceChar        sclkch [ SCLKLN  ];

   SpiceDouble             sclkdp  =  0.0;


   /*
   Fetch the input string into a fixed length local array.
   */
   getFixedInputString_jni ( env, J_sclkch, SCLKLN, sclkch );

   /*
   Handle any exception that may have occurred. 
   */
   JNI_EXC_VAL ( env,  ((jdouble)sclkdp) );

 
   scencd_c ( (SpiceInt)J_sc, sclkch, &sclkdp );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jdouble)sclkdp) );

   /*
   Normal return. 
   */
   return ( (jdouble) sclkdp );
}



/* 
Wrapper for CSPICE function scfmt_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_scfmt
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_sc,
    jdouble       J_sclkdp ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.scfmt";
   static SpiceChar        sclkch [ SCLKLN ];
 

   /*
   Null-terminate `sclkch' in case the string is not initialized
   by the CSPICE call.
   */
   sclkch[0] = 0;

   scfmt_c ( (SpiceInt)J_sc, (SpiceDouble)J_sclkdp, SCLKLN, sclkch );
 

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller,  ((jstring)0) );

   /*
   Normal return. 
   */
   return (  createJavaString_jni( env, sclkch )  );
}



/* 
Wrapper for CSPICE function scs2e_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_scs2e
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_sc,
    jstring       J_sclkch ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.scs2e";
   static SpiceChar        sclkch [ SCLKLN  ];

   SpiceDouble             et  =  0.0;

   /*
   Fetch the input string into a fixed length local array.
   */
   getFixedInputString_jni ( env, J_sclkch, SCLKLN, sclkch );

   /*
   Handle any exception that may have occurred. 
   */
   JNI_EXC_VAL ( env,  ((jdouble)et) );
 

   scs2e_c ( (SpiceInt)J_sc, sclkch, &et );
 

   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jdouble)et) );

   /*
   Normal return. 
   */
   return ( (jdouble)et );
}



/* 
Wrapper for CSPICE function sct2e_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_sct2e
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_sc,
    jdouble       J_sclkdp  ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.sct2e";
   SpiceDouble et = 0;
 

   sct2e_c ( (SpiceInt)J_sc, (SpiceDouble)J_sclkdp, &et );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jdouble)et) );

   /*
   Normal return. 
   */
   return ( (jdouble) et );
}




/* 
Wrapper for CSPICE function sctiks_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_sctiks
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_sc,
    jstring       J_sclkch ) 
{ 
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.sctiks";
   static SpiceChar        sclkch [ SCLKLN  ];

   SpiceDouble             sclkdp  =  0.0;


   /*
   Fetch the input string into a fixed length local array.
   */
   getFixedInputString_jni ( env, J_sclkch, SCLKLN, sclkch );

   /*
   Handle any exception that may have occurred. 
   */
   JNI_EXC_VAL ( env,  ((jdouble)sclkdp) );
 

   sctiks_c ( (SpiceInt)J_sc, sclkch, &sclkdp );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jdouble)sclkdp) );


   /*
   Normal return. 
   */
   return ( (jdouble) sclkdp );
}



/* 
Wrapper for CSPICE function sincpt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_sincpt
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_method, 
   jstring            J_target, 
   jdouble            J_et,
   jstring            J_fixref,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jstring            J_dref,
   jdoubleArray       J_dvec,
   jdoubleArray       J_spoint, 
   jdoubleArray       J_trgepc, 
   jdoubleArray       J_srfvec,
   jbooleanArray      J_found   )
{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller  = "CSPICE.sincpt";
   static SpiceChar        dref    [ FRNMLN ];
   static SpiceChar        fixref  [ FRNMLN ];
   static SpiceChar      * method;
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];
   
   SpiceDouble             dvec    [ 3 ];
   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];
   SpiceDouble             srfvec  [ 3 ];
   SpiceDouble             trgepc;
  
   SpiceInt                methodLen;


   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr  );
   getFixedInputString_jni ( env, J_dref,   FRNMLN,     dref    );
   getFixedInputString_jni ( env, J_fixref, FRNMLN,     fixref  );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr  );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target  );
   getVarInputString_jni   ( env, J_method, &methodLen, &method );
   getVec3D_jni            ( env, J_dvec,               dvec    );

   JNI_EXC( env );

   et = (SpiceDouble)J_et;

   

   sincpt_c ( method,  target,  et,    fixref,   abcorr,  obsrvr,  
              dref,    dvec,  spoint,  &trgepc,  srfvec,  &found );


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
   Set the values of our output arrays, including the found flag.
   */
   updateVecGB_jni ( env, 1, &found,  J_found  );

   if ( found )
   {
      updateVec3D_jni ( env,    spoint,  J_spoint );
      updateVecGD_jni ( env, 1, &trgepc, J_trgepc );
      updateVec3D_jni ( env,    srfvec,  J_srfvec );
   }
}



/* 
Wrapper for CSPICE function spd_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_spd
  (JNIEnv * env, jclass J_class)
{
   return (jdouble)spd_c();
}



/* 
Wrapper for CSPICE function sphcyl_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_sphcyl
  (JNIEnv * env, 
   jclass             J_class, 
   jdouble            J_r,
   jdouble            J_colat,
   jdouble            J_lon     )
{
 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             result[3];


   sphcyl_c ( (SpiceDouble) J_r, 
              (SpiceDouble) J_colat, 
              (SpiceDouble) J_lon, 
              result,
              result+1,
              result+2              );

   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function sphlat_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_sphlat
  (JNIEnv * env, 
   jclass             J_class, 
   jdouble            J_r,
   jdouble            J_colat,
   jdouble            J_lon     )
{
 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             result[3];


   sphlat_c ( (SpiceDouble) J_r, 
              (SpiceDouble) J_colat, 
              (SpiceDouble) J_lon, 
              result,
              result+1,
              result+2              );

   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function sphrec_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_sphrec
  (JNIEnv * env, 
   jclass             J_class, 
   jdouble            J_r,
   jdouble            J_colat,
   jdouble            J_lon     )
{
 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             result[3];


   sphrec_c ( (SpiceDouble) J_r, 
              (SpiceDouble) J_colat, 
              (SpiceDouble) J_lon, 
              result               );

   createVec3D_jni ( env, result, &retArray );

   return retArray;
}



/* 
Wrapper for CSPICE function spkcls_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkcls
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.spkcls";

   
   spkcls_c ( (SpiceInt)J_handle );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function spkcov_c 

NOTE: the input and returned arrays have no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_spkcov
  (JNIEnv * env, 
   jclass             J_class,
   jstring            J_file,
   jint               J_body, 
   jint               J_size,
   jdoubleArray       J_cover  )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;


   /*
   The cell below will be allocated dynamically.
   */
   SpiceCell             * cover;

   static SpiceChar      * caller   = "CSPICE.spkcov";
   static SpiceChar        file  [ FNAMLN ];

   SpiceDouble           * coverData;

   SpiceInt                coverSize;
   SpiceInt                maxSize;


   /*
   Get the size of the input window data array. 
   */
   coverSize = (*env)->GetArrayLength ( env, J_cover );
   
   JNI_EXC_VAL  ( env, retArray );


   /*
   Capture the input SPK name. 
   */
   getFixedInputString_jni ( env, J_file, FNAMLN, file );
   JNI_EXC_VAL  ( env, retArray );

 
   if ( coverSize > 0 )
   {
      /*
      Capture the contents of the input array `cover' in dynamic
      memory.  Check out and return if an exception is thrown.
      */
      getVecGD_jni ( env, J_cover, &coverSize, &coverData );
      JNI_EXC_VAL  ( env, retArray );
   }
   else
   {
      coverData = 0;
   }

   
   /*
   If the specified output cell size is smaller than the input
   array size, we have a problem. 
   */
   maxSize = (SpiceInt)J_size;

   if ( maxSize < coverSize )
   {
      /*
      We must free the data from the input array before
      returning. 
      */
      if ( coverSize > 0 )
      {
         freeVecGD_jni ( env, J_cover, coverData );
      }

      setmsg_c ( "Input cell size is #; output size is #;" );
      errint_c ( "#",  coverSize                           );
      errint_c ( "#",  maxSize                             );
      sigerr_c ( "SPICE(OUTPUTCELLTOOSMALL)"               );

      SPICE_EXC_VAL(env, caller, retArray );
   }

   /*
   Create a dynamically allocated cell of size maxSize to hold
   the results of our spkcov_c call. Initialize the cell with
   the data from the input array, if any.
   */
   cover = zzalcell_c ( SPICE_DP, maxSize, coverSize, 0, coverData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   At this point, we're holding on to a dynamically allocated
   cell. We can't return before freeing this cell, so we must
   be careful about how we handle errors.

   However, we're now done with the coverData array.
   */
   if ( coverSize > 0 )
   {
      freeVecGD_jni ( env, J_cover, coverData );   
   }
   

   /*
   Make `cover' into a SPICE window. 
   */
   wnvald_c ( maxSize, coverSize, cover );

   
   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   We're finally ready for our CSPICE call. 
   */
   spkcov_c ( file, (SpiceInt)J_body, cover );


   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   At this point, the data portion of `cover' is exactly
   what we want to return. 
   */
   createVecGD_jni ( env, 
                     card_c(cover), 
                     (SpiceDouble *)cover->data, 
                     &retArray                   );

   /*
   De-allocate the dynamic cell before departure.
   */
   zzdacell_c ( cover );

   /*
   Handle any JNI or SPICE error. 
   */
   JNI_EXC_VAL  ( env,         retArray );
   SPICE_EXC_VAL( env, caller, retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function spkcpo_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkcpo
(  JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_target, 
   jdouble            J_et, 
   jstring            J_outref, 
   jstring            J_refloc, 
   jstring            J_abcorr, 
   jdoubleArray       J_obspos, 
   jstring            J_obsctr, 
   jstring            J_obsref, 
   jdoubleArray       J_state, 
   jdoubleArray       J_lt        )
{
   /*
   Local variables 
   */
   static SpiceChar        abcorr [ CORRLN ];
   static SpiceChar      * caller   = "CSPICE.spkcpo";
   static SpiceChar        obsctr [ BDNMLN ];
   static SpiceChar        obsref [ FRNMLN ];
   static SpiceChar        outref [ FRNMLN ];
   static SpiceChar        refloc [ FRNMLN ];
   static SpiceChar        target [ BDNMLN ];

   SpiceDouble             et;
   SpiceDouble             lt;
   SpiceDouble             obspos [3];
   SpiceDouble             state  [6];


   /*
   Get local copies of the input strings.

   Note that the strings are presumed to be left-justified.
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_outref, FRNMLN, outref );
   getFixedInputString_jni ( env, J_refloc, FRNMLN, refloc );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsctr, BDNMLN, obsctr );
   getFixedInputString_jni ( env, J_obsref, FRNMLN, obsref );

   /*
   Fetch input d.p. array. 
   */
   getVec3D_jni ( env, J_obspos, obspos);

   /*
   Check for exceptions and SPICE errors; return if any are 
   found. 
   */
   JNI_EXC  ( env );
   SPICE_EXC( env, caller );

   et = (SpiceDouble) J_et;
   

   spkcpo_c ( target, et,     outref, refloc, abcorr, 
              obspos, obsctr, obsref, state, &lt     );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Transfer the state and light time to the corresponding 
   output Java arrays. Note that light time is stored in
   an array of length 1 so that it may be treated as an
   output argument.
   */
   updateVecGD_jni ( env, 6, state,  J_state );
   updateVecGD_jni ( env, 1, &lt,    J_lt    );

   return;
}



/* 
Wrapper for CSPICE function spkcpt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkcpt
(  JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_trgpos, 
   jstring            J_trgctr, 
   jstring            J_trgref, 
   jdouble            J_et, 
   jstring            J_outref, 
   jstring            J_refloc, 
   jstring            J_abcorr, 
   jstring            J_obsrvr,
   jdoubleArray       J_state, 
   jdoubleArray       J_lt        )
{
   /*
   Local variables 
   */
   static SpiceChar        abcorr [ CORRLN ];
   static SpiceChar      * caller   = "CSPICE.spkcpt";
   static SpiceChar        obsrvr [ BDNMLN ];
   static SpiceChar        outref [ FRNMLN ];
   static SpiceChar        refloc [ FRNMLN ];
   static SpiceChar        trgctr [ BDNMLN ];
   static SpiceChar        trgref [ FRNMLN ];

   SpiceDouble             et;
   SpiceDouble             lt;
   SpiceDouble             trgpos [3];
   SpiceDouble             state  [6];


   /*
   Get local copies of the input strings.

   Note that the strings are presumed to be left-justified.
   */
   getFixedInputString_jni ( env, J_trgctr, BDNMLN, trgctr );
   getFixedInputString_jni ( env, J_trgref, FRNMLN, trgref );
   getFixedInputString_jni ( env, J_outref, FRNMLN, outref );
   getFixedInputString_jni ( env, J_refloc, FRNMLN, refloc );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );

   /*
   Fetch input d.p. array and scalars.
   */
   getVec3D_jni ( env, J_trgpos, trgpos );

   et = (SpiceDouble) J_et;

   /*
   Check for exceptions and SPICE errors; return if any are 
   found. 
   */
   JNI_EXC  ( env );
   SPICE_EXC( env, caller );
   

   spkcpt_c ( trgpos, trgctr, trgref, et,     outref,
              refloc, abcorr, obsrvr, state, &lt     );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Transfer the state and light time to the corresponding 
   output Java arrays. Note that light time is stored in
   an array of length 1 so that it may be treated as an
   output argument.
   */
   updateVecGD_jni ( env, 6, state,  J_state );
   updateVecGD_jni ( env, 1, &lt,    J_lt    );

   return;
}



/* 
Wrapper for CSPICE function spkcvo_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkcvo
(  JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_target, 
   jdouble            J_et, 
   jstring            J_outref, 
   jstring            J_refloc, 
   jstring            J_abcorr, 
   jdoubleArray       J_obssta, 
   jdouble            J_obsepc, 
   jstring            J_obsctr, 
   jstring            J_obsref, 
   jdoubleArray       J_state, 
   jdoubleArray       J_lt        )
{
   /*
   Local variables 
   */
   static SpiceChar        abcorr [ CORRLN ];
   static SpiceChar      * caller   = "CSPICE.spkcvo";
   static SpiceChar        obsctr [ BDNMLN ];
   static SpiceChar        obsref [ FRNMLN ];
   static SpiceChar        outref [ FRNMLN ];
   static SpiceChar        refloc [ FRNMLN ];
   static SpiceChar        target [ BDNMLN ];

   SpiceDouble             et;
   SpiceDouble             lt;
   SpiceDouble             obsepc;
   SpiceDouble             obssta [6];
   SpiceDouble             state  [6];


   /*
   Get local copies of the input strings.

   Note that the strings are presumed to be left-justified.
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_outref, FRNMLN, outref );
   getFixedInputString_jni ( env, J_refloc, FRNMLN, refloc );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsctr, BDNMLN, obsctr );
   getFixedInputString_jni ( env, J_obsref, FRNMLN, obsref );

   /*
   Fetch input d.p. array and scalars.
   */
   getVecFixedD_jni ( env, J_obssta, 6, obssta );

   et     = (SpiceDouble) J_et;
   obsepc = (SpiceDouble) J_obsepc;

   /*
   Check for exceptions and SPICE errors; return if any are 
   found. 
   */
   JNI_EXC  ( env );
   SPICE_EXC( env, caller );
   

   spkcvo_c ( target, et,     outref, refloc, abcorr, 
              obssta, obsepc, obsctr, obsref, state,  &lt );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Transfer the state and light time to the corresponding 
   output Java arrays. Note that light time is stored in
   an array of length 1 so that it may be treated as an
   output argument.
   */
   updateVecGD_jni ( env, 6, state,  J_state );
   updateVecGD_jni ( env, 1, &lt,    J_lt    );

   return;
}



/* 
Wrapper for CSPICE function spkcvt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkcvt
(  JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_trgsta, 
   jdouble            J_trgepc, 
   jstring            J_trgctr, 
   jstring            J_trgref, 
   jdouble            J_et, 
   jstring            J_outref, 
   jstring            J_refloc, 
   jstring            J_abcorr, 
   jstring            J_obsrvr,
   jdoubleArray       J_state, 
   jdoubleArray       J_lt        )
{
   /*
   Local variables 
   */
   static SpiceChar        abcorr [ CORRLN ];
   static SpiceChar      * caller   = "CSPICE.spkcvt";
   static SpiceChar        obsrvr [ BDNMLN ];
   static SpiceChar        outref [ FRNMLN ];
   static SpiceChar        refloc [ FRNMLN ];
   static SpiceChar        trgctr [ BDNMLN ];
   static SpiceChar        trgref [ FRNMLN ];

   SpiceDouble             et;
   SpiceDouble             lt;
   SpiceDouble             trgepc;
   SpiceDouble             trgsta [6];
   SpiceDouble             state  [6];


   /*
   Get local copies of the input strings.

   Note that the strings are presumed to be left-justified.
   */
   getFixedInputString_jni ( env, J_trgctr, BDNMLN, trgctr );
   getFixedInputString_jni ( env, J_trgref, FRNMLN, trgref );
   getFixedInputString_jni ( env, J_outref, FRNMLN, outref );
   getFixedInputString_jni ( env, J_refloc, FRNMLN, refloc );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );

   /*
   Fetch input d.p. array and scalars.
   */
   getVecFixedD_jni ( env, J_trgsta, 6, trgsta );

   et     = (SpiceDouble) J_et;
   trgepc = (SpiceDouble) J_trgepc;

   /*
   Check for exceptions and SPICE errors; return if any are 
   found. 
   */
   JNI_EXC  ( env );
   SPICE_EXC( env, caller );
   

   spkcvt_c ( trgsta, trgepc, trgctr, trgref, et,     
              outref, refloc, abcorr, obsrvr, state, &lt );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Transfer the state and light time to the corresponding 
   output Java arrays. Note that light time is stored in
   an array of length 1 so that it may be treated as an
   output argument.
   */
   updateVecGD_jni ( env, 6, state,  J_state );
   updateVecGD_jni ( env, 1, &lt,    J_lt    );

   return;
}






/* 
Wrapper for CSPICE function spkezr_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkezr
(  JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_target, 
   jdouble            J_et, 
   jstring            J_ref, 
   jstring            J_abcorr, 
   jstring            J_obsrvr, 
   jdoubleArray       J_state, 
   jdoubleArray       J_lt        )
{
   /*
   Local variables 
   */
   static SpiceChar        abcorr [ CORRLN ];
   static SpiceChar      * caller   = "CSPICE.spkezr";
   static SpiceChar        obsrvr [ BDNMLN ];
   static SpiceChar        target [ BDNMLN ];
   static SpiceChar        ref    [ FRNMLN ];

   SpiceDouble             et;
   SpiceDouble             lt;
   SpiceDouble             state [6];


   /*
   Get local copies of the input strings.

   Note that the strings are presumed to be left-justified.
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_ref,    FRNMLN, ref    );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );

   /*
   Check for exceptions and SPICE errors; return if any are 
   found. 
   */
   JNI_EXC  ( env );
   SPICE_EXC( env, caller );

   et = (SpiceDouble) J_et;
   


   spkezr_c ( target, et, ref, abcorr, obsrvr, state, &lt );


   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Transfer the state and light time to the corresponding 
   output Java arrays. Note that light time is stored in
   an array of length 1 so that it may be treated as an
   output argument.
   */
   updateVecGD_jni ( env, 6, state,  J_state );
   updateVecGD_jni ( env, 1, &lt,    J_lt    );

   return;
}



/* 
Wrapper for CSPICE function spkobj_c 

NOTE: the returned array has no control area.
*/
JNIEXPORT jintArray JNICALL Java_spice_basic_CSPICE_spkobj
  (JNIEnv * env, 
   jclass             J_class,
   jstring            J_file,
   jint               J_size,
   jintArray          J_cover  )
{ 
   /*
   Local variables 
   */
   jintArray            retArray = 0;


   /*
   The cells below will be dynamically allocated.
   */
   SpiceCell             * cover;

   static SpiceChar      * caller   = "CSPICE.spkobj";
   static SpiceChar        file  [ FNAMLN ];

   SpiceInt                coverSize;
   SpiceInt                maxSize;
   SpiceInt              * coverData;


   /*
   Get the size of the input set data array. 
   */
   coverSize = (*env)->GetArrayLength ( env, J_cover );
   
   JNI_EXC_VAL  ( env, retArray );


   /*
   Capture the input SPK name. 
   */
   getFixedInputString_jni ( env, J_file, FNAMLN, file );
   JNI_EXC_VAL  ( env, retArray );

 
   if ( coverSize > 0 )
   {
      /*
      Capture the contents of the input array `cover' in dynamic
      memory.  Check out and return if an exception is thrown.
      */
      getVecGI_jni ( env, J_cover, &coverSize, &coverData );
      JNI_EXC_VAL  ( env, retArray );
   }
   else
   {
      coverData = 0;
   }

   
   /*
   If the specified output cell size is smaller than the input
   array size, we have a problem. 
   */
   maxSize = (SpiceInt)J_size;

   if ( maxSize < coverSize )
   {
      /*
      We must free the data from the input array before
      returning. 
      */
      if ( coverSize > 0 )
      {
         freeVecGI_jni ( env, J_cover, coverData );
      }

      setmsg_c ( "Input cell size is #; output size is #;" );
      errint_c ( "#",  coverSize                           );
      errint_c ( "#",  maxSize                             );
      sigerr_c ( "SPICE(OUTPUTCELLTOOSMALL)"               );

      SPICE_EXC_VAL(env, caller, retArray );
   }

   /*
   Create a dynamically allocated cell of size maxSize to hold
   the results of our spkobj_c call. Initialize the cell with
   the data from the input array, if any.
   */
   cover = zzalcell_c ( SPICE_INT, maxSize, coverSize, 0, coverData );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   At this point, we're holding on to a dynamically allocated
   cell. We can't return before freeing this cell, so we must
   be careful about how we handle errors.

   However, we're now done with the coverData array.
   */
   if ( coverSize > 0 )
   {
      freeVecGI_jni ( env, J_cover, coverData );   
   }
   

   /*
   Make the input cell into a set before passing it to spkobj_c. 
   */
   valid_c ( maxSize, coverSize, cover );

   if ( failed_c() )
   {
      /*
      Free the ID set before departure. 
      */
      zzdacell_c ( cover );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   We're finally ready for our CSPICE call. 
   */
   spkobj_c ( file, cover );


   if ( failed_c() )
   {
      /*
      De-allocate the dynamic cell before all else. 
      */
      zzdacell_c ( cover );

      /*
      NOW throw an exception and return. 
      */
      SPICE_EXC_VAL(env, caller, retArray );
   }


   /*
   At this point, the data portion of `cover' is exactly
   what we want to return. 
   */
   createVecGI_jni ( env, 
                     card_c(cover), 
                     (SpiceInt *)cover->data, 
                     &retArray                   );

   /*
   De-allocate the dynamic cell before departure.
   */
   zzdacell_c ( cover );

   
   /*
   Handle any JNI or SPICE error. 
   */
   JNI_EXC_VAL  ( env,         retArray );
   SPICE_EXC_VAL( env, caller, retArray );


   return ( retArray );
}



/* 
Wrapper for CSPICE function spkopn_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_spkopn
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_fname,
   jstring            J_ifname,   
   jint               J_ncomch )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.spkopn";
   static SpiceChar        fname  [ FNAMLN ];
   static SpiceChar        ifname [ IFNLEN ];

   SpiceInt                handle = 0;


   /*
   Fetch the input strings. 
   */
   getFixedInputString_jni ( env, J_fname,  FNAMLN, fname  );
   getFixedInputString_jni ( env, J_ifname, IFNLEN, ifname );
   
   JNI_EXC_VAL( env, ((jint) handle) );


   spkopn_c ( fname, ifname, (SpiceInt)J_ncomch, &handle );


   SPICE_EXC_VAL( env, caller, ((jint)handle) );

   /*
   Normal return. 
   */
   return ( (jint)handle );
}



/* 
Wrapper for CSPICE function spkpos_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkpos
(  JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_target, 
   jdouble            J_et, 
   jstring            J_ref, 
   jstring            J_abcorr, 
   jstring            J_obsrvr, 
   jdoubleArray       J_pos, 
   jdoubleArray       J_lt        )
{
   /*
   Local variables 
   */
   static SpiceChar        abcorr [ CORRLN ];
   static SpiceChar      * caller   = "CSPICE.spkpos";
   static SpiceChar        obsrvr [ BDNMLN ];
   static SpiceChar        target [ BDNMLN ];
   static SpiceChar        ref    [ FRNMLN ];

   SpiceDouble             et;
   SpiceDouble             lt;
   SpiceDouble             pos [3];



   /*
   Get local copies of the input strings.

   Note that the strings are presumed to be left-justified.
   */
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_ref,    FRNMLN, ref    );
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );

   /*
   Check for exceptions and SPICE errors; return if any are 
   found. 
   */
   JNI_EXC  ( env );
   SPICE_EXC( env, caller );

   et = (SpiceDouble) J_et;
   
 
   spkpos_c ( target, et, ref, abcorr, obsrvr, pos, &lt );

 
   /*
   Throw an exception if a SPICE error occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Transfer the state and light time to the corresponding 
   output Java arrays. Note that light time is stored in
   an array of length 1 so that it may be treated as an
   output argument.
   */
   updateVec3D_jni ( env,    pos,  J_pos );
   updateVecGD_jni ( env, 1, &lt,  J_lt  );

   return;
}



/* 
Wrapper for CSPICE function spkuef_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkuef
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle )
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.spkuef";

 
   spkuef_c ( (SpiceInt) J_handle );
  
   /*
   If the call resulted in a SPICE error, throw an exception. 
   */
   SPICE_EXC ( env, caller );
}



/* 
Wrapper for CSPICE function spkw02_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkw02
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_body,
   jint               J_center,
   jstring            J_frame,
   jdouble            J_first,
   jdouble            J_last,
   jstring            J_segid,
   jdouble            J_intlen,
   jint               J_n,
   jint               J_polydg,
   jdoubleArray       J_cdata,
   jdouble            J_btime   )
{
   /*
   Local constants
   */                                        

   /*
   Local variables and initializations
   */
   static SpiceChar      * caller    = "CSPICE.spkw02";

   static SpiceChar        frame  [ FRNMLN ];
   static SpiceChar        segid  [ SIDLEN ];

   SpiceDouble           * cdata;

   SpiceInt                nElts;

   /*
   Fetch the frame name and segment ID.
   */
   getFixedInputString_jni ( env, J_frame, FRNMLN, frame );
   getFixedInputString_jni ( env, J_segid, SIDLEN, segid );

   JNI_EXC( env );

   /*
   Fetch the coefficient array into a one-dimensional C array.
   Both Java and C use row-major order, so the coefficients
   are correctly ordered.    
   */
   getVecGD_jni ( env, J_cdata, &nElts, &cdata );
   JNI_EXC( env );


   spkw02_c ( (SpiceInt)      J_handle,
              (SpiceInt)      J_body,
              (SpiceInt)      J_center,
              frame,
              (SpiceDouble)   J_first,
              (SpiceDouble)   J_last,
              segid,
              (SpiceDouble)   J_intlen,
              (SpiceInt)      J_n,
              (SpiceInt)      J_polydg,
              (SpiceDouble *) cdata,
              (SpiceDouble)   J_btime );

   /*
   Always free the dynamically allocated coefficient array. 
   */
   freeVecGD_jni ( env, J_cdata, cdata );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function spkw03_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkw03
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_body,
   jint               J_center,
   jstring            J_frame,
   jdouble            J_first,
   jdouble            J_last,
   jstring            J_segid,
   jdouble            J_intlen,
   jint               J_n,
   jint               J_polydg,
   jdoubleArray       J_cdata,
   jdouble            J_btime   )
{
   /*
   Local constants
   */                                        

   /*
   Local variables and initializations
   */
   static SpiceChar      * caller    = "CSPICE.spkw03";

   static SpiceChar        frame  [ FRNMLN ];
   static SpiceChar        segid  [ SIDLEN ];

   SpiceDouble           * cdata;

   SpiceInt                nElts;

   /*
   Fetch the frame name and segment ID.
   */
   getFixedInputString_jni ( env, J_frame, FRNMLN, frame );
   getFixedInputString_jni ( env, J_segid, SIDLEN, segid );

   JNI_EXC( env );

   /*
   Fetch the coefficient array into a one-dimensional C array.
   Both Java and C use row-major order, so the coefficients
   are correctly ordered.    
   */
   getVecGD_jni ( env, J_cdata, &nElts, &cdata );
   JNI_EXC( env );


   spkw03_c ( (SpiceInt)      J_handle,
              (SpiceInt)      J_body,
              (SpiceInt)      J_center,
              frame,
              (SpiceDouble)   J_first,
              (SpiceDouble)   J_last,
              segid,
              (SpiceDouble)   J_intlen,
              (SpiceInt)      J_n,
              (SpiceInt)      J_polydg,
              (SpiceDouble *) cdata,
              (SpiceDouble)   J_btime );

   /*
   Always free the dynamically allocated coefficient array. 
   */
   freeVecGD_jni ( env, J_cdata, cdata );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function spkw05_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkw05
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_body,
   jint               J_center,
   jstring            J_frame,
   jdouble            J_first,
   jdouble            J_last,
   jstring            J_segid,
   jdouble            J_gm,
   jint               J_n,
   jdoubleArray       J_states,
   jdoubleArray       J_epochs  )
{
   /*
   Local constants
   */                                        

   /*
   Local variables and initializations
   */
   static SpiceChar      * caller    = "CSPICE.spkw05";

   static SpiceChar        frame  [ FRNMLN ];
   static SpiceChar        segid  [ SIDLEN ];

   SpiceDouble           * epochs;
   SpiceDouble           * states;

   SpiceInt                nElts;
   SpiceInt                nEpochs;

   /*
   Fetch the frame name and segment ID.
   */
   getFixedInputString_jni ( env, J_frame, FRNMLN, frame );
   getFixedInputString_jni ( env, J_segid, SIDLEN, segid );

   JNI_EXC( env );

   /*
   Fetch the state array into a one-dimensional C array.
   Both Java and C use row-major order, so the state
   vectors are correctly ordered. 

   Fetch the epochs as well.
   */
   getVecGD_jni ( env, J_states, &nElts,   &states );
   JNI_EXC( env );

   getVecGD_jni ( env, J_epochs, &nEpochs, &epochs );
   JNI_EXC( env );



   spkw05_c ( (SpiceInt)    J_handle,
              (SpiceInt)    J_body,
              (SpiceInt)    J_center,
              frame,
              (SpiceDouble) J_first,
              (SpiceDouble) J_last,
              segid,
              (SpiceDouble) J_gm,
              (SpiceInt)    J_n,
              states,
              epochs                 );

   /*
   Always free the dynamically allocated state and epoch arrays. 
   */
   freeVecGD_jni ( env, J_states, states );
   freeVecGD_jni ( env, J_epochs, epochs );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function spkw09_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkw09
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_body,
   jint               J_center,
   jstring            J_frame,
   jdouble            J_first,
   jdouble            J_last,
   jstring            J_segid,
   jint               J_degree,
   jint               J_n,
   jdoubleArray       J_states,
   jdoubleArray       J_epochs  )
{
   /*
   Local constants
   */                                        

   /*
   Local variables and initializations
   */
   static SpiceChar      * caller    = "CSPICE.spkw09";

   static SpiceChar        frame  [ FRNMLN ];
   static SpiceChar        segid  [ SIDLEN ];

   SpiceDouble           * epochs;
   SpiceDouble           * states;

   SpiceInt                nElts;
   SpiceInt                nEpochs;

   /*
   Fetch the frame name and segment ID.
   */
   getFixedInputString_jni ( env, J_frame, FRNMLN, frame );
   getFixedInputString_jni ( env, J_segid, SIDLEN, segid );

   JNI_EXC( env );

   /*
   Fetch the state array into a one-dimensional C array.
   Both Java and C use row-major order, so the state
   vectors are correctly ordered. 

   Fetch the epochs as well.
   */
   getVecGD_jni ( env, J_states, &nElts,   &states );
   JNI_EXC( env );

   getVecGD_jni ( env, J_epochs, &nEpochs, &epochs );
   JNI_EXC( env );



   spkw09_c ( (SpiceInt)    J_handle,
              (SpiceInt)    J_body,
              (SpiceInt)    J_center,
              frame,
              (SpiceDouble) J_first,
              (SpiceDouble) J_last,
              segid,
              (SpiceInt)    J_degree,
              (SpiceInt)    J_n,
              states,
              epochs                 );


   /*
   Always free the dynamically allocated state and epoch arrays. 
   */
   freeVecGD_jni ( env, J_states, states );
   freeVecGD_jni ( env, J_epochs, epochs );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function spkw13_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_spkw13
  (JNIEnv * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_body,
   jint               J_center,
   jstring            J_frame,
   jdouble            J_first,
   jdouble            J_last,
   jstring            J_segid,
   jint               J_degree,
   jint               J_n,
   jdoubleArray       J_states,
   jdoubleArray       J_epochs  )
{
   /*
   Local constants
   */                                        

   /*
   Local variables and initializations
   */
   static SpiceChar      * caller    = "CSPICE.spkw13";

   static SpiceChar        frame  [ FRNMLN ];
   static SpiceChar        segid  [ SIDLEN ];

   SpiceDouble           * epochs;
   SpiceDouble           * states;

   SpiceInt                nElts;
   SpiceInt                nEpochs;

   /*
   Fetch the frame name and segment ID.
   */
   getFixedInputString_jni ( env, J_frame, FRNMLN, frame );
   getFixedInputString_jni ( env, J_segid, SIDLEN, segid );

   JNI_EXC( env );

   /*
   Fetch the state array into a one-dimensional C array.
   Both Java and C use row-major order, so the state
   vectors are correctly ordered. 

   Fetch the epochs as well.
   */
   getVecGD_jni ( env, J_states, &nElts,   &states );
   JNI_EXC( env );

   getVecGD_jni ( env, J_epochs, &nEpochs, &epochs );
   JNI_EXC( env );



   spkw13_c ( (SpiceInt)    J_handle,
              (SpiceInt)    J_body,
              (SpiceInt)    J_center,
              frame,
              (SpiceDouble) J_first,
              (SpiceDouble) J_last,
              segid,
              (SpiceInt)    J_degree,
              (SpiceInt)    J_n,
              states,
              epochs                 );


   /*
   Always free the dynamically allocated state and epoch arrays. 
   */
   freeVecGD_jni ( env, J_states, states );
   freeVecGD_jni ( env, J_epochs, epochs );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );
}




/* 
Wrapper for CSPICE function srfc2s_c
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_srfc2s
  ( JNIEnv     *  env, 
    jclass          J_class, 
    jint            J_code,
    jint            J_bodyid,
    jobjectArray    J_srfstr,
    jbooleanArray   J_isname )

{
   /*
   Local variables 
   */
   jstring                 jSrfStr;

   SpiceBoolean            isname;

   static SpiceChar      * caller   = "CSPICE.srfc2s";
   SpiceChar               srfstr[ SFNMLN ];


   srfc2s_c ( (SpiceInt)J_code, (SpiceInt)J_bodyid, SFNMLN, srfstr, &isname );

   SPICE_EXC( env, caller );

   /*
   Set the output string. 
   */
   jSrfStr = createJavaString_jni ( env, srfstr );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_srfstr, 0, jSrfStr );

   JNI_EXC( env );

   /*
   Set the "isname" flag. 
   */
   updateVecGB_jni ( env, 1, &isname, J_isname );

   JNI_EXC( env );

   return;
}





/* 
Wrapper for CSPICE function srfnrm_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_srfnrm
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_method,
   jstring            J_target,
   jdouble            J_et,
   jstring            J_fixref,
   jint               J_npts,
   jobjectArray       J_srfpts   )
{
   /*
   Local variables 
   */
   jobjectArray            jNormals = (jobjectArray)0;

   static SpiceChar      * caller   = "CSPICE.srfnrm";
   SpiceChar             * method;
   SpiceChar               fixref[ FRNMLN ];
   SpiceChar               target[ BDNMLN ];

   SpiceDouble             et;
   SpiceDouble          (* normls)[3];
   SpiceDouble          (* srfpts)[3];

   SpiceInt                methodLen;
   SpiceInt                ncols;
   SpiceInt                npts;
   SpiceInt                nrows;


   /*
   Get local copies of input scalars. 
   */
   et   = (SpiceDouble) J_et;
   npts = (SpiceInt   ) J_npts;

   if ( npts < 1 ) 
   {
      /*
      The array defining the input coordinates must be non-empty. 
      */
      setmsg_c ( "Coordinate pair count was #; this count must "
                 "be at least 1."                                );
      errint_c ( "#", npts                                       );
      sigerr_c ( "SPICE(INVALIDCOUNT)"                           );

      SPICE_EXC_VAL( env, caller, jNormals );
   }

   /*
   Fetch the input strings. 
   */
   getVarInputString_jni   ( env, J_method, &methodLen, &method );
   JNI_EXC_VAL( env, jNormals );

   getFixedInputString_jni ( env, J_target, BDNMLN,     target  );
   JNI_EXC_VAL( env, jNormals );

   getFixedInputString_jni ( env, J_fixref, FRNMLN,     fixref  );
   JNI_EXC_VAL( env, jNormals );

   /*
   Fetch the input surface point array. 
   */
   getMatGD_jni ( env, J_srfpts, &nrows, &ncols, (SpiceDouble **)&srfpts );
   JNI_EXC_VAL( env, jNormals );

   /*
   Check the dimensions of the `srfpts' array.
   */
   if (  ( nrows < npts ) || ( ncols != 3 )  )
   {
      /*
      Free the dynamic memory that has been allocated so far. 
      */
      free_SpiceMemory( (void *)  srfpts );

      freeVarInputString_jni ( env, J_method, method );


      setmsg_c ( "Input surface point array must have "
                 "row count at least as larget as the input npts, "
                 "and must have column count equal to 3."
                 "The actual array dimensions were #x#."           );
      errint_c ( "#", nrows                                        );
      errint_c ( "#", ncols                                        );
      sigerr_c ( "SPICE(INVALIDDIMENSION)"                         );
      
      SPICE_EXC_VAL( env, caller, jNormals );
   }


   /*
   Allocate a dynamic array to hold the output normals.
   */
   normls = ( SpiceDouble(*)[3] )alloc_SpiceDouble_C_array( npts, 3 );

   SPICE_EXC_VAL( env, caller, jNormals );

   /*
   Find the surface normals.
   */
   srfnrm_c ( method, target, et, fixref, npts, srfpts, normls );
   

   /*
   Regardless of whether the call succeeded, free the dynamic
   memory used to hold inputs. 

   Free the method string. 
   */
   freeVarInputString_jni ( env, J_method, method );

   /*
   Free the surface point array. 
   */
   free_SpiceMemory( (void *)  srfpts );


   /*
   Create an output Java vector containing the result. 
   */
   if ( !failed_c() ) 
   {
      createMatGD_jni ( env, npts, 3, normls, &jNormals );
      JNI_EXC_VAL( env, jNormals );
   }

   /*
   Free the normls array. 
   */
   free_SpiceMemory( (void *)  normls );

   /*
   Check for SPICE error.
   */
   SPICE_EXC_VAL( env, caller, jNormals );


   return jNormals;
}







/* 
Wrapper for CSPICE function srfcss_c
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_srfcss
  ( JNIEnv     *  env, 
    jclass          J_class, 
    jint            J_code,
    jstring         J_bodstr,
    jobjectArray    J_srfstr,
    jbooleanArray   J_isname )

{
   /*
   Local variables 
   */
   jstring                 jSrfStr;

   SpiceBoolean            isname;

   SpiceChar               bodstr[ BDNMLN ];
   static SpiceChar      * caller   = "CSPICE.srfcss";
   SpiceChar               srfstr[ SFNMLN ];


   /*
   Get a local string containing the body name.
   */
   getFixedInputString_jni ( env, J_bodstr, BDNMLN, bodstr );


   /*
   Get the surface string. 
   */
   srfcss_c ( (SpiceInt)J_code, bodstr, SFNMLN, srfstr, &isname );

   SPICE_EXC( env, caller );

   /*
   Set the output string. 
   */
   jSrfStr = createJavaString_jni ( env, srfstr );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_srfstr, 0, jSrfStr );

   JNI_EXC( env );

   /*
   Set the "isname" flag. 
   */
   updateVecGB_jni ( env, 1, &isname, J_isname );

   JNI_EXC( env );

   return;
}




/* 
Wrapper for CSPICE function srfs2c_c
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_srfs2c
  ( JNIEnv     *  env, 
    jclass          J_class, 
    jstring         J_srfstr,
    jstring         J_bodstr,
    jintArray       J_code,
    jbooleanArray   J_found )

{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   SpiceChar               bodstr[ BDNMLN ];
   static SpiceChar      * caller   = "CSPICE.srfs2c";
   SpiceChar               srfstr[ SFNMLN ];

   SpiceInt                code;


   /*
   Get a local string containing the surface name.
   */
   getFixedInputString_jni ( env, J_srfstr, SFNMLN, srfstr );
   JNI_EXC( env );

   /*
   Get a local string containing the body name.
   */
   getFixedInputString_jni ( env, J_bodstr, BDNMLN, bodstr );
   JNI_EXC( env );


   /*
   Get the surface code. 
   */
   srfs2c_c ( srfstr, bodstr, &code, &found );

   SPICE_EXC( env, caller );

   /*
   Set the output arrays. 
   */ 
   updateVecGI_jni ( env, 1, &code, J_code );
   JNI_EXC( env );

   /*
   Set the "isname" flag. 
   */
   updateVecGB_jni ( env, 1, &found, J_found );
   JNI_EXC( env );

   return;
}



/* 
Wrapper for CSPICE function srfscc_c
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_srfscc
  ( JNIEnv     *  env, 
    jclass          J_class, 
    jstring         J_srfstr,
    jint            J_bodyid,
    jintArray       J_code,
    jbooleanArray   J_found )

{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   static SpiceChar      * caller   = "CSPICE.srfscc";
   SpiceChar               srfstr[ SFNMLN ];

   SpiceInt                code;


   /*
   Get a local string containing the surface name.
   */
   getFixedInputString_jni ( env, J_srfstr, SFNMLN, srfstr );
   JNI_EXC( env );


   /*
   Get the surface code. 
   */
   srfscc_c ( srfstr, (SpiceInt)J_bodyid, &code, &found );

   SPICE_EXC( env, caller );

   /*
   Set the output arrays. 
   */ 
   updateVecGI_jni ( env, 1, &code, J_code );
   JNI_EXC( env );

   /*
   Set the "isname" flag. 
   */
   updateVecGB_jni ( env, 1, &found, J_found );
   JNI_EXC( env );

   return;
}





/* 
Wrapper for CSPICE function stelab_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_stelab
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_pobj,
   jdoubleArray       J_vobs  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller    = "CSPICE.stelab";

   SpiceDouble             pobj   [3];
   SpiceDouble             vobs   [3];
   SpiceDouble             appobj [3];
 

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni ( env, J_pobj, pobj );
   getVec3D_jni ( env, J_vobs, vobs );

   JNI_EXC_VAL( env, retArray );


   stelab_c( pobj, vobs, appobj );


   SPICE_EXC_VAL( env, caller, retArray );


   createVec3D_jni ( env, appobj, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function stlabx_c
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_stlabx
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_pobj,
   jdoubleArray       J_vobs  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller    = "CSPICE.stlabx";

   SpiceDouble             pobj   [3];
   SpiceDouble             vobs   [3];
   SpiceDouble             negv   [3];
   SpiceDouble             appobj [3];
 

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni ( env, J_pobj, pobj );
   getVec3D_jni ( env, J_vobs, vobs );

   JNI_EXC_VAL( env, retArray );


   /*
   Work around the fact that CSPICE has no stlabx wrapper. 
   */
   vminus_c ( vobs, negv );
   stelab_c ( pobj, negv, appobj );


   SPICE_EXC_VAL( env, caller, retArray );


   createVec3D_jni ( env, appobj, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function stpool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_stpool
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_name,
   jint               J_nth,
   jstring            J_contin,
   jobjectArray       J_component,
   jbooleanArray      J_found        )
{
   /*
   Local constants
   */                                        
   #define MAXCON          81

   /*
   Local variables and initializations
   */
   SpiceBoolean            found;

   static SpiceChar      * caller    = "CSPICE.stpool";
   static SpiceChar      * cdata     = 0;

   static SpiceChar        dtype    [ 1 ];
   static SpiceChar        contin   [ MAXCON ];
   static SpiceChar        kvname   [ KVNMLN ];
   static SpiceChar        message  [ LMSGLN ];

   SpiceInt                maxchr;
   SpiceInt                maxLen;
   SpiceInt                nBytes;
   SpiceInt                nth;
   SpiceInt                size;

   jstring                 retString = 0;
   


   /*
   Capture the kernel variable name in a local buffer, then
   release the dynamically allocated version of the name.
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, kvname );
   
   JNI_EXC( env );


   /*
   Capture the continuation marker string in a local buffer.
   */
   getFixedInputString_jni ( env, J_contin, MAXCON, contin );
   
   JNI_EXC( env );


   /*
   See whether the requested kernel variable is present in the kernel
   pool, and whether it has character data type.
   */
   dtpool_c ( kvname, &found, &size, dtype );

   SPICE_EXC ( env, caller );

   if (  ( !found )  ||  ( dtype[0] != 'C' )  )
   {
      /*
      We're going to throw a "kernel variable not found" 
      exception. The exception message will indicate
      the specific cause.
      */

      if ( !found )
      {
         strncpy ( message, 
                   "Kernel variable # was not found in the "
                   "kernel pool.",
                   LMSGLN                                   );
      }
      else
      {
         strncpy ( message, 
                   "Character kernel variable # "
                   "was not found in the kernel pool. "
                   "A numeric variable having this name "
                   "is present in the pool.",
                   LMSGLN                                );
      }

      /*
      Substitute the kernel variable name into the message. 
      */
      repmc_c ( message, "#", kvname, LMSGLN, message );

      /*
      Throw the exception and return. 
      */
      zzThrowException_jni ( env, KVNF_EXC, message );

      return;
   } 
   
   /*
   Find the maximum length of a kernel pool string datum. 
   This length is given by the kernel pool parameter MAXCHR.
   */
   szpool_c ( "MAXCHR", &maxchr, &found );

   SPICE_EXC( env, caller );


   /*
   Determine the maximum length of a string needed to capture
   the component. Allocate memory to hold this string. 
   */
   maxLen = maxchr * size + 1;

   nBytes = maxLen * sizeof(SpiceChar);

   cdata  = alloc_SpiceString ( nBytes );

   SPICE_EXC( env, caller );


   /*
   Finally, look up the requested kernel variable component. 
   */
   nth = (SpiceInt)J_nth;


   stpool_c ( kvname, nth, contin, maxLen, cdata, &size, &found );


   /*
   Handle any SPICE error that occurred on the 
   stpool_c call.
   */
   if ( failed_c() )
   {
      free_SpiceMemory ( (void *)  cdata );

      SPICE_EXC( env, caller );
   }

   /*
   We now deal with the unusual case of updating an output
   Java object array.

   Start by creating a Java string from the C string `cdata'.
   */  
   retString = createJavaString_jni ( env, cdata );

   /*
   Set element 0 of the object array to hold the Java string.
   */
   (*env)->SetObjectArrayElement ( env, J_component, 0, retString );

   /*
   Set the found flag.
   */
   updateVecGB_jni( env, 1, &found, J_found );

   /*
   Free the dynamic C string before returning. 
   */
   free_SpiceMemory ( (void *)  cdata );

   return;
}



/* 
Wrapper for CSPICE function str2et_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_str2et
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_timeString ) 
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.str2et";
   SpiceChar             * timstr;
   SpiceDouble             et  =  0;
   SpiceInt                timstrLen;
   

   /*
   Fetch input string into dynmically allocated memory. 
   Check for a JNI exception.
   */
   getVarInputString_jni ( env, J_timeString, &timstrLen, &timstr );
   JNI_EXC_VAL( env, ((jdouble)0.0) );


   str2et_c ( timstr, &et );


   /*
   Free the dynamically allocated memory.
   */
   freeVarInputString_jni ( env, J_timeString, timstr );

   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jdouble)0.0) );

   /*
   Normal return. 
   */ 
   return ( (jdouble) et );
}



/* 
Wrapper for CSPICE function subpnt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_subpnt
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_method, 
   jstring            J_target, 
   jdouble            J_et,
   jstring            J_fixref,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jdoubleArray       J_spoint, 
   jdoubleArray       J_trgepc, 
   jdoubleArray       J_srfvec   )
{

   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller  = "CSPICE.subpnt";
   static SpiceChar        fixref  [ FRNMLN ];
   static SpiceChar      * method;
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];
   SpiceDouble             srfvec  [ 3 ];
   SpiceDouble             trgepc;
  
   SpiceInt                methodLen;


   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr );
   getFixedInputString_jni ( env, J_fixref, FRNMLN,     fixref );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target );
   getVarInputString_jni   ( env, J_method, &methodLen, &method );

   et = (SpiceDouble)J_et;

   JNI_EXC( env );


   subpnt_c ( method,  target,  et,      fixref, 
              abcorr,  obsrvr,  spoint,  &trgepc,  srfvec );


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
   updateVecGD_jni ( env, 1, &trgepc, J_trgepc );
   updateVec3D_jni ( env,    srfvec,  J_srfvec );
}



/* 
Wrapper for CSPICE function subslr_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_subslr
(  JNIEnv           * env, 
   jclass             J_class,
   jstring            J_method, 
   jstring            J_target, 
   jdouble            J_et,
   jstring            J_fixref,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jdoubleArray       J_spoint, 
   jdoubleArray       J_trgepc, 
   jdoubleArray       J_srfvec   )
{

   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller  = "CSPICE.subslr";
   static SpiceChar        fixref  [ FRNMLN ];
   SpiceChar             * method;
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];
   SpiceDouble             srfvec  [ 3 ];
   SpiceDouble             trgepc;
  
   SpiceInt                methodLen;


   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr );
   getFixedInputString_jni ( env, J_fixref, FRNMLN,     fixref );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target );
   getVarInputString_jni   ( env, J_method, &methodLen, &method );

   et = (SpiceDouble)J_et;

   JNI_EXC( env );


   subslr_c ( method,  target,  et,      fixref, 
              abcorr,  obsrvr,  spoint,  &trgepc,  srfvec );


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
   updateVecGD_jni ( env, 1, &trgepc, J_trgepc );
   updateVec3D_jni ( env,    srfvec,  J_srfvec );
}



/* 
Wrapper for CSPICE function surfnm_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_surfnm
(  JNIEnv           * env, 
   jclass             J_class, 
   jdouble            J_a,
   jdouble            J_b,
   jdouble            J_c,
   jdoubleArray       J_point )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.surfnm";

   SpiceDouble             point  [3];
   SpiceDouble             normal [3];

   /*
   Get the input position vector. 
   */
   getVec3D_jni ( env, J_point, point );

   JNI_EXC_VAL( env, retArray );


   surfnm_c ( (SpiceDouble)J_a, 
              (SpiceDouble)J_b, 
              (SpiceDouble)J_c, 
              point, 
              normal            ); 

   SPICE_EXC_VAL( env, caller, retArray );

 
   /*
   Create the output vector. 
   */
   createVec3D_jni ( env, normal, &retArray );
   
   return ( retArray );
}



/* 
Wrapper for CSPICE function surfpt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_surfpt
(  JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_positn, 
   jdoubleArray       J_u, 
   jdouble            J_a,
   jdouble            J_b,
   jdouble            J_c,
   jdoubleArray       J_point,
   jbooleanArray      J_found  )
{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   static SpiceChar      * caller = "CSPICE.surfpt";

   SpiceDouble             point  [3];
   SpiceDouble             positn [3];
   SpiceDouble             u      [3];


   /*
   Get the input position vector. 
   */
   getVec3D_jni ( env, J_positn, positn );
   getVec3D_jni ( env, J_u,      u      );

   JNI_EXC( env );


   surfpt_c ( positn, 
              u,
              (SpiceDouble)J_a, 
              (SpiceDouble)J_b, 
              (SpiceDouble)J_c, 
              point, 
              &found            ); 

   SPICE_EXC( env, caller );


   /*
   Assign values to the output arguments by updating
   their elements. Note that J_found is an array. 
   */
   updateVecGB_jni ( env, 1, &found, J_found );

   if ( found )
   {
      updateVec3D_jni ( env, point, J_point );
   }
}





/* 
Wrapper for CSPICE function swpool_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_swpool
  (JNIEnv * env, 
   jclass             J_class, 
   jstring            J_agent,
   jobjectArray       J_names  )
{
   /*
   Local variables
   */
   static SpiceChar        agent  [ KVNMLN ];
   static SpiceChar      * caller = "CSPICE.swpool";

   SpiceInt                nStr;
   SpiceInt                fStrLen;

   void                  * fStrArray = 0;


   /*
   Fetch the agent name.  
   */
   getFixedInputString_jni ( env, J_agent, KVNMLN, agent );

   JNI_EXC( env );

   /*
   Fetch the array of watched kernel variable names in
   a Fortran-style array. 
   */
   getFortranStringArray_jni ( env,       J_names,    &nStr, 
                               &fStrLen,  &fStrArray         );
   JNI_EXC  ( env );
   SPICE_EXC( env, caller );

   /*
   Call the f2c'd routine swpool_.  Given the string manipulation tools
   we have, it's more convenient to do this than to call swpool_c.
   */
   swpool_ ( (char    * ) agent, 
             (integer * ) &nStr,
             (char    * ) fStrArray,
             (ftnlen    ) strlen(agent),
             (ftnlen    ) fStrLen       );

   /*
   Clean up our dynamically allocated array. The string
   utility that creates this array used malloc directly,
   so use free() here.
   */
   free ( fStrArray );

   /*
   Handle exceptions or SPICE errors. 
   */
   JNI_EXC  ( env ); 
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function sxform_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_sxform
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_from, 
   jstring            J_to, 
   jdouble            J_et     )

{
   /*
   Local variables 
   */
   jobjectArray            retArray = 0;

   static SpiceChar      * caller = "CSPICE.sxform";
   static SpiceChar        from  [ FRNMLN ];
   static SpiceChar        to    [ FRNMLN ];

   SpiceDouble             xform [6][6];


   /*
   Capture the input strings in local buffers.
   */
   getFixedInputString_jni ( env, J_from, FRNMLN, from );
   JNI_EXC_VAL( env, retArray );

   getFixedInputString_jni ( env, J_to,   FRNMLN, to   );
   JNI_EXC_VAL( env, retArray );

   /*
   Get the frame transformation. 
   */
   sxform_c ( from, to, (SpiceDouble)J_et, xform );

   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Create the output array. This array has dimensions 1x36.
   */
   createVecGD_jni ( env, 36, (CONST_VEC xform), &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function szpool_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_szpool
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_name    )
{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   static SpiceChar      * caller = "CSPICE.szpool";
   static SpiceChar        name    [ KVNMLN ];

   SpiceInt                param  = 0;


   /*
   Capture the parameter name in a local buffer, then
   release the dynamically allocated version of the name.
   */
   getFixedInputString_jni ( env, J_name, KVNMLN, name );
   
   JNI_EXC_VAL( env, ((jint)param) );


   /*
   Look up the requested kernel pool parameter.
   */
   szpool_c ( name, &param, &found );

   SPICE_EXC_VAL( env, caller, ((jint)param) );
  

   /*
   If the parameter was not found, throw an exception.
   */   
   if ( !found )
   {
      setmsg_c ( "Kernel pool parameter # does not exist." );
      errch_c  ( "#", name                                 );
      sigerr_c ( "SPICE(NOPARAMETER)"                      );

      SPICE_EXC_VAL( env, caller, ((jint)param) );
   }


   /*
   Return the requested value.
   */
   return ( (jint)param );
}






/* 
Wrapper for CSPICE function termpt_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_termpt
  (JNIEnv           * env, 
   jclass             J_class, 
   jstring            J_method,
   jstring            J_ilusrc,
   jstring            J_target,
   jdouble            J_et,
   jstring            J_fixref,
   jstring            J_abcorr,
   jstring            J_corloc,
   jstring            J_obsrvr,
   jdoubleArray       J_refvec,
   jdouble            J_rolstp,
   jint               J_ncuts,
   jdouble            J_schstp,
   jdouble            J_soltol,
   jint               J_maxn,
   jintArray          J_npts,
   jobjectArray       J_points,
   jdoubleArray       J_epochs,
   jobjectArray       J_trmvcs    )
{
   /*
   Local variables 
   */
   SpiceChar               abcorr[ CORRLN ];
   static SpiceChar      * caller   = "CSPICE.termpt";
   SpiceChar               corloc[ LOCLEN ];
   SpiceChar               fixref[ FRNMLN ];
   SpiceChar               ilusrc[ BDNMLN ];
   SpiceChar             * method;
   SpiceChar               obsrvr[ BDNMLN ];
   SpiceChar               target[ BDNMLN ];

   SpiceDouble           * epochs;
   SpiceDouble             et;
   SpiceDouble          (* points)[3];
   SpiceDouble             refvec [3];
   SpiceDouble             rolstp;
   SpiceDouble             schstp;
   SpiceDouble             soltol;
   SpiceDouble          (* trmvcs)[3];
   
   SpiceInt                i;
   SpiceInt                maxn;
   SpiceInt                methodLen;
   SpiceInt                ncuts;
   SpiceInt                needed;
   SpiceInt              * npts;
   SpiceInt                totpts;


   /*
   Get local copies of input scalars. 
   */
   et     = (SpiceDouble) J_et;
   rolstp = (SpiceDouble) J_rolstp;
   maxn   = (SpiceInt   ) J_maxn;
   ncuts  = (SpiceInt   ) J_ncuts;
   schstp = (SpiceDouble) J_schstp;
   soltol = (SpiceDouble) J_soltol;

   /*
   Fetch the reference vector. 
   */
   getVec3D_jni ( env, J_refvec, refvec );
   JNI_EXC( env );


   /*
   Fetch the input strings. 
   */
   getVarInputString_jni   ( env, J_method, &methodLen, &method );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_ilusrc, BDNMLN,     ilusrc  );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_target, BDNMLN,     target  );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_fixref, FRNMLN,     fixref  );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr  );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_corloc, LOCLEN,     corloc  );
   JNI_EXC( env );

   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr  );
   JNI_EXC( env );

 
   /*
   Let `needed' be the item count we'll use for dynamic memory allocation. 
   To avoid unpleasantness related to malloc and free, we'll make sure that 
   `needed' is strictly positive.
   */
   needed = maxi_c( 2, maxn, 1 );

    /*
   Allocate a dynamic array to hold the output point counts for each cut. 
   */
   npts = ( SpiceInt *)alloc_SpiceMemory( needed * sizeof(SpiceInt) );

   SPICE_EXC( env, caller);

   /*
   Allocate a dynamic array to hold the output terminator points. 
   */
   points = ( SpiceDouble(*)[3] )alloc_SpiceDouble_C_array( needed, 3 );

   SPICE_EXC( env, caller);

   /*
   Allocate a dynamic array to hold the output epochs. 
   */
   epochs = ( SpiceDouble * )alloc_SpiceDouble_C_array( needed, 1 );

   /*
   Allocate a dynamic array to hold the output terminator vectors. 
   */
   trmvcs = ( SpiceDouble(*)[3] )alloc_SpiceDouble_C_array( needed, 3 );

   SPICE_EXC( env, caller);


   /*
   Find the terminator points. 
   */
   termpt_c ( method, ilusrc, target, et,     fixref, abcorr, 
              corloc, obsrvr, refvec, rolstp, ncuts,  schstp,
              soltol, maxn,   npts,   points, epochs, trmvcs  );   

   /*
   Regardless of whether the call succeeded, free the dynamic
   memory used to hold inputs. 

   Free the method string. 
   */
   freeVarInputString_jni ( env, J_method, method );


   /*
   Update the output arrays. 
   */
   if ( !failed_c() )
   {
      /*
      Compute the total number of points found. 
      */
      totpts = 0;

      for ( i = 0;  i < ncuts;  i++ )
      {
         totpts += npts[i];
      }

      /*
      The `npts' array is a simple jintArray. 
      */
      updateVecGI_jni ( env, ncuts, npts, J_npts );
      JNI_EXC( env );

      /*
      The `points' array is a jobjectArray which has jdoubleArray
      elements.  
      */
      updateMatGD_jni ( env, totpts, 3, points, J_points );

      /*
      The `epochs' array is a simple jdoubleArray. 
      */
      updateVecGD_jni ( env, totpts, epochs, J_epochs );

      /*
      The `trmvcs' array is a jobjectArray which has jdoubleArray
      elements.  
      */
      updateMatGD_jni ( env, totpts, 3, trmvcs, J_trmvcs );

   }
 
   /*
   Free the dynamic memory used to store the values returned
   from termpt_c. 
   */
   free_SpiceMemory( (void *)  npts   );
   free_SpiceMemory( (void *)  points );
   free_SpiceMemory( (void *)  epochs );
   free_SpiceMemory( (void *)  trmvcs );

   /*
   Handle any SPICE errors. 
   */
   SPICE_EXC( env, caller );


   return;
}






/* 
Wrapper for CSPICE function tkvrsn_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_tkvrsn
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_item ) 
{
  
   /*
   Local variables 
   */   
   jstring                 retString = 0;

   static SpiceChar      * caller   = "CSPICE.tkvrsn";  
   static SpiceChar      * item;
   ConstSpiceChar        * valPtr;

   SpiceInt                itemLen;


  
   /*
   Fetch the input string into a dynamically allocated local array.
   */
   getVarInputString_jni ( env, J_item, &itemLen, &item );

   /*
   Check for a JNI exception.
   */
   JNI_EXC_VAL( env, retString );


   valPtr = tkvrsn_c ( item );


   /*
   De-allocate the item string, no matter what. 
   */
   freeVarInputString_jni ( env, J_item, item );


   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, retString );


   /*
   Normal return. 
   */ 
   return (  createJavaString_jni( env, (SpiceChar *)valPtr )  );
}



/* 
Wrapper for CSPICE function timout_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_timout
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jdouble       J_et,
    jstring       J_tpicture ) 
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.timout";
   static SpiceChar        timstr [ TIMLEN ];
   static SpiceChar        tpictr [ TIMLEN ];
   

   /*
   Fetch the input string into a fixed length local array.
   */
   getFixedInputString_jni ( env, J_tpicture, TIMLEN, tpictr );

   /*
   Check for a JNI exception.
   */
   JNI_EXC_VAL( env, ((jstring)0) );


   timout_c ( (SpiceDouble)J_et, tpictr, TIMLEN, timstr );


   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jstring)0) );

   /*
   Normal return. 
   */ 
   return (  createJavaString_jni( env, timstr )  );
}




/* 
Wrapper for CSPICE function tparse_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_tparse
  ( JNIEnv        * env, 
    jclass          cls,
    jstring         J_string,
    jdoubleArray    J_sp2000,
    jobjectArray    J_errmsg   )
{
   /*
   Local variables 
   */
   jstring                 jErrmsg;

   static SpiceChar      * caller = "CSPICE.tparse";
   static SpiceChar        errmsg  [ LMSGLN ];
   SpiceChar               string  [ TIMLEN ];

   SpiceDouble             sp2000;

   /*
   Fetch the input string. 
   */
   getFixedInputString_jni ( env, J_string, TIMLEN, string );

   JNI_EXC( env );

   tparse_c ( string, LMSGLN, &sp2000, errmsg );

   SPICE_EXC( env, caller );


   /*
   Update the output arrays. 
   */

   /*
   Set the "sp2000" argument. 
   */
   updateVecGD_jni ( env, 1, &sp2000, J_sp2000 );
   JNI_EXC( env );     
  
   /*
   Create a new Java string to hold the error message.

   Update the first element of J_errmsg with a reference
   to jErrmsg.
   */
   jErrmsg = createJavaString_jni ( env, errmsg );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_errmsg, 0, jErrmsg );   
   JNI_EXC( env );
}

 



/*
Wrapper for CSPICE function tpictr_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_tpictr
  ( JNIEnv        * env, 
    jclass          cls,
    jstring         J_sample,
    jobjectArray    J_pictur,
    jbooleanArray   J_ok,
    jobjectArray    J_errmsg   )
{
   /*
   Local variables 
   */
   jstring                 jErrmsg;
   jstring                 jPictur;

   SpiceBoolean            ok;

   static SpiceChar      * caller = "CSPICE.tpictr";
   static SpiceChar        errmsg  [ LMSGLN ];
   static SpiceChar        pictur  [ TIMLEN ];
   static SpiceChar        sample  [ TIMLEN ];


   /*
   Fetch the input string. 
   */
   getFixedInputString_jni ( env, J_sample, TIMLEN, sample );

   JNI_EXC( env );


   tpictr_c ( sample, TIMLEN, LMSGLN, pictur, &ok, errmsg );

   SPICE_EXC( env, caller );


   /*
   Update the output arrays. 
   */

   /*
   Create a new Java string to hold the picture.

   Update the first element of J_pictur with a reference
   to jPictur.
   */
   jPictur = createJavaString_jni ( env, pictur );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_pictur, 0, jPictur );   
   JNI_EXC( env );


   /*
   Set the "ok" argument. 
   */
   updateVecGB_jni ( env, 1, &ok, J_ok );
   JNI_EXC( env );


   /*
   Create a new Java string to hold the error message.

   Update the first element of J_errmsg with a reference
   to jErrmsg.
   */
   jErrmsg = createJavaString_jni ( env, errmsg );
   JNI_EXC( env );

   (*env)->SetObjectArrayElement ( env, J_errmsg, 0, jErrmsg );   
   JNI_EXC( env );
}



/* 
Wrapper for CSPICE function tsetyr_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_tsetyr
  ( JNIEnv *  env, 
    jclass    cls,
    jint      J_year )
{
   /*
   Local variables 
   */

   static SpiceChar      * caller = "CSPICE.tsetyr";


   tsetyr_c (  (SpiceInt) J_year  );

   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function twovec_c 
*/
JNIEXPORT jobjectArray JNICALL Java_spice_basic_CSPICE_twovec
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_axdef,
   jint               J_indexa,
   jdoubleArray       J_plndef,
   jint               J_indexp  )
{
   /*
   Local variables 
   */
   jobjectArray            retMat = 0;

   static SpiceChar      * caller = "CSPICE.twovec";

   SpiceDouble             axdef  [3];
   SpiceDouble             plndef [3];
   SpiceDouble             r      [3][3];

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni ( env, J_axdef,  axdef  );
   getVec3D_jni ( env, J_plndef, plndef );

   JNI_EXC_VAL( env, retMat );

   
   twovec_c ( axdef, (SpiceInt)J_indexa, plndef, (SpiceInt)J_indexp, r ); 

   SPICE_EXC_VAL( env, caller, retMat );

   
   /*
   Create and return the output matrix. 
   */
   createMat33D_jni ( env, CONST_MAT( r ), &retMat );

   return ( retMat );
}



/* 
Wrapper for CSPICE function tyear_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_tyear
  ( JNIEnv *  env, 
    jclass    cls  ) 
{
   return (  (jdouble)tyear_c()  );
}



/* 
Wrapper for CSPICE function ucrss_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_ucrss
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v1,
   jdoubleArray       J_v2  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             v1 [3];
   SpiceDouble             v2 [3];
   SpiceDouble             v3 [3];
 

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni ( env, J_v1, v1 );
   getVec3D_jni ( env, J_v2, v2 );

   JNI_EXC_VAL( env, retArray );

   ucrss_c( v1, v2, v3 );


   createVec3D_jni ( env, v3, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function unitim_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_unitim
  ( JNIEnv *  env, 
    jclass    J_class, 
    jdouble   J_et, 
    jstring   J_insys,  
    jstring   J_outsys ) 
{
   /*
   Local parameters 
   */
   #define  SYSLEN         81
 
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.unitim";
   static SpiceChar        insys  [ SYSLEN ];
   static SpiceChar        outsys [ SYSLEN ];

   SpiceDouble             t = 0.0;



   /*
   Fetch input strings.
   */
   getFixedInputString_jni ( env, J_insys,  SYSLEN, insys  );
   getFixedInputString_jni ( env, J_outsys, SYSLEN, outsys );

   JNI_EXC_VAL( env, ((jdouble) t) );

  
   t = unitim_c ( (SpiceDouble)J_et, insys, outsys );


   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jdouble)t) );

   /*
   Normal return. 
   */

   return ( (jdouble) t );
}



/* 
Wrapper for CSPICE function unload_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_unload
  ( JNIEnv *  env, 
    jclass    J_class, 
    jstring   J_file ) 
{
   /*
   Local variables 
   */   
   static SpiceChar      * caller   = "CSPICE.unload";
   SpiceChar             * file;
   SpiceInt                fileLen;


   /*
   Fetch input string into dynmically allocated memory. 
   Check for a JNI exception.
   */
   getVarInputString_jni ( env, J_file, &fileLen, &file );
   JNI_EXC( env );

 
   /*
   Unload the file. 
   */
   unload_c ( file );


   /*
   Free the dynamically allocated memory.
   */
   freeVarInputString_jni ( env, J_file, file );

   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC( env, caller );
}



/* 
Wrapper for CSPICE function unorm_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_unorm
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v1,
   jdoubleArray       J_vout,
   jdoubleArray       J_vmag   )
{
   /*
   Local variables 
   */
   SpiceDouble             v1   [3];
   SpiceDouble             vout [3];
   SpiceDouble             vmag ;


   /*
   Fetch the Java vector. 
   */
   getVec3D_jni ( env, J_v1, v1 );

   JNI_EXC( env );

   /*
   Note: unorm_c is error free. 
   */
   unorm_c( v1, vout, &vmag );

   
   updateVec3D_jni ( env,    vout,  J_vout );
   updateVecGD_jni ( env, 1, &vmag, J_vmag );

   JNI_EXC( env );
}



/* 
Wrapper for CSPICE function vcrss_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_vcrss
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v1,
   jdoubleArray       J_v2  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             v1 [3];
   SpiceDouble             v2 [3];
   SpiceDouble             v3 [3];
 

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni ( env, J_v1, v1 );
   getVec3D_jni ( env, J_v2, v2 );

   JNI_EXC_VAL( env, retArray );

   vcrss_c( v1, v2, v3 );


   createVec3D_jni ( env, v3, &retArray );

   return ( retArray );
}



/* 
Wrapper for CSPICE function vdist_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_vdist
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v1,
   jdoubleArray       J_v2  )
{
   /*
   Local variables 
   */
   SpiceDouble             result = 0;
   SpiceDouble             v1 [3];
   SpiceDouble             v2 [3];
 

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni ( env, J_v1, v1 );
   getVec3D_jni ( env, J_v2, v2 );

   JNI_EXC_VAL( env, ((jdouble) result) );

   result = vdist_c( v1, v2 );

   return ( (jdouble)result );
}



/* 
Wrapper for CSPICE function vhat_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_vhat
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v1  )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             v1 [3];
   SpiceDouble             v2 [3];


   /*
   Fetch the Java vector. 
   */
   getVec3D_jni( env, J_v1, v1 );

   JNI_EXC_VAL( env, retArray );


   vhat_c( v1, v2 );


   createVec3D_jni( env, v2, &retArray );

   return( retArray );
}



/* 
Wrapper for CSPICE function vnorm_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_vnorm
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v1  )
{
   /*
   Local variables 
   */
   SpiceDouble             result = 0;
   SpiceDouble             v1 [3];
 

   /*
   Fetch the Java vector. 
   */
   getVec3D_jni ( env, J_v1, v1 );


   result = vnorm_c( v1 );

   return ( (jdouble)result );
}


/* 
Wrapper for CSPICE function vperp_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_vperp
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_a,
   jdoubleArray       J_b      )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             a [3];
   SpiceDouble             b [3];
   SpiceDouble             p [3];


   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni( env, J_a, a );
   getVec3D_jni( env, J_b, b );

   JNI_EXC_VAL( env, retArray );

   /*
   Since vperp_c is error free, we won't call
   SPICE_EXC_VAL. 
   */
   vperp_c( a, b, p );

   createVec3D_jni( env, p, &retArray );

   return( retArray );
}



/* 
Wrapper for CSPICE function vprjp_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_vprjp
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_vin,
   jdoubleArray       J_plane    )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.vprjp";

   jdoubleArray            retArray = 0;

   SpiceDouble             vin        [3];
   SpiceDouble             planeArray [PLMAX];
   SpiceDouble             vout       [3];

   SpicePlane              plane;

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni    ( env, J_vin,      vin        );
   getVecFixedD_jni( env, J_plane, 4, planeArray );

   JNI_EXC_VAL( env, retArray );

   /*
   Create a SPICE plane from the input array. 
   */
   nvc2pl_c ( planeArray, planeArray[3], &plane );

   vprjp_c( vin, &plane, vout );


   SPICE_EXC_VAL ( env, caller, retArray );


   createVec3D_jni( env, vout, &retArray );

   return( retArray );
}



/* 
Wrapper for CSPICE function vprjpi_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_vprjpi
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_vin,
   jdoubleArray       J_projpl,  
   jdoubleArray       J_invpl,  
   jdoubleArray       J_vout,
   jbooleanArray      J_found  )
{
   /*
   Local variables 
   */
   SpiceBoolean            found;

   static SpiceChar      * caller = "CSPICE.vprjpi";

   SpiceDouble             vin         [3];
   SpiceDouble             projplArray [PLMAX];
   SpiceDouble             invplArray  [PLMAX];
   SpiceDouble             vout        [3];

   SpicePlane              projPlane;
   SpicePlane              invPlane;

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni    ( env, J_vin,           vin         );
   getVecFixedD_jni( env, J_projpl, PLMAX, projplArray );
   getVecFixedD_jni( env, J_invpl,  PLMAX, invplArray  );

   JNI_EXC( env );

   /*
   Create SPICE planes from the input plane arrays. 
   */
   nvc2pl_c ( projplArray, projplArray[3], &projPlane );
   nvc2pl_c ( invplArray,  invplArray[3],  &invPlane  );


   vprjpi_c( vin, &projPlane, &invPlane, vout, &found );


   SPICE_EXC ( env, caller );

   /*
   Set output arguments. 
   */
   updateVecGB_jni( env, 1,    &found,  J_found );  
   updateVec3D_jni( env,       vout,    J_vout  );   
}



/* 
Wrapper for CSPICE function vproj_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_vproj
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_a,
   jdoubleArray       J_b      )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             a [3];
   SpiceDouble             b [3];
   SpiceDouble             p [3];


   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni( env, J_a, a );
   getVec3D_jni( env, J_b, b );

   JNI_EXC_VAL( env, retArray );

   /*
   Since vproj_c is error free, we won't call
   SPICE_EXC_VAL. 
   */
   vproj_c( a, b, p );

   createVec3D_jni( env, p, &retArray );

   return( retArray );
}



/* 
Wrapper for CSPICE function vrotv_c 
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_vrotv
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v,
   jdoubleArray       J_axis,
   jdouble            J_theta   )
{
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceDouble             axis [3];
   SpiceDouble             rv   [3];
   SpiceDouble             v    [3];


   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni( env, J_v,    v    );
   getVec3D_jni( env, J_axis, axis );

   JNI_EXC_VAL( env, retArray );

   /*
   Since vrotv_c is error free, we won't call
   SPICE_EXC_VAL. 
   */
   vrotv_c( v, axis, (SpiceDouble)J_theta, rv );


   createVec3D_jni( env, rv, &retArray );

   return( retArray );
}



/* 
Wrapper for CSPICE function vsep_c 
*/
JNIEXPORT jdouble JNICALL Java_spice_basic_CSPICE_vsep
  (JNIEnv * env, 
   jclass             J_class, 
   jdoubleArray       J_v1,
   jdoubleArray       J_v2  )
{
   /*
   Local variables 
   */
   SpiceDouble             result = 0;
   SpiceDouble             v1 [3];
   SpiceDouble             v2 [3];
 

   /*
   Fetch the Java vectors. 
   */
   getVec3D_jni ( env, J_v1, v1 );
   getVec3D_jni ( env, J_v2, v2 );

   JNI_EXC_VAL( env, ((jdouble) result) );

   result = vsep_c( v1, v2 );

   return ( (jdouble)result );
}



/* 
Wrapper for CSPICE function wncomd_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wncomd
  (JNIEnv * env, 
   jclass             J_class,
   jdouble            J_left,
   jdouble            J_right,
   jdoubleArray       J_window     )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceCell             * window;
   SpiceCell             * compwin;

   static SpiceChar      * caller   = "CSPICE.wncomd";
   
   SpiceDouble           * outData;
   SpiceDouble           * windowData;

   SpiceInt                insize;
   SpiceInt                outsize;


   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC_VAL ( env, retArray ); 

 
   /*
   `outsize' is the largest possible cell cardinality of the output window.
   */
   outsize  = insize + 2;

   /*
   At this point we can create a dynamically allocated cell representing
   the input window; this cell has enough room to hold the result
   of the insertion.
   */   
   window = zzalcell_c ( SPICE_DP, insize, insize, 0, windowData );
  
   if ( failed_c() )
   {
      freeVecGD_jni ( env, J_window, windowData );

      SPICE_EXC_VAL( env, caller, retArray );      
   }

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );

   /*
   Allocate a window to hold the result of the complement operation.
   */
   compwin = zzalcell_c ( SPICE_DP, outsize, 0, 0, 0 );

   /*
   If we've had a SPICE error, don't continue. De-allocate the 
   window holding the input window contents before returning.
   */
   if ( failed_c() )
   {
      zzdacell_c ( window );

      SPICE_EXC_VAL( env, caller, retArray );
   }


   /*
   Perform the contraction.
   */
   wncomd_c ( (SpiceDouble)J_left, 
              (SpiceDouble)J_right, window, compwin );


   /*
   De-allocate the input cell. 
   */
   zzdacell_c ( window );

   /*
   If the CSPICE call failed, de-allocate the output cell.
   */
   if ( failed_c() )
   {
      zzdacell_c ( compwin );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   Return the result in a new Java array.
   */
   outData = (SpiceDouble *) (compwin->data);

   createVecGD_jni ( env, compwin->card, outData, &retArray );

   /*
   Free the dynamically allocated output cell.
   */
   zzdacell_c ( compwin );

   return retArray;
}



/* 
Wrapper for CSPICE function wncond_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wncond
  (JNIEnv * env, 
   jclass             J_class,
   jdouble            J_left,
   jdouble            J_right,
   jdoubleArray       J_window     )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceCell             * window;
   
   static SpiceChar      * caller   = "CSPICE.wncond";

   SpiceDouble           * outData;
   SpiceDouble           * windowData;

   SpiceInt                insize;
   SpiceInt                outsize;


   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC_VAL ( env, retArray ); 

   /*
   `outsize' is the largest possible cardinality of the output window.
   */
   outsize  = insize;

   /*
   At this point we can create a dynamically allocated cell representing
   the input window; this cell has enough room to hold the result
   of the insertion.
   */   
   window = zzalcell_c ( SPICE_DP, outsize, insize, 0, windowData );
  
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );

   /*
   Perform the contraction.
   */
   wncond_c ( (SpiceDouble)J_left, (SpiceDouble)J_right, window );


   /*
   If the CSPICE call failed, de-allocate the cell.
   */
   if ( failed_c() )
   {
      zzdacell_c ( window );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   Return the result in a new Java array.
   */
   outData = (SpiceDouble *) (window->data);

   createVecGD_jni ( env, window->card, outData, &retArray );

   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   return retArray;
}



/* 
Wrapper for CSPICE function wndifd_c 

NOTE: the input and returned arrays have no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wndifd
  (JNIEnv * env, 
   jclass             J_class,
   jdoubleArray       J_a,
   jdoubleArray       J_b     )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;


   /*
   The cells below will have their data arrays allocated
   at run time; their sizes will be set when they're known. 
   */
   SpiceCell             * a;
   SpiceCell             * b;
   SpiceCell             * c;

   static SpiceChar      * caller   = "CSPICE.wndifd";
   
   SpiceDouble           * aData;
   SpiceDouble           * bData;
   SpiceDouble           * cData;

   SpiceInt                size;
   SpiceInt                sizea;
   SpiceInt                sizeb;
   SpiceInt                sizec;


   /*
   Capture the contents of the input arrays `a' and `b' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_a, &sizea, &aData );
   JNI_EXC_VAL  ( env, retArray )

   getVecGD_jni ( env, J_b, &sizeb, &bData );

   /*
   If an exception has been thrown, we must attempt to release
   the dynamic memory allocated for the first input cell. 
   */
   if (  (*env)->ExceptionOccurred( env )  )
   {
      (*env)->ReleaseDoubleArrayElements(env, J_a, (jdouble *)aData, 0);

      return retArray;
   }
 

   /*
   `sizec' is the largest possible cardinality of the difference
   of the input windows.
   */
   sizec  = sizea + sizeb;

   /*
   At this point we can create dynamically allocated cells representing
   the input windows. We also allocate a cell to hold the output window.
   For now, the output cell has cardinality 0.
   */   
   a = zzalcell_c ( SPICE_DP, sizea, sizea, 0, aData );
   b = zzalcell_c ( SPICE_DP, sizeb, sizeb, 0, bData );
   c = zzalcell_c ( SPICE_DP, sizec, 0,     0, 0     );

   /*
   De-allocate the input data arrays since we've transferred the
   input data to cells `a' and `b'.
   */
   freeVecGD_jni ( env, J_a, aData );
   freeVecGD_jni ( env, J_b, bData );

   /*
   If we had a memory allocation error, we'll de-allocate the dynamically
   allocated cells before leaving. 
   */
   if ( failed_c() ) 
   {
      zzdacell_c ( a );
      zzdacell_c ( b );
      zzdacell_c ( c );

      SPICE_EXC_VAL ( env, caller, retArray );
   }


   /*
   Compute the difference of the input windows.
   */
   wndifd_c ( a, b, c );


   /*
   De-allocate the input cells.
   */
   zzdacell_c ( a );
   zzdacell_c ( b );

 
   /*
   If the CSPICE call failed, de-allocate the output cell 
   for wndifd_c.
   */
   if ( failed_c() )
   {
      zzdacell_c ( c );

      SPICE_EXC_VAL ( env, caller, retArray );
   }


   /*
   Create a result array and transfer the contents of the window `c'
   to the result array. 
   */
   size  = card_c( c );

   cData = (SpiceDouble *) (c->data);

   createVecGD_jni ( env, size, cData, &retArray );

   /*
   Free the dynamically allocated output array for wndifd_c.
   */
   zzdacell_c ( c );

   return retArray; 
}



/* 
Wrapper for CSPICE function wnelmd_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_wnelmd
  (JNIEnv * env, 
   jclass             J_class,
   jdouble            J_point,
   jdoubleArray       J_window     )
{ 
   /*
   Local variables 
   */

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceBoolean            retVal = 0;

   SpiceCell             * window;
   
   static SpiceChar      * caller   = "CSPICE.wnelmd";

   SpiceDouble           * windowData;

   SpiceInt                insize;


   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC_VAL ( env, ((jboolean)retVal) ); 


   /*
   At this point we can create a dynamically allocated cell representing
   the input window.
   */   
   window = zzalcell_c ( SPICE_DP, insize, insize, 0, windowData );
  
   SPICE_EXC_VAL( env, caller, ((jboolean)retVal) );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );


   /*
   Perform the inclusion test.
   */
   retVal = wnelmd_c ( (SpiceDouble)J_point, window );


   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   /*
   If the CSPICE call failed, throw an exception.
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)retVal) );

   /*
   Normal return. 
   */
   return ( retVal );
}



/* 
Wrapper for CSPICE function wnexpd_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wnexpd
  (JNIEnv * env, 
   jclass             J_class,
   jdouble            J_left,
   jdouble            J_right,
   jdoubleArray       J_window     )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceCell             * window;

   static SpiceChar      * caller   = "CSPICE.wnexpd";
   
   SpiceDouble           * outData;
   SpiceDouble           * windowData;

   SpiceInt                insize;
   SpiceInt                outsize;


   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC_VAL ( env, retArray ); 
 

   /*
   `outsize' is the largest possible cardinality of the output window.
   */
   outsize = insize;

   /*
   At this point we can create a dynamically allocated cell representing
   the input window; this cell has enough room to hold the result
   of the insertion.
   */   
   window = zzalcell_c ( SPICE_DP, outsize, insize, 0, windowData );
  
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );

   /*
   Perform the expansion.
   */
   wnexpd_c ( (SpiceDouble)J_left, (SpiceDouble)J_right, window );


   /*
   If the CSPICE call failed, de-allocate the cell.
   */
   if ( failed_c() )
   {
      zzdacell_c ( window );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   Return the result in a new Java array.
   */
   outData = (SpiceDouble *) (window->data);

   createVecGD_jni ( env, window->card, outData, &retArray );

   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   return retArray;
}

 
/* 
Wrapper for CSPICE function wnextd_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wnextd
  (JNIEnv * env, 
   jclass             J_class,
   jstring            J_side,
   jdoubleArray       J_window     )
{ 
   /*
   Local parameters 
   */
   #define SIDELN          2


   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceCell             * window;
   
   static SpiceChar      * caller   = "CSPICE.wnextd";
   SpiceChar               side  [ SIDELN ];

   SpiceDouble           * outData;
   SpiceDouble           * windowData;

   SpiceInt                insize;
   SpiceInt                outsize;


   /*
   Capture the input string. 
   */
   getFixedInputString_jni ( env, J_side, SIDELN, side );

   JNI_EXC_VAL ( env, retArray ); 

   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC_VAL ( env, retArray ); 

   /*
   `outsize' is the largest possible cardinality of the output
   window following the extraction operation.
   */
   outsize = insize;

   /*
   At this point we can create a dynamically allocated cell representing
   the input window; this cell has enough room to hold the result
   of the extraction.
   */   
   window = zzalcell_c ( SPICE_DP, outsize, insize, 0, windowData );
  
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );

   /*
   Perform the extraction. Note that `side' is a character, not
   a string.
   */
   wnextd_c ( side[0], window );


   /*
   If the CSPICE call failed, de-allocate the cell.
   */
   if ( failed_c() )
   {
      zzdacell_c ( window );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   Return the result in a new Java array.
   */
   outData = (SpiceDouble *) (window->data);

   createVecGD_jni ( env, window->card, outData, &retArray );

   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   return ( retArray );
}



/* 
Wrapper for CSPICE function wnfild_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wnfild
  (JNIEnv * env, 
   jclass             J_class,
   jdouble            J_small,
   jdoubleArray       J_window     )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceCell             * window;
   
   static SpiceChar      * caller   = "CSPICE.wnfild";

   SpiceDouble           * outData;
   SpiceDouble           * windowData;

   SpiceInt                insize;
   SpiceInt                outsize;


   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC_VAL ( env, retArray ); 

   /*
   `outsize' is the largest possible cardinality of the output
   window following the fill operation.
   */
   outsize = insize;

   /*
   At this point we can create a dynamically allocated cell representing
   the input window; this cell has enough room to hold the result
   of the fill operation.
   */   
   window = zzalcell_c ( SPICE_DP, outsize, insize, 0, windowData );
  
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );

   /*
   Perform the fill operation.
   */
   wnfild_c ( (SpiceDouble)J_small, window );


   /*
   If the CSPICE call failed, de-allocate the cell.
   */
   if ( failed_c() )
   {
      zzdacell_c ( window );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   Return the result in a new Java array.
   */
   outData = (SpiceDouble *) (window->data);

   createVecGD_jni ( env, window->card, outData, &retArray );

   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   return ( retArray );
}



/* 
Wrapper for CSPICE function wnfltd_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wnfltd
  (JNIEnv * env, 
   jclass             J_class,
   jdouble            J_small,
   jdoubleArray       J_window     )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceCell             * window;
   
   static SpiceChar      * caller   = "CSPICE.wnfltd";

   SpiceDouble           * outData;
   SpiceDouble           * windowData;

   SpiceInt                insize;
   SpiceInt                outsize;


   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC_VAL ( env, retArray ); 

   /*
   `outsize' is the largest possible cardinality of the output
   window following the filter operation.
   */
   outsize = insize;

   /*
   At this point we can create a dynamically allocated cell representing
   the input window; this cell has enough room to hold the result
   of the filter operation.
   */   
   window = zzalcell_c ( SPICE_DP, outsize, insize, 0, windowData );
  
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );

   /*
   Perform the filter operation.
   */
   wnfltd_c ( (SpiceDouble)J_small, window );

   /*
   If the CSPICE call failed, de-allocate the cell.
   */
   if ( failed_c() )
   {
      zzdacell_c ( window );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   Return the result in a new Java array.
   */
   outData = (SpiceDouble *) (window->data);

   createVecGD_jni ( env, window->card, outData, &retArray );

   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   return ( retArray );
}



/* 
Wrapper for CSPICE function wnincd_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_wnincd
  (JNIEnv * env, 
   jclass             J_class,
   jdouble            J_left,
   jdouble            J_right,
   jdoubleArray       J_window     )
{ 
   /*
   Local variables 
   */

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceBoolean            retVal = 0;

   SpiceCell             * window;
   
   static SpiceChar      * caller   = "CSPICE.wnincd";

   SpiceDouble           * windowData;

   SpiceInt                insize;


   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC_VAL ( env, ((jboolean)retVal) ); 


   /*
   At this point we can create a dynamically allocated cell representing
   the input window.
   */   
   window = zzalcell_c ( SPICE_DP, insize, insize, 0, windowData );
  
   SPICE_EXC_VAL( env, caller, ((jboolean)retVal) );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );

   /*
   Perform the inclusion test.
   */
   retVal = wnincd_c ( (SpiceDouble)J_left, (SpiceDouble)J_right, window );


   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   /*
   If the CSPICE call failed, throw an exception.
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)retVal) );

   /*
   Normal return. 
   */
   return ( retVal );
}



/* 
Wrapper for CSPICE function wninsd_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wninsd
  (JNIEnv * env, 
   jclass             J_class,
   jdouble            J_left,
   jdouble            J_right,
   jdoubleArray       J_window     )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceCell             * window;
   
   static SpiceChar      * caller   = "CSPICE.wninsd";

   SpiceDouble           * outData;
   SpiceDouble           * windowData;

   SpiceInt                insize;
   SpiceInt                outsize;


   /*
   Capture the contents of the input array `J_window' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_window, &insize, &windowData );

   JNI_EXC_VAL ( env, retArray ); 

   /*
   `outsize' is the largest possible cardinality of the output
   window following insertion.
   */
   outsize  = insize + 2;

   /*
   At this point we can create a dynamically allocated cell representing
   the input window; this cell has enough room to hold the result
   of the insertion.
   */   
   window = zzalcell_c ( SPICE_DP, outsize, insize, 0, windowData );
  
   SPICE_EXC_VAL( env, caller, retArray );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_window, windowData );

   /*
   Perform the insertion.
   */
   wninsd_c ( (SpiceDouble)J_left, (SpiceDouble)J_right, window );


   /*
   If the CSPICE call failed, de-allocate the cell.
   */
   if ( failed_c() )
   {
      zzdacell_c ( window );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   Return the result in a new Java array.
   */
   outData = (SpiceDouble *) (window->data);

   createVecGD_jni ( env, window->card, outData, &retArray );

   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   return retArray;
}



/* 
Wrapper for CSPICE function wnintd_c 

NOTE: the input and returned arrays have no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wnintd
  (JNIEnv * env, 
   jclass             J_class,
   jdoubleArray       J_a,
   jdoubleArray       J_b     )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * a;
   SpiceCell             * b;
   SpiceCell             * c;
   
   static SpiceChar      * caller   = "CSPICE.wnintd";

   SpiceDouble           * aData;
   SpiceDouble           * bData;
   SpiceDouble           * cData;

   SpiceInt                size;
   SpiceInt                sizea;
   SpiceInt                sizeb;
   SpiceInt                sizec;


   /*
   Capture the contents of the input arrays `a' and `b' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_a, &sizea, &aData );
   JNI_EXC_VAL  ( env, retArray )

   getVecGD_jni ( env, J_b, &sizeb, &bData );
   /*
   If an exception has been thrown, we must attempt to release
   the dynamic memory allocatd for the first input cell. 
   */
   if (  (*env)->ExceptionOccurred( env )  )
   {
      (*env)->ReleaseDoubleArrayElements(env, J_a, (jdouble *)aData, 0);

      return retArray;
   }
 

   /*
   `sizec' is the largest possible cell cardinality of the intersection
   of the input windows.
   */
   sizec  = sizea + sizeb - 2;

   /*
   At this point we can create dynamically allocated cells representing
   the input windows. We also allocate a cell to hold the output window.
   For now, the output cell has cardinality 0.
   */   
   a = zzalcell_c ( SPICE_DP, sizea, sizea, 0, aData );
   b = zzalcell_c ( SPICE_DP, sizeb, sizeb, 0, bData );
   c = zzalcell_c ( SPICE_DP, sizec, 0,     0, 0     );

   /*
   De-allocate the input data arrays since we've transferred the
   input data to cells `a' and `b'.
   */
   freeVecGD_jni ( env, J_a, aData );
   freeVecGD_jni ( env, J_b, bData );

   /*
   If we had a memory allocation error, we'll de-allocate the dynamically
   allocated cells before leaving. 
   */
   if ( failed_c() ) 
   {
      zzdacell_c ( a );
      zzdacell_c ( b );
      zzdacell_c ( c );

      SPICE_EXC_VAL ( env, caller, retArray );
   }


   /*
   Compute the intersection of the input windows.
   */
   wnintd_c ( a, b, c );


   /*
   De-allocate the input cells.
   */
   zzdacell_c ( a );
   zzdacell_c ( b );
 
   /*
   If the CSPICE call failed, de-allocate the output cell 
   for wnintd_c.
   */
   if ( failed_c() )
   {
      zzdacell_c ( c );

      SPICE_EXC_VAL ( env, caller, retArray );
   }

   /*
   Create a result array and transfer the contents of the window `c'
   to the result array. 
   */
   size  = card_c( c );

   cData = (SpiceDouble *) (c->data);

   createVecGD_jni ( env, size, cData, &retArray );

   /*
   Free the dynamically allocated output cell for wnintd_c.
   */
   zzdacell_c ( c );

   return retArray; 
}

 
 

/* 
Wrapper for CSPICE function wnreld_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jboolean JNICALL Java_spice_basic_CSPICE_wnreld
  (JNIEnv           * env, 
   jclass             cls,
   jdoubleArray       J_a,
   jstring            J_op,
   jdoubleArray       J_b     )
{ 
   /*
   Local variables 
   */
   #define OPLEN           11

   SpiceBoolean            retVal = 0;

   SpiceCell             * a;
   SpiceCell             * b;
   
   static SpiceChar      * caller = "CSPICE.wnreld";
   static SpiceChar        op  [ OPLEN ];

   SpiceDouble           * aData;
   SpiceDouble           * bData;

   SpiceInt                asize;
   SpiceInt                bsize;

   
   /*
   Capture the input string. 
   */
   getFixedInputString_jni ( env, J_op, OPLEN, op );

   JNI_EXC_VAL( env, ((jboolean)retVal) );

   /*
   Capture the contents of the input arrays `J_a' and `J_b' in 
   dynamic memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_a, &asize, &aData );
   getVecGD_jni ( env, J_b, &bsize, &bData );

   JNI_EXC_VAL( env, ((jboolean)retVal) ); 


   /*
   At this point we can create dynamically allocated cells representing
   the input windows.
   */   
   a = zzalcell_c ( SPICE_DP, asize, asize, 0, aData );
   b = zzalcell_c ( SPICE_DP, bsize, bsize, 0, bData );
  

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java arrays. 
   */
   freeVecGD_jni ( env, J_a, aData );
   freeVecGD_jni ( env, J_b, bData );

   JNI_EXC_VAL( env, ((jboolean)retVal) ); 


   /*
   Perform the comparison.
   */
   retVal = wnreld_c ( a, op, b );


   /*
   Free the dynamically allocated cells.
   */
   zzdacell_c ( a );
   zzdacell_c ( b );

   /*
   If the CSPICE call failed, throw an exception.
   */
   SPICE_EXC_VAL( env, caller, ((jboolean)retVal) );

   /*
   Normal return. 
   */
   return ( (jboolean)retVal );
}



/* 
Wrapper for CSPICE function wnsumd_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_wnsumd
  (JNIEnv           * env, 
   jclass             cls,
   jdoubleArray       J_window,
   jdoubleArray       J_meas,
   jdoubleArray       J_avg,
   jdoubleArray       J_stddev,
   jintArray          J_shortest,
   jintArray          J_longest   )
{ 
   /*
   Local variables 
   */
   SpiceCell             * window;
   
   static SpiceChar      * caller = "CSPICE.wnsumd";

   SpiceDouble             avg;
   SpiceDouble             meas;
   SpiceDouble             stddev;
   SpiceDouble           * windowData;

   SpiceInt                size;
   SpiceInt                shortest;
   SpiceInt                longest;
   

   /*
   Capture the contents of the input window in 
   dynamic memory.  Check out and return if an 
   exception is thrown.
   */
   getVecGD_jni ( env, J_window, &size, &windowData );

   JNI_EXC( env ); 

   /*
   At this point we can create a dynamically allocated cell representing
   the input window.
   */   
   window = zzalcell_c ( SPICE_DP, size, size, 0, windowData );  

   SPICE_EXC( env, caller );

   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array.
   */
   freeVecGD_jni ( env, J_window, windowData );

   JNI_EXC( env ); 


   /*
   Perform the summary.
   */
   wnsumd_c ( window, &meas, &avg, &stddev, &shortest, &longest );


   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   /*
   If the CSPICE call failed, throw an exception.
   */
   SPICE_EXC( env, caller );

   /*
   Set the output arguments. 
   */
   updateVecGD_jni ( env, 1, &meas,     J_meas     );
   updateVecGD_jni ( env, 1, &avg,      J_avg      );
   updateVecGD_jni ( env, 1, &stddev,   J_stddev   );
   updateVecGI_jni ( env, 1, &shortest, J_shortest );
   updateVecGI_jni ( env, 1, &longest,  J_longest  );
}



/* 
Wrapper for CSPICE function wnunid_c 

NOTE: the input and returned arrays have no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wnunid
  (JNIEnv * env, 
   jclass             J_class,
   jdoubleArray       J_a,
   jdoubleArray       J_b     )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   SpiceCell             * a;
   SpiceCell             * b;
   SpiceCell             * c;
   
   static SpiceChar      * caller   = "CSPICE.wnunid";

   SpiceDouble           * aData;
   SpiceDouble           * bData;
   SpiceDouble           * cData;

   SpiceInt                size;
   SpiceInt                sizea;
   SpiceInt                sizeb;
   SpiceInt                sizec;

 
   /*
   Capture the contents of the input arrays `a' and `b' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_a, &sizea, &aData );
   JNI_EXC_VAL  ( env, retArray )

   getVecGD_jni ( env, J_b, &sizeb, &bData );

   /*
   If an exception has been thrown, we must attempt to release
   the dynamic memory allocated for the first input cell. 
   */
   if (  (*env)->ExceptionOccurred( env )  )
   {
      (*env)->ReleaseDoubleArrayElements(env, J_a, (jdouble *)aData, 0);

      return retArray;
   }
 
   /*
   `sizec' is the largest possible cell cardinality of the union
   of the input windows.
   */
   sizec  = sizea + sizeb;

   /*
   At this point we can create dynamically allocated cells representing
   the input windows. We also allocate a cell to hold the output window.
   For now, the output cell has cardinality 0.
   */   
   a = zzalcell_c ( SPICE_DP, sizea, sizea, 0, aData );
   b = zzalcell_c ( SPICE_DP, sizeb, sizeb, 0, bData );
   c = zzalcell_c ( SPICE_DP, sizec, 0,     0, 0     );

   /*
   De-allocate the input data arrays since we've transferred the
   input data to cells `a' and `b'.
   */
   freeVecGD_jni ( env, J_a, aData );
   freeVecGD_jni ( env, J_b, bData );

   /*
   If we had a memory allocation error, we'll de-allocate the dynamically
   allocated cells before leaving. 
   */
   if ( failed_c() ) 
   {
      zzdacell_c ( a );
      zzdacell_c ( b );
      zzdacell_c ( c );

      SPICE_EXC_VAL ( env, caller, retArray );
   }
 

   /*
   Compute the union of the input windows.
   */
   wnunid_c ( a, b, c );


   /*
   De-allocate the input cells.
   */
   zzdacell_c ( a );
   zzdacell_c ( b );

   /*
   If the CSPICE call failed, de-allocate the output cell 
   for wnunid_c.
   */
   if ( failed_c() )
   {
      zzdacell_c ( c );

      SPICE_EXC_VAL ( env, caller, retArray );
   }

   /*
   Create a result array and transfer the contents of the window `c'
   to the result array. 
   */
   size  = card_c( c );

   cData = (SpiceDouble *) (c->data);

   createVecGD_jni ( env, size, cData, &retArray );

   /*
   Free the dynamically allocated output cell for wnunid_c.
   */
   zzdacell_c ( c );

   return retArray; 
}



/* 
Wrapper for CSPICE function wnvald_c 

NOTE: the input/output array has no control area.
*/
JNIEXPORT jdoubleArray JNICALL Java_spice_basic_CSPICE_wnvald
  (JNIEnv * env, 
   jclass             J_class,
   jint               J_size,
   jint               J_card,
   jdoubleArray       J_endpoints  )
{ 
   /*
   Local variables 
   */
   jdoubleArray            retArray = 0;

   /*
   The cell below will have its data array allocated
   at run time; its size will be set when it's known. 
   */
   SpiceCell             * window;

   static SpiceChar      * caller   = "CSPICE.wnvald";
   
   SpiceDouble           * outData;
   SpiceDouble           * endpointData;

   SpiceInt                insize;
   SpiceInt                outsize;


   /*
   Capture the contents of the input array `J_endpoints' in dynamic
   memory.  Check out and return if an exception is thrown.
   */
   getVecGD_jni ( env, J_endpoints, &insize, &endpointData );

   JNI_EXC_VAL ( env, retArray ); 

 
   /*
   `outsize' is the largest possible cardinality of the output window.
   */
   outsize  = insize + 2;

   /*
   At this point we can create a dynamically allocated cell representing
   the input window; this cell has enough room to hold the result
   of the insertion.
   */   
   window = zzalcell_c ( SPICE_DP, outsize, insize, 0, endpointData );

   SPICE_EXC_VAL( env, caller, retArray );
  
   /*
   Now that the input data have been captured, de-allocate the 
   dynamic memory used to capture the input Java array. 
   */
   freeVecGD_jni ( env, J_endpoints, endpointData );


   /*
   Validate the cell, thereby producing a SPICE window.
   */
   wnvald_c ( (SpiceInt)J_size, (SpiceInt)J_card, window );


   /*
   If the CSPICE call failed, de-allocate the cell.
   */
   if ( failed_c() )
   {
      zzdacell_c ( window );

      SPICE_EXC_VAL( env, caller, retArray );
   }

   /*
   Return the result in a new Java array.
   */
   outData = (SpiceDouble *) (window->data);

   createVecGD_jni ( env, window->card, outData, &retArray );

   /*
   Free the dynamically allocated cell.
   */
   zzdacell_c ( window );

   return retArray;
}



/* 
Wrapper for CSPICE function xf2eul_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_xf2eul
  (JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_xform, 
   jintArray          J_axes,
   jdoubleArray       J_angles,
   jbooleanArray      J_unique )
{
   /*
   Local variables  
   */
   SpiceBoolean            unique;

   static SpiceChar      * caller = "CSPICE.xf2eul";

   SpiceDouble             angles [6];
   SpiceDouble             xform  [6][6];

   SpiceInt                axes   [3];


   /*
   Get the input matrix in a one-dimensional C array. 
   */
   getVecFixedD_jni ( env, J_xform, 36, (SpiceDouble *)xform );

   JNI_EXC( env );

   /*
   Get the input axis sequence in an array as well. 
   */
   getVec3I_jni ( env, J_axes, axes );

   JNI_EXC( env );


   /*
   Make the CSPICE call. 
   */
   xf2eul_c ( xform, axes[0], axes[1], axes[2], angles, &unique );
   
   SPICE_EXC( env, caller );

  
   /*
   Update the output Euler state array. 
   */
   updateVecGD_jni ( env, 6, angles, J_angles );


   /*
   Update the output boolean "unique" array. 
   */
   updateVecGB_jni ( env, 1, &unique, J_unique );
}



/* 
Wrapper for CSPICE function xf2rav_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_xf2rav
  (JNIEnv           * env, 
   jclass             J_class, 
   jdoubleArray       J_xform, 
   jobjectArray       J_r,
   jdoubleArray       J_av )
{
   /*
   Local variables  
   */
   static SpiceChar      * caller = "CSPICE.xf2rav";

   SpiceDouble             av     [3];
   SpiceDouble             r      [3][3];
   SpiceDouble             xform  [6][6];


   /*
   Get the input matrix in a one-dimensional C array. 
   */
   getVecFixedD_jni ( env, J_xform, 36, (SpiceDouble *)xform );

   JNI_EXC( env );
 

   /*
   Make the CSPICE call. 
   */
   xf2rav_c ( xform, r, av );
   
   SPICE_EXC( env, caller );


   /*
   Update the output matrix. 
   */
   updateMat33D_jni ( env, CONST_MAT(r), J_r );

   /*
   Update the output angular velocity array. 
   */
   updateVec3D_jni ( env, av, J_av );
}


/* 
Wrapper for CSPICE function dascls_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dascls
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_handle ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.dascls";


   dascls_c ( (SpiceInt)J_handle );


   SPICE_EXC ( env, caller );
}




/* 
Wrapper for CSPICE function dasac_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dasac
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_handle,
    jobjectArray  J_buffer  )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.dasac";

   SpiceInt                fStrLen;
   SpiceInt                handle;
   SpiceInt                nStr;

   void                  * fStrArray;


   /*
   Fetch the input string buffer into a dynamically allocated
   array of Fortran-style strings. 
   */
   getFortranStringArray_jni ( env,   J_buffer, 
                               &nStr, &fStrLen, &fStrArray );
   JNI_EXC( env );


   /*
   Add the contents of the buffer to the DAF. 
   */
   
   handle = (SpiceInt) J_handle;

   dasac_ ( (integer *) &handle,
            (integer *) &nStr,
            (char    *) fStrArray,
            (ftnlen   ) fStrLen    );

   /*
   Always free the Fortran string array.
   */
   free ( fStrArray );

   SPICE_EXC( env, caller );

}




/* 
Wrapper for CSPICE function dasdc_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dasdc
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_handle ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller   = "CSPICE.dasdc";


   dasdc_c ( (SpiceInt)J_handle );


   SPICE_EXC ( env, caller );
}





JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dasec
  (JNIEnv           * env, 
   jclass             J_class, 
   jint               J_handle,
   jint               J_bufsiz,
   jint               J_lenout,
   jintArray          J_n,
   jobjectArray       J_buffer,
   jbooleanArray      J_done    )
{
   /*
   Local variables 
   */
   jstring                 bufferElt;

   SpiceBoolean            done;

   SpiceChar             * buffer;
   static SpiceChar      * caller = "CSPICE.dasec";
   SpiceChar             * strPtr;

   SpiceInt                bufsiz;
   SpiceInt                i;
   SpiceInt                lenout ;
   SpiceInt                n;
   SpiceInt                nBytes;


   /*
   Dynamically allocate an array of C strings to hold
   the indicated size block of comment data.
   */
   lenout =  (SpiceInt) J_lenout;
   bufsiz =  (SpiceInt) J_bufsiz;

   if ( bufsiz == 0  )
   {
      n    = 0;
      done = SPICEFALSE;

      updateVecGI_jni ( env, 1, &n,    J_n    );
      updateVecGB_jni ( env, 1, &done, J_done );      

      return;
   }

   nBytes = bufsiz * lenout * sizeof(SpiceChar);

   buffer = (void *)alloc_SpiceMemory ( (size_t)nBytes );

   SPICE_EXC( env, caller );


   /*
   Fetch the comment area data that fits in a buffer of
   the specified dimensions.
   */
   dasec_c ( (SpiceInt) J_handle,
             bufsiz,
             lenout,
             &n,
             buffer,
             &done               );


   /*
   Set the output arguments. 

   The first step will be to create a Java string for
   each element of `buffer' and update the corresponding
   elements of J_buffer with these Java strings.
   */
   for ( i = 0;  i < n;  i++ )
   {
      strPtr    = buffer + ( i * lenout );

      bufferElt = createJavaString_jni ( env, strPtr );

      (*env)->SetObjectArrayElement ( env, J_buffer, i, bufferElt );
   }

   /*
   Always de-allocate the buffer of C-style strings. 
   */
   free_SpiceMemory ( (void *)  buffer );

   /*
   Handle any SPICE error that may have occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Update the other outputs.
   */
   updateVecGI_jni ( env, 1, &n,    J_n    );
   updateVecGB_jni ( env, 1, &done, J_done );
}




/* 
Wrapper for CSPICE function dashfn_c 
*/
JNIEXPORT jstring JNICALL Java_spice_basic_CSPICE_dashfn
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jint          J_handle ) 
{
   /*
   Local variables 
   */
   jstring                 retName  = 0;

   static SpiceChar      * caller   = "CSPICE.dashfn";
   static SpiceChar        fname [ FNAMLN ];


   dashfn_c ( (SpiceInt)J_handle, FNAMLN, fname );

   /*
   Note that control does NOT return to the caller when     
   an exception is thrown from C code; hence we return
   a null pointer if we throw an exception.
   */
   SPICE_EXC_VAL( env, caller, retName );

   /*
   Normal return. 
   */
   return ( createJavaString_jni( env, fname )  );
}


/* 
Wrapper for CSPICE function dasopr_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_dasopr
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_fname ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.dasopr";
   static SpiceChar      * fname;

   SpiceInt                fileLen;
   SpiceInt                handle  = 0;


   /*
   Fetch input string into dynmically allocated memory. 
   Check for a JNI exception.
   */
   getVarInputString_jni ( env, J_fname, &fileLen, &fname );

   JNI_EXC_VAL( env, ((jint)handle) );


   /*
   Open the file. 
   */
   dasopr_c ( fname, &handle );


   /*
   Free the dynamically allocated memory for the file name.
   */
   freeVarInputString_jni ( env, J_fname, fname );

   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jint)handle) );
   
   return ( (jint) handle );
}




/* 
Wrapper for CSPICE function dasopw_c 
*/
JNIEXPORT jint JNICALL Java_spice_basic_CSPICE_dasopw
  ( JNIEnv     *  env, 
    jclass        J_class, 
    jstring       J_fname ) 
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "CSPICE.dasopw";
   static SpiceChar      * fname;

   SpiceInt                fileLen;
   SpiceInt                handle  = 0;


   /*
   Fetch input string into dynmically allocated memory. 
   Check for a JNI exception.
   */
   getVarInputString_jni ( env, J_fname, &fileLen, &fname );

   JNI_EXC_VAL( env, ((jint)handle) );


   /*
   Open the file. 
   */
   dasopw_c ( fname, &handle );


   /*
   Free the dynamically allocated memory for the file name.
   */
   freeVarInputString_jni ( env, J_fname, fname );

   /*
   Check for a SPICE error and throw an exception if one
   occurred. 
   */
   SPICE_EXC_VAL( env, caller, ((jint)handle) );
   
   return ( (jint) handle );
}



/* 
Wrapper for CSPICE function dasrfr_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dasrfr
  (JNIEnv           * env, 
   jclass             J_class,
   jint               J_handle,
   jobjectArray       J_idword,
   jobjectArray       J_ifname,
   jintArray          J_nresvr,
   jintArray          J_nresvc, 
   jintArray          J_ncomr,
   jintArray          J_ncomc   )
{
   /*
   Local variables 
   */
   jstring                 jIDWord;
   jstring                 jIfname;
 
   static SpiceChar      * caller = "CSPICE.dasrfr";
   static SpiceChar        ifname [ IFNLEN ];
   static SpiceChar        idword [ IDWLEN ];

   SpiceInt                ncomc;
   SpiceInt                ncomr;
   SpiceInt                nresvc;
   SpiceInt                nresvr;


   /*
   Fetch the requested file record, if possible. 
   */
   dasrfr_c ( (SpiceInt)J_handle,  IDWLEN,  IFNLEN,
              idword,              ifname,  &nresvr,
              &nresvc,             &ncomr,  &ncomc  );

   SPICE_EXC( env, caller );

   /*
   Set the numeric output arguments. Each argument is an array whose 
   elements will be updated. 
   */  
   updateVecGI_jni ( env, 1, &nresvr, J_nresvr );
   updateVecGI_jni ( env, 1, &nresvc, J_nresvc );
   updateVecGI_jni ( env, 1, &ncomr,  J_ncomr  );
   updateVecGI_jni ( env, 1, &ncomc,  J_ncomc  );

   JNI_EXC( env );

   /*
   Create a new Java string to hold the internal file name. 
   */
   jIfname = createJavaString_jni ( env, ifname );

   JNI_EXC  ( env );
   SPICE_EXC( env, caller );

   /*
   Create a new Java string to hold the ID word.
   */
   jIDWord = createJavaString_jni ( env, idword );

   /*
   Update the first element of J_ifname with a reference
   to jIfname.
   */
   (*env)->SetObjectArrayElement ( env, J_ifname, 0, jIfname );
   JNI_EXC( env );

   /*
   Update the first element of J_idword with a reference
   to jIDWord.
   */
   (*env)->SetObjectArrayElement ( env, J_idword, 0, jIDWord );
   JNI_EXC( env );

}




/* 
Wrapper for CSPICE function dlabbs_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dlabbs
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_descr, 
   jbooleanArray     J_found )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dlabbs";

   SpiceBoolean            found;
   SpiceDLADescr           dladsc;
   SpiceInt                dladscArray [SPICE_DLA_DSCSIZ];



   /*
   Find the DLA descriptor of the last DLA segment.
   */
   dlabbs_c ( (SpiceInt) J_handle, &dladsc, &found );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Copy the descriptor contents to the output descriptor array.
   */
   dladscArray[SPICE_DLA_BWDIDX] = dladsc.bwdptr;
   dladscArray[SPICE_DLA_FWDIDX] = dladsc.fwdptr;
   dladscArray[SPICE_DLA_IBSIDX] = dladsc.ibase;
   dladscArray[SPICE_DLA_ISZIDX] = dladsc.isize;
   dladscArray[SPICE_DLA_DBSIDX] = dladsc.dbase;
   dladscArray[SPICE_DLA_DSZIDX] = dladsc.dsize;
   dladscArray[SPICE_DLA_CBSIDX] = dladsc.cbase;
   dladscArray[SPICE_DLA_CSZIDX] = dladsc.csize;

   /*
   Set the output arguments. 
   */
   updateVecGI_jni ( env, SPICE_DLA_DSCSIZ, dladscArray,  J_descr );
   updateVecGB_jni ( env, 1,                &found,       J_found );
}



/* 
Wrapper for CSPICE function dlabfs_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dlabfs
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_descr, 
   jbooleanArray     J_found )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dlabfs";

   SpiceBoolean            found;
   SpiceDLADescr           dladsc;
   SpiceInt                dladscArray [SPICE_DLA_DSCSIZ];



   /*
   Find the DLA descriptor of the first DLA segment.
   */
   dlabfs_c ( (SpiceInt) J_handle, &dladsc, &found );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Copy the descriptor contents to the output descriptor array.
   */
   dladscArray[SPICE_DLA_BWDIDX] = dladsc.bwdptr;
   dladscArray[SPICE_DLA_FWDIDX] = dladsc.fwdptr;
   dladscArray[SPICE_DLA_IBSIDX] = dladsc.ibase;
   dladscArray[SPICE_DLA_ISZIDX] = dladsc.isize;
   dladscArray[SPICE_DLA_DBSIDX] = dladsc.dbase;
   dladscArray[SPICE_DLA_DSZIDX] = dladsc.dsize;
   dladscArray[SPICE_DLA_CBSIDX] = dladsc.cbase;
   dladscArray[SPICE_DLA_CSZIDX] = dladsc.csize;

   /*
   Set the output arguments. 
   */
   updateVecGI_jni ( env, SPICE_DLA_DSCSIZ, dladscArray,  J_descr );
   updateVecGB_jni ( env, 1,                &found,       J_found );
}



/* 
Wrapper for CSPICE function dlafns_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dlafns
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_descr, 
   jintArray         J_nxtdsc, 
   jbooleanArray     J_found  )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dlafns";

   SpiceBoolean            found;
   SpiceDLADescr           dladsc;
   SpiceDLADescr           nxtdsc;
   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];

   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_descr, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC( env );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC( env, caller );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Find the DLA descriptor of the next DLA segment.
   */
   dlafns_c ( (SpiceInt) J_handle, &dladsc, &nxtdsc, &found );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Copy the output descriptor contents to the descriptor array.
   */
   DLADescrArray[SPICE_DLA_BWDIDX] = nxtdsc.bwdptr;
   DLADescrArray[SPICE_DLA_FWDIDX] = nxtdsc.fwdptr;
   DLADescrArray[SPICE_DLA_IBSIDX] = nxtdsc.ibase;
   DLADescrArray[SPICE_DLA_ISZIDX] = nxtdsc.isize;
   DLADescrArray[SPICE_DLA_DBSIDX] = nxtdsc.dbase;
   DLADescrArray[SPICE_DLA_DSZIDX] = nxtdsc.dsize;
   DLADescrArray[SPICE_DLA_CBSIDX] = nxtdsc.cbase;
   DLADescrArray[SPICE_DLA_CSZIDX] = nxtdsc.csize;

   /*
   Set the output arguments. 
   */
   updateVecGI_jni ( env, SPICE_DLA_DSCSIZ, DLADescrArray,  J_nxtdsc );
   updateVecGB_jni ( env, 1,                &found,         J_found  );
}



/* 
Wrapper for CSPICE function dlafps_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_dlafps
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_descr, 
   jintArray         J_prvdsc, 
   jbooleanArray     J_found  )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.dlafps";

   SpiceBoolean            found;
   SpiceDLADescr           dladsc;
   SpiceDLADescr           prvdsc;
   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];

   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_descr, SPICE_DLA_DSCSIZ, DLADescrArray );
   JNI_EXC( env );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC( env, caller );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Find the DLA descriptor of the preceding DLA segment.
   */
   dlafps_c ( (SpiceInt) J_handle, &dladsc, &prvdsc, &found );
              
   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Copy the output descriptor contents to the descriptor array.
   */
   DLADescrArray[SPICE_DLA_BWDIDX] = prvdsc.bwdptr;
   DLADescrArray[SPICE_DLA_FWDIDX] = prvdsc.fwdptr;
   DLADescrArray[SPICE_DLA_IBSIDX] = prvdsc.ibase;
   DLADescrArray[SPICE_DLA_ISZIDX] = prvdsc.isize;
   DLADescrArray[SPICE_DLA_DBSIDX] = prvdsc.dbase;
   DLADescrArray[SPICE_DLA_DSZIDX] = prvdsc.dsize;
   DLADescrArray[SPICE_DLA_CBSIDX] = prvdsc.cbase;
   DLADescrArray[SPICE_DLA_CSZIDX] = prvdsc.csize;

   /*
   Set the output arguments. 
   */
   updateVecGI_jni ( env, SPICE_DLA_DSCSIZ, DLADescrArray,  J_prvdsc );
   updateVecGB_jni ( env, 1,                &found,         J_found  );
}




/* 
Wrapper for CSPICE function illumPl02
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_illumPl02
(  JNIEnv           * env, 
   jclass             J_class,
   jint               J_handle, 
   jintArray          J_dladsc, 
   jstring            J_target, 
   jdouble            J_et,
   jstring            J_abcorr,
   jstring            J_obsrvr,
   jdoubleArray       J_spoint, 
   jdoubleArray       J_phase,
   jdoubleArray       J_solar,
   jdoubleArray       J_emissn )
{
   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static SpiceChar      * caller  = "CSPICE.illumPl02";
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDLADescr           dladsc;
   
   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];
   SpiceDouble             phase;
   SpiceDouble             solar;
   SpiceDouble             emissn;
  
   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;


   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );

   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr  );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr  );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target  );
   getVec3D_jni            ( env, J_spoint,             spoint  );

   JNI_EXC( env );


   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   handle = (SpiceInt   ) J_handle;
   et     = (SpiceDouble) J_et;

   
   illum_pl02 ( handle,  &dladsc,  target,  et,      abcorr,   
                obsrvr,  spoint,   &phase,  &solar,  &emissn );


   /*
   Handle any SPICE exception that may have occurred. 
   */
   SPICE_EXC( env, caller );

   /*
   Set the values of our output arrays.
   */
   updateVecGD_jni ( env, 1, &phase,  J_phase  );
   updateVecGD_jni ( env, 1, &solar,  J_solar  );
   updateVecGD_jni ( env, 1, &emissn, J_emissn );
}



/* 
Wrapper for CSPICE function limbPl02 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_limbPl02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jstring           J_target,
   jdouble           J_et,
   jstring           J_fixfrm,
   jstring           J_abcorr,
   jstring           J_obsrvr,
   jint              J_npoints,
   jdoubleArray      J_trgepc,
   jdoubleArray      J_obspos,
   jobjectArray      J_limbpts,
   jintArray         J_plateIDs )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static ConstSpiceChar * caller = "CSPICE.limbPl02";
   static SpiceChar        fixfrm  [ FRNMLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDLADescr           dladsc;

   SpiceDouble             et;
   SpiceDouble             obspos [3];
   SpiceDouble             trgepc;
   SpiceDouble         ( * limbPtr )[3];

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;
   SpiceInt              * plateIDPtr;
   SpiceInt                npoints;

   /*
   Capture scalar inputs in local C variables.
   */
   handle  = (SpiceInt   ) J_handle;
   et      = (SpiceDouble) J_et;
   npoints = (SpiceInt   ) J_npoints;

   /*
   Check npoints.
   */
   if ( npoints < 1 )
   {
      setmsg_c ( "The requested number of limb points must be "
                 "positive but was #."                          );
      errint_c ( "#", npoints                                   );
      sigerr_c ( "SPICE(INVALIDCOUNT)"                          );

      SPICE_EXC( env, caller );
   }


   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr );
   getFixedInputString_jni ( env, J_fixfrm, FRNMLN,     fixfrm );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target );

   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC( env, caller );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];


   /*
   We're going to fetch at least one limb point. Allocate
   dynamic memory in which to store the data.
   */
   limbPtr = ( SpiceDouble(*)[3] )alloc_SpiceDouble_C_array( npoints, 3 );

   SPICE_EXC( env, caller );

   plateIDPtr = alloc_SpiceInt_C_array( npoints, 1 );

   if ( failed_c() )
   {
      /*
      We must free the limb point array before throwing
      an exception. 
      */
      free_SpiceMemory( (void *)  limbPtr );

      SPICE_EXC( env, caller );
   }

   /*
   At this point we have allocated the arrays we need in order
   to capture the outputs of limb_pl02. 
   */      
   limb_pl02( handle,  &dladsc, target,  et,      
              fixfrm,  abcorr,  obsrvr,  npoints, 
              &trgepc, obspos,  limbPtr, plateIDPtr );

   if ( !failed_c() )
   {
      /*
      Update output arrays. 
      */
      updateVecGD_jni ( env, 1,          &trgepc,    J_trgepc   );
      updateVec3D_jni ( env,             obspos,     J_obspos   );
      updateMatGD_jni ( env, npoints, 3, limbPtr,    J_limbpts  );
      updateVecGI_jni ( env, npoints,    plateIDPtr, J_plateIDs );
   }

   /*
   Always free the dynamically allocated arrays. 
   */
   free_SpiceMemory( (void *)  limbPtr    );
   free_SpiceMemory( (void *)  plateIDPtr );
   

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

}

 


/* 
Wrapper for CSPICE function llgridPl02 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_llgridPl02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jobjectArray      J_grid,
   jobjectArray      J_spoints,
   jintArray         J_plateIDs )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static ConstSpiceChar * caller = "CSPICE.llgridPl02";

   SpiceDLADescr           dladsc;

   ConstSpiceDouble    ( * gridPtr )[2] = 0;
   SpiceDouble         ( * surfPtr )[3] = 0;

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;
   SpiceInt              * plateIDPtr;
   SpiceInt                ncols;
   SpiceInt                nrows;
   SpiceInt                npoints;


   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC( env, caller );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];

   /*
   Fetch the input coordinate grid into a dynamically allocated
   array. This array must always be freed via free_SpiceMemory 
   prior to exit. 
   */
   getMatGD_jni( env, J_grid, &nrows, &ncols, (SpiceDouble **)&gridPtr );

   SPICE_EXC( env, caller );

   /*
   Make sure the dimensions of the input grid are sensible. 
   The column count must be 2.
   */
   if ( ncols != 2 )
   {
      setmsg_c ( "The input grid has dimensions #x#. The "
                 "column count must be 2."                   );
      errint_c ( "#", nrows                                  );
      errint_c ( "#", ncols                                  );
      sigerr_c ( "SPICE(INVALIDCOUNT)"                       );

      SPICE_EXC( env, caller );
   }

   /*
   Capture scalar inputs in local C variables.
   */
   handle  = (SpiceInt) J_handle;

   /*
   Each input coordinate corresponds to a row of the input
   array. 
   */
   npoints = nrows;

   /*
   If we're going to compute at least one surface point, allocate
   dynamic memory in which to store the data.
   */
   if ( npoints > 0 )
   {
      surfPtr = ( SpiceDouble(*)[3] )alloc_SpiceDouble_C_array( npoints, 3 );

      SPICE_EXC( env, caller );
 
      plateIDPtr = alloc_SpiceInt_C_array( npoints, 1 );

      if ( failed_c() )
      {
         /*
         We must free the grid and surface point arrays before throwing
         an exception. 
         */
         free_SpiceMemory( (void *)  gridPtr );
         free_SpiceMemory( (void *)  surfPtr );

         SPICE_EXC( env, caller );
      }

      /*
      At this point we have allocated the arrays we need in order
      to capture the outputs of limb_pl02. 
      */      
      llgrid_pl02( handle,  &dladsc, npoints,
                   gridPtr, surfPtr, plateIDPtr );

      /*
      We're done with the input grid pointer.
      */
      free_SpiceMemory( (void *)  gridPtr );

      if ( !failed_c() )
      {
         /*
         Update output arrays. 
         */
         updateMatGD_jni ( env, npoints, 3, surfPtr,    J_spoints  );
         updateVecGI_jni ( env, npoints,    plateIDPtr, J_plateIDs );
      }

      /*
      Always free the dynamically allocated arrays. 
      */
      free_SpiceMemory( (void *)  surfPtr    );
      free_SpiceMemory( (void *)  plateIDPtr );
   }

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );
}




/* 
Wrapper for CSPICE function subptPl02_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_subptPl02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jstring           J_method,
   jstring           J_target,
   jdouble           J_et,
   jstring           J_abcorr,
   jstring           J_obsrvr,
   jdoubleArray      J_spoint, 
   jdoubleArray      J_alt, 
   jintArray         J_plid    )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static ConstSpiceChar * caller = "CSPICE.subptPl02";
   SpiceChar             * method;
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDLADescr           dladsc;

   SpiceDouble             alt;
   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;
   SpiceInt                methodLen;
   SpiceInt                plid;


   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );

   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target );
   getVarInputString_jni   ( env, J_method, &methodLen, &method );

   JNI_EXC( env );


   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];


   /*
   Find the intercept if it exists.
   */

   handle = (SpiceInt   ) J_handle;
   et     = (SpiceDouble) J_et;


   subpt_pl02 ( handle, &dladsc, method, target, et,
                abcorr, obsrvr,  spoint, &alt,   &plid );          

   /*
   Regardless of whether a SPICE error occurred, free the 
   dynamically allocated memory here. 
   */
   freeVarInputString_jni ( env, J_method, method );

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Set the output arguments. 
   */
   updateVec3D_jni ( env,      spoint,  J_spoint );
   updateVecGD_jni ( env,  1,  &alt,    J_alt    );
   updateVecGI_jni ( env,  1,  &plid,   J_plid   );
}



/* 
Wrapper for CSPICE function subsolPl02_c 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_subsolPl02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jstring           J_method,
   jstring           J_target,
   jdouble           J_et,
   jstring           J_abcorr,
   jstring           J_obsrvr,
   jdoubleArray      J_spoint, 
   jdoubleArray      J_alt, 
   jintArray         J_plid    )
{
   /*
   Constants 
   */

   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static ConstSpiceChar * caller = "CSPICE.subsolPl02";
   SpiceChar             * method;
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];

   SpiceDLADescr           dladsc;

   SpiceDouble             alt;
   SpiceDouble             et;
   SpiceDouble             spoint  [ 3 ];

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;
   SpiceInt                methodLen;
   SpiceInt                plid;


   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );

   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN,     abcorr );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN,     obsrvr );
   getFixedInputString_jni ( env, J_target, BDNMLN,     target );
   getVarInputString_jni   ( env, J_method, &methodLen, &method );

   JNI_EXC( env );


   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];


   /*
   Find the intercept if it exists.
   */

   handle = (SpiceInt   ) J_handle;
   et     = (SpiceDouble) J_et;


   subsol_pl02 ( handle, &dladsc, method, target, et,
                 abcorr, obsrvr,  spoint, &alt,   &plid );          

   /*
   Regardless of whether a SPICE error occurred, free the 
   dynamically allocated memory here. 
   */
   freeVarInputString_jni ( env, J_method, method );

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );

   /*
   Set the output arguments. 
   */
   updateVec3D_jni ( env,      spoint,  J_spoint );
   updateVecGD_jni ( env,  1,  &alt,    J_alt    );
   updateVecGI_jni ( env,  1,  &plid,   J_plid   );
}



/* 
Wrapper for CSPICE function termPl02 
*/
JNIEXPORT void JNICALL Java_spice_basic_CSPICE_termPl02
  (JNIEnv          * env, 
   jclass            J_class, 
   jint              J_handle, 
   jintArray         J_dladsc, 
   jstring           J_trmtyp,
   jstring           J_source,
   jstring           J_target,
   jdouble           J_et,
   jstring           J_fixfrm,
   jstring           J_abcorr,
   jstring           J_obsrvr,
   jint              J_npoints,
   jdoubleArray      J_trgepc,
   jdoubleArray      J_obspos,
   jobjectArray      J_trmpts,
   jintArray         J_plateIDs )
{
   /*
   Constants 
   */

   #define TMTPLN          80

   /*
   Local variables 
   */
   static SpiceChar        abcorr  [ CORRLN ];
   static ConstSpiceChar * caller = "CSPICE.termPl02";
   static SpiceChar        fixfrm  [ FRNMLN ];
   static SpiceChar        obsrvr  [ BDNMLN ];
   static SpiceChar        source  [ BDNMLN ];
   static SpiceChar        target  [ BDNMLN ];
   static SpiceChar        trmtyp  [ TMTPLN ];

   SpiceDLADescr           dladsc;

   SpiceDouble             et;
   SpiceDouble             obspos [3];
   SpiceDouble             trgepc;
   SpiceDouble         ( * trmPtr )[3];

   SpiceInt                DLADescrArray [SPICE_DLA_DSCSIZ];
   SpiceInt                handle;
   SpiceInt              * plateIDPtr;
   SpiceInt                npoints;


   /*
   Capture scalar inputs in local C variables.
   */
   handle  = (SpiceInt   ) J_handle;
   et      = (SpiceDouble) J_et;
   npoints = (SpiceInt   ) J_npoints;

   /*
   Check npoints.
   */
   if ( npoints < 1 )
   {
      setmsg_c ( "The requested number of terminator points "
                 "must be positive but was #."               );
      errint_c ( "#", npoints                                );
      sigerr_c ( "SPICE(INVALIDCOUNT)"                       );

      SPICE_EXC( env, caller );
   }

   /*
   Capture the input strings in local buffers. The method
   length is unknown; use a dynamically allocated buffer for it.
   */
   getFixedInputString_jni ( env, J_abcorr, CORRLN, abcorr );
   getFixedInputString_jni ( env, J_fixfrm, FRNMLN, fixfrm );
   getFixedInputString_jni ( env, J_obsrvr, BDNMLN, obsrvr );
   getFixedInputString_jni ( env, J_source, BDNMLN, source );
   getFixedInputString_jni ( env, J_target, BDNMLN, target );
   getFixedInputString_jni ( env, J_trmtyp, TMTPLN, trmtyp );

   /*
   Capture the input Java descriptor array in a C array. 
   */
   getVecFixedI_jni ( env, J_dladsc, SPICE_DLA_DSCSIZ, DLADescrArray );

   /*
   Handle any SPICE error. 
   */ 
   SPICE_EXC( env, caller );

   /*
   Copy the input descriptor array contents to the input descriptor.
   */
   dladsc.bwdptr = DLADescrArray[SPICE_DLA_BWDIDX];
   dladsc.fwdptr = DLADescrArray[SPICE_DLA_FWDIDX];
   dladsc.ibase  = DLADescrArray[SPICE_DLA_IBSIDX];
   dladsc.isize  = DLADescrArray[SPICE_DLA_ISZIDX];
   dladsc.dbase  = DLADescrArray[SPICE_DLA_DBSIDX];
   dladsc.dsize  = DLADescrArray[SPICE_DLA_DSZIDX];
   dladsc.cbase  = DLADescrArray[SPICE_DLA_CBSIDX];
   dladsc.csize  = DLADescrArray[SPICE_DLA_CSZIDX];


   /*
   If we're going to fetch at least one terminator point, allocate
   dynamic memory in which to store the data.
   */
   if ( npoints > 0 )
   {
      trmPtr = ( SpiceDouble(*)[3] )alloc_SpiceDouble_C_array( npoints, 3 );

      SPICE_EXC( env, caller );
 
      plateIDPtr = alloc_SpiceInt_C_array( npoints, 1 );

      if ( failed_c() )
      {
         /*
         We must free the terminator point array before throwing
         an exception. 
         */
         free_SpiceMemory( (void *)  trmPtr );

         SPICE_EXC( env, caller );
      }

      /*
      At this point we have allocated the arrays we need in order
      to capture the outputs of term_pl02. 
      */      
      term_pl02( handle,  &dladsc, trmtyp,  source,    target,        
                 et,      fixfrm,  abcorr,  obsrvr,    npoints, 
                 &trgepc, obspos,  trmPtr,  plateIDPtr         );

      if ( !failed_c() )
      {
         /*
         Update output arrays. 
         */
         updateVecGD_jni ( env, 1,          &trgepc,    J_trgepc   );
         updateVec3D_jni ( env,             obspos,     J_obspos   );
         updateMatGD_jni ( env, npoints, 3, trmPtr,     J_trmpts   );
         updateVecGI_jni ( env, npoints,    plateIDPtr, J_plateIDs );
      }

      /*
      Always free the dynamically allocated arrays. 
      */
      free_SpiceMemory( (void *)  trmPtr     );
      free_SpiceMemory( (void *)  plateIDPtr );
   }

   /*
   Handle any SPICE error. 
   */
   SPICE_EXC( env, caller );
}

/*
End of file cspice_jni.c 
*/
