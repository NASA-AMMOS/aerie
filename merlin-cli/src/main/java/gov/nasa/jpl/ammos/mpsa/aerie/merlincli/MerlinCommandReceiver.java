package gov.nasa.jpl.ammos.mpsa.aerie.merlincli;

/**
 * A receiver for Merlin commands.
 *
 * A Merlin command is a command regarding plans, mission models, and simulation thereof.
 */
interface MerlinCommandReceiver {
    boolean createPlan(String path);
    boolean updatePlanFromFile(String planId, String path);
    boolean updatePlanFromTokens(String planId, String[] tokens);
    boolean updatePlan(String planId, String planUpdateJson);
    boolean deletePlan(String planId);
    boolean downloadPlan(String planId, String outName);
    boolean appendActivityInstances(String planId, String path);
    boolean displayActivityInstance(String planId, String activityId);
    boolean updateActivityInstance(String planId, String activityId, String[] tokens);
    boolean deleteActivityInstance(String planId, String activityId);
    boolean listPlans();
    boolean createAdaptation(String path, String[] tokens);
    boolean deleteAdaptation(String adaptationId);
    boolean displayAdaptation(String adaptationId);
    boolean listAdaptations();
    boolean listActivityTypes(String adaptationId);
    boolean displayActivityType(String adaptationId, String activityType);
    boolean convertApfFile(String input, String output, String dir, String[] tokens);
}
