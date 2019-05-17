package spice.tspice;

import java.io.*;
import static java.lang.Math.PI;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.DPR;
import static spice.basic.AngularUnits.RPD;



/**
Class TestSurfaceInterceptWithDSKInfo provides methods that implement 
test families for the class SurfacePointWithDSKIntercept.


<h3> Version 1.0.0 03-JAN-2017 (NJB)</h3>

*/
public class TestSurfaceInterceptWithDSKInfo
{


   /**
   Test family 001 for methods of the class 
   spice.basic.SurfaceInterceptWithDSKInfo.
   <pre>
   -Procedure f_SurfaceInterceptWithDSKInfo (Test SurfaceInterceptWithDSKInfo)

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

      This routine tests methods of class SurfaceInterceptWithDSKInfo.

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

      -JNISpice Version 1.0.0 12-DEC-2016 (NJB) (EDW)

   -&
   </pre>
   */

   public static boolean f_SurfaceInterceptWithDSKInfo()

      throws SpiceException
   {
      //
      // Local constants
      //
      final String                   DSK0 = "surfacexptinfo_test0.bds";    
      final String                   PCK0 = "surfacexptinfo_test.tpc";    

      final double                   SML    = 1.e-6;
      final double                   TIGHT  = 1.e-12;

      final int                      MARSIX = 0;
      final int                      SATIX  = 1;

  
      //
      // Local variables
      //
      Body[]                         obs   = {
                                                new Body( "Earth" ),
                                                new Body( "Sun"   ),  
                                             };

      Body                           obsrvr;
      Body                           target;
      Body[]                         targs = {
                                                new Body( "Mars"   ),
                                                new Body( "Saturn" ),  
                                             };

      DLADescriptor                  dladsc;

      DSK                            dsk0;

      DSKDescriptor                  dskdsc;
      DSKDescriptor                  xdsdsc;

      LatitudinalCoordinates         latcor;

      ReferenceFrame                 fixref;

      ReferenceFrame[]               frames =
                                     {
                                        new ReferenceFrame( 
                                           "IAU_MARS" ), 
                                        new ReferenceFrame( 
                                           "IAU_SATURN" )
                                     };

      String                         label;
      String                         title;

      Surface[]                      emptyList = new Surface[0];
      Surface[]                      srflst;

      SurfaceInterceptWithDSKInfo    surfx;
      SurfaceInterceptWithDSKInfo    surfx1;
      SurfaceInterceptWithDSKInfo    surfx2;

      TDBTime                        et;
      TDBTime                        xepoch;

      Vector3[]                      dirs;
      Vector3                        point;
      Vector3                        raydir;
      Vector3                        srfvec;
      Vector3                        vertex;
      Vector3[]                      vrtces;
      Vector3                        xnorml;
      Vector3                        xpt;
      Vector3                        xsrfvc;

      boolean[]                      fndArr = new boolean[1];
      boolean                        ok;
      boolean                        pri    = false;

      double                         dlat;
      double                         dlon;
      double                         lat;
      double                         lon;
      double[][]                     lonlat;
      double                         r;
      double                         tol;
      double[]                       xxpt = new double[3];

      int                            bodyid;
      int                            handle;
      int                            i;
      int                            j;
      int                            k;
      int                            nlat;
      int                            nlon;
      int                            nrays;
      int                            nslat;
      int                            nslon;
      int                            nsurf;
      int                            plid;
      int[]                          plidArr = new int[1];
      int                            surfid;


      //
      // Start tests.
      //


      try
      {

         JNITestutils.topen ( "f_SurfaceInterceptWithDSKInfo" );




         //*********************************************************************
         //
         //  Set up.
         //
         //*********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Setup: create a text PCK." );

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
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Setup: create a DSK file containing " + 
                              "segments for Mars and Saturn."         );

         //
         // Delete the DSK file if it already exists. 
         //
         (new File(DSK0)).delete();

         //
         // Create the DSKs.
         //
         // We'll use a test utility that creates a tessellated plate model 
         // DSK. 
         //
         // Start out by creating a segment for Mars. We'll create  
         // a very low-resolution tessellated ellipsoid.
         //

         bodyid = 499;
         surfid = 1;
         fixref = frames[MARSIX];
         nlon   = 80;
         nlat   = 40;

         //
         // Create the DSK.
         //
         JNITestutils.t_elds2z( bodyid, surfid, fixref.getName(),
                                nlon,   nlat,   DSK0             ); 

         //
         // Add a Saturn segment.
         //
         bodyid = 699;
         surfid = 2;
         fixref = frames[SATIX];
         nlon   = 60;
         nlat   = 30;

         //
         // Append to the DSK.
         //
         JNITestutils.t_elds2z( bodyid, surfid, fixref.getName(),
                                nlon,   nlat,   DSK0             ); 
 

         //
         // Load the dsk for later use. 
         //
         KernelDatabase.load( DSK0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Setup: prepare for Mars spear test " +
                              "by obtaining DLA and DSK "           +
                              "segment descriptors."                  );


         dsk0   = DSK.openForRead( DSK0 );

         dladsc = dsk0.beginForwardSearch();

         dskdsc = dsk0.getDSKDescriptor( dladsc );






         //*********************************************************************
         //
         // Constructor tests
         //
         //*********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "No-args constructor" );
 
         //
         // Make sure we can make the call.
         //
         surfx = new SurfaceInterceptWithDSKInfo();
        

         
        


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test SurfaceInterceptWithDSKInfo " +
                              "copy constructor."                   );

