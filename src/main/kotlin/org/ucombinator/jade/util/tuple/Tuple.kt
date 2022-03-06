package org.ucombinator.jade.util.tuple

@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
data class Fourple<out A, out B, out C, out D>(val _1: A, val _2: B, val _3: C, val _4: D)

@Suppress("VARIABLE_NAME_INCORRECT_FORMAT")
data class Fiveple<out A, out B, out C, out D, out E>(val _1: A, val _2: B, val _3: C, val _4: D, val _5: E)

@Suppress("FUNCTION_NAME_INCORRECT_CASE")
fun <A, B> Pair<A, B>._1(): A = this.first

@Suppress("FUNCTION_NAME_INCORRECT_CASE")
fun <A, B> Pair<A, B>._2(): B = this.second

@Suppress("FUNCTION_NAME_INCORRECT_CASE")
fun <A, B, C> Triple<A, B, C>._1(): A = this.first

@Suppress("FUNCTION_NAME_INCORRECT_CASE")
fun <A, B, C> Triple<A, B, C>._2(): B = this.second

@Suppress("FUNCTION_NAME_INCORRECT_CASE")
fun <A, B, C> Triple<A, B, C>._3(): C = this.third