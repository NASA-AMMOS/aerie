
package spice.basic;


/**
Class DSKDescriptor represents DSK segment descriptors
and declares DSK constants.

<p> Version 1.0.0 15-NOV-2016 (NJB)
<pre>
Based on the DSK class of the Alpha DSK JNISpice Toolkit.

Index parameters declared in the Alpha DSK Toolkit
DSK were changed from Fortran style (1-based)
to Java style (0-based).
</pre>
*/
public class DSKDescriptor
{

   //
   // Public fields
   //


   //    Segment descriptor parameters
   //
   //    Each segment descriptor occupies a contiguous
   //    range of DAS d.p. addresses.
   //
   //       The DSK segment descriptor layout is:
   //
   //          +---------------------+
   //          | Surface ID code     |      
   //          +---------------------+
   //          | Center ID code      |      
   //          +---------------------+
   //          | Data class code     |  
   //          +---------------------+
   //          | Data type           |  
   //          +---------------------+
   //          | Ref frame code      |  
   //          +---------------------+
   //          | Coord sys code      |  
   //          +---------------------+
   //          | Coord sys parameters|  {10 elements}
   //          +---------------------+
   //          | Min coord 1         |  
   //          +---------------------+
   //          | Max coord 1         |  
   //          +---------------------+
   //          | Min coord 2         |  
   //          +---------------------+
   //          | Max coord 2         |  
   //          +---------------------+
   //          | Min coord 3         |  
   //          +---------------------+
   //          | Max coord 3         |  
   //          +---------------------+
   //          | Start time          |  
   //          +---------------------+
   //          | Stop time           |  
   //          +---------------------+
   //
   //    Parameters defining offsets for segment descriptor elements
   //    follow.
   //
   //
   //    Surface ID code:
   //
   public static final int                  SRFIDX = 0;

   //
   //    Central ephemeris object NAIF ID:
   //
   public static final int                  CTRIDX = SRFIDX + 1;

   //
   //    Data class:
   //
   //    The "data class" is a code indicating the category of
   //    data contained in the segment.
   //    
   public static final int                  CLSIDX = CTRIDX + 1;


   //
   //    Data type:
   //
   public static final int                  TYPIDX = CLSIDX + 1;

   //
   //    Frame ID:
   //
   public static final int                  FRMIDX = TYPIDX + 1;

   //
   //    Coordinate system code:
   //
   public static final int                  SYSIDX = FRMIDX + 1;

   //
   //    Coordinate system parameter start index:
   //
   public static final int                  PARIDX = SYSIDX + 1;

   //
   //    Number of coordinate system parameters:
   //
   public static final int                  NSYPAR = 10;

   //
   //    Ranges for coordinate bounds:
   //
   public static final int                  MN1IDX = PARIDX + NSYPAR;
   public static final int                  MX1IDX = MN1IDX + 1;
   public static final int                  MN2IDX = MX1IDX + 1;
   public static final int                  MX2IDX = MN2IDX + 1;
   public static final int                  MN3IDX = MX2IDX + 1;
   public static final int                  MX3IDX = MN3IDX + 1;

   //
   //    Coverage time bounds:
   //
   public static final int                  BTMIDX = MX3IDX + 1;
   public static final int                  ETMIDX = BTMIDX + 1;

   //
   //    Descriptor size (24):
   //
   public static final int                  DSKDSZ = ETMIDX + 1;


   //
   //    Data class values:
   //
   //       Class 1 indicates a surface that can be represented
   //       as single-valued function of its domain coordinates.
   //       
   //       An example is a surface defined by a function that
   //       maps each planetodetic longitude and latitude pair to
   //       a unique altitude.
   //
   public static final int                  SVFCLS = 1;

   //
   //       Class 2 indicates a general surface. Surfaces that
   //       have multiple points for a given pair of domain
   //       coordinates---for example, multiple radii for a given
   //       latitude and longitude---belong to class 2.
   //       
   public static final int                  GENCLS = 2;

   //
   //    Coordinate system values:
   //
   //       The coordinate system code indicates the system to which the
   //       tangential coordinate bounds belong. 
   //
   //       Code 1 refers to the planetocentric latitudinal system. 
   //
   //       In this system, the first tangential coordinate is longitude
   //       and the second tangential coordinate is latitude. The third
   //       coordinate is radius.
   //        
   //
   public static final int                  LATSYS = 1;

   //
   //       Code 2 refers to the cylindrical system. 
   //
   //       In this system, the first tangential coordinate is radius and
   //       the second tangential coordinate is longitude. The third,
   //       orthogonal coordinate is Z.
   //        
   //
   public static final int                  CYLSYS = 2;

   //
   //       Code 3 refers to the rectangular system. 
   //
   //       In this system, the first tangential coordinate is X and
   //       the second tangential coordinate is Y. The third,
   //       orthogonal coordinate is Z.
   //        
   //
   public static final int                  RECSYS = 3;

   //
   //       Code 4 refers to the planetodetic/geodetic system. 
   //
   //       In this system, the first tangential coordinate is longitude
   //       and the second tangential coordinate is planetodectic
   //       latitude. The third, orthogonal coordinate is altitude.
   //        
   //
   public static final int                  PDTSYS = 4;




   //
   // Private fields
   //

