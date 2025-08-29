package xjs.compat.serialization.writer;

import xjs.data.JsonValue;
import xjs.data.serialization.writer.JsonWriterOptions;
import xjs.data.serialization.writer.ValueWriter;
import xjs.data.serialization.writer.WritingFunction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.function.Function;

@FunctionalInterface
public interface BinaryWritingFunction extends WritingFunction {

    @Override
    void write(final OutputStream os, final JsonValue value, final JsonWriterOptions options) throws IOException;

    @Override
    default void write(final Writer tw, final JsonValue value, final JsonWriterOptions options) {
        throw new UnsupportedOperationException("Cannot write binary type to writer");
    }

    @Override
    default void write(final File file, final JsonValue value, final JsonWriterOptions options) throws IOException {
        this.write(new FileOutputStream(file), value, options);
    }

    @Override
    default String stringify(final JsonValue value, final JsonWriterOptions options) throws IOException {
        final var os = new ByteArrayOutputStream();
        this.write(os, value, options);
        return os.toString();
    }

    static BinaryWritingFunction fromWriter(final Function<OutputStream, ValueWriter> c) {
        return (os, value, options) -> {
            final ValueWriter writer = c.apply(os);
            writer.write(value);

            try {
                writer.close();
            } catch (final Exception e) {
                throw new IOException(e);
            }
        };
    }
}
