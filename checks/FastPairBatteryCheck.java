package com.budscurtain;

public final class FastPairBatteryCheck {
    public static void main(String[] args) {
        FastPairBattery show = FastPairBattery.parse(hex("00400102030421050633565240"));
        require(show != null && show.show && show.left == 86 && show.right == 82 && show.cradle == 64);
        FastPairBattery hide = FastPairBattery.parse(hex("00400102030421050634FF327F"));
        require(hide != null && !hide.show && hide.left == null && hide.right == 50 && hide.cradle == null);
        // Real service-data bytes captured from the user's Buds 5, 2026-09-30.
        byte[] actual = hex("10405204380221FA1533E4E42B");
        require(FastPairBattery.parse(actual) == null);
        FastPairBattery buds = FastPairBattery.parse(actual, true);
        require(buds != null && buds.show && buds.left == 100 && buds.right == 100 && buds.cradle == 43);
        require(buds.leftCharging && buds.rightCharging && !buds.caseCharging);
        FastPairBattery worn = FastPairBattery.parse(hex("10405204380221FA153364647F"), true);
        require(worn != null && !worn.leftCharging && !worn.rightCharging);
        require(!FastPairBattery.parse(hex("10405204380221FA15337FE43D"), true).leftCharging);
        require(!FastPairBattery.parse(hex("10405204380221FA1533E4643A"), true).rightCharging);
        // Today's one-bud trace: the other battery and case are unknown.
        require(FastPairBattery.parse(hex("10405204380221FA15337F577F"), true).cradle == null);
        require(FastPairBattery.parse(hex("10401910001C21B5D033E4E42B"), true).cradle == 43);
        // A marker inside the account filter must not become a battery record.
        require(FastPairBattery.parse(hex("00403364642B210506")) == null);
        require(FastPairBattery.parse(hex("00400102030421050633656667")) == null);
        require(FastPairBattery.parse(hex("00400102030421050633E4E4")) == null);
        require(FastPairBattery.parse(hex("2040010203042105063364642B"), true) == null);
        require(FastPairBattery.parse(hex("0040010203042105063364642B1A01")).cradle == 43);
        require(FastPairBattery.parse(hex("0040010203042105063364642B20FF")) == null);
        require(FastPairBattery.parse(hex("003164642B")) == null);
        require(FastPairBattery.parse(hex("00400102030421050633000000")).left == 0);
        require(FastPairBattery.parse(null) == null);
        System.out.println("Fast Pair: captured Buds 5 packets, charging flags, bounds and invalid packets OK");
    }
    private static byte[] hex(String s) { return java.util.HexFormat.of().parseHex(s); }
    private static void require(boolean condition) { if (!condition) throw new AssertionError(); }
}
