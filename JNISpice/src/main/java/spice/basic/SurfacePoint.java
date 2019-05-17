
package spice.basic;

/**
Class SurfacePoint represents 3-dimensional points located
on surfaces of specified bodies.

<p> Methods of this class may be used to map planetocentric
(longitude, latitude) coordinate pairs to surface points, 
and to compute outward surface normal vectors at specified
surface points. See the methods {@link #create} and
{@link #getNormals}.

<p> A SurfacePoint instance consists of
<ul>
<li> A 3-dimensional vector.</li>
<li> A body.</li>
<li> A body-fixed, body-centered reference frame</li>
<li> A time. Times are used for DSK segment selection.
<li> A surface specification.</li>
</ul> 

<p> 
The surface specification may be assigned the following values:    

   <pre>

   "ELLIPSOID" 

      The SurfacePoint instance uses a triaxial 
      ellipsoid to model the surface of the target 
      body. The ellipsoid's radii must be available 
      in the kernel pool. 


   "DSK/UNPRIORITIZED[/SURFACES = &#60surface list&#62]" 

      The SurfacePoint instance uses topographic data 
      to model the surface of the target body. These 
      data must be provided by loaded DSK files. 

      The surface list specification is optional. The 
      syntax of the list is 

         &#60surface 1&#62 [, &#60surface 2&#62...] 

      If present, it indicates that data only for the 
      listed surfaces are to be used; however, data 
      need not be available for all surfaces in the 
      list. If absent, loaded DSK data for any surface 
      associated with the target body are used. 

      The surface list may contain surface names or 
      surface ID codes. Names containing blanks must 
      be delimited by escaped double quotes, for example 

         "SURFACES = \"Mars MEGDR 128 PIXEL/DEG\"" 

      If multiple surfaces are specified, their names 
      or IDs must be separated by commas. 

</pre>

<p>
   See the Particulars section below for details 
   concerning use of DSK data. 

<p>
   Neither case nor white space are significant in the surface 
   specification, except within double-quoted strings. For 
   example, the string " eLLipsoid " is valid. 

<p>
   Within double-quoted strings, blank characters are 
   significant, but multiple consecutive blanks are 
   considered equivalent to a single blank. Case is  
   not significant. So 
<pre>
   "Mars MEGDR 128 PIXEL/DEG" 
</pre>
   is equivalent to  
<pre>
   " mars megdr  128  pixel/deg " 
</pre>
   but not to 
<pre>
   "MARS MEGDR128PIXEL/DEG" 
</pre>


<h3> Particulars </h3>

<pre>
Using DSK data 
============== 

   DSK loading and unloading 
   ------------------------- 

   DSK files providing data used by this routine are loaded by 
   calling {@link KernelDatabase#load} and can be unloaded by 
   {@link KernelDatabase#unload} calling or {@link 
   KernelDatabase#clear}. See the documentation of 
   KernelDatabase.load  for limits on numbers 
   of loaded DSK files. 

   For run-time efficiency, it's desirable to avoid frequent 
   loading and unloading of DSK files. When there is a reason to 
   use multiple versions of data for a given target body---for 
   example, if topographic data at varying resolutions are to be 
   used---the surface list can be used to select DSK data to be 
   used for a given computation. It is not necessary to unload 
   the data that are not to be used. This recommendation presumes 
   that DSKs containing different versions of surface data for a 
   given body have different surface ID codes. 


   DSK data priority 
   ----------------- 

   A DSK coverage overlap occurs when two segments in loaded DSK 
   files cover part or all of the same domain---for example, a 
   given longitude-latitude rectangle---and when the time 
   intervals of the segments overlap as well. 

   When DSK data selection is prioritized, in case of a coverage 
   overlap, if the two competing segments are in different DSK 
   files, the segment in the DSK file loaded last takes 
   precedence. If the two segments are in the same file, the 
   segment located closer to the end of the file takes 
   precedence. 

   When DSK data selection is unprioritized, data from competing 
   segments are combined. For example, if two competing segments 
   both represent a surface as sets of triangular plates, the 
   union of those sets of plates is considered to represent the 
   surface.  

   Currently only unprioritized data selection is supported. 
   Because prioritized data selection may be the default behavior 
   in a later version of the routine, the UNPRIORITIZED keyword is 
   required in the surface specification. 


   Syntax of the surface specification
   ----------------------------------- 

   The keywords and surface list in the surface specification
   are called "clauses." The clauses may appear in any 
   order, for example 

      DSK/UNPRIORITIZED/&#60surface list&#62 
      DSK/&#60surface list&#62/UNPRIORITIZED 
      UNPRIORITIZED/&#60surface list&#62/DSK

   The simplest form of a surface specification indicating use of 
   DSK data is one that lacks a surface list, for example: 

      "DSK/UNPRIORITIZED" 

   For applications in which all loaded DSK data for the target 
   body are for a single surface, and there are no competing 
   segments, the above strings suffice. This is expected to be 
   the usual case. 

   When, for the specified target body, there are loaded DSK 
   files providing data for multiple surfaces for that body, the 
   surfaces to be used by this routine for a given call must be 
   specified in a surface list, unless data from all of the 
   surfaces are to be used together. 

   The surface list consists of the string 

      SURFACES = 

   followed by a comma-separated list of one or more surface 
   identifiers. The identifiers may be names or integer codes in 
   string format. For example, suppose we have the surface 
   names and corresponding ID codes shown below: 

      Surface Name                              ID code 
      ------------                              ------- 
      "Mars MEGDR 128 PIXEL/DEG"                1 
      "Mars MEGDR 64 PIXEL/DEG"                 2 
      "Mars_MRO_HIRISE"                         3 

   If data for all of the above surfaces are loaded, then 
   data for surface 1 can be specified by either 

      "SURFACES = 1" 

   or 

      "SURFACES = \"Mars MEGDR 128 PIXEL/DEG\"" 

   Double quotes are used to delimit the surface name because 
   it contains blank characters.  

   To use data for surfaces 2 and 3 together, any 
   of the following surface lists could be used: 

      "SURFACES = 2, 3" 

      "SURFACES = \"Mars MEGDR  64 PIXEL/DEG\", 3" 

      "SURFACES = 2, Mars_MRO_HIRISE" 

      "SURFACES = \"Mars MEGDR 64 PIXEL/DEG\", Mars_MRO_HIRISE" 

   An example of a surface specification that could be constructed 
   using one of the surface lists above is 

   "DSK/UNPRIORITIZED/SURFACES= \"Mars MEGDR 64 PIXEL/DEG\",3" 
</pre>



<h3> Version 1.0.0 28-DEC-2016 (NJB)</h3>

*/
public class SurfacePoint extends Vector3
{

