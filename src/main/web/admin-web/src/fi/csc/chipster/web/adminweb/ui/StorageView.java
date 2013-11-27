package fi.csc.chipster.web.adminweb.ui;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

import javax.jms.JMSException;

import org.apache.log4j.Logger;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;

import fi.csc.chipster.web.adminweb.ChipsterAdminUI;
import fi.csc.chipster.web.adminweb.data.StorageAdminAPI;
import fi.csc.chipster.web.adminweb.data.StorageAggregate;
import fi.csc.chipster.web.adminweb.data.StorageAggregateContainer;
import fi.csc.chipster.web.adminweb.data.StorageEntryContainer;
import fi.csc.chipster.web.adminweb.util.StringUtils;
import fi.csc.microarray.config.ConfigurationLoader.IllegalConfigurationException;
import fi.csc.microarray.exception.MicroarrayException;

@SuppressWarnings("serial")
public class StorageView extends VerticalLayout implements ClickListener, ValueChangeListener {
	
	private static final Logger logger = Logger.getLogger(StorageView.class);

	private static final String DISK_USAGE_BAR_CAPTION = "Total disk usage";
	private StorageEntryTable entryTable;
	private StorageAggregateTable aggregateTable;

	private HorizontalLayout toolbarLayout;

	private Button refreshButton = new Button("Refresh");

	private StorageEntryContainer entryDataSource;
	private StorageAggregateContainer aggregateDataSource;

	private ChipsterAdminUI app;
	private ProgressIndicator diskUsageBar;
	private HorizontalLayout storagePanels;
	
	private ProgressIndicator progressIndicator = new ProgressIndicator(0.0f);

	private StorageAdminAPI adminEndpoint;
	private static final int POLLING_INTERVAL = 100;
	
	private ExecutorService executor = Executors.newCachedThreadPool();

	public StorageView(ChipsterAdminUI app) {

		this.app = app;

		this.addComponent(getToolbar());
		
		progressIndicator.setWidth(100, Unit.PERCENTAGE);
		this.addComponent(progressIndicator);
		
		entryTable = new StorageEntryTable(this);
		aggregateTable = new StorageAggregateTable(this);

		
		HorizontalLayout aggregatePanelLayout = new HorizontalLayout();
		HorizontalLayout entryPanelLayout = new HorizontalLayout();
		
		aggregatePanelLayout.setSizeFull();
		entryPanelLayout.setSizeFull();
		
		aggregatePanelLayout.addComponent(aggregateTable);
		entryPanelLayout.addComponent(entryTable);
		
		aggregatePanelLayout.setExpandRatio(aggregateTable, 1);
		entryPanelLayout.setExpandRatio(entryTable, 1);
		
		Panel aggregatePanel = new Panel("Disk usage by user");
		Panel entryPanel = new Panel("Stored sessions");
		
		aggregatePanel.setWidth(300, Unit.PIXELS);
		aggregatePanel.setHeight(100, Unit.PERCENTAGE);
		entryPanel.setSizeFull();
		
		aggregatePanel.setContent(aggregatePanelLayout);
		entryPanel.setContent(entryPanelLayout);
		
		storagePanels = new HorizontalLayout();
		storagePanels.setSizeFull();
					
		storagePanels.addComponent(aggregatePanel);
		storagePanels.addComponent(entryPanel);
		
		storagePanels.setExpandRatio(entryPanel, 1);
		
		this.setSizeFull();
		this.addComponent(storagePanels);		
		this.setExpandRatio(storagePanels, 1);
		
		try {
			
			adminEndpoint = new StorageAdminAPI();
			entryDataSource = new StorageEntryContainer(adminEndpoint);
			aggregateDataSource = new StorageAggregateContainer(adminEndpoint);
			
			entryTable.setContainerDataSource(entryDataSource);
			aggregateTable.setContainerDataSource(aggregateDataSource);
		} catch (JMSException | IOException | IllegalConfigurationException | MicroarrayException | InstantiationException | IllegalAccessException e) {
			logger.error(e);
		}		
	}

	public void update() {
		
		updateStorageAggregates();
		updateStorageTotals();
	}
	
	private void waitForUpdate(final Future<?> future) {				
					
		//This makes the browser start polling, but the browser will get it only if this is executed in this original thread.
		setProgressIndicatorValue(0f);
		
		executor.execute(new Runnable() {
			public void run() {								
				try {									
					/* Separate delay from what happens in the Container, because communication between
					 * threads is messy. Nevertheless, these delays should have approximately same duration
					 * to prevent user from starting several background updates causing concurrent modifications.   
					 */
					final int DELAY = 300; 				
					for (int i = 0; i <= DELAY; i++) {

						try {
							future.get(POLLING_INTERVAL, TimeUnit.MILLISECONDS);
							break;
						} catch (TimeoutException e) {
							//No results yet, update progress bar							
							setProgressIndicatorValue((float)i/DELAY);			
						}
					}
					//Update was successful
					entryUpdateDone();

				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				} finally {				
					setProgressIndicatorValue(1.0f);
				}
			}
		});
	}
	
	private void setProgressIndicatorValue(float value) {
		//This happens in initialization 
		if (progressIndicator.getUI() != null ) {
			
			Lock indicatorLock = progressIndicator.getUI().getSession().getLockInstance();
			
			//Component has to be locked before modification from background thread
			indicatorLock.lock();					
			try {
				progressIndicator.setValue(value);
				
				if (value == 1.0f) {
					refreshButton.setEnabled(true);
					progressIndicator.setPollingInterval(Integer.MAX_VALUE);	
				} else {
					refreshButton.setEnabled(false);
					progressIndicator.setPollingInterval(POLLING_INTERVAL);
				}
			} finally {
				indicatorLock.unlock();
			}
		}
	}

