package gyurix.bungeelib.utils;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeSet;

/**
 * Created by GyuriX on 2015.12.29..
 */
public class Orderable implements Comparable<Orderable> {
    public final Object key;
    public final Comparable value;

    public Orderable(Object key, Comparable value) {
        this.key = key;
        this.value = value;
    }

    public static TreeSet<Orderable> order(HashMap<Object, Comparable> data) {
        TreeSet<Orderable> out = new TreeSet<>();
        for (Entry<Object, Comparable> e : data.entrySet()) {
            out.add(new Orderable(e.getKey(), e.getValue()));
        }
        return out;
    }

    @Override
    public int compareTo(Orderable o) {
        if (value.compareTo(o.value) == 0)
            return key.toString().compareTo(o.key.toString());
        return 0 - value.compareTo(o.value);
    }

    @Override
    public int hashCode() {
        return key.hashCode() * 100000 + value.hashCode();
    }

    @Override
    public String toString() {
        return key + " - " + value;
    }
}
