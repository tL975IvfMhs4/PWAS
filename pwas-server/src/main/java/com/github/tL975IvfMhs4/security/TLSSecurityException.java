package com.github.tL975IvfMhs4.security;

import java.util.Arrays;

public class TLSSecurityException extends RuntimeException {
    public TLSSecurityException(String message) {
        super(message);
    }

    public TLSSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructeur avec message à formater. Si le dernier argument est une exception,
     * on ne l’utilise pas au formatage et on l’enregistre comme cause.
     * @param message
     * @param args
     */
    public TLSSecurityException(String message, Object... args) {
        super(formatMessage(message, args), extractCause(args));
    }

    /**
     * Formate le message de l’exception à partir des arguments donnés. Si le dernier argument
     * est une exception, on ne l’utilise pas au formatage.
     * @param message
     * @param args
     * @return
     */
    private static String formatMessage(String message, Object... args) {
        if (args.length > 0 && !(args[args.length - 1] instanceof Throwable)) {
            return String.format(message, Arrays.copyOf(args, args.length - 1));
        }
        return String.format(message, args);
    }

    /**
     * Si le dernier argument est une exception, on la retourne. Sinon, retourne null.
     * @param args
     * @return
     */
    private static Throwable extractCause(Object[] args) {
        return args.length > 0 && args[args.length - 1] instanceof Throwable ? (Throwable) args[args.length - 1] : null;
    }
}
