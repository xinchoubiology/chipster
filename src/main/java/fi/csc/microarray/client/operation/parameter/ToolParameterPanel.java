package fi.csc.microarray.client.operation.parameter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import fi.csc.microarray.client.operation.Operation;
import fi.csc.microarray.client.operation.OperationPanel;
import fi.csc.microarray.constants.VisualConstants;
import fi.csc.microarray.exception.MicroarrayException;

/**
 * ParameterPanel is the GUI component for showing the parameters for an
 * operation, along with a contextual help textbox showing information about
 * the currently selected parameter and eventual errors in user input. 
 *  A new ParameterPanel will be created every time it is
 * to be shown, whether the corresponding Operation (derived from an
 * OperationDefinition) already exists or not. At least for now. Thought
 * it would save some memory.
 * 
 * @author Janne KÃ¤ki, Aleksi Kallio, Petri KlemelÃ¤
 *
 */
@SuppressWarnings("serial")
public class ToolParameterPanel extends ParameterPanel {
	private JScrollPane scroller;

	private static final int LEFT_MARGIN = 10;
	private static final int TOP_MARGIN = 2;

	private OperationPanel parent;
	
	/**
	 * Creates a new ParameterPanel for the given operation.
	 * 
	 * @param operation The operation which is to be started from this panel.
	 * @param client The client that is to be informed when a job is done.
	 * @throws MicroarrayException 
	 */
	public ToolParameterPanel(Operation operation, OperationPanel parent) throws MicroarrayException {
		super(operation, new BorderLayout());
		this.parent = parent;		
	
		
		JPanel paramPane = new JPanel(new GridBagLayout());
		GridBagConstraints con = new GridBagConstraints();
		
		con.gridx = 0; con.gridy = 0;
		con.gridwidth = 1;
		con.weightx = 1.0; con.weighty = 0;
		con.anchor = GridBagConstraints.WEST;
		
		
		paramMap = new HashMap<Parameter, ParameterInputComponent>();
		for (Parameter param : operation.getParameters()) {
			ParameterInputComponent component = createInputComponent(param);
			
			paramMap.put(param, component);
			
			con.gridx = 0;
			con.gridy++;
			con.insets.top = TOP_MARGIN;
			con.insets.left = LEFT_MARGIN;
			con.fill = GridBagConstraints.HORIZONTAL;
			paramPane.add(component.getLabel(), con);
			con.gridx = 1;
			con.anchor = GridBagConstraints.EAST;
			con.fill = GridBagConstraints.NONE;
			paramPane.add(component, con);
		}
		
		con.weighty = 1;
		con.weightx = 0;
		con.gridx = 0;		
		con.gridy++;
		con.gridwidth = 2;
		con.fill = GridBagConstraints.BOTH;
		paramPane.add(new JPanel(),con);
		
		scroller = new JScrollPane(paramPane);
		scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.setBorder(BorderFactory.createMatteBorder(0,0,0,1,VisualConstants.OPERATION_LIST_BORDER_COLOR));
		
		this.add(scroller, BorderLayout.CENTER);
	}
	
	
	/**
	 * Sets the message of the (bottom left) infobox of this parameter panel.
	 * 
	 * @param message The text to be set.
	 * @param color The font color to be used for the message.
	 */
	@Override
	public void setMessage(String message, Color color) {
		parent.setInfoText(message, color, true);
	}
}
