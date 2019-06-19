package gov.nasa.jpl.clipper.uvs.calibration;

//will need some external code to actually register these types
public class BrightStarRaster_3_1_4 implements MerlinActivity
{
    /****************************************************************************************************/
    /**    Define type level fields using get methods, these are abstract in the MerlinActivity class  **/
    /****************************************************************************************************/

    private static final String version = "3.1.4";
    private static final String labels = "bsr";
    private static final String brief = "calibrate the uvs via raster across a bright star";
    private static final String documentation =
            "scans the uvs airglow port across a field of view including one or "
            +"more well-characterized stars that are bright in the ultraviolet in "
            +"order to detect any alignment shifts in the uvs slit (for example due "
            +"to thrust eventst).\n"
            +"should occur at least once during uvs instrument comissioning."
            ;
    private static final String references = "https://github.jpl.nasa.gov/Europa/OPS/blob/0bc8a70f29d4c74e27b714f1c67c359ce8d9ea14/seq/aaf/Clipper/Instruments/UVS_activities.aaf#L545";

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

    /** Name is instance level **/
    public String getName() {
        return "UvsBrightStarRasterCal";
    }

    // The Above or Below getName style would be used, depending on each activity type, up to the adapter's choice

    /** Can also have a name parameter, and return that here **/
    private MerlinStringParameter name;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /*********************************************************************************************/
    /**    Define parameters as member variables, and make corresponding setters                 */
    /**    Activities will be instantiated by using the default constructor, and these setters   */
    /*********************************************************************************************/

    private CelestialCoordinateParameter target;

    public void setTarget(CelestialCoordinateParameter target) {
        this.target = target;
    }

    /**************************************************/
    /**   Define model function                      **/
    /**   Parameters are readily available by name   **/
    /**   Attributes are available by getters        **/
    /**************************************************/

    public void model() {

    }
}
