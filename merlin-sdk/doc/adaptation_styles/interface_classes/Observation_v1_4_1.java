public class Observation_v1_4_1 implements MerlinActivity {

    /****************************************************************************************************/
    /**    Define type level fields using get methods, these are abstract in the MerlinActivity class  **/
    /****************************************************************************************************/

    private static final String version = "1.1.4";
    private static final String labels = "UVS";
    private static final String brief = "collect calibration data from the uvs";
    private static final String documentation =
            "opens the specified port on the uvs instrument and begins collecting "
                    + " photon data for calibration purposes. note that the target pointing "
                    + " must be aquired and maintained seprately from this activity, typically "
                    + " by a parent calibration type.";
    private static final String references = "https://github.jpl.nasa.gov/Europa/OPS/blob/clipper-develop/seq/aaf/Clipper/Instruments/UVS_activities.aaf#L755";

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


    private UVSPortParameter port;
    private MerlinDurationParameter duration;
    private MerlinDataVolumeParameter dataVolume;
    private ObservationModeParameter observationMode;

    /*****************************************/
    /**   IDE Generated Parameter Setters   **/
    /*****************************************/

    public void setPort(UVSPortParameter port) {
        this.port = port;
    }

    public void setDuration(MerlinDurationParameter duration) {
        this.duration = duration;
    }

    public void setDataVolume(MerlinDataVolumeParameter dataVolume) {
        this.dataVolume = dataVolume;
    }

    public void setObservationMode(ObservationModeParameter observationMode) {
        this.observationMode = observationMode;
    }

    /**************************************************/
    /**   Define model function                      **/
    /**   Parameters are readily available by name   **/
    /**   Attributes are available by getters        **/
    /**************************************************/

    public void model() {

    }
}