   //
   // Public fields
   //
   
   Body                                 body;
   ReferenceFrame                       fixref;
   TDBTime                              et;
   
   //
   // Private fields
   // 
   String                               surfspec;

   //
   // Public constructors
   //

   /**
   No-arguments constructor.
   */
   public SurfacePoint()
   {
   }

   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public SurfacePoint ( SurfacePoint sp )
   
      throws SpiceException
   {
      super( sp );

      this.surfspec = new String        ( sp.surfspec );
      this.body     = new Body          ( sp.body     );
      this.et       = new TDBTime       ( sp.et       );
      this.fixref   = new ReferenceFrame( sp.fixref   );
   }

   /**
   Construct a SurfacePoint instance from a 3-vector, a
   body-fixed, body-centered reference frame, a time, and a surface
   specification string. See the discussion in the class
   documentation above for details.
   */
   public SurfacePoint ( String           surfspec,
                         Body             body,
                         Time             t,                         
                         ReferenceFrame   fixref,
                         Vector3          point   )

      throws SpiceException
   {
      super( point );

      this.surfspec = new String        ( surfspec );
      this.body     = new Body          ( body     );
      this.et       = new TDBTime       ( t        );
      this.fixref   = new ReferenceFrame( fixref   );
   }

   /**
   Construct a SurfacePoint instance from a 3-vector, a
   body-fixed, body-centered reference frame, and a surface
   specification string. A default time is used. This method is
   appropriate for creating SurfacePoints having ellipsoidal
   shape specifications.

   <p> See the discussion in the class
   documentation above for details.
   */
   public SurfacePoint ( String           surfspec,
                         Body             body,
                         ReferenceFrame   fixref,
                         Vector3          point   )

      throws SpiceException
   {
      super( point );

      this.surfspec = new String        ( surfspec );
      this.body     = new Body          ( body     );
      this.et       = new TDBTime       ( 0.0      );
      this.fixref   = new ReferenceFrame( fixref   );
   }




   //
   // Public methods
   //

