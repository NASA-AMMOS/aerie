package gov.nasa.jpl.aerie.scheduler;

import java.util.Map;
import java.util.TreeMap;

public class DistantState<T extends Comparable<T>> implements ExternalState<T> {

    String name;
    static Duration STEP = Duration.ofSeconds(1);
    AerieController aerieLink;
    Plan plan;
    Class<?> classOf;

    @SuppressWarnings("unchecked")
    public DistantState(String name, AerieController aerieLink, Class<?> classOf, Plan plan){
        this.name = name;
        this.aerieLink = aerieLink;
        this.classOf = classOf;
        this.plan = plan;
    }

    @Override
    public TimeWindows whenValueBetween(T inf, T sup, TimeWindows timeDomain) {
        TimeWindows win = new TimeWindows();
        Time curMin = null;
        Time curMax = null;
        for(Range<Time> g : timeDomain.getRangeSet()){
            for(Time t = g.getMinimum(); t.smallerThan(g.getMaximum()); t = t.plus(STEP)){
                T val = this.getValueAtTime(t);
                if(val.compareTo(inf) > 0 && val.compareTo(sup) < 0 ){
                    //reached interval
                    if(curMin==null){
                        curMin = t;
                        curMax = t;
                    } else{
                        //still in good interval
                        curMax = t;
                    }
                }
                //not good but we are just leaving a good interval
                else if(curMin != null && curMax != null){
                    win.union(new Range<Time>(curMin, curMax));
                    curMax = null;
                    curMin= null;
                }
            }
        }
        return win;
    }


    @Override
    public TimeWindows whenValueBelow(T val, TimeWindows timeDomain) {
        TimeWindows win = new TimeWindows();
        Time curMin = null;
        Time curMax = null;
        for(Range<Time> g : timeDomain.getRangeSet()){
            for(Time t = g.getMinimum(); t.smallerThan(g.getMaximum()); t = t.plus(STEP)){
                T value = this.getValueAtTime(t);
                if(value.compareTo(val) < 0 ){
                    //reached interval
                    if(curMin==null){
                        curMin = t;
                        curMax = t;
                    } else{
                        //still in good interval
                        curMax = t;
                    }
                }
                //not good but we are just leaving a good interval
                else if(curMin != null && curMax != null){
                    win.union(new Range<Time>(curMin, curMax));
                    curMax = null;
                    curMin= null;
                }
            }
        }
        return win;
    }

    @Override
    public TimeWindows whenValueAbove(T val, TimeWindows timeDomain) {
        TimeWindows win = new TimeWindows();
        Time curMin = null;
        Time curMax = null;
        for(Range<Time> g : timeDomain.getRangeSet()){
            for(Time t = g.getMinimum(); t.smallerThan(g.getMaximum()); t = t.plus(STEP)){
                T value = this.getValueAtTime(t);
                if(value.compareTo(val) > 0 ){
                    //reached interval
                    if(curMin==null){
                        curMin = t;
                        curMax = t;
                    } else{
                        //still in good interval
                        curMax = t;
                    }
                }
                //not good but we are just leaving a good interval
                else if(curMin != null && curMax != null){
                    win.union(new Range<Time>(curMin, curMax));
                    curMax = null;
                    curMin= null;
                }
            }
        }
        return win;
    }

    @Override
    public TimeWindows whenValueEqual(T val, TimeWindows timeDomain) {
        TimeWindows win = new TimeWindows();
        Time curMin = null;
        Time curMax = null;
        for(Range<Time> g : timeDomain.getRangeSet()){
            for(Time t = g.getMinimum(); t.smallerThan(g.getMaximum()); t = t.plus(STEP)){
                T value = this.getValueAtTime(t);
                if(value.compareTo(val) == 0 ){
                    //reached interval
                    if(curMin==null){
                        curMin = t;
                        curMax = t;
                    } else{
                        //still in good interval
                        curMax = t;
                    }
                }
                //not good but we are just leaving a good interval
                else if(curMin != null && curMax != null){
                    win.union(new Range<Time>(curMin, curMax));
                    curMax = null;
                    curMin= null;
                }
            }
        }
        return win;     }

    @Override
    public Map<Range<Time>, T> getTimeline(TimeWindows timeDomain) {
        Map<Range<Time>, T> timeline = new TreeMap<Range<Time>, T>();
        Time curMin = null;
        Time curMax = null;
        T curVal = null;
        for(Range<Time> g : timeDomain.getRangeSet()){
            for(Time t = g.getMinimum(); t.smallerThan(g.getMaximum()); t = t.plus(STEP)){
                T value = this.getValueAtTime(t);
                //corner case
                if(curVal==null){
                    curVal = value;
                }
                //same value as before
                if(curVal.compareTo(value) ==0){
                    //reached interval
                    if(curMin==null){
                        curMin = t;
                        curMax = t;
                    } else{
                        //still in good interval
                        curMax = t;
                    }
                }
                //not the same
                else if(curMin != null && curMax != null){
                    //post last interval
                    timeline.put(new Range<Time>(curMin, curMax), curVal);
                    curMax = t;
                    curVal = value;
                    curMin= t;
                }
            }
        }
        return timeline;
    }

    @Override
    public TimeWindows whenValueNotEqual(T val, TimeWindows timeDomain) {
        TimeWindows win = new TimeWindows();
        Time curMin = null;
        Time curMax = null;
        for(Range<Time> g : timeDomain.getRangeSet()){
            for(Time t = g.getMinimum(); t.smallerThan(g.getMaximum()); t = t.plus(STEP)){
                T value = this.getValueAtTime(t);
                if(value.compareTo(val) != 0 ){
                    //reached interval
                    if(curMin==null){
                        curMin = t;
                        curMax = t;
                    } else{
                        //still in good interval
                        curMax = t;
                    }
                }
                //not good but we are just leaving a good interval
                else if(curMin != null && curMax != null){
                    win.union(new Range<Time>(curMin, curMax));
                    curMax = null;
                    curMin= null;
                }
            }
        }
        return win;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getValueAtTime(Time t) {
        T ret;
        if (Double.class.equals(classOf)) {
            ret = (T) aerieLink.getDoubleValue(plan,name, t);
        } else if(Integer.class.equals(classOf)){
            ret = (T) aerieLink.getIntegerValue(plan, name, t);
        }  else {
            throw new IllegalArgumentException("Not implemented");
        }

        return ret;
    }
}
