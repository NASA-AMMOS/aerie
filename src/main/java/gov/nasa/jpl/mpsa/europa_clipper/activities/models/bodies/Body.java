package gov.nasa.jpl.mpsa.europa_clipper.activities.models.bodies;

import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Body {
    private String name;
    private int NAIFID;
    private String NAIFBodyFrame;
    private double[] radii; // in km, should have length 3: larger equatorial radius, smaller equatorial radius, then polar radius
    private Double albeido;
    private Double mu;

    // switches for how much we calculate about this body
    private boolean calculateAltitude;
    private boolean calculateEarthSpacecraftBodyAngle;
    private boolean calculateSubSCInformation;
    private boolean calculateRaDec;
    private boolean calculateIlluminationAngles;
    private boolean calculateSubSolarInformation;
    private boolean calculateLST;

    public Body(String name, int NAIFID, String NAIFBodyFrame, Double albeido){
        this(name, NAIFID, NAIFBodyFrame, albeido, false, false, false, false, false, false, false);
    }

    public Body(String name, int NAIFID, String NAIFBodyFrame, Double albeido, boolean calculateAltitude,
                boolean calculateEarthSpacecraftBodyAngle, boolean calculateSubSCInformation, boolean calculateRaDec,
                boolean calculateIlluminationAngles, boolean calculateSubSolarInformation, boolean calculateLST){
        this.name = name;
        this.NAIFID = NAIFID;
        this.NAIFBodyFrame = NAIFBodyFrame;
        this.albeido = albeido;
        this.calculateAltitude = calculateAltitude;
        this.calculateEarthSpacecraftBodyAngle = calculateEarthSpacecraftBodyAngle;
        this.calculateSubSCInformation = calculateSubSCInformation;
        this.calculateRaDec = calculateRaDec;
        this.calculateIlluminationAngles = calculateIlluminationAngles;
        this.calculateSubSolarInformation = calculateSubSolarInformation;
        this.calculateLST = calculateLST;

        // use SPICE to get body information that comes from kernels
        try {
            radii = CSPICE.bodvcd(NAIFID, "RADII");
        } catch (SpiceErrorException e) {
            radii = null;
        }
        try{
            mu = CSPICE.bodvcd(NAIFID, "GM")[0];
        } catch (SpiceErrorException e) {
            mu = null;
        }
    }

    public String getName() {
        return name;
    }

    public int getNAIFID() {
        return NAIFID;
    }

    public String getNAIFBodyFrame() {
        return NAIFBodyFrame;
    }

    public double[] getRadii() {
        return radii;
    }

    public double getAverageEquitorialRadius(){
        return (radii[0]+radii[1])/2;
    }

    public double getAverageRadius(){
        return (Math.max(radii[0],radii[1]) + radii[2])/2;
    }

    public double getMu(){
        return mu;
    }

    public boolean hasAlbeido(){
        return albeido != null;
    }

    public Double getAlbeido() {
        if(hasAlbeido()){
            return albeido;
        }
        else{
            return 0.0;
        }
    }

    public boolean doCalculateAltitude() {
        return calculateAltitude;
    }

    public boolean doCalculateEarthSpacecraftBodyAngle() {
        return calculateEarthSpacecraftBodyAngle;
    }

    public boolean doCalculateSubSCPoint() {
        return calculateSubSCInformation;
    }

    public boolean doCalculateRaDec() {
        return calculateRaDec;
    }

    public boolean doCalculateIlluminationAngles() {
        return calculateIlluminationAngles;
    }

    public boolean doCalculateSubSolarInformation() {
        return calculateSubSolarInformation;
    }

    public boolean doCalculateLST(){
        return calculateLST;
    }


    /*
     * Below here are all static methods that filter a map of bodies based on whether things should be calculated for them
     */
    public static String[] getNamesOfBodies(Map<String, Body> bodies){
        return bodies.keySet().toArray(new String[bodies.size()]);
    }

    public static String[] getAltitudeBodies(Map<String, Body> bodies){
        List<String> altitudeBodies = new ArrayList<String>();
        for (Map.Entry<String, Body> body : bodies.entrySet()) {
            if(body.getValue().doCalculateAltitude()){
                altitudeBodies.add(body.getKey());
            }
        }
        return altitudeBodies.toArray(new String[altitudeBodies.size()]);
    }

    public static String[] getEarthSCBodies(Map<String, Body> bodies){
        List<String> earthSCBodies = new ArrayList<>();
        for (Map.Entry<String, Body> body : bodies.entrySet()) {
            if(body.getValue().doCalculateEarthSpacecraftBodyAngle()){
                earthSCBodies.add(body.getKey());
            }
        }
        return earthSCBodies.toArray(new String[earthSCBodies.size()]);
    }

    public static String[] getRadiatorAvoidanceBodies(Map<String, Body> bodies){
        List<String> scEarthBodies = new ArrayList<>();
        for (Map.Entry<String, Body> body : bodies.entrySet()) {
            if(body.getValue().doCalculateSubSCPoint()){
                scEarthBodies.add(body.getKey());
            }
        }
        return scEarthBodies.toArray(new String[scEarthBodies.size()]);
    }


    public static String[] getIlluminationAngleBodies(Map<String, Body> bodies){
        List<String> illuminatedBodies = new ArrayList<>();
        for (Map.Entry<String, Body> body : bodies.entrySet()) {
            if(body.getValue().doCalculateIlluminationAngles()){
                illuminatedBodies.add(body.getKey());
            }
        }
        return illuminatedBodies.toArray(new String[illuminatedBodies.size()]);
    }

    public static String[] getSubSolarBodies(Map<String, Body> bodies){
        List<String> subSolarBodies = new ArrayList<>();
        for (Map.Entry<String, Body> body : bodies.entrySet()) {
            if(body.getValue().doCalculateSubSolarInformation()){
                subSolarBodies.add(body.getKey());
            }
        }
        return subSolarBodies.toArray(new String[subSolarBodies.size()]);
    }

    public static String[] getRaDecBodies(Map<String, Body> bodies){
        List<String> raDecBodies = new ArrayList<>();
        for (Map.Entry<String, Body> body : bodies.entrySet()) {
            if(body.getValue().doCalculateRaDec()){
                raDecBodies.add(body.getKey());
            }
        }
        return raDecBodies.toArray(new String[raDecBodies.size()]);
    }
}