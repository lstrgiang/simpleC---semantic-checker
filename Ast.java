import java.io.*;
import java.util.*;

// **********************************************************************
// Ast class (base class for all other kinds of nodes)
// **********************************************************************
abstract class Ast {
}

class Program extends Ast {

    public Program(DeclList declList) {
        this.declList = declList;
    }
    // Semantic checking
    public void check(){
      SymbolTable.Initilize();
      this.declList.check();
    }

    private DeclList declList;
}

// **********************************************************************
// Decls
// **********************************************************************
class DeclList extends Ast {

    public DeclList(LinkedList decls) {
        this.decls = decls;
    }
    public List getDecls(){return this.decls;}
    public void check(){
      Iterator<?> it = decls.iterator();
      while(it.hasNext())
        ((Decl) it.next()).check();
    }
    protected LinkedList decls;
}
  abstract class Decl extends Ast {
  abstract public void check();
  abstract public Id name();
  abstract public Type type();
}

class VarDecl extends Decl {
    public VarDecl(Type type, Id name) {
        this.type = type;
        this.name = name;
    }
    public void check(){
      List<SymbolEntry> list = SymbolTable.lookupLocal(name.val());
      if(list != null)
        Errors.semanticError(name.getLine(),name.getLine(),
          "Multiple declarations within a single scope");
      else{
        SymbolTable.addDecl(this.name.val(),
            new SymbolEntry(this.type,(SymbolTable.size() == 1) ? true : false));
      }
    }
    public Id name(){return this.name;}
    public Type type(){return this.type;};
    private Type type;
    private Id name;
}

class FnDecl extends Decl {
    //FnDecl Constructor
    public FnDecl(Type type, Id name, FormalsList formalList, FnBody body) {
        this.type = type;
        this.name = name;
        this.formalList = formalList;
        this.body = body;
    }
    //Semantic Checking
    public void check(){
      this.formalList.check();
      List<SymbolEntry> symList = SymbolTable.lookupLocal(name.val());
      boolean badDecl = false;
      if(symList != null){
          for(int i=0;i < symList.size();i++){
          if(symList.get(i) instanceof FuncEntry){
            FuncEntry fn = (FuncEntry)symList.get(i);
            if(this.isSameFnAs(fn.getFormalListOfFunc()) && !fn.isPreFn()){
              Errors.semanticError(name.getLine(),name.getChar(),
                "Multiple Declarations of A Single Function within A Single Scope");
              badDecl = true;
              break;
            }
          }
        }
      }
      if(!badDecl){
        SymbolTable.addDecl(this.name.val(),new FuncEntry(this.type,
          formalList.size(),this.formalList.getFormal(),this.body.getDeclList(),false));
      }
      SymbolTable.addScope();
      if(this.formalList != null)
        formalList.addToScope();
      boolean isReturn = this.body.check(this.type);
      SymbolTable.removeScope();
      if(!isReturn && !this.type.isVoidType())
        Errors.semanticError(this.name.getLine(),this.name.getChar(),
          "Function need return");
    }
    public boolean isSameFnAs(List formals){
      if(this.formalList.size() != formals.size()){
        return false;
      }
      if(!this.formalList.isSameListAs(formals)){
        return false;
      }

      return true;
    }
    public Id name(){return this.name;}
    public Type type(){return this.type;};

    public FormalsList getFormalList(){return this.formalList;}
    private Type type;
    private Id name;
    private FormalsList formalList;
    private FnBody body;
}

class FnPreDecl extends Decl {
    public FnPreDecl(Type type, Id name, FormalsList formalList) {
        this.type = type;
        this.name = name;
        this.formalList = formalList;
    }
    public void check(){
      this.formalList.check();
      List<SymbolEntry> symList = SymbolTable.lookupLocal(name.val());
      SymbolTable.addDecl(this.name.val(),new FuncEntry(this.type,
                        this.formalList.size(),this.formalList.getFormal(),null));
    }
    public boolean isSameFnAs(List formals){
      if(this.formalList.size() != formals.size())
        return false;
      if(!this.formalList.isSameListAs(formals))
        return false;
      return true;
    }
    public Id name(){return this.name;}
    public Type type(){return this.type;};
    public FormalsList getFormalList(){return this.formalList;}
    private Type type;
    private Id name;
    private FormalsList formalList;
}

