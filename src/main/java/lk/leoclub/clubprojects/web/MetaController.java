package lk.leoclub.clubprojects.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lk.leoclub.clubprojects.service.Catalog;
import lk.leoclub.clubprojects.service.CommitteeService;
import lk.leoclub.clubprojects.service.ProjectCompletion;
import lk.leoclub.clubprojects.service.ProjectStatus;

@RestController
@RequestMapping("/api")
public class MetaController {

    private final CommitteeService committee;

    public MetaController(CommitteeService committee) {
        this.committee = committee;
    }

    /** Everything the form needs to build its pick-lists in one round trip. */
    @GetMapping("/meta")
    public Map<String, Object> meta() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("types", Catalog.TYPES);
        m.put("categories", Catalog.CATEGORIES);
        m.put("statuses", ProjectStatus.ALL);
        m.put("committee", committee.list());
        // The fields the progress bar measures — the UI applies the same rule live.
        m.put("progressFields", ProjectCompletion.fieldNames());
        return m;
    }
}
