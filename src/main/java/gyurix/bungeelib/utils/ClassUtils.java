package gyurix.bungeelib.utils;

import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ClassUtils {
    public static final ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
    private static final HashMap<Class, Field[]> allFieldsCache = new HashMap<>();
    private static final HashMap<Class, Class[]> allInterfacesCache = new HashMap<>();
    private static final Field fieldModifiers;
    private static final Constructor objCon;

    static {
        Field fm = null;
        Constructor con = null;
        try {
            fm = Field.class.getDeclaredField("modifiers");
            fm.setAccessible(true);
            con = Object.class.getDeclaredConstructor();
        } catch (Throwable e) {
        }
        fieldModifiers = fm;
        objCon = con;
    }

    private static void addInterfaces(Class cl, HashSet<Class> set) {
        while (cl != null) {
            for (Class i : cl.getInterfaces()) {
                set.add(i);
                addInterfaces(i, set);
            }
            cl = cl.getSuperclass();
        }
    }

    public static Field[] getAllFields(Class c) {
        Field[] fs = allFieldsCache.get(c);
        if (fs != null)
            return fs;
        ArrayList<Field> fields = new ArrayList<>();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    fieldModifiers.setInt(f, fieldModifiers.getInt(f) & -17);
                    fields.add(f);
                } catch (Throwable e) {
                }
            }
            c = c.getSuperclass();
        }
        Field[] out = new Field[fields.size()];
        fields.toArray(out);
        allFieldsCache.put(c, out);
        return out;
    }

    public static Class[] getAllInterfaces(Class cl) {
        HashSet<Class> set = new HashSet<>();
        addInterfaces(cl, set);
        Class[] out = new Class[set.size()];
        set.toArray(out);
        allInterfacesCache.put(cl, out);
        return out;
    }

    public static Field getField(Class clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            fieldModifiers.setInt(f, fieldModifiers.getInt(f) & -17);
            return f;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Constructs a new instance of the given class
     *
     * @param cl - The class
     */
    public static Object newInstance(Class cl) {
        try {
            try {
                return cl.newInstance();
            } catch (Throwable err) {
                return rf.newConstructorForSerialization(cl, objCon).newInstance();
            }
        } catch (Throwable e) {
            BU.error(BU.cs, e, "SpigotLib", "gyurix");
        }
        return null;
    }

    public static void unloadClass(Class cl) {

    }
}
