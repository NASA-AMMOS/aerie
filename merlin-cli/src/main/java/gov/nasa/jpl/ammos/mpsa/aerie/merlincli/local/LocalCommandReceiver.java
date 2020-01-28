package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.local;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.MerlinCommandReceiver;
import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models.Adaptation;
import org.apache.commons.lang3.NotImplementedException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LocalCommandReceiver implements MerlinCommandReceiver {
  private final Map<String, AdaptationJar> adaptations = new HashMap<>();

  @Override
  public void createPlan(String path) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void updatePlanFromFile(String planId, String path) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void updatePlanFromTokens(String planId, String[] tokens) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void deletePlan(String planId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void downloadPlan(String planId, String outName) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void appendActivityInstances(String planId, String path) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void displayActivityInstance(String planId, String activityId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void updateActivityInstance(String planId, String activityId, String[] tokens) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void deleteActivityInstance(String planId, String activityId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void listPlans() {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public String createAdaptation(Path path, Adaptation adaptation) {
    if (!Files.isReadable(path)) {
      throw new RuntimeException("Path is not readable");
    } else if (!Files.isRegularFile(path)) {
      throw new RuntimeException("Path is not a file");
    } else if (this.adaptations.containsKey(adaptation.getName())) {
      throw new RuntimeException("An adaptation already exists with that name");
    }

    this.adaptations.put(adaptation.getName(), new AdaptationJar(path));
    return adaptation.getName();
  }

  @Override
  public void deleteAdaptation(String adaptationId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void displayAdaptation(String adaptationId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void listAdaptations() {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void listActivityTypes(String adaptationId) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void displayActivityType(String adaptationId, String activityType) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void convertApfFile(String input, String output, String dir, String[] tokens) {
    throw new NotImplementedException("TODO: implement");
  }

  @Override
  public void performSimulation(String planId) {
    throw new NotImplementedException("TODO: implement");
  }
}
