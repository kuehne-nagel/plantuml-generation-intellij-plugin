package testdata.oneComponent.service.impl;

import org.junit.Ignore;
import org.junit.Test;

@Ignore // only for diagram dependencies
public class TestServiceImplITest {

    private TestServiceImpl test;

    @Test
    public void load() {
        test.load();
    }

    @Test
    public void save() {
        test.save(null);
    }
}