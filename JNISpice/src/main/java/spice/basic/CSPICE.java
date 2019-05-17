

package spice.basic;

import spice.basic.*;


/**
Class CSPICE provides a native API enabling higher-level
JNISpice methods to call selected CSPICE and DSKLIB_C routines.

<p> In many cases the functionality provided here is also provided
in an object-oriented fashion by a higher-level API. Users should
consult in particular the documentation for the package {@link spice.basic}
before resorting to direct calls to methods in this package.

<p> Unlike the rest of the rest of the JNISpice system, the
methods of this class have interfaces that rely, with few
exceptions, on primitive scalars and arrays of primitive types.
The method interfaces tend to parallel those of the corresponding
native functions, but differences exist where slavish adherence
to parallel structure would result in (even more) unwieldy
calling sequences. For example, coordinate conversion routines
typically return their outputs as a double precision array.

<p> Output arguments pose a challenge:  many CSPICE functions
return multiple output arguments, yet Java has no mechanism
expressly meant to handle output arguments. In Java, compound
data to be returned from a method are typically packaged as
an object. Output arguments may be clumsily simulated by
use of arrays; this is the work-around employed by JNISpice.
If an underlying C function returns multiple arguments, then
(with few exceptions), the corresponding method provides
"output" arguments in the form of arrays whose contents are
modified by the native C function. Scalar output arguments
that would be passed by reference in C are passed as arrays
of length 1 here.

<p> Concerning error handling: this class uses static
initialization code to set the CSPICE error handling system
to RETURN mode and the CSPICE error output device to NULL.
When a SPICE error is signaled by CSPICE, the calling JNISpice
wrapper will capture error messages, reset the CSPICE
error status, then throw a {@link spice.basic.SpiceErrorException}
reflecting the CSPICE error. After the calling application has
caught and handled the exception, the CSPICE system normally is ready to
resume operation.

<p> All methods in this class are static and synchronized.


<h3> Version 4.0.0 25-JAN-2017 (NJB)</h3>
<pre>
    Changed argument list of 
       
       CSPICE.dskw02

    Added methods:

       CSPICE.dasac
       CSPICE.dasdc
       CSPICE.dashfn
       CSPICE.dasopw
       CSPICE.dasrfr
       CSPICE.dlabbs
       CSPICE.dlafps
       CSPICE.dskgtl
       CSPICE.dskmi2
       CSPICE.dskobj
       CSPICE.dskrb2
       CSPICE.dsksrf
       CSPICE.dskstl
       CSPICE.dskxsi
       CSPICE.dskxv
       CSPICE.illumf
       CSPICE.inedpl
       CSPICE.latsrf
       CSPICE.limbpt
       CSPICE.occult
       CSPICE.oscltx
       CSPICE.pckcls
       CSPICE.pckfrm
       CSPICE.pckobj
       CSPICE.pckopn
       CSPICE.pckw02
       CSPICE.pltar
       CSPICE.pltexp
       CSPICE.pltnp
       CSPICE.pltnrm
       CSPICE.pltvol
       CSPICE.pxfrm2
       CSPICE.spkcpo
       CSPICE.spkcpt
       CSPICE.spkcvo
       CSPICE.spkcvt
       CSPICE.srfc2s
       CSPICE.srfcss
       CSPICE.srfnrm
       CSPICE.srfs2c
       CSPICE.srfscc
       CSPICE.termpt
       CSPICE.tparse

</pre>
<h3> Version 3.0.0 16-JUN-2014 (NJB)</h3>
<pre>
    Changed argument list of method llgridPl02. The input
    grid is now 2-dimensional.
</pre>
<p> Last update was 11-MAR-2014 (NJB)(EDW)
<pre>
    Added methods:
    
       CSPICE.ccifrm
       CSPICE.gfilum
       CSPICE.gfpa
       CSPICE.gfrr
       CSPICE.gfstol
</pre>
   <p>27-FEB-2014 (NJB)
<pre>
    Added method CSPICE.gfilum.
</pre>
<h3> Version 2.0.0 19-AUG-2013 (NJB)</h3>
<pre>
    Added method CSPICE.getfat.
</pre>
<h3> Version 1.0.0 24-DEC-2009 (NJB)</h3>

*/

public class CSPICE extends Object
{

