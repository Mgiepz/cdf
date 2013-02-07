package org.pentaho.cdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

public class SimpleDashboardGenerator extends DashboardGenerator {
	
	public SimpleDashboardGenerator() {	}

	public void init() throws IOException{
		this.resourceManager.init();

		String template = requestParams.getStringParameter("templateName", null);
		String dashboard = requestParams.getStringParameter("dashboardName", null);
		
		this.templateContent = resourceManager.getStringContent(template);
		this.dashboardContent = resourceManager.getStringContent(dashboard);		
	
	}
	
	@Override
	public void updateUserLanguageKey() {
		if (logger.isDebugEnabled()) {
			logger.debug("Current user locale: " + locale.getLanguage());
		}
		templateContent = templateContent.replaceAll("#\\{LANGUAGE_CODE\\}", locale.getLanguage());
	}

	@Override
	public void processTemplateI18nTags() {   
		templateContent = processI18NInternal(templateContent);		
	}

	@Override
	public void processI18nTags() {   

		BufferedReader reader = new BufferedReader(new StringReader(dashboardContent));

		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				// Process i18n for each line of the dashboard output
				line = processI18NInternal(line);
				// Process i18n - end
				sb.append(line).append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		dashboardContent = sb.toString();
	}	

	private String processI18NInternal(String content) {

		String tagPattern = "CDF.i18n\\(\"";
		String[] test = content.split(tagPattern);
		if (test.length == 1) {
			return content;
		}
		StringBuilder resBuffer = new StringBuilder();
		int i;
		String tagValue;
		resBuffer.append(test[0]);
		for (i = 1; i < test.length; i++) {

			// First tag is processed differently that other because is the only case where I don't
			// have key in first position
			resBuffer.append("<span id=\"");
			if (i != 0) {
				// Right part of the string with the value of the tag herein
				tagValue = test[i].substring(0, test[i].indexOf("\")"));
				tagsList.add(tagValue);
				resBuffer.append(updateSelectorName(tagValue));
				resBuffer.append("\"></span>");
				resBuffer.append(test[i].substring(test[i].indexOf("\")") + 2, test[i].length()));
			}
		}
		return resBuffer.toString();
	}

	private String updateSelectorName(String name) {
		// If we've the character . in the message key substitute it conventionally to _
		// when dynamically generating the selector name. The "." character is not permitted in the
		// selector id name
		return name.replace(".", "_");
	}



	@Override
	protected void buildContext() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void buildStorage() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void splitTemplate() {

		//In the pentaho implementation we also need to call the templater before
		//to replace all sorts of tags. in the simple implementation we will only 
		//remove them

		intro = "";
		footer = "";

		String token = "{content}"; //$NON-NLS-1$
		int index = templateContent.indexOf(token);
		if (index == -1) {
			intro = templateContent ;
		} else {
			intro = templateContent.substring(0, index);
			footer = templateContent.substring(index + token.length());
		}

	}

