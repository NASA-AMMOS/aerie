

package spice.basic;

import spice.basic.CSPICE;

/**
Class ReferenceFrame represents the identities of reference
frames and supports transformations between them.

<p>Position and state vector transformation matrices are
created by methods of this class. See {@link 
#getPositionTransformation( ReferenceFrame, Time )}, 
{@link #getPositionTransformation(
ReferenceFrame, Time, Time )} and {@link #getStateTransformation( 
ReferenceFrame, Time )}.

<h3>Code Examples</h3>
<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.


<pre>
Example 1:

   //
   // Obtain the J2000 to IAU_SATURN position transformation matrix for
   // the epoch 2009 June 25 00:00:00 UTC.
   //
   // Import the SPICE API declarations.
   //
   // The static import of symbols from spice.basic.ReferenceFrame
   // enables use to use constants from that class without prefixing
   // them with their class name.
   //
   import spice.basic.*;
   import static spice.basic.ReferenceFrame.*;

   public class ReferenceFrame_ex2 {

      //
      // Load the JNISpice shared object library or DLL.
      //
      static {   System.loadLibrary ( "JNISpice" );  }

      public static void main ( String[] args ) {

         try
         {
            //
            // Load SPICE kernels via the standard meta-kernel. The
            // static `load' method of class KernelDatabase is equivalent
            // to the "FURNSH" call of other SPICE Toolkits.
            //
            KernelDatabase.load ( "standard.tm" );

            //
            // Create a TDB time representing the UTC time of interest.
            //
            TDBTime et = new TDBTime( "2009 June 25 00:00:00 UTC" );

            ReferenceFrame J2000      = new ReferenceFrame ( "J2000"      );
            ReferenceFrame IAU_SATURN = new ReferenceFrame ( "IAU_SATURN" );

            //
            // Compute the transformation at the time of interest. Note that
            // an instance of any subclass of spice.basic.Time may be used as
            // the input time argument.
            //
            Matrix33 m = J2000.getPositionTransformation( IAU_SATURN, et );

            //
            // Display the transformation matrix. We can print the
            // referenceFrame, TDBTime, and Matrix33 instances
            // since their classes override java.lang.object.toString().
            // Note that `et' will be displayed as a TDB calendar string.
            //
            String endl = System.getProperty( "line.separator" );

            System.out.println ( endl + "Transformation from frame " + J2000 +
                                 " to frame " + IAU_SATURN      + endl +
                                 "at " + et + ":" + endl + endl + m );
         }
         catch ( SpiceException exc )  {
            //
            // If JNISpice threw an exception, display a Java stack
            // trace and any SPICE diagnostic message. Note that all
            // exceptions thrown by JNISpice classes are derived
            // from SpiceException.
            //
            exc.printStackTrace();
         }
      }
   }
</pre>

When this program was executed on a PC/Linux/java 1.6.0_14/gcc platform, the
output was:

<pre>
Transformation from frame J2000 to frame IAU_SATURN
at 2009 JUN 25 00:01:06.184 (TDB):

 -1.0113056497298978e-01,  -9.9150639953328200e-01,   8.1778166479761040e-02,
  9.9119341126813720e-01,  -1.0747884707743233e-01,  -7.7355794156219220e-02,
  8.5488187996221650e-02,   7.3234944633375220e-02,   9.9364400697516870e-01
</pre>


<h3> Version 2.0.0 28-DEC-2016 (NJB)</h3>


<p>Added methods {@link #equals} and {@link #hashCode}.

<p>Added method 
{@link #getPositionTransformation(spice.basic.ReferenceFrame toFrame,
spice.basic.Time etFrom, spice.basic.Time etTo )}.

<p> Bug fix: in the ReferenceFrame(int) constructor, changed code
to test for either empty or blank string returned from CSPICE.frmnam.




<h3> Version 1.0.0 09-DEC-2009 (NJB)</h3>

*/

public class ReferenceFrame extends Object
{

   /*
   Instance variables
   */
   private             String frameName;


   /*
   Public constants
   */

   //
   // Consider adding these:
   //
   /*
   public final static ReferenceFrame B1950         = new ReferenceFrame(
                                                               "B1950" );
   public final static ReferenceFrame ECLIPJ2000    = new ReferenceFrame(
                                                               "ECLIPJ2000" );
   public final static ReferenceFrame J2000         = new ReferenceFrame(
                                                               "J2000" );
   public final static ReferenceFrame IAU_EARTH     = new ReferenceFrame(
                                                               "IAU_EARTH" );
   public final static ReferenceFrame IAU_JUPITER   = new ReferenceFrame(
                                                               "IAU_JUPITER" );
   public final static ReferenceFrame IAU_MARS      = new ReferenceFrame(
                                                               "IAU_MARS" );
   public final static ReferenceFrame IAU_MERCURY   = new ReferenceFrame(
                                                               "IAU_MERCURY" );
   public final static ReferenceFrame IAU_MOON      = new ReferenceFrame(
                                                               "IAU_MOON" );
   public final static ReferenceFrame IAU_NEPTUNE   = new ReferenceFrame(
                                                               "IAU_NEPTUNE" );
   public final static ReferenceFrame IAU_PLUTO     = new ReferenceFrame(
                                                               "IAU_PLUTO" );
   public final static ReferenceFrame IAU_SATURN    = new ReferenceFrame(
                                                               "IAU_SATURN" );
   public final static ReferenceFrame IAU_SUN       = new ReferenceFrame(
                                                               "IAU_SUN" );
   public final static ReferenceFrame IAU_URANUS    = new ReferenceFrame(
                                                               "IAU_URANUS" );
   public final static ReferenceFrame IAU_VENUS     = new ReferenceFrame(
                                                               "IAU_VENUS" );
   public final static ReferenceFrame MARSIAU       = new ReferenceFrame(
                                                               "MARSIAU" );
   */