   /**
   Create an array of SurfacePoints from an array of planetocentric
   (longitude, latitude) coordinate pairs. This method is an analog
   of the CSPICE method latsrf_c.

   <p> Units of the input points are radians.

   <p> The ith element of the returned SurfacePoint array is the 
   surface point corresponding to the ith input coordinate pair.

   <p> This method is appropriate only for surfaces that have a 
   unique point for each pair of longitude and latitude coordinates.

   <p> See the class documentation above for a discussion of
   surface specifications.

   <h3>Code Examples</h3>

<p> 
   The numerical results shown for these examples may differ across 
   platforms. The results depend on the SPICE kernels used as 
   input, the compiler and supporting libraries, and the machine  
   specific arithmetic implementation.  
 
 
   <ol> 
   
<li>
Find the surface points on a target body corresponding to a 
given planetocentric longitude/latitude grid. In order to 
duplicate the example output, the name of the meta-kernel
shown below should be supplied at the prompt. 
 
<p>  Use the meta-kernel shown below to load the required SPICE
     kernels.

<pre>
KPL/MK 

File: SurfacePointEx1.tm 

This meta-kernel is intended to support operation of SPICE 
example programs. The kernels shown here should not be 
assumed to contain adequate or correct versions of data 
required by SPICE-based user applications. 

In order for an application to use this meta-kernel, the 
kernels referenced here must be present in the user's 
current working directory. 

The names and contents of the kernels referenced 
by this meta-kernel are as follows: 

  File name                        Contents 
  ---------                        -------- 
  phobos512.bds                    DSK based on 
                                   Gaskell ICQ Q=512 
                                   Phobos plate model 
\begindata 

  PATH_SYMBOLS    = 'GEN' 
  PATH_VALUES     = '/ftp/pub/naif/generic_kernels' 

  KERNELS_TO_LOAD = ( '$GEN/dsk/phobos/phobos512.bds' ) 
\begintext 

</pre>


<p> Example code begins here.

<pre>
//
// Program SurfacePointEx1
//

import java.io.*;
import spice.basic.*;
import static spice.basic.AngularUnits.*;
import static java.lang.Math.PI;

//
// This program demonstrates use of the method
// {@link #create}.
//
// Find the surface points on a target body corresponding to a 
// given planetocentric longitude/latitude grid.
//
public class SurfacePointEx1
{
   //
   // Load SPICE shared library.
   //
   static{ System.loadLibrary( "JNISpice" ); }


   public static void main( String[] args )

      throws SpiceException
   {
      //
      // Local constants
      //
      final int                         MAXN   = 10000;

      //
      // Local variables
      //
      Body                              target = new Body( "PHOBOS" );
      
      Double[]                          coords;

      LimbPoint[][]                     limbPoints;

      RADecCoordinates                  radCoords;

      ReferenceFrame                    fixref =
                                           new ReferenceFrame( "IAU_PHOBOS" );

      String                            dsk;
      String                            surfSpec = "DSK/UNPRIORITIZED";
                                   
      SurfacePoint[]                    srfpts;
             
      TDBTime                           et     = new TDBTime( 0.0 );

      double                            dlat;
      double                            dlon;
      double                            lat;
      double                            lat0;
      double                            lon;
      double                            lon0;
      double[][]                        lonLatGrid;            
      double                            roll;
      double                            schstp;
      double                            soltol;
      double                            xlat;
      double                            xlon;
      double                            xr;

      int                               i;
      int                               j;
      int                               k;
      int                               n;
      int                               nlat;
      int                               nlon;


      try
      {
         //
         // Prompt for the name of the DSK to read.
         //
         dsk = IOUtils.prompt( "Enter DSK name   > " );

         //
         // Load the DSK file.
         //
         KernelDatabase.load( dsk );

         //
         // Set the grid dimensions.
         //
         nlon = 6;
         nlat = 3;

         //
         // Derive evenly spaced grid separations and starting
         // values in the longitude and latitude dimensions.
         // Units are degrees.
         //
         lat0 = 90.0;
         lon0 =  0.0;

         dlat = 180.0 / (nlat + 1);
         dlon = 360.0 /  nlon;

         //
         // Now generate the grid points. We generate
         // points along latitude bands, working from
         // north to south. The latitude range is selected
         // to range from +45 to -45 degrees. Longitude
         // ranges from 0 to 300 degrees. The increment
         // is 45 degrees for latitude and 60 degrees for
         // longitude.
         //

         n          = nlat * nlon;
         lonLatGrid = new double[n][2];
         k          = 0;

         for ( i = 0;  i < nlat;  i++ )
         {
            lat = RPD * ( lat0 - (i+1)*dlat );

            for ( j = 0;  j < nlon;  j++ )
            {
               lon = RPD * ( lon0 + j*dlon );
              
               lonLatGrid[k][0] = lon;
               lonLatGrid[k][1] = lat;

               ++k;
            }
         }

         //
         // Find the surface points corresponding to the grid points.
         //
         srfpts = SurfacePoint.create( surfSpec, target,     et, 
                                       fixref,   lonLatGrid      );

         //
         // Print out the surface points in latitudinal
         // coordinates and compare the derived lon/lat values
         // to those of the input grid.
         //
         for ( i = 0;  i < n;  i++ )
         {
            //
            //  Use RADecCoordinates rather than LatitudinalCoordinates
            //  to produce non-negative longitudes.
            //
            radCoords = new RADecCoordinates( srfpts[i] );
 

            System.out.format ( 

              "%n"                                 +
              "Surface point for grid point %d:%n" +
              "  Cartesian coordinates: "          +
              "(%11.4e, %11.4e, %11.4e)%n"         +
              "  Latitudinal Coordinates:%n"       +
              "   Longitude (deg): %12.6f%n"       +
              "   Latitude  (deg): %12.6f%n"       +
              "   Radius     (km): %12.6f%n"       +
              "%n"                                 +
              "  Original Grid Coordinates:%n"     +
              "   Longitude (deg): %12.6f%n"       +
              "   Latitude  (deg): %12.6f%n"       +
              "%n",
              i,
              srfpts[i].getElt(0),
              srfpts[i].getElt(1),
              srfpts[i].getElt(2),
              radCoords.getRightAscension() * DPR,              
              radCoords.getDeclination()    * DPR,
              radCoords.getRadius(),
              lonLatGrid[i][0] * DPR,
              lonLatGrid[i][1] * DPR              ); 
         }

         System.out.format ( "%n" );

      } // End of try block

      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }

      catch ( java.io.IOException exc )
      {
         exc.printStackTrace();
      }


   } // End of main method 
   
}

</pre>

<p> When this program was executed on a PC/Linux/gcc/64-bit/java 1.5
platform, the output for the first 3 points and the last 3 points 
(the rest of the output is not shown due to its large volume) 
was: 

<pre>
Surface point for grid point 0:
  Cartesian coordinates: ( 7.1817e+00,  0.0000e+00,  7.1817e+00)
  Latitudinal Coordinates:
   Longitude (deg):     0.000000
   Latitude  (deg):    45.000000
   Radius     (km):    10.156402

  Original Grid Coordinates:
   Longitude (deg):     0.000000
   Latitude  (deg):    45.000000


Surface point for grid point 1:
  Cartesian coordinates: ( 3.5820e+00,  6.2042e+00,  7.1640e+00)
  Latitudinal Coordinates:
   Longitude (deg):    60.000000
   Latitude  (deg):    45.000000
   Radius     (km):    10.131412

  Original Grid Coordinates:
   Longitude (deg):    60.000000
   Latitude  (deg):    45.000000


Surface point for grid point 2:
  Cartesian coordinates: (-3.6854e+00,  6.3832e+00,  7.3707e+00)
  Latitudinal Coordinates:
   Longitude (deg):   120.000000
   Latitude  (deg):    45.000000
   Radius     (km):    10.423766

  Original Grid Coordinates:
   Longitude (deg):   120.000000
   Latitude  (deg):    45.000000


  ...


Surface point for grid point 15:
  Cartesian coordinates: (-8.2374e+00,  1.5723e-15, -8.2374e+00)
  Latitudinal Coordinates:
   Longitude (deg):   180.000000
   Latitude  (deg):   -45.000000
   Radius     (km):    11.649512

  Original Grid Coordinates:
   Longitude (deg):   180.000000
   Latitude  (deg):   -45.000000


Surface point for grid point 16:
  Cartesian coordinates: (-3.6277e+00, -6.2833e+00, -7.2553e+00)
  Latitudinal Coordinates:
   Longitude (deg):   240.000000
   Latitude  (deg):   -45.000000
   Radius     (km):    10.260572

  Original Grid Coordinates:
   Longitude (deg):   240.000000
   Latitude  (deg):   -45.000000


Surface point for grid point 17:
  Cartesian coordinates: ( 3.2881e+00, -5.6952e+00, -6.5762e+00)
  Latitudinal Coordinates:
   Longitude (deg):   300.000000
   Latitude  (deg):   -45.000000
   Radius     (km):     9.300154

  Original Grid Coordinates:
   Longitude (deg):   300.000000
   Latitude  (deg):   -45.000000



</pre>
</li>


   </ol>

   
   */
   public static SurfacePoint[] create ( String            surfspec,
                                         Body              body,     
                                         Time              t,
                                         ReferenceFrame    fixref,
                                         double[][]        lonlat  )
      throws SpiceException
   {

      SpiceErrorException               exc;
      SurfacePoint[]                    retArray;    

      int                               i;
      int                               npts         = lonlat.length;
      double[][]                        pointsArray;



      if ( npts < 1 )
      {
         String errmsg 

            = String.format ( "Number of ray input coordinate pairs = %d; "  +
                              "count must be > 0.",
                              npts                                            );

         exc = SpiceErrorException.create( "SurfacePoint.create",
                                           "SPICE(INVALIDCOUNT)",
                                           errmsg                    );
         throw( exc );
      }


      retArray    = new SurfacePoint[npts];

      pointsArray = CSPICE.latsrf( surfspec,          body.getName(), 
                                   t.getTDBSeconds(), fixref.getName(),
                                   npts,              lonlat            );

      for ( i = 0;  i < npts;  i++ )
      {
         retArray[i] = new SurfacePoint ( surfspec,      
                                          body, 
                                          t,
                                          fixref,
                                          new Vector3( pointsArray[i] )  );
      }

      return( retArray );            
   }
                   


