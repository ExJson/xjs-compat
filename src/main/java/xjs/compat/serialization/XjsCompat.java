package xjs.compat.serialization;

import org.jetbrains.annotations.ApiStatus;
import xjs.compat.serialization.parser.BinaryParsingFunction;
import xjs.compat.serialization.parser.TxtParser;
import xjs.compat.serialization.util.UBTyping;
import xjs.compat.serialization.writer.BinaryWritingFunction;
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
    private static volatile UBTyping ubTyping;

    private XjsCompat() {}

    // Loads the provided serializers into the JsonSerializationContext.
    static {
        JsonContext.addParser("hjson", ParsingFunction.fromParser(HjsonParser::new));
        JsonContext.addWriter("hjson", WritingFunction.fromWriter(HjsonWriter::new));
        JsonContext.addParser("ubjson", BinaryParsingFunction.fromParser(UbjsonParser::new));
        JsonContext.addWriter("ubjson", BinaryWritingFunction.fromWriter(UbjsonWriter::new));
        JsonContext.addParser("txt", ParsingFunction.fromParser(TxtParser::new));
        JsonContext.addWriter("txt", WritingFunction.fromWriter((f, o) -> new TxtWriter(f)));
    }

    public static UBTyping getDefaultUbTyping() {
        return ubTyping;
    }

    public static void setDefaultUbTyping(UBTyping typing) {
        ubTyping = typing;
    }
}
