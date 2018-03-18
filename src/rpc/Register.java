package rpc;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONException;
import org.json.JSONObject;

import db.DBConnection;
import db.DBConnectionFactory;

/**
 * Servlet implementation class Register
 */
@WebServlet("/register")
public class Register extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final DBConnection conn = DBConnectionFactory.getDBConnection();
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Register() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
	   		 JSONObject msg = new JSONObject();
	   		 // get request parameters for userID and password
	   		 String user = request.getParameter("user_id");
		   	 String pwd = request.getParameter("password");
		   	 String first = request.getParameter("first_name");
		   	 String last = request.getParameter("last_name");
		   	 boolean flag = conn.register(user, pwd, first, last);
		   	 if (flag) {
		   		 msg.put("user_id", user);
			   	 msg.put("password", pwd);
			   	 msg.put("first_name", first);
			   	 msg.put("last_name", last);
			   	 msg.put("status", "OK");
		   		 RpcHelper.writeJsonObject(response, msg);
		   	 } else {
		   		 msg.put("user_id", user);
			   	 msg.put("password", pwd);
			   	 msg.put("first_name", first);
			   	 msg.put("last_name", last);
			   	 msg.put("status", "405");
		   		 RpcHelper.writeJsonObject(response, msg);
		   	 }
		   	 
	   	 } catch (JSONException e) {
	   		 // TODO Auto-generated catch block
	   		 e.printStackTrace();
	   	 } 
		 response.sendRedirect("index.html");
	}

}
