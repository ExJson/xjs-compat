package xjs.compat.serialization.parser;

import org.junit.jupiter.api.Test;
import xjs.data.Json;
import xjs.data.JsonLiteral;
import xjs.data.JsonValue;
import xjs.compat.serialization.TestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static xjs.compat.serialization.util.UBMarker.ARRAY_START;
import static xjs.compat.serialization.util.UBMarker.ARRAY_END;
import static xjs.compat.serialization.util.UBMarker.FALSE;
import static xjs.compat.serialization.util.UBMarker.FLOAT32;
import static xjs.compat.serialization.util.UBMarker.FLOAT64;
import static xjs.compat.serialization.util.UBMarker.INT32;
import static xjs.compat.serialization.util.UBMarker.OBJ_START;
import static xjs.compat.serialization.util.UBMarker.OBJ_END;
import static xjs.compat.serialization.util.UBMarker.OPTIMIZED_SIZE;
import static xjs.compat.serialization.util.UBMarker.OPTIMIZED_TYPE;
import static xjs.compat.serialization.util.UBMarker.NULL;
import static xjs.compat.serialization.util.UBMarker.STRING;
import static xjs.compat.serialization.util.UBMarker.TRUE;
import static xjs.compat.serialization.util.UBMarker.U_INT8;

public final class UbjsonParserTest {

    @Test
    void parse_readsTrue() {
        assertParseEquals(JsonLiteral.jsonTrue(), TRUE);
    }

    @Test
    void parse_readsFalse() {
        assertParseEquals(JsonLiteral.jsonFalse(), FALSE);
    }

    @Test
    void parse_readsNull() {
        assertParseEquals(JsonLiteral.jsonNull(), NULL);
    }

    @Test
    void parse_readsByte() {
        assertParseEquals(Json.value(1), U_INT8, (byte) 1);
    }

    @Test
    void parse_readsInt32() {
        assertParseEquals(Json.value(Integer.MAX_VALUE), INT32, Integer.MAX_VALUE);
    }

    @Test
    void parse_readsFloat() {
        assertParseEquals(Json.value(1.5), FLOAT32, 1.5F);
    }

    @Test
    void parse_readsDouble() {
        assertParseEquals(Json.value(3.14), FLOAT64, 3.14);
    }

    @Test
    void parse_readsStringWithSize() {
        final String s = "Hello, World!";
        final byte len = (byte) s.getBytes(StandardCharsets.UTF_8).length;
        assertParseEquals(Json.value(s), STRING, U_INT8, len, s);
    }

    @Test
    void parse_readsEmptyArray() {
        assertParseEquals(Json.array(), ARRAY_START, ARRAY_END);
    }

    @Test
    void parse_readsCompressedArray() {
        assertParseEquals(Json.array(1, 2, 3, 4, 5),
            ARRAY_START, OPTIMIZED_TYPE, U_INT8, OPTIMIZED_SIZE, U_INT8, (byte) 5,
                (byte) 1,
                (byte) 2,
                (byte) 3,
                (byte) 4,
                (byte) 5);
    }

    @Test
    void parse_readsGenericArray() {
        assertParseEquals(Json.array(1, 2, 3, 4, 5),
            ARRAY_START,
                U_INT8, (byte) 1,
                U_INT8, (byte) 2,
                U_INT8, (byte) 3,
                U_INT8, (byte) 4,
                U_INT8, (byte) 5,
            ARRAY_END);
    }

    @Test
    void parse_readsCompressedArray_recursively() {
        assertParseEquals(
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
    void parse_readsMultiTypeArray() {
        assertParseEquals(Json.array().add(1).add(true).add(3),
            ARRAY_START, OPTIMIZED_SIZE, U_INT8, (byte) 3,
                U_INT8, (byte) 1, TRUE, U_INT8, (byte) 3);
    }

    @Test
    void parse_readsEmptyObject() {
        assertParseEquals(Json.object(), OBJ_START, OBJ_END);
    }

    @Test
    void parse_readsCompressedObject() {
        assertParseEquals(
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
    void parse_readsCompressesObject_recursively() {
        assertParseEquals(
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
    void parse_readsGenericObject() {
        assertParseEquals(
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

    private static void assertParseEquals(final JsonValue expected, final Object... bytes) {
        final ByteArrayInputStream input = 
            new ByteArrayInputStream(TestUtils.getBytes(bytes));
        final JsonValue actual;
        try {
            actual = new UbjsonParser(input).parse();
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
        if (!actual.matches(expected)) {
            throw new AssertionError(buildMismatchError(expected, actual));
        }
    }

    private static String buildMismatchError(final JsonValue expected, final JsonValue actual) {
        return """
            Bytes do not match.
            Expected:
              %s
            Actual:
              %s
            """.formatted(expected, actual);
    }
}
