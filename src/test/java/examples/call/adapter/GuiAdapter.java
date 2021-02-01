package examples.call.adapter;

import examples.call.domain.Service;

public class GuiAdapter {

    private Service service;

    public void saveButton(){
        service.save();
    }

}
