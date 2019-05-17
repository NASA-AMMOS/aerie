/*

-Procedure zzutils_jni ( Supporting utilities for cspice_jni )

-Abstract

   This set of utilities supports communication between Java
   and C within the CSPICE native methods implemented in
   cspice_jni.c

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

   None.

-Keywords

   Java
   JNI

*/


/*

-Brief_I/O

   VARIABLE  I/O  DESCRIPTION
   --------  ---  --------------------------------------------------
   See the routines below.
   

-Detailed_Input

   See the routines below.

-Detailed_Output

   See the routines below.

-Parameters

   None.

-Exceptions

   See the routines below.

-Files

   None.

-Particulars

   None.

-Examples

   See usage in cspice_jni.

-Restrictions

   For use only by JNISpice native routines.

-Literature_References

   None.

-Author_and_Institution

   N.J. Bachman  (JPL)

-Version

   -JNISpice Version 3.0.0, 17-NOV-2016 (NJB)

       Added new routine
     
          createVecGB_jni

       This routine creates an array of boolean values for output.

       Added output array size checks for the array update routines

          updateMat33D_jni
          updateMatGD_jni
          updateVec3D_jni
          updateVecGB_jni
          updateVecGD_jni
          updateVecGI_jni

       Modified order of JNI error checks in

          getVecFixedI_jni


   -JNISpice Version 2.0.0, 16-JUN-2014 (NJB)

       Added general-dimension matrix input routine getMatGD_jni.

   -JNISpice Version 1.0.0, 03-DEC-2009 (NJB)

-Index_Entries

   TBD

-&
*/

#include <jni.h>
#include <string.h>
#include "SpiceUsr.h"
#include "SpiceZst.h"
#include "zzalloc.h"
#include "cspice_jni.h"

 


/*
Return a jstring representing an output C string. 
*/
jstring createJavaString_jni ( JNIEnv       * env,
                               SpiceChar    * CString )
{

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC_VAL  ( env,  (jstring)0 );


   return (  (*env)->NewStringUTF ( env, CString )  );
}


/*
Return an array of jstrings representing an array of 
Fortran-style strings. The input strings are not 
null-terminated.

If the input array is dynamically allocated, the caller must
free it.
*/
jobjectArray createJavaStringArray_jni ( JNIEnv       * env,
                                         SpiceInt       nStr,
                                         SpiceInt       fStrLen,
                                         void         * fStrArray )
{
   /*
   Local variables 
   */
   jobjectArray            resultArray = 0;
   jstring                 jStr        = 0;

   static SpiceChar      * caller      = "createJavaStringArray_jni";
   SpiceChar             * CStrPtr     = 0;
   SpiceChar             * sPtr        = 0;

   SpiceInt                i;

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC_VAL  ( env,  (jstring)0 );

   /*
   Check for exceptions and SPICE errors; return if any are 
   found. 
   */
   JNI_EXC_VAL  ( env,          resultArray );
   SPICE_EXC_VAL( env, caller,  resultArray );

   /*
   Prepare a string array to return to the caller.  

   First create the array of objects, then assign each element to
   point to a String object containing a component of the kernel
   variable.
   */
   resultArray = 

     (*env)->NewObjectArray( env, 
                             nStr, 
                             (*env)->FindClass( env, "java/lang/String" ),
                             (*env)->NewStringUTF( env, "" )               );

   JNI_EXC_VAL  ( env,  resultArray );

  
   for ( i = 0;  i < nStr;  i++ )
   {
      /*
      Let `sPtr' point to the start of the ith Fortran string. 
      */
      sPtr = (SpiceChar *)fStrArray + (i * fStrLen);

      /*
      Create a C-style string from the Fortran string.
      */
      F2C_CreateStr_Sig ( fStrLen, sPtr, &CStrPtr );

      SPICE_EXC_VAL( env, caller, resultArray );

      /*
      Create a Java string from the C string. 
      */
      jStr  = (jstring) ( (*env)->NewStringUTF ( env, CStrPtr ) );

      /*
      Insert a reference to the Java string into the ith element
      of the output array. 
      */
      (*env)->SetObjectArrayElement ( env, resultArray, i, jStr );
      
      JNI_EXC_VAL ( env, resultArray );

      /*
      Free the C string. 
      */
      free ( CStrPtr );
   }


   return ( resultArray );
}





/*
Create a new 3x3 d.p. matrix to be returned by a native method.
*/

void createMat33D_jni ( JNIEnv            * env,
                        ConstSpiceDouble    CMat [3][3],
                        jobjectArray      * jRetMat      )
{
   /*
   Local variables 
   */
   jdoubleArray            row;
   SpiceInt                i;


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );

   /*
   First create the array of objects, then assign each element to
   point to a jdoubleArray object containing a row of the matrix.

   The class name of the objects is the Java symbol for "array of
   doubles", which is "[D".
   */

   *jRetMat = 

     (*env)->NewObjectArray( env, 
                             3, 
                             (*env)->FindClass( env, "[D" ),
                             (*env)->NewDoubleArray(env,3)   );
   JNI_EXC( env );


   for ( i = 0;  i < 3;  i++ )
   {
      /*
      Create the ith row of the output matrix; the row 
      is an array of jdoubles. 
      */
      row = (jdoubleArray) ( (*env)->NewDoubleArray(env, 3) );
      JNI_EXC( env );


      /*
      Fill in the ith row of the output matrix, using the ith
      row of `CMat'.
      */
      (*env)->SetDoubleArrayRegion ( env, 
                                     (jdoubleArray) row, 
                                     (jsize)0, 
                                     3, 
                                     (jdouble *) CMat[i] );
      JNI_EXC( env );


      /*
      Insert the ith row into the output matrix; this matrix
      is an array of jobjects. 
      */
      (*env)->SetObjectArrayElement ( env, *jRetMat, i, row );
      JNI_EXC( env );

   }

   /*
   At this point *jRetMat has been filled in.
   */
}







/*
Create a new d.p. 3-vector to be returned by a native method.
*/

