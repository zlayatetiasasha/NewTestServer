/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.panels;

import com.caucho.hessian.client.HessianProxyFactory;
import java.awt.event.ActionListener;
import java.io.*;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

/**
 *
 * @author LUDMILA2
 */
public class LoginServlet extends HttpServlet {    
        
    private Hashtable users = new Hashtable();  
    
    private transient DataSource ds = null;
    
    public void initt() throws ServletException {
        try {
            InitialContext initContext = new InitialContext();
            ds = (DataSource) initContext.lookup("java:comp/env/jdbc/TestServer");
        }
        catch(NamingException ex) {
            throw new ServletException("Can not retrieve", ex);
        }
    }
               
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {              
        OutputStream outstr = null;
        ObjectOutputStream oos = null;
        System.out.println("HELLO = ");
        
        HttpSession session = req.getSession(); 
        boolean temp = false;
        for(int i = 0; i < users.size(); i++) {
            temp = users.contains(session);
            System.out.println("temp = " + temp);
        }
        if(temp) {
            System.out.println("doGet session " + session.getId());
        try {
                res.setContentType("application/x-java-serialized-object");
                Inbox inbox =(Inbox) session.getAttribute("inbox");
                System.out.println("doGet inbox");
                if(inbox != null) {
                    System.out.println("inbox != null");
                    WakeUp wakeUp = new WakeUp(inbox, 60000, true); 
                    Thread wakeUpActivity = new Thread(wakeUp); 
                    wakeUpActivity.start();
                    String s = (String)inbox.getFirst();
                    if(s != null) {
                        outstr = res.getOutputStream();
                        oos = new ObjectOutputStream(outstr);
                        System.out.println("String is sent");
                        oos.writeObject(s);
                    }
                    wakeUp.setLooping(false);
                }
            }
            catch(Exception e) {e.printStackTrace();}
            finally {
                try {
                    if(oos != null) {
                        oos.flush();
                        oos.close();
                    }
                }
                catch(Exception ex) {ex.printStackTrace();}
            }
        }
    } 
    
