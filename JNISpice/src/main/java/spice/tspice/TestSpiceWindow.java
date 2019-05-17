
package spice.tspice;


import java.util.*;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;


/**
Class TestSpiceWindow provides methods that implement test families for
the class SpiceWindow.

<p> 02-JAN-2010 (NJB)

*/
public class TestSpiceWindow extends Object
{

   /*
   Class variables
   */

   /*
   Local constants
   */
   final static double SMALL_TOL                = 1.e-12;
   final static double TOL                      = 1.e-09;

   final static int    DSIZE1                   = 10;
   final static int    DSIZE2                   = 30;
   final static int    DSIZE3                   = 2;
   final static int    NMAX                     = 5;


   /**
   Test family for methods of the class SpiceWindow.
   */

   public static boolean f_SpiceWindow()

      throws SpiceErrorException
   {


      /*
      Local variables
      */
      boolean                 ok;

      double [][]             dArrayExp;
      double                  dval;
      double []               endpoints;
      double [][]             expArray =
                              {
                                 { 1.0, 3.0 }, {7.0, 11.0}, {23.0, 27.0 }
                              };

      double [][]             expectedArray;
      double []               interval;
      double [][]             resultArray;
      double []               xArray;

      int                     count;
      int                     i;
      int                     j;
      int                     n;


      SpiceWindow             compSpiceWindow         = new SpiceWindow();
      SpiceWindow             contractSpiceWindow     = new SpiceWindow();
      SpiceWindow             expandSpiceWindow       = new SpiceWindow();
      SpiceWindow             expectedSpiceWindow     = new SpiceWindow();
      SpiceWindow             intersectSpiceWindow0   = new SpiceWindow();
      SpiceWindow             intersectSpiceWindow1   = new SpiceWindow();
      SpiceWindow             resultSpiceWindow       = new SpiceWindow();
      SpiceWindow             win0                    = new SpiceWindow();
      SpiceWindow             win1                    = new SpiceWindow();
      SpiceWindow             win2                    = new SpiceWindow();
      SpiceWindow             win3                    = new SpiceWindow();
      SpiceWindow             win4                    = new SpiceWindow();
      SpiceWindow             win5                    = new SpiceWindow();
      SpiceWindow             win6                    = new SpiceWindow();
      SpiceWindow             unionSpiceWindow0       = new SpiceWindow();
      SpiceWindow             unionSpiceWindow1       = new SpiceWindow();

      String                  label;
      String                  outStr;
      String                  shortMsg;
      String                  trace;
      String                  xStr;

      StringTokenizer         tokenizer;




      JNITestutils.topen ( "f_SpiceWindow" );


      //
      //  We enclose all tests in a try/catch block in order to
      //  facilitate handling unexpected exceptions.  Unexpected
      //  exceptions are trapped by the catch block at the end of
      //  the routine; expected exceptions are handled locally by
      //  catch blocks associated with error handling test cases.
      //
      //  Therefore, JNISpice calls that are expected to succeed don't
      //  have any subsequent "chckxc" type calls following them, nor
      //  are they wrapped in in try/catch blocks.
      //

      try
      {

         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "insert Error case: add " +
                              "[ 31, 30 ] to the SpiceWindow."      );

         try
         {
            win1.insert ( 31.0, 30.0 );

            /*
            If an exception is *not* thrown, we'll hit this call.
            */
            Testutils.dogDidNotBark ( "SPICE(BADENDPOINTS)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(BADENDPOINTS)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "getInterval error case: invalid " +
                              "interval index"                     );

         try
         {
            interval = win1.getInterval ( -1 );

            /*
            If an exception is *not* thrown, we'll hit this call.
            */
            Testutils.dogDidNotBark ( "SPICE(NOINTERVAL)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(NOINTERVAL)", ex );
         }






         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test default constructor, card, and " +
                              "getInterval." );


         win0 = new SpiceWindow();

         //
         // The cardinality of win0 should be 0.
         //
         ok = JNITestutils.chcksi ( "win0.card()",
                                     win0.card(), "=", 0, 0 );


         //
         // We should be able to insert data into this window.
         //

         win1 = win0.insert( 1.0, 2.0 );


         //
         // The cardinality of win1 should be 1.
         //
         ok = JNITestutils.chcksi ( "win1.card()",
                                     win1.card(), "=", 1, 0 );


         //
         // Check the values in win1.
         //
         interval = win1.getInterval(0);

         ok = JNITestutils.chcksd ( "left endpoint",
                                    interval[0], "=", 1.0, 0.0 );

         ok = JNITestutils.chcksd ( "right endpoint",
                                    interval[1], "=", 2.0, 0.0 );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test 1D array constructor and getInterval." );

         n = 20;

         endpoints = new double[n];

         for ( i = 0;  i < n;  i++ )
         {
            endpoints[i] = i;
         }

         win0 = new SpiceWindow( endpoints );

         //
         // The cardinality of win0 should be n/2.
         //
         ok = JNITestutils.chcksi ( "win0.card()",
                                     win0.card(), "=", n/2, 0 );


         //
         // Check the intervals in win0.
         //
         for ( i = 0;  i < win0.card();  i++ )
         {
            interval = win0.getInterval(i);

            ok = JNITestutils.chcksd ( "left endpoint " + i,
                                       interval[0],
                                       "=",
                                       2*i,
                                       0.0                  );

            ok = JNITestutils.chcksd ( "right endpoint " + i,
                                       interval[1],
                                       "=",
                                       2*i + 1,
                                       0.0                  );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test interval array constructor and " +
                              "getInterval." );

         n = 10;

         expectedArray = new double[n][2];

         for ( i = 0;  i < n;  i++ )
         {
            expectedArray[i][0] = 2*i;
            expectedArray[i][1] = 2*i + 1;
         }

         win0 = new SpiceWindow( expectedArray );

         //
         // The cardinality of win0 should be n.
         //
         ok = JNITestutils.chcksi ( "win0.card()",
                                     win0.card(), "=", n, 0 );


         //
         // Check the intervals in win0.
         //
         for ( i = 0;  i < win0.card();  i++ )
         {
            interval = win0.getInterval(i);

            ok = JNITestutils.chcksd ( "left endpoint " + i,
                                       interval[0],
                                       "=",
                                       2*i,
                                       0.0                  );

            ok = JNITestutils.chcksd ( "right endpoint " + i,
                                       interval[1],
                                       "=",
                                       2*i + 1,
                                       0.0                  );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test copy constructor and getInterval." );

         n = 20;

         endpoints = new double[n];

         for ( i = 0;  i < n;  i++ )
         {
            endpoints[i] = i;
         }

         win0 = new SpiceWindow( endpoints );
         win1 = new SpiceWindow( endpoints );

         win2 = new SpiceWindow( win0 );

         //
         // Change win0; make sure win2 doesn't change.
         //
         for ( i = 0;  i < n;  i++ )
         {
            endpoints[i] = i-100;
         }

         win0 = new SpiceWindow( endpoints );

         //
         // The cardinality of win2 should be n/2.
         //
         ok = JNITestutils.chcksi ( "win2.card()",
                                     win2.card(), "=", n/2, 0 );


         //
         // Check the intervals in win2. They should match those of win1.
         //
         for ( i = 0;  i < win2.card();  i++ )
         {
            ok = JNITestutils.chckad ( "win2 interval" + i,
                                       win2.getInterval(i),
                                       "=",
                                       win1.getInterval(i),
                                       0.0                  );
         }


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test toArray." );

         n = 20;

         endpoints = new double[n];

         for ( i = 0;  i < n;  i++ )
         {
            endpoints[i] = i;
         }

         win0 = new SpiceWindow( endpoints );

         ok = JNITestutils.chckad ( "win0 array",
                                    win0.toArray(),
                                    "=",
                                    endpoints,
                                    0.0                  );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test getMeasure." );



         n = 20;

         endpoints = new double[n];

         for ( i = 0;  i < n;  i++ )
         {
            endpoints[i] = i;
         }

         win0 = new SpiceWindow( endpoints );

         ok = JNITestutils.chcksd ( "win0 measure",
                                    win0.getMeasure(),
                                    "=",
                                    10.0,
                                    0.0                  );




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test fill." );



         n = 20;

         endpoints = new double[n];

         for ( i = 0;  i < n;  i++ )
         {
            endpoints[i] = i;
         }

         win0 = new SpiceWindow( endpoints );

         win1 = new SpiceWindow( win0 );

         //
         // Since all gaps have length 1, using a fill length of 0.99
         // should be a no-op.
         //
         win0.fill ( 0.9 );

         ok = JNITestutils.chckad ( "win0 array",
                                    win0.toArray(),
                                    "=",
                                    endpoints,
                                    0.0                  );

         //
         // This case should give us a single interval.
         //
         win2 = new SpiceWindow( win0 );

         win2.fill( 1.1 );

         xArray = new double[2];

         xArray[0] =  0.0;
         xArray[1] = 19.0;

         ok = JNITestutils.chckad ( "win2 array",
                                    win2.toArray(),
                                    "=",
                                    xArray,
                                    0.0                  );





         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test filter." );



         n = 20;

         endpoints = new double[n];

         for ( i = 0;  i < n;  i++ )
         {
            endpoints[i] = i;
         }

         win0 = new SpiceWindow( endpoints );

         win1 = new SpiceWindow( win0 );

         //
         // Since all gaps have length 1, using a filter length of 0.99
         // should be a no-op.
         //
         win0.filter ( 0.9 );

         ok = JNITestutils.chckad ( "win0 array",
                                    win0.toArray(),
                                    "=",
                                    endpoints,
                                    0.0                  );

         //
         // This case should give us an empty window
         //
         win2 = new SpiceWindow( win0 );

         win2.filter( 1.1 );


         //
         // The cardinality of win2 should be 0
         //
         ok = JNITestutils.chcksi ( "win2.card()",
                                     win2.card(), "=", 0, 0 );



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test sub." );

         //
         // expArray contains the intervals
         //
         //    [ 1.0, 3.0 ], [7.0, 11.0], [23.0, 27.0 ]
         //

         win0 = new SpiceWindow( expArray );

         win1 = new SpiceWindow();
         win1.insert( 1.0, 3.0 );

         win2 = new SpiceWindow();
         win2.insert(  7.0, 11.0 );
         win2.insert( 23.0, 27.0 );

         win3 = win0.sub( win1 );

         //
         // Subtracting win1 from win0 should yield win2.
         //
         ok = JNITestutils.chckad ( "win3 array",
                                    win3.toArray(),
                                    "=",
                                    win2.toArray(),
                                    0.0                  );

         //
         // Subtracting win2 from win0 should yield win1.
         //

         win4 = win0.sub( win2 );

         ok = JNITestutils.chckad ( "win4 array",
                                    win4.toArray(),
                                    "=",
                                    win1.toArray(),
                                    0.0                  );



         //
         //   insert, getInterval tests:
         //
         //   Test insert, getInterval first, as these will be used to
         //   build and examine SpiceWindows in the subsequent tests.
         //


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Test insert:  build the SpiceWindow " +
                              "[ 1, 3 ]  [ 7, 11 ]  [ 23, 27 ]"           );


         win1 = new SpiceWindow();

         for ( i = 0;  i < 3;  i++ )
         {
            win1.insert ( expArray[i][0], expArray[i][1] );
         }


         /*
         Check the SpiceWindow's interval count.
         */
         ok = JNITestutils.chcksi ( "win1.card()",
                                     win1.card(), "=", 3, 0 );

         /*
         Check the SpiceWindow's contents directly.
         */
         endpoints = win1.toArray();

         for ( i = 0;  i < 3;  i++ )
         {
            interval = new double[] { endpoints[2*i], endpoints[2*i + 1] };

            label    = "win1 interval[" + i + "]";

            ok       = JNITestutils.chckad( label,       interval, "=",
                                            expArray[i], 0.0            );
         }



         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Check the SpiceWindow's contents using " +
                              "getInterval."                         );

