package lk.leoclub.clubprojects.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lk.leoclub.clubprojects.dto.ProjectDto;
import lk.leoclub.clubprojects.dto.StatsDto;
import lk.leoclub.clubprojects.model.Project;
import lk.leoclub.clubprojects.repository.ProjectRepository;
import lk.leoclub.clubprojects.repository.ProjectThumbnailRepository;
import lk.leoclub.clubprojects.web.NotFoundException;

@Service
public class ProjectService {

    /** Column order used by the "Copy for Sheets" export. */
    private static final String[][] EXPORT_COLUMNS = {
            { "name", "Main Task" },
            { "type", "Project Type" },
            { "category", "Project Category" },
            { "status", "Status" },
            { "startDate", "Start date" },
            { "dueDate", "End date" },
            { "duration", "Service Hours (Project Duration)" },
            { "venue", "Venue" },
            { "onBehalfOfDistrict", "On behalf of Leo District?" },
            { "onBehalfOfMultipleDistrict", "On behalf of Leo Multiple District?" },
            { "chair", "Project Chairman(s)" },
            { "secretary", "Project Secretary(s)" },
            { "treasurer", "Project Treasurer(s)" },
            { "beneficiaries", "No. of Beneficiaries" },
            { "serviceHours", "Service Hours" },
            { "projectValue", "Project Value" },
            { "funds", "Mode of Funds Raised" },
            { "community", "Benefiting Community" },
            { "need", "Identified Community Need" },
            { "opportunity", "Service Opportunity" },
            { "collection", "Mode of Data Collection" },
            { "participation", "Project Participation" },
            { "chiefGuest", "Chief Guest" },
            { "otherGuests", "Other Guests" },
            { "assignees", "Assign to" },
            { "progress", "Progress %" },
            { "note", "Special Note" },
    };

    private final ProjectRepository repo;
    private final ProjectMapper mapper;
    private final ProjectThumbnailRepository thumbnails;

    public ProjectService(ProjectRepository repo, ProjectMapper mapper,
                          ProjectThumbnailRepository thumbnails) {
        this.repo = repo;
        this.mapper = mapper;
        this.thumbnails = thumbnails;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> list(String query, String status) {
        List<Project> found = (query == null || query.isBlank())
                ? repo.findAll()
                : repo.search(query.trim().toLowerCase());

        return found.stream()
                .filter(p -> status == null || status.isBlank() || status.equalsIgnoreCase(p.getStatus()))
                .sorted(byStatusThenDueDate())
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto get(String id) {
        return mapper.toDto(find(id));
    }

    @Transactional
    public ProjectDto create(ProjectDto dto) {
        Project p = new Project();
        mapper.apply(dto == null ? new ProjectDto() : dto, p);
        return mapper.toDto(repo.save(p));
    }

    @Transactional
    public ProjectDto update(String id, ProjectDto dto) {
        Project p = find(id);
        mapper.apply(dto, p);
        return mapper.toDto(repo.save(p));
    }

    @Transactional
    public void delete(String id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("No project with id " + id);
        }
        // Drop the image too, so no orphan blob is left in the database.
        thumbnails.findById(id).ifPresent(thumbnails::delete);
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public StatsDto stats() {
        List<Project> all = repo.findAll();
        StatsDto s = new StatsDto();
        s.total = all.size();
        s.inProgress = all.stream().filter(p -> ProjectStatus.IN_PROGRESS.equals(p.getStatus())).count();
        s.completed = all.stream().filter(p -> ProjectStatus.DONE.equals(p.getStatus())).count();
        s.notStarted = all.stream().filter(p -> ProjectStatus.NOT_STARTED.equals(p.getStatus())).count();
        s.onHold = all.stream().filter(p -> ProjectStatus.ON_HOLD.equals(p.getStatus())).count();
        s.overdue = all.stream().filter(ProjectService::isOverdue).count();
        s.averageProgress = all.isEmpty()
                ? 0
                : (int) Math.round(all.stream().mapToInt(Project::getProgress).average().orElse(0));
        return s;
    }

    /** Tab-separated export, ready to paste straight into a spreadsheet. */
    @Transactional(readOnly = true)
    public String exportTsv() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
            sb.append(i == 0 ? "" : "\t").append(EXPORT_COLUMNS[i][1]);
        }
        sb.append('\n');

        for (ProjectDto p : list(null, null)) {
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sb.append(i == 0 ? "" : "\t").append(cell(p, EXPORT_COLUMNS[i][0]));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String cell(ProjectDto p, String key) {
        String value = switch (key) {
            case "name" -> p.name;
            case "type" -> p.type;
            case "category" -> p.category;
            case "status" -> p.status;
            case "startDate" -> p.startDate;
            case "dueDate" -> p.dueDate;
            case "duration" -> p.duration;
            case "venue" -> p.venue;
            case "onBehalfOfDistrict" -> p.onBehalfOfDistrict;
            case "onBehalfOfMultipleDistrict" -> p.onBehalfOfMultipleDistrict;
            case "chair" -> p.chair;
            case "secretary" -> p.secretary;
            case "treasurer" -> p.treasurer;
            case "beneficiaries" -> p.beneficiaries;
            case "serviceHours" -> p.serviceHours;
            case "projectValue" -> p.projectValue;
            case "funds" -> p.funds;
            case "community" -> p.community;
            case "need" -> p.need;
            case "opportunity" -> p.opportunity;
            case "collection" -> p.collection;
            case "participation" -> p.participation;
            case "chiefGuest" -> p.chiefGuest;
            case "otherGuests" -> p.otherGuests;
            case "assignees" -> String.join(", ", p.assignees);
            case "progress" -> String.valueOf(p.progress);
            case "note" -> p.note;
            default -> "";
        };
        // Tabs and newlines would break the row/column layout on paste.
        return value == null ? "" : value.replaceAll("[\\t\\r\\n]+", " ");
    }

    private Project find(String id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("No project with id " + id));
    }

    /** Open projects first, then the soonest due date, then alphabetically. */
    private static Comparator<Project> byStatusThenDueDate() {
        return Comparator
                .comparingInt((Project p) -> ProjectStatus.order(p.getStatus()))
                .thenComparing(p -> p.getDueDate() == null || p.getDueDate().isBlank() ? "9999-99-99" : p.getDueDate())
                .thenComparing(p -> p.getName() == null ? "" : p.getName(), String.CASE_INSENSITIVE_ORDER);
    }

    private static boolean isOverdue(Project p) {
        if (ProjectStatus.DONE.equals(p.getStatus()) || p.getDueDate() == null || p.getDueDate().isBlank()) {
            return false;
        }
        try {
            return LocalDate.parse(p.getDueDate()).isBefore(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
