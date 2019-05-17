
package spice.basic;

import java.util.ArrayList;
import spice.basic.CSPICE;

/**
Class DAF supports creation of and low-level read
operations on DAF files.

<p>
This class supports DAF segment descriptor
list traversal and comment area access.

<p>
See the subclasses {@link spice.basic.SPK} and
{@link spice.basic.CK} for methods used to write those types of files.

<p>
Normal read access of SPK, CK, and binary PCK files
requires that these files be loaded via
{@link spice.basic.KernelDatabase#load(java.lang.String)}. This method
plays the role of the routine FURNSH in SPICELIB.

<h3>Examples</h3>

<p>
The numerical results shown for this example may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<pre>
   //
   // Example of DAF segment descriptor list traversal
   //

   import spice.basic.*;

   public class DAF_ex1
   {
      //
      // Load the JNISpice shared object library before program execution.
      //
      static
      {
         System.loadLibrary( "JNISpice" );
      }

      public static void main ( String[] args )
      {
         try
         {
            //
            // Constants
            //
            final String TIMFMT =  new String (
                                  "YYYY MON DD HR:MN:SC.###### (TDB)::TDB" );

            //
            // Local variables
            //
            boolean      found;
            DAF          d;
            double[]     dc;
            int          segno = 0;
            int[]        ic;
            String       bstr;
            String       estr;

            //
            // Load a leapseconds kernel to support time conversion.
            //
            KernelDatabase.load ( "naif0009.tls" );

            //
            // Create a DAF instance and open the DAF for
            // read access. We expect the name of the DAF
            // to be supplied on the command line.
            //
            if ( args.length == 0 )
            {
               System.out.println ( "Usage: java DAF_ex1 <filename>" );
               return;
            }

            d = DAF.openForRead( args[0] );

            //
            // Start a forward search through the segment list
            // of this DAF.
            //
            d.beginForwardSearch();

            found = d.findNextArray();

            while ( found )
            {
               ++segno;

               //
               // Get integer portion of current array summary
               // (aka descriptor).
               //
               ic = d.getIntegerSummaryComponent();
               dc = d.getDoubleSummaryComponent();

               System.out.format  ( "%n%nSegment%n%n", segno );

               System.out.println ( "Body ID       = " + ic[0] );
               System.out.println ( "Center ID     = " + ic[1] );
               System.out.println ( "Frame ID      = " + ic[2] );
               System.out.println ( "Data Type     = " + ic[3] );
               System.out.println ( "Begin address = " + ic[4] );
               System.out.println ( "End address   = " + ic[5] );

               bstr = CSPICE.timout ( dc[0], TIMFMT );
               estr = CSPICE.timout ( dc[1], TIMFMT );

               System.out.println ( "Start time    = " + bstr );
               System.out.println ( "Stop time     = " + estr );

               //
               // Find the next segment.
               //
               found = d.findNextArray();
            }

            //
            // Close the DAF.
            //
            d.close();
         }
         catch ( SpiceException exc )
         {
            exc.printStackTrace();
         }
      }
   }
</pre>

When executed on a PC/Linux/gcc/java 1.6.0_14 platform, the output
   from this program was (only partial output is shown):

   <pre>


   Segment 1

   Body ID       = 1
   Center ID     = 0
   Frame ID      = 1
   Data Type     = 2
   Begin address = 641
   End address   = 310404
   Start time    = 1899 JUL 29 00:00:00.000000 (TDB)
   Stop time     = 2053 OCT 09 00:00:00.000000 (TDB)


   Segment 2

   Body ID       = 2
   Center ID     = 0
   Frame ID      = 1
   Data Type     = 2
   Begin address = 310405
   End address   = 423048
   Start time    = 1899 JUL 29 00:00:00.000000 (TDB)
   Stop time     = 2053 OCT 09 00:00:00.000000 (TDB)


     ...


   Segment 15

   Body ID       = 499
   Center ID     = 4
   Frame ID      = 1
   Data Type     = 2
   Begin address = 2098633
   End address   = 2098644
   Start time    = 1899 JUL 29 00:00:00.000000 (TDB)
   Stop time     = 2053 OCT 09 00:00:00.000000 (TDB)
   </pre>



<h3>Author_and_Version </h3>

Version 1.0.0 19-DEC-2009 (NJB)


*/

public class DAF extends Object
{
   //
   // Fields
   //
   protected String        fileName;
   protected int           handle;
   protected String        internalFileName;
   protected int           ND;
   protected int           NI;
   protected boolean       readable;
   protected boolean       writable;



   //
   // Constructors
   //

   /**
   Construct a DAF instance representing a file.
   The file need not exist.
   */
   public DAF ( String fileName )
   {
      this.fileName    = fileName;
      handle           =  0;
      internalFileName = null;
      ND               = -1;
      NI               = -1;
      readable         = false;
      writable         = false;
   }



   //
   // Static Methods
   //




   /**
   Open a DAF for read access.
   */
   public static DAF openForRead( String fileName )

