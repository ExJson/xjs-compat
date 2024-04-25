package xjs.compat.serialization.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xjs.data.StringType;
import xjs.data.comments.CommentStyle;
import xjs.data.exception.SyntaxException;
import xjs.data.serialization.token.CommentToken;
import xjs.data.serialization.token.NumberToken;
import xjs.data.serialization.token.StringToken;
import xjs.data.serialization.token.SymbolToken;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.token.TokenType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HjsonTokenizerTest {

    @Test
    public void single_parsesLineComment() {
        final String reference = "// Hello, world!";
        assertEquals(
            comment(reference, CommentStyle.LINE, "Hello, world!"),
            single(reference));
    }

    @Test
    public void single_parsesHashComment() {
        final String reference = "# Hello, world!";
        assertEquals(
            comment(reference, CommentStyle.HASH, "Hello, world!"),
            single(reference));
    }

    @Test
    public void single_parseBlockComment() {
        final String reference = "/*\nHello\nworld!\n*/";
        assertEquals(
            comment(reference, CommentStyle.BLOCK, "Hello\nworld!"),
            single(reference));
    }

    @Test
    public void single_parsesDoubleQuote() {
        final String reference = "\"Hello, world!\"";
        assertEquals(
            string(reference, StringType.DOUBLE, "Hello, world!"),
            single(reference));
    }

    @Test
    public void single_parsesSingleQuote() {
        final String reference = "'Hello, world!'";
        assertEquals(
            string(reference, StringType.SINGLE, "Hello, world!"),
            single(reference));
    }

    @Test
    public void single_parsesTripleQuote() {
        final String reference = "'''\nHello\nworld!\n'''";
        assertEquals(
            string(reference, StringType.MULTI, "Hello\nworld!"),
            single(reference));
    }

    @Test
    public void single_parsesInteger() {
        final String reference = "1234";
        assertEquals(
            number(reference, 1234),
            single(reference));
    }

    @Test
    public void single_parsesDecimal() {
        final String reference = "1234.5";
        assertEquals(
            number(reference, 1234.5),
            single(reference));
    }

    @Test
    public void single_parsesNegativeInteger() {
        final String reference = "-1234";
        assertEquals(
            number(reference, -1234),
            single(reference));
    }

    @Test
    public void single_parsesNegativeDecimal() {
        final String reference = "-1234.5";
        assertEquals(
            number(reference, -1234.5),
            single(reference));
    }

    @Test
    public void single_parsesScientificNumber() {
        final String reference = "1234.5E6";
        assertEquals(
            number(reference, 1234.5E6),
            single(reference));
    }

    @Test
    public void single_parsesScientificNumber_withExplicitSign() {
        final String reference = "1234.5e+6";
        assertEquals(
            number(reference, 1234.5E+6),
            single(reference));
    }

    @Test
    public void single_parsesSignAfterNumber_asUnquotedString() {
        final String reference = "1234e+";
        assertEquals(
            string(reference, StringType.IMPLICIT, reference),
            single(reference));
    }

    @Test
    public void single_parsesLeadingZero_asUnquotedString() {
        final String reference = "01234";
        assertEquals(
            string(reference, StringType.IMPLICIT, reference),
            single(reference));
    }

    @Test
    public void single_parsesLeadingZero_withDecimal_asNumber() {
        final String reference = "0.1234";
        assertEquals(
            number(reference, 0.1234),
            single(reference));
    }

    @Test
    public void single_parsesSingleZero_asNumber() {
        final String reference = "0";
        assertEquals(
            number(reference, 0),
            single(reference));
    }

    @Test
    public void single_parsesSingleZero_withDecimal_asNumber() {
        final String reference = "0.";
        assertEquals(
            number(reference, 0),
            single(reference));
    }

    @Test
    public void single_parsesBreak() {
        final String reference = "\n";
        assertEquals(
            token(reference, TokenType.BREAK),
            single(reference));
    }

    @Test
    public void single_readsContainerElements_asSymbols() {
        final String reference = " {hello}";
        assertEquals(
            symbol('{', 1, 2),
            single(reference));
    }

    @Test
    public void single_parsesNonPunctuatedText_asUnquotedString() {
        final String reference = "this is a test";
        assertEquals(
            string(reference, StringType.IMPLICIT, reference),
            single(reference));
    }

    @Test
    public void single_parsesLegalToken_beforeComma_asNonString() {
        final String reference = "true, hjson is sometimes bad";
        assertEquals(
            token(TokenType.WORD, 0, 4),
            single(reference));
    }

    @Test
    public void single_parsesNonLegalToken_beforeComma_andComma_asUnquotedString() {
        final String reference = "this, is all a string // even this";
        assertEquals(
            string(reference, StringType.IMPLICIT, reference),
            single(reference));
    }

    @Test
    public void single_parsesNumberBeforeColon_asWordOrString() {
        final String number = "1";
        final String reference = number + ": true";
        assertEquals(
            string(StringType.IMPLICIT, 0, 1, number),
            single(reference));
    }

    @Test
    public void single_afterColon_includesFullLine_inUnquotedString() {
        final String key = "hello";
        final String value = "world: test12,//#/*";
        final String reference = key + ": " + value;
        assertEquals(
            stream(TokenType.OPEN, 0, 26,
                string(StringType.IMPLICIT, 0, 5, key),
                symbol(':', 5, 6),
                string(StringType.IMPLICIT, 7, 26, value)),
            all(reference));
    }

    @Test
    public void single_whenAmbiguous_readsKwOrNum_asKwOrNum() {
        final String reference = "1234";
        assertEquals(
            number(reference, 1234.0),
            single(reference));
    }

    @Test
    public void single_whenAmbiguous_readsKwOrNum_withCommentOrPunctuation_asKwOrNum() {
        final String num = "12.34";
        final String reference = num + " // comment";
        assertEquals(
            number(num, 12.34),
            single(reference));
    }

    @Test
    public void single_whenAmbiguous_readsKwOrNum_withMoreText_asText() {
        final String reference = "123e4 more text";
        assertEquals(
            string(reference, StringType.IMPLICIT, reference),
            single(reference));
    }

    @Test
    public void single_inValue_readsKwOrNum_asKwOrNum() {
        final String key = "key";
        final String value = "true";
        final String reference = key + ": " + value;
        assertEquals(
            stream(TokenType.OPEN, 0, 9,
                string(StringType.IMPLICIT, 0, 3, key),
                symbol(':', 3, 4),
                token(TokenType.WORD, 5, 9)),
            all(reference));
    }

    @Test
    public void single_inValue_readsKwOrNum_withCommentOrPunctuation_asKwOrNum() {
        final String key = "key";
        final String value = "true";
        final String commentText = "comment";
        final String comment = "// " + commentText;
        final String reference = key + ": " + value + " " + comment;
        assertEquals(
            stream(TokenType.OPEN, 0, 20,
                string(StringType.IMPLICIT, 0, 3, key),
                symbol(':', 3, 4),
                token(TokenType.WORD, 5, 9),
                comment(CommentStyle.LINE, 10, 20, commentText)),
            all(reference));
    }

    @Test
    public void single_inValue_readsKwOrNum_withMoreText_asText() {
        final String key = "key";
        final String value = "true more text";
        final String reference = key + ": " + value;
        assertEquals(
            stream(TokenType.OPEN, 0, 19,
                string(StringType.IMPLICIT, 0, 3, key),
                symbol(':', 3, 4),
                string(StringType.IMPLICIT, 5, 19, value)),
            all(reference));
    }

    @ValueSource(strings = {"'", "\"", "'''"})
    @ParameterizedTest
    public void single_doesNotTolerate_unclosedQuote(final String quote) {
        final String reference = quote + "hello, world!";
        assertThrows(SyntaxException.class, () ->
            single(reference));
    }

    @Test
    public void single_doesNotTolerate_unclosedMultiLineComment() {
        final String reference = "/*hello, world!";
        assertThrows(SyntaxException.class, () ->
            single(reference));
    }

    @Test
    public void stream_returnsLazilyEvaluatedTokens() {
        final TokenStream stream = HjsonTokenizer.stream("1234");
        stream.iterator().next();
        assertEquals(1, stream.viewTokens().size());
    }

    private static Token single(final String reference) {
        try {
            return new HjsonTokenizer(reference, false).next();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Token all(final String reference) {
        return HjsonTokenizer.stream(reference).readToEnd();
    }

    private static Token token(final String reference, final TokenType type) {
        return token(type, 0, reference.length());
    }

    private static Token comment(final String reference, final CommentStyle type, final String parsed) {
        return new CommentToken(0, reference.length(), 0, lines(reference), 0, type, parsed);
    }

    private static Token comment(final CommentStyle type, final int s, final int e, final String parsed) {
        return new CommentToken(s, e, 0, 0, s, type, parsed);
    }

    private static Token string(final String reference, final StringType type, final String parsed) {
        return new StringToken(0, reference.length(), 0, lines(reference), 0, type, parsed);
    }

    private static Token string(final StringType type, final int s, final int e, final String parsed) {
        return new StringToken(s, e, 0, s, type, parsed);
    }

    private static Token token(final TokenType type, final int s, final int e) {
        return new Token(s, e, 0, s, type);
    }

    private static Token number(final String reference, final double number) {
        return new NumberToken(0, reference.length(), 0, 0, number);
    }

    private static Token symbol(final String reference, final char symbol) {
        return symbol(symbol, 0, reference.length());
    }

    private static Token symbol(final char symbol, final int s, final int e) {
        return new SymbolToken(s, e, 0, s, symbol);
    }

    private static Token stream(
            final TokenType type, final int s, final int e, final Token... tokens) {
        return new TokenStream(s, e, 0, 0, s, type, List.of(tokens));
    }

    private static int lines(final String reference) {
        return (int) reference.lines().count() - 1;
    }
}
