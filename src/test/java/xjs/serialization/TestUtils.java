package xjs.serialization;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class TestUtils {

    private TestUtils() {}

    public static void assertBytesEqual(final byte[] expected, final byte[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError(buildMismatchError(expected, actual));
        }
    }

    private static String buildMismatchError(final byte[] expected, final byte[] actual) {
        return """
            Bytes do not match.
            Expected:
              "%s"
              %s
            Actual:
              "%s"
              %s
            """.formatted(
                new String(expected), formatAsNumbers(expected),
                new String(actual), formatAsNumbers(actual));
    }

    public static byte[] getBytes(final Object... values) {
        final ByteBuffer bytes =
            ByteBuffer.allocate(sizeOf(values));
        for (final Object value : values) {
            write(bytes, value);
        }
        return bytes.array();
    }

    private static void write(final ByteBuffer bytes, final Object value) {
        if (value instanceof Character) {
            bytes.putChar((Character) value);
        } else if (value instanceof Byte) {
            bytes.put((Byte) value);
        } else if (value instanceof Short) {
            bytes.putShort((Short) value);
        } else if (value instanceof Integer) {
            bytes.putInt((Integer) value);
        } else if (value instanceof Long) {
            bytes.putLong((Long) value);
        } else if (value instanceof Float) {
            bytes.putFloat((Float) value);
        } else if (value instanceof Double) {
            bytes.putDouble((Double) value);
        } else if (value instanceof String) {
            bytes.put(((String) value)
                .getBytes(StandardCharsets.UTF_8));
        } else {
            throw new UnsupportedOperationException(
                value.getClass().getSimpleName());
        }
    }

    private static int sizeOf(final Object... values) {
        int size = 0;
        for (final Object value : values) {
            size += sizeOf(value);
        }
        return size;
    }

    private static int sizeOf(final Object value) {
        if (value instanceof Character) {
            return 2;
        } else if (value instanceof Byte) {
            return 1;
        } else if (value instanceof Short) {
            return 2;
        } else if (value instanceof Integer) {
            return 4;
        } else if (value instanceof Long) {
            return 8;
        } else if (value instanceof Float) {
            return 4;
        } else if (value instanceof Double) {
            return 8;
        } else if (value instanceof String) {
            return ((String) value)
                .getBytes(StandardCharsets.UTF_8).length;
        }
        throw new UnsupportedOperationException(
            value.getClass().getSimpleName());
    }

    public static String formatAsNumbers(final byte[] bytes) {
        if (bytes.length == 0) {
            return "[]";
        }
        final StringBuilder sb =
            new StringBuilder()
                .append('[')
                .append(bytes[0]);
        for (int i = 1; i < bytes.length; i++) {
            sb.append(", ").append(bytes[i]);
        }
        return sb.append(']').toString();
    }
}
