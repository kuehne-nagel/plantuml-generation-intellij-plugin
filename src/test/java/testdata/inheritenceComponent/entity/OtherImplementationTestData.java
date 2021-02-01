package testdata.inheritenceComponent.entity;

import java.math.BigDecimal;

public class OtherImplementationTestData implements AbstractionInterfaceTestData{

    public String otherTest;
    public BigDecimal otherData;

    @Override
    public AbstractionTestData getAbstraction() {
        AbstractionTestData data = new AbstractionTestData();
        data.test = otherTest;
        data.data = otherData;

        return data;
    }
}
