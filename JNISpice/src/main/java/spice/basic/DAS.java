
package spice.basic;

import java.util.ArrayList;
import spice.basic.CSPICE;

/**
Class DAS supports creation of and low-level read
operations on DAS files.

<p>
This class supports DAS comment area read access.

<p>
See the subclass  
{@link spice.basic.DSK} for methods used to write that type of file.

<p>
Normal read access of DSK files
requires that these files be loaded via
{@link spice.basic.KernelDatabase#load(java.lang.String)}. This method
plays the role of the routine FURNSH in SPICELIB.

<h3>Examples</h3>
TBD
<p>
The numerical results shown for this example may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<pre>
</pre>


<h3>Author_and_Version </h3>

Version 1.0.0 10-DEC-2016 (NJB)


*/

public class DAS extends Object
{
   //
   // Fields
   //
   protected String        fileName;
   protected int           handle;
   protected boolean       readable;
   protected boolean       writable;


   //
   // Nested classes
   //

   //
   // Class FileRecord centralizes file record fetch
   // actions.
   //
   private class FileRecord 
   {
      //
      // Fields
      //
      String   idword;
      String   ifname;
      int      nresvr;
      int      nresvc;
      int      ncomr;
      int      ncomc;

      //
      // Constructors
      //
      private FileRecord ( DAS das )

         throws SpiceException
      {
         //
         // Local variables
         //
         String[] idwordArray = new String[1];
         String[] ifnameArray = new String[1];
         int[]    nresvrArray = new int[1];
         int[]    nresvcArray = new int[1];
         int[]    ncomrArray  = new int[1];
         int[]    ncomcArray  = new int[1];

         //
         // Fetch file record parameters from the DAS file.
         // 
         CSPICE.dasrfr( das.handle,  idwordArray, ifnameArray,
                        nresvrArray, nresvcArray, ncomrArray,  ncomcArray ); 
 
         idword = idwordArray[0];
         ifname = ifnameArray[0];
         nresvr = nresvrArray[0];
         nresvc = nresvcArray[0];
         ncomr  = ncomrArray[0];
         ncomc  = ncomcArray[0];      
      }
   }

   //
   // Constructors
   //

   /**
   Construct a DAS instance representing a file.
   The file need not exist.

   <p> User applications will not need to call this
   constructor directly. See the methods {@link #openForRead}
   and {@link #openForWrite}.
   */
   protected DAS ( String fileName )
   {
      this.fileName    = fileName;
      handle           = 0;
      readable         = false;
      writable         = false;
   }


   /**
   Construct a DAS instance from handle of an open DAS file.
   */
   public DAS( int  handle )
  
      throws SpiceException
   {
      //
      // Map the input handle to a file name. If the
      // handle is invalid (for example, if it's stale),
      // an exception will be thrown.
      //    
      this.fileName = CSPICE.dashfn( handle );
      this.handle   = handle;

      //
      // Any DAS file that has a valid handle is readable.
      //
      readable = true;

      //
      // Handles of writable DAS files are negative.
      //
      writable = (handle < 0);
   }


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public DAS ( DAS das )
   {
      this.fileName    = new String(das.fileName);
      handle           = das.handle;
      readable         = das.readable;
      writable         = das.writable;
   }


   /**
   No-args constructor.
   */
   public DAS ()
   {
   }


   //
   // Static Methods
   //




   /**
   Open a DAS file for write access.
   */
   public static DAS openForRead( String fileName )

      throws SpiceException
   {
      //
      // Create a new DAS.
      //
      DAS d = new DAS( fileName );

      d.handle   = CSPICE.dasopr( fileName );

      d.readable = true;

      return ( d );
   }


   /**
   Open a DAS file for read access.
   */
   public static DAS openForWrite( String fileName )

      throws SpiceException
   {
 
      //
      // Create a new DAS.
      //
      DAS d = new DAS( fileName );

      d.handle   = CSPICE.dasopw( fileName );

      d.readable = true;

      return ( d );
   }





  
   //
   // Instance Methods
   //

   /**
   Get file handle.
   */
   public int getHandle()

      throws SpiceException
   {
      checkAccess();

      return( handle );
   }

   /**
   Return the file name.
   */
   public String getFileName()

      throws SpiceException
   {
      if ( fileName == null )
      {
         SpiceException exc 

           = new SpiceException ( "File name is null." );

         throw( exc );
      }

      return( fileName );
   }


   /**
   Indicate whether a DAS file is readable.

   <p> A DAS file is readable if it has been opened
   for read OR write access.
   */
   public boolean isReadable()
   {
      return( readable );
   }

   /**
   Indicate whether a DAS file is writable.
   */
   public boolean isWritable()
   {
      return( writable );
   }
 
 
  

   /**
   Close a specified DAS file, thereby freeing resources.
   */
   public void close()

      throws SpiceException
   {
      CSPICE.dascls( handle );

      //
      // This DAS is no longer readable or writable.
      //

      readable = false;
      writable = false;
   }

 
   /**
   Append comments to the comment area of a DAS file.
   */
   public void addComments( String[] commentBuffer )

      throws SpiceException
   {
      checkAccess();

      CSPICE.dasac( handle, commentBuffer );      
   }



   /**
   Delete comments from a DAS file.
   */
   public void deleteComments()

      throws SpiceException
   {
      checkAccess();

      CSPICE.dasdc( handle );      
   }



   /**
   Read comments from an existing DAS file.
   */
   public String[] readComments( int  lineLength )

      throws SpiceException
   {
      //
      // Local constants
      //
      final int NLINES = 20;

      checkAccess();
 

      //
      // We'll accumulate comments from the DAS file in an ArrayList
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

      CSPICE.dasec ( handle,
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
         CSPICE.dasec ( handle,
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
   Get the internal file name from a DAS file.

   <p> This method initializes the internal file name 
   field of the DAS instance and returns a deep copy
   of the name.
   */
   public String getInternalFileName()

      throws SpiceException
   {
      checkAccess();

      //
      // Create a FileRecord instance for this file.
      //
      FileRecord fr = new FileRecord( this );

      return (  new String( fr.ifname )  );
   }

 

   /**
   Get the number of comment records in a DAS file.
   */
   public int getCommentRecordCount()

      throws SpiceException
   {
      FileRecord fr = new FileRecord( this );  

      return ( fr.ncomr );    
   }


   /**
   Get the number of comment characters in a DAS file.
   */
   public int getCommentCharacterCount()

      throws SpiceException
   {
      FileRecord fr = new FileRecord( this );  

      return ( fr.ncomc );    
   }


   /**
   Helper method for diagnosing improper file access.
   This method centralizes error handling for cases
   where access to a closed file is requested. 
   */
   private void checkAccess()
 
      throws SpiceErrorException
   {
      if (  (!readable)  &&  (!writable)  )
      {
         String excMsg;

         excMsg = String.format ( " File %s is closed. The file " +
                                  "must be opened for read or "   +
                                  "write access in order to "     +
                                  "perform the requested "        +
                                  "operation",  fileName            );

         SpiceErrorException exc 

            = SpiceErrorException.create ( "DAS.checkAccess()",
                                           "SPICE(DASFILECLOSED)",
                                           excMsg                 );

         throw ( exc );
      }

   }
}