         vertex = new Vector3( 0.0, 0.0, 1.e6 );
         raydir = vertex.negate();

         target = targs[0];

         fixref = frames[0];
         
         xepoch = new TDBTime ( 5.e8 );

         srflst = new Surface[0];

         //
         // Create intercept instance.
         //
         surfx  = 

            new SurfaceInterceptWithDSKInfo( pri,    target, srflst, xepoch, 
                                             fixref, vertex, raydir         );

         ok  = JNITestutils.chcksl( "surfx.wasFound", surfx.wasFound(), true );



         //
         // Make a copy of the intercept.
         //        
         surfx1 = new SurfaceInterceptWithDSKInfo( surfx );
         

         //
         // Check members of surfx1.
         //
         ok  = JNITestutils.chcksl( "surfx1.wasFound", 
                                    surfx1.wasFound(), true );

         tol = 0.0;

         ok  = JNITestutils.chckad( "surfx1.v", 
                                    surfx1.getIntercept().toArray(), "=",
                                    surfx.getIntercept().toArray(),  tol  );
 
         ok  = JNITestutils.chcksd( "surfx1.targetEpoch", 
                                    surfx1.getTargetEpoch().getTDBSeconds(), 
                                    "=",
                                    surfx.getTargetEpoch().getTDBSeconds(),  
                                    tol                                    );

         ok  = JNITestutils.chckad( "surfx1.surfaceVector", 
                                    surfx1.getSurfaceVector().toArray(), "~~/",
                                    surfx.getSurfaceVector().toArray(),  tol );


         ok  = JNITestutils.chckad( "surfx1.dskdsc", 
                                    surfx1.getDSKDescriptor().toArray(), "=",
                                    surfx.getDSKDescriptor().toArray(),  tol );

         ok  = JNITestutils.chckai( "surfx1.dladsc", 
                                    surfx1.getDLADescriptor().toArray(), "=",
                                    surfx.getDLADescriptor().toArray()       );

         ok  = JNITestutils.chcksi( "surfx1 DSK handle", 
                                    surfx1.getDSK().getHandle(), "=",
                                    surfx.getDSK().getHandle(),  0    );
         
         ok  = JNITestutils.chckai( "surfx1.ic", 
                                    surfx1.getIntComponent(), "=",
                                    surfx.getIntComponent()        );

         ok  = JNITestutils.chckad( "surfx1.dc", 
                                    surfx1.getDoubleComponent(), "=",
                                    surfx.getDoubleComponent(),  0.0 );

         //
         // Verify that `surfx1' is a deep copy: modify surfx; check surfx1.
         //

