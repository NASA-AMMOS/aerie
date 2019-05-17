
package spice.basic;

import java.util.ArrayList;
import spice.basic.CSPICE;
import spice.basic.DLADescriptor;

/**
Class DLA supports forward and backward list traversal of DLA files.

 

<h3>Examples</h3>

<p>
The numerical results shown for this example may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<ol>

<li>  Open a DLA file for read access, traverse the segment 
      list from front to back, and display segment address 
      and size attributes. 

<p> At the prompt, enter the name of the DSK file
phobos_3_3.bds. 

<p> Example code begins here.

<pre>

import java.io.*;
import spice.basic.*;

public class DLAEx1
{
   //
   // Load the JNISpice shared library.
   //
   static{  System.loadLibrary( "JNISpice" );  }


   public static void main( String[] args )
   {
      //
      // Local variables
      //
      DLA                               dla;
      DLADescriptor                     dladsc;
      String                            dlaname;
      boolean                           found;     
      int                               segno;

      
      try
      {
         //
         // Get the name of the DLA file to read; open the file
         // for read access.
         //
         dlaname = IOUtils.prompt( "Name of DLA file > " );

         dla     = DLA.openForRead( dlaname );

         //
         // Begin a forward search. An exception will be thrown 
         // if the file doesn't contain at least one segment.
         //
         segno  = 0;
         dladsc = dla.beginForwardSearch();
         found  = true;
         
         while ( found )
         {
            ++segno;

            System.out.format ( "%n"                      +
                                "Segment number = %d%n"   +
                                "%n"                      +
                                "   Backward segment pointer         = %d%n" +
                                "   Forward segment pointer          = %d%n" +
                                "   Integer component base address   = %d%n" +
                                "   Integer component size           = %d%n" +
                                "   D.p. component base address      = %d%n" +
                                "   D.p. component size              = %d%n" +
                                "   Character component base address = %d%n" +
                                "   Character component size         = %d%n" +
                                "%n",
                                segno,
                                dladsc.getBackwardPointer(),
                                dladsc.getForwardPointer(),
                                dladsc.getIntBase(),
                                dladsc.getIntSize(),
                                dladsc.getDoubleBase(),
                                dladsc.getDoubleSize(),
                                dladsc.getCharBase(),
                                dladsc.getCharSize()                        );

            //
            // Find the next segment, if there is one.
            //
            found = dla.hasNext( dladsc );

            if ( found )
            {
               dladsc = dla.getNext( dladsc );
            }
         }

      }
      catch ( java.io.IOException exc )
      {
         //
         // Handle exception raised by IOUtils.prompt call.
         //
         exc.printStackTrace();
      }
      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }
   }
}


</pre>

<p> When this program was executed on a PC/Linux/gcc/64-bit/java 1.5
platform, the output was:

<pre>

Name of DLA file > phobos_3_3.bds

Segment number = 1

   Backward segment pointer         = -1
   Forward segment pointer          = -1
   Integer component base address   = 11
   Integer component size           = 3311271
   D.p. component base address      = 0
   D.p. component size              = 494554
   Character component base address = 0
   Character component size         = 0


</pre>
</li>

<li> Open a DLA file for read access, traverse the segment 
      list from back to front, and display segment address 
      and size attributes. 

<p> At the prompt, enter the name of the DSK file
phobos_3_3.bds. 

<p> Example code begins here.


<pre>

import java.io.*;
import spice.basic.*;

public class DLAEx2
{
   //
   // Load the JNISpice shared library.
   //
   static{  System.loadLibrary( "JNISpice" );  }


   public static void main( String[] args )
   {
      //
      // Local variables
      //
      DLA                               dla;
      DLADescriptor                     dladsc;
      String                            dlaname;
      boolean                           found;     
      int                               segno;

      
      try
      {
         //
         // Get the name of the DLA file to read; open the file
         // for read access.
         //
         System.out.format( "%n" );

         dlaname = IOUtils.prompt( "Name of DLA file > " );

         dla     = DLA.openForRead( dlaname );

         //
         // Begin a backward search. An exception will be thrown 
         // if the file doesn't contain at least one segment.
         //
         segno  = 0;
         dladsc = dla.beginBackwardSearch();
         found  = true;
         
         while ( found )
         {
            ++segno;

            System.out.format ( "%n"                                         +
                                "Segment number (offset from end of file) "  +
                                "= %d%n"                                     +
                                "%n"                                         +
                                "   Backward segment pointer         = %d%n" +
                                "   Forward segment pointer          = %d%n" +
                                "   Integer component base address   = %d%n" +
                                "   Integer component size           = %d%n" +
                                "   D.p. component base address      = %d%n" +
                                "   D.p. component size              = %d%n" +
                                "   Character component base address = %d%n" +
                                "   Character component size         = %d%n" +
                                "%n",
                                segno - 1,
                                dladsc.getBackwardPointer(),
                                dladsc.getForwardPointer(),
                                dladsc.getIntBase(),
                                dladsc.getIntSize(),
                                dladsc.getDoubleBase(),
                                dladsc.getDoubleSize(),
                                dladsc.getCharBase(),
                                dladsc.getCharSize()                        );

            //
            // Find the previous segment, if there is one.
            //
            found = dla.hasPrevious( dladsc );

            if ( found )
            {
               dladsc = dla.getPrevious( dladsc );
            }
         }

      }
      catch ( java.io.IOException exc )
      {
         //
         // Handle exception raised by IOUtils.prompt call.
         //
         exc.printStackTrace();
      }
      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }
   }
}

</pre>
 

<p> When this program was executed on a PC/Linux/gcc/64-bit/java 1.5
platform, the output was:

<pre>

Name of DLA file > phobos_3_3.bds

Segment number (offset from end of file) = 0

   Backward segment pointer         = -1
   Forward segment pointer          = -1
   Integer component base address   = 11
   Integer component size           = 3311271
   D.p. component base address      = 0
   D.p. component size              = 494554
   Character component base address = 0
   Character component size         = 0


</pre>

</li>


</ol>

 


<h3>Author_and_Version </h3>

Version 1.0.0 09-JAN-2017 (NJB)


*/

