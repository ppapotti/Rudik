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
	public static String CONF_VALIDATION_THRESHOLD = "naive.runtime.score.validation_threshold";
	public static String CONF_SCORE_ALPHA = "naive.runtime.score.alpha";
	public static String CONF_SCORE_BETA= "naive.runtime.score.beta";
	public static String CONF_SCORE_GAMMA = "naive.runtime.score.gamma";
	public static String CONF_MAX_RULE_LEN = "naive.runtime.max_rule_lenght";
	public static String CONF_SPARQL_ENGINE = "naive.sparql";


	public static String CONF_EQUALITY_TYPES_NUMBER = "naive.disequality_relation.equality_types_number";


}
