package org.pentaho.cdf;

import pt.webdetails.cpf.PluginSettings;

public class CdfPluginSettings extends PluginSettings {

    private CdfPluginSettings() {}

	private static CdfPluginSettings settings = new CdfPluginSettings();
    
	@Override
    public String getPluginName() {
      return "pentaho-cdf";
    }
    
    static CdfPluginSettings getSettings(){
        return settings;
      }

	public String getDownloadableFormats() {
		return getStringSetting("settings/resources/downloadable-formats", null);
	}

}
