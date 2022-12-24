package xjs.serialization.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xjs.core.CommentType;
import xjs.core.Json;
import xjs.core.JsonArray;
import xjs.core.JsonLiteral;
import xjs.core.JsonObject;
import xjs.core.JsonString;
import xjs.core.JsonValue;
import xjs.core.StringType;
import xjs.exception.SyntaxException;
import xjs.serialization.token.HjsonTokenizer;
import xjs.serialization.token.NumberToken;
import xjs.serialization.token.StringToken;
import xjs.serialization.token.SymbolToken;
import xjs.serialization.token.Token;
import xjs.serialization.token.Token.Type;
import xjs.serialization.token.TokenStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A parser providing compatibility with Hjson files.
 */
public class HjsonParser extends CommentedTokenParser {

    public HjsonParser(final File file) throws IOException {
        super(new TokenStream(new HjsonTokenizer(new FileInputStream(file)), Type.OPEN));
    }

    public HjsonParser(final String text) {
        super(new TokenStream(new HjsonTokenizer(text), Type.OPEN));
    }

    public HjsonParser(final TokenStream root) {
        super(root);
    }

    @Override
    public @NotNull JsonValue parse() {
        if (this.root.type() == Type.OPEN) {
            this.read();
        }
        this.readWhitespace();
        if (this.isEndOfContainer()) {
            return new JsonObject();
        } else if (this.isOpenRoot()) {
            return this.readOpenRoot();
        }
        return this.readClosedRoot();
    }

    protected boolean isOpenRoot() {
        final Type type = this.current.type();
        if (type == Type.SYMBOL) { // punctuation
            return false;
        }
        final Token peek = this.peekWhitespace();
        if (peek == null) {
            return false;
        }
        return peek.isSymbol(':')
            && this.current.textOf(this.reference)
                .matches("\\S+");
    }

    protected @Nullable Token peekWhitespace() {
        Token peek = this.iterator.peek();
        int peekAmount = 1;
        while (peek != null) {
            switch (peek.type()) {
                case BREAK:
                case HASH_COMMENT:
                case LINE_COMMENT:
                case BLOCK_COMMENT:
                    peek = this.iterator.peek(++peekAmount);
                    break;
                default:
                    return peek;
            }
        }
        return null;
    }

    protected JsonValue readOpenRoot() {
        final JsonObject object = new JsonObject();
        this.splitOpenHeader(object);
        do {
            this.readWhitespace(false);
            if (this.isEndOfContainer()) {
                break;
            }
        } while (this.readNextMember(object));
        this.readBottom();
        return this.takeFormatting(object);
    }

    protected JsonValue readClosedRoot() {
        this.setAbove();
        final JsonValue result = this.readValue();
        this.readAfter();
        this.readBottom();
        return this.takeFormatting(result);
    }

    protected JsonValue readValue() {
        if (this.current.isSymbol('{')) {
            return this.readObject();
        } else if (this.current.isSymbol('[')) {
            return this.readArray();
        }
        final JsonValue value = this.readUnquoted();
        this.read();
        return value;
    }

    protected JsonObject readObject() {
        final JsonObject object = new JsonObject();
        if (!this.open('{', '}')) {
            return this.close(object, '}');
        }
        do {
            this.readWhitespace(false);
            if (this.isEndOfContainer('}')) {
                return this.close(object, '}');
            }
        } while (this.readNextMember(object));
        return this.close(object, '}');
    }

    protected boolean readNextMember(final JsonObject object) {
        this.setAbove();

        final String key = this.readKey();
        this.readBetween(':');
        final JsonValue value = this.readValue();

        object.add(key, value);

        final boolean delimiter = this.readDelimiter();
        this.takeFormatting(value);
        return delimiter;
    }

