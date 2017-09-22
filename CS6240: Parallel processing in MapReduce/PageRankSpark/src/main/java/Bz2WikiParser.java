import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * bz2 file parses Wikipages on each line.
 * The parser removes all the files that contain ~ and replace & by &amp
 * It removes the duplicate outlinks
 * It removes the self links for a page
 */
public class Bz2WikiParser {
	private static Pattern namePattern;
	private static Pattern linkPattern;
	static {
		// Keep only html pages not containing tilde (~).
		namePattern = Pattern.compile("^([^~]+)$");
		// Keep only html filenames ending relative paths and not containing tilde (~).
		linkPattern = Pattern.compile("^\\..*/([^~]+)\\.html$");
	}

	public static HashSet<String> parserToken(String args) {
		BufferedReader reader = null;
		HashSet<String> linkPageNames = new HashSet<>();
		List<String> linkOnPage = new LinkedList<>();
		Set<String> adjList = new HashSet<String>();
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			SAXParser saxParser = spf.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();

			// Parser fills this list with linked page names.
			xmlReader.setContentHandler(new WikiParser(linkPageNames));
			String line;
			line= args;
			int delimLoc = line.indexOf(':');
			String pageName = line.substring(0, delimLoc);
			String html = line.substring(delimLoc + 1);
			html = html.replaceAll("& ", "&amp;");
			Matcher matcher = namePattern.matcher(pageName);
			if (!matcher.find()) {
				return null;
			}

			// Parse page and fill list of linked pages.
			linkPageNames.clear();
			try {
				xmlReader.parse(new InputSource(new StringReader(html)));
			} catch (Exception e) {
				return null;
			}
			if(linkPageNames.contains(pageName))
				linkPageNames.remove(pageName);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return linkPageNames;
	}

	/** Parses a Wikipage, finding links inside bodyContent div element. */
	private static class WikiParser extends DefaultHandler {

		/** List of linked pages; filled by parser. */
		private HashSet<String> linkPageNames;
		/** Nesting depth inside bodyContent div element. */
		private int count = 0;

		public WikiParser(HashSet<String> linkPageNames) {
			super();
			this.linkPageNames = linkPageNames;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			if ("div".equalsIgnoreCase(qName) && "bodyContent".equalsIgnoreCase(attributes.getValue("id")) && count == 0) {
				// Beginning of bodyContent div element.
				count = 1;
			} else if (count > 0 && "a".equalsIgnoreCase(qName)) {
				// Anchor tag inside bodyContent div element.
				count++;
				String link = attributes.getValue("href");
				if (link == null) {
					return;
				}
				try {
					// Decode escaped characters in URL.
					link = URLDecoder.decode(link, "UTF-8");
				} catch (Exception e) {
					// Wiki-weirdness; use link as is.
				}
				// Keep only html filenames ending relative paths and not containing tilde (~).
				Matcher matcher = linkPattern.matcher(link);
				if (matcher.find()) {
					linkPageNames.add(matcher.group(1));
				}
			} else if (count > 0) {
				// Other element inside bodyContent div.
				count++;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (count > 0) {
				// End of element inside bodyContent div.
				count--;
			}
		}
	}
}
