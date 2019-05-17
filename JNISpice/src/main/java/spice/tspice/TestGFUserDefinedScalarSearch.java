
package spice.tspice;


import java.io.*;
import java.util.*;
import spice.basic.*;
import static spice.basic.GFConstraint.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestGFUserDefinedScalarSearch provides methods that implement
test families for the class TargetClass.

<p> Note that this class extends {@link spice.basic.GFScalarQuantity}
rather than {@link Object}. In addition to the normal test family
method, this class contains methods that override the abstract
methods of GFScalarQuantity.


<p>Version 1.0.0 31-DEC-2009 (NJB)
*/
public class TestGFUserDefinedScalarSearch extends GFScalarQuantity
{

   //
   // Class constants
   //
   private static final int             MAXNFUNC = 2;

   //
   // Class variables
   //
   private static int                   fIndex = -1;
   private static int                   nIter  = 0;

   //
   // Methods
   //

   /**
   No-argument constructor. This constructor is needed to create
   an object of type TestGFUserDefinedScalarSearch, which can be
   passed to the constructor of GFUserDefinedScalarSearch.
   */
   public TestGFUserDefinedScalarSearch()
   {
      //
      // No action is required.
      //
   }


   /**
   Override GFScalarQuantity.getQuantity. The actual function to be
   evaulated can be selected externally.
   */
   public double getQuantity( double et )

      throws SpiceException
   {
      //
      // Evaluate the local method `f' determined by `fIndex' at `et'.
      //
      return(  f( fIndex, et )  );
   }


   /**
   Override GFScalarQuantity.isQuantityDecreasing. The actual function to be
   evaulated can be selected externally.
   */
   public boolean isQuantityDecreasing( double et )

      throws SpiceException
   {
      //
      // Evaluate the local method `fdecr' determined by `fIndex' at `et'.
      //
      return(  fdecr( fIndex, et )  );
   }


   /**
   The scalar function f representing a user-defined quantity.
   */
   private double f ( int     idx,
                      double  et  )

      throws SpiceException
   {
      SpiceErrorException               exc;

      double                            x = 0;

      //
      // Count this iteration.
      //
      ++nIter;


      if ( idx == 0 )
      {
         //
         // Function 0 is about as simple as it gets.
         //

         x = Math.sin( et );

      }
      else if ( idx == 1 )
      {
         //
         // Function 1 throws an exception every time the
         // iteration count hits a multiple of 100.
         //

         if ( nIter % 100 == 0 )
         {
            exc = SpiceErrorException.create(

               "TestGFUserDefinedScalarSearch.f",

               "SPICE(ARTIFICIALERROR)",

               "Iteration index is " + idx + "." );

            throw( exc );

         }

         x = Math.sin( et );


      }
      else
      {
         exc = SpiceErrorException.create(

            "TestGFUserDefinedScalarSearch.f",

            "SPICE(INDEXOUTOFRANGE)",

            "function index is " + idx + " but should be " +
            "in range 0:" + (MAXNFUNC-1) + "."                    );

         throw( exc );
      }



      return( x );
   }


   /**
   The scalar function fdecr representing a user-defined "is quantity
   decreasing" function.
   */
   private boolean fdecr ( int     idx,
                           double  et     )

      throws SpiceException
   {
      SpiceErrorException               exc;

      boolean                           isdecr = false;

      //
      // Count this iteration.
      //
      ++nIter;


      if ( idx == 0 )
      {
         //
         // Function 0 is
         //
         //    sin( et )
         //
         // This function is decreasing when its derivative cos(et) is
         // negative.
         //

         isdecr = ( Math.cos(et)  <  0.0 );

      }
      else if ( idx == 1 )
      {
         //
         // Function 1 throws an exception every time the
         // iteration count hits a multiple of 100.
         //

         if ( nIter % 100 == 0 )
         {
            exc = SpiceErrorException.create(

               "TestGFUserDefinedScalarSearch.fdecr",

               "SPICE(ARTIFICIALERROR)",

               "Iteration index is " + nIter + "." );

            throw( exc );
         }

         isdecr = ( Math.cos(et)  <  0.0 );
      }
      else
      {
         exc = SpiceErrorException.create(

            "TestGFUserDefinedScalarSearch.f",

            "SPICE(INDEXOUTOFRANGE)",

            "function index is " + idx + " but should be " +
            "in range 0:" + (MAXNFUNC-1) + "."                    );

         throw( exc );
      }

      return( isdecr );
   }




   /**
   Test GFUserDefinedScalarSearch and associated classes.
   */
   public static boolean f_GFUserDefinedScalarSearch()

