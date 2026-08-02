package lk.leoclub.clubprojects.dto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Wire format for a project. Public fields keep this readable — it is a plain
 * data carrier, not a domain object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectDto {

    public String id;
    public String name = "";
    public String type = "Club Project";
    public String category = "Community Service";
    public String status = "Not started";

    public String startDate = "";
    public String dueDate = "";
    public String duration = "";
    public String venue = "";

    /** "Yes", "No", or empty when unanswered. */
    public String onBehalfOfDistrict = "";
    public String onBehalfOfMultipleDistrict = "";

    public String chair = "";
    public String secretary = "";
    public String treasurer = "";
    public String participation = "";

    public String beneficiaries = "";
    public String serviceHours = "";
    public String projectValue = "";
    public String funds = "";
    public String community = "";
    public String need = "";
    public String opportunity = "";
    public String collection = "";

    public String chiefGuest = "";
    public String otherGuests = "";
    public String note = "";

    /** Derived, read-only: record completeness as a percentage. */
    public int progress = 0;

    /** Derived, read-only: how the progress percentage was arrived at. */
    public int filledFields;
    public int totalFields;

    /** Derived, read-only: whether a thumbnail has been uploaded. */
    public boolean hasThumbnail;

    /** Derived, read-only: changes when the image is replaced, to bust caches. */
    public Long thumbnailVersion;

    public Set<String> assignees = new LinkedHashSet<>();
    public List<SubTaskDto> tasks = new ArrayList<>();

    /** Derived, read-only: days until the due date (negative when overdue). */
    public Integer daysToDue;

    /** Derived, read-only: true when the due date has passed and work is unfinished. */
    public boolean overdue;

    public String updatedAt;
}
