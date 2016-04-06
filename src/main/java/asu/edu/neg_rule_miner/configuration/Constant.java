package asu.edu.neg_rule_miner.configuration;

public class Constant {
	public static String CONF_FILE = "src/main/config/Configuration.xml";

	//parameter paths of the conf file
	public static String CONF_LOGGER = "logfile";

	public static String GREATER_EQUAL_REL = ">=";
	public static String LESS_EQUAL_REL = "<=";
	public static String GREATER_REL = ">";
	public static String LESS_REL = "<";
	public static String EQUAL_REL = "=";
	public static String DIFF_REL = "!=";

	public static String CONF_NUM_THREADS = "naive.runtime.num_threads";
	public static String CONF_THRESHOLD = "naive.runtime.threshold";
	public static String CONF_MAX_RULE_LEN = "naive.runtime.max_rule_lenght";
	public static String CONF_SPARQL_ENGINE = "naive.sparql";

}
