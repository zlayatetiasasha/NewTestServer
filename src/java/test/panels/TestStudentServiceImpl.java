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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.sql.DataSource;

/**
 *
 * @author hp
 */
public class TestStudentServiceImpl extends HessianServlet implements TestStudentService{
    
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
    
    public Student addStudent(Student student){
        Connection conn = null;        
        PreparedStatement prepStmt = null;
        ResultSet rs = null; 
        try {
            
              synchronized(ds) {
                conn = ds.getConnection();
            } 
            prepStmt = conn.prepareStatement("INSERT INTO student (name, email, course, grnum, faculty) VALUES(?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            prepStmt.setString(1, student.getName());
            prepStmt.setString(2, student.getEmail());
            prepStmt.setInt(3, student.getCourse());
            prepStmt.setString(4, student.getGrnump());
            prepStmt.setString(5, student.getFaculty());
            prepStmt.executeUpdate();
            
            rs = prepStmt.getGeneratedKeys();
            if(rs.next())
            {
                BigInteger lastid = BigInteger.valueOf(rs.getLong(1));
                student.setId(lastid);
            }
        }
        catch (Exception ex) {ex.printStackTrace();}
        finally {
            try {
                prepStmt.close();
                rs.close();
                conn.close();
            }
            catch(Exception ex) {ex.printStackTrace();}
        }
        
        return student;
    }
}
