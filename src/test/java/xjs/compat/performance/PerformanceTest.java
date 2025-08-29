package xjs.compat.performance;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import xjs.compat.serialization.parser.HjsonParser;
import xjs.compat.serialization.parser.UbjsonParser;
import xjs.compat.serialization.util.UBTyping;
import xjs.compat.serialization.writer.HjsonWriter;
import xjs.compat.serialization.writer.UbjsonWriter;
import xjs.data.Json;
import xjs.data.JsonCopy;
import xjs.data.JsonValue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

public class PerformanceTest {

    private static final String SIMPLE_HJSON_SAMPLE =
        "[1234,5,6,7,'abc',\"def\",[[[['ghi',{},{}]]]],true,false,null,'hello','world']";

    private static final String HJSON_SAMPLE = """
        // Comment
        a: 1 # Comment
        b:
          /* Comment */
          2
        c: [ '3a', '3b', '3c' ]
        d: { da: '4a', db: '4b' }
        e: '''
          multiline string
          that's correct,
          this really is
          multiple lines
          '''
        """;

    private static final JsonValue WRITING_SAMPLE =
        Json.parse(HJSON_SAMPLE).copy(JsonCopy.UNFORMATTED | JsonCopy.COMMENTS);

    private static final byte[] SIMPLE_UBJSON_SAMPLE;

    static {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new UbjsonWriter(out, UBTyping.BALANCED).write(WRITING_SAMPLE);
        } catch (IOException ignored) {
            throw new AssertionError("unreachable");
        }
        SIMPLE_UBJSON_SAMPLE = out.toByteArray();
    }

    public static void main(final String... args) throws Exception {
        LocalBenchmarkRunner.runIfEnabled();
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue hjsonParsingSample() {
        try (final HjsonParser parser = new HjsonParser(SIMPLE_HJSON_SAMPLE)) {
            return parser.parse();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String hjsonWritingSample() {
        try (final StringWriter sw = new StringWriter();
                final HjsonWriter writer = new HjsonWriter(sw, true)) {
            writer.write(WRITING_SAMPLE);
            return sw.toString();
        } catch (final Exception ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(true)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue ubjsonParsingSample() {
        try (final InputStream is = new ByteArrayInputStream(SIMPLE_UBJSON_SAMPLE);
                final UbjsonParser parser = new UbjsonParser(is)) {
            return parser.parse();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }

    @Enabled(false)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public byte[] ubjsonWritingSample() {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final UbjsonWriter writer = new UbjsonWriter(out, UBTyping.BALANCED)) {
            writer.write(WRITING_SAMPLE);
            return out.toByteArray();
        } catch (final IOException ignored) {
            throw new AssertionError("unreachable");
        }
    }
}
