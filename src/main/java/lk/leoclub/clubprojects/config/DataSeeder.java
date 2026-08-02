package lk.leoclub.clubprojects.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lk.leoclub.clubprojects.model.CatalogItem;
import lk.leoclub.clubprojects.model.CommitteeMember;
import lk.leoclub.clubprojects.model.Project;
import lk.leoclub.clubprojects.repository.CatalogItemRepository;
import lk.leoclub.clubprojects.repository.CommitteeMemberRepository;
import lk.leoclub.clubprojects.repository.ProjectRepository;
import lk.leoclub.clubprojects.service.Catalog;
import lk.leoclub.clubprojects.service.ProjectStatus;

/**
 * Fills an empty database with the 2026/27 committee and the project list the
 * club started the year with. Runs only when the tables are empty, so it never
 * overwrites real data.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final List<String> COMMITTEE = List.of(
            "Lithira Ramuditha", "Onel Herath", "Janith Vishula", "Winnath Edirisooriya",
            "Saviru SeneIrathne", "Matheesha De Silva", "Uvin Kaveesh", "Sandinu Nethmika",
            "Hasindu Induwara", "Vimuth Methmina", "Anuga Kumarajeewa", "Bhagya Supun",
            "Jagadakshi Matheesha", "Sanaya Dewmini", "Kavindu Sachiintha", "Chethiya Peiris",
            "Rusara Sathnidu");

    @Bean
    ApplicationRunner seed(CommitteeMemberRepository membersRepo, ProjectRepository projectsRepo,
                           CatalogItemRepository catalogRepo) {
        return args -> {
            seedCatalog(catalogRepo);
            seedCommittee(membersRepo);
            seedProjects(projectsRepo);
        };
    }

    /** Seeds the pick-lists once; after that admins own them. */
    private void seedCatalog(CatalogItemRepository repo) {
        if (repo.countByKind(CatalogItem.TYPE) == 0) {
            for (int i = 0; i < Catalog.TYPES.size(); i++) {
                repo.save(new CatalogItem(CatalogItem.TYPE, Catalog.TYPES.get(i), i));
            }
            log.info("Seeded {} project types", Catalog.TYPES.size());
        }
        if (repo.countByKind(CatalogItem.CATEGORY) == 0) {
            for (int i = 0; i < Catalog.CATEGORIES.size(); i++) {
                repo.save(new CatalogItem(CatalogItem.CATEGORY, Catalog.CATEGORIES.get(i), i));
            }
            log.info("Seeded {} project categories", Catalog.CATEGORIES.size());
        }
    }

    private void seedCommittee(CommitteeMemberRepository repo) {
        if (repo.count() > 0) {
            return;
        }
        for (int i = 0; i < COMMITTEE.size(); i++) {
            repo.save(new CommitteeMember(COMMITTEE.get(i), i));
        }
        log.info("Seeded {} committee members", COMMITTEE.size());
    }

    private void seedProjects(ProjectRepository repo) {
        if (repo.count() > 0) {
            return;
        }
        List<Project> seed = List.of(
                project("Sadhaham Pooja '26", "Community Service", ProjectStatus.DONE, "2026-07-18", "2026-07-18", 100),
                project("Path to Lead (Phase 7)", "Leadership", ProjectStatus.IN_PROGRESS, "2026-08-03", "", 20),
                project("Mental Health Awareness Program", "Health", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Construction Site Safety Program", "Community Service", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Women Empowerment Project", "Community Service", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Eyalata Adaren (Phase 2)", "Community Service", ProjectStatus.NOT_STARTED, "", "", 0),
                project("English Literacy Program", "Education", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Mocktail Stall for Foreigners", "Fundraising", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Cricket Match Stall", "Fundraising", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Halloween Photo Booth", "Fundraising", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Cancer Hospital Project", "Community Service", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Mental Pressure Support App", "Health / IT", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Beach Cleanup", "Community Service", ProjectStatus.NOT_STARTED, "", "", 0),
                project("Photography Competition", "Fundraising", ProjectStatus.NOT_STARTED, "", "", 0));

        repo.saveAll(seed);
        log.info("Seeded {} projects", seed.size());
    }

    private static Project project(String name, String category, String status,
                                   String start, String due, int progress) {
        Project p = new Project();
        p.setName(name);
        p.setCategory(category);
        p.setStatus(status);
        p.setStartDate(start);
        p.setDueDate(due);
        p.setProgress(progress);
        return p;
    }
}