void createVec3D_jni ( JNIEnv            * env,
                       ConstSpiceDouble    CVec [3],
                       jdoubleArray      * jVec       )
{
   /*
   Local variables 
   */

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );

 
   /*
   Create the output vector; the vector is an array of jdoubles. 
   */

   *jVec = (jdoubleArray) ( (*env)->NewDoubleArray(env, 3) );
   JNI_EXC( env );


   /*
   Fill in the output vector using CVec.
   */
   (*env)->SetDoubleArrayRegion ( env, 
                                  (jdoubleArray) *jVec, 
                                  (jsize)0, 
                                  3, 
                                  (jdouble *) CVec );
   JNI_EXC( env );

   /*
   At this point *jVec has been filled in.
   */
}




/*
Create a new boolean general-dimension vector to be 
returned by a native method.

NOTE: this code assumes that the jboolean and SpiceBoolean types
have the same size!
*/

void createVecGB_jni ( JNIEnv             * env,
                       SpiceInt             size,
                       ConstSpiceBoolean  * CVec,
                       jbooleanArray      * jVec )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "createVecGB_jni";


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( sizeof(jdouble) != sizeof(SpiceDouble) )
   {
      chkin_c  ( "createVecGB"                                  );
      setmsg_c ( "SpiceDouble has size # bytes; this size is "
                 "incompatible with that of type jdouble, "
                 "which is # bytes."                            );
      errint_c ( "#", (SpiceInt)(sizeof(SpiceDouble))           );
      errint_c ( "#", (SpiceInt)(sizeof(jdouble))               );
      sigerr_c ( "SPICE(BUG)"                                   );
      chkout_c ( "createVecGB"                                  );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }


   /*
   Create the output vector; the vector is an array of jbooleans. 
   */

   *jVec = (jbooleanArray) ( (*env)->NewBooleanArray(env, (jsize)size) );
   JNI_EXC( env );


   /*
   Fill in the output vector using CVec.
   */
   (*env)->SetBooleanArrayRegion ( env, 
                                  (jbooleanArray) *jVec, 
                                  (jsize        ) 0, 
                                  (jsize        ) size, 
                                  (jboolean    *) CVec  );
   JNI_EXC( env );

   /*
   At this point *jVec has been filled in.
   */
}





/*
Create a new d.p. general-dimension vector to be 
returned by a native method.

NOTE: this code assumes that the jdouble and SpiceDouble types
have the same size!
*/

void createVecGD_jni ( JNIEnv            * env,
                       SpiceInt            size,
                       ConstSpiceDouble  * CVec,
                       jdoubleArray      * jVec       )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "createVecGD_jni";


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( sizeof(jdouble) != sizeof(SpiceDouble) )
   {
      chkin_c  ( "createVecGD"                                  );
      setmsg_c ( "SpiceDouble has size # bytes; this size is "
                 "incompatible with that of type jdouble, "
                 "which is # bytes."                            );
      errint_c ( "#", (SpiceInt)(sizeof(SpiceDouble))           );
      errint_c ( "#", (SpiceInt)(sizeof(jdouble))               );
      sigerr_c ( "SPICE(BUG)"                                   );
      chkout_c ( "createVecGD"                                  );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }


   /*
   Create the output vector; the vector is an array of jdoubles. 
   */

   *jVec = (jdoubleArray) ( (*env)->NewDoubleArray(env, (jsize)size) );
   JNI_EXC( env );


   /*
   Fill in the output vector using CVec.
   */
   (*env)->SetDoubleArrayRegion ( env, 
                                  (jdoubleArray) *jVec, 
                                  (jsize       ) 0, 
                                  (jsize       ) size, 
                                  (jdouble    *) CVec  );
   JNI_EXC( env );

   /*
   At this point *jVec has been filled in.
   */
}





/*
Create a new integer general-dimension vector to be 
returned by a native method.

NOTE: this code assumes that the jint and SpiceInt types
have the same size!
*/
void createVecGI_jni ( JNIEnv            * env,
                       SpiceInt            size,
                       ConstSpiceInt     * CVec,
                       jintArray         * jVec       )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "createVecGI_jni";


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( sizeof(jint) != sizeof(SpiceInt) )
   {
      chkin_c  ( "createVecGD"                                  );
      setmsg_c ( "SpiceInt has size # bytes; this size is "
                 "incompatible with that of type jint, "
                 "which is # bytes."                            );
      errint_c ( "#", (SpiceInt)(sizeof(SpiceInt))              );
      errint_c ( "#", (SpiceInt)(sizeof(jint))                  );
      sigerr_c ( "SPICE(BUG)"                                   );
      chkout_c ( "createVecGD"                                  );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }


   /*
   Create the output vector; the vector is an array of jints. 
   */

   *jVec = (jintArray) ( (*env)->NewIntArray(env, (jsize)size) );
   JNI_EXC( env );


   /*
   Fill in the output vector using CVec.
   */
   (*env)->SetIntArrayRegion ( env, 
                              (jintArray) *jVec, 
                              (jsize    ) 0, 
                              (jsize    ) size, 
                              (jint    *) CVec );
   JNI_EXC( env );

   /*
   At this point *jVec has been filled in.
   */
}












/*
Free a dynamically allocated C-style character array that
has been used to store an input Java string. The string
must have been allocated via GetStringUTFChars. The
corresponding Java string must be supplied as an input.
*/
void freeVarInputString_jni ( JNIEnv      * env,
                              jstring       JavaStr, 
                              SpiceChar   * CString    )
{
   (*env)->ReleaseStringUTFChars(env, JavaStr, CString );
}






/*
Free a dynamically allocated d.p. array that
has been used to store a general-dimension input 
Java d.p. vector. The array must have been allocated via 
GetDoubleArrayElements. The corresponding Java array
must be supplied as an input.
*/
void freeVecGD_jni ( JNIEnv         * env,
                     jdoubleArray     jVec, 
                     SpiceDouble    * CVecPtr  )
{

   /*
   Release the elements of the dynamically allocated vector.
   */
   (*env)->ReleaseDoubleArrayElements( env, jVec, (jdouble *)CVecPtr, 0 );
}



/*
Free a dynamically allocated integer (jint) array that
has been used to store a general-dimension input 
Java integer (int) vector. The array must have been allocated via 
GetIntegerArrayElements. The corresponding Java array
must be supplied as an input.
*/
void freeVecGI_jni ( JNIEnv         * env,
                     jintArray        jVec, 
                     SpiceInt       * CVecPtr  )
{

   /*
   Release the elements of the dynamically allocated vector.
   */
   (*env)->ReleaseIntArrayElements( env, jVec, (jint *)CVecPtr, 0 );
}








