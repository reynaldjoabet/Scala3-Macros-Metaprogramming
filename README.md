# Scala3-Macros-Metaprogramming
Scala 3 gives you static metaprogramming built on:
- Inline: evaluate code at compile-time when arguments are known.
- Quotes & Splices: build and transform syntax trees (macros).
- Type-level ops: summon givens, compute with types, match types.
- Derivation: auto-create typeclass instances via Mirror

## Inline basics (zero macros, already powerful)

You can't define and use a macro in the same file

macros let you generate or transform code at compile time.

- Quotes — the context needed for quoting and splicing code
- Expr — a representation of typed expressions
- Type — a representation of types
- ToExpr / FromExpr — typeclass interfaces for converting between runtime values and compile-time expressions

When you import quotes.reflect.*, you get access to compiler internals, like:
Term, TypeTree, Symbol, DefDef, ValDef, etc.
Useful for inspecting and transforming abstract syntax trees (ASTs).

validating SQL queries, JSON schemas, or file paths at compile time

### Type-Safe Database Queries (DSLs like Doobie or Slick)
Macros can analyze and generate SQL queries based on Scala code, ensuring type correctness.
For example, a macro could:
Parse a Scala case class
Generate an SQL INSERT or SELECT statement automatically
Ensure field names and types match
This is how libraries like `Slick` and `Quill` achieve compile-time safety for queries.

### Compile-Time Code Generation
Macros can generate boilerplate code automatically — such as:
JSON (de)serialization
Equality and hashing methods
Typeclass instances
Example — Auto-Derivation of Typeclasses

### Inlining and Performance Optimization
Macros can inline complex logic so that unnecessary computations are eliminated at compile time

### Safer DSLs (Domain-Specific Languages)
Macros let you build fluent, type-safe DSLs that look like natural Scala code but have compile-time checking.
Examples:
HTML builders (scalatags, lihaoyi)
SQL/GraphQL DSLs (Caliban, Sangria)
Testing frameworks (like ScalaTest’s assert macros)

## Boxing
If you use a primitive in a context that requires an object (like putting an Int into a List or a Map), the JVM "boxes" it into a wrapper object (e.g., java.lang.Integer).

- Unboxed (Primitive): Int = 4 bytes.
- Boxed (Object): Integer object = 16 bytes (12-byte header + 4-byte value)

Int usually represents a single word, while long represents double word

A word - is a number of bits for majority of the cpu registers

Boxed(reference) types are primitive data types wrapped in an object. Values of such types are accessed through a pointer

Why a performance penalty for using box types?
- Stack vs Heap: primitive types are allocated on the stack while boxed types live in the heap. Stack memory is way faster than heap to access
- Data Size: Boxing overhead is significantWhile regular int takes only 4 bytes of space (32-bits),Integer type takes 16 bytes(128-bits)
- Boxed Objects: The collection (like a List) doesn't hold the numbers; it holds pointers to objects scattered elsewhere on the heap. The CPU has to "follow the pointer" to find the actual value. This often results in a cache miss, forcing the CPU to wait for the much slower RAM

