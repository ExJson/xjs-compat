package xjs.compat.serialization.writer;

import xjs.comments.CommentType;
import xjs.core.JsonContainer;
import xjs.core.JsonValue;
import xjs.core.JsonReference;
import xjs.core.StringType;
import xjs.serialization.writer.JsonWriterOptions;
import xjs.serialization.writer.XjsWriter;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

/**
 * A writer providing compatibility with Hjson files.
 */
public class HjsonWriter extends XjsWriter {

    public HjsonWriter(final File file, final boolean format) throws IOException {
        super(file, format);
    }

    public HjsonWriter(final Writer writer, final boolean format) {
        super(writer, format);
    }

    public HjsonWriter(final File file, final JsonWriterOptions options) throws IOException {
        super(file, options);
    }

    public HjsonWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    protected boolean shouldSeparateOpener() {
        final JsonContainer c = this.parent();
        return this.format
            && this.allowCondense
            && this.level > 0
            && c.size() > 0
            && this.getLinesAbove(this.getFirst(this.parent())) == 0;
    }

    @Override
    protected boolean shouldSeparateCloser() {
        return this.level > 0 && this.isCondensed();
    }

    @Override
    protected void delimit() throws IOException {
        if (this.peek == null) {
            return;
        }
        if (!this.format) {
            this.tw.write(',');
        } else if (this.allowCondense && this.getLinesAbove(this.peek()) == 0) {
            this.tw.write(',');
            this.tw.write(this.separator);
        }
    }

    @Override
    protected StringType getKeyType(final String key) {
        if (key.isEmpty()) {
            return StringType.SINGLE;
        }
        boolean whitespaceFound = false;
        for (int i = 0; i < key.length(); i++) {
            final char c = key.charAt(i);
            if (c == '\'') {
                return StringType.DOUBLE;
            } else if (c == '"') {
                return StringType.SINGLE;
            }
            whitespaceFound |= Character.isWhitespace(c);
        }
        return whitespaceFound ? StringType.SINGLE : StringType.IMPLICIT;
    }

    protected void writeString(final JsonValue value) throws IOException {
        switch (this.getStringType(value)) {
            case SINGLE -> this.writeQuoted(value.asString(), '\'');
            case DOUBLE -> this.writeQuoted(value.asString(), '"');
            case MULTI -> this.writeMulti(value.asString());
            case IMPLICIT -> this.tw.write(value.asString());
            default -> throw new IllegalStateException("unreachable");
        }
    }

    protected StringType getStringType(final JsonValue value) {
        final StringType type = StringType.fromValue(value);
        final String s = value.asString();

        if (this.isCondensed() && (type == StringType.IMPLICIT || type == StringType.NONE)) {
            return s.contains("'") ? StringType.DOUBLE : StringType.SINGLE;
        } else if (type == StringType.MULTI) {
            return type;
        } else if (type == StringType.SINGLE || type == StringType.DOUBLE) {
            return this.omitQuotes && this.canBeImplicit(s) ? StringType.IMPLICIT : type;
        } else if (type == StringType.IMPLICIT) {
            return this.checkImplicitString(value, s);
        } else if (type == StringType.NONE) {
            return this.selectStringType(s);
        }
        return type;
    }

    protected boolean canBeImplicit(final String s) {
        return !s.isEmpty() && this.isValidFirstChar(s.charAt(0)) && !s.contains("\n");
    }

    protected StringType checkImplicitString(final JsonValue value,  final String s) {
        if (s.contains("\n")) {
            return StringType.MULTI;
        } else if (value.hasComment(CommentType.EOL)) {
            return s.contains("'") ? StringType.DOUBLE : StringType.SINGLE;
        }
        return StringType.IMPLICIT;
    }

    protected StringType selectStringType(final String s) {
        if (s.isEmpty()) {
            return StringType.SINGLE;
        } else if (s.contains("\n")) {
            return StringType.MULTI;
        }
        return StringType.IMPLICIT;
    }

    protected boolean isValidFirstChar(final char c) {
        return c != '{' && c != '}' && c != '[' && c != ']' && c != ',' && c != ':';
    }

    @Override
    protected boolean isCondensed(final JsonContainer c) {
        if (!this.format) {
            return true;
        } else if (c == null || !this.allowCondense) {
            return false;
        } else if (this.level < 1) {
            return this.isOpenRootCondensed(c);
        }
        // Use a stricter algorithm to tolerate Hjson's stricter syntax rules
        for (final JsonReference reference : c.references()) {
            if (reference.getOnly().getLinesAbove() == 0) {
                return true;
            }
        }
        return false;
    }

    protected boolean isOpenRootCondensed(final JsonContainer c) {
        if (c.size() < 2) {
            return false;
        }
        // Ignore the first value in an open root
        for (int i = 1; i < c.size(); i++) {
            if (c.getReference(i).getOnly().getLinesAbove() == 0) {
                return true;
            }
        }
        return false;
    }
}
