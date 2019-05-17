
package spice.basic;


/**
Class DSK02 declares constants pertaining to type 2 DSK segments.

<p> Version 2.0.0 18-OCT-2016 (NJB)

   Updated constants to make them compatible with SPICELIB.

   Added declarations of spatial index constants.


<p> Version 1.0.0 20-SEP-2010 (NJB)
*/
public class DSK02
{
   //
   // Public fields
   //
   //
   //    Each type 2 DSK segment has integer, d.p., and character
   //    components.  The segment layout in DAS address space is as
   //    follows:
   //
   //
   //       Integer layout:
   //
   //          +-----------------+
   //          | NV              |  (# of vertices)
   //          +-----------------+
   //          | NP              |  (# of plates )
   //          +-----------------+
   //          | NVXTOT          |  (total number of voxels)
   //          +-----------------+
   //          | VGREXT          |  (voxel grid extents, 3 integers)   
   //          +-----------------+
   //          | CGRSCL          |  (coarse voxel grid scale, 1 integer)  
   //          +-----------------+
   //          | VOXNPT          |  (size of voxel-plate pointer list)
   //          +-----------------+
   //          | VOXNPL          |  (size of voxel-plate list)
   //          +-----------------+
   //          | VTXNPL          |  (size of vertex-plate list)
   //          +-----------------+
   //          | PLATES          |  (NP 3-tuples of vertex IDs)
   //          +-----------------+
   //          | VOXPTR          |  (voxel-plate pointer array)
   //          +-----------------+
   //          | VOXPLT          |  (voxel-plate list)
   //          +-----------------+
   //          | VTXPTR          |  (vertex-plate pointer array)
   //          +-----------------+
   //          | VTXPLT          |  (vertex-plate list)
   //          +-----------------+
   //          | CGRPTR          |  (coarse grid occupancy pointers)
   //          +-----------------+
   //


   
   //
   //    Parameters defining offsets for integer segment components
   //    follow. The components are as shown in the diagram above,
   //    starting with the index of the vertex count.
   //
   //    The indices declared below serve as offsets from the
   //    base of the corresponding segment component. These offsets
   //    are added to the component's base address to produce a 
   //    DAS address.
   //
   //    This addressing mechanism is language independent. Indices
   //    are 1-based.
   //
   //
   //       Index of number of vertices:
   //
   public static final int                  IXNV   = 1;
   //
   //       Index of number of plates:
   //
   public static final int                  IXNP   = IXNV   + 1;
   //
   //       Index of total voxel count:
   //   
   public static final int                  IXNVXT = IXNP   + 1;
   //
   //       Index of voxel grid extents:
   //   
   public static final int                  IXVGRX = IXNVXT + 1;
   //
   //       Index of coarse voxel grid scale:
   //   
   public static final int                  IXCGSC = IXVGRX + 3;
   //
   //       Index of size of voxel-plate pointer list:
   //   
   public static final int                  IXVXPS = IXCGSC + 1;
   //
   //       Index of size of voxel-plate list:
   //   
   public static final int                  IXVXLS = IXVXPS + 1;
   //
   //       Index of size of vertex-plate list:
   //   
   public static final int                  IXVTLS = IXVXLS + 1;
   //
   //       Index of first address of plate set:
   //   
   public static final int                  IXPLAT = IXVTLS + 1;

   //
   //    Note that integer data following the base of the plate
   //    array have variable size, so there are no parameters
   //    pointing to the bases of these items. The addresses are
   //    computed at run time.
   //


   //
   //    D.p. layout:
   //        
   //       +-----------------+
   //       | DSK descriptor  |  DSKDSZ elements
   //       +-----------------+
   //       | Vertex bounds   |  6 values (min/max for each component)
   //       +-----------------+
   //       | Voxel origin    |  3 elements
   //       +-----------------+
   //       | Voxel size      |  1 element
   //       +-----------------+
   //       | Vertices        |  3*NV elements
   //       +-----------------+
   //

