package testdata.oneComponent.richclient.impl;

import testdata.oneComponent.dto.TestDataDto;
import testdata.oneComponent.entity.ds.TestDataDs;

public class TestDataDtoMapper {
    public TestDataDs mapToTestDataDs(TestDataDto data){
        return new TestDataDs();
    }

    public TestDataDto mapToTestDataDto(TestDataDs data){
        return new TestDataDto();
    }
}
