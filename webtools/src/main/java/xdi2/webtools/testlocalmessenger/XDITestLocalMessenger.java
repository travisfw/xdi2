package xdi2.webtools.testlocalmessenger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.client.local.XDILocalClient;
import xdi2.core.Graph;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.io.XDIReader;
import xdi2.core.io.XDIReaderRegistry;
import xdi2.core.io.XDIWriter;
import xdi2.core.io.XDIWriterRegistry;
import xdi2.core.io.readers.AutoReader;
import xdi2.core.io.writers.XDIDisplayWriter;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.MessageResult;
import xdi2.messaging.target.impl.graph.GraphMessagingTarget;
import xdi2.messaging.target.interceptor.impl.RefInterceptor;
import xdi2.messaging.target.interceptor.impl.LinkContractInterceptor;
import xdi2.messaging.target.interceptor.impl.VariablesInterceptor;

/**
 * Servlet implementation class for Servlet: XDITestLocalMessenger
 *
 */
public class XDITestLocalMessenger extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

	private static final long serialVersionUID = -585559530693089575L;

	private static Logger log = LoggerFactory.getLogger(XDITestLocalMessenger.class);

	private static MemoryGraphFactory graphFactory;
	private static String sampleInput;
	private static List<String> sampleMessages;

	static {

		graphFactory = MemoryGraphFactory.getInstance();
		graphFactory.setSortmode(MemoryGraphFactory.SORTMODE_ORDER);

		InputStream inputStream = XDITestLocalMessenger.class.getResourceAsStream("graph.xdi");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try {

			int i;

			while ((i = inputStream.read()) != -1) outputStream.write(i);
			sampleInput = new String(outputStream.toByteArray());
		} catch (Exception ex) {

		} finally {

			try {

				inputStream.close();
				outputStream.close();
			} catch (Exception ex) {

			}
		}

		sampleMessages = new ArrayList<String> ();

		while (true) {

			inputStream = XDITestLocalMessenger.class.getResourceAsStream("message" + (sampleMessages.size() + 1) + ".xdi");
			outputStream = new ByteArrayOutputStream();

			try {

				int i;

				while ((i = inputStream.read()) != -1) outputStream.write(i);
				sampleMessages.add(new String(outputStream.toByteArray()));
			} catch (Exception ex) {

				break;
			} finally {

				try {

					inputStream.close();
					outputStream.close();
				} catch (Exception ex) {

				}
			}
		}
	}


	public XDITestLocalMessenger() {

		super();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String sample = request.getParameter("sample");
		if (sample == null) sample = "1";

		request.setAttribute("resultFormat", XDIDisplayWriter.FORMAT_NAME);
		request.setAttribute("writeContexts", null);
		request.setAttribute("writeOrdered", "on");
		request.setAttribute("writePretty", null);
		request.setAttribute("variablesSupport", "on");
		request.setAttribute("dollarIsSupport", "on");
		request.setAttribute("linkContractsSupport", null);
		request.setAttribute("input", sampleInput);
		request.setAttribute("sampleMessages", sampleMessages);
		request.setAttribute("message", "0");
		request.getRequestDispatcher("/XDITestLocalMessenger.jsp").forward(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String resultFormat = request.getParameter("resultFormat");
		String writeContexts = request.getParameter("writeContexts");
		String writeOrdered = request.getParameter("writeOrdered");
		String writePretty = request.getParameter("writePretty");
		String variablesSupport = request.getParameter("variablesSupport");
		String dollarIsSupport = request.getParameter("dollarIsSupport");
		String linkContractsSupport = request.getParameter("linkContractsSupport");
		String input = request.getParameter("input");
		String message = request.getParameter("message");
		String output = "";
		String stats = "-1";
		String error = null;

		Properties xdiResultWriterParameters = new Properties();

		if ("on".equals(writeContexts)) xdiResultWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_CONTEXTS, "1");
		if ("on".equals(writeOrdered)) xdiResultWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_ORDERED, "1");
		if ("on".equals(writePretty)) xdiResultWriterParameters.setProperty(XDIWriterRegistry.PARAMETER_PRETTY, "1");
		
		XDIReader xdiReader = XDIReaderRegistry.getAuto();
		XDIWriter xdiInputWriter;
		XDIWriter xdiResultWriter = XDIWriterRegistry.forFormat(resultFormat, xdiResultWriterParameters);
		MessageEnvelope messageEnvelope = null;
		MessageResult messageResult = null;
		Graph graphInput = graphFactory.openGraph();

		long start = System.currentTimeMillis();

		try {

			// parse the input graph and remember its format

			xdiReader.read(graphInput, new StringReader(input));
			String inputFormat = ((AutoReader) xdiReader).getLastSuccessfulReader().getFormat();

			// parse the message envelope

			messageEnvelope = new MessageEnvelope();

			xdiReader.read(messageEnvelope.getGraph(), new StringReader(sampleMessages.get(Integer.parseInt(message))));

			// prepare the messaging target

			GraphMessagingTarget messagingTarget = new GraphMessagingTarget();
			messagingTarget.setGraph(graphInput);

			if ("on".equals(variablesSupport)) {

				VariablesInterceptor variablesInterceptor = new VariablesInterceptor();
				messagingTarget.getInterceptors().add(variablesInterceptor);
			}

			if ("on".equals(dollarIsSupport)) {

				RefInterceptor dollarIsInterceptor = new RefInterceptor();
				messagingTarget.getInterceptors().add(dollarIsInterceptor);
			}

			if ("on".equals(linkContractsSupport)) {

				LinkContractInterceptor linkContractsInterceptor = new LinkContractInterceptor();
				linkContractsInterceptor.setLinkContractsGraph(graphInput);
				messagingTarget.getInterceptors().add(linkContractsInterceptor);
			}

			messagingTarget.init();

			// send the message envelope and read result

			XDILocalClient client = new XDILocalClient(messagingTarget);

			messageResult = client.send(messageEnvelope, null);

			// output the modified input graph

			xdiInputWriter = XDIWriterRegistry.forFormat(inputFormat, null);

			StringWriter writer1 = new StringWriter();
			xdiInputWriter.write(graphInput, writer1);
			input = StringEscapeUtils.escapeHtml(writer1.getBuffer().toString());

			// output the message result

			StringWriter writer2 = new StringWriter();
			xdiResultWriter.write(messageResult.getGraph(), writer2);
			output = StringEscapeUtils.escapeHtml(writer2.getBuffer().toString());
		} catch (Exception ex) {

			if (ex instanceof Xdi2ClientException) {

				messageResult = ((Xdi2ClientException) ex).getErrorMessageResult();

				// output the message result

				if (messageResult != null) {

					StringWriter writer2 = new StringWriter();
					xdiResultWriter.write(messageResult.getGraph(), writer2);
					output = StringEscapeUtils.escapeHtml(writer2.getBuffer().toString());
				}
			}

			log.error(ex.getMessage(), ex);
			error = ex.getMessage();
			if (error == null) error = ex.getClass().getName();
		}

		long stop = System.currentTimeMillis();

		stats = "";
		stats += Long.toString(stop - start) + " ms time. ";
		if (messageEnvelope != null) stats += Integer.toString(messageEnvelope.getMessageCount()) + " message(s). ";
		if (messageEnvelope != null) stats += Integer.toString(messageEnvelope.getOperationCount()) + " operation(s). ";
		if (messageResult != null) stats += Integer.toString(messageResult.getGraph().getRootContextNode().getAllStatementCount()) + " result statement(s). ";

		// display results

		request.setAttribute("sampleMessages", sampleMessages);
		request.setAttribute("resultFormat", resultFormat);
		request.setAttribute("writeContexts", writeContexts);
		request.setAttribute("writeOrdered", writeOrdered);
		request.setAttribute("writePretty", writePretty);
		request.setAttribute("variablesSupport", variablesSupport);
		request.setAttribute("dollarIsSupport", dollarIsSupport);
		request.setAttribute("linkContractsSupport", linkContractsSupport);
		request.setAttribute("input", input);
		request.setAttribute("message", message);
		request.setAttribute("output", output);
		request.setAttribute("stats", stats);
		request.setAttribute("error", error);

		request.getRequestDispatcher("/XDITestLocalMessenger.jsp").forward(request, response);
	}   	  	    
}