class FormalsList extends Ast {
    public FormalsList(LinkedList formals) {
        this.formals = formals;
    }
    public void addToScope(){
      for(int i=0;i<this.formals.size();i++){
        SymbolTable.addDecl(((FormalDecl)this.formals.get(i)).name().val(),
          new SymbolEntry(((FormalDecl)this.formals.get(i)).type(),false));
      }
    }
    public boolean isSameListAs(FormalsList list){
      return this.isSameListAs(list.getFormal());
    }
    public boolean isSameListAs(List formals){
      if(this.formals.size() != formals.size())
        return false;
      Iterator<?> it1 = this.formals.iterator();
      Iterator<?> it2 = formals.iterator();
      while(it1.hasNext() && it2.hasNext())
        if(!((FormalDecl) it1.next()).isSameFormalTypeAs((FormalDecl)it2.next())){

          return false;
        }

      return true;
    }
    public LinkedList getFormal(){return this.formals;}
    //check() function check all of the FormalDecl in the List formals
    public void check(){
      Iterator<?> it = formals.iterator();
      while(it.hasNext()){
        FormalDecl tmp = (FormalDecl) it.next();
        if(tmp.instanceNumIn(formals) > 1)
          Errors.semanticError(tmp.name().getLine(),tmp.name().getChar(),
            "Multiple Declarations of A Symbol within a Single Formal Declaration");
      }
    }
    public int size(){return formals.size();}
    // linked list of kids (FormalDecls)
    private LinkedList formals;
}

class FormalDecl extends Decl {
    //Formal Constructor
    public FormalDecl(Type type, Id name) {
        this.type = type;
        this.name = name;
    }
    //isSameFormalTypeAs return true when this.formal has the same type as
    //the given formal's type
    public boolean isSameFormalTypeAs(FormalDecl formal){
      if(!this.type.name().equals(formal.type().name())){
        return false;
      }
      return true;
    }
    public void check(){

    }
    //isSameFormalNameAs return true when this.formal has the same name as
    //the given formal's name
    public boolean isSameFormalNameAs(FormalDecl formal){
      if(formal.name().val() == null || this.name.val() == null) return false;
      if(!this.name.val().equals(formal.name().val()))
        return false;
      return true;
    }
    public boolean isSameTypeAs(Type type){
      if(!this.type.name().equals(type.name()))
        return false;
      return true;
    }
    public boolean isSameTypeAs(String typeName){
      if(!this.type.name().equals(typeName))
        return false;
      return true;
    }
    //instanceNumIn return the number of instance of the current formal
    //that is in the List formals
    public int instanceNumIn(LinkedList formals){
      Iterator<?> it = formals.iterator();
      int instance = 0;
      while(it.hasNext()){
        FormalDecl tmp = (FormalDecl) it.next();
        if(this.isSameFormalNameAs(tmp) && this.isSameFormalTypeAs(tmp))
          instance++;
      }
      return instance;
    }
    //type() return type of the current formal
    public Type type(){return this.type;}
    //name() return Id name of the current formal
    public Id name(){return this.name;}
    //Private Values
    private Type type;
    private Id name;
}

class FnBody extends Ast {

    public FnBody(DeclList declList, StmtList stmtList) {
        this.declList = declList;
        this.stmtList = stmtList;
    }
    public boolean check(Type returnType){
      this.declList.check();
      return this.stmtList.check(returnType);
    }
    public List getDeclList(){return this.declList.getDecls();}
    private DeclList declList;
    private StmtList stmtList;
}

class StmtList extends Ast {

    public StmtList(LinkedList stmts) {
        this.stmts = stmts;
    }
    public boolean check(Type returnType){
      Iterator<?> it = this.stmts.iterator();
      boolean isReturn = false;
      while(it.hasNext())
        isReturn = ((Stmt) it.next()).check(returnType) || isReturn;
      return isReturn;
    }
    // linked list of kids (Stmts)
    private LinkedList stmts;
}

