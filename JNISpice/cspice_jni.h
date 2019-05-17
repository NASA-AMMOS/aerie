
#include <jni.h>
#include "SpiceUsr.h"


/*
   Version 3.0.0 29-DEC-2016 (NJB) 

      Added prototypes for utilities

         getMatGD_jni
         createVecGB_jni

      Added definition of aberration correction locus macro

         LOCLEN

      Added definition of ID word string length

         IDWLEN


   Last update was 27-FEB-2014 (NJB) 

      Added constant

         ANTPLN      

   Version 2.0.0 19-AUG-2013 (NJB) 

      Added constant

         KARCLN      
   
   Version 1.0.0 15-AUG-2009 (NJB) 
*/



/*
Include guard: 
*/

#ifndef HAVE_CSPICE_JNI

   #define HAVE_CSPICE_JNI



   /*
   Prototypes
   */

   jstring 
   createJavaString_jni                ( JNIEnv             * env,
                                         SpiceChar          * cString );

   jobjectArray 
   createJavaStringArray_jni           ( JNIEnv             * env,
                                         SpiceInt             nStr,
                                         SpiceInt             fStrLen,
                                         void               * fStrArray );

   void 
   createMat33D_jni                     ( JNIEnv            * env,
                                          ConstSpiceDouble    cMat [3][3],
                                          jobjectArray      * jMat        );

   void 
   createVec3D_jni                      ( JNIEnv            * env,
                                          ConstSpiceDouble    CVec [3],
                                          jdoubleArray      * jVec       );

   void 
   createVecGB_jni                      ( JNIEnv             * env,
                                          SpiceInt             size,
                                          ConstSpiceBoolean  * CVec,
                                          jbooleanArray      * jVec );

   void 
   createVecGD_jni                      ( JNIEnv            * env,
                                          SpiceInt            size,
                                          ConstSpiceDouble  * CVec,
                                          jdoubleArray      * jVec       );

   void 
   createVecGI_jni                      ( JNIEnv            * env,
                                          SpiceInt            size,
                                          ConstSpiceInt       CVec [3],
                                          jintArray         * jVec       );

   void 
   freeVarInputString_jni               ( JNIEnv            * env,
                                          jstring             JavaStr, 
                                          SpiceChar         * CString    );

   void 
   freeVecGD_jni                        ( JNIEnv            * env,
                                          jdoubleArray        jVec, 
                                          SpiceDouble       * CVecPtr  );

   void
   freeVecGI_jni                        ( JNIEnv            * env,
                                          jintArray           jVec, 
                                          SpiceInt          * CVecPtr  );

   void 
   getFixedInputString_jni              ( JNIEnv            * env,
                                          jstring             JavaStr, 
                                          SpiceInt            lenout,
                                          SpiceChar         * cString );

   void 
   getFortranStringArray_jni            ( JNIEnv            * env,
                                          jobjectArray        jStrArray, 
                                          SpiceInt          * nStr,
                                          SpiceInt          * fStrLen,
                                          void             ** fStrArray );

   void 
   getMat33D_jni                        ( JNIEnv            * env,
                                          jobjectArray        jMat,  
                                          SpiceDouble         cMat [3][3] );

   void 
   getMatGD_jni                         ( JNIEnv            * env,
                                          jobjectArray        jMat,  
                                          SpiceInt          * nrows,
                                          SpiceInt          * ncols,
                                          SpiceDouble      ** cMat  );

   void 
   getVarInputString_jni                ( JNIEnv            * env,
                                          jstring             JavaStr, 
                                          SpiceInt          * CStringLen,
                                          SpiceChar        ** CString    );

   void 
   getVec3D_jni                         ( JNIEnv            * env,
                                          jdoubleArray        jVec,       
                                          SpiceDouble         CVec [3] );

   void 
   getVec3I_jni                         ( JNIEnv            * env,
                                          jdoubleArray        jVec,       
                                          SpiceInt            CVec [3] );

   void 
   getVecFixedD_jni                     ( JNIEnv            * env,
                                          jdoubleArray        jVec,       
                                          SpiceInt            expSize,
                                          SpiceDouble       * CVecPtr  );

   void 
   getVecGD_jni                         ( JNIEnv            * env,
                                          jdoubleArray        jVec,       
                                          SpiceInt          * CVecLen,
                                          SpiceDouble      ** CVecPtr  );

   void 
   getVecGI_jni                         ( JNIEnv            * env,
                                          jintArray           jVec,       
                                          SpiceInt          * CVecLen,
                                          SpiceInt         ** CVecPtr  );

   void 
   updateMat33D_jni                     ( JNIEnv            * env,
                                          ConstSpiceDouble    CVec [3][3],
                                          jobjectArray        jVec        );

   void 
   updateVec3D_jni                      ( JNIEnv            * env,
                                          ConstSpiceDouble    CVec [3],
                                          jdoubleArray        jVec      );

   void 
   updateVecGB_jni                      ( JNIEnv            * env,
                                          SpiceInt            size,
                                          ConstSpiceBoolean * CVec,
                                          jbooleanArray       jVec      );

   void 
   updateVecGD_jni                      ( JNIEnv            * env,
                                          SpiceInt            size,
                                          ConstSpiceDouble  * CVec,
                                          jdoubleArray        jVec      );

   void 
   updateVecGI_jni                      ( JNIEnv            * env,
                                          SpiceInt            size,
                                          ConstSpiceInt     * CVec,
                                          jintArray           jVec      );

   SpiceCell * 
   zzalcell_c                           ( SpiceCellDataType   dataType,
                                          SpiceInt            size,
                                          SpiceInt            card,
                                          SpiceInt            length,
                                          const void        * data     );

   void                              
   zzdacell_c                           ( SpiceCell         * cell );


   void 
   zzThrowException_jni                 ( JNIEnv            * env,
                                          ConstSpiceChar    * excName,
                                          ConstSpiceChar    * message );

   void 
   zzThrowSpiceErrorException_jni       ( JNIEnv            * env,
                                          ConstSpiceChar    * caller );

   void 
   createMatGD_jni                      ( JNIEnv            * env,
                                          int                 nrows,
                                          int                 ncols,
                                          void              * CMat,
                                          jobjectArray      * jRetMat      );

   void 
   createMatGI_jni                      ( JNIEnv            * env,
                                          int                 nrows,
                                          int                 ncols,
                                          void              * CMat,
                                          jobjectArray      * jRetMat      );


   void 
   getVecFixedI_jni                     ( JNIEnv            * env,
                                          jintArray           jVec,
                                          SpiceInt            expSize,
                                          SpiceInt          * CVecPtr  );


   void
   updateMatGD_jni                      ( JNIEnv            * env,
                                          SpiceInt            nrows,
                                          SpiceInt            ncols,
                                          void              * CMat,
                                          jobjectArray        jMat        );

   /*
   Macros
   */

   /*
   Exception name macros: 
   */
   #define DAFRECNF_EXC     "spice/basic/DAFRecordNotFoundException"
   #define IDNF_EXC         "spice/basic/IDCodeNotFoundException"
   #define KVCNF_EXC        "spice/basic/KernelVarCompNotFoundException"
   #define KVNF_EXC         "spice/basic/KernelVarNotFoundException"
   #define NNF_EXC          "spice/basic/NameNotFoundException"
   #define PARNF_EXC        "spice/basic/ParameterNotFoundException"
   #define SPICE_ERROR_EXC  "spice/basic/SpiceErrorException"


   /*
   Macro that throws a SpiceErrorException and returns:
   this is to be used by void functions.
   */
   #define SPICE_EXC( env, caller )                       \
   {                                                      \
      if ( failed_c() )                                   \
      {                                                   \
         zzThrowSpiceErrorException_jni ( env, caller );  \
         return;                                          \
      }                                                   \
   }

   /*
   Macro that throws a SpiceErrorException and returns
   a value:
   */
   #define SPICE_EXC_VAL( env, caller, retVal )          \
   {                                                     \
      if ( failed_c() )                                  \
      {                                                  \
         zzThrowSpiceErrorException_jni( env, caller );  \
         return ( retVal );                              \
      }                                                  \
   }


   /*
   Macro that tests for a JNI exception and returns
   if one occurred:
   */
   #define JNI_EXC( env )                      \
   {                                           \
      if ( (*env)->ExceptionOccurred( env ) )  \
      {                                        \
          return;                              \
      }                                        \
   }


   /*
   Macro that tests for a JNI exception and returns
   a value if such an exception occurred:
   */
   #define JNI_EXC_VAL( env, retVal )          \
   {                                           \
      if ( (*env)->ExceptionOccurred( env ) )  \
      {                                        \
          return ( retVal );                   \
      }                                        \
   }




   /* 
   File scope constants
   */

   /*
   Maximum length of buffered file name 
   */
   #define FNAMLN                    256

   /*
   Maximum length of CSPICE traceback message
   */
   #define TRCLEN                  ( (32 * 100) + (2 * 99) + 1 )

   /*
   Maximum length of CSPICE short error message
   */
   #define SMSGLN                     26

   /*
   Maximum length of CSPICE long error message
   */
   #define LMSGLN                   ( ( 23 * 80 ) + 1 )

   /*
   Maximum length of Toolkit version info 
   */
   #define VERLEN                     81

   /*
   Maximum length of SPICE error exception message
   */
   #define EXMLEN                   ( TRCLEN + LMSGLN +          \
                                      SMSGLN + VERLEN + 32 + 8 )

   /*
   Maximum length of kernel variable name. 
   */
   #define KVNMLN                     33

   /*
   Maximum length of string kernel variable value.
   Note: this value applies to Fortran-style strings.
   */
   #define F_KVSTLN                   80

   /*
   Maximum length of body name.
   */
   #define BDNMLN                     37

   /*
   Maximum length of surface name.
   */
   #define SFNMLN                     37

   /*
   Maximum length of frame name.
   */
   #define FRNMLN                     33

   /*
   Maximum length of aberration correction specifier.
   */
   #define CORRLN                     11

   /*
   Maximum length of aberration correction locus specifier.
   */
   #define LOCLEN                     51

   /*
   Maximum length of time string.
   */
   #define TIMLEN                     1001

   /*
   Maximum length of SCLK string. 
   */
   #define SCLKLN                     1001

   /*
   Maximum length of unit name string. 
   */
   #define UNITLN                     1001

   /*
   Maximum length of a GF relational operator. 
   */
   #define RLOPLN                     81 

   /*
   Maximum length of a GF occultation type specifier.
   */
   #define OCCLN                      8

   /*
   Maximum length of a GF coordinate system specifier.
   */
   #define CSYSLN                     81

   /*
   Maximum length of a GF coordinate specifier.
   */
   #define CORDLN                     81

   /*
   Maximum length of a GF sub-observer point method specifier.
   */
   #define METHLN                     81

   /*
   Maximum length of a KEEPER file type specifier.
   */
   #define KTYPLN                     9

   /*
   Maximum length of a target body shape specifier.
   */
   #define BSHPLN                     501

   /*
   Length of an SPK, CK, or binary PCK segment ID. 
   */
   #define SIDLEN                     41

   /*
   Length of a DAF or DAS ID word.
   */
   #define IDWLEN                     9

   /*
   Length of a DAF or DAS internal file name.
   */
   #define IFNLEN                     61

   /*
   Plane array upper bound.
   */
   #define PLMAX                      4

   /*
   Ellipse array upper bound.
   */
   #define ELMAX                      9


   /*
   Maximum length of kernel architecture specifier, including
   terminating null. This specifier is returned by getfat_c.
   */
   #define KARCLN                     4

   /*
   Maximum length of an illumination angle type name, including
   terminating null.
   */
   #define ANTPLN                     81

#endif

