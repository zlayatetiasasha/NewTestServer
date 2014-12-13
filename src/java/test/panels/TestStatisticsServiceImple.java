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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.sql.DataSource;

/**
 *
 * @author Asus
 */
public class TestStatisticsServiceImple extends HessianServlet implements TestStatisticsService {

    private final static BigInteger QUESTION_TYPE_OPEN_QUESTION = BigInteger.valueOf(3L);

    private transient DataSource ds;

    public void init() throws ServletException {
        try {
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/TestServer");
        } catch (NamingException ex) {
            throw new ServletException("Can not retrieve", ex);
        }
    }

    //returns id of registered answeredTest
    public BigInteger registerAnsweredTest(AnsweredTest answt) {
        BigInteger id = null;
        boolean temp = false;
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }
            pst = conn.prepareStatement("INSERT INTO answeredtest (student_id, test_id) "
                    + "VALUES (?, ?);", Statement.RETURN_GENERATED_KEYS);

            pst.setLong(1, answt.getStudent().getId().longValue());
            pst.setLong(2, answt.getTest().getId().longValue());
            pst.executeUpdate();
            rs = pst.getGeneratedKeys();
            while (rs.next()) {
                id = BigInteger.valueOf(rs.getLong(1));
                answt.setId(id);
            }
            ///Adding concrete answers to AnswersStudent

            List<AnswersStudent> answersStudent = addAnswersStudent(answt, conn);
            calculateAndUpdateResultForAnsweredTest(answersStudent, answt, conn);
            System.out.println("answeredTest added!");

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
        return id;

    }

    public BigInteger getResultForAnsweredTest(BigInteger answd_test_id) {
        BigInteger result = null;
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }

            pst = conn.prepareStatement("SELECT result from answeredtest where id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, answd_test_id.longValue());
            rs = pst.executeQuery();
            while (rs.next()) {
                result = BigInteger.valueOf(rs.getLong("result"));
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

        return result;
    }

    public List<AnswersStudent> getAllAnsweredInfoForTest(BigInteger test_id) {
        List<AnswersStudent> answersStudent = null;

        return answersStudent;
    }

    public List<BigInteger> getAllResultsForTest(BigInteger test_id) {

        List<BigInteger> results = null;

        return results;
    }

    //PRIVATE SECTOR
    private List<AnswersStudent> addAnswersStudent(AnsweredTest t, Connection conn) {
        PreparedStatement pst = null;
        List<AnswersStudent> answersStudent = new ArrayList<AnswersStudent>();
        try {
            Test test = t.getTest();
            Student student = t.getStudent();
            if (t.getTest() != null && t.getTest().getQuestions() != null) {
                for (int i = 0; i < t.getTest().getQuestions().size(); i++) {
                    Question question = t.getTest().getQuestion(i);

                    for (int j = 0; j < t.getTest().getQuestion(i).getAnswers().size(); j++) {
                        Answer answer = t.getTest().getQuestion(i).getAnswer(j);

                        if (answer.getId() == null) {
                            pst = conn.prepareStatement("INSERT INTO answersstudent (student_id, test_id, question_id, answer_text, id_answered_test) "
                                    + "VALUES (?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);

                            pst.setString(4, answer.getText());

                        } else {
                            pst = conn.prepareStatement("INSERT INTO answersstudent (student_id, test_id, question_id, answer_id, id_answered_test) "
                                    + "VALUES (?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);

                            pst.setLong(4, answer.getId().longValue());

                        }

                        pst.setLong(1, student.getId().longValue());
                        pst.setLong(2, test.getId().longValue());
                        pst.setLong(3, question.getId().longValue());
                        pst.setLong(5, t.getId().longValue());
                        pst.executeUpdate();
                        AnswersStudent answst = new AnswersStudent(student, test, question, answer);
                        answersStudent.add(answst);

                    }
                }
                System.out.println("Answers of student recorded to AnswersStudent table!");

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                pst.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return answersStudent;
    }

    private BigInteger calculateAndUpdateResultForAnsweredTest(List<AnswersStudent> answersStudent, AnsweredTest answt, Connection conn) {
        /*    PreparedStatement pst = null;
         ResultSet rs = null;*/
        BigInteger result = null;
        try {
            Test test = getTestById(answt.getTest().getId(), conn);
            result = calculateResult(test, answt);

            /*  pst = conn.prepareStatement("SELECT result from answeredtest where id=?", Statement.RETURN_GENERATED_KEYS);
             //  pst.setLong(1, answd_test_id.longValue());
             rs = pst.executeQuery();
             while (rs.next()) {
             //   result = BigInteger.valueOf(rs.getLong("result"));    
             }*/
            System.out.println("got test here!");

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                /*  pst.close();
                 rs.close();*/

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return result;
    }

    //PRIVATE sector
    //HOW TO CALCULATE THE RESULT?????
    private BigInteger calculateResult(Test test, AnsweredTest answeredTest) {
        BigInteger result = null;
        /*List<Question> questionsAnswered = answeredTest.getTest().getQuestions();
        List<Question> questionsRight = test.getQuestions();

        
        Set<BigInteger> ids = new HashSet<BigInteger>();
        Map<BigInteger, Question> rightMap = new HashMap<>();
        Map<BigInteger, Question> answeredMap = new HashMap<>();

        for (Question qright : questionsRight) {
            rightMap.put(qright.getId(), qright);
            ids.add(qright.getId());
        }

        for (Question qanswered : questionsAnswered) {
            answeredMap.put(qanswered.getId(), qanswered);
            ids.add(qanswered.getId());
        }
        int i = 0;

        for (BigInteger id : ids) {
            Question qr = rightMap.get(id);
            Question qa = answeredMap.get(id);
            
            List<Answer> answersRight = qr.getAnswers();
            List<Answer> answerscheck = qa.getAnswers();

            Set<BigInteger> ansids = new HashSet<BigInteger>();
            Map<BigInteger, Answer> answerrightMap = new HashMap<>();
            Map<BigInteger, Answer> answeransweredMap = new HashMap<>();

            for (Answer aright : answersRight) {
                answerrightMap.put(aright.getId(), aright);
                ansids.add(aright.getId());
            }

            for (Answer acheck : answerscheck) {
                answeransweredMap.put(acheck.getId(), acheck);
                ansids.add(acheck.getId());
            }
            
            for(BigInteger ansid : ansids){
               
            // Answer ar=answerrightMap.get(ansid);
            // Answer ac=answeransweredMap.get(ansid);
             
            }

        }

        /*   for (BigInteger id   : ids) {
         request = rightMap.get(id);
         response = answeredMap.get(id);
         // now matching
         }*/
        return result;
    }

    private Test getTestById(BigInteger id, Connection conn) {
        boolean temp = false;
        Test t = null;

        PreparedStatement pst = null;
        ResultSet rs = null;
        try {

            pst = conn.prepareStatement("SELECT * from test where id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, id.longValue());
            rs = pst.executeQuery();

            //If test is found
            if (rs != null && rs.next()) {
                //get Questions with answers for test
                List<Question> questions = getListQuestionsForTest(conn, id);
                System.out.println("created questions for test" + questions.size());

                t = createTestObject(rs, questions);
                System.out.println("got test:" + t.getTitle());

            } else {
                System.out.println("There is no such test!");
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
        return t;
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
