package testdata.oneComponent.dataaccess;

import testdata.oneComponent.entity.TestData;

public interface TestDataDao {


    TestData load();

    TestData save(TestData data);

}
