package lk.leoclub.clubprojects.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sub_tasks")
public class SubTask {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(length = 500)
    private String title = "";

    private boolean done = false;

    /** Keeps the checklist in the order the user arranged it. */
    private int position = 0;

    public SubTask() {
    }

    public SubTask(String title, boolean done, int position) {
        this.title = title;
        this.done = done;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
