package com.cityparty.module.credit;

public final class CreditLevelResolver {

    private CreditLevelResolver() {
    }

    public static String resolve(Integer creditScore) {
        int score = creditScore == null ? 100 : creditScore;
        if (score >= 110) {
            return "优秀";
        }
        if (score >= 100) {
            return "良好";
        }
        if (score >= 80) {
            return "正常";
        }
        return "待提升";
    }
}
