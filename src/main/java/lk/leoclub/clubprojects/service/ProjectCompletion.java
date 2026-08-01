package lk.leoclub.clubprojects.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import lk.leoclub.clubprojects.model.Project;

/**
 * Works out how complete a project's record is: the share of the reporting
 * fields that actually have something in them.
 *
 * <p>This is the single source of truth for the progress bar. The browser asks
 * for {@link #fieldNames()} through {@code /api/meta} and applies the same rule
 * while you type, so the live number and the stored one can never drift apart.
 *
 * <p>{@code type} and {@code category} are deliberately left out — they always
 * carry a default, so counting them would hand out free progress.
 */
public final class ProjectCompletion {

    /** Assignees are a list rather than a text field, so they are counted separately. */
    public static final String ASSIGNEES = "assignees";

    private static final Map<String, Function<Project, String>> TRACKED = trackedFields();

    private ProjectCompletion() {
    }

    private static Map<String, Function<Project, String>> trackedFields() {
        Map<String, Function<Project, String>> m = new LinkedHashMap<>();
        // The basics
        m.put("startDate", Project::getStartDate);
        m.put("dueDate", Project::getDueDate);
        m.put("duration", Project::getDuration);
        m.put("venue", Project::getVenue);
        // Who is running it
        m.put("chair", Project::getChair);
        m.put("secretary", Project::getSecretary);
        m.put("treasurer", Project::getTreasurer);
        m.put("participation", Project::getParticipation);
        // Impact and reporting
        m.put("beneficiaries", Project::getBeneficiaries);
        m.put("serviceHours", Project::getServiceHours);
        m.put("projectValue", Project::getProjectValue);
        m.put("funds", Project::getFunds);
        m.put("community", Project::getCommunity);
        m.put("collection", Project::getCollection);
        m.put("need", Project::getNeed);
        m.put("opportunity", Project::getOpportunity);
        // Guests and notes
        m.put("chiefGuest", Project::getChiefGuest);
        m.put("otherGuests", Project::getOtherGuests);
        m.put("note", Project::getNote);
        return Collections.unmodifiableMap(m);
    }

    /** Field names in form order, with {@code assignees} last. */
    public static List<String> fieldNames() {
        List<String> names = new ArrayList<>(TRACKED.keySet());
        names.add(ASSIGNEES);
        return List.copyOf(names);
    }

    /** How many of the tracked fields have been filled in. */
    public static int filled(Project p) {
        int count = 0;
        for (Function<Project, String> read : TRACKED.values()) {
            String value = read.apply(p);
            if (value != null && !value.isBlank()) {
                count++;
            }
        }
        if (p.getAssignees() != null && !p.getAssignees().isEmpty()) {
            count++;
        }
        return count;
    }

    public static int total() {
        return TRACKED.size() + 1;
    }

    /** Completeness as a 0–100 percentage. */
    public static int percent(Project p) {
        int total = total();
        return total == 0 ? 0 : (int) Math.round(filled(p) * 100.0 / total);
    }
}
