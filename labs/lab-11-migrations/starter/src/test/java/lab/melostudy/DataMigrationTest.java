package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Task 3: a migration that changes existing data. The test builds a SCRATCH schema at version 2 (legacy
 * rows in place), applies the rest of the migrations, and checks what happened to those rows.
 * Flyway is driven from the test with its own API; the schema the application uses is never touched.
 */
@QuarkusTest
class DataMigrationTest {

    private static final String SCRATCH_SCHEMA = "migration_scratch";

    @Inject
    DataSource dataSource;

    @AfterEach
    void dropTheScratchSchema() {
        flyway(null).clean();
    }

    @Test
    void legacyCategoriesAreNormalizedByTheMigrationAndNewOnesAreChecked() throws SQLException {
        flyway("2").clean();
        flyway("2").migrate();
        execute("INSERT INTO product (name, name_key, category, price, stock) VALUES "
                + "('Legacy One', 'legacy one', 'Books', 10, 1), ('Legacy Two', 'legacy two', '  GAMES ', 10, 1), "
                + "('Fine', 'fine', 'music', 10, 1)");

        flyway(null).migrate();

        assertEquals(List.of("books", "games", "music"), categories());
        SQLException rejected = assertThrows(SQLException.class, () -> execute(
                "INSERT INTO product (name, name_key, category, price, stock) VALUES ('Late', 'late', 'Books', 10, 1)"));
        assertEquals("23514", rejected.getSQLState());
    }

    private Flyway flyway(String target) {
        var configuration = Flyway.configure().dataSource(dataSource).schemas(SCRATCH_SCHEMA).cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = inScratchSchema()) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } finally {
                connection.setSchema("public"); // the connection goes back to a shared pool
            }
        }
    }

    private List<String> categories() throws SQLException {
        List<String> result = new ArrayList<>();
        try (Connection connection = inScratchSchema()) {
            try (Statement statement = connection.createStatement();
                    ResultSet rows = statement.executeQuery("SELECT category FROM product ORDER BY id")) {
                while (rows.next()) {
                    result.add(rows.getString(1));
                }
            } finally {
                connection.setSchema("public");
            }
        }
        return result;
    }

    private Connection inScratchSchema() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setSchema(SCRATCH_SCHEMA);
        return connection;
    }
}
