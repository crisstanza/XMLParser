package parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import utils.XmlUtils;
import dao.MyGeometry;
import dao.MyGeometryDao;

public final class MyXMLParser {

	private final int which;

	private Element doc = null;
	private List<MyData> data = null;
	private List<MyGeometry> geoData = null;

	public MyXMLParser(final int which) {
		this.which = which;
	}

	public final void parse() throws Exception {
		doc = XmlUtils.getDocumentElement(MyConstants.IN_XML);
		{
			data = new ArrayList<MyXMLParser.MyData>();
			final List<Node> rows = XmlUtils.getChildNodes(doc);
			for (Node row : rows) {
				data.add(dataFromRow(row));
			}
			Collections.sort(data, new Comparator<MyXMLParser.MyData>() {
				@Override
				public int compare(MyData o1, MyData o2) {
					if (o1.geometry[which] == null) {
						return Integer.MIN_VALUE;
					} else if (o2.geometry[which] == null) {
						return Integer.MAX_VALUE;
					} else {
						return o1.geometry[which].length() - o2.geometry[which].length();
					}
				}
			});
		}
		{
			final MyGeometryDao dao = new MyGeometryDao();
			geoData = dao.findAll();
			Collections.sort(geoData, new Comparator<MyGeometry>() {
				@Override
				public int compare(MyGeometry o1, MyGeometry o2) {
					if (o1.wkt == null) {
						return Integer.MIN_VALUE;
					} else if (o2.wkt == null) {
						return Integer.MAX_VALUE;
					} else {
						return o1.wkt.length() - o2.wkt.length();
					}
				}
			});
		}
	}

	private final MyData dataFromRow(Node row) {
		final MyData myData = new MyData();
		final List<Node> columns = XmlUtils.getChildNodes(row);
		for (Node column : columns) {
			final Node attribute = XmlUtils.getAttribute(column, "NAME");
			if (attribute.getNodeValue().equals("ID_PROJETO")) {
				myData.project = column.getTextContent();
			} else if (attribute.getNodeValue().equals("GEOMETRY")) {
				myData.geometry[MyConstants.GEOMETRY_ORIG] = fix(column.getTextContent());
			} else if (attribute.getNodeValue().equals("GEOMETRY_2")) {
				myData.geometry[MyConstants.GEOMETRY_2] = fix(column.getTextContent());
			} else if (attribute.getNodeValue().equals("GEOMETRY_3")) {
				myData.geometry[MyConstants.GEOMETRY_3] = fix(column.getTextContent());
			}
		}
		return myData;
	}

