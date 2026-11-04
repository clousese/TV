package androidx.media3.common;

import java.util.function.Consumer;

// LIVE-ONLY STUB: disc-menu controller from the author's private Media3 fork
// (Blu-ray / ISO disc navigation). Public Media3 1.11 has no such class.
// Live playback never opens a disc menu; all methods are no-ops returning
// the "unavailable" outcome.
public interface DiscMenuController {

    enum OpenResult {
        SUCCESS,
        RUNTIME_MISSING,
        UNAVAILABLE,
        TIMED_OUT
    }

    default boolean isNavigationPlayback() {
        return false;
    }

    default boolean isActive() {
        return false;
    }

    default boolean supportsPointer() {
        return false;
    }

    default boolean sendAction(String action) {
        return false;
    }

    default boolean open(String action, Consumer<OpenResult> callback) {
        if (callback != null) callback.accept(OpenResult.UNAVAILABLE);
        return false;
    }
}
