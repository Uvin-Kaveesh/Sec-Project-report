package lk.leoclub.clubprojects.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * One entry in a pick-list on the project form — either a project type or a
 * project category. Both live in the same table, told apart by {@link #kind},
 * because they behave identically and there is no reason to write it twice.
 */
@Entity
@Table(name = "catalog_items",
       uniqueConstraints = @UniqueConstraint(columnNames = { "kind", "label" }))
public class CatalogItem {

    public static final String TYPE = "TYPE";
    public static final String CATEGORY = "CATEGORY";

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(nullable = false, length = 80)
    private String label;

    /** Controls the order the options appear in on the form. */
    private int position;

    public CatalogItem() {
    }

    public CatalogItem(String kind, String label, int position) {
        this.kind = kind;
        this.label = label;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