	/**
	 * Set disk usage. Calls from other threads are allowed.
	 * 
	 * @param usedSpace
	 * @param freeSpace
	 */
	public void setDiskUsage(long usedSpace, long freeSpace) {
		
		//maybe null if the UI thread hasn't initialized this yet
		if (diskUsageBar.getUI() != null) {
			Lock barLock = diskUsageBar.getUI().getSession().getLockInstance();
			barLock.lock();
			try {
				long used = usedSpace;
				long total = usedSpace + freeSpace;
				float division = used / (float)total;
				
				diskUsageBar.setValue(division);
				diskUsageBar.setCaption(DISK_USAGE_BAR_CAPTION + " ( " + 
						StringUtils.getHumanReadable(used) + " / " + StringUtils.getHumanReadable(total) + " )");
				
				if (division > 0.7) {
					diskUsageBar.removeStyleName("ok");
					diskUsageBar.addStyleName("fail");
				} else {
					diskUsageBar.removeStyleName("fail");
					diskUsageBar.addStyleName("ok");
				}
				
				diskUsageBar.markAsDirty();
			} finally {
				barLock.unlock();
			}
		}
	}

	public HorizontalLayout getToolbar() {

		if (toolbarLayout == null) {
			toolbarLayout = new HorizontalLayout();
			refreshButton.addClickListener((ClickListener)this);
			toolbarLayout.addComponent(refreshButton);

			refreshButton.setIcon(new ThemeResource("../runo/icons/32/reload.png"));
			
			Label spaceEater = new Label(" ");
			toolbarLayout.addComponent(spaceEater);
			toolbarLayout.setExpandRatio(spaceEater, 1);
			
			diskUsageBar = new ProgressIndicator(0f);
			diskUsageBar.setCaption(DISK_USAGE_BAR_CAPTION);
			diskUsageBar.setStyleName("big");
			diskUsageBar.setPollingInterval(Integer.MAX_VALUE);
			
			diskUsageBar.setWidth(300, Unit.PIXELS);
			toolbarLayout.addComponent(diskUsageBar);
			toolbarLayout.setExpandRatio(diskUsageBar, 1);
					
			toolbarLayout.addComponent(app.getTitle());	

			toolbarLayout.setWidth("100%");
			toolbarLayout.setStyleName("toolbar");
		}
		return toolbarLayout;

	}

	public void buttonClick(ClickEvent event) {
		final Button source = event.getButton();

		if (source == refreshButton) {
						
			update();
			updateStorageEntries(null);
		}
	}

	public void valueChange(ValueChangeEvent event) {
		Property<?> property = event.getProperty();
		if (property == aggregateTable) {
						
			Object tableValue = aggregateTable.getValue();
			if (tableValue instanceof StorageAggregate) {
				StorageAggregate storageUser = (StorageAggregate) tableValue;
				updateStorageEntries(storageUser.getUsername());
			}			
		}
	}
	
	private void updateStorageEntries(final String username) {
		Future<?> future = executor.submit(new Runnable() {

			@Override
			public void run() {				
				entryDataSource.update(StorageView.this, username);
			}			
		});
		
		waitForUpdate(future);
	}
	
	private void updateStorageAggregates() {
		Future<?> future = executor.submit(new Runnable() {

			@Override
			public void run() {				
				aggregateDataSource.update(StorageView.this);
			}			
		});
		
		waitForUpdate(future);
	}
	
	private void updateStorageTotals() {
		Future<?> future = executor.submit(new Runnable() {

			@Override
			public void run() {								
				try {
					Long[] totals = adminEndpoint.getStorageUsage();	
					
					if (totals != null) {
						StorageView.this.setDiskUsage(totals[0], totals[1]);
					} else {
						Notification.show("Timeout", "Chipster filebroker server doesn't respond", Type.ERROR_MESSAGE);
						logger.error("timeout while waiting storage usage totals");
					}

				} catch (JMSException | InterruptedException e) {
					logger.error(e);
				}			
			}			
		});
		
		waitForUpdate(future);
	}
	
	public ChipsterAdminUI getApp() {
		return app;
	}

	public void delete(Object itemId) {
		//TODO are you sure?
		try {
			adminEndpoint.deleteRemoteSession(entryDataSource.getItem(itemId).getBean().getID());
		} catch (JMSException e) {
			logger.warn("could not delete session", e);
			return;
		}
		
		entryDataSource.removeItem(itemId);
		
		aggregateDataSource.update(this);
		updateStorageTotals();
	}
	
	/**
	 * Calling from background threads allowed
	 */
	public void entryUpdateDone() {
					
				
		if (entryTable.getUI() != null) {
			Lock entryTableLock = entryTable.getUI().getSession().getLockInstance();
			entryTableLock.lock();
			try {

				entryTable.setVisibleColumns(StorageEntryContainer.NATURAL_COL_ORDER);
				entryTable.setColumnHeaders(StorageEntryContainer.COL_HEADERS_ENGLISH);

			} finally {
				entryTableLock.unlock();
			}
		}
		
		if (aggregateTable.getUI() != null) {
			Lock aggregateTableLock = aggregateTable.getUI().getSession().getLockInstance();
			aggregateTableLock.lock();
			try {						
				aggregateTable.setVisibleColumns(StorageAggregateContainer.NATURAL_COL_ORDER);
				aggregateTable.setColumnHeaders(StorageAggregateContainer.COL_HEADERS_ENGLISH);				

			} finally {
				aggregateTableLock.unlock();
			}
		}
	}

	public AbstractClientConnector getEntryTable() {
		return entryTable;
	}

	public void clean() {
		if (adminEndpoint != null) {
			adminEndpoint.clean();
		}
	}
}
