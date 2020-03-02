package gyurix.bungeelib.utils;

import java.util.Iterator;

import static java.lang.Integer.MAX_VALUE;

public class StringUtils {
    public static String join(Object[] data, char sep) {
        return join(data, sep, 0, MAX_VALUE);
    }

    public static String join(Object[] data, char sep, int from, int to) {
        if (data == null || data.length == 0 || from >= data.length || to < from)
            return "";
        StringBuilder sb = new StringBuilder();
        if (from < 0)
            from = 0;
        if (to > data.length)
            to = data.length;
        for (int i = from; i < to; i++)
            sb.append(sep).append(data[i]);
        return sb.length() == 0 ? "" : sb.substring(1);
    }

    public static String join(Object[] data, String sep) {
        return join(data, sep, 0, MAX_VALUE);
    }

    public static String join(Object[] data, String sep, int from, int to) {
        if (data == null || data.length == 0 || from >= data.length || to < from)
            return "";
        StringBuilder sb = new StringBuilder();
        if (from < 0)
            from = 0;
        if (to > data.length)
            to = data.length;
        for (int i = from; i < to; i++)
            sb.append(sep).append(data[i]);
        return sb.length() == 0 ? "" : sb.substring(sep.length());
    }

    public static String join(Iterator it, char sep) {
        return join(it, sep, 0, MAX_VALUE);
    }

    public static String join(Iterator it, char sep, int from, int to) {
        if (it == null || to < from)
            return "";
        int i = 0;
        StringBuilder sb = new StringBuilder();
        while (i < from) {
            if (!it.hasNext())
                return "";
            it.next();
            ++i;
        }
        while (i < to) {
            if (!it.hasNext())
                return sb.length() == 0 ? "" : sb.substring(1);
            sb.append(sep).append(it.next());
        }
        return sb.length() == 0 ? "" : sb.substring(1);
    }

    public static String join(Iterator it, String sep) {
        return join(it, sep, 0, MAX_VALUE);
    }

    public static String join(Iterator it, String sep, int from, int to) {
        if (it == null || to < from)
            return "";
        int i = 0;
        StringBuilder sb = new StringBuilder();
        while (i < from) {
            if (!it.hasNext())
                return "";
            it.next();
            ++i;
        }
        while (i < to) {
            if (!it.hasNext())
                return sb.length() == 0 ? "" : sb.substring(sep.length());
            sb.append(sep).append(it.next());
        }
        return sb.length() == 0 ? "" : sb.substring(sep.length());
    }

    public static String join(Iterable it, char sep) {
        return join(it, sep, 0, MAX_VALUE);
    }

    public static String join(Iterable data, char sep, int from, int to) {
        return data == null ? "" : join(data.iterator(), sep, from, to);
    }

    public static String join(Iterable it, String sep) {
        return join(it, sep, 0, MAX_VALUE);
    }

    public static String join(Iterable data, String sep, int from, int to) {
        return data == null ? "" : join(data.iterator(), sep, from, to);
    }
}