/*
Capture an input Java string (jstring) in a fixed-length
C-style character array. 
*/
void getFixedInputString_jni ( JNIEnv     * env,
                               jstring      JavaStr, 
                               SpiceInt     lenout,
                               SpiceChar  * CString )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "getFixedInputString_jni";
   ConstSpiceChar        * dynPtr;

   SpiceInt                CStringLen;


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );

   
   /*
   Check the input string length against the available room.
   */
   CStringLen = (SpiceInt) ( (*env)->GetStringUTFLength(env, JavaStr ) );

   if ( CStringLen >= lenout )
   {
      chkin_c  ( "getFixedInputString"                     );
      setmsg_c ( "Input string length # exceeds available "
                 "room #."                                 );
      errint_c ( "#", CStringLen                           );
      errint_c ( "#", lenout-1                             );
      sigerr_c ( "SPICE(STRINGTOOLONG)"                    );
      chkout_c ( "getFixedInputString"                     );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }

   /*
   Don't allow zero-length input strings 
   */
   if ( CStringLen == 0 )
   {
      chkin_c  ( "getFixedInputString"                     );
      setmsg_c ( "Input string length is zero."            );
      sigerr_c ( "SPICE(EMPTYSTRING)"                      );
      chkout_c ( "getFixedInputString"                     );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }

   /*
   Get C string input in dynamic memory. 
   */
   dynPtr  = (*env)->GetStringUTFChars(env, JavaStr, 0);
   JNI_EXC( env );

   /*
   Copy string into local memory so we can release 
   the dynamic memory ASAP. 
   */
   strncpy ( CString, dynPtr, lenout );

   /*
   Release dynamic memory. 
   */
   (*env)->ReleaseStringUTFChars ( env,  JavaStr,  dynPtr );
   JNI_EXC( env );
}




/*
Capture an input Java string array (jstring) in a 
dynamically allocated Fortran-style character array. 
The caller must free this array using free().
*/
void getFortranStringArray_jni ( JNIEnv         * env,
                                 jobjectArray     jStrArray, 
                                 SpiceInt       * nStr,
                                 SpiceInt       * fStrLen,
                                 void          ** fStrArray )
{
   /*
   Local variables 
   */
   jobject                 jStrPtr;

   static SpiceChar      * caller = "getFortranStringArray_jni";
   SpiceChar            ** CPtrArray;

   SpiceInt                i;
   SpiceInt                n;


   /*
   Initialize the output arguments. 
   */
   
   *nStr      = 0;
   *fStrLen   = 0;
   *fStrArray = 0;

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );
 

   /*
   Get the input array's dimension. Return if an exception occurs.
   */
   *nStr  =  (*env)->GetArrayLength ( env, jStrArray );
   JNI_EXC ( env );

   /*
   No valid Fortran array can have length zero.
   */
   if ( *nStr == 0 )
   {
      chkin_c ( caller                                                      );
      setmsg_c( "Input Java string array has length zero. A Fortran "
                "string array cannot be constructed from this Java array."  );
      sigerr_c( "SPICE(ZEROLENGTHARRAY)"                                    );
      chkout_c( caller                                                      );
      return;
   }


   /*
   Allocate an array of character pointers.
   */
   CPtrArray = alloc_SpiceString_Pointer_array( *nStr );

   SPICE_EXC( env, caller );

   /*
   Loop over the Java object array, fetching string pointers as we go. 
   Store each string pointer in our string pointer array.
   */

   i = 0;

   while (     ( i < *nStr ) 
           &&  !( (*env)->ExceptionOccurred(env) )
           &&  !failed_c()                          )
   {
      /*
      Get a pointer to the ith element of the input array.
      */

      jStrPtr = ( jobject )
                (*env)->GetObjectArrayElement ( env, jStrArray, i );

      /*
      No string may have a null pointer.
      */
      if ( jStrPtr == 0  )
      {
         chkin_c ( caller                                                    );
         setmsg_c( "String at index # in input Java string array "
                   "has null pointer. A Fortran string array cannot be "
                   "constructed from this Java array."                       );
         errint_c( "#", i                                                    );
         sigerr_c( "SPICE(NULLPOINTER)"                                      );
         chkout_c( caller                                                    );
      }

      else if (  ! (*env)->ExceptionOccurred(env)  )
      {
         /*
         Get a pointer to a dynamically allocated C string containing
         containing the data of the ith element of the input array.

         Store the string pointer in the ith element of our array
         of C string pointers. 
         */
         CPtrArray[i] = (SpiceChar*)
                        ( (*env)->GetStringUTFChars(env, jStrPtr, 0) );

         /*
         No string may have a null pointer.
         */
         if ( CPtrArray[i] == 0  )
         {
            chkin_c ( caller                                                 );
            setmsg_c( "String at index # in input Java string array "
                      "has null pointer. A Fortran string array cannot be "
                      "constructed from this Java array."                    );
            errint_c( "#", i                                                 );
            sigerr_c( "SPICE(NULLPOINTER)"                                   );
            chkout_c( caller                                                 );
         }


         /*
         Zero-length strings are not valid in Fortran.
         */
         if (  strlen( CPtrArray[i] ) == 0  )
         {
            chkin_c ( caller                                                 );
            setmsg_c( "String at index # in input Java string array "
                      "has length zero. A Fortran string array cannot be "
                      "constructed from this Java array."                    );
            errint_c( "#", i                                                 );
            sigerr_c( "SPICE(EMPTYSTRING)"                                   );
            chkout_c( caller                                                 );
         }

         if (  ! ( (*env)->ExceptionOccurred(env) || failed_c() )  )
         {
            ++ i;
         }
       }
   }

   /*
   If an exception or SPICE error occurred, free the dynamic memory
   used to capture strings locally. The loop counter `i' indicates the
   number of strings captured before an exception occurred.
   */
   n = i;

   if (  (*env)->ExceptionOccurred(env)  ||  failed_c()  )
   {
      for ( i = 0;  i < n;  i++ )
      {
         jStrPtr = ( jobject )
                   (*env)->GetObjectArrayElement ( env, jStrArray, i );

         if ( jStrPtr != 0 )
         {
            (*env)->ReleaseStringUTFChars(env, jStrPtr, CPtrArray[i] );  
         }
      }

      free_SpiceMemory ( (void *)CPtrArray );

      return;
   }


   /*
   Convert the set of strings referenced by our array of string 
   pointers to a Fortran style array.  
   */
   C2F_CreateStrArr_Sig ( *nStr, 
                          (ConstSpiceChar **) CPtrArray,
                          fStrLen,
                          (SpiceChar **)fStrArray         );

   /*
   Regardless of whether the above conversion succeeded, free
   our dynamically allocated strings and then our string pointer
   array.
   */
   for ( i = 0;  i < *nStr;  i++ )
   {
      jStrPtr = ( jobject )
                (*env)->GetObjectArrayElement ( env, jStrArray, i );

      (*env)->ReleaseStringUTFChars(env, jStrPtr, CPtrArray[i] );  
   }
   
   free_SpiceMemory ( (void *)CPtrArray );

   /*
   If an error didn't occur, the outputs

      fStrLen
      fStrArray

   are set. 
   */
}



