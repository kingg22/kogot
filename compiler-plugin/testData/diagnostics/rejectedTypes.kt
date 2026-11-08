// RUN_PIPELINE_TILL: FRONTEND
// Black box: types that can never cross the Variant boundary, in every checked position. Byte is rejected on purpose
// (signed in Kotlin, unsigned in Godot); Kotlin collections, unsigned and function types need their Godot equivalents.

import io.github.kingg22.godot.api.builtin.*

class UserClass
data class UserData(val value: Int)
@JvmInline value class UserValue(val value: Int)
object UserObject
enum class UserEnum { ENTRY }
sealed class UserSealed
abstract class UserAbstract
open class UserOpen

// A lambda parameter cannot spell a function type inline.
typealias Action = () -> Unit

fun valueArguments(
    byte: Byte,
    nullableByte: Byte?,
    char: Char,
    nullableChar: Char?,
    uByte: UByte,
    nullableUByte: UByte?,
    uShort: UShort,
    nullableUShort: UShort?,
    uInt: UInt,
    nullableUInt: UInt?,
    uLong: ULong,
    nullableULong: ULong?,
    intArray: IntArray,
    nullableIntArray: IntArray?,
    byteArray: ByteArray,
    nullableByteArray: ByteArray?,
    arrayOfInt: Array<Int>,
    nullableArrayOfInt: Array<Int>?,
    list: List<Int>,
    nullableList: List<Int>?,
    mutableList: MutableList<Int>,
    nullableMutableList: MutableList<Int>?,
    map: Map<String, Int>,
    nullableMap: Map<String, Int>?,
    set: Set<Int>,
    nullableSet: Set<Int>?,
    iterable: Iterable<Int>,
    nullableIterable: Iterable<Int>?,
    pair: Pair<Int, Int>,
    nullablePair: Pair<Int, Int>?,
    regex: Regex,
    nullableRegex: Regex?,
    throwable: Throwable,
    nullableThrowable: Throwable?,
    lazy: Lazy<Int>,
    nullableLazy: Lazy<Int>?,
    function: () -> Unit,
    nullableFunction: (() -> Unit)?,
    userClass: UserClass,
    nullableUserClass: UserClass?,
    userData: UserData,
    nullableUserData: UserData?,
    userValue: UserValue,
    nullableUserValue: UserValue?,
    userObject: UserObject,
    nullableUserObject: UserObject?,
    userEnum: UserEnum,
    nullableUserEnum: UserEnum?,
    userSealed: UserSealed,
    nullableUserSealed: UserSealed?,
    userAbstract: UserAbstract,
    nullableUserAbstract: UserAbstract?,
    userOpen: UserOpen,
    nullableUserOpen: UserOpen?,
) {
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>byte<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableByte<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>char<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableChar<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>uByte<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUByte<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>uShort<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUShort<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>uInt<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUInt<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>uLong<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableULong<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>intArray<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableIntArray<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>byteArray<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableByteArray<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>arrayOfInt<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableArrayOfInt<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>list<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableList<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>mutableList<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableMutableList<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>map<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableMap<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>set<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableSet<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>iterable<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableIterable<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>pair<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullablePair<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>regex<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableRegex<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>throwable<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableThrowable<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>lazy<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableLazy<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>function<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableFunction<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>userClass<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUserClass<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>userData<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUserData<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>userValue<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUserValue<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>userObject<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUserObject<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>userEnum<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUserEnum<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>userSealed<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUserSealed<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>userAbstract<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUserAbstract<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>userOpen<!>)
    <!MUST_BE_VARIANT_CHECKED!>anyToVariant<!>(<!NOT_VARIANT_COMPATIBLE!>nullableUserOpen<!>)
}

fun explicitTypeArguments() {
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Byte<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Byte?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Char<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Char?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UByte<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UByte?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UShort<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UShort?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UInt<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UInt?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>ULong<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>ULong?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>IntArray<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>IntArray?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>ByteArray<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>ByteArray?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Array<Int><!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Array<Int>?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>List<Int><!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>List<Int>?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>MutableList<Int><!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>MutableList<Int>?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Map<String, Int><!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Map<String, Int>?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Set<Int><!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Set<Int>?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Iterable<Int><!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Iterable<Int>?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Pair<Int, Int><!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Pair<Int, Int>?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Regex<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Regex?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Throwable<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Throwable?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Lazy<Int><!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>Lazy<Int>?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>() -> Unit<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>(() -> Unit)?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserClass<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserClass?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserData<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserData?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserValue<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserValue?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserObject<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserObject?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserEnum<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserEnum?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserSealed<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserSealed?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserAbstract<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserAbstract?<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserOpen<!>>()
    <!MUST_BE_VARIANT_CHECKED!>variantOf<!><<!NOT_VARIANT_COMPATIBLE!>UserOpen?<!>>()
}

fun callableParameters() {
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { byte: Byte -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { char: Char -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { uByte: UByte -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { uShort: UShort -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { uInt: UInt -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { uLong: ULong -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { intArray: IntArray -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { byteArray: ByteArray -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { arrayOfInt: Array<Int> -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { list: List<Int> -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { mutableList: MutableList<Int> -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { map: Map<String, Int> -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { set: Set<Int> -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { iterable: Iterable<Int> -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { pair: Pair<Int, Int> -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { regex: Regex -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { throwable: Throwable -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { lazy: Lazy<Int> -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { function: Action -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { userClass: UserClass -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { userData: UserData -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { userValue: UserValue -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { userObject: UserObject -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { userEnum: UserEnum -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { userSealed: UserSealed -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { userAbstract: UserAbstract -> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { userOpen: UserOpen -> }
}

fun callableResults(
    byte: Byte,
    char: Char,
    uByte: UByte,
    uShort: UShort,
    uInt: UInt,
    uLong: ULong,
    intArray: IntArray,
    byteArray: ByteArray,
    arrayOfInt: Array<Int>,
    list: List<Int>,
    mutableList: MutableList<Int>,
    map: Map<String, Int>,
    set: Set<Int>,
    iterable: Iterable<Int>,
    pair: Pair<Int, Int>,
    regex: Regex,
    throwable: Throwable,
    lazy: Lazy<Int>,
    function: () -> Unit,
    userClass: UserClass,
    userData: UserData,
    userValue: UserValue,
    userObject: UserObject,
    userEnum: UserEnum,
    userSealed: UserSealed,
    userAbstract: UserAbstract,
    userOpen: UserOpen,
) {
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>byte<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> byte }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>char<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> char }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>uByte<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> uByte }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>uShort<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> uShort }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>uInt<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> uInt }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>uLong<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> uLong }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>intArray<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> intArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>byteArray<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> byteArray }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>arrayOfInt<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> arrayOfInt }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>list<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> list }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>mutableList<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> mutableList }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>map<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> map }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>set<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> set }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>iterable<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> iterable }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>pair<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> pair }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>regex<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> regex }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>throwable<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> throwable }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>lazy<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> lazy }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>function<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> function }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>userClass<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> userClass }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>userData<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> userData }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>userValue<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> userValue }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>userObject<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> userObject }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>userEnum<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> userEnum }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>userSealed<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> userSealed }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>userAbstract<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> userAbstract }
    <!MUST_BE_VARIANT_CHECKED!>Callable<!> { <!NOT_VARIANT_COMPATIBLE!>userOpen<!> }
    <!MUST_BE_VARIANT_CHECKED, NOT_VARIANT_COMPATIBLE!>Callable<!> { _: Int -> userOpen }
}