// **********************************************************************
// Types
// **********************************************************************
class Type extends Ast {

    private Type() {}

    public static Type CreateSimpleType(String name)
    {
        Type t = new Type();
        t.name = name;
        t.size = -1;
        t.numPointers = 0;

        return t;
    }
    public static Type CreateArrayType(String name, int size)
    {
        Type t = new Type();
        t.name = name;
        t.size = size;
        t.numPointers = 0;

        return t;
    }
    public static Type CreatePointerType(String name, int numPointers)
    {
        Type t = new Type();
        t.name = name;
        t.size = -1;
        t.numPointers = numPointers;

        return t;
    }
    public static Type CreateArrayPointerType(String name, int size, int numPointers)
    {
        Type t = new Type();
        t.name = name;
        t.size = size;
        t.numPointers = numPointers;

        return t;
    }

    public String name()
    {
        return name;
    }
    public boolean isVoidType(){return this.name.equals(voidTypeName);}
    public boolean isIntType(){return this.name.equals(intTypeName);}
    public boolean isStringType(){return this.name.equals(stringTypeName);}
    public boolean isErrorType(){return this.name.equals(errorTypeName);}
    public boolean isErrorUndefinedType(){return this.name.equals(errorUndefinedTypeName);}

    public boolean isErrorFnUndefined(){return this.name.equals(errorFnUndefined);}
    public boolean isSameTypeAs(Type _type){return this.name.equals(_type.name());}
    private String name;
    private int size;  // use if this is an array type
    private int numPointers;

    public static final String voidTypeName = "void";
    public static final String intTypeName = "int";
    public static final String stringTypeName = "String";
    public static final String errorTypeName = "error";
    public static final String errorUndefinedTypeName = "errorUndefined";
    public static final String errorFnUndefined="errorFnUndefined";
}

// **********************************************************************
// Stmts
// **********************************************************************
abstract class Stmt extends Ast {
  abstract public boolean check(Type returnType);
}
// class IOStmt extends Stmt{
//   public ReadStmt(int intVal, String stringval,Exp exp,String stmt){
//     this.exp = exp;
//     this.intVal = intVal;
//     this.stringVal = stringVal;
//     this.name = stmt;
//   }
//   public boolean check(){Type returnType}{
//     if(exp!= null){
//       Type expType = exp.typeCheck();
//       if(expType == true) return true;
//       if(typeExp.isErrorUndefinedType())
//         Errors.semanticError(exp.getLine(),exp.getChar(),
//           "Undefined symbol");
//       else if(typeExp.isVoidType())
//             Errors.semanticError(exp.getLine(),exp.getChar(),
//             "Use of non-numeric variable");
//       else if(typeExp.isErrorFnUndefined())
//             Errors.semanticError(exp.getLine(),exp.getChar(),
//                 "Undefined function");
//       else Errors.semanticError(exp.getLine(),exp.getChar(),
//           "Type Error");
//     }
//     return false;
//   }
//   public boolean isReadStmt(){return this.name.equals(readStmt);}
//   public boolean isWriteStmt(){return this.name.equals(writeStmt);}
//   public static String readStmt = "cin";
//   public static String writeStmt = "cout";
//   private String ioName;
//   private Exp exp;
//   private int intVal;
//   private String stringVal;
//
// }
class AssignStmt extends Stmt {

