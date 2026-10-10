package com.budscurtain;

/** Joins the Buds 5's alternating left/right advertisements for a short time. */
final class BudsDockEvidence {
    static final long MAX_AGE_MS = 6000;
    private long leftAt = -1, rightAt = -1, caseAt = -1;
    Integer left, right, cradle;
    boolean leftCharging, rightCharging, caseCharging;

    void observe(FastPairBattery battery, long now) {
        if (battery.left != null) {
            left = battery.left;
            leftCharging = battery.leftCharging;
            leftAt = leftCharging ? now : -1;
        }
        if (battery.right != null) {
            right = battery.right;
            rightCharging = battery.rightCharging;
            rightAt = rightCharging ? now : -1;
        }
        if (battery.cradle != null) {
            cradle = battery.cradle;
            caseCharging = battery.caseCharging;
            caseAt = now;
        }
    }

    boolean bothDocked(long now) {
        return fresh(leftAt, now) && fresh(rightAt, now) && fresh(caseAt, now);
    }

    void reset() {
        leftAt = rightAt = caseAt = -1;
        left = right = cradle = null;
        leftCharging = rightCharging = caseCharging = false;
    }

    private static boolean fresh(long at, long now) {
        return at >= 0 && now >= at && now - at <= MAX_AGE_MS;
    }
}
