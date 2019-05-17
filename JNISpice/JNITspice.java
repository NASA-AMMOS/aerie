import spice.basic.CSPICE;
import spice.testutils.JNITestutils;
import spice.tspice.*;

public class JNITspice extends Object
{
   /*
   Load the JNISpice C shared object library.

   This step is executed before the main program runs.
   */
   static {
             System.loadLibrary( "JNISpice" );
          }


   public static void main ( String[] args )
   {

      try
      {
         boolean ok = true;

         //
         // Concatenate the command line arguments into a single string.
         //
         StringBuffer sb = new StringBuffer( "JNITspice " );

         for ( int i = 0;  i < args.length;  i++ )
         {
            sb.append( args[i] );
            sb.append( " "     );
         }

         String cml = sb.toString();

         System.out.println( "args = " + cml );

         CSPICE.putcml ( args );

         JNITestutils.tsetup ( cml, "JNISpice{0-9}{0-9}.log", "1.0.0" );


         // For debugging:
         //if ( !ok ) { return; }

         ok = TestAberrationCorrection.f_AberrationCorrection();
         ok = TestAngularUnits.f_AngularUnits();
         ok = TestAxisAndAngle.f_AxisAndAngle();
         ok = TestBody.f_Body();
         ok = TestCK.f_CK();
         ok = TestCylindricalCoordinates.f_CylindricalCoordinates();
         ok = TestDAF.f_DAF();
         ok = TestDistanceUnits.f_DistanceUnits();
         ok = TestEllipse.f_Ellipse();
         ok = TestEllipsePlaneIntercept.f_EllipsePlaneIntercept();
         ok = TestEllipsoid.f_Ellipsoid();
         ok = TestEllipsoidLineNearPoint.f_EllipsoidLineNearPoint();
         ok = TestEllipsoidPointNearPoint.f_EllipsoidPointNearPoint();
         ok = TestEulerAngles.f_EulerAngles();
         ok = TestEulerState.f_EulerState();
         ok = TestFOV.f_FOV();
         ok = TestFrameInfo.f_FrameInfo();
         ok = TestGFAngularSeparationSearch.f_GFAngularSeparationSearch();
         ok = TestGFConstraint.f_GFConstraint();
         ok = TestGFDistanceSearch.f_GFDistanceSearch();
         ok = TestGFIlluminationAngleSearch.f_GFIlluminationAngleSearch();
         ok = TestGFOccultationSearch.f_GFOccultationSearch();
         ok = TestGFPhaseAngleSearch.f_GFPhaseAngleSearch();
         ok = TestGFPositionCoordinateSearch.f_GFPositionCoordinateSearch();
         ok = TestGFRangeRateSearch.f_GFRangeRateSearch();
         ok = TestGFRayInFOVSearch.f_GFRayInFOVSearch();
         ok = TestGFSubObserverCoordinateSearch.
              f_GFSubObserverCoordinateSearch();
         ok = TestGFSurfaceInterceptCoordinateSearch.
              f_GFSurfaceInterceptCoordinateSearch();
         ok = TestGFTargetInFOVSearch.f_GFTargetInFOVSearch();
         ok = TestGFUserDefinedScalarSearch.f_GFUserDefinedScalarSearch();
         ok = TestGeodeticCoordinates.f_GeodeticCoordinates();
         ok = TestGeometry02.f_Geometry02();
         ok = TestInstrument.f_Instrument();
         ok = TestJEDDuration.f_JEDDuration();
         ok = TestJEDTime.f_JEDTime();
         ok = TestKernelDatabase.f_KernelDatabase();
         ok = TestKernelPool.f_KernelPool();
         ok = TestLatitudinalCoordinates.f_LatitudinalCoordinates();
         ok = TestLine.f_Line();
         ok = TestLocalSolarTime.f_LocalSolarTime();
         ok = TestMatrix33.f_Matrix33();
         ok = TestMatrix66.f_Matrix66();
         ok = TestOsculatingElements.f_OsculatingElements();
         ok = TestPhysicalConstants.f_PhysicalConstants();
         ok = TestPlane.f_Plane();
         ok = TestPlanetographicCoordinates.f_PlanetographicCoordinates();
         ok = TestPointingAndAVRecord.f_PointingAndAVRecord();
         ok = TestPointingRecord.f_PointingRecord();
         ok = TestPositionRecord.f_PositionRecord();
         ok = TestPositionVector.f_PositionVector();
         ok = TestRADecCoordinates.f_RADecCoordinates();
         ok = TestRay.f_Ray();
         ok = TestRayEllipsoidIntercept.f_RayEllipsoidIntercept();
         ok = TestRayPlaneIntercept.f_RayPlaneIntercept();
         ok = TestReferenceFrame.f_ReferenceFrame();
         ok = TestRotationAndAV.f_RotationAndAV();
         ok = TestSCLKDuration.f_SCLKDuration();
         ok = TestSCLKTime.f_SCLKTime();
         ok = TestSPK.f_SPK();
         ok = TestSphericalCoordinates.f_SphericalCoordinates();
         ok = TestSpiceQuaternion.f_SpiceQuaternion();
         ok = TestSpiceWindow.f_SpiceWindow();
         ok = TestStateRecord.f_StateRecord();
         ok = TestStateVector.f_StateVector();
         ok = TestTDBDuration.f_TDBDuration();
         ok = TestTDBTime.f_TDBTime();
         ok = TestTDTDuration.f_TDTDuration();
         ok = TestTDTTime.f_TDTTime();
         ok = TestTextIO.f_TextIO();
         ok = TestTimeConstants.f_TimeConstants();
         ok = TestTimeSystem.f_TimeSystem();
         ok = TestUnits.f_Units();
         ok = TestVector3.f_Vector3();
         ok = TestVector6.f_Vector6();
         ok = TestVelocityVector.f_VelocityVector();


         JNITestutils.tclose();

      }
      catch ( Throwable th )
      {
         System.out.println ( "exception thrown on JNI call" );
         th.printStackTrace();
      }

   }

}
