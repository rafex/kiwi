package dev.rafex.kiwi.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import dev.rafex.kiwi.db.Db;
import dev.rafex.kiwi.repository.impl.RoleRepositoryImpl;
import dev.rafex.kiwi.repository.impl.UserRepositoryImpl;
import dev.rafex.kiwi.security.PasswordHasherPBKDF2;
import dev.rafex.kiwi.services.impl.UserProvisioningServiceImpl;

public final class Main {

    private Main() {
    }

    public static void main(final String[] args) throws Exception {
        final var a = Args.parse(args);

        if (a.help) {
            printHelp();
            return;
        }

        final var username = required("username", a.username);
        final var password = readPassword(a);
        final var roles = parseRoles(a.roles);

        final var ds = Db.dataSource();

        // wiring repos + service
        final var userRepo = new UserRepositoryImpl(ds);
        final var roleRepo = new RoleRepositoryImpl(ds);
        final var hasher = new PasswordHasherPBKDF2(32); // 32 bytes si tu password_hash es 32 bytes
        final var provisioning = new UserProvisioningServiceImpl(userRepo, roleRepo, hasher);

        // Crea usuario
        final var res = provisioning.createUser(username, password, roles);

        if (!res.ok()) {
            System.err.println("ERROR: cannot create user. code=" + res.code());
            System.exit(2);
        }

        System.out.println("OK: user created");
        System.out.println("user_id=" + res.userId());
        System.out.println("username=" + username);
        System.out.println("roles=" + roles);
    }

    // ---------- helpers ----------

    private static char[] readPassword(final Args a) {
        if (a.password != null && !a.password.isBlank()) {
            return a.password.toCharArray();
        }
        if (a.passwordEnv != null && !a.passwordEnv.isBlank()) {
            final var v = System.getenv(a.passwordEnv);
            if (v == null || v.isBlank()) {
                throw new IllegalArgumentException("password env var is empty: " + a.passwordEnv);
            }
            return v.toCharArray();
        }
        // último recurso: prompt interactivo (si hay consola)
        final var console = System.console();
        if (console != null) {
            final var p = console.readPassword("Password for %s: ", a.username != null ? a.username : "user");
            if (p == null || p.length == 0) {
                throw new IllegalArgumentException("password is empty");
            }
            return p;
        }
        throw new IllegalArgumentException("missing --password or --password-env (no console available)");
    }

    private static List<String> parseRoles(final String rolesCsv) {
        if (rolesCsv == null || rolesCsv.isBlank()) {
            return List.of();
        }
        final var out = new ArrayList<String>();
        for (final var p : rolesCsv.split(",")) {
            final var r = p.trim();
            if (!r.isEmpty()) {
                out.add(r);
            }
        }
        return out;
    }

    private static String required(final String name, final String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing --" + name);
        }
        return value;
    }

    private static void printHelp() {
        System.out.println("""
                Usage:
                  java ... dev.rafex.kiwi.tools.Main \\
                    --username <name> \\
                    [--password <pass> | --password-env <ENV_VAR>] \\
                    [--roles admin,writer]

                Required env vars:
                  JDBC_URL   e.g. jdbc:postgresql://localhost:5432/kiwi
                  DB_USER
                  DB_PASS

                Examples:
                  export JDBC_URL='jdbc:postgresql://localhost:5432/kiwi'
                  export DB_USER='kiwi_app'
                  export DB_PASS='...'
                  export ADMIN_PASS='Admin123!'

                  java -cp target/... dev.rafex.kiwi.tools.CreateUserMain \\
                    --username admin --password-env ADMIN_PASS --roles admin
                """);
    }

    // ---------- args parsing (simple) ----------
    private static final class Args {
        boolean help;
        String username;
        String password;
        String passwordEnv;
        String roles;

        static Args parse(final String[] args) {
            final var a = new Args();
            final var list = Arrays.asList(args);

            for (var i = 0; i < list.size(); i++) {
                final var k = list.get(i);

                if ("-h".equals(k) || "--help".equals(k)) {
                    a.help = true;
                    return a;
                }

                if (k.startsWith("--")) {
                    final var key = k.substring(2).toLowerCase(Locale.ROOT);
                    final var val = i + 1 < list.size() && !list.get(i + 1).startsWith("--") ? list.get(++i) : null;

                    switch (key) {
                    case "username" -> a.username = val;
                    case "password" -> a.password = val;
                    case "password-env" -> a.passwordEnv = val;
                    case "roles" -> a.roles = val;
                    default -> throw new IllegalArgumentException("unknown option: " + k);
                    }
                    continue;
                }

                throw new IllegalArgumentException("unexpected arg: " + k);
            }

            return a;
        }
    }
}