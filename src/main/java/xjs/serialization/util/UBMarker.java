package xjs.serialization.util;

/**
 * Marker bytes for UBJson
 */
public class UBMarker {

    /** Null value */
    public static final byte NULL = 'Z';

    /** True literal */
    public static final byte TRUE = 'T';

    /** False literal */
    public static final byte FALSE = 'F';

    /** Single character */
    public static final byte CHAR = 'C';

    /** 8-bit integer */
    public static final byte INT8 = 'i';

    /** Unsigned 8-bit integer */
    public static final byte U_INT8 = 'U';

    /** 16-bit integer */
    public static final byte INT16 = 'I';

    /** 32-bit integer */
    public static final byte INT32 = 'l';

    /** 64-bit integer */
    public static final byte INT64 = 'L';

    /** 32-bit float */
    public static final byte FLOAT32 = 'd';

    /** 64-bit float */
    public static final byte FLOAT64 = 'D';

    /** Character array */
    public static final byte STRING = 'S';

    /** Array opener */
    public static final byte ARRAY_START = '[';

    /** Array closer */
    public static final byte ARRAY_END = ']';

    /** Object opener */
    public static final byte OBJ_START = '{';

    /** Object closer */
    public static final byte OBJ_END = '}';

    /** Typed array indicator */
    public static final byte OPTIMIZED_TYPE = '$';

    /** Container size indicator */
    public static final byte OPTIMIZED_SIZE = '#';
}
