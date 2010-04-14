package fi.csc.microarray.databeans.handlers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import fi.csc.microarray.databeans.DataBean;
import fi.csc.microarray.databeans.DataBean.DataBeanType;

public class LocalFileDataBeanHandler extends DataBeanHandlerBase {

	public LocalFileDataBeanHandler() {
		super(DataBeanType.LOCAL_USER, DataBeanType.LOCAL_TEMP);
	}
	
	
	public InputStream getInputStream(DataBean dataBean) throws FileNotFoundException {
		checkCompatibility(dataBean);
		return new BufferedInputStream(new FileInputStream(getFile(dataBean)));
	}

	public long getContentLength(DataBean dataBean) {
		checkCompatibility(dataBean);
		return getFile(dataBean).length();
	}

	/**
	 * Only delete temporary files, never user files.
	 */
	public void delete(DataBean dataBean) {
		if (dataBean.getType().equals(DataBean.DataBeanType.LOCAL_TEMP)) {

			checkCompatibility(dataBean);
			File file = getFile(dataBean);
			file.delete();
		}
	}
	
	
	protected void checkCompatibility(DataBean dataBean) throws IllegalArgumentException {
		super.checkCompatibility(dataBean);

		URL url = dataBean.getContentUrl();
		
		// null url
		if (url == null) {
			throw new IllegalArgumentException("DataBean url is null.");
		} 
		
		// protocol not "file"
		else if (!"file".equals(url.getProtocol())) {
			throw new IllegalArgumentException("Protocol of " + url.toString() + " is not \"file\".");
		} 
		
		// null or empty path
		else if (url.getPath() == null || url.getPath().length() == 0) {
			throw new IllegalArgumentException("Illegal path:" + url.toString());
		} 
	}
	
	private File getFile(DataBean dataBean) {
		return new File(dataBean.getContentUrl().getPath());
	}

}
