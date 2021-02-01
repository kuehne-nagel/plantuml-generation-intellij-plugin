package com.kn.diagrams.generator.sidebar;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.kn.diagrams.generator.actions.AbstractDiagramAction;
import com.kn.diagrams.generator.actions.AbstractDiagramActionKt;
import com.kn.diagrams.generator.actions.DiagramActions;
import com.kn.diagrams.generator.config.*;
import com.kn.diagrams.generator.generator.Aggregation;
import com.kn.diagrams.generator.graph.EdgeMode;
import com.kn.diagrams.generator.graph.GraphRestriction;
import com.kn.diagrams.generator.graph.GraphTraversal;
import com.kn.diagrams.generator.settings.CallConfigurationDefaults;
import com.kn.diagrams.generator.settings.ConfigurationDefaults;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

public class DiagramGeneratorConfigurationToolWindow extends JPanel{

    private Project project;

    private JPanel content;

    private JTextField restrictionClassPackageIncludes;
    private JTextField restrictionClassNameExcludes;
    private JTextField restrictionClassNameIncludes;
    private JTextField restrictionClassAnnotationExcludes;
    private JTextField restrictionClassInheritanceExcludes;
    private JCheckBox restrictionCutMappings;
    private JCheckBox restrictionCutConstructors;
    private JCheckBox restrictionCutEnums;
    private JTextField restrictionClassPackageExcludes;
    private JTextField restrictionMethodNameExcludes;
    private JCheckBox restrictionCutTests;
    private JCheckBox restrictionCutInterfaceStructures;
    private JCheckBox restrictionCutDataStructures;
    private JCheckBox restrictionCutGettersAndSetters;
    private JTextField restrictionMethodNameIncludes;
    private JCheckBox restrictionCutClient;
    private JCheckBox restrictionCutDataAccess;

    private JButton loadRestrictionDefaultsCallDiagram;
    private JButton loadRestrictionDefaultsStructureDiagram;
    private JButton loadRestrictionDefaultsFlowDiagram;

    private JSpinner traversalForwardDepth;
    private JSpinner traversalBackwardDepth;
    private JTextField traversalClassNameExcludes;
    private JTextField traversalClassNameIncludes;
    private JTextField traversalMethodNameExcludes;
    private JTextField traversalMethodNameIncludes;
    private JCheckBox traversalHidePrivateMethods;
    private JCheckBox traversalHideDataStructures;
    private JCheckBox traversalHideMappings;
    private JCheckBox traversalHideInterfaceCalls;
    private JCheckBox traversalOnlyShowApplicationEntryPoints;

    private JButton loadTraversalDefaultsCallDiagram;
    private JButton loadTraversalDefaultsStructureDiagram;
    private JButton loadTraversalDefaultsFlowDiagram;

    // general actions
    private JButton loadMetadata;
    private JButton regenerate;

    private JCheckBox callShowMethodParameterNames;
    private JCheckBox callShowMethodParameterTypes;
    private JCheckBox callShowMethodReturnType;
    private JCheckBox callShowCallOrder;
    private JSpinner callShowPackageLevels;
    private JCheckBox callShowDetailedClasses;
    private JComboBox callAggregation;
    private JComboBox callEdgeMode;

    private JCheckBox structureShowMethodParameterNames;
    private JComboBox structureAggregation;
    private JCheckBox structureShowMethodReturnType;
    private JSpinner structureShowPackageLevels;
    private JCheckBox structureShowDetailedClasses;

    private JButton generateCallDiagram;
    private JButton generateFlowDiagram;
    private JButton generateStructureDiagram;
    private JCheckBox structureShowMethodParameterTypes;
    private JCheckBox structureShowMethods;
    private JCheckBox structureShowClassGenericTypes;

    public DiagramGeneratorConfigurationToolWindow(Project project){
        this.project = project;

        CallConfigurationDefaults defaults = ConfigurationDefaults.Companion.callDiagram();
        initRestrictionFields(defaults.getGraphRestriction());
        initTraversalFields(defaults.getGraphTraversal());

        initCallDetailFields(defaults.getDetails());
        initStructureDetailFields(ConfigurationDefaults.Companion.structureDiagram().getDetails());

        registerRestrictionActions();
        registerTraversalActions();
        registerGeneralActions();

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
    }

