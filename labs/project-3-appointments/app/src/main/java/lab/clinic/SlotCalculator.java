package lab.clinic;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * The arithmetic of time, with no framework: working hours are LOCAL times, appointments are INSTANTS, and the
 * bridge between them is the clinic's zone. Because it is a plain class, the daylight-saving cases are plain JUnit.
 *
 * <p>The rule that makes daylight saving work: the length of a block is measured in instants. A block "00:00 to
 * 06:00" is 5 hours long on the day the clocks go forward and 7 on the day they go back.
 */
public final class SlotCalculator {

    /** A block of local time on one day. */
    public record Block(LocalTime start, LocalTime end) {
    }

    /** A half-open interval of instants: it contains {@code start} and not {@code end}. */
    public record Interval(Instant start, Instant end) {
        boolean overlaps(Interval other) {
            return start.isBefore(other.end) && other.start.isBefore(end);
        }
    }

    private SlotCalculator() {
    }

    /**
     * Every start time at which an appointment of {@code duration} fits in a block of {@code date}, does not overlap
     * a booked interval and starts no earlier than {@code earliest} nor later than {@code latest}.
     */
    public static List<Interval> freeSlots(LocalDate date, ZoneId zone, List<Block> blocks, List<Interval> booked,
            Duration duration, Duration step, Instant earliest, Instant latest) {
        List<Interval> slots = new ArrayList<>();
        for (Block block : blocks) {
            Instant blockEnd = date.atTime(block.end()).atZone(zone).toInstant();
            Instant start = date.atTime(block.start()).atZone(zone).toInstant();
            // Stepping in INSTANTS: a local time that does not exist (02:30 on the day the clocks go forward) never appears
            for (Instant t = start; !t.plus(duration).isAfter(blockEnd); t = t.plus(step)) {
                Interval slot = new Interval(t, t.plus(duration));
                if (t.isBefore(earliest) || t.isAfter(latest)) {
                    continue;
                }
                if (booked.stream().noneMatch(slot::overlaps)) {
                    slots.add(slot);
                }
            }
        }
        return slots;
    }

    /** Does {@code [start, end)} lie completely inside one of the blocks of the day it starts on? */
    public static boolean fitsInOneBlock(Instant start, Instant end, ZoneId zone, List<Block> blocksOfThatDay) {
        LocalDate date = start.atZone(zone).toLocalDate();
        for (Block block : blocksOfThatDay) {
            Instant blockStart = date.atTime(block.start()).atZone(zone).toInstant();
            Instant blockEnd = date.atTime(block.end()).atZone(zone).toInstant();
            if (!start.isBefore(blockStart) && !end.isAfter(blockEnd)) {
                return true;
            }
        }
        return false;
    }
}
