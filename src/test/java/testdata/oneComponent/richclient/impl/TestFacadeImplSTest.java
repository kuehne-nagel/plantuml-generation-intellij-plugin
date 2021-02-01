package testdata.oneComponent.richclient.impl;

import org.junit.Ignore;
import org.junit.Test;

@Ignore // only for diagram dependencies
public class TestFacadeImplSTest {

    private TestFacadeImpl test;

    @Test
    public void load() {
        test.load();
    }

    @Test
    public void save() {
        test.save(null);
    }
}