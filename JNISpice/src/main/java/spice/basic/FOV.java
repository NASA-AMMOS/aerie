

package spice.basic;

import spice.basic.CSPICE;

/**
Class FOV represents instrument fields of view.


<h3>Code Examples</h3>

<p>
The numerical results shown for these examples may differ across
platforms. The results depend on the SPICE kernels used as
input, the compiler and supporting libraries, and the machine
specific arithmetic implementation.


<p> 1) The example program in this section loads the IK file
   'example.ti' with the following contents defining four FOVs of
   various shapes and sizes:

<pre>
   KPL/IK

   The keywords below define a circular, 10-degree wide FOV with
   the boresight along the +Z axis of the 'SC999_INST001' frame
   for an instrument with ID -999001 using the "angles"-class
   specification.

   \begindata
      INS-999001_FOV_CLASS_SPEC       = 'ANGLES'
      INS-999001_FOV_SHAPE            = 'CIRCLE'
      INS-999001_FOV_FRAME            = 'SC999_INST001'
      INS-999001_BORESIGHT            = ( 0.0, 0.0, 1.0 )
      INS-999001_FOV_REF_VECTOR       = ( 1.0, 0.0, 0.0 )
      INS-999001_FOV_REF_ANGLE        = ( 5.0 )
      INS-999001_FOV_ANGLE_UNITS      = ( 'DEGREES' )
   \begintext

   The keywords below define an elliptical FOV with 2- and
   4-degree angular extents in the XZ and XY planes and the
   boresight along the +X axis of the 'SC999_INST002' frame for
   an instrument with ID -999002 using the "corners"-class
   specification.

   \begindata
      INS-999002_FOV_SHAPE            = 'ELLIPSE'
      INS-999002_FOV_FRAME            = 'SC999_INST002'
      INS-999002_BORESIGHT            = ( 1.0, 0.0, 0.0 )
      INS-999002_FOV_BOUNDARY_CORNERS = ( 1.0, 0.0, 0.01745506,
                                          1.0, 0.03492077, 0.0 )
   \begintext

   The keywords below define a rectangular FOV with 1.2- and
   0.2-degree angular extents in the ZX and ZY planes and the
   boresight along the +Z axis of the 'SC999_INST003' frame for
   an instrument with ID -999003 using the "angles"-class
   specification.

   \begindata
      INS-999003_FOV_CLASS_SPEC       = 'ANGLES'
      INS-999003_FOV_SHAPE            = 'RECTANGLE'
      INS-999003_FOV_FRAME            = 'SC999_INST003'
      INS-999003_BORESIGHT            = ( 0.0, 0.0, 1.0 )
      INS-999003_FOV_REF_VECTOR       = ( 1.0, 0.0, 0.0 )
      INS-999003_FOV_REF_ANGLE        = ( 0.6 )
      INS-999003_FOV_CROSS_ANGLE      = ( 0.1 )
      INS-999003_FOV_ANGLE_UNITS      = ( 'DEGREES' )
   \begintext

   The keywords below define a triangular FOV with the boresight
   along the +Y axis of the 'SC999_INST004' frame for an
   instrument with ID -999004 using the "corners"-class
   specification.

   \begindata
      INS-999004_FOV_SHAPE            = 'POLYGON'
      INS-999004_FOV_FRAME            = 'SC999_INST004'
      INS-999004_BORESIGHT            = (  0.0,  1.0,  0.0 )
      INS-999004_FOV_BOUNDARY_CORNERS = (  0.0,  0.8,  0.5,
                                           0.4,  0.8, -0.2,
                                          -0.4,  0.8, -0.2 )
   \begintext
</pre>

<p> The program shown below loads the IK, fetches parameters for each
   of the four FOVs and prints these parameters to the screen.

<pre>
import spice.basic.*;

class FOVEx1
{
   //
   // Load the JNISpice shared object library
   // at initialization time.
   //
   static { System.loadLibrary( "JNISpice" ); }

   public static void main ( String[] args )
   {
      try
      {
         FOV                       fov;
         ReferenceFrame            frame;
         String                    shape;
         Vector3[]                 bounds;
         Vector3                   bsight;
         int[]                     insids =
                                   { -999001, -999002, -999003, -999004 };
         //
         // Load instrument kernel.
         //
         KernelDatabase.load ( "example.ti");

         System.out.format( "--------------------------------------%n" );

         for ( int i = 0;  i < insids.length;  i++ )
         {
            //
            // Create an Instrument instance from the ith ID code;
            // create a FOV instance from the Instrument.
            //
            fov = new FOV(  new Instrument( insids[i] )  );

            System.out.println( "Instrument ID: " + insids[i]               );
            System.out.println( "    FOV shape: " + fov.getShape()          );
            System.out.println( "    FOV frame: " + fov.getReferenceFrame() );
            System.out.println( "FOV boresight: " + fov.getBoresight()      );
            System.out.println( "  FOV corners: "                           );

            bounds = fov.getBoundary();

            for ( int j = 0;  j < bounds.length;  j++ )
            {
               System.out.println( "               " + bounds[j] );
            }

            System.out.format( "--------------------------------------%n" );
         }
      }
      catch ( SpiceException exc )
      {
         exc.printStackTrace();
      }
   }
}
</pre>

<p> When run on a PC/Linux/java 1.6.0_14/gcc platform,
  output from this program was (some of the lines below were wrapped
  to fit into the 80-character page width):

<pre>
   --------------------------------------
   Instrument ID: -999001
       FOV shape: CIRCLE
       FOV frame: SC999_INST001
   FOV boresight: (  0.0000000000000000e+00,   0.0000000000000000e+00,   1.000
0000000000000e+00)
     FOV corners:
                  (  8.7155742747658170e-02,   0.0000000000000000e+00,   9.961
9469809174550e-01)
   --------------------------------------
   Instrument ID: -999002
       FOV shape: ELLIPSE
       FOV frame: SC999_INST002
   FOV boresight: (  1.0000000000000000e+00,   0.0000000000000000e+00,   0.000
0000000000000e+00)
     FOV corners:
                  (  1.0000000000000000e+00,   0.0000000000000000e+00,   1.745
5060000000000e-02)
                  (  1.0000000000000000e+00,   3.4920770000000000e-02,   0.000
0000000000000e+00)
   --------------------------------------
   Instrument ID: -999003
       FOV shape: RECTANGLE
       FOV frame: SC999_INST003
   FOV boresight: (  0.0000000000000000e+00,   0.0000000000000000e+00,   1.000
0000000000000e+00)
     FOV corners:
                  (  1.0471768168559534e-02,   1.7452326687281040e-03,   9.999
4364652932120e-01)
                  ( -1.0471768168559534e-02,   1.7452326687281040e-03,   9.999
4364652932120e-01)
                  ( -1.0471768168559534e-02,  -1.7452326687281040e-03,   9.999
4364652932120e-01)
                  (  1.0471768168559534e-02,  -1.7452326687281040e-03,   9.999
4364652932120e-01)
   --------------------------------------
   Instrument ID: -999004
       FOV shape: POLYGON
       FOV frame: SC999_INST004
   FOV boresight: (  0.0000000000000000e+00,   1.0000000000000000e+00,   0.000
0000000000000e+00)
     FOV corners:
                  (  0.0000000000000000e+00,   8.0000000000000000e-01,   5.000
0000000000000e-01)
                  (  4.0000000000000000e-01,   8.0000000000000000e-01,  -2.000
0000000000000e-01)
                  ( -4.0000000000000000e-01,   8.0000000000000000e-01,  -2.000
0000000000000e-01)
   --------------------------------------
</pre>


<p> Version 1.0.0 15-DEC-2009 (NJB)
*/
public class FOV extends Object
{
   //
   // Public constants
   //

