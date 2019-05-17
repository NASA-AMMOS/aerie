
package spice.basic;

import spice.basic.CSPICE;

/**
Class PointingRecord encapsulates outputs from C-kernel
lookups.

<p> Version 1.0.0 15-DEC-2009 (NJB)
*/
public class PointingRecord extends Object
{

   //
   // Fields
   //

   //
   // These fields have package-private access so this class may
   // be subclassed.
   //

   boolean            pointingFound;
   Matrix33           CMatrix;
   Instrument         requestInst;
   ReferenceFrame     ref;
   SCLKDuration       requestTol;
   SCLKTime           requestSCLKTime;
   SCLKTime           actualSCLKTime;


   //
   // Constructors
   //




   /**
   No-arguments constructor.
   */
   public PointingRecord()
   {
      pointingFound   = false;
      CMatrix         = null;
      requestInst     = null;
      ref             = null;
      requestTol      = null;
      requestSCLKTime = null;
      actualSCLKTime  = null;
   }



   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public PointingRecord( PointingRecord rec )

      throws SpiceException
   {
      this.pointingFound   = rec.pointingFound;
      this.CMatrix         = new Matrix33      ( rec.CMatrix         );
      this.requestInst     = new Instrument    ( rec.requestInst     );
      ref                  = new ReferenceFrame( rec.ref             );
      requestTol           = new SCLKDuration  ( rec.requestTol      );
      requestSCLKTime      = new SCLKTime      ( rec.requestSCLKTime );
      actualSCLKTime       = new SCLKTime      ( rec.actualSCLKTime  );
   }



   /**
   Return a PointingRecord for a specified instrument
   or structure at a given time, using a specified
   lookup tolerance.
   */
   public PointingRecord ( Instrument      inst,
                           Time            t,
                           ReferenceFrame  ref,
                           Duration        tolerance )

      throws SpiceException
   {

      //
      // We'll convert the input Time to encoded SCLK
      // and the input duration to ticks. In order to
      // do this, we'll need the SCLK ID code associated
      // with the instrument.
      //

      //
      // The instrument becomes part of the pointing record.
      //
      requestInst     =  new Instrument( inst );

      SCLK clockID    = inst.getSCLK();

      //
      // The request time becomes part of the pointing record.
      //
      requestSCLKTime = new SCLKTime( clockID, t );

      double sclkdp   = requestSCLKTime.getContinuousTicks();

      //
      // The tolerance becomes part of the pointing record.
      //
      requestTol      = new SCLKDuration( clockID, tolerance, t );

      double dpTol    = requestTol.getMeasure();



      //
      // The reference frame becomes part of the pointing record.
      //
      this.ref       = new ReferenceFrame( ref );


      //
      // Declare outputs for CSPICE.ckgp call.
      //
      double[]   clkout    = new double[1];
      double[][] cmat      = new double[3][3];
      boolean[]  foundArr  = new boolean[1];


      CSPICE.ckgp ( inst.getIDCode(), sclkdp, dpTol,
                    ref.getName(),    cmat,   clkout, foundArr );

      //
      // The found flag becomes part of the pointing record.
      //
      pointingFound = foundArr[0];

      if ( pointingFound )
      {
          CMatrix        = new Matrix33 ( cmat );
          actualSCLKTime = new SCLKTime ( clockID, clkout[0] );
      }
      else
      {
          CMatrix        = null;
          actualSCLKTime = null;
      }

      //
      // All seven components of the pointing record are set.
      //
   }



   //
   // Methods
   //

   /**
   Retrieve the found flag from a PointingRecord. The caller should always
   test the found flag before attempting to extract data from the pointing
   record.
   */
   public boolean wasFound()
   {
      return ( pointingFound );
   }


   /**
   Obtain a C-matrix from a PointingRecord. The matrix
   is available only if pointing was found when the
   record was created.
   */
   public Matrix33 getCMatrix()

      throws PointingNotFoundException, SpiceException
   {
      if ( !pointingFound )
      {
         PointingNotFoundException exc = PointingNotFoundException.create(

            "PointingRecord.getCMatrix",

            "Pointing was not found for instrument " + requestInst +
            " at time " + requestSCLKTime.getString() + " using " +
            "a tolerance of " + requestTol + " ticks."                );


         throw ( exc );
      }

      return (  new Matrix33( CMatrix )  );
   }

   /**
   Obtain the actual SCLK epoch from a PointingRecord. The epoch
   is available only if pointing was found when the
   record was created.
   */
   public SCLKTime getActualSCLKTime()

      throws PointingNotFoundException, SpiceException
   {
      if ( !pointingFound )
      {
         PointingNotFoundException exc = PointingNotFoundException.create(

            "PointingRecord.getActualTime",

            "Pointing was not found for instrument " + requestInst +
            " at time " + requestSCLKTime.getString() + " using " +
            "a tolerance of " + requestTol + " ticks."                );


         throw ( exc );
      }

      return (  new SCLKTime( actualSCLKTime )  );
   }


   /**
   Get the ReferenceFrame from a pointing record. This is the
   base frame for the C-matrix; the C-matrix maps vectors from
   the base frame to the instrument frame via left multiplication:
   <pre>
      V           = C-matrix * V
       instrument               base
   </pre>
   */
   public ReferenceFrame getReferenceFrame()

      throws SpiceException
   {
      return (  new ReferenceFrame( ref )  );
   }


   /**
   Get the Instrument from a pointing record.
   */
   public Instrument getInstrument()

      throws SpiceException
   {
      return (  new Instrument( requestInst )  );
   }



   /**
   Get the request SCLK time from a pointing record.
   */
   public SCLKTime getRequestSCLKTime()

      throws SpiceException
   {
      return (  new SCLKTime( requestSCLKTime )  );
   }



   /**
   Get the tolerance from a pointing record; the value is
   expressed in ticks of the clock associated with the
   record's Instrument.
   */
   public SCLKDuration getTolerance()
   {
      return (  new SCLKDuration( requestTol )  );
   }


}
