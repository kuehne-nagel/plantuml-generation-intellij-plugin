package examples.flow;

import kotlin.random.Random;

public class FlowService {

    @FlowDiagramTerminal("save entity")
    public void save(){
        validateEntity();

        if(isAlreadySaved()){
            update();
        } else{
            create();
        }
    }

    // alternative branch is used when this method is left
    @FlowDiagramCondition(value = "is entity valid", branch = "No", alternativeBranch = "Yes")
    private void validateEntity(){
        if(isNotValid()){
            createViolationException();
        }
    }

    @FlowDiagramCondition(value = "is already saved", branch = "Yes")
    @FlowDiagramTerminal("create entity")
    private void create(){

    }

    @FlowDiagramCondition(value = "is already saved", branch = "no")
    @FlowDiagramTerminal("update entity")
    private void update(){

    }

    @FlowDiagramTerminal("display error message")
    private void createViolationException(){
        throw new RuntimeException();
    }

    private boolean isNotValid(){
        return Random.Default.nextBoolean();
    }

    private boolean isAlreadySaved(){
        return Random.Default.nextBoolean();
    }


}
