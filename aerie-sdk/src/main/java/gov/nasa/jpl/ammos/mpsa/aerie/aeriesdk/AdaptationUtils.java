package gov.nasa.jpl.ammos.mpsa.aerie.aeriesdk;

import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinSDKAdaptation;
import gov.nasa.jpl.ammos.mpsa.aerie.schemas.Adaptation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.UnmodifiableIterator;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

public class AdaptationUtils {
    public static final String ADAPTATION_CLASSPATH = "gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.MerlinSDKAdaptation";
    public static final String JPL_CLASSPATH = "gov.nasa.jpl";

    // TODO: Move this into the Adaptation Runtime Service when it is complete
    public static MerlinSDKAdaptation loadAdaptation(String adaptationLocation) throws IOException, InstantiationException, IllegalAccessException {
        // Load the adaptation jar
        URL adaptationURL = new File(adaptationLocation).toURI().toURL();
        URLClassLoader loader = new URLClassLoader(new URL[]{adaptationURL},
                Thread.currentThread().getContextClassLoader());

        ClassPath classpath = ClassPath.from(loader);

        // Find all classes in the jpl package
        UnmodifiableIterator classes = classpath.getTopLevelClassesRecursive(JPL_CLASSPATH).iterator();

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
        for (Class loadedClass : pkgClasses) {
            try {
                if (Class.forName(ADAPTATION_CLASSPATH).isAssignableFrom(loadedClass)
                        && !Class.forName(ADAPTATION_CLASSPATH).equals(loadedClass)) {
                    return (MerlinSDKAdaptation) loadedClass.newInstance();
                }
            } catch (ClassNotFoundException e) {
                // Ignore this error and move on to the next class
            }
        }

        return null;
    }

}
