package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.protocol.types.Duration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Class mocking the behavior of an externally defined state and implementing ExternalState interface
 * @param <T> the type of the variable managed by the state
 */
public class SimResource<T extends Comparable<T>> implements
        ExternalState<T> {

    TreeMap<Range<Time>, T> values;

    public boolean isEmpty(){
        return values == null || values.isEmpty();
    }

    public void failIfEmpty(){
        if(isEmpty()){
            throw new IllegalArgumentException("Trying to use uninitialized resource (have you simulated before?)");
        }
    }

    public void initFromSimRes(List<Pair<Duration, T>> fileValues, Time planningHorizonStart){
        values = new TreeMap<Range<Time>, T>();
        Time start = null;
        T val =null;
        T lastVal = null;
        int i = 0;
        for(Pair<Duration, T> entry : fileValues){
            i++;
            val = entry.getValue();

           var time = planningHorizonStart.plus(gov.nasa.jpl.aerie.scheduler.Duration.fromMillis(entry.getKey().in(Duration.MILLISECOND)));
            if(start == null){
                start = time;
                lastVal = val;
            }
            if(!val.equals(lastVal) || i == fileValues.size()) {
                values.put(new Range<Time>(start, time), lastVal);
                start = time;
            }
            lastVal= val;
        }
    }


    public T getValueAtTime(Time t){
        failIfEmpty();
        //NB: reverse iteration so that the inclusive contains() queries encounter the latest-starting range first
        //TODO: could be vastly improved by leveraging the non-overlapping data invariant and tree map floorKey queries
        for(Map.Entry<Range<Time>,T> intv : values.descendingMap().entrySet()){
            if(intv.getKey().contains(t)){
                return intv.getValue();
            }
        }
        return null;
    }

    public TimeWindows whenValueBetween(T inf, T sup, TimeWindows windows){
        failIfEmpty();

        TimeWindows returnWindows = new TimeWindows();

        Collection< Range<Time> > windowsR = windows.getRangeSet();
        for(Map.Entry<Range<Time>,T> intv : values.entrySet()){
            if(intv.getValue().compareTo(inf) >= 0 && intv.getValue().compareTo(sup) <= 0){
                Range<Time> stateRange = intv.getKey();
                for(Range<Time> range : windowsR){
                    Range<Time> inter = range.intersect(stateRange);
                    if(inter != null){
                        returnWindows.union(inter);
                    }
                }
            }
        }
        return returnWindows;
    }

    public TimeWindows whenValueBelow(T val, TimeWindows windows){
        failIfEmpty();

        TimeWindows returnWindows = new TimeWindows();

        Collection< Range<Time> > windowsR = windows.getRangeSet();
        for(Map.Entry<Range<Time>,T> intv : values.entrySet()){
            if(intv.getValue().compareTo(val) < 0){
                Range<Time> stateRange = intv.getKey();
                for(Range<Time> range : windowsR){
                    Range<Time> inter = range.intersect(stateRange);
                    if(inter != null){
                        returnWindows.union(inter);
                    }
                }
            }
        }
        return returnWindows;
    }

    public TimeWindows whenValueAbove(T val, TimeWindows windows){
        failIfEmpty();

        TimeWindows returnWindows = new TimeWindows();

        Collection< Range<Time> > windowsR = windows.getRangeSet();
        for(Map.Entry<Range<Time>,T> intv : values.entrySet()){
            if(intv.getValue().compareTo(val) > 0){
                Range<Time> stateRange = intv.getKey();
                for(Range<Time> range : windowsR){
                    Range<Time> inter = range.intersect(stateRange);
                    if(inter != null){
                        returnWindows.union(inter);
                    }
                }
            }
        }
        return returnWindows;

    }

    public TimeWindows whenValueEqual(T val, TimeWindows windows){
        failIfEmpty();

        TimeWindows returnWindows = new TimeWindows();

        Collection< Range<Time> > windowsR = windows.getRangeSet();
        for(Map.Entry<Range<Time>,T> intv : values.entrySet()){
            if(intv.getValue().compareTo(val) == 0){
                Range<Time> stateRange = intv.getKey();
                for(Range<Time> range : windowsR){
                    Range<Time> inter = range.intersect(stateRange);
                    if(inter != null){
                        returnWindows.union(inter);
                    }
                }
            }
        }
        return returnWindows;

    }

    @Override
    public Map<Range<Time>, T> getTimeline(TimeWindows timeDomain) {
        return values;
    }

    @Override
    public TimeWindows whenValueNotEqual(T val, TimeWindows windows) {
        failIfEmpty();
        Collection<Range<Time>> result = new ArrayList<Range<Time>>();
        Collection< Range<Time> > windowsR = windows.getRangeSet();
        for(Map.Entry<Range<Time>,T> intv : values.entrySet()){
            if(intv.getValue().compareTo(val) != 0){
                Range<Time> stateRange = intv.getKey();
                for(Range<Time> range : windowsR){
                    Range<Time> inter = range.intersect(stateRange);
                    if(inter != null){
                        result.add(inter);
                        //returnWindows.union(inter);
                    }
                }
            }
        }
        return TimeWindows.of(result);
    }

    public void draw(){
        for(Map.Entry<Range<Time>, T> v : values.entrySet()){
            if(v.getValue() instanceof Boolean){
                Boolean val = (Boolean) v.getValue();
                String toPrint = "";
                if(val){
                    toPrint = "X";
                } else{
                    toPrint = "-";
                }
                int max = (int) v.getKey().getMaximum().toEpochMilliseconds()/1000;
                int min = (int) v.getKey().getMinimum().toEpochMilliseconds()/1000;

                for(int i = min; i < max; i++){
                    System.out.print(toPrint+"  ");

                }

            }
        }
        System.out.println("");


    }

}
