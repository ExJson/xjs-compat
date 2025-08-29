package xjs.compat.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.data.JsonString;
import xjs.data.JsonValue;
import xjs.data.serialization.parser.ValueParser;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A parser providing compatibility with simple text files
 */
public class TxtParser implements ValueParser {
    private final PositionTrackingReader reader;

    public TxtParser(final String text) {
        this(PositionTrackingReader.fromString(text));
    }

    public TxtParser(final File file) throws IOException {
        this(PositionTrackingReader.fromReader(new FileReader(file), true));
    }

    public TxtParser(final PositionTrackingReader reader) {
        this.reader = reader.capturingFullText();
    }

    @Override
    public @NotNull JsonValue parse() throws IOException {
        return new JsonString(this.reader.readToEnd());
    }

    @Override
    public void close() {}
}