         surfx = 

           new SurfaceInterceptWithDSKInfo( pri,       targs[1], 
                                            srflst,    xepoch, 
                                            frames[1], vertex,    raydir );


         //
         // Generate an instance that should match the original `surfx'.
         //
         surfx2  = 

            new SurfaceInterceptWithDSKInfo( pri,    target, srflst, xepoch, 
                                             fixref, vertex, raydir         );

         ok = JNITestutils.chcksl( "surfx2.wasFound", surfx2.wasFound(), true );

  
        ok  = JNITestutils.chckad( "surfx1.v", 
                                    surfx1.getIntercept().toArray(), "=",
                                    surfx2.getIntercept().toArray(),  tol  );
 
         ok  = JNITestutils.chcksd( "surfx1.targetEpoch", 
                                    surfx1.getTargetEpoch().getTDBSeconds(), 
                                    "=",
                                    surfx2.getTargetEpoch().getTDBSeconds(),  
                                    tol                                    );

         ok  = JNITestutils.chckad( "surfx1.surfaceVector", 
                                    surfx1.getSurfaceVector().toArray(), "~~/",
                                    surfx2.getSurfaceVector().toArray(),  tol );


         ok  = JNITestutils.chckad( "surfx1.dskdsc", 
                                    surfx1.getDSKDescriptor().toArray(), "=",
                                    surfx2.getDSKDescriptor().toArray(),  tol );

         ok  = JNITestutils.chckai( "surfx1.dladsc", 
                                    surfx1.getDLADescriptor().toArray(), "=",
                                    surfx2.getDLADescriptor().toArray()       );

         ok  = JNITestutils.chcksi( "surfx1 DSK handle", 
                                    surfx1.getDSK().getHandle(), "=",
                                    surfx2.getDSK().getHandle(),  0    );
         
         ok  = JNITestutils.chckai( "surfx1.ic", 
                                    surfx1.getIntComponent(), "=",
                                    surfx2.getIntComponent()        );

         ok  = JNITestutils.chckad( "surfx1.dc", 
                                    surfx1.getDoubleComponent(), "=",
                                    surfx2.getDoubleComponent(),  0.0 );




       


         //*********************************************************************
         //
         // Method tests
         //
         //*********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         //
         // Perform spear test. We'll create a set of rays pointing inward
         // toward the target (Mars) and find the surface intercepts of these
         // rays.
         //
         // This test does not exercise the vectorization capability of 
         // SurfaceIntercept.create. We'll get to that later on.
         //
         // We test both the SurfaceIntercept and SurfacePoint `create' 
         // methods here.
         //
         // Below, we'll also test the normal vector methods of class
         // SurfacePoint.
         //
         //

         //
         // The surface list consists of one surface ID (for the only Mars
         // segment).
         //

         target    = targs [MARSIX];
         fixref    = frames[MARSIX];     
         nsurf     = 1;
         srflst    = new Surface[1];
         srflst[0] = new Surface( 1, target );

         //
         // Pick our longitude and latitude band counts so we don't 
         // end up sampling from plate boundaries. This simplifies
         // our checks on the outputs.
         //
         nslon = 37;
         nslat = 23;

         dlon  = 2*PI / nslon;
         dlat  =   PI / nslat;

         //
         // Pick a magnitude for the ray's vertex. 
         //
         r = 1.0e6;

         //
         // Pick an evaluation epoch. 
   
         et =  new TDBTime( 10 * CSPICE.jyear() );

         //
         //
         // The prioritization flag is always "false" for the N0066 version
         // of SPICE. 
         //
         pri = false;

