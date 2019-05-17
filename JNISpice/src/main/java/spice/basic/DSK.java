
package spice.basic;

import static spice.basic.DSKDescriptor.*;

/**
Class DSK supports segment-level access to DSK files; this
class also provides methods to obtain DSK coverage 
information and to access DSK subsystem tolerance parameters.

<p> Many SPICE-based user applications won't need to make
direct use of the DSK class. A typical computation involving
DSK data can be performed by loading one or more DSK files
via {@link KernelDatabase#load} and then calling high-level
computational methods, for example constructors of class
{@link SurfaceIntercept}.

<p>DSK segment list traversal methods are inherited from
class {@link DLA}.

<p>Methods specific to DSK data type 2 are provided in
class {@link CSPICE}. Additional methods for computations
involving triangular plates are provided in classes
{@link TriangularPlate} and {@link TriangularPlateVertices}.



<h3> Version 3.0.0 09-JAN-2017 (NJB) </h3>

<p> This class is now derived from the DLA class.

<p>
   DSK descriptor parameters are now declared in class 
   {@link spice.basic.DSKDescriptor}.

<p>  
   Data class parameter value SPHCLS was changed to SVFCLS.
   Data class parameter GENCLS was added.

<p>
   DSK tolerance keyword parameters are now declared in class 
   {@link spice.basic.DSKToleranceKey}.

<p> The internal file name is no longer an instance field of this class.


<h3> Version 2.0.0 08-JUN-2014 (NJB) </h3>
<pre>
Index parameters were changed from Fortran style (1-based)
to Java style (0-based).
</pre>
<h3> Version 1.0.0 18-SEP-2010 (NJB)</h3>
*/
public class DSK extends DLA
{

   //
   // Public fields
   //

   //
   // Constructors
   //

   /**
   Construct a DSK instance representing a file.
   The file need not exist.

   <p> User applications will not need to call this
   constructor directly. See the methods {@link #openForRead}
   and {@link #openForWrite}.

   */

   protected DSK ( String filename )

      throws SpiceErrorException
   {
       super( filename );
   }


   /**
   No-arguments constructor.
   */
   public DSK()
   {
      readable = false; 
      writable = false;
   }


   /**
   Construct a DSK instance from a DAS instance. This
   constructor creates a deep copy.

   <p> User applications usually will not need to call this
   constructor directly. See the methods {@link #openForRead}
   and {@link #openForWrite}.
   */
   public DSK ( DAS das )

      throws SpiceException
   {
      super();

      //
      // Copy the attributes of the DAS file.
      //
      this.fileName         = das.getFileName();
      this.handle           = das.getHandle();
      this.readable         = das.isReadable();
      this.writable         = das.isWritable();

      //
      // Check attributes of DAS file. The file must have 
      // file type DSK.
      //
      String[] archArray = new String[1];
      String[] typeArray = new String[1];

      CSPICE.getfat( fileName, archArray, typeArray );

      if (     ( !  (archArray[0]).equals ( "DAS" )  )
           ||  ( !  (typeArray[0]).equals ( "DSK" )  )   )
      {
         SpiceErrorException exc = 

            SpiceErrorException.create( "DSK( DAS das )",
                                        "SPICE(NOTADSKFILE)",
                                        "The input DAS file has " + 
                                        "architecture "           +
                                        archArray[0]              +
                                        " and file type "         +
                                        typeArray[0]              +
                                        " The file can't be "     +
                                        "used to construct "      +
                                        "a DSK."                    );
         throw( exc );
      }

   }


   //
   // Static methods
   //
   
   /**
   Open a DSK file for read access.
   */
   public static DSK openForRead ( String filename )
     
      throws SpiceException
   {
       //
       // Get a DAS instance associated with the file.
       // The DAS is opened for read access.
       //
       // Create a DSK instance from the DAS instance.
       //
       return(  new DSK( DAS.openForRead(filename) )  );
   }


   /**
   Open a DSK file for write access.
   */
   public static DSK openForWrite ( String filename )
     
      throws SpiceException
   {
       //
       // Get a DAS instance associated with the file.
       // The DAS is opened for write access.
       //
       // Create a DSK instance from the DAS instance.
       //
       return(  new DSK( DAS.openForWrite(filename) )  );
   }


   //
   // Instance methods
   //

   /**
   Get a DSKDescriptor for a specified DSK segment. The
   segment is identified by its DLA Descriptor.
   */
   public DSKDescriptor getDSKDescriptor( DLADescriptor dladsc )

      throws SpiceException
   {
      double[] dskDscArray 

         = CSPICE.dskgd ( this.handle, dladsc.toArray() );

      return (  new DSKDescriptor( dskDscArray )  );
   }


   /**
   Get the set of Bodies covered by a DSK file.
   */
   public Body[] getBodies()
 
      throws SpiceException
   {
      //
      // We must provide a maximum size for the output array to 
      // be created by CSPICE.dskobj.
      //
      int maxsize      = Math.max( 1, getSegmentCount() );

      //
      // The initial ID set is empty.
      //
      int[] initialSet = new int[0];

      int[] objArray = CSPICE.dskobj( fileName, maxsize, initialSet );

      int n = objArray.length;

      Body[] retArray = new Body[n];

      for ( int i = 0;  i < n;  i++ )
      {
         retArray[i] = new Body( objArray[i] );
      }

      return( retArray );
   }



   /**
   Get the set of Surfaces associated with a specified Body in a DSK file.
   */
   public Surface[] getSurfaces( Body b )
 
      throws SpiceException
   {
      //
      // We must provide a maximum size for the output array to 
      // be created by CSPICE.dsksrf.
      //
      int maxsize      = Math.max( 1, getSegmentCount() );

      //
      // The initial ID set is empty.
      //
      int[] initialSet = new int[0];

      int[] srfArray = CSPICE.dsksrf( fileName, b.getIDCode(), 
                                      maxsize,  initialSet     );

      int n = srfArray.length;

      Surface[] retArray = new Surface[n];

      for ( int i = 0;  i < n;  i++ )
      {
         retArray[i] = new Surface( srfArray[i], b );
      }

      return( retArray );
   }


   
   /**
   Obtain the value of a DSK tolerance parameter.
   */
   public static double getTolerance ( DSKToleranceKey keyword )

      throws SpiceErrorException
   {
      return(  CSPICE.dskgtl( keyword.getIntKeyword() )  );
   }



   /**
   Set the value of a DSK tolerance parameter.
   */
   public static void setTolerance ( DSKToleranceKey keyword,
                                     double          value   )

      throws SpiceErrorException
   {
      CSPICE.dskstl( keyword.getIntKeyword(), value );
   }

}


