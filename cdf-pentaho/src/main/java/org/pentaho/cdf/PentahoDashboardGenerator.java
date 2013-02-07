package org.pentaho.cdf;

import java.io.IOException;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IUITemplater;
import org.pentaho.platform.engine.core.solution.ActionInfo;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import pt.webdetails.cpf.http.ICommonParameterProvider;
import pt.webdetails.cpf.repository.PentahoRepositoryAccess;

public class PentahoDashboardGenerator extends SimpleDashboardGenerator {
	

	@Override
	protected void splitTemplate() {

		final IUITemplater templater = PentahoSystem.get(IUITemplater.class, userSession);
		if (templater != null) {
			templater.processTemplate(templateContent, "", userSession);
		}
		
		super.splitTemplate();
	
	}

	public static final String SOLUTION_DIR = "cdf";
	private IPentahoSession userSession;

	@Override
	public void init() throws IOException {
		
		String rootDir = "system/" + PLUGIN_NAME;

		String solution = requestParams.getStringParameter("solution", null);
		String path = requestParams.getStringParameter("path", null);
		String template = requestParams.getStringParameter("templateName", null);
		String dashboard = requestParams.getStringParameter("dashboardName", null);
	
        if (template == null || template.equals("")) {
            template = "";
        } else {
            template = "-" + template;
        }

        String fullDashboardPath = null;

        if (dashboard != null) {
            if (dashboard.startsWith("/") || dashboard.startsWith("\\")) { //$NON-NLS-1$ //$NON-NLS-2$
                fullDashboardPath = dashboard;
            } else {
                fullDashboardPath = ActionInfo.buildSolutionPath(solution, path, dashboard);
            }
        }
		
		if (fullDashboardPath == null ||  !PentahoRepositoryAccess.getRepository().resourceExists(fullDashboardPath) ) {
        	fullDashboardPath = rootDir + "/default-dashboard-template.html";     
        }
        
		this.dashboardContent = resourceManager.getStringContent(fullDashboardPath);	
		
        final String dashboardTemplate = "template-dashboard" + template + ".html"; //$NON-NLS-1$
       
        String templatePath = SOLUTION_DIR + "/templates/" + dashboardTemplate;
        
        if (!PentahoRepositoryAccess.getRepository().resourceExists(templatePath)) {
        	templatePath = rootDir + "/" + dashboardTemplate;
        }
		
		this.templateContent = resourceManager.getStringContent(templatePath);

	}

	public PentahoDashboardGenerator() {
		super();
	}

	public PentahoDashboardGenerator(String relativeUrl, String pluginName,
			ICommonParameterProvider requestParams) throws IOException {
	}
	
	@Override
	protected void buildContext() {
//        DashboardContext context = new DashboardContext(userSession);
//        output += context.getContext(requestParams);
	}

//	@Override
//	protected void buildStorage() {
//
//        final StringBuilder s = new StringBuilder();
//        s.append("\n<script language=\"javascript\" type=\"text/javascript\">\n");
//        s.append("  Dashboards.storage = ");
//        
//        try {
//			s.append(StorageEngine.getInstance().read(requestParams, userSession)).append("\n");
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//        
//        s.append("</script>\n");
//
//        output += s.toString();
//
//	}

	public void setSession(IPentahoSession userSession) {
		this.userSession = userSession;
		
	}


}
