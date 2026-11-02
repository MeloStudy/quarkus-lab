package lab.melostudy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import jakarta.enterprise.context.ApplicationScoped;

/** Reads the fee table with plain JDBC. The table is not an entity: it is created by the database (Dev Services script here). */
@ApplicationScoped
public class FeeRepository {

    private final DataSource dataSource;

    FeeRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<BigDecimal> percent(String from, String to) {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection
                        .prepareStatement("SELECT percent FROM exchange_fee WHERE from_code = ? AND to_code = ?")) {
            statement.setString(1, from);
            statement.setString(2, to);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(rows.getBigDecimal(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
