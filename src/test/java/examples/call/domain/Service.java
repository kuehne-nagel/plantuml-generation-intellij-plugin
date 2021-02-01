package examples.call.domain;

public class Service {
    private Repository repository;

    public void save(){
        repository.save();
        notifyChanges();
    }

    private void notifyChanges(){

    }
}
