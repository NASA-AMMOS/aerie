public class DeclinationParameter extends MerlinParameter<Float> {

    public DeclinationParameter(Float val) {
        super(val);
    }

    // One potential style would be to just put the values in the functions
    public Float  getDefault() {
        return Math.PI/2;
    }
    public String getUnits() {
        return "radians";
    }
    public String getDimension() {
        return "angle";
    }
    public String getBrief() {
        return "the target pointing declination angle, in radians";
    }
    public String getDocumentation() {
        return "the angle measured in radians north of the celestial equator at "
             + "the specified epoch time along the hour circle that passes through "
             + "the target point; essentially celestial latitude";
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
        validators.add( Validators.inRange( -Math.PI/2, Math.PI/2 ) );

        return validators;
    }
}