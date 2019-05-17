package spice.tspice;

import java.io.*;
import static java.lang.Math.PI;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.RPD;



/**
Class TestTerminatorPoint provides methods that implement test families for
the class TerminatorPoint.


<h3> Version 1.0.0 03-JAN-2017 (NJB)</h3>

*/
public class TestTerminatorPoint
{


   /**
   Test family 001 for methods of the class spice.basic.TerminatorPoint.
   <pre>
   -Procedure f_TerminatorPoint (Test TerminatorPoint)

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

      This routine tests methods of class TerminatorPoint.

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

      -JNISpice Version 1.0.0 03-DEC-2016 (NJB) (EDW)

   -&
   </pre>
   */

   public static boolean f_TerminatorPoint()

      throws SpiceException
   {
      //
      // Local constants
      //
      final String                   DSK0 = "terminatorpt_test0.bds";    
      final String                   DSK1 = "terminatorpt_test1.bds";    
      final String                   PCK0 = "terminatorpt_test.tpc";    
      final String                   SPK0 = "terminatorpt_test.bsp";    

      final double                   LOOSE  = 1.e-6;
      final double                   MED    = 1.e-8;
      final double                   VTIGHT = 1.e-14;
      final double                   TIGHT  = 1.e-12;

      final int                      MAXCUT = 1000;
      final int                      MAXN   = 10000;
      final int                      NCORR  = 3;
      final int                      NLOC   = 2;
      final int                      NOBS   = 2;
      final int                      NSHAPE = 2;
      final int                      NSRC   = 2;
      final int                      NTARGS = 2;
      final int                      NTIMES = 2;


  
      //
      // Local variables
      //
      AberrationCorrection           abcorr;
      AberrationCorrection[]         corrs = 
                                     { 
                                        new AberrationCorrection( "None" ),
                                        new AberrationCorrection( "CN"   ),
                                        new AberrationCorrection( "CN+S" )
                                     };

      Body[]                         obs   = {
                                                new Body( "Earth" ),
                                                new Body( "Sun"   ),  
                                             };

      Body                           ilusrc;
      Body                           obsrvr;

      Body[]                         srcs  = {
                                                new Body( "Sun"  ),
                                                new Body( "Moon" )
                                             };

      Body                           target;
      Body[]                         targs = {
                                                new Body( "Mars"   ),
                                                new Body( "Phobos" ),  
                                             };

      Ellipse                        srclmb;

      EllipsePlaneIntercept          ellplx;

      Ellipsoid                      ellpsd;
      Ellipsoid                      srcshp;

      TerminatorPoint[][]            eterm;
      TerminatorPoint[][]            term;
      TerminatorPoint                tpoint;

      Plane                          cutpln;

      PositionVector                 ilupos;
      PositionVector                 trgpos;

      Ray                            ray;

      RayEllipsoidIntercept          rayx;

      ReferenceFrame                 fixref;

      ReferenceFrame[]               frames =
                                     {
                                        new ReferenceFrame( 
                                           "IAU_MARS" ), 
                                        new ReferenceFrame( 
                                           "SUN_PHOBOS_VIEW_ZY" )
                                     };

      StateRecord                    lptsta;

      String                         corloc;

      String[]                       frmbuf = 
      {
         "FRAME_SUN_PHOBOS_VIEW_ZY        = 401000",
         "FRAME_401000_NAME               = 'SUN_PHOBOS_VIEW_ZY' ",
         "FRAME_401000_CLASS              = 5",
         "FRAME_401000_CLASS_ID           = 401000",
         "FRAME_401000_CENTER             = 401",
         "FRAME_401000_RELATIVE           = 'J2000' ",
         "FRAME_401000_DEF_STYLE          = 'PARAMETERIZED' ",
         "FRAME_401000_FAMILY             = 'TWO-VECTOR' ",
         "FRAME_401000_PRI_AXIS           = 'Z' ",
         "FRAME_401000_PRI_VECTOR_DEF     = 'OBSERVER_TARGET_POSITION' ",
         "FRAME_401000_PRI_OBSERVER       = 'SUN' ",
         "FRAME_401000_PRI_TARGET         = 'PHOBOS' ",
         "FRAME_401000_PRI_ABCORR         = 'NONE' ",
         "FRAME_401000_SEC_AXIS           = 'Y' ",
         "FRAME_401000_SEC_VECTOR_DEF     = 'OBSERVER_TARGET_VELOCITY' ",
         "FRAME_401000_SEC_OBSERVER       = 'SUN' ",
         "FRAME_401000_SEC_TARGET         = 'PHOBOS' ",
         "FRAME_401000_SEC_ABCORR         = 'NONE' ",
         "FRAME_401000_SEC_FRAME          = 'J2000' " 
      };

      String                         label;

      String[]                       locs   = 
                                     { "CENTER", "Ellipsoid Terminator" };

      String                         method; 
      String                         sfnmth;
      String                         shape;
      String[]                       shapes = { "DSK", "Ellipsoid" };
      String                         title;
      String                         shadow;

      Surface[]                      srflst;

      SurfaceIntercept[]             surfxArr;

      SurfacePoint                   spoint;
 
      TDBDuration                    tdelta;

      TDBTime                        epoch;
      TDBTime                        et;
      TDBTime                        et0;
      TDBTime                        xepoch;

      Vector3                        axis;
      Vector3                        center;
      Vector3[]                      cutpts;
      Vector3[]                      dirs;
      Vector3                        lproj;
      Vector3                        normal;
      Vector3                        origin = new Vector3( 0.0, 0.0, 0.0 );
      Vector3                        plnnml;
      Vector3                        plnref;
      Vector3                        point;
      Vector3                        raydir;
      Vector3                        refvec;
      Vector3                        srfvec;
      Vector3                        tanvec;
      Vector3                        vertex;
      Vector3                        viewpt;
      Vector3[]                      vrtces;
      Vector3                        vtemp;
      Vector3                        xpt;
      Vector3                        xpt1;
      Vector3                        xpt2;
      Vector3                        xsrfvc;

      boolean                        found;
      boolean                        ok;
      boolean                        pri;

      double                         a;
      double                         angle;
      double                         b;
      double                         c;
      double                         dlat;
      double                         dlon;
      double                         dp;
      double                         r;
      double[]                       radii;
      double                         rcross;
      double                         roll;
      double                         rolstp;
      double                         schstp;
      double                         soltol;
      double[]                       srcrad;
      double                         tol;

      int                            bodyid;
      int                            corix;
      int                            cutix;
      int                            i;
      int                            locix;
      int                            ncross;
      int                            ncuts;
      int                            nlat;
      int                            nlon;
      int                            npolyv;
      int                            obsix;
      int                            rowsiz;
      int                            shapix;
      int                            spkhan;
      int                            srcix;
      int                            surfid;
      int                            targix;
      int                            timix;

      //
      // Start tests.
      //


      try
      {

         JNITestutils.topen ( "f_TerminatorPoint" );

         //
         // Constructor tests
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TerminatorPoint no-args constructor" );
 
         //
         // Make sure we can make the call.
         //
         tpoint = new TerminatorPoint();
        

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "TerminatorPoint field-based constructor" );

         point  = new Vector3(  1.0, 2.0, 3.0 );

         tanvec = new Vector3( -4.0, 5.0, 6.0 );
         
         xepoch = new TDBTime ( 5.e8 );

         tpoint = new TerminatorPoint( point, xepoch, tanvec );

         //
         // Check members of tpoint.
         //
         tol = 0.0;

         ok  = JNITestutils.chckad( "tpoint.v", tpoint.toArray(), "=",
                                                point.toArray(),  tol  );
 
         ok  = JNITestutils.chcksd( "tpoint.targetEpoch", 
                                    tpoint.getTargetEpoch().getTDBSeconds(), 
                                    "=",
                                    xepoch.getTDBSeconds(),  tol  );

         ok  = JNITestutils.chckad( "tpoint.surfaceVector", 
                                    tpoint.getSurfaceVector().toArray(), "=",
                                    tanvec.toArray(),  tol  );



         //*********************************************************************
         //
         //  Set up.
         //
         //*********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Setup: create a text PCK and a default SPK." );

         (new File(PCK0)).delete();

         //
         //    Don't load the PCK; do save it. 
         //
         JNITestutils.tstpck( PCK0, false, true );

         //
         // Load via `load' to avoid later complexities. 
         //
         KernelDatabase.load( PCK0 );

         //
         // Create and load the SPK.      
         //
         spkhan = JNITestutils.tstspk( SPK0, false );

         KernelDatabase.load( SPK0 );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Setup: create two DSKs containing " + 
                              "target shapes."                       );