/*
Get an input 3x3 d.p. matrix passed to a native method.
*/

void getMat33D_jni ( JNIEnv            * env,
                     jobjectArray        jMat,       
                     SpiceDouble         CMat [3][3] )
{
   /*
   Local variables 
   */
   jdouble               * rowPtr;
   jdoubleArray            jInputRow;

   SpiceInt                i;
   SpiceInt                j;


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   for ( i = 0;  i < 3;  i++ )
   {
      /*
      Get a pointer to the ith row of the input matrix; this row
      is a Java array of jdoubles.
      */
      jInputRow =  ( jdoubleArray )
                   (*env)->GetObjectArrayElement ( env, jMat, i );
      JNI_EXC( env );


      /*
      Get a pointer to a dynamically allocated array of jdoubles
      containing the data of the ith row of the input matrix.
      */
      rowPtr   =  (*env)->GetDoubleArrayElements( env, jInputRow, 0 ); 
      JNI_EXC( env );

      /*
      Copy the row's data to the ith row of the output matrix. Use
      an explicit type cast. 
      */
      for ( j = 0;  j < 3;  j++  )
      {
         CMat[i][j] = (SpiceDouble) rowPtr[j]; 
      }

      /*
      Release the elements of the ith row of the input matrix.
      */
      (*env)->ReleaseDoubleArrayElements( env, jInputRow, rowPtr, 0 );
      JNI_EXC( env );

   }

}



/*
Get an input general dimension (MxN) d.p. matrix passed 
to a native method.

The returned matrix is dynamically allocated; the caller
must free the matrix using the CSPICE free_SpiceMemory function.
*/
void getMatGD_jni ( JNIEnv            * env,
                    jobjectArray        jMat,       
                    SpiceInt          * nrows,
                    SpiceInt          * ncols,
                    SpiceDouble      ** CMat  )
{
   /*
   Local variables 
   */
   jdouble               * rowPtr;
   jdoubleArray            jInputRow;

   static SpiceChar      * caller = "getMatGD_jni";

   SpiceInt                i;
   SpiceInt                j;
   SpiceInt                k;

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );

   if ( sizeof(jdouble) != sizeof(SpiceDouble) )
   {
      chkin_c  ( caller                                         );
      setmsg_c ( "SpiceDouble has size # bytes; this size is "
                 "incompatible with that of type jdouble, "
                 "which is # bytes."                            );
      errint_c ( "#", (SpiceInt)(sizeof(SpiceDouble))           );
      errint_c ( "#", (SpiceInt)(sizeof(jdouble))               );
      sigerr_c ( "SPICE(BUG)"                                   );
      chkout_c ( caller                                         );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }


   /*
   Get the vector's row count. Return if an exception occurs.
   */
   *nrows  =  (*env)->GetArrayLength ( env, jMat );
   JNI_EXC ( env );

   if ( *nrows == 0 )
   {
      chkin_c  ( caller                                         );
      setmsg_c ( "Matrix row count is 0; count must be > 0."    );
      sigerr_c ( "SPICE(INVALIDSIZE)"                           );
      chkout_c ( caller                                         );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }

   /*
   Get the vector's column count. This is the length of the 
   first row. Return if an exception occurs.
   */
   jInputRow =  ( jdoubleArray )
                (*env)->GetObjectArrayElement ( env, jMat, 0 );
   JNI_EXC ( env );

   *ncols    =  (*env)->GetArrayLength ( env, jInputRow );
   JNI_EXC ( env );

   if ( *ncols == 0 )
   {
      chkin_c  ( caller                                         );
      setmsg_c ( "Matrix column count is 0; count must be > 0." );
      sigerr_c ( "SPICE(INVALIDSIZE)"                           );
      chkout_c ( caller                                         );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }

   /*
   Allocate memory for the output array. 
   */
   
   *CMat = alloc_SpiceDouble_C_array ( *nrows, *ncols );

   JNI_EXC ( env );

 
   k = 0;

   for ( i = 0;  i <  (*nrows);  i++ )
   {
      /*
      Get a pointer to the ith row of the input matrix; this row
      is a Java array of jdoubles.
      */
      jInputRow =  ( jdoubleArray )
                   (*env)->GetObjectArrayElement ( env, jMat, i );
      JNI_EXC( env );


      /*
      Get a pointer to a dynamically allocated array of jdoubles
      containing the data of the ith row of the input matrix.
      */
      rowPtr   =  (*env)->GetDoubleArrayElements( env, jInputRow, 0 ); 
      JNI_EXC( env );

      /*
      Copy the row's data to the output matrix. Use
      an explicit type cast. 
      */
      for ( j = 0;  j < (*ncols);  j++  )
      {
         (*CMat)[k+j] = (SpiceDouble) rowPtr[j]; 
      }

      k += *ncols;

      /*
      Release the elements of the ith row of the input matrix.
      */
      (*env)->ReleaseDoubleArrayElements( env, jInputRow, rowPtr, 0 );
      JNI_EXC( env );

      /*
      Dispose of the reference to jInputRow. We clean this up
      here to avoid accumulation of local references.
      */     
      (*env)->DeleteLocalRef( env, jInputRow );

   }
}



