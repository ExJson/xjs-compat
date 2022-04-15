package xjs.serialization.parser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

public class HjsonParser extends XjsParser {

    public HjsonParser(final File file) throws IOException {
        super(file);
    }

    public HjsonParser(final Reader reader) throws IOException {
        super(reader);
    }

    public HjsonParser(final String text) {
        super(text);
    }
}