         (new File(DSK0)).delete();
         (new File(DSK1)).delete();

         //
         // Create the DSKs.
         //

         //
         // Start out by creating a segment for Mars. We'll create  
         // a very low-resolution tessellated ellipsoid.
         //

         bodyid = 499;
         surfid = 1;
         fixref = frames[0];
         nlon   = 100;
         nlat   = 51;

         JNITestutils.t_elds2z( bodyid, surfid, fixref.getName(),
                                nlon,   nlat,   DSK0             ); 


         //
         // Compute lon/lat deltas for later use. 
         //
         dlon = 2*PI / nlon;
         dlat =   PI / nlat;

         //
         // Add shape data for Phobos. We'll use a set of nested tori having
         // a central axis that points from the sun toward Phobos. This will
         // require creation of three new segments. 
         //   
         //  We'll need to load the frame definition first. The frame name is
         //
         //     SUN_PHOBOS_VIEW_ZY
         //
         KernelPool.loadFromBuffer( frmbuf );

         bodyid = 401;
         surfid = 2;
         fixref = frames[1];
         npolyv = 50;
         ncross = 100;
         r      = 100.0;
         rcross =  10.0;

         center = new Vector3( origin );
         normal = new Vector3( 0.0, 0.0, 1.0 );

         JNITestutils.t_torus( bodyid, surfid,           fixref.getName(),
                               npolyv, ncross,           r,    
                               rcross, center.toArray(), normal.toArray(), 
                               DSK1                                        );

