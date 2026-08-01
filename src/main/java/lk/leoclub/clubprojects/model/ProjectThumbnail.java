package lk.leoclub.clubprojects.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The raw bytes of a project's thumbnail, keyed by the project id.
 *
 * <p>Kept in its own table on purpose: listing projects for the home screen
 * would otherwise pull every image out of the database along with it. The
 * project row carries only the small metadata needed to build the image URL.
 */
@Entity
@Table(name = "project_thumbnails")
public class ProjectThumbnail {

    @Id
    @Column(name = "project_id", length = 36)
    private String projectId;

    /** Explicit blob rather than @Lob — SQLite is happier with it. */
    @Column(columnDefinition = "blob")
    private byte[] data;

    @Column(name = "content_type", length = 60)
    private String contentType;

    public ProjectThumbnail() {
    }

    public ProjectThumbnail(String projectId, byte[] data, String contentType) {
        this.projectId = projectId;
        this.data = data;
        this.contentType = contentType;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
