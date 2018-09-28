package gyurix.bungeelib.utils;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * Created by GyuriX on 2016. 07. 14..
 */
public class ArrayUtils {
    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static <T> boolean contains(T[] in, T element) {
        for (T o : in) {
            if (Objects.equals(o, element))
                return true;
        }
        return false;
    }

    public static <T> int indexOf(T[] in, T element) {
        int len = in.length;
        for (int i = 0; i < len; i++) {
            if (Objects.equals(in[i], element))
                return i;
        }
        return -1;
    }

    public static <T> int lastIndexOf(T[] in, T element) {
        for (int i = in.length - 1; i > -1; i--) {
            if (Objects.equals(in[i], element))
                return i;
        }
        return -1;
    }

    public static <T> T[] subArray(T[] in, int from, int to) {
        if (in == null)
            return null;
        if (to > in.length)
            to = in.length;
        if (from < 0)
            from = 0;
        if (to <= from)
            return (T[]) Array.newInstance(in.getClass().getComponentType(), 0);
        T[] out = (T[]) Array.newInstance(in.getClass().getComponentType(), to - from);
        System.arraycopy(in, from, out, 0, to - from);
        return out;
    }
}