   /**
   Compute the unit length outward normal vector at a specified SurfacePoint.
 
   <p> Also see the method {@link #getNormals}.
   */
   public Vector3 getNormal()

      throws SpiceException
   {
      double[][]                        points;
      double[][]                        normals;


      points    = new double[1][];

      //
      // `v' is the double[] member of the superclass Vector3.
      //
      points[0] = v;

      normals   = CSPICE.srfnrm( surfspec, 
                                 body.getName(), 
                                 et.getTDBSeconds(), 
                                 fixref.getName(), 
                                 1,
                                 points            );
 

      return(  new Vector3( normals[0] )  );  
   }


   /**
   Compute the unit length outward normal vectors corresponding
   to an array of SurfacePoint instances. This method is an analog
   of the CSPICE method srfnrm_c.
 
   <p> All elements of the input SurfacePoint array must have
   matching attributes, other than their "point" members.

   <p> Also see the methods {@link #getNormal}, 
   {@link #getNormalsUnchecked}.



   <h3>Code Examples</h3>

<p> 
   The numerical results shown for these examples may differ across 
   platforms. The results depend on the SPICE kernels used as 
   input, the compiler and supporting libraries, and the machine  
   specific arithmetic implementation.  
 
 
   <ol> 
   
<li>
Compute outward normal vectors at surface points on a target 
      body, where the points correspond to a given planetocentric 
      longitude/latitude grid. Use both ellipsoid and DSK shape 
      models. 
 
<p>  Use the meta-kernel shown below to load the required SPICE
     kernels.

<pre>
KPL/MK 

File: SurfacePointEx2.tm 

This meta-kernel is intended to support operation of SPICE 
example programs. The kernels shown here should not be 
assumed to contain adequate or correct versions of data 
required by SPICE-based user applications. 

In order for an application to use this meta-kernel, the 
kernels referenced here must be present in the user's 
current working directory. 

The names and contents of the kernels referenced 
by this meta-kernel are as follows: 

   File name                        Contents 
   ---------                        -------- 
   pck00010.tpc                     Planet orientation and 
                                    radii 
   phobos512.bds                    DSK based on 
                                    Gaskell ICQ Q=512 
                                    plate model 
\begindata 

   PATH_SYMBOLS    = 'GEN'
   PATH_VALUES     = '/ftp/pub/naif/generic_kernels' 

   KERNELS_TO_LOAD = ( '$GEN/pck/pck00010.tpc', 
                       '$GEN/dsk/phobos/phobos512.bds' ) 
\begintext 

</pre>


<p> Example code begins here.

<pre>
//
// Program SurfacePointEx2
//

import spice.basic.*;
import static spice.basic.AngularUnits.*;
import static java.lang.Math.PI;

//
// This program demonstrates use of the method
// {@link #getNormals}.
//
// Compute outward normal vectors at surface points on a target 
// body, where the points correspond to a given planetocentric 
// longitude/latitude grid. Use both ellipsoid and DSK shape 
// models. 
//
public class SurfacePointEx2
{
   //
   // Load SPICE shared library.
   //
   static{ System.loadLibrary( "JNISpice" ); }


   public static void main( String[] args )

      throws SpiceException
   {
      //
      // Local constants
      //
      final String                      META   = "SurfacePointEx2.tm";

      final int                         MAXN   = 10000;

      //
      // Local variables
      //
      Body                              target = new Body( "PHOBOS" );
      
      Double[]                          coords;

      LimbPoint[][]                     limbPoints;

      RADecCoordinates                  DSKNormalRadCoords;
      RADecCoordinates                  DSKRadCoords;
      RADecCoordinates                  ellNormalRadCoords;
      RADecCoordinates                  ellRadCoords;

      ReferenceFrame                    fixref =
                                           new ReferenceFrame( "IAU_PHOBOS" );

      String                            dsk;
      String[]                          surfSpecs = 
                                        {
                                           "ELLIPSOID",
                                           "DSK/UNPRIORITIZED"
                                        };
                                   
      SurfacePoint[][]                  srfpts;
             
      TDBTime                           et     = new TDBTime( 0.0 );

      Vector3[][]                       normls;

      double                            dlat;
      double                            dlon;
      double                            lat;
      double                            lat0;
      double                            lon;
      double                            lon0;
      double[][]                        lonLatGrid;            
      double                            roll;
      double                            schstp;
      double                            soltol;
      double                            xlat;
      double                            xlon;
      double                            xr;

      int                               i;
      int                               j;
      int                               k;
      int                               n;
      int                               nlat;
      int                               nlon;


      try
      {
         //
         // Load the DSK file via the meta-kernel.
         //
         KernelDatabase.load( META );

         //
         // Set the grid dimensions.
         //
         nlon = 6;
         nlat = 3;

         //
         // Derive evenly spaced grid separations and starting
         // values in the longitude and latitude dimensions.
         // Units are degrees.
         //
         lat0 = 90.0;
         lon0 =  0.0;

         dlat = 180.0 / (nlat + 1);
         dlon = 360.0 /  nlon;

         //
         // Now generate the grid points. We generate
         // points along latitude bands, working from
         // north to south. The latitude range is selected
         // to range from +45 to -45 degrees. Longitude
         // ranges from 0 to 300 degrees. The increment
         // is 45 degrees for latitude and 60 degrees for
         // longitude.
         //

         n          = nlat * nlon;
         lonLatGrid = new double[n][2];
         k          = 0;

         for ( i = 0;  i < nlat;  i++ )
         {
            lat = RPD * ( lat0 - (i+1)*dlat );

            for ( j = 0;  j < nlon;  j++ )
            {
               lon = RPD * ( lon0 + j*dlon );
              
               lonLatGrid[k][0] = lon;
               lonLatGrid[k][1] = lat;

               ++k;
            }
         }

         //
         // Find the surface points corresponding to the grid points.
         //
         // Compute outward normal vectors at the surface points,
         // using both surface representations.
         //
         srfpts = new SurfacePoint[2][];
         normls = new Vector3     [2][];

 
         for ( i = 0;  i < 2;  i++ )
         {
            srfpts[i] = SurfacePoint.create( surfSpecs[i], target,     et, 
                                             fixref,       lonLatGrid      );

            normls[i] = SurfacePoint.getNormals( srfpts[i] );

            //
            // Print out the surface points in latitudinal
            // coordinates and compare the derived lon/lat values
            // to those of the input grid.
            //
         }

         System.out.format( "%n" );

         for ( i = 0;  i < n;  i++ )
         {
            //
            //  Display the ith surface point on the reference ellipsoid.
            //
            //  Use RADecCoordinates rather than LatitudinalCoordinates
            //  to produce non-negative longitudes.
            //
            ellRadCoords = new RADecCoordinates( srfpts[0][i] );
 
            System.out.format ( 

               "%n"                                      +
               "Surface point for grid point %d:%n"      +
               "  Latitudinal Coordinates:%n"            +
               "   Longitude           (deg): %12.6f%n"  +
               "   Latitude            (deg): %12.6f%n"  +
               "   Ellipsoid Radius     (km): %12.6f%n",
               i,
               ellRadCoords.getRightAscension() * DPR,              
               ellRadCoords.getDeclination()    * DPR,
               ellRadCoords.getRadius()                      ); 

            //
            // Compute the RA/Dec coordinates of the ith point
            // on the DSK surface. Display the radius.
            //
            DSKRadCoords = new RADecCoordinates( srfpts[1][i] );

            System.out.format ( 

               "   DSK Radius           (km): %12.6f%n",       
               DSKRadCoords.getRadius()                  ); 

            //
            // Compute the RA/Dec coordinates of the outward normal
            // vector at the ith ellipsoid surface point.
            //
            ellNormalRadCoords = new RADecCoordinates( normls[0][i] );

            System.out.format ( 

               "  Ellipsoid normal vector direction:\n"  +
               "    Longitude (deg):           %12.6f\n" +
               "    Latitude  (deg):           %12.6f\n",
               ellNormalRadCoords.getRightAscension() * DPR,
               ellNormalRadCoords.getDeclination()    * DPR );

            //
            // Compute the RA/Dec coordinates of the outward normal
            // vector at the ith DSK surface point.
            //
            DSKNormalRadCoords = new RADecCoordinates( normls[1][i] );

            System.out.format ( 

               "  DSK normal vector direction:\n"        +
               "    Longitude (deg):           %12.6f\n" +
               "    Latitude  (deg):           %12.6f\n",
               DSKNormalRadCoords.getRightAscension() * DPR,
               DSKNormalRadCoords.getDeclination()    * DPR );
         }

         System.out.format ( "%n" );

      } // End of try block

      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }

   } // End of main method 
   
}


</pre>

<p> When this program was executed on a PC/Linux/gcc/64-bit/java 1.5
platform, the output for the first 3 points 
(the rest of the output is not shown due to its large volume) was: 

<pre>


Surface point for grid point 0:
  Latitudinal Coordinates:
   Longitude           (deg):     0.000000
   Latitude            (deg):    45.000000
   Ellipsoid Radius     (km):    10.542977
   DSK Radius           (km):    10.156402
  Ellipsoid normal vector direction:
    Longitude (deg):               0.000000
    Latitude  (deg):              63.895146
  DSK normal vector direction:
    Longitude (deg):             341.337568
    Latitude  (deg):              62.610726

Surface point for grid point 1:
  Latitudinal Coordinates:
   Longitude           (deg):    60.000000
   Latitude            (deg):    45.000000
   Ellipsoid Radius     (km):    10.172847
   DSK Radius           (km):    10.131412
  Ellipsoid normal vector direction:
    Longitude (deg):              66.059787
    Latitude  (deg):              58.877649
  DSK normal vector direction:
    Longitude (deg):              48.859884
    Latitude  (deg):              56.924717

Surface point for grid point 2:
  Latitudinal Coordinates:
   Longitude           (deg):   120.000000
   Latitude            (deg):    45.000000
   Ellipsoid Radius     (km):    10.172847
   DSK Radius           (km):    10.423766
  Ellipsoid normal vector direction:
    Longitude (deg):             113.940213
    Latitude  (deg):              58.877649
  DSK normal vector direction:
    Longitude (deg):             118.553200
    Latitude  (deg):              55.906774


</pre>
</li>


   </ol>
   */
   public static Vector3[] getNormals( SurfacePoint[]  srfpts )

