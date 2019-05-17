/*

-Procedure  zzdacell_c ( PRIVATE: deallocate a SpiceCell )

-Abstract
 
   SPICE Private routine intended solely for the support of SPICE
   routines.  Users should not call this routine directly due
   to the volatile nature of this routine.  

   Deallocate a cell allocated by zzalcell_c.

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


   void zzdacell_c ( SpiceCell  * cell )
/*

-Brief_I/O
 
   Variable  I/O  Description 
   --------  ---  -------------------------------------------------- 
   cell      I-O  Cell to be deallocated.

-Detailed_Input
   
   cell           is a pointer to a SpiceCell allocated by zzalcell_c.
   
-Detailed_Output

   cell           is no longer a valid pointer.

-Parameters
 
   None. 
 
-Exceptions
 
   1) If the input pointer `cell' is null, the error SPICE(NULLPOINTER)
      will be signaled.

   2) If the input pointer was not obtained via a call to 
      zzalcell, the memory deallocation attempted by this routine
      will fail.  Normally this will result in program termination.

-Files
 
   None. 
 
-Particulars
 
   Cells allocated by zzalcell_c reside in dynamically allocated
   memory.  Each cell occupies two distinct blocks of contiguous
   memory:  one for a SpiceCell structure itself, and one for the data
   associated with the structure.
 
   To avoid memory leaks, applications normally should use this routine
   to deallocate cells allocated by zzalcell_c.

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
         window = zzdacell_c ( SPICE_DP, SIZE, CARD, 0, dpData ); 

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
 
   2) The SpiceCell pointer passed to this routine must have been
      obtained via a call to zzalcell_c.
 
-Literature_References
 
   None.

-Author_and_Institution
 
   N.J. Bachman   (JPL) 
 
-Version
 
   -CSPICE Version 1.0.0, 02-DEC-2003 (NJB)

-Index_Entries
 
   None.
 
-&
*/

{
   /*
   Use discovery check-in.

   Check the data pointer.  Signal an error and return if we
   have a null pointer.
   */
   CHKPTR ( CHK_DISCOVER, "zzdacell", cell );

   
   /*
   Free the data array associated with the cell. 
   */
   free ( cell->base );

   /*
   Free the cell itself.
   */
   free ( cell );

}


