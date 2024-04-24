package xjs.compat.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.core.JsonString;
import xjs.core.JsonValue;
import xjs.core.StringType;
import xjs.serialization.parser.ValueParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * A parser providing compatibility with simple text files
 */
public class TxtParser implements ValueParser {
    private final String text;

    public TxtParser(final File file) throws IOException {
        this(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
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
