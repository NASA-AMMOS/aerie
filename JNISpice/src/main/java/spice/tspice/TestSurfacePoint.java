package spice.tspice;

import java.io.*;
import static java.lang.Math.PI;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;
import static spice.basic.AngularUnits.DPR;
import static spice.basic.AngularUnits.RPD;



/**
Class TestSurfacePoint provides methods that implement test families for
the class SurfacePoint.


<h3> Version 1.0.0 03-JAN-2017 (NJB)</h3>

*/
public class TestSurfacePoint
{


   /**
   Test family 001 for methods of the class spice.basic.SurfacePoint.
   <pre>
   -Procedure f_SurfacePoint (Test SurfacePoint)

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

      This routine tests methods of class SurfacePoint. 

      Because it is convenient to do so, this routine also
      tests the `create' method of class {@link SurfaceIntercept}.

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

      -JNISpice Version 1.0.0 06-DEC-2016 (NJB) (EDW)

   -&
   </pre>
   */

   public static boolean f_SurfacePoint()

      throws SpiceException
   {
      //
      // Local constants
      //
      final String                   DSK0 = "surfacepoint_test0.bds";    
      final String                   PCK0 = "surfacepoint_test.tpc";    

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

      DSK                            dsk0 = null;

      DSKDescriptor                  dskdsc;

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
      String                         srfspc;
      String                         title;

      Surface[]                      srflst;

      SurfaceIntercept[]             surfxArr;

      SurfacePoint                   spoint;
      SurfacePoint                   spoint1;
      SurfacePoint[]                 spointArr;

      TDBTime                        et;
      TDBTime                        xepoch;

      Vector3[]                      dirs;
      Vector3                        normal;
      Vector3[]                      normlsArr;
      Vector3                        point;
      Vector3                        raydir;
      Vector3                        vertex;
      Vector3[]                      vrtces;
      Vector3                        xnorml;
      Vector3                        xpt;

      boolean[]                      fndArr = new boolean[1];
      boolean                        ok;
      boolean                        pri;

      double                         dlat;
      double                         dlon;
      double                         lat;
      double                         lon;
      double[][]                     lonlat;
      double                         r;
      double                         tol;
      double[]                       xnormlArr;
      double[]                       xxpt = new double[3];

      int                            bodyid;
      int                            i;
      int                            j;
      int                            k;
      int                            nlat;
      int                            nlon;
      int                            nrays;
      int                            nslat;
      int                            nslon;
      int                            nsurf;
      int[]                          plidArr = new int[1];
      int                            surfid;


      //
      // Start tests.
      //


      try
      {

         JNITestutils.topen ( "f_SurfacePoint" );




         //*********************************************************************
         //
         // Setup
         //
         //*********************************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Create and load LSK." );

         JNITestutils.tstlsk();



         //*********************************************************************
         //
         // Constructor tests
         //
         //*********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfacePoint no-args constructor" );
 
         //
         // Make sure we can make the call.
         //
         spoint = new SurfacePoint();
        

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfacePoint field-based constructor; " +
                              "accepts a time input."                   );

         point  = new Vector3(  1.0, 2.0, 3.0 );

         target = targs[0];

         fixref = frames[0];
         
         xepoch = new TDBTime ( 5.e8 );

         srfspc = "Ellipsoid";

         spoint = new SurfacePoint( srfspc, target, xepoch, fixref, point );

         //
         // Check members of spoint.
         //
         tol = 0.0;

         ok  = JNITestutils.chckad( "spoint.v", spoint.toArray(), "=",
                                                point.toArray(),  tol  );
 
         ok  = JNITestutils.chcksc( "spoint.body", 
                                    spoint.getBody().getName(), "=",
                                    target.getName()                 );

         ok  = JNITestutils.chcksd( "spoint.et", 
                                    spoint.getTDBTime().getTDBSeconds(), 
                                    "=",
                                    xepoch.getTDBSeconds(),  tol  );

         ok  = JNITestutils.chcksc( "spoint.fixref", 
                                    spoint.getReferenceFrame().getName(), "=",
                                    fixref.getName()                          );

         ok  = JNITestutils.chcksc( "spoint.surfspec", 
                                    spoint.getSurfaceSpecificationString(), "=",
                                    srfspc                                    );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfacePoint field-based constructor; " +
                              "does not accept a time input."                 );

         point  = new Vector3(  1.0, 2.0, 3.0 );

         target = targs[0];

         fixref = frames[0];
         
         xepoch = new TDBTime ( 5.e8 );

         srfspc = "Ellipsoid";

         spoint = new SurfacePoint( srfspc, target, fixref, point );

         //
         // Check members of spoint.
         //
         tol = 0.0;

         ok  = JNITestutils.chckad( "spoint.v", spoint.toArray(), "=",
                                                point.toArray(),  tol  );
 
         ok  = JNITestutils.chcksc( "spoint.body", 
                                    spoint.getBody().getName(), "=",
                                    target.getName()                 );

         ok  = JNITestutils.chcksd( "spoint.et", 
                                    spoint.getTDBTime().getTDBSeconds(), 
                                    "=",
                                    0.0,  tol  );

         ok  = JNITestutils.chcksc( "spoint.fixref", 
                                    spoint.getReferenceFrame().getName(), "=",
                                    fixref.getName()                          );

         ok  = JNITestutils.chcksc( "spoint.surfspec", 
                                    spoint.getSurfaceSpecificationString(), 
                                    "=", srfspc                            );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Test SurfacePoint copy constructor." );

         point  = new Vector3(  1.0, 2.0, 3.0 );

         target = targs[0];

         fixref = frames[0];
         
