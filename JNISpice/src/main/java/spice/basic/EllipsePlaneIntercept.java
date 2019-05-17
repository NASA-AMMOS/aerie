
package spice.basic;

import spice.basic.CSPICE;

/**
Class EllipsePlaneIntercept represents the result of an
ellipse-plane intercept computation.

<p> Each EllipsePlaneIntercept instance consists of
<ul>
<li> An intersection count. </li>
<li> A Vector3 instance representing one point of intersection.
This instance is valid if and only if the intersection count is
at least 1 but is finite.  </li>
<li> A Vector3 instance representing a second point of intersection.
This instance is valid if and only if the intersection count is 2.  </li>

<p>Applications using this class should call the {@link #wasFound}
method before attempting to retrieve the points of intersection.


<h3> Version 1.0.1 16-DEC-2016 (NJB)</h3>

Corrected description in the class abstract.

<h3> Version 1.0.0 09-DEC-2009 (NJB)</h3>
*/
public class EllipsePlaneIntercept extends Object
{
   //
   // Public constants
   //
   public final static int       INFINITY         =   -1;

   //
   // Fields
   //
   private int               nxpts;
   private Vector3           xpt1     = null;
   private Vector3           xpt2     = null;



   //
   // Constructors
   //

   /**
   Construct an Ellipse-Plane intercept from an Ellipse and a Plane.
   */
   public EllipsePlaneIntercept ( Ellipse    ellipse,
                                  Plane      plane    )
      throws SpiceException

   {
      double[]       ellipseArray = ellipse.toArray();
      double[]       planeArray   = plane.toArray();
      double[]       xpt1Array    = new double[3];
      double[]       xpt2Array    = new double[3];
      int[]          nxptsArray   = new int[1];

      CSPICE.inelpl ( ellipseArray,
                      planeArray,
                      nxptsArray,
                      xpt1Array,
                      xpt2Array   );

      nxpts = nxptsArray[0];

      if (  ( nxpts == 1 ) || ( nxpts == 2 )  )
      {
         xpt1 = new Vector3( xpt1Array );
         xpt2 = new Vector3( xpt2Array );
      }
   }


   //
   // Methods
   //

   /**
   Fetch the intercept count.
   */
   public int getInterceptCount()
   {
      return nxpts;
   }

   /**
   Indicate that a finite intersection exists.
   */
   public boolean wasFound()
   {
      return (  ( nxpts == 1 ) || ( nxpts == 2 )  );
   }

   /**
   Fetch the intercepts. This method should be called only if
   the intercept count is non-zero and finite.
   */
   public Vector3[] getIntercepts()

      throws PointNotFoundException
   {
      if ( !wasFound() )
      {
         String msg;

         if ( nxpts == 0 )
         {
            msg = "Ellipse-plane intercept does not exist.";
         }
         else
         {
            msg = "Ellipse lies in plane.";
         }

         PointNotFoundException exc;

         exc = PointNotFoundException.create(

            "EllipsePlaneIntercept.getIntercepts",
            msg                                    );

         throw ( exc );
      }

      Vector3[] xptArray = new Vector3[2];

      xptArray[0]        = new Vector3( xpt1 );
      xptArray[1]        = new Vector3( xpt2 );

      return (  xptArray  );
   }
}

