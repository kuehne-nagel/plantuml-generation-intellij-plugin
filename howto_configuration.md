## Configuration

### Project Classification

The project classification defines naming patterns and packages to identify different kinds of classes. This allows you 
to easily play around with the filter logic and find an understandable representation. The classification can only be 
changed via the [Settings](#Settings Page) or [Metadata Section](#Metadata Section) inside the generated diagram file. 

### Restriction Filter

The restriction filter defines how the code-graph is build up. Basic nodes are Classes and Methods. 
Edges are method calls, class fields and other relationships. The more restrictive the filter is the faster the diagram is generated.

### Traversal filter

The traversal filter configures the search inside the code-graph. Nodes can be excluded and therefore not be displayed. 
Skipped nodes do not stop the search. Instead, a displayed edge always contains two non-filtered nodes. This allows 
to arbitrary filter and hide un-relevant information. E.g. show only a call hierarchy for public methods.

### Metadata Section

Saves the configuration which was the base to generate the diagram. This allows everyone to generate the diagram again and produce the same result. Also the puml file can be renamed since the root class of the diagram is saved as metadata. You can also change the metadata manually and directly regenerate again to save some time. But be aware that there is no validation of the format in place.
Every diagram can be shown in the browser for better zooming. Small examples will fit in the PlantUML sidebar.

### Side Bar for configuration

The side bar provides you a UI to change the metadata section. 

### Settings Page

Navigate to: Settings > Other Settings > Diagram Generation

Here you can define your default values which are used when a diagram is generated from the context menu.
