package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Task 1: the database that Dev Services starts for the tests is the one production runs, with the data we need. */
@QuarkusTest
class FeeTableTest {

    @Inject
    FeeRepository fees;

    @Inject
    DataSource dataSource;

    @Test
    void theTestDatabaseIsPostgreSqlSeventeen() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT current_setting('server_version_num')")) {
            rows.next();
            // server_version_num is 170011 for 17.11, 180000 for 18.0
            assertTrue(rows.getString(1).startsWith("17"), "server_version_num = " + rows.getString(1));
        }
    }

    @Test
    void theFeesComeFromTheInitScript() {
        assertEquals(0, new BigDecimal("1.00").compareTo(fees.percent("USD", "EUR").orElseThrow()));
        assertEquals(0, new BigDecimal("1.50").compareTo(fees.percent("EUR", "USD").orElseThrow()));
        assertEquals(0, new BigDecimal("0.75").compareTo(fees.percent("USD", "GBP").orElseThrow()));
    }

    @Test
    void aPairWithoutAFeeHasNoEntry() {
        assertTrue(fees.percent("USD", "PEN").isEmpty());
        assertTrue(fees.percent("USD", "EUR").isPresent(), "the table exists and has its rows"); // not vacuous
    }
}
