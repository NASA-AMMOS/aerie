public class OneHourDefaultDurationParmaeter extends MerlinDurationParameter {

    @Override
    public Float getDuration() {
        return 60.0 * 60.0;
    }
}