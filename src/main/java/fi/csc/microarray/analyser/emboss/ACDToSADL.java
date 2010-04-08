package fi.csc.microarray.analyser.emboss;

import java.util.HashMap;
import java.util.LinkedList;

import fi.csc.microarray.description.GenericInputTypes;
import fi.csc.microarray.description.SADLDescription;
import fi.csc.microarray.description.SADLDescription.Name;
import fi.csc.microarray.description.SADLDescription.Input;
import fi.csc.microarray.description.SADLDescription.Output;
import fi.csc.microarray.description.SADLDescription.Parameter;
import fi.csc.microarray.description.SADLSyntax.ParameterType;

/**
 * Conversion of ACD abstraction to SADL abstraction.
 * 
 * @author naktinis
 *
 */
public class ACDToSADL {
	
	private ACDDescription acd;
	
	public ACDToSADL(ACDDescription acd) {
		this.acd = acd;
	}

	/**
	 * Analyse a given ACD object and store it as a SADL abstraction.
	 * 
	 * @return SADL object.
	 */
	public SADLDescription convert() {
        SADLDescription sadl = new SADLDescription(Name.createName(acd.getName()), acd.getGroups().get(0),
	                                               acd.getDescription());
	    
	    // Get all input parameters
	    // We are also safe from trying to include non-input parameters in input
	    //     section (such as toggle), since SADLParameterCreator does type-checking.
	    // TODO: deal with non-input parameters in input and output sections
	    
	    LinkedList<ACDParameter> params = acd.getParameters();
	    for (ACDParameter param : params) {
	        SADLParameterCreator.createAndAdd(param, sadl);
        }
        
	    return sadl;
    }
	
	/**
	 * Create SADL parameters using information from ACD parser.
	 * 
	 * @author naktinis
	 * 
	 */
	public static class SADLParameterCreator {	    
	    /**
	     * Create a SADL parameter (Parameter, Input or Output) and
	     * add it to a given object.
	     * 
	     * @param parser - ACD Parser object (to read the parameter).
	     * @param index - parameter index in the ACD Parser (to read the parameter).
	     * @param internalRepr - the object to add this parameter to.
	     */

	    public static void createAndAdd(ACDParameter acdParam, SADLDescription internalRepr) {
	        // Try to create a parameter (simple types, list types)
	        Parameter param = createParameter(acdParam);
	        if (param != null) {
	            internalRepr.addParameter(param);
	            return;
	        }
	        
	        // Try to create an input
	        Input input = createInput(acdParam);
	        if (input != null) {
	            internalRepr.addInput(input);
	            return;
	        }
	        
	        // Try to create an output
	        Output output = createOutput(acdParam);
	        if (output != null) {
	            internalRepr.addOutput(output);
	        }
	    }
	    
