package androidx.media3.common.util;

import java.util.List;

// LIVE-ONLY STUB: clear-key license parser from the author's private
// Media3 fork. Public Media3 1.11 has no such class. Only used by
// MediaItemFactory.buildDrmConfig to decide between the DRM scheme from
// the source and CLEARKEY: a key that looks like a clear-key JSON document
// (contains a "keys" member) is treated as clear-key, anything else as
// not-a-clear-key-license. Live streams rarely carry DRM, and this keeps
// the original branch logic intact.
public final class LocalClearKeyLicense {

    private LocalClearKeyLicense() {
    }

    public static List<String> parse(String key) {
        if (key == null || key.isEmpty() || !key.contains("\"keys\"")) {
            return List.of();
        }
        return List.of(key);
    }
}
