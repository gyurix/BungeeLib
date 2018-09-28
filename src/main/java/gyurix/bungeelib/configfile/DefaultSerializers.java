package gyurix.bungeelib.configfile;

import com.google.gson.internal.Primitives;
import gyurix.bungeelib.configfile.ConfigSerialization.ConfigOptions;
import gyurix.bungeelib.configfile.ConfigSerialization.Serializer;
import gyurix.bungeelib.configfile.ConfigSerialization.StringSerializable;
import gyurix.bungeelib.utils.ArrayUtils;
import gyurix.bungeelib.utils.BU;
import gyurix.bungeelib.utils.ClassUtils;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static gyurix.bungeelib.utils.ClassUtils.getAllFields;
import static gyurix.bungeelib.utils.ClassUtils.newInstance;

public class DefaultSerializers {
    public static void init() {
        ConfigSerialization.serializers.put(String.class, new StringSerializer());
        ConfigSerialization.serializers.put(Class.class, new ClassSerializer());
        ConfigSerialization.serializers.put(UUID.class, new UUIDSerializer());
        ConfigSerialization.serializers.put(ConfigData.class, new ConfigDataSerializer());

        NumberSerializer numberSerializer = new NumberSerializer();
        ConfigSerialization.serializers.put(Byte.class, numberSerializer);
        ConfigSerialization.serializers.put(Short.class, numberSerializer);
        ConfigSerialization.serializers.put(Integer.class, numberSerializer);
        ConfigSerialization.serializers.put(Long.class, numberSerializer);
        ConfigSerialization.serializers.put(Float.class, numberSerializer);
        ConfigSerialization.serializers.put(Double.class, numberSerializer);
        ConfigSerialization.serializers.put(Boolean.class, new BooleanSerializer());
        ConfigSerialization.serializers.put(Character.class, new CharacterSerializer());
        ConfigSerialization.serializers.put(Array.class, new ArraySerializer());
        ConfigSerialization.serializers.put(Collection.class, new CollectionSerializer());
        ConfigSerialization.serializers.put(Map.class, new MapSerializer());
        ConfigSerialization.serializers.put(Object.class, new ObjectSerializer());
        ConfigSerialization.serializers.put(Pattern.class, new PatternSerializer());
        ConfigSerialization.serializers.put(SimpleDateFormat.class, new SimpleDateFormatSerializer());

        ConfigSerialization.aliases.put(String.class, "str");
        ConfigSerialization.aliases.put(UUID.class, "uuid");
        ConfigSerialization.aliases.put(Byte.class, "b");
        ConfigSerialization.aliases.put(Short.class, "s");
        ConfigSerialization.aliases.put(Integer.class, "i");
        ConfigSerialization.aliases.put(Long.class, "l");
        ConfigSerialization.aliases.put(Float.class, "f");
        ConfigSerialization.aliases.put(Double.class, "d");
        ConfigSerialization.aliases.put(Boolean.class, "bool");
        ConfigSerialization.aliases.put(Character.class, "c");
        ConfigSerialization.aliases.put(Array.class, "[]");
        ConfigSerialization.aliases.put(Collection.class, "{}");
        ConfigSerialization.aliases.put(List.class, "{L}");
        ConfigSerialization.aliases.put(Set.class, "{S}");
        ConfigSerialization.aliases.put(LinkedHashSet.class, "{LS}");
        ConfigSerialization.aliases.put(TreeSet.class, "{TS}");
        ConfigSerialization.aliases.put(Map.class, "<>");
        ConfigSerialization.aliases.put(LinkedHashMap.class, "<L>");
        ConfigSerialization.aliases.put(TreeMap.class, "<T>");
        ConfigSerialization.aliases.put(Object.class, "?");
        ConfigSerialization.interfaceBasedClasses.put(List.class, ArrayList.class);
        ConfigSerialization.interfaceBasedClasses.put(Set.class, HashSet.class);
        ConfigSerialization.interfaceBasedClasses.put(Map.class, HashMap.class);
    }

    public static class ArraySerializer implements Serializer {
        public Object fromData(ConfigData input, Class fixClass, Type... parameterTypes) {
            Class cl = Object.class;
            Type[] types = new Type[0];
            if (parameterTypes.length >= 1) {
                if (parameterTypes[0] instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) parameterTypes[0];
                    cl = (Class) pt.getRawType();
                    types = pt.getActualTypeArguments();
                } else {
                    cl = (Class) parameterTypes[0];
                }
            }
            if (input.listData != null) {
                Object ar = Array.newInstance(cl, input.listData.size());
                int i = 0;
                for (ConfigData d : input.listData) {
                    Array.set(ar, i++, d.deserialize(cl, types));
                }
                return ar;
            } else {
                String[] sd = input.stringData.split("\\;");
                Object ar = Array.newInstance(cl, sd.length);
                int i = 0;
                for (String d : sd) {
                    Array.set(ar, i++, new ConfigData(d).deserialize(cl, types));
                }
                return ar;
            }
        }


