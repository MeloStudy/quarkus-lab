package lab.melostudy;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;

/** Request helpers shared by the tests. */
public final class Api {

    private Api() {
    }

    public static RequestSpecification bearer(String token) {
        return given().header("Authorization", "Bearer " + token);
    }

    public static RequestSpecification as(String upn, String... groups) {
        return bearer(Tokens.signed(upn, groups));
    }
}
