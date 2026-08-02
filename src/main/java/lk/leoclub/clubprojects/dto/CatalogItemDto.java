package lk.leoclub.clubprojects.dto;

public class CatalogItemDto {

    public String id;
    public String kind;
    public String label;

    /** Derived, read-only: how many projects currently use this option. */
    public int usageCount;

    public CatalogItemDto() {
    }

    public CatalogItemDto(String id, String kind, String label, int usageCount) {
        this.id = id;
        this.kind = kind;
        this.label = label;
        this.usageCount = usageCount;
    }
}
