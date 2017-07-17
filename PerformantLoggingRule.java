import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.rule.optimizations.AbstractOptimizationRule;
import net.sourceforge.pmd.lang.rule.properties.StringMultiProperty;

import org.jaxen.JaxenException;


/**
 * Check that log.debug, log.trace, log.error, etc... statements are guarded by
 * some test expression on log.isDebugEnabled() or log.isTraceEnabled().
 * 
 * @author Kshitiz Garg
 * 
 */
public class PerformantLoggingRule extends AbstractOptimizationRule implements Rule {

	private static final String GUARD_METHOD_NAME = "isLoggable";
	
    public static final StringMultiProperty LOG_LEVELS = new StringMultiProperty("logLevels", "LogLevels to guard",
            new String[] {}, 1.0f, ',');

    public static final StringMultiProperty GUARD_METHODS = new StringMultiProperty("guardsMethods",
            "method use to guard the log statement", new String[] {}, 2.0f, ',');

    protected Map<String, String> guardStmtByLogLevel = new HashMap<>(10);

    private static final String XPATH_EXPRESSION = "//PrimaryPrefix[ends-with(Name/@Image, 'LOG_LEVEL')]"
            + "[boolean(../descendant::AdditiveExpression/@Image = '+')]\n"
            + "[count(ancestor::IfStatement/Expression/descendant::PrimaryExpression["
            + "ends-with(descendant::PrimaryPrefix/Name/@Image,'GUARD')]) = 0]";

    public PerformantLoggingRule() {
        definePropertyDescriptor(LOG_LEVELS);
        definePropertyDescriptor(GUARD_METHODS);
    }

    @Override
    public Object visit(ASTCompilationUnit unit, Object data) {

	    String[] logLevels = getProperty(LOG_LEVELS);
	    String[] guardMethods = getProperty(GUARD_METHODS);

        if (guardStmtByLogLevel.isEmpty() && logLevels.length > 0 && guardMethods.length > 0) {
            configureGuards(logLevels, guardMethods);
        } else if ( guardStmtByLogLevel.isEmpty() ) {
            configureDefaultGuards();
        }
        findViolationForEachLogStatement(unit, data, XPATH_EXPRESSION);
        return super.visit(unit, data);
    }

    private void configureGuards(String[] logLevels, String[] guardMethods) {
        String[] methods = guardMethods;
        if (methods.length != logLevels.length) {
            String firstMethodName = guardMethods[0];
            methods = new String[logLevels.length];
            for (int i = 0; i < logLevels.length; i++) {
                methods[i] = firstMethodName;
            }
        }
        for (int i = 0; i < logLevels.length; i++) {
            guardStmtByLogLevel.put("." + logLevels[i], methods[i]);
        }
    }

    private void configureDefaultGuards() {
        guardStmtByLogLevel.put("debug", GUARD_METHOD_NAME);
        guardStmtByLogLevel.put("error", GUARD_METHOD_NAME);
        guardStmtByLogLevel.put("trace", GUARD_METHOD_NAME);
        guardStmtByLogLevel.put("warn", GUARD_METHOD_NAME);
        guardStmtByLogLevel.put("info", GUARD_METHOD_NAME);
    }

    protected void findViolationForEachLogStatement(ASTCompilationUnit unit, Object data, String xpathExpression) {
        for (Entry<String, String> entry : guardStmtByLogLevel.entrySet()) {
            List<Node> nodes = findViolations(unit, entry.getKey(), entry.getValue(), xpathExpression);
            for (Node node: nodes) {
            	super.addViolation(data, node);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Node> findViolations(ASTCompilationUnit unit, String logLevel, String guard, String xpathExpression) {
        try {
            return unit.findChildNodesWithXPath(xpathExpression
                    .replaceAll("LOG_LEVEL_UPPERCASE", logLevel.toUpperCase()).replaceAll("LOG_LEVEL", logLevel)
                    .replaceAll("GUARD", guard));
        } catch (JaxenException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    protected void buildGuardStatementMap(List<String> logLevels, List<String> guardMethods) {
        for (String logLevel : logLevels) {
            boolean found = false;
            for (String guardMethod : guardMethods) {
                if (!found && guardMethod.toLowerCase().contains(logLevel.toLowerCase())) {
                    found = true;
                    guardStmtByLogLevel.put("." + logLevel, guardMethod);
                }
            }

            if (!found) {
                throw new IllegalArgumentException("No guard method associated to the logLevel:" + logLevel
                        + ". Should be something like 'is" + logLevel + "Enabled'.");
            }
        }
    }
}