      throws SpiceException
   {
      SpiceErrorException               exc;

      Vector3[]                         retArray;

      int                               n = srfpts.length;


      if ( n < 1 )
      {
         String errmsg 

            = String.format ( "Number of ray input surface points = %d; "  +
                              "count must be > 0.",
                              n                                            );

         exc = SpiceErrorException.create( "SurfacePoint.getNormals",
                                           "SPICE(INVALIDCOUNT)",
                                           errmsg                    );
         throw( exc );
      }


      //
      // Compute normals. Check for matching attributes of input
      // array elements.
      //
      retArray = getNormals( srfpts, true );

      return( retArray );  
   }


   /**
   Compute the unit length outward normal vectors corresponding
   to an array of SurfacePoint instances, without checking
   for consistency of the attributes of the input array elements. 
 
   <p> All elements of the input SurfacePoint array must have
   matching attributes, other than their "point" members.

   <p> For efficiency, this method doesn't check the attributes of the input 
   array elements. The users' application is responsible for
   ensuring consistency of the input array elements.

   <p> Also see the methods {@link #getNormal}, 
   {@link #getNormals}.
   */
   public static Vector3[] getNormalsUnchecked( SurfacePoint[]  srfpts )

      throws SpiceException
   {
      Vector3[]                         retArray;

      //
      // Compute normals. DO NOT check for matching attributes of input
      // array elements.
      //
      retArray = getNormals( srfpts, false );

      return( retArray );  
   }



