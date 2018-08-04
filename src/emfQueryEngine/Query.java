package emfQueryEngine;

import java.util.*;

//This java file defines the data structure to save the information of the SQL.

public class Query {
	public String className;
	public String tableName;
	
	public String where;  //Where clause.
	
	public String outputPosition;  //This vatiable set the output position of the file.
	
	//Save information of each clause.
	public HashMap<String,Class> types=new HashMap<String,Class>(); //Save the types of each attribute needed.
	public ArrayList<String> projections=new ArrayList<String>();
	public ArrayList<String> groupBy=new ArrayList<String>();
	
	//The following 3 is used for connections
	public String user;
	public String password;
	public String partOfUrl;
	
	
	//For each group variable, there's a list for aggregates about that group variable.
	public HashMap<Integer,ArrayList<String>> aggregates=new HashMap<Integer,ArrayList<String>>();
	
	//Separate the whole such that clause, sentences about the same group variable is saved under the same key.
	public  HashMap<Integer,String> suchThat=new HashMap<Integer,String>(); 
	public String having;
	public  ArrayList<Integer> priority=new ArrayList<Integer>();
	
	
	//Do topological sort below. 	
	public void topologicalSort()
	{
		ArrayList<Integer> result=new TopoSort(suchThat).topoSort();
		priority.addAll(result);
		return;
	}
}
