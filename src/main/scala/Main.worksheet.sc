val age: Int = 32

//val rockets:Short=434

Int.MaxValue
Int.MinValue

Byte.MaxValue
Byte.MinValue

val bigInt: BigInt = BigInt("123456789012345678901234567890")
Short.MaxValue
Short.MinValue

Long.MaxValue
Long.MinValue

Float.MaxValue
Float.MinValue

Double.MaxValue
Double.MinValue

/// calculate the memory size of an object
Char.MaxValue
Char.MinValue

//8bits ==1 byte
java.lang.Integer.SIZE

java.lang.Byte.SIZE

java.lang.Short.SIZE

java.lang.Long.SIZE

java.lang.Float.SIZE

java.lang.Double.SIZE

java.lang.Character.SIZE

val isEmployed: Boolean = true

class Student(name: String, age: Int)

val stu = new Student("Peter", 20)

val stu100 = new Student("Joan", 19)
class Employee(salary: Double, department: String)

//If you have an array of a million Ints, that's ~4MB.

(1 to 1_000_000).map(_ + 1)

import example.*
// 2. CALL SITE (user's code)
greet("World")

// 3. What happens during COMPILATION of user's code:
//    - greetImpl runs
//    - Prints "Macro is expanding!" to compiler output
//    - Returns Expr[Unit] containing AST for: println("Hello, " + "World")

// 4. What exists in compiled bytecode (RUNTIME):
println("Hello, " + "World")
