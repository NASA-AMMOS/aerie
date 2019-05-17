

package spice.basic;

/**
Class GFConstraint represents relational constraints
applicable to GF numeric quantity searches.

<p> Version 1.0.0 21-DEC-2009 (NJB)
*/
public class GFConstraint
{

   //
   // Public constants
   //
   public final static String ABSOLUTE_MAXIMUM = "ABSMAX";
   public final static String ABSOLUTE_MINIMUM = "ABSMIN";
   public final static String ADJUSTED_ABSMAX  = "ADJ_ABSMAX";
   public final static String ADJUSTED_ABSMIN  = "ADJ_ABSMIN";
   public final static String EQUALS           = "=";
   public final static String GREATER_THAN     = ">";
   public final static String LESS_THAN        = "<";
   public final static String LOCAL_MAXIMUM    = "LOCMAX";
   public final static String LOCAL_MINIMUM    = "LOCMIN";


   //
   // Fields
   //

   private String               relation;
   private double               referenceValue;
   private double               adjustmentValue;


   //
   // Private Constructors
   //
   private GFConstraint( String        relation,
                         double        referenceValue,
                         double        adjustmentValue )
   {

      this.relation        =  relation;
      this.referenceValue  =  referenceValue;
      this.adjustmentValue =  adjustmentValue;
   }


   //
   // Public static factory methods
   //

   /**
   Create a GF local or absolute extremum constraint. This
   constructor is applicable to constraints using local or unadjusted
   absolute extrema relations.
   */
   public static GFConstraint createExtremumConstraint( String extremumType )

      throws SpiceException

   {
      String relString = extremumType.trim().toUpperCase();

      if (    ( !relString.equals(ABSOLUTE_MINIMUM) )
           && ( !relString.equals(ABSOLUTE_MAXIMUM) )
           && ( !relString.equals(LOCAL_MINIMUM)    )
           && ( !relString.equals(LOCAL_MAXIMUM)    )  )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "GFConstraint.createExtremumConstraint",

            "SPICE(NOTAPPLICABLE)",

            "Extremum type must be absolute or local extremum " +
            "but was " + extremumType                              );

         throw ( exc );
      }

      GFConstraint GFCons = new GFConstraint ( relString, 0.0, 0.0 );

      return ( GFCons );
   }


   /**
   Create a GF absolute extremum constraint with an adjustment value.
   */
   public static GFConstraint createExtremumConstraint ( String extremumType,
                                                       double adjustmentValue )
      throws SpiceException

   {
      String relString = extremumType.trim().toUpperCase();

      if (    ( !relString.equals(ADJUSTED_ABSMAX) )
           && ( !relString.equals(ADJUSTED_ABSMIN) )  )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "GFConstraint.createExtremumConstraint",

            "SPICE(NOTAPPLICABLE)",

            "This constructor is applicable only for " +
            "adjusted absolute extrema; " +
            "extremum type was " + extremumType                     );

         throw ( exc );
      }


      if (  adjustmentValue <= 0.0  )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "GFConstraint.createExtremumConstraint",

            "SPICE(VALUEOUTOFRANGE)",

            "This constructor supportes only " +
            "positive adjustment values. The supplied " +
            "value was " + adjustmentValue                  );

         throw ( exc );
      }


      GFConstraint GFCons = new GFConstraint ( relString, 0.0,
                                               adjustmentValue );

      return ( GFCons );
   }




   /**
   Create a GF relational constraint with a reference value.
   */
   public static GFConstraint createReferenceConstraint ( String relation,
                                                        double referenceValue )
      throws SpiceException

   {
      String relString = relation.trim().toUpperCase();

      if (    ( !relString.equals(GREATER_THAN) )
           && ( !relString.equals(LESS_THAN   ) )
           && ( !relString.equals(EQUALS      ) )  )
      {
         SpiceErrorException exc = SpiceErrorException.create(

            "GFConstraint.createReferenceConstraint",

            "SPICE(NOTAPPLICABLE)",

            "This constructor is applicable only for " +
            "order relations { <, =, > }; " +
            "relation was " + relation                    );

         throw ( exc );
      }


      GFConstraint GFCons = new GFConstraint ( relString, referenceValue, 0.0 );

      return ( GFCons );
   }





   //
   // Methods
   //

   /**
   Get the relation from this constraint.
   */
   public String getRelation()
   {
      return ( new String(relation) );
   }

   /**
   Get the reference value from this constraint.
   */
   public double getReferenceValue()
   {
      return ( referenceValue );
   }

   /**
   Get the adjustment value from this constraint.
   */
   public double getAdjustmentValue()
   {
      return ( adjustmentValue );
   }

   /**
   Get the CSPICE String representing the relation.
   */
   public String getCSPICERelation()
   {
      String result;

      if ( relation.equals(ADJUSTED_ABSMAX) )
      {
         result = new String( "ABSMAX" );
      }
      else if ( relation.equals(ADJUSTED_ABSMIN) )
      {
         result = new String( "ABSMIN" );
      }
      else
      {
         result = new String ( relation );
      }


      return ( result );
   }

   /**
   Indicate whether this constraint is an order constraint:
   that is, an equality or inequality.
   */
   public boolean isOrder()
   {
      if (     ( relation.equals( ">" ) )
           ||  ( relation.equals( "=" ) )
           ||  ( relation.equals( "<" ) )  )
      {
         return true;
      }

      return false;
   }


   /**
   Indicate whether this constraint is an
   extremum constraint.
   */
   public boolean isExtremum()
   {
      //
      // Note that the relation is stored in upper case, trimmed form.
      //
      if (     ( relation.equals( LOCAL_MINIMUM    ) )
           ||  ( relation.equals( LOCAL_MAXIMUM    ) )
           ||  ( relation.equals( ABSOLUTE_MINIMUM ) )
           ||  ( relation.equals( ABSOLUTE_MAXIMUM ) )
           ||  ( relation.equals( ADJUSTED_ABSMAX  ) )
           ||  ( relation.equals( ADJUSTED_ABSMIN  ) )  )
      {
         return true;
      }

      return false;
   }


   /**
   Indicate whether this constraint is an unadjusted
   extremum constraint.
   */
   public boolean isUnadjustedExtremum()
   {
      if (  this.isExtremum()  &&  ( adjustmentValue == 0.0 )  )
      {
         return true;
      }

      return false;
   }


   /**
   Indicate whether this constraint is an adjusted
   extremum constraint.
   */
   public boolean isAdjustedExtremum()
   {
      if (  this.isExtremum()  &&  ( adjustmentValue > 0.0 )  )
      {
         return true;
      }

      return false;
   }


}
