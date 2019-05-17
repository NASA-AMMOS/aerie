

package spice.basic;

/**
Class GFUserDefinedScalarSearch supports searches for
events involving user-defined scalar functions of time.

<p>The JNISpice abstract class GFScalarQuantity defines the
interfaces of the user-defined methods called by this class'
search algorithm. These methods are
<pre>
   double   getQuantity         ( double et )
   boolean  isQuantityDecreasing( double et )
</pre>
<p>Note that these methods have different prototypes than
those of their CSPICE counterparts.

<p>Users subclass GFScalarQuantity to create their own
scalar quantities to be used in GF searches.

<h2>Code Examples</h2>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.

<p> 1) Run searches to find times when two different constraints on
       a user-defined scalar function are met.

<p>Example code begins here.

<pre>
   //
   // Run searches to find times when two different constraints on
   // a user-defined scalar function are met.
   //
   // In this example, we use the test program class itself to
   // implement a subclass of GFScalarQuantity.
   //
   // The "run" method for a user-defined scalar search is
   // provided by class GFUserDefinedScalarSearch. An instance
   // of this class is created by calling a constructor that
   // takes as its only input an instance of a subclass of
   // GFScalarQuantity.
   //

   import spice.basic.*;

   class GFUserDefinedScalarSearchEx1 extends GFScalarQuantity
   {
      //
      // Load the JNISpice shared library.
      //
      static { System.loadLibrary( "JNISpice" ); }

      //
      // A no-argument constructor for this class.
      //
      public GFUserDefinedScalarSearchEx1 ()
      {
      }

      //
      // Implement the abstract methods of GFScalarQuantity.
      //
      // In a more complex program, this implementation would
      // best be provided by a separate class, not by the
      // test driver itself, as is the case here.
      //

      //
      // Return the quantity
      //
      //    f(et) = sin( pi*et )
      //
      // evaluated at the input time.
      //
      public double getQuantity( double et )

         throws SpiceException
      {
         return(   Math.sin( Math.PI * et )  );
      }


      //
      // Indicate whether the quantity f(et) is decreasing
      // at the input time.
      //
      public boolean isQuantityDecreasing( double et )

         throws SpiceException
      {
         return(  Math.cos(Math.PI * et) < 0  );
      }


      //
      // Run a GF user-defined scalar quantity search.
      //
      public static void main ( String[] args )
      {
         try
         {
            //
            // Maximum number of workspace intervals.
            //
            final int MAXIVL = 100;

            //
            // The result window.
            //
            SpiceWindow result;

            //
            // Create a user-defined scalar quantity.
            //
            GFUserDefinedScalarSearchEx1 q = new GFUserDefinedScalarSearchEx1();

            //
            // Create a user defined scalar search instance.
            // The input argument is an instance of this test class,
            // GFUserDefinedScalarSearchEx1, which we have made
            // a subclass of GFScalarQuantity.
            //
            GFUserDefinedScalarSearch search =

               new GFUserDefinedScalarSearch( q );

            //
            // Create a confinement window representing the interval
            // [0.0, 10.0].
            //
            SpiceWindow confine = new SpiceWindow();

            confine.insert( 0.0, 10.0 );

            //
            // First search: find times when the function attains a local
            // maximum.
            //
            // Define the search constraint.
            //
            GFConstraint cons =

               GFConstraint.createExtremumConstraint(
                                                  GFConstraint.LOCAL_MAXIMUM );

            //
            // Set the step size.
            //
            double step = 0.25;

            //
            // Run the search on the confinement window.
            //
            result = search.run( confine, cons, step, MAXIVL );

            //
            // Display the result window.
            //
            System.out.format ( "%n"                                    +
                                "Local maxima of f(et) = sin( pi*et ) " +
                                "were found at:%n"                      +
                                result                                    );


            //
            // Second search: find roots of the function.
            //
            //
            // We can re-use the GFUserDefinedScalarSearch instance we
            // created for the first search.
            //
            // Define the search constraint.
            //
            cons = GFConstraint.createReferenceConstraint( "=", 0.0 );

            //
            // Set the step size.
            //
            step = 0.25;

            //
            // Run the search on the confinement window.
            //
            result = search.run( confine, cons, step, MAXIVL );

            //
            // Display the result window.
            //
            System.out.format ( "%n"                                  +
                                "Roots of  f(et) = sin( pi*et ) "     +
                                "were found at:%n"                    +
                                result                                  );
         }
         catch ( SpiceException exc ) {
            exc.printStackTrace();
         }
      }
   }
</pre>

<p>When run on a PC/Linux/java 1.6.0_14/gcc platform, the output from
this program was:


<pre>

   Local maxima of f(et) = sin( pi*et ) were found at:

   [  5.0000047683715820e-01,   5.0000047683715820e-01]
   [  2.5000004768371580e+00,   2.5000004768371580e+00]
   [  4.5000004768371580e+00,   4.5000004768371580e+00]
   [  6.4999995231628420e+00,   6.4999995231628420e+00]
   [  8.4999995231628420e+00,   8.4999995231628420e+00]


   Roots of  f(et) = sin( pi*et ) were found at:

   [  1.0000000000000000e+00,   1.0000000000000000e+00]
   [  2.0000000000000000e+00,   2.0000000000000000e+00]
   [  3.0000000000000000e+00,   3.0000000000000000e+00]
   [  4.0000000000000000e+00,   4.0000000000000000e+00]
   [  5.0000000000000000e+00,   5.0000000000000000e+00]
   [  6.0000004768367035e+00,   6.0000004768367035e+00]
   [  7.0000004768376130e+00,   7.0000004768376130e+00]
   [  8.0000004768367030e+00,   8.0000004768367030e+00]
   [  9.0000004768376130e+00,   9.0000004768376130e+00]

</pre>

<p>Note that the default convergence tolerance yields single precision roots.




<p> Version 1.0.0 02-JAN-2010 (NJB)
*/
public class GFUserDefinedScalarSearch extends GFNumericSearch
{
   //
   // Public constants
   //


   //
   // Fields
   //
   private GFScalarQuantity             quantity;

   //
   // Constructors
   //

   public GFUserDefinedScalarSearch ( GFScalarQuantity quantity )
   {
      //
      // Just save the input.
      //
      this.quantity          = quantity;
   }


   /**
   Run a user-defined scalar quantity
   search over a specified confinement window, using
   a specified constraint and step size (units are TDB seconds).
   */
   public SpiceWindow run ( SpiceWindow   confinementWindow,
                            GFConstraint  constraint,
                            double        step,
                            int           maxWorkspaceIntervals )

      throws SpiceException
   {

      double[] resultArray = CSPICE.gfuds ( quantity,
                                            constraint.getCSPICERelation(),
                                            constraint.getReferenceValue(),
                                            constraint.getAdjustmentValue(),
                                            step,
                                            maxWorkspaceIntervals,
                                            confinementWindow.toArray()    );

      return (  new SpiceWindow( resultArray )  );
   }


}