    public AssignStmt(Exp lhs, Exp exp) {
        this.lhs = lhs;
        this.exp = exp;
    }
    public boolean check(Type returnType){
      if(lhs instanceof RelationalExp)
        Errors.semanticError(lhs.getLine(),lhs.getChar(),
          "Assignment of relational expression");
      if(exp instanceof RelationalExp)
        Errors.semanticError(exp.getLine(),exp.getChar(),
          "Assignment from relational expression");
      Type typeLeft = lhs.typeCheck();
      Type typeExp = exp.typeCheck();
      if(typeLeft.isSameTypeAs(typeExp) && typeLeft.isIntType())
        return false;
      if(lhs instanceof CallExp)
        Errors.semanticError(lhs.getLine(),lhs.getChar(),
          "Invalid function call assignment");
      if(typeLeft.isErrorUndefinedType())
        Errors.semanticError(lhs.getLine(),lhs.getChar(),
          "Undefined symbol");
      else if(typeLeft.isVoidType())
        Errors.semanticError(lhs.getLine(),lhs.getChar(),
        "Use of non-numeric variable");
      else
        Errors.semanticError(lhs.getLine(),lhs.getChar(),
          "Type Error");
      if(typeExp.isErrorUndefinedType())
        Errors.semanticError(exp.getLine(),exp.getChar(),
          "Undefined symbol");
      else if(typeExp.isVoidType())
            Errors.semanticError(exp.getLine(),exp.getChar(),
            "Use of non-numeric variable");
      else if(typeExp.isErrorFnUndefined())
            Errors.semanticError(exp.getLine(),exp.getChar(),
                "Undefined function");
      else Errors.semanticError(exp.getLine(),exp.getChar(),
          "Type Error");
      return false;
    }

    private Exp lhs;
    private Exp exp;
}

class IfStmt extends Stmt {

    public IfStmt(Exp exp, DeclList declList, StmtList stmtList) {
        this.exp = exp;
        this.declList = declList;
        this.stmtList = stmtList;
    }
    public boolean check(Type returnType){
      SymbolTable.addScope();
      this.declList.check();
      boolean isReturn = this.stmtList.check(returnType);
      SymbolTable.removeScope();
      Type typeExp = exp.typeCheck();
      if(typeExp.isIntType()) return isReturn;
      if(typeExp.isErrorUndefinedType())
        Errors.semanticError(exp.getLine(),exp.getChar(),
          "Undefined symbol");
      else if(typeExp.isVoidType())
            Errors.semanticError(exp.getLine(),exp.getChar(),
            "Use of non-numeric variable");
      else if(typeExp.isErrorFnUndefined())
            Errors.semanticError(exp.getLine(),exp.getChar(),
                "Undefined function");
      else Errors.semanticError(exp.getLine(),exp.getChar(),
          "Type Error");
      return isReturn;
    }

    private Exp exp;
    private DeclList declList;
    private StmtList stmtList;
}

class IfElseStmt extends Stmt {

    public IfElseStmt(Exp exp, DeclList declList1, StmtList stmtList1,
            DeclList declList2, StmtList stmtList2) {
        this.exp = exp;
        this.declList1 = declList1;
        this.stmtList1 = stmtList1;
        this.declList2 = declList2;
        this.stmtList2 = stmtList2;
    }
    public boolean check(Type returnType){
      SymbolTable.addScope();
      this.declList1.check();
      boolean isReturn1 = this.stmtList1.check(returnType);
      SymbolTable.removeScope();
      SymbolTable.addScope();
      this.declList2.check();
      boolean isReturn2 = this.stmtList2.check(returnType);
      SymbolTable.removeScope();
      Type typeExp = exp.typeCheck();
      if(typeExp.isIntType()) return isReturn1 || isReturn2;
      if(typeExp.isErrorUndefinedType())
        Errors.semanticError(exp.getLine(),exp.getChar(),
          "Undefined symbol");
      else if(typeExp.isVoidType())
            Errors.semanticError(exp.getLine(),exp.getChar(),
            "Use of non-numeric variable");
      else if(typeExp.isErrorFnUndefined())
            Errors.semanticError(exp.getLine(),exp.getChar(),
                "Undefined function");
      else Errors.semanticError(exp.getLine(),exp.getChar(),
          "Type Error");
      return isReturn1 || isReturn2;
    }
    private Exp exp;
    private DeclList declList1;
    private DeclList declList2;
    private StmtList stmtList1;
    private StmtList stmtList2;
}

