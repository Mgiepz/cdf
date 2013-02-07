package org.pentaho.cdf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.pentaho.cdf.comments.CommentsEngine;
import org.pentaho.cdf.export.Export;
import org.pentaho.cdf.export.ExportCSV;
import org.pentaho.cdf.export.ExportExcel;
import org.pentaho.cdf.storage.StorageEngine;
import org.pentaho.cdf.views.ViewManager;
import org.pentaho.platform.api.engine.ILogger;
import org.pentaho.platform.api.engine.IMimeTypeListener;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.engine.core.solution.ActionInfo;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.platform.util.xml.dom4j.XmlDom4JHelper;

import pt.webdetails.cpf.SpringEnabledContentGenerator;
import pt.webdetails.cpf.WrapperUtils;
import pt.webdetails.cpf.annotations.AccessLevel;
import pt.webdetails.cpf.annotations.Exposed;
import pt.webdetails.cpf.audit.CpfAuditHelper;
import pt.webdetails.cpf.http.ICommonParameterProvider;
import pt.webdetails.cpf.repository.BaseRepositoryAccess.FileAccess;
import pt.webdetails.cpf.repository.PentahoRepositoryAccess;

/**
 * This is the main class of the CDF plugin. It handles all requests to
 * /pentaho/content/pentaho-cdf. These requests include:
 * <p/>
 * - JSONSolution - GetCDFResource - .xcdf requests - js files 
 *
 * @author Will Gorman (wgorman@pentaho.com)
 * @author Marius Giepz (mgiepz@gmail.com) refactoring for standalone
 */
public class CdfContentGenerator extends SpringEnabledContentGenerator{

	private static final long serialVersionUID = 5608691656289862706L;
	private static final Log logger = LogFactory.getLog(CdfContentGenerator.class);
	public static final String PLUGIN_NAME = "pentaho-cdf"; //$NON-NLS-1$
	private static final String MIMETYPE = "text/html"; //$NON-NLS-1$
	public static final String SOLUTION_DIR = "cdf";

	public static String ENCODING = "UTF-8";

	/*
	 * This block initializes exposed methods
	 */
	private static Map<String, Method> exposedMethods = new HashMap<String, Method>();

	static
	{
		//to keep case-insensitive methods
		logger.info("loading exposed methods");
		exposedMethods = getExposedMethods(CdfContentGenerator.class, true);
	}

	@Override
	protected Method getMethod(String methodName) throws NoSuchMethodException
	{
		Method method = exposedMethods.get(StringUtils.lowerCase(methodName));
		if (method == null)
		{
			throw new NoSuchMethodException();
		}
		return method;
	}

	public CdfContentGenerator() {}

