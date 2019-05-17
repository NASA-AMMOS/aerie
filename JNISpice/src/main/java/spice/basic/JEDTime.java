

package spice.basic;

import spice.basic.CSPICE;
import static spice.basic.TimeConstants.*;

/**
Class JEDime represents times as Julian ephemeris dates.

<p> Version 1.0.0 22-DEC-2009 (NJB)
*/


public class JEDTime extends Time
{

   /*
   Instance variables
   */
   private double           JED;


   /*
   Constructors
   */

   /**
   Construct a JEDTime from a double precision Julian ephemeris
   date having units of Julian days.

   <p> Note that the input is a primitive type, so no semantic
   checking is possible. The user must ensure that the input value
   has the correct reference epoch and time system.
   */
   public JEDTime ( double  JED )
   {
      this.JED = JED;
   }

   /**
   Copy constructor:  create a new JEDTime value from another.
   */
   public JEDTime ( JEDTime t )
   {
      this.JED = t.JED;
   }


   /**
   Universal constructor: create a JEDTime from any {@link spice.basic.Time}
   instance.

   <p> This constructor performs all supported time conversions that produce
   a result expressed as seconds past J2000 TDB.
   */
   public JEDTime ( Time t )

      throws SpiceException
   {
      double secPastJ2000TDB  = t.getTDBSeconds();
      double daysPastJ2000TDB = secPastJ2000TDB / SPD;

      JED = J2000 + daysPastJ2000TDB;
   }




   /*
   Instance methods
   */

   /**
   Get the scalar count of days stored in this JED instance.
   */
   public double getDays()
   {
      return JED;
   }

   /**
   Subtract a {@link spice.basic.Time} instance from this
   JEDTime, producing a JEDDuration.
   */
   public JEDDuration sub ( Time  t )

      throws SpiceException
   {
      double inputJED = J2000 +  ( t.getTDBSeconds() / SPD );

      double diff     = this.JED - inputJED;

      return (  new JEDDuration(diff)  );
   }


   /**
   Add a Duration to a JEDTime.
   */
   public JEDTime add ( Duration d )

      throws SpiceException
   {
      //
      // Convert the input duration to an offset from this
      // epoch, measured in Julian days.
      //
      double offset = d.getTDBSeconds( this );

      double days   = this.JED + ( offset / SPD );

      return ( new JEDTime(days) );
   }


   /**
   Subtract a Duration from a JEDTime.
   */
   public JEDTime sub ( Duration d )

      throws SpiceException
   {
      //
      // Convert the input duration to an offset from this
      // epoch, measured in Julian days.
      //
      double offset = d.getTDBSeconds( this );

      double days   = this.JED - ( offset / SPD );

      return ( new JEDTime(days) );
    }




   /**
   Test two JEDTimes for equality.
   */
   public boolean equals( Object obj )
   {
       if (  !( obj instanceof JEDTime )  )
       {
          return false;
       }

       return (     ( (JEDTime)obj ).JED
                 ==   this.JED          );
   }


   /**
   Return hash code for a JEDTime object.  This method is overridden
   to support the overridden equals( Object ) method.
   */
   public int hashCode()
   {
      /*
      The hashcode value is the hash code of the double precision
      member value secPastJ2000JED.
      */
      return (   ( new Double( JED ) ).hashCode()  );
   }



   /**
   Convert a JEDTime to a formatted time string using a format picture.
   The format picture may be any supported by the SPICE routine TIMOUT.
   */
   public String toString ( String picture )

      throws SpiceErrorException
   {
      return (  CSPICE.timout ( this.getTDBSeconds(), picture )  );
   }


   /**
   Convert a JEDTime to a formatted time string using a format code and
   an integer precision level for fractional seconds.
   */
   public String toUTCString ( String    format,
                               int       precision )

      throws SpiceErrorException
   {
      return (  CSPICE.et2utc ( this.getTDBSeconds(), format, precision )  );
   }


   /**
   Convert a JEDTime to a formatted time string using a default picture.
   Note:  this method overrides Object's "toString" method, and
   as such cannot throw a SpiceErrorException.  Instead, if a
   conversion error occurs, the associated message is returned.
   */
   public String toString ()
   {
      String outStr;

      try
      {
         outStr = String.format ( "%30.9f (JED)", JED );
      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr.trim() );
   }


   /**
   Express a JEDTime as a count of TDB seconds past J2000 TDB.
   */
   public double getTDBSeconds ()
   {
      return ( SPD*(JED-J2000) );
   }

}