/*
Capture an input Java string (jstring) in a dynamically
allocated C-style character array. NOTE: the caller must
deallocate the string via a call to freeVarInputString
once the string is no longer needed.
*/
void getVarInputString_jni ( JNIEnv           * env,
                             jstring            JavaStr, 
                             SpiceInt         * CStringLen,
                             SpiceChar       ** CString    )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "getVarInputString_jni";


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC( env );


   /*
   Create a C string in dynamic memory from the input Java string.
   Let *Cstring point to the C string.

   NOTE: this code assumes that the SpiceChar type has size of
   1 byte!
   */

   if ( sizeof(SpiceChar) != 1 )
   {
      chkin_c  ( "getVarInputString"                        );
      setmsg_c ( "SpiceChar has size # bytes; this size is "
                 "incompatible with that of type jbyte."    );
      errint_c ( "#", (SpiceInt)(sizeof(SpiceChar))         );
      sigerr_c ( "SPICE(BUG)"                               );
      chkout_c ( "getVarInputString"                        );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }


   *CString    = (SpiceChar*)( (*env)->GetStringUTFChars(env, JavaStr, 0) );
   JNI_EXC( env );

   /*
   Set the output string length argument. 
   */
   *CStringLen = (SpiceInt) ( (*env)->GetStringUTFLength(env, JavaStr ) );

}



/*
Get an input 3-vector passed to a native method.
*/

void getVec3D_jni ( JNIEnv            * env,
                    jdoubleArray        jVec,       
                    SpiceDouble         CVec [3] )
{
   /*
   Local variables 
   */
   jdouble               * jVecPtr; 
   SpiceInt                i;

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   /*
   Get a pointer to a dynamically allocated array of jdoubles
   containing the data of the input vector
   */
   jVecPtr  =  (*env)->GetDoubleArrayElements( env, jVec, 0 ); 
   JNI_EXC( env );

   /*
   Copy the vector's data to the ith element of the output vector. Use
   an explicit type cast. 
   */
   for ( i = 0;  i < 3;  i++  )
   {
      CVec[i] = (SpiceDouble) jVecPtr[i]; 
   }

   /*
   Release the elements of the dynamically allocated vector.
   */
   (*env)->ReleaseDoubleArrayElements( env, jVec, jVecPtr, 0 );
   JNI_EXC( env );
}



/*
Get an input array of 3 integers passed to a native method.

This special-purpose utility handles Euler axis sequences.
*/
void getVec3I_jni ( JNIEnv            * env,
                    jdoubleArray        jVec,       
                    SpiceInt            CVec [3] )
{
   /*
   Local variables 
   */
   jint                  * jVecPtr; 
   SpiceInt                i;

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   /*
   Get a pointer to a dynamically allocated array of jints
   containing the data of the input array.
   */
   jVecPtr  =  (*env)->GetIntArrayElements( env, jVec, 0 ); 
   JNI_EXC( env );

   /*
   Copy the vector's data to the ith element of the output array. Use
   an explicit type cast. 
   */
   for ( i = 0;  i < 3;  i++  )
   {
      CVec[i] = (SpiceInt) jVecPtr[i]; 
   }

   /*
   Release the elements of the dynamically allocated vector.
   */
   (*env)->ReleaseIntArrayElements( env, jVec, jVecPtr, 0 );
   JNI_EXC( env );
}



/*
Get an input fixed-size vector passed to a native method.
*/

void getVecFixedD_jni ( JNIEnv            * env,
                        jdoubleArray        jVec,       
                        SpiceInt            expSize,
                        SpiceDouble       * CVec    )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "getVecFixedD_jni";

   jdouble               * jVecPtr; 

   SpiceInt                i;
   SpiceInt                size;


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   /*
   Get the size of the input Java array. 
   */
   
   size = (*env)->GetArrayLength( env, jVec );

   if ( size != expSize )
   {
      /*
      Signal a SPICE error, then throw an exception and return.
      */
      setmsg_c ( "Input array has size #; expected size is #." );
      errint_c ( "#",  size                                    );
      errint_c ( "#",  expSize                                 );
      sigerr_c ( "SPICE(SIZEMISMATCH)"                         );

      SPICE_EXC( env, caller );
   }

   /*
   Get a pointer to a dynamically allocated array of jdoubles
   containing the data of the input vector
   */
   jVecPtr  =  (*env)->GetDoubleArrayElements( env, jVec, 0 ); 
   JNI_EXC( env );

   /*
   Copy the vector's data to the ith element of the output vector. Use
   an explicit type cast. 
   */
   for ( i = 0;  i < size;  i++  )
   {
      CVec[i] = (SpiceDouble) jVecPtr[i]; 
   }

   /*
   Release the elements of the dynamically allocated vector.
   */
   (*env)->ReleaseDoubleArrayElements( env, jVec, jVecPtr, 0 );
   JNI_EXC( env );
}



/*
Get an input general-dimension Java double vector and store the 
contents in a dynamically allocated C-style SpiceDouble 
array. NOTE: the caller must deallocate the array via a call 
to freeVecGD once the vector is no longer needed.

NOTE: this code assumes that the jdouble and SpiceDouble types
have the same size!
*/

void getVecGD_jni ( JNIEnv            * env,
                    jdoubleArray        jVec,       
                    SpiceInt          * CVecLen,
                    SpiceDouble      ** CVecPtr  )
{
   /*
   Local variables 
   */ 
   static SpiceChar      * caller = "getVecGD_jni";


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( sizeof(jdouble) != sizeof(SpiceDouble) )
   {
      chkin_c  ( "getVecGD"                                     );
      setmsg_c ( "SpiceDouble has size # bytes; this size is "
                 "incompatible with that of type jdouble, "
                 "which is # bytes."                            );
      errint_c ( "#", (SpiceInt)(sizeof(SpiceDouble))           );
      errint_c ( "#", (SpiceInt)(sizeof(jdouble))               );
      sigerr_c ( "SPICE(BUG)"                                   );
      chkout_c ( "getVecGD"                                     );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }


   /*
   Get the vector's length. Return if an exception occurs.
   */
   *CVecLen  =  (*env)->GetArrayLength ( env, jVec );
   JNI_EXC ( env );


   /*
   Get a pointer to a dynamically allocated array of jdoubles
   containing the data of the input array.
   */

   *CVecPtr  =  (SpiceDouble *) 
                ( (*env)->GetDoubleArrayElements( env, jVec, 0 ) );
}



