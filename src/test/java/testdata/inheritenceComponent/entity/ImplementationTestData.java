package testdata.inheritenceComponent.entity;

import java.math.BigDecimal;

public class ImplementationTestData extends AbstractInheritanceTestData implements AbstractionInterfaceTestData{

    public BigDecimal someData;

    @Override
    public AbstractionTestData getAbstraction() {
        AbstractionTestData data = new AbstractionTestData();
        data.test = abstractTest;
        data.data = someData;

        return data;
    }
}