class WhileStmt extends Stmt {
    public WhileStmt(Exp exp, DeclList declList, StmtList stmtList) {
        this.exp = exp;
        this.declList1 = declList;
        this.stmtList = stmtList;
    }
    public boolean check(Type returnType){
      SymbolTable.addScope();
      this.declList1.check();
      boolean isReturn = this.stmtList.check(returnType);
      SymbolTable.removeScope();
      Type typeExp = exp.typeCheck();
      if(typeExp.isIntType()) return isReturn;
      if(typeExp.isErrorUndefinedType())
        Errors.semanticError(exp.getLine(),exp.getChar(),
          "Undefined symbol");
      else if(typeExp.isVoidType())
            Errors.semanticError(exp.getLine(),exp.getChar(),
            "Use of non-numeric variable");
      else if(typeExp.isErrorFnUndefined())
            Errors.semanticError(exp.getLine(),exp.getChar(),
                "Undefined function");
      else Errors.semanticError(exp.getLine(),exp.getChar(),
          "Type Error");
      return isReturn;
    }
    private Exp exp;
    private DeclList declList1;
    private StmtList stmtList;
}

class ForStmt extends Stmt {

    public ForStmt(Stmt init, Exp cond, Stmt incr,
            DeclList declList1, StmtList stmtList) {
        this.init = init;
        this.cond = cond;
        this.incr = incr;
        this.declList1 = declList1;
        this.stmtList = stmtList;
    }
    public boolean check(Type returnType){
      SymbolTable.addScope();
      this.declList1.check();
      boolean isReturn3 = this.stmtList.check(returnType);
      SymbolTable.removeScope();
      boolean isReturn1 = this.init.check(returnType);
      boolean isReturn2 = this.incr.check(returnType);
      Type typeCond = cond.typeCheck();
      if(typeCond.isIntType()) return isReturn3 || isReturn1 || isReturn2;
      if(typeCond.isErrorUndefinedType())
        Errors.semanticError(cond.getLine(),cond.getChar(),
          "Undefined symbol");
      else if(typeCond.isVoidType())
            Errors.semanticError(cond.getLine(),cond.getChar(),
            "Use of non-numeric variable");
      else if(typeCond.isErrorFnUndefined())
            Errors.semanticError(cond.getLine(),cond.getChar(),
                "Undefined function");
      else Errors.semanticError(cond.getLine(),cond.getChar(),
          "Type Error");
      return isReturn1 || isReturn2 || isReturn3;
    }
    private Stmt init;
    private Exp cond;
    private Stmt incr;
    private DeclList declList1;
    private StmtList stmtList;
}

class CallStmt extends Stmt {
    public boolean check(Type returnType){
      Type callType = callExp.typeCheck();
      if(callType.isIntType()) return false;
      if(callType.isErrorUndefinedType())
        Errors.semanticError(callExp.getLine(),callExp.getChar(),
          "Undefined symbol");
      else if(callType.isVoidType())
        Errors.semanticError(callExp.getLine(),callExp.getChar(),
          "Use of non-numeric variable");
      else if(callType.isErrorFnUndefined())
        Errors.semanticError(callExp.getLine(),callExp.getChar(),
          "Undefined function");
      else Errors.semanticError(callExp.getLine(),callExp.getChar(),
          "Type Error");
      return false;
    }
    public CallStmt(CallExp callExp) {
        this.callExp = callExp;
    }

    private CallExp callExp;
}

class ReturnStmt extends Stmt {
    public ReturnStmt(Exp exp){
      this.exp = exp;
    }
    public boolean check(Type returnType){
      if(exp != null){
        if(returnType.isVoidType())
          Errors.semanticError(exp.getLine(),exp.getChar(),
              "Do not need return value");
        Type typeExp = exp.typeCheck();
        if(typeExp.isIntType()) return true;
        if(typeExp.isErrorUndefinedType())
          Errors.semanticError(exp.getLine(),exp.getChar(),
            "Undefined symbol");
        else if(typeExp.isErrorFnUndefined())
              Errors.semanticError(exp.getLine(),exp.getChar(),
                  "Undefined function");

        else Errors.semanticError(exp.getLine(),exp.getChar(),
            "Type Error");
      }else{
        if(!returnType.isVoidType())
          Errors.semanticError(-1,-1,
              "Return value needed");
      }
      return true;
    }

