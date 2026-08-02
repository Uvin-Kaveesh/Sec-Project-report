package lk.leoclub.clubprojects.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    private String name = "";
    private String type = "Club Project";
    private String category = "Community Service";
    private String status = "Not started";

    /** ISO date (yyyy-MM-dd). Stored as text — SQLite has no native date type. */
    @Column(name = "start_date", length = 10)
    private String startDate = "";

    @Column(name = "due_date", length = 10)
    private String dueDate = "";

    private String duration = "";
    private String venue = "";

    /**
     * "Yes", "No", or empty when nobody has answered yet. Kept as text rather
     * than a Boolean so the blank third state needs no special handling, and so
     * the spreadsheet export reads the way the club's forms already do.
     */
    @Column(name = "on_behalf_of_district", length = 3)
    private String onBehalfOfDistrict = "";

    @Column(name = "on_behalf_of_multiple_district", length = 3)
    private String onBehalfOfMultipleDistrict = "";
    private String chair = "";
    private String secretary = "";
    private String treasurer = "";
    private String participation = "";
    private String beneficiaries = "";

    @Column(name = "service_hours")
    private String serviceHours = "";

    @Column(name = "project_value")
    private String projectValue = "";

    private String funds = "";
    private String community = "";

    @Column(length = 2000)
    private String need = "";

    @Column(length = 2000)
    private String opportunity = "";

    private String collection = "";

    @Column(name = "chief_guest")
    private String chiefGuest = "";

    @Column(name = "other_guests")
    private String otherGuests = "";

    @Column(length = 2000)
    private String note = "";

    private int progress = 0;

    /**
     * Thumbnail metadata. The bytes live in {@link ProjectThumbnail}; these two
     * columns are all the home screen needs to know an image exists and to bust
     * the browser cache when it is replaced. Null when there is no photo.
     */
    @Column(name = "thumbnail_type", length = 60)
    private String thumbnailType;

    @Column(name = "thumbnail_updated_at")
    private Long thumbnailUpdatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_assignees", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "member")
    private Set<String> assignees = new LinkedHashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "project_id")
    @OrderBy("position ASC")
    private List<SubTask> tasks = new ArrayList<>();

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getOnBehalfOfDistrict() {
        return onBehalfOfDistrict;
    }

    public void setOnBehalfOfDistrict(String onBehalfOfDistrict) {
        this.onBehalfOfDistrict = onBehalfOfDistrict;
    }

    public String getOnBehalfOfMultipleDistrict() {
        return onBehalfOfMultipleDistrict;
    }

    public void setOnBehalfOfMultipleDistrict(String onBehalfOfMultipleDistrict) {
        this.onBehalfOfMultipleDistrict = onBehalfOfMultipleDistrict;
    }

    public String getChair() {
        return chair;
    }

    public void setChair(String chair) {
        this.chair = chair;
    }

    public String getSecretary() {
        return secretary;
    }

    public void setSecretary(String secretary) {
        this.secretary = secretary;
    }

    public String getTreasurer() {
        return treasurer;
    }

    public void setTreasurer(String treasurer) {
        this.treasurer = treasurer;
    }

    public String getParticipation() {
        return participation;
    }

    public void setParticipation(String participation) {
        this.participation = participation;
    }

    public String getBeneficiaries() {
        return beneficiaries;
    }

    public void setBeneficiaries(String beneficiaries) {
        this.beneficiaries = beneficiaries;
    }

    public String getServiceHours() {
        return serviceHours;
    }

    public void setServiceHours(String serviceHours) {
        this.serviceHours = serviceHours;
    }

    public String getProjectValue() {
        return projectValue;
    }

    public void setProjectValue(String projectValue) {
        this.projectValue = projectValue;
    }

    public String getFunds() {
        return funds;
    }

    public void setFunds(String funds) {
        this.funds = funds;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public String getNeed() {
        return need;
    }

    public void setNeed(String need) {
        this.need = need;
    }

    public String getOpportunity() {
        return opportunity;
    }

    public void setOpportunity(String opportunity) {
        this.opportunity = opportunity;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getChiefGuest() {
        return chiefGuest;
    }

    public void setChiefGuest(String chiefGuest) {
        this.chiefGuest = chiefGuest;
    }

    public String getOtherGuests() {
        return otherGuests;
    }

    public void setOtherGuests(String otherGuests) {
        this.otherGuests = otherGuests;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getThumbnailType() {
        return thumbnailType;
    }

    public void setThumbnailType(String thumbnailType) {
        this.thumbnailType = thumbnailType;
    }

    public Long getThumbnailUpdatedAt() {
        return thumbnailUpdatedAt;
    }

    public void setThumbnailUpdatedAt(Long thumbnailUpdatedAt) {
        this.thumbnailUpdatedAt = thumbnailUpdatedAt;
    }

    public Set<String> getAssignees() {
        return assignees;
    }

    public void setAssignees(Set<String> assignees) {
        this.assignees = assignees;
    }

    public List<SubTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<SubTask> tasks) {
        this.tasks = tasks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
