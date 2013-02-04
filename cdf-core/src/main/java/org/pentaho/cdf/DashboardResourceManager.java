package org.pentaho.cdf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;

import pt.webdetails.cpf.repository.IRepositoryAccess;
import pt.webdetails.packager.Packager;

/**
 * This class encapsulates handling of resource files
 * like blueprint and scripts
 * @author mg
 *
 */
public class DashboardResourceManager {
	
	/*
	 * Provide access to the applications repository, using an
	 * implementation of the cpf-api
	 * This should be injected
	 */
	
	@Autowired
	private IRepositoryAccess repository;
	
	protected Packager packager;
	
	private String RELATIVE_URL_TAG;

	private Map<String,Properties> resourceIncludes;

	//path from the repository root, i.e. "system/pentaho-cdf"
	private String rootdir;
	
	/**
	 * On init we register resources with the packager and 
	 * store the content of the resources in a static Hashmap
	 */
	protected void init() throws FileNotFoundException, IOException {

		final Properties blueprintResources = new Properties();
		blueprintResources.load(repository.getResourceInputStream(rootdir + "/resources-blueprint.txt"));  
		resourceIncludes.put("blueprint", blueprintResources);
				
		final Properties mobileResources = new Properties();
		mobileResources.load(repository.getResourceInputStream(rootdir + "/resources-mobile.txt"));
		resourceIncludes.put("mobile", mobileResources);
		
		ArrayList<String> scriptsList = new ArrayList<String>();
		ArrayList<String> stylesList = new ArrayList<String>();

		this.packager = Packager.getInstance();

		boolean scriptsAvailable = packager.isPackageRegistered("scripts");
		boolean stylesAvailable = packager.isPackageRegistered("styles");
		boolean mobileScriptsAvailable = packager.isPackageRegistered("scripts-mobile");
		boolean mobileStylesAvailable = packager.isPackageRegistered("styles-mobile");
		if (!scriptsAvailable) {
			scriptsList.clear();
			scriptsList.addAll(Arrays.asList(blueprintResources.get("commonLibrariesScript").toString().split(",")));
			for (int i = 0; i < scriptsList.size(); i++) {
				String fname = scriptsList.get(i);
				scriptsList.set(i, fname.replaceAll(RELATIVE_URL_TAG + "/content/pentaho-cdf", ""));
			}
			packager.registerPackage("scripts", Packager.Filetype.JS, rootdir, rootdir + "/js/scripts.js", scriptsList.toArray(new String[scriptsList.size()]));
		}

		if (!stylesAvailable) {
			stylesList.clear();
			stylesList.addAll(Arrays.asList(blueprintResources.get("commonLibrariesLink").toString().split(",")));
			for (int i = 0; i < stylesList.size(); i++) {
				String fname = stylesList.get(i);
				stylesList.set(i, fname.replaceAll(RELATIVE_URL_TAG + "/content/pentaho-cdf", ""));
			}
			packager.registerPackage("styles", Packager.Filetype.CSS, rootdir, rootdir + "/js/styles.css", stylesList.toArray(new String[stylesList.size()]));
		}
		if (!mobileScriptsAvailable) {
			scriptsList.clear();
			scriptsList.addAll(Arrays.asList(mobileResources.get("commonLibrariesScript").toString().split(",")));
			for (int i = 0; i < scriptsList.size(); i++) {
				String fname = scriptsList.get(i);
				scriptsList.set(i, fname.replaceAll(RELATIVE_URL_TAG + "/content/pentaho-cdf", ""));
			}
			packager.registerPackage("scripts-mobile", Packager.Filetype.JS, rootdir, rootdir + "/js/scripts-mobile.js", scriptsList.toArray(new String[scriptsList.size()]));
		}

		if (!mobileStylesAvailable) {
			stylesList.clear();
			stylesList.addAll(Arrays.asList(mobileResources.get("commonLibrariesLink").toString().split(",")));
			for (int i = 0; i < stylesList.size(); i++) {
				String fname = stylesList.get(i);
				stylesList.set(i, fname.replaceAll(RELATIVE_URL_TAG + "/content/pentaho-cdf", ""));
			}
			packager.registerPackage("styles-mobile", Packager.Filetype.CSS, rootdir, rootdir + "/js/styles-mobile.css", stylesList.toArray(new String[stylesList.size()]));
		}

	}

	public void setRepository(IRepositoryAccess repository) {
		this.repository = repository;
	}

	public String getMinifiedPackage(String id) {
		return packager.minifyPackage(id);
	}

	public Properties getResourceIncludes(String id) throws IOException {
		return resourceIncludes.get(id);
	}

	public String getStringContent(String fileName) throws IOException {
		
		return repository.getResourceAsString(fileName);
	
	}

}