   //
   // At module start-up, initialize SPICE error handling:
   //
   //    - Use RETURN mode so exceptions may be thrown
   //      in response to SPICE errors.
   //
   //    - Suppress console output of error messages.
   //      Instead, short, long, and SPICE traceback error
   //      messages are combined into a single message
   //      associated with the exception that is thrown.
   //
   static
   {
      try
      {
         erract ( "SET", "RETURN" );
         errdev ( "SET", "NULL"   );
      }
      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }
   }


   /**
   Compute a rotation matrix from a rotation axis and angle.
   */
   public native synchronized static double[][] axisar ( double[]  axis,
                                                         double    angle )
      throws SpiceErrorException;



   /**
   Return the Julian Date corresponding to Besselian Date 1900.0.
   */
   public native synchronized static double b1900 ();


   /**
   Return the Julian Date corresponding to Besselian Date 1950.0.
   */
   public native synchronized static double b1950 ();


   /**
   Throw an exception if a given kernel variable does not have
   specified attributes.
   */
   public native synchronized static boolean badkpv ( String   caller,
                                                      String   name,
                                                      String   comp,
                                                      int      size,
                                                      int      divby,
                                                      String   type  )
      throws SpiceErrorException;


   /**
   Translate the SPICE integer code of a body into a common name
   for that body.
   */
   public native synchronized static String bodc2n ( int code )

      throws SpiceErrorException, NameNotFoundException;


   /**
   Translate the SPICE integer code of a body into a common name
   for that body, or a string representation of the code if no
   name is associated with the code.
   */
   public native synchronized static String bodc2s ( int code )

      throws SpiceErrorException;


   /**
   Determine whether values exist for some item for any body
   in the kernel pool.
   */
   public native synchronized static boolean bodfnd ( int    code,
                                                      String item )

      throws SpiceErrorException;


   /**
   Translate the name of a body into the SPICE integer ID code for
   that body.
   */
   public native synchronized static int bodn2c ( String name )

      throws SpiceErrorException, IDCodeNotFoundException;


   /**
   Translate the name of a body into the SPICE integer ID code for
   that body; translate a string representation of an integer to
   an integer.
   */
   public native synchronized static int bods2c ( String name )

      throws SpiceErrorException, IDCodeNotFoundException;



   /**
   Return the values of some item for any body in the
   kernel pool.
   */
   public native synchronized static double[] bodvcd ( int     body,
                                                       String  item  )
      throws SpiceErrorException;



   /**
   Map a frame class and frame class ID to a reference frame ID code,
   name, and center.
   */
   public native synchronized static void ccifrm ( int       frclss,
                                                   int       clssid,
                                                   int[]     frcode,
                                                   String[]  frname,
                                                   int[]     center,
                                                   boolean[] found   )
      throws SpiceErrorException;



   /**
   Create a SPICE ellipse from a center and generating vectors.
   */
   public native synchronized static double[] cgv2el ( double[]   center,
                                                       double[]   gv1,
                                                       double[]   gv2    )
      throws SpiceErrorException;



   /**
   Close a CK file.
   */
   public native synchronized static void ckcls ( int   handle )

      throws SpiceErrorException;



   /**
   Return a coverage window for a specified instrument and CK file.
   Add this coverage to that contained in an input window.
   */
   public native synchronized static double[]  ckcov ( String    ck,
                                                       int       idcode,
                                                       boolean   needav,
                                                       String    level,
                                                       double    tol,
                                                       String    timsys,
                                                       int       size,
                                                       double[]  cover )
      throws SpiceErrorException;



   /**
   Get instrument pointing for a specified spacecraft clock time.
   */
   public native synchronized static void ckgp ( int           inst,
                                                 double        sclkdp,
                                                 double        tol,
                                                 String        ref,
                                                 double [][]   cmat,
                                                 double []     clkout,
                                                 boolean[]     found  )
      throws SpiceErrorException;


   /**
   Get instrument pointing and angular velocity for a specified
   spacecraft clock time.
   */
   public native synchronized static void ckgpav ( int           inst,
                                                   double        sclkdp,
                                                   double        tol,
                                                   String        ref,
                                                   double [][]   cmat,
                                                   double []     av,
                                                   double []     clkout,
                                                   boolean[]     found  )
      throws SpiceErrorException;



   /**
   Unload a CK from the CKBSR system.
   */
   public native synchronized static void ckupf ( int       handle )

      throws SpiceErrorException;



   /**
   Return the SCLK or SPK ID associated with an instrument ID code.
   */
   public native synchronized static int ckmeta ( int       CKID,
                                                  String    meta )
      throws SpiceErrorException;


   /**
   Return an ordered array of unique ID codes of instruments for which a
   specified CK file contains data.
   */
   public native synchronized static int[] ckobj ( String    file,
                                                   int       size,
                                                   int[]     ids  )
      throws SpiceErrorException;


   /**
   Open a new CK file.
   */
   public native synchronized static int ckopn ( String     fname,
                                                 String     ifname,
                                                 int        ncomch )
      throws SpiceErrorException;



   /**
   Write a type 1 segment to a CK file.
   */
   public native synchronized static void ckw01 ( int         handle,
                                                  double      begtim,
                                                  double      endtim,
                                                  int         inst,
                                                  String      ref,
                                                  boolean     avflag,
                                                  String      segid,
                                                  int         nrec,
                                                  double[]    sclkdp,
                                                  double[]    quats,
                                                  double[]    avvs    )
      throws SpiceErrorException;



   /**
   Write a type 2 segment to a CK file.
   */
   public native synchronized static void ckw02 ( int         handle,
                                                  double      begtim,
                                                  double      endtim,
                                                  int         inst,
                                                  String      ref,
                                                  String      segid,
                                                  int         nrec,
                                                  double[]    start,
                                                  double[]    stop,
                                                  double[]    quats,
                                                  double[]    avvs,
                                                  double[]    rates  )
      throws SpiceErrorException;


   /**
   Write a type 3 segment to a CK file.
   */
   public native synchronized static void ckw03 ( int         handle,
                                                  double      begtim,
                                                  double      endtim,
                                                  int         inst,
                                                  String      ref,
                                                  boolean     avflag,
                                                  String      segid,
                                                  int         nrec,
                                                  double[]    sclkdp,
                                                  double[]    quats,
                                                  double[]    avvs,
                                                  int         nints,
                                                  double[]    starts  )
      throws SpiceErrorException;



   /**
   Return the speed of light.
   */
   public native synchronized static double clight ();



   /**
   Clear the kernel pool.
   */
   public native synchronized static void clpool ()

      throws SpiceErrorException;


   /**
   Convert conic elements to a state at a given epoch.
   */
   public native synchronized static double[] conics ( double[]   elts,
                                                       double     et   )
      throws SpiceErrorException;



   /**
   Convert a measurement from one physical unit to another.
   */
   public native synchronized static double convrt ( double       x,
                                                     String       in,
                                                     String       out )
      throws SpiceErrorException;


   /**
   Determine whether or not any of the variables that are to be watched
   and have a specified agent on their distribution list have been
   updated.
   */
   public native synchronized static boolean cvpool ( String agent )

      throws SpiceErrorException;


   /**
   Convert from cylindrical coordinates to latitudinal coordinates.
   */
   public native synchronized static double[] cyllat ( double r,
                                                       double lon,
                                                       double z   )
      throws SpiceErrorException;


   /**
   Convert from cylindrical coordinates to rectangular coordinates.
   */
   public native synchronized static double[] cylrec ( double r,
                                                       double lon,
                                                       double z   )
      throws SpiceErrorException;


   /**
   Convert from cylindrical coordinates to spherical coordinates.
   */
   public native synchronized static double[] cylsph ( double r,
                                                       double lon,
                                                       double z   )
      throws SpiceErrorException;


   /**
   Add comments from a buffer to a DAF.
   */
   public native synchronized static void dafac ( int        handle,
                                                  String[]   buffer )
      throws SpiceErrorException;


   /**
   Begin a backward search for arrays in a DAF.
   */
   public native synchronized static void dafbbs ( int handle )

      throws SpiceErrorException;


   /**
   Begin a forward search for arrays in a DAF.
   */
   public native synchronized static void dafbfs ( int handle )

      throws SpiceErrorException;


   /**
   Close the DAF associated with a given handle.
   */
   public native synchronized static void dafcls ( int handle )

      throws SpiceErrorException;


   /**
   Select a DAF that already has a search in progress as the
   one to continue searching.
   */
   public native synchronized static void dafcs ( int handle )

      throws SpiceErrorException;


   /**
   Delete comments from a DAF.
   */
   public native synchronized static void dafdc ( int handle )

      throws SpiceErrorException;


   /**
   Extract comments from a DAF into a buffer.
   */
   public native synchronized static void dafec ( int         handle,
                                                  int         bufsiz,
                                                  int         lenout,
                                                  int[]       n,
                                                  String[]    buffer,
                                                  boolean[]   done   )
      throws SpiceErrorException;


   /**
   Find the next (forward) array in the current DAF.
   */
   public native synchronized static boolean daffna ()

      throws SpiceErrorException;


   /**
   Find the previous (backward) array in the current DAF.
   */
   public native synchronized static boolean daffpa ()

      throws SpiceErrorException;


   /**
   Return double precision data from the specified address range.
   */
   public native synchronized static double[] dafgda ( int    handle,
                                                       int    begin,
                                                       int    end    )
      throws SpiceErrorException;


   /**
   Return (get) the name for the current array in the current DAF.
   */
   public native synchronized static String dafgn ()

      throws SpiceErrorException;


   /**
   Return (get) the summary for the current array in the current DAF.
   */
   public native synchronized static double[] dafgs ( int    size )

      throws SpiceErrorException;


   /**
   Return the specified summary record.
   */
   public native synchronized static double[] dafgsr ( int    handle,
                                                       int    recno,
                                                       int    begin,
                                                       int    end    )

      throws SpiceErrorException, DAFRecordNotFoundException;


   /**
   Open a DAF for subsequent read requests.
   */
   public native synchronized static int dafopr ( String   fname )

      throws SpiceErrorException;


   /**
   Open a DAF for writing.
   */
   public native synchronized static int dafopw ( String   fname )

      throws SpiceErrorException;


   /**
   Read a DAF character record.
   */
   public native synchronized static String dafrcr ( int  handle,
                                                     int  recno   )

      throws SpiceErrorException;


   /**
   Obtain a file record object from a DAF.
   */
   public native synchronized static void dafrfr ( int      handle,
                                                   int[]    nd,
                                                   int[]    ni,
                                                   String[] ifname,
                                                   int[]    fward,
                                                   int[]    bward,
                                                   int[]    free   )
      throws SpiceErrorException;


   /**
   Unpack a DAF summary.
   */
   public native synchronized static void dafus ( double[]   sum,
                                                  int        nd,
                                                  int        ni,
                                                  double[]   dc,
                                                  int[]      ic   )
      throws SpiceErrorException;



   /**
   Close the DAS file associated with a given handle.
   */
   public native synchronized static void dascls ( int handle )

      throws SpiceErrorException;



   /**
   Append buffered comments to a DAS file.
   */
   public native synchronized static void dasac ( int         handle,
                                                  String[]    buffer )
      throws SpiceErrorException;



   /**
   Delete comments from a DAS file.
   */
   public native synchronized static void dasdc ( int         handle )

      throws SpiceErrorException;



   /**
   Extract comments from a DAS file into a buffer.
   */
   public native synchronized static void dasec ( int         handle,
                                                  int         bufsiz,
                                                  int         lenout,
                                                  int[]       n,
                                                  String[]    buffer,
                                                  boolean[]   done   )
      throws SpiceErrorException;



   /**
   Map a DAS file handle to the corresponding file name.
   */
   public native synchronized static String dashfn ( int  handle )

      throws SpiceErrorException;



   /**
   Open a DAS file for subsequent read requests.
   */
   public native synchronized static int dasopr ( String   fname )

      throws SpiceErrorException;



   /**
   Open a DAS file for write access.
   */
   public native synchronized static int dasopw ( String   fname )

      throws SpiceErrorException;


   /**
   Read file record paramters from a DAS file.
   */
   public native synchronized static void dasrfr ( int         handle,
                                                   String[]    idword,
                                                   String[]    ifname,
                                                   int[]       nresvr,
                                                   int[]       nresvc,
                                                   int[]       ncomr,
                                                   int[]       ncomc  )
      throws SpiceErrorException;


   /**
   Return the Jacobian matrix of the rectangular to cylindrical
   transformation.
   */
   public native synchronized static double[][] dcyldr ( double  x,
                                                         double  y,
                                                         double  z )
      throws SpiceErrorException;


   /**
   Return the difference between TDB and UTC at a specified epoch.
   */
   public native synchronized static double deltet ( double    epoch,
                                                     String    eptype )
      throws SpiceErrorException;


   /**
   Return the determinant of a 3x3 matrix.
   */
   public native synchronized static double det ( double[][] m )

      throws SpiceErrorException;



   /**
   Return the Jacobian matrix of the rectangular to geodetic
   transformation.
   */
   public native synchronized static double[][] dgeodr ( double  x,
                                                         double  y,
                                                         double  z,
                                                         double  re,
                                                         double  f   )
      throws SpiceErrorException;


   /**
   Begin a backward search for arrays in a DLA.
   */
   public native synchronized static void dlabbs ( int        handle,
                                                   int[]      descr,
                                                   boolean[]  found  )
      throws SpiceErrorException;


   /**
   Begin a forward search for arrays in a DLA.
   */
   public native synchronized static void dlabfs ( int        handle,
                                                   int[]      descr,
                                                   boolean[]  found  )
      throws SpiceErrorException;



   /**
   Find the next (forward) segment in a DLA.
   */
   public native synchronized static void dlafns ( int        handle,
                                                   int[]      descr,
                                                   int[]      nxtdsc,
                                                   boolean[]  found   )
      throws SpiceErrorException;



   /**
   Find the previous (backward) segment in a DLA.
   */
   public native synchronized static void dlafps ( int        handle,
                                                   int[]      descr,
                                                   int[]      prvdsc,
                                                   boolean[]  found   )
      throws SpiceErrorException;



   /**
   Return the Jacobian matrix of the rectangular to latitudinal
   transformation.
   */
   public native synchronized static double[][] dlatdr ( double  x,
                                                         double  y,
                                                         double  z )
      throws SpiceErrorException;



   /**
   Return the Jacobian matrix of the rectangular to planetographic
   transformation.
   */
   public native synchronized static double[][] dpgrdr ( String  body,
                                                         double  x,
                                                         double  y,
                                                         double  z,
                                                         double  re,
                                                         double  f   )
      throws SpiceErrorException;



   /**
   Return the number of degrees per radian.
   */
   public native synchronized static double dpr ();


   /**
   Return the Jacobian matrix of the cylindrical to rectangular
   transformation.
   */
   public native synchronized static double[][] drdcyl ( double  radius,
                                                         double  longitude,
                                                         double  z         )
      throws SpiceErrorException;


   /**
   Return the Jacobian matrix of the geodetic to rectangular
   transformation.
   */
   public native synchronized static double[][] drdgeo ( double  longitude,
                                                         double  latitude,
                                                         double  altitude,
                                                         double  re,
                                                         double  f           )
      throws SpiceErrorException;


   /**
   Return the Jacobian matrix of the latitudinal to rectangular
   transformation.
   */
   public native synchronized static double[][] drdlat ( double  radius,
                                                         double  longitude,
                                                         double  latitude   )
      throws SpiceErrorException;


   /**
   Return the Jacobian matrix of the planetographic to rectangular
   transformation.
   */
   public native synchronized static double[][] drdpgr ( String  body,
                                                         double  longitude,
                                                         double  latitude,
                                                         double  altitude,
                                                         double  re,
                                                         double  f           )
      throws SpiceErrorException;


   /**
   Return the Jacobian matrix of the spherical to rectangular
   transformation.
   */
   public native synchronized static double[][] drdsph ( double  radius,
                                                         double  colatitude,
                                                         double  longitude   )
      throws SpiceErrorException;



   /**
   Return bookkeeping data from a DSK type 2 segment.
   */
   public native synchronized static void dskb02 ( int         handle,
                                                   int[]       dladsc,
                                                   int[]       nv,
                                                   int[]       np,
                                                   int[]       nvxtot,
                                                   double[][]  vtxbds,
                                                   double[]    voxsiz,
                                                   double[]    voxori,
                                                   int[]       vgrext,
                                                   int[]       cgscal,
                                                   int[]       vtxnpl,
                                                   int[]       voxnpt,
                                                   int[]       voxnpl  )
      throws SpiceErrorException;



   /**
   Fetch double precision data from a type 2 DSK segment.
   */
   public native synchronized static double[] dskd02 ( int         handle,
                                                       int[]       dladsc,
                                                       int         item,
                                                       int         start,
                                                       int         room   )
      throws SpiceErrorException;



   /*
   Return the DSK descriptor from a DSK segment identified
   by a DAS handle and DLA descriptor. 
   */
   public native synchronized static double[] dskgd ( int         handle,
                                                      int[]       dladsc  )
      throws SpiceErrorException;



   /**
   Retrieve the value of a specified DSK tolerance or margin parameter.
   */
   public native synchronized static double dskgtl ( int keywrd )

      throws SpiceErrorException;



   /**
   Fetch integer data from a type 2 DSK segment.
   */
   public native synchronized static int[] dski02 ( int         handle,
                                                    int[]       dladsc,
                                                    int         item,
                                                    int         start,
                                                    int         room   )
      throws SpiceErrorException;


   /**
   Make spatial index for a DSK type 2 segment. The index is returned
   as a pair of arrays, one of type SpiceInt and one of type
   SpiceDouble. These arrays are suitable for use with the DSK type 2
   writer CSPICE.dskw02.
   */
   public native synchronized static void dskmi2 ( int         nv,
                                                   double[]    vrtces,
                                                   int         np,
                                                   int[]       plates,
                                                   double      finscl,
                                                   int         corscl,
                                                   int         worksz,
                                                   int         voxpsz,
                                                   int         voxlsz,
                                                   boolean     makvtl,
                                                   int         spxisz,
                                                   double[]    spaixd,
                                                   int[]       spaixi  )
      throws SpiceErrorException;



   /**
   Compute the unit normal vector for a specified plate from a type 
   2 DSK segment. 
   */
   public native synchronized static double[] dskn02 ( int         handle,
                                                       int[]       dladsc,
                                                       int         plid    )
      throws SpiceErrorException;



   /**
   Find the set of body ID codes of all objects for which
   topographic data are provided in a specified DSK file.
   */
   public native synchronized static int[] dskobj ( String        dsk,
                                                    int           size,
                                                    int[]         ids  )

      throws SpiceErrorException;



   /**
   Open a new DSK file for subsequent write operations.
   */
   public native synchronized static int dskopn ( String          fname,
                                                  String          ifname,
                                                  int             ncomch )
      throws SpiceErrorException;



   /**
   Fetch plates from a type 2 DSK segment.
   */
   public native synchronized static int[][] dskp02 ( int         handle,
                                                      int[]       dladsc,
                                                      int         start,
                                                      int         room   )
      throws SpiceErrorException;



   /**
   Derive range bounds on third coordinate for a plate set.

   Note that the plate and vertex arrays are 1-dimensional. 
   */
   public native synchronized static void dskrb2 ( int         nv,
                                                   double[]    vrtces,
                                                   int         np,
                                                   int[]       plates,
                                                   int         corsys,
                                                   double[]    corpar,
                                                   double[]    mncor3,
                                                   double[]    mxcor3  )
      throws SpiceErrorException;



   /**
   Set the value of a specified DSK tolerance or margin parameter.
   */
   public native synchronized static void dskstl ( int     keywrd,
                                                   double  dpval   )
      throws SpiceErrorException;



   /**
   Find the set of surface ID codes for all surfaces associated with
   a given body in a specified DSK file.
   */
   public native synchronized static int[] dsksrf ( String        dsk,
                                                    int           bodyid,
                                                    int           size,
                                                    int[]         srfids )
      throws SpiceErrorException;




   /**
   Fetch vertices from a type 2 DSK segment.
   */
   public native synchronized static double[][] dskv02 ( int         handle,
                                                         int[]       dladsc,
                                                         int         start,
                                                         int         room   )
      throws SpiceErrorException;


   /**
   Write a type 2 segment to a DSK file. 

   Note that the plate and vertex arrays are 1-dimensional. 
   */
   public native synchronized static void dskw02 ( int         handle,
                                                   int         center,
                                                   int         surfce,
                                                   int         dclass,
                                                   String      frame,
                                                   int         corsys,
                                                   double[]    corpar,
                                                   double      mncor1,
                                                   double      mxcor1,
                                                   double      mncor2,
                                                   double      mxcor2,
                                                   double      mncor3,
                                                   double      mxcor3,
                                                   double      first,
                                                   double      last,
                                                   int         nv,
                                                   double[]    vrtces,
                                                   int         np,
                                                   int[]       plates,
                                                   double[]    spaixd,
                                                   int[]       spaixi  )
      throws SpiceErrorException;



   /**
   Determine the plate ID and body-fixed coordinates of the
   intersection of a specified ray with the surface defined by a
   type 2 DSK plate model.
   */
   public native synchronized static void dskx02 ( int         handle,
                                                   int[]       dladsc,
                                                   double[]    vertex,
                                                   double[]    raydir,
                                                   int[]       plid,
                                                   double[]    xpt,
                                                   boolean[]   found  )
      throws SpiceErrorException;

 

   /**
   Compute a ray-surface intercept using data provided by
   multiple loaded DSK segments. Return information about
   the source of the data defining the surface on which the
   intercept was found: DSK handle, DLA and DSK descriptors,
   and DSK data type-dependent parameters.
   */
   public native synchronized static void dskxsi ( boolean       pri,
                                                   String        target,
                                                   int           nsurf,
                                                   int[]         srflst,
                                                   double        et,
                                                   String        fixref,
                                                   double[]      vertex,
                                                   double[]      raydir,
                                                   int           maxd,
                                                   int           maxi,
                                                   double[]      xpt,
                                                   int[]         handle,
                                                   int[]         dladsc,
                                                   double[]      dskdsc,
                                                   double[]      dc,
                                                   int[]         ic,
                                                   boolean[]     found   )
       throws SpiceErrorException;




   /**
   Compute ray-surface intercepts for a set of rays, using data
   provided by multiple loaded DSK segments.
   */
   public native synchronized static void dskxv ( boolean       pri,
                                                  String        target,
                                                  int           nsurf,
                                                  int[]         srflst,
                                                  double        et,
                                                  String        fixref,
                                                  int           nrays,
                                                  double[][]    vtxarr,
                                                  double[][]    dirarr,
                                                  double[][]    xptarr,
                                                  boolean[]     fndarr  )
      throws SpiceErrorException;



   /**
   Return plate model size parameters---plate count and
   vertex count---for a type 2 DSK segment.
   */
   public native synchronized static void dskz02 ( int         handle,
                                                   int[]       dladsc,
                                                   int[]       nv,
                                                   int[]       np   )
      throws SpiceErrorException;














   /**
   Return the Jacobian matrix of the rectangular to spherical
   transformation.
   */
   public native synchronized static double[][] dsphdr ( double  x,
                                                         double  y,
                                                         double  z )
      throws SpiceErrorException;




   /**
   Return type and dimension attributes of a kernel pool variable.
   */
   public native synchronized static void dtpool ( String     name,
                                                   boolean[]  found,
                                                   int[]      n,
                                                   String[]   type  )
      throws SpiceErrorException;



   /**
   Return the derivative of the cross product of two vectors.
   */
   public native synchronized static double[] dvcrss ( double[] s1,
                                                       double[] s2 )
      throws SpiceErrorException;


   /**
   Return the derivative of the dot product of two vectors.
   */
   public native synchronized static double dvdot ( double[] s1,
                                                    double[] s2 )
      throws SpiceErrorException;


   /**
   Return the unit vector defined by a state
   vector and corresponding derivative.
   */
   public native synchronized static double[] dvhat ( double[] s1 )

      throws SpiceErrorException;


   /**
   Return the derivative of the angular separation of two vectors.
   */
   public native synchronized static double dvsep ( double[] s1,
                                                    double[] s2 )
      throws SpiceErrorException;


   /**
   Delete a kernel pool variable.
   */
   public native synchronized static void dvpool ( String name )

      throws SpiceErrorException;



   /**
   Find the limb of a tri-axial ellipsoid as seen from a specified point.
   */
   public native synchronized static double[] edlimb ( double   a,
                                                       double   b,
                                                       double   c,
                                                       double[] viewpt )
      throws SpiceErrorException;


   /**
   Create a center and generating vectors from a SPICE ellipse.
   */
   public native synchronized static     void el2cgv ( double[]   ellipse,
                                                       double[]   center,
                                                       double[]   smajor,
                                                       double[]   sminor  )
      throws SpiceErrorException;


   /**
   Determine whether two strings are equivalent.
   */
   public native synchronized static boolean eqstr ( String a,
                                                     String b  )
      throws SpiceErrorException;



   /**
   Set or retrieve the default error action.
   */
   public native synchronized static String erract ( String op,
                                                     String action )
      throws SpiceErrorException;


   /**
   Set or retrieve the default error device.
   */
   public native synchronized static String errdev ( String op,
                                                     String device )
      throws SpiceErrorException;


   /**
   Convert an input time from ephemeris seconds past J2000
   to local solar time at a specified location on a specified body.
   */
   public native synchronized static void   et2lst ( double     et,
                                                     int        body,
                                                     double     lon,
                                                     String     type,
                                                     int[]      hr,
                                                     int[]      min,
                                                     int[]      sec,
                                                     String[]   time,
                                                     String[]   ampm  )
      throws SpiceErrorException;


   /**
   Convert an input time from ephemeris seconds past J2000
   to Calendar, Day-of-Year, or Julian Date format, UTC.
   */
   public native synchronized static String et2utc ( double     et,
                                                     String     format,
                                                     int        prec  )
      throws SpiceErrorException;


   /**
   Convert an input time from ephemeris seconds past J2000
   to a fixed format calendar TDB string.
   */
   public native synchronized static String etcal ( double     et )
      throws SpiceErrorException;



   /**
   Convert a sequence of Euler angles and axes to a rotation matrix.
   */
   public native synchronized static double[][] eul2m ( double[] angles,
                                                        int[]    axes   )
      throws SpiceErrorException;


   /**
   Convert a sequence of Euler angles, rates, and axes to a state
   transformation matrix.
   */
   public native synchronized static double[] eul2xf ( double[] angles,
                                                       int[]    axes   )
      throws SpiceErrorException;


   /**
   Look up the frame specification parameters associated with
   a frame ID code.
   */
   public native synchronized static void frinfo ( int        frameID,
                                                   int[]      centerID,
                                                   int[]      frclass,
                                                   int[]      frclassID,
                                                   boolean[]  found     )
      throws SpiceErrorException;


   /**
   Look up the frame name associated with a frame ID code.
   */
   public native synchronized static String frmnam ( int code )

      throws SpiceErrorException;


   /**
   Furnish a program with the kernels and related data needed
   to carry out the program's computations.
   */
   public native synchronized static void furnsh (  String file )

      throws SpiceErrorException;


   /**
   Return the character value of a kernel variable from the
   kernel pool.
   */
   public native synchronized static String[] gcpool ( String name,
                                                       int    start,
                                                       int    room  )

      throws SpiceErrorException, KernelVarNotFoundException;


   /**
   Return the double precision value of a kernel variable from
   the kernel pool.
   */
   public native synchronized static double[] gdpool ( String name,
                                                       int    start,
                                                       int    room  )

      throws SpiceErrorException, KernelVarNotFoundException;




   /**
   Convert from geodetic coordinates to rectangular coordinates.
   */
   public native synchronized static double[] georec ( double   longitude,
                                                       double   latitude,
                                                       double   altitude,
                                                       double   re,
                                                       double   f          )
      throws SpiceErrorException;


   /**
   Determine the file architecture and file type of most SPICE kernel
   files.
   */
   public native synchronized static void getfat ( String    file,
                                                   String[]  arch,
                                                   String[]  type )
      throws SpiceErrorException;


   /**
   Return the field-of-view (FOV) configuration for a
   specified instrument.
   */
   public native synchronized static void getfov (  int        instID,
                                                    String[]   shape,
                                                    String[]   ref,
                                                    double[]   bsight,
                                                    int[]      size,
                                                    double[]   bounds  )
      throws SpiceErrorException;



   /**
   Indicate whether an interrupt was detected.
   */
   public native synchronized static boolean gfbail()

      throws SpiceErrorException;


   /**
   Clear interrupt status.
   */
   public native synchronized static void gfclrh()

      throws SpiceErrorException;


   /**
   Perform a GF distance search.
   */
   public native synchronized static double[] gfdist ( String     target,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       String     relate,
                                                       double     refval,
                                                       double     adjust,
                                                       double     step,
                                                       int        nintvls,
                                                       double[]   cnfine  )
      throws SpiceErrorException;


   /**
   Perform a GF illumination angle search.
   */
   public native synchronized static double[] gfilum ( String     method,
                                                       String     angtyp,
                                                       String     target,
                                                       String     illmn,
                                                       String     fixref,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       double[]   spoint,
                                                       String     relate,
                                                       double     refval,
                                                       double     adjust,
                                                       double     step,
                                                       int        nintvls,
                                                       double[]   cnfine  )
      throws SpiceErrorException;





   /**
   Perform a custom GF occultation search using user-specified step
   and refinement functions.
   */
   public native synchronized static double[] gfocce ( String         occtyp,
                                                       String         front,
                                                       String         fshape,
                                                       String         fframe,
                                                       String         back,
                                                       String         bshape,
                                                       String         bframe,
                                                       String         abcorr,
                                                       String         obsrvr,
                                                       int            nintvls,
                                                       double[]       cnfine,
                                                       GFSearchUtils  utils  )
      throws SpiceErrorException;




   /**
   Return a coverage window for a specified reference frame class ID
   and PCK file. Union this coverage with that contained in an input window.
   */
   public native synchronized static double[] pckcov ( String    file,
                                                       int       classID,
                                                       int       size,
                                                       double[]  cover )
      throws SpiceErrorException;




   /**
   Return an ordered array of unique reference frame class ID codes of all 
   frames associated with segments in a specified binary PCK file.

   The returned array is the union of this ID set with the set of IDs in the
   input array `ids'.
   */
   public native synchronized static int[] pckfrm ( String         pck,
                                                    int            size,
                                                    int[]          ids   )

      throws SpiceErrorException;



   /**
   Perform a GF occultation search.
   */
   public native synchronized static double[] gfoclt ( String     occtyp,
                                                       String     front,
                                                       String     fshape,
                                                       String     fframe,
                                                       String     back,
                                                       String     bshape,
                                                       String     bframe,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       double     step,
                                                       int        nintvls,
                                                       double[]   cnfine  )
      throws SpiceErrorException;


   /**
   Perform a GF phase angle search.
   */
   public native synchronized static double[] gfpa ( String     target,
                                                     String     illum,
                                                     String     abcorr,
                                                     String     obsrvr,
                                                     String     relate,
                                                     double     refval,
                                                     double     adjust,
                                                     double     step,
                                                     int        nintvls,
                                                     double[]   cnfine  )
      throws SpiceErrorException;


   /**
   Perform a GF position vector coordinate search.
   */
   public native synchronized static double[] gfposc ( String     target,
                                                       String     frame,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       String     crdsys,
                                                       String     coord,
                                                       String     relate,
                                                       double     refval,
                                                       double     adjust,
                                                       double     step,
                                                       int        nintvls,
                                                       double[]   cnfine  )
      throws SpiceErrorException;



   /**
   Get a refined GF root estimate.
   */
   public native synchronized static double gfrefn ( double    t1,
                                                     double    t2,
                                                     boolean   s1,
                                                     boolean   s2 )
      throws SpiceErrorException;


   /**
   Finalize a GF progress report.
   */
   public native synchronized static void   gfrepf ()

      throws SpiceErrorException;


   /**
   Initialize a GF progress report.
   */
   public native synchronized static void   gfrepi ( double[] window,
                                                     String   begmsg,
                                                     String   endmsg )
      throws SpiceErrorException;



   /**
   Update a GF progress report.
   */
   public native synchronized static void   gfrepu ( double   ivbeg,
                                                     double   ivend,
                                                     double   time  )
      throws SpiceErrorException;



   /**
   Perform a GF ray in FOV search.
   */
   public native synchronized static double[] gfrfov ( String     inst,
                                                       double[]   raydir,
                                                       String     rframe,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       double     step,
                                                       int        nintvls,
                                                       double[]   cnfine  )
      throws SpiceErrorException;


   /**
   Perform a GF range rate search.
   */
   public native synchronized static double[] gfrr ( String     target,
                                                     String     abcorr,
                                                     String     obsrvr,
                                                     String     relate,
                                                     double     refval,
                                                     double     adjust,
                                                     double     step,
                                                     int        nintvls,
                                                     double[]   cnfine  )
      throws SpiceErrorException;



   /**
   Perform a GF angular separation coordinate search.
   */
   public native synchronized static double[] gfsep  ( String     targ1,
                                                       String     shape1,
                                                       String     frame1,
                                                       String     targ2,
                                                       String     shape2,
                                                       String     frame2,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       String     relate,
                                                       double     refval,
                                                       double     adjust,
                                                       double     step,
                                                       int        nintvls,
                                                       double[]   cnfine  )
      throws SpiceErrorException;


   /**
   Perform a GF surface intercept coordinate search.
   */
   public native synchronized static double[] gfsntc ( String     target,
                                                       String     fixref,
                                                       String     method,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       String     dref,
                                                       double[]   dvec,
                                                       String     crdsys,
                                                       String     coord,
                                                       String     relate,
                                                       double     refval,
                                                       double     adjust,
                                                       double     step,
                                                       int        nintvls,
                                                       double[]   cnfine  )
      throws SpiceErrorException;



   /**
   Set the GF step size.
   */
   public native synchronized static void gfsstp ( double    step )

      throws SpiceErrorException;



   /**
   Get the current GF step size.
   */
   public native synchronized static double gfstep ( double    et )

      throws SpiceErrorException;



   /**
   Set the GF tolerance.
   */
   public native synchronized static void gfstol ( double     tol )

      throws SpiceErrorException;



   /**
   Perform a GF sub-observer point coordinate search.
   */
   public native synchronized static double[] gfsubc ( String     target,
                                                       String     fixref,
                                                       String     method,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       String     crdsys,
                                                       String     coord,
                                                       String     relate,
                                                       double     refval,
                                                       double     adjust,
                                                       double     step,
                                                       int        nintvls,
                                                       double[]   cnfine  )
      throws SpiceErrorException;


   /**
   Perform a GF target in FOV search.
   */
   public native synchronized static double[] gftfov ( String     inst,
                                                       String     target,
                                                       String     tshape,
                                                       String     tframe,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       double     step,
                                                       int        nintvls,
                                                       double[]   cnfine  )
      throws SpiceErrorException;




   /**
   Perform a GF user-defined scalar quantity search.
   */
   public native synchronized static  double[] gfuds ( GFScalarQuantity quant,
                                                       String           relate,
                                                       double           refval,
                                                       double           adjust,
                                                       double           step,
                                                       int             nintvls,
                                                       double[]        cnfine )
      throws SpiceErrorException;



   /**
   Return the integer value of a kernel variable from the kernel pool.
   */
   public native synchronized static int[] gipool ( String name,
                                                    int    start,
                                                    int    room  )

      throws SpiceErrorException, KernelVarNotFoundException;


   /**
   Retrieve the names of kernel variables matching a specified
   template.
   */
   public native synchronized static String[] gnpool ( String  template,
                                                       int     start,
                                                       int     room  )
      throws SpiceErrorException;



   /**
   Find the intersection of a plane and a triaxial ellipsoid.
   */
   public native synchronized static     void inedpl ( double     a,
                                                       double     b,
                                                       double     c,         
                                                       double[]   plane,
                                                       double[]   ellipse,
                                                       boolean[]  found    )
      throws SpiceErrorException;



   /**
   Find the intersection of an ellipse and a plane.
   */
   public native synchronized static     void inelpl ( double[]   ellipse,
                                                       double[]   plane,
                                                       int[]      nxpts,
                                                       double[]   xpt1,
                                                       double[]   xpt2    )
      throws SpiceErrorException;





   /**
   Compute the illumination angles---phase, incidence, and
   emission---at a specified point on a target body. Return logical
   flags indicating whether the surface point is visible from
   the observer's position and whether the surface point is
   illuminated.

   The target body's surface is represented using topographic data
   provided by DSK files, or by a reference ellipsoid.

   The illumination source is a specified ephemeris object.
   */
   public native synchronized static     void illumf ( String     method,
                                                       String     target,
                                                       String     ilusrc,
                                                       double     et,
                                                       String     fixref,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       double[]   spoint,
                                                       double[]   trgepc,
                                                       double[]   srfvec,
                                                       double[]   angles,
                                                       boolean[]  visibl,
                                                       boolean[]  lit    )
      throws SpiceErrorException;



   /**
   Compute the illumination angles---phase, solar incidence, and
   emission---at a specified point on a target body at a particular
   epoch, optionally corrected for light time and stellar aberration.
   The target body's surface is represented by a triangular plate model
   contained in a type 2 DSK segment.
   */
   public native synchronized static void illumPl02 ( int        handle,
                                                      int[]      dladsc,
                                                      String     target,
                                                      double     et,
                                                      String     abcorr,
                                                      String     obsrvr,
                                                      double[]   spoint,
                                                      double[]   trgepc,
                                                      double[]   srfvec,
                                                      double[]   angles   )
      throws SpiceErrorException;




   /**
   Find the illumination angles at a specified surface point of a
   target body.
   */
   public native synchronized static void ilumin     ( String     method,
                                                       String     target,
                                                       double     et,
                                                       String     fixref,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       double[]   spoint,
                                                       double[]   trgepc,
                                                       double[]   srfvec,
                                                       double[]   angles   )
      throws SpiceErrorException;



 
   /**
   Find the intersection of a ray and a plane.
   */
   public native synchronized static void inrypl ( double[]    vertex,
                                                   double[]    dir,
                                                   double[]    plane,
                                                   int[]       nxpts,
                                                   double[]    xpt     )
      throws SpiceErrorException;



   /**
   Return the inverse of a nonsingular 3x3 matrix.
   */
   public native synchronized static double[][] invert ( double[][] m )

      throws SpiceErrorException;


   /**
   Indicate whether a given matrix is a rotation matrix.
   */
   public native synchronized static boolean isrot ( double[][] m,
                                                     double     ntol,
                                                     double     dtol  )

      throws SpiceErrorException;


   /**
   Return the Julian Date of 1899 DEC 31 12:00:00 (1900 JAN 0.5).
   */
   public native synchronized static double j1900 ();


   /**
   Return the Julian Date of 1950 JAN 01 00:00:00 (1950 JAN 1.0).
   */
   public native synchronized static double j1950 ();


   /**
   Return the Julian Date of 2000 JAN 01 12:00:00 (2000 JAN 1.5).
   */
   public native synchronized static double j2000 ();


   /**
   Return the Julian Date of 2100 JAN 01 12:00:00 (2100 JAN 1.5).
   */
   public native synchronized static double j2100 ();


   /**
   Return the number of seconds in a Julian year.
   */
   public native synchronized static double jyear ();


   /**
   Clear the keeper system and kernel pool.
   */
   public native synchronized static void kclear ();


   /**
   Return data for a kernel of specified type and index
   loaded in the keeper system.
   */
   public native synchronized static void kdata ( int       which,
                                                  String    kind,
                                                  String[]  file,
                                                  String[]  filtyp,
                                                  String[]  source,
                                                  int[]     handle,
                                                  boolean[] found   )
      throws SpiceErrorException;


   /**
   Return data for a kernel, specified by name, that is
   loaded in the keeper system.
   */
   public native synchronized static void kinfo ( String    file,
                                                  String[]  filtyp,
                                                  String[]  source,
                                                  int[]     handle,
                                                  boolean[] found   )
      throws SpiceErrorException;


   /**
   Return the count of kernels of a specified type
   loaded in the keeper system.
   */
   public native synchronized static int ktotal ( String kind )

      throws SpiceErrorException;



   /**
   Convert from latitudinal coordinates to cylindrical coordinates.
   */
   public native synchronized static double[] latcyl ( double   radius,
                                                       double   longitude,
                                                       double   latitude    )
      throws SpiceErrorException;


   /**
   Convert from latitudinal coordinates to rectangular coordinates.
   */
   public native synchronized static double[] latrec ( double   radius,
                                                       double   longitude,
                                                       double   latitude    )
      throws SpiceErrorException;


   /**
   Convert from latitudinal coordinates to spherical coordinates.
   */
   public native synchronized static double[] latsph ( double   radius,
                                                       double   longitude,
                                                       double   latitude    )
      throws SpiceErrorException;



   /**
   Map array of planetocentric longitude/latitude coordinate pairs
   to surface points on a specified target body.

   The surface of the target body may be represented by a triaxial
   ellipsoid or by topographic data provided by DSK files.
   */
   public native synchronized static double[][] latsrf ( String        method, 
                                                         String        target,
                                                         double        et,
                                                         String        fixref,
                                                         int           npts,
                                                         double[][]    lonlat  )
      throws SpiceErrorException;


   /**
   Load a text kernel into the kernel pool.
   */
   public native synchronized static void ldpool ( String filename )

      throws SpiceErrorException;



   /**
   Find limb points on a target body. The limb is the set of points
   of tangency on the target of rays emanating from the observer.
   The caller specifies half-planes bounded by the observer-target
   center vector in which to search for limb points.

   The surface of the target body may be represented either by a
   triaxial ellipsoid or by topographic data.
   */
   public native synchronized static void limbpt ( String         method,
                                                   String         target,
                                                   double         et,
                                                   String         fixref,
                                                   String         abcorr, 
                                                   String         corloc,
                                                   String         obsrvr,
                                                   double[]       refvec,
                                                   double         rolstp,
                                                   int            ncuts,
                                                   double         schstp,
                                                   double         soltol,
                                                   int            maxn,
                                                   int[]          npts,
                                                   double[][]     points,
                                                   double[]       epochs,
                                                   double[][]     tangts  )

      throws SpiceErrorException;




   /**
   Compute a set of points on the limb of a specified target body,
   where the target body's surface is represented by a triangular plate
   model contained in a type 2 DSK segment.
   */
   public native synchronized static void limbPl02 ( int           handle,
                                                     int[]         dladsc,
                                                     String        target,
                                                     double        et,
                                                     String        fixfrm,
                                                     String        abcorr,
                                                     String        obsrvr,
                                                     int           npoints,
                                                     double[]      trgepc,
                                                     double[]      obspos,
                                                     double[][]    limbpts,
                                                     int[]         plateIDs )
      throws SpiceErrorException;



   /**  
   Given the planetocentric longitude and latitude values of a set of
   surface points on a specified target body, compute the corresponding
   rectangular coordinates of those points.  The target body's
   surface is represented by a triangular plate model contained in a
   type 2 DSK segment.

   CAUTION: the argument list of this routine has changed!
   This routine accepts the input grid values as a 2-D (Nx2) array.
   The previous version accepted a 1D array.

   Elements

       [i][0]
       [i][1]

   of `grid' are, respectively, the longitude and latitude of the
   ith grid point.

   */
   public native synchronized static  void llgridPl02 ( int         handle,
                                                        int[]       dladsc,
                                                        double[][]  grid,
                                                        double[][]  spoints,
                                                        int[]       plateIDs )
      throws SpiceErrorException;




   /**
   Load variables contained in a string array into the kernel pool.
   */
   public native synchronized static void lmpool ( String[]  cvals  )

      throws SpiceErrorException;


   /**
   Compute the one-way light time between a specified target and
   observer.
   */
   public native synchronized static void ltime ( double    etobs,
                                                  int       obs,
                                                  String    dir,
                                                  int       targ,
                                                  double[]  ettarg,
                                                  double[]  elapsd  )
      throws SpiceErrorException;


   /**
   Convert a rotation matrix and a sequence of axes to a
   sequence of Euler angles.
   */
   public native synchronized static double[] m2eul ( double[][]   m,
                                                      int[]        axes )
      throws SpiceErrorException;


   /**
   Convert a rotation matrix to a SPICE quaternion.
   */
   public native synchronized static double[] m2q ( double[][]  m )

      throws SpiceErrorException;



   /**
   Multiply two 3x3 matrices.
   */
   public native synchronized static double[][] mxm ( double[][]  m1,
                                                      double[][]  m2  )
      throws SpiceErrorException;



   /**
   Left-multiply a 3-vector by a 3x3 matrix.
   */
   public native synchronized static double[]   mxv ( double[][]  m,
                                                      double[]    v  )
      throws SpiceErrorException;



   /**
   Look up the frame ID code associated with a frame name.
   */
   public native synchronized static int namfrm ( String name )

      throws SpiceErrorException;


   /**
   Return the nearest point on an ellipsoid to a given line; also return
   the distance between the ellipsoid and the line.
   */
   public native synchronized static void     npedln ( double   a,
                                                       double   b,
                                                       double   c,
                                                       double[] linept,
                                                       double[] linedr,
                                                       double[] point,
                                                       double[] dist    )
      throws SpiceErrorException;


   /**
   Return the nearest point on an ellipseto a given point; also return
   the distance between the ellipse and the point.
   */
   public native synchronized static void     npelpt ( double[] point,
                                                       double[] ellipse,
                                                       double[] pnear,
                                                       double[] dist    )
      throws SpiceErrorException;


   /**
   Return the nearest point on a line to a given point; also return
   the distance between the two points.
   */
   public native synchronized static void     nplnpt ( double[] linept,
                                                       double[] linedr,
                                                       double[] point,
                                                       double[] pnear,
                                                       double[] dist   )
      throws SpiceErrorException;


   /**
   Convert a normal vector and constant to a plane.
   */
   public native synchronized static double[] nvc2pl ( double[]  normal,
                                                       double    constant )
      throws SpiceErrorException;



   /**
   Convert a normal vector and point to a plane.
   */
   public native synchronized static double[] nvp2pl ( double[]  normal,
                                                       double[]  point   )
      throws SpiceErrorException;



   /**
   This routine locates the point on the surface of an ellipsoid
   that is nearest to a specified position. It also returns the
   altitude of the position above the ellipsoid.
   */
   public native synchronized static void nearpt    (  double[]   positn,
                                                       double     a,
                                                       double     b,
                                                       double     c,
                                                       double[]   point,
                                                       double[]   alt     )
      throws SpiceErrorException;



   /**
   Determine the occultation condition (not occulted, partially,
   etc.) of one target relative to another target as seen by
   an observer at a given time.

   The surfaces of the target bodies may be represented by triaxial
   ellipsoids or by topographic data provided by DSK files.
   */
    public native synchronized static int occult ( String         targ1,
                                                   String         shape1,
                                                   String         frame1,
                                                   String         targ2,
                                                   String         shape2,
                                                   String         frame2,
                                                   String         abcorr,
                                                   String         obsrvr,
                                                   double         et      )
      throws SpiceErrorException;                                 



   /**
   Compute osculating elements corresponding to a state.
   */
   public native synchronized static double[] oscelt ( double[] state,
                                                       double   et,
                                                       double   mu     )
      throws SpiceErrorException;


   /**
   Compute extended osculating elements corresponding to a state.
   */
   public native synchronized static double[] oscltx ( double[] state,
                                                       double   et,
                                                       double   mu     )
      throws SpiceErrorException;


   /**
   Close a binary PCK file.
   */
   public native synchronized static void pckcls ( int   handle )

      throws SpiceErrorException;



   /**
   Open a new binary PCK file.
   */
   public native synchronized static int pckopn ( String     fname,
                                                  String     ifname,
                                                  int        ncomch )
      throws SpiceErrorException;



   /**
   Unload a binary PCK from the PCKBSR system.
   */
   public native synchronized static void pckuof ( int       handle )

      throws SpiceErrorException;



   /**
   Write a type 2 segment to a binary PCK file.
   */
   public native synchronized static void pckw02 ( int         handle,
                                                   int         clssid,
                                                   String      frame,
                                                   double      first,
                                                   double      last,
                                                   String      segid,
                                                   double      intlen,
                                                   int         n,
                                                   int         polydg,
                                                   double[]    cdata,
                                                   double      btime  )
      throws SpiceErrorException;




   /**
   Insert a character kernel variable into the kernel pool.
   */
   public native synchronized static void pcpool (  String    name,
                                                    String[]  cvals  )

      throws SpiceErrorException;



   /**
   Insert a double precision kernel variable into the kernel pool.
   */
   public native synchronized static void pdpool (  String    name,
                                                    double[]  dvals  )

      throws SpiceErrorException;


   /**
   Convert from planetographic coordinates to rectangular coordinates.
   */
   public native synchronized static double[] pgrrec ( String   body,
                                                       double   longitude,
                                                       double   latitude,
                                                       double   altitude,
                                                       double   re,
                                                       double   f          )
      throws SpiceErrorException;


   /**
   Return the value of pi.
   */
   public native synchronized static double pi ();



   /**
   Insert an integer kernel variable into the kernel pool.
   */
   public native synchronized static void pipool (  String    name,
                                                    int[]     ivals  )
      throws SpiceErrorException;


   /**
   Project an ellipse orthogonally onto a plane.
   */
   public native synchronized static double[] pjelpl ( double[] elin,
                                                       double[] plane )
      throws SpiceErrorException;


   /**
   Map a plane array to a normal vector and constant.
   */
   public native synchronized static void pl2nvc (  double[]  plane,
                                                    double[]  normal,
                                                    double[]  constant )
      throws SpiceErrorException;

   /**
   Map a plane array to a normal vector and point.
   */
   public native synchronized static void pl2nvp (  double[]  plane,
                                                    double[]  normal,
                                                    double[]  point )
      throws SpiceErrorException;


   /**
   Map a plane array to a point and spanning vectors.
   */
   public native synchronized static void pl2psv (  double[]  plane,
                                                    double[]  point,
                                                    double[]  span1,
                                                    double[]  span2 )
      throws SpiceErrorException;

  
   /**
   Compute the total area of a collection of triangular plates.
   */
   public native synchronized static double pltar ( int          nv,
                                                    double[]     vrtces,
                                                    int          np,
                                                    int[]        plates )
      throws SpiceErrorException;



   /**
   Expand a triangular plate by a specified amount. The expanded
   plate is co-planar with, and has the same orientation as, the
   original. The centroids of the two plates coincide.
   */
   public native synchronized static double[][] pltexp ( double[][]   iverts,
                                                         double       delta   )
      throws SpiceErrorException;



   /**
   Find the nearest point on a triangular plate to a given point. 
   */
   public native synchronized static void pltnp ( double[] point,
                                                  double[] v1,
                                                  double[] v2,
                                                  double[] v3,
                                                  double[] pnear,
                                                  double[] dist   )
      throws SpiceErrorException;



   /**
   Compute an outward normal vector of a triangular plate.
   The vector does not necessarily have unit length.
   */
   public native synchronized static double[] pltnrm ( double[] v1,
                                                       double[] v2,
                                                       double[] v3 )
      throws SpiceErrorException;



   /**
   Compute the volume of a three-dimensional region bounded by a
   collection of triangular plates.
   */
   public native synchronized static double pltvol ( int          nv,
                                                     double[]     vrtces,
                                                     int          np,
                                                     int[]        plates )
      throws SpiceErrorException;



   /**
   Convert a point and two spanning vectors to a plane.
   */
   public native synchronized static double[] psv2pl ( double[]  point,
                                                       double[]  span1,
                                                       double[]  span2 )
      throws SpiceErrorException;



   /**
   Store the contents of argv for later access.
   */
   public native synchronized static void putcml (  String[]   argv  )

      throws SpiceErrorException;


   /**
   This function prompts a user for keyboard input.
   */
   public native synchronized static String prompt   ( String   promptStr )

      throws SpiceErrorException;


   /**
   Propagate a state using a two-body model.
   */
   public native synchronized static void prop2b ( double      gm,
                                                   double[]    pvinit,
                                                   double      dt,
                                                   double[]    pvprop )
      throws SpiceErrorException;


   /**
   Return the rotation matrix from one frame to
   another at a specified epoch.
   */
   public native synchronized static double[][] pxform  (  String     from,
                                                           String     to,
                                                           double     et   )
      throws SpiceErrorException;


   /**
   Return the 3x3 matrix that transforms position vectors from one
   specified frame at a specified epoch to another specified
   frame at another specified epoch.
   */
   public native synchronized static double[][] pxfrm2  (  String     from,
                                                           String     to,
                                                           double     etfrom,
                                                           double     etto   )
      throws SpiceErrorException;




   /**
   Convert a SPICE quaternion to a matrix.
   */
   public native synchronized static double[][] q2m ( double[]   q )

      throws SpiceErrorException;




   /**
   Convert a SPICE quaternion and its derivative to angular velocity.
   */
   public native synchronized static double[] qdq2av ( double[]   q,
                                                       double[]   dq )
      throws SpiceException;


   /**
   Multiply two SPICE quaternions.
   */
   public native synchronized static double[] qxq ( double[]   q1,
                                                    double[]   q2 )
      throws SpiceException;



   /**
   Convert from range, right ascension, and declination to
   rectangular coordinates.
   */
   public native synchronized static double[] radrec ( double   range,
                                                       double   ra,
                                                       double   dec        )
      throws SpiceErrorException;


   /**
   Convert a rotation matrix and angular velocity vector to
   a state transformation matrix.
   */
   public native synchronized static double[] rav2xf ( double[][]   r,
                                                       double[]     av )
      throws SpiceErrorException;


   /**
   Map a rotation matrix to a rotation axis and angle.
   */
   public native synchronized static void raxisa ( double[][]   r,
                                                   double[]     axis,
                                                   double[]     angle  )
      throws SpiceErrorException;


   /**
   Convert from rectangular coordinates to cylindrical coordinates.
   */
   public native synchronized static double[] reccyl ( double[]   rectan )

      throws SpiceErrorException;



   /**
   Convert from rectangular coordinates to geodetic coordinates.
   */
   public native synchronized static double[] recgeo ( double[]   rectan,
                                                       double     Re,
                                                       double     f      )
      throws SpiceErrorException;


   /**
   Convert from rectangular coordinates to latitudinal coordinates.
   */
   public native synchronized static double[] reclat ( double[]   rectan )

      throws SpiceErrorException;



   /**
   Convert from rectangular coordinates to planetographic coordinates.
   */
   public native synchronized static double[] recpgr ( String     body,
                                                       double[]   rectan,
                                                       double     Re,
                                                       double     f      )
      throws SpiceErrorException;



   /**
   Convert from rectangular coordinates to RA/Dec coordinates.
   */
   public native synchronized static double[] recrad ( double[]   rectan )

      throws SpiceErrorException;



   /**
   Convert from rectangular coordinates to spherical coordinates.
   */
   public native synchronized static double[] recsph ( double[]   rectan )

      throws SpiceErrorException;



   /**
   Reset SPICE error handling system.
   */
   public native synchronized static void reset ();


   /**
   Create a matrix that rotates a reference frame by a specified
   angle about a specified coordinate axis.
   */
   public native synchronized static double[][] rotate ( double angle,
                                                         int    iaxis  )
      throws SpiceErrorException;


   /**
   Create a matrix that rotates a rotation matrix by a specified
   angle about a specified coordinate axis.
   */
   public native synchronized static double[][] rotmat ( double[][]  r,
                                                         double      angle,
                                                         int         iaxis  )
      throws SpiceErrorException;


   /**
   Transform a vector to a basis rotated by a specified
   angle about a specified coordinate axis.
   */
   public native synchronized static double[] rotvec ( double[]    v,
                                                       double      angle,
                                                       int         iaxis  )
      throws SpiceErrorException;


   /**
   Return the number of radians per degree.
   */
   public native synchronized static double rpd ();



   /**
   Convert double precision encoding of spacecraft clock time into
   a character representation.
   */
   public native synchronized static String scdecd (  int        clkid,
                                                      double     sclkdp )
      throws SpiceErrorException;


   /**
   Convert ephemeris seconds past J2000 (ET) to continuous encoded
   spacecraft clock ("ticks").  Non-integral tick values may be
   returned.
   */
   public native synchronized static double sce2c (  int        clkid,
                                                     double     et  )
      throws SpiceErrorException;


   /**
   Convert ephemeris seconds past J2000 (ET) to a character string
   representation of an SCLK value.
   */
   public native synchronized static String sce2s (  int        clkid,
                                                     double     et  )
      throws SpiceErrorException;


   /**
   Encode character representation of spacecraft clock time into a
   double precision number.
   */
   public native synchronized static double scencd (  int        clkid,
                                                      String     sclkch )

      throws SpiceErrorException;


   /**
   Convert a tick duration to an SCLK string.
   */
   public native synchronized static String  scfmt ( int        sc,
                                                     double     sclkdp )
      throws SpiceErrorException;


   /**
   Convert a spacecraft clock string to ephemeris seconds past
   J2000 (ET).
   */
   public native synchronized static double scs2e (  int        clkid,
                                                     String     sclkch )
      throws SpiceErrorException;


   /**
   Convert encoded spacecraft clock (`ticks') to ephemeris
   seconds past J2000 (ET).
   */
   public native synchronized static double sct2e (  int        clkid,
                                                     double     sclkdp )
      throws SpiceErrorException;


   /**
   Convert a spacecraft clock format string to number of "ticks".
   */
   public native synchronized static double sctiks (  int        clkid,
                                                      String     clkstr )
      throws SpiceErrorException;


   /**
   Determine the coordinates of the surface intercept of a ray on a target
   body at a particular epoch, optionally corrected for planetary
   (light time) and stellar aberration.  Also, return the epoch
   associated with the surface intercept and the vector from
   the observer to the surface intercept; this vector is expressed
   in the frame designated by `fixref'.
   */
   public native synchronized static void sincpt     ( String     method,
                                                       String     target,
                                                       double     et,
                                                       String     fixref,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       String     dref,
                                                       double[]   dvec,
                                                       double[]   spoint,
                                                       double[]   trgepc,
                                                       double[]   srfvec,
                                                       boolean[]  found   )
      throws SpiceErrorException;


   /**
   Return the number of seconds in a Julian day.
   */
   public native synchronized static double spd();



   /**
   Convert from spherical coordinates to cylindrical coordinates.
   */
   public native synchronized static double[] sphcyl ( double   r,
                                                       double   colatitude,
                                                       double   longitude   )
      throws SpiceErrorException;



   /**
   Convert from spherical coordinates to latitudinal coordinates.
   */
   public native synchronized static double[] sphlat ( double   r,
                                                       double   colatitude,
                                                       double   longitude   )
      throws SpiceErrorException;



   /**
   Convert from spherical coordinates to rectangular coordinates.
   */
   public native synchronized static double[] sphrec ( double   r,
                                                       double   colatitude,
                                                       double   longitude   )
      throws SpiceErrorException;



   /**
   Close an SPK file.
   */
   public native synchronized static void spkcls ( int   handle )

      throws SpiceErrorException;



   /**
   Return a coverage window for a specified body and SPK file. Add this
   coverage to that contained in an input window.
   */
   public native synchronized static double[] spkcov ( String    file,
                                                       int       body,
                                                       int       size,
                                                       double[]  cover )
      throws SpiceErrorException;




   /**
   Return the state of a specified target relative to an "observer," 
   where the observer has constant position in a specified reference 
   frame. The observer's position is provided by the calling program 
   rather than by loaded SPK files. 
   */
   public native synchronized static void spkcpo     (  String     target,
                                                        double     et,
                                                        String     outref,
                                                        String     refloc,
                                                        String     abcorr,
                                                        double[]   obspos,
                                                        String     obsctr,
                                                        String     obsref,
                                                        double[]   state,
                                                        double[]   lt       )
      throws SpiceErrorException;



   /**
   Return the state, relative to a specified observer, of a target
   having constant position in a specified reference frame. The
   target's position is provided by the calling program rather than by
   loaded SPK files. 
   */
   public native synchronized static void spkcpt     (  double[]   trgpos,
                                                        String     trgctr,
                                                        String     trgref,
                                                        double     et,
                                                        String     outref,
                                                        String     refloc,
                                                        String     abcorr,
                                                        String     obsrvr,
                                                        double[]   state,
                                                        double[]   lt       )
      throws SpiceErrorException;



   /**
   Return the state of a specified target relative to an "observer," 
   where the observer has constant velocity in a specified reference 
   frame. The observer's state is provided by the calling program 
   rather than by loaded SPK files. 
   */
   public native synchronized static void spkcvo     (  String     target,
                                                        double     et,
                                                        String     outref,
                                                        String     refloc,
                                                        String     abcorr,
                                                        double[]   obssta,
                                                        double     obsepc,
                                                        String     obsctr,
                                                        String     obsref,
                                                        double[]   state,
                                                        double[]   lt       )
      throws SpiceErrorException;



   /**
   Return the state, relative to a specified observer, of a target
   having constant velocity in a specified reference frame. The
   target's state is provided by the calling program rather than by
   loaded SPK files. 
   */
   public native synchronized static void spkcvt     (  double[]   trgsta,
                                                        double     trgepc,
                                                        String     trgctr,
                                                        String     trgref,
                                                        double     et,
                                                        String     outref,
                                                        String     refloc,
                                                        String     abcorr,
                                                        String     obsrvr,
                                                        double[]   state,
                                                        double[]   lt       )
      throws SpiceErrorException;




   /**
   Return the state (position and velocity) of a target body
   relative to an observing body, optionally corrected for light
   time (planetary aberration) and stellar aberration.
   */
   public native synchronized static void spkezr     (  String     target,
                                                        double     et,
                                                        String     ref,
                                                        String     abcorr,
                                                        String     observer,
                                                        double[]   state,
                                                        double[]   lt       )
      throws SpiceErrorException;




   /**
   Return an ordered array of unique ID codes of bodies for which a
   specified SPK file contains data.
   */
   public native synchronized static int[] spkobj ( String    file,
                                                    int       size,
                                                    int[]     ids  )
      throws SpiceErrorException;


   /**
   Open a new SPK file.
   */
   public native synchronized static int spkopn ( String     fname,
                                                  String     ifname,
                                                  int        ncomch )
      throws SpiceErrorException;


   /**
   Return the position of a target body relative to an observing body,
   optionally corrected for light time (planetary aberration) and
   stellar aberration.
   */
   public native synchronized static void spkpos     (  String     target,
                                                        double     et,
                                                        String     ref,
                                                        String     abcorr,
                                                        String     observer,
                                                        double[]   pos,
                                                        double[]   lt       )
      throws SpiceErrorException;


   /**
   Unload an SPK from the SPKBSR system.
   */
   public native synchronized static void spkuef ( int       handle )

      throws SpiceErrorException;


   /**
   Write a type 2 segment to an SPK file.
   */
   public native synchronized static void spkw02 ( int         handle,
                                                   int         body,
                                                   int         center,
                                                   String      frame,
                                                   double      first,
                                                   double      last,
                                                   String      segid,
                                                   double      intlen,
                                                   int         n,
                                                   int         polydg,
                                                   double[]    cdata,
                                                   double      btime  )
      throws SpiceErrorException;


   /**
   Write a type 3 segment to an SPK file.
   */
   public native synchronized static void spkw03 ( int         handle,
                                                   int         body,
                                                   int         center,
                                                   String      frame,
                                                   double      first,
                                                   double      last,
                                                   String      segid,
                                                   double      intlen,
                                                   int         n,
                                                   int         polydg,
                                                   double[]    cdata,
                                                   double      btime  )
      throws SpiceErrorException;


   /**
   Write a type 5 segment to an SPK file.
   */
   public native synchronized static void spkw05 ( int         handle,
                                                   int         body,
                                                   int         center,
                                                   String      frame,
                                                   double      first,
                                                   double      last,
                                                   String      segid,
                                                   double      gm,
                                                   int         n,
                                                   double[]    states,
                                                   double[]    epochs  )
      throws SpiceErrorException;


   /**
   Write a type 9 segment to an SPK file.
   */
   public native synchronized static void spkw09 ( int         handle,
                                                   int         body,
                                                   int         center,
                                                   String      frame,
                                                   double      first,
                                                   double      last,
                                                   String      segid,
                                                   int         degree,
                                                   int         n,
                                                   double[]    states,
                                                   double[]    epochs  )
      throws SpiceErrorException;



   /**
   Write a type 13 segment to an SPK file.
   */
   public native synchronized static void spkw13 ( int         handle,
                                                   int         body,
                                                   int         center,
                                                   String      frame,
                                                   double      first,
                                                   double      last,
                                                   String      segid,
                                                   int         degree,
                                                   int         n,
                                                   double[]    states,
                                                   double[]    epochs  )
      throws SpiceErrorException;



   /**
   Translate a surface ID code, together with a body ID code, to the
   corresponding surface name. If no such name exists, return a
   string representation of the surface ID code.
   */
   public native synchronized static void srfc2s ( int          code,
                                                   int          bodyid,
                                                   String[]     srfstr,
                                                   boolean[]    isname  )
      throws SpiceErrorException;



   /**
   Translate a surface ID code, together with a body string, to the
   corresponding surface name. If no such surface name exists,
   return a string representation of the surface ID code.
   */
   public native synchronized static void srfcss ( int          code,
                                                   String       bodstr,
                                                   String[]     srfstr,
                                                   boolean[]    isname  )
      throws SpiceErrorException;



   /**
   Map array of surface points on a specified target body to
   the corresponding unit length outward surface normal vectors.

   The surface of the target body may be represented by a triaxial
   ellipsoid or by topographic data provided by DSK files.
   */
   public native synchronized static double[][] srfnrm ( String       method,
                                                         String       target,
                                                         double       et,
                                                         String       fixref,
                                                         int          npts,
                                                         double[][]   srfpts )
      throws SpiceErrorException;



   /**
   Translate a surface string, together with a body string, to the
   corresponding surface ID code. The input strings may contain
   names or integer ID codes.
   */
   public native synchronized static void srfs2c ( String       srfstr,
                                                   String       bodstr,
                                                   int[]        code,
                                                   boolean[]    found  )
      throws SpiceErrorException;



   /**
   Translate a surface string, together with a body ID code, to the
   corresponding surface ID code. The input surface string may
   contain a name or an integer ID code.
   */
   public native synchronized static void srfscc ( String       srfstr,
                                                   int          bodyid,
                                                   int[]        code,
                                                   boolean[]    found  )
      throws SpiceErrorException;



   /**
   Correct a position vector for reception stellar aberration.
   */
   public native synchronized static double[] stelab( double[]    pobj,
                                                      double[]    vobs )
      throws SpiceErrorException;



   /**
   Correct a position vector for transmission stellar aberration.
   */
   public native synchronized static double[] stlabx( double[]    pobj,
                                                      double[]    vobs )
      throws SpiceErrorException;



   /**
   Retrieve the nth string from the kernel pool variable, where the
   string may be continued across several components of the kernel pool
   variable.
   */
   public native synchronized static void stpool ( String    name,
                                                   int       nth,
                                                   String    contin,
                                                   String[]  component,
                                                   boolean[] found      )
      throws SpiceErrorException,
             KernelVarNotFoundException;



   /**
   Convert a string representing an epoch to a double precision
   value representing the number of TDB seconds past the J2000
   epoch corresponding to the input epoch.
   */
   public native synchronized static double str2et ( String timeString )

      throws SpiceErrorException;


   /**
   Determine the coordinates of the sub-observer point on a target
   body at a particular epoch, optionally corrected for planetary
   (light time) and stellar aberration.  Also, return the epoch
   associated with the sub-observer point and the vector from
   the observer to the sub-observer point; this vector is expressed
   in the frame designated by `fixref'.
   */
   public native synchronized static void subpnt     ( String     method,
                                                       String     target,
                                                       double     et,
                                                       String     fixref,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       double[]   spoint,
                                                       double[]   trgepc,
                                                       double[]   srfvec  )

      throws SpiceErrorException;






   /**
   Compute the rectangular coordinates of the sub-observer point on a
   target body at a particular epoch, optionally corrected for light
   time and stellar aberration.  The target body's surface is
   represented by a triangular plate model contained in a type 2 DSK
   segment. Return the sub-observer point's coordinates expressed in the
   body-fixed frame associated with the target body.  Also, return the
   observer's altitude above the target body. 
   */
   public native synchronized static void subptPl02 ( int         handle,
                                                      int[]       dladsc,
                                                      String      method,
                                                      String      target,
                                                      double      et,
                                                      String      abcorr,
                                                      String      obsrvr,
                                                      double[]    spoint,
                                                      double[]    alt,
                                                      int[]       plateID  )
      throws SpiceErrorException;



   /**
   Determine the coordinates of the sub-solar point on a target
   body at a particular epoch, optionally corrected for planetary
   (light time) and stellar aberration.  Also, return the epoch
   associated with the sub-solar point and the vector from
   the observer to the sub-solar point; this vector is expressed
   in the frame designated by `fixref'.
   */
   public native synchronized static void subslr     ( String     method,
                                                       String     target,
                                                       double     et,
                                                       String     fixref,
                                                       String     abcorr,
                                                       String     obsrvr,
                                                       double[]   spoint,
                                                       double[]   trgepc,
                                                       double[]   srfvec  )
      throws SpiceErrorException;




   /**
   Compute the rectangular coordinates of the sub-solar point on a
   target body at a particular epoch, optionally corrected for light
   time and stellar aberration.  The target body's surface is
   represented by a triangular plate model contained in a type 2 DSK
   segment. Return the sub-solar point's coordinates expressed in the
   body-fixed frame associated with the target body.  Also, return the
   observer's distance from the sub-solar point.
   */
   public native synchronized static void subsolPl02 ( int         handle,
                                                       int[]       dladsc,
                                                       String      method,
                                                       String      target,
                                                       double      et,
                                                       String      abcorr,
                                                       String      obsrvr,
                                                       double[]    spoint,
                                                       double[]    alt,
                                                       int[]       plateID  )
      throws SpiceErrorException;





   /**
   Compute the outward pointing unit normal vector at a specified
   point on the surface of an ellipsoid.
   */
   public native synchronized static double[] surfnm ( double     a,
                                                       double     b,
                                                       double     c,
                                                       double[]   point   )
      throws SpiceErrorException;



   /**
   Determine the intersection of a line-of-sight vector with the
   surface of an ellipsoid.
   */
   public native synchronized static void surfpt    (  double[]   positn,
                                                       double[]   u,
                                                       double     a,
                                                       double     b,
                                                       double     c,
                                                       double[]   point,
                                                       boolean[]  found   )
      throws SpiceErrorException;


   /**
   Add a name to the list of agents to notify whenever a member of
   a list of kernel variables is updated.
   */
   public native synchronized static void swpool ( String    agent,
                                                   String[]  names )
      throws SpiceErrorException;


   /**
   Return the state transformation matrix from one frame to
   another at a specified epoch. The result is returned as a
   1x36 array.
   */
   public native synchronized static double[] sxform  (  String     from,
                                                         String     to,
                                                         double     et   )
      throws SpiceErrorException;



   /**
   Return the kernel pool size limitations.
   */
   public native synchronized static int szpool ( String  name )

      throws SpiceErrorException;



   /**
   Find terminator points on a target body. The terminator is the set
   of points of tangency on the target body of planes tangent to both
   this body and to a light source. The caller specifies half-planes,
   bounded by the illumination source center-target center vector, in
   which to search for terminator points.

   The terminator can be either umbral or penumbral. The umbral
   terminator is the boundary of the region on the target surface
   where no light from the source is visible. The penumbral
   terminator is the boundary of the region on the target surface
   where none of the light from the source is blocked by the target
   itself.

   The surface of the target body may be represented either by a
   triaxial ellipsoid or by topographic data. 
   */
   public native synchronized static void termpt ( String         method,
                                                   String         ilusrc,
                                                   String         target,
                                                   double         et,
                                                   String         fixref,
                                                   String         abcorr, 
                                                   String         corloc,
                                                   String         obsrvr,
                                                   double[]       refvec,
                                                   double         rolstp,
                                                   int            ncuts,
                                                   double         schstp,
                                                   double         soltol,
                                                   int            maxn,
                                                   int[]          npts,
                                                   double[][]     points,
                                                   double[]       epochs,
                                                   double[][]     trmvcs  )
      throws SpiceErrorException;



   /**
   This routine converts an input epoch represented in TDB seconds
   past the TDB epoch of J2000 to a character string formatted to
   the specifications of a user's format picture.
   */
   public native synchronized static String timout (  double     et,
                                                      String     picture )
      throws SpiceErrorException;



   /**
   This routine returns Toolkit version information.
   */
   public native synchronized static String tkvrsn ( String item )

      throws SpiceErrorException;


   /**
   Parse a time string; return the result as a double precision
   count of seconds past J2000 in an unspecified time system.
   Return an error message if the input string cannot be parsed.
   */
   public native synchronized static void tparse ( String     string,
                                                   double[]   sp2000,
                                                   String[]   errmsg )
      throws SpiceErrorException;


   /**
   Create a time format picture from an example time string.
   */
   public native synchronized static void tpictr ( String     sample,
                                                   String[]   pictur,
                                                   boolean[]  ok,
                                                   String[]   errmsg )
      throws SpiceErrorException;


   /**
   Set the lower bound of the range used to interpret a two-digit year.
   */
   public native synchronized static void tsetyr ( int   year)

      throws SpiceErrorException;


   /**
   Create a matrix that transforms vectors to a reference frame
   defined by two specified vectors.
   */
   public native synchronized static double[][]  twovec ( double[]    axdef,
                                                          int         indexa,
                                                          double[]    plndef,
                                                          int         indexp )
      throws SpiceErrorException;


   /**
   Return the number of seconds in a tropical year.
   */
   public native synchronized static double tyear ();



   /**
   Compute the unitized cross product of two double precision,
   3-dimensional vectors.
   */
   public native synchronized static double[]  ucrss ( double[] v1,
                                                       double[] v2 )
      throws SpiceErrorException;



   /**
   Convert between uniform time systems.
   */
   public native synchronized static double unitim ( double et,
                                                     String insys,
                                                     String outsys )
      throws SpiceErrorException;


   /**
   Unload a SPICE kernel.
   */
   public native synchronized static void unload ( String file )

      throws SpiceErrorException;


   /**
   Given a 3-vector, return a unitized version of the vector and the
   norm of the original vector.
   */
   public native synchronized static void unorm ( double[]   v1,
                                                  double[]   vout,
                                                  double[]   vmag  )
      throws SpiceErrorException;


   /**
   Compute the cross product of two double precision,
   3-dimensional vectors.
   */
   public native synchronized static double[]  vcrss ( double[] v1,
                                                       double[] v2 )
      throws SpiceErrorException;


   /**
   Compute the distance between two double precision,
   3-dimensional vectors.
   */
   public native synchronized static double vdist ( double[] v1,
                                                    double[] v2 )
      throws SpiceErrorException;



   /**
   Return a unit vector parallel to a given 3-dimensional vector.
   */
   public native synchronized static double[] vhat ( double[] v1 )

      throws SpiceErrorException;



   /**
   Compute the magnitude of a double precision, 3-dimensional vector.
   */
   public native synchronized static double vnorm ( double[] v1 )

      throws SpiceErrorException;



   /**
   Return the component of a vector perpendicular to a given
   3-dimensional vector.
   */
   public native synchronized static double[] vperp( double[] a,
                                                     double[] b )
      throws SpiceErrorException;



   /**
   Return the projection of a vector onto a given 3-dimensional vector.
   */
   public native synchronized static double[] vproj ( double[] a,
                                                      double[] b )
      throws SpiceErrorException;



   /**
   Return the orthogonal projection of a vector onto a given plane.
   */
   public native synchronized static double[] vprjp ( double[] vin,
                                                      double[] plane )
      throws SpiceErrorException;


   /**
   Return the pre-image in a given plane of the orthogonal projection
   of a vector onto a given plane.
   */
   public native synchronized static void     vprjpi ( double[]  vin,
                                                       double[]  projpl,
                                                       double[]  invpl,
                                                       double[]  vout,
                                                       boolean[] found )
      throws SpiceErrorException;


   /**
   Rotate a vector about a given 3-dimensional vector.
   */
   public native synchronized static double[] vrotv ( double[] v1,
                                                      double[] axis,
                                                      double   theta  )
      throws SpiceErrorException;


   /**
   Compute the angular separation between two double precision,
   3-dimensional vectors.
   */
   public native synchronized static double vsep ( double[] v1,
                                                   double[] v2 )
      throws SpiceErrorException;



   /**
   Compute the complement with respect to a specified interval
   of a CSPICE window.
   */
   public native synchronized static double[] wncomd ( double    left,
                                                       double    right,
                                                       double[]  window )
      throws SpiceErrorException;


   /**
   Contract each of the intervals of a CSPICE window.
   */
   public native synchronized static double[] wncond ( double    left,
                                                       double    right,
                                                       double[]  window )
      throws SpiceErrorException;



   /**
   Compute the difference of two CSPICE windows.
   */
   public native synchronized static double[] wndifd ( double[]  a,
                                                       double[]  b  )
      throws SpiceErrorException;


   /**
   Determine whether a point is contained in a specified
   CSPICE window.
   */
   public native synchronized static boolean wnelmd ( double    point,
                                                      double[]  window )
      throws SpiceErrorException;



   /**
   Expand each of the intervals of a CSPICE window.
   */
   public native synchronized static double[] wnexpd ( double    left,
                                                       double    right,
                                                       double[]  window )
      throws SpiceErrorException;


   /**
   Extract the left or right endpoints from a CSPICE window.
   */
   public native synchronized static double[] wnextd ( String    side,
                                                       double[]  window )
      throws SpiceErrorException;


   /**
   Fill small gaps between adjacent intervals of a CSPICE window.
   */
   public native synchronized static double[] wnfild ( double    small,
                                                       double[]  window )
      throws SpiceErrorException;


   /**
   Filter small intervals from a CSPICE window.
   */
   public native synchronized static double[] wnfltd ( double   small,
                                                       double[] window )
      throws SpiceErrorException;


   /**
   Determine whether an interval is included in a CSPICE window.
   */
   public native synchronized static boolean  wnincd ( double   left,
                                                       double   right,
                                                       double[] window )
      throws SpiceErrorException;


   /**
   Insert an interval into a CSPICE window.
   */
   public native synchronized static double[] wninsd ( double   left,
                                                       double   right,
                                                       double[] window )
      throws SpiceErrorException;


   /**
   Compute the intersection of two CSPICE windows.
   */
   public native synchronized static double[] wnintd ( double[]  a,
                                                       double[]  b  )
       throws SpiceErrorException;


   /**
   Compare two SPICE windows.
   */
   public native synchronized static boolean  wnreld ( double[] a,
                                                       String   op,
                                                       double[] b   )
      throws SpiceErrorException;


   /**
   Summarize the contents of a SPICE window.
   */
   public native synchronized static void  wnsumd ( double[] window,
                                                    double[] meas,
                                                    double[] avg,
                                                    double[] stddev,
                                                    int[]    shortest,
                                                    int[]    longest   )
       throws SpiceErrorException;


   /**
   Compute the union of two CSPICE windows.
   */
   public native synchronized static double[] wnunid ( double[]  a,
                                                       double[]  b  )
      throws SpiceErrorException;


   /**
   Validate a CSPICE window.
   */
   public native synchronized static double[] wnvald ( int      size,
                                                       int      card,
                                                       double[] endpoints )
      throws SpiceErrorException;


   /**
   Convert a state transformation matrix and a sequence of axes to a
   sequence of Euler angles and corresponding rates.
   */
   public native synchronized static void xf2eul ( double[]    xform,
                                                   int[]       axes,
                                                   double[]    angles,
                                                   boolean[]   unique  )
      throws SpiceErrorException;


   /**
   Convert a state transformation matrix and a sequence of axes to a
   sequence of Euler angles and corresponding rates.
   */
   public native synchronized static void xf2rav ( double[]    xform,
                                                   double[][]  r,
                                                   double[]    av  )
      throws SpiceErrorException;






   /**
   Compute a set of points on the umbral or penumbral terminator of a
   specified target body, where the target body's surface is
   represented by a triangular plate model contained in a type 2 DSK
   segment.
   */
   public native synchronized static void termPl02 ( int           handle,
                                                     int[]         dladsc,
                                                     String        trmtyp,
                                                     String        source,
                                                     String        target,
                                                     double        et,
                                                     String        fixfrm,
                                                     String        abcorr,
                                                     String        obsrvr,
                                                     int           npoints,
                                                     double[]      trgepc,
                                                     double[]      obspos,
                                                     double[][]    trmpts,
                                                     int[]         plateIDs )
      throws SpiceErrorException;




}

