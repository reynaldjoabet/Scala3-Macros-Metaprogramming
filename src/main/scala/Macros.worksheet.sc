import example.Macros.*
import scala.quoted.Expr

val x: Int = 5
val a: Int = 10

// Usage
showType[Option[Int]] // "Type: Option[Int], isCase: false, fields: List()"

// Usage
optimize(5 + 0) // Compiles to: 5
optimize(x * 1) // Compiles to: x
optimize(a - a)

// Usage
case class Person(name: String, age: Int)
toMap(Person("Alice", 30))

// Map("name" -> "Alice", "age" -> 30)      // Compiles to: 0

// Usage
mustBePositive(5) // OK, info message
//mustBePositive(-1)   // Compile error!
mustBePositive(x) // Warning, runtime check inserted

// Usage
tupleToList((1, "hello", true)) // List(1, "hello", true)

// Usage
enum Color { case Red, Green, Blue }
enumName(Color.Red) // "Red"

// Usage - ALL resolved at compile time
describe[Int] // "integer"
describe[String] // "string"
describe[List[Int]] // "list of integer"
describe[List[String]] // "list of string"

// Usage - return type is refined!
val b: Boolean = parse("true") // Compiler knows it's Boolean
val i: Int = parse("123") // Compiler knows it's Int
val s: String = parse("hello") // Compiler knows it's String

val kl: "hellohellohello" = repeat("hello", 3) // Literal type!

val intSize: 4 = sizeof[Int] // Literal type 4

import scala.compiletime.constValue

inline def tupleSize[T <: Tuple]: Int = constValue[Tuple.Size[T]]

val n = tupleSize[(Int, String, Boolean)]

tupleSize[EmptyTuple] // 0
tupleSize[(Int, String)]

// ============================================================
// REAL-LIFE TUPLE OPERATIONS
// ============================================================

// --- 1. Basic construction & decomposition ---
val config = ("localhost", 8080, true) // (String, Int, Boolean)
config.head // "localhost"
config.tail // (8080, true)
config.last // true
config(1) // 8080
config.size // 3

// Pattern matching with *: (the tuple cons type)
config match {
  case host *: port *: ssl *: EmptyTuple =>
    s"Connecting to $host:$port (ssl=$ssl)"
}

// --- 2. Prepend, Append, Concat ---
val withProtocol = "https" *: config // ("https", "localhost", 8080, true)
val withTimeout = config :* 30_000 // ("localhost", 8080, true, 30000)
val full = withProtocol ++ withTimeout // all 7 elements merged

// --- 3. Map: apply a polymorphic function to each element ---
val data = (1, "hello", 2.5)
val wrapped =
  data.map([t] => (x: t) => Option(x)) // (Some(1), Some("hello"), Some(2.5))

// --- 4. Zip: pair up two tuples element-wise ---
val keys = ("name", "age", "active")
val values = ("Alice", 30, true)
val zipped = keys.zip(values) // (("name","Alice"), ("age",30), ("active",true))

// --- 5. Take, Drop, SplitAt ---
val row = ("id-42", "Alice", 30, "alice@example.com", true)
row.take(2) // ("id-42", "Alice")
row.drop(3) // ("alice@example.com", true)
val (metadata, payload) = row.splitAt(1) // ("id-42",) and ("Alice", 30, ...)

// --- 6. Reverse ---
val reversed = (1, 2, 3, 4).reverse // (4, 3, 2, 1)

// --- 7. Init / Last ---
val path = ("users", "123", "profile", "edit")
path.init // ("users", "123", "profile")
path.last // "edit"

// --- 8. toList / toArray / toIArray ---
val asList = (1, "two", 3.0).toList // List[Int | String | Double]
val asArray = (1, "two", 3.0).toArray // Array[Object]

// --- 9. Real use-case: type-safe config builder ---
// Tuples let you accumulate heterogeneous values with full type tracking.
case class ServerConfig(host: String, port: Int, ssl: Boolean)

def buildConfig(t: (String, Int, Boolean)): ServerConfig =
  ServerConfig(t(0), t(1), t(2))

val cfg = buildConfig(("0.0.0.0", 443, true))

// --- 10. Real use-case: type-safe database row ---
// Each column keeps its type — no casting, no runtime errors
type UserRow = (Int, String, String, Boolean)
val row1: UserRow = (1, "alice", "alice@dev.com", true)
val (id, name, email, active) = (row1(0), row1(1), row1(2), row1(3))

// --- 11. Compile-time tuple type operations with constValue ---
inline def tupleHead[T <: NonEmptyTuple]: Tuple.Head[T] =
  ??? // type-level only — Head[(Int, String)] = Int

// Size is a literal Int at compile time
val sz: 5 = constValue[Tuple.Size[(Int, String, Boolean, Double, Long)]]

// --- 12. Real use-case: Named tuples (Scala 3.x) for lightweight records ---
// (Requires Scala 3.5+)
type Person2 = (name: String, age: Int)
val person: Person2 = (name = "Bob", age = 25)
person.name // "Bob" — field access by name!
person.age // 25

// Convert named tuple to regular tuple ops
person.toTuple.reverse // (25, "Bob")

// --- 13. Real use-case: Tuple.fromProduct for generic programming ---
case class Point(x: Double, y: Double, z: Double)
val pt = Point(1.0, 2.0, 3.0)
val ptTuple = Tuple.fromProduct(pt) // (1.0, 2.0, 3.0) as Tuple
// Round-trip back:
val pt2 = Tuple.fromProductTyped(pt) // typed as (Double, Double, Double)

// --- 14. Real use-case: Accumulating heterogeneous results ---
def fetchUser(): String = "Alice"
def fetchAge(): Int = 30
def fetchScore(): Double = 95.5

val results = (fetchUser(), fetchAge(), fetchScore()) // (String, Int, Double)
// Each element retains its precise type — no Any, no casting

// --- 15. Compile-time type-level Filter (from Tuple companion) ---
import scala.compiletime.erasedValue
type IsString[x] <: Boolean = x match {
  case String => true
  case _      => false
}
// Tuple.Filter[(Int, String, Double, String), IsString] =:= (String, String)
summon[
  Tuple.Filter[(Int, String, Double, String), IsString] =:= (String, String)
]

// --- 16. Type-level Contains / Disjoint ---
summon[Tuple.Contains[("a", "b", "c"), "b"] =:= true]
summon[Tuple.Contains[("a", "b", "c"), "z"] =:= false]
summon[Tuple.Disjoint[("a", "b"), ("c", "d")] =:= true]
summon[Tuple.Disjoint[("a", "b"), ("b", "c")] =:= false]

// --- 17. Fold: collapse a tuple type into a single type ---
// Union[T] is Fold[T, Nothing, [x, y] =>> x | y]
type MyUnion = Tuple.Union[(Int, String, Boolean)] // Int | String | Boolean
val u: MyUnion = 42 // OK
val u2: MyUnion = "hello" // OK
val u3: MyUnion = true // OK
