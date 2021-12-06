package gov.nasa.jpl.aerie.scheduler;

import gov.nasa.jpl.aerie.merlin.driver.MissionModel;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarClassLoader {

  public static Collection<Problem> loadProblemsFromJar(String pathToJar, MissionModel<?> missionModel)
  throws IOException, ClassNotFoundException, InvocationTargetException, InstantiationException
  {
    try {
      return load(pathToJar, Problem.class, missionModel);
    }catch(NoSuchMethodException e) {
      e.printStackTrace();
      throw new IllegalArgumentException(
          "Problem class found in jar but has not constructor with one parameter of type MissionModel");
    } catch (IllegalAccessException e){
      e.printStackTrace();
      throw new IllegalArgumentException(
          "Could not create an instance of the Problem: maybe constructor is private ? ");
    }
  }

  /** Explores a given jar and looks for all classes implementing the given class. Creates one instance of each of them and returns them as a collection.
   *  Warning: user should pay attention when building the jar so the paths correspond to package names
   * @param pathToJar the path to the jar
   * @param clazz the class we are looking implementations of
   * @param <C>
   * @return one instance for each class found implementing the interface
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  private static <C> Collection<C> load(String pathToJar, Class<C> clazz, Object... initArgs)
  throws IOException, ClassNotFoundException, InvocationTargetException, InstantiationException,
         IllegalAccessException, NoSuchMethodException
  {
    final JarFile jarFile = new JarFile(pathToJar);
    final Enumeration<JarEntry> e = jarFile.entries();

    final var ret = new ArrayList<C>();

    final URL[] urls = {new URL("jar:file:" + pathToJar + "!/")};
    final URLClassLoader cl = URLClassLoader.newInstance(urls);

    Class[] parameterType = new Class[initArgs.length];
    int i = 0;
    for(var args:initArgs){
      parameterType[i] = args.getClass();
      i++;
    }

    while (e.hasMoreElements()) {
      final JarEntry je = e.nextElement();
      if (je.isDirectory() || !je.getName().endsWith(".class")) {
        continue;
      }
      String className = je.getName().substring(0, je.getName().length() - 6);
      className = className.replace('/', '.');
      final Class<?> c = cl.loadClass(className);

      if(clazz.isAssignableFrom(c)){
        //extends the right thing
        final C a = clazz.cast(c.getDeclaredConstructor(parameterType).newInstance(initArgs));
        ret.add(a);
      }
    }
    return ret;
  }


}
