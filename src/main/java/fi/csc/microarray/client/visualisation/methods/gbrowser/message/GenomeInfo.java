package fi.csc.microarray.client.visualisation.methods.gbrowser.message;

import java.net.URL;

//Backport from Chipster 3 GenoemInfo
public class GenomeInfo {
	
	private String species;
	private String version;
	private URL ensemblBrowserUrl;
	private URL ucscBrowserUrl;
	
	public String getSpecies() {
		return species;
	}
	
	public void setSpecies(String species) {
		this.species = species;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}	
	
	public URL getEnsembl() {
		return ensemblBrowserUrl;
	}
	
	public void setEnsemblBrowserUrl(URL ensemblBrowserUrl) {
		this.ensemblBrowserUrl = ensemblBrowserUrl;
	}
	
	public URL getBrowserUrl() {
		return ucscBrowserUrl;
	}
	
	public void setUcscBrowserUrl(URL ucscBrowserUrl) {
		this.ucscBrowserUrl = ucscBrowserUrl;
	}
}