
package spice.basic;

/**
Class AberrationCorrection represents selections of aberration
corrections that can be applied to state vectors.

<p> This class provides methods to enable an application to determine
attributes of a correction, for example whether stellar aberration
correction is included.

<p> Normally this class is used in association with constructors
for any of the classes
<ul>
<li> {@link spice.basic.StateRecord} </li>
<li> {@link spice.basic.StateVector} </li>
<li> {@link spice.basic.PositionRecord} </li>
<li> {@link spice.basic.PositionVector} </li>
<li> {@link spice.basic.VelocityVector} </li>
</ul>
<p> Aberration corrections are applied to states of a target
relative to an observer.  When light time corrections are used,
the orientation of a time-dependent reference frame also may depend
on the one-way light time between the observer and the ephemeris
object acting as the center of the reference frame.  Details
can be found in the documentation of methods that employ
aberration corrections.

<pre>

   About aberration corrections
   ============================

   In space science or engineering applications one frequently
   wishes to know where to point a remote sensing instrument, such
   as an optical camera or radio antenna, in order to observe or
   otherwise receive radiation from a target.  This pointing problem
   is complicated by the finite speed of light:  one needs to point
   to where the target appears to be as opposed to where it actually
   is at the epoch of observation.  We use the adjectives
   "geometric," "uncorrected," or "true" to refer to an actual
   position or state of a target at a specified epoch.  When a
   geometric position or state vector is modified to reflect how it
   appears to an observer, we describe that vector by any of the
   terms "apparent," "corrected," "aberration corrected," or "light
   time and stellar aberration corrected." JNISpice can correct for two
   phenomena affecting the apparent location of an object:  one-way
   light time (also called "planetary aberration") and stellar
   aberration.

   One-way light time
   ------------------

   Correcting for one-way light time is done by computing, given an
   observer and observation epoch, where a target was when the observed
   photons departed the target's location.  The vector from the
   observer to this computed target location is called a "light time
   corrected" vector.  The light time correction depends on the motion
   of the target relative to the solar system barycenter, but it is
   independent of the velocity of the observer relative to the solar
   system barycenter. Relativistic effects such as light bending and
   gravitational delay are not accounted for in the light time
   correction performed by this routine.

   Stellar aberration
   ------------------

   The velocity of the observer also affects the apparent location
   of a target:  photons arriving at the observer are subject to a
   "raindrop effect" whereby their velocity relative to the observer
   is, using a Newtonian approximation, the photons' velocity
   relative to the solar system barycenter minus the velocity of the
   observer relative to the solar system barycenter.  This effect is
   called "stellar aberration."  Stellar aberration is independent
   of the velocity of the target.  The stellar aberration formula
   used by this routine does not include (the much smaller)
   relativistic effects.

   Stellar aberration corrections are applied after light time
   corrections:  the light time corrected target position vector is
   used as an input to the stellar aberration correction.

   When light time and stellar aberration corrections are both
   applied to a geometric position vector, the resulting position
   vector indicates where the target "appears to be" from the
   observer's location.

   As opposed to computing the apparent position of a target, one
   may wish to compute the pointing direction required for
   transmission of photons to the target.  This also requires correction
   of the geometric target position for the effects of light time
   and stellar aberration, but in this case the corrections are
   computed for radiation traveling *from* the observer to the target.
   We will refer to this situation as the "transmission" case.

   The "transmission" light time correction yields the target's
   location as it will be when photons emitted from the observer's
   location at `et' arrive at the target.  The transmission stellar
   aberration correction is the inverse of the traditional stellar
   aberration correction:  it indicates the direction in which
   radiation should be emitted so that, using a Newtonian
   approximation, the sum of the velocity of the radiation relative
   to the observer and of the observer's velocity, relative to the
   solar system barycenter, yields a velocity vector that points in
   the direction of the light time corrected position of the target.

   One may object to using the term "observer" in the transmission
   case, in which radiation is emitted from the observer's location.
   The terminology was retained for consistency with earlier
   documentation.

   Below, we indicate the aberration corrections to use for some
   common applications:

      1) Find the apparent direction of a target.  This is
         the most common case for a remote-sensing observation.

            Use "LT+S":  apply both light time and stellar
            aberration corrections.

         Note that using light time corrections alone ("LT") is
         generally not a good way to obtain an approximation to an
         apparent target vector:  since light time and stellar
         aberration corrections often partially cancel each other,
         it may be more accurate to use no correction at all than to
         use light time alone.


      2) Find the corrected pointing direction to radiate a signal
         to a target.  This computation is often applicable for
         implementing communications sessions.

            Use "XLT+S":  apply both light time and stellar
            aberration corrections for transmission.


      3) Compute the apparent position of a target body relative
         to a star or other distant object.

            Use "LT" or "LT+S" as needed to match the correction
            applied to the position of the distant object.  For
            example, if a star position is obtained from a catalog,
            the position vector may not be corrected for stellar
            aberration.  In this case, to find the angular
            separation of the star and the limb of a planet, the
            vector from the observer to the planet should be
            corrected for light time but not stellar aberration.


      4) Obtain an uncorrected state vector derived directly from
         data in an SPK file.

            Use "NONE".


      5) Use a geometric state vector as a low-accuracy estimate
         of the apparent state for an application where execution
         speed is critical.

            Use "NONE".


      6) While this routine cannot perform the relativistic
         aberration corrections required to compute states
         with the highest possible accuracy, it can supply the
         geometric states required as inputs to these computations.

            Use "NONE", then apply relativistic aberration
            corrections (not available in the SPICE Toolkit).
</pre>

<h3> Version 2.0.0 02-DEC-2016 (NJB)</h3>

Added method {@link #isGeometric}.

<h3> Version 1.0.0 10-DEC-2009 (NJB)</h3>
*/
public class AberrationCorrection extends Object
{
   /*
   Static variables
   */
   static String[]   abcorrNames = { "NONE",
                                     "LT",
                                     "LT+S",
                                     "CN",
                                     "CN+S",
                                     "S",
                                     "XLT",
                                     "XLT+S",
                                     "XCN",
                                     "XCN+S",
                                     "XS"    };

