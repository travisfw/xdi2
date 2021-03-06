package xdi2.resolution;

import java.io.Serializable;

import xdi2.core.Graph;
import xdi2.core.Literal;
import xdi2.core.Relation;
import xdi2.core.xri3.XDI3Segment;
import xdi2.messaging.MessageResult;

public class XDIResolutionResult implements Serializable {

	private static final long serialVersionUID = -1141807747864855392L;

	private String xri;
	private MessageResult messageResult;
	private String inumber;
	private String uri;

	private XDIResolutionResult(String xri, MessageResult messageResult, String inumber, String uri) {

		this.xri = xri;
		this.messageResult = messageResult;
		this.inumber = inumber;
		this.uri = uri;
	}

	/**
	 * Parses a XdiResolutionResult from an XDI2 message result.
	 * @return The XdiResolutionResult.
	 */
	public static XDIResolutionResult fromXriAndMessageResult(String xri, MessageResult messageResult) {

		Graph graph = messageResult.getGraph();

		// find I-Number

		String inumber;

		Relation relation = graph.findRelation(XDI3Segment.create(xri), XDI3Segment.create("$is"));		

		if (relation != null && relation.getTargetContextNodeXri().isINumber()) { 

			inumber = relation.getTargetContextNodeXri().toString();
		} else if (XDI3Segment.create(xri).isINumber()) {

			inumber = xri;
		} else {

			inumber = null;
		}

		// find URI

		String uri;

		Literal literal;

		if (inumber != null) {

			literal = graph.findLiteral(XDI3Segment.create("(" + inumber + ")" + "$!($uri)"));
		} else {

			literal = graph.findLiteral(XDI3Segment.create("(" + xri + ")" + "$!($uri)"));
		}

		if (literal != null) {

			uri = literal.getLiteralData();
		} else {

			uri = null;
		}

		// done

		return new XDIResolutionResult(xri, messageResult, inumber, uri);
	}

	public String getXri() {

		return this.xri;
	}

	public MessageResult getMessageResult() {

		return this.messageResult;
	}

	public String getInumber() {

		return this.inumber;
	}

	public String getUri() {

		return this.uri;
	}

	@Override
	public String toString() {

		return this.inumber + " / " + this.uri;
	}
}