	@Override
	protected void mergeMessages() {
		intro = intro.replaceAll("\\{load\\}", "onload=\"load()\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		intro = intro.replaceAll("\\{body-tag-unload\\}", "");
		intro = intro.replaceAll("#\\{GLOBAL_MESSAGE_SET_NAME\\}", ""); //TODO:
		intro = intro.replaceAll("#\\{GLOBAL_MESSAGE_SET_PATH\\}", ""); //TODO:
		intro = intro.replaceAll("#\\{GLOBAL_MESSAGE_SET\\}", buildMessageSetCode(tagsList));
	}

	protected String buildMessageSetCode(ArrayList<String> tagsList) {
		StringBuilder messageCodeSet = new StringBuilder();
		for (String tag : tagsList) {
			messageCodeSet.append("\\$('#").append(updateSelectorName(tag)).append("').html(jQuery.i18n.prop('").append(tag).append("'));\n");
		}
		return messageCodeSet.toString();
	}

	@Override
	protected void buildIntro() throws InvalidTemplateException {
		int indexOfHead = intro.indexOf("<head>");
		if(indexOfHead>0){
			output += intro.substring(0, indexOfHead + 6);	
		}else{
			throw new InvalidTemplateException("Template does not have a head-element");
		}
	}

	@Override
	protected void buildHeaders() throws Exception{

		final String dashboardType = requestParams.getStringParameter("dashboardType", "blueprint");
		final String scheme = requestParams.hasParameter("scheme") ? requestParams.getStringParameter("scheme", "") : "http";
		final String suffix;

		/*
		 * depending on the dashboard type, the minification engine and its file
		 * set will vary.
		 */

		if (dashboardType.equals("mobile")) {
			suffix = "-mobile";
		} else {
			suffix = "";
		}
	
		HashMap<String, String> includes = new HashMap<String, String>();
		
		final Properties resources = resourceManager.getResourceIncludes(dashboardType);

		final ArrayList<String> miniscripts = new ArrayList<String>();
		final ArrayList<String> ministyles = new ArrayList<String>();

		final ArrayList<String> scripts = new ArrayList<String>();
		final ArrayList<String> styles = new ArrayList<String>();

		miniscripts.addAll(Arrays.asList(resources.getProperty("commonLibrariesScript", "").split(",")));
		ministyles.addAll(Arrays.asList(resources.getProperty("commonLibrariesLink", "").split(",")));
		scripts.addAll(getExtraScripts(dashboardContent, resources));
		styles.addAll(getExtraStyles(dashboardContent, resources));
		styles.addAll(Arrays.asList(resources.getProperty("style", "").split(",")));
		StringBuilder scriptsBuilder = new StringBuilder();
		StringBuilder stylesBuilder = new StringBuilder();
		final String absRoot = requestParams.hasParameter("root") ? (scheme.equals("") ? "" : (scheme + "://")) + requestParams.getParameter("root").toString() : "";

		// Add common libraries
		if (requestParams.hasParameter("debug") && requestParams.getParameter("debug").toString().equals("true")) {
			// DEBUG MODE
			for (String header : miniscripts) {
				scriptsBuilder.append("<script type=\"text/javascript\" src=\"").append(header.replaceAll("@RELATIVE_URL@", absRoot + RELATIVE_URL)).append("\"></script>\n");
			}
			for (String header : ministyles) {
				stylesBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(header.replaceAll("@RELATIVE_URL@", absRoot + RELATIVE_URL)).append("\"/>\n");
			}

		} else {
			// NORMAL MODE
			logger.info("[Timing] starting minification: " + (new SimpleDateFormat("HH:mm:ss.SSS")).format(new Date()));
			String stylesHash = resourceManager.getMinifiedPackage("styles" + suffix);
			String scriptsHash = resourceManager.getMinifiedPackage("scripts" + suffix);
			stylesBuilder.append("<link href=\"").append(absRoot).append(RELATIVE_URL).append("/content/").append(PLUGIN_NAME).append("/js/styles")
			.append(suffix).append(".css?version=").append(stylesHash).append("\" rel=\"stylesheet\" type=\"text/css\" />");
			scriptsBuilder.append("<script type=\"text/javascript\" src=\"").append(absRoot).append(RELATIVE_URL).append("/content/").append(PLUGIN_NAME).append("/js/scripts")
			.append(suffix).append(".js?version=").append(scriptsHash).append("\"></script>");
			logger.info("[Timing] finished minification: " + (new SimpleDateFormat("HH:mm:ss.SSS")).format(new Date()));
		}
		// Add extra components libraries

		for (String header : scripts) {
			scriptsBuilder.append("<script type=\"text/javascript\" src=\"").append(header.replaceAll("@RELATIVE_URL@", absRoot + RELATIVE_URL)).append("\"></script>\n");
		}
		for (String header : styles) {
			stylesBuilder.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(header.replaceAll("@RELATIVE_URL@", absRoot + RELATIVE_URL)).append("\"/>\n");
		}

		// Add ie8 blueprint condition
		stylesBuilder.append("<!--[if lte IE 8]><link rel=\"stylesheet\" href=\"").append(absRoot).append(RELATIVE_URL)
		.append("/content/").append(PLUGIN_NAME).append("/js/blueprint/ie.css\" type=\"text/css\" media=\"screen, projection\"><![endif]-->");

		StringBuilder stuff = new StringBuilder();
		includes.put("scripts", scriptsBuilder.toString());
		includes.put("styles", stylesBuilder.toString());
		for (String key : includes.keySet()) {
			stuff.append(includes.get(key));
		}

		output += stuff.toString();

	}

	@Override
	protected void buildContent() {
		 output += "<div id=\"dashboardContent\">";
		 output += dashboardContent;
		 output += "</div>";
	}
	
	@Override
	protected void buildFooter() {
		output += footer;
	}

	protected ArrayList<String> getExtraScripts(String dashboardContentOrig, Properties resources) {

		// Compare this ignoring case
		final String dashboardContent = dashboardContentOrig.toLowerCase();

		ArrayList<String> scripts = new ArrayList<String>();
		boolean all;
		if (dashboardContent == null || StringUtils.isEmpty(dashboardContent)) {
			all = true;
		} else {
			all = false;
		}

		final Enumeration<?> resourceKeys = resources.propertyNames();
		while (resourceKeys.hasMoreElements()) {

			final String scriptkey = (String) resourceKeys.nextElement();

			final String key;

			if (scriptkey.indexOf("Script") != -1 && scriptkey.indexOf("commonLibraries") == -1) {
				key = scriptkey.replaceAll("Script$", "");
			} else {
				continue;
			}

			final int keyIndex = all ? 0 : dashboardContent.indexOf(key.toLowerCase());
			if (keyIndex != -1) {
				if (all || matchComponent(keyIndex, key.toLowerCase(), dashboardContent)) {
					// ugly hack -- if we don't know for sure we need OpenStreetMaps,
					// don't load it!
					if (all && scriptkey.indexOf("mapScript") != -1) {
						continue;
					}
					scripts.addAll(Arrays.asList(resources.getProperty(scriptkey).split(",")));
				}
			}
		}

		return scripts;
	}

	public ArrayList<String> getExtraStyles(String dashboardContentOrig, Properties resources) {
		// Compare this ignoring case
		final String dashboardContent = dashboardContentOrig.toLowerCase();

		ArrayList<String> styles = new ArrayList<String>();
		boolean all;
		if (dashboardContent == null || StringUtils.isEmpty(dashboardContent)) {
			all = true;
		} else {
			all = false;
		}

		if (dashboardContent != null && !StringUtils.isEmpty(dashboardContent)) {
			final Enumeration<?> resourceKeys = resources.propertyNames();
			while (resourceKeys.hasMoreElements()) {

				final String scriptkey = (String) resourceKeys.nextElement();

				final String key;

				if (scriptkey.indexOf("Link") != -1 && scriptkey.indexOf("commonLibraries") == -1) {
					key = scriptkey.replaceAll("Link$", "");
				} else {
					continue;
				}

				final int keyIndex = all ? 0 : dashboardContent.indexOf(key.toLowerCase());
				if (keyIndex != -1) {
					if (matchComponent(keyIndex, key.toLowerCase(), dashboardContent)) {
						styles.addAll(Arrays.asList(resources.getProperty(scriptkey).split(",")));
					}
				}
			}
		}
		return styles;
	}

	protected boolean matchComponent(int keyIndex, final String key, final String content) {

		for (int i = keyIndex - 1; i > 0; i--) {
			if (content.charAt(i) == ':' || content.charAt(i) == '"' || ("" + content.charAt(i)).trim().equals("")) {
				// noinspection UnnecessaryContinue
				continue;
			} else {
				if ((i - 3) > 0 && content.substring((i - 3), i + 1).equals("type")) {
					return true;
				}

				break;
			}
		}

		keyIndex = content.indexOf(key, keyIndex + key.length());
		if (keyIndex != -1) {
			return matchComponent(keyIndex, key, content);
		}

		return false;
	}

}
