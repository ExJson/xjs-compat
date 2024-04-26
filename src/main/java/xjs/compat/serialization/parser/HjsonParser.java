package xjs.compat.serialization.parser;

import xjs.compat.serialization.token.HjsonTokenizer;
import xjs.data.serialization.parser.DjsParser;
import xjs.data.serialization.token.TokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A parser providing compatibility with Hjson files.
 */
public class HjsonParser extends DjsParser {

    public HjsonParser(final File file) throws IOException {
        super(HjsonTokenizer.stream(new FileInputStream(file)));
    }

    public HjsonParser(final String text) {
        super(HjsonTokenizer.stream(text));
    }

    public HjsonParser(final TokenStream root) {
        super(root);
    }
}
