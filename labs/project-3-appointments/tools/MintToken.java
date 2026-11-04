import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Mints a signed token for trying the packaged application by hand, with nothing but the JDK:
 *
 * <pre>java tools/MintToken.java reception front-desk
 * java tools/MintToken.java patient bea 1
 * java tools/MintToken.java practitioner ana 1</pre>
 *
 * A patient token carries {@code patient_id} and a practitioner token {@code practitioner_id}: the number is the last argument. It signs
 * with the throw-away private key of the tests ({@code app/src/test/resources/privateKey.pem}): the matching public key is the one the
 * application verifies with. Nothing here protects anything; in a real system the identity provider mints tokens and its private key
 * never leaves it. The token lives for one hour.
 */
public class MintToken {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: java tools/MintToken.java <reception|practitioner|patient> <subject> [id] [path to privateKey.pem]");
            System.exit(1);
        }
        String role = args[0];
        String subject = args[1];
        String id = args.length > 2 ? args[2] : null;
        Path keyFile = Path.of(args.length > 3 ? args[3] : "app/src/test/resources/privateKey.pem");

        String pem = Files.readString(keyFile).replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));

        String idClaim = id == null ? "" : ",\"" + (role.equals("patient") ? "patient_id" : "practitioner_id") + "\":" + id;
        long now = System.currentTimeMillis() / 1000;
        String header = base64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = base64(("{\"iss\":\"https://clinic.example/issuer\",\"aud\":\"clinic-api\",\"sub\":\"" + subject
                + "\",\"upn\":\"" + subject + "\",\"groups\":[\"" + role + "\"]" + idClaim + ",\"iat\":" + now + ",\"exp\":" + (now + 3600) + "}")
                .getBytes(StandardCharsets.UTF_8));

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(key);
        signature.update((header + "." + payload).getBytes(StandardCharsets.UTF_8));
        System.out.println(header + "." + payload + "." + base64(signature.sign()));
    }

    private static String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