         for ( i = 0;  i < nslon;  i++ )
         {
            //
            // Choose sample longitudes so that plate boundaries are not hit. 
            //
            lon = (0.5 * RPD) + (i * dlon);

            for ( j = 0;  j <= nslat;  j++ )
            {
               if ( j == 0 )
               {
                  lat = ( PI/2) - SML;
               }
               else if ( j == nslat )
               {
                  lat = (-PI/2) + SML;
               }
               else
               {
                  lat = (PI/2) - j*dlat;
               }
               


               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfaceInterceptWithDSKInfo "  +
                                      "constructor: "                      +
                                      "test DSKSI analog method. "         +
                                      "Mars spear test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );
 
               JNITestutils.tcase ( title );

               //
               // Create the ray's vertex and direction vector.
               //
               latcor = new LatitudinalCoordinates( r, lon, lat );
                
               vertex = latcor.toRectangular();
               raydir = vertex.negate();   
             
               //
               // Find the ray-surface intercept for the current ray.
               // The `create' method returns an array of SurfaceIntercepts.
               //
               // In this case our input and output arrays have length 1.
               //
               surfx = new

                 SurfaceInterceptWithDSKInfo( pri,    target, srflst, et,
                                              fixref, vertex, raydir     );
               //
               // We expect to find an intercept every time. 
               //
               ok = JNITestutils.chcksl( "found", surfx.wasFound(), true);

               //
               // Use surfx's handle and DLA descriptor to find the 
               // expected intercept. 
               //

               handle = surfx.getDSK().getHandle();
                
               dladsc = surfx.getDLADescriptor();

               CSPICE.dskx02( handle,           dladsc.toArray(),  
                              vertex.toArray(), raydir.toArray(), 
                              plidArr,          xxpt,             fndArr );
               //
               // Check the intercept. 
               //
               ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

               tol = TIGHT;

               ok = JNITestutils.chckad( "surfx", 
                                         surfx.getIntercept().toArray(),
                                         "~~/",   xxpt,            tol );


               //
               // Check the surface vector. It's not really needed, but we
               // inherit it from SurfaceIntercept.
               //
               xsrfvc = surfx.getIntercept().sub( vertex );

               ok = JNITestutils.chckad( "srfvec", 
                                         surfx.getSurfaceVector().toArray(),
                                         "~~/",    
                                         xsrfvc.toArray(),
                                         tol                                );
               //
               // Check the target epoch. It should match the input epoch.
               //
               ok   = 

                  JNITestutils.chcksd( "epoch", 
                                       surfx.getTargetEpoch().getTDBSeconds(),
                                       "=", 
                                       et.getTDBSeconds(),
                                       0.0                                    );
               //
               // Check the plate ID. We can do this only because we've 
               // offset the vertex coordinates away from those of the 
               // plates' edges.
               //
               plid = surfx.getIntComponent()[0];

               ok   = JNITestutils.chcksi( "plid", plid, "=", plidArr[0], 0 );


               //
               // Retrieve the d.p. information component, just to make sure
               // the call works. The contents of the array are undefined.
               //
               double[] dparr = surfx.getDoubleComponent();


               //
               // Check the DSK Descriptor.
               //
               dskdsc = surfx.getDSKDescriptor();

               xdsdsc = new DSKDescriptor( CSPICE.dskgd(handle, 
                                                        dladsc.toArray()) );

               ok = JNITestutils.chckad( "DSK descriptor", 
                                         dskdsc.toArray(),
                                         "=",    
                                         xdsdsc.toArray(),
                                         0.0                 );


               //
               // Repeat the constructor call, this time with an empty
               // surface list.
               //
               surfx = new

                 SurfaceInterceptWithDSKInfo( pri,    target, emptyList, et,
                                              fixref, vertex, raydir       );
               //
               // We expect to find an intercept every time. 
               //
               ok = JNITestutils.chcksl( "found", surfx.wasFound(), true);

               //
               // Check the intercept. 
               //
               ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

               tol = TIGHT;

               ok = JNITestutils.chckad( "surfx (2)", 
                                         surfx.getIntercept().toArray(),
                                         "~~/",       xxpt,            tol );
            }
         }


         
         
