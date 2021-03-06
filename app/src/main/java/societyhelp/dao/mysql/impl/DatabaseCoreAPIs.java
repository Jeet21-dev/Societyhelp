package societyhelp.dao.mysql.impl;

//import android.util.Log;

import android.util.Log;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import societyhelp.app.util.RandomString;
import societyhelp.app.util.SocietyHelpConstant;
import societyhelp.app.util.Util;
import societyhelp.core.SocietyAuthorization;
import societyhelp.dao.DatabaseConstant;
import societyhelp.dao.mysql.impl.ExpenseType.ExpenseTypeConst;
import societyhelp.dao.mysql.impl.ExpenseType.PaymentStatusConst;
//import societyhelp.dao.SocietyHelpDatabaseFactory;
import societyhelp.parser.LoadBhowaInitialData;
import societyhelp.parser.LoadBhowaInitialData.LoadData;

public class DatabaseCoreAPIs extends Queries implements DatabaseConstant, SocietyHelpConstant {

	private static long autoSplitId;
	private static Connection connection;

    public static void main(String[] args) {
		
		try {
			
			DatabaseCoreAPIs db = new DatabaseCoreAPIs("jdbc:mysql://localhost:3306/societyhelp", "root", "root123");
			System.out.println("Start parsing ...");
			long sTime = System.currentTimeMillis();
			
			/*LoadData ld = LoadBhowaInitialData.loadInitialData("D:\\workspace_android\\societyhelp\\docs\\initial_data\\LoadDataNew.csv");
			long eTime = System.currentTimeMillis();
			System.out.println("Parsed.(milis) - " + (eTime - sTime));
			db.loadInitialData(ld);
			sTime = System.currentTimeMillis();
			Util.generateBalanceSheet(db.getBalanceSheetData(), db.getFlatWisePayables());
			eTime = System.currentTimeMillis();
			System.out.println("Generate XLS (milis) - " + (eTime - sTime));*/
			//db.rollbackAutoSplit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    private String databaseDBURL;
    private String databaseUser;
    private String databasePassword;
    private boolean isInitialized;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            e.getStackTrace();
            //Log.e("Error", "Driver Initialization failed", e);
        }
    }

    public DatabaseCoreAPIs(String strDatabaseDBURL, String strDatabaseUser, String strDatabasePassword) {
        init(strDatabaseDBURL, strDatabaseUser, strDatabasePassword);
        isInitialized = true;
    }

    private void init(String strDatabaseDBURL, String strDatabaseUser, String strDatabasePassword) {
        databaseDBURL = strDatabaseDBURL;
        databaseDBURL = strDatabaseDBURL;
        databaseUser = strDatabaseUser;
        databasePassword = strDatabasePassword;
    }

    public synchronized Connection getDBInstance() throws Exception {

        if (!isInitialized)
            throw new ExceptionInInitializerError("DBURL, User and Password are not initialize.");

        try {
            if(connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(databaseDBURL, databaseUser, databasePassword);
            }
            return connection;
        } catch (Exception e) {
            throw e;
        }
    }

    private void close(Connection connection, PreparedStatement pStat, ResultSet result) {

        try {
            if (connection != null) {
                connection.close();
            }

            if (pStat != null) {
                pStat.close();
            }

            if (result != null) {
                result.close();
            }
        } catch (SQLException e) {
            //Log.e("Error", "Connection close has some problem.", e);
        }
    }

    private void close(Connection connection, Statement pStat, ResultSet result) {

        try {
            if (connection != null) {
                connection.close();
            }

            if (pStat != null) {
                pStat.close();
            }

            if (result != null) {
                result.close();
            }
        } catch (SQLException e) {
            //Log.e("Error", "Connection close has some problem.", e);
        }
    }

