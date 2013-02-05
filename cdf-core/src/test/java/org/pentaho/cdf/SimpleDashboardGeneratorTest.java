package org.pentaho.cdf;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pt.webdetails.cpf.http.CommonParameterProvider;
import pt.webdetails.cpf.http.ICommonParameterProvider;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-application-context.xml"})
public class SimpleDashboardGeneratorTest {

	private String PLUGIN_NAME = "cdf-standalone";
	
	private String RELATIVE_URL = "root";

	@Autowired
	private DashboardGenerator dashboardGenerator;
	
	
	@Before
	public void setUp() throws Exception {

		InputStream dashboardIs = this.getClass().getResourceAsStream("default-dashboard-template.html");
		InputStream templateIs = this.getClass().getResourceAsStream("template-dashboard-clean.html");
		InputStream includesIs = this.getClass().getResourceAsStream("resources-blueprint.txt");
		final Properties testResources = new Properties();
		testResources.load(includesIs);  

		when(dashboardGenerator.resourceManager.getStringContent("test-template")).thenReturn(IOUtils.toString(templateIs));
		when(dashboardGenerator.resourceManager.getStringContent("test-dashboard")).thenReturn(IOUtils.toString(dashboardIs));
		when(dashboardGenerator.resourceManager.getResourceIncludes(Mockito.anyString())).thenReturn(testResources);

	}

	@Test
	public void testGenerateHtmlOutput() throws Exception {
	
		ICommonParameterProvider requestParams = new CommonParameterProvider();
		requestParams.put("template", "test-template");
		requestParams.put("dashboard", "test-dashboard");
		requestParams.put("debug", "false");
		
		
		dashboardGenerator.setPluginName(PLUGIN_NAME);
		dashboardGenerator.setRelativeUrl(RELATIVE_URL);
		dashboardGenerator.setRequestParams(requestParams);
		dashboardGenerator.setEncoding("utf-8");
		dashboardGenerator.setLocale(Locale.getDefault());
		dashboardGenerator.init();
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		dashboardGenerator.generateHtmlOutput(out);

		File file = new File("c:\\tmp\\out.html");  
		FileOutputStream fos = new FileOutputStream(file);  
		fos.write(out.toByteArray());  
		fos.flush();  
		fos.close();  

	}
	
    @After
    public void tearDown() throws Exception {
        reset(dashboardGenerator.resourceManager);
    }

}
