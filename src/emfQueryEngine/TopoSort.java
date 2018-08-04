package emfQueryEngine;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopoSort {

	public HashMap<Integer,String> ST;
	public HashMap<Integer,ArrayList<Integer>> dependsOn=new HashMap<Integer,ArrayList<Integer>>(); //The map used to save all the dependencies.
	ArrayList<Integer> result=new ArrayList<Integer>(); //A list used to save the result.
	public TopoSort (HashMap<Integer,String> suchThat)
	{
		 this.ST=suchThat; //Initialize. Get the such that clause.
		 //ArrayList<Integer> temp0=new ArrayList<Integer>();
		 //dependsOn.put(0,temp0);
	}
	
	public ArrayList<Integer> topoSort()
	{
		
		ArrayList<Integer> result=new ArrayList<Integer>(); //A list used to save the result.
		
		
		//First find the dependencies of the of table.
		for(int i:ST.keySet()) //For each such that phrase.
		{
			//Utilize Regular Expression to find the numbers in the such that String.			
			String tempST=ST.get(i);
			
			//The attribute or aggregates in the such that clause has a form like "avg_1_prod", and "prod_1" etc.			
			String numbers="_[0-9]+"; 						//The result should be like "_1", "_23" etc.
			Pattern numsPattern=Pattern.compile(numbers);   //Set up pattern.
			Matcher foundNum= numsPattern.matcher(tempST);
			
			//This ArrayList is used to save the index number of the group variables that are depended on by the current group variable. 
			ArrayList<Integer> temp=new ArrayList<Integer>();
			while(foundNum.find())
			{
				//remove the first "_" of the founded result string.
				String s=foundNum.group().substring(1);
				int tempIndex=Integer.parseInt(s);
				if(tempIndex!=i&&!temp.contains(tempIndex)) temp.add(tempIndex); //Update temp.
			}
			dependsOn.put(i, temp); //update.
		}
		
		//Second, implement Topological sort on the Query structure.
		while(result.size()!=dependsOn.size())
		{
			topoHelper(dependsOn,result);
			
		}
		return result;
	}
	
	public void topoHelper(HashMap<Integer,ArrayList<Integer>> dependsOn,ArrayList<Integer> result)
	{
		for(int task:dependsOn.keySet())
		{
			if(!result.contains(task))
				checkforpretask(dependsOn, task,result);
		}
		
		
	}
	
	public static void checkforpretask(HashMap<Integer, ArrayList<Integer>> dependsOn, int task,ArrayList<Integer> result)
	{
		
		if(dependsOn.get(task).isEmpty())
			;
		else{
			//if(task!=0)
			//{
				for(int pretask:dependsOn.get(task))
				{
					
					if(pretask!=0)checkforpretask(dependsOn, pretask,result);
				}
			//}
		}
		if(!result.contains(task))
			result.add(task);
	}
	
}