    private void registerGeneralActions() {
        loadMetadata.addActionListener(this::handleLoadFromMetadata);
        regenerate.addActionListener(e -> performAction(DiagramActions.RegenerateDiagramAction));

        generateCallDiagram.addActionListener(e -> performAction(DiagramActions.GenerateCallDiagramAction));
        generateStructureDiagram.addActionListener(e -> performAction(DiagramActions.GenerateStructureDiagramAction));
        generateFlowDiagram.addActionListener(e -> performAction(DiagramActions.GenerateFlowDiagramAction));
    }

    private void registerTraversalActions() {
        loadTraversalDefaultsCallDiagram.addActionListener(e ->
                initTraversalFields(ConfigurationDefaults.Companion.callDiagram().getGraphTraversal()));
        loadTraversalDefaultsStructureDiagram.addActionListener(e ->
                initTraversalFields(ConfigurationDefaults.Companion.structureDiagram().getGraphTraversal()));
        loadTraversalDefaultsFlowDiagram.addActionListener(e ->
                initTraversalFields(ConfigurationDefaults.Companion.flowDiagram().getGraphTraversal()));
    }

    private void registerRestrictionActions() {
        loadRestrictionDefaultsCallDiagram.addActionListener(e ->
                initRestrictionFields(ConfigurationDefaults.Companion.callDiagram().getGraphRestriction()));
        loadRestrictionDefaultsStructureDiagram.addActionListener(e ->
                initRestrictionFields(ConfigurationDefaults.Companion.structureDiagram().getGraphRestriction()));
        loadRestrictionDefaultsFlowDiagram.addActionListener(e ->
                initRestrictionFields(ConfigurationDefaults.Companion.flowDiagram().getGraphRestriction()));
    }

    private void initCallDetailFields(CallDiagramDetails details) {
        callAggregation.setSelectedItem(details.getAggregation());
        callEdgeMode.setSelectedItem(details.getEdgeMode());
        callShowCallOrder.setSelected(details.getShowCallOrder());
        callShowDetailedClasses.setSelected(details.getShowDetailedClassStructure());
        callShowMethodParameterNames.setSelected(details.getShowMethodParametersNames());
        callShowMethodParameterTypes.setSelected(details.getShowMethodParametersTypes());
        callShowMethodReturnType.setSelected(details.getShowMethodReturnType());
        callShowPackageLevels.setValue(details.getShowPackageLevels());
    }

    private void initStructureDetailFields(StructureDiagramDetails details) {
        structureAggregation.setSelectedItem(details.getAggregation());
        structureShowDetailedClasses.setSelected(details.getShowDetailedClassStructure());
        structureShowClassGenericTypes.setSelected(details.getShowClassGenericTypes());
        structureShowMethods.setSelected(details.getShowMethods());
        structureShowMethodParameterNames.setSelected(details.getShowMethodParameterNames());
        structureShowMethodParameterTypes.setSelected(details.getShowMethodParameterTypes());
        structureShowMethodReturnType.setSelected(details.getShowMethodReturnType());
        structureShowPackageLevels.setValue(details.getShowPackageLevels());
    }

    private void initTraversalFields(GraphTraversal traversal) {
        traversalForwardDepth.setValue(traversal.getForwardDepth());
        traversalBackwardDepth.setValue(traversal.getBackwardDepth());

        traversalClassNameExcludes.setText(traversal.getClassNameExcludeFilter());
        traversalClassNameIncludes.setText(traversal.getClassNameIncludeFilter());
        traversalMethodNameExcludes.setText(traversal.getMethodNameExcludeFilter());
        traversalMethodNameIncludes.setText(traversal.getMethodNameIncludeFilter());

        traversalHideDataStructures.setSelected(traversal.getHideDataStructures());
        traversalHideInterfaceCalls.setSelected(traversal.getHideInterfaceCalls());
        traversalHideMappings.setSelected(traversal.getHideMappings());
        traversalHidePrivateMethods.setSelected(traversal.getHidePrivateMethods());
        traversalOnlyShowApplicationEntryPoints.setSelected(traversal.getOnlyShowApplicationEntryPoints());
    }

