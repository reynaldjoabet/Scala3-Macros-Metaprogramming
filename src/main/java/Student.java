package example;

import scala.StringContext.s$;

class Student {
	private int age;
	private String name;
   
   public Student(){
    
   }
   
	public Student(String name, int age) {
		this.age = age;
		this.name = name;
	}

    public void setAge(int age){
        this.age=age;
    }

	char grade = 'B';
    
    // getter
    public String getName(){
        return this.name;
    }

    // setter
    public void setName(String nam){
        this.name=nam;
    }


	public static void main(String args[]) {

		Student student = new Student("Paul", 23);

		Student student3 = new Student("Paula", 20);

		System.out.println("my name is" + student.name);
	}
}