   /*
   Instance variables
   */

   String                   name;
   boolean                  isReceptionType;
   boolean                  isConvergedNewtonian;
   boolean                  hasLightTime;
   boolean                  hasStellarAberration;


   /*
   Constructors
   */



   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public AberrationCorrection( AberrationCorrection abcorr )
   {
      name                 = new String ( abcorr.name );
      isReceptionType      = abcorr.isReceptionType;
      isConvergedNewtonian = abcorr.isConvergedNewtonian;
      hasLightTime         = abcorr.hasLightTime;
      hasStellarAberration = abcorr.hasStellarAberration;
   }



   /**
   Construct AberrationCorrection from a string.

   <pre>
   abcorr      indicates the aberration corrections to be applied
               to the state of a target body to account for one-way
               light time and stellar aberration.  See the discussion
               in the Particulars section for recommendations on
               how to choose aberration corrections.

               `abcorr' may be any of the following:

                  "NONE"     Apply no correction. Return the
                             geometric state of the target body
                             relative to the observer.

               The following values of `abcorr' apply to the
               "reception" case in which photons depart from the
               target's location at the light-time corrected epoch
               et-lt and *arrive* at the observer's location at
               `et':

                  "LT"       Correct for one-way light time (also
                             called "planetary aberration") using a
                             Newtonian formulation. This correction
                             yields the state of the target at the
                             moment it emitted photons arriving at
                             the observer at `et'.

                             The light time correction uses an
                             iterative solution of the light time
                             equation (see Particulars for details).
                             The solution invoked by the "LT" option
                             uses one iteration.

                  "LT+S"     Correct for one-way light time and
                             stellar aberration using a Newtonian
                             formulation. This option modifies the
                             state obtained with the "LT" option to
                             account for the observer's velocity
                             relative to the solar system
                             barycenter. The result is the apparent
                             state of the target---the position and
                             velocity of the target as seen by the
                             observer.

                  "CN"       Converged Newtonian light time
                             correction.  In solving the light time
                             equation, the "CN" correction iterates
                             until the solution converges (three
                             iterations on all supported platforms).

                             The "CN" correction typically does not
                             substantially improve accuracy because
                             the errors made by ignoring
                             relativistic effects may be larger than
                             the improvement afforded by obtaining
                             convergence of the light time solution.
                             The "CN" correction computation also
                             requires a significantly greater number
                             of CPU cycles than does the
                             one-iteration light time correction.

                  "CN+S"     Converged Newtonian light time
                             and stellar aberration corrections.


               The following values of `abcorr' apply to the
               "transmission" case in which photons *depart* from
               the observer's location at `et' and arrive at the
               target's location at the light-time corrected epoch
               et+lt:

                  "XLT"      "Transmission" case:  correct for
                             one-way light time using a Newtonian
                             formulation. This correction yields the
                             state of the target at the moment it
                             receives photons emitted from the
                             observer's location at `et'.

                  "XLT+S"    "Transmission" case:  correct for
                             one-way light time and stellar
                             aberration using a Newtonian
                             formulation  This option modifies the
                             state obtained with the "XLT" option to
                             account for the observer's velocity
                             relative to the solar system
                             barycenter. The position component of
                             the computed target state indicates the
                             direction that photons emitted from the
                             observer's location must be "aimed" to
                             hit the target.

                  "XCN"      "Transmission" case:  converged
                             Newtonian light time correction.

                  "XCN+S"    "Transmission" case:  converged
                             Newtonian light time and stellar
                             aberration corrections.


               Neither special nor general relativistic effects are
               accounted for in the aberration corrections applied
               by this routine.

               Case and blanks are not significant in the string
               `abcorr'.

   </pre>
   */
   public AberrationCorrection( String abcorr )

