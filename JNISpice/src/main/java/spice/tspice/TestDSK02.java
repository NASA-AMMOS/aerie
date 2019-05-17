
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import static spice.basic.DLA.*;
import static spice.basic.DLADescriptor.*;
import static spice.basic.DSK.*;
import static spice.basic.DSKDescriptor.*;
import static spice.basic.DSK02.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestDSK02 provides methods that implement test families for
DSK type 2 native methods of the class CSPICE.

<p>Version 1.0.0 29-DEC-2016 (NJB)
<pre>
   Class was formerly named

      TestDSK0

   Test method was formerly named
 
      f_DSK0

   Class was re-written to test new DSK writing APIs. Tests
   now are parallel to those in the TSPICE routine 

      f_dsk02.for
</pre>

<p> Replaces TestDSK0. Last version was 21-SEP-2010 (NJB).
*/
public class TestDSK02 extends Object
{

   //
   // Class constants
   //

   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test DSK type 2 APIs.
   */
   public static boolean f_DSK02()

      throws SpiceException
   {
      //
      // Constants
      //

      final String                      DSK0             =    "dsk02_test0.bds";
      final String                      DSK1             =    "dsk02_test1.bds";

      final double                      DPMAX            =    Double.MAX_VALUE;
      final double                      DPMIN            =   - DPMAX;
      final double                      TIGHT            =     1.e-12;
      final double                      VTIGHT           =     1.e-14;

      final int                         DTYPE            =     2;
      final int                         IBUFSZ           =     MAXCGR;
      final int                         MAXP             =     20000;
      final int                         MAXV             =     10000;
      final int                         VOXPSZ           =    100000;
      final int                         VOXNPL           =    200000;
      final int                         MXIXSZ           =   1000000;
      final int                         WORKSZ           =   1000000;
      final int                         DBUFSZ           =   3 * MAXV;
 
      //
      // Local variables
      //

      boolean[]                         foundArray = new boolean[1];
      boolean                           ok;


      double                            a;
      double                            b;
      double                            c;
      double[]                          corpar  = new double [ NSYPAR ];
      double[]                          dbuff   = new double [ DBUFSZ ];
      double[]                          dskdsc;
      double[]                          dtmpbf;
      double[]                          dtpbf2;
      double                            finscl;
      double                            first;
      double                            last;
      double[]                          mn32    = new double [1];
      double[]                          mn33    = new double [1];
      double                            mncor1;
      double                            mncor2;
      double[]                          mncor3  = new double [1];
      double[]                          mx32    = new double [1];
      double[]                          mx33    = new double [1];
      double                            mxcor1;
      double                            mxcor2;
      double[]                          mxcor3  = new double [1];
      double[]                          normal  = new double [3];
      double[][]                        ovtbds  = new double [3][2];
      double[]                          ovxori  = new double [3];
      double[]                          ovxsiz  = new double [1];
      double[]                          spaixd  = new double [ IXDFIX ];
      double[]                          spaxd2  = new double [ IXDFIX ];
      double[]                          spaxd3  = new double [ IXDFIX ];
      double[]                          tmpv1;
      double[]                          v1      = new double [3];
      double[]                          v2      = new double [3];
      double[]                          v3      = new double [3];
      double[]                          tmpv2;
      double[][]                        varray;
      double[][]                        verts   = new double [3][3]; 
      double[]                          vrtces  = new double [ 3 * MAXV ];
      double[]                          vrtcs2  = new double [ 3 * MAXV ];
      double[]                          vrtcs3  = new double [ 3 * MAXV ];   
      double[][]                        xformArr;
      double[]                          xnorml  = new double [3];
      double[]                          xunrml  = new double [3];
      double[][]                        work    = new double [ WORKSZ ][2];


      int                               addr;
      int                               bodyid;
      int                               corscl;
      int                               corsys;
      int                               dclass;
      int[]                             dlads2  = new int [ DLADSZ ];
      int[]                             dlads3  = new int [ DLADSZ ];
      int[]                             dladsc  = new int [ DLADSZ ]; 
      int                               framid;
      int                               handle;
      int                               han1;
      int                               i;
      int[]                             ibuff   = new int [ IBUFSZ ];
      int[]                             itmpbf;
      int                               j;
      int                               k;
      int                               n;
      int                               nlat;
      int                               nlon;
      int                               np;
      int                               np2;
      int[]                             npArr   = new int [1];
      int                               nv;
      int                               nv2;
      int[]                             nvArr   = new int [1]; 
      int                               nvxtot;
      int[]                             nxtdsc  = new int [ DLADSZ ];
      int[]                             nxtds2  = new int [ DLADSZ ];
      int[]                             ocrscl  = new int [1];
      int[]                             onp     = new int [1];
      int[]                             onv     = new int [1];
      int[]                             onvxtt  = new int [1];
      int[]                             ovgrxt  = new int [3];
      int[]                             ovlsiz  = new int [1];
      int[]                             ovpsiz  = new int [1];
      int[]                             ovtlsz  = new int [1];
      int[][]                           parray;
      int[]                             plate   = new int [3];
      int[]                             plates  = new int [ 3 * MAXP ];
      int[]                             plats2  = new int [ 3 * MAXP ];
      int[]                             spaixi  = new int [ MXIXSZ ];
      int[]                             spaxi2  = new int [ MXIXSZ ];
      int[]                             spaxi3  = new int [ MXIXSZ ];
      int                               spxisz;
      int                               surfid;
      int                               surf2;
      int[]                             vgrext  = new int [ 3 ];       
      int                               vpsiz3;
      int                               vpsize;
      int                               vlsize;
      int                               vlsiz3;
      int                               voxnpl;
      int                               voxnpt;
      int                               vtxnpl;
      int                               xncgr;
      int                               xnp;
      int                               xnv;

      String                            frame;
      String                            ifname;





      //
      //  We enclose all tests in a try/catch block in order to
      //  facilitate handling unexpected exceptions.  Unexpected
      //  exceptions are trapped by the catch block at the end of
      //  the routine; expected exceptions are handled locally by
      //  catch blocks associated with error handling test cases.
      //
      //  Therefore, JNISpice calls that are expected to succeed don't
      //  have any subsequent "chckxc" type calls following them, nor
      //  are they wrapped in in try/catch blocks.
      //
      //  Expected exceptions that are *not* thrown are tested
      //  via a call to {@link spice.testutils.Testutils#dogDidNotBark}.
      //

      try
      {

         JNITestutils.topen ( "f_DSK02" );




         // ***********************************************************
         //
         //    DSKMI2, DSKRB2, DSKW02 Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Create new Mars DSK files." );

         //
         // Delete the DSKs if they exist.
         //
         ( new File ( DSK0 ) ).delete();
         ( new File ( DSK1 ) ).delete();


 
         //
         //
         // Create vertices and plates.
         //
         // The Mars radii used here need not be consistent with
         // the current generic PCK.
         //
         a      =  3396.19;
         b      =  a;
         c      =  3376.20;

         nlon   = 20;
         nlat   = 10;

         JNITestutils.zzellplt ( a,     b,      c,     nlon,  nlat,
                                 nvArr, vrtces, npArr, plates      );
         //
         // Create a spatial index for the plate set.
         //
         //  Use a heuristic formula for the fine scale.
         //
         nv = nvArr[0];
         np = npArr[0];

      
         finscl = Math.max( 1.0,  ( Math.pow( np, 0.23 ) / 8 )  );

         //
         // Pick a one-size-fits-all value for the coarse scale.
         //
         corscl = 10;


         //
         // Set the spatial index integer component size.
         //
         vpsize = VOXPSZ;
         vlsize = VOXNPL;
         spxisz = MXIXSZ;

         //
         // Create a spatial index that includes a vertex-plate mapping.
         //
         CSPICE.dskmi2 ( nv,     vrtces, np,     plates, finscl,
                         corscl, WORKSZ, vpsize, vlsize, true,
                         spxisz, spaixd, spaixi                  );

         //
         // Generate bounds for the 3rd coordinate.
         //

         //
         // Populate the `corpar' array with bogus values. These are
         // needed for testing. They won't be used by SPICE routines
         // since the system is latitudinal.
         //
         for ( i = 0;  i < NSYPAR;  i++ )
         {
            corpar[i] = i+1;
         }

         CSPICE.dskrb2 ( nv,     vrtces, np,     plates, 
                         LATSYS, corpar, mncor3, mxcor3 );

         //
         // Set segment attribute inputs.
         //
         corsys  =  LATSYS;

         mncor1  =  0.0;
         mxcor1  =  2 * Math.PI;
         mncor2  = -Math.PI/2;
         mxcor2  =  Math.PI/2;

         first   = -CSPICE.jyear() / 100;
         last    =  CSPICE.jyear() / 100;

         dclass  =  2;
         bodyid  =  499;
         surfid  =  1;
         frame   =  new String( "IAU_MARS" );

         framid  =  CSPICE.namfrm ( frame );

         //
         // Write the file.
         //
         ifname = DSK0;

         handle = CSPICE.dskopn ( DSK0, ifname, 0 );

         CSPICE.dskw02 ( handle, bodyid,    surfid,    dclass, frame, 
                         corsys, corpar,    mncor1,    mxcor1, mncor2,
                         mxcor2, mncor3[0], mxcor3[0], first,  last,
                         nv,     vrtces,    np,        plates, spaixd,  
                         spaixi                                        );

         //
         // Create a second segment having higher resolution.
         //
         nlon = 30;
         nlat = 15;

         JNITestutils.zzellplt ( a,     b,      c,     nlon,  nlat,
                                 nvArr, vrtcs2, npArr, plats2      );
         nv2 = nvArr[0];
         np2 = npArr[0];

      
         finscl = Math.max( 1.0,  ( Math.pow( np2, 0.23 ) / 8 )  );
         corscl = 10;

         vpsize = VOXPSZ;
         vlsize = VOXNPL;
         spxisz = MXIXSZ;

         CSPICE.dskmi2 ( nv2,    vrtcs2, np,     plats2, finscl,
                         corscl, WORKSZ, vpsize, vlsize, true,
                         spxisz, spaxd2, spaxi2                  );

         CSPICE.dskrb2 ( nv2,    vrtcs2, np2,  plats2, 
                         LATSYS, corpar, mn32, mx32   );

         //
         // Set segment attribute inputs.
         //
         corsys  =  LATSYS;

         mncor1  =  0.0;
         mxcor1  =  2 * Math.PI;
         mncor2  = -Math.PI/2;
         mxcor2  =  Math.PI/2;

         first   = -CSPICE.jyear() / 100;
         last    =  CSPICE.jyear() / 100;

         dclass  =  2;
         bodyid  =  499;
         surf2   =  2;
         frame   =  new String( "IAU_MARS" );

         framid  =  CSPICE.namfrm ( frame );

         //
         // Write the file.
         //
         CSPICE.dskw02 ( handle, bodyid,  surf2,   dclass, frame, 
                         corsys, corpar,  mncor1,  mxcor1, mncor2,
                         mxcor2, mn32[0], mx32[0], first,  last,
                         nv2,    vrtcs2,  np2,     plats2, spaxd2,  
                         spaxi2                                    );
         //
         // Close the file.
         //
         CSPICE.dascls ( handle );


         //
         // 
         // Create a second DSK file containing data similar to that
         // of the first segment. We want the segment's DAS address
         // ranges to be identical to those of the first segment,
         // but both the integer and d.p. data to be different. To
         // achieve this, we'll rotate the vertices to a different
         // frame. We'll still label the frame as IAU_MARS.
         //
         // Let XFORM be a matrix that permutes the standard basis
         // vectors.
         //
         xformArr = new double[3][3];

         xformArr[0][1]  = 1.0;
         xformArr[1][2]  = 1.0;
         xformArr[2][0]  = 1.0;

         tmpv1 = new double[3];
         tmpv2 = new double[3];

         for ( i = 0;  i < nv;  i++ )
         {
            k = 3*i;

            System.arraycopy ( vrtces, k, tmpv1,  0, 3 );             
                      
            tmpv2 = CSPICE.mxv ( xformArr, tmpv1 );

            System.arraycopy ( tmpv2,  0, vrtcs3, k, 3 );             
         }

         CSPICE.dskmi2 ( nv,     vrtcs3, np,     plates, finscl,
                         corscl, WORKSZ, vpsize, vlsize, true,
                         spxisz, spaxd3, spaxi3                  );

         CSPICE.dskrb2 ( nv,     vrtcs3, np,   plates, 
                         LATSYS, corpar, mn33, mx33   );

         //
         // Open a second, new DSK file.
         //
         han1 = CSPICE.dskopn ( DSK1, DSK1, 0 );


         //
         // Write the segment.
         //
         CSPICE.dskw02 ( han1,   bodyid,  surfid,  dclass, frame, 
                         corsys, corpar,  mncor1,  mxcor1, mncor2,
                         mxcor2, mn33[0], mx33[0], first,  last,
                         nv,     vrtcs3,  np,      plates, spaxd3,  
                         spaxi3                                  );
 
         //
         // Close the file.
         //
         CSPICE.dascls ( han1 );


         //*********************************************************************
         //
         // DSKI02 tests
         //
         //*********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: check DSK segment's vertex and " +
                              "plate counts."                             );

         //
         // Open the first DSK file and locate the first segment.
         //
         handle = CSPICE.dasopr ( DSK0 );

         CSPICE.dlabfs ( handle, dladsc, foundArray );
         
         ok     = JNITestutils.chcksl ( "foundArray", foundArray[0], true );

         //
         // Fetch and check the vertex and plate counts.
         //
         xnv    = nv;
         xnp    = np;
 
         ibuff = CSPICE.dski02 ( handle, dladsc, KWNV, 0, 1 );

         ok    = JNITestutils.chcksi ( "nv", ibuff[0], "=", xnv, 0 );


         ibuff = CSPICE.dski02 ( handle, dladsc, KWNP, 0, 1 );

         ok    = JNITestutils.chcksi ( "np", ibuff[0], "=", xnp, 0 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check voxel grid extents." );

         ibuff  = CSPICE.dski02 ( handle, dladsc, KWVGRX, 0, 3 );

         itmpbf = new int[3];

         System.arraycopy ( spaixi, SIVGRX, itmpbf, 0, 3 );

         ok    = JNITestutils.chckai ( "vgrext", ibuff, "=", itmpbf );

         //
         // We'll use the total voxel count later.
         //
         nvxtot = itmpbf[0] * itmpbf[1] * itmpbf[2];


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check coarse voxel grid scale." );

         ibuff  = CSPICE.dski02 ( handle, dladsc, KWCGSC, 0, 1 );

         itmpbf = new int[1];

         System.arraycopy ( spaixi, SICGSC, itmpbf, 0, 1 );

         ok    = JNITestutils.chcksi ( "corscl", ibuff[0], "=", itmpbf[0], 0 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check voxel pointer count." );

         ibuff  = CSPICE.dski02 ( handle, dladsc, KWVXPS, 0, 1 );

         voxnpt = ibuff[0];

         itmpbf = new int[1];

         System.arraycopy ( spaixi, SIVXNP, itmpbf, 0, 1 );

         ok    = JNITestutils.chcksi ( "VOXNPT", voxnpt, "=", itmpbf[0], 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check voxel-plate correspondence " +
                              "size." );

         ibuff  = CSPICE.dski02 ( handle, dladsc, KWVXLS, 0, 1 );

         voxnpl = ibuff[0];

         itmpbf = new int[1];

         System.arraycopy ( spaixi, SIVXNL, itmpbf, 0, 1 );

         ok    = JNITestutils.chcksi ( "VOXNPL", voxnpl, "=", itmpbf[0], 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check vertex-plate correspondence " +
                              "size." );

         ibuff  = CSPICE.dski02 ( handle, dladsc, KWVTLS, 0, 1 );

         vtxnpl = ibuff[0];

         itmpbf = new int[1];

         System.arraycopy ( spaixi, SIVTNL, itmpbf, 0, 1 );

         ok    = JNITestutils.chcksi ( "VTXNPL", vtxnpl, "=", itmpbf[0], 0 );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check coarse grid." );

         ibuff = CSPICE.dski02 ( handle, dladsc, KWCGPT, 0, MAXCGR );

         //
         // Check the coarse grid size first.
         //
         xncgr  = nvxtot / (int) ( Math.pow(corscl, 3) );

         ok     = JNITestutils.chcksi ( "NCGR", ibuff.length, "=", xncgr, 0 );

         itmpbf = new int[xncgr];

         System.arraycopy ( spaixi, SICGRD, itmpbf, 0, xncgr );

         ok    = JNITestutils.chckai ( "CGRPTR", ibuff, "=", itmpbf );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check plates." );

         ibuff = CSPICE.dski02 ( handle, dladsc, KWPLAT, 0, 3*np );

         ok    = JNITestutils.chckai ( "plates", ibuff, "=", plates );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check voxel-plate pointer array." );

         ibuff  = CSPICE.dski02 ( handle, dladsc, KWVXPT, 0, vpsize );

         itmpbf = new int[vpsize];

         addr   = SICGRD + MAXCGR;

         System.arraycopy ( spaixi, addr, itmpbf, 0, vpsize );

         ok    = JNITestutils.chckai ( "VOXPTR", ibuff, "=", itmpbf );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check voxel-plate " + 
                              "correspondence list."          );

         ibuff  = CSPICE.dski02 ( handle, dladsc, KWVXPL, 0, voxnpl );


         itmpbf = new int[voxnpl];

         addr   = SICGRD + MAXCGR + voxnpt;
 
         System.arraycopy ( spaixi, addr, itmpbf, 0, voxnpl );

         ok    = JNITestutils.chckai ( "VOXLST", ibuff, "=", itmpbf );
        


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check vertex-plate pointer array." );

         ibuff  = CSPICE.dski02 ( handle, dladsc, KWVTPT, 0, nv );
  
         itmpbf = new int[nv];

         addr   = SICGRD + MAXCGR + voxnpt + voxnpl;

         System.arraycopy ( spaixi, addr, itmpbf, 0, nv );

         ok     = JNITestutils.chckai ( "VRTPTR", ibuff, "=", itmpbf );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: Check vertex-plate " +
                              "correspondence list." );

         ibuff  = CSPICE.dski02 ( handle, dladsc, KWVTPL, 0, vtxnpl );
 
         itmpbf = new int[vtxnpl];

         addr   = SICGRD + MAXCGR + voxnpt + voxnpl + nv;

         System.arraycopy ( spaixi, addr, itmpbf, 0, vtxnpl );

         ok    = JNITestutils.chckai ( "VRTLST", ibuff, "=", itmpbf );
 


         // 
         //
         // DSKIO2: the following cases exercise the logic for use 
         // of saved values.
         //
         // 


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: read from second segment " +
                              "of first file." );

