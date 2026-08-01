package lk.leoclub.clubprojects.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lk.leoclub.clubprojects.dto.ProjectDto;
import lk.leoclub.clubprojects.dto.StatsDto;
import lk.leoclub.clubprojects.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    // Parameter names are given explicitly rather than relying on the compiler
    // keeping them, so these endpoints bind correctly however the code is built.
    @GetMapping
    public List<ProjectDto> list(@RequestParam(name = "q", required = false) String q,
                                 @RequestParam(name = "status", required = false) String status) {
        return service.list(q, status);
    }

    @GetMapping("/stats")
    public StatsDto stats() {
        return service.stats();
    }

    @GetMapping("/export.tsv")
    public ResponseEntity<String> export() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"club-projects.tsv\"")
                .body(service.exportTsv());
    }

    @GetMapping("/{id}")
    public ProjectDto get(@PathVariable("id") String id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectDto create(@RequestBody(required = false) ProjectDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable("id") String id, @RequestBody ProjectDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") String id) {
        service.delete(id);
    }
}