         xepoch = new TDBTime ( 5.e8 );

         srfspc = "Ellipsoid";

         spoint  = new SurfacePoint( srfspc, target, fixref, point );

         spoint1 = new SurfacePoint( spoint );



         //
         // Check members of spoint1.
         //
         tol = 0.0;

         ok  = JNITestutils.chckad( "spoint1.v", spoint1.toArray(), "=",
                                                 point.toArray(),  tol  );
 
         ok  = JNITestutils.chcksc( "spoint1.body", 
                                    spoint1.getBody().getName(), "=",
                                    target.getName()                 );

         ok  = JNITestutils.chcksd( "spoint1.et", 
                                    spoint1.getTDBTime().getTDBSeconds(), 
                                    "=",
                                    0.0,  tol  );

         ok  = JNITestutils.chcksc( "spoint1.fixref", 
                                    spoint1.getReferenceFrame().getName(), "=",
                                    fixref.getName()                          );

         ok  = JNITestutils.chcksc( "spoint1.surfspec", 
                                    spoint1.getSurfaceSpecificationString(), 
                                    "=", srfspc                               );

         //
         // Verify that `spoint1' is a deep copy: modify spoint; check spoint1.
         //
         spoint = new SurfacePoint();


         //
         // Check members of spoint1.
         //
         tol = 0.0;

         ok  = JNITestutils.chckad( "spoint1.v", spoint1.toArray(), "=",
                                                 point.toArray(),  tol  );
 
         ok  = JNITestutils.chcksc( "spoint1.body", 
                                    spoint1.getBody().getName(), "=",
                                    target.getName()                 );

         ok  = JNITestutils.chcksd( "spoint1.et", 
                                    spoint1.getTDBTime().getTDBSeconds(), 
                                    "=",
                                    0.0,  tol  );

         ok  = JNITestutils.chcksc( "spoint1.fixref", 
                                    spoint1.getReferenceFrame().getName(), "=",
                                    fixref.getName()                          );

         ok  = JNITestutils.chcksc( "spoint1.surfspec", 
                                    spoint1.getSurfaceSpecificationString(), 
                                    "=",  srfspc                            );



         //*********************************************************************
         //
         // Method tests
         //
         //*********************************************************************


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
               lat = (PI/2) - j*dlat;


               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfaceIntercept.create: "     +
                                      "test DSKXV analog method. "         +
                                      "Mars spear test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );
 
               JNITestutils.tcase ( title );

               // System.out.println ( title );

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
               dirs      = new Vector3[1];
               dirs[0]   = raydir;
               vrtces    = new Vector3[1];
               vrtces[0] = vertex;

               surfxArr  = SurfaceIntercept.create( pri,    target, srflst, et,
                                                    fixref, vrtces, dirs      );
               //
               // We expect to find an intercept every time. 
               //
               ok = JNITestutils.chcksl( "found", surfxArr[0].wasFound(), true);

