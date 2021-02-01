package examples.dataflow.domain;

public class Service {

    public Entity loadById(String id){
        Entity entity = new Entity();
        entity.setId(id);
        entity.setName(id);

        return entity;
    }

}