      throws SpiceException
   {
      //
      // The following variables are declared as 1-dimensional
      // arrays so that they can serve as output arguments
      // in a call to CSPICE.dafrfr:
      //
      String[]          ifname = new String[1];
      int[]             fward  = new int[1];
      int[]             bward  = new int[1];
      int[]             free   = new int[1];
      int[]             nd     = new int[1];
      int[]             ni     = new int[1];


      //
      // Create a new DAF.
      //
      DAF d = new DAF( fileName );

      d.handle = CSPICE.dafopr( fileName );

      //
      // Obtain the DAF descriptor parameters ND and NI,
      // since these will be used for descriptor unpacking.
      //
      CSPICE.dafrfr ( d.handle, nd, ni, ifname, fward, bward, free );



      d.ND               = nd[0];
      d.NI               = ni[0];

      d.internalFileName = ifname[0];

      d.readable         = true;

      return ( d );
   }


   /**
   Open an existing DAF for write access.

   <p> Note that a DAF cannot be opened for write access
   if it has already been opened for read access.
   */
   public static DAF openForWrite( String fileName )

      throws SpiceException
   {
      //
      // The following variables are declared as 1-dimensional
      // arrays so that they can serve as output arguments
      // in a call to CSPICE.dafrfr:
      //
      String[]          ifname = new String[1];
      int[]             fward  = new int[1];
      int[]             bward  = new int[1];
      int[]             free   = new int[1];
      int[]             nd     = new int[1];
      int[]             ni     = new int[1];

      //
      // Create a new DAF.
      //
      DAF d = new DAF( fileName );

      //
      // If the file is already open for read access, the following
      // call will fail.
      //
      d.handle = CSPICE.dafopw( fileName );

      //
      // Obtain the DAF descriptor parameters ND and NI,
      // since these will be used for descriptor unpacking.
      //
      CSPICE.dafrfr ( d.handle, nd, ni, ifname, fward, bward, free );



      d.ND               = nd[0];
      d.NI               = ni[0];

      d.internalFileName = ifname[0];

      //
      // DAFs opened for write access are both readable and writable.
      //
      d.readable         = true;
      d.writable         = true;

      return ( d );
   }




   //
   // Instance Methods
   //

   /**
   Get file handle.
   */
   public int getHandle()
   {
      return( handle );
   }

   /**
   Return the file name.
   */
   public String getFileName()
   {
      return( fileName );
   }

   /**
   Indicate whether a DAF is readable.

   <p> A DAF is readable if it has been opened
   for read OR write access.
   */
   public boolean isReadable()
   {
      return( readable );
   }

   /**
   Indicate whether a DAF is writable.
   */
   public boolean isWritable()
   {
      return( writable );
   }


   /**
   Get number of integer summary components.
   */
   public int getNI()
   {
      return NI;
   }

   /**
   Get number of double precision summary components.
   */
   public int getND()
   {
      return ND;
   }


   /**
   Get internal file name.
   */
   public String getInternalFileName()

      throws SpiceException
   {
      if ( internalFileName == null )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.getInternalFileName",

            "SPICE(NOTAVAILABLE)",

            "Internal file name has not been read from DAF " + fileName);