   /**
   Get the body from a SurfacePoint. This method returns
   a deep copy.
   */
   public Body getBody()
 
      throws SpiceException
   {
      return( new Body(body) );
   }

   /**
   Get the reference frame from a SurfacePoint. This method returns
   a deep copy.
   */
   public ReferenceFrame getReferenceFrame()

      throws SpiceException
   {
      return( new ReferenceFrame(fixref) );
   }

   /**
   Get the time from a SurfacePoint. This method returns
   a deep copy.
   */
   public TDBTime getTDBTime()

      throws SpiceException
   {
      return( new TDBTime(et) );
   }

   /**
   Get the surface specification from a SurfacePoint, represented
   as a String. This method returns a deep copy.
   */
   public String getSurfaceSpecificationString()

      throws SpiceException
   {
      return( new String(surfspec) );
   }



   //
   // Private methods
   //
   

   //
   // Compute unit outward surface normals corresponding to an array
   // of SurfacePoints. Optionally check consistency of attributes of
   // the input array elements.
   //
   private static Vector3[] getNormals( SurfacePoint[]  srfpts,
                                        boolean         check   )

      throws SpiceException
   {
      SpiceErrorException               exc;

      Vector3[]                         retArray;

      double[][]                        points;
      double[][]                        normals;

      int                               i;
      int                               n = srfpts.length;


      if ( n < 1 )
      {
         String errmsg 

            = String.format ( "Number of ray input surface points = %d; "  +
                              "count must be > 0.",
                              n                                              );

         exc = SpiceErrorException.create( "SurfacePoint.getNormals",
                                           "SPICE(INVALIDCOUNT)",
                                           errmsg                    );
         throw( exc );
      }


      if ( check )
      {
         //
         // Verify that all `srfpts' elements have attributes matching
         // those of the first element. Only the "point" members may
         // vary.
         //
         checkAttributes ( "SurfacePoint.getNormals(SurfacePoint[])", srfpts );
      }

      points = new double[n][];

      for ( i = 0;  i < n;  i++ )
      {
         points[i] = srfpts[i].toArray();
      }

      normals = CSPICE.srfnrm( srfpts[0].surfspec, 
                               srfpts[0].body.getName(), 
                               srfpts[0].et.getTDBSeconds(), 
                               srfpts[0].fixref.getName(), 
                               n,
                               points            );
 
      retArray = new Vector3[ n ];

      for ( i = 0;  i < n;  i++ )
      {
         retArray[i] = new Vector3( normals[i] );
      }

      return( retArray );  
   }