    private GraphRestriction getRestrictions(){
        GraphRestriction restriction = new GraphRestriction();
        restriction.setClassPackageExcludeFilter(restrictionClassPackageExcludes.getText());
        restriction.setClassPackageIncludeFilter(restrictionClassPackageIncludes.getText());
        restriction.setClassNameExcludeFilter(restrictionClassNameExcludes.getText());
        restriction.setClassNameIncludeFilter(restrictionClassNameIncludes.getText());
        restriction.setRemoveByInheritance(restrictionClassInheritanceExcludes.getText());
        restriction.setRemoveByAnnotation(restrictionClassAnnotationExcludes.getText());

        restriction.setMethodNameExcludeFilter(restrictionMethodNameExcludes.getText());
        restriction.setMethodNameIncludeFilter(restrictionMethodNameIncludes.getText());

        restriction.setCutClient(restrictionCutClient.isSelected());
        restriction.setCutTests(restrictionCutTests.isSelected());
        restriction.setCutDataStructures(restrictionCutDataStructures.isSelected());
        restriction.setCutInterfaceStructures(restrictionCutInterfaceStructures.isSelected());
        restriction.setCutMappings(restrictionCutMappings.isSelected());
        restriction.setCutDataAccess(restrictionCutDataAccess.isSelected());
        restriction.setCutEnum(restrictionCutEnums.isSelected());
        restriction.setCutGetterAndSetter(restrictionCutGettersAndSetters.isSelected());
        restriction.setCutConstructors(restrictionCutConstructors.isSelected());

        return restriction;
    }

    private GraphTraversal getTraversal(){
        GraphTraversal traversal = new GraphTraversal();
        traversal.setClassNameExcludeFilter(traversalClassNameExcludes.getText());
        traversal.setClassNameIncludeFilter(traversalClassNameIncludes.getText());

        traversal.setMethodNameExcludeFilter(traversalMethodNameExcludes.getText());
        traversal.setMethodNameIncludeFilter(traversalMethodNameIncludes.getText());

        traversal.setForwardDepth((Integer) traversalForwardDepth.getValue());
        traversal.setBackwardDepth((Integer) traversalBackwardDepth.getValue());
        traversal.setHideDataStructures(traversalHideDataStructures.isSelected());
        traversal.setHideInterfaceCalls(traversalHideInterfaceCalls.isSelected());
        traversal.setHideMappings(traversalHideMappings.isSelected());
        traversal.setHidePrivateMethods(traversalHidePrivateMethods.isSelected());
        traversal.setOnlyShowApplicationEntryPoints(traversalOnlyShowApplicationEntryPoints.isSelected());

        return traversal;
    }

    private CallDiagramDetails getCallDetails(){
        CallDiagramDetails details = new CallDiagramDetails();

        details.setAggregation(Aggregation.valueOf(callAggregation.getSelectedItem().toString()));
        details.setEdgeMode(EdgeMode.valueOf(callEdgeMode.getSelectedItem().toString()));
        details.setShowCallOrder(callShowCallOrder.isSelected());
        details.setShowMethodParametersTypes(callShowMethodParameterTypes.isSelected());
        details.setShowMethodParametersNames(callShowMethodParameterNames.isSelected());
        details.setShowMethodReturnType(callShowMethodReturnType.isSelected());
        details.setShowDetailedClassStructure(callShowCallOrder.isSelected());
        details.setShowPackageLevels((Integer) callShowPackageLevels.getValue());

        return details;
    }

    private StructureDiagramDetails getStructureDetails(){
        StructureDiagramDetails details = new StructureDiagramDetails();

        details.setAggregation(Aggregation.valueOf(structureAggregation.getSelectedItem().toString()));
        details.setShowPackageLevels((Integer) structureShowPackageLevels.getValue());
        details.setShowClassGenericTypes(structureShowClassGenericTypes.isSelected());
        details.setShowMethods(structureShowMethods.isSelected());
        details.setShowMethodParameterNames(structureShowMethodParameterNames.isSelected());
        details.setShowMethodParameterTypes(structureShowMethodParameterTypes.isSelected());
        details.setShowMethodReturnType(structureShowMethodReturnType.isSelected());
        details.setShowDetailedClassStructure(structureShowDetailedClasses.isSelected());

        return details;

    }

