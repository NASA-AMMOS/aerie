/*

-Procedure  zzalcell_c ( PRIVATE: allocate a SpiceCell dynamically )

-Abstract
 
   SPICE Private routine intended solely for the support of SPICE
   routines.  Users should not call this routine directly due
   to the volatile nature of this routine.  

   Allocate dynamic storage for a SpiceCell of specified type and
   dimensions. Fill in cell with supplied data. Return a pointer to
   an initialized SpiceCell structure.

-Disclaimer

   THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE
   CALIFORNIA INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S.
   GOVERNMENT CONTRACT WITH THE NATIONAL AERONAUTICS AND SPACE
   ADMINISTRATION (NASA). THE SOFTWARE IS TECHNOLOGY AND SOFTWARE
   PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS AND IS PROVIDED "AS-IS"
   TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, INCLUDING ANY
   WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR A
   PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC
   SECTIONS 2312-2313) OR FOR ANY PURPOSE WHATSOEVER, FOR THE
   SOFTWARE AND RELATED MATERIALS, HOWEVER USED.

   IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA
   BE LIABLE FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT
   LIMITED TO, INCIDENTAL OR CONSEQUENTIAL DAMAGES OF ANY KIND,
   INCLUDING ECONOMIC DAMAGE OR INJURY TO PROPERTY AND LOST PROFITS,
   REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE ADVISED, HAVE
   REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.

   RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF
   THE SOFTWARE AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY
   CALTECH AND NASA FOR ALL THIRD-PARTY CLAIMS RESULTING FROM THE
   ACTIONS OF RECIPIENT IN THE USE OF THE SOFTWARE.

-Required_Reading
 
   CELLS
   WINDOWS
 
-Keywords
 
   UTILITY

*/

   #include <stdio.h>
   #include <stddef.h>
   #include <stdlib.h>
   #include "SpiceUsr.h"
   #include "SpiceZmc.h"


   SpiceCell * zzalcell_c ( SpiceCellDataType    dataType,
                            SpiceInt             size,
                            SpiceInt             card,
                            SpiceInt             length,
                            const void         * data     )
