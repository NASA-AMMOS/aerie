
package spice.basic;

import spice.basic.CSPICE;

/**
Class PointingAndAVRecord encapsulates outputs from C-kernel
pointing and angular velocity lookups.

<p> Most methods of this class are inherited from
{@link spice.basic.PointingRecord}; see that class for
details.

<p> Version 1.0.0 13-DEC-2009 (NJB)
*/
public class PointingAndAVRecord extends PointingRecord
{

   //
   // Fields
   //
   private Vector3            angvel = null;

   //
   // Constructors
   //


   /**
   No-arguments constructor.
   */
   public PointingAndAVRecord()
   {
      super();

      angvel = null;
   }


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public PointingAndAVRecord( PointingAndAVRecord rec )

      throws SpiceException
   {
      this.pointingFound   = rec.pointingFound;
      this.CMatrix         = new Matrix33      ( rec.CMatrix         );
      this.requestInst     = new Instrument    ( rec.requestInst     );
      ref                  = new ReferenceFrame( rec.ref             );
      requestTol           = new SCLKDuration  ( rec.requestTol      );
      requestSCLKTime      = new SCLKTime      ( rec.requestSCLKTime );
      actualSCLKTime       = new SCLKTime      ( rec.actualSCLKTime  );
      angvel               = new Vector3       ( rec.angvel          );
   }





   /**
   Return a PointingAndAVRecord for a specified instrument
   or structure at a given time, using a specified
   lookup tolerance.
   */
   public PointingAndAVRecord ( Instrument      inst,
                                Time            t,
                                ReferenceFrame  ref,
                                Duration        tolerance )

      throws SpiceException
   {
      //
      // Create an uninitialized PointingRecord instance.
      //
      super();

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
      double[]   av        = new double[3];
      double[]   clkout    = new double[1];
      double[][] cmat      = new double[3][3];
      boolean[]  foundArr  = new boolean[1];


      CSPICE.ckgpav ( inst.getIDCode(), sclkdp, dpTol,  ref.getName(),
                      cmat,             av,     clkout, foundArr       );

      //
      // The found flag becomes part of the pointing record.
      //
      pointingFound = foundArr[0];

      if ( pointingFound )
      {
          CMatrix        = new Matrix33 ( cmat );
          angvel         = new Vector3  ( av   );
          actualSCLKTime = new SCLKTime ( clockID, clkout[0] );
      }
      else
      {
          CMatrix        = null;
          angvel         = null;
          actualSCLKTime = null;
      }

      //
      // All eight components of the pointing record are set.
      //
   }



   //
   // Methods
   //


   /**
   Obtain an angular velocity vector from a PointingAndAVRecord. The vector
   is available only if pointing was found when the
   record was created.

   <p> This vector represents the angular velocity of the
   instrument frame relative to the base frame and is expressed
   relative to the base frame.

   <p> The angular velocity vector has units of radians/second.
   */
   public Vector3 getAngularVelocity()

      throws PointingNotFoundException, SpiceException
   {
      if ( !pointingFound )
      {
         PointingNotFoundException exc = PointingNotFoundException.create(

            "PointingRecord.getAngularVelocity",

            "Pointing was not found for instrument " + requestInst +
            " at time " + requestSCLKTime.getString() + " using " +
            "a tolerance of " + requestTol + " ticks."                );

         throw ( exc );
      }

      return (  new Vector3(angvel)  );
   }
}
