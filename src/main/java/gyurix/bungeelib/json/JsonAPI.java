package gyurix.bungeelib.json;

import com.google.common.primitives.Primitives;
import gyurix.bungeelib.configfile.ConfigSerialization.StringSerializable;
import gyurix.bungeelib.utils.Reflection;

import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;

import static gyurix.bungeelib.utils.BU.cs;
import static gyurix.bungeelib.utils.BU.error;
import static gyurix.bungeelib.utils.Reflection.getField;

public class JsonAPI {
    public static final Type[] emptyTypeArray = new Type[0];

    public static int HextoDec(char c) {
        return c >= '0' && c <= '9' ? c - 48 : c >= 'A' && c <= 'F' ? c - 65 : c - 97;
    }

    private static Object deserialize(Object parent, StringReader in, Class cl, Type... params) throws Throwable {
        cl = Primitives.wrap(cl);
        char c = '-';
        if (in.hasNext())
            c = in.next();
        else in.id++;
        if (Map.class.isAssignableFrom(cl)) {
            if (c != '{') {
                throw new Throwable("JSONAPI: Error on deserializing Json " + new String(in.str) + ", expected {, found " + c + " (character id: " + in.id + ')');
            }
            Class keyClass = (Class) (params[0] instanceof ParameterizedType ? ((ParameterizedType) params[0]).getRawType() : params[0]);
            Type[] keyType = params[0] instanceof ParameterizedType ? ((ParameterizedType) params[0]).getActualTypeArguments() : emptyTypeArray;
            Class valueClass = (Class) (params[1] instanceof ParameterizedType ? ((ParameterizedType) params[1]).getRawType() : params[1]);
            Type[] valueType = params[1] instanceof ParameterizedType ? ((ParameterizedType) params[1]).getActualTypeArguments() : emptyTypeArray;
            Map map = cl == EnumMap.class ? new EnumMap<>(keyClass) : (Map) cl.newInstance();
            if (in.next() == '}')
                return map;
            else
                in.id -= 2;
            while (in.next() != '}') {
                Object key = deserialize(map, in, keyClass, keyType);
                if (in.next() != ':')
                    throw new Throwable("JSONAPI: Error on deserializing Json " + new String(in.str) + ", expected :, found " + in.last() + " (character id: " + (in.id - 1) + ')');
                map.put(key, deserialize(map, in, valueClass, valueType));
            }
            return map;
        } else if (Collection.class.isAssignableFrom(cl)) {
            if (c != '[') {
                throw new Throwable("JSONAPI: Error on deserializing Json " + new String(in.str) + ", expected {, found " + c + " (character id: " + in.id + ')');
            }
            Class dataClass = (Class) (params[0] instanceof ParameterizedType ? ((ParameterizedType) params[0]).getRawType() : params[0]);
            Type[] dataType = params[0] instanceof ParameterizedType ? ((ParameterizedType) params[0]).getActualTypeArguments() : emptyTypeArray;
            Collection col = (Collection) cl.newInstance();
            if (in.next() == ']')
                return col;
            else
                in.id -= 2;
            while (in.next() != ']') {
                col.add(deserialize(col, in, dataClass, dataType));
            }
            return col;
        } else if (cl.isArray()) {
            if (c != '[') {
                throw new Throwable("JSONAPI: Error on deserializing Json " + new String(in.str) + ", expected {, found " + c + " (character id: " + in.id + ')');
            }
            Class dataClass = cl.getComponentType();
            ArrayList col = new ArrayList();
            if (in.next() == ']') {
                return Array.newInstance(dataClass, 0);
            } else
                in.id -= 2;
            while (in.next() != ']') {
                col.add(deserialize(null, in, dataClass));
            }
            Object[] out = (Object[]) Array.newInstance(dataClass, col.size());
            return col.toArray(out);
        } else if (c == '{') {
            Object obj = Reflection.newInstance(cl);
            if (in.next() == '}')
                return obj;
            else
                in.id -= 2;
            while (in.next() != '}') {
                String fn = readString(in);
                if (in.next() != ':')
                    throw new Throwable("JSONAPI: Error on deserializing Json " + new String(in.str) + ", expected :, found " + in.last() + " (character id: " + (in.id - 1) + ')');
                try {
                    Field f = getField(cl, fn);
                    Type gt = f.getGenericType();
                    f.set(obj, deserialize(obj, in, f.getType(), gt instanceof ParameterizedType ? ((ParameterizedType) gt).getActualTypeArguments() : emptyTypeArray));
                } catch (Throwable e) {
                    cs.sendMessage("§6[§eJSONAPI§6] §cField §f" + fn + "§e is declared in json, but it is missing from class §e" + cl.getName() + "§c.");
                    error(cs, e, "SpigotLib", "gyurix");
                }
            }
            try {
                Field f = getField(cl, "parent");
                f.set(obj, parent);
            } catch (Throwable e) {
            }
            try {
                Field f = getField(cl, "self");
                f.set(obj, obj);
            } catch (Throwable e) {
            }
            try {
                Field f = getField(cl, "instance");
                f.set(obj, obj);
            } catch (Throwable e) {
            }
            return obj;
        } else {
            in.id--;
            String str = readString(in);
            try {
                return Reflection.getConstructor(cl, String.class).newInstance(str);
            } catch (Throwable e) {
            }
            try {
                return Reflection.getMethod(cl, "valueOf", String.class).invoke(null, str);
            } catch (Throwable e) {
            }
            try {
                Method m = Reflection.getMethod(cl, "fromString", String.class);
                if (cl == UUID.class && !str.contains("-"))
                    str = str.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                return m.invoke(null, str);

            } catch (Throwable e) {
                error(cs, e, "SpigotLib", "gyurix");
            }
            throw new Throwable("JSONAPI: Error on deserializing Json " + new String(in.str) + ", expected " + cl.getName() + ", found String.");
        }
    }

