/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.panels;

/**
 *
 * @author LUDMILA2
 */
import com.caucho.hessian.server.HessianServlet;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.sql.DataSource;

public class TestServiceImpl extends HessianServlet implements TestService {

    private transient DataSource ds;

    public void init() throws ServletException {
        try {
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/TestServer");
        } catch (NamingException ex) {
            throw new ServletException("Can not retrieve", ex);
        }
    }

    public boolean addTest(Test t) {
        boolean temp = false;
        Connection conn = null;
        PreparedStatement pst = null;
        PreparedStatement pst2 = null;
        PreparedStatement pst3 = null;
        ResultSet rs = null;
        ResultSet rs2 = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }

            pst = conn.prepareStatement("INSERT INTO test (teacher_id, title, author, "
                    + "theme, repeatPass, date, access, instruction, time, poor, unsat, sat, good, exc) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, t.getTId().longValue());
            pst.setString(2, t.getTitle());
            pst.setString(3, t.getAuthor());
            pst.setString(4, t.getTheme());
            pst.setInt(5, t.getPassScore());
            pst.setString(6, t.getDate());
            pst.setInt(7, t.getAccess());
            pst.setString(8, t.getInstructions());
            pst.setInt(9, t.getTimeToPass());
            pst.setInt(10, t.getPoor());
            pst.setInt(11, t.getUnsat());
            pst.setInt(12, t.getSat());
            pst.setInt(13, t.getGood());
            pst.setInt(14, t.getExc());
            pst.executeUpdate();
            rs = pst.getGeneratedKeys();

            if (rs.next()) {
                BigInteger id_test = BigInteger.valueOf(rs.getLong(1));

                List<Question> questions = t.getQuestions();
                for (int i = 0; i < questions.size(); i++) {
                    pst2 = conn.prepareStatement("INSERT INTO question (test_id, question_type_id, text,"
                            + " value) VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
                    pst2.setLong(1, id_test.longValue());
                    pst2.setLong(2, questions.get(i).getQuestionTypeId().longValue());
                    pst2.setString(3, questions.get(i).getText());
                    pst2.setInt(4, questions.get(i).getValue());
                    pst2.executeUpdate();
                    rs2 = pst2.getGeneratedKeys();
                 
                    if (rs2.next()) {
                        BigInteger id_q = BigInteger.valueOf(rs2.getLong(1));
                        List<Answer> answers = questions.get(i).getAnswers();
                        for (int j = 0; j < answers.size(); j++) {
                            pst3 = conn.prepareStatement("INSERT INTO answer (question_id, text,"
                                    + " isRight) VALUES (?, ?, ?);");
                            pst3.setLong(1, id_q.longValue());
                            pst3.setString(2, answers.get(j).getText());
                            pst3.setInt(3, answers.get(j).getIsRight());
                            pst3.executeUpdate();
                        }
                        
                    }

                }
            }
            temp = true;
            System.out.println("added test.");
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                pst.close();
                pst2.close();
                pst3.close();
                rs.close();
                rs2.close();
                conn.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return temp;
    }

    public Test getTestById(BigInteger id) {
        boolean temp = false;
        Test t = null;

        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }

            pst = conn.prepareStatement("SELECT * from test where id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, id.longValue());
            rs = pst.executeQuery();
            
            //If test is found
            if (rs != null&&rs.next()) {
                //get Questions with answers for test
                List<Question> questions = getListQuestionsForTest(conn,id);
                System.out.println("created questions for test" + questions.size());

                t = createTestObject(rs, questions);
                System.out.println("got test:"+t.getTitle());

            } else {
                System.out.println("There is no such test!");
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
        return t;
    }
    
    
    
    
    
    //PRIVATE sector
    private Test createTestObject(ResultSet rs, List<Question> questions) throws SQLException {
       
       Test t = new Test(BigInteger.valueOf(rs.getLong("id")),
                BigInteger.valueOf(rs.getLong("teacher_id")),
                questions, 
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("theme"),
                rs.getInt("passScore"),
                rs.getString("date"),
                rs.getString("instruction"),
                rs.getInt("timeToPass"),
                rs.getInt("poor"),
                rs.getInt("unsat"),
                rs.getInt("sat"),
                rs.getInt("good"),
                rs.getInt("exc"),
                rs.getInt("access"));
        
     return t;
    }

    private List<Question> getListQuestionsForTest(Connection conn, BigInteger id) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<Question> questions = new ArrayList<Question>();
        try {
            pst = conn.prepareStatement("SELECT * from question where test_id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, id.longValue());
            rs = pst.executeQuery();
            //make a list of questions
            questions = new ArrayList<Question>();
            while (rs.next()) {
                Question q = new Question(BigInteger.valueOf(rs.getLong("id")),
                        BigInteger.valueOf(rs.getLong("question_type_id")),
                        BigInteger.valueOf(rs.getLong("test_id")),
                        rs.getString("text"),
                        rs.getInt("value"));
                List<Answer> answers=getListAnswersForQuestion(conn,q.getId());
                q.setAnswers(answers);
                questions.add(q);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                pst.close();
                rs.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return questions;

    }

    
     private List<Answer> getListAnswersForQuestion(Connection conn, BigInteger id) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<Answer> answers = new ArrayList<Answer>();
        try {
            pst = conn.prepareStatement("SELECT * from answer where question_id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, id.longValue());
            rs = pst.executeQuery();
            //make a list of answers
            answers = new ArrayList<Answer>();
            while (rs.next()) {
                Answer a = new Answer(BigInteger.valueOf(rs.getLong("id")),
                        BigInteger.valueOf(rs.getLong("question_id")),
                        rs.getInt("isRight"),
                        rs.getString("text"));
                answers.add(a);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                pst.close();
                rs.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return answers;

    }
}
