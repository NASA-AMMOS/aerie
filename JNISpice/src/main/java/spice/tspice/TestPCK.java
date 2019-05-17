
package spice.tspice;


import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import static spice.basic.AngularUnits.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;



/**
Class TestPCK provides methods that implement test families for
the class PCK.

<h3>Version 1.0.0 29-DEC-2016 (NJB)</h3>
*/
public class TestPCK extends Object
{

   //
   // Class constants
   //
   private static final String          DSK0          = "pcktest.bds";
   private static final String          PCK0          = "pcktest.bpc";
   private static final String          SPK0          = "pcktest.bsp";

   //
   // This value has been loosened for portability. It was set to 1.e-11
   // on cayenne.
   //
   private static final double          MEDTOL         = 1.e-10;

   //
   // Class variables
   //
   
   //
   // Methods
   //


   /**
   Evaluate an nth degree Chebyshev polynomial at argument theta.
   */
   private static double T( int n, double theta )
   {
      return( Math.cos( n*Math.acos( Math.min( 1.0, Math.max( -1.0,
                                                              theta ) ) ) ) );
   }




   /**
   Test PCK methods.
   */
   public static boolean f_PCK()

      throws SpiceException
   {
      //
      // Constants
      //
      final int                         MAXR   = 4;
      final int                         MAXSEG = 30;
      final int                         NCOEFF = 2;
      final int                         NFRM   = 3;
      final int                         NSTATE = 2;

      //
      // Local variables
      //
      AxisAndAngle                      axang;

      DAS                               das0;

      EulerAngles                       eulang;

      Matrix33                          qmat;
      Matrix33                          rmat;
      Matrix33                          xmat;

      PCK                               pck0;

      ReferenceFrame                    PCK4    = new ReferenceFrame( "PCK4"  );
      ReferenceFrame                    PCK5    = new ReferenceFrame( "PCK5"  );
      ReferenceFrame                    PCK6    = new ReferenceFrame( "PCK6"  );
      ReferenceFrame[]                  PCKfrm  = { PCK4, PCK5, PCK6 };

      ReferenceFrame                    basefr  = new ReferenceFrame( "J2000" );

      SpiceWindow                       cover;
      SpiceWindow[]                     xcovArr = new SpiceWindow[3];


      String[]                          frmbuf = { 

         "FRAME_PCK4                        = 1400004 ",
         "FRAME_1400004_NAME                = 'PCK4'  ",
         "FRAME_PCK4_CLASS                  = 2       ",
         "FRAME_PCK4_CLASS_ID               = 1400004 ",
         "FRAME_PCK4_CENTER                 = 4       ",
         "                                            ",
         "FRAME_PCK5                        = 1400005 ",
         "FRAME_1400005_NAME                = 'PCK5'  ",
         "FRAME_PCK5_CLASS                  = 2       ",
         "FRAME_PCK5_CLASS_ID               = 1400005 ",
         "FRAME_PCK5_CENTER                 = 4       ",
         "                                            ",
         "FRAME_PCK6                        = 1400006 ",
         "FRAME_1400006_NAME                = 'PCK6'  ",
         "FRAME_PCK6_CLASS                  = 2       ",
         "FRAME_PCK6_CLASS_ID               = 1400006 ",
         "FRAME_PCK6_CENTER                 = 4       "
                                                        };

      String                            label;
      String                            title;

      TDBTime                           et;

      boolean                           ok;
  
      double[]                          angles = new double[3];
      double[][][][][]                  coeffs = new
                                          double[NFRM][MAXSEG][MAXR][3][NCOEFF];

      double[]                          dvals;
      double                            etsec;
      double                            first;
      double                            intbeg;
      double                            intlen;
      double[]                          intval;
      double                            last;
      double                            t;
      double                            tol;
      double                            x;

      int[]                             axes   = { 3, 1, 3 };
      int                               degree;
      int[]                             clssid = { 1400004, 
                                                   1400005, 
                                                   1400006 };
      int                               h;
      int                               handle;
      int[]                             ids;
      int                               i;          
      int                               j;
      int                               k;
      int                               ncomc;
      int[]                             nrec = {  2,  3,  4 };
      int[]                             nseg = { 10, 20, 30 };
      int                               r;
      int                               s;
      int[]                             xids;



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

         JNITestutils.topen ( "f_PCK" );





         // ***********************************************************
         //
         //  PCK openNew error cases
         //
         // ***********************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Open existing PCK as a new file." );

         ( new File ( PCK0 ) ).delete();

         pck0 = PCK.openNew( PCK0, PCK0, 0 );

         try
         {
            pck0 = PCK.openNew( PCK0, PCK0, 0 );

            Testutils.dogDidNotBark ( "SPICE(FILEOPENFAIL)" );

            pck0.close();         
         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(FILEOPENFAIL)", exc );
         }

         pck0.close();                          


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Open new PCK with empty file name." );

         //
         // Open a new PCK file. First delete any existing file of
         // the name we want to use.
         //



         ( new File ( PCK0 ) ).delete();

         pck0 = null;

         try
         {
            pck0 = PCK.openNew(   "", "Type 3 PCK internal file name.",  4 );

            //
            //If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         if ( pck0 != null )
         {
            pck0.close();
         }

         ( new File ( PCK0 ) ).delete();



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Open new PCK with empty internal file name." );

         //
         // Open a new PCK file. First delete any existing file of
         // the name we want to use.
         //
         ( new File ( PCK0 ) ).delete();

         pck0 = null;

         try
         {
            pck0 = PCK.openNew(  PCK0, "",  4 );

            //
            //If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );

         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         if ( pck0 != null )
         {
            pck0.close();
         }

         ( new File ( PCK0 ) ).delete();

 
          

         //
         // --------Case-----------------------------------------------
         //

         // ***********************************************************
         //
         //  writeType02Segment tests
         //
         // ***********************************************************

         JNITestutils.tcase ( "Setup: create PCK." );

         //
         // Delete PCK0 if it exists. Create but do not load a new
         // version of the file.
         //
         ( new File ( PCK0 ) ).delete();


         ncomc = 0;

         pck0  = PCK.openNew( PCK0, PCK0, ncomc );

  
       
         //
         // Initialize coverage windows.
         //
         for ( i = 0;  i < NFRM;  i++  )
         {
            xcovArr[i] = new SpiceWindow();
         } 
                
        
         //
         // Create data for the new PCK.
         //
         for ( i = 0;  i < NFRM;  i++  )
         {
            for ( j = 0;  j < nseg[i];  j++  )
            {
               //
               // Create segments for frame I.
               //
               if ( i == 0  )
               {
                  //
                  // Create nseg[0] segments, each one separated by a 1 
                  // second gap.
                  //            
                  first = (j-1) * 11.0; 
                  last  = first + 10.0; 
               } 
               else if ( i == 1 )
               {
                  //
                  // Create nseg[1] segments, each one separated
                  // by a 1 second gap.  This time, create the 
                  // segments in decreasing time order.
                  //
                  first = ( nseg[1] - j ) * 101.0;
                  last  = first + 100.0;
               }
               else
               {
                  //
                  // i == 3
                  //
                  // Create nseg[2] segments with no gaps.
                  //
                  first = (j-1) * 1000.0;
                  last  = first + 1000.0;
               }

               //
               // Add to the expected coverage window for this frame.
               //       
               xcovArr[i].insert( first, last );


               //
               // Create a coefficient set for each record.
               //
              
               for ( r = 0;  r < nrec[i];  r++ )
               {
                  x = 100*i + 5*j + r;

                  coeffs[i][j][r][0][0] = 11.0 + x;
                  coeffs[i][j][r][0][1] = 21.0 + x;

                  coeffs[i][j][r][1][0] = 12.0 + x;
                  coeffs[i][j][r][1][1] = 22.0 + x;

                  coeffs[i][j][r][2][0] = 13.0 + x;
                  coeffs[i][j][r][2][1] = 23.0 + x;
               }
                
               
               degree = 1;
               intlen = (last-first) / nrec[i];

               //
               // Write the Jth segment for the Ith frame.
               //
               pck0.writeType02Segment( clssid[i], 
                                        basefr, 
                                        new TDBTime( first ), 
                                        new TDBTime( last  ),
                                        "TEST",    
                                        new TDBDuration( intlen ),  
                                        nrec[i],  
                                        degree,
                                        coeffs[i][j],    
                                        new TDBTime( first )      );
            }

         }

         pck0.close();  


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Check orientation from segments." );

         //
         // Load PCK frame specifications associated with the PCK 
         // segments.
         //
         KernelPool.loadFromBuffer( frmbuf );

         //
         // Check each PCK segment.
         //

         // 
         // Open the PCK for read access, and also
         // load the PCK.
         //
         pck0 = PCK.openForRead ( PCK0 );

         KernelDatabase.load    ( PCK0 );

         for ( i = 0;  i < NFRM;  i++ )
         {
            //
            // Get the time bounds for the ith segment.
            //
            cover  = pck0.getCoverage( clssid[i] );

            //
            // Loop over segments for the ith frame.
            //
            for ( j = 0;  j < nseg[i];  j++ )
            {

               if ( i == 0 )
               {
                  //
                  // The coverage of the segments lies in
                  // disjoint intervals.
                  //
                  intval = cover.getInterval( j );            
                  first  = intval[0];
                  last   = intval[1];
               }
               else if ( i == 1 )
               {
                  //
                  // The coverage of the segments lies in
                  // disjoint intervals. The intervals are ordered
                  // in inverse relation to the order of the segments.
                  //
                  intval = cover.getInterval( nseg[i]-(j+1) );            
                  first  = intval[0];
                  last   = intval[1];
               }
                  else
               {
                  //
                  // i == 2
                  //
                  // Coverage is continuous; compute the segment bounds
                  // explicitly.
                  //
                  first = (j-1) * 1000.0;
                  last  = first + 1000.0;
               }

               //
               // Loop over all records in the current segment.
               //

               intlen = (last-first) / nrec[i];

               for ( r = 0;  r < nrec[i];  r++ )
               {

                  //
                  // Pick a request time.
                  //
                  etsec  = first + r*intlen + intlen/3;
                  et     = new TDBTime( etsec );

                  //
                  // Compute an expected orientation value for the
                  // ith PCK frame, relative to J2000, at `et.'
                  //
                  xmat = basefr.getPositionTransformation( PCKfrm[i], et );

                  //
                  // Now compute orientation directly from the Cheby
                  // coefficients.
                  //

                  //
                  // Let t be the time argument on the interval [-1, 1].
                  //
            
                  intbeg = first + r*intlen;

                  t = -1.0 + ( 2 * (etsec-intbeg) / intlen );
            
                  for ( k = 0;  k < 3;  k++ )
                  {
          
                     angles[k] = 0.0;

                     for ( h = 0;  h < NCOEFF;  h++ )
                     {
                        angles[k] +=  coeffs[i][j][r][k][h] * T(h,t);
                     }
                  }
               
                  eulang = new

                     EulerAngles( angles[2], angles[1], angles[0], 3, 1, 3 );
 
                  rmat   = eulang.toMatrix();

         

                  qmat   = rmat.mxm( xmat.xpose() );

                  axang  = new AxisAndAngle( qmat );

                  label  = String.format( "Angular error for frame %s, " +
                                          "segment %s, at time %f",
                                          clssid[i], j, etsec      );

                  tol = MEDTOL;

                  ok = JNITestutils.chcksd( label, axang.getAngle(), 
                                            "~",   0.0,  tol         );       
               }         
            }
         }


         //
         // We're done with the PCK, as far as the KEEPER subsystem
         // is concerned.
         //
         KernelDatabase.unload( PCK0 );        

         //
         // Close the PCK to support upcoming error tests.
         //
         pck0.close();



         // ***********************************************************
         //
         //  writeType02Segment error cases
         //
         // ***********************************************************

  
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Write segment using stale PCK instance" );

         try
         {
               first  = 0.0;
               last   = 1.0;
               degree = 1;
               intlen = last-first;

               pck0.writeType02Segment( clssid[0], 
                                        basefr, 
                                        new TDBTime( first ), 
                                        new TDBTime( last  ),
                                        "TEST",    
                                        new TDBDuration( intlen ),  
                                        nrec[0],  
                                        degree,
                                        coeffs[0][0],    
                                        new TDBTime( first )      );

            Testutils.dogDidNotBark ( "SPICE(DAFNOSUCHHANDLE)" );
         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(DAFNOSUCHHANDLE)", exc );
         }


         // ***********************************************************
         //
         //  PCK openForRead error cases
         //
         // ***********************************************************
 
   
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Open non-existent PCK for read access." );

         try
         {
            pck0 = PCK.openForRead( "bogus.tpc" );

            Testutils.dogDidNotBark ( "SPICE(FILENOTFOUND)" );

         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(FILENOTFOUND)", exc );      
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Open SPK file for read access as a PCK file." );

         //
         // Create SPK file.
         //
         handle = JNITestutils.tstspk( SPK0, false );

         try
         {
            
            pck0 = PCK.openForRead( SPK0 );

            Testutils.dogDidNotBark ( "SPICE(FILEISNOTAPCK)" );

         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(FILEISNOTAPCK)", exc );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Open DSK file for read access as a PCK file." );

         //
         // Create DSK file.
         //
         JNITestutils.t_smldsk( 499, 499, "IAU_MARS", DSK0 );

         try
         {
            
            pck0 = PCK.openForRead( DSK0 );

            Testutils.dogDidNotBark ( "SPICE(FILARCHMISMATCH)" );

         }
         catch ( SpiceException exc )
         {

            //exc.printStackTrace();

            ok = JNITestutils.chckth( true, "SPICE(FILARCHMISMATCH)", exc );
         }




         


         // ***********************************************************
         //
         //    getCoverage( clssid ) tests
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Open PCK for read access." );

         pck0 = PCK.openForRead( PCK0 );


         //
         // Find the available coverage in PCK for the frames in the
         // frame array.
         //

         for ( i = 0;  i < NFRM;  i++  )
         {

            //
            // --------Case-----------------------------------------------
            //

            title = String.format( "Check time coverage for frame %d.", i );

            JNITestutils.tcase ( title );

            //
            // In this test, we use the coverage method that doesn't
            // use an input coverage window.
            //
            cover = pck0.getCoverage( clssid[i] );

            //
            // Check cardinality of coverage window. 
            //
            ok = JNITestutils.chcksi( "cover.card()", cover.card(), 
                                      "=",            xcovArr[i].card(), 0 );

            //
            // Check the contents of the coverage window.
            //
            tol = 0.0;

            ok  = JNITestutils.chckad( "cover", cover.toArray(), 
                                       "=",     xcovArr[i].toArray(), tol );

           /*
           System.out.println( "========================" );
           System.out.format( "Coverage for frame %d: ", i );

           System.out.println( cover );
           */

         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "getCoverage: get coverage for an ID not " +
                              "present in the PCK."                        ); 

         //
         // We should get an empty SpiceWindow.
         //
         cover = pck0.getCoverage( -1 );

         ok    = JNITestutils.chcksi( "cover.card()", cover.card(), "=", 0, 0 );



         // ***********************************************************
         //
         //    getCoverage( clssid, cover ) tests
         //
         // ***********************************************************

         //
         // Find the available coverage in PCK for the frames in the
         // frame array.  This time, start with a non-empty coverage window.
         //

         for ( i = 0;  i < NFRM;  i++  )
         {
            //
            // --------Case-----------------------------------------------
            //

            title = String.format( "Check time coverage for frame %d. "    +
                                   "Create coverage window starting with " + 
                                   "non-empty window.", i                   );

            JNITestutils.tcase ( title );

            //
            // In this test, we put an interval into cover before using it.
            //
            cover = new SpiceWindow();

            cover.insert( 1.e6, 1.e7 );        

            cover = pck0.getCoverage( clssid[i], cover );

            //
            // Update the expected coverage window as well.
            //
            xcovArr[i].insert( 1.e6, 1.e7 );        

            //
            // Check cardinality of coverage window. 
            //
            ok = JNITestutils.chcksi( "cover.card()", cover.card(), 
                                      "=",            xcovArr[i].card(), 0 );

            //
            // Check the contents of the coverage window.
            //
            tol = 0.0;

            ok  = JNITestutils.chckad( "cover", cover.toArray(), 
                                       "=",     xcovArr[i].toArray(), tol );



            //System.out.println( "cover.card: " + cover.card() );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "getCoverage( clssid, cover): get " +
                              "coverage for an ID not "           +
                              "present in the PCK."                  ); 

         //
         // We should get a SpiceWindow containing only the initial values..
         //

         
         cover = new SpiceWindow();
         
         cover.insert( 1.e6, 1.e7 );        

         cover = pck0.getCoverage( -1, cover );

         ok    = JNITestutils.chcksi( "cover.card()", cover.card(), "=", 1, 0 );






         // ***********************************************************
         //
         //    getCoverage( clssid ) error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "getCoverage(clssid): Try to get coverage " + 
                              "from stale PCK instance."                   ); 

         pck0.close();

         try
         {
            cover = pck0.getCoverage( clssid[0] );

            Testutils.dogDidNotBark( "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException exc )
         {

            JNITestutils.chckth( true, "SPICE(DAFNOTREADABLE)", exc );
         }






         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Open PCK for read access." );

         pck0 = PCK.openForRead( PCK0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "getCoverage(clssid,cover): Try to get " + 
                              "coverage from stale PCK instance."       ); 

         pck0.close();

         try
         {
            cover = pck0.getCoverage( clssid[0], cover );

            Testutils.dogDidNotBark( "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException exc )
         {

            JNITestutils.chckth( true, "SPICE(DAFNOTREADABLE)", exc );
         }

 

         // ***********************************************************
         //
         //    getFrameClassIDs tests
         //
         // ***********************************************************

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Re-open PCK for read access." );

         pck0 = PCK.openForRead( PCK0 );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase( "Find the frames in our test PCK." );


         ids = pck0.getFrameClassIDs();

         //
         // Check ID set.
         //
         ok = JNITestutils.chckai( "ids", ids, "=", clssid );

        

         // ***********************************************************
         //
         //    getFrameClassIDs() error cases
         //
         // ***********************************************************

         JNITestutils.tcase ( "getFrameClassIDs(clssid): Try to get  " + 
                              "coverage from stale PCK instance."        ); 

         pck0.close();

         try
         {
            ids = pck0.getFrameClassIDs();

            Testutils.dogDidNotBark( "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException exc )
         {

            JNITestutils.chckth( true, "SPICE(DAFNOTREADABLE)", exc );
         }






         // ***********************************************************
         //
         //    getFrameClassIDs(ids) tests
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase( "Re-open PCK for read access." );

         pck0 = PCK.openForRead( PCK0 );

         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase( "Find the frames in our test PCK. " +
                             "Start with a non-empty ID set."      );


         ids     = new int[2];
         ids[0]  = -200000;
         ids[1]  = -100000;

         xids    = new int[ NFRM + 2 ];
         xids[0] = ids[0];
         xids[1] = ids[1];

         System.arraycopy( clssid, 0, xids, 2, NFRM );

         ids = pck0.getFrameClassIDs( ids );

         //
         // Check ID set.
         //
         ok = JNITestutils.chckai( "ids", ids, "=", xids );

          

                
         // ***********************************************************
         //
         //    getFrameClassIDs() error cases
         //
         // ***********************************************************

         JNITestutils.tcase ( "getFrameClassIDs(clssid): Try to get  " + 
                              "coverage from stale PCK instance."        ); 

         pck0.close();

         try
         {
            ids = pck0.getFrameClassIDs( ids );

            Testutils.dogDidNotBark( "SPICE(DAFNOTREADABLE)" );

         }
         catch ( SpiceException exc )
         {

            JNITestutils.chckth( true, "SPICE(DAFNOTREADABLE)", exc );
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
         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Clean up." );

         KernelDatabase.clear();

         ( new File ( PCK0 ) ).delete();
         ( new File ( DSK0 ) ).delete();
         ( new File ( PCK0 ) ).delete();
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

