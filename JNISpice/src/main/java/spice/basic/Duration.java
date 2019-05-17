
package spice.basic;

/**
Class Duration is an abstract superclass of all JNISpice types
that represent durations. Durations represent relative
times: "10 TDB seconds" is an example of a duration;
"100 Cassini SCLK ticks" is another example.

<p> Durations are associated with specific time systems.
JNISpice supports Durations associated with the TDB, TDT,
JED, and SCLK time systems.

<p> Durations are used to perform arithmetic with {@link
spice.basic.Time} instances: a Duration can be added to or
subtracted from a Time instance. Subtracting one Time instance
from another yields a Duration.

<p> An object of a given Duration subclass can be added to or
subtracted from another duration of the same subclass. Arithmetic
on operands of differing duration subclasses is not
supported, hence addition and subtraction methods are not
declared in Duration itself. See the Duration subclasses
<ul>
<li> {@link spice.basic.TDBDuration}
<li> {@link spice.basic.TDTDuration}
<li> {@link spice.basic.JEDDuration}
<li> {@link spice.basic.SCLKDuration}
</ul>
for documentation of their arithmetic methods.


<p> Since the rate at which time progresses in one time
system may not be constant when measured in another,
conversion of Durations between an arbitrary pair of
time systems is meaningful only if a reference time is
supplied. JNISpice requires the start epoch of a Duration
in order to convert the Duration to a given time system.

<p> Version 1.0.0 21-DEC-2009 (NJB)
*/
public abstract class Duration extends Object
{

   /**
   Every Duration subclass must support conversion to a measurement
   expressed as seconds in the TDB time system.

   <p> Since the relative rates at which time progresses,
   as measured in different time systems, can be a function
   of time, converting Durations from one time system to another
   requires specification of a start epoch. To avoid circular
   class dependencies, the epoch is specified as a double, not
   a Time.

   <p>Note that this method introduces a circular dependency
   between the classes {@link Time} and Duration; this is
   permitted by the Java language specification (third edition,
   section 7.3).
   */
   public abstract double getTDBSeconds( Time startTime )

      throws SpiceException;

   /**
   Every Duration subclass can return a duration measurement in
   its native time system.

   <p> The meaning of the returned value depends on the subclass
   to which the Duration instance belongs: for TDBDurations the
   returned value has units of TDB seconds, while for SCLKDurations
   the returned value is a tick count.
   */
   public abstract double getMeasure()

      throws SpiceException;


   /**
   Negate a Duration.
   */
   public abstract Duration negate();


   /**
   Scale a Duration by the scalar `s'.
   */
   public abstract Duration scale ( double s );
}
