package xjs.serialization.writer;

import xjs.core.JsonArray;
import xjs.core.JsonContainer;
import xjs.core.JsonObject;
import xjs.core.JsonValue;
import xjs.serialization.util.UBMarker;
import xjs.serialization.util.UBTyping;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class UbjsonWriter implements ValueWriter {
    protected static final int U_INT_8_MIN = 0;
    protected static final int U_INT_8_MAX = (1 << 8) - 1;
    protected static final int INT_8_MAX = (1 << 8) / 2 - 1;
    protected static final int INT_8_MIN = (1 << 8) / -2;
    protected static final int INT_16_MAX = (1 << 16) / 2 - 1;
    protected static final int INT_16_MIN = (1 << 16) / -2;
    protected static final int INT_32_MIN = Integer.MIN_VALUE;
    protected static final int INT_32_MAX = Integer.MAX_VALUE;

    protected final OutputStream output;
    protected final UBTyping typing;

    public UbjsonWriter(final File file) throws IOException {
        this(new FileOutputStream(file), UBTyping.COMPRESSED);
    }

    public UbjsonWriter(final File file, final UBTyping typing) throws IOException {
        this(new FileOutputStream(file), typing);
    }

    public UbjsonWriter(final OutputStream output, final UBTyping typing) {
        this.output = output;
        this.typing = typing;
    }

    @Override
    public void write(final JsonValue value) throws IOException {
        this.writeValue(value);
    }

    protected void writeNull() throws IOException {
        this.output.write(UBMarker.NULL);
    }

    protected void writeBool(final boolean b) throws IOException {
        this.output.write(b ? UBMarker.TRUE : UBMarker.FALSE);
    }

    protected void writeInt8(final byte value) throws IOException {
        this.output.write(UBMarker.INT8);
        this.writeRawInt8(value);
    }

    protected void writeRawInt8(final byte value) throws IOException {
        this.output.write(value);
    }

    protected void writeUInt8(final short value) throws IOException {
        this.output.write(UBMarker.U_INT8);
        this.writeRawUInt8(value);
    }

    protected void writeRawUInt8(final short value) throws IOException {
        this.output.write(value & 0xFF);
    }

    protected void writeInt16(final short value) throws IOException {
        this.output.write(UBMarker.INT16);
        this.writeRawInt16(value);
    }

    protected void writeRawInt16(final short value) throws IOException {
        this.output.write(value >> 8);
        this.output.write(value);
    }

    protected void writeInt32(final int value) throws IOException {
        this.output.write(UBMarker.INT32);
        this.writeRawInt32(value);
    }

    protected void writeRawInt32(final int value) throws IOException {
        this.output.write(value >> 24);
        this.output.write(value >> 16);
        this.output.write(value >> 8);
        this.output.write(value);
    }

    protected void writeInt64(final long value) throws IOException {
        this.output.write(UBMarker.INT32);
        this.writeRawInt64(value);
    }

    protected void writeRawInt64(final long value) throws IOException {
        this.output.write((byte) (0xFF & (value >> 56)));
        this.output.write((byte) (0xFF & (value >> 48)));
        this.output.write((byte) (0xFF & (value >> 40)));
        this.output.write((byte) (0xFF & (value >> 32)));
        this.output.write((byte) (0xFF & (value >> 24)));
        this.output.write((byte) (0xFF & (value >> 16)));
        this.output.write((byte) (0xFF & (value >> 8)));
        this.output.write((byte) (0xFF & value));
    }

    protected void writeInt(final long value) throws IOException {
        if (value >= U_INT_8_MIN && value <= U_INT_8_MAX) {
            this.writeUInt8((byte) value);
        } else if (value >= INT_8_MIN && value <= INT_8_MAX) {
            this.writeInt8((byte) value);
        } else if (value >= INT_16_MIN && value <= INT_16_MAX) {
            this.writeInt16((short) value);
        } else if (value >= INT_32_MIN && value <= INT_32_MAX) {
            this.writeInt32((int) value);
        } else {
            this.writeInt64(value);
        }
    }

    protected void writeFloat32(final float value) throws IOException {
        this.output.write(UBMarker.FLOAT32);
        this.writeRawFloat32(value);
    }

    protected void writeRawFloat32(final float value) throws IOException {
        this.writeRawInt32(Float.floatToIntBits(value));
    }

    protected void writeFloat64(final double value) throws IOException {
        this.output.write(UBMarker.FLOAT64);
        this.writeRawFloat64(value);
    }

    protected void writeRawFloat64(final double value) throws IOException {
        this.writeRawInt64(Double.doubleToLongBits(value));
    }

    protected void writeFloat(final double value) throws IOException {
        if ((float) value == value) {
            this.writeFloat32((float) value);
        } else {
            this.writeFloat64(value);
        }
    }

    protected void writeNumber(final double value) throws IOException {
        final long integer = (long) value;
        if (integer == value) {
            this.writeInt(integer);
        } else {
            this.writeFloat(value);
        }
    }

    protected void writeString(final String value) throws IOException {
        this.output.write(UBMarker.STRING);
        this.writeRawString(value);
    }

    protected void writeRawString(final String value) throws IOException {
        final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        this.writeInt(bytes.length);
        this.output.write(bytes);
    }

    protected void writeArray(final JsonArray array) throws IOException {
        this.output.write(UBMarker.ARRAY_START);
        this.writeRawArray(array);
    }

    protected void writeRawArray(final JsonArray array) throws IOException {
        if (array.isEmpty()) {
            this.output.write(UBMarker.ARRAY_END);
        } else if (this.typing == UBTyping.WEAK) {
            this.writeGenericArray(array);
        } else {
            final byte type = this.getCompressionType(array);
            if (type != 0) {
                this.writeOptimizedArray(array, type);
            } else if (this.typing == UBTyping.COMPRESSED) {
                this.writeGenericArray(array);
            } else {
                this.writeSizedArray(array);
            }
        }
    }

    protected void writeSizedArray(final JsonArray array) throws IOException {
        this.output.write(UBMarker.OPTIMIZED_SIZE);
        this.writeInt(array.size());
        for (final JsonValue value : array.visitAll()) {
            this.writeValue(value);
        }
    }

    protected void writeGenericArray(final JsonArray array) throws IOException {
        for (final JsonValue value : array.visitAll()) {
            this.writeValue(value);
        }
        this.output.write(UBMarker.ARRAY_END);
    }

    protected void writeOptimizedArray(final JsonArray array, final byte type) throws IOException {
        this.output.write(UBMarker.OPTIMIZED_TYPE);
        this.output.write(type);
        this.output.write(UBMarker.OPTIMIZED_SIZE);
        this.writeInt(array.size());
        for (final JsonValue value : array.visitAll()) {
            this.writeRawValue(value, type);
        }
    }

    protected byte getCompressionType(final JsonContainer container) {
        final int minSize = this.typing == UBTyping.STRONG ? 1 : 2;
        if (container.size() < minSize) {
            return 0;
        }
        final byte type = this.getContainerType(container);
        if (this.typing != UBTyping.STRONG) {
            if (this.isSingleByte(type) && container.size() < 5) {
                return 0;
            }
        }
        return type;
    }

    protected byte getType(final JsonValue value) {
        switch (value.getType()) {
            case STRING: return UBMarker.STRING;
            case BOOLEAN: return value.asBoolean() ? UBMarker.TRUE : UBMarker.FALSE;
            case ARRAY: return UBMarker.ARRAY_START;
            case OBJECT: return UBMarker.OBJ_START;
            case NUMBER: return this.getNumberType(value.asDouble());
            default: return UBMarker.NULL;
        }
    }

    protected byte getContainerType(final JsonContainer container) {
        final JsonValue firstValue = container.get(0);
        if (firstValue.isNumber()) {
            return this.getNumberType(container);
        }
        final byte type = this.getType(firstValue);
        if (this.isMarkerOnly(type)) {
            return 0;
        }
        for (int i = 1; i < container.size(); i++) {
            if (type != this.getType(container.getReference(i).getOnly())) {
                return 0;
            }
        }
        return type;
    }

    protected byte getNumberType(final JsonContainer container) {
        double max = Integer.MIN_VALUE;
        double min = Integer.MAX_VALUE;
        for (final JsonValue value : container.visitAll()) {
            if (!value.isNumber()) {
                return 0;
            }
            max = Double.max(max, value.asDouble());
            min = Double.min(min, value.asDouble());
        }
        return this.getNumberType(min, max);
    }

    protected byte getNumberType(final double min, final double max) {
        if ((long) min == min && (long) max == max) {
            if (min >= U_INT_8_MIN && max <= U_INT_8_MAX) {
                return UBMarker.U_INT8;
            } else if (min >= INT_8_MIN && max <= INT_8_MAX) {
                return UBMarker.INT8;
            } else if (min >= INT_16_MIN && max <= INT_16_MAX) {
                return UBMarker.INT16;
            } else if (min >= INT_32_MIN && max <= INT_32_MAX) {
                return UBMarker.INT32;
            }
            return UBMarker.INT64;
        }
        if ((float) min == min && (float) max == max) {
            return UBMarker.FLOAT32;
        }
        return UBMarker.FLOAT64;
    }

    protected byte getNumberType(final double value) {
        return this.getNumberType(value, value);
    }

    protected boolean isMarkerOnly(final byte marker) {
        return marker == UBMarker.TRUE || marker == UBMarker.FALSE || marker == UBMarker.NULL;
    }

    protected boolean isSingleByte(final byte marker) {
        return marker == UBMarker.INT8 || marker == UBMarker.U_INT8;
    }

    protected void writeObject(final JsonObject object) throws IOException {
        this.output.write(UBMarker.OBJ_START);
        this.writeRawObject(object);
    }

    protected void writeRawObject(final JsonObject object) throws IOException {
        if (object.isEmpty()) {
            this.output.write(UBMarker.OBJ_END);
        } else if (this.typing == UBTyping.WEAK) {
            this.writeGenericObject(object);
        } else {
            final byte type = this.getCompressionType(object);
            if (type != 0) {
                this.writeOptimizedObject(object, type);
            } else if (this.typing == UBTyping.COMPRESSED) {
                this.writeGenericObject(object);
            } else {
                this.writeSizedObject(object);
            }
        }
    }

    protected void writeSizedObject(final JsonObject object) throws IOException {
        this.output.write(UBMarker.OPTIMIZED_SIZE);
        this.writeInt(object.size());
        for (final JsonObject.Member member : object) {
            this.writeRawString(member.getKey());
            this.writeValue(member.getOnly());
        }
    }

    protected void writeGenericObject(final JsonObject object) throws IOException {
        for (final JsonObject.Member member : object) {
            this.writeRawString(member.getKey());
            this.writeValue(member.getOnly());
        }
        this.output.write(UBMarker.OBJ_END);
    }

    protected void writeOptimizedObject(final JsonObject object, final byte type) throws IOException {
        this.output.write(UBMarker.OPTIMIZED_TYPE);
        this.output.write(type);
        this.output.write(UBMarker.OPTIMIZED_SIZE);
        this.writeInt(object.size());
        for (final JsonObject.Member member : object) {
            this.writeRawString(member.getKey());
            this.writeRawValue(member.getOnly(), type);
        }
    }

    protected void writeValue(final JsonValue value) throws IOException {
        switch (value.getType()) {
            case NUMBER:
                this.writeNumber(value.asDouble());
                break;
            case ARRAY:
                this.writeArray(value.asArray());
                break;
            case OBJECT:
                this.writeObject(value.asObject());
                break;
            case BOOLEAN:
                this.writeBool(value.asBoolean());
                break;
            case STRING:
                this.writeString(value.asString());
                break;
            default:
                this.writeNull();
        }
    }

    protected void writeRawValue(final JsonValue value, final byte type) throws IOException {
        switch (type) {
            case UBMarker.INT8:
                this.writeRawInt8((byte) value.asDouble());
                break;
            case UBMarker.U_INT8:
                this.writeRawUInt8((short) value.asDouble());
                break;
            case UBMarker.INT16:
                this.writeRawInt16((short) value.asDouble());
                break;
            case UBMarker.INT32:
                this.writeRawInt32(value.asInt());
                break;
            case UBMarker.INT64:
                this.writeRawInt64(value.asLong());
                break;
            case UBMarker.FLOAT32:
                this.writeRawFloat32(value.asFloat());
                break;
            case UBMarker.FLOAT64:
                this.writeRawFloat64(value.asDouble());
                break;
            case UBMarker.STRING:
                this.writeRawString(value.asString());
                break;
            case UBMarker.ARRAY_START:
                this.writeRawArray(value.asArray());
                break;
            case UBMarker.OBJ_START:
                this.writeRawObject(value.asObject());
                break;
            case UBMarker.NULL:
            case UBMarker.TRUE:
            case UBMarker.FALSE:
                break;
            default:
                throw new IllegalStateException("Unrecognized marker: " + (char) type);
        }
    }

    @Override
    public void close() throws Exception {
        this.output.close();
    }
}
