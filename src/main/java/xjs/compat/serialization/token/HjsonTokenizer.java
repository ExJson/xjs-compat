package xjs.compat.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.compat.serialization.util.StringContext;
import xjs.data.StringType;
import xjs.data.serialization.token.NumberToken;
import xjs.data.serialization.token.ParsedToken;
import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenStream;
import xjs.data.serialization.token.TokenType;
import xjs.data.serialization.token.Tokenizer;
import xjs.data.serialization.util.PositionTrackingReader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses Hjson tokens as a subset of XJS tokens, treating Hjson keys
 * and values as two types of WORD tokens, depending on the context.
 */
public class HjsonTokenizer extends Tokenizer {

    protected final StringContext stringContext = new StringContext();

    /**
     * Begins parsing tokens when given a typically ongoing input.
     *
     * @param is Any source of character bytes.
     * @param containerized Whether to generate containers on the fly.
     * @throws IOException If the reader fails to parse any initial bytes.
     */
    public HjsonTokenizer(final InputStream is, final boolean containerized) throws IOException {
        super(is, containerized);
    }

    /**
     * Begins parsing tokens when given a full text as the source.
     *
     * @param text The full text and source of tokens.
     * @param containerized Whether to generate containers on the fly.
     */
    public HjsonTokenizer(final String text, final boolean containerized) {
        super(text, containerized);
    }

    /**
     * Begins parsing tokens from any other source.
     *
     * @param reader A reader providing characters and positional data.
     * @param containerized Whether to generate containers on the fly.
     */
    public HjsonTokenizer(final PositionTrackingReader reader, final boolean containerized) {
        super(reader, containerized);
    }

    /**
     * Generates a lazily-evaluated {@link TokenStream stream of
     * tokens} from the input text.
     *
     * @param reader The source of tokens being parsed
     * @return A new {@link TokenStream}.
     */
    public static TokenStream stream(final PositionTrackingReader reader) {
        return new TokenStream(new HjsonTokenizer(reader, false), TokenType.OPEN);
    }

    @Override
    protected @Nullable Token single() throws IOException {
        final PositionTrackingReader reader = this.reader;
        reader.skipLineWhitespace();
        if (reader.isEndOfText()) {
            return null;
        }
        final char c = (char) reader.current;
        this.stringContext.prepare(c);
        this.startReading();
        final Token t;
        switch (c) {
            case '/', '#' -> t = this.comment(c);
            case '\'', '"' -> t = this.quote(c);
            case '\n' -> t = this.newLine();
            default -> {
                if (this.isPunctuation(c) || c == ':') {
                    reader.read();
                    t = this.newSymbolToken(c);
                } else {
                    t = this.word();
                }
            }
        }
        this.stringContext.update(t);
        return t;
    }

    @Override
    protected Token comment(final char c) throws IOException {
        final PositionTrackingReader reader = this.reader;
        if (c == '#') {
            return reader.readHashComment();
        } else if (reader.peek() == '/') {
            reader.read();
            return reader.readLineComment();
        } else if (reader.peek() == '*') {
            reader.read();
            return reader.readBlockComment();
        }
        return this.word();
    }

    @Override
    protected Token word() throws IOException {
        if (this.stringContext.isExpectingKey()) {
            return this.key();
        } else if (this.stringContext.isAmbiguous()) {
            return this.ambiguous();
        }
        return this.unquoted();
    }

    protected Token key() throws IOException {
        final PositionTrackingReader reader = this.reader;
        reader.startCapture();
        do { // can safely assume first char is legal
            final char c = (char) reader.current;
            if (!this.isLegalKeyCharacter(c)) {
                break;
            }
            reader.read();
        } while (!reader.isEndOfText());
        return this.newWordToken(reader.endCapture());
    }

