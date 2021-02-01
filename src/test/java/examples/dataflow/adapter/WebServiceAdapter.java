package examples.dataflow.adapter;

import examples.dataflow.domain.Entity;
import examples.dataflow.domain.Service;

public class WebServiceAdapter {

    private Service service;

    public Response restMethod(Request request){
        Entity entity = service.loadById(request.getId());
        Response response = new Response();

        entity.setId(entity.getId());
        entity.setName(entity.getName());

        return response;
    }

}
