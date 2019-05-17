
package spice.basic;

/**
Class SCLKDuration measures time intervals in
units of SCLK ticks, where the ticks are associated
with a specified clock.


<p> Version 1.0.0 27-DEC-2009 (NJB)
*/
public class SCLKDuration extends Duration
{
   //
   // Fields
   //
   private SCLK                 clockID;
   private double               measure;


   //
   // Constructors
   //

   //
   // Note that there is no reasonable defintion
   // for a no-arguments SCLKDuration constructor,
   // since the clock can't be specified.
   //



   /**
   Construct a SCLKDuration from a clock ID and a tick count.
   */
   public SCLKDuration ( SCLK       clock,
                         double     ticks    )
   {
      this.clockID = clock;
      measure      = ticks;
   }


   /**
   Construct a SCLKDuration from a clock ID and an SCLK duration string.
   */
   public SCLKDuration ( SCLK       clock,
                         String     clkstr )

      throws SpiceException
   {
      this.clockID = clock;

      measure      = CSPICE.sctiks( clock.getIDCode(),
                                    clkstr              );
   }


   /**
   Copy constructor.
   */
   public SCLKDuration( SCLKDuration d )
   {
      clockID = d.clockID;
      measure = d.measure;
   }


   /**
   Create a SCLKDuration from an SCLK ID, any Duration subclass and
   a start time.
   */
   public SCLKDuration ( SCLK     clock,
                         Duration d,
                         Time     startTime )

      throws SpiceException
   {

      //
      // Given initial values to the fields of this instance.
      //
      this.measure = 0.0;
      this.clockID = null;

      //
      // In the special case where the input duration is already
      // a count of ticks of the SCLK designated by `clockID',
      // we can avoid round-off error by avoiding conversion of
      // the duration to TDB.
      //

      if ( ( d     instanceof   SCLKDuration ) &&
           ( clock.getIDCode() == ((SCLKDuration)d).getSCLK().getIDCode() ) )
      {
         //
         // The input duration value is for the SCLK designated by `clock'.
         // We can simply copy the duration value. This avoids introducing
         // unnecessary round-off error.
         //
         this.measure = ((SCLKDuration)d).getMeasure();
         this.clockID = clock;
      }
      else
      {
         double startTDBSeconds = startTime.getTDBSeconds();
         double stopTDBSeconds  = ( startTime.add(d) ).getTDBSeconds();

         int    sc              = clock.getIDCode();

         double startTicks      = CSPICE.sce2c( sc, startTDBSeconds );
         double stopTicks       = CSPICE.sce2c( sc, stopTDBSeconds  );

         this.measure           = stopTicks - startTicks;
         this.clockID           = clock;
      }
   }



   //
   // Methods
   //

   /**
   Return the SCLK ID of a SCLKDuration. This method
   returns a deep copy.
   */
   public SCLK getSCLK()

      throws SpiceException
   {
      return ( new SCLK(this.clockID) );
   }

   /**
   Return the measure of a SCLKDuration. Units
   are SCLK ticks.
   */
   public double getMeasure()

      throws SpiceException
   {
      return ( measure );
   }


   /**
   Convert this instance to a count of TDB seconds, given
   a start time.
   */
   public double getTDBSeconds ( Time   startTime )

      throws SpiceException
   {
      double startTDBSeconds = startTime.getTDBSeconds();

      int    sc          = clockID.getIDCode();

      double startTicks  = CSPICE.sce2c( sc, startTDBSeconds );
      double endTicks    = startTicks + this.measure;
      double endTDB      = CSPICE.sct2e( sc, endTicks );
      double diff        = endTDB - startTDBSeconds;

      return ( diff );
   }


   /**
   Add a SCLKDuration to this instance.
   */
   public SCLKDuration add ( SCLKDuration d )

      throws SpiceException
   {
      if ( this.clockID != d.clockID )
      {
         String msg = "SCLKDurations can be added only if their " +
                      "clock IDs match. The ID of this instance is " +
                      this.clockID + "; the ID of the input instance " +
                      "is " + d.clockID;

         SpiceException exc = new SpiceException ( msg );

         throw ( exc );
      }

      double sum = this.measure + d.measure;

      return (  new SCLKDuration( this.clockID, sum) );
   }


   /**
   Subtract a SCLKDuration from this instance.
   */
   public SCLKDuration sub ( SCLKDuration d )

      throws SpiceException
   {
      if ( this.clockID != d.clockID )
      {
         String msg = "SCLKDurations can be subtracted only if their " +
                      "clock IDs match. The ID of this instance is " +
                      this.clockID + "; the ID of the input instance " +
                      "is " + d.clockID;

         SpiceException exc = new SpiceException ( msg );

         throw ( exc );
      }

      double diff = this.measure - d.measure;

      return (  new SCLKDuration( this.clockID, diff) );
   }


   /**
   Negate a SCLKDuration.
   */
   public SCLKDuration negate()
   {
      return (  new SCLKDuration( this.clockID, -this.measure ) );
   }


   /**
   Scale a SCLKDuration.
   */
   public SCLKDuration scale ( double s )
   {
      return (  new SCLKDuration( this.clockID,  s * this.measure )  );
   }


   /**
   Convert a non-negative SCLKDuration to a string.

   <p>
   Design topic: it would be convenient and expected for this
   functionality to be provided via an instance rather than a
   class method. But for that to be feasible, the method would
   need to work with negative durations.

   <p> So, should signed SCLK duration strings be supported?
   */
   public static String format( SCLKDuration d )

      throws SpiceException
   {
      String result = CSPICE.scfmt( d.clockID.getIDCode(), d.getMeasure() );

      return( result );
   }


}
