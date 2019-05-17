
package spice.basic;


/**
Class OccultationState provides methods to classify
the occultation of one target object by another, as
seen from a specified viewing location.
 
<p>The principal computation method of this class is 
{@link #getOccultationState}. See the detailed documentation
of that method for a code example.

<p>To search for times when a given type of occultation
occurs, see class {@link GFOccultationSearch}.

<h2>Files</h2>

<p>Appropriate SPICE kernels must be loaded by the calling program
   before methods of this class are called.

<p>The following data are required:

<ul>
<li>
        SPK data: the calling application must load ephemeris data
        for the targets and observer. If aberration
        corrections are used, the states of the target bodies and of
        the observer relative to the solar system barycenter must be
        calculable from the available ephemeris data. Typically
        ephemeris data are made available by loading one or more SPK
        files via {@link KernelDatabase#load}.
</li>

<li>
        Target body orientation data: these may be provided in a text
        or binary PCK file. In some cases, target body orientation
        may be provided by one more more CK files. In either case,
        data are made available by loading the files via KernelDatabase.load.

        <p>Orientation data are not required for point targets.
</li>
<li>    Shape data for the target bodies:
        <pre>
   PCK data:
 
      If a target body shape is modeled as an ellipsoid,
      triaxial radii for that target body must be loaded into
      the kernel pool. Typically this is done by loading a
      text PCK file via KernelDatabase.load.
 
   DSK data:
 
      If a target shape is modeled by DSK data, DSK files
      containing topographic data for that target body must be
      loaded. If a surface list is specified, data for at
      least one of the listed surfaces must be loaded.
</pre>
</ul>
<p>    The following data may be required:
<ul>
<li>       Frame data: if a frame definition is required to convert the
           observer or target states to the body-fixed frame of the
           target, that definition must be available in the kernel
           pool. Typically the definition is supplied by loading a
           frame kernel via KernelDatabase.load.
</li>
<li>       Surface name-ID associations: if surface names are included
           in the shape specification of either target body, the 
           association of these names with their
           corresponding surface ID codes must be established by
           assignments of the kernel variables
<pre>
   NAIF_SURFACE_NAME
   NAIF_SURFACE_CODE
   NAIF_SURFACE_BODY
</pre>
<p>        Normally these associations are made by loading a text
           kernel containing the necessary assignments. An example
           of such a set of assignments is
<pre>
   NAIF_SURFACE_NAME += 'Mars MEGDR 128 PIXEL/DEG'
   NAIF_SURFACE_CODE += 1
   NAIF_SURFACE_BODY += 499
</pre>
<li>
           SCLK data: if a target body's orientation is provided by
           CK files, an associated SCLK kernel must be loaded.
</li>
</ul>
 
<p>
   Kernel data are normally loaded once per program run, NOT every
   time a method of this class is called.



<h2>Particulars</h2>


<h3>Using DSK data</h3>

 
<p><b>DSK loading and unloading</b>
 
<p>DSK files providing data used by this routine are loaded by 
      calling {@link KernelDatabase#load} and can be unloaded by 
      calling {@link KernelDatabase#unload} or
      {@link KernelDatabase#clear}. See the documentation of 
      KernelDatabase.load for limits on numbers 
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
 
 
<p><b>DSK data priority</b>

 
<p>   A DSK coverage overlap occurs when two segments in loaded DSK 
      files cover part or all of the same domain---for example, a 
      given longitude-latitude rectangle---and when the time 
      intervals of the segments overlap as well. 
 
<p>   When DSK data selection is prioritized, in case of a coverage 
      overlap, if the two competing segments are in different DSK 
      files, the segment in the DSK file loaded last takes 
      precedence. If the two segments are in the same file, the 
      segment located closer to the end of the file takes 
      precedence. 
 
<p>   When DSK data selection is unprioritized, data from competing 
      segments are combined. For example, if two competing segments 
      both represent a surface as a set of triangular plates, the 
      union of those sets of plates is considered to represent the 
      surface.  
 
<p>   Currently only unprioritized data selection is supported. 
      Because prioritized data selection may be the default behavior 
      in a later version of the routine, the UNPRIORITIZED keyword is 
      required in the `shape1' and `shape2' arguments of 
      method getOccultationState.
 
       
<p> <b>Syntax of the shape input arguments for the DSK case</b>

 
<p>   The keywords and surface list in the target shape arguments
      `shape1' and `shape2' of getOccultationState 
      are called "clauses." The clauses may
      appear in any order, for example
<pre> 
   "DSK/&#60surface list&#62/UNPRIORITIZED"
   "DSK/UNPRIORITIZED/&#60surface list&#62"
   "UNPRIORITIZED/&#60surface list&#62/DSK"
</pre> 
      The simplest form of a "shape" argument specifying use of 
      DSK data is one that lacks a surface list, for example: 
<pre> 
   "DSK/UNPRIORITIZED" 
</pre>
<p>
      For applications in which all loaded DSK data for a target 
      body are for a single surface, and there are no competing 
      segments, the above string suffices. This is expected to be 
      the usual case. 

<p> 
      When, for the specified target body, there are loaded DSK 
      files providing data for multiple surfaces for that body, the 
      surfaces to be used by this routine for a given call must be 
      specified in a surface list, unless data from all of the 
      surfaces are to be used together. 
 
<p>
      A surface list consists of the string 
<pre>
   "SURFACES = "
</pre>
<p> 
      followed by a comma-separated list of one or more surface 
      identifiers. The identifiers may be names or integer codes in 
      string format. For example, suppose we have the surface 
      names and corresponding ID codes shown below: 
 
<pre>
   Surface Name                              ID code 
   ------------                              ------- 
   "Mars MEGDR 128 PIXEL/DEG"                1 
   "Mars MEGDR 64 PIXEL/DEG"                 2 
   "Mars_MRO_HIRISE"                         3 
</pre>

<p>
      If data for all of the above surfaces are loaded, then 
      data for surface 1 can be specified by either 
<pre>
   "SURFACES = 1" 
</pre>
or 
<pre> 
   "SURFACES = \"Mars MEGDR 128 PIXEL/DEG\"" 
</pre> 
      Escaped double quotes are used to delimit the surface name because 
      it contains blank characters.  
      
<p>    
      To use data for surfaces 2 and 3 together, any 
      of the following surface lists could be used: 
<pre>
   "SURFACES = 2, 3" 
 
   "SURFACES = \"Mars MEGDR  64 PIXEL/DEG\", 3" 
 
   "SURFACES = 2, Mars_MRO_HIRISE" 
 
   "SURFACES = \"Mars MEGDR 64 PIXEL/DEG\", Mars_MRO_HIRISE" 
</pre>   
      An example of a shape argument that could be constructed 
      using one of the surface lists above is 
<pre>
   "DSK/UNPRIORITIZED/SURFACES = \"Mars MEGDR 64 PIXEL/DEG\", 3" 
</pre>




<h3>Version 1.0.0 29-DEC-2016 (NJB)</h3>
*/
public class OccultationState extends Object
{


