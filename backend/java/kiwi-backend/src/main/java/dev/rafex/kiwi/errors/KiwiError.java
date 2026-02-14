package dev.rafex.kiwi.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KiwiError extends Exception {

    private static final Logger LOG = LoggerFactory.getLogger(KiwiError.class);
    private static final long serialVersionUID = 1L;

    private final String code;

    /* ===================== CONSTRUCTORS ===================== */

    public KiwiError(final String code, final String message) {
        super(message);
        this.code = code;
        LOG.error("KiwiError [{}]: {}", code, message);
    }

    public KiwiError(final String code, final String message, final Object... args) {
        super(format(message, args));
        this.code = code;
        LOG.error("KiwiError [{}]: {}", code, format(message, args));
    }

    public KiwiError(final String code, final String message, final Throwable cause) {
        super(message, cause);
        this.code = code;
        LOG.error("KiwiError [{}]: {}", code, message, cause);
    }

    public KiwiError(final String code, final String message, final Throwable cause, final Object... args) {
        super(format(message, args), cause);
        this.code = code;
        LOG.error("KiwiError [{}]: {}", code, format(message, args), cause);
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