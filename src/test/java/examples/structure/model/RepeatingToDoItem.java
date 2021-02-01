package examples.structure.model;

import javax.validation.constraints.NotNull;

public class RepeatingToDoItem extends ToDoItem {

    @NotNull
    private Repeat repeatEvery;
    private int repeatUnit;

    public Repeat getRepeatEvery() {
        return repeatEvery;
    }

    public void setRepeatEvery(Repeat repeatEvery) {
        this.repeatEvery = repeatEvery;
    }

    public int getRepeatUnit() {
        return repeatUnit;
    }

    public void setRepeatUnit(int repeatUnit) {
        this.repeatUnit = repeatUnit;
    }
}
