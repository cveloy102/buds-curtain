package com.budscurtain;

public final class CurtainStateCheck {
    public static void main(String[] args) {
        CurtainState state = new CurtainState();
        require(!state.wantsCard(0));
        require(state.accept(true, 1000, 1000) && state.wantsCard(1000));
        state.dismiss();
        require(state.accept(true, 2000, 2000) && !state.wantsCard(2000));
        require(state.accept(true, 40000, 40000) && state.wantsCard(40000));
        state.dismiss();
        require(state.accept(false, 41000, 41000) && !state.wantsCard(41000));
        require(state.accept(true, 42000, 42000) && state.wantsCard(42000));
        require(!state.accept(false, 41500, 42000) && state.wantsCard(42000));
        require(!state.wantsCard(50001));
        require(!state.accept(true, 50000, 60000));
        require(!state.accept(true, 63000, 60000));
        state.disconnected(70000);
        require(!state.wantsCard(70000));
        require(!state.accept(true, 69999, 70000));
        require(!state.accept(true, 71000, 71000));
        state.connected();
        require(state.accept(true, 71500, 71500) && state.wantsCard(71500));
        // Continuous advertisements never undo a manual dismissal.
        state.dismiss();
        for (long t = 72000; t < 150000; t += 1000) {
            require(state.accept(true, t, t) && !state.wantsCard(t));
        }
        CurtainState partial = new CurtainState();
        require(partial.accept(true, 1000, 1000));
        require(partial.acceptPartial(2000, 2000) && partial.wantsCard(2000));
        require(!partial.accept(true, 1500, 2000));
        partial.dismiss();
        require(partial.acceptPartial(3000, 3000) && !partial.wantsCard(3000));
        require(partial.acceptPartial(4000, 4000) && !partial.wantsCard(4000));
        require(!partial.wantsCard(9001));
        require(partial.accept(true, 10000, 10000) && partial.wantsCard(10000));
        System.out.println("Curtain state: hide, swipe, stale packets, timeout and reconnect OK");
    }
    private static void require(boolean condition) { if (!condition) throw new AssertionError(); }
}
