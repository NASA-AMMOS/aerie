
package spice.tspice;


import java.io.*;
import java.util.Arrays;
import spice.basic.*;
import spice.testutils.JNITestutils;
import spice.testutils.Testutils;



/**
Class TestGeometry02 provides methods that implement test families for
the classes
<pre>
   SubObserverRecord
   SubSolarRecord
   SurfaceIntercept
   IlluminationAngles
</pre>

<p> See the test family {@link TestSurfacePoint} for test cases
applicable to the method {@link SurfaceIntercept#create}.


<p>Version 3.0.0 29-DEC-2016 (NJB)
<pre>
   Updated expected short error messages for blank and
   invalid method strings. These messages now are generated 
   by the method lexer-parser subsystem.

   Added "not found" test cases for class SurfaceIntercept.

   Moved clean-up code to "finally" block.
</pre>

<p>Version 2.0.0 11-MAR-2014 (NJB)

<p>Loosened tolerance for the surface intercept
latitude (named "surf xpoint lat" in the call
to JNITestutils.chcksd), where this latitude is
compared to that of the sub-solar point on the
earth, from TIGHT_TOL to MED_TOL.

<p> Updated expected short error message for
bad method string to be compatible with SPICELIB
routine ILUMIN.

<p>Version 1.0.0 22-NOV-2009 (NJB)
*/
public class TestGeometry02 extends Object
{

   //
   // Class constants
   //
   private static String  PCK           = "geomtest.pck";
   private static String  SPK           = "geomtest.bsp";


   //
   // Class variables
   //


   //
   // Methods
   //

   /**
   Test methods of high-level geometry classes.

   <p> These are
   <pre>
      SubObserverRecord
      SubSolarRecord
      SurfaceIntercept
      IlluminationAngles
   </pre>
   and associated classes.
   */
   public static boolean f_Geometry02()

      throws SpiceException
   {
      //
      // Constants
      //
      final Body                        EARTH           =
                                        new Body( "EARTH" );

      final Body                        MOON           =
                                        new Body( "MOON" );

      final Body                        SUN           =
                                        new Body( "Sun" );

      final ReferenceFrame              IAU_EARTH =
                                        new ReferenceFrame( "IAU_EARTH" );

      final ReferenceFrame              IAU_MOON =
                                        new ReferenceFrame( "IAU_MOON" );

      final ReferenceFrame              J2000 =
                                        new ReferenceFrame( "J2000" );

      final double                      MEDABS          = 1.e-5;
      final double                      MEDREL          = 1.e-10;
      final double                      LOOSE_TOL       = 1.e-7;
      final double                      MED_LOOSE_TOL   = 1.e-10;
      final double                      MED_TOL         = 1.e-11;
      final double                      TIGHT_TOL       = 1.e-12;
      final double                      VTIGHT_TOL      = 1.e-14;

      final String                      UTC             = "1999 Jan 1";


      //
      // Local variables
      //
      AberrationCorrection              abcorr;

      Body                              observer;
      Body                              target;

      GeodeticCoordinates               subGeoCoords;
      GeodeticCoordinates               sunGeoCoords;

      IlluminationAngles                angles;

      LatitudinalCoordinates            subLatCoords;
      LatitudinalCoordinates            sunLatCoords;
      LatitudinalCoordinates            xptLatCoords;

      Matrix33                          xform;

      ReferenceFrame                    fixref;

      StateRecord                       sr;

      SubObserverRecord                 subrec;

      SubSolarRecord                    solrec;

      SurfaceIntercept                  srfxpt;

      String                            method;
      String                            qname;

      TDBTime                           et;
      TDBTime                           trgepc;

      Vector3                           dvec;
      Vector3                           spoint;
      Vector3                           srfvec;

      boolean                           ok;

      double                            f;
      double[]                          radii;
      double                            re;
      double                            rp;

      int                               handle = 0;
      int                               n;


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
      //  Expected exceptions that are *not* thrown are tested
      //  via a call to {@link spice.testutils.Testutils#dogDidNotBark}.
      //

      try
      {

         JNITestutils.topen ( "f_Geometry02" );


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Setup: create and load kernels." );


         //
         // Clear the KernelDatabase system.
         //
         KernelDatabase.clear();

         JNITestutils.tstlsk();

         //
         // Delete PCK if it exists. Create and load a PCK file.
         // Delete the file afterward.
         //
         ( new File ( PCK ) ).delete();

         JNITestutils.tstpck( PCK, true, false );

         //
         // Delete SPK if it exists. Create and load a new
         // version of the file.
         //
         ( new File ( SPK ) ).delete();

         handle = JNITestutils.tstspk( SPK, true );



         // ***********************************************************
         //
         //    Error cases
         //
         // ***********************************************************




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SubObserverRecord: empty method string." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "XLT+S" );
            method   = "";
            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            subrec   = new SubObserverRecord( method, target, et,
                                              fixref, abcorr, observer );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SubObserverRecord: blank method string." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "XLT+S" );
            method   = " ";
            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            subrec   = new SubObserverRecord( method, target, et,
                                              fixref, abcorr, observer );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(BADMETHODSYNTAX)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(BADMETHODSYNTAX)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "IlluminationAngles: empty method string." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "XLT+S" );
            method   = "";
            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            subrec   = new SubObserverRecord( method, target, et,
                                              fixref, abcorr, observer );

            angles   = new IlluminationAngles( "",
                                               target, et,
                                               fixref, abcorr, observer,
                                               subrec.getSubPoint()      );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "IlluminationAngles: blank method string." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "XLT+S" );
            method   = SubObserverRecord.NEAR_POINT_ELLIPSOID;

            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            subrec   = new SubObserverRecord( method, target, et,
                                              fixref, abcorr, observer );

