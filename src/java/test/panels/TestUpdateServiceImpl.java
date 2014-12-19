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
import java.util.Iterator;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.sql.DataSource;

/**
 *
 * @author Asus
 */
public class TestUpdateServiceImpl extends HessianServlet implements TestUpdateService {

    private transient DataSource ds;

    public void init() throws ServletException {
        try {
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/TestServer");
        } catch (NamingException ex) {
            throw new ServletException("Can not retrieve", ex);
        }
    }

    public boolean update(Test t) {
        boolean temp = false;
        Connection conn = null;
        PreparedStatement pst = null;
        PreparedStatement pst2 = null;
        PreparedStatement pst3 = null;

        try {
            synchronized (ds) {
                conn = ds.getConnection();
            }

            String sq = "UPDATE test SET ";
            sq += "teacher_id = ?, ";
            sq += "title = ?, ";
            sq += "author = ?, ";
            sq += "theme = ?, ";
            sq += "repeatPass = ?, ";
            sq += "date = ?, ";
            sq += "access = ?, ";
            sq += "instruction = ?, ";
            sq += "time = ?, ";
            sq += "poor = ?, ";
            sq += "unsat = ?, ";
            sq += "sat = ?, ";
            sq += "good = ?, ";
            sq += "exc = ? ";
            sq += "WHERE id = ?";

            pst = conn.prepareStatement(sq);
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
            pst.setLong(15, t.getId().longValue());
            pst.executeUpdate();

            List<Question> questions = t.getQuestions();
            if (questions != null) {
                for (int i = 0; i < questions.size(); i++) {
                    String sq2 = "UPDATE question SET ";
                    sq2 += "test_id = ?, ";
                    sq2 += "question_type_id = ?, ";
                    sq2 += "text = ?, ";
                    sq2 += "value = ? ";
                    sq2 += "WHERE id = ?";

                    pst2 = conn.prepareStatement(sq2);
                    pst2.setLong(1, t.getId().longValue());

                    pst2.setLong(2, questions.get(i).getQuestionTypeId().longValue());

                    pst2.setString(3, questions.get(i).getText());

                    pst2.setInt(4, questions.get(i).getValue());

                    pst2.setLong(5, questions.get(i).getId().longValue());

                    pst2.executeUpdate();

                    List<Answer> answers = questions.get(i).getAnswers();
                    if (answers != null) {
                        for (int j = 0; j < answers.size(); j++) {
                            String sq3 = "UPDATE answer SET ";
                            sq3 += "question_id = ?, ";
                            sq3 += "text = ?, ";
                            sq3 += "isRight = ? ";
                            sq3 += "WHERE id = ?";

                            pst3 = conn.prepareStatement(sq3);

                            pst3.setLong(1, questions.get(i).getId().longValue());

                            pst3.setString(2, answers.get(j).getText());

                            pst3.setInt(3, answers.get(j).getIsRight());

                            pst3.setLong(4, answers.get(j).getId().longValue());

                            pst3.executeUpdate();
                        }
                    }
                }
            }

            temp = true;

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (pst != null) {
                    pst.close();
                }
                if (pst2 != null) {
                    pst2.close();
                }
                if (pst3 != null) {
                    pst3.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return temp;
    }
}