   /*
   Constructors
   */


   /**
   Construct a ReferenceFrame from a string.
   */
   public ReferenceFrame ( String name )

      throws SpiceException
   {
      //
      // Reject blank or empty frame names.
      //

      if ( name.equals( "" ) )
      {
         SpiceException exc = SpiceErrorException.create(

            "ReferenceFrame",
            "SPICE(EMPTYSTRING)",
            "Input reference frame name string was empty." );

         throw ( exc );
      }
      else if ( name.trim().equals( "" ) )
      {
         SpiceException exc = SpiceErrorException.create(

            "ReferenceFrame",
            "SPICE(BLANKSTRING)",
            "Input  reference frame name string was blank." );

         throw ( exc );
      }

      frameName = name;
   }


   /**
   Construct a ReferenceFrame from an integer code.
   */
   public ReferenceFrame ( int code )

      throws FrameNotFoundException, SpiceException
   {
      frameName = CSPICE.frmnam ( code );

      if (  frameName.trim().equals( "" )  )
      {
         FrameNotFoundException exc = FrameNotFoundException.create(

            "ReferenceFrame",
            "No frame name was found for reference frame " + code );

         throw ( exc );
      }
   }


   /**
   Construct a ReferenceFramefrom another ReferenceFrame.
   This constructor creates a deep copy.
   */
   public ReferenceFrame ( ReferenceFrame f )
   {
      this.frameName = new String ( f.frameName );
   }


   /*
   Instance methods
   */


   /**
   Return a hash code for this instance.

   This method does not throw an exception.
   */
   public int hashCode()
   {
      try
      {
         return ( this.getIDCode() ); 
      }
      catch( Throwable th )
      {
         return 0;
      }
   }


   /**
   Test two ReferenceFrame instances for equality.

   This method does not throw an exception.
   */
   public boolean equals( Object obj )
   {
      try
      {

         if ( obj == null )
         {
            return false;
         }
         else if ( !(obj instanceof ReferenceFrame) )
         {
            return false;
         }
         else
         {
            int thisID = this.getIDCode();
            int objID  = ( (ReferenceFrame)obj ).getIDCode();
            
            return(  thisID == objID );
         }
          
      }
      catch ( Throwable th )
      {
         return( false );
      }
   }


   /**
   Return the SPICE frame ID of a reference frame.
   */
   public int getIDCode()

      throws SpiceException
   {
      return (  CSPICE.namfrm( frameName )  );
   }


   /**
   Return the name of a reference frame as a String.
   This method normalizes the name if the name is known to
   the SPICE system.
   */
   public String getName()

      throws SpiceException
   {
      String name = new String( frameName );

      int code = this.getIDCode();

      if ( code != 0 )
      {
         //
         // This is the normal case: the frame is known to
         // SPICE.
         //
         name = ( new ReferenceFrame(code) ).frameName;
      }
      else
      {
         //
         // The frame is unknown.
         //
         name = new String( frameName );
      }

      return ( name );
   }



   /**
   Return frame specification parameters for this instance.
   */
   public FrameInfo getFrameInfo()

      throws FrameNotFoundException, SpiceException
   {
      FrameInfo info = new FrameInfo( this.getIDCode() );

      return( info );
   }


   /**
   Override toString(). This method normalizes the name if
   the name is known to the SPICE system.
   */
   public String toString()
   {
      String name = null;

      try
      {
         name = this.getName();
      }
      catch( SpiceException exc )
      {
         name = exc.getMessage();
      }

      return ( name );
   }


   /**
   Return a 3x3 matrix that transforms 3-vectors from one
   reference frame to another.
   */
   public Matrix33
   getPositionTransformation ( ReferenceFrame toFrame, Time t )

      throws SpiceException
   {
      String           toName = toFrame.getName();
      double           et     = t.getTDBSeconds();
      double[][]       m      = CSPICE.pxform( frameName, toName, et );

      return (  new Matrix33( m )  );
   }


   /**
   Return a 3x3 matrix that transforms 3-vectors from one
   reference frame, evaluated at a specified time, to another,
   specified at a second time.
   */
   public Matrix33
   getPositionTransformation ( ReferenceFrame toFrame, 
                               Time           fromTime,
                               Time           toTime   )

      throws SpiceException
   {
      String           toName = toFrame.getName();
      double           etfrom = fromTime.getTDBSeconds();
      double           etto   = toTime.getTDBSeconds();
      double[][]       m      = CSPICE.pxfrm2( frameName, toName, 
                                               etfrom,    etto   );

      return (  new Matrix33( m )  );
   }





   /**
   Return a 6x6 matrix that transforms state vectors from one
   reference frame to another.
   */
   public Matrix66
   getStateTransformation ( ReferenceFrame toFrame, Time t )

      throws SpiceException
   {
      double[][]       m         = new double[6][6];
      int              start;

      String           toName    = toFrame.getName();
      double           et        = t.getTDBSeconds();
      double[]         retArray  = CSPICE.sxform( frameName, toName, et );

      for ( int row = 0;  row < 6;  row++ )
      {
         start = 6*row;

         System.arraycopy ( retArray, start, m[row], 0, 6 );
      }

      return (  new Matrix66( m )  );
   }

}