         throw ( exc );
      }

      return ( internalFileName );
   }





   /**
   Begin forward search through segment list.
   */
   public void beginForwardSearch()

      throws SpiceException
   {
      if ( !readable )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.beginForwardSearch",

            "SPICE(DAFNOTREADABLE)",

            "DAF " + fileName + " must be opened for read or" +
            "write access before it can be searched."           );

         throw ( exc );
      }

      CSPICE.dafbfs( handle );
   }


   /**
   Begin backward search through segment list.
   */
   public void beginBackwardSearch()

      throws SpiceException
   {
      if ( !readable )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.beginBackwardSearch",

            "SPICE(DAFNOTREADABLE)",

            "DAF " + fileName + " must be opened for read or" +
            "write access before it can be searched."           );

         throw ( exc );
      }

      CSPICE.dafbbs( handle );
   }


   /**
   Find the next array in the segment list.

   <p>
   This methods returns a "found" flag.
   */
   public boolean findNextArray()

      throws SpiceException
   {
      if ( !readable )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.findNextArray",

            "SPICE(DAFNOTREADABLE)",

            "DAF " + fileName + " must be opened for read or" +
            "write access before it can be searched."           );

         throw ( exc );
      }

      //
      // Since multiple searches might be underway, continue
      // the search on this file.
      //
      CSPICE.dafcs ( handle );

      //
      // Now find the next array.
      //
      boolean found = CSPICE.daffna();

      return ( found );
   }


   /**
   Find the previous array in the segment list.

   <p>
   This methods returns a "found" flag.
   */
   public boolean findPreviousArray()

      throws SpiceException
   {
      if ( !readable )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.findPreviousArray",

            "SPICE(DAFNOTREADABLE)",

            "DAF " + fileName + " must be opened for read or" +
            "write access before it can be searched."           );

         throw ( exc );
      }

      //
      // Since multiple searches might be underway, continue
      // the search on this file.
      //
      CSPICE.dafcs ( handle );

      //
      // Now find the previous array.
      //
      boolean found = CSPICE.daffpa();

      return ( found );
   }


   /**
   Get the array name (also called the "segment identifier")
   (also called the "array name") for the current array (also
   called "segment").
   */
   public String getArrayName()

      throws SpiceException
   {
      if ( !readable )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.getArrayName",

            "SPICE(DAFNOTREADABLE)",

            "DAF " + fileName + " must be opened for read or" +
            "write access before it can be searched."           );

         throw ( exc );
      }

      String name = CSPICE.dafgn();

      return ( name );
   }


   /**
   >>> Decide whether this method should be public.

   Get the summary (also called "descriptor") of the current
   array (also called "segment").
   */
   private double[] getArraySummary()

      throws SpiceException

   {
      if ( !readable )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.getArraySummary",

            "SPICE(DAFNOTREADABLE)",

            "DAF " + fileName + " must be opened for read or" +
            "write access before it can be searched."           );

         throw ( exc );
      }

      int          size = ND + ( ( NI + 1 ) / 2 );

      double[]     sum  = CSPICE.dafgs( size );

      return ( sum );
   }




   /**
   Get the double precision component of the array summary
   for the current segment.
   */
   public double[] getDoubleSummaryComponent()

      throws SpiceException
   {

      double[]     sum = this.getArraySummary();


      double[]     dc   = new double[ND];
      int[]        ic   = new int   [NI];

      CSPICE.dafus ( sum, ND, NI, dc, ic );

      return ( dc );
   }


   /**
   Get the integer component of the array summary
   for the current segment.
   */
   public int[] getIntegerSummaryComponent()

      throws SpiceException
   {
      double[]     sum = this.getArraySummary();

      double[]     dc   = new double[ND];
      int[]        ic   = new int   [NI];

      CSPICE.dafus ( sum, ND, NI, dc, ic );

      return ( ic );
   }


   /**
   Close a specified DAF, thereby freeing resources.
   */
   public void close()

      throws SpiceException
   {
      CSPICE.dafcls( handle );

      //
      // This DAF is no longer readable or writable.
      //

      readable = false;
      writable = false;
   }


   /**
   Add comments to an existing DAF.
   */
   public void addComments ( String[] commentBuffer )

      throws SpiceException
   {
      if ( !writable )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.addComments",

            "SPICE(DAFNOTWRITABLE)",

            "DAF " + fileName + " must be opened for " +
            "write access via openForWrite() before " +
            "comments can be added to the file."         );

         throw ( exc );
      }

      CSPICE.dafac ( handle, commentBuffer );
   }


   /**
   Read comments from an existing DAF.
   */
   public String[] readComments( int  lineLength )

      throws SpiceException
   {
      //
      // Local constants
      //
      final int NLINES = 20;

      if ( !readable )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.readComments",

            "SPICE(DAFNOTREADABLE)",

            "DAF " + fileName + " must be opened for read or" +
            "write access comments can be read."               );

         throw ( exc );
      }


      //
      // We'll accumulate comments from the DAF in an ArrayList
      // of Strings. This allows us to build up a list of
      // arbitrary size.
      //
      ArrayList<String> alist  = new ArrayList<String>( NLINES );



      int[]     n      = new int[1];
      String[]  combuf = new String[NLINES];
      boolean[] done   = new boolean[1];

      //
      // Fetch comments into a buffer of size NLINES.
      // Continue until `done' indicates there are no
      // more comments to fetch.
      //

      CSPICE.dafec ( handle,
                     NLINES,
                     lineLength,
                     n,
                     combuf,
                     done       );

      for ( int i = 0;  i < n[0];  i++ )
      {
         alist.add( combuf[i] );
      }

      while ( !done[0] )
      {
         CSPICE.dafec ( handle,
                        NLINES,
                        lineLength,
                        n,
                        combuf,
                        done       );

         for ( int i = 0;  i < n[0];  i++ )
         {
            alist.add( combuf[i] );
         }
      }


      //
      // Extract the comments from the ArrayList into
      // an array of Strings.
      //
      String[] retbuf = alist.toArray( new String[0] );


      return ( retbuf );
   }


   /**
   Delete comments from a DAF.
   */
   public void deleteComments()

      throws SpiceException
   {
      if ( !writable )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "DAF.deleteComments",

            "SPICE(DAFNOTWRITABLE)",

            "DAF " + fileName + " must be opened for " +
            "write access via openForWrite() before " +
            "comments can be deleted from the file."         );

         throw ( exc );
      }

      CSPICE.dafdc( handle );
   }





   /**
   Count the segments in a DAF file.
   */
   public int countSegments()

      throws SpiceException
   {
      boolean found;
      int     n = 0;


      this.beginForwardSearch();

      found = this.findNextArray();

      while ( found )
      {
         ++n;

         found = this.findNextArray();
      }

      return ( n );
   }

}





