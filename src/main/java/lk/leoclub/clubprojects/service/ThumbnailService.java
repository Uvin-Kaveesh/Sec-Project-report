package lk.leoclub.clubprojects.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lk.leoclub.clubprojects.model.Project;
import lk.leoclub.clubprojects.model.ProjectThumbnail;
import lk.leoclub.clubprojects.repository.ProjectRepository;
import lk.leoclub.clubprojects.repository.ProjectThumbnailRepository;
import lk.leoclub.clubprojects.web.BadRequestException;
import lk.leoclub.clubprojects.web.NotFoundException;

@Service
public class ThumbnailService {

    /** The browser shrinks images before upload; this is the safety net. */
    static final long MAX_BYTES = 2L * 1024 * 1024;

    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ProjectRepository projects;
    private final ProjectThumbnailRepository thumbnails;

    public ThumbnailService(ProjectRepository projects, ProjectThumbnailRepository thumbnails) {
        this.projects = projects;
        this.thumbnails = thumbnails;
    }

    @Transactional
    public Project save(String projectId, MultipartFile file) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException("No project with id " + projectId));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose an image to upload.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("That image is larger than 2 MB. Try a smaller one.");
        }

        String type = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BadRequestException("Only JPEG, PNG, WebP or GIF images can be used.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("That image could not be read.");
        }
        if (bytes.length == 0) {
            throw new BadRequestException("That image appears to be empty.");
        }

        thumbnails.save(new ProjectThumbnail(projectId, bytes, type));

        project.setThumbnailType(type);
        project.setThumbnailUpdatedAt(System.currentTimeMillis());
        return projects.save(project);
    }

    @Transactional(readOnly = true)
    public ProjectThumbnail get(String projectId) {
        return thumbnails.findById(projectId)
                .orElseThrow(() -> new NotFoundException("No thumbnail for project " + projectId));
    }

    @Transactional
    public void delete(String projectId) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new NotFoundException("No project with id " + projectId));

        thumbnails.deleteById(projectId);
        project.setThumbnailType(null);
        project.setThumbnailUpdatedAt(null);
        projects.save(project);
    }

    /** Used when a project is deleted, so no orphan image is left behind. */
    @Transactional
    public void deleteIfPresent(String projectId) {
        Optional<ProjectThumbnail> existing = thumbnails.findById(projectId);
        existing.ifPresent(thumbnails::delete);
    }
}