         for ( i = 0;  i < 3;  i++ )
         {
            interval = win1.getInterval(i);

            label    = "win1 interval[" + i + "]";

            JNITestutils.chckad ( label, interval, "=", expArray[i], 0.0 );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add [ 5,  5 ] to the SpiceWindow." );

         win1.insert ( 5.0, 5.0 );

         dArrayExp =  new double[4][2];

         dArrayExp[0][0] =  1.0;
         dArrayExp[0][1] =  3.0;

         dArrayExp[1][0] =  5.0;
         dArrayExp[1][1] =  5.0;

         dArrayExp[2][0] =  7.0;
         dArrayExp[2][1] = 11.0;

         dArrayExp[3][0] = 23.0;
         dArrayExp[3][1] = 27.0;


         for ( i = 0;  i < 4;  i++ )
         {
            interval = win1.getInterval(i);

            label    = "win1 interval[" + i + "]";

            JNITestutils.chckad ( label, interval, "=", dArrayExp[i], 0.0 );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add [ 4,  8 ] to the SpiceWindow." );

         win1.insert ( 4.0, 8.0 );

         dArrayExp       =  new double[3][2];

         dArrayExp[0][0] =  1.0;
         dArrayExp[0][1] =  3.0;

         dArrayExp[1][0] =  4.0;
         dArrayExp[1][1] = 11.0;

         dArrayExp[2][0] = 23.0;
         dArrayExp[2][1] = 27.0;


         for ( i = 0;  i < 3;  i++ )
         {
            interval = win1.getInterval(i);

            label    = "win1 interval[" + i + "]";

            JNITestutils.chckad ( label, interval, "=", dArrayExp[i], 0.0 );
         }

         /*
         Check the SpiceWindow's interval count.
         */
         ok = JNITestutils.chcksi ( "win1.card()",
                                     win1.card(), "=", 3, 0 );





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Add [ 0, 30 ] to the SpiceWindow." );

         win1.insert ( 0.0, 30.0 );


         dArrayExp       =  new double[1][2];

         dArrayExp[0][0] =   0.0;
         dArrayExp[0][1] =  30.0;

         interval = win1.getInterval(0);

         label    = "win1 interval[" + 0 + "]";

         JNITestutils.chckad ( "win1", interval, "=", dArrayExp[0], 0.0 );


         /*
         Check the SpiceWindow's interval count.
         */
         ok = JNITestutils.chcksi ( "win1.card()",
                                     win1.card(), "=", 1, 0 );





         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "Create a large (10000 intervals) " +
                              "SpiceWindow."                            );

         win2 = new SpiceWindow();

         int nmax = 10000;

         double[] expIvalArray = new double[2 * nmax];

         for ( i = 0;  i < nmax;  i += 2 )
         {
            expIvalArray[i  ] =  (double)  i;
            expIvalArray[i+1] =  (double)  i+0.5;

            win2.insert ( expIvalArray[i], expIvalArray[i+1] );
         }

         double[] bigArray = win2.toArray();

         JNITestutils.chckad ( "bigArray", bigArray, "=", expIvalArray, 0.0 );



         //
         // --------Case-----------------------------------------------
         //

         //
         //   complement tests:
         //
         JNITestutils.tcase ( "complement normal case #1." );

         double[][] dCompArray = new double[3][2];

         dCompArray[0][0] =  1.0;
         dCompArray[0][1] =  3.0;

         dCompArray[1][0] =  7.0;
         dCompArray[1][1] = 11.0;

         dCompArray[2][0] = 23.0;
         dCompArray[2][1] = 27.0;


         compSpiceWindow = new SpiceWindow ( dCompArray );

         /*
         Set up the expected result window.
         */
         expectedArray = new double[2][2];

         expectedArray[0][0] =  3.0;
         expectedArray[0][1] =  7.0;

         expectedArray[1][0] = 11.0;
         expectedArray[1][1] = 20.0;

         expectedSpiceWindow = new SpiceWindow ( expectedArray );


         /*
         Do the complement.
         */
         resultSpiceWindow = compSpiceWindow.complement ( 2.0, 20.0 );


         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    =  JNITestutils.chcksi ( "result interval count", count,
                                        "=", 2, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            JNITestutils.chckad ( label,
                                  interval, "=", expectedArray[i], 0.0 );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "complement normal case #2." );

         //
         //  Use the copy constructor to create the SpiceWindow to
         //  be complemented.
         //
         SpiceWindow compSpiceWindow2 = new SpiceWindow ( compSpiceWindow );



         /*
         Set up the expected result window.
         */
         expectedArray = new double[4][2];

         expectedArray[0][0] =  0.0;
         expectedArray[0][1] =  1.0;

         expectedArray[1][0] =  3.0;
         expectedArray[1][1] =  7.0;

         expectedArray[2][0] = 11.0;
         expectedArray[2][1] = 23.0;

         expectedArray[3][0] = 27.0;
         expectedArray[3][1] = 100.0;


         /*
         Do the complement.
         */
         resultSpiceWindow = compSpiceWindow2.complement ( 0.0, 100.0 );

         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    = JNITestutils.chcksi ( "result interval count", count,
                                       "=", 4, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            JNITestutils.chckad ( label,
                                  interval, "=", expectedArray[i], 0.0 );
         }




         //
         // --------Case-----------------------------------------------
         //


         //
         //   contract tests:
         //
         JNITestutils.tcase ( "contract normal test #1" );

         //
         //   Create SpiceWindow to be contracted.
         //
         double[][] dContractArray = new double[4][2];

         dContractArray[0][0] =  1.0;
         dContractArray[0][1] =  3.0;

         dContractArray[1][0] =  7.0;
         dContractArray[1][1] = 11.0;

         dContractArray[2][0] = 23.0;
         dContractArray[2][1] = 27.0;

         dContractArray[3][0] = 29.0;
         dContractArray[3][1] = 29.0;

         contractSpiceWindow = new SpiceWindow ( dContractArray );



         /*
         Set up the expected result window.
         */
         expectedArray = new double[2][2];

         expectedArray[0][0] =  9.0;
         expectedArray[0][1] = 10.0;

         expectedArray[1][0] = 25.0;
         expectedArray[1][1] = 26.0;

         expectedSpiceWindow = new SpiceWindow ( expectedArray );


         /*
         Do the contraction.
         */
         contractSpiceWindow.contract ( 2.0, 1.0 );
         resultSpiceWindow = contractSpiceWindow;


         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    =  JNITestutils.chcksi ( "result interval count", count,
                                        "=", 2, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            ok       = JNITestutils.chckad ( label,
                                             interval,
                                             "=",
                                             expectedArray[i],
                                             0.0               );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "contract normal test #2" );

         /*
         Set up the expected result window.
         */
         expectedArray = new double[2][2];

         expectedArray[0][0] =  7.0;
         expectedArray[0][1] =  8.0;

         expectedArray[1][0] = 23.0;
         expectedArray[1][1] = 24.0;

         expectedSpiceWindow = new SpiceWindow ( expectedArray );

         /*
         Use the result of the previous test case as the input
         window.
         */
         contractSpiceWindow = new SpiceWindow ( resultSpiceWindow );

         /*
         Do the contraction.
         */
         contractSpiceWindow.contract ( -2.0, 2.0 );
         resultSpiceWindow = contractSpiceWindow;


         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    =  JNITestutils.chcksi ( "result interval count", count,
                                        "=", 2, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            ok       = JNITestutils.chckad ( label,
                                             interval,
                                             "=",
                                             expectedArray[i],
                                             0.0               );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "contract normal test #3" );

         /*
         Set up the expected result window.
         */
         expectedArray = new double[2][2];

         expectedArray[0][0] =  5.0;
         expectedArray[0][1] =  9.0;

         expectedArray[1][0] = 21.0;
         expectedArray[1][1] = 25.0;

         expectedSpiceWindow = new SpiceWindow ( expectedArray );

         /*
         Use the result of the previous test case as the input
         window.
         */
         contractSpiceWindow = new SpiceWindow ( resultSpiceWindow );

         /*
         Do the contraction.
         */
         contractSpiceWindow.contract ( -2.0, -1.0 );
         resultSpiceWindow = contractSpiceWindow;


         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    =  JNITestutils.chcksi ( "result interval count", count,
                                        "=", 2, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            ok       = JNITestutils.chckad ( label,
                                             interval,
                                             "=",
                                             expectedArray[i],
                                             0.0               );
         }



         //
         //   expand tests:
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "expand normal test #1" );

