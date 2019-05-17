
package spice.basic;

/**
Class GFDefaultSearchUtils is the default
GF search utility class that provides user-defined
GF search step, convergence tolerance, refinement, progress
reporting, and interrupt handling functions.

<p> Normally SPICE users wishing to customize GF search
functionality should subclass this class, overriding
any methods for which custom functionality is sought.
Note that certain subsets of methods of this class
work cooperatively, so all methods of such a subset
must be overridden if any one of them is overridden.
For example, all of the progress reporting methods
must be overridden together if custom progress
reporting is desired.

<p> Note that the calling application must set the
search step size via a call to {@link #setSearchStep}
prior to starting a GF search that uses these utilities.

<p> Version 1.0.0 30-DEC-2009 (NJB)
*/

public class GFSearchUtils extends Object
{
   //
   // Fields
   //
   private boolean                reportingEnabled = false;
   private boolean                interruptEnabled = false;
   private double                 tolerance        = 1.e-6;

   /**
   No-arguments constructor.
   */
   public GFSearchUtils() {};


   //
   // Methods
   //

   /**
   Set the search step size.
   */
   public void setSearchStep( double step )

      throws SpiceException
   {
      CSPICE.gfsstp( step );
   }


   /**
   Return the search step size last set by {@link #setSearchStep}.
   */
   public double getSearchStep( double et )

      throws SpiceException
   {
      return(   CSPICE.gfstep( et )   );
   }


   /**
   Get a refined root estimate.

   The inputs are, respectively, the endpoints of an interval
   that brackets a root, and the binary states corresponding
   to the those endpoints.
   */
   public double getRefinement( double   t1,
                                double   t2,
                                boolean  s2,
                                boolean  s1  )
      throws SpiceException
   {
      double t = CSPICE.gfrefn( t1, t2, s1, s2 );

      return( t );
   }


   /**
   Enable or disable progress reporting. The boolean
   input argument should be set to `true' to enable
   progress reporting and to `false' otherwise.
   */
   public void setReportingEnabled ( boolean isEnabled )
   {
      reportingEnabled = isEnabled;
   }

   /**
   Determine whether progress reporting is enabled.
   */
   public boolean isReportingEnabled()
   {
      return ( reportingEnabled );
   }


   /**
   Enable or disable interrupt handling. The boolean
   input argument should be set to `true' to enable
   interrupt handling and to `false' otherwise.
   */
   public void setInterruptHandlingEnabled ( boolean isEnabled )
   {
      interruptEnabled = isEnabled;
   }

   /**
   Determine whether interrupt handling is enabled.
   */
   public boolean isInterruptHandlingEnabled()
   {
      return ( interruptEnabled );
   }


   /**
   Indicate whether an interrupt occurred.

   <p> This is currently a no-op function which must be overridden
   by the user if GF interrupt detection is desired.
   */
   public boolean interruptOccurred()

      throws SpiceException
   {
      boolean retval;

      retval = CSPICE.gfbail();

      //System.out.println ( "gfbail returned: " + retval );

      return ( retval );
   }


   /**
   Clear interrupt status.

   <p> This is currently a no-op function which must be overridden
   by the user if GF interrupt detection is desired.
   */
   public void clearInterruptStatus()

      throws SpiceException
   {
      CSPICE.gfclrh();
   }



   /**
   Set the convergence tolerance. Units are TDB seconds.
   */
   public void setTolerance( double tol )
   {
      tolerance = tol;
   }


   /**
   Get the convergence tolerance.
   */
   public double getTolerance()
   {
      return ( tolerance );
   }


   /**
   Initialize a GF progress report.
   */
   public void initializeReport( SpiceWindow   confine,
                                 String        begmsg,
                                 String        endmsg  )


   /*
   public void initializeReport( double[]      confine,
                                 String        begmsg,
                                 String        endmsg  )
   */
      throws SpiceException
   {

      CSPICE.gfrepi ( confine.toArray(),
                      begmsg,
                      endmsg             );
   }


   /**
   Update a GF progress report.
   */
   public void updateReport( double       ivbeg,
                             double       ivend,
                             double       t      )
      throws SpiceException
   {
      CSPICE.gfrepu ( ivbeg,
                      ivend,
                      t      );
   }

   /**
   Finalize a GF progress report.
   */
   public void finalizeReport()

      throws SpiceException
   {
      CSPICE.gfrepf();
   }


}
