package gyurix.bungeelib.test;

import gyurix.bungeelib.utils.ClassUtils;
import gyurix.bungeelib.utils.StringUtils;

import java.util.Scanner;

/**
 * Created by GyuriX on 2016. 07. 14..
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String s = "";
        while (true) {
            try {
                s = scanner.nextLine();
                Class cl = Class.forName(s);
                System.out.println(StringUtils.join(ClassUtils.getAllInterfaces(cl), ", "));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }
}
