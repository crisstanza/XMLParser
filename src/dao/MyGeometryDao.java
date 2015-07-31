package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import oracle.spatial.geometry.JGeometry;
import oracle.spatial.util.WKT;

public final class MyGeometryDao {

	public List<MyGeometry> findAll() throws Exception {
		Connection con = null;
		Statement st = null;
		String sql = null;
		ResultSet rs = null;
		try {
			con = open();
			st = con.createStatement();
			sql = "SELECT id_projeto, geometry_3 FROM BASE_2015_01_DISSOLVE";
			rs = st.executeQuery(sql);
			List<MyGeometry> result = new ArrayList<MyGeometry>();
			while (rs.next()) {
				result.add(result(rs));
			}
			return result;
		} finally {
			close(con, st, rs);
		}
	}

	private final MyGeometry result(ResultSet rs) throws Exception {
		final MyGeometry myGeometry = new MyGeometry();
		myGeometry.project = rs.getString(1);
		oracle.sql.STRUCT struct = (oracle.sql.STRUCT) rs.getObject(2);
		JGeometry jGeometry = JGeometry.load(struct);
		try {
			myGeometry.wkt = new String(new WKT().fromJGeometry(jGeometry));
		} catch (final Exception exc) {
			exc.printStackTrace();
			myGeometry.wkt = exc.getMessage();
		}
		return myGeometry;
	}

	private final Connection open() throws Exception {
		Class.forName("oracle.jdbc.driver.OracleDriver");
		return DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe", "", "");
	}

	private final void close(Connection con, Statement st, ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
			if (st != null) {
				st.close();
			}
			if (con != null) {
				con.close();
			}
		} catch (Exception exc) {
			// nada a fazer!
		}
	}

}
