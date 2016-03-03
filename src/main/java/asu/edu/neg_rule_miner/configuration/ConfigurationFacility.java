package asu.edu.neg_rule_miner.configuration;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.varia.NullAppender;

import asu.edu.neg_rule_miner.RuleMinerException;

public class ConfigurationFacility {

	private static Configuration instance;


	public synchronized static Configuration getConfiguration(){

		if(instance == null){
			BasicConfigurator.configure(new NullAppender());

			Configuration config = null;
			try {
				config = new XMLConfiguration(Constant.CONF_FILE);
			} catch (ConfigurationException e) {
				throw new RuleMinerException("Unable to read conf file at ''"+
						Constant.CONF_FILE, e);
			}
			instance = config;
			//read the logger properties
			String logFile = config.getString(Constant.CONF_LOGGER);
			if(logFile != null)
				PropertyConfigurator.configure(logFile);
		}
		return instance;

	}

}
