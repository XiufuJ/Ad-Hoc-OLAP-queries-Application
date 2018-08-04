package emfQueryEngine;

import com.squareup.javapoet.*;
import emfQueryEngine.Query;
import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;


public class Generator {
	private Query query;

	public Generator(Query q)
	{

		q.topologicalSort();
		
		this.query=q;
	}
	
	public void generate() 
	{	
		TypeSpec emfQuery=TypeSpec
				.classBuilder(query.className)
				.addModifiers(Modifier.PUBLIC)
				.addMethod(main())
				.addType(setMFStructure())
				.addMethods(setEqualsHelper())			
				.build();
		
		JavaFile outputFile=JavaFile
				.builder("", emfQuery)
				.build();
		
		File file = new File(query.outputPosition);
		
		
		try{		
		outputFile.writeTo(System.out);
		outputFile.writeTo(file);  //Test.
		}
		catch(IOException e) 
		{
			 System.err.println("BUILD failed.");
	            e.printStackTrace();
		}
	}
	
	
	
	
	//This function generates the codes to scan the table and update the MF structure. The main function.
	public MethodSpec main()
	{
		// Link to the DB first.	
		MethodSpec.Builder main=MethodSpec
				.methodBuilder("main")
				.returns(void.class)

				.addModifiers(Modifier.PUBLIC,Modifier.STATIC)
				.addException(SQLException.class)
				.addException(ClassNotFoundException.class)
				.addParameter(String[].class,"args")
				
				
				.addStatement("String usr= $S",query.user)  											//Set up user to the DB
				.addStatement("String pwd= $S",query.password) 											//Set up password
				.addStatement("String url=\"jdbc:postgresql://localhost:5432/$L\"",query.partOfUrl) 	//Set up URL
				.beginControlFlow("try")
				.addStatement("Class.forName(\"org.postgresql.Driver\")")
				.addStatement("System.out.println(\"Success loading Driver\")")
				.nextControlFlow("catch(Exception e)")
				.addStatement("System.out.println(\"Fail loading Driver\")")
				.endControlFlow()
				.beginControlFlow("try")
				.addStatement("$T conn=$T.getConnection(url, usr, pwd)",Connection.class,DriverManager.class)   //Import DriverManager and Connection. 
				.addStatement("System.out.println(\"Success connecting server!\")")				
				.addStatement("$T stmt = conn.createStatement()",Statement.class);								// Import Statement.
		
		StringBuilder exQ=new StringBuilder();
		exQ.append("select * from ");
		exQ.append(query.tableName);
		if(query.where!=null)
		{
			exQ.append(" where ");
			exQ.append(query.where);
		}
		String executeQ=exQ.toString();
		main.addStatement("$T rs = stmt.executeQuery($S)",ResultSet.class,executeQ); // Import ResultSe.
				
		
		
		//Set up the data structure to save all the data needed.
		main
		.addStatement("$T<String,mfStructure> resultFinal=new HashMap<String,mfStructure>()",HashMap.class) //Import java.util.HashMap. Set the the MF structure to save the data scanned.
		.beginControlFlow("while(rs.next())");
		
		
		//First scan, set up the the the 0th group variable (group variable 0).
		//This part initialize the key set of the HashMap resultFinal. Cannot be done just by looping.
		
		
		main.addStatement("StringBuilder setKey=new StringBuilder()");		
		for(int i=0;i<query.groupBy.size();++i)
		{
			String groupAttr=query.groupBy.get(i);
			main.addStatement("$T $L=rs.$L($S)",query.types.get(groupAttr),groupAttr,getTypeHelper(groupAttr),groupAttr);
			main.addStatement("setKey.append($L)",query.groupBy.get(i));
			if(i!=query.groupBy.size()-1) main.addStatement("setKey.append(\"_\")");  //The form of the key should be like A_B, A_B_C, etc.
		}
		main
		.addStatement("String key=setKey.toString()") //Set up the key.
		.addStatement("mfStructure tempTuple") 				     //Set up the value of the HashMap.
		.beginControlFlow("if(resultFinal.containsKey(key))") 	 //If exist, get the mfStructure out. Ready for update.
		.addStatement("tempTuple=resultFinal.get(key)")
		.nextControlFlow("else") 						 	 //If doesn't exist, initialize.
		.addStatement("tempTuple=new mfStructure()")  //Initialize tempTuple.
		.addStatement("resultFinal.put(key,tempTuple)");
		
		
		for(String tempAttr:query.groupBy)  //Initialize the mfStructure.
		{
			main.addStatement("tempTuple.$L=$L",tempAttr,tempAttr);  //Initialize each grouping attributes in the value of temp.
		}
		
		main.endControlFlow();
		
		
		//Start updating. Update the attributes in the HashMap aggregates.
		
	
		if(query.aggregates.containsKey(0)) //Sometimes aggregates of group variable 0 is not actually needed.
		{
			ArrayList<String> aggrGV0=query.aggregates.get(0);
			for(String tempAggr: aggrGV0)
			{
				main.addStatement(updateAggrHelper(tempAggr));
			}
		}
		main.endControlFlow();
		//Start scans for other group variables from the 1st to the nth.
		//The having clause isn't processed here.
		//Because in suchthat clause every group variable is included. The conditions inside such that clause defines all of the group variables.
		//But the conditions in having clause may only restrict only some of the group variables, not all of them.
		//So that's why I didn't set the structure of query.having as HashMap<Integer,ArrayList<String>>.
		//Because if there are no conditions to restrict one group variable, its inappropriate to put a combination like (key,null) into the HashMap.
		//The having clause will be take into account during the output period.
		
		for (int gvIndex:query.priority) //gvIndex is the index to the current processing group variable.
		{
			//gvIndex doesn't contain 0. Group variable 0 is processed during the very first scan above.
			//The range of gvIndex is from 1 to n.
			main.addStatement("rs=stmt.executeQuery($S)",executeQ) //Start a new round of scan.这里得改。
			.beginControlFlow("while(rs.next())") //For every tuple got from the scan.
			.beginControlFlow("for(mfStructure tempTuple:resultFinal.values())")   //可能要判断是不是要加是否是null的判断。麻烦。
			.beginControlFlow("if($L)",suchThatHelper(gvIndex,query.suchThat.get(gvIndex)));
			
			//System.out.println("当前扫描的group variable 下标为： "+gvIndex);
			for(String aggr:query.aggregates.get(gvIndex))  //测试后发现有问题得改
			{
				//System.out.println("当前扫描的属性为："+aggr);
				main.addStatement("$L",updateAggrHelper(aggr));
			}
			
			main.endControlFlow()
			.endControlFlow()
			.endControlFlow();
			
		}
		
		//Output the resultFinal
		//Set up the header first
		for(String proj:query.projections)
		{
			main.addStatement("System.out.printf(\"%-18s \", $S)",proj);
		}
		main.addStatement("System.out.println()");
		
		main.beginControlFlow("for(mfStructure tempTuple:resultFinal.values())");
		if(query.having!=null) main.beginControlFlow("if($L)",havingHelper(query.having)); //Add restrictions in having clause.
		
		
		//There are projections like(sum_2_quant/sum_1_quant). So the following loop is needed. To make it easier to output.
		for(ArrayList<String> aggrsList:query.aggregates.values())
		{
			
			//Update the attributes which isn't one of the grouping attributes.
			for(String aggrT:aggrsList)
			{
				main.addStatement("$T $L=tempTuple.$L",query.types.get(aggrT),aggrT,aggrT);
				
			}
		}
		
		for(String proj:query.projections)
		{
			 //Set up output and the output format.
			if(query.groupBy.contains(proj))
				 main.addStatement("System.out.printf(\"%-18s \", tempTuple.$L)",proj);
			//Grouping attributes can't be the Null.
			//The remaining attributes are aggregates.
			
			else
			{
				main.beginControlFlow("if(equalsHelper(($L)$L,0.0))","double",proj)
				.addStatement("System.out.printf(\"%-18s \", \"Null\")")
				.nextControlFlow("else")
				.addStatement("System.out.printf(\"%-18s \", ($L)$L)","double",proj)
				.endControlFlow();
			}	
		}
		main.addStatement("System.out.println()"); //Change line.
		
		if(query.having!=null) main.endControlFlow();
		main.endControlFlow();
		
		main
		.nextControlFlow("catch(SQLException e)")
		.addStatement("System.out.println(\"Connection URL or username or password errors!\")")
		.addStatement("e.printStackTrace()")
		.endControlFlow();
		return main.build();	
	}
	
	
	//To generate the mfStructure
	public TypeSpec setMFStructure()
	{
		TypeSpec.Builder mfStruct=TypeSpec.classBuilder("mfStructure")
		.addModifiers(Modifier.PUBLIC,Modifier.STATIC);
		
		for(String groupAttr:query.groupBy)
		{
			mfStruct.addField(FieldSpec.builder(query.types.get(groupAttr), groupAttr).build());
		}
		
		for(ArrayList<String> aggregates2:query.aggregates.values())
		{
			for(String aggre:aggregates2)
			{
				FieldSpec.Builder field=FieldSpec.builder(query.types.get(aggre),aggre);
				if(aggre.contains("min"))
				{
					field.initializer("Integer.MAX_VALUE");
				}
				mfStruct.addField(field.build());
			}
		}
		return mfStruct.build();
	}
	
	
	
	
	//To generate the equalsHelper in the final java file.	
	public ArrayList<MethodSpec> setEqualsHelper()
	{
		ArrayList<MethodSpec> res=new ArrayList<MethodSpec>();
		
		//Need to generate 3 version of equalsHelper here. For int, double, and String.
		MethodSpec intEquals=MethodSpec
				.methodBuilder("equalsHelper")
				.returns(boolean.class)
				.addModifiers(Modifier.PUBLIC,Modifier.STATIC)
				.addParameter(int.class,"a")
				.addParameter(int.class,"b")
				.addStatement("return a==b")
				.build();
		
		MethodSpec doubleEquals=MethodSpec
				.methodBuilder("equalsHelper")
				.returns(boolean.class)
				.addModifiers(Modifier.PUBLIC,Modifier.STATIC)
				.addParameter(double.class,"a")
				.addParameter(double.class,"b")
				.addStatement("return a==b")
				.build();
		
		MethodSpec stringEquals=MethodSpec
				.methodBuilder("equalsHelper")
				.returns(boolean.class)
				.addModifiers(Modifier.PUBLIC,Modifier.STATIC)
				.addParameter(String.class,"a")
				.addParameter(String.class,"b")
				.addStatement("return a.equals(b)")
				.build();
		
		res.add(intEquals);
		res.add(doubleEquals);
		res.add(stringEquals);
		
		return res;
	}	
	
