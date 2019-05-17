
package spice.basic;

import static spice.basic.TimeConstants.*;

/**
Class JEDDuration measures time intervals in
units of Julian ephemeris days; the time
system used for this measurement is TDB.

<p> Version 1.0.0 28-NOV-2009 (NJB)
*/
public class JEDDuration extends Duration
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
   public JEDDuration()
   {
   }


   /**
   Construct a JEDDuration from a double precision count of
   Julian ephemeris days.
   */
   public JEDDuration ( double days )
   {
      measure = days;
   }


   /**
   Copy constructor.
   */
   public JEDDuration( JEDDuration d )
   {
      measure = d.measure;
   }


   /**
   Create a TDTDuration from any Duration subclass and
   a start Time.
   */
   public JEDDuration ( Duration d,
                        Time     startTime )

      throws SpiceException
   {
      double TDBOffset = d.getTDBSeconds( startTime );

      measure = TDBOffset / SPD;
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
   public double getTDBSeconds ( Time   startTime )

      throws SpiceException
   {
      //
      // The input argument is needed only because this
      // method implements the corresponding method of
      // class Duration.
      //

      return (  (this.measure)*SPD  );
   }


   /**
   Add a JEDDuration to this instance.
   */
   public JEDDuration add ( JEDDuration d )
   {
      double sum = this.measure + d.measure;

      return (  new JEDDuration(sum) );
   }


   /**
   Subtract a JEDDuration from this instance.
   */
   public JEDDuration sub ( JEDDuration d )
   {
      double diff = this.measure - d.measure;

      return (  new JEDDuration(diff) );
   }


   /**
   Negate a JEDDuration.
   */
   public JEDDuration negate()
   {
      return (  new JEDDuration( -this.measure ) );
   }


   /**
   Scale a JEDDuration.
   */
   public JEDDuration scale ( double s )
   {
      return (  new JEDDuration( s * this.measure ) );
   }

}
