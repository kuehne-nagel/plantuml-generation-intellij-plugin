package com.kn.diagrams.generator.settings;


import com.intellij.openapi.options.Configurable;
import com.kn.diagrams.generator.graph.ProjectClassification;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.kn.diagrams.generator.config.SerializationKt.getSerializer;
import static com.kn.diagrams.generator.config.SerializationKt.toJsonWithComments;

public class DiagramGeneratorSettingsPage implements Configurable {

    private JPanel panel;
    private JTextArea projectClassification;
    private JTextArea callDiagramDefaults;
    private JTextArea structureDiagramDefaults;
    private JTextArea flowDiagramDefaults;

    private JButton resetStructureDiagramDefaults;
    private JButton resetFlowDiagramDefaults;
    private JButton resetCallDiagramDefaults;
    private JButton resetClassification;

    public DiagramGeneratorSettingsPage() {
        resetClassification.addActionListener(e -> projectClassification.setText(toJsonWithComments(new ProjectClassification())));
        resetCallDiagramDefaults.addActionListener(e -> callDiagramDefaults.setText(toJsonWithComments(new CallConfigurationDefaults().defaulted())));
        resetStructureDiagramDefaults.addActionListener(e -> structureDiagramDefaults.setText(toJsonWithComments(new StructureConfigurationDefaults().defaulted())));
        resetFlowDiagramDefaults.addActionListener(e -> flowDiagramDefaults.setText(toJsonWithComments(new FlowConfigurationDefaults().defaulted())));
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Diagram Generation";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return panel;
    }

    @Override
    public boolean isModified() {
        return isModified(DiagramGenerationSettings.getInstance());
    }

    @Override
    public void apply() {
        DiagramGenerationSettings instance = DiagramGenerationSettings.getInstance();
        getData(instance);
    }

    @Override
    public void reset() {
        DiagramGenerationSettings newInstance = DiagramGenerationSettings.getInstance();
        setData(newInstance);
    }

    @Override
    public void disposeUIResources() {
    }

    public void setData(DiagramGenerationSettings data) {
        projectClassification.setText(data.getProjectClassification());
        callDiagramDefaults.setText(data.getCallDiagramDefaults());
        structureDiagramDefaults.setText(data.getStructureDiagramDefaults());
        flowDiagramDefaults.setText(data.getFlowDiagramDefaults());
    }

    public void getData(DiagramGenerationSettings data) {
        data.setProjectClassification(roundTripSerialization(projectClassification.getText(), ProjectClassification.class));
        data.setCallDiagramDefaults(roundTripSerialization(callDiagramDefaults.getText(), CallConfigurationDefaults.class));
        data.setStructureDiagramDefaults(roundTripSerialization(structureDiagramDefaults.getText(), StructureConfigurationDefaults.class));
        data.setFlowDiagramDefaults(roundTripSerialization(flowDiagramDefaults.getText(), FlowConfigurationDefaults.class));
    }

    private String roundTripSerialization(String text, Class<?> clazz){
        return toJsonWithComments(getSerializer().fromJson(text, clazz));
    }

    public boolean isModified(DiagramGenerationSettings data) {
        return !projectClassification.getText().equals(data.getProjectClassification())
                || !callDiagramDefaults.getText().equals(data.getProjectClassification())
                || !structureDiagramDefaults.getText().equals(data.getProjectClassification())
                || !flowDiagramDefaults.getText().equals(data.getProjectClassification());
    }
}
