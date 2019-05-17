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

   -JNISpice Version 1.0.0, 03-DEC-2009 (NJB)

-Index_Entries

   TBD

-&
*/

#include <jni.h>
#include <string.h>
#include "SpiceUsr.h"
#include "SpiceZfc.h"
#include "SpiceZst.h"
#include "cspice_jni.h"



void zzThrowSpiceErrorException_jni ( JNIEnv          * env,
                                      ConstSpiceChar  * caller )
{
   /*
   Local variables 
   */
   SpiceChar               excMsg [ EXMLEN ];
   SpiceChar               lmsg   [ LMSGLN ];
   SpiceChar               smsg   [ SMSGLN ];
   SpiceChar               trace  [ TRCLEN ];
   ConstSpiceChar        * versn;


   

   getmsg_c ( "short", SMSGLN, smsg );
   getmsg_c ( "long",  LMSGLN, lmsg );

   qcktrc_  ( (char *) trace, 
              (ftnlen) TRCLEN-1 );

   /*
   The string returned, output, is a Fortranish type string.
   Convert the string to C style.
   */
   F2C_ConvertStr ( TRCLEN, trace );

   /*
   Re-set the SPICE error status. 
   */
   reset_c();

   /*
   Get the SPICE Toolkit version. 
   */
   versn = tkvrsn_c ( "TOOLKIT" );

   /*
   Create the composite error message. 
   */
   excMsg[0] = 0;

   strncpy ( excMsg, "#: #: #: [#] #", EXMLEN );

   repmc_c ( excMsg, "#", versn,  EXMLEN, excMsg );
   repmc_c ( excMsg, "#", caller, EXMLEN, excMsg );
   repmc_c ( excMsg, "#", smsg,   EXMLEN, excMsg );
   repmc_c ( excMsg, "#", trace,  EXMLEN, excMsg );
   repmc_c ( excMsg, "#", lmsg,   EXMLEN, excMsg );

   /*
   printf ( "zzThrowSpiceErrorException: message = %s\n", excMsg  );
   */

   /*
   Throw a SpiceErrorException using the composite message.
   */
   zzThrowException_jni ( env, SPICE_ERROR_EXC, excMsg );
}