    private Exp exp; // null for empty return
}

// **********************************************************************
// Exps
// **********************************************************************
abstract class Exp extends Ast {
    public abstract int getLine();
    public abstract int getChar();
    public abstract Type typeCheck();
}

abstract class BasicExp extends Exp{
    private int lineNum;
    private int charNum;

    public BasicExp(int lineNum, int charNum){
        this.lineNum = lineNum;
        this.charNum = charNum;
    }

    public int getLine(){
        return lineNum;
    }
    public int getChar(){
        return charNum;
    }
}

class IntLit extends BasicExp {

    public IntLit(int lineNum, int charNum, int intVal) {
        super(lineNum, charNum);
        this.intVal = intVal;
    }
    public Type typeCheck(){return Type.CreateSimpleType(Type.intTypeName);}
    private int intVal;
}

class StringLit extends BasicExp {

    public StringLit(int lineNum, int charNum, String strVal) {
        super(lineNum, charNum);
        this.strVal = strVal;
    }

    public String str() {
        return strVal;
    }
    public Type typeCheck(){return Type.CreateSimpleType(Type.stringTypeName);}
    private String strVal;
}

class Id extends BasicExp {

    public Id(int lineNum, int charNum, String strVal) {
        super(lineNum, charNum);
        this.strVal = strVal;
    }
    public String val(){return this.strVal;}
    public Type typeCheck(){
      List<SymbolEntry> symTab = SymbolTable.lookupGlobal(strVal);
      if(SymbolTable.lookupGlobal(strVal) == null)
        return Type.CreateSimpleType(Type.errorUndefinedTypeName);
      symTab.get(0).setIsUSed(true);
      return symTab.get(0).getType();
    }
    private String strVal;
}

class ArrayExp extends Exp {

    public ArrayExp(Exp lhs, Exp exp) {
        this.lhs = lhs;
        this.exp = exp;
    }

    public int getLine() {
        return lhs.getLine();
    }
    public Type typeCheck(){
      Type typeLeft = lhs.typeCheck();
      Type typeExp = exp.typeCheck();
      if(typeLeft.isSameTypeAs(typeExp))
        return typeLeft;
      else{
        if(typeLeft.isIntType() && !typeExp.isIntType())
          return typeExp;
        if(!typeLeft.isIntType() && typeExp.isIntType())
          return typeLeft;
      }
      return null;
    }
    public int getChar() {
        return lhs.getChar();
    }

    private Exp lhs;
    private Exp exp;
}

class CallExp extends Exp {

    public CallExp(Id name, ActualList actualList) {
        this.name = name;
        this.actualList = actualList;
    }

    public CallExp(Id name) {
        this.name = name;
        this.actualList = new ActualList(new LinkedList());
    }
    public Type typeCheck(){
      List<?> expsList = actualList.actualExps();
      Iterator<?> itList = expsList.iterator();
      List<Type> expsType = new LinkedList<Type>();
      while(itList.hasNext()){
        Exp exp = (Exp) itList.next();
        Type type = exp.typeCheck();
        expsType.add(type);
        if(type.isIntType())  continue;
        if(type.isVoidType())
          Errors.semanticError(exp.getLine(),exp.getChar(),
              "Use of non-numeric expression");
        else if(type.isErrorUndefinedType())
          Errors.semanticError(exp.getLine(),exp.getChar(),
              "Undefined symbol");
        else
          Errors.semanticError(exp.getLine(),exp.getChar(),
              "Type Error");
      }
      List<SymbolEntry> symTab = SymbolTable.lookupGlobal(this.name.val());
      if(symTab == null || symTab.size ()<= 0)
        return Type.CreateSimpleType(Type.errorFnUndefined);
      else{
        for(int i=0;i<symTab.size();i++){
          if(symTab.get(i) instanceof FuncEntry){
            FuncEntry fn = (FuncEntry)symTab.get(i);
            if(fn.numParams()==this.actualList.expsNum()){
              List<FormalDecl> formals = fn.getFormalListOfFunc();
              boolean isSameFunc = true;
              for(int j=0;j<this.actualList.expsNum();j++){
                if(!formals.get(j).isSameTypeAs(expsType.get(j))){
                    isSameFunc = false;
                    break;
                }
              }
              if(isSameFunc) return fn.getReturnType();
            }
          }
        }
        return Type.CreateSimpleType(Type.errorFnUndefined);
      }
    }
    public int getLine() {
        return name.getLine();
    }

