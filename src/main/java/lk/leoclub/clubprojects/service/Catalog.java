package lk.leoclub.clubprojects.service;

import java.util.List;

/** Fixed pick-lists offered by the UI. */
public final class Catalog {

    public static final List<String> TYPES = List.of(
            "Club Project", "Joint Project", "Zone Project", "District Project");

    public static final List<String> CATEGORIES = List.of(
            "Community Service", "Fundraising", "Health", "Education",
            "Leadership", "Environment", "Sports", "Health / IT");

    private Catalog() {
    }
}
