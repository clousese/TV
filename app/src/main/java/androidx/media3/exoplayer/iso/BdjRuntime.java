package androidx.media3.exoplayer.iso;

import android.app.Application;
import android.os.CancellationSignal;

import androidx.media3.common.MediaItem;

// LIVE-ONLY STUB: Blu-ray Disc Java (BDJ) runtime helper from the author's
// private Media3 fork. Public Media3 1.11 has no such class. Live playback
// never uses BDJ, so prepare is a no-op and getStatus reports no restart
// required.
public final class BdjRuntime {

    public static final class Status {
        public final boolean restartRequired;

        public Status(boolean restartRequired) {
            this.restartRequired = restartRequired;
        }
    }

    private BdjRuntime() {
    }

    public static void prepare(Application application, CancellationSignal signal) {
        // No-op in the live-only build: no BDJ runtime is prepared.
    }

    public static Status getStatus(Application application) {
        return new Status(false);
    }
}
