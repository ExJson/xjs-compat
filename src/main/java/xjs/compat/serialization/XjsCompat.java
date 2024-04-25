package xjs.compat.serialization;

import org.jetbrains.annotations.ApiStatus;
import xjs.compat.serialization.parser.TxtParser;
import xjs.data.serialization.JsonContext;
import xjs.compat.serialization.parser.HjsonParser;
import xjs.data.serialization.parser.ParsingFunction;
import xjs.compat.serialization.parser.UbjsonParser;
import xjs.compat.serialization.writer.HjsonWriter;
import xjs.compat.serialization.writer.TxtWriter;
import xjs.compat.serialization.writer.UbjsonWriter;
import xjs.data.serialization.writer.WritingFunction;

/**
 * Core utilities used by xjs-compat. For internal use only.
 */
@ApiStatus.Internal
@SuppressWarnings("unused") // reflective access
public final class XjsCompat {

    private XjsCompat() {}

    // Loads the provided serializers into the JsonSerializationContext.
    static {
        JsonContext.addParser("hjson", ParsingFunction.fromParser(HjsonParser::new));
        JsonContext.addWriter("hjson", WritingFunction.fromWriter(HjsonWriter::new));
        JsonContext.addParser("ubjson", ParsingFunction.fromParser(UbjsonParser::new));
        JsonContext.addWriter("ubjson", WritingFunction.fromWriter((f, o) -> new UbjsonWriter(f)));
        JsonContext.addParser("txt", ParsingFunction.fromParser(TxtParser::new));
        JsonContext.addWriter("txt", WritingFunction.fromWriter((f, o) -> new TxtWriter(f)));
    }
}
