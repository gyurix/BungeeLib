package gyurix.bungeelib.utils;


import gyurix.bungeelib.configfile.ConfigSerialization.StringSerializable;

/**
 * Created by GyuriX.
 */
public class MultiIntData implements StringSerializable {
    int[] minValues, maxValues;

    public MultiIntData(String in) {
        String[] ss = in.split(", *");
        minValues = new int[ss.length];
        maxValues = new int[ss.length];
        int i = 0;
        for (String s : ss) {
            String[] d = s.split(":", 2);
            int first = Integer.valueOf(d[0]);
            minValues[i] = first;
            if (d.length == 2) {
                int second = Integer.valueOf(d[1]);
                maxValues[i] = second;
            } else maxValues[i] = first;
            ++i;
        }
    }

    public boolean contains(int value) {
        for (int i = 0; i < minValues.length; i++) {
            if (minValues[i] <= value && maxValues[i] >= value)
                return true;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < minValues.length; i++) {
            sb.append(',');
            sb.append(minValues[i]);
            if (minValues[i] != maxValues[i]) {
                sb.append(':');
                sb.append(maxValues[i]);
            }
        }
        return sb.length() == 0 ? "" : sb.substring(1);
    }
}