[primitive-vs-boxed-performance](https://alammori.com/benchmarks/primitive-vs-boxed-performance)

When CPU fetches data from memory it never fetches just one value. Instead it fetches what is called a cache line. The size of cache line is usually 64 Bytes, at least for Intel and AMD

What that means, is that CPU fetches an entire block from RAM to its cache and doesn’t have to do expensive round trips to RAM as long as it has required data in the cache.

In this case it played a significant role, because our variables were co-allocated in RAM, so each fetch cycle retrieved anywhere from 1 to 16 variables.

array of Integers is an array of pointers. Although the pointers were fetched from the RAM, on each access CPU has to do one more trip to the RAM to fetch actual data

Because your Int primitives are packed together, that one 64-byte fetch actually brings in 16 integers (64÷4=16).

When you declare a variable for an object (e.g., User person = new User();), the variable itself (the address or pointer) is stored on the Stack.

- The stack is fast and handles local variables within a function's scope.
- Once the function finishes, the stack frame is cleared, and that reference is gone.

The object's contents (its fields, properties, and values) are stored on the Heap.
- The heap is a large pool of memory used for dynamic allocation.
- When you use the new keyword, you are telling the system to find a spot on the heap big enough to hold that object's data

Stack size is usually fixed and much smaller than the heap (often 1MB to 8MB by default, depending on the OS).

Every time a thread calls a function, it pushes a new "frame" onto its stack. This frame contains local variables and the return address.

### Call Site
Where the macro is invoked in user code.

```scala
// Definition
inline def debug(inline expr: Any): Unit = ${ debugImpl('expr) }

// Call site (where you use it)
val x = 42
debug(x + 1)  // ← This is the CALL SITE
```

### Definition Site
Where the macro is defined

```scala
// Definition site ↓
inline def debug(inline expr: Any): Unit = ${ debugImpl('expr) }

def debugImpl(expr: Expr[Any])(using Quotes): Expr[Unit] = {
  // Macro implementation lives here
  '{ println(${Expr(expr.show)} + " = " + $expr) }
}

```
### Expansion Site
Where the macro's generated code is inserted (usually the same as call site).

```scala
// Before expansion (call site)
debug(x + 1)

// After expansion (expansion site - same location)
println("x + 1" + " = " + (x + 1))
```
### Splice Site
Where `${ ... }` appears inside a macro—triggers compile-time execution.

```scala
inline def show[T]: String = ${ showImpl[T] }  // ← Splice site
//                           ^^^^^^^^^^^
```
### Quote Site
Where `'{ ... }` appears—creates code that will exist at runtime.
```scala
def showImpl[T: Type](using Quotes): Expr[String] = {
  val typeName = Type.show[T]
  '{ "Type: " + ${Expr(typeName)} }  // ← Quote site
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
}
```

### The Splice ( ) is the "Hole" in the Compiler

`inline def example: Int = ${ /* HOLE - escape to compile-time */ }`

```sh
┌─────────────────────────────────────────────────────────┐
│  RUNTIME WORLD (normal Scala code)                      │
│                                                         │
│    val x = 1 + 2                                        │
│    val y = ${ ══════════════════════════╗               │
│              ║  COMPILE-TIME WORLD      ║               │
│              ║  (macro execution)       ║               │
│              ║                          ║               │
│              ║  // Can inspect types    ║               │
│              ║  // Generate code        ║               │
│              ║  // Access compiler      ║               │
│              ╚══════════════════════════╝ }             │
│    val z = y + 1                                        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

Quotes Fill the Hole Back
And `'{ } `is how you return from the hole back to runtime:

```scala
def macroImpl(using Quotes): Expr[Int] = {
  // Inside the hole (compile-time)
  val computed = 1 + 2 + 3  // Runs at compile time
  
  '{ 100 }  // "Fill the hole" with this runtime code
  // ↑ Returns an Expr that becomes the value at the call site
}

inline def example: Int = ${ macroImpl }
// After expansion: val x: Int = 100
```

Quotes Build Code, They Don't Run It
`'{ ... }` doesn't execute the code—it constructs an AST (Abstract Syntax Tree) representing that code.

`${ ... }` (splice)	Executes Scala code	Compile time
`'{ ... }` (quote)	Builds an AST representing code.The quote itself runs at compile time, but the code inside runs at runtime

```sh
COMPILE TIME                          RUNTIME
─────────────────────────────────────────────────────────
                                      
${ greetImpl('name) }                 
    │                                 
    ▼                                 
greetImpl executes                    
    │                                 
    ▼                                 
'{ println(...) }                     
    │                                 
    │ (builds AST)                    
    ▼                                 
Expr[Unit] returned                   
    │                                 
    │ (inserted into program)         
    ▼                                 
─────────────────────────────────────────────────────────
                                      println("Hello, World")
                                          │
                                          ▼
                                      "Hello, World" printed
```                                     
When you write `'{ println("Hello") }`, you are not executing `println`.
- Instead, you are creating a Data Structure (an `Expr[Unit]`) that represents the idea of printing `"Hello."`
- In Scala 3, a `Quote` is essentially a serialized AST (`Abstract Syntax Tree`).

Inlining: The compiler takes that tree and pastes it directly into your source code where the `Splice` was


```scala
inline def query[T]: EntityQuery[T] = ${ QueryMacro[T] }
inline def select[T]: Query[T] = ${ QueryMacro[T] }

def max[A](a: A): A = NonQuotedException()
def min[A](a: A): A = NonQuotedException()
def count[A](a: A): A = NonQuotedException()
def avg[A](a: A)(implicit n: Numeric[A]): BigDecimal = NonQuotedException()
def sum[A](a: A)(implicit n: Numeric[A]): A = NonQuotedException()

def avg[A](a: Option[A])(implicit n: Numeric[A]): Option[BigDecimal] = NonQuotedException()
def sum[A](a: Option[A])(implicit n: Numeric[A]): Option[A] = NonQuotedException()

extension [T](o: Option[T]) {
  def filterIfDefined(f: T => Boolean): Boolean = NonQuotedException()
}

object extras extends DateOps {
  extension [T](a: T) {
    def getOrNull: T =
      throw new IllegalArgumentException(
        "Cannot use getOrNull outside of database queries since only database value-types (e.g. Int, Double, etc...) can be null."
      )

def ===(b: T): Boolean =
  (a, b) match {
    case (a: Option[_], b: Option[_]) => 
      a.exists(av => b.exists(bv => av == bv))
    case (a: Option[_], b)            => 
      a.exists(av => av == b)
    case (a, b: Option[_])            => 
      b.exists(bv => bv == a)
    case (a, b)                       => a == b
  }

def =!=(b: T): Boolean =
  (a, b) match {
    case (a: Option[_], b: Option[_]) => 
      a.exists(av => b.exists(bv => av != bv))
    case (a: Option[_], b)            => 
      a.exists(av => av != b)
    case (a, b: Option[_])            => 
      b.exists(bv => bv != a)
    case (a, b)                       => a != b
  }
  }
}

inline def static[T](inline value: T): T = ${ StaticSpliceMacro('value) }

inline def insertMeta[T](inline exclude: (T => Any)*): InsertMeta[T] = ${ InsertMetaMacro[T]('exclude) }

inline def updateMeta[T](inline exclude: (T => Any)*): UpdateMeta[T] = ${ UpdateMetaMacro[T]('exclude) }

inline def lazyLift[T](inline vv: T): T = ${ LiftMacro.applyLazy[T, Nothing]('vv) }

inline def quote[T](inline bodyExpr: Quoted[T]): Quoted[T] = ${ QuoteMacro[T]('bodyExpr) }

inline def quote[T](inline bodyExpr: T): Quoted[T] = ${ QuoteMacro[T]('bodyExpr) }

inline implicit def unquote[T](inline quoted: Quoted[T]): T = ${ UnquoteMacro[T]('quoted) }

inline implicit def autoQuote[T](inline body: T): Quoted[T] = ${ QuoteMacro[T]('body) }

```

The call site is where the inline method is used.. `quote` is inline, very important

`Quotes, Expr[T], and Type[T]` — The Macro API Foundation
Every macro implementation receives a Quotes context parameter

`Quotes` — Gives access to the compiler's reflection API via quotes.reflect._
`Expr[T]` — A typed representation of a Scala expression at compile time. It's code-as-data.
`Type[T]` — A compile-time representation of a type. Passed via using `Type[T]`.
`Expr[T]` is not a value — it's a description of code that will eventually produce a value of type T at runtime.

Transparent Inline
This is a Scala 3 feature that allows a macro to change its return type based on the input.

`Expr[T]`: Represents a typed abstract syntax tree (AST) of code that will evaluate to a value of type T. You build these using quotes ('{ ... }) and evaluate them using splices (${ ... }).

`Type[T]`: Represents a Scala type. Used to pass type information into macros.

`quotes.reflect.*`: The low-level API. When you need to inspect case class fields, check class flags, or manually stitch together ASTs (Term, Tree, Symbol), you drop down into the reflection API.


[sanely-automatic-derivation](https://kubuszok.com/2025/sanely-automatic-derivation/)

It creates a "phantom" value of any type T that only exists at compile time and is completely erased before runtime.

```scala
import scala.compiletime.erasedValue

// Returns a phantom value of type T — never actually instantiated
erasedValue[T]

```


```scala
inline def apply[This >: this.type <: Tuple](n: Int): Elem[This, n.type] =
  runtime.Tuples.apply(this, n).asInstanceOf[Elem[This, n.type]]
```
`n.type` is the singleton type of the argument `n`


Because the method is `inline`, the compiler expands it at the call site. That matters because the return type mentions things like `this.type` and `n.type` (singleton types), and inlining helps the compiler keep those precise.

`Match types: type-level functions over tuples`

In object Tuple, you’ve got:
`Head`, `Tail`, `Last`, `Init`,`Concat` (`++`),`Elem`,`Size`,`Map`, `FlatMap`, `Filter`, `Zip`,`Fold` and derived helpers like `Union`
These are “functions at the type level”. Example:

```scala
type Elem[X <: Tuple, N <: Int] = X match {
  case x *: xs =>
    N match {
      case 0 => x
      case S[n1] => Elem[xs, n1]
    }
}

type First = Elem[(String, Int, Boolean), 0]  // String

type Head[X <: Tuple] = X match {
  case x *: _ => x
}
```

The tuple type `(A, B, C)` is encoded as: `A *: (B *: (C *: EmptyTuple))`