            angles   = new IlluminationAngles( " ",
                                               target, et,
                                               fixref, abcorr, observer,
                                               subrec.getSubPoint()      );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(BADMETHODSYNTAX)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(BADMETHODSYNTAX)", ex );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "IlluminationAngles: bad method string." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "LT+S" );
            method   = SubObserverRecord.NEAR_POINT_ELLIPSOID;
            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            subrec   = new SubObserverRecord( method, target, et,
                                              fixref, abcorr, observer );

            angles   = new IlluminationAngles( "X",
                                               target, et,
                                               fixref, abcorr, observer,
                                               subrec.getSubPoint()      );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //
            Testutils.dogDidNotBark ( "SPICE(BADMETHODSYNTAX)" );
         }
         catch ( SpiceException ex )
         {
            //ex.printStackTrace();
            ok = JNITestutils.chckth ( true, "SPICE(BADMETHODSYNTAX)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SubSolarRecord: empty method string." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "XLT+S" );
            method   = "";
            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            solrec   = new SubSolarRecord( method, target, et,
                                           fixref, abcorr, observer );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SubSolarRecord: blank method string." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "XLT+S" );
            method   = " ";
            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            solrec   = new SubSolarRecord( method, target, et,
                                           fixref, abcorr, observer );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(NOTSUPPORTED)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(NOTSUPPORTED)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SurfaceIntercept: empty method string." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "XLT+S" );
            method   = "";
            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            dvec     = new Vector3(0.0, 0.0, 1.0);

            srfxpt   = new SurfaceIntercept( method, target, et,
                                             fixref, abcorr, observer,
                                             J2000, dvec );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //

            Testutils.dogDidNotBark ( "SPICE(EMPTYSTRING)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(EMPTYSTRING)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SurfaceIntercept: blank method string." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "XLT+S" );
            method   = " ";
            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            dvec     = new Vector3(0.0, 0.0, 1.0);

            srfxpt   = new SurfaceIntercept( method, target, et,
                                             fixref, abcorr, observer,
                                             J2000, dvec );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //


            Testutils.dogDidNotBark ( "SPICE(BADMETHODSYNTAX)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(BADMETHODSYNTAX)", ex );
         }


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SurfaceIntercept: zero direction vector." );

         try
         {
            observer = new Body ( "Earth" );
            target   = new Body ( "Moon"  );
            fixref   = new ReferenceFrame ( "IAU_MOON" );
            abcorr   = new AberrationCorrection ( "XLT+S" );
            method   = SurfaceIntercept.ELLIPSOID;
            et       = new TDBTime( "2009 Oct 27 00:00:00 UTC" );

            dvec     = new Vector3(0.0, 0.0, 0.0);

            srfxpt   = new SurfaceIntercept( method, target, et,
                                             fixref, abcorr, observer,
                                             J2000, dvec );

            //
            // If an exception is *not* thrown, we'll hit this call.
            //


            Testutils.dogDidNotBark ( "SPICE(ZEROVECTOR)" );
         }
         catch ( SpiceException ex )
         {
            ok = JNITestutils.chckth ( true, "SPICE(ZEROVECTOR)", ex );
         }



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SurfaceIntercept: intercept not found, " +
                               "attempt to access intercept member."     );


         observer = SUN;
         target   = EARTH;
         fixref   = IAU_EARTH;
         abcorr   = new AberrationCorrection( "NONE" );
         et       = new TDBTime( UTC );

         sr       = new StateRecord( target, et, J2000, abcorr, observer );
         dvec     = sr.getPosition().negate();

         srfxpt   = new SurfaceIntercept( SurfaceIntercept.ELLIPSOID,
                                          target,   et,    fixref, abcorr,
                                          observer, J2000, dvec            );

         ok = JNITestutils.chcksl( "found", srfxpt.wasFound(), false );


         try
         {
            srfxpt.getIntercept();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SurfaceIntercept: intercept not found, " +
                               "attempt to access surface vector member."  );

         try
         {
            srfxpt.getSurfaceVector();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }

         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase (  "SurfaceIntercept: intercept not found, " +
                               "attempt to access target epoch member."  );

         try
         {
            srfxpt.getTargetEpoch();

            Testutils.dogDidNotBark( "SPICE(POINTNOTFOUND)" );

         }
         catch ( PointNotFoundException exc )
         {
            ok = JNITestutils.chckth( true, "SPICE(POINTNOTFOUND)", exc );
         }


         // ***********************************************************
         //
         //    Normal cases
         //
         // ***********************************************************

         // ***********************************************************
         //
         //    SubObserverRecord tests
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "SubObserverRecord: find the sub-solar " +
                              "point of the Sun on the Earth using the " +
                              "INTERCEPT definition." );


         observer = new Body ( "Sun" );
         target   = new Body ( "Earth"  );
         fixref   = new ReferenceFrame ( "IAU_EARTH" );
         abcorr   = new AberrationCorrection ( "NONE" );
         et       = new TDBTime( UTC );


         method   = SubObserverRecord.INTERCEPT_ELLIPSOID;

         //
         // Find the sub-observer point.
         //
         subrec   = new SubObserverRecord( method, target, et,
                                           fixref, abcorr, observer );

         spoint   = subrec.getSubPoint();
         srfvec   = subrec.getSurfaceVector();
         trgepc   = subrec.getTargetEpoch();

         subLatCoords = new LatitudinalCoordinates( spoint );

         //
         // Get the state of the sun relative to the Earth in
         // Earth bodyfixed coordinates at et. Note that observer and
         // target are swapped in this call.
         //
         sr = new StateRecord( observer, et, fixref, abcorr, target );

         sunLatCoords = new LatitudinalCoordinates( sr.getPosition() );

         //
         // Make sure the directional coordinates match up.
         //
         ok = JNITestutils.chcksd( "Sub point lon",
                                   subLatCoords.getLongitude(),
                                   "~/",
                                   sunLatCoords.getLongitude(),
                                   TIGHT_TOL                    );

         ok = JNITestutils.chcksd( "Sub point lat",
                                   subLatCoords.getLatitude(),
                                   "~/",
                                   sunLatCoords.getLatitude(),
                                   TIGHT_TOL                    );


         //
         // Use class SurfaceIntercept to check spoint, trgepc and srfvec.
         //
         // Map srfvec to the J2000 frame.
         //

         xform  = fixref.getPositionTransformation( J2000, et );

         dvec   = xform.mxv( srfvec );


         srfxpt = new SurfaceIntercept( SurfaceIntercept.ELLIPSOID,
                                        target,    et,    fixref,  abcorr,
                                        observer,  J2000, dvec             );

         //
         // For safety, make sure an intercept was found.
         //

         ok = JNITestutils.chcksl( "found", srfxpt.wasFound(), true );

         if ( srfxpt.wasFound() )
         {
            //
            // The intercept epochs of subrec and srfxpt should
            // match.
            //
            ok = JNITestutils.chcksd( "trgepc",
                                      trgepc.getTDBSeconds(),
                                      "~/",
                                      srfxpt.getTargetEpoch().getTDBSeconds(),
                                      TIGHT_TOL                              );
            //
            // The intercept surface points of subrec and srfxpt should
            // match.
            //
            //
            ok = JNITestutils.chckad( "spoint",
                                      spoint.toArray(),
                                      "~~/",
                                      srfxpt.getIntercept().toArray(),
                                      MED_TOL                          );
         }




         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "SubObserverRecord: find the sub-solar " +
                              "point of the Sun on the Earth using the " +
                              "NEAR POINT definition."                     );

         method   = "NEAR POINT:ELLIPSOID";

         //
         // Find the sub-observer point.
         //
         subrec   = new SubObserverRecord( method, target, et,
                                           fixref, abcorr, observer );

         spoint   = subrec.getSubPoint();
         srfvec   = subrec.getSurfaceVector();
         trgepc   = subrec.getTargetEpoch();


         //
         // We'll need the radii of the earth.
         //
         radii = EARTH.getValues( "RADII" );

         re = radii[0];
         rp = radii[2];

         f  =  ( re - rp ) / re;

         subGeoCoords = new GeodeticCoordinates( spoint, re, f );

         //
         // Get the state of the sun relative to the Earth in
         // Earth bodyfixed coordinates at et. Note that observer and
         // target are swapped in this call.
         //
         sr = new StateRecord( observer, et, fixref, abcorr, target );

         sunGeoCoords = new GeodeticCoordinates( sr.getPosition(), re, f );


         //
         // Make sure the directional coordinates match up.
         //
         ok = JNITestutils.chcksd( "Sub point geodetic lon",
                                   subGeoCoords.getLongitude(),
                                   "~/",
                                   sunGeoCoords.getLongitude(),
                                   TIGHT_TOL                    );

         ok = JNITestutils.chcksd( "Sub point geodetic lat",
                                   subGeoCoords.getLatitude(),
                                   "~/",
                                   sunGeoCoords.getLatitude(),
                                   TIGHT_TOL                    );





         // ***********************************************************
         //
         //    IlluminationAngles tests
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "IlluminationAngles: find the illumination " +
                              "angles on the earth as seen from the moon, " +
                              "evaluated at the sub-moon point " +
                              "(NEARPOINT method)."                         );

         observer = MOON;
         target   = EARTH;
         fixref   = IAU_EARTH;
         abcorr   = new AberrationCorrection ( "NONE" );
         et       = new TDBTime( UTC );

         method   = SubObserverRecord.NEAR_POINT_ELLIPSOID;

         subrec   = new SubObserverRecord( method,  target,  et,
                                           fixref,  abcorr,  observer );

         angles   = new IlluminationAngles( IlluminationAngles.ELLIPSOID,
                                            target, et,       fixref,
                                            abcorr, observer,
                                            subrec.getSubPoint() );



         //
         // We should have an emission angle of zero.
         //
         ok = JNITestutils.chcksd( "Emission angle",
                                   angles.getEmissionAngle(),
                                   "~",
                                   0.0,
                                   TIGHT_TOL                    );

         //
         // The phase angle should match the solar incidence angle.
         //
         ok = JNITestutils.chcksd( "Phase angle",
                                   angles.getPhaseAngle(),
                                   "~",
                                   angles.getSolarIncidenceAngle(),
                                   TIGHT_TOL                    );



         //
         // Use class SurfaceIntercept to check spoint, trgepc and srfvec.
         // Note that `spoint' comes from the sub-observer point, while
         // `trgepc' and `srfvec' come from the IlluminationAngles
         // instance.
         //
         spoint   = subrec.getSubPoint();

         srfvec   = angles.getSurfaceVector();
         trgepc   = angles.getTargetEpoch();

         //
         // Map srfvec to the J2000 frame.
         //

         xform  = fixref.getPositionTransformation( J2000, et );

         dvec   = xform.mxv( srfvec );


         srfxpt = new SurfaceIntercept( SurfaceIntercept.ELLIPSOID,
                                        target,    et,    fixref,  abcorr,
                                        observer,  J2000, dvec             );

         //
         // For safety, make sure an intercept was found.
         //

         ok = JNITestutils.chcksl( "found", srfxpt.wasFound(), true );


         if ( srfxpt.wasFound() )
         {
            //
            // The intercept epochs of subrec and srfxpt should
            // match.
            //
            ok = JNITestutils.chcksd( "trgepc",
                                      trgepc.getTDBSeconds(),
                                      "~/",
                                      srfxpt.getTargetEpoch().getTDBSeconds(),
                                      TIGHT_TOL                              );
            //
            // The intercept surface points of subrec and srfxpt should
            // match.
            //
            //
            ok = JNITestutils.chckad( "spoint",
                                      spoint.toArray(),
                                      "~~/",
                                      srfxpt.getIntercept().toArray(),
                                      MED_TOL                          );
         }



         // ***********************************************************
         //
         //    SubSolarRecord tests
         //
         // ***********************************************************


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SubSolarRecord: find the sub-solar " +
                              "point of the Sun on the Earth using the " +
                              "INTERCEPT definition."                     );


         method   = SubSolarRecord.INTERCEPT_ELLIPSOID;

         observer = SUN;
         target   = EARTH;
         fixref   = IAU_EARTH;
         abcorr   = new AberrationCorrection ( "NONE" );
         et       = new TDBTime( UTC );

         //
         // Find the sub-solar point.
         //
         solrec   = new SubSolarRecord( method, target, et,
                                        fixref, abcorr, observer );

         spoint   = subrec.getSubPoint();
         srfvec   = subrec.getSurfaceVector();
         trgepc   = subrec.getTargetEpoch();


         //
         // Find the sub-solar point, using SubObserverRecord.
         //
         subrec   = new SubObserverRecord(
                                         SubObserverRecord.INTERCEPT_ELLIPSOID,
                                         target, et,
                                         fixref, abcorr, observer );

         //
         // Make sure the surface points match up.
         //
         ok = JNITestutils.chckad( "Sub solar point",
                                   solrec.getSubPoint().toArray(),
                                   "~/",
                                   subrec.getSubPoint().toArray(),
                                   TIGHT_TOL              );



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SubSolarRecord: find the sub-solar " +
                              "point of the Sun on the Earth using the " +
                              "NEAR POINT definition."                     );


         method   = SubSolarRecord.NEAR_POINT_ELLIPSOID;

         observer = SUN;
         target   = EARTH;
         fixref   = IAU_EARTH;
         abcorr   = new AberrationCorrection ( "NONE" );
         et       = new TDBTime( UTC );

         //
         // Find the sub-solar point.
         //
         solrec   = new SubSolarRecord( method, target, et,
                                        fixref, abcorr, observer );

         spoint   = subrec.getSubPoint();
         srfvec   = subrec.getSurfaceVector();
         trgepc   = subrec.getTargetEpoch();


         //
         // Find the sub-solar point, using SubObserverRecord.
         //
         subrec   = new SubObserverRecord(
                                        SubObserverRecord.NEAR_POINT_ELLIPSOID,
                                        target, et,
                                        fixref, abcorr, observer );

         //
         // Make sure the surface points match up.
         //
         ok = JNITestutils.chckad( "Sub solar point",
                                   solrec.getSubPoint().toArray(),
                                   "~/",
                                   subrec.getSubPoint().toArray(),
                                   TIGHT_TOL              );


         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SubSolarRecord: make sure the solar " +
                              "incidence angle at the sub-solar " +
                              "point on the moon as seen from the " +
                              "earth is zero. Use LT+S correction. " +
                              "Near point method."                     );


         method   = SubSolarRecord.NEAR_POINT_ELLIPSOID;

         observer = EARTH;
         target   = MOON;
         fixref   = IAU_MOON;
         abcorr   = new AberrationCorrection ( "LT+S" );
         et       = new TDBTime( UTC );

         //
         // Find the sub-solar point.
         //
         solrec   = new SubSolarRecord( method,
                                        target, et,
                                        fixref, abcorr, observer );

         angles   = new IlluminationAngles( IlluminationAngles.ELLIPSOID,
                                            target, et,       fixref,
                                            abcorr, observer,
                                            solrec.getSubPoint() );

         //
         // The solar incidence angle should be zero.
         //
         ok = JNITestutils.chcksd( "solar incidence angle",
                                   angles.getSolarIncidenceAngle(),
                                   "~",
                                   0.0,
                                   TIGHT_TOL                    );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SubSolarRecord: make sure the solar " +
                              "incidence angle at the sub-solar " +
                              "point on the Earth as seen from the " +
                              "Moon is zero. Use LT+S correction. " +
                              "Near point method."                     );

         //
         // This case uses an oblate target, so the "near point" definition
         // really should give different results than the "intercept"
         // definition. Use CN+S corrections to minimize light time errors.
         //
         method   = SubSolarRecord.NEAR_POINT_ELLIPSOID;

         observer = MOON;
         target   = EARTH;
         fixref   = IAU_EARTH;
         abcorr   = new AberrationCorrection ( "CN+S" );
         et       = new TDBTime( UTC );

         //
         // Find the sub-solar point.
         //
         solrec   = new SubSolarRecord( method,
                                        target, et,
                                        fixref, abcorr, observer );

         angles   = new IlluminationAngles( IlluminationAngles.ELLIPSOID,
                                            target, et,       fixref,
                                            abcorr, observer,
                                            solrec.getSubPoint() );

         //
         // The solar incidence angle should be (very close to) zero.
         //
         ok = JNITestutils.chcksd( "solar incidence angle",
                                   angles.getSolarIncidenceAngle(),
                                   "~",
                                   0.0,
                                   TIGHT_TOL                    );




         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SubSolarRecord: use class SurfaceIntercept " +
                              "to check trgepc and srfvec."                 );


         method   = SubSolarRecord.NEAR_POINT_ELLIPSOID;

         observer = MOON;
         target   = EARTH;
         fixref   = IAU_EARTH;
         //abcorr   = new AberrationCorrection ( "CN" );
         abcorr   = new AberrationCorrection ( "NONE" );
         et       = new TDBTime( UTC );

         //
         // Find the sub-solar point.
         //
         solrec   = new SubSolarRecord( method,
                                        target, et,
                                        fixref, abcorr, observer );

         trgepc   = solrec.getTargetEpoch();
         srfvec   = solrec.getSurfaceVector();

         //
         // Transform the surface vector to the J2000 frame for
         // use with class SurfaceIntercept. This step is necessary to avoid
         // evaluation of the body-fixed frame at incompatible
         // epochs: trgepc for the sub-solar point and et-<light time
         // to target center> for the surface vector.
         //
         xform    = IAU_EARTH.getPositionTransformation( J2000, trgepc );

         dvec     = xform.mxv( srfvec );

         srfxpt   = new SurfaceIntercept( SurfaceIntercept.ELLIPSOID,
                                          target,   et,    fixref, abcorr,
                                          observer, J2000, dvec            );


         ok = JNITestutils.chcksl( "found", srfxpt.wasFound(), true );

         if ( srfxpt.wasFound() )
         {
            //
            // The intercept epochs of solrec and srfxpt should
            // match.
            //
            ok = JNITestutils.chcksd( "trgepc",
                                      trgepc.getTDBSeconds(),
                                      "~/",
                                      srfxpt.getTargetEpoch().getTDBSeconds(),
                                      TIGHT_TOL                              );
            //
            // The surface intercept point of srfxpt should
            // match the sub-solar point.
            //
            //
            ok = JNITestutils.chckad( "spoint",
                                      solrec.getSubPoint().toArray(),
                                      "~~",
                                      srfxpt.getIntercept().toArray(),
                                      MED_LOOSE_TOL                    );
         }



         // ***********************************************************
         //
         //    SurfaceIntercept tests
         //
         // ***********************************************************



         //
         // --------Case-----------------------------------------------
         //
         JNITestutils.tcase ( "SurfaceIntercept: find the sub-solar " +
                              "point of the sun on the Earth. Compare " +
                              "to the corresponding SubObserverRecord " +
                              "found using the INTERCEPT definition."     );


         observer = SUN;
         target   = EARTH;
         fixref   = IAU_EARTH;
         abcorr   = new AberrationCorrection( "NONE" );
         et       = new TDBTime( UTC );

         sr       = new StateRecord( target, et, J2000, abcorr, observer );
         dvec     = sr.getPosition();

         srfxpt   = new SurfaceIntercept( SurfaceIntercept.ELLIPSOID,
                                          target,   et,    fixref, abcorr,
                                          observer, J2000, dvec            );

         ok = JNITestutils.chcksl( "found", srfxpt.wasFound(), true );

         //
         // Compute the corresponding sub-observer point.
         //
         subrec   = new SubObserverRecord(
                                        SubObserverRecord.INTERCEPT_ELLIPSOID,
                                        target, et,
                                        fixref, abcorr, observer );

         if ( srfxpt.wasFound() )
         {
            xptLatCoords = new LatitudinalCoordinates( srfxpt.getIntercept() );
            subLatCoords = new LatitudinalCoordinates( subrec.getSubPoint()  );

            //
            // Make sure the directional coordinates match up.
            //
            ok = JNITestutils.chcksd( "surf xpoint lon",
                                      xptLatCoords.getLongitude(),
                                      "~/",
                                      subLatCoords.getLongitude(),
                                      TIGHT_TOL                    );

            ok = JNITestutils.chcksd( "surf xpoint lat",
                                      xptLatCoords.getLatitude(),
                                      "~/",
                                      subLatCoords.getLatitude(),
                                      MED_TOL                    );

            //
            // The intercept epochs of subrec and srfxpt should
            // match.
            //
            ok = JNITestutils.chcksd( "trgepc",
                                      srfxpt.getTargetEpoch().getTDBSeconds(),
                                      "~/",
                                      subrec.getTargetEpoch().getTDBSeconds(),
                                      TIGHT_TOL                              );

            //
            //  Check the intercept point error in terms of offset magnitude.
            // ( "~~" is the symbol for L2 comparison used by chckad.
            //
            ok = JNITestutils.chckad( "Intercept point",
                                      srfxpt.getIntercept().toArray(),
                                      "~~",
                                      subrec.getSubPoint().toArray(),
                                      LOOSE_TOL                      );
         }



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

      finally
      {
         //
         // --------Case-----------------------------------------------
         //

         JNITestutils.tcase ( "Clean up." );

         //
         // Get rid of the SPK file.
         //
         CSPICE.spkuef( handle );

         ( new File ( SPK ) ).delete();

         //
         // Get rid of the PCK file.
         //
         ( new File ( PCK ) ).delete();
      }


      //
      // Retrieve the current test status.
      //
      ok = JNITestutils.tsuccess();

      return ( ok );
   }

}

