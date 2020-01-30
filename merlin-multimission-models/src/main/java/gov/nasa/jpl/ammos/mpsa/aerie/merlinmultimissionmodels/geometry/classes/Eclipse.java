package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.classes;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.geometry.Globals.OccultationType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.jpltime.Time;

public class Eclipse {

    private Time startTime;
    private Time endTime;
    private OccultationType eclipseType;
    private double fracSunVisible;
    
    public Eclipse(Time startTime, Time endTime, OccultationType eclipseType, double fracSunVisible) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.eclipseType = eclipseType;
        this.fracSunVisible = fracSunVisible;
    }

    public Time getStart() {
        return this.startTime;
    }

    public Time getEnd() {
        return this.endTime;
    }

    public OccultationType getEclipseType() {
        return this.eclipseType;
    }

    public double getFracSunVisible() {
        return this.fracSunVisible;
    }
}