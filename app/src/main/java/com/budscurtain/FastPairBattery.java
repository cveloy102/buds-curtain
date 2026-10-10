package com.budscurtain;

/** Length/type fields in Fast Pair FE2C service data (not the whole BLE record). */
final class FastPairBattery {
    final boolean show;
    final Integer left;
    final Integer right;
    final Integer cradle;
    final boolean leftCharging, rightCharging, caseCharging;

    private FastPairBattery(boolean show, byte left, byte right, byte cradle) {
        this.show = show;
        this.left = level(left); this.right = level(right); this.cradle = level(cradle);
        leftCharging = this.left != null && (left & 0x80) != 0;
        rightCharging = this.right != null && (right & 0x80) != 0;
        caseCharging = this.cradle != null && (cradle & 0x80) != 0;
    }

    static FastPairBattery parse(byte[] data) {
        return parse(data, false);
    }

    static FastPairBattery parse(byte[] data, boolean allowBuds5Header) {
        if (data == null || data.length < 9) return null;
        // Google documents 0x00. Our selected Xiaomi Buds 5 advertises 0x10
        // with the same field layout; opt in only for that known device.
        int header = data[0] & 0xff;
        if (header != 0 && !(allowBuds5Header && header == 0x10)) return null;
        boolean filter = false, salt = false;
        FastPairBattery battery = null;
        int offset = 1;
        while (offset < data.length) {
            int field = data[offset++] & 0xff;
            int length = field >>> 4, type = field & 0x0f;
            if (length == 0 || offset + length > data.length) return null;
            if (type == 0 || type == 2) {
                if (filter || salt || battery != null) return null;
                filter = true;
            } else if (type == 1) {
                if (!filter || salt || (length != 1 && length != 2)) return null;
                salt = true;
            } else if (type == 3 || type == 4) {
                if (!filter || !salt || battery != null || length != 3) return null;
                for (int i = 0; i < 3; i++) {
                    int value = data[offset + i] & 0x7f;
                    if (value > 100 && value != 127) return null;
                }
                battery = new FastPairBattery(type == 3, data[offset], data[offset+1], data[offset+2]);
            }
            offset += length;
        }
        return battery;
    }

    private static Integer level(byte raw) {
        int value = raw & 0x7f;
        return value <= 100 ? value : null;
    }

}