    public int getChar() {
        return name.getChar();
    }

    private Id name;
    private ActualList actualList;
}
class ActualList extends Ast {

    public ActualList(LinkedList exps) {
        this.exps = exps;
    }
    public List actualExps(){return this.exps;}
    public int expsNum(){return this.exps.size();}
    // linked list of kids (Exps)
    private LinkedList exps;
}

abstract class UnaryExp extends Exp {

    public UnaryExp(Exp exp) {
        this.exp = exp;
    }
    public Type typeCheck(){return exp.typeCheck(); }
    public int getLine() {
        return exp.getLine();
    }

    public int getChar() {
        return exp.getChar();
    }

    protected Exp exp;
}

abstract class BinaryExp extends Exp {

    public BinaryExp(Exp exp1, Exp exp2) {
        this.exp1 = exp1;
        this.exp2 = exp2;
    }

    public int getLine() {
        return exp1.getLine();
    }

    public int getChar() {
        return exp1.getChar();
    }

    protected Exp exp1;
    protected Exp exp2;
}


// **********************************************************************
// UnaryExps
// **********************************************************************
class UnaryMinusExp extends UnaryExp {

    public UnaryMinusExp(Exp exp) {
        super(exp);
    }
}

class NotExp extends UnaryExp {

    public NotExp(Exp exp) {
        super(exp);
    }
}

class AddrOfExp extends UnaryExp {

    public AddrOfExp(Exp exp) {
        super(exp);
    }
}

class DeRefExp extends UnaryExp {

    public DeRefExp(Exp exp) {
        super(exp);
    }
}

class RelationalExp extends BinaryExp{
  public RelationalExp(Exp exp1, Exp exp2){
    super(exp1,exp2);
  }
  public Type typeCheck(){
    Type type1 = exp1.typeCheck();
    Type type2 = exp2.typeCheck();
    if(type1.isErrorUndefinedType() || type2.isErrorUndefinedType())
      return Type.CreateSimpleType(Type.errorUndefinedTypeName);
    else if(type1.isSameTypeAs(type2))
      return type1;
    else
      return Type.CreateSimpleType(Type.errorTypeName);

  }
}
class ArithmeticExp extends BinaryExp{
  public ArithmeticExp(Exp exp1, Exp exp2){
    super(exp1,exp2);
  }

  public Type typeCheck(){
    Type type1 = exp1.typeCheck();
    Type type2 = exp2.typeCheck();
    if(exp2 instanceof RelationalExp || exp1 instanceof RelationalExp)
      return Type.CreateSimpleType(Type.errorTypeName);
    if(type1.isErrorUndefinedType() || type2.isErrorUndefinedType())
      return Type.CreateSimpleType(Type.errorUndefinedTypeName);
    if(type1.isSameTypeAs(type2))
      return type1;
    // else if(type1.isStringType() && type2.isIntType() ||
    // type1.isIntType() && type2.isStringType())
    else
      return Type.CreateSimpleType(Type.errorTypeName);
  }
}


// **********************************************************************
// BinaryExps
// **********************************************************************
class PlusExp extends ArithmeticExp {

