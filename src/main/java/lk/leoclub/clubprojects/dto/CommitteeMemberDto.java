package lk.leoclub.clubprojects.dto;

public class CommitteeMemberDto {

    public String id;
    public String name;
    public String role = "";

    /** Derived, read-only: how many projects this member is assigned to. */
    public int projectCount;

    public CommitteeMemberDto() {
    }

    public CommitteeMemberDto(String id, String name, String role, int projectCount) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.projectCount = projectCount;
    }
}
