package emfQueryEngine;

import java.util.*;
import java.util.regex.Pattern;
import java.io.BufferedReader;  
import java.io.FileInputStream;  
import java.io.IOException;  
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;    
import javax.swing.*;


public class Engine {
	 
	
	public static void main(String[] args){
		
		ArrayList<String> inputFile=new ArrayList<String>();
		
		try
		{
			String str = JOptionPane.showInputDialog("Please input the file name of the input file.");  
			
			readFile(str,inputFile);
		}
		catch(IOException e)
		{
			JOptionPane.showMessageDialog(null,"Input file not found.");
		}
		
        Query q = new Query();
        
        loadQuery(q,inputFile);
        loadType(q);
        Generator emf = new Generator(q);
        emf.generate();
        
	}
	
	public static void readFile(String fileName,ArrayList<String> inputFile) throws IOException
	{
		String realPath=".\\Input\\";
		StringBuilder sb=new StringBuilder();
		sb.append(realPath);
		sb.append(fileName);
		String finalPath= sb.toString();
		
		FileInputStream inputStream = new FileInputStream(finalPath);  
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));  
              
        String temp = null;  
        while((temp = reader.readLine()) != null)  
        {  
            inputFile.add(temp);  
        }  
              
        //close  
        inputStream.close();  
        reader.close();  
	}
	
	public static void loadQuery(Query q,ArrayList<String> inputFile)
	{
		for(String s:inputFile)
		{
			String[] in=s.split(":");
			switch(in[0])
			{
			
			case "tableName":
				q.tableName=in[1].trim();
				break;
			case "partOfUrl":
				q.partOfUrl=in[1].trim();
				break;
			case "user":
				q.user=in[1].trim();
				break;
			case "password":
				q.password=in[1].trim();
				break;
			case "className":
				q.className=in[1].trim();
				break;
			case "where":
				q.where=in[1].trim();
				break;
			case "having":
				q.having=in[1].trim();
				break;
			case "outputPosition":
				q.outputPosition=in[1].trim();
				break;
			case "projections":
				String[] projs=in[1].split(",");
				for(String tempStr:projs)
				{
					q.projections.add(tempStr.trim());
				}
				break;
			case "groupBy":
				String[] groupAttrs=in[1].split(",");
				for(String tempStr:groupAttrs)
				{
					q.groupBy.add(tempStr.trim());
				}
				break;
			case "aggregates":
				String[] aggr=in[1].trim().split(",");
				for(int i=0;i<aggr.length;++i)
				{
					String attr="([a-zA-Z]+_)(\\d+)(_[a-zA-Z]+)?";
					Pattern attrP=Pattern.compile(attr);
					Matcher mP=attrP.matcher(aggr[i]);
					while(mP.find())
					{
						//May contain the aggregates of group variable 0.
						//So need to get the index number and check it.
						String tempAggr=mP.group(); 
						String[] part3=mP.group().trim().split("_");  //Split into 3 parts. Eg. avg_1_quant into avg, 1, and quant.
						int index=Integer.parseInt(part3[1]);
						if(!q.aggregates.containsKey(index)) //When the key integer dooesn't exist.
						{
							ArrayList<String> aggrs=new ArrayList<String>();
							if(part3[0].equals("avg")) //If it is avg.
							{
								StringBuilder sb1=new StringBuilder(); //Add sum.
								sb1.append("sum");
								sb1.append(tempAggr.substring(3)); //The rest of the part. Eg. _1_quant.
								aggrs.add(sb1.toString());
								StringBuilder sb2=new StringBuilder();  //Add cnt.
								sb2.append("cnt");
								sb2.append(tempAggr.substring(3));
								aggrs.add(sb2.toString());
							}
								aggrs.add(tempAggr);
								q.aggregates.put(index,aggrs);
						}
						else //When the key is contained.
						{
							if(part3[0].equals("avg")) //If the aggregate is an average aggregate.
							{
								StringBuilder sb1=new StringBuilder(); //Add sum if sum doesn't exist.
								sb1.append("sum");
								sb1.append(tempAggr.substring(3));
								if(!q.aggregates.get(index).contains(sb1.toString()))
									q.aggregates.get(index).add(sb1.toString());
								
								StringBuilder sb2=new StringBuilder();  //Add cnt if sum doesn't exist.
								sb2.append("cnt");
								sb2.append(tempAggr.substring(3));
								if(!q.aggregates.get(index).contains(sb2.toString()))
									q.aggregates.get(index).add(sb2.toString());
							}
							if(!q.aggregates.get(index).contains(tempAggr))
								q.aggregates.get(index).add(tempAggr);		
						}
					}
				}
				break;
			case "suchThat":
				String[] sT=in[1].trim().split(",");
				for(int i=0;i<sT.length;++i)
				{
					q.suchThat.put(i+1,sT[i]);
				}
				break;
			default:
				break;	
			}
		}
	}
	
	public static void loadType(Query q)
	{
		q.types.put("cust", String.class);
		q.types.put("prod", String.class);
        q.types.put("month", int.class);
        q.types.put("state", String.class);
        q.types.put("quant", int.class);
        
       
        
        q.types.put("cust_0", String.class);
        q.types.put("prod_0", String.class);
        q.types.put("month_0", int.class);
        q.types.put("state_0", String.class);
        q.types.put("quant_0", int.class);
        
        q.types.put("sum_0_quant", int.class);
        q.types.put("cnt_0_quant", int.class);
        q.types.put("avg_0_quant", double.class);
        q.types.put("max_0_quant", int.class);
        q.types.put("min_0_quant", int.class);
        
        int len=q.aggregates.size();
        if(q.aggregates.containsKey(0)) len-=1;
        for(int i=1;i<=len;++i)
        {
        	//Add type to the possible grouping attributes of the group variables.
        	String cust=String.format("cust_%s", i);
        	String prod=String.format("prod_%s", i);
        	String month=String.format("month_%s", i);
        	String state=String.format("state_%s", i);
        	String quant=String.format("quant_%s", i);
        	
        	q.types.put(cust, String.class);
            q.types.put(prod, String.class);
            q.types.put(month, int.class);
            q.types.put(state, String.class);
            q.types.put(quant, int.class);
        	
        	//Add type to the aggregates of the group variables.
        	String sum=String.format("sum_%s_quant", i);
        	String cnt=String.format("cnt_%s_quant", i);
        	String avg=String.format("avg_%s_quant", i);
        	String max=String.format("max_%s_quant", i);
        	String min=String.format("min_%s_quant", i);
        	
        	q.types.put(sum, int.class);
            q.types.put(cnt, int.class);
            q.types.put(avg, double.class);
            q.types.put(max, int.class);
            q.types.put(min, int.class);
        }
        
	}
}
