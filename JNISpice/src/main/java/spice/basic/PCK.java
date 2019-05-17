

package spice.basic;

/**
Class PCK provides methods for writing, summarizing,
and conducting low-level read operations on PCK files.

<p>
To make PCK data available to programs for state or position
transformation computations,
use the method
{@link spice.basic.KernelDatabase#load(java.lang.String)}.

<p>
State or position transformation matrices, which may rely on
PCK data for their construction, are normally computed via methods
of the class {@link ReferenceFrame}.

<p>Note that the "frame class ID" argument occurring in some
of the calling sequences of methods of this class is referred to
as a "body" or "body ID" in older SPICE documentation. The new
terminology is more accurate, since the ID code is not necessarily
that of a body. In all cases the ID designates a reference frame 
of the PCK class.

<p> Version 1.0.0 20-DEC-2016 (NJB)

*/
public class PCK extends DAF
{

   //
   // Private methods
   //

   /**
   Construct an PCK instance representing a file.
   The file need not exist.
   */


   private PCK ( String fileName )
   {
      super ( fileName );
   }


   /**
   Open a new PCK file. The file must not exist prior
   to this method call.
   */


   
   private void openNew( String         internalFileName,
                         int            nCommentChars )

      throws SpiceException
   {
      this.handle = CSPICE.pckopn ( this.fileName,
                                    internalFileName,
                                    nCommentChars     );

      this.fileName         = fileName;
      this.internalFileName = internalFileName;
      ND                    =  2;
      NI                    =  5;
      readable              = false;
      writable              = true;
   }
   

   //
   // Static methods
   //

   /**
   Create a new PCK file. The file must not exist prior
   to this method call.
   */
   public static PCK openNew ( String         fileName,
                               String         internalFileName,
                               int            nCommentChars     )
      throws SpiceException
   {
      PCK pck = new PCK( fileName );

      pck.openNew ( internalFileName,
                    nCommentChars     );

      return ( pck );
   }


   /**
   Open an existing PCK file for read access.
   */
   public static PCK openForRead( String fileName )

      throws SpiceException
   {

      PCK pck = new PCK( fileName );

      //
      // We need to make sure we're dealing with a PCK file.
      // If the file architecture isn't DAF, DAFOPR will take
      // care of signaling an error. We'll handle the issue
      // of an invalid file type separately.
      //
      DAF daf = DAF.openForRead( fileName );

      pck.handle           = daf.getHandle();
      pck.internalFileName = daf.getInternalFileName();
      pck.ND               = daf.getND();
      pck.NI               = daf.getNI();
      pck.readable         = true;
      pck.writable         = false;

      String[]             archArr = new String[1];
      String[]             typeArr = new String[1];

      CSPICE.getfat( fileName, archArr, typeArr );

      if (    ( !archArr[0].equals("DAF") ) 
           || ( !typeArr[0].equals("PCK") )  )
      {
         String errmsg = String.format( "File <%s> has archtecture %s "+ 
                                        "and type <5s>.",
                                        archArr[0], 
                                        typeArr[0]                       );
         SpiceErrorException exc = 

            SpiceErrorException.create( "PCK.openForRead", 
                                        "SPICE(FILEISNOTAPCK)",
                                         errmsg                 );
         throw( exc );
      }

      return ( pck );
   }


   //
   // Constructors
   //

   //
   // Instance methods
   //

   /**
   Obtain a set of frame class ID codes of body-fixed
   reference frames for which a PCK file
   contains data.
   */
   public int[] getFrameClassIDs()

      throws SpiceException
   {
      int   size       = this.countSegments();

      int[] initialSet = new int[0];

      int[] objectSet = CSPICE.pckfrm( fileName, size, initialSet );

      return ( objectSet );
   }

   /**
   Obtain a set of frame class ID codes of frames for which a PCK file
   contains data, merged with an existing set of ID codes.

   <p> The result is returned in a new set.
   */
   public int[] getFrameClassIDs( int[] initialSet )

      throws SpiceException
   {
      int   size      = this.countSegments();

      int[] objectSet = CSPICE.pckfrm( fileName, size + initialSet.length,
                                       initialSet );

      return ( objectSet );
   }


   /**
   Obtain a SpiceWindow representing the time coverage provided by
   this PCK for a given reference frame.

   <p> The returned window contains times expressed as seconds past
   J2000 TDB.
   */
   public SpiceWindow getCoverage( int  classID )

      throws SpiceException
   {
      int size = 2 * this.countSegments();

      double[] initialWindowArray = new double[0];

      double[] resultArray = CSPICE.pckcov( fileName, classID, size,
                                            initialWindowArray      );

      SpiceWindow result = new SpiceWindow( resultArray );

      return ( result );
   }


   /**
   Return a SpiceWindow representing the union of a given
   SPICE coverage window with the time coverage provided by this
   PCK for a given object.

   <p> The returned window contains times expressed as seconds past
   J2000 TDB.
   */
   public SpiceWindow getCoverage( int          classID,
                                   SpiceWindow  cover )
      throws SpiceException
   {
      int size = 2 * ( this.countSegments() + cover.card() );

      double[] resultArray = CSPICE.pckcov( fileName, classID, size,
                                            cover.toArray()         );

      SpiceWindow result = new SpiceWindow( resultArray );

      return ( result );
   }


   /**
   Write a type 2 segment to a PCK file.
   */
   public void writeType02Segment ( int             clssid,
                                    ReferenceFrame  baseFrame,
                                    Time            first,
                                    Time            last,
                                    String          segid,
                                    TDBDuration     intlen,
                                    int             n,
                                    int             polydg,
                                    double[][][]    ChebyCoeffs,
                                    Time            btime  )

      throws SpiceException
   {
      int              nrec       = ChebyCoeffs.length;
      int              ncoeff     = (ChebyCoeffs[0][0]).length;
      int              to         = 0;
      double[]         cdata      = new double[nrec * 3 * ncoeff];

      //
      // Create a one-dimensional coefficient array, since this is
      // what CSPICE.pckw02 expects.
      //
      for ( int i = 0;  i < nrec;  i++ )
      {
         for ( int j = 0;  j < 3;  j++ )
         {
            System.arraycopy ( ChebyCoeffs[i][j], 0, cdata, to, ncoeff );

            to += ncoeff;
         }
      }

      CSPICE.pckw02 ( this.handle,
                      clssid,
                      baseFrame.getName(),
                      first.getTDBSeconds(),
                      last.getTDBSeconds(),
                      segid,
                      intlen.getMeasure(),
                      n,
                      polydg,
                      cdata,
                      btime.getTDBSeconds()   );
   }
 }

 
 
 
