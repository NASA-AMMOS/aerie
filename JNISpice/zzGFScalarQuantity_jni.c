/*

-Procedure TBD ( TBD )

-Abstract

   TBD

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

   TBD

-Keywords

   TBD

*/


/*

-Brief_I/O

   VARIABLE  I/O  DESCRIPTION
   --------  ---  --------------------------------------------------
   TBD

-Detailed_Input

   TBD

-Detailed_Output

   TBD

-Parameters

   TBD

-Exceptions

   TBD

-Files

   TBD

-Particulars

   TBD

-Examples

   TBD

-Restrictions

   TBD

-Literature_References

   TBD

-Author_and_Institution

   N.J. Bachman  (JPL)

-Version

   -JNISpice Version 1.0.0, 31-DEC-2009 (NJB)

-Index_Entries

   TBD

-&
*/

/*
   Provide GF user-defined scalar quantity search routines that
   call user-specified Java methods.

   Version 1.0.0 31-DEC-2009 (NJB) 
*/

#include <jni.h>
#include "SpiceUsr.h"
#include "SpiceZfc.h"
#include "cspice_jni.h"

/*
Class scope static variables 
*/


static JNIEnv             * savEnv;
static jclass               GFScalarQuantityClass;
static jmethodID            GFGetQuantityID;
static jmethodID            GFIsDecreasingID;
static jobject              GFScalarQuantity;

/*
Initialize this set of routines prior to a search.
*/
void zzGFScalarQuantityInit_jni ( JNIEnv    * env,
                                  jobject     scalarQuantityObj )
{
   /* static SpiceChar  * caller = "zzGFSearchUtilsInit"; */

   /*
   Save the JNI environment and the input object reference.
   */
   savEnv             = env;
   GFScalarQuantity   = scalarQuantityObj;

   /*
   Get the class of the input object. 
   */
   GFScalarQuantityClass = (*env)->GetObjectClass( env, 
                                                   GFScalarQuantity );
   JNI_EXC( env );


   /*
   Get the ID of the "get Quantity" method. 
   */
   GFGetQuantityID       = (*env)->GetMethodID ( env, 
                                                 GFScalarQuantityClass,
                                                 "getQuantity",
                                                 "(D)D"                  );
   JNI_EXC( env );


   /*
   Get the ID of the "is quantity decreasing" method. 
   */
   GFIsDecreasingID      = (*env)->GetMethodID ( env, 
                                                 GFScalarQuantityClass,
                                                 "isQuantityDecreasing",
                                                 "(D)Z"                  );
   JNI_EXC( env );
}
                              


/*
Call the GFScalarQuantity "getQuantity" method. 
This routine has a prototype compatible with that required 
by the CSPICE GF mid-level APIs.
*/
void zzudfunc_jni ( SpiceDouble     et,
                    SpiceDouble   * value )

{
   /*
   Return on entry of an exception has occurred.
   */
   JNI_EXC( savEnv );


   /*
   Call the GFScalarQuantity getQuantity method, passing
   in the arguments passed to this routine.
   */
   
   *value  =  (*savEnv)->CallDoubleMethod( savEnv, 
                                           GFScalarQuantity,
                                           GFGetQuantityID,
                                           et               );
}                     

 
/*
Call the GFScalarQuantity "is quantity decreasing" method. 
This routine has a prototype compatible with that required 
by the CSPICE GF mid-level APIs.
*/
void zzudqdec_jni ( void (* udfunc ) ( SpiceDouble   et,
                                       SpiceDouble * value ),

                    SpiceDouble        et,
                    SpiceBoolean     * isdecr                )

{
   /*
   Return on entry of an exception has occurred.
   */
   JNI_EXC( savEnv );


   /*
   Call the GFScalarQuantity "is decreasing" method, passing
   in the time and boolean pointer arguments passed to this routine.
   The input user defined quantity function pointer is not needed.
   */
   
   *isdecr  =  (*savEnv)->CallBooleanMethod( savEnv, 
                                             GFScalarQuantity,
                                             GFIsDecreasingID,
                                             et               );
}                     

 
                                         




 
