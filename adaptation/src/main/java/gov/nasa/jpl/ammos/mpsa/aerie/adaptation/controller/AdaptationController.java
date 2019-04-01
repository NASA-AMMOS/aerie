package gov.nasa.jpl.ammos.mpsa.aerie.adaptation.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.Adaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.AdaptationRepository;
import gov.nasa.jpl.ammos.mpsa.aerie.adaptation.activities.ActivityTypeSerializer;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.builders.AdaptationBuilder;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinSDKAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.activities.ActivityType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.net.URLClassLoader;

import com.google.common.collect.UnmodifiableIterator;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

@CrossOrigin
@RestController
@RequestMapping("/adaptations")
public class AdaptationController {

  @Autowired
  private AdaptationRepository repository;

  private static final Logger logger = LoggerFactory.getLogger(AdaptationController.class);

  private final String ADAPTATION_FILE_PATH = new File("").getAbsolutePath() + "/adaptation_files/";

  /**
   * @return Adaptation with specified name and version, or list of adaptations with name (if no
   * version provided)
   */
  @GetMapping("/{id}")
  public ResponseEntity<Object> getAdaptation(@PathVariable("id") Integer id) {
    logger.debug("GET adaptation/" + id + " requested");
    Optional<Adaptation> optAdapt = repository.findById(id);
    if (optAdapt.isPresent()) {
      logger.debug("returning " + optAdapt.get());
      return ResponseEntity.ok(optAdapt.get());
    }
    return ResponseEntity.notFound().build();
  }

  /**
   * @return All adaptations
   */
  @GetMapping("")
  public ResponseEntity<Object> getAvailableAdaptations() {
    return ResponseEntity.ok(repository.findAll());
  }

