package xdi2.core.io.writers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.Literal;
import xdi2.core.Relation;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.io.AbstractXDIWriter;
import xdi2.core.io.MimeType;
import xdi2.core.io.XDIWriterRegistry;
import xdi2.core.util.iterators.SelectingIterator;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3XRef;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

public class XDIJSONWriter extends AbstractXDIWriter {

	private static final long serialVersionUID = -5510592554616900152L;

	private static final Logger log = LoggerFactory.getLogger(XDIJSONWriter.class);

	public static final String FORMAT_NAME = "XDI/JSON";
	public static final String FILE_EXTENSION = "json";
	public static final MimeType MIME_TYPE = new MimeType("application/xdi+json");

	private boolean writeContexts;
	private boolean writePretty;

	public XDIJSONWriter(Properties parameters) {

		super(parameters);
	}

	@Override
	protected void init() {

		// check parameters

		this.writeContexts = "1".equals(this.parameters.getProperty(XDIWriterRegistry.PARAMETER_CONTEXTS, XDIWriterRegistry.DEFAULT_CONTEXTS));
		this.writePretty = "1".equals(this.parameters.getProperty(XDIWriterRegistry.PARAMETER_PRETTY, XDIWriterRegistry.DEFAULT_PRETTY));

		if (log.isDebugEnabled()) log.debug("Parameters: writeContexts=" + this.writeContexts + ", writePretty=" + this.writePretty);
	}

	private void writeContextNode(ContextNode contextNode, BufferedWriter bufferedWriter, State state) throws IOException {

		String xri = contextNode.getXri().toString();

		// write context nodes

		if (contextNode.containsContextNodes()) {

			Iterator<ContextNode> needWriteContextStatements;

			if (this.writeContexts) {

				needWriteContextStatements = contextNode.getContextNodes();
			} else {

				// ignore implied context nodes

				needWriteContextStatements = new SelectingIterator<ContextNode> (contextNode.getContextNodes()) {

					@Override
					public boolean select(ContextNode contextNode) {

						return ! contextNode.getStatement().isImplied();
					}
				};
			}

			if (needWriteContextStatements.hasNext()) {

				startItem(bufferedWriter, state);
				bufferedWriter.write("\"" + xri + "/()\":[");
				for (; needWriteContextStatements.hasNext(); ) {

					ContextNode innerContextNode = needWriteContextStatements.next();
					bufferedWriter.write("\"" + innerContextNode.getArcXri().toString() + "\"" + (needWriteContextStatements.hasNext() ? "," : ""));
				}
				bufferedWriter.write("]");
				finishItem(bufferedWriter, state);
			}

			for (Iterator<ContextNode> innerContextNodes = contextNode.getContextNodes(); innerContextNodes.hasNext(); ) {

				ContextNode innerContextNode = innerContextNodes.next();
				this.writeContextNode(innerContextNode, bufferedWriter, state);
			}
		}

		// write relations

		Map<XDI3Segment, List<Relation>> relationsMap = new HashMap<XDI3Segment, List<Relation>> ();

		for (Iterator<Relation> relations = contextNode.getRelations(); relations.hasNext(); ) {

			Relation relation = relations.next();

			List<Relation> relationsList = relationsMap.get(relation.getArcXri());

			if (relationsList == null) {

				relationsList = new ArrayList<Relation> ();
				relationsMap.put(relation.getArcXri(), relationsList);
			}

			relationsList.add(relation);
		}

		for (Entry<XDI3Segment, List<Relation>> entry : relationsMap.entrySet()) {

			XDI3Segment relationArcXri = entry.getKey();
			List<Relation> relationsList = entry.getValue();

			startItem(bufferedWriter, state);
			bufferedWriter.write("\"" + xri + "/" + relationArcXri + "\":[");

			Graph tempGraph = null;

			boolean missingTrailingComma = false;

			for (int i = 0; i < relationsList.size(); i++) {

				XDI3Segment targetContextNodeXri = relationsList.get(i).getTargetContextNodeXri();
				XDI3XRef xref = targetContextNodeXri.getFirstSubSegment().getXRef();

				// if the target context node XRI is a valid statement in a cross-reference, add it to the temporary graph

				if (xref != null && xref.hasStatement()) {

					if (tempGraph == null) tempGraph = MemoryGraphFactory.getInstance().openGraph();

					tempGraph.createStatement(xref.getStatement());
				} else {

					bufferedWriter.write("\"" + targetContextNodeXri + "\"");

					if (i < relationsList.size() - 1) {

						bufferedWriter.write(",");
					} else {

						missingTrailingComma = true;
					}
				}
			}

			if (tempGraph != null) {

				if (missingTrailingComma) bufferedWriter.write(",");

				// write the temporary graph recursively

				this.write(tempGraph, bufferedWriter);
			}

			bufferedWriter.write("]");
			finishItem(bufferedWriter, state);
		}

		// write literal

		Literal literal = contextNode.getLiteral();

		if (literal != null) {

			startItem(bufferedWriter, state);
			bufferedWriter.write("\"" + xri + "/!\":[" + (literal.getLiteralData() == null ? "" : JSON.toJSONString(literal.getLiteralData())) + "]");
			finishItem(bufferedWriter, state);
		}
	}

	private void write(Graph graph, BufferedWriter bufferedWriter) throws IOException {

		State state = new State();

		startGraph(bufferedWriter, state);
		this.writeContextNode(graph.getRootContextNode(), bufferedWriter, state);
		finishGraph(bufferedWriter, state);

		bufferedWriter.flush();
	}

	@Override
	public Writer write(Graph graph, Writer writer) throws IOException {

		// write

		if (this.writePretty) {

			try {

				StringWriter stringWriter = new StringWriter();
				this.write(graph, new BufferedWriter(stringWriter));

				JSONObject json = JSON.parseObject(stringWriter.toString());
				writer.write(JSON.toJSONString(json, this.writePretty));
			} catch (JSONException ex) {

				throw new IOException("Problem while constructing JSON object: " + ex.getMessage(), ex);
			}
		} else {

			this.write(graph, new BufferedWriter(writer));
		}

		writer.flush();

		return writer;
	}

	private static void startItem(BufferedWriter bufferedWriter, State state) throws IOException {

		if (state.first) {

			state.first = false;
		} else {

			bufferedWriter.write(",");
		}
	}

	private static void finishItem(BufferedWriter bufferedWriter, State state) throws IOException {

	}

	private static void startGraph(BufferedWriter bufferedWriter, State state) throws IOException {

		state.first = true;

		bufferedWriter.write("{");
	}

	private static void finishGraph(BufferedWriter bufferedWriter, State state) throws IOException {

		bufferedWriter.write("}");
	}

	private static class State {

		private boolean first;

		private State() {

			this.first = true;
		}
	}
}
