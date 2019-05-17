/*

-Procedure zzGFSearchUtils ( Private---GF search utilities )

-Abstract

   SPICE Private routine intended solely for the support of SPICE
   routines.  Users should not call this routine directly due
   to the volatile nature of this routine.
   
   Provide GF search step and refinement functions that
   call user-specified Java methods.

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

   GF
   JNISpice

-Keywords

   PRIVATE
   SEARCH
   UTILTITY

*/


#include <jni.h>
#include "SpiceUsr.h"
#include "SpiceZfc.h"
#include "cspice_jni.h"
#include "zzGFSearchUtils.h"

/*
Class scope static variables 
*/


static JNIEnv             * savEnv;
static jclass               GFSearchUtilsClass;
static jmethodID            GFFinalizeReportID;
static jmethodID            GFGetSearchStepID;
static jmethodID            GFGetToleranceID;
static jmethodID            GFInitializeReportID;
static jmethodID            GFInterruptOccurredID;
static jmethodID            GFIsInterruptHandlingEnabledID;
static jmethodID            GFIsReportingEnabledID;
static jmethodID            GFRefinementID;
static jmethodID            GFUpdateReportID;
static jobject              GFSearchUtils;


/*

-Brief_I/O

   See the functions defined in this file for I/O descriptions.

-Detailed_Input

   See the functions defined in this file for I/O descriptions.

-Detailed_Output

   See the functions defined in this file for I/O descriptions.

-Parameters

   None.

-Exceptions

   The functions defined in this file simply return if
   a Java exception is thrown during their operation.

-Files

   None.

-Particulars
    
   This set of functions allows the JNISpice system to perform GF
   searches using and user-defined step size, search refinement, and
   progress reporting routines. An interface for retrieving a
   user-specified convergence tolerance is provided as well.
   These routines allow JNISpice to call the mid-level CSPICE
   GF routines

      gfevnt_c
      gffove_c
      gfocce_c
 
   These functions are used as follows: JNISpice's C bridge code calls
   the initialization function at the start of any GF search that uses
   these routines. Note that this step must be performed for every
   search, not just once at program initialization. This is because the
   calling Java thread may well change between calls to this package.
 
   The intialization step fetches the IDs of the user-defined Java
   methods that are to be called during the search. These IDs are
   stored so they may be used by adapter functions that serve as actual
   arguments passed to the CSPICE GF search routines. The adapters in
   turn call the


   The functions defined in this file are shown below.


      Adapter initialization
      ----------------------
      zzGFSearchUtilsInit


      Search refinement adapter
      -------------------------
      zzgfrefn_jni


      Step size adapter
      -----------------
      zzgfstep_jni 


      Convergence tolerance fetch adapter
      -----------------------------------
      zzgetTolerance_jni


      Interrupt handling adapters
      ---------------------------
      zzisInterruptHandlingEnabled_jni
      zzinterruptOccurred_jni


      GF progress reporting adapters
      ------------------------------
      zzisReportingEnabled_jni
      zzinitializeReport_jni
      zzupdateReport_jni
      zzfinalizeReport_jni



-Examples

   None.

-Restrictions

   For use only within the JNISpice implementation.

-Literature_References
 
   None.

-Author_and_Institution

   N.J. Bachman  (JPL)

-Version

   -JNISpice  Version 1.1.0 09-OCT-2013 (NJB) 

      Changed argument list declaration of 

         zzinterruptOccurred_jni

      from

         ()

      to 

         (void)

      This was done to appease a picky MS Visual Studio
      compiler.

   -JNISpice  Version 1.0.0 30-APR-2010 (NJB) 

-Index_Entries

   private JNISpice GF search utilities

-&
*/






