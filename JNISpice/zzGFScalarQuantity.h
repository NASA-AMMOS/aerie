
#include <jni.h>
#include "SpiceUsr.h"
#include "SpiceZfc.h"
#include "cspice_jni.h"

/*
   Version 1.0.0 29-DEC-2009 (NJB)
*/

void zzGFScalarQuantityInit_jni ( JNIEnv    * env,
                                  jobject     scalarQuantityObj );


void zzudfunc_jni ( SpiceDouble     et,
                    SpiceDouble   * value );



void zzudqdec_jni ( void         (* udfunc) ( SpiceDouble    et,
                                              SpiceDouble  * value ),
                    SpiceDouble     et,
                    SpiceBoolean  * isdecr );


/*
   End of header file
*/


