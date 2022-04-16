package xjs.serialization.writer;

import xjs.core.CommentType;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.core.JsonReference;
import xjs.core.StringType;

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

    public HjsonWriter(final Writer writer, final JsonWriterOptions options) {
        super(writer, options);
    }

    @Override
    protected void write(final JsonValue value, final int level) throws IOException {
        this.write(value, false, level);
    }

    protected void write(final JsonValue value, final boolean parentCondensed, final int level) throws IOException {
        final boolean condensed = this.isCondensed(value);
        JsonValue previous = null;

        switch (value.getType()) {
            case OBJECT:
                this.open(condensed, '{');
                for (final JsonObject.Member member : value.asObject()) {
                    this.writeNextMember(previous, member, condensed, level);
                    previous = member.getOnly();
                }
                this.writeEolComment(level, previous, null);
                this.close(value.asObject(), condensed, level, '}');
                break;
            case ARRAY:
                this.open(condensed, '[');
                for (final JsonValue v : value.asArray().visitAll()) {
                    this.writeNextElement(previous, v, condensed, level);
                    previous = v;
                }
                this.writeEolComment(level, previous, null);
                this.close(value.asArray(), condensed, level, ']');
                break;
            case NUMBER:
                this.writeNumber(value.asDouble());
                break;
            case STRING:
                this.writeString(value, parentCondensed, level);
                break;
            default:
                this.tw.write(value.toString());
        }
    }

    @Override
    protected void writeNextMember(
            final JsonValue previous, final JsonObject.Member member, final boolean condensed, final int level) throws IOException {
        this.delimit(previous, member.getOnly());
        this.writeEolComment(level, previous, member.getOnly());
        this.writeLinesAbove(level + 1, previous == null, condensed, member.getOnly());
        this.writeHeader(level + 1, member.getOnly());
        this.writeString(member.getKey(), level);
        this.tw.write(':');
        this.separate(level + 2, member.getOnly());
        this.writeValueComment(level + 2, member.getOnly());
        this.write(member.getOnly(), condensed, level + 1);
    }

    @Override
    protected void writeNextElement(
            final JsonValue previous, final JsonValue value, final boolean condensed, final int level) throws IOException {
        this.delimit(previous, value);
        this.writeEolComment(level, previous, value);
        this.writeLinesAbove(level + 1, previous == null, condensed, value);
        this.writeHeader(level + 1, value);
        this.write(value, condensed, level + 1);
    }

    @Override
    protected void delimit(final JsonValue previous, final JsonValue next) throws IOException {
        if (previous == null) {
            return;
        }
        if (!this.format) {
            this.tw.write(',');
        } else if (next.getLinesAbove() == 0 && this.allowCondense) {
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

    protected void writeString(final JsonValue value, final boolean condensed, int level) throws IOException {
        switch (this.getStringType(value, condensed)) {
            case SINGLE:
                this.writeQuoted(value.asString(), '\'');
                break;
            case DOUBLE:
                this.writeQuoted(value.asString(), '"');
                break;
            case MULTI:
                this.writeMulti(value.asString(), level + 1);
                break;
            case IMPLICIT:
                this.tw.write(value.asString());
                break;
            default:
                throw new IllegalStateException("unreachable");
        }
    }

    protected StringType getStringType(final JsonValue value, final boolean condensed) {
        final StringType type = StringType.fromValue(value);
        final String s = value.asString();

        if (condensed && (type == StringType.IMPLICIT || type == StringType.NONE)) {
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
    protected boolean isCondensed(final JsonValue value) {
        if (!this.format) {
            return true;
        }
        // Use a stricter algorithm to tolerate Hjson's stricter syntax rules
        if (this.allowCondense && value.isContainer()) {
            for (final JsonReference reference : value.asContainer().references()) {
                if (reference.getOnly().getLinesAbove() == 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
