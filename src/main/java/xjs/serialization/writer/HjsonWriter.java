package xjs.serialization.writer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class HjsonWriter extends XjsWriter {

    public HjsonWriter(final File file, final boolean format) throws IOException {
        super(file, format);
    }

    public HjsonWriter(final Writer writer, final boolean format) {
        super(writer, format);
    }

    public HjsonWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }
}
