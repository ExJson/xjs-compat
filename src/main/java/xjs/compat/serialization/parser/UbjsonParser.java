package xjs.compat.serialization.parser;

import org.jetbrains.annotations.NotNull;
import xjs.data.Json;
import xjs.data.JsonArray;
import xjs.data.JsonLiteral;
import xjs.data.JsonObject;
import xjs.data.JsonReference;
import xjs.data.JsonValue;
import xjs.data.serialization.parser.ValueParser;
import xjs.compat.serialization.util.UBMarker;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UbjsonParser implements ValueParser {
    protected final InputStream input;

    public UbjsonParser(final File file) throws IOException {
        this(new FileInputStream(file));
    }

    public UbjsonParser(final InputStream input) {
        this.input = input;
    }

    @Override
    public @NotNull JsonValue parse() throws IOException {
        return this.readValue();
    }

    protected byte read() throws IOException {
        final int value = this.input.read();
        if (value == -1) {
            throw new IOException("Unexpected end of input");
        }
        return (byte) value;
    }

    protected long readInt() throws IOException {
        return this.readInt(this.read());
    }

    protected long readInt(final byte type) throws IOException {
        return switch (type) {
            case UBMarker.INT8 -> this.read();
            case UBMarker.U_INT8 -> this.readUInt8();
            case UBMarker.INT16 -> this.readInt16();
            case UBMarker.INT32 -> this.readInt32();
            case UBMarker.INT64 -> this.readInt64();
            default -> throw new IOException("Not an integer");
        };
    }

    protected short readUInt8() throws IOException {
        return (short) (this.read() & 0xFF);
    }

    protected short readInt16() throws IOException {
        return (short) ((this.read() & 255) << 8 | this.read() & 255);
    }

    protected int readInt32() throws IOException {
        return (this.read() & 255) << 24
            | (this.read() & 255) << 16
            | (this.read() & 255) << 8
            | this.read() & 255;
    }

    protected long readInt64() throws IOException {
        return ((long) this.read() & 255) << 56
            | ((long) this.read() & 255) << 48
            | ((long) this.read() & 255) << 40
            | ((long) this.read() & 255) << 32
            | ((long) this.read() & 255) << 24
            | ((long) this.read() & 255) << 16
            | ((long) this.read() & 255) << 8
            | (long) this.read() & 255;
    }

    protected float readFloat32() throws IOException {
        return Float.intBitsToFloat(this.readInt32());
    }

    protected double readFloat64() throws IOException {
        return Double.longBitsToDouble(this.readInt64());
    }

    protected String readString() throws IOException {
        return this.readString(this.read());
    }

    protected String readString(final byte sizeType) throws IOException {
        final int size = (int) this.readInt(sizeType);
        final byte[] bytes = new byte[size];
        int bytesLeft = size;
        int offset = 0;

        while (bytesLeft > 0) {
            final int bytesRead = this.input.read(bytes, offset, size);
            if (bytesRead < 0) {
                throw new IOException("Unexpected end of input");
            }
            bytesLeft -= bytesRead;
            offset += bytesRead;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    protected JsonArray readOptimizedArray(final int size, final byte type) throws IOException {
        final List<JsonReference> array = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            array.add(new JsonReference(this.readValue(type)));
        }
        return new JsonArray(array);
    }

    protected JsonArray readSizedArray(final int size) throws IOException {
        final List<JsonReference> array = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            array.add(new JsonReference(this.readValue(this.read())));
        }
        return new JsonArray(array);
    }

    protected JsonArray readGenericArray(byte type) throws IOException {
        final JsonArray array = new JsonArray();
        while (type != UBMarker.ARRAY_END) {
            array.add(this.readValue(type));
            type = this.read();
        }
        return array;
    }

    protected JsonArray readArray() throws IOException {
        final byte marker = this.read();
        if (marker == UBMarker.OPTIMIZED_TYPE) {
            final byte type = this.read();
            if (this.read() != UBMarker.OPTIMIZED_SIZE) {
                throw new IOException("Missing size marker");
            }
            return this.readOptimizedArray((int) this.readInt(), type);
        } else if (marker == UBMarker.OPTIMIZED_SIZE) {
            return this.readSizedArray((int) this.readInt());
        }
        return this.readGenericArray(marker);
    }

    protected JsonObject readOptimizedObject(final int size, final byte type) throws IOException {
        // Todo: JsonObject does not support sized construction.
        final JsonObject object = new JsonObject();
        for (int i = 0; i < size; i++) {
            object.add(this.readString(), this.readValue(type));
        }
        return object;
    }

    protected JsonObject readSizedObject(final int size) throws IOException {
        final JsonObject object = new JsonObject();
        for (int i = 0; i < size; i++) {
            object.add(this.readString(), this.readValue(this.read()));
        }
        return object;
    }

    protected JsonObject readGenericObject(byte type) throws IOException {
        final JsonObject object = new JsonObject();
        while (type != UBMarker.OBJ_END) {
            object.add(this.readString(type), this.readValue());
            type = this.read();
        }
        return object;
    }

    protected JsonObject readObject() throws IOException {
        final byte marker = this.read();
        if (marker == UBMarker.OPTIMIZED_TYPE) {
            final byte type = this.read();
            if (this.read() != UBMarker.OPTIMIZED_SIZE) {
                throw new IOException("Missing size marker");
            }
            return this.readOptimizedObject((int) this.readInt(), type);
        } else if (marker == UBMarker.OPTIMIZED_SIZE) {
            return this.readSizedObject((int) this.readInt());
        }
        return this.readGenericObject(marker);
    }

    protected JsonValue readValue() throws IOException {
        return this.readValue(this.read());
    }

    protected JsonValue readValue(final byte type) throws IOException {
        switch (type) {
            case UBMarker.NULL: return JsonLiteral.jsonNull();
            case UBMarker.TRUE: return JsonLiteral.jsonTrue();
            case UBMarker.FALSE: return JsonLiteral.jsonFalse();
            case UBMarker.CHAR:
            case UBMarker.INT8: return Json.value(this.read());
            case UBMarker.U_INT8:
            case UBMarker.INT16:
            case UBMarker.INT32:
            case UBMarker.INT64: return Json.value(this.readInt(type));
            case UBMarker.FLOAT32: return Json.value(this.readFloat32());
            case UBMarker.FLOAT64: return Json.value(this.readFloat64());
            case UBMarker.STRING: return Json.value(this.readString());
            case UBMarker.ARRAY_START: return this.readArray();
            case UBMarker.OBJ_START: return this.readObject();
        }
        throw new IOException("Unrecognized marker: " + (char) type);
    }

    @Override
    public void close() throws Exception {
        this.input.close();
    }
}
