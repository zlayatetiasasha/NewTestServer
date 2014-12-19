/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.panels;

import com.caucho.hessian.server.HessianServlet;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.sql.DataSource;

/**
 *
 * @author hp
 */
public class TestGetStatisticsServiceImpl extends HessianServlet implements TestGetStatisticsService {

    private transient DataSource ds;

    public void init() throws ServletException {
        try {
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/TestServer");
        } catch (NamingException ex) {
            throw new ServletException("Can not retrieve", ex);
        }
    }

    public BigInteger getAverageResultForTest(BigInteger test_id) {
        List<BigInteger> results = null;
        BigInteger avg = BigInteger.valueOf(0L);
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        PreparedStatement pst1 = null;
        ResultSet rs1 = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }

            pst = conn.prepareStatement("SELECT result from answeredtest where test_id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, test_id.longValue());
            rs = pst.executeQuery();
            results = new ArrayList<>();
            while (rs.next()) {
                BigInteger res = BigInteger.valueOf(rs.getLong("result"));
                results.add(res);
            }

            Integer count;
            pst1 = conn.prepareStatement("SELECT count(*) c from answeredtest where test_id=?", Statement.RETURN_GENERATED_KEYS);
            pst1.setLong(1, test_id.longValue());
            rs1 = pst1.executeQuery();
            if (rs1 != null && rs.next()) {
                count = rs1.getInt("c");

                if (count != 0) {
                    int sum = 0;
                    for (int i = 0; i < results.size(); i++) {
                        if (results.get(i).intValue() >= 0) {
                            sum += (results.get(i).intValue());
                        }
                    }
                    Long l=Math.round((double)sum/(double)count);
                    
                    avg = BigInteger.valueOf(l);
                    System.out.println(avg);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                pst.close();
                rs.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return avg;

    }
}
