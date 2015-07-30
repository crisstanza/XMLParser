package utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Classe auxiliar para executar opera&ccedil;&otilde;es com XML.
 *
 * @author Cris Stanza, 14-Jul-2015
 */
public final class XmlUtils {

	private static final String YES = "yes";
	private static final String NO = "no";

	private static final String INDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";
	private static final String FOUR = "4";

	private static final String XML_DECLARATION_WITH_NO_NEW_LINE = "\\?><";
	private static final String XML_DECLARATION_WITH_NEW_LINE = "\\?>\n<";

	private XmlUtils() {
	}

	/**
	 * Formata (identa) um XML recebido na forma de String.
	 * 
	 * @param xml
	 *            XML a ser formatado.
	 * @return Uma String com o mesmo XML, porém formatado (identado).
	 * @throws Exception
	 *             No caso de qualquer tipo de erro.
	 * 
	 */
	public static String format(String xml) throws Exception {
		Source xmlInput = new StreamSource(new StringReader(xml));
		StreamResult xmlOutput = new StreamResult(new StringWriter());
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, YES);
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, NO);
		transformer.setOutputProperty(INDENT_AMOUNT, FOUR);
		transformer.transform(xmlInput, xmlOutput);
		String newXml = xmlOutput.getWriter().toString();
		return newXml.replaceFirst(XML_DECLARATION_WITH_NO_NEW_LINE, XML_DECLARATION_WITH_NEW_LINE);
	}

	public static Element getDocumentElement(String path) throws Exception {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(path).getDocumentElement();
	}

	public static List<Node> getChildNodes(Node node) {
		final NodeList children = node.getChildNodes();
		final int length = children.getLength();
		List<Node> data = new ArrayList<Node>();
		for (int i = 0; i < length; i++) {
			final Node row = children.item(i);
			if (row.getNodeType() == Node.ELEMENT_NODE) {
				data.add(row);
			}
		}
		return data;
	}

	public static Node getAttribute(Node node, String name) {
		final NamedNodeMap attributes = node.getAttributes();
		final int length = attributes.getLength();
		for (int i = 0; i < length; i++) {
			final Node attribute = attributes.item(i);
			if (attribute.getNodeName().equals(name)) {
				return attribute;
			}
		}
		return null;
	}

}
