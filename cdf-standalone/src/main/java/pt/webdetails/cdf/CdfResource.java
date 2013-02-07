package pt.webdetails.cdf;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.Locale;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.pentaho.cdf.DashboardGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pt.webdetails.cpf.http.CommonParameterProvider;
import pt.webdetails.cpf.http.ICommonParameterProvider;

@Component
@Path("/cdf")
public class CdfResource implements Serializable{

	private static final String ENCODING = "utf-8";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String PLUGIN_NAME = "cdf";

	@Autowired
	private DashboardGenerator dashboardGenerator; 
	
	
	@GET
	@Produces({"text/html" })
	@Path("/RenderXCDF")
	public String renderXcdf(){

		return null;

	}

	@GET
	@Produces({"text/html" })
	@Path("/{dashboard}/html")
	public String renderHtml(@PathParam("dashboard") String dashboard) throws Exception{

		ICommonParameterProvider requestParams = new CommonParameterProvider();
		requestParams.put("templateName", "template");
		requestParams.put("dashboardName", dashboard);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		renderHtmlDashboard(requestParams, baos);
		return baos.toString(ENCODING);

	}

	@GET
	@Produces({"application/json" })
	@Path("/JSONSolution")
	public String getJSONSolution(){
			
		return null;

	}

	@GET
	@Path("/GetCDFResource")
	public String getCdfResource(){
			
		//we need to set the proper contenttype here
		return null;

	}
	
	/**
	 * This method renders the html output using a DashboardContentBuilder
	 * 
	 * @param requestParams
	 * @param out
	 * @throws Exception
	 */
	public void renderHtmlDashboard(final ICommonParameterProvider requestParams, final OutputStream out) throws Exception {

		dashboardGenerator.setPluginName(PLUGIN_NAME);
		dashboardGenerator.setRelativeUrl("");
		dashboardGenerator.setRequestParams(requestParams);
		dashboardGenerator.setEncoding(ENCODING);
		dashboardGenerator.setLocale(Locale.getDefault());
		dashboardGenerator.init();
		dashboardGenerator.generateHtmlOutput(out);

	}
	
}
