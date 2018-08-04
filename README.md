# CS562-Database-Management-System-II-course-project
The final project for CS562(Database Management System II) at Stevens Institute of Technology.

### This project made use of the Javapoet APIs.

### 1. About the project:


    This project is a JAVA engine that can generate java files according to Extended SQL input 
    with 'such that' clause inside. When running the generated JAVA file, the generated results 
    are the exactly the same as the one generated using standard SQL.
    



### 2. This project is to deal with the problem below:


    Ad-hoc OLAP queries (also known as multi-dimensional queries) expressed in standard SQL, even the simplest types, 
    often lead to  complex relational algebraic expressions with multiple joins, group-bys, and sub-queries.
    When faced with the challenges of processing such queries, traditional query optimizers do not consider
    the “big picture”. Rather, they try to optimize a series of joins and group-bys, leading to poor performance.



### 3. This project provides:

        A syntactic framework to allow succinct expression of ad-hoc OLAP queries by extending the group-by statement
        and adding the new clause, such that, and in turn, provide a simple, efficient and scalable algorithm to
        process the queries.



### 4. About the files:

    i) Input are all in the document 'Input'.

    ii) The generated JAVA files are all in the document 'Generated'.

    iii) QueryTest1.java to QueryTest2.java are for the test of the generated files.
    
    iv) The tested queries are int the file Sample Queries_PDF version.pdf

### 5. About the sales table
    The schema: 
    
    sales (cust,prod,day,month,year,state,quant) 
    
    The table stores the information about the purchases of a product by a customer on a date and state for a sale amount.

### 6. About the input.

    i) All the libraries are included in the document "lib", we changed the class path so that it should be working 
       without adding libraries again.

    ii) All the input are inside the document "Input".

    iii) All the generated Java files are inside the document "Generated".

    iv) Please make sure that everything about one clause are all inside one line.
        One clause can only been found in no more than one line.

    v) Please saperate the name of the data and the content of the data with a ":".
         Without this ":", data cannot be identified.
         For example,
         groupBy : cust,prod

    vi) About the syntax of attributes.

        Attributes have a form like "avg_1_quant" or "month_2"
        Other format cannnot be identified by the program.

    vii) About group varaible 0

        In the data "suchThat", "aggregates", and "having" every attribute of the group variable 0 should have
        a form like "avg_0_quant" or "month_0", with "_0" inside.

        In the data "projections", "groupBy", and "where" every attribute of the group variable 0 should have
        a form like "cust", "prod", "year", without "_0" inside.

    viii) About such that clause. Conditions about one group variable are conbined by "and" or "or:,
          the conditions of different group variables are combined by ",".
          For example,
          such that: cust_1=cust_0 and prod_1=prod_0, cust_2=cust_0 and prod_2=prod_0;  

    ix) About having clause,
        conditions are conbined by "and" or "or".
        For example,
        having: cnt_2_quant=cnt_1_quant/2.

    x) About "*",
        the program cannot identify "*". Please don't input "*".

    xi) About calculations,
        the program can only do calculations include +, -, *, and /, and others can't.
        Please don't input other advanced calculation operations.
        Symbles like "()" can be included,
        For example,
            avg_2_quant>(avg_1_quant+3)*2
