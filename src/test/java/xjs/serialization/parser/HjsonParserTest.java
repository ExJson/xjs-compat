package xjs.serialization.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import xjs.core.CommentType;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.exception.SyntaxException;
import xjs.serialization.writer.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class HjsonParserTest extends CommonParserTest {

    @Test
    public void parse_readsUnquotedStrings() {
        assertEquals("hello", this.parse("hello").asString());
    }

    @ParameterizedTest
    @CsvSource({"1", "true", "\"string\""})
    public void parse_readsAfterNumber_asUnquotedString(final String next) {
        assertEquals("1 " + next, this.parse("1 " + next).asString());
    }

    @Test
    public void parse_readsUnquotedKeys() {
        assertEquals("key", this.parse("{key:'value'}").asObject().keys().get(0));
    }

    @Test
    public void parse_readsMultipleUnquotedKeys() {
        assertTrue(new JsonObject().add("k1", 1).add("k2", 2)
            .matches(this.parse("{k1:1,k2:2}")));
    }

    @Test
    public void parse_readsOpenRoot() {
        assertTrue(new JsonObject().add("a", 1).add("b", 2)
            .matches(this.parse("a:1,b:2")));
    }

    @Test
    public void parse_noValueForLastKey_isEmptyString() {
        assertTrue(new JsonObject().add("k", "")
            .matches(this.parse("k:")));
    }

    @Test
    public void parse_toleratesWhitespace_afterKey() {
        assertTrue(new JsonObject().add("k", "v")
            .matches(this.parse("k\n:\nv")));
    }

    @Test
    public void multipleCommas_inArray_throwsException() {
        assertThrows(SyntaxException.class, () -> this.parse("[,,]"));
    }

    @Test
    public void punctuation_atBeginningOfValue_throwsException() {
        assertThrows(SyntaxException.class, () -> this.parse("k:,"));
    }

    @Test
    public void emptyString_beforeColon_throwsException() {
        assertThrows(SyntaxException.class, () -> this.parse(":"));
    }

    @Test
    public void emptyFile_isImplicitlyAnObject() {
        assertTrue(this.parse("").isObject());
    }

    @Test
    public void parseValue_readsUntilEndOfLine() {
        assertTrue(new JsonObject().add("k", "v").add("r", "t")
            .matches(this.parse("k:v\nr:t")));
    }

    @Test
    public void parseKey_withNewLines_throwsException() {
        assertThrows(SyntaxException.class, () -> this.parse("{k\nk:1}"));
    }

    @Test
    public void parseValue_readsUntilNewLines() {
        assertTrue(new JsonObject().add("k", "v,//not a comment")
            .matches(this.parse("k:v,//not a comment")));
    }

    @Test
    public void parse_readsSingleQuotedString() {
        assertEquals("", this.parse("''").asString());
    }

    @Test
    public void parse_readsMultilineString() {
        assertEquals("test", this.parse("'''test'''").asString());
    }

    @Test
    public void parse_toleratesEmptyMultilineString() {
        assertEquals("", this.parse("''''''").asString());
    }

    @Test
    public void multilineString_ignoresLeadingWhitespace() {
        assertEquals("test", this.parse("'''  test'''").asString());
    }

    @Test
    public void multilineString_ignoresTrailingNewline() {
        assertEquals("test", this.parse("'''test\n'''").asString());
    }

    @Test
    public void multilineString_preservesIndentation_bySubsequentLines() {
        final String text = """
            multi:
              '''
              0
               1
                2
              '''
            """;
        assertEquals("0\n 1\n  2", this.parse(text).asObject().getAsserted("multi").asString());
    }

    @ParameterizedTest
    @CsvSource({"/*header*/", "#header", "//header"})
    public void parse_preservesHeaderComment_atTopOfFile(final String comment) {
        assertEquals(comment,
            this.parse(comment + "\n{}").getComments().getData(CommentType.HEADER));
    }

    @ParameterizedTest
    @CsvSource({"/*footer*/", "#footer", "//footer"})
    public void parse_preservesFooterComment_atBottomOfFile(final String comment) {
        assertEquals(comment,
            this.parse("{}\n" + comment).getComments().getData(CommentType.FOOTER));
    }

    @ParameterizedTest
    @CsvSource({"/*eol*/", "#eol", "//eol"})
    public void parse_preservesEolComment_afterClosingRootBrace(final String comment) {
        assertEquals(comment,
            this.parse("{}" +  comment).getComments().getData(CommentType.EOL));
    }

    @ParameterizedTest
    @CsvSource({"/*header*/", "#header", "//header"})
    public void parse_preservesHeader_aboveValue(final String comment) {
        assertEquals(comment,
            this.parse(comment + "\nk:v").asObject().get(0).getComments().getData(CommentType.HEADER));
    }

    @ParameterizedTest
    @CsvSource({"/*value*/", "#value", "//value"})
    public void parse_preservesValueComment_betweenKeyValue(final String comment) {
        assertEquals(comment + "\n",
            this.parse("k:\n" + comment + "\nv").asObject().get(0).getComments().getData(CommentType.VALUE));
    }

    @ParameterizedTest
    @CsvSource({"/*eol*/", "#eol", "//eol"})
    public void parse_preservesEolComment_afterValue(final String comment) {
        assertEquals(comment,
            this.parse("k:1" + comment).asObject().get(0).getComments().getData(CommentType.EOL));
    }

    @ParameterizedTest
    @CsvSource({"/*interior*/", "#interior", "//interior"})
    public void parse_preservesInteriorComment_inContainer(final String comment) {
        assertEquals(comment + "\n",
            this.parse("{\n" + comment + "\n}").getComments().getData(CommentType.INTERIOR));
    }

    @ParameterizedTest
    @CsvSource({"/*comment*/", "#comment", "//comment"})
    public void parse_preservesNewlines_afterComments(final String comment) {
        assertEquals(comment + "\n",
            this.parse("k1:v1\n" + comment + "\n\nk:v")
                .asObject().get(1).getComments().getData(CommentType.HEADER));
    }

    @Test
    public void parse_readsUntilLastEmptyLine_asHeader() {
        final String header = """
            // header part 1
            // header part 2
            
            // header part 3""";
        final String json = """

            // comment of "key"
            key: value""";

        final JsonValue parsed = this.parse(header + "\n" + json);
        assertEquals(header, parsed.getComments().getData(CommentType.HEADER));
    }

    @Test
    public void parse_preservesEmptyLines_ignoringComments() throws IOException {
        final String json = """
             
             key:
               value
             
             another:
             
               # comment
               value
               
             k3: 3, k4: 4
               
               
             # and
             finally: value
             """;
        final String expected = """
             {
               "key":
                 "value",
               
               "another":
             
                 "value",
             
               "k3": 3, "k4": 4,
             
             
               "finally": "value"
             }""";
        final StringWriter sw = new StringWriter();
        final JsonWriter writer = new JsonWriter(sw, true);
        writer.write(this.parse(json));
        assertEquals(expected.replace("\r", ""), sw.toString().replace("\r", ""));
    }

    @Override
    protected JsonValue parse(final String json) {
        return new HjsonParser(json).parse();
    }
}
