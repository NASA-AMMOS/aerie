
package spice.tspice;


import spice.basic.*;
import static spice.basic.DSKDescriptor.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestDLADescriptor provides methods that implement test families for
class DSKDescriptor.

<p>Version 1.0.0 15-NOV-2016 (NJB)

*/
public class TestDSKDescriptor extends Object
{
 
   //
   // Methods
   //

   /**
   Test class DSKDescriptor APIs.
   */
   public static boolean f_DSKDescriptor()

      throws SpiceException
   {
      //
      // Constants
      //
 
      //
      // Local variables
      //
      DSKDescriptor                     dskdsc;
      DSKDescriptor                     dskdsc2;
 
      boolean                           ok;
 
      double[]                          dArray;
      double                            xdval;
      double[]                          xdArray;
      double[]                          xdArray2;

      int                               i;
      int[]                             iArray;
      int                               j;
      int                               k;
      int                               xival;


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

         JNITestutils.topen ( "f_DSKDescriptor" );




         // ***********************************************************
         //
         //      Normal cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check DSK descriptor size" );

         xival = 24;

         ok = JNITestutils.chcksi( "DSKDSZ", DSKDSZ, "=", xival, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check maximum number of coordinate parameters" );

         xival = 10;

         ok = JNITestutils.chcksi( "NSYPAR", NSYPAR, "=", xival, 0 );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Construct DSKDescriptor from array." );

         //
         // Create double array. The values of the array must be
         // distinct but need not be realistic.
         //
         dArray = new double[ DSKDSZ ];

         for ( i = 0;  i < DSKDSZ;  i++ )
         {
            dArray[i] = (double)(i+1);
         }

         dskdsc = new DSKDescriptor( dArray );

         //
         // Below we'll test the DSKDescriptor accessor functions.
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get surface ID." );

         int surfce = dskdsc.getSurfaceID();

         xival = 1;

         ok = JNITestutils.chcksi( "surfce", surfce, "=", xival, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get center ID." );

         int center = dskdsc.getCenterID();

         xival = 2;

         ok = JNITestutils.chcksi( "center", center, "=", xival, 0 );
 

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get data class." );

         int dclass = dskdsc.getDataClass();

         xival = 3;

         ok = JNITestutils.chcksi( "dclass", dclass, "=", xival, 0 );
 
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get data type." );

         int dtype = dskdsc.getDataType();

         xival = 4;

         ok = JNITestutils.chcksi( "dtype", dtype, "=", xival, 0 );
 
                  
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get frame ID." );

         int framid = dskdsc.getFrameID();

         xival = 5;

         ok = JNITestutils.chcksi( "framid", framid, "=", xival, 0 );
 

                  
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get coordinate system ID." );

         int corsys = dskdsc.getCoordSysID();

         xival = 6;

         ok = JNITestutils.chcksi( "corsys", corsys, "=", xival, 0 );
 
                  
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get coordinate parameters." );

         double[] corpar = dskdsc.getCoordParams();

         xdArray = new double[NSYPAR];

         System.arraycopy ( dArray, PARIDX, xdArray, 0, NSYPAR );

         ok = JNITestutils.chckad( "corpar", corpar, "=", xdArray, 0.0 );
 
                  

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get coordinate bounds." );

         double[][] bounds = dskdsc.getCoordBounds();

         double[] bounds1D = new double[6];

         xdArray2          = new double[6];

         k = 0;

         for ( i = 0;  i < 3;  i++ )
         {
            for ( j = 0;  j < 2;  j++ )
            {
               bounds1D[k] = bounds[i][j];

               xdArray2[k] = dArray[ MN1IDX + k ];

               ++k;
            }
         }

         ok = JNITestutils.chckad( "bounds", bounds1D, "=", xdArray2, 0.0 );
 
                  
         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Get time bounds." );

         double[] timeBounds = dskdsc.getTimeBounds();

         xdArray[0] = dArray[BTMIDX];        
         xdArray[1] = dArray[ETMIDX];

         ok = JNITestutils.chckad( "time bounds", timeBounds,
                                   "=",           xdArray,    0.0 );
 


         //
         // Test the toArray method.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Fetch descriptor contents into double array." );

         double[] doubleArray2 = dskdsc.toArray();

         ok = JNITestutils.chckad( "doubleArray2", doubleArray2, 
                                   "=",            dArray,       0.0 );


         //
         // Test the other constructors.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Call no-args constructor." );

         dskdsc = new DSKDescriptor();

         double[] doubleArray3 = new double[ DSKDSZ ];

         ok = JNITestutils.chckad( "no-args array", dskdsc.toArray(), 
                                   "=",             doubleArray3,     0.0 );
         

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Call copy constructor." );

         dskdsc = new DSKDescriptor( dArray );

         dskdsc2 = new DSKDescriptor( dskdsc );

         ok = JNITestutils.chckad( "copy's array", dskdsc2.toArray(), 
                                   "=",            dskdsc.toArray(),  0.0 );

         //
         // Verify that we can modify dladsc without changing dskdsc2.
         //
         // Save the contents of dskdsc2.
         //
         doubleArray2 = dskdsc2.toArray();

         //
         // Update dladsc.
         //
         dArray[0] = -1.0;

         dskdsc = new DSKDescriptor( dArray );  

         //
         // Compare the current contents of dladsc2 against this
         // object's previously saved contents.
         //
         ok = JNITestutils.chckad( "dskdsc2 array", dskdsc2.toArray(), 
                                   "=",             doubleArray2,      0.0 );


         //
         // Check public parameters that haven't been checked so far.
         //


         //
         // Check coodinate system codes.
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check LATSYS" );

         xival = 1;

         ok = JNITestutils.chcksi( "LATSYS", LATSYS, "=", xival, 0 );
         

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check CYLSYS" );

         xival = 2;

         ok = JNITestutils.chcksi( "CYLSYS", CYLSYS, "=", xival, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check RECSYS" );

         xival = 3;

         ok = JNITestutils.chcksi( "RECSYS", RECSYS, "=", xival, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check PDTSYS" );

         xival = 4;

         ok = JNITestutils.chcksi( "PDTSYS", PDTSYS, "=", xival, 0 );



         //
         // Check data class codes.
         //


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check SVFCLS" );

         xival = 1;

         ok = JNITestutils.chcksi( "SVFCLS", SVFCLS, "=", xival, 0 );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Check GENCLS" );

         xival = 2;

         ok = JNITestutils.chcksi( "GENCLS", GENCLS, "=", xival, 0 );



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

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

