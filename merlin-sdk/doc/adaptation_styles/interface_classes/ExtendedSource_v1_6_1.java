public class ExtendedSource_v1_6_1 implements MerlinActivity {

    /****************************************************************************************************/
    /**    Define type level fields using get methods, these are abstract in the MerlinActivity class  **/
    /****************************************************************************************************/

    private static final String version = "1.6.1";
    private static final String labels = "es";
    private static final String brief = "calibrate the uvs airglow port by pointing to jupiter";
    private static final String documentation =
            "points the uvs at a non-point source of ultraviolet light (typically "
                    +" the disc of jupiter while it is sufficiently illuminated by the sun) "
                    +" and collect air-glow port photon data from the source in order to "
                    +" characterize the air-glow slit's optical throughput (in combination "
                    +" with prior information of the point spread function and slit shape).\n"
                    +"should occur once during tour"
            ;
    private static final String references = "https://github.jpl.nasa.gov/Europa/OPS/blob/clipper-develop/seq/aaf/Clipper/Instruments/UVS_activities.aaf#L695";

    public static String getVersion() {
        return version;
    }

    public static String getLabels() {
        return labels;
    }

    public static String getBrief() {
        return brief;
    }

    public static String documentation() {
        return documentation;
    }

    /***************************/
    /** Parameter Definitions **/
    /***************************/


    private SolarSystemBodiesParameter targetBody;


    /*****************************************/
    /**   IDE Generated Parameter Setters   **/
    /*****************************************/

    public void setTargetBody(SolarSystemBodiesParameter targetBody) {
        this.targetBody = targetBody;
    }

    /**************************************************/
    /**   Define model function                      **/
    /**   Parameters are readily available by name   **/
    /**   Attributes are available by getters        **/
    /**************************************************/

    public void model() {

    }

    /************************************/
    /**   Simple parameter overrides   **/
    /************************************/

    public enum SolarSystemBodies {
        MERCURY,
        VENUS,
        EARTH,
        MARS,
        JUPITER,
        SATURN,
        URANUS,
        NEPTUNE
    }

    public static class SolarSystemBodiesParameter extends MerlinEnumParameter<SolarSystemBodies> {

        @Override
        public String getDefault() {
            return SolarSystemBodies.JUPITER;
        }

        @Override
        public String getBrief() {
            return "target body for uvs to point at during calibration";
        }

        @Override
        public String getDocumentation() {
            return "the target body that the uvs should point at and track continuously "
                    +" during the extended source calibration, selected from the set of "
                    +" solar system bodies in the navigation frame tree"
                    ;
        }
    }


}