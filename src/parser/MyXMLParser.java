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
					if (o1.wkt_orig == null) {
						return Integer.MIN_VALUE;
					} else if (o2.wkt_orig == null) {
						return Integer.MAX_VALUE;
					} else {
						return o1.wkt_orig.length() - o2.wkt_orig.length();
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
			} else if (attribute.getNodeValue().equals("GEOM_ORIG")) {
				myData.geometry[MyConstants.GEOMETRY_ORIG] = fix(column.getTextContent());
			} else if (attribute.getNodeValue().equals("GEOM_S")) {
				myData.geometry[MyConstants.GEOMETRY_S] = fix(column.getTextContent());
			} else if (attribute.getNodeValue().equals("GEOM_SR")) {
				myData.geometry[MyConstants.GEOMETRY_SR] = fix(column.getTextContent());
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

	public void printWKT_ORIG() throws Exception {
		{
			System.out.println("	" + MyConstants.OUT_WKT_ORIG_TXT);
		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_WKT_ORIG_TXT));
			writer.write("-- Total: " + geoData.size() + System.lineSeparator());
			writer.write(System.lineSeparator());
			for (MyGeometry geometry : geoData) {
				writer.write(geometry.wkt_orig + " -- " + geometry.project + System.lineSeparator());
			}
		} finally {
			close(writer);
		}
	}

	public void printWKT_S() throws Exception {
		{
			System.out.println("	" + MyConstants.OUT_WKT_S_TXT);
		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_WKT_S_TXT));
			writer.write("-- Total: " + geoData.size() + System.lineSeparator());
			writer.write(System.lineSeparator());
			for (MyGeometry geometry : geoData) {
				writer.write(geometry.wkt_s + " -- " + geometry.project + System.lineSeparator());
			}
		} finally {
			close(writer);
		}
	}

	public void printWKT_SR() throws Exception {
		{
			System.out.println("	" + MyConstants.OUT_WKT_SR_TXT);
		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_WKT_SR_TXT));
			writer.write("-- Total: " + geoData.size() + System.lineSeparator());
			writer.write(System.lineSeparator());
			for (MyGeometry geometry : geoData) {
				writer.write(reduceWKT(geometry.wkt_s) + " -- " + geometry.project + System.lineSeparator());
			}
		} finally {
			close(writer);
		}
	}

	public void printWKT_ORIG_JS() throws Exception {
		{
			System.out.println("	" + MyConstants.OUT_WKT_ORIG_JS);
		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_WKT_ORIG_JS));
			writer.write("var g = [];" + System.lineSeparator());
			for (MyGeometry myGeo : geoData) {
				if (myGeo.wkt_orig.length() < MyConstants.WKT_STRING_LIMIT) {
					writer.write("g.push([" + wkt2js(myGeo.wkt_orig) + "]);" + System.lineSeparator());
				}
			}
			writer.write("var geometries = g;" + System.lineSeparator());
		} finally {
			close(writer);
		}
	}

	public void printWKT_S_JS() throws Exception {
		{
			System.out.println("	" + MyConstants.OUT_WKT_S_JS);
		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_WKT_S_JS));
			writer.write("var g = [];" + System.lineSeparator());
			for (MyGeometry myGeo : geoData) {
				if (myGeo.wkt_s.length() < MyConstants.WKT_STRING_LIMIT) {
					writer.write("g.push([" + wkt2js(myGeo.wkt_s) + "]);" + System.lineSeparator());
				}
			}
			writer.write("var geometries = g;" + System.lineSeparator());
		} finally {
			close(writer);
		}
	}

	public void printWKT_SR_JS() throws Exception {
		{
			System.out.println("	" + MyConstants.OUT_WKT_SR_JS);
		}
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(MyConstants.OUT_WKT_SR_JS));
			writer.write("var g = [];" + System.lineSeparator());
			for (MyGeometry myGeo : geoData) {
				if (myGeo.wkt_s.length() < MyConstants.WKT_STRING_LIMIT) {
					writer.write("g.push([" + reduceJS(myGeo.wkt_s) + "]);" + System.lineSeparator());
				}
			}
			writer.write("var geometries = g;" + System.lineSeparator());
		} finally {
			close(writer);
		}
	}

	private String reduceWKT(String wkt) {
		List<Pontos> reduced = reduce(wkt);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < reduced.size(); i++) {
			Pontos x = reduced.get(i);
			if (i == 0) {
				sb.append(x.getTipo());
				sb.append(" (");
				if (x.getTipo().equals("MULTIPOLYGON")) {
					sb.append("(");
				}
			}
			sb.append("(");
			for (int j = 0; j < x.size(); j++) {
				sb.append(x.get(j));
				if (j < x.size() - 1) {
					sb.append(j % 2 == 0 ? " " : ", ");
				}
			}
			sb.append(")");
			if (i < reduced.size() - 1) {
				sb.append(",");
			} else {
				if (x.getTipo().equals("MULTIPOLYGON")) {
					sb.append(")");
				}
			}
		}
		sb.append(")");
		return sb.toString();
	}

	private String reduceJS(String wkt) {
		List<Pontos> reduced = reduce(wkt);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < reduced.size(); i++) {
			sb.append("[");
			List<Double> x = reduced.get(i);
			for (int j = 0; j < x.size(); j++) {
				sb.append(x.get(j));
				if (j < x.size() - 1) {
					sb.append(",");
				}
			}
			sb.append("]");
			if (i < reduced.size() - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	private final List<Pontos> reduce(String wkt) {
		final String js = wkt2js(wkt);
		String[] parts = js.split("],");
		List<double[]> partsAsDoubleList = new ArrayList<double[]>();
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			part = part.replaceAll("\\[", "");
			part = part.replaceAll("\\]", "");
			partsAsDoubleList.add(toDoubleArray(part));
		}
		List<Pontos> reduced = new ArrayList<Pontos>();
		for (int i = 0; i < partsAsDoubleList.size(); i++) {
			Pontos doubleList = new Pontos(wkt.split(" ")[0]);
			double[] doubleArray = partsAsDoubleList.get(i);
			double last = 0;
			boolean add = false;
			for (int j = 0; j < doubleArray.length; j++) {
				double d = doubleArray[j];
				if (doubleArray.length > MyConstants.MIN_POINTS_TO_REDUCE) {
					if (j % 2 == 0) {
						double diff = Math.abs(d - last);
						if ((diff > MyConstants.DELTA) || (j == (doubleArray.length - 2))) {
							doubleList.add(d);
							last = d;
							add = true;
						} else {
							add = false;
						}
					} else {
						if (add || j == (doubleArray.length - 1)) {
							doubleList.add(d);
						}
					}
				} else {
					doubleList.add(d);
				}
			}
			reduced.add(doubleList);
		}
		return reduced;
	}

	private double[] toDoubleArray(String arrayAsString) {
		String[] parts = arrayAsString.split(MyConstants.LIST_SEPARATOR);
		double[] doubleArray = new double[parts.length];
		for (int i = 0; i < parts.length; i++) {
			doubleArray[i] = Double.parseDouble(parts[i]);
		}
		return doubleArray;
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
		public static final int GEOMETRY_S = 1;
		public static final int GEOMETRY_SR = 2;

		private static final double DELTA = 0.000001;
		private static final int MIN_POINTS_TO_REDUCE = 50;

		private static final String IN_XML = "./in/in.xml";
		private static final String OUT_PLSQL = "./out/out.plsql";
		private static final String OUT_SQL = "./out/out.sql";
		private static final String OUT_WKT_ORIG_JS = "./out/out-wkt-orig.js";
		private static final String OUT_WKT_S_JS = "./out/out-wkt-s.js";
		private static final String OUT_WKT_SR_JS = "./out/out-wkt-sr.js";
		private static final String OUT_TXT = "./out/out.txt";
		private static final String OUT_WKT_ORIG_TXT = "./out/out-wkt-orig.txt";
		private static final String OUT_WKT_S_TXT = "./out/out-wkt-s.txt";
		private static final String OUT_WKT_SR_TXT = "./out/out-wkt-sr.txt";

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
