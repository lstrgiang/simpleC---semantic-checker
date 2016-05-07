# simpleC---semantic-checker
A simple compiler to check the simpleC grammar
#Usage
> You can compile and run the start project by following steps:
> Note: make sure that you have java and javac commands configured that can run in your command line environment.
> - ```java -cp jlex.jar JLex.Main c.jlex```  //make c.jlex.java file
> - ```java -cp javacup.jar java_cup.Main c.cup```  //make sym.java, parser.java files
> - ```javac -classpath jlex.jar;javacup.jar *.java```  //compile all java file
> - ```java -classpath .;jlex.jar;javacup.jar Checker test.c``` //check test.c file
