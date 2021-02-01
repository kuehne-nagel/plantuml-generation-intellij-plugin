package com.kn.diagrams.generator.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.kn.diagrams.generator.config.toJsonWithComments
import com.kn.diagrams.generator.graph.ProjectClassification
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.builder.EqualsBuilder
import org.apache.commons.lang.builder.HashCodeBuilder

@State(name = "DiagramGenerationSettings", storages = [Storage("diagram_generation.cfg")])
class DiagramGenerationSettings : PersistentStateComponent<DiagramGenerationSettings?> {

    var projectClassification = ""
    var callDiagramDefaults = ""
    var structureDiagramDefaults = ""
    var flowDiagramDefaults = ""

    override fun getState(): DiagramGenerationSettings? {
        return this
    }

    override fun loadState(state: DiagramGenerationSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun equals(other: Any?): Boolean {
        return EqualsBuilder.reflectionEquals(other, this)
    }

    override fun hashCode(): Int {
        return HashCodeBuilder.reflectionHashCode(this)
    }

    companion object {
        @JvmStatic
        val instance: DiagramGenerationSettings
            get() {
                val settings = ServiceManager.getService(DiagramGenerationSettings::class.java)

                if (StringUtils.isBlank(settings.projectClassification)) {
                    settings.projectClassification = toJsonWithComments(ProjectClassification())
                }
                if (StringUtils.isBlank(settings.callDiagramDefaults)) {
                    settings.callDiagramDefaults = toJsonWithComments(CallConfigurationDefaults().defaulted())
                }
                if (StringUtils.isBlank(settings.structureDiagramDefaults)) {
                    settings.structureDiagramDefaults = toJsonWithComments(StructureConfigurationDefaults().defaulted())
                }
                if (StringUtils.isBlank(settings.flowDiagramDefaults)) {
                    settings.flowDiagramDefaults = toJsonWithComments(FlowConfigurationDefaults().defaulted())
                }

                return settings
            }
    }
}