   //
   //       Index of segment descriptor in d.p. DLA segment component:
   //   
   public static final int                  IXDSCR = 1;
   //
   //       DSK descriptor size: this local parameter MUST be kept
   //       consistent with the parameter DSKDSZ which is declared in
   //       the SPICELIB INCLUDE file dskdsc.inc.
   // 
   public static final int                  DSCSZ2 = 24;  
   //
   //       Index of vertex bounds:
   //   
   public static final int                  IXVTBD = IXDSCR + DSCSZ2;
   //
   //       Index of voxel grid origin:
   //   
   public static final int                  IXVXOR = IXVTBD + 6;
   //
   //       Index of voxel size:
   //   
   public static final int                  IXVXSZ = IXVXOR + 3;
   //
   //       Index of first address of vertex array:
   //   
   public static final int                  IXVERT = IXVXSZ + 1;

   //
   //    Keywords used by fetch routines:
   //

   //
   //       Integer item keyword parameters:
   // 
   //
   //       Keyword for number of vertices:
   //
   public static final int                  KWNV   = 1;
   //
   //       Keyword for number of plates:
   //
   public static final int                  KWNP   = KWNV   +  1;
   //
   //       Keyword for total voxel count:
   //   
   public static final int                  KWNVXT = KWNP   + 1;
   //
   //       Keyword for voxel grid extents:
   //   
   public static final int                  KWVGRX = KWNVXT + 1;
   //
   //       Keyword for coarse voxel grid scale:
   //   
   public static final int                  KWCGSC = KWVGRX + 1;
   //
   //       Keyword for size of voxel-plate pointer list:
   //   
   public static final int                  KWVXPS = KWCGSC + 1;
   //
   //       Keyword for size of voxel-plate list:
   //   
   public static final int                  KWVXLS = KWVXPS + 1;
   //
   //       Keyword for size of vertex-plate list:
   //   
   public static final int                  KWVTLS = KWVXLS + 1;
   //
   //       Keyword for plate set:
   //   
   public static final int                  KWPLAT = KWVTLS + 1;
   //
   //       Keyword for voxel-plate pointer list:
   //   
   public static final int                  KWVXPT = KWPLAT + 1;
   //
   //       Keyword for voxel-plate list:
   //   
   public static final int                  KWVXPL = KWVXPT + 1;
   //
   //       Keyword for vertex-plate pointer list:
   //   
   public static final int                  KWVTPT = KWVXPL + 1;
   //
   //       Keyword for vertex-plate list:
   //   
   public static final int                  KWVTPL = KWVTPT + 1;
   //
   //       Keyword for coarse voxel grid pointer list:
   //   
   public static final int                  KWCGPT = KWVTPL + 1;



   //
   //    Double precision item keyword parameters:
   // 

   //
   //       Keyword for DSK descriptor:
   //   
   public static final int                  KWDSC  = KWCGPT + 1;
   //
   //       Keyword for vertex bounds:
   //   
   public static final int                  KWVTBD = KWDSC  + 1;
   //
   //       Keyword for voxel grid origin:
   //   
   public static final int                  KWVXOR = KWVTBD + 1;
   //
   //       Keyword for voxel size:
   //   
   public static final int                  KWVXSZ = KWVXOR + 1;
   //
   //       Keyword for vertex set:
   //   
   public static final int                  KWVERT = KWVXSZ + 1;
 
     
   //
   //    DSK type 2 plate model capacity limit parameters:
   // 

   //
   //  MAXVRT is the maximum number of vertices the triangular
   //         plate model software will support.
   //
   public static final int                  MAXVRT = 16000002;

   //
   //  MAXPLT is the maximum number of plates that the triangular
   //         plate model software will support.
   //
   public static final int                  MAXPLT = 2*(MAXVRT-2);

   //
   //  MAXNPV is the maximum allowed number of vertices, 
   //  not taking into account shared vertices. 
   //
   public static final int                  MAXNPV = (3*MAXPLT/2) + 1;

   //
   //  MAXVOX is the maximum number of voxels.
   //
   public static final int                  MAXVOX = 100000000;

