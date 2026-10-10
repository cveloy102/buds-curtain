package com.budscurtain;

/** A dismissed card stays dismissed until a hide signal or a gap in advertising. */
final class CurtainState {
    static final long STALE_MS = 8000;
    private long lastEvent = -1;
    private long lastPacket = -1;
    private long blockUntil;
    private boolean open, dismissed;

    boolean accept(boolean show, long timestamp, long now) {
        if (!freshPacket(timestamp, now)) return false;
        if (show && timestamp < blockUntil) return false;
        if (lastEvent >= 0 && timestamp - lastEvent > STALE_MS) { open = false; dismissed = false; }
        lastPacket = timestamp;
        lastEvent = timestamp;
        if (!show) { open = false; dismissed = false; }
        else if (!open) { open = true; dismissed = false; }
        return true;
    }

    /** A partial hide carries battery data, but must not close or prolong the UI. */
    boolean acceptPartial(long timestamp, long now) {
        if (!freshPacket(timestamp, now) || timestamp < blockUntil) return false;
        lastPacket = timestamp;
        return true;
    }

    private boolean freshPacket(long timestamp, long now) {
        return timestamp > lastPacket && timestamp >= now - STALE_MS && timestamp <= now + 1000;
    }

    boolean wantsCard(long now) { return open && !dismissed && now - lastEvent <= STALE_MS; }
    void dismiss() { dismissed = true; }
    void connected() { blockUntil = 0; }
    void disconnected(long now) {
        lastEvent = lastPacket = now;
        blockUntil = now + 1500;
        open = false; dismissed = false;
    }
}
