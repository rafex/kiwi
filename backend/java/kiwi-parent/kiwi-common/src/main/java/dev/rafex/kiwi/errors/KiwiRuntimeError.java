package dev.rafex.kiwi.errors;

import dev.rafex.kiwi.logging.Log;

public class KiwiRuntimeError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String code;

    public KiwiRuntimeError(final String code, final String message) {
        super(message);
        this.code = code;
        Log.error(getClass(), "KiwiRuntimeError [{}]: {}", code, message);
    }

    public KiwiRuntimeError(final String code, final String message, final Object... args) {
        this(code, format(message, args));
    }

    public KiwiRuntimeError(final String code, final String message, final Throwable cause) {
        super(message, cause);
        this.code = code;
        Log.error(getClass(), cause, "KiwiRuntimeError [{}]: {}", code, message);
    }

    public KiwiRuntimeError(final String code, final String message, final Throwable cause, final Object... args) {
        this(code, format(message, args), cause);
    }

    public String getCode() {
        return code;
    }

    private static String format(final String message, final Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        final var sb = new StringBuilder(message.length() + 64);
        var argIdx = 0;
        var start = 0;
        int idx;
        while ((idx = message.indexOf("{}", start)) >= 0 && argIdx < args.length) {
            sb.append(message, start, idx);
            sb.append(args[argIdx] == null ? "null" : args[argIdx].toString());
            start = idx + 2;
            argIdx++;
        }
        sb.append(message, start, message.length());
        return sb.toString();
    }
}
