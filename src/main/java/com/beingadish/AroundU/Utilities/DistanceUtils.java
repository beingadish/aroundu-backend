package com.beingadish.AroundU.Utilities;

/**
 * Provides the Haversine formula for calculating great-circle distance between
 * two points on Earth given their latitude/longitude in degrees.
 * <p>
 * All returned distances are in <b>kilometres</b>.
 */
public final class DistanceUtils {

    private DistanceUtils() {
        // utility class
    }

    /**
     * Mean radius of the Earth in kilometres.
     */
    private static final double EARTH_RADIUS_KM = 6_371.0;

    /**
     * Calculates the distance in km between two geographic coordinates using
     * the <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine
     * formula</a>.
     *
     * @param lat1 latitude of point 1 (degrees)
     * @param lon1 longitude of point 1 (degrees)
     * @param lat2 latitude of point 2 (degrees)
     * @param lon2 longitude of point 2 (degrees)
     * @return distance in kilometres
     * @throws IllegalArgumentException if any coordinate is null
     */
    public static double haversine(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            throw new IllegalArgumentException("All coordinates must be non-null");
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
