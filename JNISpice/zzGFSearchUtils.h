



void zzGFSearchUtilsInit ( JNIEnv    * env,
                           jobject     searchUtilsObj );



void zzgfrefn_jni ( SpiceDouble     t1,
                    SpiceDouble     t2,
                    SpiceBoolean    s1,
                    SpiceBoolean    s2,
                    SpiceDouble    *t  );



void zzgfstep_jni ( SpiceDouble     et,
                    SpiceDouble   * step );



void zzgfsstp_jni ( SpiceDouble  step );


void zzfinalizeReport_jni(void);


void zzupdateReport_jni ( SpiceDouble      ivbeg,
                          SpiceDouble      ivend,
                          SpiceDouble      time    );


void zzinitializeReport_jni ( SpiceCell       * window,
                              ConstSpiceChar  * begmsg,
                              ConstSpiceChar  * endmsg  );

SpiceDouble zzgetTolerance_jni();

SpiceBoolean zzisInterruptHandlingEnabled_jni();

SpiceBoolean zzisReportingEnabled_jni();

SpiceBoolean zzinterruptOccurred_jni( void );
