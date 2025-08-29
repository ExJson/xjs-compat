package xjs.compat.serialization.parser;

import xjs.compat.serialization.token.HjsonTokenizer;
import xjs.data.StringType;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.parser.DjsParser;
import xjs.data.serialization.token.StringToken;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A parser providing compatibility with Hjson files.
 */
public class HjsonParser extends DjsParser {

    public HjsonParser(final File file) throws IOException {
        this(PositionTrackingReader.fromReader(new FileReader(file)));
    }

    public HjsonParser(final String text) {
        this(PositionTrackingReader.fromString(text));
    }

    public HjsonParser(final PositionTrackingReader reader) {
        this(HjsonTokenizer.stream(reader));
    }

    public HjsonParser(final TokenStream root) {
        super(root);
    }

    @Override
    protected String readKey() {
        if (this.current instanceof StringToken t) {
            switch (t.stringType()) {
                case SINGLE, DOUBLE, IMPLICIT -> {
                    this.read();
                    return t.parsed();
                }
                default -> throw this.illegalKeyType(t.stringType());
            }
        }
        return super.readKey();
    }

    protected SyntaxException illegalKeyType(final StringType t) {
        return this.unexpected("string type in key: " + t + " (must be single, double, unquoted)");
    }
}