         CSPICE.dlafns ( handle, dladsc, nxtdsc, foundArray );

         ok = JNITestutils.chcksl ( "found", foundArray[0], true );

         //
         // Fetch the vertex count. Compare against the value used
         // to create the second segment.
         //
         ibuff = CSPICE.dski02 ( handle, nxtdsc, KWNV, 0, 1 );

         ok = JNITestutils.chcksi ( "NV",  ibuff[0], "=", nv2, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: read from first segment " +
                              "of first file again." );

         //
         // This call resets the previous DLA descriptor to the first one of
         // the first file. This sets up the next test, which shows that
         // DSKI02 can detect a segment change when the DLA segment
         // descriptor start addresses match those of the previous segment,
         // but the handle changes.
         //

         //
         // Fetch the voxel size. Compare against the value from the
         // second segment's spatial index.
         //
         ibuff = CSPICE.dski02 ( handle, dladsc, KWNV, 0, 1 );

         ok = JNITestutils.chcksi ( "NV", ibuff[0], "=", nv, 0 );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: read from first segment " +
                              "of second file." );

         han1 = CSPICE.dasopr ( DSK1 );

         CSPICE.dlabfs ( han1, dlads3, foundArray );

         ok = JNITestutils.chcksl ( "found", foundArray[0], true );

         //
         // Check voxel-plate correspondence list. This is the call
         // to DSKI02 where the input handle changes. We use IBUFSZ
         // as the "room" argument so we don't need to look up the
         // list size. We want to look up the voxel-plate list at 
         // this point because this is an integer structure that
         // differs depending on which file we're reading.
         //        
         int[] voxls3 = CSPICE.dski02 ( han1, dlads3, KWVXPL, 0, IBUFSZ );

         //
         // We need to look up the size of the voxel-plate pointer
         // array in order to get the correct index of the voxel-plate
         // list in the spatial index. Compare against the value from the
         // second file's first segment's spatial index. Note the 
         // spatial index components have the suffix "3" since this is 
         // the third segment of the set we've created.
         //
         ibuff = CSPICE.dski02 ( han1, dlads3, KWVXPS, 0, 1 );

         ok    = JNITestutils.chcksi ( "VOXNPT",       ibuff[0], "=", 
                                       spaxi3[SIVXNP], 0             );

         vpsiz3 = ibuff[0];      

         //
         // Compare against the corresponding sub-array of the 
         // integer spatial index component.
         //
         addr  = SICGRD + MAXCGR + vpsiz3;

         itmpbf = new int[ voxls3.length ];

         System.arraycopy ( spaxi3, addr, itmpbf, 0, voxls3.length );

         ok    = JNITestutils.chckai ( "VOXLS3", voxls3, "=", itmpbf );
                                       
        
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKI02: read from first segment " +
                              "of first file yet again." );

         //
         // Fetch NV from the first segment of the first file.
         //
         ibuff = CSPICE.dski02 ( handle, dladsc, KWNV, 0, 1 );

         ok    = JNITestutils.chcksi ( "NV", ibuff[0], "=", nv, 0 );



         // ***********************************************************
         //
         //    DSKIO2 Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "DSKI02 error: invalid keyword." );

         try
         {
            ibuff  = CSPICE.dski02 ( handle, dladsc, -1, 0, 3 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(NOTSUPPORTED)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(NOTSUPPORTED)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "DSKI02 error: invalid room value." );



         try
         {
            ibuff  = CSPICE.dski02 ( handle, dladsc, KWPLAT, 0, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(VALUEOUTOFRANGE)", ex );
         }


         
         try
         {
            ibuff  = CSPICE.dski02 ( handle, dladsc, KWPLAT, 0, -1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(VALUEOUTOFRANGE)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "DSKI02 error: invalid start value." );



         try
         {
            ibuff  = CSPICE.dski02 ( handle, dladsc, KWPLAT, -1, 3*np );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(INDEXOUTOFRANGE)", ex );
         }







         //********************************************************************
         //
         // DSKD02 tests
         //
         //********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "DSKD02: check DSK descriptor." );


         dbuff = CSPICE.dskd02 ( handle, dladsc, KWDSC, 0, DSKDSZ );


         //
         // Check descriptor elements.
         //

         ok = JNITestutils.chcksi ( "BODYID", (int)dbuff[CTRIDX], "=", 
                                     bodyid,  0                       );
        
         ok = JNITestutils.chcksi ( "SURFID", (int)dbuff[SRFIDX], "=", 
                                     surfid,  0                       );        

         ok = JNITestutils.chcksi ( "FRAMID", (int)dbuff[FRMIDX], "=", 
                                     framid,  0                       );
        
         ok = JNITestutils.chcksi ( "DCLASS", (int)dbuff[CLSIDX], "=", 
                                     dclass,  0                       );
        
         ok = JNITestutils.chcksi ( "DTYPE",  (int)dbuff[TYPIDX], "=", 
                                     DTYPE,   0                       );
        
         ok = JNITestutils.chcksi ( "CORSYS", (int)dbuff[SYSIDX], "=", 
                                     corsys,  0                       );
        


         ok = JNITestutils.chcksd ( "MNCOR1", dbuff[MN1IDX], "=", 
                                     mncor1,  0.0                );
        
         ok = JNITestutils.chcksd ( "MXCOR1", dbuff[MX1IDX], "=", 
                                     mxcor1,  0.0                );
        

         ok = JNITestutils.chcksd ( "MNCOR2", dbuff[MN2IDX], "=", 
                                     mncor2,  0.0                );
        
         ok = JNITestutils.chcksd ( "MXCOR2", dbuff[MX2IDX], "=", 
                                     mxcor2,  0.0                );
        
         ok = JNITestutils.chcksd ( "MNCOR3",    dbuff[MN3IDX], "=", 
                                     mncor3[0],  0.0                );
        
         ok = JNITestutils.chcksd ( "MXCOR3",    dbuff[MX3IDX], "=", 
                                     mxcor3[0],  0.0                );

         //
         // Check coordinate parameters.
         //
         double[] parbuf = new double[NSYPAR];
           
         System.arraycopy ( dbuff, PARIDX, parbuf, 0, NSYPAR );

         ok = JNITestutils.chckad ( "CORPAR", parbuf, "=", corpar, 0.0 );
         
         //
         // Check time bounds.
         //
         ok = JNITestutils.chcksd ( "FIRST",  dbuff[BTMIDX], "=", 
                                    first,    0.0                );

         ok = JNITestutils.chcksd ( "LAST",   dbuff[ETMIDX], "=", 
                                    last,     0.0                );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKD02: check vertex bounds." );


         dbuff = CSPICE.dskd02 ( handle, dladsc, KWVTBD, 0, 6 );

         //
         // Extract the expected bounds from the d.p. component of the
         // spatial index.
         //
         double[] vtxbds = new double[6];

         System.arraycopy ( spaixd, SIVTBD, vtxbds, 0, 6 );
        

         ok = JNITestutils.chckad ( "VTXBDS", dbuff, "=", vtxbds, 0.0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKD02: check voxel origin" );


         dbuff = CSPICE.dskd02 ( handle, dladsc, KWVXOR, 0, 3 );

         //
         // Extract the expected bounds from the d.p. component of the
         // spatial index.
         //
         double[] voxori = new double[3];

         System.arraycopy ( spaixd, SIVXOR, voxori, 0, 3 );
        

         ok = JNITestutils.chckad ( "VOXORI", dbuff, "=", voxori, 0.0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKD02: check voxel size" );


         dbuff = CSPICE.dskd02 ( handle, dladsc, KWVXSZ, 0, 1 );


         ok = JNITestutils.chcksd ( "VOXSIZ",       dbuff[0], "=", 
                                    spaixd[SIVXSZ], 0.0           );



         // 
         //
         // DSKDO2: the following cases exercise the logic for use 
         // of saved values.
         //
         // 

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKD02: read from second segment " +
                              "of first file." );

         CSPICE.dlafns ( handle, dladsc, nxtdsc, foundArray );

         ok = JNITestutils.chcksl ( "found", foundArray[0], true );

         //
         // Fetch the voxel size. Compare against the value used
         // to create the second segment.
         //
         dbuff = CSPICE.dskd02 ( handle, nxtdsc, KWVXSZ, 0, 1 );

         ok = JNITestutils.chcksd ( "VOXSIZ",       dbuff[0], "=", 
                                    spaxd2[SIVXSZ], 0.0            );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKD02: read from first segment " +
                              "of second file." );

         CSPICE.dlabfs ( han1, dlads3, foundArray );

         ok = JNITestutils.chcksl ( "found", foundArray[0], true );

         //
         // Fetch the voxel size. Compare against the value used
         // to create the second segment.
         //
         dbuff = CSPICE.dskd02 ( han1, dlads3, KWVXSZ, 0, 1 );

         ok = JNITestutils.chcksd ( "VOXSIZ",       dbuff[0], "=", 
                                    spaxd3[SIVXSZ], 0.0            );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKD02: read from first segment " +
                              "of first file again." );

         CSPICE.dlabfs ( handle, dladsc, foundArray );

         ok = JNITestutils.chcksl ( "found", foundArray[0], true );

         //
         // Fetch the voxel size. Compare against the value used
         // to create the second segment.
         //
         dbuff = CSPICE.dskd02 ( handle, nxtdsc, KWVXSZ, 0, 1 );

         ok = JNITestutils.chcksd ( "VOXSIZ",       dbuff[0], "=", 
                                    spaxd2[SIVXSZ], 0.0            );



         // ***********************************************************
         //
         //    DSKDO2 Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "DSKD02 error: invalid keyword." );

         try
         {
            dbuff  = CSPICE.dskd02 ( handle, dladsc, -1, 0, 3 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(NOTSUPPORTED)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(NOTSUPPORTED)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "DSKD02 error: invalid room value." );



         try
         {
            dbuff  = CSPICE.dskd02 ( handle, dladsc, KWVERT, 0, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(VALUEOUTOFRANGE)", ex );
         }


         
         try
         {
            dbuff  = CSPICE.dskd02 ( handle, dladsc, KWVERT, 0, -1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(VALUEOUTOFRANGE)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "DSKD02 error: invalid start value." );



         try
         {
            dbuff  = CSPICE.dskd02 ( handle, dladsc, KWVERT, -1, 3*np );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(INDEXOUTOFRANGE)", ex );
         }






         //********************************************************************
         //
         // DSKB02 tests
         //
         //********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "DSKB02: check parameters from first " +
                               "segment of first file."                 );

         CSPICE.dlabfs ( handle, dladsc, foundArray );

         ok = JNITestutils.chcksl ( "found", foundArray[0], true );


         CSPICE.dskb02 ( handle, dladsc, onv,    onp,    onvxtt, ovtbds, 
                         ovxsiz, ovxori, ovgrxt, ocrscl, ovtlsz, ovpsiz,
                         ovlsiz                                         );

         //
         // Check the voxel grid extent out of order so it can be used to
         // check the total count.
         //
         itmpbf = new int [3];

         System.arraycopy ( spaixi, SIVGRX, itmpbf, 0, 3 );

         ok = JNITestutils.chckai ( "VGREXT", ovgrxt, "=", itmpbf );

         //
         // Check the total voxel count.
         // 
         j  = ovgrxt[0] * ovgrxt[1] * ovgrxt[2];

         ok = JNITestutils.chcksi ( "NVXTOT", onvxtt[0], "=", j, 0 ); 
      
         //
         // Check the vertex bounds. We need to transfer the bounds to
         // a 1-d array in order to check them.
         //
         dtmpbf = new double [6];
        
         System.arraycopy ( spaixd, SIVTBD, dtmpbf, 0, 6 );

         dtpbf2 = new double [6];

         System.arraycopy ( ovtbds[0], 0, dtpbf2, 0, 2 );
         System.arraycopy ( ovtbds[1], 0, dtpbf2, 2, 2 );
         System.arraycopy ( ovtbds[2], 0, dtpbf2, 4, 2 );

         ok = JNITestutils.chckad ( "VTXBDS", dtpbf2, "=", dtmpbf, 0.0 );

         //
         // Check the voxel origin.
         //
         dtmpbf = new double [3];
        
         System.arraycopy ( spaixd, SIVXOR, dtmpbf, 0, 3 );

         ok = JNITestutils.chckad ( "VOXORI", ovxori, "=", dtmpbf, 0.0 );

         //
         // Check the voxel size.
         // 
         ok = JNITestutils.chcksd ( "VOXSIZ", ovxsiz[0],      "=", 
                                              spaixd[SIVXSZ], 0.0 );   
         //
         // Check the coarse voxel scale.
         // 
         ok = JNITestutils.chcksi ( "CORSCL", ocrscl[0],      "=", 
                                              spaixi[SICGSC], 0 );   
         //
         // Check the vertex-plate list size.
         // 
         ok = JNITestutils.chcksi ( "VTXLSZ", ovtlsz[0],      "=", 
                                              spaixi[SIVTNL], 0 );       
         //
         // Check the voxel-plate pointer array size.
         //     
         ok = JNITestutils.chcksi ( "VOXPSZ", ovpsiz[0],      "=", 
                                              spaixi[SIVXNP], 0 );       
         //
         // Check the voxel-plate list size.
         // 
         ok = JNITestutils.chcksi ( "VOXLSZ", ovlsiz[0],      "=", 
                                              spaixi[SIVXNL], 0 );       
 


         //********************************************************************
         //
         // DSKZ02 tests
         //
         //********************************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "DSKZ02: check DSK segment's vertex and " +
                               "plate counts."                             );

         CSPICE.dlabfs ( handle, dladsc, foundArray );

         ok = JNITestutils.chcksl ( "found", foundArray[0], true );


         CSPICE.dskz02 ( handle, dladsc, nvArr, npArr );

         ok = JNITestutils.chcksi ( "NV", nvArr[0], "=", nv, 0 );
         ok = JNITestutils.chcksi ( "NP", npArr[0], "=", np, 0 );
 


         //********************************************************************
         //
         // DSKV02 tests
         //
         //********************************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKV02: get vertices from first segment " +
                              "of first file in one call."                );

         CSPICE.dlabfs ( handle, dladsc, foundArray );

         ok     = JNITestutils.chcksl ( "found", foundArray[0], true );
         
         //
         // Note vertex numbers are platform-independent and 1-based.
         //
         varray = CSPICE.dskv02 ( handle, dladsc, 1, nv );
          
         dbuff  = new double[ 3 * nv ];

         for ( j = 0, i = 0;  i < nv;  i++ )
         {
            System.arraycopy ( varray[i], 0, dbuff, j, 3 );

            j += 3;
         }

         ok     = JNITestutils.chckad ( "VRTCES", dbuff, "=", vrtces, 0.0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKV02: get vertices from first segment " +
                              "of first file one at a time."              );
         
         //
         // Note vertex numbers are platform-independent and 1-based.
         //

          
         dbuff  = new double[3*nv];

         for ( j = 0, i = 0;  i < nv;  i++ )
         {

            varray = CSPICE.dskv02 ( handle, dladsc, i+1, 1 );

            System.arraycopy ( varray[0], 0, dbuff, j, 3 );

            j += 3;
         }

         ok = JNITestutils.chckad ( "VRTCES", dbuff, "=", vrtces, 0.0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKV02 error: bad ROOM value." );

        
         try 
         {
            varray = CSPICE.dskv02 ( handle, dladsc, 1, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(VALUEOUTOFRANGE)", ex );
         }

         

         try 
         {
            varray = CSPICE.dskv02 ( handle, dladsc, 1, -1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(VALUEOUTOFRANGE)", ex );
         }

         


         //********************************************************************
         //
         // DSKP02 tests
         //
         //********************************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKP02: get plates from first segment " +
                              "of first file in one call."                );

         CSPICE.dlabfs ( handle, dladsc, foundArray );

         ok     = JNITestutils.chcksl ( "found", foundArray[0], true );
         
         //
         // Note vertex numbers are platform-independent and 1-based.
         //
         parray = CSPICE.dskp02 ( handle, dladsc, 1, np );
          
         ibuff  = new int[ 3 * np ];

         for ( j = 0, i = 0;  i < np;  i++ )
         {
            System.arraycopy ( parray[i], 0, ibuff, j, 3 );

            j += 3;
         }

         ok     = JNITestutils.chckai ( "PLATES", ibuff, "=", plates );

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKP02: get plates from first segment " +
                              "of first file one at a time."              );
         
         //
         // Note vertex numbers are platform-independent and 1-based.
         //

          
         ibuff  = new int[3*np];

         for ( j = 0, i = 0;  i < np;  i++ )
         {

            parray = CSPICE.dskp02 ( handle, dladsc, i+1, 1 );

            System.arraycopy ( parray[0], 0, ibuff, j, 3 );

            j += 3;
         }

         ok = JNITestutils.chckai ( "PLATES", ibuff, "=", plates );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKP02 error: bad ROOM value." );

        
         try 
         {
            parray = CSPICE.dskp02 ( handle, dladsc, 1, 0 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(VALUEOUTOFRANGE)", ex );
         }

         

         try 
         {
            parray = CSPICE.dskp02 ( handle, dladsc, 1, -1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(VALUEOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(VALUEOUTOFRANGE)", ex );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKP02 error: bad START value." );

        
         try 
         {
            parray = CSPICE.dskp02 ( handle, dladsc, 0, 1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(INDEXOUTOFRANGE)", ex );
         }


        try 
         {
            parray = CSPICE.dskp02 ( handle, dladsc, np+1, 1 );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(INDEXOUTOFRANGE)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true,  "SPICE(INDEXOUTOFRANGE)", ex );
         }

         //********************************************************************
         //
         // DSKN02 tests
         //
         //********************************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKN02: check normal vectors for all plates " +
                              "in first segment of first file."               );

         CSPICE.dlabfs ( handle, dladsc, foundArray );

         ok = JNITestutils.chcksl ( "found", foundArray[0], true );


         for ( i = 1;  i <= np;  i++ )
         {            
            //
            // Get the normal vector for the ith plate. Note plate indices
            // are 1-based.
            //
            normal = CSPICE.dskn02( handle, dladsc, i );

            //
            // Get the Ith plate; look up its vertices.
            //
            parray = CSPICE.dskp02 ( handle, dladsc, i, 1 ); 

            for ( j = 0;  j < 3;  j++ )
            {
               varray   = CSPICE.dskv02 ( handle, dladsc, parray[0][j], 1 );

               verts[j] = varray[0];
            }

            xnorml = CSPICE.pltnrm ( verts[0], verts[1], verts[2] );

            xunrml = CSPICE.vhat ( xnorml );

            String label = String.format ( "Normal vector for plate %d", i );
                                         
            ok = JNITestutils.chckad ( label, normal, "~~/", xunrml, TIGHT );
         }



         //********************************************************************
         //
         // DSKGD tests
         //
         //********************************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "DSKGD: check DSK descriptor of "  +
                              "first segment of first file."       );

         CSPICE.dlabfs ( handle, dladsc, foundArray );

         ok = JNITestutils.chcksl ( "found", foundArray[0], true );

         dskdsc = CSPICE.dskgd ( handle, dladsc );

         //
         // Check descriptor elements.
         //
         ok = JNITestutils.chcksi ( "bodyid", (int)dskdsc[CTRIDX], 
                                    "=",      bodyid,    0         );

         ok = JNITestutils.chcksi ( "surfid", (int)dskdsc[SRFIDX], 
                                    "=",      surfid,    0         );

         ok = JNITestutils.chcksi ( "framid", (int)dskdsc[FRMIDX], 
                                    "=",      framid,    0         );

         ok = JNITestutils.chcksi ( "dclass", (int)dskdsc[CLSIDX], 
                                    "=",      dclass,    0         );

         ok = JNITestutils.chcksi ( "dtype",  (int)dskdsc[TYPIDX], 
                                    "=",      DTYPE,     0         );

         ok = JNITestutils.chcksi ( "corsys", (int)dskdsc[SYSIDX], 
                                    "=",      LATSYS,    0         );

         ok = JNITestutils.chcksd ( "mncor1", dskdsc[MN1IDX], 
                                    "=",      mncor1,    0.0       );

         ok = JNITestutils.chcksd ( "mxcor1", dskdsc[MX1IDX], 
                                    "=",      mxcor1,    0.0       );

         ok = JNITestutils.chcksd ( "mncor2", dskdsc[MN2IDX], 
                                    "=",      mncor2,    0.0       );

         ok = JNITestutils.chcksd ( "mxcor2", dskdsc[MX2IDX], 
                                    "=",      mxcor2,    0.0       );

         ok = JNITestutils.chcksd ( "mncor3", dskdsc[MN3IDX], 
                                    "=",      mncor3[0], 0.0       );

         ok = JNITestutils.chcksd ( "mxcor3", dskdsc[MX3IDX], 
                                    "=",      mxcor3[0], 0.0       );

         //
         // Get copy of slice of `dsksdc' containing the coordinate
         // parameters. Note these are bogus values used only for
         // testing.
         //
         dbuff = new double[NSYPAR];

         System.arraycopy ( dskdsc, PARIDX, dbuff, 0, NSYPAR );
        
         ok = JNITestutils.chckad ( "corpar", dbuff, 
                                    "=",      corpar, 0.0 );


         ok = JNITestutils.chcksd ( "first",  dskdsc[BTMIDX], 
                                    "=",      first,     0.0       );

         ok = JNITestutils.chcksd ( "last",   dskdsc[ETMIDX], 
                                    "=",      last,      0.0       );



      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         ex.printStackTrace();

         ok = JNITestutils.chckth ( false, "", ex );
      }
 
      finally
      {

         //********************************************************************
         //
         // Clean up.
         //
         //********************************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Clean up: unload and delete DSK files." );

         //
         // Unload all kernels.
         //
         // CSPICE.kclear();


         //
         // Delete the DSKs if they exist.
         //

         ( new File ( DSK0 ) ).delete();
         ( new File ( DSK1 ) ).delete();
      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

