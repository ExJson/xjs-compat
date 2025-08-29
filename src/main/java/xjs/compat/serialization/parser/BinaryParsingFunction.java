package xjs.compat.serialization.parser;

import xjs.data.JsonValue;
import xjs.data.serialization.parser.ParsingFunction;
import xjs.data.serialization.parser.ValueParser;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.function.Function;

@FunctionalInterface
public interface BinaryParsingFunction extends ParsingFunction {

    @Override
    JsonValue parse(final InputStream is) throws IOException;

    @Override
    default JsonValue parse(final PositionTrackingReader reader) {
        throw new UnsupportedOperationException("Cannot parse binary type from reader");
    }

    @Override
    default JsonValue parse(final Reader reader) {
        throw new UnsupportedOperationException("Cannot parse binary type from reader");
    }

    @Override
    default JsonValue parse(final String s) {
        try {
            return this.parse(new ByteArrayInputStream(s.getBytes())); // NOT utf-8
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    default JsonValue parse(final File file) throws IOException {
        return this.parse(new FileInputStream(file));
    }

    static BinaryParsingFunction fromParser(final Function<InputStream, ValueParser> c) {
        return is -> {
            final ValueParser parser = c.apply(is);
            final JsonValue value = parser.parse();

            try {
                parser.close();
            } catch (final Exception e) {
                throw new IOException(e);
            }
            return value;
        };
    }
}
