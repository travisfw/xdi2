package xdi2.core.features.ordering;

import java.util.Iterator;

import xdi2.core.ContextNode;
import xdi2.core.Relation;
import xdi2.core.constants.XDIDictionaryConstants;
import xdi2.core.util.iterators.ReadOnlyIterator;
import xdi2.core.util.iterators.TerminatingOnNullIterator;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3SubSegment;
import xdi2.core.xri3.XRI3Constants;

public class Ordering {

	private Ordering() { }

	/*
	 * Methods for arc XRIs.
	 */

	public static XDI3SubSegment indexToXri(int index) {

		return XDI3SubSegment.create("" + XRI3Constants.GCS_DOLLAR + XRI3Constants.LCS_STAR + Integer.toString(index));
	}

	public static int xriToIndex(XDI3SubSegment arcXri) {

		if (! XRI3Constants.GCS_DOLLAR.equals(arcXri.getGCS())) return -1;
		if (! XRI3Constants.LCS_STAR.equals(arcXri.getLCS())) return -1;
		if (! arcXri.hasLiteral()) return -1;

		return Integer.parseInt(arcXri.getLiteral());
	}

	/*
	 * Methods for ordering context statements.
	 */

	public static ContextNode getOrderedContextNodeByIndex(final ContextNode contextNode, int index) {

		XDI3SubSegment indexArcXri = indexToXri(index);
		ContextNode indexContextNode = contextNode.getContextNode(indexArcXri);
		Relation indexRelation = indexContextNode == null ? null : indexContextNode.getRelation(XDIDictionaryConstants.XRI_S_REF);
		ContextNode orderedContextNode = indexRelation == null ? null : indexRelation.follow();

		return orderedContextNode;
	}

	public static ReadOnlyIterator<ContextNode> getOrderedContextNodes(final ContextNode contextNode) {

		return new TerminatingOnNullIterator<ContextNode> (new ReadOnlyIterator<ContextNode> () {

			private int index = 0;
			private ContextNode nextOrderedContextNode = null;
			private boolean triedNextOrderedContextNode = false;

			@Override
			public boolean hasNext() {

				this.tryNextIndexContextNode();

				return this.nextOrderedContextNode != null;
			}

			@Override
			public ContextNode next() {

				this.tryNextIndexContextNode();

				this.index++;
				this.triedNextOrderedContextNode = false;

				return this.nextOrderedContextNode;
			}

			private void tryNextIndexContextNode() {

				if (this.triedNextOrderedContextNode) return;

				this.nextOrderedContextNode = getOrderedContextNodeByIndex(contextNode, this.index + 1);

				this.triedNextOrderedContextNode = true;
			}
		});
	}

	/*
	 * Methods for ordering relations without a given arc XRI.
	 */

	public static Relation getOrderedRelationByIndex(final ContextNode contextNode, int index) {

		XDI3SubSegment indexArcXri = indexToXri(index);

		for (Iterator<Relation> indexRelations = contextNode.getRelations(); indexRelations.hasNext(); ) {

			Relation indexRelation = indexRelations.next();

			if (indexRelation.getArcXri().getLastSubSegment().equals(indexArcXri)) return indexRelation;
		}

		return null;
	}

	public static ReadOnlyIterator<Relation> getOrderedRelations(final ContextNode contextNode) {

		return new TerminatingOnNullIterator<Relation> (new ReadOnlyIterator<Relation> () {

			private int index = 0;
			private Relation nextOrderedRelation = null;
			private boolean triedNextOrderedRelation = false;

			@Override
			public boolean hasNext() {

				this.tryNextIndexRelation();

				return this.nextOrderedRelation != null;
			}

			@Override
			public Relation next() {

				this.tryNextIndexRelation();

				this.index++;
				this.triedNextOrderedRelation = false;

				return this.nextOrderedRelation;
			}

			private void tryNextIndexRelation() {

				if (this.triedNextOrderedRelation) return;

				this.nextOrderedRelation = getOrderedRelationByIndex(contextNode, this.index + 1);

				this.triedNextOrderedRelation = true;
			}
		});
	}

	/*
	 * Methods for ordering relations with a given arc XRI.
	 */

	public static Relation getOrderedRelationByIndex(final ContextNode contextNode, XDI3Segment arcXri, int index) {

		XDI3Segment indexArcXri = XDI3Segment.create("" + arcXri + indexToXri(index));
		Relation indexRelation = contextNode.getRelation(indexArcXri);

		return indexRelation;
	}

	public static ReadOnlyIterator<Relation> getOrderedRelations(final ContextNode contextNode, final XDI3Segment arcXri) {

		return new TerminatingOnNullIterator<Relation> (new ReadOnlyIterator<Relation> () {

			private int index = 0;
			private Relation nextOrderedRelation = null;
			private boolean triedNextOrderedRelation = false;

			@Override
			public boolean hasNext() {

				this.tryNextIndexRelation();

				return this.nextOrderedRelation != null;
			}

			@Override
			public Relation next() {

				this.tryNextIndexRelation();

				this.index++;
				this.triedNextOrderedRelation = false;

				return this.nextOrderedRelation;
			}

			private void tryNextIndexRelation() {

				if (this.triedNextOrderedRelation) return;

				this.nextOrderedRelation = getOrderedRelationByIndex(contextNode, arcXri, this.index + 1);

				this.triedNextOrderedRelation = true;
			}
		});
	}
}
