
package spice.basic;



/**
Class KernelPool supports access to the kernel pool
data structure.

<p> Version 1.0.0 08-JAN-2010 (NJB)
*/
public class KernelPool extends Object
{
   //
   // Public constants
   //
   public static final int    CHARACTER  =  0;
   public static final int    NUMERIC    =  1;

   public static final String CSPICE_CHARACTER  = "C";
   public static final String CSPICE_NUMERIC    = "N";


   //
   // Private methods
   //

   /**
   Map a CSPICE data type name to a KernelVarDescriptor int parameter.
   */
   private static int mapDataType( String cspiceType )

      throws SpiceException
   {
      int dataType;

      if ( CSPICE.eqstr( cspiceType, CSPICE_CHARACTER ) )
      {
         dataType = CHARACTER;
      }
      else if ( CSPICE.eqstr( cspiceType, CSPICE_NUMERIC ) )
      {
         dataType = NUMERIC;
      }
      else
      {
         String msg = "The data type " + cspiceType +
                      "was not recognized.";

         SpiceException exc = new SpiceException(msg);

         throw ( exc );
      }

      return(  dataType  );
   }


   /**
   Make sure a variable of a given name and data type exists.
   Signal a {@link KernelVarNotFound} exception if not.
   Return the size of the variable.
   */
   private static int checkVar ( String name,
                                 int    dataType )

      throws SpiceException, KernelVarNotFoundException
   {

      String          typeStr;

      if ( dataType == CHARACTER )
      {
         typeStr = "character";
      }
      else
      {
         typeStr = "numeric";
      }

      //
      // Obtain the size of the variable's data. If the
      // variable doesn't exist, an exception will be
      // thrown.
      //
      KernelVarDescriptor descr = getAttributes( name );

      //
      // If we didn't find a variable of the given name, throw
      // a "kernel variable not found" exception.
      //
      if ( !descr.exists() )
      {
         String msg = "The variable " + name +
                      "could not be found in the kernel pool.";

         KernelVarNotFoundException exc = new KernelVarNotFoundException(msg);

         throw ( exc );
      }

      //
      // If we found a variable of the given name, but the
      // type is not `dataType', throw a
      // "kernel variable not found" exception.
      //
      if (  dataType  !=  descr.getDataType()  )
      {
         String msg = "The variable " + name + " was found in the " +
                      "kernel pool, but the variable has " + typeStr + " type.";

         KernelVarNotFoundException exc = new KernelVarNotFoundException(msg);

         throw ( exc );
      }

      //
      // Return the size of the variable.
      //

      return ( descr.getSize() );
   }


   //
   // Public static methods
   //


   /**
   Get the attributes of a kernel variable specified by name.
   */
   public static KernelVarDescriptor getAttributes( String    name )

      throws SpiceException
   {
      KernelVarDescriptor descr;

      boolean[] foundArray  =  new boolean[1];
      int[]     sizeArray   =  new int[1];
      String[]  typeArray   =  new String[1];

      CSPICE.dtpool( name, foundArray, sizeArray, typeArray );

      if ( foundArray[0] )
      {
         descr =

         new KernelVarDescriptor ( foundArray[0],
                                   name,
                                   sizeArray[0],
                                   mapDataType( typeArray[0] )  );
      }
      else
      {
         descr =

         new KernelVarDescriptor ( false,
                                   name,
                                   0,
                                   0    );
      }

      return( descr );
   }





   /**
   Indicate whether a kernel variable of a given name and data type exists.

   <p> Note that this functionality differs from that of the CSPICE
   routine `expool_c', which is restricted to numeric kernel variables.
   */
   public static boolean exists ( String     name,
                                  int        dataType )

      throws SpiceException
   {
      KernelVarDescriptor          descr;

      descr = getAttributes( name );

      //
      // If there's no variable of the given name, we're done.
      //

      if ( !descr.exists() )
      {
         return( false );
      }


      //
      // If the variable is present but has the wrong type,
      // we're done.
      //

      if (  dataType  !=  descr.getDataType() )
      {
         return( false );
      }

      return( true );
   }



   /**
   Indicate whether a kernel variable of a given name exists.

   <p> Note that this functionality differs from that of the CSPICE
   routine `expool_c', which is restricted to numeric kernel variables.
   */
   public static boolean exists ( String  name )

      throws SpiceException
   {
      return(  getAttributes( name ).exists()  );
   }



   /**
   Get the data type of a specified kernel variable.
   */
   public static int getDataType( String  name )

      throws SpiceException, KernelVarNotFoundException

