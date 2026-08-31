package com.cityparty.module.recommendation.algorithm;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RecommendationScorerTest {

    private final RecommendationScorer scorer = new RecommendationScorer();

    @Test
    void scoresCompletePartialAndMissingInterestMatches() {
        var complete = scorer.scoreInterest(List.of("周末", "新手友好"), List.of("新手友好", "周末"));
        var partial = scorer.scoreInterest(List.of("周末", "运动"), List.of("周末", "新手友好"));
        var missing = scorer.scoreInterest(List.of("运动"), List.of("周末", "新手友好"));

        assertThat(complete.score()).isCloseTo(100D, within(0.0001D));
        assertThat(complete.matchedInterests()).containsExactly("周末", "新手友好");
        assertThat(partial.score()).isBetween(0.01D, 99.99D);
        assertThat(missing.score()).isZero();
        assertThat(missing.matchedInterests()).isEmpty();
    }

    @Test
    void marksInterestUnavailableWhenUserHasNoInterests() {
        var score = scorer.scoreInterest(List.of(" "), List.of("周末"));

        assertThat(score.score()).isNull();
        assertThat(score.matchedInterests()).isEmpty();
    }

    @Test
    void distanceScoreDecaysAndHaversineMatchesKnownDistance() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 12, 0);
        var zeroKm = scorer.scoreDistance(decimal(116.4), decimal(39.9), decimal(116.4), decimal(39.9));
        var oneKm = scorer.scoreDistance(decimal(116.4), decimal(39.9), decimal(116.4), decimal(39.909));
        var fiveKm = scorer.scoreDistance(decimal(116.4), decimal(39.9), decimal(116.4), decimal(39.945));
        var tenKm = scorer.scoreDistance(decimal(116.4), decimal(39.9), decimal(116.4), decimal(39.99));
        var crossCity = scorer.score(new RecommendationScorer.ScoreInput(
                List.of(),
                List.of("桌游"),
                decimal(116.4),
                decimal(39.9),
                decimal(121.47),
                decimal(31.23),
                1,
                10,
                0,
                now.plusDays(2),
                100,
                now
        ));

        assertThat(oneKm.distanceKm()).isCloseTo(1D, within(0.03D));
        assertThat(zeroKm.score()).isGreaterThan(oneKm.score());
        assertThat(oneKm.score()).isGreaterThan(fiveKm.score());
        assertThat(fiveKm.score()).isGreaterThan(tenKm.score());
        assertThat(crossCity.distanceKm()).isGreaterThan(BigDecimal.valueOf(100));
        assertThat(crossCity.distanceScore()).isLessThan(BigDecimal.ONE);
    }

    @Test
    void marksDistanceUnavailableWithoutCompleteCoordinates() {
        var score = scorer.scoreDistance(null, decimal(39.9), decimal(116.4), decimal(39.9));

        assertThat(score.distanceKm()).isNull();
        assertThat(score.score()).isNull();
    }

    @Test
    void hotnessIncreasesWithSignupRatioAndFavorites() {
        double lowSignup = scorer.scoreHotness(1, 10, 0);
        double highSignup = scorer.scoreHotness(8, 10, 0);
        double lowFavorites = scorer.scoreHotness(1, 10, 1);
        double highFavorites = scorer.scoreHotness(1, 10, 10);

        assertThat(highSignup).isGreaterThan(lowSignup);
        assertThat(highFavorites).isGreaterThan(lowFavorites);
    }

    @Test
    void nearTermActivityScoresHigherThanDistantActivity() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 31, 12, 0);

        assertThat(scorer.scoreTime(now.plusDays(2), now))
                .isGreaterThan(scorer.scoreTime(now.plusDays(30), now));
    }

    @Test
    void creditScoreHonorsNormalizationBoundaries() {
        assertThat(scorer.scoreCredit(50)).isZero();
        assertThat(scorer.scoreCredit(60)).isZero();
        assertThat(scorer.scoreCredit(100)).isCloseTo(80D, within(0.0001D));
        assertThat(scorer.scoreCredit(110)).isCloseTo(100D, within(0.0001D));
        assertThat(scorer.scoreCredit(120)).isCloseTo(100D, within(0.0001D));
        assertThat(scorer.scoreCredit(null)).isNull();
    }

    @Test
    void dynamicallyNormalizesWeightsForMissingInterestAndDistance() {
        double allFeatures = scorer.combineScores(100D, 0D, 50D, 75D, 80D);
        double withoutInterest = scorer.combineScores(null, 0D, 50D, 75D, 80D);
        double withoutDistance = scorer.combineScores(100D, null, 50D, 75D, 80D);
        double coldStart = scorer.combineScores(null, null, 50D, 75D, 80D);

        assertThat(allFeatures).isCloseTo(61.75D, within(0.0001D));
        assertThat(withoutInterest).isCloseTo(41.1538D, within(0.0001D));
        assertThat(withoutDistance).isCloseTo(82.3333D, within(0.0001D));
        assertThat(coldStart).isCloseTo(66.875D, within(0.0001D));
        assertThat(List.of(allFeatures, withoutInterest, withoutDistance, coldStart))
                .allMatch(score -> score >= 0D && score <= 100D);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}
