package gyurix.bungeelib.utils;

/**
 * Class for converting null objects to 0 values and vice versa
 */
public class NullUtils {
    public static Boolean from0(boolean data) {
        return data == false ? null : data;
    }

    public static Float from0(float data) {
        return data == 0 ? null : data;
    }

    public static Double from0(double data) {
        return data == 0 ? null : data;
    }

    public static Byte from0(byte data) {
        return data == 0 ? null : data;
    }

    public static Short from0(short data) {
        return data == 0 ? null : data;
    }

    public static Integer from0(int data) {
        return data == 0 ? null : data;
    }

    public static Long from0(long data) {
        return data == 0 ? null : data;
    }

    public static String from0(String in) {
        return in == null || in.isEmpty() ? null : in;
    }

    public static boolean to0(Boolean data) {
        return data == null ? false : data;
    }

    public static float to0(Float data) {
        return data == null ? 0 : data;
    }

    public static double to0(Double data) {
        return data == null ? 0 : data;
    }

    public static byte to0(Byte data) {
        return data == null ? 0 : data;
    }

    public static short to0(Short data) {
        return data == null ? 0 : data;
    }

    public static int to0(Integer data) {
        return data == null ? 0 : data;
    }

    public static long to0(Long data) {
        return data == null ? 0L : data;
    }

    public static String to0(String data) {
        return data == null ? "" : data;
    }
}
