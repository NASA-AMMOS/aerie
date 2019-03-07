package gov.nasa.jpl.adaptation.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gov.nasa.jpl.adaptation.Adaptation;
import gov.nasa.jpl.adaptation.AdaptationRepository;
import gov.nasa.jpl.adaptation.activities.ActivityType;
import gov.nasa.jpl.adaptation.activities.ActivityTypeSerializer;
import gov.nasa.jpl.engine.Setup;
import gov.nasa.jpl.input.ReflectionUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@RestController
@RequestMapping("/adaptations")
public class AdaptationController {

    @Autowired
    private AdaptationRepository repository;

    private static final Logger logger = LoggerFactory.getLogger(AdaptationController.class);

    private final String ADAPTATION_FILE_PATH = new File("").getAbsolutePath() + "/adaptation_files/";

    /**
     * @param id
     * @return Adaptation with specified name and version, or list of adaptations with name (if no version provided)
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
     * @param name          Name of the adaptation
     * @param owner         User who owns this adaptation
     * @param version       Version number for this adaptation
     * @param mission       Mission the adaptation is for
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
        logger.debug("Now creating adaptation: " + name + " version: " + version + " by " + owner + "for mission: " + mission + " in path: " + filePath);
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
     *
     * @param id
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
        HashMap<String, ActivityType> activityTypeList = null;
        if (optAdapt.isPresent()) {
            Adaptation adaptation = optAdapt.get();

            try {
                URL adaptationURL = new File(adaptation.getLocation()).toURI().toURL();
                Setup.initializeEngine(adaptationURL);

                activityTypeList = getActivityTypeList();

            } catch (MalformedURLException e) {
                logger.error("Exception when trying to get activities from adaptation: " + e.getStackTrace());
                e.printStackTrace();
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }

            GsonBuilder gsonMapBuilder = new GsonBuilder();
            gsonMapBuilder.registerTypeAdapter(ActivityType.class, new ActivityTypeSerializer());
            Gson gsonObject = gsonMapBuilder.create();
            String JSONObject = gsonObject.toJson(activityTypeList);

            return ResponseEntity.ok(JSONObject);
        } else {
            logger.error("Getting activities for adaptation " + id + " NOT FOUND");
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * @param location Path to the directory for the file to be placed
     * @param basename Basename for the file
     * @return String path followed by _X where X is the smallest positive integer that makes the filename unique (unless basename is already unique)
     */
    private String getUniqueFilePath(String location, String basename) {
        String filename = location + basename;
        File file = new File(filename);
        for (int i = 0; file.exists(); ++i) {
            filename = location + basename + "_" + i;
            file = new File(filename);
            i += 1;
        }
        return filename;
    }


    private HashMap<String, ActivityType> getActivityTypeList() {
        HashMap<String, ActivityType> activityTypesToReturn = new HashMap<>();
        String activitySuperClassLocationInPackage = "gov.nasa.jpl.activity.Activity";

        List<Class> allCustomClasses = ReflectionUtilities.getListOfAllCustomClasses();
        for (Class loadedClass : allCustomClasses) {
            try {
                if (Class.forName(activitySuperClassLocationInPackage).isAssignableFrom(loadedClass) && !Class.forName(activitySuperClassLocationInPackage).equals(loadedClass)) {
                    // we grab and get names for all parameters for this activity type
                    Parameter[] constructorParameters = loadedClass.getConstructors()[0].getParameters();
                    Type[] constructorParameterTypes = loadedClass.getConstructors()[0].getGenericParameterTypes();
                    List<Map<String, String>> parameters = new ArrayList<>();
                    for (int i = 1; i < constructorParameters.length; i++) {
                        // we assign to i-1 here because the first parameter is always the time, which doesn't 'count'
                        Map<String, String> paramMap = new HashMap<>();
                        paramMap.put("name", constructorParameters[i].getName());
                        Type type = constructorParameterTypes[i];
                        String typeName = type.getTypeName();
                        if (type == Double.TYPE) {
                            typeName = "java.lang.Double";
                        } else if (type == Float.TYPE) {
                            typeName = "java.lang.Float";
                        } else if (type == Integer.TYPE) {
                            typeName = "java.lang.Integer";
                        } else if (type == Long.TYPE) {
                            typeName = "java.lang.Long";
                        } else if (type == Byte.TYPE) {
                            typeName = "java.lang.Byte";
                        } else if (type == Boolean.TYPE) {
                            typeName = "java.lang.Boolean";
                        }

                        paramMap.put("type", typeName);
                        parameters.add(paramMap);
                    }
                    ActivityType newType = new ActivityType(ReflectionUtilities.getClassNameWithoutPackage(loadedClass.getName()), loadedClass, parameters);
                    activityTypesToReturn.put(ReflectionUtilities.getClassNameWithoutPackage(loadedClass.getName()), newType);
                }
            } catch (ClassNotFoundException e) {
                // if we can't load the class, we'll just move on to the next one
            }
        }

        return activityTypesToReturn;
    }
}
