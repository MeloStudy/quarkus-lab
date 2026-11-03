package lab.melostudy;

import java.time.Duration;
import java.util.Set;

import io.smallrye.jwt.build.Jwt;

/**
 * Prints a signed token so you can call the running service with curl.
 * Arguments: upn, then the groups (for example {@code alice@lab.example editor reader}).
 */
public class GenerateToken {

    public static void main(String[] args) {
        String upn = args.length > 0 ? args[0] : "alice@lab.example";
        Set<String> groups = args.length > 1 ? Set.of(java.util.Arrays.copyOfRange(args, 1, args.length)) : Set.of("reader");
        String token = Jwt.issuer(Tokens.ISSUER)
                .upn(upn)
                .groups(groups)
                .audience(Tokens.AUDIENCE)
                .expiresIn(Duration.ofMinutes(30))
                .sign("privateKey.pem");
        System.out.println(token);
    }
}
