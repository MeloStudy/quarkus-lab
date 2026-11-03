package lab.tracker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** {@code TRK-12}: the public name of an issue (project code + number inside the project). */
public record IssueKey(String code, int number) {

    private static final Pattern FORMAT = Pattern.compile("([A-Z]{2,5})-([1-9][0-9]{0,8})");

    /** A key that cannot exist is "not found", not "bad request": the URL is well-formed, the thing is absent. */
    public static IssueKey parse(String text) {
        Matcher m = FORMAT.matcher(text == null ? "" : text);
        if (!m.matches()) {
            throw TrackerException.notFound("Issue " + text + " does not exist");
        }
        return new IssueKey(m.group(1), Integer.parseInt(m.group(2)));
    }

    @Override
    public String toString() {
        return code + "-" + number;
    }
}
