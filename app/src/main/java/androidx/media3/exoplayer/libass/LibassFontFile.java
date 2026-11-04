package androidx.media3.exoplayer.libass;

import java.io.File;
import java.io.IOException;

// LIVE-ONLY STUB: libass font-file helper from the author's private Media3
// fork (reads the family name out of a font file for the libass subtitle
// renderer). Public Media3 1.11 has no such class. Live playback does not
// use libass, so getFamilyName returns an empty string: callers that
// require a family name fail cleanly with "font family name is missing",
// and callers that tolerate failure (readFamilyName) get null.
public final class LibassFontFile {

    private LibassFontFile() {
    }

    public static String getFamilyName(File file) throws IOException {
        return "";
    }
}
