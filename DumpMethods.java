import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class DumpMethods {
    public static void main(String[] args) throws Exception {
        URL url = new java.io.File(args[0]).toURI().toURL();
        try (URLClassLoader cl = new URLClassLoader(new URL[]{url}, DumpMethods.class.getClassLoader())) {
            Class<?> clazz = cl.loadClass("net.caffeinemc.mods.sodium.client.world.LevelSlice");
            for (Method m : clazz.getDeclaredMethods()) {
                System.out.println(m);
            }
        }
    }
}
