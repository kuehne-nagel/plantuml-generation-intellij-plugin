package examples.structure.model;

import javax.validation.constraints.NotNull;

public abstract class ToDoItem {

    @NotNull
    private String name;
    private long start;
    private int lengthInMinutes;

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public int getLengthInMinutes() {
        return lengthInMinutes;
    }

    public void setLengthInMinutes(int lengthInMinutes) {
        this.lengthInMinutes = lengthInMinutes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void toSomethingNice(){

    }
}
