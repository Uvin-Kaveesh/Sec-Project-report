package lk.leoclub.clubprojects.service;

import java.util.List;

/** The four states a project can be in, and the order they are shown in. */
public final class ProjectStatus {

    public static final String IN_PROGRESS = "In progress";
    public static final String DONE = "Done";
    public static final String NOT_STARTED = "Not started";
    public static final String ON_HOLD = "On hold";

    public static final List<String> ALL = List.of(IN_PROGRESS, DONE, NOT_STARTED, ON_HOLD);

    private ProjectStatus() {
    }

    /** Falls back to "Not started" for anything unrecognised. */
    public static String normalise(String status) {
        if (status == null) {
            return NOT_STARTED;
        }
        return ALL.stream()
                .filter(s -> s.equalsIgnoreCase(status.trim()))
                .findFirst()
                .orElse(NOT_STARTED);
    }

    public static int order(String status) {
        int i = ALL.indexOf(normalise(status));
        return i < 0 ? ALL.size() : i;
    }
}
