import parser.MyXMLParser;
import parser.MyXMLParser.MyConstants;

/**
 * @author Cris Stanza, 29-Jul-2015.
 */
public final class Main {

	private Main() {
	}

	public static final void main(final String[] args) throws Exception {
		System.out.println("[start]");
		{
			new Main().start();
		}
		System.out.println("[end]");
	}

	private final void start() throws Exception {
		final MyXMLParser parser = new MyXMLParser(MyConstants.GEOMETRY_S);
		{
			parser.parse();
			// parser.printPLSQL();
			// parser.printSQL();
			// parser.printJS();
			{
				parser.printWKT_ORIG();
				parser.printWKT_S();
				parser.printWKT_SR();
			}
			{
				parser.printWKT_ORIG_JS();
				parser.printWKT_S_JS();
				parser.printWKT_SR_JS();
			}
			// parser.printTXT();
		}
	}

}
