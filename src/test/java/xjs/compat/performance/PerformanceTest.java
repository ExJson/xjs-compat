package xjs.compat.performance;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import xjs.compat.serialization.parser.HjsonParser;
import xjs.compat.serialization.writer.HjsonWriter;
import xjs.data.Json;
import xjs.data.JsonCopy;
import xjs.data.JsonValue;

import java.io.IOException;
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

    private static final JsonValue DJS_WRITING_SAMPLE =
        Json.parse(HJSON_SAMPLE).copy(JsonCopy.UNFORMATTED | JsonCopy.COMMENTS);

    public static void main(final String... args) throws Exception {
        LocalBenchmarkRunner.runIfEnabled();
    }

    @Enabled(true)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public JsonValue djsParsingSample() {
        return new HjsonParser(SIMPLE_HJSON_SAMPLE).parse();
    }

    @Enabled(true)
    @Benchmark
    @Fork(2)
    @Threads(4)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public String djsWritingSample() throws IOException {
        final StringWriter sw = new StringWriter();
        new HjsonWriter(sw, true).write(DJS_WRITING_SAMPLE);
        return sw.toString();
    }
}
