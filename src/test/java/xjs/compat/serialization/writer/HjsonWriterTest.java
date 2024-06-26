package xjs.compat.serialization.writer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xjs.data.comments.CommentStyle;
import xjs.data.comments.CommentType;
import xjs.compat.serialization.parser.HjsonParser;
import xjs.data.Json;
import xjs.data.JsonArray;
import xjs.data.JsonCopy;
import xjs.data.JsonContainer;
import xjs.data.JsonObject;
import xjs.data.JsonString;
import xjs.data.JsonValue;
import xjs.data.StringType;
import xjs.data.serialization.JsonContext;
import xjs.data.serialization.writer.JsonWriterOptions;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class HjsonWriterTest {

    @BeforeAll
    static void setup() {
        JsonContext.setEol("\n");
    }

    @AfterAll
    static void teardown() {
        JsonContext.setEol(System.getProperty("line.separator"));
    }

    @Test
    public void write_printsTrue() {
        assertEquals("true", write(Json.value(true)));
    }

    @Test
    public void write_printsFalse() {
        assertEquals("false", write(Json.value(false)));
    }

    @Test
    public void write_printsNull() {
        assertEquals("null", write(Json.value(null)));
    }

    @Test
    public void write_printsInteger() {
        assertEquals("1234", write(Json.value(1234)));
    }

    @Test
    public void write_printsDecimal() {
        assertEquals("12.34", write(Json.value(12.34)));
    }

    @Test
    public void write_printsSingleQuotedString() {
        assertEquals("'test'",
            write(new JsonString("test", StringType.SINGLE),
                new JsonWriterOptions().setOmitQuotes(false)));
    }

    @Test
    public void write_printsDoubleQuotedString() {
        assertEquals("\"test\"",
            write(new JsonString("test", StringType.DOUBLE),
                new JsonWriterOptions().setOmitQuotes(false)));
    }

    @Test
    public void write_printsMultiString() {
        assertEquals("'''\ntest\n'''", write(new JsonString("test", StringType.MULTI)));
    }

    @Test
    public void write_formatsMultiString_forCurrentLevel() {
        assertEquals("[\n  '''\n  test\n  '''\n]",
            write(new JsonArray()
                .add(new JsonString("test", StringType.MULTI))));
    }

    @Test
    public void write_printsUnquotedString() {
        assertEquals("test", write(new JsonString("test", StringType.IMPLICIT)));
    }

    @Test
    public void write_printsEmptyArray() {
        assertEquals("[]", write(new JsonArray()));
    }

    @Test
    public void write_printsEmptyObject() {
        assertEquals("{}", write(new JsonObject()));
    }

    @Test
    public void write_printsCondensedArray() {
        assertEquals("[ 1, 2, 3 ]", write(new JsonArray().add(1).add(2).add(3).condense()));
    }

    @Test
    public void write_printsMultilineArray() {
        assertEquals("[\n  1\n  2\n  3\n]", write(new JsonArray().add(1).add(2).add(3)));
    }

    @Test
    public void write_printsEmptyStrings_inSingleQuotes() {
        final JsonContainer array = new JsonArray()
            .add(new JsonString("", StringType.IMPLICIT))
            .add(new JsonString("", StringType.IMPLICIT))
            .add(new JsonString("", StringType.IMPLICIT))
            .condense();
        assertEquals("[ '', '', '' ]", write(array));
    }

    @Test
    public void write_printsCondensedObject() {
        assertEquals("1: 1, 2: 2, 3: 3",
            write(new JsonObject().add("1", 1).add("2", 2).add("3", 3).condense()));
    }

    @Test
    public void write_printsMultilineObject() {
        assertEquals("1: 1\n2: 2\n3: 3",
            write(new JsonObject().add("1", 1).add("2", 2).add("3", 3)));
    }

    @Test
    public void write_preservesWhitespaceAbove() {
        assertEquals("\n\ntrue", write(Json.value(true).setLinesAbove(2)));
    }

    @Test
    public void write_preservesWhitespaceBetween() {
        assertEquals("a:\n\n\n\n  1234",
            write(new JsonObject().add("a", Json.value(1234).setLinesBetween(4))));
    }

    @Test
    public void writeUnformatted_doesNotPrintWhitespace() {
        assertEquals("1:1,2:2,3:3",
            write(new JsonObject().add("1", 1).add("2", 2).add("3", 3), null));
    }

    @Test
    public void write_printsRecursively() {
        assertEquals("[\n  1\n  []\n]",
            write(new JsonArray().add(1).add(new JsonArray())));
    }

    @Test
    public void write_withoutAllowCondense_printsExpanded() {
        final JsonWriterOptions options = new JsonWriterOptions().setAllowCondense(false);
        assertEquals("[\n  1\n  2\n  3\n]",
            write(new JsonArray().add(1).add(2).add(3).condense(), options));
    }

    @Test
    public void write_preservesComplexFormatting() {
        final JsonObject object =
            new JsonObject()
                .add("1", Json.value(1).setLinesBetween(1))
                .add("2", 2)
                .add("a", new JsonArray()
                    .add(3)
                    .add(4)
                    .add(new JsonObject()
                        .add("5", 5)
                        .add("6", 6)
                        .condense())
                    .setLinesAbove(2));
        final String expected = """
            1:
              1
            2: 2
            
            a: [
              3
              4
              { 5: 5, 6: 6 }
            ]""";
        assertEquals(expected, write(object));
    }

    @Test
    public void write_withLineSpacing_overridesDefaultSpacing() {
        final JsonWriterOptions options = new JsonWriterOptions().setDefaultSpacing(2);
        final JsonObject object =
            new JsonObject()
                .add("1", 2)
                .add("3", 4)
                .add("5", new JsonArray().add(6).add(7).add(8).condense())
                .add("9", 0);
        final String expected = """
            1: 2
              
            3: 4
              
            5: [ 6, 7, 8 ]
              
            9: 0""";
        assertEquals(expected, write(object, options));
    }

    @Test
    public void write_withMinAndMaxSpacing_overridesConfiguredSpacing_ignoringCondensed() {
        final JsonWriterOptions options = new JsonWriterOptions().setMinSpacing(2).setMaxSpacing(2);
        final String input = """
            1: 2
            3: 4
            5: {
              6: [ 7, 8, 9 ]
            }""";
        final String expected = """
            1: 2
            
            3: 4
            
            5: {
              6: [ 7, 8, 9 ]
            }""";
        final JsonObject object = new HjsonParser(input).parse().asObject();
        assertEquals(expected, write(object, options));
    }

    @Test
    public void write_withHeaderOnRootObject_addsImplicitEmptyLine() {
        final JsonObject object = new JsonObject().add("key", "value");
        object.setComment(CommentType.HEADER, CommentStyle.LINE, "header");
        final String expected = """
            // header
            
            key: value""";
        assertEquals(expected, write(object));
    }

    @Test
    public void parse_thenRewrite_preservesComplexFormatting() {
        final String expected = """
            
            # Header
            1:
              # Value
              1
            2: // Value
             
              2 # E0l
            /* header */
            z:
            
              /// Value
              y
            
            a: /* Value */ [
              3, 4
              { 5: 5, 6: 6 /* eol */ }
              [ /* interior */ ]
              
              
              /**
               * Interior 1
               * Interior 2
               */
            
            ] # eol
            
            a1:
              /**
               * 1
               * 2
               */
              block value
            
            enter: {
              level: {
                two: [
                  // header
                  # header
                  yes
                  // header
                  yes
                ]
              }
            }
            
            b: '})](*&(*%#&)!'
            c: { '': '' }
            d: [ '', '', '' ]""";
        assertEquals(expected, write(new HjsonParser(expected).parse()));
    }

    @Test
    public void write_printsHeaderComment() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.setComment(CommentType.HEADER, CommentStyle.HASH, "Header");

        assertEquals("# Header\nv", write(v));
    }

    @Test
    public void write_printsFooterComment() {
        final JsonValue v = new JsonObject();
        v.setComment(CommentType.FOOTER, CommentStyle.LINE, "Footer");

        assertEquals("{}\n// Footer", write(v));
    }

    @Test
    public void write_printsEolComment() {
        final JsonValue v = Json.value(1234);
        v.setComment(CommentType.EOL, CommentStyle.LINE_DOC, "Eol");

        assertEquals("1234 /// Eol", write(v));
    }

    @Test
    public void write_reformatsEolComment_afterUnquotedString() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.setComment(CommentType.EOL, CommentStyle.LINE_DOC, "Eol");

        assertEquals("'v' /// Eol", write(v));
    }

    @Test
    public void write_printsValueComment() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.setComment(CommentType.VALUE, CommentStyle.BLOCK, "Value");

        assertEquals("k: /* Value */ v", write(new JsonObject().add("k", v)));
    }

    @Test
    public void writeValueComment_withLinesBetween_coercesOntoNextLine() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.setComment(CommentType.VALUE, CommentStyle.BLOCK, "Value");
        v.setLinesBetween(1);

        assertEquals("k:\n  /* Value */\n  v", write(new JsonObject().add("k", v)));
    }

    @Test
    public void writeValueComment_withLinesBetween_andLinesBelow_doesNotInsertExtraLines() {
        final JsonValue v = new JsonString("v", StringType.IMPLICIT);
        v.setComment(CommentType.VALUE, CommentStyle.BLOCK, "Value");
        v.setLinesBetween(1);

        assertEquals("k:\n  /* Value */\n  v", write(new JsonObject().add("k", v)));
    }

    @Test
    public void write_printsInteriorComment() {
        final JsonValue v = new JsonArray();
        v.setComment(CommentType.INTERIOR, CommentStyle.MULTILINE_DOC, "Interior");

        assertEquals("[ /** Interior */ ]", write(v));
    }

    @Test
    public void writeInteriorComment_withNewlineInComment_insertsLinesAround() {
        final JsonValue v = new JsonArray();
        v.setComment(CommentType.INTERIOR, CommentStyle.MULTILINE_DOC, "Interior\nLine 2");

        assertEquals("[\n  /**\n   * Interior\n   * Line 2\n   */\n]", write(v));
    }

    @Test
    public void writeInteriorComment_withNewlineAboveComment_insertsLineBelow() {
        final JsonArray v = new JsonArray();
        v.setLinesTrailing(1);
        v.setComment(CommentType.INTERIOR, CommentStyle.MULTILINE_DOC, "Interior");

        assertEquals("[\n  /** Interior */\n]", write(v));
    }

    @Test
    public void write_withSmartSpacing_separatesContainers() {
        final String input = """
            a: 1
            b: 2
            c: {
              c1: '3a'
              c2: '3b'
            }
            d: [
              '4a'
              {}
              '4b'
            ]
            e: 5
            f: 6
            // header
            g: 7
            h: 8""";
        final String expected = """
            a: 1
            b: 2
            
            c: {
              c1: 3a
              c2: 3b
            }
            
            d: [
              4a
              {}
              4b
            ]
            
            e: 5
            f: 6
            
            // header
            g: 7
            
            h: 8""";

        final JsonValue value = new HjsonParser(input).parse().copy(JsonCopy.UNFORMATTED | JsonCopy.COMMENTS);
        assertEquals(expected, write(value, new JsonWriterOptions().setSmartSpacing(true)));
    }

    private static String write(final JsonValue value) {
        return write(value, new JsonWriterOptions());
    }

    private static String write(final JsonValue value, final JsonWriterOptions options) {
        final StringWriter sw = new StringWriter();
        final HjsonWriter writer =
            options != null ? new HjsonWriter(sw, options) : new HjsonWriter(sw, false);
        try {
            writer.write(value);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return sw.toString();
    }
}