      throws SpiceErrorException
   {
      /*
      Make sure the string is equivalent to one of the recognized patterns.
      */
      name          = null;

      boolean found = false;
      int     i     = 0;

      while ( !found  && ( i < abcorrNames.length )  )
      {
         found = CSPICE.eqstr( abcorr, abcorrNames[i] );

         if ( found )
         {
            name = abcorrNames[i];
         }
         else
         {
            ++i ;
         }
      }

      if ( !found )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "AberrationCorrection",
            "SPICE(NOTSUPPORTED)",
            "Aberration correction string " +
            abcorr +
            " is not recognized."                  );

         throw( exc );
      }

      /*
      The case is "reception" if the string is not "NONE" and
      does *not* start with 'X'.
      */
      isReceptionType       =     ( !name.equals     ("NONE") )
                               && ( !name.startsWith ("X"   ) );

      /*
      Stellar aberration correction is requested if the substring
      "S" is present.
      */
      hasStellarAberration  =  name.indexOf ( "S" ) >  -1;

      /*
      The requested light time correction is converged
      Newtonian if the substring "CN" is present.
      */
      isConvergedNewtonian  =  name.indexOf ( "CN" ) >  -1;

      /*
      The requested correction has light time corrections if either
      of the substrings "LT" or "CN" are present.
      */
      hasLightTime  =  isConvergedNewtonian  ||  ( name.indexOf( "LT" ) >  -1 );


   }

   /*
   Instance Methods
   */

   /**
   Test two AberrationCorrections for equality.
   */
   public boolean equals ( Object obj )
   {
       if (  !( obj instanceof AberrationCorrection )  )
       {
          return false;
       }

       return (  ( (AberrationCorrection)obj ).name.equals (this.name)  );
   }


   /**
   Return hash code for an AberrationCorrection object.  This method
   is overridden to support the overridden equals( Object ) method.
   */
   public int hashCode()
   {
      /*
      The hashcode value is the hash code of the string representation
      of the aberration correction.
      */
      return (  name.hashCode() );
   }




   /**
   Return a string describing the aberration correction.
   */
   public String getName ()
   {
      return new String ( name );
   }


   /**
   Indicate whether the correction is geometric (equivalent to "NONE").
   */
   public boolean isGeometric()
   {
      return( !hasLightTime && !hasStellarAberration );
   }


   /**
   Indicate whether stellar aberration correction is included.
   */
   public boolean hasStellarAberration ()
   {
      return ( hasStellarAberration );
   }


   /**
   Indicate whether light time correction (converged Newtonian or
   not) is included.
   */
   public boolean hasLightTime ()
   {
      return ( hasLightTime );
   }


   /**
   Indicate whether the correction is converged Newtonian.
   */
   public boolean isConvergedNewtonian ()
   {
      return ( isConvergedNewtonian );
   }


   /**
   Indicate whether the correction type is "reception."
   */
   public boolean isReceptionType ()
   {
      return ( isReceptionType );
   }

   /**
   Return a string describing the aberration correction.  This
   method overrides Object's toString() method.
   */
   public String toString()
   {
      return ( this.getName() );
   }


}


