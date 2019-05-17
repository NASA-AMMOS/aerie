
package spice.basic;

/**
Class TDBDuration measures time intervals in
units of TDB seconds.


<p> Version 1.0.0 28-NOV-2009 (NJB)
*/
public class TDBDuration extends Duration
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
   public TDBDuration()
   {
      measure = 0.0;
   }


   /**
   Construct a TDBDuration from a double precision count of
   seconds past J2000 TDB.
   */
   public TDBDuration ( double seconds )
   {
      measure = seconds;
   }


   /**
   Copy constructor.
   */
   public TDBDuration( TDBDuration d )
   {
      measure = d.measure;
   }


   /**
   Create a TDBDuration from any Duration subclass and
   a start time.
   */
   public TDBDuration ( Duration d,
                        Time     startTime )

      throws SpiceException
   {
      measure = d.getTDBSeconds( startTime );
   }



   //
   // Methods
   //


   /**
   Return the measure of a TDBDuration. Units
   are TDB seconds.
   */
   public double getMeasure()

      throws SpiceException
   {
      return ( measure );
   }

   /**
   Convert this instance to a count of TDB seconds.

   <p> The signature of this method includes an input
   Time because this
   input is present in the {@link spice.basic.Duration}
   superclass version of this method. This input argument
   is not used in the method's implementation.
   */

   public double getTDBSeconds ( Time  startTime )

      throws SpiceException
   {
      return ( measure );
   }


   /**
   Add a TDBDuration to this instance.
   */
   public TDBDuration add ( TDBDuration d )
   {
      double sum = this.measure + d.measure;

      return (  new TDBDuration(sum) );
   }


   /**
   Subtract a TDBDuration from this instance.
   */
   public TDBDuration sub ( TDBDuration d )
   {
      double diff = this.measure - d.measure;

      return (  new TDBDuration(diff) );
   }


   /**
   Negate a TDBDuration.
   */
   public TDBDuration negate()
   {
      return (  new TDBDuration( -this.measure ) );
   }


   /**
   Scale a TDBDuration.
   */
   public TDBDuration scale ( double s )
   {
      return (  new TDBDuration( s * this.measure ) );
   }



}
