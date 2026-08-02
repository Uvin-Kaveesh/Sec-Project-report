package lk.leoclub.clubprojects.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lk.leoclub.clubprojects.dto.CatalogItemDto;
import lk.leoclub.clubprojects.model.CatalogItem;
import lk.leoclub.clubprojects.service.CatalogService;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    /** Both lists with usage counts, for the admin panel. */
    @GetMapping
    public Map<String, Object> all() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("types", service.list(CatalogItem.TYPE));
        out.put("categories", service.list(CatalogItem.CATEGORY));
        return out;
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemDto addType(@RequestBody Map<String, String> body) {
        return service.add(CatalogItem.TYPE, body == null ? null : body.get("label"));
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemDto addCategory(@RequestBody Map<String, String> body) {
        return service.add(CatalogItem.CATEGORY, body == null ? null : body.get("label"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("id") String id) {
        service.remove(id);
    }
}
