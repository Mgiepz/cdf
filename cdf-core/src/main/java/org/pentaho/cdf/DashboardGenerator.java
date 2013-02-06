/**
 * 
 */
package org.pentaho.cdf;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import pt.webdetails.cpf.http.ICommonParameterProvider;

/**
 * This abstract class encapsulates the algorithm for generating
 * a cdf dashboard.
 * Individual implementations like Pentaho and Saiku can override template methods
 * and/or use provided hooks to customize that process.
 * 
 * @author mg
 *
 */
public abstract class DashboardGenerator {

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setRelativeUrl(String relativeUrl) {
		RELATIVE_URL = relativeUrl;
	}

	public void setPluginName(String pluginName) {
		PLUGIN_NAME = pluginName;
	}

	public void setRequestParams(ICommonParameterProvider requestParams) {
		this.requestParams = requestParams;
	}

	public DashboardGenerator() {
	}

	protected String RELATIVE_URL;

	protected String PLUGIN_NAME;

	/*
	 * An outer template consists of html snippets that are wrapped
	 * around many dashboards for a common appearance.
	 * examples are:
	 * template-dashboard.html
	 * template-dashboard-clean.html
	 * template-dashboard-mantle.html
	 */
	protected String templateContent;

	/*
	 * This is the html document containing the dashboard template
	 * default-dashboard-template.html is the fallback
	 */
	protected String dashboardContent;

	protected ICommonParameterProvider requestParams; 

	protected static final Log logger = LogFactory.getLog(DashboardGenerator.class);

	protected ArrayList<String> tagsList = new ArrayList<String>();	

	protected String intro;

	protected String footer;

	protected String encoding;

	protected Locale locale = Locale.getDefault();

	protected String output = "";

	@Autowired
	protected DashboardResourceManager resourceManager;

	public void init() throws IOException{
		resourceManager.init();
	};

	public final void generateHtmlOutput(OutputStream out) throws Exception{

		updateUserLanguageKey();

		processTemplateI18nTags();

		splitTemplate();

		mergeMessages();

		buildIntro();

		buildHeaders();

		buildContext();

		buildStorage();

		buildContent();

		buildFooter();

		out.write(output.getBytes(encoding));

	}

	protected abstract void buildIntro() throws InvalidTemplateException;

	// Merge dashboard related message file with global message file 
	// and save it in the dashboard cache
	protected abstract void mergeMessages();

	//Split the template into intro/footer and replace/remove some tags
	protected abstract void splitTemplate();

	// Fill the template with the correct user locale
	protected abstract void updateUserLanguageKey();

	// Process i18n on dashboard outer template
	protected abstract void processTemplateI18nTags();

	protected abstract void buildHeaders() throws Exception;

	protected abstract void buildContext();

	protected abstract void buildStorage();

	// Process i18n for each line of the dashboard output
	protected abstract void processI18nTags();

	protected abstract void buildFooter();

	protected abstract void buildContent();



}