   private int                surfce;
   private int                center;
   private int                dclass;
   private int                dtype;
   private int                frmcde;
   private int                corsys;
   private double[]           corpar;
   private double             co1min;     
   private double             co1max;
   private double             co2min;
   private double             co2max;
   private double             co3min;
   private double             co3max;
   private double             start;
   private double             stop;
  

   //
   // Constructors
   //

   /**
   Construct a DSK descriptor instance from an array of doubles.
   */
   public DSKDescriptor ( double[] descrArray )
   {
      surfce = (int)descrArray[ SRFIDX ];
      center = (int)descrArray[ CTRIDX ];
      dclass = (int)descrArray[ CLSIDX ];
      dtype  = (int)descrArray[ TYPIDX ];
      frmcde = (int)descrArray[ FRMIDX ];
      corsys = (int)descrArray[ SYSIDX ];

      corpar = new double[ NSYPAR ];

      System.arraycopy ( descrArray, PARIDX, corpar, 0, NSYPAR );

      co1min = descrArray[ MN1IDX ];
      co1max = descrArray[ MX1IDX ];
      co2min = descrArray[ MN2IDX ];
      co2max = descrArray[ MX2IDX ];
      co3min = descrArray[ MN3IDX ];
      co3max = descrArray[ MX3IDX ];
      start  = descrArray[ BTMIDX ];  
      stop   = descrArray[ ETMIDX ];
   }


   /**
   No-arguments constructor.
   */
   public DSKDescriptor()
   {
      // 
      // Allocate space for coordinate parameters, since
      // this array will be assumed to be non-null by other
      // methods.
      //
   
      corpar= new double[ NSYPAR ];
   }


   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public DSKDescriptor( DSKDescriptor dskdsc )
   {
      this.surfce = dskdsc.surfce;
      this.center = dskdsc.center;
      this.dclass = dskdsc.dclass;
      this.dtype  = dskdsc.dtype;
      this.frmcde = dskdsc.frmcde;
      this.corsys = dskdsc.corsys;

      this.corpar = new double[ NSYPAR ];

      System.arraycopy ( dskdsc.corpar, 0, this.corpar, 0, NSYPAR );

      this.co1max = dskdsc.co1max;
      this.co1min = dskdsc.co1min;
      this.co2max = dskdsc.co2max;
      this.co2min = dskdsc.co2min;
      this.co3max = dskdsc.co3max;
      this.co3min = dskdsc.co3min;
      this.start  = dskdsc.start;
      this.stop   = dskdsc.stop;
   }


   //
   // Instance Methods
   //

   /**
   Get surface ID.
   */
   public int getSurfaceID()
   {
      return( surfce );
   }


   /**
   Get central body ID.
   */
   public int getCenterID()
   {
      return( center );
   }

   /**
   Get data class.
   */
   public int getDataClass()
   {
      return( dclass );
   }

   /**
   Get data type.
   */
   public int getDataType()
   {
      return( dtype );
   }

   /**
   Get reference frame ID code.
   */
   public int getFrameID()
   {
      return( frmcde );
   }

   /**
   Get coordinate system ID.
   */
   public int getCoordSysID()
   {
      return( corsys );
   }

   /**
   Get coordinate system parameters.
   */
   public double[] getCoordParams()
   {
      double[] params = new double[ NSYPAR ];

      System.arraycopy( corpar, 0, params, 0, NSYPAR );

      return( params );
   }


   /**
   Get coordinate bounds.

   <pre>
   The output array contains the minimum and maximum values
   of the ith coordinate, respectively, in the elements
   indexed

      [i][0]
      [i][1]

   The range of i is [0,2].

   </pre>
   */
   public double[][] getCoordBounds()
   {
      final int SIZE  = 3;

      double[][] bounds = new double[ SIZE ][2];

      bounds[0][0] = co1min;
      bounds[0][1] = co1max;
      bounds[1][0] = co2min;
      bounds[1][1] = co2max;
      bounds[2][0] = co3min;
      bounds[2][1] = co3max;

      return( bounds );
   }


   /**
   Get time bounds.
   */
   public double[] getTimeBounds()
   { 
      final int SIZE  = 2;

      double[] bounds = new double[ SIZE ];
   
      bounds[0] = start;
      bounds[1] = stop;

      return( bounds );
   }


   /**
   Extract descriptor contents into an array of type double.
   */ 
   public double[] toArray()
   {
      double[] retArray = new double[DSKDSZ];

      retArray[ SRFIDX ] = surfce;
      retArray[ CTRIDX ] = center;
      retArray[ CLSIDX ] = dclass;
      retArray[ TYPIDX ] = dtype;
      retArray[ FRMIDX ] = frmcde;
      retArray[ SYSIDX ] = corsys;

      System.arraycopy ( corpar, 0, retArray, PARIDX, NSYPAR );

      retArray[ MN1IDX ] = co1min;
      retArray[ MX1IDX ] = co1max;
      retArray[ MN2IDX ] = co2min;
      retArray[ MX2IDX ] = co2max;
      retArray[ MN3IDX ] = co3min;
      retArray[ MX3IDX ] = co3max;
      retArray[ BTMIDX ] = start;  
      retArray[ ETMIDX ] = stop;

      return( retArray );
   }  
 
}