/*

-Brief_I/O
 
   Variable  I/O  Description 
   --------  ---  -------------------------------------------------- 
   dataType   I   Data type of cell: character, d.p., or integer.
   size       I   Cell size.
   card       I   Cell cardinality.
   length     I   String length.
   data       I   Data pointer.

   The function returns a pointer to a SpiceCell containing the
   provided data.

-Detailed_Input
   
   dataType       is a member of the enumerated type SpiceCellDataType.
                  `dataType' may have any of the values
   
                     SPICE_CHR
                     SPICE_DP
                     SPICE_INT

                  See the headers SpiceCel.h and SpiceZdf.h for details.

   size           is the size (maximum allowed cardinality) of the
                  cell to be created.

   card           is the cardinality (number of occupied elements)
                  of the cell to be created.
 
   length         is the length of strings in the cell, if the
                  cell has character type.  `length' is ignored
                  if the cell has numeric type.

   data           is a pointer to the data with which the cell is to be
                  populated.  If the data have numeric type, they
                  should be organized as an array of dimension `card'.
                  If the data have character type, the data must be
                  organized as a two-dimensional character array having
                  dimensions

                     [card][length]

                  Each row of the array should be a null-terminated
                  string.
   
-Detailed_Output

   The function returns a pointer to a SpiceCell containing the
   provided data.  The SpiceCell and its data reside in dynamically
   allocated memory.  The data type of the cell is that specified
   by the input argument `dataType'.  

   The control area of the returned cell is initialized, the cell
   contains the data provided via the argument `data', and the cell
   has the size and cardinality indicated by the arguments `size'
   and `card' respectively. 

   If the cell has character type, and if the size exceeds the 
   cardinality, the unoccupied portion of the cell's data area
   is initialized to contain zero-length strings.  As a consequence,
   if `cell' is a character cell created by this routine, the macro call

      SPICE_CELL_ELEM_C ( cell, i )

   expands to a pointer to the string 

      ""

   if `i' is greater than the cardinality of `cell' and less than or
   equal the size of `cell'.
 
   The cell must be deallocated via zzdacell_c, never by free().

-Parameters
 
   None. 
 
-Exceptions
 
   1) If input `size' is negative, the error SPICE(INVALIDSIZE)
      is signaled.

   2) If input `card' is negative, the error SPICE(INVALIDCARDINALITY)
      is signaled.
 
   3) If the input `card' is positive and the input data pointer 
      is null, the error SPICE(NULLPOINTER) is signaled.

   4) If the cell has character type and the string length is 
      non-positive, the error SPICE(INVALIDSIZE) is signaled.

   5) If the input data type is not one of

         SPICE_CHR
         SPICE_DP
         SPICE_INT

      the error SPICE(NOTSUPPORTED) is signaled.

   6) If a memory allocation attempt fails, the error SPICE(MALLOCFAILED)
      is signaled.

-Files
 
   None. 
 
-Particulars
 
   Cells allocated by this routine reside in dynamically allocated
   memory.  Each cell occupies two distinct blocks of contiguous
   memory:  one for a SpiceCell structure itself, and one for the data
   associated with the structure.
 
   To avoid memory leaks, applications normally should deallocate
   cells created using this routine.  The private CSPICE utility
   routine

      zzdacell_c   { Private:  deallocate cell }

   is provided for this purpose.

-Examples
 
   1) Create a dyamically allocated CSPICE window from a double
      precision array.

         #include "SpiceUsr.h"
            .
            .
            .

         #define              SIZE       10
         #define              CARD       4
         SpiceDouble          dpData = { -1.0, 2.0, 5.0, 5.5 };

         /.
         Unlike normal usage, we don't use the macro call

            SPICEDOUBLE_CELL ( name, size )
        
         to declare this cell.  Instead we just declare a cell
         pointer.
         ./
         SpiceCell          * window;

         /.
         Allocate a double precision cell and populate it with data.
         We use the enumerated type value SPICE_DP to indicate
         the data type.  The string length is set to zero since
         this cell has numeric type.
         ./
         window = zzalcell_c ( SPICE_DP, SIZE, CARD, 0, dpData ); 

         /.
         As a safety measure, validate the window.  This ensures
         that `window' contains a CSPICE window as opposed to just a 
         cell.  (In this case, we had a window by construction.)
         ./
         wnvald_c ( SIZE, CARD, window );

         if ( failed_c() )
         {
            [ handle error condition ]
         }

         /.
         At this point, `window' is a valid input argument for the
         CSPICE window routines.  
         ./

         [ use window... ] 
          
         /.
         When we're done with the window, de-allocate it:
         ./
         zzdacell_c ( window );


-Restrictions
 
   1) This is a private routine.  It should not be called directly
      by user applications.  The interface and functionality may
      change.
 
   2) The SpiceCell pointer returned by this routine must not be
      passed to free().  Instead, deallocate the cell using the
      CSPICE routine zzdacell_c.

   3) Character string data passed to this routine must consist of
      null-terminated strings.
 
-Literature_References
 
   None.

-Author_and_Institution
 
   N.J. Bachman   (JPL) 
 
-Version
 
   -CSPICE Version 2.0.0, 20-AUG-2009 (NJB)

      Zero-size cells are now allowed.

   -CSPICE Version 1.0.0, 02-DEC-2003 (NJB)

-Index_Entries
 
   None.
 
-&
*/

