package lk.leoclub.clubprojects.dto;

public class SubTaskDto {

    public String id;
    public String title = "";
    public boolean done = false;

    public SubTaskDto() {
    }

    public SubTaskDto(String id, String title, boolean done) {
        this.id = id;
        this.title = title;
        this.done = done;
    }
}
