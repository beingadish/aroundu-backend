package com.beingadish.AroundU.common.util;

/**
 * Calculates a simple popularity score for jobs and workers.
 * <p>
 * The score is a weighted integer used solely for ordering; it has no business
 * meaning beyond "higher = more popular".
 */
public final class PopularityUtils {

    private PopularityUtils() {
        // utility class
    }

    // Weights
    private static final int BID_WEIGHT = 3;
    private static final int VIEW_WEIGHT = 1;

    /**
     * Calculates a popularity score for a job based on bid count and view
     * count.
     *
     * @param bidCount number of bids placed on the job (may be null)
     * @param viewCount number of times the job has been viewed (may be null)
     * @return the weighted popularity score (≥ 0)
     */
    public static int calculateJobPopularityScore(Integer bidCount, Integer viewCount) {
        int bids = bidCount != null ? bidCount : 0;
        int views = viewCount != null ? viewCount : 0;
        return (bids * BID_WEIGHT) + (views * VIEW_WEIGHT);
    }

    /**
     * Calculates a composite score for a worker based on their completion rate
     * and average rating.
     *
     * @param successRate percentage of successfully completed jobs (0–100, may
     * be null)
     * @param averageRating average review rating (0–5, may be null)
     * @param completedJobs total completed jobs (may be null)
     * @return the weighted worker score (≥ 0)
     */
    public static double calculateWorkerScore(Double successRate, Double averageRating, Integer completedJobs) {
        double rate = successRate != null ? successRate : 0.0;
        double rating = averageRating != null ? averageRating : 0.0;
        int completed = completedJobs != null ? completedJobs : 0;

        // Weighted: 40% rating (scaled to 100), 40% success rate, 20% volume (capped at 100)
        double ratingComponent = (rating / 5.0) * 100.0 * 0.4;
        double rateComponent = rate * 0.4;
        double volumeComponent = Math.min(completed, 100) * 0.2;

        return ratingComponent + rateComponent + volumeComponent;
    }
}
