
package spice.basic;

import spice.basic.CSPICE;

/**
Class EllipsoidPlaneIntercept represents the result of a
ellipsoid-plane intercept computation. 


<p> Each EllipsoidPlaneIntercept instance consists of
<ul>
<li> A "was found" boolean flag indicating whether an 
intersection exists.</li>
<li> An Ellipse instance representing the intersection of the
ellipsoid and plane.
This instance is valid if and only if "was found" flag is true.
</li>
</ul>

<p>Applications using this class should call the {@link #wasFound}
method before attempting to retrieve the intersection Ellipse.
The {@link #getIntercept} method will throw a 
{@link PointNotFoundException}
if it is called on an EllipsoidPlaneIntercept instance
for which the intercept does not exist.

<p>To find the ellipsoid-plane intersection in the special case
where the plane is the limb plane for a specified view point,
use the method {@link Ellipsoid#getLimb(Vector3)}.

<p>For high-level limb computations for which the observer
and target are specified as {@link Body} instances, use the
method {@link LimbPoint#create}. 


<h3>Code Examples</h3>

The numerical results shown for these examples may differ 
across platforms. The results depend on the SPICE kernels used as 
input, the compiler and supporting libraries, and the machine 
specific arithmetic implementation. 

<ol>
<li>Compute an Ellipsoid-Plane intercept using a known
limb plane for a given view point. Compare the resulting 
ellipse to the limb.


<p>Example code starts here.
<pre>

//
// Program EllipsoidPlaneInterceptEx1
//

import spice.basic.*;

//
// Compute an Ellipsoid-Plane intercept using a known
// limb plane for a given view point. Compare the resulting 
// ellipse to the limb.
//
public class EllipsoidPlaneInterceptEx1
{
   //
   // Load SPICE shared library.
   //
   static{ System.loadLibrary( "JNISpice" ); }


   public static void main( String[] args )

      throws SpiceException
   {
 
      //
      // Local variables
      //
      Ellipse                           limb;
      Ellipsoid                         ellipsoid; 
      EllipsoidPlaneIntercept           intercept;

      Plane                             limbPlane;

      Vector3                           centerLimb;
      Vector3                           centerIntercept;
      Vector3                           smajorLimb;
      Vector3                           smajorIntercept;
      Vector3                           sminorLimb;
      Vector3                           sminorIntercept;
      Vector3                           viewpt;
  
      double                            a;
      double                            b;
      double                            c;

      try
      {
         //
         // Choose semi-axis lengths of a triaxial ellipsoid.
         // 
         a = 5000.0;
         b = 3000.0;
         c = 1000.0;

         ellipsoid = new Ellipsoid( a, b, c );

         viewpt    = new Vector3( -7000.0, 6000.0, 2000.0 );

         //
         // Find the limb of the ellipsoid as seen from `viewpt'.
         // Extract the center and semi-axes of the limb.
         //          
         limb       = ellipsoid.getLimb( viewpt );

         centerLimb = limb.getCenter();
         smajorLimb = limb.getSemiMajorAxis();
         sminorLimb = limb.getSemiMinorAxis();

         //
         // Construct the limb plane from the center and the semi-axes,
         // which serve as spanning vectors.
         //
         limbPlane = new Plane( centerLimb, smajorLimb, sminorLimb );

         //
         // Compute the ellipsoid-plane intercept using the 
         // limb plane and the ellipsoid.
         // 
         intercept = new EllipsoidPlaneIntercept( ellipsoid, limbPlane );

         //
         // Compare the limb and the intercept we just computed.
         //
         centerLimb      = limb.getCenter();
         smajorLimb      = limb.getSemiMajorAxis();
         sminorLimb      = limb.getSemiMinorAxis();
 
         centerIntercept = intercept.getIntercept().getCenter();
         smajorIntercept = intercept.getIntercept().getSemiMajorAxis();
         sminorIntercept = intercept.getIntercept().getSemiMinorAxis();

         //
         // Adjust intercept semi-axes if they're not aligned with
         // those of the limb. The semi-axes are determined only up
         // to sign.
         //
         if ( smajorLimb.dot( smajorIntercept ) < 0.0 )
         {
            smajorIntercept = smajorIntercept.negate();
         }

         if ( sminorLimb.dot( sminorIntercept ) < 0.0 )
         {
            sminorIntercept = sminorIntercept.negate();
         }

         System.out.format ( "%n" );

         System.out.println( " Difference of ellipse centers         = \n  " + 
                              centerLimb.sub( centerIntercept )               );

         System.out.println( " Difference of ellipse semi-major axes = \n  " + 
                              smajorLimb.sub( smajorIntercept )               );

         System.out.println( " Difference of ellipse semi-minor axes = \n  " + 
                              sminorLimb.sub( sminorIntercept )               );

         System.out.format ( "%n" );

      } // End of try block

      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }

   } // End of main method 
   
}

</pre>

<p>When this program was executed on a PC/Linux/gcc/64-bit/java 1.5 platform, 
the output was: 

<pre>

 Difference of ellipse centers         =
  (  2.2737367544323206e-13,  -2.2737367544323206e-13,   2.8421709430404007e-14)
 Difference of ellipse semi-major axes =
  (  0.0000000000000000e+00,  -4.5474735088646410e-13,   2.8421709430404007e-14)
 Difference of ellipse semi-minor axes =
  (  5.6843418860808015e-14,  -4.5474735088646410e-13,   0.0000000000000000e+00)

</pre>


</li>
</ol>


<h3> Version 1.0.0 17-DEC-2016 (NJB)</h3>
*/
public class EllipsoidPlaneIntercept extends Object
{

   //
   // Fields
   //
   private boolean           wasFound;
   private Ellipse           interceptEllipse;

   //
   // Constructors
   //

   /**
   Construct an Ellipsoid-Plane intercept from an Ellipsoid and a Plane.
   */
   public EllipsoidPlaneIntercept ( Ellipsoid  ellipsoid,
                                    Plane      plane     )
      throws SpiceException

   {
      final int      ELLSIZ       = 9;

      boolean[]      foundArray   = new boolean[1];

      double[]       ellipseArray = new double[ ELLSIZ ];
      double[]       planeArray   = plane.toArray();
      double[]       radii        = ellipsoid.getRadii();
 

      CSPICE.inedpl ( radii[0],   radii[1],     radii[2], 
                      planeArray, ellipseArray, foundArray );

      wasFound = foundArray[0];

      if ( wasFound )
      {
         interceptEllipse = new Ellipse( ellipseArray );
      }
   }

   /**
   No-arguments constructor.
   */
   public EllipsoidPlaneIntercept()
   {
   }

   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public EllipsoidPlaneIntercept( EllipsoidPlaneIntercept epx )

      throws PointNotFoundException
   {
      this.wasFound  = epx.wasFound;

      if ( this.wasFound );
      {
         this.interceptEllipse = epx.getIntercept();
      }
   }
    
  

   //
   // Methods
   //

    
   /**
   Indicate whether an intersection exists.
   */
   public boolean wasFound()
   {
      return ( wasFound );
   }

   /**
   Fetch the intercept. This method should be called only if
   the intercept was found.

   <p>This method returns a deep copy.
   */
   public Ellipse getIntercept()

      throws PointNotFoundException
   {
      if ( !wasFound() )
      {
         String msg = "Ellipse-plane intercept does not exist.";
          
         PointNotFoundException exc

            = PointNotFoundException.create(

            "EllipsoidPlaneIntercept.getIntercepts",
            msg                                    );

         throw ( exc );
      }

      
      return ( new Ellipse(interceptEllipse)  );
   }
}

