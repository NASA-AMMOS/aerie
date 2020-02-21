package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.MasterSystemModel;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.systemmodels.MissionModelGlue.Registry;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Duration;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Instant;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.TimeUnit;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.time.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public class DataSystemModel implements SystemModel{

    private DataModelSlice initialSlice;
    private MissionModelGlue glue;
    private Registry registry;
    private String name = GlobalPronouns.DataSystemModel;

    public DataSystemModel(MissionModelGlue glue, Instant initialTime){
        this.glue = glue;
        registry = glue.registry();
        registerSelf();
        initialSlice = new DataModelSlice(initialTime);
    }

    @Override
    public Registry getRegistry(){
        return this.glue.registry();
    }

    @Override
    public MasterSystemModel getMasterSystemModel() { return this.glue.MasterSystemModel(); }

    @Override
    public Slice getInitialSlice(){
        return this.initialSlice.cloneSlice();
    }

    @Override
    public void mapStateNameToSystemModelName(String stateName){
        this.glue.mapStateNameToSystemModelName(stateName, this.name);
    }


    @Override
    public void step(Slice aSlice, Duration dt) {
        DataModelSlice slice = (DataModelSlice) aSlice;
        if (slice.dataRate.lastEntry().getValue() > 100){
            slice.dataProtocol.put(slice.time, GlobalPronouns.spacewire);
        }
        slice.setTime(slice.time().plus(dt));
    }

    @Override
    public void registerSelf() {
        registry.provideSettable(this, GlobalPronouns.dataRate, Double.class, this::setDataRate, this::getDataRate);
        registry.provideSettable(this, GlobalPronouns.dataProtocol, String.class, this::setDataProtocol, this::getDataProtocol);
        registry.registerGetter(this, GlobalPronouns.dataVolume, Double.class, this::getDataVolume);
    }

    @Override
    public String getName(){
        return this.name;
    }

    public double getDataRate(Slice slice){
        return ((DataModelSlice)slice).dataRate.lastEntry().getValue();
    }

    public double getDataVolume(Slice s){
        final var slice = (DataModelSlice) s;

        var dataVolume = slice.initialVolume;
        var dataRate = 0.0;
        var currentTime = slice.startTime;
        for (final var entry : slice.dataRate.entrySet()) {
            final var dt = entry.getKey().durationFrom(currentTime);
            currentTime = entry.getKey();

            dataVolume += dataRate * (double)(dt.durationInMicroseconds / 1000000L);
            dataRate = entry.getValue();
        }

        return dataVolume;
    }

    public String getDataProtocol(Slice slice){
        return ((DataModelSlice)slice).dataProtocol.lastEntry().getValue();
    }

    public void setDataRate(Slice s, Double dataRate){
        final var slice = (DataModelSlice) s;
        slice.dataRate.put(slice.time, dataRate);
    }

    public void setDataProtocol(Slice s, String dataProtocol){
        final var slice = (DataModelSlice) s;
        slice.dataProtocol.put(slice.time, dataProtocol);
    }

    /* Need to create a cumulative event interface
    public void incrementDataRate(Slice slice, double delta){
        ((DataModelSlice)slice).dataRate += delta;
    }

    public void decrementDataRate(Slice slice,  double delta){
        ((DataModelSlice)slice).dataRate -= delta;
    }*/

    public List<Window> whenDataRateLessThan(final Slice s, final double x){
        final DataModelSlice slice = (DataModelSlice) s;
        List<Window> windows = new ArrayList<>();
        //todo: populate windows

        System.out.println("Returning windows for data rate less than " + x);
        return windows;
    }

    public List<Window> whenDataRateGreaterThan(final Slice s, final double x){
        final DataModelSlice slice = (DataModelSlice) s;
        List<Window> windows = new ArrayList<>();
        //todo: populate windows

        System.out.println("Returning windows for data rate greater than than " + x);
        return windows;
    }

    public List<Window> whenDataVolumeGreaterThan(final Slice s, final double threshold){
        final DataModelSlice slice = (DataModelSlice) s;
        final var windows = new ArrayList<Window>();

        var dataVolume = slice.initialVolume;
        var dataRate = 0.0;
        var currentTime = slice.startTime;

        final var iter = slice.dataRate.entrySet().iterator();
        while (iter.hasNext()) {
            Instant start = null;
            while (iter.hasNext()) {
                final var point = iter.next();

                final var prevDataVolume = dataVolume;
                final var prevDataRate = dataRate;
                final var prevTime = currentTime;

                final var dt = point.getKey().durationFrom(currentTime);
                dataVolume += dataRate * (double)(dt.durationInMicroseconds / 1000000L);
                dataRate = point.getValue();
                currentTime = point.getKey();

                if (dataRate > 0) {
                    final var projection = Duration.fromQuantity((long) ((threshold - prevDataVolume) / prevDataRate * 1000000.0), TimeUnit.MICROSECONDS);;

                    if (prevTime.plus(projection).compareTo(point.getKey()) < 0) {
                        start = prevTime.plus(projection);
                        break;
                    }
                }
            }
            if (start == null) break;

            Instant end = null;
            while (iter.hasNext()) {
                final var point = iter.next();

                final var prevDataVolume = dataVolume;
                final var prevDataRate = dataRate;
                final var prevTime = currentTime;

                final var dt = point.getKey().durationFrom(currentTime);
                dataVolume += dataRate * (double)(dt.durationInMicroseconds / 1000000L);
                dataRate = point.getValue();
                currentTime = point.getKey();

                if (dataRate < 0) {
                    final var projection = Duration.fromQuantity((long) ((threshold - prevDataVolume) / prevDataRate * 1000000.0), TimeUnit.MICROSECONDS);;

                    if (prevTime.plus(projection).compareTo(point.getKey()) < 0) {
                        end = prevTime.plus(projection);
                        break;
                    }
                }
            }

            if (end == null) {
                if (dataRate < 0) {
                    final var prevDataVolume = dataVolume;
                    final var prevDataRate = dataRate;
                    final var prevTime = currentTime;

                    final var projection = Duration.fromQuantity((long) ((threshold - prevDataVolume) / prevDataRate * 1000000.0), TimeUnit.MICROSECONDS);;
                    end = prevTime.plus(projection);
                } else {
                    end = slice.time;
                }
            }

            windows.add(Window.between(start, end));
        }

        return windows;
    }

    public List<Window> whenDataProtocolEquals(final Slice s, final String expectation){
        final var slice = (DataModelSlice) s;
        final var windows = new ArrayList<Window>();

        final var iter = slice.dataProtocol.entrySet().iterator();
        while (iter.hasNext()) {
            Instant start = null;
            while (iter.hasNext()) {
                final var point = iter.next();
                if (Objects.equals(point.getValue(), expectation)) {
                    start = point.getKey();
                    break;
                }
            }
            if (start == null) break;

            Instant end = null;
            while (iter.hasNext()) {
                final var point = iter.next();
                if (!Objects.equals(point.getValue(), expectation)) {
                    end = point.getKey();
                    break;
                }
            }
            if (end == null) end = slice.time;

            windows.add(Window.between(start, end));
        }

        return windows;
    }

    private static class DataModelSlice implements Slice{
        private final TreeMap<Instant, Double> dataRate;
        private final double initialVolume;
        private final TreeMap<Instant, String> dataProtocol;
        private final Instant startTime;
        private Instant time;

        public DataModelSlice(Instant startTime){
            this(0.0, 0.0, GlobalPronouns.UART, startTime);
        }

        public DataModelSlice(final DataModelSlice other){
            this.dataRate = new TreeMap<>(other.dataRate);
            this.initialVolume = other.initialVolume;
            this.dataProtocol = new TreeMap<>(other.dataProtocol);
            this.startTime = other.startTime;
            this.time = other.time;
        }

        public DataModelSlice(double dataRate, double dataVolume, String dataProtocol, Instant startTime){
            this.dataRate = new TreeMap<>();
            this.initialVolume = dataVolume;
            this.dataProtocol = new TreeMap<>();
            this.startTime = startTime;
            this.time = startTime;

            this.dataRate.put(startTime, dataRate);
            this.dataProtocol.put(startTime, dataProtocol);
        }

        @Override
        public Instant time(){
            return this.time;
        }

        @Override
        public void setTime(Instant time){
            this.time = time;
        }

        @Override
        public DataModelSlice cloneSlice(){
            return new DataModelSlice(this);
        }
    }
}
