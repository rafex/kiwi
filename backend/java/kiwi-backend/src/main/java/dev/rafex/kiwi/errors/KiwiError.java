package dev.rafex.kiwi.errors;

import dev.rafex.kiwi.logging.Log;

public class KiwiError extends Exception {

    private static final long serialVersionUID = 1L;

    private final String code;

    /* ===================== CONSTRUCTORS ===================== */

    public KiwiError(final String code, final String message) {
        super(message);
        this.code = code;
        Log.error(getClass(), "KiwiError [{}]: {}", code, message);
    }

    public KiwiError(final String code, final String message, final Object... args) {
        super(format(message, args));
        this.code = code;
        Log.error(getClass(), "KiwiError [{}]: {}", code, format(message, args));
    }

    public KiwiError(final String code, final String message, final Throwable cause) {
        super(message, cause);
        this.code = code;
        Log.error(getClass(), "KiwiError [{}]: {}", cause, code, message);
    }

    public KiwiError(final String code, final String message, final Throwable cause, final Object... args) {
        super(format(message, args), cause);
        this.code = code;
        Log.error(getClass(), "KiwiError [{}]: {}", cause, code, format(message, args));
    }

    /* ===================== GETTERS ===================== */

    public String getCode() {
        return code;
    }

    /* ===================== INTERNAL FORMAT ===================== */

    private static String format(String message, final Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }

        for (final Object arg : args) {
            message = message.replaceFirst("\\{}", java.util.regex.Matcher.quoteReplacement(arg == null ? "null" : arg.toString()));
        }

        return message;
    }
}