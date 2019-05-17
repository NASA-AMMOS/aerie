
package spice.basic;


/**
Class Time is an abstract superclass that represents absolute times used by
JNISpice.

<p> The subclasses of Time
<ul>
<li> {@link spice.basic.TDBTime}
<li> {@link spice.basic.TDTTime}
<li> {@link spice.basic.JEDTime}
<li> {@link spice.basic.SCLKTime}
</ul>
implement time representations and provide time string
parsing, conversion, and output formatting methods.

<p> In the JNISpice Toolkit, class {@link spice.basic.TDBTime}
plays the role that "seconds past J2000 TDB" does in the underlying CSPICE
Toolkit: TDB is the time system of the independent
time variable for all geometric computations other than
interpolation of pointing data obtained from C-kernels.
However, JNISpice is more flexible than CSPICE in the way its
APIs accept input time values: most high-level JNISpice methods that
accept input times
specify those arguments as having type Time, so any subclass of Time may
be used as an actual input.

<p> Time arithmetic is supported by all Time subclasses: a Time instance may
be subtracted from another, yielding a {@link spice.basic.Duration}, and
a Duration may be added to or subtracted from any Time instance.

<p> Normally input calendar format or DOY (day of year) time strings
that represent UTC, TDB, or TDT times should be passed to the
{@link spice.basic.TDBTime#TDBTime(java.lang.String)} constructor, since
this usually minimizes the number of conversions that will be needed
when the resulting object is passed as an input to other
JNISpice methods. One exception is the case where the time value is
to be used to perform arithmetic in the TDT time system; then conversion
of the string directly to a TDTTime instance is likely to improve
computational speed and accuracy.

<p> Each Time subclass has a constructor that accepts an input of
type double; these constructors interpret the input as a count
of time units appropriate to their respective classes. In particular,
the interpretation of the input 0.0 is as follows:
<ul>
<li> TDBTime: 0 seconds past J2000 TDB
<li> TDTTime: 0 seconds past J2000 TDT
<li> JEDTime: Julian ephemeris date 0
<li> SCLKTime: 0 ticks of the associated SCLK
</ul>

<p> The numeric constructors simplify various time conversions, since
a numeric value in any system can be converted to a object of another
system via sequential constructor calls. For example:

<p> Convert JED 2451545.0 to seconds past J2000 TDB,
seconds past J2000 TDT, and Cassini SCLK; print the results
(a Cassini SCLK kernel and a leapseconds kernel must be loaded in
order to perform these conversions):
<pre>
   JEDTime jed = new JEDTime(2451545.0);
   System.out.println(  new TDBTime( jed )  );
   System.out.println(  new TDTTime( jed )  );
   System.out.println(  new SCLKTime( new SCLK(-82), jed )  );
</pre>
The output is:
<pre>
   2000 JAN 01 12:00:00.000000 TDB
   2000 JAN 01 12:00:00.000073 TDT
   1/1325419621.115
</pre>

<p> Class Time has no "UTCTime" subclass because numeric forms of UTC
such as "seconds past J2000 UTC"
don't support accurate arithmetic computations,
due to these forms' inability to represent leapseconds. This is true even
for computations involving past times (for which the epochs of leapsecond
additions are known).

<p> The difference between TDB and UTC seconds past J2000, often called
"DUT" or "DELTA ET," can be obtained from {@link spice.basic.CSPICE#deltet}.

<p> Local solar time is not part of the Time class tree; the class
{@link spice.basic.LocalSolarTime} supports this form of time.



<p> Version 1.0.0 21-DEC-2009 (NJB)
*/

public abstract class Time extends Object {

   /*
   Public methods
   */

   /**
   Return a double precision representation of a time value
   as TDB seconds past J2000 TDB.

   <p> All subclasses of Time must implement this method.
   */
   public abstract double getTDBSeconds()

      throws SpiceException;

   /**
   Subtract one Time instance from another, yielding a Duration.
   */
   public abstract Duration sub( Time t2 )

      throws SpiceException;

   /**
   Subtract a Duration from a Time instance, yielding another
   Time instance.
   */
   public abstract Time sub( Duration d )

      throws SpiceException;

   /**
   Add a Duration to a Time instance, yielding another
   Time instance.
   */
   public abstract Time add( Duration d )

      throws SpiceException;




}
