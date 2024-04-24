package xjs.compat.serialization.util;

import xjs.core.JsonValue;

/**
 * The level of typing to automatically resolve when
 * writing {@link JsonValue}s in UBJSON format.
 */
public enum UBTyping {

    /**
     * Never use type hints.
     */
    WEAK,

    /**
     * Always use type hints.
     */
    STRONG,

    /**
     * Prefer sized, typeless containers. This may
     * provide a subtle optimization when reading
     * from the disk.
     */
    BALANCED,

    /**
     * Prefer the smallest possible option.
     */
    COMPRESSED
}
