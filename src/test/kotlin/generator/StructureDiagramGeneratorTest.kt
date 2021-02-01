package generator

import org.junit.Test
import testdata.inheritenceComponent.entity.AbstractInheritanceTestData
import testdata.inheritenceComponent.entity.AbstractionInterfaceTestData
import testdata.inheritenceComponent.entity.ImplementationTestData
import testdata.inheritenceComponent.entity.OtherImplementationTestData
import testdata.oneComponent.dataaccess.impl.JpaTestDataDao
import testdata.oneComponent.domain.TestManager
import testdata.oneComponent.domain.impl.TestManagerImpl
import testdata.oneComponent.entity.SubTestData
import testdata.oneComponent.entity.TestData
import testdata.oneComponent.entity.ds.TestDataDs
import testdata.oneComponent.entity.mapper.TestDataMapper
import testdata.oneComponent.richclient.impl.TestDataDtoMapper
import testdata.oneComponent.richclient.impl.TestFacadeImpl
import testdata.oneComponent.service.TestDataService
import testdata.oneComponent.service.impl.TestServiceImpl
import testdata.someComponent.someFocusArea.entity.CardinalityTestData
import testdata.someComponent.someFocusArea.entity.ComponentTestData

// TODO more tests needed for show/hide methods/fields
class StructureDiagramGeneratorTest : AbstractStructureDiagramGeneratorTest() {

    @Test
    fun testDefaultSettings() {
        diagram = classDiagram(TestData::class)

        assertClassField(TestData::subObject)
        assertClassField(TestData::text)

        assertClassField(SubTestData::subText)
        assertClassField(SubTestData::subBackReference)
        assertClassField(SubTestData::subComponentData)

        assertClassField(ComponentTestData::componentText)

        assertFieldEdge(TestData::subObject, SubTestData::class)
        assertNode("String", false)

        assertFieldEdge(SubTestData::subBackReference, TestData::class)
        assertFieldEdge(SubTestData::subComponentData, ComponentTestData::class)
    }


    @Test
    fun testZeroDepth() {
        diagram = classDiagram(TestData::class) {
            graphTraversal.forwardDepth = 0
            graphTraversal.backwardDepth = 0
        }

        assertClass(TestData::class) // root node is always displayed
        assertNoClass(SubTestData::class)
        assertNoClass(ComponentTestData::class)

    }

    @Test
    fun testOneDepthForward() {
        diagram = classDiagram(TestData::class) {
            graphTraversal.backwardDepth = 0 // subBackReference is not found
            graphTraversal.forwardDepth = 1
        }

        assertClassField(TestData::subObject)
        assertClassField(TestData::text)
        assertClassField(SubTestData::subText)
        assertClassField(SubTestData::subBackReference)
        assertClassField(SubTestData::subComponentData)
        assertNoClass(ComponentTestData::class)

        assertFieldEdge(TestData::subObject, SubTestData::class)
        assertNode("String", false)

        assertNoFieldEdge(SubTestData::subBackReference, TestData::class)
        assertNoFieldEdge(SubTestData::subComponentData, ComponentTestData::class)
    }

    @Test
    fun testOneDepthBackward() {
        diagram = classDiagram(TestData::class) {
            graphTraversal.backwardDepth = 1
            graphTraversal.forwardDepth = 0
        }

        assertClassField(TestData::subObject)
        assertClassField(TestData::text)
        assertClassField(SubTestData::subText)
        assertClassField(SubTestData::subBackReference)
        assertClassField(SubTestData::subComponentData)
        assertNoClass(ComponentTestData::class)

        assertNoFieldEdge(TestData::subObject, SubTestData::class)
        assertNode("String", false)

        assertFieldEdge(SubTestData::subBackReference, TestData::class)
        assertNoFieldEdge(SubTestData::subComponentData, ComponentTestData::class)
    }

    @Test
    fun testInterfaceMethods() {
        diagram = classDiagram(AbstractionInterfaceTestData::class) {
            graphTraversal.forwardDepth = 0
        }

        assertClassMethod(AbstractionInterfaceTestData::getAbstraction)

    }

    @Test
    fun testFullInheritanceStructure() {
        diagram = classDiagram(AbstractInheritanceTestData::class) {
            details.showMethodParameterTypes = true
            details.showMethodParameterNames = true
            details.showMethodReturnType = true
        }

        assertClassField(AbstractInheritanceTestData::abstractTest)
        assertClassMethod(AbstractionInterfaceTestData::getAbstraction)
        assertClassField(ImplementationTestData::someData)
        assertClassField(OtherImplementationTestData::otherData)
        assertClassField(OtherImplementationTestData::otherTest)

    }

