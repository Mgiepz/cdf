package org.pentaho.cdf;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import pt.webdetails.cpf.http.CommonParameterProvider;
import pt.webdetails.cpf.http.ICommonParameterProvider;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-application-context.xml"})
public class SimpleDashboardGeneratorTest {

	private String PLUGIN_NAME = "pentaho-cdf";
	
	private String RELATIVE_URL = "/content/";

	@Autowired
	private DashboardGenerator dashboardGenerator;
	
	
	@Before
	public void setUp() throws Exception {

		String testDashboard = null;
		String testTemplate = null;
		
		when(dashboardGenerator.resourceManager.getStringContent("test-template")).thenReturn(testTemplate);
		when(dashboardGenerator.resourceManager.getStringContent("test-dashboard")).thenReturn(testDashboard);
		
	}

	@Test
	public void testGenerateHtmlOutput() throws Exception {
	
		ICommonParameterProvider requestParams = new CommonParameterProvider();
		requestParams.put("template", "test-template");
		requestParams.put("dashboard", "test-dashboard");

		dashboardGenerator.setPluginName(PLUGIN_NAME);
		dashboardGenerator.setRelativeUrl(RELATIVE_URL);
		dashboardGenerator.setRequestParams(requestParams);
		dashboardGenerator.init();
		
		OutputStream out = new ByteArrayOutputStream();
		dashboardGenerator.generateHtmlOutput(out);

	}
	
    @After
    public void tearDown() throws Exception {
        reset(dashboardGenerator.resourceManager);
    }

}
