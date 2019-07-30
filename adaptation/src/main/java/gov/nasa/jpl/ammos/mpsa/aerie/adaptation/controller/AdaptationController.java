package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controller;

import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.ActivityType;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.Repositories.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.models.AdaptationProjection;
import gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk.AdaptationUtils;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityMapper;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ParameterSchema;

import gov.nasa.jpl.ammos.mpsa.aerie.schemas.ActivityTypeParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin
@RestController
@RequestMapping("/adaptations")
public class AdaptationController {

  private final String ADAPTATION_FILE_PATH = new File("").getAbsolutePath() + "/adaptation_files/";

  private AdaptationRepository repository;

  public AdaptationController(AdaptationRepository adaptationRepository) {
    this.repository = adaptationRepository;
  }

  /**
   * @return Adaptation with specified ID
   */
  @GetMapping("/{adaptationId}")
  public ResponseEntity<Object> getAdaptation(@PathVariable("adaptationId") UUID adaptationId) {
    Optional<Adaptation> optAdapt = repository.findById(adaptationId.toString());
    if (optAdapt.isPresent()) {
      return ResponseEntity.ok(optAdapt.get());
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * @return All adaptations
   */
  @GetMapping("")
  public ResponseEntity<Object> getAvailableAdaptations(@RequestParam(value = "mission", required = false) String mission,
                                                        @RequestParam(value = "owner", required = false) String owner
  ) {
    List<AdaptationProjection> filteredAdaptations = repository.findAllProjectedBy()
            .stream()
            .filter(a -> mission == null || a.getMission().equals(mission))
            .filter(a -> owner == null || a.getOwner().equals(owner))
            .collect(Collectors.toList());

    return ResponseEntity.ok(filteredAdaptations);
  }

  /**
   * Create a new adaptation
   *
   * @param multipartFile The JAR file containing the adaptation
   * @param adaptation The adaptation to upload
   *
   *                      TODO: Process the adaptation when it is uploaded
   *                      TODO: Store activity types in database for quick reference later on
   */
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Object> addAdaptation(@RequestParam("file") MultipartFile multipartFile,
                                              @Valid Adaptation adaptation)
  {
    adaptation.setId(UUID.randomUUID().toString());

   if (adaptation.getMission() == null) {
     adaptation.setMission("");
   }

   if (adaptation.getOwner() == null) {
     adaptation.setOwner("");
   }

    // Check if this adaptation already exists
    for (Adaptation adapt : repository.findAll()) {
      if (adaptation.equals(adapt)) {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
    }

    String location = getStorageLocation(adaptation);
    File path = new File(location);
    // Ensure the path exists by making any missing directories in location
    path.mkdirs();

    // Create the file object under a unique name based on the file provided
    String filePath = getUniqueFilePath(location, multipartFile.getOriginalFilename());
    File file = new File(filePath);

    // Transfer the contents of the uploaded file to the created file
    try {
      file.createNewFile();
      multipartFile.transferTo(file);
      adaptation.setLocation(filePath);
    } catch (IOException e) {
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    try {
      Map<String, ParameterSchema> activityList = loadActivities(adaptation);

      if (activityList == null) return ResponseEntity.unprocessableEntity().build();

      List<ActivityType> activityTypes = new ArrayList<>();
      for (String activityTypeName : activityList.keySet()) {
        ParameterSchema parameterSchema = activityList.get(activityTypeName);
        List<ActivityTypeParameter> parameters = buildParameterList(parameterSchema);

        if (parameters == null) {
          return ResponseEntity.unprocessableEntity().build();
        }

        activityTypes.add(new ActivityType(UUID.randomUUID().toString(), activityTypeName, parameters));
      }
      adaptation.setActivityTypes(activityTypes);


    } catch (IOException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    repository.save(adaptation);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * Delete the adaptation with given id
   */
  @DeleteMapping("/{adaptationId}")
  public ResponseEntity<Object> deleteAdaptation(@PathVariable("adaptationId") UUID adaptationId) {
    Optional<Adaptation> optAdapt = repository.findById(adaptationId.toString());
    if (!optAdapt.isPresent()) {
      return ResponseEntity.notFound().build();
    }

    Adaptation adaptation = optAdapt.get();

    // Ensure the file is deleted before removing the adaptation
    String location = adaptation.getLocation();
    if (location == null) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    File file = new File(location);
    if (file.exists() && !file.delete()) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    repository.delete(adaptation);
    return ResponseEntity.ok().build();
  }

  /**
   * Get the Activity Types in an Adaptation
   *
   * @return Array of Activity Type objects
   */
  @GetMapping("/{adaptationId}/activities")
  public ResponseEntity<Object> getActivityTypesForAdaptation(@PathVariable("adaptationId") UUID adaptationId) {
    Optional<Adaptation> adaptationOpt = repository.findById(adaptationId.toString());

    if (!adaptationOpt.isPresent()) return ResponseEntity.notFound().build();

    Adaptation adaptation = adaptationOpt.get();
    List<ActivityType> activityTypes = adaptation.getActivityTypes();
    return ResponseEntity.ok(activityTypes);
  }

  /**
   * Get an Activity Type in an Adaptation
   *
   * @return Array of Activity Type objects
   */
  @GetMapping("/{adaptationId}/activities/{activityId}")
  public ResponseEntity<Object> getActivityTypeForAdaptation(@PathVariable("adaptationId") UUID adaptationId,
                                                             @PathVariable("activityId") UUID activityId) {

    Optional<Adaptation> adaptationOpt = repository.findById(adaptationId.toString());

    if (!adaptationOpt.isPresent()) return ResponseEntity.notFound().build();

    Adaptation adaptation = adaptationOpt.get();
    List<ActivityType> activityTypes = adaptation.getActivityTypes();
    for (ActivityType activityType : activityTypes) {
      if (activityType.getId().equals(activityId.toString())) {
        return ResponseEntity.ok(activityType);
      }
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Get the Parameters for an Activity Type in an Adaptation
   *
   * @return Array of Activity Type objects
   */
  @GetMapping("/{adaptationId}/activities/{activityId}/parameters")
  public ResponseEntity<Object> getParametersForActivityType(@PathVariable("adaptationId") UUID adaptationId,
                                                             @PathVariable("activityId") UUID activityId) {

    Optional<Adaptation> adaptationOpt = repository.findById(adaptationId.toString());

    if (!adaptationOpt.isPresent()) return ResponseEntity.notFound().build();

    Adaptation adaptation = adaptationOpt.get();
    List<ActivityType> activityTypes = adaptation.getActivityTypes();
    for (ActivityType activityType : activityTypes) {
      if (activityType.getId().equals(activityId.toString())) {
        return ResponseEntity.ok(activityType.getParameters());
      }
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * Determine the storage location for an adaptation
   * @param adaptation - The adaptation to be stored
   * @return Full path to directory where adaptation should be stored
   */
  private String getStorageLocation(Adaptation adaptation) {
    final String subdir;
    if (adaptation.getMission() == null || adaptation.getMission().equals("")) {
      subdir = "other";
    } else {
      subdir = adaptation.getMission();
    }
    return ADAPTATION_FILE_PATH + subdir + "/";
  }

  /**
   * @param location Path to the directory for the file to be placed
   * @param basename Basename for the file
   * @return String path followed by _X where X is the smallest positive integer
   *         that makes the filename unique (unless basename is already unique)
   */
  private String getUniqueFilePath(String location, String basename) {
    String filename = location + basename;
    File file = new File(filename);
    for (int i = 0; file.exists(); ++i) {
      filename = location + basename + "_" + i;
      file = new File(filename);
    }
    return filename;
  }

  private Map<String, ParameterSchema> loadActivities(Adaptation adaptation) throws IOException {
    final MerlinAdaptation userAdaptation = AdaptationUtils.loadAdaptation(adaptation.getLocation());

    if (userAdaptation == null) {
      return null;
    }

    final ActivityMapper activityMapper = userAdaptation.getActivityMapper();
    final Map<String, ParameterSchema> activitySchemas = activityMapper.getActivitySchemas();

    return activitySchemas;

  }

  private List<ActivityTypeParameter> buildParameterList(ParameterSchema parameterSchema) {
    List<ActivityTypeParameter> parameters = new ArrayList<>();

    Optional<Map<String, ParameterSchema>> parameterMapOpt = parameterSchema.asMap();

    if (!parameterMapOpt.isPresent()) return null;

    for (var parameterEntry : parameterMapOpt.get().entrySet()) {
      String parameterName = parameterEntry.getKey();
      String parameterType = parameterEntry.getValue().match(new SchemaTypeNameVisitor());
      parameters.add(new ActivityTypeParameter(parameterName, parameterType));
    }

    return parameters;
  }
}
