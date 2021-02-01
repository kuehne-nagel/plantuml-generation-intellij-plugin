package testdata.oneComponent.richclient.impl;

import javax.validation.constraints.NotNull;

import testdata.oneComponent.dto.TestDataDto;
import testdata.oneComponent.richclient.TestFacade;
import testdata.oneComponent.service.TestDataService;

public class TestFacadeImpl implements TestFacade {

    @NotNull
    public TestDataService service;
    public TestDataDtoMapper mapper;

    @Override
    public TestDataDto load() {
        return mapper.mapToTestDataDto(service.load());
    }

    @Override
    public void save(TestDataDto data) {
        service.save(mapper.mapToTestDataDs(data));
    }
}