   //
   // FOV shapes
   //
   public final static String CIRCLE    =  "CIRCLE";
   public final static String ELLIPSE   =  "ELLIPSE";
   public final static String POLYGON   =  "POLYGON";
   public final static String RECTANGLE =  "RECTANGLE";


   //
   // Private constants
   //
   //
   // Maximum number of boundary vectors for a polygonal FOV.
   // This value must be kept in sync with SPICE_GF_MAXVRT.
   //
   public final static int    MAXVRT    = 100000;


   //
   // Fields
   //
   private Instrument          inst;
   private ReferenceFrame      FOVFrame;
   private String              FOVShape;
   private Vector3             boresight;
   private Vector3[]           FOVBounds;


   //
   // Constructors
   //
   public FOV ( Instrument    inst )

      throws SpiceException
   {
      //
      // Declare arrays used to capture JNI routine outputs.
      //
      String[]        shapeArray = new String[1];
      String[]        frameArray = new String[1];
      double[]        bsight     = new double[3];
      int[]           size       = new int   [1];
      double[]        bounds     = new double[MAXVRT*3];

      //
      // Call the JNI routine.
      //
      CSPICE.getfov ( inst.getIDCode(),
                      shapeArray,
                      frameArray,
                      bsight,
                      size,
                      bounds           );
      //
      // Transfer outputs to this object's fields.
      //

      FOVShape  = shapeArray[0];

      FOVFrame  = new ReferenceFrame( frameArray[0] );

      boresight = new Vector3( bsight );

      int n     = size[0];
      FOVBounds = new Vector3[ n ];


      int start = 0;

      for ( int i = 0;  i < n;  i++ )

      {
         FOVBounds[i] = new Vector3( bounds[start  ],
                                     bounds[start+1],
                                     bounds[start+2] );
         start += 3;
      }

      //
      // Store a deep copy of the instrument itself.
      //
      this.inst = new Instrument( inst );
   }



   //
   // Methods
   //

   /**
   Return the Instrument with which a FOV is associated.
   */
   public Instrument getInstrument()

      throws SpiceException
   {
      return ( new Instrument(inst) );
   }

   /**
   Return the boresight vector of the Instrument with which a
   FOV is associated.
   */
   public Vector3 getBoresight()
   {
      return ( new Vector3(boresight) );
   }


   /**
   Return the reference frame of a FOV.
   */
   public ReferenceFrame getReferenceFrame()
   {
      return (  new ReferenceFrame( FOVFrame )  );
   }


   /**
   Return the shape of a FOV.
   */
   public String getShape()
   {
      return (  new String( FOVShape )  );
   }

   /**
   Return the boundary vectors of a FOV.
   */
   public Vector3[] getBoundary()
   {

      Vector3[] outArray = new Vector3[ FOVBounds.length ];

      for ( int i = 0;  i < FOVBounds.length;  i++ )
      {
         outArray[i] = new Vector3( FOVBounds[i] );
      }

      return ( outArray );
   }


}