    // best attempt at resolving key vs value without lookahead
    // if the input is a valid kw or number, contains comment-like
    // symbols or punctuation, and is followed by a colon or nl,
    // we are not able to read it correctly as a key.
    // for example, the following text in hjson is bizarrely a key:
    //   true//:
    protected Token ambiguous() throws IOException {
        final PositionTrackingReader reader = this.reader;
        reader.startCapture();

        while (true) {
            if (reader.isEndOfText()) {
                final String prefix = reader.endCapture();
                final Token t = this.asKwOrNum(prefix);
                if (t != null) return t;
                return this.newUnquoted(prefix);
            } else if (reader.current == ':') {
                return this.newUnquoted(reader.endCapture());
            } else if (reader.isWhitespace()) {
                final String prefix = reader.endCapture();
                reader.startCapture();
                reader.skipLineWhitespace();
                if (reader.current == ':' || reader.current == '\n') {
                    reader.invalidateCapture();
                    return this.newUnquoted(prefix);
                }
                if (this.canBeEndOfKwOrNum()) {
                    final Token t = this.asKwOrNum(prefix);
                    if (t != null) return t;
                }
                final String remaining = reader.endCapture(reader.skipToNL());
                return this.newUnquoted(prefix + remaining);
            } else if (this.canBeEndOfKwOrNum()) {
                final String prefix = reader.endCapture();
                final Token t = this.asKwOrNum(prefix);
                if (t != null) return t;
                reader.startCapture();
                final String remaining = reader.endCapture(reader.skipToNL());
                return this.newUnquoted(prefix + remaining);
            }
            reader.read();
        }
    }

    protected Token unquoted() throws IOException {
        final PositionTrackingReader reader = this.reader;
        reader.startCapture();

        while (!reader.isEndOfText()) {
            if (reader.isWhitespace()) {
                final String prefix = reader.endCapture();
                reader.startCapture();
                reader.skipLineWhitespace();
                if (this.canBeEndOfKwOrNum()) {
                    final Token t = this.asKwOrNum(prefix);
                    if (t != null) return t;
                }
                final String remaining = reader.endCapture(reader.skipToNL());
                return this.newUnquoted(prefix + remaining);
            } else if (this.canBeEndOfKwOrNum()) {
                // true, false, null, or number, then punctuation or comment -> end of value
                final String prefix = reader.endCapture();
                final Token t = this.asKwOrNum(prefix);
                if (t != null) return t;
                reader.startCapture();
                final String remaining = reader.endCapture(reader.skipToNL());
                return this.newUnquoted(prefix + remaining);
            }
            reader.read();
        }
        // hit end of text, no whitespace encountered
        final String parsed = reader.endCapture();
        final Token t = this.asKwOrNum(parsed);
        if (t != null) return t;
        return this.newUnquoted(parsed);
    }

    protected @Nullable Token asKwOrNum(final String text) {
        switch (text) {
            case "true", "false", "null" -> {
                final int e = this.index + text.length();
                return this.newWordToken(text, e);
            }
            default -> {
                if (this.isOctalFormat(text)) {
                    return null; // disallow octal format
                }
                try {
                    final double n = Double.parseDouble(text);
                    final int e = this.index + text.length();
                    return this.newNumberToken(text, n, e);
                } catch (final NumberFormatException ignored) {}
                return null;
            }
        }
    }

    protected boolean isOctalFormat(final String text) {
        return text.length() > 1 && text.charAt(0) == '0' && Character.isDigit(text.charAt(1));
    }

    protected boolean isLegalKeyCharacter(final char c) {
        return !this.isWhitespace(c) && c != ':' && !this.isPunctuation(c);
    }

    protected boolean isWhitespace(final char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    protected boolean isPunctuation(final char c) {
        return c == ',' || c == '{' || c == '}' || c == '[' || c == ']';
    }

    protected boolean canBeEndOfKwOrNum() throws IOException {
        final char c = (char) this.reader.current;
        if (c == '\n' || c == ',' || c == '}' || c == ']' || c == '#') {
            return true;
        } else if (c == '/') {
            final int peek = this.reader.peek();
            return peek == '/' || peek == '*';
        }
        return false;
    }

    protected Token newUnquoted(final String text) {
        return this.newStringToken(text, StringType.IMPLICIT);
    }
    
    protected Token newNumberToken(final String text, final double number, final int e) {
        return new NumberToken(this.index, e, this.line, this.column, number, text);
    }

    protected Token newWordToken(final String capture, final int e) {
        return new ParsedToken(this.index, e, this.line, this.column, TokenType.WORD, capture);
    }
}