    public static <T> T deserialize(String json, Class<T> cl, Type... params) {
        StringReader sr = new StringReader(json);
        try {
            return (T) deserialize(null, sr, cl, params);
        } catch (Throwable e) {
            cs.sendMessage("§cFailed to deserialize JSON §e" + json + "§c to class §e" + cl.getName());
            error(cs, e, "SpigotLib", "gyurix");
            return null;
        }
    }

    public static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String readString(StringReader in) {
        int start = in.id;
        int end = -1;
        boolean esc = false;
        boolean stresc = false;
        while (in.hasNext()) {
            char c = in.next();
            if (esc)
                esc = false;
            else if (c == '\\')
                esc = true;
            else if (c == '\"') {
                if (stresc) {
                    end = in.id - 1;
                    break;
                } else {
                    stresc = true;
                    start = in.id;
                }
            } else if (!stresc && (c == ']' || c == '}' || c == ',' || c == ':')) {
                in.id--;
                break;
            }
        }
        if (end == -1)
            end = in.id;
        return unescape(new String(in.str, start, end - start));
    }

    private static void serialize(StringBuilder sb, Object o) {
        if (o == null) {
            sb.append("null");
            return;
        }
        Class cl = o.getClass();
        if (o instanceof String || o instanceof UUID || o.getClass().isEnum() || o instanceof StringSerializable) {
            sb.append('\"').append(escape(o.toString())).append('"');
        } else if (o instanceof Boolean || o instanceof Byte || o instanceof Short || o instanceof Integer || o instanceof Long || o instanceof Float || o instanceof Double) {
            sb.append(o);
        } else if (o instanceof Iterable || cl.isArray()) {
            sb.append('[');
            if (cl.isArray()) {
                int max = Array.getLength(o);
                for (int i = 0; i < max; i++) {
                    serialize(sb, Array.get(o, i));
                    sb.append(',');
                }
            } else {
                for (Object obj : (Iterable) o) {
                    serialize(sb, obj);
                    sb.append(',');
                }
            }
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.setCharAt(sb.length() - 1, ']');
            } else {
                sb.append(']');
            }
        } else if (o instanceof Map) {
            sb.append('{');
            for (Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                String key = String.valueOf(e.getKey());
                sb.append('\"').append(escape(key)).append("\":");
                serialize(sb, e.getValue());
                sb.append(',');
            }
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.setCharAt(sb.length() - 1, '}');
            } else {
                sb.append('}');
            }
        } else {
            sb.append('{');
            if (cl.getName().startsWith("java.")) {
                sb.append("Class ").append(cl.getName()).append(" shouldn't be serialized}");
                return;
            }
            for (Field f : cl.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    JsonSettings settings = f.getAnnotation(JsonSettings.class);
                    String fn = f.getName();
                    boolean serialize = !(fn.equals("self") || fn.equals("parent") || fn.equals("instance"));
                    if (settings != null) {
                        serialize = settings.serialize();
                    }
                    Object fo = f.get(o);
                    if (!serialize || fo == null)
                        continue;
                    sb.append('\"').append(escape(fn)).append("\":");
                    serialize(sb, fo);
                    sb.append(',');
                } catch (Throwable e) {
                    cs.sendMessage("§eJsonAPI:§c Error on serializing §e" + f.getName() + "§c field in §e" + o.getClass().getName() + "§c class. Current JSON:\n§f" + sb);
                }
            }
            if (sb.charAt(sb.length() - 1) == ',') {
                sb.setCharAt(sb.length() - 1, '}');
            } else {
                sb.append('}');
            }
        }
    }

    public static String serialize(Object o) {
        StringBuilder sb = new StringBuilder();
        try {
            serialize(sb, o);
            return sb.toString();
        } catch (Throwable e) {
            System.err.println("JsonAPI: Error on serializing " + o.getClass().getName() + " object.");
            e.printStackTrace();
            return "{}";
        }
    }

    public static String unescape(String s) {
        boolean esc = false;
        int utf = -1;
        int utfc = -1;
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (esc) {
                switch (c) {
                    case 'b':
                        out.append('\b');
                        break;
                    case 'f':
                        out.append('\f');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    case 'r':
                        out.append('\r');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'u':
                        utf = 0;
                        utfc = 0;
                        break;
                    default:
                        out.append(c);
                }
                esc = false;
                continue;
            }
            if (utf >= 0) {
                utf = (utf << 4) + HextoDec(c);
                if (++utfc != 4) continue;
                out.append((char) utf);
                utf = -1;
                utfc = -1;
                continue;
            }
            if (c == '\\') {
                esc = true;
                continue;
            }
            out.append(c);
        }
        return out.toString();
    }
}