    String password;
    String login;
    String m, s;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                    OutputStream outstr;
                    ObjectOutputStream oos = null;
                    
                    
                try {
                    response.setContentType("application/x-java-serialized-object");
                    HttpSession session = request.getSession();
                    InputStream in = request.getInputStream();
                    
                    ObjectInputStream inputFromApplet = new ObjectInputStream(in);
                    String str = (String) inputFromApplet.readObject();
                    outstr = response.getOutputStream();
                        oos = new ObjectOutputStream(outstr);
                       // oos.writeObject(true);
                    
                    System.out.println(str);
                    String teacherToCheck  = null, teacherToRegister=null, userToRemove = null, test = null,
                            addNewTest=null;
                    if(str.equals("checkTeacher")) teacherToCheck  = str;
                    if(str.equals("registerTeacher")) teacherToRegister = str;
                    if(str.equals("removeUser")) userToRemove = str;
                    if(str.equals("test")) test = str; 
                    if(str.equals("addNewTest")) addNewTest=str;
                   System.out.println("I am here");       
                    if(teacherToCheck != null ) {
                        password = (String) inputFromApplet.readObject();
                        login = (String) inputFromApplet.readObject();
                     //   outstr = response.getOutputStream();
                     //   oos = new ObjectOutputStream(outstr);
                        checkTeacher(oos, session, password, login);
                        System.out.println("Teacher to check");
                        session.setMaxInactiveInterval(-1);   
                    }
                    if(teacherToRegister != null ) {
                        password = (String) inputFromApplet.readObject();
                        login = (String) inputFromApplet.readObject();
                     //   outstr = response.getOutputStream();
                      //  oos = new ObjectOutputStream(outstr);
                        registerTeacher(oos, session, password, login);
                        System.out.println("Teacher to register");
                        session.setMaxInactiveInterval(-1);   
                    }
                   
                    else if(userToRemove != null) {
                     //   outstr = response.getOutputStream();
                    //    oos = new ObjectOutputStream(outstr);
                        System.out.println("userToRemove");
                        removeUser((String) inputFromApplet.readObject());
                        oos.writeObject(true);
                    }                    
                     else if(test != null) {
                    //    outstr = response.getOutputStream();
                    //    oos = new ObjectOutputStream(outstr);
                        oos.writeObject(true);
                        System.out.println("Hello i've hot test!");
                    }
                    
                    Enumeration en = users.elements();
                    while(en.hasMoreElements()) {
                        HttpSession se =(HttpSession) en.nextElement();
                        Inbox inbox = (Inbox) se.getAttribute("inbox");
                        if(inbox != null) {        
                            if(test != null) {
                                inbox.putLast("test");
                            }
                        }
                    }
                    if(userToRemove != null)
                        session.invalidate();
                    
                   System.out.println("Users = " + users.size());
            } catch (Exception e) {
                    e.printStackTrace();
            }
            finally {
                if(oos != null) {
                    oos.flush();
                    oos.close();
                }
            }
    } 
   
    public void checkTeacher(ObjectOutputStream out, HttpSession session, String login, String password) {
        Connection conn = null;        
        PreparedStatement prepStmt = null;
        ResultSet rs = null; 
        Teacher teacher = null;
        try {
            initt();
            synchronized(ds) {
                conn = ds.getConnection();
            }

            String sq = "SELECT t_id " +
                        "FROM logteacher " +
                        "WHERE (login = ?) & (password = ?);";
            prepStmt = conn.prepareStatement(sq, Statement.RETURN_GENERATED_KEYS);
            prepStmt.setString(1, login);
            prepStmt.setString(2, password);
            rs = prepStmt.executeQuery();
            if(rs.next()) {
                BigInteger id_teacher = BigInteger.valueOf(rs.getLong("t_id"));
                teacher = new Teacher(id_teacher);
            }
            
            System.out.println("I Am checking Teacher");
            
            if(teacher!=null)
            {
                getServletContext().setAttribute(password, teacher);
                users.put(password, session);
            }
            
            out.writeObject(teacher);
            
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
        session.setAttribute("inbox", new Inbox());
        session.setAttribute("user", password);
    } 
    
    public void registerTeacher(ObjectOutputStream out, HttpSession session, String login, String password) {
        
        Teacher teacher = null;
        Teacher newTeacherAdded=null;
        Connection conn = null;        
        PreparedStatement prepStmt = null;
        ResultSet rs = null; 
        ResultSet rs2 = null;
        PreparedStatement prepStmt2 = null;
        PreparedStatement prepStmt3 = null;
        try {
            initt();
            synchronized(ds) {
                conn = ds.getConnection();
            }
            
            String sq = "SELECT t_id " +
                        "FROM logteacher " +
                        "WHERE (login = ?) & (password = ?);";
            prepStmt = conn.prepareStatement(sq, Statement.RETURN_GENERATED_KEYS);
            prepStmt.setString(1, login);
            prepStmt.setString(2, password);
            rs = prepStmt.executeQuery();
            if(rs.next()) {
                BigInteger id_teacher = BigInteger.valueOf(rs.getLong("t_id"));
                teacher = new Teacher(id_teacher);
                out.writeObject(teacher);
            }
            else {
                newTeacherAdded = new Teacher();
                prepStmt2 = conn.prepareStatement("INSERT INTO teacher VALUES();", Statement.RETURN_GENERATED_KEYS);
                prepStmt2.executeUpdate();
                
                rs2 = prepStmt2.getGeneratedKeys();
                if(rs2.next())
                {
                    BigInteger lastid = BigInteger.valueOf(rs2.getLong(1));
                    newTeacherAdded.setId(lastid);

                    prepStmt3 = conn.prepareStatement("INSERT INTO logteacher (t_id, login, password) VALUES (?, ?, ?);");
                    prepStmt3.setLong(1, lastid.longValue());
                    prepStmt3.setString(2, login);
                    prepStmt3.setString(3, password);
                    prepStmt3.executeUpdate();

                    getServletContext().setAttribute(password, teacher);
                    users.put(password, session);
                    out.writeObject(newTeacherAdded);
                }
            }
        }
        catch (Exception ex) {ex.printStackTrace();}
        finally {
            try {
                prepStmt.close();
                prepStmt2.close();
                prepStmt3.close();
                rs.close();
                rs2.close();
            }
            catch(Exception ex) {ex.printStackTrace();}
        }
        session.setAttribute("inbox", new Inbox());
        session.setAttribute("user", password);
        
    } 
    
   /* public void addStudent(ObjectOutputStream out, HttpSession session, String login, String password) {
        Student student = null;
        try {
            Long id = Factory.getInstance().getStudentDAO().checkStudent(login, password);
            if (id < 0) {
                Factory.getInstance().getStudentDAO().addStudent(new Student());
            }
            else {
                student = Factory.getInstance().getStudentDAO().getStudentById(id);
            }
            Object value = getServletContext().getAttribute(password);
            if(value != null && value.getClass().getName().equals("Teacher")) {
                out.writeObject(null);
            }
            else {
                getServletContext().setAttribute(password, student);
                users.put(password, session);
                out.writeObject(student);
            }
        }
        catch (Exception ex) {ex.printStackTrace();}
        session.setAttribute("inbox", new Inbox());
        session.setAttribute("user", password);
    }
    */
    public void removeUser(String user) {
        users.remove(user);
        System.out.println("removeUser");
        
    }
        
    
        class Inbox extends Vector  implements Serializable {

            public synchronized void putLast(Object obj) {
                add(obj);
                notify();
            }

            public synchronized void putFirst(Object obj) {
                add(0, obj);
                notify();
            }

            public synchronized Object getFirst() {
                while(isEmpty()) {
                    try {
                        wait();
                    }
                    catch(InterruptedException e) { }
                }
                Object obj = firstElement();
                remove(0);
                return obj;
            }
        };
        
        class WakeUp implements Runnable, Serializable  {

            private int wakeUpFrequency;   
            private boolean looping = true;
            private Inbox inbox;

            public WakeUp(Inbox inbox, int wakeUpFrequency) {
                this.inbox = inbox;
                this.wakeUpFrequency = wakeUpFrequency;
            }

            public WakeUp(Inbox inbox, int wakeUpFrequency, boolean looping) {
                this.inbox = inbox;
                this.wakeUpFrequency = wakeUpFrequency;
                this.looping = looping;
            }
            
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(wakeUpFrequency);
                        synchronized(this) {
                        while (!looping)
                            wait(); 
                        }
                    }    
                    catch (InterruptedException e) { } 
                }
            }
            
            public synchronized void setFrequency(int wakeUpFrequency) {
                this.wakeUpFrequency = wakeUpFrequency;
            }

            public int getFrequency() {
                return wakeUpFrequency;
            }

            public synchronized void  setLooping(boolean b) {
                looping = b;
                    if (looping)
                        notify();
            }
            
            public boolean isLooping() {
                return looping; 
            }
        };
        
        public void destroy() {
            Enumeration en = users.elements();
            while(en.hasMoreElements()) {
                try {
                    HttpSession s =(HttpSession) en.nextElement();
                    s.invalidate();
                }
                catch(Exception e) { users = null;}
            }
            users = null;
        }
}

