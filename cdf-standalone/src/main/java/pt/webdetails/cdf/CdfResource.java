package pt.webdetails.cdf;

import java.io.Serializable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

@Component
@Path("/cdf")
public class CdfResource implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@GET
	@Produces({"text/html" })
	@Path("/RenderXCDF")
	public String renderXcdf(){

		return null;

	}

	@GET
	@Produces({"text/html" })
	@Path("/RenderHTML")
	public String renderHtml(){
			
		return null;

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
	
}
