

package spice.basic;

import spice.basic.CSPICE;

/**
Class TDTTime implements the representation of time as
seconds past J2000 TDT (Terrestrial Dynamical Time).

<p> A TDTTime instance can be converted to any other
{@link spice.basic.Time} subclass by passing the
instance to a constructor of that subclass.

<p> Class TDTTime provides a convenient way to create
a {@link spice.basic.Time} value from a double precision
number representing seconds past J2000 TDT.

<p> Class TDTTime also provides a convenient way to
perform arithmetic using TDT time values.

<p> Version 1.0.0 05-DEC-2009 (NJB)
*/

public class TDTTime extends Time
{

   /*
   Instance variables
   */
   private             double secPastJ2000TDT;





   /*
   Constructors
   */

   /**
   Construct a TDTTime from a double precision count of
   seconds past J2000 TDT.
   */
   public TDTTime ( double TDTsecondsPastJ2000 )
   {
      secPastJ2000TDT = TDTsecondsPastJ2000;
   }


   /**
   Create a TDTTime instance from a calendar, DOY, or Julian date
   string accepted by STR2ET.
   */
   public TDTTime ( String timeString )

      throws SpiceException

   {
     double  secPastJ2000TDB = CSPICE.str2et ( timeString );

     secPastJ2000TDT         = CSPICE.unitim ( secPastJ2000TDB, "TDB", "TDT" );
   }


   /**
   Copy constructor:  create a new TDTTime value from another.
   */
   public TDTTime ( TDTTime t )
   {
      this.secPastJ2000TDT = t.secPastJ2000TDT;
   }


   /**
   Universal constructor: create a TDTTime from any {@link spice.basic.Time}
   instance.

   <p> This constructor performs all supported time conversions that produce
   a result expressed as seconds past J2000 TDT.
   */
   public TDTTime ( Time t )

      throws SpiceException
   {
      double inputTDB      = t.getTDBSeconds();

      this.secPastJ2000TDT =  CSPICE.unitim ( inputTDB, "TDB", "TDT" );
   }





   /*
   Instance methods
   */

   /**
   Retrieve seconds past J2000 TDT.
   */
   public double getTDTSeconds()
   {
      return ( secPastJ2000TDT );
   }


   /**
   Subtract a {@link spice.basic.Time} instance from this
   TDTTime, producing a TDTDuration.
   */
   public TDTDuration sub ( Time  t )

      throws SpiceException
   {
      TDTTime input = new TDTTime( t );

      double diff   = secPastJ2000TDT - input.secPastJ2000TDT;


      return (  new TDTDuration( diff )  );
   }


   /**
   Add a Duration to a TDTTime.
   */
   public TDTTime add ( Duration d )

      throws SpiceException
   {
      //
      // Convert the input duration to an offset from this
      // epoch, measured in TDT seconds.
      //
      TDTDuration TDTOffset =  new TDTDuration ( d, this );

      double TDTseconds     =  secPastJ2000TDT + TDTOffset.getMeasure();

      return ( new TDTTime(TDTseconds) );
   }


   /**
   Subtract a Duration from a TDTTime.
   */
   public TDTTime sub ( Duration d )

      throws SpiceException
   {
      //
      // Just add the negative of the input Duration to
      // this TDTTime.
      //
      return (  this.add( d.negate() )  );
   }




   /**
   Test two TDTTimes for equality.
   */
   public boolean equals( Object obj )
   {
       if (  !( obj instanceof TDTTime )  )
       {
          return false;
       }

       return (     ( (TDTTime)obj ).secPastJ2000TDT
                 ==   this.secPastJ2000TDT          );
   }


   /**
   Return hash code for a TDBTime object.  This method is overridden
   to support the overridden equals( Object ) method.
   */
   public int hashCode()
   {
      /*
      The hashcode value is the hash code of the double precision
      member value secPastJ2000TDT.
      */
      return (   ( new Double( secPastJ2000TDT ) ).hashCode()  );
   }



   /**
   Convert a TDTTime to a formatted time string using a format picture
   accepted by TIMOUT.
   */
   public String toString ( String picture )

      throws SpiceException

   {
      return (  ( new TDBTime(this) ).toString(picture)  );
   }


   /**
   Convert a TDBTime to a formatted time string using a format code and
   an integer precision level for fractional seconds.
   */
   public String toUTCString ( String    format,
                               int       precision )
      throws SpiceException

   {
      return (  ( new TDBTime(this) ).toUTCString(format, precision)  );
   }


   /**
   Convert a TDTTime to a formatted time string using a default picture.
   Note:  this method overrides Object's "toString" method, and
   as such cannot throw a SpiceErrorException.  Instead, if a
   conversion error occurs, the associated message is returned.
   */
   public String toString ()
   {
      String picture = "YYYY MON DD HR:MN:SC.######::TDT::RND TDT";
      String timstr  = null;

      try
      {
         timstr = this.toString( picture );
      }
      catch ( SpiceException se )
      {
         timstr =   "Could not convert time value. SPICE error "
                  + "diagnostic  was <"
                  + se.getMessage()
                  + ">";
      }
      return ( timstr );
   }


   /**
   Express a TDTTime as a count of TDB seconds past J2000 TDB.
   */
   public double getTDBSeconds()

      throws SpiceException
   {
      double secPastJ2000TDB = CSPICE.unitim ( secPastJ2000TDT, "TDT", "TDB" );

      return ( secPastJ2000TDB );
   }

}