    public Login loginDB(String userName, String password) throws Exception {

        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        Login login = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(loginQuery);
            pStat.setString(1, userName);
            pStat.setString(2, password);
            result = pStat.executeQuery();
            if (result != null && result.next()) {
                login = new Login();
                //reset the DB URL, user and password
                login.societyName = result.getString(4);
                login.societyId = result.getString(5);
                login.isAdmin = result.getBoolean(6);
                //init(url, user, pass);
                //Reinitialize the factory class, so always new instance will use these configuration.
                //SocietyHelpDatabaseFactory.init(url, user, pass);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return login;
    }

    public void activityLoggingDB(Object activity) throws Exception {

        if (activity instanceof UserActivity) {
            UserActivity objActivity = (UserActivity) activity;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                pStat = con.prepareStatement(activityLoggingQuery);
                pStat.setString(1, objActivity.userName);
                pStat.setString(2, objActivity.mobileNo);
                pStat.setString(3, objActivity.activity.name());
                pStat.setString(4, objActivity.comment);
                pStat.setDate(5, new Date(System.currentTimeMillis()));
                res = pStat.executeQuery();

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public boolean isStatementAlreadyProcessedDB(String monthlyStatementFileName) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(isStatementProcessedQuery);
            pStat.setString(1, monthlyStatementFileName);
            res = pStat.executeQuery();
            if (res != null && res.next()) return true;

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
        return false;
    }

    public boolean uploadMonthlyTransactionsDB(Object transactions) throws Exception {

        if (transactions instanceof BankStatement) {
            BankStatement bankStatement = (BankStatement) transactions;
            Connection con = null;
            PreparedStatement pStat = null;

            try {
                con = getDBInstance();

                //cleanTransactionStagingDataQuery
                System.out.println("Clearing Transaction Staging table data.");
                pStat = con.prepareStatement(cleanTransactionStagingDataQuery);
                pStat.execute();
                System.out.println("Cleared Transaction Staging table data.");
                
                pStat = con.prepareStatement(transactionStagingQuery);
                for (SocietyHelpTransaction t : bankStatement.allTransactions) {
                	if(t.name == null) continue;
                	System.out.println("Transaction : " + t);
                    pStat.setInt(1, t.srNo);
                    pStat.setString(2, t.name);
                    pStat.setFloat(3, t.amount);
                    if (t.transactionDate != null)
                        pStat.setDate(4, new Date(t.transactionDate.getTime()));
                    else pStat.setDate(4, new Date(System.currentTimeMillis()));
                    pStat.setString(5, t.transactionFlow);
                    pStat.setString(6, t.type);
                    pStat.setString(7, t.reference);
                    pStat.setString(8, bankStatement.uploadedLoginId);
                    pStat.addBatch();
                    pStat.clearParameters();
                }

                System.out.println("Executing Batch operations");
                pStat.executeBatch();
                System.out.println("Executed Batch operations");
                saveStatementProcessedDB(bankStatement.bankStatementFileName, bankStatement.uploadedLoginId);

                return true;
            } catch (Exception e) {
            	e.printStackTrace();
                throw e;
            } finally {
                close(con, pStat, null);
            }

        }
        return false;
    }

    public List<StagingTransaction> getAllStaggingTransaction() throws Exception {
        List<StagingTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            /*
            SELECT `Transaction_ID`, `StatementID`, `Name`, `Amount`, `Transaction_Date`,
			`Transaction_Flow`, `Transaction_Mode`, `Transaction_Reference`,
			`Upload_Date`, `Uploaded_LoginId` FROM `Transactions_Staging_Data`
			*/
            connection = getDBInstance();
            pStat = connection.prepareStatement(allTransactionStagingDataQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                StagingTransaction t = new StagingTransaction();
                t.transactionId = result.getInt(1);
                t.srNo = result.getInt(2);
                t.name = result.getString(3);
                t.amount = result.getFloat(4);
                t.transactionDate = result.getDate(5);
                t.transactionFlow = result.getString(6);
                t.type = result.getString(7);
                t.reference = result.getString(8);
                t.updloadedDate = result.getTimestamp(9);
                t.uploadedBy = result.getString(10);
                t.verifiedBy = "";
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }


    public void saveStatementProcessedDB(String monthlyStatementFileName, String loginId) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(statementProcessedQuery);
            pStat.setString(1, monthlyStatementFileName);
            pStat.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            pStat.setString(3, loginId);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public void insertRawData(List<String> data) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(insertRawDataQuery);
            for (String d : data) {
                pStat.setString(1, d);
                pStat.addBatch();
                pStat.clearParameters();
            }
            pStat.executeBatch();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }

    }

    public List<String> showRawData() throws Exception {
        List<String> allRawData = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectAllRawQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                allRawData.add(result.getString(1));
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return allRawData;
    }

    public void deleteAllRawData() throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(deleteAllRawQuery);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }

    }

    public List<UserDetails> getAllUsers() throws Exception {
        List<UserDetails> users = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(allUsersQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                UserDetails u = new UserDetails();
                u.userId = result.getString(1);
                u.loginId = result.getString(2);
                u.userType = result.getString(3);

                u.isActive = result.getInt(4) == 1 ? true : false;
                u.flatId = result.getString(5);
                u.userName = result.getString(6);

                u.nameAlias = result.getString(7);
                u.mobileNo = result.getLong(8);
                u.mobileNoAlternative = result.getLong(9);

                u.emailId = result.getString(10);
                u.address = result.getString(11);
                u.flatJoinDate = result.getDate(12);

                u.flatLeftDate = result.getDate(13);

                users.add(u);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return users;
    }
	
	public List<UserDetails> getAllSocietyUsers(String societyId) throws Exception {
		List<UserDetails> users = new ArrayList<>();
		Connection connection = null;
		PreparedStatement pStat = null;
		ResultSet result = null;
		try {
			connection = getDBInstance();
			pStat = connection.prepareStatement(allSocietyUsersQuery);
			pStat.setString(1, societyId);
			result = pStat.executeQuery();
			while (result.next()) {
				UserDetails u = new UserDetails();
				u.userId = result.getString(1);
				u.loginId = result.getString(2);
				u.userType = result.getString(3);
				
				u.isActive = result.getInt(4) == 1 ? true : false;
				u.flatId = result.getString(5);
				u.userName = result.getString(6);
				
				u.nameAlias = result.getString(7);
				u.mobileNo = result.getLong(8);
				u.mobileNoAlternative = result.getLong(9);
				
				u.emailId = result.getString(10);
				u.address = result.getString(11);
				u.flatJoinDate = result.getDate(12);
				
				u.flatLeftDate = result.getDate(13);
				
				users.add(u);
			}
			
		} catch (Exception e) {
			throw e;
		} finally {
			close(connection, pStat, result);
		}
		return users;
	}

    public List<ExpenseType> getExpenseType() throws Exception {
        List<ExpenseType> types = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(expenseTypeQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                ExpenseType t = new ExpenseType();
                t.expenseTypeId = result.getInt(1);
                t.type = result.getString(2);
                t.transactionPriority = result.getInt(3);
                types.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return types;
    }

    public void errorLogging(String data) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(errorLoggingQuery);
            pStat.setString(1, data);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }


    public List<String> getAllTransactionStagingUsers() throws Exception {
        List<String> uniqueNames = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(allTransactionStagingUsersQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                uniqueNames.add(result.getString(1));
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return uniqueNames;
    }

    public void setAliasOfUserId(String userId, String alias) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(updateAliasUserId);
            pStat.setString(1, alias);
            pStat.setString(2, userId);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public void createUserLogin(Object oLogin) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;
        try {
            if(oLogin instanceof Login) {
                Login login = (Login) oLogin;
                con = getDBInstance();
                pStat = con.prepareStatement(createLoginQuery);
                pStat.setString(1, login.loginId);
                pStat.setString(2, login.password);
                pStat.setBoolean(3, login.isAdmin);
                pStat.setString(4, login.societyId);
                pStat.executeUpdate();
            }
        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public void createUserLogin(List<String> userIds, String adminLoginId) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;
        RandomString rStr = new RandomString(4);
        try {
            con = getDBInstance();
            pStat = con.prepareStatement(createLoginQuery);
            for (String d : userIds) {
                pStat.setString(1, d);
                pStat.setString(2, rStr.nextString());
                pStat.setString(3, adminLoginId);
                pStat.addBatch();
                pStat.clearParameters();
            }
            pStat.executeBatch();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public void addFlatDetails(Object oflat) throws Exception {

        if (oflat instanceof Flat) {
            Flat flat = (Flat) oflat;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                pStat = con.prepareStatement(insertFlatDetailsQuery);
                pStat.setString(1, flat.flatId);
                pStat.setString(2, flat.flatNumber);
                pStat.setInt(3, flat.area);
                pStat.setFloat(4, flat.maintenanceAmount);
                pStat.setString(5, flat.block);
                pStat.setInt(6,flat.societyId);

                pStat.executeUpdate();

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public void addFlatDetails(List<Flat> flatList) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;
        try {
            con = getDBInstance();
            pStat = con.prepareStatement(insertFlatDetailsQuery);
            for (Flat flat : flatList) {
                pStat.setString(1, flat.flatId);
                pStat.setString(2, flat.flatNumber);
                pStat.setInt(3, flat.area);
                pStat.setFloat(4, flat.maintenanceAmount);
                pStat.setString(5, flat.block);
                pStat.addBatch();
                pStat.clearParameters();
            }
            pStat.executeBatch();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public List<Login> getAllLogin(String loginId) throws Exception {
        List<Login> logins = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			SELECT `Login_Id`, `Password`, `Status`, `Authorised_Activity` FROM `sql6134070`.`Login` LIMIT 0, 1000;
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectAllLoginQuery);
            pStat.setString(1, loginId);
            result = pStat.executeQuery();
            while (result.next()) {
                Login l = new Login();
                l.loginId = result.getString(1);
                l.password = result.getString(2);
                l.status = result.getBoolean(3);
                l.societyId = result.getString(4);
                logins.add(l);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return logins;
    }

    public List<Flat> getAllFlats() throws Exception {
        List<Flat> flats = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			f.Flat_Id,f.Flat_Number,f.Area,f.Maintenance_Amount," +
					"f.Block_Number,f.Status,group_concat(ud.Name, '')" +

			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectAllFlatQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                Flat l = new Flat();
                l.flatId = result.getString(1);
                l.flatNumber = result.getString(2);
                l.area = result.getInt(3);
                l.maintenanceAmount = result.getFloat(4);
                l.block = result.getString(5);
                l.status = result.getBoolean(6);
                l.owner = result.getString(7);
                flats.add(l);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return flats;
    }

    public List<Flat> getAllFlatsInSociety(String societyId) throws Exception {
        List<Flat> flats = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			f.Flat_Id,f.Flat_Number,f.Area,f.Maintenance_Amount," +
					"f.Block_Number,f.Status,group_concat(ud.Name, '')" +

			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectAllFlatInSocietyQuery);
            pStat.setString(1, societyId);
            result = pStat.executeQuery();
            while (result.next()) {
                Flat l = new Flat();
                l.flatId = result.getString(1);
                l.flatNumber = result.getString(2);
                l.area = result.getInt(3);
                l.maintenanceAmount = result.getFloat(4);
                l.block = result.getString(5);
                l.status = result.getBoolean(6);
                flats.add(l);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return flats;
    }

    public void addUserDetails(Object oflat) throws Exception {

        if (oflat instanceof UserDetails) {
            UserDetails ud = (UserDetails) oflat;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                pStat = con.prepareStatement(insertUserDetailsQuery);
				/*
		        User_Id, User_Type, Flat_Id,
                Name, Name_Alias, Mobile_No,
                Moble_No_Alternate,Email_Id,Address,
                Flat_Join_Date,Flat_Left_Date, Login_Id)
				 */
                pStat.setString(1, ud.userId);
                pStat.setString(2, ud.userType);
                pStat.setString(3, ud.flatId);

                pStat.setString(4, ud.userName);
                pStat.setString(5, ud.nameAlias);
                pStat.setLong(6, ud.mobileNo);

                pStat.setLong(7, ud.mobileNoAlternative);
                pStat.setString(8, ud.emailId);
                pStat.setString(9, ud.address);
                pStat.setDate(10, ud.flatJoinDate == null ? new Date(System.currentTimeMillis()) : ud.flatJoinDate);
                pStat.setDate(11, ud.flatLeftDate == null ? new Date(System.currentTimeMillis()) : ud.flatLeftDate);
                pStat.setString(12, ud.loginId);

                StringBuilder au = new StringBuilder();
                boolean isFirst = true;
                for (SocietyAuthorization.Type auT : ud.sAuthorizations) {
                    if (isFirst) {
                        au.append(auT.ordinal());
                        isFirst = false;
                    } else au.append(",").append(auT.ordinal());
                }
                pStat.setString(13, au.toString());
                pStat.executeUpdate();

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public void addUserDetails(List<UserDetails> userdetailList) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(insertUserDetailsQuery);

            for (UserDetails ud : userdetailList) {
                pStat.setString(1, ud.userId);
                pStat.setString(2, ud.userType);
                pStat.setString(3, ud.flatId);

                pStat.setString(4, ud.userName);
                pStat.setString(5, ud.nameAlias);
                pStat.setLong(6, ud.mobileNo);

                pStat.setLong(7, ud.mobileNoAlternative);
                pStat.setString(8, ud.emailId);
                pStat.setString(9, ud.address);
                pStat.setDate(10, ud.flatJoinDate == null ? new Date(System.currentTimeMillis()) : ud.flatJoinDate);
                pStat.setDate(11, ud.flatLeftDate == null ? new Date(System.currentTimeMillis()) : ud.flatLeftDate);
                pStat.setString(12, ud.loginId);

                StringBuilder au = new StringBuilder();
                boolean isFirst = true;
                for (SocietyAuthorization.Type auT : ud.sAuthorizations) {
                    if (isFirst) {
                        au.append(auT.ordinal());
                        isFirst = false;
                    } else au.append(",").append(auT.ordinal());
                }
                pStat.setString(13, au.toString());
                pStat.addBatch();
                pStat.clearParameters();
            }

            pStat.executeBatch();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }

    }

    public List<SocietyHelpTransaction> getAllDetailTransaction() throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			Transaction_ID,StatementID,ud.Name,Amount," +
            "Transaction_Date,Transaction_Flow,Transaction_Mode,Transaction_Reference," +
            "ud.User_Id,ud.Flat_Id,Admin_Approved,Admin_Comment," +
                "User_Comment
		    */
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectFinalTransactionQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.transactionId = result.getInt(1);
                t.srNo = result.getInt(2);
                t.name = result.getString(3);
                t.amount = result.getFloat(4);
                t.transactionDate = result.getDate(5);
                t.transactionFlow = result.getString(6);
                t.type = result.getString(7);
                t.reference = result.getString(8);
                t.userId = result.getString(9);
                t.flatId = result.getString(10);
                t.verifiedBy = "";
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<SocietyHelpTransaction> saveVerifiedTransactionsDB(Object transactions) throws Exception {

        List<SocietyHelpTransaction> uploadedTransactions = new ArrayList<>();

        if (transactions instanceof BankStatement) {
            BankStatement bankStatement = (BankStatement) transactions;
            Connection con = null;
            PreparedStatement pStat = null;

            try {
                con = getDBInstance();

                pStat = con.prepareStatement(insertFinalTransactionQuery);
                for (SocietyHelpTransaction t : bankStatement.allTransactions) {
                    if (t.userId != null) {
                        pStat.setFloat(1, t.amount);
                        if (t.transactionDate != null)
                            pStat.setDate(2, new Date(t.transactionDate.getTime()));
                        else pStat.setDate(2, new Date(System.currentTimeMillis()));
                        pStat.setString(3, t.transactionFlow);
                        pStat.setString(4, t.type);
                        pStat.setString(5, t.reference);
                        pStat.setString(6, t.userId);
                        pStat.setString(7, t.flatId);
                        pStat.setString(8, t.verifiedBy);
                        pStat.setBoolean(9, t.splitted);
                        pStat.setLong(10, autoSplitId);	
                        pStat.addBatch();
                        pStat.clearParameters();
                        uploadedTransactions.add(t);
                    }
                }

                System.out.println("Executing Batch operations");
                pStat.executeBatch();
                System.out.println("Executed Batch operations");

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, null);
            }

        }
        return uploadedTransactions;
    }


    public void loadTransactionsInVerifiedDB(List<SocietyHelpTransaction> listTransaction) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet rs = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(insertFinalTransactionQuery, Statement.RETURN_GENERATED_KEYS);

            for (SocietyHelpTransaction t : listTransaction) {
                if (t != null && t.userId != null) {

                    pStat.setFloat(1, t.amount);
                    if (t.transactionDate != null)
                        pStat.setDate(2, new Date(t.transactionDate.getTime()));
                    else pStat.setDate(2, new Date(System.currentTimeMillis()));
                    pStat.setString(3, t.transactionFlow);
                    pStat.setString(4, t.type);
                    pStat.setString(5, FlatWisePayable.InitialDataLoad);
                    pStat.setString(6, t.userId);
                    pStat.setString(7, t.flatId);
                    pStat.setString(8, t.verifiedBy);
                    pStat.setBoolean(9, t.splitted);
                    pStat.setLong(10, autoSplitId);
                    pStat.addBatch();
                    pStat.clearParameters();
                }
            }

            pStat.executeBatch();

            rs = pStat.getGeneratedKeys();
            int i = 0;
            for (; rs.next(); ) {
                listTransaction.get(i++).transactionId = rs.getInt(1);
            }
            rs.close();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, rs);
        }
    }

    public List<SocietyHelpTransaction> getMyTransactions(String userId) throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(myTransactionsQuery);
            pStat.setString(1, userId);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
               SELECT `Transaction_ID`, `Amount`, `Transaction_Date`,
               `Transaction_Flow`, `Transaction_Mode`, `Transaction_Reference`,
               `Flat_Id`, `Admin_Comment`,`User_Comment`
               FROM Transactions_Verified
                 */
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.transactionId = result.getInt(1);
                t.amount = result.getFloat(2);
                t.transactionDate = result.getDate(3);
                t.transactionFlow = result.getString(4);
                t.type = result.getString(5);
                t.reference = result.getString(6);
                t.flatId = result.getString(7);
                t.verifiedBy = result.getString(8);
                t.splitted = result.getBoolean(9);
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }


    public float myDue(String flatId) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(myDueQuery);
            pStat.setString(1, flatId);
            pStat.setString(2, flatId);
            pStat.setString(3, flatId);
            pStat.setString(4, flatId);
            res = pStat.executeQuery();
            if (res != null && res.next()) {
                return res.getFloat("MyDue");
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
        return 0f;
    }

    public UserDetails getMyDetails(String loginId) throws Exception {

        UserDetails u = null;
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(myDetailsQuery);
            pStat.setString(1, loginId);
            result = pStat.executeQuery();
            if (result.next()) {
                u = new UserDetails();
                u.userId = result.getString(1);
                u.loginId = result.getString(2);
                u.userType = result.getString(3);

                u.isActive = result.getInt(4) == 1 ? true : false;
                u.flatId = result.getString(5);
                u.userName = result.getString(6);

                u.nameAlias = result.getString(7);
                u.mobileNo = result.getLong(8);
                u.mobileNoAlternative = result.getLong(9);

                u.emailId = result.getString(10);
                u.address = result.getString(11);
                u.flatJoinDate = result.getDate(12);

                u.flatLeftDate = result.getDate(13);

                String userAuthIds = result.getString(14);

                if (userAuthIds != null)
                    for (String authId : userAuthIds.split(","))
                        u.sAuthorizations.add(SocietyAuthorization.Type.values()[Integer.valueOf(authId)]);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return u;
    }


    public void addMonthlyMaintenance(Object objFwp) throws Exception {

        if (objFwp instanceof FlatWisePayable) {

            FlatWisePayable fwp = (FlatWisePayable) objFwp;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                if (fwp.flatId.equals(CONST_LIST_STR_ALL_FLATS)) {
                    if (fwp.expenseType.equals(ExpenseType.ExpenseTypeConst.Monthly_Maintenance)) {
                        pStat = con.prepareStatement(addlatMaintenancePayablesQuery);
                        pStat.setInt(1, fwp.month);
                        pStat.setInt(2, fwp.year);
                        pStat.setInt(3, fwp.expenseType.ordinal());
                        pStat.executeUpdate();
                    } else {
                        pStat = con.prepareStatement(addAllFlatPayablesQuery);
                        pStat.setFloat(1, fwp.amount);
                        pStat.setInt(2, fwp.month);
                        pStat.setInt(3, fwp.year);
                        pStat.setInt(4, fwp.expenseType.ordinal());
                        pStat.executeUpdate();
                    }
                } else {
                    pStat = con.prepareStatement(addSingleFlatPayablesQuery);
                    pStat.setString(1, fwp.flatId);
                    pStat.setFloat(2, fwp.amount);
                    pStat.setInt(3, fwp.month);
                    pStat.setInt(4, fwp.year);
                    pStat.setInt(5, fwp.expenseType.ordinal());
                    pStat.setString(6, fwp.comments);
                    pStat.setInt(7, fwp.paymentStatus.ordinal());

                    pStat.executeUpdate();
                }

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public List<SocietyHelpTransaction> getFlatWiseTransactions(String flatId) throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            connection = getDBInstance();
            pStat = connection.prepareStatement(flatWiseTransactionsQuery);
            pStat.setString(1, flatId);
            result = pStat.executeQuery();
            while (result.next()) {
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.flatId = result.getString(1);
                t.transactionFlow = result.getString(2);
                t.amount = result.getFloat(3);
                t.transactionDate = result.getDate(4);
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<Login> getAllAssignedLogin() throws Exception {
        List<Login> logins = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			SELECT `Login_Id`, `Password`, `Status`, `Authorised_Activity` FROM `sql6134070`.`Login` LIMIT 0, 1000;
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(selectAllAssignedLoginIdsQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                Login l = new Login();
                l.loginId = result.getString(1);
                logins.add(l);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return logins;
    }

    public void addUserCashPaymentDB(Object payment) throws Exception {
        if (payment instanceof UserPaid) {

            UserPaid userPaid = (UserPaid) payment;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                pStat = con.prepareStatement(addUserPaymentQuery);

                pStat.setString(1, userPaid.userId);
                pStat.setString(2, userPaid.flatId);
                pStat.setFloat(3, userPaid.amount);
                pStat.setDate(4, userPaid.expendDate);
                pStat.setString(5, userPaid.userComment);
                pStat.setInt(6, userPaid.expenseType.ordinal());

                pStat.executeUpdate();

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
    }

    public List<UserPaid> getUnVerifiedUserCashPayment() throws Exception {
        List<UserPaid> payments = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			Payment_ID,User_ID,Flat_ID,Amount,Paid_Date,Type,User_Comment,Admin_Comment
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(unVerifiedCashPaymentByUserQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                UserPaid paid = new UserPaid();
                paid.paymentId = result.getInt(1);
                paid.userId = result.getString(2);
                paid.flatId = result.getString(3);
                paid.amount = result.getFloat(4);
                paid.expendDate = result.getDate(5);
                paid.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(6)];
                paid.userComment = result.getString(7);
                paid.adminComment = result.getString(8);
                payments.add(paid);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return payments;
    }

    public List<UserPaid> getUnSplittedUserCashPayment() throws Exception {
        List<UserPaid> payments = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			Payment_ID,User_ID,Flat_ID,Amount,Paid_Date,Type,User_Comment,Admin_Comment
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(unSplittedCashPaymentByUserQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                UserPaid paid = new UserPaid();
                paid.paymentId = result.getInt(1);
                paid.userId = result.getString(2);
                paid.flatId = result.getString(3);
                paid.amount = result.getFloat(4);
                paid.amountInitial = paid.amount;
                paid.expendDate = result.getDate(5);
                paid.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(6)];
                paid.userComment = result.getString(7);
                paid.adminComment = result.getString(8);
                payments.add(paid);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return payments;
    }

    public List<ApartmentExpense> getUnSplittedApartmentCashExpense() throws Exception {
        List<ApartmentExpense> payments = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(unSplittedApartmentExpensePaymentQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                ApartmentExpense paid = new ApartmentExpense();
                paid.apartmentCashExpenseId = result.getInt(1);
                paid.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(2)];
                paid.amount = result.getFloat(3);
                paid.amountInitial = paid.amount;
                paid.expendDate = result.getDate(4);
                paid.expendByUserId = result.getString(5);
                paid.isVerified = result.getBoolean(6);
                paid.verifiedBy = result.getString(7);
                paid.expendyComment = result.getString(8);
                paid.adminComment = result.getString(9);
                paid.splitted = result.getBoolean(10);
                payments.add(paid);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return payments;
    }

    public void saveVerifiedCashPayment(String userId, String paymentIds) throws Exception {
        Connection con = null;
        Statement stat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            con.setAutoCommit(false); //transaction block start
            //TODO add here code for
            // Insert into Transactions_BalanceSheet table and get the Balance_Sheet_Transaction_ID id
            // Update User_paid verified state
            // Flat_Wise_Payable_Paid_Mapping map payable to paid

            stat = con.createStatement();
            String sqlQuery = "update User_Paid set Verified=1, Verified_by='"
                    + userId + "' where Payment_Id in (" + paymentIds + ")";
            stat.executeUpdate(sqlQuery);

            stat = con.createStatement();
            sqlQuery = "update User_Paid set Verified=1, Verified_by='"
                    + userId + "' where Payment_Id in (" + paymentIds + ")";
            stat.executeUpdate(sqlQuery);

            con.commit(); //transaction block end

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, stat, res);
        }
    }

    private List<FlatWisePayable> getInitialPayables(List<FlatWisePayable> iPayables, List<TransactionOnBalanceSheet> iPaids){
    	List<FlatWisePayable> payables = new ArrayList<FlatWisePayable>();
    	
    	for(FlatWisePayable fwp : iPayables) {
    		for(TransactionOnBalanceSheet tbs : iPaids) {
    			if(tbs.flatId.endsWith(fwp.flatId) && tbs.amount != fwp.amount) {
    				if(fwp.amount > tbs.amount) {
    					fwp.amount = fwp.amount - tbs.amount;  
    					fwp.amountInitial = fwp.amount;
    					fwp.paymentStatus = PaymentStatusConst.Initial_Load_Data;
    					payables.add(fwp);
    				}
    			}
    		}
    	}
    	return payables;
    }
    
    private List<TransactionOnBalanceSheet> getInitialAdvance(List<FlatWisePayable> iPayables, List<TransactionOnBalanceSheet> iPaids){
    	List<TransactionOnBalanceSheet> paid = new ArrayList<TransactionOnBalanceSheet>();
    	
    	for(FlatWisePayable fwp : iPayables) {
    		for(TransactionOnBalanceSheet tbs : iPaids) {
    			if(tbs.flatId.endsWith(fwp.flatId) && tbs.amount != fwp.amount) {
    				if(tbs.amount > fwp.amount) {
    					tbs.amount = tbs.amount - fwp.amount;
    					tbs.flatWisePayableID = -2;
    					paid.add(tbs);
    				}
    			}
    		}
    	}
    	return paid;
    }
    
    public List<UserPaid> generateSplittedTransactionsFlatWise() throws Exception {
        //If user has multiple flats? How to solve
        //Validate one user per flat
        List<UserPaid> splittedPaidAmountFlatWise = new ArrayList<>();
        try {
        	//Pick all initial payable amount flatwise
        	List<FlatWisePayable> initalPayables = getInitialLoadPayableAmountFlatWise();
        	//Pick all inital paid amount flatwise
        	List<TransactionOnBalanceSheet> initalPaid = initialPaidFlatWise();
            // pick all flat wise payables
            List<SocietyHelpTransaction> unSplitedTransactions = getUnSplittedCreditTransactions();
            // pick user un splitted cash payment
            List<UserPaid> unSplitedUserCashPayment = getUnSplittedUserCashPayment();
            // pick all flat wise advance payment
            List<TransactionOnBalanceSheet> advancePayment = getAdvancePayment();
            //Flat wise advance payment after initial load data
            List<TransactionOnBalanceSheet> advanceIntialPayment = getInitialAdvance(initalPayables, initalPaid);
            //advancePayment.addAll(advanceIntialPayment);
            // all payables
            List<FlatWisePayable> unPaidAmountFlatWise = getUnPaidAmountFlatWise();
            //Flat wise payable after initial load data
            List<FlatWisePayable> unPaidInitialAmountFlatWise = getInitialPayables(initalPayables, initalPaid);
            //unPaidAmountFlatWise.addAll(unPaidInitialAmountFlatWise);
            // Final Balance Sheet Transactions
            List<TransactionOnBalanceSheet> readyToAddInBalanceSheet = new ArrayList<>();
            
            
            boolean exactMatchFound;
            boolean payableTotlyPaid;
            
            for (FlatWisePayable payable : unPaidInitialAmountFlatWise) {
                //Check for exact match
                exactMatchFound = isExactMatchFoundInAdvancePayment(payable, advancePayment, readyToAddInBalanceSheet);
                if (!exactMatchFound)
                    exactMatchFound = isExactMatchFoundInBankTransactionPayment(payable, unSplitedTransactions, readyToAddInBalanceSheet);
                if (!exactMatchFound)
                    exactMatchFound = isExactMatchFoundInUserCashPaid(payable, unSplitedUserCashPayment, readyToAddInBalanceSheet);
                //Now if it extact match found, then pick next payable
                if (exactMatchFound) continue;

                //Now split the already paid amount
                payableTotlyPaid = totalPayableFoundInAdvancePayment(payable, advancePayment, readyToAddInBalanceSheet);
                if (!payableTotlyPaid)
                    payableTotlyPaid = totalPayableFoundInBankTransactionPayment(payable, unSplitedTransactions, readyToAddInBalanceSheet);
                if (!payableTotlyPaid)
                    payableTotlyPaid = totalPayableFoundInUserCashPaid(payable, unSplitedUserCashPayment, readyToAddInBalanceSheet);
            }
            
            for (FlatWisePayable payable : unPaidAmountFlatWise) {
            	exactMatchFound = isExactMatchFoundInAdvancePayment(payable, advanceIntialPayment, readyToAddInBalanceSheet);
                //Check for exact match
            	if (!exactMatchFound)
            		exactMatchFound = isExactMatchFoundInAdvancePayment(payable, advancePayment, readyToAddInBalanceSheet);
                if (!exactMatchFound)
                    exactMatchFound = isExactMatchFoundInBankTransactionPayment(payable, unSplitedTransactions, readyToAddInBalanceSheet);
                if (!exactMatchFound)
                    exactMatchFound = isExactMatchFoundInUserCashPaid(payable, unSplitedUserCashPayment, readyToAddInBalanceSheet);
                //Now if it extact match found, then pick next payable
                if (exactMatchFound) continue;

                //Now split the already paid amount
                payableTotlyPaid = totalPayableFoundInAdvancePayment(payable, advancePayment, readyToAddInBalanceSheet);
                if (!payableTotlyPaid)
                    payableTotlyPaid = totalPayableFoundInBankTransactionPayment(payable, unSplitedTransactions, readyToAddInBalanceSheet);
                if (!payableTotlyPaid)
                    payableTotlyPaid = totalPayableFoundInUserCashPaid(payable, unSplitedUserCashPayment, readyToAddInBalanceSheet);
            }
            addToBalanceSheet(readyToAddInBalanceSheet, unSplitedUserCashPayment, unSplitedTransactions, unPaidAmountFlatWise);


        } catch (Exception e) {
            //Log.e("error", "Error while generating splitted transactions.", e);
            throw e;
        }
        return splittedPaidAmountFlatWise;
    }

    public void rollbackAutoSplit()
    {
    	Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
        	con = getDBInstance();
            con.setAutoCommit(false); //transaction block start
     
            pStat = con.prepareStatement(rollbackUserPaidDataQuery);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(rollbackTransactionVerifiedQuery);
            pStat.execute();
            pStat.close();
            
            pStat = con.prepareStatement(rollbackTransactionsBalanceSheet);
            pStat.execute();
            pStat.close();
            
            pStat = con.prepareStatement(rollbackFlatWisePayable);
            pStat.execute();
            pStat.close();
            
            pStat = con.prepareStatement(rollbackApartmentExpense);
            pStat.execute();
            pStat.close();
            
            pStat = con.prepareStatement(rollbackApartmentEarning);
            pStat.execute();
            pStat.close();
            
            pStat = con.prepareStatement(insertFromBalanceRollbackDataQuery);
            pStat.execute();
            pStat.close();
            
            pStat = con.prepareStatement(deleteAllBackupDataQuery);
            pStat.execute();
            pStat.close();
            
            con.commit(); //transaction block end
            
        } catch (Exception e) {
            e.printStackTrace();
        	//Log.e("Error", "Message - " + e.getMessage());
            //throw e;
        } finally {
        	autoSplitId = 0;
            close(con, pStat, res);
        }
    	
    	
    }
    
    public boolean addToBalanceSheet(List<TransactionOnBalanceSheet> readyToAddInBalanceSheet,
                                     List<UserPaid> unSplitedUserCashPayment, List<SocietyHelpTransaction> unSplitedTransactions,
                                     List<FlatWisePayable> unPaidAmountFlatWise) {
        // All this should happen in a only SQL transaction.
        // Insert in transactions_balance sheet (all splitted transaction)
        // Insert in flat_wise_payable_paid_mapping table
        // Update transactions_verified transaction splitted
        // Update user_paid transaction splitted

        List<TransactionOnBalanceSheet> insertTransactions = tobeInsertedTransactionInBalanceSheet(readyToAddInBalanceSheet);
        List<TransactionOnBalanceSheet> updateTransaction = tobeUpdatedTransactionInBalanceSheet(readyToAddInBalanceSheet);
        //do not make entry of advance update transaction
        List<TransactionOnBalanceSheet> advancePayment = toBeInsertAsAdvancePayment(unSplitedUserCashPayment, unSplitedTransactions);

        
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
        	autoSplitId = System.currentTimeMillis();
        	deleteAutoSplitIdDB();
        	insertAutoSplitIdDB(autoSplitId);
        	
        	deleteAllBackupData();
        	insertToRollbackDataInBatch(updateTransaction);
        	
            List<SocietyHelpTransaction> debitTransactions = getUnSplittedDebitTransactions();
            List<TransactionOnBalanceSheet> tobeInsertedDebitTransactions = convertDebitedTransactionToBalanceSheetTransaction(debitTransactions);
            List<ApartmentExpense> apartmentCashExpense = getUnSplittedApartmentCashExpense();
            List<TransactionOnBalanceSheet> tobeInsertedApartmentCashExpense = convertCashExpenseToBalanceSheetTransaction(apartmentCashExpense);

            con = getDBInstance();
            con.setAutoCommit(false); //transaction block start

            insertToPreparedStatementInBatch(con, insertTransactions);
            updateToPreparedStatementInBatch(con, updateTransaction);
            insertToPreparedStatementInBatch(con, advancePayment);
            insertToPreparedStatementInBatch(con, tobeInsertedDebitTransactions); //insert debit transactions - expense
            insertToPreparedStatementInBatch(con, tobeInsertedApartmentCashExpense); //insert cash expense transactions

            updateSplittedUserCash(con, insertTransactions);
            updateSplittedBankStatement(con, insertTransactions);
            updateSplittedBankStatement(con, tobeInsertedDebitTransactions); //update debit transactions
            updateFlatWisePayableStatus(con, unPaidAmountFlatWise);
            updateAdvanceBankStatement(con, advancePayment);
            updateSplittedApartementCashExpense(con, apartmentCashExpense); //update Apartment expense table

            con.commit(); //transaction block end
            
        } catch (Exception e) {
        	e.printStackTrace();
            //Log.e("Error", "Message - " + e.getMessage());
            //throw e;
        } finally {
        	autoSplitId = 0;
            close(con, pStat, res);
        }
        return false;
    }

    public void insertToPaymentToPaidFlatwise(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception {
        PreparedStatement pStat = con.prepareStatement(insertFlatWisePayableToPaidMapping);
        for (TransactionOnBalanceSheet t : insertTransactions) {
            pStat.setInt(1, t.flatWisePayableID);
            pStat.setInt(2, t.balanceSheetTransactionID);
            pStat.addBatch();
            pStat.clearParameters();
        }
        pStat.executeBatch();
    }

    public void updateSplittedBankStatement(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception {
        PreparedStatement pStat = con.prepareStatement(updateBankTransactionSpillted);
        for (TransactionOnBalanceSheet t : insertTransactions) {
            if (t.transactionFromBankStatementID > 0) {
            	pStat.setLong(1, autoSplitId);
            	pStat.setInt(2, t.transactionFromBankStatementID);
                pStat.addBatch();
                pStat.clearParameters();
            }
        }
        pStat.executeBatch();
    }


    public void updateAdvanceBankStatement(Connection con, List<TransactionOnBalanceSheet> advanceTransactions) throws Exception {
        PreparedStatement pStat = con.prepareStatement(updateBankTransactionSpillted);
        for (TransactionOnBalanceSheet t : advanceTransactions) {
            if (t.transactionFromBankStatementID > 0) {
            	pStat.setLong(1, autoSplitId);
                pStat.setInt(2, t.transactionFromBankStatementID);
                pStat.addBatch();
                pStat.clearParameters();
            }
        }
        pStat.executeBatch();

        PreparedStatement pStatCash = con.prepareStatement(updateUserCashSpitted);
        for (TransactionOnBalanceSheet t : advanceTransactions) {
            if (t.userCashPaymentID > 0) {
            	pStatCash.setLong(1, autoSplitId);
                pStatCash.setInt(2, t.userCashPaymentID);
                pStatCash.addBatch();
                pStatCash.clearParameters();
            }
        }
        pStatCash.executeBatch();
    }

    public void updateSplittedUserCash(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception {
        PreparedStatement pStat = con.prepareStatement(updateUserCashSpitted);
        for (TransactionOnBalanceSheet t : insertTransactions) {

            if (t.userCashPaymentID > 0) {
            	pStat.setLong(1, autoSplitId);
            	pStat.setInt(2, t.userCashPaymentID);
                pStat.addBatch();
                pStat.clearParameters();
            }
        }
        pStat.executeBatch();
    }

    public void updateSplittedApartementCashExpense(Connection con, List<ApartmentExpense> cashExpense) throws Exception {
        PreparedStatement pStat = con.prepareStatement(updateApartmentCashSpitted);
        for (ApartmentExpense t : cashExpense) {
        	pStat.setLong(1, autoSplitId);
        	pStat.setInt(2, t.apartmentCashExpenseId);
            pStat.addBatch();
            pStat.clearParameters();
        }
        pStat.executeBatch();
    }

    public void updateFlatWisePayableStatus(Connection con, List<FlatWisePayable> flatWisePayables) throws Exception {
        PreparedStatement pStat = con.prepareStatement(updateFlatWisePayableStatus);
        for (FlatWisePayable t : flatWisePayables) {

        	if(!t.paymentStatus.equals(PaymentStatusConst.Initial_Load_Data)) { // Initial payable should not modified. It will be balanced by paid with flat_wise_payable_id '-1'
	            if (java.lang.Float.compare(t.amount, 0) == 0) {
	                	//.
	            	if(t.paymentStatus.equals(PaymentStatusConst.Initial_Load_Advance_Paid)) {
	            		pStat.setInt(1, 5); //Flag for payable is paid by advance initial data load.
	            	}else {
	            		pStat.setInt(1, 2); //means Full_Paid check table User_Payment_Status
	            	}
                	pStat.setLong(2, autoSplitId);
                    pStat.setInt(3, t.paymentId);
                    pStat.addBatch();
                    pStat.clearParameters();

	            } else if (java.lang.Float.compare(t.amountInitial, t.amount) > 0) {
	            	if(t.paymentStatus.equals(PaymentStatusConst.Initial_Load_Advance_Paid)) {
	            		pStat.setInt(1, 5); //Flag for payable is paid by advance initial data load.
	            	}else {
	            		pStat.setInt(1, 3); //means Partial_Paid check table User_Payment_Status
	            	}
	                pStat.setLong(2, autoSplitId);
	                pStat.setInt(3, t.paymentId);
	                pStat.addBatch();
	                pStat.clearParameters();
	            }
        	}
        }
        pStat.executeBatch();
    }

    public String getFlatId(String flatNo)
    {
        try {
            if(flatNo != null) flatNo = flatNo.trim();
            return String.format("Flat_%03d", Integer.parseInt(flatNo));
        }catch (Exception e) {
            e.printStackTrace();
        }
        return "Flat_"+flatNo;
    }

    public void loadUserPaid(List<LoadBhowaInitialData.LoadUserPaid> paidUser, Map<String, String> flatIdUserIdMapping) {
        try {
            List<SocietyHelpTransaction> paidTransactions = new ArrayList<>();
            SocietyHelpTransaction curTransaction;

            for (LoadBhowaInitialData.LoadUserPaid lup : paidUser) {
                for (Date payableDate : lup.dateAmountMapping.keySet()) {
                    curTransaction = new SocietyHelpTransaction();
                    if (lup.dateAmountMapping.get(payableDate).amount == null ||
                            lup.dateAmountMapping.get(payableDate).amount == 0) {
                        //continue;
                        curTransaction.amount = 0;
                    } else {
                        curTransaction.amount = lup.dateAmountMapping.get(payableDate).amount;
                    }
                    curTransaction.flatId = getFlatId(lup.flatNo); //"Flat_" + lup.flatNo;
                    curTransaction.userId = flatIdUserIdMapping.get(curTransaction.flatId);
                    curTransaction.transactionFlow = "Credit";
                    curTransaction.expenseType = lup.dateAmountMapping.get(payableDate).expenseType;
                    curTransaction.verifiedBy = "superadmin";
                    curTransaction.splitted = true;
                    curTransaction.type = "";
                    curTransaction.transactionDate = payableDate;
                    paidTransactions.add(curTransaction);
                }
            }

            loadTransactionsInVerifiedDB(paidTransactions);

            List<TransactionOnBalanceSheet> listBalanceSheetTransaction = new ArrayList<>();

            TransactionOnBalanceSheet curBalanceSheetTransaction;
            for (SocietyHelpTransaction pt : paidTransactions) {

                if (pt.transactionId > 0) {
                    curBalanceSheetTransaction = new TransactionOnBalanceSheet();
                    curBalanceSheetTransaction.transactionFlow = "Credit";
                    curBalanceSheetTransaction.userId = pt.userId;
                    curBalanceSheetTransaction.flatId = pt.flatId;
                    curBalanceSheetTransaction.amount = pt.amount;
                    curBalanceSheetTransaction.isVerifiedByAdmin = true;
                    curBalanceSheetTransaction.transactionFromBankStatementID = pt.transactionId;
                    curBalanceSheetTransaction.expenseType = pt.expenseType;
                    listBalanceSheetTransaction.add(curBalanceSheetTransaction);
                }
            }

            insertToPreparedStatementInBatch(listBalanceSheetTransaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertToPreparedStatementInBatch(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception {
        PreparedStatement pStat = con.prepareStatement(insertToBalanceQuery);
        for (TransactionOnBalanceSheet t : insertTransactions) {
            pStat.setFloat(1, t.amount);
            pStat.setBoolean(2, t.isVerifiedByAdmin);
            pStat.setBoolean(3, t.isVerifiedByUser);
            pStat.setInt(4, t.expenseType.ordinal());
            pStat.setInt(5, t.transactionFromBankStatementID);
            pStat.setInt(6, t.userCashPaymentID);
            pStat.setInt(7, t.transactionExpenseId);
            pStat.setString(8, t.transactionFlow);
            pStat.setInt(9, t.flatWisePayableID);
            pStat.setInt(10, t.apartmentEarningID);
            pStat.setLong(11, autoSplitId);
            pStat.addBatch();
            pStat.clearParameters();
        }
        pStat.executeBatch();
    }

    public void insertToRollbackDataInBatch(List<TransactionOnBalanceSheet> insertTransactions) throws Exception {
    	Connection con = null;
    	PreparedStatement pStat = null;
    	try {
    		con = getDBInstance();
	    	pStat = con.prepareStatement(insertToBalanceRollbackDataQuery);
	        for (TransactionOnBalanceSheet t : insertTransactions) {
	            pStat.setInt(1, t.balanceSheetTransactionID);
	            pStat.addBatch();
	            pStat.clearParameters();
	        }
	        pStat.executeBatch();
	    }
    	catch(Exception e) {
	    	e.printStackTrace();
	    	throw e;
	    }
    	finally {
            close(con, pStat, null);
        }
    }
//insertFromBalanceRollbackDataQuery
    
    public void insertFromRollbackDataToBalanceSheet(List<TransactionOnBalanceSheet> insertTransactions) throws Exception {
    	Connection con = null;
    	PreparedStatement pStat = null;
    	try {
    		con = getDBInstance();
	    	pStat = con.prepareStatement(insertToBalanceRollbackDataQuery);
	        for (TransactionOnBalanceSheet t : insertTransactions) {
	            pStat.setInt(1, t.balanceSheetTransactionID);
	            pStat.addBatch();
	            pStat.clearParameters();
	        }
	        pStat.executeBatch();
	    }
    	catch(Exception e) {
	    	e.printStackTrace();
	    	throw e;
	    }
    	finally {
            close(con, pStat, null);
        }
    }
    
    public void insertToPreparedStatementInBatch(List<TransactionOnBalanceSheet> listT) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(insertToBalanceQuery);

            for (TransactionOnBalanceSheet t : listT) {
                pStat.setFloat(1, t.amount);
                pStat.setBoolean(2, t.isVerifiedByAdmin);
                pStat.setBoolean(3, t.isVerifiedByUser);
                pStat.setInt(4, t.expenseType.ordinal());
                pStat.setInt(5, t.transactionFromBankStatementID);
                pStat.setInt(6, t.userCashPaymentID);
                pStat.setInt(7, t.transactionExpenseId);
                pStat.setString(8, t.transactionFlow);
                pStat.setInt(9, TransactionOnBalanceSheet.initDataLoadFlatWisePayableId);
                pStat.setInt(10, t.apartmentEarningID);
                pStat.setLong(11, autoSplitId);
                pStat.addBatch();
                pStat.clearParameters();
            }
            pStat.executeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(con, pStat, null);
        }
    }

    public void updateToPreparedStatementInBatch(Connection con, List<TransactionOnBalanceSheet> insertTransactions) throws Exception {
        PreparedStatement pStat = con.prepareStatement(updateBalanceSheet_Transaction);
        /*
        SET Expense_Type_Id=? ,Amount=?," +
				"User_Cash_Payment_ID=?, Transaction_From_Bank_Statement_ID=?," +
				"Flat_Wise_Payable_ID=?  WHERE Balance_Sheet_Transaction_Id=?";
         */
        for (TransactionOnBalanceSheet t : insertTransactions) {
            pStat.setInt(1, t.expenseType.ordinal());
            pStat.setFloat(2, t.amount);
            pStat.setInt(3, t.userCashPaymentID);
            pStat.setInt(4, t.transactionFromBankStatementID);
            pStat.setInt(5, t.flatWisePayableID);
            pStat.setInt(6, t.balanceSheetTransactionID);
            pStat.setLong(7, autoSplitId);
            pStat.addBatch();
            pStat.clearParameters();
        }
        pStat.executeBatch();
    }

    public List<TransactionOnBalanceSheet> toBeInsertAsAdvancePayment(List<UserPaid> unSplitedUserCashPayment, List<SocietyHelpTransaction> unSplitedTransactions) {
        List<TransactionOnBalanceSheet> advancePayment = new ArrayList<>();
        for (UserPaid t : unSplitedUserCashPayment) {
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amount = t.amount;
            balanceSheetTranction.expenseType = ExpenseType.ExpenseTypeConst.Advance_Payment;
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            balanceSheetTranction.action = TransactionOnBalanceSheet.DBAction.INSERT;
            balanceSheetTranction.isVerifiedByAdmin = true;
            balanceSheetTranction.flatId = t.flatId;
            balanceSheetTranction.userId = t.userId;
            balanceSheetTranction.userCashPaymentID = t.paymentId;
            advancePayment.add(balanceSheetTranction);
        }

        for (SocietyHelpTransaction t : unSplitedTransactions) {
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amount = t.amount;
            balanceSheetTranction.expenseType = ExpenseType.ExpenseTypeConst.Advance_Payment;
            balanceSheetTranction.transactionFromBankStatementID = t.transactionId;
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            balanceSheetTranction.action = TransactionOnBalanceSheet.DBAction.INSERT;
            balanceSheetTranction.isVerifiedByAdmin = true;
            balanceSheetTranction.flatId = t.flatId;
            balanceSheetTranction.userId = t.userId;
            balanceSheetTranction.transactionFromBankStatementID = t.transactionId;
            advancePayment.add(balanceSheetTranction);
        }

        return advancePayment;
    }

    public List<TransactionOnBalanceSheet> tobeUpdatedTransactionInBalanceSheet(List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {
        List<TransactionOnBalanceSheet> updateTransaction = new ArrayList<>();
        for (TransactionOnBalanceSheet transaction : readyToAddInBalanceSheet) {
            if (transaction.action.equals(TransactionOnBalanceSheet.DBAction.UPDATE))
                updateTransaction.add(transaction);
        }
        return updateTransaction;
    }

    public List<TransactionOnBalanceSheet> tobeInsertedTransactionInBalanceSheet(List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {
        List<TransactionOnBalanceSheet> insertTransaction = new ArrayList<>();
        for (TransactionOnBalanceSheet transaction : readyToAddInBalanceSheet) {
            if (transaction.action.equals(TransactionOnBalanceSheet.DBAction.INSERT))
                insertTransaction.add(transaction);
        }
        return insertTransaction;
    }

    //Bank Monthly Statement Transaction Payment split to payables
    public boolean totalPayableFoundInUserCashPaid(FlatWisePayable payable, List<UserPaid> unSplitedUserCashPayment, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {

        List<UserPaid> consumedPayment = new ArrayList<>();
        for (UserPaid cashTransaction : unSplitedUserCashPayment) {
            if (cashTransaction.flatId == null || !cashTransaction.flatId.equals(payable.flatId))
                continue;
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amountInitial = payable.amount;
            balanceSheetTranction.flatId = payable.flatId;
            //Payable is greater then Paid
            if (payable.amount >= cashTransaction.amount) {
                payable.amount = payable.amount - cashTransaction.amount;
                consumedPayment.add(cashTransaction); //it is totally consume, added in consume, so it will be removed from advancePayment list;
                balanceSheetTranction.amount = cashTransaction.amount;
            }
            //Payable is less then Paid
            else if (payable.amount < cashTransaction.amount) {
                cashTransaction.amount = cashTransaction.amount - payable.amount;
                balanceSheetTranction.amount = payable.amount;
                payable.amount = 0;
            }

            balanceSheetTranction.expenseType = payable.expenseType;
            balanceSheetTranction.userCashPaymentID = cashTransaction.paymentId; //Monthly statement transaction id mapping only. [transactions_verified table]
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            if(payable.paymentStatus.equals(PaymentStatusConst.Initial_Load_Data)) {
            	balanceSheetTranction.flatWisePayableID = -1; //Flag represent as to balance initial load data payment done. 
            }else{
            	balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
            }
            readyToAddInBalanceSheet.add(balanceSheetTranction);

            if (payable.amount == 0) break;
        }

        for (UserPaid consumedTransaction : consumedPayment) {
            unSplitedUserCashPayment.remove(consumedTransaction);
        }
        if (payable.amount == 0) return true; //payable amount is totally paid.

        return false; //payable amount is not totally paid.
    }

    //Bank Monthly Statement Transaction Payment split to payables
    public boolean totalPayableFoundInBankTransactionPayment(FlatWisePayable payable, List<SocietyHelpTransaction> unSplitedTransactions, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {

        List<SocietyHelpTransaction> consumedPayment = new ArrayList<>();
        for (SocietyHelpTransaction bankTransaction : unSplitedTransactions) {
            if (bankTransaction.flatId == null || !bankTransaction.flatId.equals(payable.flatId))
                continue;
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amountInitial = payable.amount;
            balanceSheetTranction.flatId = payable.flatId;
            //Payable is greater then Paid
            if (payable.amount >= bankTransaction.amount) {
                payable.amount = payable.amount - bankTransaction.amount;
                consumedPayment.add(bankTransaction); //it is totally consume, added in consume, so it will be removed from advancePayment list;
                balanceSheetTranction.amount = bankTransaction.amount;
            }
            //Payable is less then Paid
            else if (payable.amount < bankTransaction.amount) {
                bankTransaction.amount = bankTransaction.amount - payable.amount;
                balanceSheetTranction.amount = payable.amount;
                payable.amount = 0;
            }

            balanceSheetTranction.expenseType = payable.expenseType;
            balanceSheetTranction.transactionFromBankStatementID = bankTransaction.transactionId; //Monthly statement transaction id mapping only. [transactions_verified table]
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            if(payable.paymentStatus.equals(PaymentStatusConst.Initial_Load_Data)) {
            	balanceSheetTranction.flatWisePayableID = -1; //Flag represent as to balance initial load data payment done. 
            }else{
            	balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
            }
            readyToAddInBalanceSheet.add(balanceSheetTranction);

            if (payable.amount == 0) break;
        }

        for (SocietyHelpTransaction consumedTransaction : consumedPayment) {
            unSplitedTransactions.remove(consumedTransaction);
        }
        if (payable.amount == 0) return true; //payable amount is totally paid.

        return false; //payable amount is not totally paid.
    }

    //Advance payment split to payables
    public boolean totalPayableFoundInAdvancePayment(FlatWisePayable payable, List<TransactionOnBalanceSheet> advancePayment, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {

        List<TransactionOnBalanceSheet> consumedPayment = new ArrayList<>();
        for (TransactionOnBalanceSheet advanceAmountInBalSheet : advancePayment) {
            if (advanceAmountInBalSheet.flatId == null || !advanceAmountInBalSheet.flatId.equals(payable.flatId))
                continue;
            
            if(advanceAmountInBalSheet .flatWisePayableID == -2 ) {
            	payable.paymentStatus = PaymentStatusConst.Initial_Load_Advance_Paid; //Flag to set initial data has some advance amount, payable will be paid by it.
            }
            
            //Payable is greater then Paid
            if (payable.amount >= advanceAmountInBalSheet.amount) {
                advanceAmountInBalSheet.action = TransactionOnBalanceSheet.DBAction.UPDATE;
                advanceAmountInBalSheet.expenseType = payable.expenseType;
                advanceAmountInBalSheet.amountInitial = payable.amount;
                if(payable.paymentStatus.equals(PaymentStatusConst.Initial_Load_Data)) {
                	advanceAmountInBalSheet.flatWisePayableID = -1; //Flag represent as to balance initial load data payment done. 
                }else{
                	advanceAmountInBalSheet.flatWisePayableID = payable.paymentId;
                }
                payable.amount = payable.amount - advanceAmountInBalSheet.amount;
                readyToAddInBalanceSheet.add(advanceAmountInBalSheet);
                consumedPayment.add(advanceAmountInBalSheet); //it is totally consume, added in consume, so it will be removed from advancePayment list;
            }
            //Payable is less then Paid
            else if (payable.amount < advanceAmountInBalSheet.amount) {
                advanceAmountInBalSheet.amount = advanceAmountInBalSheet.amount - payable.amount;
                advanceAmountInBalSheet.action = TransactionOnBalanceSheet.DBAction.UPDATE;
                advanceAmountInBalSheet.amountInitial = advanceAmountInBalSheet.amount;
                readyToAddInBalanceSheet.add(advanceAmountInBalSheet);

                TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
                balanceSheetTranction.amount = payable.amount;
                balanceSheetTranction.amountInitial = payable.amount;
                balanceSheetTranction.flatId = payable.flatId;
                balanceSheetTranction.expenseType = payable.expenseType;
                balanceSheetTranction.transactionFromBankStatementID = advanceAmountInBalSheet.transactionFromBankStatementID; //Monthly statement transaction id mapping only. [transactions_verified table]
                balanceSheetTranction.userCashPaymentID = advanceAmountInBalSheet.userCashPaymentID; //Mapping of user cash id to this transaction
                balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
                if(payable.paymentStatus.equals(PaymentStatusConst.Initial_Load_Data)) {
                	balanceSheetTranction.flatWisePayableID = -1; //Flag represent as to balance initial load data payment done. 
                }else{
                	balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
                }
                readyToAddInBalanceSheet.add(balanceSheetTranction);
                payable.amount = 0;
                break;
            }
        }

        for (TransactionOnBalanceSheet consumedTransaction : consumedPayment) {
            advancePayment.remove(consumedTransaction);
        }

        if (payable.amount == 0) return true; //payable amount is totally paid.

        return false; //payable amount is not totally paid.
    }

    //Search for exact search in User cash payment
    public boolean isExactMatchFoundInUserCashPaid(FlatWisePayable payable, List<UserPaid> unSplitedUserCashPayment, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {
        UserPaid foundTransaction = null;
        for (UserPaid curTransaction : unSplitedUserCashPayment) {
            if (curTransaction.flatId == null || !curTransaction.flatId.equals(payable.flatId))
                continue;
            if (curTransaction.amount == payable.amount) {
                foundTransaction = curTransaction;
                break;
            }
        }
        if (foundTransaction != null) {
            unSplitedUserCashPayment.remove(foundTransaction);
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amount = payable.amount;
            balanceSheetTranction.amountInitial = payable.amount;
            balanceSheetTranction.expenseType = payable.expenseType;
            balanceSheetTranction.flatId = payable.flatId;
            balanceSheetTranction.userCashPaymentID = foundTransaction.paymentId; //User cash payment transaction id mapping only. [user_paid table]
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            if(payable.paymentStatus.equals(PaymentStatusConst.Initial_Load_Data)) {
            	balanceSheetTranction.flatWisePayableID = -1; //Flag represent as to balance initial load data payment done. 
            }else{
            	balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
            }
            readyToAddInBalanceSheet.add(balanceSheetTranction);
            payable.amount = 0;
            return true;
        }
        return false;
    }

    //Search for exact search in Bank statement transaction
    public boolean isExactMatchFoundInBankTransactionPayment(FlatWisePayable payable, List<SocietyHelpTransaction> unSplitedTransactions, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {
        SocietyHelpTransaction foundTransaction = null;
        for (SocietyHelpTransaction curTransaction : unSplitedTransactions) {
            if (curTransaction.flatId == null || !curTransaction.flatId.equals(payable.flatId))
                continue;
            if (curTransaction.amount == payable.amount) {
                foundTransaction = curTransaction;
                break;
            }
        }
        if (foundTransaction != null) {
            unSplitedTransactions.remove(foundTransaction);
            TransactionOnBalanceSheet balanceSheetTranction = new TransactionOnBalanceSheet();
            balanceSheetTranction.amount = payable.amount;
            balanceSheetTranction.amountInitial = payable.amount;
            balanceSheetTranction.flatId = payable.flatId;
            balanceSheetTranction.expenseType = payable.expenseType;
            balanceSheetTranction.transactionFromBankStatementID = foundTransaction.transactionId; //Monthly statement transaction id mapping only. [transactions_verified table]
            balanceSheetTranction.transactionFlow = CONST_CREDIT_TRANSACTION_FLOW;
            if(payable.paymentStatus.equals(PaymentStatusConst.Initial_Load_Data)) {
            	balanceSheetTranction.flatWisePayableID = -1; //Flag represent as to balance initial load data payment done. 
            }else{
            	balanceSheetTranction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
            }
            readyToAddInBalanceSheet.add(balanceSheetTranction);
            payable.amount = 0;
            return true;
        }
        return false;
    }

    //Search for exact search in Advance payment
    public boolean isExactMatchFoundInAdvancePayment(FlatWisePayable payable, List<TransactionOnBalanceSheet> advancePayment, List<TransactionOnBalanceSheet> readyToAddInBalanceSheet) {


        TransactionOnBalanceSheet foundTransaction = null;
        for (TransactionOnBalanceSheet advanceAmountInBalSheet : advancePayment) {
            if (advanceAmountInBalSheet.flatId == null || !advanceAmountInBalSheet.flatId.equals(payable.flatId))
                continue;
            if (advanceAmountInBalSheet.amount == payable.amount) {
                foundTransaction = advanceAmountInBalSheet;
                break;
            }
        }
        if (foundTransaction != null) {
            advancePayment.remove(foundTransaction);
            if(foundTransaction.flatWisePayableID == -2 ) {
            	payable.paymentStatus = PaymentStatusConst.Initial_Load_Advance_Paid; //Flag to set initial data has some advance amount, payable will be paid by it.
            }
            foundTransaction.expenseType = payable.expenseType;
            if(payable.paymentStatus.equals(PaymentStatusConst.Initial_Load_Data)) {
            	foundTransaction.flatWisePayableID = -1; //Flag represent as to balance initial load data payment done. 
            } else {
	            foundTransaction.flatWisePayableID = payable.paymentId; //Mapping of flat wise payment to balance sheet's transaction id
	        }
            foundTransaction.action = TransactionOnBalanceSheet.DBAction.UPDATE;
            readyToAddInBalanceSheet.add(foundTransaction);
            payable.amount = 0;
            return true;
        }
        return false;
    }

    public SortedSet<FlatWisePayable> getTransactionWiseFlatPayables(SocietyHelpTransaction transaction, List<FlatWisePayable> unPaidAmountFlatWise) {
        SortedSet<FlatWisePayable> flatWisePayables = new TreeSet<>();

        for (FlatWisePayable fp : unPaidAmountFlatWise) {
            if (fp.flatId.equals(transaction.flatId)) flatWisePayables.add(fp);
        }

        return flatWisePayables;
    }


    public List<SocietyHelpTransaction> getUnSplittedCreditTransactions() throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(unSplittedCreditTransactionsQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                SELECT Transaction_ID,Amount,Transaction_Date,Transaction_Flow," +
				"Transaction_Mode,Transaction_Reference,User_Id,Flat_Id," +
				"Verified_By,Splitted " +
				"FROM Transactions_Verified " +
				"where splitted=0";
                 */
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.transactionId = result.getInt(1);
                t.amount = result.getFloat(2);
                t.amountInitial = t.amount;
                t.transactionDate = result.getDate(3);
                t.transactionFlow = result.getString(4);
                t.type = result.getString(5);
                t.reference = result.getString(6);
                t.userId = result.getString(7);
                t.flatId = result.getString(8);
                t.verifiedBy = result.getString(9);
                t.splitted = result.getBoolean(10);
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<TransactionOnBalanceSheet> convertDebitedTransactionToBalanceSheetTransaction(List<SocietyHelpTransaction> tobeInsertedDebitTransactions) {
        List<TransactionOnBalanceSheet> readyToBalanceSheetTransactions = new ArrayList<>();
        for (SocietyHelpTransaction t : tobeInsertedDebitTransactions) {
            TransactionOnBalanceSheet paid = new TransactionOnBalanceSheet();
            paid.transactionFromBankStatementID = t.transactionId;
            paid.amount = t.amount;
            paid.amountInitial = t.amount;
            paid.action = TransactionOnBalanceSheet.DBAction.INSERT;
            paid.expenseType = t.expenseType;
            paid.transactionFlow = CONST_DEBIT_TRANSACTION_FLOW;
            readyToBalanceSheetTransactions.add(paid);
        }
        return readyToBalanceSheetTransactions;
    }

    public List<TransactionOnBalanceSheet> convertCashExpenseToBalanceSheetTransaction(List<ApartmentExpense> apartmentCashExpense) {
        List<TransactionOnBalanceSheet> readyToBalanceSheetTransactions = new ArrayList<>();
        for (ApartmentExpense t : apartmentCashExpense) {
            TransactionOnBalanceSheet paid = new TransactionOnBalanceSheet();
            paid.transactionExpenseId = t.apartmentCashExpenseId; //Cash Expense ID
            paid.amount = t.amount;
            paid.amountInitial = t.amount;
            paid.action = TransactionOnBalanceSheet.DBAction.INSERT;
            paid.expenseType = t.expenseType;
            paid.transactionFlow = CONST_DEBIT_TRANSACTION_FLOW;
            readyToBalanceSheetTransactions.add(paid);
        }
        return readyToBalanceSheetTransactions;
    }

    public List<SocietyHelpTransaction> getUnSplittedDebitTransactions() throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(unSplittedDebitTransactionsQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                Transaction_From_Bank_Statement_ID,Amount,Transaction_Date,Transaction_Flow," +
					"Transaction_Mode,Transaction_Reference,tv.User_Id,ud.Service_Type_Id," +
					"Verified_By,Splitted " +
                 */
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.transactionId = result.getInt(1);
                t.amount = result.getFloat(2);
                t.amountInitial = t.amount;
                t.transactionDate = result.getDate(3);
                t.transactionFlow = result.getString(4);
                t.type = result.getString(5);
                t.reference = result.getString(6);
                t.userId = result.getString(7);
                t.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(8)];
                t.verifiedBy = result.getString(9);
                t.splitted = result.getBoolean(10);
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<TransactionOnBalanceSheet> getAdvancePayment() throws Exception {
        List<TransactionOnBalanceSheet> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(advanceTransactionsQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                tbs.Balance_Sheet_Transaction_ID, tbs.Amount, tbs.Verified_By_Admin,
                tbs.Verified_By_User,tbs.Expense_Type_Id,tbs.Transaction_From_Bank_Statement_ID,
                tbs.User_Cash_Payment_ID, tbs.Transaction_Expense_Id,

                tv.User_Id User_Id_tv, tv.Flat_Id User_Id_tv,"+
				ud.User_Id User_Id_ud, ud.Flat_Id Flat_Id_ud,
				ae.Expend_By_UserId User_Id_ae

               */
                TransactionOnBalanceSheet t = new TransactionOnBalanceSheet();
                t.balanceSheetTransactionID = result.getInt(1);
                t.amount = result.getFloat(2);
                t.amountInitial = t.amount;
                t.isVerifiedByAdmin = result.getBoolean(3);
                t.isVerifiedByUser = result.getBoolean(4);
                t.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(5)];
                t.transactionFromBankStatementID = result.getInt(6);
                t.userCashPaymentID = result.getInt(7);
                t.transactionExpenseId = result.getInt(8);

                if (t.transactionFromBankStatementID > 0) {
                    t.userId = result.getString(9);
                    t.flatId = result.getString(10);
                } else if (t.userCashPaymentID > 0) {
                    t.userId = result.getString(11);
                    t.flatId = result.getString(12);
                } else if (t.transactionExpenseId > 0) {
                    t.userId = result.getString(13);
                    t.flatId = "";
                }

                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<TransactionOnBalanceSheet> initialPaidFlatWise() throws Exception {
        List<TransactionOnBalanceSheet> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(initialPaidFlatWiseAmountQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
				"SELECT tv.Flat_Id, sum(tbs.Amount), tbs.Flat_Wise_Payable_ID "+
               */
                TransactionOnBalanceSheet t = new TransactionOnBalanceSheet();
                t.flatId = result.getString(1);
                t.amount = result.getFloat(2);
                t.amountInitial = t.amount;
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<TransactionOnBalanceSheet> getBalanceSheetData() throws Exception {
        List<TransactionOnBalanceSheet> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(balanceSheetQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                    tbs.Balance_Sheet_Transaction_ID, tbs.Amount, tbs.Verified_By_Admin,
                    tbs.Verified_By_User,tbs.Expense_Type_Id,tbs.Transaction_From_Bank_Statement_ID,
                    7,8
                    tbs.User_Cash_Payment_ID, tbs.Transaction_Expense_Id,
                    9,10
                    tv.User_Id User_Id_tv, tv.Flat_Id User_Id_tv,"+
                    11,12
					"ud.User_Id User_Id_ud, ud.Flat_Id Flat_Id_ud,
					13,
					ae.Expend_By_UserId User_Id_ae, "+
					14,15,16
					"tbs.Transaction_Flow, et.Type, tv.Transaction_Mode,"+
					17,18,19
					"tv.Transaction_Date UserPaidDate, aearn.Earned_Date ApartmentEarnDate, ae.Expend_Date ApartmentExpenseDate,"+
					20,21,22, 23, 24
					"f.Flat_Number, f.Block_Number, f.Area , userD.Name, ud.Paid_Date  

               */
                TransactionOnBalanceSheet t = new TransactionOnBalanceSheet();
                t.balanceSheetTransactionID = result.getInt(1);
                t.amount = result.getFloat(2);
                t.amountInitial = t.amount;
                t.isVerifiedByAdmin = result.getBoolean(3);
                t.isVerifiedByUser = result.getBoolean(4);
                t.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(5)];
                t.transactionFromBankStatementID = result.getInt(6);
                t.userCashPaymentID = result.getInt(7);
                t.transactionExpenseId = result.getInt(8);
                t.userName = result.getString(23);
                t.flatNo = result.getString(20);
                t.block = result.getString(21);
                t.area = result.getInt(22);

                if (t.transactionFromBankStatementID > 0) {
                    t.userId = result.getString(9);
                    t.flatId = result.getString(10);
                } else if (t.userCashPaymentID > 0) {
                    t.userId = result.getString(11);
                    t.flatId = result.getString(12);
                } else if (t.transactionExpenseId > 0) {
                    t.userId = result.getString(13);
                    t.flatId = "";
                }
                t.transactionFlow = result.getString(14);

                if(result.getString(14).equals("Credit"))
                {
                    if(result.getString(15).equals("Club_Store_Earning") || result.getString(15).equals("Interest_Income"))
                    {
                        t.transactionDate = result.getDate(18);
                    }
                    else
                    {
                    	if(t.userCashPaymentID > 0) {
                    		t.transactionDate = result.getDate(24);
                    	}
                    	else 
                    	{
                    		t.transactionDate = result.getDate(17);
                    	}
                    }
                } 
                else
                {
                    t.transactionDate = result.getDate(19);
                }
                
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<FlatWisePayable> getUnPaidAmountFlatWise() throws Exception {
        List<FlatWisePayable> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            //pStat = connection.prepareStatement(unPaidFlatWiseAmountQuery);
            pStat = connection.prepareStatement(payableAndPaidFlatWiseAmountQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                         fwp.Flat_Wise_Payable_ID, fwp.Amount Payable, sum(tbs.Amount) Paid, " +
						"et.Type, et.Expense_Type_Id, et.Payable_Priority, " +
						"fwp.Flat_Id, fwp.Status, " +
						"fwp.Month, fwp.Year, " +
						"fwp.Comments, fwp.Payment_Status_ID "
                 */
                FlatWisePayable t = new FlatWisePayable();
                t.paymentId = result.getInt(1);
                t.amount = result.getFloat(2) - result.getFloat(3);
                t.amountInitial = t.amount;
                t.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(5)];
                t.payablePriority = result.getInt(6);
                t.flatId = result.getString(7);
                t.status = result.getBoolean(8);
                t.month = result.getInt(9);
                t.year = result.getInt(10);
                t.comments = result.getString(11);
                t.paymentStatus = ExpenseType.PaymentStatusConst.values()[result.getInt(12)];
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        Collections.sort(list);
        return list;
    }

    public List<FlatWisePayable> getInitialLoadPayableAmountFlatWise() throws Exception {
        List<FlatWisePayable> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(initialPayableFlatWiseAmountQuery);
            result = pStat.executeQuery();
            while (result.next()) {
            	FlatWisePayable t = new FlatWisePayable();
                t.amount = result.getFloat(1);
                t.amountInitial = t.amount;
                t.expenseType = ExpenseTypeConst.Monthly_Maintenance;
                t.payablePriority = 1;
                t.flatId = result.getString(2);
                t.status = result.getBoolean(3);
                
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        Collections.sort(list);
        return list;
    }

    
    public List<UserPaid> getPaidFlatnExpenseTypeWisePayment() throws Exception {
        List<UserPaid> payments = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			Payment_ID,User_ID,Flat_ID,Amount,Paid_Date,Type,User_Comment,Admin_Comment
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(paidFlatnExpenseTypeWiseAmountQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                UserPaid paid = new UserPaid();
                paid.paymentId = result.getInt(1);
                paid.userId = result.getString(2);
                paid.flatId = result.getString(3);
                paid.amount = result.getFloat(4);
                paid.expendDate = result.getDate(5);
                paid.expenseType = ExpenseType.ExpenseTypeConst.valueOf(result.getString(6));
                paid.userComment = result.getString(7);
                paid.adminComment = result.getString(8);
                payments.add(paid);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return payments;
    }

    public List<FlatWisePayable> getFlatWisePayables() throws Exception {
        List<FlatWisePayable> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(allFlatWiseAmountQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                FlatWisePayable t = new FlatWisePayable();
                /*
                Flat_Wise_Payable_ID,Flat_Id,Status,Month, " +
						5
						"Year,Amount,Type,Comments, " +
						"Status_Type " +

                 */
                t.paymentId = result.getInt(1);
                t.flatId = result.getString(2);
                t.status = result.getBoolean(3);
                t.month = result.getInt(4);
                t.year = result.getInt(5);
                t.amount = result.getFloat(6);
                t.expenseType = ExpenseType.ExpenseTypeConst.valueOf(result.getString(7).trim().replace("\n", "").replace("\r", "").replaceAll(" ", ""));
                t.comments = result.getString(8);
                //t.paymentIds = result.getString(9);
                t.paymentStatus = ExpenseType.PaymentStatusConst.valueOf(result.getString(9).trim().replace("\n", "").replace("\r", "").replaceAll(" ", ""));
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public void loadInitialData(Object data) throws Exception {
        if (data instanceof LoadBhowaInitialData.LoadData) {

            LoadBhowaInitialData.LoadData loadData = (LoadBhowaInitialData.LoadData) data;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {

                long startTime = System.currentTimeMillis();
                cleanDatabase();
                long endTime = System.currentTimeMillis();
                System.out.println("Cleaning (milis) - " + (endTime - startTime));

                startTime = System.currentTimeMillis();
                Map<String, String> flatIdUserIdMapping = generateLoginFlatUser(loadData);
                endTime = System.currentTimeMillis();
                System.out.println("Flat Login User (milis)- " + (endTime - startTime));

                startTime = System.currentTimeMillis();
                loadInitialPayables(loadData.payables);
                endTime = System.currentTimeMillis();
                System.out.println("Payables(milis)- " + (endTime - startTime));

                startTime = System.currentTimeMillis();
                loadInitialPayables(loadData.penalty);
                endTime = System.currentTimeMillis();
                System.out.println("Penality (milis)- " + (endTime - startTime));

                startTime = System.currentTimeMillis();
                loadUserPaid(loadData.userPaid, flatIdUserIdMapping);
                endTime = System.currentTimeMillis();
                System.out.println("User Paid (milis)- " + (endTime - startTime));

                startTime = System.currentTimeMillis();
                loadToApartmentEarning(loadData.storeRent);
                endTime = System.currentTimeMillis();
                System.out.println("Apartement Earning (milis)- " + (endTime - startTime));

                startTime = System.currentTimeMillis();
                loadToApartmentEarning(loadData.interestIncome);
                endTime = System.currentTimeMillis();
                System.out.println("Interest Income (milis)- " + (endTime - startTime));

                startTime = System.currentTimeMillis();
                loadToApartmentExpense(loadData.apartmentExpenses);
                endTime = System.currentTimeMillis();
                System.out.println("Apartment Expense (milis)- " + (endTime - startTime));
                startTime = System.currentTimeMillis();

            } catch (Exception e) {
            	e.printStackTrace();
                //Log.e("error", e.getMessage(), e);
             } finally {
                close(con, pStat, res);
            }
        }
    }

    public void cleanDatabase() throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();

            pStat = con.prepareStatement(cleanLoginDatabase);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(cleanUserDetailsDatabase);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(deleteAllBackupDataQuery);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(cleanTransactionStagingDataQuery);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(cleanFlatDatabase);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(cleanFlatPayablesDatabase);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(cleanTransactionVerficationDatabase);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(cleanBalanceSheetDatabase);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(cleanApartmentEarningDatabase);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(cleanApartmentExpenseDatabase);
            pStat.executeUpdate();
            pStat.close();
            
            pStat = con.prepareStatement(deleteAllBackupDataQuery);
            pStat.executeUpdate();
            pStat.close();
                        
        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public Map<String, String> generateLoginFlatUser(LoadBhowaInitialData.LoadData loadData) throws Exception {

        String curUserId;
        List<String> loginIds = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        List<Flat> flatList = new ArrayList<>();
        List<UserDetails> userDetailList = new ArrayList<>();
        Map<String, String> flatIdUserIdMapping = new HashMap<>();

        Flat curFlat;
        UserDetails curUserDetail;
        String curUserDetailId;
        boolean userDetailIdFound;
        int i = 0;
        for (LoadBhowaInitialData.LoadFlatWisePayble payable : loadData.payables) {
            curUserId = i + "";
            userDetailIdFound = false;
            try {
                if (payable.userName != null) {
                    payable.userName = payable.userName.trim();
                    if (payable.userName.length() <= 4) curUserId = payable.userName;
                    else {
                        String[] tokens = payable.userName.split(" ");
                        for (String userId : tokens) {
                            if (userId.length() > 2) {
                                if (userId.length() <= 4) curUserId = userId;
                                else curUserId = userId.substring(0, 4);
                                break;
                            }
                        }
                    }
                    if (loginIds.contains(curUserId)) {
                        curUserId += ++i;
                        loginIds.add(curUserId);
                        payable.loginId = curUserId;
                    } else {
                        loginIds.add(curUserId);
                        payable.loginId = curUserId;
                    }

                    curFlat = new Flat();
                    curFlat.area = payable.area;
                    curFlat.block = payable.blockNo;
                    curFlat.flatNumber = payable.flatNo;
                    curFlat.flatId = getFlatId(curFlat.flatNumber); //"Flat_" + curFlat.flatNumber;
                    curFlat.maintenanceAmount = payable.maintenanceAmount;
                    flatList.add(curFlat);

                    //payable.loginId = curUserId;
                    payable.flatId = curFlat.flatId;

                    curUserDetailId = payable.userName.replaceAll("[^a-zA-Z0-9]+", "");
                    if (userIds.contains(curUserDetailId)) {
                        curUserDetailId += ++i;
                        userIds.add(curUserDetailId);
                        payable.userId = curUserDetailId;
                    } else {
                        userIds.add(curUserDetailId);
                        payable.userId = curUserDetailId;
                    }

                    for (UserDetails addedUser : userDetailList) {
                        if (addedUser.userId.equals(curUserDetailId)) userDetailIdFound = true;
                    }
                    if (!userDetailIdFound) {
                        curUserDetail = new UserDetails();
                        curUserDetail.userId = curUserDetailId;
                        curUserDetail.flatId = payable.flatId;
                        curUserDetail.isActive = true;
                        curUserDetail.loginId = payable.loginId;
                        curUserDetail.userName = payable.userName;
                        curUserDetail.userType = UserDetails.OWNER;
                        curUserDetail.sAuthorizations.add(SocietyAuthorization.Type.FLAT_DETAIL_VIEW);
                        curUserDetail.sAuthorizations.add(SocietyAuthorization.Type.MY_DUES_VIEWS);
                        curUserDetail.sAuthorizations.add(SocietyAuthorization.Type.PDF_TRANSACTION_VIEW);
                        curUserDetail.sAuthorizations.add(SocietyAuthorization.Type.USER_DETAIL_VIEW);
                        curUserDetail.sAuthorizations.add(SocietyAuthorization.Type.TRANSACTIONS_DETAIL_VIEW);
                        userDetailList.add(curUserDetail);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!flatIdUserIdMapping.containsKey(payable.flatId))
                flatIdUserIdMapping.put(payable.flatId, payable.userId);
        }

        for (LoadBhowaInitialData.LoadTenantUser tenant : loadData.tenantUser) {
            curUserId = i + "";
            userDetailIdFound = false;
            try {
                if (tenant.userName != null) {

                    tenant.userName = tenant.userName.trim();
                    if (tenant.userName.length() <= 4) curUserId = tenant.userName;
                    else {
                        String[] tokens = tenant.userName.split(" ");
                        for (String userId : tokens) {
                            if (userId.length() > 2) {
                                if (userId.length() <= 4) curUserId = userId;
                                else curUserId = userId.substring(0, 4);
                                break;
                            }
                        }
                    }
                    if (loginIds.contains(curUserId)) {
                        curUserId += ++i;
                        loginIds.add(curUserId);
                        tenant.userId = curUserId;
                    } else {
                        loginIds.add(curUserId);
                        tenant.userId = curUserId;
                    }

                    curUserDetailId = tenant.userName.replaceAll("[^a-zA-Z0-9]+", "");
                    if (userIds.contains(curUserDetailId)) {
                        curUserDetailId += ++i;
                        userIds.add(curUserDetailId);
                    } else {
                        userIds.add(curUserDetailId);
                    }

                    for (UserDetails addedUser : userDetailList) {
                        if (addedUser.userId.equals(curUserDetailId)) userDetailIdFound = true;
                    }
                    if (!userDetailIdFound) {
                        curUserDetail = new UserDetails();
                        curUserDetail.userId = curUserDetailId;
                        curUserDetail.flatId = getFlatId(tenant.flatNo);//"Flat_" + tenant.flatNo;
                        curUserDetail.isActive = true;
                        tenant.flatId = curUserDetail.flatId;
                        curUserDetail.loginId = curUserId;
                        curUserDetail.userName = tenant.userName;
                        curUserDetail.userType = UserDetails.TENANT;
                        curUserDetail.sAuthorizations.add(SocietyAuthorization.Type.MY_DUES_VIEWS);
                        curUserDetail.sAuthorizations.add(SocietyAuthorization.Type.PDF_TRANSACTION_VIEW);
                        curUserDetail.sAuthorizations.add(SocietyAuthorization.Type.TRANSACTIONS_DETAIL_VIEW);
                        userDetailList.add(curUserDetail);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        createUserLogin(loginIds, "superadmin");
        addFlatDetails(flatList);
        addUserDetails(userDetailList);

        return flatIdUserIdMapping;
    }

    public List<UserPaid> getUserPaidFromInitialData(List<LoadBhowaInitialData.LoadUserPaid> data) {
        List<UserPaid> list = new ArrayList<>();
        for (LoadBhowaInitialData.LoadUserPaid lt : data) {
            for (Date d : lt.dateAmountMapping.keySet()) {
                UserPaid at = new UserPaid();
                //at.expenseType = lt.expenseType;
                //at.userId = lt.;
                //at.amount = lt.dateAmountMapping.get(d);
                at.expendDate = d;
                list.add(at);
            }
        }
        return list;
    }

    public List<ApartmentExpense> getApartmentExpenseFromInitialData(List<LoadBhowaInitialData.LoadApartmentExpense> data) {
        List<ApartmentExpense> aExpense = new ArrayList<>();
        for (LoadBhowaInitialData.LoadApartmentExpense lt : data) {
            for (Date d : lt.dateAmountMapping.keySet()) {
                ApartmentExpense at = new ApartmentExpense();
                at.expenseType = lt.expenseType;
                at.splitted = false;
                at.amount = lt.dateAmountMapping.get(d);
                at.expendDate = d;
                aExpense.add(at);
            }
        }
        return aExpense;
    }

    public List<ApartmentEarning> getApartmentEarningFromInitialData(List<LoadBhowaInitialData.LoadApartmentEarning> data) {
        List<ApartmentEarning> aEarning = new ArrayList<>();
        for (LoadBhowaInitialData.LoadApartmentEarning lt : data) {
            for (Date d : lt.dateAmountMapping.keySet()) {
                ApartmentEarning at = new ApartmentEarning();
                at.expenseType = lt.expenseType;
                at.splitted = false;
                at.amount = lt.dateAmountMapping.get(d);
                at.expendDate = d;
                aEarning.add(at);
            }
        }
        return aEarning;
    }

    public void loadToApartmentExpense(List<LoadBhowaInitialData.LoadApartmentExpense> aExpense) throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet rs = null;
        try {
            con = getDBInstance();
            pStat = con.prepareStatement(insertApartmentExpenseQuery, Statement.RETURN_GENERATED_KEYS);

            List<TransactionOnBalanceSheet> listBalanceSheetTransaction = new ArrayList<>();
            TransactionOnBalanceSheet curBalanceSheetTransaction;

            for (LoadBhowaInitialData.LoadApartmentExpense t : aExpense) {
                for (Date expenseDate : t.dateAmountMapping.keySet()) {
                    //if (t.dateAmountMapping.get(expenseDate) > 0) {
                        curBalanceSheetTransaction = new TransactionOnBalanceSheet();
                        pStat.setInt(1, t.expenseType.ordinal());
                        pStat.setFloat(2, t.dateAmountMapping.get(expenseDate));
                        pStat.setDate(3, expenseDate);

                        pStat.setString(4, "");
                        pStat.setBoolean(5, true);
                        pStat.setString(6, "superadmin");

                        pStat.setString(7, "");
                        pStat.setString(8, "");
                        pStat.setBoolean(9, true);

                        curBalanceSheetTransaction.transactionFlow = "Debit";
                        curBalanceSheetTransaction.userId = "";
                        curBalanceSheetTransaction.flatId = "";
                        curBalanceSheetTransaction.amount = t.dateAmountMapping.get(expenseDate);
                        curBalanceSheetTransaction.isVerifiedByAdmin = true;
                        curBalanceSheetTransaction.expenseType = t.expenseType;
                        listBalanceSheetTransaction.add(curBalanceSheetTransaction);

                        pStat.addBatch();
                        pStat.clearParameters();
                    //}
                }
            }
            pStat.executeBatch();

            rs = pStat.getGeneratedKeys();
            int i = 0;
            for (; rs.next(); ) {
                listBalanceSheetTransaction.get(i++).transactionExpenseId = rs.getInt(1);
            }
            rs.close();

            insertToPreparedStatementInBatch(listBalanceSheetTransaction);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(con, pStat, rs);
        }
    }


    public void loadToApartmentEarning(List<LoadBhowaInitialData.LoadApartmentEarning> aEarning) throws Exception {

        Connection con = null;
        ResultSet rs = null;
        PreparedStatement pStat = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(insertApartmentEarningQuery, Statement.RETURN_GENERATED_KEYS);

            List<TransactionOnBalanceSheet> listBalanceSheetTransaction = new ArrayList<>();
            TransactionOnBalanceSheet curBalanceSheetTransaction;
//"(Expense_Type_Id,Amount,Earned_Date,Verified,Verified_By,Admin_Comment,Splitted) " +
            for (LoadBhowaInitialData.LoadApartmentEarning t : aEarning) {
                for (Date earnDate : t.dateAmountMapping.keySet()) {
                    //if (t.dateAmountMapping.get(earnDate) > 0) {
                        curBalanceSheetTransaction = new TransactionOnBalanceSheet();
                        pStat.setInt(1, t.expenseType.ordinal());
                        pStat.setFloat(2, t.dateAmountMapping.get(earnDate));
                        pStat.setDate(3, earnDate);
                        pStat.setBoolean(4, true);
                        pStat.setString(5, "superadmin");
                        pStat.setString(6, "");
                        pStat.setBoolean(7, true);

                        curBalanceSheetTransaction.transactionFlow = "Credit";
                        curBalanceSheetTransaction.userId = "";
                        curBalanceSheetTransaction.flatId = "";
                        curBalanceSheetTransaction.amount = t.dateAmountMapping.get(earnDate);
                        curBalanceSheetTransaction.isVerifiedByAdmin = true;
                        curBalanceSheetTransaction.expenseType = t.expenseType;
                        listBalanceSheetTransaction.add(curBalanceSheetTransaction);

                        pStat.addBatch();
                        pStat.clearParameters();
                    //}
                }
            }

            pStat.executeBatch();

            rs = pStat.getGeneratedKeys();
            int i = 0;
            for (; rs.next(); ) {
                listBalanceSheetTransaction.get(i++).apartmentEarningID = rs.getInt(1);
            }
            rs.close();

            insertToPreparedStatementInBatch(listBalanceSheetTransaction);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(con, pStat, rs);
        }
    }

    public void addUserCashPaymentDB(Connection con, List<UserPaid> ud) throws Exception {

        PreparedStatement pStat = con.prepareStatement(addUserPaymentQuery);
        for (UserPaid userPaid : ud) {
            pStat.setString(1, userPaid.userId);
            pStat.setString(2, userPaid.flatId);
            pStat.setFloat(3, userPaid.amount);
            pStat.setDate(4, userPaid.expendDate);
            pStat.setString(5, userPaid.userComment);
            pStat.setInt(6, userPaid.expenseType.ordinal());
            pStat.addBatch();
            pStat.clearParameters();
        }
        pStat.executeUpdate();
    }


    public void loadInitialPayables(List<LoadBhowaInitialData.LoadFlatWisePayble> payables) throws Exception {
        Calendar cal = Calendar.getInstance();
        PreparedStatement pStat = null;
        Connection con = null;
        try {
            con = getDBInstance();
            pStat = con.prepareStatement(addSingleFlatPayablesQuery);

            for (LoadBhowaInitialData.LoadFlatWisePayble fwp : payables) {
                for (Date payableDate : fwp.dateAmountMapping.keySet()) {

                    if (fwp.dateAmountMapping.get(payableDate).amount == null ||
                            fwp.dateAmountMapping.get(payableDate).amount == 0) {
                        //continue;
                        pStat.setFloat(2, 0);
                    } else {
                        pStat.setFloat(2, fwp.dateAmountMapping.get(payableDate).amount);
                    }
                    cal.setTime(payableDate);
                    //pStat.setString(1, "Flat_" + fwp.flatNo);
                    pStat.setString(1, getFlatId(fwp.flatNo));
                    pStat.setInt(3, cal.get(Calendar.MONTH) + 1);
                    pStat.setInt(4, cal.get(Calendar.YEAR));
                    pStat.setFloat(5, fwp.dateAmountMapping.get(payableDate).expenseType.ordinal());
                    pStat.setString(6, "Load data from current XLS file");
                    pStat.setInt(7, ExpenseType.PaymentStatusConst.Initial_Load_Data.ordinal());
                    pStat.addBatch();
                    pStat.clearParameters();
                }
            }
            pStat.executeBatch();
        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, null);
        }
    }
    
    public void deleteAllBackupData() throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(deleteAllBackupDataQuery);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }

    }

    public void insertAutoSplitIdDB(long autoSplitId) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(insertLastAutoSplitQuery);
            pStat.setLong(1, autoSplitId);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public void deleteAutoSplitIdDB() throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {
            con = getDBInstance();
            pStat = con.prepareStatement(deleteLastAutoSplitQuery);
            pStat.executeUpdate();

        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public String createSociety(Object obj) throws Exception {

        String generatedSocietyId = "";

        if(obj instanceof SocietyDetails) {
            SocietyDetails societyDetails = (SocietyDetails) obj;
            Connection con = null;
            PreparedStatement pStat = null;
            ResultSet res = null;

            try {
                con = getDBInstance();
                pStat = con.prepareStatement(createSocietyQuery, Statement.RETURN_GENERATED_KEYS);

                pStat.setString(1, societyDetails.societyName);
                pStat.setString(2, societyDetails.emailId);
                pStat.setString(3, societyDetails.mobileNo);
                pStat.setString(4, societyDetails.city);
                pStat.setString(5, societyDetails.country);

                pStat.executeUpdate();
                ResultSet rs = pStat.getGeneratedKeys();
                rs.next();
                generatedSocietyId = rs.getString(1);

            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            } finally {
                close(con, pStat, res);
            }
        }
        return generatedSocietyId;
    }

    public List<TransactionOnBalanceSheet> userWiseAutoSplitTransactions(String loginId) throws Exception {
        List<TransactionOnBalanceSheet> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(userWiseAutoSplitTransactionsQuery);
            pStat.setString(1, loginId);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
    			"SELECT tv.flat_id, ud.Name, Balance_Sheet_Transaction_ID,tb.Transaction_From_Bank_Statement_ID, " +
					"tv.Amount total_amount, tb.Amount splitted_amount, " +
					"tv.Transaction_Date," +
					"Verified_By_Admin, Verified_By_User, Expense_Type_Id, tb.Transaction_Flow," +
					"tb.Flat_Wise_Payable_ID" +

               */
                TransactionOnBalanceSheet t = new TransactionOnBalanceSheet();
                t.flatId = result.getString(1);
                t.userName = result.getString(2);
                t.balanceSheetTransactionID = result.getInt(3);
                t.transactionFromBankStatementID = result.getInt(4);
                t.amount = result.getFloat(5);
                t.amountInitial = result.getFloat(6);
                t.transactionDate = result.getDate(7);
                t.isVerifiedByAdmin = result.getBoolean(8);
                t.isVerifiedByUser = result.getBoolean(9);
                t.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(10)];
                t.transactionFlow = result.getString(11);
                t.flatWisePayableID = result.getInt(12);
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<TransactionOnBalanceSheet> flatWiseAutoSplitTransactions(String flatId) throws Exception {
        List<TransactionOnBalanceSheet> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(flatWiseAutoSplitTransactionsQuery);
            pStat.setString(1, flatId);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
    			"SELECT tv.flat_id, ud.Name, Balance_Sheet_Transaction_ID,tb.Transaction_From_Bank_Statement_ID, " +
					"tv.Amount total_amount, tb.Amount splitted_amount, " +
					"tv.Transaction_Date," +
					"Verified_By_Admin, Verified_By_User, Expense_Type_Id, tb.Transaction_Flow," +
					"tb.Flat_Wise_Payable_ID" +

               */
                TransactionOnBalanceSheet t = new TransactionOnBalanceSheet();
                t.flatId = result.getString(1);
                t.userName = result.getString(2);
                t.balanceSheetTransactionID = result.getInt(3);
                t.transactionFromBankStatementID = result.getInt(4);
                t.amount = result.getFloat(5);
                t.amountInitial = result.getFloat(6);
                t.transactionDate = result.getDate(7);
                t.isVerifiedByAdmin = result.getBoolean(8);
                t.isVerifiedByUser = result.getBoolean(9);
                t.expenseType = ExpenseType.ExpenseTypeConst.values()[result.getInt(10)];
                t.transactionFlow = result.getString(11);
                t.flatWisePayableID = result.getInt(12);
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<SocietyHelpTransaction> getFlatWiseUnSplittedTransaction(String flatId) throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(flatWiseUnSplittedTransactionQuery);
            pStat.setString(1, flatId);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                    Transaction_From_Bank_Statement_ID,Amount,Transaction_Date," +
					"Transaction_Mode,Transaction_Reference, ud.User_Id, ud.Name ," +
					"tv.Flat_Id,Verified_By " +
				 */
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.transactionId = result.getInt(1);
                t.amount = result.getFloat(2);
                t.transactionDate = result.getDate(3);
                t.type = result.getString(4);
                t.reference = result.getString(5);
                t.userId = result.getString(6);
                t.name  = result.getString(7);
                t.flatId = result.getString(8);
                t.verifiedBy = result.getString(9);

                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<SocietyHelpTransaction> getUserWiseUnSplittedTransaction(String loginId) throws Exception {
        List<SocietyHelpTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {

            connection = getDBInstance();
            pStat = connection.prepareStatement(userWiseUnSplittedTransactionQuery);
            pStat.setString(1, loginId);
            result = pStat.executeQuery();
            while (result.next()) {
                /*
                    Transaction_From_Bank_Statement_ID,Amount,Transaction_Date," +
					"Transaction_Mode,Transaction_Reference, ud.User_Id, ud.Name ," +
					"tv.Flat_Id,Verified_By " +
				 */
                SocietyHelpTransaction t = new SocietyHelpTransaction();
                t.transactionId = result.getInt(1);
                t.amount = result.getFloat(2);
                t.transactionDate = result.getDate(3);
                t.type = result.getString(4);
                t.reference = result.getString(5);
                t.userId = result.getString(6);
                t.name  = result.getString(7);
                t.flatId = result.getString(8);
                t.verifiedBy = result.getString(9);

                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public List<SocietyDetails> getAllSociety() throws Exception {
        List<SocietyDetails> allSociety = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
			/*
			SELECT Society_Id,Society_Name,Database_URL,Database_User,Database_Password,
			Email_Id,Admin_Mobile_No,Created_Date,Service_Start_Date,
			Service_Renewal_Date,Charge_Per_Flat,City,Country,Address,Status FROM societyhelp
			 */
            connection = getDBInstance();
            pStat = connection.prepareStatement(allSocietyQuery);
            result = pStat.executeQuery();
            SocietyDetails society;
            while (result.next()) {
                society = new SocietyDetails();
                society.societyId = result.getInt(0);
                society.societyName = result.getString(1);
                society.databaseURL = result.getString(2);
                society.databaseUser = result.getString(3);
                society.databasePassword = result.getString(4);
                society.emailId = result.getString(5);
                society.mobileNo = result.getString(6);
                society.serviceStartDate = result.getTimestamp(7);
                society.serviceRenewalDate = result.getTimestamp(8);
                society.chargePerFlat = result.getFloat(9);
                society.city = result.getString(10);
                society.country = result.getString(11);
                society.address = result.getString(12);
                society.status = result.getBoolean(13);
                allSociety.add(society);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return allSociety;
    }

  public List<WaterSuppyReading> getAllWaterSupplier() throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        List<WaterSuppyReading> suppliers = new ArrayList<>();

        try {
            con = getDBInstance();
            //"SELECT Supplier_Id,Supplier_Name,Capacity_In_Liter FROM water_suppiler";
            pStat = con.prepareStatement(allWaterSupplierQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                WaterSuppyReading w = new WaterSuppyReading();
                w.supplierId = result.getInt(1);
                w.supplierName = result.getString(2);
                w.capacityInLiter = result.getInt(3);
                suppliers.add(w);
            }
        }catch(Exception e) {
            Log.e("error",e.getMessage());
            throw e;
        }
        finally {
            close(con, pStat, result);
        }
        return suppliers;
    }

    public void insertWaterReading(Object reading) throws Exception {
        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet res = null;

        try {

            if (reading instanceof WaterSuppyReading) {
                WaterSuppyReading waterReading = (WaterSuppyReading) reading;

                try {
                    con = getDBInstance();
                    pStat = con.prepareStatement(insertWaterReadingQuery);
                    pStat.setInt(1, waterReading.supplierId);
                    pStat.setInt(2, waterReading.capacityInLiter);
                    pStat.setInt(3, waterReading.readingBeforeSupply);
                    pStat.setInt(4, waterReading.readingAfterSupply);
                    pStat.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                    pStat.setString(6, waterReading.loginId);
                    pStat.executeUpdate();

                } catch (Exception e) {
                    throw e;
                } finally {
                    close(con, pStat, res);
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            close(con, pStat, res);
        }
    }

    public List<WaterSuppyReading> getAllWaterReading() throws Exception {

        Connection con = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        List<WaterSuppyReading> suppliers = new ArrayList<>();

        try {
            con = getDBInstance();
            //"SELECT ws.Supplier_Id, ws.Supplier_Name, wsr.Capacity_In_Liter, Supply_Time, Reading_Before_Supply, Reading_After_Supply " +
            pStat = con.prepareStatement(allWaterReadingQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                WaterSuppyReading w = new WaterSuppyReading();
                w.supplierId = result.getInt(1);
                w.supplierName = result.getString(2);
                w.capacityInLiter = result.getInt(3);
                w.SupplyTime = result.getTimestamp(4);
                w.readingBeforeSupply = result.getInt(5);
                w.readingAfterSupply = result.getInt(6);
                w.loginId = result.getString(7);
                suppliers.add(w);
            }
        } catch (Exception e) {
            Log.e("error", e.getMessage());
            throw e;
        }
        finally {
            close(con, pStat, result);
        }
        return suppliers;
    }

    public List<StagingTransaction> getUnsettledCreditTransaction() throws Exception {
        List<StagingTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            /*
            SELECT `Transaction_ID`, `StatementID`, `Name`, `Amount`, `Transaction_Date`,
			`Transaction_Flow`, `Transaction_Mode`, `Transaction_Reference`,
			`Upload_Date`, `Uploaded_LoginId` FROM `Transactions_Staging_Data`
			*/
            connection = getDBInstance();
            pStat = connection.prepareStatement(getUnSettledCreditTransactionQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                StagingTransaction t = new StagingTransaction();
                t.transactionId = result.getInt(1);
                t.srNo = result.getInt(2);
                t.name = result.getString(3);
                t.amount = result.getFloat(4);
                t.transactionDate = result.getDate(5);
                t.transactionFlow = result.getString(6);
                t.type = result.getString(7);
                t.reference = result.getString(8);
                t.updloadedDate = result.getTimestamp(9);
                t.uploadedBy = result.getString(10);
                t.verifiedBy = "";
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }


    public List<StagingTransaction> getUnsettledDebitTransaction() throws Exception {
        List<StagingTransaction> list = new ArrayList<>();
        Connection connection = null;
        PreparedStatement pStat = null;
        ResultSet result = null;
        try {
            /*
            SELECT `Transaction_ID`, `StatementID`, `Name`, `Amount`, `Transaction_Date`,
			`Transaction_Flow`, `Transaction_Mode`, `Transaction_Reference`,
			`Upload_Date`, `Uploaded_LoginId` FROM `Transactions_Staging_Data`
			*/
            connection = getDBInstance();
            pStat = connection.prepareStatement(getUnSettledDebitTransactionQuery);
            result = pStat.executeQuery();
            while (result.next()) {
                StagingTransaction t = new StagingTransaction();
                t.transactionId = result.getInt(1);
                t.srNo = result.getInt(2);
                t.name = result.getString(3);
                t.amount = result.getFloat(4);
                t.transactionDate = result.getDate(5);
                t.transactionFlow = result.getString(6);
                t.type = result.getString(7);
                t.reference = result.getString(8);
                t.updloadedDate = result.getTimestamp(9);
                t.uploadedBy = result.getString(10);
                t.verifiedBy = "";
                list.add(t);
            }

        } catch (Exception e) {
            throw e;
        } finally {
            close(connection, pStat, result);
        }
        return list;
    }

    public void settledCreditTransaction(Object transactions) throws  Exception {

        if (transactions instanceof List) {
            List<StagingTransaction> tList = (List<StagingTransaction>) transactions;
            Connection con = null;
            PreparedStatement pStat = null;

            try {
                con = getDBInstance();
                con.setAutoCommit(false);

                pStat = con.prepareStatement(insertSettledCreditTransactionQuery);
                for(StagingTransaction t : tList) {
                    if (t.userId != null) {
                        pStat.setFloat(1, t.amount);
                        if (t.transactionDate != null)
                            pStat.setDate(2, new Date(t.transactionDate.getTime()));
                        else pStat.setDate(2, new Date(System.currentTimeMillis()));
                        pStat.setString(3, t.transactionFlow);
                        pStat.setString(4, t.type);
                        pStat.setString(5, t.reference);
                        pStat.setString(6, t.userId);
                        pStat.setString(7, t.flatId);
                        pStat.setString(8, t.verifiedBy);
                        pStat.setBoolean(9, t.splitted);
                        pStat.setLong(10, autoSplitId);
                        pStat.addBatch();
                        pStat.clearParameters();
                    }
                }
                pStat.executeBatch();

                pStat = con.prepareStatement(deleteUnSettledTransactionQuery);
                for(StagingTransaction t : tList) {
                    if (t.userId != null) {
                        pStat.setInt(1, t.transactionId);
                        pStat.addBatch();
                        pStat.clearParameters();
                    }
                }
                pStat.executeBatch();
                con.commit();

            } catch (Exception e) {
                throw e;
            } finally {
                close(con, pStat, null);
            }
        }
    }
}