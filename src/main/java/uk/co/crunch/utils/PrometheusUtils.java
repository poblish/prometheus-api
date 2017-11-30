package uk.co.crunch.utils;

public class PrometheusUtils {
    public static String normaliseName(String name) {
        return name.replace('.','_')
                .replace('-','_')
                .replace('#','_')
                .replace(' ','_')
                .toLowerCase();
    }
}
