package testdata.oneComponent.domain;

import testdata.oneComponent.entity.TestData;

public interface TestManager {

    TestData load();

    TestData save(TestData data);

}