/*
Initialize this set of routines prior to a search.
*/
void zzGFSearchUtilsInit ( JNIEnv    * env,
                           jobject     searchUtilsObj )
{
   /* static SpiceChar  * caller = "zzGFSearchUtilsInit"; */

   /*
   Save the JNI environment and the input object reference.
   */
   savEnv             = env;
   GFSearchUtils      = searchUtilsObj;

   /*
   Get the class of the input object. 
   */
   GFSearchUtilsClass = (*env)->GetObjectClass( env, 
                                                GFSearchUtils );
   JNI_EXC( env );


   /*
   Get the ID of the "get search step" method. 
   */
   GFGetSearchStepID        = (*env)->GetMethodID ( env, 
                                                    GFSearchUtilsClass,
                                                    "getSearchStep",
                                                    "(D)D"                  );
   JNI_EXC( env );


   /*
   Get the ID of the "get search tolerance" method. 
   */
   GFGetToleranceID         = (*env)->GetMethodID ( env, 
                                                    GFSearchUtilsClass,
                                                    "getTolerance",
                                                    "()D"              );

   /*
   Get the ID of the search refinement method. 
   */
   GFRefinementID           = (*env)->GetMethodID ( env, 
                                                    GFSearchUtilsClass,
                                                    "getRefinement",
                                                    "(DDZZ)D"               );
   JNI_EXC( env );


   /*
   Get the ID of the progress report "enabled" status method.
   */
   GFIsReportingEnabledID = (*env)->GetMethodID( env, 
                                                 GFSearchUtilsClass,
                                                 "isReportingEnabled",
                                                 "()Z"               );
   JNI_EXC( env );


   /*
   Get the ID of the interrupt handling "enabled" status method.
   */
   GFIsInterruptHandlingEnabledID = (*env)->GetMethodID( env, 
                                                 GFSearchUtilsClass,
                                                 "isInterruptHandlingEnabled",
                                                 "()Z"                       );
   JNI_EXC( env );


   /*
   Get the ID of the interrupt status method.
   */
   GFInterruptOccurredID = (*env)->GetMethodID( env, 
                                                GFSearchUtilsClass,
                                                "interruptOccurred",
                                                "()Z"               );
   JNI_EXC( env );



   /*
   Get the ID of the progress report finalization method. 
   */
   GFFinalizeReportID = (*env)->GetMethodID( env, 
                                             GFSearchUtilsClass,
                                             "finalizeReport",
                                             "()V" );
   JNI_EXC( env );


   /*
   Get the ID of the progress report initialization method. 
   */
   
   GFInitializeReportID = 
      (*env)->GetMethodID( env, 
      GFSearchUtilsClass,
      "initializeReport",
      "(Lspice/basic/SpiceWindow;Ljava/lang/String;Ljava/lang/String;)V" );

   JNI_EXC( env );


   /*
   Get the ID of the progress report update method. 
   */
   GFUpdateReportID = (*env)->GetMethodID( env, 
                                           GFSearchUtilsClass,
                                           "updateReport",
                                           "(DDD)V" );
   JNI_EXC( env );



}
                              

/*
Call the GFSearchUtils "getRefinement" method. 
This routine has a prototype compatible with that required 
by the CSPICE GF mid-level APIs.
*/
void zzgfrefn_jni ( SpiceDouble     t1,
                    SpiceDouble     t2,
                    SpiceBoolean    s1,
                    SpiceBoolean    s2,
                    SpiceDouble   * t  )
{
   /*
   Call the GFSearchUtils getRefinement method, passing
   in the arguments passed to this routine.
   */
   
   *t  =  (*savEnv)->CallDoubleMethod( savEnv, 
                                       GFSearchUtils,
                                       GFRefinementID,
                                       t1,
                                       t2,
                                       s1,
                                       t2                        );
}                     


/*
Call the GFSearchUtils "getSearchStep" method. 
This routine has a prototype compatible with that required 
by the CSPICE GF mid-level APIs.
*/
void zzgfstep_jni ( SpiceDouble     et,
                    SpiceDouble   * step )
{
   /*
   Call the GFSearchUtils getSearchStep method, passing
   in the arguments passed to this routine.
   */

   *step  =  (*savEnv)->CallDoubleMethod( savEnv, 
                                          GFSearchUtils,
                                          GFGetSearchStepID,
                                          et                         );
}                     


/*
Call the GFSearchUtils "getTolerance" method. 
*/
SpiceDouble zzgetTolerance_jni()
{
   SpiceDouble             tol;


   /*
   Call the GFSearchUtils getTolerance method.
   */

   tol  =  (*savEnv)->CallDoubleMethod( savEnv, 
                                        GFSearchUtils,
                                        GFGetToleranceID );

   return( tol );
}                     