   {
      KernelVarDescriptor descr = getAttributes( name );

      if ( !descr.exists() )
      {
         String msg = "The variable " + name +
                      "could not be found in the kernel pool.";

         KernelVarNotFoundException exc = new KernelVarNotFoundException(msg);

         throw ( exc );
      }

      return(  descr.getDataType()  );
   }



   /**
   Get the size of a specified kernel variable.
   */
   public static int getSize( String  name )

      throws SpiceException, KernelVarNotFoundException

   {
      KernelVarDescriptor descr = getAttributes( name );

      if ( !descr.exists() )
      {
         String msg = "The variable " + name +
                      "could not be found in the kernel pool.";

         KernelVarNotFoundException exc = new KernelVarNotFoundException(msg);

         throw ( exc );
      }

      return(  descr.getSize()  );
   }





   /**
   Fetch a character kernel variable.

   <p> To obtain a slice of a character kernel variable,
   see the method {@link #getCharacter( String name, int start, int room )}.
   */
   public static String[] getCharacter ( String name )

      throws SpiceException, KernelVarNotFoundException
   {
      //
      // Make sure the variable exists and has the correct data type.
      // Obtain the variable's size as well.
      //
      int size = checkVar( name, CHARACTER );

      //
      // Fetch and return the variable.
      //
      return(   CSPICE.gcpool( name, 0,  size )  );
   }



   /**
   Fetch a slice (a contiguous sequence of array elements)
   of a character kernel variable.

   <p> To obtain an entire character kernel variable in one call.
   see the method {@link #getCharacter( String name )}.
   */
   public static String[] getCharacter ( String name,
                                         int    start,
                                         int    room   )

      throws SpiceException, KernelVarNotFoundException
   {
      //
      // Make sure the variable exists and has the correct data type.
      // Obtain the variable's size as well.
      //
      int size = checkVar( name, CHARACTER );

      //
      // Fetch and return the requested variable slice.
      //
      return(   CSPICE.gcpool( name, start, room )  );
   }



   /**
   Return a component of a string value from the kernel pool.

   <p> This method is analgous to the CSPICE routine stpool_c.

   <p>This method treats the string array associated with a character
   kernel variable as a sequence of components, where each component
   consists of a sequence of array elements connected by continuation
   characters.

   <p> It is permissible for the input index not to correspond to
   an existing component. The returned {@link KernelVarStringComponent}
   instance contains a "found flag" that indicates whether the
   component exists.

   <p> The indices of the components are zero-based.
   */
   public static KernelVarStringComponent getStringComponent ( String  name,
                                                               int     nth,
                                                               String  contin )

      throws SpiceException
   {
      String[]  value = new String[1];
      boolean[] found = new boolean[1];

      CSPICE.stpool( name, nth, contin, value, found );

      KernelVarStringComponent comp =

         new KernelVarStringComponent( name, nth, value[0], found[0] );

      return( comp );
   }


   /**
   Fetch a double precision kernel variable.

   <p> To obtain a slice of a double precision kernel variable,
   see the method {@link #getDouble( String name, int start, int room )}.
   */
   public static double[] getDouble ( String name )

      throws SpiceException, KernelVarNotFoundException
   {
      //
      // Make sure the variable exists and has the correct data type.
      // Obtain the variable's size as well.
      //
      int size = checkVar( name, NUMERIC );

      //
      // Fetch and return the variable.
      //
      return(   CSPICE.gdpool( name, 0,  size )  );
   }



   /**
   Fetch a slice (a contiguous sequence of array elements)
   of a double precision kernel variable.

   <p> To obtain an entire double precision kernel variable in one call.
   see the method {@link #getDouble( String name )}.
   */
   public static double[] getDouble ( String name,
                                      int    start,
                                      int    room   )

      throws SpiceException, KernelVarNotFoundException
   {
      //
      // Make sure the variable exists and has the correct data type.
      // Obtain the variable's size as well.
      //
      int size = checkVar( name, NUMERIC );

      //
      // Fetch and return the requested variable slice.
      //
      return(   CSPICE.gdpool( name, start, room )  );
   }




   /**
   Fetch an integer kernel variable.

   <p> To obtain a slice of an integer kernel variable,
   see the method {@link #getInteger( String name, int start, int room )}.
   */
   public static int[] getInteger ( String name )

      throws SpiceException, KernelVarNotFoundException
   {
      //
      // Make sure the variable exists and has the correct data type.
      // Obtain the variable's size as well.
      //
      int size = checkVar( name, NUMERIC );

      //
      // Fetch and return the variable.
      //
      return(   CSPICE.gipool( name, 0,  size )  );
   }



