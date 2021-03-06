package xdi2.core.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.GraphFactory;
import xdi2.core.Literal;
import xdi2.core.Relation;
import xdi2.core.Statement;
import xdi2.core.constants.XDIConstants;
import xdi2.core.exceptions.Xdi2RuntimeException;
import xdi2.core.features.roots.InnerRoot;
import xdi2.core.features.roots.Root;
import xdi2.core.features.roots.Roots;
import xdi2.core.io.MimeType;
import xdi2.core.io.XDIWriter;
import xdi2.core.io.XDIWriterRegistry;
import xdi2.core.util.iterators.ReadOnlyIterator;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3Statement;

public abstract class AbstractGraph implements Graph {

	private static final long serialVersionUID = -5285276230236236923L;

	private static final Logger log = LoggerFactory.getLogger(AbstractContextNode.class);

	private GraphFactory graphFactory;

	protected AbstractGraph(GraphFactory graphFactory) {

		this.graphFactory = graphFactory;
	}

	/*
	 * General methods
	 */

	@Override
	public GraphFactory getGraphFactory() {

		return this.graphFactory;
	}

	@Override
	public void clear() {

		this.getRootContextNode().clear();
	}

	@Override
	public boolean isEmpty() {

		return this.getRootContextNode().isEmpty();
	}

	@Override
	public ContextNode findContextNode(XDI3Segment contextNodeXri, boolean create) {

		if (XDIConstants.XRI_S_ROOT.equals(contextNodeXri)) return this.getRootContextNode();

		return this.getRootContextNode().findContextNode(contextNodeXri, create);
	}

	@Override
	public Relation findRelation(XDI3Segment contextNodeXri, XDI3Segment arcXri, XDI3Segment targetContextNodeXri) {

		return this.getRootContextNode().findRelation(contextNodeXri, arcXri, targetContextNodeXri);
	}

	@Override
	public Relation findRelation(XDI3Segment contextNodeXri, XDI3Segment arcXri) {

		return this.getRootContextNode().findRelation(contextNodeXri, arcXri);
	}

	@Override
	public ReadOnlyIterator<Relation> findRelations(XDI3Segment contextNodeXri, XDI3Segment arcXri) {

		return this.getRootContextNode().findRelations(contextNodeXri, arcXri);
	}

	@Override
	public Literal findLiteral(XDI3Segment contextNodeXri, String literalData) {

		return this.getRootContextNode().findLiteral(contextNodeXri, literalData);
	}

	@Override
	public Literal findLiteral(XDI3Segment contextNodeXri) {

		return this.getRootContextNode().findLiteral(contextNodeXri);
	}

	@Override
	public boolean containsContextNode(XDI3Segment contextNodeXri) {

		return this.findContextNode(contextNodeXri, false) != null;
	}

	@Override
	public boolean containsRelation(XDI3Segment contextNodeXri, XDI3Segment arcXri, XDI3Segment targetContextNodeXri) {

		return this.findRelation(contextNodeXri, arcXri, targetContextNodeXri) != null;
	}

	@Override
	public boolean containsRelations(XDI3Segment contextNodeXri, XDI3Segment arcXri) {

		return this.findRelation(contextNodeXri, arcXri) != null;
	}

	@Override
	public boolean containsLiteral(XDI3Segment contextNodeXri, String literalData) {

		return this.findLiteral(contextNodeXri, literalData) != null;
	}

	@Override
	public boolean containsLiteral(XDI3Segment contextNodeXri) {

		return this.findLiteral(contextNodeXri) != null;
	}

	@Override
	public String toString(String format, Properties parameters) {

		if (format == null) format = XDIWriterRegistry.getDefault().getFormat();

		XDIWriter writer = XDIWriterRegistry.forFormat(format, parameters);
		StringWriter buffer = new StringWriter();

		try {

			writer.write(this, buffer);
		} catch (IOException ex) {

			return "[Exception: " + ex.getMessage() + "]";
		}

		return buffer.toString();
	}

	@Override
	public String toString(MimeType mimeType) {

		if (mimeType == null) throw new NullPointerException();

		XDIWriter writer = XDIWriterRegistry.forMimeType(mimeType);
		if (writer == null) throw new Xdi2RuntimeException("Unknown MIME type for XDI serialization: " + mimeType);

		StringWriter buffer = new StringWriter();

		try {

			writer.write(this, buffer);
		} catch (IOException ex) {

			return "[Exception: " + ex.getMessage() + "]";
		}

		return buffer.toString();
	}