	//----------------------------------------------------------------------------------------------------------------------------
	//Below are the helper functions.
	
	
	/*
	 * When getting the data from the database,
	 * the attributes may have different types,
	 * a function to decide to get what type of data is essential.
	 * It is defined below.
	 */
	public String getTypeHelper(String attribute)
	{
		Class tempType=query.types.get(attribute);
		//Only 3 possible types. They are String, int, and double(for the aggregate function Average).
		StringBuilder sb=new StringBuilder();
		if(tempType.equals(String.class)) sb.append("getString");
		else if(tempType.equals(int.class)) sb.append("getInt");
		else if(tempType.equals(double.class)) sb.append("getDouble"); //Not sure if there are other types.
		return sb.toString();
	}
	
	//The function defined below is used to update the aggregates.
	public String updateAggrHelper(String tempAggr)
	{
		//The aggregates are like "avg_1_quant" etc. So split the string into an Array here.
		String[] aggr=tempAggr.split("_");
		
		/* temp[0] is the aggregate
		 * temp[1] is the index of the group variable currently being processed.
		 * temp[2] is the attribute to be aggregated. Sometime does not exist. For example,cnt_1.
		 */
		StringBuilder result=new StringBuilder();
		
		//During the input, if avg is needed, then sum and cnt is add to query.aggregates before avg.
		//So when looping, first update sum and cnt, then avg.
		//So avg here is made sure calculated by the updated sum and cnt.
		//Use switch case here may cause serious problems. That why there are so many ifs and elses...
		if(aggr[0].equals("sum"))
				result.append( String.format("tempTuple.%s+=rs.%s(\"%s\")",tempAggr,getTypeHelper(tempAggr),aggr[2])); //temp.sum_X+=rs.getY(Z)
		else if (aggr[0].equals("cnt"))
				result.append( String.format("tempTuple.%s+=1",tempAggr)); //temp.cnt_X+=1;
		else if (aggr[0].equals("avg"))
				result.append( String.format("tempTuple.%s=(double) tempTuple.sum_%s_%s/tempTuple.cnt_%s_%s",tempAggr,aggr[1],aggr[2],aggr[1],aggr[2])); //temp.avg_P_Q=temp.sum_P_Q/temp.cnt_P_Q;
		else if (aggr[0].equals("min"))
				result.append( String.format("if(rs.%s(\"%s\")<tempTuple.%s) tempTuple.%s=rs.%s(\"%s\")",getTypeHelper(tempAggr),aggr[2],tempAggr,tempAggr,getTypeHelper(tempAggr),aggr[2])); 
				//Cannot use "Math.min()" here since it may be a comparison of Strings.
				//if(getX(Y)<temp.Z) temp.min_I=getX(Y);
		else if (aggr[0].equals("max"))
				result.append(String.format("if(rs.%s(\"%s\")>tempTuple.%s) tempTuple.%s=rs.%s(\"%s\")",getTypeHelper(tempAggr),aggr[2],tempAggr,tempAggr,getTypeHelper(tempAggr),aggr[2]));
				//if(getX(Y)>temp.Z) temp.max_I=getX(Y);
		else result.append("");
		
		return result.toString();
	}
	
	
	//A such that helper is used to translate the suchthat clause into a string used in if().
	public String suchThatHelper(int index,String suchThat)
	{
		String partSuchThat = suchThat
                .replaceAll("and", "&&")
                .replaceAll("or", "||");
		StringBuilder sb=new StringBuilder();
		ArrayList<String> partsFinal=new ArrayList<String>();
		ArrayList<String> symbles=new ArrayList<String>();
		ArrayList<String> checkNull=new ArrayList<String>();
		
		String logics="(<=|>=|!=|<|>|=|&&|[|]{2})";
		String[] parts=partSuchThat.split("<=|>=|!=|<|>|=|&&|[|]{2}");
		
		
		Pattern symbleP = Pattern.compile(logics);
		Matcher mS = symbleP.matcher(partSuchThat);
		while(mS.find())
		{
			symbles.add(mS.group());
		}
		
		for(String s:parts)
		{
			partHelper(partsFinal,s.trim(),index,checkNull);
		}
		
		//Check Null first.
		for(String check:checkNull)
		{
			if(!check.substring(0,3).equals("min"))
			sb.append(String.format("!equalsHelper(%s,0.0)",check));
			else sb.append(String.format("!equalsHelper(%s,Integer.MAX_VALUE)",check));
			sb.append("&&");
		}
		
		
		//Start combination.
		int i=0;
		for(String str:symbles)
		{
			String t;
			switch(str)
			{
				case "&&":
					sb.append("&&");
					break;
				case "||":
					sb.append("||");
					break;
				case "=":
					t=String.format("equalsHelper(%s,%s)",partsFinal.get(i),partsFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case "!=":
					t=String.format("!equalsHelper(%s,%s)",partsFinal.get(i),partsFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case ">=":
					t=String.format("(%s>%s||equalsHelper(%s,%s))", partsFinal.get(i),partsFinal.get(i+1),partsFinal.get(i),partsFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case "<=":
					t=String.format("(%s<%s||equalsHelper(%s,%s))", partsFinal.get(i),partsFinal.get(i+1),partsFinal.get(i),partsFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case ">":
					t=String.format("%s>%s", partsFinal.get(i),partsFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case "<":
					t=String.format("%s<%s", partsFinal.get(i),partsFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				default:
					break;				
			}
		}
		return sb.toString();
	}
	
	//This helper function is used in the suchThatHelper().
	public void partHelper(ArrayList<String>partsFinal,String s,int index,ArrayList<String> checkNull)
	{
		if(s.indexOf("_")==-1) //When s is just a string or a number, but not an attribute.
		{
			partsFinal.add(s);
			return;
		}
		else
		{			
			String attr="([a-zA-Z]+_)(\\d+)(_[a-zA-Z]+)?";
			Pattern attrP=Pattern.compile(attr);
			Matcher attr1=attrP.matcher(s);
			int last=0;
			StringBuilder sb=new StringBuilder();
			while(attr1.find()) 
			{
				String t=attr1.group();
				int start=s.indexOf(t);
				sb.append(s.substring(last,start));
				last=start+t.length();
				
				
				String[] tempArr=t.split("_");
				
				StringBuilder res=new StringBuilder();
				if(Integer.parseInt(tempArr[1])==index) 
				{				
					//For the data of the the group variable currently being processed and scanned.
					//For example, cust_2 -> rs.getString("cust").
					res.append(String.format("rs.%s(\"%s\")",getTypeHelper(tempArr[0]),tempArr[0])); //注意最后这里要改过去。改成getTypeHelper(tempArr[0]);
				}
				else if(Integer.parseInt(tempArr[1])==0&&tempArr.length==2) //这里的情况是，从GV0中获取值。毕竟再tempTuple中，GV0的那些是不带下标的。
				{
					//For the data of the group variable 0.
					//Situations like cust_0 -> tempTuple.cust.
					res.append(String.format("tempTuple.%s",tempArr[0]));
				}
				else
				{
					//For other situations. For example, avg_1_quant -> tempTuple.avg_1_quant.
					res.append(String.format("tempTuple.%s",t));
				}
				
				sb.append(res.toString());
				
				//The attributes used to compare should not be Null. If there is an aggregate is involved, check it first.
				String head=t.substring(0,3);
				if(head.equals("sum")||head.equals("cnt")||head.equals("avg")||head.equals("max")||head.equals("min"))
					checkNull.add(res.toString());
			}
			if(last<s.length()) sb.append(s.substring(last,s.length()));
			partsFinal.add(sb.toString()); 
			return;
		}
	}
	
	
	public String havingHelper(String allHaving)
	{
		String having = allHaving
                .replaceAll("and", "&&")
                .replaceAll("or", "||");
		
		StringBuilder sb=new StringBuilder();
		ArrayList<String> havingFinal=new ArrayList<String>();
		ArrayList<String> symbles=new ArrayList<String>();
		ArrayList<String> checkNull=new ArrayList<String>();
		
		String logics="(<=|>=|!=|<|>|=|&&|[|]{2})";
		String[] parts=having.split("<=|>=|!=|<|>|=|&&|[|]{2}");
		
		
		Pattern symbleP = Pattern.compile(logics);
		Matcher mS = symbleP.matcher(having);
		while(mS.find())
		{
			symbles.add(mS.group());
		}
		
		for(String s:parts)
		{
			havingAttrHelper(havingFinal,s.trim(),checkNull);
		}
		
		//Check Null first.
		for(String check:checkNull)
		{
			if(!check.substring(0,3).equals("min"))
			sb.append(String.format("!equalsHelper(%s,0.0)",check));
			else sb.append(String.format("!equalsHelper(%s,Integer.MAX_VALUE)",check));
			sb.append("&&");
		}
		
		//Start combination.
		int i=0;
		for(String str:symbles)
		{
			String t;
			switch(str)
			{
				case "&&":
					sb.append("&&");
					break;
				case "||":
					sb.append("||");
					break;
				case "=":
					t=String.format("equalsHelper(%s,%s)",havingFinal.get(i),havingFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case "!=":
					t=String.format("!equalsHelper(%s,%s)",havingFinal.get(i),havingFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case ">=":
					t=String.format("(%s>%s||equalsHelper(%s,%s))", havingFinal.get(i),havingFinal.get(i+1),havingFinal.get(i),havingFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case "<=":
					t=String.format("(%s<%s||equalsHelper(%s,%s))", havingFinal.get(i),havingFinal.get(i+1),havingFinal.get(i),havingFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case ">":
					t=String.format("%s>%s", havingFinal.get(i),havingFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				case "<":
					t=String.format("%s<%s", havingFinal.get(i),havingFinal.get(i+1));
					i+=2;
					sb.append(t);
					break;
				default:
					break;				
			}
		}
		return sb.toString();
	}
	
	public void havingAttrHelper(ArrayList<String> havingFinal,String s,ArrayList<String> checkNull)
	{
		
		if(s.indexOf("_")==-1) //When s is just a string or a number, but not an attribute.
		{
			havingFinal.add(s);
			return;
		}
		else
		{
			String attr="([a-zA-Z]+_)(\\d+)(_[a-zA-Z]+)?";
			Pattern attrP=Pattern.compile(attr);
			Matcher attr1=attrP.matcher(s);
			int last=0;
			
			StringBuilder sb=new StringBuilder();
			while(attr1.find()) 
			{
				String t=attr1.group();
				int start=s.indexOf(t);
				sb.append(s.substring(last,start));
				//end=start+t.length();
				last=start+t.length();
				
				
				
				String[] tempArr=t.split("_");
				
				StringBuilder res=new StringBuilder();
					//For other situations. For example, avg_1_quant -> tempTuple.avg_1_quant.
					res.append(String.format("tempTuple.%s",t));
				
				
				sb.append(res.toString());
				
				//The attributes used to compare should not be Null. If there is an aggregate is involved, check it first.
				String head=t.substring(0,3);
				if(head.equals("sum")||head.equals("cnt")||head.equals("avg")||head.equals("max")||head.equals("min"))
					checkNull.add(res.toString());
				
			}
			if(last<s.length()) sb.append(s.substring(last,s.length()));
			havingFinal.add(sb.toString()); 
			return;
		
		}	
		
	}
	
}