	public final void printPLSQL() throws Exception {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_PLSQL));
			writer.write("DECLARE" + System.lineSeparator());
			writer.write("g MDSYS.SDO_GEOMETRY;" + System.lineSeparator());
			writer.write("BEGIN" + System.lineSeparator());
			for (MyData myData : data) {
				if (myData.geometry[which] == null) {
					writer.write("NULL;" + System.lineSeparator());
				} else {
					final MyGeomData myGeomData = new MyGeomData(myData.geometry[which]);
					final String[] arrayElemInfo = array(myGeomData.elemInfo);
					final String[] arrayOrdinate = array(myGeomData.ordinate);
					writer.write(fixSize("g := " + myGeomData.part1 + "sdo_elem_info_array(" + (arrayElemInfo.length > MyConstants.MAX_ARGS ? "" : myGeomData.elemInfo) + "), sdo_ordinate_array(" + (arrayOrdinate.length > MyConstants.MAX_ARGS ? "" : myGeomData.ordinate) + "));" + System.lineSeparator()));
					if (arrayElemInfo.length > MyConstants.MAX_ARGS) {
						for (int i = 0; i < arrayElemInfo.length; i++) {
							writer.write("g.SDO_ELEM_INFO.EXTEND; g.SDO_ELEM_INFO(" + (i + 1) + ") := " + arrayElemInfo[i] + ";" + System.lineSeparator());
						}
					}
					if (arrayOrdinate.length > MyConstants.MAX_ARGS) {
						for (int i = 0; i < arrayOrdinate.length; i++) {
							writer.write("g.SDO_ORDINATES.EXTEND; g.SDO_ORDINATES(" + (i + 1) + ") := " + arrayOrdinate[i] + ";" + System.lineSeparator());
						}
					}
					writer.write(String.format(MyConstants.SQL, "g", myData.project) + System.lineSeparator());
					writer.write(MyConstants.SQL_COMMIT + System.lineSeparator());
				}
			}
			writer.write("END;" + System.lineSeparator());
			writer.write("/" + System.lineSeparator());
		} finally {
			close(writer);
		}
	}

	private final String fixSize(String str) {
		if (str.length() > MyConstants.MAX_LINE) {
			int first = str.indexOf(',', MyConstants.MAX_LINE - MyConstants.MAX_TOLERANCE);
			return str.substring(0, first + 1) + System.lineSeparator() + fixSize(str.substring(first + 1));
		}
		return str;
	}

	private final String[] array(String str) {
		return str.split(MyConstants.LIST_SEPARATOR);
	}

	public final void printSQL() throws Exception {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_SQL));
			for (MyData myData : data) {
				writer.write(String.format(MyConstants.SQL, myData.geometry[which], myData.project) + System.lineSeparator());
			}
			writer.write(MyConstants.SQL_COMMIT + System.lineSeparator());
		} finally {
			close(writer);
		}
	}

	public final void printJS() throws Exception {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_JS));
			writer.write("var g = [];" + System.lineSeparator());
			for (MyData myData : data) {
				if (myData.geometry[which] != null) {
					MyGeomData myGeomData = new MyGeomData(myData.geometry[which]);
					writer.write("g.push([" + myGeomData.ordinate + "]);" + System.lineSeparator());
				}
			}
			writer.write("var geometries = g;" + System.lineSeparator());
		} finally {
			close(writer);
		}
	}

	public void printWKT() throws Exception {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_WKT_TXT));
			writer.write("Total: " + geoData.size() + System.lineSeparator());
			writer.write(System.lineSeparator());
			for (MyGeometry geometry : geoData) {
				writer.write("project: " + geometry.project + "		wkt: " + geometry.wkt + System.lineSeparator());
			}
		} finally {
			close(writer);
		}
	}

	public void printWKTJS() throws Exception {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_WKT_JS));
			writer.write("var g = [];" + System.lineSeparator());
			for (MyGeometry myGeo : geoData) {
				if (myGeo.wkt.length() < MyConstants.WKT_STRING_LIMIT) {
					writer.write("g.push([" + wkt2js(myGeo.wkt) + "]); // " + myGeo.wkt.length() + System.lineSeparator());
				}
			}
			writer.write("var geometries = g;" + System.lineSeparator());
		} finally {
			close(writer);
		}
	}

	private final String wkt2js(final String str) {
		String result = str;
		result = result.replaceFirst("MULTIPOLYGON ", "");
		result = result.replaceFirst("POLYGON ", "");
		result = result.replaceAll("\\(\\(", "[");
		result = result.replaceAll("\\)\\)", "]");
		result = result.replaceAll("\\(", "[");
		result = result.replaceAll("\\)", "]");
		result = result.replaceAll(" ", ",");
		result = result.replaceAll(",,", ",");
		return result;
	}

	public final void printTXT() throws Exception {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_TXT));
			writer.write("Total: " + data.size() + System.lineSeparator());
			writer.write(System.lineSeparator());
			for (MyData myData : data) {
				if (myData.geometry[which] == null) {
					writer.write("project: " + myData.project + "		Elem Info: " + null + "		Ordinate: " + null + System.lineSeparator());
				} else {
					MyGeomData myGeomData = new MyGeomData(myData.geometry[which]);
					final String[] arrayElemInfo = array(myGeomData.elemInfo);
					final String[] arrayOrdinate = array(myGeomData.ordinate);
					writer.write("project: " + myData.project + "		Elem Info: " + arrayElemInfo.length + "		Ordinate: " + arrayOrdinate.length + System.lineSeparator());
				}
			}
		} finally {
			close(writer);
		}
	}

	private final String fix(String str) {
		if (MyConstants.FIX) {
			return str.replaceFirst("8307", "NULL");
		} else {
			return str;
		}
	}

	private final void close(final Writer closeMe) {
		try {
			if (closeMe != null) {
				closeMe.close();
			}
		} catch (final Exception exc) {
			// nothing to do!
		}
	}

	private static final class MyData {
		private String[] geometry = new String[3];
		private String project;
	}

	private static final class MyGeomData {
		private String part1, elemInfo, ordinate;

		private MyGeomData(String geometry) {
			final String[] parts = geometry.split("MDSYS.SDO_ELEM_INFO_ARRAY\\(");
			this.part1 = parts[0];
			final String[] parts2 = parts[1].split("\\),MDSYS.SDO_ORDINATE_ARRAY\\(");
			this.elemInfo = parts2[0];
			final String[] parts3 = parts2[1].split("\\)\\)");
			this.ordinate = parts3[0];
		}
	}

	public static final class MyConstants {
		public static final int GEOMETRY_ORIG = 0;
		public static final int GEOMETRY_2 = 1;
		public static final int GEOMETRY_3 = 2;

		private static final String IN_XML = "./in/in.xml";
		private static final String OUT_PLSQL = "./out/out.plsql";
		private static final String OUT_SQL = "./out/out.sql";
		private static final String OUT_JS = "./out/out.js";
		private static final String OUT_TXT = "./out/out.txt";
		public static final String OUT_WKT_TXT = "./out/out-wkt.txt";
		public static final String OUT_WKT_JS = "./out/out-wkt.js";

		private static final boolean FIX = false;

		private static final String LIST_SEPARATOR = "\\s*,\\s*";

		private static final String SQL = "UPDATE local SET geometria = %s WHERE identificacao = '%s';";
		private static final String SQL_COMMIT = "COMMIT;";

		private static final int MAX_ARGS = 999;
		private static final int MAX_LINE = 2499;
		private static final int MAX_TOLERANCE = 50;
		private static final int WKT_STRING_LIMIT = Integer.MAX_VALUE;

		private MyConstants() {
		}
	}

}
