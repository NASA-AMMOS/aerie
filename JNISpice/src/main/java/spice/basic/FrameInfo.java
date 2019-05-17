
package spice.basic;

/**
Class FrameInfo packages specification
parameters common to all SPICE reference frames.

<p> Version 1.1.0 02-FEB-2011 (NJB)(EDW)

   <pre>
   Alpha JNISpice build for Ames Reasearch Center. This version of the FrameInfo
   class includes a constructor with an argument list of the frame class and
   frame class ID.
   </pre>

<p> Version 1.0.0 22-DEC-2009 (NJB)
*/
public class FrameInfo extends Object
{

   //
   // Fields
   //

   private int                 frameID;
   private int                 frameCenterID;
   private int                 frameClass;
   private int                 frameClassID;


   //
   // Consructors
   //

   /**
   Create a FrameInfo object from a frame class and frame class ID.
   */
   public FrameInfo( int              frameClass, 
                     int              frameClassID)

      throws FrameNotFoundException, SpiceException
      {
      int[]          frcodeArray  = new int[1];
      String[]       frnameArray  = new String[1];
      int[]          centerArray  = new int[1];
      boolean[]      foundArray   = new boolean[1];


      CSPICE.ccifrm( frameClass,
                     frameClassID,
                     frcodeArray,
                     frnameArray,
                     centerArray,
                     foundArray  );

      //
      // Ignore the availablity of the frame name for this implementation.
      //
      if ( !foundArray[0] )
         {
         String msg = "Frame info for frame class code " +
                       frameClass             +
                       " and frame class ID " +
                       frameClassID           +
                       " was not found.";

         FrameNotFoundException exc =
                           FrameNotFoundException.create( "RefFrameInfo", msg );

         throw( exc );
         }

      this.frameID        = frcodeArray[0];
      this.frameCenterID  = centerArray[0];
      this.frameClass     = frameClass;
      this.frameClassID   = frameClassID;
      }



   /**
   Copy constructor. This constructor creates a deep copy.
   */
   public FrameInfo( FrameInfo  info )
   {
      this.frameID        = info.frameID;
      this.frameCenterID  = info.frameCenterID;
      this.frameClass     = info.frameClass;
      this.frameClassID   = info.frameClassID;
   }


   /**
   Create a FrameInfo instance from a SPICE frame ID code.
   */
   public FrameInfo( int  frameIDCode )

      throws FrameNotFoundException, SpiceException
   {
      int[]          frameCenterArray  = new int[1];
      int[]          frameClassArray   = new int[1];
      int[]          frameClassIDArray = new int[1];
      boolean[]      foundArray        = new boolean[1];

      CSPICE.frinfo( frameIDCode,
                     frameCenterArray,
                     frameClassArray,
                     frameClassIDArray,
                     foundArray         );

      if ( !foundArray[0] )
      {
         String msg = "Frame info for frame ID code " +
                      frameIDCode + " was not found.";

         FrameNotFoundException exc =

            FrameNotFoundException.create( "FrameInfo", msg );

         throw( exc );
      }

      this.frameID       = frameIDCode;
      this.frameCenterID = frameCenterArray[0];
      this.frameClass    = frameClassArray[0];
      this.frameClassID  = frameClassIDArray[0];
   }


   /**
   Create a FrameInfo instance from a ReferenceFrame.
   */
   public FrameInfo( ReferenceFrame ref )

      throws FrameNotFoundException, SpiceException
   {
      int IDCode     = ref.getIDCode();

      FrameInfo info = new FrameInfo( IDCode );

      this.frameID       = info.frameID;
      this.frameCenterID = info.frameCenterID;
      this.frameClass    = info.frameClass;
      this.frameClassID  = info.frameClassID;
   }


   //
   // Methods
   //

   /**
   Return the frame ID code for this instance.
   */
   public int getFrameID()
   {
      return( frameID );
   }

   /**
   Return the frame center ID code for this instance.
   */
   public int getFrameCenterID()
   {
      return( frameCenterID );
   }


   /**
   Return the frame class for this instance.
   */
   public int getFrameClass()
   {
      return( frameClass );
   }


   /**
   Return the frame class ID for this instance.
   */
   public int getFrameClassID()
   {
      return( frameClassID );
   }

   /**
   Convert this instance to a String.
   */
   public String toString()
   {
      String outStr;

      try
      {
         outStr = String.format(

            "Frame ID:          %d%n" +
            "Frame center ID:   %d%n" +
            "Frame class:       %d%n" +
            "Frame class ID:    %d%n",
            frameID,
            frameCenterID,
            frameClass,
            frameClassID               );
      }
      catch ( Exception exc )
      {
         outStr = exc.getMessage();
      }

      return ( outStr );
   }
   
}
