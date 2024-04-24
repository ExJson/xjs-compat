package xjs.compat.serialization.writer;

import xjs.core.JsonFormat;
import xjs.core.JsonValue;
import xjs.serialization.writer.ValueWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class TxtWriter implements ValueWriter {
    private final Writer tw;

    public TxtWriter(final File file) throws IOException {
        this.tw = new FileWriter(file);
    }

    public TxtWriter(final Writer tw) {
        this.tw = tw;
    }

    @Override
    public void write(final JsonValue value) throws IOException {
        if (value.isPrimitive()) {
            this.tw.write(value.intoString());
        } else {
            this.tw.write(value.toString(JsonFormat.JSON_FORMATTED));
        }
    }

    @Override
    public void close() throws IOException {
        this.tw.close();
    }
}
