package spice.tspice;

import java.io.*;
import static java.lang.Math.PI;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.RPD;



/**
Class TestSubSolarRecord provides methods that implement test families for
the class SubSolarRecord.


<h3> Version 1.0.0 23-JAN-2017 (NJB)</h3>

*/
public class TestSubSolarRecord
{


   /**
   Test family 001 for methods of the class spice.basic.SubSolarRecord.
   <pre>
   -Procedure f_SubSolarRecord (Test SubSolarRecord)

   -Copyright

      Copyright (2004), California Institute of Technology.
      U.S. Government sponsorship acknowledged.

   -Required_Reading

      None.

   -Keywords

      TESTING

   -Brief_I/O

      VARIABLE  I/O  DESCRIPTION
      --------  ---  --------------------------------------------------

      The method returns the boolean true if all tests pass,
      false otherwise.

   -Detailed_Input

      None.

   -Detailed_Output

      The method returns the boolean true if all tests pass,
      false otherwise.  If any tests fail, diagnostic messages
      are sent to the test logger.

   -Parameters

      None.

   -Files

      None.

   -Exceptions

      Error free.

   -Particulars

      This routine tests methods of class SubSolarRecord.

   -Examples

      None.

   -Restrictions

      None.

   -Author_and_Institution

      N.J. Bachman   (JPL)
      E.D. Wright    (JPL)

   -Literature_References

      None.

   -Version

      -JNISpice Version 1.0.0 23-JAN-2017 (NJB) (EDW)

   -&
   </pre>
   */

   public static boolean f_SubSolarRecord()

