package com.cityparty.module.recommendation.algorithm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.stereotype.Component;

@Component
public class RecommendationScorer {

    private static final double EARTH_RADIUS_KM = 6371.0088D;
    private static final double INTEREST_WEIGHT = 0.35D;
    private static final double DISTANCE_WEIGHT = 0.25D;
    private static final double HOTNESS_WEIGHT = 0.15D;
    private static final double TIME_WEIGHT = 0.15D;
    private static final double CREDIT_WEIGHT = 0.10D;

    public ScoreResult score(ScoreInput input) {
        InterestScore interest = scoreInterest(input.userInterests(), input.activityFeatures());
        DistanceScore distance = scoreDistance(
                input.userLongitude(),
                input.userLatitude(),
                input.activityLongitude(),
                input.activityLatitude()
        );
        double hotness = scoreHotness(input.approvedCount(), input.maxParticipants(), input.favoriteCount());
        double time = scoreTime(input.startTime(), input.now());
        Double credit = scoreCredit(input.creatorCreditScore());
        double finalScore = combineScores(interest.score(), distance.score(), hotness, time, credit);

        return new ScoreResult(
                rounded(finalScore),
                roundedDistance(distance.distanceKm()),
                interest.matchedInterests(),
                roundedNullable(interest.score()),
                roundedNullable(distance.score()),
                rounded(hotness),
                rounded(time),
                roundedNullable(credit)
        );
    }

    public InterestScore scoreInterest(Collection<String> userInterests, Collection<String> activityFeatures) {
        Set<String> normalizedUserInterests = normalize(userInterests);
        if (normalizedUserInterests.isEmpty()) {
            return new InterestScore(null, List.of());
        }

        Set<String> normalizedActivityFeatures = normalize(activityFeatures);
        Set<String> matched = new TreeSet<>(normalizedUserInterests);
        matched.retainAll(normalizedActivityFeatures);
        if (matched.isEmpty()) {
            return new InterestScore(0D, List.of());
        }

        Set<String> union = new TreeSet<>(normalizedUserInterests);
        union.addAll(normalizedActivityFeatures);
        double jaccard = (double) matched.size() / union.size();
        double coverage = normalizedActivityFeatures.isEmpty()
                ? 0D
                : (double) matched.size() / normalizedActivityFeatures.size();
        double score = 100D * (0.7D * jaccard + 0.3D * coverage);
        return new InterestScore(clamp(score), List.copyOf(matched));
    }

