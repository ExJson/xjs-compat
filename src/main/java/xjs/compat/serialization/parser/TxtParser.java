package xjs.compat.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.core.JsonString;
import xjs.core.JsonValue;
import xjs.serialization.parser.ValueParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A parser providing compatibility with simple text files
 */
public class TxtParser implements ValueParser {
    private final String text;

    public TxtParser(final File file) throws IOException {
        this(Files.readString(file.toPath()));
    }

    public TxtParser(final String text) {
        this.text = text;
    }

    @Override
    public @NotNull JsonValue parse() {
        return new JsonString(this.text);
    }

    @Override
    public void close() {}
}
