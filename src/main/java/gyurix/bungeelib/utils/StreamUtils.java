package gyurix.bungeelib.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by GyuriX on 2016. 07. 14..
 */
public class StreamUtils {
    public static boolean toFile(InputStream is, File f) {
        try {
            f.getParentFile().mkdirs();
            f.createNewFile();
            FileOutputStream fos = new FileOutputStream(f);
            while (true) {
                int id = is.read();
                if (id < 0)
                    break;
                fos.write(id);
            }
            is.close();
            fos.close();
            return true;
        } catch (Throwable e) {
            BU.error(BU.cs, e, "BungeeLib", "gyurix");
            return false;
        }
    }
}