public class DLA extends DAS
{
   //
   // Fields
   //
  
   //
   // Constructors
   //

   
   //
   // This constructor must remain protected until a method of creating
   // a new DLA file is provided.
   //
   protected DLA ( String filename )

      throws SpiceErrorException
   {
       super( filename );
   }

   /**
   No-args constructor.
   */
   public DLA ()
   {
       super();
   }


   /**
   Construct a DLA instance from a DAS instance. This
   constructor creates a deep copy.

   <p> The DAL file must have type DLA or DSK.

   <p> User applications will not need to call this
   constructor directly. See the methods {@link #openForRead}
   and {@link #openForWrite}.
   */
   protected DLA ( DAS das )

      throws SpiceException
   {
      super();

      //
      // Copy the attributes of the DAS file.
      //
      this.fileName         = das.getFileName();
      this.handle           = das.getHandle();
      this.readable         = das.isReadable();
      this.writable         = das.isWritable();

      //
      // Check attributes of DAS file. The file must have 
      // file type DLA or DSK.
      //
      String[] archArray = new String[1];
      String[] typeArray = new String[1];

      CSPICE.getfat( fileName, archArray, typeArray );

      boolean archMatch = (archArray[0]).equals ( "DAS" );

      boolean typeMatch = (typeArray[0]).equals ( "DLA" )  ||
                          (typeArray[0]).equals ( "DSK" );

      if (  !( archMatch && typeMatch )  )
      {
         SpiceErrorException exc = 

            SpiceErrorException.create( "DLA( DAS das )",
                                        "SPICE(NOTADLAFILE)",
                                        "The input DAS file has " + 
                                        "architecture "           +
                                        archArray[0]              +
                                        " and file type "         +
                                        typeArray[0]              +
                                        ". The file can't be "    +
                                        "used to construct "      +
                                        "a DLA file."              );
         throw( exc );
      }

   }



   //
   // Static Methods
   //

   /**
   Open a DLA file for read access.
   */
   public static DLA openForRead ( String filename )
     
      throws SpiceException
   {
       //
       // Get a DAS instance associated with the file.
       // The DAS is opened for read access.
       //
       // Create a DLA instance from the DAS instance.
       //
       return(  new DLA( DAS.openForRead(filename) )  );
   }


   /*

   To be added post-N0066. Support for DLABNA, DLAENA is required.

   //
   Open a DLA file for write access.
   //
   public static DLA openForWrite ( String filename )
     
      throws SpiceException
   {
       //
       // Get a DAS instance associated with the file.
       // The DAS is opened for write access.
       //
       // Create a DLA instance from the DAS instance.
       //
       return(  new DLA( DAS.openForWrite(filename) )  );
   }
   */



   //
   // Instance Methods
   //

   /**
   Start a forward search on a DLA file.

   <p>An exception is thrown if the file contains no segments.
   */
   public DLADescriptor beginForwardSearch()

      throws SpiceException
   {
      boolean[] foundArray = new boolean[1];

      int handle = this.getHandle();

      int[] descrArray = new int[ DLADescriptor.DLADSZ ];

      
      CSPICE.dlabfs( handle, descrArray, foundArray );

      if ( !foundArray[0] ) 
      {
         SpiceErrorException exc = 

            SpiceErrorException.create( "DLA.beginForwardSearch",
                                        "SPICE(NODLASEGMENTS)",
                                        "No segments were found in "  + 
                                        "the DLA file designated by " +
                                        "the DAS handle "             +
                                        handle                        );
         throw( exc );
      }

      return(  new DLADescriptor( descrArray )  );
   }


