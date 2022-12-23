package xjs.serialization.token;

import org.jetbrains.annotations.Nullable;
import xjs.serialization.token.Token.Type;
import xjs.serialization.util.PositionTrackingReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

/**
 * Parses Hjson tokens as a subset of XJS tokens, treating Hjson keys
 * and values as two types of WORD tokens, depending on the context.
 */
public class HjsonTokenizer extends Tokenizer {

    protected final BitSet arrays = new BitSet();

    // The first token is ambiguous in Hjson.
    protected boolean expectingValue = false;
    protected boolean colonRead = false;
    protected boolean top = true;
    protected int level = 0;

    /**
     * Begins parsing tokens when given a typically ongoing input.
     *
     * @param is Any source of character bytes.
     * @throws IOException If the reader fails to parse any initial bytes.
     */
    public HjsonTokenizer(final InputStream is) throws IOException {
        super(is);
    }

    /**
     * Begins parsing tokens when given a full text as the source.
     *
     * @param text The full text and source of tokens.
     */
    public HjsonTokenizer(final String text) {
        super(text);
    }

    /**
     * Begins parsing tokens from any other source.
     *
     * @param reader A reader providing characters and positional data.
     */
    public HjsonTokenizer(final PositionTrackingReader reader) {
        super(reader);
    }

    @Override
    protected @Nullable Token single() throws IOException {
        final Token single = super.single();
        this.updateContext(single);
        return single;
    }

    protected void updateContext(final Token t) {
        if (t instanceof SymbolToken) {
            final char symbol = ((SymbolToken) t).symbol;
            if (symbol == '{') {
                this.push(false);
            } else if (symbol == '[') {
                this.push(true);
            } else if (symbol == '}' || symbol == ']') {
                this.pop();
            } else if (symbol == ':') {
                this.colonRead = true;
            }
        } else {
            this.colonRead = false;
        }
        this.top = false;
    }

    protected void push(final boolean array) {
        this.arrays.set(++this.level, array);
        this.expectingValue = array;
        this.colonRead = array;
    }

    protected void pop() {
        if (this.level == 0) {
            this.expectingValue = false;
            return;
        }
        final boolean flag = this.arrays.get(--this.level);
        this.expectingValue = flag;
        this.colonRead = flag;
    }

    @Override
    protected Token word(final int i, final int l, final int o) throws IOException {
        if (this.isReadingKey()) {
            return key(i, l, o);
        }
        final char c = (char) reader.current;
        if (this.isPunctuation(c)) {
            reader.read();
            return new SymbolToken(i, reader.index, l, o, c);
        }
        return unquoted(i, l, o);
    }

    protected boolean isReadingKey() {
        return !this.expectingValue && !this.colonRead;
    }

    protected Token key(final int i, final int l, final int o) throws IOException {
        char c = (char) reader.current;
        if (c != '_' && !Character.isLetterOrDigit(c)) {
            reader.read();
            return new SymbolToken(i, reader.index, l, o, c);
        }
        do {
            if (this.isLegalKeyCharacter(c)) {
                reader.read();
            } else if (reader.index - i == 0) {
                reader.read();
                return new SymbolToken(i, reader.index, l, o, c);
            } else {
                break;
            }
            c = (char) reader.current;
        } while (!reader.isEndOfText());

        if (this.top) {
            return this.checkFirstOpenKey(i, l, o);
        }
        return new Token(i, reader.index, l, o, Type.WORD);
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

    protected Token checkFirstOpenKey(final int i, final int l, final int o) throws IOException {
        final int e = reader.index;
        reader.skipLineWhitespace();
        if (reader.current == ':' || reader.current == '\n') {
            return new Token(i, e, l, o, Type.WORD);
        }
        return unquoted(i, l, o);
    }

    protected Token unquoted(final int i, final int l, final int o) throws IOException {
        final char c = (char) reader.current;
        if (this.isPunctuation(c)) {
            reader.read();
            return new SymbolToken(i, i + 1, l, o, c);
        }
        final int last = reader.skipToNL();
        final int e = Math.min(last + 1, reader.getFullText().length());
        return new Token(i, e, l, o, Type.WORD);
    }
}
