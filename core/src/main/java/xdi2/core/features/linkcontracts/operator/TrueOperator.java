package xdi2.core.features.linkcontracts.operator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xdi2.core.Relation;
import xdi2.core.constants.XDIConstants;
import xdi2.core.exceptions.Xdi2RuntimeException;
import xdi2.core.features.linkcontracts.condition.Condition;
import xdi2.core.features.linkcontracts.evaluation.PolicyEvaluationContext;
import xdi2.core.util.GraphUtil;

/**
 * An XDI $true operator, represented as a relation.
 * 
 * @author markus
 */
public class TrueOperator extends ConditionOperator {

	private static final long serialVersionUID = 4296419491079293469L;

	protected TrueOperator(Relation relation) {

		super(relation);
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if a relation is a valid XDI $true operator.
	 * @param relation The relation to check.
	 * @return True if the relation is a valid XDI $true operator.
	 */
	public static boolean isValid(Relation relation) {

		if (! XDIConstants.XRI_S_TRUE.equals(relation.getArcXri())) return false;

		return true;
	}

	/**
	 * Factory method that creates an XDI $true operator bound to a given relation.
	 * @param relation The relation that is an XDI $true operator.
	 * @return The XDI $true operator.
	 */
	public static TrueOperator fromRelation(Relation relation) {

		if (! isValid(relation)) return null;

		return new TrueOperator(relation);
	}

	public static TrueOperator fromCondition(Condition condition) {

		return fromRelation(GraphUtil.relationFromComponents(XDIConstants.XRI_S_ROOT, XDIConstants.XRI_S_TRUE, condition.getStatement().toXriSegment()));
	}

	/*
	 * Instance methods
	 */

	@Override
	public Boolean[] evaluateInternal(PolicyEvaluationContext policyEvaluationContext) {

		Iterator<Condition> conditions = this.getConditions();
		if (conditions == null) throw new Xdi2RuntimeException("Missing or invalid condition in $true operator.");

		List<Boolean> values = new ArrayList<Boolean> ();
		while (conditions.hasNext()) values.add(Boolean.valueOf(Boolean.TRUE.equals(conditions.next().evaluate(policyEvaluationContext))));

		return values.toArray(new Boolean[values.size()]);
	}
}