      throws SpiceException
   {
      //
      // Local constants
      //
      final String                   DSK0 = "subpnt_dsk0.bds";    
      final String                   DSK1 = "subpnt_dsk1.bds";    
      final String                   DSK2 = "subpnt_dsk2.bds";    
      final String                   DSK3 = "subpnt_dsk3.bds";    
      final String                   PCK0 = "subpnt_test.tpc";    
      final String                   SPK0 = "subpnt_test.bsp";    
      final String                   SPK1 = "orbiter.bsp";    

      final double                   LOOSE  = 5.e-6;
      final double                   MTIGHT = 1.e-10;
      final double                   VTIGHT = 1.e-14;
      final double                   TIGHT  = 1.e-12;
 
      final int                      NCORR  = 5;
      final int                      NMAP   = 4;
      final int                      NMETH  = 8;
      final int                      NOBS   = 2;
      final int                      NREFS  = 1;
      final int                      NSHAPE = 2;
      final int                      NTARG  = 2;
      final int                      NTIMES = 2;
      final int                      SCID   = -499;

  
      //
      // Local variables
      //
      AberrationCorrection           abcorr;
      AberrationCorrection[]         corrs = 
                                     { 
                                        new AberrationCorrection( "None"  ),
                                        new AberrationCorrection( "Lt"    ),
                                        new AberrationCorrection( "Lt+S"  ),
                                        new AberrationCorrection( "Cn"    ),
                                        new AberrationCorrection( "Cn+s"  )
                                     };

      Body                           center = new Body( "Mars" );

      Body[]                         obs    = {
                                                new Body( "Earth"        ),
                                                new Body( "MARS_ORBITER" ),  
                                              };
      Body                           obsrvr;
      Body                           orbiter = new Body( SCID  );
      Body                           sun     = new Body( "sun" );
      Body                           target;

      Body[]                         targs = {
                                                new Body( "Mars"   ),
                                                new Body( "Phobos" ),  
                                             };

      DSK                            dsk0;

      Ellipsoid                      elipsd;
     
      EllipsoidPointNearPoint        enearp;

      OsculatingElements             oscElts;

      PositionRecord                 trgpos;

      Ray                            ray;

      RayEllipsoidIntercept          rayx;

      ReferenceFrame                 fixref;

      ReferenceFrame[][]             refs =
                                     {
                                        { new ReferenceFrame( 
                                           "IAU_MARS" )       }, 
                                        { new ReferenceFrame( 
                                           "IAU_PHOBOS" )     },
                                     };

      ReferenceFrame                 marsiau = new ReferenceFrame( "MARSIAU" );

      SPK                            spk1;
      
      String                         label;
      String                         method; 

      String[]                       methds = {

                    "ELLIPSOID / intercept",
                    "intercept: ellipsoid",
                    "near point: ellipsoid",
                    "near point /ellipsoid",
                    "nadir/dsk/unprioritized/surfaces=\"high-res\"",
                    "dsk/ nadir /unprioritized/surfaces=\"high-res\"",
                    "intercept/ UNPRIORITIZED/ dsk /SURFACES =\"LOW-RES\"",
                    "intercept/UNPRIORITIZED/ dsk /SURFACES =\"LOW-RES\""   

                                     };

      String[]                       obsnms;
      String                         sfnmth;
      String                         shape;
      String[]                       shapes = { "DSK", "Ellipsoid" };
      String[]                       srfnms;
      String                         title;
      String                         utc;

      StateRecord                    state;
      StateRecord                    sunState;

      StateVector                    state0;
      StateVector[]                  states;
 
      Surface[]                      srflst;

      SurfaceIntercept[]             srfxArr;

      SubSolarRecord                 subsol;
      SubSolarRecord                 subsol0;
      SubSolarRecord                 subsol1;

      TDBDuration                    tdelta;

      TDBTime                        epoch;
      TDBTime[]                      epochs;
      TDBTime                        et;
      TDBTime                        et0;
      TDBTime                        first;
      TDBTime                        last;
      TDBTime                        trgepc;
      TDBTime                        xepoch;

      Vector3                        obspos;
      Vector3[]                      rayDirs;
      Vector3[]                      rayVrts;
      Vector3                        spoint;
      Vector3                        srfvec;
      Vector3                        subdir;
      Vector3                        sunpos;
      Vector3                        xsubpt;
      Vector3                        xsrfvc;

      boolean                        found;
      boolean                        isdsk;
      boolean                        isintr;
      boolean                        ok;
      boolean                        pri;

      double                         dlat;
      double                         dlon;
      double[]                       elts   = new double[8];
      double                         etol;
      double[]                       radii;
      double                         tol;
      double                         tolscl;

      int[]                          badcds;
      int                            bodyid;
      int                            coridx;
      int                            han1;
      int                            i;
      int                            mix;
      int                            nlat;
      int                            nlon;
      int[]                          obscds;
      int                            obsidx;
      int                            refidx;
      int                            shapix;
      int                            spkhan;
      int[]                          srfbod;
      int[]                          srfids;
      int                            surfid;
      int                            trgidx;
      int                            timidx;

      //
      // Start tests.
      //


      try
      {

         JNITestutils.topen ( "f_SubSolarRecord" );

        
         //*********************************************************************
         //
         //  Set up.
         //
         //*********************************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Setup: create PCK, SPK files." );

         //
         // Create generic test SPK. Load via keeper system.
         //
         han1 = JNITestutils.tstspk( SPK0, false );

         KernelDatabase.load( SPK0 );

         //
         // Create generic test PCK. Load via keeper system.
         //
         JNITestutils.tstpck( PCK0, false, true );
       
         KernelDatabase.load( PCK0 );

         //
         // Create generic test LSK. Load and delete file.
         //
         JNITestutils.tstlsk();

         //
         // Set initial time.
         //

         utc    = "2004 FEB 17";

         et0    = new TDBTime( utc );

         et     = et0;

         tdelta = new TDBDuration(  CSPICE.jyear()  );


         //
         //  Create a Mars orbiter SPK file.
         //
         spk1   = SPK.openNew( SPK1, SPK1, 0 );

         //
         //
         //  Set up elements defining a state.  The elements expected
         //  by CONICS are:
         //
         //     RP      Perifocal distance.
         //     ECC     Eccentricity.
         //     INC     Inclination.
         //     LNODE   Longitude of the ascending node.
         //     ARGP    Argument of periapse.
         //     M0      Mean anomaly at epoch.
         //     T0      Epoch.
         //     MU      Gravitational parameter.
         //
         elts[0] =  3800.0;
         elts[1] =     1.e-1;
         elts[2] =    80.0 * RPD;
         elts[3] =     0.0;
         elts[4] =    90.0 * RPD;
         elts[5] =     0.0;
         elts[6] =    et.getTDBSeconds();
         elts[7] = 42828.314;

         oscElts = new OsculatingElements( elts );

         state0  = oscElts.propagate( et );
 
         first   = new TDBTime( -10*CSPICE.jyear() );
         last    = new TDBTime(  10*CSPICE.jyear() );

         states    = new StateVector[1];
         states[0] = state0;
         
         epochs    = new TDBTime[1];
         epochs[0] = et;

         spk1.writeType05Segment( orbiter, center, marsiau, first, last,
                                  "Mars Orbiter",  elts[7], 1,     states, 
                                  epochs                                   );

         spk1.close();

         //
         // Load the new SPK file.
         //
         KernelDatabase.load( SPK1 );

         //
         //  Add the orbiter's name/ID mapping to the kernel pool.
         //
         obsnms    = new String[1];
         obscds    = new int[1];
         //
         // Note: index should be 0 on LHS and 1 on RHS.
         //
         obsnms[0] = obs[1].getName();
         obscds[0] = SCID;

         KernelPool.putCharacter( "NAIF_BODY_NAME", obsnms ); 
         KernelPool.putInteger  ( "NAIF_BODY_CODE", obscds ); 

         //
         //  Add an incomplete frame definition to the kernel pool;
         //  we'll need this later.
         //

         badcds    = new int[1];
         badcds[0] = -666;

         KernelPool.putInteger  ( "FRAME_BAD_NAME", badcds ); 


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Setup: create DSK files." );

         //
         //  For Mars, surface 1 is the "main" surface.
         // 
         target = new Body( "Mars" );
         bodyid = target.getIDCode();
         fixref = new ReferenceFrame( "IAU_MARS" );

         surfid = 1;
         nlon   = 200;
         nlat   = 100;

         //
         // If the DSK already exists, delete it. Otherwise we'd append to
         // it, which is not what we want.
         //
         ( new File( DSK0 ) ).delete();

         JNITestutils.t_elds2z ( bodyid, surfid, fixref.getName(), 
                                 nlon,   nlat,   DSK0              );
         //
         // Load main Mars DSK.
         //
         KernelDatabase.load( DSK0 );


         //
         //  Surface 2 for Mars is very low-res.
         //
         bodyid = target.getIDCode();
         surfid = 2;
         nlon   = 40;
         nlat   = 20;

         ( new File( DSK1 ) ).delete();

         JNITestutils.t_elds2z ( bodyid, surfid, fixref.getName(), 
                                 nlon,   nlat,   DSK1              );

         //
         // Surface 1 for Phobos is low-res.
         //
         target = new Body( "Phobos" );
         bodyid = target.getIDCode();
         fixref = new ReferenceFrame( "IAU_Phobos" );
         surfid = 1;

         nlon   = 200;
         nlat   = 100;

         ( new File( DSK2 ) ).delete();

         //
         // Create and load the first Phobos DSK.
         //
         JNITestutils.t_elds2z ( bodyid, surfid, fixref.getName(), 
                                 nlon,   nlat,   DSK2              );

         KernelDatabase.load( DSK2 );

         //
         //  Surface 2 for Phobos is lower-res.
         //
         surfid = 2;
           
         nlon   = 80;
         nlat   = 40;

         ( new File( DSK3 ) ).delete();

         //
         // Create and load the second Phobos DSK.
         //
         JNITestutils.t_elds2z ( bodyid, surfid, fixref.getName(), 
                                 nlon,   nlat,   DSK3              );

         KernelDatabase.load( DSK3 );

      
         //
         // Set up a surface name-ID map.
         //
         srfbod    = new int[NMAP];
         srfids    = new int[NMAP];
         srfnms    = new String[NMAP];

         srfbod[0] = 499;
         srfids[0] = 1;
         srfnms[0] = "high-res";

         srfbod[1] = 499;
         srfids[1] = 1;
         srfnms[1] = "low-res";

         srfbod[2] = 401;
         srfids[2] = 1;
         srfnms[2] = "high-res";

         srfbod[3] = 401;
         srfids[3] = 1;
         srfnms[3] = "low-res";

         KernelPool.putCharacter( "NAIF_SURFACE_NAME", srfnms ); 
         KernelPool.putInteger  ( "NAIF_SURFACE_CODE", srfids ); 
         KernelPool.putInteger  ( "NAIF_SURFACE_BODY", srfbod ); 


         //*********************************************************************
         //
         //  Main test loop
         //
         //*********************************************************************

         //
         // Loop over every choice of observer.
         //
         for ( obsidx = 0;  obsidx < NOBS;  obsidx++ )
         {
            obsrvr = obs[obsidx];

          
            //
            // Loop over every choice of target.
            //
            for ( trgidx = 0;  trgidx < NTARG;  trgidx++ )
            {

               target = targs[trgidx]; 

               //
               //  Get target radii.
               //
               radii = target.getValues( "RADII" );
          
               //
               // Loop over the time sequence.
               //
               for ( timidx = 0;  timidx < NTIMES;  timidx++ )
               {
                  //
                  // Use `tdelta' as the time step.
                  //
                  et = et0.add( tdelta.scale( timidx ) );
 
                  //
                  // Loop over every aberration correction choice.
                  //
                  for ( coridx = 0;  coridx < NCORR;  coridx++ )
                  {
                     abcorr = corrs[coridx];

                     //
                     // Loop over every target body-fixed frame choice.
                     //
                     for ( refidx = 0;  refidx < NREFS;  refidx++ )
                     {
                        fixref = refs[trgidx][refidx];

                        //
                        // Loop over all method choices.
                        //
                        for ( mix = 0;  mix < NMETH;  mix++ )
                        {
                           method = methds[mix];

                           //
                           // --------Case--------------------------------------
                           //

                           title = String.format( 
                                     "Compute sub-solar point.  " + 
                                     "Method = %s. "                 +
                                     "Observer = %s; target = %s;  " +
                                     "Aberration correction = %s;  " +
                                     "Target frame = %s; time = %s.",
                                     method,
                                     obsrvr.getName(),
                                     target.getName(),
                                     abcorr.getName(),
                                     fixref.getName(),
                                     et.toString()                    );

                           JNITestutils.tcase ( title );


                           //
                           // Start off by computing the sub-solar point.
                           // We'll then check the results.
                           //
                           subsol = new 

                              SubSolarRecord( method, target, et, 
                                              fixref, abcorr, obsrvr );

                           trgepc = subsol.getTargetEpoch();
                           srfvec = subsol.getSurfaceVector();
       

                           //
                           // We'll treat the computed sub-solar point as
                           // an ephemeris object and find its position
                           // relative to the observer.
                           //
                           state = new 

                              StateRecord( subsol, target,   fixref, et,
                                           fixref, "TARGET", abcorr, obsrvr );
                           // 
                           // If `subsol' is correct, then the position of
                           // `subsol' relative to the observer should be equal
                           // `srfvec'. The light time obtained from `state'
                           // should match that implied by `trgepc'.
                           //


                           //
                           // Derive a target epoch from `state'. Set the
                           // surface vector tolerance as well.
                           //
                           if ( abcorr.hasLightTime() )
                           {
                              if ( abcorr.isReceptionType() )
                              {
                                 xepoch = et.sub( state.getLightTime() );
                              }
                              else
                              {
                                 xepoch = et.add( state.getLightTime() );
                              }

                              if ( abcorr.isConvergedNewtonian() )
                              {
                                 tol = MTIGHT;
                              }
                              else
                              {
                                 tol = LOOSE;
                              }
                           }
                           else
                           {
                              xepoch = et;
                              tol    = VTIGHT;
                           }

                           ok = JNITestutils.chckad( 

                                   "srfvec",
                                   srfvec.toArray(),
                                   "~~/",
                                   state.getPosition().toArray(),
                                   tol                            );
 
                           //
                           // Check target epoch.
                           //
                           etol = TIGHT;

                           ok = JNITestutils.chcksd( "trgepc", 
                                                     trgepc.getTDBSeconds(),
                                                     "~/",
                                                     xepoch.getTDBSeconds(),
                                                     etol                   );

                           //
                           // We've checked the consistency of `subsol',
                           // `srfvec', and `trgepc', but we haven't done
                           // anything to show that `subsol' is a sub-solar
                           // point. Do that now.

 
                           //
                           // Compute the state of the sun relative to
                           // the sub-solar point, as seen from the sub-solar
                           // point at `trgepc'.
                           //
                           sunState = new 

                             StateRecord( sun,    trgepc, fixref, "OBSERVER",
                                          abcorr, subsol, target, fixref     );

                           //
                           // Compute the position of the sun relative to the
                           // the target body's center.
                           //
                           sunpos = sunState.getPosition().add( subsol );

                           //
                           // The low-level form of the sub-solar point 
                           // computation depends on the type of target 
                           // shape model and the computation type.
                           // 
                           isdsk  = method.toUpperCase().indexOf( "DSK" ) > -1; 

                           isintr = method.toUpperCase().
                                           indexOf( "INTERCEPT" ) > -1; 


                           //
                           // For the following tests, we must scale the 
                           // tolerance to allow for the fact that the error
                           // in the target-sun vector may be much larger
                           // than the error in the sub-solar point.
                           //
                           tolscl =   tol
                                    * Math.max( 1.0, sunpos.norm() / 
                                                     subsol.norm()   );


                           if ( isdsk ) 
                           {
                              //
                              // The target shape is modeled as a DSK.
                              //

                              rayVrts    = new Vector3[1];
                              rayDirs    = new Vector3[1];


                              if ( isintr )
                              {
                                 //
                                 // Find the intercept on the target's DSK model
                                 // of the ray having vertex `sunpos' and 
                                 // direction opposite to `sunpos'.
                                 //

                                 rayVrts[0] = sunpos;
                                 rayDirs[0] = sunpos.negate();

                              }
                              else
                              {
                                 //
                                 // This is the nadir case. The ray we want
                                 // points from the observer to the nearest
                                 // point on the reference ellipsoid.
                                 //
                                 elipsd = new Ellipsoid( radii[0], 
                                                         radii[1], radii[2] );

                                 enearp = new 
                           
                                    EllipsoidPointNearPoint( elipsd, sunpos );

                                 rayVrts[0] = sunpos;
                                 rayDirs[0] = enearp.sub( sunpos );

                              }

                              //
                              // We have only one ray, but in this
                              // case, SurfaceIntercept.create is the
                              // simplest low-level method of finding
                              // the intercept.
                              //                              
                              pri        = false;

                              srflst     = new Surface[0];

                              srfxArr    = 

                                    SurfaceIntercept.create( pri, 
                                                             target, 
                                                             srflst,
                                                             trgepc,
                                                             fixref,
                                                             rayVrts,
                                                             rayDirs  );
                                                                 
                              //
                              // Verify that the intercept exists.
                              //
                              ok = JNITestutils.

                                      chcksl( "found", 
                                              srfxArr[0].wasFound(),
                                              true                   );

                              spoint = srfxArr[0].getIntercept();

                              ok = JNITestutils.chckad( "spoint", 
                                                        spoint.toArray(), 
                                                        "~~/",    
                                                        subsol.toArray(),
                                                        tolscl           );
                           }
                           else  
                           {
                              //
                              // The target shape is modeled as an ellipsoid.
                              //
                              elipsd = new Ellipsoid( radii[0], 
                                                      radii[1], radii[2] );

                              if ( isintr )
                              {
                                 //
                                 // Find the intercept on the 
                                 // target's ellipsoidal model of the ray having
                                 // vertex `sunpos' and direction opposite to
                                 // `sunpos'.
                                 //

                                 ray  = new Ray( sunpos, sunpos.negate() );

                                 rayx = new RayEllipsoidIntercept( ray, 
                                                                   elipsd );

                                 ok   = JNITestutils.chcksl( "found",
                                                             rayx.wasFound(),
                                                             true            );
                                 //
                                 // Check the intercept point.
                                 //
                                 // Use the tolerance we set for the `srfvec'
                                 // test.
                                 //
                                 ok = JNITestutils.

                                         chckad( "rayx", 
                                                 rayx.getIntercept().toArray(), 
                                                 "~~/",    
                                                 subsol.toArray(),
                                                 tolscl                       );

                              }
                              else
                              {
                                 //
                                 // Find the nearest point to `sunpos' on the 
                                 // target's ellipsoidal model.
                                 //

                                 enearp = new 
                           
                                    EllipsoidPointNearPoint( elipsd, sunpos );

                                 //
                                 // Check the near point.
                                 //
                                 // Use the tolerance we set for the `srfvec'
                                 // test.
                                 //
                                 ok = JNITestutils.chckad( "enearp", 
                                                           enearp.toArray(), 
                                                           "~~/",    
                                                           subsol.toArray(),
                                                           tolscl           );
                              }
                              
                           }
                           //
                           // End of tests on sub-solar point.
                           //

    

                        }
                        //
                        // End of method loop.
                        //
                     }
                     //
                     // End of target frame loop.
                     //
                  }
                  //
                  // End of aberration correction loop.
                  //
               }
               //
               // End of time loop.
               //
            }
            //
            //  End of the target loop.
            //
         }
         //
         //  End of the observer loop.
         //


         //*********************************************************************
         //
         //  Error cases
         //
         //*********************************************************************

         //
         // Generate an error at the CSPICE level.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "No observer data available." );
 
         target = targs[0];
         obsrvr = new Body( "Gaspra" );
         fixref = refs [0][0];
         abcorr = corrs[0];
         et     = et0;
         method = methds[0];

         try
         {
            subsol = new SubSolarRecord( method, target, et, 
                                            fixref, abcorr, obsrvr );

            Testutils.dogDidNotBark( "SPICE(SPKINSUFFDATA)" );

         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(SPKINSUFFDATA)", exc );
         }




         //*********************************************************************
         //
         //  Constructor tests
         //
         //*********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SubSolarRecord no-args constructor" );
 
         //
         // Make sure we can make the call.
         //
         subsol = new SubSolarRecord();


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SubSolarRecord copy constructor" );

         // 
         // Create two sub-solar records using identical inputs.
         //
         target = targs[0];
         obsrvr = obs  [0];
         fixref = refs [0][0];
         abcorr = corrs[0];
         et     = et0;
         method = methds[0];

         subsol0 = new SubSolarRecord( method, target, et, 
                                          fixref, abcorr, obsrvr );
                                    
         subsol1 = new SubSolarRecord( method, target, et, 
                                          fixref, abcorr, obsrvr );
         //
         // Make a copy of subsol0.
         // 
         subsol = new SubSolarRecord( subsol0 );

         //
         // Test the copy. We expect exact equality.
         //
         ok = JNITestutils.chcksd ( "trgepc",
                                    subsol.getTargetEpoch().getTDBSeconds(),
                                    "=",
                                    subsol0.getTargetEpoch().getTDBSeconds(),
                                    0.0                                      );

         ok = JNITestutils.chckad( "spoint", 
                                   subsol.toArray(), 
                                   "=",    
                                   subsol0.toArray(),
                                   0.0               );
 
         ok = JNITestutils.chckad( "srfvec", 
                                   subsol.getSurfaceVector().toArray(), 
                                   "=",    
                                   subsol0.getSurfaceVector().toArray(),
                                   0.0                                  );
                                    

         //
         // Modify subsol0. Verify that subsol doesn't change.
         //
         target = targs[0];
         obsrvr = obs  [1];
         fixref = refs [0][0];
         abcorr = corrs[1];
         et     = et0;
         method = methds[0];

         subsol0 = new SubSolarRecord( method, target, et, 
                                          fixref, abcorr, obsrvr );

         ok = JNITestutils.chcksd ( "trgepc",
                                    subsol.getTargetEpoch().getTDBSeconds(),
                                    "=",
                                    subsol1.getTargetEpoch().getTDBSeconds(),
                                    0.0                                      );

         ok = JNITestutils.chckad( "spoint", 
                                   subsol.toArray(), 
                                   "=",    
                                   subsol1.toArray(),
                                   0.0               );
 
         ok = JNITestutils.chckad( "srfvec", 
                                   subsol.getSurfaceVector().toArray(), 
                                   "=",    
                                   subsol1.getSurfaceVector().toArray(),
                                   0.0                                  );


      }
      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         ok = JNITestutils.chckth ( false, "", ex );
      }

      finally
      {
         //*********************************************************************
         //
         //  Clean up.
         //
         //*********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Clean up SPICE kernels" );

         //
         // Unload all kernels and clear the kernel pool.
         //
         KernelDatabase.clear();
 
         //
         // Delete kernel files.
         //
         ( new File( PCK0 ) ).delete();
         ( new File( SPK0 ) ).delete();
         ( new File( SPK1 ) ).delete();
         ( new File( DSK0 ) ).delete();
         ( new File( DSK1 ) ).delete();
         ( new File( DSK2 ) ).delete();
         ( new File( DSK3 ) ).delete();      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

} /* End f_LimbPoint */











