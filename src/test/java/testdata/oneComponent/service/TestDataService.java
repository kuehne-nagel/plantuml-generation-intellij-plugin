package testdata.oneComponent.service;

import testdata.oneComponent.entity.ds.TestDataDs;

public interface TestDataService {

    TestDataDs load();

    TestDataDs save(TestDataDs data);

}