    private void initRestrictionFields(GraphRestriction restriction) {
        restrictionClassPackageExcludes.setText(restriction.getClassPackageExcludeFilter());
        restrictionClassPackageIncludes.setText(restriction.getClassPackageIncludeFilter());
        restrictionClassNameExcludes.setText(restriction.getClassNameExcludeFilter());
        restrictionClassNameIncludes.setText(restriction.getClassNameIncludeFilter());
        restrictionClassInheritanceExcludes.setText(restriction.getRemoveByInheritance());
        restrictionClassAnnotationExcludes.setText(restriction.getRemoveByAnnotation());

        restrictionMethodNameExcludes.setText(restriction.getMethodNameExcludeFilter());
        restrictionMethodNameIncludes.setText(restriction.getMethodNameIncludeFilter());

        restrictionCutClient.setSelected(restriction.getCutClient());
        restrictionCutTests.setSelected(restriction.getCutTests());
        restrictionCutDataStructures.setSelected(restriction.getCutDataStructures());
        restrictionCutInterfaceStructures.setSelected(restriction.getCutInterfaceStructures());
        restrictionCutMappings.setSelected(restriction.getCutMappings());
        restrictionCutDataAccess.setSelected(restriction.getCutDataAccess());
        restrictionCutEnums.setSelected(restriction.getCutEnum());
        restrictionCutGettersAndSetters.setSelected(restriction.getCutGetterAndSetter());
        restrictionCutConstructors.setSelected(restriction.getCutConstructors());
    }

    private void handleLoadFromMetadata(ActionEvent actionEvent) {
        DiagramConfiguration loadedConfig = Optional.ofNullable(FileEditorManager.getInstance(project).getSelectedEditor())
                .map(editor -> ((TextEditor) editor).getEditor().getDocument().getText())
                .map(diagramText -> SerializationKt.loadFromMetadata(DiagramConfiguration.Companion, diagramText))
                .orElse(null);

        if(loadedConfig instanceof StructureConfiguration){
            StructureConfiguration structureConfiguration = (StructureConfiguration) loadedConfig;
            initRestrictionFields(structureConfiguration.getGraphRestriction());
            initTraversalFields(structureConfiguration.getGraphTraversal());
            initStructureDetailFields(structureConfiguration.getDetails());
        } else if(loadedConfig instanceof CallConfiguration){
            CallConfiguration callConfiguration = (CallConfiguration) loadedConfig;
            initRestrictionFields(callConfiguration.getGraphRestriction());
            initTraversalFields(callConfiguration.getGraphTraversal());
            initCallDetailFields(callConfiguration.getDetails());
        } else if(loadedConfig instanceof FlowConfiguration){
            FlowConfiguration flowConfiguration = (FlowConfiguration) loadedConfig;
            initRestrictionFields(flowConfiguration.getGraphRestriction());
            initTraversalFields(flowConfiguration.getGraphTraversal());
        }
    }

    private DiagramConfiguration getDiagramConfiguration(DiagramActions actionId, PsiClass rootClass){
        DiagramConfiguration configuration = null;

        if(actionId == DiagramActions.GenerateCallDiagramAction){
            configuration = new CallConfiguration(rootClass, null,
                    ConfigurationDefaults.Companion.classification(),
                    getRestrictions(),
                    getTraversal(),
                    getCallDetails());
        } else if(actionId == DiagramActions.GenerateStructureDiagramAction){
            configuration = new StructureConfiguration(rootClass,
                    ConfigurationDefaults.Companion.classification(),
                    getRestrictions(),
                    getTraversal(),
                    getStructureDetails());
        } else if(actionId == DiagramActions.GenerateFlowDiagramAction){
            configuration = new FlowConfiguration(rootClass, null,
                    ConfigurationDefaults.Companion.classification(),
                    getRestrictions(),
                    getTraversal());
        }

        return configuration;
    }

    private void performAction(DiagramActions actionId) {
        FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        DataContext dataContext = DataManager.getInstance().getDataContext(selectedEditor.getComponent());

        AnActionEvent event = new AnActionEvent(null, dataContext,
                ActionPlaces.UNKNOWN, new Presentation(),
                ActionManager.getInstance(), 0);

        AnAction action = ActionManager.getInstance().getAction(actionId.name());
        if(action instanceof AbstractDiagramAction){
            PsiClass rootClass = AbstractDiagramActionKt.findFirstClass(event);
            DiagramConfiguration configuration = getDiagramConfiguration(actionId, rootClass);

            ((AbstractDiagramAction) action).generateWith(event, configuration);
        } else {
            action.actionPerformed(event);
        }
    }

}