   /**
   Start a backward search on a DLA file.

   <p>An exception is thrown if the file contains no segments.
   */
   public DLADescriptor beginBackwardSearch()

      throws SpiceException
   {
      boolean[] foundArray = new boolean[1];

      int handle = this.getHandle();

      int[] descrArray = new int[ DLADescriptor.DLADSZ ];

      
      CSPICE.dlabbs( handle, descrArray, foundArray );

      if ( !foundArray[0] ) 
      {
         SpiceErrorException exc = 

            SpiceErrorException.create( "DLA.beginBackwardSearch",
                                        "SPICE(NODLASEGMENTS)",
                                        "No segments were found in "  + 
                                        "the DLA file designated by " +
                                        "the DAS handle "             +
                                        handle                        );
         throw( exc );
      }

      return(  new DLADescriptor( descrArray )  );
   }



   /**
   Indicate whether a DLA segment has a successor.
   */
   public boolean hasNext( DLADescriptor   DLADescr )

      throws SpiceException
   {
      boolean[] foundArray = new boolean[1];

      int handle   = this.getHandle();

      int[] curdsc = DLADescr.toArray();

      int[] nxtdsc = new int[ DLADescriptor.DLADSZ ];

      CSPICE.dlafns( handle, curdsc, nxtdsc, foundArray );

      return( foundArray[0] );      
   }



   /**
   Indicate whether a DLA segment has a predecessor.
   */
   public boolean hasPrevious( DLADescriptor   DLADescr )

      throws SpiceException
   {
      boolean[] foundArray = new boolean[1];

      int handle   = this.getHandle();

      int[] curdsc = DLADescr.toArray();

      int[] prvdsc = new int[ DLADescriptor.DLADSZ ];

      CSPICE.dlafps( handle, curdsc, prvdsc, foundArray );

      return( foundArray[0] );      
   }



   /**
   Get the DLA descriptor of the successor of a given DLA segment.
   */
   public DLADescriptor getNext( DLADescriptor   DLADescr )

      throws SpiceException
   {
      boolean[] foundArray = new boolean[1];

      int handle   = this.getHandle();

      int[] curdsc = DLADescr.toArray();

      int[] nxtdsc = new int[ DLADescriptor.DLADSZ ];

      CSPICE.dlafns( handle, curdsc, nxtdsc, foundArray );

      if ( !foundArray[0] ) 
      {
         SpiceErrorException exc = 

            SpiceErrorException.create( "DLA.getNext",
                                        "SPICE(NOSUCCESSOR)",
                                        "No next DLA segment was found " +
                                        "in the DLA file designated "    + 
                                        "by the DAS handle "             +
                                         handle                           );
         throw( exc );
      }
      
      return(  new DLADescriptor( nxtdsc )  );
   }




   /**
   Get the DLA descriptor of the predecessor of a given DLA segment.
   */
   public DLADescriptor getPrevious( DLADescriptor   DLADescr )

      throws SpiceException
   {
      boolean[] foundArray = new boolean[1];

      int handle   = this.getHandle();

      int[] curdsc = DLADescr.toArray();

      int[] prvdsc = new int[ DLADescriptor.DLADSZ ];

      CSPICE.dlafps( handle, curdsc, prvdsc, foundArray );

      if ( !foundArray[0] ) 
      {
         SpiceErrorException exc = 

            SpiceErrorException.create( "DLA.getPrevious",
                                        "SPICE(NOPREDECESSOR)",
                                        "No previous DLA segment was found " +
                                        "in the DLA file designated "        + 
                                        "by the DAS handle "                 +
                                         handle                               );
         throw( exc );
      }
      
      return(  new DLADescriptor( prvdsc )  );
   }


   
   /**
   Count the segments in a DLA file.
   */
   public int getSegmentCount()

      throws SpiceException
   {
      int count;

      //
      // If the file has no segments, the beginForwardSearch
      // method will signal an error. For this method, we need
      // to treat the zero-segment case as valid.
      //
      // Start by finding out whether a forward search turns up
      // at least one segment.
      //
      boolean[] foundArray = new boolean[1];

      int handle           = this.getHandle();

      int[] descrArray     = new int[ DLADescriptor.DLADSZ ];
      
      CSPICE.dlabfs( handle, descrArray, foundArray );
      
      if ( !foundArray[0] )
      {
         count = 0;
      }
      else
      {
         //
         // Start a new forward search and traverse the file.
         //
         DLADescriptor dladsc = beginForwardSearch();

         count = 1;

         while ( hasNext(dladsc) )
         {
             ++count;

             dladsc = getNext(dladsc);
         }
      }
      return( count );
   }

}





