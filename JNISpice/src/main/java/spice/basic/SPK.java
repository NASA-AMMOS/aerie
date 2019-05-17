

package spice.basic;

/**
Class SPK provides methods for writing, summarizing,
and conducting low-level read operations on SPK files.

<p>
To make SPK data available to programs for state or position computations,
use the method
{@link spice.basic.KernelDatabase#load(java.lang.String)}.

<p>
State or position vectors are normally computed via methods
of the classes {@link spice.basic.StateVector}, {@link
spice.basic.StateRecord}, {@link spice.basic.PositionVector},
or {@link spice.basic.PositionRecord}.

<p> Version 1.0.0 04-JAN-2010 (NJB)

*/
public class SPK extends DAF
{


   //
   // Private methods
   //

   /**
   Construct an SPK instance representing a file.
   The file need not exist.
   */


   private SPK ( String fileName )
   {
      super ( fileName );
   }


   /**
   Open a new SPK file. The file must not exist prior
   to this method call.
   */
   private void openNew( String         internalFileName,
                         int            nCommentChars )

      throws SpiceException
   {
      this.handle = CSPICE.spkopn ( this.fileName,
                                    internalFileName,
                                    nCommentChars     );

      this.fileName         = fileName;
      this.internalFileName = internalFileName;
      ND                    =  2;
      NI                    =  6;
      readable              = false;
      writable              = true;
   }


   //
   // Static methods
   //

   /**
   Create a new SPK file. The file must not exist prior
   to this method call.
   */
   public static SPK openNew ( String         fileName,
                               String         internalFileName,
                               int            nCommentChars     )
      throws SpiceException
   {
      SPK spk = new SPK( fileName );

      spk.openNew ( internalFileName,
                    nCommentChars     );

      return ( spk );
   }


   /**
   Open an existing SPK file for read access.
   */
   public static SPK openForRead( String fileName )

      throws SpiceException
   {
      SPK spk = new SPK( fileName );

      DAF daf = DAF.openForRead( fileName );

      spk.handle           = daf.getHandle();
      spk.internalFileName = daf.getInternalFileName();
      spk.ND               = daf.getND();
      spk.NI               = daf.getNI();
      spk.readable         = true;
      spk.writable         = false;

      return ( spk );
   }


   //
   // Constructors
   //



   //
   // Instance methods
   //



   /**
   Obtain a set of ID codes of objects for which an SPK file
   contains data.
   */
   public int[] getBodies()

      throws SpiceException
   {
      int   size       = this.countSegments();

      int[] initialSet = new int[0];

      int[] objectSet = CSPICE.spkobj( fileName, size, initialSet );

      return ( objectSet );
   }



   /**
   Obtain a set of ID codes of objects for which an SPK file
   contains data, merged with an existing set of ID codes.

   <p> The result is returned in a new set.
   */
   public int[] getBodies( int[] initialSet )

      throws SpiceException
   {
      int   size      = this.countSegments();

      int[] objectSet = CSPICE.spkobj( fileName, size + initialSet.length,
                                       initialSet );

      return ( objectSet );
   }



   /**
   Obtain a SpiceWindow representing the time coverage provided by
   this SPK for a given object.

   <p> The returned window contains times expressed as seconds past
   J2000 TDB.
   */
   public SpiceWindow getCoverage( int  body )

      throws SpiceException
   {
      int size = 2 * this.countSegments();

      double[] initialWindowArray = new double[0];

      double[] resultArray = CSPICE.spkcov( fileName, body, size,
                                            initialWindowArray );

      SpiceWindow result = new SpiceWindow( resultArray );

      return ( result );
   }


   /**
   Return a SpiceWindow representing the union of a given
   SPICE coverage window with the time coverage provided by this
   SPK for a given object.

   <p> The returned window contains times expressed as seconds past
   J2000 TDB.
   */
   public SpiceWindow getCoverage( int          body,
                                   SpiceWindow  cover )
      throws SpiceException
   {
      int size = 2 * ( this.countSegments() + cover.card() );

      double[] resultArray = CSPICE.spkcov( fileName, body, size,
                                            cover.toArray() );

      SpiceWindow result = new SpiceWindow( resultArray );

      return ( result );
   }





   /**
   Write a type 2 segment to an SPK file.
   */
   public void writeType02Segment ( Body            body,
                                    Body            center,
                                    ReferenceFrame  frame,
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
      // what CSPICE.spkw02 expects.
      //
      for ( int i = 0;  i < nrec;  i++ )
      {
         for ( int j = 0;  j < 3;  j++ )
         {
            System.arraycopy ( ChebyCoeffs[i][j], 0, cdata, to, ncoeff );

            to += ncoeff;
         }
      }

      CSPICE.spkw02 ( this.handle,
                      body.getIDCode(),
                      center.getIDCode(),
                      frame.getName(),
                      first.getTDBSeconds(),
                      last.getTDBSeconds(),
                      segid,
                      intlen.getMeasure(),
                      n,
                      polydg,
                      cdata,
                      btime.getTDBSeconds()   );
   }