               //
               // Use descriptors to find the expected intercept. 
               //
               CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                              vertex.toArray(), raydir.toArray(), 
                              plidArr,          xxpt,             fndArr );
               //
               // Check the intercept. 
               //
               ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

               tol = TIGHT;

               ok = JNITestutils.chckad( "surfxArr[0]", 
                                         surfxArr[0].getIntercept().toArray(),
                                         "~~/",         xxpt,            tol );


               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfacePoint.create: "         +
                                      "test LATSRF analog method. "        +
                                      "Mars spear test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );

               lonlat       = new double[1][2];

               lonlat[0][0] = lon;
               lonlat[0][1] = lat;

               srfspc       = "DSK/UNPRIORITIZED";
               spointArr    = SurfacePoint.create( srfspc, target, et,
                                                   fixref, lonlat     );
             
               //
               // We expect the returned array to contain a single element.
               //
               ok = JNITestutils.chcksi( "spointArr.length", 
                                         spointArr.length,   "=", 1, 0 );

               //
               // Check the surface point's Cartesian coordinates.
               //
               tol = TIGHT;

               ok = JNITestutils.chckad( "spointArr[0]", spointArr[0].toArray(),
                                         "~~/",          xxpt,            tol );

               //
               // Check the other attributes of the surface point: Body, Time,
               // ReferenceFrame, and surface specification.
               //
               ok = JNITestutils.chcksc( "spointArr[0].body", 
                                         spointArr[0].getBody().getName(),
                                         "=",
                                         target.getName()                 );

               ok = JNITestutils.chcksd( "spointArr[0].et", 
                                         spointArr[0].getTDBTime()
                                         .getTDBSeconds(),
                                         "~",
                                         et.getTDBSeconds(), tol );

               ok = JNITestutils.chcksc( "spointArr[0].fixref", 
                                         spointArr[0].getReferenceFrame()
                                         .getName(),
                                         "=",
                                         fixref.getName()                 );

               ok = JNITestutils.chcksc( "spointArr[0].surfspec", 
                                         spointArr[0]
                                         .getSurfaceSpecificationString(),
                                         "=",
                                         srfspc                          );

               //
               // --------Case-----------------------------------------------
               //
               //
               //  Test the surface normal vector computation methods of
               //  class SurfacePoint.
               //
               //

               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfacePoint.getNormal: "         +
                                      "Normal vector test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );

               JNITestutils.tcase ( title );

               //
               // Get the outward surface unit normal at the surface point
               //
               //    spointArr[0].
               //

               normal = spointArr[0].getNormal();

               //
               // Get the outward normal vector from the plate on which
               // spointArr[0] lies.
               //
               xnormlArr = CSPICE.dskn02 ( dsk0.getHandle(), dladsc.toArray(),
                                           plidArr[0]                         );
               
               xnorml    = new Vector3( xnormlArr );

               //
               // Check the normal vector. Perform this check only if
               // the latitude is bounded away from +/- pi/2 radians.
               //
               // There's little chance of agreement at the poles.
               //
               
               if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
               {

                  tol = TIGHT;

                  ok = JNITestutils.chckad( "normal", normal.toArray(),
                                            "~~/",    xnorml.toArray(), tol );
               }


               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfacePoint.getNormals: "        +
                                      "Normal vector test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );

               JNITestutils.tcase ( title );

               //
               // Get the outward surface unit normal at the array of surface 
               // points 
               //
               //    spointArr
               //
               // In this case the array has length 1.
               //

               normlsArr = SurfacePoint.getNormals( spointArr );

               //
               // Check the normal vector. Perform this check only if
               // the latitude is bounded away from +/- pi/2 radians.
               //
               // There's little chance of agreement at the poles.
               //
               
               if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
               {
                  tol = TIGHT;

                  ok = JNITestutils.chckad( "normal", normlsArr[0].toArray(),
                                            "~~/",    xnorml.toArray(), tol );
               }

               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfacePoint.getNormalsUnchecked:" +
                                      " normal vector test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );

               JNITestutils.tcase ( title );

               //
               // Get the outward surface unit normal at the array of surface 
               // points 
               //
               //    spointArr
               //
               // In this case the array has length 1.
               //
               // This is a boundary case for getNormalsUnchecked, since 
               // there is no cross-point checking to perform.
               //

               normlsArr = SurfacePoint.getNormalsUnchecked( spointArr );

               //
               // Check the normal vector. Perform this check only if
               // the latitude is bounded away from +/- pi/2 radians.
               //
               // There's little chance of agreement at the poles.
               //
               
               if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
               {
                  tol = TIGHT;

                  ok = JNITestutils.chckad( "normal", normlsArr[0].toArray(),
                                            "~~/",    xnorml.toArray(), tol );
               }

            }
         }




         // ********************************************************************
         //
         // MARS VECTORIZED CASE
         //
         //
         //
         // Now repeat the previous tests, this time using vectorized
         // calls to the "create" methods. 
         //
         // Start out by creating arrays of ray vertices and direction
         // vectors.
         //
         // ********************************************************************



         //
         // --------Case-----------------------------------------------
         //
         title = "Set up for Mars vectorized intercept test cases.";

         JNITestutils.tcase ( title );



         nrays = nslon * (nslat + 1);

         vrtces = new Vector3[nrays];
         dirs   = new Vector3[nrays];


         lonlat = new double[nrays][2];

         k      = 0;

         for ( i = 0;  i < nslon;  i++ )
         {
            //
            // Choose sample longitudes so that plate boundaries are not hit. 
            //
            lon = (0.5 * RPD) + (i * dlon);

            for ( j = 0;  j <= nslat;  j++ )
            {
               lat = (PI/2) - j*dlat;

               //
               // Create the ray's vertex and direction vector.
               //
               latcor = new LatitudinalCoordinates( r, lon, lat );
                
               vrtces[k] = latcor.toRectangular();
               dirs[k]   = vrtces[k].negate();   


               //
               // Save the horizontal latitudinal coordinates of the kth point
               // as well; these will be used for vectorized SurfacePoint
               // creation.
               //

               lonlat[k][0] = lon;
               lonlat[k][1] = lat;

               ++k;
            }
         }


         //
         // --------Case-----------------------------------------------
         //
         title = "Mars Vectorized intercept generation: non-empty " +
                 "surface list.";

         JNITestutils.tcase ( title );



         //
         // Find ray-surface intercepts for all the rays. Use a 
         // non-empty Surface list.
         //
         // The Surface list for Mars was initialized earlier.
         //
         surfxArr = SurfaceIntercept.create( pri,    target, srflst, et, 
                                             fixref, vrtces, dirs       );
         //
         // Check the results. 
         //

         //
         // We expect the returned array to contain `nrays' elements.
         //
         ok = JNITestutils.chcksi( "surfxArr.length", 
                                    surfxArr.length, "=", nrays, 0 );



         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();

            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfaceIntercept.create: "  +
                                   "Mars vectorized intercept test " +
                                   "for lon %f (deg),lat %f(deg). "  +
                                   "Surface list is non-empty.",
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );

            //
            // Use descriptors to find the expected intercept. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

            tol = TIGHT;

            label = String.format( "surfxArr[%d].found", k );

            ok = JNITestutils.chcksl( label, surfxArr[k].wasFound(),
                                      true                           );



            label = String.format( "surfxArr[%d]", k );

            ok = JNITestutils.chckad( label, 
                                      surfxArr[k].getIntercept().toArray(),
                                      "~~/", 
                                      xxpt,  tol );
         }


  

         //
         // --------Case-----------------------------------------------
         //
         title = "Mars Vectorized intercept generation: empty " +
                 "surface list.";

         JNITestutils.tcase ( title );


         //
         // Find ray-surface intercepts for all the rays. Use an
         // empty Surface list.
         //
         srflst = new Surface[0];

         surfxArr = SurfaceIntercept.create( pri,    target, srflst, et, 
                                             fixref, vrtces, dirs       );
         //
         // Check the results. 
         //

         //
         // We expect the returned array to contain `nrays' elements.
         //
         ok = JNITestutils.chcksi( "surfxArr.length", 
                                    surfxArr.length, "=", nrays, 0 );



         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();

            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfaceIntercept.create: "  +
                                   "Mars vectorized intercept test " +
                                   "for lon %f (deg),lat %f(deg). "  +
                                   "Surface list is empty.",
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );

            //
            // Use descriptors to find the expected intercept. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

            tol = TIGHT;

            label = String.format( "surfxArr[%d].found", k );

            ok = JNITestutils.chcksl( label, surfxArr[k].wasFound(),
                                      true                           );



            label = String.format( "surfxArr[%d]", k );

            ok = JNITestutils.chckad( label, 
                                      surfxArr[k].getIntercept().toArray(),
                                      "~~/", xxpt,            tol );
         }
  




         //
         // Class SurfacePoint test:
         //
         // Create surface points corresponding to the longitude/latitude
         // values of all vertices. 
         //

         //
         // --------Case-----------------------------------------------
         //
         title = "Mars Vectorized surface point generation";

         JNITestutils.tcase ( title );


         srfspc = "DSK/UNPRIORITIZED";

         spointArr = SurfacePoint.create( srfspc, target, et, 
                                          fixref, lonlat      );
         

         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();

            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfacePoint.create: "  +
                                   "Mars vectorized test " +
                                   "for lon %f (deg),lat %f(deg). ",
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );

            //
            // Use descriptors to find the expected surface point. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

            tol = TIGHT;

          


            label = String.format( "spointArr[%d]", k );

            ok = JNITestutils.chckad( label, spointArr[k].toArray(),
                                      "~~/", xxpt,            tol );
         }
  



 
         //
         // --------Case-----------------------------------------------
         //
         //
         //  Test the surface normal vector computation methods of
         //  class SurfacePoint.
         //
         //

         //
         // --------Case-----------------------------------------------
         //
         title = "Mars Vectorized normal vector generation: use " +
                 "checked creation method.";

         JNITestutils.tcase ( title );


         //
         // Find normal vectors at the ray-surface intercepts for all 
         // the rays. Use the array of SurfacePoints found previously.
         //
 
         normlsArr = SurfacePoint.getNormals( spointArr );

         //
         // Check the results. 
         //

         //
         // We expect the returned array to contain `nrays' elements.
         //
         ok = JNITestutils.chcksi( "normlsArr.length", 
                                    normlsArr.length, "=", nrays, 0 );



         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();


            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfacePoint.getNormals: "      +
                                   "Mars vectorized normal vector test " +
                                   "for lon %f (deg),lat %f(deg).",  
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );


            //
            // Use descriptors to find the plate ID for the kth intercept. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

  
            //
            // Get the outward surface unit normal at the surface point
            //
            //    spointArr[0].
            //

            normal = normlsArr[k];

            //
            // Get the outward normal vector from the plate on which
            // spointArr[0] lies.
            //
            xnormlArr = CSPICE.dskn02 ( dsk0.getHandle(), dladsc.toArray(),
                                        plidArr[0]                         );
               
            xnorml    = new Vector3( xnormlArr );

            //
            // Check the normal vector. Perform this check only if
            // the latitude is bounded away from +/- pi/2 radians.
            //
            // There's little chance of agreement at the poles.
            //
               
            if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
            {

               tol = TIGHT;

               ok = JNITestutils.chckad( "normal", normal.toArray(),
                                            "~~/",    xnorml.toArray(), tol );
            }
             
         }



         //
         // --------Case-----------------------------------------------
         //
         title = "Mars Vectorized normal vector generation: use " +
                 "unchecked creation method.";

         JNITestutils.tcase ( title );

       
         //
         // Find normal vectors at the ray-surface intercepts for all 
         // the rays. Use the array of SurfacePoints found previously.
         //
         // Create normal vectors using the "unchecked" method.
         //
 
         normlsArr = SurfacePoint.getNormalsUnchecked( spointArr );

         //
         // Check the results. 
         //

         //
         // We expect the returned array to contain `nrays' elements.
         //
         ok = JNITestutils.chcksi( "normlsArr.length", 
                                    normlsArr.length, "=", nrays, 0 );



         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();


            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfacePoint.getNormals: "      +
                                   "Mars vectorized normal vector test " +
                                   "for lon %f (deg),lat %f(deg).",      
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );


            //
            // Use descriptors to find the plate ID for the kth intercept. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

  
            //
            // Get the outward surface unit normal at the surface point
            //
            //    spointArr[0].
            //

            normal = normlsArr[k];

            //
            // Get the outward normal vector from the plate on which
            // spointArr[0] lies.
            //
            xnormlArr = CSPICE.dskn02 ( dsk0.getHandle(), dladsc.toArray(),
                                        plidArr[0]                         );
               
            xnorml    = new Vector3( xnormlArr );

            //
            // Check the normal vector. Perform this check only if
            // the latitude is bounded away from +/- pi/2 radians.
            //
            // There's little chance of agreement at the poles.
            //
               
            if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
            {

               tol = TIGHT;

               ok = JNITestutils.chckad( "normal", normal.toArray(),
                                         "~~/",    xnorml.toArray(), tol );
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
               lat = (PI/2) - j*dlat;

               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfaceIntercept.create: "       +
                                      "Saturn spear test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                               );
 
               JNITestutils.tcase ( title );

               // System.out.println ( title );

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
               dirs      = new Vector3[1];
               dirs[0]   = raydir;
               vrtces    = new Vector3[1];
               vrtces[0] = vertex;

               surfxArr  = SurfaceIntercept.create( pri,    target, srflst, et,
                                                    fixref, vrtces, dirs      );
               //
               // We expect to find an intercept every time. 
               //
               ok = JNITestutils.chcksl( "found", surfxArr[0].wasFound(), true);

               //
               // Use descriptors to find the expected intercept. 
               //
               CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                              vertex.toArray(), raydir.toArray(), 
                              plidArr,          xxpt,             fndArr );
               //
               // Check the intercept. 
               //
               ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

               tol = TIGHT;

               ok = JNITestutils.chckad( "surfxArr[0]", 
                                         surfxArr[0].getIntercept().toArray(),
                                         "~~/",         xxpt,            tol );



               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfacePoint.create: "           +
                                      "Saturn spear test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                              );

               lonlat       = new double[1][2];

               lonlat[0][0] = lon;
               lonlat[0][1] = lat;

               srfspc       = "DSK/UNPRIORITIZED";
               spointArr    = SurfacePoint.create( srfspc, target, et,
                                                   fixref, lonlat     );
             
               //
               // We expect the returned array to contain a single element.
               //
               ok = JNITestutils.chcksi( "spointArr.length", 
                                         spointArr.length,   "=", 1, 0 );

               //
               // Check the surface point's Cartesian coordinates.
               //
               tol = TIGHT;

               ok = JNITestutils.chckad( "spointArr[0]", spointArr[0].toArray(),
                                         "~~/",          xxpt,            tol );

               //
               // Check the other attributes of the surface point: Body, Time,
               // ReferenceFrame, and surface specification.
               //
               ok = JNITestutils.chcksc( "spointArr[0].body", 
                                         spointArr[0].getBody().getName(),
                                         "=",
                                         target.getName()                 );

               ok = JNITestutils.chcksd( "spointArr[0].et", 
                                         spointArr[0].getTDBTime()
                                         .getTDBSeconds(),
                                         "~",
                                         et.getTDBSeconds(), tol );

               ok = JNITestutils.chcksc( "spointArr[0].fixref", 
                                         spointArr[0].getReferenceFrame()
                                         .getName(),
                                         "=",
                                         fixref.getName()                 );

               ok = JNITestutils.chcksc( "spointArr[0].surfspec", 
                                         spointArr[0]
                                         .getSurfaceSpecificationString(),
                                         "=",
                                         srfspc                          );
 

               //
               // --------Case-----------------------------------------------
               //
               //
               //  Test the surface normal vector computation methods of
               //  class SurfacePoint.
               //
               //

               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfacePoint.getNormal: Saturn "  +
                                      "Normal vector test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );

               JNITestutils.tcase ( title );

               //
               // Get the outward surface unit normal at the surface point
               //
               //    spointArr[0].
               //

               normal = spointArr[0].getNormal();

               //
               // Get the outward normal vector from the plate on which
               // spointArr[0] lies.
               //
               xnormlArr = CSPICE.dskn02 ( dsk0.getHandle(), dladsc.toArray(),
                                           plidArr[0]                         );
               
               xnorml    = new Vector3( xnormlArr );

               //
               // Check the normal vector. Perform this check only if
               // the latitude is bounded away from +/- pi/2 radians.
               //
               // There's little chance of agreement at the poles.
               //
               
               if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
               {

                  tol = TIGHT;

                  ok = JNITestutils.chckad( "normal", normal.toArray(),
                                            "~~/",    xnorml.toArray(), tol );
               }


               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfacePoint.getNormals: Saturn " +
                                      "normal vector test for lon %f (deg), " +
                                      "lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );

               JNITestutils.tcase ( title );

               //
               // Get the outward surface unit normal at the array of surface 
               // points 
               //
               //    spointArr
               //
               // In this case the array has length 1.
               //

               normlsArr = SurfacePoint.getNormals( spointArr );

               //
               // Check the normal vector. Perform this check only if
               // the latitude is bounded away from +/- pi/2 radians.
               //
               // There's little chance of agreement at the poles.
               //
               
               if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
               {
                  tol = TIGHT;

                  ok = JNITestutils.chckad( "normal", normlsArr[0].toArray(),
                                            "~~/",    xnorml.toArray(), tol );
               }

               //
               // --------Case-----------------------------------------------
               //
               title = String.format( "Test SurfacePoint.getNormalsUnchecked:" +
                                      " Saturn normal vector test for "        +
                                      "lon %f (deg), lat %f(deg).",
                                      lon * DPR,
                                      lat * DPR                             );

               JNITestutils.tcase ( title );

               //
               // Get the outward surface unit normal at the array of surface 
               // points 
               //
               //    spointArr
               //
               // In this case the array has length 1.
               //
               // This is a boundary case for getNormalsUnchecked, since 
               // there is no cross-point checking to perform.
               //

               normlsArr = SurfacePoint.getNormalsUnchecked( spointArr );

               //
               // Check the normal vector. Perform this check only if
               // the latitude is bounded away from +/- pi/2 radians.
               //
               // There's little chance of agreement at the poles.
               //
               
               if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
               {
                  tol = TIGHT;

                  ok = JNITestutils.chckad( "normal", normlsArr[0].toArray(),
                                            "~~/",    xnorml.toArray(), tol );
               }

           }
         }
 

 

         // ********************************************************************
         //
         // SATURN VECTORIZED CASE
         //
         //
         //
         // Now repeat the previous tests, this time using vectorized
         // calls to the "create" methods. 
         //
         // Start out by creating arrays of ray vertices and direction
         // vectors.
         //
         // ********************************************************************



         //
         // --------Case-----------------------------------------------
         //
         title = "Set up for Saturn vectorized intercept test cases.";

         JNITestutils.tcase ( title );



         nrays = nslon * (nslat + 1);

         vrtces = new Vector3[nrays];
         dirs   = new Vector3[nrays];

         lonlat = new double[nrays][2];

         k = 0;

         for ( i = 0;  i < nslon;  i++ )
         {
            //
            // Choose sample longitudes so that plate boundaries are not hit. 
            //
            lon = (0.5 * RPD) + (i * dlon);

            for ( j = 0;  j <= nslat;  j++ )
            {
               lat = (PI/2) - j*dlat;

               //
               // Create the ray's vertex and direction vector.
               //
               latcor = new LatitudinalCoordinates( r, lon, lat );
                
               vrtces[k] = latcor.toRectangular();
               dirs[k]   = vrtces[k].negate();   

               //
               // Save the horizontal latitudinal coordinates of the kth point
               // as well; these will be used for vectorized SurfacePoint
               // creation.
               //

               lonlat[k][0] = lon;
               lonlat[k][1] = lat;


               ++k;
            }
         }

         //
         // Find ray-surface intercepts for all the rays. Use a 
         // non-empty Surface list.
         //
         // The Surface list for Saturn was initialized earlier.
         //
         surfxArr = SurfaceIntercept.create( pri,    target, srflst, et, 
                                             fixref, vrtces, dirs       );
         //
         // Check the results. 
         //

         //
         // We expect the returned array to contain `nrays' elements.
         //
         ok = JNITestutils.chcksi( "surfxArr.length", 
                                    surfxArr.length, "=", nrays, 0 );



         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();

            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfaceIntercept.create: "    +
                                   "Saturn vectorized intercept test " +
                                   "for lon %f (deg),lat %f(deg). "    +
                                   "Surface list is non-empty.",
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );

            //
            // Use descriptors to find the expected intercept. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

            tol = TIGHT;

            label = String.format( "surfxArr[%d].found", k );

            ok = JNITestutils.chcksl( label, surfxArr[k].wasFound(),
                                      true                           );



            label = String.format( "surfxArr[%d]", k );

            ok = JNITestutils.chckad( label, 
                                      surfxArr[k].getIntercept().toArray(),
                                      "~~/", xxpt,            tol );
         }
  



         //
         // Find ray-surface intercepts for all the rays. Use an
         // empty Surface list.
         //
         srflst = new Surface[0];

         surfxArr = SurfaceIntercept.create( pri,    target, srflst, et, 
                                             fixref, vrtces, dirs       );
         //
         // Check the results. 
         //

         //
         // We expect the returned array to contain `nrays' elements.
         //
         ok = JNITestutils.chcksi( "surfxArr.length", 
                                    surfxArr.length, "=", nrays, 0 );



         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();

            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfaceIntercept.create: "    +
                                   "Saturn vectorized intercept test " +
                                   "for lon %f (deg),lat %f(deg). "    +
                                   "Surface list is empty.",
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );

            //
            // Use descriptors to find the expected intercept. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

            tol = TIGHT;

            label = String.format( "surfxArr[%d].found", k );

            ok = JNITestutils.chcksl( label, surfxArr[k].wasFound(),
                                      true                           );



            label = String.format( "surfxArr[%d]", k );

            ok = JNITestutils.chckad( label, 
                                      surfxArr[k].getIntercept().toArray(),
                                      "~~/", xxpt,            tol );
         }
  

         //
         // Class SurfacePoint test:
         //
         // Create surface points corresponding to the longitude/latitude
         // values of all vertices. 
         //

         //
         // --------Case-----------------------------------------------
         //
         title = "Saturn Vectorized surface point generation";

         JNITestutils.tcase ( title );


         srfspc = "DSK/UNPRIORITIZED";

         spointArr = SurfacePoint.create( srfspc, target, et, 
                                          fixref, lonlat      );
         

         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();

            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfacePoint.create: "  +
                                   "Saturn vectorized test " +
                                   "for lon %f (deg),lat %f(deg). ",
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );

            //
            // Use descriptors to find the expected surface point. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

            tol = TIGHT;

          


            label = String.format( "spointArr[%d]", k );

            ok = JNITestutils.chckad( label, spointArr[k].toArray(),
                                      "~~/", xxpt,            tol );
         }
  



 
         //
         // --------Case-----------------------------------------------
         //
         //
         //  Test the surface normal vector computation methods of
         //  class SurfacePoint.
         //
         //

         //
         // --------Case-----------------------------------------------
         //
         title = "Saturn vectorized normal vector generation: use " +
                 "checked creation method.";

         JNITestutils.tcase ( title );


         //
         // Find normal vectors at the ray-surface intercepts for all 
         // the rays. Use the array of SurfacePoints found previously.
         //
 
         normlsArr = SurfacePoint.getNormals( spointArr );

         //
         // Check the results. 
         //

         //
         // We expect the returned array to contain `nrays' elements.
         //
         ok = JNITestutils.chcksi( "normlsArr.length", 
                                    normlsArr.length, "=", nrays, 0 );



         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();


            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfacePoint.getNormals: "        +
                                   "Saturn vectorized normal vector test " +
                                   "for lon %f (deg),lat %f(deg).",  
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );


            //
            // Use descriptors to find the plate ID for the kth intercept. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

  
            //
            // Get the outward surface unit normal at the surface point
            //
            //    spointArr[0].
            //

            normal = normlsArr[k];

            //
            // Get the outward normal vector from the plate on which
            // spointArr[0] lies.
            //
            xnormlArr = CSPICE.dskn02 ( dsk0.getHandle(), dladsc.toArray(),
                                        plidArr[0]                         );
               
            xnorml    = new Vector3( xnormlArr );

            //
            // Check the normal vector. Perform this check only if
            // the latitude is bounded away from +/- pi/2 radians.
            //
            // There's little chance of agreement at the poles.
            //
               
            if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
            {

               tol = TIGHT;

               ok = JNITestutils.chckad( "normal", normal.toArray(),
                                            "~~/",    xnorml.toArray(), tol );
            }
             
         }



         //
         // --------Case-----------------------------------------------
         //
         title = "Saturn Vectorized normal vector generation: use " +
                 "unchecked creation method.";

         JNITestutils.tcase ( title );

       
         //
         // Find normal vectors at the ray-surface intercepts for all 
         // the rays. Use the array of SurfacePoints found previously.
         //
         // Create normal vectors using the "unchecked" method.
         //
 
         normlsArr = SurfacePoint.getNormalsUnchecked( spointArr );

         //
         // Check the results. 
         //

         //
         // We expect the returned array to contain `nrays' elements.
         //
         ok = JNITestutils.chcksi( "normlsArr.length", 
                                    normlsArr.length, "=", nrays, 0 );



         for ( k = 0;  k < nrays;  k++ )
         {
            latcor = new LatitudinalCoordinates( vrtces[k] );

            lon    = latcor.getLongitude();
            lat    = latcor.getLatitude();


            //
            // --------Case-----------------------------------------------
            //
            title = String.format( "Test SurfacePoint.getNormals: "        +
                                   "Saturn vectorized normal vector test " +
                                   "for lon %f (deg),lat %f(deg).",      
                                   lon * DPR,
                                   lat * DPR                          );
 
            JNITestutils.tcase ( title );


            //
            // Use descriptors to find the plate ID for the kth intercept. 
            //
            vertex = vrtces[k];
            raydir = dirs[k];

            CSPICE.dskx02( dsk0.getHandle(), dladsc.toArray(),  
                           vertex.toArray(), raydir.toArray(), 
                           plidArr,          xxpt,             fndArr );
            //
            // Check the intercept. 
            //
            ok  = JNITestutils.chcksl( "dskx02 found", fndArr[0], true);

  
            //
            // Get the outward surface unit normal at the surface point
            //
            //    spointArr[0].
            //

            normal = normlsArr[k];

            //
            // Get the outward normal vector from the plate on which
            // spointArr[0] lies.
            //
            xnormlArr = CSPICE.dskn02 ( dsk0.getHandle(), dladsc.toArray(),
                                        plidArr[0]                         );
               
            xnorml    = new Vector3( xnormlArr );

            //
            // Check the normal vector. Perform this check only if
            // the latitude is bounded away from +/- pi/2 radians.
            //
            // There's little chance of agreement at the poles.
            //
               
            if (  Math.abs(lat)  <  (PI/2) - (dlat/2)  )
            {

               tol = TIGHT;

               ok = JNITestutils.chckad( "normal", normal.toArray(),
                                         "~~/",    xnorml.toArray(), tol );
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
         // Test the DSKXV analog `create'.
         //

         //
         // --------Case-----------------------------------------------
         //
         title = String.format( "SurfaceIntercept.create: frame not " +
                                "centered on target body."             );

         JNITestutils.tcase( title );


         try
         {
            pri       = false;
            target    = targs [0];
            fixref    = frames[1];
            et        = new TDBTime(0.0);
            point     = new Vector3( 1.0, 2.0, 3.0 );

            vrtces    = new Vector3[1];
            dirs      = new Vector3[1];

            vrtces[0] = new Vector3 ( 0.0, 0.0, 1e7 );
            dirs[0]   = vrtces[0].negate();
         
            //
            // Use an empty surface list.
            //
            srflst    = new Surface[0];

            surfxArr  = SurfaceIntercept.create( pri, target, srflst,
                                                 et,  fixref, vrtces, dirs );
             
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
         title = String.format( "SurfaceIntercept.create: input "     +
                                "vertex and direction vector arrays " +
                                "are empty."                            );

         JNITestutils.tcase( title );

         try
         {
            pri       = false;
            target    = targs [0];
            fixref    = frames[0];
            et        = new TDBTime(0.0);
            point     = new Vector3( 1.0, 2.0, 3.0 );

            vrtces    = new Vector3[0];
            dirs      = new Vector3[0];

          
            //
            // Use an empty surface list.
            //
            srflst    = new Surface[0];

            surfxArr  = SurfaceIntercept.create( pri, target, srflst,
                                                 et,  fixref, vrtces, dirs );
             
            Testutils.dogDidNotBark (  "SPICE(INVALIDCOUNT)" );  

         } 
         catch ( SpiceException exc )
         {
            //exc.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(INVALIDCOUNT)", exc );
         }



        //
         // --------Case-----------------------------------------------
         //
         title = String.format( "SurfaceIntercept.create: input "     +
                                "vertex and direction vector arrays " +
                                "have mismatched sizes."               );

         JNITestutils.tcase( title );

         try
         {
            pri       = false;
            target    = targs [0];
            fixref    = frames[0];
            et        = new TDBTime(0.0);
            point     = new Vector3( 1.0, 2.0, 3.0 );

            vrtces    = new Vector3[3];
            dirs      = new Vector3[4];

          
            //
            // Use an empty surface list.
            //
            srflst    = new Surface[0];

            surfxArr  = SurfaceIntercept.create( pri, target, srflst,
                                                 et,  fixref, vrtces, dirs );
             
            Testutils.dogDidNotBark (  "SPICE(SIZEMISMATCH)" );  

         } 
         catch ( SpiceException exc )
         {
            //exc.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(SIZEMISMATCH)", exc );
         }


         //*********************************************************************
         //
         //  Tests for class SurfacePoint exception handling
         //
         //*********************************************************************

         //
         // Each method that can cause a CSPICE error to be generated must
         // be tested for at least one such case.
         //

         //
         // --------Case-----------------------------------------------
         //
         title = String.format( "SurfacePoint.getNormals: bad frame in " +
                                "surface point instance."                 );

         JNITestutils.tcase( title );

         try
         {
            //
            // Create a SurfacePoint instance with a frame not 
            // centered on the body.
            // 
            spointArr = new SurfacePoint[1];
            
            target    = targs [0];
            fixref    = frames[1];
            et        = new TDBTime(0.0);
            point     = new Vector3( 1.0, 2.0, 3.0 );
            srfspc    = "Ellipsoid";

            spointArr[0] = new SurfacePoint( srfspc, target, et, 
                                             fixref, point      );

            normlsArr = SurfacePoint.getNormals( spointArr );
            
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
         title = String.format( "SurfacePoint.getNormals: incompatible " +
                                "attributes of input SurfacePoints."      );

         JNITestutils.tcase( title );

         //
         // This error is detected within the Java implementation, not
         // in native C code.
         //

         try
         {
            //
            // Create two valid SurfacePoint instances for different bodies.
            // 
            spointArr = new SurfacePoint[2];
            
            et        = new TDBTime(0.0);
            point     = new Vector3( 1.0, 2.0, 3.0 );
            srfspc    = "Ellipsoid";

            for ( i = 0;  i < 2;  i++ )
            {
               spointArr[i] = new SurfacePoint( srfspc,
                                                targs[i],
                                                et,
                                                frames[i],
                                                point      );
            }

            normlsArr = SurfacePoint.getNormals( spointArr );
            
            Testutils.dogDidNotBark (  "SPICE(BADATTRIBUTES)" );  

         } 
         catch ( SpiceException exc )
         {
            //exc.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(BADATTRIBUTES)", exc );
         }


         //
         // --------Case-----------------------------------------------
         //
         title = String.format( "SurfacePoint.getNormalsUnchecked: bad " +
                                "frame in surface point instance."        );

         JNITestutils.tcase( title );


         spointArr = new SurfacePoint[1];
            
         et        = new TDBTime(0.0);
         point     = new Vector3( 1.0, 2.0, 3.0 );
         srfspc    = "Ellipsoid";

         spointArr[0] = new SurfacePoint( srfspc,
                                          targs[0],
                                          et,
                                          frames[1],
                                          point      );

         try
         {
            //
            // Use the SurfacePoint instance from the previous case.
            //
            normlsArr = SurfacePoint.getNormalsUnchecked( spointArr );
            
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
         title = String.format( "SurfacePoint.getNormals: empty input " +
                                "point array."                           );

         JNITestutils.tcase( title );

         try
         {
            //
            // Create a SurfacePoint instance with a frame not 
            // centered on the body.
            // 
            spointArr = new SurfacePoint[0];
            
            target    = targs [0];
            fixref    = frames[1];
            et        = new TDBTime(0.0);
            point     = new Vector3( 1.0, 2.0, 3.0 );
            srfspc    = "Ellipsoid";

            normlsArr = SurfacePoint.getNormals( spointArr );
            
            Testutils.dogDidNotBark (  "SPICE(INVALIDCOUNT)" );  

         } 
         catch ( SpiceException exc )
         {
            //exc.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(INVALIDCOUNT)", exc );
         }







         //
         // --------Case-----------------------------------------------
         //
         title = String.format( "SurfacePoint.getNormal: bad " +
                                "frame in surface point instance."        );

         JNITestutils.tcase( title );

         try
         {

            spointArr = new SurfacePoint[1];
            
            et        = new TDBTime(0.0);
            point     = new Vector3( 1.0, 2.0, 3.0 );
            srfspc    = "Ellipsoid";

            spointArr[0] = new SurfacePoint( srfspc,
                                             targs[0],
                                             et,
                                             frames[1],
                                             point      );

            normal = spointArr[0].getNormal();
            
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
         title = String.format( "SurfacePoint.create: empty input " +
                                "coordinate pair array."              );
         JNITestutils.tcase( title );

         try
         {
            //
            //  Try to create a SurfacePoint instance using fields obtained 
            //  from the SurfacePoint instance we already have.
            //
            lonlat = new double[0][2];

            spoint = new SurfacePoint( srfspc,
                                       targs[0],
                                       et,
                                       frames[1],
                                       point      );
            spointArr = 

               SurfacePoint.create( spoint.getSurfaceSpecificationString(),
                                    spoint.getBody(),
                                    spoint.getTDBTime(),
                                    spoint.getReferenceFrame(),
                                    lonlat                                 );

            
            Testutils.dogDidNotBark (  "SPICE(INVALIDCOUNT)" );  

         } 
         catch ( SpiceException exc )
         {
            //exc.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(INVALIDCOUNT)", exc );
         }




         //
         // --------Case-----------------------------------------------
         //
         title = String.format( "SurfacePoint.create: incompatible " +
                                "frame and target inputs."            );
         JNITestutils.tcase( title );

         try
         {
            //
            //  Try to create a SurfacePoint instance using fields obtained 
            //  from the SurfacePoint instance we already have.
            //
            lonlat       = new double[1][2];

            lonlat[0][0] = PI/2;
            lonlat[0][1] = PI/4;
 
            spoint    = spointArr[0];

            spointArr = 

               SurfacePoint.create( spoint.getSurfaceSpecificationString(),
                                    spoint.getBody(),
                                    spoint.getTDBTime(),
                                    spoint.getReferenceFrame(),
                                    lonlat                                 );

            
            Testutils.dogDidNotBark (  "SPICE(INVALIDFRAME)" );  

         } 
         catch ( SpiceException exc )
         {
            //exc.printStackTrace();

            ok = JNITestutils.chckth ( true, "SPICE(INVALIDFRAME)", exc );
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

         //ex.printStackTrace();

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
         dsk0.close();

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