   //
   // Check SurfacePoint array for matching attributes.
   // Only the "point" elements may differ. 
   // 
   // Throw an exception if the attributes don't match.
   //
   private static void checkAttributes ( String          caller,
                                         SurfacePoint[]  srfpts )

      throws SpiceException, SpiceErrorException
   {
      //
      // Local constants
      //
      final int                         NATT = 4;

      //
      // Local variables
      //
      String[]                          attNames =
                                        {
                                           "surface specification",
                                           "body",
                                           "time",
                                           "reference frame"
                                        };

      boolean                           match;
      boolean[]                         matches; 

      int                               i;
      int                               j;
      int                               n = srfpts.length;

      //
      // Check attributes of the succeeding elements against
      // those of the first. Only the "point" elements may
      // differ. 
      //
      matches = new boolean[ NATT ];
      match   = true;

      for ( i = 1;  i < n;  i++ )
      {
         matches[0] = srfpts[i].surfspec.equals( srfpts[0].surfspec );
         matches[1] = srfpts[i].body.equals    ( srfpts[0].body     );
         matches[2] = srfpts[i].et.equals      ( srfpts[0].et       );      
         matches[3] = srfpts[i].fixref.equals  ( srfpts[0].fixref   );      

         for ( j = 0;  j < NATT;  j++ )
         {
            match = match && matches[j];
         }
         //
         // Throw an exception if we don't have a match.
         //        
         if ( !match )
         {
            String errMsg = 

               String.format( "SurfacePoint array match failure at "    +
                              "index %d: element [0] has attributes %n" +
                              "   surface specification: %s%n "         +
                              "   body:                  %s%n "         +
                              "   TDB time:              %s%n "         +
                              "   reference frame:       %s%n "         +
                              "while element [%d] has attributes %n"    +
                              "   surface specification: %s%n "         +
                              "   body:                  %s%n "         +
                              "   TDB time:              %s%n "         +
                              "   reference frame:       %s%n ",
                              i,
                              srfpts[0].surfspec,
                              srfpts[0].body.getName(),
                              srfpts[0].et.toString(),
                              srfpts[0].fixref.getName(),
                              i,
                              srfpts[i].surfspec,
                              srfpts[i].body.getName(),
                              srfpts[i].et.toString(),
                              srfpts[i].fixref.getName()                );
                                                                       
               for ( j = 0;  j < NATT;  j++ )
               {
                  if ( !matches[j] )
                  {
                     errMsg = 

                        errMsg + 
                        String.format( " Attribute %s doesn't match.%n",
                                       attNames[j]                       );
                  }
               }



            SpiceErrorException exc 

               = SpiceErrorException.create ( caller, 
                                              "SPICE(BADATTRIBUTES)",
                                              errMsg                  );
            throw( exc );

         } // End of match failure case.

      } // End of loop over `srfpts' array members.
   }
 

}