    public DistanceScore scoreDistance(BigDecimal userLongitude,
                                       BigDecimal userLatitude,
                                       BigDecimal activityLongitude,
                                       BigDecimal activityLatitude) {
        if (userLongitude == null || userLatitude == null
                || activityLongitude == null || activityLatitude == null) {
            return new DistanceScore(null, null);
        }

        double lat1 = Math.toRadians(userLatitude.doubleValue());
        double lat2 = Math.toRadians(activityLatitude.doubleValue());
        double deltaLat = Math.toRadians(activityLatitude.subtract(userLatitude).doubleValue());
        double deltaLongitude = Math.toRadians(activityLongitude.subtract(userLongitude).doubleValue());
        double haversine = Math.sin(deltaLat / 2D) * Math.sin(deltaLat / 2D)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLongitude / 2D) * Math.sin(deltaLongitude / 2D);
        double boundedHaversine = Math.min(Math.max(haversine, 0D), 1D);
        double distanceKm = EARTH_RADIUS_KM
                * 2D
                * Math.atan2(Math.sqrt(boundedHaversine), Math.sqrt(1D - boundedHaversine));
        double score = 100D * Math.exp(-distanceKm / 5D);
        return new DistanceScore(distanceKm, clamp(score));
    }

    public double scoreHotness(Integer approvedCount, Integer maxParticipants, Integer favoriteCount) {
        int safeApprovedCount = Math.max(approvedCount == null ? 0 : approvedCount, 0);
        int safeMaxParticipants = Math.max(maxParticipants == null ? 0 : maxParticipants, 0);
        int safeFavoriteCount = Math.max(favoriteCount == null ? 0 : favoriteCount, 0);
        double signupScore = safeMaxParticipants > 0
                ? 100D * Math.min((double) safeApprovedCount / safeMaxParticipants, 1D)
                : 0D;
        double favoriteScore = 100D * (1D - Math.exp(-safeFavoriteCount / 5D));
        return clamp(0.7D * signupScore + 0.3D * favoriteScore);
    }

    public double scoreTime(LocalDateTime startTime, LocalDateTime now) {
        if (startTime == null || now == null || !startTime.isAfter(now)) {
            return 0D;
        }
        double daysUntilStart = Duration.between(now, startTime).toSeconds() / 86_400D;
        double score = daysUntilStart <= 3D
                ? 100D
                : 100D * Math.exp(-(daysUntilStart - 3D) / 14D);
        return clamp(score);
    }

    public Double scoreCredit(Integer creditScore) {
        if (creditScore == null) {
            return null;
        }
        return clamp((creditScore - 60D) / 50D * 100D);
    }

    public double combineScores(Double interest,
                                Double distance,
                                Double hotness,
                                Double time,
                                Double credit) {
        double weightedScore = 0D;
        double availableWeight = 0D;
        if (interest != null) {
            weightedScore += INTEREST_WEIGHT * clamp(interest);
            availableWeight += INTEREST_WEIGHT;
        }
        if (distance != null) {
            weightedScore += DISTANCE_WEIGHT * clamp(distance);
            availableWeight += DISTANCE_WEIGHT;
        }
        if (hotness != null) {
            weightedScore += HOTNESS_WEIGHT * clamp(hotness);
            availableWeight += HOTNESS_WEIGHT;
        }
        if (time != null) {
            weightedScore += TIME_WEIGHT * clamp(time);
            availableWeight += TIME_WEIGHT;
        }
        if (credit != null) {
            weightedScore += CREDIT_WEIGHT * clamp(credit);
            availableWeight += CREDIT_WEIGHT;
        }
        return availableWeight == 0D ? 0D : clamp(weightedScore / availableWeight);
    }

    public Set<String> normalize(Collection<String> values) {
        Set<String> normalized = new TreeSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                normalized.add(value.trim());
            }
        }
        return normalized;
    }

    private double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0D;
        }
        return Math.min(Math.max(value, 0D), 100D);
    }

    private BigDecimal rounded(double value) {
        return BigDecimal.valueOf(clamp(value)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal roundedNullable(Double value) {
        return value == null ? null : rounded(value);
    }

    private BigDecimal roundedDistance(Double value) {
        if (value == null) {
            return null;
        }
        double safeDistance = Double.isFinite(value) && value >= 0D ? value : 0D;
        return BigDecimal.valueOf(safeDistance).setScale(2, RoundingMode.HALF_UP);
    }

    public record ScoreInput(Collection<String> userInterests,
                             Collection<String> activityFeatures,
                             BigDecimal userLongitude,
                             BigDecimal userLatitude,
                             BigDecimal activityLongitude,
                             BigDecimal activityLatitude,
                             Integer approvedCount,
                             Integer maxParticipants,
                             Integer favoriteCount,
                             LocalDateTime startTime,
                             Integer creatorCreditScore,
                             LocalDateTime now) {
    }

    public record ScoreResult(BigDecimal recommendationScore,
                              BigDecimal distanceKm,
                              List<String> matchedInterests,
                              BigDecimal interestScore,
                              BigDecimal distanceScore,
                              BigDecimal hotnessScore,
                              BigDecimal timeScore,
                              BigDecimal creditScore) {
    }

    public record InterestScore(Double score, List<String> matchedInterests) {
    }

    public record DistanceScore(Double distanceKm, Double score) {
    }
}