    public PlusExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class MinusExp extends ArithmeticExp {

    public MinusExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class TimesExp extends ArithmeticExp {

    public TimesExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class DivideExp extends ArithmeticExp {

    public DivideExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class ModuloExp extends ArithmeticExp {

    public ModuloExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class AndExp extends ArithmeticExp {

    public AndExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class OrExp extends ArithmeticExp {

    public OrExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class EqualsExp extends RelationalExp {

    public EqualsExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class NotEqualsExp extends RelationalExp {

    public NotEqualsExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class LessExp extends RelationalExp {

    public LessExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class GreaterExp extends RelationalExp {

    public GreaterExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class LessEqExp extends RelationalExp {

    public LessEqExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}

class GreaterEqExp extends RelationalExp {

    public GreaterEqExp(Exp exp1, Exp exp2) {
        super(exp1, exp2);
    }
}




class SymbolTable {
  public static void Initilize(){
    list = new LinkedList<HashMap<String,List<SymbolEntry>>>();
    list.add(new HashMap<String,List<SymbolEntry>>());
  }

  public static void addDecl(String name, SymbolEntry sym){
    if(list == null)   Initilize();
      if (name == null || sym == null)
          return;

      if (list.isEmpty())
          return;

      HashMap<String, List<SymbolEntry>> symTab = list.get(0);
      if (symTab.containsKey(name)){
          symTab.get(name).add(sym);
      }
      List<SymbolEntry> addNew = new LinkedList<SymbolEntry>();
      addNew.add(sym);
      symTab.put(name, addNew);
  }
  public static void addScope() {
    if(list == null)   Initilize();
    list.add(0, new HashMap<String, List<SymbolEntry>>());
  }
  public static List lookupAtScope(int pos,String name){
    if(list == null)   Initilize();
    if (list.isEmpty()) return null;
    if (pos >= list.size()) return null;
    HashMap<String, List<SymbolEntry>> symTab = list.get(0);
    return list.get(pos).get(name);
  }
  public static List lookupLocal(String name) {return lookupAtScope(0,name);}
  public static List lookupPreScope(String name){return lookupAtScope(1,name);}
  protected static List lookupGlobal(String name) {
      if (list.isEmpty())
          return null;

      for (HashMap<String,List<SymbolEntry>> symTab : list) {
          List<SymbolEntry> sym = symTab.get(name);
          if (sym != null)
              return sym;
      }
      return null;
  }
  public static void removeScope(){
      if (list.isEmpty())
          return;
      list.remove(0);
  }
  //Return the size of the list;
  public static int size() {return list.size();}
  public static HashMap<String,List<SymbolEntry>> currentScope(){return list!= null ? list.get(0) : null;};
  //List of HashMap: Every list is a single scope. The Last one is the gobal
  //scope list.
  private static List<HashMap<String, List<SymbolEntry>>> list;

}

//Entry for the symbol table
//Each entry contains a Type
class SymbolEntry{
  private Type type;
  private boolean isGlobalVar = false;
  private boolean isUsed = false;
  public SymbolEntry(Type type){
    this.type = type;
  }
  public SymbolEntry(Type type,boolean isGlobal){
    this.type = type;
    this.isGlobalVar = isGlobal;
  }
  public Type getType(){return this.type;}
  public void setGlobal(boolean isGlobal){this.isGlobalVar = isGlobal;}
  public void setIsUSed(boolean isUsed){this.isUsed = isUsed;}
  public boolean isGlobal(){return this.isGlobalVar;}

  public boolean isUsed(){return this.isUsed;}
}

class FuncEntry extends SymbolEntry{
  private int numParams;
  private List formals = null;
  private List decls = null;
  private boolean isFnPre = true;
  //private HashMap<String,Type> paramList = null;
  public FuncEntry(Type type,int params,List formals,List decls){
    super(type);
    this.numParams = params;
    this.formals = formals;
    this.decls = decls;
  }
  public FuncEntry(Type type,int params,List formals,List decls,boolean isPre){
    super(type);
    this.numParams = params;
    this.formals = formals;
    this.decls = decls;
    this.isFnPre = false;
  }
  public int numParams(){return this.numParams;}
  public Type getReturnType(){return this.getType();}
  public List getFormalListOfFunc(){return this.formals;}
  public List getDeclListOfFunc(){return this.decls;}
  public boolean isPreFn(){return this.isFnPre;}
}