   //
   //  MAXCGR is the maximum size of the coarse voxel grid.
   //  
   public static final int                  MAXCGR = 100000;

   //
   //  MAXEDG is the maximum allowed number of vertex or plate 
   //  neighbors a vertex may have. 
   //
   public static final int                  MAXEDG = 120;



   //
   // DSK type 2 spatial index parameters
   // ===================================
   //
   //    DSK type 2 spatial index integer component
   //    ------------------------------------------
   //
   //    This is the layout in memory of an array containing
   //    the integer component of a type 2 spatial index:   
   //
   //       +-----------------+
   //       | VGREXT          |  (voxel grid extents, 3 integers)   
   //       +-----------------+
   //       | CGRSCL          |  (coarse voxel grid scale, 1 integer)  
   //       +-----------------+
   //       | VOXNPT          |  (size of voxel-plate pointer list)
   //       +-----------------+
   //       | VOXNPL          |  (size of voxel-plate list)
   //       +-----------------+
   //       | VTXNPL          |  (size of vertex-plate list)
   //       +-----------------+
   //       | CGRPTR          |  (coarse grid occupancy pointers)
   //       +-----------------+
   //       | VOXPTR          |  (voxel-plate pointer array)
   //       +-----------------+
   //       | VOXPLT          |  (voxel-plate list)
   //       +-----------------+
   //       | VTXPTR          |  (vertex-plate pointer array)
   //       +-----------------+
   //       | VTXPLT          |  (vertex-plate list)
   //       +-----------------+
   //
   //
   //    Index parameters
   //
   //       Indices are language-dependent and for JNISpice are 0-based.
   //
   // Grid extents:
   //
   public static final int                  SIVGRX = 0;  
 
   //
   // Coarse grid scale:
   //
   public static final int                  SICGSC = SIVGRX + 3;  

   //
   // Voxel pointer count:
   //
   public static final int                  SIVXNP = SICGSC + 1;  

   //
   // Voxel-plate list count:
   //
   public static final int                  SIVXNL = SIVXNP + 1;  

   //
   // Vertex-plate list count:
   //
   public static final int                  SIVTNL = SIVXNL + 1;  
 
   //
   // Coarse grid pointers:
   //
   public static final int                  SICGRD = SIVTNL + 1;  
 
   //
   // Size of fixed-size portion of integer component:
   // 
   public static final int                  IXIFIX = MAXCGR + 7;  

   //
   //    DSK type 2 spatial index double precision component
   //    ---------------------------------------------------
   //
   //       +-----------------+
   //       | Vertex bounds   |  6 values (min/max for each component)
   //       +-----------------+
   //       | Voxel origin    |  3 elements
   //       +-----------------+
   //       | Voxel size      |  1 element
   //       +-----------------+
   //
  
   //
   //    Index parameters
   //
   //       Indices are language-dependent and for JNISpice are 0-based.
   //
  
   //
   // Vertex bounds:
   //
   public static final int                  SIVTBD = 0;  

   //
   // Voxel grid origin:
   //
   public static final int                  SIVXOR = SIVTBD + 6;  

   //
   // Voxel size:  
   //
   public static final int                  SIVXSZ = SIVXOR + 3;  
 
   
   //
   // Other d.p. spatial index parameters:
   //


   //
   // Size of fixed-size portion of double precision component:
   // 
   public static final int                  IXDFIX = 10;  
 
   //
   // The limits below are used to define a suggested maximum
   // size for the integer component of the spatial index. 
   //

   //
   // Maximum number of entries in voxel-plate pointer array:
   //           
   public static final int                  MAXVXP = MAXPLT / 2;  

   //
   // Maximum cell size:
   //
   public static final int                  MAXCEL = 60000000;  
 
   //
   // Maximum number of entries in voxel-plate list:
   //
   public static final int                  MXNVLS = MAXCEL + MAXVXP/2;  
 
   //
   // Spatial index integer component size:
   //
   public static final int                  SPAISZ =   IXIFIX + MAXVXP  
                                                     + MXNVLS + MAXVRT
                                                     + MAXNPV;
}





