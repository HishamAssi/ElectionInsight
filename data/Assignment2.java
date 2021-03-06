import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// If you are looking for Java data structures, these are highly useful.
// Remember that an important part of your mark is for doing as much in SQL (not Java) as you can.
// Solutions that use only or mostly Java will not receive a high mark.

public class Assignment2 extends JDBCSubmission {

    public Assignment2() throws ClassNotFoundException {

        Class.forName("org.postgresql.Driver");
    }

    @Override
    public boolean connectDB(String url, String username, String password) {
    	try {
    		connection = DriverManager.getConnection(url, username, password);
    		return true;
    		
    	}
    	catch (SQLException se) {
    		System.err.println("SQL Exception." +
                    "<Message>: " + se.getMessage());
    		return false;
    	}
    }

    @Override
    public boolean disconnectDB() {
    	try {
    		connection.close();
    	}
    	catch (SQLException se) {
    		System.err.println("SQL Exception." +
                    "<Message>: " + se.getMessage());
    		return false;
    	}
        // Implement this method!
        return true;
    }

    @Override
    public ElectionCabinetResult electionSequence(String countryName) {
	try {
	
	String queryCountryId = "SELECT id FROM country WHERE name = ?";
	
	PreparedStatement getCountryId = connection.prepareStatement(queryCountryId);
	getCountryId.setString(1,countryName);

	ResultSet CountryId = getCountryId.executeQuery();
	
	CountryId.next();
	int countryId = CountryId.getInt("id");
	
	/* This query returns 2 columns election id and cabinet id where the cabinet id's
	date falls in between the start date of election id and the end date of election id.
	* We first find the ranges of each election to the next of its same type in order to 
    use the start date.
	* We then find all the cabinets that have a start date greater than   	
	or equal to the start date of this election, and less than the end date. 
	* If an election is the most recent one and has no end date, we only include the
	combination of it with cabinets that started after the start date of this most recents
	election(s).
	*/
	String queryElectionSequence = "SELECT e.election_id, cabinet.id as cabinet_id " +
		"FROM (SELECT e1.e_date as e_start, e2.e_date as e_end, e1.id as election_id, " +
			"e1.country_id as country_id  FROM election e1, election e2 " +
				"WHERE e1.country_id = e2.country_id AND (e1.e_type = e2.e_type AND e2.e_date = " +
					"(SELECT min(e3.e_date) " +
						"FROM election AS e3 " +  
							"WHERE e3.e_date > e1.e_date AND e3.country_id = e1.country_id " + 
								"GROUP BY e1.country_id)) " + 
					"OR (e1.e_date IN" +  
						"(SELECT max(e3.e_date) " +
							"FROM election e3 " + 
								"WHERE e3.e_type = 'Parliamentary election' " +
									"GROUP BY e3.country_id) " + 
						"AND e2.e_date=e1.e_date) " + 
					"OR (e1.e_date IN " + 
						"(SELECT max(e3.e_date) " + 
							"FROM election e3 " +
								"WHERE e3.e_type = 'European Parliament' " + 
									"GROUP BY e3.country_id) " + 
					"AND e2.e_date = e1.e_date)) AS e " + 
		"JOIN cabinet ON cabinet.country_id=e.country_id " + 
			"AND ((e.e_start <= cabinet.start_date AND cabinet.start_date < e.e_end) " + 
			"OR (e.e_start=e.e_end AND e.e_start <= cabinet.start_date)) " + 
			"WHERE e.country_id = ? " + 
			"ORDER BY e.e_start desc, cabinet.start_date asc;";

	PreparedStatement getElectionSequence = connection.prepareStatement(queryElectionSequence);
	getElectionSequence.setInt(1,countryId);

	ResultSet election_sequence = getElectionSequence.executeQuery();
	
	List<Integer> elections = new ArrayList<Integer>();
	List<Integer> cabinets = new ArrayList<Integer>();
	
	// Retrieve the tuples from the election sequence query
	while(election_sequence.next()) {
		elections.add(election_sequence.getInt("election_id") );
		cabinets.add(election_sequence.getInt("cabinet_id"));
	}
	
        return new ElectionCabinetResult(elections, cabinets);
	} catch (SQLException se) {
			System.err.println("SQL Exception." +
                    "<Message>: " + se.getMessage());
    		return null;
	}
    }

    @Override
    public List<Integer> findSimilarPoliticians(Integer politicianId, Float threshold) {
    	
		try {
		    // Retrieve the comment and description of the input politician that we are comparing against.
			String queryMainPolitician = "SELECT id, description, comment FROM politician_president WHERE id = ?";
			PreparedStatement getMainPolitician = connection.prepareStatement(queryMainPolitician);
			getMainPolitician.setInt(1, politicianId);
			ResultSet politicianInfo = getMainPolitician.executeQuery();
			
			// Store the extracted information into structure for use in the following query
			politicianInfo.next();
			String politicianDescription = politicianInfo.getString("description");
			String politicianComment = politicianInfo.getString("comment");
			String politicianAll = politicianDescription + " " + politicianComment;
			
			
			// Get all the politicians, comments, and descriptions of those other than the input politicianId
			String queryOtherPoliticians = "SELECT id, description, comment FROM politician_president WHERE id != ?";
			PreparedStatement getOtherPoliticians = connection.prepareStatement(queryOtherPoliticians);
			getOtherPoliticians.setInt(1, politicianId);
			ResultSet otherPoliticianInfo = getOtherPoliticians.executeQuery();
			
			List<Integer> similarPoliticians = new ArrayList<Integer>();

	 		//Compare the similarity of each politician's comments and descriptions to the input one
			// Only include politicians whose similarity exceeds the given threshold.
			while(otherPoliticianInfo.next()){
				String otherPoliticianDescription = otherPoliticianInfo.getString("description");
				String otherPoliticianComment = otherPoliticianInfo.getString("comment");
				String otherPoliticianAll = otherPoliticianDescription + " " + otherPoliticianComment;
				if (similarity(politicianDescription, otherPoliticianAll) > threshold){
					similarPoliticians.add(otherPoliticianInfo.getInt("id"));
				}
			}
			return similarPoliticians;
		}
		catch (SQLException se) {
			System.err.println("SQL Exception." +
                    "<Message>: " + se.getMessage());
    		return null;
		}
    }

    public static void main(String[] args) {
   
    }

}


