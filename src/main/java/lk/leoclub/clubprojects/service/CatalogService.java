package lk.leoclub.clubprojects.service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lk.leoclub.clubprojects.dto.CatalogItemDto;
import lk.leoclub.clubprojects.model.CatalogItem;
import lk.leoclub.clubprojects.repository.CatalogItemRepository;
import lk.leoclub.clubprojects.repository.ProjectRepository;
import lk.leoclub.clubprojects.web.BadRequestException;
import lk.leoclub.clubprojects.web.NotFoundException;

/**
 * The project-form pick-lists — types and categories — as editable data rather
 * than a hardcoded list. Admins manage them from the admin panel.
 */
@Service
public class CatalogService {

    private static final int MAX_LABEL = 80;

    private final CatalogItemRepository items;
    private final ProjectRepository projects;

    public CatalogService(CatalogItemRepository items, ProjectRepository projects) {
        this.items = items;
        this.projects = projects;
    }

    /** Plain labels, for the project form. */
    @Transactional(readOnly = true)
    public List<String> labels(String kind) {
        return items.findByKindOrderByPositionAsc(kind).stream()
                .map(CatalogItem::getLabel)
                .toList();
    }

    /** Full rows with usage counts, for the admin panel. */
    @Transactional(readOnly = true)
    public List<CatalogItemDto> list(String kind) {
        Map<String, Integer> usage = usageFor(kind);
        return items.findByKindOrderByPositionAsc(kind).stream()
                .map(i -> new CatalogItemDto(i.getId(), i.getKind(), i.getLabel(),
                        usage.getOrDefault(i.getLabel(), 0)))
                .toList();
    }

    @Transactional
    public CatalogItemDto add(String kind, String rawLabel) {
        String normalisedKind = normaliseKind(kind);
        String label = rawLabel == null ? "" : rawLabel.trim().replaceAll("\\s+", " ");

        if (label.isEmpty()) {
            throw new BadRequestException("Please enter a name.");
        }
        if (label.length() > MAX_LABEL) {
            throw new BadRequestException("That name is too long.");
        }
        if (items.existsByKindAndLabelIgnoreCase(normalisedKind, label)) {
            throw new BadRequestException("\"" + label + "\" is already on the list.");
        }

        int position = (int) items.countByKind(normalisedKind);
        CatalogItem saved = items.save(new CatalogItem(normalisedKind, label, position));
        return new CatalogItemDto(saved.getId(), saved.getKind(), saved.getLabel(), 0);
    }

    /**
     * Removes an option from the pick-list. Projects already using it keep the
     * value they were given — the same rule as removing a committee member.
     */
    @Transactional
    public void remove(String id) {
        CatalogItem item = items.findById(id)
                .orElseThrow(() -> new NotFoundException("No catalog item with id " + id));

        // A form with no options at all would be unusable.
        if (items.countByKind(item.getKind()) <= 1) {
            throw new BadRequestException("At least one "
                    + (CatalogItem.TYPE.equals(item.getKind()) ? "project type" : "category")
                    + " has to remain.");
        }
        items.delete(item);
    }

    private Map<String, Integer> usageFor(String kind) {
        List<Object[]> rows = CatalogItem.TYPE.equals(kind)
                ? projects.countProjectsPerType()
                : projects.countProjectsPerCategory();

        Map<String, Integer> usage = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                usage.put((String) row[0], ((Number) row[1]).intValue());
            }
        }
        return usage;
    }

    private static String normaliseKind(String kind) {
        String k = kind == null ? "" : kind.trim().toUpperCase(Locale.ROOT);
        if (CatalogItem.TYPE.equals(k) || CatalogItem.CATEGORY.equals(k)) {
            return k;
        }
        throw new BadRequestException("Unknown list: " + kind);
    }
}