   /**
   Write a type 3 segment to an SPK file.
   */
   public void writeType03Segment ( Body            body,
                                    Body            center,
                                    ReferenceFrame  frame,
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
      double[]         cdata      = new double[nrec * 6 * ncoeff];

      //
      // Create a one-dimensional coefficient array, since this is
      // what CSPICE.spkw03 expects.
      //
      for ( int i = 0;  i < nrec;  i++ )
      {
         for ( int j = 0;  j < 6;  j++ )
         {
            System.arraycopy ( ChebyCoeffs[i][j], 0, cdata, to, ncoeff );

            to += ncoeff;
         }
      }

      CSPICE.spkw03 ( this.handle,
                      body.getIDCode(),
                      center.getIDCode(),
                      frame.getName(),
                      first.getTDBSeconds(),
                      last.getTDBSeconds(),
                      segid,
                      intlen.getMeasure(),
                      n,
                      polydg,
                      cdata,
                      btime.getTDBSeconds()   );
   }






   /**
   Write a type 5 segment to an SPK file.
   */
   public void writeType05Segment ( Body                body,
                                    Body                center,
                                    ReferenceFrame      frame,
                                    Time                first,
                                    Time                last,
                                    String              segid,
                                    double              gm,
                                    int                 n,
                                    StateVector[]       states,
                                    Time[]              epochs  )
      throws SpiceException
   {
      int              nstates    = states.length;
      int              to         = 0;
      double[]         stateArray = new double[6 * nstates];
      double[]         epochArray = new double[nstates];

      //
      // Create a one-dimensional state array, since this is
      // what CSPICE.spkw05 expects.
      //
      for ( int i = 0;  i < nstates;  i++ )
      {
         System.arraycopy ( (states[i]).toArray(), 0, stateArray, to, 6 );

         to += 6;

         epochArray[i] = (epochs[i]).getTDBSeconds();
      }


      CSPICE.spkw05 ( this.handle,
                      body.getIDCode(),
                      center.getIDCode(),
                      frame.getName(),
                      first.getTDBSeconds(),
                      last.getTDBSeconds(),
                      segid,
                      gm,
                      n,
                      stateArray,
                      epochArray             );
   }



   /**
   Write a type 9 segment to an SPK file.
   */

   public void writeType09Segment ( Body                body,
                                    Body                center,
                                    ReferenceFrame      frame,
                                    Time                first,
                                    Time                last,
                                    String              segid,
                                    int                 degree,
                                    int                 n,
                                    StateVector[]       states,
                                    Time[]              epochs  )

      throws SpiceException
   {
      int              nstates    = states.length;
      int              to         = 0;
      double[]         stateArray = new double[6 * nstates];
      double[]         epochArray = new double[nstates];

      //
      // Create a one-dimensional state array, since this is
      // what CSPICE.spkw09 expects.
      //
      for ( int i = 0;  i < nstates;  i++ )
      {
         System.arraycopy ( (states[i]).toArray(), 0, stateArray, to, 6 );

         to += 6;

         epochArray[i] = (epochs[i]).getTDBSeconds();
      }


      CSPICE.spkw09 ( this.handle,
                      body.getIDCode(),
                      center.getIDCode(),
                      frame.getName(),
                      first.getTDBSeconds(),
                      last.getTDBSeconds(),
                      segid,
                      degree,
                      n,
                      stateArray,
                      epochArray             );
   }




   /**
   Write a type 13 segment to an SPK file.
   */

   public void writeType13Segment ( Body                body,
                                    Body                center,
                                    ReferenceFrame      frame,
                                    Time                first,
                                    Time                last,
                                    String              segid,
                                    int                 degree,
                                    int                 n,
                                    StateVector[]       states,
                                    Time[]              epochs  )

      throws SpiceException
   {
      int              nstates    = states.length;
      int              to         = 0;
      double[]         stateArray = new double[6 * nstates];
      double[]         epochArray = new double[nstates];

      //
      // Create a one-dimensional state array, since this is
      // what CSPICE.spkw13 expects.
      //
      for ( int i = 0;  i < nstates;  i++ )
      {
         System.arraycopy ( (states[i]).toArray(), 0, stateArray, to, 6 );

         to += 6;

         epochArray[i] = (epochs[i]).getTDBSeconds();
      }


      CSPICE.spkw13 ( this.handle,
                      body.getIDCode(),
                      center.getIDCode(),
                      frame.getName(),
                      first.getTDBSeconds(),
                      last.getTDBSeconds(),
                      segid,
                      degree,
                      n,
                      stateArray,
                      epochArray             );
   }


}