         r = 70;

         JNITestutils.t_torus( bodyid, surfid,           fixref.getName(),
                               npolyv, ncross,           r,    
                               rcross, center.toArray(), normal.toArray(), 
                               DSK1                                        );
         r = 40;

         JNITestutils.t_torus( bodyid, surfid,           fixref.getName(),
                               npolyv, ncross,           r,    
                               rcross, center.toArray(), normal.toArray(), 
                               DSK1                                        );
         //
         // Load the DSKs. 
         //
         KernelDatabase.load( DSK0 );
         KernelDatabase.load( DSK1 );  


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Top of f_TerminatorPoint normal case loop." );

  
        
         //
         // Set an initial time and time delta.
         //
         et0    = new TDBTime(  10 * CSPICE.jyear() );
         tdelta = new TDBDuration( 3600.0 );

         //
         // Loop over targets. 
         //
         for ( targix = 0;  targix < NTARGS;  targix++  )
         {
            target = targs [targix];
            fixref = frames[targix];

            // 
            // Loop over observers. 
            //
            for ( obsix = 0;  obsix < NOBS;  obsix++  )
            {
               obsrvr = obs[obsix];

               // 
               // Loop over ilumination sources. 
               //
               for ( srcix = 0;  srcix < NSRC;  srcix++  )
               {
                  ilusrc = srcs[srcix];

                  //
                  // Loop over aberration corrections. 
                  //
                  for ( corix = 0;  corix < NCORR;  corix++  )
                  {
                     abcorr = corrs[corix];

                     //
                     // Loop over aberration correction loci.
                     //
                     for ( locix = 0;  locix < NLOC;  locix++  )
                     {
                        corloc = locs[locix];


                        //
                        // Loop over observation times. 
                        //
                        for ( timix = 0;  timix < NTIMES;  timix++  )
                        {
                           et = et0.add( tdelta.scale(timix) );

                           //
                           // Loop over target shapes. 
                           //
                           for ( shapix = 0;  shapix < NSHAPE;  shapix++  )
                           {
                              shape = shapes[shapix];

                              //
                              // Put together a method string. 
                              //

                              if ( CSPICE.eqstr( shape, "ellipsoid" ) )
                              {
                                 method = "Ellipsoid/Tangent/Penumbral";

                                 shadow = "Penumbral";

                              }
                              else
                              {
                                 //
                                 // This is the DSK case. 
                                 //
                                 // The target index, plus 1, is the 
                                 // surface ID.
                                 //
                                 method = String.format( 

                                    "dsk/unprioritized/tangent/" +
                                    "umbral/surfaces = %d",
                                    targix + 1                        );

                                 shadow = "Umbral";
                              }


                              //
                              // --------Case-----------------------------------
                              //

                              title = String.format( 
                                 "TerminatorPoint.create: method = %s; " +
                                 "locus = %s; "                    +
                                 "abcorr = %s; "                   +
                                 "target = %s;  "                  +
                                 "source = %s;  "                  +
                                 "et = %23.17e; fixref = "         +
                                 "%s; obsrvr = %s. ",
                                 method,
                                 corloc,
                                 abcorr.getName(),
                                 target.getName(),
                                 ilusrc.getName(),
                                 et.getTDBSeconds(),
                                 fixref.getName(),
                                 obsrvr.getName()                     );

                             JNITestutils.tcase ( title );


                              if ( CSPICE.eqstr(shape, "DSK") )
                              {
                                 //
                                 // This is the DSK case. 
                                 //
                                 soltol = 1.e-9;

                                 if ( CSPICE.eqstr( target.getName(), "MARS" ) )
                                 {
                                    schstp = 4.0;
                                    refvec = new Vector3( 0.0, 0.0, 1.0 );
                                 }
                                 else                              
                                 {
                                    //
                                    // The target is Phobos. 
                                    //
                                    schstp = 1.e-8;
                                    refvec = new Vector3( 1.0, 0.0, 0.0 );
                                 }
                              }
                              else
                              {
                                 //
                                 // `refvec' is unused by CSPICE in this case,
                                 // but the bridge code requires it to be
                                 // initialized, and termpt_ will verify that
                                 // it's non-zero. Make it the +X axis.
                                 //
                                 refvec = new Vector3( 1.0, 0.0, 0.0 );

                                 soltol = 0.0;
                                 schstp = 0.0;
                              }

                              rolstp = 15.0 * RPD;

                              ncuts  = 4;

                              term = TerminatorPoint.create( 

                                 method, ilusrc, target, et,     fixref, 
                                 abcorr, corloc, obsrvr, refvec, rolstp,
                                 ncuts,  schstp, soltol, MAXN           );


                              //
                              // We're going to focus on just a few combinations
                              // of observer, target, shape, aberration 
                              // correction, and aberration correction locus.
                              //

                              if (   CSPICE.eqstr( obsrvr.getName(), "SUN"   )
                                  && CSPICE.eqstr( target.getName(), "PHOBOS")
                                  && CSPICE.eqstr( shape,            "DSK"   )
                                  && CSPICE.eqstr( abcorr.getName(), "NONE"  )
                                  && CSPICE.eqstr( corloc,           "CENTER") )
                              {
                                 //
                                 // This is the case where Phobos is
                                 // represented by a set of three nested tori.
                                 //
                                 // We expect that each torus will contribute 
                                 // an upper and lower terminator point in each 
                                 // cutting half-plane.
                                 //

                                 //
                                 // System.out.format ( "%s%n", method );
                                 //

                                 for ( cutix = 0; cutix < ncuts; cutix++ )
                                 {
                                    //
                                    // We cut through 3 tori in each half-plane.
                                    // This gives us 6 terminator points. 
                                    //
                                    label  = String.format( "npts[%d]", cutix );

                                    rowsiz = term[cutix].length;

                                    ok     = JNITestutils.chcksi( 

                                                label, rowsiz, "=", 6, 0 );

                                    //
                                    // Check the epochs, points, and tangent 
                                    // vectors. 
                                    //
                                    for ( i = 0; i < rowsiz; i++ )
                                    {
                                       //
                                       // Capture the current terminator point.
                                       //
                                       tpoint = term[cutix][i];

                                       //
                                       // Since the aberration correction is 
                                       // NONE, each epoch should match the 
                                       // input epoch. There still is one epoch
                                       // per terminator point; check them all.
                                       //
                                       tol = TIGHT;

                                       label  = String.format( 
                                                "epoch[%d] for cut [%d]", 
                                                i, cutix                  );

                                       epoch  = tpoint.getTargetEpoch();

                                       ok     = JNITestutils.chcksd( label,
                                                epoch.getTDBSeconds(),   "~",
                                                et.getTDBSeconds(),      tol  );

                                       //
                                       // Each surface vector should be the sum
                                       // of the observer-target vector and the
                                       // corresponding terminator point. 
                                       //
                                       trgpos = new

                                          PositionVector( target, et, fixref,
                                                          abcorr, obsrvr     );

                                       xsrfvc = trgpos.add( tpoint );

                                       srfvec = tpoint.getSurfaceVector();

                                       tol    = TIGHT;

                                       label  = String.format( 

                                          "tangts[%d] for cut %d", 
                                          i, cutix                 );

                                       ok = JNITestutils.chckad ( 

                                            label,  srfvec.toArray(), "~~/",
                                                    xsrfvc.toArray(), tol    );
                                    }
                                    //
                                    // End of loop over current row.
                                    //
                                 }
                                 //
                                 // End of loop over cuts.
                                 //
                              }
                              //
                              // End of geometric Earth-Phobos case.
                              //


                              else if(  

                                CSPICE.eqstr( target.getName(), "MARS"      ) &&
                                CSPICE.eqstr( shape,            "ELLIPSOID" ) &&
                                CSPICE.eqstr( corloc, "ELLIPSOID terminator")  )
                              {
                                 //
                                 // This is a set of cases where the epochs
                                 // associated with the terminator points are 
                                 // expected to be accurate, since they're
                                 // computed individually for each terminator 
                                 // point, and because the reference ellipsoid
                                 // is used for the light time corrections.
                                 //
                                 // In these cases, we should get accurate
                                 // results for any aberration correction.
                                 //
                                 // We expect just one terminator point per 
                                 // cutting half-plane. 
                                 //
                                 for ( cutix = 0; cutix < ncuts; cutix++ )
                                 {

                                    label  = String.format( "npts[%d]", cutix );

                                    rowsiz = term[cutix].length;

                                    ok     = JNITestutils.chcksi( 

                                                label, rowsiz, "=", 1, 0 );
                                    //
                                    // Find the state of the terminator point 
                                    // as seen by the observer. 
                                    //
                                    tpoint = term[cutix][0];

                                    srfvec = tpoint.getSurfaceVector();
                                    epoch  = tpoint.getTargetEpoch();

                                    lptsta = new

                                       StateRecord( tpoint, target, fixref,
                                                    et,     fixref, "target",
                                                    abcorr, obsrvr           );

                                    xsrfvc = lptsta.getPosition();

                                    //
                                    // Check the epoch. 
                                    //
                                    if ( abcorr.isGeometric() )
                                    {
                                       xepoch = et;
                                    }
                                    else
                                    {
                                       xepoch = et.sub( lptsta.getLightTime() );
                                    }


                                    label = String.format( "epoch for cut %d", 
                                                           cutix              );

                                    tol = TIGHT;

                                    ok = JNITestutils.chcksd( 

                                         label, epoch.getTDBSeconds(),  "~",
                                                xepoch.getTDBSeconds(), tol   );

                                    //
                                    // Check the terminator point and tangent 
                                    // vector. Note that xsrfvc is derived 
                                    // from both the computed terminator point
                                    // and the expected tangent vector.
                                    //
                                    tol   = TIGHT;

                                    label = String.format( "srfvec for cut %d", 
                                                           cutix              );

                                    ok = JNITestutils.chckad( 

                                         label,  srfvec.toArray(),  
                                          "~~/", xsrfvc.toArray(), tol );
                                 }
                                 //
                                 // End of case for current cut.
                                 //

                              }
                              //
                              // End of Mars ellipsoid, ellipsoid terminator 
                              // locus case.
                              // 



                              else if (   

                                CSPICE.eqstr(target.getName(), "MARS"      ) &&
                                CSPICE.eqstr(shape,            "DSK"       ) &&
                                CSPICE.eqstr(corloc, "ELLIPSOID terminator")   )

                              {
                                 //
                                 // This is a set of cases where the epochs
                                 // associated with the terminator points are 
                                 // expected to be LESS accurate, since, while
                                 // they're computed individually for each 
                                 // terminator point, the computations are done 
                                 // for terminator points on the reference 
                                 // ellipsoid rather than on the DSK surface.
                                 //
                                 // Compute the ellipsoid terminator points 
                                 // just to obtain the associated epochs. 
                                 //

                                 eterm = TerminatorPoint.create( 

                                    "ellipsoid/tangent/umbral", 
                                    ilusrc, target, et,     fixref, abcorr,
                                    corloc, obsrvr, refvec, rolstp, ncuts, 
                                    schstp, soltol, MAXN                   );

                                 //
                                 // We expect just one terminator point per 
                                 // cutting half-plane. 
                                 //
                                 for ( cutix = 0; cutix < ncuts; cutix++ )
                                 {

                                    label  = String.format( "npts[%d]", cutix );

                                    rowsiz = term[cutix].length;

                                    ok     = JNITestutils.chcksi( 

                                                label, rowsiz, "=", 1, 0 );
                                    //
                                    // Find the state of the terminator point 
                                    // as seen by the observer. 
                                    //
                                    tpoint = term[cutix][0];

                                    srfvec = tpoint.getSurfaceVector();
                                    epoch  = tpoint.getTargetEpoch();

                                    lptsta = new

                                       StateRecord( tpoint, target, fixref,
                                                    et,     fixref, "target",
                                                    abcorr, obsrvr           );

                                    xsrfvc = lptsta.getPosition();

                                    //
                                    // Check the epoch against that obtained by
                                    // treating the terminator point as an
                                    // ephemeris object.
                                    //
                                    if ( abcorr.isGeometric() )
                                    {
                                       xepoch = et;
                                    }
                                    else
                                    {
                                       xepoch = et.sub( lptsta.getLightTime() );
                                    }

                                    //
                                    // We don't expect tight agreement. 
                                    //
                                    label = String.format( "(1) epoch for " +
                                                           "cut %d", 
                                                           cutix              );

                                    tol   = 1.e-3;

                                    ok = JNITestutils.chcksd( 

                                         label, epoch.getTDBSeconds(),  "~",
                                                xepoch.getTDBSeconds(), tol   );

                                    //
                                    // Check the epoch against that obtained for
                                    // an ellipsoidal target shape. This match
                                    // should be very good.
                                    //
                                    xepoch = eterm[cutix][0].getTargetEpoch();


                                    label = String.format( "(2) epoch for " +
                                                           "cut %d", 
                                                           cutix              );

                                    tol   = TIGHT;

                                    ok = JNITestutils.chcksd( 

                                         label, epoch.getTDBSeconds(),  "~",
                                                xepoch.getTDBSeconds(), tol  );

                                    //
                                    // Check the terminator point and surface 
                                    // vector. Note that xsrfvc is derived from 
                                    // both the computed terminator point and
                                    // the expected tangent vector.
                                    //
                                    tol   = LOOSE;

                                    label = String.format( "srfvec for cut %d", 
                                                           cutix              );

                                    ok = JNITestutils.chckad( 

                                         label,  srfvec.toArray(),  
                                          "~~/", xsrfvc.toArray(), tol );
                                 }
                                 //
                                 // End of case for current cut.
                                 //

                              }
                              //
                              // End of Mars ellipsoid, ellipsoid terminator 
                              // locus case.
                              // 

                              //
                              // The following tests apply to all combinations
                              // of targets, observers, and shapes.
                              //       

                              if ( abcorr.isGeometric() )
                              {
                                 //
                                 // The geometric case is relatively simple,
                                 // since the target frame is evaluated at
                                 // the input epoch `et'. 
                                 //
                                 // We've done some checks on the consistency of
                                 // the terminator points and tangent vectors,
                                 // but we haven't done anything to verify that 
                                 // the terminator points returned by `create' 
                                 // are really on the terminator, and that 
                                 // they lie in the correct half-planes. Do 
                                 // that now.
                                 //
                                 for ( cutix = 0;  cutix < ncuts;  cutix++ )
                                 {
                                    rowsiz = term[cutix].length;

                                    //
                                    // Check the points in the current 
                                    // half-plane. 
                                    //
                                    for ( i = 0;  i < rowsiz;  i++ )
                                    {         

                                       tpoint = term[cutix][i];
                                       srfvec = tpoint.getSurfaceVector();
                                       epoch  = tpoint.getTargetEpoch();

                                       //
                                       // Make sure the terminator point is on 
                                       // the target surface. To do this, we'll
                                       // get an outward normal vector at the 
                                       // point, create a vertex some distance
                                       // from the surface in the normal 
                                       // direction, and then find the 
                                       // surface intercept of a ray emanating 
                                       // from this vertex and pointing in the 
                                       // direction opposite to the normal. 
                                       //
                                       // If the original terminator point is 
                                       // on the surface, then the intercept 
                                       // found by this process should be very
                                       // close to the terminator point. Of 
                                       // course, if the terminator point is
                                       // too far off the surface, the attempt
                                       // to find an outward surface normal 
                                       // will fail.
                                       //

                                       if ( CSPICE.eqstr( shape, "DSK" ) )
                                       {                          
                                          sfnmth = "DSK/UNPRIORITIZED";
                                       }
                                       else
                                       {
                                          sfnmth = "ELLIPSOID";
                                       }

                                       //
                                       // Let `spoint' be a SurfacePoint 
                                       // instance equivalent to `tpoint'.
                                       //
                                       spoint = 

                                          new SurfacePoint( sfnmth, target, et,
                                                            fixref, tpoint    );

                                       normal = spoint.getNormal();
                                       raydir = normal.negate();

                                       vertex = tpoint.add( normal );

                                       if ( CSPICE.eqstr( shape, "DSK" ) )
                                       {
                                          srflst    = new Surface[0];

                                          vrtces    = new Vector3[1];
                                          vrtces[0] = vertex;

                                          dirs      = new Vector3[1];
                                          dirs[0]   = raydir;

                                          pri       = false;

                                          surfxArr  = SurfaceIntercept.create( 

                                                         pri, target, srflst, 
                                                         et,  fixref, vrtces,
                                                         dirs                 );

                                          xpt       = surfxArr[0].
                                                      getIntercept();
                                       }
                                       else
                                       {
                                          radii  = target.getValues( "RADII" );

                                          a      = radii[0];
                                          b      = radii[1];
                                          c      = radii[2];

                                          ellpsd = new Ellipsoid( a, b, c );

                                          ray    = new Ray( vertex, raydir );

                                          rayx   = new

                                             RayEllipsoidIntercept(ray, ellpsd);

                                          //
                                          // We expect an intercept to exist.
                                          // If not, getIntercept will throw an 
                                          // (unexpected) exception.
                                          //
                                          xpt = rayx.getIntercept();
                                       }

                                       //
                                       // We expect the intercept to be very
                                       // close to the original terminator  
                                       // point.
                                       //

                                       tol   = TIGHT;

                                       label = String.format( 

                                               "terminator point %d " + 
                                               "in half plane %d", 
                                                i, cutix           );

                                        ok = JNITestutils.chckad( 

                                                label,  tpoint.toArray(),  
                                                 "~~/", xpt.toArray(),    tol );

                                       

                                       //
                                       // Now verify that the terminator point 
                                       // is in the correct cutting half-plane. 
                                       //
                                       // Start out by getting the axis vector,
                                       // which is parallel to the target 
                                       // center-source vector. The evaluation
                                       // epoch is that of the target; however
                                       // this is a geometric case, so `et' may
                                       // be used as well.
                                       //
                                       ilupos = new

                                          PositionVector( ilusrc, et,    fixref,
                                                          abcorr, target      );

                                       axis = ilupos;

                                       //
                                       // Let `plnref' be a vector in the 
                                       // half-plane. Let `vtemp' be the result
                                       // of rotating `refvec' about `axis' by
                                       // the roll angle, and let `plnref' be 
                                       // the component of `vtemp' orthogonal
                                       // to `axis'.
                                       // 
                                       roll   = cutix * rolstp;

                                       vtemp  = refvec.rotate( axis, roll );

                                       plnnml = axis.ucross( vtemp );

                                       plnref = plnnml.ucross( axis );

                                       //
                                       // Create a SPICE plane containing the 
                                       // target center and normal to plnnml. 
                                       //
                                       cutpln = new Plane( plnnml, origin );

                                       //
                                       // Project the terminator point  
                                       // orthogonally onto the plane 
                                       // containing the cutting half-plane. 
                                       //
                                       // The projection should be very close to
                                       // the original terminator point. 
                                       // However, since the observer may be far
                                       // from the target, the round-off error 
                                       // in the axis may be fairly large. So we
                                       // use a tolerance a bit looser than
                                       // TIGHT.
                                       //
                                       lproj = cutpln.project( tpoint );

                                       tol   = MED;

                                       label = String.format( 

                                             "terminator point %d projection " +
                                             "in half plane %d", 
                                              i, cutix           );

                                        ok = JNITestutils.chckad( 

                                                label,  lproj.toArray(),  
                                                 "~~/", tpoint.toArray(), tol );

                                        if ( !ok ) return (ok);


                                       //
                                       // We've verified the point is in the 
                                       // correct plane; we must ensure it's
                                       // in the correct half-plane. 
                                       //

                                       dp    = tpoint.dot( plnref );

                                       label = String.format( 

                                          "terminator point %d inner product " +
                                          "with reference vector "             +
                                          "in half plane %d", 
                                          i, cutix           );

                                       ok = JNITestutils.chcksd( 

                                                label, dp, ">", 0.0, 0.0 );
                                                                               
                                       //
                                       // Programmer's note:
                                       //
                                       // This next step is not necessary to 
                                       // verify the correctness of the 
                                       // `create' method. It need not be
                                       // replicated in JNITspice test 
                                       // families (But it doesn't hurt to
                                       // include it.) 
                                       // 
                                       // We'll produce a vector in the 
                                       // current half-plane that's tangent 
                                       // to both the illumination source 
                                       // and to the target.
                                       // 
                                       // Start out by finding the limb on the 
                                       // illumination source as seen from 
                                       // the current terminator point. The 
                                       // of tangency on the source must lie
                                       // on the limb.
                                       // 
                                       // Note: for ellipsoids, this approach
                                       // is slightly wrong, since we're 
                                       // actually defining the terminator 
                                       // using planes that are tangent to
                                       // both target and source. But it's a 
                                       // good enough approximation for this
                                       // test.
                                       // 
                                       // The source's shape is considered to 
                                       // be a sphere by `create', so we treat
                                       // it as a sphere here, as well.
                                       //
                                       // Find the limb on the illumination 
                                       // source as seen from `tpoint'.
                                       //
                                       
                                       srcrad = ilusrc.getValues( "RADII" );

                                       srcshp = new Ellipsoid( srcrad[0],
                                                               srcrad[1],
                                                               srcrad[2] );
                                       //
                                       // Because we're treating the 
                                       // illumination source as a sphere,
                                       // we can use the target body-fixed
                                       // frame instead of that of the source.
                                       //
                                       viewpt = tpoint.sub( ilupos );
 
                                       srclmb = srcshp.getLimb( viewpt );

                                       //
                                       // Cut the limb with the current half-
                                       // plane.
                                       //
                                       ellplx = new EllipsePlaneIntercept ( 
     
                                                   srclmb, cutpln );
                                       
                                       //
                                       // Check the intercept count.
                                       // 
                                       ok = JNITestutils.chcksi( "count", 
                                               
                                            ellplx.getInterceptCount(),
                                            "=",  2,  0                  );

                                       //
                                       // The intersection points are offsets
                                       // from the center of the illumination 
                                       // source. Translate them so that 
                                       // they're relative to the target center.
                                       //
                                       cutpts = ellplx.getIntercepts();

                                       xpt1   = cutpts[0].add( axis );
                                       xpt2   = cutpts[1].add( axis );
                                      
                                       //
                                       // If the shadow type is "umbral," then
                                       // the tangent point on the source is on
                                       // the same side of the axis as the
                                       // reference vector. Otherwise, it's on
                                       // the opposite side.
                                       //
                                       if ( CSPICE.eqstr( shadow, "Umbral" ) )
                                       {
                                          if ( xpt1.dot(plnref) > 0.0 )
                                          {
                                             vertex = xpt1;
                                          }
                                          else
                                          {
                                             vertex = xpt2;
                                          }                               
                                       }
                                       else
                                       {
                                          if ( xpt1.dot(plnref) < 0.0 )
                                          {
                                             vertex = xpt1;
                                          }
                                          else
                                          {
                                             vertex = xpt2; 
                                          }                               
                                       }

                                       tanvec = tpoint.sub( vertex );

                                       //
                                       // Make sure that the tangent vector for 
                                       // the current terminator point, if 
                                       // rotated slightly away from the target
                                       // surface, defines the direction of a 
                                       // ray that misses the target.
                                       //
                                       // We need to be careful with the nested 
                                       // tori shape: for this one, "away from 
                                       // the surface" does not coincide with 
                                       // "away from the observer-target center 
                                       // vector."
                                       //

                                       if ( rowsiz == 1 )
                                       {
                                          //
                                          // We have a convex surface (in the
                                          // context of this test family).
                                          //
                                          // "Away from the surface" corresponds
                                          // to a negative rotation about
                                          // plnnml.
                                          //
                                          angle = -1.e-8;
                                       }
                                       else
                                       {
                                          if ( i%2 == 0 )
                                          {
                                             //
                                             // This is an "outer" terminator 
                                             // point.
                                             //

                                             angle = -1.e-8;
                                          }
                                          else
                                          {
                                             angle =  1.e-8;
                                          }
                                       }


                                       raydir = tanvec.rotate( plnnml, angle );

                                       //
                                       // The vertex has been set above.
                                       //

                                       if ( CSPICE.eqstr( shape, "DSK" ) )
                                       {
                                          srflst    = new Surface[0];

                                          vrtces    = new Vector3[1];
                                          vrtces[0] = vertex;
                                          dirs      = new Vector3[1];
                                          dirs[0]   = raydir;
                                          pri       = false;

                                          surfxArr  = SurfaceIntercept.create( 

                                                         pri, target, srflst, 
                                                         et,  fixref, vrtces,
                                                         dirs                 );

                                          found = surfxArr[0].wasFound();
                                       }
                                       else
                                       {
                                          radii  = target.getValues( "RADII" );

                                          a      = radii[0];
                                          b      = radii[1];
                                          c      = radii[2];

                                          ellpsd = new Ellipsoid( a, b, c );
                                          ray    = new Ray( vertex, raydir );

                                          rayx   = new

                                             RayEllipsoidIntercept(ray, ellpsd);

                                          found = rayx.wasFound();

                                       }
                                      

                                       //
                                       // No intercept should be found. 
                                       //
                                       label = String.format( 

                                           "terminator point %d off-surface " + 
                                           "ray intercept found "             +
                                           "in half plane %d", 
                                           i, cutix           );

                                       ok = JNITestutils.chcksl( 

                                                label, found, false );

                                       //
                                       // Now check that rotating the tangent 
                                       // vector towards the target results 
                                       // in an intercept. 
                                       //
                                       angle *= -1;

                                       raydir = tanvec.rotate( plnnml, angle );


                                       if ( CSPICE.eqstr( shape, "DSK" ) )
                                       {
                                          srflst    = new Surface[0];

                                          vrtces    = new Vector3[1];
                                          vrtces[0] = vertex;
                                          dirs      = new Vector3[1];
                                          dirs[0]   = raydir;
                                          pri       = false;

                                          surfxArr  = SurfaceIntercept.create( 

                                                         pri, target, srflst, 
                                                         et,  fixref, vrtces,
                                                         dirs                 );

                                          found = surfxArr[0].wasFound();
                                       }
                                       else
                                       {
                                          radii  = target.getValues( "RADII" );

                                          a      = radii[0];
                                          b      = radii[1];
                                          c      = radii[2];

                                          ellpsd = new Ellipsoid( a, b, c );
                                          ray    = new Ray( vertex, raydir );

                                          rayx   = new

                                             RayEllipsoidIntercept(ray, ellpsd);

                                          found = rayx.wasFound();

                                       }

                                       //
                                       // An intercept should be found. 
                                       //
                                       label = String.format( 

                                           "terminator point %d off-surface " + 
                                           "ray intercept found "             +
                                           "in half plane %d", 
                                           i, cutix           );

                                       ok = JNITestutils.chcksl( 

                                                label, found, true ); 
                                    }
                                    // 
                                    // End of loop for current half-plane. 
                                    //                        
                                 }
                                 //
                                 // End of loop for all half-planes.
                                 //
                              }
                              //
                              // End of general geometric case.
                              //                            
                           }
                           //
                           // End of shape loop. 
                           //
                        }
                        //
                        // End of time loop. 
                        //
                     }
                     //
                     // End of aberration correction locus loop. 
                     //
                  }
                  //
                  // End of aberration correction loop. 
                  //


               }
               //
               // End of illumination source loop.
               //

            }
            //
            // End of observer loop.
            //
         }
         //
         // End of target loop. 
         //




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
         ( new File( DSK0 ) ).delete();
         ( new File( DSK1 ) ).delete();
      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

} /* End f_TerminatorPoint */











