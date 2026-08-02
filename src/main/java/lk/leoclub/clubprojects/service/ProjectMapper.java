package lk.leoclub.clubprojects.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import lk.leoclub.clubprojects.dto.ProjectDto;
import lk.leoclub.clubprojects.dto.SubTaskDto;
import lk.leoclub.clubprojects.model.Project;
import lk.leoclub.clubprojects.model.SubTask;

@Component
public class ProjectMapper {

    public ProjectDto toDto(Project p) {
        ProjectDto d = new ProjectDto();
        d.id = p.getId();
        d.name = p.getName();
        d.type = p.getType();
        d.category = p.getCategory();
        d.status = p.getStatus();
        d.startDate = p.getStartDate();
        d.dueDate = p.getDueDate();
        d.duration = p.getDuration();
        d.venue = p.getVenue();
        // Columns added after the first release are null on older rows.
        d.onBehalfOfDistrict = nz(p.getOnBehalfOfDistrict());
        d.onBehalfOfMultipleDistrict = nz(p.getOnBehalfOfMultipleDistrict());
        d.chair = p.getChair();
        d.secretary = p.getSecretary();
        d.treasurer = p.getTreasurer();
        d.participation = p.getParticipation();
        d.beneficiaries = p.getBeneficiaries();
        d.serviceHours = p.getServiceHours();
        d.projectValue = p.getProjectValue();
        d.funds = p.getFunds();
        d.community = p.getCommunity();
        d.need = p.getNeed();
        d.opportunity = p.getOpportunity();
        d.collection = p.getCollection();
        d.chiefGuest = p.getChiefGuest();
        d.otherGuests = p.getOtherGuests();
        d.note = p.getNote();
        d.progress = p.getProgress();
        d.filledFields = ProjectCompletion.filled(p);
        d.totalFields = ProjectCompletion.total();
        d.hasThumbnail = p.getThumbnailUpdatedAt() != null;
        d.thumbnailVersion = p.getThumbnailUpdatedAt();
        d.assignees = new LinkedHashSet<>(p.getAssignees());
        d.tasks = p.getTasks().stream()
                .map(t -> new SubTaskDto(t.getId(), t.getTitle(), t.isDone()))
                .toList();
        d.updatedAt = p.getUpdatedAt() == null ? null : p.getUpdatedAt().toString();

        LocalDate due = parseDate(p.getDueDate());
        if (due != null) {
            d.daysToDue = (int) ChronoUnit.DAYS.between(LocalDate.now(), due);
            d.overdue = d.daysToDue < 0 && !ProjectStatus.DONE.equals(p.getStatus());
        }
        return d;
    }

    /** Copies every client-editable field from the DTO onto the entity. */
    public void apply(ProjectDto d, Project p) {
        p.setName(nz(d.name));
        p.setType(blankTo(d.type, "Club Project"));
        p.setCategory(nz(d.category));
        p.setStatus(ProjectStatus.normalise(d.status));
        p.setStartDate(nz(d.startDate));
        p.setDueDate(nz(d.dueDate));
        p.setDuration(nz(d.duration));
        p.setVenue(nz(d.venue));
        p.setOnBehalfOfDistrict(yesNo(d.onBehalfOfDistrict));
        p.setOnBehalfOfMultipleDistrict(yesNo(d.onBehalfOfMultipleDistrict));
        p.setChair(nz(d.chair));
        p.setSecretary(nz(d.secretary));
        p.setTreasurer(nz(d.treasurer));
        p.setParticipation(nz(d.participation));
        p.setBeneficiaries(nz(d.beneficiaries));
        p.setServiceHours(nz(d.serviceHours));
        p.setProjectValue(nz(d.projectValue));
        p.setFunds(nz(d.funds));
        p.setCommunity(nz(d.community));
        p.setNeed(nz(d.need));
        p.setOpportunity(nz(d.opportunity));
        p.setCollection(nz(d.collection));
        p.setChiefGuest(nz(d.chiefGuest));
        p.setOtherGuests(nz(d.otherGuests));
        p.setNote(nz(d.note));

        p.setAssignees(d.assignees == null ? new LinkedHashSet<>() : new LinkedHashSet<>(d.assignees));

        applyTasks(d.tasks, p);

        // Progress is measured, not supplied: whatever the client sends for it is
        // ignored in favour of how much of the record is actually filled in.
        p.setProgress(ProjectCompletion.percent(p));
    }

    /**
     * Merges the incoming checklist onto the persistent one by id, so rows that
     * survive an edit are updated rather than deleted and re-inserted. Anything
     * left over is dropped by orphanRemoval.
     */
    private void applyTasks(List<SubTaskDto> source, Project p) {
        List<SubTaskDto> incoming = source == null ? List.of() : source;

        Map<String, SubTask> existing = new LinkedHashMap<>();
        for (SubTask t : p.getTasks()) {
            existing.put(t.getId(), t);
        }

        List<SubTask> merged = new ArrayList<>(incoming.size());
        for (int i = 0; i < incoming.size(); i++) {
            SubTaskDto dto = incoming.get(i);
            SubTask task = (dto.id == null || dto.id.isBlank()) ? null : existing.remove(dto.id);
            if (task == null) {
                task = new SubTask();
            }
            task.setTitle(nz(dto.title));
            task.setDone(dto.done);
            task.setPosition(i);
            merged.add(task);
        }

        p.getTasks().clear();
        p.getTasks().addAll(merged);
    }

    private static LocalDate parseDate(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    /** Accepts only "Yes" or "No"; anything else means the question is unanswered. */
    private static String yesNo(String s) {
        String v = nz(s);
        if (v.equalsIgnoreCase("Yes")) {
            return "Yes";
        }
        if (v.equalsIgnoreCase("No")) {
            return "No";
        }
        return "";
    }

    private static String blankTo(String s, String fallback) {
        String v = nz(s);
        return v.isEmpty() ? fallback : v;
    }
}