/*
Get an input general-dimension Java integer (int) vector 
and store the contents in a dynamically allocated C-style 
SpiceInt array. NOTE: the caller must deallocate the array 
via a call to freeVecGI once the vector is no longer needed.

NOTE: this code assumes that the jint and SpiceInt types
have the same size!
*/

void getVecGI_jni ( JNIEnv            * env,
                    jintArray           jVec,   
                    SpiceInt          * CVecLen,    
                    SpiceInt         ** CVecPtr  )
{
   /*
   Local variables 
   */ 
   static SpiceChar      * caller = "getVecGI_jni";


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC( env );


   if ( sizeof(jint) != sizeof(SpiceInt) )
   {
      chkin_c  ( "getVecGI"                                     );
      setmsg_c ( "SpiceInt has size # bytes; this size is "
                 "incompatible with that of type jint, "
                 "which is # bytes."                            );
      errint_c ( "#", (SpiceInt)(sizeof(SpiceInt))              );
      errint_c ( "#", (SpiceInt)(sizeof(jint))                  );
      sigerr_c ( "SPICE(BUG)"                                   );
      chkout_c ( "getVecGI"                                     );

      zzThrowSpiceErrorException_jni ( env, caller );
      return;
   }

   /*
   Get the vector's length. Return if an exception occurs.
   */
   *CVecLen  =  (*env)->GetArrayLength ( env, jVec );
   JNI_EXC ( env );


   /*
   Get a pointer to a dynamically allocated array of jdoubles
   containing the data of the input array.
   */
     
   *CVecPtr  =  (SpiceInt *)
                (  (*env)->GetIntArrayElements( env, jVec, 0 )  ); 
}






/*
Update a 3x3 d.p. matrix passed into a native method. This
allows the array to act as an output argument.
*/

void updateMat33D_jni ( JNIEnv            * env,
                        ConstSpiceDouble    CMat [3][3],
                        jobjectArray        jMat        )
{
   /*
   Local variables 
   */
   jdouble               * jRowPtr;
   jdoubleArray            jRow;
   
   static SpiceChar      * caller = "updateMat33D_jni";

   SpiceInt                i;
   SpiceInt                j;
   SpiceInt                jNCols;
   SpiceInt                jNRows;


   /*
   Get the size of the Java array to be updated. The first
   dimension of the array is the row count.
   */   
   jNRows = (*env)->GetArrayLength( env, jMat );
   
   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( jNRows != 3 )
   {
      /*
      Signal a SPICE error, then throw an exception and return.
      */
      setmsg_c ( "Output array has row count #; dimension "
                 "must be 3x3."                             );
      errint_c ( "#",  jNRows                               );
      sigerr_c ( "SPICE(SIZEMISMATCH)"                      );

      SPICE_EXC( env, caller );
   }


   for ( i = 0;  i < 3;  i++ )
   {
      /*
      Get a pointer to the ith object in the input array. 
      */
      jRow     =  ( jdoubleArray )
                  (*env)->GetObjectArrayElement  ( env, jMat, i );

      jNCols = (*env)->GetArrayLength( env, jRow );

      JNI_EXC( env );


      if ( jNCols != 3 )
      {
         /*
         Signal a SPICE error, then throw an exception and return.
         */
         setmsg_c ( "Output array has column count #; dimension "
                    "must be 3x3."                               );
         errint_c ( "#",  jNRows                                 );
         sigerr_c ( "SPICE(SIZEMISMATCH)"                        );

         SPICE_EXC( env, caller );
      }

      /*
      Get a pointer to the values of the ith row, which are stored
      in dynamically allocated memory. 
      */
      jRowPtr   =  (*env)->GetDoubleArrayElements( env, jRow, 0 );  
      JNI_EXC( env );

      /*
      Update the dynamic memory using the ith row of the input C
      matrix. 
      */
      for ( j = 0;  j < 3;  j++  )
      {
         jRowPtr[j] = CMat[i][j];
      }

      /*
      Release the elements of the ith row of the input matrix.
      This step transfers data from the dynamically allocated 
      row to the Java object representing the ith row.
      */
      (*env)->ReleaseDoubleArrayElements( env,  jRow,  jRowPtr,  0 );
   }

   /*
   The Java matrix has been updated. 
   */
}






/*
Update a d.p. 3-vector passed into a native method. This
allows the vector to act as an output argument.
*/

void updateVec3D_jni ( JNIEnv            * env,
                       ConstSpiceDouble    CVec [3],
                       jdoubleArray        jVec      )
{
   /*
   Local variables 
   */
   jdouble               * jVecPtr;   

   static SpiceChar      * caller      = "updateVec3D_jni";

   SpiceInt                i;
   SpiceInt                jVecSize;



   jVecSize = (*env)->GetArrayLength( env, jVec );

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( jVecSize != 3 )
   {
      /*
      Signal a SPICE error, then throw an exception and return.
      */
      setmsg_c ( "Output array has size #; size must be 3." );
      errint_c ( "#",  jVecSize                                 );
      sigerr_c ( "SPICE(SIZEMISMATCH)"                          );

      SPICE_EXC( env, caller );
   }


   /*
   Get a pointer to the values of the vector, which are stored
   in dynamically allocated memory. 
   */
   jVecPtr   =  (*env)->GetDoubleArrayElements( env, jVec, 0 );  
   JNI_EXC( env );

   /*
   Update the dynamic memory using the input C vector. Use
   an explicit type cast.
   */
   for ( i = 0;  i < 3;  i++  )
   {
      jVecPtr[i] = (jdouble)CVec[i];
   }

   /*
   Release the elements of the vector. This step transfers data from
   the dynamically allocated C vector to the Java vector.
   */
   (*env)->ReleaseDoubleArrayElements( env,  jVec,  jVecPtr,  0 );

   /*
   The Java vector has been updated. 
   */
}


/*
Update a Boolean general-dimension vector passed into a 
native method. This allows the vector to act as an output argument.
*/