  /**
   * Create a new adaptation
   *
   * @param multipartFile The JAR file containing the adaptation
   * @param name Name of the adaptation
   * @param owner User who owns this adaptation
   * @param version Version number for this adaptation
   * @param mission Mission the adaptation is for
   * <p>
   * TODO: Process the adaptation when it is uploaded TODO: Store activity types in database for
   * quick reference later on
   */
  @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Object> addAdaptation(@RequestParam("file") MultipartFile multipartFile,
      @RequestParam("name") String name,
      @RequestParam("owner") String owner,
      @RequestParam("version") String version,
      @RequestParam("mission") String mission) {

    logger.debug("POST adaptation/ to create a new adaptation");

    String location = ADAPTATION_FILE_PATH + mission + "/";
    File path = new File(location);
    // Ensure the path exists by making any missing directories in location
    path.mkdirs();

    // Create the file object under a unique name based on the file provided
    String filePath = getUniqueFilePath(location, multipartFile.getOriginalFilename());
    File file = new File(filePath);

    logger.debug("creating a file in path: " + filePath);
    // Transfer the contents of the uploaded file to the created file
    try {
      logger.debug("starting file transfer");
      file.createNewFile();
      multipartFile.transferTo(file);
    } catch (IOException e) {
      e.printStackTrace();
      logger.error("Exception uploading file for adaptation: " + e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    Adaptation adaptation = new Adaptation(name, version, owner, mission, filePath);
    logger.debug("Now creating adaptation: " + name + " version: " + version + " by " + owner
        + "for mission: " + mission + " in path: " + filePath);
    // Check if this adaptation already exists
    for (Adaptation adapt : repository.findAll()) {
      if (adaptation.equals(adapt)) {
        logger.error("adaptation conflict");
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
      }
    }

    repository.save(adaptation);
    logger.debug("saving adapatation");
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * Delete the adaptation with given id
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Object> deleteAdaptation(@PathVariable("id") Integer id) {
    logger.debug("DELETE adaptation/" + id);

    Optional<Adaptation> optAdapt = repository.findById(id);
    if (optAdapt.isPresent()) {
      Adaptation adaptation = optAdapt.get();

      // Ensure the file is deleted before removing the adaptation
      String location = adaptation.getLocation();
      File file = new File(location);
      if (file.exists() && !file.delete()) {
        logger.error("Adaptation " + id + " error trying to delete file");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      }

      repository.delete(adaptation);
      logger.debug("DELETED adaptation" + id);
      return ResponseEntity.ok().build();
    } else {
      logger.error("Adaptation " + id + " not found");
      return ResponseEntity.notFound().build();
    }
  }

  /**
   * Get the Activity Types in the Adaptation
   *
   * @return Array of Activity Type objects
   */
  @GetMapping("/{id}/activities")
  public ResponseEntity<Object> getActivityTypesForAdaptation(@PathVariable("id") Integer id) {

    logger.debug("GET adaptation/" + id + " /activities");

    Optional<Adaptation> optAdapt = repository.findById(id);
    if (optAdapt.isPresent()) {
      Adaptation adaptation = optAdapt.get();
      try {
        MerlinSDKAdaptation userAdaptation = loadAdaptation(adaptation);
        if (userAdaptation == null) {
          logger.error("NullAdaptation:\n loadAdaptation returned a null adaptation");
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        AdaptationBuilder builder = userAdaptation.init();

        HashMap<String, ActivityType> activityTypes = new HashMap<>();
        builder.getActivivityTypes().forEach((a) -> {
          ActivityType activityType = a.getActivityType();
          activityTypes.put(activityType.getName(), activityType);
        });

        GsonBuilder gsonMapBuilder = new GsonBuilder();
        gsonMapBuilder.registerTypeAdapter(ActivityType.class, new ActivityTypeSerializer());
        Gson gsonObject = gsonMapBuilder.create();
        String JSONObject = gsonObject.toJson(activityTypes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity(JSONObject, headers, HttpStatus.OK);

      } catch (IOException e) {
        // Could not load the adaptation
        logger.error("ClassPathLoaderError:\n " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      } catch (InstantiationException e) {
        // Could not instantiate the adaptation
        logger.error("InstantiationException:\n " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      } catch (IllegalAccessException e) {
        // Could not access the adaptation
        logger.error("IllegalAccessException:\n " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      } catch (Exception e) {
        // Generic error
        logger.error("Exception:\n" + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      }
    }

    logger.error("Getting activities for adaptation " + id + " NOT FOUND");
    return ResponseEntity.notFound().build();
  }

  /**
   * @param location Path to the directory for the file to be placed
   * @param basename Basename for the file
   * @return String path followed by _X where X is the smallest positive integer that makes the
   * filename unique (unless basename is already unique)
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

  // TODO: Move this into the Adaptation Runtime Service when it is complete
  private MerlinSDKAdaptation loadAdaptation(Adaptation adaptation) throws IOException, InstantiationException, IllegalAccessException {
    // Load the adaptation jar
    URL adaptationURL = new File(adaptation.getLocation()).toURI().toURL();
    URLClassLoader loader = new URLClassLoader(new URL[]{adaptationURL},
        Thread.currentThread().getContextClassLoader());

    ClassPath classpath = ClassPath.from(loader);

    // Find all classes in the jpl package
    String pkg = "gov.nasa.jpl";
    UnmodifiableIterator classes = classpath.getTopLevelClassesRecursive(pkg).iterator();

    List<Class<?>> pkgClasses = new ArrayList();
    while(classes.hasNext()) {
      ClassInfo classInfo = (ClassInfo)classes.next();

      try {
        pkgClasses.add(classInfo.load());
      } catch (NoClassDefFoundError e) {

      }
    }

    // Find classes in the jpl package which implement the MerlinSDK Adaptation interface
    // And return the first one that is found
    String adaptationInterface = "MerlinSDKAdaptation";

    for (Class loadedClass : pkgClasses) {
      try {
        if (Class.forName(adaptationInterface).isAssignableFrom(loadedClass)
            && !Class.forName(adaptationInterface).equals(loadedClass)) {
          MerlinSDKAdaptation loadedAdaptation = (MerlinSDKAdaptation) loadedClass.newInstance();
          return loadedAdaptation;
        }
      } catch (ClassNotFoundException e) {
        // Ignore this error and move on to the next class
      }
    }

    return null;
  }

}
