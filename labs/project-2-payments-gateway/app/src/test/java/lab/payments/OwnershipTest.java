package lab.payments;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/** Whose payments are whose: written against the identity in the token, never against anything the client sends (lab 19). */
@QuarkusTest
@TestProfile(LogProfile.class)
class OwnershipTest extends PaymentsTestBase {

    private final String ana = Tokens.customer(Api.ANA);
    private final String ben = Tokens.customer(Api.BEN);

    @Test
    void aCustomerReadsTheirOwnPayment() {
        String id = Api.pay(ana, "o-1", Api.body(Api.CARD)).then().statusCode(201).extract().path("id");

        Api.get(ana, id).then().statusCode(200).body("id", is(id)).body("status", is("CAPTURED"));
    }

    @Test
    void somebodyElsesPaymentDoesNotExist() {
        String id = Api.pay(ana, "o-2", Api.body(Api.CARD)).then().statusCode(201).extract().path("id");
        Api.get(ana, id).then().statusCode(200);

        // 404, not 403: a 403 would confirm that the id exists. The answer is the same as for an id that never existed.
        var theirs = Api.get(ben, id).then().statusCode(404).extract().path("code");
        var nobodys = Api.get(ben, "pay_doesnotexist").then().statusCode(404).extract().path("code");
        org.junit.jupiter.api.Assertions.assertEquals(nobodys, theirs);
    }

    @Test
    void financeSeesEveryPayment() {
        String id = Api.pay(ana, "o-3", Api.body(Api.CARD)).then().statusCode(201).extract().path("id");

        Api.get(Tokens.finance("emp_1"), id).then().statusCode(200).body("id", is(id));
    }

    @Test
    void aListHoldsOnlyWhatTheCallerMaySee() {
        String anas = Api.pay(ana, "o-4a", Api.body(Api.CARD)).then().statusCode(201).extract().path("id");
        String bens = Api.pay(ben, "o-4b", Api.body(Api.CARD)).then().statusCode(201).extract().path("id");

        Api.as(ana).get("/api/payments").then().statusCode(200).body("id", containsInAnyOrder(anas));
        Api.as(ben).get("/api/payments").then().statusCode(200).body("id", containsInAnyOrder(bens));
        Api.as(Tokens.finance("emp_1")).get("/api/payments").then().statusCode(200).body("id", containsInAnyOrder(anas, bens));
    }

    @Test
    void aListCanBeFilteredByStatusAndAMadeUpStatusIs400() {
        Api.pay(ana, "o-5a", Api.body(Api.CARD)).then().statusCode(201);
        Api.pay(ana, "o-5b", Api.body("tok_declined1")).then().statusCode(402);

        Api.as(ana).get("/api/payments?status=DECLINED").then().statusCode(200).body("$", hasSize(1)).body("[0].status", is("DECLINED"));
        Api.as(ana).get("/api/payments?status=captured").then().statusCode(200).body("$", hasSize(1));
        Api.as(ana).get("/api/payments?status=REFUNDED").then().statusCode(200).body("$", empty());
        Api.as(ana).get("/api/payments?status=PAID").then().statusCode(400).body("code", is("VALIDATION_ERROR"))
                .body("errors.field", containsInAnyOrder("status"));
    }

    @Test
    void theOwnerCannotBeChosenByTheClient() {
        // The request has no "customer" field: sending one is a client bug that must be reported, never obeyed.
        String body = "{\"amount\":2500,\"currency\":\"EUR\",\"cardToken\":\"" + Api.CARD + "\",\"customer\":\"" + Api.BEN + "\"}";
        Api.pay(ana, "o-6", body).then().statusCode(400).body("code", is("MALFORMED_REQUEST"));
        Api.as(ben).get("/api/payments").then().statusCode(200).body("$", empty());
    }
}
