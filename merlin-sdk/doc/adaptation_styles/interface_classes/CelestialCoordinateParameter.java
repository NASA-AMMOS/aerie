public class CelestialCoordinateParameter extends MerlinParameter<CelestialCoordinate> {

    private static final String units = "radians, radians";
    private static final String dimension = "angle, angle";
    private static final String brief = "Coordinate defined by target right acension and declination angles";
    private static final String documentation =
            "CelestialCoordinate consists of RA and DEC:"
            + "RA:"
            + "    the angle measured in radians north of the celestial equator at "
            + "    the specified epoch time along the hour circle that passes through "
            + "    the target point; essentially celestial latitude"
            + "DEC:"
            + "    the angle measured in radians north of the celestial equator at "
            + "    the specified epoch time along the hour circle that passes through "
            + "    the target point; essentially celestial latitude";

    public CelestialCoordinateParameter(CelestialCoordinate coordinate) {
        super(coordinate);
    }

    public CelestialCoordinate getDefault() {
        // return user defined default celestial coordinate
        return new CelestialCoordinate(0, Math.PI/2);
    }

    public List<Validator> getValidators() {
        List<Validator> validators = new ArrayList<>();

        /** Add validator using a lambda that takes this instance **/
        validators.add( new Validator((CelestialCoordinate coord) -> {
            CelestialCoordinate coord = coord.getValue();
            float ra = coord.getRightAcension();
            if ( (ra < -Math.PI) || (ra > Math.PI) ) {
                throw ParameterValidationException("right acension=" + v + " out of range [-pi,+pi]");
            }
        }));

        validators.add( new Validator((CelestialCoordinate coord) -> {
            CelestialCoordinate coord = coord.getValue();
            float dec = coord.getDeclination();
            if ( (dec < -Math.PI/2) || (dec > Math.PI/2) ) {
                throw ParameterValidationException("declination=" + v + " out of range [-pi/2,+pi/2]");
            }
        }));

        return validators;
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


}