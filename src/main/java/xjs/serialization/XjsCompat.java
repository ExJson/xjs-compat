package xjs.serialization;

import org.jetbrains.annotations.ApiStatus;
import xjs.serialization.parser.HjsonParser;
import xjs.serialization.writer.HjsonWriter;

/**
 * Core utilities used by xjs-compat. For internal use only.
 */
@ApiStatus.Internal
public final class XjsCompat {

    private XjsCompat() {}

    // Loads the provided serializers into the JsonSerializationContext.
    static {
        JsonSerializationContext.addParser("hjson", file -> new HjsonParser(file).parse());
        JsonSerializationContext.addWriter("hjson", (w, v, o) -> new HjsonWriter(w, o).write(v));
    }
}
