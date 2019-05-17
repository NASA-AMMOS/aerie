
package spice.basic;

/**
Class TDTDuration measures time intervals in
units of TDT seconds.

<p> Version 1.0.0 03-NOV-2009 (NJB)
*/
public class TDTDuration extends Duration
{
   //
   // Fields
   //
   private double               measure;


   //
   // Constructors
   //

   /**
   No-arguments constructor.
   */
   public TDTDuration()
   {
      measure = 0.0;
   }


   /**
   Construct a TDTDuration from a double precision count of
   seconds past J2000 TDT.
   */
   public TDTDuration ( double seconds )
   {
      measure = seconds;
   }


   /**
   Copy constructor.
   */
   public TDTDuration( TDTDuration d )
   {
      measure = d.measure;
   }


   /**
   Create a TDTDuration from any Duration subclass and
   a start Time.
   */
   public TDTDuration ( Duration d,
                        Time     startTime )

      throws SpiceException
   {
      //
      // Get the start time as a count of TDB seconds
      // past J2000.
      //
      double startTDBSeconds = startTime.getTDBSeconds();

      //
      // To find the TDT duration corresponding to the
      // inputs, we need to express the start and end
      // times as TDT seconds past J2000. The start
      // time can be translated immediately; the end
      // time will have to be expressed as TDB seconds
      // past J2000 as an intermediate step.
      //
      double startTDT  = toTDTSeconds( startTDBSeconds );

      double TDBOffset = d.getTDBSeconds( startTime );

      double endTDB    = startTDBSeconds + TDBOffset;

      double endTDT    = CSPICE.unitim ( endTDB, "TDB", "TDT" );

      //
      // Finally, compute the TDT duration.
      //
      measure = endTDT - startTDT;
   }



   //
   // Methods
   //


   /**
   Return the measure of a TDTDuration. Units
   are TDT seconds.
   */
   public double getMeasure()

      throws SpiceException
   {
      return ( measure );
   }



   /**
   Convert this instance to a count of of TDB seconds,
   measured relative to a given count of TDB seconds
   past J2000 TDB.
   */
   public double getTDBSeconds ( Time  startTime )

      throws SpiceException
   {
      //
      // Get the start time as a count of TDB seconds
      // past J2000.
      //
      double startTDBSeconds = startTime.getTDBSeconds();

      //
      // Get the start time as a count of TDT seconds
      // past J2000.
      //
      double TDTStart = toTDTSeconds( startTDBSeconds );

      //
      // Add this TDTDuration to the start time to obtain
      // the stop epoch.
      //
      double TDTStop = TDTStart + measure;

      //
      // Converting the start and stop times to TDB seconds
      // past J2000 and finding the difference yields the
      // duration in TDB seconds.
      //
      double TDBStop  = CSPICE.unitim ( TDTStop,  "TDT", "TDB" );


      return (  TDBStop - startTDBSeconds  );
   }


   /**
   Add a TDTDuration to this instance.
   */
   public TDTDuration add ( TDTDuration d )
   {
      double sum = this.measure + d.measure;

      return (  new TDTDuration(sum) );
   }


   /**
   Subtract a TDTDuration from this instance.
   */
   public TDTDuration sub ( TDTDuration d )
   {
      double diff = this.measure - d.measure;

      return (  new TDTDuration(diff) );
   }


   /**
   Negate a TDTDuration.
   */
   public TDTDuration negate()
   {
      return (  new TDTDuration( -this.measure ) );
   }


   /**
   Scale a TDTDuration.
   */
   public TDTDuration scale ( double s )
   {
      return (  new TDTDuration( s * this.measure ) );
   }


   //
   // Private methods
   //

   /**
   Convert a count of TDB seconds past J2000 TDB to
   a count of TDT seconds past J2000 TDT.

   <p> This method is needed to avoid circular class definitions:
   Since TDTTime depends on this class, this class should not
   refer to TDTTime. Hence the constructors of TDTTime are off-limits.
   */

   private double toTDTSeconds( double TDBSeconds )

      throws SpiceErrorException
   {
      double TDTSeconds = CSPICE.unitim ( TDBSeconds, "TDB", "TDT" );

      return ( TDTSeconds );
   }

}
