package lab.melostudy;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

/**
 * By default Jackson is lenient with dates: {@code [2026,12,1]} and {@code "2026-12-01T10:00:00"} are both accepted
 * as a {@link LocalDate}. For a public contract that is a bug magnet; this makes a calendar day mean exactly
 * {@code "yyyy-MM-dd"}.
 */
@Singleton
public class StrictDates implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper mapper) {
        mapper.configOverride(LocalDate.class).setFormat(JsonFormat.Value.forLeniency(Boolean.FALSE));
    }
}
