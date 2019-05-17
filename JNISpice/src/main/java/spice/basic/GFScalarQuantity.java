package spice.basic;

/**
Abstract class GFScalarQuantity provides a template for user-defined
classes representing scalar functions of time to be used
in GF searches.

<h2>Code Examples</h2>


<p> See the examples in
{@link spice.basic.GFUserDefinedScalarSearch}.

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.



<p> Version 1.0.0 30-DEC-2009 (NJB)
*/
public abstract class GFScalarQuantity extends Object
{

   /**
   Return the value of a user-defined
   scalar quantity at a specified time. The time is
   expressed as seconds past J2000 TDB.
   */
   public abstract double getQuantity( double  et )

      throws SpiceException;

   /**
   Return a boolean value indicating whether
   the scalar function represented by getQuantity is decreasing
   at a specified time. The time is expressed as seconds past J2000 TDB.
   */
   public abstract boolean isQuantityDecreasing( double  et )

      throws SpiceException;
}
