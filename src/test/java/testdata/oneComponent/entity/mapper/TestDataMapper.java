package testdata.oneComponent.entity.mapper;

import testdata.oneComponent.entity.TestData;
import testdata.oneComponent.entity.ds.TestDataDs;

public class TestDataMapper {

    public TestDataDs mapToTestDataDs(TestData data){
        return new TestDataDs();
    }

    public TestData mapToTestData(TestDataDs data){
        return new TestData();
    }

}
