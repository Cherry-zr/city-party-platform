package com.cityparty.module.credit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CreditLevelResolverTest {

    @ParameterizedTest
    @CsvSource({
            "120, 优秀",
            "110, 优秀",
            "109, 良好",
            "100, 良好",
            "99, 正常",
            "80, 正常",
            "79, 待提升",
            "60, 待提升"
    })
    void resolvesConfiguredCreditLevelBoundaries(int score, String expectedLevel) {
        assertThat(CreditLevelResolver.resolve(score)).isEqualTo(expectedLevel);
    }
}
