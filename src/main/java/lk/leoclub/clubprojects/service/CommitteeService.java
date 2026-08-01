package lk.leoclub.clubprojects.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lk.leoclub.clubprojects.dto.CommitteeMemberDto;
import lk.leoclub.clubprojects.model.CommitteeMember;
import lk.leoclub.clubprojects.repository.CommitteeMemberRepository;
import lk.leoclub.clubprojects.repository.ProjectRepository;
import lk.leoclub.clubprojects.web.BadRequestException;
import lk.leoclub.clubprojects.web.NotFoundException;

@Service
public class CommitteeService {

    private static final int MAX_NAME_LENGTH = 80;

    private final CommitteeMemberRepository members;
    private final ProjectRepository projects;

    public CommitteeService(CommitteeMemberRepository members, ProjectRepository projects) {
        this.members = members;
        this.projects = projects;
    }

    @Transactional(readOnly = true)
    public List<CommitteeMemberDto> list() {
        Map<String, Integer> counts = projectCounts();
        return members.findAllByOrderByPositionAsc().stream()
                .map(m -> new CommitteeMemberDto(
                        m.getId(), m.getName(), m.getRole(),
                        counts.getOrDefault(m.getName(), 0)))
                .toList();
    }

    @Transactional
    public CommitteeMemberDto add(String rawName) {
        String name = rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new BadRequestException("Please enter a name.");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BadRequestException("That name is too long.");
        }
        if (members.existsByName(name)) {
            throw new BadRequestException(name + " is already on the committee.");
        }

        CommitteeMember saved = members.save(new CommitteeMember(name, nextPosition()));
        // A brand new member cannot be on any project yet.
        return new CommitteeMemberDto(saved.getId(), saved.getName(), saved.getRole(), 0);
    }

    /**
     * Takes someone off the committee. Projects they already ran keep their name
     * on the assignee list — the history stays true, they just stop appearing as
     * a choice for new work.
     */
    @Transactional
    public void remove(String id) {
        if (!members.existsById(id)) {
            throw new NotFoundException("No committee member with id " + id);
        }
        members.deleteById(id);
    }

    private int nextPosition() {
        return members.findAllByOrderByPositionAsc().stream()
                .mapToInt(CommitteeMember::getPosition)
                .max()
                .orElse(-1) + 1;
    }

    private Map<String, Integer> projectCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Object[] row : projects.countProjectsPerAssignee()) {
            counts.put((String) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }
}
