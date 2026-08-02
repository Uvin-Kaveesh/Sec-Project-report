package lk.leoclub.clubprojects.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lk.leoclub.clubprojects.model.CatalogItem;
import lk.leoclub.clubprojects.service.CatalogService;
import lk.leoclub.clubprojects.service.CommitteeService;
import lk.leoclub.clubprojects.service.ProjectCompletion;
import lk.leoclub.clubprojects.service.ProjectStatus;

@RestController
@RequestMapping("/api")
public class MetaController {

    private final CommitteeService committee;
    private final CatalogService catalog;

    public MetaController(CommitteeService committee, CatalogService catalog) {
        this.committee = committee;
        this.catalog = catalog;
    }

    /** Everything the form needs to build its pick-lists in one round trip. */
    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("types", catalog.labels(CatalogItem.TYPE));
        m.put("categories", catalog.labels(CatalogItem.CATEGORY));
        m.put("statuses", ProjectStatus.ALL);
        m.put("committee", committee.list());
        // The fields the progress bar measures — the UI applies the same rule live.
        m.put("progressFields", ProjectCompletion.fieldNames());
        return m;
    }
}