    protected String readKey() {
        if (this.current instanceof SymbolToken) {
            final char c = ((SymbolToken) this.current).symbol;
            if (c == ':') {
                throw this.emptyKey();
            } else if (this.isPunctuationChar(c)) {
                throw this.punctuationInKey(c);
            }
        }
        final String text;
        if (this.current instanceof StringToken) {
            text = ((StringToken) this.current).parsed;
        } else {
            text = this.current.textOf(this.reference);
        }
        final Token peek = this.peekWhitespace();
        if (peek != null && peek.type() != Type.SYMBOL) {
            throw this.whitespaceInKey();
        }
        this.read();
        return text;
    }

    protected JsonArray readArray() {
        final JsonArray array = new JsonArray();
        if (!this.open('[', ']')) {
            return this.close(array, ']');
        }
        do {
            this.readWhitespace(false);
            if (this.isEndOfContainer(']')) {
                return this.close(array, ']');
            }
        } while (this.readNextElement(array));
        return this.close(array, ']');
    }

    protected boolean readNextElement(final JsonArray array) {
        this.setAbove();

        final JsonValue value = this.readValue();
        array.add(value);

        final boolean delimiter = this.readDelimiter();
        this.takeFormatting(value);
        return delimiter;
    }

    protected boolean readDelimiter() {
        this.readLineWhitespace();
        if (this.readIf(',')) {
            this.readLineWhitespace();
            this.readNl();
            this.setComment(CommentType.EOL);
            return true;
        } else if (this.readNl()) {
            this.setComment(CommentType.EOL);
            this.readWhitespace(false);
            this.readIf(',');
            return true;
        } else if (this.isEndOfContainer()) {
            this.setComment(CommentType.EOL);
        }
        return false;
    }

    protected JsonValue readUnquoted() {
        if (this.current instanceof NumberToken) {
            final JsonValue full = this.checkAfterUnquoted();
            if (full != null) {
                return full;
            }
            return Json.value(((NumberToken) this.current).number);
        } else if (this.current instanceof StringToken) {
            final StringToken t = (StringToken) this.current;
            return new JsonString(t.parsed, this.getStringType(t));
        } else if (this.current instanceof SymbolToken) {
            final char c = ((SymbolToken) this.current).symbol;
            // Tokenizer only returns symbols for punctuation.
            assert this.isPunctuationChar(c);
            throw this.punctuationInValue(c);
        }
        final String text = this.current.textOf(this.reference);
        switch (text) {
            case "true": return JsonLiteral.jsonTrue();
            case "false": return JsonLiteral.jsonFalse();
            case "null": return JsonLiteral.jsonNull();
            default: return new JsonString(text);
        }
    }

    protected @Nullable JsonValue checkAfterUnquoted() {
        final Token peek = this.iterator.peek();
        if (peek == null) {
            return null;
        }
        final Type type = peek.type();
        if (type == Type.BREAK
                || type == Type.HASH_COMMENT
                || type == Type.LINE_COMMENT
                || type == Type.BLOCK_COMMENT
                || type == Type.SYMBOL) {
            return null;
        }
        final int s = this.current.start();
        if (this.skipTo(',', true, true) > 0) {
            return new JsonString(
                this.iterator.getText(s, this.current.end()));
        }
        return null;
    }

    protected StringType getStringType(final StringToken t) {
        switch (t.type()) {
            case SINGLE_QUOTE: return StringType.SINGLE;
            case DOUBLE_QUOTE: return StringType.DOUBLE;
            case TRIPLE_QUOTE: return StringType.MULTI;
        }
        throw new IllegalStateException("unreachable");
    }

    protected boolean isPunctuationChar(final char c) {
        return c == '{' || c == '}' || c == '[' || c == ']' || c == ',' || c == ':';
    }

    protected SyntaxException emptyKey() {
        return this.expected("key (for an empty key name use quotes)");
    }

    protected SyntaxException whitespaceInKey() {
        return this.unexpected("whitespace in key (use quotes to include)");
    }

    protected SyntaxException punctuationInKey(final char c) {
        return this.unexpected("punctuation ('" + c + "') in key (use quotes to include)");
    }

    protected SyntaxException punctuationInValue(final char c) {
        return this.unexpected("punctuation ('" + c + "') in value (use quotes to include)");
    }

    @Override
    public void close() throws Exception {
        this.root.close();
    }
}
