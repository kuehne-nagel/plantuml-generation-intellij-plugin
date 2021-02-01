package testdata.oneComponent.domain.impl;

import testdata.oneComponent.dataaccess.TestDataDao;
import testdata.oneComponent.domain.TestManager;
import testdata.oneComponent.entity.TestData;

public class TestManagerImpl implements TestManager {

    public TestDataDao dao;

    @Override
    public TestData load() {
        return dao.load();
    }

    @Override
    public TestData save(TestData data) {
        return dao.save(data);
    }
}
