package fi.csc.microarray.analyser.java;

import fi.csc.microarray.analyser.OnDiskAnalysisJobBase;

public abstract class JavaAnalysisJobBase extends OnDiskAnalysisJobBase {

	@Override
	protected void preExecute() throws Exception {
		super.preExecute();		
	}

	@Override
	protected void postExecute() throws Exception {
		super.postExecute();
	}

	@Override
	protected void cleanUp() {
		super.cleanUp();
	}
	
	@Override
	protected void cancelRequested() {
		// ignore by default
	}

	public abstract String getSADL();
}
