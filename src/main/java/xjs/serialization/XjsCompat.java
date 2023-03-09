package xjs.serialization;

import org.jetbrains.annotations.ApiStatus;
import xjs.serialization.parser.HjsonParser;
import xjs.serialization.parser.ParsingFunction;
import xjs.serialization.parser.TxtParser;
import xjs.serialization.parser.UbjsonParser;
import xjs.serialization.writer.HjsonWriter;
import xjs.serialization.writer.TxtWriter;
import xjs.serialization.writer.UbjsonWriter;
import xjs.serialization.writer.WritingFunction;

/**
 * Core utilities used by xjs-compat. For internal use only.
 */
@ApiStatus.Internal
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
