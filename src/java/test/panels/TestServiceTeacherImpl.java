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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.sql.DataSource;

/**
 *
 * @author Asus
 */
public class TestServiceTeacherImpl extends HessianServlet implements TestServiceTeacher {

    private final static int TEST_ACCESS_GENERAL = 0;

    private transient DataSource ds;

    public void init() throws ServletException {
        try {
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/TestServer");
        } catch (NamingException ex) {
            throw new ServletException("Can not retrieve", ex);
        }
    }

    public Test[] getTeacherTests(BigInteger teacher_id) {

        List<Test> temp = null;
        List<Question> questions = null;
        List<Answer> answers = null;
        Test[] tests = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        PreparedStatement prepStmt1 = null;
        ResultSet rs2 = null;
        PreparedStatement prepStmt2 = null;
        ResultSet rs3 = null;
        PreparedStatement prepStmt3 = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }
            String sq = "SELECT * "
                    + "FROM test "
                    + "WHERE (teacher_id = ?);";
            stmt = conn.prepareStatement(sq);
            stmt.setLong(1, teacher_id.longValue());

            rs = stmt.executeQuery();
            temp = new ArrayList<Test>();
            while (rs.next()) {
                //get Questions with answers for test
                BigInteger test_id = BigInteger.valueOf(rs.getLong("id"));
                questions = getListQuestionsForTest(conn, test_id);

                Test t = createTestObject(rs, questions);
                temp.add(t);
            }


             tests = temp.toArray(new Test[temp.size()]);
             
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (prepStmt1 != null) {
                    prepStmt1.close();
                }
                if (prepStmt2 != null) {
                    prepStmt2.close();
                }
                if (prepStmt3 != null) {
                    prepStmt3.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (rs1 != null) {
                    rs1.close();
                }
                if (rs2 != null) {
                    rs2.close();
                }
                if (rs3 != null) {
                    rs3.close();
                }
                if (conn != null) {
                    conn.close();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("returnin tests");

        return tests;
    }

    public Test[] getAllAccessibleTestsForAndroid() {
         List<Test> temp = null;
        List<Question> questions = null;
        List<Answer> answers = null;
        Test[] tests = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        PreparedStatement prepStmt1 = null;
        ResultSet rs2 = null;
        PreparedStatement prepStmt2 = null;
        ResultSet rs3 = null;
        PreparedStatement prepStmt3 = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }
            String sq = "SELECT * "
                    + "FROM test "
                    + "WHERE (access = ?);";
            stmt = conn.prepareStatement(sq);
            stmt.setLong(1, TEST_ACCESS_GENERAL);

            rs = stmt.executeQuery();
            temp = new ArrayList<Test>();
            while (rs.next()) {
                //get Questions with answers for test
                BigInteger test_id = BigInteger.valueOf(rs.getLong("id"));
                System.out.println("got test for Anroid");
                questions = getListQuestionsForTest(conn, test_id);
                System.out.println(questions.size()+" questions");
                Test t = createTestObject(rs, questions);
                temp.add(t);
            }


             tests = temp.toArray(new Test[temp.size()]);
             
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (prepStmt1 != null) {
                    prepStmt1.close();
                }
                if (prepStmt2 != null) {
                    prepStmt2.close();
                }
                if (prepStmt3 != null) {
                    prepStmt3.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (rs1 != null) {
                    rs1.close();
                }
                if (rs2 != null) {
                    rs2.close();
                }
                if (rs3 != null) {
                    rs3.close();
                }
                if (conn != null) {
                    conn.close();
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("returnin tests");

        return tests; 
        
   /*     List<Test> temp = null;
        List<Question> questions = null;
        List<Answer> answers = null;
        Test[] tests = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        PreparedStatement prepStmt1 = null;
        ResultSet rs2 = null;
        PreparedStatement prepStmt2 = null;
        ResultSet rs3 = null;
        PreparedStatement prepStmt3 = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }
            String sq = "SELECT * "
                    + "FROM test "
                    + "WHERE (access = ?);";
            stmt = conn.prepareStatement(sq);
            stmt.setInt(1, TEST_ACCESS_GENERAL);

            rs = stmt.executeQuery();

            temp = new ArrayList<Test>();
            while (rs.next()) {
                Test curTest = new Test();
                Long id_test = rs.getLong(1);
                curTest.setId(BigInteger.valueOf(id_test));

                // read test questions
                String sq1 = "SELECT * "
                        + "FROM question "
                        + "WHERE (test_id = ?);";
                prepStmt1 = conn.prepareStatement(sq1);
                prepStmt1.setLong(1, id_test);
                rs1 = prepStmt1.executeQuery();
                questions = new ArrayList<Question>();
                while (rs1.next()) {
                    Question q = new Question();
                    Long id_q = rs1.getLong(1);
                    q.setId(BigInteger.valueOf(id_q));
                    q.setText(rs1.getString("text"));
                    q.setValue(rs1.getInt("value"));
                    q.setTest(curTest);

                    // read question answers
                    String sq2 = "SELECT * "
                            + "FROM answer "
                            + "WHERE (question_id = ?);";
                    prepStmt2 = conn.prepareStatement(sq2);
                    prepStmt2.setLong(1, id_q);
                    rs2 = prepStmt2.executeQuery();
                    answers = new ArrayList<Answer>();
                    while (rs2.next()) {
                        Answer a = new Answer();
                        a.setId(BigInteger.valueOf(rs2.getLong(1)));
                        a.setText(rs2.getString("text"));
                        a.setIsRight(rs2.getInt("isRight"));
                        a.setQuestion(q);
                        answers.add(a);
                    }

                    // read question type 
                    Long id_qt = rs1.getLong("question_type_id");
                    String sq3 = "SELECT * "
                            + "FROM questiontype "
                            + "WHERE (id = ?);";
                    prepStmt3 = conn.prepareStatement(sq3);
                    prepStmt3.setLong(1, id_qt);
                    rs3 = prepStmt3.executeQuery();
                    if (rs3.next()) {
                        QuestionType qt = new QuestionType();
                        qt.setId(BigInteger.valueOf(rs3.getLong(1)));
                        qt.setName(rs3.getString("name"));
                        q.setQuestionTypeId(qt.getId());
                        q.setQuestionType(qt);
                    }

                    questions.add(q);
                }

                curTest.setQuestions(questions);

                String title = rs.getString("title");
                String author = rs.getString("author");
                String theme = rs.getString("theme");
                String date = rs.getString("date");
                String instruction = rs.getString("instruction");
                Integer passScore = rs.getInt("passScore");
                Integer access = rs.getInt("access");
                Integer time = rs.getInt("time");
                Integer poor = rs.getInt("poor");
                Integer unsat = rs.getInt("unsat");
                Integer sat = rs.getInt("sat");
                Integer good = rs.getInt("good");
                Integer exc = rs.getInt("exc");

                curTest.setTitle(title);
                curTest.setAuthor(author);
                curTest.setTheme(theme);
                curTest.setDate(date);
                curTest.setInstructions(instruction);
                curTest.setTimeToPass(time);
                curTest.setPoor(poor);
                curTest.setUnsat(unsat);
                curTest.setSat(sat);
                curTest.setGood(good);
                curTest.setEXc(exc);
                curTest.setAccess(access);
                curTest.setPassScore(passScore);
                temp.add(curTest);
            }
*/
       
    }

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
                List<Answer> answers = getListAnswersForQuestion(conn, q.getId());
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