   //
   // Methods
   // 
   

   /**
   Determine the occultation condition (not occulted, partially
   occulted, etc.) of one target relative to another target as seen by
   an observer at a given time.

   <p>
   The surfaces of the target bodies may be represented by triaxial
   ellipsoids or by topographic data provided by DSK files.

   <h3>Code Examples</h3>

<p> 
   The numerical results shown for these examples may differ across 
   platforms. The results depend on the SPICE kernels used as 
   input, the compiler and supporting libraries, and the machine  
   specific arithmetic implementation.  

<ol>

<li>  Find whether MRO is occulted by Mars as seen by
      the DSS-13 ground station at a few specific
      times.

<p>  Use the meta-kernel shown below to load the required SPICE
     kernels.

<pre>
KPL/MK

File: OccultationStateEx1.tm

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
   de410.bsp                        Planetary ephemeris
   mar063.bsp                       Mars satellite ephemeris
   pck00010.tpc                     Planet orientation and
                                    radii
   naif0012.tls                     Leapseconds
   mro_psp35.bsp                    MRO ephemeris
   megr90n000cb_plate.bds           Plate model based on
                                    MEGDR DEM, resolution
                                    4 pixels/degree.

\begindata

   PATH_SYMBOLS    = ( 'MRO', 'GEN' )

   PATH_VALUES     = (
                       '/ftp/pub/naif/pds/data+'
                       '/mro-m-spice-6-v1.0/+'
                       'mrosp_1000/data/spk',
                       '/ftp/pub/naif/generic_kernels'
                     )

   KERNELS_TO_LOAD = ( '$MRO/de410.bsp',
                       '$MRO/mar063.bsp',
                       '$MRO/mro_psp34.bsp',
                       '$GEN/spk/stations/+'
                       'earthstns_itrf93_050714.bsp',
                       '$GEN/pck/earth_latest_high_prec.bpc',
                       'pck00010.tpc',
                       'naif0012.tls',
                       'megr90n000cb_plate.bds'
                     )
\begintext

</pre>

<p> Example code begins here.
<pre>

   import spice.basic.*;
   import static spice.basic.OccultationState.*;


   class OccultationStateEx1
   {
      //
      // Load the JNISpice shared object library
      // at initialization time.
      //
      static { System.loadLibrary( "JNISpice" ); }

      public static void main ( String[] args )
      {
         try 
         {
            //
            // Constants
            //
            final String META    = "OccultationStateEx1.tm";

            final String OUTFMT  =  "%s %s %s %s wrt %s%n";

            final String TIMFMT  =
               "YYYY-MON-DD HR:MN ::UTC-8";
            //
            // Declare and assign values to variables required to
            // specify the geometric condition to search for.
            //

            AberrationCorrection
               abcorr = new AberrationCorrection( "CN" );

            Body targ2             = new Body ( "Mars"   );
            Body targ1             = new Body ( "MRO"    );
            Body obsrvr            = new Body ( "DSS-13" );
  
            OccultationCode occ;            

            // For point targets, the frame is not used. We
            // can provide the name of an arbitrary built-in
            // frame.
            //
            ReferenceFrame frame1  = new ReferenceFrame( "J2000"    );
            ReferenceFrame frame2  = new ReferenceFrame( "IAU_MARS" );

            String[] outputStr     = { "totally occulted by",
                                       "transited by",
                                       "partially occulted by",
                                       "not occulted by"        };

            String   shape1        = GF.PTSHAP;
            String   shape2;
            String[] shapes        = {GF.EDSHAP, "DSK/UNPRIORITIZED"}; 
            String   timstr;

            TDBDuration dt         = new TDBDuration( 1000.0 );

            TDBTime et;         
            TDBTime etStart;
            TDBTime etStop;

            int     i;

            //
            // Load kernels.
            //
            KernelDatabase.load ( META );

            //
            // Convert the time bounds of our computation interval
            // to TDB.
            //
            etStart = new TDBTime ( "2015-FEB-28 1:15:00 UTC" );
            etStop  = new TDBTime ( "2015-FEB-28 2:50:00 UTC" );

            //
            // Loop over the Mars shapes.
            //
            for ( i = 0;  i < 2;  i++ )
            {
               shape2 = shapes[i];

               System.out.format ( "%nMars shape: %s%n%n", shape2 );

               //
               // Step through the interval, computing the occultation
               // state at intervals of `dt' TDB seconds.
               //
                      
               et = etStart;

               while(  et.getTDBSeconds() < etStop.getTDBSeconds() )
               {
                  //
                  // Calculate the type of occultation that
                  // corresponds to time `et'.
        
                  occ = 

                     OccultationState.
                     getOccultationState( targ1,  shape1, frame1,
                                          targ2,  shape2, frame2,
                                          abcorr, obsrvr, et     );
                  //
                  // Display the results.
                  //
                  timstr = et.toString( TIMFMT );

                  switch( occ )
                  {
                     case TOTAL1:

                        System.out.format( OUTFMT,          timstr, 
                                           targ1.getName(), outputStr[0], 
                                           targ2.getName(), obsrvr.getName() );
                        break;

                     case ANNLR1:

                        System.out.format( OUTFMT,          timstr, 
                                           targ1.getName(), outputStr[1], 
                                           targ2.getName(), obsrvr.getName() );
                        break;

                     case PARTL1:

                        System.out.format( OUTFMT,          timstr, 
                                           targ1.getName(), outputStr[2], 
                                           targ2.getName(), obsrvr.getName() );
                        break;

                     case TOTAL2:
   
                        System.out.format( OUTFMT,          timstr, 
                                           targ2.getName(), outputStr[0], 
                                           targ1.getName(), obsrvr.getName() );
                        break;

                     case ANNLR2:

                        System.out.format( OUTFMT,          timstr, 
                                           targ2.getName(), outputStr[1], 
                                           targ1.getName(), obsrvr.getName() );
                        break;

                     case PARTL2:

                        System.out.format( OUTFMT,          timstr, 
                                           targ2.getName(), outputStr[2], 
                                           targ1.getName(), obsrvr.getName() );
                        break;

                     case NOOCC:

                        System.out.format( OUTFMT,          timstr, 
                                           targ1.getName(), outputStr[3], 
                                           targ2.getName(), obsrvr.getName() );
                        break;
                  }

                  et = et.add( dt );
               }
               //
               // End of time loop.
               //
            }
            //
            // End of shape loop.
            //
         }
         catch ( SpiceException exc ) {
            exc.printStackTrace();
         }

         System.out.format( "%n" );
      }
   }

</pre>

<p> When this program was executed on a PC/Linux/gcc/64-bit/java 1.5
platform, the output was:

<pre>

Mars shape: ELLIPSOID

2015-FEB-27 17:15 Mars transited by MRO wrt DSS-13
2015-FEB-27 17:31 MRO not occulted by Mars wrt DSS-13
2015-FEB-27 17:48 MRO totally occulted by Mars wrt DSS-13
2015-FEB-27 18:04 MRO totally occulted by Mars wrt DSS-13
2015-FEB-27 18:21 MRO not occulted by Mars wrt DSS-13
2015-FEB-27 18:38 Mars transited by MRO wrt DSS-13

Mars shape: DSK/UNPRIORITIZED

2015-FEB-27 17:15 Mars transited by MRO wrt DSS-13
2015-FEB-27 17:31 MRO not occulted by Mars wrt DSS-13
2015-FEB-27 17:48 MRO totally occulted by Mars wrt DSS-13
2015-FEB-27 18:04 MRO totally occulted by Mars wrt DSS-13
2015-FEB-27 18:21 MRO not occulted by Mars wrt DSS-13
2015-FEB-27 18:38 Mars transited by MRO wrt DSS-13

</pre>


</li>
</ol>



   */
   public static 

      OccultationCode getOccultationState ( Body                 targ1,
                                            String               shape1,
                                            ReferenceFrame       frame1,
                                            Body                 targ2,
                                            String               shape2,
                                            ReferenceFrame       frame2,
                                            AberrationCorrection abcorr, 
                                            Body                 obsrvr,
                                            Time                 t       )

      throws SpiceException
   {
      int                               intCode;


      intCode = CSPICE.occult ( targ1.getName(),   
                                shape1, 
                                frame1.getName(),
                                targ2.getName(),   
                                shape2,
                                frame2.getName(),
                                abcorr.toString(), 
                                obsrvr.getName(),
                                t.getTDBSeconds()  );

      return( OccultationCode.mapIntCode(intCode) );
   }
}
