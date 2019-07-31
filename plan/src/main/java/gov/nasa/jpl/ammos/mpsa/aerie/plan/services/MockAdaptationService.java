package gov.nasa.jpl.ammos.mpsa.aerie.plan.services;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.services.AdaptationService;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityTypeParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockAdaptationService implements AdaptationService {
  @Override
  public Map<String, ActivityType> getActivityTypes(String adaptationId) {
    Map<String, ActivityType> activityTypes = new HashMap<>();

    {
      List<ActivityTypeParameter> testParameters = new ArrayList<>();
      {
        ActivityTypeParameter param = new ActivityTypeParameter();
        param.setName("foo");
        param.setType("String");
        param.setDefaultValue("");
        testParameters.add(param);
      }

      ActivityType testActivity = new ActivityType();
      testActivity.setId("1426a0fa-875b-4018-b57a-a19f8e50172c");
      testActivity.setName("test");
      testActivity.setParameters(testParameters);

      activityTypes.put(testActivity.getName(), testActivity);
    }

    {
      ActivityType testActivity = new ActivityType();
      testActivity.setId("1426a0fb-875b-4018-b57a-a19f8e50172c");
      testActivity.setName("test_no_parameters");

      activityTypes.put(testActivity.getName(), testActivity);
    }

    return activityTypes;
  }
}
