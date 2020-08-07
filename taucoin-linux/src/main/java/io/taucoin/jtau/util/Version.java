package io.taucoin.jtau.util;

public class Version {

    private static final int MAJOR = 0;

    private static final int MINOR = 1;

    private static final int MAINTENANCE = 1;

    public static String get() {
        return "v" + MAJOR + "." + MINOR + "." + MAINTENANCE;
    }
}