	    /**
	     * Create a parameter and add it to a given SADL object.
	     * Creates simple parameters, such as integers, strings etc.
	     * 
	     * @param parser - parser object.
	     * @param index - index of a field to be parsed.
	     * @return SADL parameter object or null.
	     */
	    public static Parameter createParameter(ACDParameter param) {
	        String fieldType = param.getType();
	        String fieldName = param.getName();
	        
	        // Detect the parameter functional group
	        Integer type = ACDParameter.detectParameterGroup(fieldType.toLowerCase());
	        
	        // Map simple ACD parameters to SADL parameters
	        HashMap<String, ParameterType> typeMap = new HashMap<String, ParameterType>();
	        typeMap.put("array", ParameterType.STRING);
	        typeMap.put("float", ParameterType.DECIMAL);
	        typeMap.put("integer", ParameterType.INTEGER);
	        typeMap.put("string", ParameterType.STRING);
	        typeMap.put("range", ParameterType.STRING);
	        typeMap.put("boolean", ParameterType.ENUM);
	        typeMap.put("toggle", ParameterType.ENUM);
	        typeMap.put("list", ParameterType.ENUM);
	        typeMap.put("selection", ParameterType.ENUM);
	        
	        // Read common attributes
	        // Don't use attributes with variable references etc.
            String fieldDefault = null;
            String fieldMin = null;
            String fieldMax = null;
	        if (param.attributeIsEvaluated("default") &&
	            !param.getAttribute("default").equals("")) {
	            fieldDefault = param.getAttribute("default");
	        }
            if (param.attributeIsEvaluated("minimum")) {
                fieldMin = param.getAttribute("minimum");
            }
            if (param.attributeIsEvaluated("maximum")) {
                fieldMax = param.getAttribute("maximum");
            }
	        
	        // Use help attribute if available
            String fieldHelp = param.getAttribute("help");
            String fieldInfo = param.getAttribute("information");
            if (fieldHelp == null || fieldHelp == "") {
                fieldHelp = param.getAttribute("information");
            }
            if (fieldHelp != null) {
                fieldHelp = fieldHelp.replaceAll("\n", "");
            }
            
	        // Construct a parameter
	        Parameter sadlParam = null;
	        if (fieldType.equals("boolean") || fieldType.equals("toggle")) {
	            // Boolean types need some special handling	            
	            Name[] fieldOptions = {Name.createName("Y", "Yes"), Name.createName("N", "No")};
	            
	            // We need to have some default value for boolean, since there are
	            // only 2 options (or consider adding a third option - "not selected")
	            if (fieldDefault == null) {
	                fieldDefault = "N";
	            }
	            
	            sadlParam = new Parameter(Name.createName(fieldName, fieldInfo), typeMap.get(fieldType), fieldOptions,
	                    null, null, fieldDefault, fieldHelp);
	        } else if (type == ACDParameter.PARAM_GROUP_SIMPLE) {
	            sadlParam = new Parameter(Name.createName(fieldName, fieldInfo), typeMap.get(fieldType), null,
	                                 fieldMin, fieldMax, fieldDefault, fieldHelp);
	        } else if (type == ACDParameter.PARAM_GROUP_LIST) {
	            HashMap<String, String> fieldOptions = param.getList();
                LinkedList<String> fieldValueList = new LinkedList<String>(fieldOptions.values());
	            Name[] fieldValues = new Name[fieldValueList.size()];
	            
	            // Sometimes default value points to label instead of value
	            if (!fieldOptions.keySet().contains(fieldDefault)) {
	                // Find the key for this default label and store
	                // the key instead of label
	                for (String key : fieldOptions.keySet()) {
	                    if (fieldOptions.get(key).equals(fieldDefault)) {
	                        fieldDefault = key;
	                    }
	                }
	            }
	            
                // Convert to string array
                // NOTE: I planned adding an additional blank choice, but all
                // of the lists in current acd files have default parameters
	            int i = 0;
                for (String key : fieldOptions.keySet()) {
                    fieldValues[i] = Name.createName(key, fieldOptions.get(key));
                    i++;
                }	            
	            
                sadlParam = new Parameter(Name.createName(fieldName, fieldInfo), typeMap.get(fieldType), fieldValues,
	                                 fieldMin, fieldMax, fieldDefault, fieldHelp);
	        }
	        
	        // Mark as optional if needed
	        if (sadlParam != null) {
	            sadlParam.setOptional(param.isAdditional() || param.isAdvanced());
	        }
	        
	        return sadlParam;
	    }
	    
	    /**
	     * Create a SADL input and add it to a given object.
	     * Creates objects representing input files.
	     * 
	     * @param parser - parser object.
	     * @param index - index of a field to be parsed.
	     * @return SADL parameter object or null.
	     */
	    public static Input createInput(ACDParameter param) { 
	        String fieldType = param.getType();
	        String fieldName = param.getName();
	        
	        // Detect the parameter functional group
	        Integer type = ACDParameter.detectParameterGroup(fieldType.toLowerCase());
	        
	        // TODO: help attribute; comment attribute
	        // Skip all non-required inputs
	        if (type == ACDParameter.PARAM_GROUP_INPUT &&
	            param.isRequired()) {
	            Input input = new Input(GenericInputTypes.GENERIC, Name.createName(fieldName), true);
	            return input;
	        } else {
	            return null;
	        }
	    }
	    
	    /**
	     * Create objects representing output files (currently
	     * returns a string).
	     * 
	     * @param parser - parser object.
	     * @param index - index of a field to be parsed.
	     * @return SADL parameter object or null.
	     */
	    public static Output createOutput(ACDParameter param) { 
	        // Detect the parameter functional group
	        String fieldType = param.getType();
	        Integer type = ACDParameter.detectParameterGroup(fieldType.toLowerCase());
	        
	        // TODO: help attribute; comment attribute
	        if ((type == ACDParameter.PARAM_GROUP_OUTPUT ||
	             type == ACDParameter.PARAM_GROUP_GRAPHICS) &&
	            (!param.isAdvanced())) {
	            return new Output(Name.createName(param.getOutputFilename(true)), !param.isRequired());
	        } else {
	            return null;
	        }
	    }
	}
}