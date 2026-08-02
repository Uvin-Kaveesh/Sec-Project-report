package lk.leoclub.clubprojects.web;

import java.time.Duration;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lk.leoclub.clubprojects.model.SiteAsset;
import lk.leoclub.clubprojects.service.SiteService;

@RestController
@RequestMapping("/api/site")
public class SiteController {

    private final SiteService service;

    public SiteController(SiteService service) {
        this.service = service;
    }

    /** Whether a logo exists, and its version stamp. Open to everyone. */
    @GetMapping
    public Map<String, Object> info() {
        return service.info();
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        SiteAsset asset = service.logo();
        return ResponseEntity.ok()
                // The URL carries a ?v= stamp, so this is safe to cache hard.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .body(asset.getData());
    }

    @PostMapping("/logo")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        return service.saveLogo(file);
    }

    @DeleteMapping("/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove() {
        service.deleteLogo();
    }
}