    @Test
    fun testCardinality() {
        diagram = classDiagram(TestFacadeImpl::class)

        assertClassField(TestServiceImpl::manager, cardinalityMandatory()) // @Autowired
        assertClassField(TestServiceImpl::mapper, cardinalityOptional()) // @Autowired with required = false
        assertClassField(TestManagerImpl::dao, cardinalityOptional()) // no @Autowired
        assertClassField(TestFacadeImpl::service, cardinalityMandatory()) // @NotNull
        assertClassField(TestFacadeImpl::mapper, cardinalityOptional()) // no @NotNull

    }

    @Test
    fun testCardinalityCollections() {
        diagram = classDiagram(CardinalityTestData::class)

        assertClassField(CardinalityTestData::array, cardinalityUnbounded())
        assertClassField(CardinalityTestData::collection, cardinalityUnbounded())
        assertClassField(CardinalityTestData::list1, cardinalityUnbounded())
        assertClassField(CardinalityTestData::list2, cardinalityUnbounded())
        assertClassField(CardinalityTestData::list3, cardinalityUnbounded())
        assertClassField(CardinalityTestData::list4, cardinalityUnbounded())
        assertClassField(CardinalityTestData::set1, cardinalityUnbounded())
        assertClassField(CardinalityTestData::set2, cardinalityUnbounded())
        assertClassField(CardinalityTestData::map1, cardinalityUnbounded())
        assertClassField(CardinalityTestData::map2, cardinalityUnbounded())

    }

    @Test
    fun testClassFilter() {
        diagram = classDiagram(TestFacadeImpl::class) {
            graphRestriction.classNameExcludeFilter = sequenceOf(
                    "NonExistingClass",
                    JpaTestDataDao::class.simpleName,
                    "Test", // no full match -> no effect
                    "*Mapper" // excludes both mapper
            ).joinToString(";")
        }

        assertClassField(TestFacadeImpl::mapper)
        assertClassField(TestServiceImpl::manager)
        assertClassField(TestManagerImpl::dao)

        assertNoClass(JpaTestDataDao::class)
        assertNoClass(TestDataMapper::class)
        assertNoClass(TestDataDtoMapper::class)
    }

    @Test
    fun testMethodFilter() {
        diagram = classDiagram(TestFacadeImpl::class) {
            graphRestriction.methodNameExcludeFilter = sequenceOf(
                    "nonExistingMethod",
                    "mapTo", // no full match -> no effect
                    "save",
                    "*DataDs" // one method in each of both mappers
            ).joinToString(";")
            details.showMethodParameterTypes = true
            details.showMethodParameterNames = true
            details.showMethodReturnType = true
        }

        assertClassMethod(TestFacadeImpl::load)
        assertClassMethod(TestServiceImpl::load)
        assertClassMethod(TestManagerImpl::load)
        assertClassMethod(TestDataMapper::mapToTestData)
        assertClassMethod(TestDataDtoMapper::mapToTestDataDto)

        assertNoClassMethod(TestFacadeImpl::save)
        assertNoClassMethod(TestServiceImpl::save)
        assertNoClassMethod(TestManagerImpl::save)
        assertNoClassMethod(JpaTestDataDao::save)

        assertNoClassMethod(TestDataMapper::mapToTestDataDs)
        assertNoClassMethod(TestDataDtoMapper::mapToTestDataDs)
    }

    @Test
    fun testMethodParameterAndReturnTypes() {
        diagram = classDiagram(TestServiceImpl::class) {
            details.showMethodParameterTypes = true
            details.showMethodParameterNames = true
            details.showMethodReturnType = true
        }

        assertClassMethod(TestDataService::load, returnType(TestDataDs::class), noParameters())
        assertClassMethod(TestDataService::save, returnType(TestDataDs::class), parameters(TestDataDs::class))

        assertClassField(TestServiceImpl::manager, returnType(TestManager::class))
        assertClassField(TestServiceImpl::mapper, returnType(TestDataMapper::class))

    }


    @Test
    fun testNoTypesOnMethodsShown() {
        diagram = classDiagram(TestServiceImpl::class) {
            details.showMethodParameterTypes = true
            details.showMethodParameterNames = true
            details.showMethodReturnType = false
        }

        assertClassMethod(TestDataService::load, noReturnType(), noParameters())
        assertClassMethod(TestDataService::save, noReturnType(), noParameters())

        assertClassField(TestServiceImpl::manager, returnType(TestManager::class))
        assertClassField(TestServiceImpl::mapper, returnType(TestDataMapper::class))

    }

}

