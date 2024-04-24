package xjs.compat.serialization.writer;

import org.junit.jupiter.api.Test;
import xjs.core.Json;
import xjs.core.JsonLiteral;
import xjs.core.JsonValue;
import xjs.compat.serialization.TestUtils;
import xjs.compat.serialization.util.UBTyping;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static xjs.compat.serialization.util.UBMarker.ARRAY_END;
import static xjs.compat.serialization.util.UBMarker.ARRAY_START;
import static xjs.compat.serialization.util.UBMarker.FALSE;
import static xjs.compat.serialization.util.UBMarker.FLOAT32;
import static xjs.compat.serialization.util.UBMarker.FLOAT64;
import static xjs.compat.serialization.util.UBMarker.INT16;
import static xjs.compat.serialization.util.UBMarker.INT32;
import static xjs.compat.serialization.util.UBMarker.NULL;
import static xjs.compat.serialization.util.UBMarker.OBJ_END;
import static xjs.compat.serialization.util.UBMarker.OBJ_START;
import static xjs.compat.serialization.util.UBMarker.OPTIMIZED_SIZE;
import static xjs.compat.serialization.util.UBMarker.OPTIMIZED_TYPE;
import static xjs.compat.serialization.util.UBMarker.STRING;
import static xjs.compat.serialization.util.UBMarker.TRUE;
import static xjs.compat.serialization.util.UBMarker.U_INT8;

public final class UbjsonWriterTest {

    @Test
    void write_printsTrue() {
        assertWriteEquals(JsonLiteral.jsonTrue(), TRUE);
    }

    @Test
    void write_printsFalse() {
        assertWriteEquals(JsonLiteral.jsonFalse(), FALSE);
    }

    @Test
    void write_printsNull() {
        assertWriteEquals(JsonLiteral.jsonNull(), NULL);
    }

    @Test
    void write_printsByte() {
        assertWriteEquals(Json.value(1), U_INT8, (byte) 1);
    }

    @Test
    void write_printsInt32() {
        assertWriteEquals(Json.value(Integer.MAX_VALUE), INT32, Integer.MAX_VALUE);
    }

    @Test
    void write_printsFloat() {
        assertWriteEquals(Json.value(1.5), FLOAT32, 1.5F);
    }

    @Test
    void write_printsDouble() {
        assertWriteEquals(Json.value(3.14), FLOAT64, 3.14);
    }

    @Test
    void write_printsStringWithSize() {
        final String s = "Hello, World!";
        final byte len = (byte) s.getBytes(StandardCharsets.UTF_8).length;
        assertWriteEquals(Json.value(s), STRING, U_INT8, len, s);
    }

    @Test
    void write_printsEmptyArray() {
        assertWriteEquals(Json.array(), ARRAY_START, ARRAY_END);
    }

    @Test
    void write_compressesArray() {
        assertWriteEquals(Json.array(1, 2, 3, 4, 5),
            ARRAY_START, OPTIMIZED_TYPE, U_INT8, OPTIMIZED_SIZE, U_INT8, (byte) 5,
                (byte) 1,
                (byte) 2,
                (byte) 3,
                (byte) 4,
                (byte) 5);
    }

    @Test
    void write_compressesArray_withNonUniformDistribution() {
        assertWriteEquals(Json.array(-1, 1, 128),
            ARRAY_START, OPTIMIZED_TYPE, INT16, OPTIMIZED_SIZE, U_INT8, (byte) 3,
                (short) -1,
                (short) 1,
                (short) 128);
    }

    @Test
    void writeGeneric_doesNotCompressArray() {
        assertGenericEquals(Json.array(1, 2, 3, 4, 5),
            ARRAY_START,
                U_INT8, (byte) 1,
                U_INT8, (byte) 2,
                U_INT8, (byte) 3,
                U_INT8, (byte) 4,
                U_INT8, (byte) 5,
            ARRAY_END);
    }

    @Test
    void write_compressesArray_recursively() {
        assertWriteEquals(
            Json.array().add(
                Json.array(1, 2, 3, 4, 5)),
            ARRAY_START, OPTIMIZED_SIZE, U_INT8, (byte) 1,
                ARRAY_START, OPTIMIZED_TYPE, U_INT8, OPTIMIZED_SIZE, U_INT8, (byte) 5,
                    (byte) 1,
                    (byte) 2,
                    (byte) 3,
                    (byte) 4,
                    (byte) 5);
    }