{
   /*
   Local constants 
   */ 
   #define CTRLSZ           SPICE_CELL_CTRLSZ

   /*
   Local variables 
   */ 
   SpiceCell             * cellPtr = 0;
   SpiceInt                dataOffset;
   SpiceInt                nCellBytes;
   SpiceInt                nDataBytes;

   void                  * basePtr;


   /*
   Standard SPICE error handling. 
   */
   if ( return_c() )
   {
      return cellPtr;
   }
   chkin_c ( "zzalcell_c"  );


   /*
   Check the data array size.
   */
   if ( size < 0 )
   {
      setmsg_c ( "Cell data array size # is negative."  );
      errint_c ( "#",  size                             );
      sigerr_c ( "SPICE(INVALIDSIZE)"                   );
      chkout_c ( "zzalcell_c"                           );

      return ( (SpiceCell *)0 );
   }

   /*
   Check the data array cardinality.
   */
   if ( card < 0 )
   {
      setmsg_c ( "Cell data array cardinality # is non-positive."  );
      errint_c ( "#",  card                                        );
      sigerr_c ( "SPICE(INVALIDCARDINALITY)"                       );
      chkout_c ( "zzalcell_c"                                      );
      
      return ( (SpiceCell *)0 );
   }

   /*
   Check the data pointer.  Unless the cardinality of the input 
   endpoint set is zero, signal an error and return if we
   have a null pointer.
   */
   if ( card > 0 ) 
   {
      CHKPTR_VAL ( CHK_STANDARD, "zzalcell", data, (SpiceCell *)0 );
   }

   /*
   Determine the size of a SpiceCell structure. 
   */
   nCellBytes  = sizeof ( SpiceCell );

   /*
   Allocate a memory block to hold the SpiceCell structure. 
   */
   cellPtr = malloc ( nCellBytes );

   if ( !cellPtr )
   {
      setmsg_c ( "Could not allocate # bytes of memory for "
                 "SpiceCell structure."                     );
      errint_c ( "#",  nCellBytes                           );
      sigerr_c ( "SPICE(MALLOCFAILED)"                      );
      chkout_c ( "zzalcell_c"                               );

      return cellPtr;
   }

   /*
   From this point onward, if an error is signaled, the memory
   referenced by cellPtr must be freed.

   Determine the size of the data block with which the SpiceCell 
   structure is associated.
   */
   if ( dataType == SPICE_INT )
   {
      nCellBytes = ( CTRLSZ + size ) * sizeof ( SpiceInt );
      nDataBytes =       card        * sizeof ( SpiceInt );

      dataOffset =   CTRLSZ * sizeof ( SpiceInt );
   }

   else if ( dataType == SPICE_DP )
   {
      nCellBytes =  ( CTRLSZ + size ) * sizeof ( SpiceDouble );
      nDataBytes =       card         * sizeof ( SpiceDouble ); 

      dataOffset =   CTRLSZ * sizeof ( SpiceDouble );
   }

   else if ( dataType == SPICE_CHR )
   {
      /*
      Check the string length.
      */
      if ( length < 1 )
      {
         setmsg_c ( "Cell string length # is non-positive."  );
         errint_c ( "#",  length                             );
         sigerr_c ( "SPICE(INVALIDSIZE)"                     );
         chkout_c ( "zzalcell_c"                             );

         free ( cellPtr );

         return ( (SpiceCell *)0 );
      }

      nCellBytes =  ( CTRLSZ + size ) * length * sizeof ( SpiceChar );
      nDataBytes =       card         * length * sizeof ( SpiceChar );  
  
      dataOffset =    CTRLSZ * length * sizeof ( SpiceChar );
   }

   else
   {
      setmsg_c ( "Cell data type # not supported." );
      errint_c ( "#",  (SpiceInt)dataType          );
      sigerr_c ( "SPICE(NOTSUPPORTED)"             );
      chkout_c ( "zzalcell_c"                      );

      free ( cellPtr );

      return ( (SpiceCell *)0 );
   }

   /*
   Allocate the cell's data array.
   */
   basePtr = malloc ( nCellBytes );

   if ( !basePtr )
   {
      setmsg_c ( "Could not allocate # bytes of memory for "
                 "SpiceCell data array"                     );
      errint_c ( "#",  nCellBytes                           );
      sigerr_c ( "SPICE(MALLOCFAILED)"                      );
      chkout_c ( "zzalcell_c"                               );

      free ( cellPtr );

      return ( (SpiceCell *)0 );
   }

   /*
   We've allocated memory for the cell's data.  Fill in the cell 
   structure. 
   */
   cellPtr->dtype   = dataType;
   cellPtr->length  = length;
   cellPtr->size    = size;
   cellPtr->card    = card;
   cellPtr->isSet   = SPICEFALSE;
   cellPtr->adjust  = SPICEFALSE;
   cellPtr->init    = SPICEFALSE;
   cellPtr->base    = basePtr;
   cellPtr->data    = (char *)basePtr  +  dataOffset;

   /*
   Initialize the control area of the data array. 
   */
   ssize_c ( size, cellPtr );
   scard_c ( card, cellPtr );

   /*
   Copy the input data into the data array. 
   */
   if ( nDataBytes > 0 )
   {
      memmove ( cellPtr->data, data, nDataBytes );
   }

   chkout_c ( "zzalcell_c" );

   return cellPtr;
}


