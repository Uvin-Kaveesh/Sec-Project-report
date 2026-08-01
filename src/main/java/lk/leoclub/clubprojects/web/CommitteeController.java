package lk.leoclub.clubprojects.web;

import java.util.List;
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

import lk.leoclub.clubprojects.dto.CommitteeMemberDto;
import lk.leoclub.clubprojects.service.CommitteeService;

@RestController
@RequestMapping("/api/committee")
public class CommitteeController {

    private final CommitteeService service;

    public CommitteeController(CommitteeService service) {
        this.service = service;
    }

    @GetMapping
    public List<CommitteeMemberDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommitteeMemberDto add(@RequestBody Map<String, String> body) {
        return service.add(body == null ? null : body.get("name"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("id") String id) {
        service.remove(id);
    }
}
