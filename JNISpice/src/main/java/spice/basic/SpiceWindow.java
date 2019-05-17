

package spice.basic;

import java.util.Formatter;



/**
Class SpiceWindow represents unions of disjoint intervals on the real
line.  SpiceWindows are frequently used to represent time periods, for
example periods when a specified geometric condition occurs.

<p>SpiceWindows are used in JNISpice where the abstract data type Window 
is used in SPICELIB, or where the SpiceCell structure is used to
represent windows in CSPICE.

<p>Each SpiceWindow instance contains a one-dimensional array of interval
endpoints; the endpoints are arranged in increasing order. 
Unlike windows in SPICELIB and CSPICE, SpiceWindow objects don't
have separate cardinality and size values: all of the elements of
the array of endpoints are considered to be valid data.


<h3> Version 1.0.1 20-DEC-2016 (NJB)</h3>

Updated class description and constructor documentation.

<h3> Version 1.0.0 02-JAN-2010 (NJB)</h3>
*/
public class SpiceWindow extends Object
{

   /*
   Class variables
   */


   /*
   Instance variables
   */
   double[]                     endpoints;


   /*
   Constructors
   */


   /**
   Default constructor.
   */
   public SpiceWindow()
   {
      /*
      Create an array with initial capacity zero.
      */
      endpoints   =  new double[0];
   }

   /**
   Copy constructor. This creates a deep copy.
   */
   public SpiceWindow( SpiceWindow s )
   {
      /*
      Create a SpiceWindow from another.
      */
      int card       =  s.endpoints.length;
      this.endpoints =  new double[card];

      System.arraycopy ( s.endpoints,    0,
                         this.endpoints, 0,  card );
   }



   /**
   Construct a SpiceWindow from a one-dimensional array of double
   precision endpoints.

   <p>All of the elements of the array are considered to be valid data.
   The cardinality of the SpiceWindow is one half of the array length.
   */
   public SpiceWindow ( double[]   endpoints )

      throws SpiceErrorException
   {

      /*
      We use the CSPICE window validation capability to create a
      valid SpiceWindow.
      */
      int card       = endpoints.length;
      this.endpoints = new double[card];

      System.arraycopy ( endpoints, 0, this.endpoints, 0, card );

      this.endpoints = CSPICE.wnvald ( card, card, this.endpoints );
   }



   /**
   Construct a SpiceWindow from an array of double
   precision endpoint intervals.

   <p>All of the elements of the array are considered to be valid data.
   The cardinality of the SpiceWindow is the length of the array
   `intervals'.
   */
   public SpiceWindow ( double[][]  intervals )

      throws SpiceErrorException
   {
      /*
      We use the CSPICE window validation capability to create a
      valid SpiceWindow.
      */
      int n          = intervals.length;
      int size       = 2 * n;
      endpoints      = new double[ size ];

      for ( int i = 0;  i < n;  i++  )
      {
         endpoints[2*i    ] = intervals[i][0];
         endpoints[2*i + 1] = intervals[i][1];
      }

      //System.out.println ( "Constructor (using array of intervals):" );
      this.endpoints = CSPICE.wnvald ( size, size, endpoints );
   }





   /*
   Instance methods
   */


   /**
   Get the cardinality (number of intervals) of a SpiceWindow.
   */
   public int card()
   {
      return ( endpoints.length/2 );
   }


   /**
   Get an array of endpoints from a SpiceWindow.
   */
   public double[] toArray()
   {
      return ( endpoints );
   }


   /**
   Fetch an interval, specified by index, from a SpiceWindow.
   */
   public double[] getInterval( int index )

      throws SpiceErrorException
   {
      SpiceErrorException exc        = null;
      int                 nIntervals = this.card();

      if ( nIntervals == 0 )
      {
          exc = SpiceErrorException.create(

            "SpiceWindow.getInterval",

            "SPICE(NOINTERVAL)",

            "Spice window is empty. Interval " +
            index + " does not exist."           );

         throw ( exc );

      }
      else if (  ( index < 0 ) || ( index >= nIntervals )  )
      {
         exc = SpiceErrorException.create(

            "SpiceWindow.getInterval",

            "SPICE(NOINTERVAL)",

            "Interval index out of range. " +
            "Index must be in range 0:"     +
            (nIntervals-1)                  +
            ".  Actual index was "          +
            index                              );

         throw ( exc );
      }


      int i              =  index * 2;
      double[] interval  =  new double[2];

      interval[0] = endpoints[i  ];
      interval[1] = endpoints[i+1];

      return ( interval );
   }



