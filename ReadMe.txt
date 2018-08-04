About the input.

1. All the libraries are included in the document "lib", we changed the class path so that it should be working without adding libraries again.

2. All the input are inside the document "Input".

3. All the generated Java files are inside the document "Generated".

4. Please make sure that everything about one clause are all inside one line.
   One clause can only been found in no more than one line.

5. Please saperate the name of the data and the content of the data with a ":".
   Without this ":", data cannot be identified.
   For example,
	groupBy : cust,prod

6. About the syntax of attributes.

	Attributes have a form like "avg_1_quant" or "month_2"
	Other format cannnot be identified by the program.

7. About group varaible 0

	In the data "suchThat", "aggregates", and "having" every attribute of the group variable 0 should have a form like
	"avg_0_quant" or "month_0", with "_0" inside.

	In the data "projections", "groupBy", and "where" every attribute of the group variable 0 should have a form like
	"cust", "prod", "year", without "_0" inside.

8. About such that clause. Conditions about one group variable are conbined by "and" or "or:,
   the conditions of different group variables are combined by ",".
   For example,
	such that: cust_1=cust_0 and prod_1=prod_0, cust_2=cust_0 and prod_2=prod_0;  

9. About having clause,
	conditions are conbined by "and" or "or".
	For example,
	having: cnt_2_quant=cnt_1_quant/2.

10. About "*",
	the program cannot identify "*". Please don't input "*".

7. About calculations,
	the program can only do calculations include +, -, *, and /, and others can't.
	Please don't input other advanced calculation operations.
	Symbles like "()" can be included,
	For example,
		avg_2_quant>(avg_1_quant+3)*2