   /**
   Fetch a slice (a contiguous sequence of array elements)
   of an integer kernel variable.

   <p> To obtain an entire integer kernel variable in one call.
   see the method {@link #getInteger( String name )}.
   */
   public static int[] getInteger ( String name,
                                    int    start,
                                    int    room   )

      throws SpiceException, KernelVarNotFoundException
   {
      //
      // Make sure the variable exists and has the correct data type.
      // Obtain the variable's size as well.
      //
      int size = checkVar( name, NUMERIC );

      //
      // Fetch and return the requested variable slice.
      //
      return(   CSPICE.gipool( name, start, room )  );
   }


   /**
   Insert a character kernel variable into the kernel pool.
   */
   public static void putCharacter ( String    name,
                                             String[]  values )

      throws SpiceException
   {
      CSPICE.pcpool( name, values );
   }


   /**
   Insert a double precision kernel variable into the kernel pool.
   */
   public static void putDouble ( String    name,
                                  double[]  values )

      throws SpiceException
   {
      CSPICE.pdpool( name, values );
   }



   /**
   Insert an integer kernel variable into the kernel pool.
   */
   public static void putInteger ( String    name,
                                           int[]     values )

      throws SpiceException
   {
      CSPICE.pipool( name, values );
   }


   /**
   Load into the kernel pool variables defined by
   "keyword=value" assignments in a string array.
   */
   public static void loadFromBuffer ( String[] cvals )

      throws SpiceException
   {
      CSPICE.lmpool( cvals );
   }


   /**
   Load a text kernel into the kernel pool.

   <p> Caution: kernels loaded via this method are NOT loaded
   into the kernel database.
   */
   public static void load ( String    name )

      throws SpiceException
   {
      CSPICE.ldpool( name );
   }


   /**
   Clear the kernel pool.

   <p> Caution: this routine should not be used together with
   the kernel database.
   */
   public static void clear()

      throws SpiceException
   {
      CSPICE.clpool();
   }


   /**
   Return kernel pool size parameters specified by name.

   <pre>
   name       is the name of a kernel pool size parameter.
              The following parameters may be specified:

              MAXVAR      is the maximum number of variables that the
                          kernel pool may contain at any one time.
                          MAXVAR should be a prime number.

              MAXLEN      is the maximum length of the variable names
                          that can be stored in the kernel pool.

              MAXVAL      is the maximum number of distinct values that
                          may belong to the variables in the kernel
                          pool.  Each variable must have at least one
                          value, and may have any number, so long as
                          the total number does not exceed MAXVAL.
                          MAXVAL must be at least as large as MAXVAR.

              MXNOTE      is the maximum number of distinct
                          variable-agents pairs that can be maintained
                          by the kernel pool.  (A variable is "paired"
                          with an agent, if that agent is to be
                          notified whenever the variable is updated.)

              MAXAGT      is the maximum number of agents that can be
                          kept on the distribution list for
                          notification of updates to kernel variables.

              MAXCHR      is the maximum number of characters that can
                          be stored in a component of a string valued
                          kernel variable.

              MAXLIN      is the maximum number of character strings
                          that can be stored as data for kernel pool
                          variables.

              Note that the case of name is insignificant.  Embedded
              blanks are also ignored.
   </pre>

   */
   public static int getParameter( String name )

      throws SpiceException

   {
      return(  CSPICE.szpool( name )  );
   }


   /**
   Delete a variable from the kernel pool.
   */
   public static void delete( String   name )

      throws SpiceException

   {
      CSPICE.dvpool( name );
   }


   /**
   Retrieve an array containing all names of kernel variables
   that match a given template.

   <p>
   To obtain a slice of the array of variable names matching a
   template,
   see the method {@link #getNames( String template, int start, int room )}.
   */
   public static String[] getNames( String   template )

      throws SpiceException

   {
      int room = CSPICE.szpool( "MAXVAR" );

      return(  CSPICE.gnpool( template,  0,  room )  );
   }


   /**
   Retrieve a slice of an array containing all names of kernel variables
   that match a given template.

   <p>
   To obtain the full array of variable names matching a
   template, see the method {@link #getNames( String template)}.
   */
   public static String[] getNames( String   template,
                                    int      start,
                                    int      room      )

      throws SpiceException

   {
      return(  CSPICE.gnpool( template,  0,  room )  );
   }


   /**
   Associate an agent with a set of kernel variables to be watched.
   */
   public static void setWatch( String    agent,
                                String[]  names  )

      throws SpiceException

   {
      CSPICE.swpool( agent, names );
   }



   /**
   Determine whether any of the kernel variables associated with
   a specified agent have been updated.
   */
   public static boolean checkWatch( String   agent )

      throws SpiceException

   {
      return(  CSPICE.cvpool( agent )  );
   }


}
