package gyurix.bungeelib.configfile;

import com.google.common.primitives.Primitives;
import gyurix.bungeelib.configfile.ConfigSerialization.Serializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class ConfigData {
    public String comment;
    public ArrayList<ConfigData> listData;
    public LinkedHashMap<ConfigData, ConfigData> mapData;
    public Object objectData;
    public String stringData;
    public Type[] types;

    public ConfigData() {
    }

    public ConfigData(String stringData) {
        this.stringData = stringData;
    }

    public ConfigData(Object obj) {
        objectData = obj;
    }

    public ConfigData(String stringData, String comment) {
        this.stringData = stringData;
        if (comment != null && !comment.isEmpty())
            this.comment = comment;
    }

    public static String escape(String in) {
        StringBuilder out = new StringBuilder();
        String escSpace = "\n:->";
        char prev = '\n';
        for (char c : in.toCharArray()) {
            switch (c) {
                case ' ':
                    out.append(escSpace.contains(String.valueOf(prev)) ? "\\" + c : c);
                    break;
                case '‼':
                    if (prev == '\\')
                        out.deleteCharAt(out.length() - 1);
                    out.append('‼');
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\n':
                    out.append(escSpace.contains(String.valueOf(prev)) ? "\\n" : '\n');
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                default:
                    out.append(c);
            }
            prev = c;
        }
        if (prev == '\n' && out.length() != 0) {
            out.setCharAt(out.length() - 1, '\\');
            out.append('n');
        }
        return out.toString();
    }

    public static ConfigData serializeObject(Object obj, boolean className, Type... parameters) {
        if (obj == null) {
            return null;
        }
        Class c = Primitives.wrap(obj.getClass());
        if (c.isArray()) {
            className = true;
            parameters = new Type[]{c.getComponentType()};
        }
        Serializer s = ConfigSerialization.getSerializer(c);
        ConfigData cd = s.toData(obj, parameters);
        if (cd.stringData != null && cd.stringData.startsWith("‼"))
            cd.stringData = '\\' + cd.stringData;
        if (className) {
            String prefix = '‼' + ConfigSerialization.getAlias(obj.getClass());
            for (Type t : parameters) {
                prefix = prefix + '-' + ConfigSerialization.getAlias((Class) t);
            }
            prefix += '‼';
            cd.stringData = prefix + cd.stringData;
        }
        return cd;
    }

    public static ConfigData serializeObject(Object obj, Type... parameters) {
        return serializeObject(obj, false, parameters);
    }

    public static String unescape(String in) {
        StringBuilder out = new StringBuilder(in.length());
        String uchars = "0123456789abcdef0123456789ABCDEF";
        boolean escape = false;
        int ucode = -1;
        for (char c : in.toCharArray()) {
            if (ucode != -1) {
                int id = uchars.indexOf(c) % 16;
                if (id == -1) {
                    out.append((char) ucode);
                    ucode = -1;
                } else {
                    ucode = ucode * 16 + id;
                    continue;
                }
            }
            if (escape) {
                switch (c) {
                    case 'u':
                        ucode = 0;
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
                    case 'b':
                        out.append('\b');
                        break;
                    case ' ':
                    case '-':
                    case '>':
                    case '\\':
                        out.append(c);
                }
                escape = false;
            } else if (!(escape = c == '\\')) {
                out.append(c);
            }
        }
        if (ucode != -1)
            out.append((char) ucode);
        return out.toString().replaceAll("\n +#", "\n#");
    }

    public <T> T deserialize(Class<T> c, Type... types) {
        this.types = types;
        if (objectData != null)
            return (T) objectData;
        String str = stringData == null ? "" : stringData;

        if (str.startsWith("‼")) {
            str = str.substring(1);
            int id = str.indexOf('‼');
            if (id != -1) {
                str = str.substring(0, id);
                String[] classNames = str.split("-");
                c = ConfigSerialization.realClass(classNames[0]);
                types = new Type[classNames.length - 1];
                for (int i = 1; i < classNames.length; i++) {
                    types[i - 1] = ConfigSerialization.realClass(classNames[i]);
                }
                stringData = stringData.substring(id + 2);
                Serializer ser = ConfigSerialization.getSerializer(c);
                objectData = ser.fromData(this, c, types);
            }
        } else {
            Serializer ser = ConfigSerialization.getSerializer(c);
            objectData = ser.fromData(this, c, types);
        }
        stringData = null;
        mapData = null;
        listData = null;
        return (T) objectData;
    }

    public int hashCode() {
        return stringData == null ? objectData == null ? listData == null ? mapData == null ? 0 :
                mapData.hashCode() : listData.hashCode() : objectData.hashCode() : stringData.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof ConfigData && ((ConfigData) obj).stringData.equals(stringData);
    }

    public String toString() {
        ConfigData other = this;
        while (true) {
            StringBuilder out = new StringBuilder();
            if (other.objectData != null) {
                other = serializeObject(objectData, types);
                continue;
            }
            if (other.stringData != null && !other.stringData.isEmpty()) {
                out.append(escape(stringData));
            }
            if (other.mapData != null && !other.mapData.isEmpty()) {
                for (Entry<ConfigData, ConfigData> d : other.mapData.entrySet()) {
                    String value = d.getValue().toString();
                    if (value == null)
                        continue;
                    if (d.getKey().comment != null)
                        out.append("\n#").append(d.getKey().comment.replace("\n", "\n#"));
                    value = value.replace("\n", "\n  ");
                    String key = d.getKey().toString().replace("\n", "\n  ");
                    if (key.contains("\n")) {
                        out.append("\n  > ").append(key).append("\n  : ").append(value);
                    } else {
                        out.append("\n  ").append(key).append(": ").append(value);
                    }
                }
            }
            if (other.listData != null && !other.listData.isEmpty()) {
                for (ConfigData d : other.listData) {
                    String data = d.toString();
                    if (data == null)
                        data = "";
                    else if (data.startsWith("\n  "))
                        data = data.substring(3);
                    if (d.comment != null)
                        out.append("\n#").append(d.comment.replace("\n", "\n#"));
                    out.append("\n- ").append(data);
                }
            }
            if (out.length() == 0)
                return null;
            return out.toString();
        }
    }
}