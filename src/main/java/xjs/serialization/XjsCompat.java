package xjs.serialization;

import org.jetbrains.annotations.ApiStatus;
import xjs.serialization.parser.HjsonParser;
import xjs.serialization.writer.HjsonWriter;

/**
 * Core utilities used by xjs-compat. For internal use only.
 */
public final class XjsCompat {

    private XjsCompat() {}

    /**
     * Loads the provided serializers into the {@link JsonSerializationContext}.
     */
    @ApiStatus.Internal
    public static void init() {
        JsonSerializationContext.addParser("hjson", file -> new HjsonParser(file).parse());
        JsonSerializationContext.addWriter("hjson", (w, v, o) -> new HjsonWriter(w, o).write(v));
    }
}