void updateVecGB_jni ( JNIEnv            * env,
                       SpiceInt            size,
                       ConstSpiceBoolean * CVec,
                       jbooleanArray       jVec  )
{
   /*
   Local variables 
   */
   jboolean              * jVecPtr;

   static SpiceChar      * caller = "updateVecGB_jni";

   SpiceInt                i;
   SpiceInt                jVecSize;


   jVecSize = (*env)->GetArrayLength( env, jVec );

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( size > jVecSize )
   {
      /*
      Signal a SPICE error, then throw an exception and return.
      */
      setmsg_c ( "Output array has size #; required size is #." );
      errint_c ( "#",  jVecSize                                 );
      errint_c ( "#",  size                                     );
      sigerr_c ( "SPICE(SIZEMISMATCH)"                          );

      SPICE_EXC( env, caller );
   }

   /*
   Get a pointer to the values of the vector, which are stored
   in dynamically allocated memory. 
   */
   jVecPtr   =  (*env)->GetBooleanArrayElements( env, jVec, 0 );  
   JNI_EXC( env );

   /*
   Update the dynamic memory using the input C vector. Use
   an explicit type cast.
   */
   for ( i = 0;  i < size;  i++  )
   {
      jVecPtr[i] = (jboolean)CVec[i];
   }

   /*
   Release the elements of the vector. This step transfers data from
   the dynamically allocated C vector to the Java vector.
   */
   (*env)->ReleaseBooleanArrayElements( env,  jVec,  jVecPtr,  0 );

   /*
   The Java vector has been updated. 
   */
}



/*
Update a d.p. general-dimension vector passed into a native method. This
allows the vector to act as an output argument.
*/

void updateVecGD_jni ( JNIEnv            * env,
                       SpiceInt            size,
                       ConstSpiceDouble  * CVec,
                       jdoubleArray        jVec  )
{
   /*
   Local variables 
   */
   jdouble               * jVecPtr;

   static SpiceChar      * caller = "updateVecGD_jni";

   SpiceInt                i;
   SpiceInt                jVecSize;



   /*
   Get the size of the Java array to be updated. 
   */   
   jVecSize = (*env)->GetArrayLength( env, jVec );
   
   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( size > jVecSize )
   {
      /*
      Signal a SPICE error, then throw an exception and return.
      */
      setmsg_c ( "Output array has size #; required size is #." );
      errint_c ( "#",  jVecSize                                 );
      errint_c ( "#",  size                                     );
      sigerr_c ( "SPICE(SIZEMISMATCH)"                          );

      SPICE_EXC( env, caller );
   }


   /*
   Get a pointer to the values of the vector, which are stored
   in dynamically allocated memory. 
   */
   jVecPtr   =  (*env)->GetDoubleArrayElements( env, jVec, 0 );  
   JNI_EXC( env );

   /*
   Update the dynamic memory using the input C vector. Use
   an explicit type cast.
   */
   for ( i = 0;  i < size;  i++  )
   {
      jVecPtr[i] = (jdouble)CVec[i];
   }

   /*
   Release the elements of the vector. This step transfers data from
   the dynamically allocated C vector to the Java vector.
   */
   (*env)->ReleaseDoubleArrayElements( env,  jVec,  jVecPtr,  0 );

   JNI_EXC( env );

   /*
   The Java vector has been updated. 
   */
}

 

/*
Update an integer general-dimension vector passed into a native 
method. This allows the vector to act as an output argument.
*/

void updateVecGI_jni ( JNIEnv            * env,
                       SpiceInt            size,
                       ConstSpiceInt     * CVec,
                       jintArray           jVec  )
{
   /*
   Local variables 
   */
   jint                  * jVecPtr;

   static SpiceChar      * caller = "updateVecGI_jni";

   SpiceInt                i;
   SpiceInt                jVecSize;


   /*
   Get the size of the Java array to be updated. 
   */
   jVecSize = (*env)->GetArrayLength( env, jVec );

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( size > jVecSize )
   {
      /*
      Signal a SPICE error, then throw an exception and return.
      */
      setmsg_c ( "Output array has size #; required size is #." );
      errint_c ( "#",  jVecSize                                 );
      errint_c ( "#",  size                                     );
      sigerr_c ( "SPICE(SIZEMISMATCH)"                          );

      SPICE_EXC( env, caller );
   }

   /*
   Get a pointer to the values of the vector, which are stored
   in dynamically allocated memory. 
   */
   jVecPtr   =  (*env)->GetIntArrayElements( env, jVec, 0 );  
   JNI_EXC( env );

   /*
   Update the dynamic memory using the input C vector. Use
   an explicit type cast.
   */
   for ( i = 0;  i < size;  i++  )
   {
      jVecPtr[i] = (jint)CVec[i];
   }

   /*
   Release the elements of the vector. This step transfers data from
   the dynamically allocated C vector to the Java vector.
   */
   (*env)->ReleaseIntArrayElements( env,  jVec,  jVecPtr,  0 );

   JNI_EXC( env );

   /*
   The Java vector has been updated. 
   */
}

 

/*
Create a new 2D d.p. matrix to be returned by a native method.
*/

void createMatGD_jni ( JNIEnv            * env,
                       int                 nrows,
                       int                 ncols,
                       void              * CMat,
                       jobjectArray      * jRetMat      )
{
   /*
   Local variables 
   */

   jdoubleArray            row;
   SpiceDouble           * rowPtr;
   SpiceInt                i;


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );

   /*
   First create the array of objects, then assign each element to
   point to a jdoubleArray object containing a row of the matrix.

   The class name of the objects is the Java symbol for "array of
   doubles", which is "[D".
   */

   *jRetMat = 

     (*env)->NewObjectArray( env, 
                             nrows, 
                             (*env)->FindClass( env, "[D" ),
                             (*env)->NewDoubleArray(env,ncols)   );
   JNI_EXC( env );


   for ( i = 0;  i < nrows;  i++ )
   {
      /*
      Create the ith row of the output matrix; the row 
      is an array of jdoubles. 
      */
      row = (jdoubleArray) ( (*env)->NewDoubleArray(env, ncols) );
      JNI_EXC( env );


      /*
      Fill in the ith row of the output matrix, using the ith
      row of `CMat'.
      */
      rowPtr =   ((SpiceDouble *)CMat) + ( i * ncols );

      (*env)->SetDoubleArrayRegion ( env, 
                                     (jdoubleArray) row, 
                                     (jsize)0, 
                                     ncols, 
                                     (jdouble *) rowPtr );
      JNI_EXC( env );


      /*
      Insert the ith row into the output matrix; this matrix
      is an array of jobjects. 
      */
      (*env)->SetObjectArrayElement ( env, *jRetMat, i, row );
      JNI_EXC( env );

   }

   /*
   At this point *jRetMat has been filled in.
   */
}



