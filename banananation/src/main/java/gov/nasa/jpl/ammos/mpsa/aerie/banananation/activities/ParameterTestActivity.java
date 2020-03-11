package gov.nasa.jpl.ammos.mpsa.aerie.banananation.activities;

import gov.nasa.jpl.ammos.mpsa.aerie.banananation.state.BananaStates;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.Activity;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.annotations.Parameter;

import java.util.List;
import java.util.Map;

@ActivityType(name="ParameterTest", states=BananaStates.class)
public class ParameterTestActivity implements Activity<BananaStates> {
  @Parameter public double a = 3.141;
  @Parameter public float b = 1.618f;
  @Parameter public byte c = 16;
  @Parameter public short d = 32;
  @Parameter public int e = 64;
  @Parameter public long f = 128;
  @Parameter public char g = 'g';
  @Parameter public String h = "h";

  @Parameter
  public List<Integer> intList = null;

  @Parameter
  public List<List<String>> stringList = null;

  @Parameter
  public Map<Integer, List<String>> mappyBoi = null;

  @Parameter
  public List<Integer>[][] intListArrayArray = null;

  @Parameter
  public List<Map<String[][], Map<Integer, List<Float>[]>>> obnoxious;
}
