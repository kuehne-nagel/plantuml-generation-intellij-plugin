package testdata.oneComponent.richclient;

import testdata.oneComponent.dto.TestDataDto;

public interface TestFacade {

    TestDataDto load();

    void save(TestDataDto data);

}