/*
Create a new 2D integer matrix to be returned by a native method.
*/

void createMatGI_jni ( JNIEnv            * env,
                       int                 nrows,
                       int                 ncols,
                       void              * CMat,
                       jobjectArray      * jRetMat      )
{
   /*
   Local variables 
   */

   jintArray               row;

   SpiceInt                i;
   SpiceInt              * rowPtr;


   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );

   /*
   First create the array of objects, then assign each element to
   point to a jintArray object containing a row of the matrix.

   The class name of the objects is the Java symbol for "array of
   ints", which is "[I".
   */

   *jRetMat = 

     (*env)->NewObjectArray( env, 
                             nrows, 
                             (*env)->FindClass( env, "[I" ),
                             (*env)->NewIntArray(env,ncols)   );
   JNI_EXC( env );


   for ( i = 0;  i < nrows;  i++ )
   {
      /*
      Create the ith row of the output matrix; the row 
      is an array of jints. 
      */
      row = (jintArray) ( (*env)->NewIntArray(env, ncols) );
      JNI_EXC( env );


      /*
      Fill in the ith row of the output matrix, using the ith
      row of `CMat'.
      */
      rowPtr =   ((SpiceInt *)CMat) + ( i * ncols );

      (*env)->SetIntArrayRegion ( env, 
                                  (jintArray) row, 
                                  (jsize)0, 
                                  ncols, 
                                  (jint *) rowPtr );
      JNI_EXC( env );


      /*
      Insert the ith row into the output matrix; this matrix
      is an array of jobjects. 
      */
      (*env)->SetObjectArrayElement ( env, *jRetMat, i, row );
      JNI_EXC( env );

   }

   /*
   At this point *jRetMat has been filled in.
   */
}



/*
Get an input fixed-size integer vector passed to a native method.
*/

void getVecFixedI_jni ( JNIEnv            * env,
                        jintArray           jVec,       
                        SpiceInt            expSize,
                        SpiceInt          * CVec    )
{
   /*
   Local variables 
   */
   static SpiceChar      * caller = "getVecFixedI_jni";

   jint                  * jVecPtr; 

   SpiceInt                i;
   SpiceInt                size;


   /*
   Get the size of the input Java array. 
   */
   
   size = (*env)->GetArrayLength( env, jVec );

   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( size != expSize )
   {
      /*
      Signal a SPICE error, then throw an exception and return.
      */
      setmsg_c ( "Input array has size #; expected size is #." );
      errint_c ( "#",  size                                    );
      errint_c ( "#",  expSize                                 );
      sigerr_c ( "SPICE(SIZEMISMATCH)"                         );

      SPICE_EXC( env, caller );
   }

   /*
   Get a pointer to a dynamically allocated array of jints
   containing the data of the input vector
   */
   jVecPtr  =  (*env)->GetIntArrayElements( env, jVec, 0 ); 
   JNI_EXC( env );

   /*
   Copy the vector's data to the ith element of the output vector. Use
   an explicit type cast. 
   */
   for ( i = 0;  i < size;  i++  )
   {
      CVec[i] = (SpiceInt) jVecPtr[i]; 
   }

   /*
   Release the elements of the dynamically allocated vector.
   */
   (*env)->ReleaseIntArrayElements( env, jVec, jVecPtr, 0 );
   JNI_EXC( env );
}



/*
Update a general size, 2-D d.p. matrix passed into a native method. This
allows the array to act as an output argument.
*/

void updateMatGD_jni ( JNIEnv            * env,
                       SpiceInt            nrows,
                       SpiceInt            ncols,                       
                       void              * CMat,
                       jobjectArray        jMat        )
{
   /*
   Local variables 
   */
   jdouble               * jRowPtr;
   jdoubleArray            jRow;
   
   static SpiceChar      * caller = "updateMatGD_jni";

   SpiceDouble           * dpPtr;

   SpiceInt                i;
   SpiceInt                j;
   SpiceInt                jNCols;
   SpiceInt                jNRows;


   /*
   Get the size of the Java array to be updated. The first
   dimension of the array is the row count.
   */   
   jNRows = (*env)->GetArrayLength( env, jMat );
   
   /*
   Check for exceptions; return if any are found. 
   */
   JNI_EXC ( env );


   if ( nrows > jNRows )
   {
      /*
      Signal a SPICE error, then throw an exception and return.
      */
      setmsg_c ( "Output array has row count #; required "
                 "count is #."                             );
      errint_c ( "#",  jNRows                              );
      errint_c ( "#",  nrows                               );
      sigerr_c ( "SPICE(SIZEMISMATCH)"                     );

      SPICE_EXC( env, caller );
   }


   for ( i = 0;  i < nrows;  i++ )
   {
      /*
      Get a pointer to the ith object in the input array. 
      */
      jRow     =  ( jdoubleArray )
                  (*env)->GetObjectArrayElement  ( env, jMat, i );
      JNI_EXC( env );

      /*
      Get a pointer to the values of the ith row, which are stored
      in dynamically allocated memory. 
      */
      jRowPtr   =  (*env)->GetDoubleArrayElements( env, jRow, 0 );  
      JNI_EXC( env );

      /*
      Get the size of the Java array to be updated. The second
      dimension of the array is the column count.
      */   
      jNCols = (*env)->GetArrayLength( env, jRow );
   
      if ( ncols > jNCols )
      {
         /*
         Signal a SPICE error, then throw an exception and return.
         */
         setmsg_c ( "Row # in output array has column "
                    "count #; required count is #."     );
         errint_c ( "#",  i                             );
         errint_c ( "#",  jNCols                        );
         errint_c ( "#",  ncols                         );
         sigerr_c ( "SPICE(SIZEMISMATCH)"               );

         SPICE_EXC( env, caller );
      }


      /*
      Update the dynamic memory using the ith row of the input C
      matrix. 
      */
      for ( j = 0;  j < ncols;  j++  )
      {
         dpPtr      = ((SpiceDouble *)CMat) + ((i*ncols) + j);

         jRowPtr[j] = *dpPtr;
      }

      /*
      Release the elements of the ith row of the input matrix.
      This step transfers data from the dynamically allocated 
      row to the Java object representing the ith row.
      */
      (*env)->ReleaseDoubleArrayElements( env,  jRow,  jRowPtr,  0 );
   }

   /*
   The Java matrix has been updated. 
   */
}



