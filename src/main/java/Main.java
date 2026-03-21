package example;

import java.util.ArrayList;
import java.util.List;

public class Main{

    int age= 10;
    String name="my name";
    double price=4.13;
    float pi=3.17f;

    long distance=90;
    byte mybyte=120;
    List<Byte> myBytes= new ArrayList();


  char grade='B';

  public static void main(String args[]) {
    Student student= new Student("Paul",34);
    
    Student stu= new Student();

   student.setName("Peter");

   stu.setName("Luke");
   stu.setAge(24);

    System.out.println("my name is" + student.getName());
  }

  //statistically typed
  ///dynamically typed
  
}

//Local primitive variables live in stack memory. 
// If a primitive is part of an object or class, it’s stored in the heap right alongside the object. 
// There’s no wrapper or extra memory overhead either way, so access stays quick and direct.


//stack is per thread
//1 cpu = 1 thread( simplistic view,old design)
//hardware threads represent the number of concurrent execution context the CPU can handle
//many software threads