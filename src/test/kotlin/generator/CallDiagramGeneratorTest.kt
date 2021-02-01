package generator

import org.junit.Test
import testdata.oneComponent.dataaccess.impl.JpaTestDataDao
import testdata.oneComponent.domain.impl.TestManagerImpl
import testdata.oneComponent.entity.TestData
import testdata.oneComponent.entity.ds.TestDataDs
import testdata.oneComponent.entity.mapper.TestDataMapper
import testdata.oneComponent.richclient.impl.TestDataDtoMapper
import testdata.oneComponent.richclient.impl.TestFacadeImpl
import testdata.oneComponent.service.impl.TestServiceImpl

class CallDiagramGeneratorTest : AbstractCallDiagramGeneratorTest() {

    @Test
    fun testStandardConfig() {
        diagram = callDiagram(TestFacadeImpl::load)

        assertCallEdge(TestFacadeImpl::load, TestServiceImpl::load)
        assertNoCallEdge(TestFacadeImpl::load, TestDataDtoMapper::mapToTestDataDs)
        assertCallEdge(TestServiceImpl::load, TestManagerImpl::load)
        assertNoCallEdge(TestServiceImpl::load, TestDataMapper::mapToTestDataDs)
        assertNoCallEdge(TestManagerImpl::load, JpaTestDataDao::load)
    }

    @Test
    fun testNotHideDataAccess() {
        diagram = callDiagram(TestFacadeImpl::load) {
            graphRestriction.cutDataAccess = false
        }

        assertCallEdge(TestFacadeImpl::load, TestServiceImpl::load)
        assertNoCallEdge(TestFacadeImpl::load, TestDataDtoMapper::mapToTestDataDs)
        assertCallEdge(TestServiceImpl::load, TestManagerImpl::load)
        assertNoCallEdge(TestServiceImpl::load, TestDataMapper::mapToTestDataDs)
        assertCallEdge(TestManagerImpl::load, JpaTestDataDao::load)
    }

    @Test
    fun testForwardDepth() {
        diagram = callDiagram(TestServiceImpl::load) {
            graphRestriction.cutMappings = false
            graphRestriction.cutDataAccess = false

            graphTraversal.forwardDepth = 1
            graphTraversal.hideMappings = false
        }

        assertCallEdge(TestFacadeImpl::load, TestServiceImpl::load)
        assertNoCallEdge(TestFacadeImpl::load, TestDataDtoMapper::mapToTestDataDto) // is a separate branch
        assertCallEdge(TestServiceImpl::load, TestManagerImpl::load)
        assertCallEdge(TestServiceImpl::load, TestDataMapper::mapToTestDataDs)
        assertNoCallEdge(TestManagerImpl::load, JpaTestDataDao::load) // depth 2
    }

    @Test
    fun testBackwardDepth() {
        diagram = callDiagram(TestManagerImpl::load) {
            graphRestriction.cutDataAccess = false

            graphTraversal.backwardDepth = 1
            graphTraversal.hideMappings = false
        }

        assertNoCallEdge(TestFacadeImpl::load, TestServiceImpl::load) // depth 2
        assertNoCallEdge(TestFacadeImpl::load, TestDataDtoMapper::mapToTestDataDto) // is a separate branch
        assertCallEdge(TestServiceImpl::load, TestManagerImpl::load)
        assertNoCallEdge(TestServiceImpl::load, TestDataMapper::mapToTestDataDs)  // is a separate branch
        assertCallEdge(TestManagerImpl::load, JpaTestDataDao::load)
    }


    @Test
    fun testMethodFilter() {
        diagram = callDiagram(TestFacadeImpl::save) {
            graphRestriction.methodNameExcludeFilter = sequenceOf(
                    "nonExistingMethod",
                    "mapTo", // no full match -> no effect
                    "mapToTestDataDto",
                    "*DataDs" // one method in each of both mappers
            ).joinToString(";")
            graphRestriction.cutDataAccess = false

            graphTraversal.forwardDepth = 9
            graphTraversal.hideMappings = false
        }

        assertCallEdge(TestFacadeImpl::save, TestServiceImpl::save)

        assertCallEdge(TestServiceImpl::save, TestManagerImpl::save)
        assertCallEdge(TestServiceImpl::save, TestDataMapper::mapToTestData)

        assertCallEdge(TestManagerImpl::save, JpaTestDataDao::save)

        assertNoCallEdge(TestFacadeImpl::save, TestDataDtoMapper::mapToTestDataDto)
        assertNoCallEdge(TestFacadeImpl::save, TestDataDtoMapper::mapToTestDataDs)
        assertNoCallEdge(TestServiceImpl::save, TestDataMapper::mapToTestDataDs)
    }

    @Test
    fun testClassFilter() {
        diagram = callDiagram(TestFacadeImpl::save) {
            graphRestriction.classNameExcludeFilter = sequenceOf(
                    "NonExistingClass",
                    "Test", // no full match -> no effect
                    JpaTestDataDao::class.simpleName,
                    "*Mapper" // one method in each of both mappers
            ).joinToString(";")
        }

        assertCallEdge(TestFacadeImpl::save, TestServiceImpl::save)
        assertCallEdge(TestServiceImpl::save, TestManagerImpl::save)

        assertNoCallEdge(TestManagerImpl::save, JpaTestDataDao::save)
        assertNoCallEdge(TestFacadeImpl::save, TestDataDtoMapper::mapToTestDataDto)
        assertNoCallEdge(TestFacadeImpl::save, TestDataDtoMapper::mapToTestDataDs)
        assertNoCallEdge(TestServiceImpl::save, TestDataMapper::mapToTestData)
        assertNoCallEdge(TestServiceImpl::save, TestDataMapper::mapToTestDataDs)
    }


    @Test
    fun testMethodParameterAndReturnTypes() {
        diagram = callDiagram(TestServiceImpl::load) {
            graphRestriction.cutMappings = false
            graphTraversal.hideMappings = false

            details.showMethodParametersTypes = true
            details.showMethodReturnType = true
        }

        assertNoCallEdge(TestFacadeImpl::load, TestDataDtoMapper::mapToTestDataDto)
        assertCallEdge(TestFacadeImpl::load, TestServiceImpl::load)
        assertCallEdge(TestServiceImpl::load, TestManagerImpl::load)
        assertCallEdge(TestServiceImpl::load, TestDataMapper::mapToTestDataDs)

        assertMethodNode(TestServiceImpl::load, returnType(TestDataDs::class))
        assertMethodNode(TestDataMapper::mapToTestDataDs, parameters(TestData::class), returnType(TestDataDs::class))
    }
}
