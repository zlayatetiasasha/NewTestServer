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

    //Main method to add students answers: returns id of registered answeredTest
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
        List<AnswersStudent> answersStudent = new ArrayList<AnswersStudent>();
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }

            pst = conn.prepareStatement("SELECT * from answersstudent where test_id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, test_id.longValue());
            rs = pst.executeQuery();
            while (rs.next()) {
                Test t = getTestById(test_id, conn);
                BigInteger q_id = BigInteger.valueOf(rs.getLong("question_id"));
                Question q = getQuestionById(q_id, conn);
                BigInteger student_id = BigInteger.valueOf(rs.getLong("student_Id"));
                Student st = getStudentById(student_id, conn);
                List<Answer> ans = getAnswersofStudentForQuestionAndTest(q.getId(), t.getId(), student_id, conn);

                AnswersStudent as = new AnswersStudent(st, t, q, ans);
                answersStudent.add(as);
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
        return answersStudent;
    }

    public List<BigInteger> getAllResultsForTest(BigInteger test_id) {

        List<BigInteger> results = null;

        return results;
    }

    public List<AnswersStudent> getAllAnswersForTestForStudent(BigInteger student_id, BigInteger test_id) {
        List<AnswersStudent> result = null;
        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }

            pst = conn.prepareStatement("SELECT * from answersstudent where test_id=? and student_id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, test_id.longValue());
            pst.setLong(1, student_id.longValue());
            rs = pst.executeQuery();
            Student st = null;
            Test t = null;
            while (rs.next()) {
                st = new Student(student_id);
                t = new Test(test_id);
                while (rs.next()) {
                    Question q = new Question(BigInteger.valueOf(rs.getLong("question_id")));
                    BigInteger answer_id = BigInteger.valueOf(rs.getLong("answer_id"));
                    Answer a = null;
                    if (answer_id == null || answer_id.intValue() == 0) {
                        a = new Answer(rs.getString("answered_text"), q.getId());
                    } else {
                        a = new Answer(answer_id);
                    }
                    // AnswersStudent as = new AnswersStudent(st,t,q,)
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

        return result;

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

                            pst.setString(4, answer.getAnsweredText());

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
                        ResultSet rs = pst.getGeneratedKeys();
                        if (rs != null && rs.next()) {
                            AnswersStudent answst = new AnswersStudent(BigInteger.valueOf(rs.getLong(1)));
                            answersStudent.add(answst);
                        }

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
        PreparedStatement pst = null;
        ResultSet rs = null;
        PreparedStatement pst1 = null;
        ResultSet rs1 = null;
        Integer result = null;
        try {
            Test test = getTestById(answt.getTest().getId(), conn);
            result = calculateResult(test, answt);
            pst1 = conn.prepareStatement("UPDATE answeredtest set result=? where id=?");
            System.out.println("result=" + result);
            pst1.setLong(1, result.longValue());
            pst1.setLong(2, answt.getId().longValue());
            pst1.executeUpdate();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                pst1.close();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return BigInteger.valueOf(result.intValue());
    }

    //PRIVATE sector
    //HOW TO CALCULATE THE RESULT?????
    private Integer calculateResult(Test test, AnsweredTest answeredTest) {

        List<Question> questionsAnswered = answeredTest.getTest().getQuestions();
        List<Question> questionsRight = test.getQuestions();
        Map<BigInteger, BigInteger> rightMap = new HashMap<>();

        for (int i = 0; i < questionsRight.size(); i++) {
            for (int j = 0; j < questionsRight.get(i).getAnswers().size(); j++) {
                if (questionsRight.get(i).getAnswers().get(j).getIsRight() == 1) {
                    rightMap.put(BigInteger.valueOf(Long.valueOf(i)),
                            questionsRight.get(i).getAnswers().get(j).getId());
                }
            }
        }

        Integer score = 0;

        for (int i = 0; i < questionsAnswered.size(); i++) {
            for (int j = 0; j < questionsAnswered.get(i).getAnswers().size(); j++) {
                for (Map.Entry<BigInteger, BigInteger> entry : rightMap.entrySet()) {
                    System.out.println("question answered.getAnswers=" + questionsAnswered.get(i).getAnswers().get(j).getId());
                    System.out.println("entry value=" + entry.getValue());
                    if (questionsAnswered.get(i).getAnswers().get(j).getId() == entry.getValue()) {
                        score += (questionsRight.get((int) entry.getKey().longValue()).getValue()).intValue();
                    }
                }
            }
        }
        System.out.println("score=" + score);
        return score;

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
                t = createTestObject(rs, questions);

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

    private Question getQuestionById(BigInteger question_id, Connection conn) {
        boolean temp = false;
        Question question = null;

        PreparedStatement pst = null;
        ResultSet rs = null;
        try {

            pst = conn.prepareStatement("SELECT * from question where id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, question_id.longValue());
            rs = pst.executeQuery();

            //If test is found
            if (rs != null && rs.next()) {
                //get Questions with answers for test
                question = new Question(BigInteger.valueOf(rs.getLong("id")), BigInteger.valueOf(rs.getLong("question_type_id")),
                        BigInteger.valueOf(rs.getLong("test_id")), rs.getString("text"), rs.getInt("value"));

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
        return question;

    }

    private List<Answer> getAnswersofStudentForQuestionAndTest(BigInteger question_id, BigInteger test_id, BigInteger student_id, Connection conn) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        List<Answer> answers = new ArrayList<Answer>();
        try {
            pst = conn.prepareStatement("SELECT * from answersstudent where question_id=? and test_id=? and student_id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, question_id.longValue());
            pst.setLong(2, test_id.longValue());
            pst.setLong(3, student_id.longValue());
            rs = pst.executeQuery();
            //make a list of answers
            answers = new ArrayList<Answer>();
            while (rs.next()) {
                BigInteger answ_id = BigInteger.valueOf(rs.getLong("answer_id"));
                Answer a = null;
                System.out.println("answ" + answ_id);
                if (answ_id == null || answ_id.equals(BigInteger.valueOf(0L))) {
                    a = new Answer(rs.getString("answer_text"), question_id);
                    System.out.println("text=" + a.getAnsweredText());
                } else {
                    a = getAnswerByID(answ_id, conn);

                }
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

    private Student getStudentById(BigInteger student_id, Connection conn) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        Student st = null;
        try {
            pst = conn.prepareStatement("SELECT * from student where id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, student_id.longValue());
            rs = pst.executeQuery();
            //make a list of answers

            while (rs.next()) {
                st = new Student(student_id, rs.getString("name"), rs.getString("email"),
                        rs.getInt("course"), rs.getString("grnum"), rs.getString("faculty"));
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
        return st;

    }

    private Answer getAnswerByID(BigInteger answ_id, Connection conn) {
        PreparedStatement pst = null;
        ResultSet rs = null;
        Answer a = null;
        try {
            pst = conn.prepareStatement("SELECT * from answer where id=?", Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, answ_id.longValue());
            rs = pst.executeQuery();
            //make a list of answers

            while (rs.next()) {
                a = new Answer(answ_id, BigInteger.valueOf(rs.getLong("question_id")), rs.getInt("isRight"), rs.getString("text"));
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
        return a;

    }

}