   /**
   Compute the union of two SpiceWindows.
   */
   public SpiceWindow union ( SpiceWindow b )

      throws SpiceErrorException
   {
      double[] resultArray = CSPICE.wnunid ( this.endpoints, b.endpoints );

      return (  new SpiceWindow ( resultArray )  );
   }




   /**
   Compute the intersection of two SpiceWindows.
   */
   public SpiceWindow intersect ( SpiceWindow b )

      throws SpiceErrorException
   {
      double[] resultArray = CSPICE.wnintd ( this.endpoints, b.endpoints );

      return (  new SpiceWindow ( resultArray )  );
   }




   /**
   Compute the difference of two SpiceWindows:  subtract a specified
   SpiceWindow from another.
   */
   public SpiceWindow sub ( SpiceWindow b )

      throws SpiceErrorException
   {
      double[] resultArray = CSPICE.wndifd ( endpoints, b.endpoints );

      return (  new SpiceWindow ( resultArray )  );
   }



   /**
   Insert an interval into a SpiceWindow.

   <p>Unlike windows in SPICELIB and CSPICE, SpiceWindows don't need
   to contain empty space to accommodate inserted intervals; in fact
   they don't contain empty space at all. The data array of a SpiceWindow 
   is re-allocated as necessary to accommodate insertions. Thus the 
   following code fragment is valid:
   <pre>
   SpiceWindow w = new SpiceWindow();

   w.insert( 1.0, 2.0 );
   </pre>

   <p> This method updates the SpiceWindow's endpoint array
   and returns a reference to the window.
   */
   public SpiceWindow insert ( double left,
                               double right )

      throws SpiceErrorException
   {
      /*
      Update this SpiceWindow.
      */
      endpoints = CSPICE.wninsd ( left, right, endpoints );

      return ( this );
   }



   /**
   Complement a SpiceWindow with respect to a specified interval.
   */
   public SpiceWindow complement ( double left,
                                   double right )

      throws SpiceErrorException
   {
      /*
      Create the complement of this schedule.
      */
      double[] newEndpoints = CSPICE.wncomd ( left, right, endpoints );

      return ( new SpiceWindow(newEndpoints) );
   }



   /**
   Contract a SpiceWindow using specified inset values.
   */
   public void contract ( double left,
                          double right )

      throws SpiceErrorException
   {
      /*
      Contract this SpiceWindow.
      */
      this.endpoints = CSPICE.wncond ( left, right, endpoints );
   }



   /**
   Expand a SpiceWindow using specified inset values.
   */
   public void expand ( double left,
                        double right )

      throws SpiceErrorException
   {
      /*
      Expand this SpiceWindow.
      */
      this.endpoints = CSPICE.wnexpd ( left, right, endpoints );
   }


   /**
   Fill in gaps shorter than a specified in from a SpiceWindow.
   */
   public void fill ( double small )

      throws SpiceErrorException
   {
      /*
      Fill in this SpiceWindow.
      */
      this.endpoints = CSPICE.wnfild ( small, endpoints );
   }


   /**
   Filter intervals shorter than a specified length from a SpiceWindow.
   */
   public void filter ( double small )

      throws SpiceErrorException
   {
      /*
      Filter this SpiceWindow.
      */
      this.endpoints = CSPICE.wnfltd ( small, endpoints );
   }





   /**
   Get the measure of this SpiceWindow.
   */
   public double getMeasure()
   {
      double measure = 0.0;

      for ( int i = 0; i < endpoints.length;  i += 2 )
      {
         measure += ( endpoints[i+1] - endpoints[i] );
      }

      return ( measure );
   }


   /**
   Override Object's toString method.
   */
   public String toString()
   {
      String outStr;

      if ( this.card() > 0 )
      {
         try
         {
            StringBuilder sb = new StringBuilder();
            Formatter     fm = new Formatter( sb );


            double[]      iv         = new double[2];
            int           nIntervals = this.card();

            fm.format( "%n" );
            outStr = sb.toString();


            for ( int i = 0; i < nIntervals;  i++ )
            {
               iv = this.getInterval(i);


               fm.format ( "[%24.16e, %24.16e]%n", iv[0], iv[1] );
            }

            if ( nIntervals > 0 )
            {
               fm.format( "%n" );
            }

            outStr = sb.toString();

         }
         catch ( Exception exc )
         {
            outStr = exc.getMessage();
         }
      }
      else
      {
         outStr = String.format( "%n<empty>%n" );
      }


      return ( outStr );

   }

}