    @Test
    void writeCompressed_hasStrongerCompression() {
        assertCompressedEquals(
            Json.array().add(
                Json.array(1, 2, 3, 4, 5)),
            ARRAY_START,
                ARRAY_START, OPTIMIZED_TYPE, U_INT8, OPTIMIZED_SIZE, U_INT8, (byte) 5,
                    (byte) 1,
                    (byte) 2,
                    (byte) 3,
                    (byte) 4,
                    (byte) 5,
            ARRAY_END);
    }

    @Test
    void write_doesNotCompress_multiTypeArray() {
        assertWriteEquals(Json.array().add(1).add(true).add(3),
            ARRAY_START, OPTIMIZED_SIZE, U_INT8, (byte) 3,
            U_INT8, (byte) 1, TRUE, U_INT8, (byte) 3);
    }

    @Test
    void write_printsEmptyObject() {
        assertWriteEquals(Json.object(), OBJ_START, OBJ_END);
    }

    @Test
    void write_compressesObject() {
        assertWriteEquals(
            Json.object()
                .add("a", 1)
                .add("b", 2)
                .add("c", 3)
                .add("d", 4)
                .add("e", 5),
            OBJ_START, OPTIMIZED_TYPE, U_INT8, OPTIMIZED_SIZE, U_INT8, (byte) 5,
                U_INT8, (byte) 1, "a", (byte) 1,
                U_INT8, (byte) 1, "b", (byte) 2,
                U_INT8, (byte) 1, "c", (byte) 3,
                U_INT8, (byte) 1, "d", (byte) 4,
                U_INT8, (byte) 1, "e", (byte) 5);
    }

    @Test
    void write_compressesObject_recursively() {
        assertWriteEquals(
            Json.object()
                .add("x", Json.object()
                    .add("a", 1).add("b", 2).add("c", 3).add("d", 4).add("e", 5)),
            OBJ_START, OPTIMIZED_SIZE, U_INT8, (byte) 1,
                U_INT8, (byte) 1, "x",
                OBJ_START, OPTIMIZED_TYPE, U_INT8, OPTIMIZED_SIZE, U_INT8, (byte) 5,
                    U_INT8, (byte) 1, "a", (byte) 1,
                    U_INT8, (byte) 1, "b", (byte) 2,
                    U_INT8, (byte) 1, "c", (byte) 3,
                    U_INT8, (byte) 1, "d", (byte) 4,
                    U_INT8, (byte) 1, "e", (byte) 5);
    }

    @Test
    void writeGeneric_doesNotCompressObject() {
        assertGenericEquals(
            Json.object()
                .add("a", 1)
                .add("b", 2)
                .add("c", 3)
                .add("d", 4)
                .add("e", 5),
            OBJ_START,
                U_INT8, (byte) 1, "a", U_INT8, (byte) 1,
                U_INT8, (byte) 1, "b", U_INT8, (byte) 2,
                U_INT8, (byte) 1, "c", U_INT8, (byte) 3,
                U_INT8, (byte) 1, "d", U_INT8, (byte) 4,
                U_INT8, (byte) 1, "e", U_INT8, (byte) 5,
            OBJ_END);
    }

    private static void assertWriteEquals(final JsonValue value, final Object... bytes) {
        assertWriteEquals(UBTyping.BALANCED, value, bytes);
    }

    private static void assertCompressedEquals(final JsonValue value, final Object... bytes) {
        assertWriteEquals(UBTyping.COMPRESSED, value, bytes);
    }

    private static void assertGenericEquals(final JsonValue value, final Object... bytes) {
        assertWriteEquals(UBTyping.WEAK, value, bytes);
    }

    private static void assertWriteEquals(
            final UBTyping typing, final JsonValue value, final Object... bytes) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            new UbjsonWriter(output, typing).write(value);
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
        TestUtils.assertBytesEqual(TestUtils.getBytes(bytes), output.toByteArray());
    }
}
