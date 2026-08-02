package lk.leoclub.clubprojects.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lk.leoclub.clubprojects.model.Project;

public interface ProjectRepository extends JpaRepository<Project, String> {

    long countByStatus(String status);

    /**
     * Assignee name paired with the number of projects it appears on, for the
     * whole committee in one query. Returns rows of {@code [String, Long]}.
     */
    @Query("select a, count(p) from Project p join p.assignees a group by a")
    List<Object[]> countProjectsPerAssignee();

    /** Project type paired with how many projects use it. Rows of {@code [String, Long]}. */
    @Query("select p.type, count(p) from Project p group by p.type")
    List<Object[]> countProjectsPerType();

    /** Project category paired with how many projects use it. */
    @Query("select p.category, count(p) from Project p group by p.category")
    List<Object[]> countProjectsPerCategory();

    /**
     * Free-text search across the fields a committee member would actually
     * search by, including the names of everyone assigned to the project.
     */
    @Query("""
            select distinct p from Project p
            left join p.assignees a
            where lower(coalesce(p.name, ''))      like concat('%', :q, '%')
               or lower(coalesce(p.category, ''))  like concat('%', :q, '%')
               or lower(coalesce(p.type, ''))      like concat('%', :q, '%')
               or lower(coalesce(p.venue, ''))     like concat('%', :q, '%')
               or lower(coalesce(p.chair, ''))     like concat('%', :q, '%')
               or lower(coalesce(p.secretary, '')) like concat('%', :q, '%')
               or lower(coalesce(p.treasurer, '')) like concat('%', :q, '%')
               or lower(coalesce(p.note, ''))      like concat('%', :q, '%')
               or lower(coalesce(a, ''))           like concat('%', :q, '%')
            """)
    List<Project> search(@Param("q") String lowercaseQuery);
}
