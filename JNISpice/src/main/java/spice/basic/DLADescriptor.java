
package spice.basic;


/**
Class DLADescriptor represents DLA segment descriptors
and declares DLA constants.

<p>
Within a DLA file, each DLA segment descriptor occupies a 
contiguous range of DAS integer addresses. The DLA segment 
components it describes are, respectively, contiguous 
ranges of DAS integer, double precision, and character 
addresses. Any of the components may be empty.

<p>
The base addresses stored in the descriptor are predecessors
of the first DAS addresses of the respective components.
DAS addresses are 1-based for all SPICE language versions.



<p> Version 1.0.0 14-NOV-2016 (NJB)
<pre>
</pre>
*/
public class DLADescriptor
{

   //
   // Public fields
   //


   //    Segment descriptor parameters

   //
   //
   //
   //       The DLA segment descriptor layout is:
   //
   //          +--------------------------+
   //          | Backward pointer         |      
   //          +--------------------------+
   //          | Forward pointer          |      
   //          +--------------------------+
   //          | Integer base address     |  
   //          +--------------------------+
   //          | Integer component size   |  
   //          +--------------------------+
   //          | Ref frame code           |  
   //          +--------------------------+
   //          | D.P. component size      |  
   //          +--------------------------+
   //          | Character base address   |  
   //          +--------------------------+
   //          | Character component size |  
   //          +--------------------------+
 
   //
   //    Parameters defining offsets for segment descriptor elements
   //    follow. The offsets are 0-based.
   //
   //

   /**
   Index of DLA backward segment pointer.
   */
   public static final int                  BWDIDX = 0;

   /**
   Index of DLA forward segment pointer.
   */
   public static final int                  FWDIDX = BWDIDX + 1;

   /**
   Index of DLA integer component base address.
   */
   public static final int                  IBSIDX = FWDIDX + 1;

   /**
   Index of DLA integer component size.
   */
   public static final int                  ISZIDX = IBSIDX + 1;

   /**
   Index of DLA double precision component base address.
   */
   public static final int                  DBSIDX = ISZIDX + 1;

   /**
   Index of DLA double precision component size.
   */
   public static final int                  DSZIDX = DBSIDX + 1;

   /**
   Index of DLA character component base address.
   */
   public static final int                  CBSIDX = DSZIDX + 1;

   /**
   Index of DLA character component size.
   */
   public static final int                  CSZIDX = CBSIDX + 1;
 
   //
   // Other public fields
   //

   /**
   Size of DLA descriptor array.
   */
   public static final int                  DLADSZ = 8;
 

   //
   // Private fields
   //

   private int                bwdptr;
   private int                fwdptr;
   private int                ibase;
   private int                isize;
   private int                dbase;
   private int                dsize;
   private int                cbase;
   private int                csize;
  

   //
   // Constructors
   //

   /**
   Construct a DLA descriptor instance from an array of ints.
   */
   public DLADescriptor ( int[] descrArray )
   {
      bwdptr = descrArray[ BWDIDX ];
      fwdptr = descrArray[ FWDIDX ];
      ibase  = descrArray[ IBSIDX ];  
      isize  = descrArray[ ISZIDX ];
      dbase  = descrArray[ DBSIDX ];  
      dsize  = descrArray[ DSZIDX ];
      cbase  = descrArray[ CBSIDX ];  
      csize  = descrArray[ CSZIDX ];  
   }

   /**
   No-arguments constructor.
   */
   public DLADescriptor()
   {
   }

  
   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public DLADescriptor( DLADescriptor dladsc )
   {
      this.bwdptr = dladsc.bwdptr;
      this.fwdptr = dladsc.fwdptr;
      this.ibase  = dladsc.ibase;
      this.isize  = dladsc.isize;
      this.dbase  = dladsc.dbase;
      this.dsize  = dladsc.dsize;
      this.cbase  = dladsc.cbase;
      this.csize  = dladsc.csize;
   }



   //
   // Instance Methods
   //


   /**
   Get DLA segment backward pointer.
   */
   public int getBackwardPointer()
   {
      return( bwdptr );
   }

   /**
   Get DLA segment forward pointer.
   */
   public int getForwardPointer()
   {
      return( fwdptr );
   }

   /**
   Get DLA integer component base address.
   */
   public int getIntBase()
   {
      return( ibase );
   }

   /**
   Get DLA integer component size. The size is a count
   of DAS integer addresses.
   */
   public int getIntSize()
   {
      return( isize );
   }



   /**
   Get DLA double precision component base address.
   */
   public int getDoubleBase()
   {
      return( dbase );
   }

   /**
   Get DLA double precision component size. The size is a count
   of DAS integer addresses.
   */
   public int getDoubleSize()
   {
      return( dsize );
   }

   /**
   Get DLA character component base address.
   */
   public int getCharBase()
   {
      return( cbase );
   }

   /**
   Get DLA character component size. The size is a count
   of DAS character addresses.
   */
   public int getCharSize()
   {
      return( csize );
   }

  
   /**
   Extract DLA descriptor contents into an int array.
   */
   public int[] toArray()
   {
      int[] retArray = new int[ DLADSZ ];

      retArray[BWDIDX] = bwdptr; 
      retArray[FWDIDX] = fwdptr;
      retArray[IBSIDX] = ibase; 
      retArray[ISZIDX] = isize;
      retArray[DBSIDX] = dbase; 
      retArray[DSZIDX] = dsize;
      retArray[CBSIDX] = cbase; 
      retArray[CSZIDX] = csize;

      return( retArray );      
   }
}
