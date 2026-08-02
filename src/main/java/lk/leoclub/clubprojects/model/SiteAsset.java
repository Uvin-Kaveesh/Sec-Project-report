package lk.leoclub.clubprojects.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A site-wide image, keyed by a fixed name. Only the club logo uses this today,
 * but the shape leaves room for a banner or a favicon later without another
 * table.
 */
@Entity
@Table(name = "site_assets")
public class SiteAsset {

    public static final String LOGO = "logo";

    @Id
    @Column(length = 40)
    private String id;

    @Column(columnDefinition = "blob")
    private byte[] data;

    @Column(name = "content_type", length = 60)
    private String contentType;

    /** Doubles as the cache-busting stamp in the image URL. */
    @Column(name = "updated_at")
    private Long updatedAt;

    public SiteAsset() {
    }

    public SiteAsset(String id, byte[] data, String contentType, Long updatedAt) {
        this.id = id;
        this.data = data;
        this.contentType = contentType;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