         //
         //   Create SpiceWindow to be expanded.
         //
         double[][] dExpandArray = new double[4][2];

         dExpandArray[0][0] =  1.0;
         dExpandArray[0][1] =  3.0;

         dExpandArray[1][0] =  7.0;
         dExpandArray[1][1] = 11.0;

         dExpandArray[2][0] = 23.0;
         dExpandArray[2][1] = 27.0;

         dExpandArray[3][0] = 29.0;
         dExpandArray[3][1] = 29.0;

         expandSpiceWindow = new SpiceWindow ( dExpandArray );



         /*
         Set up the expected result window.
         */
         expectedArray = new double[3][2];

         expectedArray[0][0] =  -1.0;
         expectedArray[0][1] =   4.0;

         expectedArray[1][0] =   5.0;
         expectedArray[1][1] =  12.0;

         expectedArray[2][0] =  21.0;
         expectedArray[2][1] =  30.0;

         expectedSpiceWindow = new SpiceWindow ( expectedArray );


         /*
         Do the expansion.
         */
         expandSpiceWindow.expand ( 2.0, 1.0 );
         resultSpiceWindow = expandSpiceWindow;


         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    =  JNITestutils.chcksi ( "result interval count", count,
                                        "=", 3, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            ok       = JNITestutils.chckad ( label,
                                             interval,
                                             "=",
                                             expectedArray[i],
                                             0.0               );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "expand normal test #2" );

