public class BrightStarStare_v2_7_1 implements MerlinActivity {

    private static final String version = "2.7.1";
    private static final String labels = "bss";
    private static final String brief = "calibrate the uvs via long exposure to a bright star";
    private static final String documentation =
            "points the uvs at a well-characterized star that is bright in the "
                    +"the ultraviolet and then observes it using both the airglow and "
                    +"high-resolution ports in turn in order to ascertain ongoing detector "
                    +"degredation and the optics point spread function.\n"
                    +"should occur several times throughout the mission, including tour"
            ;
    private static final String references = "https://github.jpl.nasa.gov/Europa/OPS/blob/clipper-develop/seq/aaf/Clipper/Instruments/UVS_activities.aaf#L541"

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

    private RightAcensionParameter targetRightAcension;
    private DeclinationParameter   targetDeclination;
    private OneHourDefaultDurationParameter highResolutionPortDuration;
    private highResolutionPortDataVolumeParameter highResolutionPortData; // Type defined below
    private OneHourDefaultDurationParameter airGlowPortDuration;
    private airGlowPortDataVolumeParameter airGlowPortData; // Type defined below

    /*****************************************/
    /**   IDE Generated Parameter Setters   **/
    /*****************************************/

    public void setTargetRightAcension(RightAcensionParameter targetRightAcension) {
        this.targetRightAcension = targetRightAcension;
    }

    public void setTargetDeclination(DeclinationParameter targetDeclination) {
        this.targetDeclination = targetDeclination;
    }

    public void setHighResolutionPortDuration(OneHourDefaultDurationParameter highResolutionPortDuration) {
        this.highResolutionPortDuration = highResolutionPortDuration;
    }

    public void setHighResolutionPortData(highResolutionPortDataVolumeParameter highResolutionPortData) {
        this.highResolutionPortData = highResolutionPortData;
    }

    public void setAirGlowPortDuration(OneHourDefaultDurationParameter airGlowPortDuration) {
        this.airGlowPortDuration = airGlowPortDuration;
    }

    public void setAirGlowPortData(airGlowPortDataVolumeParameter airGlowPortData) {
        this.airGlowPortData = airGlowPortData;
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

    public static class highResolutionPortDataVolumeParameter extends MerlinDataVolumeParameter {

        @Override
        public Float getDefault() {
            return 100 * DataVolume.Megabit;
        }
    }

    public static class airGlowPortDataVolumeParameter extends MerlinDataVolumeParameter {

        @Override
        public Float getDefault() {
            return 200 * DataVolume.Megabit;
        }
    }
}