	/*
	 * Methods related to statements
	 */

	@Override
	public Statement createStatement(XDI3Statement statementXri) {

		// inner root statement?

		if (statementXri.hasInnerRootStatement()) {

			Root root = Roots.findLocalRoot(this).findRoot(statementXri.getSubject(), true);

			XDI3Segment subject = root.getRelativePart(statementXri.getSubject());
			XDI3Segment predicate = statementXri.getPredicate();
			
			InnerRoot innerRoot = root.findInnerRoot(subject, predicate, true);

			return innerRoot.createRelativeStatement(statementXri.getInnerRootStatement());
		}

		// add the statement

		ContextNode baseContextNode = this.findContextNode(statementXri.getSubject(), true);

		if (statementXri.isContextNodeStatement()) {

			ContextNode contextNode = baseContextNode.createContextNodes(statementXri.getObject());
			if (log.isTraceEnabled()) log.trace("Under " + contextNode.getXri() + ": Created context node --> " + contextNode.getXri());

			return contextNode.getStatement();
		} else if (statementXri.isRelationStatement()) {

			Relation relation = baseContextNode.createRelation(statementXri.getArcXri(), statementXri.getTargetContextNodeXri());
			if (log.isTraceEnabled()) log.trace("Under " + baseContextNode.getXri() + ": Created relation " + relation.getArcXri() + " --> " + relation.getTargetContextNodeXri());

			return relation.getStatement();
		} else if (statementXri.isLiteralStatement()) {

			Literal literal = baseContextNode.createLiteral(statementXri.getLiteralData());
			if (log.isTraceEnabled()) log.trace("Under " + baseContextNode.getXri() + ": Created literal --> " + literal.getLiteralData());

			return literal.getStatement();
		} else {

			throw new Xdi2RuntimeException("Invalid statement XRI: " + statementXri);
		}
	}

	@Override
	public Statement findStatement(XDI3Statement statementXri) {

		if (statementXri.isContextNodeStatement()) {

			ContextNode contextNode = this.findContextNode(statementXri.getSubject(), false);
			contextNode = contextNode == null ? null : contextNode.findContextNode(statementXri.getObject(), false);

			return contextNode == null ? null : contextNode.getStatement();
		} else if (statementXri.isRelationStatement()) {

			Relation relation = this.findRelation(statementXri.getSubject(), statementXri.getArcXri(), statementXri.getTargetContextNodeXri());

			return relation == null ? null : relation.getStatement();
		} else if (statementXri.isLiteralStatement()) {

			Literal literal = this.findLiteral(statementXri.getSubject(), statementXri.getLiteralData());

			return literal == null ? null : literal.getStatement();
		}

		return null;
	}

	@Override
	public boolean containsStatement(XDI3Statement statementXri) {

		return this.findStatement(statementXri) != null;
	}

	/*
	 * Methods related to transactions.
	 */

	@Override
	public boolean supportsTransactions() {

		return false;
	}

	@Override
	public void beginTransaction() {

	}

	@Override
	public void commitTransaction() {

	}

	@Override
	public void rollbackTransaction() {

	}

	/*
	 * Object methods
	 */

	@Override
	public String toString() {

		return this.toString(null, null);
	}

	@Override
	public boolean equals(Object object) {

		if (object == null || ! (object instanceof Graph)) return false;
		if (object == this) return true;

		Graph other = (Graph) object;

		// TODO: do this without serializing to string

		return this.toString(new MimeType("text/xdi;contexts=1;ordered=1")).equals(other.toString(new MimeType("text/xdi;contexts=1;ordered=1")));
	}

	@Override
	public int hashCode() {

		// TODO: do this without serializing to string

		return this.toString(new MimeType("text/xdi;contexts=1;ordered=1")).hashCode();
	}

	@Override
	public int compareTo(Graph other) {

		if (other == null || other == this) return 0;

		// TODO: do this without serializing to string

		String string1 = this.toString(new MimeType("text/xdi;contexts=1;ordered=1"));
		String string2 = other.toString(new MimeType("text/xdi;contexts=1;ordered=1"));

		return string1.compareTo(string2);
	}
}
