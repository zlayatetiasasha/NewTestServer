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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.sql.DataSource;

/**
 *
 * @author Asus
 */
public class TestDeleteServiceImpl extends HessianServlet implements TestDeleteService {

    private transient DataSource ds;
    
    public void init() throws ServletException {
        try {
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/TestServer");
        }
        catch(NamingException ex) {
            throw new ServletException("Can not retrieve", ex);
        }
    }
    
    public boolean deleteTest(Test t) {
        boolean temp = false;
        Connection conn = null;        
        PreparedStatement prepStmt = null;
        PreparedStatement prepStmt2 = null;
        PreparedStatement prepStmt3 = null;
        try
        {
            init();
            synchronized(ds) {
                conn = ds.getConnection();
            }                
            String sq = "DELETE FROM test " +
                        "WHERE (id = ?);";
            prepStmt = conn.prepareStatement(sq);
            prepStmt.setLong(1, t.getId().longValue());
            prepStmt.executeUpdate();

            String sq2 = "DELETE FROM question " +
                        "WHERE (test_id = ?);";
            prepStmt2 = conn.prepareStatement(sq2);
            prepStmt2.setLong(1, t.getId().longValue());
            prepStmt2.executeUpdate();
            
            for(int i = 0; i < t.getQuestions().size(); i++) {
                String sq3 = "DELETE FROM answer " +
                        "WHERE (question_id = ?);";
                prepStmt3 = conn.prepareStatement(sq3);
                prepStmt3.setLong(1, t.getQuestions().get(i).getId().longValue());
                prepStmt3.executeUpdate();
            }
            
            temp = true;
        }    
        catch (Exception ex) {ex.printStackTrace();}
                finally {
                    try {
                        prepStmt.close();
                        prepStmt2.close();
                        prepStmt3.close();
                        conn.close();
                    }
                    catch(Exception ex) {ex.printStackTrace();}
                }
            
        return temp;
    }    
}
