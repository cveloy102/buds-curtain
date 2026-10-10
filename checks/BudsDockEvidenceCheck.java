package com.budscurtain;

public final class BudsDockEvidenceCheck {
    public static void main(String[] args) {
        BudsDockEvidence dock = new BudsDockEvidence();
        FastPairBattery left = packet("10405204380221FA1533E47F3A");
        FastPairBattery right = packet("10405204380221FA15337FE43A");
        FastPairBattery partialHide = packet("10405204380221FA1534E47F3A");
        FastPairBattery fullHide = packet("10405204380221FA1534E4E43A");
        CurtainState state = new CurtainState();
        require(state.accept(left.show, 1000, 1000));
        dock.observe(left, 1000);
        require(!dock.bothDocked(1000));
        dock.observe(partialHide, 1200);
        require(!dock.bothDocked(1200));
        require(state.accept(right.show, 1400, 1400));
        dock.observe(right, 1400);
        require(dock.bothDocked(1400) && state.wantsCard(1400));
        require(dock.left == 100 && dock.right == 100 && dock.cradle == 58);
        require(dock.bothDocked(4800));
        require(!dock.bothDocked(7201));

        dock.reset();
        dock.observe(left, 10000);
        dock.observe(right, 15500);
        require(dock.bothDocked(15500));
        require(!dock.bothDocked(16001));

        dock.reset();
        FastPairBattery worn = packet("10405204380221FA15337F577F");
        for (long t = 10000; t < 30000; t += 2000) {
            dock.observe(worn, t);
            require(!dock.bothDocked(t));
        }
        dock.observe(left, 30000);
        dock.observe(right, 30300);
        require(dock.bothDocked(30300));
        dock.observe(packet("10405204380221FA153364643A"), 30600);
        require(!dock.bothDocked(30600));
        dock.observe(fullHide, 31000);
        dock.reset();
        require(!dock.bothDocked(31000));
        System.out.println("Buds 5: alternating packets, one bud, removal and timeout OK");
    }

    private static FastPairBattery packet(String hex) {
        FastPairBattery battery = FastPairBattery.parse(java.util.HexFormat.of().parseHex(hex), true);
        require(battery != null);
        return battery;
    }
    private static void require(boolean condition) { if (!condition) throw new AssertionError(); }
}
