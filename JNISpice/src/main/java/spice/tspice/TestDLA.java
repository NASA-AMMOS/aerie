
package spice.tspice;


import java.io.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestDLA provides methods that implement test families for
class DLA.

<h3>Version 1.0.0 09-JAN-2017 (NJB)</h3>

*/
public class TestDLA extends Object
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
   Test class DSK APIs.
   */
   public static boolean f_DLA()

      throws SpiceException
   {
      //
      // Constants
      //

      final String                      DSK0             =    "dla_test0.bds";
      final String                      DSK1             =    "dla_test1.bds";
      final String                      SPK0             =    "spk_test0.bsp";
 
      //
      // Local variables
      //
      Body[]                            bodies;
      Body                              body;

      DLA                               dla     = null;
      DLA                               dla1    = null;
      DLA                               dla2    = null;

      DLADescriptor                     dladsc;
      DLADescriptor                     nxtdsc;
      DLADescriptor                     prvdsc;

      DSK                               dsk     = null;
      DSK                               dsk1    = null;

      DSKDescriptor                     dskdsc;

      ReferenceFrame                    frame;
      ReferenceFrame                    frame1;
 
      String                            label;

      boolean                           found;
      boolean                           ok;
  
      int                               bodyid;
      int                               framid;
      int                               handle;
      int                               i;
      int                               j;
      int                               nfound;
      int                               nseg;
      int                               nseg1;
      int                               surfid;

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

         JNITestutils.topen ( "f_DLA" );




         // ***********************************************************
         //
         //      Normal cases
         //
         // ***********************************************************


         //
         // Test constructors.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Call no-arguments constructor." );

         dla = new DLA();

         //
         // All we expect to be set in this object are false values
         // for the "readable" and "writable" flags.
         //
         ok = JNITestutils.chcksl( "readable", dla.isReadable(), false );
         ok = JNITestutils.chcksl( "writable", dla.isWritable(), false );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Setup: create two DSK files having multiple " + 
                              "segments."                                     );


         //
         // Delete the DSKs and SPK if they exist.
         //

         ( new File ( DSK0 ) ).delete();
         ( new File ( DSK1 ) ).delete();
         ( new File ( SPK0 ) ).delete();


         nseg   = 100;

         bodyid = 499;
         frame  = new ReferenceFrame( "IAU_MARS" );

         for ( i = 0;  i < nseg;  i++ )
         {
            surfid = i;

            JNITestutils.t_smldsk( bodyid, surfid, frame.getName(), DSK0 ); 
         }


         nseg1  = 200;

         bodyid = 399;
         frame1 = new ReferenceFrame( "IAU_EARTH" );

         for ( i = 0;  i < nseg1;  i++ )
         {
            surfid = -i;

            JNITestutils.t_smldsk( bodyid, surfid, frame1.getName(), DSK1 ); 
         }


         //
         // Test methods.
         // 


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get the segment count for DSK0." );

         dla = DLA.openForRead ( DSK0 );

         i   = dla.getSegmentCount();

         ok  = JNITestutils.chcksi ( "count", i, "=", nseg, 0 ); 


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get the segment count for DSK1." );

         dla1 = DLA.openForRead ( DSK1 );

         i    = dla1.getSegmentCount();

         ok   = JNITestutils.chcksi ( "count", i, "=", nseg1, 0 ); 


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Traverse DSK0 in forward order." );



         //
         // We'll need to treat this DLA file as a DSK, because we
         // need to examine the segments' DSK descriptors. So, obtain 
         // a DSK instance from from the DLA instance.
         //
         dsk = new DSK( dla );

         //
         // Start the search. We expect to find `nseg' segments.
         //
         nfound = 0;

         dladsc = dla.beginForwardSearch();

         //
         // Note that an exception will be thrown if the DLA file does
         // not contain at least one segment.
         //

         found  = true;

         while ( found )
         {
            ++ nfound;

            dskdsc = dsk.getDSKDescriptor( dladsc );

            surfid = dskdsc.getSurfaceID();

            //
            // Check the surface ID of the current segment. The surface
            // IDs of the segments are distinct.
            //
            label = String.format( "Surface ID for segment %d", nfound-1 );

            ok    = JNITestutils.chcksi( label, surfid, "=", nfound-1, 0 );

            //System.out.println( surfid );

            //
            // Check the body ID of the current segment.
            //
            label  = String.format( "Body ID for segment %d", nfound-1 );

            bodyid = dskdsc.getCenterID();

            ok     = JNITestutils.chcksi( label, bodyid, "=", 499, 0 );

            //
            // Check the frame ID of the current segment.
            //
            label  = String.format( "Frame ID for segment %d", nfound-1 );

            framid = dskdsc.getFrameID();

            ok     = JNITestutils.chcksi( label, framid, "=", 
                                                 frame.getIDCode(), 0 );



            found  = dla.hasNext( dladsc );

            if ( found )
            {
               dladsc = dla.getNext( dladsc );
            }
         }
         
         //
         // Check the segment count.
         //
         ok = JNITestutils.chcksi( "nfound", nfound, "=", nseg, 0 );






         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Leave DSK0 open; traverse DSK1 in " + 
                              "forward order."                       );


         //
         // We'll need to treat this DLA file as a DSK, because we
         // need to examine the segments' DSK descriptors. So, obtain 
         // a DSK instance from from the DLA instance.
         //
         dsk1 = new DSK( dla1 );

         //
         // Start the search. We expect to find `nseg' segments.
         //
         nfound = 0;

         dladsc = dla1.beginForwardSearch();

         //
         // Note that an exception will be thrown if the DLA file does
         // not contain at least one segment.
         //

         found  = true;

         while ( found )
         {
            ++ nfound;

            dskdsc = dsk1.getDSKDescriptor( dladsc );

            surfid = dskdsc.getSurfaceID();

            //
            // Check the surface ID of the current segment.
            //
            label = String.format( "Surface ID for segment %d", nfound-1 );

            ok    = JNITestutils.chcksi( label, surfid, "=", 1-nfound, 0 );

            //System.out.println( surfid );


            //
            // Check the body ID of the current segment.
            //
            label  = String.format( "Body ID for segment %d", nfound-1 );

            bodyid = dskdsc.getCenterID();

            ok     = JNITestutils.chcksi( label, bodyid, "=", 399, 0 );

            //
            // Check the frame ID of the current segment.
            //
            label  = String.format( "Frame ID for segment %d", nfound-1 );

            framid = dskdsc.getFrameID();

            ok     = JNITestutils.chcksi( label, framid, "=", 
                                                 frame1.getIDCode(), 0 );


            found  = dla1.hasNext( dladsc );

            if ( found )
            {
               dladsc = dla1.getNext( dladsc );
            }
         }
         
         //
         // Check the segment count.
         //
         ok = JNITestutils.chcksi( "nfound", nfound, "=", nseg1, 0 );





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Traverse DSK0 in backward order." );


         //
         // Start the search. We expect to find `nseg' segments.
         //
         nfound = 0;

         dladsc = dla.beginBackwardSearch();

         //
         // Note that an exception will be thrown if the DLA file does
         // not contain at least one segment.
         //

         found  = true;

         while ( found )
         {
            ++ nfound;

            dskdsc = dsk.getDSKDescriptor( dladsc );

            surfid = dskdsc.getSurfaceID();

            //
            // Check the surface ID of the current segment. The surface
            // IDs of the segments are distinct.
            //
            label = String.format( "Surface ID for segment %d", nseg-1 );

            ok    = JNITestutils.chcksi( label, surfid, "=", nseg-nfound, 0 );

            //System.out.println( surfid );

            //
            // Check the body ID of the current segment.
            //
            label  = String.format( "Body ID for segment %d", nfound-1 );

            bodyid = dskdsc.getCenterID();

            ok     = JNITestutils.chcksi( label, bodyid, "=", 499, 0 );

            //
            // Check the frame ID of the current segment.
            //
            label  = String.format( "Frame ID for segment %d", nfound-1 );

            framid = dskdsc.getFrameID();

            ok     = JNITestutils.chcksi( label, framid, "=", 
                                                 frame.getIDCode(), 0 );



            found  = dla.hasPrevious( dladsc );

            if ( found )
            {
               dladsc = dla.getPrevious( dladsc );
            }
         }
         
         //
         // Check the segment count.
         //
         ok = JNITestutils.chcksi( "nfound", nfound, "=", nseg, 0 );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Leave DSK0 open; traverse DSK1 in " + 
                              "backward order."                       );

        

         //
         // Start the search. We expect to find `nseg' segments.
         //
         nfound = 0;

         dladsc = dla1.beginBackwardSearch();

         //
         // Note that an exception will be thrown if the DLA file does
         // not contain at least one segment.
         //

         found  = true;

         while ( found )
         {
            ++ nfound;

            dskdsc = dsk1.getDSKDescriptor( dladsc );

            surfid = dskdsc.getSurfaceID();

            //
            // Check the surface ID of the current segment.
            //
            label = String.format( "Surface ID for segment %d", nfound-1 );

            ok    = JNITestutils.chcksi( label, surfid, "=", nfound-nseg1, 0 );

            //System.out.println( surfid );


            //
            // Check the body ID of the current segment.
            //
            label  = String.format( "Body ID for segment %d", nfound-1 );

            bodyid = dskdsc.getCenterID();

            ok     = JNITestutils.chcksi( label, bodyid, "=", 399, 0 );

            //
            // Check the frame ID of the current segment.
            //
            label  = String.format( "Frame ID for segment %d", nfound-1 );

            framid = dskdsc.getFrameID();

            ok     = JNITestutils.chcksi( label, framid, "=", 
                                                 frame1.getIDCode(), 0 );


            found  = dla1.hasPrevious( dladsc );

            if ( found )
            {
               dladsc = dla1.getPrevious( dladsc );
            }
         }
         
         //
         // Check the segment count.
         //
         ok = JNITestutils.chcksi( "nfound", nfound, "=", nseg1, 0 );



         // ***********************************************************
         //
         //      Error cases
         //
         // ***********************************************************


      
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Constructor error: try to create DLA from " +
                              "non-DLA file."                               );

         handle = JNITestutils.tstspk( SPK0, false );

         try
         {
            dla2 = DLA.openForRead( SPK0 );

            Testutils.dogDidNotBark( "SPICE(FILARCHMISMATCH)" );

         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth ( true, "SPICE(FILARCHMISMATCH)", exc );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "getNext error: try to get successor of " +
                              "last segment."                             );

         dladsc = dla.beginBackwardSearch();

         try
         {
            dladsc = dla.getNext( dladsc );

            Testutils.dogDidNotBark ( "SPICE(NOSUCCESSOR)" );
         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth ( true, "SPICE(NOSUCCESSOR)", exc );

            //exc.printStackTrace();
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "getPrevious error: try to get predecessor of " +
                              "first segment."                                );

         dladsc = dla.beginForwardSearch();

         try
         {
            dladsc = dla.getPrevious( dladsc );

            Testutils.dogDidNotBark ( "SPICE(NOPREDECESSOR)" );
         }
         catch ( SpiceException exc )
         {
            ok = JNITestutils.chckth ( true, "SPICE(NOPREDECESSOR)", exc );

            //exc.printStackTrace();
         }


      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         //ex.printStackTrace();

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
         // Close DLA files. Unload all kernels.
         //
         dla.close();
         dla1.close();

         CSPICE.kclear();


         //
         // Delete the DSKs and SPK if they exist.
         //

         ( new File ( DSK0 ) ).delete();
         ( new File ( DSK1 ) ).delete();
         ( new File ( SPK0 ) ).delete();

      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

