package lab.clinic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import lab.clinic.SlotCalculator.Block;
import lab.clinic.SlotCalculator.Interval;

/** Plain JUnit: the arithmetic of time needs no Quarkus, so the daylight-saving cases run in milliseconds. */
class SlotCalculatorTest {

    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final Duration QUARTER = Duration.ofMinutes(15);
    private static final Instant LONG_AGO = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant FAR_AHEAD = Instant.parse("2100-01-01T00:00:00Z");

    private static List<Interval> slots(String date, List<Block> blocks, List<Interval> booked, Duration duration) {
        return SlotCalculator.freeSlots(LocalDate.parse(date), MADRID, blocks, booked, duration, QUARTER, LONG_AGO, FAR_AHEAD);
    }

    private static Block block(String start, String end) {
        return new Block(LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void anOrdinaryMorningHasAStartEveryQuarterHourUntilTheLastOneThatFits() {
        List<Interval> result = slots("2026-03-09", List.of(block("09:00", "13:00")), List.of(), Duration.ofMinutes(30));

        assertEquals(15, result.size()); // 09:00 ... 12:30
        assertEquals(Instant.parse("2026-03-09T08:00:00Z"), result.get(0).start()); // 09:00 CET
        assertEquals(Instant.parse("2026-03-09T11:30:00Z"), result.get(14).start()); // 12:30 CET
    }

    @Test
    void theNightThatLosesAnHourHasFiveHoursOfSlotsNotSix() {
        // Sunday 2026-03-29: at 02:00 the clocks jump to 03:00. A block "00:00-06:00" is 5 real hours long.
        List<Interval> result = slots("2026-03-29", List.of(block("00:00", "06:00")), List.of(), QUARTER);

        assertEquals(20, result.size());
        assertTrue(result.stream().noneMatch(s -> s.start().atZone(MADRID).getHour() == 2),
                "02:xx does not exist on that day");
        assertEquals(Instant.parse("2026-03-28T23:00:00Z"), result.get(0).start()); // 00:00 CET
        assertEquals(Instant.parse("2026-03-29T03:45:00Z"), result.get(19).start()); // 05:45 CEST
    }

    @Test
    void theNightThatGainsAnHourHasSevenHoursOfSlotsAndTwoStartsAtTwoOClock() {
        // Sunday 2026-10-25: at 03:00 the clocks go back to 02:00. A block "00:00-06:00" is 7 real hours long.
        List<Interval> result = slots("2026-10-25", List.of(block("00:00", "06:00")), List.of(), QUARTER);

        assertEquals(28, result.size());
        List<Interval> twoOClock = result.stream()
                .filter(s -> s.start().atZone(MADRID).toLocalTime().equals(LocalTime.of(2, 0))).toList();
        assertEquals(2, twoOClock.size(), "the local time 02:00 happens twice");
        assertEquals(Instant.parse("2026-10-25T00:00:00Z"), twoOClock.get(0).start()); // 02:00 CEST (+02:00)
        assertEquals(Instant.parse("2026-10-25T01:00:00Z"), twoOClock.get(1).start()); // 02:00 CET (+01:00)
    }

    @Test
    void aBookedIntervalRemovesEverySlotThatOverlapsItAndNoOther() {
        Interval booked = new Interval(Instant.parse("2026-03-09T09:00:00Z"), Instant.parse("2026-03-09T09:30:00Z")); // 10:00-10:30
        List<Interval> result = slots("2026-03-09", List.of(block("09:00", "13:00")), List.of(booked), Duration.ofMinutes(30));

        List<Instant> starts = result.stream().map(Interval::start).toList();
        assertTrue(starts.contains(Instant.parse("2026-03-09T08:30:00Z")), "09:30-10:00 ends exactly when the booking starts");
        assertFalse(starts.contains(Instant.parse("2026-03-09T08:45:00Z")), "09:45-10:15 overlaps");
        assertFalse(starts.contains(Instant.parse("2026-03-09T09:00:00Z")), "10:00-10:30 is the booking");
        assertFalse(starts.contains(Instant.parse("2026-03-09T09:15:00Z")), "10:15-10:45 overlaps");
        assertTrue(starts.contains(Instant.parse("2026-03-09T09:30:00Z")), "10:30-11:00 starts exactly when the booking ends");
        assertEquals(15 - 3, result.size());
    }

    @Test
    void slotsBeforeTheEarliestAndAfterTheLatestAreLeftOut() {
        Instant earliest = Instant.parse("2026-03-09T09:00:00Z"); // 10:00 CET
        Instant latest = Instant.parse("2026-03-09T09:30:00Z"); // 10:30 CET
        List<Interval> result = SlotCalculator.freeSlots(LocalDate.parse("2026-03-09"), MADRID,
                List.of(block("09:00", "13:00")), List.of(), Duration.ofMinutes(30), QUARTER, earliest, latest);

        assertEquals(3, result.size()); // 10:00, 10:15, 10:30
        assertEquals(earliest, result.get(0).start());
        assertEquals(latest, result.get(2).start());
    }

    @Test
    void twoBlocksWithALunchBreakNeverProduceASlotAcrossTheBreak() {
        List<Interval> result = slots("2026-03-09", List.of(block("09:00", "13:00"), block("15:00", "18:00")), List.of(),
                Duration.ofMinutes(60));

        // 09:00 ... 12:00 = 13 starts; 15:00 ... 17:00 = 9 starts
        assertEquals(13 + 9, result.size());
        assertTrue(result.stream().noneMatch(s -> s.start().atZone(MADRID).getHour() == 13
                || s.start().atZone(MADRID).getHour() == 14));
    }

    @Test
    void anAppointmentFitsInABlockOnlyIfItEndsBeforeTheBlockDoes() {
        List<Block> blocks = List.of(block("09:00", "13:00"), block("15:00", "18:00"));
        Instant noon = Instant.parse("2026-03-09T11:00:00Z"); // 12:00 CET

        assertTrue(SlotCalculator.fitsInOneBlock(noon, noon.plus(Duration.ofMinutes(60)), MADRID, blocks));
        assertFalse(SlotCalculator.fitsInOneBlock(noon, noon.plus(Duration.ofMinutes(75)), MADRID, blocks),
                "it would end at 13:15");
        assertFalse(SlotCalculator.fitsInOneBlock(Instant.parse("2026-03-09T11:45:00Z"), Instant.parse("2026-03-09T14:15:00Z"),
                MADRID, blocks), "it would cross the lunch break");
    }
}
