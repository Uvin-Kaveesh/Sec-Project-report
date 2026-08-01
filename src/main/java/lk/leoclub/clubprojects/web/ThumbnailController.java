package lk.leoclub.clubprojects.web;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lk.leoclub.clubprojects.dto.ProjectDto;
import lk.leoclub.clubprojects.model.ProjectThumbnail;
import lk.leoclub.clubprojects.service.ProjectMapper;
import lk.leoclub.clubprojects.service.ThumbnailService;

@RestController
@RequestMapping("/api/projects/{id}/thumbnail")
public class ThumbnailController {

    private final ThumbnailService service;
    private final ProjectMapper mapper;

    public ThumbnailController(ThumbnailService service, ProjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** Returns the updated project so the UI can refresh its cache-busting version. */
    @PostMapping
    public ProjectDto upload(@PathVariable("id") String id, @RequestParam("file") MultipartFile file) {
        return mapper.toDto(service.save(id, file));
    }

    @GetMapping
    public ResponseEntity<byte[]> get(@PathVariable("id") String id) {
        ProjectThumbnail thumb = service.get(id);
        return ResponseEntity.ok()
                // Safe to cache hard: the URL carries a ?v= stamp that changes on replacement.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .contentType(MediaType.parseMediaType(thumb.getContentType()))
                .body(thumb.getData());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") String id) {
        service.delete(id);
    }
}