         //*********************************************************************
         //
         //  Set up for Saturn tests.
         //
         //*********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Setup: prepare for Saturn spear test by " +
                              "obtaining DLA and DSK segment descriptors." );


         dladsc = dsk0.getNext( dladsc );

         dskdsc = dsk0.getDSKDescriptor( dladsc );


         //
         // --------Case-----------------------------------------------
         //
         //
         // Perform Saturn spear test. We'll create a set of rays pointing 
         // inward toward the target (Saturn) and find the surface 
         // intercepts of these rays.
         //
   

         //
         // The surface list consists of one surface ID (for the only Saturn
         // segment).
   
         target    = targs [SATIX];
         fixref    = frames[SATIX];     
         nsurf     = 1;
         srflst    = new Surface[1];
         srflst[0] = new Surface( 2, target );

         //
         // Pick our longitude and latitude band counts so we don't 
         // end up sampling from plate boundaries. This simplifies
         // our checks on the outputs.
         //
         nslon = 37;
         nslat = 23;

         dlon  = 2*PI / nslon;
         dlat  =   PI / nslat;

         //
         // Pick a magnitude for the ray's vertex. 
         //
         r = 1.0e6;

         //
         // Pick an evaluation epoch. 
   
         et =  new TDBTime( -10 * CSPICE.jyear() );

         //
         //
         // The prioritization flag is always "false" for the N0066 version
         // of SPICE. 
         //
         pri = false;

         for ( i = 0;  i < nslon;  i++ )
         {

            //
            // Choose sample longitudes so that plate boundaries are not hit. 
            //
            lon = (0.5 * RPD) + (i * dlon);

            for ( j = 0;  j <= nslat;  j++ )
            {

              if ( j == 0 )
               {
                  lat = ( PI/2) - SML;
               }
               else if ( j == nslat )
               {
                  lat = (-PI/2) + SML;
               }
               else
               {
                  lat = (PI/2) - j*dlat;
               }

               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfaceInterceptWithDSKInfo "    +
                                      "constructor: "                        +
                                      "test DSKSI analog method. "           +
                                      "Saturn spear test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );
 
               JNITestutils.tcase ( title );

               //
               // Create the ray's vertex and direction vector.
               //
               latcor = new LatitudinalCoordinates( r, lon, lat );
                
               vertex = latcor.toRectangular();
               raydir = vertex.negate();   
             
               //
               // Find the ray-surface intercept for the current ray.
               // The `create' method returns an array of SurfaceIntercepts.
               //
               // In this case our input and output arrays have length 1.
               //
               surfx = new

                 SurfaceInterceptWithDSKInfo( pri,    target, srflst, et,
                                              fixref, vertex, raydir     );
               //
               // We expect to find an intercept every time. 
               //
               ok = JNITestutils.chcksl( "found", surfx.wasFound(), true);


               //
               // Use surfx's handle and DLA descriptor to find the 
               // expected intercept. 
               //

               handle = surfx.getDSK().getHandle();
                
               dladsc = surfx.getDLADescriptor();

               CSPICE.dskx02( handle,           dladsc.toArray(),  
                              vertex.toArray(), raydir.toArray(), 
                              plidArr,          xxpt,             fndArr );
               //
               // Check the intercept. 
               //
               ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

               tol = TIGHT;

               ok = JNITestutils.chckad( "surfx", 
                                         surfx.getIntercept().toArray(),
                                         "~~/",   xxpt,            tol );


               //
               // Check the surface vector. It's not really needed, but we
               // inherit it from SurfaceIntercept.
               //
               xsrfvc = surfx.getIntercept().sub( vertex );

               ok = JNITestutils.chckad( "srfvec", 
                                         surfx.getSurfaceVector().toArray(),
                                         "~~/",    
                                         xsrfvc.toArray(),
                                         tol                                );
               //
               // Check the target epoch. It should match the input epoch.
               //
               ok   = 

                  JNITestutils.chcksd( "epoch", 
                                       surfx.getTargetEpoch().getTDBSeconds(),
                                       "=", 
                                       et.getTDBSeconds(),
                                       0.0                                    );
               //
               // Check the plate ID. We can do this only because we've 
               // offset the vertex coordinates away from those of the 
               // plates' edges.
               //
               plid = surfx.getIntComponent()[0];

               ok   = JNITestutils.chcksi( "plid", plid, "=", plidArr[0], 0 );


               //
               // Retrieve the d.p. information component, just to make sure
               // the call works. The contents of the array are undefined.
               //
               double[] dparr = surfx.getDoubleComponent();


               //
               // Check the DSK Descriptor.
               //
               dskdsc = surfx.getDSKDescriptor();

               xdsdsc = new DSKDescriptor( CSPICE.dskgd(handle, 
                                                        dladsc.toArray()) );

               ok = JNITestutils.chckad( "DSK descriptor", 
                                         dskdsc.toArray(),
                                         "=",    
                                         xdsdsc.toArray(),
                                         0.0                 );


               //
               // Repeat the constructor call, this time with an empty
               // surface list.
               //
               surfx = new

                 SurfaceInterceptWithDSKInfo( pri,    target, emptyList, et,
                                              fixref, vertex, raydir       );
               //
               // We expect to find an intercept every time. 
               //
               ok = JNITestutils.chcksl( "found", surfx.wasFound(), true);

               //
               // Check the intercept. 
               //
               ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

               tol = TIGHT;

               ok = JNITestutils.chckad( "surfx (2)", 
                                         surfx.getIntercept().toArray(),
                                         "~~/",       xxpt,            tol );


            }
         }

         

   
         //*********************************************************************
         //
         //  Test exception handling
         //
         //*********************************************************************

         //*********************************************************************
         //
         //  Tests for class SurfaceIntercept exception handling
         //
         //    - Restricted to tests for the `create' method. Other 
         //      tests are provided in TestGeometry02.
         //
         //
         //*********************************************************************

         //
         // Test the DSKXSI analog constructor.
         //

         //
         // --------Case-----------------------------------------------
         //
         title = String.format( "SurfaceIntercept.create: frame not " +
                                "centered on target body."             );

         JNITestutils.tcase( title );


         try
         {
            target    = targs [0];
            fixref    = frames[1];
  
         
            //
            // Use an empty surface list.
            //
            srflst = new Surface[0];

            surfx  = 

               new SurfaceInterceptWithDSKInfo( pri,    target, srflst, et,
                                                fixref, vertex, raydir     );
             
            Testutils.dogDidNotBark (  "SPICE(INVALIDFRAME)" );  

         } 
         catch ( SpiceException exc )
         {
            //exc.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(INVALIDFRAME)", exc );
         }


  

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfaceInterceptWithDSKInfo constructor: " +
                              "intercept not found, "                     +
                              "attempt to access intercept member."        );

            target    = targs [0];
            fixref    = frames[0];


            surfx  = 

               new SurfaceInterceptWithDSKInfo( pri,    target, srflst, et,
                                                fixref, vertex, vertex     );
  
         try
         {
            surfx.getIntercept();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfaceInterceptWithDSKInfo: intercept " +
                              "not found, "                             +
                              "attempt to access surface vector member."  );

         try
         {
            surfx.getSurfaceVector();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfaceInterceptWithDSKInfo: intercept " +
                              "not found, "                             +
                              "attempt to access target epoch member."  );

         try
         {
            surfx.getSurfaceVector();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfaceInterceptWithDSKInfo: intercept " +
                              "not found, "                             +
                              "attempt to access DSK member."  );

         try
         {
            surfx.getDSK();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfaceInterceptWithDSKInfo: intercept " +
                              "not found, "                             +
                              "attempt to access DSKDescriptor member."  );

         try
         {
            surfx.getDSKDescriptor();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfaceInterceptWithDSKInfo: intercept " +
                              "not found, "                             +
                              "attempt to access DLADescriptor member."  );

         try
         {
            surfx.getDLADescriptor();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfaceInterceptWithDSKInfo: intercept " +
                              "not found, "                             +
                              "attempt to access ic member."  );

         try
         {
            surfx.getIntComponent();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfaceInterceptWithDSKInfo: intercept " +
                              "not found, "                             +
                              "attempt to access cc member."  );

         try
         {
            surfx.getDoubleComponent();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }


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
         ( new File( DSK0 ) ).delete();
      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

} /* End f_LimbPoint */











