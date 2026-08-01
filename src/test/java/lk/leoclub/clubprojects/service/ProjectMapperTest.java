package lk.leoclub.clubprojects.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import lk.leoclub.clubprojects.dto.ProjectDto;
import lk.leoclub.clubprojects.dto.SubTaskDto;
import lk.leoclub.clubprojects.model.Project;
import lk.leoclub.clubprojects.model.SubTask;

class ProjectMapperTest {

    private final ProjectMapper mapper = new ProjectMapper();

    @Test
    void anEmptyRecordScoresZero() {
        Project p = new Project();
        mapper.apply(new ProjectDto(), p);

        assertThat(p.getProgress()).isZero();
    }

    @Test
    void progressSentByTheClientIsIgnoredInFavourOfWhatIsActuallyFilledIn() {
        ProjectDto dto = new ProjectDto();
        dto.progress = 95;   // a client claiming to be nearly done...
        dto.venue = "Club Hall";

        Project p = new Project();
        mapper.apply(dto, p);

        // ...gets scored on the one field it actually filled.
        assertThat(p.getProgress()).isEqualTo(Math.round(100f / ProjectCompletion.total()));
    }

    @Test
    void fillingEveryTrackedFieldReachesOneHundred() {
        ProjectDto dto = new ProjectDto();
        dto.startDate = "2026-08-01";
        dto.dueDate = "2026-08-10";
        dto.duration = "4";
        dto.venue = "Club Hall";
        dto.chair = "A";
        dto.secretary = "B";
        dto.treasurer = "C";
        dto.participation = "12";
        dto.beneficiaries = "40";
        dto.serviceHours = "48";
        dto.projectValue = "25000";
        dto.funds = "Sponsorships";
        dto.community = "Local school";
        dto.collection = "Forms";
        dto.need = "Literacy gap";
        dto.opportunity = "Teaching";
        dto.chiefGuest = "Guest";
        dto.otherGuests = "Others";
        dto.note = "Nothing outstanding";
        dto.assignees = new LinkedHashSet<>(List.of("Uvin Kaveesh"));

        Project p = new Project();
        mapper.apply(dto, p);

        assertThat(p.getProgress()).isEqualTo(100);
    }

    @Test
    void markingDoneNoLongerInflatesAnEmptyRecord() {
        ProjectDto dto = new ProjectDto();
        dto.status = "Done";

        Project p = new Project();
        mapper.apply(dto, p);

        assertThat(p.getStatus()).isEqualTo(ProjectStatus.DONE);
        assertThat(p.getProgress()).isZero();
    }

    @Test
    void unknownStatusFallsBackToNotStarted() {
        ProjectDto dto = new ProjectDto();
        dto.status = "Whatever";

        Project p = new Project();
        mapper.apply(dto, p);

        assertThat(p.getStatus()).isEqualTo(ProjectStatus.NOT_STARTED);
    }

    @Test
    void editingAStepKeepsItsExistingRowInsteadOfReplacingIt() {
        Project p = new Project();
        SubTask existing = new SubTask("Book the venue", false, 0);
        p.getTasks().add(existing);

        ProjectDto dto = new ProjectDto();
        dto.tasks = List.of(new SubTaskDto(existing.getId(), "Book the venue", true));

        mapper.apply(dto, p);

        assertThat(p.getTasks()).hasSize(1);
        assertThat(p.getTasks().get(0)).isSameAs(existing);
        assertThat(p.getTasks().get(0).isDone()).isTrue();
    }

    @Test
    void removedStepsAreDropped() {
        Project p = new Project();
        p.getTasks().add(new SubTask("One", false, 0));
        p.getTasks().add(new SubTask("Two", false, 1));

        ProjectDto dto = new ProjectDto();
        dto.tasks = List.of();

        mapper.apply(dto, p);

        assertThat(p.getTasks()).isEmpty();
    }

    @Test
    void overdueIsOnlyReportedForUnfinishedWork() {
        Project p = new Project();
        p.setStatus(ProjectStatus.DONE);
        p.setDueDate("2020-01-01");

        assertThat(mapper.toDto(p).overdue).isFalse();

        p.setStatus(ProjectStatus.IN_PROGRESS);
        assertThat(mapper.toDto(p).overdue).isTrue();
    }
}
