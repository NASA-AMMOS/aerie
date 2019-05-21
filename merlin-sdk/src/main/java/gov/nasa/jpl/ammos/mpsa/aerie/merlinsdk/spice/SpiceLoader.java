package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.spice;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import spice.basic.CSPICE;
import spice.basic.SpiceErrorException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

// Inspired by:
// https://stackoverflow.com/questions/12036607/bundle-native-dependencies-in-runnable-jar-with-maven

public class SpiceLoader {

    public static void loadSpice() {
        String library = "JNISpice";
        try {
            System.load(saveLibrary(library));
        } catch (IOException e) {
            System.loadLibrary(library);
        }
    }

    public static String getLibraryNameByOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("win")) {
            return "JNISpice.dll";
        } else if (osName.startsWith("linux")) {
            return "libJNISpice.so";
        } else if (osName.startsWith("mac")) {
            return "libJNISpice.jnilib";
        } else {
            throw new UnsupportedOperationException("Platform " + osName + " is not supported by JNI Spice.");
        }
    }

    private static String saveLibrary(String library) throws IOException {
        InputStream in = null;
        OutputStream out = null;

        try {
            String libraryName = getLibraryNameByOS();
            in = SpiceLoader.class.getResourceAsStream("/" + libraryName);
            String tmpDirName = System.getProperty("java.io.tmpdir");
            File tmpDir = new File(tmpDirName);
            if (!tmpDir.exists()) {
                tmpDir.mkdir();
            }
            File file = File.createTempFile(library + "-", ".tmp", tmpDir);
            // Clean up the file when exiting
            file.deleteOnExit();
            out = new FileOutputStream(file);

            int cnt;
            byte buf[] = new byte[16 * 1024];
            // copy until done.
            while ((cnt = in.read(buf)) >= 1) {
                out.write(buf, 0, cnt);
            }
            return file.getAbsolutePath();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