        public ConfigData toData(Object input, Type... parameters) {
            Class cl = parameters.length >= 1 ? (Class) parameters[0] : Object.class;
            ConfigData d = new ConfigData();
            d.listData = new ArrayList<>();
            for (Object o : Arrays.asList((Object[]) input)) {
                if (o != null) {
                    d.listData.add(ConfigData.serializeObject(o, o.getClass() != cl));
                }
            }
            return d;
        }
    }

    public static class BooleanSerializer implements Serializer {
        public Object fromData(ConfigData input, Class cl, Type... parameters) {
            String s = input.stringData.toLowerCase();
            return s.equals("+") || s.equals("true") || s.equals("yes");
        }

        public ConfigData toData(Object in, Type... parameters) {
            return new ConfigData((boolean) in ? "+" : "-");
        }
    }

    public static class CharacterSerializer implements Serializer {
        public Object fromData(ConfigData input, Class cl, Type... parameters) {
            return input.stringData.charAt(0);
        }

        public ConfigData toData(Object in, Type... parameters) {
            return new ConfigData(String.valueOf(in));
        }
    }

    private static class ClassSerializer implements Serializer {
        public Object fromData(ConfigData input, Class cl, Type... parameters) {
            try {
                return Class.forName(input.stringData);
            } catch (ClassNotFoundException e) {
                BU.error(BU.cs, e, "BungeeLib", "gyurix");
            }
            return null;
        }

        public ConfigData toData(Object input, Type... parameters) {
            return new ConfigData(((Class) input).getName());
        }
    }

    public static class CollectionSerializer implements Serializer {
        public Object fromData(ConfigData input, Class fixClass, Type... parameterTypes) {
            try {
                Collection col = (Collection) fixClass.newInstance();
                Class cl;
                Type[] types;
                ParameterizedType pt;
                cl = Object.class;
                types = new Type[0];
                if (parameterTypes.length >= 1) {
                    if (parameterTypes[0] instanceof ParameterizedType) {
                        pt = (ParameterizedType) parameterTypes[0];
                        cl = (Class) pt.getRawType();
                        types = pt.getActualTypeArguments();
                    } else {
                        cl = (Class) parameterTypes[0];
                    }
                }
                if (input.listData != null) {
                    for (ConfigData d : input.listData) {
                        col.add(d.deserialize(cl, types));
                    }
                } else {
                    for (String s : input.stringData.split("[;,] *")) {
                        col.add(new ConfigData(s).deserialize(cl, types));
                    }
                }
                return col;
            } catch (Throwable e) {
                BU.error(BU.cs, e, "BungeeLib", "gyurix");
            }
            return null;
        }


        public ConfigData toData(Object input, Type... parameters) {
            Type[] types = new Type[0];
            Class cl = Object.class;
            if (parameters.length >= 1) {
                if (parameters[0] instanceof ParameterizedType) {
                    ParameterizedType key = (ParameterizedType) parameters[0];
                    types = key.getActualTypeArguments();
                    cl = (Class) key.getRawType();
                } else {
                    cl = (Class) parameters[0];
                }
            }
            if (((Collection) input).isEmpty())
                return new ConfigData("");
            ConfigData d = new ConfigData();
            d.listData = new ArrayList<>();
            for (Object o : (Collection) input) {
                d.listData.add(ConfigData.serializeObject(o, o.getClass() != cl, types));
            }
            return d;
        }
    }

    public static class ConfigDataSerializer implements Serializer {
        public Object fromData(ConfigData data, Class cl, Type... type) {
            return data;
        }

        public ConfigData toData(Object data, Type... type) {
            return (ConfigData) data;
        }
    }

    public static class MapSerializer implements Serializer {
        public Object fromData(ConfigData input, Class fixClass, Type... parameterTypes) {
            try {
                Map map;
                if (fixClass == EnumMap.class)
                    map = new EnumMap((Class) parameterTypes[0]);
                else
                    map = (Map) fixClass.newInstance();
                Class keyClass;
                Type[] keyTypes;
                Class valueClass;
                Type[] valueTypes;
                ParameterizedType pt;
                if (input.mapData != null) {
                    keyClass = Object.class;
                    keyTypes = new Type[0];
                    if (parameterTypes.length >= 1) {
                        if (parameterTypes[0] instanceof ParameterizedType) {
                            pt = (ParameterizedType) parameterTypes[0];
                            keyClass = (Class) pt.getRawType();
                            keyTypes = pt.getActualTypeArguments();
                        } else {
                            keyClass = (Class) parameterTypes[0];
                        }
                    }
                    valueClass = Object.class;
                    valueTypes = new Type[0];
                    if (parameterTypes.length >= 2) {
                        if (parameterTypes[1] instanceof ParameterizedType) {
                            pt = (ParameterizedType) parameterTypes[1];
                            valueClass = (Class) pt.getRawType();
                            valueTypes = pt.getActualTypeArguments();
                        } else {
                            valueClass = (Class) parameterTypes[1];
                        }
                    }
                    for (Entry<ConfigData, ConfigData> e : input.mapData.entrySet()) {
                        try {
                            map.put(e.getKey().deserialize(keyClass, keyTypes), e.getValue().deserialize(valueClass, valueTypes));
                        } catch (Throwable err) {
                            System.err.println("Map element deserialization error:\n" +
                                    "Key = " + e.getKey() + "; Value = " + e.getValue());
                            BU.error(BU.cs, err, "BungeeLib", "gyurix");
                        }
                    }
                }
                return map;
            } catch (Throwable e) {
                BU.error(BU.cs, e, "BungeeLib", "gyurix");
            }
            return null;
        }


        public ConfigData toData(Object input, Type... parameters) {
            if (((Map) input).isEmpty())
                return new ConfigData();
            Class keyClass = Object.class;
            Class valueClass = Object.class;
            Type[] keyTypes = new Type[0];
            Type[] valueTypes = new Type[0];
            if (parameters.length >= 1) {
                if (parameters[0] instanceof ParameterizedType) {
                    ParameterizedType key = (ParameterizedType) parameters[0];
                    keyTypes = key.getActualTypeArguments();
                    keyClass = (Class) key.getRawType();
                } else {
                    keyClass = (Class) parameters[0];
                }
                if (parameters.length >= 2) {
                    if (parameters[1] instanceof ParameterizedType) {
                        ParameterizedType value = (ParameterizedType) parameters[1];
                        valueTypes = value.getActualTypeArguments();
                        valueClass = (Class) value.getRawType();
                    } else {
                        valueClass = (Class) parameters[1];
                    }
                }
            }
            ConfigData d = new ConfigData();
            d.mapData = new LinkedHashMap();
            for (Entry<?, ?> e : ((Map<?, ?>) input).entrySet()) {
                Object key = e.getKey();
                Object value = e.getValue();
                if (key != null && value != null)
                    d.mapData.put(ConfigData.serializeObject(key, key.getClass() != keyClass, keyTypes),
                            ConfigData.serializeObject(value, value.getClass() != valueClass, valueTypes));
            }
            return d;
        }
    }

    public static class NumberSerializer implements Serializer {
        public static final HashMap<Class, Method> methods = new HashMap();

        static {
            try {
                methods.put(Short.class, Short.class.getMethod("decode", String.class));
                methods.put(Integer.class, Integer.class.getMethod("decode", String.class));
                methods.put(Long.class, Long.class.getMethod("decode", String.class));
                methods.put(Float.class, Float.class.getMethod("valueOf", String.class));
                methods.put(Double.class, Double.class.getMethod("valueOf", String.class));
                methods.put(Byte.class, Byte.class.getMethod("valueOf", String.class));
            } catch (Throwable e) {
                BU.error(BU.cs, e, "BungeeLib", "gyurix");
            }
        }

        public Object fromData(ConfigData input, Class fixClass, Type... parameters) {
            Method m = methods.get(Primitives.wrap(fixClass));
            try {
                String s = input.stringData.replace(" ", "");
                return m.invoke(null, s.isEmpty() ? "0" : s);
            } catch (Throwable e) {
                System.out.println("INVALID NUMBER: " + input);
                BU.error(BU.cs, e, "BungeeLib", "gyurix");
                try {
                    return m.invoke(null, "0");
                } catch (Throwable e2) {
                    System.out.println("Not a number class: " + fixClass.getSimpleName());
                    BU.error(BU.cs, e, "BungeeLib", "gyurix");
                }
            }
            return null;
        }

        public ConfigData toData(Object input, Type... parameters) {
            return new ConfigData(input.toString());
        }
    }

    public static class ObjectSerializer implements Serializer {
        public Object fromData(ConfigData input, Class fixClass, Type... parameters) {
            try {
                if (fixClass.isEnum()) {
                    if (input.stringData == null || input.stringData.equals(""))
                        return null;
                    for (Object en : fixClass.getEnumConstants()) {
                        if (en.toString().equals(input.stringData))
                            return en;
                    }
                    return null;
                }
                if (ArrayUtils.contains(fixClass.getInterfaces(), StringSerializable.class) || fixClass == BigDecimal.class || fixClass == BigInteger.class) {
                    if (input.stringData == null || input.stringData.equals(""))
                        return null;
                    return fixClass.getConstructor(String.class).newInstance(input.stringData);
                }
            } catch (Throwable e) {
                System.err.println("Error on deserializing \"" + input.stringData + "\" to a " + fixClass.getName() + " object.");
                BU.error(BU.cs, e, "BungeeLib", "gyurix");
                return null;
            }
            Object obj = newInstance(fixClass);
            if (input.mapData == null)
                return obj;
            for (Field f : getAllFields(fixClass)) {
                f.setAccessible(true);
                try {
                    String fn = f.getName();
                    ConfigData d = input.mapData.get(new ConfigData(fn));
                    Class cl = Primitives.wrap(f.getType());
                    if (d != null) {
                        Type[] types = f.getGenericType() instanceof ParameterizedType ? ((ParameterizedType) f.getGenericType()).getActualTypeArguments() : cl.isArray() ? new Type[]{cl.getComponentType()} : new Type[0];
                        Object out = d.deserialize(ConfigSerialization.getNotInterfaceClass(cl), types);
                        if (out != null)
                            f.set(obj, out);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            try {
                if (ArrayUtils.contains(fixClass.getInterfaces(), PostLoadable.class)) {
                    ((PostLoadable) obj).postLoad();
                }
            } catch (Throwable e) {
                System.err.println("Error on post loading " + fixClass.getName() + " object.");
                e.printStackTrace();
            }
            return obj;
        }

        public ConfigData toData(Object obj, Type... parameters) {
            Class c = Primitives.wrap(obj.getClass());
            if (c.isEnum() || ArrayUtils.contains(c.getInterfaces(), StringSerializable.class) || c == BigDecimal.class || c == BigInteger.class) {
                return new ConfigData(obj.toString());
            }
            ConfigOptions dfOptions = (ConfigOptions) c.getAnnotation(ConfigOptions.class);
            String dfValue = dfOptions == null ? "null" : dfOptions.defaultValue();
            boolean dfSerialize = dfOptions == null || dfOptions.serialize();
            String comment = dfOptions == null ? "" : dfOptions.comment();
            ConfigData out = new ConfigData();
            if (!comment.isEmpty())
                out.comment = comment;
            out.mapData = new LinkedHashMap();
            for (Field f : getAllFields(c)) {
                try {
                    String dffValue = dfValue;
                    boolean serialize = dfSerialize;
                    comment = "";
                    ConfigOptions options = f.getAnnotation(ConfigOptions.class);
                    if (options != null) {
                        serialize = options.serialize();
                        dffValue = options.defaultValue();
                        comment = options.comment();
                    }
                    if (!serialize)
                        continue;
                    Object o = f.get(obj);
                    if (o != null && !o.toString().matches(dffValue)) {
                        String fn = f.getName();
                        String cn = ConfigSerialization.calculateClassName(Primitives.wrap(f.getType()), o.getClass());
                        Type t = f.getGenericType();
                        out.mapData.put(new ConfigData(fn, comment), ConfigData.serializeObject(o, !cn.isEmpty(),
                                t instanceof ParameterizedType ?
                                        ((ParameterizedType) t).getActualTypeArguments() :
                                        ((Class) t).isArray() ?
                                                new Type[]{((Class) t).getComponentType()} :
                                                new Type[0]));
                    }
                } catch (Throwable e) {
                    BU.error(BU.cs, e, "BungeeLib", "gyurix");
                }
            }
            return out;
        }
    }

    public static class PatternSerializer implements Serializer {
        public Object fromData(ConfigData data, Class paramClass, Type... paramVarArgs) {
            return Pattern.compile(data.stringData);
        }

        public ConfigData toData(Object pt, Type... paramVarArgs) {
            return new ConfigData(((Pattern) pt).pattern());
        }
    }

    public static class SimpleDateFormatSerializer implements Serializer {
        public static final Field patternF = ClassUtils.getField(SimpleDateFormat.class, "pattern");

        public Object fromData(ConfigData input, Class cl, Type... parameters) {
            return new SimpleDateFormat(input.stringData);
        }

        public ConfigData toData(Object input, Type... parameters) {
            try {
                return new ConfigData((String) patternF.get(input));
            } catch (Throwable e) {
                BU.error(BU.cs, e, "BungeeLib", "gyurix");
            }
            return new ConfigData();
        }
    }

    public static class StringSerializer implements Serializer {
        public Object fromData(ConfigData input, Class cl, Type... parameters) {
            return input.stringData;
        }

        public ConfigData toData(Object input, Type... parameters) {
            return new ConfigData((String) input);
        }
    }

    public static class UUIDSerializer implements Serializer {
        public Object fromData(ConfigData input, Class cl, Type... parameters) {
            return UUID.fromString(input.stringData);
        }

        public ConfigData toData(Object input, Type... parameters) {
            return new ConfigData(input.toString());
        }
    }

}