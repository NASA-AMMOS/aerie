public class RightAcensionParameter extends MerlinParameter<Float> {

    // One potential style would be to declare everything together as final static variables
    private static final Float  defaultValue  = 0.0;
    private static final Float  minimum       = -Math.PI;
    private static final Float  maximum       = Math.PI;
    private static final String units         = "radians";
    private static final String dimension     = "angle";
    private static final Float  defaultValue  = 0.0;
    private static final String brief         = "the target pointing right acension angle, in radians";
    private static final String documentation = "the angle measured in radians north of the celestial equator at "
                                              + "the specified epoch time along the hour circle that passes through "
                                              + "the target point; essentially celestial latitude";

    public RightAcensionParameter(Float val) {
        super(val);
    }

    public Float  getDefault() {
        return defaultValue;
    }
    public String getUnits() {
        return units;
    }
    public String getDimension() {
        return dimension;
    }
    public String getBrief() {
        return brief;
    }
    public String getDocumentation() {
        return documentation;
    }

    public List<Validator> getValidators() {
        List<Validator> validators;

        /** This would call getValue on the parameter, and enforce that **/
        /**      a) The type is comparable                              **/
        /**      b) The value is greater than arg1, using compareTo     **/
        /**      c) The value is less than arg2, using compareTo        **/
        /**                                                             **/
        /** If a) fails, the error is in this code                      **/
        /** If b) or c) fails, the error is in the planned value        **/
        validators.add( Validators.inRange( minimum, maximum ) );

        return validators;
    }
}