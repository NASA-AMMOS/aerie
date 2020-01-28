package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.local;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.MerlinCommandReceiver;
import org.apache.commons.lang3.NotImplementedException;

public class LocalCommandReceiver implements MerlinCommandReceiver {
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
  public void createAdaptation(String path, String[] tokens) {
    throw new NotImplementedException("TODO: implement");
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
}