         /*
         Set up the expected result window.
         */
         expectedArray = new double[3][2];

         expectedArray[0][0] =   1.0;
         expectedArray[0][1] =   6.0;

         expectedArray[1][0] =   7.0;
         expectedArray[1][1] =  14.0;

         expectedArray[2][0] =  23.0;
         expectedArray[2][1] =  32.0;

         expectedSpiceWindow = new SpiceWindow ( expectedArray );

         /*
         Use the result of the previous test case as the input
         window.
         */
         expandSpiceWindow = new SpiceWindow ( resultSpiceWindow );

         /*
         Do the expansion.
         */
         expandSpiceWindow.expand ( -2.0, 2.0 );
         resultSpiceWindow = expandSpiceWindow;


         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    =  JNITestutils.chcksi ( "result interval count", count,
                                        "=", 3, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            ok       = JNITestutils.chckad ( label,
                                             interval,
                                             "=",
                                             expectedArray[i],
                                             0.0               );
         }




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "expand normal test #3" );

         /*
         Set up the expected result window.
         */
         expectedArray = new double[3][2];

         expectedArray[0][0] =   3.0;
         expectedArray[0][1] =   5.0;

         expectedArray[1][0] =   9.0;
         expectedArray[1][1] =  13.0;

         expectedArray[2][0] =  25.0;
         expectedArray[2][1] =  31.0;


         expectedSpiceWindow = new SpiceWindow ( expectedArray );

         /*
         Use the result of the previous test case as the input
         window.
         */
         expandSpiceWindow = new SpiceWindow ( resultSpiceWindow );

         /*
         Do the expansion.
         */
         expandSpiceWindow.expand ( -2.0, -1.0 );
         resultSpiceWindow = expandSpiceWindow;


         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    =  JNITestutils.chcksi ( "result interval count", count,
                                        "=", 3, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            ok       = JNITestutils.chckad ( label,
                                             interval,
                                             "=",
                                             expectedArray[i],
                                             0.0               );
         }




         //
         // --------Case-----------------------------------------------
         //


         //
         //   union tests:
         //
         JNITestutils.tcase ( "union normal test #1" );

         //
         //   Create first SpiceWindow.
         //
         double[][] dUnionArray0 = new double[3][2];

         dUnionArray0[0][0] =  1.0;
         dUnionArray0[0][1] =  3.0;

         dUnionArray0[1][0] =  7.0;
         dUnionArray0[1][1] = 11.0;

         dUnionArray0[2][0] = 23.0;
         dUnionArray0[2][1] = 27.0;


         unionSpiceWindow0 = new SpiceWindow ( dUnionArray0 );


         //
         //   Create second SpiceWindow.
         //
         double[][] dUnionArray1 = new double[3][2];

         dUnionArray1[0][0] =  2.0;
         dUnionArray1[0][1] =  6.0;

         dUnionArray1[1][0] =  8.0;
         dUnionArray1[1][1] = 10.0;

         dUnionArray1[2][0] = 16.0;
         dUnionArray1[2][1] = 18.0;


         unionSpiceWindow1 = new SpiceWindow ( dUnionArray1 );


         /*
         Set up the expected result window.
         */
         expectedArray = new double[4][2];

         expectedArray[0][0] =  1.0;
         expectedArray[0][1] =  6.0;

         expectedArray[1][0] =  7.0;
         expectedArray[1][1] = 11.0;

         expectedArray[2][0] = 16.0;
         expectedArray[2][1] = 18.0;

         expectedArray[3][0] = 23.0;
         expectedArray[3][1] = 27.0;

         expectedSpiceWindow = new SpiceWindow ( expectedArray );


         /*
         Do the union.
         */
         resultSpiceWindow = unionSpiceWindow0.union ( unionSpiceWindow1 );


         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    =  JNITestutils.chcksi ( "result interval count", count,
                                        "=", 4, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            ok       = JNITestutils.chckad ( label,
                                             interval,
                                             "=",
                                             expectedArray[i],
                                             0.0               );
         }



         //
         //   intersect tests:
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "intersect normal test #1" );

         //
         //   Create first SpiceWindow.
         //
         double[][] dIntersectArray0 = new double[3][2];

         dIntersectArray0[0][0] =  1.0;
         dIntersectArray0[0][1] =  3.0;

         dIntersectArray0[1][0] =  7.0;
         dIntersectArray0[1][1] = 11.0;

         dIntersectArray0[2][0] = 23.0;
         dIntersectArray0[2][1] = 27.0;


         intersectSpiceWindow0 = new SpiceWindow ( dIntersectArray0 );


         //
         //   Create second SpiceWindow.
         //
         double[][] dIntersectArray1 = new double[3][2];

         dIntersectArray1[0][0] =  2.0;
         dIntersectArray1[0][1] =  4.0;

         dIntersectArray1[1][0] =  8.0;
         dIntersectArray1[1][1] = 10.0;

         dIntersectArray1[2][0] = 16.0;
         dIntersectArray1[2][1] = 18.0;


         intersectSpiceWindow1 = new SpiceWindow ( dIntersectArray1 );


         /*
         Set up the expected result window.
         */
         expectedArray = new double[2][2];

         expectedArray[0][0] =  2.0;
         expectedArray[0][1] =  3.0;

         expectedArray[1][0] =  8.0;
         expectedArray[1][1] = 10.0;

         expectedSpiceWindow = new SpiceWindow ( expectedArray );


         /*
         Do the intersection.
         */
         resultSpiceWindow = intersectSpiceWindow0.intersect (
                                                       intersectSpiceWindow1 );


         /*
         Check the result window's interval count.
         */
         count = resultSpiceWindow.card();

         ok    =  JNITestutils.chcksi ( "result interval count", count,
                                        "=", 2, 0                       );
         /*
         Check the result.
         */
         for ( i = 0;  i < count;  i++ )
         {
            interval = resultSpiceWindow.getInterval( i );

            label    = "result interval[" + i + "]";

            JNITestutils.chckad ( label,
                                  interval, "=", expectedArray[i], 0.0 );
         }




         //
         // toString tests
         //

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "toString test: window with three intervals" );


         win0   = new SpiceWindow();

         win0.insert( -4.e-200, -3.e-200 );
         win0.insert( -2.e-200, -1.e-200 );

         outStr = win0.toString();

         xStr   = String.format( "%n%s%n%s%n%n",
                     "[-4.0000000000000000e-200, -3.0000000000000000e-200]",
                     "[-2.0000000000000000e-200, -1.0000000000000000e-200]"  );

         JNITestutils.chcksc ( "outStr", outStr, "=", xStr );



         //
         // For debugging:
         //
         //System.out.println( xStr   );
         //System.out.println( outStr );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "toString test: empty window" );


         win0   = new SpiceWindow();

         outStr = win0.toString();

         xStr   = String.format( "%n<empty>%n"  );

         JNITestutils.chcksc ( "outStr", outStr, "=", xStr );



         //
         // For debugging:
         //
         //System.out.println( ">>>" + xStr + "<<<"   );
         //System.out.println( outStr );


      }

      catch ( SpiceException ex )
      {
         //
         //  Getting here means we've encountered an unexpected
         //  SPICE exception.  This is analogous to encountering
         //  an unexpected SPICE error in CSPICE.
         //

         ok = JNITestutils.chckth ( false, "", ex );
      }

      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }


} /* End f_SpiceWindow */

