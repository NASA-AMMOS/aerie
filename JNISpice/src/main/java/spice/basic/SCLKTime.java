

package spice.basic;

import java.lang.Math;
import spice.basic.CSPICE;

/**
Class SCLKTime represents times measured by spacecraft
clocks.

<p> This class supports conversion between SCLK strings
and encoded SCLK; it also supports conversion between
encoded SCLK and other {@link spice.basic.Time} classes.

<p> Version 1.0.0 15-DEC-2009 (NJB)
*/


public class SCLKTime extends Time {

   /*
   Instance variables
   */
   private             SCLK    clockID;
   private             double  ticks;


   /*
   Constructors
   */



   /**
   Create an SCLK time from another SCLK time. This
   constructor creates a deep copy.
   */
   public SCLKTime ( SCLKTime t )
   {
      this.clockID = new SCLK( t.clockID );
      this.ticks   = t.ticks;
   }

   /**
   Construct an SCLKTime from an SCLK string.
   */
   public SCLKTime ( SCLK    clock,
                     String  sclkch )

      throws SpiceException

   {
      clockID = new SCLK ( clock );

      ticks   = CSPICE.scencd ( clock.getIDCode(),  sclkch  );
   }



   /**
   Construct an SCLKTime from a Time.
   */
   public SCLKTime ( SCLK  clock,
                     Time  time  )

      throws SpiceException

   {
      clockID = new SCLK ( clock );

      if (    ( time   instanceof   SCLKTime )
           && ( clock.getIDCode() == ((SCLKTime)time).getSCLK().getIDCode() ) )
      {
         //
         // The input time value is for the SCLK designated by `clock'.
         // We can simply copy the tick value. This avoids introducing
         // unnecessary round-off error.
         //

         this.ticks = ((SCLKTime)time).ticks;
      }
      else
      {
         //
         // Convert the input time to TDB seconds and then
         // to ticks for the input SCLK,
         //
         ticks   = CSPICE.sce2c ( clock.getIDCode(),
                                  time.getTDBSeconds () );
      }
   }


   /**
   Construct an SCLKTime from double precision ticks.
   */
   public SCLKTime ( SCLK    clock,
                     double  ticks  )

      throws SpiceException

   {
      //
      // We make sure the tick value is non-negative.
      //
      if ( ticks < 0.0 )
      {
         SpiceException exc = SpiceErrorException.create(

            "SCLKTime",
            "SPICE(INVALIDTICKS)",
            "Tick value must be non-negative but was #." );

         throw( exc );
      }

      clockID    = new SCLK ( clock );

      this.ticks = ticks;
   }


   /*
   Instance methods
   */


   /**
   Get SCLK associated with an SCLKTime.
   */
   public SCLK getSCLK()

      throws SpiceException
   {
      return clockID;
   }


   /**
   Convert an SCLKTime to TDB seconds past J2000 TDB.
   */

   public double getTDBSeconds()

      throws SpiceException

   {
      int clock = clockID.getIDCode();

      return (  CSPICE.sct2e( clock, ticks )  );
   }


   /**
   Convert an SCLKTime to an SCLK string. This method
   throws SpiceException, unlike toString().
   */

   public String getString()

      throws SpiceException

   {
      int clock = clockID.getIDCode();

      return new String (  CSPICE.scdecd ( clock, ticks )  );
   }


   /**
   Convert an SCLKTime to an SCLK string. This method
   overrides Object's toString().
   */
   public String toString()
   {
      String outStr = null;

      try
      {
         outStr = this.getString();
      }
      catch ( SpiceException exc )
      {
         outStr = exc.getMessage();
      }

      return( outStr );
   }



   /**
   Get continuous ticks as a double precision number.
   */
   public double getContinuousTicks()
   {
      return ticks;
   }

   /**
   Get discrete ticks as a double precision number.
   */
   public double getDiscreteTicks()
   {
      return (  Math.rint( ticks )  );
   }


   /**
   Subtract a Time instance from a SCLKTime, producing
   an SCLKDuration.
   */
   public SCLKDuration sub ( Time t )

      throws SpiceException
   {
      SCLKDuration result = null;
      double       diff   = 0.0;

      if (     ( t   instanceof   SCLKTime )
            && ( clockID.getIDCode() == ((SCLKTime)t).getSCLK().getIDCode() ) )
      {
         //
         // The input time value is for the SCLK designated by `clock'.
         // We can simply subtract the tick value. This avoids introducing
         // unnecessary round-off error.
         //
         diff = this.ticks - ((SCLKTime)t).ticks;
      }
      else
      {
         //
         // Convert the input time to an SCLK time for this clock, then
         // perform the subtraction.
         //

         SCLKTime ticks2 = new SCLKTime( clockID, t );

         diff            = this.ticks - ticks2.getContinuousTicks();
      }

      result = new SCLKDuration ( clockID, diff );

      return ( result );
   }




   /**
   Add a Duration to an SCLKTime instance, producing
   another SCLKTime.
   */
   public SCLKTime add ( Duration d )

      throws SpiceException
   {
      SCLKTime result   = null;
      double   tickSum  = 0.0;
      double   secSum   = 0.0;


      if ( ( d   instanceof   SCLKDuration ) &&
           ( clockID.getIDCode() == ((SCLKDuration)d).getSCLK().getIDCode() ) )
      {
         //
         // The input duration is for the SCLK designated by `clock'.
         // We can simply add the duration's tick value. This avoids introducing
         // unnecessary round-off error.
         //
         tickSum = this.ticks + ((SCLKDuration)d).getMeasure();

         result  = new SCLKTime ( clockID, tickSum  );
      }
      else
      {
         //
         // Do the arithmetic using TDB seconds past J2000.
         //

         secSum = this.getTDBSeconds() + d.getTDBSeconds(this);

         result = new SCLKTime ( clockID, new TDBTime(secSum)  );
      }

      return ( result );
   }



   /**
   Subtract a Duration from an SCLKTime instance, producing
   another SCLKTime.
   */
   public SCLKTime sub ( Duration d )

      throws SpiceException
   {
      SCLKTime result    = null;
      double   tickDiff  = 0.0;
      double   secDiff   = 0.0;


      if ( ( d   instanceof   SCLKDuration ) &&
           ( clockID.getIDCode() == ((SCLKDuration)d).getSCLK().getIDCode() ) )
      {
         //
         // The input duration is for the SCLK designated by `clock'.
         // We can simply subtract the duration's tick value. This
         // avoids introducing unnecessary round-off error.
         //
         tickDiff = this.ticks - ((SCLKDuration)d).getMeasure();

         result   = new SCLKTime ( clockID, tickDiff  );
      }
      else
      {
         //
         // Do the arithmetic using TDB seconds past J2000.
         //

         secDiff   = this.getTDBSeconds() - d.getTDBSeconds(this);

         result = new SCLKTime ( clockID, new TDBTime(secDiff)  );
      }

      return ( result );
   }

}
