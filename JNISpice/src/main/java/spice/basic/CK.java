

package spice.basic;


/**
Class CK provides methods for writing, summarizing,
and conducting low-level read operations on CK files.

<p>
To make CK data available to programs for frame
transformations, use the method
{@link spice.basic.KernelDatabase#load(java.lang.String)}.

<p> Version 1.0.0 04-JAN-2010 (NJB)
*/
public class CK extends DAF
{

   //
   // Private methods
   //

   /**
   Construct an CK instance representing a file.
   The file need not exist.
   */


   private CK ( String fileName )
   {
      super ( fileName );
   }


   /**
   Open a new CK file. The file must not exist prior
   to this method call.
   */
   private void openNew( String         internalFileName,
                         int            nCommentChars )

      throws SpiceException
   {
      this.handle = CSPICE.ckopn ( this.fileName,
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
   Create a new CK file. The file must not exist prior
   to this method call.
   */
   public static CK openNew ( String         fileName,
                              String         internalFileName,
                              int            nCommentChars     )
      throws SpiceException
   {
      CK ck = new CK( fileName );

      ck.openNew ( internalFileName, nCommentChars );

      return ( ck );
   }


   /**
   Open an existing CK file for read access.
   */
   public static CK openForRead( String fileName )

      throws SpiceException
   {
      CK ck = new CK( fileName );

      DAF daf = DAF.openForRead( fileName );

      ck.handle           = daf.getHandle();
      ck.internalFileName = daf.getInternalFileName();
      ck.ND               = daf.getND();
      ck.NI               = daf.getNI();
      ck.readable         = true;
      ck.writable         = false;

      return ( ck );
   }




   /**
   Open an existing CK for write access.

   <p> Note that a CK cannot be opened for write access
   if it has already been opened for read access.
   */
   public static CK openForWrite( String fileName )

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
      // Create a new CK instance.
      //
      CK ck = new CK( fileName );

      //
      // If the file is already open for read access, the following
      // call will fail.
      //
      ck.handle = CSPICE.dafopw( fileName );

      //
      // Obtain the DAF descriptor parameters ND and NI,
      // since these will be used for descriptor unpacking.
      //
      CSPICE.dafrfr ( ck.handle, nd, ni, ifname, fward, bward, free );

      ck.ND               = nd[0];
      ck.NI               = ni[0];

      ck.internalFileName = ifname[0];

      ck.writable         = true;

      return ( ck );
   }



   //
   // Constructors
   //




   //
   // Instance methods
   //

   /**
   Obtain a set of ID codes of objects for which an CK file
   contains data.
   */
   public int[] getInstruments()

      throws SpiceException
   {
      int   size       = this.countSegments();

      int[] initialSet = new int[0];

      int[] objectSet = CSPICE.ckobj( fileName, size, initialSet );

      return ( objectSet );
   }


   /**
   Obtain a set of ID codes of objects for which an CK file
   contains data, merged with an existing set of ID codes.

   <p> The result is returned in a new set.
   */
   public int[] getInstruments( int[] initialSet )

      throws SpiceException
   {
      int   size      = this.countSegments();

      int[] objectSet = CSPICE.ckobj( fileName, size + initialSet.length,
                                      initialSet );

      return ( objectSet );
   }

   /**
   Obtain a SpiceWindow representing the time coverage provided by
   this CK for a given instrument.
   */
   public SpiceWindow getCoverage( Instrument    instrument,
                                   boolean       needav,
                                   String        level,
                                   SCLKDuration  tol,
                                   TimeSystem    timsys,
                                   int           nintvls    )
      throws SpiceException
   {
      //
      // Validate tolerance: the SCLK associated with the
      // tolerance must match that associated with the
      // instrument.
      //

      if (     instrument.getSCLK().getIDCode()
               !=  tol.getSCLK().getIDCode()     )
      {
         String msg = "SCLK associated with Instrument " +
                      instrument.getIDCode() + " is " +
                      instrument.getSCLK().getIDCode() + " " +
                      "while SCLK associated with the input " +
                      "tolerance is " + tol.getSCLK().getIDCode();

         SpiceException exc = SpiceErrorException.create(

            "CK.getCoverage", "SPICE(CLOCKMISMATCH)", msg );

         throw ( exc );
      }

      double[] initialWindow = new double[0];

      double[] coverArray = CSPICE.ckcov( fileName,
                                          instrument.getIDCode(),
                                          needav,
                                          level,
                                          tol.getMeasure(),
                                          timsys.toString(),
                                          2*nintvls,
                                          initialWindow       );

      SpiceWindow cover = new SpiceWindow( coverArray );

      return ( cover );
   }



   /**
   Obtain a SpiceWindow representing the time coverage provided by
   this CK for a given instrument; return the union of this window
   with a pre-existing coverage window.

   <p> Note that the resulting window will be meaningful only if
   the input window is compatible with the requested coverage
   representation for this CK instance: specifically, the instruments,
   coverage levels, tolerances, time systems, and "need angular velocity"
   flags must match.
   */
   public SpiceWindow getCoverage( Instrument    instrument,
                                   boolean       needav,
                                   String        level,
                                   SCLKDuration  tol,
                                   TimeSystem    timsys,
                                   SpiceWindow   cover,
                                   int           nintvls    )
      throws SpiceException
   {
      //
      // Validate tolerance: the SCLK associated with the
      // tolerance must match that associated with the
      // instrument.
      //

      if (     instrument.getSCLK().getIDCode()
               !=  tol.getSCLK().getIDCode()     )
      {
         String msg = "SCLK associated with Instrument " +
                      instrument.getIDCode() + " is " +
                      instrument.getSCLK().getIDCode() + " " +
                      "while SCLK associated with the input " +
                      "tolerance is " + tol.getSCLK().getIDCode();
         SpiceException exc = SpiceErrorException.create(

            "CK.getCoverage", "SPICE(CLOCKMISMATCH)", msg );

         throw ( exc );
      }

      double[] initialWindow = cover.toArray();

      double[] coverArray = CSPICE.ckcov( fileName,
                                          instrument.getIDCode(),
                                          needav,
                                          level,
                                          tol.getMeasure(),
                                          timsys.toString(),
                                          2*nintvls,
                                          initialWindow       );

      SpiceWindow outCover = new SpiceWindow( coverArray );

      return ( outCover );
   }



   /**
   Write a type 2 segment to an CK file.
   */
   public void writeType02Segment ( Time               first,
                                    Time               last,
                                    Instrument         inst,
                                    ReferenceFrame     frame,
                                    String             segid,
                                    Time[]             startTimes,
                                    Time[]             stopTimes,
                                    SpiceQuaternion[]  quats,
                                    Vector3[]          avvs,
                                    double[]           rates     )


      throws SpiceException
   {
      SCLK             clock       = inst.getSCLK();

      int              nrec        = startTimes.length;

      int              to;

      double           firstSCLK;
      double           lastSCLK;

      double[]         avArray     = new double[3 * nrec];
      double[]         quatArray   = new double[4 * nrec];
      double[]         startArray  = new double[nrec];
      double[]         stopArray   = new double[nrec];


      //
      // Create a one-dimensional quaternion array, since this is
      // what CSPICE.ckw02 expects.
      //
      to = 0;

      for ( int i = 0;  i < nrec;  i++ )
      {
         System.arraycopy ( quats[i].toArray(), 0, quatArray, to, 4 );

         to += 4;
      }

      //
      // Same deal for angular velocity.
      //
      to = 0;

      for ( int i = 0;  i < nrec;  i++ )
      {
         System.arraycopy ( avvs[i].toArray(), 0, avArray, to, 3 );

         to += 3;
      }


      //
      // Copy the interval start and stop tick values to respective arrays.
      //
      for ( int i = 0;  i < nrec;  i++ )
      {
         startArray[i] = ( new SCLKTime(clock, startTimes[i]) ).
                               getContinuousTicks();
         stopArray [i] = ( new SCLKTime(clock, stopTimes [i]) ).
                               getContinuousTicks();
      }

      firstSCLK = ( new SCLKTime(clock, first) ).getContinuousTicks();
      lastSCLK  = ( new SCLKTime(clock, last ) ).getContinuousTicks();

      CSPICE.ckw02 ( this.handle,
                     firstSCLK,
                     lastSCLK,
                     inst.getIDCode(),
                     frame.getName(),
                     segid,
                     nrec,
                     startArray,
                     stopArray,
                     quatArray,
                     avArray,
                     rates            );
   }






   /**
   Write a type 3 segment to an CK file.
   */
   public void writeType03Segment ( Time               first,
                                    Time               last,
                                    Instrument         inst,
                                    ReferenceFrame     frame,
                                    boolean            avflag,
                                    String             segid,
                                    Time[]             timeTags,
                                    SpiceQuaternion[]  quats,
                                    Vector3[]          avvs,
                                    Time[]             startTimes   )


      throws SpiceException
   {
      SCLK             clock       = inst.getSCLK();

      int              nrec        = timeTags.length;
      int              nints       = startTimes.length;

      int              to;

      double           firstSCLK;
      double           lastSCLK;

      double[]         avArray     = new double[3 * nrec];
      double[]         quatArray   = new double[4 * nrec];
      double[]         sclkdpArray = new double[nrec];
      double[]         startArray  = new double[nints];

      //
      // Create a one-dimensional quaternion array, since this is
      // what CSPICE.ckw03 expects.
      //
      to = 0;

      for ( int i = 0;  i < nrec;  i++ )
      {
         System.arraycopy ( quats[i].toArray(), 0, quatArray, to, 4 );

         to += 4;
      }

      //
      // Same deal for angular velocity.
      //
      if ( avflag )
      {
         to = 0;

         for ( int i = 0;  i < nrec;  i++ )
         {
            System.arraycopy ( avvs[i].toArray(), 0, avArray, to, 3 );

            to += 3;
         }
      }


      //
      // Copy the tick values to an array.
      //
      for ( int i = 0;  i < nrec;  i++ )
      {
         sclkdpArray[i] = ( new SCLKTime(clock, timeTags[i]) ).
                                getContinuousTicks();
      }

      //
      // Copy the interval start times to an array.
      //
      for ( int i = 0;  i < nints;  i++ )
      {
         startArray[i] = ( new SCLKTime(clock, startTimes[i]) ).
                               getContinuousTicks();
      }

      firstSCLK = ( new SCLKTime(clock, first) ).getContinuousTicks();
      lastSCLK  = ( new SCLKTime(clock, last ) ).getContinuousTicks();

      CSPICE.ckw03 ( this.handle,
                     firstSCLK,
                     lastSCLK,
                     inst.getIDCode(),
                     frame.getName(),
                     avflag,
                     segid,
                     nrec,
                     sclkdpArray,
                     quatArray,
                     avArray,
                     nints,
                     startArray    );
   }







   /**
   Count the segments in an CK file.
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