      throws SpiceException
   {
      //
      // Constants
      //
      final double                      TIGHT_TOL = 1.e-12;
      final double                      MED_TOL   = 1.e-6;

      final int                         MAXIVL    = 10000;

      //
      // Local variables
      //
      GFConstraint                      constraint;

      GFUserDefinedScalarSearch         search;

      SpiceWindow                       cnfine = null;
      SpiceWindow                       result = null;

      TestGFUserDefinedScalarSearch     quant;


      boolean                           ok;

      double[]                          interval;
      double                            finish;
      double                            refval;
      double                            start;
      double                            step;
      double                            xVal;

      int                               i;
      int                               xN;


      //
      //  We enclose all tests in a try/catch block in order to
      //  facilitate handling unexpected exceptions.  Unexpected
      //  exceptions are trapped by the catch block at the end of
      //  the routine; expected exceptions are handled locally by
      //  catch blocks associated with error handling test cases.
      //
      //  Therefore, JNISpice calls that are expected to succeed don't
      //  have any subsequent "chckxc" type calls following them, nor
      //  are they wrapped in in try/catch blocks.
      //
      //  Expected exceptions that are *not* thrown are tested
      //  via a call to {@link spice.testutils.Testutils#dogDidNotBark}.
      //

      try
      {

         JNITestutils.topen ( "f_GFUserDefinedScalarSearch" );




         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Error: find local maxima of sin(x); where " +
                              "subject function throws exception " +
                              "after 100 iterations."                );

         try
         {
            //
            // Set the function index and iteration count.
            //

            fIndex = 1;

            nIter  = 0;

            //
            // Create the constraint.
            //
            constraint = GFConstraint.createExtremumConstraint( LOCAL_MAXIMUM );

            //
            // Pick a confinement window. Note that our domain is just
            // the real line; we're not using time as the independent
            // variable.
            //

            cnfine = new SpiceWindow();

            cnfine = cnfine.insert( 0.0, 10*Math.PI );


            //
            // Create an instance of a subclass of GFScalarQuantity.
            //
            quant  = new TestGFUserDefinedScalarSearch();

            //
            // Create a search object.
            //
            search = new GFUserDefinedScalarSearch( quant );

            //
            // Pick a step size.
            //
            step   = Math.PI/4;

            //
            // Run the search.
            //
            result = search.run( cnfine, constraint, step, MAXIVL );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark (  "SPICE(ARTIFICIALERROR)" );

         }
         catch ( SpiceException ex )
         {
            // For debugging:
            //ex.printStackTrace();

            ok = JNITestutils.chckth ( true,   "SPICE(ARTIFICIALERROR)", ex );
         }




         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Find local maxima of sin(x)." );

         //
         // Set the function index.
         //
         fIndex = 0;

         //
         // Create the constraint.
         //
         constraint = GFConstraint.createExtremumConstraint( LOCAL_MAXIMUM );

         //
         // Pick a confinement window. Note that our domain is just
         // the real line; we're not using time as the independent
         // variable.
         //

         cnfine = new SpiceWindow();

         cnfine = cnfine.insert( 0.0, 10*Math.PI );


         //
         // Create an instance of a subclass of GFScalarQuantity.
         //
         quant  = new TestGFUserDefinedScalarSearch();

         //
         // Create a search object.
         //
         search = new GFUserDefinedScalarSearch( quant );

         //
         // Pick a step size.
         //
         step   = Math.PI/4;

         //
         // Run the search.
         //
         result = search.run( cnfine, constraint, step, MAXIVL );

         //
         // Check the number of local maxima.
         //
         xN = 5;

         ok = JNITestutils.chcksi ( "result card",
                                    result.card(),
                                    "=",
                                    xN,
                                    0             );

         //
         // Check the result window.
         //

         for( i = 0;  i < result.card();  i++ )
         {
            interval = result.getInterval(i);

            start    = interval[0];
            finish   = interval[1];

            //
            // `start' and `finish' should match exactly.
            //
            ok = JNITestutils.chcksd ( "start v finish",
                                       start,
                                       "=",
                                       finish,
                                       0.0             );


            //
            // In the ith interval, `start' should be pi/2 + 2*i*pi.
            //
            // Note that we're using default GF convergence tolerance,
            // so we're not going to get double precision results.
            //
            xVal = Math.PI * ( 0.5  +  (2.0 * i) );

            ok = JNITestutils.chcksd ( "start " + i,
                                       start,
                                       "~",
                                       xVal,
                                       MED_TOL       );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Find x such that sin(x) > sqrt(2)/2." );

         //
         // Set the function index.
         //
         fIndex = 0;

         //
         // We can use the search object we created in the previous
         // test case, but we need a new constraint.
         //
         refval     = Math.sqrt(2.0) / 2;

         constraint = GFConstraint.createReferenceConstraint( GREATER_THAN,
                                                                      refval );

         //
         // Run the search.
         //
         result = search.run( cnfine, constraint, step, MAXIVL );

         //
         // Check the number of local maxima.
         //
         xN = 5;

         ok = JNITestutils.chcksi ( "result card",
                                    result.card(),
                                    "=",
                                    xN,
                                    0             );

         //
         // Check the result window.
         //

         for( i = 0;  i < result.card();  i++ )
         {
            interval = result.getInterval(i);

            start    = interval[0];
            finish   = interval[1];


            //
            // In the ith interval, `start' should be pi/4 + 2*i*pi.
            // `finish' should be 3*pi/4 + 2*i*pi.
            //
            // Note that we're using default GF convergence tolerance,
            // so we're not going to get double precision results.
            //
            xVal = Math.PI * ( 0.25  +  (2.0 * i) );

            ok = JNITestutils.chcksd ( "start " + i,
                                       start,
                                       "~",
                                       xVal,
                                       MED_TOL       );

            xVal = Math.PI * ( 0.75  +  (2.0 * i) );

            ok = JNITestutils.chcksd ( "finish " + i,
                                       finish,
                                       "~",
                                       xVal,
                                       MED_TOL       );
         }


      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         ex.printStackTrace();

         ok = JNITestutils.chckth ( false, "", ex );
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

