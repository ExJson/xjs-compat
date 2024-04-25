package xjs.compat.serialization.util;

import xjs.data.serialization.token.Token;
import xjs.data.serialization.token.TokenType;

import java.util.BitSet;

public class StringContext {
    // 0 = object, 1 = array
    protected final BitSet containers = new BitSet();
    protected boolean expectingKey = false; // first is ambiguous (open/closed root)
    protected boolean top = true;
    protected int level;

    public void prepare(final char c) {
        switch (c) {
            case '{' -> this.push(false);
            case '[' -> this.push(true);
            case '}', ']' -> this.pop();
            case ':' -> this.expectingKey = false;
        }
    }

    public void update(final Token parsed) {
        if (parsed == null) {
            return;
        }
        if (!parsed.isMetadata()) {
            if (parsed.type() != TokenType.SYMBOL && !this.containers.get(this.level)) {
                this.expectingKey = true;
            }
            this.top = false;
        }
    }

    protected void push(final boolean isArray) {
        this.containers.set(++this.level, isArray);
        this.expectingKey = !isArray;
    }

    // no need to track whether the correct type
    // was popped. let the tokenizer handle that.
    protected void pop() {
        if (this.level == 0) {
            this.expectingKey = true;
            return;
        }
        final boolean isArray = this.containers.get(--this.level);
        this.expectingKey = !isArray;
    }

    public boolean isExpectingKey() {
        return this.expectingKey;
    }

    public boolean isAmbiguous() {
        return this.top; // ambiguous if first significant token
    }
}