/*
Call the GFSearchUtils "is reporting enabled" method. 
*/
SpiceBoolean zzisReportingEnabled_jni()
{
   SpiceBoolean            enabled;

   /*
   Call the GFSearchUtils isReportEnabled method.
   */

   enabled  =  (*savEnv)->CallBooleanMethod( savEnv, 
                                             GFSearchUtils,
                                             GFIsReportingEnabledID );

   return( enabled );
}                     



/*
Call the GFSearchUtils "is interrupt handling enabled" method. 
*/
SpiceBoolean zzisInterruptHandlingEnabled_jni()
{
   SpiceBoolean            enabled;

   /*
   Call the GFSearchUtils isInterruptEnabled method.
   */

   enabled  =  (*savEnv)->CallBooleanMethod( savEnv, 
                                             GFSearchUtils,
                                             GFIsInterruptHandlingEnabledID );

   return( enabled );
}                     




/*
Call the GFSearchUtils "interruptOccurred" method. 
This routine has a prototype compatible with that required 
by the CSPICE GF mid-level APIs.
*/
SpiceBoolean zzinterruptOccurred_jni( void )
{
   /*
   Local variables 
   */
   SpiceBoolean            occurred;

   occurred = (*savEnv)->CallBooleanMethod( savEnv, 
                                            GFSearchUtils,
                                            GFInterruptOccurredID );

   return( occurred );
}




/*
Call the GFSearchUtils "initializeReport" method. 
This routine has a prototype compatible with that required 
by the CSPICE GF mid-level APIs.
*/
void zzinitializeReport_jni ( SpiceCell       * window,
                              ConstSpiceChar  * begmsg,
                              ConstSpiceChar  * endmsg  )
{
   /*
   Local variables 
   */
   jclass                  windowClass;
   jobject                 jSpiceWindow;
   jdoubleArray            jwindowArray;
   jmethodID               windowConsID;
   jstring                 jBegmsg;
   jstring                 jEndmsg;
   SpiceInt                card;

   /*
   Create the Java objects we'll need in order to call the
   Java GF progress report initialization routine. 
   */
   jBegmsg = createJavaString_jni( savEnv, (SpiceChar *)begmsg );
   jEndmsg = createJavaString_jni( savEnv, (SpiceChar *)endmsg );

   card = card_c( window );

   /*
   Create a Java SpiceWindow from the input CSPICE window.
   We'll need to call a SpiceWindow constructor.

   Note: JNI documentation says the package name is delimited using
   the '/' character. 
   */
   windowClass = (*savEnv)->FindClass( savEnv, "spice/basic/SpiceWindow" );

   JNI_EXC( savEnv );

   windowConsID = (*savEnv)->GetMethodID( savEnv, 
                                          windowClass,
                                          "<init>",
                                          "([D)V"      );

   JNI_EXC( savEnv );

   createVecGD_jni( savEnv, card, window->data, &jwindowArray );   
   
   
   jSpiceWindow = (*savEnv)->NewObject( savEnv, 
                                        windowClass,
                                        windowConsID,
                                        jwindowArray   );


   /*
   Call the GFSearchUtils initializeReport method, passing
   in the arguments passed to this routine.
   */

   (*savEnv)->CallVoidMethod( savEnv, 
                              GFSearchUtils,
                              GFInitializeReportID,
                              jSpiceWindow,
                              jBegmsg,
                              jEndmsg              );
}                     




/*
Call the GFSearchUtils "updateReport" method. 
This routine has a prototype compatible with that required 
by the CSPICE GF mid-level APIs.
*/
void zzupdateReport_jni ( SpiceDouble      ivbeg,
                          SpiceDouble      ivend,
                          SpiceDouble      time    )
{
   /*
   Call the GFSearchUtils initializeReport method, passing
   in the arguments passed to this routine.
   */

   (*savEnv)->CallVoidMethod( savEnv, 
                              GFSearchUtils,
                              GFUpdateReportID,
                              ivbeg,
                              ivend,
                              time               );
}                                          


/*
Call the GFSearchUtils "finalizeReport" method. 
This routine has a prototype compatible with that required 
by the CSPICE GF mid-level APIs.
*/
void zzfinalizeReport_jni(void)
{
   /*
   Call the GFSearchUtils finalizeReport method, passing
   in the arguments passed to this routine.
   */

   (*savEnv)->CallVoidMethod( savEnv, 
                              GFSearchUtils,
                              GFFinalizeReportID    );
}                                          




 