	@Override
	public String getPluginName() {
		return PLUGIN_NAME;
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void renderXcdf(final OutputStream out) throws Exception {

		IParameterProvider requestParams = getRequestParameters();

		long start = System.currentTimeMillis();

		UUID uuid = CpfAuditHelper.startAudit(PLUGIN_NAME, requestParams.getParameter("action").toString(), getObjectName(), this.userSession, this, requestParams);

		try {
			final IMimeTypeListener mimeTypeListener = outputHandler.getMimeTypeListener();
			if (mimeTypeListener != null) {
				mimeTypeListener.setMimeType(MIMETYPE);
			}

			renderXCDFDashboard(WrapperUtils.wrapParamProvider(requestParams), out);

			long end = System.currentTimeMillis();
			CpfAuditHelper.endAudit(PLUGIN_NAME, requestParams.getParameter("action").toString(), getObjectName(), this.userSession, this, start, uuid, end);

		} catch (Exception e) {
			long end = System.currentTimeMillis();
			CpfAuditHelper.endAudit(PLUGIN_NAME, requestParams.getParameter("action").toString(), getObjectName(), this.userSession, this, start, uuid, end);
			throw e;
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void jsonSolution(final OutputStream out) throws JSONException, ParserConfigurationException {

		IParameterProvider requestParams = getRequestParameters();

		if (requestParams == null) {
			error(Messages.getErrorString("CdfContentGenerator.ERROR_0004_NO_REQUEST_PARAMS")); //$NON-NLS-1$
			throw new InvalidParameterException(Messages.getString("CdfContentGenerator.ERROR_0017_NO_REQUEST_PARAMS")); //$NON-NLS-1$
		}

		final String solution = requestParams.getStringParameter("solution", null); //$NON-NLS-1$
		final String path = requestParams.getStringParameter("path", null); //$NON-NLS-1$
		final String mode = requestParams.getStringParameter("mode", null); //$NON-NLS-1$
		final String contextPath = ((HttpServletRequest) parameterProviders.get("path").getParameter("httprequest")).getContextPath();
		final NavigateComponent nav = new NavigateComponent(userSession, contextPath);
		final String json = nav.getNavigationElements(mode, solution, path);

		final PrintWriter pw = new PrintWriter(out);

		String callback = requestParams.getStringParameter("callback", null);
		if (callback != null) {
			pw.println(callback + "(" + json + ");");

		} else {
			pw.println(json);
		}

		pw.flush();
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void renderHtml(final OutputStream out) throws Exception {

		IParameterProvider requestParams = getRequestParameters();

		final IMimeTypeListener mimeTypeListener = outputHandler.getMimeTypeListener();
		if (mimeTypeListener != null) {
			mimeTypeListener.setMimeType(MIMETYPE);
		}

		ICommonParameterProvider wrappedParamProvider = WrapperUtils.wrapParamProvider(requestParams);

		wrappedParamProvider.put("template", requestParams.getStringParameter("template", null));
		wrappedParamProvider.put("dashboard", requestParams.getStringParameter("dashboard", "template.html"));
		wrappedParamProvider.put("messages", requestParams.getStringParameter("messages", null));

		renderHtmlDashboard(wrappedParamProvider, out);
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void exportFile(final OutputStream output) {

		IParameterProvider requestParams = getRequestParameters();

		try {

			final ByteArrayOutputStream out = new ByteArrayOutputStream();

			final ServiceCallAction serviceCallAction = ServiceCallAction.getInstance();
			if (serviceCallAction.execute(requestParams, userSession, out)) {

				final String exportType = requestParams.getStringParameter("exportType", "excel");

				Export export;

				if (exportType.equals("csv")) {
					export = new ExportCSV(output);
					setResponseHeaders(MimeType.PLAIN_TEXT, 0, "export" + export.getExtension());
				} else {
					export = new ExportExcel(output);
					setResponseHeaders(MimeType.XLS, 0, "export" + export.getExtension());
				}

				export.exportFile(new JSONObject(out.toString()));
			}

		} catch (IOException e) {
			logger.error("IOException  exporting file", e);
		} catch (JSONException e) {
			logger.error("JSONException exporting file", e);
		}

	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void cdfSettings(final OutputStream out) {

		IParameterProvider requestParams = getRequestParameters();

		final String method = requestParams.getStringParameter("method", null);
		final String key = requestParams.getStringParameter("key", null);

		if (method.equals("set")) {
			CdfSettings.getInstance().setValue(key, requestParams.getParameter("value"), userSession);
		} else {
			final Object value = CdfSettings.getInstance().getValue(key, userSession);
			final PrintWriter pw = new PrintWriter(out);
			pw.println(value != null ? value.toString() : "");
			pw.flush();
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void callAction(final OutputStream out) {

		IParameterProvider requestParams = getRequestParameters();

		final ServiceCallAction serviceCallAction = ServiceCallAction.getInstance();
		serviceCallAction.execute(requestParams, userSession, out);
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void processComments(final OutputStream out) throws JSONException {

		IParameterProvider requestParams = getRequestParameters();

		String result;

		try {

			final CommentsEngine commentsEngine = CommentsEngine.getInstance();
			result = commentsEngine.process(requestParams, userSession);

		} catch (InvalidCdfOperationException ex) {

			final String errMessage = ex.getCause().getClass().getName() + " - " + ex.getMessage();
			logger.error("Error processing comment: " + errMessage);
			final JSONObject json = new JSONObject();
			json.put("error", errMessage);
			result = json.toString(2);

		}

		final PrintWriter pw = new PrintWriter(out);
		pw.println(result);
		pw.flush();

	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void processStorage(final OutputStream out) throws JSONException {

		IParameterProvider requestParams = getRequestParameters();

		String result;

		try {

			final StorageEngine storagesEngine = StorageEngine.getInstance();
			result = storagesEngine.process(requestParams, userSession);

		} catch (InvalidCdfOperationException ex) {

			final String errMessage = ex.getCause().getClass().getName() + " - " + ex.getMessage();
			logger.error("Error processing storage: " + errMessage);
			final JSONObject json = new JSONObject();
			json.put("error", errMessage);
			result = json.toString(2);

		}

		final PrintWriter pw = new PrintWriter(out);
		pw.println(result);
		pw.flush();

	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void generateContext(final OutputStream out) throws Exception {

		IParameterProvider requestParams = getRequestParameters();

		DashboardContext context = new DashboardContext(userSession);
		String sContext = context.getContext(WrapperUtils.wrapParamProvider(requestParams));

		out.write(sContext.getBytes(ENCODING));
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void views(final OutputStream out) {

		IParameterProvider requestParams = getRequestParameters();
		IParameterProvider pathParams = getPathParameters();

		ViewManager man = ViewManager.getInstance();
		man.process(requestParams, pathParams, out);
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void clearCache(final OutputStream out) {

		try {
			DashboardContext.clearCache();
			out.write("Cache cleared".getBytes("utf-8"));
		} catch (IOException e) {
			logger.error("failed to clear CDFcache");
		}
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getHeaders(final OutputStream out) throws Exception {

		IParameterProvider requestParams = getRequestParameters();

		String dashboard = requestParams.getStringParameter("dashboardContent", "");
		//		service.getHeaders(dashboard, WrapperUtils.wrapParamProvider(requestParams), out); //here we must provide a static method in 
	}

	@Exposed(accessLevel = AccessLevel.PUBLIC)
	public void getCDFResource(final OutputStream out) throws Exception {

		IParameterProvider requestParams = getRequestParameters();

		if (requestParams == null) {
			error(Messages.getErrorString("CdfContentGenerator.ERROR_0004_NO_REQUEST_PARAMS")); //$NON-NLS-1$
			throw new InvalidParameterException(Messages.getString("CdfContentGenerator.ERROR_0017_NO_REQUEST_PARAMS")); //$NON-NLS-1$
		}

		final String resource = requestParams.getStringParameter("resource", null); //$NON-NLS-1$

		//TODO:
		//Setting always the correct mimetype should be facilitated somehow
		//		getContentItem().setMimeType(MimeHelper.getMimeTypeFromFileName(resource));

		final HttpServletResponse response = (HttpServletResponse) parameterProviders.get("path").getParameter("httpresponse");
		try {
			getSolutionFile(resource, out, this);
		} catch (SecurityException e) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
		}
	}

	/**
	 * This method derives metadata information from the xcdf definition and
	 * passes it down to the html renderer.
	 * In an alternative server environment we might call renderHtml directly,
	 * if we get the metadata from somewhere else (i.e. Saiku-Repository)
	 * 
	 * @param requestParams
	 * @param out
	 * @throws Exception
	 */
	public void renderXCDFDashboard(final ICommonParameterProvider requestParams, final OutputStream out) throws Exception {

		final String solution = requestParams.getStringParameter("solution", null); //$NON-NLS-1$
		final String path = requestParams.getStringParameter("path", null); //$NON-NLS-1$
		final String action = requestParams.getStringParameter("action", null); //$NON-NLS-1$
		String templateName = requestParams.getStringParameter("template", null); //$NON-NLS-1$

		final String fullPath = ActionInfo.buildSolutionPath(solution, path, action);
		// Check for access permissions
		if (!PentahoRepositoryAccess.getRepository().hasAccess(fullPath, FileAccess.EXECUTE)){
			out.write("Access Denied".getBytes(ENCODING));
			return;
		}

		String dashboardName = null;
		String messagesBaseFilename = null;

		if (PentahoRepositoryAccess.getRepository().resourceExists(fullPath)) {
			final String dashboardMetadata = PentahoRepositoryAccess.getRepository().getResourceAsString(fullPath, FileAccess.EXECUTE);
			final Document doc = DocumentHelper.parseText(dashboardMetadata);
			dashboardName = XmlDom4JHelper.getNodeText("/cdf/template", doc, "");

			// Get message file base name if any
			if (doc.selectSingleNode("/cdf/messages") != null) {
				messagesBaseFilename = XmlDom4JHelper.getNodeText("/cdf/messages", doc);
			}

			// If a "style" tag exists, use that one
			if (doc.selectSingleNode("/cdf/style") != null) {
				templateName = XmlDom4JHelper.getNodeText("/cdf/style", doc);
			}
		}

		requestParams.put("templateName", templateName);
		requestParams.put("dashboardName", dashboardName); //TODO: Stupid naming!!!
		requestParams.put("messagesBaseFilename", messagesBaseFilename);

		renderHtmlDashboard(requestParams, out);

	}

	/**
	 * This method renders the html output using a DashboardContentBuilder
	 * 
	 * @param requestParams
	 * @param out
	 * @throws Exception
	 */
	public void renderHtmlDashboard(final ICommonParameterProvider requestParams, final OutputStream out) throws Exception {

		DashboardGenerator dashboardGenerator = getDashboardGenerator(requestParams);
		
		dashboardGenerator.generateHtmlOutput(out);
		
		setResponseHeaders(MimeType.HTML, 0, null);

	}

	private DashboardGenerator getDashboardGenerator(
			final ICommonParameterProvider requestParams) throws IOException {
		PentahoDashboardGenerator dashboardGenerator = (PentahoDashboardGenerator) pluginContext.getBean("dashboardGenerator");

		dashboardGenerator.setPluginName(PLUGIN_NAME);
		dashboardGenerator.setRelativeUrl(getRelativeUrl());
		dashboardGenerator.setRequestParams(requestParams);
		dashboardGenerator.setEncoding("utf-8");
		dashboardGenerator.setLocale(LocaleHelper.getLocale());
		dashboardGenerator.setSession(this.userSession);
		dashboardGenerator.init();
		return dashboardGenerator;
		
	}

	private String getRelativeUrl() {

		String relativeUrl = null;

		try{

			if (parameterProviders.get("path") != null
					&& parameterProviders.get("path").getParameter("httprequest") != null
					&& ((HttpServletRequest) parameterProviders.get("path").getParameter("httprequest")).getContextPath() != null) {
				relativeUrl = ((HttpServletRequest) parameterProviders.get("path").getParameter("httprequest")).getContextPath();
			} else {
				relativeUrl = getBaseUrl();
				/* If we detect an empty string, things will break.
				 * If we detect an absolute url, things will *probably* break.
				 * In either of these cases, we'll resort to Catalina's context,
				 * and its getContextPath() method for better results.
				 */
				if ("".equals(relativeUrl) || relativeUrl.matches("^http://.*")) {
					Object context = PentahoSystem.getApplicationContext().getContext();
					Method getContextPath = context.getClass().getMethod("getContextPath", null);
					if (getContextPath != null) {
						relativeUrl = getContextPath.invoke(context, null).toString();
					}
				}
			}

			if (relativeUrl.endsWith("/")) {
				relativeUrl = relativeUrl.substring(0, relativeUrl.length() - 1);
			}

			return relativeUrl;

		} catch (Exception e) {
			logger.error("Error creating cdf content: ", e);
		}

		return relativeUrl;
	}

	public void getSolutionFile(final String resourcePath, final OutputStream out, final ILogger logger) throws Exception {

		final String formats = CdfPluginSettings.getSettings().getDownloadableFormats();

		List<String> allowedFormats = Arrays.asList(StringUtils.split(formats, ','));
		String extension = resourcePath.replaceAll(".*\\.(.*)", "$1");
		if (allowedFormats.indexOf(extension) < 0) {
			// We can't provide this type of file
			throw new SecurityException("Not allowed");
		}

		InputStream in =  PentahoRepositoryAccess.getRepository().getResourceInputStream(resourcePath,FileAccess.EXECUTE);

		try {
			IOUtils.copy(in, out);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private static String getBaseUrl() {

		String baseUrl;
		try {

			URI uri = new URI(PentahoSystem.getApplicationContext().getFullyQualifiedServerURL());
			baseUrl = uri.getPath();
			if (!baseUrl.endsWith("/")) {
				baseUrl += "/";
			}
		} catch (URISyntaxException ex) {
			logger.fatal("Error building BaseURL from " + PentahoSystem.getApplicationContext().getFullyQualifiedServerURL(), ex);
			baseUrl = "";
		}

		return baseUrl;

	}

}
